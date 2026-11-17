package org.overengineer.bettacraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.filter.FilterChamberBlockEntity;
import org.overengineer.bettacraft.filter.FilterMediaItem;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;

/**
 * M42 (found by the integration suite: ME/RS buses and pipes could not change filter media): the item capability of the
 * multiblock filter's chambers. A hopper above puts a fresh medium into an empty chamber (one, the rest stays), a hopper
 * below takes nothing that still works and pulls a used-up medium out; then the next fresh one goes in. With the switch
 * {@code equipment.filterMediaAutomation} off the chamber offers no capability. Own batches (the switch is global).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FilterMediaAutomationGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private FilterMediaAutomationGameTests() {}

    private static FilterChamberBlockEntity chamber(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel, ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
        if (!(h.getLevel().getBlockEntity(h.absolutePos(rel)) instanceof FilterChamberBlockEntity c)) throw new GameTestAssertException("no chamber");
        return c;
    }

    private static HopperBlockEntity hopper(GameTestHelper h, BlockPos rel, Direction facing) {
        h.setBlock(rel, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, facing));
        if (!(h.getLevel().getBlockEntity(h.absolutePos(rel)) instanceof HopperBlockEntity hb)) throw new GameTestAssertException("no hopper");
        return hb;
    }

    private static int count(HopperBlockEntity hb) {
        int n = 0;
        for (int i = 0; i < hb.getContainerSize(); i++) n += hb.getItem(i).getCount();
        return n;
    }

    @GameTest(template = EMPTY, batch = "bettacraft_m42_media", timeoutTicks = 300)
    public static void hoppersChangeTheFilterMedia(GameTestHelper h) {
        if (!Features.enabled(Feature.FILTER_MEDIA_AUTOMATION)) throw new GameTestAssertException("the switch is off");
        FilterChamberBlockEntity c = chamber(h, new BlockPos(2, 2, 2));
        HopperBlockEntity above = hopper(h, new BlockPos(2, 3, 2), Direction.DOWN);
        above.setItem(0, new ItemStack(ModItems.FILTER_FOAM.get())); // media do not stack
        above.setItem(1, new ItemStack(ModItems.FILTER_FOAM.get()));
        above.setItem(2, new ItemStack(net.minecraft.world.item.Items.DIRT, 4)); // not a medium: stays
        HopperBlockEntity below = hopper(h, new BlockPos(2, 1, 2), Direction.EAST);
        h.setBlock(new BlockPos(3, 1, 2), Blocks.CHEST); // the hopper below passes on what it pulls
        // the capability: fresh medium in, nothing out while it works, the dirt refused
        var cap = org.overengineer.bettacraft.platform.Platform.get().itemHandler(h.getLevel(), h.absolutePos(new BlockPos(2, 2, 2)), Direction.UP);
        h.assertTrue(cap != null && cap.getSlots() == 1, "the chamber offers one slot");
        h.assertTrue(!cap.isItemValid(0, new ItemStack(net.minecraft.world.item.Items.DIRT)), "only media are valid");
        h.runAfterDelay(40, () -> {
            h.assertTrue(c.medium().is(ModItems.FILTER_FOAM.get()) && c.medium().getCount() == 1, "the hopper put one sponge in: " + c.medium());
            h.assertTrue(above.getItem(0).isEmpty() && above.getItem(1).getCount() == 1, "one sponge waits in the hopper: " + above.getItem(1));
            h.assertTrue(cap.extractItem(0, 1, true).isEmpty(), "a working medium does not come out");
            // used up: the hopper below pulls it out, the next fresh one goes in
            ItemStack worn = c.medium().copy();
            worn.setDamageValue(FilterMediaItem.MAX_DIRT);
            c.setMedium(worn);
            h.assertTrue(c.spent(), "fully clogged is spent");
            h.runAfterDelay(60, () -> {
                h.assertTrue(above.getItem(1).isEmpty(), "the second sponge went in: " + above.getItem(1));
                h.assertTrue(c.medium().is(ModItems.FILTER_FOAM.get()) && c.medium().getDamageValue() == 0, "a fresh sponge is in: " + c.medium());
                var chest = org.overengineer.bettacraft.platform.Platform.get().itemHandler(h.getLevel(), h.absolutePos(new BlockPos(3, 1, 2)), null);
                boolean wornOut = false;
                for (int i = 0; chest != null && i < chest.getSlots(); i++) {
                    ItemStack s = chest.getStackInSlot(i);
                    if (s.is(ModItems.FILTER_FOAM.get()) && s.getDamageValue() == FilterMediaItem.MAX_DIRT) wornOut = true;
                }
                h.assertTrue(wornOut || count(below) == 1, "the clogged sponge was pulled out below");
                h.assertTrue(above.getItem(2).getCount() == 4, "the dirt stays in the hopper");
                h.succeed();
            });
        });
    }

    @GameTest(template = EMPTY, batch = "bettacraft_m42_media_off", timeoutTicks = 100)
    public static void filterMediaAutomationOff(GameTestHelper h) {
        boolean saved = Features.own(Feature.FILTER_MEDIA_AUTOMATION);
        Features.set(Feature.FILTER_MEDIA_AUTOMATION, false);
        try {
            FilterChamberBlockEntity c = chamber(h, new BlockPos(2, 2, 2));
            GameTestCompat.invalidateCapabilities(h.getLevel(), h.absolutePos(new BlockPos(2, 2, 2)));
            h.assertTrue(org.overengineer.bettacraft.platform.Platform.get().itemHandler(h.getLevel(), h.absolutePos(new BlockPos(2, 2, 2)), Direction.UP) == null,
                    "no capability with the switch off");
            HopperBlockEntity above = hopper(h, new BlockPos(2, 3, 2), Direction.DOWN);
            above.setItem(0, new ItemStack(ModItems.FILTER_FOAM.get(), 1));
            h.runAfterDelay(40, () -> {
                Features.set(Feature.FILTER_MEDIA_AUTOMATION, saved);
                h.assertTrue(c.medium().isEmpty() && above.getItem(0).getCount() == 1, "the hopper puts nothing in: " + c.medium());
                h.succeed();
            });
        } catch (RuntimeException e) {
            Features.set(Feature.FILTER_MEDIA_AUTOMATION, saved);
            throw e;
        }
    }
}
