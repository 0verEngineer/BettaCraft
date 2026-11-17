package org.overengineer.bettacraft.datagen.model;

/**
 * M52b: stands for NeoForge 1.21.1's existing file helper in the constructors of our model generators. It checked that
 * referenced models and textures exist; the generators of this mod reference their hand-written models by id only.
 */
public final class ExistingFileHelper {
    public static final ExistingFileHelper INSTANCE = new ExistingFileHelper();

    private ExistingFileHelper() {}
}
