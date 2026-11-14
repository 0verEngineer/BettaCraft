package org.overengineer.bettacraft.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.monitor.MonitorTank;
import org.overengineer.bettacraft.monitor.MonitorValue;

/**
 * Server -> client (M27): the data of the monitors a player sees, bundled: one payload per player and 20 ticks with an
 * {@link Entry} for each monitor screen (its controller) whose data changed or that the player did not have yet. The
 * tanks (names, live or not, last values) come when they changed; the history lines (graphs) only when a new hour was
 * recorded, the tiles ask for other values or the player is new ({@link org.overengineer.bettacraft.monitor.MonitorSync}).
 */
public record MonitorDataPayload(List<Entry> entries) implements CustomPacketPayload {
    public static final Type<MonitorDataPayload> TYPE = new Type<>(BettaCraft.id("monitor_data"));
    public static final int MAX_ENTRIES = 256, MAX_TANKS = 64, MAX_LINES = 256, MAX_POINTS = 24 * 7;

    /** The history of one value of one tank: {@code points} oldest first, the last one is hour {@code lastHour}. */
    public record HistoryLine(UUID tank, MonitorValue value, long lastHour, float[] points) {}

    /** {@code tanks} null = unchanged; {@code history} null = unchanged. */
    public record Entry(BlockPos pos, @Nullable List<MonitorTank> tanks, @Nullable List<HistoryLine> history) {}

    public static final StreamCodec<FriendlyByteBuf, MonitorDataPayload> STREAM_CODEC = StreamCodec.of(MonitorDataPayload::write, MonitorDataPayload::read);

    private static void write(FriendlyByteBuf buf, MonitorDataPayload p) {
        buf.writeVarInt(p.entries.size());
        for (Entry e : p.entries) {
            buf.writeBlockPos(e.pos());
            buf.writeBoolean(e.tanks() != null);
            if (e.tanks() != null) {
                buf.writeVarInt(e.tanks().size());
                for (MonitorTank t : e.tanks()) t.write(buf);
            }
            buf.writeBoolean(e.history() != null);
            if (e.history() != null) {
                buf.writeVarInt(e.history().size());
                for (HistoryLine l : e.history()) {
                    buf.writeUUID(l.tank());
                    buf.writeVarInt(l.value().ordinal());
                    buf.writeLong(l.lastHour());
                    buf.writeVarInt(l.points().length);
                    for (float f : l.points()) buf.writeFloat(f);
                }
            }
        }
    }

    private static MonitorDataPayload read(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        if (n < 0 || n > MAX_ENTRIES) throw new IllegalArgumentException("monitor entries: " + n);
        List<Entry> entries = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            BlockPos pos = buf.readBlockPos();
            List<MonitorTank> tanks = null;
            if (buf.readBoolean()) {
                int k = buf.readVarInt();
                if (k < 0 || k > MAX_TANKS) throw new IllegalArgumentException("monitor tanks: " + k);
                tanks = new ArrayList<>(k);
                for (int j = 0; j < k; j++) tanks.add(MonitorTank.read(buf));
            }
            List<HistoryLine> history = null;
            if (buf.readBoolean()) {
                int k = buf.readVarInt();
                if (k < 0 || k > MAX_LINES) throw new IllegalArgumentException("monitor history: " + k);
                history = new ArrayList<>(k);
                for (int j = 0; j < k; j++) {
                    UUID tank = buf.readUUID();
                    int v = buf.readVarInt();
                    long last = buf.readLong();
                    int m = buf.readVarInt();
                    if (m < 0 || m > MAX_POINTS) throw new IllegalArgumentException("monitor points: " + m);
                    float[] pts = new float[m];
                    for (int q = 0; q < m; q++) pts[q] = buf.readFloat();
                    if (v >= 0 && v < MonitorValue.ALL.length) history.add(new HistoryLine(tank, MonitorValue.ALL[v], last, pts));
                }
            }
            entries.add(new Entry(pos, tanks, history));
        }
        return new MonitorDataPayload(entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
