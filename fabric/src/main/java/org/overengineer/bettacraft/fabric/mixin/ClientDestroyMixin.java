package org.overengineer.bettacraft.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.PlayerDestroyable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** M52: NeoForge's onDestroyedByPlayer in the client's prediction (a refused break does not flicker). */
@Mixin(MultiPlayerGameMode.class)
public abstract class ClientDestroyMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "destroyBlock", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;playerWillDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private void bettacraft$destroyedByPlayer(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Level level = minecraft.level;
        if (level == null || minecraft.player == null) return;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof PlayerDestroyable d)) return;
        boolean removed = d.onDestroyedByPlayer(state, level, pos, minecraft.player, false, level.getFluidState(pos));
        if (removed) state.getBlock().destroy(level, pos, state);
        cir.setReturnValue(removed);
    }
}
