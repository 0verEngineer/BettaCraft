package org.overengineer.bettacraft.gametest;

import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.food.FoodMakerBlockEntity;
import org.overengineer.bettacraft.registry.ModItems;

/**
 * M26 part b, dev group {@code -Pstorage} (AE2 + Refined Storage in run/storage-mods): the storage mods move items in and
 * out of the food maker through its plain item capability, no special code. AE2: an export bus puts eggs from a chest
 * (behind a storage bus) into the machine, an import bus takes the fry powder out into the chest. RS: an importer takes
 * the food out into a creative storage block. M32 part c: an AE2 pattern provider pushes a processing pattern into the
 * machine (autocrafting). Without the mods the tests pass at once. Only AE2 / RS ids and reflection.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StorageGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private StorageGameTests() {}

    private static Item item(String id) {
        Item i = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        if (i == Items.AIR) throw new GameTestAssertException("no item " + id);
        return i;
    }

    private static Block block(String id) {
        Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        if (b == Blocks.AIR) throw new GameTestAssertException("no block " + id);
        return b;
    }

    private static Method method(Class<?> c, String name, int params) {
        for (Method m : c.getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == params) {
                try {
                    m.setAccessible(true); // public methods of package-private implementation classes
                } catch (RuntimeException ignored) {
                }
                return m;
            }
        }
        throw new GameTestAssertException("no method " + c.getName() + "." + name);
    }

    /** AE2: a cable part (side null) or a bus on a side of the cable bus at {@code abs}. */
    private static Object aePart(ServerLevel level, BlockPos abs, Direction side, String item) throws Exception {
        Method set = method(Class.forName("appeng.api.parts.PartHelper"), "setPart", 5);
        Object part = set.invoke(null, level, abs, side, null, item(item));
        if (part == null) throw new GameTestAssertException("AE2 did not place " + item);
        return part;
    }

    private static FoodMakerBlockEntity maker(GameTestHelper h, BlockPos rel) {
        FoodMakerBlockEntity m = FoodMakerGameTests.maker(h, rel);
        m.setSpring(FoodMakerBlockEntity.springMax());
        return m;
    }

    private static int count(ChestBlockEntity chest, Item item) {
        int n = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).is(item)) n += chest.getItem(i).getCount();
        return n;
    }

    /** AE2: export bus (eggs from the chest into the machine), import bus (the fry powder out into the chest). */
    @GameTest(template = EMPTY, batch = "bettacraft_storage", timeoutTicks = 900)
    public static void ae2BusesFillAndEmptyTheFoodMaker(GameTestHelper h) throws Exception {
        if (!ModList.get().isLoaded("ae2")) {
            h.succeed();
            return;
        }
        if (FoodMakerBlockEntity.powered()) {
            h.succeed(); // the spring test needs the power options off
            return;
        }
        ServerLevel level = h.getLevel();
        FoodMakerBlockEntity m = maker(h, new BlockPos(2, 2, 2));
        m.items().setStackInSlot(1, new ItemStack(Items.WHEAT, 4));
        h.setBlock(new BlockPos(5, 2, 2), Blocks.CHEST.defaultBlockState());
        if (!(level.getBlockEntity(h.absolutePos(new BlockPos(5, 2, 2))) instanceof ChestBlockEntity chest)) throw new GameTestAssertException("no chest");
        chest.setItem(0, new ItemStack(Items.EGG, 2));
        // cable bus east of the machine: import bus west (machine), storage bus east (a chest one further), export bus on a
        // second cable bus south of the machine facing north; power from a creative energy cell on top
        BlockPos busA = h.absolutePos(new BlockPos(3, 2, 2)), busB = h.absolutePos(new BlockPos(3, 2, 3)), busC = h.absolutePos(new BlockPos(2, 2, 3));
        for (BlockPos p : new BlockPos[]{busA, busB, busC}) aePart(level, p, null, "ae2:fluix_glass_cable");
        aePart(level, h.absolutePos(new BlockPos(4, 2, 2)), null, "ae2:fluix_glass_cable");
        aePart(level, h.absolutePos(new BlockPos(4, 2, 2)), Direction.EAST, "ae2:storage_bus");
        aePart(level, busA, Direction.WEST, "ae2:import_bus");
        Object export = aePart(level, busC, Direction.NORTH, "ae2:export_bus");
        Object config = method(export.getClass(), "getConfig", 0).invoke(export);
        Object key = Class.forName("appeng.api.stacks.AEItemKey").getMethod("of", ItemLike.class).invoke(null, Items.EGG);
        Class<?> aeKey = Class.forName("appeng.api.stacks.AEKey");
        Object stack = Class.forName("appeng.api.stacks.GenericStack").getConstructor(aeKey, long.class).newInstance(key, 1L);
        method(config.getClass(), "setStack", 2).invoke(config, 0, stack);
        h.setBlock(new BlockPos(3, 3, 2), block("ae2:creative_energy_cell").defaultBlockState());
        h.succeedWhen(() -> {
            h.assertTrue(count(chest, Items.EGG) < 2, "the export bus took an egg: " + count(chest, Items.EGG));
            h.assertTrue(count(chest, ModItems.FRY_POWDER.get()) >= 8, "the import bus took the fry powder out: " + count(chest, ModItems.FRY_POWDER.get())
                    + ", machine " + m.items().getStackInSlot(FoodMakerBlockEntity.OUTPUT) + " status " + m.status());
        });
    }

    /** Refined Storage: an importer takes the food out of the machine into the network (creative controller and storage). */
    @GameTest(template = EMPTY, batch = "bettacraft_storage", timeoutTicks = 900)
    public static void refinedStorageImporterEmptiesTheFoodMaker(GameTestHelper h) {
        if (!ModList.get().isLoaded("refinedstorage")) {
            h.succeed();
            return;
        }
        if (FoodMakerBlockEntity.powered()) {
            h.succeed();
            return;
        }
        FoodMakerBlockEntity m = maker(h, new BlockPos(2, 2, 2));
        m.items().setStackInSlot(0, new ItemStack(Items.EGG));
        m.items().setStackInSlot(1, new ItemStack(Items.WHEAT));
        BlockState importer = block("refinedstorage:importer").defaultBlockState();
        for (Property<?> p : importer.getProperties()) {
            if (p.getName().equals("direction")) importer = with(importer, p, "west");
        }
        h.setBlock(new BlockPos(3, 2, 2), importer);
        h.setBlock(new BlockPos(4, 2, 2), block("refinedstorage:creative_controller").defaultBlockState());
        h.setBlock(new BlockPos(4, 3, 2), block("refinedstorage:creative_storage_block").defaultBlockState());
        h.succeedWhen(() -> {
            h.assertTrue(m.items().getStackInSlot(0).isEmpty(), "made the batch: " + m.status() + " " + m.progress());
            h.assertTrue(m.items().getStackInSlot(FoodMakerBlockEntity.OUTPUT).isEmpty(), "the importer took the food out: "
                    + m.items().getStackInSlot(FoodMakerBlockEntity.OUTPUT));
        });
    }

    private static <T extends Comparable<T>> BlockState with(BlockState s, Property<T> p, String value) {
        return p.getValue(value).map(v -> s.setValue(p, v)).orElse(s);
    }

    /**
     * M32 part c, AE2 autocrafting: a processing pattern (cod, wheat, two dried kelp -> 12 betta pellets) in a pattern
     * provider next to the food maker; the provider pushes the inputs through the machine's item capability into its input
     * slots (never the output), the machine makes the food, which an import bus would take back as in the test above.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_storage", timeoutTicks = 900)
    public static void ae2PatternProviderFeedsTheFoodMaker(GameTestHelper h) throws Exception {
        if (!ModList.get().isLoaded("ae2")) {
            h.succeed();
            return;
        }
        if (FoodMakerBlockEntity.powered()) {
            h.succeed(); // the spring needs the power options off
            return;
        }
        ServerLevel level = h.getLevel();
        FoodMakerBlockEntity m = maker(h, new BlockPos(2, 2, 2));
        h.setBlock(new BlockPos(3, 2, 2), block("ae2:pattern_provider").defaultBlockState());
        h.setBlock(new BlockPos(4, 2, 2), block("ae2:creative_energy_cell").defaultBlockState());
        Object provider = level.getBlockEntity(h.absolutePos(new BlockPos(3, 2, 2)));
        if (provider == null) throw new GameTestAssertException("no pattern provider");
        Object logic = method(provider.getClass(), "getLogic", 0).invoke(provider);
        Class<?> aeKey = Class.forName("appeng.api.stacks.AEKey");
        Class<?> itemKey = Class.forName("appeng.api.stacks.AEItemKey");
        Class<?> generic = Class.forName("appeng.api.stacks.GenericStack");
        Method keyOf = itemKey.getMethod("of", ItemLike.class);
        java.lang.reflect.Constructor<?> stack = generic.getConstructor(aeKey, long.class);
        java.util.List<Object> in = java.util.List.of(stack.newInstance(keyOf.invoke(null, Items.COD), 1L),
                stack.newInstance(keyOf.invoke(null, Items.WHEAT), 1L), stack.newInstance(keyOf.invoke(null, Items.DRIED_KELP), 2L));
        java.util.List<Object> out = java.util.List.of(stack.newInstance(keyOf.invoke(null, ModItems.BETTA_PELLETS.get()), 12L));
        Class<?> helper = Class.forName("appeng.api.crafting.PatternDetailsHelper");
        ItemStack pattern = (ItemStack) helper.getMethod("encodeProcessingPattern", java.util.List.class, java.util.List.class).invoke(null, in, out);
        Object patternInv = method(logic.getClass(), "getPatternInv", 0).invoke(logic);
        method(patternInv.getClass(), "setItemDirect", 2).invoke(patternInv, 0, pattern);
        method(logic.getClass(), "updatePatterns", 0).invoke(logic);
        Object details = helper.getMethod("decodePattern", ItemStack.class, net.minecraft.world.level.Level.class).invoke(null, pattern, level);
        if (details == null) throw new GameTestAssertException("the pattern does not decode");
        Object[] inputs = (Object[]) method(details.getClass(), "getInputs", 0).invoke(details);
        Class<?> counter = Class.forName("appeng.api.stacks.KeyCounter");
        Object holders = java.lang.reflect.Array.newInstance(counter, inputs.length);
        for (int i = 0; i < inputs.length; i++) {
            Object[] possible = (Object[]) method(inputs[i].getClass(), "getPossibleInputs", 0).invoke(inputs[i]);
            long mult = (long) method(inputs[i].getClass(), "getMultiplier", 0).invoke(inputs[i]);
            Object kc = counter.getConstructor().newInstance();
            Object what = generic.getMethod("what").invoke(possible[0]);
            long amount = (long) generic.getMethod("amount").invoke(possible[0]);
            counter.getMethod("add", aeKey, long.class).invoke(kc, what, amount * mult);
            java.lang.reflect.Array.set(holders, i, kc);
        }
        Method push = method(logic.getClass(), "pushPattern", 2);
        boolean[] pushed = {false};
        h.onEachTick(() -> {
            if (pushed[0]) return;
            try {
                pushed[0] = (boolean) push.invoke(logic, details, holders); // true once the grid is up
                if (pushed[0]) m.setSpring(FoodMakerBlockEntity.springMax());
            } catch (ReflectiveOperationException e) {
                throw new GameTestAssertException("pushPattern: " + e);
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(pushed[0], "the pattern provider pushed the pattern");
            h.assertTrue(m.items().getStackInSlot(FoodMakerBlockEntity.OUTPUT).is(ModItems.BETTA_PELLETS.get())
                    && m.items().getStackInSlot(FoodMakerBlockEntity.OUTPUT).getCount() == 12, "the machine made the pellets: "
                    + m.items().getStackInSlot(FoodMakerBlockEntity.OUTPUT) + " status " + m.status());
        });
    }
}
