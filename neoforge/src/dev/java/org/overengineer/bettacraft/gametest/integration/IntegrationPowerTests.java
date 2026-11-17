package org.overengineer.bettacraft.gametest.integration;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import static org.overengineer.bettacraft.gametest.integration.IntegrationRig.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.equipment.FeederBlockEntity;
import org.overengineer.bettacraft.gametest.PlumbingGameTestsAccess;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/**
 * M42 integration suite: Flux Networks (a network made through its data like its GUI does, a plug at a creative cell, a
 * point at a tank) and Mekanism's cables, transporters and pipes into our blocks. Power tests switch the filter power
 * option on in their own batch and restore it.
 */
@PrefixGameTestTemplate(false)
public final class IntegrationPowerTests {
    private static final String EMPTY = IntegrationSuite.EMPTY;

    private IntegrationPowerTests() {}

    /** Waits, restores the config and checks the tank's FE buffer. */
    private static void bufferFills(GameTestHelper h, TankBlockEntity t, Runnable restore, String who) {
        h.runAfterDelay(160, () -> {
            long got = t.getTank().powerBuffer();
            restore.run();
            h.assertTrue(got > 0, who + " delivers FE to the tank, buffer " + got);
            h.succeed();
        });
    }

    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_flux", timeoutTicks = 300)
    public static void fluxNetworkPowersATank(GameTestHelper h) {
        if (IntegrationSuite.lacks(h, "fluxnetworks", "powah")) return;
        Runnable restore = powerOn();
        TankBlockEntity t;
        try {
            h.setBlock(new BlockPos(1, 2, 2), block("powah:energy_cell_creative"));
            h.setBlock(new BlockPos(2, 2, 2), block("fluxnetworks:flux_plug"));
            h.setBlock(new BlockPos(6, 2, 2), block("fluxnetworks:flux_point"));
            t = tank(h, 7, 2, 2, 1, 1, 1, true);
            Object data = callStatic("sonar.fluxnetworks.common.connection.FluxNetworkData", "getInstance");
            Object network = call(data, "createNetwork", PlumbingGameTestsAccess.player(h), "bettacraft-test", 0xff0000,
                    enumValue("sonar.fluxnetworks.api.network.SecurityLevel", "PUBLIC"), "");
            if (network == null) throw new GameTestFailure("Flux Networks made no network");
            h.assertTrue((boolean) call(be(h, new BlockPos(2, 2, 2)), "connect", network), "the plug joins the network");
            h.assertTrue((boolean) call(be(h, new BlockPos(6, 2, 2)), "connect", network), "the point joins the network");
        } catch (RuntimeException e) {
            restore.run();
            throw e;
        }
        bufferFills(h, t, restore, "Flux Networks");
    }

    /** Mekanism: a universal cable from a creative cell (it pushes) into a tank block. */
    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_mekanism_power", timeoutTicks = 300)
    public static void mekanismUniversalCablePowersATank(GameTestHelper h) {
        if (IntegrationSuite.lacks(h, "mekanism", "powah")) return;
        Runnable restore = powerOn();
        TankBlockEntity t;
        try {
            h.setBlock(new BlockPos(1, 2, 2), block("powah:energy_cell_creative"));
            h.setBlock(new BlockPos(2, 2, 2), block("mekanism:basic_universal_cable"));
            h.setBlock(new BlockPos(3, 2, 2), block("mekanism:basic_universal_cable"));
            t = tank(h, 4, 2, 2, 1, 1, 1, true);
        } catch (RuntimeException e) {
            restore.run();
            throw e;
        }
        bufferFills(h, t, restore, "the Mekanism universal cable");
    }

    /** Mekanism: a logistical transporter pulling from a chest pushes the pellets into a feeder (the dirt stays). */
    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_mekanism", timeoutTicks = 900)
    public static void mekanismTransporterFillsAFeeder(GameTestHelper h) {
        if (IntegrationSuite.lacks(h, "mekanism")) return;
        ChestBlockEntity c = chest(h, new BlockPos(1, 2, 2), new ItemStack(ModItems.BETTA_PELLETS.get(), 16), new ItemStack(Items.DIRT, 16));
        h.setBlock(new BlockPos(2, 2, 2), block("mekanism:basic_logistical_transporter"));
        h.setBlock(new BlockPos(3, 2, 2), block("mekanism:basic_logistical_transporter"));
        FeederBlockEntity f = feeder(h, new BlockPos(4, 2, 2));
        pull(h, new BlockPos(2, 2, 2), Direction.WEST);
        h.succeedWhen(() -> {
            h.assertTrue(count(f, ModItems.BETTA_PELLETS.get()) == 16, "the transporter moved all pellets: " + count(f, ModItems.BETTA_PELLETS.get()));
            h.assertTrue(count(f, Items.DIRT) == 0, "no dirt in the feeder");
        });
    }

    /** Mekanism: a mechanical pipe pulling from a reservoir fills a tank. */
    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_mekanism", timeoutTicks = 600)
    public static void mekanismPipeFillsATank(GameTestHelper h) {
        if (IntegrationSuite.lacks(h, "mekanism")) return;
        PlumbingGameTestsAccess.reservoir(h, new BlockPos(1, 2, 2), 16000);
        h.setBlock(new BlockPos(2, 2, 2), block("mekanism:basic_mechanical_pipe"));
        h.setBlock(new BlockPos(3, 2, 2), block("mekanism:basic_mechanical_pipe"));
        TankBlockEntity t = tank(h, 4, 2, 2, 2, 2, 2, false);
        pull(h, new BlockPos(2, 2, 2), Direction.WEST);
        h.succeedWhen(() -> h.assertTrue(t.getTank().waterMb() > 0, "the Mekanism pipe fills the tank: " + t.getTank().waterMb()));
    }

    /** Sets a Mekanism transmitter's side to PULL (what the configurator does). */
    private static void pull(GameTestHelper h, BlockPos rel, Direction side) {
        Object transmitter = call(be(h, rel), "getTransmitter");
        call(transmitter, "setConnectionTypeRaw", side, enumValue("mekanism.common.lib.transmitter.ConnectionType", "PULL"));
    }
}
