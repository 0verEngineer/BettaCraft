package org.overengineer.bettacraft.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * M52: data we attach to entities (NeoForge: a data attachment type; Fabric: an attachment of its attachment API), with
 * an optional serializer (absent: not saved) and copy on death. Created in {@link org.overengineer.bettacraft.registry.ModAttachments};
 * the loader registers every one under its id before the game uses them.
 */
public final class Attachment<T> {
    /** Reads the saved tag (already migrated by the caller's schema) and writes it; a null tag is not saved. */
    public interface Serializer<T> {
        T read(CompoundTag tag, HolderLookup.Provider provider);

        @Nullable
        CompoundTag write(T value, HolderLookup.Provider provider);
    }

    /** The loader's side of one attachment. */
    public interface Backend<T> {
        T get(Entity holder);

        Optional<T> existing(Entity holder);

        boolean has(Entity holder);

        void set(Entity holder, T value);

        void remove(Entity holder);
    }

    private static final List<Attachment<?>> ALL = new ArrayList<>();

    private final ResourceLocation id;
    private final Supplier<T> defaultValue;
    private final @Nullable Serializer<T> serializer;
    private final boolean copyOnDeath;
    private Backend<T> backend;

    private Attachment(ResourceLocation id, Supplier<T> defaultValue, @Nullable Serializer<T> serializer, boolean copyOnDeath) {
        this.id = id;
        this.defaultValue = defaultValue;
        this.serializer = serializer;
        this.copyOnDeath = copyOnDeath;
        synchronized (ALL) {
            ALL.add(this);
        }
    }

    public static <T> Attachment<T> saved(ResourceLocation id, Supplier<T> defaultValue, Serializer<T> serializer, boolean copyOnDeath) {
        return new Attachment<>(id, defaultValue, serializer, copyOnDeath);
    }

    public static <T> Attachment<T> transientData(ResourceLocation id, Supplier<T> defaultValue) {
        return new Attachment<>(id, defaultValue, null, false);
    }

    public static List<Attachment<?>> all() {
        synchronized (ALL) {
            return List.copyOf(ALL);
        }
    }

    public ResourceLocation id() {
        return id;
    }

    public Supplier<T> defaultValue() {
        return defaultValue;
    }

    public @Nullable Serializer<T> serializer() {
        return serializer;
    }

    public boolean copyOnDeath() {
        return copyOnDeath;
    }

    /** Set once by the loader when it registers the attachment. */
    public void bind(Backend<T> backend) {
        this.backend = backend;
    }

    private Backend<T> backend() {
        if (backend == null) throw new IllegalStateException("attachment " + id + " is not registered");
        return backend;
    }

    /** The value (created from the default and attached if missing). */
    public T get(Entity holder) {
        return backend().get(holder);
    }

    public Optional<T> existing(Entity holder) {
        return backend().existing(holder);
    }

    public boolean has(Entity holder) {
        return backend().has(holder);
    }

    public void set(Entity holder, T value) {
        backend().set(holder, value);
    }

    public void remove(Entity holder) {
        backend().remove(holder);
    }
}
