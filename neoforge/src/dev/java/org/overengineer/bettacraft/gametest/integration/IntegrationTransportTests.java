package org.overengineer.bettacraft.gametest.integration;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import static org.overengineer.bettacraft.gametest.integration.IntegrationRig.*;

import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.equipment.FeederBlockEntity;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/**
 * M42 integration suite: the item, fluid and energy transport of other mods into our blocks through their plain
 * capabilities. Items: pellets from a chest into a feeder (whose capability takes food only; a stack of dirt next to the
 * pellets must stay in the chest). Fluids: water into a tank. Energy: FE into a tank (filter power option on). Each mod's
 * own blocks, set up through its classes by reflection the way its GUI would.
 */
@PrefixGameTestTemplate(false)
public final class IntegrationTransportTests {
    private static final String EMPTY = IntegrationSuite.EMPTY;

    private IntegrationTransportTests() {}

    /** Chest (1,2,2) with 16 pellets and 16 dirt; the test's transport; the feeder at {@code feederAt}. */
    private static ChestBlockEntity pelletChest(GameTestHelper h) {
        return chest(h, new BlockPos(1, 2, 2), new ItemStack(ModItems.BETTA_PELLETS.get(), 16), new ItemStack(Items.DIRT, 16));
    }

    private static void pelletsArrive(GameTestHelper h, ChestBlockEntity c, FeederBlockEntity f, String who) {
        h.succeedWhen(() -> {
            h.assertTrue(count(f, ModItems.BETTA_PELLETS.get()) == 16, who + " moved all pellets into the feeder: " + count(f, ModItems.BETTA_PELLETS.get()));
            h.assertTrue(count(c, Items.DIRT) == 16, who + " left the dirt in the chest (the feeder takes food only): " + count(c, Items.DIRT));
            h.assertTrue(count(f, Items.DIRT) == 0, "no dirt in the feeder");
        });
    }

    // ------------------------------------------------------------------ Pipez

    private static BlockState pipez(String id) {
        return with(block(id).defaultBlockState(), "has_data", "true");
    }

    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_pipez", timeoutTicks = 900)
    public static void pipezItemPipeFillsAFeeder(GameTestHelper h) {
        if (IntegrationSuite.lacks(h, "pipez")) return;
        ChestBlockEntity c = pelletChest(h);
        h.setBlock(new BlockPos(2, 2, 2), pipez("pipez:item_pipe"));
        FeederBlockEntity f = feeder(h, new BlockPos(3, 2, 2));
        call(be(h, new BlockPos(2, 2, 2)), "setExtracting", Direction.WEST, true);
        pelletsArrive(h, c, f, "the Pipez item pipe");
    }

    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_pipez", timeoutTicks = 900)
    public static void pipezFluidPipeFillsATank(GameTestHelper h) {
        if (IntegrationSuite.lacks(h, "pipez")) return;
        org.overengineer.bettacraft.gametest.PlumbingGameTestsAccess.reservoir(h, new BlockPos(1, 2, 2), 16000);
        h.setBlock(new BlockPos(2, 2, 2), pipez("pipez:fluid_pipe"));
        TankBlockEntity t = tank(h, 3, 2, 2, 2, 2, 2, false);
        call(be(h, new BlockPos(2, 2, 2)), "setExtracting", Direction.WEST, true);
        h.succeedWhen(() -> h.assertTrue(t.getTank().waterMb() > 0, "the Pipez fluid pipe fills the tank: " + t.getTank().waterMb()));
    }

    // ------------------------------------------------------------------ EnderIO

    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_enderio", timeoutTicks = 900)
    public static void enderioItemConduitFillsAFeeder(GameTestHelper h) throws Exception {
        if (IntegrationSuite.lacks(h, "enderio")) return;
        ServerLevel level = h.getLevel();
        ChestBlockEntity c = pelletChest(h);
        FeederBlockEntity f = feeder(h, new BlockPos(3, 2, 2));
        var reg = level.registryAccess().lookupOrThrow(ResourceKey.createRegistryKey(Identifier.parse("enderio:conduit")));
        Holder<?> conduit = reg.getOrThrow(ResourceKey.create(reg.key(), Identifier.parse("enderio:item")));
        h.setBlock(new BlockPos(2, 2, 2), block("enderio:conduit"));
        Object be = be(h, new BlockPos(2, 2, 2));
        be.getClass().getMethod("addConduit", Holder.class, Direction.class, Player.class).invoke(be, conduit, Direction.UP,
                org.overengineer.bettacraft.gametest.PlumbingGameTestsAccess.player(h));
        Method get = be.getClass().getMethod("getConnectionConfig", Holder.class, Direction.class);
        Method set = be.getClass().getMethod("setConnectionConfig", Holder.class, Direction.class,
                Class.forName("com.enderio.enderio.api.conduits.connection.config.ConnectionConfig"));
        Object west = get.invoke(be, conduit, Direction.WEST);
        if (west == null) throw new GameTestFailure("the item conduit did not connect to the chest");
        Class<?> rc = Class.forName("com.enderio.enderio.api.io.RedstoneControl");
        Object extract = call(west, "withIsExtract", true);
        extract = call(extract, "withExtractRedstoneControl", rc.getField("ALWAYS_ACTIVE").get(null));
        extract = call(extract, "withIsInsert", false);
        set.invoke(be, conduit, Direction.WEST, extract);
        Object east = get.invoke(be, conduit, Direction.EAST);
        if (east == null) throw new GameTestFailure("the item conduit did not connect to the feeder");
        set.invoke(be, conduit, Direction.EAST, call(call(east, "withIsInsert", true), "withIsExtract", false));
        pelletsArrive(h, c, f, "the EnderIO item conduit");
    }

    // ------------------------------------------------------------------ Create

    /** A chute with pellets above a feeder drops them into it (and keeps dirt, which the feeder refuses). */
    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_create", timeoutTicks = 200)
    public static void createChuteFillsAFeeder(GameTestHelper h) {
        if (IntegrationSuite.lacks(h, "create")) return;
        FeederBlockEntity f = feeder(h, new BlockPos(2, 2, 2));
        h.setBlock(new BlockPos(2, 3, 2), block("create:chute"));
        h.setBlock(new BlockPos(4, 2, 2), org.overengineer.bettacraft.registry.ModBlocks.FEEDER.get().defaultBlockState());
        FeederBlockEntity f2 = (FeederBlockEntity) be(h, new BlockPos(4, 2, 2));
        h.setBlock(new BlockPos(4, 3, 2), block("create:chute"));
        call(be(h, new BlockPos(2, 3, 2)), "setItem", new ItemStack(ModItems.BETTA_PELLETS.get(), 8));
        call(be(h, new BlockPos(4, 3, 2)), "setItem", new ItemStack(Items.DIRT, 8));
        h.succeedWhen(() -> {
            h.assertTrue(count(f, ModItems.BETTA_PELLETS.get()) == 8, "the chute put the pellets into the feeder: " + count(f, ModItems.BETTA_PELLETS.get()));
            h.assertTrue(count(f2, Items.DIRT) == 0, "no dirt in the feeder");
            ItemStack held = (ItemStack) call(be(h, new BlockPos(4, 3, 2)), "getItem");
            h.assertTrue(held.is(Items.DIRT) && held.getCount() == 8, "the chute keeps the dirt: " + held);
        });
    }

    // ------------------------------------------------------------------ Applied Energistics 2

    private static Object aePart(ServerLevel level, BlockPos abs, Direction side, String item) throws Exception {
        Method set = null;
        for (Method m : Class.forName("appeng.api.parts.PartHelper").getMethods()) if (m.getName().equals("setPart") && m.getParameterCount() == 5) set = m;
        if (set == null) throw new GameTestFailure("AE2 API changed: no PartHelper.setPart");
        Object part = set.invoke(null, level, abs, side, null, item(item));
        if (part == null) throw new GameTestFailure("AE2 did not place " + item);
        return part;
    }

    private static void aeConfig(Object bus, Object key) throws Exception {
        Object config = call(bus, "getConfig");
        Class<?> aeKey = Class.forName("appeng.api.stacks.AEKey");
        Object stack = Class.forName("appeng.api.stacks.GenericStack").getConstructor(aeKey, long.class).newInstance(key, 1L);
        call(config, "setStack", 0, stack);
    }

    /** An export bus with pellets in its filter, from a chest behind a storage bus into a feeder. */
    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_ae2", timeoutTicks = 900)
    public static void ae2ExportBusFillsAFeeder(GameTestHelper h) throws Exception {
        if (IntegrationSuite.lacks(h, "ae2")) return;
        ServerLevel level = h.getLevel();
        FeederBlockEntity f = feeder(h, new BlockPos(2, 2, 2));
        ChestBlockEntity c = chest(h, new BlockPos(5, 2, 2), new ItemStack(ModItems.BETTA_PELLETS.get(), 16), new ItemStack(Items.DIRT, 16));
        BlockPos a = h.absolutePos(new BlockPos(3, 2, 2)), b = h.absolutePos(new BlockPos(4, 2, 2));
        aePart(level, a, null, "ae2:fluix_glass_cable");
        aePart(level, b, null, "ae2:fluix_glass_cable");
        Object export = aePart(level, a, Direction.WEST, "ae2:export_bus");
        aePart(level, b, Direction.EAST, "ae2:storage_bus");
        aeConfig(export, Class.forName("appeng.api.stacks.AEItemKey").getMethod("of", ItemLike.class).invoke(null, ModItems.BETTA_PELLETS.get()));
        h.setBlock(new BlockPos(3, 3, 2), block("ae2:creative_energy_cell"));
        h.succeedWhen(() -> {
            h.assertTrue(count(f, ModItems.BETTA_PELLETS.get()) == 16, "the export bus moved all pellets into the feeder: " + count(f, ModItems.BETTA_PELLETS.get()));
            h.assertTrue(count(c, Items.DIRT) == 16, "the dirt stays");
        });
    }

    /** An export bus with water in its filter, from a reservoir behind a storage bus into a tank. */
    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_ae2", timeoutTicks = 900)
    public static void ae2ExportBusFillsATankWithWater(GameTestHelper h) throws Exception {
        if (IntegrationSuite.lacks(h, "ae2")) return;
        ServerLevel level = h.getLevel();
        TankBlockEntity t = tank(h, 1, 2, 2, 2, 2, 2, false);
        org.overengineer.bettacraft.gametest.PlumbingGameTestsAccess.reservoir(h, new BlockPos(6, 2, 2), 16000);
        BlockPos a = h.absolutePos(new BlockPos(3, 2, 2)), b = h.absolutePos(new BlockPos(4, 2, 2)), c = h.absolutePos(new BlockPos(5, 2, 2));
        aePart(level, a, null, "ae2:fluix_glass_cable");
        aePart(level, b, null, "ae2:fluix_glass_cable");
        aePart(level, c, null, "ae2:fluix_glass_cable");
        Object export = aePart(level, a, Direction.WEST, "ae2:export_bus");
        aePart(level, c, Direction.EAST, "ae2:storage_bus");
        aeConfig(export, Class.forName("appeng.api.stacks.AEFluidKey").getMethod("of", net.minecraft.world.level.material.Fluid.class)
                .invoke(null, net.minecraft.world.level.material.Fluids.WATER));
        h.setBlock(new BlockPos(4, 3, 2), block("ae2:creative_energy_cell"));
        h.succeedWhen(() -> h.assertTrue(t.getTank().waterMb() > 0, "the export bus fills the tank: " + t.getTank().waterMb()));
    }

    // ------------------------------------------------------------------ Refined Storage

    /** An exporter with pellets in its filter, from a chest behind an external storage into a feeder. */
    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_rs", timeoutTicks = 900)
    public static void refinedStorageExporterFillsAFeeder(GameTestHelper h) throws Exception {
        if (IntegrationSuite.lacks(h, "refinedstorage")) return;
        FeederBlockEntity f = feeder(h, new BlockPos(2, 2, 2));
        h.setBlock(new BlockPos(3, 2, 2), with(block("refinedstorage:exporter").defaultBlockState(), "direction", "west"));
        h.setBlock(new BlockPos(4, 2, 2), block("refinedstorage:creative_controller").defaultBlockState());
        h.setBlock(new BlockPos(5, 2, 2), with(block("refinedstorage:external_storage").defaultBlockState(), "direction", "east"));
        ChestBlockEntity c = chest(h, new BlockPos(6, 2, 2), new ItemStack(ModItems.BETTA_PELLETS.get(), 16), new ItemStack(Items.DIRT, 16));
        Object exporter = be(h, new BlockPos(3, 2, 2));
        Object key = Class.forName("com.refinedmods.refinedstorage.common.support.resource.ItemResource")
                .getConstructor(net.minecraft.world.item.Item.class).newInstance(ModItems.BETTA_PELLETS.get());
        call(exporter, "setFilters", java.util.List.of(key));
        h.succeedWhen(() -> {
            h.assertTrue(count(f, ModItems.BETTA_PELLETS.get()) == 16, "the exporter moved all pellets into the feeder: " + count(f, ModItems.BETTA_PELLETS.get())
                    + ", chest " + count(c, ModItems.BETTA_PELLETS.get()));
            h.assertTrue(count(c, Items.DIRT) == 16, "the dirt stays");
        });
    }

    /** An exporter with water in its filter, from a reservoir behind an external storage into a tank. */
    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_rs", timeoutTicks = 900)
    public static void refinedStorageExporterFillsATankWithWater(GameTestHelper h) throws Exception {
        if (IntegrationSuite.lacks(h, "refinedstorage")) return;
        TankBlockEntity t = tank(h, 1, 2, 2, 2, 2, 2, false);
        h.setBlock(new BlockPos(3, 2, 2), with(block("refinedstorage:exporter").defaultBlockState(), "direction", "west"));
        h.setBlock(new BlockPos(4, 2, 2), block("refinedstorage:creative_controller").defaultBlockState());
        h.setBlock(new BlockPos(5, 2, 2), with(block("refinedstorage:external_storage").defaultBlockState(), "direction", "east"));
        org.overengineer.bettacraft.gametest.PlumbingGameTestsAccess.reservoir(h, new BlockPos(6, 2, 2), 16000);
        Object key = Class.forName("com.refinedmods.refinedstorage.common.support.resource.FluidResource")
                .getConstructor(net.minecraft.world.level.material.Fluid.class).newInstance(net.minecraft.world.level.material.Fluids.WATER);
        call(be(h, new BlockPos(3, 2, 2)), "setFilters", java.util.List.of(key));
        h.succeedWhen(() -> h.assertTrue(t.getTank().waterMb() > 0, "the exporter fills the tank: " + t.getTank().waterMb()));
    }

    // ------------------------------------------------------------------ LaserIO

    /** Two laser nodes linked; an extract card on the source side, an insert card on the target side. */
    private static void laser(GameTestHelper h, BlockPos a, Direction aSide, BlockPos b, Direction bSide, String card) {
        h.setBlock(a, block("laserio:laser_node"));
        h.setBlock(b, block("laserio:laser_node"));
        Object na = be(h, a), nb = be(h, b);
        call(na, "handleConnection", nb);
        ItemStack extract = new ItemStack(item(card)), insert = new ItemStack(item(card));
        callStatic("com.direwolf20.laserio.common.items.cards.BaseCard", "setTransferMode", extract, (byte) 1); // EXTRACT
        callStatic("com.direwolf20.laserio.common.items.cards.BaseCard", "setTransferMode", insert, (byte) 0); // INSERT
        Object[] caches = (Object[]) field(na, "nodeSideCaches");
        call(field(caches[aSide.ordinal()], "itemHandler"), "setStackInSlot", 0, extract);
        Object[] cachesB = (Object[]) field(nb, "nodeSideCaches");
        call(field(cachesB[bSide.ordinal()], "itemHandler"), "setStackInSlot", 0, insert);
        call(na, "updateThisNode");
        call(nb, "updateThisNode");
    }

    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_laserio", timeoutTicks = 900)
    public static void laserioItemCardsFillAFeeder(GameTestHelper h) {
        if (IntegrationSuite.lacks(h, "laserio")) return;
        ChestBlockEntity c = pelletChest(h);
        FeederBlockEntity f = feeder(h, new BlockPos(6, 2, 2));
        laser(h, new BlockPos(2, 2, 2), Direction.WEST, new BlockPos(5, 2, 2), Direction.EAST, "laserio:card_item");
        pelletsArrive(h, c, f, "LaserIO");
    }

    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_laserio", timeoutTicks = 900)
    public static void laserioFluidCardsFillATank(GameTestHelper h) {
        if (IntegrationSuite.lacks(h, "laserio")) return;
        org.overengineer.bettacraft.gametest.PlumbingGameTestsAccess.reservoir(h, new BlockPos(1, 2, 2), 16000);
        TankBlockEntity t = tank(h, 6, 2, 2, 2, 2, 2, false);
        laser(h, new BlockPos(2, 2, 2), Direction.WEST, new BlockPos(5, 2, 2), Direction.EAST, "laserio:card_fluid");
        h.succeedWhen(() -> h.assertTrue(t.getTank().waterMb() > 0, "LaserIO fills the tank: " + t.getTank().waterMb()));
    }

    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_laserio_power", timeoutTicks = 900)
    public static void laserioEnergyCardsPowerATank(GameTestHelper h) {
        if (IntegrationSuite.lacks(h, "laserio", "powah")) return;
        Runnable restore = powerOn();
        TankBlockEntity t;
        try {
            h.setBlock(new BlockPos(1, 2, 2), block("powah:energy_cell_creative"));
            t = tank(h, 6, 2, 2, 1, 1, 1, true);
            laser(h, new BlockPos(2, 2, 2), Direction.WEST, new BlockPos(5, 2, 2), Direction.EAST, "laserio:card_energy");
        } catch (RuntimeException e) {
            restore.run();
            throw e;
        }
        h.runAfterDelay(120, () -> {
            long got = t.getTank().powerBuffer();
            restore.run();
            h.assertTrue(got > 0, "LaserIO delivers FE to the tank, buffer " + got);
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ Modular Routers

    private static ItemStack routerModule(String id) {
        ItemStack s = new ItemStack(item(id));
        try {
            Class<?> settings = Class.forName("me.desht.modularrouters.logic.settings.ModuleSettings");
            Object def = settings.getField("DEFAULT").get(null);
            Object front = enumValue("me.desht.modularrouters.logic.settings.RelativeDirection", "FRONT");
            Object value = settings.getConstructors()[0].newInstance(call(def, "flags"), front, call(def, "termination"), call(def, "redstoneBehaviour"), call(def, "regulatorAmount"));
            @SuppressWarnings("unchecked")
            DataComponentType<Object> type = (DataComponentType<Object>) ((java.util.function.Supplier<?>) staticField("me.desht.modularrouters.core.ModDataComponents", "COMMON_MODULE_SETTINGS")).get();
            s.set(type, value);
        } catch (ReflectiveOperationException e) {
            throw new GameTestFailure("Modular Routers API changed: " + e);
        }
        return s;
    }

    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_routers", timeoutTicks = 900)
    public static void modularRouterSenderFillsAFeeder(GameTestHelper h) {
        if (IntegrationSuite.lacks(h, "modularrouters")) return;
        h.setBlock(new BlockPos(2, 2, 2), with(block("modularrouters:modular_router").defaultBlockState(), "facing", "east"));
        FeederBlockEntity f = feeder(h, new BlockPos(3, 2, 2));
        h.setBlock(new BlockPos(2, 2, 4), with(block("modularrouters:modular_router").defaultBlockState(), "facing", "east"));
        FeederBlockEntity f2 = feeder(h, new BlockPos(3, 2, 4));
        Object router = be(h, new BlockPos(2, 2, 2)), router2 = be(h, new BlockPos(2, 2, 4));
        call(call(router, "getModules"), "setStackInSlot", 0, routerModule("modularrouters:sender_module_1"));
        call(call(router, "getBuffer"), "insertItem", 0, new ItemStack(ModItems.BETTA_PELLETS.get(), 16), false);
        call(call(router2, "getModules"), "setStackInSlot", 0, routerModule("modularrouters:sender_module_1"));
        call(call(router2, "getBuffer"), "insertItem", 0, new ItemStack(Items.DIRT, 16), false);
        h.succeedWhen(() -> {
            h.assertTrue(count(f, ModItems.BETTA_PELLETS.get()) == 16, "the sender module moved all pellets into the feeder: " + count(f, ModItems.BETTA_PELLETS.get()));
            h.assertTrue(count(f2, Items.DIRT) == 0, "no dirt in the other feeder");
            ItemStack left = (ItemStack) call(router2, "getBufferItemStack");
            h.assertTrue(left.is(Items.DIRT) && left.getCount() == 16, "the router keeps the dirt: " + left);
        });
    }

    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_routers", timeoutTicks = 900)
    public static void modularRouterFluidModuleFillsATank(GameTestHelper h) {
        if (IntegrationSuite.lacks(h, "modularrouters")) return;
        h.setBlock(new BlockPos(2, 2, 2), with(block("modularrouters:modular_router").defaultBlockState(), "facing", "east"));
        TankBlockEntity t = tank(h, 3, 2, 2, 2, 2, 2, false);
        Object router = be(h, new BlockPos(2, 2, 2));
        ItemStack module = routerModule("modularrouters:fluid_module");
        try {
            Class<?> fs = Class.forName("me.desht.modularrouters.logic.compiled.CompiledFluidModule$FluidModuleSettings");
            Object def = fs.getField("DEFAULT").get(null);
            Object out = enumValue("me.desht.modularrouters.logic.settings.TransferDirection", "FROM_ROUTER");
            Object value = fs.getConstructors()[0].newInstance(call(def, "maxTransfer"), out, call(def, "forceEmpty"), call(def, "regulateAbsolute"));
            @SuppressWarnings("unchecked")
            DataComponentType<Object> type = (DataComponentType<Object>) ((java.util.function.Supplier<?>) staticField("me.desht.modularrouters.core.ModDataComponents", "FLUID_SETTINGS")).get();
            module.set(type, value);
        } catch (ReflectiveOperationException e) {
            throw new GameTestFailure("Modular Routers API changed: " + e);
        }
        call(call(router, "getModules"), "setStackInSlot", 0, module);
        call(call(router, "getBuffer"), "insertItem", 0, new ItemStack(Items.WATER_BUCKET), false);
        h.succeedWhen(() -> {
            h.assertTrue(t.getTank().waterMb() >= 1000, "the fluid module emptied the bucket into the tank: " + t.getTank().waterMb());
            h.assertTrue(((ItemStack) call(router, "getBufferItemStack")).is(Items.BUCKET), "an empty bucket is left");
        });
    }

    // ------------------------------------------------------------------ XNet

    /**
     * An XNet controller between two connectors ({@code from} -- connector -- controller -- connector -- {@code to} along x),
     * placed like a player would (the cable network is made in setPlacedBy), one channel of the type with an extracting
     * connector at {@code from} and an inserting one at {@code to}; the controller gets FE from a creative cell above it.
     */
    private static void xnet(GameTestHelper h, String type, String modeField, BlockPos from, BlockPos to) {
        BlockPos c1 = from.east(), ctrl = c1.east(), c2 = ctrl.east();
        if (!c2.east().equals(to)) throw new IllegalArgumentException("the target must be east of the second connector");
        place(h, ctrl, "xnet:controller");
        place(h, c1, "xnet:connector");
        place(h, c2, "xnet:connector");
        h.setBlock(ctrl.above(), block("powah:energy_cell_creative"));
        Object controller = be(h, ctrl);
        call(controller, "createChannel", 0, type);
        try {
            Class<?> sided = Class.forName("mcjty.rftoolsbase.api.xnet.keys.SidedPos");
            java.lang.reflect.Constructor<?> sp = sided.getConstructor(BlockPos.class, Direction.class);
            Object ext = call(controller, "createConnector", 0, sp.newInstance(h.absolutePos(from), Direction.EAST));
            Object ins = call(controller, "createConnector", 0, sp.newInstance(h.absolutePos(to), Direction.WEST));
            setField(call(ext, "getConnectorSettings"), modeField, enumValue("mcjty.xnet.apiimpl.enums.InsExtMode", "EXT"));
            setField(call(ins, "getConnectorSettings"), modeField, enumValue("mcjty.xnet.apiimpl.enums.InsExtMode", "INS"));
        } catch (ReflectiveOperationException e) {
            throw new GameTestFailure("XNet API changed: " + e);
        }
        call(controller, "markAsDirty");
    }

    /** A block placed with its placement hook (setPlacedBy), like a player would place it. */
    private static void place(GameTestHelper h, BlockPos rel, String id) {
        BlockState s = block(id).defaultBlockState();
        h.setBlock(rel, s);
        BlockPos abs = h.absolutePos(rel);
        s.getBlock().setPlacedBy(h.getLevel(), abs, h.getLevel().getBlockState(abs), null, new ItemStack(s.getBlock()));
    }

    private static void setField(Object o, String name, Object value) {
        for (Class<?> k = o.getClass(); k != null; k = k.getSuperclass()) {
            try {
                java.lang.reflect.Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                f.set(o, value);
                return;
            } catch (NoSuchFieldException ignored) {
                // the super class
            } catch (IllegalAccessException e) {
                throw new GameTestFailure("field " + name + ": " + e);
            }
        }
        throw new GameTestFailure("API changed: no field " + o.getClass().getName() + "." + name);
    }

    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_xnet", timeoutTicks = 900)
    public static void xnetItemChannelFillsAFeeder(GameTestHelper h) {
        if (IntegrationSuite.lacks(h, "xnet", "powah")) return;
        ChestBlockEntity c = pelletChest(h);
        FeederBlockEntity f = feeder(h, new BlockPos(5, 2, 2));
        xnet(h, "xnet.item", "itemMode", new BlockPos(1, 2, 2), new BlockPos(5, 2, 2));
        pelletsArrive(h, c, f, "XNet");
    }

    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_xnet", timeoutTicks = 900)
    public static void xnetFluidChannelFillsATank(GameTestHelper h) {
        if (IntegrationSuite.lacks(h, "xnet", "powah")) return;
        org.overengineer.bettacraft.gametest.PlumbingGameTestsAccess.reservoir(h, new BlockPos(1, 2, 2), 16000);
        TankBlockEntity t = tank(h, 5, 2, 2, 2, 2, 2, false);
        xnet(h, "xnet.fluid", "fluidMode", new BlockPos(1, 2, 2), new BlockPos(5, 2, 2));
        h.succeedWhen(() -> h.assertTrue(t.getTank().waterMb() > 0, "XNet fills the tank: " + t.getTank().waterMb()));
    }

    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_xnet_power", timeoutTicks = 400)
    public static void xnetEnergyChannelPowersATank(GameTestHelper h) {
        if (IntegrationSuite.lacks(h, "xnet", "powah")) return;
        Runnable restore = powerOn();
        TankBlockEntity t;
        try {
            h.setBlock(new BlockPos(1, 2, 2), block("powah:energy_cell_creative"));
            t = tank(h, 5, 2, 2, 1, 1, 1, true);
            xnet(h, "xnet.energy", "energyMode", new BlockPos(1, 2, 2), new BlockPos(5, 2, 2));
        } catch (RuntimeException e) {
            restore.run();
            throw e;
        }
        h.runAfterDelay(200, () -> {
            long got = t.getTank().powerBuffer();
            restore.run();
            h.assertTrue(got > 0, "XNet delivers FE to the tank, buffer " + got);
            h.succeed();
        });
    }
}
