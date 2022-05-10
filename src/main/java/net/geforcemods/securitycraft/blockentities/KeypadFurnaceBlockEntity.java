package net.geforcemods.securitycraft.blockentities;

import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.inventory.KeypadFurnaceMenu;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.crafting.IRecipeType;

public class KeypadFurnaceBlockEntity extends AbstractKeypadFurnaceBlockEntity {
	public KeypadFurnaceBlockEntity() {
		super(SCContent.KEYPAD_FURNACE_BLOCK_ENTITY.get(), IRecipeType.SMELTING);
	}

	@Override
	protected Container createMenu(int windowId, PlayerInventory inv) {
		return new KeypadFurnaceMenu(windowId, inv, this);
	}
}
