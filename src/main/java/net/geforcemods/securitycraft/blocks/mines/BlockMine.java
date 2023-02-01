package net.geforcemods.securitycraft.blocks.mines;

import java.util.Random;

import net.geforcemods.securitycraft.ConfigHandler;
import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.api.TileEntityOwnable;
import net.geforcemods.securitycraft.util.BlockUtils;
import net.geforcemods.securitycraft.util.EntityUtils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockMine extends BlockExplosive {
	public static final PropertyBool DEACTIVATED = PropertyBool.create("deactivated");

	public BlockMine(Material material) {
		super(material);
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isNormalCube(IBlockState state) {
		return false;
	}

	@Override
	public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
		if (world.getBlockState(pos.down()).getMaterial() != Material.AIR)
			return;
		else if (world.getBlockState(pos).getValue(DEACTIVATED))
			world.destroyBlock(pos, true);
		else
			explode(world, pos);
	}

	@Override
	public boolean canPlaceBlockAt(World world, BlockPos pos) {
		return world.getBlockState(pos.down()).getBlock().isSideSolid(world.getBlockState(pos.down()), world, pos.down(), EnumFacing.UP);
	}

	@Override
	public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
		if (!world.isRemote)
			if (player != null && player.capabilities.isCreativeMode && !ConfigHandler.mineExplodesWhenInCreative)
				return super.removedByPlayer(state, world, pos, player, willHarvest);
			else if (!EntityUtils.doesPlayerOwn(player, world, pos)) {
				explode(world, pos);
				return super.removedByPlayer(state, world, pos, player, willHarvest);
			}

		return super.removedByPlayer(state, world, pos, player, willHarvest);
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		float fifth = 0.2F;
		float tenth = 0.1F;

		return BlockUtils.fromBounds(0.5F - fifth, 0.0F, 0.5F - fifth, 0.5F + fifth, (tenth * 2.0F) / 2 + 0.1F, 0.5F + fifth);
	}

	@Override
	public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
		if (world.isRemote)
			return;
		else if (entity instanceof EntityItem)
			return;
		else if (!state.getBoundingBox(world, pos).offset(pos).grow(0.01D).intersects(entity.getEntityBoundingBox()))
			return;
		else if (!EntityUtils.doesEntityOwn(entity, world, pos) && !(entity instanceof EntityPlayer && ((EntityPlayer) entity).isCreative()))
			explode(world, pos);
	}

	@Override
	public boolean activateMine(World world, BlockPos pos) {
		IBlockState state = world.getBlockState(pos);

		if (state.getValue(DEACTIVATED)) {
			world.setBlockState(pos, state.withProperty(DEACTIVATED, false));
			return true;
		}

		return false;
	}

	@Override
	public boolean defuseMine(World world, BlockPos pos) {
		IBlockState state = world.getBlockState(pos);

		if (!state.getValue(DEACTIVATED)) {
			world.setBlockState(pos, state.withProperty(DEACTIVATED, true));
			return true;
		}

		return false;
	}

	@Override
	public void explode(World world, BlockPos pos) {
		if (world.isRemote)
			return;

		if (!world.getBlockState(pos).getValue(DEACTIVATED)) {
			world.destroyBlock(pos, false);
			world.newExplosion((Entity) null, pos.getX(), pos.getY(), pos.getZ(), ConfigHandler.smallerMineExplosion ? 1.0F : 3.0F, ConfigHandler.shouldSpawnFire, ConfigHandler.mineExplosionsBreakBlocks);
		}
	}

	@Override
	public Item getItemDropped(IBlockState state, Random random, int fortune) {
		return Item.getItemFromBlock(SCContent.mine);
	}

	@Override
	public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
		return new ItemStack(Item.getItemFromBlock(SCContent.mine));
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(DEACTIVATED, meta == 1 ? true : false);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(DEACTIVATED) ? 1 : 0;
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, DEACTIVATED);
	}

	@Override
	public boolean isActive(World world, BlockPos pos) {
		return !world.getBlockState(pos).getValue(DEACTIVATED);
	}

	@Override
	public boolean isDefusable() {
		return true;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityOwnable();
	}
}