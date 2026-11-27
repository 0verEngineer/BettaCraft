package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankPower;

/**
 * Forge Energy: the FE values of the server config, and real cables and pipes of other mods delivering FE to a tank
 * block, a cabinet, an external filter and a light (only with {@code gradlew runGameTestServer -Ppower}, which loads
 * EnderIO, Mekanism, Powah, Flux Networks and, locally only, Pipez; without them those tests pass without doing anything).
 * Mekanism cable networks do not run in the GameTest server (not even between a Mekanism cable and a Powah cell) and
 * Flux Networks needs a network made in its GUI: both are checked by hand. Every test has its own batch because the
 * config is global.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PowerGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private PowerGameTests() {}

    // ------------------------------------------------------------------ helpers

    private static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz) {
        for (int a = 0; a < sx; a++)
            for (int b = 0; b < sy; b++)
                for (int c = 0; c < sz; c++) {
                    BlockPos p = new BlockPos(x + a, y + b, z + c);
                    h.setBlock(p, ModBlocks.TANK.get().defaultBlockState());
                    TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(p), null);
                }
        TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(new BlockPos(x, y, z)));
        if (c == null) throw new GameTestFailure("tank did not form");
        TankInteractions.changeWater(h.getLevel(), c, c.getTank().capacityMb());
        return c;
    }

    private static ExternalFilterBlockEntity filter(GameTestHelper h, BlockPos rel, Direction towardsTank) {
        h.setBlock(rel, ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, towardsTank));
        if (!(h.getLevel().getBlockEntity(h.absolutePos(rel)) instanceof ExternalFilterBlockEntity f)) throw new GameTestFailure("no filter BE");
        return f;
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id)).orElseThrow(() -> new GameTestFailure("no block " + id));
    }

    /**
     * The creative FE source for the EnderIO conduit tests: EnderIO's creative power block, or (M52c: EnderIO 9 on 26.1 has none)
     * Powah's creative cell, which pushes into its neighbours too.
     */
    private static Block creativePower() {
        return BuiltInRegistries.BLOCK.getOptional(Identifier.parse("enderio:creative_power"))
                .or(() -> BuiltInRegistries.BLOCK.getOptional(Identifier.parse("powah:energy_cell_creative")))
                .orElseThrow(() -> new GameTestFailure("no creative power block (EnderIO or Powah)"));
    }

    private static boolean loaded(String... mods) {
        for (String m : mods) if (!org.overengineer.bettacraft.platform.Platform.get().isModLoaded(m)) return false;
        return true;
    }

    /** The power settings of the server config, restored after a test. */
    private record PowerConfig(ServerConfig.PowerCosts costs, double heaterFe, double coolerFe, int filterFe, int lightFe, int perBlock, int perCabinet) {
        static PowerConfig current() {
            return new PowerConfig(ServerConfig.POWER_COSTS.get(),
                    ServerConfig.POWER_HEATER_FE_PER_BLOCK.get(), ServerConfig.POWER_COOLER_FE_PER_BLOCK.get(), ServerConfig.POWER_FILTER_FE.get(),
                    ServerConfig.POWER_LIGHT_FE.get(), ServerConfig.POWER_STORE_PER_BLOCK.get(), ServerConfig.POWER_STORE_PER_CABINET.get());
        }

        void apply() {
            ServerConfig.POWER_COSTS.set(costs);
            ServerConfig.POWER_HEATER_FE_PER_BLOCK.set(heaterFe);
            ServerConfig.POWER_COOLER_FE_PER_BLOCK.set(coolerFe);
            ServerConfig.POWER_FILTER_FE.set(filterFe);
            ServerConfig.POWER_LIGHT_FE.set(lightFe);
            ServerConfig.POWER_STORE_PER_BLOCK.set(perBlock);
            ServerConfig.POWER_STORE_PER_CABINET.set(perCabinet);
        }
    }

    // ------------------------------------------------------------------ config values

    /** Heater, cooler, filter and light use and the buffer size all come from the server config. */
    @GameTest(template = EMPTY, batch = "bettacraft_power_config")
    public static void feValuesComeFromTheConfig(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        PowerConfig saved = PowerConfig.current();
        try {
            new PowerConfig(ServerConfig.PowerCosts.HARD, 3.0, 5.0, 7, 2, 5000, 1000).apply();
            // heating tank: 2x2x2 = 8 water blocks, a small heater (enough for 16), one filter, one light block
            TankBlockEntity warm = tank(h, 2, 1, 2, 2, 2, 2);
            ExternalFilterBlockEntity f1 = filter(h, new BlockPos(4, 1, 2), Direction.WEST);
            f1.insertHeater(new ItemStack(ModItems.HEATER_SMALL.get()));
            TankEquipment.invalidate(warm);
            h.setBlock(new BlockPos(2, 3, 2), ModBlocks.TANK_LIGHT.get().defaultBlockState());
            warm.getTank().setTargetTemp(34.0F);
            h.assertTrue(TankPower.heaterDemand(level, warm) == 8 * 3, "heater: 8 blocks x 3 FE, got " + TankPower.heaterDemand(level, warm));
            warm.getTank().setPowerBuffer(1_000_000);
            TankPower.step(level, warm, 1);
            h.assertTrue(warm.lastDemand() == 8 * 3 + 7 + 2, "heater + filter 7 + light 2, got " + warm.lastDemand());
            ServerConfig.POWER_HEATER_FE_PER_BLOCK.set(6.0);
            h.assertTrue(TankPower.heaterDemand(level, warm) == 8 * 6, "a config change is used at once");

            // cooling tank: the cooler value, not the heater value
            TankBlockEntity cool = tank(h, 8, 1, 2, 2, 2, 2);
            ExternalFilterBlockEntity f2 = filter(h, new BlockPos(10, 1, 2), Direction.WEST);
            f2.insertHeater(new ItemStack(ModItems.COOLER_SMALL.get()));
            TankEquipment.invalidate(cool);
            cool.getTank().setTargetTemp(5.0F);
            h.assertTrue(TankPower.heaterDemand(level, cool) == 8 * 5, "cooler: 8 blocks x 5 FE, got " + TankPower.heaterDemand(level, cool));
            // a tank that needs heating draws nothing for a cooler
            cool.getTank().setTargetTemp(34.0F);
            h.assertTrue(TankPower.heaterDemand(level, cool) == 0, "a cooler draws nothing while the tank needs heating");

            // M67: the store: 5000 FE per water block (8 of them), no cabinet
            TankPower.step(level, cool, 1);
            var storage = org.overengineer.bettacraft.platform.Platform.get().energyStorage(level, cool.getBlockPos(), null);
            h.assertTrue(storage != null && storage.getMaxEnergyStored() == 8 * 5000, "store 40000 FE: " + (storage == null ? -1 : storage.getMaxEnergyStored()));
            cool.getTank().setPowerBuffer(0);
            h.assertTrue(storage.receiveEnergy(100000, false) == 8 * 5000 && storage.receiveEnergy(1, true) == 0, "takes at most the store");
        } finally {
            saved.apply();
        }
        h.succeed();
    }

    /** Turning power on at runtime: cables that are already connected see the tank's storage without a chunk reload. */
    @GameTest(template = EMPTY, timeoutTicks = 200, batch = "bettacraft_power_toggle")
    public static void connectedCablesNoticePowerBeingSwitchedOn(GameTestHelper h) {
        if (!loaded("powah")) {
            h.succeed();
            return;
        }
        PowerConfig saved = PowerConfig.current();
        new PowerConfig(ServerConfig.PowerCosts.OFF, saved.heaterFe(), saved.coolerFe(), saved.filterFe(), saved.lightFe(), saved.perBlock(), saved.perCabinet()).apply();
        h.setBlock(new BlockPos(1, 1, 2), block("powah:energy_cell_creative"));
        h.setBlock(new BlockPos(2, 1, 2), block("powah:energy_cable_starter"));
        h.setBlock(new BlockPos(3, 1, 2), block("powah:energy_cable_starter"));
        TankBlockEntity c = tank(h, 4, 1, 2, 1, 1, 1);
        h.runAfterDelay(30, () -> {
            h.assertTrue(c.getTank().powerBuffer() == 0, "no FE while power is off");
            ServerConfig.POWER_COSTS.set(ServerConfig.PowerCosts.HARD);
            h.runAfterDelay(60, () -> {
                long got = c.getTank().powerBuffer();
                saved.apply();
                h.assertTrue(got > 0, "the connected cable delivers after power was switched on, buffer " + got);
                h.succeed();
            });
        });
    }

    // ------------------------------------------------------------------ cables of other mods

    /** Powah energy cable into a tank block, from a Powah creative cell. */
    @GameTest(template = EMPTY, timeoutTicks = 200, batch = "bettacraft_power_powah")
    public static void powahCableFeedsATankBlock(GameTestHelper h) {
        if (!loaded("powah")) {
            h.succeed();
            return;
        }
        PowerConfig saved = PowerConfig.current();
        ServerConfig.POWER_COSTS.set(ServerConfig.PowerCosts.HARD);
        TankBlockEntity c = tank(h, 4, 1, 2, 2, 1, 1);
        h.setBlock(new BlockPos(1, 1, 2), block("powah:energy_cell_creative"));
        h.setBlock(new BlockPos(2, 1, 2), block("powah:energy_cable_starter"));
        h.setBlock(new BlockPos(3, 1, 2), block("powah:energy_cable_starter"));
        h.runAfterDelay(60, () -> {
            long got = c.getTank().powerBuffer();
            saved.apply();
            h.assertTrue(got > 0, "Powah cable delivers to the tank, buffer " + got);
            h.succeed();
        });
    }

    /** Pipez energy pipe (extracting from a Powah creative cell) into a cabinet under the tank. */
    @GameTest(template = EMPTY, timeoutTicks = 200, batch = "bettacraft_power_pipez")
    public static void pipezPipeFeedsACabinet(GameTestHelper h) {
        if (!loaded("pipez", "powah")) {
            h.succeed();
            return;
        }
        PowerConfig saved = PowerConfig.current();
        ServerConfig.POWER_COSTS.set(ServerConfig.PowerCosts.HARD);
        ServerLevel level = h.getLevel();
        h.setBlock(new BlockPos(4, 1, 2), ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
        TankBlockEntity c = tank(h, 4, 2, 2, 1, 1, 1);
        h.setBlock(new BlockPos(1, 1, 2), block("powah:energy_cell_creative"));
        for (int x = 2; x <= 3; x++) {
            BlockState s = block("pipez:energy_pipe").defaultBlockState();
            for (var p : s.getProperties()) if (p.getName().equals("has_data")) s = s.setValue((BooleanProperty) p, true);
            h.setBlock(new BlockPos(x, 1, 2), s);
        }
        try {
            Object pipe = level.getBlockEntity(h.absolutePos(new BlockPos(2, 1, 2)));
            pipe.getClass().getMethod("setExtracting", Direction.class, boolean.class).invoke(pipe, Direction.WEST, true);
        } catch (ReflectiveOperationException | NullPointerException e) {
            saved.apply();
            throw new GameTestFailure("Pipez API changed: " + e);
        }
        h.runAfterDelay(80, () -> {
            long got = c.getTank().powerBuffer();
            saved.apply();
            h.assertTrue(got > 0, "Pipez pipe delivers through the cabinet, buffer " + got);
            h.succeed();
        });
    }

    /** EnderIO energy conduits (from an EnderIO creative power block) into an external filter, and into a light. */
    @GameTest(template = EMPTY, timeoutTicks = 200, batch = "bettacraft_power_enderio")
    public static void enderioConduitFeedsAFilterAndALight(GameTestHelper h) {
        if (!loaded("enderio")) {
            h.succeed();
            return;
        }
        PowerConfig saved = PowerConfig.current();
        ServerConfig.POWER_COSTS.set(ServerConfig.PowerCosts.HARD);
        TankBlockEntity a, b;
        try { // M52c: a failing setup (another EnderIO version) must not leave the power options on for later batches
            // conduit -> filter
            a = tank(h, 6, 1, 2, 1, 1, 1);
            filter(h, new BlockPos(5, 1, 2), Direction.EAST);
            h.setBlock(new BlockPos(2, 1, 2), creativePower());
            conduit(h, new BlockPos(3, 1, 2));
            conduit(h, new BlockPos(4, 1, 2));
            // creative power right next to a light on another tank
            b = tank(h, 4, 1, 6, 1, 1, 1);
            h.setBlock(new BlockPos(4, 2, 6), ModBlocks.TANK_LIGHT.get().defaultBlockState());
            h.setBlock(new BlockPos(5, 2, 6), creativePower());
        } catch (RuntimeException e) {
            saved.apply();
            throw e;
        }
        h.runAfterDelay(80, () -> {
            long viaFilter = a.getTank().powerBuffer(), viaLight = b.getTank().powerBuffer();
            saved.apply();
            h.assertTrue(viaFilter > 0, "EnderIO conduit delivers through the external filter, buffer " + viaFilter);
            h.assertTrue(viaLight > 0, "FE into the light reaches the tank below, buffer " + viaLight);
            h.succeed();
        });
    }

    /** EnderIO energy conduits (from an EnderIO creative power block) into a cabinet under the tank. */
    @GameTest(template = EMPTY, timeoutTicks = 200, batch = "bettacraft_power_enderio_cabinet")
    public static void enderioConduitFeedsACabinet(GameTestHelper h) {
        if (!loaded("enderio")) {
            h.succeed();
            return;
        }
        PowerConfig saved = PowerConfig.current();
        ServerConfig.POWER_COSTS.set(ServerConfig.PowerCosts.HARD);
        TankBlockEntity c;
        try { // M52c: a failing setup must not leave the power options on
            h.setBlock(new BlockPos(4, 1, 2), ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
            c = tank(h, 4, 2, 2, 1, 1, 1);
            h.setBlock(new BlockPos(1, 1, 2), creativePower());
            conduit(h, new BlockPos(2, 1, 2));
            conduit(h, new BlockPos(3, 1, 2));
        } catch (RuntimeException e) {
            saved.apply();
            throw e;
        }
        h.runAfterDelay(80, () -> {
            long got = c.getTank().powerBuffer();
            saved.apply();
            h.assertTrue(got > 0, "EnderIO conduit delivers through the cabinet, buffer " + got);
            h.succeed();
        });
    }

    /** Places an EnderIO energy conduit like a player would (EnderIO API, by reflection: no compile dependency). */
    private static void conduit(GameTestHelper h, BlockPos rel) {
        ServerLevel level = h.getLevel();
        try {
            var reg = level.registryAccess().lookupOrThrow(ResourceKey.createRegistryKey(Identifier.parse("enderio:conduit")));
            Holder<?> energy = reg.getOrThrow(ResourceKey.create(reg.key(), Identifier.parse("enderio:energy")));
            h.setBlock(rel, block("enderio:conduit"));
            Object be = level.getBlockEntity(h.absolutePos(rel));
            be.getClass().getMethod("addConduit", Holder.class, Direction.class, net.minecraft.world.entity.player.Player.class)
                    .invoke(be, energy, Direction.UP, MockPlayers.serverPlayer(h));
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new GameTestFailure("EnderIO API changed: " + e);
        }
    }
}
