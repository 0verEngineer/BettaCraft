package org.overengineer.bettacraft.reef;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;

/**
 * M20: the technique of a reef tank. Protein skimmers and live rock clean salt water (extra filter capacity, nothing in
 * fresh water), flow pumps and filters move the water ({@link #flow}), and the colour of the lights decides how much
 * of the light the corals can use ({@link #spectrum}: actinic blue + white is best).
 */
public final class TankReef {
    private TankReef() {}

    /** Protein skimmers hanging on the tank (also without salt water; for the power and the menu). */
    public static int skimmerCount(Level level, TankBlockEntity c) {
        return TankEquipment.skimmers(level, c).size();
    }

    /** Water blocks the skimmers keep clean: only in salt water (no foam in fresh water) and with power. */
    public static double skimmerBlocks(Level level, TankBlockEntity c) {
        if (!c.getTank().marine() || !c.filterPowered()) return 0;
        return skimmerCount(level, c) * (double) ServerConfig.getOr(ServerConfig.REEF_SKIMMER_BLOCKS, 48);
    }

    /** Blocks of live rock in the interior (0 without one). */
    public static int liveRock(ServerLevel level, TankData t) {
        var r = InteriorRegions.get(level.getServer()).get(t.regionId());
        if (r == null || !r.built()) return 0;
        return InteriorManager.equipment(level.getServer(), r).liveRock();
    }

    /** Water blocks the live rock keeps clean (salt water only): per tank block worth of live rock. */
    public static double liveRockBlocks(ServerLevel level, TankData t) {
        if (!t.marine()) return 0;
        var r = InteriorRegions.get(level.getServer()).get(t.regionId());
        if (r == null || !r.built()) return 0;
        int n = InteriorManager.equipment(level.getServer(), r).liveRock();
        if (n <= 0) return 0;
        return n / Math.pow(r.scale(), 3) * ServerConfig.getOr(ServerConfig.REEF_LIVE_ROCK_BLOCKS, 24.0);
    }

    /** Flow pumps under water in the interior. */
    public static int pumps(ServerLevel level, TankData t) {
        var r = InteriorRegions.get(level.getServer()).get(t.regionId());
        if (r == null || !r.built()) return 0;
        var eq = InteriorManager.equipment(level.getServer(), r);
        return eq.pumps().isEmpty() ? 0 : eq.pumpsBelow(TankCare.waterLocalHeight(t, r.scale()));
    }

    /**
     * Water movement relative to what strong-flow corals need: 1 = strong flow everywhere. Pumps count fully, the
     * outlets of external filters a third, internal filters a sixth of a pump. Without power nothing moves.
     */
    public static double flow(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        if (t.waterMb() <= 0 || !c.filterPowered()) return 0;
        double pump = ServerConfig.getOr(ServerConfig.REEF_PUMP_BLOCKS, 24);
        // M23: by the power regulators (corner filters like internal ones)
        double ext = 0;
        for (var f : TankEquipment.filters(level, c)) ext += f.powerShare();
        double[] in = TankWater.interiorShares(level, t);
        double moved = in[2] * pump + ext * pump / 3 + (in[0] + in[1]) * pump / 6;
        return flow(moved, t.waterBlocks());
    }

    /** Pure: flow from the water blocks the pumps move and the water volume. */
    public static double flow(double movedBlocks, double waterBlocks) {
        if (waterBlocks <= 0) return 0;
        return Math.min(3.0, movedBlocks / waterBlocks);
    }

    // ------------------------------------------------------------------ light spectrum

    /** Light colours that count as actinic (blue) for the corals. */
    public static boolean actinic(DyeColor c) {
        return c == DyeColor.BLUE || c == DyeColor.LIGHT_BLUE || c == DyeColor.CYAN || c == DyeColor.PURPLE;
    }

    /** {white-ish lit bars, actinic lit bars, other coloured lit bars} above the tank. */
    public static int[] lightColours(Level level, TankData t) {
        int white = 0, blue = 0, other = 0;
        for (BlockPos p : TankCare.aboveTop(t)) {
            if (!level.isLoaded(p)) continue;
            BlockState s = level.getBlockState(p);
            if (!(s.getBlock() instanceof TankLightBlock) || !s.getValue(TankLightBlock.LIT)) continue;
            DyeColor col = s.getValue(TankLightBlock.COLOR);
            if (actinic(col)) blue++;
            else if (col == DyeColor.WHITE || col == DyeColor.LIGHT_GRAY) white++;
            else other++;
        }
        return new int[]{white, blue, other};
    }

    /**
     * Share of the light the corals use (0..1): actinic blue mixed with white is best (1), only blue 0.85, only white
     * 0.7, other colours 0.4, sunlight or no tank light 0.6.
     */
    public static double spectrum(int white, int blue, int other) {
        int all = white + blue + other;
        if (all <= 0) return 0.6;
        double w = white / (double) all, b = blue / (double) all, o = other / (double) all;
        // white and blue together: up to +0.3 (half and half reaches 1)
        double v = 0.7 * w + 0.85 * b + 0.4 * o + 0.3 * Math.min(w, b) * 2;
        return Math.max(0, Math.min(1, v));
    }

    public static double spectrum(Level level, TankData t) {
        int[] c = lightColours(level, t);
        return spectrum(c[0], c[1], c[2]);
    }

    /** Whether actinic light shines on the tank (corals glow). */
    public static boolean glows(Level level, TankData t) {
        return lightColours(level, t)[1] > 0;
    }
}
