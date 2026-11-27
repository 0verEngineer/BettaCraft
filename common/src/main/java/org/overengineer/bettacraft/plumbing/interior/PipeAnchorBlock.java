package org.overengineer.bettacraft.plumbing.interior;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.plumbing.PipeBlock;
import org.overengineer.bettacraft.plumbing.PipeRing;

/**
 * M40: the connection point of a filter pipe in the tank interior ({@link Plumb#ANCHOR}). The game puts it where a filter
 * pipe (or a filter connector, or the pipes of a filter from before M40) docks on the tank outside: on the wall, the top
 * or the floor of that tank block ({@link TankAnchors}). {@link #MOUNT} is the side of the tank it sits on (pointing out of
 * the tank). Interior pipes join it like the bulkhead's connection and lead the water on to the guards and outlets. It
 * cannot be broken by hand: it goes when the pipe outside goes.
 * <p>
 * M68 part C: where a pipe bundle docks with two to four lines, every line has its own dock at the spot of its flange
 * ({@link #LINE} 1..4 = slot 0..3 of the bundle; 0 = the one dock in the middle of the face for a single pipe). Its
 * {@link #RING} shows the role colour of its line outside (blue intake, orange return, green fresh, brown waste water).
 * No facing and no activity (the outlets have them): earlier states with them load without them.
 */
public class PipeAnchorBlock extends PlumbingBlock {
    public static final DirectionProperty MOUNT = DirectionProperty.create("mount");
    /** M68 part C: 0 = the dock of a single pipe (middle of the face), 1..4 = the line of bundle slot 0..3. */
    public static final IntegerProperty LINE = IntegerProperty.create("line", 0, 4);
    /** M68 part C: the ring colour of its line. */
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<PipeRing> RING = PipeBlock.RING;
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        // a flange on the wall (2 px) and a short stub into the tank
        SHAPES.put(Direction.DOWN, Shapes.or(box(2, 0, 2, 14, 2, 14), box(5, 2, 5, 11, 8, 11)));
        SHAPES.put(Direction.UP, Shapes.or(box(2, 14, 2, 14, 16, 14), box(5, 8, 5, 11, 14, 11)));
        SHAPES.put(Direction.NORTH, Shapes.or(box(2, 2, 0, 14, 14, 2), box(5, 5, 2, 11, 11, 8)));
        SHAPES.put(Direction.SOUTH, Shapes.or(box(2, 2, 14, 14, 14, 16), box(5, 5, 8, 11, 11, 14)));
        SHAPES.put(Direction.WEST, Shapes.or(box(0, 2, 2, 2, 14, 14), box(2, 5, 5, 8, 11, 11)));
        SHAPES.put(Direction.EAST, Shapes.or(box(14, 2, 2, 16, 14, 14), box(8, 5, 5, 14, 11, 11)));
    }

    public PipeAnchorBlock(Properties properties) {
        super(Plumb.ANCHOR, properties);
        registerDefaultState(defaultBlockState().setValue(MOUNT, Direction.DOWN).setValue(LINE, 0).setValue(RING, PipeRing.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        PipeBlock.SIDES.values().forEach(b::add);
        b.add(MOUNT, LINE, RING);
    }

    static VoxelShape shape(BlockState state) {
        return state.hasProperty(MOUNT) ? SHAPES.get(state.getValue(MOUNT)) : SHAPES.get(Direction.DOWN);
    }
}
