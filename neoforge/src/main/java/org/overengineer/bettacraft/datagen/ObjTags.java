package org.overengineer.bettacraft.datagen;

import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagKey;

/**
 * M52d: Minecraft 26.2 tag providers take resource keys only (the intrinsic holder providers with {@code add(Block)} are
 * gone). The datagen of the mod keeps adding objects: this appender turns them into their registry keys.
 */
public final class ObjTags {
    private ObjTags() {
    }

    public static <T> Appender<T> of(TagAppender<T> raw, Registry<T> registry) {
        return new Appender<>(raw, o -> registry.getResourceKey(o).orElseThrow(() -> new IllegalStateException("not registered: " + o)));
    }

    public static final class Appender<T> {
        private final TagAppender<T> raw;
        private final Function<T, ResourceKey<T>> key;

        Appender(TagAppender<T> raw, Function<T, ResourceKey<T>> key) {
            this.raw = raw;
            this.key = key;
        }

        @SafeVarargs
        public final Appender<T> add(T... values) {
            for (T v : values) raw.add(key.apply(v));
            return this;
        }

        public Appender<T> addAll(Iterable<? extends T> values) {
            for (T v : values) raw.add(key.apply(v));
            return this;
        }

        @SafeVarargs
        public final Appender<T> add(ResourceKey<T>... keys) {
            for (ResourceKey<T> k : keys) raw.add(k);
            return this;
        }

        public Appender<T> add(TagEntry entry) {
            raw.add(entry);
            return this;
        }

        public Appender<T> addOptional(ResourceKey<T> k) {
            raw.addOptional(k);
            return this;
        }

        public Appender<T> addTag(TagKey<T> tag) {
            raw.addTag(tag);
            return this;
        }

        public Appender<T> addOptionalTag(TagKey<T> tag) {
            raw.addOptionalTag(tag);
            return this;
        }

        @SafeVarargs
        public final Appender<T> addTags(TagKey<T>... tags) {
            for (TagKey<T> t : tags) raw.addTag(t);
            return this;
        }

        public Appender<T> replace(boolean value) {
            raw.replace(value);
            return this;
        }
    }
}
