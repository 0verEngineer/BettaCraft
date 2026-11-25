package org.overengineer.bettacraft.registry;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import org.overengineer.bettacraft.platform.registry.DeferredRegister;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.equipment.FeederMenu;
import org.overengineer.bettacraft.tank.menu.TankMenu;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BettaCraft.MOD_ID);

    public static final Supplier<MenuType<TankMenu>> TANK = MENUS.register("tank", () -> org.overengineer.bettacraft.platform.Platform.get().menuType(TankMenu::new));
    public static final Supplier<MenuType<FeederMenu>> FEEDER = MENUS.register("feeder", () -> org.overengineer.bettacraft.platform.Platform.get().menuType(FeederMenu::new));
    /** M66: the clip-on feeder's menu (two slots). */
    public static final Supplier<MenuType<FeederMenu>> CLIP_FEEDER = MENUS.register("clip_feeder", () -> org.overengineer.bettacraft.platform.Platform.get().menuType(FeederMenu::clip));

    public static final Supplier<MenuType<org.overengineer.bettacraft.equipment.ExternalFilterMenu>> EXTERNAL_FILTER = MENUS.register("external_filter",
            () -> org.overengineer.bettacraft.platform.Platform.get().menuType(org.overengineer.bettacraft.equipment.ExternalFilterMenu::new));
    public static final Supplier<MenuType<org.overengineer.bettacraft.equipment.CabinetMenu>> CABINET = MENUS.register("cabinet",
            () -> org.overengineer.bettacraft.platform.Platform.get().menuType(org.overengineer.bettacraft.equipment.CabinetMenu::new));

    /** M24: the crucible. */
    public static final Supplier<MenuType<org.overengineer.bettacraft.crucible.CrucibleMenu>> CRUCIBLE = MENUS.register("crucible",
            () -> org.overengineer.bettacraft.platform.Platform.get().menuType(org.overengineer.bettacraft.crucible.CrucibleMenu::new));

    /** M25 part c: the dosing pump. */
    public static final Supplier<MenuType<org.overengineer.bettacraft.reef.DosingPumpMenu>> DOSING_PUMP = MENUS.register("dosing_pump",
            () -> org.overengineer.bettacraft.platform.Platform.get().menuType(org.overengineer.bettacraft.reef.DosingPumpMenu::new));

    /** M26: the live food pump. */
    public static final Supplier<MenuType<org.overengineer.bettacraft.food.LiveFoodPumpMenu>> LIVE_FOOD_PUMP = MENUS.register("live_food_pump",
            () -> org.overengineer.bettacraft.platform.Platform.get().menuType(org.overengineer.bettacraft.food.LiveFoodPumpMenu::new));

    /** M26 part b: the food maker. */
    public static final Supplier<MenuType<org.overengineer.bettacraft.food.FoodMakerMenu>> FOOD_MAKER = MENUS.register("food_maker",
            () -> org.overengineer.bettacraft.platform.Platform.get().menuType(org.overengineer.bettacraft.food.FoodMakerMenu::new));

    /** M37: the fertilizer maker. */
    public static final Supplier<MenuType<org.overengineer.bettacraft.fertilizer.FertilizerMakerMenu>> FERTILIZER_MAKER = MENUS.register("fertilizer_maker",
            () -> org.overengineer.bettacraft.platform.Platform.get().menuType(org.overengineer.bettacraft.fertilizer.FertilizerMakerMenu::new));

    public static void register() {
        MENUS.register();
    }

    /** M49 part a: the chambers of the sump and the calcium reactor. */
    public static final Supplier<MenuType<org.overengineer.bettacraft.equipment.SumpMenu>> SUMP = MENUS.register("sump",
            () -> org.overengineer.bettacraft.platform.Platform.get().menuType(org.overengineer.bettacraft.equipment.SumpMenu::new));
    public static final Supplier<MenuType<org.overengineer.bettacraft.reef.CalciumReactorMenu>> CALCIUM_REACTOR = MENUS.register("calcium_reactor",
            () -> org.overengineer.bettacraft.platform.Platform.get().menuType(org.overengineer.bettacraft.reef.CalciumReactorMenu::new));

    private ModMenus() {}
}
