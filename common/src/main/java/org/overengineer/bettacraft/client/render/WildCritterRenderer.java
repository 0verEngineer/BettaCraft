package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.entity.critter.WildCritterEntity;

/**
 * M22.3: a shrimp, crayfish, snail or hermit crab in the world, drawn with the same models, colours and looks as the
 * animals of a tank ({@link CritterRenderer}); the legs walk while it crawls.
 */
public class WildCritterRenderer extends EntityRenderer<WildCritterEntity> {
    public WildCritterRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.05F;
    }

    @Override
    public void render(WildCritterEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        Critter k = e.drawn();
        double dx = e.getX() - e.xo, dz = e.getZ() - e.zo;
        k.moving = dx * dx + dz * dz > 1e-7;
        float body = Mth.rotLerp(partialTick, e.yBodyRotO, e.yBodyRot);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180F - body));
        CritterRenderer.drawOne(pose, buffers, k, e.level().getGameTime() + partialTick, light, e.hurtTime > 0);
        pose.popPose();
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(WildCritterEntity e) {
        return CritterRenderer.texture(e.drawn().species.model());
    }
}
