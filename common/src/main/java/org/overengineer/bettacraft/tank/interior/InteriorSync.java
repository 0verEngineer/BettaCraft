package org.overengineer.bettacraft.tank.interior;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.overengineer.bettacraft.platform.network.PacketDistributor;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.network.InteriorDataPayload;
import org.overengineer.bettacraft.network.InteriorDeltaPayload;
import org.overengineer.bettacraft.network.InteriorRequestPayload;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server side of the miniature sync (PLAN 7.4).
 * <ul>
 *     <li>Full sync: all non-air blocks of a region (palette sections), cached per version, sent on request.</li>
 *     <li>Delta sync (M3): every block change in the interior marks its 16^3 section dirty ({@link #onBlockUpdated},
 *     hooked into {@code ServerLevel#sendBlockUpdated}); every 5 ticks only the dirty sections are re-encoded and
 *     pushed to players near the tank as a delta from version n to n+1. Clients that miss a version request a full
 *     resync.</li>
 *     <li>The render limit ({@code interior.maxRenderedBlocks}) is enforced on the full sync (truncation); a delta that
 *     would exceed it falls back to a full sync.</li>
 * </ul>
 */
public final class InteriorSync {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/interior-sync");
    /** More dirty sections than this in one flush: send the region in full instead. */
    static final int MAX_DELTA_SECTIONS = 96;
    /** M17: ticks between two full sends a client may ask for (per player and region). */
    public static final int REQUEST_COOLDOWN = 40;

    private record Encoded(int version, List<byte[]> parts, InteriorSnapshot.Stats stats, boolean truncated) {}

    /** Per-region sync state. */
    private static final class State {
        int version = 1;
        boolean all;
        final LongOpenHashSet dirty = new LongOpenHashSet();
        /** Non-air blocks per section key (valid after the first full encode). */
        final Long2IntOpenHashMap counts = new Long2IntOpenHashMap();
        boolean countsValid;
        boolean truncated;
        Encoded cache;
        /** M17: game time of the last full send per player that asked for it (requests are rate-limited). */
        final it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap<java.util.UUID> lastRequested = new it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap<>();

        int total() {
            int t = 0;
            for (int v : counts.values()) t += v;
            return t;
        }
    }

    private static final Int2ObjectOpenHashMap<State> STATES = new Int2ObjectOpenHashMap<>();
    /** Bytes of the last delta / full sync sent (bench, menu). */
    public static volatile int lastDeltaBytes, lastFullBytes;
    /** Full sends to single players so far (M17 tests of the request limit). */
    public static volatile int fullSends;

    private InteriorSync() {}

    /** M53c: the last version of a forgotten region (its id is handed out again: the clients' old copy must not count). */
    private static final it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap LAST_VERSION = new it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap();

    private static State state(int regionId) {
        return STATES.computeIfAbsent(regionId, k -> {
            State st = new State();
            st.version = LAST_VERSION.getOrDefault(regionId, 0) + 1;
            return st;
        });
    }

    /** Whole region changed (merge, split, bulk writes, commands). */
    public static void markAllDirty(int regionId) {
        if (regionId >= 0) {
            state(regionId).all = true;
            InteriorObstacles.forget(regionId);
            InteriorGround.forget(regionId);
        }
    }

    /** Kept for callers of the M0 API: resends the whole region. */
    public static void markDirty(int regionId) {
        markAllDirty(regionId);
    }

    public static void forget(int regionId) {
        State old = STATES.remove(regionId);
        if (old != null) LAST_VERSION.put(regionId, Math.max(old.version, LAST_VERSION.getOrDefault(regionId, 0)));
        InteriorObstacles.forget(regionId);
        InteriorGround.forget(regionId);
    }

    public static void clearAll() {
        STATES.clear();
        LAST_VERSION.clear();
    }

    /** M17: a player logged out: forget their request times. */
    public static void forgetPlayer(java.util.UUID player) {
        for (State st : STATES.values()) st.lastRequested.removeLong(player);
    }

    public static int version(int regionId) {
        State s = STATES.get(regionId);
        return s == null ? 1 : s.version;
    }

    /** Non-air blocks synced for the region (-1 if not encoded yet) and whether the render limit cut it off. */
    public static int syncedBlocks(int regionId) {
        State s = STATES.get(regionId);
        return s == null || !s.countsValid ? -1 : s.total();
    }

    public static boolean truncated(int regionId) {
        State s = STATES.get(regionId);
        return s != null && s.truncated;
    }

    static long sectionKey(int sx, int sy, int sz) {
        return SectionPos.asLong(sx, sy, sz);
    }

    /** Hook from {@code ServerLevel#sendBlockUpdated}: a block of the interior dimension changed. */
    public static void onBlockUpdated(ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        if (!InteriorManager.isInterior(level)) return;
        int id = InteriorRegion.idAt(pos.getX(), pos.getZ());
        InteriorRegion r = InteriorRegions.get(level.getServer()).get(id);
        if (r == null || !r.built()) return;
        int sx = (pos.getX() >> 4) - (r.originX() >> 4), sz = (pos.getZ() >> 4) - (r.originZ() >> 4);
        if (r.containsInterior(pos)) {
            if (oldState == newState) return;
            state(id).dirty.add(sectionKey(sx, pos.getY() >> 4, sz));
            r.equipment().onChange(r.localX(pos.getX()), r.localY(pos.getY()), r.localZ(pos.getZ()), oldState, newState);
            InteriorObstacles.invalidate(id, r.localX(pos.getX()), r.localY(pos.getY()), r.localZ(pos.getZ()));
            InteriorGround.invalidate(id, r.localX(pos.getX()), r.localZ(pos.getZ())); // M14: where the small animals walk
            // M13: the root of an aquarium plant vanished (not by a player breaking it): forget the plant
            if (oldState.getBlock() instanceof org.overengineer.bettacraft.plants.AquaticPlantBlock
                    && oldState.getValue(org.overengineer.bettacraft.plants.AquaticPlantBlock.PART).ordinal() <= org.overengineer.bettacraft.plants.PlantPart.ROOT.ordinal()
                    && newState.getBlock() != oldState.getBlock()) {
                org.overengineer.bettacraft.plants.InteriorPlants.onRootGone(r, r.localX(pos.getX()), r.localY(pos.getY()), r.localZ(pos.getZ()));
            }
            InteriorRegions.get(level.getServer()).setDirty();
        } else if (pos.getY() == InteriorRegion.FLOOR_Y && r.localX(pos.getX()) >= 0 && r.localX(pos.getX()) < r.sizeX()
                && r.localZ(pos.getZ()) >= 0 && r.localZ(pos.getZ()) < r.sizeZ()) {
            state(id).dirty.add(sectionKey(sx, InteriorSnapshot.FLOOR_SY, sz));
            if (Substrate.isFloor(oldState) && Substrate.isFloor(newState)) {
                boolean was = oldState.getValue(InteriorFloorBlock.SUBSTRATE), is = newState.getValue(InteriorFloorBlock.SUBSTRATE);
                if (was != is) {
                    r.equipment().substrate += is ? 1 : -1;
                    InteriorRegions.get(level.getServer()).setDirty();
                }
            }
        }
    }

    /** Hook from {@code ServerLevel#sendBlockUpdated} with an unchanged state: block entity data changed (floor substrate). */
    public static void onBlockEntityUpdated(ServerLevel level, BlockPos pos) {
        if (!InteriorManager.isInterior(level) || pos.getY() != InteriorRegion.FLOOR_Y) return;
        int id = InteriorRegion.idAt(pos.getX(), pos.getZ());
        InteriorRegion r = InteriorRegions.get(level.getServer()).get(id);
        if (r == null || !r.built()) return;
        int lx = r.localX(pos.getX()), lz = r.localZ(pos.getZ());
        if (lx < 0 || lz < 0 || lx >= r.sizeX() || lz >= r.sizeZ()) return;
        state(id).dirty.add(sectionKey((pos.getX() >> 4) - (r.originX() >> 4), InteriorSnapshot.FLOOR_SY, (pos.getZ() >> 4) - (r.originZ() >> 4)));
    }

    /** Encodes the full region now (as a first client request would); used by tests and the bench. */
    public static int prepareFull(MinecraftServer server, int regionId) {
        InteriorRegion r = InteriorRegions.get(server).get(regionId);
        if (r == null) return -1;
        State st = state(regionId);
        if (st.all || !st.dirty.isEmpty()) full(st);
        send(server, r, null);
        return st.cache == null ? -1 : st.cache.stats().bytes();
    }

    /** Called every few server ticks: push changes to nearby players (delta if possible). */
    public static void flush(MinecraftServer server) {
        if (STATES.isEmpty()) return;
        ServerLevel interior = InteriorManager.interiorLevel(server);
        for (var e : STATES.int2ObjectEntrySet()) {
            State st = e.getValue();
            if (!st.all && st.dirty.isEmpty()) continue;
            InteriorRegion r = InteriorRegions.get(server).get(e.getIntKey());
            if (r == null || interior == null) {
                st.dirty.clear();
                st.all = false;
                continue;
            }
            List<ServerPlayer> viewers = viewers(server, r);
            if (st.all || !st.countsValid || st.dirty.size() > MAX_DELTA_SECTIONS) {
                full(st);
            } else if (!delta(interior, r, st, viewers)) {
                full(st);
            } else {
                continue;
            }
            for (ServerPlayer p : viewers) send(server, r, p);
        }
    }

    private static void full(State st) {
        st.version++;
        st.cache = null;
        st.all = false;
        st.dirty.clear();
    }

    /** Encodes and sends the dirty sections. False if a full sync is needed instead. */
    private static boolean delta(ServerLevel interior, InteriorRegion r, State st, List<ServerPlayer> viewers) {
        int limit = ServerConfig.getOr(ServerConfig.INTERIOR_MAX_RENDERED_BLOCKS, 262144);
        if (st.truncated) return false;
        InteriorSnapshot snap = new InteriorSnapshot();
        Long2IntOpenHashMap newCounts = new Long2IntOpenHashMap();
        BlockState[] buf = new BlockState[InteriorSnapshot.SECTION_VOLUME];
        for (long key : st.dirty) {
            int sx = SectionPos.x(key), sy = SectionPos.y(key), sz = SectionPos.z(key);
            int before = snap.nonAir();
            encodeSection(interior, r, sx, sy, sz, buf, snap, true);
            newCounts.put(key, snap.nonAir() - before);
        }
        int total = st.total();
        for (var c : newCounts.long2IntEntrySet()) total += c.getIntValue() - st.counts.get(c.getLongKey());
        if (total > limit) return false;
        byte[] data = snap.bytes();
        if (data.length > InteriorDataPayload.MAX_PART_BYTES) return false;
        for (var c : newCounts.long2IntEntrySet()) {
            if (c.getIntValue() == 0) st.counts.remove(c.getLongKey());
            else st.counts.put(c.getLongKey(), c.getIntValue());
        }
        int from = st.version;
        st.version++;
        st.cache = null;
        st.dirty.clear();
        lastDeltaBytes = data.length;
        InteriorDeltaPayload payload = new InteriorDeltaPayload(r.id(), from, st.version, data);
        for (ServerPlayer p : viewers) PacketDistributor.sendToPlayer(p, payload);
        if (org.overengineer.bettacraft.debug.LogArea.NETWORK.debug) org.overengineer.bettacraft.debug.LogArea.NETWORK.debug("Delta region {} v{}->v{}: {} sections, {} bytes, {} viewers", r.id(), from, st.version, newCounts.size(), data.length, viewers.size());
        return true;
    }

    private static List<ServerPlayer> viewers(MinecraftServer server, InteriorRegion r) {
        List<ServerPlayer> out = new ArrayList<>();
        ServerLevel tankLevel = server.getLevel(r.tankDimension());
        if (tankLevel == null) return out;
        double range = ServerConfig.getOr(ServerConfig.INTERIOR_SYNC_RANGE, 96);
        for (ServerPlayer p : tankLevel.players()) {
            // players without our channel (e.g. vanilla clients on a server that allows them, test players) get nothing
            if (p.blockPosition().distSqr(r.tankMin()) <= range * range && org.overengineer.bettacraft.platform.Platform.get().hasChannel(p, InteriorDataPayload.TYPE)) out.add(p);
        }
        return out;
    }

    public static void handleRequest(ServerPlayer player, InteriorRequestPayload req) {
        MinecraftServer server = player.level().getServer();
        InteriorRegion r = InteriorRegions.get(server).get(req.regionId());
        if (r == null || !r.built() || player.level().dimension() != r.tankDimension()) return;
        double range = ServerConfig.getOr(ServerConfig.INTERIOR_SYNC_RANGE, 96) + 32;
        // M32 part b: the miniature of a tank travelling on a contraption or an airship
        if (!org.overengineer.bettacraft.tank.TankMoves.travellingRegionNear(player, r.id(), req.controller(), range)) {
            if (player.blockPosition().distSqr(req.controller()) > range * range) return;
            // M17: never load a chunk for a client request (getBlockEntity loads it synchronously on the server)
            if (!player.level().isLoaded(req.controller())) return;
            TankBlockEntity c = TankMultiblock.getController(player.level(), req.controller());
            if (c == null || c.getTank().regionId() != r.id()) return;
        }
        State st = state(r.id());
        if (req.knownVersion() == st.version && !st.all && st.dirty.isEmpty()) return;
        // M17: one full send per player and region per REQUEST_COOLDOWN ticks; a client asking again and again (or with a
        // made-up version) cannot make the server send megabytes every tick. Changes still arrive through flush().
        long now = server.overworld().getGameTime();
        long last = st.lastRequested.getOrDefault(player.getUUID(), Long.MIN_VALUE);
        if (last != Long.MIN_VALUE && now - last >= 0 && now - last < REQUEST_COOLDOWN) return;
        st.lastRequested.put(player.getUUID(), now);
        send(server, r, player);
    }

    private static void send(MinecraftServer server, InteriorRegion r, @javax.annotation.Nullable ServerPlayer player) {
        State st = state(r.id());
        Encoded e = st.cache;
        if (e == null || e.version != st.version) {
            ServerLevel interior = InteriorManager.interiorLevel(server);
            if (interior == null) return;
            int limit = ServerConfig.getOr(ServerConfig.INTERIOR_MAX_RENDERED_BLOCKS, 262144);
            InteriorSnapshot snap = new InteriorSnapshot();
            st.counts.clear();
            boolean truncated = encodeRegion(interior, r, snap, limit, st.counts);
            st.countsValid = true;
            st.truncated = truncated;
            e = new Encoded(st.version, snap.toParts(InteriorDataPayload.MAX_PART_BYTES), snap.stats(), truncated);
            st.cache = e;
            lastFullBytes = e.stats.bytes();
            if (org.overengineer.bettacraft.debug.LogArea.NETWORK.debug) org.overengineer.bettacraft.debug.LogArea.NETWORK.debug("Encoded interior region {} ({}x{}x{}) v{}: {} non-air blocks in {} sections -> {} bytes in {} part(s), {} ms{}",
                    r.id(), r.sizeX(), r.sizeY(), r.sizeZ(), st.version, e.stats.nonAir(), e.stats.sections(), e.stats.bytes(), e.parts.size(),
                    e.stats.encodeNanos() / 1_000_000.0, truncated ? " (TRUNCATED by maxRenderedBlocks)" : "");
        }
        if (player == null) return;
        fullSends++;
        for (int i = 0; i < e.parts.size(); i++) {
            PacketDistributor.sendToPlayer(player, new InteriorDataPayload(r.id(), e.version, i, e.parts.size(),
                    r.sizeX(), r.sizeY(), r.sizeZ(), r.scale(), e.truncated, e.parts.get(i)));
        }
    }

    /** Encodes all interior blocks (shell excluded) and the floor substrate. Returns true if the block limit cut it off. */
    public static boolean encodeRegion(ServerLevel level, InteriorRegion r, InteriorSnapshot snap, int limit) {
        return encodeRegion(level, r, snap, limit, new Long2IntOpenHashMap());
    }

    static boolean encodeRegion(ServerLevel level, InteriorRegion r, InteriorSnapshot snap, int limit, Long2IntOpenHashMap counts) {
        int x0 = r.originX(), z0 = r.originZ();
        int x1 = x0 + r.sizeX() - 1, y1 = InteriorRegion.BASE_Y + r.sizeY() - 1, z1 = z0 + r.sizeZ() - 1;
        BlockState[] buf = new BlockState[InteriorSnapshot.SECTION_VOLUME];
        for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
            for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
                int sx = cx - (x0 >> 4), sz = cz - (z0 >> 4);
                int before = snap.nonAir();
                encodeSection(level, r, sx, InteriorSnapshot.FLOOR_SY, sz, buf, snap, false);
                if (snap.nonAir() > before) counts.put(sectionKey(sx, InteriorSnapshot.FLOOR_SY, sz), snap.nonAir() - before);
                for (int sy = InteriorRegion.BASE_Y >> 4; sy <= y1 >> 4; sy++) {
                    before = snap.nonAir();
                    encodeSection(level, r, sx, sy, sz, buf, snap, false);
                    if (snap.nonAir() > before) counts.put(sectionKey(sx, sy, sz), snap.nonAir() - before);
                    if (snap.nonAir() > limit) return true;
                }
            }
        }
        return false;
    }

    /** Encodes one section (region-relative chunk sx/sz, world section sy or {@link InteriorSnapshot#FLOOR_SY}). */
    static void encodeSection(ServerLevel level, InteriorRegion r, int sx, int sy, int sz, BlockState[] buf, InteriorSnapshot snap, boolean keepEmpty) {
        int x0 = r.originX(), z0 = r.originZ();
        int x1 = x0 + r.sizeX() - 1, y0 = InteriorRegion.BASE_Y, y1 = y0 + r.sizeY() - 1, z1 = z0 + r.sizeZ() - 1;
        int cx = (x0 >> 4) + sx, cz = (z0 >> 4) + sz;
        java.util.Arrays.fill(buf, null);
        LevelChunk chunk = level.getChunk(cx, cz);
        if (sy == InteriorSnapshot.FLOOR_SY) {
            for (BlockEntity be : chunk.getBlockEntities().values()) {
                BlockPos p = be.getBlockPos();
                if (p.getY() != InteriorRegion.FLOOR_Y || !(be instanceof InteriorFloorBlockEntity f)) continue;
                if (p.getX() < x0 || p.getX() > x1 || p.getZ() < z0 || p.getZ() > z1) continue;
                if (!f.getBlockState().is(org.overengineer.bettacraft.registry.ModBlocks.INTERIOR_FLOOR.get())
                        || !f.getBlockState().getValue(InteriorFloorBlock.SUBSTRATE)) continue; // M17: stale block entity
                buf[InteriorSnapshot.index(p.getX() & 15, 0, p.getZ() & 15)] = f.substrate();
            }
            snap.addSection(sx, sy, sz, buf, keepEmpty);
            return;
        }
        if (sy < level.getMinSectionY() || sy >= level.getMaxSectionY() + 1) return;
        LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sy));
        if (!section.hasOnlyAir()) {
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
                    }
                }
            }
        }
        snap.addSection(sx, sy, sz, buf, keepEmpty);
    }
}
