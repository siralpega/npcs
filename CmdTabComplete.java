package com.github.siralpega.NPCs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import com.github.siralpega.NPCs.Types.NPCBase;
import com.github.siralpega.NPCs.Types.NPCMenu;

public class CmdTabComplete implements TabCompleter
{
	private NPCsPlugin plugin;
	public CmdTabComplete(NPCsPlugin main) {
		plugin = main;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) 
	{
		List<String> options = new ArrayList<String>(), completions = new ArrayList<String>();
		if(args.length == 1)
		{
			if(sender.hasPermission("npcs.base"))
			{
				options.add("spawn");
				options.add("despawn");
				options.add("inv");
				options.add("bring");
				options.add("goto");
				options.add("list");
				options.add("modify");
				options.add("skin");
			}
			if(sender.hasPermission("npcs.create"))
				options.add("create");
			if(sender.hasPermission("npcs.delete"))
				options.add("delete");
			StringUtil.copyPartialMatches(args[0], options, completions);
		}
		else if(args.length == 2)
		{
			if(args[0].equals("bring") || args[0].equals("goto") || args[0].equals("despawn") || args[0].equals("skin") || args[0].equals("skin"))
				for(NPCBase npc : plugin.getNpcs().values())
					options.add((npc.getConfigID() != null) ? npc.getConfigID() : npc.getEntityID() + "");
			else if(args[0].equals("inv"))
			{
				for(NPCBase npc : plugin.getNpcs().values())
					if(npc instanceof NPCMenu)
						options.add(npc.getConfigID());
			}
			else if(!args[0].equals("create") && !args[0].equals("list"))
				options.clear();
			else if(args[0].equals("create") && sender.hasPermission("npcs.create"))	
			{
				options.add("config");
				options.add("menu");
				options.add("talk");
				options.add("dummy");
				options.add("name");
			}
			else if(args[0].equals("list"))	
			{
				options.add("active");
				options.add("inactive");
				options.add("all");
			}
			StringUtil.copyPartialMatches(args[1], options, completions);
		}
		else if(args.length == 3)
		{
			if(args[0].equals("config") && sender.hasPermission("npcs.create"))
			{
				options.add("menu");
				options.add("talk");
				options.add("dummy");
			}
			else if(args[0].equals("modify"))
			{
				options.add("name");
				options.add("unique");
				options.add("canMoveItems");
				options.add("location");
				options.add("spawnOnStart");
			}
			StringUtil.copyPartialMatches(args[2], options, completions);
		}
		Collections.sort(completions);
		return completions;
	}

}