package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
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
import org.overengineer.bettacraft.terrarium.HerpBreeding;
import org.overengineer.bettacraft.terrarium.HerpClutch;
import org.overengineer.bettacraft.terrarium.HerpEntity;
import org.overengineer.bettacraft.terrarium.HerpSpawns;
import org.overengineer.bettacraft.terrarium.HerpSpecies;
import org.overengineer.bettacraft.terrarium.LandWalker;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.overengineer.bettacraft.terrarium.TerrariumFood;
import org.overengineer.bettacraft.terrarium.TerrariumMode;
import org.overengineer.bettacraft.terrarium.TerrariumNeeds;

/**
 * M31 part c: the amphibians and reptiles. Each goes only into its kind of tank and minds its climate (the dart frog the
 * dry air, the crested gecko the heat); the corn snake eats about once a week; the crested gecko climbs a glass wall and
 * clings there, the horned frog sits buried, the dart frog hops; dart frogs of one species lay onto moss and their young
 * are tadpoles in the water (two species do not mate); the leopard gecko's nest temperature decides the sex (fixed seeds:
 * cool females, 32 °C males); the wild animals have their biomes, spawn rules and sources. Climate set by hand (deterministic).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HerpGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private HerpGameTests() {}

    // ------------------------------------------------------------------ helpers

    private static ServerLevel interiorLevel(GameTestHelper h) {
        ServerLevel in = InteriorManager.interiorLevel(h.getLevel().getServer());
        if (in == null) throw new GameTestAssertException("no interior dimension");
        return in;
    }

    /** The interior with a floor of this substrate. */
    private static InteriorRegion interior(GameTestHelper h, TankBlockEntity c, BlockState floor) {
        InteriorRegion r = InteriorManager.ensureRegion(h.getLevel(), c);
        ServerLevel in = interiorLevel(h);
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), floor, false);
        }
        return r;
    }

    private static void land(GameTestHelper h, InteriorRegion r, int x0, int x1, int top, BlockState surface) {
        ServerLevel in = interiorLevel(h);
        for (int x = x0; x <= x1; x++) for (int z = 0; z < r.sizeZ(); z++) for (int y = 0; y < top; y++) {
            in.setBlock(r.toWorld(x, y, z), y == top - 1 ? surface : Blocks.DIRT.defaultBlockState(), 3);
        }
    }

    /** A 3x3x3 paludarium: land of {@code surface} over x >= 16 up to 24 interior blocks, a third of water. */
    private static TankBlockEntity paludarium(GameTestHelper h, int x, BlockState surface) {
        TankBlockEntity c = CritterGameTests.tank(h, x, 2, 2, 3, 3, 3, 0);
        InteriorRegion r = interior(h, c, Blocks.SAND.defaultBlockState());
        land(h, r, 16, r.sizeX() - 1, 24, surface);
        TankInteractions.changeWater(h.getLevel(), c, 9000);
        TankClimate.step(h.getLevel(), c, 0.01);
        TankData t = c.getTank();
        if (t.terrariumMode() != TerrariumMode.PALUDARIUM) throw new GameTestAssertException("no paludarium: " + t.terrariumMode() + " land " + t.landShare());
        t.setClimate(new Climate.State(24, 24, 24, 90, 0, 0));
        t.setWaterTemp(24F);
        return c;
    }

    /** A dry 3x3x3 terrarium with a sand floor. */
    private static TankBlockEntity terrarium(GameTestHelper h, int x) {
        TankBlockEntity c = CritterGameTests.tank(h, x, 2, 2, 3, 3, 3, 0);
        interior(h, c, Blocks.SAND.defaultBlockState());
        TankClimate.step(h.getLevel(), c, 0.01);
        if (c.getTank().terrariumMode() != TerrariumMode.TERRARIUM) throw new GameTestAssertException("no terrarium: " + c.getTank().terrariumMode());
        return c;
    }

    private static ItemStack box(GameTestHelper h, HerpSpecies s, boolean female, int variety) {
        HerpEntity e = ModEntities.herp(s).create(h.getLevel());
        if (e == null) throw new GameTestAssertException("no " + s.id());
        e.setFemale(female);
        e.setVariety(variety);
        ItemStack stack = e.getBucketItemStack();
        e.saveToBucketTag(stack);
        e.discard();
        return stack;
    }

    private static HerpEntity put(GameTestHelper h, TankBlockEntity c, HerpSpecies s, boolean female, int variety) {
        List<Entity> before = TankInhabitants.liveInhabitants(h.getLevel(), c.getTank());
        var err = TankInhabitants.insert(h.getLevel(), c, box(h, s, female, variety), ModEntities.herp(s), null, 0L);
        if (err != null) throw new GameTestAssertException(s.id() + " in: " + err.getString());
        for (Entity e : TankInhabitants.liveInhabitants(h.getLevel(), c.getTank())) {
            if (e instanceof HerpEntity t && !before.contains(e)) return t;
        }
        throw new GameTestAssertException("no " + s.id() + " in the tank");
    }

    // ------------------------------------------------------------------ admission and climate

    /**
     * Each species into its tank: the dart frog into a paludarium (a dry terrarium refuses it), the others into a dry
     * terrarium (a paludarium refuses the leopard gecko); every one travels in a box without water. Their climate: dry air
     * troubles the dart frog, heat harms the crested gecko; the snake eats again only after about a week.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void herpsGoIntoTheirHabitat(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity palu = paludarium(h, 2, Blocks.MOSS_BLOCK.defaultBlockState());
        TankBlockEntity dry = terrarium(h, 14);
        for (HerpSpecies s : List.of(HerpSpecies.DART_FROG, HerpSpecies.HORNED_FROG, HerpSpecies.LEOPARD_GECKO, HerpSpecies.CRESTED_GECKO, HerpSpecies.CORN_SNAKE)) {
            ItemStack b = box(h, s, true, 0);
            h.assertTrue(b.is(ModItems.herpContainer(s)) && TankInhabitants.carriedWater(b) == 0, s.id() + " travels in a box");
        }
        put(h, palu, HerpSpecies.DART_FROG, true, 0);
        h.assertTrue(TankInhabitants.insert(level, dry, box(h, HerpSpecies.DART_FROG, true, 0), ModEntities.herp(HerpSpecies.DART_FROG), null, 0L) != null,
                "a dry terrarium refuses the dart frog");
        HerpEntity gecko = put(h, dry, HerpSpecies.CRESTED_GECKO, true, 0);
        put(h, dry, HerpSpecies.LEOPARD_GECKO, false, 0);
        HerpEntity snake = put(h, dry, HerpSpecies.CORN_SNAKE, false, 0);
        put(h, dry, HerpSpecies.HORNED_FROG, false, 0);
        h.assertTrue(dry.getTank().waterMb() == 0, "still dry");
        h.assertTrue(TankInhabitants.insert(level, palu, box(h, HerpSpecies.LEOPARD_GECKO, true, 0), ModEntities.herp(HerpSpecies.LEOPARD_GECKO), null, 0L) != null,
                "a paludarium refuses the leopard gecko");
        // the climate: dry air troubles the dart frog; heat harms the crested gecko
        var dryAir = new TerrariumNeeds.Conditions(TerrariumMode.PALUDARIUM, 24, 40, 0, 24, 0.7, 24);
        h.assertTrue(HerpSpecies.DART_FROG.needs().assess(dryAir).problems().contains(TerrariumNeeds.Problem.TOO_DRY), "40 % is too dry for a dart frog");
        var hot = new TerrariumNeeds.Conditions(TerrariumMode.TERRARIUM, 31, 70, 0, 31, 1.0, Double.NaN);
        var a = gecko.terrariumNeeds().assess(hot);
        h.assertTrue(a.problems().contains(TerrariumNeeds.Problem.TOO_HOT) && a.harmful(), "31 °C harms the crested gecko");
        h.assertTrue(HerpSpecies.LEOPARD_GECKO.needs().assess(hot).problems().stream().noneMatch(TerrariumNeeds.HARMFUL::contains), "but not the leopard gecko");
        // the snake: a meal, then nothing for days
        snake.setLastFedForTest(level.getGameTime() - 7 * 24000);
        h.assertTrue(snake.eats(TerrariumFood.Diet.PREY), "a hungry snake takes its prey");
        h.assertFalse(snake.eats(TerrariumFood.Diet.INSECTS), "no crickets for the snake");
        snake.feed();
        h.assertFalse(snake.eats(TerrariumFood.Diet.PREY), "a fed snake refuses food for days");
        h.assertTrue(gecko.eats(TerrariumFood.Diet.FRUIT) && gecko.eats(TerrariumFood.Diet.SMALL_INSECTS), "the crested gecko eats fruit and fruit flies");
        h.assertTrue(TerrariumFood.diet(new ItemStack(ModItems.CRICKETS.get())) == TerrariumFood.Diet.INSECTS, "crickets are insects");
        h.assertTrue(TerrariumFood.diet(new ItemStack(net.minecraft.world.item.Items.RABBIT)) == TerrariumFood.Diet.PREY, "raw rabbit is prey");
        h.succeed();
    }

    // ------------------------------------------------------------------ movement

    /** The crested gecko walks to a glass wall, climbs it high above the ground and clings there (the pose synced). */
    @GameTest(template = EMPTY, timeoutTicks = 1200)
    public static void crestedGeckoClimbsTheGlass(GameTestHelper h) {
        TankBlockEntity c = terrarium(h, 2);
        TankData t = c.getTank();
        t.setClimate(new Climate.State(23, 23, 23, 70, 0, 0));
        HerpEntity gecko = put(h, c, HerpSpecies.CRESTED_GECKO, true, 0);
        gecko.moveTo(t.min().getX() + 1.5, t.min().getY() + 0.05, t.min().getZ() + 1.5, 0, 0);
        gecko.walker().forceActivity(LandWalker.Activity.CLIMB);
        double floor = t.min().getY();
        h.succeedWhen(() -> {
            h.assertTrue(gecko.isAlive(), "alive");
            h.assertTrue(gecko.walker().onWall(), "on the glass: " + gecko.position() + " pose " + gecko.walker().pose());
            h.assertTrue(gecko.getY() > floor + 0.45, "high on the wall: y " + (gecko.getY() - floor));
            h.assertTrue(gecko.herpPose().wall() != null, "the pose synced");
            double dx = Math.min(gecko.getX() - t.min().getX(), t.max().getX() + 1 - gecko.getX());
            double dz = Math.min(gecko.getZ() - t.min().getZ(), t.max().getZ() + 1 - gecko.getZ());
            h.assertTrue(Math.min(dx, dz) < 0.1, "at the glass: " + gecko.position());
        });
    }

    /** The horned frog rests buried in the ground; the dart frog hops (lifted off the ground in an arc) on the land. */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void hornedFrogBuriesAndDartFrogHops(GameTestHelper h) {
        TankBlockEntity dry = terrarium(h, 2);
        dry.getTank().setClimate(new Climate.State(26, 26, 26, 80, 0, 0));
        HerpEntity horned = put(h, dry, HerpSpecies.HORNED_FROG, false, 0);
        horned.walker().forceActivity(LandWalker.Activity.REST);
        TankBlockEntity palu = paludarium(h, 14, Blocks.MOSS_BLOCK.defaultBlockState());
        HerpEntity dart = put(h, palu, HerpSpecies.DART_FROG, false, 0);
        dart.walker().forceActivity(LandWalker.Activity.LAND);
        double[] lift = {0};
        h.onEachTick(() -> {
            if (!dart.isAlive() || dart.isInWater()) return;
            var ter = dart.walker().terrain(h.getLevel(), palu.getTank());
            double g = ter.ground(dart.getX(), dart.getZ());
            if (Double.isFinite(g)) lift[0] = Math.max(lift[0], dart.getY() - g);
        });
        h.succeedWhen(() -> {
            h.assertTrue(horned.walker().pose() == LandWalker.Pose.BURIED && horned.herpPose() == LandWalker.Pose.BURIED, "the horned frog sits buried");
            h.assertTrue(lift[0] > 0.03, "the dart frog hops: highest lift " + lift[0]);
        });
    }

    // ------------------------------------------------------------------ breeding

    /**
     * Dart frogs of two species do not mate; a pair of one species lays onto the moss (a frog clutch in the interior) and the
     * young hatch as tadpoles in the water; a tadpole needs water only, a froglet the paludarium.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void dartFrogsLayAndTadpolesSwim(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = paludarium(h, 2, Blocks.MOSS_BLOCK.defaultBlockState());
        TankData t = c.getTank();
        HerpEntity f = put(h, c, HerpSpecies.DART_FROG, true, 0);
        HerpEntity other = put(h, c, HerpSpecies.DART_FROG, false, 1); // D. auratus
        f.feed();
        other.feed();
        f.setNextLayForTest(0);
        HerpBreeding.step(level, c, 0.001);
        h.assertTrue(f.nestTarget() == null, "no mate of her own species: no eggs");
        HerpEntity m = put(h, c, HerpSpecies.DART_FROG, false, 0);
        m.feed();
        HerpBreeding.step(level, c, 0.001);
        h.assertTrue(f.nestTarget() != null, "she found a place on the moss");
        h.assertTrue(f.nestTarget().y > t.waterSurfaceY(), "on the land");
        f.moveTo(f.nestTarget().x, f.nestTarget().y, f.nestTarget().z);
        HerpBreeding.step(level, c, 0.001);
        h.assertTrue(t.herpClutches().size() == 1, "one clutch, got " + t.herpClutches().size());
        HerpClutch k = t.herpClutches().getFirst();
        h.assertTrue(k.eggs >= 3 && k.eggs <= 6, "3-6 eggs: " + k.eggs);
        ServerLevel in = interiorLevel(h);
        h.assertTrue(in.getBlockState(k.interiorPos()).is(ModBlocks.FROG_EGGS.get()), "a frog clutch in the interior");
        h.assertTrue(in.getBlockState(k.interiorPos().below()).is(Blocks.MOSS_BLOCK), "on the moss");
        t.herpClutches().clear();
        int n = HerpBreeding.hatch(level, c, k);
        h.assertTrue(n == k.eggs, "all hatch: " + n);
        double surface = t.waterSurfaceY();
        int tadpoles = 0;
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) {
            if (!(e instanceof HerpEntity y) || !y.isBaby()) continue;
            h.assertTrue(y.isTadpole(), "a tadpole");
            h.assertTrue(y.getY() < surface, "in the water: y " + y.getY() + " surface " + surface);
            h.assertTrue(y.variety() == 0, "the mother's species");
            h.assertTrue(y.gait().style() == LandWalker.Style.SWIM, "swims only");
            h.assertTrue(y.terrariumNeeds().air() == TerrariumNeeds.Range.ANY, "a tadpole minds the water only");
            tadpoles++;
        }
        h.assertTrue(tadpoles == k.eggs, "tadpoles: " + tadpoles);
        h.succeed();
    }

    /** The same leopard gecko clutch (fixed seed): 27 °C gives only females, 32 °C only males (pattern FMF). */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void leopardGeckoNestTemperatureDecidesTheSex(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerLevel in = interiorLevel(h);
        int[][] counts = new int[2][2];
        for (int hot = 0; hot < 2; hot++) {
            TankBlockEntity c = terrarium(h, hot == 1 ? 14 : 2);
            TankData t = c.getTank();
            float nest = hot == 1 ? 32.2F : 27F;
            t.setClimate(new Climate.State(nest - 2, nest, nest + 8, 40, 0, 0));
            InteriorRegion r = InteriorManager.ensureRegion(level, c);
            BlockPos egg = r.toWorld(10, 0, 10);
            in.setBlock(egg, ModBlocks.TURTLE_EGGS.get().withEggs(2), 3);
            // eight eggs (the real clutch is two) to see the ratio
            HerpClutch k = new HerpClutch(t.nextHerpClutchId(), HerpSpecies.LEOPARD_GECKO, 8,
                    new Vec3(t.min().getX() + 1, t.min().getY() + 0.05, t.min().getZ() + 1), egg, 777L);
            t.herpClutches().add(k);
            for (int i = 0; i < 60 && !t.herpClutches().isEmpty(); i++) HerpBreeding.step(level, c, 0.25);
            h.assertTrue(t.herpClutches().isEmpty(), "hatched");
            for (Entity e : TankInhabitants.liveInhabitants(level, t)) {
                if (!(e instanceof HerpEntity y)) continue;
                h.assertTrue(y.isBaby(), "a hatchling");
                counts[hot][y.isFemale() ? 1 : 0]++;
            }
        }
        h.assertTrue(counts[0][1] >= 7, "27 °C: females, got " + counts[0][1] + " females, " + counts[0][0] + " males");
        h.assertTrue(counts[1][0] >= 6, "32 °C: males, got " + counts[1][0] + " males, " + counts[1][1] + " females");
        h.succeed();
    }

    // ------------------------------------------------------------------ the wild

    /**
     * The biomes (jungle: dart frogs and crested geckos; desert and badlands: the leopard gecko; forest and plains: the
     * corn snake; savanna and swamp: the horned frog), their CREATURE spawns, the spawn rule of the land animals (soil, sand
     * or terracotta in daylight; not on stone, not in water) and a wild source for each of them.
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void wildHerps(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        h.assertTrue(HerpSpawns.home(biomes.getHolderOrThrow(Biomes.JUNGLE)) == HerpSpawns.Home.JUNGLE, "jungle");
        h.assertTrue(HerpSpawns.home(biomes.getHolderOrThrow(Biomes.DESERT)) == HerpSpawns.Home.ARID, "desert");
        h.assertTrue(HerpSpawns.home(biomes.getHolderOrThrow(Biomes.BADLANDS)) == HerpSpawns.Home.ARID, "badlands");
        h.assertTrue(HerpSpawns.home(biomes.getHolderOrThrow(Biomes.FOREST)) == HerpSpawns.Home.WOODLAND, "forest");
        h.assertTrue(HerpSpawns.home(biomes.getHolderOrThrow(Biomes.SNOWY_PLAINS)) == null, "no reptiles in the snow");
        List<EntityType<?>> jungle = mobs(biomes.getHolderOrThrow(Biomes.JUNGLE).value()), desert = mobs(biomes.getHolderOrThrow(Biomes.DESERT).value()),
                forest = mobs(biomes.getHolderOrThrow(Biomes.FOREST).value()), savanna = mobs(biomes.getHolderOrThrow(Biomes.SAVANNA).value());
        h.assertTrue(jungle.contains(ModEntities.herp(HerpSpecies.DART_FROG)) && jungle.contains(ModEntities.herp(HerpSpecies.CRESTED_GECKO)), "jungle animals");
        h.assertTrue(desert.contains(ModEntities.herp(HerpSpecies.LEOPARD_GECKO)), "the leopard gecko in deserts");
        h.assertTrue(forest.contains(ModEntities.herp(HerpSpecies.CORN_SNAKE)), "the corn snake in forests");
        h.assertTrue(savanna.contains(ModEntities.herp(HerpSpecies.HORNED_FROG)), "the horned frog in savannas");
        EntityType<?> gecko = ModEntities.herp(HerpSpecies.LEOPARD_GECKO);
        BlockPos p = new BlockPos(20, 2, 20);
        for (var ground : List.of(Blocks.SAND, Blocks.RED_SAND, Blocks.TERRACOTTA, Blocks.GRASS_BLOCK)) {
            h.setBlock(p.below(), ground);
            h.assertTrue(HerpSpawns.check(gecko, level, MobSpawnType.NATURAL, h.absolutePos(p), level.random), "on " + ground);
        }
        h.setBlock(p.below(), Blocks.STONE);
        h.assertFalse(HerpSpawns.check(gecko, level, MobSpawnType.NATURAL, h.absolutePos(p), level.random), "not on stone");
        BlockPos wet = new BlockPos(8, 2, 8);
        h.setBlock(wet.below(), Blocks.SAND);
        h.setBlock(wet, Blocks.WATER);
        h.assertFalse(HerpSpawns.check(ModEntities.herp(HerpSpecies.DART_FROG), level, MobSpawnType.NATURAL, h.absolutePos(wet), level.random), "not in water");
        // the wild variety: never a morph
        var random = net.minecraft.util.RandomSource.create(5);
        for (int i = 0; i < 50; i++) {
            h.assertTrue(HerpSpawns.variety(HerpSpecies.CORN_SNAKE, biomes.getHolderOrThrow(Biomes.FOREST), random, MobSpawnType.NATURAL) == 0, "wild corn snakes");
        }
        for (HerpSpecies s : List.of(HerpSpecies.DART_FROG, HerpSpecies.HORNED_FROG, HerpSpecies.LEOPARD_GECKO, HerpSpecies.CRESTED_GECKO, HerpSpecies.CORN_SNAKE)) {
            List<Source> src = Sources.of(Target.item(s.containerId()));
            h.assertTrue(src.stream().anyMatch(x -> x.kind() == Source.Kind.SPAWN), s.id() + " spawns wild");
        }
        h.assertTrue(Sources.of(Target.item("leopard_gecko_box")).stream().allMatch(x -> x.kind() != Source.Kind.SPAWN || x.where() == Where.ARID), "the gecko on arid land");
        h.succeed();
    }

    private static List<EntityType<?>> mobs(net.minecraft.world.level.biome.Biome b) {
        List<EntityType<?>> out = new ArrayList<>();
        for (var d : b.getMobSettings().getMobs(MobCategory.CREATURE).unwrap()) out.add(d.type);
        return out;
    }
}
