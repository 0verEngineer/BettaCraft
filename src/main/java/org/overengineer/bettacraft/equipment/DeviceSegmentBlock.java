package org.overengineer.bettacraft.equipment;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * M23: a piece of interior equipment with a motor or an air lift: the internal (motor) filter, the corner filter, the
 * flow pump and the air-driven sponge filter. The anchor block carries the power regulator ({@link #POWER} 0..10 =
 * 0..100 %), whether an intake sponge is fitted ({@link #SPONGE}, motor filters) and whether it runs right now
 * ({@link #ACTIVE}: the sponge filter gets air from an air pump). Block states of earlier versions load at 100 %, without
 * sponge, inactive (the server sets ACTIVE on its next care step).
 */
public class DeviceSegmentBlock extends SegmentBlock {
    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 10);
    public static final BooleanProperty SPONGE = BooleanProperty.create("sponge");
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public DeviceSegmentBlock(Kind kind, Supplier<? extends Item> item, Properties properties) {
        super(kind, item, properties);
        registerDefaultState(defaultBlockState().setValue(POWER, 10).setValue(SPONGE, false).setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWER, SPONGE, ACTIVE);
    }

    /** M23: inside the interior a sponge filter that gets air sends bursts of big bubbles out of its lift tube (client). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (kind() != Kind.SPONGE_FILTER || state.getValue(PART) != Part.TOP) return;
        Regulator.Device d = Regulator.find(level, pos);
        if (d == null) return;
        BlockState a = level.getBlockState(d.pos());
        if (!a.is(this) || !a.getValue(ACTIVE)) return;
        int scale = Math.max(1, pos.getY() - d.pos().getY() + 1);
        int[] shape = SegmentBlock.shape(Kind.SPONGE_FILTER, 16);
        org.overengineer.bettacraft.util.ClientHooks.sponge.burst(level, d.pos(), shape[0], scale);
    }

    /** Right-click with an empty hand: the power regulator (client screen). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!org.overengineer.bettacraft.config.CommonConfig.powerRegulator()) return InteractionResult.PASS;
        if (level.isClientSide) org.overengineer.bettacraft.util.ClientHooks.openRegulator(pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
