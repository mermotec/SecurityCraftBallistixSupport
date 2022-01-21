package net.geforcemods.securitycraft.screen.components;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.client.gui.GuiUtils;
import net.minecraftforge.fml.client.gui.widget.ExtendedButton;

@OnlyIn(Dist.CLIENT)
public class PictureButton extends ExtendedButton {
	private final ItemRenderer itemRenderer;
	private Block blockToRender;
	private Item itemToRender;
	private ResourceLocation textureLocation;
	private int u;
	private int v;
	private int drawOffsetX;
	private int drawOffsetY;
	private int drawWidth;
	private int drawHeight;
	private int textureWidth;
	private int textureHeight;

	public PictureButton(int xPos, int yPos, int width, int height, ItemRenderer itemRenderer, ItemStack itemToRender) {
		this(xPos, yPos, width, height, itemRenderer, itemToRender, b -> {});
	}

	public PictureButton(int xPos, int yPos, int width, int height, ItemRenderer itemRenderer, ItemStack itemToRender, IPressable onClick) {
		super(xPos, yPos, width, height, "", onClick);
		this.itemRenderer = itemRenderer;

		if (!itemToRender.isEmpty() && itemToRender.getItem() instanceof BlockItem)
			blockToRender = Block.getBlockFromItem(itemToRender.getItem());
		else
			this.itemToRender = itemToRender.getItem();
	}

	public PictureButton(int xPos, int yPos, int width, int height, ResourceLocation texture, int textureX, int textureY, int drawOffsetX, int drawOffsetY, int drawWidth, int drawHeight, int textureWidth, int textureHeight, IPressable onClick) {
		super(xPos, yPos, width, height, "", onClick);

		itemRenderer = null;
		textureLocation = texture;
		u = textureX;
		v = textureY;
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
		this.drawOffsetX = drawOffsetX;
		this.drawOffsetY = drawOffsetY;
		this.drawWidth = drawWidth;
		this.drawHeight = drawHeight;
	}

	@Override
	public void render(int mouseX, int mouseY, float partialTicks) {
		if (visible) {
			Minecraft mc = Minecraft.getInstance();
			FontRenderer font = mc.fontRenderer;

			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			isHovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
			GuiUtils.drawContinuousTexturedBox(WIDGETS_LOCATION, x, y, 0, 46 + getYImage(isHovered()) * 20, width, height, 200, 20, 2, 3, 2, 2, getBlitOffset());

			if (blockToRender != null) {
				RenderSystem.enableRescaleNormal();
				itemRenderer.renderItemAndEffectIntoGUI(new ItemStack(blockToRender), x + 2, y + 3);
				itemRenderer.renderItemOverlayIntoGUI(font, new ItemStack(blockToRender), x + 2, y + 3, "");
			}
			else if (itemToRender != null) {
				RenderSystem.enableRescaleNormal();
				itemRenderer.renderItemAndEffectIntoGUI(new ItemStack(itemToRender), x + 2, y + 2);
				itemRenderer.renderItemOverlayIntoGUI(font, new ItemStack(itemToRender), x + 2, y + 2, "");
				RenderSystem.disableLighting();
			}
			else {
				ResourceLocation texture = getTextureLocation();

				if (texture != null) {
					RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
					mc.getTextureManager().bindTexture(texture);
					blit(x + drawOffsetX, y + drawOffsetY, drawWidth, drawHeight, u, v, drawWidth, drawHeight, textureWidth, textureHeight);
				}
			}
		}
	}

	public ResourceLocation getTextureLocation() {
		return textureLocation;
	}

	public Item getItemStack() {
		return (blockToRender != null ? blockToRender.asItem() : itemToRender);
	}
}
