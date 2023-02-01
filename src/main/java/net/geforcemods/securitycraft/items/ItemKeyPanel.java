package net.geforcemods.securitycraft.items;

import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.api.IPasswordConvertible;
import net.geforcemods.securitycraft.api.SecurityCraftAPI;
import net.geforcemods.securitycraft.misc.SCSounds;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemKeyPanel extends ItemBlock {
	public ItemKeyPanel() {
		super(SCContent.keyPanelFloorCeilingBlock);
	}

	@Override
	public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);
		IBlockState state = world.getBlockState(pos);

		for (IPasswordConvertible pc : SecurityCraftAPI.getRegisteredPasswordConvertibles()) {
			if (pc.isValidStateForConversion(state)) {
				if (pc.convert(player, world, pos)) {
					if (!player.capabilities.isCreativeMode)
						stack.shrink(1);

					world.playSound(player, pos, SCSounds.LOCK.event, SoundCategory.BLOCKS, 1.0F, 1.0F);
					return EnumActionResult.SUCCESS;
				}
			}
		}

		//respect replaceable blocks when trying placing the key panel
		if (state.getBlock().isReplaceable(world, pos)) {
			pos = pos.offset(facing.getOpposite());
			state = world.getBlockState(pos);
		}

		if (state.isSideSolid(world, pos, facing)) {
			IBlockState stateToPlace;
			BlockPos placeAt = pos.offset(facing);
			IBlockState stateAtPlacePosition = world.getBlockState(placeAt);

			if (player.canPlayerEdit(placeAt, facing, stack) && stateAtPlacePosition.getBlock().isReplaceable(world, placeAt)) {
				if (facing.getAxis() == Axis.Y)
					stateToPlace = SCContent.keyPanelFloorCeilingBlock.getStateForPlacement(world, placeAt, facing, hitX, hitY, hitZ, 0, player, hand);
				else
					stateToPlace = SCContent.keyPanelWallBlock.getStateForPlacement(world, placeAt, facing, hitX, hitY, hitZ, 0, player, hand);

				if (stateToPlace != null) {
					SoundType soundType = stateToPlace.getBlock().getSoundType(stateToPlace, world, placeAt, player);

					world.setBlockState(placeAt, stateToPlace);
					world.playSound(player, placeAt, soundType.getPlaceSound(), SoundCategory.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
					ItemBlock.setTileEntityNBT(world, player, placeAt, stack);

					if (player instanceof EntityPlayerMP)
						CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, placeAt, stack);

					if (!player.isCreative())
						stack.shrink(1);

					stateToPlace.getBlock().onBlockPlacedBy(world, placeAt, stateToPlace, player, stack);
					return EnumActionResult.SUCCESS;
				}
			}
		}

		return EnumActionResult.FAIL;
	}
}