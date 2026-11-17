package org.overengineer.bettacraft.api.event;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * M32 part d: an animal of a Bettacraft tank died (also an animal of vanilla or another mod in the tank). Posted before the
 * animal is removed, so {@link #getEntity} can still be read; for the small animals the tank keeps as data (shrimp, snails,
 * crayfish: {@link #getEntity} null) one event per species and cause with {@link #getCount}.
 * <p>{@link #getCause} is a short id, the same as the tank log uses ({@code cause.bettacraft.<cause>} is its text where
 * the log has one). Care: {@code hunger}, {@code both} (hunger and temperature), {@code temperature},
 * {@code water_quality}, {@code water_chemistry}, {@code co2}, {@code oxygen}, {@code salinity}, {@code overcrowding},
 * {@code teeth}, {@code fin_nipping}, {@code lonely}, {@code flow}, {@code no_posts}, {@code illness}. Killed:
 * {@code fight}, {@code chased}, {@code predator}, {@code eaten} (prey of a fish or crayfish), {@code filter_intake},
 * {@code player}, {@code killed} (another entity), {@code drowned}, {@code starved}, {@code injuries}. Small animals:
 * {@code critter.<cause>} (for example {@code critter.hunger}). {@link #getKiller} is the attacking entity where there is
 * one.</p>
 */
public class AnimalDiedEvent extends TankEvent {
    private final ResourceLocation species;
    @Nullable
    private final Entity entity, killer;
    private final int count;
    private final String cause;

    public AnimalDiedEvent(ServerLevel level, BlockPos tankPos, UUID tankId, String tankName, ResourceLocation species, @Nullable Entity entity,
                           int count, String cause, @Nullable Entity killer) {
        super(level, tankPos, tankId, tankName);
        this.species = species;
        this.entity = entity;
        this.count = count;
        this.cause = cause;
        this.killer = killer;
    }

    public ResourceLocation getSpecies() {
        return species;
    }

    /** The animal (still in the level), null for small animals kept as data. */
    @Nullable
    public Entity getEntity() {
        return entity;
    }

    public int getCount() {
        return count;
    }

    public String getCause() {
        return cause;
    }

    @Nullable
    public Entity getKiller() {
        return killer;
    }
}
