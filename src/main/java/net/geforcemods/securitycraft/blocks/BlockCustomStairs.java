package net.geforcemods.securitycraft.blocks;

import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;

public class BlockCustomStairs extends BlockStairs {
	public BlockCustomStairs(IBlockState state) {
		super(state);
		useNeighborBrightness = true;
	}
}