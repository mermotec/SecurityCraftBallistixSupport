package net.geforcemods.securitycraft.blocks;

import java.util.List;

import net.geforcemods.securitycraft.api.IModuleInventory;
import net.geforcemods.securitycraft.compat.IOverlayDisplay;
import net.geforcemods.securitycraft.items.ItemModule;
import net.geforcemods.securitycraft.misc.EnumModuleType;
import net.geforcemods.securitycraft.tileentity.TileEntityDisguisable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBreakable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockDisguisable extends BlockOwnable implements IOverlayDisplay {
	public BlockDisguisable(Material material) {
		super(material);
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
		IBlockState actualState = getDisguisedBlockState(world, pos);

		if (actualState != null && actualState.getBlock() != this)
			return actualState.getLightValue(world, pos);
		else
			return super.getLightValue(state, world, pos);
	}

	@Override
	public SoundType getSoundType(IBlockState state, World world, BlockPos pos, Entity entity) {
		IBlockState actualState = getDisguisedBlockState(world, pos);

		if (actualState != null && actualState.getBlock() != this)
			return actualState.getBlock().getSoundType(actualState, world, pos, entity);
		else
			return blockSoundType;
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
		IBlockState actualState = getDisguisedBlockState(world, pos);

		if (actualState != null && actualState.getBlock() != this)
			return actualState.getBoundingBox(world, pos);
		else
			return super.getBoundingBox(state, world, pos);
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
		IBlockState actualState = getDisguisedBlockState(world, pos);

		if (actualState != null && actualState.getBlock() != this)
			return actualState.getCollisionBoundingBox(world, pos);
		else
			return super.getCollisionBoundingBox(state, world, pos);
	}

	@Override
	public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World world, BlockPos pos) {
		IBlockState actualState = getDisguisedBlockState(world, pos);

		if (actualState != null && actualState.getBlock() != this)
			return actualState.getSelectedBoundingBox(world, pos);
		else
			return super.getSelectedBoundingBox(state, world, pos);
	}

	@Override
	public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, Entity entity, boolean isActualState) {
		IBlockState actualState = getDisguisedBlockState(world, pos);

		if (actualState != null && actualState.getBlock() != this)
			actualState.addCollisionBoxToList(world, pos, entityBox, collidingBoxes, entity, true);
		else
			addCollisionBoxToList(pos, entityBox, collidingBoxes, getCollisionBoundingBox(state, world, pos));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public BlockRenderLayer getRenderLayer() {
		return BlockRenderLayer.TRANSLUCENT;
	}

	@Override
	public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
		TileEntity te = world.getTileEntity(pos);

		if (te instanceof IModuleInventory && ((IModuleInventory) te).isModuleEnabled(EnumModuleType.DISGUISE)) {
			ItemStack module = ((IModuleInventory) te).getModule(EnumModuleType.DISGUISE);

			if (!module.hasTagCompound())
				module.setTagCompound(new NBTTagCompound());

			IBlockState disguisedState = NBTUtil.readBlockState(module.getTagCompound().getCompoundTag("SavedState"));

			if (disguisedState != null && disguisedState.getBlock() != Blocks.AIR)
				return disguisedState.getBlockFaceShape(world, pos, face);
			else {
				Block block = ((ItemModule) module.getItem()).getBlockAddon(module.getTagCompound());
				if (block == null)
					return BlockFaceShape.SOLID;
				else
					return block.getDefaultState().getBlockFaceShape(world, pos, face);
			}
		}

		return BlockFaceShape.SOLID;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess world, BlockPos pos, EnumFacing side) {
		if (!(world.getTileEntity(pos) instanceof IModuleInventory))
			return true;

		IModuleInventory te = (IModuleInventory) world.getTileEntity(pos);

		if (te.isModuleEnabled(EnumModuleType.DISGUISE)) {
			ItemStack disguiseModule = te.getModule(EnumModuleType.DISGUISE);

			if (!disguiseModule.hasTagCompound())
				disguiseModule.setTagCompound(new NBTTagCompound());

			IBlockState disguisedState = NBTUtil.readBlockState(disguiseModule.getTagCompound().getCompoundTag("SavedState"));

			if (disguisedState != null && disguisedState.getBlock() != Blocks.AIR) {
				// If this block has a disguise module added with a transparent block inserted.
				if (!disguisedState.isOpaqueCube() || !disguisedState.isFullCube())
					return checkForSideTransparency(world, world.getBlockState(pos.offset(side)), pos.offset(side), side);
			}
			else {
				Block blockToDisguiseAs = ((ItemModule) disguiseModule.getItem()).getBlockAddon(disguiseModule.getTagCompound());

				if (blockToDisguiseAs != null) {
					// If this block has a disguise module added with a transparent block inserted.
					if (!blockToDisguiseAs.getDefaultState().isOpaqueCube() || !blockToDisguiseAs.getDefaultState().isFullCube())
						return checkForSideTransparency(world, world.getBlockState(pos.offset(side)), pos.offset(side), side);
				}
			}
		}

		return true;
	}

	public boolean checkForSideTransparency(IBlockAccess world, IBlockState neighborState, BlockPos neighborPos, EnumFacing side) {
		Block neighborBlock = neighborState.getBlock();

		if (neighborBlock.isAir(neighborState, world, neighborPos))
			return true;

		// Slightly cheating here, checking if the block is an instance of BlockBreakable
		// and a vanilla block instead of checking for specific blocks, since all vanilla
		// BlockBreakable blocks are transparent.
		return !(neighborBlock instanceof BlockBreakable && neighborBlock.getRegistryName().getNamespace().equals("minecraft"));
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}

	@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
		IBlockState disguisedState = getDisguisedBlockState(world, pos);

		return disguisedState != null ? disguisedState : state;
	}

	public IBlockState getDisguisedBlockState(IBlockAccess world, BlockPos pos) {
		TileEntity tile = world.getTileEntity(pos);

		if (tile instanceof IModuleInventory) {
			IModuleInventory te = (IModuleInventory) tile;

			return getDisguisedBlockStateFromStack(world, pos, te.isModuleEnabled(EnumModuleType.DISGUISE) ? te.getModule(EnumModuleType.DISGUISE) : ItemStack.EMPTY);
		}

		return null;
	}

	public IBlockState getDisguisedBlockStateFromStack(IBlockAccess world, BlockPos pos, ItemStack module) {
		if (!module.isEmpty()) {
			if (!module.hasTagCompound())
				module.setTagCompound(new NBTTagCompound());

			IBlockState disguisedState = NBTUtil.readBlockState(module.getTagCompound().getCompoundTag("SavedState"));

			if (disguisedState != null && disguisedState.getBlock() != Blocks.AIR)
				return disguisedState;
			else if (world != null && pos != null) { //fallback, mainly for upgrading old worlds from before the state selector existed
				ItemStack disguisedStack = ((ItemModule) module.getItem()).getAddonAsStack(module.getTagCompound());
				Block block = Block.getBlockFromItem(disguisedStack.getItem());
				boolean hasMeta = disguisedStack.getHasSubtypes();

				disguisedState = hasMeta ? block.getStateFromMeta(disguisedStack.getItemDamage()) : block.getDefaultState();

				if (block != this)
					return disguisedState.getActualState(world, pos);
			}
		}

		return null;
	}

	public ItemStack getDisguisedStack(IBlockAccess world, BlockPos pos) {
		IBlockState disguisedState = getDisguisedBlockState(world, pos);

		if (disguisedState != null)
			return new ItemStack(disguisedState.getBlock(), 1, disguisedState.getBlock().getMetaFromState(disguisedState));

		return new ItemStack(this);
	}

	@Override
	public ItemStack getDisplayStack(World world, IBlockState state, BlockPos pos) {
		return getDisguisedStack(world, pos);
	}

	@Override
	public boolean shouldShowSCInfo(World world, IBlockState state, BlockPos pos) {
		return getDisguisedStack(world, pos).getItem() == Item.getItemFromBlock(this);
	}

	@Override
	public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
		return getDisguisedStack(world, pos);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityDisguisable();
	}
}
