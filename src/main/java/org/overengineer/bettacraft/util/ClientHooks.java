package org.overengineer.bettacraft.util;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;

/**
 * M23: calls from common code (blocks) into the client (screens). The client sets the hooks at start-up; on a
 * dedicated server they stay no-ops.
 */
public final class ClientHooks {
    private ClientHooks() {}

    /** Opens the power regulator screen for the device at this position (client only). */
    public static volatile Consumer<BlockPos> regulator = p -> {};

    /** Client tick of an air stone in a tank interior (bubble column, sound). */
    @FunctionalInterface
    public interface Bubbles {
        void tick(net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state, float surfaceY);
    }

    public static volatile Bubbles bubbles = (l, p, s, y) -> {};

    /** Client tick of an air pump (hum). */
    public static volatile Consumer<org.overengineer.bettacraft.equipment.AirPumpBlockEntity> pumpTick = b -> {};

    public static void openRegulator(BlockPos pos) {
        regulator.accept(pos.immutable());
    }

    /** M24: the water change controller and the multiblock filter screens. */
    public static volatile Consumer<BlockPos> waterChanger = p -> {};
    public static volatile Consumer<BlockPos> filter = p -> {};

    public static void openWaterChanger(BlockPos pos) {
        waterChanger.accept(pos.immutable());
    }

    public static void openFilter(BlockPos pos) {
        filter.accept(pos.immutable());
    }
}
