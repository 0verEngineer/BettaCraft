package org.overengineer.bettacraft.equipment;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
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

    /** Right-click with an empty hand: the power regulator (client screen). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!Features.enabled(Feature.POWER_REGULATOR)) return InteractionResult.PASS;
        if (level.isClientSide) org.overengineer.bettacraft.util.ClientHooks.openRegulator(pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
