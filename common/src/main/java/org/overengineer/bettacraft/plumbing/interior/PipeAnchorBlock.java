package org.overengineer.bettacraft.plumbing.interior;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * M40: the connection point of a filter pipe in the tank interior ({@link Plumb#ANCHOR}). The game puts it where a filter
 * pipe (or a filter connector, or the pipes of a filter from before M40) docks on the tank outside: on the wall, the top
 * or the floor of that tank block, in the middle of its face ({@link TankAnchors}). {@link #MOUNT} is the side of the
 * tank it sits on (pointing out of the tank). Interior pipes join it like the bulkhead's connection and lead the water on
 * to the guards and outlets. It cannot be broken by hand: it goes when the pipe outside goes.
 */
public class PipeAnchorBlock extends PlumbingBlock {
    public static final EnumProperty<Direction> MOUNT = EnumProperty.create("mount", Direction.class);
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
        registerDefaultState(defaultBlockState().setValue(MOUNT, Direction.DOWN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(MOUNT);
    }

    static VoxelShape shape(BlockState state) {
        return state.hasProperty(MOUNT) ? SHAPES.get(state.getValue(MOUNT)) : SHAPES.get(Direction.DOWN);
    }
}
