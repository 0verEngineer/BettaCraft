package org.overengineer.bettacraft.datagen.model;

import net.minecraft.resources.Identifier;

/**
 * M52b: our own model data generators, the API of NeoForge 1.21.1's block state and model providers that the data
 * generators of the mod were written against (NeoForge 21.4 replaced them with vanilla's {@code ModelProvider}). The
 * generators stay as they are on every branch; these classes write the same JSON (written for this mod, not taken from
 * NeoForge). A model file: the id of a model.
 */
public abstract class ModelFile {
    protected Identifier location;

    protected ModelFile(Identifier location) {
        this.location = location;
    }

    public Identifier getLocation() {
        return location;
    }

    /** The location used in JSON (the same as {@link #getLocation}). */
    public Identifier getUncheckedLocation() {
        return location;
    }

    /** A model somebody else writes (or a vanilla one), not checked. */
    public static class UncheckedModelFile extends ModelFile {
        public UncheckedModelFile(String location) {
            this(Identifier.parse(location));
        }

        public UncheckedModelFile(Identifier location) {
            super(location);
        }
    }

    /** An existing model (a hand-written one of the mod or a vanilla one). */
    public static class ExistingModelFile extends ModelFile {
        public ExistingModelFile(Identifier location, ExistingFileHelper helper) {
            super(location);
        }
    }
}
