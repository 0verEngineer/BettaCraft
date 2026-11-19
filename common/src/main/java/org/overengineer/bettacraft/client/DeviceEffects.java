package org.overengineer.bettacraft.client;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.platform.network.PacketDistributor;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.equipment.AirPumpBlockEntity;
import org.overengineer.bettacraft.equipment.DeviceSegmentBlock;
import org.overengineer.bettacraft.equipment.Regulator;
import org.overengineer.bettacraft.equipment.SegmentBlock;
import org.overengineer.bettacraft.network.RegulatorPayload;
import org.overengineer.bettacraft.registry.ModParticles;
import org.overengineer.bettacraft.registry.ModSounds;

/**
 * M23 client side of the devices: the hum of running air pumps (a looping sound per pump nearby), the bubble columns
 * and sound of the air stones inside a tank interior, the bubbles of the sponge filters there, the power regulator
 * (screen on right-click, sneak + mouse wheel) and the per-device volumes ({@code sound.*Volume}).
 */
public final class DeviceEffects {
    private static final RandomSource RAND = RandomSource.create();
    private static final Map<BlockPos, PumpHum> HUMS = new HashMap<>();
    /** Water surface (interior y) per interior region, learnt from the air stones (their block entities know it). */
    private static final it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap SURFACE = new it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap();
    /** Running sponge filters near the player in a tank interior: {anchor x, y, z, top y}, found every two seconds. */
    private static final java.util.List<int[]> SPONGES = new java.util.ArrayList<>();
    private static int scanTicks;

    private DeviceEffects() {}

    /** M52: the air bubble particle's provider is registered by the loader's client glue. */
    public static void init() {
        org.overengineer.bettacraft.platform.client.ClientEvents.TICK_POST.add(DeviceEffects::tick);
        org.overengineer.bettacraft.platform.client.ClientEvents.MOUSE_SCROLL.add(DeviceEffects::onScroll);
        org.overengineer.bettacraft.util.ClientHooks.regulator = pos -> Minecraft.getInstance().gui.setScreen(
                new org.overengineer.bettacraft.client.screen.RegulatorScreen(pos));
        org.overengineer.bettacraft.util.ClientHooks.bubbles = DeviceEffects::stoneTick;
        // M24
        org.overengineer.bettacraft.util.ClientHooks.waterChanger = pos -> Minecraft.getInstance().gui.setScreen(
                new org.overengineer.bettacraft.client.screen.WaterChangerScreen(pos));
        org.overengineer.bettacraft.util.ClientHooks.filter = pos -> Minecraft.getInstance().gui.setScreen(
                new org.overengineer.bettacraft.client.screen.MultiblockFilterScreen(pos));
        org.overengineer.bettacraft.util.ClientHooks.monitor = pos -> Minecraft.getInstance().gui.setScreen(
                new org.overengineer.bettacraft.client.screen.MonitorEditorScreen(pos)); // M27
    }

    /** Dev smoke test: running sponge filters found around the player in a tank interior. */
    public static int spongeFiltersForTest() {
        return SPONGES.size();
    }

    /** Dev smoke test: bubbles the sponge filters sent up so far, and the surface they rise to. */
    public static String spongeBubblesForTest() {
        StringBuilder b = new StringBuilder().append(spongeBubbles);
        for (int[] sp : SPONGES) b.append(" at ").append(sp[0]).append(',').append(sp[3]).append(',').append(sp[2]).append(" surface ")
                .append(SURFACE.getOrDefault(org.overengineer.bettacraft.tank.interior.InteriorRegion.idAt(sp[0], sp[2]), Float.NaN));
        return b.toString();
    }

    private static int spongeBubbles;

    /** Client volume 0..1 of a kind of device (M29.1: 0 for the air and flow pumps unless their sound switch is on). */
    public static double volume(Regulator.Sound kind) {
        if (!org.overengineer.bettacraft.registry.ModSounds.devices()) return 0.0; // M33 part b: features.sounds.enabled
        try {
            return switch (kind) {
                case FILTER -> ClientConfig.VOLUME_FILTER.get();
                case AIR_PUMP -> ClientConfig.AIR_PUMP_SOUND.get() ? ClientConfig.VOLUME_AIR_PUMP.get() : 0.0;
                case AIR_STONE -> ClientConfig.VOLUME_AIR_STONE.get();
                case FLOW_PUMP -> ClientConfig.FLOW_PUMP_SOUND.get() ? ClientConfig.VOLUME_FLOW_PUMP.get() : 0.0;
            };
        } catch (IllegalStateException e) {
            return 1.0;
        }
    }

    // ------------------------------------------------------------------ air pumps

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            HUMS.clear();
            return;
        }
        if (mc.isPaused()) return;
        Vec3 cam = mc.gameRenderer.mainCamera().position();
        spongeFilters(mc);
        if (volume(Regulator.Sound.AIR_PUMP) <= 0.001) { // M29.1: the hum is off by default
            HUMS.values().forEach(PumpHum::silence);
            HUMS.clear();
            return;
        }
        for (AirPumpBlockEntity be : AirPumpBlockEntity.CLIENT_LOADED) hum(mc, cam, be);
        // M29.1: the air pumps in the cabinets hum too (a little lower, inside the cabinet)
        for (org.overengineer.bettacraft.equipment.CabinetBlockEntity be : org.overengineer.bettacraft.equipment.CabinetBlockEntity.CLIENT_AIR_PUMPS) hum(mc, cam, be);
        HUMS.values().removeIf(PumpHum::isStopped);
    }

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity & org.overengineer.bettacraft.equipment.AirSource> void hum(Minecraft mc, Vec3 cam, T be) {
        if (be.getLevel() != mc.level || be.isRemoved() || !be.airRunning() || !ModSounds.devices()) return;
        BlockPos p = be.getBlockPos();
        if (net.minecraft.world.phys.Vec3.atCenterOf(p).distanceToSqr(cam) > 16 * 16) return;
        PumpHum h = HUMS.get(p);
        if (h == null || h.isStopped()) {
            h = new PumpHum(be, be);
            HUMS.put(p.immutable(), h);
            mc.getSoundManager().play(h);
        }
    }

    /** The looping hum of one air pump (free-standing or in a cabinet); stops when the pump stands still, goes or is out of range. */
    static final class PumpHum extends AbstractTickableSoundInstance {
        private final net.minecraft.world.level.block.entity.BlockEntity be;
        private final org.overengineer.bettacraft.equipment.AirSource air;

        PumpHum(net.minecraft.world.level.block.entity.BlockEntity be, org.overengineer.bettacraft.equipment.AirSource air) {
            super(ModSounds.pick(ModSounds.AIR_PUMP_HUM, ModSounds.AIR_PUMP_HUM_VANILLA), SoundSource.BLOCKS, RandomSource.create());
            this.be = be;
            this.air = air;
            this.looping = true;
            this.delay = 0;
            BlockPos p = be.getBlockPos();
            this.x = p.getX() + 0.5;
            this.y = p.getY() + 0.25;
            this.z = p.getZ() + 0.5;
            this.volume = level();
        }

        private float level() {
            return (float) (air.airShare() * volume(Regulator.Sound.AIR_PUMP) * (air instanceof AirPumpBlockEntity ? 1.0 : 0.7));
        }

        void silence() {
            stop();
        }

        @Override
        public void tick() {
            Minecraft mc = Minecraft.getInstance();
            if (be.isRemoved() || !air.airRunning() || mc.level == null || be.getLevel() != mc.level
                    || net.minecraft.world.phys.Vec3.atCenterOf(be.getBlockPos()).distanceToSqr(mc.gameRenderer.mainCamera().position()) > 18 * 18) {
                stop();
                return;
            }
            volume = Math.max(0.0001F, level());
            pitch = 0.9F + 0.2F * (float) air.airShare();
        }
    }

    // ------------------------------------------------------------------ inside the tank interior

    /** Client tick of an air stone (its block entity): the bubble column up to the water surface and now and then its sound. */
    private static void stoneTick(Level level, BlockPos pos, BlockState state, float surfaceY) {
        if (!state.hasProperty(DeviceSegmentBlock.ACTIVE) || !state.getValue(DeviceSegmentBlock.ACTIVE)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.isPaused() || mc.player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) > 48 * 48) return;
        double share = Regulator.share(state);
        if (share <= 0) return;
        if (!Float.isNaN(surfaceY)) SURFACE.put(org.overengineer.bettacraft.tank.interior.InteriorRegion.idAt(pos.getX(), pos.getZ()), surfaceY);
        float top = Float.isNaN(surfaceY) ? pos.getY() + 6 : surfaceY;
        if (top <= pos.getY() + 0.4) return;
        int n = RAND.nextDouble() < 0.4 + 0.6 * share ? 1 + (share > 0.6 && RAND.nextBoolean() ? 1 : 0) : 0;
        for (int i = 0; i < n; i++) {
            level.addParticle(new ModParticles.AirBubble(top, 0.07F + 0.05F * RAND.nextFloat()), true, false,
                    pos.getX() + 0.5 + (RAND.nextDouble() - 0.5) * 0.4, pos.getY() + 0.4, pos.getZ() + 0.5 + (RAND.nextDouble() - 0.5) * 0.4,
                    (RAND.nextDouble() - 0.5) * 0.02, 0.06 + RAND.nextDouble() * 0.03, (RAND.nextDouble() - 0.5) * 0.02);
        }
        if (ClientConfig.FILTER_SOUND.get() && RAND.nextInt(50) == 0 && mc.player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) < 16 * 16) {
            float vol = (float) (0.5 * share * volume(Regulator.Sound.AIR_STONE));
            if (vol > 0.001F) level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    ModSounds.pick(ModSounds.AIR_STONE_BUBBLES, ModSounds.AIR_STONE_BUBBLES_VANILLA), SoundSource.BLOCKS, vol,
                    0.9F + RAND.nextFloat() * 0.2F, false);
        }
    }

    /**
     * Sponge filters inside a tank interior: every two seconds the loaded sections around the player are searched for running
     * sponge filters (cheap: only sections whose palette has one); every tick big bubbles rise out of their lift tubes up to
     * the water surface (known from the air stones of the region, else 8 blocks).
     */
    private static void spongeFilters(Minecraft mc) {
        if (!org.overengineer.bettacraft.tank.interior.InteriorManager.isInterior(mc.level)) {
            SPONGES.clear();
            return;
        }
        if (--scanTicks <= 0) {
            scanTicks = 40;
            SPONGES.clear();
            var block = org.overengineer.bettacraft.registry.ModBlocks.SPONGE_FILTER_SEGMENT.get();
            BlockPos at = mc.player.blockPosition();
            for (int cx = (at.getX() >> 4) - 2; cx <= (at.getX() >> 4) + 2; cx++)
                for (int cz = (at.getZ() >> 4) - 2; cz <= (at.getZ() >> 4) + 2; cz++) {
                    if (!mc.level.hasChunk(cx, cz)) continue;
                    var chunk = mc.level.getChunk(cx, cz);
                    var sections = chunk.getSections();
                    for (int si = 0; si < sections.length; si++) {
                        var sec = sections[si];
                        if (sec == null || sec.hasOnlyAir() || !sec.maybeHas(s -> s.is(block))) continue;
                        int baseY = chunk.getSectionYFromSectionIndex(si) << 4;
                        for (int y = 0; y < 16; y++)
                            for (int z = 0; z < 16; z++)
                                for (int x = 0; x < 16; x++) {
                                    BlockState s = sec.getBlockState(x, y, z);
                                    if (!s.is(block) || s.getValue(SegmentBlock.PART) != SegmentBlock.Part.ANCHOR || !s.getValue(DeviceSegmentBlock.ACTIVE)) continue;
                                    int wx = (cx << 4) + x, wy = baseY + y, wz = (cz << 4) + z;
                                    int top = wy;
                                    while (top < wy + 32 && mc.level.getBlockState(new BlockPos(wx, top + 1, wz)).is(block)) top++;
                                    SPONGES.add(new int[]{wx, wy, wz, top + 1});
                                    if (SPONGES.size() >= 32) return;
                                }
                    }
                }
        }
        for (int[] sp : SPONGES) {
            if (mc.player.distanceToSqr(sp[0], sp[3], sp[2]) > 48 * 48) continue;
            int region = org.overengineer.bettacraft.tank.interior.InteriorRegion.idAt(sp[0], sp[2]);
            float surface = SURFACE.containsKey(region) ? SURFACE.get(region) : sp[3] + 8;
            if (surface <= sp[3]) continue;
            double x = sp[0] + 1.0, z = sp[2] + 1.0;
            if (RAND.nextInt(3) == 0) {
                int n = 1 + RAND.nextInt(3);
                spongeBubbles += n;
                // force: also further than 32 blocks (the interior is large; we limit it to 48 ourselves)
                for (int i = 0; i < n; i++) mc.level.addParticle(new ModParticles.AirBubble(surface, 0.14F + 0.08F * RAND.nextFloat()), true, false,
                        x + (RAND.nextDouble() - 0.5) * 0.25, sp[3] + RAND.nextDouble() * 0.2, z + (RAND.nextDouble() - 0.5) * 0.25, 0, 0.05 + RAND.nextDouble() * 0.04, 0);
            }
            if (ClientConfig.FILTER_SOUND.get() && RAND.nextInt(60) == 0 && mc.player.distanceToSqr(x, sp[3], z) < 16 * 16) {
                float vol = (float) (0.5 * volume(Regulator.Sound.AIR_STONE));
                if (vol > 0.001F) mc.level.playLocalSound(x, sp[3], z, ModSounds.pick(ModSounds.SPONGE_FILTER_GURGLE, ModSounds.SPONGE_FILTER_GURGLE_VANILLA),
                        SoundSource.BLOCKS, vol, 0.9F + RAND.nextFloat() * 0.2F, false);
            }
        }
    }

    // ------------------------------------------------------------------ sneak + mouse wheel

    private static boolean onScroll(double deltaX, double deltaY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gui.screen() != null || !mc.player.isShiftKeyDown()) return false;
        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult bh) || hit.getType() != HitResult.Type.BLOCK) return false;
        Regulator.Device d = Regulator.find(mc.level, bh.getBlockPos());
        if (d == null) return false;
        if (!Features.enabled(Feature.POWER_REGULATOR) && !d.alwaysAdjustable()) return false;
        int step = deltaY > 0 ? 1 : deltaY < 0 ? -1 : 0;
        if (step == 0) return false;
        PacketDistributor.sendToServer(new RegulatorPayload(d.pos(), step, true));
        return true;
    }
}
