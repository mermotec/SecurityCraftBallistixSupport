package net.geforcemods.securitycraft.screen;

import java.util.ArrayDeque;
import java.util.Deque;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.geforcemods.securitycraft.SecurityCraft;
import net.geforcemods.securitycraft.items.ModuleItem;
import net.geforcemods.securitycraft.network.server.UpdateNBTTagOnServer;
import net.geforcemods.securitycraft.screen.components.CallbackCheckbox;
import net.geforcemods.securitycraft.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.ScrollPanel;
import net.minecraftforge.fml.client.gui.widget.ExtendedButton;

@OnlyIn(Dist.CLIENT)
public class EditModuleScreen extends Screen {
	private static CompoundNBT savedModule;
	private static final ResourceLocation TEXTURE = new ResourceLocation("securitycraft:textures/gui/container/edit_module.png");
	private final String editModule = Utils.localize("gui.securitycraft:editModule").getFormattedText();
	private final ItemStack module;
	private TextFieldWidget inputField;
	private Button addButton, removeButton, copyButton, pasteButton, clearButton;
	private int xSize = 247, ySize = 186;
	private PlayerList playerList;
	private int guiLeft;

	public EditModuleScreen(ItemStack item) {
		super(new TranslationTextComponent(item.getTranslationKey()));

		module = item;
	}

	@Override
	public void init() {
		super.init();

		guiLeft = (width - xSize) / 2;

		int guiTop = (height - ySize) / 2;
		int controlsStartX = (int) (guiLeft + xSize * (3.0F / 4.0F)) - 43;
		String checkboxText = Utils.localize("gui.securitycraft:editModule.affectEveryone").getFormattedText();
		int length = font.getStringWidth(checkboxText) + 24; //24 = checkbox width + 4 pixels of buffer

		minecraft.keyboardListener.enableRepeatEvents(true);
		addButton(inputField = new TextFieldWidget(font, controlsStartX - 17, height / 2 - 75, 110, 15, ""));
		addButton(addButton = new ExtendedButton(controlsStartX, height / 2 - 55, 76, 20, Utils.localize("gui.securitycraft:editModule.add").getFormattedText(), this::addButtonClicked));
		addButton(removeButton = new ExtendedButton(controlsStartX, height / 2 - 30, 76, 20, Utils.localize("gui.securitycraft:editModule.remove").getFormattedText(), this::removeButtonClicked));
		addButton(copyButton = new ExtendedButton(controlsStartX, height / 2 - 5, 76, 20, Utils.localize("gui.securitycraft:editModule.copy").getFormattedText(), this::copyButtonClicked));
		addButton(pasteButton = new ExtendedButton(controlsStartX, height / 2 + 20, 76, 20, Utils.localize("gui.securitycraft:editModule.paste").getFormattedText(), this::pasteButtonClicked));
		addButton(clearButton = new ExtendedButton(controlsStartX, height / 2 + 45, 76, 20, Utils.localize("gui.securitycraft:editModule.clear").getFormattedText(), this::clearButtonClicked));
		addButton(new CallbackCheckbox(guiLeft + xSize / 2 - length / 2, guiTop + ySize - 25, 20, 20, checkboxText, module.hasTag() && module.getTag().getBoolean("affectEveryone"), newState -> {
			module.getOrCreateTag().putBoolean("affectEveryone", newState);
			SecurityCraft.channel.sendToServer(new UpdateNBTTagOnServer(module));
		}, 0x404040));
		children.add(playerList = new PlayerList(minecraft, 110, 141, height / 2 - 76, guiLeft + 10));

		addButton.active = false;
		removeButton.active = false;

		if (module.getTag() == null || module.getTag().isEmpty() || (module.getTag() != null && module.getTag().equals(savedModule)))
			copyButton.active = false;

		if (savedModule == null || savedModule.isEmpty() || (module.getTag() != null && module.getTag().equals(savedModule)))
			pasteButton.active = false;

		if (module.getTag() == null || module.getTag().isEmpty())
			clearButton.active = false;

		inputField.setMaxStringLength(16);
		inputField.setValidator(s -> !s.contains(" "));
		inputField.setResponder(s -> {
			if (s.isEmpty())
				addButton.active = false;
			else {
				if (module.hasTag()) {
					for (int i = 1; i <= ModuleItem.MAX_PLAYERS; i++) {
						if (s.equals(module.getTag().getString("Player" + i))) {
							addButton.active = false;
							removeButton.active = true;
							playerList.setSelectedIndex(i - 1);
							return;
						}
					}
				}

				addButton.active = true;
			}

			removeButton.active = false;
			playerList.setSelectedIndex(-1);
		});
		setFocusedDefault(inputField);
	}

	@Override
	public void onClose() {
		super.onClose();

		if (minecraft != null)
			minecraft.keyboardListener.enableRepeatEvents(false);
	}

	@Override
	public void render(int mouseX, int mouseY, float partialTicks) {
		renderBackground();
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		minecraft.getTextureManager().bindTexture(TEXTURE);
		int startX = (width - xSize) / 2;
		int startY = (height - ySize) / 2;
		blit(startX, startY, 0, 0, xSize, ySize);
		super.render(mouseX, mouseY, partialTicks);
		font.drawSplitString(editModule, startX + xSize / 2 - font.getStringWidth(editModule) / 2, startY + 6, width, 4210752);

		if (playerList != null)
			playerList.render(mouseX, mouseY, partialTicks);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
		if (playerList != null && playerList.isMouseOver(mouseX, mouseY))
			playerList.mouseScrolled(mouseX, mouseY, scroll);

		return super.mouseScrolled(mouseX, mouseY, scroll);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (playerList != null)
			playerList.mouseClicked(mouseX, mouseY, button);

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (playerList != null)
			playerList.mouseReleased(mouseX, mouseY, button);

		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (playerList != null)
			playerList.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);

		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	private void addButtonClicked(Button button) {
		if (inputField.getText().isEmpty())
			return;

		if (module.getTag() == null)
			module.setTag(new CompoundNBT());

		for (int i = 1; i <= ModuleItem.MAX_PLAYERS; i++) {
			if (module.getTag().contains("Player" + i) && module.getTag().getString("Player" + i).equals(inputField.getText())) {
				if (i == 9)
					addButton.active = false;

				return;
			}
		}

		module.getTag().putString("Player" + getNextFreeSlot(module.getTag()), inputField.getText());

		if (module.getTag() != null && module.getTag().contains("Player" + ModuleItem.MAX_PLAYERS))
			addButton.active = false;

		inputField.setText("");
		updateButtonStates();
	}

	private void removeButtonClicked(Button button) {
		if (inputField.getText().isEmpty())
			return;

		if (module.getTag() == null)
			module.setTag(new CompoundNBT());

		for (int i = 1; i <= ModuleItem.MAX_PLAYERS; i++) {
			if (module.getTag().contains("Player" + i) && module.getTag().getString("Player" + i).equals(inputField.getText())) {
				module.getTag().remove("Player" + i);
				defragmentTag(module.getTag());
			}
		}

		inputField.setText("");
		updateButtonStates();
	}

	private void copyButtonClicked(Button button) {
		savedModule = module.getTag().copy();
		copyButton.active = false;
		updateButtonStates();
	}

	private void pasteButtonClicked(Button button) {
		module.setTag(savedModule.copy());
		updateButtonStates();
	}

	private void clearButtonClicked(Button button) {
		module.setTag(new CompoundNBT());
		inputField.setText("");
		updateButtonStates();
	}

	private void updateButtonStates() {
		if (module.getTag() != null)
			SecurityCraft.channel.sendToServer(new UpdateNBTTagOnServer(module));

		addButton.active = module.getTag() != null && !module.getTag().contains("Player" + ModuleItem.MAX_PLAYERS) && !inputField.getText().isEmpty();
		removeButton.active = !(module.getTag() == null || module.getTag().isEmpty() || inputField.getText().isEmpty());
		copyButton.active = !(module.getTag() == null || module.getTag().isEmpty() || (module.getTag() != null && module.getTag().equals(savedModule)));
		pasteButton.active = !(savedModule == null || savedModule.isEmpty() || (module.getTag() != null && module.getTag().equals(savedModule)));
		clearButton.active = !(module.getTag() == null || module.getTag().isEmpty());
	}

	private int getNextFreeSlot(CompoundNBT tag) {
		for (int i = 1; i <= ModuleItem.MAX_PLAYERS; i++) {
			if (!tag.contains("Player" + i) || tag.getString("Player" + i).isEmpty())
				return i;
		}

		return 0;
	}

	private void defragmentTag(CompoundNBT tag) {
		Deque<Integer> freeIndices = new ArrayDeque<>();

		for (int i = 1; i <= ModuleItem.MAX_PLAYERS; i++) {
			if (!tag.contains("Player" + i) || tag.getString("Player" + i).isEmpty())
				freeIndices.add(i);
			else if (!freeIndices.isEmpty()) {
				String player = tag.getString("Player" + i);
				int nextFreeIndex = freeIndices.poll();

				tag.putString("Player" + nextFreeIndex, player);
				tag.remove("Player" + i);
				freeIndices.add(i);
			}
		}
	}

	class PlayerList extends ScrollPanel {
		private final int slotHeight = 12, listLength = ModuleItem.MAX_PLAYERS;
		private int selectedIndex = -1;

		public PlayerList(Minecraft client, int width, int height, int top, int left) {
			super(client, width, height, top, left);
		}

		@Override
		protected int getContentHeight() {
			int height = 50 + (listLength * font.FONT_HEIGHT);

			if (height < bottom - top - 8)
				height = bottom - top - 8;

			return height;
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (isMouseOver(mouseX, mouseY) && mouseX < left + width - 6) {
				int clickedIndex = ((int) (mouseY - top + scrollDistance - border)) / slotHeight;

				if (module.hasTag() && module.getTag().contains("Player" + (clickedIndex + 1))) {
					selectedIndex = clickedIndex;
					inputField.setText(module.getTag().getString("Player" + (clickedIndex + 1)));
				}
			}

			return super.mouseClicked(mouseX, mouseY, button);
		}

		@Override
		protected void drawPanel(int entryRight, int relativeY, Tessellator tessellator, int mouseX, int mouseY) {
			if (module.hasTag()) {
				CompoundNBT tag = module.getTag();
				int baseY = top + border - (int) scrollDistance;
				int mouseListY = (int) (mouseY - top + scrollDistance - border);
				int slotIndex = mouseListY / slotHeight;

				//highlight hovered slot
				if (slotIndex != selectedIndex && mouseX >= left && mouseX < right - 6 && slotIndex >= 0 && mouseListY >= 0 && slotIndex < listLength && mouseY >= top && mouseY <= bottom) {
					if (tag.contains("Player" + (slotIndex + 1)) && !tag.getString("Player" + (slotIndex + 1)).isEmpty())
						renderBox(tessellator.getBuffer(), left, entryRight - 6, baseY + slotIndex * slotHeight, slotHeight - 4, 0x80);
				}

				if (selectedIndex >= 0)
					renderBox(tessellator.getBuffer(), left, entryRight - 6, baseY + selectedIndex * slotHeight, slotHeight - 4, 0xFF);

				//draw entry strings
				for (int i = 0; i < ModuleItem.MAX_PLAYERS; i++) {
					if (tag.contains("Player" + (i + 1))) {
						String name = tag.getString("Player" + (i + 1));

						if (!name.isEmpty())
							font.drawString(name, left - 2 + width / 2 - font.getStringWidth(name) / 2, relativeY + (slotHeight * i), 0xC6C6C6);
					}
				}
			}
		}

		private void renderBox(BufferBuilder bufferBuilder, int min, int max, int slotTop, int slotBuffer, int borderColor) {
			RenderSystem.enableBlend();
			RenderSystem.disableTexture();
			RenderSystem.defaultBlendFunc();
			bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
			bufferBuilder.pos(min, slotTop + slotBuffer + 2, 0).tex(0, 1).color(borderColor, borderColor, borderColor, 0xFF).endVertex();
			bufferBuilder.pos(max, slotTop + slotBuffer + 2, 0).tex(1, 1).color(borderColor, borderColor, borderColor, 0xFF).endVertex();
			bufferBuilder.pos(max, slotTop - 2, 0).tex(1, 0).color(borderColor, borderColor, borderColor, 0xFF).endVertex();
			bufferBuilder.pos(min, slotTop - 2, 0).tex(0, 0).color(borderColor, borderColor, borderColor, 0xFF).endVertex();
			bufferBuilder.pos(min + 1, slotTop + slotBuffer + 1, 0).tex(0, 1).color(0x00, 0x00, 0x00, 0xFF).endVertex();
			bufferBuilder.pos(max - 1, slotTop + slotBuffer + 1, 0).tex(1, 1).color(0x00, 0x00, 0x00, 0xFF).endVertex();
			bufferBuilder.pos(max - 1, slotTop - 1, 0).tex(1, 0).color(0x00, 0x00, 0x00, 0xFF).endVertex();
			bufferBuilder.pos(min + 1, slotTop - 1, 0).tex(0, 0).color(0x00, 0x00, 0x00, 0xFF).endVertex();
			bufferBuilder.finishDrawing();
			WorldVertexBufferUploader.draw(bufferBuilder);
			RenderSystem.enableTexture();
			RenderSystem.disableBlend();
		}

		public void setSelectedIndex(int selectedIndex) {
			this.selectedIndex = selectedIndex;
		}
	}
}
