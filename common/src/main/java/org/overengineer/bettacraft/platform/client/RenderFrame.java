package org.overengineer.bettacraft.platform.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;

/**
 * M52b: what a level render stage event of 1.21.1 carried and the 1.21.11 events of the loaders no longer do: the culling
 * frustum and the projection matrix of the frame (captured by {@code LevelRendererFrustumMixin} when the level renderer
 * prepares them), for {@link RenderLevelStageEvent}.
 */
public final class RenderFrame {
    private static volatile Frustum frustum;
    private static final Matrix4f PROJECTION = new Matrix4f();

    private RenderFrame() {}

    public static void capture(Frustum f, Matrix4f projection) {
        frustum = f;
        PROJECTION.set(projection);
    }

    /** The culling frustum of this frame (an everything-visible one before the first frame). */
    public static Frustum frustum() {
        Frustum f = frustum;
        if (f == null) {
            f = new Frustum(new Matrix4f(), new Matrix4f());
            var cam = Minecraft.getInstance().gameRenderer.getMainCamera().position();
            f.prepare(cam.x, cam.y, cam.z);
        }
        return f;
    }

    public static Matrix4f projection() {
        return new Matrix4f(PROJECTION);
    }
}
