package org.overengineer.bettacraft.mixin.registry;

import net.minecraft.resources.DependantName;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import org.overengineer.bettacraft.platform.registry.BlockPrefixProperties;
import org.overengineer.bettacraft.platform.registry.RegistrationIds;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** M52b: the id of an item we register, when its properties were made without one (see {@link RegistrationIds}). */
@Mixin(Item.Properties.class)
public abstract class ItemPropertiesMixin implements BlockPrefixProperties {
    @Shadow private ResourceKey<Item> id;
    @Shadow private DependantName<Item, String> descriptionId;
    @Shadow @Final private static DependantName<Item, String> ITEM_DESCRIPTION_ID;

    @Inject(method = {"effectiveDescriptionId", "effectiveModel"}, at = @At("HEAD"))
    private void bettacraft$idOfOurRegistration(CallbackInfoReturnable<?> cir) {
        if (id == null) id = RegistrationIds.item();
    }

    @Unique
    @Override
    public void bettacraft$blockPrefixIfDefault() {
        if (descriptionId == ITEM_DESCRIPTION_ID) ((Item.Properties) (Object) this).useBlockDescriptionPrefix();
    }
}
