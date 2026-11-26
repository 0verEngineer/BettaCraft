package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
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
    private static final ResourceLocation[] STAGES = new ResourceLocation[AlgaeSim.STAGES + 1];

    static {
        for (int i = 1; i <= AlgaeSim.STAGES; i++) STAGES[i] = BettaCraft.id("textures/entity/algae/algae_" + i + ".png");
    }

    /** Distance of the algae film from the outer face of the block (inside the 1/16 glass; M66: 0.012 and not 0.003, a film that close to the glass fought it for the depth buffer
     * (diagonal flicker over the whole wall when moving, with shader packs from about ten blocks away)). */
    private static final float INSET = 1F / 16F + 0.012F;

    private AlgaeRenderer() {}

    public static void render(TankBlockEntity be, TankData t, PoseStack pose, MultiBufferSource buffers) {
        Level level = be.getLevel();
        if (level == null || t.algae().isEmpty()) return;
        BlockPos origin = be.getBlockPos();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        // M66: one pass per stage. A buffer source ends (draws) the running batch whenever the render type changes, so drawing the
        // faces in map order (the four stage textures mixed) was one draw call per change, hundreds on a large tank, and with a
        // shader pack each one is expensive (the lag at large tanks with algae)
        for (int stage = 1; stage <= AlgaeSim.STAGES; stage++) {
            VertexConsumer vc = null;
            for (var e : t.algae().long2ObjectEntrySet()) {
                float[] v = e.getValue();
                if (v[0] <= 0 && v[1] <= 0 && v[2] <= 0 && v[3] <= 0) continue;
                p.set(e.getLongKey());
                if (!t.contains(p)) continue;
                float top = -1;
                int light = 0;
                for (int d = 0; d < 4; d++) {
                    if (Math.min(AlgaeSim.STAGES, Math.round(v[d] * AlgaeSim.STAGES)) != stage) continue;
                    if (t.contains(BlockPos.asLong(p.getX() + AlgaeSim.stepX(d), p.getY(), p.getZ() + AlgaeSim.stepZ(d)))) continue;
                    if (top < 0) {
                        BlockState s = level.getBlockState(p);
                        top = 1F;
                        if (s.getBlock() instanceof TankBlock) {
                            int fill = s.getValue(TankBlock.FILL);
                            top = fill <= 0 ? 0.25F : Math.min(fill / 16F, 1 - TankWaterGeometry.INSET);
                        }
                        light = LevelRenderer.getLightColor(level, p);
                    }
                    if (vc == null) vc = buffers.getBuffer(RenderType.entityTranslucent(STAGES[stage]));
                    // along a flat wall the film runs on into the next block (no seam); at a corner it stops at the glass
                    int ax = d == 0 || d == 2 ? 1 : 0, az = 1 - ax;
                    boolean neg = wallAt(t, p.getX() - ax, p.getY(), p.getZ() - az, d), pos = wallAt(t, p.getX() + ax, p.getY(), p.getZ() + az, d);
                    quad(pose, vc, p.getX() - origin.getX(), p.getY() - origin.getY(), p.getZ() - origin.getZ(), d, top, light, neg, pos);
                }
            }
        }
    }

    /** Whether block (x, y, z) belongs to the tank and its face towards {@code dir} is glass too (the wall goes on). */
    private static boolean wallAt(TankData t, int x, int y, int z, int dir) {
        return t.contains(BlockPos.asLong(x, y, z)) && !t.contains(BlockPos.asLong(x + AlgaeSim.stepX(dir), y, z + AlgaeSim.stepZ(dir)));
    }

    private static void quad(PoseStack pose, VertexConsumer vc, float bx, float by, float bz, int dir, float top, int light, boolean negOpen, boolean posOpen) {
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
            vc.addVertex(m, c[0], c[1], c[2]).setColor(0xFFFFFFFF).setUv(c[3], c[4]).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light).setNormal(last, nx, 0, nz);
        }
    }
}
