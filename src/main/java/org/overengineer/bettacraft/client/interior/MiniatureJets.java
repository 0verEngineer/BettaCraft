package org.overengineer.bettacraft.client.interior;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
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

    static double surfaceY(TankData t) {
        double area = (double) t.sizeX() * t.sizeZ();
        double h = area <= 0 ? 0 : t.waterMb() / (area * TankData.MB_PER_BLOCK);
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
        for (FilterFlow.Path p : paths) {
            double scale = jetScale(region, t, p);
            int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(p.nozzle()));
            FlowRenderer.jet(ps, buffers.getBuffer(rt), p.jet(8, scale), p.inward(), cam, light, time,
                    (float) (0.05 * scale + 0.018), p.submerged() ? 0.9F : 1.0F);
        }
        buffers.endBatch(rt);
    }

    /** Client tick: bubbles at the outlets of nearby tanks (and the quiet filter sound). */
    public static void tickParticles(Minecraft mc, RandomSource rand, boolean sound) {
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        for (TankBlockEntity be : TankBlockEntity.CLIENT_CONTROLLERS) {
            TankData t = be.getTank();
            if (t == null || t.regionId() < 0 || be.getLevel() != mc.level) continue;
            ClientRegion region = ClientInteriorCache.get(t.regionId());
            if (region == null) continue;
            for (FilterFlow.Path p : paths(t, region)) {
                if (p.nozzle().distanceToSqr(cam) > 32 * 32) continue;
                if (sound && rand.nextInt(90) == 0 && p.nozzle().distanceToSqr(cam) < 12 * 12) {
                    mc.level.playLocalSound(p.nozzle().x, p.nozzle().y, p.nozzle().z, org.overengineer.bettacraft.registry.ModSounds.FILTER_RUNNING.get(),
                            net.minecraft.sounds.SoundSource.BLOCKS, 0.15F, 1.2F + rand.nextFloat() * 0.2F, false);
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
