package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;

/**
 * M67: the power settings a GameTest changes ({@code power.costs} and the power switches), kept and put back:
 * <pre>
 * PowerSettings saved = PowerSettings.current();
 * try { PowerSettings.costs(ServerConfig.PowerCosts.HARD); ... } finally { saved.apply(); }
 * </pre>
 */
public record PowerSettings(ServerConfig.PowerCosts costs, boolean power, boolean winding, boolean tankCrank, boolean joined) {
    public static PowerSettings current() {
        return new PowerSettings(ServerConfig.POWER_COSTS.get(), Features.own(Feature.POWER), Features.own(Feature.WINDING),
                Features.own(Feature.TANK_CRANK), Features.own(Feature.CABINET_SHARED_SPRING));
    }

    public void apply() {
        ServerConfig.POWER_COSTS.set(costs);
        Features.set(Feature.POWER, power);
        Features.set(Feature.WINDING, winding);
        Features.set(Feature.TANK_CRANK, tankCrank);
        Features.set(Feature.CABINET_SHARED_SPRING, joined);
    }

    /** Power on with these costs (the switches of power, winding and the tank crank on). */
    public static void costs(ServerConfig.PowerCosts costs) {
        ServerConfig.POWER_COSTS.set(costs);
        Features.set(Feature.POWER, true);
        Features.set(Feature.WINDING, true);
        Features.set(Feature.TANK_CRANK, true);
    }

    /** HARD (on) or OFF. */
    public static void hard(boolean on) {
        costs(on ? ServerConfig.PowerCosts.HARD : ServerConfig.PowerCosts.OFF);
    }
}
