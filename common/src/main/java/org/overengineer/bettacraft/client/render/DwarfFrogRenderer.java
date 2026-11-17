package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.client.model.DwarfFrogModel;
import org.overengineer.bettacraft.entity.frog.DwarfFrogEntity;

/** M14: the dwarf clawed frog (model of nominal length 16 px, drawn at its real size of about 0.3 blocks). */
public class DwarfFrogRenderer extends MobRenderer<DwarfFrogEntity, MobState<DwarfFrogEntity>, DwarfFrogModel> {
    private static final Identifier TEXTURE = BettaCraft.id("textures/entity/critters/frog.png");
    public static final float SCALE = 0.3F;

    public DwarfFrogRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DwarfFrogModel(ctx.bakeLayer(CritterRenderer.FROG_LAYER)), 0.1F);
    }

    @Override
    public MobState<DwarfFrogEntity> createRenderState() {
        return new MobState<>();
    }

    @Override
    public void extractRenderState(DwarfFrogEntity frog, MobState<DwarfFrogEntity> state, float partialTick) {
        super.extractRenderState(frog, state, partialTick);
        state.entity = frog;
        state.partialTick = partialTick;
    }

    @Override
    public Identifier getTextureLocation(MobState<DwarfFrogEntity> state) {
        return TEXTURE;
    }

    @Override
    protected void scale(MobState<DwarfFrogEntity> state, PoseStack pose) {
        float k = SCALE * state.entity.sizeFactor(); // M47: froglets are smaller
        pose.scale(k, k, k);
    }
}
