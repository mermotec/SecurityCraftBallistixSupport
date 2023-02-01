package net.geforcemods.securitycraft.tileentity;

import java.util.List;

import net.geforcemods.securitycraft.api.CustomizableSCTE;
import net.geforcemods.securitycraft.api.Option;
import net.geforcemods.securitycraft.api.Option.DisabledOption;
import net.geforcemods.securitycraft.api.Option.IgnoreOwnerOption;
import net.geforcemods.securitycraft.api.Option.OptionBoolean;
import net.geforcemods.securitycraft.api.Option.OptionDouble;
import net.geforcemods.securitycraft.api.Option.OptionInt;
import net.geforcemods.securitycraft.api.Owner;
import net.geforcemods.securitycraft.blocks.BlockPortableRadar;
import net.geforcemods.securitycraft.misc.EnumModuleType;
import net.geforcemods.securitycraft.util.EntityUtils;
import net.geforcemods.securitycraft.util.PlayerUtils;
import net.geforcemods.securitycraft.util.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextFormatting;

public class TileEntityPortableRadar extends CustomizableSCTE implements ITickable {
	private OptionDouble searchRadiusOption = new OptionDouble(this::getPos, "searchRadius", 25.0D, 5.0D, 50.0D, 1.0D, true);
	private OptionInt searchDelayOption = new OptionInt(this::getPos, "searchDelay", 4, 4, 10, 1, true);
	private OptionBoolean repeatMessageOption = new OptionBoolean("repeatMessage", true);
	private DisabledOption disabled = new DisabledOption(false);
	private IgnoreOwnerOption ignoreOwner = new IgnoreOwnerOption(true);
	private boolean shouldSendNewMessage = true;
	private Owner lastPlayer = new Owner();
	private int ticksUntilNextSearch = getSearchDelay();

	@Override
	public void update() {
		if (!world.isRemote && !disabled.get() && ticksUntilNextSearch-- <= 0) {
			ticksUntilNextSearch = getSearchDelay();

			EntityPlayerMP owner = world.getMinecraftServer().getPlayerList().getPlayerByUsername(getOwner().getName());
			AxisAlignedBB area = new AxisAlignedBB(pos).grow(getSearchRadius(), getSearchRadius(), getSearchRadius());
			List<EntityPlayer> entities = world.getEntitiesWithinAABB(EntityPlayer.class, area, e -> !(isOwnedBy(e) && ignoresOwner()) && !isAllowed(e) && !e.isSpectator() && !EntityUtils.isInvisible(e));

			if (isModuleEnabled(EnumModuleType.REDSTONE)) {
				BlockPortableRadar.togglePowerOutput(world, pos, !entities.isEmpty());
			}

			if (owner != null) {
				for (EntityPlayer e : entities) {
					if (shouldSendMessage(e)) {
						PlayerUtils.sendMessageToPlayer(owner, Utils.localize("tile.securitycraft:portableRadar.name"), hasCustomName() ? (Utils.localize("messages.securitycraft:portableRadar.withName", TextFormatting.ITALIC + e.getName() + TextFormatting.RESET, TextFormatting.ITALIC + getName() + TextFormatting.RESET)) : (Utils.localize("messages.securitycraft:portableRadar.withoutName", TextFormatting.ITALIC + e.getName() + TextFormatting.RESET, pos)), TextFormatting.BLUE);
						setSentMessage();
					}
				}
			}
		}
	}

	@Override
	public void onModuleRemoved(ItemStack stack, EnumModuleType module, boolean toggled) {
		super.onModuleRemoved(stack, module, toggled);

		if (module == EnumModuleType.REDSTONE)
			BlockPortableRadar.togglePowerOutput(world, pos, false);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);

		NBTTagCompound lastPlayerTag = new NBTTagCompound();

		tag.setBoolean("shouldSendNewMessage", shouldSendNewMessage);
		lastPlayer.writeToNBT(lastPlayerTag, false);
		tag.setTag("lastPlayer", lastPlayerTag);
		return tag;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		shouldSendNewMessage = tag.getBoolean("shouldSendNewMessage");
		lastPlayer = Owner.fromCompound(tag.getCompoundTag("lastPlayer"));
	}

	@Override
	public void readOptions(NBTTagCompound tag) {
		if (tag.hasKey("enabled"))
			tag.setBoolean("disabled", !tag.getBoolean("enabled")); //legacy support

		for (Option<?> option : customOptions()) {
			option.readFromNBT(tag);
		}
	}

	public boolean shouldSendMessage(EntityPlayer player) {
		Owner currentPlayer = new Owner(player);

		if (!currentPlayer.equals(lastPlayer)) {
			shouldSendNewMessage = true;
			lastPlayer = currentPlayer;
		}

		return (shouldSendNewMessage || repeatMessageOption.get()) && !(lastPlayer.owns(this) && ignoresOwner());
	}

	public void setSentMessage() {
		shouldSendNewMessage = false;
	}

	public double getSearchRadius() {
		return searchRadiusOption.get();
	}

	public int getSearchDelay() {
		return searchDelayOption.get() * 20;
	}

	public boolean ignoresOwner() {
		return ignoreOwner.get();
	}

	@Override
	public EnumModuleType[] acceptedModules() {
		return new EnumModuleType[] {
				EnumModuleType.REDSTONE, EnumModuleType.ALLOWLIST
		};
	}

	@Override
	public Option<?>[] customOptions() {
		return new Option[] {
				searchRadiusOption, searchDelayOption, repeatMessageOption, disabled, ignoreOwner
		};
	}
}