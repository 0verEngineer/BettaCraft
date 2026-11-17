package org.overengineer.bettacraft.gametest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.command.ForeignCommand;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.foreign.ForeignPlantProfile;
import org.overengineer.bettacraft.plants.foreign.ForeignPlantRole;
import org.overengineer.bettacraft.plants.foreign.ForeignPlants;
import org.overengineer.bettacraft.reef.TankReef;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankNitrate;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;

/**
 * M38 GameTests: plants and corals of Minecraft and other mods in the interior work like a full-grown plant of ours of their
 * growth form times interiorPlants.foreignPlantFactor (nitrate, waste, breeding, hiding, the small animals), without
 * growth and without needs (no light, no fertilizer, no warmth); touching blocks of one profile are one plant, the
 * maxPlantsPerTank limit holds; Fintastic's live rock filters salt water a little; data pack profiles and the warning
 * about renamed ids; the switch; the command; the real plants of Fintastic (always in dev), Regions Unexplored and
 * Twilight Forest ({@code -Pbiomes}) and Alex's Caves ({@code -Paquatic}). Deterministic: the care is stepped directly.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ForeignPlantGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private ForeignPlantGameTests() {}

    // ------------------------------------------------------------------ helpers

    private static TankBlockEntity tank(GameTestHelper h, int x, int z) {
        return AerationGameTests.tank(h, x, 1, z, 2, 1, 1);
    }

    private static ServerLevel interior(GameTestHelper h) {
        ServerLevel l = InteriorManager.interiorLevel(h.getLevel().getServer());
        if (l == null) throw new GameTestAssertException("interior dimension not loaded");
        return l;
    }

    private static InteriorRegion region(GameTestHelper h, TankBlockEntity c) {
        return InteriorManager.ensureRegion(h.getLevel(), c);
    }

    /** The world position of interior-local (x, y, z) (y 0 = the first block above the floor). */
    private static BlockPos at(InteriorRegion r, int x, int y, int z) {
        return new BlockPos(r.originX() + x, InteriorRegion.BASE_Y + y, r.originZ() + z);
    }

    private static void set(ServerLevel in, InteriorRegion r, int x, int y, int z, BlockState s) {
        in.setBlock(at(r, x, y, z), s, 2);
    }

    /** A kelp column of {@code n} blocks (kelp_plant below the kelp tip). */
    private static void kelp(ServerLevel in, InteriorRegion r, int x, int z, int n) {
        for (int y = 0; y < n; y++) set(in, r, x, y, z, (y == n - 1 ? Blocks.KELP : Blocks.KELP_PLANT).defaultBlockState());
    }

    private static void tallSeagrass(ServerLevel in, InteriorRegion r, int x, int z) {
        set(in, r, x, 0, z, Blocks.TALL_SEAGRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
        set(in, r, x, 1, z, Blocks.TALL_SEAGRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private static ForeignPlants.Effect effect(GameTestHelper h, InteriorRegion r) {
        return ForeignPlants.effect(h.getLevel().getServer(), r);
    }

    private static double ref(ForeignPlantRole role, InteriorRegion r) {
        return ForeignPlants.factor() * role.adultMass(r.scale());
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) <= 1e-6 * Math.max(1, Math.abs(b));
    }

    @javax.annotation.Nullable
    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id)).orElse(null);
    }

    /** Places the block (default state) at a free spot of a row and checks it counts with its role. */
    private static void checkReal(GameTestHelper h, List<String[]> plants) {
        ServerLevel in = interior(h);
        TankBlockEntity c = AerationGameTests.tank(h, 2, 1, 2, 4, 1, 4);
        InteriorRegion r = region(h, c);
        int x = 2;
        Map<String, ForeignPlantRole> want = new LinkedHashMap<>();
        for (String[] p : plants) {
            Block b = block(p[0]);
            if (b == null) throw new GameTestAssertException(p[0] + " is not registered (renamed?)");
            set(in, r, x, 0, 4, b.defaultBlockState());
            // the dry interior keeps it (#bettacraft:interior_water_plants, the mixins of M7)
            h.assertTrue(b.defaultBlockState().canSurvive(in, at(r, x, 0, 4)) || p.length > 2, p[0] + " survives in the dry interior");
            want.put(p[0], ForeignPlantRole.byId(p[1]));
            x += 3;
        }
        var e = effect(h, r);
        double mass = 0;
        for (var g : e.groups()) {
            String id = BuiltInRegistries.BLOCK.getKey(g.block()).toString();
            ForeignPlantRole role = want.remove(id);
            h.assertTrue(role == g.role(), id + " counts as " + role + ", not " + g.role());
            h.assertTrue(near(g.mass(), ref(role, r)), id + ": like a full-grown plant of its form times the factor");
            mass += g.mass();
        }
        h.assertTrue(want.isEmpty(), "every plant counts: missing " + want.keySet());
        h.assertTrue(near(InteriorPlants.foreignShare(h.getLevel().getServer(), c.getTank()), mass / Math.pow(r.scale(), 3) / c.getTank().waterBlocks()),
                "their plant mass is the tank's plant share");
    }

    // ------------------------------------------------------------------ vanilla stand-ins

    /**
     * A tank with kelp, tall seagrass and a lily pad in its interior against an empty one: three plants (the kelp column and
     * the two halves of the seagrass are one plant each) like full-grown plants of ours of their form times 0.6; they lower
     * the nitrate, take up waste, give the breeding bonus, hiding spots and food for the small animals, and the lily pad
     * shades the surface. No light at all over the tanks: they need none.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m38_plants", timeoutTicks = 200)
    public static void vanillaPlantsWorkLikeOurs(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerLevel in = interior(h);
        TankBlockEntity bare = tank(h, 2, 2), planted = tank(h, 2, 8);
        InteriorRegion rb = region(h, bare), rp = region(h, planted);
        kelp(in, rp, 4, 4, 6);
        kelp(in, rp, 24, 12, 4);
        tallSeagrass(in, rp, 12, 4);
        set(in, rp, 20, 14, 8, Blocks.LILY_PAD.defaultBlockState());
        var e = effect(h, rp);
        h.assertTrue(e.plants() == 4, "four plants, not " + e.plants() + ": " + e.groups());
        double want = 2 * ref(ForeignPlantRole.STEM, rp) + ref(ForeignPlantRole.RIBBON, rp) + ref(ForeignPlantRole.FLOATING, rp);
        h.assertTrue(near(e.mass(), want), "mass " + e.mass() + " = 0.6 x (stem + ribbon + carpet) " + want);
        h.assertTrue(e.floating() == 1, "the lily pad floats");
        h.assertTrue(effect(h, rb).plants() == 0 && InteriorPlants.volumeShare(level.getServer(), bare.getTank()) == 0, "the bare tank has none");
        double share = InteriorPlants.volumeShare(level.getServer(), planted.getTank());
        h.assertTrue(share > InteriorPlants.BONUS_FROM, "a well planted 2-block tank: " + share);
        h.assertTrue(InteriorPlants.ownShare(level.getServer(), planted.getTank()) == 0, "none of them is ours");
        // the nitrate: falls with the plants, rises without (no animals: leftovers only)
        h.assertTrue(TankNitrate.plants(level, planted) > 0 && TankNitrate.plants(level, bare) == 0, "planted for the nitrate, without light");
        h.assertTrue(TankNitrate.ratePerDay(level, planted) < 0 && TankNitrate.ratePerDay(level, bare) > 0,
                "nitrate falls " + TankNitrate.ratePerDay(level, planted) + " / rises " + TankNitrate.ratePerDay(level, bare));
        // the waste: taken up in the dark
        bare.getTank().setWaterQuality(0.5F);
        planted.getTank().setWaterQuality(0.5F);
        InteriorPlants.step(level, bare, 1.0);
        InteriorPlants.step(level, planted, 1.0);
        h.assertTrue(bare.getTank().waterQuality() == 0.5F && planted.getTank().waterQuality() > 0.5F,
                "waste taken up: " + planted.getTank().waterQuality());
        h.assertTrue(InteriorPlants.uptakePerDay(level, planted, rp) > 0, "the menu shows the uptake");
        // breeding, hiding, the small animals
        h.assertTrue(InteriorPlants.breedingBonus(level.getServer(), planted.getTank()) > 1.0, "breeding bonus of a planted tank");
        h.assertTrue(InteriorPlants.hiding(level.getServer(), planted.getTank()) > 0, "hiding spots");
        h.assertTrue(TankCritters.foodPerDay(level, planted.getTank()) > TankCritters.foodPerDay(level, bare.getTank()), "food for the small animals");
        h.assertTrue(TankCritters.hiding(level, planted.getTank()) > TankCritters.hiding(level, bare.getTank()), "hiding for the young shrimp");
        // shade at the surface
        h.assertTrue(InteriorPlants.surfaceCover(planted.getTank(), rp) > 0 && InteriorPlants.surfaceCover(bare.getTank(), rb) == 0, "the lily pad shades");
        // the menu
        var view = org.overengineer.bettacraft.tank.menu.TankView.read(org.overengineer.bettacraft.tank.menu.TankView.build(MockPlayers.serverPlayer(h), planted));
        h.assertTrue(view.aquatic.foreign == 4 && view.aquatic.foreignMass > 0 && view.aquatic.share > 0, "the plants tab counts them");
        h.succeed();
    }

    /**
     * Vanilla corals (a fan, a coral plant, a wall fan on a coral block) and a sea pickle: corals count; the coral block is a
     * coral of its own; they count in fresh water as in salt water (no needs, no reef elements).
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m38_plants", timeoutTicks = 100)
    public static void vanillaCoralsCountWithoutNeeds(GameTestHelper h) {
        ServerLevel in = interior(h);
        TankBlockEntity c = tank(h, 2, 2);
        InteriorRegion r = region(h, c);
        set(in, r, 4, 0, 4, Blocks.BRAIN_CORAL_FAN.defaultBlockState());
        set(in, r, 8, 0, 4, Blocks.TUBE_CORAL.defaultBlockState());
        set(in, r, 12, 0, 4, Blocks.FIRE_CORAL_BLOCK.defaultBlockState());
        set(in, r, 12, 0, 5, Blocks.FIRE_CORAL_WALL_FAN.defaultBlockState());
        set(in, r, 16, 0, 4, Blocks.SEA_PICKLE.defaultBlockState());
        set(in, r, 20, 0, 4, Blocks.DEAD_BRAIN_CORAL_FAN.defaultBlockState());
        var e = effect(h, r);
        // fan, plant, wall fan (its own profile file, next to the block), the block, the pickle; the dead fan is decoration
        h.assertTrue(e.plants() == 5, "five corals, not " + e.plants() + ": " + e.groups());
        for (var g : e.groups()) h.assertTrue(g.role() == ForeignPlantRole.CORAL, g + " counts as a coral");
        double fresh = InteriorPlants.foreignShare(h.getLevel().getServer(), c.getTank());
        c.getTank().setSalinity(35F);
        double salt = InteriorPlants.foreignShare(h.getLevel().getServer(), c.getTank());
        h.assertTrue(fresh > 0 && near(fresh, salt), "the same in fresh and salt water");
        h.succeed();
    }

    /**
     * They never grow and have no needs: an interior with kelp and Fintastic's hornwort, at 100 % liquid fertilizer, cold
     * and dark, stepped for days: the blocks stay, the fertilizer is not used up by them, and the effect stays the same.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m38_plants", timeoutTicks = 200)
    public static void foreignPlantsNeitherGrowNorNeedAnything(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerLevel in = interior(h);
        TankBlockEntity c = tank(h, 2, 2);
        InteriorRegion r = region(h, c);
        kelp(in, r, 4, 4, 3);
        Block hornwort = block("fintastic:hornwort");
        if (hornwort != null) set(in, r, 10, 0, 4, hornwort.defaultBlockState());
        c.getTank().setWaterTemp(8F);
        c.getTank().setTargetTemp(8F);
        c.getTank().setFertilizer(1F);
        double before = effect(h, r).mass();
        int blocksBefore = InteriorManager.equipment(level.getServer(), r).foreignPlants().size();
        for (int i = 0; i < 20; i++) {
            InteriorPlants.step(level, c, 0.5);
        }
        h.assertTrue(InteriorManager.equipment(level.getServer(), r).foreignPlants().size() == blocksBefore, "no block more or less");
        h.assertTrue(in.getBlockState(at(r, 4, 3, 4)).isAir(), "the kelp did not grow upwards");
        h.assertTrue(near(effect(h, r).mass(), before) && before > 0, "the same effect, cold and dark");
        h.assertTrue(c.getTank().fertilizer() >= 0.99F, "the fertilizer is not theirs: " + c.getTank().fertilizer());
        h.assertTrue(r.plants().isEmpty(), "they are not in the index of our plants");
        h.succeed();
    }

    /**
     * Touching blocks of one profile are one plant, blocks of different profiles next to each other two; at most
     * maxPlantsPerTank count (config restored); the factor scales them (0: no effect).
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m38_config", timeoutTicks = 100)
    public static void plantsGroupAndTheLimitAndFactorHold(GameTestHelper h) {
        ServerLevel in = interior(h);
        TankBlockEntity c = tank(h, 2, 2);
        InteriorRegion r = region(h, c);
        kelp(in, r, 4, 4, 8);
        kelp(in, r, 5, 4, 2); // touches the first column: the same plant
        set(in, r, 4, 0, 5, Blocks.SEAGRASS.defaultBlockState()); // touches the kelp, but another profile: a plant of its own
        for (int i = 0; i < 4; i++) set(in, r, 10 + 2 * i, 0, 10, Blocks.SEAGRASS.defaultBlockState()); // four apart
        int cap = ServerConfig.getOr(ServerConfig.AQUATIC_MAX_PER_TANK, 48);
        double factor = ForeignPlants.factor();
        try {
            var e = effect(h, r);
            h.assertTrue(e.plants() == 6, "kelp (10 blocks) + 5 seagrass: " + e.groups());
            h.assertTrue(e.groups().stream().anyMatch(g -> g.blocks() == 10 && g.role() == ForeignPlantRole.STEM), "the kelp is one plant of 10 blocks");
            ServerConfig.AQUATIC_MAX_PER_TANK.set(4);
            e = effect(h, r);
            h.assertTrue(e.plants() == 4 && e.overLimit() == 2, "the limit: 4 count, 2 over: " + e.plants() + "/" + e.overLimit());
            ServerConfig.AQUATIC_MAX_PER_TANK.set(cap);
            double full = effect(h, r).mass();
            ServerConfig.FOREIGN_PLANT_FACTOR.set(1.2);
            h.assertTrue(near(effect(h, r).mass(), full * 2), "twice the factor, twice the mass");
            ServerConfig.FOREIGN_PLANT_FACTOR.set(0.0);
            h.assertTrue(effect(h, r).mass() == 0 && InteriorPlants.foreignShare(h.getLevel().getServer(), c.getTank()) == 0, "factor 0: no effect");
        } finally {
            ServerConfig.AQUATIC_MAX_PER_TANK.set(cap);
            ServerConfig.FOREIGN_PLANT_FACTOR.set(factor);
        }
        h.succeed();
    }

    /** features.aquaticPlants.foreignPlants off: decoration only (no share, no uptake, no shade, no live rock); on again: back. */
    @GameTest(template = EMPTY, batch = "bettacraft_m38_config", timeoutTicks = 100)
    public static void foreignPlantSwitchOffMakesThemDecoration(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerLevel in = interior(h);
        TankBlockEntity c = tank(h, 2, 2);
        InteriorRegion r = region(h, c);
        kelp(in, r, 4, 4, 4);
        set(in, r, 8, 14, 4, Blocks.LILY_PAD.defaultBlockState());
        boolean on = Features.own(Feature.FOREIGN_PLANTS);
        try {
            h.assertTrue(InteriorPlants.foreignShare(level.getServer(), c.getTank()) > 0, "on: they count");
            Features.set(Feature.FOREIGN_PLANTS, false);
            h.assertTrue(!Features.enabled(Feature.FOREIGN_PLANTS), "off");
            h.assertTrue(effect(h, r).plants() == 0 && InteriorPlants.volumeShare(level.getServer(), c.getTank()) == 0, "off: no plant share");
            c.getTank().setWaterQuality(0.5F);
            InteriorPlants.step(level, c, 1.0);
            h.assertTrue(c.getTank().waterQuality() == 0.5F, "off: no uptake");
            h.assertTrue(InteriorPlants.surfaceCover(c.getTank(), r) == 0 && TankNitrate.plants(level, c) == 0, "off: no shade, no nitrate uptake");
            h.assertTrue(InteriorManager.equipment(level.getServer(), r).foreignPlants().size() == 5, "the blocks stay in the index");
            Features.set(Feature.FOREIGN_PLANTS, true);
            h.assertTrue(effect(h, r).plants() == 2, "on again");
        } finally {
            Features.set(Feature.FOREIGN_PLANTS, on);
        }
        h.succeed();
    }

    /**
     * Fintastic's live rock in a salt water tank filters a little (times the factor of our live rock); in fresh water and
     * with dry rock nothing; corals can be glued onto it.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m38_plants", timeoutTicks = 100)
    public static void fintasticLiveRockIsASmallFilter(GameTestHelper h) {
        Block live = block("fintastic:live_rock"), dead = block("fintastic:dead_live_rock");
        if (live == null || dead == null) {
            h.succeed(); // -PnoCompat
            return;
        }
        ServerLevel level = h.getLevel();
        ServerLevel in = interior(h);
        TankBlockEntity ours = tank(h, 2, 2), theirs = tank(h, 2, 8), dry = tank(h, 2, 14);
        InteriorRegion ro = region(h, ours), rt = region(h, theirs), rd = region(h, dry);
        for (int i = 0; i < 64; i++) {
            set(in, ro, 2 + i % 8, i / 8, 4, ModBlocks.LIVE_ROCK.get().defaultBlockState());
            set(in, rt, 2 + i % 8, i / 8, 4, live.defaultBlockState());
            set(in, rd, 2 + i % 8, i / 8, 4, dead.defaultBlockState());
        }
        double freshTheirs = TankReef.liveRockBlocks(level, theirs.getTank());
        for (TankBlockEntity c : List.of(ours, theirs, dry)) c.getTank().setSalinity(35F);
        double o = TankReef.liveRockBlocks(level, ours.getTank()), t = TankReef.liveRockBlocks(level, theirs.getTank()), d = TankReef.liveRockBlocks(level, dry.getTank());
        h.assertTrue(freshTheirs == 0, "no filter in fresh water");
        h.assertTrue(o > 0 && near(t, o * ForeignPlants.factor()), "theirs filter 0.6 x ours: " + t + " / " + o);
        h.assertTrue(d == 0, "dry rock is decoration");
        h.assertTrue(effect(h, rt).plants() == 0 && effect(h, rt).liveRock() == 64, "live rock is no plant");
        h.assertTrue(live.defaultBlockState().is(org.overengineer.bettacraft.registry.ModTags.REEF_SURFACES), "corals grow on it");
        h.succeed();
    }

    /**
     * Data pack profiles: a later file replaces a default (kelp as decoration with "role": "none"), tags work, a missing id of
     * a loaded mod is warned about as renamed, one of a missing mod quietly skipped; the interior counts again when other
     * blocks have a profile (the signature). The loaded profiles are restored.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m38_config", timeoutTicks = 100)
    public static void dataPackProfilesReplaceAndWarn(GameTestHelper h) {
        ServerLevel in = interior(h);
        TankBlockEntity c = tank(h, 2, 2);
        InteriorRegion r = region(h, c);
        kelp(in, r, 4, 4, 3);
        set(in, r, 10, 0, 4, Blocks.FERN.defaultBlockState()); // no water plant: no profile
        set(in, r, 14, 0, 4, Blocks.SEAGRASS.defaultBlockState());
        var saved = ForeignPlants.files();
        try {
            h.assertTrue(effect(h, r).plants() == 2, "kelp and seagrass by default");
            Map<Identifier, ForeignPlantProfile> files = new LinkedHashMap<>(saved);
            files.put(BettaCraft.id("zz_kelp_off"), new ForeignPlantProfile(List.of(Identifier.parse("minecraft:kelp"), Identifier.parse("minecraft:kelp_plant")),
                    List.of(), ForeignPlantRole.NONE, 1, false));
            files.put(BettaCraft.id("zz_ferns"), new ForeignPlantProfile(List.of(), List.of(Identifier.parse("minecraft:replaceable_by_trees")),
                    ForeignPlantRole.MOSS, 0.5, false));
            int warned = ForeignPlants.renamedWarnings;
            boolean fintastic = org.overengineer.bettacraft.platform.Platform.get().isModLoaded("fintastic");
            files.put(BettaCraft.id("zz_missing"), new ForeignPlantProfile(List.of(Identifier.parse("fintastic:hornwart"),
                    Identifier.parse("nosuchmod:weed")), List.of(), ForeignPlantRole.STEM, 1, false));
            List<Identifier> missing = ForeignPlants.set(files);
            h.assertTrue(missing.contains(Identifier.parse("nosuchmod:weed")) && missing.contains(Identifier.parse("fintastic:hornwart")), "missing ids: " + missing);
            h.assertTrue(ForeignPlants.renamedWarnings == warned + (fintastic ? 1 : 0), "the renamed Fintastic id is warned about (once)");
            var e = effect(h, r); // the signature changed: the interior was counted again
            h.assertTrue(e.plants() == 2, "the fern (from the tag) and the seagrass, the kelp is decoration now: " + e.groups());
            h.assertTrue(e.groups().stream().anyMatch(g -> g.block() == Blocks.FERN && near(g.mass(), 0.5 * ref(ForeignPlantRole.MOSS, r))), "the fern as half a moss");
        } finally {
            ForeignPlants.set(saved);
        }
        h.assertTrue(effect(h, r).plants() == 2 && effect(h, r).groups().stream().noneMatch(g -> g.block() == Blocks.FERN), "restored");
        h.succeed();
    }

    /** {@code /bettacraft foreign plants}: the rows by block with role and profile, and the plant blocks of other mods without one. */
    @GameTest(template = EMPTY, batch = "bettacraft_m38_plants", timeoutTicks = 100)
    public static void commandListsTheForeignPlants(GameTestHelper h) {
        ServerLevel in = interior(h);
        TankBlockEntity c = tank(h, 2, 2);
        InteriorRegion r = region(h, c);
        kelp(in, r, 4, 4, 3);
        kelp(in, r, 8, 4, 2);
        set(in, r, 12, 0, 4, Blocks.SEAGRASS.defaultBlockState());
        Block fossil = block("fintastic:fossil_stromatolite_growths"); // a Fintastic plant block without a profile
        if (fossil != null) set(in, r, 16, 0, 4, fossil.defaultBlockState());
        var rows = ForeignCommand.plantRows(h.getLevel(), c);
        h.assertTrue(rows.size() == 2, "kelp and seagrass rows: " + rows);
        var kelp = rows.stream().filter(x -> x.block().getPath().equals("kelp")).findFirst().orElseThrow(() -> new GameTestAssertException("no kelp row: " + rows));
        h.assertTrue(kelp.plants() == 2 && kelp.blocks() == 5 && kelp.role().equals("stem") && kelp.profile().equals(BettaCraft.id("minecraft_kelp")),
                "the kelp row: " + kelp);
        var un = ForeignCommand.unprofiledPlants(h.getLevel(), c);
        if (fossil != null) h.assertTrue(un.getOrDefault(Identifier.parse("fintastic:fossil_stromatolite_growths"), 0) == 1, "without a profile: " + un);
        h.assertTrue(!un.containsKey(Identifier.parse("minecraft:kelp")), "vanilla blocks are not listed as missing");
        h.succeed();
    }

    // ------------------------------------------------------------------ the real plants of other mods

    /** Fintastic (always in dev unless -PnoCompat): every plant of the defaults counts with its role. */
    @GameTest(template = EMPTY, batch = "bettacraft_m38_plants", timeoutTicks = 100)
    public static void fintasticPlantsCount(GameTestHelper h) {
        if (block("fintastic:hornwort") == null) {
            h.succeed();
            return;
        }
        checkReal(h, List.of(new String[]{"fintastic:anubias", "epiphyte"}, new String[]{"fintastic:hornwort", "stem"},
                new String[]{"fintastic:amazon_sword", "rosette"}, new String[]{"fintastic:duckweed", "floating"},
                new String[]{"fintastic:lotus_pad", "floating"}, new String[]{"fintastic:aquatic_moss_phyllid", "moss"},
                new String[]{"fintastic:green_algae_carpet", "carpet"}, new String[]{"fintastic:red_algae_fan", "moss"},
                new String[]{"fintastic:dragons_breath_algae", "moss"}, new String[]{"fintastic:caulerpa", "ribbon"},
                new String[]{"fintastic:sea_grapes", "stem"}));
        h.succeed();
    }

    /** {@code -Pbiomes}: Regions Unexplored's cattail, duckweed and lily pads, Twilight Forest's huge lily pad and water lily. */
    @GameTest(template = EMPTY, batch = "bettacraft_m38_plants", timeoutTicks = 100)
    public static void biomeModPlantsCount(GameTestHelper h) {
        boolean ru = block("regions_unexplored:cattail") != null, tf = block("twilightforest:huge_lily_pad") != null;
        if (Boolean.getBooleanOr("bettacraft.biomemods", false) && !(ru && tf)) throw new GameTestAssertException("-Pbiomes but Regions Unexplored / Twilight Forest are not loaded");
        if (!ru && !tf) {
            h.succeed();
            return;
        }
        List<String[]> plants = new java.util.ArrayList<>();
        if (ru) {
            plants.add(new String[]{"regions_unexplored:cattail", "ribbon"});
            plants.add(new String[]{"regions_unexplored:duckweed", "floating"});
            plants.add(new String[]{"regions_unexplored:flowering_lily_pad", "floating"});
            plants.add(new String[]{"regions_unexplored:giant_lily_pad", "floating"});
        }
        if (tf) {
            plants.add(new String[]{"twilightforest:huge_lily_pad", "floating"});
            plants.add(new String[]{"twilightforest:huge_water_lily", "floating"});
        }
        checkReal(h, plants);
        h.succeed();
    }

    /** {@code -Paquatic}: Alex's Caves' ping-pong sponge counts as a coral. */
    @GameTest(template = EMPTY, batch = "bettacraft_m38_plants", timeoutTicks = 100)
    public static void alexsCavesSpongeCounts(GameTestHelper h) {
        boolean ac = block("alexscaves:ping_pong_sponge") != null;
        if (Boolean.getBooleanOr("bettacraft.aquaticmods", false) && !ac) throw new GameTestAssertException("-Paquatic but Alex's Caves is not loaded");
        if (!ac) {
            h.succeed();
            return;
        }
        checkReal(h, List.<String[]>of(new String[]{"alexscaves:ping_pong_sponge", "coral"}));
        h.succeed();
    }
}
