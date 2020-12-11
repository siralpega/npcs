package com.github.siralpega.NPCs;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.github.siralpega.NPCs.Types.NPCBase;
import com.github.siralpega.NPCs.Types.NPCDummy;
import com.github.siralpega.NPCs.Types.NPCMenu;
import com.github.siralpega.NPCs.Types.NPCTalk;
import com.github.siralpega.util.packets.MyPacketLib;

import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.v1_16_R2.EnumHand;
import net.minecraft.server.v1_16_R2.PacketPlayInUseEntity.EnumEntityUseAction;
//https://www.spigotmc.org/threads/left-click-entity-event.285712/
public class NPCInteract implements Listener
{
	private NPCsPlugin npcPlugin;
	private static NPCInteract npcInteract;
	private ProtocolManager protocolManager;
	private MyPacketLib packetManager;

	public NPCInteract(NPCsPlugin plugin)
	{
		this.npcPlugin = plugin;
		npcInteract = this;
		protocolManager = ProtocolLibrary.getProtocolManager();
		packetManager = new MyPacketLib();
		register();
	}

	public void register()
	{
		//RIGHT CLICK
		protocolManager.addPacketListener(new PacketAdapter(npcPlugin,
				ListenerPriority.NORMAL,
				PacketType.Play.Client.USE_ENTITY) {
			@Override
			public void onPacketReceiving(PacketEvent event) {
				if (event.getPacketType() == PacketType.Play.Client.USE_ENTITY)
				{
					int targetId = packetManager.getEntityID(event.getPacket());
					if(npcPlugin.getActiveNPC(targetId) == null)
						return;

					//TODO: The inventory for the map could be created on enable from a yml file?
					if(packetManager.getPlayerInteractHand(event.getPacket()) == EnumHand.MAIN_HAND && (packetManager.getActionType(event.getPacket()) == EnumEntityUseAction.INTERACT 
							|| packetManager.getActionType(event.getPacket()) == EnumEntityUseAction.ATTACK))
					{
						new BukkitRunnable()
						{
							@Override
							public void run()
							{
								interact(event.getPlayer(), targetId);
							}
						}.runTask(plugin);
					}
				}
			}
		});
		//OTHER EVENT GOES BELOW HERE

	}

	public void interact(Player p, int target)
	{
		NPCBase npc = npcPlugin.getActiveNPC(target);
		if(npc == null)
		{
			p.sendMessage(ChatColor.RED + " Error: Couldn't find the npc with an entity id of " + target);
			return;
		}
		if(npc instanceof NPCMenu)
			((NPCMenu)npc).onInteract(p);
		else if(npc instanceof NPCTalk)
			((NPCTalk)npc).onInteract(p);	
		else if(npc instanceof NPCDummy)
			((NPCDummy)npc).onInteract(p);	//this call to dummy is useless. remove it?

	}

	public static NPCInteract getInstance()
	{
		return npcInteract;
	}
}
