package net.geforcemods.securitycraft.network.server;

import java.util.function.Supplier;

import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.inventory.BriefcaseContainer;
import net.geforcemods.securitycraft.inventory.BriefcaseMenu;
import net.geforcemods.securitycraft.util.PlayerUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkHooks;

public class OpenBriefcaseInventory {
	private ITextComponent name;

	public OpenBriefcaseInventory() {}

	public OpenBriefcaseInventory(ITextComponent name) {
		this.name = name;
	}

	public static void encode(OpenBriefcaseInventory message, PacketBuffer buf) {
		buf.writeComponent(message.name);
	}

	public static OpenBriefcaseInventory decode(PacketBuffer buf) {
		OpenBriefcaseInventory message = new OpenBriefcaseInventory();

		message.name = buf.readComponent();
		return message;
	}

	public static void onMessage(OpenBriefcaseInventory message, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayerEntity player = ctx.get().getSender();
			BlockPos pos = player.blockPosition();

			if (PlayerUtils.isHoldingItem(player, SCContent.BRIEFCASE.get(), null)) {
				NetworkHooks.openGui(player, new INamedContainerProvider() {
					@Override
					public Container createMenu(int windowId, PlayerInventory inv, PlayerEntity player) {
						return new BriefcaseMenu(windowId, inv, new BriefcaseContainer(PlayerUtils.getSelectedItemStack(player, SCContent.BRIEFCASE.get())));
					}

					@Override
					public ITextComponent getDisplayName() {
						return message.name;
					}
				}, pos);
			}
		});

		ctx.get().setPacketHandled(true);
	}
}
