package org.overengineer.bettacraft.client.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.BettaCraft;

/**
 * M52b: the item properties of 1.21.1 ({@code ItemProperties}: a number per item and property, the model overrides chose
 * a model at a threshold) on the 1.21.4 item models: one conditional property {@code bettacraft:property} with the
 * property id and the threshold of the old override; true when the property's value reaches it.
 */
public final class ItemPredicates {
    /** The value of a property of a stack (like the 1.21.1 item property function). */
    @FunctionalInterface
    public interface Function {
        float get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed);
    }

    public static final Identifier TYPE = BettaCraft.id("property");
    private static final Map<Item, Map<Identifier, Function>> FUNCTIONS = new HashMap<>();

    private ItemPredicates() {}

    public static void register(Item item, Identifier property, Function function) {
        FUNCTIONS.computeIfAbsent(item, k -> new HashMap<>()).put(property, function);
    }

    public static float value(ItemStack stack, Identifier property, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        var m = FUNCTIONS.get(stack.getItem());
        Function f = m == null ? null : m.get(property);
        return f == null ? 0F : f.get(stack, level, entity, seed);
    }

    /** {@code {"property": "bettacraft:property", "name": "bettacraft:filled", "threshold": 1.0}}. */
    public record Property(Identifier name, float threshold) implements ConditionalItemModelProperty {
        public static final MapCodec<Property> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.fieldOf("name").forGetter(Property::name),
                Codec.FLOAT.optionalFieldOf("threshold", 1F).forGetter(Property::threshold)).apply(i, Property::new));

        @Override
        public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed, ItemDisplayContext context) {
            return value(stack, name, level, entity, seed) >= threshold;
        }

        @Override
        public MapCodec<Property> type() {
            return MAP_CODEC;
        }
    }
}
