package org.overengineer.bettacraft.gametest;

import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.PackedTank;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankMoveMap;
import org.overengineer.bettacraft.tank.TankMoves;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorCleanup;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M53c: removed interiors leave nothing on the disk and their numbers are handed out again; the interiors of standing, packed
 * and travelling tanks are never touched; grown, split, merged and turned tanks lose nothing; empty interior chunks are not
 * stored; the start cleans an old world's folder; the switch off keeps everything as before.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InteriorCleanupGameTests {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/gametest");
    private static final String EMPTY = "empty_32x10x32";
    private static final String BATCH = "bettacraft_m53c_cleanup";

    private InteriorCleanupGameTests() {}

    private static ServerLevel interior(GameTestHelper h) {
        ServerLevel in = InteriorManager.interiorLevel(h.getLevel().getServer());
        if (in == null) throw new GameTestAssertException("no interior dimension");
        return in;
    }

    /** Nothing of the cell on the disk any more (chunks and stored entities). */
    private static boolean gone(MinecraftServer server, int id) {
        return InteriorCleanup.storedChunks(server, id, "region").isEmpty() && InteriorCleanup.storedChunks(server, id, "entities").isEmpty();
    }

    private static void breakTank(GameTestHelper h, TankData t) {
        for (long m : new ArrayList<>(t.members())) h.getLevel().setBlockAndUpdate(BlockPos.of(m), Blocks.AIR.defaultBlockState());
    }

    /** A blocks scan of a region's interior: whether the block is somewhere in it. */
    private static boolean contains(ServerLevel in, InteriorRegion r, net.minecraft.world.level.block.Block b) {
        for (int x = 0; x < r.sizeX(); x++)
            for (int y = 0; y < Math.min(4, r.sizeY()); y++)
                for (int z = 0; z < r.sizeZ(); z++) if (in.getBlockState(r.toWorld(x, y, z)).is(b)) return true;
        return false;
    }

    /** A tank broken: its region is dead, its chunks leave the disk, the number goes to the next tank, whose interior is empty. */
    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 12000)
    public static void aBrokenTanksInteriorLeavesTheDiskAndItsNumberIsReused(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        MinecraftServer server = level.getServer();
        ServerLevel in = interior(h);
        InteriorRegions regions = InteriorRegions.get(server);
        TankBlockEntity c = InteriorGameTests.tank(h, 2, 1, 2, 1, 1, 1, 0);
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        int id = r.id();
        BlockPos diamond = r.toWorld(5, 0, 5);
        in.setBlock(diamond, Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
        breakTank(h, c.getTank());
        h.assertTrue(!regions.living(id) && regions.dying().contains(id), "the broken tank's region " + id + " is dying (until the world is saved)");
        InteriorCleanup.runNow(server);
        boolean[] done = {false};
        h.succeedWhen(() -> {
            if (done[0]) return;
            if (!regions.free().contains(id)) throw new GameTestAssertException("not free yet: " + InteriorCleanup.describe(server, id));
            h.assertTrue(gone(server, id), "nothing of region " + id + " is on the disk");
            int lowest = regions.free().firstInt();
            TankBlockEntity n = InteriorGameTests.tank(h, 6, 1, 2, 1, 1, 1, 0);
            InteriorRegion nr = InteriorManager.ensureRegion(level, n);
            h.assertTrue(nr.id() == lowest, "the lowest free number " + lowest + " is handed out again (got " + nr.id() + ")");
            h.assertTrue(nr.serial() != r.serial(), "a new serial for the reused number");
            h.assertTrue(!contains(in, nr, Blocks.DIAMOND_BLOCK), "the new interior is empty");
            if (nr.id() == id) h.assertTrue(!in.getBlockState(diamond).is(Blocks.DIAMOND_BLOCK), "the old diamond block is gone");
            done[0] = true;
        });
    }

    /**
     * A removed region is not touched before the whole world was saved (a crash would go back to a world whose tank still
     * points to it): it stays dying, its chunks stay; the save makes it dead.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m53c_cleanup_dying", timeoutTicks = 12000)
    public static void aRemovedRegionWaitsForTheNextSaveOfTheWorld(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        MinecraftServer server = level.getServer();
        ServerLevel in = interior(h);
        InteriorRegions regions = InteriorRegions.get(server);
        TankBlockEntity c = InteriorGameTests.tank(h, 2, 1, 2, 1, 1, 1, 0);
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        int id = r.id();
        // a block in the region's cell but outside the interior (taking the interior down leaves it): a stored chunk to watch
        BlockPos marker = r.toWorld(r.sizeX() + 40, 0, 5);
        in.setBlock(marker, Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
        ChunkPos mc = new ChunkPos(marker);
        breakTank(h, c.getTank());
        int saves = InteriorCleanup.worldSaves;
        h.assertTrue(regions.dying().contains(id) && !regions.dead().contains(id), "dying, not dead");
        in.save(null, true, false); // the interior's chunks to the disk (not a save of the whole world)
        h.runAfterDelay(500, () -> {
            if (InteriorCleanup.worldSaves == saves) {
                h.assertTrue(regions.dying().contains(id), "still dying without a save of the world: " + id);
                h.assertTrue(InteriorCleanup.storedChunks(server, id, "region").contains(mc), "its chunks are still on the disk");
            } else {
                LOGGER.info("M53c: an autosave came in between, region {} may be dead already", id);
            }
            server.saveEverything(true, false, false);
            h.assertTrue(!regions.dying().contains(id) && (regions.dead().contains(id) || regions.free().contains(id)), "dead after the save");
            InteriorCleanup.runNow(server);
            h.succeedWhen(() -> {
                if (!regions.free().contains(id)) throw new GameTestAssertException("not free yet: " + InteriorCleanup.describe(server, id));
                h.assertTrue(gone(server, id), "nothing of region " + id + " is on the disk");
                h.assertTrue(!in.getBlockState(marker).is(Blocks.DIAMOND_BLOCK), "the block outside the interior is gone too");
            });
        });
    }

    /** Grown, split, merged and turned: every block comes along, the old regions leave nothing on the disk. */
    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 12000)
    public static void growingSplittingMergingAndTurningLoseNothingAndLeaveNothing(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        MinecraftServer server = level.getServer();
        ServerLevel in = interior(h);
        InteriorRegions regions = InteriorRegions.get(server);
        TankBlockEntity c = InteriorGameTests.tank(h, 2, 1, 2, 3, 1, 1, 0);
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        List<Integer> old = new ArrayList<>(List.of(r.id()));
        in.setBlock(r.toWorld(5, 0, 5), Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
        in.setBlock(r.toWorld(2 * r.scale() + 5, 0, 5), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        // split: the middle block goes
        level.setBlockAndUpdate(h.absolutePos(new BlockPos(3, 1, 2)), Blocks.AIR.defaultBlockState());
        TankBlockEntity a = TankMultiblock.getController(level, h.absolutePos(new BlockPos(2, 1, 2)));
        TankBlockEntity b = TankMultiblock.getController(level, h.absolutePos(new BlockPos(4, 1, 2)));
        h.assertTrue(a != null && b != null && a != b, "split into two tanks");
        InteriorRegion ra = InteriorManager.ensureRegion(level, a), rb = InteriorManager.ensureRegion(level, b);
        h.assertTrue(contains(in, ra, Blocks.DIAMOND_BLOCK) && contains(in, rb, Blocks.GOLD_BLOCK), "each piece keeps its block");
        old.add(ra.id());
        old.add(rb.id());
        // grow: the middle block again merges them
        h.setBlock(new BlockPos(3, 1, 2), ModBlocks.TANK.get().defaultBlockState());
        TankMultiblock.onTankBlockAdded(level, h.absolutePos(new BlockPos(3, 1, 2)), null);
        TankBlockEntity m = TankMultiblock.getController(level, h.absolutePos(new BlockPos(2, 1, 2)));
        h.assertTrue(m != null && m.getTank().members().size() == 3, "merged again");
        InteriorRegion rm = InteriorManager.ensureRegion(level, m);
        h.assertTrue(contains(in, rm, Blocks.DIAMOND_BLOCK) && contains(in, rm, Blocks.GOLD_BLOCK), "the merged tank has both blocks");
        // turned (as a mover brings it back a quarter turn round): the data of the tank as it would arrive
        TankData before = TankData.load(m.getTank().save());
        TankData moved = TankData.load(m.getTank().save());
        BlockPos from = m.getTank().min();
        TankMoveMap map = new TankMoveMap(from, from.offset(0, 0, 8), TankMoveMap.rotations().get(1));
        moved.relocate(map, null);
        InteriorManager.onTankTurned(level, before, moved, map);
        InteriorRegion rt = InteriorRegions.get(server).get(moved.regionId());
        h.assertTrue(rt != null && rt.id() != rm.id(), "the turned tank has a new region");
        h.assertTrue(contains(in, rt, Blocks.DIAMOND_BLOCK) && contains(in, rt, Blocks.GOLD_BLOCK), "the turned tank has both blocks");
        old.add(rm.id());
        int turnedId = rt.id();
        for (int id : old) {
            h.assertTrue(!regions.living(id) && (regions.dying().contains(id) || regions.dead().contains(id) || regions.free().contains(id)),
                    "old region " + id + " is removed");
        }
        InteriorCleanup.runNow(server);
        boolean[] done = {false};
        h.succeedWhen(() -> {
            if (done[0]) return;
            for (int id : old) {
                if (!regions.free().contains(id) && !regions.living(id)) throw new GameTestAssertException("old region not free yet: " + InteriorCleanup.describe(server, id));
                h.assertTrue(regions.living(id) || gone(server, id), "nothing of old region " + id + " is on the disk");
            }
            h.assertTrue(regions.living(turnedId) && contains(in, rt, Blocks.DIAMOND_BLOCK) && contains(in, rt, Blocks.GOLD_BLOCK),
                    "the turned tank still has its blocks after the clean-up");
            // the turned data never stood in the world: let its region go (the test area's tank goes with the area)
            InteriorManager.freeRegion(level, moved);
            done[0] = true;
        });
    }

    /** The interiors of a packed tank and of a travelling tank are never touched; a removed one with its shell is kept. */
    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 12000)
    public static void packedAndTravellingTanksSurviveTheCleanUp(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        MinecraftServer server = level.getServer();
        ServerLevel in = interior(h);
        InteriorRegions regions = InteriorRegions.get(server);
        // packed: what a mover holds when it cannot put the tank down (CreateLandings)
        TankBlockEntity p = InteriorGameTests.tank(h, 2, 1, 2, 1, 1, 1, 0);
        InteriorRegion rp = InteriorManager.ensureRegion(level, p);
        in.setBlock(rp.toWorld(5, 0, 5), Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
        CompoundTag data = p.saveWithoutMetadata(level.registryAccess());
        data.put("stowed", TankMoves.pack(level, p.getTank(), new ArrayList<Entity>()));
        BlockPos old = p.getBlockPos();
        level.removeBlockEntity(old);
        level.setBlock(old, Blocks.AIR.defaultBlockState(), 3);
        ItemStack packed = PackedTank.pack(new ItemStack(ModBlocks.TANK.get()), data);
        // travelling: on a contraption somewhere
        TankBlockEntity t = InteriorGameTests.tank(h, 8, 1, 2, 1, 1, 1, 0);
        InteriorRegion rt = InteriorManager.ensureRegion(level, t);
        in.setBlock(rt.toWorld(5, 0, 5), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        TankMoves.travellingRegion(rt.id(), level, () -> Vec3.atCenterOf(t.getBlockPos()));
        // removed from the list without taking the shell down (as an older version after a crash could leave it): kept
        TankBlockEntity k = InteriorGameTests.tank(h, 14, 1, 2, 1, 1, 1, 0);
        InteriorRegion rk = InteriorManager.ensureRegion(level, k);
        regions.remove(rk.id());
        k.getTank().setRegionId(-1);
        // and a broken tank to wait for
        TankBlockEntity x = InteriorGameTests.tank(h, 20, 1, 2, 1, 1, 1, 0);
        int broken = InteriorManager.ensureRegion(level, x).id();
        breakTank(h, x.getTank());
        InteriorCleanup.runNow(server);
        boolean[] done = {false};
        h.succeedWhen(() -> {
            if (done[0]) return;
            if (!regions.free().contains(broken) || !regions.kept().contains(rk.id())) {
                throw new GameTestAssertException("the clean-up is not through yet: " + InteriorCleanup.describe(server, broken) + "; shell "
                        + InteriorCleanup.describe(server, rk.id()));
            }
            h.assertTrue(regions.living(rp.id()) && in.getBlockState(rp.toWorld(5, 0, 5)).is(Blocks.DIAMOND_BLOCK), "the packed tank's interior is there");
            h.assertTrue(regions.living(rt.id()) && in.getBlockState(rt.toWorld(5, 0, 5)).is(Blocks.GOLD_BLOCK), "the travelling tank's interior is there");
            h.assertTrue(!InteriorCleanup.storedChunks(server, rk.id(), "region").isEmpty(), "the kept interior stays on the disk");
            // the packed tank comes back with its interior
            BlockPos at = h.absolutePos(new BlockPos(2, 1, 8));
            level.setBlockAndUpdate(at, Blocks.STONE.defaultBlockState());
            ServerPlayer pl = MockPlayers.serverPlayer(h);
            try {
                pl.setItemInHand(InteractionHand.MAIN_HAND, packed);
                packed.useOn(new UseOnContext(level, pl, InteractionHand.MAIN_HAND, packed,
                        new BlockHitResult(Vec3.atCenterOf(at).add(0, 0.5, 0), Direction.UP, at, false)));
            } finally {
                pl.discard();
            }
            TankMoves.stoppedTravelling(rt.id());
            done[0] = true;
        });
    }

    /** M53c item 2: a small tank stores only the chunks of its shell (before: the 3 rings of generation around it as well). */
    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 2400)
    public static void emptyInteriorChunksAreNotStored(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        MinecraftServer server = level.getServer();
        TankBlockEntity c = InteriorGameTests.tank(h, 2, 1, 2, 2, 2, 2, 0);
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        int id = r.id();
        int[] sc = {(r.originX() - 1) >> 4, (r.originX() + r.sizeX()) >> 4, (r.originZ() - 1) >> 4, (r.originZ() + r.sizeZ()) >> 4};
        int shell = (sc[1] - sc[0] + 1) * (sc[3] - sc[2] + 1);
        @SuppressWarnings("unchecked")
        java.util.concurrent.CompletableFuture<Void>[] sync = new java.util.concurrent.CompletableFuture[1];
        boolean[] done = {false};
        h.succeedWhen(() -> {
            if (done[0]) return;
            if (InteriorCleanup.held(server, id)) throw new GameTestAssertException("the interior chunks are still loaded");
            if (sync[0] == null) sync[0] = InteriorCleanup.sync(server);
            if (!sync[0].isDone()) throw new GameTestAssertException("writing");
            List<ChunkPos> stored = InteriorCleanup.storedChunks(server, id, "region");
            for (ChunkPos p : stored) {
                if (p.x >= sc[0] && p.x <= sc[1] && p.z >= sc[2] && p.z <= sc[3]) continue;
                String what;
                try {
                    CompoundTag t = InteriorCleanup.storedChunk(server, p);
                    what = t == null ? "?" : t.getString("Status") + " " + t.getList("sections", 10).size() + " sections, block entities "
                            + t.getList("block_entities", 10).size() + ", structures " + t.getCompound("structures") + ", "
                            + (t.getList("sections", 10).isEmpty() ? "" : t.getList("sections", 10).getCompound(0).toString());
                } catch (java.io.IOException e) {
                    what = e.toString();
                }
                throw new GameTestAssertException("only shell chunks " + java.util.Arrays.toString(sc) + " are stored, not " + p + ": " + what);
            }
            h.assertTrue(!stored.isEmpty() && stored.size() <= shell, "the shell's chunks are stored: " + stored.size() + " of " + shell);
            LOGGER.info("M53c: a 2x2x2 tank stores {} interior chunks, {} KB (shell {} chunk columns)", stored.size(),
                    InteriorCleanup.storedBytes(server, id) / 1024, shell);
            done[0] = true;
        });
    }

    /** An old world: at the start the files of removed regions go, those of living ones, unknown ones and shells stay. */
    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 100)
    public static void theStartCleansAnOldWorldsFolder(GameTestHelper h) throws Exception {
        Path root = Files.createTempDirectory("bettacraft-m53c");
        try {
            Path dim = root.resolve("dimensions/bettacraft/tank_interior");
            Path region = Files.createDirectories(dim.resolve("region")), entities = Files.createDirectories(dim.resolve("entities"));
            // v4 list (before M53c): living 2, counter at 6; 1, 3, 4 and 5 were removed; 9 is not known (the list older than the world)
            CompoundTag list = TagParser.parseTag("{schema:4,next_id:6,regions:[{id:2,size:[I;16,16,16],scale:16,tank_dim:\"minecraft:overworld\","
                    + "tank_min:[I;0,64,0],built:1b,equipment:{},cells:[B;1B],plants:{list:[],next_id:1}}]}");
            CompoundTag file = new CompoundTag();
            file.put("data", list);
            Path dat = Files.createDirectories(root.resolve("data")).resolve(InteriorRegions.NAME + ".dat");
            NbtIo.writeCompressed(file, dat);
            RegionStorageInfo info = new RegionStorageInfo("test", Level.OVERWORLD, "chunk");
            // 1: removed, shell taken down (an empty chunk); 2: living; 3: removed but its shell is still there; 4: removed, entities only
            // 9: unknown (at the counter or above)
            write(info, region, 1, chunk(false));
            write(info, entities, 1, new CompoundTag());
            write(info, region, 2, chunk(true));
            write(info, region, 3, chunk(true));
            write(info, entities, 4, new CompoundTag());
            write(info, region, 9, chunk(false));
            long[] res = InteriorCleanup.cleanFolder(dat, dim);
            h.assertTrue(!Files.exists(file(region, 1)) && !Files.exists(file(entities, 1)), "the removed region 1 is deleted");
            h.assertTrue(!Files.exists(file(entities, 4)), "the removed region 4 is deleted");
            h.assertTrue(Files.exists(file(region, 2)), "the living region 2 stays");
            h.assertTrue(Files.exists(file(region, 3)), "region 3 with its shell stays");
            h.assertTrue(Files.exists(file(region, 9)), "the unknown region 9 stays");
            h.assertTrue(res[0] == 3 && res[2] == 1, "3 files deleted, 1 shell kept: " + java.util.Arrays.toString(res));
            // the list loaded by the game: 1, 3, 4, 5 dead (the running clean-up then frees 1, 4, 5 and keeps 3)
            CompoundTag migrated = org.overengineer.bettacraft.data.schema.Schemas.INTERIOR_REGIONS.migrate(list);
            h.assertTrue(java.util.Arrays.equals(migrated.getIntArray("dead"), new int[]{1, 3, 4, 5}), "dead after the migration: "
                    + java.util.Arrays.toString(migrated.getIntArray("dead")));
            // a v5 list: 10 dead, 11 free, 7 dying (removed after the last complete save: a crash may have brought its tank back)
            CompoundTag v5 = TagParser.parseTag("{schema:5,next_id:12,regions:[],dying:[I;7],dead:[I;10],free:[I;11]}");
            CompoundTag file5 = new CompoundTag();
            file5.put("data", v5);
            NbtIo.writeCompressed(file5, dat);
            write(info, region, 7, chunk(false));
            write(info, region, 10, chunk(false));
            write(info, entities, 11, new CompoundTag());
            long[] res5 = InteriorCleanup.cleanFolder(dat, dim);
            h.assertTrue(Files.exists(file(region, 7)), "the dying region 7 stays");
            h.assertTrue(!Files.exists(file(region, 10)) && !Files.exists(file(entities, 11)), "the dead 10 and the free 11 go");
            h.assertTrue(res5[0] == 2, "2 files deleted: " + java.util.Arrays.toString(res5));
            h.succeed();
        } finally {
            try (var s = Files.walk(root)) {
                s.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    private static Path file(Path folder, int id) {
        ChunkPos o = origin(id);
        return folder.resolve("r." + o.getRegionX() + "." + o.getRegionZ() + ".mca");
    }

    private static ChunkPos origin(int id) {
        return new ChunkPos(InteriorRegion.originX(id) >> 4, InteriorRegion.originZ(id) >> 4);
    }

    private static CompoundTag chunk(boolean shell) {
        CompoundTag sec = new CompoundTag();
        sec.putByte("Y", (byte) 0);
        CompoundTag states = new CompoundTag();
        ListTag palette = new ListTag();
        CompoundTag air = new CompoundTag();
        air.putString("Name", "minecraft:air");
        palette.add(air);
        if (shell) {
            CompoundTag floor = new CompoundTag();
            floor.putString("Name", "bettacraft:interior_floor");
            palette.add(floor);
        }
        states.put("palette", palette);
        sec.put("block_states", states);
        ListTag sections = new ListTag();
        sections.add(sec);
        CompoundTag c = new CompoundTag();
        c.put("sections", sections);
        return c;
    }

    private static void write(RegionStorageInfo info, Path folder, int id, CompoundTag tag) throws java.io.IOException {
        ChunkPos o = origin(id);
        try (RegionFile f = new RegionFile(info, file(folder, id), folder, false); DataOutputStream out = f.getChunkDataOutputStream(o)) {
            NbtIo.write(tag, out);
        }
    }

    /** The switch off: a broken tank's region stays dead, nothing of it is deleted, its number is not handed out. */
    @GameTest(template = EMPTY, batch = "bettacraft_m53c_cleanup_off", timeoutTicks = 1200)
    public static void theSwitchOffKeepsRemovedInteriors(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        MinecraftServer server = level.getServer();
        ServerLevel in = interior(h);
        InteriorRegions regions = InteriorRegions.get(server);
        boolean was = Features.own(Feature.INTERIOR_CLEANUP);
        Features.set(Feature.INTERIOR_CLEANUP, false);
        InteriorCleanup.reset();
        try {
            TankBlockEntity c = InteriorGameTests.tank(h, 2, 1, 2, 1, 1, 1, 0);
            InteriorRegion r = InteriorManager.ensureRegion(level, c);
            int id = r.id();
            in.setBlock(r.toWorld(5, 0, 5), Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
            breakTank(h, c.getTank());
            h.assertTrue(regions.dying().contains(id), "dying");
            server.saveEverything(true, false, false);
            h.assertTrue(regions.dead().contains(id), "dead after the save");
            h.runAfterDelay(800, () -> {
                try {
                    in.save(null, true, false); // the interior's chunks to the disk (stored although empty: the switch is off)
                    h.assertTrue(regions.dead().contains(id) && !regions.free().contains(id), "still dead with the switch off: " + id);
                    h.assertTrue(!InteriorCleanup.storedChunks(server, id, "region").isEmpty(), "its chunks are still on the disk");
                    h.succeed();
                } finally {
                    Features.set(Feature.INTERIOR_CLEANUP, was);
                }
            });
        } catch (RuntimeException e) {
            Features.set(Feature.INTERIOR_CLEANUP, was);
            throw e;
        }
    }
}
