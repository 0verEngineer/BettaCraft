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
    public static final DeferredItem<BlockItem> CLIP_FEEDER = ITEMS.registerSimpleBlockItem(ModBlocks.CLIP_FEEDER); // M21.5
    /** M18: in 16 colours (block state component, see TankLightItem). */
    public static final DeferredItem<org.overengineer.bettacraft.equipment.TankLightItem> TANK_LIGHT = ITEMS.register("tank_light",
            () -> new org.overengineer.bettacraft.equipment.TankLightItem(ModBlocks.TANK_LIGHT.get(), new Item.Properties()));

    /** M11: makes male bettas flare (optional-features #2). */
    public static final DeferredItem<org.overengineer.bettacraft.equipment.MirrorStickItem> MIRROR_STICK = ITEMS.register("mirror_stick",
            () -> new org.overengineer.bettacraft.equipment.MirrorStickItem(new Item.Properties().stacksTo(1)));

    /** M12: takes floating plants off the water surface of a tank (sneaking: thins the tank out by half). */
    public static final DeferredItem<org.overengineer.bettacraft.tank.plants.AquariumNetItem> AQUARIUM_NET = ITEMS.register("tank_net",
            () -> new org.overengineer.bettacraft.tank.plants.AquariumNetItem(new Item.Properties().durability(250)));

    /** M14: cleans algae off the glass of a tank (sneaking: the whole side). */
    public static final DeferredItem<org.overengineer.bettacraft.tank.algae.AlgaeScraperItem> ALGAE_SCRAPER = ITEMS.register("algae_scraper",
            () -> new org.overengineer.bettacraft.tank.algae.AlgaeScraperItem(new Item.Properties().durability(200)));
    /** M14: a dwarf clawed frog in a bucket of water (like the betta bucket). */
    public static final DeferredItem<net.minecraft.world.item.MobBucketItem> FROG_BUCKET = ITEMS.register("dwarf_frog_bucket",
            () -> new net.minecraft.world.item.MobBucketItem(ModEntities.DWARF_FROG.get(), net.minecraft.world.level.material.Fluids.WATER,
                    net.minecraft.sounds.SoundEvents.BUCKET_EMPTY_FISH, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<DeferredSpawnEggItem> FROG_SPAWN_EGG = ITEMS.register("dwarf_frog_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.DWARF_FROG, 0x6E6450, 0x3A3428, new Item.Properties()));

    /** M12: floating plants for the water surface of a tank (no blocks; the lily pad is the vanilla item). */
    public static final java.util.Map<org.overengineer.bettacraft.tank.plants.FloatingPlantType,
            DeferredItem<org.overengineer.bettacraft.tank.plants.FloatingPlantItem>> FLOATING_PLANTS = floatingPlants();

    private static java.util.Map<org.overengineer.bettacraft.tank.plants.FloatingPlantType,
            DeferredItem<org.overengineer.bettacraft.tank.plants.FloatingPlantItem>> floatingPlants() {
        var m = new java.util.EnumMap<org.overengineer.bettacraft.tank.plants.FloatingPlantType,
                DeferredItem<org.overengineer.bettacraft.tank.plants.FloatingPlantItem>>(org.overengineer.bettacraft.tank.plants.FloatingPlantType.class);
        for (var t : org.overengineer.bettacraft.tank.plants.FloatingPlantType.values()) {
            if (!t.grows()) continue;
            m.put(t, ITEMS.register(t.id(), () -> new org.overengineer.bettacraft.tank.plants.FloatingPlantItem(t, new Item.Properties())));
        }
        return java.util.Collections.unmodifiableMap(m);
    }

    /** M13: aquarium plants for the tank interior (cuttings; a plant taken out keeps its stage in a component). */
    public static final java.util.Map<org.overengineer.bettacraft.plants.PlantSpecies,
            DeferredItem<org.overengineer.bettacraft.plants.AquaticPlantItem>> AQUATIC_PLANTS = aquaticPlants();

    private static java.util.Map<org.overengineer.bettacraft.plants.PlantSpecies, DeferredItem<org.overengineer.bettacraft.plants.AquaticPlantItem>> aquaticPlants() {
        var m = new java.util.EnumMap<org.overengineer.bettacraft.plants.PlantSpecies, DeferredItem<org.overengineer.bettacraft.plants.AquaticPlantItem>>(
                org.overengineer.bettacraft.plants.PlantSpecies.class);
        for (var sp : org.overengineer.bettacraft.plants.PlantSpecies.values()) {
            m.put(sp, ITEMS.register(sp.id(), () -> new org.overengineer.bettacraft.plants.AquaticPlantItem(sp, new Item.Properties())));
        }
        return java.util.Collections.unmodifiableMap(m);
    }

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

    // ------------------------------------------------------------------ M19 tankmates

    /** A tankmate in a bucket of water, one per species (vanilla mob bucket; the fish overlay tinted per species). */
    public static final java.util.Map<org.overengineer.bettacraft.tankmates.TankmateSpecies,
            DeferredItem<org.overengineer.bettacraft.tankmates.TankmateBucketItem>> TANKMATE_BUCKETS = tankmateBuckets();
    public static final java.util.Map<org.overengineer.bettacraft.tankmates.TankmateSpecies, DeferredItem<DeferredSpawnEggItem>> TANKMATE_SPAWN_EGGS = tankmateEggs();

    private static java.util.Map<org.overengineer.bettacraft.tankmates.TankmateSpecies,
            DeferredItem<org.overengineer.bettacraft.tankmates.TankmateBucketItem>> tankmateBuckets() {
        var m = new java.util.EnumMap<org.overengineer.bettacraft.tankmates.TankmateSpecies,
                DeferredItem<org.overengineer.bettacraft.tankmates.TankmateBucketItem>>(org.overengineer.bettacraft.tankmates.TankmateSpecies.class);
        for (var sp : org.overengineer.bettacraft.tankmates.TankmateSpecies.values()) {
            m.put(sp, ITEMS.register(sp.bucketId(), () -> new org.overengineer.bettacraft.tankmates.TankmateBucketItem(sp, new Item.Properties().stacksTo(1))));
        }
        return java.util.Collections.unmodifiableMap(m);
    }

    private static java.util.Map<org.overengineer.bettacraft.tankmates.TankmateSpecies, DeferredItem<DeferredSpawnEggItem>> tankmateEggs() {
        var m = new java.util.EnumMap<org.overengineer.bettacraft.tankmates.TankmateSpecies, DeferredItem<DeferredSpawnEggItem>>(
                org.overengineer.bettacraft.tankmates.TankmateSpecies.class);
        for (var sp : org.overengineer.bettacraft.tankmates.TankmateSpecies.values()) {
            m.put(sp, ITEMS.register(sp.spawnEggId(), () -> new DeferredSpawnEggItem(ModEntities.TANKMATES.get(sp), sp.eggColor1(), sp.eggColor2(),
                    new Item.Properties())));
        }
        return java.util.Collections.unmodifiableMap(m);
    }

    /** M19: hangs inside the glass of a tank; young livebearers born into it are safe until they are juveniles. */
    public static final DeferredItem<org.overengineer.bettacraft.tankmates.BreedingBoxItem> BREEDING_BOX = ITEMS.register("breeding_box",
            () -> new org.overengineer.bettacraft.tankmates.BreedingBoxItem(new Item.Properties().stacksTo(16)));
    /** M19: fine powder food for the fry of every species (the whole fry stage). */
    public static final DeferredItem<Item> FRY_POWDER = ITEMS.registerSimpleItem("fry_powder");
    /** M19: sinking algae wafers: only the bottom fish (corydoras, plecos, otocinclus, loaches) and the small animals eat them. */
    public static final DeferredItem<Item> ALGAE_WAFER = ITEMS.registerSimpleItem("algae_wafer");
    /** M19: the blocks of the spawning cave and the spawning mop (decoration; also placeable one by one in the interior). */
    public static final DeferredItem<BlockItem> SPAWNING_CAVE = ITEMS.registerSimpleBlockItem(ModBlocks.SPAWNING_CAVE);
    public static final DeferredItem<BlockItem> SPAWNING_MOP = ITEMS.registerSimpleBlockItem(ModBlocks.SPAWNING_MOP);

    // ------------------------------------------------------------------ M20 reef tanks

    /** M20: sea water (from an ocean with an empty bucket, or water + sea salt); pours like water, salts a tank. */
    public static final DeferredItem<org.overengineer.bettacraft.reef.SeaWaterBucketItem> SEA_WATER_BUCKET = ITEMS.register("sea_water_bucket",
            () -> new org.overengineer.bettacraft.reef.SeaWaterBucketItem(new Item.Properties().stacksTo(1).craftRemainder(net.minecraft.world.item.Items.BUCKET)));
    /** M20: one portion salts a bucket of water to sea water (right-click a tank). */
    public static final DeferredItem<Item> SEA_SALT = ITEMS.register("sea_salt", () -> new org.overengineer.bettacraft.reef.ReefItem(new Item.Properties(), "sea_salt"));
    /** M20: raises the calcium of a reef tank for the stony corals (right-click a tank). */
    public static final DeferredItem<Item> CALCIUM_ADDITIVE = ITEMS.register("calcium_additive", () -> new org.overengineer.bettacraft.reef.ReefItem(new Item.Properties(), "calcium_additive"));
    /** M20: flow pump for the tank interior. */
    public static final DeferredItem<org.overengineer.bettacraft.reef.FlowPumpItem> FLOW_PUMP = ITEMS.register("flow_pump",
            () -> new org.overengineer.bettacraft.reef.FlowPumpItem(new Item.Properties()));
    public static final DeferredItem<BlockItem> PROTEIN_SKIMMER = ITEMS.registerSimpleBlockItem(ModBlocks.PROTEIN_SKIMMER);
    public static final DeferredItem<BlockItem> REEF_ROCK = ITEMS.registerSimpleBlockItem(ModBlocks.REEF_ROCK);
    public static final DeferredItem<BlockItem> LIVE_ROCK = ITEMS.registerSimpleBlockItem(ModBlocks.LIVE_ROCK);

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    private ModItems() {}
}
