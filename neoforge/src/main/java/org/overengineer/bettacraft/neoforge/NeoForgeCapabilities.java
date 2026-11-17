package org.overengineer.bettacraft.neoforge;

import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.neoforge.transfer.NeoResources;
import org.overengineer.bettacraft.neoforge.transfer.NeoTransfer;
import org.overengineer.bettacraft.osmosis.WaterBucketWrapper;
import org.overengineer.bettacraft.platform.energy.IEnergyStorage;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.platform.item.IItemHandler;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.registry.ModBlocks;

/**
 * M52: what our blocks and items offer other mods through NeoForge's capabilities (moved from the mod class). The handlers
 * are the common ones, wrapped by {@link NeoTransfer}.
 */
final class NeoForgeCapabilities {
    private NeoForgeCapabilities() {}

    @FunctionalInterface
    interface BlockFn<T> {
        @Nullable T apply(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity be, @Nullable Direction side);
    }

    static void register(RegisterCapabilitiesEvent event) {
        // M22.1: a bucket of sea water is a bucket of water for other mods' tanks and pipes (it empties into a bucket)
        // M25: other mods see the fluids of their own (bettacraft:sea_water, bettacraft:ro_water)
        event.registerItem(net.neoforged.neoforge.capabilities.Capabilities.Fluid.ITEM,
                (stack, access) -> new WaterBucketWrapper(access, org.overengineer.bettacraft.registry.ModFluids.SEA_WATER),
                org.overengineer.bettacraft.registry.ModItems.SEA_WATER_BUCKET.get());
        event.registerItem(net.neoforged.neoforge.capabilities.Capabilities.Fluid.ITEM,
                (stack, access) -> new WaterBucketWrapper(access, org.overengineer.bettacraft.registry.ModFluids.RO_WATER),
                org.overengineer.bettacraft.registry.ModItems.RO_WATER_BUCKET.get());
        // M25: the reverse osmosis unit (water in, reverse osmosis water out; parts from hoppers; FE for the pressure pump while
        // a power option is on) and the CO2 filling station (cylinders, FE for the compressor, CO2 gas of other mods)
        fluids(event, ModBlockEntities.RO_UNIT.get(), (be, side) -> be.fluid());
        items(event, ModBlockEntities.RO_UNIT.get(), (be, side) -> be.parts());
        items(event, ModBlockEntities.DOSING_PUMP.get(), (be, side) -> be.automation()); // M25c
        items(event, ModBlockEntities.CALCIUM_REACTOR.get(), (be, side) -> be.items()); // M49
        energy(event, ModBlockEntities.RO_UNIT.get(),
                (be, side) -> org.overengineer.bettacraft.crucible.CrucibleBlockEntity.energyOffered() ? be.energy() : null);
        items(event, ModBlockEntities.CO2_STATION.get(), (be, side) -> be.automation());
        fluids(event, ModBlockEntities.CO2_STATION.get(), (be, side) -> be.gasIn());
        energy(event, ModBlockEntities.CO2_STATION.get(),
                (be, side) -> org.overengineer.bettacraft.crucible.CrucibleBlockEntity.energyOffered() ? be.energy() : null);
        // M42: hoppers, pipes and ME / RS buses change the filter media (fresh ones in, used-up ones out)
        items(event, ModBlockEntities.FILTER_CHAMBER.get(),
                (be, side) -> org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.FILTER_MEDIA_AUTOMATION) ? be.automation() : null);
        // hoppers, pipes and other mods insert food into the feeder
        items(event, ModBlockEntities.FEEDER.get(),
                (be, side) -> be.items());
        // cabinet storage (not the filter slot): the feeder and hoppers use it
        items(event, ModBlockEntities.CABINET.get(),
                (be, side) -> be.storage());
        // M9 live food breeders: ingredients and bottles in, food out; water from pipes into the larva tub
        items(event, ModBlockEntities.CULTURE_TUB.get(),
                (be, side) -> be.automation());
        fluids(event, ModBlockEntities.CULTURE_TUB.get(),
                (be, side) -> be.fluid());
        // M26: live food cultures (starters, food and mud in; the harvest and empty bottles out; water from pipes) and the
        // live food pump (its empty bottles out; FE while the pumps need it)
        items(event, ModBlockEntities.LIVE_CULTURE.get(),
                (be, side) -> be.automation());
        fluids(event, ModBlockEntities.LIVE_CULTURE.get(),
                (be, side) -> be.fluid());
        items(event, ModBlockEntities.LIVE_FOOD_PUMP.get(),
                (be, side) -> be.bottleHandler());
        energy(event, ModBlockEntities.LIVE_FOOD_PUMP.get(),
                (be, side) -> org.overengineer.bettacraft.plumbing.DeviceEnergy.required() ? be.energy() : null);
        // M26 part b: the food maker (ingredients in from every side, the food out; FE while a power option is on): hoppers,
        // pipes and AE2 / RS buses, interfaces and pattern providers use it without special code
        items(event, ModBlockEntities.FOOD_MAKER.get(),
                (be, side) -> be.automation());
        // M37: the fertilizer maker (kelp meal, salt and bottles in, filled bottles out; water in, fertilizer out; FE with a power option)
        items(event, ModBlockEntities.FERTILIZER_MAKER.get(),
                (be, side) -> be.automation());
        fluids(event, ModBlockEntities.FERTILIZER_MAKER.get(),
                (be, side) -> be.fluids());
        energy(event, ModBlockEntities.FERTILIZER_MAKER.get(),
                (be, side) -> org.overengineer.bettacraft.fertilizer.FertilizerMakerBlockEntity.powered() ? be.energy() : null);
        // M27: every block of a monitor screen takes FE into the screen's buffer while a power option is on
        energy(event, ModBlockEntities.MONITOR.get(),
                (be, side) -> org.overengineer.bettacraft.monitor.MonitorSync.powerRequired() ? be.screenEnergy() : null);
        energy(event, ModBlockEntities.FOOD_MAKER.get(),
                (be, side) -> org.overengineer.bettacraft.food.FoodMakerBlockEntity.powered() ? be.energy() : null);
        // M9 tank water from pipes (fresh water in, used water out) and FE for the equipment (power.* config):
        // any tank block, an attached external filter, a cabinet under the tank or a light on it; only offered while the
        // server config needs power (no misleading energy bars in info mods otherwise)
        fluids(event, ModBlockEntities.TANK.get(),
                (be, side) -> new org.overengineer.bettacraft.tank.TankFluid(be.getLevel(), be.getBlockPos(), side)); // M24: a bulkhead from below
        energy(event, ModBlockEntities.TANK.get(),
                (be, side) -> org.overengineer.bettacraft.tank.TankPower.anyRequired() ? new org.overengineer.bettacraft.tank.TankPower.Storage(be.getLevel(), be.getBlockPos()) : null);
        energy(event, ModBlockEntities.EXTERNAL_FILTER.get(),
                (be, side) -> org.overengineer.bettacraft.tank.TankPower.anyRequired() ? new org.overengineer.bettacraft.tank.TankPower.Storage(be.getLevel(), be::tankBlockPos) : null);
        energy(event, ModBlockEntities.CABINET.get(),
                (be, side) -> org.overengineer.bettacraft.tank.TankPower.anyRequired() ? new org.overengineer.bettacraft.tank.TankPower.Storage(be.getLevel(), be.getBlockPos().above()) : null);
        energyBlocks(event, (level, pos, state, be, side) -> org.overengineer.bettacraft.tank.TankPower.anyRequired()
                        ? new org.overengineer.bettacraft.tank.TankPower.Storage(level, pos.below()) : null, ModBlocks.TANK_LIGHT.get(),
                ModBlocks.LED_PANEL.get(), ModBlocks.HIGH_POWER_LED.get(), ModBlocks.BASKING_LAMP.get()); // M23: the stronger lights too; M31 the basking lamp
        // M31: the mister takes water from pipes and other mods; it forwards FE to its tank
        fluids(event, ModBlockEntities.MISTER.get(),
                (be, side) -> be.fluidHandler());
        energy(event, ModBlockEntities.MISTER.get(),
                (be, side) -> org.overengineer.bettacraft.tank.TankPower.anyRequired() ? new org.overengineer.bettacraft.tank.TankPower.Storage(be.getLevel(),
                        () -> org.overengineer.bettacraft.equipment.AirPumpBlockEntity.tankBlock(be.getLevel(), be.getBlockPos())) : null);
        // M24: pipes carry water (no block entity: the network does it), the reservoir holds it; FE for the water devices
        fluidBlocks(event, (level, pos, state, be, side) -> new org.overengineer.bettacraft.plumbing.PipeFluid(level, pos, side),
                ModBlocks.GLASS_PIPE.get(), ModBlocks.STAINLESS_STEEL_PIPE.get());
        fluids(event, ModBlockEntities.RESERVOIR.get(),
                (be, side) -> be.water());
        // M40 part b: the fresh and waste water lines (single pipes and bundles that carry one) and the water ports of the filter;
        // the intake and return lines stay inside the filter
        fluidBlocks(event, (level, pos, state, be, side) -> org.overengineer.bettacraft.plumbing.FilterLineFluid.of(level, pos, state, be),
                ModBlocks.FILTER_FRESH_PIPE.get(), ModBlocks.FILTER_WASTE_PIPE.get(), ModBlocks.FILTER_PIPE_BUNDLE.get());
        fluids(event, ModBlockEntities.FILTER_PORT.get(),
                (be, side) -> be.outside());
        items(event, ModBlockEntities.CRUCIBLE.get(),
                (be, side) -> be.automation(side));
        energy(event, ModBlockEntities.CRUCIBLE.get(),
                (be, side) -> org.overengineer.bettacraft.crucible.CrucibleBlockEntity.energyOffered() ? be.energy() : null);
        energy(event, ModBlockEntities.WATER_PUMP.get(),
                (be, side) -> org.overengineer.bettacraft.plumbing.DeviceEnergy.required() ? be.energy() : null);
        energy(event, ModBlockEntities.FILTER_PUMP_HEAD.get(),
                (be, side) -> org.overengineer.bettacraft.plumbing.DeviceEnergy.required() ? be.energy() : null);
        energy(event, ModBlockEntities.WATER_CHANGER.get(),
                (be, side) -> org.overengineer.bettacraft.plumbing.DeviceEnergy.required() ? be.energy() : null);
        energy(event, ModBlockEntities.LEVEL_SENSOR.get(),
                (be, side) -> org.overengineer.bettacraft.plumbing.DeviceEnergy.required() ? be.energy() : null);
        // M23: the air pump forwards FE to the tank it serves
        energy(event, ModBlockEntities.AIR_PUMP.get(),
                (be, side) -> org.overengineer.bettacraft.tank.TankPower.anyRequired() ? new org.overengineer.bettacraft.tank.TankPower.Storage(be.getLevel(), be::tankBlockPos) : null);
    }

    private static <B extends BlockEntity> void items(RegisterCapabilitiesEvent e, BlockEntityType<B> type, BiFunction<B, Direction, @Nullable IItemHandler> f) {
        e.registerBlockEntity(Capabilities.Item.BLOCK, type, (be, side) -> NeoResources.items(f.apply(be, side)));
    }

    private static <B extends BlockEntity> void fluids(RegisterCapabilitiesEvent e, BlockEntityType<B> type, BiFunction<B, Direction, @Nullable IFluidHandler> f) {
        e.registerBlockEntity(Capabilities.Fluid.BLOCK, type, (be, side) -> NeoResources.fluids(f.apply(be, side)));
    }

    private static <B extends BlockEntity> void energy(RegisterCapabilitiesEvent e, BlockEntityType<B> type, BiFunction<B, Direction, @Nullable IEnergyStorage> f) {
        e.registerBlockEntity(Capabilities.Energy.BLOCK, type, (be, side) -> NeoResources.energy(f.apply(be, side)));
    }

    private static void fluidBlocks(RegisterCapabilitiesEvent e, BlockFn<IFluidHandler> f, Block... blocks) {
        e.registerBlock(Capabilities.Fluid.BLOCK, (level, pos, state, be, side) -> NeoResources.fluids(f.apply(level, pos, state, be, side)), blocks);
    }

    private static void energyBlocks(RegisterCapabilitiesEvent e, BlockFn<IEnergyStorage> f, Block... blocks) {
        e.registerBlock(Capabilities.Energy.BLOCK, (level, pos, state, be, side) -> NeoResources.energy(f.apply(level, pos, state, be, side)), blocks);
    }
}
