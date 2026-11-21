package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.big.BigAnimals;
import org.overengineer.bettacraft.big.TransportTubItem;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.food.BettaFood;
import org.overengineer.bettacraft.food.FoodDiet;
import org.overengineer.bettacraft.foreign.ForeignBreeding;
import org.overengineer.bettacraft.foreign.ForeignCare;
import org.overengineer.bettacraft.foreign.ForeignNests;
import org.overengineer.bettacraft.foreign.ForeignProfile;
import org.overengineer.bettacraft.foreign.ForeignProfiles;
import org.overengineer.bettacraft.foreign.ForeignTerrarium;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.terrarium.Climate;

/**
 * M39 part a: the water animals of other mods by their shipped profiles, with the real mods: Fintastic, Aquaculture, Fish of
 * Thieves and Naturalist on the dev runtime (unless {@code -PnoCompat}), Critters and Companions, Living Things, Fish Eggs,
 * Shiny and Alex's Caves with {@code -Paquatic}, Twilight Forest with {@code -Pbiomes}; a test whose mod is missing passes
 * with a log line. Big fish go into big tanks by the size rule (M30) with their real length, the Aquaculture turtles breed
 * by our turtle rules with the sex from the nest temperature, Fish of Thieves inherit their colour variant and trophies do
 * not breed, unsuitable animals are refused with their reason, the food of other mods is in our food tags, and Shiny leaves
 * our animals alone while its own shiny fish stay shiny in our tanks. Random draws are made certain (extreme temperatures,
 * long steps) or repeated until they happen.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ForeignWaterAnimalsGameTests {
    private static final String EMPTY = "empty_32x10x32";
    private static final String TOO_SMALL = "message.bettacraft.big.too_small";

    private ForeignWaterAnimalsGameTests() {}

    // ------------------------------------------------------------------ helpers

    private static boolean skip(GameTestHelper h, String... mods) {
        for (String m : mods) {
            if (!org.overengineer.bettacraft.platform.Platform.get().isModLoaded(m)) {
                org.slf4j.LoggerFactory.getLogger("bettacraft/gametest").info("{} not installed: M39 test skipped", m);
                h.succeed();
                return true;
            }
        }
        return false;
    }

    private static EntityType<?> type(String id) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(id)).orElseThrow(() -> new GameTestAssertException("no entity " + id));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).orElseThrow(() -> new GameTestAssertException("no item " + id));
    }

    private static ForeignProfile profile(EntityType<?> t) {
        ForeignProfile p = ForeignProfiles.of(t);
        if (p == null) throw new GameTestAssertException("no profile for " + BuiltInRegistries.ENTITY_TYPE.getKey(t));
        return p;
    }

    /** A new animal of this type in a transport tub (big ones) or a fish bag (small ones). */
    private static ItemStack holder(GameTestHelper h, EntityType<?> type, boolean tub) {
        Entity e = type.create(h.getLevel());
        if (e == null) throw new GameTestAssertException("no " + type);
        ItemStack s = tub ? TransportTubItem.fillWith(e) : FishBagItem.fillWith(e);
        e.discard();
        return s;
    }

    private static Component insert(GameTestHelper h, TankBlockEntity c, ItemStack stack) {
        return ForeignTerrariumGameTests.insert(h, c, stack);
    }

    /** Inserts and expects the message {@code key} (null: accepted). */
    private static void expect(GameTestHelper h, TankBlockEntity c, ItemStack stack, String key, String what) {
        Component err = insert(h, c, stack);
        if (key == null) {
            if (err != null) throw new GameTestAssertException(what + " refused: " + err.getString());
        } else if (!ForeignTerrariumGameTests.says(err, key)) {
            throw new GameTestAssertException(what + ": expected " + key + ", got " + (err == null ? "accepted" : err.getString()));
        }
    }

    private static List<LivingEntity> ofType(GameTestHelper h, TankBlockEntity c, EntityType<?> t) {
        return ForeignTerrariumGameTests.ofType(h, c, t);
    }

    private static TankBlockEntity tank(GameTestHelper h, int x, int z, int sx, int sy, int sz, float temp) {
        return ForeignGameTests.tank(h, x, z, sx, sy, sz, temp);
    }

    private static TankBlockEntity sea(GameTestHelper h, int x, int z, int sx, int sy, int sz, float temp) {
        TankBlockEntity c = tank(h, x, z, sx, sy, sz, temp);
        c.getTank().setSalinity(35F);
        return c;
    }

    /** Feeds and steps the pair until one carries a brood (at most {@code tries} steps). */
    private static LivingEntity breedUntilBrood(ServerLevel level, TankBlockEntity c, List<LivingEntity> pair, int tries) {
        for (int i = 0; i < tries; i++) {
            for (LivingEntity e : pair) if (org.overengineer.bettacraft.platform.Platform.get().persistentData(e).contains(ForeignBreeding.BROOD)) return e;
            ForeignBreeding.step(level, c, 0.05);
            ForeignCare.feed(pair, pair.size(), null);
        }
        for (LivingEntity e : pair) if (org.overengineer.bettacraft.platform.Platform.get().persistentData(e).contains(ForeignBreeding.BROOD)) return e;
        return null;
    }

    // ------------------------------------------------------------------ Fintastic

    /**
     * Fintastic's big fish by their real length (they were on the tank blacklist before): the featherback (0.8 m) not into a tank
     * of 6 x 3 x 4 (the size rule names the minimum), into one of 10 x 3 x 5; the coelacanth (2 m) into 24 x 6 x 12 sea water. The
     * dwarf frog is a small aquarium animal that eats Fintastic's live food and needs plants for its eggs.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m39_fintastic", timeoutTicks = 300)
    public static void fintasticBigFishAndDwarfFrog(GameTestHelper h) {
        if (skip(h, "fintastic")) return;
        EntityType<?> featherback = type("fintastic:featherback"), coelacanth = type("fintastic:coelacanth"), arapaima = type("fintastic:arapaima");
        h.assertTrue(Math.abs(BigAnimals.length(arapaima, null) - 2.5) < 1e-6, "the arapaima is 2.5 m");
        h.assertTrue(TankInhabitants.accepts(arapaima, null) == TankInhabitants.Acceptance.BIG, "the arapaima is a big animal, not blacklisted");
        h.assertTrue(type("fintastic:arapaima").is(ModTags.PREDATORS), "a big predator");
        TankBlockEntity small = tank(h, 1, 1, 6, 3, 4, 26F);
        expect(h, small, holder(h, featherback, true), TOO_SMALL, "featherback into 6 x 3 x 4");
        TankBlockEntity mid = tank(h, 1, 7, 10, 3, 5, 26F);
        expect(h, mid, holder(h, featherback, true), null, "featherback into 10 x 3 x 5");
        TankBlockEntity big = sea(h, 1, 14, 24, 6, 12, 18F);
        expect(h, mid, holder(h, coelacanth, true), TOO_SMALL, "coelacanth into 10 x 3 x 5");
        expect(h, big, holder(h, coelacanth, true), null, "coelacanth into 24 x 6 x 12");
        LivingEntity coel = ofType(h, big, coelacanth).getFirst();
        ForeignProfile cp = profile(coelacanth);
        h.assertTrue(!ForeignCare.conditions(big, coel, cp).tempBad(), "18 °C is right for a coelacanth");
        big.getTank().setWaterTemp(27F);
        h.assertTrue(ForeignCare.conditions(big, coel, cp).tempBad(), "27 °C is too warm for it");
        // the dwarf frog
        EntityType<?> frog = type("fintastic:dwarf_frog");
        TankBlockEntity ft = tank(h, 18, 1, 3, 2, 3, 25F);
        LivingEntity a = ForeignGameTests.add(h, ft, frog, t -> t.putInt("Variant", 1));
        ForeignGameTests.add(h, ft, frog, t -> t.putInt("Variant", 2));
        h.assertTrue(TankInhabitants.accepts(frog, a) == TankInhabitants.Acceptance.OK, "the dwarf frog fits an aquarium");
        h.assertTrue("plants".equals(ForeignBreeding.blocked(h.getLevel(), ft, profile(frog))), "its eggs need plants");
        ForeignCare.advance(h.getLevel(), ft, a, profile(frog), 2.0);
        int fed = TankCare.feed(h.getLevel(), ft, new ItemStack(item("fintastic:daphnia")), 1);
        h.assertTrue(fed >= 1 && ForeignCare.health(a).daysSinceFed() < 1e-6, "Fintastic's daphnia feed it (live food)");
        h.succeed();
    }

    // ------------------------------------------------------------------ Aquaculture

    /**
     * Aquaculture's big fish (blacklisted before) by the size rule: the catfish (1 m) not into 6 x 3 x 4, into 12 x 3 x 6; the tuna
     * (2 m) not into 12 x 3 x 6, into 24 x 6 x 12 sea water, where it eats raw fish by hand. The jellyfish lives in a sea water
     * tank on live food; the carp is a normal food fish that spawns on plants in a big tank.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m39_aquaculture", timeoutTicks = 300)
    public static void aquacultureBigFishJellyfishAndCarp(GameTestHelper h) {
        if (skip(h, "aquaculture")) return;
        EntityType<?> catfish = type("aquaculture:catfish"), tuna = type("aquaculture:tuna"), jelly = type("aquaculture:jellyfish"), carp = type("aquaculture:carp");
        TankBlockEntity small = tank(h, 1, 1, 6, 3, 4, 22F);
        TankBlockEntity mid = tank(h, 8, 1, 12, 3, 6, 22F);
        TankBlockEntity big = sea(h, 1, 9, 24, 6, 12, 20F);
        expect(h, small, holder(h, catfish, true), TOO_SMALL, "catfish into 6 x 3 x 4");
        expect(h, mid, holder(h, catfish, true), null, "catfish into 12 x 3 x 6");
        expect(h, mid, holder(h, tuna, true), TOO_SMALL, "tuna into 12 x 3 x 6");
        expect(h, big, holder(h, tuna, true), null, "tuna into 24 x 6 x 12");
        LivingEntity t = ofType(h, big, tuna).getFirst();
        h.assertTrue(BigAnimals.isBig(t) && Math.abs(BigAnimals.length(tuna, t) - 2.0) < 1e-6, "the tuna counts 2 m");
        ForeignCare.advance(h.getLevel(), big, t, profile(tuna), 3.0);
        List<LivingEntity> eaters = ForeignCare.eaters(h.getLevel(), big.getTank(), new ItemStack(net.minecraft.world.item.Items.COD), null);
        h.assertTrue(eaters.contains(t), "the tuna eats raw fish");
        // the jellyfish in sea water
        TankBlockEntity jt = sea(h, 26, 1, 3, 2, 3, 22F);
        LivingEntity j = ForeignGameTests.add(h, jt, jelly, x -> {});
        h.assertTrue(TankInhabitants.accepts(jelly, j) == TankInhabitants.Acceptance.OK, "the jellyfish fits a tank");
        ForeignProfile jp = profile(jelly);
        h.assertTrue(jp.salinity().min() >= 20 && jp.eatsKind("live") && !jp.breeding().breeds(), "a sea animal on live food that does not breed");
        ForeignCare.advance(h.getLevel(), jt, j, jp, 3.0);
        h.assertTrue(TankCare.feed(h.getLevel(), jt, new ItemStack(ModItems.DAPHNIA.get()), 1) >= 1 && ForeignCare.health(j).daysSinceFed() < 1e-6, "it eats daphnia");
        // the carp: a food fish of the normal tank (no size rule), eggs on plants in a tank of 24 water blocks
        h.assertTrue(BigAnimals.length(carp, null) == 0 && TankInhabitants.accepts(carp, null) == TankInhabitants.Acceptance.OK, "the carp is no big animal");
        ForeignProfile kp = profile(carp);
        h.assertTrue(kp.breeding().needsPlants() && kp.breeding().minWaterBlocks() >= 24, "eggs on plants, a big tank");
        h.succeed();
    }

    /**
     * The box turtle of Aquaculture by our turtle rules: a dry terrarium; a pair of two females does not breed (log); a female and a
     * male lay eggs into the sand, and the nest temperature decides the sex of the young (33 °C: all female, 24 °C: all male).
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m39_turtles", timeoutTicks = 400)
    public static void aquacultureBoxTurtleSexByNestTemperature(GameTestHelper h) {
        if (skip(h, "aquaculture")) return;
        ServerLevel level = h.getLevel();
        EntityType<?> box = type("aquaculture:box_turtle");
        ForeignProfile p = profile(box);
        h.assertTrue(p.breeding().sex() == ForeignProfile.Sex.TEMPERATURE && p.terrarium() != null, "a turtle profile with sexes by temperature");
        // two females
        TankBlockEntity same = ForeignTerrariumGameTests.terrarium(h, 1, 1, 6, 3, 4);
        same.getTank().setClimate(new Climate.State(28, 28, 40, 75, 5, 0));
        for (int i = 0; i < 2; i++) ForeignBreeding.setFemale(ForeignTerrariumGameTests.put(h, same, ForeignTerrariumGameTests.container(h, box, false, false)), true);
        for (LivingEntity e : ofType(h, same, box)) ForeignTerrariumGameTests.feed(e);
        ForeignBreeding.step(level, same, 30.0);
        h.assertTrue(ofType(h, same, box).stream().noneMatch(e -> org.overengineer.bettacraft.platform.Platform.get().persistentData(e).contains(ForeignBreeding.BROOD)), "two females lay no eggs");
        h.assertTrue(same.getTank().log().stream().anyMatch(e -> e.key().equals("log.bettacraft.foreign.cannot_breed")
                && e.args().contains("@gui.bettacraft.foreign.blocked.no_male")), "the log says there is no male");
        // warm and cool nests
        for (float nest : new float[]{33F, 24F}) {
            TankBlockEntity c = ForeignTerrariumGameTests.terrarium(h, nest > 30 ? 9 : 17, 1, 6, 3, 4);
            c.getTank().setClimate(new Climate.State(28, 28, 40, 75, 5, 0));
            LivingEntity f = ForeignTerrariumGameTests.put(h, c, ForeignTerrariumGameTests.container(h, box, false, false));
            LivingEntity m = ForeignTerrariumGameTests.put(h, c, ForeignTerrariumGameTests.container(h, box, false, false));
            ForeignBreeding.setFemale(f, true);
            ForeignBreeding.setFemale(m, false);
            LivingEntity mother = ForeignTerrariumGameTests.layEggs(h, c, box);
            // M52.5: the clutch incubates at the nest temperature from the start (the eggs she carried had progressed at 28 °C for a
            // random time, now and then into the thermosensitive period: a male at 33 °C)
            if (mother != null) {
                net.minecraft.nbt.CompoundTag clutch = org.overengineer.bettacraft.platform.Platform.get().persistentData(mother)
                        .getCompound(ForeignBreeding.BROOD).getCompound("nest");
                clutch.putDouble("progress", 0d);
                clutch.remove("tsp_sum");
                clutch.remove("tsp_days");
            }
            h.assertTrue(mother == f, "the female lays the eggs");
            c.getTank().setClimate(new Climate.State(nest, nest, 40, 75, 5, 0));
            List<LivingEntity> before = ofType(h, c, box);
            ForeignBreeding.step(level, c, 12.0);
            List<LivingEntity> young = new ArrayList<>();
            for (LivingEntity e : ofType(h, c, box)) if (!before.contains(e)) young.add(e);
            h.assertTrue(young.size() >= 2 && young.size() <= 4, "2-4 hatchlings at " + nest + " °C, got " + young.size() + ": " + c.getTank().log());
            boolean wantFemale = nest > 30;
            for (LivingEntity y : young) {
                h.assertTrue(y.isBaby(), "a hatchling");
                h.assertTrue(ForeignBreeding.female(y) == wantFemale, (wantFemale ? "female" : "male") + " at " + nest + " °C");
            }
            h.assertTrue(c.getTank().log().stream().anyMatch(e -> e.key().equals("log.bettacraft.foreign.hatched_sexes")), "the sexes are logged");
            h.assertTrue(ForeignBreeding.sexOf(young.getFirst()) == wantFemale, "the menu shows the sex");
        }
        h.succeed();
    }

    /** The arrau turtle (0.8 m) is a big animal of the paludarium: not in the terrarium box, from the tub only into a big one. */
    @GameTest(template = EMPTY, batch = "bettacraft_m39_turtles", timeoutTicks = 300)
    public static void arrauTurtleIsABigPaludariumAnimal(GameTestHelper h) {
        if (skip(h, "aquaculture")) return;
        EntityType<?> arrau = type("aquaculture:arrau_turtle");
        ForeignProfile p = profile(arrau);
        h.assertTrue(p.land() != null && !p.land().box() && Math.abs(p.bigLength() - 0.8) < 1e-6, "0.8 m, no box");
        TankBlockEntity small = ForeignTerrariumGameTests.paludarium(h, 1, 1, 6, 3, 4, 0, 0.5, Blocks.SAND.defaultBlockState());
        TankBlockEntity big = ForeignTerrariumGameTests.paludarium(h, 1, 8, 12, 4, 6, 0, 0.5, Blocks.SAND.defaultBlockState());
        expect(h, small, holder(h, arrau, true), TOO_SMALL, "arrau into a small paludarium");
        expect(h, big, holder(h, arrau, true), null, "arrau into a big paludarium");
        LivingEntity a = ofType(h, big, arrau).getFirst();
        h.assertTrue(!ForeignTerrarium.boxTakes(a), "the box does not take it");
        h.assertTrue(ForeignTerrarium.gait(arrau) != null, "the walker moves it");
        h.succeed();
    }

    // ------------------------------------------------------------------ Fish of Thieves

    /**
     * Fish of Thieves splashtails: a pair breeds by our rules, every young takes the colour variant of a parent and is no trophy;
     * a trophy fish does not breed (question F1), so a trophy with one normal fish gives no young.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m39_fot", timeoutTicks = 300)
    public static void fishOfThievesInheritTheirVariantTrophiesDoNotBreed(GameTestHelper h) {
        if (skip(h, "fishofthieves")) return;
        ServerLevel level = h.getLevel();
        EntityType<?> splash = type("fishofthieves:splashtail");
        ForeignProfile p = profile(splash);
        h.assertTrue(p.breeding().inherit().contains("variant") && p.breeding().sterile().contains("Trophy"), "the shipped profile");
        // a trophy and a normal one: no brood
        TankBlockEntity tt = tank(h, 1, 1, 3, 2, 3, 23F);
        LivingEntity trophy = ForeignGameTests.add(h, tt, splash, t -> {
            t.putString("variant", "fishofthieves:ruby");
            t.putBoolean("Trophy", true);
        });
        LivingEntity single = ForeignGameTests.add(h, tt, splash, t -> t.putString("variant", "fishofthieves:sunny"));
        h.assertTrue(ForeignBreeding.sterile(trophy, p) && !ForeignBreeding.sterile(single, p), "the trophy is sterile");
        h.assertTrue(breedUntilBrood(level, tt, List.of(trophy, single), 400) == null, "a trophy does not breed");
        // two normal ones
        TankBlockEntity c = tank(h, 6, 1, 3, 2, 3, 23F);
        LivingEntity a = ForeignGameTests.add(h, c, splash, t -> t.putString("variant", "fishofthieves:ruby"));
        LivingEntity b = ForeignGameTests.add(h, c, splash, t -> t.putString("variant", "fishofthieves:sunny"));
        h.assertTrue(TankInhabitants.accepts(splash, a) == TankInhabitants.Acceptance.OK, "a splashtail fits a tank");
        LivingEntity mother = breedUntilBrood(level, c, List.of(a, b), 2000);
        h.assertTrue(mother != null, "the pair spawned");
        ForeignBreeding.step(level, c, p.breeding().days() + 0.1);
        List<LivingEntity> young = new ArrayList<>();
        for (LivingEntity e : ofType(h, c, splash)) if (e != a && e != b) young.add(e);
        h.assertTrue(!young.isEmpty(), "young splashtails");
        for (LivingEntity y : young) {
            CompoundTag d = y.saveWithoutId(new CompoundTag());
            String v = d.getString("variant");
            h.assertTrue(v.equals("fishofthieves:ruby") || v.equals("fishofthieves:sunny"), "the variant of a parent: " + v);
            h.assertFalse(d.getBoolean("Trophy"), "no young trophy");
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ Naturalist

    /**
     * Naturalist's sea animals: the ray (0.8 m) by the size rule, the starfish and the giant isopod (cold sea water) as small
     * animals; the angler fish and the blob fish are refused as deep-sea animals, the great white shark and the whale as too big
     * (question E1), with their reason; Aquaculture's shroomas as mushroom creatures.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m39_naturalist", timeoutTicks = 300)
    public static void naturalistSeaAnimalsAndRefusals(GameTestHelper h) {
        if (skip(h, "naturalist")) return;
        EntityType<?> ray = type("naturalist:ray"), star = type("naturalist:starfish"), iso = type("naturalist:giant_isopod");
        TankBlockEntity small = sea(h, 1, 1, 6, 3, 4, 24F);
        TankBlockEntity big = sea(h, 1, 7, 10, 3, 5, 24F);
        expect(h, small, holder(h, ray, true), TOO_SMALL, "ray into 6 x 3 x 4");
        expect(h, big, holder(h, ray, true), null, "ray into 10 x 3 x 5");
        expect(h, small, holder(h, star, false), null, "starfish into a small sea water tank");
        TankBlockEntity cold = sea(h, 14, 1, 3, 2, 3, 8F);
        LivingEntity i = ForeignGameTests.add(h, cold, iso, t -> {});
        h.assertTrue(!ForeignCare.conditions(cold, i, profile(iso)).tempBad(), "8 °C suits the giant isopod");
        cold.getTank().setWaterTemp(24F);
        h.assertTrue(ForeignCare.conditions(cold, i, profile(iso)).tempBad(), "24 °C is too warm for it");
        for (String[] r : new String[][]{{"naturalist:anglerfish", "deep_sea"}, {"naturalist:blobfish", "deep_sea"},
                {"naturalist:great_white_shark", "too_big"}, {"naturalist:whale", "too_big"}, {"aquaculture:brown_shrooma", "fungus"}}) {
            if (!org.overengineer.bettacraft.platform.Platform.get().isModLoaded(ResourceLocation.parse(r[0]).getNamespace())) continue;
            EntityType<?> t = type(r[0]);
            h.assertTrue(r[1].equals(ForeignProfiles.refusal(t)), r[0] + " refused for " + r[1]);
            h.assertTrue(TankInhabitants.accepts(t, null) == TankInhabitants.Acceptance.REFUSED, r[0] + " is refused");
            h.assertTrue(ForeignProfiles.of(t) == null, r[0] + " has no care profile");
            expect(h, small, holder(h, t, true), "message.bettacraft.tank.refused." + r[1], r[0]);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ -Paquatic: Critters and Companions, Living Things

    /** The dumbo octopus of Critters and Companions: a small animal of cold sea water (6 °C fine, 14 °C too warm). */
    @GameTest(template = EMPTY, batch = "bettacraft_m39_aquatic", timeoutTicks = 200)
    public static void dumboOctopusInColdSeaWater(GameTestHelper h) {
        if (skip(h, "crittersandcompanions")) return;
        EntityType<?> octo = type("crittersandcompanions:dumbo_octopus");
        TankBlockEntity c = sea(h, 1, 1, 3, 2, 3, 6F);
        expect(h, c, holder(h, octo, false), null, "dumbo octopus into cold sea water");
        LivingEntity o = ofType(h, c, octo).getFirst();
        h.assertTrue(!ForeignCare.conditions(c, o, profile(octo)).tempBad(), "6 °C suits it");
        c.getTank().setWaterTemp(14F);
        h.assertTrue(ForeignCare.conditions(c, o, profile(octo)).tempBad(), "14 °C is too warm");
        h.succeed();
    }

    /**
     * Living Things: seahorses breed as livebearers and the young take the colour of a parent; the manta ray (2.5 m) and the shark
     * (2 m) only into big sea water tanks by the size rule; the crab lives in a paludarium, not in an aquarium.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m39_livingthings", timeoutTicks = 400)
    public static void livingThingsSeahorseMantaSharkCrab(GameTestHelper h) {
        if (skip(h, "livingthings")) return;
        ServerLevel level = h.getLevel();
        EntityType<?> horse = type("livingthings:seahorse"), manta = type("livingthings:mantaray"), shark = type("livingthings:shark"),
                crab = type("livingthings:crab");
        TankBlockEntity st = sea(h, 1, 1, 3, 2, 3, 25F);
        LivingEntity a = ForeignGameTests.add(h, st, horse, t -> t.putInt("SeahorseVariant", 1));
        LivingEntity b = ForeignGameTests.add(h, st, horse, t -> t.putInt("SeahorseVariant", 3));
        LivingEntity mother = breedUntilBrood(level, st, List.of(a, b), 2000);
        h.assertTrue(mother != null, "a seahorse carries young");
        ForeignBreeding.step(level, st, profile(horse).breeding().days() + 0.1);
        List<LivingEntity> young = new ArrayList<>();
        for (LivingEntity e : ofType(h, st, horse)) if (e != a && e != b) young.add(e);
        h.assertTrue(!young.isEmpty(), "young seahorses");
        for (LivingEntity y : young) {
            int v = y.saveWithoutId(new CompoundTag()).getInt("SeahorseVariant");
            h.assertTrue(v == 1 || v == 3, "the colour of a parent: " + v);
        }
        // the big ones
        TankBlockEntity mid = sea(h, 6, 1, 12, 3, 6, 25F);
        TankBlockEntity big = sea(h, 1, 9, 30, 8, 15, 25F);
        expect(h, mid, holder(h, manta, true), TOO_SMALL, "manta ray into 12 x 3 x 6");
        expect(h, mid, holder(h, shark, true), TOO_SMALL, "shark into 12 x 3 x 6");
        expect(h, big, holder(h, manta, true), null, "manta ray into 30 x 8 x 15");
        h.assertTrue(Math.abs(BigAnimals.length(shark, null) - 2.0) < 1e-6 && shark.is(ModTags.PREDATORS), "the shark: 2 m, a predator");
        // the crab
        TankBlockEntity palu = ForeignTerrariumGameTests.paludarium(h, 20, 1, 6, 3, 6, 0, 0.5, Blocks.SAND.defaultBlockState());
        expect(h, palu, ForeignTerrariumGameTests.container(h, crab, false, false), null, "crab into a paludarium");
        expect(h, mid, ForeignTerrariumGameTests.container(h, crab, false, false), "message.bettacraft.terrarium.wrong_mode", "crab into an aquarium");
        h.succeed();
    }

    /** Alex's Caves (-Paquatic) and Twilight Forest (-Pbiomes): their creatures are refused with the reason (question E1). */
    @GameTest(template = EMPTY, batch = "bettacraft_m39_refused", timeoutTicks = 200)
    public static void caveAndTwilightCreaturesRefused(GameTestHelper h) {
        boolean ac = org.overengineer.bettacraft.platform.Platform.get().isModLoaded("alexscaves"), tf = org.overengineer.bettacraft.platform.Platform.get().isModLoaded("twilightforest");
        if (!ac && !tf) {
            h.succeed();
            return;
        }
        TankBlockEntity c = sea(h, 1, 1, 4, 2, 4, 20F);
        List<String[]> cases = new ArrayList<>();
        if (ac) {
            cases.add(new String[]{"alexscaves:lanternfish", "deep_sea"});
            cases.add(new String[]{"alexscaves:radgill", "cave"});
            cases.add(new String[]{"alexscaves:sweetish_fish", "cave"});
            cases.add(new String[]{"alexscaves:hullbreaker", "too_big"});
        }
        if (tf) {
            cases.add(new String[]{"twilightforest:fire_beetle", "hostile"});
            cases.add(new String[]{"twilightforest:helmet_crab", "hostile"});
        }
        for (String[] r : cases) {
            EntityType<?> t = type(r[0]);
            h.assertTrue(r[1].equals(ForeignProfiles.refusal(t)), r[0] + " refused for " + r[1]);
            h.assertTrue(TankInhabitants.accepts(t, null) == TankInhabitants.Acceptance.REFUSED, r[0] + " is refused");
            Component msg = TankInhabitants.rejection(TankInhabitants.Acceptance.REFUSED, t);
            h.assertTrue(ForeignTerrariumGameTests.says(msg, "message.bettacraft.tank.refused." + r[1]), r[0] + ": " + msg.getString());
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ food of other mods

    /**
     * Question C1: Fintastic's copepods, daphnia and fairy shrimp are live food (and its daphnia start a daphnia barrel); with
     * -Paquatic, Fish Eggs' fish flakes are flake food and its fish and squid eggs protein-rich frozen food.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m39_food", timeoutTicks = 100)
    public static void foodOfOtherMods(GameTestHelper h) {
        if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded("fintastic")) {
            for (String id : List.of("fintastic:copepod", "fintastic:daphnia", "fintastic:fairy_shrimp")) {
                ItemStack s = new ItemStack(item(id));
                h.assertTrue(BettaFood.kind(s) == BettaFood.Kind.LIVE && s.is(ModTags.LIVE_FOOD), id + " is live food");
                h.assertTrue(s.is(FoodDiet.Group.LIVE.tag), id + " in the live food group");
            }
            h.assertTrue(new ItemStack(item("fintastic:daphnia")).is(ModTags.DAPHNIA_STARTERS), "Fintastic's daphnia start a barrel");
        }
        h.assertTrue(new ItemStack(ModItems.DAPHNIA.get()).is(ModTags.DAPHNIA_STARTERS), "ours too");
        if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded("fisheggs")) {
            ItemStack flakes = new ItemStack(item("fisheggs:fish_food"));
            h.assertTrue(BettaFood.kind(flakes) == BettaFood.Kind.NORMAL && flakes.is(FoodDiet.Group.FLAKES.tag), "fish flakes are flake food");
            for (String id : List.of("fisheggs:fish_eggs", "fisheggs:squid_eggs")) {
                ItemStack s = new ItemStack(item(id));
                h.assertTrue(s.is(ModTags.FROZEN_FOOD) && BettaFood.kind(s) == BettaFood.Kind.LIVE, id + " is frozen food");
                h.assertTrue(s.is(ModTags.CONDITIONING_FOOD), id + " conditions for breeding (protein)");
            }
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ Shiny (-Paquatic)

    /**
     * Shiny swaps vanilla mobs for shiny ones as they spawn (by the id {@code minecraft:x} -> {@code shiny:shiny_x}): our animals
     * are never swapped (a betta from a spawn egg stays a betta with its genes) and no shiny type of ours exists; a shiny tropical
     * fish goes into a tank with the fish bag and comes out again still shiny.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m39_shiny", timeoutTicks = 200)
    public static void shinyLeavesOursAndKeepsItsOwn(GameTestHelper h) {
        if (skip(h, "shiny")) return;
        ServerLevel level = h.getLevel();
        for (ResourceLocation id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            if (!id.getNamespace().equals(BettaCraft.MOD_ID)) continue;
            h.assertTrue(BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.fromNamespaceAndPath("shiny", "shiny_" + id.getPath())).isEmpty(),
                    "no shiny type for " + id);
        }
        for (int x = 19; x <= 21; x++) for (int z = 19; z <= 21; z++) for (int y = 2; y <= 3; y++) h.setBlock(new BlockPos(x, y, z), Blocks.WATER.defaultBlockState());
        Entity spawned = ModEntities.BETTA.get().spawn(level, h.absolutePos(new BlockPos(20, 2, 20)), MobSpawnType.SPAWN_EGG);
        h.assertTrue(spawned instanceof BettaEntity, "a betta from a spawn egg");
        ((BettaEntity) spawned).setPersistenceRequired(); // no player near: a water mob would despawn on its own
        java.util.UUID id = spawned.getUUID();
        String genes = ((BettaEntity) spawned).genome().toString();
        EntityType<?> shinyFish = type("shiny:shiny_tropical_fish");
        TankBlockEntity c = tank(h, 1, 1, 3, 2, 3, 25F);
        expect(h, c, holder(h, shinyFish, false), null, "a shiny tropical fish into a tank");
        LivingEntity fish = ofType(h, c, shinyFish).getFirst();
        h.runAfterDelay(20, () -> {
            Entity e = level.getEntity(id);
            h.assertTrue(e != null, "the betta is still there (same UUID)");
            h.assertTrue(e.getType() == ModEntities.BETTA.get(), "the betta is still ours: " + e.getType());
            h.assertTrue(((BettaEntity) e).genome().toString().equals(genes), "with its genes");
            Entity f = level.getEntity(fish.getUUID());
            h.assertTrue(f != null && f.getType() == shinyFish && c.getTank().inhabitants().contains(f.getUUID()), "the shiny fish stays shiny in its tank");
            ServerPlayer p = MockPlayers.serverPlayer(h);
            p.getInventory().add(new ItemStack(ModItems.FISH_BAG.get()));
            Component err = TankInhabitants.takeOut(p, c, f.getUUID(), true);
            h.assertTrue(err == null, "taken out: " + (err == null ? "" : err.getString()));
            boolean found = false;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (s.getItem() instanceof FishBagItem && FishBagItem.containedType(s) == shinyFish) found = true;
            }
            h.assertTrue(found, "the bag holds the shiny fish");
            p.discard();
            e.discard();
            h.succeed();
        });
    }
}
