package org.overengineer.bettacraft.registry;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.overengineer.bettacraft.platform.registry.DeferredRegister;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.tank.TankRecipe;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, BettaCraft.MOD_ID);

    public static final Supplier<RecipeSerializer<TankRecipe>> TANK = SERIALIZERS.register("tank", TankRecipe.Serializer::new);

    /** M11: textured vanilla-glass tank to clean tank and back. */
    public static final Supplier<RecipeSerializer<org.overengineer.bettacraft.tank.TankStyleRecipe>> TANK_STYLE = SERIALIZERS.register("tank_style",
            org.overengineer.bettacraft.tank.TankStyleRecipe.Serializer::new);

    public static final Supplier<RecipeSerializer<org.overengineer.bettacraft.equipment.CabinetRecipe>> CABINET = SERIALIZERS.register("cabinet",
            org.overengineer.bettacraft.equipment.CabinetRecipe.Serializer::new);

    /** M21.7: the cabinet of any stone (with the slabs of the same stone). */
    public static final Supplier<RecipeSerializer<org.overengineer.bettacraft.equipment.StoneCabinetRecipe>> STONE_CABINET = SERIALIZERS.register("stone_cabinet",
            org.overengineer.bettacraft.equipment.StoneCabinetRecipe.Serializer::new);

    // M24: the crucible (stainless steel)
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, BettaCraft.MOD_ID);
    public static final Supplier<net.minecraft.world.item.crafting.RecipeType<org.overengineer.bettacraft.crucible.CrucibleRecipe>> CRUCIBLE_TYPE =
            TYPES.register("crucible", () -> simpleType("crucible"));
    public static final Supplier<RecipeSerializer<org.overengineer.bettacraft.crucible.CrucibleRecipe>> CRUCIBLE = SERIALIZERS.register("crucible",
            org.overengineer.bettacraft.crucible.CrucibleRecipe.Serializer::new);

    // M26 part b: the food maker
    public static final Supplier<net.minecraft.world.item.crafting.RecipeType<org.overengineer.bettacraft.food.FoodMakerRecipe>> FOOD_MAKER_TYPE =
            TYPES.register("food_maker", () -> simpleType("food_maker"));
    public static final Supplier<RecipeSerializer<org.overengineer.bettacraft.food.FoodMakerRecipe>> FOOD_MAKER = SERIALIZERS.register("food_maker",
            org.overengineer.bettacraft.food.FoodMakerRecipe.Serializer::new);

    public static void register() {
        SERIALIZERS.register();
        TYPES.register();
    }

    /** M52: a recipe type that is just its id (NeoForge's RecipeType.simple); registered by TYPES. */
    private static <T extends net.minecraft.world.item.crafting.Recipe<?>> net.minecraft.world.item.crafting.RecipeType<T> simpleType(String path) {
        String name = BettaCraft.id(path).toString();
        return new net.minecraft.world.item.crafting.RecipeType<>() {
            @Override
            public String toString() {
                return name;
            }
        };
    }

    private ModRecipes() {}
}
