package org.overengineer.bettacraft.terrarium;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.registry.ModBlockEntities;

/** M31: the block entity of the thermo-hygrometer: only there for its screen renderer (it holds no data). */
public class ThermoHygrometerBlockEntity extends BlockEntity {
    public ThermoHygrometerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.THERMO_HYGROMETER.get(), pos, state);
    }
}
