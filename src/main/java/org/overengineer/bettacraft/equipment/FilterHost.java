package org.overengineer.bettacraft.equipment;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * An external filter attached to a tank (PLAN 6.2): either the filter block next to the tank or a filter in the slot
 * of a cabinet under the tank. Holds up to two heaters. The water flow is purely visual (OFFEN-11/17).
 */
public interface FilterHost {
    /** Whether a filter is present (a cabinet may have an empty filter slot). */
    boolean hasFilter();

    /** Heater in slot 0..1 (empty if none). */
    ItemStack heater(int slot);

    /** Puts a heater into a free slot; false if there is none (or no filter). */
    boolean insertHeater(ItemStack heater);

    /** Tank block the filter's pipe goes into, or null. */
    @Nullable
    BlockPos tankBlockPos();

    default int heaterBlocks() {
        if (!hasFilter()) return 0;
        int sum = 0;
        for (int i = 0; i < FilterHeaters.SLOTS; i++) sum += FilterHeaters.capacity(heater(i));
        return sum;
    }

    default int heaterCount() {
        if (!hasFilter()) return 0;
        int n = 0;
        for (int i = 0; i < FilterHeaters.SLOTS; i++) if (!heater(i).isEmpty()) n++;
        return n;
    }

    default int freeHeaterSlots() {
        return hasFilter() ? FilterHeaters.SLOTS - heaterCount() : 0;
    }

    /** Controller of the tank this filter is attached to (also on the client, with the client view). */
    @Nullable
    static TankBlockEntity tankOf(Level level, @Nullable BlockPos tankBlock) {
        return tankBlock == null || !level.isLoaded(tankBlock) ? null : TankMultiblock.getController(level, tankBlock);
    }

    /** Direction from the tank wall outwards for the filter block next to it. */
    static Direction outward(Direction facingToTank) {
        return facingToTank.getOpposite();
    }
}
