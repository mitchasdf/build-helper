package com.edmistone.buildhelper.items;

import java.util.List;

import javax.annotation.Nullable;

import com.edmistone.buildhelper.Info;
import com.edmistone.buildhelper.operations.Chat;
import com.edmistone.buildhelper.registry.Sounds;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/** Symmetry tool is used to place lines of symmetry in the world
 *  that can be used to build multiple blocks at once 
 *  @author Aaron Edmistone */
public class ItemSymmetryTool extends Item
{
	public ItemSymmetryTool(String registryName)
	{
		super(new Properties().group(Info.TAB));
		this.setRegistryName(new ResourceLocation(Info.MODID, registryName));
	}
	
	public enum SymmetryMode
	{
		None,
		EastWest,
		NorthSouth,
		Both
	}
	
	@Override
	public ActionResult<ItemStack> onItemRightClick (World world, PlayerEntity player, Hand hand)
	{		
		CompoundNBT playerTags = player.getPersistentData();
		int newMode = playerTags.getInt("SymmetryMode") + 1;
		
		if(newMode >= SymmetryMode.values().length)
			newMode = 0;
		
		playerTags.putInt("SymmetryMode", newMode);
		playerTags.putFloat("SymmetryPosX", newMode == 0 ? 0 : MathHelper.floor(player.getPosX()));
		playerTags.putFloat("SymmetryPosY", newMode == 0 ? 0 : MathHelper.floor(player.getPosY()));
		playerTags.putFloat("SymmetryPosZ", newMode == 0 ? 0 : MathHelper.floor(player.getPosZ()));
		
		player.writeUnlessRemoved(playerTags);
		
		if(world.isRemote)
		{
			world.playSound(
					player,
					player.getPosition(),
					Sounds.SYMMETRY_TOOL_USE,
					SoundCategory.PLAYERS,
					0.5F,
					world.rand.nextFloat() * 0.1F + 0.9F);
			
			Chat.send(player, "Symmetry mode set to " + SymmetryMode.values()[newMode].name());
		}
		
		return new ActionResult<ItemStack>(ActionResultType.SUCCESS, player.getHeldItem(hand));
	}
	
	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn)
	{		
		super.addInformation(stack, worldIn, tooltip, flagIn);
		tooltip.add(new StringTextComponent(TextFormatting.AQUA + I18n.format("symmetry_tool.tooltip")));
	}
}