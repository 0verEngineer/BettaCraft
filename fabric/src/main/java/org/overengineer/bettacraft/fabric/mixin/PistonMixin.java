package org.overengineer.bettacraft.fabric.mixin;

import org.overengineer.bettacraft.fabric.FabricGameEvents;
import org.overengineer.bettacraft.platform.event.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** M52: fires the common game event NeoForge fires here (Fabric API has no callback for it). */
@Mixin(net.minecraft.world.level.block.piston.PistonBaseBlock.class)
public abstract class PistonMixin {

    @Inject(method = "triggerEvent", at = @At("HEAD"), cancellable = true)
    private void bettacraft$piston(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
            int id, int param, CallbackInfoReturnable<Boolean> cir) {
        if (id != 0 && id != 1 && id != 2) return;
        var dir = state.getValue(net.minecraft.world.level.block.piston.PistonBaseBlock.FACING);
        if (FabricGameEvents.post(new PistonEvent.Pre(level, pos, dir, id == 0)).isCanceled()) cir.setReturnValue(false);
    }
}
