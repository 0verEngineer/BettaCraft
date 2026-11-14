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
    public static final int MAX_TYPE_LENGTH = 32;

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

    /** The values of {@link #tank} whose history this tile shows (empty if none). */
    public List<MonitorValue> historyValues() {
        if (tank == null) return List.of();
        List<MonitorValue> out = new ArrayList<>();
        if (VALUE.equals(type) && spark()) out.add(value());
        if (settings.contains("history")) {
            for (String s : settings.getString("history").split(",")) {
                MonitorValue v = MonitorValue.byId(s.trim());
                if (v != null && !out.contains(v)) out.add(v);
            }
        }
        return out;
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
