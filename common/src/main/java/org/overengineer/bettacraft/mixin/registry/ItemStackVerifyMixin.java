package org.overengineer.bettacraft.mixin.registry;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.food.ArtemiaCystsItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * M52b: Minecraft 1.21.2 dropped {@code Item#verifyComponentsAfterLoad}, which cleaned the components of a stack whenever it
 * was made from saved or sent data or got components applied. The jars of artemia cysts used it to drop the spent uses of
 * old jars (M35.1); this puts the same hook back at the same places (the constructor all stacks go through, and the three
 * component appliers).
 */
@Mixin(ItemStack.class)
public abstract class ItemStackVerifyMixin {
    @Inject(method = "<init>(Lnet/minecraft/world/level/ItemLike;ILnet/minecraft/core/component/PatchedDataComponentMap;)V", at = @At("TAIL"))
    private void bettacraft$verifyAfterCreate(CallbackInfo ci) {
        ArtemiaCystsItem.stripOldUses((ItemStack) (Object) this);
    }

    @Inject(method = "applyComponentsAndValidate", at = @At("TAIL"))
    private void bettacraft$verifyAfterValidate(DataComponentPatch patch, CallbackInfo ci) {
        ArtemiaCystsItem.stripOldUses((ItemStack) (Object) this);
    }

    @Inject(method = "applyComponents(Lnet/minecraft/core/component/DataComponentPatch;)V", at = @At("TAIL"))
    private void bettacraft$verifyAfterPatch(DataComponentPatch patch, CallbackInfo ci) {
        ArtemiaCystsItem.stripOldUses((ItemStack) (Object) this);
    }

    @Inject(method = "applyComponents(Lnet/minecraft/core/component/DataComponentMap;)V", at = @At("TAIL"))
    private void bettacraft$verifyAfterMap(DataComponentMap map, CallbackInfo ci) {
        ArtemiaCystsItem.stripOldUses((ItemStack) (Object) this);
    }
}
