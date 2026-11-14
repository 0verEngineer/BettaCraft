package org.overengineer.bettacraft.equipment;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * An external filter attached to a tank (PLAN 6.2): either the filter block next to the tank or a filter in the slot
 * of a cabinet under the tank. Holds up to two heaters or coolers (M9). Since M9 it cleans the water (water quality);
 * the water flow itself is still only visual (OFFEN-11/17).
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

    /** M23: power regulator 0..10 (0..100 %). */
    default int regulator() {
        return 10;
    }

    default void setRegulator(int v) {}

    /** M23: share 0..1 of the regulator (1 while features.powerRegulator is off). */
    default double powerShare() {
        return org.overengineer.bettacraft.config.CommonConfig.powerRegulator() ? Math.max(0, Math.min(10, regulator())) / 10.0 : 1.0;
    }

    /** M23: an intake sponge is fitted (the filter does not suck in fry). */
    default boolean intakeSponge() {
        return false;
    }

    default void setIntakeSponge(boolean on) {}

    default int heaterBlocks() {
        if (!hasFilter()) return 0;
        int sum = 0;
        for (int i = 0; i < FilterHeaters.SLOTS; i++) sum += FilterHeaters.capacity(heater(i));
        return sum;
    }

    default int coolerBlocks() {
        if (!hasFilter()) return 0;
        int sum = 0;
        for (int i = 0; i < FilterHeaters.SLOTS; i++) sum += FilterHeaters.coolingCapacity(heater(i));
        return sum;
    }

    default int coolerCount() {
        if (!hasFilter()) return 0;
        int n = 0;
        for (int i = 0; i < FilterHeaters.SLOTS; i++) if (FilterHeaters.isCooler(heater(i))) n++;
        return n;
    }

    /** Heaters and coolers in the slots. */
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
}
