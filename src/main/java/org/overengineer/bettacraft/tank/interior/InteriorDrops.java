package org.overengineer.bettacraft.tank.interior;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.overengineer.bettacraft.equipment.SegmentBlock;

/**
 * Turns interior blocks into the items a player placed (PLAN 4.1 / I-3b): one item per block (picked like pick-block,
 * so kelp plants give kelp), two-block plants, doors and beds only once, double slabs, candles and sea pickles by
 * count, heater/filter columns once per piece, container contents, and paid substrate. Many stacks are packed into
 * chest items that keep their contents (container component), 27 stacks each.
 */
public final class InteriorDrops {
    /** Above this many stacks the drops are packed into chests. */
    static final int PACK_ABOVE = 27;

    private InteriorDrops() {}

    /** Collects the item(s) for one interior block (does not remove it). */
    public static void collect(ServerLevel level, BlockPos pos, BlockState s, List<ItemStack> out) {
        if (s.isAir() || InteriorManager.isShellBlock(s)) return;
        // M13: aquarium plants come out as whole plants from the plant index (see InteriorManager#dropInteriorOfBlock)
        if (s.getBlock() instanceof org.overengineer.bettacraft.plants.AquaticPlantBlock) return;
        if (s.getBlock() instanceof SegmentBlock seg) {
            if (s.getValue(SegmentBlock.PART) == SegmentBlock.Part.ANCHOR) out.add(seg.itemFor(s));
            return;
        }
        if (s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && s.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER
                && (s.getBlock() instanceof DoublePlantBlock || s.getBlock() instanceof DoorBlock)) return;
        if (s.getBlock() instanceof BedBlock && s.getValue(BedBlock.PART) == BedPart.HEAD) return;
        ItemStack item;
        try {
            item = s.getBlock().getCloneItemStack(level, pos, s);
        } catch (RuntimeException e) {
            item = ItemStack.EMPTY;
        }
        if (item.isEmpty()) {
            Item i = s.getBlock().asItem();
            item = i == Items.AIR ? ItemStack.EMPTY : new ItemStack(i);
        }
        if (!item.isEmpty()) {
            item = new ItemStack(item.getItem());
            int n = 1;
            if (s.getBlock() instanceof SlabBlock && s.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) n = 2;
            else if (s.getBlock() instanceof CandleBlock) n = s.getValue(CandleBlock.CANDLES);
            else if (s.getBlock() instanceof SeaPickleBlock) n = s.getValue(SeaPickleBlock.PICKLES);
            item.setCount(n);
            out.add(item);
        }
        if (s.hasBlockEntity()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof Container c) {
                for (int i = 0; i < c.getContainerSize(); i++) {
                    ItemStack st = c.getItem(i);
                    if (!st.isEmpty()) out.add(st.copy());
                }
                c.clearContent();
            }
        }
    }

    /** Merges equal items into full stacks. */
    public static List<ItemStack> merge(List<ItemStack> items) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack st : items) {
            ItemStack left = st.copy();
            for (ItemStack o : out) {
                if (left.isEmpty()) break;
                if (ItemStack.isSameItemSameComponents(o, left) && o.getCount() < o.getMaxStackSize()) {
                    int move = Math.min(left.getCount(), o.getMaxStackSize() - o.getCount());
                    o.grow(move);
                    left.shrink(move);
                }
            }
            while (!left.isEmpty()) {
                out.add(left.split(Math.min(left.getCount(), left.getMaxStackSize())));
            }
        }
        return out;
    }

    /** Drops the items at {@code pos}: as they are, or packed into named chests when there are many stacks. */
    public static void drop(ServerLevel level, BlockPos pos, List<ItemStack> items) {
        List<ItemStack> stacks = merge(items);
        if (stacks.size() <= PACK_ABOVE) {
            for (ItemStack s : stacks) Block.popResource(level, pos, s);
            return;
        }
        int chests = (stacks.size() + 26) / 27;
        for (int c = 0; c < chests; c++) {
            List<ItemStack> part = stacks.subList(c * 27, Math.min(stacks.size(), (c + 1) * 27));
            Block.popResource(level, pos, packedChest(part, c + 1, chests));
        }
    }

    public static ItemStack packedChest(List<ItemStack> content, int index, int total) {
        ItemStack chest = new ItemStack(Items.CHEST);
        chest.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(content));
        chest.set(DataComponents.CUSTOM_NAME, Component.translatable("item.bettacraft.interior_chest", index, total));
        return chest;
    }
}
