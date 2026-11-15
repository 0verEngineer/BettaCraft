package org.overengineer.bettacraft.monitor;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/**
 * M27: the time series of every tank for the graphs of the monitors. One point per in-game hour (1000 ticks of game time)
 * with all {@link MonitorValue}s, the last {@link #HOURS} hours (7 in-game days), recorded by the care step of the tank
 * ({@link #onCare}) whether a monitor looks or not, so a new monitor shows the past days at once.
 * <p>
 * One SavedData per dimension ({@code bettacraft_tank_history}); per tank the newest hour and the points as float bits in an
 * int array (at most 168 x 13 values, about 9 KB). The value ids are saved with it, so new values can be added later. Tanks
 * that recorded nothing for {@link #HOURS} hours (broken up, gone) are dropped. Gaps (unloaded hours) are {@link Float#NaN}.
 */
public final class TankHistory extends SavedData {
    public static final String NAME = "bettacraft_tank_history";
    public static final int HOURS = 7 * 24;
    public static final long HOUR_TICKS = 1000;

    /** The points of one tank, oldest first; the last one is {@link #lastHour}. */
    public static final class Series {
        long lastHour;
        final ArrayDeque<float[]> points = new ArrayDeque<>();

        public long lastHour() {
            return lastHour;
        }

        public int size() {
            return points.size();
        }

        /** The last {@code hours} points of {@code v}, oldest first (NaN where nothing was recorded). */
        public float[] values(MonitorValue v, int hours) {
            int n = Math.max(0, Math.min(hours, points.size()));
            float[] out = new float[n];
            int skip = points.size() - n, i = 0, k = 0;
            for (float[] p : points) {
                if (k++ < skip) continue;
                out[i++] = v.ordinal() < p.length ? p[v.ordinal()] : Float.NaN;
            }
            return out;
        }
    }

    private final Map<UUID, Series> tanks = new HashMap<>();
    private long lastPrune = Long.MIN_VALUE;

    public static TankHistory get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(TankHistory::new, TankHistory::load, null), NAME);
    }

    public static long hour(long gameTime) {
        return Math.floorDiv(gameTime, HOUR_TICKS);
    }

    /** From the care step of a tank: records a point when a new hour began. */
    public static void onCare(ServerLevel level, TankBlockEntity controller) {
        if (!Features.enabled(Feature.MONITORS) || controller.getTank() == null) return;
        TankHistory h = get(level);
        long hour = hour(level.getGameTime());
        Series s = h.tanks.get(controller.getTank().id());
        if (s != null && s.lastHour == hour) return;
        h.record(controller.getTank().id(), hour, MonitorValue.sampleAll(level, controller));
    }

    /** Adds the point of {@code hour} (gaps since the last point become NaN; an older hour is ignored). */
    public void record(UUID tank, long hour, float[] values) {
        Series s = tanks.computeIfAbsent(tank, k -> new Series());
        if (!s.points.isEmpty()) {
            if (hour <= s.lastHour) return;
            long gap = Math.min(HOURS, hour - s.lastHour - 1);
            if (hour - s.lastHour - 1 >= HOURS) s.points.clear();
            for (long g = 0; g < gap && !s.points.isEmpty(); g++) {
                float[] nan = new float[MonitorValue.ALL.length];
                java.util.Arrays.fill(nan, Float.NaN);
                s.points.addLast(nan);
            }
        }
        s.points.addLast(values.clone());
        while (s.points.size() > HOURS) s.points.removeFirst();
        s.lastHour = hour;
        prune(hour);
        setDirty();
    }

    private void prune(long hour) {
        if (lastPrune != Long.MIN_VALUE && hour - lastPrune < 24) return;
        lastPrune = hour;
        for (Iterator<Series> it = tanks.values().iterator(); it.hasNext(); ) if (hour - it.next().lastHour > HOURS) it.remove();
    }

    @Nullable
    public Series series(UUID tank) {
        return tanks.get(tank);
    }

    public int tankCount() {
        return tanks.size();
    }

    // ------------------------------------------------------------------ saving

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("version", 1);
        ListTag ids = new ListTag();
        for (MonitorValue v : MonitorValue.ALL) ids.add(StringTag.valueOf(v.id));
        tag.put("values", ids);
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Series> e : tanks.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", e.getKey());
            c.putLong("last", e.getValue().lastHour);
            int m = MonitorValue.ALL.length;
            int[] data = new int[e.getValue().points.size() * m];
            int i = 0;
            for (float[] p : e.getValue().points) for (int k = 0; k < m; k++) data[i++] = Float.floatToIntBits(k < p.length ? p[k] : Float.NaN);
            c.putIntArray("data", data);
            list.add(c);
        }
        tag.put("tanks", list);
        return tag;
    }

    public static TankHistory load(CompoundTag tag, HolderLookup.Provider registries) {
        TankHistory h = new TankHistory();
        ListTag ids = tag.getList("values", Tag.TAG_STRING);
        int saved = ids.size();
        int[] map = new int[saved];
        for (int k = 0; k < saved; k++) {
            MonitorValue v = MonitorValue.byId(ids.getString(k));
            map[k] = v == null ? -1 : v.ordinal();
        }
        ListTag list = tag.getList("tanks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            if (!c.hasUUID("id") || saved == 0) continue;
            Series s = new Series();
            s.lastHour = c.getLong("last");
            int[] data = c.getIntArray("data");
            for (int p = 0; p + saved <= data.length; p += saved) {
                float[] point = new float[MonitorValue.ALL.length];
                java.util.Arrays.fill(point, Float.NaN);
                for (int k = 0; k < saved; k++) if (map[k] >= 0) point[map[k]] = Float.intBitsToFloat(data[p + k]);
                s.points.addLast(point);
            }
            while (s.points.size() > HOURS) s.points.removeFirst();
            if (!s.points.isEmpty()) h.tanks.put(c.getUUID("id"), s);
        }
        return h;
    }
}
