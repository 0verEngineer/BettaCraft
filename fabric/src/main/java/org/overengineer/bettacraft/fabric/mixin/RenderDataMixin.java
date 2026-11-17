package org.overengineer.bettacraft.fabric.mixin;

import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.platform.model.ModelDataSource;
import org.overengineer.bettacraft.plumbing.FilterPipeBlockEntity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.interior.InteriorFloorBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * M52: our block entities hand their model values to Fabric's chunk mesher as render data (NeoForge: the model data,
 * see the NeoForge ModelDataSourceMixin); {@code fabric/client/FabricDynamicModel} reads them.
 */
@Mixin({TankBlockEntity.class, CabinetBlockEntity.class, FilterPipeBlockEntity.class, InteriorFloorBlockEntity.class})
public abstract class RenderDataMixin implements RenderDataBlockEntity {
    @Override
    public Object getRenderData() {
        return ((ModelDataSource) this).modelValues();
    }
}
