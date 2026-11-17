package org.overengineer.bettacraft.platform.registry;

import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * M52b: since Minecraft 1.21.2 an item's and a block's properties must carry the registry id before the object is made
 * ({@code Item.Properties#setId}, {@code BlockBehaviour.Properties#setId}). Our registrations build their properties
 * inside the factories (as in 1.21.1); {@link DeferredRegister} runs every factory inside {@link #with}, and the mixins
 * {@code ItemPropertiesMixin} / {@code BlockPropertiesMixin} take the id from here when the properties have none. Block
 * items made this way keep the block's name ({@code block.bettacraft.x}), as {@code BlockItem} did in 1.21.1
 * ({@code BlockItemMixin}).
 */
public final class RegistrationIds {
    private static final ThreadLocal<ResourceKey<?>> CURRENT = new ThreadLocal<>();

    private RegistrationIds() {}

    public static <T> T with(ResourceKey<? extends Registry<?>> registry, Identifier id, Supplier<T> factory) {
        ResourceKey<?> before = CURRENT.get();
        @SuppressWarnings({"unchecked", "rawtypes"})
        ResourceKey<?> key = ResourceKey.create((ResourceKey) registry, id);
        CURRENT.set(key);
        try {
            return factory.get();
        } finally {
            if (before == null) CURRENT.remove();
            else CURRENT.set(before);
        }
    }

    /** The item being registered by us right now, or null. */
    @Nullable
    @SuppressWarnings("unchecked")
    public static ResourceKey<Item> item() {
        ResourceKey<?> k = CURRENT.get();
        return k != null && k.isFor(Registries.ITEM) ? (ResourceKey<Item>) k : null;
    }

    /** The block being registered by us right now, or null. */
    @Nullable
    @SuppressWarnings("unchecked")
    public static ResourceKey<Block> block() {
        ResourceKey<?> k = CURRENT.get();
        return k != null && k.isFor(Registries.BLOCK) ? (ResourceKey<Block>) k : null;
    }
}
