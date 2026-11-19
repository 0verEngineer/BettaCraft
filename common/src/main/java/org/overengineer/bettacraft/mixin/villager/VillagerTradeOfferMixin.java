package org.overengineer.bettacraft.mixin.villager;

import java.util.List;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import org.overengineer.bettacraft.villager.TradeFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * M52c (26.1): a {@code villager_trade} entry of ours (item modifier {@link TradeFunction}) gets its whole offer from our
 * code (price, goods, uses, experience, as the 1.21.x item listings made it); null skips the entry like a failed
 * merchant predicate.
 */
@Mixin(VillagerTrade.class)
public abstract class VillagerTradeOfferMixin implements org.overengineer.bettacraft.villager.TradeEntry {
    @Shadow
    @Final
    private List<LootItemFunction> givenItemModifiers;

    @Override
    public @org.jetbrains.annotations.Nullable String bettacraft$offerId() {
        for (LootItemFunction f : givenItemModifiers) if (f instanceof TradeFunction t) return t.offer();
        return null;
    }

    @Inject(method = "getOffer", at = @At("HEAD"), cancellable = true)
    private void bettacraft$offer(LootContext context, CallbackInfoReturnable<MerchantOffer> cir) {
        for (LootItemFunction f : givenItemModifiers) {
            if (f instanceof TradeFunction t) {
                cir.setReturnValue(t.offer(context));
                return;
            }
        }
    }
}
