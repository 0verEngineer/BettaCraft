package org.overengineer.bettacraft.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.equipment.HeaterItem;
import org.overengineer.bettacraft.tank.TankBlockItem;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BettaCraft.MOD_ID);

    public static final DeferredItem<TankBlockItem> TANK = ITEMS.register("tank",
            () -> new TankBlockItem(ModBlocks.TANK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> FEEDER = ITEMS.registerSimpleBlockItem(ModBlocks.FEEDER);
    public static final DeferredItem<BlockItem> TANK_LIGHT = ITEMS.registerSimpleBlockItem(ModBlocks.TANK_LIGHT);

    public static final DeferredItem<BettaBucketItem> BETTA_BUCKET = ITEMS.register("betta_bucket",
            () -> new BettaBucketItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<FishBagItem> FISH_BAG = ITEMS.register("fish_bag",
            () -> new FishBagItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<DeferredSpawnEggItem> BETTA_SPAWN_EGG = ITEMS.register("betta_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.BETTA, 0x6B6A3A, 0x13817F, new Item.Properties()));

    public static final DeferredItem<Item> SWAMP_WATER_BOTTLE = ITEMS.register("swamp_water_bottle",
            () -> new Item(new Item.Properties().stacksTo(16).craftRemainder(Items.GLASS_BOTTLE)));
    public static final DeferredItem<Item> BETTA_PELLETS = ITEMS.registerSimpleItem("betta_pellets");
    public static final DeferredItem<Item> MOSQUITO_LARVAE = ITEMS.registerSimpleItem("mosquito_larvae");

    public static final DeferredItem<HeaterItem> HEATER_SMALL = ITEMS.register("heater_small",
            () -> new HeaterItem(16, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<HeaterItem> HEATER_MEDIUM = ITEMS.register("heater_medium",
            () -> new HeaterItem(32, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<HeaterItem> HEATER_LARGE = ITEMS.register("heater_large",
            () -> new HeaterItem(64, new Item.Properties().stacksTo(16)));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    private ModItems() {}
}
