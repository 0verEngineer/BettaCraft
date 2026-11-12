package org.overengineer.bettacraft.registry;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;

public final class ModTags {
    /** Glass blocks usable for tanks ({@code #c:glass_blocks}). */
    public static final TagKey<Block> TANK_GLASS_BLOCKS = Tags.Blocks.GLASS_BLOCKS;
    public static final TagKey<Item> TANK_GLASS_ITEMS = Tags.Items.GLASS_BLOCKS;

    private ModTags() {}
}
