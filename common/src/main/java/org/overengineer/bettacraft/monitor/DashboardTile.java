package org.overengineer.bettacraft.monitor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;

/**
 * M27: one tile of a monitor's dashboard. Position and size are in grid cells ({@link DashboardLayout#CELLS} per monitor
 * block and axis, x to the right and y down from the top left corner of the screen). {@link #type} names the widget
 * ({@link #VALUE} in part a; part b adds the others), {@link #tank} the tank it shows (by its stable id, null for widgets of
 * several tanks), {@link #settings} what the widget needs (value tile: {@code value} = a {@link MonitorValue} id,
 * {@code spark} = a small graph of the last day; graphs: {@code history} = value ids separated by commas), and two ARGB
 * colours (0 = the widget's default).
 */
public final class DashboardTile {
    /** A value tile: one value of a tank, big, with the tank's name. */
    public static final String VALUE = "value";
    /** Traffic light: the state of a tank (green, yellow, red) with the warnings of its menu. */
    public static final String STATUS = "status";
    /** History graph: one to three values ({@code history}) over {@code days} (1-7) game days. */
    public static final String GRAPH = "graph";
    /** Animal list: count per species, breeding (nest, eggs, young). */
    public static final String ANIMALS = "animals";
    /** Device status: filters, heaters, light, CO2, air pumps (on/off, power level), energy. */
    public static final String DEVICES = "devices";
    /** Alarm: blinks while {@code value} is above / below {@code limit} ({@code below}); {@code redstone}: the screen gives a signal. */
    public static final String ALARM = "alarm";
    /** Event log: the latest logbook entries of one tank or (tank null) of every tank of the screen. */
    public static final String LOG = "log";
    /** Every widget type in the order of the editor. */
    public static final List<String> TYPES = List.of(VALUE, STATUS, GRAPH, ANIMALS, DEVICES, ALARM, LOG);
    /** Most values one graph shows. */
    public static final int MAX_GRAPH_VALUES = 3;
    public static final int MAX_TYPE_LENGTH = 32;
    /** M33 part d: longest text setting a client may send ({@code value} id, {@code history} list). */
    public static final int MAX_SETTING_LENGTH = 256;

    public int id;
    public int x, y, w = 1, h = 1;
    public String type = VALUE;
    @Nullable
    public UUID tank;
    public CompoundTag settings = new CompoundTag();
    public int background, foreground;

    public DashboardTile() {}

    public DashboardTile(int id, int x, int y, int w, int h, String type, @Nullable UUID tank) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.type = type;
        this.tank = tank;
    }

    /** A value tile of {@code value} of {@code tank}. */
    public static DashboardTile value(int id, int x, int y, int w, int h, @Nullable UUID tank, MonitorValue value, boolean spark) {
        DashboardTile t = new DashboardTile(id, x, y, w, h, VALUE, tank);
        t.settings.putString("value", value.id);
        t.settings.putBoolean("spark", spark);
        return t;
    }

    /** The value of a value tile (temperature when unknown). */
    public MonitorValue value() {
        MonitorValue v = MonitorValue.byId(settings.getString("value"));
        return v == null ? MonitorValue.TEMPERATURE : v;
    }

    public boolean spark() {
        return settings.getBoolean("spark");
    }

    /** A history graph of up to three values over {@code days}. */
    public static DashboardTile graph(int id, int x, int y, int w, int h, @Nullable UUID tank, int days, MonitorValue... values) {
        DashboardTile t = new DashboardTile(id, x, y, w, h, GRAPH, tank);
        t.setHistory(List.of(values));
        t.settings.putInt("days", days);
        return t;
    }

    /** An alarm tile: {@code value} above (or below) {@code limit}, with a redstone signal or not. */
    public static DashboardTile alarm(int id, int x, int y, int w, int h, @Nullable UUID tank, MonitorValue value, boolean below, float limit,
                                      boolean redstone) {
        DashboardTile t = new DashboardTile(id, x, y, w, h, ALARM, tank);
        t.settings.putString("value", value.id);
        t.settings.putBoolean("below", below);
        t.settings.putFloat("limit", limit);
        t.settings.putBoolean("redstone", redstone);
        return t;
    }

    /** The widget type, {@link #VALUE} for unknown ones (a newer version's widget in an older game). */
    public String kind() {
        return TYPES.contains(type) ? type : VALUE;
    }

    /** Graph: game days shown (1-7). */
    public int days() {
        int d = settings.contains("days") ? settings.getInt("days") : 1;
        return Math.max(1, Math.min(7, d));
    }

    /** Graph: the values (at most {@link #MAX_GRAPH_VALUES}, the value tile's value if none are set). */
    public List<MonitorValue> graphValues() {
        List<MonitorValue> out = new ArrayList<>();
        for (String s : settings.getString("history").split(",")) {
            MonitorValue v = MonitorValue.byId(s.trim());
            if (v != null && !out.contains(v) && out.size() < MAX_GRAPH_VALUES) out.add(v);
        }
        if (out.isEmpty()) out.add(value());
        return out;
    }

    public void setHistory(List<MonitorValue> values) {
        StringBuilder b = new StringBuilder();
        for (MonitorValue v : values) {
            if (v == null) continue;
            if (b.length() > 0) b.append(',');
            b.append(v.id);
        }
        settings.putString("history", b.toString());
    }

    /** Alarm: true = alarm below the limit, false = above. */
    public boolean below() {
        return settings.getBoolean("below");
    }

    public float limit() {
        return settings.getFloat("limit");
    }

    /** Alarm: the screen gives a redstone signal while it is on. */
    public boolean redstone() {
        return ALARM.equals(type) && settings.getBoolean("redstone");
    }

    /** Alarm: the value of the tank is past the limit (an unknown tank or value, or a tank not loaded, is no alarm). */
    public boolean triggered(@Nullable MonitorTank t) {
        if (!ALARM.equals(type) || t == null || !t.live) return false;
        float v = t.value(value());
        if (!Float.isFinite(v)) return false;
        return below() ? v < limit() : v > limit();
    }

    /** The widget shows more than the values: warnings, animals, devices or the log ({@link MonitorDetail}). */
    public boolean needsDetail() {
        return STATUS.equals(type) || ANIMALS.equals(type) || DEVICES.equals(type) || LOG.equals(type);
    }

    /** The values of {@link #tank} whose history this tile shows (empty if none). */
    public List<MonitorValue> historyValues() {
        if (tank == null) return List.of();
        List<MonitorValue> out = new ArrayList<>();
        if ((VALUE.equals(type) || ALARM.equals(type)) && spark()) out.add(value());
        if (GRAPH.equals(type)) {
            for (MonitorValue v : graphValues()) if (!out.contains(v)) out.add(v);
        } else if (settings.contains("history")) {
            for (String s : settings.getString("history").split(",")) {
                MonitorValue v = MonitorValue.byId(s.trim());
                if (v != null && !out.contains(v)) out.add(v);
            }
        }
        return out;
    }

    /**
     * M33 part d (hardening): only the settings the widgets know, with sane values (a layout from a client must not carry
     * arbitrary NBT into the block entity, the chunk and every client's sync).
     */
    public void sanitizeSettings() {
        CompoundTag in = settings, out = new CompoundTag();
        for (String k : new String[]{"value", "history"}) {
            if (in.contains(k, net.minecraft.nbt.Tag.TAG_STRING) && in.getString(k).length() <= MAX_SETTING_LENGTH) out.putString(k, in.getString(k));
        }
        for (String k : new String[]{"spark", "below", "redstone"}) if (in.contains(k, net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) out.putBoolean(k, in.getBoolean(k));
        if (in.contains("days", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) out.putInt("days", Math.max(1, Math.min(7, in.getInt("days"))));
        if (in.contains("limit", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC) && Float.isFinite(in.getFloat("limit"))) out.putFloat("limit", in.getFloat("limit"));
        settings = out;
    }

    public DashboardTile copy() {
        DashboardTile t = new DashboardTile(id, x, y, w, h, type, tank);
        t.settings = settings.copy();
        t.background = background;
        t.foreground = foreground;
        return t;
    }

    public CompoundTag save() {
        CompoundTag c = new CompoundTag();
        c.putInt("id", id);
        c.putIntArray("rect", new int[]{x, y, w, h});
        c.putString("type", type);
        if (tank != null) c.putUUID("tank", tank);
        if (!settings.isEmpty()) c.put("settings", settings.copy());
        if (background != 0) c.putInt("bg", background);
        if (foreground != 0) c.putInt("fg", foreground);
        return c;
    }

    public static DashboardTile load(CompoundTag c) {
        DashboardTile t = new DashboardTile();
        t.id = c.getInt("id");
        int[] r = c.getIntArray("rect");
        if (r.length == 4) {
            t.x = r[0];
            t.y = r[1];
            t.w = r[2];
            t.h = r[3];
        }
        String type = c.getString("type");
        t.type = type.isEmpty() || type.length() > MAX_TYPE_LENGTH ? VALUE : type;
        t.tank = c.hasUUID("tank") ? c.getUUID("tank") : null;
        t.settings = c.getCompound("settings").copy();
        t.background = c.getInt("bg");
        t.foreground = c.getInt("fg");
        return t;
    }
}
