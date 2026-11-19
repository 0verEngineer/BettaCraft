package org.overengineer.bettacraft.tank.interior;

import org.overengineer.bettacraft.util.Perms;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.registry.ModAttachments;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModDimensions;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server logic of the tank interior dimension (PLAN 7): region allocation (lazy, on first entry), shell building
 * (asynchronously for players, see {@link InteriorPreparation}), entering/leaving with rights, and keeping interiors
 * consistent when tanks merge, split or break.
 */
public final class InteriorManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/interior");

    private InteriorManager() {}

    @Nullable
    public static ServerLevel interiorLevel(MinecraftServer server) {
        return server.getLevel(ModDimensions.TANK_INTERIOR);
    }

    public static boolean isInterior(net.minecraft.world.level.Level level) {
        return level.dimension() == ModDimensions.TANK_INTERIOR;
    }

    /**
     * M29.1: vanilla water blocks do not belong in the interior (its water is the tank's own model): a water block
     * set there becomes air. Plants, corals and decor keep their {@code waterlogged} state; outside nothing changes.
     */
    public static BlockState dryInterior(net.minecraft.world.level.Level level, BlockState state) {
        return state.is(net.minecraft.world.level.block.Blocks.WATER) && isInterior(level)
                ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : state;
    }

    public static boolean isShellBlock(BlockState s) {
        return s.is(ModBlocks.INTERIOR_WALL.get()) || s.is(ModBlocks.INTERIOR_FLOOR.get());
    }

    // ------------------------------------------------------------------ enter / leave

    /** Shift + right-click / menu button: enter the tank's interior (region is prepared first if needed). */
    public static void enter(ServerPlayer player, BlockPos clicked) {
        // M33 part b: interior off: nobody goes in (who is inside can still leave)
        if (!org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.INTERIOR)) {
            player.sendOverlayMessage(org.overengineer.bettacraft.config.FeatureGate.offMessage(org.overengineer.bettacraft.config.Feature.INTERIOR));
            return;
        }
        if (isInterior(player.level())) {
            player.sendOverlayMessage(Component.translatable("message.bettacraft.interior.nested"));
            return;
        }
        if (org.overengineer.bettacraft.tank.TankMoves.travelling(player.level(), clicked)) { // M32 part b
            player.sendOverlayMessage(Component.translatable("message.bettacraft.travel.travelling"));
            return;
        }
        TankBlockEntity controller = TankMultiblock.getController(player.level(), clicked);
        ServerLevel interior = interiorLevel(player.level().getServer());
        if (controller == null || interior == null) {
            player.sendOverlayMessage(Component.translatable(interior == null
                    ? "message.bettacraft.interior.missing_dimension" : "message.bettacraft.tank.not_formed"));
            return;
        }
        if (!TankInhabitants.canModify(player, controller.getTank())) {
            player.sendOverlayMessage(Component.translatable("message.bettacraft.tank.locked").withStyle(ChatFormatting.RED));
            return;
        }
        InteriorRegion region = regionFor((ServerLevel) player.level(), controller);
        if (region.built()) {
            teleportInto(player, region);
        } else {
            InteriorPreparation.prepare(player.level().getServer(), region, player);
        }
    }

    /** Teleports into a built region (centre of the floor) and remembers the return point. */
    static void teleportInto(ServerPlayer player, InteriorRegion region) {
        ServerLevel interior = interiorLevel(player.level().getServer());
        if (interior == null || isInterior(player.level())) return;
        ModAttachments.INTERIOR_RETURN.set(player, new InteriorReturn(player.level().dimension(), player.position(),
                player.getYRot(), player.getXRot(), region.id()));
        BlockPos target = entryPoint(region);
        while (target.getY() < InteriorRegion.BASE_Y + region.sizeY() - 2 && !interior.getBlockState(target).isAir()) target = target.above();
        player.teleportTo(interior, target.getX() + 0.5, target.getY(), target.getZ() + 0.5, java.util.Set.of(), player.getYRot(), player.getXRot(), false);
        player.sendOverlayMessage(Component.translatable("message.bettacraft.interior.entered",
                region.sizeX(), region.sizeY(), region.sizeZ()));
        org.overengineer.bettacraft.progression.Progress.award(player, org.overengineer.bettacraft.progression.Progress.ENTER_INTERIOR);
    }

    /** Middle of the floor; for a free shape (M10) the middle of the bottom cell nearest to the centre. */
    static BlockPos entryPoint(InteriorRegion region) {
        if (region.fullBox()) return region.toWorld(region.sizeX() / 2, 0, region.sizeZ() / 2);
        int s = region.scale();
        double mx = region.cellsX() / 2.0, mz = region.cellsZ() / 2.0;
        int bx = -1, by = 0, bz = -1;
        double best = Double.MAX_VALUE;
        for (int y = 0; y < region.cellsY() && bx < 0; y++) {
            for (int z = 0; z < region.cellsZ(); z++)
                for (int x = 0; x < region.cellsX(); x++) {
                    if (!region.hasCell(x, y, z)) continue;
                    double d = (x + 0.5 - mx) * (x + 0.5 - mx) + (z + 0.5 - mz) * (z + 0.5 - mz);
                    if (d < best) {
                        best = d;
                        bx = x;
                        by = y;
                        bz = z;
                    }
                }
        }
        if (bx < 0) return region.toWorld(region.sizeX() / 2, 0, region.sizeZ() / 2);
        return region.toWorld(bx * s + s / 2, by * s, bz * s + s / 2);
    }

    public static void leave(ServerPlayer player) {
        InteriorReturn ret = ModAttachments.INTERIOR_RETURN.get(player);
        ModAttachments.INTERIOR_RETURN.remove(player);
        ServerLevel target = ret.isSet() ? player.level().getServer().getLevel(ret.dimension()) : null;
        if (target == null || isInterior(target)) {
            // No valid return point: world spawn of the overworld.
            ServerLevel overworld = player.level().getServer().overworld();
            BlockPos spawn = overworld.getRespawnData().pos();
            player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, java.util.Set.of(), player.getYRot(), player.getXRot(), false);
            return;
        }
        Vec3 p = ret.position();
        player.teleportTo(target, p.x, p.y, p.z, java.util.Set.of(), ret.yRot(), ret.xRot(), false);
    }

    /** Whether the player may change blocks inside the region at {@code pos} (tank lock, PLAN 7.1). */
    public static boolean canBuild(Player player, BlockPos pos) {
        if (Perms.has(player, 2)) return true;
        InteriorRegion r = player.level().getServer() == null ? null : regionAt(player.level().getServer(), pos);
        if (r == null) return false;
        ServerLevel tankLevel = player.level().getServer().getLevel(r.tankDimension());
        if (tankLevel != null && tankLevel.isLoaded(r.tankMin())) {
            TankBlockEntity c = TankMultiblock.getController(tankLevel, r.tankMin());
            if (c != null && c.getTank().regionId() == r.id()) return TankInhabitants.canModify(player, c.getTank());
        }
        return r.owner() == null || r.owner().equals(player.getUUID());
    }

    // ------------------------------------------------------------------ regions

    /** The tank's region, allocated if needed but not necessarily built. */
    public static InteriorRegion regionFor(ServerLevel tankLevel, TankBlockEntity controller) {
        TankData t = controller.getTank();
        InteriorRegions regions = InteriorRegions.get(tankLevel.getServer());
        InteriorRegion r = regions.get(t.regionId());
        if (r == null) {
            r = allocate(tankLevel, t);
            controller.markTankChanged();
        } else {
            r.retarget(tankLevel.dimension(), t.anchor(), t.id());
            regions.setDirty();
        }
        return r;
    }

    /** Returns the tank's region, allocating and building it synchronously on first use (tests, commands). */
    public static InteriorRegion ensureRegion(ServerLevel tankLevel, TankBlockEntity controller) {
        InteriorRegion r = regionFor(tankLevel, controller);
        if (!r.built()) buildShell(tankLevel.getServer(), r);
        return r;
    }

    private static InteriorRegion allocate(ServerLevel tankLevel, TankData t) {
        int s = t.scale();
        byte[] cells = org.overengineer.bettacraft.tank.TankShape.encode(t.members(), t.min().getX(), t.min().getY(), t.min().getZ(),
                t.sizeX(), t.sizeY(), t.sizeZ());
        InteriorRegions regions = InteriorRegions.get(tankLevel.getServer());
        InteriorRegion r;
        for (int skipped = 0; ; skipped++) {
            r = regions.allocate(t.sizeX() * s, t.sizeY() * s, t.sizeZ() * s, s, tankLevel.dimension(), t.anchor(), t.id(), t.owner(), cells);
            if (skipped >= MAX_SKIPPED_REGIONS || !usedBefore(tankLevel.getServer(), r)) break;
            // M42 part b: the region list (saved data, written at autosave and shutdown) is older than the interior chunks (written
            // whenever they unload), e.g. after a crash: this id was handed out before and its interior is still there. A new tank
            // must not take over that old interior (its blocks, plants, chests); the id stays used.
            LOGGER.warn("Interior region {} was used before (its chunks are still in the interior dimension; the region list is older "
                    + "than the world, e.g. after a crash): skipping it", r.id());
            regions.remove(r.id());
        }
        t.setRegionId(r.id());
        return r;
    }

    /** Ids tried at most for one new region before the next one is taken anyway (a whole row of old interiors). */
    private static final int MAX_SKIPPED_REGIONS = 4096;

    /**
     * M42 part b: whether the interior of this freshly allocated region already exists: its first floor block is there (its chunk
     * loaded), or its chunk was saved before. Regions are 2048 blocks apart, so the chunk of a region's corner is never made for
     * another region.
     */
    static boolean usedBefore(MinecraftServer server, InteriorRegion r) {
        ServerLevel in = interiorLevel(server);
        if (in == null) return false;
        net.minecraft.world.level.ChunkPos cp = new net.minecraft.world.level.ChunkPos(r.originX() >> 4, r.originZ() >> 4);
        net.minecraft.world.level.chunk.LevelChunk now = in.getChunkSource().getChunkNow(cp.x(), cp.z());
        if (now != null) return !now.getBlockState(new BlockPos(r.originX(), InteriorRegion.FLOOR_Y, r.originZ())).isAir();
        try {
            return in.getChunkSource().chunkMap.read(cp).join().isPresent();
        } catch (RuntimeException e) {
            LOGGER.debug("Could not read the interior chunk {} of region {}", cp, r.id(), e);
            return false;
        }
    }

    /** Chunk x range [0] .. [1] and z range [2] .. [3] (inclusive) touched by the region's shell. */
    static int[] shellChunks(InteriorRegion r) {
        return new int[]{(r.originX() - 1) >> 4, (r.originX() + r.sizeX()) >> 4, (r.originZ() - 1) >> 4, (r.originZ() + r.sizeZ()) >> 4};
    }

    /** Places floor, invisible walls and ceiling of a region synchronously (loads/generates the chunks on the spot). */
    public static void buildShell(MinecraftServer server, InteriorRegion r) {
        ServerLevel level = interiorLevel(server);
        if (level == null) return;
        long start = System.nanoTime();
        LongSet touched = new LongOpenHashSet();
        int[] c = shellChunks(r);
        for (int cx = c[0]; cx <= c[1]; cx++)
            for (int cz = c[2]; cz <= c[3]; cz++) buildShellChunk(level, r, cx, cz, touched);
        finishShell(server, level, r, touched, (System.nanoTime() - start) / 1_000_000.0, "sync");
    }

    static void finishShell(MinecraftServer server, ServerLevel level, InteriorRegion r, LongSet touched, double ms, String mode) {
        InteriorWorldOps.resendChunks(level, touched);
        r.setBuilt(true);
        r.equipment().markNeedsScan(); // M25: count the floor of the new shell (water chemistry)
        InteriorRegions.get(server).setDirty();
        LOGGER.debug("Built interior region {} ({}x{}x{}, scale {}) {} in {} ms main thread, {} chunks", r.id(), r.sizeX(), r.sizeY(),
                r.sizeZ(), r.scale(), mode, String.format(java.util.Locale.ROOT, "%.1f", ms), touched.size());
    }

    /** The part of the shell (floor, 4 walls, ceiling) inside one chunk column. */
    static void buildShellChunk(ServerLevel level, InteriorRegion r, int cx, int cz, LongSet touched) {
        int x0 = r.originX(), z0 = r.originZ();
        int x1 = x0 + r.sizeX() - 1, z1 = z0 + r.sizeZ() - 1;
        int fy = InteriorRegion.FLOOR_Y, top = InteriorRegion.BASE_Y + r.sizeY();
        BlockState wall = ModBlocks.INTERIOR_WALL.get().defaultBlockState();
        BlockState floor = ModBlocks.INTERIOR_FLOOR.get().defaultBlockState();
        int ax = cx << 4, bx = ax + 15, az = cz << 4, bz = az + 15;
        int[][] boxes = {
                {x0, fy, z0, x1, fy, z1},
                {x0 - 1, fy, z0 - 1, x0 - 1, top, z1 + 1},
                {x1 + 1, fy, z0 - 1, x1 + 1, top, z1 + 1},
                {x0, fy, z0 - 1, x1, top, z0 - 1},
                {x0, fy, z1 + 1, x1, top, z1 + 1},
                {x0, top, z0, x1, top, z1}};
        for (int i = 0; i < boxes.length; i++) {
            int[] b = boxes[i];
            int ix0 = Math.max(b[0], ax), ix1 = Math.min(b[3], bx), iz0 = Math.max(b[2], az), iz1 = Math.min(b[5], bz);
            if (ix0 > ix1 || iz0 > iz1) continue;
            InteriorWorldOps.fill(level, ix0, b[1], iz0, ix1, b[4], iz1, i == 0 ? floor : wall, touched);
        }
        if (!r.fullBox()) buildShapeWalls(level, r, ax, bx, az, bz, wall, touched);
    }

    /**
     * M10: closes off the cells of the bounding box that are not part of the tank's shape. Each such cell gets a wall
     * layer (one block thick, inside the cell) on every face towards a cell of the shape, and wall instead of floor
     * below it; the rest of it stays air that nobody can reach. Only the part inside the chunk column [ax..bx] x [az..bz].
     */
    static void buildShapeWalls(ServerLevel level, InteriorRegion r, int ax, int bx, int az, int bz, BlockState wall, LongSet touched) {
        int s = r.scale(), x0 = r.originX(), z0 = r.originZ();
        int cxa = Math.max(0, Math.floorDiv(ax - x0, s)), cxb = Math.min(r.cellsX() - 1, Math.floorDiv(bx - x0, s));
        int cza = Math.max(0, Math.floorDiv(az - z0, s)), czb = Math.min(r.cellsZ() - 1, Math.floorDiv(bz - z0, s));
        for (int cy = 0; cy < r.cellsY(); cy++)
            for (int cz = cza; cz <= czb; cz++)
                for (int cx = cxa; cx <= cxb; cx++) {
                    if (r.hasCell(cx, cy, cz)) continue;
                    int lx = cx * s, ly = cy * s, lz = cz * s;
                    if (cy == 0) fillClipped(level, r, lx, -1, lz, lx + s - 1, -1, lz + s - 1, ax, bx, az, bz, wall, touched);
                    for (Direction d : Direction.values()) {
                        if (!r.hasCell(cx + d.getStepX(), cy + d.getStepY(), cz + d.getStepZ())) continue;
                        int a0 = lx, a1 = lx + s - 1, b0 = ly, b1 = ly + s - 1, c0 = lz, c1 = lz + s - 1;
                        switch (d) {
                            case WEST -> a1 = a0;
                            case EAST -> a0 = a1;
                            case DOWN -> b1 = b0;
                            case UP -> b0 = b1;
                            case NORTH -> c1 = c0;
                            case SOUTH -> c0 = c1;
                        }
                        fillClipped(level, r, a0, b0, c0, a1, b1, c1, ax, bx, az, bz, wall, touched);
                    }
                }
    }

    /** Fills an interior-local box (y = -1 is the floor layer), clipped to the chunk column. */
    private static void fillClipped(ServerLevel level, InteriorRegion r, int lx0, int ly0, int lz0, int lx1, int ly1, int lz1,
                                    int ax, int bx, int az, int bz, BlockState state, LongSet touched) {
        int wx0 = Math.max(r.originX() + lx0, ax), wx1 = Math.min(r.originX() + lx1, bx);
        int wz0 = Math.max(r.originZ() + lz0, az), wz1 = Math.min(r.originZ() + lz1, bz);
        if (wx0 > wx1 || wz0 > wz1) return;
        InteriorWorldOps.fill(level, wx0, InteriorRegion.BASE_Y + ly0, wz0, wx1, InteriorRegion.BASE_Y + ly1, wz1, state, touched);
    }

    /** Removes everything of a region (shell included) and forgets it. */
    private static void destroyRegion(MinecraftServer server, InteriorRegion r) {
        ServerLevel level = interiorLevel(server);
        if (level != null && r.built()) {
            LongSet touched = new LongOpenHashSet();
            InteriorWorldOps.clear(level, r.originX() - 1, InteriorRegion.FLOOR_Y, r.originZ() - 1,
                    r.originX() + r.sizeX(), InteriorRegion.BASE_Y + r.sizeY(), r.originZ() + r.sizeZ(), touched);
            InteriorWorldOps.resendChunks(level, touched);
        }
        InteriorRegions.get(server).remove(r.id());
        InteriorSync.forget(r.id());
        InteriorPreparation.cancel(r.id());
    }

    /** Benchmark helper: removes a region that belongs to no tank. */
    static void destroyRegionForBench(MinecraftServer server, InteriorRegion r) {
        destroyRegion(server, r);
    }

    // ------------------------------------------------------------------ structural changes

    public static void onTanksMerged(ServerLevel tankLevel, List<TankData> olds, TankData merged) {
        MinecraftServer server = tankLevel.getServer();
        InteriorRegions regions = InteriorRegions.get(server);
        List<TankData> withRegion = olds.stream().filter(t -> regions.get(t.regionId()) != null && regions.get(t.regionId()).built()).toList();
        for (TankData t : olds) {
            InteriorRegion unbuilt = regions.get(t.regionId());
            if (unbuilt != null && !unbuilt.built()) destroyRegion(server, unbuilt);
        }
        merged.setRegionId(-1);
        if (withRegion.isEmpty()) return;
        ServerLevel interior = interiorLevel(server);
        if (interior == null) return;

        InteriorRegion target = allocate(tankLevel, merged);
        buildShell(server, target);
        int s = merged.scale();
        LongSet touched = new LongOpenHashSet();
        List<ItemStack> lostFloor = new ArrayList<>();
        for (TankData old : withRegion) {
            InteriorRegion src = regions.get(old.regionId());
            BlockPos off = old.min().subtract(merged.min());
            int dx = off.getX() * s, dy = off.getY() * s, dz = off.getZ() * s;
            if (src.scale() != s) {
                // M30: the tank grew past bigTankRules.interiorScale8Above: its interior is shrunk into the coarser one of the
                // big tank, what does not stay comes out as items (InteriorRescale)
                ejectPlayers(interior, src);
                InteriorRescale.shrinkInto(interior, src, target, dx, dy, dz, dy == 0, lostFloor, touched);
                destroyRegion(server, src);
                continue;
            }
            // the old region only has blocks in its own cells (the rest is walled off), so the whole box can be copied
            copyInterior(interior, src, 0, 0, 0, src.sizeX(), src.sizeY(), src.sizeZ(), target.toWorld(dx, dy, dz), touched);
            if (dy == 0) {
                copyFloor(interior, src, 0, 0, src.sizeX(), src.sizeZ(), target, dx, dz);
            } else {
                // M10: the old floor is no floor any more (the tank grew below it): paid substrate comes back as items
                collectFloor(interior, src, lostFloor);
            }
            movePlayers(interior, src, target, dx, dy, dz, null);
            // M13: the aquarium plants move along with their blocks
            for (var pl : src.plants().all()) target.plants().adopt(pl, pl.x + dx, pl.y + dy, pl.z + dz);
            destroyRegion(server, src);
        }
        if (!lostFloor.isEmpty()) InteriorDrops.drop(tankLevel, merged.anchor(), lostFloor);
        InteriorWorldOps.resendChunks(interior, touched);
        target.equipment().markNeedsScan();
        InteriorSync.markAllDirty(target.id());
    }

    public static void onTankSplit(ServerLevel tankLevel, TankData old, List<TankData> pieces) {
        MinecraftServer server = tankLevel.getServer();
        InteriorRegions regions = InteriorRegions.get(server);
        InteriorRegion src = regions.get(old.regionId());
        for (TankData p : pieces) p.setRegionId(-1);
        if (src == null) return;
        if (!src.built()) {
            destroyRegion(server, src);
            return;
        }
        ServerLevel interior = interiorLevel(server);
        if (interior == null) return;
        int s = old.scale();
        LongSet touched = new LongOpenHashSet();
        for (TankData piece : pieces) {
            InteriorRegion target = allocate(tankLevel, piece);
            buildShell(server, target);
            BlockPos off = piece.min().subtract(old.min());
            int lx = off.getX() * s, ly = off.getY() * s, lz = off.getZ() * s;
            if (piece.isCuboid()) {
                copyInterior(interior, src, lx, ly, lz, piece.sizeX() * s, piece.sizeY() * s, piece.sizeZ() * s, target.toWorld(0, 0, 0), touched);
                if (ly == 0) copyFloor(interior, src, lx, lz, piece.sizeX() * s, piece.sizeZ() * s, target, 0, 0);
            } else {
                // M10: the bounding boxes of the parts of a free shape can overlap, so copy cell by cell
                for (long m : piece.members()) {
                    BlockPos c = BlockPos.of(m);
                    BlockPos co = c.subtract(old.min()), cp = c.subtract(piece.min());
                    copyInterior(interior, src, co.getX() * s, co.getY() * s, co.getZ() * s, s, s, s,
                            target.toWorld(cp.getX() * s, cp.getY() * s, cp.getZ() * s), touched);
                    if (co.getY() == 0 && cp.getY() == 0) copyFloor(interior, src, co.getX() * s, co.getZ() * s, s, s, target, cp.getX() * s, cp.getZ() * s);
                }
            }
            movePlayers(interior, src, target, -lx, -ly, -lz, piece);
            // M13: aquarium plants go with the piece their root lies in (cell of the old tank -> cell of the piece)
            for (var pl : src.plants().all()) {
                BlockPos cell = old.min().offset(Math.floorDiv(pl.x, s), Math.floorDiv(pl.y, s), Math.floorDiv(pl.z, s));
                if (!piece.members().contains(cell.asLong())) continue;
                BlockPos d = old.min().subtract(piece.min());
                target.plants().adopt(pl, pl.x + d.getX() * s, pl.y + d.getY() * s, pl.z + d.getZ() * s);
            }
            target.equipment().markNeedsScan();
            InteriorSync.markAllDirty(target.id());
        }
        InteriorWorldOps.resendChunks(interior, touched);
        ejectPlayers(interior, src);
        destroyRegion(server, src);
    }

    /**
     * M32 part b: a tank came back turned from a move (Create contraption, Sable sub-level): its interior is copied block
     * by block into a new region of the turned shape, the blocks turned with it (about the vertical axis), the floor
     * substrate and the aquarium plants too. A tank lying on its side afterwards keeps its blocks (unturned) but its old
     * floor comes out as items (the new bottom gets a plain floor). {@code old} has the coordinates before the move,
     * {@code moved} after {@link TankData#relocate}; the turtle eggs of {@code moved} get their new egg blocks.
     */
    public static void onTankTurned(ServerLevel tankLevel, TankData old, TankData moved, org.overengineer.bettacraft.tank.TankMoveMap map) {
        MinecraftServer server = tankLevel.getServer();
        InteriorRegions regions = InteriorRegions.get(server);
        InteriorRegion src = regions.get(old.regionId());
        moved.setRegionId(-1);
        if (src == null) return;
        if (!src.built()) {
            destroyRegion(server, src);
            return;
        }
        ServerLevel interior = interiorLevel(server);
        if (interior == null) return;
        int s = old.scale();
        boolean upright = map.upright();
        net.minecraft.world.level.block.Rotation rot = switch (upright ? map.quarterTurns() : 0) {
            case 1 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_90;
            case 2 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_180;
            case 3 -> net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90;
            default -> net.minecraft.world.level.block.Rotation.NONE;
        };
        InteriorRegion target = allocate(tankLevel, moved);
        buildShell(server, target);
        LongSet touched = new LongOpenHashSet();
        BlockPos a = src.toWorld(0, 0, 0);
        List<InteriorWorldOps.Captured> blocks = InteriorWorldOps.capture(interior, a.getX(), a.getY(), a.getZ(),
                a.getX() + src.sizeX() - 1, a.getY() + src.sizeY() - 1, a.getZ() + src.sizeZ() - 1);
        List<InteriorWorldOps.Captured> turned = new ArrayList<>(blocks.size());
        for (InteriorWorldOps.Captured c : blocks) {
            int[] q = turnLocal(old, moved, s, map, c.dx(), c.dy(), c.dz());
            if (q[0] < 0 || q[1] < 0 || q[2] < 0 || q[0] >= target.sizeX() || q[1] >= target.sizeY() || q[2] >= target.sizeZ()) continue;
            turned.add(new InteriorWorldOps.Captured(q[0], q[1], q[2], c.state().rotate(rot), c.blockEntity()));
        }
        InteriorWorldOps.paste(interior, target.toWorld(0, 0, 0), turned, touched);
        List<ItemStack> lostFloor = new ArrayList<>();
        if (upright) {
            for (int x = 0; x < src.sizeX(); x++)
                for (int z = 0; z < src.sizeZ(); z++) {
                    InteriorFloorBlockEntity be = Substrate.active(interior, new BlockPos(src.originX() + x, InteriorRegion.FLOOR_Y, src.originZ() + z));
                    if (be == null) continue;
                    int[] q = turnLocal(old, moved, s, map, x, 0, z);
                    BlockPos to = new BlockPos(target.originX() + q[0], InteriorRegion.FLOOR_Y, target.originZ() + q[2]);
                    if (target.containsShell(to)) Substrate.set(interior, to, be.substrate(), be.paid());
                }
        } else {
            collectFloor(interior, src, lostFloor);
        }
        for (var pl : src.plants().all()) {
            int[] q = turnLocal(old, moved, s, map, pl.x, pl.y, pl.z);
            target.plants().adopt(pl, q[0], q[1], q[2]);
        }
        for (var c : moved.herpClutches()) {
            BlockPos e = BlockPos.of(c.interior);
            int[] q = turnLocal(old, moved, s, map, e.getX() - a.getX(), e.getY() - a.getY(), e.getZ() - a.getZ());
            c.interior = target.toWorld(q[0], q[1], q[2]).asLong();
        }
        if (!lostFloor.isEmpty()) InteriorDrops.drop(tankLevel, moved.anchor(), lostFloor);
        InteriorWorldOps.resendChunks(interior, touched);
        ejectPlayers(interior, src);
        destroyRegion(server, src);
        target.equipment().markNeedsScan();
        InteriorSync.markAllDirty(target.id());
    }

    /** Interior block (lx, ly, lz) of the old tank's region -> its block in the region of the moved tank. */
    private static int[] turnLocal(TankData old, TankData moved, int s, org.overengineer.bettacraft.tank.TankMoveMap map, int lx, int ly, int lz) {
        BlockPos om = old.min(), nm = moved.min();
        net.minecraft.world.phys.Vec3 w = map.point(om.getX() + (lx + 0.5) / s, om.getY() + (ly + 0.5) / s, om.getZ() + (lz + 0.5) / s);
        return new int[]{(int) Math.floor((w.x - nm.getX()) * s), (int) Math.floor((w.y - nm.getY()) * s), (int) Math.floor((w.z - nm.getZ()) * s)};
    }

    /**
     * Drops the interior part belonging to one tank block as items at that block and clears it (PLAN 4.1). The floor
     * substrate under the block (bottom layer only) drops too if it was paid for.
     */
    public static void dropInteriorOfBlock(ServerLevel tankLevel, TankData t, BlockPos pos) {
        MinecraftServer server = tankLevel.getServer();
        InteriorRegion r = InteriorRegions.get(server).get(t.regionId());
        ServerLevel interior = interiorLevel(server);
        if (r == null || interior == null || !r.built()) return;
        int s = t.scale();
        BlockPos off = pos.subtract(t.min());
        BlockPos a = r.toWorld(off.getX() * s, off.getY() * s, off.getZ() * s);
        BlockPos b = a.offset(s - 1, s - 1, s - 1);
        List<ItemStack> items = new ArrayList<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        InteriorWorldOps.forEachBlock(interior, a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ(),
                (x, y, z, st) -> InteriorDrops.collect(interior, m.set(x, y, z).immutable(), st, items));
        if (off.getY() == 0) {
            for (int x = a.getX(); x <= b.getX(); x++)
                for (int z = a.getZ(); z <= b.getZ(); z++) {
                    ItemStack back = Substrate.clear(interior, new BlockPos(x, InteriorRegion.FLOOR_Y, z));
                    if (!back.isEmpty()) items.add(back);
                }
        }
        // M13: aquarium plants rooted in this block come out as whole plants (with their stage)
        for (var pl : new ArrayList<>(r.plants().all())) {
            BlockPos root = r.toWorld(pl.x, pl.y, pl.z);
            if (root.getX() < a.getX() || root.getY() < a.getY() || root.getZ() < a.getZ() || root.getX() > b.getX() || root.getY() > b.getY()
                    || root.getZ() > b.getZ()) continue;
            r.plants().remove(pl);
            items.add(org.overengineer.bettacraft.plants.InteriorPlants.item(pl.species, pl.stage));
        }
        LongSet touched = new LongOpenHashSet();
        InteriorWorldOps.clear(interior, a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ(), touched);
        InteriorWorldOps.resendChunks(interior, touched);
        InteriorDrops.drop(tankLevel, pos, items);
        r.equipment().markNeedsScan();
        InteriorSync.markAllDirty(r.id());
    }

    public static void freeRegion(ServerLevel tankLevel, TankData t) {
        MinecraftServer server = tankLevel.getServer();
        InteriorRegion r = InteriorRegions.get(server).get(t.regionId());
        if (r == null) return;
        ServerLevel interior = interiorLevel(server);
        if (interior != null) ejectPlayers(interior, r);
        destroyRegion(server, r);
        t.setRegionId(-1);
    }

    private static void copyInterior(ServerLevel level, InteriorRegion src, int lx, int ly, int lz, int sx, int sy, int sz,
                                     BlockPos dst, LongSet touched) {
        BlockPos a = src.toWorld(lx, ly, lz);
        List<InteriorWorldOps.Captured> blocks = InteriorWorldOps.capture(level, a.getX(), a.getY(), a.getZ(),
                a.getX() + sx - 1, a.getY() + sy - 1, a.getZ() + sz - 1);
        InteriorWorldOps.paste(level, dst, blocks, touched);
    }

    /** Takes all floor substrate of a region out (paid substrate as items). */
    private static void collectFloor(ServerLevel level, InteriorRegion src, List<ItemStack> out) {
        for (int x = 0; x < src.sizeX(); x++)
            for (int z = 0; z < src.sizeZ(); z++) {
                BlockPos at = new BlockPos(src.originX() + x, InteriorRegion.FLOOR_Y, src.originZ() + z);
                if (!(level.getBlockEntity(at) instanceof InteriorFloorBlockEntity)) continue;
                ItemStack back = Substrate.clear(level, at);
                if (!back.isEmpty()) out.add(back);
            }
    }

    /** Copies the floor substrate of a source box (local x/z) to the target region at offset dx/dz. */
    private static void copyFloor(ServerLevel level, InteriorRegion src, int lx, int lz, int sx, int sz, InteriorRegion target, int dx, int dz) {
        for (int x = 0; x < sx; x++)
            for (int z = 0; z < sz; z++) {
                BlockPos from = new BlockPos(src.originX() + lx + x, InteriorRegion.FLOOR_Y, src.originZ() + lz + z);
                InteriorFloorBlockEntity be = Substrate.active(level, from); // M17: not a stale one
                if (be != null) {
                    BlockPos to = new BlockPos(target.originX() + dx + x, InteriorRegion.FLOOR_Y, target.originZ() + dz + z);
                    if (target.containsShell(to)) Substrate.set(level, to, be.substrate(), be.paid());
                }
            }
    }

    /**
     * Moves the players of {@code from} that stand in the part that becomes {@code to}; with {@code piece} (a split, M10)
     * only those whose tank block belongs to that piece.
     */
    private static void movePlayers(ServerLevel level, InteriorRegion from, InteriorRegion to, int dx, int dy, int dz, @Nullable TankData piece) {
        for (ServerPlayer p : new ArrayList<>(level.players())) {
            if (!from.containsShell(p.blockPosition())) continue;
            double lx = p.getX() - from.originX() + dx, ly = p.getY() - InteriorRegion.BASE_Y + dy, lz = p.getZ() - from.originZ() + dz;
            if (lx < 0 || lz < 0 || lx >= to.sizeX() || lz >= to.sizeZ() || ly < 0 || ly >= to.sizeY()) continue;
            if (piece != null && !to.inShape((int) Math.floor(lx), (int) Math.floor(Math.min(ly, to.sizeY() - 1)), (int) Math.floor(lz))) continue;
            p.teleportTo(level, to.originX() + lx, InteriorRegion.BASE_Y + ly, to.originZ() + lz, java.util.Set.of(), p.getYRot(), p.getXRot(), false);
            InteriorReturn ret = ModAttachments.INTERIOR_RETURN.get(p);
            if (ret.isSet()) {
                ModAttachments.INTERIOR_RETURN.set(p, new InteriorReturn(ret.dimension(), ret.position(), ret.yRot(), ret.xRot(), to.id()));
            }
        }
    }

    private static void ejectPlayers(ServerLevel level, InteriorRegion r) {
        for (ServerPlayer p : new ArrayList<>(level.players())) {
            if (r.containsShell(p.blockPosition())) {
                leave(p);
                p.sendOverlayMessage(Component.translatable("message.bettacraft.interior.invalid"));
            }
        }
    }

    /** M15: the players standing in the tank's interior right now (the tank must not be broken meanwhile). */
    public static List<ServerPlayer> playersInside(MinecraftServer server, TankData t) {
        ServerLevel in = interiorLevel(server);
        InteriorRegion r = t.regionId() < 0 ? null : InteriorRegions.get(server).get(t.regionId());
        if (in == null || r == null || !r.built()) return List.of();
        List<ServerPlayer> out = new ArrayList<>();
        // M17: a player who died inside (death screen) does not keep the tank locked
        for (ServerPlayer p : in.players()) if (!p.isSpectator() && p.isAlive() && r.containsShell(p.blockPosition())) out.add(p);
        return out;
    }

    /** M15: per region the players inside it, sent to the tank's clients (they keep the client from breaking it). */
    private static final it.unimi.dsi.fastutil.ints.Int2IntMap OCCUPANTS = new it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap();
    /** M17: holds the chunk of an occupied tank (renewed every 10 ticks while players are inside). */

    public static void updateOccupants(MinecraftServer server) {
        ServerLevel in = interiorLevel(server);
        it.unimi.dsi.fastutil.ints.Int2IntMap now = new it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap();
        if (in != null) {
            for (ServerPlayer p : in.players()) {
                if (p.isSpectator() || !p.isAlive()) continue;
                InteriorRegion r = regionAt(server, p.blockPosition());
                if (r != null) now.mergeInt(r.id(), 1, Integer::sum);
            }
        }
        it.unimi.dsi.fastutil.ints.IntSet ids = new it.unimi.dsi.fastutil.ints.IntOpenHashSet(now.keySet());
        ids.addAll(OCCUPANTS.keySet());
        for (int id : ids) {
            int n = now.getOrDefault(id, 0);
            InteriorRegion r = InteriorRegions.get(server).get(id);
            ServerLevel tankLevel = r == null ? null : server.getLevel(r.tankDimension());
            // M17: keep the tank's chunk loaded (not ticking) while someone is inside: the lock and trust checks read
            // the tank itself; with its chunk unloaded only the owner could build and the occupant count went stale
            if (tankLevel != null && n > 0) {
                net.minecraft.world.level.ChunkPos cp = net.minecraft.world.level.ChunkPos.containing(r.tankMin());
                tankLevel.getChunkSource().addTicketWithRadius(org.overengineer.bettacraft.registry.ModTickets.OCCUPIED_TANK.get(), cp, 0);
            }
            if (tankLevel != null && tankLevel.isLoaded(r.tankMin())) {
                TankBlockEntity c = TankMultiblock.getController(tankLevel, r.tankMin());
                // M17: compared with the tank itself: a tank whose chunk was unloaded meanwhile comes back with 0
                if (c != null && c.getTank().regionId() == id && c.getTank().occupants() != n) {
                    c.getTank().setOccupants(n);
                    c.markTankChanged();
                }
            }
            if (n == 0) OCCUPANTS.remove(id);
            else OCCUPANTS.put(id, n);
        }
    }

    static void clearOccupants() {
        OCCUPANTS.clear();
    }

    /** Region a position of the interior dimension belongs to (shell included), or null. */
    @Nullable
    public static InteriorRegion regionAt(MinecraftServer server, BlockPos pos) {
        InteriorRegion r = InteriorRegions.get(server).get(InteriorRegion.idAt(pos.getX(), pos.getZ()));
        return r != null && r.containsShell(pos) ? r : null;
    }

    /** The region's equipment, rescanned first if a bulk change made it stale. */
    public static InteriorEquipment equipment(MinecraftServer server, InteriorRegion r) {
        // M38: other blocks have a foreign plant profile now (a data pack, a mod added or updated): count them again
        if (r.built() && r.equipment().foreignSignature() != org.overengineer.bettacraft.plants.foreign.ForeignPlants.signature()) r.equipment().markNeedsScan();
        if (r.equipment().needsScan()) {
            ServerLevel interior = interiorLevel(server);
            if (interior != null) {
                long t0 = System.nanoTime();
                r.equipment().rescan(interior, r);
                InteriorRegions.get(server).setDirty();
                LOGGER.debug("Rescanned equipment of region {} in {} ms", r.id(), (System.nanoTime() - t0) / 1e6);
            }
        }
        return r.equipment();
    }
}
