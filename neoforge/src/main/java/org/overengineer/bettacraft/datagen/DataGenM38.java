package org.overengineer.bettacraft.datagen;

import java.util.List;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * M38 data: the water plants of Regions Unexplored, Alex's Caves and Twilight Forest stand in the dry interior and sway in
 * the miniature like those of Fintastic (M7, M22.1); their floating leaves mark the surface for bubble nests; corals and
 * epiphytes grow on Fintastic's live rock. What these plants do in a tank is in the profiles
 * {@code data/bettacraft/bettacraft/foreign_plants} (read from the jars of ATM10AA: Regions Unexplored 0.6.3, Alex's Caves
 * 1.1.3, Twilight Forest 4.8.3345, Fintastic 3.3).
 */
final class DataGenM38 {
    private DataGenM38() {}

    /** Water plants of other mods (besides Fintastic) that stand and sway in the interior. */
    static final List<String[]> WATER_PLANTS = List.of(
            new String[]{Compat.RU, "cattail"}, new String[]{Compat.ALEXS_CAVES, "ping_pong_sponge"},
            new String[]{Compat.TF, "huge_water_lily"});
    /** Floating leaves of other mods: they stand in the interior and mark the water surface (bubble nests). */
    static final List<String[]> FLOATING = List.of(
            new String[]{Compat.RU, "duckweed"}, new String[]{Compat.RU, "flowering_lily_pad"}, new String[]{Compat.RU, "giant_lily_pad"},
            new String[]{Compat.TF, "huge_lily_pad"}, new String[]{Compat.TF, "huge_water_lily"});
    /** Fintastic's rocks: corals and epiphytes grow on them like on our reef rock. */
    static final List<String> FINTASTIC_ROCKS = List.of("live_rock", "porous_live_rock", "green_algae_live_rock", "red_algae_live_rock",
            "dead_live_rock", "dead_porous_live_rock");

    static void blockTags(java.util.function.Function<TagKey<Block>, net.minecraft.data.tags.TagAppender<Block, Block>> tag) {
        var interior = tag.apply(ModTags.INTERIOR_WATER_PLANTS);
        var sway = tag.apply(ModTags.SWAYING_PLANTS);
        for (String[] p : WATER_PLANTS) {
            interior.add(net.minecraft.tags.TagEntry.optionalElement(Compat.rl(p[0], p[1])));
            sway.add(net.minecraft.tags.TagEntry.optionalElement(Compat.rl(p[0], p[1])));
        }
        var floating = tag.apply(ModTags.FLOATING_PLANTS);
        for (String[] p : FLOATING) {
            interior.add(net.minecraft.tags.TagEntry.optionalElement(Compat.rl(p[0], p[1])));
            floating.add(net.minecraft.tags.TagEntry.optionalElement(Compat.rl(p[0], p[1])));
        }
        var reef = tag.apply(ModTags.REEF_SURFACES);
        var epiphyte = tag.apply(ModTags.EPIPHYTE_SURFACES);
        for (String r : FINTASTIC_ROCKS) {
            reef.add(net.minecraft.tags.TagEntry.optionalElement(Compat.rl(Compat.FINTASTIC, r)));
            epiphyte.add(net.minecraft.tags.TagEntry.optionalElement(Compat.rl(Compat.FINTASTIC, r)));
        }
    }
}
