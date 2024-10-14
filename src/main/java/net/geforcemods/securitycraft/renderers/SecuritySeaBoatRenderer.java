package net.geforcemods.securitycraft.renderers;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BoatRenderState;

public class SecuritySeaBoatRenderer extends BoatRenderer {
	private final EntityModel<BoatRenderState> model;

	public SecuritySeaBoatRenderer(EntityRendererProvider.Context ctx, ModelLayerLocation location) {
		super(ctx, location);
		model = new SecuritySeaBoatModel(ctx.bakeLayer(location));
	}

	@Override
	protected EntityModel<BoatRenderState> model() {
		return model;
	}
}
