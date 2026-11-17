package org.overengineer.bettacraft.client.render;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.aquarist.AquaristEntity;

/**
 * M51: the hired aquarist: Minecraft's human model (wide arms, with the second skin layer for the clothes) and one of
 * four procedural skins with his work clothes ({@code tools/assetgen/hired_aquarist.py}); the item of his job in his hand.
 */
public class AquaristRenderer extends HumanoidMobRenderer<AquaristEntity, AvatarRenderState, PlayerModel> {
    private static final Identifier[] TEXTURES = new Identifier[AquaristEntity.VARIANTS];

    static {
        for (int i = 0; i < TEXTURES.length; i++) TEXTURES[i] = BettaCraft.id("textures/entity/hired_aquarist/aquarist_" + i + ".png");
    }

    public AquaristRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(AquaristEntity e, AvatarRenderState state, float partialTick) {
        super.extractRenderState(e, state, partialTick);
        if (state instanceof State s) s.variant = e.variant();
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return TEXTURES[state instanceof State s ? s.variant : 0];
    }

    /** M52b: the player model's render state plus the skin variant. */
    private static final class State extends AvatarRenderState {
        int variant;
    }
}
