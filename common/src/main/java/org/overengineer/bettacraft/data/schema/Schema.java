package org.overengineer.bettacraft.data.schema;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * The {@code "schema"} version field every persisted Bettacraft object carries (PLAN section 2).
 * <p>
 * Version 0 means "no schema field present", i.e. data written before versioning existed.
 * This class only touches plain NBT and works without a Minecraft bootstrap (unit-testable).
 */
public final class Schema {
    public static final String KEY = "schema";

    private Schema() {}

    /** Reads the schema version; missing or non-numeric values count as version 0. */
    public static int read(CompoundTag tag) {
        return tag.contains(KEY, Tag.TAG_ANY_NUMERIC) ? tag.getInt(KEY) : 0;
    }

    public static CompoundTag write(CompoundTag tag, int version) {
        tag.putInt(KEY, version);
        return tag;
    }
}
