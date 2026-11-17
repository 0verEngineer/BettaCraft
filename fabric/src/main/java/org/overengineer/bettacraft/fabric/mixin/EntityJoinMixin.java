package org.overengineer.bettacraft.fabric.mixin;

import org.overengineer.bettacraft.fabric.FabricGameEvents;
import org.overengineer.bettacraft.platform.event.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** M52: fires the common game event NeoForge fires here (Fabric API has no callback for it). */
@Mixin(net.minecraft.world.level.entity.PersistentEntitySectionManager.class)
public abstract class EntityJoinMixin {

    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void bettacraft$join(net.minecraft.world.level.entity.EntityAccess access, boolean worldGenSpawned, CallbackInfoReturnable<Boolean> cir) {
        if (access instanceof net.minecraft.world.entity.Entity e && FabricGameEvents.listens(EntityJoinLevelEvent.class)
                && FabricGameEvents.post(new EntityJoinLevelEvent(e, e.level(), worldGenSpawned)).isCanceled()) cir.setReturnValue(false);
    }
}
