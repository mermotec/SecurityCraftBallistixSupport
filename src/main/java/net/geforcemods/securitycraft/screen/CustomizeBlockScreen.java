package net.geforcemods.securitycraft.screen;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.geforcemods.securitycraft.SecurityCraft;
import net.geforcemods.securitycraft.api.ICustomizable;
import net.geforcemods.securitycraft.api.IModuleInventory;
import net.geforcemods.securitycraft.api.Option;
import net.geforcemods.securitycraft.api.Option.BooleanOption;
import net.geforcemods.securitycraft.api.Option.DoubleOption;
import net.geforcemods.securitycraft.api.Option.IntOption;
import net.geforcemods.securitycraft.inventory.CustomizeBlockMenu;
import net.geforcemods.securitycraft.network.server.ToggleOption;
import net.geforcemods.securitycraft.network.server.UpdateSliderValue;
import net.geforcemods.securitycraft.screen.components.CallbackSlider;
import net.geforcemods.securitycraft.screen.components.PictureButton;
import net.geforcemods.securitycraft.screen.components.TextHoverChecker;
import net.geforcemods.securitycraft.util.IHasExtraAreas;
import net.geforcemods.securitycraft.util.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.widget.ExtendedButton;

public class CustomizeBlockScreen extends AbstractContainerScreen<CustomizeBlockMenu> implements IHasExtraAreas {
	//@formatter:off
	private static final ResourceLocation[] TEXTURES = {
			new ResourceLocation("securitycraft:textures/gui/container/customize0.png"),
			new ResourceLocation("securitycraft:textures/gui/container/customize1.png"),
			new ResourceLocation("securitycraft:textures/gui/container/customize2.png"),
			new ResourceLocation("securitycraft:textures/gui/container/customize3.png"),
			new ResourceLocation("securitycraft:textures/gui/container/customize4.png"),
			new ResourceLocation("securitycraft:textures/gui/container/customize5.png")
	};
	//@formatter:on
	private final List<Rect2i> extraAreas = new ArrayList<>();
	private IModuleInventory moduleInv;
	private PictureButton[] descriptionButtons = new PictureButton[5];
	private AbstractWidget[] optionButtons = new AbstractWidget[5];
	private List<TextHoverChecker> hoverCheckers = new ArrayList<>();
	private final String blockName;
	private final TranslatableComponent name;
	private final int maxNumberOfModules;

	public CustomizeBlockScreen(CustomizeBlockMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		moduleInv = menu.moduleInv;
		blockName = menu.moduleInv.getBlockEntity().getBlockState().getBlock().getDescriptionId().substring(5);
		name = Utils.localize(moduleInv.getBlockEntity().getBlockState().getBlock().getDescriptionId());
		maxNumberOfModules = moduleInv.getMaxNumberOfModules();
	}

	@Override
	public void init() {
		super.init();

		final int numberOfColumns = 2;

		for (int i = 0; i < maxNumberOfModules; i++) {
			int column = i % numberOfColumns;

			addRenderableWidget(descriptionButtons[i] = new PictureButton(leftPos + 127 + column * 22, (topPos + 16) + (Math.floorDiv(i, numberOfColumns) * 22), 20, 20, itemRenderer, new ItemStack(moduleInv.acceptedModules()[i].getItem())));
			hoverCheckers.add(new TextHoverChecker(descriptionButtons[i], getModuleDescription(i)));
		}

		if (moduleInv.getBlockEntity() instanceof ICustomizable customizable) {
			Option<?>[] options = customizable.customOptions();

			if (options != null) {
				for (int i = 0; i < options.length; i++) {
					Option<?> option = options[i];

					if (option.isSlider()) {
						if (option instanceof DoubleOption doubleOption)
							optionButtons[i] = new CallbackSlider(leftPos + 178, (topPos + 10) + (i * 25), 120, 20, Utils.localize("option" + blockName + "." + option.getName(), ""), TextComponent.EMPTY, doubleOption.getMin(), doubleOption.getMax(), doubleOption.get(), doubleOption.getIncrement(), 0, true, slider -> {
								doubleOption.setValue(slider.getValue());
								SecurityCraft.channel.sendToServer(new UpdateSliderValue(doubleOption.getPos(), option, doubleOption.get()));
							});
						else if (option instanceof IntOption intOption)
							optionButtons[i] = new CallbackSlider(leftPos + 178, (topPos + 10) + (i * 25), 120, 20, Utils.localize("option" + blockName + "." + option.getName(), ""), TextComponent.EMPTY, intOption.getMin(), intOption.getMax(), intOption.get(), true, slider -> {
								intOption.setValue(slider.getValueInt());
								SecurityCraft.channel.sendToServer(new UpdateSliderValue(intOption.getPos(), option, intOption.get()));
							});

						optionButtons[i].setFGColor(14737632);
					}
					else {
						optionButtons[i] = new ExtendedButton(leftPos + 178, (topPos + 10) + (i * 25), 120, 20, getOptionButtonTitle(option), this::optionButtonClicked);
						optionButtons[i].setFGColor(option.toString().equals(option.getDefaultValue().toString()) ? 16777120 : 14737632);
					}

					addRenderableWidget(optionButtons[i]);
					hoverCheckers.add(new TextHoverChecker(optionButtons[i], getOptionDescription(i)));
				}
			}
		}

		for (AbstractWidget button : optionButtons) {
			if (button != null)
				extraAreas.add(new Rect2i(button.x, button.y, button.getWidth(), button.getHeight()));
		}
	}

	@Override
	public void render(PoseStack pose, int mouseX, int mouseY, float partialTicks) {
		super.render(pose, mouseX, mouseY, partialTicks);

		if (getSlotUnderMouse() != null && !getSlotUnderMouse().getItem().isEmpty())
			renderTooltip(pose, getSlotUnderMouse().getItem(), mouseX, mouseY);

		for (TextHoverChecker hoverChecker : hoverCheckers) {
			//last check hides the tooltip when a slider is being dragged
			if (hoverChecker != null && hoverChecker.checkHover(mouseX, mouseY) && (!(hoverChecker.getWidget() instanceof CallbackSlider) || !isDragging()))
				renderTooltip(pose, minecraft.font.split(hoverChecker.getName(), 150), mouseX, mouseY);
		}
	}

	@Override
	protected void renderLabels(PoseStack pose, int mouseX, int mouseY) {
		font.draw(pose, name, imageWidth / 2 - font.width(name) / 2, 6, 4210752);
		font.draw(pose, Utils.INVENTORY_TEXT, 8, imageHeight - 96 + 2, 4210752);
	}

	@Override
	protected void renderBg(PoseStack pose, float partialTicks, int mouseX, int mouseY) {
		renderBackground(pose);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem._setShaderTexture(0, TEXTURES[maxNumberOfModules]);
		blit(pose, leftPos, topPos, 0, 0, imageWidth, imageHeight);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		return (getFocused() != null && isDragging() && button == 0 ? getFocused().mouseDragged(mouseX, mouseY, button, dragX, dragY) : false) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	private void optionButtonClicked(Button button) {
		for (int i = 0; i < optionButtons.length; i++) {
			if (button != optionButtons[i])
				continue;

			Option<?> tempOption = ((ICustomizable) moduleInv.getBlockEntity()).customOptions()[i]; //safe cast, as this method is only called when it can be casted

			tempOption.toggle();
			button.setFGColor(tempOption.toString().equals(tempOption.getDefaultValue().toString()) ? 16777120 : 14737632);
			button.setMessage(getOptionButtonTitle(tempOption));
			SecurityCraft.channel.sendToServer(new ToggleOption(moduleInv.getBlockEntity().getBlockPos().getX(), moduleInv.getBlockEntity().getBlockPos().getY(), moduleInv.getBlockEntity().getBlockPos().getZ(), i));
			return;
		}
	}

	private Component getModuleDescription(int moduleId) {
		//@formatter:off
		String moduleDescription = "module" + blockName + "." + descriptionButtons[moduleId].getItemStack().getDescriptionId().substring(5).replace("securitycraft.", "") + ".description";

		return Utils.localize(descriptionButtons[moduleId].getItemStack().getDescriptionId())
				.append(new TextComponent(":"))
				.withStyle(ChatFormatting.RESET)
				.append(new TextComponent("\n\n"))
				.append(Utils.localize(moduleDescription));
		//@formatter:on
	}

	private TranslatableComponent getOptionDescription(int optionId) {
		Option<?> option = ((ICustomizable) moduleInv.getBlockEntity()).customOptions()[optionId];
		String optionDescription = "option" + blockName + "." + option.getName() + ".description";

		return Utils.localize("gui.securitycraft:customize.tooltip", new TranslatableComponent(optionDescription), new TranslatableComponent("gui.securitycraft:customize.currentSetting", getValueText(option)));
	}

	private Component getOptionButtonTitle(Option<?> option) {
		return Utils.localize("option" + blockName + "." + option.getName(), getValueText(option));
	}

	private Component getValueText(Option<?> option) {
		if (option instanceof BooleanOption booleanOption)
			return new TranslatableComponent(booleanOption.get() ? "gui.securitycraft:invScan.yes" : "gui.securitycraft:invScan.no");
		else
			return new TextComponent(option.toString());
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}
}