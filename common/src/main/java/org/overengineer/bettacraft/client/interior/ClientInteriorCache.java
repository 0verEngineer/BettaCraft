package org.overengineer.bettacraft.client.interior;

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import org.overengineer.bettacraft.platform.network.PacketDistributor;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.network.InteriorDataPayload;
import org.overengineer.bettacraft.network.InteriorDeltaPayload;
import org.overengineer.bettacraft.network.InteriorRequestPayload;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.interior.InteriorSnapshot;

/**
 * Client store of received interior regions. Requests missing regions of nearby tanks, applies deltas, and checks
 * every few seconds whether its version is still current (a missed delta triggers a full resync).
 */
public final class ClientInteriorCache {
    private static final Int2ObjectOpenHashMap<ClientRegion> REGIONS = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectOpenHashMap<byte[][]> PENDING = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectOpenHashMap<int[]> PENDING_META = new Int2ObjectOpenHashMap<>();
    private static final Int2LongOpenHashMap LAST_REQUEST = new Int2LongOpenHashMap();
    /** Bytes received for the last full region / delta (bench). */
    public static volatile int lastFullBytes, lastDeltaBytes;

    private ClientInteriorCache() {}

    @Nullable
    /** M22.1: every miniature is meshed again (which blocks sway changed). */
    public static void markAllDirty() {
        for (ClientRegion r : REGIONS.values()) r.markAllDirty();
    }

    public static ClientRegion get(int id) {
        return REGIONS.get(id);
    }

    public static void accept(InteriorDataPayload p) {
        if (p.parts() < 1 || p.parts() > 1024) return; // M17: malformed data
        int[] meta = PENDING_META.get(p.regionId());
        byte[][] parts = PENDING.get(p.regionId());
        if (meta == null || meta[0] != p.version() || parts == null || parts.length != p.parts()) {
            parts = new byte[p.parts()][];
            meta = new int[]{p.version()};
            PENDING.put(p.regionId(), parts);
            PENDING_META.put(p.regionId(), meta);
        }
        if (p.part() < 0 || p.part() >= parts.length) return;
        parts[p.part()] = p.data();
        for (byte[] b : parts) if (b == null) return;

        PENDING.remove(p.regionId());
        PENDING_META.remove(p.regionId());
        long t0 = System.nanoTime();
        List<InteriorSnapshot.Section> sections = new ArrayList<>();
        int bytes = 0;
        for (byte[] b : parts) {
            sections.addAll(InteriorSnapshot.decode(b));
            bytes += b.length;
        }
        lastFullBytes = bytes;
        ClientRegion region = new ClientRegion(p.regionId(), p.version(), p.sizeX(), p.sizeY(), p.sizeZ(), p.scale(), p.truncated(), sections);
        ClientRegion old = REGIONS.put(p.regionId(), region);
        if (old != null) {
            if (old.sizeX == region.sizeX && old.sizeY == region.sizeY && old.sizeZ == region.sizeZ) {
                // keep showing the old meshes until the new ones are built (no flicker)
                region.meshes.putAll(old.meshes);
                region.cellGeneration.putAll(old.cellGeneration);
                region.lightKey = old.lightKey;
                region.shaderPack = old.shaderPack;
                old.meshes.clear();
                region.markAllDirty();
            }
            old.close();
        }
        boolean stats = ClientConfig.LOG_MINIATURE_STATS.get();
        if (stats || org.overengineer.bettacraft.debug.LogArea.NETWORK.debug) { // M57: also with the network area of the debug log
            MiniatureRenderer.logStats(stats, "Received interior region {} v{}: {} bytes, {} sections, {} blocks, decoded in {} ms",
                    p.regionId(), p.version(), bytes, sections.size(), region.nonAir, (System.nanoTime() - t0) / 1_000_000.0);
        }
    }

    public static void acceptDelta(InteriorDeltaPayload p) {
        lastDeltaBytes = p.data().length;
        ClientRegion r = REGIONS.get(p.regionId());
        if (r == null) return; // full sync will come on request
        if (r.version != p.fromVersion()) {
            requestNow(p.regionId(), r.version);
            return;
        }
        r.apply(p.toVersion(), InteriorSnapshot.decode(p.data()));
    }

    private static void requestNow(int regionId, int known) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (TankBlockEntity be : TankBlockEntity.CLIENT_CONTROLLERS) {
            TankData t = be.getTank();
            if (t != null && t.regionId() == regionId && be.getLevel() == mc.level) { // M17: not a stale block entity
                LAST_REQUEST.put(regionId, net.minecraft.util.Util.getMillis() / 50L); // M67.5: the same clock as tick()
                PacketDistributor.sendToServer(new InteriorRequestPayload(regionId, be.getBlockPos(), known));
                return;
            }
        }
    }

    /** Client tick: request missing regions, re-check versions every 5 s, drop regions that are no longer needed. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        // M67.5: real time in ticks of 50 ms (the game time stands still while the ticks are frozen: a region dropped after 600 idle
        // ticks was never asked for again)
        long now = net.minecraft.util.Util.getMillis() / 50L;
        double dist = ClientConfig.MINIATURE_RENDER_DISTANCE.get();
        IntOpenHashSet used = new IntOpenHashSet();
        for (TankBlockEntity be : TankBlockEntity.CLIENT_CONTROLLERS) {
            TankData t = be.getTank();
            if (t == null || t.regionId() < 0 || be.getLevel() != mc.level) continue;
            used.add(t.regionId());
            if (be.getBlockPos().distToCenterSqr(mc.player.position()) > (dist + 16) * (dist + 16)) continue;
            ClientRegion r = REGIONS.get(t.regionId());
            long last = LAST_REQUEST.getOrDefault(t.regionId(), -1000L);
            if ((r == null && now - last > 100) || (r != null && now - last > 100 && (now + t.regionId()) % 100 == 0)) {
                LAST_REQUEST.put(t.regionId(), now);
                PacketDistributor.sendToServer(new InteriorRequestPayload(t.regionId(), be.getBlockPos(), r == null ? -1 : r.version));
            }
        }
        // M32 part b: tanks travelling on contraptions and airships (seen by their renderer lately)
        var seen = org.overengineer.bettacraft.client.render.TravellingTankRenderer.SEEN.int2ObjectEntrySet().iterator();
        while (seen.hasNext()) {
            var e = seen.next();
            var s = e.getValue();
            if (now - s.time() > 200 || now < s.time()) {
                seen.remove();
                continue;
            }
            int id = e.getIntKey();
            used.add(id);
            ClientRegion r = REGIONS.get(id);
            long last = LAST_REQUEST.getOrDefault(id, -1000L);
            if ((r == null && now - last > 100) || (r != null && now - last > 100 && (now + id) % 100 == 0)) {
                LAST_REQUEST.put(id, now);
                PacketDistributor.sendToServer(new InteriorRequestPayload(id, s.requestPos(), r == null ? -1 : r.version));
            }
        }
        var it = REGIONS.int2ObjectEntrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            ClientRegion r = e.getValue();
            if (used.contains(e.getIntKey())) {
                r.idleTicks = 0;
            } else if (++r.idleTicks > 600) {
                r.close();
                it.remove();
            }
        }
    }

    public static void clear() {
        for (ClientRegion r : REGIONS.values()) r.close();
        REGIONS.clear();
        PENDING.clear();
        PENDING_META.clear();
        LAST_REQUEST.clear();
        MiniatureRenderer.drainResults();
    }
}
