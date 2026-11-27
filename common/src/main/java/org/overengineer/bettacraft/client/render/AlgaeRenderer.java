package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.client.tank.TankWaterGeometry;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.algae.AlgaeSim;

/**
 * Algae on the inside of the glass (M14), drawn by the controller's block entity renderer: one quad per glass face with
 * algae, just inside the glass, from the bottom of the block up to the water in it, in four stages. The alpha is in the
 * texture (a translucent entity render type), so it works with and without shader packs; seen from outside the glass
 * looks dimmer, from inside too (no culling).
 */
public final class AlgaeRenderer {
    /**
     * The four stages side by side in one texture (M66). One texture is one render type, so every film quad of the tank goes
     * into one batch, which the buffer source sorts back to front before drawing (entityTranslucent sorts on upload). With one
     * texture per stage the stages were four batches drawn one after the other: the film writes depth, so a front-wall quad of
     * a lower stage hid every back-wall quad of a higher stage behind it, and which back-wall patches showed through changed
     * with every step (the algae "jumping" around on large tanks).
     */
    private static final Identifier TEXTURE = BettaCraft.id("textures/entity/algae/algae.png");

    /** Distance of the algae film from the outer face of the block (inside the 1/16 glass; M66: 0.012 and not 0.003, a film that close to the glass fought it for the depth buffer
     * (diagonal flicker over the whole wall when moving, with shader packs from about ten blocks away)). */
    private static final float INSET = 1F / 16F + 0.012F;

    /**
     * M66: the water between the camera and a film seen through the tank (the far glass) fades it, exp(-path / this) with the path
     * through the water in blocks: a third left behind 9 blocks of water, a sixth behind 16. Without it the far glass of a large
     * tank was as strong as the near one, and the two films, each with its blocks at different stages, slid over each other with
     * every step: the algae seemed to jump around.
     */
    private static final float WATER_CLARITY = 8F;
    /** Longest water path counted (blocks). */
    private static final int MAX_PATH = 48;

    /** Dev tour only (M66 v6, the proof of the causes): "nodepth" = the film without depth writes, "off" = no film; empty = normal. */
    public static volatile String testMode = "";

    private AlgaeRenderer() {}

    private static RenderType type() {
        // Port (26.x): render types are built from render pipelines here; the "nodepth" proof switch of 1.21.1 draws the normal film
        return net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(TEXTURE);
    }

    public static void render(TankBlockEntity be, TankData t, PoseStack pose, MultiBufferSource buffers) {
        Level level = be.getLevel();
        if (level == null || t.algae().isEmpty() || "off".equals(testMode)) return;
        BlockPos origin = be.getBlockPos();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        net.minecraft.world.phys.Vec3 cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.mainCamera().position();
        // one batch for the whole tank (see TEXTURE): one draw call, sorted back to front
        VertexConsumer vc = null;
        for (var e : t.algae().long2ObjectEntrySet()) {
            float[] v = e.getValue();
            if (v[0] <= 0 && v[1] <= 0 && v[2] <= 0 && v[3] <= 0) continue;
            p.set(e.getLongKey());
            if (!t.contains(p)) continue;
            float top = -1;
            int light = 0;
            for (int d = 0; d < 4; d++) {
                int stage = Math.min(AlgaeSim.STAGES, Math.round(v[d] * AlgaeSim.STAGES));
                if (stage <= 0) continue;
                if (t.contains(BlockPos.asLong(p.getX() + AlgaeSim.stepX(d), p.getY(), p.getZ() + AlgaeSim.stepZ(d)))) continue;
                if (top < 0) {
                    BlockState s = level.getBlockState(p);
                    top = 1F;
                    if (s.getBlock() instanceof TankBlock) {
                        int fill = s.getValue(TankBlock.FILL);
                        top = fill <= 0 ? 0.25F : Math.min(fill / 16F, 1 - TankWaterGeometry.INSET);
                    }
                    light = net.minecraft.util.LightCoordsUtil.getLightCoords(level, p);
                }
                if (vc == null) vc = buffers.getBuffer(type());
                // along a flat wall the film runs on into the next block (no seam); at a corner it stops at the glass
                int ax = d == 0 || d == 2 ? 1 : 0, az = 1 - ax;
                boolean neg = wallAt(t, p.getX() - ax, p.getY(), p.getZ() - az, d), pos = wallAt(t, p.getX() + ax, p.getY(), p.getZ() + az, d);
                int alpha = Math.round(255 * seenThroughWater(t, p, d, cam));
                if (alpha < 4) continue;
                quad(pose, vc, p.getX() - origin.getX(), p.getY() - origin.getY(), p.getZ() - origin.getZ(), d, top, light, neg, pos, stage, alpha);
            }
        }
    }

    /**
     * How much of the film on face {@code d} of block {@code p} the water lets through to the camera: 1 when the camera looks at it
     * through the glass (the near wall), else exp(-path / {@link #WATER_CLARITY}) with the path through the tank's water towards
     * the camera (the run of tank blocks inwards from the face, or the camera's distance when it is inside), longer when the
     * camera looks at the glass at a slant.
     */
    static float seenThroughWater(TankData t, BlockPos p, int d, net.minecraft.world.phys.Vec3 cam) {
        int nx = AlgaeSim.stepX(d), nz = AlgaeSim.stepZ(d);
        double fx = p.getX() + 0.5 + 0.5 * nx, fz = p.getZ() + 0.5 + 0.5 * nz;
        double along = (cam.x - fx) * nx + (cam.z - fz) * nz; // >= 0: the camera is outside this glass
        if (along >= 0) return 1F;
        int run = 1;
        while (run < MAX_PATH && t.contains(BlockPos.asLong(p.getX() - nx * run, p.getY(), p.getZ() - nz * run))) run++;
        double path = Math.min(run, -along);
        double vx = cam.x - fx, vy = cam.y - (p.getY() + 0.5), vz = cam.z - fz;
        double len = Math.sqrt(vx * vx + vy * vy + vz * vz);
        double cos = len < 1e-3 ? 1 : Math.max(0.25, -along / len);
        return (float) Math.exp(-(path / cos) / WATER_CLARITY);
    }

    /** Whether block (x, y, z) belongs to the tank and its face towards {@code dir} is glass too (the wall goes on). */
    private static boolean wallAt(TankData t, int x, int y, int z, int dir) {
        return t.contains(BlockPos.asLong(x, y, z)) && !t.contains(BlockPos.asLong(x + AlgaeSim.stepX(dir), y, z + AlgaeSim.stepZ(dir)));
    }

    private static void quad(PoseStack pose, VertexConsumer vc, float bx, float by, float bz, int dir, float top, int light, boolean negOpen, boolean posOpen, int stage, int alpha) {
        PoseStack.Pose last = pose.last();
        Matrix4f m = last.pose();
        float lo = 1F / 16F, hi = 1F - 1F / 16F;
        // the along-axis ends: x for south / north, z for west / east
        float a0 = negOpen ? 0F : lo, a1 = posOpen ? 1F : hi;
        float y0 = by + lo, y1 = by + Math.max(lo + 0.05F, top);
        float v0 = 1 - (y1 - by), v1 = 1 - (y0 - by);
        float[][] q;
        float nx = 0, nz = 0;
        switch (dir) {
            case 0 -> { // south: plane z = bz + 1 - INSET
                float z = bz + 1 - INSET;
                q = new float[][]{{bx + a0, y0, z, a0, v1}, {bx + a1, y0, z, a1, v1}, {bx + a1, y1, z, a1, v0}, {bx + a0, y1, z, a0, v0}};
                nz = -1;
            }
            case 2 -> { // north
                float z = bz + INSET;
                q = new float[][]{{bx + a1, y0, z, 1 - a1, v1}, {bx + a0, y0, z, 1 - a0, v1}, {bx + a0, y1, z, 1 - a0, v0}, {bx + a1, y1, z, 1 - a1, v0}};
                nz = 1;
            }
            case 1 -> { // west
                float x = bx + INSET;
                q = new float[][]{{x, y0, bz + a0, a0, v1}, {x, y0, bz + a1, a1, v1}, {x, y1, bz + a1, a1, v0}, {x, y1, bz + a0, a0, v0}};
                nx = 1;
            }
            default -> { // east
                float x = bx + 1 - INSET;
                q = new float[][]{{x, y0, bz + a1, 1 - a1, v1}, {x, y0, bz + a0, 1 - a0, v1}, {x, y1, bz + a0, 1 - a0, v0}, {x, y1, bz + a1, 1 - a1, v0}};
                nx = -1;
            }
        }
        for (float[] c : q) {
            // u into the stage's quarter of the texture
            vc.addVertex(m, c[0], c[1], c[2]).setColor(255, 255, 255, alpha).setUv((stage - 1 + c[3]) / AlgaeSim.STAGES, c[4]).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light).setNormal(last, nx, 0, nz);
        }
    }
}
