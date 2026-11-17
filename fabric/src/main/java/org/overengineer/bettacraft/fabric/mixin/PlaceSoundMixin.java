package org.overengineer.bettacraft.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.PositionalBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** M52: NeoForge's positional sound type when a block is placed. */
@Mixin(BlockItem.class)
public abstract class PlaceSoundMixin {
    @WrapOperation(method = "place", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"))
    private SoundType bettacraft$sound(BlockState state, Operation<SoundType> op, @Local(argsOnly = true) BlockPlaceContext ctx) {
        if (state.getBlock() instanceof PositionalBlock.Sound s) return s.getSoundType(state, ctx.getLevel(), ctx.getClickedPos(), ctx.getPlayer());
        return op.call(state);
    }
}
