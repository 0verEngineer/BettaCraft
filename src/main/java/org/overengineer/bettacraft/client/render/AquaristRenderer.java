package org.overengineer.bettacraft.client.render;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.aquarist.AquaristEntity;

/**
 * M51: the hired aquarist: Minecraft's human model (wide arms, with the second skin layer for the clothes) and one of
 * four procedural skins with his work clothes ({@code tools/assetgen/hired_aquarist.py}); the item of his job in his hand.
 */
public class AquaristRenderer extends HumanoidMobRenderer<AquaristEntity, PlayerModel<AquaristEntity>> {
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[AquaristEntity.VARIANTS];

    static {
        for (int i = 0; i < TEXTURES.length; i++) TEXTURES[i] = BettaCraft.id("textures/entity/hired_aquarist/aquarist_" + i + ".png");
    }

    public AquaristRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(AquaristEntity e) {
        return TEXTURES[e.variant()];
    }
}
