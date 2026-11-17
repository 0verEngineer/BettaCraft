package org.overengineer.bettacraft.compat.wthit;

import mcp.mobius.waila.api.ICommonRegistrar;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IWailaCommonPlugin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.overengineer.bettacraft.compat.info.InfoData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/**
 * WTHIT integration, server side (optional, PLAN 10, M8): sends the tags of {@link InfoData} to the client. Declared
 * in {@code waila_plugins.json}, so WTHIT only loads it when it is installed.
 */
public final class WthitCommonPlugin implements IWailaCommonPlugin {
    private static void put(mcp.mobius.waila.api.IDataWriter data, CompoundTag out) {
        if (out != null) data.raw().put(InfoData.KEY, out);
    }

    @Override
    public void register(ICommonRegistrar registrar) {
        IDataProvider<BlockEntity> tank = (data, accessor, config) -> put(data, InfoData.tank(accessor.getLevel(), accessor.getTarget().getBlockPos()));
        IDataProvider<BlockEntity> filter = (data, accessor, config) -> put(data, InfoData.filter(accessor.getLevel(), accessor.getTarget()));
        IDataProvider<BettaEntity> betta = (data, accessor, config) -> put(data, InfoData.betta(accessor.getTarget()));
        registrar.blockData(tank, TankBlockEntity.class);
        registrar.blockData(filter, ExternalFilterBlockEntity.class);
        registrar.blockData(filter, CabinetBlockEntity.class);
        IDataProvider<BlockEntity> culture = (data, accessor, config) -> put(data, InfoData.culture(accessor.getLevel(), accessor.getTarget()));
        registrar.blockData(culture, org.overengineer.bettacraft.food.CultureTubBlockEntity.class);
        registrar.entityData(betta, BettaEntity.class);
        IDataProvider<BlockEntity> spring = (data, accessor, config) -> put(data, InfoData.spring(accessor.getLevel(), accessor.getTarget())); // M37
        registrar.blockData(spring, org.overengineer.bettacraft.food.FoodMakerBlockEntity.class);
        registrar.blockData(spring, org.overengineer.bettacraft.fertilizer.FertilizerMakerBlockEntity.class);
        registrar.blockData(spring, org.overengineer.bettacraft.osmosis.RoUnitBlockEntity.class);
        registrar.blockData(spring, org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity.class);
        registrar.blockData(spring, org.overengineer.bettacraft.plumbing.WaterPumpBlockEntity.class);
        registrar.blockData(spring, org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity.class);
    }
}
