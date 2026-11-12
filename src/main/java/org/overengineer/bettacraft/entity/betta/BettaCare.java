package org.overengineer.bettacraft.entity.betta;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.equipment.FeederBlockEntity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankTemperature;

/**
 * Needs of a betta living in a Bettacraft tank (PLAN 5.5): the entity runs its own timers (and its own catch-up
 * after loading) from game time differences. Outside a tank nothing happens (wild bettas have no needs).
 */
public final class BettaCare {
    private static final int INTERVAL = 20;
    private static final double TICKS_PER_DAY = 24000.0;

    private BettaCare() {}

    public static BettaHealth.Params params() {
        return new BettaHealth.Params(
                ServerConfig.getOr(ServerConfig.HEALTH_DAYS_UNTIL_SICK, 7.0),
                ServerConfig.getOr(ServerConfig.HEALTH_DAYS_SICK_UNTIL_DEATH, 14.0),
                ServerConfig.getOr(ServerConfig.HEALTH_BOTH_BAD_MULTIPLIER, 2.0),
                ServerConfig.getOr(ServerConfig.HEALTH_RECOVERY_PER_DAY, 2.0),
                1.0);
    }

    static void serverTick(ServerLevel level, BettaEntity fish) {
        if (fish.simStarted && (fish.tickCount + fish.getId()) % INTERVAL != 0) return;
        long now = level.getGameTime();
        TankBlockEntity controller = TankInhabitants.resolveController(level, fish);
        if (controller == null) {
            // wild or released: no needs, time stands still for the timers
            fish.tankWater = null;
            fish.lastSimTime = now;
            fish.simStarted = true;
            return;
        }
        TankData tank = controller.getTank();
        fish.tankWater = TankCare.waterBox(tank);
        if (!fish.simStarted) {
            fish.simStarted = true;
            if (fish.lastSimTime < 0 || !ServerConfig.getOr(ServerConfig.SIMULATE_UNLOADED_TIME, false)) {
                fish.lastSimTime = now; // default: time stood still while the chunk was unloaded
                return;
            }
        }
        if (fish.lastSimTime < 0) fish.lastSimTime = now;
        double days = (now - fish.lastSimTime) / TICKS_PER_DAY;
        fish.lastSimTime = now;
        if (days <= 0) return;

        BettaHealth.Params p = params();
        BettaHealth.Conditions c = new BettaHealth.Conditions(!TankTemperature.isTolerable(tank.waterTemp()), controller.isCrowded());
        boolean wasSick = fish.health.isSick();
        BettaHealth.Event event;
        if (days > 1.0) {
            event = catchUp(level, controller, fish, days, c, p);
        } else {
            event = fish.health.step(days, c, p);
        }
        switch (event) {
            case GOT_SICK -> TankCare.log(level, controller, "log.bettacraft.sick", TankCare.nameArg(fish));
            case RECOVERED -> TankCare.log(level, controller, "log.bettacraft.recovered", TankCare.nameArg(fish));
            case DIED -> {
                die(level, controller, fish, c, p);
                return;
            }
            default -> {}
        }
        if (wasSick != fish.health.isSick()) fish.syncLook();
    }

    /** Catch-up after unloaded time (only with simulateUnloadedTime): day by day, the feeder feeds virtually. */
    private static BettaHealth.Event catchUp(ServerLevel level, TankBlockEntity controller, BettaEntity fish, double days,
                                             BettaHealth.Conditions c, BettaHealth.Params p) {
        FeederBlockEntity feeder = TankCare.findFeeder(level, controller.getTank());
        BettaHealth.Event result = BettaHealth.Event.NONE;
        double left = days;
        while (left > 0) {
            double d = Math.min(1.0, left);
            left -= d;
            if (feeder != null && feeder.consumeVirtualPortion(0.25 * feeder.feedingsPerDay())) {
                fish.health.feed();
            }
            BettaHealth.Event e = fish.health.step(d, c, p);
            if (e == BettaHealth.Event.DIED) return e;
            if (e != BettaHealth.Event.NONE) result = e;
        }
        return result;
    }

    private static void die(ServerLevel level, TankBlockEntity controller, BettaEntity fish, BettaHealth.Conditions c, BettaHealth.Params p) {
        List<Component> causes = new ArrayList<>();
        if (fish.health.isHungry(p)) causes.add(Component.translatable("cause.bettacraft.hunger"));
        if (c.tempBad()) causes.add(Component.translatable("cause.bettacraft.temperature"));
        if (c.crowdBad()) causes.add(Component.translatable("cause.bettacraft.overcrowding"));
        if (causes.isEmpty()) causes.add(Component.translatable("cause.bettacraft.illness"));
        Component cause = causes.getFirst();
        for (int i = 1; i < causes.size(); i++) cause = Component.translatable("cause.bettacraft.and", cause, causes.get(i));
        Component msg = Component.translatable("message.bettacraft.betta.died", fish.getName(), cause);
        AABB area = fish.getBoundingBox().inflate(32);
        for (ServerPlayer sp : level.players()) {
            if (area.contains(sp.position())) sp.sendSystemMessage(msg);
        }
        String causeKey = c.tempBad() && fish.health.isHungry(p) ? "both" : fish.health.isHungry(p) ? "hunger"
                : c.tempBad() ? "temperature" : c.crowdBad() ? "overcrowding" : "illness";
        TankCare.log(level, controller, "log.bettacraft.died", TankCare.nameArg(fish), "@cause.bettacraft." + causeKey);
        controller.getTank().inhabitants().remove(fish.getUUID());
        controller.markTankChanged();
        fish.discard(); // the fish simply vanishes, no drops (PLAN 5.5)
    }
}
