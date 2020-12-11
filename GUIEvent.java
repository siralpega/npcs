package com.github.siralpega.NPCs;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.github.siralpega.NPCs.Types.NPCMenu;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class GUIEvent implements Listener
{
	Inventory inv;
	final String npcConfigId; //the config id associated with the npc that owns the inventory
	final boolean canModifyInventory;

	public GUIEvent(Inventory i, String config)
	{
		this(i, config, false);
	}

	public GUIEvent(Inventory i, String config, boolean canModifyInv)
	{
		inv = i;
		npcConfigId = config;
		canModifyInventory = canModifyInv;
	}

	public void setInventory(Inventory _inv)
	{
		inv = _inv;
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e)
	{
		if(e.getInventory() != this.inv)
			return;
		if(e.getClickedInventory() != this.inv) //allows the player's inventory to be clicked
		{
			if((e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY || e.getAction() == InventoryAction.COLLECT_TO_CURSOR ) && !canModifyInventory)
				e.setCancelled(true);
			return;
		}
		if(!canModifyInventory)
			e.setCancelled(true);


		if(e.getCurrentItem() != null)
		{
			ItemStack clickedItem = new ItemStack(e.getCurrentItem()); //The item that was clicked, WITHOUT custom persistent data (a clone that we can use to give to the player)

			PersistentDataContainer container = e.getCurrentItem().getItemMeta().getPersistentDataContainer();
			NamespacedKey actionKey = getKeyThenRemoveFromItem(NPCMenu.INTERACT_ACTION, clickedItem);
			if(container.has(actionKey, PersistentDataType.STRING))
			{
				String action = container.get(actionKey, PersistentDataType.STRING);

				//ITEM BUYING/SELLING
				//TODO: get first letter of each word capitalized 
				//TODO: add a tag/container saying "bought from this npc" ?
				if(action.equals(NPCMenu.INTERACT_ACTION_SELL) || action.equals(NPCMenu.INTERACT_ACTION_BUY))
				{
					removeActionText(clickedItem);
					NamespacedKey priceKey = getKeyThenRemoveFromItem(NPCMenu.INTERACT_ATTR_PRICE, clickedItem);
					String item = container.get(priceKey, PersistentDataType.STRING);
					ItemStack price = new ItemStack(Material.valueOf(item.substring(item.indexOf(" ") + 1)), Integer.parseInt(item.substring(0, item.indexOf(" "))));

					if(action.equals(NPCMenu.INTERACT_ACTION_SELL)) //npc -> player
						buyOrSellItemEvent(clickedItem, price, (Player)e.getWhoClicked());	
					else if(action.equals(NPCMenu.INTERACT_ACTION_BUY)) //player -> npc
						buyOrSellItemEvent(price, clickedItem, (Player)e.getWhoClicked());	
				}
				//COMMANDS

			}


		}
		//e.getView().close();
	}

	private void buyOrSellItemEvent(ItemStack toPlayerItem, ItemStack fromPlayerItem, Player p)
	{
		String toPlayerItemName = (toPlayerItem.getItemMeta().hasDisplayName() ? toPlayerItem.getItemMeta().getDisplayName() : toPlayerItem.getType().name().replace("_", " ").toLowerCase());
		String fromPlayerItemName= (fromPlayerItem.getItemMeta().hasDisplayName() ? fromPlayerItem.getItemMeta().getDisplayName() : fromPlayerItem.getType().name().replace("_", " ").toLowerCase());
		TextComponent toHoverText = new TextComponent(ChatColor.GOLD + "[" + toPlayerItemName + "]"), 
				fromHoverText = new TextComponent(ChatColor.GOLD + "[" + fromPlayerItemName + "]"),
				message = new TextComponent(ChatColor.GREEN + "You got " + ChatColor.GOLD + toPlayerItem.getAmount() + " ");
		String toLore = ChatColor.DARK_PURPLE + "", fromLore = ChatColor.DARK_PURPLE + "";
		if(toPlayerItem.getItemMeta().hasLore())
		{
			if(toPlayerItem.getItemMeta().hasLore())
				for(String str : toPlayerItem.getItemMeta().getLore())
					toLore += "\n" + ChatColor.ITALIC + str;
			if(fromPlayerItem.getItemMeta().hasLore())
				for(String str : fromPlayerItem.getItemMeta().getLore())
					fromLore += "\n" + ChatColor.ITALIC + str;	
		}
		toHoverText.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new Text(toPlayerItemName + toLore)));
		fromHoverText.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new Text(fromPlayerItemName + fromLore)));

		//Check if player has the price
		if(p.getInventory().containsAtLeast(fromPlayerItem, fromPlayerItem.getAmount()))
		{
			p.getInventory().removeItem(fromPlayerItem);
			p.getInventory().addItem(toPlayerItem); 

			message.addExtra(toHoverText);
			message.addExtra(ChatColor.GREEN + " for " + ChatColor.GOLD + fromPlayerItem.getAmount() + " ");
		}
		else
			message = new TextComponent(ChatColor.LIGHT_PURPLE + "You don't have " + ChatColor.GOLD + fromPlayerItem.getAmount() + " ");
		message.addExtra(fromHoverText);
		p.spigot().sendMessage(message);
	}

	@SuppressWarnings("unused")
	private void runCommandEvent(Player p)
	{

	}

	private void removeActionText(ItemStack item) //item must be a clone or copy of the event's item.
	{
		ItemMeta im = item.getItemMeta();
		List<String> lore = im.getLore();
		if(lore.size() > 0)
			lore.remove(0); //assume that the action text will always be the first line. is this dangerous? maybe, but NPCMenu hard-codes action to be first line right now.
		im.setLore(lore);
		item.setItemMeta(im);
	}

	private NamespacedKey getKeyThenRemoveFromItem(String key, ItemStack item) //item must be a clone or copy of the event's item.
	{
		NamespacedKey _key = getKey(key);
		ItemMeta clickedItemMeta = item.getItemMeta();
		clickedItemMeta.getPersistentDataContainer().remove(_key);
		item.setItemMeta(clickedItemMeta);
		return _key;
	}

	private NamespacedKey getKey(String key)
	{
		NamespacedKey _key = new NamespacedKey(NPCsPlugin.getInstance(), key);
		return _key;
	}

	@EventHandler
	public void InventoryCloseEvent(InventoryCloseEvent event)
	{

	}
}
