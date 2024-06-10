package net.geforcemods.securitycraft.screen.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.geforcemods.securitycraft.screen.components.SSSConnectionList.ConnectionAccessor;
import net.geforcemods.securitycraft.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.INameable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.client.gui.ScrollPanel;

public class SSSConnectionList<T extends Screen & ConnectionAccessor> extends ScrollPanel {
	private static final ResourceLocation BEACON_GUI = new ResourceLocation("textures/gui/container/beacon.png");
	private static final int SLOT_HEIGHT = 12;
	private final T parent;
	private final List<ConnectionInfo> connectionInfo = new ArrayList<>();
	private final FontRenderer font;

	public SSSConnectionList(T parent, Minecraft client, int width, int height, int top, int left) {
		super(client, width, height, top, left);
		this.parent = parent;
		font = client.font;
		refreshPositions();
	}

	public void refreshPositions() {
		World level = Minecraft.getInstance().level;

		connectionInfo.clear();

		for (BlockPos pos : parent.getPositions()) {
			TileEntity be = level.getBlockEntity(pos);
			ITextComponent blockName;

			if (be instanceof INameable)
				blockName = ((INameable) be).getDisplayName();
			else if (be != null)
				blockName = Utils.localize(be.getBlockState().getBlock().getDescriptionId());
			else
				blockName = new StringTextComponent("????");

			connectionInfo.add(new ConnectionInfo(pos, blockName));
		}
	}

	@Override
	protected int getContentHeight() {
		int height = connectionInfo.size() * 12;

		if (height < bottom - top - 4)
			height = bottom - top - 4;

		return height;
	}

	@Override
	protected void drawPanel(MatrixStack pose, int entryRight, int relativeY, Tessellator tesselator, int mouseX, int mouseY) {
		int baseY = top + border - (int) scrollDistance;
		int slotBuffer = SLOT_HEIGHT - 4;
		int mouseListY = (int) (mouseY - top + scrollDistance - (border / 2));
		int slotIndex = mouseListY / SLOT_HEIGHT;

		//highlight hovered slot
		if (mouseX >= left && mouseX <= right - 7 && slotIndex >= 0 && mouseListY >= 0 && slotIndex < connectionInfo.size() && mouseY >= top && mouseY <= bottom) {
			int min = left;
			int max = entryRight - 6; //6 is the width of the scrollbar
			int slotTop = baseY + slotIndex * SLOT_HEIGHT;
			BufferBuilder bufferBuilder = tesselator.getBuilder();

			RenderSystem.enableBlend();
			RenderSystem.disableTexture();
			RenderSystem.defaultBlendFunc();
			bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
			bufferBuilder.vertex(min, slotTop + slotBuffer + 2, 0).color(0x80, 0x80, 0x80, 0xFF).endVertex();
			bufferBuilder.vertex(max, slotTop + slotBuffer + 2, 0).color(0x80, 0x80, 0x80, 0xFF).endVertex();
			bufferBuilder.vertex(max, slotTop - 2, 0).color(0x80, 0x80, 0x80, 0xFF).endVertex();
			bufferBuilder.vertex(min, slotTop - 2, 0).color(0x80, 0x80, 0x80, 0xFF).endVertex();
			bufferBuilder.vertex(min + 1, slotTop + slotBuffer + 1, 0).color(0x00, 0x00, 0x00, 0xFF).endVertex();
			bufferBuilder.vertex(max - 1, slotTop + slotBuffer + 1, 0).color(0x00, 0x00, 0x00, 0xFF).endVertex();
			bufferBuilder.vertex(max - 1, slotTop - 1, 0).color(0x00, 0x00, 0x00, 0xFF).endVertex();
			bufferBuilder.vertex(min + 1, slotTop - 1, 0).color(0x00, 0x00, 0x00, 0xFF).endVertex();
			bufferBuilder.end();
			WorldVertexBufferUploader.end(bufferBuilder);
			RenderSystem.enableTexture();
			RenderSystem.disableBlend();

			Minecraft.getInstance().getTextureManager().bind(BEACON_GUI);
			blit(pose, left, slotTop - 3, 14, 14, 110, 219, 21, 22, 256, 256);
		}

		int i = 0;

		for (ConnectionInfo info : connectionInfo) {
			int yStart = relativeY + (SLOT_HEIGHT * i++);

			font.draw(pose, info.blockName, left + 13, yStart, 0xC6C6C6);
		}
	}

	@Override
	public void render(MatrixStack pose, int mouseX, int mouseY, float partialTicks) {
		super.render(pose, mouseX, mouseY, partialTicks);

		//draw tooltip for long block names
		int mouseListY = (int) (mouseY - top + scrollDistance - (border / 2));
		int slotIndex = mouseListY / SLOT_HEIGHT;

		if (slotIndex >= 0 && slotIndex < connectionInfo.size() && mouseListY >= 0 && mouseX >= left && mouseX < right - 6 && mouseY >= top && mouseY <= bottom) {
			ITextComponent blockName = connectionInfo.get(slotIndex).blockName;
			int length = font.width(blockName);
			int baseY = top + border - (int) scrollDistance;

			if (length + 13 >= width - 6) //6 = barWidth
				parent.renderTooltip(pose, blockName, left + 1, baseY + (SLOT_HEIGHT * slotIndex + SLOT_HEIGHT));

			font.draw(pose, Utils.getFormattedCoordinates(connectionInfo.get(slotIndex).pos), left + 13, top + height + 5, 4210752);
		}
	}

	@Override
	protected boolean clickPanel(double mouseX, double mouseY, int button) {
		int slotIndex = (int) (mouseY + (border / 2)) / SLOT_HEIGHT;

		if (slotIndex >= 0 && slotIndex < connectionInfo.size()) {
			Minecraft mc = Minecraft.getInstance();
			double relativeMouseY = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();

			if (relativeMouseY >= top && relativeMouseY <= bottom && mouseX < 13) {
				parent.removePosition(connectionInfo.get(slotIndex).pos);
				Minecraft.getInstance().getSoundManager().play(SimpleSound.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				return true;
			}
		}

		return false;
	}

	public interface ConnectionAccessor {
		public Set<BlockPos> getPositions();

		public void removePosition(BlockPos pos);
	}

	private class ConnectionInfo {
		private final BlockPos pos;
		private final ITextComponent blockName;

		public ConnectionInfo(BlockPos pos, ITextComponent blockName) {
			this.pos = pos;
			this.blockName = blockName;
		}
	}
}