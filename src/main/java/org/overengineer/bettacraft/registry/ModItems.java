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
import org.overengineer.bettacraft.food.ArtemiaCystsItem;
import org.overengineer.bettacraft.food.CultureItem;
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
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> SALT_WATER_BOTTLE = ITEMS.register("salt_water_bottle",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> BETTA_PELLETS = ITEMS.registerSimpleItem("betta_pellets");
    public static final DeferredItem<Item> MOSQUITO_LARVAE = ITEMS.registerSimpleItem("mosquito_larvae");

    // fry food (M2)
    public static final DeferredItem<Item> INFUSORIA = ITEMS.register("infusoria",
            () -> new Item(new Item.Properties().stacksTo(16).craftRemainder(Items.GLASS_BOTTLE)));
    public static final DeferredItem<Item> ARTEMIA_NAUPLII = ITEMS.register("artemia_nauplii",
            () -> new Item(new Item.Properties().stacksTo(16).craftRemainder(Items.GLASS_BOTTLE)));
    public static final DeferredItem<CultureItem> INFUSORIA_CULTURE = ITEMS.register("infusoria_culture",
            () -> new CultureItem(INFUSORIA, false, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<CultureItem> ARTEMIA_CULTURE = ITEMS.register("artemia_culture",
            () -> new CultureItem(ARTEMIA_NAUPLII, true, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<ArtemiaCystsItem> ARTEMIA_CYSTS = ITEMS.register("artemia_cysts",
            () -> new ArtemiaCystsItem(new Item.Properties()));

    public static final DeferredItem<HeaterItem> HEATER_SMALL = ITEMS.register("heater_small",
            () -> new HeaterItem(16, org.overengineer.bettacraft.equipment.SegmentBlock.Size.SMALL, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<HeaterItem> HEATER_MEDIUM = ITEMS.register("heater_medium",
            () -> new HeaterItem(32, org.overengineer.bettacraft.equipment.SegmentBlock.Size.MEDIUM, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<HeaterItem> HEATER_LARGE = ITEMS.register("heater_large",
            () -> new HeaterItem(64, org.overengineer.bettacraft.equipment.SegmentBlock.Size.LARGE, new Item.Properties().stacksTo(16)));

    /** Coolers (M9): the counterpart of the heaters, same sizes. */
    public static final DeferredItem<HeaterItem> COOLER_SMALL = ITEMS.register("cooler_small",
            () -> new HeaterItem(16, org.overengineer.bettacraft.equipment.SegmentBlock.Size.SMALL, true, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<HeaterItem> COOLER_MEDIUM = ITEMS.register("cooler_medium",
            () -> new HeaterItem(32, org.overengineer.bettacraft.equipment.SegmentBlock.Size.MEDIUM, true, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<HeaterItem> COOLER_LARGE = ITEMS.register("cooler_large",
            () -> new HeaterItem(64, org.overengineer.bettacraft.equipment.SegmentBlock.Size.LARGE, true, new Item.Properties().stacksTo(16)));

    /** Lid (M9) and the live food breeders (M9). */
    public static final DeferredItem<BlockItem> TANK_LID = ITEMS.registerSimpleBlockItem(ModBlocks.TANK_LID);
    public static final DeferredItem<BlockItem> LARVA_TUB = ITEMS.registerSimpleBlockItem(ModBlocks.LARVA_TUB);
    public static final DeferredItem<BlockItem> ARTEMIA_HATCHERY = ITEMS.registerSimpleBlockItem(ModBlocks.ARTEMIA_HATCHERY);

    /** Internal filter, placed into the tank interior (PLAN 6.2 / 7.3). */
    public static final DeferredItem<org.overengineer.bettacraft.equipment.InternalFilterItem> INTERNAL_FILTER = ITEMS.register("internal_filter",
            () -> new org.overengineer.bettacraft.equipment.InternalFilterItem(new Item.Properties().stacksTo(16)));

    /** External filter (M4) and aquarium cabinet of any planks (M4). */
    public static final DeferredItem<org.overengineer.bettacraft.equipment.ExternalFilterItem> EXTERNAL_FILTER = ITEMS.register("external_filter",
            () -> new org.overengineer.bettacraft.equipment.ExternalFilterItem(ModBlocks.EXTERNAL_FILTER.get(), new Item.Properties().stacksTo(16)));
    public static final DeferredItem<org.overengineer.bettacraft.equipment.CabinetBlockItem> CABINET = ITEMS.register("cabinet",
            () -> new org.overengineer.bettacraft.equipment.CabinetBlockItem(ModBlocks.CABINET.get(), new Item.Properties()));

    /** Decoration structure items (PLAN 7.3), one per {@link org.overengineer.bettacraft.decor.DecorType}. */
    public static final java.util.Map<org.overengineer.bettacraft.decor.DecorType, DeferredItem<org.overengineer.bettacraft.decor.DecorItem>> DECOR = decor();

    private static java.util.Map<org.overengineer.bettacraft.decor.DecorType, DeferredItem<org.overengineer.bettacraft.decor.DecorItem>> decor() {
        var m = new java.util.EnumMap<org.overengineer.bettacraft.decor.DecorType, DeferredItem<org.overengineer.bettacraft.decor.DecorItem>>(
                org.overengineer.bettacraft.decor.DecorType.class);
        for (var t : org.overengineer.bettacraft.decor.DecorType.values()) {
            m.put(t, ITEMS.register(t.itemId(), () -> new org.overengineer.bettacraft.decor.DecorItem(t, new Item.Properties().stacksTo(16))));
        }
        return java.util.Collections.unmodifiableMap(m);
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    private ModItems() {}
}
