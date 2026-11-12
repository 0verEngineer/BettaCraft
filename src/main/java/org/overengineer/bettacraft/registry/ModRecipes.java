package org.overengineer.bettacraft.registry;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.tank.TankRecipe;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, BettaCraft.MOD_ID);

    public static final Supplier<RecipeSerializer<TankRecipe>> TANK = SERIALIZERS.register("tank", TankRecipe.Serializer::new);

    public static final Supplier<RecipeSerializer<org.overengineer.bettacraft.equipment.CabinetRecipe>> CABINET = SERIALIZERS.register("cabinet",
            org.overengineer.bettacraft.equipment.CabinetRecipe.Serializer::new);

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
    }

    private ModRecipes() {}
}
