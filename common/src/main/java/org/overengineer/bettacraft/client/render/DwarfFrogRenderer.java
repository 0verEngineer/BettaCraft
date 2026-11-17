package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.client.model.DwarfFrogModel;
import org.overengineer.bettacraft.entity.frog.DwarfFrogEntity;

/** M14: the dwarf clawed frog (model of nominal length 16 px, drawn at its real size of about 0.3 blocks). */
public class DwarfFrogRenderer extends MobRenderer<DwarfFrogEntity, DwarfFrogModel> {
    private static final ResourceLocation TEXTURE = BettaCraft.id("textures/entity/critters/frog.png");
    public static final float SCALE = 0.3F;

    public DwarfFrogRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DwarfFrogModel(ctx.bakeLayer(CritterRenderer.FROG_LAYER)), 0.1F);
    }

    @Override
    public ResourceLocation getTextureLocation(DwarfFrogEntity frog) {
        return TEXTURE;
    }

    @Override
    protected void scale(DwarfFrogEntity frog, PoseStack pose, float partialTick) {
        float k = SCALE * frog.sizeFactor(); // M47: froglets are smaller
        pose.scale(k, k, k);
    }
}
