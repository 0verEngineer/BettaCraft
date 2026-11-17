package org.overengineer.bettacraft.registry;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.overengineer.bettacraft.platform.registry.DeferredRegister;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.tank.TankBlockEntity;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BettaCraft.MOD_ID);

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<TankBlockEntity>> TANK = BLOCK_ENTITIES.register("tank",
            () -> new BlockEntityType<>(TankBlockEntity::new, java.util.Set.of(ModBlocks.TANK.get())));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.equipment.FeederBlockEntity>> FEEDER = BLOCK_ENTITIES.register("feeder",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.equipment.FeederBlockEntity::new, java.util.Set.of(ModBlocks.FEEDER.get())));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.equipment.ClipFeederBlockEntity>> CLIP_FEEDER = BLOCK_ENTITIES.register("clip_feeder",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.equipment.ClipFeederBlockEntity::new, java.util.Set.of(ModBlocks.CLIP_FEEDER.get())));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.tank.interior.InteriorFloorBlockEntity>> INTERIOR_FLOOR = BLOCK_ENTITIES.register("interior_floor",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.tank.interior.InteriorFloorBlockEntity::new, java.util.Set.of(ModBlocks.INTERIOR_FLOOR.get())));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity>> EXTERNAL_FILTER = BLOCK_ENTITIES.register("external_filter",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity::new, java.util.Set.of(ModBlocks.EXTERNAL_FILTER.get())));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.equipment.CabinetBlockEntity>> CABINET = BLOCK_ENTITIES.register("cabinet",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.equipment.CabinetBlockEntity::new, java.util.Set.of(ModBlocks.CABINET.get())));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.food.CultureTubBlockEntity>> CULTURE_TUB = BLOCK_ENTITIES.register("culture_tub",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.food.CultureTubBlockEntity::new, java.util.Set.of(ModBlocks.LARVA_TUB.get(), ModBlocks.ARTEMIA_HATCHERY.get())));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.equipment.AirStoneBlockEntity>> AIR_STONE = BLOCK_ENTITIES.register("air_stone",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.equipment.AirStoneBlockEntity::new, java.util.Set.of(ModBlocks.AIR_STONE.get())));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.equipment.AirPumpBlockEntity>> AIR_PUMP = BLOCK_ENTITIES.register("air_pump",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.equipment.AirPumpBlockEntity::new, java.util.Set.of(ModBlocks.AIR_PUMP.get())));

    // M24
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.crucible.CrucibleBlockEntity>> CRUCIBLE = BLOCK_ENTITIES.register("crucible",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.crucible.CrucibleBlockEntity::new, java.util.Set.of(ModBlocks.CRUCIBLE.get())));
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.plumbing.WaterPumpBlockEntity>> WATER_PUMP = BLOCK_ENTITIES.register("water_pump",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.plumbing.WaterPumpBlockEntity::new, java.util.Set.of(ModBlocks.WATER_PUMP.get())));
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.plumbing.ReservoirBlockEntity>> RESERVOIR = BLOCK_ENTITIES.register("reservoir",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.plumbing.ReservoirBlockEntity::new, java.util.Set.of(ModBlocks.RESERVOIR.get())));
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.plumbing.LevelSensorBlockEntity>> LEVEL_SENSOR = BLOCK_ENTITIES.register("level_sensor",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.plumbing.LevelSensorBlockEntity::new, java.util.Set.of(ModBlocks.LEVEL_SENSOR.get())));
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity>> WATER_CHANGER = BLOCK_ENTITIES.register("water_change_controller",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity::new, java.util.Set.of(ModBlocks.WATER_CHANGE_CONTROLLER.get())));
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.filter.FilterChamberBlockEntity>> FILTER_CHAMBER = BLOCK_ENTITIES.register("filter_chamber",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.filter.FilterChamberBlockEntity::new, java.util.Set.of(ModBlocks.FILTER_CHAMBER.get())));
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity>> FILTER_PUMP_HEAD = BLOCK_ENTITIES.register("filter_pump_head",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity::new, java.util.Set.of(ModBlocks.FILTER_PUMP_HEAD.get())));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.plumbing.FilterPipeBlockEntity>> FILTER_PIPE = BLOCK_ENTITIES.register("filter_pipe",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.plumbing.FilterPipeBlockEntity::new, java.util.Set.of(ModBlocks.FILTER_INLET_PIPE.get(), ModBlocks.FILTER_OUTLET_PIPE.get(), ModBlocks.FILTER_FRESH_PIPE.get(), ModBlocks.FILTER_WASTE_PIPE.get(), ModBlocks.FILTER_PIPE_BUNDLE.get()))); // M40 (part b: the fresh and waste line, the bundle)
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.filter.FilterPortBlockEntity>> FILTER_PORT = BLOCK_ENTITIES.register("filter_port",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.filter.FilterPortBlockEntity::new, java.util.Set.of(ModBlocks.FILTER_FRESH_PORT.get(), ModBlocks.FILTER_WASTE_PORT.get()))); // M40 part b

    // M25
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.co2.BioCo2BlockEntity>> BIO_CO2 = BLOCK_ENTITIES.register("bio_co2",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.co2.BioCo2BlockEntity::new, java.util.Set.of(ModBlocks.BIO_CO2.get())));
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.co2.Co2CylinderBlockEntity>> CO2_CYLINDER = BLOCK_ENTITIES.register("co2_cylinder",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.co2.Co2CylinderBlockEntity::new, java.util.Set.of(ModBlocks.CO2_CYLINDER.get())));
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.co2.Co2StationBlockEntity>> CO2_STATION = BLOCK_ENTITIES.register("co2_station",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.co2.Co2StationBlockEntity::new, java.util.Set.of(ModBlocks.CO2_STATION.get())));
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.osmosis.RoUnitBlockEntity>> RO_UNIT = BLOCK_ENTITIES.register("ro_unit",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.osmosis.RoUnitBlockEntity::new, java.util.Set.of(ModBlocks.RO_UNIT.get())));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.terrarium.MisterBlockEntity>> MISTER = BLOCK_ENTITIES.register("mister",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.terrarium.MisterBlockEntity::new, java.util.Set.of(ModBlocks.MISTER.get()))); // M31
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.terrarium.ThermoHygrometerBlockEntity>> THERMO_HYGROMETER = BLOCK_ENTITIES.register("thermo_hygrometer",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.terrarium.ThermoHygrometerBlockEntity::new, java.util.Set.of(ModBlocks.THERMO_HYGROMETER.get())));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.reef.DosingPumpBlockEntity>> DOSING_PUMP = BLOCK_ENTITIES.register("dosing_pump",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.reef.DosingPumpBlockEntity::new, java.util.Set.of(ModBlocks.DOSING_PUMP.get())));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.reef.CalciumReactorBlockEntity>> CALCIUM_REACTOR = BLOCK_ENTITIES.register("calcium_reactor",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.reef.CalciumReactorBlockEntity::new, java.util.Set.of(ModBlocks.CALCIUM_REACTOR.get()))); // M49

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.food.LiveCultureBlockEntity>> LIVE_CULTURE = BLOCK_ENTITIES.register("live_culture",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.food.LiveCultureBlockEntity::new, java.util.Set.of(ModBlocks.TUBIFEX_DISH.get(), ModBlocks.DAPHNIA_BARREL.get())));
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.food.LiveFoodPumpBlockEntity>> LIVE_FOOD_PUMP = BLOCK_ENTITIES.register("live_food_pump",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.food.LiveFoodPumpBlockEntity::new, java.util.Set.of(ModBlocks.LIVE_FOOD_PUMP.get())));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.food.FoodMakerBlockEntity>> FOOD_MAKER = BLOCK_ENTITIES.register("food_maker",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.food.FoodMakerBlockEntity::new, java.util.Set.of(ModBlocks.FOOD_MAKER.get())));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.fertilizer.FertilizerMakerBlockEntity>> FERTILIZER_MAKER = BLOCK_ENTITIES.register("fertilizer_maker",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.fertilizer.FertilizerMakerBlockEntity::new, java.util.Set.of(ModBlocks.FERTILIZER_MAKER.get()))); // M37

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.monitor.MonitorBlockEntity>> MONITOR = BLOCK_ENTITIES.register("monitor",
            () -> new BlockEntityType<>(org.overengineer.bettacraft.monitor.MonitorBlockEntity::new, java.util.Set.of(ModBlocks.MONITOR.get()))); // M27

    public static void register() {
        BLOCK_ENTITIES.register();
    }

    private ModBlockEntities() {}
}
