package org.overengineer.bettacraft.util;

import java.util.Collection;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/**
 * M17: the static client sets of block entities (tank controllers, filters) lose their entries only when a chunk is
 * unloaded or a block removed. A dimension change (also entering or leaving a tank interior) or a disconnect drops the
 * whole client level without either, so the old block entities - and through them the whole old level - stayed in the
 * sets. The client calls {@link #purgeLevel} on every client level unload and {@link #purgeLevel}(null) on logout.
 */
public final class ClientBlockEntitySets {
    private ClientBlockEntitySets() {}

    /** Removes every entry of the given level (all entries if {@code level} is null); returns how many. */
    public static <T> int purge(Collection<T> set, Function<T, ?> levelOf, @Nullable Object level) {
        int before = set.size();
        if (level == null) set.clear();
        else set.removeIf(e -> {
            Object l = levelOf.apply(e);
            return l == null || l == level;
        });
        return before - set.size();
    }

    public static int purgeLevel(@Nullable Object level) {
        return purge(TankBlockEntity.CLIENT_CONTROLLERS, TankBlockEntity::getLevel, level)
                + purge(ExternalFilterBlockEntity.CLIENT_LOADED, ExternalFilterBlockEntity::getLevel, level)
                + purge(CabinetBlockEntity.CLIENT_FILTERS, CabinetBlockEntity::getLevel, level);
    }
}
