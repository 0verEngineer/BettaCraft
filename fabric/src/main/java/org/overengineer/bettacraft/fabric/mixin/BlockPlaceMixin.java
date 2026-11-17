package org.overengineer.bettacraft.fabric.mixin;

import org.overengineer.bettacraft.fabric.FabricGameEvents;
import org.overengineer.bettacraft.platform.event.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** M52: fires the common game event NeoForge fires here (Fabric API has no callback for it). */
@Mixin(net.minecraft.world.item.BlockItem.class)
public abstract class BlockPlaceMixin {

    @Inject(method = "placeBlock", at = @At("HEAD"), cancellable = true)
    private void bettacraft$place(net.minecraft.world.item.context.BlockPlaceContext ctx, net.minecraft.world.level.block.state.BlockState state,
            CallbackInfoReturnable<Boolean> cir) {
        if (FabricGameEvents.post(new BlockEvent.EntityPlaceEvent(ctx.getLevel(), ctx.getClickedPos(), state, ctx.getPlayer())).isCanceled()) cir.setReturnValue(false);
    }
}
