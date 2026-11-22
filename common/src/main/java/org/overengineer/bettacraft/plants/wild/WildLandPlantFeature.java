package org.overengineer.bettacraft.plants.wild;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.sources.WildTables;
import org.overengineer.bettacraft.sources.WildTables.LandPlace;
import org.overengineer.bettacraft.world.BiomeWaters;

/**
 * M56: a small group of one wild terrarium plant on the land of its biome ({@link WildTables#wildLandPlants}; the land by
 * {@link BiomeWaters#landPlace}, so the biomes of biome mods count): bromeliads, ferns and orchids on the jungle floor, the
 * mosses on the moss of the lush caves, carnivorous plants and sedges in swamps, cacti and succulents in deserts and badlands,
 * grasses and aloes in savannas. At most one try per chunk (three in the lush caves) with a chance per place
 * ({@link #chance}) times {@code spawn.wildPlantFrequency}; nothing with {@code features.terrariumPlants.wildPlants} off.
 */
public class WildLandPlantFeature implements Feature {
    /** 26.3: features are datapack objects; the two kinds are registered as unit codecs of their instance. */
    public static final WildLandPlantFeature LAND = new WildLandPlantFeature(false), CAVES = new WildLandPlantFeature(true);
    public static final com.mojang.serialization.MapCodec<WildLandPlantFeature> CODEC = com.mojang.serialization.MapCodec.unit(LAND);
    public static final com.mojang.serialization.MapCodec<WildLandPlantFeature> CAVE_CODEC = com.mojang.serialization.MapCodec.unit(CAVES);

    private final boolean cave;

    private WildLandPlantFeature(boolean cave) {
        this.cave = cave;
    }

    @Override
    public com.mojang.serialization.MapCodec<WildLandPlantFeature> codec() {
        return cave ? CAVE_CODEC : CODEC;
    }

    /** Chance of a try per chunk (per try of the cave feature) before {@code spawn.wildPlantFrequency}. */
    public static double chance(LandPlace p) {
        return switch (p) {
            case JUNGLE -> 0.35;
            case LUSH_CAVES -> 0.3;
            case SWAMP -> 0.3;
            case DESERT, SAVANNA -> 0.2;
        };
    }

    @Override
    public boolean place(WorldGenLevel level, net.minecraft.world.level.chunk.ChunkGenerator generator, RandomSource r, BlockPos origin) {
        if (!Features.enabled(org.overengineer.bettacraft.config.Feature.TERRARIUM_PLANTS_WILD)) return false;
        LandPlace p = BiomeWaters.landPlace(level.getBiome(origin));
        if (p == null || (p == LandPlace.LUSH_CAVES) != cave) return false;
        if (r.nextDouble() >= chance(p) * CommonConfig.wildPlantFrequency()) return false;
        return place(level, origin, r, p, cave);
    }

    /** One group near {@code origin} (no chance roll, no biome check: the GameTests call it). */
    public static boolean place(WorldGenLevel level, BlockPos origin, RandomSource r, LandPlace p, boolean cave) {
        var table = WildTables.wildLandPlants(p);
        if (table.isEmpty()) return false;
        return placeGroup(level, origin, r, table.pick(new java.util.Random(r.nextLong())), cave);
    }

    /** A group of one species: 1-2 cacti or epiphytes, 2-4 rosettes or stem plants, 3-5 patches of moss or carpet. */
    public static boolean placeGroup(WorldGenLevel level, BlockPos origin, RandomSource r, PlantSpecies s, boolean cave) {
        var holder = ModBlocks.WILD_LAND_PLANTS.get(s);
        if (holder == null) return false;
        BlockState plant = holder.get().defaultBlockState();
        int want = switch (s.form()) {
            case CACTUS, EPIPHYTE -> 1 + r.nextInt(2);
            case MOSS, CARPET -> 3 + r.nextInt(3);
            default -> 2 + r.nextInt(3);
        };
        int radius = 2;
        int placed = 0;
        for (int t = 0; t < want * 5 && placed < want; t++) {
            int x = origin.getX() + (t == 0 ? 0 : r.nextInt(radius * 2 + 1) - radius), z = origin.getZ() + (t == 0 ? 0 : r.nextInt(radius * 2 + 1) - radius);
            BlockPos at = spot(level, x, z, origin.getY(), cave);
            if (at == null || !WildLandPlantBlock.canGrowOn(s, level.getBlockState(at.below()))) continue;
            if (!level.getBlockState(at.below()).isFaceSturdy(level, at.below(), Direction.UP)) continue;
            level.setBlock(at, plant, 2);
            placed++;
        }
        return placed > 0;
    }

    /** The free block on the ground of the column (x, z): on the surface (under the leaves of the trees) or in the cave. */
    @Nullable
    static BlockPos spot(WorldGenLevel level, int x, int z, int yHint, boolean cave) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        if (!cave) {
            m.set(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            return free(level, m) ? m.immutable() : null;
        }
        for (int y = yHint + 4; y >= yHint - 8; y--) {
            m.set(x, y, z);
            if (free(level, m) && !level.getBlockState(m.below()).isAir()) return m.immutable();
        }
        return null;
    }

    /** Air (or short grass or a fern, which the plant replaces) without water. */
    private static boolean free(WorldGenLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return (s.isAir() || s.is(net.minecraft.world.level.block.Blocks.SHORT_GRASS) || s.is(net.minecraft.world.level.block.Blocks.FERN))
                && level.getFluidState(p).isEmpty();
    }
}
