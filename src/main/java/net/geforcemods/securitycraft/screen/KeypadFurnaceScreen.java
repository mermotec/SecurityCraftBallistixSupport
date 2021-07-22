package net.geforcemods.securitycraft.screen;

import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.geforcemods.securitycraft.containers.KeypadFurnaceContainer;
import net.geforcemods.securitycraft.util.Utils;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class KeypadFurnaceScreen extends AbstractContainerScreen<KeypadFurnaceContainer>
{
	private static final ResourceLocation FURNACE_GUI_TEXTURES = new ResourceLocation("textures/gui/container/furnace.png");
	private Component title;

	public KeypadFurnaceScreen(KeypadFurnaceContainer container, Inventory inv, Component name)
	{
		super(container, inv, name);

		title = new Random().nextInt(100) < 5 ? new TextComponent("Keypad Gurnace")
				: (container.te.hasCustomSCName() ? container.te.getCustomSCName() : Utils.localize("gui.securitycraft:protectedFurnace.name"));
	}

	@Override
	public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks)
	{
		super.render(matrix, mouseX, mouseY, partialTicks);
		renderTooltip(matrix, mouseX, mouseY);
	}

	@Override
	protected void renderLabels(PoseStack matrix, int mouseX, int mouseY)
	{
		font.draw(matrix, title, imageWidth / 2 - font.width(title) / 2, 6.0F, 4210752);
		font.draw(matrix, inventory.getDisplayName().getString(), 8.0F, imageHeight - 96 + 2, 4210752);
	}

	@Override
	protected void renderBg(PoseStack matrix, float partialTicks, int mouseX, int mouseY)
	{
		renderBackground(matrix);
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		minecraft.getTextureManager().bind(FURNACE_GUI_TEXTURES);
		blit(matrix, leftPos, topPos, 0, 0, imageWidth, imageHeight);

		if(((AbstractFurnaceMenu)menu).isLit())
		{
			int burnLeftScaled = ((AbstractFurnaceMenu)menu).getLitProgress();

			blit(matrix, leftPos + 56, topPos + 36 + 12 - burnLeftScaled, 176, 12 - burnLeftScaled, 14, burnLeftScaled + 1);
		}

		blit(matrix, leftPos + 79, topPos + 34, 176, 14, ((AbstractFurnaceMenu)menu).getBurnProgress() + 1, 16);
	}
}