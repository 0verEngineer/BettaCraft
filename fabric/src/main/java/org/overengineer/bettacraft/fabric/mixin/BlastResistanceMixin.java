package org.overengineer.bettacraft.fabric.mixin;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.overengineer.bettacraft.platform.PositionalBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** M52: NeoForge's positional blast resistance (a stone cabinet resists like its stone). */
@Mixin(ExplosionDamageCalculator.class)
public abstract class BlastResistanceMixin {
    @Inject(method = "getBlockExplosionResistance", at = @At("HEAD"), cancellable = true)
    private void bettacraft$positional(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, FluidState fluid,
                                      CallbackInfoReturnable<Optional<Float>> cir) {
        if (state.getBlock() instanceof PositionalBlock.Blast b) {
            cir.setReturnValue(Optional.of(Math.max(b.getExplosionResistance(state, level, pos, explosion), fluid.getExplosionResistance())));
        }
    }
}
