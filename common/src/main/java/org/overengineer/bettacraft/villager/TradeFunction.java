package org.overengineer.bettacraft.villager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

/**
 * M52c (26.1): item modifier {@code bettacraft:trade} ({@code {"type": "bettacraft:trade", "offer":
 * "wandering_trader/dwarf_crayfish"}}) of our {@code villager_trade} entries: marks the entry as one of our offers made in
 * code ({@link BettaTrades#listing}). {@code VillagerTradeOfferMixin} makes the whole offer from the listing (price, uses,
 * experience); applied on its own (another mod reading the trade) it gives the goods of such an offer.
 */
public record TradeFunction(String offer) implements LootItemFunction {
    public static final MapCodec<TradeFunction> CODEC = Codec.STRING.fieldOf("offer").xmap(TradeFunction::new, TradeFunction::offer);

    @Override
    public MapCodec<TradeFunction> codec() {
        return CODEC;
    }

    /** The offer of the listing for the trader of the loot context, or null (no trader, unknown id, no offer). */
    public @Nullable MerchantOffer offer(LootContext context) {
        TradeListing listing = BettaTrades.listing(offer);
        Entity trader = context.getOptional(LootContextParams.THIS_ENTITY);
        if (listing == null || trader == null || !(context.getLevel() instanceof ServerLevel level)) return null;
        return listing.getOffer(level, trader, context.getRandom());
    }

    @Override
    public ItemStack apply(ItemStack stack, LootContext context) {
        MerchantOffer o = offer(context);
        return o == null ? ItemStack.EMPTY : o.getResult().copy();
    }
}
