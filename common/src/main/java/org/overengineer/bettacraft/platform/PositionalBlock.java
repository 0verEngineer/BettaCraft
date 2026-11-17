package org.overengineer.bettacraft.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * M52: blocks whose sound, blast resistance and harvesting depend on their block entity (a stone cabinet, a covered pipe).
 * NeoForge calls these methods by their names; Fabric's glue from the places vanilla asks the block state (mixins in
 * {@code fabric/mixin}: steps, placing, breaking, explosions, the right tool).
 */
public final class PositionalBlock {
    private PositionalBlock() {}

    public interface Sound {
        SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity);
    }

    public interface Blast {
        float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion);
    }

    public interface Harvest {
        boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player);
    }
}
