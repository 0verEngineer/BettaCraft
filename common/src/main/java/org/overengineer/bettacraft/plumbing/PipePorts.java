package org.overengineer.bettacraft.plumbing;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.tank.TankBlock;

/**
 * M68: the line that leaves a port ({@link PipePort}): what a device with ports asks to work through its pipes. The multiblock
 * filter asks it for its inlets and outlets; part B's water change controller for its four ports.
 * <p>
 * A port face meets either a pipe (joined back to the port: the line is that pipe's network, carrying at most its
 * {@link PipeNetwork#rate()}, with its tank docks, fluid ends and the other ports on it), or a block right in front of it
 * without a pipe in between (a tank, a reservoir, another mod's tank: no limit by pipes), or nothing.
 */
public final class PipePorts {
    private PipePorts() {}

    /** No limit (the port touches the block directly). */
    public static final int UNLIMITED = Integer.MAX_VALUE;

    /**
     * The line at a port face. {@code network}: the pipe network joined to it (null without a pipe); {@code direct}: the block
     * right in front of the face when it is no pipe (a tank, a device; null otherwise); {@code rate}: mB per tick the line
     * carries at most ({@link #UNLIMITED} for a direct neighbour, 0 for none).
     */
    public record Line(@Nullable PipeNetwork network, @Nullable BlockPos direct, Direction face, int rate) {
        static final Line NONE_UP = new Line(null, null, Direction.UP, 0);

        /** Nothing at this port. */
        public boolean none() {
            return network == null && direct == null;
        }

        /** The network touches ports of two roles: the line does nothing. */
        public boolean shortCircuit() {
            return network != null && network.shortCircuit();
        }

        /** The tanks the line reaches: the docks of its network, or the tank right in front of the port. */
        public List<PipeNetwork.Dock> docks(Level level) {
            if (network != null) return network.docks();
            List<PipeNetwork.Dock> out = new ArrayList<>();
            if (direct != null && level.isLoaded(direct) && level.getBlockState(direct).getBlock() instanceof TankBlock) out.add(new PipeNetwork.Dock(direct, face.getOpposite()));
            return out;
        }

        /** The blocks with a fluid handler at the ends of the line (its network's ends that are no tank, or the block in front). */
        public List<PipeNetwork.End> ends(Level level) {
            if (network != null) return network.ends();
            List<PipeNetwork.End> out = new ArrayList<>();
            if (direct != null) out.add(new PipeNetwork.End(direct, face.getOpposite()));
            return out;
        }
    }

    /** The line that leaves the port at {@code port} on its face {@code face} (pointing out of the port). */
    public static Line line(Level level, BlockPos port, Direction face) {
        BlockPos n = port.relative(face);
        if (!level.isLoaded(n)) return new Line(null, null, face, 0);
        BlockState s = level.getBlockState(n);
        if (Pipes.isPipe(s)) {
            PipeNetwork net = PipeNetwork.towards(level, n, face.getOpposite());
            return net == null ? new Line(null, null, face, 0) : new Line(net, null, face, net.rate());
        }
        if (s.isAir()) return new Line(null, null, face, 0);
        return new Line(null, n.immutable(), face, UNLIMITED);
    }
}
