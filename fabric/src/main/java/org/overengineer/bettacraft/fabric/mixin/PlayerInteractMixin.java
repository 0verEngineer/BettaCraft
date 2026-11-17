package org.overengineer.bettacraft.fabric.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.overengineer.bettacraft.fabric.FabricGameEvents;
import org.overengineer.bettacraft.platform.event.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** M52: NeoForge's EntityInteract event, where NeoForge fires it (Player#interactOn, also for interactions from code). */
@Mixin(Player.class)
public abstract class PlayerInteractMixin {
    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void bettacraft$entityInteract(Entity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (((Player) (Object) this).isSpectator()) return;
        var e = FabricGameEvents.post(new PlayerInteractEvent.EntityInteract((Player) (Object) this, hand, entity));
        if (e.isCanceled()) cir.setReturnValue(e.getCancellationResult());
    }
}
