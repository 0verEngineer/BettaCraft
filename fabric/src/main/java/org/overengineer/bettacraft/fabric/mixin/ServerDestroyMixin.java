package org.overengineer.bettacraft.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.PlayerDestroyable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * M52: NeoForge's onDestroyedByPlayer on the server: a {@link PlayerDestroyable} block removes itself (and may refuse:
 * a tank with a player inside) where vanilla calls playerWillDestroy and removes it; the rest as vanilla. Applied after
 * Fabric's block break event (priority), so the event still comes first.
 */
@Mixin(value = ServerPlayerGameMode.class, priority = 1500)
public abstract class ServerDestroyMixin {
    @Shadow protected ServerLevel level;
    @Shadow @Final protected ServerPlayer player;

    @Shadow public abstract boolean isCreative();

    @Inject(method = "destroyBlock", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;playerWillDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private void bettacraft$destroyedByPlayer(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof PlayerDestroyable d)) return;
        Block block = state.getBlock();
        BlockEntity be = level.getBlockEntity(pos);
        boolean correct = player.hasCorrectToolForDrops(state);
        boolean removed = d.onDestroyedByPlayer(state, level, pos, player, !isCreative() && correct, level.getFluidState(pos));
        if (removed) block.destroy(level, pos, state);
        if (isCreative()) {
            cir.setReturnValue(true);
            return;
        }
        ItemStack tool = player.getMainHandItem();
        ItemStack copy = tool.copy();
        tool.mineBlock(level, state, pos, player);
        if (removed && correct) block.playerDestroy(level, player, pos, state, be, copy);
        cir.setReturnValue(true);
    }
}
