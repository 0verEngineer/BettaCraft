package org.overengineer.bettacraft.mixin.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import org.overengineer.bettacraft.platform.registry.BlockPrefixProperties;
import org.overengineer.bettacraft.platform.registry.RegistrationIds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * M52b: a block item we register is named like its block ({@code block.bettacraft.x}), as in 1.21.1 where
 * {@code BlockItem#getDescriptionId} was the block's. Only while one of our items is being registered.
 */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin extends Item {
    private BlockItemMixin(Item.Properties properties) {
        super(properties);
    }

    // M52c: the block tooltip is in ItemTooltipMixin (an inject; an added method here clashed with other mods)

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static Item.Properties bettacraft$blockName(Item.Properties properties) {
        if (RegistrationIds.item() != null) ((BlockPrefixProperties) properties).bettacraft$blockPrefixIfDefault();
        return properties;
    }
}
