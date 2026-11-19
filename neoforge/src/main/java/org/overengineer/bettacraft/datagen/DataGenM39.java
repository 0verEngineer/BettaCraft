package org.overengineer.bettacraft.datagen;

import java.util.List;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.overengineer.bettacraft.food.FoodDiet;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * M39 part a data (question C1): the food of other mods in our food tags, all optional ({@code "required": false}). Checked in
 * the jars of ATM10AA: Fintastic 3.3 ({@code copepod}, {@code daphnia}, {@code fairy_shrimp}: live food like our daphnia and
 * artemia; Fintastic's daphnia also starts our daphnia barrel), Fish Eggs 1.0.2 ({@code fish_food} "Fish Flakes": flake food;
 * {@code fish_eggs}, {@code squid_eggs}: protein-rich frozen food that conditions for breeding like tubifex).
 */
final class DataGenM39 {
    /** Live food of other mods (live food, betta food, the live food group). */
    static final List<Identifier> LIVE = List.of(Compat.rl(Compat.FINTASTIC, "copepod"), Compat.rl(Compat.FINTASTIC, "daphnia"),
            Compat.rl(Compat.FINTASTIC, "fairy_shrimp"));
    /** Flake food of other mods. */
    static final List<Identifier> FLAKES = List.of(Compat.rl(Compat.FISH_EGGS, "fish_food"));
    /** Protein-rich frozen food of other mods (fish and squid roe). */
    static final List<Identifier> FROZEN = List.of(Compat.rl(Compat.FISH_EGGS, "fish_eggs"), Compat.rl(Compat.FISH_EGGS, "squid_eggs"));

    private DataGenM39() {}

    static void itemTags(Function<TagKey<Item>, ObjTags.Appender<Item>> tag) {
        for (var f : LIVE) {
            tag.apply(ModTags.LIVE_FOOD).add(net.minecraft.tags.TagEntry.optionalElement(f));
            tag.apply(ModTags.BETTA_FOOD).add(net.minecraft.tags.TagEntry.optionalElement(f));
        }
        for (var f : FLAKES) {
            tag.apply(ModTags.BETTA_FOOD).add(net.minecraft.tags.TagEntry.optionalElement(f));
            tag.apply(FoodDiet.Group.FLAKES.tag).add(net.minecraft.tags.TagEntry.optionalElement(f));
        }
        for (var f : FROZEN) {
            tag.apply(ModTags.FROZEN_FOOD).add(net.minecraft.tags.TagEntry.optionalElement(f));
            tag.apply(ModTags.BETTA_FOOD).add(net.minecraft.tags.TagEntry.optionalElement(f));
            tag.apply(ModTags.CONDITIONING_FOOD).add(net.minecraft.tags.TagEntry.optionalElement(f));
        }
        // the seed of a daphnia barrel: ours and Fintastic's
        tag.apply(ModTags.DAPHNIA_STARTERS).add(ModItems.DAPHNIA.get()).add(net.minecraft.tags.TagEntry.optionalElement(Compat.rl(Compat.FINTASTIC, "daphnia")));
    }
}
