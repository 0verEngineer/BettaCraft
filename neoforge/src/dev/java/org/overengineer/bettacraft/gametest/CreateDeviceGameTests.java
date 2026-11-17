package org.overengineer.bettacraft.gametest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.compat.create.CreateKinetics;
import org.overengineer.bettacraft.food.FoodMakerBlockEntity;
import org.overengineer.bettacraft.osmosis.RoUnitBlockEntity;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/**
 * M32 part c, dev group {@code -Pcreate}: Create's rotation drives our devices (a creative motor at the food maker winds
 * its spring, at the reverse osmosis unit it boosts like FE, the speed maps to the work), a Display Link writes the
 * values of a tank onto a sign, Create's pump and pipes fill a tank through its fluid capability. Only Create's ids and
 * reflection; without Create the tests pass at once.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CreateDeviceGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private CreateDeviceGameTests() {}

    private static boolean create() {
        return org.overengineer.bettacraft.platform.Platform.get().isModLoaded("create");
    }

    static Block block(String id) {
        Block b = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
        if (b == Blocks.AIR) throw new GameTestAssertException("no block " + id);
        return b;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static BlockState with(BlockState s, String property, Comparable value) {
        for (Property<?> p : s.getProperties()) if (p.getName().equals(property)) return s.setValue((Property) p, value);
        throw new GameTestAssertException("no property " + property + " on " + s);
    }

    private static Field field(Class<?> c, String name) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try {
                Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new GameTestAssertException("no field " + c.getName() + "." + name);
    }

    static Method method(Class<?> c, String name, int params) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            for (Method m : k.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == params) {
                    m.setAccessible(true);
                    return m;
                }
            }
        }
        throw new GameTestAssertException("no method " + c.getName() + "." + name);
    }

    /** A creative motor at {@code rel} whose shaft points {@code facing}, turning at {@code rpm}. */
    static void motor(GameTestHelper h, BlockPos rel, Direction facing, int rpm) {
        h.setBlock(rel, with(block("create:creative_motor").defaultBlockState(), "facing", facing));
        BlockEntity be = h.getBlockEntity(rel);
        if (be == null) throw new GameTestAssertException("no motor block entity");
        try {
            Object speed = field(be.getClass(), "generatedSpeed").get(be);
            method(speed.getClass(), "setValue", 1).invoke(speed, rpm);
        } catch (ReflectiveOperationException e) {
            throw new GameTestAssertException("motor speed: " + e);
        }
    }

    /** Speed to work: 64 RPM and more is full work, slower proportionally, nothing standing still. */
    @GameTest(template = EMPTY, batch = "bettacraft_create", timeoutTicks = 100)
    public static void rotationDrivesTheFoodMakerAndTheOsmosisUnit(GameTestHelper h) {
        if (!create()) {
            h.succeed();
            return;
        }
        ServerLevel level = h.getLevel();
        h.assertTrue(CreateKinetics.work(0F) == 0F && CreateKinetics.work(32F) == 0.5F && CreateKinetics.work(256F) == 1F, "the speed mapping");
        FoodMakerBlockEntity m = FoodMakerGameTests.maker(h, new BlockPos(2, 2, 2));
        m.items().setStackInSlot(0, new ItemStack(Items.COD));
        m.items().setStackInSlot(1, new ItemStack(Items.WHEAT));
        m.items().setStackInSlot(2, new ItemStack(Items.DRIED_KELP, 2));
        m.setSpring(0);
        motor(h, new BlockPos(3, 2, 2), Direction.WEST, 64);
        // M37: the fertilizer maker with a motor beside it
        var fert = FertilizerGameTests.maker(h, new BlockPos(2, 2, 8));
        fert.items().setStackInSlot(org.overengineer.bettacraft.fertilizer.FertilizerMakerBlockEntity.MEAL, new ItemStack(ModItems.KELP_MEAL.get(), 2));
        fert.items().setStackInSlot(org.overengineer.bettacraft.fertilizer.FertilizerMakerBlockEntity.SALT, new ItemStack(ModItems.NUTRIENT_SALT.get(), 2));
        fert.setWater(1000);
        fert.windSpring().set(0);
        motor(h, new BlockPos(3, 2, 8), Direction.WEST, 64);
        // the reverse osmosis unit next to water, a motor at half speed
        BlockPos water = new BlockPos(10, 2, 2), ro = new BlockPos(11, 2, 2);
        h.setBlock(water, Blocks.WATER.defaultBlockState());
        h.setBlock(ro, ModBlocks.RO_UNIT.get().defaultBlockState());
        RoUnitBlockEntity unit = (RoUnitBlockEntity) level.getBlockEntity(h.absolutePos(ro));
        if (unit == null) throw new GameTestAssertException("no RO unit");
        unit.parts().setStackInSlot(RoUnitBlockEntity.MEMBRANE, new ItemStack(ModItems.RO_MEMBRANE.get()));
        unit.parts().setStackInSlot(RoUnitBlockEntity.PREFILTER, new ItemStack(ModItems.RO_PREFILTER.get()));
        int slow = unit.step(level, 20);
        h.assertTrue(!unit.boosted(), "no boost without rotation: " + slow);
        motor(h, new BlockPos(12, 2, 2), Direction.WEST, 128);
        h.succeedWhen(() -> {
            h.assertTrue(CreateKinetics.speed(level, m.getBlockPos()) == 64F, "the food maker sees 64 RPM: " + CreateKinetics.speed(level, m.getBlockPos()));
            h.assertTrue(m.spring() > 0 || !m.items().getStackInSlot(FoodMakerBlockEntity.OUTPUT).isEmpty(), "the shaft winds the spring: " + m.spring());
            h.assertTrue(m.status() == FoodMakerBlockEntity.WORKING || !m.items().getStackInSlot(FoodMakerBlockEntity.OUTPUT).isEmpty(),
                    "the food maker works: " + m.status());
            int fast = unit.step(level, 20);
            h.assertTrue(unit.boosted() && fast > slow, "the shaft boosts the osmosis unit: " + slow + " -> " + fast);
            h.assertTrue(CreateKinetics.speed(level, fert.getBlockPos()) == 64F, "the fertilizer maker sees 64 RPM");
            h.assertTrue(fert.status() == org.overengineer.bettacraft.fertilizer.FertilizerMakerBlockEntity.WORKING || fert.fertilizer() > 0,
                    "the shaft drives the fertilizer maker: " + fert.status());
        });
    }

    /** A Display Link on a tank writes its values onto a sign (temperature first). */
    @GameTest(template = EMPTY, batch = "bettacraft_create", timeoutTicks = 100)
    public static void displayLinkShowsTankValues(GameTestHelper h) throws Throwable {
        if (!create()) {
            h.succeed();
            return;
        }
        ServerLevel level = h.getLevel();
        TankBlockEntity c = CritterGameTests.tank(h, 4, 2, 4, 1, 1, 1, 1000L);
        c.getTank().setWaterQuality(1F);
        h.setBlock(new BlockPos(5, 2, 4), with(block("create:display_link").defaultBlockState(), "facing", Direction.EAST));
        h.setBlock(new BlockPos(8, 2, 4), Blocks.OAK_SIGN.defaultBlockState());
        BlockEntity link = h.getBlockEntity(new BlockPos(5, 2, 4));
        // method handles, not getDeclaredMethods: the display link has methods with ComputerCraft types (not loaded here)
        java.lang.invoke.MethodHandles.Lookup lookup = java.lang.invoke.MethodHandles.publicLookup();
        lookup.findVirtual(link.getClass(), "target", java.lang.invoke.MethodType.methodType(void.class, BlockPos.class))
                .invoke(link, h.absolutePos(new BlockPos(8, 2, 4)));
        Object holder = Class.forName("org.overengineer.bettacraft.compat.create.CreateDisplays").getField("TANK_VALUES").get(null);
        Object source = holder.getClass().getMethod("get").invoke(holder);
        Object all = Class.forName("com.simibubi.create.api.behaviour.display.DisplaySource")
                .getMethod("getAll", net.minecraft.world.level.LevelAccessor.class, BlockPos.class).invoke(null, level, c.getBlockPos());
        h.assertTrue(((java.util.List<?>) all).contains(source), "the tank offers our source: " + all);
        link.getClass().getField("activeSource").set(link, source);
        lookup.findVirtual(link.getClass(), "updateGatheredData", java.lang.invoke.MethodType.methodType(void.class)).invoke(link);
        h.succeedWhen(() -> {
            if (!(level.getBlockEntity(h.absolutePos(new BlockPos(8, 2, 4))) instanceof SignBlockEntity sign)) throw new GameTestAssertException("no sign");
            String first = sign.getFrontText().getMessage(0, false).getString();
            String second = sign.getFrontText().getMessage(1, false).getString();
            h.assertTrue(first.contains("°C"), "the temperature on the sign: '" + first + "'");
            h.assertTrue(second.contains("100"), "the water quality on the sign: '" + second + "'");
        });
    }

    /** Create's mechanical pump moves water from a creative fluid tank through its pipes into our tank. */
    @GameTest(template = EMPTY, batch = "bettacraft_create", timeoutTicks = 400)
    public static void createPipesFillATank(GameTestHelper h) throws Exception {
        if (!create()) {
            h.succeed();
            return;
        }
        TankBlockEntity c = CritterGameTests.tank(h, 6, 2, 26, 1, 1, 1, 0L);
        h.setBlock(new BlockPos(2, 2, 26), block("create:creative_fluid_tank").defaultBlockState());
        h.setBlock(new BlockPos(3, 2, 26), block("create:fluid_pipe").defaultBlockState());
        h.setBlock(new BlockPos(4, 2, 26), with(block("create:mechanical_pump").defaultBlockState(), "facing", Direction.EAST));
        h.setBlock(new BlockPos(5, 2, 26), block("create:fluid_pipe").defaultBlockState());
        h.setBlock(new BlockPos(4, 2, 25), with(block("create:cogwheel").defaultBlockState(), "axis", Direction.Axis.X));
        motor(h, new BlockPos(3, 2, 25), Direction.EAST, 64);
        BlockEntity tankBe = h.getBlockEntity(new BlockPos(2, 2, 26));
        Object inv = method(tankBe.getClass(), "getTankInventory", 0).invoke(tankBe);
        method(inv.getClass(), "setContainedFluid", 1).invoke(inv, new net.neoforged.neoforge.fluids.FluidStack(Fluids.WATER, 1000));
        // the pipes connect to their neighbours when placed by a player; here their shape is updated
        for (int x : new int[]{3, 5}) {
            BlockPos p = h.absolutePos(new BlockPos(x, 2, 26));
            h.getLevel().setBlock(p, Block.updateFromNeighbourShapes(h.getLevel().getBlockState(p), h.getLevel(), p), Block.UPDATE_ALL);
        }
        h.succeedWhen(() -> h.assertTrue(c.getTank().waterMb() >= 500, "the pump filled the tank: " + c.getTank().waterMb() + " mB"));
    }
}
