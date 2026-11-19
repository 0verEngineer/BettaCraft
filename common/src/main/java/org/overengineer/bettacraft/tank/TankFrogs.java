package org.overengineer.bettacraft.tank;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.frog.DwarfFrogEntity;
import org.overengineer.bettacraft.food.BettaFood;

/**
 * The dwarf clawed frogs of a tank (M14): air above the water, slow eaters (bettas snatch their food), the log.
 */
public final class TankFrogs {
    /** Smallest air gap (blocks) between the water surface and the top of the tank a frog can breathe in. */
    public static final double MIN_AIR = 0.04;

    private TankFrogs() {}

    /**
     * Whether a frog can breathe at the surface of this tank: the water stops below the top of the tank (the air gap of
     * {@code tank.waterHeadroom}, or a tank that is not full). A full tank without the gap has no air.
     */
    public static boolean hasAir(TankData t) {
        if (t.waterMb() <= 0) return true;
        double top = Double.NEGATIVE_INFINITY;
        int layer = (int) Math.floor(t.waterSurfaceY() - 1e-4);
        // the open top over the surface layer: the highest tank block above the surface in any column of that layer
        for (long l : t.members()) {
            if (net.minecraft.core.BlockPos.getY(l) == layer) {
                int x = net.minecraft.core.BlockPos.getX(l), z = net.minecraft.core.BlockPos.getZ(l);
                int y = layer;
                while (t.contains(net.minecraft.core.BlockPos.asLong(x, y + 1, z))) y++;
                top = Math.max(top, y + 1);
            }
        }
        if (!Double.isFinite(top)) return true;
        return top - t.waterSurfaceY() >= MIN_AIR;
    }

    public static List<DwarfFrogEntity> frogs(ServerLevel level, TankData tank) {
        List<DwarfFrogEntity> out = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(level, tank)) if (e instanceof DwarfFrogEntity f) out.add(f);
        return out;
    }

    /** Frogs that eat this food: live and frozen food (larvae, artemia) and sinking pellets; no fry food. */
    public static boolean eats(BettaFood.Kind kind) {
        return kind == BettaFood.Kind.LIVE || kind == BettaFood.Kind.NORMAL;
    }

    /**
     * Feeds the frogs with what is left of {@code portions} after the bettas ate. While bettas are in the tank each frog
     * only gets its share with the chance {@code frogs.frogFoodShareWithBettas} (they are slow eaters), unless the food is
     * aimed at them ({@code targeted}: sneak + right-click). Returns the portions the frogs ate.
     */
    public static int feed(ServerLevel level, TankBlockEntity c, BettaFood.Kind kind, int portions, boolean bettasThere, boolean targeted) {
        if (!eats(kind) || portions <= 0) return 0;
        List<DwarfFrogEntity> frogs = frogs(level, c.getTank());
        if (frogs.isEmpty()) return 0;
        frogs.sort(java.util.Comparator.comparingDouble(DwarfFrogEntity::daysSinceFed).reversed());
        double share = ServerConfig.getOr(ServerConfig.FROGS_FOOD_SHARE, 0.4);
        int used = 0;
        int missed = 0;
        for (DwarfFrogEntity f : frogs) {
            if (used >= portions) break;
            if (bettasThere && !targeted && level.getRandom().nextDouble() >= share) {
                missed++;
                continue;
            }
            f.feed();
            used++;
        }
        if (used > 0) TankCare.log(level, c, "log.bettacraft.frogs_fed", String.valueOf(used));
        if (missed > 0 && used == 0) TankCare.log(level, c, "log.bettacraft.frogs_missed", String.valueOf(missed));
        return used;
    }

    /** Sneak + right-click on the tank with frog food: the food goes straight to the frogs. */
    public static boolean feedTargeted(ServerLevel level, TankBlockEntity c, Player player, ItemStack stack) {
        BettaFood.Kind kind = BettaFood.kind(stack);
        if (kind == null || !eats(kind)) return false;
        List<DwarfFrogEntity> frogs = frogs(level, c.getTank());
        if (frogs.isEmpty()) return false;
        int n = feed(level, c, kind, BettaFood.fishPerItem(kind), true, true);
        if (n <= 0) return false;
        // M52c: the particle item before the stack shrinks (a particle of an empty stack throws since 26.1)
        var shown = net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(stack.copyWithCount(1));
        if (!player.getAbilities().instabuild) stack.shrink(1);
        for (DwarfFrogEntity f : frogs) {
            level.sendParticles(new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM, shown),
                    f.getX(), f.getY() + 0.2, f.getZ(), 6, 0.05, 0.05, 0.05, 0.01);
        }
        return true;
    }

    /** Care step: a log line when the frogs have no air. */
    public static void step(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        List<DwarfFrogEntity> frogs = frogs(level, t);
        if (frogs.isEmpty()) {
            c.frogAirLogged = false;
            return;
        }
        boolean air = hasAir(t);
        if (!air && !c.frogAirLogged) {
            TankCare.log(level, c, "log.bettacraft.frogs_no_air");
            c.frogAirLogged = true;
        } else if (air) {
            c.frogAirLogged = false;
        }
    }

    public static void onDeath(ServerLevel level, DwarfFrogEntity frog, DamageSource source) {
        TankBlockEntity c = TankInhabitants.resolveController(level, frog);
        if (c == null) return;
        String cause = source.is(net.minecraft.world.damagesource.DamageTypes.DROWN) ? "drowned"
                : source.is(net.minecraft.world.damagesource.DamageTypes.STARVE) ? "starved" : "other";
        TankCare.log(level, c, "log.bettacraft.frog_died", TankCare.nameArg(frog), "@cause.bettacraft.frog." + cause);
    }

    /** Frogs that have run out of air (menu warning). */
    public static boolean anyDrowning(ServerLevel level, TankData t) {
        for (DwarfFrogEntity f : frogs(level, t)) if (f.breath() <= 0) return true;
        return false;
    }
}
