package org.overengineer.bettacraft.tankmates;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.BettaCare;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateCare;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankBreeding;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.tank.TankWaterVolume;

/**
 * M19: what the tankmates of a tank do together, run by the controller with the care step (like {@link TankBreeding}
 * for the bettas):
 * <ul>
 *     <li><b>Shoals</b>: a shoaling fish in a group smaller than its species' minimum is stressed.</li>
 *     <li><b>Bettas and rivals</b>: male bettas chase male guppies (long colourful fins) and male dwarf gouramis (labyrinth
 *     fish rivals; they fight back), gourami males fight each other, pleco males without a cave of their own are chased
 *     off; the clown loaches eat snails; otocinclus and bristlenoses graze algae.</li>
 *     <li><b>Fry and eggs</b>: adult bettas and tankmates eat the fry and the eggs they can reach (plants, moss and the
 *     floating plants hide the fry, a spawning mop or dense plants the eggs).</li>
 *     <li><b>Breeding</b> by species: corydoras glue eggs on the glass after a cool water change, otocinclus now and then;
 *     plecos spawn in a spawning cave, the male guards; tetras, rasboras and kuhli loaches scatter eggs into fine-leaved
 *     plants or a spawning mop (harlequins under broad leaves); guppies, endlers and platys bear live young (into the
 *     breeding box if there is one); the dwarf gourami builds a foam nest and guards it. Clown loaches do not breed.</li>
 * </ul>
 * Eggs, larvae and boxed young are {@link Clutch} data; free-swimming young are fry entities.
 */
public final class TankmateBreeding {
    static final int HOLD = 60;
    /** A cool water change makes the corydoras spawn for this long. */
    public static final double COOL_WINDOW_DAYS = 1.0;

    private TankmateBreeding() {}

    // ------------------------------------------------------------------ config

    static double speed() {
        return ServerConfig.getOr(ServerConfig.TANKMATES_BREEDING_SPEED, 1.0);
    }

    public static double gestationDays() {
        return ServerConfig.getOr(ServerConfig.TANKMATES_GESTATION_DAYS, 3.0);
    }

    static int maxFry() {
        return ServerConfig.getOr(ServerConfig.TANKMATES_MAX_FRY, 20);
    }

    public static int boxCapacity() {
        return ServerConfig.getOr(ServerConfig.TANKMATES_BOX_CAPACITY, 20);
    }

    // ------------------------------------------------------------------ helpers

    public static List<TankmateEntity> tankmates(List<Entity> live) {
        List<TankmateEntity> out = new ArrayList<>();
        for (Entity e : live) if (e instanceof TankmateEntity m && m.isAlive()) out.add(m);
        out.sort(Comparator.comparing(Entity::getUUID));
        return out;
    }

    public static List<TankmateEntity> tankmates(ServerLevel level, TankData t) {
        return tankmates(TankInhabitants.liveInhabitants(level, t));
    }

    /** Healthy, fed adult at half health or more. */
    public static boolean fit(TankmateEntity m) {
        return m.isAlive() && m.isAdult() && !m.health().isSick() && !m.health().isHungry(BettaCare.params()) && m.getHealth() >= m.getMaxHealth() * 0.5F;
    }

    private static Map<TankmateSpecies, List<TankmateEntity>> bySpecies(List<TankmateEntity> mates) {
        Map<TankmateSpecies, List<TankmateEntity>> m = new EnumMap<>(TankmateSpecies.class);
        for (TankmateEntity e : mates) m.computeIfAbsent(e.species(), k -> new ArrayList<>()).add(e);
        return m;
    }

    private static double chance(double ratePerDay, double days) {
        return 1 - Math.exp(-Math.max(0, ratePerDay) * days);
    }

    /** The tank is at its stocking limit: no new spawns (like the small animals, M14). */
    public static boolean full(ServerLevel level, TankBlockEntity c, List<Entity> live) {
        TankData t = c.getTank();
        return TankInhabitants.stocking(live, t) >= TankInhabitants.hardCap(t.waterBlocks()) - 1e-6
                || live.size() >= ServerConfig.getOr(ServerConfig.TANK_MAX_ENTITIES, 64);
    }

    // ------------------------------------------------------------------ the step

    /** One step of {@code days} (also used by GameTests with longer steps). */
    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        if (days <= 0) return;
        TankData t = c.getTank();
        List<Entity> live = TankInhabitants.liveInhabitants(level, t);
        List<TankmateEntity> mates = tankmates(live);
        if (mates.isEmpty() && t.clutches().isEmpty()) {
            t.setFishGrazing(0);
            return;
        }
        long now = level.getGameTime();
        List<BettaEntity> bettas = new ArrayList<>();
        for (Entity e : live) if (e instanceof BettaEntity b && b.isAlive()) bettas.add(b);
        Map<TankmateSpecies, List<TankmateEntity>> by = bySpecies(mates);
        shoals(level, c, by);
        aggression(level, c, bettas, mates, days, now);
        grazing(t, mates);
        snails(level, c, mates, days);
        fry(level, c, bettas, mates, days);
        boolean full = full(level, c, live);
        caves(level, c, mates, now);
        for (var e : by.entrySet()) {
            switch (e.getKey().breeding()) {
                case GLASS_EGGS -> glassEggs(level, c, e.getKey(), e.getValue(), days, full);
                case CAVE, ANEMONE -> cave(level, c, e.getKey(), e.getValue(), days, now, full); // M20: clownfish at their anemone
                case SCATTER -> scatter(level, c, e.getKey(), e.getValue(), days, full);
                case LIVEBEARER -> livebearer(level, c, e.getKey(), e.getValue(), by, days, full);
                case FOAM_NEST -> foam(level, c, e.getKey(), e.getValue(), days, now, full);
                case NONE -> {}
            }
        }
        clutches(level, c, bettas, mates, days, now);
        births(level, c, mates, now);
    }

    // ------------------------------------------------------------------ shoals, rivals, algae, snails

    private static void shoals(ServerLevel level, TankBlockEntity c, Map<TankmateSpecies, List<TankmateEntity>> by) {
        for (var e : by.entrySet()) {
            TankmateSpecies s = e.getKey();
            int n = 0;
            for (TankmateEntity m : e.getValue()) if (m.lifeStage().ordinal() >= LifeStage.JUVENILE.ordinal()) n++;
            boolean small = s.shoaling() && n < s.shoal();
            for (TankmateEntity m : e.getValue()) {
                m.setShoal(n, small && m.lifeStage() != LifeStage.FRY);
            }
            String key = "shoal:" + s.id();
            if (small && n > 0 && c.tankmateLogged.add(key)) {
                TankCare.log(level, c, "log.bettacraft.tm.shoal_small", "@" + s.translationKey(), String.valueOf(n), String.valueOf(s.shoal()));
            } else if (!small) {
                c.tankmateLogged.remove(key);
            }
        }
    }

    private static void aggression(ServerLevel level, TankBlockEntity c, List<BettaEntity> bettas, List<TankmateEntity> mates, double days, long now) {
        for (TankmateEntity m : mates) {
            m.setChased(false);
            m.setGuarding(false);
        }
        double rate = ServerConfig.getOr(ServerConfig.TANKMATES_BETTA_AGGRESSION, 1.0);
        List<BettaEntity> males = new ArrayList<>();
        for (BettaEntity b : bettas) if (b.isAdult() && !b.isFemale()) males.add(b);
        boolean chased = false;
        if (rate > 0 && !males.isEmpty()) {
            for (TankmateEntity m : mates) {
                if (!m.species().bettaTarget() || m.isFemale() || !m.isAdult()) continue;
                BettaEntity b = nearest(m, males);
                if (b == null) continue;
                b.pursue(m, now + HOLD);
                b.flare(now + HOLD);
                m.flee(b, now + HOLD);
                m.setChased(true);
                m.addDamage(1.0 * rate * days, b);
                // a gourami fights back
                if (m.species().group() == TankmateSpecies.Group.GOURAMI) b.addDamage(0.5 * rate * days, m);
                chased = true;
                if (!c.tankmateChaseLogged) {
                    c.tankmateChaseLogged = true;
                    TankCare.log(level, c, "log.bettacraft.tm.betta_chases", TankCare.nameArg(b), TankCare.nameArg(m));
                }
            }
        }
        if (!chased) c.tankmateChaseLogged = false;
        // gourami males fight each other
        List<TankmateEntity> gouramis = new ArrayList<>();
        for (TankmateEntity m : mates) if (m.species().group() == TankmateSpecies.Group.GOURAMI && m.isAdult() && !m.isFemale()) gouramis.add(m);
        if (gouramis.size() >= 2) {
            for (TankmateEntity g : gouramis) {
                TankmateEntity rival = nearest(g, gouramis);
                if (rival == null) continue;
                g.setChased(true);
                g.addDamage(0.6 * days, rival);
            }
            if (c.tankmateLogged.add("gourami_fight")) {
                TankCare.log(level, c, "log.bettacraft.tm.gourami_fight", TankCare.nameArg(gouramis.get(0)), TankCare.nameArg(gouramis.get(1)));
            }
        } else {
            c.tankmateLogged.remove("gourami_fight");
        }
    }

    @Nullable
    private static <T extends Entity> T nearest(Entity from, List<T> list) {
        T best = null;
        double bd = Double.MAX_VALUE;
        for (T e : list) {
            if (e == from || !e.isAlive()) continue;
            double d = e.distanceToSqr(from);
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        return best;
    }

    private static void grazing(TankData t, List<TankmateEntity> mates) {
        double g = 0;
        for (TankmateEntity m : mates) g += m.species().algae() * (m.isAdult() ? 1.0 : 0.4);
        t.setFishGrazing(g * ServerConfig.getOr(ServerConfig.ALGAE_CLEANER_STRENGTH, 1.0));
    }

    /** Clown loaches eat snails (not the big apple snails; nerites have hard shells). */
    private static void snails(ServerLevel level, TankBlockEntity c, List<TankmateEntity> mates, double days) {
        double rate = ServerConfig.getOr(ServerConfig.TANKMATES_LOACH_SNAILS, 0.5);
        int loaches = 0;
        for (TankmateEntity m : mates) if (m.species().eatsSnails() && m.lifeStage().ordinal() >= LifeStage.SUBADULT.ordinal()) loaches++;
        if (loaches == 0 || rate <= 0) {
            c.tankmateSnailLogged = false;
            return;
        }
        TankData t = c.getTank();
        RandomSource r = level.getRandom();
        double expected = rate * loaches * days;
        int n = (int) Math.floor(expected) + (r.nextDouble() < expected - Math.floor(expected) ? 1 : 0);
        int eaten = 0;
        for (int i = 0; i < n; i++) {
            List<Critter> prey = new ArrayList<>();
            for (Critter k : t.critters()) {
                if (!k.alive() || k.species.group() != CritterSpecies.Group.SNAIL) continue;
                if (k.species == CritterSpecies.APPLE_SNAIL && k.adult()) continue;
                prey.add(k);
            }
            if (prey.isEmpty()) break;
            Critter k = prey.get(r.nextInt(prey.size()));
            boolean nerite = k.species.model() == CritterSpecies.Model.NERITE;
            if (nerite && r.nextInt(3) != 0) continue;
            t.critters().remove(k);
            eaten++;
        }
        if (eaten > 0) {
            c.markTankChanged();
            if (!c.tankmateSnailLogged) {
                c.tankmateSnailLogged = true;
                TankCare.log(level, c, "log.bettacraft.tm.ate_snails", "@" + TankmateSpecies.CLOWN_LOACH.translationKey(), String.valueOf(eaten));
            }
        }
    }

    // ------------------------------------------------------------------ fry eaten

    /** Fry of the tankmates (and of the bettas) are hunted by every adult that is bigger. */
    private static void fry(ServerLevel level, TankBlockEntity c, List<BettaEntity> bettas, List<TankmateEntity> mates, double days) {
        double rate = ServerConfig.getOr(ServerConfig.TANKMATES_FRY_EATEN, 0.5);
        if (rate <= 0) return;
        double bettaHunt = 0, mateHunt = 0;
        for (BettaEntity b : bettas) if (b.isAdult()) bettaHunt += 1.0;
        for (TankmateEntity m : mates) if (m.isAdult()) mateHunt += m.species().eatsFry();
        if (bettaHunt + mateHunt <= 0) return;
        double hide = TankCritters.hiding(level, c.getTank());
        RandomSource r = level.getRandom();
        int eaten = 0;
        Entity by = null;
        for (TankmateEntity f : mates) {
            if (f.lifeStage() != LifeStage.FRY) continue;
            // the adults of the young's own kind count too (guppies eat their own fry)
            double p = chance(rate * (bettaHunt + mateHunt) * (1 - hide), days);
            if (r.nextDouble() < p) {
                by = bettaHunt > 0 && (mateHunt <= 0 || r.nextDouble() < bettaHunt / (bettaHunt + mateHunt)) ? bettas.getFirst() : null;
                eat(level, c, f);
                eaten++;
            }
        }
        // betta fry: the bettas' own males are handled by TankBreeding; the tankmates eat them as well
        if (mateHunt > 0) {
            for (BettaEntity b : bettas) {
                if (b.lifeStage() != LifeStage.FRY) continue;
                if (r.nextDouble() < chance(rate * mateHunt * (1 - hide), days)) {
                    eat(level, c, b);
                    eaten++;
                }
            }
        }
        if (eaten > 0) {
            TankCare.log(level, c, by != null ? "log.bettacraft.tm.fry_eaten_betta" : "log.bettacraft.tm.fry_eaten", String.valueOf(eaten));
        }
    }

    private static void eat(ServerLevel level, TankBlockEntity c, Entity prey) {
        level.sendParticles(ParticleTypes.BUBBLE_POP, prey.getX(), prey.getY(), prey.getZ(), 3, 0.05, 0.05, 0.05, 0.01);
        c.getTank().inhabitants().remove(prey.getUUID());
        prey.discard();
        c.markTankChanged();
    }

    // ------------------------------------------------------------------ breeding

    private static List<TankmateEntity> adults(List<TankmateEntity> list, boolean female, boolean fitOnly) {
        List<TankmateEntity> out = new ArrayList<>();
        for (TankmateEntity m : list) if (m.isAdult() && m.isFemale() == female && (!fitOnly || fit(m))) out.add(m);
        return out;
    }

    private static int brood(TankmateSpecies s, TankmateEntity mother, RandomSource r) {
        int n = s.broodMin() + r.nextInt(Math.max(1, s.broodMax() - s.broodMin() + 1));
        if (mother.breeding().isConditioned()) n = Math.round(n * 1.3F);
        return Math.max(1, n);
    }

    /** Corydoras after a cool water change (otocinclus without one, rarely): eggs on the glass, the decoration and leaves. */
    private static void glassEggs(ServerLevel level, TankBlockEntity c, TankmateSpecies s, List<TankmateEntity> list, double days, boolean full) {
        if (full) return;
        TankData t = c.getTank();
        List<TankmateEntity> males = adults(list, false, true), females = adults(list, true, true);
        if (males.isEmpty() || females.isEmpty() || !s.tolerates(t.waterTemp())) return;
        double rate;
        if (s.group() == TankmateSpecies.Group.CORY) {
            if (!coolChangeRecent(level, t)) return;
            rate = 4.0;
        } else {
            if (!s.breedingTemp(t.waterTemp())) return;
            rate = 0.12;
        }
        RandomSource r = level.getRandom();
        TankWaterVolume w = new TankWaterVolume(t);
        for (TankmateEntity f : females) {
            if (f.breeding().cooldown() > 0) continue;
            double conditioned = f.breeding().isConditioned() ? 1.5 : 1.0;
            if (r.nextDouble() >= chance(rate * conditioned * speed(), days)) continue;
            TankmateEntity m = nearest(f, males);
            if (m == null) continue;
            int total = brood(s, f, r);
            int spots = 2 + r.nextInt(3);
            List<TankmateSpots.Spot> leaves = TankmateSpots.plants(level.getServer(), t, true);
            int made = 0;
            for (int i = 0; i < spots && total > 0; i++) {
                int n = i == spots - 1 ? total : Math.max(1, total / (spots - i) + r.nextInt(3) - 1);
                n = Math.min(n, total);
                total -= n;
                double[] g = new double[4];
                Clutch k;
                if ((leaves.isEmpty() || r.nextInt(3) > 0) && TankmateSpots.glassSpot(t, w, r, g) != null) {
                    k = new Clutch(t.nextClutchId(), s, Clutch.Kind.GLASS, g[0], g[1], g[2], n, f.genes(), m.genes());
                    k.face = (int) g[3];
                } else if (!leaves.isEmpty()) {
                    Vec3 p = leaves.get(r.nextInt(leaves.size())).pos();
                    k = new Clutch(t.nextClutchId(), s, Clutch.Kind.GLASS, p.x, p.y, p.z, n, f.genes(), m.genes());
                    k.cover = 0.3;
                } else {
                    continue;
                }
                k.generation = Math.max(f.generation(), m.generation()) + 1;
                t.clutches().add(k);
                made += n;
            }
            if (made <= 0) continue;
            f.breeding().setCooldown(2.0);
            level.sendParticles(ParticleTypes.BUBBLE, f.getX(), f.getY() + 0.1, f.getZ(), 4, 0.1, 0.05, 0.1, 0.01);
            TankCare.log(level, c, "log.bettacraft.tm.spawned_glass", TankCare.nameArg(f), String.valueOf(made));
            c.markTankChanged();
        }
    }

    /** Within a day after a cool water change. */
    public static boolean coolChangeRecent(ServerLevel level, TankData t) {
        return t.lastCoolChange() >= 0 && level.getGameTime() - t.lastCoolChange() <= COOL_WINDOW_DAYS * 24000;
    }

    /** The adult pleco males (of every cave species) get a cave each, in UUID order; the others are chased off. */
    private static void caves(ServerLevel level, TankBlockEntity c, List<TankmateEntity> mates, long now) {
        List<TankmateEntity> males = new ArrayList<>();
        List<TankmateEntity> clowns = new ArrayList<>();
        for (TankmateEntity m : mates) {
            m.setCave(null, Vec3.ZERO);
            if (m.species().breeding() == TankmateSpecies.Breeding.CAVE && m.isAdult() && !m.isFemale()) males.add(m);
            if (m.species().breeding() == TankmateSpecies.Breeding.ANEMONE) clowns.add(m);
        }
        if (!clowns.isEmpty()) anemones(level, c, clowns);
        if (males.isEmpty()) return;
        List<TankmateSpots.Spot> caves = TankmateSpots.caves(level.getServer(), c.getTank());
        // a male guarding a clutch keeps his cave
        List<Vec3> taken = new ArrayList<>();
        for (Clutch k : c.getTank().clutches()) {
            if (k.kind != Clutch.Kind.CAVE || k.guardian == null) continue;
            for (TankmateEntity m : males) if (m.getUUID().equals(k.guardian)) {
                Vec3 dir = Vec3.ZERO;
                for (var sp : caves) if (sp.pos().distanceToSqr(k.pos()) < 1e-4) dir = sp.dir();
                m.setCave(k.pos(), dir);
                taken.add(k.pos());
            }
        }
        int i = 0;
        for (TankmateEntity m : males) {
            if (m.cave() != null) continue;
            while (i < caves.size() && taken.contains(caves.get(i).pos())) i++;
            if (i < caves.size()) {
                var sp = caves.get(i++);
                m.setCave(sp.pos(), sp.dir());
            } else if (!caves.isEmpty() && males.size() > 1) {
                m.setChased(true); // no cave left for him: the cave owners chase him off
            }
        }
    }

    /**
     * M20: clownfish move into the host anemones: the pair (the biggest female and male) of each anemone lives in it,
     * the young as well; adults without an anemone of their own are chased off by the owners.
     */
    private static void anemones(ServerLevel level, TankBlockEntity c, List<TankmateEntity> clowns) {
        List<TankmateSpots.Spot> spots = TankmateSpots.anemones(level.getServer(), c.getTank());
        if (spots.isEmpty()) return;
        // the males guarding eggs keep their anemone
        List<Vec3> homes = new ArrayList<>();
        for (TankmateSpots.Spot sp : spots) homes.add(sp.pos());
        int[] females = new int[homes.size()], males = new int[homes.size()];
        clowns.sort(java.util.Comparator.comparing((TankmateEntity m) -> !m.isAdult()).thenComparingInt(Entity::getId));
        for (TankmateEntity m : clowns) {
            int best = -1;
            for (int i = 0; i < homes.size(); i++) {
                if (m.isAdult() && (m.isFemale() ? females[i] : males[i]) > 0) continue;
                if (best < 0 || m.distanceToSqr(homes.get(i)) < m.distanceToSqr(homes.get(best))) best = i;
            }
            if (best < 0) {
                if (m.isAdult()) m.setChased(true);
                continue;
            }
            if (m.isAdult()) {
                if (m.isFemale()) females[best]++;
                else males[best]++;
            }
            m.setCave(homes.get(best), Vec3.ZERO);
        }
    }

    private static void cave(ServerLevel level, TankBlockEntity c, TankmateSpecies s, List<TankmateEntity> list, double days, long now, boolean full) {
        TankData t = c.getTank();
        List<TankmateEntity> females = adults(list, true, true);
        RandomSource r = level.getRandom();
        boolean temp = s.breedingTemp(t.waterTemp());
        for (TankmateEntity m : adults(list, false, false)) {
            Vec3 cave = m.cave();
            if (cave == null) continue;
            boolean guarding = false;
            for (Clutch k : t.clutches()) if (k.kind == Clutch.Kind.CAVE && m.getUUID().equals(k.guardian)) guarding = true;
            if (guarding) {
                m.holdAt(cave, now + HOLD);
                m.setGuarding(true);
                continue;
            }
            // a male with a cave sits in it most of the time
            if (((now / 1200) + m.getId()) % 5 < 3) m.holdAt(cave, now + HOLD);
            if (full || !fit(m) || !temp || females.isEmpty()) continue;
            m.breeding().addReady(days * speed() * (m.breeding().isConditioned() ? 1.5 : 1.0));
            if (m.breeding().ready() < 1.0) continue;
            TankmateEntity f = null;
            for (TankmateEntity x : females) if (x.breeding().cooldown() <= 0 && (f == null || x.distanceToSqr(cave) < f.distanceToSqr(cave))) f = x;
            if (f == null) continue;
            boolean anemone = s.breeding() == TankmateSpecies.Breeding.ANEMONE;
            // M20: a clownfish female spawns only in her own anemone (the pair shares it)
            if (anemone && (f.cave() == null || f.cave().distanceToSqr(cave) > 1e-4)) continue;
            f.holdAt(cave, now + HOLD);
            m.holdAt(cave, now + HOLD);
            m.breeding().resetReady();
            int n = brood(s, f, r);
            Vec3 at = anemone ? TankmateSpots.anemoneFoot(level.getServer(), t, cave) : cave;
            Clutch k = new Clutch(t.nextClutchId(), s, Clutch.Kind.CAVE, at.x, at.y, at.z, n, f.genes(), m.genes());
            k.guardian = m.getUUID();
            k.generation = Math.max(f.generation(), m.generation()) + 1;
            t.clutches().add(k);
            f.breeding().setCooldown(3.0);
            TankCare.log(level, c, anemone ? "log.bettacraft.tm.spawned_anemone" : "log.bettacraft.tm.spawned_cave", TankCare.nameArg(f),
                    TankCare.nameArg(m), String.valueOf(n));
            c.markTankChanged();
        }
    }

    /** Egg scatterers: into fine-leaved plants, moss or a spawning mop (harlequins: under broad leaves). */
    private static void scatter(ServerLevel level, TankBlockEntity c, TankmateSpecies s, List<TankmateEntity> list, double days, boolean full) {
        if (full) return;
        TankData t = c.getTank();
        if (!s.breedingTemp(t.waterTemp())) return;
        List<TankmateEntity> males = adults(list, false, true), females = adults(list, true, true);
        if (males.isEmpty() || females.isEmpty()) return;
        RandomSource r = level.getRandom();
        for (TankmateEntity f : females) {
            if (f.breeding().cooldown() > 0) continue;
            double cond = f.breeding().isConditioned() ? 2.0 : 1.0;
            if (r.nextDouble() >= chance(0.6 * cond * speed(), days)) continue;
            TankmateEntity m = nearest(f, males);
            if (m == null) continue;
            int n = brood(s, f, r);
            List<TankmateSpots.Spot> mops = TankmateSpots.mops(level.getServer(), t);
            List<TankmateSpots.Spot> plants = TankmateSpots.plants(level.getServer(), t, s.broadLeafSpawner());
            double share = TankmateSpots.plantShare(level.getServer(), t, s.broadLeafSpawner());
            int spots = 1 + r.nextInt(3);
            int made = 0;
            for (int i = 0; i < spots && n > 0; i++) {
                int k = i == spots - 1 ? n : Math.max(1, n / (spots - i));
                n -= k;
                Vec3 p;
                double cover;
                if (!mops.isEmpty() && (plants.isEmpty() || r.nextInt(3) > 0)) {
                    p = mops.get(r.nextInt(mops.size())).pos();
                    cover = 0.8;
                } else if (!plants.isEmpty()) {
                    p = plants.get(r.nextInt(plants.size())).pos();
                    cover = Math.min(0.7, 0.25 + share * 6);
                } else {
                    // nothing to spawn into: the eggs fall onto the bare ground, where the adults find almost all of them
                    p = new Vec3(f.getX(), new TankWaterVolume(t).box().minY + 0.02, f.getZ());
                    cover = 0.02;
                }
                Clutch cl = new Clutch(t.nextClutchId(), s, Clutch.Kind.SCATTER, p.x + (r.nextDouble() - 0.5) * 0.1, p.y, p.z + (r.nextDouble() - 0.5) * 0.1,
                        k, f.genes(), m.genes());
                cl.cover = cover;
                cl.generation = Math.max(f.generation(), m.generation()) + 1;
                t.clutches().add(cl);
                made += k;
            }
            f.breeding().setCooldown(2.0);
            boolean bare = mops.isEmpty() && plants.isEmpty();
            TankCare.log(level, c, bare ? "log.bettacraft.tm.spawned_bare" : "log.bettacraft.tm.spawned_scatter", TankCare.nameArg(f), String.valueOf(made));
            c.markTankChanged();
        }
    }

    /**
     * Livebearers: a female with a male becomes pregnant; the young come after {@link #gestationDays}. M21.6: with the
     * livebearer genetics a guppy female also takes an endler male and the other way round (real: they cross freely),
     * her own kind three times as likely; the young are hybrids of her species.
     */
    private static void livebearer(ServerLevel level, TankBlockEntity c, TankmateSpecies s, List<TankmateEntity> list,
                                   Map<TankmateSpecies, List<TankmateEntity>> by, double days, boolean full) {
        if (full) return;
        TankData t = c.getTank();
        if (!s.breedingTemp(t.waterTemp())) return;
        List<TankmateEntity> males = new ArrayList<>();
        for (TankmateEntity m : adults(list, false, false)) for (int i = 0; i < 3; i++) males.add(m);
        TankmateSpecies other = hybridPartner(s);
        if (other != null && by.containsKey(other)) males.addAll(adults(by.get(other), false, false));
        if (males.isEmpty()) return;
        RandomSource r = level.getRandom();
        for (TankmateEntity f : adults(list, true, true)) {
            if (f.isGravid() || f.breeding().cooldown() > 0) continue;
            if (r.nextDouble() >= chance(0.8 * speed(), days)) continue;
            TankmateEntity m = males.get(r.nextInt(males.size()));
            f.setGravid(0, m.genes());
            if (m.species() == s) TankCare.log(level, c, "log.bettacraft.tm.gravid", TankCare.nameArg(f));
            else TankCare.log(level, c, "log.bettacraft.tm.gravid_hybrid", TankCare.nameArg(f), TankCare.nameArg(m));
        }
    }

    /** M21.6: the species a guppy or an endler crosses with (with features.livebearerGenetics), or null. */
    @Nullable
    public static TankmateSpecies hybridPartner(TankmateSpecies s) {
        if (!org.overengineer.bettacraft.config.CommonConfig.livebearerGenetics()) return null;
        return s == TankmateSpecies.GUPPY ? TankmateSpecies.ENDLER_GUPPY : s == TankmateSpecies.ENDLER_GUPPY ? TankmateSpecies.GUPPY : null;
    }

    /** The dwarf gourami: a foam nest like the bettas', the male guards it and chases the female away after spawning. */
    private static void foam(ServerLevel level, TankBlockEntity c, TankmateSpecies s, List<TankmateEntity> list, double days, long now, boolean full) {
        TankData t = c.getTank();
        RandomSource r = level.getRandom();
        Clutch nest = null;
        for (Clutch k : t.clutches()) if (k.kind == Clutch.Kind.FOAM && k.species == s) nest = k;
        List<TankmateEntity> females = adults(list, true, true);
        boolean temp = s.breedingTemp(t.waterTemp());
        if (nest == null) {
            if (full || !temp || females.isEmpty()) return;
            TankmateEntity male = null;
            for (TankmateEntity m : adults(list, false, true)) if (male == null) male = m;
            if (male == null) return;
            double plants = org.overengineer.bettacraft.tank.plants.TankPlants.breedingBonus(t);
            male.breeding().addReady(days * speed() * plants * (male.breeding().isConditioned() ? 2.0 : 1.0));
            if (male.breeding().ready() < ServerConfig.getOr(ServerConfig.BREEDING_READY_DAYS, 0.25)) return;
            male.breeding().resetReady();
            Vec3 p = TankBreeding.nestSpot(level, t, male.position());
            if (t.nest() != null && t.nest().pos().distanceTo(p) < 0.6) p = new TankWaterVolume(t).surfacePoint(p.add(0.8, 0, 0.8), 0.35, 0.06);
            Clutch k = new Clutch(t.nextClutchId(), s, Clutch.Kind.FOAM, p.x, p.y, p.z, 0, TankmateGenes.wild(s), male.genes());
            k.guardian = male.getUUID();
            k.building = true;
            t.clutches().add(k);
            TankCare.log(level, c, "log.bettacraft.nest_started", TankCare.nameArg(male));
            c.markTankChanged();
            return;
        }
        TankmateEntity male = null;
        for (TankmateEntity m : list) if (m.getUUID().equals(nest.guardian)) male = m;
        if (male == null) return; // clutches(): unguarded
        male.holdAt(nest.pos().add(0, -0.15, 0), now + HOLD);
        male.setGuarding(true);
        if (nest.building) {
            if (fit(male) && temp) {
                nest.progress += days;
                if (r.nextInt(3) == 0) level.sendParticles(ParticleTypes.BUBBLE, nest.x, nest.y - 0.1, nest.z, 2, 0.12, 0.04, 0.12, 0.01);
            }
            if (nest.progress >= ServerConfig.getOr(ServerConfig.BREEDING_NEST_BUILD_DAYS, 0.25)) {
                nest.building = false;
                nest.progress = 0;
                TankCare.log(level, c, "log.bettacraft.nest_built", TankCare.nameArg(male));
                c.markTankChanged();
            }
            return;
        }
        if (nest.count <= 0) {
            TankmateEntity f = null;
            for (TankmateEntity x : females) if (x.breeding().cooldown() <= 0 && (f == null || x.distanceToSqr(nest.pos()) < f.distanceToSqr(nest.pos()))) f = x;
            if (f != null && temp && !full) {
                f.holdAt(nest.pos().add(0, -0.15, 0), now + HOLD);
                nest.unguarded = 0;
                nest.progress += days;
                if (nest.progress >= 0.02) {
                    nest.count = brood(s, f, r);
                    nest.mother = f.genes();
                    nest.father = male.genes();
                    nest.generation = Math.max(f.generation(), male.generation()) + 1;
                    nest.age = 0;
                    nest.progress = 0;
                    f.breeding().setCooldown(3.0);
                    level.sendParticles(ParticleTypes.BUBBLE_POP, nest.x, nest.y, nest.z, 10, 0.2, 0.05, 0.2, 0.01);
                    TankCare.log(level, c, "log.bettacraft.spawned", TankCare.nameArg(f), TankCare.nameArg(male), String.valueOf(nest.count));
                    c.markTankChanged();
                }
            } else {
                nest.unguarded += days; // waiting for a female; the nest falls apart after a few days
                if (nest.unguarded >= 3.0) {
                    t.clutches().remove(nest);
                    TankCare.log(level, c, "log.bettacraft.nest_collapsed");
                    c.markTankChanged();
                }
            }
            return;
        }
        // eggs in the nest: the male chases the females
        double hide = org.overengineer.bettacraft.plants.InteriorPlants.hiding(level.getServer(), t);
        for (TankmateEntity f : adults(list, true, false)) {
            if (hide > 0 && r.nextDouble() < hide) continue;
            f.flee(male, now + HOLD);
            f.setChased(true);
            f.addDamage(1.0 * days, male);
        }
    }

    // ------------------------------------------------------------------ clutches

    private static void clutches(ServerLevel level, TankBlockEntity c, List<BettaEntity> bettas, List<TankmateEntity> mates, double days, long now) {
        TankData t = c.getTank();
        if (t.clutches().isEmpty()) return;
        RandomSource r = level.getRandom();
        double eggRate = ServerConfig.getOr(ServerConfig.TANKMATES_EGGS_EATEN, 1.5);
        double hunters = 0;
        for (BettaEntity b : bettas) if (b.isAdult()) hunters += 1.0;
        for (TankmateEntity m : mates) if (m.isAdult()) hunters += m.species().eatsFry();
        boolean bad = TankWater.isBad(t);
        float temp = t.waterTemp();
        boolean changed = false;
        for (Clutch k : new ArrayList<>(t.clutches())) {
            if (k.kind == Clutch.Kind.FOAM && (k.building || k.count <= 0)) {
                if (guardianGone(level, mates, k)) {
                    t.clutches().remove(k);
                    TankCare.log(level, c, "log.bettacraft.nest_abandoned");
                    changed = true;
                }
                continue;
            }
            int before = k.count, stage = k.stage();
            if (k.species.tolerates(temp)) k.age += days;
            boolean guarded = true;
            if (k.kind == Clutch.Kind.CAVE || k.kind == Clutch.Kind.FOAM) {
                if (guardianGone(level, mates, k)) {
                    guarded = false;
                    k.unguarded += days;
                    if (k.unguarded >= 0.5) {
                        t.clutches().remove(k);
                        TankCare.log(level, c, "log.bettacraft.tm.clutch_died", "@" + k.species.translationKey(), String.valueOf(k.count));
                        changed = true;
                        continue;
                    }
                } else {
                    k.unguarded = 0;
                }
            }
            if (k.kind != Clutch.Kind.BOX) {
                // the adults eat what they reach (a guarded cave or nest is safe)
                boolean exposed = k.kind == Clutch.Kind.GLASS || k.kind == Clutch.Kind.SCATTER || !guarded;
                if (exposed && hunters > 0 && eggRate > 0) k.count -= binomial(r, k.count, chance(eggRate * hunters * (1 - k.cover), days));
                // eggs grow fungus in bad water
                if (bad && k.stage() == 0) k.count -= binomial(r, k.count, chance(0.3, days));
            }
            if (k.count <= 0) {
                t.clutches().remove(k);
                TankCare.log(level, c, "log.bettacraft.tm.eggs_eaten", "@" + k.species.translationKey());
                changed = true;
                continue;
            }
            if (k.age >= k.totalDays()) {
                release(level, c, k, r);
                changed = true;
                continue;
            }
            if (k.count != before || k.stage() != stage) changed = true;
            if (k.stage() != stage && stage == 0) TankCare.log(level, c, "log.bettacraft.tm.hatched", "@" + k.species.translationKey(), String.valueOf(k.count));
        }
        if (changed) c.markTankChanged();
        else c.setChanged();
    }

    private static boolean guardianGone(ServerLevel level, List<TankmateEntity> mates, Clutch k) {
        if (k.guardian == null) return true;
        for (TankmateEntity m : mates) if (m.getUUID().equals(k.guardian) && m.isAlive()) return false;
        return true;
    }

    static int binomial(RandomSource r, int n, double p) {
        if (n <= 0 || p <= 0) return 0;
        if (p >= 1) return n;
        if (n > 60) return (int) Math.round(Mth.clamp(n * p + r.nextGaussian() * Math.sqrt(n * p * (1 - p)), 0, n));
        int k = 0;
        for (int i = 0; i < n; i++) if (r.nextDouble() < p) k++;
        return k;
    }

    /** The larvae swim free (the box: juveniles leave it): fry entities near the clutch. */
    private static void release(ServerLevel level, TankBlockEntity c, Clutch k, RandomSource r) {
        TankData t = c.getTank();
        t.clutches().remove(k);
        int live = TankInhabitants.liveInhabitants(level, t).size();
        int room = Math.max(0, ServerConfig.getOr(ServerConfig.TANK_MAX_ENTITIES, 64) - live);
        int n = Math.min(Math.min(k.count, maxFry()), room);
        LifeStage stage = k.kind == Clutch.Kind.BOX ? LifeStage.JUVENILE : LifeStage.FRY;
        TankWaterVolume w = new TankWaterVolume(t);
        int made = 0;
        for (int i = 0; i < n; i++) {
            // M21.6: the sex first, the X and Y of the livebearer genome depend on it
            boolean female = r.nextBoolean();
            TankmateGenes g = TankmateGenes.cross(k.mother, k.father, female, r::nextLong);
            Vec3 p = w.clamp(new Vec3(k.x + (r.nextDouble() - 0.5) * 0.3, k.y - 0.05 - r.nextDouble() * 0.15, k.z + (r.nextDouble() - 0.5) * 0.3),
                    0.08 + 1.0 / 16, 0.05 + 1.0 / 16, 0.05);
            if (spawn(level, c, k.species, g, female, stage, k.generation, p) != null) made++;
        }
        TankCare.log(level, c, k.kind == Clutch.Kind.BOX ? "log.bettacraft.tm.box_released" : "log.bettacraft.tm.fry_free",
                "@" + k.species.translationKey(), String.valueOf(made));
    }

    /** Spawns a young tankmate in the tank (offspring, catches, tests). */
    @Nullable
    public static TankmateEntity spawn(ServerLevel level, TankBlockEntity c, TankmateSpecies s, TankmateGenes genes, boolean female, LifeStage stage,
                                       int generation, Vec3 p) {
        TankmateEntity e = ModEntities.TANKMATES.get(s).get().create(level);
        if (e == null) return null;
        e.setup(genes, female, stage, generation);
        e.moveTo(p.x, p.y, p.z, level.getRandom().nextFloat() * 360F, 0F);
        TankInhabitants.adopt(e, c);
        if (!level.addFreshEntity(e)) return null;
        c.getTank().inhabitants().add(e.getUUID());
        return e;
    }

    /** Pregnant livebearers give birth after the gestation: into the breeding box if there is one with room. */
    private static void births(ServerLevel level, TankBlockEntity c, List<TankmateEntity> mates, long now) {
        TankData t = c.getTank();
        RandomSource r = level.getRandom();
        double gest = gestationDays();
        for (TankmateEntity f : mates) {
            if (!f.isGravid() || f.gravidDays() < gest) continue;
            TankmateSpecies s = f.species();
            int n = brood(s, f, r);
            TankmateGenes father = f.gravidFather() != null ? f.gravidFather() : TankmateGenes.wild(s);
            f.setGravid(-1, null);
            f.breeding().setCooldown(0.5);
            int boxed = 0;
            if (t.breedingBox()) {
                int inBox = boxFry(t);
                boxed = Math.min(n, Math.max(0, boxCapacity() - inBox));
                if (boxed > 0) {
                    Vec3 b = boxPos(t);
                    Clutch k = new Clutch(t.nextClutchId(), s, Clutch.Kind.BOX, b.x, b.y, b.z, boxed, f.genes(), father);
                    k.generation = f.generation() + 1;
                    k.cover = 1.0;
                    t.clutches().add(k);
                }
            }
            int free = n - boxed;
            int live = TankInhabitants.liveInhabitants(level, t).size();
            free = Math.min(free, Math.max(0, ServerConfig.getOr(ServerConfig.TANK_MAX_ENTITIES, 64) - live));
            int made = 0;
            TankWaterVolume w = new TankWaterVolume(t);
            for (int i = 0; i < free; i++) {
                Vec3 p = w.clamp(f.position().add((r.nextDouble() - 0.5) * 0.3, 0.05, (r.nextDouble() - 0.5) * 0.3), 0.08 + 1.0 / 16, 0.05 + 1.0 / 16, 0.05);
                boolean female = r.nextBoolean();
                if (spawn(level, c, s, TankmateGenes.cross(f.genes(), father, female, r::nextLong), female, LifeStage.FRY, f.generation() + 1, p) != null) made++;
            }
            TankCare.log(level, c, boxed > 0 ? "log.bettacraft.tm.born_box" : "log.bettacraft.tm.born", TankCare.nameArg(f),
                    String.valueOf(made + boxed), String.valueOf(boxed));
            c.markTankChanged();
        }
    }

    /** Young livebearers in the breeding box. */
    public static int boxFry(TankData t) {
        int n = 0;
        for (Clutch k : t.clutches()) if (k.kind == Clutch.Kind.BOX) n += k.count;
        return n;
    }

    /** Where the breeding box hangs: inside the front glass of the tank's first top block, near the surface. */
    public static Vec3 boxPos(TankData t) {
        TankWaterVolume w = new TankWaterVolume(t);
        var box = w.box();
        double x = (box.minX + box.maxX) / 2, z = (box.minZ + box.maxZ) / 2;
        switch (t.front()) {
            case NORTH -> z = box.minZ + 0.2;
            case SOUTH -> z = box.maxZ - 0.2;
            case WEST -> x = box.minX + 0.2;
            default -> x = box.maxX - 0.2;
        }
        return w.surfacePoint(new Vec3(x, w.surface(), z), 0.2, 0.15);
    }

    // ------------------------------------------------------------------ water changes

    /**
     * Fresh water poured into a tank (M19): when {@code tankmates.waterChangeCools} is on it has the ambient temperature
     * (the tank cools down a little), and a cool water change - {@code tankmates.coolChangeShare} of the volume within a
     * quarter of a day that lowers the temperature by {@code tankmates.coolChangeDrop} - sets off the corydoras.
     */
    public static void onFreshWater(ServerLevel level, TankBlockEntity c, long before, long added) {
        TankData t = c.getTank();
        if (added <= 0 || before <= 0) return;
        long now = level.getGameTime();
        float tempBefore = t.waterTemp();
        if (ServerConfig.getOr(ServerConfig.TANKMATES_WATER_CHANGE_COOLS, true)) {
            float ambient = TankCare.ambient(level, t);
            if (ambient < tempBefore) t.setWaterTemp((float) ((tempBefore * before + ambient * added) / (double) (before + added)));
        }
        boolean fresh = t.freshWindowStart() < 0 || now - t.freshWindowStart() > 6000 || Float.isNaN(t.freshWindowTemp());
        if (fresh) t.setFreshWindow(now, added, tempBefore);
        else t.setFreshWindow(t.freshWindowStart(), t.freshWindowMb() + added, t.freshWindowTemp());
        double share = t.freshWindowMb() / (double) Math.max(1, t.capacityMb());
        double drop = t.freshWindowTemp() - t.waterTemp();
        if (share >= ServerConfig.getOr(ServerConfig.TANKMATES_COOL_CHANGE_SHARE, 0.25) - 1e-6
                && drop >= ServerConfig.getOr(ServerConfig.TANKMATES_COOL_CHANGE_DROP, 1.5) - 1e-4) {
            boolean first = t.lastCoolChange() < t.freshWindowStart();
            t.setLastCoolChange(now);
            if (first) TankCare.log(level, c, "log.bettacraft.tm.cool_change", String.format(java.util.Locale.ROOT, "%.1f", drop));
        }
    }

    // ------------------------------------------------------------------ menu

    /** Breeding tab: per species in the tank, the state of its breeding. */
    public static ListTag describe(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        ListTag out = new ListTag();
        Map<TankmateSpecies, List<TankmateEntity>> by = bySpecies(tankmates(level, t));
        for (Clutch k : t.clutches()) by.computeIfAbsent(k.species, x -> new ArrayList<>());
        for (var e : by.entrySet()) {
            TankmateSpecies s = e.getKey();
            CompoundTag d = new CompoundTag();
            d.putString("species", s.id());
            int males = 0, females = 0, young = 0, gravid = 0;
            boolean fitM = false, fitF = false;
            for (TankmateEntity m : e.getValue()) {
                if (!m.isAdult()) young++;
                else if (m.isFemale()) {
                    females++;
                    fitF |= fit(m);
                    if (m.isGravid()) gravid++;
                } else {
                    males++;
                    fitM |= fit(m);
                }
            }
            d.putInt("males", males);
            d.putInt("females", females);
            d.putInt("young", young);
            d.putBoolean("fit", fitM && fitF);
            d.putBoolean("temp", s.breedingTemp(t.waterTemp()));
            d.putInt("gravid", gravid);
            switch (s.breeding()) {
                case CAVE -> d.putInt("caves", TankmateSpots.caves(level.getServer(), t).size());
                case ANEMONE -> d.putInt("anemones", TankmateSpots.anemones(level.getServer(), t).size());
                case SCATTER -> {
                    d.putInt("mops", TankmateSpots.mops(level.getServer(), t).size());
                    d.putFloat("plants", (float) TankmateSpots.plantShare(level.getServer(), t, s.broadLeafSpawner()));
                }
                case GLASS_EGGS -> d.putBoolean("cool", coolChangeRecent(level, t));
                case LIVEBEARER -> d.putBoolean("box", t.breedingBox());
                default -> {}
            }
            ListTag cl = new ListTag();
            for (Clutch k : t.clutches()) {
                if (k.species != s) continue;
                CompoundTag kc = new CompoundTag();
                kc.putString("kind", k.kind.id());
                kc.putInt("count", k.count);
                kc.putInt("stage", k.stage());
                kc.putBoolean("building", k.building);
                kc.putFloat("days", (float) k.daysLeft());
                cl.add(kc);
            }
            d.put("clutches", cl);
            out.add(d);
        }
        return out;
    }

    /** Warnings for the menu and the overlays: shoals that are too small, temperatures, bettas chasing tankmates. */
    public static List<String> warnings(ServerLevel level, TankBlockEntity c) {
        List<String> out = new ArrayList<>();
        TankData t = c.getTank();
        Map<TankmateSpecies, List<TankmateEntity>> by = bySpecies(tankmates(level, t));
        boolean chased = false;
        for (var e : by.entrySet()) {
            TankmateSpecies s = e.getKey();
            int n = 0;
            for (TankmateEntity m : e.getValue()) {
                if (m.lifeStage().ordinal() >= LifeStage.JUVENILE.ordinal()) n++;
                if ((m.stressBits() & TankmateCare.STRESS_CHASED) != 0) chased = true;
            }
            if (s.shoaling() && n > 0 && n < s.shoal()) out.add("tm_shoal");
            if (!s.tolerates(t.waterTemp())) out.add("tm_temp");
        }
        if (chased) out.add("tm_chased");
        return out.stream().distinct().toList();
    }
}
