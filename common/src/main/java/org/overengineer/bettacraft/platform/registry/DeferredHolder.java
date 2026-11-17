package org.overengineer.bettacraft.platform.registry;

import com.mojang.datafixers.util.Either;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

/**
 * M52: a registry object registered through {@link DeferredRegister}, bound lazily to the registry entry once the loader
 * has registered it (NeoForge: in its RegisterEvent; Fabric: at mod initialisation in the order of the vanilla registries).
 * The loader-independent counterpart of NeoForge's class of the same name: same methods, so the common code reads the same.
 */
public class DeferredHolder<R, T extends R> implements Holder<R>, Supplier<T> {
    protected final ResourceKey<R> key;
    private Holder<R> holder;

    protected DeferredHolder(ResourceKey<R> key) {
        this.key = Objects.requireNonNull(key);
    }

    public static <R, T extends R> DeferredHolder<R, T> create(ResourceKey<? extends Registry<R>> registryKey, ResourceLocation id) {
        return new DeferredHolder<>(ResourceKey.create(registryKey, id));
    }

    public static <R, T extends R> DeferredHolder<R, T> create(ResourceKey<R> key) {
        return new DeferredHolder<>(key);
    }

    @SuppressWarnings("unchecked")
    private Registry<R> registry() {
        return (Registry<R>) BuiltInRegistries.REGISTRY.get(key.registry());
    }

    private Holder<R> bind(boolean required) {
        if (holder == null) {
            Registry<R> reg = registry();
            if (reg != null) holder = reg.getHolder(key).orElse(null);
            if (holder == null && required) throw new IllegalStateException("Registry object not present: " + key);
        }
        return holder;
    }

    @Override
    public R value() {
        return bind(true).value();
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get() {
        return (T) value();
    }

    public Optional<T> asOptional() {
        return isBound() ? Optional.of(get()) : Optional.empty();
    }

    public ResourceLocation getId() {
        return key.location();
    }

    public ResourceKey<R> getKey() {
        return key;
    }

    /** The registry entry itself (a vanilla holder), e.g. for codecs that compare holders by identity. */
    public Holder<R> getDelegate() {
        return bind(true);
    }

    @Override
    public boolean isBound() {
        Holder<R> h = bind(false);
        return h != null && h.isBound();
    }

    @Override
    public boolean is(ResourceLocation id) {
        return key.location().equals(id);
    }

    @Override
    public boolean is(ResourceKey<R> other) {
        return key.equals(other);
    }

    @Override
    public boolean is(Predicate<ResourceKey<R>> predicate) {
        return predicate.test(key);
    }

    @Override
    public boolean is(TagKey<R> tag) {
        return bind(true).is(tag);
    }

    @Override
    @Deprecated
    public boolean is(Holder<R> other) {
        return bind(true).is(other);
    }

    @Override
    public Stream<TagKey<R>> tags() {
        return bind(true).tags();
    }

    @Override
    public Either<ResourceKey<R>, R> unwrap() {
        return Either.left(key);
    }

    @Override
    public Optional<ResourceKey<R>> unwrapKey() {
        return Optional.of(key);
    }

    @Override
    public Kind kind() {
        return Kind.REFERENCE;
    }

    @Override
    public boolean canSerializeIn(HolderOwner<R> owner) {
        return bind(true).canSerializeIn(owner);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Holder<?> h && h.kind() == Kind.REFERENCE && h.unwrapKey().map(key::equals).orElse(false);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "DeferredHolder{" + key + "}";
    }
}
