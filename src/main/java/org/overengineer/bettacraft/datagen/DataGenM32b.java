package org.overengineer.bettacraft.datagen;

import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.overengineer.bettacraft.registry.ModBlocks;

/**
 * M32 part b data: Sable (Create Aeronautics) takes a tank into a sub-level without removing it the normal way
 * ({@code sable:silent_assembly_removal}: the block entity goes first, so a tank that flies away is not broken at its old
 * place); the tank then moves along as data ({@code TankMoves}).
 */
final class DataGenM32b {
    static final TagKey<Block> SABLE_SILENT_ASSEMBLY_REMOVAL = TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("sable", "silent_assembly_removal"));

    private DataGenM32b() {}

    static void blockTags(java.util.function.Function<TagKey<Block>, IntrinsicHolderTagsProvider.IntrinsicTagAppender<Block>> tag) {
        tag.apply(SABLE_SILENT_ASSEMBLY_REMOVAL).add(ModBlocks.TANK.get());
    }
}
