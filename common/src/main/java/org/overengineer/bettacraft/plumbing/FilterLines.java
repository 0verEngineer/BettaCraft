package org.overengineer.bettacraft.plumbing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.overengineer.bettacraft.tank.TankBlock;

/**
 * M40: the filter lines from a filter connector to the tanks. Each end is a {@link Dock}: the tank block and its side the line
 * docks on. M68: a line is the pipe network at the connector ({@link PipePorts#line}); its docks are the network's tank docks
 * (any pipe, glass or steel, bundled or not), or the tank the connector touches directly.
 */
public final class FilterLines {
    private FilterLines() {}

    /**
     * Where a line docks on a tank: the tank block and its side (pointing out of the tank, to the pipe); M68 part C: the slot of the
     * bundle that docks there (-1: a single pipe or the connector itself), so its own dock inside is asked.
     */
    public record Dock(BlockPos member, Direction face, int slot) {
        public Dock(BlockPos member, Direction face) {
            this(member, face, -1);
        }
    }

    /** Forgets every cached line (tests). */
    public static void forget() {
        PipeNetwork.forgetAll();
    }

    /** The docks the connector at {@code connector} (facing {@code facing}) leads to. */
    public static List<Dock> docks(Level level, BlockPos connector, Direction facing) {
        List<Dock> out = new ArrayList<>();
        PipePorts.Line l = PipePorts.line(level, connector, facing);
        if (l.network() == null && l.direct() != null && !(level.getBlockState(l.direct()).getBlock() instanceof TankBlock)) return out;
        for (PipeNetwork.Dock d : l.docks(level)) {
            Dock x = new Dock(d.member(), d.face(), d.slot());
            if (!out.contains(x)) out.add(x);
        }
        return out;
    }
}
