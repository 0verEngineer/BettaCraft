package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.client.model.TerrariumAnimalModel;
import org.overengineer.bettacraft.terrarium.TerrariumAnimalEntity;

/**
 * M31: renderer of a terrarium animal: its model ({@code TerrariumModelDefinitions}) and texture
 * ({@code textures/entity/terrarium/<model>.png}) drawn at its size ({@code scale}: the 16 px model is one block long at 1).
 */
public class TerrariumAnimalRenderer extends MobRenderer<TerrariumAnimalEntity, TerrariumAnimalModel> {
    private final ResourceLocation texture;
    private final float scale;

    public TerrariumAnimalRenderer(EntityRendererProvider.Context ctx, String model, float scale, float shadow) {
        super(ctx, new TerrariumAnimalModel(ctx.bakeLayer(layer(model))), shadow);
        this.texture = BettaCraft.id("textures/entity/terrarium/" + model + ".png");
        this.scale = scale;
    }

    public static ModelLayerLocation layer(String model) {
        return new ModelLayerLocation(BettaCraft.id(model), "main");
    }

    @Override
    public ResourceLocation getTextureLocation(TerrariumAnimalEntity e) {
        return texture;
    }

    @Override
    protected void scale(TerrariumAnimalEntity e, PoseStack pose, float partialTick) {
        float s = e.isBaby() ? scale * 0.5F : scale;
        pose.scale(s, s, s);
    }
}
