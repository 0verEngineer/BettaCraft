package org.overengineer.bettacraft.datagen.model;

import net.minecraft.resources.Identifier;

/** M52b: a block model being generated (see {@link ModelFile}). */
public class BlockModelBuilder extends ModelBuilder<BlockModelBuilder> {
    public BlockModelBuilder(Identifier location, ExistingFileHelper helper) {
        super(location);
    }
}
