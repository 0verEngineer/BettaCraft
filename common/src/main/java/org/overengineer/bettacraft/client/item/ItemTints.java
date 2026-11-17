package org.overengineer.bettacraft.client.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.BettaCraft;

/**
 * M52b: the item colours of 1.21.1 ({@code ItemColor}: a colour per item and tint index) on the 1.21.4 item models. The
 * colour functions stay in {@code ClientSetup#itemColors}; every tinted item's {@code items/<id>.json} names one tint
 * source {@code bettacraft:tint} per tint index, which asks the function of its item here.
 */
public final class ItemTints {
    /** The colour of a tint index of a stack (ARGB; like the 1.21.1 item colour). */
    @FunctionalInterface
    public interface Tint {
        int color(ItemStack stack, int tintIndex);
    }

    public static final net.minecraft.resources.Identifier TYPE = BettaCraft.id("tint");
    private static final Map<Item, Tint> TINTS = new IdentityHashMap<>();

    private ItemTints() {}

    public static void register(Tint tint, net.minecraft.world.level.ItemLike... items) {
        for (var i : items) TINTS.put(i.asItem(), tint);
    }

    public static int color(ItemStack stack, int index) {
        Tint t = TINTS.get(stack.getItem());
        if (t == null) return -1;
        int c = t.color(stack, index);
        // 1.21.1 drew the item colour opaque when the function gave an RGB value without alpha
        return (c & 0xFF000000) == 0 ? c | 0xFF000000 : c;
    }

    /** The tint source of one tint index ({@code {"type": "bettacraft:tint", "index": n}}). */
    public record Source(int index) implements ItemTintSource {
        public static final MapCodec<Source> MAP_CODEC = Codec.INT.optionalFieldOf("index", 0).xmap(Source::new, Source::index);

        @Override
        public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity) {
            return color(stack, index);
        }

        @Override
        public MapCodec<Source> type() {
            return MAP_CODEC;
        }
    }
}
