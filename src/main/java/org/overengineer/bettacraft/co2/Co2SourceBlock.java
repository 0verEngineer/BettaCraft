package org.overengineer.bettacraft.co2;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * M25: a block that doses CO2 into the tank it stands next to or on (like the air pump: below first, then the sides);
 * its block entity is a {@link Co2Source}.
 */
public interface Co2SourceBlock {
    /** Tells the tank it touches to look for its equipment again (placed, removed). */
    static void invalidateTank(Level level, BlockPos pos) {
        var c = org.overengineer.bettacraft.equipment.AirPumpBlockEntity.findTank(level, pos);
        if (c != null) org.overengineer.bettacraft.tank.TankEquipment.invalidate(c);
    }
}
