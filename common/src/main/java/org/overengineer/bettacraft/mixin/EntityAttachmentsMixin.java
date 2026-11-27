package org.overengineer.bettacraft.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.overengineer.bettacraft.platform.CrossLoaderAttachments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Our entity attachments saved by the other loader are read too ({@link CrossLoaderAttachments}). */
@Mixin(Entity.class)
public abstract class EntityAttachmentsMixin {
    @Inject(method = "load", at = @At("HEAD"))
    private void bettacraft$crossLoaderAttachments(CompoundTag tag, CallbackInfo ci) {
        CrossLoaderAttachments.merge(tag);
    }
}
