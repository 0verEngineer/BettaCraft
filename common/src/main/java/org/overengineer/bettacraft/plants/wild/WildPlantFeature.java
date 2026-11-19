package org.overengineer.bettacraft.plants.wild;

import org.overengineer.bettacraft.config.Features;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.Fluids;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.sources.WildTables;
import org.overengineer.bettacraft.sources.WildTables.PlantWater;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.world.BiomeWaters;

/**
 * M28.1: a small group of one wild aquarium plant on the bottom, or a patch of one floating plant on the calm surface,
 * in the waters of its origin ({@link WildTables#wildPlants}, {@link WildTables#wildFloating}; the water by
 * {@link BiomeWaters#plantWater}, so the biomes of biome mods count). At most one try per chunk (and a few in the lush
 * caves), with a chance per water ({@link #chance}) times the config {@code spawn.wildPlantFrequency}; nothing with
 * {@code features.aquaticPlants.wildPlants}, {@code features.floatingPlants.wildPlants} off. Strong-light plants only in shallow water. Epiphytes and mosses sit on stone or wood:
 * on a soft bottom a stone (or in the jungle and mangroves a sunken root) is laid under them.
 */
public class WildPlantFeature implements Feature {
    /** M52e (26.3): features are datapack objects; the two kinds are registered as unit codecs of their instance. */
    public static final WildPlantFeature OPEN_WATER = new WildPlantFeature(false), CAVES = new WildPlantFeature(true);
    public static final com.mojang.serialization.MapCodec<WildPlantFeature> CODEC = com.mojang.serialization.MapCodec.unit(OPEN_WATER);
    public static final com.mojang.serialization.MapCodec<WildPlantFeature> CAVE_CODEC = com.mojang.serialization.MapCodec.unit(CAVES);

    /** Deepest water (blocks above the floor) for the strong-light plants and for the others. */
    public static final int SHALLOW = 4, DEEP = 12;

    /** Cave mode: the origin lies anywhere in the cave (height range), the floor is searched below it. */
    private final boolean cave;

    private WildPlantFeature(boolean cave) {
        this.cave = cave;
    }

    @Override
    public com.mojang.serialization.MapCodec<WildPlantFeature> codec() {
        return cave ? CAVE_CODEC : CODEC;
    }

    /** Chance of a try (one per chunk; in the lush caves per try of the cave feature) before {@code spawn.wildPlantFrequency}. */
    public static double chance(PlantWater w) {
        return switch (w) {
            case JUNGLE -> 0.5;
            case SWAMP, MANGROVE -> 0.4;
            case RIVER -> 0.35;
            case COLD -> 0.15;
            case LUSH_CAVES -> 0.35;
        };
    }

    /** Share (percent) of the tries that become a floating patch where the water has floating plants. */
    public static int floatingShare(PlantWater w) {
        return switch (w) {
            case SWAMP -> 45;
            case MANGROVE -> 50;
            case JUNGLE, COLD -> 25;
            case RIVER -> 15;
            case LUSH_CAVES -> 0;
        };
    }

    @Override
    public boolean place(WorldGenLevel level, net.minecraft.world.level.chunk.ChunkGenerator generator, RandomSource r, BlockPos origin) {
        // M33: the aquarium plants and the floating plants have their own switches (each with its group)
        if (!Features.enabled(org.overengineer.bettacraft.config.Feature.AQUATIC_WILD)
                && !Features.enabled(org.overengineer.bettacraft.config.Feature.FLOATING_WILD)) return false;
        PlantWater w = BiomeWaters.plantWater(level.getBiome(origin));
        if (w == null || (w == PlantWater.LUSH_CAVES) != cave) return false;
        if (r.nextDouble() >= chance(w) * CommonConfig.wildPlantFrequency()) return false;
        return place(level, origin, r, w, cave);
    }

    /** One group in the water {@code w} near {@code origin} (no chance roll, no biome check: the GameTests call it). */
    public static boolean place(WorldGenLevel level, BlockPos origin, RandomSource r, PlantWater w, boolean cave) {
        var floating = WildTables.wildFloating(w);
        boolean aquatic = Features.enabled(org.overengineer.bettacraft.config.Feature.AQUATIC_WILD);
        if (!floating.isEmpty() && (!aquatic || r.nextInt(100) < floatingShare(w))) {
            var t = floating.pick(new java.util.Random(r.nextLong()));
            boolean on = Features.enabled(t.plantItem() ? org.overengineer.bettacraft.config.Feature.AQUATIC_WILD
                    : org.overengineer.bettacraft.config.Feature.FLOATING_WILD);
            if (on && placeFloating(level, origin, r, t, cave)) return true;
        }
        var table = WildTables.wildPlants(w);
        if (table.isEmpty() || !aquatic) return false;
        return placeGroup(level, origin, r, table.pick(new java.util.Random(r.nextLong())), w, cave);
    }

    // ------------------------------------------------------------------ under water

    /** A group of one species on the bottom: 1-3 epiphytes, 2-4 mosses or rosettes, 3-6 stem plants, 4-8 carpet patches. */
    public static boolean placeGroup(WorldGenLevel level, BlockPos origin, RandomSource r, PlantSpecies s, PlantWater w, boolean cave) {
        WildPlantKind kind = WildPlantKind.of(s);
        if (kind == null) return false;
        var block = ModBlocks.WILD_PLANTS.get(kind).get();
        BlockState plant = block.withSpecies(s);
        int want = switch (kind) {
            case EPIPHYTE -> 1 + r.nextInt(3);
            case MOSS, ROSETTE -> 2 + r.nextInt(3);
            case STEM -> 3 + r.nextInt(4);
            case CARPET -> 4 + r.nextInt(5);
        };
        int radius = kind == WildPlantKind.CARPET ? 3 : 2;
        int maxDepth = s.light() == PlantSpecies.Light.HIGH || s.light() == PlantSpecies.Light.VERY_HIGH ? SHALLOW : DEEP;
        // the centre: the origin or another spot of the chunk that is under water (narrow rivers)
        BlockPos centre = null;
        for (int t = 0; t < 5 && centre == null; t++) {
            int x = origin.getX() + (t == 0 ? 0 : r.nextInt(15) - 7), z = origin.getZ() + (t == 0 ? 0 : r.nextInt(15) - 7);
            BlockPos p = floor(level, x, z, origin.getY(), cave);
            if (p != null && depth(level, p) <= maxDepth) centre = p;
        }
        if (centre == null) return false;
        int placed = 0;
        for (int t = 0; t < want * 4 && placed < want; t++) {
            int x = centre.getX() + (placed == 0 ? 0 : r.nextInt(radius * 2 + 1) - radius), z = centre.getZ() + (placed == 0 ? 0 : r.nextInt(radius * 2 + 1) - radius);
            BlockPos p = floor(level, x, z, centre.getY(), cave);
            if (p == null || Math.abs(p.getY() - centre.getY()) > 2) continue;
            int d = depth(level, p);
            if (d > maxDepth || d < (kind == WildPlantKind.STEM || kind == WildPlantKind.ROSETTE ? 2 : 1)) continue;
            if (!ground(level, p.below(), s, kind, w, r)) continue;
            level.setBlock(p, plant, 2);
            placed++;
        }
        return placed > 0;
    }

    /** The water block on the floor of the column (x, z), or null when the floor there is not under water. */
    @Nullable
    static BlockPos floor(WorldGenLevel level, int x, int z, int yHint, boolean cave) {
        if (!cave) {
            BlockPos p = new BlockPos(x, level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z), z);
            return water(level, p) && !water(level, p.below()) ? p : null;
        }
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = yHint + 4; y >= yHint - 8; y--) {
            m.set(x, y, z);
            if (water(level, m) && !water(level, m.below()) && level.getBlockState(m.below()).isFaceSturdy(level, m.below(), Direction.UP)) return m.immutable();
        }
        return null;
    }

    private static boolean water(WorldGenLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return s.is(Blocks.WATER) && level.getFluidState(p).isSourceOfType(Fluids.WATER);
    }

    /** Water blocks from {@code p} up to the surface (up to {@link #DEEP} + 1). */
    static int depth(WorldGenLevel level, BlockPos p) {
        int d = 0;
        BlockPos.MutableBlockPos m = p.mutable();
        while (d <= DEEP && level.getFluidState(m).isSourceOfType(Fluids.WATER)) {
            d++;
            m.move(Direction.UP);
        }
        return d;
    }

    private static boolean soft(BlockState s) {
        return s.is(BlockTags.SUBSTRATE_OVERWORLD) || s.is(BlockTags.SAND) || s.is(Blocks.GRAVEL) || s.is(Blocks.CLAY) || s.is(Blocks.MUD);
    }

    private static boolean hard(BlockState s) {
        return s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(Blocks.COBBLESTONE) || s.is(Blocks.MOSSY_COBBLESTONE) || s.is(BlockTags.LOGS)
                || s.is(Blocks.STONE_BRICKS) || s.is(Blocks.MOSSY_STONE_BRICKS) || s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.CLAY);
    }

    /**
     * Whether the plant may grow on the block {@code below}; epiphytes and mosses get a stone or a sunken root laid onto a
     * soft bottom (rarely: one block per plant).
     */
    private static boolean ground(WorldGenLevel level, BlockPos below, PlantSpecies s, WildPlantKind kind, PlantWater w, RandomSource r) {
        BlockState b = level.getBlockState(below);
        if (!b.isFaceSturdy(level, below, Direction.UP)) return false;
        return switch (kind) {
            case ROSETTE, STEM, CARPET -> soft(b) || s.alsoOnHardscape() && hard(b);
            case EPIPHYTE, MOSS -> {
                if (s == PlantSpecies.MARIMO || hard(b)) yield true; // the Marimo rolls on any bottom
                if (!soft(b)) yield false;
                level.setBlock(below, rock(w, r), 2);
                yield true;
            }
        };
    }

    /** A stone, or a sunken root (log lying in the water) in the jungle and the mangroves. */
    private static BlockState rock(PlantWater w, RandomSource r) {
        if ((w == PlantWater.JUNGLE || w == PlantWater.MANGROVE) && r.nextBoolean()) {
            return (w == PlantWater.JUNGLE ? Blocks.JUNGLE_LOG : Blocks.MANGROVE_LOG).defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, r.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z);
        }
        return switch (r.nextInt(3)) {
            case 0 -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case 1 -> Blocks.COBBLESTONE.defaultBlockState();
            default -> Blocks.STONE.defaultBlockState();
        };
    }

    // ------------------------------------------------------------------ on the surface

    /** A patch of one floating plant on the calm surface: 3-6 blocks, duckweed 4-9. */
    public static boolean placeFloating(WorldGenLevel level, BlockPos origin, RandomSource r, FloatingPlantType t, boolean cave) {
        if (cave || t == FloatingPlantType.LILY_PAD) return false;
        BlockState patch = ModBlocks.WILD_FLOATING_PLANT.get().defaultBlockState().setValue(WildFloatingPlantBlock.TYPE, t);
        int want = t == FloatingPlantType.DUCKWEED ? 4 + r.nextInt(6) : 3 + r.nextInt(4);
        BlockPos centre = null;
        for (int k = 0; k < 5 && centre == null; k++) {
            int x = origin.getX() + (k == 0 ? 0 : r.nextInt(15) - 7), z = origin.getZ() + (k == 0 ? 0 : r.nextInt(15) - 7);
            centre = surface(level, x, z);
        }
        if (centre == null) return false;
        int placed = 0;
        for (int k = 0; k < want * 4 && placed < want; k++) {
            int x = centre.getX() + (placed == 0 ? 0 : r.nextInt(7) - 3), z = centre.getZ() + (placed == 0 ? 0 : r.nextInt(7) - 3);
            BlockPos p = surface(level, x, z);
            if (p == null || p.getY() != centre.getY() || level.getBlockState(p).is(patch.getBlock())) continue;
            level.setBlock(p, patch, 2);
            placed++;
        }
        return placed > 0;
    }

    /** The air block above still water in the column (x, z), or null (no water, ice, running water, something on it). */
    @Nullable
    static BlockPos surface(WorldGenLevel level, int x, int z) {
        BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), z);
        BlockPos water = top.below();
        if (!water(level, water) || !level.getBlockState(top).isAir()) return null;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            // still water: the sides are water sources or solid ground, never running water
            var f = level.getFluidState(water.relative(d));
            if (!f.isEmpty() && !f.isSource()) return null;
        }
        return top;
    }
}
