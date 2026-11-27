package org.overengineer.bettacraft.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.overengineer.bettacraft.platform.registry.DeferredItem;
import org.overengineer.bettacraft.platform.registry.DeferredRegister;
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
            () -> new org.overengineer.bettacraft.containers.AnimalBucketItem(ModEntities.DWARF_FROG.get(), net.minecraft.world.level.material.Fluids.WATER,
                    net.minecraft.sounds.SoundEvents.BUCKET_EMPTY_FISH, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> FROG_SPAWN_EGG = ITEMS.register("dwarf_frog_spawn_egg",
            () -> org.overengineer.bettacraft.platform.Platform.get().spawnEgg(ModEntities.DWARF_FROG, 0x6E6450, 0x3A3428, new Item.Properties()));
    /** M31: a musk turtle in a bucket of water. */
    public static final DeferredItem<net.minecraft.world.item.MobBucketItem> MUSK_TURTLE_BUCKET = ITEMS.register("musk_turtle_bucket",
            () -> new org.overengineer.bettacraft.terrarium.TurtleBucketItem(ModEntities.MUSK_TURTLE.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> MUSK_TURTLE_SPAWN_EGG = ITEMS.register("musk_turtle_spawn_egg",
            () -> org.overengineer.bettacraft.platform.Platform.get().spawnEgg(ModEntities.MUSK_TURTLE, 0x3E3526, 0xC8B878, new Item.Properties()));
    /** M31 part b: the other turtles: buckets of water for the water turtles, a terrarium box for the tortoise. */
    public static final DeferredItem<net.minecraft.world.item.MobBucketItem> SLIDER_TURTLE_BUCKET = ITEMS.register("slider_turtle_bucket",
            () -> new org.overengineer.bettacraft.terrarium.TurtleBucketItem(ModEntities.SLIDER_TURTLE.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<net.minecraft.world.item.MobBucketItem> REEVES_TURTLE_BUCKET = ITEMS.register("reeves_turtle_bucket",
            () -> new org.overengineer.bettacraft.terrarium.TurtleBucketItem(ModEntities.REEVES_TURTLE.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<net.minecraft.world.item.MobBucketItem> HERMANN_TORTOISE_BOX = ITEMS.register("hermann_tortoise_box",
            () -> new org.overengineer.bettacraft.terrarium.AnimalBoxItem(ModEntities.HERMANN_TORTOISE.get(), new Item.Properties().stacksTo(1)));
    /** M31 part b: the empty terrarium box (catches a tortoise; later the land animals of part c). M35: also holds a land animal of another mod. */
    public static final DeferredItem<org.overengineer.bettacraft.terrarium.TerrariumBoxItem> TERRARIUM_BOX = ITEMS.register("terrarium_box",
            () -> new org.overengineer.bettacraft.terrarium.TerrariumBoxItem(new Item.Properties().stacksTo(16)));
    /** M31 part d: the mob jar ("Mobglas"): catches a weakened mob alive for a tank. */
    public static final DeferredItem<org.overengineer.bettacraft.mobs.MobJarItem> MOB_JAR = ITEMS.register("mob_jar",
            () -> new org.overengineer.bettacraft.mobs.MobJarItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> SLIDER_TURTLE_SPAWN_EGG = ITEMS.register("slider_turtle_spawn_egg",
            () -> org.overengineer.bettacraft.platform.Platform.get().spawnEgg(ModEntities.SLIDER_TURTLE, 0x3E5A2A, 0xC83A2A, new Item.Properties()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> HERMANN_TORTOISE_SPAWN_EGG = ITEMS.register("hermann_tortoise_spawn_egg",
            () -> org.overengineer.bettacraft.platform.Platform.get().spawnEgg(ModEntities.HERMANN_TORTOISE, 0xC8A050, 0x3A2A18, new Item.Properties()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> REEVES_TURTLE_SPAWN_EGG = ITEMS.register("reeves_turtle_spawn_egg",
            () -> org.overengineer.bettacraft.platform.Platform.get().spawnEgg(ModEntities.REEVES_TURTLE, 0x5A4630, 0xD8C060, new Item.Properties()));

    /** M31 part c: the amphibians and reptiles in their terrarium boxes, and their spawn eggs. */
    public static final java.util.Map<org.overengineer.bettacraft.terrarium.HerpSpecies, DeferredItem<net.minecraft.world.item.MobBucketItem>> HERP_BOXES = herpBoxes();
    public static final java.util.Map<org.overengineer.bettacraft.terrarium.HerpSpecies, DeferredItem<net.minecraft.world.item.SpawnEggItem>> HERP_SPAWN_EGGS = herpEggs();

    private static java.util.Map<org.overengineer.bettacraft.terrarium.HerpSpecies, DeferredItem<net.minecraft.world.item.MobBucketItem>> herpBoxes() {
        var m = new java.util.LinkedHashMap<org.overengineer.bettacraft.terrarium.HerpSpecies, DeferredItem<net.minecraft.world.item.MobBucketItem>>();
        for (var s : ModEntities.HERPS.keySet()) {
            m.put(s, ITEMS.register(s.containerId(), () -> new org.overengineer.bettacraft.terrarium.AnimalBoxItem(ModEntities.HERPS.get(s).get(), new Item.Properties().stacksTo(1))));
        }
        return java.util.Collections.unmodifiableMap(m);
    }

    /** Spawn egg colours (base, spots) of the part c species. */
    private static int[] eggColours(org.overengineer.bettacraft.terrarium.HerpSpecies s) {
        return switch (s.id()) {
            case "dart_frog" -> new int[]{0x2050C8, 0x101418};
            case "mudskipper" -> new int[]{0x6E6A52, 0x4C90D8}; // M49
            case "horned_frog" -> new int[]{0x6E9A3A, 0x6A3A20};
            case "leopard_gecko" -> new int[]{0xE8C858, 0x3A3028};
            case "crested_gecko" -> new int[]{0xB8783A, 0xE0C08A};
            default -> new int[]{0xD8602A, 0x8A2A1A};
        };
    }

    private static java.util.Map<org.overengineer.bettacraft.terrarium.HerpSpecies, DeferredItem<net.minecraft.world.item.SpawnEggItem>> herpEggs() {
        var m = new java.util.LinkedHashMap<org.overengineer.bettacraft.terrarium.HerpSpecies, DeferredItem<net.minecraft.world.item.SpawnEggItem>>();
        for (var s : ModEntities.HERPS.keySet()) {
            int[] c = eggColours(s);
            m.put(s, ITEMS.register(s.spawnEggId(), () -> org.overengineer.bettacraft.platform.Platform.get().spawnEgg(ModEntities.HERPS.get(s), c[0], c[1], new Item.Properties())));
        }
        return java.util.Collections.unmodifiableMap(m);
    }

    /** M31 part c: feeder insects: crickets (a culture box in the grass) and flightless fruit flies (a culture on fruit). */
    public static final DeferredItem<Item> CRICKETS = ITEMS.register("crickets", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> FRUIT_FLIES = ITEMS.register("fruit_flies", () -> new Item(new Item.Properties()));
    public static final DeferredItem<CultureItem> CRICKET_CULTURE = ITEMS.register("cricket_culture",
            () -> new CultureItem(CRICKETS, false, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<CultureItem> FRUIT_FLY_CULTURE = ITEMS.register("fruit_fly_culture",
            () -> new CultureItem(FRUIT_FLIES, false, new Item.Properties().stacksTo(16)));

    /** M31 part b: the bucket or box item of a species (part c: the boxes of the amphibians and reptiles). */
    public static Item herpContainer(org.overengineer.bettacraft.terrarium.HerpSpecies s) {
        var box = HERP_BOXES.get(s);
        if (box != null) return box.get();
        if (s == org.overengineer.bettacraft.terrarium.HerpSpecies.SLIDER) return SLIDER_TURTLE_BUCKET.get();
        if (s == org.overengineer.bettacraft.terrarium.HerpSpecies.HERMANN) return HERMANN_TORTOISE_BOX.get();
        if (s == org.overengineer.bettacraft.terrarium.HerpSpecies.REEVES) return REEVES_TURTLE_BUCKET.get();
        return MUSK_TURTLE_BUCKET.get();
    }

    /** M12: floating plants for the water surface of a tank (no blocks; the lily pad is the vanilla item). */
    public static final java.util.Map<org.overengineer.bettacraft.tank.plants.FloatingPlantType,
            DeferredItem<org.overengineer.bettacraft.tank.plants.FloatingPlantItem>> FLOATING_PLANTS = floatingPlants();

    private static java.util.Map<org.overengineer.bettacraft.tank.plants.FloatingPlantType,
            DeferredItem<org.overengineer.bettacraft.tank.plants.FloatingPlantItem>> floatingPlants() {
        var m = new java.util.EnumMap<org.overengineer.bettacraft.tank.plants.FloatingPlantType,
                DeferredItem<org.overengineer.bettacraft.tank.plants.FloatingPlantItem>>(org.overengineer.bettacraft.tank.plants.FloatingPlantType.class);
        for (var t : org.overengineer.bettacraft.tank.plants.FloatingPlantType.values()) {
            if (!t.grows() || t.plantItem()) continue; // M28: floating Riccia is the aquarium plant item
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
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> BETTA_SPAWN_EGG = ITEMS.register("betta_spawn_egg",
            () -> org.overengineer.bettacraft.platform.Platform.get().spawnEgg(ModEntities.BETTA, 0x6B6A3A, 0x13817F, new Item.Properties()));

    // M22.1: bottles pour out anywhere (sneak + right-click, or right-click into the air), see ContainerRules
    public static final DeferredItem<Item> SWAMP_WATER_BOTTLE = ITEMS.register("swamp_water_bottle",
            () -> new org.overengineer.bettacraft.containers.PourableBottleItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> SALT_WATER_BOTTLE = ITEMS.register("salt_water_bottle",
            () -> new org.overengineer.bettacraft.containers.PourableBottleItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> BETTA_PELLETS = ITEMS.registerSimpleItem("betta_pellets");
    public static final DeferredItem<Item> MOSQUITO_LARVAE = ITEMS.registerSimpleItem("mosquito_larvae");

    // fry food (M2)
    public static final DeferredItem<Item> INFUSORIA = ITEMS.register("infusoria",
            () -> new org.overengineer.bettacraft.containers.PourableBottleItem(new Item.Properties().stacksTo(16).craftRemainder(Items.GLASS_BOTTLE)));
    public static final DeferredItem<Item> ARTEMIA_NAUPLII = ITEMS.register("artemia_nauplii",
            () -> new org.overengineer.bettacraft.containers.PourableBottleItem(new Item.Properties().stacksTo(16).craftRemainder(Items.GLASS_BOTTLE)));
    public static final DeferredItem<CultureItem> INFUSORIA_CULTURE = ITEMS.register("infusoria_culture",
            () -> new CultureItem(INFUSORIA, false, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<CultureItem> ARTEMIA_CULTURE = ITEMS.register("artemia_culture",
            () -> new CultureItem(ARTEMIA_NAUPLII, true, new Item.Properties().stacksTo(16)));
    // M48 part b: the food chain of the clownfish larvae (phytoplankton feeds the rotifers, the rotifers feed the larvae)
    public static final DeferredItem<Item> PHYTOPLANKTON = ITEMS.register("phytoplankton",
            () -> new org.overengineer.bettacraft.containers.PourableBottleItem(new Item.Properties().stacksTo(16).craftRemainder(Items.GLASS_BOTTLE)));
    public static final DeferredItem<Item> ROTIFERS = ITEMS.register("rotifers",
            () -> new org.overengineer.bettacraft.containers.PourableBottleItem(new Item.Properties().stacksTo(16).craftRemainder(Items.GLASS_BOTTLE)));
    // M49 part a: the sump, the Chaetomorpha of its refugium, the calcium reactor and its coral rubble
    public static final DeferredItem<org.overengineer.bettacraft.equipment.SumpItem> SUMP = ITEMS.register("sump",
            () -> new org.overengineer.bettacraft.equipment.SumpItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> CHAETOMORPHA = ITEMS.register("chaetomorpha", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CORAL_RUBBLE = ITEMS.register("coral_rubble", () -> new Item(new Item.Properties()));
    public static final DeferredItem<BlockItem> CALCIUM_REACTOR = ITEMS.registerSimpleBlockItem(ModBlocks.CALCIUM_REACTOR);
    public static final DeferredItem<CultureItem> PHYTOPLANKTON_CULTURE = ITEMS.register("phytoplankton_culture",
            () -> new CultureItem(PHYTOPLANKTON, true, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<CultureItem> ROTIFER_CULTURE = ITEMS.register("rotifer_culture",
            () -> new CultureItem(ROTIFERS, false, new Item.Properties().stacksTo(16)));
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

    /** A tankmate in a bucket of water, one per species (vanilla mob bucket; M55: the species' own sprite in the water). */
    public static final java.util.Map<org.overengineer.bettacraft.tankmates.TankmateSpecies,
            DeferredItem<org.overengineer.bettacraft.tankmates.TankmateBucketItem>> TANKMATE_BUCKETS = tankmateBuckets();
    public static final java.util.Map<org.overengineer.bettacraft.tankmates.TankmateSpecies, DeferredItem<net.minecraft.world.item.SpawnEggItem>> TANKMATE_SPAWN_EGGS = tankmateEggs();

    private static java.util.Map<org.overengineer.bettacraft.tankmates.TankmateSpecies,
            DeferredItem<org.overengineer.bettacraft.tankmates.TankmateBucketItem>> tankmateBuckets() {
        var m = new java.util.EnumMap<org.overengineer.bettacraft.tankmates.TankmateSpecies,
                DeferredItem<org.overengineer.bettacraft.tankmates.TankmateBucketItem>>(org.overengineer.bettacraft.tankmates.TankmateSpecies.class);
        for (var sp : org.overengineer.bettacraft.tankmates.TankmateSpecies.values()) {
            if (sp.isLarge()) continue; // M30: big animals do not go into a bucket (transport tub)
            m.put(sp, ITEMS.register(sp.bucketId(), () -> new org.overengineer.bettacraft.tankmates.TankmateBucketItem(sp, new Item.Properties().stacksTo(1))));
        }
        return java.util.Collections.unmodifiableMap(m);
    }

    private static java.util.Map<org.overengineer.bettacraft.tankmates.TankmateSpecies, DeferredItem<net.minecraft.world.item.SpawnEggItem>> tankmateEggs() {
        var m = new java.util.EnumMap<org.overengineer.bettacraft.tankmates.TankmateSpecies, DeferredItem<net.minecraft.world.item.SpawnEggItem>>(
                org.overengineer.bettacraft.tankmates.TankmateSpecies.class);
        for (var sp : org.overengineer.bettacraft.tankmates.TankmateSpecies.values()) {
            m.put(sp, ITEMS.register(sp.spawnEggId(), () -> org.overengineer.bettacraft.platform.Platform.get().spawnEgg(ModEntities.TANKMATES.get(sp), sp.eggColor1(), sp.eggColor2(),
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

    // ------------------------------------------------------------------ M22.3 wildlife

    /** M22.3: shrimp, crayfish and snails of one species in a plain bucket of water. */
    public static final DeferredItem<org.overengineer.bettacraft.containers.CritterBucketItem> CRITTER_BUCKET = ITEMS.register("critter_bucket",
            () -> new org.overengineer.bettacraft.containers.CritterBucketItem(new Item.Properties().stacksTo(1).craftRemainder(net.minecraft.world.item.Items.BUCKET)));
    /** M22.3: analyses the hidden genes of the animals in a tank (64 uses, repaired with glass panes). */
    public static final DeferredItem<org.overengineer.bettacraft.genetics.MagnifierItem> MAGNIFIER = ITEMS.register("magnifier",
            () -> new org.overengineer.bettacraft.genetics.MagnifierItem(new Item.Properties().durability(64)));
    /** M22.3: the wild corals as blocks (silk touch), for reefs of one's own. */
    public static final java.util.Map<org.overengineer.bettacraft.plants.PlantSpecies, DeferredItem<BlockItem>> WILD_CORALS = wildCoralItems();

    private static java.util.Map<org.overengineer.bettacraft.plants.PlantSpecies, DeferredItem<BlockItem>> wildCoralItems() {
        var m = new java.util.EnumMap<org.overengineer.bettacraft.plants.PlantSpecies, DeferredItem<BlockItem>>(org.overengineer.bettacraft.plants.PlantSpecies.class);
        for (var e : ModBlocks.WILD_CORALS.entrySet()) m.put(e.getKey(), ITEMS.registerSimpleBlockItem(e.getValue()));
        return java.util.Collections.unmodifiableMap(m);
    }

    // ------------------------------------------------------------------ M23 aeration, internal filters, LED tiers

    public static final DeferredItem<org.overengineer.bettacraft.equipment.InternalFilterItem> SPONGE_FILTER = ITEMS.register("sponge_filter",
            () -> new org.overengineer.bettacraft.equipment.InternalFilterItem(org.overengineer.bettacraft.equipment.SegmentBlock.Kind.SPONGE_FILTER, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<org.overengineer.bettacraft.equipment.InternalFilterItem> CORNER_FILTER = ITEMS.register("corner_filter",
            () -> new org.overengineer.bettacraft.equipment.InternalFilterItem(org.overengineer.bettacraft.equipment.SegmentBlock.Kind.CORNER_FILTER, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<org.overengineer.bettacraft.equipment.IntakeSpongeItem> INTAKE_SPONGE = ITEMS.register("intake_sponge",
            () -> new org.overengineer.bettacraft.equipment.IntakeSpongeItem(new Item.Properties()));
    public static final DeferredItem<org.overengineer.bettacraft.equipment.AirStoneItem> AIR_STONE = ITEMS.register("air_stone",
            () -> new org.overengineer.bettacraft.equipment.AirStoneItem(ModBlocks.AIR_STONE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> AIR_PUMP = ITEMS.registerSimpleBlockItem(ModBlocks.AIR_PUMP);
    /** M23: air hose (crafting part of the air stone and the sponge filter). */
    public static final DeferredItem<Item> AIR_HOSE = ITEMS.registerSimpleItem("air_hose");
    public static final DeferredItem<org.overengineer.bettacraft.equipment.TankLightItem> LED_PANEL = ITEMS.register("led_panel",
            () -> new org.overengineer.bettacraft.equipment.TankLightItem(ModBlocks.LED_PANEL.get(), new Item.Properties()));
    public static final DeferredItem<org.overengineer.bettacraft.equipment.TankLightItem> HIGH_POWER_LED = ITEMS.register("high_power_led",
            () -> new org.overengineer.bettacraft.equipment.TankLightItem(ModBlocks.HIGH_POWER_LED.get(), new Item.Properties()));

    // ------------------------------------------------------------------ M24 pipes, stainless steel, multiblock filter, water changes

    /** M24: stainless steel (#c:ingots/stainless_steel; steel of other mods is no stainless steel). */
    public static final DeferredItem<Item> STAINLESS_STEEL_INGOT = ITEMS.registerSimpleItem("stainless_steel_ingot");

    // ------------------------------------------------------------------ M30 catching tools for big animals
    /** M30: the large net (catches a wild big animal into an empty transport tub). */
    public static final DeferredItem<org.overengineer.bettacraft.big.LargeNetItem> LARGE_NET = ITEMS.register("large_net",
            () -> new org.overengineer.bettacraft.big.LargeNetItem(new Item.Properties().durability(120)));
    /** M30: the transport tub (one big animal; slows the player while carried). */
    public static final DeferredItem<org.overengineer.bettacraft.big.TransportTubItem> TRANSPORT_TUB = ITEMS.register("transport_tub",
            () -> new org.overengineer.bettacraft.big.TransportTubItem(new Item.Properties().stacksTo(1)));
    /** M30: the steel leader (fishing for big animals, in the other hand). */
    public static final DeferredItem<org.overengineer.bettacraft.big.SteelLeaderItem> STEEL_LEADER = ITEMS.register("steel_leader",
            () -> new org.overengineer.bettacraft.big.SteelLeaderItem(new Item.Properties().durability(32)));
    public static final DeferredItem<BlockItem> CRUCIBLE = ITEMS.registerSimpleBlockItem(ModBlocks.CRUCIBLE);
    /** M68: the two pipes for everything (sneak + right-click into a pipe: a bundle; in a tank interior: an interior pipe). */
    public static final DeferredItem<org.overengineer.bettacraft.plumbing.PipeItem> GLASS_PIPE = ITEMS.register("glass_pipe",
            () -> new org.overengineer.bettacraft.plumbing.PipeItem(ModBlocks.GLASS_PIPE.get(), false, new Item.Properties()));
    public static final DeferredItem<org.overengineer.bettacraft.plumbing.PipeItem> STAINLESS_STEEL_PIPE = ITEMS.register("stainless_steel_pipe",
            () -> new org.overengineer.bettacraft.plumbing.PipeItem(ModBlocks.STAINLESS_STEEL_PIPE.get(), true, new Item.Properties()));
    /** M68: the pipe wrench (closes or opens a side of a pipe). */
    public static final DeferredItem<org.overengineer.bettacraft.plumbing.PipeWrenchItem> PIPE_WRENCH = ITEMS.register("pipe_wrench",
            () -> new org.overengineer.bettacraft.plumbing.PipeWrenchItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<BlockItem> WATER_PUMP = ITEMS.registerSimpleBlockItem(ModBlocks.WATER_PUMP);
    public static final DeferredItem<BlockItem> RESERVOIR = ITEMS.registerSimpleBlockItem(ModBlocks.RESERVOIR);
    public static final DeferredItem<BlockItem> LEVEL_SENSOR = ITEMS.registerSimpleBlockItem(ModBlocks.LEVEL_SENSOR);
    public static final DeferredItem<BlockItem> WATER_CHANGE_CONTROLLER = ITEMS.registerSimpleBlockItem(ModBlocks.WATER_CHANGE_CONTROLLER);
    public static final DeferredItem<org.overengineer.bettacraft.plumbing.interior.PlumbingItem> BULKHEAD = plumbing(ModBlocks.BULKHEAD, org.overengineer.bettacraft.plumbing.interior.Plumb.PORT);
    public static final DeferredItem<org.overengineer.bettacraft.plumbing.interior.PlumbingItem> INTAKE_STRAINER = plumbing(ModBlocks.INTAKE_STRAINER, org.overengineer.bettacraft.plumbing.interior.Plumb.STRAINER);
    public static final DeferredItem<org.overengineer.bettacraft.plumbing.interior.PlumbingItem> LILY_PIPE = plumbing(ModBlocks.LILY_PIPE, org.overengineer.bettacraft.plumbing.interior.Plumb.LILY);
    public static final DeferredItem<org.overengineer.bettacraft.plumbing.interior.PlumbingItem> NOZZLE = plumbing(ModBlocks.NOZZLE, org.overengineer.bettacraft.plumbing.interior.Plumb.NOZZLE);
    public static final DeferredItem<org.overengineer.bettacraft.plumbing.interior.PlumbingItem> SPRAY_BAR = plumbing(ModBlocks.SPRAY_BAR, org.overengineer.bettacraft.plumbing.interior.Plumb.SPRAY_BAR);
    public static final DeferredItem<BlockItem> FILTER_CHAMBER = ITEMS.registerSimpleBlockItem(ModBlocks.FILTER_CHAMBER);
    public static final DeferredItem<BlockItem> FILTER_PUMP_HEAD = ITEMS.registerSimpleBlockItem(ModBlocks.FILTER_PUMP_HEAD);
    public static final DeferredItem<BlockItem> FILTER_INLET = ITEMS.registerSimpleBlockItem(ModBlocks.FILTER_INLET);
    public static final DeferredItem<BlockItem> FILTER_OUTLET = ITEMS.registerSimpleBlockItem(ModBlocks.FILTER_OUTLET);
    // M40: the guards (M68: the filter pipes, their T-piece and the water ports are gone, see register())
    /** M40 part c: the pipe cover tool (a block in the off hand draws a filter pipe as that block; not used up). */
    public static final DeferredItem<org.overengineer.bettacraft.plumbing.PipeCoverItem> PIPE_COVER = ITEMS.register("pipe_cover",
            () -> new org.overengineer.bettacraft.plumbing.PipeCoverItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<org.overengineer.bettacraft.plumbing.interior.PlumbingItem> INTAKE_GUARD = plumbing(ModBlocks.INTAKE_GUARD, org.overengineer.bettacraft.plumbing.interior.Plumb.INTAKE_GUARD);
    public static final DeferredItem<org.overengineer.bettacraft.plumbing.interior.PlumbingItem> OUTLET_GUARD = plumbing(ModBlocks.OUTLET_GUARD, org.overengineer.bettacraft.plumbing.interior.Plumb.OUTLET_GUARD);
    public static final DeferredItem<org.overengineer.bettacraft.filter.FilterMediaItem> FILTER_FOAM = media("filter_foam", org.overengineer.bettacraft.filter.FilterMedium.SPONGE);
    public static final DeferredItem<org.overengineer.bettacraft.filter.FilterMediaItem> CERAMIC_RINGS = media("ceramic_rings", org.overengineer.bettacraft.filter.FilterMedium.CERAMIC);
    public static final DeferredItem<org.overengineer.bettacraft.filter.FilterMediaItem> ACTIVATED_CARBON = media("activated_carbon", org.overengineer.bettacraft.filter.FilterMedium.CARBON);
    public static final DeferredItem<org.overengineer.bettacraft.filter.FilterMediaItem> FILTER_PEAT = media("filter_peat", org.overengineer.bettacraft.filter.FilterMedium.PEAT);

    // M25: water chemistry
    public static final DeferredItem<org.overengineer.bettacraft.chem.ChemItem> TDS_METER = chem("tds_meter", org.overengineer.bettacraft.chem.ChemItem.Kind.TDS_METER, new Item.Properties().stacksTo(1));
    public static final DeferredItem<org.overengineer.bettacraft.chem.ChemItem> PH_TEST_KIT = chem("ph_test_kit", org.overengineer.bettacraft.chem.ChemItem.Kind.PH_TEST, new Item.Properties().durability(50));
    public static final DeferredItem<org.overengineer.bettacraft.chem.ChemItem> REMINERALISING_SALT = chem("remineralising_salt", org.overengineer.bettacraft.chem.ChemItem.Kind.REMINERALISER, new Item.Properties());
    public static final DeferredItem<org.overengineer.bettacraft.chem.ChemItem> ALMOND_LEAF = chem("almond_leaf", org.overengineer.bettacraft.chem.ChemItem.Kind.ALMOND_LEAF, new Item.Properties());
    /** M28: alder cones: placed on the floor (leaf litter) or put into a tank like the almond leaves, with fewer tannins. */
    public static final DeferredItem<org.overengineer.bettacraft.chem.ChemItem> ALDER_CONE = chem("alder_cone", org.overengineer.bettacraft.chem.ChemItem.Kind.ALDER_CONE, new Item.Properties());

    // M25 part b: CO2 and reverse osmosis
    /** Yeast for the bio CO2 (brown mushroom and sugar in a bowl). */
    public static final DeferredItem<Item> YEAST = ITEMS.register("yeast", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CO2_SOLENOID = ITEMS.register("co2_solenoid", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RO_MEMBRANE = ITEMS.register("ro_membrane", () -> new Item(new Item.Properties().durability(100)));
    public static final DeferredItem<Item> RO_PREFILTER = ITEMS.register("ro_prefilter", () -> new Item(new Item.Properties().durability(100)));
    public static final DeferredItem<org.overengineer.bettacraft.osmosis.RoWaterBucketItem> RO_WATER_BUCKET = ITEMS.register("ro_water_bucket",
            () -> new org.overengineer.bettacraft.osmosis.RoWaterBucketItem(new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
    public static final DeferredItem<BlockItem> BIO_CO2 = ITEMS.registerSimpleBlockItem(ModBlocks.BIO_CO2);
    public static final DeferredItem<org.overengineer.bettacraft.co2.Co2CylinderItem> CO2_CYLINDER = ITEMS.register("co2_cylinder",
            () -> new org.overengineer.bettacraft.co2.Co2CylinderItem(ModBlocks.CO2_CYLINDER.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<BlockItem> CO2_STATION = ITEMS.registerSimpleBlockItem(ModBlocks.CO2_STATION);
    public static final DeferredItem<BlockItem> RO_UNIT = ITEMS.registerSimpleBlockItem(ModBlocks.RO_UNIT);

    // M31: terrarium devices
    public static final DeferredItem<BlockItem> HEAT_MAT = ITEMS.registerSimpleBlockItem(ModBlocks.HEAT_MAT);
    public static final DeferredItem<BlockItem> BASKING_LAMP = ITEMS.registerSimpleBlockItem(ModBlocks.BASKING_LAMP);
    public static final DeferredItem<BlockItem> MISTER = ITEMS.registerSimpleBlockItem(ModBlocks.MISTER);
    public static final DeferredItem<BlockItem> THERMO_HYGROMETER = ITEMS.registerSimpleBlockItem(ModBlocks.THERMO_HYGROMETER);

    // M25 part c: reef dosing
    public static final DeferredItem<BlockItem> DOSING_PUMP = ITEMS.registerSimpleBlockItem(ModBlocks.DOSING_PUMP);
    /** M25 part d: active soil (substrate for soft water). */
    public static final DeferredItem<BlockItem> ACTIVE_SOIL = ITEMS.registerSimpleBlockItem(ModBlocks.ACTIVE_SOIL);
    /** M47 part a: the breeding basin (workplace of the aquarist villager). */
    public static final DeferredItem<BlockItem> BREEDING_BASIN = ITEMS.registerSimpleBlockItem(ModBlocks.BREEDING_BASIN);
    public static final DeferredItem<org.overengineer.bettacraft.reef.DosingCanisterItem> DOSING_CANISTER = canister("dosing_canister", null);
    public static final DeferredItem<org.overengineer.bettacraft.reef.DosingCanisterItem> CALCIUM_CANISTER = canister("calcium_canister",
            org.overengineer.bettacraft.reef.ReefDosing.Additive.CALCIUM);
    public static final DeferredItem<org.overengineer.bettacraft.reef.DosingCanisterItem> ALKALINITY_CANISTER = canister("alkalinity_canister",
            org.overengineer.bettacraft.reef.ReefDosing.Additive.ALKALINITY);
    public static final DeferredItem<org.overengineer.bettacraft.reef.DosingCanisterItem> MAGNESIUM_CANISTER = canister("magnesium_canister",
            org.overengineer.bettacraft.reef.ReefDosing.Additive.MAGNESIUM);
    public static final DeferredItem<org.overengineer.bettacraft.reef.DosingCanisterItem> TRACE_CANISTER = canister("trace_canister",
            org.overengineer.bettacraft.reef.ReefDosing.Additive.TRACE);

    // M26 part a: live food cultures and the live food pump
    public static final DeferredItem<org.overengineer.bettacraft.food.LiveFoodItem> TUBIFEX = ITEMS.register("tubifex",
            () -> new org.overengineer.bettacraft.food.LiveFoodItem(new Item.Properties()));
    public static final DeferredItem<Item> DAPHNIA = ITEMS.registerSimpleItem("daphnia");
    /** Mulm washed out of dirty filter media: food for the tubifex dish. */
    public static final DeferredItem<Item> MULM = ITEMS.registerSimpleItem("mulm");
    public static final DeferredItem<Item> GREEN_WATER_BOTTLE = ITEMS.register("green_water_bottle",
            () -> new org.overengineer.bettacraft.containers.PourableBottleItem(new Item.Properties().stacksTo(16).craftRemainder(Items.GLASS_BOTTLE)));
    public static final DeferredItem<CultureItem> GREEN_WATER_CULTURE = ITEMS.register("green_water_culture",
            () -> new CultureItem(GREEN_WATER_BOTTLE, true, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<BlockItem> TUBIFEX_DISH = ITEMS.registerSimpleBlockItem(ModBlocks.TUBIFEX_DISH);
    public static final DeferredItem<BlockItem> DAPHNIA_BARREL = ITEMS.registerSimpleBlockItem(ModBlocks.DAPHNIA_BARREL);
    public static final DeferredItem<BlockItem> LIVE_FOOD_PUMP = ITEMS.registerSimpleBlockItem(ModBlocks.LIVE_FOOD_PUMP);

    // M26 part b: the food maker and its foods
    public static final DeferredItem<BlockItem> FOOD_MAKER = ITEMS.registerSimpleBlockItem(ModBlocks.FOOD_MAKER);
    public static final DeferredItem<BlockItem> MONITOR = ITEMS.registerSimpleBlockItem(ModBlocks.MONITOR); // M27
    /** Flakes: float at the surface (the midwater fish like them, the bottom fish hardly get any). */
    public static final DeferredItem<Item> FISH_FLAKES = ITEMS.registerSimpleItem("fish_flakes");
    /** Granules: sink slowly (the bottom fish get their share). */
    public static final DeferredItem<Item> FOOD_GRANULES = ITEMS.registerSimpleItem("food_granules");
    public static final DeferredItem<Item> SHRIMP_FOOD = ITEMS.registerSimpleItem("shrimp_food");
    public static final DeferredItem<Item> FROZEN_MOSQUITO_LARVAE = ITEMS.registerSimpleItem("frozen_mosquito_larvae");
    public static final DeferredItem<Item> FROZEN_ARTEMIA = ITEMS.registerSimpleItem("frozen_artemia");
    public static final DeferredItem<Item> FROZEN_DAPHNIA = ITEMS.registerSimpleItem("frozen_daphnia");
    /** M29 part b: hard food with crushed shells; wears down the teeth of the puffers (#bettacraft:hard_food). */
    public static final DeferredItem<Item> SHELL_FOOD = ITEMS.registerSimpleItem("shell_food");

    // M37: liquid fertilizer, its intermediates, the fertilizer maker and the canister of the dosing pump
    public static final DeferredItem<BlockItem> FERTILIZER_MAKER = ITEMS.registerSimpleBlockItem(ModBlocks.FERTILIZER_MAKER);
    /** Ground dried kelp with bone meal: the organic part of the fertilizer. */
    public static final DeferredItem<Item> KELP_MEAL = ITEMS.registerSimpleItem("kelp_meal");
    /** Bone meal, charcoal (potash) and iron: the mineral salts and the iron of the fertilizer. */
    public static final DeferredItem<Item> NUTRIENT_SALT = ITEMS.registerSimpleItem("nutrient_salt");
    public static final DeferredItem<org.overengineer.bettacraft.fertilizer.FertilizerBottleItem> LIQUID_FERTILIZER = ITEMS.register("liquid_fertilizer",
            () -> new org.overengineer.bettacraft.fertilizer.FertilizerBottleItem(new Item.Properties()));
    public static final DeferredItem<org.overengineer.bettacraft.reef.DosingCanisterItem> FERTILIZER_CANISTER = canister("fertilizer_canister",
            org.overengineer.bettacraft.reef.ReefDosing.Additive.FERTILIZER);

    /** M37: the crank: winds the spring of the food maker, the fertilizer maker and (with the power options) the filters and cabinets. */
    public static final DeferredItem<org.overengineer.bettacraft.winding.CrankItem> CRANK = ITEMS.register("crank",
            () -> new org.overengineer.bettacraft.winding.CrankItem(new Item.Properties().stacksTo(1)));

    private static DeferredItem<org.overengineer.bettacraft.reef.DosingCanisterItem> canister(String id, org.overengineer.bettacraft.reef.ReefDosing.Additive a) {
        return ITEMS.register(id, () -> new org.overengineer.bettacraft.reef.DosingCanisterItem(a, new Item.Properties().stacksTo(a == null ? 16 : 1)));
    }

    private static DeferredItem<org.overengineer.bettacraft.chem.ChemItem> chem(String id, org.overengineer.bettacraft.chem.ChemItem.Kind kind, Item.Properties props) {
        return ITEMS.register(id, () -> new org.overengineer.bettacraft.chem.ChemItem(kind, props));
    }

    private static DeferredItem<org.overengineer.bettacraft.plumbing.interior.PlumbingItem> plumbing(
            org.overengineer.bettacraft.platform.registry.DeferredBlock<org.overengineer.bettacraft.plumbing.interior.PlumbingBlock> block, org.overengineer.bettacraft.plumbing.interior.Plumb kind) {
        return ITEMS.register(block.getId().getPath(), () -> new org.overengineer.bettacraft.plumbing.interior.PlumbingItem(block.get(), kind, new Item.Properties()));
    }

    private static DeferredItem<org.overengineer.bettacraft.filter.FilterMediaItem> media(String id, org.overengineer.bettacraft.filter.FilterMedium m) {
        return ITEMS.register(id, () -> new org.overengineer.bettacraft.filter.FilterMediaItem(m, new Item.Properties()));
    }

    public static void register() {
        // M68: the items of the removed pipes become the two pipes wherever they lie (inventories, chests, other mods' storage):
        // every filter pipe, the T-piece and the filter's water ports a stainless steel pipe, the interior pipe a glass pipe (1:1)
        for (String old : java.util.List.of("filter_inlet_pipe", "filter_outlet_pipe", "filter_fresh_pipe", "filter_waste_pipe", "pipe_tee",
                "filter_fresh_port", "filter_waste_port"))
            ITEMS.addAlias(old, "stainless_steel_pipe");
        ITEMS.addAlias("interior_pipe", "glass_pipe");
        ITEMS.register();
    }

    private ModItems() {}
}
