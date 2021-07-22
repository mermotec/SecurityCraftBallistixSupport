package net.geforcemods.securitycraft.network.server;

import java.util.function.Supplier;

import net.geforcemods.securitycraft.api.CustomizableTileEntity;
import net.geforcemods.securitycraft.api.ICustomizable;
import net.geforcemods.securitycraft.api.IOwnable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

public class ToggleOption {

	private int x, y, z, id;

	public ToggleOption(){ }

	public ToggleOption(int x, int y, int z, int id){
		this.x = x;
		this.y = y;
		this.z = z;
		this.id = id;
	}

	public static void encode(ToggleOption message, FriendlyByteBuf buf)
	{
		buf.writeInt(message.x);
		buf.writeInt(message.y);
		buf.writeInt(message.z);
		buf.writeInt(message.id);
	}

	public static ToggleOption decode(FriendlyByteBuf buf)
	{
		ToggleOption message = new ToggleOption();

		message.x = buf.readInt();
		message.y = buf.readInt();
		message.z = buf.readInt();
		message.id = buf.readInt();
		return message;
	}

	public static void onMessage(ToggleOption message, Supplier<NetworkEvent.Context> ctx)
	{
		ctx.get().enqueueWork(() -> {
			BlockPos pos = new BlockPos(message.x, message.y, message.z);
			int id = message.id;
			Player player = ctx.get().getSender();
			BlockEntity te = player.level.getBlockEntity(pos);

			if(te instanceof ICustomizable && (!(te instanceof IOwnable) || ((IOwnable)te).getOwner().isOwner(player))) {
				((ICustomizable)te).customOptions()[id].toggle();
				((ICustomizable)te).onOptionChanged(((ICustomizable)te).customOptions()[id]);

				if(te instanceof CustomizableTileEntity)
					((CustomizableTileEntity)te).sync();
			}
		});

		ctx.get().setPacketHandled(true);
	}
}
