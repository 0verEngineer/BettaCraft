package org.overengineer.bettacraft.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.client.render.FilterRenderer;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity;
import org.overengineer.bettacraft.equipment.FilterFlow;
import org.overengineer.bettacraft.equipment.FilterHost;
import org.overengineer.bettacraft.registry.ModSounds;

/**
 * Client tick for running filters (M4): bubbles where the jet enters the water (or along a submerged current),
 * splashes, and the quiet bubbling sound (client config {@code sound.filterSound}). Purely visual.
 */
final class FilterEffects {
    private static final RandomSource RAND = RandomSource.create();
    private static final double RANGE = 32;

    private FilterEffects() {}

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        List<BlockEntity> filters = new ArrayList<>(ExternalFilterBlockEntity.CLIENT_LOADED);
        filters.addAll(CabinetBlockEntity.CLIENT_FILTERS);
        boolean sound = ClientConfig.FILTER_SOUND.get();
        for (BlockEntity be : filters) {
            if (be.getLevel() != mc.level || be.isRemoved()) continue;
            if (be.getBlockPos().getCenter().distanceToSqr(cam) > RANGE * RANGE) continue;
            FilterFlow.Path p = FilterRenderer.path(mc.level, be);
            if (p == null || !p.wet()) continue;
            if (((FilterHost) be).powerShare() <= 0) continue; // M23: switched to 0 %
            Vec3 hit = p.impact();
            if (p.submerged()) {
                if (RAND.nextInt(2) == 0) {
                    org.overengineer.bettacraft.tank.TankBlockEntity c = FilterHost.tankOf(mc.level, ((FilterHost) be).tankBlockPos());
                    List<Vec3> jet = p.jet(6, c == null || c.getTank() == null ? 1.0 : FilterFlow.fitScale(c.getTank(), p, 1.0));
                    Vec3 at = jet.get(1 + RAND.nextInt(jet.size() - 1));
                    mc.level.addParticle(ParticleTypes.BUBBLE, at.x, at.y, at.z, p.inward().getStepX() * 0.15, 0.03, p.inward().getStepZ() * 0.15);
                }
            } else {
                if (RAND.nextInt(2) == 0) {
                    mc.level.addParticle(ParticleTypes.BUBBLE, hit.x + RAND.nextGaussian() * 0.05, hit.y - 0.1 - RAND.nextDouble() * 0.2,
                            hit.z + RAND.nextGaussian() * 0.05, 0, -0.05, 0);
                }
                if (RAND.nextInt(4) == 0) mc.level.addParticle(ParticleTypes.SPLASH, hit.x, p.surfaceY() + 0.02, hit.z, 0, 0.05, 0);
            }
            // M23: own sound (or Minecraft's), quieter with the power regulator and the client volume of the filters
            double share = ((FilterHost) be).powerShare();
            float vol = (float) ((0.25F + RAND.nextFloat() * 0.1F) * share * DeviceEffects.volume(org.overengineer.bettacraft.equipment.Regulator.Sound.FILTER));
            if (sound && ModSounds.devices() && vol > 0.001F && RAND.nextInt(70) == 0 && hit.distanceToSqr(cam) < 16 * 16) {
                mc.level.playLocalSound(hit.x, hit.y, hit.z, ModSounds.pick(ModSounds.FILTER_RUNNING, ModSounds.FILTER_RUNNING_VANILLA), SoundSource.BLOCKS,
                        vol, 0.9F + RAND.nextFloat() * 0.2F, false);
            }
        }
        org.overengineer.bettacraft.client.interior.MiniatureJets.tickParticles(mc, RAND, sound);
    }
}
