package com.github.siralpega.NPCs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.github.siralpega.NPCs.Types.NPCBase;
import com.github.siralpega.NPCs.Types.NPCDummy;
import com.github.siralpega.NPCs.Types.NPCMenu;
import com.github.siralpega.NPCs.Types.NPCTalk;

public class NPCsPlugin extends JavaPlugin implements CommandExecutor
{
	//TODO
	//TODO: Have them be affected by gravity, maintain eye contact with a player, and also have the option to take damage.
	//TODO: Fine tune buy and sell event in GUIEvent and create command event
	//TODO: optimize and make error catches for new configuration cache

	//BACKBURNER:
	//TODO: command to display info of the npc to click on / are looking at  ex: /npc info -> right click -> displays info OR /npc info <config id>
	//TODO: json?

	//Packet list: https://wiki.vg/Protocol

	private static NPCsPlugin instance;
	private Map<Integer, NPCBase> npcsActive;
	private Map<String, NPCBase> npcsConfig;

	@Override
	public void onEnable()
	{
		instance = this;
		npcsActive = new HashMap<Integer, NPCBase>();
		npcsConfig = new HashMap<String, NPCBase>();

		//Commands
		CommandNPC cNPC= new CommandNPC(this);
		getCommand("npc").setExecutor(cNPC);
		getCommand("npc").setTabCompleter(new CmdTabComplete(this));

		CommandSkin cSkin = new CommandSkin(this);
		getCommand("skin").setExecutor(cSkin);

		//Listeners
		getServer().getPluginManager().registerEvents(new NPCInteract(this), this);	
		getServer().getPluginManager().registerEvents(new ShowHideListener(this), this);	

		//Config Defaults & Spawn NPCs on server start
		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				File sf = new File("plugins/NPCs/skins.yml");
				FileConfiguration skins = YamlConfiguration.loadConfiguration(sf);
				skins.addDefault("default.texture", "ewogICJ0aW1lc3RhbXAiIDogMTYwMjA0NDk4MTQwMSwKICAicHJvZmlsZUlkIiA6ICJiZjQ5MDcyYWI1MzA0YTM5YmFhNjQwZjgyMmIzYzcxYSIsCiAgInByb2ZpbGVOYW1lIiA6ICJBbHBlZ2FfIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzY2NmViOGNkMzBlZTUyN2Y0NzdmOTZlYmIxYWI4NTdkNjZhZDRmMjY5NWMxZGI5N2RjOTI3YTUyMjkwZDRmZDAiCiAgICB9CiAgfQp9");
				skins.addDefault("default.signature", "LIpzkN2m7ebmGwLB9P/mB+XjDcLZSSaf0D0eEvYmx1L7l2OaC5+6k9QS0xRcVUIa8+rrvm/vhm/0590n5I+lzZNrCPR0R2aR/uvkJidSkmETugLftrbOlemGnPjVxEmmxTVvKB+BilyZxIYKn5xyK3t782NDmmqvUrEEdYBMRt2cUklJCfz38BuCeTmqj3IfjEvKPb0Km8ZHfqcMr+ElXIEB5bR84aJlgAUtn+0ec5hZ9RSouGvzw8qoBLjVDvD+oOWUAgUNI3kCDqhVFCx0W0IQM+OQ5v2Hs5LyZKAs6crWY5o6e2shYeDwytfhJKySzAreoEW0OgwlxqDIdAFD7wroifrbR7SVYrzjVL/c5yYt3+Ao9Etos7IHyVunVyKN66OMoMgfFgxoJuUIavKQ/bIOWvkrFgoA6+91b+wHAdyXIjWdJYXfTLKv+gislOwH2bzBYSVhQpnkBPRInzQXC/7YbAmh5lf5fTBZss5ziKQcwfrLku5UMNkKUw7Ch23snklUppalFX7X7Vsl66oxAxmuWy9nqNU7KgKBK+dBQYDGlWYieHzKtvYOCEO2aDPecZ++LC3V+EilxwEUl4pQoHPtcp8nXFLYcSHzEQ/dpleCiZpMW5aWb/2F4Dj8nG9zqWIZCKv3hvjnOBYVShM5ujR4mDUazlkqNb0fTfFhqM8="); 
				skins.addDefault("default.uuid", "bf49072ab5304a39baa640f822b3c71a");
				skins.options().copyDefaults(true);
				try {skins.save(sf);} catch (IOException e) {e.printStackTrace();}

				HashMap<String, String> startNPCs = new HashMap<String, String>();
				List<String> startOnSpawn = new ArrayList<String>();
				File file = new File("plugins/NPCs/npcs.yml");
				FileConfiguration c = YamlConfiguration.loadConfiguration(file);
				String path = "npcs.";
				if(c.getConfigurationSection("npcs") == null)
					return;
				for(String npc : c.getConfigurationSection("npcs").getKeys(false))
				{
					if(c.getBoolean(path + npc + ".spawnOnStart") == true)
						startOnSpawn.add(npc);
					startNPCs.put(npc, (c.getString(path + npc + ".type") != null) ? c.getString(path + npc + ".type") : "dummy");
				}
				new BukkitRunnable()
				{
					@Override
					public void run()
					{
						NPCBase entity;
						for(String id : startNPCs.keySet())
						{
							switch (startNPCs.get(id))
							{
							case "menu":
								entity = new NPCMenu(id); break;
							case "talk":
								entity = new NPCTalk(id); break;
							default: 
								entity = new NPCDummy(id); break;
							}
							if(startOnSpawn.contains(id))
							{
								entity.spawn();
								for(Player p : Bukkit.getOnlinePlayers())
									entity.show(p);
							}
							npcsConfig.put(id, entity);
						}
					}
				}.runTask(instance);
			}
		}.runTaskAsynchronously(this);
	}


	@Override
	public void onDisable()
	{

		File npcFile = new File("plugins/NPCs/npcs.yml");
		FileConfiguration c = YamlConfiguration.loadConfiguration(npcFile);
		/*	int size = c.getInt("size");
		for(int i = 1; i <= size; i++)
			c.set("npcs." + i + ".entityID", null); */

		for(String str : c.getConfigurationSection("npcs").getKeys(false))
			c.set("npcs." + str + ".entityID", null); 

		try { c.save(npcFile); } catch (IOException e){ e.printStackTrace(); } 

		for(NPCBase npc : npcsActive.values())
			for(Player p : Bukkit.getOnlinePlayers())
				npc.hide(p);
		getLogger().info("Finished shutdown");
	}

	public static NPCsPlugin getInstance()
	{
		return instance;
	}

	public Map<Integer, NPCBase> getNpcs()
	{
		return npcsActive;
	} 
	
	public Map<String, NPCBase> getConfigNpcs()
	{
		return npcsConfig;
	}

	public boolean containsNPC(String configID)
	{
		return npcsConfig.containsKey(configID);
	}

	public NPCBase getNPC(String configID)
	{
		return npcsConfig.get(configID);
	}

	public NPCBase getActiveNPC(int entityID)
	{
		return npcsActive.get(entityID);
	}

	public void addNPC(NPCBase npc, int entityID)
	{
		npcsActive.put(entityID, npc);
		if(npc.getConfigID() != null) //to update the cache with any new data from file
			npcsConfig.put(npc.getConfigID(), npc);
	}
	
	public void addNPCtoConfig(String id, String type)
	{
		NPCBase entity;
		switch (type)
		{
		case "menu":
			entity = new NPCMenu(id); break;
		case "talk":
			entity = new NPCTalk(id); break;
		default: 
			entity = new NPCDummy(id); break;
		}
		npcsConfig.put(id, entity);
	}
	
	public void removeNPCFromConfig(String id)
	{
		npcsConfig.remove(id);
	}

	public void removeNPC(NPCBase npc, int entityID)
	{
		npcsActive.remove(entityID);
	}
}
