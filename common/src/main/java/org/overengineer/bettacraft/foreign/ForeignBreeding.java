package org.overengineer.bettacraft.foreign;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaHealth;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tankmates.TankmateSpots;
import org.overengineer.bettacraft.terrarium.TerrariumNeeds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M32 part a (question A2): breeding of the foreign animals in our tanks.
 * <ul>
 *     <li><b>Ours</b> ({@code features.foreignAnimals.breeding}): two grown, fed, healthy animals of a type with a breeding profile,
 *     in water of their breeding temperature, in a tank with enough water ({@link ForeignProfile.Breeding#minWaterBlocks}) and
 *     plants where the eggs need them, breed now and then ({@link ForeignProfile.Breeding#rate}). A livebearer mother
 *     carries the young, a clutch of eggs hatches, after {@link ForeignProfile.Breeding#days}; then the young join the tank
 *     (as many as it has room for). Every young takes the NBT keys of {@link ForeignProfile.Breeding#inherit} from one of
 *     its parents at random (the mod's own variants) and the {@link ForeignProfile.Breeding#offspringNbt}. The brood lives in
 *     the mother's persistent data ({@link #BROOD}); one brood per type and tank at a time.</li>
 *     <li><b>Theirs suppressed</b> ({@code features.foreignAnimals.suppressOwnBreeding}), in our tanks only: every animal of vanilla or
 *     another mod that lives in a tank loses its love each care step ({@code resetLove}, vanilla animals and mods with the
 *     same method by reflection), cannot be made to fall in love with its food there, a vanilla baby from two parents in a
 *     tank is cancelled ({@link BabyEntitySpawnEvent}, and the same event of mods that post their own, found by name and
 *     hooked by reflection: Fintastic's {@code BabyFishSpawnEvent}), and as a last resort a baby of a mod's own breeding
 *     that joins the level right at its parents in a tank (a mod without any event) is removed. Our own breeding (the axolotl
 *     eggs of M29, the turtles and herps of M31, the mobs in glass) does not go through any of these.</li>
 * </ul>
 */
public final class ForeignBreeding {
    /** M52: listens to other mods' own breeding events (NeoForge: Fintastic's), set by the loader. */
    public static Runnable modEventHook = () -> {};

    static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/foreign");
    public static final String BROOD = "bettacraft:foreign_brood", REST = "bettacraft:foreign_rest";
    /** M39: the sex of an animal of a sexed profile in its persistent data ({@code true}: female). */
    public static final String SEX = "bettacraft:foreign_female";
    static final long LOG_TICKS = 6000;
    /** Set while we add an animal of our own breeding (the join guard lets it in). */
    static int ours;

    private ForeignBreeding() {}

    // ------------------------------------------------------------------ our breeding

    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        if (days <= 0) return;
        TankData t = c.getTank();
        if (t.inhabitants().isEmpty()) return;
        List<Entity> live = TankInhabitants.liveInhabitants(level, t);
        if (Features.enabled(Feature.SUPPRESS_FOREIGN_BREEDING)) for (Entity e : live) calm(e);
        boolean foreignOn = Features.enabled(Feature.FOREIGN_BREEDING) && !ForeignProfiles.all().isEmpty();
        if (!foreignOn && !org.overengineer.bettacraft.terrarium.SeaTurtles.breedingOn()) return;
        double speed = ServerConfig.getOr(ServerConfig.FOREIGN_BREEDING_SPEED, 1.0);
        Map<EntityType<?>, List<LivingEntity>> byType = new LinkedHashMap<>();
        for (Entity e : live) {
            ForeignProfile p = profile(e.getType(), foreignOn);
            if (p == null || !p.breeding().breeds() || !(e instanceof LivingEntity le)) continue;
            byType.computeIfAbsent(e.getType(), k -> new ArrayList<>()).add(le);
        }
        RandomGenerator rnd = level.getRandom()::nextLong;
        for (Map.Entry<EntityType<?>, List<LivingEntity>> en : byType.entrySet()) {
            ForeignProfile p = profile(en.getKey(), foreignOn);
            if (p == null) continue;
            boolean brooding = false;
            for (LivingEntity e : en.getValue()) {
                CompoundTag d = org.overengineer.bettacraft.platform.Platform.get().persistentData(e);
                if (d.getDouble(REST) > 0) d.putDouble(REST, Math.max(0, d.getDouble(REST) - days));
                if (d.contains(BROOD, Tag.TAG_COMPOUND)) {
                    brooding = true;
                    CompoundTag b = d.getCompound(BROOD);
                    if (p.breeding().nests()) { // M35: eggs into a nest on the land
                        ForeignNests.step(level, c, e, p, b, days, rnd);
                        continue;
                    }
                    b.putDouble("age", b.getDouble("age") + days);
                    if (b.getDouble("age") >= p.breeding().days()) {
                        d.remove(BROOD);
                        d.putDouble(REST, p.breeding().pauseDays());
                        hatch(level, c, e, p, b, rnd);
                    }
                }
            }
            double sp = p == org.overengineer.bettacraft.terrarium.SeaTurtles.BREEDING ? 1.0 : speed; // M35: the sea turtle by the turtle settings
            if (!brooding && sp > 0) mate(level, c, p, en.getValue(), days * sp, rnd);
        }
    }

    /** M35: the breeding profile of a type: its profile (with our breeding of the foreign animals on), else the sea turtle's. */
    @Nullable
    static ForeignProfile profile(EntityType<?> type, boolean foreignOn) {
        ForeignProfile p = foreignOn ? ForeignProfiles.of(type) : null;
        return p != null ? p : org.overengineer.bettacraft.terrarium.SeaTurtles.breeding(type);
    }

    /** Why a pair of this profile does not breed in this tank now, or null if it may. */
    @Nullable
    public static String blocked(ServerLevel level, TankBlockEntity c, ForeignProfile p) {
        TankData t = c.getTank();
        ForeignProfile.Breeding b = p.breeding();
        if (!b.breeds()) return "none";
        if (p.terrarium() == null && t.waterMb() <= 0) return "dry";
        if (p.terrarium() == null && !b.breedingTemp(t.waterTemp())) return "temperature";
        if (t.waterBlocks() + 1e-6 < b.minWaterBlocks()) return "tank_size";
        if (c.isCrowded()) return "crowded";
        if (p.terrarium() == null && TankWater.qualityEnabled() && t.waterQuality() < p.minQuality()) return "water";
        if (p.terrarium() != null) { // M35: a terrarium animal breeds where it lives well (no harmful problem: the right tank, warm enough)
            TerrariumNeeds.Assessment a = p.terrarium().assess(org.overengineer.bettacraft.terrarium.TankClimate.conditions(t));
            if (a.harmful()) return "climate";
        }
        if (p.land() != null && !p.land().needs().isEmpty()) { // M39 part c (#25): bees without a hive or flowers, rabbits without grass
            List<ForeignProfile.Need> missing = MinecraftPets.missing(level, c, p.land());
            if (!missing.isEmpty()) return "no_" + missing.getFirst().key();
        }
        if (b.needsPlants() && TankmateSpots.plants(level.getServer(), t, false).isEmpty() && TankmateSpots.plants(level.getServer(), t, true).isEmpty()) {
            return "plants";
        }
        return null;
    }

    /** Whether this animal may breed now: grown, not resting, fed and healthy (with our care on). */
    static boolean ready(LivingEntity e, ForeignProfile p) {
        if (!e.isAlive() || e.isBaby() || org.overengineer.bettacraft.platform.Platform.get().persistentData(e).getDouble(REST) > 0) return false;
        if (sterile(e, p)) return false; // M39 (F1): a trophy fish of Fish of Thieves
        if (ForeignProfiles.cared(e) == null) return true; // M35: the sea turtle has no hunger of ours
        BettaHealth h = ForeignCare.health(e);
        return !h.isSick() && !h.isHungry(ForeignCare.params(p));
    }

    /** M39 (question F1): whether one of the sterile keys of the profile is set on the animal (a trophy fish does not breed). */
    public static boolean sterile(Entity e, ForeignProfile p) {
        if (p.breeding().sterile().isEmpty()) return false;
        CompoundTag all = e.saveWithoutId(new CompoundTag());
        for (String k : p.breeding().sterile()) if (all.getBoolean(k)) return true;
        return false;
    }

    /**
     * M39: whether an animal of a sexed profile is female. One without a sex yet (caught wild, from before) gets one now, half
     * and half by its UUID (the same answer every time).
     */
    public static boolean female(Entity e) {
        CompoundTag d = org.overengineer.bettacraft.platform.Platform.get().persistentData(e);
        if (!d.contains(SEX)) d.putBoolean(SEX, (e.getUUID().getLeastSignificantBits() & 1L) == 0);
        return d.getBoolean(SEX);
    }

    /** M39: sets the sex of an animal of a sexed profile. */
    public static void setFemale(Entity e, boolean female) {
        org.overengineer.bettacraft.platform.Platform.get().persistentData(e).putBoolean(SEX, female);
    }

    /** M39: the sex of an animal with a sexed breeding profile (true: female) for the menu, or null if it has none. */
    @Nullable
    public static Boolean sexOf(Entity e) {
        ForeignProfile p = ForeignProfiles.cared(e);
        return p != null && p.breeding().sexed() && p.breeding().breeds() ? female(e) : null;
    }

    /**
     * M66: the breeding tab: per type of animal of another mod with a breeding profile (and the sea turtle) the counts, the conditions
     * and the brood ({@link org.overengineer.bettacraft.tank.BreedingOverview}).
     */
    public static net.minecraft.nbt.ListTag overview(ServerLevel level, TankBlockEntity c, List<Entity> live) {
        net.minecraft.nbt.ListTag out = new net.minecraft.nbt.ListTag();
        boolean foreignOn = Features.enabled(Feature.FOREIGN_BREEDING) && !ForeignProfiles.all().isEmpty();
        if (!foreignOn && !org.overengineer.bettacraft.terrarium.SeaTurtles.breedingOn()) return out;
        Map<EntityType<?>, List<LivingEntity>> byType = new LinkedHashMap<>();
        for (Entity e : live) {
            ForeignProfile p = profile(e.getType(), foreignOn);
            if (p == null || !p.breeding().breeds() || !(e instanceof LivingEntity le)) continue;
            byType.computeIfAbsent(e.getType(), k -> new ArrayList<>()).add(le);
        }
        for (Map.Entry<EntityType<?>, List<LivingEntity>> en : byType.entrySet()) {
            ForeignProfile p = profile(en.getKey(), foreignOn);
            if (p == null) continue;
            int males = 0, females = 0, young = 0, adults = 0, ready = 0;
            double brood = -1;
            for (LivingEntity e : en.getValue()) {
                if (e.isBaby()) {
                    young++;
                    continue;
                }
                adults++;
                if (p.breeding().sexed()) {
                    if (female(e)) females++;
                    else males++;
                }
                if (ready(e, p)) ready++;
                CompoundTag d = org.overengineer.bettacraft.platform.Platform.get().persistentData(e);
                if (d.contains(BROOD, Tag.TAG_COMPOUND)) {
                    double days = Math.max(1e-6, p.breeding().days());
                    brood = Math.max(brood, d.getCompound(BROOD).getDouble("age") / days);
                }
            }
            var entry = org.overengineer.bettacraft.tank.BreedingOverview.entry(en.getKey().getDescriptionId(), males, females, young);
            org.overengineer.bettacraft.tank.BreedingOverview.cond(entry, p.breeding().sexed() ? "pair" : "pair_two",
                    p.breeding().sexed() ? males > 0 && females > 0 : adults >= 2);
            org.overengineer.bettacraft.tank.BreedingOverview.cond(entry, "ready_two", ready >= 2);
            String why = blocked(level, c, p);
            if (why == null) org.overengineer.bettacraft.tank.BreedingOverview.cond(entry, "f_free", true);
            else if (why.startsWith("no_")) org.overengineer.bettacraft.tank.BreedingOverview.cond(entry, "f_need", false, why.substring(3));
            else org.overengineer.bettacraft.tank.BreedingOverview.cond(entry, "f_" + why, false);
            if (brood >= 0) org.overengineer.bettacraft.tank.BreedingOverview.state(entry, "brood", Math.round(Math.min(1, brood) * 100));
            out.add(entry);
        }
        return out;
    }

    private static void mate(ServerLevel level, TankBlockEntity c, ForeignProfile p, List<LivingEntity> list, double days, RandomGenerator rnd) {
        List<LivingEntity> ready = new ArrayList<>();
        for (LivingEntity e : list) if (ready(e, p)) ready.add(e);
        if (ready.size() < 2) return;
        String why = blocked(level, c, p);
        long now = level.getGameTime();
        // M39: a sexed profile needs a female and a male among the ready ones
        List<LivingEntity> females = new ArrayList<>(), males = new ArrayList<>();
        if (why == null && p.breeding().sexed()) {
            for (LivingEntity e : ready) (female(e) ? females : males).add(e);
            if (females.isEmpty() || males.isEmpty()) why = "sexes";
        }
        if (why != null) {
            if ("sexes".equals(why) && c.logDue("foreign_breed:" + ready.get(0).getType().getDescriptionId() + why, now, LOG_TICKS * 4)) {
                TankCare.log(level, c, "log.bettacraft.foreign.cannot_breed", TankCare.nameArg(ready.get(0)),
                        "@gui.bettacraft.foreign.blocked." + (females.isEmpty() ? "no_female" : "no_male"));
            }
            if (("tank_size".equals(why) || "plants".equals(why) || why.startsWith("no_")) && c.logDue("foreign_breed:" + ready.get(0).getType().getDescriptionId() + why, now, LOG_TICKS * 4)) {
                TankCare.log(level, c, "log.bettacraft.foreign.cannot_breed", TankCare.nameArg(ready.get(0)), "@gui.bettacraft.foreign.blocked." + why);
            }
            return;
        }
        int pairs = p.breeding().sexed() ? Math.min(females.size(), males.size()) : ready.size() / 2;
        if (rnd.nextDouble() >= 1 - Math.exp(-p.breeding().rate() * pairs * days)) return;
        List<LivingEntity> mothers = p.breeding().sexed() ? females : ready, fathers = p.breeding().sexed() ? males : ready;
        LivingEntity mother = mothers.get(rnd.nextInt(mothers.size()));
        LivingEntity father = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity e : fathers) {
            if (e == mother) continue;
            double dd = e.distanceToSqr(mother);
            if (dd < best) {
                best = dd;
                father = e;
            }
        }
        if (father == null) return;
        CompoundTag brood = new CompoundTag();
        brood.putDouble("age", 0);
        brood.put("father", inherited(father, p));
        org.overengineer.bettacraft.platform.Platform.get().persistentData(mother).put(BROOD, brood);
        org.overengineer.bettacraft.platform.Platform.get().persistentData(father).putDouble(REST, p.breeding().pauseDays() * 0.5);
        TankCare.log(level, c, p.breeding().type() == ForeignProfile.Type.LIVEBEARER ? "log.bettacraft.foreign.pregnant"
                : p.breeding().nests() ? "log.bettacraft.foreign.gravid" : "log.bettacraft.foreign.spawned", TankCare.nameArg(mother)); // M35: a nest on the land
        c.setChanged();
    }

    /** The values of the inherited keys of an animal (its save data). */
    static CompoundTag inherited(Entity e, ForeignProfile p) {
        CompoundTag out = new CompoundTag();
        if (p.breeding().inherit().isEmpty()) return out;
        CompoundTag all = e.saveWithoutId(new CompoundTag());
        for (String k : p.breeding().inherit()) if (all.contains(k)) out.put(k, all.get(k).copy());
        return out;
    }

    private static void hatch(ServerLevel level, TankBlockEntity c, LivingEntity mother, ForeignProfile p, CompoundTag brood, RandomGenerator rnd) {
        TankData t = c.getTank();
        ForeignProfile.Breeding b = p.breeding();
        int live = TankInhabitants.liveInhabitants(level, t).size();
        int room = Math.max(0, ServerConfig.getOr(ServerConfig.TANK_MAX_ENTITIES, 64) - live);
        int n = Math.min(room, b.youngMin() + (b.youngMax() > b.youngMin() ? rnd.nextInt(b.youngMax() - b.youngMin() + 1) : 0));
        CompoundTag motherGenes = inherited(mother, p), fatherGenes = brood.getCompound("father");
        int made = 0;
        for (int i = 0; i < n; i++) {
            Vec3 at = ForeignNests.inTank(t, mother.position().add((rnd.nextDouble() - 0.5) * 0.4, 0, (rnd.nextDouble() - 0.5) * 0.4), mother.position());
            Entity y = spawnYoung(level, c, mother.getType(), p, motherGenes, fatherGenes, at, rnd);
            if (y == null) continue;
            made++;
            if (b.sexed()) setFemale(y, rnd.nextBoolean()); // M39: no nest, no temperature: half and half
        }
        TankCare.log(level, c, "log.bettacraft.foreign.young", TankCare.nameArg(mother), String.valueOf(made));
        org.overengineer.bettacraft.api.ApiEvents.born(level, c, org.overengineer.bettacraft.api.ApiEvents.id(mother.getType()), made, b.type() != ForeignProfile.Type.LIVEBEARER, true, false); // M32 part d
        c.markTankChanged();
    }

    /** A young of this profile in the tank (hatching, GameTests): the inherited keys from either parent, the profile's NBT. */
    @Nullable
    public static Entity spawnYoung(ServerLevel level, TankBlockEntity c, EntityType<?> parentType, ForeignProfile p, CompoundTag mother,
                                    CompoundTag father, Vec3 at, RandomGenerator rnd) {
        ForeignProfile.Breeding b = p.breeding();
        EntityType<?> type = b.offspring() != null ? BuiltInRegistries.ENTITY_TYPE.getOptional(b.offspring()).orElse(null) : parentType;
        if (type == null) {
            LOGGER.warn("offspring {} is not registered", b.offspring());
            return null;
        }
        Entity e = type.create(level);
        if (e == null) return null;
        // M35: a terrarium animal hatches where it is (its nest on the land), a water animal in the water
        Vec3 p0 = p.terrarium() != null ? at : new TankWaterVolume(c.getTank()).clamp(at, Math.max(0.1, e.getBbWidth() / 2), 0.05, Math.max(0.1, e.getBbHeight()));
        if (!b.inherit().isEmpty() || b.offspringNbt() != null || !b.sterile().isEmpty()) {
            CompoundTag data = e.saveWithoutId(new CompoundTag());
            for (String k : b.inherit()) {
                if (b.sterile().contains(k)) continue; // M39 (F1): never inherited
                CompoundTag from = rnd.nextBoolean() ? mother : father;
                if (!from.contains(k)) from = from == mother ? father : mother;
                if (from.contains(k)) data.put(k, from.get(k).copy());
            }
            if (b.offspringNbt() != null) data.merge(b.offspringNbt().copy());
            for (String k : b.sterile()) data.putBoolean(k, false); // M39 (F1): a young trophy fish? never
            data.remove("UUID");
            try {
                e.load(data);
            } catch (Exception ex) {
                LOGGER.warn("cannot apply the inherited data to a young {}: {}", BuiltInRegistries.ENTITY_TYPE.getKey(type), ex.toString());
            }
        }
        if (b.baby() && e instanceof Mob m) m.setBaby(true);
        e.moveTo(p0.x, p0.y, p0.z, (float) (rnd.nextDouble() * 360), 0F);
        TankInhabitants.adopt(e, c);
        ours++;
        try {
            if (!level.addFreshEntity(e)) return null;
        } finally {
            ours--;
        }
        c.getTank().inhabitants().add(e.getUUID());
        return e;
    }

    // ------------------------------------------------------------------ their breeding suppressed

    private static final Map<Class<?>, Method[]> LOVE = new ConcurrentHashMap<>();

    /** Whether this inhabitant is an animal of vanilla or another mod (not one of ours). */
    public static boolean foreign(Entity e) {
        return !BettaCraft.MOD_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace());
    }

    /** {@code resetLove()} and {@code isFood(ItemStack)} of the animal's class (vanilla {@code Animal} and mods with the same names). */
    private static Method[] loveMethods(Class<?> c) {
        return LOVE.computeIfAbsent(c, k -> {
            Method reset = null, food = null;
            try {
                reset = k.getMethod("resetLove");
            } catch (NoSuchMethodException ignored) {
            }
            try {
                food = k.getMethod("isFood", ItemStack.class);
                if (food.getReturnType() != boolean.class) food = null;
            } catch (NoSuchMethodException ignored) {
            }
            return new Method[]{reset, food};
        });
    }

    /** Takes the love from a foreign inhabitant (its own breeding cannot start in the tank). */
    static void calm(Entity e) {
        if (!foreign(e) || !(e instanceof LivingEntity)) return;
        if (e instanceof net.minecraft.world.entity.Mob mob) org.overengineer.bettacraft.compat.ModBreedingGoals.strip(mob); // M42: Fish Eggs
        if (e instanceof net.minecraft.world.entity.animal.Animal a) {
            if (a.isInLove()) a.resetLove();
            return;
        }
        Method m = loveMethods(e.getClass())[0];
        if (m == null) return;
        try {
            m.invoke(e);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            LOVE.put(e.getClass(), new Method[2]); // do not try again
        }
    }

    /** Whether this item is the breeding food of the animal (vanilla {@code isFood} or the mod's method of that name). */
    static boolean isBreedingFood(Entity e, ItemStack s) {
        if (s.isEmpty()) return false;
        if (e instanceof net.minecraft.world.entity.animal.Animal a) return a.isFood(s);
        Method m = loveMethods(e.getClass())[1];
        if (m == null) return false;
        try {
            return (boolean) m.invoke(e, s);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return false;
        }
    }






    static boolean inTank(@Nullable Object o) {
        return o instanceof Entity e && !e.level().isClientSide && foreign(e) && TankInhabitants.hasTankRef(e);
    }


}
