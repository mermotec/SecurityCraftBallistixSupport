package net.geforcemods.securitycraft.entity;

import net.geforcemods.securitycraft.ConfigHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EntityBouncingBetty extends Entity {
	/** How long the fuse is */
	public int fuse;

	public EntityBouncingBetty(World world) {
		super(world);
		preventEntitySpawning = true;
		setSize(0.500F, 0.200F);
	}

	public EntityBouncingBetty(World world, double x, double y, double z) {
		this(world);
		setPosition(x, y, z);
		float f = (float) (Math.random() * Math.PI * 2.0D);
		motionX = -((float) Math.sin(f)) * 0.02F;
		motionY = 0.20000000298023224D;
		motionZ = -((float) Math.cos(f)) * 0.02F;
		fuse = 80;
		prevPosX = x;
		prevPosY = y;
		prevPosZ = z;
	}

	@Override
	protected void entityInit() {}

	@Override
	protected boolean canTriggerWalking() {
		return false;
	}

	@Override
	public boolean canBeCollidedWith() {
		return !isDead;
	}

	@Override
	public void onUpdate() {
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;
		motionY -= 0.03999999910593033D;
		move(MoverType.SELF, motionX, motionY, motionZ);
		motionX *= 0.9800000190734863D;
		motionY *= 0.9800000190734863D;
		motionZ *= 0.9800000190734863D;

		if (onGround) {
			motionX *= 0.699999988079071D;
			motionZ *= 0.699999988079071D;
			motionY *= -0.5D;
		}

		if (!world.isRemote && fuse-- <= 0) {
			setDead();
			explode();
		}
		else
			world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, posX, posY + 0.5D, posZ, 0.0D, 0.0D, 0.0D);
	}

	private void explode() {
		world.newExplosion(this, posX, posY, posZ, ConfigHandler.smallerMineExplosion ? 3.0F : 6.0F, ConfigHandler.shouldSpawnFire, ConfigHandler.mineExplosionsBreakBlocks);
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {
		tag.setByte("Fuse", (byte) fuse);
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
		fuse = tag.getByte("Fuse");
	}

	@SideOnly(Side.CLIENT)
	public float getShadowSize() {
		return 0.0F;
	}
}