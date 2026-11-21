package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.entity.critter.WildCritterEntity;

/**
 * M22.3: a shrimp, crayfish, snail or hermit crab in the world, drawn with the same models, colours and looks as the
 * animals of a tank ({@link CritterRenderer}); the legs walk while it crawls. M54: head first (they crawled backwards).
 */
public class WildCritterRenderer extends EntityRenderer<WildCritterEntity, WildCritterRenderer.State> {
    public WildCritterRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.05F;
    }

    /** M52b: the render state: the entity of this frame (the critter look and its walk are read at submit time). */
    public static final class State extends EntityRenderState {
        WildCritterEntity entity;
        float partialTick;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(WildCritterEntity e, State state, float partialTick) {
        super.extractRenderState(e, state, partialTick);
        state.entity = e;
        state.partialTick = partialTick;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        WildCritterEntity e = state.entity;
        if (e != null) {
            float partialTick = state.partialTick;
            Critter k = e.drawn();
            double dx = e.getX() - e.xo, dz = e.getZ() - e.zo;
            k.moving = dx * dx + dz * dz > 1e-7;
            float body = Mth.rotLerp(partialTick, e.yBodyRotO, e.yBodyRot);
            RecordingBuffers buffers = new RecordingBuffers();
            pose.pushPose();
            // M54: the models face -z and the critter scale flips z (as in the tank, CritterRenderer#orient): the entity's yaw
            // turns them as it is (180 - yaw, the vanilla turn for the mob models, made them crawl backwards)
            pose.mulPose(Axis.YP.rotationDegrees(org.overengineer.bettacraft.critters.CritterFacing.modelTurn(body)));
            CritterRenderer.drawOne(pose, buffers, k, e.level().getGameTime() + partialTick, state.lightCoords, e.hurtTime > 0);
            pose.popPose();
            buffers.submitTo(collector);
        }
        super.submit(state, pose, collector, camera);
    }
}
