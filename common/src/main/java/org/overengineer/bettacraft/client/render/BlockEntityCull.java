package org.overengineer.bettacraft.client.render;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.overengineer.bettacraft.client.interior.IrisCompat;
import org.overengineer.bettacraft.platform.client.RenderLevelStageEvent;

/**
 * M66.5: frustum culling for our block entity renderers that sit in the always-drawn ("global") list: the tank controllers
 * (algae of travelling tanks, floating plants, small animals, nests, clutches, sign) and the cabinets and external filters
 * (hose and water jet). Vanilla and NeoForge test a global block entity against the view; Sodium draws every global block entity
 * in range without that test, so every hose and every tank's small animals of the loaded world were built every frame, also
 * behind the camera (a third of the render thread in the showcase with Sodium). The renderers ask {@link #outside} first.
 * <p>
 * The frustum is the main camera's of the frame, taken at the first render stage before the block entities. In the Iris shadow
 * pass nothing is culled (its view is the sun's). A frustum older than a quarter second (no stage fired, e.g. another renderer
 * replaced the level rendering) culls nothing.
 */
public final class BlockEntityCull {
    private static volatile Frustum frustum;
    private static volatile long takenAt;
    /** Tests: switch the culling off to compare. */
    public static volatile boolean disabledForTest;

    private BlockEntityCull() {}

    public static void onRenderStage(RenderLevelStageEvent e) {
        RenderLevelStageEvent.Stage s = e.getStage();
        if (s != RenderLevelStageEvent.Stage.AFTER_SKY && s != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS && s != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (e.getFrustum() == null || IrisCompat.renderingShadowPass()) return;
        frustum = e.getFrustum();
        takenAt = System.nanoTime();
    }

    /** Whether {@code box} lies outside the camera's view this frame (false when unsure: then it is drawn). */
    public static boolean outside(AABB box) {
        Frustum f = frustum;
        if (f == null || disabledForTest || box == null || IrisCompat.renderingShadowPass()) return false;
        if (System.nanoTime() - takenAt > 250_000_000L) return false;
        return !f.isVisible(box);
    }

    /** Logout: no frustum of the old level. */
    public static void reset() {
        frustum = null;
    }
}
