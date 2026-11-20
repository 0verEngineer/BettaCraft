package org.overengineer.bettacraft.mixin.registry;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.overengineer.bettacraft.util.BlockTooltip;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * M52b/M52c: the tooltip of a block of ours ({@link BlockTooltip}; {@code Block#appendHoverText} is gone since 1.21.5).
 * An inject at the end of {@code Item#appendHoverText}, not a new {@code BlockItem#appendHoverText}: other mods add that
 * method too (sathlib in ATM11), and only one of the added methods would survive.
 */
@Mixin(Item.class)
public abstract class ItemTooltipMixin {
    @Inject(method = "appendHoverText", at = @At("TAIL"))
    @SuppressWarnings("deprecation")
    private void bettacraft$blockTooltip(ItemStack stack, Item.TooltipContext ctx, TooltipDisplay display, Consumer<Component> tooltip,
            TooltipFlag flag, CallbackInfo ci) {
        if ((Object) this instanceof BlockItem item && item.getBlock() instanceof BlockTooltip t) t.appendHoverText(stack, ctx, display, tooltip, flag);
    }
}
