package org.overengineer.bettacraft.network;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.overengineer.bettacraft.BettaCraft;

/**
 * M52b: since Minecraft 1.21.2 the server keeps the recipes; the clients only get what the recipe book shows. Our devices
 * show their recipes in their windows, in JEI / REI / EMI and in the handbook, so the server sends them (food maker,
 * crucible) and the stone / slab pairs of the stone cabinet (found in the crafting and stonecutter recipes) at login and
 * after every reload.
 */
public record RecipeSyncPayload(List<RecipeHolder<?>> recipes, List<Item> stones, List<Item> slabs) implements CustomPacketPayload {
    public static final Type<RecipeSyncPayload> TYPE = new Type<>(BettaCraft.id("recipes"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeSyncPayload> STREAM_CODEC = StreamCodec.composite(
            RecipeHolder.STREAM_CODEC.apply(ByteBufCodecs.list()), RecipeSyncPayload::recipes,
            ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs.list()), RecipeSyncPayload::stones,
            ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs.list()), RecipeSyncPayload::slabs, RecipeSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
