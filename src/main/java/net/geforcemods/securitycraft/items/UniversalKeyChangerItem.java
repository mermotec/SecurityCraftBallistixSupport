package net.geforcemods.securitycraft.items;

import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.api.IOwnable;
import net.geforcemods.securitycraft.api.IPasswordProtected;
import net.geforcemods.securitycraft.blocks.DisguisableBlock;
import net.geforcemods.securitycraft.containers.GenericTEContainer;
import net.geforcemods.securitycraft.tileentity.DisguisableTileEntity;
import net.geforcemods.securitycraft.util.PlayerUtils;
import net.geforcemods.securitycraft.util.Utils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public class UniversalKeyChangerItem extends Item {

	public UniversalKeyChangerItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public ActionResultType onItemUseFirst(ItemStack stack, ItemUseContext ctx)
	{
		return onItemUseFirst(ctx.getPlayer(), ctx.getWorld(), ctx.getPos(), ctx.getFace(), ctx.getItem(), ctx.getHand());
	}

	public ActionResultType onItemUseFirst(PlayerEntity player, World world, BlockPos pos, Direction side, ItemStack stack, Hand hand) {
		ActionResultType briefcaseResult = handleBriefcase(player, hand).getType();

		if(briefcaseResult != ActionResultType.PASS)
			return briefcaseResult;

		TileEntity te = world.getTileEntity(pos);

		if(te instanceof IPasswordProtected) {
			if(((IOwnable) te).getOwner().isOwner(player))
			{
				if(!world.isRemote)
					NetworkHooks.openGui((ServerPlayerEntity)player, new INamedContainerProvider() {
						@Override
						public Container createMenu(int windowId, PlayerInventory inv, PlayerEntity player)
						{
							return new GenericTEContainer(SCContent.cTypeKeyChanger, windowId, world, pos);
						}

						@Override
						public ITextComponent getDisplayName()
						{
							return new TranslationTextComponent(getTranslationKey());
						}
					}, pos);

				return ActionResultType.SUCCESS;
			}
			else if(!(te instanceof DisguisableTileEntity) || (((BlockItem)((DisguisableBlock)((DisguisableTileEntity)te).getBlockState().getBlock()).getDisguisedStack(world, pos).getItem()).getBlock() instanceof DisguisableBlock)) {
				PlayerUtils.sendMessageToPlayer(player, Utils.localize(SCContent.UNIVERSAL_KEY_CHANGER.get().getTranslationKey()), Utils.localize("messages.securitycraft:notOwned", ((IOwnable)world.getTileEntity(pos)).getOwner().getName()), TextFormatting.RED);
				return ActionResultType.FAIL;
			}
		}

		return ActionResultType.PASS;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
		return handleBriefcase(player, hand);
	}

	private ActionResult<ItemStack> handleBriefcase(PlayerEntity player, Hand hand)
	{
		ItemStack keyChanger = player.getHeldItem(hand);

		if (hand == Hand.MAIN_HAND && player.getHeldItemOffhand().getItem() == SCContent.BRIEFCASE.get()) {
			ItemStack briefcase = player.getHeldItemOffhand();

			if (BriefcaseItem.isOwnedBy(briefcase, player)) {
				if (briefcase.hasTag() && briefcase.getTag().contains("passcode")) {
					briefcase.getTag().remove("passcode");
					PlayerUtils.sendMessageToPlayer(player, Utils.localize(SCContent.UNIVERSAL_KEY_CHANGER.get().getTranslationKey()), Utils.localize("messages.securitycraft:universalKeyChanger.briefcase.passcodeReset"), TextFormatting.GREEN);
					return ActionResult.newResult(ActionResultType.SUCCESS, keyChanger);
				}
				else
					PlayerUtils.sendMessageToPlayer(player, Utils.localize(SCContent.UNIVERSAL_KEY_CHANGER.get().getTranslationKey()), Utils.localize("messages.securitycraft:universalKeyChanger.briefcase.noPasscode"), TextFormatting.RED);
			}
			else
				PlayerUtils.sendMessageToPlayer(player, Utils.localize(SCContent.UNIVERSAL_KEY_CHANGER.get().getTranslationKey()), Utils.localize("messages.securitycraft:universalKeyChanger.briefcase.notOwned"), TextFormatting.RED);

			return ActionResult.newResult(ActionResultType.SUCCESS, keyChanger);
		}

		return ActionResult.newResult(ActionResultType.PASS, keyChanger);
	}
}
