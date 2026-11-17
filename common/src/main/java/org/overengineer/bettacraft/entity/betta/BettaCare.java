package org.overengineer.bettacraft.entity.betta;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.equipment.FeederBlockEntity;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankTemperature;

/**
 * Needs of a betta living in a Bettacraft tank (PLAN 5.3, 5.5, 5.6): the entity runs its own timers (and its own
 * catch-up after loading) from game time differences: health, growth through the life stages, breeding state, healing,
 * flaring. Outside a tank nothing happens (wild bettas have no needs).
 */
public final class BettaCare {
    private static final int INTERVAL = 20;
    private static final double TICKS_PER_DAY = 24000.0;
    /** Males flare at other adult males within this distance (also through tank glass). */
    public static final double FLARE_RANGE = 6.0;

    private BettaCare() {}

    public static BettaHealth.Params params() {
        return new BettaHealth.Params(
                ServerConfig.getOr(ServerConfig.HEALTH_DAYS_UNTIL_SICK, 7.0),
                ServerConfig.getOr(ServerConfig.HEALTH_DAYS_SICK_UNTIL_DEATH, 14.0),
                ServerConfig.getOr(ServerConfig.HEALTH_BOTH_BAD_MULTIPLIER, 2.0),
                ServerConfig.getOr(ServerConfig.HEALTH_RECOVERY_PER_DAY, 2.0),
                ServerConfig.getOr(ServerConfig.HEALTH_HUNGRY_AFTER_DAYS, 1.0), // M17: configurable
                org.overengineer.bettacraft.food.FoodDiet.params(), // M26 part b: a varied diet
                org.overengineer.bettacraft.config.Care.mode()); // M33 part c: care.mode
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
        fish.tankWater = new org.overengineer.bettacraft.tank.TankWaterVolume(tank);
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
        if (fish.isAdult() && !fish.isFemale()) updateFlaring(level, fish, now);
        if (days <= 0) return;
        advance(level, controller, fish, days);
    }

    /**
     * Advances the needs of a fish in a tank by {@code days} (also used by GameTests). Health timers, growth (only fed
     * and at a tolerable temperature), breeding timers and healing of wounds.
     */
    public static void advance(ServerLevel level, TankBlockEntity controller, BettaEntity fish, double days) {
        // M33 part b: bettas off: their care rests (no hunger, illness, growth or breeding timers; nothing dies)
        if (!org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.BETTAS)) return;
        TankData tank = controller.getTank();
        BettaHealth.Params p = params();
        boolean tempOk = TankTemperature.isTolerable(tank.waterTemp());
        double salt = org.overengineer.bettacraft.reef.SaltTolerance.shock(org.overengineer.bettacraft.reef.SaltTolerance.BETTA, tank);
        BettaHealth.Conditions c = new BettaHealth.Conditions(!tempOk, controller.isCrowded() && fish.isAdult(),
                org.overengineer.bettacraft.tank.TankWater.isBad(tank) || salt >= org.overengineer.bettacraft.tank.Salinity.STRESS_FROM
                        || org.overengineer.bettacraft.tank.TankAeration.lowFor(tank, true) // M23: labyrinth fish hardly mind low oxygen
                        || org.overengineer.bettacraft.chem.TankChemistry.bad(tank, org.overengineer.bettacraft.chem.ChemTolerance.BETTA), salt); // M25: pH / TDS far out of the betta's range
        boolean wasSick = fish.health.isSick();
        OldAge.start(fish.health, fish.liveStage, fish.stageDays, LifeStage::days); // M33 part d
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
                dieOfIllness(level, controller, fish, c, p);
                return;
            }
            default -> {}
        }
        if (OldAge.due(fish.health)) { // M33 part d
            dieOfOldAge(level, controller, fish);
            return;
        }
        if (wasSick != fish.health.isSick()) fish.syncLook();
        fish.syncDiet(); // M26 part b
        if (org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.BREEDING)) fish.breeding.tick(days); // M33 part b

        boolean fed = !fish.health.isHungry(p);
        if (fish.liveStage != LifeStage.ADULT && fed && tempOk && !fish.health.isSick()) grow(level, controller, fish, days);
        double heal = ServerConfig.getOr(ServerConfig.HEALTH_HEAL_PER_DAY, 2.0);
        if (org.overengineer.bettacraft.reef.ReefCare.cleaners(controller.getTank())) heal *= 1.5; // M20: a cleaner shrimp picks the wounds clean
        if (fed && !fish.health.isSick() && fish.getHealth() < fish.getMaxHealth() && heal > 0) fish.heal((float) (heal * days));
    }

    private static void grow(ServerLevel level, TankBlockEntity controller, BettaEntity fish, double days) {
        fish.stageDays += days;
        double need = fish.liveStage.days();
        while (fish.liveStage != LifeStage.ADULT && fish.stageDays >= need) {
            double rest = fish.stageDays - need;
            fish.setStage(fish.liveStage.next());
            fish.stageDays = rest;
            need = fish.liveStage.days();
            if (fish.liveStage == LifeStage.ADULT) {
                TankCare.log(level, controller, "log.bettacraft.grown_up", TankCare.nameArg(fish));
                org.overengineer.bettacraft.progression.Progress.onGrownUp(level, controller, fish);
                controller.markTankChanged();
            }
        }
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

    // ------------------------------------------------------------------ flaring

    private static void updateFlaring(ServerLevel level, BettaEntity fish, long now) {
        if (fish.tankWater == null) return;
        AABB area = fish.getBoundingBox().inflate(FLARE_RANGE);
        for (BettaEntity other : level.getEntitiesOfClass(BettaEntity.class, area, o -> o != fish && o.isAlive() && o.isAdult() && !o.isFemale())) {
            if (canSee(level, fish.getEyePosition(), other.getEyePosition())) {
                fish.flare(now + 60);
                return;
            }
        }
    }

    /** Line of sight where glass and water do not block (only blocks that occlude the view do). */
    public static boolean canSee(Level level, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        int steps = Math.max(1, (int) Math.ceil(len / 0.25));
        BlockPos last = null;
        for (int i = 1; i < steps; i++) {
            BlockPos p = BlockPos.containing(from.add(d.scale(i / (double) steps)));
            if (p.equals(last)) continue;
            last = p;
            if (!level.isLoaded(p)) return false;
            if (level.getBlockState(p).canOcclude()) return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ death

    private static void dieOfIllness(ServerLevel level, TankBlockEntity controller, BettaEntity fish, BettaHealth.Conditions c, BettaHealth.Params p) {
        List<Component> causes = new ArrayList<>();
        if (c.saltBad()) causes.add(Component.translatable("cause.bettacraft.salinity")); // M20
        if (fish.health.isHungry(p)) causes.add(Component.translatable("cause.bettacraft.hunger"));
        if (c.tempBad()) causes.add(Component.translatable("cause.bettacraft.temperature"));
        if (c.crowdBad()) causes.add(Component.translatable("cause.bettacraft.overcrowding"));
        boolean chem = org.overengineer.bettacraft.chem.TankChemistry.bad(controller.getTank(), org.overengineer.bettacraft.chem.ChemTolerance.BETTA); // M25
        boolean quality = c.waterBad() && (!chem || org.overengineer.bettacraft.tank.TankWater.isBad(controller.getTank()));
        boolean co2 = chem && org.overengineer.bettacraft.chem.TankChemistry.co2Bad(controller.getTank(), org.overengineer.bettacraft.chem.ChemTolerance.BETTA); // M25 part b
        if (chem) causes.add(Component.translatable(co2 ? "cause.bettacraft.co2" : "cause.bettacraft.water_chemistry"));
        if (quality) causes.add(Component.translatable("cause.bettacraft.water_quality"));
        if (causes.isEmpty()) causes.add(Component.translatable("cause.bettacraft.illness"));
        Component cause = causes.getFirst();
        for (int i = 1; i < causes.size(); i++) cause = Component.translatable("cause.bettacraft.and", cause, causes.get(i));
        announce(level, fish, cause);
        String causeKey = c.saltBad() ? "salinity" : c.tempBad() && fish.health.isHungry(p) ? "both" : fish.health.isHungry(p) ? "hunger"
                : c.tempBad() ? "temperature" : c.crowdBad() ? "overcrowding" : chem && !quality ? (co2 ? "co2" : "water_chemistry") : c.waterBad() ? "water_quality" : "illness";
        TankCare.log(level, controller, "log.bettacraft.died", TankCare.nameArg(fish), "@cause.bettacraft." + causeKey);
        org.overengineer.bettacraft.api.ApiEvents.died(level, controller, fish, causeKey, null); // M32 part d
        controller.getTank().inhabitants().remove(fish.getUUID());
        controller.markTankChanged();
        fish.discard(); // the fish simply vanishes, no drops (PLAN 5.5)
    }

    /** M33 part d: old age ({@link OldAge}): message, tank log and the KubeJS event with the cause {@code old_age}. */
    static void dieOfOldAge(ServerLevel level, TankBlockEntity controller, BettaEntity fish) {
        announce(level, fish, Component.translatable("cause.bettacraft." + OldAge.CAUSE));
        TankCare.log(level, controller, "log.bettacraft.died", TankCare.nameArg(fish), "@cause.bettacraft." + OldAge.CAUSE);
        org.overengineer.bettacraft.api.ApiEvents.died(level, controller, fish, OldAge.CAUSE, null);
        controller.getTank().inhabitants().remove(fish.getUUID());
        controller.markTankChanged();
        fish.discard();
    }

    /** Death by damage (fight, chasing male, predator, ...): message and tank log. */
    static void onDeath(ServerLevel level, BettaEntity fish, DamageSource source) {
        TankBlockEntity controller = TankInhabitants.resolveController(level, fish);
        if (controller == null) return;
        Entity attacker = source.getEntity();
        String causeKey;
        Component cause;
        if (attacker instanceof BettaEntity b && fish.isFemale() && !b.isFemale()) {
            causeKey = "chased";
            cause = Component.translatable("cause.bettacraft.chased", b.getName());
        } else if (attacker instanceof BettaEntity b) {
            causeKey = "fight";
            cause = Component.translatable("cause.bettacraft.fight", b.getName());
        } else if (attacker != null && attacker.getType().is(ModTags.PREDATORS)) {
            causeKey = "predator";
            cause = Component.translatable("cause.bettacraft.predator", attacker.getName());
        } else {
            causeKey = "injuries";
            cause = Component.translatable("cause.bettacraft.injuries");
        }
        announce(level, fish, cause);
        if (attacker != null && !"injuries".equals(causeKey)) {
            TankCare.log(level, controller, "log.bettacraft.killed_" + causeKey, TankCare.nameArg(fish), TankCare.nameArg(attacker));
        } else {
            TankCare.log(level, controller, "log.bettacraft.died", TankCare.nameArg(fish), "@cause.bettacraft." + causeKey);
        }
        controller.getTank().inhabitants().remove(fish.getUUID());
        controller.markTankChanged();
    }

    private static void announce(ServerLevel level, Entity fish, Component cause) {
        Component msg = Component.translatable("message.bettacraft.betta.died", fish.getName(), cause);
        AABB area = fish.getBoundingBox().inflate(32);
        for (ServerPlayer sp : level.players()) {
            if (area.contains(sp.position())) sp.sendSystemMessage(msg);
        }
    }
}
