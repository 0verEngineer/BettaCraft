package org.overengineer.bettacraft.registry;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.equipment.FeederMenu;
import org.overengineer.bettacraft.tank.menu.TankMenu;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BettaCraft.MOD_ID);

    public static final Supplier<MenuType<TankMenu>> TANK = MENUS.register("tank", () -> IMenuTypeExtension.create(TankMenu::new));
    public static final Supplier<MenuType<FeederMenu>> FEEDER = MENUS.register("feeder", () -> IMenuTypeExtension.create(FeederMenu::new));

    public static final Supplier<MenuType<org.overengineer.bettacraft.equipment.ExternalFilterMenu>> EXTERNAL_FILTER = MENUS.register("external_filter",
            () -> IMenuTypeExtension.create(org.overengineer.bettacraft.equipment.ExternalFilterMenu::new));
    public static final Supplier<MenuType<org.overengineer.bettacraft.equipment.CabinetMenu>> CABINET = MENUS.register("cabinet",
            () -> IMenuTypeExtension.create(org.overengineer.bettacraft.equipment.CabinetMenu::new));

    /** M24: the crucible. */
    public static final Supplier<MenuType<org.overengineer.bettacraft.crucible.CrucibleMenu>> CRUCIBLE = MENUS.register("crucible",
            () -> IMenuTypeExtension.create(org.overengineer.bettacraft.crucible.CrucibleMenu::new));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }

    private ModMenus() {}
}
