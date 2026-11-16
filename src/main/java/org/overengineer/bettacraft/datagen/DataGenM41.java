package org.overengineer.bettacraft.datagen;

import net.minecraft.core.Direction;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import org.overengineer.bettacraft.BettaCraft;

/**
 * M41 data: the model of the drive adapter and its mining tags. The block exists only with Create (datagen runs without
 * it), so everything goes by id: the model by name, the tags optional; the block state ({@code blockstates/drive_adapter.json})
 * and the recipe ({@code recipe/compat/create_drive_adapter.json}, with a {@code mod_loaded} condition) are written by hand
 * in {@code src/main/resources}. The block drops itself without a loot table.
 */
final class DataGenM41 {
    private DataGenM41() {}

    /**
     * Modelled facing north (the shaft side; the block state turns it): a mounting plate on the tank side (z 14..16), the
     * casing (2..14, z 5..14) with the shaft hole in front, a brass bearing ring (5..11, z 3..5). The shaft stub turns in its
     * block entity renderer.
     */
    static void blockStates(BlockStateProvider p) {
        BlockModelBuilder m = p.models().getBuilder("drive_adapter")
                .texture("plate", p.modLoc("block/drive_adapter_plate")).texture("casing", p.modLoc("block/drive_adapter_casing"))
                .texture("front", p.modLoc("block/drive_adapter_front")).texture("ring", p.modLoc("block/drive_adapter_ring"))
                .texture("particle", p.modLoc("block/drive_adapter_casing"));
        m.element().from(0, 0, 14).to(16, 16, 16).allFaces((d, f) -> {
            f.texture("#plate");
            if (d == Direction.SOUTH) f.cullface(d);
        }).end();
        // the casing shows its whole texture (wooden frame) on every face
        m.element().from(2, 2, 5).to(14, 14, 14).allFaces((d, f) -> f.texture(d == Direction.NORTH ? "#front" : "#casing").uvs(0, 0, 16, 16)).end();
        m.element().from(5, 5, 3).to(11, 11, 5).allFaces((d, f) -> f.texture("#ring")).end();
    }

    static void itemModels(ItemModelProvider p) {
        p.withExistingParent("drive_adapter", p.modLoc("block/drive_adapter"));
    }

    static void blockTags(java.util.function.Function<TagKey<Block>, IntrinsicHolderTagsProvider.IntrinsicTagAppender<Block>> tag) {
        tag.apply(BlockTags.MINEABLE_WITH_PICKAXE).addOptional(BettaCraft.id("drive_adapter"));
        tag.apply(BlockTags.MINEABLE_WITH_AXE).addOptional(BettaCraft.id("drive_adapter"));
    }
}
