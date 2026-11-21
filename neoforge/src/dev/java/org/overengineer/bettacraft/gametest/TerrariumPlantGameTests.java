package org.overengineer.bettacraft.gametest;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.plants.AquaticPlantItem;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.LandPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.plants.TerrariumPlantLife;
import org.overengineer.bettacraft.plants.wild.WildLandPlantBlock;
import org.overengineer.bettacraft.plants.wild.WildLandPlantFeature;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.sources.WildTables;
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
import org.overengineer.bettacraft.terrarium.HerpSpecies;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.overengineer.bettacraft.world.BiomeWaters;

/**
 * M56: the terrarium plants. A fern grows in a humid terrarium and a cactus languishes there, the other way round in a dry
 * one; an aquarium plant above the water of a humid paludarium grows emersed; the shears take an offset of a cactus and divide
 * a moss; the wild terrarium plants are in their biomes, the feature puts a group of them on the ground, shears cut a cutting
 * and the plant grows back, a plant item plants a decoration in the world that gives the plant back; a tortoise grazes an
 * edible plant; dart frog tadpoles go into the cups of grown bromeliads (also without water); snakes and geckos hide in the
 * plants; the switches of {@code features.terrariumPlants} turn each of it off. Climate set by hand (deterministic).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TerrariumPlantGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private TerrariumPlantGameTests() {}

    // ------------------------------------------------------------------ helpers

    private static void check(boolean ok, String what) {
        if (!ok) throw new GameTestAssertException(what);
    }

    private static ServerLevel interior(GameTestHelper h) {
        ServerLevel in = InteriorManager.interiorLevel(h.getLevel().getServer());
        if (in == null) throw new GameTestAssertException("no interior dimension");
        return in;
    }

    /** A 3x3x3 tank at (x, 2, 2) with a soil floor in its interior and a lit light bar on top; water in mB. */
    private static TankBlockEntity tank(GameTestHelper h, int x, long water) {
        TankBlockEntity c = CritterGameTests.tank(h, x, 2, 2, 3, 3, 3, 0);
        InteriorRegion r = InteriorManager.ensureRegion(h.getLevel(), c);
        ServerLevel in = interior(h);
        for (int a = 0; a < r.sizeX(); a++)
            for (int b = 0; b < r.sizeZ(); b++) Substrate.set(in, new BlockPos(r.originX() + a, InteriorRegion.FLOOR_Y, r.originZ() + b), Blocks.DIRT.defaultBlockState(), false);
        for (int a = 0; a < 3; a++) h.setBlock(new BlockPos(x + a, 5, 3), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.LIT, true));
        if (water > 0) TankInteractions.changeWater(h.getLevel(), c, water);
        TankClimate.step(h.getLevel(), c, 0.01);
        return c;
    }

    private static InteriorRegion region(GameTestHelper h, TankBlockEntity c) {
        return InteriorManager.ensureRegion(h.getLevel(), c);
    }

    /** Sets the climate of a terrarium or paludarium by hand. */
    private static void climate(TankBlockEntity c, float air, float humidity) {
        c.getTank().setClimate(new Climate.State(air, air, air, humidity, 0, 0));
    }

    private static InteriorPlant plant(GameTestHelper h, TankBlockEntity c, PlantSpecies s, int lx, int lz, int stage) {
        InteriorRegion r = region(h, c);
        InteriorPlant p = InteriorPlants.plantAt(interior(h), r, s, lx, 0, lz, stage, 5600L + lx * 31L + lz);
        if (p == null) throw new GameTestAssertException(s.id() + " not planted");
        return p;
    }

    private static Map<Feature, Boolean> off(Feature... features) {
        Map<Feature, Boolean> saved = new EnumMap<>(Feature.class);
        for (Feature f : features) {
            saved.put(f, Features.own(f));
            Features.set(f, false);
        }
        return saved;
    }

    private static void restore(Map<Feature, Boolean> saved) {
        saved.forEach(Features::set);
    }

    // ------------------------------------------------------------------ growth by the climate

    /** A fern grows in a humid, warm terrarium while a cactus languishes there; in a dry, hot one it is the other way round. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void plantsGrowInTheirClimateAndLanguishInAnother(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 0);
        InteriorRegion r = region(h, c);
        check(c.getTank().hasClimate(), "the dry tank is a terrarium with a climate");
        InteriorPlant fern = plant(h, c, PlantSpecies.BOSTON_FERN, 10, 24, 1);
        InteriorPlant cactus = plant(h, c, PlantSpecies.BARREL_CACTUS, 34, 24, 4);
        climate(c, 25, 90);
        InteriorPlants.grow(level, c, r, PlantSpecies.BOSTON_FERN.daysToFull() * 1.2);
        check(fern.stage == PlantSpecies.BOSTON_FERN.stages(), "the fern is full grown in the rainforest: " + fern.stage);
        check(cactus.stage < 4 && cactus.stage >= 1, "the cactus languishes in the rainforest: " + cactus.stage);
        // the plants tab counts them
        ServerPlayer viewer = MockPlayers.serverPlayer(h);
        var view = org.overengineer.bettacraft.tank.menu.TankView.read(org.overengineer.bettacraft.tank.menu.TankView.build(viewer, c));
        viewer.discard();
        check(view.aquatic.land == 2, "two terrarium plants in the menu: " + view.aquatic.land);
        // the desert: the cactus grows again, the fern languishes
        climate(c, 32, 20);
        int fernBefore = fern.stage;
        InteriorPlants.grow(level, c, r, 12);
        check(fern.stage < fernBefore, "the fern languishes in the desert: " + fern.stage);
        check(cactus.wilt == 0, "the cactus heals in the desert: " + cactus.wilt);
        // under water a terrarium plant does not grow
        check(LandPlants.rate(PlantSpecies.BOSTON_FERN, new LandPlants.Env(15, 90, 25, true), true) == 0, "under water");
        h.succeed();
    }

    /** An aquarium plant above the water of a humid paludarium grows in its emersed form; in dry air it does not. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void aquariumPlantsGrowEmersedInAHumidPaludarium(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 0);
        InteriorRegion r = region(h, c);
        ServerLevel in = interior(h);
        // land on one side (16 high), water below it on the other
        for (int x = 24; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) for (int y = 0; y < 16; y++) in.setBlock(r.toWorld(x, y, z), Blocks.DIRT.defaultBlockState(), 3);
        TankInteractions.changeWater(level, c, 6000);
        TankClimate.step(level, c, 0.01);
        check(c.getTank().hasClimate(), "a paludarium: " + c.getTank().terrariumMode());
        InteriorPlant sword = InteriorPlants.plantAt(in, r, PlantSpecies.AMAZON_SWORD, 36, 16, 24, 1, 56001L);
        check(sword != null, "planted on the land");
        check(sword.y + 0.5 >= org.overengineer.bettacraft.tank.TankCare.waterLocalHeight(c.getTank(), r.scale()), "above the water");
        climate(c, 25, 40);
        InteriorPlants.grow(level, c, r, 10);
        check(sword.stage == 1, "no emersed growth in dry air: " + sword.stage);
        climate(c, 25, 90);
        InteriorPlants.grow(level, c, r, 10);
        check(sword.stage > 1, "emersed growth in humid air: " + sword.stage);
        h.succeed();
    }

    /** Shears: an offset of a cactus (a small rooted plant), half of a moss clump (division); a land plant is not planted under water. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void shearsTakeOffsetsAndDivideMosses(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 0);
        InteriorRegion r = region(h, c);
        ServerLevel in = interior(h);
        InteriorPlant cactus = plant(h, c, PlantSpecies.PRICKLY_PEAR, 12, 24, PlantSpecies.PRICKLY_PEAR.stages());
        InteriorPlant moss = plant(h, c, PlantSpecies.CUSHION_MOSS, 30, 24, PlantSpecies.CUSHION_MOSS.stages());
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        sp.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        sp.getInventory().clearContent();
        InteriorPlants.trim(in, r, cactus, sp, null, null);
        ItemStack offset = sp.getInventory().items.stream().filter(s -> s.is(ModItems.AQUATIC_PLANTS.get(PlantSpecies.PRICKLY_PEAR).get())).findFirst().orElse(ItemStack.EMPTY);
        check(offset.getCount() == 1 && AquaticPlantItem.stage(offset) == 1, "an offset (stage 1): " + offset + " stage " + AquaticPlantItem.stage(offset));
        check(cactus.stage == PlantSpecies.PRICKLY_PEAR.stages() - 1, "the cactus lost a stage: " + cactus.stage);
        int full = PlantSpecies.CUSHION_MOSS.stages();
        InteriorPlants.trim(in, r, moss, sp, null, null);
        ItemStack half = sp.getInventory().items.stream().filter(s -> s.is(ModItems.AQUATIC_PLANTS.get(PlantSpecies.CUSHION_MOSS).get())).findFirst().orElse(ItemStack.EMPTY);
        check(AquaticPlantItem.stage(half) == full / 2 && moss.stage == full - full / 2, "the moss divided in halves: " + AquaticPlantItem.stage(half) + " / " + moss.stage);
        sp.discard();
        h.succeed();
    }

    // ------------------------------------------------------------------ in the world

    private static Holder<Biome> biome(ServerLevel level, ResourceKey<Biome> key) {
        return level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(key);
    }

    private static boolean hasFeature(Holder<Biome> b, String id) {
        return b.value().getGenerationSettings().features().stream().anyMatch(set -> set.stream()
                .anyMatch(f -> f.unwrapKey().map(k -> k.location().equals(BettaCraft.id(id))).orElse(false)));
    }

    /** The wild terrarium plants are in their biomes (the tags) and their feature puts a group of one species on the ground. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void wildTerrariumPlantsInTheirBiomes(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        record Want(ResourceKey<Biome> biome, WildTables.LandPlace place) {}
        for (Want w : List.of(new Want(Biomes.JUNGLE, WildTables.LandPlace.JUNGLE), new Want(Biomes.SWAMP, WildTables.LandPlace.SWAMP),
                new Want(Biomes.MANGROVE_SWAMP, WildTables.LandPlace.SWAMP), new Want(Biomes.DESERT, WildTables.LandPlace.DESERT),
                new Want(Biomes.BADLANDS, WildTables.LandPlace.DESERT), new Want(Biomes.SAVANNA, WildTables.LandPlace.SAVANNA),
                new Want(Biomes.LUSH_CAVES, WildTables.LandPlace.LUSH_CAVES))) {
            Holder<Biome> b = biome(level, w.biome());
            check(BiomeWaters.landPlace(b) == w.place(), w.biome().location() + " is " + BiomeWaters.landPlace(b) + ", not " + w.place());
            check(hasFeature(b, "wild_land_plants") && hasFeature(b, "wild_cave_land_plants"), "the wild terrarium plants are not in " + w.biome().location());
        }
        for (ResourceKey<Biome> k : List.of(Biomes.PLAINS, Biomes.OCEAN, Biomes.TAIGA, Biomes.SNOWY_PLAINS)) {
            check(BiomeWaters.landPlace(biome(level, k)) == null, "terrarium plants would grow in " + k.location());
        }
        check(biome(level, Biomes.DESERT).is(ModTags.DESERT_PLANT_LANDS), "the desert is not in #bettacraft:desert_plant_lands");
        // a group on sand: cacti only on the soil
        BlockPos base = h.absolutePos(new BlockPos(6, 1, 6));
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            level.setBlock(base.offset(x, -1, z), Blocks.SAND.defaultBlockState(), 2);
            for (int y = 0; y < 4; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        check(WildLandPlantFeature.placeGroup(level, base, RandomSource.create(5601), PlantSpecies.COLUMN_CACTUS, true), "no cactus placed");
        int n = 0;
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            BlockState s = level.getBlockState(base.offset(x, 0, z));
            if (s.getBlock() instanceof WildLandPlantBlock b) {
                check(b.species() == PlantSpecies.COLUMN_CACTUS && !s.getValue(WildLandPlantBlock.PLANTED), "a wild column cactus");
                n++;
            }
        }
        check(n >= 1 && n <= 2, "one or two cacti: " + n);
        h.succeed();
    }

    /** Shears cut an offset off a wild cactus and it grows back; a plant item plants a decoration on soil (not on stone) that gives it back. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void wildPlantsShearsAndDecoration(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos at = h.absolutePos(new BlockPos(4, 2, 4));
        level.setBlock(at.below(), Blocks.SAND.defaultBlockState(), 3);
        level.setBlock(at, ModBlocks.WILD_LAND_PLANTS.get(PlantSpecies.ECHEVERIA).get().defaultBlockState(), 3);
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        sp.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        sp.getInventory().clearContent();
        ItemStack shears = new ItemStack(Items.SHEARS);
        sp.setItemInHand(InteractionHand.MAIN_HAND, shears);
        sp.gameMode.useItemOn(sp, level, shears, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false));
        check(level.getBlockState(at).getValue(WildLandPlantBlock.TRIMMED), "trimmed");
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(2));
        check(drops.size() == 1 && drops.getFirst().getItem().is(ModItems.AQUATIC_PLANTS.get(PlantSpecies.ECHEVERIA).get())
                && AquaticPlantItem.stage(drops.getFirst().getItem()) == 1, "an Echeveria offset: " + drops);
        drops.forEach(ItemEntity::discard);
        RandomSource r = RandomSource.create(7);
        for (int i = 0; i < 400 && level.getBlockState(at).getValue(WildLandPlantBlock.TRIMMED); i++) level.getBlockState(at).randomTick(level, at, r);
        check(!level.getBlockState(at).getValue(WildLandPlantBlock.TRIMMED), "it grew back");
        check(Block.getDrops(level.getBlockState(at), level, at, null, null, ItemStack.EMPTY).isEmpty(), "a wild one gives nothing by hand");
        level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
        // planted as decoration: on sand yes, on stone no; broken it gives the plant back; it does not grow there
        ItemStack fern = InteriorPlants.item(PlantSpecies.ALOE, 0);
        sp.setItemInHand(InteractionHand.MAIN_HAND, fern);
        var res = fern.useOn(new UseOnContext(sp, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(at.below()), Direction.UP, at.below(), false)));
        BlockState planted = level.getBlockState(at);
        check(planted.getBlock() instanceof WildLandPlantBlock b && b.species() == PlantSpecies.ALOE && planted.getValue(WildLandPlantBlock.PLANTED),
                "planted in the world: " + planted + " " + res);
        check(fern.isEmpty(), "the item was used");
        check(!planted.isRandomlyTicking(), "it does not grow in the world");
        var back = Block.getDrops(planted, level, at, null, null, ItemStack.EMPTY);
        check(back.size() == 1 && back.getFirst().is(ModItems.AQUATIC_PLANTS.get(PlantSpecies.ALOE).get()), "the plant back by hand: " + back);
        level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(at.below(), Blocks.STONE.defaultBlockState(), 3);
        ItemStack again = InteriorPlants.item(PlantSpecies.ALOE, 0);
        sp.setItemInHand(InteractionHand.MAIN_HAND, again);
        again.useOn(new UseOnContext(sp, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(at.below()), Direction.UP, at.below(), false)));
        check(level.getBlockState(at).isAir() && again.getCount() == 1, "no aloe on stone");
        sp.discard();
        h.succeed();
    }

    // ------------------------------------------------------------------ the animals

    /** A hungry Hermann's tortoise grazes the edible plant with the most stages; a fed one leaves it alone. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void tortoisesGrazeTheEdiblePlants(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 0);
        climate(c, 28, 40);
        InteriorPlant pear = plant(h, c, PlantSpecies.PRICKLY_PEAR, 12, 24, PlantSpecies.PRICKLY_PEAR.stages());
        InteriorPlant aloe = plant(h, c, PlantSpecies.ALOE, 30, 24, PlantSpecies.ALOE.stages());
        HerpEntity t = HerpGameTests.put(h, c, HerpSpecies.HERMANN, true, 0);
        t.feed();
        check(TerrariumPlantLife.graze(level, c) == 0, "a fed tortoise does not graze");
        t.setLastFedForTest(level.getGameTime() - 24000L);
        check(TerrariumPlantLife.grazer(t), "Hermann's tortoise grazes");
        check(TerrariumPlantLife.graze(level, c) == 1, "it grazed");
        check(pear.stage == PlantSpecies.PRICKLY_PEAR.stages() - 1, "the prickly pear lost a stage: " + pear.stage);
        check(aloe.stage == PlantSpecies.ALOE.stages(), "the aloe is not food");
        check(t.daysSinceFed() < 0.01, "the tortoise is fed");
        h.succeed();
    }

    /** Dart frog tadpoles go into the cups of grown bromeliads (one each), also in a terrarium without water; they stay there fed. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void dartFrogTadpolesGoIntoBromeliads(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 0);
        climate(c, 25, 90);
        TankData t = c.getTank();
        InteriorPlant small = plant(h, c, PlantSpecies.GUZMANIA, 10, 10, 1);
        plant(h, c, PlantSpecies.NEOREGELIA, 24, 24, PlantSpecies.NEOREGELIA.stages());
        plant(h, c, PlantSpecies.VRIESEA, 38, 38, PlantSpecies.VRIESEA.stages());
        check(TerrariumPlantLife.cups(level.getServer(), t).size() == 2, "two grown bromeliads (the small one has no cup yet): "
                + TerrariumPlantLife.cups(level.getServer(), t).size() + " " + small);
        HerpClutch k = new HerpClutch(1, HerpSpecies.DART_FROG, 4, new Vec3(t.min().getX() + 1.5, t.min().getY() + 0.2, t.min().getZ() + 1.5), BlockPos.ZERO, 56002L);
        var watcher = QuestAdv.watcher(h, c);
        int n = HerpBreeding.hatch(level, c, k);
        check(n == 2, "no water: as many tadpoles as cups hatch: " + n);
        QuestAdv.check(h, watcher, "bromeliad_tadpole");
        QuestAdv.gone(h, watcher);
        int inCups = 0;
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) {
            if (!(e instanceof HerpEntity y) || !y.isTadpole()) continue;
            check(y.inNursery(), "every tadpole sits in a bromeliad");
            y.setLastFedForTest(level.getGameTime() - 24000L * 3);
            inCups++;
        }
        check(inCups == 2, "two tadpoles: " + inCups);
        check(TerrariumPlantLife.freeCups(level, t).isEmpty(), "both cups taken");
        org.overengineer.bettacraft.terrarium.TerrariumCare.step(level, c, 0.01);
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) {
            if (e instanceof HerpEntity y && y.isTadpole()) check(y.daysSinceFed() < 0.01, "the mother feeds it in the cup");
        }
        h.succeed();
    }

    /** Snakes and geckos find a hiding place at the foot of a grown shelter plant; a frog does not hide there. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void snakesAndGeckosHideInThePlants(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 0);
        climate(c, 26, 50);
        TankData t = c.getTank();
        plant(h, c, PlantSpecies.AGAVE, 24, 24, PlantSpecies.AGAVE.stages());
        HerpEntity snake = HerpGameTests.put(h, c, HerpSpecies.CORN_SNAKE, true, 0);
        check(TerrariumPlantLife.hides(snake), "the snake hides in plants");
        double cx = t.min().getX() + 1.5, cz = t.min().getZ() + 1.5;
        double[] s = TerrariumPlantLife.shelter(level.getServer(), t, cx, cz, 2.5, new java.util.Random(1)::nextLong);
        check(s != null && Math.abs(s[0] - cx) < 0.3 && Math.abs(s[2] - cz) < 0.3, "a spot at the agave: " + java.util.Arrays.toString(s));
        var ter = new org.overengineer.bettacraft.critters.TankTerrain(t, level.getServer(), region(h, c), true);
        check(ter.plantShelter(cx, cz, 2.5, new java.util.Random(2)::nextLong) != null, "the terrain finds it too");
        h.succeed();
    }

    // ------------------------------------------------------------------ the switches

    /**
     * features.terrariumPlants.* off: the plants rest, cannot be planted, no wild ones, no grazing, the tadpoles go into the water,
     * no hiding in plants. Config restored afterwards.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m56_terrarium_plants", timeoutTicks = 200)
    public static void terrariumPlantSwitchesOff(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 0);
        InteriorRegion r = region(h, c);
        climate(c, 25, 90);
        InteriorPlant fern = plant(h, c, PlantSpecies.BOSTON_FERN, 10, 24, 1);
        plant(h, c, PlantSpecies.NEOREGELIA, 24, 24, PlantSpecies.NEOREGELIA.stages());
        InteriorPlant pear = plant(h, c, PlantSpecies.PRICKLY_PEAR, 38, 24, PlantSpecies.PRICKLY_PEAR.stages());
        HerpEntity t = HerpGameTests.put(h, c, HerpSpecies.HERMANN, true, 0);
        HerpEntity snake = HerpGameTests.put(h, c, HerpSpecies.CORN_SNAKE, true, 0);
        Map<Feature, Boolean> saved = off(Feature.BROMELIAD_TADPOLES, Feature.TORTOISE_GRAZING, Feature.PLANT_HIDING, Feature.TERRARIUM_PLANTS_WILD);
        try {
            check(TerrariumPlantLife.freeCups(level, c.getTank()).isEmpty(), "bromeliadTadpoles off: no cups");
            t.setLastFedForTest(level.getGameTime() - 24000L);
            check(TerrariumPlantLife.graze(level, c) == 0 && pear.stage == PlantSpecies.PRICKLY_PEAR.stages(), "tortoiseGrazing off: no grazing");
            check(!TerrariumPlantLife.hides(snake), "hiding off");
            check(!Features.enabled(Feature.TERRARIUM_PLANTS_WILD), "wildPlants off");
            InteriorPlants.grow(level, c, r, 2);
            check(fern.stage > 1, "the plants themselves still grow");
        } finally {
            restore(saved);
        }
        int before = fern.stage;
        saved = off(Feature.TERRARIUM_PLANTS);
        try {
            check(!LandPlants.enabled() && !Features.enabled(Feature.TORTOISE_GRAZING) && !Features.enabled(Feature.BROMELIAD_TADPOLES), "the group is off");
            InteriorPlants.grow(level, c, r, 20);
            check(fern.stage == before, "terrariumPlants off: they rest: " + fern.stage);
            ItemStack item = InteriorPlants.item(PlantSpecies.ALOE, 0);
            BlockPos at = h.absolutePos(new BlockPos(20, 2, 20));
            level.setBlock(at.below(), Blocks.SAND.defaultBlockState(), 3);
            ServerPlayer sp = MockPlayers.serverPlayer(h);
            sp.setItemInHand(InteractionHand.MAIN_HAND, item);
            item.useOn(new UseOnContext(sp, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(at.below()), Direction.UP, at.below(), false)));
            check(level.getBlockState(at).isAir(), "terrariumPlants off: nothing planted in the world");
            sp.discard();
        } finally {
            restore(saved);
        }
        h.succeed();
    }
}
