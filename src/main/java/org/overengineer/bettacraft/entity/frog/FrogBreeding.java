package org.overengineer.bettacraft.entity.frog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.food.BettaFood;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankFrogs;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.plants.TankPlants;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;

/**
 * M47 part c: the breeding of the dwarf clawed frogs, like in nature (E1), run with the care step of the tank:
 * <ol>
 *     <li><b>Amplexus:</b> within a day after a cool water change ({@link TankmateBreeding#coolChangeRecent}, the trigger of
 *     the corydoras) at 24-28 °C a grown, fed male clasps a grown, fed female from behind; the pair swims up to the surface
 *     ({@link DwarfFrogEntity#AMPLEXUS_TICKS}) and the female releases her eggs there.</li>
 *     <li><b>Spawn:</b> 20-50 eggs float at the surface ({@link FrogSpawn}, among the floating plants). Adult bettas, the
 *     tankmates that eat eggs and a little the frogs themselves eat them; floating plants hide most of them. After
 *     {@code frogs.frogSpawnHatchDays} the rest hatch.</li>
 *     <li><b>Tadpoles</b> ({@link Tadpole}, data animals like the shrimp): they hover in the open water and need fine food
 *     (infusoria, artemia nauplii, fry food; a planted tank grows a little by itself). Hungry for two days they die. Bettas
 *     hunt them (with bettas in the tank hardly one survives), other fish too; plants hide them. After
 *     {@code frogs.tadpoleDays} a tadpole becomes a froglet ({@link DwarfFrogEntity}, small, grows up in
 *     {@code frogs.frogletDays}).</li>
 * </ol>
 * Switch {@code features.frogs.breeding} (off: no new amplexus; spawn and tadpoles already there still grow).
 */
public final class FrogBreeding {
    /** Breeding temperature (°C). */
    public static final float MIN_TEMP = 24F, MAX_TEMP = 28F;
    /** Chance per day of a ready female after a cool water change (times frogs.frogBreedingSpeed). */
    public static final double RATE = 6.0;
    /** Eggs of one spawning. */
    public static final int EGGS_MIN = 20, EGGS_MAX = 50;
    /** Fine food portions a tadpole needs per day; planted tanks grow this share of it by themselves (infusoria). */
    public static final double FOOD_PER_DAY = 1.0, PLANTED_SHARE = 0.3, BARE_SHARE = 0.05;
    /** Days a tadpole survives without enough food. */
    public static final double STARVE_DAYS = 2.0;
    /** Eggs a hunter eats per day (times tankmates.eggsEatenPerDay, uncovered spawn) and tadpoles a betta hunts per day. */
    public static final double EGG_HUNT = 2.0, TADPOLE_HUNT_BETTA = 0.8, TADPOLE_HUNT_FISH = 0.25;
    /** Days the female (male) waits before the next amplexus. */
    public static final double FEMALE_REST = 3.0, MALE_REST = 1.0;

    private FrogBreeding() {}

    public static boolean enabled() {
        return Features.enabled(Feature.FROG_BREEDING);
    }

    public static double hatchDays() {
        return ServerConfig.getOr(ServerConfig.FROGS_SPAWN_HATCH_DAYS, 2.0);
    }

    public static double tadpoleDays() {
        return ServerConfig.getOr(ServerConfig.FROGS_TADPOLE_DAYS, 6.0);
    }

    public static double frogletDays() {
        return ServerConfig.getOr(ServerConfig.FROGS_FROGLET_DAYS, 4.0);
    }

    public static int maxTadpoles() {
        return ServerConfig.getOr(ServerConfig.FROGS_MAX_TADPOLES, 40);
    }

    static double speed() {
        return ServerConfig.getOr(ServerConfig.FROGS_BREEDING_SPEED, 1.0);
    }

    private static double chance(double rate, double days) {
        return 1 - Math.exp(-Math.max(0, rate) * days);
    }

    /** The water is right for spawning (the breeding temperature, air to breathe). */
    public static boolean breedingTemp(TankData t) {
        return t.waterTemp() >= MIN_TEMP - 1e-4 && t.waterTemp() <= MAX_TEMP + 1e-4;
    }

    /** A frog that may go into amplexus now: grown, fed within two days, at least half health, rested, not drowning. */
    public static boolean ready(DwarfFrogEntity f) {
        return f.isAlive() && f.isAdult() && f.daysSinceFed() < 2.0 && f.getHealth() >= f.getMaxHealth() * 0.5F && f.restDays() <= 0
                && f.breath() > 0 && !f.inAmplexus();
    }

    // ------------------------------------------------------------------ the step

    /** One care step of {@code days} (also used by GameTests with longer steps). */
    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        if (days <= 0 || !Features.enabled(Feature.FROGS)) return; // M33 part b: the frogs rest
        TankData t = c.getTank();
        boolean brood = !t.frogSpawn().isEmpty() || !t.tadpoles().isEmpty();
        List<DwarfFrogEntity> frogs = brood || t.lastCoolChange() >= 0 ? TankFrogs.frogs(level, t) : List.of();
        boolean changed = false;
        if (enabled() && frogs.size() >= 2) changed |= pair(level, c, frogs, days);
        if (brood) {
            List<Entity> live = TankInhabitants.liveInhabitants(level, t);
            changed |= spawn(level, c, live, days);
            changed |= tadpoles(level, c, live, days);
        }
        if (changed) c.markTankChanged();
    }

    /** After a cool water change at the right temperature a ready male clasps a ready female. */
    private static boolean pair(ServerLevel level, TankBlockEntity c, List<DwarfFrogEntity> frogs, double days) {
        TankData t = c.getTank();
        if (!TankmateBreeding.coolChangeRecent(level, t) || !breedingTemp(t) || !TankFrogs.hasAir(t)) return false;
        if (t.tadpoles().size() + eggs(t) >= maxTadpoles()) return false;
        RandomSource r = level.getRandom();
        List<DwarfFrogEntity> females = new ArrayList<>(), males = new ArrayList<>();
        for (DwarfFrogEntity f : frogs) if (ready(f)) (f.isFemale() ? females : males).add(f);
        females.sort(Comparator.comparing(Entity::getUUID));
        boolean started = false;
        for (DwarfFrogEntity f : females) {
            if (males.isEmpty()) break;
            if (r.nextDouble() >= chance(RATE * speed(), days)) continue;
            DwarfFrogEntity m = males.getFirst();
            for (DwarfFrogEntity x : males) if (x.distanceToSqr(f) < m.distanceToSqr(f)) m = x;
            males.remove(m);
            m.clasp(f);
            TankCare.log(level, c, "log.bettacraft.frog.amplexus", TankCare.nameArg(m), TankCare.nameArg(f));
            started = true;
        }
        return started;
    }

    /** All the eggs of the spawn clumps in a tank. */
    public static int eggs(TankData t) {
        int n = 0;
        for (FrogSpawn s : t.frogSpawn()) n += s.count;
        return n;
    }

    /**
     * The end of the amplexus (called by the male): the female releases her eggs at the surface above her. Returns the
     * clump or null (no tank, no water).
     */
    @Nullable
    public static FrogSpawn lay(ServerLevel level, TankBlockEntity c, DwarfFrogEntity female, DwarfFrogEntity male) {
        TankData t = c.getTank();
        if (t.waterMb() <= 0) return null;
        RandomSource r = level.getRandom();
        TankWaterVolume w = new TankWaterVolume(t);
        Vec3 p = w.surfacePoint(female.position(), 0.15, 0.02);
        int n = EGGS_MIN + r.nextInt(EGGS_MAX - EGGS_MIN + 1);
        FrogSpawn s = new FrogSpawn(t.nextFrogBroodId(), p.x, w.surface(), p.z, n);
        t.frogSpawn().add(s);
        female.rest(FEMALE_REST);
        male.rest(MALE_REST);
        level.sendParticles(ParticleTypes.BUBBLE, p.x, w.surface() - 0.05, p.z, 8, 0.08, 0.02, 0.08, 0.01);
        TankCare.log(level, c, "log.bettacraft.frog.spawned", TankCare.nameArg(female), String.valueOf(n));
        c.markTankChanged();
        return s;
    }

    /** Hunters of the floating eggs: adult bettas, tankmates by how much they eat eggs and fry, a little the frogs. */
    private static double eggHunters(List<Entity> live) {
        double h = 0;
        for (Entity e : live) {
            if (e instanceof BettaEntity b && b.isAdult()) h += 1.0;
            else if (e instanceof TankmateEntity m && m.isAdult()) h += m.species().eatsFry();
            else if (e instanceof DwarfFrogEntity f && f.isAdult()) h += 0.02; // they feed at the bottom, now and then one snaps at the spawn
        }
        return h;
    }

    /** How much of the spawn the floating plants hide (0..0.9). */
    public static double spawnCover(TankData t) {
        return Math.min(0.9, TankPlants.coverage(t) * 1.2);
    }

    private static boolean spawn(ServerLevel level, TankBlockEntity c, List<Entity> live, double days) {
        TankData t = c.getTank();
        if (t.frogSpawn().isEmpty()) return false;
        RandomSource r = level.getRandom();
        double hunters = eggHunters(live);
        double rate = ServerConfig.getOr(ServerConfig.TANKMATES_EGGS_EATEN, 1.5) * EGG_HUNT * hunters * (1 - spawnCover(t));
        double surface = t.waterMb() > 0 ? new TankWaterVolume(t).surface() : Double.NaN;
        boolean bad = TankWater.isBad(t);
        boolean changed = false;
        for (FrogSpawn s : new ArrayList<>(t.frogSpawn())) {
            s.age += days;
            if (Double.isFinite(surface) && Math.abs(s.y - surface) > 1e-3) {
                s.y = surface; // the spawn floats with the water level
                changed = true;
            }
            int before = s.count;
            if (rate > 0) s.count -= binomial(r, s.count, chance(rate, days));
            if (bad) s.count -= binomial(r, s.count, chance(0.3, days)); // fungus in bad water
            if (t.waterMb() <= 0) s.count = 0;
            if (s.count != before) changed = true;
            if (s.count <= 0) {
                t.frogSpawn().remove(s);
                TankCare.log(level, c, "log.bettacraft.frog.spawn_eaten");
                changed = true;
                continue;
            }
            if (s.age < hatchDays()) continue;
            // the tadpoles hatch (as many as there is room for)
            t.frogSpawn().remove(s);
            int n = Math.min(s.count, Math.max(0, maxTadpoles() - t.tadpoles().size()));
            TankWaterVolume w = new TankWaterVolume(t);
            for (int i = 0; i < n; i++) {
                Vec3 p = w.clamp(new Vec3(s.x + r.nextGaussian() * 0.15, s.y - 0.1 - r.nextDouble() * 0.25, s.z + r.nextGaussian() * 0.15), 0.08, 0.1, 0.06);
                t.tadpoles().add(new Tadpole(t.nextFrogBroodId(), p.x, p.y, p.z));
            }
            if (n > 0) TankCare.log(level, c, "log.bettacraft.frog.tadpoles_hatched", String.valueOf(n));
            changed = true;
        }
        return changed;
    }

    /** Hunters of the tadpoles: {bettas, other fish} (adults). */
    private static double tadpoleHunt(List<Entity> live) {
        double h = 0;
        for (Entity e : live) {
            if (e instanceof BettaEntity b && b.isAdult()) h += TADPOLE_HUNT_BETTA;
            else if (e instanceof TankmateEntity m && m.isAdult()) h += TADPOLE_HUNT_FISH * Math.min(1.0, m.species().eatsFry() * 2);
        }
        return h;
    }

    /** How well plants hide the tadpoles (interior plants and floating plants, 0..0.7). */
    public static double tadpoleCover(ServerLevel level, TankData t) {
        return Math.min(0.7, org.overengineer.bettacraft.plants.InteriorPlants.hiding(level.getServer(), t) + TankPlants.coverage(t) * 0.3);
    }

    /** The tank grows a little fine food by itself: more with plants (interior or floating). */
    public static boolean planted(ServerLevel level, TankData t) {
        return !t.plants().isEmpty() || org.overengineer.bettacraft.plants.InteriorPlants.hiding(level.getServer(), t) > 0;
    }

    private static boolean tadpoles(ServerLevel level, TankBlockEntity c, List<Entity> live, double days) {
        TankData t = c.getTank();
        List<Tadpole> list = t.tadpoles();
        if (list.isEmpty()) return false;
        RandomSource r = level.getRandom();
        boolean changed = false;
        if (t.waterMb() <= 0) {
            list.clear();
            TankCare.log(level, c, "log.bettacraft.frog.tadpoles_dried");
            return true;
        }
        // food: what the tank grows by itself, then the fine food in the water
        double need = list.size() * FOOD_PER_DAY * days;
        double own = need * (planted(level, t) ? PLANTED_SHARE : BARE_SHARE);
        double fromPool = Math.min(t.tadpoleFood(), need - own);
        t.setTadpoleFood(t.tadpoleFood() - fromPool);
        double fed = need <= 0 ? 1 : Math.min(1, (own + fromPool) / need);
        // the hunters
        double hunt = tadpoleHunt(live) * (1 - tadpoleCover(level, t));
        double eaten = hunt > 0 ? chance(hunt, days) : 0;
        int starved = 0, hunted = 0;
        TankWaterVolume w = new TankWaterVolume(t);
        for (Tadpole k : new ArrayList<>(list)) {
            k.age += days;
            k.hunger = Math.max(0, k.hunger + days * (1 - fed) - days * fed * 0.5);
            if (k.hunger >= STARVE_DAYS) {
                list.remove(k);
                starved++;
                continue;
            }
            if (eaten > 0 && r.nextDouble() < eaten) {
                list.remove(k);
                hunted++;
                continue;
            }
            // now and then a tadpole moves on to another spot in the open water
            if (r.nextDouble() < chance(30, days)) {
                Vec3 p = w.clamp(new Vec3(k.x + r.nextGaussian() * 0.3, k.y + r.nextGaussian() * 0.15, k.z + r.nextGaussian() * 0.3), 0.08, 0.12, 0.06);
                k.x = p.x;
                k.y = p.y;
                k.z = p.z;
                changed = true;
            }
        }
        if (starved > 0) TankCare.log(level, c, "log.bettacraft.frog.tadpoles_starved", String.valueOf(starved));
        if (hunted > 0) TankCare.log(level, c, "log.bettacraft.frog.tadpoles_eaten", String.valueOf(hunted));
        changed |= starved > 0 || hunted > 0;
        // grown tadpoles become froglets (as many as the tank holds)
        int frogs = 0;
        for (Tadpole k : new ArrayList<>(list)) {
            if (k.age < tadpoleDays()) continue;
            if (!room(level, c)) break;
            if (froglet(level, c, new Vec3(k.x, k.y, k.z)) == null) break;
            list.remove(k);
            frogs++;
        }
        if (frogs > 0) {
            TankCare.log(level, c, "log.bettacraft.frog.froglets", String.valueOf(frogs));
            changed = true;
        }
        return changed;
    }

    /** Room for one more frog: the entity limit and the stocking limit of the tank. */
    private static boolean room(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        List<Entity> live = TankInhabitants.liveInhabitants(level, t);
        return live.size() < ServerConfig.getOr(ServerConfig.TANK_MAX_ENTITIES, 64)
                && TankInhabitants.stocking(live, t) < TankInhabitants.hardCap(t.waterBlocks()) - 1e-6;
    }

    /** A new froglet in the tank at {@code p}. */
    @Nullable
    public static DwarfFrogEntity froglet(ServerLevel level, TankBlockEntity c, Vec3 p) {
        DwarfFrogEntity f = ModEntities.DWARF_FROG.get().create(level);
        if (f == null) return null;
        f.makeFroglet();
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        Vec3 at = w.clamp(p, 0.2, 0.05, 0.2);
        f.moveTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360F, 0F);
        f.setPersistenceRequired();
        TankInhabitants.adopt(f, c);
        if (!level.addFreshEntity(f)) return null;
        c.getTank().inhabitants().add(f.getUUID());
        f.feed();
        return f;
    }

    // ------------------------------------------------------------------ food

    /** Fine food (infusoria, artemia nauplii, fry food) for the tadpoles. */
    public static boolean eats(@Nullable BettaFood.Kind kind) {
        return kind == BettaFood.Kind.FRY_START || kind == BettaFood.Kind.FRY;
    }

    /** Whether the tank has tadpoles that want this food. */
    public static boolean wantFood(TankData t, @Nullable BettaFood.Kind kind) {
        return eats(kind) && !t.tadpoles().isEmpty() && t.tadpoleFood() < t.tadpoles().size() * FOOD_PER_DAY * 2;
    }

    /** {@code portions} of fine food into the water for the tadpoles (at most three days of it). Returns what they took. */
    public static int feed(TankData t, @Nullable BettaFood.Kind kind, int portions) {
        if (!wantFood(t, kind) || portions <= 0) return 0;
        double cap = t.tadpoles().size() * FOOD_PER_DAY * 3;
        int took = (int) Math.min(portions, Math.ceil(cap - t.tadpoleFood()));
        if (took <= 0) return 0;
        t.setTadpoleFood(t.tadpoleFood() + took);
        return took;
    }

    static int binomial(RandomSource r, int n, double p) {
        if (n <= 0 || p <= 0) return 0;
        if (p >= 1) return n;
        if (n > 60) return (int) Math.round(Mth.clamp(n * p + r.nextGaussian() * Math.sqrt(n * p * (1 - p)), 0, n));
        int k = 0;
        for (int i = 0; i < n; i++) if (r.nextDouble() < p) k++;
        return k;
    }
}
