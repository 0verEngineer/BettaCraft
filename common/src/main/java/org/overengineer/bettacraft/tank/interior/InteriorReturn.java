package org.overengineer.bettacraft.tank.interior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Player attachment {@code bettacraft:interior_return}: where to send a player back when they leave an interior.
 * Persisted (survives logout inside the interior) and versioned via the {@code schema} field.
 */
public record InteriorReturn(int schema, ResourceKey<Level> dimension, Vec3 position, float yRot, float xRot, int regionId, long serial) {
    public static final int CURRENT_SCHEMA = 1;

    public static final Codec<InteriorReturn> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("schema", 0).forGetter(InteriorReturn::schema),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(InteriorReturn::dimension),
            Vec3.CODEC.fieldOf("pos").forGetter(InteriorReturn::position),
            Codec.FLOAT.optionalFieldOf("y_rot", 0F).forGetter(InteriorReturn::yRot),
            Codec.FLOAT.optionalFieldOf("x_rot", 0F).forGetter(InteriorReturn::xRot),
            Codec.INT.optionalFieldOf("region", -1).forGetter(InteriorReturn::regionId),
            // M53c: the serial of the region (its id may be handed out again after the region was removed)
            Codec.LONG.optionalFieldOf("serial", 0L).forGetter(InteriorReturn::serial)
    ).apply(i, InteriorReturn::new));

    public InteriorReturn(ResourceKey<Level> dimension, Vec3 position, float yRot, float xRot, InteriorRegion region) {
        this(CURRENT_SCHEMA, dimension, position, yRot, xRot, region.id(), region.serial());
    }

    /** Placeholder value meaning "not inside an interior". */
    public static final InteriorReturn NONE = new InteriorReturn(CURRENT_SCHEMA, Level.OVERWORLD, Vec3.ZERO, 0, 0, -1, 0L);

    public boolean isSet() {
        return regionId >= 0;
    }

    /** M53c: whether this return point belongs to that region (same id and the same allocation of it). */
    public boolean belongsTo(InteriorRegion region) {
        return regionId == region.id() && serial == region.serial();
    }
}
