package net.geforcemods.securitycraft.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.geforcemods.securitycraft.SecurityCraft;
import net.geforcemods.securitycraft.inventory.BriefcaseMenu;
import net.geforcemods.securitycraft.inventory.KeycardHolderMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class ItemInventoryScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
	protected ResourceLocation texture;

	public ItemInventoryScreen(T menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = imageWidth / 2 - font.width(title) / 2;
		inventoryLabelY = imageHeight - 94;
	}

	@Override
	public void render(PoseStack pose, int mouseX, int mouseY, float partialTicks) {
		super.render(pose, mouseX, mouseY, partialTicks);

		if (getSlotUnderMouse() != null && !getSlotUnderMouse().getItem().isEmpty())
			renderTooltip(pose, getSlotUnderMouse().getItem(), mouseX, mouseY);
	}

	@Override
	protected void renderBg(PoseStack pose, float partialTicks, int mouseX, int mouseY) {
		renderBackground(pose);
		RenderSystem._setShaderTexture(0, texture);
		blit(pose, leftPos, topPos, 0, 0, imageWidth, imageHeight);
	}

	public static class Briefcase extends ItemInventoryScreen<BriefcaseMenu> {
		public Briefcase(BriefcaseMenu menu, Inventory inventory, Component title) {
			super(menu, inventory, title);
			texture = new ResourceLocation(SecurityCraft.MODID, "textures/gui/container/briefcase_inventory.png");
		}
	}

	public static class KeycardHolder extends ItemInventoryScreen<KeycardHolderMenu> {
		public KeycardHolder(KeycardHolderMenu menu, Inventory inventory, Component title) {
			super(menu, inventory, title);
			texture = new ResourceLocation(SecurityCraft.MODID, "textures/gui/container/keycard_holder.png");
			imageHeight = 133;
		}
	}
}
