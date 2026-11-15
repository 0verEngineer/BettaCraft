package org.overengineer.bettacraft.api.event;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * M32 part d: an alarm tile of a monitor (M27) went off or cleared. A monitor with alarm tiles watches its tanks every
 * second, also when nobody looks at it. {@link #isActive} true: the value of the tank went past the limit; false: it is
 * back, or the monitor stopped watching (broken, unloaded, without power, the tile removed). {@link #getValue} is the id
 * of the monitor value ({@code temperature}, {@code water_quality}, {@code ph}, {@code tds}, {@code salinity}, ...),
 * {@link #getCurrent} its value now (NaN when the monitor stopped watching), {@link #getLimit} the limit of the tile and
 * {@link #isBelow} whether the alarm is for values below it.
 */
public class TankAlarmEvent extends TankEvent {
    private final BlockPos monitorPos;
    private final String value;
    private final float current, limit;
    private final boolean below, active;

    public TankAlarmEvent(ServerLevel level, BlockPos tankPos, UUID tankId, String tankName, BlockPos monitorPos, String value, float current,
                          float limit, boolean below, boolean active) {
        super(level, tankPos, tankId, tankName);
        this.monitorPos = monitorPos.immutable();
        this.value = value;
        this.current = current;
        this.limit = limit;
        this.below = below;
        this.active = active;
    }

    /** The controller block of the monitor screen. */
    public BlockPos getMonitorPos() {
        return monitorPos;
    }

    public String getValue() {
        return value;
    }

    public float getCurrent() {
        return current;
    }

    public float getLimit() {
        return limit;
    }

    public boolean isBelow() {
        return below;
    }

    public boolean isActive() {
        return active;
    }
}
