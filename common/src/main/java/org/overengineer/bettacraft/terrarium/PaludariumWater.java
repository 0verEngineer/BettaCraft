package org.overengineer.bettacraft.terrarium;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;

/**
 * M35 part b: the water animals in the water part of a paludarium (our fish, the fish of other mods with a profile and the
 * ones the M16 rules admit). A paludarium has its water in a part of its height by nature (a third, a half), so the fill
 * rule of the aquariums ({@code tank.minFillToInsert} of the whole tank) would refuse every fish; there the water part decides:
 * it must be deep enough for the animal ({@link #depthFor}), and the stocking (the water blocks) and the size rule of the big
 * animals (the water part, M30) measure it as always. In the tank the fish stay in the water part
 * ({@code foreign.ForeignWaterPart} for the fish of other mods, the obstacles of the interior for ours).
 */
public final class PaludariumWater {
    /** The shallowest water part (tank blocks) for any water animal. */
    public static final double MIN_DEPTH = 0.5;

    private PaludariumWater() {}

    /** Whether the water part rule applies: the terraria on and the tank a paludarium now. */
    public static boolean applies(ServerLevel level, TankBlockEntity c) {
        return TankClimate.enabled() && c.getTank().waterMb() > 0 && TankClimate.refreshMode(level, c) == TerrariumMode.PALUDARIUM;
    }

    /** The water depth (tank blocks) an animal of this height needs: its height and a little more, at least {@link #MIN_DEPTH}. Pure. */
    public static double depthFor(double height) {
        return Math.max(MIN_DEPTH, height + 0.1);
    }

    /** The depth of the water of the tank (tank blocks above its lowest floor). */
    public static double depth(TankData t) {
        return t.waterMb() <= 0 ? 0 : t.waterSurfaceY() - t.min().getY();
    }

    /** Why this water animal cannot go into the water part of the paludarium, or null. */
    @Nullable
    public static Component refusal(TankData t, EntityType<?> type) {
        double need = depthFor(type.getDimensions().height()), have = depth(t);
        if (have + 1e-6 >= need) return null;
        return Component.translatable("message.bettacraft.paludarium.too_shallow", type.getDescription(), fmt(have), fmt(need));
    }

    /**
     * A spot in the open water of the paludarium for a water animal of this height (over a water column, under the surface,
     * above the ground and the decoration), or null if there is none.
     */
    @Nullable
    public static net.minecraft.world.phys.Vec3 waterSpot(ServerLevel level, TankData t, net.minecraft.util.RandomSource r, double height) {
        org.overengineer.bettacraft.critters.TankTerrain ter = new org.overengineer.bettacraft.critters.TankTerrain(t, level.getServer(),
                org.overengineer.bettacraft.tank.interior.InteriorRegions.get(level.getServer()).get(t.regionId()), level.isBrightOutside());
        double surface = ter.surface();
        if (!Double.isFinite(surface)) return null;
        double m = 0.3;
        for (int i = 0; i < 40; i++) {
            double x = t.min().getX() + m + r.nextDouble() * Math.max(0.01, t.sizeX() - 2 * m);
            double z = t.min().getZ() + m + r.nextDouble() * Math.max(0.01, t.sizeZ() - 2 * m);
            double g = ter.ground(x, z);
            if (!Double.isFinite(g) || g > surface - Math.min(height, 0.3) - 0.1) continue;
            double y = g + 0.05 + r.nextDouble() * Math.max(0, surface - g - height - 0.1);
            if (!t.contains(net.minecraft.core.BlockPos.containing(x, y, z))) continue;
            return new net.minecraft.world.phys.Vec3(x, y, z);
        }
        return null;
    }

    private static String fmt(double v) {
        return String.format(java.util.Locale.ROOT, "%.1f", v);
    }
}
