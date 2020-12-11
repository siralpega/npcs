package com.github.siralpega.NPCs;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import com.github.siralpega.NPCs.Types.NPCMenu;

import net.md_5.bungee.api.ChatColor;


public class EditNPCListener implements Listener
{
	final Player p;
	final Inventory inv;
	final String config;
	NPCMenu npc;

	public EditNPCListener(Inventory i, Player player, String _config, NPCMenu _npc)
	{
		inv = i;
		p = player;
		config = _config;
		npc = _npc;
	}

	@EventHandler
	public void InventoryCloseEvent(InventoryCloseEvent event) {
		if (event.getPlayer() == p && event.getInventory() == this.inv)
		{
			org.bukkit.inventory.ItemStack[] contents = event.getInventory().getContents();
			Inventory i = Bukkit.createInventory(null, contents.length, ((npc.getName() != null) ? npc.getName() : "NPC"));
			i.setContents(contents);

			new BukkitRunnable()
			{
				@Override
				public void run()
				{
					File f = new File(NPCsPlugin.getInstance().getDataFolder() + File.separator + "npcs.yml");
					YamlConfiguration c = YamlConfiguration.loadConfiguration(f);
					String path = "npcs." + config + ".inv.items.";
					//		c.set("npcs." + config + ".inv.items", null);

					for(int k = 0; k < i.getSize(); k++)
					{
						ItemStack item = i.getItem(k);
						if(item == null)
						{
							c.set(path + k, null);
							continue;
						}
						String value = item.getType().toString();
						c.set(path + k + ".material", value);
						c.set(path + k + ".amount", item.getAmount());
						ItemMeta meta = item.getItemMeta();
						if(meta.hasDisplayName())
							c.set(path + k + ".name", meta.getDisplayName());
						if(meta instanceof Damageable)
						{
							Damageable d = (Damageable) meta;
							c.set(path + k + ".durability", item.getType().getMaxDurability() - d.getDamage());
						}
						if(meta.hasLore())
						{
							List<String> current = meta.getLore();
							for(int i = 0; i < current.size(); i++)
							if(current.get(i).contains("BUY FOR") || current.get(i).contains("SELL FOR")) //this isn't a good way of doing this but \_O_/
								current.remove(i);
							if(current.size() > 0)
								c.set(path + k + ".lore", current);
						}

						if(!meta.getPersistentDataContainer().isEmpty())
						{
							Iterator<NamespacedKey> it = meta.getPersistentDataContainer().getKeys().iterator();
							while(it.hasNext())
							{
								NamespacedKey key = it.next();
								c.set(path + k + ".interactable." + key.getKey(), meta.getPersistentDataContainer().get(key, PersistentDataType.STRING)); //shouldn't assume that the value will be a string
							}
						}
					}
					try { c.save(f); } catch (IOException e) {e.printStackTrace();}
					new BukkitRunnable()
					{
						@Override
						public void run()
						{
							event.getPlayer().sendMessage(ChatColor.YELLOW + "Inventory updated for NPC with id of " + config);
							npc.updateInventory();
						}
					}.runTask(NPCsPlugin.getInstance());

				}
			}.runTaskAsynchronously(NPCsPlugin.getInstance());	
			org.bukkit.event.inventory.InventoryCloseEvent.getHandlerList().unregister(this);
		}
		else
			org.bukkit.event.inventory.InventoryCloseEvent.getHandlerList().unregister(this);
	}
}
