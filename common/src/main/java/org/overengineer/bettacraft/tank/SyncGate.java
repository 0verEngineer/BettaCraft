package org.overengineer.bettacraft.tank;

import java.util.Arrays;

/**
 * M17: remembers the last payload of a periodic sync (small animals, floating plants: every care step to all players
 * near the tank) and lets a new one through only when its bytes differ, or after {@link #MAX_SILENCE} ticks anyway.
 */
public final class SyncGate {
    public static final long MAX_SILENCE = 200;
    private byte[] last;
    private long lastTime = Long.MIN_VALUE;

    /** True if {@code data} should be sent at {@code now} (and remembers it). */
    public boolean changed(byte[] data, long now) {
        if (last != null && Arrays.equals(last, data) && now - lastTime >= 0 && now - lastTime < MAX_SILENCE) return false;
        last = data;
        lastTime = now;
        return true;
    }

    /** Forget the last payload: the next one is sent (e.g. after a full tank update). */
    public void reset() {
        last = null;
    }
}
