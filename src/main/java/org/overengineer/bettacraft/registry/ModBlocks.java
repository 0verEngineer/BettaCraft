package org.overengineer.bettacraft.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.equipment.FeederBlock;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.interior.InteriorFloorBlock;
import org.overengineer.bettacraft.tank.interior.InteriorWallBlock;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BettaCraft.MOD_ID);

    public static final DeferredBlock<TankBlock> TANK = BLOCKS.register("tank", () -> new TankBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.NONE)
            .strength(0.3F) // like glass (hardness and blast resistance)
            .sound(SoundType.GLASS)
            .noOcclusion()
            .pushReaction(PushReaction.BLOCK)
            .isValidSpawn((s, l, p, e) -> false)
            .isRedstoneConductor((s, l, p) -> false)
            .isSuffocating((s, l, p) -> false)
            .isViewBlocking((s, l, p) -> false)));

    /** Automatic feeder on top of a tank. */
    public static final DeferredBlock<FeederBlock> FEEDER = BLOCKS.register("feeder", () -> new FeederBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(1.5F, 6.0F)
            .sound(SoundType.METAL)
            .noOcclusion()));

    /** M21.5: clip-on feeder on the side of the glass (top edge). */
    public static final DeferredBlock<org.overengineer.bettacraft.equipment.ClipFeederBlock> CLIP_FEEDER = BLOCKS.register("clip_feeder",
            () -> new org.overengineer.bettacraft.equipment.ClipFeederBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(1.0F, 6.0F).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.DESTROY)));

    /** Light bar for tanks: light level 15, switched off by redstone. */
    public static final DeferredBlock<TankLightBlock> TANK_LIGHT = BLOCKS.register("tank_light", () -> new TankLightBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(0.8F)
            .sound(SoundType.METAL)
            .noOcclusion()
            .lightLevel(TankLightBlock::lightLevel)
            .isRedstoneConductor((s, l, p) -> false)));

    /** Heater rod in a tank interior (PLAN 7.3), one block per segment. */
    public static final DeferredBlock<org.overengineer.bettacraft.equipment.SegmentBlock> HEATER_SEGMENT = BLOCKS.register("heater_segment",
            () -> new org.overengineer.bettacraft.equipment.SegmentBlock(org.overengineer.bettacraft.equipment.SegmentBlock.Kind.HEATER, () -> ModItems.HEATER_SMALL.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE).strength(0.3F).sound(SoundType.GLASS).noLootTable().lightLevel(s -> 3)
                    .pushReaction(PushReaction.BLOCK)));

    /** Cooler rod in a tank interior (M9), one block per segment. */
    public static final DeferredBlock<org.overengineer.bettacraft.equipment.SegmentBlock> COOLER_SEGMENT = BLOCKS.register("cooler_segment",
            () -> new org.overengineer.bettacraft.equipment.SegmentBlock(org.overengineer.bettacraft.equipment.SegmentBlock.Kind.COOLER, () -> ModItems.COOLER_SMALL.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.3F).sound(SoundType.GLASS).noLootTable().lightLevel(s -> 2)
                    .pushReaction(PushReaction.BLOCK)));

    /** Internal filter box in a tank interior (PLAN 7.3). */
    public static final DeferredBlock<org.overengineer.bettacraft.equipment.SegmentBlock> FILTER_SEGMENT = BLOCKS.register("filter_segment",
            () -> new org.overengineer.bettacraft.equipment.DeviceSegmentBlock(org.overengineer.bettacraft.equipment.SegmentBlock.Kind.FILTER, () -> ModItems.INTERNAL_FILTER.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY).strength(0.5F).sound(SoundType.METAL).noLootTable().pushReaction(PushReaction.BLOCK)));

    /** External filter next to a tank (PLAN 6.2), two heater slots. */
    public static final DeferredBlock<org.overengineer.bettacraft.equipment.ExternalFilterBlock> EXTERNAL_FILTER = BLOCKS.register("external_filter",
            () -> new org.overengineer.bettacraft.equipment.ExternalFilterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY).strength(1.0F, 6.0F).sound(SoundType.METAL).noOcclusion()
                    .isRedstoneConductor((s, l, p) -> false)));

    /** Aquarium cabinet of any planks (PLAN 6.5). */
    public static final DeferredBlock<org.overengineer.bettacraft.equipment.CabinetBlock> CABINET = BLOCKS.register("cabinet",
            () -> new org.overengineer.bettacraft.equipment.CabinetBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).ignitedByLava()));

    /** Glass lid on top of a tank (M9): stops evaporation from the block below. */
    public static final DeferredBlock<org.overengineer.bettacraft.equipment.TankLidBlock> TANK_LID = BLOCKS.register("tank_lid",
            () -> new org.overengineer.bettacraft.equipment.TankLidBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
                    .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)));

    /** Larva tub (M9): breeds mosquito larvae under the open sky. */
    public static final DeferredBlock<org.overengineer.bettacraft.food.CultureTubBlock> LARVA_TUB = BLOCKS.register("larva_tub",
            () -> new org.overengineer.bettacraft.food.CultureTubBlock(org.overengineer.bettacraft.food.CultureTubBlock.Kind.LARVAE, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));

    /** Artemia hatchery (M9): hatches artemia nauplii in salt water with light. */
    public static final DeferredBlock<org.overengineer.bettacraft.food.CultureTubBlock> ARTEMIA_HATCHERY = BLOCKS.register("artemia_hatchery",
            () -> new org.overengineer.bettacraft.food.CultureTubBlock(org.overengineer.bettacraft.food.CultureTubBlock.Kind.ARTEMIA, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY).strength(1.0F).sound(SoundType.GLASS).noOcclusion()
                    .isRedstoneConductor((s, l, p) -> false)));

    /** Floor of a tank interior; can carry a substrate look (PLAN 7.2). Unbreakable. */
    public static final DeferredBlock<InteriorFloorBlock> INTERIOR_FLOOR = BLOCKS.register("interior_floor", () -> new InteriorFloorBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .isValidSpawn((s, l, p, e) -> false)
                    .pushReaction(PushReaction.BLOCK)));

    /** Invisible, unbreakable wall bounding a tank interior. */
    public static final DeferredBlock<InteriorWallBlock> INTERIOR_WALL = BLOCKS.register("interior_wall", () -> new InteriorWallBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .noOcclusion()
                    .isValidSpawn((s, l, p, e) -> false)
                    .isRedstoneConductor((s, l, p) -> false)
                    .isSuffocating((s, l, p) -> false)
                    .isViewBlocking((s, l, p) -> false)
                    .pushReaction(PushReaction.BLOCK)));

    /** M13: aquarium plants of the tank interior, one block per species (parts and redness as states). */
    public static final java.util.Map<org.overengineer.bettacraft.plants.PlantSpecies,
            DeferredBlock<org.overengineer.bettacraft.plants.AquaticPlantBlock>> AQUATIC_PLANTS = aquaticPlants();

    private static java.util.Map<org.overengineer.bettacraft.plants.PlantSpecies, DeferredBlock<org.overengineer.bettacraft.plants.AquaticPlantBlock>> aquaticPlants() {
        var m = new java.util.EnumMap<org.overengineer.bettacraft.plants.PlantSpecies, DeferredBlock<org.overengineer.bettacraft.plants.AquaticPlantBlock>>(
                org.overengineer.bettacraft.plants.PlantSpecies.class);
        for (var sp : org.overengineer.bettacraft.plants.PlantSpecies.values()) {
            m.put(sp, BLOCKS.register(sp.id(), () -> new org.overengineer.bettacraft.plants.AquaticPlantBlock(sp, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT).strength(0.2F).sound(SoundType.WET_GRASS).noCollission().noOcclusion().noLootTable()
                    .isValidSpawn((s, l, p, e) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)
                    .isRedstoneConductor((s, l, p) -> false).pushReaction(PushReaction.DESTROY))));
        }
        return java.util.Collections.unmodifiableMap(m);
    }

    /** M19: fired clay of a spawning cave (decoration for the tank interior; plecos breed in it). */
    public static final DeferredBlock<net.minecraft.world.level.block.Block> SPAWNING_CAVE = BLOCKS.register("spawning_cave",
            () -> new net.minecraft.world.level.block.Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE)
                    .strength(0.8F).sound(SoundType.DECORATED_POT)));

    /** M19: wool strands of a spawning mop (tetras and rasboras spawn into it; the adults do not reach the eggs). */
    public static final DeferredBlock<org.overengineer.bettacraft.tankmates.SpawningMopBlock> SPAWNING_MOP = BLOCKS.register("spawning_mop",
            () -> new org.overengineer.bettacraft.tankmates.SpawningMopBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN)
                    .strength(0.2F).sound(SoundType.WOOL).noCollission().noOcclusion()
                    .isValidSpawn((s, l, p, e) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)
                    .isRedstoneConductor((s, l, p) -> false).pushReaction(PushReaction.DESTROY)));

    // ------------------------------------------------------------------ M20 reef tanks

    /** M20: flow pump in a tank interior (a small box with a jet like the internal filter): water movement for the corals. */
    public static final DeferredBlock<org.overengineer.bettacraft.equipment.SegmentBlock> PUMP_SEGMENT = BLOCKS.register("flow_pump_segment",
            () -> new org.overengineer.bettacraft.equipment.DeviceSegmentBlock(org.overengineer.bettacraft.equipment.SegmentBlock.Kind.PUMP, () -> ModItems.FLOW_PUMP.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK).strength(0.5F).sound(SoundType.METAL).noLootTable().pushReaction(PushReaction.BLOCK)));

    /** M20: protein skimmer hanging on a tank like the external filter: cleans salt water (no effect in fresh water). */
    public static final DeferredBlock<org.overengineer.bettacraft.reef.ProteinSkimmerBlock> PROTEIN_SKIMMER = BLOCKS.register("protein_skimmer",
            () -> new org.overengineer.bettacraft.reef.ProteinSkimmerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY).strength(1.0F, 6.0F).sound(SoundType.METAL).noOcclusion()
                    .isRedstoneConductor((s, l, p) -> false)));

    /** M20: porous reef rock (aragonite) for the rock work of a reef tank; corals are glued onto it. */
    public static final DeferredBlock<net.minecraft.world.level.block.Block> REEF_ROCK = BLOCKS.register("reef_rock",
            () -> new net.minecraft.world.level.block.Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(1.2F, 3.0F).sound(SoundType.CALCITE).requiresCorrectToolForDrops()));

    /** M20: live rock: reef rock with coralline algae and bacteria; in salt water it cleans like a filter. */
    public static final DeferredBlock<net.minecraft.world.level.block.Block> LIVE_ROCK = BLOCKS.register("live_rock",
            () -> new net.minecraft.world.level.block.Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_MAGENTA)
                    .strength(1.2F, 3.0F).sound(SoundType.CALCITE).requiresCorrectToolForDrops()));

    /** M22.3: the corals of the mod growing wild in warm ocean reefs (full size, waterlogged; shears cut a frag). */
    public static final java.util.Map<org.overengineer.bettacraft.plants.PlantSpecies,
            DeferredBlock<org.overengineer.bettacraft.reef.WildCoralBlock>> WILD_CORALS = wildCorals();

    private static java.util.Map<org.overengineer.bettacraft.plants.PlantSpecies, DeferredBlock<org.overengineer.bettacraft.reef.WildCoralBlock>> wildCorals() {
        var m = new java.util.EnumMap<org.overengineer.bettacraft.plants.PlantSpecies, DeferredBlock<org.overengineer.bettacraft.reef.WildCoralBlock>>(
                org.overengineer.bettacraft.plants.PlantSpecies.class);
        for (var sp : org.overengineer.bettacraft.plants.PlantSpecies.values()) {
            if (!sp.form().coral()) continue;
            m.put(sp, BLOCKS.register("wild_" + sp.id(), () -> new org.overengineer.bettacraft.reef.WildCoralBlock(sp, BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_PINK).noCollission().instabreak().randomTicks()
                    .sound(net.minecraft.world.level.block.SoundType.WET_GRASS).pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY))));
        }
        return java.util.Collections.unmodifiableMap(m);
    }

    /**
     * M28.1: the aquarium plants growing wild in the world, one block per growth form with the species as state (shears
     * cut a cutting, it grows back; no items).
     */
    public static final java.util.Map<org.overengineer.bettacraft.plants.wild.WildPlantKind,
            DeferredBlock<org.overengineer.bettacraft.plants.wild.WildPlantBlock>> WILD_PLANTS = wildPlants();

    private static java.util.Map<org.overengineer.bettacraft.plants.wild.WildPlantKind, DeferredBlock<org.overengineer.bettacraft.plants.wild.WildPlantBlock>> wildPlants() {
        var m = new java.util.EnumMap<org.overengineer.bettacraft.plants.wild.WildPlantKind, DeferredBlock<org.overengineer.bettacraft.plants.wild.WildPlantBlock>>(
                org.overengineer.bettacraft.plants.wild.WildPlantKind.class);
        for (var k : org.overengineer.bettacraft.plants.wild.WildPlantKind.values()) {
            m.put(k, BLOCKS.register(k.id(), () -> {
                var p = BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().instabreak().randomTicks()
                        .sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY).noLootTable();
                if (k != org.overengineer.bettacraft.plants.wild.WildPlantKind.MOSS && k != org.overengineer.bettacraft.plants.wild.WildPlantKind.CARPET) {
                    p = p.offsetType(BlockBehaviour.OffsetType.XZ);
                }
                return switch (k) {
                    case EPIPHYTE -> new org.overengineer.bettacraft.plants.wild.WildPlantBlock.Epiphyte(p);
                    case MOSS -> new org.overengineer.bettacraft.plants.wild.WildPlantBlock.Moss(p);
                    case ROSETTE -> new org.overengineer.bettacraft.plants.wild.WildPlantBlock.Rosette(p);
                    case STEM -> new org.overengineer.bettacraft.plants.wild.WildPlantBlock.Stem(p);
                    case CARPET -> new org.overengineer.bettacraft.plants.wild.WildPlantBlock.Carpet(p);
                };
            }));
        }
        return java.util.Collections.unmodifiableMap(m);
    }

    /** M28.1: a patch of a floating plant on calm water (like a lily pad; the type as state, no item). */
    public static final DeferredBlock<org.overengineer.bettacraft.plants.wild.WildFloatingPlantBlock> WILD_FLOATING_PLANT = BLOCKS.register(
            "wild_floating_plant", () -> new org.overengineer.bettacraft.plants.wild.WildFloatingPlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT).instabreak().sound(SoundType.LILY_PAD).noOcclusion().noLootTable().pushReaction(PushReaction.DESTROY)));

    // ------------------------------------------------------------------ M23 aeration, internal filters, LED tiers

    /** M23: air-driven sponge filter in a tank interior (needs an air pump). */
    public static final DeferredBlock<org.overengineer.bettacraft.equipment.DeviceSegmentBlock> SPONGE_FILTER_SEGMENT = BLOCKS.register("sponge_filter_segment",
            () -> new org.overengineer.bettacraft.equipment.DeviceSegmentBlock(org.overengineer.bettacraft.equipment.SegmentBlock.Kind.SPONGE_FILTER, () -> ModItems.SPONGE_FILTER.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK).strength(0.4F).sound(SoundType.WOOL).noLootTable().pushReaction(PushReaction.BLOCK)));

    /** M23: motor corner filter in a tank interior. */
    public static final DeferredBlock<org.overengineer.bettacraft.equipment.DeviceSegmentBlock> CORNER_FILTER_SEGMENT = BLOCKS.register("corner_filter_segment",
            () -> new org.overengineer.bettacraft.equipment.DeviceSegmentBlock(org.overengineer.bettacraft.equipment.SegmentBlock.Kind.CORNER_FILTER, () -> ModItems.CORNER_FILTER.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY).strength(0.5F).sound(SoundType.METAL).noLootTable().pushReaction(PushReaction.BLOCK)));

    /** M23: air stone in a tank interior. */
    public static final DeferredBlock<org.overengineer.bettacraft.equipment.AirStoneBlock> AIR_STONE = BLOCKS.register("air_stone",
            () -> new org.overengineer.bettacraft.equipment.AirStoneBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE).strength(0.3F).sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.DESTROY)));

    /** M23: air pump next to a tank. */
    public static final DeferredBlock<org.overengineer.bettacraft.equipment.AirPumpBlock> AIR_PUMP = BLOCKS.register("air_pump",
            () -> new org.overengineer.bettacraft.equipment.AirPumpBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(0.8F).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.DESTROY)));

    /** M23: LED panel (2 blocks wide), the middle light tier. */
    public static final DeferredBlock<org.overengineer.bettacraft.equipment.LedPanelBlock> LED_PANEL = BLOCKS.register("led_panel",
            () -> new org.overengineer.bettacraft.equipment.LedPanelBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(0.8F).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(TankLightBlock::lightLevel).isRedstoneConductor((s, l, p) -> false)));

    /** M23: high-power LED, the strongest light tier. */
    public static final DeferredBlock<org.overengineer.bettacraft.equipment.HighPowerLedBlock> HIGH_POWER_LED = BLOCKS.register("high_power_led",
            () -> new org.overengineer.bettacraft.equipment.HighPowerLedBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(1.0F).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(TankLightBlock::lightLevel).isRedstoneConductor((s, l, p) -> false)));

    // ------------------------------------------------------------------ M24 pipes, stainless steel, multiblock filter, water changes

    /** M24: the crucible for stainless steel. */
    public static final DeferredBlock<org.overengineer.bettacraft.crucible.CrucibleBlock> CRUCIBLE = BLOCKS.register("crucible",
            () -> new org.overengineer.bettacraft.crucible.CrucibleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NETHER).strength(2.5F, 6.0F).sound(SoundType.NETHER_BRICKS).requiresCorrectToolForDrops().noOcclusion()
                    .lightLevel(s -> s.getValue(org.overengineer.bettacraft.crucible.CrucibleBlock.LIT) ? 9 : 0)));
    public static final DeferredBlock<org.overengineer.bettacraft.plumbing.PipeBlock> GLASS_PIPE = BLOCKS.register("glass_pipe",
            () -> new org.overengineer.bettacraft.plumbing.PipeBlock(false, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE).strength(0.3F).sound(SoundType.GLASS).noOcclusion().isRedstoneConductor((s, l, p) -> false)));
    public static final DeferredBlock<org.overengineer.bettacraft.plumbing.PipeBlock> STAINLESS_STEEL_PIPE = BLOCKS.register("stainless_steel_pipe",
            () -> new org.overengineer.bettacraft.plumbing.PipeBlock(true, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(1.5F, 6.0F).sound(SoundType.METAL).noOcclusion().isRedstoneConductor((s, l, p) -> false)));
    public static final DeferredBlock<org.overengineer.bettacraft.plumbing.WaterPumpBlock> WATER_PUMP = BLOCKS.register("water_pump",
            () -> new org.overengineer.bettacraft.plumbing.WaterPumpBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(1.5F, 6.0F).sound(SoundType.METAL)));
    public static final DeferredBlock<org.overengineer.bettacraft.plumbing.WaterDeviceBlock> RESERVOIR = BLOCKS.register("reservoir",
            () -> new org.overengineer.bettacraft.plumbing.WaterDeviceBlock(org.overengineer.bettacraft.plumbing.WaterDeviceBlock.Kind.RESERVOIR,
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0F).sound(SoundType.GLASS).noOcclusion()));
    public static final DeferredBlock<org.overengineer.bettacraft.plumbing.WaterDeviceBlock> LEVEL_SENSOR = BLOCKS.register("level_sensor",
            () -> new org.overengineer.bettacraft.plumbing.WaterDeviceBlock(org.overengineer.bettacraft.plumbing.WaterDeviceBlock.Kind.LEVEL_SENSOR,
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0F).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<org.overengineer.bettacraft.plumbing.WaterDeviceBlock> WATER_CHANGE_CONTROLLER = BLOCKS.register("water_change_controller",
            () -> new org.overengineer.bettacraft.plumbing.WaterDeviceBlock(org.overengineer.bettacraft.plumbing.WaterDeviceBlock.Kind.WATER_CHANGER,
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL).noOcclusion()));
    // the plumbing of the tank interior
    public static final DeferredBlock<org.overengineer.bettacraft.plumbing.interior.PlumbingBlock> BULKHEAD = plumbing("bulkhead", org.overengineer.bettacraft.plumbing.interior.Plumb.PORT);
    public static final DeferredBlock<org.overengineer.bettacraft.plumbing.interior.PlumbingBlock> INTERIOR_PIPE = plumbing("interior_pipe", org.overengineer.bettacraft.plumbing.interior.Plumb.PIPE);
    public static final DeferredBlock<org.overengineer.bettacraft.plumbing.interior.PlumbingBlock> INTAKE_STRAINER = plumbing("intake_strainer", org.overengineer.bettacraft.plumbing.interior.Plumb.STRAINER);
    public static final DeferredBlock<org.overengineer.bettacraft.plumbing.interior.PlumbingBlock> LILY_PIPE = plumbing("lily_pipe", org.overengineer.bettacraft.plumbing.interior.Plumb.LILY);
    public static final DeferredBlock<org.overengineer.bettacraft.plumbing.interior.PlumbingBlock> NOZZLE = plumbing("nozzle", org.overengineer.bettacraft.plumbing.interior.Plumb.NOZZLE);
    public static final DeferredBlock<org.overengineer.bettacraft.plumbing.interior.PlumbingBlock> SPRAY_BAR = plumbing("spray_bar", org.overengineer.bettacraft.plumbing.interior.Plumb.SPRAY_BAR);
    // the multiblock external filter
    public static final DeferredBlock<org.overengineer.bettacraft.filter.FilterPartBlock> FILTER_CHAMBER = filterPart("filter_chamber", org.overengineer.bettacraft.filter.FilterPartBlock.Part.CHAMBER);
    public static final DeferredBlock<org.overengineer.bettacraft.filter.FilterPartBlock> FILTER_PUMP_HEAD = filterPart("filter_pump_head", org.overengineer.bettacraft.filter.FilterPartBlock.Part.PUMP_HEAD);
    public static final DeferredBlock<org.overengineer.bettacraft.filter.FilterPartBlock> FILTER_INLET = filterPart("filter_inlet", org.overengineer.bettacraft.filter.FilterPartBlock.Part.INLET);
    public static final DeferredBlock<org.overengineer.bettacraft.filter.FilterPartBlock> FILTER_OUTLET = filterPart("filter_outlet", org.overengineer.bettacraft.filter.FilterPartBlock.Part.OUTLET);

    // ------------------------------------------------------------------ M25 CO2 and reverse osmosis

    public static final DeferredBlock<org.overengineer.bettacraft.co2.BioCo2Block> BIO_CO2 = BLOCKS.register("bio_co2",
            () -> new org.overengineer.bettacraft.co2.BioCo2Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE).strength(0.3F).sound(SoundType.GLASS).noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<org.overengineer.bettacraft.co2.Co2CylinderBlock> CO2_CYLINDER = BLOCKS.register("co2_cylinder",
            () -> new org.overengineer.bettacraft.co2.Co2CylinderBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(1.5F, 6.0F).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<org.overengineer.bettacraft.co2.Co2StationBlock> CO2_STATION = BLOCKS.register("co2_station",
            () -> new org.overengineer.bettacraft.co2.Co2StationBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(2.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final DeferredBlock<org.overengineer.bettacraft.osmosis.RoUnitBlock> RO_UNIT = BLOCKS.register("ro_unit",
            () -> new org.overengineer.bettacraft.osmosis.RoUnitBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(1.5F, 6.0F).sound(SoundType.METAL).noOcclusion()));

    // ------------------------------------------------------------------ M31 terrarium devices

    /** M31: heat mat stuck under or onto a tank. */
    /** M31 part b: a turtle clutch half buried in the land of a tank's interior (no item; the tank incubates it). */
    public static final DeferredBlock<org.overengineer.bettacraft.terrarium.TurtleEggsBlock> TURTLE_EGGS = BLOCKS.register("turtle_eggs",
            () -> new org.overengineer.bettacraft.terrarium.TurtleEggsBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SAND)
                    .strength(0.5F).sound(SoundType.METAL).noOcclusion().noCollission().pushReaction(PushReaction.DESTROY)));
    /** M31 part c: a dart frog clutch: clear jelly with dark embryos on the land of a tank's interior (no item). */
    public static final DeferredBlock<org.overengineer.bettacraft.terrarium.TurtleEggsBlock> FROG_EGGS = BLOCKS.register("frog_eggs",
            () -> new org.overengineer.bettacraft.terrarium.TurtleEggsBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE)
                    .strength(0.1F).sound(SoundType.FROGSPAWN).noOcclusion().noCollission().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<org.overengineer.bettacraft.terrarium.HeatMatBlock> HEAT_MAT = BLOCKS.register("heat_mat",
            () -> new org.overengineer.bettacraft.terrarium.HeatMatBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(0.3F).sound(SoundType.WOOL).noOcclusion().noCollission().pushReaction(PushReaction.DESTROY)
                    .lightLevel(s -> s.getValue(org.overengineer.bettacraft.terrarium.HeatMatBlock.HEATING) ? 3 : 0)));
    /** M31: basking lamp (heat and UV) on a tank. */
    public static final DeferredBlock<org.overengineer.bettacraft.terrarium.BaskingLampBlock> BASKING_LAMP = BLOCKS.register("basking_lamp",
            () -> new org.overengineer.bettacraft.terrarium.BaskingLampBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(0.8F).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(TankLightBlock::lightLevel).isRedstoneConductor((s, l, p) -> false)));
    /** M31: mister (fogger) on or beside a tank. */
    public static final DeferredBlock<org.overengineer.bettacraft.terrarium.MisterBlock> MISTER = BLOCKS.register("mister",
            () -> new org.overengineer.bettacraft.terrarium.MisterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ).strength(0.8F).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.DESTROY)));
    /** M31: thermo-hygrometer on a tank wall. */
    public static final DeferredBlock<org.overengineer.bettacraft.terrarium.ThermoHygrometerBlock> THERMO_HYGROMETER = BLOCKS.register("thermo_hygrometer",
            () -> new org.overengineer.bettacraft.terrarium.ThermoHygrometerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.3F).sound(SoundType.METAL).noOcclusion().noCollission().pushReaction(PushReaction.DESTROY)));

    /** M25 part c: the dosing pump of a reef tank. */
    public static final DeferredBlock<org.overengineer.bettacraft.reef.DosingPumpBlock> DOSING_PUMP = BLOCKS.register("dosing_pump",
            () -> new org.overengineer.bettacraft.reef.DosingPumpBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(1.5F, 6.0F).sound(SoundType.METAL).noOcclusion()));

    /** M25 part d: active soil, fired clay granules with humic acids: as interior substrate it keeps the water soft and acidic. */
    public static final DeferredBlock<net.minecraft.world.level.block.Block> ACTIVE_SOIL = BLOCKS.register("active_soil",
            () -> new net.minecraft.world.level.block.Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BLACK)
                    .strength(0.6F).sound(SoundType.GRAVEL)));

    /** M28: Indian almond leaves lying on the floor (placed with the almond leaf item; tannins, food for the shrimp). */
    public static final DeferredBlock<org.overengineer.bettacraft.decor.LitterBlock> ALMOND_LEAF_LITTER = BLOCKS.register("almond_leaf_litter",
            () -> new org.overengineer.bettacraft.decor.LitterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN)
                    .instabreak().noCollission().noOcclusion().sound(SoundType.AZALEA_LEAVES).pushReaction(PushReaction.DESTROY),
                    ModItems.ALMOND_LEAF, Block.box(0, 0, 0, 16, 1, 16)));
    /** M28: alder cones lying on the floor (placed with the alder cone item; a few tannins, food for the shrimp). */
    public static final DeferredBlock<org.overengineer.bettacraft.decor.LitterBlock> ALDER_CONES = BLOCKS.register("alder_cones",
            () -> new org.overengineer.bettacraft.decor.LitterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN)
                    .instabreak().noCollission().noOcclusion().sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY),
                    ModItems.ALDER_CONE, Block.box(3, 0, 3, 13, 9, 13)));

    /** M26 part a: tubifex worms in mud and stale water. */
    public static final DeferredBlock<org.overengineer.bettacraft.food.LiveCultureBlock> TUBIFEX_DISH = BLOCKS.register("tubifex_dish",
            () -> new org.overengineer.bettacraft.food.LiveCultureBlock(org.overengineer.bettacraft.food.LiveCultureBlock.Kind.TUBIFEX, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_ORANGE).strength(1.0F).sound(SoundType.DECORATED_POT).noOcclusion()));
    /** M26 part a: daphnia (water fleas) in a barrel with a glass top. */
    public static final DeferredBlock<org.overengineer.bettacraft.food.LiveCultureBlock> DAPHNIA_BARREL = BLOCKS.register("daphnia_barrel",
            () -> new org.overengineer.bettacraft.food.LiveCultureBlock(org.overengineer.bettacraft.food.LiveCultureBlock.Kind.DAPHNIA, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
    /** M26 part a: pumps live food from a breeder through the pipes into tanks. */
    public static final DeferredBlock<org.overengineer.bettacraft.food.LiveFoodPumpBlock> LIVE_FOOD_PUMP = BLOCKS.register("live_food_pump",
            () -> new org.overengineer.bettacraft.food.LiveFoodPumpBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(1.5F, 6.0F).sound(SoundType.METAL).noOcclusion()));

    /** M26 part b: makes fish food from vanilla ingredients and live food (wound spring or FE). */
    public static final DeferredBlock<org.overengineer.bettacraft.food.FoodMakerBlock> FOOD_MAKER = BLOCKS.register("food_maker",
            () -> new org.overengineer.bettacraft.food.FoodMakerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(2.0F, 6.0F).sound(SoundType.METAL).noOcclusion()));

    /** M37: makes liquid fertilizer from kelp meal, nutrient salt and water (wound spring or FE). */
    public static final DeferredBlock<org.overengineer.bettacraft.fertilizer.FertilizerMakerBlock> FERTILIZER_MAKER = BLOCKS.register("fertilizer_maker",
            () -> new org.overengineer.bettacraft.fertilizer.FertilizerMakerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(2.0F, 6.0F).sound(SoundType.METAL).noOcclusion()));

    /** M27: a monitor block; next to each other they join into screens that show the tanks in range. */
    public static final DeferredBlock<org.overengineer.bettacraft.monitor.MonitorBlock> MONITOR = BLOCKS.register("monitor",
            () -> new org.overengineer.bettacraft.monitor.MonitorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK).strength(1.5F, 6.0F).sound(SoundType.METAL).pushReaction(PushReaction.BLOCK)));

    private static DeferredBlock<org.overengineer.bettacraft.plumbing.interior.PlumbingBlock> plumbing(String id, org.overengineer.bettacraft.plumbing.interior.Plumb kind) {
        return BLOCKS.register(id, () -> new org.overengineer.bettacraft.plumbing.interior.PlumbingBlock(kind, BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL).strength(0.3F).sound(SoundType.GLASS).noOcclusion().pushReaction(PushReaction.DESTROY)
                .isRedstoneConductor((s, l, p) -> false)));
    }

    private static DeferredBlock<org.overengineer.bettacraft.filter.FilterPartBlock> filterPart(String id, org.overengineer.bettacraft.filter.FilterPartBlock.Part part) {
        return BLOCKS.register(id, () -> new org.overengineer.bettacraft.filter.FilterPartBlock(part, BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL).strength(1.5F, 6.0F).sound(SoundType.METAL).noOcclusion()));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

    private ModBlocks() {}
}
