package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.sources.Source;
import org.overengineer.bettacraft.sources.Sources;
import org.overengineer.bettacraft.sources.Target;
import org.overengineer.bettacraft.sources.Where;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.terrarium.Climate;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.overengineer.bettacraft.terrarium.TerrariumCare;
import org.overengineer.bettacraft.terrarium.TerrariumMode;
import org.overengineer.bettacraft.terrarium.HerpBreeding;
import org.overengineer.bettacraft.terrarium.HerpClutch;
import org.overengineer.bettacraft.terrarium.HerpEntity;
import org.overengineer.bettacraft.terrarium.HerpSpawns;
import org.overengineer.bettacraft.terrarium.HerpSpecies;

/**
 * M31 part b: the turtles. Each one goes only into its kind of tank (the tortoise from its box without water, the sea turtle
 * only as a hatchling, which leaves in a bag when it outgrows the tank); a female lays only into sand or soil on the land;
 * the nest temperature decides the sex (fixed seeds: warm gives females, cool males); the tortoise brumates in the cold
 * (and without brumation the cold harms it); the wild turtles have their biomes, spawn rules and sources. The climate is
 * set by hand within one tick (deterministic).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TurtleGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private TurtleGameTests() {}

    // ------------------------------------------------------------------ helpers

    private static ServerLevel interiorLevel(GameTestHelper h) {
        ServerLevel in = InteriorManager.interiorLevel(h.getLevel().getServer());
        if (in == null) throw new GameTestFailure("no interior dimension");
        return in;
    }

    /** The interior with a sand floor. */
    private static InteriorRegion interior(GameTestHelper h, TankBlockEntity c) {
        InteriorRegion r = InteriorManager.ensureRegion(h.getLevel(), c);
        ServerLevel in = interiorLevel(h);
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.SAND.defaultBlockState(), false);
        }
        return r;
    }

    /** Land: interior columns x0..x1 (all z) filled up to the local height {@code top} (exclusive), {@code surface} on top. */
    private static void land(GameTestHelper h, InteriorRegion r, int x0, int x1, int top, BlockState fill, BlockState surface) {
        ServerLevel in = interiorLevel(h);
        for (int x = x0; x <= x1; x++) for (int z = 0; z < r.sizeZ(); z++) for (int y = 0; y < top; y++) {
            in.setBlock(r.toWorld(x, y, z), y == top - 1 ? surface : fill, 3);
        }
    }

    /** A 3x3x3 paludarium: land of {@code surface} over the half x >= 24 up to 24 blocks, a third of water, the climate set. */
    private static TankBlockEntity paludarium(GameTestHelper h, int x, BlockState surface) {
        TankBlockEntity c = CritterGameTests.tank(h, x, 2, 2, 3, 3, 3, 0);
        InteriorRegion r = interior(h, c);
        land(h, r, 24, r.sizeX() - 1, 24, Blocks.DIRT.defaultBlockState(), surface);
        TankInteractions.changeWater(h.getLevel(), c, 9000);
        TankClimate.step(h.getLevel(), c, 0.01);
        TankData t = c.getTank();
        if (t.terrariumMode() != TerrariumMode.PALUDARIUM) throw new GameTestFailure("no paludarium: " + t.terrariumMode() + " land " + t.landShare());
        climate(t, 25, 25);
        t.setWaterTemp(25F);
        return c;
    }

    /** A dry terrarium with a sand floor. */
    private static TankBlockEntity terrarium(GameTestHelper h, int x) {
        TankBlockEntity c = CritterGameTests.tank(h, x, 2, 2, 3, 3, 3, 0);
        interior(h, c);
        TankClimate.step(h.getLevel(), c, 0.01);
        if (c.getTank().terrariumMode() != TerrariumMode.TERRARIUM) throw new GameTestFailure("no terrarium: " + c.getTank().terrariumMode());
        return c;
    }

    /** A climate with a lit basking lamp (the spot 12 K warmer). */
    private static void climate(TankData t, float air, float ground) {
        t.setClimate(new Climate.State(air, ground, air + 12, 60, 5, 0));
    }

    /** A climate without a basking spot (the cold reaches the animal everywhere). */
    private static void cold(TankData t, float air) {
        t.setClimate(new Climate.State(air, air, air, 60, 5, 0));
    }

    private static ItemStack container(GameTestHelper h, HerpSpecies s, boolean female) {
        HerpEntity e = ModEntities.herp(s).create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (e == null) throw new GameTestFailure("no " + s.id());
        e.setFemale(female);
        ItemStack stack = e.getBucketItemStack();
        e.saveToBucketTag(stack);
        e.discard();
        return stack;
    }

    private static HerpEntity put(GameTestHelper h, TankBlockEntity c, HerpSpecies s, boolean female) {
        List<Entity> before = TankInhabitants.liveInhabitants(h.getLevel(), c.getTank());
        var err = TankInhabitants.insert(h.getLevel(), c, container(h, s, female), ModEntities.herp(s), null, 0L);
        if (err != null) throw new GameTestFailure(s.id() + " in: " + err.getString());
        for (Entity e : TankInhabitants.liveInhabitants(h.getLevel(), c.getTank())) {
            if (e instanceof HerpEntity t && !before.contains(e)) return t;
        }
        throw new GameTestFailure("no " + s.id() + " in the tank");
    }

    // ------------------------------------------------------------------ admission

    /**
     * The slider into a paludarium (not a dry terrarium), the tortoise from its box into a dry terrarium without water (not a
     * paludarium); M35: a sea turtle, grown or a hatchling in a fish bag, is refused by a small paludarium (the size rule of
     * the big animals; the big sea water paludarium: {@code ForeignTerrariumGameTests}).
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void turtlesGoIntoTheirHabitat(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity palu = paludarium(h, 2, Blocks.SAND.defaultBlockState());
        TankBlockEntity dry = terrarium(h, 12);
        put(h, palu, HerpSpecies.SLIDER, true);
        h.assertTrue(TankInhabitants.insert(level, dry, container(h, HerpSpecies.SLIDER, true), ModEntities.SLIDER_TURTLE.get(), null, 0L) != null,
                "a dry terrarium refuses the slider");
        ItemStack box = container(h, HerpSpecies.HERMANN, false);
        h.assertTrue(box.is(ModItems.HERMANN_TORTOISE_BOX.get()), "the tortoise travels in a box");
        h.assertTrue(TankInhabitants.carriedWater(box) == 0, "the box brings no water");
        put(h, dry, HerpSpecies.HERMANN, false);
        h.assertTrue(dry.getTank().waterMb() == 0, "still dry");
        h.assertTrue(TankInhabitants.insert(level, palu, container(h, HerpSpecies.HERMANN, true), ModEntities.HERMANN_TORTOISE.get(), null, 0L) != null,
                "a paludarium refuses the tortoise");
        // the vanilla sea turtle (M35: a big animal): neither a grown one nor a hatchling fits a small paludarium
        Turtle grown = EntityTypes.TURTLE.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        grown.snapTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(20, 2, 20)));
        ItemStack bigBag = FishBagItem.fillWith(grown);
        grown.discard();
        h.assertTrue(TankInhabitants.insert(level, palu, bigBag, EntityTypes.TURTLE, null, true) != null, "a grown sea turtle is refused");
        Turtle baby = EntityTypes.TURTLE.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        baby.setAge(-24000);
        ItemStack bag = FishBagItem.fillWith(baby);
        baby.discard();
        var err = TankInhabitants.insert(level, palu, bag, EntityTypes.TURTLE, null, true);
        h.assertTrue(BigTankGameTests.says(err, "message.bettacraft.big.too_small"), "a hatchling too: " + (err == null ? "accepted" : err.getString()));
        h.succeed();
    }

    // ------------------------------------------------------------------ eggs

    /** A pair of musk turtles: no eggs on stone land; on sand the female walks to a nest and lays eggs there (block and clutch). */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void eggsOnlyInSandOrSoil(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (boolean sand : new boolean[]{false, true}) {
            TankBlockEntity c = paludarium(h, sand ? 14 : 2, sand ? Blocks.SAND.defaultBlockState() : Blocks.STONE.defaultBlockState());
            TankData t = c.getTank();
            HerpEntity f = put(h, c, HerpSpecies.MUSK, true), m = put(h, c, HerpSpecies.MUSK, false);
            f.feed();
            m.feed();
            f.setNextLayForTest(0);
            HerpBreeding.step(level, c, 0.001);
            if (!sand) {
                h.assertTrue(f.nestTarget() == null, "no nest on stone");
                h.assertTrue(t.herpClutches().isEmpty(), "no eggs on stone");
                h.assertTrue(t.log().stream().anyMatch(l -> l.key().equals("log.bettacraft.turtle.no_nest")), "the log says why");
                continue;
            }
            h.assertTrue(f.nestTarget() != null, "she found a nest in the sand");
            h.assertTrue(f.nestTarget().y > t.waterSurfaceY(), "on the land");
            f.snapTo(f.nestTarget().x, f.nestTarget().y, f.nestTarget().z);
            HerpBreeding.step(level, c, 0.001);
            h.assertTrue(t.herpClutches().size() == 1, "one clutch, got " + t.herpClutches().size());
            HerpClutch k = t.herpClutches().getFirst();
            h.assertTrue(k.eggs >= 2 && k.eggs <= 4, "2-4 eggs: " + k.eggs);
            ServerLevel in = interiorLevel(h);
            h.assertTrue(in.getBlockState(k.interiorPos()).is(ModBlocks.TURTLE_EGGS.get()), "the eggs in the interior");
            h.assertTrue(in.getBlockState(k.interiorPos().below()).is(Blocks.SAND), "laid into the sand");
            h.assertFalse(f.layReady(level.getGameTime()), "she waits before the next clutch");
        }
        h.succeed();
    }

    /** The same clutch (fixed seed) at 32 °C gives only females, at 26 °C only males (slider, pivot 29.2 °C). */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void nestTemperatureDecidesTheSex(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerLevel in = interiorLevel(h);
        int[][] counts = new int[2][2];
        for (int warm = 0; warm < 2; warm++) {
            TankBlockEntity c = terrarium(h, warm == 1 ? 14 : 2);
            TankData t = c.getTank();
            climate(t, warm == 1 ? 30 : 24, warm == 1 ? 32 : 26);
            InteriorRegion r = InteriorManager.ensureRegion(level, c);
            BlockPos egg = r.toWorld(10, 0, 10);
            in.setBlock(egg, ModBlocks.TURTLE_EGGS.get().withEggs(6), 3);
            HerpClutch k = new HerpClutch(t.nextHerpClutchId(), HerpSpecies.SLIDER, 6,
                    new net.minecraft.world.phys.Vec3(t.min().getX() + 1, t.min().getY() + 0.05, t.min().getZ() + 1), egg, 424242L);
            t.herpClutches().add(k);
            var watcher = QuestAdv.watcher(h, c); // M45
            for (int i = 0; i < 40 && !t.herpClutches().isEmpty(); i++) HerpBreeding.step(level, c, 0.25);
            h.assertTrue(t.herpClutches().isEmpty(), "hatched");
            QuestAdv.check(h, watcher, "turtle_clutch");
            QuestAdv.gone(h, watcher);
            h.assertFalse(in.getBlockState(egg).is(ModBlocks.TURTLE_EGGS.get()), "the egg block is gone");
            for (Entity e : TankInhabitants.liveInhabitants(level, t)) {
                if (!(e instanceof HerpEntity y)) continue;
                h.assertTrue(y.isBaby() && y.growth() < 0.01F, "a hatchling");
                counts[warm][y.isFemale() ? 1 : 0]++;
            }
        }
        h.assertTrue(counts[1][1] == 6 && counts[1][0] == 0, "warm nest: 6 females, got " + counts[1][1] + " females, " + counts[1][0] + " males");
        h.assertTrue(counts[0][0] == 6 && counts[0][1] == 0, "cool nest: 6 males, got " + counts[0][0] + " males, " + counts[0][1] + " females");
        h.succeed();
    }

    // ------------------------------------------------------------------ brumation

    /** Below 12 °C the tortoise brumates: no harm from the cold, no hunger; warm air wakes it; frost still harms. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void tortoiseBrumatesInTheCold(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = terrarium(h, 2);
        TankData t = c.getTank();
        HerpEntity tortoise = put(h, c, HerpSpecies.HERMANN, true);
        tortoise.setLastFedForTest(level.getGameTime() - 24000);
        cold(t, 8);
        float hp = tortoise.getHealth();
        TerrariumCare.step(level, c, 2.0);
        h.assertTrue(tortoise.isBrumating(), "brumates at 8 °C");
        h.assertTrue(tortoise.getHealth() >= hp, "the cold does not harm it: " + tortoise.getHealth());
        h.assertTrue(tortoise.daysSinceFed() <= 1.0 + 1e-6, "it got no hungrier: " + tortoise.daysSinceFed());
        h.assertTrue(tortoise.stress() < 0.2, "no stress: " + tortoise.stress());
        h.assertFalse(tortoise.eats(org.overengineer.bettacraft.terrarium.TerrariumFood.Diet.GREENS), "it does not eat");
        cold(t, 13);
        TerrariumCare.step(level, c, 0.1);
        h.assertTrue(tortoise.isBrumating(), "13 °C: still asleep (wakes 2 K above)");
        cold(t, 15);
        TerrariumCare.step(level, c, 0.1);
        h.assertFalse(tortoise.isBrumating(), "15 °C: awake");
        cold(t, 0);
        TerrariumCare.step(level, c, 0.1);
        h.assertTrue(tortoise.isBrumating(), "frost: brumating");
        hp = tortoise.getHealth();
        TerrariumCare.step(level, c, 1.5);
        h.assertTrue(tortoise.getHealth() < hp, "but frost harms it: " + tortoise.getHealth());
        h.succeed();
    }

    /** With terrarium.brumation off the tortoise stays awake and the cold harms it (config restored). */
    @GameTest(template = EMPTY, timeoutTicks = 200, batch = "bettacraft_m31b_brumation_off")
    public static void withoutBrumationTheColdHarms(GameTestHelper h) {
        boolean saved = ServerConfig.TERRA_BRUMATION.get();
        try {
            ServerConfig.TERRA_BRUMATION.set(false);
            ServerLevel level = h.getLevel();
            TankBlockEntity c = terrarium(h, 2);
            HerpEntity tortoise = put(h, c, HerpSpecies.HERMANN, false);
            cold(c.getTank(), 8);
            float hp = tortoise.getHealth();
            TerrariumCare.step(level, c, 1.5);
            h.assertFalse(tortoise.isBrumating(), "no brumation");
            h.assertTrue(tortoise.getHealth() < hp, "the cold harms it: " + tortoise.getHealth());
            h.succeed();
        } finally {
            ServerConfig.TERRA_BRUMATION.set(saved);
        }
    }

    // ------------------------------------------------------------------ the wild

    /**
     * The biomes of the turtles (swamp, river, savanna; none in plains), their CREATURE spawns in the biome, the spawn rule
     * (the shore next to water, the tortoise on grass under the sky, never on stone), and a wild source for every turtle.
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void wildTurtles(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
        h.assertTrue(HerpSpawns.home(biomes.getOrThrow(Biomes.SWAMP)) == HerpSpawns.Home.SWAMP, "swamp");
        h.assertTrue(HerpSpawns.home(biomes.getOrThrow(Biomes.RIVER)) == HerpSpawns.Home.RIVER, "river");
        h.assertTrue(HerpSpawns.home(biomes.getOrThrow(Biomes.SAVANNA)) == HerpSpawns.Home.DRY_LAND, "savanna");
        h.assertTrue(HerpSpawns.entries(HerpSpawns.home(biomes.getOrThrow(Biomes.PLAINS))).stream().noneMatch(e -> e.species().turtle()),
                "no turtles in plains (part c: the corn snake lives there)");
        List<EntityType<?>> swamp = new ArrayList<>(), savanna = new ArrayList<>();
        for (var d : biomes.getOrThrow(Biomes.SWAMP).value().getMobSettings().getMobs(MobCategory.CREATURE).unwrap()) swamp.add(d.value().type());
        for (var d : biomes.getOrThrow(Biomes.SAVANNA).value().getMobSettings().getMobs(MobCategory.CREATURE).unwrap()) savanna.add(d.value().type());
        h.assertTrue(swamp.contains(ModEntities.SLIDER_TURTLE.get()) && swamp.contains(ModEntities.MUSK_TURTLE.get()), "sliders and musk turtles in swamps");
        h.assertTrue(savanna.contains(ModEntities.HERMANN_TORTOISE.get()) && !savanna.contains(ModEntities.SLIDER_TURTLE.get()), "the tortoise in savannas");
        // the rules: a slider on stone next to water, a tortoise on grass under the open sky, not on stone
        BlockPos shore = new BlockPos(8, 2, 8), water = new BlockPos(9, 1, 8);
        h.setBlock(shore.below(), Blocks.STONE);
        h.setBlock(water, Blocks.WATER);
        h.assertTrue(HerpSpawns.check(ModEntities.SLIDER_TURTLE.get(), level, EntitySpawnReason.NATURAL, h.absolutePos(shore), level.getRandom()), "a slider on the shore");
        BlockPos field = new BlockPos(20, 2, 20);
        h.setBlock(field.below(), Blocks.GRASS_BLOCK);
        h.assertTrue(HerpSpawns.check(ModEntities.HERMANN_TORTOISE.get(), level, EntitySpawnReason.NATURAL, h.absolutePos(field), level.getRandom()), "a tortoise on grass");
        h.setBlock(field.below(), Blocks.STONE);
        h.assertFalse(HerpSpawns.check(ModEntities.HERMANN_TORTOISE.get(), level, EntitySpawnReason.NATURAL, h.absolutePos(field), level.getRandom()), "not on stone");
        BlockPos dryLand = new BlockPos(26, 2, 26);
        h.setBlock(dryLand.below(), Blocks.STONE);
        h.assertFalse(HerpSpawns.check(ModEntities.SLIDER_TURTLE.get(), level, EntitySpawnReason.NATURAL, h.absolutePos(dryLand), level.getRandom()), "a slider far from water: no");
        // a wild source for each turtle: its bucket or box
        for (HerpSpecies s : HerpSpecies.ALL) {
            List<Source> src = Sources.of(Target.item(s.containerId()));
            h.assertTrue(src.stream().anyMatch(x -> x.kind() == Source.Kind.SPAWN), s.id() + " spawns wild");
        }
        h.assertTrue(Sources.of(Target.item("hermann_tortoise_box")).stream().allMatch(x -> x.kind() != Source.Kind.SPAWN || x.where() == Where.DRY_LAND),
                "the tortoise only on dry land");
        h.succeed();
    }
}
