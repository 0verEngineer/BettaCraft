package org.overengineer.bettacraft.food;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaBreeding;
import org.overengineer.bettacraft.entity.betta.BettaHealth;
import org.overengineer.bettacraft.tankmates.TankmateFeeding;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M26 part b: food variety. Every fish food belongs to a {@link Group} (item tags {@code #bettacraft:food_groups/<group>}),
 * every species to a {@link Diet}. What a diet likes and what suits it badly are item tags too
 * ({@code #bettacraft:diet/<diet>/favourite}, {@code #bettacraft:diet/<diet>/poor}), so datapacks can change them:
 * <ul>
 *     <li>a <b>favourite</b> food conditions an adult for breeding (like live food),</li>
 *     <li>a <b>poor</b> food fills only for a while ({@code diet.poorFoodFedDays}: the fish is hungry again sooner),</li>
 *     <li>a fish that ate {@code diet.varietyGroups} groups within {@code diet.varietyDays} has a <b>varied diet</b>: it
 *     recovers faster from stress and sickness ({@code diet.varietyRecovery}), a betta shows stronger colours.</li>
 * </ul>
 */
public final class FoodDiet {
    /** Food groups (the ordinal is the index in {@link BettaHealth}; at most {@link BettaHealth#FOOD_GROUPS}). */
    public enum Group {
        PELLETS, FLAKES, GRANULES, WAFERS, FROZEN, LIVE;

        public final TagKey<Item> tag = TagKey.create(Registries.ITEM, BettaCraft.id("food_groups/" + name().toLowerCase(Locale.ROOT)));

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** What kind of eater a species is. */
    public enum Diet {
        /** Bettas: carnivores at the surface. */
        BETTA,
        /** Midwater fish (tetras, rasboras, livebearers, gouramis, marine fish): eat about everything. */
        OMNIVORE,
        /** Bottom fish (corydoras, loaches): sinking food. */
        BOTTOM,
        /** Algae grazers (plecos, otocinclus): plant food. */
        GRAZER,
        /** Seahorses: frozen and live food. */
        SEAHORSE,
        /** M29 part b: puffers: hard food (shell food), frozen and live food; flakes and pellets badly. */
        PUFFER,
        /** M30: big predators (sharks, the big catfish): frozen and live food (and the fish they catch); flakes and wafers badly. */
        CARNIVORE;

        public final TagKey<Item> favourite = TagKey.create(Registries.ITEM, BettaCraft.id("diet/" + id() + "/favourite"));
        public final TagKey<Item> poor = TagKey.create(Registries.ITEM, BettaCraft.id("diet/" + id() + "/poor"));

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum Fit { FAVOURITE, OK, POOR }

    private FoodDiet() {}

    @Nullable
    public static Group group(ItemStack food) {
        for (Group g : Group.values()) if (food.is(g.tag)) return g;
        return null;
    }

    public static Diet of(TankmateSpecies s) {
        if (s.large() != null) return s.large().diet(); // M30
        if (s.seahorse()) return Diet.SEAHORSE;
        if (s.puffer()) return Diet.PUFFER; // M29 part b
        if (s.grazer()) return Diet.GRAZER;
        if (s.zone().bottom() || s.model() == TankmateSpecies.Model.LOACH) return Diet.BOTTOM;
        return Diet.OMNIVORE;
    }

    public static Fit fit(Diet diet, ItemStack food) {
        if (food.is(diet.favourite)) return Fit.FAVOURITE;
        if (food.is(diet.poor)) return Fit.POOR;
        return Fit.OK;
    }

    /** The diet bonus of the health timers (off with {@code features.foodVariety.enabled}). */
    public static BettaHealth.Diet params() {
        if (!Features.enabled(Feature.FOOD_VARIETY)) return BettaHealth.Diet.OFF;
        return new BettaHealth.Diet(ServerConfig.getOr(ServerConfig.DIET_VARIETY_DAYS, 3.0), ServerConfig.getOr(ServerConfig.DIET_VARIETY_GROUPS, 3),
                ServerConfig.getOr(ServerConfig.DIET_VARIETY_RECOVERY, 1.5));
    }

    /**
     * A fish of this diet ate the food (after {@link BettaHealth#feed}): remembers its group; a poor food fills for a
     * shorter while, a favourite conditions an adult (unless it is live food, which conditions anyway).
     */
    public static void eaten(ItemStack food, BettaHealth health, BettaBreeding breeding, boolean adult, Diet diet) {
        if (!Features.enabled(Feature.FOOD_VARIETY) || food.isEmpty()) return;
        Group g = group(food);
        if (g != null) health.ate(g.ordinal());
        switch (fit(diet, food)) {
            case POOR -> health.feedPoorly(ServerConfig.getOr(ServerConfig.DIET_POOR_FED_DAYS, 0.5));
            case FAVOURITE -> {
                if (adult && BettaFood.kind(food) != BettaFood.Kind.LIVE) breeding.condition();
            }
            default -> {}
        }
    }

    /** Shown in the tank menu: the food groups eaten lately, and whether that counts as varied. */
    public static net.minecraft.nbt.CompoundTag info(BettaHealth h) {
        BettaHealth.Diet d = params();
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        t.putInt("n", h.variety(d.windowDays()));
        t.putInt("need", d.groups() == Integer.MAX_VALUE ? 0 : d.groups());
        t.putBoolean("varied", h.varied(d));
        return t;
    }

    /** Bottom fish get sinking food (granules, wafers) even when faster fish are there. */
    public static boolean sinks(ItemStack food) {
        return TankmateFeeding.bottomFood(food) || food.is(org.overengineer.bettacraft.registry.ModTags.SINKING_FOOD);
    }
}
