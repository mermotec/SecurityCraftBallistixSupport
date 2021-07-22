package net.geforcemods.securitycraft.network.server;

import java.util.function.Supplier;

import net.geforcemods.securitycraft.containers.KeycardReaderContainer;
import net.geforcemods.securitycraft.tileentity.KeycardReaderTileEntity;
import net.geforcemods.securitycraft.util.ModuleUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

public class SyncKeycardSettings
{
	private BlockPos pos;
	private int signature;
	private boolean[] acceptedLevels;
	private boolean link;

	public SyncKeycardSettings() {}

	public SyncKeycardSettings(BlockPos pos, boolean[] acceptedLevels, int signature, boolean link)
	{
		this.pos = pos;
		this.acceptedLevels = acceptedLevels;
		this.signature = signature;
		this.link = link;
	}

	public static void encode(SyncKeycardSettings message, FriendlyByteBuf buf)
	{
		buf.writeBlockPos(message.pos);
		buf.writeVarInt(message.signature);
		buf.writeBoolean(message.link);

		for(int i = 0; i < 5; i++)
		{
			buf.writeBoolean(message.acceptedLevels[i]);
		}
	}

	public static SyncKeycardSettings decode(FriendlyByteBuf buf)
	{
		SyncKeycardSettings message = new SyncKeycardSettings();

		message.pos = buf.readBlockPos();
		message.signature = buf.readVarInt();
		message.link = buf.readBoolean();
		message.acceptedLevels = new boolean[5];

		for(int i = 0; i < 5; i++)
		{
			message.acceptedLevels[i] = buf.readBoolean();
		}

		return message;
	}

	public static void onMessage(SyncKeycardSettings message, Supplier<NetworkEvent.Context> ctx)
	{
		ctx.get().enqueueWork(() -> {
			BlockPos pos = message.pos;
			Player player = ctx.get().getSender();
			BlockEntity tile = player.level.getBlockEntity(pos);

			if(tile instanceof KeycardReaderTileEntity)
			{
				KeycardReaderTileEntity te = (KeycardReaderTileEntity)tile;
				boolean isOwner = te.getOwner().isOwner(player);

				if(isOwner || ModuleUtils.isAllowed(te, player))
				{
					if(isOwner)
					{
						te.setAcceptedLevels(message.acceptedLevels);
						te.setSignature(message.signature);
					}

					if(message.link)
					{
						AbstractContainerMenu container = player.containerMenu;

						if(container instanceof KeycardReaderContainer)
							((KeycardReaderContainer)container).link();
					}
				}
			}
		});

		ctx.get().setPacketHandled(true);
	}
}
