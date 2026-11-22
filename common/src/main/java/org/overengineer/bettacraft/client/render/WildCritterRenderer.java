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
 * animals of a tank ({@link CritterRenderer}); the legs walk while it crawls. M54: head first (they crawled backwards).
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
        net.minecraft.core.Direction wall = e.climbWall();
        if (wall != null) {
            // M62: on a wall: the belly against it (as on the glass of a tank, CritterRenderer#orient), head up (down on the way down)
            k.moving = true;
            float off = e.getBbWidth() * 0.5F - 0.01F;
            pose.translate(wall.getStepX() * off, e.getBbHeight() * 0.3F, wall.getStepZ() * off);
            CritterRenderer.orientOnGlass(pose, wall.get2DDataValue(), e.getY() < e.yo - 1e-4 ? 180F : 0F);
            CritterRenderer.drawOne(pose, buffers, k, e.level().getGameTime() + partialTick, light, e.hurtTime > 0);
            pose.popPose();
            super.render(e, yaw, partialTick, pose, buffers, light);
            return;
        }
        // M54: the models face -z and the critter scale flips z (as in the tank, CritterRenderer#orient): the entity's yaw
        // turns them as it is (180 - yaw, the vanilla turn for the mob models, made them crawl backwards)
        pose.mulPose(Axis.YP.rotationDegrees(org.overengineer.bettacraft.critters.CritterFacing.modelTurn(body)));
        CritterRenderer.drawOne(pose, buffers, k, e.level().getGameTime() + partialTick, light, e.hurtTime > 0);
        pose.popPose();
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(WildCritterEntity e) {
        return CritterRenderer.texture(e.drawn().species.model());
    }
}
