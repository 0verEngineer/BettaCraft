package org.overengineer.bettacraft.reef;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModBlocks;

/**
 * M22.3: a colony of one Bettacraft coral (or the bubble tip anemone) on a warm ocean reef. Placed after the vanilla
 * reef (the vegetation step of the warm ocean); it looks for the tops of vanilla coral blocks with water above around the
 * spot and settles a few colonies of one species there. Nothing happens away from a reef or with
 * {@code features.wildCorals} off.
 */
public class WildCoralFeature extends Feature<NoneFeatureConfiguration> {
    /** Search radius around the spot. */
    public static final int RADIUS = 6;

    public WildCoralFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    /** The species of a colony ({@link org.overengineer.bettacraft.sources.WildTables#WILD_CORALS}). */
    public static PlantSpecies pick(RandomSource r) {
        return org.overengineer.bettacraft.sources.WildTables.WILD_CORALS.pick(new java.util.Random(r.nextLong()));
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        if (!CommonConfig.wildCorals()) return false;
        return place(ctx.level(), ctx.origin(), ctx.random());
    }

    /** Settles 2-5 colonies of one species on the coral tops near {@code origin}; true if at least one grew. */
    public static boolean place(WorldGenLevel level, BlockPos origin, RandomSource r) {
        PlantSpecies s = pick(r);
        BlockState coral = ModBlocks.WILD_CORALS.get(s).get().defaultBlockState();
        int want = 2 + r.nextInt(4), placed = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int tries = 0; tries < 48 && placed < want; tries++) {
            int x = origin.getX() + r.nextInt(RADIUS * 2 + 1) - RADIUS, z = origin.getZ() + r.nextInt(RADIUS * 2 + 1) - RADIUS;
            // the spot lies on the sea floor (placement heightmap): look for a coral top in water a few blocks above and below
            int top = origin.getY();
            for (int y = top + 8; y >= top - 6; y--) {
                p.set(x, y, z);
                if (!level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.WATER) || !level.getFluidState(p).is(FluidTags.WATER)) continue;
                BlockState below = level.getBlockState(p.below());
                if (!below.is(BlockTags.CORAL_BLOCKS)) continue;
                if (!level.getFluidState(p.above()).is(FluidTags.WATER)) continue;
                level.setBlock(p, coral, 2);
                placed++;
                break;
            }
        }
        return placed > 0;
    }
}
