package org.overengineer.bettacraft.compat.create;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.tank.TankBlock;

/**
 * M41: the drive adapter. {@code FACING} is the side of its shaft (pointing away from the tank); its back (a mounting plate)
 * sits on a tank wall or under a tank. Placed against a tank it faces away from it; next to a tank it finds the tank by
 * itself (below it first). Without a tank at its back it turns idle.
 */
public class DriveAdapterBlock extends DirectionalKineticBlock implements IBE<DriveAdapterBlockEntity> {
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction d : Direction.values()) {
            // modelled for NORTH: the plate on the south side (z 14..16), the casing (z 5..14), the bearing ring (z 4..5)
            SHAPES.put(d, Shapes.or(box(d, 0, 0, 14, 16, 16, 16), box(d, 2, 2, 5, 14, 14, 14), box(d, 5, 5, 3, 11, 11, 5)));
        }
    }

    /** A box of the model for NORTH, turned to {@code facing}. */
    private static VoxelShape box(Direction facing, double x0, double y0, double z0, double x1, double y1, double z1) {
        double[] a = turn(facing, x0, y0, z0), b = turn(facing, x1, y1, z1);
        return net.minecraft.world.level.block.Block.box(Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]),
                Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2]));
    }

    /** A point of the NORTH model (pixels) turned like the block state does (y for the sides, x for up and down). */
    static double[] turn(Direction facing, double x, double y, double z) {
        return switch (facing) {
            case NORTH -> new double[]{x, y, z};
            case SOUTH -> new double[]{16 - x, y, 16 - z};
            case EAST -> new double[]{16 - z, y, x};
            case WEST -> new double[]{z, y, 16 - x};
            case UP -> new double[]{x, 16 - z, y};
            case DOWN -> new double[]{x, z, 16 - y};
        };
    }

    public DriveAdapterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction clicked = ctx.getClickedFace();
        BlockPos pos = ctx.getClickedPos();
        LevelReader level = ctx.getLevel();
        // placed on a tank: facing away from it
        if (level.getBlockState(pos.relative(clicked.getOpposite())).getBlock() instanceof TankBlock) {
            return defaultBlockState().setValue(FACING, clicked);
        }
        // beside a tank: under it first, then the side the player looks at, then any
        if (level.getBlockState(pos.above()).getBlock() instanceof TankBlock) return defaultBlockState().setValue(FACING, Direction.DOWN);
        for (Direction d : ctx.getNearestLookingDirections()) {
            if (level.getBlockState(pos.relative(d)).getBlock() instanceof TankBlock) return defaultBlockState().setValue(FACING, d.getOpposite());
        }
        return defaultBlockState().setValue(FACING, clicked);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    /** No loot table (it would be data of a block that exists only with Create): the block drops itself. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(this));
    }

    @Override
    public Class<DriveAdapterBlockEntity> getBlockEntityClass() {
        return DriveAdapterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends DriveAdapterBlockEntity> getBlockEntityType() {
        return CreateDrive.ADAPTER_BE.get();
    }
}
