package org.overengineer.bettacraft.gametest.framework;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.Level;

/** M52c (26.1): the day time of the tests and smoke scenes ({@code setDayTime} is gone; the overworld clock, like 1.21.x's day time). */
public final class DevTime {
    private DevTime() {}

    public static void set(Level level, long time) {
        if (!(level instanceof ServerLevel sl)) return;
        sl.getServer().clockManager().setTotalTicks(sl.registryAccess().getOrThrow(WorldClocks.OVERWORLD), time);
    }
}
