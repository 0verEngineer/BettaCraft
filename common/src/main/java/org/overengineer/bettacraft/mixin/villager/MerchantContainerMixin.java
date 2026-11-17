package org.overengineer.bettacraft.mixin.villager;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.overengineer.bettacraft.villager.AquaristTrades;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * M47 part a: the buying offers of the aquarist pay by the genes of the fish in the trade slot. Their result is a marked
 * emerald ({@link AquaristTrades#isAppraisal}); it is replaced by the price of the bucket or bag that lies in the payment
 * slot (nothing when the aquarist does not take that fish), on the server and the same way on the client (the slot shows
 * the price at once; the server's result is the one the player gets).
 */
@Mixin(MerchantContainer.class)
public abstract class MerchantContainerMixin {
    @Shadow
    @Final
    private NonNullList<ItemStack> itemStacks;
    @Shadow
    private MerchantOffer activeOffer;

    @Shadow
    public abstract void setItem(int index, ItemStack stack);

    @Inject(method = "updateSellItem", at = @At("TAIL"))
    private void bettacraft$payByGenes(CallbackInfo ci) {
        MerchantOffer offer = activeOffer;
        if (offer == null || !AquaristTrades.isAppraisal(offer.getResult())) return;
        ItemStack a = itemStacks.get(0), b = itemStacks.get(1);
        ItemStack fish = offer.getItemCostA().test(a) ? a : b;
        ItemStack result = itemStacks.get(2);
        if (result.isEmpty() && !offer.getItemCostA().test(fish)) return;
        setItem(2, AquaristTrades.payFor(fish));
    }
}
