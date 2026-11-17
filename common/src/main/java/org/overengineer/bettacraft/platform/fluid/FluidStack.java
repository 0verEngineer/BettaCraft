package org.overengineer.bettacraft.platform.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * M52: an amount of a fluid in millibuckets (1000 per bucket on every loader; Fabric's droplets are converted at the
 * platform border) with data components. Saved as {@code {id, amount, components}}, the format older worlds have.
 */
public final class FluidStack {
    public static final FluidStack EMPTY = new FluidStack(null);

    public static final Codec<Holder<Fluid>> FLUID_NON_EMPTY_CODEC = BuiltInRegistries.FLUID.holderByNameCodec()
            .validate(h -> h.is(Fluids.EMPTY.builtInRegistryHolder()) ? DataResult.error(() -> "Fluid must not be minecraft:empty") : DataResult.success(h));

    public static final Codec<FluidStack> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(i -> i.group(
            FLUID_NON_EMPTY_CODEC.fieldOf("id").forGetter(FluidStack::getFluidHolder),
            ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(FluidStack::getAmount),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(s -> s.components.asPatch())
    ).apply(i, FluidStack::new)));

    public static final Codec<FluidStack> OPTIONAL_CODEC = ExtraCodecs.optionalEmptyMap(CODEC)
            .xmap(o -> o.orElse(FluidStack.EMPTY), s -> s.isEmpty() ? Optional.empty() : Optional.of(s));

    private static final StreamCodec<RegistryFriendlyByteBuf, Holder<Fluid>> FLUID_STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.FLUID);

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> OPTIONAL_STREAM_CODEC = new StreamCodec<>() {
        @Override
        public FluidStack decode(RegistryFriendlyByteBuf buf) {
            int amount = buf.readVarInt();
            if (amount <= 0) return FluidStack.EMPTY;
            Holder<Fluid> fluid = FLUID_STREAM_CODEC.decode(buf);
            DataComponentPatch patch = DataComponentPatch.STREAM_CODEC.decode(buf);
            return new FluidStack(fluid, amount, patch);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, FluidStack stack) {
            if (stack.isEmpty()) {
                buf.writeVarInt(0);
            } else {
                buf.writeVarInt(stack.getAmount());
                FLUID_STREAM_CODEC.encode(buf, stack.getFluidHolder());
                DataComponentPatch.STREAM_CODEC.encode(buf, stack.components.asPatch());
            }
        }
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public FluidStack decode(RegistryFriendlyByteBuf buf) {
            FluidStack stack = OPTIONAL_STREAM_CODEC.decode(buf);
            if (stack.isEmpty()) throw new DecoderException("Empty FluidStack not allowed");
            return stack;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, FluidStack stack) {
            if (stack.isEmpty()) throw new EncoderException("Empty FluidStack not allowed");
            OPTIONAL_STREAM_CODEC.encode(buf, stack);
        }
    };

    private final @Nullable Fluid fluid;
    private int amount;
    private final PatchedDataComponentMap components;

    private FluidStack(@Nullable Void empty) {
        this.fluid = null;
        this.components = new PatchedDataComponentMap(DataComponentMap.EMPTY);
    }

    public FluidStack(Holder<Fluid> fluid, int amount, DataComponentPatch patch) {
        this(fluid.value(), amount, PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, patch));
    }

    public FluidStack(Holder<Fluid> fluid, int amount) {
        this(fluid.value(), amount, new PatchedDataComponentMap(DataComponentMap.EMPTY));
    }

    public FluidStack(Fluid fluid, int amount) {
        this(fluid, amount, new PatchedDataComponentMap(DataComponentMap.EMPTY));
    }

    private FluidStack(Fluid fluid, int amount, PatchedDataComponentMap components) {
        this.fluid = fluid;
        this.amount = amount;
        this.components = components;
    }

    public static Optional<FluidStack> parse(HolderLookup.Provider provider, Tag tag) {
        return CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), tag).result();
    }

    public static FluidStack parseOptional(HolderLookup.Provider provider, CompoundTag tag) {
        return tag.isEmpty() ? EMPTY : parse(provider, tag).orElse(EMPTY);
    }

    public Tag save(HolderLookup.Provider provider, Tag prefix) {
        if (isEmpty()) throw new IllegalStateException("Cannot encode empty FluidStack");
        return CODEC.encode(this, provider.createSerializationContext(NbtOps.INSTANCE), prefix).getOrThrow();
    }

    public Tag save(HolderLookup.Provider provider) {
        if (isEmpty()) throw new IllegalStateException("Cannot encode empty FluidStack");
        return CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), this).getOrThrow();
    }

    public Tag saveOptional(HolderLookup.Provider provider) {
        return isEmpty() ? new CompoundTag() : save(provider, new CompoundTag());
    }

    public boolean isEmpty() {
        return this == EMPTY || fluid == null || fluid == Fluids.EMPTY || amount <= 0;
    }

    public Fluid getFluid() {
        return isEmpty() ? Fluids.EMPTY : fluid;
    }

    public Holder<Fluid> getFluidHolder() {
        return getFluid().builtInRegistryHolder();
    }

    public int getAmount() {
        return isEmpty() ? 0 : amount;
    }

    public void setAmount(int amount) {
        if (this == EMPTY) throw new IllegalStateException("Can't modify the empty stack.");
        this.amount = amount;
    }

    public void limitSize(int max) {
        if (!isEmpty() && getAmount() > max) setAmount(max);
    }

    public void grow(int added) {
        setAmount(amount + added);
    }

    public void shrink(int removed) {
        grow(-removed);
    }

    public FluidStack split(int amount) {
        int n = Math.min(amount, this.amount);
        FluidStack out = copyWithAmount(n);
        shrink(n);
        return out;
    }

    public FluidStack copyAndClear() {
        if (isEmpty()) return EMPTY;
        FluidStack out = copy();
        setAmount(0);
        return out;
    }

    public FluidStack copy() {
        return isEmpty() ? EMPTY : new FluidStack(fluid, amount, components.copy());
    }

    public FluidStack copyWithAmount(int amount) {
        if (isEmpty()) return EMPTY;
        FluidStack out = copy();
        out.setAmount(amount);
        return out;
    }

    public boolean is(TagKey<Fluid> tag) {
        return getFluidHolder().is(tag);
    }

    public boolean is(Fluid fluid) {
        return getFluid() == fluid;
    }

    public boolean is(Predicate<Holder<Fluid>> predicate) {
        return predicate.test(getFluidHolder());
    }

    public boolean is(Holder<Fluid> holder) {
        return is(holder.value());
    }

    public boolean is(HolderSet<Fluid> set) {
        return set.contains(getFluidHolder());
    }

    public Stream<TagKey<Fluid>> getTags() {
        return getFluidHolder().tags();
    }

    public PatchedDataComponentMap getComponents() {
        return components;
    }

    public DataComponentPatch getComponentsPatch() {
        return isEmpty() ? DataComponentPatch.EMPTY : components.asPatch();
    }

    public boolean isComponentsPatchEmpty() {
        return isEmpty() || components.asPatch().isEmpty();
    }

    public <T> @Nullable T get(DataComponentType<? extends T> type) {
        return components.get(type);
    }

    public <T> T getOrDefault(DataComponentType<? extends T> type, T fallback) {
        return components.getOrDefault(type, fallback);
    }

    public boolean has(DataComponentType<?> type) {
        return components.has(type);
    }

    public <T> @Nullable T set(DataComponentType<T> type, @Nullable T value) {
        return components.set(type, value);
    }

    public <T> @Nullable T remove(DataComponentType<? extends T> type) {
        return components.remove(type);
    }

    public void applyComponents(DataComponentPatch patch) {
        components.applyPatch(patch);
    }

    public static boolean matches(FluidStack a, FluidStack b) {
        if (a == b) return true;
        return a.getAmount() == b.getAmount() && isSameFluidSameComponents(a, b);
    }

    public static boolean isSameFluid(FluidStack a, FluidStack b) {
        return a.getFluid() == b.getFluid();
    }

    public static boolean isSameFluidSameComponents(FluidStack a, FluidStack b) {
        if (!isSameFluid(a, b)) return false;
        return a.isEmpty() && b.isEmpty() || java.util.Objects.equals(a.components, b.components);
    }

    public static int hashFluidAndComponents(@Nullable FluidStack stack) {
        if (stack == null) return 0;
        int i = 31 + stack.getFluid().hashCode();
        return 31 * i + stack.getComponents().hashCode();
    }

    /** The translation key of the fluid (its block's for the vanilla fluids, like NeoForge's fluid types). */
    public String getDescriptionId() {
        return org.overengineer.bettacraft.platform.Platform.get().fluidDescriptionId(getFluid());
    }

    public Component getHoverName() {
        return Component.translatable(getDescriptionId());
    }

    @Override
    public String toString() {
        return getAmount() + " " + BuiltInRegistries.FLUID.getKey(getFluid());
    }
}
