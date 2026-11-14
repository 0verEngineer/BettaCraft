package org.overengineer.bettacraft.equipment;

import com.mojang.serialization.MapCodec;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * M23: LED panel, the middle light tier. One item places a panel two blocks wide (the second half lies away from the
 * player, {@link #PARTNER_POS}); panels in a row along their length ({@link #AXIS}) join into one long panel. Gives
 * light 15 plus {@code lights.panelBoost} on the part of the tank it covers. Colour, redstone, FE, dimmer and timer
 * like the light bar.
 */
public class LedPanelBlock extends TankLightBlock {
    public static final MapCodec<LedPanelBlock> CODEC = simpleCodec(LedPanelBlock::new);
    /** The other half of the panel lies in the positive direction across the length axis. */
    public static final BooleanProperty PARTNER_POS = BooleanProperty.create("partner_pos");

    public LedPanelBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PARTNER_POS, true));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(PARTNER_POS);
    }

    @Override
    public double boost() {
        return org.overengineer.bettacraft.config.CommonConfig.ledTiers()
                ? org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.LIGHT_PANEL_BOOST, 2.5) : 0;
    }

    /** Direction from this half to the other one. */
    public static Direction partnerDir(BlockState s) {
        Direction.Axis across = s.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        return Direction.fromAxisAndDirection(across, s.getValue(PARTNER_POS) ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        double y0 = state.getValue(HANGING) ? 14 : 0;
        return box(0, y0, 0, 16, y0 + 2, 16);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        boolean hanging = ctx.getClickedFace() == Direction.DOWN;
        Direction forward = ctx.getHorizontalDirection();
        Direction.Axis axis = forward.getClockWise().getAxis();
        boolean partnerPos = forward.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        // join a panel next to it along its length (same side of the partner, so the two strips line up)
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockState n = level.getBlockState(pos.relative(d));
            if (n.getBlock() == this && n.getValue(AXIS) == d.getAxis() && n.getValue(HANGING) == hanging) {
                axis = d.getAxis();
                partnerPos = n.getValue(PARTNER_POS);
                break;
            }
        }
        BlockState s = defaultBlockState().setValue(AXIS, axis).setValue(HANGING, hanging).setValue(PARTNER_POS, partnerPos)
                .setValue(POWERED, level.hasNeighborSignal(pos));
        BlockPos other = pos.relative(partnerDir(s));
        if (!level.getBlockState(other).canBeReplaced(ctx) || !level.getWorldBorder().isWithinBounds(other)) return null;
        return connectPanel(s, level, pos);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;
        Direction d = partnerDir(state);
        BlockPos other = pos.relative(d);
        BlockState half = state.setValue(PARTNER_POS, !state.getValue(PARTNER_POS))
                .setValue(POWERED, level.hasNeighborSignal(other));
        level.setBlock(other, connectPanel(half, level, other), Block.UPDATE_ALL);
        level.blockUpdated(pos, Blocks.AIR);
    }

    private BlockState connectPanel(BlockState s, LevelAccessor level, BlockPos pos) {
        Direction neg = Direction.fromAxisAndDirection(s.getValue(AXIS), Direction.AxisDirection.NEGATIVE);
        return s.setValue(NEG, joinsPanel(s, level.getBlockState(pos.relative(neg))))
                .setValue(POS, joinsPanel(s, level.getBlockState(pos.relative(neg.getOpposite()))));
    }

    private boolean joinsPanel(BlockState self, BlockState other) {
        return other.getBlock() == this && other.getValue(AXIS) == self.getValue(AXIS) && other.getValue(HANGING) == self.getValue(HANGING)
                && other.getValue(PARTNER_POS) == self.getValue(PARTNER_POS);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (dir == partnerDir(state)) {
            // the other half is gone: this one goes too (it drops nothing, the broken half dropped the panel)
            boolean partner = neighbor.getBlock() == this && neighbor.getValue(PARTNER_POS) != state.getValue(PARTNER_POS)
                    && neighbor.getValue(AXIS) == state.getValue(AXIS) && neighbor.getValue(HANGING) == state.getValue(HANGING);
            return partner ? state : Blocks.AIR.defaultBlockState();
        }
        if (dir.getAxis() == state.getValue(AXIS)) return connectPanel(state, level, pos);
        return state;
    }

    /** The whole panel: both halves and every panel joined along the length. */
    @Override
    protected List<BlockPos> fixture(Level level, BlockPos pos, BlockState state) {
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        todo.add(pos);
        seen.add(pos);
        while (!todo.isEmpty() && out.size() < MAX_BAR * 2) {
            BlockPos p = todo.poll();
            BlockState s = level.getBlockState(p);
            if (s.getBlock() != this) continue;
            out.add(p);
            List<BlockPos> next = new ArrayList<>();
            Direction neg = Direction.fromAxisAndDirection(s.getValue(AXIS), Direction.AxisDirection.NEGATIVE);
            if (s.getValue(NEG)) next.add(p.relative(neg));
            if (s.getValue(POS)) next.add(p.relative(neg.getOpposite()));
            next.add(p.relative(partnerDir(s)));
            for (BlockPos n : next) if (seen.add(n)) todo.add(n);
        }
        return out;
    }
}
