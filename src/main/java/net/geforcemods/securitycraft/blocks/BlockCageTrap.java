package net.geforcemods.securitycraft.blocks;

import java.util.List;

import org.apache.logging.log4j.util.TriConsumer;

import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.api.IOwnable;
import net.geforcemods.securitycraft.api.Owner;
import net.geforcemods.securitycraft.blocks.reinforced.BlockReinforcedIronBars;
import net.geforcemods.securitycraft.items.ItemModule;
import net.geforcemods.securitycraft.misc.EnumModuleType;
import net.geforcemods.securitycraft.tileentity.TileEntityCageTrap;
import net.geforcemods.securitycraft.tileentity.TileEntityDisguisable;
import net.geforcemods.securitycraft.tileentity.TileEntityReinforcedIronBars;
import net.geforcemods.securitycraft.util.PlayerUtils;
import net.geforcemods.securitycraft.util.Utils;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockCageTrap extends BlockDisguisable {
	public static final PropertyBool DEACTIVATED = PropertyBool.create("deactivated");

	public BlockCageTrap(Material material) {
		super(material);
		setSoundType(SoundType.METAL);
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
		if (state.getValue(DEACTIVATED))
			return super.getCollisionBoundingBox(state, world, pos);
		else
			return null;
	}

	@Override
	public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, Entity entity, boolean isActualState) {
		TileEntity tile = world.getTileEntity(pos);

		if (tile instanceof TileEntityCageTrap) {
			TileEntityCageTrap cageTrap = (TileEntityCageTrap) tile;

			if (cageTrap.isDisabled()) {
				addCorrectShape(state, world, pos, entityBox, collidingBoxes, entity, isActualState, cageTrap);
				return;
			}

			if (entity instanceof EntityPlayer && ((cageTrap.isOwnedBy((EntityPlayer) entity) && cageTrap.ignoresOwner()) || cageTrap.isAllowed(entity)))
				addCorrectShape(state, world, pos, entityBox, collidingBoxes, entity, isActualState, cageTrap);

			if (entity instanceof EntityLiving && !state.getValue(DEACTIVATED)) {
				if (cageTrap.capturesMobs())
					addCollisionBoxToList(pos, entityBox, collidingBoxes, NULL_AABB);
				else
					addCorrectShape(state, world, pos, entityBox, collidingBoxes, entity, isActualState, cageTrap);

				return;
			}
			else if (entity instanceof EntityItem) {
				addCorrectShape(state, world, pos, entityBox, collidingBoxes, entity, isActualState, cageTrap);
				return;
			}

			if (state.getValue(DEACTIVATED))
				addCorrectShape(state, world, pos, entityBox, collidingBoxes, entity, isActualState, cageTrap);
			else
				addCollisionBoxToList(pos, entityBox, collidingBoxes, NULL_AABB);
		}
		else
			addCollisionBoxToList(pos, entityBox, collidingBoxes, NULL_AABB);
	}

	private void addCorrectShape(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, Entity entity, boolean isActualState, TileEntityDisguisable disguisableTe) {
		if (disguisableTe.isModuleEnabled(EnumModuleType.DISGUISE)) {
			ItemStack moduleStack = disguisableTe.getModule(EnumModuleType.DISGUISE);

			if (!moduleStack.isEmpty() && (((ItemModule) moduleStack.getItem()).getBlockAddon(moduleStack.getTagCompound()) != null))
				super.addCollisionBoxToList(state, world, pos, entityBox, collidingBoxes, entity, isActualState);
		}

		addCollisionBoxToList(pos, entityBox, collidingBoxes, FULL_BLOCK_AABB);
	}

	@Override
	public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
		if (!world.isRemote) {
			if (state.getValue(DEACTIVATED))
				return;

			TileEntityCageTrap cageTrap = (TileEntityCageTrap) world.getTileEntity(pos);
			boolean isPlayer = entity instanceof EntityPlayer;

			if (isPlayer || (entity instanceof EntityMob && cageTrap.capturesMobs())) {
				if (!state.getBoundingBox(world, pos).offset(pos).intersects(entity.getEntityBoundingBox()))
					return;

				if ((isPlayer && cageTrap.isOwnedBy((EntityPlayer) entity)) && cageTrap.ignoresOwner())
					return;

				BlockPos topMiddle = pos.up(4);
				String ownerName = cageTrap.getOwner().getName();
				BlockModifier placer = new BlockModifier(world, new MutableBlockPos(pos), cageTrap.getOwner());

				placer.loop((w, p, o) -> {
					if (w.isAirBlock(p)) {
						if (p.equals(topMiddle))
							w.setBlockState(p, SCContent.horizontalReinforcedIronBars.getDefaultState());
						else
							w.setBlockState(p, ((BlockReinforcedIronBars) SCContent.reinforcedIronBars).getActualState(SCContent.reinforcedIronBars.getDefaultState(), w, p));
					}
				});
				placer.loop((w, p, o) -> {
					TileEntity te = w.getTileEntity(p);

					if (te instanceof IOwnable)
						((IOwnable) te).setOwner(o.getUUID(), o.getName());

					if (te instanceof TileEntityReinforcedIronBars)
						((TileEntityReinforcedIronBars) te).setCanDrop(false);
				});
				world.setBlockState(pos, getDefaultState().withProperty(DEACTIVATED, true));
				world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 3.0F, 1.0F);

				if (isPlayer && PlayerUtils.isPlayerOnline(ownerName))
					PlayerUtils.sendMessageToPlayer(PlayerUtils.getPlayerFromName(ownerName), Utils.localize("tile.securitycraft:cageTrap.name"), Utils.localize("messages.securitycraft:cageTrap.captured", entity.getName(), pos), TextFormatting.BLACK);
			}
		}
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		ItemStack stack = player.getHeldItem(hand);

		if (stack.getItem() == SCContent.wireCutters) {
			if (!state.getValue(DEACTIVATED)) {
				world.setBlockState(pos, state.withProperty(DEACTIVATED, true));

				if (!player.isCreative())
					stack.damageItem(1, player);

				world.playSound(null, pos, SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.BLOCKS, 1.0F, 1.0F);
				return true;
			}
		}
		else if (stack.getItem() == Items.REDSTONE) {
			if (state.getValue(DEACTIVATED)) {
				world.setBlockState(pos, state.withProperty(DEACTIVATED, false));

				if (!player.isCreative())
					stack.shrink(1);

				world.playSound(null, pos, SoundEvents.BLOCK_TRIPWIRE_CLICK_ON, SoundCategory.BLOCKS, 1.0F, 1.0F);
				return true;
			}
		}
		return false;
	}

	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
		return getDefaultState().withProperty(DEACTIVATED, false);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(DEACTIVATED, (meta == 1));
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
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityCageTrap();
	}

	public static class BlockModifier {
		private World world;
		private MutableBlockPos pos;
		private BlockPos origin;
		private Owner owner;

		public BlockModifier(World world, MutableBlockPos origin, Owner owner) {
			this.world = world;
			pos = origin.setPos(origin.getX() - 1, origin.getY() + 1, origin.getZ() - 1);
			this.origin = origin.toImmutable();
			this.owner = owner;
		}

		public void loop(TriConsumer<World, MutableBlockPos, Owner> ifTrue) {
			for (int y = 0; y < 4; y++) {
				for (int x = 0; x < 3; x++) {
					for (int z = 0; z < 3; z++) {
						//skip the middle column above the cage trap, but not the place where the horizontal iron bars are
						if (!(x == 1 && z == 1 && y != 3))
							ifTrue.accept(world, pos, owner);

						pos.setPos(pos.getX(), pos.getY(), pos.getZ() + 1);
					}

					pos.setPos(pos.getX() + 1, pos.getY(), pos.getZ() - 3);
				}

				pos.setPos(pos.getX() - 3, pos.getY() + 1, pos.getZ());
			}

			pos.setPos(origin); //reset the mutable block pos for the next usage
		}
	}
}