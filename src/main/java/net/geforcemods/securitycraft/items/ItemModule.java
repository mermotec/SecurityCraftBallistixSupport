package net.geforcemods.securitycraft.items;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import net.geforcemods.securitycraft.SecurityCraft;
import net.geforcemods.securitycraft.api.ILinkedAction;
import net.geforcemods.securitycraft.api.IModuleInventory;
import net.geforcemods.securitycraft.api.IOwnable;
import net.geforcemods.securitycraft.api.TileEntityLinkable;
import net.geforcemods.securitycraft.misc.EnumModuleType;
import net.geforcemods.securitycraft.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemModule extends Item {
	public static final int MAX_PLAYERS = 50;
	private final EnumModuleType module;
	private final boolean containsCustomData;
	private boolean canBeCustomized;
	private int guiToOpen;

	public ItemModule(EnumModuleType module, boolean containsCustomData) {
		this(module, containsCustomData, false, -1);
	}

	public ItemModule(EnumModuleType module, boolean containsCustomData, boolean canBeCustomized, int guiToOpen) {
		this.module = module;
		this.containsCustomData = containsCustomData;
		this.canBeCustomized = canBeCustomized;
		this.guiToOpen = guiToOpen;

		setMaxStackSize(1);
		setCreativeTab(SecurityCraft.tabSCTechnical);
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		TileEntity te = world.getTileEntity(pos);

		if (te instanceof IModuleInventory) {
			IModuleInventory inv = (IModuleInventory) te;
			ItemStack stack = player.getHeldItem(hand);
			EnumModuleType type = ((ItemModule) stack.getItem()).getModuleType();

			if (te instanceof IOwnable && !((IOwnable) te).isOwnedBy(player))
				return EnumActionResult.PASS;

			if (inv.acceptsModule(type) && !inv.hasModule(type)) {
				if (!world.isRemote) {
					inv.insertModule(stack, false);

					if (inv instanceof TileEntityLinkable) {
						TileEntityLinkable linkable = (TileEntityLinkable) inv;

						linkable.createLinkedBlockAction(new ILinkedAction.ModuleInserted(stack, (ItemModule) stack.getItem(), false), linkable);
					}

					if (!player.isCreative())
						stack.shrink(1);
				}

				return EnumActionResult.SUCCESS;
			}
		}

		return EnumActionResult.PASS;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);

		if (!player.isSneaking()) {
			if (!stack.hasTagCompound())
				stack.setTagCompound(new NBTTagCompound());

			if (canBeCustomized()) {
				player.openGui(SecurityCraft.instance, guiToOpen, world, (int) player.posX, (int) player.posY, (int) player.posZ);
				return ActionResult.newResult(EnumActionResult.SUCCESS, stack);
			}
		}

		return ActionResult.newResult(EnumActionResult.PASS, stack);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
		if (containsCustomData || canBeCustomized())
			list.add(Utils.localize("tooltip.securitycraft:module.modifiable").getFormattedText());
		else
			list.add(Utils.localize("tooltip.securitycraft:module.notModifiable").getFormattedText());

		if (canBeCustomized()) {
			Block addon = getBlockAddon(stack.getTagCompound());

			if (addon != null)
				list.add(Utils.localize("tooltip.securitycraft:module.itemAddons.added", TextFormatting.GRAY + Utils.localize(addon).getFormattedText()).getFormattedText());
		}

		if (containsCustomData) {
			boolean affectsEveryone = false;
			int playerCount = 0;
			int teamCount = 0;

			if (stack.hasTagCompound()) {
				NBTTagCompound tag = stack.getTagCompound();

				affectsEveryone = tag.getBoolean("affectEveryone");

				if (!affectsEveryone) {
					playerCount = ItemModule.getPlayersFromModule(stack).size();
					teamCount = tag.getTagList("ListedTeams", Constants.NBT.TAG_STRING).tagCount();
				}
			}

			if (affectsEveryone)
				list.add(Utils.localize("tooltip.securitycraft:module.affects_everyone").getFormattedText());
			else {
				list.add(Utils.localize("tooltip.securitycraft:module.added_players", "" + TextFormatting.GRAY + playerCount).getFormattedText());
				list.add(Utils.localize("tooltip.securitycraft:module.added_teams", "" + TextFormatting.GRAY + teamCount).getFormattedText());
			}
		}
	}

	public EnumModuleType getModuleType() {
		return module;
	}

	public Block getBlockAddon(NBTTagCompound tag) {
		ItemStack stack = getAddonAsStack(tag);

		if (stack.getItem() instanceof ItemBlock)
			return ((ItemBlock) stack.getItem()).getBlock();
		else
			return null;
	}

	public ItemStack getAddonAsStack(NBTTagCompound tag) {
		if (tag == null)
			return ItemStack.EMPTY;

		NBTTagList items = tag.getTagList("ItemInventory", Constants.NBT.TAG_COMPOUND);

		if (items != null && !items.isEmpty())
			return new ItemStack(items.getCompoundTagAt(0));

		return ItemStack.EMPTY;
	}

	public boolean canBeCustomized() {
		return canBeCustomized;
	}

	public static boolean doesModuleHaveTeamOf(ItemStack module, String name, World level) {
		ScorePlayerTeam team = level.getScoreboard().getPlayersTeam(name);

		if (!module.hasTagCompound())
			module.setTagCompound(new NBTTagCompound());

		//@formatter:off
		return team != null && StreamSupport.stream(module.getTagCompound().getTagList("ListedTeams", Constants.NBT.TAG_STRING).spliterator(), false)
				.filter(tag -> tag instanceof NBTTagString)
				.map(tag -> ((NBTTagString) tag).getString())
				.anyMatch(team.getName()::equals);
		//@formatter:on
	}

	public static List<String> getPlayersFromModule(ItemStack stack) {
		List<String> list = new ArrayList<>();

		if (stack.getItem() instanceof ItemModule && stack.hasTagCompound()) {
			for (int i = 1; i <= MAX_PLAYERS; i++) {
				if (stack.getTagCompound().getString("Player" + i) != null && !stack.getTagCompound().getString("Player" + i).isEmpty())
					list.add(stack.getTagCompound().getString("Player" + i).toLowerCase());
			}
		}

		return list;
	}
}