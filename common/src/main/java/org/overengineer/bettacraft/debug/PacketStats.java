package org.overengineer.bettacraft.debug;

import java.util.Map;
import java.util.TreeMap;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * M57: the packets Bettacraft sends, for the network area of the debug log (only called while it is on): with TRACE
 * every packet, with DEBUG one line a minute with the count per packet type. The sizes of the big packets (the interior
 * syncs) are written where they are encoded ({@code InteriorSync}).
 */
public final class PacketStats {
    static final long PERIOD_MS = 60_000L;
    private static final Map<String, Integer> COUNTS = new TreeMap<>();
    private static long since = -1;

    private PacketStats() {}

    public static void sent(CustomPacketPayload payload, String to) {
        String id = payload.type().id().toString();
        if (LogArea.NETWORK.trace) LogArea.NETWORK.trace("send {} to {}", id, to);
        String summary = null;
        synchronized (COUNTS) {
            long now = System.currentTimeMillis();
            if (since < 0) since = now;
            COUNTS.merge(id, 1, Integer::sum);
            if (now - since >= PERIOD_MS) {
                summary = COUNTS.toString();
                COUNTS.clear();
                since = now;
            }
        }
        if (summary != null) LogArea.NETWORK.debug("packets sent in the last minute: {}", summary);
    }
}
