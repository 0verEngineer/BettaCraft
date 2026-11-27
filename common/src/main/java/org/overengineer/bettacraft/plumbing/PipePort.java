package org.overengineer.bettacraft.plumbing;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * M68: a block with pipe ports: the network of the pipe that joins it on a port face gets that port's role ({@link PipeRole}).
 * The multiblock filter's inlet and outlet ({@code FilterPartBlock}); part B: the water change controller's four ports. Pipes
 * join a port face like a pipe (one line per port face, also from a bundle); the port is no fluid end of the network (the
 * block works through its lines itself, see {@link PipePorts#line}).
 */
public interface PipePort {
    /**
     * The role of the port on {@code face} of this block ({@code face} points out of the block, towards the pipe), or null when
     * there is no port on that face. Must depend on the block state only (it is also asked while a chunk is meshed).
     */
    @Nullable
    PipeRole pipeRole(BlockGetter level, BlockPos pos, BlockState state, Direction face);
}
