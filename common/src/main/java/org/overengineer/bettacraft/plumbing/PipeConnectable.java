package org.overengineer.bettacraft.plumbing;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * M24: a block of ours that pipes join on some faces although it offers no fluid handler there itself (the water pump
 * pulls and pushes on its own; the connectors of the multiblock filter).
 */
public interface PipeConnectable {
    /** Whether a pipe on {@code face} of this block joins it. */
    boolean pipeConnects(BlockState state, Direction face);
}
