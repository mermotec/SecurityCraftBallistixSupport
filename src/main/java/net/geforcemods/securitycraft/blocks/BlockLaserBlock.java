package net.geforcemods.securitycraft.blocks;

import java.util.Random;

import net.geforcemods.securitycraft.ConfigHandler;
import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.SecurityCraft;
import net.geforcemods.securitycraft.api.IOwnable;
import net.geforcemods.securitycraft.misc.EnumModuleType;
import net.geforcemods.securitycraft.network.client.OpenLaserScreen;
import net.geforcemods.securitycraft.tileentity.TileEntityLaserBlock;
import net.geforcemods.securitycraft.util.BlockUtils;
import net.geforcemods.securitycraft.util.PlayerUtils;
import net.geforcemods.securitycraft.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockLaserBlock extends BlockDisguisable {
	public static final PropertyBool POWERED = PropertyBool.create("powered");

	public BlockLaserBlock(Material material) {
		super(material);
		setSoundType(SoundType.METAL);
	}

	@Override
	public boolean isNormalCube(IBlockState state) {
		return false;
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		TileEntityLaserBlock be = (TileEntityLaserBlock) world.getTileEntity(pos);

		if (!world.isRemote && be.isOwnedBy(player)) {
			if (!be.isEnabled())
				player.sendStatusMessage(Utils.localize("gui.securitycraft:scManual.disabled"), true);
			else
				SecurityCraft.network.sendTo(new OpenLaserScreen(pos, be.getSideConfig()), (EntityPlayerMP) player);
		}

		return true;
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase entity, ItemStack stack) {
		super.onBlockPlacedBy(world, pos, state, entity, stack);

		if (!world.isRemote)
			setLaser(world, pos, entity instanceof EntityPlayer ? (EntityPlayer) entity : null);
	}

	public void setLaser(World level, BlockPos pos, EntityPlayer player) {
		for (EnumFacing facing : EnumFacing.values()) {
			setLaser(level, pos, facing, player);
		}
	}

	public void setLaser(World world, BlockPos pos, EnumFacing facing, EntityPlayer player) {
		TileEntityLaserBlock thisTe = (TileEntityLaserBlock) world.getTileEntity(pos);

		if (!thisTe.isSideEnabled(facing))
			return;

		int boundType = facing == EnumFacing.UP || facing == EnumFacing.DOWN ? 1 : (facing == EnumFacing.NORTH || facing == EnumFacing.SOUTH ? 2 : 3);

		for (int i = 1; i <= ConfigHandler.laserBlockRange; i++) {
			BlockPos offsetPos = pos.offset(facing, i);
			IBlockState offsetState = world.getBlockState(offsetPos);
			Block offsetBlock = offsetState.getBlock();

			if (!offsetBlock.isAir(offsetState, world, offsetPos) && !offsetBlock.isReplaceable(world, offsetPos) && offsetBlock != SCContent.laserBlock)
				return;
			else if (offsetBlock == SCContent.laserBlock) {
				TileEntityLaserBlock thatTe = (TileEntityLaserBlock) world.getTileEntity(offsetPos);

				if (thisTe.getOwner().owns(thatTe) && thisTe.isEnabled() && thatTe.isEnabled()) {
					if (!thatTe.isSideEnabled(facing.getOpposite())) {
						thisTe.setSideEnabled(facing, false, null);
						return;
					}

					EnumModuleType failedType = thisTe.synchronizeWith(thatTe);

					if (failedType != null) {
						if (player != null) {
							PlayerUtils.sendMessageToPlayer(player, Utils.localize(this), Utils.localize("messages.securitycraft:laser.sync_failed", Utils.getFormattedCoordinates(thatTe.getPos()), Utils.localize(failedType.getTranslationKey())), TextFormatting.RED);
							thisTe.setSideEnabled(facing, false, null);
							thatTe.setSideEnabled(facing.getOpposite(), false, null);
							player.closeScreen();
						}

						return;
					}

					for (int j = 1; j < i; j++) {
						offsetPos = pos.offset(facing, j);
						offsetState = world.getBlockState(offsetPos);

						if (offsetState.getBlock().isAir(offsetState, world, offsetPos) || offsetState.getBlock().isReplaceable(world, offsetPos)) {
							world.setBlockState(offsetPos, SCContent.laserField.getDefaultState().withProperty(BlockLaserField.BOUNDTYPE, boundType));

							TileEntity te = world.getTileEntity(offsetPos);

							if (te instanceof IOwnable)
								((IOwnable) te).setOwner(thisTe.getOwner().getUUID(), thisTe.getOwner().getName());
						}
					}
				}

				return;
			}
		}
	}

	@Override
	public void onPlayerDestroy(World world, BlockPos pos, IBlockState state) {
		if (!world.isRemote)
			destroyAdjacentLasers(world, pos);
	}

	public static void destroyAdjacentLasers(World world, BlockPos pos) {
		BlockUtils.removeInSequence(SCContent.laserField, world, pos, EnumFacing.VALUES);
	}

	@Override
	public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
		setLaser(world, pos, null);
	}

	@Override
	public boolean canProvidePower(IBlockState state) {
		return true;
	}

	@Override
	public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
		if (state.getValue(POWERED)) {
			TileEntity te = world.getTileEntity(pos);

			if (te instanceof TileEntityLaserBlock && ((TileEntityLaserBlock) te).isModuleEnabled(EnumModuleType.REDSTONE))
				return 15;
		}

		return 0;
	}

	@Override
	public int getStrongPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
		return getWeakPower(state, world, pos, side);
	}

	@Override
	public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
		if (!world.isRemote && state.getValue(POWERED)) {
			world.setBlockState(pos, state.withProperty(BlockLaserBlock.POWERED, false));
			BlockUtils.updateIndirectNeighbors(world, pos, SCContent.laserBlock);
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
		if (state.getValue(POWERED)) {
			double x = pos.getX() + 0.5F + (rand.nextFloat() - 0.5F) * 0.2D;
			double y = pos.getY() + 0.7F + (rand.nextFloat() - 0.5F) * 0.2D;
			double z = pos.getZ() + 0.5F + (rand.nextFloat() - 0.5F) * 0.2D;
			double magicNumber1 = 0.2199999988079071D;
			double magicNumber2 = 0.27000001072883606D;

			world.spawnParticle(EnumParticleTypes.REDSTONE, x - magicNumber2, y + magicNumber1, z, 0.0D, 0.0D, 0.0D);
			world.spawnParticle(EnumParticleTypes.REDSTONE, x + magicNumber2, y + magicNumber1, z, 0.0D, 0.0D, 0.0D);
			world.spawnParticle(EnumParticleTypes.REDSTONE, x, y + magicNumber1, z - magicNumber2, 0.0D, 0.0D, 0.0D);
			world.spawnParticle(EnumParticleTypes.REDSTONE, x, y + magicNumber1, z + magicNumber2, 0.0D, 0.0D, 0.0D);
			world.spawnParticle(EnumParticleTypes.REDSTONE, x, y, z, 0.0D, 0.0D, 0.0D);
		}
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(POWERED, (meta == 1));
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getBlock() != this ? 0 : (state.getValue(POWERED) ? 1 : 0);
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, POWERED);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityLaserBlock();
	}
}