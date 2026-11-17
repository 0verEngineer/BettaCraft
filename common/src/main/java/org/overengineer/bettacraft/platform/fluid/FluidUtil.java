package org.overengineer.bettacraft.platform.fluid;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.platform.Platform;

/** M52: fluid container items (buckets, other mods' tanks) and tanks, through the platform layer. */
public final class FluidUtil {
    private FluidUtil() {}

    public static Optional<IFluidHandlerItem> getFluidHandler(ItemStack stack) {
        return Platform.get().fluidHandler(stack);
    }

    public static boolean interactWithFluidHandler(Player player, InteractionHand hand, Level level, BlockPos pos, @Nullable Direction side) {
        return Platform.get().interactWithFluidHandler(player, hand, level, pos, side);
    }

    public static boolean interactWithFluidHandler(Player player, InteractionHand hand, IFluidHandler handler) {
        return Platform.get().interactWithFluidHandler(player, hand, handler);
    }

    public static Platform.FluidActionResult tryEmptyContainer(ItemStack container, IFluidHandler dest, int maxAmount, @Nullable Player player, boolean doDrain) {
        return Platform.get().tryEmptyContainer(container, dest, maxAmount, player, doDrain);
    }

    public static Platform.FluidActionResult tryFillContainer(ItemStack container, IFluidHandler source, int maxAmount, @Nullable Player player, boolean doFill) {
        return Platform.get().tryFillContainer(container, source, maxAmount, player, doFill);
    }
}
