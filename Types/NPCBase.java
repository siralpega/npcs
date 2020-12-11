package com.github.siralpega.NPCs.Types;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.github.siralpega.NPCs.NPCsPlugin;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import net.md_5.bungee.api.ChatColor;

//Learned how to use packets from: https://www.youtube.com/watch?v=BTvqMzHUZ_s & how to use reflection from https://youtu.be/wVyue_zXsmQ 
public abstract class NPCBase
{
	private Location spawnLocation;
	private String name, texture, signature, configID, skin, type;
	private GameProfile profile;
	private Object entityPlayer;
	private ChatColor nameColor = ChatColor.RED;
	protected int entityID;
	protected boolean unique = true;
	private boolean inConfig = false, active = false, canSpawn = false;

	//CREATE DEFAULT (no configuration)
	public NPCBase(Location playerLocation, String name, String texture, String signature)
	{
		this.spawnLocation = playerLocation; 
		this.name = name;
		this.texture = texture;
		this.signature = signature;
		this.type = "dummy";
		if(this.texture == null || this.signature == null)
		{
			File sf = new File("plugins/NPCs/skins.yml");
			FileConfiguration sc = YamlConfiguration.loadConfiguration(sf);
			this.texture = sc.getString("default.texture");
			this.signature = sc.getString("default.signature");
			this.skin = "default";
		}
		canSpawn = true;
	}

	//CREATE FROM CONFIG
	public NPCBase(String configId)
	{
		File npcFile = new File("plugins/NPCs/npcs.yml");
		FileConfiguration c = YamlConfiguration.loadConfiguration(npcFile);
		this.configID = configId;
		if(!c.contains("npcs." + configId))
		{
			NPCsPlugin.getInstance().getLogger().warning("Tried to spawn an NPC from the config, but couldn't find id of " + configId);
			return;
		}
		inConfig = true;
		this.name = c.getString("npcs." + configId + ".name");
		this.skin = c.getString("npcs." + configId + ".skin");
		File sf = new File("plugins/NPCs/skins.yml");
		FileConfiguration sc = YamlConfiguration.loadConfiguration(sf);
		if(skin != null)
		{
			this.texture = sc.getString(skin + ".texture");
			this.signature = sc.getString(skin + ".signature");
		}
		if(skin == null || this.texture == null || this.signature == null)
		{
			this.texture = sc.getString("default.texture");
			this.signature = sc.getString("default.signature");
			this.skin = "default";
		}

		this.type = c.getString("npcs." + configId + ".type");
		if(type == null)
			type = "dummy";
		this.spawnLocation = c.getLocation("npcs." + configId + ".location");
		if(spawnLocation == null)
		{
			NPCsPlugin.getInstance().getLogger().warning("There is no location for NPC " + configId + " to spawn at");
		} 
		if(c.contains("npcs." + configId + ".unique"))
			this.unique = c.getBoolean("npcs." + configId + ".unique");
		
		canSpawn = true;
	}

	/*
	 * PACKETS
	 */

	/**
	 * Registers the NPC with the game/server 
	 */
	public boolean spawn()
	{
		if(!canSpawn || spawnLocation == null || active) // if an error occurred during setup, or is this is already active (in world)
			return false;
		//Java Reflection -- getting a class w/o referencing it
		try
		{
			Object mcServer = getCraftbukkitClass("CraftServer").getMethod("getServer").invoke(Bukkit.getServer());
			Object worldServer = getCraftbukkitClass("CraftWorld").getMethod("getHandle").invoke(spawnLocation.getWorld());

			if(name == null)
				name = "default name";
			if(name.length() > 14) // &6 name is 16 chars, which is max
				this.name = this.name.substring(0, 14);
			profile = new GameProfile(UUID.randomUUID(), nameColor + name);
			profile.getProperties().put("textures", new Property("textures", texture, signature));

			Constructor<?> entityPlayerCon = getNMSClass("EntityPlayer").getDeclaredConstructors()[0];
			Constructor<?> interactManagerCon = getNMSClass("PlayerInteractManager").getDeclaredConstructors()[0];

			entityPlayer = entityPlayerCon.newInstance(mcServer, worldServer, profile, interactManagerCon.newInstance(worldServer));
			entityPlayer.getClass().getMethod("setLocation", double.class, double.class, double.class, float.class, float.class).invoke(entityPlayer, 
					spawnLocation.getBlockX(), spawnLocation.getY(), spawnLocation.getZ(), spawnLocation.getYaw(), spawnLocation.getPitch());
			this.entityID = (int) entityPlayer.getClass().getMethod("getId").invoke(entityPlayer);

			//	NPCsPlugin.getInstance().getNpcs().put(entityID, this);
			NPCsPlugin.getInstance().addNPC(this, entityID);
			NPCsPlugin.getInstance().getLogger().info("Spawned " + this.name + " with id of " + this.entityID);			
			active = true;
			if(inConfig)
				updateConfigEntityId(configID, entityID);
			return true;
		}
		catch(Exception e){e.printStackTrace(); return false;}
	}

	/**
	 * Unregisters the NPC from game/server, then hides it for all players
	 */
	public void despawn() { despawn(false); }
	public void despawn(boolean updateConfig)
	{
		if(!active)
			return;
		Entity e = getEntity();
		e.remove();
		for(Player p : Bukkit.getOnlinePlayers()) //TURN into a task?
			hide(p);

		if(inConfig && updateConfig)
			updateConfigEntityId(configID, entityID);
		//	NPCsPlugin.getInstance().getNpcs().remove(entityID);
		NPCsPlugin.getInstance().removeNPC(this, entityID);
		NPCsPlugin.getInstance().getLogger().info("Despawned " + this.name + " with entity id of " + this.entityID);	
		active = false;
	}

	/**
	 * Sends the NPC packet(s) to the Player (client side)
	 * @param p player
	 */
	public void show(Player p)
	{
		//Java Reflection -- getting a class w/o referencing it
		try
		{
			//new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, (EntityPlayer)entityPlayer));
			Object addPlayerEnum = getNMSClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction").getField("ADD_PLAYER").get(null); //$ means sub-class
			Constructor<?> packetPlayOutCon = getNMSClass("PacketPlayOutPlayerInfo").getConstructor(getNMSClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction"), 
					Class.forName("[Lnet.minecraft.server." + getVersion() + ".EntityPlayer;")); //array.toString() makes this

			//Add NPCS to array packet
			Object array = Array.newInstance(getNMSClass("EntityPlayer"), 1);
			Array.set(array, 0, entityPlayer);

			//Send packet
			Object packetPlayOut = packetPlayOutCon.newInstance(addPlayerEnum, array);
			sendPacket(p, packetPlayOut);

			//PacketPlayOutNamedEntitySpawn
			Constructor<?> packetPlayOutNamedEntity = getNMSClass("PacketPlayOutNamedEntitySpawn").getConstructor(getNMSClass("EntityHuman"));
			Object packetPlayOutEntity = packetPlayOutNamedEntity.newInstance(entityPlayer);
			sendPacket(p, packetPlayOutEntity);

			//PacketPlayOutEntitiyHeadRotation
			Constructor<?> packetPlayOutHeadCon = getNMSClass("PacketPlayOutEntityHeadRotation").getConstructor(getNMSClass("Entity"), byte.class);
			float yaw = (float) entityPlayer.getClass().getField("yaw").get(entityPlayer);
			Object packetPlayOutHead = packetPlayOutHeadCon.newInstance(entityPlayer, (byte) (yaw * 256 / 360));
			sendPacket(p, packetPlayOutHead);
		}
		catch(Exception e)
		{
			e.printStackTrace();	
		}
	}

	/**
	 * Removes and destroys (entity) and the NPC from the player (client side)
	 * In the future, you can hide NPCs from individual players
	 * {@link} https://wiki.vg/Protocol#Spawn_Player
	 * @param p The player to send the packet to
	 */
	public void hide(Player p)
	{
		//PlayerConnection connection = ((CraftPlayer)p).getHandle().playerConnection;
		//connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, (EntityPlayer)entityPlayer));
		//connection.sendPacket(new PacketPlayOutEntityDestroy(entityID));
		try {
			//new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, (EntityPlayer)entityPlayer)) [unregister w/ client]
			Object removePlayerEnum = getNMSClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction").getField("REMOVE_PLAYER").get(null); //$ means sub-class
			Constructor<?> packetPlayOutCon = getNMSClass("PacketPlayOutPlayerInfo").getConstructor(getNMSClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction"), 
					Class.forName("[Lnet.minecraft.server." + getVersion() + ".EntityPlayer;")); //array.toString() makes this

			//Add NPC to array packet (yes, even though w/o reflection you don't use arrays, for reflection you do! can't just cast it because, well, geez
			Object array = Array.newInstance(getNMSClass("EntityPlayer"), 1);
			Array.set(array, 0, entityPlayer);

			//Send packet
			Object packetPlayOut = packetPlayOutCon.newInstance(removePlayerEnum, array);
			sendPacket(p, packetPlayOut);

			//new PacketPlayOutEntityDestroy(entityID); [destroy entity]
			Constructor<?> packetPlayOutEntityCon = getNMSClass("PacketPlayOutEntityDestroy").getDeclaredConstructors()[1];
			//Array of entityID (yes, yet again you need to use arrays for reflection but w/o it you don't have to.
			array = Array.newInstance(int.class, 1);
			Array.set(array, 0, entityID);
			//Send packet
			Object packetPlayOutEntity = packetPlayOutEntityCon.newInstance(array);
			sendPacket(p, packetPlayOutEntity);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchFieldException | SecurityException | NoSuchMethodException | ClassNotFoundException e) {
			e.printStackTrace();
			NPCsPlugin.getInstance().getLogger().warning("Couldn't remove player entity/packet"); 
		} 
	}

	/**
	 * Sends a packet to the player telling them that this NPC has tp'd (sends new location)
	 * {@link} https://wiki.vg/Protocol#Entity_Teleport
	 * @param p The player to send the packet to
	 */
	public void teleport(Player p)
	{
		try
		{
			//new PacketPlayOutEntityTeleport(Entity e)
			Constructor<?> packetPlayOutCon = getNMSClass("PacketPlayOutEntityTeleport").getConstructor(getNMSClass("Entity"));

			//Send packet
			Object packetPlayOut = packetPlayOutCon.newInstance(getNMSClass("Entity").cast(entityPlayer));
			sendPacket(p, packetPlayOut);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException | NoSuchMethodException e) { 
			e.printStackTrace();
			NPCsPlugin.getInstance().getLogger().warning("Couldn't (update) teleport npc for player");
		} 
	}

	private void sendPacket(Player p, Object packet)
	{
		try
		{
			Object handle = p.getClass().getMethod("getHandle").invoke(p);
			Object pc = handle.getClass().getField("playerConnection").get(handle);
			pc.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(pc, packet);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			NPCsPlugin.getInstance().getLogger().warning("Couldn't send a packet!");
		}
	}

	/*
	 * ABSTRACTIONS
	 */

	public abstract void onInteract(Player p);

	/*
	 * GETTERS/SETTERS
	 */

	/**
	 * Get the entity id of this NPC on the server
	 */
	public int getEntityID()
	{
		return entityID;
	}

	/**
	 * Get the id of this NPC in the configuration
	 */
	public String getConfigID()
	{
		return configID;
	}

	public String getName()
	{
		return name;
	}

	public String getSkin()
	{
		return skin;
	}
	
	public String getType()
	{
		if(this instanceof NPCMenu)
			return "menu";
		else if(this instanceof NPCTalk)
			return "talk";
		else
			return "dummy";
	}
	/*
	 * Returns if this NPC is unique and can only exist once at a time in the world
	 */
	public boolean isUnique()
	{
		return unique;
	}
	
	/*
	 * Returns if this NPC is active (in the game)
	 */
	public boolean isActive()
	{
		return active;
	}

	/**
	 * Get the spawn location of this NPC
	 */
	public Location getSpawnLocation()
	{
		return spawnLocation;
	}

	/**
	 * Get the bukkit Entity of this class
	 */
	public Entity getEntity()
	{
		try { return (Entity) entityPlayer.getClass().getMethod("getBukkitEntity").invoke(entityPlayer);} 
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) { e.printStackTrace(); return null;}
	}

	/**
	 * Updates something about the entity
	 * @param entity The entity we are updating (from getEntity()). If null, will get the current entity attached
	 * @param method The method we wish to invoke on the entity
	 * @param value The new value (if applicable) we are setting
	 */
	public void updateEntity(@Nullable Entity entity, String method, Object value)
	{
		try 
		{

			entityPlayer.getClass().getMethod("setLocation", double.class, double.class, double.class, float.class, float.class).invoke(entityPlayer, 
					((Location)value).getX(), ((Location)value).getY(), ((Location)value).getZ(), ((Location)value).getYaw(), ((Location)value).getPitch());

			/*	entityPlayer.getClass().getMethod("setLocation", double.class, double.class, double.class, float.class, float.class).invoke(entityPlayer, 
					((Location)value).getX(), ((Location)value).getY(), ((Location)value).getZ(), ((Location)value).getYaw(), ((Location)value).getPitch()); */
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}



	/**
	 * Updates the worldid (entityId) field in npcs.yml using the npc id. You SHOULD NOT call this method from anywhere but in NPCBase. I only did it once as a work around for /reload
	 */
	public void updateConfigEntityId(String configID, int entityID)
	{
		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				File npcFile = new File("plugins/NPCs/npcs.yml");
				FileConfiguration c = YamlConfiguration.loadConfiguration(npcFile);
				final int entity = c.getInt("npcs." + configID + ".entityID");
				if(entity == entityID) //delete
					c.set("npcs." + configID + ".entityID", null);
				else
					c.set("npcs." + configID + ".entityID", entityID);
				try { c.save(npcFile);} catch (IOException e){ e.printStackTrace(); }  
			}
		}.runTaskAsynchronously(NPCsPlugin.getInstance());			
	}

	public static void updateConfigEntityId(int configID, int entityID, boolean delete)
	{
		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				File npcFile = new File("plugins/NPCs/npcs.yml");
				FileConfiguration c = YamlConfiguration.loadConfiguration(npcFile);
				final int entity = c.getInt("npcs." + configID + ".entityID");
				if(entity == 0)
					return;
				if(delete) 
					c.set("npcs." + configID + ".entityID", null);
				else
					c.set("npcs." + configID + ".entityID", entityID);
				try { c.save(npcFile);} catch (IOException e){ e.printStackTrace(); } 
			}
		}.runTaskAsynchronously(NPCsPlugin.getInstance());	
	}

	public void setSkin(String id, String _texture, String _signature)
	{
		skin = id;
		texture = _texture;
		signature = _signature;
		profile.getProperties().put("textures", new Property("textures", texture, signature));
	}
	
	public void setLocation(Location loc)
	{
		spawnLocation = loc;
	}

	/*
	 * REFLECTION GETTERS
	 */

	private Class<?> getNMSClass(String name)
	{
		try
		{
			return Class.forName("net.minecraft.server." + getVersion() + "." + name);
		}
		catch(ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	private Class<?> getCraftbukkitClass(String name)
	{
		try
		{
			return Class.forName("org.bukkit.craftbukkit." + getVersion() + "." + name);
		}
		catch(ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	private String getVersion()
	{
		//net.	minecraft.	server.	v1_15_R1
		return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
	}	
}