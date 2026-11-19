package org.overengineer.bettacraft.gametest.integration;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.equipment.FeederBlockEntity;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;

/** M42: shared helpers of the integration tests (blocks and items by id, our targets, reflection on the mods' classes). */
final class IntegrationRig {
    private IntegrationRig() {}

    static Block block(String id) {
        Block b = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
        if (b == Blocks.AIR) throw new GameTestFailure("no block " + id);
        return b;
    }

    static Item item(String id) {
        Item i = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
        if (i == Items.AIR) throw new GameTestFailure("no item " + id);
        return i;
    }

    /** A block state with a property set by name and value name (left as it is when the block has no such property). */
    static BlockState with(BlockState s, String property, String value) {
        for (Property<?> p : s.getProperties()) if (p.getName().equals(property)) return set(s, p, value);
        return s;
    }

    private static <T extends Comparable<T>> BlockState set(BlockState s, Property<T> p, String value) {
        return p.getValue(value).map(v -> s.setValue(p, v)).orElseThrow(() -> new GameTestFailure("no value " + value + " of " + p.getName()));
    }

    static ChestBlockEntity chest(GameTestHelper h, BlockPos rel, ItemStack... stacks) {
        h.setBlock(rel, Blocks.CHEST.defaultBlockState());
        if (!(h.getLevel().getBlockEntity(h.absolutePos(rel)) instanceof ChestBlockEntity c)) throw new GameTestFailure("no chest");
        for (int i = 0; i < stacks.length; i++) c.setItem(i, stacks[i]);
        return c;
    }

    static int count(ChestBlockEntity c, Item item) {
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) if (c.getItem(i).is(item)) n += c.getItem(i).getCount();
        return n;
    }

    /** A feeder standing on its own (its item capability takes food only). */
    static FeederBlockEntity feeder(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel, ModBlocks.FEEDER.get().defaultBlockState());
        if (!(h.getLevel().getBlockEntity(h.absolutePos(rel)) instanceof FeederBlockEntity f)) throw new GameTestFailure("no feeder");
        return f;
    }

    static int count(FeederBlockEntity f, Item item) {
        int n = 0;
        for (int i = 0; i < f.items().getSlots(); i++) if (f.items().getStackInSlot(i).is(item)) n += f.items().getStackInSlot(i).getCount();
        return n;
    }

    /** A tank of tank blocks, filled with water when {@code water}. */
    static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz, boolean water) {
        for (int a = 0; a < sx; a++)
            for (int b = 0; b < sy; b++)
                for (int c = 0; c < sz; c++) {
                    BlockPos p = new BlockPos(x + a, y + b, z + c);
                    h.setBlock(p, ModBlocks.TANK.get().defaultBlockState());
                    TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(p), null);
                }
        TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(new BlockPos(x, y, z)));
        if (c == null) throw new GameTestFailure("tank did not form");
        if (water) TankInteractions.changeWater(h.getLevel(), c, c.getTank().capacityMb());
        return c;
    }

    /** Switches the filter power option on (the tank then takes FE) and gives back a restore action. */
    static Runnable powerOn() {
        boolean filter = ServerConfig.POWER_FILTER.get();
        ServerConfig.POWER_FILTER.set(true);
        return () -> ServerConfig.POWER_FILTER.set(filter);
    }

    // ------------------------------------------------------------------ reflection

    /** Whether the object has a method of that name and parameter count (its class or a superclass). */
    static boolean has(Object o, String name, int params) {
        for (Class<?> k = o.getClass(); k != null; k = k.getSuperclass()) {
            for (Method m : k.getDeclaredMethods()) if (m.getName().equals(name) && m.getParameterCount() == params) return true;
        }
        for (Method m : o.getClass().getMethods()) if (m.getName().equals(name) && m.getParameterCount() == params) return true;
        return false;
    }

    /**
     * Puts a stack into a slot of another mod's item handler: {@code setStackInSlot} (the IItemHandler of 1.21.x and handlers
     * that keep it), else the transfer API of NeoForge 26.1 ({@code ItemStacksResourceHandler#set(slot, ItemResource, amount)}).
     */
    static void setSlot(Object handler, int slot, net.minecraft.world.item.ItemStack stack) {
        if (has(handler, "setStackInSlot", 2)) {
            call(handler, "setStackInSlot", slot, stack);
            return;
        }
        Object resource;
        try {
            resource = Class.forName("net.neoforged.neoforge.transfer.item.ItemResource").getMethod("of", net.minecraft.world.item.ItemStack.class).invoke(null, stack);
        } catch (ReflectiveOperationException e) {
            throw new GameTestFailure("API changed: no ItemResource.of: " + e);
        }
        call(handler, "set", slot, resource, stack.getCount());
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
        for (Method m : c.getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == params) {
                m.setAccessible(true);
                return m;
            }
        }
        throw new GameTestFailure("API changed: no method " + c.getName() + "." + name + "/" + params);
    }

    /** The method of that name whose parameters take the arguments (overloads with the same count are told apart). */
    static Method method(Class<?> c, String name, Object[] args) {
        Method fallback = null;
        java.util.List<Method> all = new java.util.ArrayList<>();
        for (Class<?> k = c; k != null; k = k.getSuperclass()) all.addAll(java.util.Arrays.asList(k.getDeclaredMethods()));
        all.addAll(java.util.Arrays.asList(c.getMethods()));
        for (Method m : all) {
            if (!m.getName().equals(name) || m.getParameterCount() != args.length) continue;
            if (fallback == null) fallback = m;
            Class<?>[] p = m.getParameterTypes();
            boolean ok = true;
            for (int i = 0; i < p.length && ok; i++) ok = args[i] == null ? !p[i].isPrimitive() : box(p[i]).isInstance(args[i]);
            if (ok) {
                m.setAccessible(true);
                return m;
            }
        }
        if (fallback == null) throw new GameTestFailure("API changed: no method " + c.getName() + "." + name + "/" + args.length);
        fallback.setAccessible(true);
        return fallback;
    }

    private static Class<?> box(Class<?> c) {
        if (!c.isPrimitive()) return c;
        if (c == int.class) return Integer.class;
        if (c == long.class) return Long.class;
        if (c == boolean.class) return Boolean.class;
        if (c == byte.class) return Byte.class;
        if (c == short.class) return Short.class;
        if (c == float.class) return Float.class;
        if (c == double.class) return Double.class;
        if (c == char.class) return Character.class;
        return Void.class;
    }

    static Object call(Object o, String name, Object... args) {
        try {
            return method(o.getClass(), name, args).invoke(o, args);
        } catch (ReflectiveOperationException e) {
            Throwable t = e.getCause() != null ? e.getCause() : e;
            throw new GameTestFailure(o.getClass().getSimpleName() + "." + name + ": " + t);
        }
    }

    static Object callStatic(String cls, String name, Object... args) {
        try {
            return method(Class.forName(cls), name, args).invoke(null, args);
        } catch (ReflectiveOperationException e) {
            Throwable t = e.getCause() != null ? e.getCause() : e;
            throw new GameTestFailure(cls + "." + name + ": " + t);
        }
    }

    static Object field(Object o, String name) {
        for (Class<?> k = o.getClass(); k != null; k = k.getSuperclass()) {
            try {
                Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(o);
            } catch (NoSuchFieldException ignored) {
                // the super class
            } catch (IllegalAccessException e) {
                throw new GameTestFailure("field " + name + ": " + e);
            }
        }
        throw new GameTestFailure("API changed: no field " + o.getClass().getName() + "." + name);
    }

    static Object staticField(String cls, String name) {
        try {
            Field f = Class.forName(cls).getField(name);
            return f.get(null);
        } catch (ReflectiveOperationException e) {
            throw new GameTestFailure("API changed: " + cls + "." + name + ": " + e);
        }
    }

    static Object enumValue(String cls, String name) {
        return staticField(cls, name);
    }

    static Object be(GameTestHelper h, BlockPos rel) {
        Object be = h.getLevel().getBlockEntity(h.absolutePos(rel));
        if (be == null) throw new GameTestFailure("no block entity at " + rel + " (" + h.getBlockState(rel) + ")");
        return be;
    }

    static Direction towards(BlockPos from, BlockPos to) {
        return Direction.getApproximateNearest(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ());
    }
}
