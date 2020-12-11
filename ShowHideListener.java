package com.github.siralpega.NPCs;

import java.util.Iterator;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.github.siralpega.NPCs.Types.NPCBase;

public class ShowHideListener implements Listener
{
	final NPCsPlugin plugin;

	public ShowHideListener(NPCsPlugin npcPlugin)
	{
		plugin = npcPlugin;
		
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e)
	{
		Iterator<NPCBase> it = plugin.getNpcs().values().iterator();
		Player p = e.getPlayer();
		while(it.hasNext())
			it.next().show(p);
		//TODO: perhaps we don't want to send every NPC? what if the NPC is out of rendering distance? does it matter? will it be destroyed if it is out of render distance?
	}
}
