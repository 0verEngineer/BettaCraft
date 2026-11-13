package org.overengineer.bettacraft.food;

import javax.annotation.Nullable;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * Who eats what (PLAN 5.3 / 6.3). Kinds come from item tags so datapacks can add food:
 * <ul>
 *     <li>{@code #bettacraft:fry_food_start} (infusoria): fry, the whole fry stage</li>
 *     <li>{@code #bettacraft:fry_food} (artemia nauplii): fry in the second half of the stage</li>
 *     <li>{@code #bettacraft:live_food} (mosquito larvae): juveniles and older, conditions adults for breeding</li>
 *     <li>other {@code #bettacraft:betta_food} (pellets): juveniles and older</li>
 * </ul>
 */
public final class BettaFood {
    public enum Kind { FRY_START, FRY, LIVE, NORMAL }

    /** One normal food item feeds up to this many fish. */
    public static final int FISH_PER_FOOD = 4;

    private BettaFood() {}

    @Nullable
    public static Kind kind(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.is(ModTags.FRY_FOOD_START)) return Kind.FRY_START;
        if (stack.is(ModTags.FRY_FOOD)) return Kind.FRY;
        if (stack.is(ModTags.LIVE_FOOD)) return Kind.LIVE;
        if (stack.is(ModTags.BOTTOM_FOOD)) return Kind.NORMAL; // M19: algae wafers (only the bottom fish eat them)
        if (stack.is(ModTags.BETTA_FOOD)) return Kind.NORMAL;
        return null;
    }

    public static boolean isFryFood(Kind k) {
        return k == Kind.FRY_START || k == Kind.FRY;
    }

    /** @param stageProgress 0..1 progress within the current stage */
    public static boolean canEat(LifeStage stage, double stageProgress, Kind kind) {
        if (stage == LifeStage.FRY) {
            return kind == Kind.FRY_START || (kind == Kind.FRY && stageProgress >= 0.5);
        }
        if (!stage.isEntity()) return false;
        return kind == Kind.LIVE || kind == Kind.NORMAL;
    }

    public static int fishPerItem(Kind kind) {
        return isFryFood(kind) ? ServerConfig.getOr(ServerConfig.FOOD_FRY_PER_ITEM, 20) : FISH_PER_FOOD;
    }
}
