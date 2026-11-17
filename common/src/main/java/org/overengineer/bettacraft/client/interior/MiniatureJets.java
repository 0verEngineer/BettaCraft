package org.overengineer.bettacraft.client.interior;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.platform.client.RenderLevelStageEvent;
import org.overengineer.bettacraft.client.render.FlowRenderer;
import org.overengineer.bettacraft.equipment.FilterFlow;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;

/**
 * Water jets of the internal filters in the miniature (M4): one small ribbon jet per filter outlet, drawn right after
 * the opaque miniature and before the tank water (never in the Iris shadow pass, see {@link MiniatureRenderer}), plus
 * bubbles.
 */
public final class MiniatureJets {
    private static final float MARGIN = 0.5F / 16F;

    private MiniatureJets() {}

    /** World position of an interior-local point of the region inside the tank (same transform as the miniature). */
    static Vec3 toWorld(TankData t, ClientRegion r, double lx, double ly, double lz) {
        BlockPos min = t.min();
        double fx = (t.sizeX() - 2 * MARGIN) / r.sizeX, fy = (t.sizeY() - 2 * MARGIN) / r.sizeY, fz = (t.sizeZ() - 2 * MARGIN) / r.sizeZ;
        return new Vec3(min.getX() + MARGIN + lx * fx, min.getY() + MARGIN + ly * fy, min.getZ() + MARGIN + lz * fz);
    }

    /**
     * M29.1: where the air hoses of a tank end: the air stones (at their foot) and sponge filters (at the top of the lift
     * tube) that get air, in world coordinates; empty while the interior is not known on this client.
     */
    public static List<Vec3> airTargets(TankData t) {
        ClientRegion r = t.regionId() < 0 ? null : ClientInteriorCache.get(t.regionId());
        if (r == null) return List.of();
        List<Vec3> out = new ArrayList<>();
        for (ClientRegion.Bubbler b : r.bubblers()) out.add(toWorld(t, r, b.x(), b.sponge() ? b.y() : b.y() - 0.3, b.z()));
        return out;
    }

    /** M29.1: the height of the tank floor (the substrate) in world coordinates, for hoses lying on it. */
    public static double floorY(TankData t) {
        ClientRegion r = t.regionId() < 0 ? null : ClientInteriorCache.get(t.regionId());
        return r == null ? t.min().getY() + 0.06 : toWorld(t, r, 0, 0.6, 0).y;
    }

    /** Water surface (M10: per layer, correct for free shapes; the client view knows the tank's blocks). */
    static double surfaceY(TankData t) {
        double h = t.waterSurfaceY() - t.min().getY();
        return t.min().getY() + Math.min(t.sizeY() - 1.0 / 16, Math.max(1.0 / 16, h));
    }

    /** Jet paths of one tank (world coordinates). */
    static List<FilterFlow.Path> paths(TankData t, ClientRegion r) {
        List<ClientRegion.Outlet> outlets = r.outlets();
        if (outlets.isEmpty() || t.waterMb() <= 0) return List.of();
        double surface = surfaceY(t);
        List<FilterFlow.Path> out = new ArrayList<>(outlets.size());
        for (ClientRegion.Outlet o : outlets) {
            Vec3 w = toWorld(t, r, o.x(), o.y(), o.z());
            out.add(new FilterFlow.Path(w, w, w, w, o.facing(), surface, true));
        }
        return out;
    }

    /** Jet length relative to the external filter: a quarter block wide filter at scale 16 gets a short jet. */
    static double jetScale(ClientRegion r) {
        return Math.max(0.25, Math.min(1.0, 0.35 * 16.0 / Math.max(1, r.scale)));
    }

    /** Jet scale for one outlet: never longer than the way to the glass in front of it. */
    static double jetScale(ClientRegion r, TankData t, FilterFlow.Path p) {
        return FilterFlow.fitScale(t, p, jetScale(r));
    }

    static void render(RenderLevelStageEvent event, TankBlockEntity be, ClientRegion region, Vec3 cam) {
        TankData t = be.getTank();
        List<FilterFlow.Path> paths = paths(t, region);
        if (paths.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType rt = FlowRenderer.jetType();
        PoseStack ps = event.getPoseStack();
        float time = (mc.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false)) / 20F;
        List<ClientRegion.Outlet> outlets = region.outlets();
        for (int i = 0; i < paths.size(); i++) {
            FilterFlow.Path p = paths.get(i);
            // M23: a regulated device blows a shorter jet
            double share = i < outlets.size() ? outlets.get(i).share() : 1.0;
            double scale = jetScale(region, t, p) * (0.4 + 0.6 * share);
            int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(p.nozzle()));
            FlowRenderer.jet(ps, buffers.getBuffer(rt), p.jet(8, scale), p.inward(), cam, light, time,
                    (float) (0.05 * scale + 0.018), p.submerged() ? 0.9F : 1.0F);
        }
        buffers.endBatch(rt);
    }

    /** M23: bubble columns of the air stones and sponge filters in the miniature (tiny bubbles up to the water surface). */
    static void bubbles(Minecraft mc, RandomSource rand, TankData t, ClientRegion region, Vec3 cam, boolean sound) {
        if (t.waterMb() <= 0) return;
        double surface = surfaceY(t);
        double fy = (t.sizeY() - 2 * MARGIN) / region.sizeY;
        float size = (float) Math.max(0.02, Math.min(0.06, 0.9 * fy));
        for (ClientRegion.Bubbler b : region.bubblers()) {
            Vec3 w = toWorld(t, region, b.x(), b.y(), b.z());
            if (w.y >= surface || w.distanceToSqr(cam) > 32 * 32) continue;
            double rate = b.sponge() ? 0.5 : 0.3 + 0.6 * b.share();
            if (rand.nextDouble() < rate) {
                double spread = (b.sponge() ? 0.6 : 0.3) * fy * 16 / 16.0;
                mc.level.addParticle(new org.overengineer.bettacraft.registry.ModParticles.AirBubble((float) surface, size),
                        w.x + (rand.nextDouble() - 0.5) * spread, w.y, w.z + (rand.nextDouble() - 0.5) * spread, 0, 0.02 + rand.nextDouble() * 0.01, 0);
            }
            if (sound && rand.nextInt(120) == 0 && w.distanceToSqr(cam) < 10 * 10) {
                var ev = b.sponge() ? org.overengineer.bettacraft.registry.ModSounds.pick(org.overengineer.bettacraft.registry.ModSounds.SPONGE_FILTER_GURGLE,
                        org.overengineer.bettacraft.registry.ModSounds.SPONGE_FILTER_GURGLE_VANILLA)
                        : org.overengineer.bettacraft.registry.ModSounds.pick(org.overengineer.bettacraft.registry.ModSounds.AIR_STONE_BUBBLES,
                        org.overengineer.bettacraft.registry.ModSounds.AIR_STONE_BUBBLES_VANILLA);
                float vol = (float) (0.2 * b.share() * org.overengineer.bettacraft.client.DeviceEffects.volume(org.overengineer.bettacraft.equipment.Regulator.Sound.AIR_STONE));
                if (vol > 0.001F) mc.level.playLocalSound(w.x, w.y, w.z, ev, net.minecraft.sounds.SoundSource.BLOCKS, vol, 0.9F + rand.nextFloat() * 0.2F, false);
            }
        }
    }

    /** Client tick: bubbles at the outlets of nearby tanks (and the quiet filter sound). */
    public static void tickParticles(Minecraft mc, RandomSource rand, boolean sound) {
        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        for (TankBlockEntity be : TankBlockEntity.CLIENT_CONTROLLERS) {
            TankData t = be.getTank();
            if (t == null || t.regionId() < 0 || be.getLevel() != mc.level) continue;
            ClientRegion region = ClientInteriorCache.get(t.regionId());
            if (region == null) continue;
            bubbles(mc, rand, t, region, cam, sound); // M23
            List<FilterFlow.Path> paths = paths(t, region);
            List<ClientRegion.Outlet> outlets = region.outlets();
            for (int i = 0; i < paths.size(); i++) {
                FilterFlow.Path p = paths.get(i);
                if (p.nozzle().distanceToSqr(cam) > 32 * 32) continue;
                ClientRegion.Outlet o = i < outlets.size() ? outlets.get(i) : null;
                double share = o == null ? 1.0 : o.share();
                if (sound && rand.nextInt(90) == 0 && p.nozzle().distanceToSqr(cam) < 12 * 12) {
                    // M23: own sounds (or Minecraft's), quieter with the regulator, per device kind volume
                    boolean pump = o != null && o.pump();
                    var ev = pump ? org.overengineer.bettacraft.registry.ModSounds.pick(org.overengineer.bettacraft.registry.ModSounds.FLOW_PUMP_WHIR,
                            org.overengineer.bettacraft.registry.ModSounds.FLOW_PUMP_WHIR_VANILLA)
                            : org.overengineer.bettacraft.registry.ModSounds.pick(org.overengineer.bettacraft.registry.ModSounds.FILTER_RUNNING,
                            org.overengineer.bettacraft.registry.ModSounds.FILTER_RUNNING_VANILLA);
                    float vol = (float) (0.15 * share * (pump ? org.overengineer.bettacraft.client.DeviceEffects.volume(org.overengineer.bettacraft.equipment.Regulator.Sound.FLOW_PUMP)
                            : org.overengineer.bettacraft.client.DeviceEffects.volume(org.overengineer.bettacraft.equipment.Regulator.Sound.FILTER)));
                    if (vol > 0.001F) mc.level.playLocalSound(p.nozzle().x, p.nozzle().y, p.nozzle().z, ev,
                            net.minecraft.sounds.SoundSource.BLOCKS, vol, 1.2F + rand.nextFloat() * 0.2F, false);
                }
                if (rand.nextInt(3) != 0) continue;
                List<Vec3> jet = p.jet(4, jetScale(region, t, p));
                Vec3 at = jet.get(1 + rand.nextInt(jet.size() - 1));
                if (at.y < p.surfaceY()) {
                    mc.level.addParticle(ParticleTypes.BUBBLE, at.x, at.y, at.z, p.inward().getStepX() * 0.05, 0.02, p.inward().getStepZ() * 0.05);
                } else if (rand.nextInt(3) == 0) {
                    Vec3 hit = jet.getLast();
                    mc.level.addParticle(ParticleTypes.SPLASH, hit.x, p.surfaceY(), hit.z, 0, 0, 0);
                }
            }
        }
    }
}
