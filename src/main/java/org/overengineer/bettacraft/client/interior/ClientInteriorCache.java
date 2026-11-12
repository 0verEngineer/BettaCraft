package org.overengineer.bettacraft.client.interior;

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.network.InteriorDataPayload;
import org.overengineer.bettacraft.network.InteriorRequestPayload;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.interior.InteriorSnapshot;

/** Client store of received interior regions; requests regions for nearby tanks. */
public final class ClientInteriorCache {
    private static final Int2ObjectOpenHashMap<ClientRegion> REGIONS = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectOpenHashMap<byte[][]> PENDING = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectOpenHashMap<int[]> PENDING_META = new Int2ObjectOpenHashMap<>();
    private static final Int2LongOpenHashMap LAST_REQUEST = new Int2LongOpenHashMap();

    private ClientInteriorCache() {}

    @Nullable
    public static ClientRegion get(int id) {
        return REGIONS.get(id);
    }

    public static void accept(InteriorDataPayload p) {
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
        ClientRegion region = new ClientRegion(p.regionId(), p.version(), p.sizeX(), p.sizeY(), p.sizeZ(), p.scale(), p.truncated(), sections);
        ClientRegion old = REGIONS.put(p.regionId(), region);
        if (old != null) old.close();
        if (ClientConfig.LOG_MINIATURE_STATS.get()) {
            MiniatureRenderer.LOGGER.info("Received interior region {} v{}: {} bytes, {} sections, {} blocks, decoded in {} ms",
                    p.regionId(), p.version(), bytes, sections.size(), region.nonAir, (System.nanoTime() - t0) / 1_000_000.0);
        }
    }

    /** Client tick: request missing regions of nearby tanks, drop regions that are no longer needed. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        long now = mc.level.getGameTime();
        double dist = ClientConfig.MINIATURE_RENDER_DISTANCE.get();
        IntOpenHashSet used = new IntOpenHashSet();
        for (TankBlockEntity be : TankBlockEntity.CLIENT_CONTROLLERS) {
            TankData t = be.getTank();
            if (t == null || t.regionId() < 0 || be.getLevel() != mc.level) continue;
            used.add(t.regionId());
            if (be.getBlockPos().distToCenterSqr(mc.player.position()) > (dist + 16) * (dist + 16)) continue;
            ClientRegion r = REGIONS.get(t.regionId());
            if (r == null && now - LAST_REQUEST.getOrDefault(t.regionId(), -1000L) > 100) {
                LAST_REQUEST.put(t.regionId(), now);
                PacketDistributor.sendToServer(new InteriorRequestPayload(t.regionId(), be.getBlockPos(), -1));
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
    }
}
