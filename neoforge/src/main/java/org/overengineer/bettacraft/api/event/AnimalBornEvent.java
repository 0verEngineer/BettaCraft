package org.overengineer.bettacraft.api.event;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/**
 * M32 part d: young animals came into a Bettacraft tank by breeding: betta fry swim free from the nest, tankmates are born
 * or swim free from their clutch, shrimp, snails and crayfish are born, axolotl, turtle and herp eggs hatch, the mobs in
 * glass and the animals of other mods with a profile breed. One event per brood.
 * <ul>
 * <li>{@link #getSpecies}: the entity type id ({@code bettacraft:betta}, {@code bettacraft:neon_tetra},
 * {@code fintastic:guppy}, ...) or, for the small animals the tank keeps as data, the critter id
 * ({@code bettacraft:cherry_shrimp}, see {@link #isCritter}).</li>
 * <li>{@link #getCount}: how many young (at least 1).</li>
 * <li>{@link #isHatched}: true for eggs that hatched or larvae that swim free, false for live births.</li>
 * <li>{@link #isForeign}: an animal of vanilla or another mod bred by its Bettacraft profile.</li>
 * </ul>
 */
public class AnimalBornEvent extends TankEvent {
    private final ResourceLocation species;
    private final int count;
    private final boolean hatched, foreign, critter;

    public AnimalBornEvent(ServerLevel level, BlockPos tankPos, UUID tankId, String tankName, ResourceLocation species, int count,
                           boolean hatched, boolean foreign, boolean critter) {
        super(level, tankPos, tankId, tankName);
        this.species = species;
        this.count = count;
        this.hatched = hatched;
        this.foreign = foreign;
        this.critter = critter;
    }

    public ResourceLocation getSpecies() {
        return species;
    }

    public int getCount() {
        return count;
    }

    public boolean isHatched() {
        return hatched;
    }

    public boolean isForeign() {
        return foreign;
    }

    /** A small animal kept as tank data (shrimp, snail, crayfish, hermit crab): no entity. */
    public boolean isCritter() {
        return critter;
    }
}
