package org.overengineer.bettacraft.gametest.integration;

import static org.overengineer.bettacraft.gametest.integration.IntegrationRig.*;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.crucible.CrucibleBlockEntity;
import org.overengineer.bettacraft.food.FoodMakerBlockEntity;
import org.overengineer.bettacraft.gametest.PlumbingGameTestsAccess;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;

/**
 * M42 integration suite, autocrafting: an AE2 pattern provider pushes a processing pattern into the crucible (the food
 * maker has its own test in the normal suite, StorageGameTests), and the Refined Storage autocrafter, as the external
 * pattern sink of a crafting task, pushes the ingredients of a processing pattern into the food maker.
 */
@PrefixGameTestTemplate(false)
public final class IntegrationStorageTests {
    private static final String EMPTY = IntegrationSuite.EMPTY;

    private IntegrationStorageTests() {}

    /** Encodes a processing pattern in the provider at {@code rel} and pushes it each tick until the grid takes it. */
    private static boolean[] ae2Push(GameTestHelper h, BlockPos rel, List<Object[]> inputs, Object[] output, Runnable onPushed) throws Exception {
        ServerLevel level = h.getLevel();
        Object provider = be(h, rel);
        Object logic = call(provider, "getLogic");
        Class<?> aeKey = Class.forName("appeng.api.stacks.AEKey");
        Class<?> generic = Class.forName("appeng.api.stacks.GenericStack");
        Method keyOf = Class.forName("appeng.api.stacks.AEItemKey").getMethod("of", ItemLike.class);
        Constructor<?> stack = generic.getConstructor(aeKey, long.class);
        List<Object> in = new java.util.ArrayList<>();
        for (Object[] i : inputs) in.add(stack.newInstance(keyOf.invoke(null, i[0]), ((Integer) i[1]).longValue()));
        List<Object> out = List.of(stack.newInstance(keyOf.invoke(null, output[0]), ((Integer) output[1]).longValue()));
        Class<?> helper = Class.forName("appeng.api.crafting.PatternDetailsHelper");
        ItemStack pattern = (ItemStack) helper.getMethod("encodeProcessingPattern", List.class, List.class).invoke(null, in, out);
        call(call(logic, "getPatternInv"), "setItemDirect", 0, pattern);
        call(logic, "updatePatterns");
        Object details = helper.getMethod("decodePattern", ItemStack.class, net.minecraft.world.level.Level.class).invoke(null, pattern, level);
        if (details == null) throw new GameTestAssertException("the pattern does not decode");
        Object[] ins = (Object[]) call(details, "getInputs");
        Class<?> counter = Class.forName("appeng.api.stacks.KeyCounter");
        Object holders = Array.newInstance(counter, ins.length);
        for (int i = 0; i < ins.length; i++) {
            Object[] possible = (Object[]) call(ins[i], "getPossibleInputs");
            long mult = (long) call(ins[i], "getMultiplier");
            Object kc = counter.getConstructor().newInstance();
            counter.getMethod("add", aeKey, long.class).invoke(kc, generic.getMethod("what").invoke(possible[0]), (long) generic.getMethod("amount").invoke(possible[0]) * mult);
            Array.set(holders, i, kc);
        }
        boolean[] pushed = {false};
        h.onEachTick(() -> {
            if (pushed[0]) return;
            pushed[0] = (boolean) call(logic, "pushPattern", details, holders);
            if (pushed[0]) onPushed.run();
        });
        return pushed;
    }

    /** AE2: a pattern provider pushes 4 iron, coal and quartz into the crucible over a magma block; out come 3 stainless steel. */
    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_ae2_crafting", timeoutTicks = 900)
    public static void ae2PatternProviderFeedsTheCrucible(GameTestHelper h) throws Exception {
        if (IntegrationSuite.lacks(h, "ae2")) return;
        h.setBlock(new BlockPos(2, 1, 2), Blocks.MAGMA_BLOCK);
        h.setBlock(new BlockPos(2, 2, 2), ModBlocks.CRUCIBLE.get().defaultBlockState());
        CrucibleBlockEntity c = (CrucibleBlockEntity) be(h, new BlockPos(2, 2, 2));
        h.setBlock(new BlockPos(3, 2, 2), block("ae2:pattern_provider"));
        h.setBlock(new BlockPos(4, 2, 2), block("ae2:creative_energy_cell"));
        boolean[] pushed = ae2Push(h, new BlockPos(3, 2, 2), List.of(new Object[]{Items.IRON_INGOT, 4}, new Object[]{Items.COAL, 1}, new Object[]{Items.QUARTZ, 1}),
                new Object[]{ModItems.STAINLESS_STEEL_INGOT.get(), 3}, () -> {});
        h.succeedWhen(() -> {
            h.assertTrue(pushed[0], "the pattern provider pushed the pattern");
            ItemStack out = c.items().getStackInSlot(CrucibleBlockEntity.OUTPUT);
            h.assertTrue(out.is(ModItems.STAINLESS_STEEL_INGOT.get()) && out.getCount() == 3, "the crucible melted 3 stainless steel: " + out
                    + ", slots " + c.items().getStackInSlot(0) + " " + c.items().getStackInSlot(1) + " " + c.items().getStackInSlot(2) + " " + c.items().getStackInSlot(3));
        });
    }

    /** Refined Storage: the autocrafter facing the food maker accepts the ingredients of a crafting step and pushes them in. */
    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_rs_crafting", timeoutTicks = 900)
    public static void refinedStorageAutocrafterFeedsTheFoodMaker(GameTestHelper h) throws Exception {
        if (IntegrationSuite.lacks(h, "refinedstorage")) return;
        if (FoodMakerBlockEntity.powered()) throw new GameTestAssertException("the spring of the food maker needs the power options off");
        FoodMakerBlockEntity m = PlumbingGameTestsAccess.foodMaker(h, new BlockPos(2, 2, 2));
        h.setBlock(new BlockPos(3, 2, 2), with(block("refinedstorage:autocrafter").defaultBlockState(), "direction", "west"));
        h.setBlock(new BlockPos(4, 2, 2), block("refinedstorage:creative_controller").defaultBlockState());
        Object crafter = be(h, new BlockPos(3, 2, 2));
        Constructor<?> res = Class.forName("com.refinedmods.refinedstorage.common.support.resource.ItemResource").getConstructor(Item.class);
        Constructor<?> amount = Class.forName("com.refinedmods.refinedstorage.api.resource.ResourceAmount")
                .getConstructor(Class.forName("com.refinedmods.refinedstorage.api.resource.ResourceKey"), long.class);
        List<Object> ingredients = List.of(amount.newInstance(res.newInstance(Items.COD), 1L), amount.newInstance(res.newInstance(Items.WHEAT), 1L),
                amount.newInstance(res.newInstance(Items.DRIED_KELP), 2L));
        Object execute = enumValue("com.refinedmods.refinedstorage.api.core.Action", "EXECUTE");
        Object accepted = enumValue("com.refinedmods.refinedstorage.api.autocrafting.task.ExternalPatternSink$Result", "ACCEPTED");
        Object[] result = {null};
        h.onEachTick(() -> {
            if (accepted.equals(result[0])) return;
            result[0] = call(crafter, "accept", ingredients, execute); // a step of a task; rejected until the node is active
            if (accepted.equals(result[0])) m.setSpring(FoodMakerBlockEntity.springMax());
        });
        h.succeedWhen(() -> {
            h.assertTrue(accepted.equals(result[0]), "the autocrafter accepted the step: " + result[0]);
            ItemStack out = m.items().getStackInSlot(FoodMakerBlockEntity.OUTPUT);
            h.assertTrue(out.is(ModItems.BETTA_PELLETS.get()) && out.getCount() == 12, "the food maker made 12 pellets: " + out + " status " + m.status());
        });
    }

    /**
     * AE2 and the filter media (M42 fix): an export bus with activated carbon in its filter puts a fresh one into the empty
     * filter chamber, an import bus takes it out only once it is used up (into the chest behind the storage bus), then the
     * export bus puts the next fresh one in.
     */
    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_ae2_media", timeoutTicks = 900)
    public static void ae2BusesChangeTheFilterMedia(GameTestHelper h) throws Exception {
        if (IntegrationSuite.lacks(h, "ae2")) return;
        ServerLevel level = h.getLevel();
        h.setBlock(new BlockPos(2, 2, 2), ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
        var chamber = (org.overengineer.bettacraft.filter.FilterChamberBlockEntity) be(h, new BlockPos(2, 2, 2));
        var chest = chest(h, new BlockPos(5, 2, 2), new ItemStack(ModItems.ACTIVATED_CARBON.get()), new ItemStack(ModItems.ACTIVATED_CARBON.get()));
        Method setPart = null;
        for (Method m : Class.forName("appeng.api.parts.PartHelper").getMethods()) if (m.getName().equals("setPart") && m.getParameterCount() == 5) setPart = m;
        if (setPart == null) throw new GameTestAssertException("AE2 API changed: no PartHelper.setPart");
        for (BlockPos p : new BlockPos[]{new BlockPos(3, 2, 2), new BlockPos(4, 2, 2), new BlockPos(3, 2, 3), new BlockPos(2, 2, 3)}) {
            setPart.invoke(null, level, h.absolutePos(p), null, null, item("ae2:fluix_glass_cable"));
        }
        Object export = setPart.invoke(null, level, h.absolutePos(new BlockPos(3, 2, 2)), net.minecraft.core.Direction.WEST, null, item("ae2:export_bus"));
        setPart.invoke(null, level, h.absolutePos(new BlockPos(2, 2, 3)), net.minecraft.core.Direction.NORTH, null, item("ae2:import_bus"));
        setPart.invoke(null, level, h.absolutePos(new BlockPos(4, 2, 2)), net.minecraft.core.Direction.EAST, null, item("ae2:storage_bus"));
        Object config = call(export, "getConfig");
        Class<?> aeKey = Class.forName("appeng.api.stacks.AEKey");
        Object key = Class.forName("appeng.api.stacks.AEItemKey").getMethod("of", ItemLike.class).invoke(null, ModItems.ACTIVATED_CARBON.get());
        call(config, "setStack", 0, Class.forName("appeng.api.stacks.GenericStack").getConstructor(aeKey, long.class).newInstance(key, 1L));
        h.setBlock(new BlockPos(3, 3, 2), block("ae2:creative_energy_cell"));
        boolean[] wornOut = {false};
        h.onEachTick(() -> {
            // once the first one is in, wear it out (as if the filter had run for days)
            if (!wornOut[0] && !chamber.medium().isEmpty()) {
                h.assertTrue(count(chest, ModItems.ACTIVATED_CARBON.get()) == 1, "the export bus took one carbon: " + count(chest, ModItems.ACTIVATED_CARBON.get()));
                ItemStack worn = chamber.medium().copy();
                worn.setDamageValue(org.overengineer.bettacraft.filter.FilterMediaItem.MAX_DIRT);
                chamber.setMedium(worn);
                wornOut[0] = true;
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(wornOut[0], "a medium went in");
            h.assertTrue(!chamber.medium().isEmpty() && chamber.medium().getDamageValue() == 0, "a fresh carbon is in: " + chamber.medium());
            int spent = 0, fresh = 0;
            for (int i = 0; i < chest.getContainerSize(); i++) {
                ItemStack st = chest.getItem(i);
                if (!st.is(ModItems.ACTIVATED_CARBON.get())) continue;
                if (st.getDamageValue() > 0) spent += st.getCount();
                else fresh += st.getCount();
            }
            h.assertTrue(spent == 1 && fresh == 0, "the used-up carbon came back into the chest: spent " + spent + ", fresh " + fresh);
        });
    }
}
