package com.github.siralpega.NPCs.Types;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

/*
 * A NPC for chatting
 */
public class NPCTalk extends NPCBase
{
	public NPCTalk(Location playerLocation, String name, String texture, String signature) {super(playerLocation, name, texture, signature);}
	public NPCTalk(String configID) 
	{ 
		super(configID);
	}

	@Override
	public void onInteract(Player p) {	
		//dialogue with player
		p.sendMessage(ChatColor.GREEN + "Hello. This is a placeholder message.");
	}

}
