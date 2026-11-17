package org.overengineer.bettacraft.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.PositionalBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** M52: NeoForge's canHarvestBlock for the breaking speed (a stone cabinet needs a pickaxe like its stone). */
@Mixin(BlockBehaviour.class)
public abstract class HarvestMixin {
    @WrapOperation(method = "getDestroyProgress", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;hasCorrectToolForDrops(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean bettacraft$harvest(Player player, BlockState state, Operation<Boolean> op, @Local(argsOnly = true) BlockGetter level,
                                       @Local(argsOnly = true) BlockPos pos) {
        if (state.getBlock() instanceof PositionalBlock.Harvest h) return h.canHarvestBlock(state, level, pos, player);
        return op.call(player, state);
    }
}
