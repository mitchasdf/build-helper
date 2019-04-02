package com.edmistone.buildhelper.operations;

import java.util.Deque;
import java.util.List;

import com.edmistone.buildhelper.helpers.BlockHelper;
import com.edmistone.buildhelper.helpers.StructureBoundingBoxHelper;
import com.edmistone.buildhelper.operations.Clone.CloneMode;
import com.edmistone.buildhelper.operations.Clone.StaticCloneData;
import com.google.common.collect.Lists;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

/** Contains methods to rotate blocks, entities and other things */
public class Rotate
{
	/** Rotates region of source. Modified version of the clone command method. */
	public static void rotate(BlockPos sourceBlockPosStart, BlockPos sourceBlockPosEnd, EntityPlayer player, World world)
	{
		int offset = 100;
        StructureBoundingBox sourceBoundingBox = StructureBoundingBoxHelper.reduceBoundingBox(new StructureBoundingBox(sourceBlockPosStart, sourceBlockPosEnd));
        StructureBoundingBox destinationBoundingBox = new StructureBoundingBox(
        		sourceBoundingBox.minX,
        		sourceBoundingBox.minY + offset,
        		sourceBoundingBox.minZ,
        		sourceBoundingBox.maxX,
        		sourceBoundingBox.maxY + offset,
        		sourceBoundingBox.maxZ);
        
        int totalBlocks = sourceBoundingBox.getXSize() * sourceBoundingBox.getYSize() * sourceBoundingBox.getZSize();

        if (totalBlocks > 32768)
        {
        	player.addChatMessage(new TextComponentString(TextFormatting.RED + "BUILD TOOL: Build area too large (> 32768 blocks)"));
        	return;
        }
        
        Block block = null;
        int j = -1;
        	
    	if (sourceBoundingBox.minY < 0 || sourceBoundingBox.maxY >= 256 || destinationBoundingBox.minY < 0 || destinationBoundingBox.maxY >= 256)
        {
    		player.addChatMessage(new TextComponentString(TextFormatting.RED + "BUILD TOOL: Out of bounds"));
    		return;
        }

        if (!world.isAreaLoaded(sourceBoundingBox) || !world.isAreaLoaded(destinationBoundingBox))
        {
        	player.addChatMessage(new TextComponentString(TextFormatting.RED + "BUILD TOOL: Out of bounds"));
        	return;
        }

        List<StaticCloneData> fullBlocks = Lists.<StaticCloneData>newArrayList();
        List<StaticCloneData> tileEntityBlocks = Lists.<StaticCloneData>newArrayList();
        List<StaticCloneData> partialBlocks = Lists.<StaticCloneData>newArrayList();
        Deque<BlockPos> oldSourceBlocks = Lists.<BlockPos>newLinkedList();
        BlockPos sourceDestinationDifference = new BlockPos(destinationBoundingBox.minX - sourceBoundingBox.minX, destinationBoundingBox.minY - sourceBoundingBox.minY, destinationBoundingBox.minZ - sourceBoundingBox.minZ);
        
        for (int z = destinationBoundingBox.minZ; z <= destinationBoundingBox.maxZ; ++z)
        {
            for (int y = destinationBoundingBox.minY; y <= destinationBoundingBox.maxY; ++y)
            {
                for (int x = destinationBoundingBox.minX; x <= destinationBoundingBox.maxX; ++x)
                {
                	BlockHelper.deleteBlock(world, new BlockPos(destinationBoundingBox.maxZ-z+destinationBoundingBox.minZ,y,x));
                }
            }
        }
        
        for (int z = sourceBoundingBox.minZ; z <= sourceBoundingBox.maxZ; ++z)
        {
            for (int y = sourceBoundingBox.minY; y <= sourceBoundingBox.maxY; ++y)
            {
                for (int x = sourceBoundingBox.minX; x <= sourceBoundingBox.maxX; ++x)
                {
                	BlockPos currentSourceBlock = new BlockPos(x, y, z);
                    BlockPos currentDestinationBlock = new BlockPos(sourceBoundingBox.maxZ - z + sourceBoundingBox.minZ, y+offset, x);
                    
                    IBlockState currentBlockState = world.getBlockState(currentSourceBlock);

                    if ((currentBlockState.getBlock() != Blocks.AIR) && (block == null || currentBlockState.getBlock() == block && (j < 0 || currentBlockState.getBlock().getMetaFromState(currentBlockState) == j)))
                    {
                        TileEntity tileentity = world.getTileEntity(currentSourceBlock);

                        if (tileentity != null)
                        {
                            NBTTagCompound nbttagcompound = tileentity.writeToNBT(new NBTTagCompound());
                            tileEntityBlocks.add(new StaticCloneData(currentDestinationBlock, currentBlockState, nbttagcompound));
                            oldSourceBlocks.addLast(currentSourceBlock);
                        }
                        else if (!currentBlockState.isFullBlock() && !currentBlockState.isFullCube())
                        {
                            partialBlocks.add(new StaticCloneData(currentDestinationBlock, currentBlockState, (NBTTagCompound)null));
                            oldSourceBlocks.addFirst(currentSourceBlock);
                        }
                        else
                        {
                            fullBlocks.add(new StaticCloneData(currentDestinationBlock, currentBlockState, (NBTTagCompound)null));
                            oldSourceBlocks.addLast(currentSourceBlock);
                        }
                    }
                }
            }
        }

        List<StaticCloneData> allDestinationBlocks = Lists.<StaticCloneData>newArrayList();
        allDestinationBlocks.addAll(fullBlocks);
        allDestinationBlocks.addAll(tileEntityBlocks);
        allDestinationBlocks.addAll(partialBlocks);
        List<StaticCloneData> allDestinationBlocksReversed = Lists.<StaticCloneData>reverse(allDestinationBlocks);

        for (StaticCloneData destinationBlock : allDestinationBlocksReversed)
        {
            TileEntity destinationTileEntity = world.getTileEntity(destinationBlock.pos);

            if (destinationTileEntity instanceof IInventory)
            {
                ((IInventory)destinationTileEntity).clear();
            }
            
            world.setBlockState(destinationBlock.pos, Blocks.BARRIER.getDefaultState(), 2);
        }

        totalBlocks = 0;

        for (StaticCloneData destinationBlock : allDestinationBlocks)
        {
            if (world.setBlockState(destinationBlock.pos, BlockHelper.RotateFacing(destinationBlock.blockState), 2))
            {
                ++totalBlocks;
            }
        }

        for (StaticCloneData destinationTileEntities : tileEntityBlocks)
        {
            TileEntity currentDestinationTileEntity = world.getTileEntity(destinationTileEntities.pos);

            if (destinationTileEntities.nbt != null && currentDestinationTileEntity != null)
            {
                destinationTileEntities.nbt.setInteger("x", destinationTileEntities.pos.getX());
                destinationTileEntities.nbt.setInteger("y", destinationTileEntities.pos.getY());
                destinationTileEntities.nbt.setInteger("z", destinationTileEntities.pos.getZ());
                currentDestinationTileEntity.readFromNBT(destinationTileEntities.nbt);
                currentDestinationTileEntity.markDirty();
            }

            world.setBlockState(destinationTileEntities.pos, destinationTileEntities.blockState, 2);
        }

        for (StaticCloneData currentDestinationBlock : allDestinationBlocksReversed)
        {
            world.notifyNeighborsRespectDebug(currentDestinationBlock.pos, currentDestinationBlock.blockState.getBlock());
        }

        List<NextTickListEntry> sourceBlocksPendingUpdate = world.getPendingBlockUpdates(sourceBoundingBox, false);

        if (sourceBlocksPendingUpdate != null)
        {
            for (NextTickListEntry currentBlockPendingUpdate : sourceBlocksPendingUpdate)
            {
                if (sourceBoundingBox.isVecInside(currentBlockPendingUpdate.position))
                {
                    BlockPos destinationBlockPosition = currentBlockPendingUpdate.position.add(sourceDestinationDifference);
                    world.scheduleBlockUpdate(destinationBlockPosition, currentBlockPendingUpdate.getBlock(), (int)(currentBlockPendingUpdate.scheduledTime - world.getWorldInfo().getWorldTotalTime()), currentBlockPendingUpdate.priority);
                }
            }
        }
        
        for (int z = sourceBoundingBox.minZ; z <= sourceBoundingBox.maxZ; ++z)
        {
            for (int y = sourceBoundingBox.minY; y <= sourceBoundingBox.maxY; ++y)
            {
                for (int x = sourceBoundingBox.minX; x <= sourceBoundingBox.maxX; ++x)
                {
                	BlockHelper.deleteBlock(world, new BlockPos(x,y,z));
                }
            }
        }
        
        sourceBoundingBox = new StructureBoundingBox(sourceBlockPosStart, sourceBlockPosEnd);
        
        BlockPos newSourceBlockPosStart = new BlockPos(sourceBoundingBox.maxZ - sourceBlockPosStart.getZ() + sourceBoundingBox.minZ ,sourceBlockPosStart.getY()+offset, sourceBlockPosStart.getX());
        BlockPos newSourceBlockPosEnd = new BlockPos(sourceBoundingBox.maxZ - sourceBlockPosEnd.getZ() + sourceBoundingBox.minZ,sourceBlockPosEnd.getY()+offset, sourceBlockPosEnd.getX());
        BlockPos destinationBlock = new BlockPos(sourceBoundingBox.minX+1, sourceBoundingBox.minY+1, sourceBoundingBox.minZ+1);
        
        Clone.clone(newSourceBlockPosStart, newSourceBlockPosEnd, destinationBlock, CloneMode.MOVE, player, world, false);
	}
}
