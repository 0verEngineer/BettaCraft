package org.overengineer.bettacraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import org.overengineer.bettacraft.plumbing.ReservoirBlockEntity;

/** M42: the package helpers of the GameTests that the integration suite (gametest/integration) uses too. */
public final class PlumbingGameTestsAccess {
    private PlumbingGameTestsAccess() {}

    /** A reservoir with {@code mb} of plain water (none when 0). */
    public static ReservoirBlockEntity reservoir(GameTestHelper h, BlockPos rel, int mb) {
        return PlumbingGameTests.reservoir(h, rel, mb, null);
    }

    /** A dry 2x{@code sy}x2 tank at (x, 2, 2): a terrarium. */
    public static org.overengineer.bettacraft.tank.TankBlockEntity terrarium(GameTestHelper h, int x, int sy) {
        org.overengineer.bettacraft.tank.TankBlockEntity c = CritterGameTests.tank(h, x, 2, 2, 2, sy, 2, 0);
        var m = org.overengineer.bettacraft.terrarium.TankClimate.refreshMode(h.getLevel(), c);
        if (m != org.overengineer.bettacraft.terrarium.TerrariumMode.TERRARIUM) throw new org.overengineer.bettacraft.gametest.framework.GameTestFailure("no terrarium: " + m);
        return c;
    }

    /** A food maker (spring wound up by the caller as needed). */
    public static org.overengineer.bettacraft.food.FoodMakerBlockEntity foodMaker(GameTestHelper h, BlockPos rel) {
        return FoodMakerGameTests.maker(h, rel);
    }

    /** The mock player of the GameTests that can receive payloads. */
    public static ServerPlayer player(GameTestHelper h) {
        return MockPlayers.serverPlayer(h);
    }
}
