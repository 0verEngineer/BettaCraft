package org.overengineer.bettacraft.critters;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;

/**
 * The life of the small animals of a tank (M14), one care step at a time: eating (a shared food pool of biofilm, algae
 * and leftovers), growing up, moulting (crayfish), water and temperature stress, bettas hunting the young (plants and
 * moss hide them), assassin snails eating other snails, broods and clutches, and the bodies of dead animals. Pure logic
 * (unit-tested); {@code TankCritters} wires it to the tank.
 */
public final class CritterLife {
    /** Days the body of a dead animal lies there (faster with scavengers in the tank). */
    public static final double CORPSE_DAYS = 1.0, CORPSE_DAYS_SCAVENGED = 0.3;
    /** Days a shed crayfish shell lies there. */
    public static final double MOLT_DAYS = 1.0;
    /** Days between two moults of a crayfish (plus up to two days at random). */
    public static final double MOLT_INTERVAL = 3.0;
    /** Hunting rate of one betta per day on a fully exposed young shrimp (scaled by the prey's weight and the hiding). */
    public static final double HUNT_RATE = 0.5;
    /** Hazard per day of an assassin snail to catch a snail. */
    public static final double ASSASSIN_RATE = 0.3;
    /** Food stock the pool keeps at most, in days of the colony's needs. */
    public static final double POOL_DAYS = 3.0;
    /**
     * M29 part d: small animals one big crayfish catches per day when nothing hides them ({@code invertebrates.bigCrayfishPreyPerDay},
     * set by the mod; plants, moss and shelters hide the prey).
     */
    public static volatile java.util.function.DoubleSupplier BIG_PREY = () -> 0.4;

    private CritterLife() {}

    /**
     * Conditions of the tank for one step.
     *
     * @param bettas  adult bettas hunting
     * @param hiding  0..1 share of the hunts that fail because the prey hides (plants, moss)
     * @param cap     most animals the tank holds (breeding stops there)
     * @param breed   breeding speed per group (config)
     * @param air     whether there is air above the water (apple snail clutches)
     * @param hunting whether bettas hunt at all (config)
     */
    public record Env(float quality, boolean qualityEnabled, float temp, int bettas, double hiding, int cap,
                      Map<CritterSpecies.Group, Double> breed, boolean air, boolean hunting, double salinity, double saltSpeed, double oxygenShortage,
                      double ph, double tds, double co2) {
        /** M25 part a: without CO2 dosing. */
        public Env(float quality, boolean qualityEnabled, float temp, int bettas, double hiding, int cap,
                   Map<CritterSpecies.Group, Double> breed, boolean air, boolean hunting, double salinity, double saltSpeed, double oxygenShortage,
                   double ph, double tds) {
            this(quality, qualityEnabled, temp, bettas, hiding, cap, breed, air, hunting, salinity, saltSpeed, oxygenShortage, ph, tds, Double.NaN);
        }

        /** M23 and M24: without water chemistry (NaN: nobody minds it). */
        public Env(float quality, boolean qualityEnabled, float temp, int bettas, double hiding, int cap,
                   Map<CritterSpecies.Group, Double> breed, boolean air, boolean hunting, double salinity, double saltSpeed, double oxygenShortage) {
            this(quality, qualityEnabled, temp, bettas, hiding, cap, breed, air, hunting, salinity, saltSpeed, oxygenShortage, Double.NaN, Double.NaN);
        }

        /** M25: how far the water is out of the species' pH / TDS range (0 without chemistry). */
        public double chemDistance(CritterSpecies s) {
            double d = Double.isNaN(ph) ? 0 : org.overengineer.bettacraft.chem.ChemTolerance.critter(s).distance(ph, tds);
            return Math.max(d, co2Distance(s));
        }

        /** M25 part b: how far too much CO2 is out of the range (shrimp and crayfish bear less; 0 without CO2 dosing). */
        public double co2Distance(CritterSpecies s) {
            if (Double.isNaN(co2)) return 0;
            return org.overengineer.bettacraft.co2.TankCo2.distance(co2, org.overengineer.bettacraft.co2.TankCo2.stressFrom(),
                    org.overengineer.bettacraft.co2.TankCo2.lethalFrom(), s.isSnail() ? 1.0 : org.overengineer.bettacraft.co2.TankCo2.SHRIMP);
        }

        /** M22 and earlier: without an oxygen shortage. */
        public Env(float quality, boolean qualityEnabled, float temp, int bettas, double hiding, int cap,
                   Map<CritterSpecies.Group, Double> breed, boolean air, boolean hunting, double salinity, double saltSpeed) {
            this(quality, qualityEnabled, temp, bettas, hiding, cap, breed, air, hunting, salinity, saltSpeed, 0);
        }

        public Env(float quality, boolean qualityEnabled, float temp, int bettas, double hiding, int cap,
                   Map<CritterSpecies.Group, Double> breed, boolean air, boolean hunting) {
            this(quality, qualityEnabled, temp, bettas, hiding, cap, breed, air, hunting, 0, 1);
        }

        public double breed(CritterSpecies s) {
            return breed.getOrDefault(s.group(), 1.0);
        }
    }

    public enum Cause { WATER, TEMPERATURE, HUNGER, BETTA, ASSASSIN, CRAYFISH, AIR, SALINITY, OXYGEN, CHEMISTRY, CO2, BIG_CRAYFISH }

    /** What happened in a step (for the tank log and the GameTests). */
    public static final class Result {
        public final List<Critter> born = new ArrayList<>();
        public final Map<CritterSpecies, int[]> bornBySpecies = new EnumMap<>(CritterSpecies.class);
        public final List<Critter> died = new ArrayList<>();
        public final Map<Critter, Cause> causes = new java.util.IdentityHashMap<>();
        public final List<Critter> eaten = new ArrayList<>();
        public final List<Critter> molted = new ArrayList<>();
        public final List<CritterObject> hatchedClutches = new ArrayList<>();
        /** Bodies that rotted away (the water gets dirtier). */
        public int rotted;
        public double foodEaten;
        public int berried;

        void born(Critter c) {
            born.add(c);
            bornBySpecies.computeIfAbsent(c.species, k -> new int[1])[0]++;
        }
    }

    // ------------------------------------------------------------------ food

    /** Food an animal needs per day (young half). */
    public static double need(Critter c) {
        return c.species.foodPerDay() * (c.adult() ? 1.0 : 0.5);
    }

    /** Food the living animals need per day. */
    public static double demand(List<Critter> list) {
        double d = 0;
        for (Critter c : list) if (c.alive()) d += need(c);
        return d;
    }

    /** Most food the pool keeps (it spoils beyond that). */
    public static double poolCap(List<Critter> list) {
        return 1.0 + POOL_DAYS * demand(list);
    }

    // ------------------------------------------------------------------ the step

    /**
     * One step of {@code days}. {@code pool} holds the food pool (portions, index 0) and is changed. New animals are added
     * to {@code list} (placed where their mother was; the caller puts them on the floor), eaten and rotted animals are
     * removed, new moults are not created here (see {@link Result#molted}).
     */
    public static Result step(List<Critter> list, List<CritterObject> objects, Env env, double days, double[] pool, RandomGenerator rnd,
                              IntSupplier ids) {
        Result r = new Result();
        if (days <= 0) return r;
        boolean scavengers = false;
        for (Critter c : list) if (c.alive() && (c.species.isCrayfish() || c.species.isShrimp() || c.species.isSnail())) scavengers = true;
        // bodies
        for (Iterator<Critter> it = list.iterator(); it.hasNext(); ) {
            Critter c = it.next();
            if (c.alive()) continue;
            c.dead += days;
            if (c.dead >= (scavengers ? CORPSE_DAYS_SCAVENGED : CORPSE_DAYS)) {
                it.remove();
                if (!scavengers) r.rotted++;
            }
        }
        // food
        // the strongest (least hungry) eat first: with too little food some starve, the others stay fit (a colony
        // shrinks to what the tank feeds instead of all starving slowly)
        List<Critter> eaters = new ArrayList<>();
        for (Critter c : list) if (c.alive()) eaters.add(c);
        java.util.Collections.shuffle(eaters, new java.util.Random(rnd.nextLong()));
        eaters.sort(java.util.Comparator.comparingDouble((Critter c) -> c.hunger));
        double food = pool[0], eatenFood = 0;
        for (Critter c : eaters) {
            double n = need(c) * days;
            if (food >= n) {
                food -= n;
                eatenFood += n;
                c.hunger = Math.max(0, c.hunger - days * 4);
            } else {
                c.hunger += days;
            }
        }
        pool[0] = Math.max(0, food);
        r.foodEaten = eatenFood;
        // ageing, moults, eggs
        for (Critter c : list) {
            if (!c.alive()) continue;
            c.age += days;
            if (c.berried()) c.eggs += days;
            if (c.species.isCrayfish()) {
                c.sinceMolt += days;
                double interval = MOLT_INTERVAL * (c.adult() ? 1.5 : 1.0);
                if (c.sinceMolt > interval && rnd.nextDouble() < days / 1.0) {
                    c.sinceMolt = 0;
                    r.molted.add(c);
                }
            }
        }
        // stress and death
        for (Critter c : list) {
            if (!c.alive()) continue;
            double rate = 0;
            Cause cause = null;
            float bad = c.species.badWater();
            if (env.qualityEnabled() && env.quality() < bad) {
                rate += 2.0 * (bad - env.quality()) / bad;
                cause = Cause.WATER;
            }
            float t = env.temp();
            double tr = t < c.species.minTemp() ? 0.12 * (c.species.minTemp() - t) : t > c.species.maxTemp() ? 0.12 * (t - c.species.maxTemp()) : 0;
            if (tr > 0) {
                rate += tr;
                if (cause == null || tr > rate - tr) cause = Cause.TEMPERATURE;
            }
            // M23: too little oxygen: shrimp, crayfish and crabs suffer, snails (many breathe air) only a little
            if (env.oxygenShortage() > 0) {
                double or = 1.5 * env.oxygenShortage() * (c.species.isSnail() ? 0.2 : 1.0);
                rate += or;
                if (cause == null || or > rate - or) cause = Cause.OXYGEN;
            }
            if (c.hunger > 3) {
                double hr = 0.25 * (c.hunger - 3);
                rate += hr;
                if (cause == null || hr > rate - hr) cause = Cause.HUNGER;
            }
            // M20: the wrong salinity (freshwater shrimp in salt water, reef animals in fresh water)
            double sr = 3 * org.overengineer.bettacraft.tank.Salinity.shockPerDay(org.overengineer.bettacraft.reef.SaltTolerance.critter(c.species),
                    env.salinity(), env.saltSpeed());
            if (sr > 0) {
                rate += sr;
                if (cause == null || sr > rate - sr) cause = Cause.SALINITY;
            }
            // M25: pH / TDS far out of the species' range (a little out only slows the breeding)
            double cd = env.chemDistance(c.species);
            if (cd >= org.overengineer.bettacraft.chem.ChemTolerance.BAD_FROM) {
                double cr = chemRate(cd);
                rate += cr;
                if (cause == null || cr > rate - cr) cause = env.co2Distance(c.species) >= cd ? Cause.CO2 : Cause.CHEMISTRY;
            }
            if (c.soft()) rate *= 1.5;
            if (rate > 0 && rnd.nextDouble() < 1 - Math.exp(-rate * days)) die(c, cause == null ? Cause.WATER : cause, r);
        }
        hunt(list, env, days, rnd, r);
        assassins(list, days, rnd, r);
        crayfish(list, days, rnd, r);
        bigCrayfish(list, env, days, rnd, r);
        breed(list, objects, env, days, rnd, ids, r);
        // objects
        for (Iterator<CritterObject> it = objects.iterator(); it.hasNext(); ) {
            CritterObject o = it.next();
            o.age += days;
            if (o.kind == CritterObject.Kind.MOLT && o.age >= MOLT_DAYS) it.remove();
            else if (o.kind == CritterObject.Kind.CLUTCH && o.age >= CritterSpecies.APPLE_SNAIL.eggDays()) {
                it.remove();
                r.hatchedClutches.add(o);
                int room = env.cap() - alive(list);
                int n = Math.min(o.count, Math.max(0, room));
                for (int i = 0; i < n; i++) {
                    CritterGenes g = o.mother != null && o.father != null
                            ? CritterGenes.cross(o.mother, o.father, CritterSpecies.APPLE_SNAIL.loci().length, rnd) : CritterGenes.WILD;
                    Critter k = new Critter(ids.getAsInt(), CritterSpecies.APPLE_SNAIL, rnd.nextBoolean(), g,
                            o.x + (rnd.nextDouble() - 0.5) * 0.2, o.y, o.z + (rnd.nextDouble() - 0.5) * 0.2);
                    k.age = 0;
                    k.generation = o.generation;
                    k.look = CritterLooks.random(k.species, rnd); // M21.5
                    list.add(k);
                    r.born(k);
                }
            }
        }
        return r;
    }

    public static int alive(List<Critter> list) {
        int n = 0;
        for (Critter c : list) if (c.alive()) n++;
        return n;
    }

    private static void die(Critter c, Cause cause, Result r) {
        c.dead = 0;
        c.eggs = -1;
        r.died.add(c);
        r.causes.put(c, cause);
    }

    /** How attractive an animal is to a hunting betta (per day and betta at {@link #HUNT_RATE}). */
    public static double preyWeight(Critter c) {
        if (!c.alive()) return 0;
        return switch (c.species.group()) {
            case SHRIMP -> c.adult() ? 0.03 : 1.0;
            case CRAYFISH -> c.adult() ? (c.soft() && !c.species.big() ? 0.25 : 0) : (c.soft() ? 1.0 : 0.6);
            case SNAIL -> !c.adult() && (c.species == CritterSpecies.RAMSHORN || c.species == CritterSpecies.BLADDER_SNAIL) ? 0.1 : 0;
        };
    }

    private static void hunt(List<Critter> list, Env env, double days, RandomGenerator rnd, Result r) {
        if (!env.hunting() || env.bettas() <= 0) return;
        double exposed = Math.max(0, 1 - env.hiding());
        for (Iterator<Critter> it = list.iterator(); it.hasNext(); ) {
            Critter c = it.next();
            double w = preyWeight(c);
            if (w <= 0) continue;
            double h = w * HUNT_RATE * env.bettas() * exposed;
            if (rnd.nextDouble() < 1 - Math.exp(-h * days)) {
                it.remove();
                r.eaten.add(c);
                r.causes.put(c, Cause.BETTA);
            }
        }
    }

    /** How likely an assassin snail goes for this snail (hard nerite shells and big apple snails rarely). */
    public static double assassinWeight(Critter c) {
        if (!c.alive() || !c.species.isSnail() || c.species == CritterSpecies.ASSASSIN_SNAIL) return 0;
        if (c.species.nerite()) return 0.1;
        if (c.species == CritterSpecies.APPLE_SNAIL) return c.adult() ? 0.2 : 1.0;
        return c.adult() ? 1.0 : 1.5;
    }

    private static void assassins(List<Critter> list, double days, RandomGenerator rnd, Result r) {
        int hunters = 0;
        for (Critter c : list) if (c.alive() && c.species == CritterSpecies.ASSASSIN_SNAIL && c.adult()) hunters++;
        if (hunters == 0) return;
        for (int i = 0; i < hunters; i++) {
            if (rnd.nextDouble() >= 1 - Math.exp(-ASSASSIN_RATE * days)) continue;
            Critter victim = pick(list, CritterLife::assassinWeight, rnd);
            if (victim == null) return;
            list.remove(victim);
            r.eaten.add(victim);
            r.causes.put(victim, Cause.ASSASSIN);
        }
    }

    /** Crayfish now and then catch a weak (starving) small animal. */
    private static void crayfish(List<Critter> list, double days, RandomGenerator rnd, Result r) {
        int n = 0;
        for (Critter c : list) if (c.alive() && c.species.isCrayfish() && c.adult()) n++;
        if (n == 0) return;
        for (Iterator<Critter> it = list.iterator(); it.hasNext(); ) {
            Critter c = it.next();
            if (!c.alive() || c.species.isCrayfish() || c.hunger < 2 || c.species == CritterSpecies.APPLE_SNAIL) continue;
            if (rnd.nextDouble() < 1 - Math.exp(-0.1 * n * days)) {
                it.remove();
                r.eaten.add(c);
                r.causes.put(c, Cause.CRAYFISH);
            }
        }
    }

    /**
     * M29 part d: how likely a big crayfish goes for this animal: shrimp (young first), small snails and young ones,
     * dwarf crayfish (soft ones first), the soft young of big crayfish (cannibals); nerites, apple snails and the big
     * snails hardly.
     */
    public static double bigCrayfishWeight(Critter c) {
        if (!c.alive()) return 0;
        CritterSpecies s = c.species;
        return switch (s.group()) {
            case SHRIMP -> c.adult() ? 1.0 : 1.5;
            case CRAYFISH -> s.big() ? (!c.adult() && c.soft() ? 0.3 : 0) : c.soft() ? 1.2 : 0.4;
            case SNAIL -> s.nerite() || s == CritterSpecies.APPLE_SNAIL || s == CritterSpecies.TYLOMELANIA || s == CritterSpecies.HERMIT_CRAB
                    || s == CritterSpecies.TURBO_SNAIL ? (c.adult() ? 0.05 : 0.5) : (c.adult() ? 0.5 : 1.0);
        };
    }

    /** M29 part d: big crayfish catch small animals (fewer where plants and moss hide them). */
    private static void bigCrayfish(List<Critter> list, Env env, double days, RandomGenerator rnd, Result r) {
        double rate = BIG_PREY.getAsDouble();
        if (rate <= 0) return;
        int n = 0;
        for (Critter c : list) if (c.alive() && c.species.big() && c.adult()) n++;
        if (n == 0) return;
        double h = rate * n * Math.max(0.1, 1 - env.hiding());
        double expected = h * days;
        int catches = (int) Math.floor(expected) + (rnd.nextDouble() < expected - Math.floor(expected) ? 1 : 0);
        for (int i = 0; i < catches; i++) {
            Critter victim = pick(list, CritterLife::bigCrayfishWeight, rnd);
            if (victim == null) return;
            list.remove(victim);
            r.eaten.add(victim);
            r.causes.put(victim, Cause.BIG_CRAYFISH);
        }
    }

    @Nullable
    private static Critter pick(List<Critter> list, java.util.function.ToDoubleFunction<Critter> weight, RandomGenerator rnd) {
        double total = 0;
        for (Critter c : list) total += weight.applyAsDouble(c);
        if (total <= 0) return null;
        double x = rnd.nextDouble() * total;
        for (Critter c : list) {
            x -= weight.applyAsDouble(c);
            if (x < 0) return c;
        }
        return null;
    }

    /** M25: death rate per day of water this far out of the pH / TDS range (from {@link org.overengineer.bettacraft.chem.ChemTolerance#BAD_FROM}). */
    public static double chemRate(double distance) {
        return distance < org.overengineer.bettacraft.chem.ChemTolerance.BAD_FROM ? 0 : 0.1 + 0.3 * (distance - org.overengineer.bettacraft.chem.ChemTolerance.BAD_FROM);
    }

    /** Whether conditions allow a brood: fed, water not bad, temperature in the range (M25: and pH / TDS). */
    public static double fitness(Critter c, Env env) {
        if (c.hunger >= 1) return 0;
        double cd = env.chemDistance(c.species);
        if (cd >= org.overengineer.bettacraft.chem.ChemTolerance.BAD_FROM) return 0;
        if (cd > 1e-6) return Math.min(0.5, fitnessBase(c, env));
        return fitnessBase(c, env);
    }

    private static double fitnessBase(Critter c, Env env) {
        float t = env.temp();
        if (t < c.species.minTemp() + 1 || t > c.species.maxTemp() - 1) return 0;
        if (env.qualityEnabled() && env.quality() < c.species.badWater() + 0.1F) return 0.2;
        return 1;
    }

    private static void breed(List<Critter> list, List<CritterObject> objects, Env env, double days, RandomGenerator rnd, IntSupplier ids, Result r) {
        int alive = alive(list);
        List<Critter> snapshot = new ArrayList<>(list);
        for (Critter c : snapshot) {
            if (!c.alive() || !c.adult() || !c.species.breeds()) continue;
            CritterSpecies s = c.species;
            double rate = s.breedRate() * env.breed(s) * fitness(c, env);
            switch (s.breeding()) {
                case PAIR_BERRIED, PAIR_CLUTCH, PAIR -> {
                    if (!c.female) continue;
                    if (c.berried()) {
                        if (s.breeding() == CritterSpecies.Breeding.PAIR_BERRIED && c.eggs >= s.eggDays()) {
                            int n = s.broodMin() + rnd.nextInt(s.broodMax() - s.broodMin() + 1);
                            n = Math.min(n, Math.max(0, env.cap() - alive));
                            for (int i = 0; i < n; i++) {
                                list.add(young(c, c.father != null ? c.father : c.genes, ids, rnd, r));
                                alive++;
                            }
                            c.eggs = -1;
                            c.father = null;
                        } else if (s.breeding() == CritterSpecies.Breeding.PAIR_CLUTCH && (c.eggs >= 2 * s.eggDays() || !env.air())) {
                            c.eggs = -1; // no place to lay above the water: she gives up
                            c.father = null;
                        }
                        continue;
                    }
                    if (alive >= env.cap() || rate <= 0) continue;
                    if (s.breeding() == CritterSpecies.Breeding.PAIR_CLUTCH && !env.air()) continue;
                    Critter male = mate(list, c, rnd);
                    if (male == null || rnd.nextDouble() >= 1 - Math.exp(-rate * days)) continue;
                    if (s.breeding() == CritterSpecies.Breeding.PAIR) {
                        list.add(young(c, male.genes, ids, rnd, r));
                        alive++;
                    } else {
                        c.eggs = 0;
                        c.father = male.genes;
                        r.berried++;
                    }
                }
                case PARTHENO -> { // M29 part d: no male needed, the young are clones of their mother
                    if (c.berried()) {
                        if (c.eggs >= s.eggDays()) {
                            int n = s.broodMin() + rnd.nextInt(s.broodMax() - s.broodMin() + 1);
                            n = Math.min(n, Math.max(0, env.cap() - alive));
                            for (int i = 0; i < n; i++) {
                                list.add(young(c, c.genes, ids, rnd, r));
                                alive++;
                            }
                            c.eggs = -1;
                            c.father = null;
                        }
                        continue;
                    }
                    if (alive >= env.cap() || rate <= 0 || rnd.nextDouble() >= 1 - Math.exp(-rate * days)) continue;
                    c.eggs = 0;
                    r.berried++;
                }
                case SELF -> {
                    if (alive >= env.cap() || rate <= 0) continue;
                    if (rnd.nextDouble() >= 1 - Math.exp(-rate * days)) continue;
                    Critter other = mate(list, c, rnd);
                    int n = s.broodMin() + rnd.nextInt(s.broodMax() - s.broodMin() + 1);
                    n = Math.min(n, Math.max(0, env.cap() - alive));
                    for (int i = 0; i < n; i++) {
                        list.add(young(c, other != null ? other.genes : c.genes, ids, rnd, r));
                        alive++;
                    }
                }
                default -> {}
            }
        }
    }

    /** A partner for {@code c}: an adult of the same species (the other sex, any for hermaphrodites), or null. */
    @Nullable
    public static Critter mate(List<Critter> list, Critter c, RandomGenerator rnd) {
        // M17: reservoir sampling, no list per female and step
        Critter pick = null;
        int n = 0;
        for (Critter o : list) {
            if (o == c || !o.alive() || !o.adult() || o.species != c.species) continue;
            if (c.species.hasSexes() && o.female == c.female) continue;
            if (rnd.nextInt(++n) == 0) pick = o;
        }
        return pick;
    }

    private static Critter young(Critter mother, CritterGenes father, IntSupplier ids, RandomGenerator rnd, Result r) {
        CritterGenes g = CritterGenes.cross(mother.genes, father, mother.species.loci().length, rnd);
        Critter k = new Critter(ids.getAsInt(), mother.species, mother.species.hasSexes() ? rnd.nextBoolean() : false, g,
                mother.x + (rnd.nextDouble() - 0.5) * 0.15, mother.y, mother.z + (rnd.nextDouble() - 0.5) * 0.15);
        k.age = 0;
        k.generation = mother.generation + 1;
        k.yaw = (float) (rnd.nextDouble() * 360);
        k.look = CritterLooks.random(k.species, rnd); // M21.5
        r.born(k);
        return k;
    }

    /** An apple snail lays her clutch where she is (on the glass above the water line). */
    public static CritterObject layClutch(Critter c, int id, RandomGenerator rnd) {
        CritterObject o = new CritterObject(id, CritterObject.Kind.CLUTCH, c.x, c.y, c.z);
        o.face = c.face;
        o.mother = c.genes;
        o.father = c.father != null ? c.father : c.genes;
        o.count = c.species.broodMin() + rnd.nextInt(c.species.broodMax() - c.species.broodMin() + 1);
        o.generation = c.generation + 1;
        o.species = c.species;
        o.yaw = (float) (rnd.nextDouble() * 360);
        c.eggs = -1;
        c.father = null;
        return o;
    }

    /** A moult left where the crayfish is. */
    public static CritterObject molt(Critter c, int id) {
        CritterObject o = new CritterObject(id, CritterObject.Kind.MOLT, c.x, c.y, c.z);
        o.species = c.species;
        o.size = (float) c.size();
        o.yaw = c.yaw;
        return o;
    }
}
