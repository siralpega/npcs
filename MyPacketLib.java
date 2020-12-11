package com.github.siralpega.util.packets;

import java.util.ArrayList;
import java.util.List;

import com.comphenix.protocol.events.PacketContainer;
@SuppressWarnings("unused")
//NOTE: Requires ProtocolLib
public class MyPacketLib 
{
	private int getPacketFieldSize(PacketContainer p)
	{
		return p.getModifier().size();
	}

	private List<String> getPacketFieldTypes(PacketContainer p)
	{
		List<String> typeNames = new ArrayList<String>();
		for(int i = 0; i < p.getModifier().getFields().size(); i++)
			typeNames.add(p.getModifier().getFields().get(i).getType().getName());
		return typeNames;
	}

	public List<String> getPacketFields(PacketContainer p)
	{
		List<String> names = new ArrayList<String>();
		for(int i = 0; i < p.getModifier().getFields().size(); i++)
			names.add(p.getModifier().getField(i).getName());
		return names;
	}
	
	public Object getPacketValue(PacketContainer p, String field)
	{
		for(int i = 0; i < p.getModifier().getFields().size(); i++)
			if(p.getModifier().getFields().get(i).getName().equalsIgnoreCase(field))
				return p.getModifier().getValues().get(i);
		return null;
	}

	public List<Object> getAllPacketValues(PacketContainer p)
	{
		List<Object> values = new ArrayList<Object>();
		for(int i = 0; i < p.getModifier().getFields().size(); i++)
			values.add(getPacketValue(p, p.getModifier().getField(i).getName()));
		return values;
	}

	public net.minecraft.server.v1_16_R2.EnumHand getPlayerInteractHand(PacketContainer p)
	{
		for(int i = 0; i < p.getModifier().getFields().size(); i++)
		{
			try
			{
				//p.getEnumModifier(net.minecraft.server.v1_15_R1.EnumHand.class, i).getValues().get(0)	
				if(p.getModifier().getValues().get(i) instanceof net.minecraft.server.v1_16_R2.EnumHand)
					return p.getEnumModifier(net.minecraft.server.v1_16_R2.EnumHand.class, i).getValues().get(0);
			}
			catch(Exception e)
			{

			}
		}
		return null;
	}
	
	public net.minecraft.server.v1_16_R2.PacketPlayInUseEntity.EnumEntityUseAction getActionType(PacketContainer p)
	{
		for(int i = 0; i < p.getModifier().getFields().size(); i++)
		{
			try
			{
				if(p.getModifier().getValues().get(i) instanceof net.minecraft.server.v1_16_R2.PacketPlayInUseEntity.EnumEntityUseAction)
					return p.getEnumModifier(net.minecraft.server.v1_16_R2.PacketPlayInUseEntity.EnumEntityUseAction.class, i).getValues().get(0);
			}
			catch(Exception e)
			{
				
			}
		}
		return null;
	}

	public int getEntityID(PacketContainer p)
	{
		return (int) p.getModifier().getValues().get(0);
	}
}
