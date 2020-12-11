package com.github.siralpega.NPCs.Types;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/*
 * A NPC for stading still and looking pretty
 */
public class NPCDummy extends NPCBase
{
	public NPCDummy(String configID) { super(configID);}
	public NPCDummy(Location playerLocation, String name, String texture, String signature) {super(playerLocation, name, texture, signature);}

	@Override
	public void onInteract(Player p) {	
		
	}

}
