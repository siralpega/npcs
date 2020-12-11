package com.github.siralpega.NPCs;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.ChatColor;

public class CommandSkin  implements CommandExecutor
{
	private NPCsPlugin plugin;
	private final File skinsFile = new File("plugins/NPCs/skins.yml");
	public CommandSkin(NPCsPlugin main)
	{
		plugin = main;
	}
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(args != null && args.length == 3)
		{
			//skin add <id> <uuid | name> or /skin replace <id> <uuid | name>
			if(!args[0].equals("add") && !args[0].equals("replace"))
				sender.sendMessage(ChatColor.RED + "Error: Unknown command " + args[0]);
			else
			{
				boolean replace = args[0].equals("replace");
				new BukkitRunnable()
				{
					@Override
					public void run()
					{
						YamlConfiguration c = YamlConfiguration.loadConfiguration(skinsFile);
						String uuid = null;
						try
						{
							@SuppressWarnings("unused")
							UUID test = UUID.fromString(args[2]);

							uuid = args[2];
						}
						catch(IllegalArgumentException e)
						{
							try
							{
								URL url_0 = new URL("https://api.mojang.com/users/profiles/minecraft/" + args[2]);
								InputStreamReader reader_0 = new InputStreamReader(url_0.openStream());
								uuid = new JsonParser().parse(reader_0).getAsJsonObject().get("id").getAsString();
							}
							catch(IOException e2) { e2.printStackTrace(); plugin.getLogger().warning(ChatColor.RED + "Error: Couldn't get UUID for " + args[2] + "! Check console");}
						}
						if(uuid != null)
						{
							try
							{
								URL url_1 = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
								InputStreamReader reader_1 = new InputStreamReader(url_1.openStream());
								JsonObject textureProperty = new JsonParser().parse(reader_1).getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();
								String texture = textureProperty.get("value").getAsString();
								String signature = textureProperty.get("signature").getAsString();
								if(args[0].equals("add") || (c.contains(args[1]) && replace))
								{
									c.set(args[1] + ".texture", texture);
									c.set(args[1] + ".signature", signature);
									c.set(args[1] + ".uuid", uuid); //where we got the texture and sig from
									try { c.save(skinsFile); } catch (IOException e) { e.printStackTrace(); plugin.getLogger().warning(ChatColor.RED + "Error: Couldn't save skins.yml to create a new skin! Check console"); }
								}
								new BukkitRunnable()
								{
									@Override
									public void run()
									{
										sender.sendMessage(ChatColor.YELLOW + "Set " + ChatColor.AQUA + args[1] + ChatColor.YELLOW + " to the texture and signature of " + ChatColor.AQUA + args[2]);
									}
								}.runTask(plugin);
							}
							catch(IOException e) { e.printStackTrace(); plugin.getLogger().warning(ChatColor.RED + "Error: Couldn't get UUID for " + args[2] + "! Check console");}
						}
					}

				}.runTaskAsynchronously(plugin);
			}
		}
		else
			sender.sendMessage(ChatColor.RED + "Incorrect syntax! /skin <add|replace> <skin id> <uuid | name> ");
		return true;
	}
}
