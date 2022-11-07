package net.geforcemods.securitycraft.api;

import net.geforcemods.securitycraft.items.ModuleItem;
import net.geforcemods.securitycraft.misc.ModuleType;
import net.minecraft.item.ItemStack;

/**
 * A simple interface which contains all the possible actions for LinkableBlockEntity.onLinkedBlockAction().
 *
 * @author Geforce, bl4ckscor3
 */
public interface LinkedAction {
	/**
	 * Used when an {@link Option} in a TileEntity is changed.
	 */
	public static final class OptionChanged<T> implements LinkedAction {
		public final Option<T> option;

		public OptionChanged(Option<T> option) {
			this.option = option;
		}
	}

	/**
	 * Used when a {@link ModuleType} is inserted into an {@link IModuleInventory}
	 */
	public static final class ModuleInserted implements LinkedAction {
		public final ItemStack stack;
		public final ModuleItem module;
		public final boolean wasModuleToggled;

		public ModuleInserted(ItemStack stack, ModuleItem module, boolean wasModuleToggled) {
			this.stack = stack;
			this.module = module;
			this.wasModuleToggled = wasModuleToggled;
		}
	}

	/**
	 * Used when a {@link ModuleType} is removed from an {@link IModuleInventory}
	 */
	public static final class ModuleRemoved implements LinkedAction {
		public final ModuleType moduleType;
		public final boolean wasModuleToggled;

		public ModuleRemoved(ModuleType moduleType, boolean wasModuleToggled) {
			this.moduleType = moduleType;
			this.wasModuleToggled = wasModuleToggled;
		}
	}

	/**
	 * Used when the {@link Owner} of a block entity changes
	 */
	public static final class OwnerChanged implements LinkedAction {
		public final Owner newOwner;

		public OwnerChanged(Owner newOwner) {
			this.newOwner = newOwner;
		}
	}
}
