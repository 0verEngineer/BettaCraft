package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.plumbing.LevelSensorBlockEntity;
import org.overengineer.bettacraft.plumbing.PipeBlock;
import org.overengineer.bettacraft.plumbing.PipeUpdates;
import org.overengineer.bettacraft.plumbing.PipeWrenchItem;
import org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/**
 * M68 part B: other mods' pipes, buses and tanks at the water change controller's lines (the replacements of the fresh and waste
 * water port tests of M40 part b). The rig: the controller at (11,2,4) facing north (fresh water inlet east, waste water outlet west,
 * the line to the tank south, the line from the tank up), our fresh water pipes at (12..13,2,4), the foreign block at (14,2,4), a
 * reservoir with water behind it at (15,2,4); a tank at (11..12,2..3,6..7) with both tank-side lines. Each test succeeds at once when
 * its mod is not loaded (the batches run with {@code -Ppower} / {@code -Pstorage} and in the integration suite).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WaterChangeForeignGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private WaterChangeForeignGameTests() {}

    private static boolean loaded(String... mods) {
        for (String m : mods) if (!org.overengineer.bettacraft.platform.Platform.get().isModLoaded(m)) return false;
        return true;
    }

    private static Block block(String id) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(net.minecraft.resources.Identifier.parse(id))
                .orElseThrow(() -> new GameTestFailure("no block " + id));
    }

    record Rig(WaterChangerBlockEntity changer, TankBlockEntity tank) {}

    /** The controller with its lines (see the class comment); {@code freshPipes}: our two fresh water pipes east of it. */
    static Rig rig(GameTestHelper h, boolean freshPipes) {
        TankBlockEntity c = PlumbingGameTests.tank(h, 11, 2, 6, 2, 2, 2);
        WaterChangerBlockEntity wc = WaterChangeGameTests.changer(h, 11, 2, 4, Direction.NORTH);
        WaterChangeGameTests.pipes(h, true, new int[]{11, 2, 5}); // fresh out: south into the tank
        WaterChangeGameTests.pipes(h, true, new int[]{11, 3, 4}, new int[]{11, 3, 5}); // waste in: up, then into the tank
        WaterChangeGameTests.apart(h, new int[]{11, 2, 5}, new int[]{11, 3, 5});
        if (freshPipes) WaterChangeGameTests.pipes(h, true, new int[]{12, 2, 4}, new int[]{13, 2, 4});
        PipeUpdates.flush(h.getLevel());
        wc.set(7, 25, true);
        return new Rig(wc, c);
    }

    /** Pipez: a fluid pipe extracting from a reservoir pushes into the controller's fresh water line (its inlet store fills). */
    @GameTest(template = EMPTY, batch = "bettacraft_power_pipez_water", timeoutTicks = 300)
    public static void pipezPipeFillsTheFreshLine(GameTestHelper h) {
        if (!loaded("pipez")) {
            h.succeed();
            return;
        }
        BlockState pz = block("pipez:fluid_pipe").defaultBlockState();
        for (var pr : pz.getProperties()) if (pr.getName().equals("has_data")) pz = pz.setValue((net.minecraft.world.level.block.state.properties.BooleanProperty) pr, true);
        Rig r = rig(h, true);
        PlumbingGameTests.reservoir(h, new BlockPos(15, 2, 4), 16000, null);
        h.setBlock(new BlockPos(14, 2, 4), pz);
        try {
            Object pipe = h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(14, 2, 4)));
            pipe.getClass().getMethod("setExtracting", Direction.class, boolean.class).invoke(pipe, Direction.EAST, true);
        } catch (ReflectiveOperationException | NullPointerException e) {
            throw new GameTestFailure("Pipez API changed: " + e);
        }
        h.succeedWhen(() -> h.assertTrue(r.changer().freshStore().amount() > 0, "the Pipez pipe fills the fresh water inlet: " + r.changer().freshStore().amount()));
    }

    /** EnderIO: a fluid conduit extracting from a reservoir pushes into the controller's fresh water line. */
    @GameTest(template = EMPTY, batch = "bettacraft_power_enderio_water", timeoutTicks = 300)
    public static void enderioConduitFillsTheFreshLine(GameTestHelper h) {
        if (!loaded("enderio")) {
            h.succeed();
            return;
        }
        ServerLevel level = h.getLevel();
        Rig r = rig(h, false);
        PlumbingGameTests.reservoir(h, new BlockPos(15, 2, 4), 16000, null);
        try {
            var reg = level.registryAccess().lookupOrThrow(net.minecraft.resources.ResourceKey.createRegistryKey(net.minecraft.resources.Identifier.parse("enderio:conduit")));
            net.minecraft.core.Holder<?> fluid = reg.getOrThrow(net.minecraft.resources.ResourceKey.create(reg.key(), net.minecraft.resources.Identifier.parse("enderio:fluid")));
            h.setBlock(new BlockPos(14, 2, 4), block("enderio:conduit"));
            Object be = level.getBlockEntity(h.absolutePos(new BlockPos(14, 2, 4)));
            be.getClass().getMethod("addConduit", net.minecraft.core.Holder.class, Direction.class, net.minecraft.world.entity.player.Player.class)
                    .invoke(be, fluid, Direction.UP, MockPlayers.serverPlayer(h));
            WaterChangeGameTests.pipes(h, true, new int[]{12, 2, 4}, new int[]{13, 2, 4});
            var get = be.getClass().getMethod("getConnectionConfig", net.minecraft.core.Holder.class, Direction.class);
            var set = be.getClass().getMethod("setConnectionConfig", net.minecraft.core.Holder.class, Direction.class,
                    Class.forName("com.enderio.enderio.api.conduits.connection.config.ConnectionConfig"));
            Object east = get.invoke(be, fluid, Direction.EAST);
            Object extract = east.getClass().getMethod("withIsExtract", boolean.class).invoke(east, true);
            Class<?> rc = Class.forName("com.enderio.enderio.api.io.RedstoneControl");
            extract = extract.getClass().getMethod("withExtractRedstoneControl", rc).invoke(extract, rc.getField("ALWAYS_ACTIVE").get(null));
            extract = extract.getClass().getMethod("withIsInsert", boolean.class).invoke(extract, false);
            set.invoke(be, fluid, Direction.EAST, extract);
            Object west = get.invoke(be, fluid, Direction.WEST);
            Object insert = west.getClass().getMethod("withIsInsert", boolean.class).invoke(west, true);
            set.invoke(be, fluid, Direction.WEST, insert);
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new GameTestFailure("EnderIO API changed: " + e);
        }
        h.succeedWhen(() -> h.assertTrue(r.changer().freshStore().amount() > 0, "the EnderIO conduit fills the fresh water inlet: " + r.changer().freshStore().amount()));
    }

    /** Mekanism: a mechanical pipe pulling from a reservoir pushes into the fresh water line, and our pipe joins it. */
    @GameTest(template = EMPTY, batch = "bettacraft_power_mekanism_water", timeoutTicks = 300)
    public static void mekanismPipeFillsTheFreshLine(GameTestHelper h) {
        if (!loaded("mekanism")) {
            h.succeed();
            return;
        }
        ServerLevel level = h.getLevel();
        Rig r = rig(h, true);
        PlumbingGameTests.reservoir(h, new BlockPos(15, 2, 4), 16000, null);
        h.setBlock(new BlockPos(14, 2, 4), block("mekanism:basic_mechanical_pipe"));
        try {
            Object tile = level.getBlockEntity(h.absolutePos(new BlockPos(14, 2, 4)));
            Object transmitter = tile.getClass().getMethod("getTransmitter").invoke(tile);
            Class<?> type = Class.forName("mekanism.common.lib.transmitter.ConnectionType");
            Object pull = type.getField("PULL").get(null);
            transmitter.getClass().getMethod("setConnectionTypeRaw", Direction.class, type).invoke(transmitter, Direction.EAST, pull);
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new GameTestFailure("Mekanism API changed: " + e);
        }
        h.succeedWhen(() -> {
            h.assertTrue(level.getBlockState(h.absolutePos(new BlockPos(13, 2, 4))).getValue(PipeBlock.SIDES.get(Direction.EAST)), "our fresh pipe joins the Mekanism pipe");
            h.assertTrue(r.changer().freshStore().amount() > 0, "Mekanism pulls from the reservoir into the fresh water inlet: " + r.changer().freshStore().amount());
        });
    }

    /** Mekanism: a fluid tank at the end of the fresh water line feeds a top-off (the controller pulls from it, no pipe of theirs). */
    @GameTest(template = EMPTY, batch = "bettacraft_power_mekanism_water", timeoutTicks = 300)
    public static void mekanismTankFeedsATopOff(GameTestHelper h) {
        if (!loaded("mekanism")) {
            h.succeed();
            return;
        }
        ServerLevel level = h.getLevel();
        Rig r = rig(h, true);
        h.setBlock(new BlockPos(14, 2, 4), block("mekanism:basic_fluid_tank"));
        BlockPos tp = h.absolutePos(new BlockPos(14, 2, 4));
        IFluidHandler tank = org.overengineer.bettacraft.platform.Platform.get().fluidHandler(level, tp, Direction.WEST);
        if (tank == null) throw new GameTestFailure("the Mekanism tank has no fluid handler");
        tank.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 8000), IFluidHandler.FluidAction.EXECUTE);
        TankBlockEntity c = r.tank();
        WaterChangeGameTests.evaporate(level, c, 700);
        LevelSensorBlockEntity s = WaterChangeGameTests.sensor(h, 13, 2, 6);
        h.succeedWhen(() -> {
            s.check(level);
            r.changer().check(level, false);
            h.assertTrue(c.getTank().waterMb() == c.getTank().capacityMb(), "topped off from the Mekanism tank: " + c.getTank().waterMb());
        });
    }

    /** AE2: an export bus with water in its config exports from the network (a storage bus on a reservoir) into the fresh water line. */
    @GameTest(template = EMPTY, batch = "bettacraft_storage_water", timeoutTicks = 600)
    public static void ae2ExportBusFillsTheFreshLine(GameTestHelper h) throws Exception {
        if (!loaded("ae2")) {
            h.succeed();
            return;
        }
        ServerLevel level = h.getLevel();
        Rig r = rig(h, true);
        PlumbingGameTests.reservoir(h, new BlockPos(16, 2, 4), 16000, null);
        java.lang.reflect.Method set = null;
        for (java.lang.reflect.Method m : Class.forName("appeng.api.parts.PartHelper").getMethods()) if (m.getName().equals("setPart") && m.getParameterCount() == 5) set = m;
        if (set == null) throw new GameTestFailure("AE2 API changed: no PartHelper.setPart");
        BlockPos a = h.absolutePos(new BlockPos(14, 2, 4)), b = h.absolutePos(new BlockPos(15, 2, 4));
        net.minecraft.world.item.Item cable = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("ae2:fluix_glass_cable"));
        net.minecraft.world.item.Item export = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("ae2:export_bus"));
        net.minecraft.world.item.Item storage = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("ae2:storage_bus"));
        set.invoke(null, level, a, null, null, cable);
        set.invoke(null, level, b, null, null, cable);
        Object bus = set.invoke(null, level, a, Direction.WEST, null, export);
        set.invoke(null, level, b, Direction.EAST, null, storage);
        if (bus == null) throw new GameTestFailure("AE2 did not place the export bus");
        java.lang.reflect.Method getConfig = bus.getClass().getMethod("getConfig");
        getConfig.setAccessible(true);
        Object config = getConfig.invoke(bus);
        Object key = Class.forName("appeng.api.stacks.AEFluidKey").getMethod("of", net.minecraft.world.level.material.Fluid.class).invoke(null, net.minecraft.world.level.material.Fluids.WATER);
        Class<?> aeKey = Class.forName("appeng.api.stacks.AEKey");
        Object stack = Class.forName("appeng.api.stacks.GenericStack").getConstructor(aeKey, long.class).newInstance(key, 1L);
        for (java.lang.reflect.Method m : config.getClass().getMethods()) {
            if (m.getName().equals("setStack") && m.getParameterCount() == 2) {
                m.setAccessible(true);
                m.invoke(config, 0, stack);
            }
        }
        h.setBlock(new BlockPos(14, 3, 4), block("ae2:creative_energy_cell"));
        h.succeedWhen(() -> h.assertTrue(r.changer().freshStore().amount() > 0, "the AE2 export bus fills the fresh water inlet: " + r.changer().freshStore().amount()));
    }

    /**
     * Refined Storage: an importer right at the controller's waste water outlet pulls the old water into the network (the importer looks
     * at what a handler holds; our pipes hold nothing, so it sits at the outlet itself, which shows its store).
     */
    @GameTest(template = EMPTY, batch = "bettacraft_storage_water", timeoutTicks = 600)
    public static void refinedStorageImporterEmptiesTheWasteLine(GameTestHelper h) {
        if (!loaded("refinedstorage")) {
            h.succeed();
            return;
        }
        Rig r = rig(h, false);
        r.changer().wasteStore().fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
        BlockState importer = block("refinedstorage:importer").defaultBlockState();
        for (var p : importer.getProperties()) {
            if (p.getName().equals("direction")) importer = with(importer, p, "east");
        }
        h.setBlock(new BlockPos(10, 2, 4), importer); // at the waste water outlet (west)
        h.setBlock(new BlockPos(9, 2, 4), block("refinedstorage:creative_controller").defaultBlockState());
        h.setBlock(new BlockPos(9, 3, 4), block("refinedstorage:creative_fluid_storage_block").defaultBlockState()); // fluids need a fluid storage
        h.succeedWhen(() -> h.assertTrue(r.changer().wasteStore().amount() < 4000, "the RS importer pulls from the waste water line: " + r.changer().wasteStore().amount()));
    }

    private static <T extends Comparable<T>> BlockState with(BlockState s, net.minecraft.world.level.block.state.properties.Property<T> p, String value) {
        return p.getValue(value).map(v -> s.setValue(p, v)).orElse(s);
    }
}
