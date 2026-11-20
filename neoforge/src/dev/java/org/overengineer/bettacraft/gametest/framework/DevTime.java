package org.overengineer.bettacraft.gametest.framework;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** M52 (backport of the 26.2 fix): the day time of the tests, with the sky darkness of the new time at once. */
public final class DevTime {
    private DevTime() {}

    public static void set(Level level, long time) {
        if (!(level instanceof ServerLevel sl)) return;
        sl.setDayTime(time);
        // the sky darkness follows the time only at the next tick: a test that simulates days at once saw the light of the time
        // before (the shared clock of the previous batch)
        for (ServerLevel l : sl.getServer().getAllLevels()) l.updateSkyBrightness();
    }
}
