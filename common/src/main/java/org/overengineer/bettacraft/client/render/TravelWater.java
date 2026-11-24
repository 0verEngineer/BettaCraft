package org.overengineer.bettacraft.client.render;

import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.overengineer.bettacraft.client.interior.IrisCompat;
import org.overengineer.bettacraft.client.tank.TankWaterRenderer;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;

/**
 * M50 (B2): the water of a travelling tank (Create contraption, Sable airship) sloshes. Its block model and fluid faces are
 * left out ({@link TankBlockEntity#WATER_DYNAMIC}); this draws the whole water body from the tank's block entity renderer
 * at the contraption's pose instead:
 * <ul>
 *     <li>From the pose of each frame the tank's centre is followed in the world; its acceleration (speeding up, braking,
 *     the pull outwards in a curve, a bearing that turns) and gravity give the direction the water "feels" down, in the
 *     tank's own frame. The surface is a plane perpendicular to it through the resting water level, so it also stays level
 *     when a bearing tilts the tank.</li>
 *     <li>The plane does not jump there: a damped spring per axis (about 0.9 Hz) lets it swing over and back after a
 *     stop; the swinging feeds waves that run over the surface and die down in a few seconds.</li>
 *     <li>The water keeps to its tank: the tilt is limited so the surface stays between the floor and the top of each
 *     column (free shapes: per column of tank blocks).</li>
 * </ul>
 * {@code render.travelSloshing} scales the effect (0 = off: the flat water of M32 is drawn by the model as before).
 */
public final class TravelWater {
    private static final double G = 9.81;
    private static final double OMEGA = 2 * Math.PI * 0.9;
    private static final double ZETA = 0.12;
    /** The steepest the surface is pulled to (about 22 degrees). */
    private static final double MAX_SLOPE = 0.4;
    private static final float INSET = 0.5F / 16F;
    private static final int COLOR = 0xFF4A90E2;
    private static final int SIDE_COLOR = 0xFF6CA6DC;

    private TravelWater() {}

    private static final class Slosh {
        long last;
        Vec3 prev;
        Vec3 vel = Vec3.ZERO, velPrev = Vec3.ZERO, acc = Vec3.ZERO;
        double sx, sz, vx, vz, energy;
        int lastFrame = Integer.MIN_VALUE;
        /** For the dev log: the resting level, the room for the tilt, the acceleration (blocks/s²). */
        double h0, room, accel, lastDt;
        int resets;
    }

    private static final Map<TankBlockEntity, Slosh> STATE = new WeakHashMap<>();
    private static int frame;

    public static double strength() {
        try {
            return ClientConfig.TRAVEL_SLOSHING.get();
        } catch (RuntimeException e) {
            return 1.0; // the config is not loaded yet
        }
    }

    /** Whether the water of this (travelling) tank block is drawn here instead of by its model and the fluid renderer. */
    public static boolean drawsWater(TankBlockEntity be) {
        return strength() > 0 && (be.onContraption() || TravellingTankRenderer.travellingBlock(be));
    }

    /** Called once per frame (start of the level render) so several passes of one frame move the water only once. */
    public static void nextFrame() {
        frame++;
    }

    /** The tilt of a travelling tank's water now: {slope x, slope z, wave amplitude} (dev smoke screenshots, tests). */
    public static double[] tilt(TankBlockEntity be) {
        Slosh s = STATE.get(be);
        return s == null ? new double[3] : new double[]{s.sx, s.sz, waveAmp(s)};
    }

    /** Every travelling tank's water now: tilt, resting level, room, acceleration (dev smoke log). */
    public static String debug() {
        StringBuilder b = new StringBuilder();
        for (Slosh s : STATE.values()) b.append(String.format(java.util.Locale.ROOT, "[tilt %.3f/%.3f h0 %.2f room %.2f acc %.2f waves %.3f at %s dt %.4f resets %d vel %.2f] ",
                s.sx, s.sz, s.h0, s.room, s.accel, waveAmp(s), s.prev, s.lastDt, s.resets, s.vel.length()));
        return b.toString();
    }

    /** The largest tilt (slope) of any travelling tank's water now (dev smoke log). */
    public static double maxTilt() {
        double m = 0;
        for (Slosh s : STATE.values()) m = Math.max(m, Math.hypot(s.sx, s.sz));
        return m;
    }

    private static double waveAmp(Slosh s) {
        return Math.min(0.06, s.energy);
    }

    /** From the tank's block entity renderer (pose at the controller block, on the contraption or ship). */
    public static void render(TankBlockEntity be, TankData t, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (t.waterMb() <= 0 || strength() <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        BlockPos o = be.getBlockPos(), mn = t.min(), mx = t.max();
        double minX = mn.getX() - o.getX(), minZ = mn.getZ() - o.getZ(), maxX = mx.getX() + 1 - o.getX(), maxZ = mx.getZ() + 1 - o.getZ();
        double cx = (minX + maxX) / 2, cz = (minZ + maxZ) / 2;
        double h0 = t.waterSurfaceY() - o.getY();
        double floor = mn.getY() - o.getY() + INSET;
        double top = mx.getY() + 1 - o.getY() - INSET;
        Slosh s = STATE.computeIfAbsent(be, k -> new Slosh());
        Matrix4f m = pose.last().pose();
        if (!IrisCompat.renderingShadowPass() && s.lastFrame != frame) {
            s.lastFrame = frame;
            update(s, m, new Vector3f((float) cx, (float) ((floor + h0) / 2), (float) cz), mc);
            // keep the plane inside the water (the lower of the room above and below the resting level)
            // below the resting level the floor limits it; above, the water may reach the top (a full tank's air moves)
            double room = Math.max(0.02, h0 - floor - 0.03);
            s.h0 = h0;
            s.room = room;
            s.accel = s.acc.length();
            double reach = Math.abs(s.sx) * (maxX - minX) / 2 + Math.abs(s.sz) * (maxZ - minZ) / 2;
            if (reach > room) {
                double k = room / reach;
                s.sx *= k;
                s.sz *= k;
                s.vx *= 0.5;
                s.vz *= 0.5;
            }
        }
        draw(be, t, s, pose, buffers, light, cx, cz, h0, mc);
    }

    private static void update(Slosh s, Matrix4f m, Vector3f centre, Minecraft mc) {
        long now = System.nanoTime();
        Vector3f c = m.transformPosition(new Vector3f(centre));
        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        Vec3 w = new Vec3(c.x() + cam.x, c.y() + cam.y, c.z() + cam.z);
        double dt = s.last == 0 ? 0 : (now - s.last) / 1e9;
        s.last = now;
        s.lastDt = dt;
        if (s.prev == null || dt <= 1e-4 || dt > 0.6) { // a pause, a jump: start again (slow frames of a window in the background still count)
            s.resets++;
            s.prev = w;
            s.vel = s.velPrev = s.acc = Vec3.ZERO;
            return;
        }
        Vec3 v = w.subtract(s.prev).scale(1 / dt);
        s.prev = w;
        double a1 = 1 - Math.exp(-dt / 0.12);
        s.velPrev = s.vel;
        s.vel = s.vel.add(v.subtract(s.vel).scale(a1));
        Vec3 acc = s.vel.subtract(s.velPrev).scale(1 / dt);
        if (acc.lengthSqr() > 400 * 400) acc = acc.normalize().scale(400); // a teleport or a jump of the pose
        s.acc = s.acc.add(acc.subtract(s.acc).scale(a1));
        double k = strength();
        // the down the water feels, in the tank's frame (the pose's rotation, transposed)
        Vec3 g = new Vec3(-s.acc.x * k, -G - s.acc.y * k, -s.acc.z * k);
        Matrix3f r = new Matrix3f();
        m.get3x3(r);
        r.transpose();
        Vector3f gl = r.transform(new Vector3f((float) g.x, (float) g.y, (float) g.z));
        double gy = Math.min(-0.2 * G, gl.y());
        double tx = gl.x() / -gy, tz = gl.z() / -gy;
        // real water in a tank does not stand much steeper than this, the rest goes into the swinging and the waves
        double t = Math.hypot(tx, tz), max = MAX_SLOPE * Math.max(1, k);
        if (t > max) {
            tx *= max / t;
            tz *= max / t;
        }
        // the damped springs (semi-implicit, small steps)
        double left = dt;
        while (left > 1e-6) {
            double h = Math.min(1 / 120.0, left);
            left -= h;
            s.vx += (OMEGA * OMEGA * (tx - s.sx) - 2 * ZETA * OMEGA * s.vx) * h;
            s.vz += (OMEGA * OMEGA * (tz - s.sz) - 2 * ZETA * OMEGA * s.vz) * h;
            s.sx += s.vx * h;
            s.sz += s.vz * h;
        }
        s.energy = s.energy * Math.exp(-dt / 1.6) + (Math.abs(s.vx) + Math.abs(s.vz)) * dt * 0.12 * k;
    }

    /** Height of the surface at (x, z) (local to the controller block). */
    private static double height(Slosh s, double h0, double cx, double cz, double x, double z, double time) {
        double a = waveAmp(s);
        double wave = a == 0 ? 0 : a * (Math.sin(2.3 * x + 3.1 * time) * Math.cos(1.9 * z - 2.4 * time) + 0.5 * Math.sin(3.7 * (x + z) - 4.2 * time)) / 1.5;
        // slope: the surface rises towards where the water is pushed (sx > 0: higher at larger x)
        return h0 + s.sx * (x - cx) + s.sz * (z - cz) + wave;
    }

    private static void draw(TankBlockEntity be, TankData t, Slosh s, PoseStack pose, MultiBufferSource buffers, int light,
                             double cx, double cz, double h0, Minecraft mc) {
        BlockPos o = be.getBlockPos();
        // the columns of the tank: lowest and highest block (local)
        Long2LongOpenHashMap cols = new Long2LongOpenHashMap();
        for (long l : t.members()) {
            BlockPos p = BlockPos.of(l);
            long key = BlockPos.asLong(p.getX() - o.getX(), 0, p.getZ() - o.getZ());
            int y = p.getY() - o.getY();
            long cur = cols.getOrDefault(key, Long.MIN_VALUE);
            int lo = cur == Long.MIN_VALUE ? y : Math.min(y, (int) (cur >> 32)), hi = cur == Long.MIN_VALUE ? y : Math.max(y, (int) cur);
            cols.put(key, ((long) lo << 32) | (hi & 0xFFFFFFFFL));
        }
        var atlas = org.overengineer.bettacraft.client.ClientCompat.blockSprites();
        TextureAtlasSprite still = atlas.apply(TankWaterRenderer.STILL), side = atlas.apply(TankWaterRenderer.SIDE);
        VertexConsumer vc = buffers.getBuffer(RenderTypes.entityTranslucent(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS));
        PoseStack.Pose p = pose.last();
        double time = (mc.level == null ? 0 : (mc.level.getGameTime() % 24000L)) / 20.0 + org.overengineer.bettacraft.client.ClientCompat.partialTick() / 20.0;
        for (var e : cols.long2LongEntrySet()) {
            int x = BlockPos.getX(e.getLongKey()), z = BlockPos.getZ(e.getLongKey());
            int lo = (int) (e.getLongValue() >> 32), hi = (int) e.getLongValue();
            double bottom = lo + INSET, roof = hi + 1 - INSET;
            if (h0 <= bottom + 0.01) continue;
            boolean w = cols.containsKey(BlockPos.asLong(x - 1, 0, z)), east = cols.containsKey(BlockPos.asLong(x + 1, 0, z));
            boolean n = cols.containsKey(BlockPos.asLong(x, 0, z - 1)), south = cols.containsKey(BlockPos.asLong(x, 0, z + 1));
            // M65: the sides behind the front of the miniature's blocks at the glass, like the water of a standing tank
            float si = org.overengineer.bettacraft.client.tank.TankWaterGeometry.sideInset();
            float x0 = x + (w ? 0 : si), x1 = x + 1 - (east ? 0 : si), z0 = z + (n ? 0 : si), z1 = z + 1 - (south ? 0 : si);
            float y00 = clampH(height(s, h0, cx, cz, x0, z0, time), bottom, roof), y10 = clampH(height(s, h0, cx, cz, x1, z0, time), bottom, roof);
            float y11 = clampH(height(s, h0, cx, cz, x1, z1, time), bottom, roof), y01 = clampH(height(s, h0, cx, cz, x0, z1, time), bottom, roof);
            // top (normal up), counter-clockwise seen from above
            quad(vc, p, still, COLOR, light, 0, 1, 0,
                    x0, y00, z0, 0, 0, x0, y01, z1, 0, 1, x1, y11, z1, 1, 1, x1, y10, z0, 1, 0);
            float b = (float) bottom;
            if (!w) quad(vc, p, side, SIDE_COLOR, light, -1, 0, 0, x0, b, z0, 0, 1, x0, b, z1, 1, 1, x0, y01, z1, 1, 1 - (y01 - b), x0, y00, z0, 0, 1 - (y00 - b));
            if (!east) quad(vc, p, side, SIDE_COLOR, light, 1, 0, 0, x1, b, z1, 0, 1, x1, b, z0, 1, 1, x1, y10, z0, 1, 1 - (y10 - b), x1, y11, z1, 0, 1 - (y11 - b));
            if (!n) quad(vc, p, side, SIDE_COLOR, light, 0, 0, -1, x1, b, z0, 0, 1, x0, b, z0, 1, 1, x0, y00, z0, 1, 1 - (y00 - b), x1, y10, z0, 0, 1 - (y10 - b));
            if (!south) quad(vc, p, side, SIDE_COLOR, light, 0, 0, 1, x0, b, z1, 0, 1, x1, b, z1, 1, 1, x1, y11, z1, 1, 1 - (y11 - b), x0, y01, z1, 0, 1 - (y01 - b));
            // bottom (normal down)
            quad(vc, p, side, SIDE_COLOR, light, 0, -1, 0, x0, b, z0, 0, 0, x1, b, z0, 1, 0, x1, b, z1, 1, 1, x0, b, z1, 0, 1);
        }
    }

    private static float clampH(double h, double lo, double hi) {
        return (float) Math.max(lo + 0.01, Math.min(hi, h));
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose p, TextureAtlasSprite sp, int argb, int light, float nx, float ny, float nz,
                             float ax, float ay, float az, float au, float av, float bx, float by, float bz, float bu, float bv,
                             float cx, float cy, float cz, float cu, float cv, float dx, float dy, float dz, float du, float dv) {
        vertex(vc, p, sp, argb, light, nx, ny, nz, ax, ay, az, au, av);
        vertex(vc, p, sp, argb, light, nx, ny, nz, bx, by, bz, bu, bv);
        vertex(vc, p, sp, argb, light, nx, ny, nz, cx, cy, cz, cu, cv);
        vertex(vc, p, sp, argb, light, nx, ny, nz, dx, dy, dz, du, dv);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, TextureAtlasSprite sp, int argb, int light, float nx, float ny, float nz,
                               float x, float y, float z, float u, float v) {
        vc.addVertex(p, x, y, z).setColor(argb).setUv(sp.getU(clamp01(u)), sp.getV(clamp01(v))).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(p, nx, ny, nz);
    }

    private static float clamp01(float f) {
        return Math.max(0, Math.min(1, f));
    }
}
