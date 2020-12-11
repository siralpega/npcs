package com.github.siralpega.NPCs.Types;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import com.github.siralpega.NPCs.GUIEvent;
import com.github.siralpega.NPCs.NPCsPlugin;

import net.md_5.bungee.api.ChatColor;
/*
 * A NPC for GUI interactions (menus, shops) 
 * TODO: maybe separate shops and menus into to separate classes
 */
public class NPCMenu extends NPCBase
{
	private boolean hasInventory = false, canMoveItems = false;
	private final int DEFAULT_INV_SIZE = 9;
	private Inventory inventory;
	private GUIEvent listener;
	public static String INTERACT_ACTION = "action", INTERACT_ACTION_BUY = "buy", INTERACT_ACTION_SELL = "sell", INTERACT_ACTION_CMD = "cmd", INTERACT_ATTR_PRICE = "price", INTERACT_ATTR_VALUE = "value";

	public NPCMenu(String configID) { this(configID, true); }
	public NPCMenu(String configID, boolean createInventory) 
	{
		super(configID);
		File npcFile = new File("plugins/NPCs/npcs.yml");
		FileConfiguration c = YamlConfiguration.loadConfiguration(npcFile);
		if(c.contains("npcs." + configID + ".inv"))
			this.hasInventory = true;
		else if(createInventory)
		{
			this.hasInventory = true;
			inventory = Bukkit.createInventory(null, DEFAULT_INV_SIZE, ((getName() != null) ? getName() : "NPC"));
		}
		this.canMoveItems = c.getBoolean("npcs." + configID + ".canMoveItems");
	}

	@Override
	public void onInteract(Player p) 
	{
		Inventory inv = getInventory();
		if(inv == null)	
			return; //not suppose to have an inventory or GUI, or it isn't set up
		p.openInventory(inv);
		//GUI events are done through listener (GUIEvent.java)
	}

	@Override
	public boolean spawn()
	{
		super.spawn();
		if(hasInventory && inventory == null)
			createInventoryFromConfig();

		NPCsPlugin plugin = NPCsPlugin.getInstance();
		listener = new GUIEvent(inventory, getConfigID(), canModifyInventory());
		plugin.getServer().getPluginManager().registerEvents(listener, plugin);
		return true;
	}

	@Override
	public void despawn()
	{
		super.despawn();
		if(listener != null)
			HandlerList.unregisterAll(listener);
	}

	
	public void updateInventory()
	{
		createInventoryFromConfig();
	}
	
	private void setInventory(Inventory inv)
	{
		inventory = inv;
		listener.setInventory(inventory);
	}

	public Inventory getInventory()
	{
		return inventory;
	}

	public GUIEvent getListener()
	{
		return listener;
	}

	/**
	 * Can the player move items around (take, place, switch slots) in this NPC's inventory. Any movements won't be saved to disk (configuration), just memory (server session)!
	 */
	public boolean canModifyInventory()
	{
		return canMoveItems;
	}

	public boolean hasInventory()
	{
		return hasInventory;
	}

	private void createInventoryFromConfig()
	{
		final NPCsPlugin npcPlugin = NPCsPlugin.getInstance();	
		NPCMenu thisNpc = this;
		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				File f = new File(npcPlugin.getDataFolder() + File.separator + "npcs.yml");
				YamlConfiguration c = YamlConfiguration.loadConfiguration(f);
				String npc = getConfigID();
				/*		ItemStack bottle = new ItemStack(Material.EXPERIENCE_BOTTLE, 2);
					ItemMeta im = bottle.getItemMeta();
					im.setDisplayName("hi");

					bottle.setItemMeta(im);
					c.set("npcs." + npc + ".inv.items.0", bottle);
					ItemStack glass = new ItemStack(Material.GLASS, 5);
					c.set("npcs." + npc + ".inv.items.1", glass);  
					c.save(f); */

				int configSize = c.getInt("npcs." + npc + ".inv.size");
				if(configSize == 0)
				{
					c.set("npcs." + npc + ".inv.size", DEFAULT_INV_SIZE);  
					try { c.save(f); } catch (IOException e) { e.printStackTrace(); NPCsPlugin.getInstance().getLogger().warning(ChatColor.RED + "Error: Couldn't save npc.yml to read the inventory! Check console"); }
				}
				int invSize = (configSize != 0) ? configSize : DEFAULT_INV_SIZE;
				ItemStack[] items = new ItemStack[invSize];
				ItemStack item;
				ItemMeta meta;
				String itemPath = "npcs." + npc + ".inv.items.", itemName, interact;
				Material mat;
				int amount, durability, durabilityPercent;
				List<String> lore;
				//Read from npc.yml file to create an ItemStack
				for(int i = 0; i < invSize; i++) 
				{
					if(!c.contains(itemPath + i, false))
						continue;

					amount = c.getInt(itemPath + i + ".amount");
					if(amount == 0)
						continue;
					try
					{
						mat = Material.valueOf(c.getString(itemPath + i + ".material"));
					}
					catch(IllegalArgumentException e) { Bukkit.getLogger().warning("cannot find Material of " + c.getString(itemPath + i + ".material")); continue;}

					item = new ItemStack(mat, amount);
					meta = item.getItemMeta();
					durability = c.getInt(itemPath + i + ".durability");
					durabilityPercent = c.getInt(itemPath + i + ".durabilityPercent");
					if(meta instanceof Damageable && (durability != 0 || durabilityPercent != 0))
					{
						Damageable d = (Damageable) meta;

						int damage;
						if(durability == 0) //if we want to set it as a percent. 
							damage = mat.getMaxDurability() - (int)(mat.getMaxDurability() * durabilityPercent * 0.01);
						else
							damage = mat.getMaxDurability() - durability;
						d.setDamage(damage);
					}
					itemName = c.getString(itemPath + i + ".name");
					if(itemName != null)
						meta.setDisplayName(itemName);
					//CUSTOM TAGS (SPIGOT NBT)
					interact = c.getString(itemPath + i + ".interactable.action");
					if(interact != null)
					{
						NamespacedKey key = new NamespacedKey(NPCsPlugin.getInstance(), INTERACT_ACTION);
						meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, interact);
						if(interact.equals(INTERACT_ACTION_SELL) || interact.equals(INTERACT_ACTION_BUY))
						{
							String price = c.getString(itemPath + i + ".interactable.price");
							if(price == null)
								price = "5 GOLD_NUGGET";
							key = new NamespacedKey(NPCsPlugin.getInstance(), INTERACT_ATTR_PRICE);
							meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, price);
							String priceLore = ChatColor.RED + "" + ChatColor.BOLD + (interact.equals(INTERACT_ACTION_SELL) ? "BUY FOR " : "SELL FOR ") + ChatColor.GOLD + price;
							List<String> text = new ArrayList<String>();
							text.add(priceLore);
							meta.setLore(text);
						}

					}
					//ITEM LORE
					lore = c.getStringList(itemPath + i + ".lore");
					if(lore != null)
					{
						List<String> current = meta.getLore();
						if(current != null)
							current.addAll(lore);
						else
							current = lore;
						meta.setLore(current);
					}

					item.setItemMeta(meta);
					items[i] = item;
				}

				new BukkitRunnable()
				{
					@Override
					public void run()
					{
						int size = invSize;
						if(size % 9 != 0)
							size =+ size + (9 - (size % 9));

						Inventory inv = Bukkit.createInventory(null, size, ((getName() != null) ? getName() : "NPC"));
						for(int i = 0; i < size; i++)
							if(items[i] != null)
								inv.setItem(i, items[i]);

						thisNpc.setInventory(inv);
					}
				}.runTask(npcPlugin);
			}
		}.runTaskAsynchronously(npcPlugin);
	}
}
