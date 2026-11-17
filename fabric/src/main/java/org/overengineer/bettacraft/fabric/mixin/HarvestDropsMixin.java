package org.overengineer.bettacraft.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.PositionalBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * M52: NeoForge's canHarvestBlock for the drops of a block broken by a player, decided before the block (and its block
 * entity, which knows the stone of a stone cabinet) is removed, like NeoForge does.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class HarvestDropsMixin {
    @Shadow protected ServerLevel level;
    @Shadow @Final protected ServerPlayer player;

    @Unique
    private Boolean bettacraft$harvest;

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void bettacraft$before(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = level.getBlockState(pos);
        bettacraft$harvest = state.getBlock() instanceof PositionalBlock.Harvest h ? h.canHarvestBlock(state, level, pos, player) : null;
    }

    @WrapOperation(method = "destroyBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;hasCorrectToolForDrops(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean bettacraft$harvest(ServerPlayer p, BlockState state, Operation<Boolean> op) {
        Boolean h = bettacraft$harvest;
        bettacraft$harvest = null;
        return h != null ? h : op.call(p, state);
    }
}
