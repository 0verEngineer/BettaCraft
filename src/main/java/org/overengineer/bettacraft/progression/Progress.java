package org.overengineer.bettacraft.progression;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.genetics.BettaTypes;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankTemperature;

/**
 * Progression events (M6): fires the {@code bettacraft:event} advancement criterion and hands catches/crafts to the
 * handbook. Events that happen in a tank without a player (nest, spawning, growing up) go to the tank owner (if online)
 * and every player within {@link #NEAR} blocks of the tank.
 */
public final class Progress {
    public static final double NEAR = 32.0;

    public static final String CATCH_BETTA = "catch_betta";
    public static final String TANK_BUILT = "tank_built";
    public static final String TANK_FILLED = "tank_filled";
    public static final String INSERTED = "inserted";
    public static final String IDEAL_TEMPERATURE = "ideal_temperature";
    public static final String FILTER = "filter_attached";
    public static final String FEEDER = "feeder";
    public static final String RECOVERED = "recovered";
    public static final String FIGHT = "fight";
    public static final String NEST_BUILT = "nest_built";
    public static final String SPAWNED = "spawned";
    public static final String FRY_FREE = "fry_free";
    public static final String GROWN_UP = "grown_up";
    public static final String VARIETY = "variety_bred";
    public static final String ENTER_INTERIOR = "enter_interior";
    public static final String DECOR = "decor_placed";
    public static final String SUBSTRATE = "substrate";
    /** M9: fresh water poured into a tank with used water (value = quality afterwards in %). */
    public static final String WATER_CHANGE = "water_change";
    /** M9: live food taken out of a larva tub or an artemia hatchery. */
    public static final String FOOD_BRED = "food_bred";
    /** M11: a male betta flared at the player's mirror stick. */
    public static final String MIRROR = "mirror_flare";

    private Progress() {}

    public static void award(@Nullable Player player, String event, int value) {
        if (player instanceof ServerPlayer sp && !(sp.level().isClientSide)) {
            try {
                ModTriggers.EVENT.get().trigger(sp, event, value);
            } catch (RuntimeException e) {
                // advancements must never break game logic (e.g. mock players without advancement data)
                org.slf4j.LoggerFactory.getLogger("bettacraft/progress").debug("advancement event {} failed: {}", event, e.toString());
            }
        }
    }

    public static void award(@Nullable Player player, String event) {
        award(player, event, 0);
    }

    /** Owner (if online) plus players near the tank. */
    public static Set<ServerPlayer> nearTank(ServerLevel level, TankData tank) {
        Set<ServerPlayer> out = new LinkedHashSet<>();
        UUID owner = tank.owner();
        if (owner != null) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(owner);
            if (p != null) out.add(p);
        }
        Vec3 c = tank.min() == null ? null : new Vec3((tank.min().getX() + tank.max().getX() + 1) / 2.0,
                (tank.min().getY() + tank.max().getY() + 1) / 2.0, (tank.min().getZ() + tank.max().getZ() + 1) / 2.0);
        if (c != null) {
            for (ServerPlayer p : level.players()) if (p.distanceToSqr(c) <= NEAR * NEAR) out.add(p);
        }
        return out;
    }

    public static void awardNear(ServerLevel level, TankBlockEntity controller, String event, int value) {
        for (ServerPlayer p : nearTank(level, controller.getTank())) award(p, event, value);
    }

    /** Maps tank log entries (TankCare.log) to events. */
    public static void onTankLog(ServerLevel level, TankBlockEntity controller, String key) {
        String event = switch (key) {
            case "log.bettacraft.nest_built" -> NEST_BUILT;
            case "log.bettacraft.spawned" -> SPAWNED;
            case "log.bettacraft.fry_free" -> FRY_FREE;
            case "log.bettacraft.fight" -> FIGHT;
            case "log.bettacraft.recovered" -> RECOVERED;
            default -> null;
        };
        if (event != null) awardNear(level, controller, event, 0);
    }

    /** A betta in a tank became adult: generation and (for bred fish) a named variety. */
    public static void onGrownUp(ServerLevel level, TankBlockEntity controller, BettaEntity fish) {
        int generation = fish.getBettaData().generation();
        awardNear(level, controller, GROWN_UP, generation);
        if (generation > 0) {
            var v = BettaTypes.variety(fish.getBettaData().genome());
            if (v != null && v.includeForm()) awardNear(level, controller, VARIETY, generation);
        }
    }

    /** A player caught a betta with a bucket or a fish bag. */
    public static void onBettaCaught(Player player) {
        award(player, CATCH_BETTA);
        if (player instanceof ServerPlayer sp) GuideBook.handle(sp, GuideBook.Trigger.CATCH);
    }

    /** A player opened the tank menu: checks the conditions that are states rather than events. */
    public static void onMenuOpened(ServerPlayer player, TankBlockEntity controller) {
        TankData t = controller.getTank();
        award(player, TANK_BUILT, t.memberCount());
        if (!org.overengineer.bettacraft.tank.TankCare.bettas(player.serverLevel(), t).isEmpty()
                && t.waterTemp() >= TankTemperature.OPTIMAL_MIN && t.waterTemp() <= TankTemperature.OPTIMAL_MAX) {
            award(player, IDEAL_TEMPERATURE);
        }
        if (!TankEquipment.filters(player.level(), controller).isEmpty()) award(player, FILTER);
    }
}
