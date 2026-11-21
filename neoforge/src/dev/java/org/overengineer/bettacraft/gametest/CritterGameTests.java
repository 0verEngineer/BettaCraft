package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterObject;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.entity.frog.DwarfFrogEntity;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankFrogs;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.algae.AlgaeSim;
import org.overengineer.bettacraft.tank.algae.TankAlgae;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;

/**
 * M14 GameTests: shrimp, crayfish, snails, dwarf clawed frogs and algae in real tanks. Breeding and genetics (colour,
 * grade, fall back to the wild form), nerites never breed, apple snail clutches above the water line, water
 * sensitivity, bettas hunting and the hiding spots of plants and moss, algae growing and stopped by snails, the scraper,
 * the stocking config, the net in the biomes (fillbiome), the trader, mud, putting in / taking out with bags and the net,
 * merging tanks, frogs breathing (and drowning without air) and eating.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CritterGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private CritterGameTests() {}

    // ------------------------------------------------------------------ helpers

    static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz, long water) {
        for (int a = 0; a < sx; a++)
            for (int b = 0; b < sy; b++)
                for (int c = 0; c < sz; c++) {
                    BlockPos p = new BlockPos(x + a, y + b, z + c);
                    h.setBlock(p, ModBlocks.TANK.get().defaultBlockState());
                    TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(p), null);
                }
        TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(new BlockPos(x, y, z)));
        if (c == null) throw new GameTestFailure("tank did not form");
        if (water > 0) TankInteractions.changeWater(h.getLevel(), c, water);
        c.getTank().setWaterTemp(25F);
        c.getTank().setTargetTemp(25F);
        return c;
    }

    private static Critter add(GameTestHelper h, TankBlockEntity c, CritterSpecies s, CritterGenes g, boolean female) {
        Critter k = TankCritters.add(h.getLevel(), c, s, g, female, s.juvenileDays() + 1);
        if (k == null) throw new GameTestFailure("tank full");
        return k;
    }

    /** {@code days} of care in steps of an hour (movement, life, algae; the temperature stays). */
    private static void days(GameTestHelper h, TankBlockEntity c, double days, boolean feed) {
        int steps = (int) Math.round(days * 24);
        for (int i = 0; i < steps; i++) {
            if (feed) c.getTank().setCritterFood(50);
            c.getTank().setWaterTemp(25F);
            TankAlgae.step(h.getLevel(), c, 1.0 / 24);
            TankCritters.step(h.getLevel(), c, 1.0 / 24);
            // more seconds of walking per hour (the care loop moves them every second)
            var terrain = TankCritters.terrain(h.getLevel(), c.getTank());
            var rnd = new java.util.Random(i);
            for (int m = 0; m < 15; m++) for (Critter k : c.getTank().critters()) org.overengineer.bettacraft.critters.CritterMovement.step(k, terrain, rnd);
        }
    }

    private static long count(TankData t, CritterSpecies s) {
        return t.critters().stream().filter(k -> k.alive() && k.species == s).count();
    }

    private static String debug(TankData t) {
        StringBuilder b = new StringBuilder("food " + t.critterFood() + " temp " + t.waterTemp() + " q " + t.waterQuality() + ":");
        for (Critter k : t.critters()) b.append(' ').append(k.species.id()).append(k.female ? "F" : "M").append(" age=").append(String.format("%.1f", k.age))
                .append(" h=").append(String.format("%.2f", k.hunger)).append(" eggs=").append(String.format("%.1f", k.eggs)).append(" dead=").append(k.dead)
                .append(" face=").append(k.face).append(" y=").append(String.format("%.2f", k.y)).append(" gen=").append(k.generation);
        return b.toString();
    }

    private static boolean logged(TankData t, String key) {
        return t.log().stream().anyMatch(e -> e.key().equals(key));
    }

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        sp.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        sp.getInventory().clearContent();
        sp.getInventory().setSelectedSlot(8); // the hand is the last hotbar slot: items given go to the first slots
        return sp;
    }

    private static void use(ServerPlayer sp, BlockPos pos, Direction face, ItemStack stack) {
        sp.setItemInHand(InteractionHand.MAIN_HAND, stack);
        Vec3 hit = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        sp.gameMode.useItemOn(sp, sp.level(), stack, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, pos, false));
    }

    private static void biome(ServerLevel level, BlockPos at, String biome) {
        for (int x = at.getX() - 4; x <= at.getX() + 4; x += 4) for (int z = at.getZ() - 4; z <= at.getZ() + 4; z += 4) level.getChunk(x >> 4, z >> 4);
        var src = level.getServer().createCommandSourceStack().withSuppressedOutput().withLevel(level);
        level.getServer().getCommands().performPrefixedCommand(src, "fillbiome " + (at.getX() - 4) + " " + (at.getY() - 3) + " " + (at.getZ() - 4) + " "
                + (at.getX() + 4) + " " + (at.getY() + 4) + " " + (at.getZ() + 4) + " " + biome);
    }

    // ------------------------------------------------------------------ breeding and genetics

    /** A red pair breeds red young (berried female, young grow up); red x blue gives wild-coloured young that carry both. */
    @GameTest(template = EMPTY, timeoutTicks = 400, batch = "bettacraft_critters_no_mutations")
    public static void shrimpBreedTrueAndMixedColoursFallBack(GameTestHelper h) {
        // M22.4: without the spontaneous mutations of M22.3 (0.3 % per gene and birth: with six colour genes and twenty young
        // a mutated one now and then made this test flaky); the mutations have their own test (WildlifeGameTests)
        boolean mutations = Features.own(Feature.MUTATIONS);
        Features.set(Feature.MUTATIONS, false);
        try {
            shrimpBreedTrue(h);
        } finally {
            Features.set(Feature.MUTATIONS, mutations);
        }
    }

    private static void shrimpBreedTrue(GameTestHelper h) {
        TankBlockEntity red = tank(h, 2, 2, 2, 3, 2, 2, 12000);
        var watcher = QuestAdv.watcher(h, red); // M45
        add(h, red, CritterSpecies.NEOCARIDINA, CritterGenes.pure(0, 0.5F), true);
        add(h, red, CritterSpecies.NEOCARIDINA, CritterGenes.pure(0, 0.5F), false);
        TankBlockEntity mix = tank(h, 8, 2, 2, 3, 2, 2, 12000);
        add(h, mix, CritterSpecies.NEOCARIDINA, CritterGenes.pure(0, 0.5F), true);
        add(h, mix, CritterSpecies.NEOCARIDINA, CritterGenes.pure(1, 0.5F), false);
        boolean berried = false;
        // M22.5: up to 40 days (was 20: a pair breeds at 0.35 a day, about 1 run in 1,300 had no brood; 3 of 4,000 pairs in a
        // diagnosis run, once in 60 full GameTest runs)
        for (int d = 0; d < 40 && !(count(red.getTank(), CritterSpecies.NEOCARIDINA) > 2 && count(mix.getTank(), CritterSpecies.NEOCARIDINA) > 2); d++) {
            days(h, red, 1, true);
            days(h, mix, 1, true);
            berried |= red.getTank().critters().stream().anyMatch(Critter::berried);
        }
        h.assertTrue(berried || count(red.getTank(), CritterSpecies.NEOCARIDINA) > 2, "a female carried eggs: " + debug(red.getTank()));
        h.assertTrue(logged(red.getTank(), "log.bettacraft.critters_born"), "the log names the young");
        QuestAdv.check(h, watcher, "critters_bred"); // M45
        QuestAdv.gone(h, watcher);
        for (Critter k : red.getTank().critters()) {
            if (k.generation == 1) h.assertTrue(k.genes.shows(0), "red x red gives red young");
        }
        // M17: an early F1 son can already mate with his red mother (a backcross, also "generation 1"): its young are
        // red/red or red/wild and blue/wild or wild/wild. So: every young carries red from the mother, every one that
        // carries blue and red only once is wild-coloured. M22.5: a backcross young red/red with one blue shows red (it
        // was counted as F1 and failed the test now and then, 1 of 20 local runs)
        long f1 = 0;
        for (Critter k : mix.getTank().critters()) {
            if (k.generation != 1) continue;
            h.assertTrue(k.genes.carries(0), "every young carries red from the mother");
            if (!k.genes.carries(1) || k.genes.shows(0)) continue;
            f1++;
            h.assertTrue("wild".equals(k.genes.variety(CritterSpecies.NEOCARIDINA)), "F1 of red x blue is wild-coloured");
        }
        h.assertTrue(f1 > 0, "F1 hatched (wild, carrying both)");
        // the young grow up
        days(h, red, 4, true);
        h.assertTrue(red.getTank().critters().stream().anyMatch(k -> k.generation == 1 && k.adult()), "the young grew up");
        h.succeed();
    }

    /** Nerites never breed; ramshorns slowly (one is enough); an apple snail pair lays its clutch above the water line. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void neritesNeverAppleClutchAboveTheWater(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 2, 2, 3, 2, 2, 12000);
        for (int i = 0; i < 6; i++) add(h, c, CritterSpecies.ZEBRA_NERITE, CritterGenes.WILD, i % 2 == 0);
        Critter mother = add(h, c, CritterSpecies.APPLE_SNAIL, CritterGenes.pure(0, 0.4F), true);
        add(h, c, CritterSpecies.APPLE_SNAIL, CritterGenes.pure(0, 0.4F), false);
        // ready to lay (the chance per day to get there is unit-tested): she has to find the glass and climb above the water
        mother.eggs = 0;
        mother.father = CritterGenes.pure(0, 0.4F);
        TankData t = c.getTank();
        h.assertTrue(TankFrogs.hasAir(t), "the default air gap is there");
        // the nerites go up the glass (a few minutes of walking)
        {
            var terrain = TankCritters.terrain(h.getLevel(), t);
            var rnd = new java.util.Random(4);
            int glass = 0;
            StringBuilder trace = new StringBuilder();
            for (int s = 0; s < 2000; s++) {
                for (Critter k : t.critters()) {
                    org.overengineer.bettacraft.critters.CritterMovement.step(k, terrain, rnd);
                    if (k.onGlass()) glass++;
                }
                if (s % 200 == 0) trace.append(" | ").append(t.critters().getFirst().debug());
            }
            h.assertTrue(glass > 0, "the snails climb the glass:" + trace + " ground " + terrain.ground(t.critters().getFirst().x, t.critters().getFirst().z)
                    + " surface " + terrain.surface());
        }
        CritterObject clutch = null;
        StringBuilder trace = new StringBuilder();
        for (int d = 0; d < 40 && clutch == null; d++) {
            days(h, c, 0.5, true);
            for (CritterObject o : t.critterObjects()) if (o.kind == CritterObject.Kind.CLUTCH) clutch = o;
            if (d % 5 == 0) for (Critter k : t.critters()) if (k.species == CritterSpecies.APPLE_SNAIL && k.female) trace.append(" | ").append(k.debug()).append(" eggs ").append(k.eggs);
        }
        if (clutch == null) throw new GameTestFailure("no clutch:" + trace);
        h.assertTrue(clutch != null, "an apple snail laid a clutch: " + debug(t));
        h.assertTrue(clutch.y > t.waterSurfaceY(), "above the water line: " + clutch.y + " > " + t.waterSurfaceY());
        h.assertTrue(clutch.face != Critter.FLOOR, "on the glass");
        h.assertTrue(logged(t, "log.bettacraft.clutch_laid"), "log");
        h.assertTrue(count(t, CritterSpecies.ZEBRA_NERITE) == 6, "nerites never breed");
        long before = count(t, CritterSpecies.APPLE_SNAIL);
        days(h, c, 3.5, true);
        h.assertTrue(count(t, CritterSpecies.APPLE_SNAIL) > before, "the clutch hatched");
        h.assertTrue(count(t, CritterSpecies.ZEBRA_NERITE) == 6, "still six nerites");
        // the menu button takes clutches off
        t.critterObjects().add(CritterObject.load(clutch.save()));
        h.assertTrue(TankCritters.removeClutches(h.getLevel(), c) >= 1, "clutch removed");
        h.assertTrue(t.critterObjects().stream().noneMatch(o -> o.kind == CritterObject.Kind.CLUTCH), "no clutch left");
        h.succeed();
    }

    /** Pink ramshorns from two wild carriers, then pure. */
    @GameTest(template = EMPTY, timeoutTicks = 400, batch = "bettacraft_critters_no_mutations")
    public static void pinkRamshornsBredPure(GameTestHelper h) {
        // M22.5: without the spontaneous mutations of M22.3 like the shrimp test (a pink allele mutated back to the wild
        // one gives a wild-coloured young in the pure pink tank; 1 of 40 local runs)
        boolean mutations = Features.own(Feature.MUTATIONS);
        Features.set(Feature.MUTATIONS, false);
        try {
            pinkRamshorns(h);
        } finally {
            Features.set(Feature.MUTATIONS, mutations);
        }
    }

    private static void pinkRamshorns(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 2, 2, 3, 2, 2, 12000);
        add(h, c, CritterSpecies.RAMSHORN, new CritterGenes(1, 0, 0.1F), true);
        add(h, c, CritterSpecies.RAMSHORN, new CritterGenes(0, 1, 0.1F), true);
        add(h, c, CritterSpecies.RAMSHORN, new CritterGenes(1, 0, 0.1F), true);
        TankData t = c.getTank();
        // M22.5: up to 100 days (was 40: snails breed slowly, many young carry no pink, 1 of 1,000 diagnosis runs and once
        // in 30 full GameTest runs had no pink young after 40 days)
        // M52.5: up to 200 days (no pink young in 100 days once on CI); it stops at the first pink one
        for (int d = 0; d < 200 && t.critters().stream().noneMatch(k -> "pink".equals(k.genes.variety(CritterSpecies.RAMSHORN))); d++) days(h, c, 1, true);
        List<Critter> pink = t.critters().stream().filter(k -> "pink".equals(k.genes.variety(CritterSpecies.RAMSHORN))).toList();
        h.assertTrue(!pink.isEmpty(), "pink young from red-brown carriers: " + debug(t));
        // a tank of pink ones only breeds pink
        TankBlockEntity p = tank(h, 8, 2, 2, 3, 2, 2, 12000);
        add(h, p, CritterSpecies.RAMSHORN, CritterGenes.pure(0, 0.4F), true);
        add(h, p, CritterSpecies.RAMSHORN, CritterGenes.pure(0, 0.4F), true);
        // M52.5: up to 100 days as above (two snails had no young in 40 days once on CI)
        for (int d = 0; d < 100 && count(p.getTank(), CritterSpecies.RAMSHORN) < 4; d++) days(h, p, 1, true);
        h.assertTrue(count(p.getTank(), CritterSpecies.RAMSHORN) > 2, "they bred");
        h.assertTrue(p.getTank().critters().stream().allMatch(k -> "pink".equals(k.genes.variety(CritterSpecies.RAMSHORN))), "pink x pink is pink");
        h.succeed();
    }

    // ------------------------------------------------------------------ water, bettas, hiding

    /** Shrimp die in poor water, snails take it; the body lies there and is removed (the log says why). */
    @GameTest(template = EMPTY)
    public static void shrimpAreSensitiveSnailsAreNot(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 2, 2, 3, 2, 2, 12000);
        for (int i = 0; i < 15; i++) add(h, c, CritterSpecies.NEOCARIDINA, CritterGenes.WILD, i % 2 == 0);
        for (int i = 0; i < 10; i++) add(h, c, CritterSpecies.BLADDER_SNAIL, CritterGenes.WILD, false);
        TankData t = c.getTank();
        for (int i = 0; i < 72; i++) {
            t.setWaterQuality(0.3F);
            t.setCritterFood(50);
            TankCritters.step(h.getLevel(), c, 1.0 / 24);
        }
        // M22.5: only the 15 shrimp put in (a brood born late in the three days could lift the count to 10 or more:
        // 1 of 20 local runs failed with "14 left")
        long left = t.critters().stream().filter(k -> k.alive() && k.species == CritterSpecies.NEOCARIDINA && k.generation == 0).count();
        h.assertTrue(left < 10, "shrimp died: " + left + " of 15 left (" + count(t, CritterSpecies.NEOCARIDINA) + " with young)");
        h.assertTrue(count(t, CritterSpecies.BLADDER_SNAIL) >= 10, "bladder snails live: " + count(t, CritterSpecies.BLADDER_SNAIL));
        h.assertTrue(logged(t, "log.bettacraft.critters_died"), "death logged");
        h.succeed();
    }

    /** A betta eats the young shrimp in a bare tank; dense plants and Java moss hide them. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void bettasHuntYoungShrimpPlantsHideThem(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity bare = tank(h, 2, 2, 2, 2, 1, 1, 2000);
        TankBlockEntity planted = tank(h, 8, 2, 2, 2, 1, 1, 2000);
        for (TankBlockEntity c : List.of(bare, planted)) {
            BettaData d = BettaData.create(BettaGenome.of(java.util.Map.of("body_color", "red"), BettaGenome.DEFAULT), false, level.getGameTime());
            var err = TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d), ModEntities.BETTA.get(), null, 0L);
            h.assertTrue(err == null, "betta in: " + (err == null ? "" : err.getString()));
        }
        // the planted tank: Java moss and stem plants up to the surface in the interior
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion r = InteriorManager.ensureRegion(level, planted);
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.GRAVEL.defaultBlockState(), false);
        long seed = 1400;
        for (int x = 3; x < r.sizeX() - 2; x += 5) {
            InteriorPlants.plantAt(in, r, PlantSpecies.ROTALA, x, 0, 4, PlantSpecies.ROTALA.stages(), seed++);
            InteriorPlants.plantAt(in, r, PlantSpecies.VALLISNERIA, x + 2, 0, 11, PlantSpecies.VALLISNERIA.stages(), seed++);
        }
        InteriorPlants.plantAt(in, r, PlantSpecies.JAVA_MOSS, 8, 0, 8, PlantSpecies.JAVA_MOSS.stages(), seed++);
        InteriorPlants.plantAt(in, r, PlantSpecies.JAVA_MOSS, 22, 0, 8, PlantSpecies.JAVA_MOSS.stages(), seed);
        double hideBare = TankCritters.hiding(level, bare.getTank()), hidePlanted = TankCritters.hiding(level, planted.getTank());
        h.assertTrue(hideBare < 0.05, "no hiding spots in a bare tank: " + hideBare);
        h.assertTrue(hidePlanted > 0.5, "dense plants and moss hide them: " + hidePlanted);
        for (TankBlockEntity c : List.of(bare, planted)) {
            for (int i = 0; i < 40; i++) TankCritters.add(level, c, CritterSpecies.NEOCARIDINA, CritterGenes.WILD, i % 2 == 0, 0);
        }
        for (int i = 0; i < 60; i++) {
            for (TankBlockEntity c : List.of(bare, planted)) {
                c.getTank().setCritterFood(50);
                c.getTank().setWaterQuality(1F);
                TankCritters.step(level, c, 1.0 / 24);
            }
        }
        long leftBare = count(bare.getTank(), CritterSpecies.NEOCARIDINA), leftPlanted = count(planted.getTank(), CritterSpecies.NEOCARIDINA);
        h.assertTrue(leftBare < 22, "the betta ate young shrimp: " + leftBare + " of 40 left");
        h.assertTrue(leftPlanted > leftBare + 5, "more survive in the plants: " + leftPlanted + " vs " + leftBare);
        h.assertTrue(logged(bare.getTank(), "log.bettacraft.critter_eaten_young"), "logged");
        h.succeed();
    }

    // ------------------------------------------------------------------ algae

    /** Algae grow under a light with waste; three nerites keep another tank clean; the scraper cleans a face and a side. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void algaeGrowSnailsStopThemScraperCleans(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity dirty = tank(h, 2, 2, 2, 4, 2, 2, 16000);
        TankBlockEntity clean = tank(h, 10, 2, 2, 4, 2, 2, 16000);
        for (int x = 0; x < 4; x++) {
            h.setBlock(new BlockPos(2 + x, 4, 2), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.LIT, true));
            h.setBlock(new BlockPos(10 + x, 4, 2), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.LIT, true));
        }
        for (int i = 0; i < 3; i++) add(h, clean, CritterSpecies.ZEBRA_NERITE, CritterGenes.WILD, i == 0);
        h.assertTrue(TankAlgae.growthPerDay(level, dirty) > 0, "algae grow under the light");
        for (int i = 0; i < 8 * 24; i++) {
            for (TankBlockEntity c : List.of(dirty, clean)) {
                c.getTank().setWaterQuality(0.5F);
                c.getTank().setCritterFood(5);
                TankAlgae.step(level, c, 1.0 / 24);
                TankCritters.step(level, c, 1.0 / 24);
            }
        }
        double d = TankAlgae.mean(dirty.getTank()), cl = TankAlgae.mean(clean.getTank());
        h.assertTrue(d > 0.8, "green glass without cleaners: " + d);
        h.assertTrue(cl < 0.1, "three nerites keep it clean: " + cl);
        h.assertTrue(logged(dirty.getTank(), "log.bettacraft.algae_growing"), "log");
        // the scraper on the north face of the first block, like a player from outside
        ServerPlayer sp = player(h);
        BlockPos face = h.absolutePos(new BlockPos(2, 2, 2));
        sp.teleportTo(level, face.getX() + 0.5, face.getY(), face.getZ() - 2.0, java.util.Set.of(), 0F, 0F, false);
        ItemStack scraper = new ItemStack(ModItems.ALGAE_SCRAPER.get());
        use(sp, face, Direction.NORTH, scraper);
        TankData t = dirty.getTank();
        h.assertTrue(AlgaeSim.get(t.algae(), face.asLong(), Direction.NORTH.get2DDataValue()) == 0, "the face is clean");
        h.assertTrue(AlgaeSim.get(t.algae(), face.above().asLong(), Direction.NORTH.get2DDataValue()) > 0.5, "the face above is not");
        h.assertTrue(scraper.getDamageValue() == 1, "a little wear");
        QuestAdv.check(h, sp, "algae_scraped"); // M45
        sp.setShiftKeyDown(true);
        use(sp, face.above(), Direction.NORTH, scraper);
        sp.setShiftKeyDown(false);
        for (long l : t.members()) {
            if (!t.contains(BlockPos.of(l).north())) h.assertTrue(AlgaeSim.get(t.algae(), l, Direction.NORTH.get2DDataValue()) == 0, "whole north side clean");
        }
        h.assertTrue(AlgaeSim.get(t.algae(), face.asLong(), Direction.WEST.get2DDataValue()) > 0.5, "the west side keeps its algae");
        h.succeed();
    }

    // ------------------------------------------------------------------ stocking

    /** 25 shrimp count as one fish (config), the cap counts them, bettas can be kept out by a shrimp colony. */
    @GameTest(template = EMPTY, batch = "bettacraft_critters_stocking")
    public static void stockingCountsSmallAnimals(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        int saved = ServerConfig.STOCKING_SHRIMP_PER_FISH.get();
        int savedCap = ServerConfig.TANK_HARD_CAP_PER_WATER_BLOCK.get();
        try {
            TankBlockEntity c = tank(h, 2, 2, 2, 1, 1, 1, 1000);
            for (int i = 0; i < 25; i++) add(h, c, CritterSpecies.NEOCARIDINA, CritterGenes.WILD, i % 2 == 0);
            // M18: young ones count at the share of a juvenile betta (0.1) of their share of a fish
            for (int i = 0; i < 10; i++) TankCritters.add(level, c, CritterSpecies.NEOCARIDINA, CritterGenes.WILD, true, 0);
            h.assertTrue(Math.abs(TankInhabitants.stocking(level, c.getTank()) - 1.04) < 1e-6, "25 adult shrimp = one fish, 10 young 0.04");
            ServerConfig.STOCKING_SHRIMP_PER_FISH.set(5);
            h.assertTrue(Math.abs(TankInhabitants.stocking(level, c.getTank()) - 5.2) < 1e-6, "5 per fish: five fish (+ 0.2 young)");
            ServerConfig.TANK_HARD_CAP_PER_WATER_BLOCK.set(5);
            BettaData d = BettaData.create(BettaGenome.DEFAULT, false, level.getGameTime());
            var err = TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d), ModEntities.BETTA.get(), null, 0L);
            h.assertTrue(err != null, "the tank is full of shrimp: no betta fits");
            var err2 = TankCritters.insert(level, c, new ArrayList<>(List.of(adultTag(CritterSpecies.NEOCARIDINA))), null, 0L);
            h.assertTrue(err2 != null, "no more adult shrimp either");
            // at the hard cap the colony stops breeding
            int before = c.getTank().critters().size();
            for (int i = 0; i < 5 * 24; i++) {
                c.getTank().setCritterFood(50);
                c.getTank().setWaterTemp(25F);
                TankCritters.step(level, c, 1.0 / 24);
            }
            h.assertTrue(!logged(c.getTank(), "log.bettacraft.critters_born"), "no young at the hard cap");
            h.assertTrue(c.getTank().critters().size() <= before, "not more animals");
            ServerConfig.STOCKING_SHRIMP_PER_FISH.set(25);
            h.assertTrue(TankCritters.insert(level, c, new ArrayList<>(List.of(adultTag(CritterSpecies.NEOCARIDINA))), null, 0L) == null, "with 25 per fish it fits");
        } finally {
            ServerConfig.STOCKING_SHRIMP_PER_FISH.set(saved);
            ServerConfig.TANK_HARD_CAP_PER_WATER_BLOCK.set(savedCap);
        }
        h.succeed();
    }

    private static CompoundTag adultTag(CritterSpecies s) {
        Critter k = new Critter(0, s, true, CritterGenes.WILD, 0, 0, 0);
        k.age = s.juvenileDays() + 1;
        return k.saveAnimal();
    }

    // ------------------------------------------------------------------ bags, net, merging

    /** A bag of shrimp goes in like a player does it; the menu takes the best one out, the net catches one through the glass. */
    @GameTest(template = EMPTY)
    public static void bagsMenuAndNet(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 2, 2, 3, 2, 2, 11000);
        ServerPlayer sp = player(h);
        BlockPos front = h.absolutePos(new BlockPos(3, 2, 2));
        sp.teleportTo(level, front.getX() + 0.5, front.getY(), front.getZ() - 2.5, java.util.Set.of(), 0F, 0F, false);
        java.util.random.RandomGenerator rnd = new java.util.Random(3);
        ItemStack bag = TankCritters.bagOf(CritterSpecies.NEOCARIDINA, 3, () -> CritterGenes.pure(0, 0.2F), rnd, true);
        List<CompoundTag> animals = new ArrayList<>(bag.get(ModDataComponents.CRITTERS.get()));
        Critter best = Critter.loadAnimal(animals.get(1), 0, 0, 0, 0);
        best.genes = CritterGenes.pure(0, 0.9F);
        animals.set(1, best.saveAnimal());
        bag.set(ModDataComponents.CRITTERS.get(), List.copyOf(animals));
        long water = c.getTank().waterMb();
        use(sp, front, Direction.NORTH, bag);
        TankData t = c.getTank();
        h.assertTrue(count(t, CritterSpecies.NEOCARIDINA) == 3, "three shrimp in: " + count(t, CritterSpecies.NEOCARIDINA));
        h.assertTrue(t.waterMb() == water + TankInhabitants.BAG_MB, "with the bag's water");
        h.assertTrue(sp.getMainHandItem().is(ModItems.FISH_BAG.get()) && !FishBagItem.isFilled(sp.getMainHandItem()), "the bag is empty again");
        sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.assertTrue(TankInhabitants.breakBlockedReason(level, front).isPresent(), "living shrimp keep the tank");
        // the menu: best grade first into the empty bag
        sp.getInventory().setItem(5, new ItemStack(ModItems.FISH_BAG.get()));
        var err = TankCritters.takeOut(level, c, sp, "neocaridina:" + best.genes.variety(CritterSpecies.NEOCARIDINA), false);
        h.assertTrue(err == null, "taken: " + (err == null ? "" : err.getString()));
        List<CompoundTag> got = sp.getInventory().getNonEquipmentItems().stream().map(TankCritters::bagged).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        h.assertTrue(got != null && got.size() == 1, "one in the bag");
        h.assertTrue(Critter.loadAnimal(got.getFirst(), 0, 0, 0, 0).genes.grade() > 0.85F, "the best one");
        // the net: aimed through the glass at a shrimp held still
        Critter k = t.critters().getFirst();
        BlockPos mid = h.absolutePos(new BlockPos(3, 2, 3));
        k.x = mid.getX() + 0.5;
        k.y = mid.getY() + 0.04;
        k.z = mid.getZ() + 0.5;
        k.idle = 10000;
        k.face = Critter.FLOOR;
        sp.teleportTo(level, mid.getX() + 0.5, mid.getY() + 0.6 - sp.getEyeHeight() + 1.0, mid.getZ() - 3.0, java.util.Set.of(), 0F, 0F, false);
        sp.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(k.x, k.y + 0.04, k.z));
        use(sp, front, Direction.NORTH, new ItemStack(ModItems.AQUARIUM_NET.get()));
        h.assertTrue(count(t, CritterSpecies.NEOCARIDINA) == 1, "the net caught one: " + count(t, CritterSpecies.NEOCARIDINA) + " left");
        List<CompoundTag> after = sp.getInventory().getNonEquipmentItems().stream().map(TankCritters::bagged).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        h.assertTrue(after != null && after.size() == 2, "into the same bag (same species)");
        // Ctrl in the menu: a full bag at once (here the last one)
        h.assertTrue(TankCritters.takeOut(level, c, sp, "neocaridina:" + t.critters().getFirst().genes.variety(CritterSpecies.NEOCARIDINA), false,
                TankCritters.MAX_PER_BAG) == null, "bulk take-out");
        h.assertTrue(count(t, CritterSpecies.NEOCARIDINA) == 0, "all out");
        h.assertTrue(TankInhabitants.breakBlockedReason(level, front).isEmpty(), "an empty tank can be broken again");
        h.succeed();
    }

    /** Merging two tanks keeps the animals of both; the algae stay with their glass. */
    @GameTest(template = EMPTY)
    public static void mergingKeepsTheAnimals(GameTestHelper h) {
        TankBlockEntity a = tank(h, 2, 2, 2, 2, 1, 1, 2000);
        TankBlockEntity b = tank(h, 5, 2, 2, 1, 1, 1, 1000);
        add(h, a, CritterSpecies.NEOCARIDINA, CritterGenes.WILD, true);
        add(h, a, CritterSpecies.ZEBRA_NERITE, CritterGenes.WILD, true);
        add(h, b, CritterSpecies.RAMSHORN, CritterGenes.pure(1, 0.5F), true);
        AlgaeSim.set(b.getTank().algae(), b.getTank().anchor().asLong(), 0, 0.7F);
        h.setBlock(new BlockPos(4, 2, 2), ModBlocks.TANK.get().defaultBlockState());
        TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(new BlockPos(4, 2, 2)), null);
        TankBlockEntity m = TankMultiblock.getController(h.getLevel(), h.absolutePos(new BlockPos(2, 2, 2)));
        h.assertTrue(m != null && m.getTank().memberCount() == 4, "merged");
        TankData t = m.getTank();
        h.assertTrue(count(t, CritterSpecies.NEOCARIDINA) == 1 && count(t, CritterSpecies.ZEBRA_NERITE) == 1 && count(t, CritterSpecies.RAMSHORN) == 1,
                "all three animals: " + t.critters().size());
        h.assertTrue(AlgaeSim.get(t.algae(), h.absolutePos(new BlockPos(5, 2, 2)).asLong(), 0) > 0.6F, "the algae of the other tank");
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        for (Critter k : t.critters()) h.assertTrue(ids.add(k.id), "unique ids");
        // saved and loaded
        TankData back = TankData.load(t.save());
        h.assertTrue(back.critters().size() == 3 && back.algae().size() == t.algae().size(), "save/load");
        h.succeed();
    }

    // ------------------------------------------------------------------ sources

    private static final int SWINGS = 60;

    /** The net in rivers, swamps, mangroves, beaches, jungle water and lush caves catches what lives there; plains water nothing. */
    @GameTest(template = EMPTY, batch = "bettacraft_critters_net", timeoutTicks = 200)
    public static void netCatchesByBiome(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        double saved = ServerConfig.CRITTERS_NET_CHANCE.get();
        double fishShare = ServerConfig.TANKMATES_NET_SHARE.get(); // M19: only small animals here (the fish: TankmateGameTests)
        double plantShare = ServerConfig.AQUATIC_NET_SHARE.get(); // M22.4: and no plants (PlantSourceGameTests)
        try {
            ServerConfig.CRITTERS_NET_CHANCE.set(1.0);
            ServerConfig.TANKMATES_NET_SHARE.set(0.0);
            ServerConfig.AQUATIC_NET_SHARE.set(0.0);
            BlockPos water = h.absolutePos(new BlockPos(16, 2, 16));
            h.setBlock(new BlockPos(16, 2, 16), Blocks.WATER);
            ServerPlayer sp = player(h);
            String[][] cases = {
                    {"minecraft:river", "neocaridina,ramshorn,bladder_snail"},
                    {"minecraft:swamp", "ramshorn,bladder_snail,trumpet_snail"},
                    {"minecraft:mangrove_swamp", "zebra_nerite,horned_nerite,olive_nerite"},
                    {"minecraft:beach", "zebra_nerite,horned_nerite,olive_nerite"},
                    {"minecraft:jungle", "apple_snail"},
                    {"minecraft:lush_caves", "trumpet_snail"},
                    {"minecraft:plains", ""}};
            for (String[] cs : cases) {
                biome(level, water, cs[0]);
                h.assertTrue(level.getBiome(water).is(net.minecraft.resources.Identifier.parse(cs[0])), "biome " + cs[0] + ": " + level.getBiome(water));
                sp.getInventory().clearContent();
                for (int i = 0; i < 4; i++) sp.getInventory().add(new ItemStack(ModItems.FISH_BAG.get(), 16));
                sp.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.AQUARIUM_NET.get()));
                Set<CritterSpecies> seen = EnumSet.noneOf(CritterSpecies.class);
                int frogs = 0, catches = 0;
                // M22.5: 60 swings (was 25: a species with a quarter of the weight was missing about once in 800 per water,
                // and the test checks six waters; failed once in 65 local runs)
                // (the filled bags are taken out after every swing: 60 of them do not fit into the inventory)
                for (int i = 0; i < SWINGS; i++) {
                    sp.getCooldowns().removeCooldown(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ModItems.AQUARIUM_NET.get()));
                    NetCatching.tryCatch(level, sp, InteractionHand.OFF_HAND, water);
                    for (int slot = 0; slot < sp.getInventory().getNonEquipmentItems().size(); slot++) {
                        ItemStack s = sp.getInventory().getNonEquipmentItems().get(slot);
                        List<CompoundTag> l = TankCritters.bagged(s);
                        if (l != null) {
                            catches += s.getCount();
                            for (CompoundTag tag : l) seen.add(Critter.loadAnimal(tag, 0, 0, 0, 0).species);
                            sp.getInventory().getNonEquipmentItems().set(slot, ItemStack.EMPTY);
                        } else if (s.getItem() instanceof FishBagItem && FishBagItem.containedType(s) == ModEntities.DWARF_FROG.get()) {
                            frogs += s.getCount();
                            catches += s.getCount();
                            sp.getInventory().getNonEquipmentItems().set(slot, ItemStack.EMPTY);
                        }
                    }
                }
                h.assertTrue(sp.getOffhandItem().getDamageValue() == SWINGS, "the net wears: " + sp.getOffhandItem().getDamageValue());
                if (cs[1].isEmpty()) {
                    h.assertTrue(catches == 0, "nothing in " + cs[0]);
                    continue;
                }
                h.assertTrue(catches == SWINGS, cs[0] + ": a catch every time with chance 1: " + catches);
                for (String id : cs[1].split(",")) h.assertTrue(seen.contains(CritterSpecies.byId(id)), cs[0] + " has " + id + ": " + seen);
                for (CritterSpecies s : seen) {
                    boolean ok = cs[1].contains(s.id()) || s == CritterSpecies.ASSASSIN_SNAIL || s == CritterSpecies.RED_NERITE
                            || s == CritterSpecies.YELLOW_NERITE && cs[0].equals("minecraft:mangrove_swamp") // M21.5
                            || s == CritterSpecies.TRUMPET_SNAIL || s == CritterSpecies.DWARF_CRAYFISH || s == CritterSpecies.RAMSHORN
                            || s == CritterSpecies.HERMIT_CRAB && cs[0].equals("minecraft:beach") // M20: hermit crabs at the beach
                            || s == CritterSpecies.CARIDINA && cs[0].equals("minecraft:river") // M25c: rare bee shrimp in rivers
                            || (s == CritterSpecies.SULAWESI_SHRIMP || s == CritterSpecies.TYLOMELANIA) && cs[0].equals("minecraft:jungle") // M25c
                            || s == CritterSpecies.MARBLED_CRAYFISH && cs[0].equals("minecraft:river") // M29d: rare big crayfish
                            || s == CritterSpecies.FLORIDA_CRAYFISH && cs[0].equals("minecraft:swamp")
                            || s == CritterSpecies.AMANO_SHRIMP && cs[0].equals("minecraft:mangrove_swamp"); // M49
                    h.assertTrue(ok, cs[0] + " should not give " + s);
                }
                if (cs[0].equals("minecraft:mangrove_swamp")) h.assertTrue(!seen.contains(CritterSpecies.NEOCARIDINA), "no shrimp in mangroves");
                if (cs[0].equals("minecraft:river")) h.assertTrue(frogs == 0, "no frogs in rivers");
            }
            // without an empty bag the catch gets away
            sp.getInventory().clearContent();
            sp.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.AQUARIUM_NET.get()));
            biome(level, water, "minecraft:river");
            sp.getCooldowns().removeCooldown(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ModItems.AQUARIUM_NET.get()));
            NetCatching.tryCatch(level, sp, InteractionHand.OFF_HAND, water);
            h.assertTrue(sp.getInventory().getNonEquipmentItems().stream().allMatch(ItemStack::isEmpty), "no bag, no catch");
        } finally {
            ServerConfig.CRITTERS_NET_CHANCE.set(saved);
            ServerConfig.AQUATIC_NET_SHARE.set(plantShare);
            ServerConfig.TANKMATES_NET_SHARE.set(fishShare);
        }
        h.succeed();
    }

    /** A shovel on mud in a swamp turns up a trumpet snail (chance 1), not elsewhere. */
    @GameTest(template = EMPTY, batch = "bettacraft_critters_mud")
    public static void shovelOnMudInASwamp(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        double saved = ServerConfig.CRITTERS_MUD_CHANCE.get();
        try {
            ServerConfig.CRITTERS_MUD_CHANCE.set(1.0);
            BlockPos mud = h.absolutePos(new BlockPos(16, 2, 16));
            h.setBlock(new BlockPos(16, 2, 16), Blocks.MUD);
            biome(level, mud, "minecraft:swamp");
            ServerPlayer sp = player(h);
            sp.teleportTo(level, mud.getX() + 0.5, mud.getY() + 1, mud.getZ() - 1.5, java.util.Set.of(), 0F, 40F, false);
            h.assertTrue(level.getBiome(mud).is(net.minecraft.world.level.biome.Biomes.SWAMP), "swamp: " + level.getBiome(mud));
            sp.getInventory().setItem(5, new ItemStack(ModItems.FISH_BAG.get(), 4));
            use(sp, mud, Direction.UP, new ItemStack(Items.IRON_SHOVEL));
            boolean snail = sp.getInventory().getNonEquipmentItems().stream().map(TankCritters::bagged).filter(java.util.Objects::nonNull)
                    .anyMatch(l -> Critter.loadAnimal(l.getFirst(), 0, 0, 0, 0).species == CritterSpecies.TRUMPET_SNAIL);
            h.assertTrue(snail, "a trumpet snail in the bag");
            h.assertTrue(level.getBlockState(mud).is(Blocks.MUD), "the mud stays");
        } finally {
            ServerConfig.CRITTERS_MUD_CHANCE.set(saved);
        }
        h.succeed();
    }

    /** The wandering trader sells crayfish and apple snails; shrimp colour lines, ramshorns, assassins and frogs rarely. */
    @GameTest(template = EMPTY)
    public static void wanderingTraderSellsSmallAnimals(GameTestHelper h) {
        WanderingTrader trader = h.spawn(EntityType.WANDERING_TRADER, new BlockPos(2, 1, 2));
        var rnd = h.getLevel().getRandom();
        Set<CritterSpecies> generic = EnumSet.noneOf(CritterSpecies.class), rare = EnumSet.noneOf(CritterSpecies.class);
        boolean frog = false, coloured = false;
        for (int level = 1; level <= 2; level++) {
            for (VillagerTrades.ItemListing l : org.overengineer.bettacraft.gametest.framework.DevTrades.wanderer(level)) {
                var offer = l.getOffer((net.minecraft.server.level.ServerLevel) trader.level(), trader, rnd);
                if (offer == null) continue;
                ItemStack r = offer.getResult();
                List<CompoundTag> c = TankCritters.bagged(r);
                if (c != null) {
                    Critter k = Critter.loadAnimal(c.getFirst(), 0, 0, 0, 0);
                    (level == 1 ? generic : rare).add(k.species);
                    if (k.species == CritterSpecies.NEOCARIDINA) coloured = !"wild".equals(k.genes.variety(k.species));
                } else if (r.getItem() instanceof FishBagItem && FishBagItem.containedType(r) == ModEntities.DWARF_FROG.get()) {
                    frog = true;
                }
            }
        }
        h.assertTrue(generic.contains(CritterSpecies.DWARF_CRAYFISH) && generic.contains(CritterSpecies.APPLE_SNAIL), "usual: " + generic);
        h.assertTrue(rare.contains(CritterSpecies.NEOCARIDINA) && rare.contains(CritterSpecies.RAMSHORN) && rare.contains(CritterSpecies.ASSASSIN_SNAIL), "rare: " + rare);
        h.assertTrue(coloured, "the shrimp are a colour line");
        h.assertTrue(frog, "a frog among the rare offers");
        trader.discard();
        h.succeed();
    }

    // ------------------------------------------------------------------ frogs

    private static DwarfFrogEntity frogIn(GameTestHelper h, TankBlockEntity c) {
        ServerLevel level = h.getLevel();
        DwarfFrogEntity f = ModEntities.DWARF_FROG.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        ItemStack bag = FishBagItem.fillWith(f);
        f.discard();
        var err = TankInhabitants.insert(level, c, bag, ModEntities.DWARF_FROG.get(), null, 0L);
        if (err != null) throw new GameTestFailure("frog in: " + err.getString());
        for (var e : TankInhabitants.liveInhabitants(level, c.getTank())) if (e instanceof DwarfFrogEntity d) return d;
        throw new GameTestFailure("no frog in the tank");
    }

    /** A frog swims up to breathe in a tank with the air gap and keeps its air (short air in the config). */
    @GameTest(template = EMPTY, batch = "bettacraft_critters_frog_air", timeoutTicks = 900)
    public static void frogBreathesAtTheSurface(GameTestHelper h) {
        double saved = ServerConfig.FROGS_AIR_MINUTES.get();
        ServerConfig.FROGS_AIR_MINUTES.set(0.25);
        TankBlockEntity c = tank(h, 2, 2, 2, 2, 2, 2, 8000);
        DwarfFrogEntity frog = frogIn(h, c);
        AtomicBoolean breathed = new AtomicBoolean();
        h.onEachTick(() -> {
            if (frog.isBreathing()) breathed.set(true);
        });
        h.runAfterDelay(800, () -> {
            try {
                h.assertTrue(frog.isAlive(), "alive");
                h.assertTrue(breathed.get(), "it swam up and breathed");
                h.assertTrue(frog.breath() > 0, "it has air: " + frog.breath());
                h.assertTrue(frog.getHealth() >= frog.getMaxHealth(), "not hurt");
                h.assertTrue(TankInhabitants.inside(c.getTank(), frog), "still in the tank");
            } finally {
                ServerConfig.FROGS_AIR_MINUTES.set(saved);
            }
            h.succeed();
        });
    }

    /** Without air above the water (no air gap, tank full) the frog runs out of air and drowns slowly; the log warns. */
    @GameTest(template = EMPTY, batch = "bettacraft_critters_frog_noair", timeoutTicks = 900)
    public static void frogWithoutAirDrowns(GameTestHelper h) {
        double saved = ServerConfig.FROGS_AIR_MINUTES.get();
        int savedHead = ServerConfig.TANK_WATER_HEADROOM.get();
        ServerConfig.FROGS_AIR_MINUTES.set(0.25);
        ServerConfig.TANK_WATER_HEADROOM.set(0);
        TankBlockEntity c = tank(h, 2, 2, 2, 2, 2, 2, 8000);
        DwarfFrogEntity frog = frogIn(h, c);
        h.assertTrue(!TankFrogs.hasAir(c.getTank()), "a full tank without the gap has no air");
        h.runAfterDelay(800, () -> {
            try {
                h.assertTrue(frog.breath() == 0, "out of air: " + frog.breath());
                h.assertTrue(!frog.isAlive() || frog.getHealth() < frog.getMaxHealth(), "it drowns");
                h.assertTrue(logged(c.getTank(), "log.bettacraft.frogs_no_air"), "log warning");
            } finally {
                ServerConfig.FROGS_AIR_MINUTES.set(saved);
                ServerConfig.TANK_WATER_HEADROOM.set(savedHead);
            }
            h.succeed();
        });
    }

    /** Frogs eat larvae and pellets; with bettas they often miss out (share 0), aimed food (sneak) always reaches them. */
    @GameTest(template = EMPTY, batch = "bettacraft_critters_frog_food")
    public static void frogsAreSlowEaters(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        double saved = ServerConfig.FROGS_FOOD_SHARE.get();
        try {
            ServerConfig.FROGS_FOOD_SHARE.set(0.0);
            TankBlockEntity c = tank(h, 2, 2, 2, 2, 2, 2, 8000);
            DwarfFrogEntity frog = frogIn(h, c);
            frog.setLastFedForTest(level.getGameTime() - 48000);
            BettaData d = BettaData.create(BettaGenome.DEFAULT, false, level.getGameTime());
            var berr = TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d), ModEntities.BETTA.get(), null, 0L);
            h.assertTrue(berr == null, "betta in: " + (berr == null ? "" : berr.getString()));
            h.assertTrue(TankCare.eaters(level, c.getTank(), org.overengineer.bettacraft.food.BettaFood.Kind.LIVE).size() == 1, "the betta eats larvae");
            h.assertTrue(frog.daysSinceFed() > 1.5, "the frog is hungry: " + frog.daysSinceFed());
            TankCare.feed(level, c, new ItemStack(ModItems.MOSQUITO_LARVAE.get(), 4), 4);
            h.assertTrue(frog.daysSinceFed() > 1.5, "the betta snatched the food");
            h.assertTrue(logged(c.getTank(), "log.bettacraft.frogs_missed"), "log");
            ServerPlayer sp = player(h);
            ItemStack food = new ItemStack(ModItems.MOSQUITO_LARVAE.get(), 2);
            h.assertTrue(TankFrogs.feedTargeted(level, c, sp, food), "aimed at the frogs");
            h.assertTrue(frog.daysSinceFed() < 0.01, "the frog ate");
            h.assertTrue(food.getCount() == 1, "one portion used");
            // without bettas they get their share; fry food they do not eat
            frog.setLastFedForTest(level.getGameTime() - 48000);
            for (var e : TankCare.bettas(level, c.getTank())) e.discard();
            TankCare.feed(level, c, new ItemStack(ModItems.INFUSORIA.get()), 1);
            h.assertTrue(frog.daysSinceFed() > 1.5, "no fry food for frogs");
            TankCare.feed(level, c, new ItemStack(ModItems.BETTA_PELLETS.get()), 1);
            h.assertTrue(frog.daysSinceFed() < 0.01, "sinking pellets");
        } finally {
            ServerConfig.FROGS_FOOD_SHARE.set(saved);
        }
        h.succeed();
    }
}
