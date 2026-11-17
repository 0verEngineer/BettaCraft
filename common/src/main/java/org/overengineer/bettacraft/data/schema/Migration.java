package org.overengineer.bettacraft.data.schema;

import net.minecraft.nbt.CompoundTag;

/**
 * Upgrades data from version {@code n} to {@code n + 1}. Implementations receive a private copy and may
 * mutate and return it. They must keep keys they do not know (unknown data is carried along, never dropped).
 */
@FunctionalInterface
public interface Migration {
    CompoundTag apply(CompoundTag data);
}
