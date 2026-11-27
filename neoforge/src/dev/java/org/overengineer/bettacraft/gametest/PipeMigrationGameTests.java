package org.overengineer.bettacraft.gametest;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity;
import org.overengineer.bettacraft.filter.MultiblockFilter;
import org.overengineer.bettacraft.plumbing.PipeBlock;
import org.overengineer.bettacraft.plumbing.PipeBlockEntity;
import org.overengineer.bettacraft.plumbing.PipeBundleBlock;
import org.overengineer.bettacraft.plumbing.PipeNetwork;
import org.overengineer.bettacraft.plumbing.PipeRing;
import org.overengineer.bettacraft.plumbing.PipeRole;
import org.overengineer.bettacraft.plumbing.PipeSlots;
import org.overengineer.bettacraft.plumbing.PipeUpdates;
import org.overengineer.bettacraft.plumbing.Pipes;
import org.overengineer.bettacraft.plumbing.interior.InteriorPlumbing;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankWater;

/**
 * M68 GameTests: worlds and items from before M68 load into the one pipe system, nothing lost. Old blocks are put in the way a
 * saved chunk has them: the block state read by its old id (the registry alias turns it into a stainless steel pipe with the
 * same sides), set without block updates, and the block entity loaded from its old data and put into the chunk (so it migrates
 * and is joined again at the end of the tick, {@link PipeUpdates}). Covered: the M40 filter lines (two lines on top of each
 * other stay apart; the old filter keeps filtering), a bundle (its lines become slots with their connections, its cover stays),
 * a T-piece (all its sides), the filter's fresh water port (a plain steel pipe that joins its old line), every old item id in a
 * chest and as a stack (filter pipes, T-piece and ports a steel pipe, the interior pipe a glass pipe, 1:1).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PipeMigrationGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private PipeMigrationGameTests() {}

    private static BlockPos abs(GameTestHelper h, int x, int y, int z) {
        return h.absolutePos(new BlockPos(x, y, z));
    }

    /** The block state an old save has: {@code id} with these properties (an alias resolves the id). */
    static BlockState oldState(ServerLevel level, String id, Map<String, String> props) {
        CompoundTag t = new CompoundTag();
        t.putString("Name", BettaCraft.id(id).toString());
        CompoundTag p = new CompoundTag();
        props.forEach(p::putString);
        t.put("Properties", p);
        return NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), t);
    }

    /** The sides of an M40 filter pipe (and its T-piece) as block state properties. */
    static Map<String, String> sides(boolean junction, Direction... joined) {
        Map<String, String> m = new java.util.HashMap<>();
        for (Direction d : Direction.values()) m.put(d.getSerializedName(), "false");
        for (Direction d : joined) m.put(d.getSerializedName(), "true");
        m.put("junction", Boolean.toString(junction));
        return m;
    }

    /**
     * An old block at {@code rel}: the state as loaded, set without updates to its neighbours (a loaded chunk has no updates), and
     * its block entity loaded from {@code be} (with {@code id}) into the chunk like a chunk load does.
     */
    static BlockEntity old(GameTestHelper h, BlockPos rel, BlockState state, String beId, CompoundTag be) {
        ServerLevel level = h.getLevel();
        BlockPos pos = h.absolutePos(rel);
        level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        if (beId == null) return null;
        CompoundTag t = be.copy();
        t.putString("id", BettaCraft.id(beId).toString());
        t.putInt("x", pos.getX());
        t.putInt("y", pos.getY());
        t.putInt("z", pos.getZ());
        BlockEntity e = BlockEntity.loadStatic(pos, level.getBlockState(pos), t, level.registryAccess());
        if (e == null) throw new GameTestFailure("the old block entity " + beId + " does not load at " + rel);
        level.getChunkAt(pos).setBlockEntity(e);
        return e;
    }

    static BlockEntity oldPipe(GameTestHelper h, int x, int y, int z, String id, boolean junction, Direction... joined) {
        return old(h, new BlockPos(x, y, z), oldState(h.getLevel(), id, sides(junction, joined)), "filter_pipe", new CompoundTag());
    }

    // ------------------------------------------------------------------ the ids

    /** The registry aliases: every old block, item and block entity id resolves to its successor; stacks and chests convert 1:1. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void pipeMigrationIdsAndItems(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (String id : List.of("filter_inlet_pipe", "filter_outlet_pipe", "filter_fresh_pipe", "filter_waste_pipe", "filter_fresh_port", "filter_waste_port"))
            h.assertTrue(BuiltInRegistries.BLOCK.getValue(BettaCraft.id(id)) == ModBlocks.STAINLESS_STEEL_PIPE.get(), "block " + id + " is a stainless steel pipe: " + BuiltInRegistries.BLOCK.getValue(BettaCraft.id(id)));
        h.assertTrue(BuiltInRegistries.BLOCK.getValue(BettaCraft.id("filter_pipe_bundle")) == ModBlocks.FILTER_PIPE_BUNDLE.get(), "the bundle keeps its id");
        h.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(BettaCraft.id("filter_port")) == ModBlockEntities.PIPE.get(), "the ports' block entity is the pipe's");
        Map<String, Item> items = Map.of("filter_inlet_pipe", ModItems.STAINLESS_STEEL_PIPE.get(), "filter_outlet_pipe", ModItems.STAINLESS_STEEL_PIPE.get(),
                "filter_fresh_pipe", ModItems.STAINLESS_STEEL_PIPE.get(), "filter_waste_pipe", ModItems.STAINLESS_STEEL_PIPE.get(),
                "pipe_tee", ModItems.STAINLESS_STEEL_PIPE.get(), "filter_fresh_port", ModItems.STAINLESS_STEEL_PIPE.get(),
                "filter_waste_port", ModItems.STAINLESS_STEEL_PIPE.get(), "interior_pipe", ModItems.GLASS_PIPE.get());
        ListTag list = new ListTag();
        int slot = 0;
        for (var e : items.entrySet()) {
            CompoundTag st = new CompoundTag();
            st.putString("id", BettaCraft.id(e.getKey()).toString());
            st.putInt("count", 5);
            ItemStack s = org.overengineer.bettacraft.util.NbtCompat.parseOptionalStack(level.registryAccess(), st);
            h.assertTrue(s.is(e.getValue()) && s.getCount() == 5, e.getKey() + " loads as " + s);
            st.putByte("Slot", (byte) slot++);
            list.add(st);
        }
        // a chest of an old world with all of them
        h.setBlock(new BlockPos(2, 2, 2), Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(abs(h, 2, 2, 2));
        CompoundTag ct = new CompoundTag();
        ct.put("Items", list);
        chest.loadWithComponents(org.overengineer.bettacraft.util.NbtCompat.input(level.registryAccess(), ct));
        int steel = 0, glass = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack s = chest.getItem(i);
            if (s.is(ModItems.STAINLESS_STEEL_PIPE.get())) steel += s.getCount();
            else if (s.is(ModItems.GLASS_PIPE.get())) glass += s.getCount();
            else h.assertTrue(s.isEmpty(), "nothing else in the chest: " + s);
        }
        h.assertTrue(steel == 35 && glass == 5, "the chest: " + steel + " steel, " + glass + " glass pipes");
        h.succeed();
    }

    // ------------------------------------------------------------------ the filter lines

    /**
     * An M40 filter (inlet and outlet facing east) with its intake line at y 2 and its return line right above it (old lines of
     * two kinds side by side never joined) into a tank: after loading two steel lines that still do not join, the intake line blue,
     * the return line orange, and the filter cleans as before once guarded.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void pipeMigrationOldFilterKeepsFiltering(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        MultiblockFilter.forget();
        InteriorPlumbing.forget();
        FilterPumpHeadBlockEntity head = FilterLinesGameTests.filter(h, new BlockPos(0, 2, 4), Direction.EAST);
        TankBlockEntity t = PlumbingGameTests.tank(h, 4, 2, 4, 2, 2, 2);
        for (int x = 2; x <= 3; x++) {
            oldPipe(h, x, 2, 4, "filter_inlet_pipe", false, Direction.WEST, Direction.EAST);
            oldPipe(h, x, 3, 4, "filter_outlet_pipe", false, Direction.WEST, Direction.EAST);
        }
        BlockPos p = abs(h, 2, 2, 4);
        h.assertTrue(level.getBlockState(p).is(ModBlocks.STAINLESS_STEEL_PIPE.get()), "a stainless steel pipe now: " + level.getBlockState(p));
        h.assertTrue(level.getBlockEntity(p) instanceof PipeBlockEntity be && be.legacy() && be.closed(Direction.UP) && be.closed(Direction.DOWN)
                && !be.closed(Direction.EAST), "it closes the sides it was not joined on");
        PipeUpdates.flush(level);
        h.assertTrue(!(level.getBlockEntity(p) instanceof PipeBlockEntity be2) || !be2.legacy(), "migrated");
        h.assertTrue(PipeBlock.sides(level.getBlockState(p)) == (PipeSlots.bit(Direction.WEST) | PipeSlots.bit(Direction.EAST)), "exactly its old sides: " + level.getBlockState(p));
        PipeNetwork in = PipeNetwork.of(level, p), out = PipeNetwork.of(level, abs(h, 2, 3, 4));
        h.assertTrue(in != out && in.role() == PipeRole.INTAKE && out.role() == PipeRole.RETURN, "two lines apart: " + in.role() + " / " + out.role());
        h.assertTrue(level.getBlockState(p).getValue(PipeBlock.RING) == PipeRing.INTAKE && level.getBlockState(abs(h, 3, 3, 4)).getValue(PipeBlock.RING) == PipeRing.RETURN, "their rings");
        FilterLinesGameTests.guardAll(level, head.getBlockPos());
        double w0 = TankWater.filterBlocks(level, t);
        int moved = head.run(level, true);
        h.assertTrue(moved > 0 && head.tanks().get(0).status() == MultiblockFilter.LinkStatus.OK && head.problem() == MultiblockFilter.Problem.NONE, "it filters: " + head.tanks());
        h.assertTrue(PlumbingGameTests.near(TankWater.filterBlocks(level, t) - w0, 36, 0.01), "as before: " + (TankWater.filterBlocks(level, t) - w0));
        h.succeed();
    }

    /**
     * An M40 bundle with the intake (west-east) and the return line (from above, east) into a tank, covered with stone bricks, next
     * to its old pipes; a T-piece of the waste line with three sides. After loading: the bundle's slots carry exactly their old
     * connections and its cover, the filter cleans; the T-piece keeps its three sides and a new pipe beside it does not join it.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void pipeMigrationOldBundleAndTee(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        MultiblockFilter.forget();
        InteriorPlumbing.forget();
        FilterPumpHeadBlockEntity head = FilterLinesGameTests.filter(h, new BlockPos(0, 2, 4), Direction.EAST);
        TankBlockEntity t = PlumbingGameTests.tank(h, 4, 2, 4, 2, 1, 2);
        oldPipe(h, 2, 2, 4, "filter_inlet_pipe", false, Direction.WEST, Direction.EAST);
        oldPipe(h, 2, 3, 4, "filter_outlet_pipe", false, Direction.WEST, Direction.EAST);
        oldPipe(h, 3, 3, 4, "filter_outlet_pipe", false, Direction.WEST, Direction.DOWN);
        int intake = 0x80 | PipeSlots.bit(Direction.WEST) | PipeSlots.bit(Direction.EAST), ret = 0x80 | PipeSlots.bit(Direction.UP) | PipeSlots.bit(Direction.EAST);
        CompoundTag bt = new CompoundTag();
        bt.putInt("lines", intake | ret << 8);
        bt.put("cover", NbtUtils.writeBlockState(Blocks.STONE_BRICKS.defaultBlockState()));
        old(h, new BlockPos(3, 2, 4), ModBlocks.FILTER_PIPE_BUNDLE.get().defaultBlockState(), "filter_pipe", bt);
        BlockPos b = abs(h, 3, 2, 4);
        int[] slots = Pipes.slotsAt(level, b);
        h.assertTrue(PipeSlots.count(slots) == 2 && PipeSlots.steel(slots[0]) && PipeSlots.steel(slots[1]), "two steel slots: " + java.util.Arrays.toString(slots));
        h.assertTrue(level.getBlockEntity(b) instanceof PipeBlockEntity be && Blocks.STONE_BRICKS.defaultBlockState().equals(be.cover()), "its cover stays");
        // a T-piece of the waste line: north, south, east (and the old pipes there)
        oldPipe(h, 8, 2, 7, "filter_waste_pipe", false, Direction.SOUTH);
        oldPipe(h, 8, 2, 9, "filter_waste_pipe", false, Direction.NORTH);
        oldPipe(h, 9, 2, 8, "filter_waste_pipe", false, Direction.WEST);
        oldPipe(h, 8, 2, 8, "filter_waste_pipe", true, Direction.NORTH, Direction.SOUTH, Direction.EAST);
        PipeUpdates.flush(level);
        slots = Pipes.slotsAt(level, b);
        h.assertTrue(PipeSlots.sides(slots[0]) == (PipeSlots.bit(Direction.WEST) | PipeSlots.bit(Direction.EAST)), "the intake slot as before");
        h.assertTrue(PipeSlots.sides(slots[1]) == (PipeSlots.bit(Direction.UP) | PipeSlots.bit(Direction.EAST)), "the return slot as before");
        h.assertTrue(PipeNetwork.of(level, b, 0).role() == PipeRole.INTAKE && PipeNetwork.of(level, b, 1).role() == PipeRole.RETURN, "their roles");
        FilterLinesGameTests.guardAll(level, head.getBlockPos());
        double w0 = TankWater.filterBlocks(level, t);
        h.assertTrue(head.run(level, true) > 0 && head.tanks().get(0).status() == MultiblockFilter.LinkStatus.OK, "it filters: " + head.tanks());
        h.assertTrue(PlumbingGameTests.near(TankWater.filterBlocks(level, t) - w0, 36, 0.01), "36 water blocks");
        BlockPos tee = abs(h, 8, 2, 8);
        int teeSides = PipeSlots.bit(Direction.NORTH) | PipeSlots.bit(Direction.SOUTH) | PipeSlots.bit(Direction.EAST);
        h.assertTrue(PipeBlock.sides(level.getBlockState(tee)) == teeSides && PipeNetwork.of(level, tee).size() == 4, "the T-piece keeps its three sides");
        h.setBlock(new BlockPos(7, 2, 8), ModBlocks.GLASS_PIPE.get().defaultBlockState());
        h.assertTrue(PipeBlock.sides(level.getBlockState(tee)) == teeSides && !PipeNetwork.of(level, tee).contains(abs(h, 7, 2, 8)), "a new pipe beside it stays apart (closed there)");
        h.succeed();
    }

    /**
     * The filter's old fresh water port (facing south, its water in the port) with its line: a plain stainless steel pipe now that
     * joins its old line, which is a plain water line; nothing of the port's block entity is in the way.
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void pipeMigrationOldPort(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        oldPipe(h, 5, 2, 6, "filter_fresh_pipe", false, Direction.NORTH, Direction.SOUTH);
        oldPipe(h, 5, 2, 7, "filter_fresh_pipe", false, Direction.NORTH);
        CompoundTag water = new CompoundTag();
        water.putInt("amount", 4000);
        BlockEntity port = old(h, new BlockPos(5, 2, 5), oldState(level, "filter_fresh_port", Map.of("facing", "south", "medium", "none", "active", "false")),
                "filter_port", water);
        BlockPos p = abs(h, 5, 2, 5);
        h.assertTrue(level.getBlockState(p).is(ModBlocks.STAINLESS_STEEL_PIPE.get()) && port instanceof PipeBlockEntity, "a steel pipe with a pipe's block entity: " + port);
        PipeUpdates.flush(level);
        h.assertTrue(level.getBlockState(p).getValue(PipeBlock.SIDES.get(Direction.SOUTH)), "it joins its old line");
        PipeNetwork n = PipeNetwork.of(level, p);
        h.assertTrue(n.size() == 3 && n.role() == null && n.carriesWater() && level.getBlockState(abs(h, 5, 2, 7)).getValue(PipeBlock.RING) == PipeRing.NONE,
                "a plain water line of three pipes: " + n.size());
        h.succeed();
    }
}
