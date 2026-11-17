package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity;
import org.overengineer.bettacraft.filter.MultiblockFilter;
import org.overengineer.bettacraft.plumbing.FilterPipeBlockEntity;
import org.overengineer.bettacraft.plumbing.FilterPipeBundleBlock;
import org.overengineer.bettacraft.plumbing.FilterPipes;
import org.overengineer.bettacraft.plumbing.PipeCovers;
import org.overengineer.bettacraft.plumbing.PipeLine;
import org.overengineer.bettacraft.plumbing.interior.InteriorPlumbing;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/**
 * M40 part c GameTests: covers on the filter pipes. The pipe cover (main hand) with a block in the off hand covers a pipe
 * and a bundle by a real right-click ({@code ServerPlayerGameMode.useItemOn}): full shape, the block used up and kept, a log
 * turned by the clicked face, a lamp shines, the filter still cleans through the covered pipes; the cover goes along when a
 * pipe becomes a bundle and back; a torch or a chest are refused; sneak-right-click gives the block back; breaking drops it.
 * The switch {@code features.pipes.pipeCovers} off (own batch). With {@code -Pstorage} / {@code -Ppower} / {@code -Pfacades}:
 * the facades of AE2, EnderIO and Mekanism Covers cover our pipes (their block read from the item data, the facade given
 * back), and Cable Facades puts its own facade on them through its block tag (the pipe still filters, hidden under it).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PipeCoverGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private PipeCoverGameTests() {}

    private static BlockPos abs(GameTestHelper h, int x, int y, int z) {
        return h.absolutePos(new BlockPos(x, y, z));
    }

    /** A filter at (0,2,4) facing east, an inlet pipe (2,2,4), a bundle of intake + return (3,2,4) into a tank at (4,2,4), guarded. */
    private record Rig(FilterPumpHeadBlockEntity head, TankBlockEntity tank, BlockPos pipe, BlockPos bundle) {}

    private static Rig rig(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        MultiblockFilter.forget();
        InteriorPlumbing.forget();
        FilterPumpHeadBlockEntity head = FilterLinesGameTests.filter(h, new BlockPos(0, 2, 4), Direction.EAST);
        h.setBlock(new BlockPos(2, 2, 4), ModBlocks.FILTER_INLET_PIPE.get().defaultBlockState());
        h.setBlock(new BlockPos(2, 3, 4), ModBlocks.FILTER_OUTLET_PIPE.get().defaultBlockState());
        h.setBlock(new BlockPos(3, 3, 4), ModBlocks.FILTER_OUTLET_PIPE.get().defaultBlockState());
        TankBlockEntity t = PlumbingGameTests.tank(h, 4, 2, 4, 2, 1, 2);
        h.setBlock(new BlockPos(3, 2, 4), ModBlocks.FILTER_INLET_PIPE.get().defaultBlockState());
        if (!FilterPipeBundleBlock.addLine(level, abs(h, 3, 2, 4), PipeLine.RETURN, null, null)) throw new GameTestFailure("no bundle");
        FilterLinesGameTests.guardAll(level, head.getBlockPos());
        return new Rig(head, t, abs(h, 2, 2, 4), abs(h, 3, 2, 4));
    }

    /** The filter of the rig cleans its tank: 36 water blocks in one run. */
    private static void filters(GameTestHelper h, Rig r, String when) {
        ServerLevel level = h.getLevel();
        r.head().rescan();
        int moved = r.head().run(level, true);
        h.assertTrue(moved > 0 && !r.head().tanks().isEmpty() && r.head().tanks().get(0).status() == MultiblockFilter.LinkStatus.OK,
                "it filters " + when + ": " + r.head().tanks());
        double served = MultiblockFilter.serving(level, r.tank())[1];
        h.assertTrue(PlumbingGameTests.near(served, 36, 0.01), "36 water blocks " + when + ": " + served);
    }

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer p = MockPlayers.serverPlayer(h);
        p.getAbilities().instabuild = false;
        p.getInventory().clearContent();
        return p;
    }

    /** A right-click on {@code pos} (from above) like a player's: block first, then the item. */
    private static InteractionResult click(ServerPlayer p, BlockPos pos, Direction face) {
        Vec3 hit = Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(0.5));
        return p.gameMode.useItemOn(p, p.level(), p.getMainHandItem(), InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, pos, false));
    }

    @javax.annotation.Nullable
    private static BlockState cover(GameTestHelper h, BlockPos pos) {
        return PipeCovers.coverAt(h.getLevel(), pos);
    }

    private static boolean full(GameTestHelper h, BlockPos pos) {
        return Block.isShapeFullBlock(h.getLevel().getBlockState(pos).getShape(h.getLevel(), pos, CollisionContext.empty()));
    }

    // ------------------------------------------------------------------ our pipe cover

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void pipeCoverHidesPipesThatStillFilter(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Rig r = rig(h);
        filters(h, r, "without covers");
        ServerPlayer p = player(h);
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PIPE_COVER.get()));
            // a torch and a chest are no covers
            p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH, 4));
            click(p, r.pipe(), Direction.UP);
            h.assertTrue(cover(h, r.pipe()) == null && p.getOffhandItem().getCount() == 4, "a torch is refused");
            p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.CHEST, 4));
            click(p, r.pipe(), Direction.UP);
            h.assertTrue(cover(h, r.pipe()) == null && p.getOffhandItem().getCount() == 4, "a chest is refused");
            // stone bricks on the inlet pipe: one used, the pipe cover kept, a full block
            p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE_BRICKS, 4));
            click(p, r.pipe(), Direction.UP);
            h.assertTrue(Blocks.STONE_BRICKS.defaultBlockState().equals(cover(h, r.pipe())), "stone bricks on the pipe: " + cover(h, r.pipe()));
            h.assertTrue(p.getOffhandItem().getCount() == 3 && p.getMainHandItem().is(ModItems.PIPE_COVER.get()), "one block used, the pipe cover kept");
            h.assertTrue(full(h, r.pipe()), "a full block now");
            // an oak log on the bundle, clicked on its east face: lying east-west
            p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.OAK_LOG, 2));
            click(p, r.bundle(), Direction.EAST);
            BlockState log = cover(h, r.bundle());
            h.assertTrue(log != null && log.is(Blocks.OAK_LOG) && log.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X, "an oak log along x on the bundle: " + log);
            h.assertTrue(full(h, r.bundle()) && FilterPipes.lines(level, r.bundle()).size() == 2, "a full block, its two lines kept");
            // the lines and connections stay: the filter cleans as before
            filters(h, r, "under the covers");
            // a lamp shines
            p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GLOWSTONE, 1));
            click(p, r.pipe(), Direction.UP);
            h.assertTrue(cover(h, r.pipe()) != null && cover(h, r.pipe()).is(Blocks.GLOWSTONE), "glowstone on the pipe");
            h.assertTrue(GameTestCompat.light(level.getBlockState(r.pipe()), level, r.pipe(), 15) == 15, "it shines: " + GameTestCompat.light(level.getBlockState(r.pipe()), level, r.pipe(), 15));
            h.assertTrue(p.getInventory().countItem(Items.STONE_BRICKS) == 1, "the stone brick of the cover came back: " + p.getInventory().countItem(Items.STONE_BRICKS));
            // sneak-right-click with the pipe cover: the glowstone back, the pipe again
            p.setShiftKeyDown(true);
            click(p, r.pipe(), Direction.UP);
            p.setShiftKeyDown(false);
            h.assertTrue(cover(h, r.pipe()) == null && !full(h, r.pipe()), "the cover is off");
            h.assertTrue(p.getInventory().countItem(Items.GLOWSTONE) == 1, "the glowstone back");
            h.assertTrue(GameTestCompat.light(level.getBlockState(r.pipe()), level, r.pipe(), 0) == 0, "no light without it");
            // the bundle keeps its cover when a line goes out (a single pipe again) and in again
            h.assertTrue(FilterPipeBundleBlock.removeLine(level, r.bundle(), PipeLine.RETURN).size() == 1, "the return line out");
            h.assertTrue(level.getBlockState(r.bundle()).is(ModBlocks.FILTER_INLET_PIPE.get()) && log.equals(cover(h, r.bundle())), "a single pipe, still the log");
            h.assertTrue(FilterPipeBundleBlock.addLine(level, r.bundle(), PipeLine.RETURN, null, null), "the return line in again");
            h.assertTrue(log.equals(cover(h, r.bundle())), "a bundle, still the log");
            // breaking gives the pipes and the log
            List<ItemStack> drops = Block.getDrops(level.getBlockState(r.bundle()), level, r.bundle(), level.getBlockEntity(r.bundle()));
            h.assertTrue(drops.size() == 3 && drops.stream().anyMatch(s -> s.is(Items.OAK_LOG)), "drops of the covered bundle: " + drops);
            h.setBlock(new BlockPos(5, 5, 5), ModBlocks.FILTER_WASTE_PIPE.get().defaultBlockState());
            BlockPos single = abs(h, 5, 5, 5);
            h.assertTrue(PipeCovers.apply(level, single, Blocks.BRICKS.defaultBlockState(), new ItemStack(Items.BRICKS), null), "bricks on a single pipe");
            List<ItemStack> sd = Block.getDrops(level.getBlockState(single), level, single, level.getBlockEntity(single));
            h.assertTrue(sd.size() == 2 && sd.stream().anyMatch(s -> s.is(Items.BRICKS)) && sd.stream().anyMatch(s -> s.is(ModItems.FILTER_WASTE_PIPE.get())), "drops of a covered pipe: " + sd);
        } finally {
            p.level().getServer().getPlayerList().remove(p);
        }
        h.succeed();
    }

    /** features.pipes.pipeCovers off: no new cover goes on; one already on stays and comes off (own batch). */
    @GameTest(template = EMPTY, batch = "bettacraft_m40_covers_off", timeoutTicks = 200)
    public static void pipeCoversOff(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        h.setBlock(new BlockPos(2, 2, 2), ModBlocks.FILTER_INLET_PIPE.get().defaultBlockState());
        h.setBlock(new BlockPos(4, 2, 2), ModBlocks.FILTER_INLET_PIPE.get().defaultBlockState());
        BlockPos a = abs(h, 2, 2, 2), b = abs(h, 4, 2, 2);
        h.assertTrue(PipeCovers.apply(level, b, Blocks.STONE.defaultBlockState(), new ItemStack(Items.STONE), null), "a cover while it is on");
        boolean saved = Features.own(Feature.PIPE_COVERS);
        try {
            Features.set(Feature.PIPE_COVERS, false);
            h.assertTrue(!PipeCovers.apply(level, a, Blocks.STONE.defaultBlockState(), new ItemStack(Items.STONE), null) && cover(h, a) == null, "no new cover");
            h.assertTrue(cover(h, b) != null, "the old cover stays");
            h.assertTrue(PipeCovers.remove(level, b).is(Items.STONE) && cover(h, b) == null, "and comes off");
        } finally {
            Features.set(Feature.PIPE_COVERS, saved);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ the facades of other mods

    private static boolean loaded(String... mods) {
        for (String m : mods) if (!org.overengineer.bettacraft.platform.Platform.get().isModLoaded(m)) return false;
        return true;
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).orElseThrow(() -> new GameTestFailure("no item " + id));
    }

    @SuppressWarnings("unchecked")
    private static void set(ItemStack stack, String component, Object value) {
        DataComponentType<Object> t = (DataComponentType<Object>) BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.parse(component));
        if (t == null) throw new GameTestFailure("no data component " + component);
        stack.set(t, value);
    }

    /** Another mod's facade used on the inlet pipe: stone bricks on it, the facade kept and given back by the pipe cover. */
    private static void foreignFacade(GameTestHelper h, ItemStack facade) {
        ServerLevel level = h.getLevel();
        Rig r = rig(h);
        h.assertTrue(PipeCovers.foreign(facade, level) != null && PipeCovers.foreign(facade, level).is(Blocks.STONE_BRICKS),
                "the facade carries stone bricks: " + PipeCovers.foreign(facade, level));
        ServerPlayer p = player(h);
        try {
            ItemStack held = facade.copyWithCount(2);
            p.setItemInHand(InteractionHand.MAIN_HAND, held);
            click(p, r.pipe(), Direction.UP);
            h.assertTrue(Blocks.STONE_BRICKS.defaultBlockState().equals(cover(h, r.pipe())), "stone bricks on the pipe: " + cover(h, r.pipe()));
            h.assertTrue(p.getMainHandItem().getCount() == 1, "one facade used: " + p.getMainHandItem());
            click(p, r.bundle(), Direction.UP);
            h.assertTrue(cover(h, r.bundle()) != null && full(h, r.bundle()), "and on the bundle");
            filters(h, r, "under the facades");
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PIPE_COVER.get()));
            p.setShiftKeyDown(true);
            click(p, r.pipe(), Direction.UP);
            p.setShiftKeyDown(false);
            h.assertTrue(cover(h, r.pipe()) == null, "the facade is off");
            h.assertTrue(p.getInventory().getNonEquipmentItems().stream().anyMatch(s -> s.is(facade.getItem()) && PipeCovers.foreign(s, level) != null),
                    "the facade is back, with its block: " + p.getInventory().getNonEquipmentItems().stream().filter(s -> !s.isEmpty()).toList());
            List<ItemStack> drops = Block.getDrops(level.getBlockState(r.bundle()), level, r.bundle(), level.getBlockEntity(r.bundle()));
            h.assertTrue(drops.stream().anyMatch(s -> s.is(facade.getItem())), "breaking the bundle drops the facade: " + drops);
        } finally {
            p.level().getServer().getPlayerList().remove(p);
        }
        h.succeed();
    }

    /** Applied Energistics 2: a cable facade of stone bricks ({@code ae2:facade_item} holds the block's item). */
    @GameTest(template = EMPTY, batch = "bettacraft_storage_facades", timeoutTicks = 200)
    public static void ae2FacadeCoversAFilterPipe(GameTestHelper h) throws Exception {
        if (!loaded("ae2")) {
            h.succeed();
            return;
        }
        Item facade = item("ae2:facade");
        ItemStack stack = (ItemStack) facade.getClass().getMethod("createFacadeForItem", ItemStack.class, boolean.class)
                .invoke(facade, new ItemStack(Items.STONE_BRICKS), false);
        h.assertTrue(!stack.isEmpty(), "AE2 makes a stone brick facade");
        foreignFacade(h, stack);
    }

    /** EnderIO: a conduit facade painted with stone bricks ({@code enderio:block_paint}). */
    @GameTest(template = EMPTY, batch = "bettacraft_power_facades", timeoutTicks = 200)
    public static void enderioFacadeCoversAFilterPipe(GameTestHelper h) throws Exception {
        if (!loaded("enderio")) {
            h.succeed();
            return;
        }
        ItemStack stack = new ItemStack(item("enderio:conduit_facade"));
        Object paint = Class.forName("com.enderio.enderio.content.paint.BlockPaintData").getMethod("of", Block.class).invoke(null, Blocks.STONE_BRICKS);
        set(stack, "enderio:block_paint", paint);
        foreignFacade(h, stack);
    }

    /** Mekanism Covers: a cover of stone bricks ({@code mekanismcovers:cover_block} holds the block id). */
    @GameTest(template = EMPTY, batch = "bettacraft_power_facades", timeoutTicks = 200)
    public static void mekanismCoverCoversAFilterPipe(GameTestHelper h) {
        if (!loaded("mekanismcovers")) {
            h.succeed();
            return;
        }
        ItemStack stack = new ItemStack(item("mekanismcovers:cover"));
        set(stack, "mekanismcovers:cover_block", BuiltInRegistries.BLOCK.getKey(Blocks.STONE_BRICKS));
        foreignFacade(h, stack);
    }

    /**
     * Cable Facades: the filter pipes and the bundle are in its block tag {@code cable_facades:supports_facade} (our data,
     * its config untouched); its facade item used on a pipe like a player puts its facade there (its world data, not our
     * cover), our pipes are hidden under it (registered through its API) and the filter still cleans through them.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_facades_cable", timeoutTicks = 200)
    public static void cableFacadesOnAFilterPipe(GameTestHelper h) throws Exception {
        if (!loaded("cable_facades")) {
            h.succeed();
            return;
        }
        ServerLevel level = h.getLevel();
        Rig r = rig(h);
        TagKey<Block> tag = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("cable_facades", "supports_facade"));
        for (Block b : List.of(ModBlocks.FILTER_INLET_PIPE.get(), ModBlocks.FILTER_OUTLET_PIPE.get(), ModBlocks.FILTER_FRESH_PIPE.get(),
                ModBlocks.FILTER_WASTE_PIPE.get(), ModBlocks.FILTER_PIPE_BUNDLE.get())) {
            h.assertTrue(b.defaultBlockState().is(tag), BuiltInRegistries.BLOCK.getKey(b) + " is in cable_facades:supports_facade");
        }
        Class<?> config = Class.forName("com.portingdeadmods.cable_facades.CFConfig");
        h.assertTrue((boolean) config.getMethod("shouldHideWhenFacaded", Block.class).invoke(null, ModBlocks.FILTER_INLET_PIPE.get()),
                "our pipes are hidden under its facades (API)");
        Class<?> utils = Class.forName("com.portingdeadmods.cable_facades.utils.FacadeUtils");
        ItemStack facade = new ItemStack(item("cable_facades:facade"), 2);
        set(facade, "cable_facades:facade_block", Optional.of(Blocks.STONE_BRICKS));
        h.assertTrue(PipeCovers.foreign(facade, level) == null, "its facades are its own (not our covers)");
        ServerPlayer p = player(h);
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND, facade);
            for (BlockPos at : List.of(r.pipe(), r.bundle())) {
                InteractionResult res = click(p, at, Direction.UP);
                Object state = utils.getMethod("getFacade", net.minecraft.world.level.BlockGetter.class, BlockPos.class).invoke(null, level, at);
                h.assertTrue((boolean) utils.getMethod("hasFacade", net.minecraft.world.level.BlockGetter.class, BlockPos.class).invoke(null, level, at),
                        "a Cable Facades facade on " + level.getBlockState(at).getBlock() + " (" + res + ")");
                h.assertTrue(state instanceof BlockState s && s.is(Blocks.STONE_BRICKS), "of stone bricks: " + state);
                h.assertTrue(cover(h, at) == null, "not our cover");
            }
            filters(h, r, "under Cable Facades");
        } finally {
            p.level().getServer().getPlayerList().remove(p);
        }
        h.succeed();
    }

    /** The pipe cover's item is in the feature table and the pipe blocks keep their block entity type. */
    @GameTest(template = EMPTY)
    public static void pipeCoverBasics(GameTestHelper h) {
        h.assertTrue(org.overengineer.bettacraft.config.FeatureContent.ofItem("pipe_cover").contains(Feature.PIPE_COVERS), "pipe_cover belongs to pipes.pipeCovers");
        h.setBlock(new BlockPos(1, 2, 1), ModBlocks.FILTER_FRESH_PIPE.get().defaultBlockState());
        h.assertTrue(h.getLevel().getBlockEntity(abs(h, 1, 2, 1)) instanceof FilterPipeBlockEntity, "a block entity for the cover");
        h.succeed();
    }
}
