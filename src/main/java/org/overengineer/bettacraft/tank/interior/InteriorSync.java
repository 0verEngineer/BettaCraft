package org.overengineer.bettacraft.tank.interior;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.neoforge.network.PacketDistributor;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.network.InteriorDataPayload;
import org.overengineer.bettacraft.network.InteriorRequestPayload;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server side of the miniature sync: encodes regions (cached per version), answers client requests and pushes updates
 * to players near the tank after interior blocks changed. M0 resends the whole region on change (delta updates: M3).
 */
public final class InteriorSync {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/interior-sync");

    private record Encoded(int version, List<byte[]> parts, InteriorSnapshot.Stats stats, boolean truncated) {}

    private static final Int2ObjectOpenHashMap<Encoded> CACHE = new Int2ObjectOpenHashMap<>();
    private static final it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap VERSIONS = new it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap();
    static { VERSIONS.defaultReturnValue(1); }
    private static final IntLinkedOpenHashSet DIRTY = new IntLinkedOpenHashSet();

    private InteriorSync() {}

    public static void markDirty(int regionId) {
        if (regionId >= 0) DIRTY.add(regionId);
    }

    public static void forget(int regionId) {
        CACHE.remove(regionId);
        DIRTY.remove(regionId);
    }

    public static void clearAll() {
        CACHE.clear();
        VERSIONS.clear();
        DIRTY.clear();
    }

    /** Called every few server ticks: re-encode changed regions and push them to nearby players. */
    public static void flush(MinecraftServer server) {
        if (DIRTY.isEmpty()) return;
        int[] ids = DIRTY.toIntArray();
        DIRTY.clear();
        for (int id : ids) {
            CACHE.remove(id);
            VERSIONS.addTo(id, 1);
            InteriorRegion r = InteriorRegions.get(server).get(id);
            if (r == null) continue;
            ServerLevel tankLevel = server.getLevel(r.tankDimension());
            if (tankLevel == null) continue;
            double range = ServerConfig.getOr(ServerConfig.INTERIOR_SYNC_RANGE, 96);
            for (ServerPlayer p : tankLevel.players()) {
                if (p.blockPosition().distSqr(r.tankMin()) <= range * range) send(server, r, p);
            }
        }
    }

    public static void handleRequest(ServerPlayer player, InteriorRequestPayload req) {
        MinecraftServer server = player.server;
        InteriorRegion r = InteriorRegions.get(server).get(req.regionId());
        if (r == null || player.level().dimension() != r.tankDimension()) return;
        double range = ServerConfig.getOr(ServerConfig.INTERIOR_SYNC_RANGE, 96) + 32;
        if (player.blockPosition().distSqr(req.controller()) > range * range) return;
        TankBlockEntity c = TankMultiblock.getController(player.level(), req.controller());
        if (c == null || c.getTank().regionId() != r.id()) return;
        if (req.knownVersion() == version(r.id()) && CACHE.containsKey(r.id())) return;
        send(server, r, player);
    }

    public static int version(int regionId) {
        return VERSIONS.get(regionId);
    }

    private static void send(MinecraftServer server, InteriorRegion r, ServerPlayer player) {
        Encoded e = CACHE.get(r.id());
        if (e == null) {
            ServerLevel interior = InteriorManager.interiorLevel(server);
            if (interior == null) return;
            int limit = ServerConfig.getOr(ServerConfig.INTERIOR_MAX_RENDERED_BLOCKS, 262144);
            InteriorSnapshot snap = new InteriorSnapshot();
            boolean truncated = encodeRegion(interior, r, snap, limit);
            e = new Encoded(version(r.id()), snap.toParts(InteriorDataPayload.MAX_PART_BYTES), snap.stats(), truncated);
            CACHE.put(r.id(), e);
            LOGGER.info("Encoded interior region {} ({}x{}x{}): {} non-air blocks in {} sections -> {} bytes in {} part(s), {} ms{}",
                    r.id(), r.sizeX(), r.sizeY(), r.sizeZ(), e.stats.nonAir(), e.stats.sections(), e.stats.bytes(), e.parts.size(),
                    e.stats.encodeNanos() / 1_000_000.0, truncated ? " (TRUNCATED by maxRenderedBlocks)" : "");
        }
        for (int i = 0; i < e.parts.size(); i++) {
            PacketDistributor.sendToPlayer(player, new InteriorDataPayload(r.id(), e.version, i, e.parts.size(),
                    r.sizeX(), r.sizeY(), r.sizeZ(), r.scale(), e.truncated, e.parts.get(i)));
        }
    }

    /** Encodes all interior blocks of a region (shell excluded). Returns true if the block limit cut it off. */
    public static boolean encodeRegion(ServerLevel level, InteriorRegion r, InteriorSnapshot snap, int limit) {
        int x0 = r.originX(), y0 = InteriorRegion.BASE_Y, z0 = r.originZ();
        int x1 = x0 + r.sizeX() - 1, y1 = y0 + r.sizeY() - 1, z1 = z0 + r.sizeZ() - 1;
        BlockState[] buf = new BlockState[InteriorSnapshot.SECTION_VOLUME];
        for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
            for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                for (int sy = y0 >> 4; sy <= y1 >> 4; sy++) {
                    LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sy));
                    if (section.hasOnlyAir()) continue;
                    java.util.Arrays.fill(buf, null);
                    boolean any = false;
                    for (int y = 0; y < 16; y++) {
                        int wy = (sy << 4) + y;
                        if (wy < y0 || wy > y1) continue;
                        for (int z = 0; z < 16; z++) {
                            int wz = (cz << 4) + z;
                            if (wz < z0 || wz > z1) continue;
                            for (int x = 0; x < 16; x++) {
                                int wx = (cx << 4) + x;
                                if (wx < x0 || wx > x1) continue;
                                BlockState s = section.getBlockState(x, y, z);
                                if (s.isAir() || InteriorManager.isShellBlock(s)) continue;
                                buf[InteriorSnapshot.index(x, y, z)] = s;
                                any = true;
                            }
                        }
                    }
                    if (any) {
                        snap.addSection(cx - (x0 >> 4), sy, cz - (z0 >> 4), buf);
                        if (snap.nonAir() > limit) return true;
                    }
                }
            }
        }
        return false;
    }

    /** Region containing a changed block of the interior dimension gets resynced. */
    public static void onInteriorBlockChanged(MinecraftServer server, BlockPos pos) {
        int id = InteriorRegion.idAt(pos.getX(), pos.getZ());
        InteriorRegion r = InteriorRegions.get(server).get(id);
        if (r != null && r.containsInterior(pos)) markDirty(id);
    }
}
