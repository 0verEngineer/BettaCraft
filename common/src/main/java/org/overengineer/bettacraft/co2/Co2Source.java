package org.overengineer.bettacraft.co2;

import net.minecraft.server.level.ServerLevel;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/** M25: a block entity next to a tank that doses CO2 into it (bio CO2 bottle, gas cylinder). */
public interface Co2Source {
    /** Bubbles per second it gives the tank right now (0 = nothing). */
    double bubbles(ServerLevel level, TankBlockEntity tank);

    /** {@code days} have passed at {@code bubbles} per second: use up the gas, update the shown state. */
    void used(ServerLevel level, double bubbles, double days);
}
