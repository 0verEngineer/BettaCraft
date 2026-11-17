package org.overengineer.bettacraft.mixin.registry;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.overengineer.bettacraft.platform.registry.RegistrationIds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** M52b: the id of a block we register, when its properties were made without one (see {@link RegistrationIds}). */
@Mixin(BlockBehaviour.Properties.class)
public abstract class BlockPropertiesMixin {
    @Shadow private ResourceKey<Block> id;

    @Inject(method = {"effectiveDrops", "effectiveDescriptionId"}, at = @At("HEAD"))
    private void bettacraft$idOfOurRegistration(CallbackInfoReturnable<?> cir) {
        if (id == null) id = RegistrationIds.block();
    }
}
