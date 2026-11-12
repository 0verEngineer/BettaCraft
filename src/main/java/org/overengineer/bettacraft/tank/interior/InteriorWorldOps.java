package org.overengineer.bettacraft.tank.interior;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Bulk block operations for interior regions that write <b>directly into chunk sections</b> (no neighbour updates,
 * no per-block packets). This keeps building a 256x256x256 region shell (~400k blocks) in the low hundreds of ms.
 * Light: walls are fully light-transparent and the floor is at the very bottom, so skipping light updates for the
 * shell is safe; copied decoration gets explicit light checks. Clients that already have affected chunks are resent.
 */
public final class InteriorWorldOps {
    private InteriorWorldOps() {}

    @FunctionalInterface
    public interface BlockVisitor {
        void visit(int x, int y, int z, BlockState state);
    }

    /** Fills the inclusive box with {@code state}. */
    public static void fill(ServerLevel level, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state, LongSet touchedChunks) {
        y0 = Math.max(y0, level.getMinBuildHeight());
        y1 = Math.min(y1, level.getMaxBuildHeight() - 1);
        for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
            for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                int ax = Math.max(x0, cx << 4), bx = Math.min(x1, (cx << 4) + 15);
                int az = Math.max(z0, cz << 4), bz = Math.min(z1, (cz << 4) + 15);
                for (int y = y0; y <= y1; y++) {
                    LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
                    for (int x = ax; x <= bx; x++) {
                        for (int z = az; z <= bz; z++) {
                            section.setBlockState(x & 15, y & 15, z & 15, state, false);
                        }
                    }
                }
                chunk.setUnsaved(true);
                touchedChunks.add(ChunkPos.asLong(cx, cz));
            }
        }
    }

    /** Visits every non-air block in the inclusive box, skipping empty sections quickly. */
    public static void forEachBlock(ServerLevel level, int x0, int y0, int z0, int x1, int y1, int z1, BlockVisitor visitor) {
        y0 = Math.max(y0, level.getMinBuildHeight());
        y1 = Math.min(y1, level.getMaxBuildHeight() - 1);
        for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
            for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                int ax = Math.max(x0, cx << 4), bx = Math.min(x1, (cx << 4) + 15);
                int az = Math.max(z0, cz << 4), bz = Math.min(z1, (cz << 4) + 15);
                for (int sy = y0 >> 4; sy <= y1 >> 4; sy++) {
                    LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sy));
                    if (section.hasOnlyAir()) continue;
                    int ay = Math.max(y0, sy << 4), by = Math.min(y1, (sy << 4) + 15);
                    for (int y = ay; y <= by; y++) {
                        for (int z = az; z <= bz; z++) {
                            for (int x = ax; x <= bx; x++) {
                                BlockState s = section.getBlockState(x & 15, y & 15, z & 15);
                                if (!s.isAir()) visitor.visit(x, y, z, s);
                            }
                        }
                    }
                }
            }
        }
    }

    /** A copied block (relative to the source box min) with optional block entity data. */
    public record Captured(int dx, int dy, int dz, BlockState state, @Nullable CompoundTag blockEntity) {}

    /** Captures all non-air, non-shell blocks of the inclusive box (positions relative to x0/y0/z0). */
    public static List<Captured> capture(ServerLevel level, int x0, int y0, int z0, int x1, int y1, int z1) {
        List<Captured> out = new ArrayList<>();
        forEachBlock(level, x0, y0, z0, x1, y1, z1, (x, y, z, s) -> {
            if (InteriorManager.isShellBlock(s)) return;
            CompoundTag be = null;
            if (s.hasBlockEntity()) {
                BlockEntity e = level.getBlockEntity(new BlockPos(x, y, z));
                if (e != null) be = e.saveWithFullMetadata(level.registryAccess());
            }
            out.add(new Captured(x - x0, y - y0, z - z0, s, be));
        });
        return out;
    }

    /** Writes captured blocks at {@code base} + relative position. */
    public static void paste(ServerLevel level, BlockPos base, List<Captured> blocks, LongSet touchedChunks) {
        for (Captured c : blocks) {
            BlockPos p = base.offset(c.dx(), c.dy(), c.dz());
            LevelChunk chunk = level.getChunk(p.getX() >> 4, p.getZ() >> 4);
            LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(p.getY()));
            section.setBlockState(p.getX() & 15, p.getY() & 15, p.getZ() & 15, c.state(), false);
            if (c.blockEntity() != null) {
                BlockEntity be = BlockEntity.loadStatic(p, c.state(), c.blockEntity(), level.registryAccess());
                if (be != null) chunk.setBlockEntity(be);
            }
            chunk.setUnsaved(true);
            touchedChunks.add(ChunkPos.asLong(p.getX() >> 4, p.getZ() >> 4));
            if (c.state().getLightEmission(level, p) > 0) {
                level.getChunkSource().getLightEngine().checkBlock(p);
            }
        }
    }

    /** Removes all non-air blocks (and block entities) in the inclusive box. */
    public static void clear(ServerLevel level, int x0, int y0, int z0, int x1, int y1, int z1, LongSet touchedChunks) {
        List<BlockPos> lit = new ArrayList<>();
        List<BlockPos> withBe = new ArrayList<>();
        LongSet chunks = new LongOpenHashSet();
        forEachBlock(level, x0, y0, z0, x1, y1, z1, (x, y, z, s) -> {
            BlockPos p = new BlockPos(x, y, z);
            if (s.hasBlockEntity()) withBe.add(p);
            if (s.getLightEmission() > 0) lit.add(p);
            LevelChunk chunk = level.getChunk(x >> 4, z >> 4);
            chunk.getSection(chunk.getSectionIndex(y)).setBlockState(x & 15, y & 15, z & 15, Blocks.AIR.defaultBlockState(), false);
            chunks.add(ChunkPos.asLong(x >> 4, z >> 4));
        });
        for (BlockPos p : withBe) level.getChunkAt(p).removeBlockEntity(p);
        for (BlockPos p : lit) level.getChunkSource().getLightEngine().checkBlock(p);
        for (long c : chunks) level.getChunk(ChunkPos.getX(c), ChunkPos.getZ(c)).setUnsaved(true);
        touchedChunks.addAll(chunks);
    }

    /** Resends full chunk data to players that already have one of the touched chunks loaded. */
    public static void resendChunks(ServerLevel level, LongSet touchedChunks) {
        for (long c : touchedChunks) {
            ChunkPos cp = new ChunkPos(c);
            List<ServerPlayer> players = level.getChunkSource().chunkMap.getPlayers(cp, false);
            if (players.isEmpty()) continue;
            LevelChunk chunk = level.getChunk(cp.x, cp.z);
            ClientboundLevelChunkWithLightPacket packet =
                    new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null);
            for (ServerPlayer p : players) p.connection.send(packet);
        }
    }
}
