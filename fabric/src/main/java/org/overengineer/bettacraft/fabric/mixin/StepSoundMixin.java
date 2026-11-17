package org.overengineer.bettacraft.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.PositionalBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** M52: NeoForge's positional sound type for steps (a stone cabinet sounds like its stone, a covered pipe like its cover). */
@Mixin(Entity.class)
public abstract class StepSoundMixin {
    @WrapOperation(method = "playStepSound", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"))
    private SoundType bettacraft$sound(BlockState state, Operation<SoundType> op, @Local(argsOnly = true) BlockPos pos) {
        if (state.getBlock() instanceof PositionalBlock.Sound s) return s.getSoundType(state, ((Entity) (Object) this).level(), pos, (Entity) (Object) this);
        return op.call(state);
    }
}
