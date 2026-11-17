package org.overengineer.bettacraft.mixin.interior;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Floating plants (lily pad, Fintastic duckweed and lotus pads) are placed on water only. The interior is dry, so
 * there they are placed like a normal block against the clicked face (PLAN 5.4/7.3, M7).
 */
@Mixin(PlaceOnWaterBlockItem.class)
public abstract class FloatingPlantItemMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void bettacraft$placeInInterior(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (InteriorManager.isInterior(context.getLevel())) {
            cir.setReturnValue(((BlockItem) (Object) this).place(new BlockPlaceContext(context)));
        }
    }
}
