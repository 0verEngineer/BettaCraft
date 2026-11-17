package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
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
import org.overengineer.bettacraft.plants.AquaticPlantItem;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.plants.wild.WildFloatingPlantBlock;
import org.overengineer.bettacraft.plants.wild.WildPlantBlock;
import org.overengineer.bettacraft.plants.wild.WildPlantKind;
import org.overengineer.bettacraft.plants.wild.WildPlantFeature;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.sources.WildTables.PlantWater;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.world.BiomeWaters;

/**
 * M28.1 part b GameTests: the wild plant feature is in the right biomes and picks the right water; it puts a group of
 * one species on the bottom of a pool (epiphytes on a stone it lays, strong-light plants only in shallow water) and a
 * patch of floating plants on the surface; shears cut a cutting of the species and the plant grows back; the drops by
 * hand, with shears and with silk touch; floating plants by hand and with the net.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class M281PlantGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private M281PlantGameTests() {}

    private static void check(boolean ok, String what) {
        if (!ok) throw new GameTestAssertException(what);
    }

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        sp.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        sp.getInventory().clearContent();
        return sp;
    }

    /** A pool {@code size} wide and {@code depth} deep with a floor of {@code floor}, its surface at sea level (absolute). */
    private static BlockPos pool(GameTestHelper h, int size, int depth, Block floor) {
        ServerLevel level = h.getLevel();
        BlockPos rel = h.absolutePos(new BlockPos(4, 0, 4));
        int sea = level.getSeaLevel();
        BlockPos min = new BlockPos(rel.getX(), sea - depth, rel.getZ());
        for (int x = -1; x <= size; x++)
            for (int z = -1; z <= size; z++) {
                boolean wall = x < 0 || z < 0 || x == size || z == size;
                level.setBlock(min.offset(x, -2, z), Blocks.STONE.defaultBlockState(), 2);
                level.setBlock(min.offset(x, -1, z), wall ? Blocks.STONE.defaultBlockState() : floor.defaultBlockState(), 2);
                for (int y = 0; y < depth; y++) level.setBlock(min.offset(x, y, z), wall ? Blocks.STONE.defaultBlockState() : Blocks.WATER.defaultBlockState(), 2);
                for (int y = depth; y < depth + 6; y++) level.setBlock(min.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
        return min;
    }

    private static void clear(GameTestHelper h, BlockPos min, int size, int depth) {
        ServerLevel level = h.getLevel();
        for (int x = -1; x <= size; x++)
            for (int z = -1; z <= size; z++)
                for (int y = -2; y < depth + 6; y++) level.setBlock(min.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
    }

    private static List<BlockPos> find(ServerLevel level, BlockPos min, int size, int depth, Class<? extends Block> type) {
        List<BlockPos> out = new ArrayList<>();
        for (int x = 0; x < size; x++)
            for (int z = 0; z < size; z++)
                for (int y = -1; y <= depth; y++) if (type.isInstance(level.getBlockState(min.offset(x, y, z)).getBlock())) out.add(min.offset(x, y, z));
        return out;
    }

    private static Holder<Biome> biome(ServerLevel level, ResourceKey<Biome> key) {
        return level.registryAccess().lookupOrThrow(Registries.BIOME).getHolderOrThrow(key);
    }

    private static boolean hasFeature(Holder<Biome> b, String id) {
        return b.value().getGenerationSettings().features().stream().anyMatch(set -> set.stream()
                .anyMatch(f -> f.unwrapKey().map(k -> k.location().equals(BettaCraft.id(id))).orElse(false)));
    }

    /** The biome modifier adds the features to the waters of the plants (by the biome tags), the water per biome is right. */
    @GameTest(template = EMPTY)
    public static void wildPlantsAreInTheirBiomes(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        record Want(ResourceKey<Biome> biome, PlantWater water) {}
        for (Want w : List.of(new Want(Biomes.JUNGLE, PlantWater.JUNGLE), new Want(Biomes.BAMBOO_JUNGLE, PlantWater.JUNGLE),
                new Want(Biomes.SWAMP, PlantWater.SWAMP), new Want(Biomes.MANGROVE_SWAMP, PlantWater.MANGROVE), new Want(Biomes.RIVER, PlantWater.RIVER),
                new Want(Biomes.FROZEN_RIVER, PlantWater.COLD), new Want(Biomes.TAIGA, PlantWater.COLD), new Want(Biomes.SNOWY_PLAINS, PlantWater.COLD),
                new Want(Biomes.LUSH_CAVES, PlantWater.LUSH_CAVES))) {
            Holder<Biome> b = biome(level, w.biome());
            check(BiomeWaters.plantWater(b) == w.water(), w.biome().location() + " is " + BiomeWaters.plantWater(b) + ", not " + w.water());
            check(hasFeature(b, "wild_plants") && hasFeature(b, "wild_cave_plants"), "the wild plants are not in " + w.biome().location());
        }
        for (ResourceKey<Biome> k : List.of(Biomes.PLAINS, Biomes.OCEAN, Biomes.WARM_OCEAN, Biomes.FROZEN_OCEAN, Biomes.BEACH, Biomes.DESERT, Biomes.SNOWY_BEACH)) {
            check(BiomeWaters.plantWater(biome(level, k)) == null, "wild plants would grow in " + k.location());
        }
        check(!hasFeature(biome(level, Biomes.PLAINS), "wild_plants") && !hasFeature(biome(level, Biomes.WARM_OCEAN), "wild_plants"),
                "the wild plants are in the plains or the warm ocean");
        check(biome(level, Biomes.FROZEN_RIVER).is(ModTags.COLD_WATERS), "the frozen river is not in #bettacraft:cold_waters");
        h.succeed();
    }

    /**
     * The feature puts a group of one species on the bottom: rosettes on sand, epiphytes on a stone it lays onto the sand,
     * strong-light plants not in deep water; a floating patch lies on the surface.
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void theFeaturePlacesGroupsOnTheBottomAndTheSurface(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        RandomSource r = RandomSource.create(2811);
        // rosettes on sand, 3 deep
        BlockPos min = pool(h, 9, 3, Blocks.SAND);
        BlockPos mid = min.offset(4, 0, 4);
        check(WildPlantFeature.placeGroup(level, mid, r, PlantSpecies.AMAZON_SWORD, PlantWater.JUNGLE, false), "no Amazon swords grew");
        var rosettes = find(level, min, 9, 3, WildPlantBlock.class);
        check(rosettes.size() >= 2, "too few rosettes: " + rosettes.size());
        for (BlockPos p : rosettes) {
            BlockState s = level.getBlockState(p);
            check(((WildPlantBlock) s.getBlock()).species(s) == PlantSpecies.AMAZON_SWORD, "another species: " + s);
            check(s.getFluidState().isSource() && p.getY() == min.getY(), "a rosette not on the bottom under water at " + p);
        }
        // epiphytes on the sand: a stone or a root is laid under them
        clear(h, min, 9, 3);
        min = pool(h, 9, 3, Blocks.SAND);
        check(WildPlantFeature.placeGroup(level, min.offset(4, 0, 4), r, PlantSpecies.ANUBIAS_NANA, PlantWater.JUNGLE, false), "no Anubias grew");
        for (BlockPos p : find(level, min, 9, 3, WildPlantBlock.class)) {
            BlockState below = level.getBlockState(p.below());
            check(!below.is(Blocks.SAND), "an Anubias sits on sand");
        }
        // a strong-light carpet does not grow 8 deep, a low-light one does
        clear(h, min, 9, 3);
        min = pool(h, 9, 8, Blocks.SAND);
        check(!WildPlantFeature.placeGroup(level, min.offset(4, 0, 4), r, PlantSpecies.MONTE_CARLO, PlantWater.SWAMP, false), "Monte Carlo grew 8 deep");
        check(WildPlantFeature.placeGroup(level, min.offset(4, 0, 4), r, PlantSpecies.MARSILEA, PlantWater.SWAMP, false), "Marsilea did not grow 8 deep");
        // a floating patch of duckweed on the surface
        clear(h, min, 9, 8);
        min = pool(h, 9, 3, Blocks.MUD);
        check(WildPlantFeature.placeFloating(level, min.offset(4, 0, 4), r, FloatingPlantType.DUCKWEED, false), "no duckweed on the surface");
        var patch = find(level, min, 9, 3, WildFloatingPlantBlock.class);
        check(patch.size() >= 3, "too little duckweed: " + patch.size());
        for (BlockPos p : patch) {
            check(p.getY() == min.getY() + 3 && level.getFluidState(p.below()).isSource(), "duckweed not on the water at " + p);
            check(WildFloatingPlantBlock.type(level.getBlockState(p)) == FloatingPlantType.DUCKWEED, "another floating plant");
        }
        // every water of the plants places something in a fitting pool
        for (PlantWater w : PlantWater.values()) {
            clear(h, min, 9, 3);
            min = pool(h, 9, 3, Blocks.GRAVEL);
            boolean any = false;
            for (int i = 0; i < 8 && !any; i++) any = WildPlantFeature.place(level, min.offset(4, 0, 4), r, w, false);
            check(any, "nothing grew for " + w);
        }
        clear(h, min, 9, 3);
        h.succeed();
    }

    /** Shears cut a cutting of the species (two of a stem plant); the plant is trimmed and grows back with random ticks. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void shearsCutACuttingAndThePlantGrowsBack(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos min = pool(h, 3, 2, Blocks.SAND);
        BlockPos at = min.offset(1, 0, 1);
        WildPlantBlock stem = ModBlocks.WILD_PLANTS.get(WildPlantKind.STEM).get();
        level.setBlock(at, stem.withSpecies(PlantSpecies.ROTALA), 3);
        ServerPlayer sp = player(h);
        ItemStack shears = new ItemStack(Items.SHEARS);
        sp.setItemInHand(InteractionHand.MAIN_HAND, shears);
        sp.gameMode.useItemOn(sp, level, shears, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false));
        BlockState s = level.getBlockState(at);
        check(s.getValue(WildPlantBlock.TRIMMED), "the plant is not trimmed");
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(2));
        check(drops.size() == 1 && drops.getFirst().getItem().getItem() instanceof AquaticPlantItem a && a.species() == PlantSpecies.ROTALA
                && drops.getFirst().getItem().getCount() == 2, "no two Rotala cuttings: " + drops);
        check(shears.getDamageValue() == 1, "the shears were not worn");
        check(!WildPlantBlock.cut(level, at, s, Direction.UP), "a trimmed plant gave a cutting");
        // regrowth: random ticks (seeded), about one in 20
        RandomSource r = RandomSource.create(7);
        for (int i = 0; i < 400 && level.getBlockState(at).getValue(WildPlantBlock.TRIMMED); i++) level.getBlockState(at).randomTick(level, at, r);
        check(!level.getBlockState(at).getValue(WildPlantBlock.TRIMMED), "the plant did not grow back");
        drops.forEach(ItemEntity::discard);
        clear(h, min, 3, 2);
        h.succeed();
    }

    /** Broken: nothing by hand, a cutting with shears, the half grown plant with silk touch. Out of the water it breaks. */
    @GameTest(template = EMPTY)
    public static void dropsByHandShearsAndSilkTouch(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos min = pool(h, 3, 2, Blocks.SAND);
        BlockPos at = min.offset(1, 0, 1);
        BlockState java = ModBlocks.WILD_PLANTS.get(WildPlantKind.EPIPHYTE).get().withSpecies(PlantSpecies.JAVA_FERN);
        level.setBlock(at.below(), Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(at, java, 3);
        check(Block.getDrops(java, level, at, null, null, ItemStack.EMPTY).isEmpty(), "the hand got something");
        check(Block.getDrops(java, level, at, null, null, new ItemStack(Items.STONE_PICKAXE)).isEmpty(), "a pickaxe got something");
        var cut = Block.getDrops(java, level, at, null, null, new ItemStack(Items.SHEARS));
        check(cut.size() == 1 && cut.getFirst().getItem() == ModItems.AQUATIC_PLANTS.get(PlantSpecies.JAVA_FERN).get() && AquaticPlantItem.stage(cut.getFirst()) == 0,
                "shears did not give a Java fern cutting: " + cut);
        ItemStack silk = new ItemStack(Items.DIAMOND_PICKAXE);
        silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        var whole = Block.getDrops(java, level, at, null, null, silk);
        check(whole.size() == 1 && AquaticPlantItem.stage(whole.getFirst()) == PlantSpecies.JAVA_FERN.stages() / 2, "silk touch did not give the plant: " + whole);
        check(level.getBlockState(at).getBlock().getCloneItemStack(level, at, level.getBlockState(at)).getItem()
                == ModItems.AQUATIC_PLANTS.get(PlantSpecies.JAVA_FERN).get(), "pick block is not the Java fern");
        // the floor gone: it breaks off
        level.setBlock(at.below(), Blocks.WATER.defaultBlockState(), 3);
        check(!(level.getBlockState(at).getBlock() instanceof WildPlantBlock), "the fern stays without a floor");
        clear(h, min, 3, 2);
        h.succeed();
    }

    /** Floating plants: broken by hand they give the plant, the net scoops them into the inventory, Riccia gives the aquarium plant. */
    @GameTest(template = EMPTY)
    public static void floatingPlantsAreCollectedByHandAndNet(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos min = pool(h, 4, 2, Blocks.SAND);
        BlockPos a = min.offset(1, 2, 1), b = min.offset(2, 2, 2), c = min.offset(1, 2, 2);
        BlockState base = ModBlocks.WILD_FLOATING_PLANT.get().defaultBlockState();
        level.setBlock(a, base.setValue(WildFloatingPlantBlock.TYPE, FloatingPlantType.HORNWORT), 3);
        level.setBlock(b, base.setValue(WildFloatingPlantBlock.TYPE, FloatingPlantType.WATER_LETTUCE), 3);
        level.setBlock(c, base.setValue(WildFloatingPlantBlock.TYPE, FloatingPlantType.RICCIA), 3);
        check(level.getBlockState(a).canSurvive(level, a), "hornwort cannot float on water");
        var hand = Block.getDrops(level.getBlockState(a), level, a, null, null, ItemStack.EMPTY);
        check(hand.size() == 1 && hand.getFirst().getItem() == ModItems.FLOATING_PLANTS.get(FloatingPlantType.HORNWORT).get(), "the hand got no hornwort: " + hand);
        var riccia = Block.getDrops(level.getBlockState(c), level, c, null, null, ItemStack.EMPTY);
        check(riccia.size() == 1 && riccia.getFirst().getItem() == ModItems.AQUATIC_PLANTS.get(PlantSpecies.RICCIA).get(), "floating Riccia gave " + riccia);
        ServerPlayer sp = player(h);
        check(WildFloatingPlantBlock.scoop(level, b, sp), "the net scooped nothing");
        check(level.getBlockState(b).isAir(), "the water lettuce is still there");
        check(sp.getInventory().countItem(ModItems.FLOATING_PLANTS.get(FloatingPlantType.WATER_LETTUCE).get()) == 1, "the water lettuce is not in the inventory");
        // on land a floating plant cannot stay
        level.setBlock(a.below(), Blocks.SAND.defaultBlockState(), 3);
        check(!(level.getBlockState(a).getBlock() instanceof WildFloatingPlantBlock), "hornwort stays on sand");
        clear(h, min, 4, 2);
        h.succeed();
    }
}
