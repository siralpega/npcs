package com.github.siralpega.NPCs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import com.github.siralpega.NPCs.Types.NPCBase;
import com.github.siralpega.NPCs.Types.NPCDummy;
import com.github.siralpega.NPCs.Types.NPCMenu;
import com.github.siralpega.NPCs.Types.NPCTalk;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class CommandNPC implements CommandExecutor
{
	private NPCsPlugin plugin;
	private final File npcFile = new File("plugins/NPCs/npcs.yml");
	public CommandNPC(NPCsPlugin main)
	{
		plugin = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if(args != null && args.length != 0) {
			if(args[0].equals("spawn"))
			{
				if(args.length > 1)
					spawnNPC(sender, args[1]);
				else
					sender.sendMessage(ChatColor.RED + "Incorrect Syntax! /npc spawn <config id>");
				return true;
			}
			if(args[0].equals("create"))
			{
				if(!sender.hasPermission("npcs.create"))
					sender.sendMessage(ChatColor.RED + "You don't have permission");
				else if(args.length > 1)
					createNPC(args, sender);
				else
					sender.sendMessage(ChatColor.RED + "Incorrect syntax! /npc create [config] [menu|talk|dummy] [name]");
				return true;
			}
			if(args[0].equals("inv") && sender instanceof Player)
			{
				if(args.length != 2)
				{
					sender.sendMessage(ChatColor.RED + "Incorrect Syntax! /npc inv <id>");
					return true;
				}
				modifyInventory(args[1], (Player) sender);
				return true;
			}
			if(args[0].equals("bring") && sender instanceof Player)
			{
				if(args.length != 2)
				{
					sender.sendMessage(ChatColor.RED + "Incorrect Syntax! /npc bring <entity id>");
					return true;
				}
				try
				{
					teleport(Integer.parseInt(args[1]), (Player)sender, true); //entity id
				}
				catch(NumberFormatException e)
				{
					teleport(args[1], (Player)sender, true); //config id
				}
				return true;
			}
			if(args[0].equals("goto") && sender instanceof Player)
			{
				if(args.length != 2)
				{
					sender.sendMessage(ChatColor.RED + "Incorrect Syntax! /npc goto <entity id>");
					return true;
				}
				try
				{
					teleport(Integer.parseInt(args[1]), (Player)sender, false); //entity id
				}
				catch(NumberFormatException e)
				{
					teleport(args[1], (Player)sender, false); //config id
				}
				return true;
			}
			if(args[0].equals("despawn"))
			{
				if(args.length != 2)
				{
					sender.sendMessage(ChatColor.RED + "Incorrect Syntax! /npc despawn <config/entity id>");
					return true;
				}
				try
				{
					despawnNPC(Integer.parseInt(args[1]), sender);  //entity id
				}
				catch(NumberFormatException e)
				{ 
					despawnNPC(args[1], sender); //config id
				}

				return true;
			}
			if(args[0].equals("delete"))
			{
				if(!sender.hasPermission("npcs.delete"))
					sender.sendMessage(ChatColor.RED + "You don't have permission");
				else if(args.length == 2)
					deleteNPC(args[1], sender, false);
				else if(args.length == 3 && args[2].equals("--yes"))
					deleteNPC(args[1], sender, true);
				else
					sender.sendMessage(ChatColor.RED + "Incorrect Syntax! /npc delete <config id>");
				return true;
			}
			if(args[0].equals("list"))
			{
				if(args.length > 2)
					sender.sendMessage(ChatColor.RED + "Incorrect syntax! /npc list [active|inactive|all]");
				else
					displayList(args, sender);
				return true;
			}
			if(args[0].equals("modify"))
			{
				if(args.length != 4)
					sender.sendMessage(ChatColor.RED + "Incorrect syntax! /npc modify <config id> <attribute> <value>");
				else
					modifyAttribute(args[1], args[2], args[3], sender); 
				return true;
			}
			if(args[0].equals("skin"))
			{
				if(args.length != 3)
					sender.sendMessage(ChatColor.RED + "Incorrect syntax! /npc skin <npc|entity id> <skin id>");
				else
					applySkin(args, sender); 
				return true;
			}
		}
		//default help
		if(sender instanceof Player)
		{
			sender.sendMessage(ChatColor.RED + "/" + label + " spawn <id> - " + ChatColor.AQUA + " spawns npc with (config) id");
			sender.sendMessage(ChatColor.RED + "/" + label + " inv <id> - " + ChatColor.AQUA + " opens npc inventory to modify");
			sender.sendMessage(ChatColor.RED + "/" + label + " bring <entity id> - " + ChatColor.AQUA + " teleports the npc to you");
			sender.sendMessage(ChatColor.RED + "/" + label + " goto <entity id> - " + ChatColor.AQUA + " teleports you to the npc");
		}
		sender.sendMessage(ChatColor.RED + "/" + label + " create [config] [menu|talk|dummy] [name] - " + ChatColor.AQUA + " creates a npc with a type and name");
		sender.sendMessage(ChatColor.RED + "/" + label + " despawn <config/entity id> - " + ChatColor.AQUA + " despawns npc with (config) id");
		sender.sendMessage(ChatColor.RED + "/" + label + " delete <config id> - " + ChatColor.AQUA + " removes (and despawns if active) the NPC from the config");
		sender.sendMessage(ChatColor.RED + "/" + label + " list [active|inactive|all] - " + ChatColor.AQUA + " displays active/inactive/all NPCs");
		sender.sendMessage(ChatColor.RED + "/" + label + " modify <config id> <attribute> <value> - " + ChatColor.AQUA + " modify an attribute of the NPC");
		sender.sendMessage(ChatColor.RED + "/" + label + " skin <npc|entity id> <skin id> - " + ChatColor.AQUA + " modify the skin of an NPC");
		return true;
	}

	private void showNPC(NPCBase npc, Player p)
	{
		npc.show(p);
	}

	public void showNPCAll(NPCBase npc)
	{
		for(Player p : Bukkit.getOnlinePlayers())
			showNPC(npc, p);
	}

	private void spawnNPC(CommandSender sender, String type, String name) { spawnNPC(sender, "", type, name, false); } 
	private void spawnNPC(CommandSender sender, String configID) { spawnNPC(sender, configID, null, null, true); }
	private void spawnNPC(CommandSender sender, String configID, String type, String name, boolean inConfig)
	{
		if(inConfig)
		{
			NPCBase npc = plugin.getNPC(configID);
			if(npc == null)
			{
				sender.sendMessage(ChatColor.RED + "Error: Tried to spawn an NPC from the config, but couldn't find id of " + configID);
				return;
			}
			if(npc.isUnique() && npc.isActive())
			{
				sender.sendMessage(ChatColor.RED + "Error: " + npc.getName() + " is unique and cannot be spawned while another NPC of that id (" + configID + ") is in the world");
				return;
			}

			if(npc.getSpawnLocation() == null)
			{
				if(sender instanceof Player)
				{
					final Location loc = ((Player) sender).getLocation();
					final String cid = configID;
					npc.setLocation(loc);
					new BukkitRunnable()
					{
						@Override
						public void run()
						{
							YamlConfiguration c = YamlConfiguration.loadConfiguration(npcFile);
							c.set("npcs." + cid + ".location", loc);
							try { c.save(npcFile); } catch (IOException e) { e.printStackTrace(); plugin.getLogger().warning(ChatColor.RED + "Error: Couldn't save npc.yml to create a new NPC! Check console"); }
						}
					}.runTaskAsynchronously(plugin);
					sender.sendMessage(ChatColor.RED + "Warning: " + ChatColor.AQUA + npc.getName() + ChatColor.YELLOW + " (" + configID + ") didn't have a spawn location set in config, it is now set to your current location");
				}
				else
				{
					sender.sendMessage(ChatColor.RED + "Error: " + npc.getName() + " doesn't have a spawn location set.");
					return;
				}
			}

			npc.spawn();
			showNPCAll(npc);
			Location loc = npc.getSpawnLocation();
			if(sender instanceof Player)
			{
				TextComponent firstMsg = new TextComponent("Spawned ");
				firstMsg.setColor(ChatColor.YELLOW);
				TextComponent nameMsg = new TextComponent(npc.getName());
				nameMsg.setColor(ChatColor.AQUA);
				nameMsg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.AQUA + npc.getName() + "\n" + ChatColor.YELLOW + "type: " + ChatColor.AQUA + npc.getType()
				+ "\n" + ChatColor.YELLOW + "configID: " + ChatColor.AQUA + configID + "\n" + ChatColor.YELLOW + "entityID: " + ChatColor.AQUA + ((npc.getEntityID() != 0) ? npc.getEntityID() : "n/a")
				+ "\n" + ChatColor.YELLOW + "unique: " + ChatColor.AQUA + npc.isUnique()
				+ "\n" + ChatColor.YELLOW + "skin: " + ChatColor.AQUA + npc.getSkin())));
				firstMsg.addExtra(nameMsg);
				TextComponent locMsg = new TextComponent(" at (" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + ")");
				firstMsg.addExtra(locMsg);
				((Player)sender).spigot().sendMessage(firstMsg);
			}
			else
				sender.sendMessage(ChatColor.YELLOW + "Spawned " + ChatColor.AQUA + npc.getName() + ChatColor.YELLOW + " at (" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + ")");
		}
		//NOT FROM CONFIG
		else 
		{
			if(sender instanceof Player)
			{
				NPCBase npc; // npc = new NPCTalk(((Player)sender).getLocation(), name, null, null);
				switch (type)
				{
				case "talk":
					npc = new NPCTalk(((Player)sender).getLocation(), name, null, null); break;
				default: 
					npc = new NPCDummy(((Player)sender).getLocation(), name, null, null); break;
				}
				npc.spawn();
				showNPCAll(npc);
				Location loc = npc.getSpawnLocation();
				TextComponent firstMsg = new TextComponent("Spawned ");
				firstMsg.setColor(ChatColor.YELLOW);
				TextComponent nameMsg = new TextComponent(npc.getName());
				nameMsg.setColor(ChatColor.AQUA);
				nameMsg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.AQUA + npc.getName() + "\n" + ChatColor.YELLOW + "type: " + ChatColor.AQUA + type
						+ "\n" + ChatColor.YELLOW + "configID: " + ChatColor.AQUA + "N/A\n" + ChatColor.YELLOW + "entityID: " + ChatColor.AQUA + npc.getEntityID()
						+ "\n" + ChatColor.YELLOW + "unique: " + ChatColor.AQUA + npc.isUnique()
						+ "\n" + ChatColor.YELLOW + "skin: " + ChatColor.AQUA + npc.getSkin())));
				firstMsg.addExtra(nameMsg);
				TextComponent locMsg = new TextComponent(" at (" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + ") [non-config]");
				firstMsg.addExtra(locMsg);
				((Player)sender).spigot().sendMessage(firstMsg);
			}
			else
				sender.sendMessage(ChatColor.RED + "Error: Can't spawn a non-config NPC from the console ");
		}
	}

	private void createNPC(String[] args, CommandSender sender)
	{
		boolean config = false;
		String name = "", type = "";
		if(args[1].equals("config"))  //save, unknown type, no spawn
		{
			config = true;
			if(args.length >= 3 && (args[2].equals("dummy") || args[2].equals("talk") || args[2].equals("menu"))) //save, type, no spawn
				type = args[2];
		}
		else if(args[1].equals("dummy") || args[1].equals("talk") || args[1].equals("menu")) //no save, type, spawn on player
			type = args[1];

		if(args.length >= 2)
		{
			int start = 1;
			if((config && type.isBlank()) || (!config && !type.isBlank()))
				start = 2;
			else if(config && !type.isBlank())
				start = 3;
			for(int a = start; a < args.length; a++)
				name += args[a] + " ";
			name = name.trim();
		}
		if(type.isBlank())
			type = "dummy";
		if(name.isBlank())
			name = "default name";

		if(!config)
		{
			if(sender instanceof Player) //no save, any type, spawn on player
				spawnNPC(sender, type, name);
			else
				sender.sendMessage(ChatColor.RED + "Error: Creating a non-config " + type + " must be from in-game. /npc create [config] [menu|talk|dummy] [name]");
		}
		else
		{
			final String typeOfNpc = type;
			final String nameOfNpc = name;
			new BukkitRunnable()
			{
				@Override
				public void run()
				{
					YamlConfiguration c = YamlConfiguration.loadConfiguration(npcFile);
					final String configID = nameOfNpc.trim().replace(" ", "_") + "_" + typeOfNpc, path = "npcs." + configID + ".";
					final boolean exists = c.contains("npcs." + configID);
					if(!exists)
					{
						//	c.set(path, value);
						c.set(path + "type", typeOfNpc);
						c.set(path + "name", nameOfNpc);
						c.set(path + "unique", true); // b/c i switched to only unique, this should only be true
						if(typeOfNpc.equals("menu"))
							c.set("npcs." + configID + ".canMoveItems", false);

						try { c.save(npcFile); } catch (IOException e) { e.printStackTrace(); plugin.getLogger().warning(ChatColor.RED + "Error: Couldn't save npc.yml to create a new NPC! Check console"); }
					}
					new BukkitRunnable()
					{
						@Override
						public void run()
						{
							if(exists)
								sender.sendMessage(ChatColor.RED + "Error: config id of " + configID + " already exists. Try a modified name, and modify the name attribute after creation.");
							else
							{
								plugin.addNPCtoConfig(configID, typeOfNpc);
								if(sender instanceof Player)
									spawnNPC(sender, configID);
								else
									sender.sendMessage(ChatColor.YELLOW + "Created a config NPC of type " + ChatColor.AQUA + typeOfNpc + ChatColor.YELLOW + " with a name of " + ChatColor.AQUA + nameOfNpc
											+ ChatColor.YELLOW + ". Type /npc spawn " + configID + " to spawn");
							}
						}
					}.runTask(plugin);	

				}
			}.runTaskAsynchronously(plugin);
		}
	}

	/*
	 * Despawns an NPC in the world with a config or entity id of id
	 */
	private void despawnNPC(Object id, CommandSender sender)
	{

		final boolean isConfigID = (id instanceof String);
		NPCBase npc = null;
		if(!isConfigID) //id is an entity id
			npc = plugin.getActiveNPC((Integer)id); 
		else  //id is a config id.
			npc = plugin.getNPC((String) id); 
		if(npc == null)
		{
			sender.sendMessage(ChatColor.RED + "The entity with a config or entity id of " + id + " isn't in the world");
			return;
		}

		npc.despawn();
		sender.sendMessage(ChatColor.AQUA + npc.getName() + ChatColor.YELLOW + " has been despawned and the entity was destroyed");
	}

	/*
	 * Deletes a NPC from the config and any active NPCs with that config id from the world
	 */
	private void deleteNPC(String configID, CommandSender sender, boolean confirmation)
	{
		if(!confirmation)
		{
			NPCBase npc = plugin.getNPC(configID);
			if(!(sender instanceof Player))
				sender.sendMessage("Are you sure you want to delete " + ((npc != null) ? npc.getName() : "the npc with id of " + configID) + "? If yes, run: /npc delete " + configID + " --yes");
			else
			{
				TextComponent message = new TextComponent(ChatColor.RED + "[!]" + ChatColor.YELLOW + "Are you sure you want to delete " + ChatColor.AQUA + 
						((npc != null) ? npc.getName() : "the npc with config id of " + configID) + ChatColor.YELLOW + " from the config and server?");
				TextComponent confirmMessage = new TextComponent(ChatColor.GREEN + "[YES]");
				confirmMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/npc delete " + configID + " --yes"));
				message.addExtra(confirmMessage);
				((Player)sender).spigot().sendMessage(message);
			}
		}
		else
		{
			new BukkitRunnable()
			{
				@Override
				public void run()
				{
					YamlConfiguration c = YamlConfiguration.loadConfiguration(npcFile);
					if(configID != null)
						c.set("npcs." + configID, null);
					try { c.save(npcFile); } catch (IOException e) { e.printStackTrace(); plugin.getLogger().warning(ChatColor.RED + "Error: Couldn't save npc.yml to create a new NPC! Check console"); }

					new BukkitRunnable()
					{
						@Override
						public void run()
						{
							final NPCBase npc = plugin.getNPC(configID);
							sender.sendMessage(ChatColor.AQUA + npc.getName() + ChatColor.YELLOW + " has been removed from the config");
							despawnNPC(npc.getEntityID(), sender);
							plugin.removeNPCFromConfig(configID);
							plugin.getLogger().info("Removed " + npc.getName() + " (id: " + configID + ") from the config");
						}
					}.runTask(plugin);	
				}
			}.runTaskAsynchronously(plugin);
		}
	}

	private void modifyInventory(String configID, Player p) 
	{
		NPCBase base = (NPCMenu) plugin.getNPC(configID);
		if(base == null || !(base instanceof NPCMenu))
		{
			if(base == null)
				p.sendMessage(ChatColor.RED + "Error: NPC (" + configID + ") isn't active in the world");
			else
				p.sendMessage(ChatColor.RED + "Error: NPC (" + configID + ") isn't of type NPCMenu (no inventory)");
		}	
		else
		{
			NPCMenu npc = (NPCMenu) base;
			if(!npc.hasInventory())
				p.sendMessage(ChatColor.RED + "Error: NPC (" + configID + ") doesn't have an inventory. Use /npc modify inv true to create an inventory first, then use this (NYI)");
			else if(npc.getInventory() == null)
				p.sendMessage(ChatColor.RED + "Error: couldn't find that inventory. id is " + configID + " and entity is " + npc.getEntityID());  //not suppose to have inventory
			else
			{
				org.bukkit.inventory.ItemStack[] contents = npc.getInventory().getContents();
				Inventory inv = Bukkit.createInventory(null, contents.length, "Edit NPC Inventory");
				inv.setContents(contents);
				p.openInventory(inv);
				plugin.getServer().getPluginManager().registerEvents(new EditNPCListener(inv, p, configID, npc), plugin);	
			}
		}
	} 

	private void teleport(Object id, Player sender, boolean toPlayer)
	{
		NPCBase npc = ((id instanceof String) ? plugin.getNPC((String) id) : plugin.getActiveNPC((int) id));

		if(npc == null)
		{
			sender.sendMessage(ChatColor.RED + "Couldn't find an entity with an entity or config id of " + id);
			return;
		}

		if(toPlayer)
		{
			npc.updateEntity(npc.getEntity(), "teleport", sender.getLocation());
			sender.sendMessage(ChatColor.YELLOW + "Teleported " + ChatColor.AQUA + npc.getName() + ChatColor.YELLOW + " to you");
			//showNPCAll(npc); //alternative: but it will re spawn the npc instead of just teleporting (most costly)
		}
		else
		{
			sender.teleport(npc.getEntity());
			sender.sendMessage(ChatColor.YELLOW + "Teleported you to " + ChatColor.AQUA + npc.getName());
		}

		for(Player p : Bukkit.getOnlinePlayers()) //send packets to each player giving them the updated location of the npc
			npc.teleport(p);
	}

	private void displayList(String[] args, CommandSender sender)
	{
		String amount = "all";
		if(args.length == 2)
			if(args[1].equals("active"))
				amount = "active";
			else if(args[1].equals("inactive"))
				amount = "inactive";
		TextComponent message = new TextComponent(ChatColor.YELLOW + "=== List of " + ChatColor.AQUA + amount + ChatColor.YELLOW + " NPCs === \n");
		List<String> alreadyActive = new ArrayList<String>();
		if(!amount.equals("inactive"))
		{
			Iterator<NPCBase> npcs = plugin.getNpcs().values().iterator();

			TextComponent active = new TextComponent(ChatColor.GREEN + "Active: (" + plugin.getNpcs().values().size() + ") \n"), hover;
			NPCBase npc;
			String type;
			while(npcs.hasNext())
			{
				npc = npcs.next();
				if(npc.getConfigID() != null)
					alreadyActive.add(npc.getConfigID());
				if(npc instanceof NPCMenu)
					type = "menu";
				else if(npc instanceof NPCTalk)
					type = "talk";
				else
					type = "dummy";
				hover = new TextComponent(ChatColor.AQUA + npc.getName());
				
				hover.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.AQUA + npc.getName() + "\n" + ChatColor.YELLOW + "type: " + ChatColor.AQUA + type
						+ "\n" + ChatColor.YELLOW + "configID: " + ChatColor.AQUA + ((npc.getConfigID() != null) ? npc.getConfigID() : "N/A") 
						+ "\n" + ChatColor.YELLOW + "entityID: " + ChatColor.AQUA + npc.getEntityID()
						+ "\n" + ChatColor.YELLOW + "unique: " + ChatColor.AQUA + npc.isUnique()
						+ "\n" + ChatColor.YELLOW + "skin: " + ChatColor.AQUA + npc.getSkin())));
				active.addExtra(hover);
				hover = new TextComponent(ChatColor.YELLOW + "   [goto]");
				hover.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/npc goto " +  npc.getEntityID()));
				active.addExtra(hover);
				hover = new TextComponent(ChatColor.YELLOW + "   [bring]");
				hover.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/npc bring " + npc.getEntityID()));
				active.addExtra(hover);
				if(type.equals("menu"))
				{
					hover = new TextComponent(ChatColor.YELLOW + "   [inv]");
					hover.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/npc inv " + npc.getConfigID()));
					active.addExtra(hover);
				}
				active.addExtra("\n");
			}
			message.addExtra(active);
		}
		if(!amount.equals("active"))
		{
			int size = plugin.getConfigNpcs().size();
			String name, type, unique, skin;
			TextComponent inactive = new TextComponent(ChatColor.RED + "Inactive: (" + (size - alreadyActive.size()) + ") \n"), hover;

			for(String id : plugin.getConfigNpcs().keySet())
				if(!alreadyActive.contains(id))
				{
					NPCBase npc = plugin.getNPC(id);
					name = npc.getName(); type = npc.getType(); unique = (npc.isUnique()) ? "true" : "false"; skin = npc.getSkin();
					hover = new TextComponent(ChatColor.AQUA + ((name != null) ? name : "(config " + id + ")"));
					hover.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.AQUA + ((name != null) ? name : "(config " + id + ")")
							+ "\n" + ChatColor.YELLOW + "type: " + ChatColor.AQUA + ((type != null) ? type : "dummy")
							+ "\n" + ChatColor.YELLOW + "configID: " + ChatColor.AQUA + id 
							+ "\n" + ChatColor.YELLOW + "unique: " + ChatColor.AQUA + ((unique != null) ? unique : "true")
							+ "\n" + ChatColor.YELLOW + "skin: " + ChatColor.AQUA + ((skin != null) ? skin : "default"))));
					inactive.addExtra(hover);
					hover = new TextComponent(ChatColor.YELLOW + "   [spawn]");
					hover.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/npc spawn " + id));
					inactive.addExtra(hover);
					inactive.addExtra("\n");
				}
			message.addExtra(inactive);
			sender.spigot().sendMessage(message);

		}
		else
			sender.spigot().sendMessage(message);
	}

	private void modifyAttribute(String configID, String attribute, Object value, CommandSender sender)
	{
		boolean isAttribute = true;
		NPCBase npc = plugin.getNPC(configID);
		if(npc == null)
		{
			sender.sendMessage(ChatColor.RED + "Couldn't find an entity with a config id of " + configID);
			return;
		}

		if(!attribute.equals("name") && !attribute.equals("unique"))
			isAttribute = false;
		if(attribute.equals("location"))
			if(!(sender instanceof Player))
				isAttribute = false;
			else 
				isAttribute = true;
		else if (attribute.equals("canMoveItems"))
			if(!(npc instanceof NPCMenu))
				isAttribute = false;
			else
				isAttribute = true;
		else if (attribute.equals("spawnOnStart"))
			isAttribute = true;

		if(isAttribute)
		{
			new BukkitRunnable()
			{
				@Override
				public void run()
				{
					YamlConfiguration c = YamlConfiguration.loadConfiguration(npcFile);
					if(attribute.equals("name"))
						c.set("npcs." + configID + "." + attribute, (String)value);
					else if(attribute.equals("unique") || attribute.equals("canMoveItems") || attribute.equals("spawnOnStart"))
						c.set("npcs." + configID + "." + attribute, ((value.equals("true") ? true : false)));
					else if(attribute.equals("location"))
						c.set("npcs." + configID + "." + attribute, ((Player)sender).getLocation());
					try{ c.save(npcFile); } catch (IOException e) {e.printStackTrace();}
					new BukkitRunnable()
					{
						@Override
						public void run()
						{
							NPCBase npc = plugin.getNPC(configID);
							if(npc != null)
							{
								npc.despawn();
								if(npc instanceof NPCMenu) //this is because the cache needs to be update. you should take the out and put in separate method called re-spawn and do a check
									plugin.addNPCtoConfig(configID, "menu");
								else if(npc instanceof NPCTalk)
									plugin.addNPCtoConfig(configID, "talk");
								else
									plugin.addNPCtoConfig(configID, "dummy");


								spawnNPC(sender, configID);
								sender.sendMessage(ChatColor.YELLOW + "Updated " + ChatColor.AQUA + attribute + ChatColor.YELLOW + " to " + ChatColor.AQUA + value + ChatColor.YELLOW + " for " 
										+ ChatColor.AQUA + npc.getName());
							}
						}
					}.runTask(plugin);
				}
			}.runTaskAsynchronously(plugin);
		}
		else
			sender.sendMessage(ChatColor.RED + "Error: " + attribute + " is not a valid attribute for the npc");
	}

	/*
	 * Search for the skin of <uuid / name> in skins.yml . if doesn't exist, get it from mojang sessions and store it in skins.yml . id is optional name of object in file, npc id will apply it to a npc
	 */
	private void applySkin(String[] args, CommandSender sender) 
	{
		new BukkitRunnable()
		{
			File skins = new File("plugins/NPCs/skins.yml");
			YamlConfiguration skinsC = YamlConfiguration.loadConfiguration(skins);
			YamlConfiguration c = YamlConfiguration.loadConfiguration(npcFile);
			@Override
			public void run()
			{
				// /npc skin <npc id or entity id> <id of skin>

				boolean npcExists = c.contains("npcs." + args[1]), skinExists = skinsC.contains(args[2]);
				final String texture = skinsC.getString(args[2] + ".texture"), signature = skinsC.getString(args[2] + ".signature");
				int entity = -1;
				if(skinExists)
				{
					if(!npcExists) //see if entity id
					{
						try { 
							entity = Integer.parseInt(args[1]);
							if(plugin.getActiveNPC(entity) != null)
								npcExists = true;
							else
								entity = -1;
						}
						catch(NumberFormatException e) { }
					}

					if(npcExists && entity == -1)
					{
						c.set("npcs." + args[1] + ".skin", args[2]);
						try { c.save(npcFile); } catch (IOException e) { e.printStackTrace(); plugin.getLogger().warning(ChatColor.RED + "Error: Couldn't save npc.yml to create a new NPC! Check console"); }
					}
				}
				final boolean valid = npcExists;
				final int eID = entity;
				new BukkitRunnable()
				{
					@Override
					public void run()
					{
						if(!skinExists)
							sender.sendMessage(ChatColor.RED + "Couldn't find skin id of " + args[2]);
						else if(!valid)
							sender.sendMessage(ChatColor.RED + "Couldn't find an entity or config id with a id of " + args[1]);
						else 
						{
							NPCBase npc = ((eID != -1) ? plugin.getActiveNPC(eID) : plugin.getNPC(args[1]));
							if(npc != null)
							{
								if(npc.getConfigID() == null)//actually we don't want to spawn a non config npc because they will be recreating it. you can add the option to set the sin in spwnNPC();
									sender.sendMessage(ChatColor.RED + "Error: changing skin of a non-config npc is not yet implemented (soon tm)");
								else
								{
									npc.setSkin(args[2], texture, signature);
									npc.despawn();
									spawnNPC(sender, npc.getConfigID());
									sender.sendMessage(ChatColor.YELLOW + "Updated the skin of " + ChatColor.AQUA + npc.getName() + ChatColor.YELLOW + " to skin id " + ChatColor.AQUA + args[2]);
								}
							}
							else
								sender.sendMessage(ChatColor.YELLOW + "Updated the skin of " + ChatColor.AQUA + args[1] + ChatColor.YELLOW + " to skin id " + ChatColor.AQUA + args[2]);
						}	
					}
				}.runTask(plugin);

			}
		}.runTaskAsynchronously(plugin);
	}
}