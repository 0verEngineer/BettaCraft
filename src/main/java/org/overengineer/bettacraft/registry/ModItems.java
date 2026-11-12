package org.overengineer.bettacraft.registry;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.tank.TankBlockItem;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BettaCraft.MOD_ID);

    public static final DeferredItem<TankBlockItem> TANK = ITEMS.register("tank",
            () -> new TankBlockItem(ModBlocks.TANK.get(), new Item.Properties()));

    // M0 placeholders: icons from the asset pipeline, function follows in M1/M2.
    public static final DeferredItem<Item> BETTA_BUCKET = ITEMS.registerSimpleItem("betta_bucket", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> FISH_BAG = ITEMS.registerSimpleItem("fish_bag", new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> BETTA_PELLETS = ITEMS.registerSimpleItem("betta_pellets");
    public static final DeferredItem<Item> MOSQUITO_LARVAE = ITEMS.registerSimpleItem("mosquito_larvae");
    public static final DeferredItem<Item> HEATER_SMALL = ITEMS.registerSimpleItem("heater_small");
    public static final DeferredItem<Item> HEATER_MEDIUM = ITEMS.registerSimpleItem("heater_medium");
    public static final DeferredItem<Item> HEATER_LARGE = ITEMS.registerSimpleItem("heater_large");

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    private ModItems() {}
}
