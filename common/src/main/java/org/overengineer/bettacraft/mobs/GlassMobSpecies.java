package org.overengineer.bettacraft.mobs;

import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.terrarium.LandWalker;
import org.overengineer.bettacraft.terrarium.TerrariumNeeds;
import org.overengineer.bettacraft.terrarium.TerrariumNeeds.Habitat;

/**
 * M31 part d: a Minecraft mob that lives shrunk in a tank ("mobs in glass"). Pure data, the table {@link #DEFAULTS} is the
 * list of question C2 (drowned, zombie, husk, skeleton, stray, spider, cave spider, silverfish, endermite, small slime, small
 * magma cube); a mob added by the config ({@code inhabitants.allow}) gets {@link #generic}.
 *
 * @param id       the entity type
 * @param scale    the {@code generic.scale} in a tank (an adult zombie: 0.18, about a third of a block tall)
 * @param gait     how it moves with the {@link LandWalker} (spiders climb the glass)
 * @param needs    its tank: the drowned needs water, the zombie goes anywhere (in water it becomes a drowned), spiders,
 *                 skeletons and slimes need land, silverfish, endermites and magma cubes a dry terrarium
 * @param breeding how it breeds in the tank (fed with {@link #food}); the undead skeletons and the silverfish and endermites do not
 * @param food     what makes a pair breed (and what it eats by hand); null = nothing
 * @param bait     what lures it into an open tank; null = it cannot be lured (catch it with the mob jar)
 * @param climbs   climbs into a tank of any height when lured (spiders); the others need the top within a jump
 * @param stock    how much an adult counts for the stocking (an adult betta = 1)
 * @param undead   burns in sunlight outside (and, with {@code terrarium.mobUndeadBurn}, in a tank without a lid)
 */
public record GlassMobSpecies(ResourceLocation id, float scale, LandWalker.Gait gait, TerrariumNeeds needs, Breeding breeding,
                              @Nullable Predicate<ItemStack> food, @Nullable Predicate<ItemStack> bait, boolean climbs, double stock, boolean undead) {

    /** How a mob breeds in the tank. */
    public enum Breeding {
        /** It does not. */
        NONE,
        /** A vanilla baby (zombies, husks, drowned) that grows up in the tank. */
        BABY,
        /** A small young one that grows to the size of its parents (spiderlings, a new small slime). */
        YOUNG
    }

    private static final LandWalker.Gait HUMANOID = new LandWalker.Gait(0.012, 0.0, true, 0.3, 0.0, 0.0, 0.35, 0.35);
    private static final LandWalker.Gait ZOMBIE = new LandWalker.Gait(0.011, 0.01, true, 0.3, 0.12, 0.0, 0.35, 0.35);
    private static final LandWalker.Gait DROWNED = new LandWalker.Gait(0.012, 0.016, true, 0.3, 0.75, 0.0, 0.3, 0.35);
    private static final LandWalker.Gait SPIDER = new LandWalker.Gait(0.02, 0.0, true, 1.5, 0.0, 0.0, 0.3, 0.18, LandWalker.Style.CLIMB, 0.4, false);
    private static final LandWalker.Gait CAVE_SPIDER = new LandWalker.Gait(0.022, 0.0, true, 1.5, 0.0, 0.0, 0.3, 0.12, LandWalker.Style.CLIMB, 0.45, false);
    private static final LandWalker.Gait CRAWLER = new LandWalker.Gait(0.024, 0.0, true, 0.3, 0.0, 0.0, 0.3, 0.1);
    private static final LandWalker.Gait SLIME = new LandWalker.Gait(0.02, 0.012, false, 0.3, 0.1, 0.0, 0.4, 0.2, LandWalker.Style.HOP, 0, false);
    private static final LandWalker.Gait MAGMA = new LandWalker.Gait(0.022, 0.0, true, 0.35, 0.0, 0.0, 0.4, 0.2, LandWalker.Style.HOP, 0, false);

    private static final Predicate<ItemStack> FLESH = s -> s.is(Items.ROTTEN_FLESH); // lambdas: no registry access before the game runs
    private static final Predicate<ItemStack> INSECTS = s -> s.is(ModTags.FEEDER_INSECTS);

    private static GlassMobSpecies of(String id, float scale, LandWalker.Gait gait, TerrariumNeeds needs, Breeding b,
                                      @Nullable Predicate<ItemStack> food, @Nullable Predicate<ItemStack> bait, boolean climbs, double stock, boolean undead) {
        return new GlassMobSpecies(ResourceLocation.withDefaultNamespace(id), scale, gait, needs, b, food, bait, climbs, stock, undead);
    }

    private static TerrariumNeeds habitat(Habitat h) {
        return TerrariumNeeds.builder(h).build();
    }

    /** The mobs of question C2. */
    public static final List<GlassMobSpecies> DEFAULTS = List.of(
            of("drowned", 0.18F, DROWNED, habitat(Habitat.WET), Breeding.BABY, FLESH, FLESH, false, 0.5, true),
            of("zombie", 0.18F, ZOMBIE, habitat(Habitat.ALL), Breeding.BABY, FLESH, FLESH, false, 0.5, true),
            of("husk", 0.18F, HUMANOID, habitat(Habitat.ANY), Breeding.BABY, FLESH, FLESH, false, 0.5, true),
            of("skeleton", 0.18F, HUMANOID, habitat(Habitat.ANY), Breeding.NONE, null, s -> s.is(Items.BONE), false, 0.5, true),
            of("stray", 0.18F, HUMANOID, habitat(Habitat.ANY), Breeding.NONE, null, s -> s.is(Items.BONE), false, 0.5, true),
            of("spider", 0.2F, SPIDER, habitat(Habitat.ANY), Breeding.YOUNG, INSECTS, INSECTS, true, 0.5, false),
            of("cave_spider", 0.3F, CAVE_SPIDER, habitat(Habitat.ANY), Breeding.YOUNG, INSECTS, INSECTS, true, 0.35, false),
            of("silverfish", 0.4F, CRAWLER, habitat(Habitat.DRY), Breeding.NONE, null, null, false, 0.25, false),
            of("endermite", 0.4F, CRAWLER, habitat(Habitat.DRY), Breeding.NONE, null, null, false, 0.25, false),
            of("slime", 0.4F, SLIME, habitat(Habitat.ANY), Breeding.YOUNG, s -> s.is(Items.SLIME_BALL), s -> s.is(Items.SLIME_BALL), false, 0.35, false),
            of("magma_cube", 0.4F, MAGMA, TerrariumNeeds.builder(Habitat.DRY).air(15, Double.NaN).build(), Breeding.YOUNG,
                    s -> s.is(Items.MAGMA_CREAM), s -> s.is(Items.MAGMA_CREAM), false, 0.35, false));

    /** A mob of another mod (or vanilla) added through {@code inhabitants.allow}: shrunk to about a third of a block, on land. */
    public static GlassMobSpecies generic(ResourceLocation id, float width, float height) {
        float s = Math.max(0.0625F, Math.min(1F, 0.35F / Math.max(0.1F, Math.max(width, height))));
        return new GlassMobSpecies(id, s, HUMANOID, habitat(Habitat.ANY), Breeding.NONE, null, null, false, 0.5, false);
    }

    /** The scale of a mob of this species at this growth (a young one starts at 55 % of the adult size). */
    public float scaleAt(double growth) {
        if (breeding == Breeding.BABY || growth >= 1) return scale; // a vanilla baby is small by itself
        return (float) (scale * (0.55 + 0.45 * Math.max(0, growth)));
    }

    public boolean eats(ItemStack s) {
        return food != null && !s.isEmpty() && food.test(s);
    }

    public boolean lures(ItemStack s) {
        return bait != null && !s.isEmpty() && bait.test(s);
    }
}
