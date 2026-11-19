package org.overengineer.bettacraft.fabric.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.frog.Tadpole;
import org.overengineer.bettacraft.fabric.FabricGameEvents;
import org.overengineer.bettacraft.platform.event.LivingConversionEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** M52: NeoForge's LivingConversionEvent.Pre of a tadpole growing into a frog (a tank keeps its tadpoles, M31). */
@Mixin(Tadpole.class)
public abstract class TadpoleAgeMixin {
    @Inject(method = "ageUp()V", at = @At("HEAD"), cancellable = true)
    private void bettacraft$convertPre(CallbackInfo ci) {
        Tadpole self = (Tadpole) (Object) this;
        if (self.level().isClientSide()) return;
        if (FabricGameEvents.post(new LivingConversionEvent.Pre(self, EntityTypes.FROG)).isCanceled()) ci.cancel();
    }
}
