package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.equipment.FilterFlow;

/**
 * Draws filter hoses and water jets (M4, PLAN 6.2). The jet is our own animated ribbon texture (never the vanilla
 * flowing water look), drawn as two crossed translucent ribbons whose texture scrolls along the flow. Everything is
 * immediate geometry from block entity renderers / the miniature pass, so Sodium's chunk meshing is not involved.
 */
public final class FlowRenderer {
    public static final ResourceLocation HOSE = BettaCraft.id("textures/entity/filter/hose.png");
    public static final ResourceLocation JET = BettaCraft.id("textures/entity/filter/jet.png");
    private static final float HOSE_HALF = 0.045F;
    /** Jet tint (ARGB). */
    private static final int JET_COLOR = 0xF0E4F0FF;

    private FlowRenderer() {}

    public static RenderType hoseType() {
        return RenderType.entityCutoutNoCull(HOSE);
    }

    public static RenderType jetType() {
        return RenderType.entityTranslucent(JET);
    }

    /** Hose from the filter up the wall, over the rim and down to the outlet, plus the outlet nozzle. */
    public static void hose(PoseStack ps, VertexConsumer vc, FilterFlow.Path p, Vec3 origin, int light, @javax.annotation.Nullable Vec3 source) {
        if (source != null) {
            // from the cabinet's back to the hose: horizontal, then vertical
            Vec3 s = source.subtract(origin), b = p.bottom().subtract(origin);
            Vec3 corner = new Vec3(b.x, s.y, b.z);
            segment(ps, vc, s, corner, HOSE_HALF, light);
            segment(ps, vc, corner, b, HOSE_HALF, light);
        }
        segment(ps, vc, p.bottom().subtract(origin), p.top().subtract(origin), HOSE_HALF, light);
        segment(ps, vc, p.top().subtract(origin), p.inside().subtract(origin), HOSE_HALF, light);
        segment(ps, vc, p.inside().subtract(origin), p.nozzle().subtract(origin), HOSE_HALF, light);
        // nozzle: a short wider piece pointing into the tank
        Direction in = p.inward();
        Vec3 n = p.nozzle().subtract(origin);
        Vec3 tip = n.add(in.getStepX() * 0.09, 0, in.getStepZ() * 0.09);
        segment(ps, vc, n, tip, HOSE_HALF * 1.35F, light);
    }

    /** Jet ribbons along the given centre line (world coordinates minus {@code origin}). */
    public static void jet(PoseStack ps, VertexConsumer vc, List<Vec3> line, Direction inward, Vec3 origin, int light, float time,
                           float width, float alpha) {
        if (line.size() < 2) return;
        Matrix4f m = ps.last().pose();
        // lateral (horizontal, across the flow) and a second ribbon rotated by 90 degrees around the flow
        Vec3 lateral = new Vec3(-inward.getStepZ(), 0, inward.getStepX()).scale(width);
        double len = 0;
        int a = Math.round(alpha * ((JET_COLOR >>> 24) & 0xFF));
        int r = (JET_COLOR >> 16) & 0xFF, g = (JET_COLOR >> 8) & 0xFF, b = JET_COLOR & 0xFF;
        float scroll = -time * 1.6F;
        for (int i = 0; i < line.size() - 1; i++) {
            Vec3 p0 = line.get(i).subtract(origin), p1 = line.get(i + 1).subtract(origin);
            Vec3 t = p1.subtract(p0);
            double segLen = t.length();
            if (segLen < 1e-5) continue;
            Vec3 up = t.normalize().cross(lateral.normalize()).scale(width * 0.8);
            float v0 = (float) (len * 1.5) + scroll, v1 = (float) ((len + segLen) * 1.5) + scroll;
            len += segLen;
            // the jet thins out towards its end
            float f0 = 1F - 0.35F * i / (line.size() - 1), f1 = 1F - 0.35F * (i + 1) / (line.size() - 1);
            int a0 = Math.round(a * (i == 0 ? 0.85F : 1F)), a1 = Math.round(a * (i + 1 == line.size() - 1 ? 0.25F : 1F));
            ribbon(m, vc, p0, p1, lateral.scale(f0), lateral.scale(f1), v0, v1, r, g, b, a0, a1, light, 0F, 0.5F);
            ribbon(m, vc, p0, p1, up.scale(f0), up.scale(f1), v0 + 0.37F, v1 + 0.37F, r, g, b, a0, a1, light, 0.5F, 1F);
        }
    }

    private static void ribbon(Matrix4f m, VertexConsumer vc, Vec3 p0, Vec3 p1, Vec3 o0, Vec3 o1, float v0, float v1,
                               int r, int g, int b, int a0, int a1, int light, float u0, float u1) {
        vertex(m, vc, p0.subtract(o0), u0, v0, r, g, b, a0, light);
        vertex(m, vc, p0.add(o0), u1, v0, r, g, b, a0, light);
        vertex(m, vc, p1.add(o1), u1, v1, r, g, b, a1, light);
        vertex(m, vc, p1.subtract(o1), u0, v1, r, g, b, a1, light);
    }

    private static void vertex(Matrix4f m, VertexConsumer vc, Vec3 p, float u, float v, int r, int g, int b, int a, int light) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, a).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0F, 1F, 0F);
    }

    /** Axis-aligned square tube between two points that differ along one axis. */
    public static void segment(PoseStack ps, VertexConsumer vc, Vec3 a, Vec3 b, float half, int light) {
        float x0 = (float) Math.min(a.x, b.x) - half, x1 = (float) Math.max(a.x, b.x) + half;
        float y0 = (float) Math.min(a.y, b.y) - half, y1 = (float) Math.max(a.y, b.y) + half;
        float z0 = (float) Math.min(a.z, b.z) - half, z1 = (float) Math.max(a.z, b.z) + half;
        box(ps.last(), vc, x0, y0, z0, x1, y1, z1, light);
    }

    private static void box(PoseStack.Pose pose, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1, int light) {
        Matrix4f m = pose.pose();
        float lx = x1 - x0, ly = y1 - y0, lz = z1 - z0;
        // u along the long side so the ribs of the hose texture follow the hose
        face(m, pose, vc, light, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, lx, lz);
        face(m, pose, vc, light, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, lx, lz);
        face(m, pose, vc, light, 0, 0, -1, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0, lx, ly);
        face(m, pose, vc, light, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, lx, ly);
        face(m, pose, vc, light, -1, 0, 0, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, ly, lz);
        face(m, pose, vc, light, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, ly, lz);
    }

    private static void face(Matrix4f m, PoseStack.Pose pose, VertexConsumer vc, int light, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
                             float sideA, float sideB) {
        float ua = Math.max(sideA, sideB) * 8F; // texture repeats every 1/8 block along the hose
        boolean along = sideA >= sideB;
        float[][] uv = along ? new float[][]{{0, 0}, {ua, 0}, {ua, 1}, {0, 1}} : new float[][]{{0, 0}, {0, 1}, {ua, 1}, {ua, 0}};
        float[][] p = {{ax, ay, az}, {bx, by, bz}, {cx, cy, cz}, {dx, dy, dz}};
        for (int i = 0; i < 4; i++) {
            vc.addVertex(m, p[i][0], p[i][1], p[i][2]).setColor(255, 255, 255, 255).setUv(uv[i][0], uv[i][1])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
        }
    }
}
