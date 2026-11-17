package org.overengineer.bettacraft.datagen.model;

import net.minecraft.data.PackOutput;

/** M52b: the block models of a mod ({@code assets/<ns>/models/block}). */
public abstract class BlockModelProvider extends ModelProvider<BlockModelBuilder> {
    public BlockModelProvider(PackOutput output, String modid, ExistingFileHelper helper) {
        super(output, modid, BLOCK_FOLDER, id -> new BlockModelBuilder(id, helper), helper);
    }

    @Override
    public String getName() {
        return "Block Models: " + modid;
    }
}
