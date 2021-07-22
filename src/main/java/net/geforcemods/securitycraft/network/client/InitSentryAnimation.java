package net.geforcemods.securitycraft.network.client;

import java.util.List;
import java.util.function.Supplier;

import net.geforcemods.securitycraft.entity.SentryEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

public class InitSentryAnimation
{
	private BlockPos pos;
	private boolean animate, animateUpwards;

	public InitSentryAnimation() {}

	public InitSentryAnimation(BlockPos sentryPos, boolean animate, boolean animateUpwards)
	{
		pos = sentryPos;
		this.animate = animate;
		this.animateUpwards = animateUpwards;
	}

	public static void encode(InitSentryAnimation message, FriendlyByteBuf buf)
	{
		buf.writeLong(message.pos.asLong());
		buf.writeBoolean(message.animate);
		buf.writeBoolean(message.animateUpwards);
	}

	public static InitSentryAnimation decode(FriendlyByteBuf buf)
	{
		InitSentryAnimation message = new InitSentryAnimation();

		message.pos = BlockPos.of(buf.readLong());
		message.animate = buf.readBoolean();
		message.animateUpwards = buf.readBoolean();
		return message;
	}

	public static void onMessage(InitSentryAnimation message, Supplier<NetworkEvent.Context> ctx)
	{
		ctx.get().enqueueWork(() -> {
			List<PathfinderMob> sentries = Minecraft.getInstance().level.<PathfinderMob>getEntitiesOfClass(SentryEntity.class, new AABB(message.pos));

			if(!sentries.isEmpty())
			{
				((SentryEntity)sentries.get(0)).animateUpwards = message.animateUpwards;
				((SentryEntity)sentries.get(0)).animate = message.animate;
			}
		});

		ctx.get().setPacketHandled(true);
	}
}
