package org.overengineer.bettacraft.gametest.integration;

import static org.overengineer.bettacraft.gametest.integration.IntegrationRig.*;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.equipment.FeederBlockEntity;
import org.overengineer.bettacraft.gametest.PlumbingGameTestsAccess;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/**
 * M42 integration suite: Integrated Dynamics with Integrated Tunnels. Two cables; an interface part on the source (it puts the
 * source into the network), an exporter part on the target with a variable card "true" (made like the logic programmer does,
 * a constant boolean) in its "export all" aspect: items from a chest into a feeder, water from a reservoir into a tank, FE
 * from a creative cell into a tank. Only the mods' classes by reflection.
 */
@PrefixGameTestTemplate(false)
public final class IntegrationTunnelsTests {
    private static final String EMPTY = IntegrationSuite.EMPTY;
    private static final String TUNNELS = "org.cyclops.integratedtunnels.part.PartTypes";

    private IntegrationTunnelsTests() {}

    /** A cable at {@code rel}, added like a player places it (network and connections). */
    private static void cable(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel, block("integrateddynamics:cable"));
        callStatic("org.cyclops.integrateddynamics.core.helper.CableHelpers", "onCableAdded", h.getLevel(), h.absolutePos(rel));
    }

    private static Object partType(String field) {
        return staticField(TUNNELS, field);
    }

    private static void addPart(GameTestHelper h, BlockPos rel, Direction side, Object type) {
        Object item = call(type, "getItem");
        boolean ok = (boolean) callStatic("org.cyclops.integrateddynamics.core.helper.PartHelpers", "addPart", h.getLevel(), h.absolutePos(rel), side,
                type, new ItemStack((net.minecraft.world.item.Item) item));
        if (!ok) throw new GameTestAssertException("Integrated Dynamics did not add the part " + type);
    }

    /** A variable card holding the constant {@code true} (a value type variable facade, as the logic programmer writes). */
    private static ItemStack trueCard() {
        try {
            Object boolType = Class.forName("org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes").getField("BOOLEAN").get(null);
            Object value = callStatic("org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeBoolean$ValueBoolean", "of", true);
            Class<?> facadeClass = Class.forName("org.cyclops.integrateddynamics.core.item.ValueTypeVariableFacade");
            Object facade = facadeClass.getConstructor(boolean.class, Class.forName("org.cyclops.integrateddynamics.api.evaluate.variable.IValueType"),
                    Class.forName("org.cyclops.integrateddynamics.api.evaluate.variable.IValue")).newInstance(true, boolType, value);
            Object handler = callStatic("org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeRegistry", "getInstance");
            Object registry = callStatic("org.cyclops.integrateddynamics.core.item.VariableFacadeHandlerRegistry", "getInstance");
            return (ItemStack) call(registry, "writeVariableFacadeItem", new ItemStack(item("integrateddynamics:variable")), facade, handler);
        } catch (ReflectiveOperationException e) {
            throw new GameTestAssertException("Integrated Dynamics API changed: " + e);
        }
    }

    /** Puts the card into the boolean "export" aspect of the exporter at rel/side and activates it; gives the aspect names. */
    private static String activate(GameTestHelper h, BlockPos rel, Direction side) {
        ServerLevel level = h.getLevel();
        Object container = ((java.util.Optional<?>) callStatic("org.cyclops.integrateddynamics.core.helper.PartHelpers", "getPartContainer",
                (Level) level, h.absolutePos(rel), side)).orElseThrow(() -> new GameTestAssertException("no part container"));
        Object state = call(container, "getPartState", side);
        Object type = ((java.util.Map<?, ?>) call(container, "getParts")).get(side);
        @SuppressWarnings("unchecked")
        List<Object> aspects = (List<Object>) call(type, "getWriteAspects");
        Object boolType;
        try {
            boolType = Class.forName("org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes").getField("BOOLEAN").get(null);
        } catch (ReflectiveOperationException e) {
            throw new GameTestAssertException("Integrated Dynamics API changed: " + e);
        }
        List<String> names = new ArrayList<>();
        int index = -1;
        for (int i = 0; i < aspects.size(); i++) {
            String name = call(aspects.get(i), "getUniqueName").toString();
            names.add(name);
            if (index < 0 && call(aspects.get(i), "getValueType") == boolType && name.contains("export")) index = i;
        }
        if (index < 0) throw new GameTestAssertException("no boolean export aspect in " + names);
        call(call(state, "getInventory"), "setItem", index, trueCard());
        Object target = callStatic("org.cyclops.integrateddynamics.api.part.PartTarget", "fromCenter", (Level) level, h.absolutePos(rel), side);
        call(type, "updateActivation", target, state, null);
        return names.get(index);
    }

    /** source -- cable(interface toward the source) -- cable(exporter toward the target) -- target, along x. */
    private static String rig(GameTestHelper h, String interfacePart, String exporterPart) {
        cable(h, new BlockPos(2, 2, 2));
        cable(h, new BlockPos(3, 2, 2));
        addPart(h, new BlockPos(2, 2, 2), Direction.WEST, partType(interfacePart));
        addPart(h, new BlockPos(3, 2, 2), Direction.EAST, partType(exporterPart));
        callStatic("org.cyclops.integrateddynamics.core.helper.NetworkHelpers", "initNetwork", h.getLevel(), h.absolutePos(new BlockPos(2, 2, 2)), Direction.EAST);
        return activate(h, new BlockPos(3, 2, 2), Direction.EAST);
    }

    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_tunnels", timeoutTicks = 900)
    public static void integratedTunnelsExporterFillsAFeeder(GameTestHelper h) {
        IntegrationSuite.require("integrateddynamics", "integratedtunnels");
        ChestBlockEntity c = chest(h, new BlockPos(1, 2, 2), new ItemStack(ModItems.BETTA_PELLETS.get(), 16), new ItemStack(Items.DIRT, 16));
        FeederBlockEntity f = feeder(h, new BlockPos(4, 2, 2));
        String aspect = rig(h, "INTERFACE_ITEM", "EXPORTER_ITEM");
        h.succeedWhen(() -> {
            h.assertTrue(count(f, ModItems.BETTA_PELLETS.get()) == 16, "the exporter (" + aspect + ") moved all pellets: " + count(f, ModItems.BETTA_PELLETS.get()));
            h.assertTrue(count(c, Items.DIRT) == 16 && count(f, Items.DIRT) == 0, "the dirt stays in the chest");
        });
    }

    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_tunnels", timeoutTicks = 900)
    public static void integratedTunnelsExporterFillsATank(GameTestHelper h) {
        IntegrationSuite.require("integrateddynamics", "integratedtunnels");
        PlumbingGameTestsAccess.reservoir(h, new BlockPos(1, 2, 2), 16000);
        TankBlockEntity t = tank(h, 4, 2, 2, 2, 2, 2, false);
        String aspect = rig(h, "INTERFACE_FLUID", "EXPORTER_FLUID");
        h.succeedWhen(() -> h.assertTrue(t.getTank().waterMb() > 0, "the fluid exporter (" + aspect + ") fills the tank: " + t.getTank().waterMb()));
    }

    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_tunnels_power", timeoutTicks = 400)
    public static void integratedTunnelsExporterPowersATank(GameTestHelper h) {
        IntegrationSuite.require("integrateddynamics", "integratedtunnels", "powah");
        Runnable restore = powerOn();
        TankBlockEntity t;
        String aspect;
        try {
            h.setBlock(new BlockPos(1, 2, 2), block("powah:energy_cell_creative"));
            t = tank(h, 4, 2, 2, 1, 1, 1, true);
            aspect = rig(h, "INTERFACE_ENERGY", "EXPORTER_ENERGY");
        } catch (RuntimeException e) {
            restore.run();
            throw e;
        }
        h.runAfterDelay(200, () -> {
            long got = t.getTank().powerBuffer();
            restore.run();
            h.assertTrue(got > 0, "the energy exporter (" + aspect + ") delivers FE to the tank, buffer " + got);
            h.succeed();
        });
    }
}
