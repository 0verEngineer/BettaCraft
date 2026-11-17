package org.overengineer.bettacraft.platform.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.platform.Platform;

/**
 * M52: an ingredient with a count (food maker, crucible). Saved flat like NeoForge's sized ingredient:
 * {@code {"item": "...", "count": 4}} / {@code {"tag": "...", "count": 4}} (the ingredient's own fields and the count); the
 * ingredient map codec comes from the loader (NeoForge: with its custom ingredient types).
 */
public final class SizedIngredient {
    public static final Codec<SizedIngredient> FLAT_CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(i -> i.group(
            Platform.get().ingredientMapCodec().forGetter(SizedIngredient::ingredient),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("count").xmap(o -> o.orElse(1), Optional::of).forGetter(SizedIngredient::count)
    ).apply(i, SizedIngredient::new)));

    public static final StreamCodec<RegistryFriendlyByteBuf, SizedIngredient> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, SizedIngredient::ingredient,
            ByteBufCodecs.VAR_INT, SizedIngredient::count,
            SizedIngredient::new);

    private final Ingredient ingredient;
    private final int count;
    private @Nullable ItemStack[] cachedStacks;

    public SizedIngredient(Ingredient ingredient, int count) {
        if (count <= 0) throw new IllegalArgumentException("Size must be positive");
        this.ingredient = ingredient;
        this.count = count;
    }

    public static SizedIngredient of(ItemLike item, int count) {
        return new SizedIngredient(Ingredient.of(item), count);
    }

    public static SizedIngredient of(TagKey<Item> tag, int count) {
        return new SizedIngredient(Ingredient.of(tag), count);
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    public int count() {
        return count;
    }

    public boolean test(ItemStack stack) {
        return ingredient.test(stack) && stack.getCount() >= count;
    }

    public ItemStack[] getItems() {
        if (cachedStacks == null) cachedStacks = Stream.of(ingredient.getItems()).map(s -> s.copyWithCount(count)).toArray(ItemStack[]::new);
        return cachedStacks;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof SizedIngredient other && count == other.count && ingredient.equals(other.ingredient);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ingredient, count);
    }

    @Override
    public String toString() {
        return count + "x " + ingredient;
    }
}
