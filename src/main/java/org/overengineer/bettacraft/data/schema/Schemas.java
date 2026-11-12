package org.overengineer.bettacraft.data.schema;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * All migration chains of Bettacraft. Every persisted object type has exactly one chain here.
 * <p>
 * Rule: when the stored format of a type changes, add a new {@code step(n, ...)} with a unit test using a frozen
 * example NBT of version {@code n} (see {@code SchemaMigrationTest}). Never edit an existing step.
 */
public final class Schemas {
    private Schemas() {}

    /** Tank block entity (all tank blocks; the controller additionally carries the tank data). */
    public static final MigrationChain TANK_BLOCK_ENTITY = MigrationChain.builder("tank_block_entity")
            .step(0, Schemas::tankV0toV1)
            .step(1, Schemas::tankV1toV2)
            .build();

    /** Betta data (entity NBT key "betta", item component bettacraft:fish_data, bucket_entity_data key "betta"). */
    public static final MigrationChain BETTA = MigrationChain.builder("betta")
            .step(0, data -> data) // v0 = written before versioning; identical layout
            .build();

    /** Entity attachment bettacraft:tank_ref (controller position + tank id). */
    public static final MigrationChain TANK_REF = MigrationChain.builder("tank_ref")
            .step(0, data -> data)
            .build();

    /** Feeder block entity. */
    public static final MigrationChain FEEDER = MigrationChain.builder("feeder")
            .step(0, data -> data)
            .build();

    /** Tank interior region SavedData (one per server, in the overworld data folder). */
    public static final MigrationChain INTERIOR_REGIONS = MigrationChain.builder("interior_regions")
            .step(0, data -> data) // v0 = written before versioning; identical layout
            .build();

    /** Player attachment holding the position to return to when leaving an interior. */
    public static final MigrationChain INTERIOR_RETURN = MigrationChain.builder("interior_return")
            .step(0, data -> data)
            .build();

    /**
     * v0 -> v1: tank data written without schema. A controller (has "members") without a fixed interior scale
     * gets the historical default 16, and a missing water volume becomes 0 mB. Everything else is kept.
     */
    static CompoundTag tankV0toV1(CompoundTag data) {
        if (data.contains("tank", Tag.TAG_COMPOUND)) {
            CompoundTag tank = data.getCompound("tank");
            if (!tank.contains("scale", Tag.TAG_ANY_NUMERIC)) {
                tank.putInt("scale", 16);
            }
            if (!tank.contains("water_mb", Tag.TAG_ANY_NUMERIC)) {
                tank.putLong("water_mb", 0L);
            }
        }
        return data;
    }

    /**
     * v1 -> v2 (M1): tanks gain care data (temperature, equipment, sign, lock, inhabitants, log). Missing values get
     * their defaults: target 27 C, water at the target temperature, sign visible at top centre, front south, unlocked.
     */
    static CompoundTag tankV1toV2(CompoundTag data) {
        if (data.contains("tank", Tag.TAG_COMPOUND)) {
            CompoundTag tank = data.getCompound("tank");
            if (!tank.contains("target_temp", Tag.TAG_ANY_NUMERIC)) tank.putFloat("target_temp", 27.0F);
            if (!tank.contains("water_temp", Tag.TAG_ANY_NUMERIC)) tank.putFloat("water_temp", tank.getFloat("target_temp"));
            if (!tank.contains("sign_visible")) tank.putBoolean("sign_visible", true);
            if (!tank.contains("sign_pos", Tag.TAG_STRING)) tank.putString("sign_pos", "top_center");
            if (!tank.contains("front", Tag.TAG_STRING)) tank.putString("front", "south");
        }
        return data;
    }
}
