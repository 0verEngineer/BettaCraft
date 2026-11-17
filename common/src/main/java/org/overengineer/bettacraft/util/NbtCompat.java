package org.overengineer.bettacraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.overengineer.bettacraft.platform.Platform;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * M52b: the NBT helpers of 1.21.1 that Minecraft 1.21.5 removed or changed (typed {@code contains}, UUIDs, block
 * positions) with their old behaviour, and the bridge between our {@link CompoundTag} based save data and the
 * {@link ValueInput} / {@link ValueOutput} of 1.21.6 (block entities, entities). The saved keys stay exactly the same: a
 * tank saved by our code still has its keys at the top level of the block entity tag.
 */
public final class NbtCompat {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private NbtCompat() {}

    /** {@code Tag.TAG_ANY_NUMERIC} of 1.21.1 (removed in 1.21.5) for {@link #has}. */
    public static final int ANY_NUMERIC = 99;

    /** The typed {@code CompoundTag#contains(String, int)} of 1.21.1: the key holds a tag of that type (99 = any number). */
    public static boolean has(CompoundTag tag, String key, int type) {
        Tag t = tag.get(key);
        if (t == null) return false;
        if (type == 99) return t instanceof NumericTag;
        return t.getId() == type;
    }

    /** {@code CompoundTag#hasUUID} of 1.21.1 (a UUID is an int array of four). */
    public static boolean hasUUID(CompoundTag tag, String key) {
        return tag.read(key, UUIDUtil.CODEC).isPresent();
    }

    /** {@code CompoundTag#getUUID} of 1.21.1; throws like it did when the key holds no UUID. */
    public static UUID getUUID(CompoundTag tag, String key) {
        return tag.read(key, UUIDUtil.CODEC).orElseThrow(() -> new IllegalArgumentException("no UUID at " + key));
    }

    public static void putUUID(CompoundTag tag, String key, UUID id) {
        tag.store(key, UUIDUtil.CODEC, id);
    }

    /** {@code NbtUtils#createUUID} of 1.21.1. */
    public static IntArrayTag createUUID(UUID id) {
        return new IntArrayTag(UUIDUtil.uuidToIntArray(id));
    }

    /** {@code NbtUtils#loadUUID} of 1.21.1. */
    public static UUID loadUUID(Tag tag) {
        if (!(tag instanceof IntArrayTag a) || a.getAsIntArray().length != 4) throw new IllegalArgumentException("not a UUID: " + tag);
        return UUIDUtil.uuidFromIntArray(a.getAsIntArray());
    }

    /** {@code NbtUtils#readBlockPos} of 1.21.1 (the int array form). */
    public static Optional<BlockPos> readBlockPos(CompoundTag tag, String key) {
        return tag.read(key, BlockPos.CODEC);
    }

    /** {@code NbtUtils#writeBlockPos} of 1.21.1 (an int array of three). */
    public static Tag writeBlockPos(BlockPos pos) {
        return new IntArrayTag(new int[] {pos.getX(), pos.getY(), pos.getZ()});
    }

    /** The whole compound of a map: every key of a value input or output is one key of the tag. */
    public static final MapCodec<CompoundTag> WHOLE = new MapCodec<>() {
        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.empty();
        }

        @Override
        public <T> DataResult<CompoundTag> decode(DynamicOps<T> ops, MapLike<T> input) {
            CompoundTag out = new CompoundTag();
            input.entries().forEach(e -> {
                Optional<String> key = ops.getStringValue(e.getFirst()).result();
                if (key.isPresent()) out.put(key.get(), ops.convertTo(NbtOps.INSTANCE, e.getSecond()));
            });
            return DataResult.success(out);
        }

        @Override
        public <T> RecordBuilder<T> encode(CompoundTag input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            for (String key : input.keySet()) {
                prefix.add(key, NbtOps.INSTANCE.convertTo(ops, input.get(key)));
            }
            return prefix;
        }
    };

    /** {@code ItemStack#save(Provider)} of 1.21.1 (throws on an empty stack like it did). */
    public static Tag saveStack(HolderLookup.Provider registries, ItemStack stack) {
        if (stack.isEmpty()) throw new IllegalStateException("Cannot encode empty ItemStack");
        return ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
    }

    /** {@code ItemStack#save(Provider, Tag)} of 1.21.1: the stack's keys merged into {@code prefix}. */
    public static Tag saveStack(HolderLookup.Provider registries, ItemStack stack, Tag prefix) {
        if (stack.isEmpty()) throw new IllegalStateException("Cannot encode empty ItemStack");
        return ItemStack.CODEC.encode(stack, registries.createSerializationContext(NbtOps.INSTANCE), prefix).getOrThrow();
    }

    /** {@code ItemStack#saveOptional} of 1.21.1: an empty compound for an empty stack. */
    public static Tag saveOptionalStack(HolderLookup.Provider registries, ItemStack stack) {
        return stack.isEmpty() ? new CompoundTag() : saveStack(registries, stack);
    }

    /** {@code ItemStack#parse} of 1.21.1. */
    public static Optional<ItemStack> parseStack(HolderLookup.Provider registries, Tag tag) {
        return ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
                .resultOrPartial(e -> LOGGER.error("Tried to load invalid item: '{}'", e));
    }

    /** {@code ItemStack#parseOptional} of 1.21.1: an empty compound is an empty stack. */
    public static ItemStack parseOptionalStack(HolderLookup.Provider registries, CompoundTag tag) {
        return tag.isEmpty() ? ItemStack.EMPTY : parseStack(registries, tag).orElse(ItemStack.EMPTY);
    }

    /** {@code Tag#getAsString} of 1.21.1: the value of a string tag, otherwise the tag as text. */
    public static String asString(Tag tag) {
        return tag instanceof StringTag s ? s.value() : tag.toString();
    }

    /** The registries for the save data of a block entity (its level's, else the running server's). */
    public static HolderLookup.Provider registries(BlockEntity be) {
        if (be.getLevel() != null) return be.getLevel().registryAccess();
        return fallback();
    }

    /** The registries for the save data of an entity. */
    public static HolderLookup.Provider registries(Entity e) {
        return e.registryAccess();
    }

    private static HolderLookup.Provider fallback() {
        MinecraftServer server = Platform.available() ? Platform.get().currentServer() : null;
        if (server != null) return server.registryAccess();
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    /** How a {@link SavedData} of ours writes itself ({@code save(CompoundTag, Provider)} of 1.21.1). */
    @FunctionalInterface
    public interface SaveFn<T> {
        CompoundTag save(T data, CompoundTag tag, HolderLookup.Provider registries);
    }

    /**
     * The {@link SavedDataType} of 1.21.5 for a saved data of ours that keeps its 1.21.1 {@code load} / {@code save}
     * methods: the same file ({@code id}) and the same "data" compound. Saved data of mods has no data fixer of its own;
     * the command storage fixer leaves our keys alone.
     */
    public static <T extends SavedData> SavedDataType<T> savedData(String id, Supplier<T> constructor,
            BiFunction<CompoundTag, HolderLookup.Provider, T> load, SaveFn<T> save) {
        Codec<T> codec = CompoundTag.CODEC.xmap(t -> load.apply(t, fallback()), d -> save.save(d, new CompoundTag(), fallback()));
        return new SavedDataType<>(id, constructor, codec, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    }

    /** A value input reading {@code tag} (for {@code BlockEntity#loadWithComponents} and friends). */
    public static ValueInput input(HolderLookup.Provider registries, CompoundTag tag) {
        return TagValueInput.create(ProblemReporter.DISCARDING, registries, tag);
    }

    /** Saves an entity without its id into a compound ({@code Entity#saveWithoutId(CompoundTag)} of 1.21.1). */
    public static CompoundTag saveEntityWithoutId(Entity e) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, e.registryAccess());
        e.saveWithoutId(out);
        return out.buildResult();
    }

    /** Saves an entity with its id ({@code Entity#save(CompoundTag)} of 1.21.1); false if it may not be saved. */
    public static boolean saveEntity(Entity e, CompoundTag into) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, e.registryAccess());
        if (!e.save(out)) return false;
        into.merge(out.buildResult());
        return true;
    }

    /** Loads an entity's saved data into it ({@code Entity#load(CompoundTag)} of 1.21.1). */
    public static void loadEntity(Entity e, CompoundTag tag) {
        e.load(input(e.registryAccess(), tag));
    }

    /**
     * M52b: synced entity data of a compound. {@code EntityDataSerializers.COMPOUND_TAG} is gone since 1.21.5; a custom
     * serializer would need a registry of its own on each loader, so the look of a fish is synced as SNBT text.
     */
    public static String syncTag(CompoundTag tag) {
        return tag.toString();
    }

    /** The compound of {@link #syncTag} (empty for empty or broken text). */
    public static CompoundTag unsyncTag(String snbt) {
        if (snbt == null || snbt.isEmpty()) return new CompoundTag();
        try {
            return net.minecraft.nbt.TagParser.parseCompoundFully(snbt);
        } catch (Exception e) {
            return new CompoundTag();
        }
    }

    /** Writes every key of {@code tag} into {@code out} (our save data into a block entity's or entity's output). */
    public static void write(ValueOutput out, CompoundTag tag) {
        if (!tag.isEmpty()) out.store(WHOLE, tag);
    }

    /** Every key of {@code in} as one compound (what {@code loadAdditional(CompoundTag, ...)} got in 1.21.1). */
    public static CompoundTag read(ValueInput in) {
        return in.read(WHOLE).orElseGet(CompoundTag::new);
    }

    /** A tag of any kind under a key (raw NBT inside a value output). */
    public static final Codec<Tag> TAG = Codec.PASSTHROUGH.xmap(d -> d.convert(NbtOps.INSTANCE).getValue(),
            t -> new Dynamic<>(NbtOps.INSTANCE, t));
}
