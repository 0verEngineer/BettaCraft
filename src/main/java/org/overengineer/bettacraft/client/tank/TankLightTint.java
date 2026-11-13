package org.overengineer.bettacraft.client.tank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.equipment.TankLightColors;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;

/**
 * M18: colour of the light that shines into a tank, for the water renderer. Every 10 ticks the client looks at the lit
 * lights on top of each tank it knows (the same places the care loop counts, {@link TankData#openTops}) and blends
 * their colours ({@link TankLightColors#blend}); when the colour of a tank changes, its blocks are meshed again. The
 * chunk mesher (worker threads, also Sodium) reads the colour per tank block through its controller position.
 * The colour comes from the block states of the lights, which every client has: nothing extra is synced.
 */
public final class TankLightTint {
    /** Controller position -> blended RGB of the lit lights (absent = no tint). */
    private static final Map<Long, Integer> COLORS = new ConcurrentHashMap<>();
    /** Open tops per client tank view (a new view object arrives with every update, the old one is collected). */
    private static final Map<TankData, List<BlockPos>> TOPS = Collections.synchronizedMap(new WeakHashMap<>());
    private static int ticks;
    private static double lastStrength = -1;

    private TankLightTint() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            COLORS.clear();
            return;
        }
        if (++ticks % 10 != 0) return;
        double strength = strength();
        boolean all = strength != lastStrength;
        lastStrength = strength;
        Set<Long> seen = new HashSet<>();
        for (TankBlockEntity be : TankBlockEntity.CLIENT_CONTROLLERS) {
            if (be.getLevel() != level || be.isRemoved()) continue;
            TankData t = be.getTank();
            if (t == null) continue;
            long key = be.getBlockPos().asLong();
            seen.add(key);
            int c = compute(level, t);
            Integer old = COLORS.get(key);
            boolean changed = old == null ? c != 0 : old != c;
            if (c == 0) COLORS.remove(key);
            else COLORS.put(key, c);
            if (changed || all && c != 0) {
                BlockPos mn = t.min(), mx = t.max();
                mc.levelRenderer.setBlocksDirty(mn.getX(), mn.getY(), mn.getZ(), mx.getX(), mx.getY(), mx.getZ());
            }
        }
        COLORS.keySet().retainAll(seen);
    }

    /**
     * Blended colour of the lit lights on top of the tank (0 = none or only white ones); M20: the salinity (whole ppt,
     * 0..60) rides in the top byte, the water renderer turns salt water clear blue ({@link #salt}).
     */
    public static int compute(ClientLevel level, TankData t) {
        return lights(level, t) | (Math.min(60, Math.round(t.salinity())) << 24);
    }

    /** M20: the salinity part of a value of {@link #colorAt} (ppt). */
    public static int salt(int value) {
        return value >>> 24;
    }

    /** M20: the light colour part of a value of {@link #colorAt} (RGB, 0 = none). */
    public static int rgb(int value) {
        return value & 0xFFFFFF;
    }

    private static int lights(ClientLevel level, TankData t) {
        List<BlockPos> tops = TOPS.computeIfAbsent(t, TankData::openTops);
        List<DyeColor> lit = new ArrayList<>();
        for (BlockPos p : tops) {
            if (!level.isLoaded(p)) continue;
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof TankLightBlock && s.getValue(TankLightBlock.LIT)) lit.add(s.getValue(TankLightBlock.COLOR));
        }
        return TankLightColors.blend(lit);
    }

    /** Light colour for the tank block at {@code pos} (0 = no tint); safe on meshing threads. */
    public static int colorAt(BlockAndTintGetter level, BlockPos pos) {
        if (COLORS.isEmpty()) return 0;
        if (!(level.getBlockEntity(pos) instanceof TankBlockEntity be)) return 0;
        BlockPos c = be.getControllerPos();
        if (c == null) return 0;
        Integer v = COLORS.get(c.asLong());
        return v == null ? 0 : v;
    }

    /** Current colour of a tank by its controller (0 = none); for the smoke tests. */
    public static int colorOf(BlockPos controller) {
        Integer v = COLORS.get(controller.asLong());
        return v == null ? 0 : rgb(v);
    }

    /** M20: salinity the water of a tank is drawn with (whole ppt); for the smoke tests. */
    public static int saltOf(BlockPos controller) {
        Integer v = COLORS.get(controller.asLong());
        return v == null ? 0 : salt(v);
    }

    public static float strength() {
        try {
            return ClientConfig.LIGHT_TINT.get().floatValue();
        } catch (IllegalStateException e) {
            return 0.6F; // config not loaded yet
        }
    }
}
