package org.overengineer.bettacraft.tank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaCare;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.BettaHealth;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.genetics.BettaLoci;
import org.overengineer.bettacraft.genetics.Locus;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * Tank-level behaviour of M2, run by the controller (PLAN 5.4, 5.6, 4.3):
 * <ol>
 *     <li><b>Breeding</b>: a healthy, fed adult pair at 26-28 C gets ready (live food doubles the speed), the male builds a
 *     bubble nest at the surface, the female spawns 8-20 eggs into it (data, not entities). Eggs hatch into larvae and
 *     become fry entities after the egg + larva days; the survival rate (60-90 %) follows the care during that time.</li>
 *     <li><b>Nest defence</b>: while there are eggs/larvae the male chases every adult female, who loses health until she is
 *     taken out.</li>
 *     <li><b>Fry eating</b>: adult males eat free-swimming fry with a chance per day.</li>
 *     <li><b>Fights</b>: two adult males fight until one dies (one in-game day by default).</li>
 *     <li><b>Predators</b> ({@code #bettacraft:predators}, e.g. the pufferfish) attack small inhabitants and eat fry.</li>
 *     <li><b>Live prey</b> ({@code #bettacraft:betta_prey}, e.g. Fintastic artemia and daphnia, M7): hungry bettas eat it
 *     like live food.</li>
 * </ol>
 */
public final class TankBreeding {
    /** Duration of the spawning embrace at the nest. */
    static final double SPAWN_DAYS = 0.02;
    /** A finished nest without a female collapses after this many days. */
    static final double NEST_DECAY_DAYS = 3.0;
    /** Prey of predators: living inhabitants up to this hitbox width. */
    static final float PREY_MAX_WIDTH = 0.6F;
    /** Behaviour refresh: goals stay active this many ticks after the last step. */
    private static final int HOLD = 60;

    private TankBreeding() {}

    // ------------------------------------------------------------------ helpers

    public static double minTemp() {
        return ServerConfig.getOr(ServerConfig.BREEDING_MIN_TEMP, 26.0);
    }

    public static double maxTemp() {
        return ServerConfig.getOr(ServerConfig.BREEDING_MAX_TEMP, 28.0);
    }

    public static boolean breedingTemp(TankData tank) {
        return tank.waterTemp() >= minTemp() - 1e-4 && tank.waterTemp() <= maxTemp() + 1e-4;
    }

    /** Healthy, fed and at least at half health. */
    public static boolean fit(BettaEntity b) {
        BettaHealth.Params p = BettaCare.params();
        return b.isAlive() && b.isAdult() && !b.health().isSick() && !b.health().isHungry(p) && b.getHealth() >= b.getMaxHealth() * 0.5F;
    }

    private static List<BettaEntity> bettas(List<Entity> live) {
        List<BettaEntity> out = new ArrayList<>();
        for (Entity e : live) if (e instanceof BettaEntity b && b.isAlive()) out.add(b);
        out.sort(Comparator.comparing(Entity::getUUID));
        return out;
    }

    @Nullable
    private static BettaEntity byUuid(List<BettaEntity> list, @Nullable UUID id) {
        if (id == null) return null;
        for (BettaEntity b : list) if (b.getUUID().equals(id)) return b;
        return null;
    }

    private static <T extends Entity> T nearest(Entity from, List<T> list) {
        T best = null;
        double bd = Double.MAX_VALUE;
        for (T t : list) {
            if (t == from || !t.isAlive()) continue;
            double d = t.distanceToSqr(from);
            if (d < bd) {
                bd = d;
                best = t;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ simulation

    /** One simulation step of {@code days} (also used by GameTests). */
    public static void step(ServerLevel level, TankBlockEntity controller, double days) {
        step(level, controller, days, level.getRandom());
    }

    /** M32 part b: a step with its own random source (GameTests: deterministic fights and nests, not the shared level random). */
    public static void step(ServerLevel level, TankBlockEntity controller, double days, RandomSource r) {
        if (days <= 0) return;
        if (!org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.BETTAS)) return; // M33 part b: bettas rest (no nests, fights, hunting)
        TankData tank = controller.getTank();
        long now = level.getGameTime();
        List<Entity> live = TankInhabitants.liveInhabitants(level, tank);
        List<BettaEntity> bettas = bettas(live);
        List<BettaEntity> males = new ArrayList<>(), females = new ArrayList<>(), fry = new ArrayList<>();
        for (BettaEntity b : bettas) {
            if (b.isAdult()) (b.isFemale() ? females : males).add(b);
            else if (b.lifeStage() == LifeStage.FRY) fry.add(b);
        }
        if (org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.BREEDING)) nest(level, controller, tank, live.size(), males, females, days, now, r); // M33 part b
        fights(level, controller, males, days, now, r);
        maleEatsFry(level, controller, males, fry, days, r);
        predators(level, controller, live, days, r);
        eatPrey(level, controller, live, bettas);
    }

    private static void nest(ServerLevel level, TankBlockEntity controller, TankData tank, int liveCount, List<BettaEntity> males,
                             List<BettaEntity> females, double days, long now, RandomSource r) {
        BubbleNest n = tank.nest();
        boolean tempOk = breedingTemp(tank);
        if (n == null) {
            BettaEntity male = males.stream().filter(TankBreeding::fit).findFirst().orElse(null);
            BettaEntity female = females.stream().filter(f -> fit(f) && f.breeding().cooldown() <= 0).findFirst().orElse(null);
            if (male == null || female == null || !tempOk) {
                for (BettaEntity m : males) m.breeding().addReady(-days);
                return;
            }
            female.showBreedingMood(now + HOLD);
            boolean live = male.breeding().isConditioned() || female.breeding().isConditioned();
            // M11: mirror training of the male (breeding.mirrorReadinessBonus, 1 = off)
            double mirror = male.breeding().isTrained() ? ServerConfig.getOr(ServerConfig.BREEDING_MIRROR_BONUS, 1.25) : 1.0;
            // M12: floating plants on the surface (breeding.floatingPlants.breedingBonus, 1 = off)
            double plants = org.overengineer.bettacraft.tank.plants.TankPlants.breedingBonus(tank)
                    * org.overengineer.bettacraft.plants.InteriorPlants.breedingBonus(level.getServer(), tank); // M13: aquarium plants
            male.breeding().addReady(days * (live ? 2.0 : 1.0) * mirror * plants * TankNitrate.breedingFactor(tank)); // M24: much nitrate
            if (male.breeding().ready() >= ServerConfig.getOr(ServerConfig.BREEDING_READY_DAYS, 0.25)) {
                male.breeding().resetReady();
                BubbleNest nest = new BubbleNest();
                nest.male = male.getUUID();
                nest.fatherFishId = male.getBettaData().fishId();
                nest.fatherName = TankCare.nameArg(male);
                nest.fatherGenotype = male.genotype();
                nest.pos = nestSpot(level, tank, male.position());
                nest.phase = BubbleNest.Phase.BUILDING;
                tank.setNest(nest);
                TankCare.log(level, controller, "log.bettacraft.nest_started", TankCare.nameArg(male));
                controller.markTankChanged();
            }
            return;
        }
        BettaEntity male = null;
        for (BettaEntity m : males) if (m.getUUID().equals(n.male)) male = m;
        switch (n.phase) {
            case BUILDING -> {
                if (male == null) {
                    abandon(level, controller, tank, "log.bettacraft.nest_abandoned");
                    return;
                }
                male.keepAtNest(n.pos, now + HOLD);
                if (fit(male) && tempOk) {
                    n.progress += days;
                    if (r.nextInt(3) == 0) level.sendParticles(ParticleTypes.BUBBLE, n.pos.x, n.pos.y - 0.1, n.pos.z, 3, 0.15, 0.05, 0.15, 0.01);
                }
                if (n.progress >= ServerConfig.getOr(ServerConfig.BREEDING_NEST_BUILD_DAYS, 0.25)) {
                    n.phase = BubbleNest.Phase.BUILT;
                    n.progress = 0;
                    TankCare.log(level, controller, "log.bettacraft.nest_built", TankCare.nameArg(male));
                }
                syncNest(controller);
            }
            case BUILT -> {
                if (male == null) {
                    abandon(level, controller, tank, "log.bettacraft.nest_abandoned");
                    return;
                }
                male.keepAtNest(n.pos, now + HOLD);
                BettaEntity female = females.stream().filter(f -> fit(f) && f.breeding().cooldown() <= 0)
                        .min(Comparator.comparingDouble(f -> f.distanceToSqr(n.pos))).orElse(null);
                if (female != null && tempOk) {
                    female.showBreedingMood(now + HOLD);
                    female.keepAtNest(n.pos, now + HOLD);
                    male.flare(now + HOLD);
                    n.waiting = 0;
                    n.progress += days;
                    if (n.progress >= SPAWN_DAYS) spawn(level, controller, n, male, female, r);
                } else {
                    n.waiting += days;
                    if (n.waiting >= NEST_DECAY_DAYS) abandon(level, controller, tank, "log.bettacraft.nest_collapsed");
                }
            }
            case EGGS -> {
                if (TankTemperature.isTolerable(tank.waterTemp())) n.progress += days;
                double tempScore = tempOk ? 1.0 : TankTemperature.isTolerable(tank.waterTemp()) ? 0.5 : 0.0;
                double score = 0.5 * tempScore + 0.25 * (controller.isCrowded() ? 0 : 1)
                        + 0.25 * (male != null && !male.health().isHungry(BettaCare.params()) ? 1 : 0);
                n.careSum += score * days;
                n.careTime += days;
                if (male != null) {
                    male.keepAtNest(n.pos, now + HOLD);
                    // M13: dense aquarium plants are hiding spots: a hidden female is not chased in this step
                    double hide = org.overengineer.bettacraft.plants.InteriorPlants.hiding(level.getServer(), tank);
                    List<BettaEntity> seen = new ArrayList<>();
                    for (BettaEntity f : females) {
                        if (hide > 0 && r.nextDouble() < hide) controller.femaleHidSteps++;
                        else seen.add(f);
                    }
                    BettaEntity target = nearest(male, seen);
                    if (target != null) {
                        male.pursue(target, now + HOLD);
                        male.flare(now + HOLD);
                        for (BettaEntity f : seen) {
                            f.flee(male, now + HOLD);
                            f.addDamage(ServerConfig.getOr(ServerConfig.BREEDING_FEMALE_DAMAGE_PER_DAY, 2.0) * days, male);
                        }
                        if (!controller.chaseLogged) {
                            controller.chaseLogged = true;
                            TankCare.log(level, controller, "log.bettacraft.female_chased", TankCare.nameArg(male), TankCare.nameArg(target));
                        }
                    } else {
                        controller.chaseLogged = false;
                    }
                }
                if (!n.hatchedLogged && n.progress >= LifeStage.EGG.days()) {
                    n.hatchedLogged = true;
                    TankCare.log(level, controller, "log.bettacraft.larvae", String.valueOf(n.eggs.size()));
                }
                if (n.progress >= LifeStage.EGG.days() + LifeStage.LARVA.days()) release(level, controller, n, liveCount, r);
                syncNest(controller);
            }
        }
    }

    /**
     * M17: the tank view (with plants, small animals and algae) went to every player near the tank on each care step
     * while a nest was built or held eggs, for days. Now only when what the clients show of the nest changes: phase,
     * position, egg count, stage, and the size of a growing nest in 32 steps.
     */
    static void syncNest(TankBlockEntity controller) {
        long sig = nestSignature(controller.getTank().nest());
        if (sig != controller.nestSignature) {
            controller.nestSignature = sig;
            controller.markTankChanged();
        } else {
            controller.setChanged();
        }
    }

    static long nestSignature(@Nullable BubbleNest n) {
        if (n == null) return 0;
        long h = 1 + n.phase().ordinal();
        h = h * 31 + n.eggCount();
        h = h * 31 + (n.phase() == BubbleNest.Phase.EGGS ? n.stage().ordinal() : 0);
        h = h * 31 + Double.hashCode(n.pos().x) * 7L + Double.hashCode(n.pos().y) * 13L + Double.hashCode(n.pos().z);
        if (n.phase() == BubbleNest.Phase.BUILDING) {
            double days = Math.max(1e-6, ServerConfig.getOr(ServerConfig.BREEDING_NEST_BUILD_DAYS, 0.25));
            h = h * 31 + (long) Math.floor(Math.min(1.0, n.progress() / days) * 32);
        }
        return h;
    }

    /**
     * Nest position (PLAN 5.4): at the water surface, preferably under the densest cluster of floating plants on the
     * surface (M12), else under the floating plant of the interior (e.g. a lily pad) that is closest to the male,
     * otherwise where the male is (slightly inside the top water block).
     */
    public static Vec3 nestSpot(ServerLevel level, TankData tank, Vec3 near) {
        // M12: under the densest cluster of floating plants on the surface (frogbit is the favourite)
        Vec3 plant = org.overengineer.bettacraft.tank.plants.TankPlants.nestCluster(tank, near);
        if (plant == null) plant = floatingPlantNear(level, tank, near);
        return nestSpot(tank, plant != null ? plant : near);
    }

    /** Tank-world position (x/z) of the floating plant in the interior closest to {@code near}, or null. */
    @javax.annotation.Nullable
    public static Vec3 floatingPlantNear(ServerLevel level, TankData tank, Vec3 near) {
        var r = org.overengineer.bettacraft.tank.interior.InteriorRegions.get(level.getServer()).get(tank.regionId());
        if (r == null || !r.built()) return null;
        var eq = org.overengineer.bettacraft.tank.interior.InteriorManager.equipment(level.getServer(), r);
        Vec3 best = null;
        double bestD = Double.MAX_VALUE;
        double s = r.scale();
        for (long l : eq.floatingPlants()) {
            Vec3 p = new Vec3(tank.min().getX() + (BlockPos.getX(l) + 0.5) / s, near.y, tank.min().getZ() + (BlockPos.getZ(l) + 0.5) / s);
            double d = p.distanceToSqr(near);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    /** At the surface near {@code near}, in a block that holds the surface (M10: never above a notch or a closed step). */
    static Vec3 nestSpot(TankData tank, Vec3 near) {
        return new TankWaterVolume(tank).surfacePoint(near, 0.3 + 1.0 / 16, 0.06);
    }

    private static void abandon(ServerLevel level, TankBlockEntity controller, TankData tank, String key) {
        if (org.overengineer.bettacraft.debug.LogArea.BREEDING.debug) org.overengineer.bettacraft.debug.LogArea.BREEDING.debug("tank {}: nest given up: {}", tank.id(), key); // M57
        tank.setNest(null);
        controller.chaseLogged = false;
        TankCare.log(level, controller, key);
        controller.markTankChanged();
    }

    private static void spawn(ServerLevel level, TankBlockEntity controller, BubbleNest n, BettaEntity male, BettaEntity female, RandomSource r) {
        int min = ServerConfig.getOr(ServerConfig.BREEDING_MIN_EGGS, 8);
        int max = Math.max(min, ServerConfig.getOr(ServerConfig.BREEDING_MAX_EGGS, 20));
        int count = min + r.nextInt(max - min + 1);
        if (female.breeding().isConditioned()) count = Math.min(max, count + 2);
        double mutation = ServerConfig.getOr(ServerConfig.BREEDING_MUTATION_CHANCE, 0.01);
        BettaGenotype mg = female.genotype(), fg = male.genotype();
        n.eggs.clear();
        for (int i = 0; i < count; i++) n.eggs.add(new BubbleNest.Egg(BettaGenotype.cross(mg, fg, r, mutation), r.nextBoolean()));
        BettaData md = female.getBettaData(), fd = male.getBettaData();
        n.phase = BubbleNest.Phase.EGGS;
        n.progress = 0;
        n.female = female.getUUID();
        n.motherFishId = md.fishId();
        n.motherName = TankCare.nameArg(female);
        n.motherGenotype = mg;
        n.fatherGenotype = fg;
        n.fatherFishId = fd.fishId();
        n.fatherName = TankCare.nameArg(male);
        n.generation = Math.max(md.generation(), fd.generation()) + 1;
        n.careSum = n.careTime = 0;
        female.breeding().setCooldown(ServerConfig.getOr(ServerConfig.BREEDING_FEMALE_COOLDOWN_DAYS, 3.0));
        controller.chaseLogged = false;
        level.sendParticles(ParticleTypes.BUBBLE_POP, n.pos.x, n.pos.y, n.pos.z, 12, 0.2, 0.05, 0.2, 0.01);
        TankCare.log(level, controller, "log.bettacraft.spawned", TankCare.nameArg(female), TankCare.nameArg(male), String.valueOf(count));
        if (org.overengineer.bettacraft.debug.LogArea.BREEDING.debug) org.overengineer.bettacraft.debug.LogArea.BREEDING.debug("tank {}: nest spawned, {} eggs (conditioned {}, mutation chance {}), mother {} {}, father {} {}, generation {}",
                controller.getTank().id(), count, female.breeding().isConditioned(), mutation, female.getUUID(), mg, male.getUUID(), fg, n.generation); // M57
    }

    /** Larvae become free-swimming fry entities; how many survive depends on the care. */
    private static void release(ServerLevel level, TankBlockEntity controller, BubbleNest n, int liveCount, RandomSource r) {
        TankData tank = controller.getTank();
        double lo = ServerConfig.getOr(ServerConfig.BREEDING_MIN_SURVIVAL, 0.6), hi = ServerConfig.getOr(ServerConfig.BREEDING_MAX_SURVIVAL, 0.9);
        double survival = lo + (Math.max(lo, hi) - lo) * Mth.clamp(n.careScore(), 0, 1);
        int survivors = (int) Math.round(n.eggs.size() * survival);
        int room = Math.max(0, ServerConfig.getOr(ServerConfig.TANK_MAX_ENTITIES, 64) - liveCount);
        survivors = Math.min(survivors, room);
        List<BubbleNest.Egg> eggs = new ArrayList<>(n.eggs);
        Collections.shuffle(eggs, new java.util.Random(r.nextLong()));
        TankWaterVolume water = new TankWaterVolume(tank);
        int spawned = 0;
        for (int i = 0; i < survivors; i++) {
            BubbleNest.Egg egg = eggs.get(i);
            BettaEntity fry = ModEntities.BETTA.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
            if (fry == null) continue;
            fry.setBettaData(BettaData.offspring(egg.genotype(), egg.female(), n.motherFishId, n.fatherFishId, n.generation, level.getGameTime()));
            Vec3 p = water.clamp(new Vec3(n.pos.x + (r.nextDouble() - 0.5) * 0.6, n.pos.y - 0.2 - r.nextDouble() * 0.3,
                    n.pos.z + (r.nextDouble() - 0.5) * 0.6), 0.1 + 1.0 / 16, 0.1 + 1.0 / 16, 0.05);
            fry.snapTo(p.x, p.y, p.z, r.nextFloat() * 360F, 0F);
            TankInhabitants.adopt(fry, controller);
            if (level.addFreshEntity(fry)) {
                tank.inhabitants().add(fry.getUUID());
                spawned++;
            }
        }
        tank.setNest(null);
        controller.chaseLogged = false;
        TankCare.log(level, controller, "log.bettacraft.fry_free", String.valueOf(spawned), String.valueOf(n.eggs.size() - spawned));
        if (org.overengineer.bettacraft.debug.LogArea.BREEDING.debug) org.overengineer.bettacraft.debug.LogArea.BREEDING.debug("tank {}: {} of {} larvae free (care score {}, survival {}, room {})", tank.id(), spawned, n.eggs.size(),
                n.careScore(), survival, room); // M57
        org.overengineer.bettacraft.api.ApiEvents.born(level, controller, ModEntities.BETTA.get(), spawned, true); // M32 part d
        controller.markTankChanged();
    }

    // ------------------------------------------------------------------ fights

    private static void fights(ServerLevel level, TankBlockEntity controller, List<BettaEntity> males, double days, long now, RandomSource r) {
        if (!ServerConfig.getOr(ServerConfig.COMBAT_MALES_FIGHT, true) || males.size() < 2) {
            controller.fightLogged = false;
            return;
        }
        double perDay = 6.0 / ServerConfig.getOr(ServerConfig.COMBAT_FIGHT_DAYS, 1.0);
        for (BettaEntity m : males) {
            if (!m.isAlive()) continue;
            BettaEntity rival = nearest(m, males);
            if (rival == null) continue;
            m.pursue(rival, now + HOLD);
            m.flare(now + HOLD);
            double dmg = rival.getMaxHealth() / 6.0 * perDay * days * (0.5 + r.nextDouble());
            // M29 part c: Betta imbellis males mostly just flare, smaragdina fight less hard than splendens
            dmg *= (m.genotype().ancestry().aggression() + rival.genotype().ancestry().aggression()) / 2;
            m.addDamage(dmg, rival);
        }
        if (!controller.fightLogged) {
            controller.fightLogged = true;
            TankCare.log(level, controller, "log.bettacraft.fight", TankCare.nameArg(males.get(0)), TankCare.nameArg(males.get(1)));
        }
    }

    private static void maleEatsFry(ServerLevel level, TankBlockEntity controller, List<BettaEntity> males, List<BettaEntity> fry, double days, RandomSource r) {
        if (!org.overengineer.bettacraft.config.Care.canDie()) return; // M33 part c: care.mode NO_DEATH / DECOR: nobody is eaten
        double rate = ServerConfig.getOr(ServerConfig.BREEDING_MALE_EATS_FRY_PER_DAY, 0.3);
        if (males.isEmpty() || fry.isEmpty() || rate <= 0) return;
        double p = 1 - Math.exp(-rate * days * males.size());
        int eaten = 0;
        for (BettaEntity f : fry) {
            if (r.nextDouble() < p) {
                eat(level, controller, f);
                eaten++;
            }
        }
        if (eaten > 0) TankCare.log(level, controller, "log.bettacraft.fry_eaten", String.valueOf(eaten), TankCare.nameArg(males.getFirst()));
    }

    private static void eat(ServerLevel level, TankBlockEntity controller, Entity prey) {
        level.sendParticles(ParticleTypes.BUBBLE_POP, prey.getX(), prey.getY(), prey.getZ(), 4, 0.05, 0.05, 0.05, 0.01);
        org.overengineer.bettacraft.api.ApiEvents.died(level, controller, prey, "eaten", null); // M32 part d
        controller.getTank().inhabitants().remove(prey.getUUID());
        prey.discard();
        controller.markTankChanged();
    }

    // ------------------------------------------------------------------ predators

    /** M36: the hitbox width for the game rules, without the size setting of the bettas (the rules do not depend on the look). */
    static float gameWidth(Entity e) {
        return e instanceof BettaEntity ? e.getBbWidth() / org.overengineer.bettacraft.config.AnimalSizes.bettaFactor() : e.getBbWidth();
    }

    static float gameHeight(Entity e) {
        return e instanceof BettaEntity ? e.getBbHeight() / org.overengineer.bettacraft.config.AnimalSizes.bettaFactor() : e.getBbHeight();
    }

    private static void predators(ServerLevel level, TankBlockEntity controller, List<Entity> live, double days, RandomSource r) {
        if (!org.overengineer.bettacraft.config.Care.canDie()) return; // M33 part c: care.mode NO_DEATH / DECOR: nobody is eaten
        double rate = ServerConfig.getOr(ServerConfig.COMBAT_PREDATOR_ATTACKS_PER_DAY, 4.0);
        if (rate <= 0) return;
        for (Entity pred : live) {
            if (!pred.isAlive() || !pred.getType().is(ModTags.PREDATORS) || !(pred instanceof LivingEntity predator)) continue;
            List<LivingEntity> prey = new ArrayList<>();
            for (Entity e : live) {
                if (e != pred && e.isAlive() && e instanceof LivingEntity le && !e.getType().is(ModTags.PREDATORS)
                        && gameWidth(e) <= PREY_MAX_WIDTH) prey.add(le);
            }
            if (prey.isEmpty()) continue;
            LivingEntity victim = prey.stream().min(Comparator.comparingDouble(e -> gameWidth(e) * gameHeight(e))).orElseThrow();
            if (pred instanceof Mob mob && mob.getNavigation().isDone()) mob.getNavigation().moveTo(victim, 1.2);
            if (r.nextDouble() >= 1 - Math.exp(-rate * days)) continue;
            boolean small = victim instanceof BettaEntity b ? !b.isAdult() : victim.getBbWidth() < 0.3F;
            if (small) {
                TankCare.log(level, controller, "log.bettacraft.eaten", TankCare.nameArg(victim), TankCare.nameArg(pred));
                eat(level, controller, victim);
            } else if (pred.getType() == EntityType.PUFFERFISH) {
                victim.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1), pred);
                victim.hurt(level.damageSources().mobAttack(predator), 2.0F);
            } else {
                victim.hurt(level.damageSources().mobAttack(predator), 3.0F);
            }
        }
    }

    // ------------------------------------------------------------------ live prey (M7)

    /** Hungry bettas eat live prey animals of other mods ({@code #bettacraft:betta_prey}); counts as live food. */
    private static void eatPrey(ServerLevel level, TankBlockEntity controller, List<Entity> live, List<BettaEntity> bettas) {
        if (!org.overengineer.bettacraft.config.Care.canDie()) return; // M33 part c: care.mode NO_DEATH / DECOR: nobody is eaten
        List<Entity> prey = new ArrayList<>();
        for (Entity e : live) if (e.isAlive() && e.getType().is(ModTags.BETTA_PREY)) prey.add(e);
        if (prey.isEmpty()) return;
        BettaHealth.Params p = BettaCare.params();
        for (BettaEntity b : bettas) {
            if (prey.isEmpty()) break;
            if (!b.isAlive() || !b.health().isHungry(p)
                    || !org.overengineer.bettacraft.food.BettaFood.canEat(b.lifeStage(), b.stageProgress(), org.overengineer.bettacraft.food.BettaFood.Kind.LIVE)) {
                continue;
            }
            Entity victim = nearest(b, prey);
            if (victim == null) break;
            prey.remove(victim);
            b.health().feed();
            if (b.isAdult()) b.breeding().condition();
            TankCare.log(level, controller, "log.bettacraft.ate_prey", TankCare.nameArg(b), TankCare.nameArg(victim));
            eat(level, controller, victim);
        }
    }

    // ------------------------------------------------------------------ menu / Jade

    /** Warning keys shown in the menu and in Jade. */
    public static List<String> warnings(ServerLevel level, TankBlockEntity controller) {
        TankData tank = controller.getTank();
        List<Entity> live = TankInhabitants.liveInhabitants(level, tank);
        int males = 0, females = 0, fry = 0;
        boolean predator = false;
        for (Entity e : live) {
            if (e instanceof BettaEntity b) {
                if (b.isAdult()) {
                    if (b.isFemale()) females++;
                    else males++;
                } else if (b.lifeStage() == LifeStage.FRY) fry++;
            }
            if (e.getType().is(ModTags.PREDATORS)) predator = true;
        }
        List<String> out = new ArrayList<>();
        BubbleNest n = tank.nest();
        if (n != null && n.hasEggs() && females > 0) out.add("remove_female");
        if (fry > 0 && males > 0) out.add("remove_male");
        if (males >= 2 && ServerConfig.getOr(ServerConfig.COMBAT_MALES_FIGHT, true)) out.add("fight");
        if (predator && live.stream().anyMatch(e -> !e.getType().is(ModTags.PREDATORS) && gameWidth(e) <= PREY_MAX_WIDTH)) out.add("predator");
        if (org.overengineer.bettacraft.tank.plants.TankPlants.dense(tank)) out.add("dense_plants"); // M12
        else if (org.overengineer.bettacraft.plants.InteriorPlants.overgrown(level.getServer(), tank)) out.add("overgrown"); // M13
        out.addAll(org.overengineer.bettacraft.tankmates.TankmateBreeding.warnings(level, controller)); // M19
        return out;
    }

    /** Everything the breeding tab shows (server -> client NBT). */
    public static CompoundTag describe(ServerLevel level, TankBlockEntity controller) {
        TankData tank = controller.getTank();
        CompoundTag out = new CompoundTag();
        List<BettaEntity> bettas = bettas(TankInhabitants.liveInhabitants(level, tank));
        int[] stages = new int[LifeStage.values().length];
        BettaEntity male = null, female = null;
        for (BettaEntity b : bettas) {
            stages[b.lifeStage().ordinal()]++;
            if (b.isAdult() && !b.isFemale() && (male == null || fit(b) && !fit(male))) male = b;
            if (b.isAdult() && b.isFemale() && (female == null || fit(b) && !fit(female))) female = b;
        }
        out.putIntArray("stages", stages);
        ListTag warn = new ListTag();
        for (String w : warnings(level, controller)) warn.add(StringTag.valueOf(w));
        out.put("warnings", warn);
        out.putFloat("temp", tank.waterTemp());
        out.putFloat("min_temp", (float) minTemp());
        out.putFloat("max_temp", (float) maxTemp());

        BubbleNest n = tank.nest();
        BettaGenotype mother = null, father = null;
        String motherName = "", fatherName = "";
        if (n != null) {
            CompoundTag nt = new CompoundTag();
            nt.putString("phase", n.phase.name().toLowerCase(java.util.Locale.ROOT));
            double frac = switch (n.phase) {
                case BUILDING -> n.progress / Math.max(1e-6, ServerConfig.getOr(ServerConfig.BREEDING_NEST_BUILD_DAYS, 0.25));
                case BUILT -> 1.0;
                case EGGS -> n.progress / Math.max(1e-6, LifeStage.EGG.days() + LifeStage.LARVA.days());
            };
            nt.putFloat("progress", (float) Mth.clamp(frac, 0, 1));
            nt.putInt("eggs", n.eggs.size());
            nt.putString("stage", n.phase == BubbleNest.Phase.EGGS ? n.stage().id() : "");
            nt.putFloat("days_left", (float) n.daysUntilFree());
            nt.putFloat("care", (float) n.careScore());
            // M66: what the care is made of right now (tooltip), and the survival range of the larvae
            nt.putInt("temp_state", breedingTemp(tank) ? 0 : TankTemperature.isTolerable(tank.waterTemp()) ? 1 : 2);
            nt.putBoolean("crowded", controller.isCrowded());
            nt.putBoolean("father_fed", male != null && !male.health().isHungry(BettaCare.params()));
            double lo = ServerConfig.getOr(ServerConfig.BREEDING_MIN_SURVIVAL, 0.6), hi = ServerConfig.getOr(ServerConfig.BREEDING_MAX_SURVIVAL, 0.9);
            nt.putFloat("surv_lo", (float) lo);
            nt.putFloat("surv_hi", (float) Math.max(lo, hi));
            out.put("nest", nt);
            if (n.motherGenotype != null) {
                mother = n.motherGenotype;
                motherName = n.motherName;
            }
            father = n.fatherGenotype;
            fatherName = n.fatherName;
        }
        if (father == null && male != null) {
            father = male.genotype();
            fatherName = TankCare.nameArg(male);
        }
        if (mother == null && female != null) {
            mother = female.genotype();
            motherName = TankCare.nameArg(female);
        }
        // readiness checklist (no nest yet)
        CompoundTag cond = new CompoundTag();
        cond.putBoolean("male", male != null);
        cond.putBoolean("female", female != null);
        cond.putBoolean("temp", breedingTemp(tank));
        cond.putBoolean("male_fit", male != null && fit(male));
        cond.putBoolean("female_fit", female != null && fit(female));
        cond.putFloat("cooldown", female == null ? 0 : (float) female.breeding().cooldown());
        cond.putBoolean("live_food", male != null && male.breeding().isConditioned() || female != null && female.breeding().isConditioned());
        cond.putBoolean("mirror", male != null && male.breeding().isTrained()
                && ServerConfig.getOr(ServerConfig.BREEDING_MIRROR_BONUS, 1.25) > 1.0);
        cond.putBoolean("plants", org.overengineer.bettacraft.tank.plants.TankPlants.breedingBonus(tank) > 1.0);
        cond.putBoolean("aquatic", org.overengineer.bettacraft.plants.InteriorPlants.breedingBonus(level.getServer(), tank) > 1.0); // M13
        cond.putFloat("hiding", (float) org.overengineer.bettacraft.plants.InteriorPlants.hiding(level.getServer(), tank)); // M13
        cond.putFloat("ready", male == null ? 0 : (float) Mth.clamp(male.breeding().ready()
                / Math.max(1e-6, ServerConfig.getOr(ServerConfig.BREEDING_READY_DAYS, 0.25)), 0, 1));
        out.put("conditions", cond);
        // M22.3: the prediction shows the hidden genes: only once both parents (in the tank) are analysed
        boolean known = org.overengineer.bettacraft.genetics.GeneVisibility.free()
                || (male == null || male.getBettaData().analysed()) && (female == null || female.getBettaData().analysed());
        if (mother != null && father != null && !known) {
            out.putString("mother", motherName);
            out.putString("father", fatherName);
            out.putBoolean("prediction_hidden", true);
        } else if (mother != null && father != null) {
            out.putString("mother", motherName);
            out.putString("father", fatherName);
            out.put("mother_genome", mother.save());
            out.put("father_genome", father.save());
            ListTag pred = new ListTag();
            for (Map.Entry<Locus, List<BettaGenotype.Outcome>> e : BettaGenotype.predict(mother, father).entrySet()) {
                CompoundTag l = new CompoundTag();
                l.putString("locus", e.getKey().id());
                ListTag outs = new ListTag();
                for (BettaGenotype.Outcome o : e.getValue()) {
                    CompoundTag oc = new CompoundTag();
                    oc.putString("a", o.allele());
                    oc.putFloat("p", (float) o.probability());
                    outs.add(oc);
                }
                l.put("o", outs);
                pred.add(l);
            }
            out.put("prediction", pred);
        }
        return out;
    }

    /** Short state for Jade: e.g. "eggs" + count. */
    public static String nestState(TankData tank) {
        BubbleNest n = tank.nest();
        if (n == null) return "none";
        return n.phase == BubbleNest.Phase.EGGS ? n.visibleStage().id() : n.phase.name().toLowerCase(java.util.Locale.ROOT);
    }

    static Locus locus(String id) {
        Locus l = BettaLoci.byId(id);
        return l == null ? BettaLoci.FINFORM : l;
    }
}
