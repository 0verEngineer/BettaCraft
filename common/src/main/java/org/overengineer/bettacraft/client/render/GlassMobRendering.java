package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.spider.Spider;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * M31 part d: a spider in glass ({@link org.overengineer.bettacraft.mobs.GlassMobs}, shrunk) that climbs the glass of its
 * tank lies with its belly on the glass, head up, like the climbing geckos ({@link HerpRenderer}); a vanilla spider climbs
 * upright. The walker turns it towards the wall and sets the synced climbing flag; the client only sees the small scale.
 * M52b: on both loaders through {@code LivingEntityRendererGlassMixin} (render states: the wall is noted when the state is
 * extracted, the turn is applied around the renderer's submit).
 */
public final class GlassMobRendering {
    /** Below this scale a spider is one in glass (wild ones are 1; the cave spider in glass 0.3). */
    static final float GLASS_SCALE = 0.6F;

    /** The render state of a living entity with the wall a spider in glass lies on (null: drawn as usual). */
    public interface GlassState {
        @Nullable Direction bettacraft$glassWall();

        void bettacraft$setGlassWall(@Nullable Direction wall);
    }

    private GlassMobRendering() {}

    /** While the state of a living entity is extracted: a climbing spider in glass notes its wall. */
    public static void extract(LivingEntity e, LivingEntityRenderState state, float partialTick) {
        if (!(state instanceof GlassState g)) return;
        Direction wall = null;
        if (e instanceof Spider s && s.isClimbing() && s.getScale() < GLASS_SCALE) {
            wall = Direction.fromYRot(Mth.rotLerp(partialTick, s.yBodyRotO, s.yBodyRot));
        }
        g.bettacraft$setGlassWall(wall);
    }

    /** Before a living entity is submitted: a climbing spider in glass lies flat on the glass. */
    public static void pre(LivingEntityRenderState state, PoseStack pose) {
        if (!(state instanceof GlassState g) || g.bettacraft$glassWall() == null) return;
        Direction wall = g.bettacraft$glassWall();
        pose.pushPose();
        // turn about the horizontal axis along the glass: its front goes up, its belly onto the glass
        pose.rotate(Axis.of(new Vector3f(-wall.getStepZ(), 0, wall.getStepX())).rotationDegrees(90F));
    }

    /** After it was submitted. */
    public static void post(LivingEntityRenderState state, PoseStack pose) {
        if (state instanceof GlassState g && g.bettacraft$glassWall() != null) pose.popPose();
    }
}
