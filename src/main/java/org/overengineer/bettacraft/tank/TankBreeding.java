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
        if (days <= 0) return;
        TankData tank = controller.getTank();
        long now = level.getGameTime();
        List<Entity> live = TankInhabitants.liveInhabitants(level, tank);
        List<BettaEntity> bettas = bettas(live);
        List<BettaEntity> males = new ArrayList<>(), females = new ArrayList<>(), fry = new ArrayList<>();
        for (BettaEntity b : bettas) {
            if (b.isAdult()) (b.isFemale() ? females : males).add(b);
            else if (b.lifeStage() == LifeStage.FRY) fry.add(b);
        }
        nest(level, controller, tank, live.size(), males, females, days, now);
        fights(level, controller, males, days, now);
        maleEatsFry(level, controller, males, fry, days);
        predators(level, controller, live, days);
    }

    private static void nest(ServerLevel level, TankBlockEntity controller, TankData tank, int liveCount, List<BettaEntity> males,
                             List<BettaEntity> females, double days, long now) {
        BubbleNest n = tank.nest();
        RandomSource r = level.getRandom();
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
            male.breeding().addReady(days * (live ? 2.0 : 1.0));
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
                controller.markTankChanged();
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
                    BettaEntity target = nearest(male, females);
                    if (target != null) {
                        male.pursue(target, now + HOLD);
                        male.flare(now + HOLD);
                        for (BettaEntity f : females) {
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
                controller.markTankChanged();
            }
        }
    }

    /**
     * Nest position (PLAN 5.4): at the water surface, preferably under the floating plant of the interior (e.g. a lily
     * pad) that is closest to the male, otherwise where the male is (slightly inside the top water block).
     */
    static Vec3 nestSpot(ServerLevel level, TankData tank, Vec3 near) {
        Vec3 plant = floatingPlantNear(level, tank, near);
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

    static Vec3 nestSpot(TankData tank, Vec3 near) {
        AABB w = TankCare.waterBox(tank);
        double x = Mth.clamp(near.x, w.minX + 0.3, w.maxX - 0.3);
        double z = Mth.clamp(near.z, w.minZ + 0.3, w.maxZ - 0.3);
        double y = Math.max(w.minY + 0.1, w.maxY - 0.06);
        return new Vec3(x, y, z);
    }

    private static void abandon(ServerLevel level, TankBlockEntity controller, TankData tank, String key) {
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
        AABB water = TankCare.waterBox(tank);
        int spawned = 0;
        for (int i = 0; i < survivors; i++) {
            BubbleNest.Egg egg = eggs.get(i);
            BettaEntity fry = ModEntities.BETTA.get().create(level);
            if (fry == null) continue;
            fry.setBettaData(BettaData.offspring(egg.genotype(), egg.female(), n.motherFishId, n.fatherFishId, n.generation, level.getGameTime()));
            Vec3 p = new Vec3(Mth.clamp(n.pos.x + (r.nextDouble() - 0.5) * 0.6, water.minX + 0.1, water.maxX - 0.1),
                    Math.max(water.minY + 0.1, n.pos.y - 0.2 - r.nextDouble() * 0.3),
                    Mth.clamp(n.pos.z + (r.nextDouble() - 0.5) * 0.6, water.minZ + 0.1, water.maxZ - 0.1));
            fry.moveTo(p.x, p.y, p.z, r.nextFloat() * 360F, 0F);
            TankInhabitants.adopt(fry, controller);
            if (level.addFreshEntity(fry)) {
                tank.inhabitants().add(fry.getUUID());
                spawned++;
            }
        }
        tank.setNest(null);
        controller.chaseLogged = false;
        TankCare.log(level, controller, "log.bettacraft.fry_free", String.valueOf(spawned), String.valueOf(n.eggs.size() - spawned));
        controller.markTankChanged();
    }

    // ------------------------------------------------------------------ fights

    private static void fights(ServerLevel level, TankBlockEntity controller, List<BettaEntity> males, double days, long now) {
        if (!ServerConfig.getOr(ServerConfig.COMBAT_MALES_FIGHT, true) || males.size() < 2) {
            controller.fightLogged = false;
            return;
        }
        double perDay = 6.0 / ServerConfig.getOr(ServerConfig.COMBAT_FIGHT_DAYS, 1.0);
        RandomSource r = level.getRandom();
        for (BettaEntity m : males) {
            if (!m.isAlive()) continue;
            BettaEntity rival = nearest(m, males);
            if (rival == null) continue;
            m.pursue(rival, now + HOLD);
            m.flare(now + HOLD);
            double dmg = rival.getMaxHealth() / 6.0 * perDay * days * (0.5 + r.nextDouble());
            m.addDamage(dmg, rival);
        }
        if (!controller.fightLogged) {
            controller.fightLogged = true;
            TankCare.log(level, controller, "log.bettacraft.fight", TankCare.nameArg(males.get(0)), TankCare.nameArg(males.get(1)));
        }
    }

    private static void maleEatsFry(ServerLevel level, TankBlockEntity controller, List<BettaEntity> males, List<BettaEntity> fry, double days) {
        double rate = ServerConfig.getOr(ServerConfig.BREEDING_MALE_EATS_FRY_PER_DAY, 0.3);
        if (males.isEmpty() || fry.isEmpty() || rate <= 0) return;
        double p = 1 - Math.exp(-rate * days * males.size());
        RandomSource r = level.getRandom();
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
        controller.getTank().inhabitants().remove(prey.getUUID());
        prey.discard();
        controller.markTankChanged();
    }

    // ------------------------------------------------------------------ predators

    private static void predators(ServerLevel level, TankBlockEntity controller, List<Entity> live, double days) {
        double rate = ServerConfig.getOr(ServerConfig.COMBAT_PREDATOR_ATTACKS_PER_DAY, 4.0);
        if (rate <= 0) return;
        RandomSource r = level.getRandom();
        for (Entity pred : live) {
            if (!pred.isAlive() || !pred.getType().is(ModTags.PREDATORS) || !(pred instanceof LivingEntity predator)) continue;
            List<LivingEntity> prey = new ArrayList<>();
            for (Entity e : live) {
                if (e != pred && e.isAlive() && e instanceof LivingEntity le && !e.getType().is(ModTags.PREDATORS)
                        && e.getBbWidth() <= PREY_MAX_WIDTH) prey.add(le);
            }
            if (prey.isEmpty()) continue;
            LivingEntity victim = prey.stream().min(Comparator.comparingDouble(e -> e.getBbWidth() * e.getBbHeight())).orElseThrow();
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
        if (predator && live.stream().anyMatch(e -> !e.getType().is(ModTags.PREDATORS) && e.getBbWidth() <= PREY_MAX_WIDTH)) out.add("predator");
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
        cond.putFloat("ready", male == null ? 0 : (float) Mth.clamp(male.breeding().ready()
                / Math.max(1e-6, ServerConfig.getOr(ServerConfig.BREEDING_READY_DAYS, 0.25)), 0, 1));
        out.put("conditions", cond);
        if (mother != null && father != null) {
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
