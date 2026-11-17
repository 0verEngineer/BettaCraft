package org.overengineer.bettacraft.client.tank;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

/** M32 part b (client only): whether a client-side level is not the world the player is in (a Create contraption's own level). */
public final class TankClientLevels {
    private TankClientLevels() {}

    public static boolean notTheWorld(Level level) {
        return level != Minecraft.getInstance().level;
    }
}
