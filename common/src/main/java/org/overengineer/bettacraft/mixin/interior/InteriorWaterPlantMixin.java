package org.overengineer.bettacraft.mixin.interior;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Water plants of other mods ({@code #bettacraft:interior_water_plants}, e.g. Fintastic) survive in the dry interior
 * dimension (PLAN 7.3, M7). Outside the interior nothing changes.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class InteriorWaterPlantMixin {
    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void bettacraft$survivesDry(LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (level instanceof Level l && InteriorManager.isInterior(l)
                && ((BlockBehaviour.BlockStateBase) (Object) this).is(ModTags.INTERIOR_WATER_PLANTS)) {
            cir.setReturnValue(true);
        }
    }
}
