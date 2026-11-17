package org.overengineer.bettacraft.platform.model;

/**
 * M52: a typed key of the render data a block entity hands to its block model (NeoForge: a ModelProperty of the model data;
 * Fabric: a key of the block entity's render data). Compared by identity.
 */
public final class ModelKey<T> {
    private final String name;

    public ModelKey(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return "ModelKey[" + name + "]";
    }
}
