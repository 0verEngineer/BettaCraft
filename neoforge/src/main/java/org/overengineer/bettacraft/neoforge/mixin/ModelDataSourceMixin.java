package org.overengineer.bettacraft.neoforge.mixin;

import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.neoforge.NeoModelData;
import org.overengineer.bettacraft.platform.model.ModelDataSource;
import org.overengineer.bettacraft.plumbing.PipeBlockEntity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.interior.InteriorFloorBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * M52: NeoForge asks a block entity for its model data with {@code getModelData()}; our block entities (common code, built
 * against vanilla) have their render data as {@link ModelDataSource#modelValues()}. This gives them the NeoForge method.
 */
@Mixin({TankBlockEntity.class, CabinetBlockEntity.class, PipeBlockEntity.class, InteriorFloorBlockEntity.class})
public abstract class ModelDataSourceMixin implements IBlockEntityExtension {
    @Override
    public ModelData getModelData() {
        return NeoModelData.toNeo(((ModelDataSource) (Object) this).modelValues());
    }
}
