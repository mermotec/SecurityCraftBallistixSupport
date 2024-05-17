package net.geforcemods.securitycraft.entity;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.SCTags;
import net.geforcemods.securitycraft.api.ICustomizable;
import net.geforcemods.securitycraft.api.IModuleInventory;
import net.geforcemods.securitycraft.api.IOwnable;
import net.geforcemods.securitycraft.api.IPasscodeProtected;
import net.geforcemods.securitycraft.api.Option;
import net.geforcemods.securitycraft.api.Option.BooleanOption;
import net.geforcemods.securitycraft.api.Option.SendAllowlistMessageOption;
import net.geforcemods.securitycraft.api.Option.SendDenylistMessageOption;
import net.geforcemods.securitycraft.api.Option.SmartModuleCooldownOption;
import net.geforcemods.securitycraft.api.Owner;
import net.geforcemods.securitycraft.inventory.CustomizeBlockMenu;
import net.geforcemods.securitycraft.misc.ModuleType;
import net.geforcemods.securitycraft.misc.SaltData;
import net.geforcemods.securitycraft.network.client.OpenScreen;
import net.geforcemods.securitycraft.network.client.OpenScreen.DataType;
import net.geforcemods.securitycraft.util.PasscodeUtils;
import net.geforcemods.securitycraft.util.PlayerUtils;
import net.geforcemods.securitycraft.util.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

public class SecuritySeaBoat extends ChestBoat implements IOwnable, IPasscodeProtected, IModuleInventory, ICustomizable {
	private static final EntityDataAccessor<Owner> OWNER = SynchedEntityData.<Owner>defineId(SecuritySeaBoat.class, Owner.getSerializer());
	private byte[] passcode;
	private UUID saltKey;
	private NonNullList<ItemStack> modules = NonNullList.<ItemStack>withSize(getMaxNumberOfModules(), ItemStack.EMPTY);
	private BooleanOption sendAllowlistMessage = new SendAllowlistMessageOption(false);
	private BooleanOption sendDenylistMessage = new SendDenylistMessageOption(true);
	private SmartModuleCooldownOption smartModuleCooldown = new SmartModuleCooldownOption();
	private long cooldownEnd = 0;
	private Map<ModuleType, Boolean> moduleStates = new EnumMap<>(ModuleType.class);

	public SecuritySeaBoat(EntityType<? extends Boat> type, Level level) {
		super(SCContent.SECURITY_SEA_BOAT_ENTITY.get(), level);
	}

	public SecuritySeaBoat(Level level, double x, double y, double z) {
		super(SCContent.SECURITY_SEA_BOAT_ENTITY.get(), level);
		setPos(x, y, z);
		xo = y;
		yo = y;
		zo = z;
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(OWNER, new Owner());
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		if (player.isSecondaryUseActive()) {
			ItemStack stack = player.getItemInHand(hand);
			Level level = player.level();

			if (player.isHolding(SCContent.CODEBREAKER.get())) {
				if (!level.isClientSide)
					handleCodebreaking(player, player.getMainHandItem().is(SCContent.CODEBREAKER.get()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);

				return InteractionResult.sidedSuccess(level.isClientSide);
			}
			else if (player.isHolding(SCContent.UNIVERSAL_KEY_CHANGER.get())) {
				if (!level.isClientSide) {
					if (isOwnedBy(player) || player.isCreative())
						PacketDistributor.PLAYER.with((ServerPlayer) player).send(new OpenScreen(DataType.CHANGE_PASSCODE_FOR_ENTITY, getId()));
					else
						PlayerUtils.sendMessageToPlayer(player, Utils.localize(SCContent.UNIVERSAL_KEY_CHANGER.get().getDescriptionId()), Utils.localize("messages.securitycraft:notOwned", PlayerUtils.getOwnerComponent(getOwner())), ChatFormatting.RED);
				}

				return InteractionResult.sidedSuccess(level.isClientSide);
			}
			else if (isOwnedBy(player)) {
				if (!level.isClientSide) {
					if (stack.is(SCContent.UNIVERSAL_OWNER_CHANGER.get())) {
						String newOwner = stack.getHoverName().getString();

						setOwner(PlayerUtils.isPlayerOnline(newOwner) ? PlayerUtils.getPlayerFromName(newOwner).getUUID().toString() : "ownerUUID", newOwner);
						PlayerUtils.sendMessageToPlayer(player, Utils.localize(SCContent.UNIVERSAL_OWNER_CHANGER.get().getDescriptionId()), Utils.localize("messages.securitycraft:universalOwnerChanger.changed", newOwner), ChatFormatting.GREEN);
					}
					else if (stack.is(SCContent.UNIVERSAL_BLOCK_MODIFIER.get())) {
						BlockPos pos = blockPosition();

						player.openMenu(new MenuProvider() {
							@Override
							public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
								return new CustomizeBlockMenu(windowId, level, pos, SecuritySeaBoat.super.getId(), inv);
							}

							@Override
							public Component getDisplayName() {
								return SecuritySeaBoat.super.getDisplayName();
							}
						}, data -> {
							data.writeBlockPos(pos);
							data.writeVarInt(SecuritySeaBoat.super.getId());
						});
					}
				}

				return InteractionResult.sidedSuccess(level.isClientSide);
			}
			else
				PlayerUtils.sendMessageToPlayer(player, Utils.localize(SCContent.UNIVERSAL_BLOCK_MODIFIER.get().getDescriptionId()), Utils.localize("messages.securitycraft:notOwned", PlayerUtils.getOwnerComponent(getOwner())), ChatFormatting.RED);
		}

		return super.interact(player, hand);
	}

	@Override
	public InteractionResult interactWithContainerVehicle(Player player) {
		Level level = level();
		BlockPos pos = blockPosition();

		if (!level.isClientSide && verifyPasscodeSet(level, pos, this, player)) {
			if (isDenied(player)) {
				if (sendsDenylistMessage())
					PlayerUtils.sendMessageToPlayer(player, Utils.localize(getType().getDescriptionId()), Utils.localize("messages.securitycraft:module.onDenylist"), ChatFormatting.RED);
			}
			else if (isAllowed(player)) {
				if (sendsAllowlistMessage())
					PlayerUtils.sendMessageToPlayer(player, Utils.localize(getType().getDescriptionId()), Utils.localize("messages.securitycraft:module.onAllowlist"), ChatFormatting.GREEN);

				activate(player);
			}
			else
				openPasscodeGUI(level, pos, player);
		}

		return !level.isClientSide ? InteractionResult.CONSUME : InteractionResult.SUCCESS;
	}

	@Override
	public void openCustomInventoryScreen(Player player) {
		interactWithContainerVehicle(player);
	}

	@Override
	public void openPasscodeGUI(Level level, BlockPos pos, Player player) {
		if (!level.isClientSide && getPasscode() != null)
			PacketDistributor.PLAYER.with((ServerPlayer) player).send(new OpenScreen(DataType.CHECK_PASSCODE_FOR_ENTITY, getId()));
	}

	@Override
	public void openSetPasscodeScreen(ServerPlayer player, BlockPos pos) {
		PacketDistributor.PLAYER.with(player).send(new OpenScreen(DataType.SET_PASSCODE_FOR_ENTITY, getId()));
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		Entity entity = source.getEntity();

		if (!(entity instanceof Player player) || isOwnedBy(player) || player.isCreative())
			return super.hurt(source, amount);
		else
			return false;
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		return !source.is(SCTags.DamageTypes.SECURITY_SEA_BOAT_VULNERABLE_TO) || super.isInvulnerableTo(source);
	}

	@Override
	public void chestVehicleDestroyed(DamageSource damageSource, Level level, Entity entity) {
		super.chestVehicleDestroyed(damageSource, level, entity);
		SaltData.removeSalt(getSaltKey());
	}

	@Override
	public Item getDropItem() {
		return (switch (getVariant()) {
			case SPRUCE -> SCContent.SPRUCE_SECURITY_SEA_BOAT;
			case BIRCH -> SCContent.BIRCH_SECURITY_SEA_BOAT;
			case JUNGLE -> SCContent.JUNGLE_SECURITY_SEA_BOAT;
			case ACACIA -> SCContent.ACACIA_SECURITY_SEA_BOAT;
			case DARK_OAK -> SCContent.DARK_OAK_SECURITY_SEA_BOAT;
			case MANGROVE -> SCContent.MANGROVE_SECURITY_SEA_BOAT;
			case CHERRY -> SCContent.CHERRY_SECURITY_SEA_BOAT;
			case BAMBOO -> SCContent.BAMBOO_SECURITY_SEA_RAFT;
			default -> SCContent.OAK_SECURITY_SEA_BOAT;
		}).get();
	}

	@Override
	public void remove(RemovalReason reason) {
		if (!level().isClientSide && reason.shouldDestroy())
			Containers.dropContents(level(), blockPosition(), modules);

		super.remove(reason);
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		CompoundTag ownerTag = new CompoundTag();
		long cooldownLeft;

		super.addAdditionalSaveData(tag);
		writeModuleInventory(tag);
		writeModuleStates(tag);
		writeOptions(tag);
		cooldownLeft = getCooldownEnd() - System.currentTimeMillis();
		tag.putLong("cooldownLeft", cooldownLeft <= 0 ? -1 : cooldownLeft);
		getOwner().save(ownerTag, needsValidation());
		tag.put("owner", ownerTag);

		if (saltKey != null)
			tag.putUUID("saltKey", saltKey);

		if (passcode != null)
			tag.putString("passcode", PasscodeUtils.bytesToString(passcode));
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		modules = readModuleInventory(tag);
		moduleStates = readModuleStates(tag);
		readOptions(tag);
		cooldownEnd = System.currentTimeMillis() + tag.getLong("cooldownLeft");
		entityData.set(OWNER, Owner.fromCompound(tag.getCompound("owner")));
		loadSaltKey(tag);
		loadPasscode(tag);
	}

	public void setOwner(Player player) {
		setOwner(player.getGameProfile().getId().toString(), player.getName().getString());
	}

	@Override
	public void setOwner(String uuid, String name) {
		entityData.set(OWNER, new Owner(name, uuid));
	}

	@Override
	public Owner getOwner() {
		return entityData.get(OWNER);
	}

	@Override
	public void onOwnerChanged(BlockState state, Level level, BlockPos pos, Player player) {}

	@Override
	public void activate(Player player) {
		//super is necessary here, because the override doesn't open the screen directly and instead opens the passcode screens
		super.openCustomInventoryScreen(player);
	}

	@Override
	public byte[] getPasscode() {
		return passcode == null || passcode.length == 0 ? null : passcode;
	}

	@Override
	public void setPasscode(byte[] passcode) {
		this.passcode = passcode;
	}

	@Override
	public UUID getSaltKey() {
		return saltKey;
	}

	@Override
	public void setSaltKey(UUID saltKey) {
		this.saltKey = saltKey;
	}

	@Override
	public void startCooldown() {
		if (!isOnCooldown())
			cooldownEnd = System.currentTimeMillis() + smartModuleCooldown.get() * 50;
	}

	@Override
	public boolean isOnCooldown() {
		return System.currentTimeMillis() < getCooldownEnd();
	}

	@Override
	public long getCooldownEnd() {
		return cooldownEnd;
	}

	@Override
	public Option<?>[] customOptions() {
		return new Option[] {
				sendAllowlistMessage, sendDenylistMessage, smartModuleCooldown
		};
	}

	@Override
	public NonNullList<ItemStack> getInventory() {
		return modules;
	}

	@Override
	public ModuleType[] acceptedModules() {
		return new ModuleType[] {
				ModuleType.ALLOWLIST, ModuleType.DENYLIST, ModuleType.SMART, ModuleType.HARMING
		};
	}

	@Override
	public boolean isModuleEnabled(ModuleType module) {
		return hasModule(module) && moduleStates.get(module) == Boolean.TRUE; //prevent NPE
	}

	@Override
	public void toggleModuleState(ModuleType module, boolean shouldBeEnabled) {
		moduleStates.put(module, shouldBeEnabled);
	}

	@Override
	public Level myLevel() {
		return level();
	}

	@Override
	public BlockPos myPos() {
		return blockPosition();
	}

	public boolean sendsAllowlistMessage() {
		return sendAllowlistMessage.get();
	}

	public void setSendsAllowlistMessage(boolean value) {
		sendAllowlistMessage.setValue(value);
	}

	public boolean sendsDenylistMessage() {
		return sendDenylistMessage.get();
	}

	public void setSendsDenylistMessage(boolean value) {
		sendDenylistMessage.setValue(value);
	}
}
