package org.overengineer.bettacraft.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;

/** M42 part b: the package helpers of the GameTests that the zoo suite (gametest/zoo) uses too. */
public final class ZooAccess {
    private ZooAccess() {}

    /** A full freshwater tank of sx * sy * sz at (x, 2, z) of {@code temp} degrees, clean water. */
    public static TankBlockEntity waterTank(GameTestHelper h, int x, int z, int sx, int sy, int sz, float temp) {
        return ForeignGameTests.tank(h, x, z, sx, sy, sz, temp);
    }

    /** A dry terrarium of sx * sy * sz at (x, 2, z) with a sand floor and a warm climate. */
    public static TankBlockEntity terrarium(GameTestHelper h, int x, int z, int sx, int sy, int sz) {
        return ForeignTerrariumGameTests.terrarium(h, x, z, sx, sy, sz);
    }

    /** A paludarium (water a third of its height, land over the {@code landShare} of the width, of {@code top}). */
    public static TankBlockEntity paludarium(GameTestHelper h, int x, int z, int sx, int sy, int sz, float ppt, double landShare, BlockState top) {
        return ForeignTerrariumGameTests.paludarium(h, x, z, sx, sy, sz, ppt, landShare, top);
    }

    /** A sand floor over the whole interior of the tank (its region built). */
    public static InteriorRegion sandFloor(GameTestHelper h, TankBlockEntity c) {
        return ForeignTerrariumGameTests.sandFloor(h, c);
    }

    /** The mock player of the GameTests that can receive payloads. */
    public static ServerPlayer player(GameTestHelper h) {
        return MockPlayers.serverPlayer(h);
    }
}
