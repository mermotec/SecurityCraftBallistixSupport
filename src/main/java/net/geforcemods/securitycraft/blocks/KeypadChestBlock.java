package net.geforcemods.securitycraft.blocks;

import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.api.IOwnable;
import net.geforcemods.securitycraft.api.IPasswordConvertible;
import net.geforcemods.securitycraft.misc.ModuleType;
import net.geforcemods.securitycraft.misc.OwnershipEvent;
import net.geforcemods.securitycraft.tileentity.KeypadChestTileEntity;
import net.geforcemods.securitycraft.util.ModuleUtils;
import net.geforcemods.securitycraft.util.PlayerUtils;
import net.geforcemods.securitycraft.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.DoubleSidedInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.ChestType;
import net.minecraft.stats.Stats;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

public class KeypadChestBlock extends ChestBlock {

	private static final ChestBlock.InventoryFactory<INamedContainerProvider> field_220110_j = new ChestBlock.InventoryFactory<INamedContainerProvider>() {
		@Override
		public INamedContainerProvider forDouble(final ChestTileEntity chest1, final ChestTileEntity chest2) {
			final IInventory chestInventory = new DoubleSidedInventory(chest1, chest2);

			return new INamedContainerProvider() {
				@Override
				public Container createMenu(int id, PlayerInventory inventory, PlayerEntity player) {
					if (chest1.canOpen(player) && chest2.canOpen(player)) {
						chest1.fillWithLoot(inventory.player);
						chest2.fillWithLoot(inventory.player);
						return ChestContainer.createGeneric9X6(id, inventory, chestInventory);
					} else {
						return null;
					}
				}

				@Override
				public ITextComponent getDisplayName() {
					if (chest1.hasCustomName()) {
						return chest1.getDisplayName();
					} else {
						return chest2.hasCustomName() ? chest2.getDisplayName() : Utils.localize("block.securitycraft.keypad_chest_double");
					}
				}
			};
		}

		@Override
		public INamedContainerProvider forSingle(ChestTileEntity te) {
			return te;
		}
	};

	public KeypadChestBlock(Block.Properties properties){
		super(properties);
	}

	/**
	 * Called upon block activation (right click on the block.)
	 */
	@Override
	public boolean onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
	{
		if(!world.isRemote && world.getTileEntity(pos) instanceof KeypadChestTileEntity && !isBlocked(world, pos)) {
			if(ModuleUtils.checkForModule(world, pos, player, ModuleType.BLACKLIST))
				return true;
			else if(ModuleUtils.checkForModule(world, pos, player, ModuleType.WHITELIST))
				activate(world, pos, player);
			else if(!PlayerUtils.isHoldingItem(player, SCContent.CODEBREAKER, hand))
				((KeypadChestTileEntity) world.getTileEntity(pos)).openPasswordGUI(player);

			return true;
		}

		return true;
	}

	public static void activate(World world, BlockPos pos, PlayerEntity player){
		if(!world.isRemote) {
			BlockState state = world.getBlockState(pos);
			ChestBlock block = (ChestBlock)state.getBlock();
			INamedContainerProvider inamedcontainerprovider = block.getContainer(state, world, pos);
			if (inamedcontainerprovider != null) {
				player.openContainer(inamedcontainerprovider);
				player.addStat(Stats.CUSTOM.get(Stats.OPEN_CHEST));
			}
		}
	}

	/**
	 * Called when the block is placed in the world.
	 */
	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack){
		super.onBlockPlacedBy(world, pos, state, entity, stack);

		boolean isPlayer = entity instanceof PlayerEntity;

		if(isPlayer)
			MinecraftForge.EVENT_BUS.post(new OwnershipEvent(world, pos, (PlayerEntity)entity));

		if(world.getTileEntity(pos.east()) instanceof KeypadChestTileEntity && isPlayer && ((KeypadChestTileEntity) world.getTileEntity(pos.east())).getOwner().isOwner((PlayerEntity) entity))
			((KeypadChestTileEntity)(world.getTileEntity(pos))).setPassword(((KeypadChestTileEntity) world.getTileEntity(pos.east())).getPassword());
		else if(world.getTileEntity(pos.west()) instanceof KeypadChestTileEntity && isPlayer && ((KeypadChestTileEntity) world.getTileEntity(pos.west())).getOwner().isOwner((PlayerEntity) entity))
			((KeypadChestTileEntity)(world.getTileEntity(pos))).setPassword(((KeypadChestTileEntity) world.getTileEntity(pos.west())).getPassword());
		else if(world.getTileEntity(pos.south()) instanceof KeypadChestTileEntity && isPlayer && ((KeypadChestTileEntity) world.getTileEntity(pos.south())).getOwner().isOwner((PlayerEntity) entity))
			((KeypadChestTileEntity)(world.getTileEntity(pos))).setPassword(((KeypadChestTileEntity) world.getTileEntity(pos.south())).getPassword());
		else if(world.getTileEntity(pos.north()) instanceof KeypadChestTileEntity && isPlayer && ((KeypadChestTileEntity) world.getTileEntity(pos.north())).getOwner().isOwner((PlayerEntity) entity))
			((KeypadChestTileEntity)(world.getTileEntity(pos))).setPassword(((KeypadChestTileEntity) world.getTileEntity(pos.north())).getPassword());
	}

	@Override
	public void onNeighborChange(BlockState state, IWorldReader world, BlockPos pos, BlockPos neighbor){
		super.onNeighborChange(state, world, pos, neighbor);

		TileEntity tileEntity = world.getTileEntity(pos);

		if (tileEntity instanceof KeypadChestTileEntity)
			((KeypadChestTileEntity) tileEntity).updateContainingBlockInfo();

	}

	@Override
	public INamedContainerProvider getContainer(BlockState state, World world, BlockPos pos) {
		return getChestInventory(state, world, pos, false, field_220110_j);
	}

	/**
	 * Returns a new instance of a block's tile entity class. Called on placing the block.
	 */
	@Override
	public TileEntity createNewTileEntity(IBlockReader reader)
	{
		return new KeypadChestTileEntity();
	}

	public static boolean isBlocked(World world, BlockPos pos)
	{
		return isBelowSolidBlock(world, pos);
	}

	private static boolean isBelowSolidBlock(World world, BlockPos pos)
	{
		return world.getBlockState(pos.up()).isNormalCube(world, pos.up());
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot)
	{
		return state.with(FACING, rot.rotate(state.get(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror)
	{
		return state.rotate(mirror.toRotation(state.get(FACING)));
	}

	public static class Convertible implements IPasswordConvertible
	{
		@Override
		public Block getOriginalBlock()
		{
			return Blocks.CHEST;
		}

		@Override
		public boolean convert(PlayerEntity player, World world, BlockPos pos)
		{
			BlockState state = world.getBlockState(pos);
			Direction facing = state.get(FACING);
			ChestType type = state.get(TYPE);

			convertChest(player, world, pos, facing, type);

			if(type != ChestType.SINGLE)
			{
				BlockPos newPos = pos.offset(getDirectionToAttached(state));
				BlockState newState = world.getBlockState(newPos);
				Direction newFacing = newState.get(FACING);
				ChestType newType = newState.get(TYPE);

				convertChest(player, world, newPos, newFacing, newType);
			}

			return true;
		}

		private void convertChest(PlayerEntity player, World world, BlockPos pos, Direction facing, ChestType type)
		{
			ChestTileEntity chest = (ChestTileEntity)world.getTileEntity(pos);
			CompoundNBT tag = chest.write(new CompoundNBT());

			chest.clear();
			world.setBlockState(pos, SCContent.KEYPAD_CHEST.get().getDefaultState().with(FACING, facing).with(TYPE, type));
			((ChestTileEntity)world.getTileEntity(pos)).read(tag);
			((IOwnable) world.getTileEntity(pos)).getOwner().set(player.getUniqueID().toString(), player.getName().getFormattedText());
		}
	}
}
