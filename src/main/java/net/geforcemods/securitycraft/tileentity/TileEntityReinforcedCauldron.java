package net.geforcemods.securitycraft.tileentity;

import net.geforcemods.securitycraft.api.CustomizableSCTE;
import net.geforcemods.securitycraft.api.Option;
import net.geforcemods.securitycraft.api.Option.OptionBoolean;
import net.geforcemods.securitycraft.misc.EnumModuleType;
import net.minecraft.entity.player.EntityPlayer;

public class TileEntityReinforcedCauldron extends CustomizableSCTE {
	private final OptionBoolean isPublic = new OptionBoolean("isPublic", false);

	public boolean isAllowedToInteract(EntityPlayer player) {
		return isPublic.get() || isOwnedBy(player) || isAllowed(player);
	}

	@Override
	public Option<?>[] customOptions() {
		return new Option[] {
				isPublic
		};
	}

	@Override
	public EnumModuleType[] acceptedModules() {
		return new EnumModuleType[] {
				EnumModuleType.ALLOWLIST
		};
	}
}