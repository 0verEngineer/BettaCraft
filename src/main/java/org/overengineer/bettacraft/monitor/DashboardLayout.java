package org.overengineer.bettacraft.monitor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * M27: the dashboard of one monitor screen: tiles on a grid of {@link #CELLS} cells per monitor block and axis (an 8 x 5
 * screen has 32 x 20 cells). Saved in the screen's controller and synced to the clients with its block entity. When the
 * screen grows, shrinks or splits, the layout of the old screen it overlaps most goes on, clipped to the new size
 * ({@link MonitorScreens}).
 * <p>
 * {@link #auto}: the monitor makes the layout itself ({@link #autoLayout}: two value tiles per tank in range) and makes it
 * again when the tanks or the size change; the editor (part b) switches it off with the first change.
 */
public final class DashboardLayout {
    public static final int CELLS = 4;
    public static final int MAX_TILES = 128;
    public static final int VERSION = 1;

    public boolean auto = true;
    public final List<DashboardTile> tiles = new ArrayList<>();
    public int nextId = 1;

    public DashboardLayout copy() {
        DashboardLayout l = new DashboardLayout();
        l.auto = auto;
        l.nextId = nextId;
        for (DashboardTile t : tiles) l.tiles.add(t.copy());
        return l;
    }

    /** Keeps the tiles inside a grid of {@code gw} x {@code gh} cells: tiles that stick out shrink, those outside go. */
    public void clip(int gw, int gh) {
        tiles.removeIf(t -> t.x < 0 || t.y < 0 || t.x >= gw || t.y >= gh || t.w < 1 || t.h < 1);
        for (DashboardTile t : tiles) {
            t.w = Math.min(t.w, gw - t.x);
            t.h = Math.min(t.h, gh - t.y);
        }
        while (tiles.size() > MAX_TILES) tiles.remove(tiles.size() - 1);
    }

    /** Checks a layout from a client (the editor, part b): nothing outside the grid, unique ids. */
    public void sanitize(int gw, int gh) {
        clip(gw, gh);
        Set<Integer> seen = new HashSet<>();
        for (DashboardTile t : tiles) nextId = Math.max(nextId, t.id + 1);
        for (DashboardTile t : tiles) {
            if (t.id <= 0 || !seen.add(t.id)) {
                t.id = nextId++;
                seen.add(t.id);
            }
        }
    }

    /** The tanks and values whose history the tiles show. */
    public Map<UUID, Set<MonitorValue>> historyNeeds() {
        Map<UUID, Set<MonitorValue>> out = new LinkedHashMap<>();
        for (DashboardTile t : tiles) {
            for (MonitorValue v : t.historyValues()) out.computeIfAbsent(t.tank, k -> EnumSet.noneOf(MonitorValue.class)).add(v);
        }
        return out;
    }

    /** Some tile shows more than the values (warnings, animals, devices, log): the server then sends {@link MonitorDetail}. */
    public boolean needsDetail() {
        for (DashboardTile t : tiles) if (t.needsDetail()) return true;
        return false;
    }

    /** Some alarm tile gives a redstone signal: the screen then checks its tanks even when nobody looks. */
    public boolean hasRedstoneAlarm() {
        for (DashboardTile t : tiles) if (t.redstone()) return true;
        return false;
    }

    /** M32 part d: some alarm tile watches a tank (events and waypoints): the screen checks its tanks even when nobody looks. */
    public boolean hasAlarm() {
        for (DashboardTile t : tiles) if (DashboardTile.ALARM.equals(t.type) && t.tank != null) return true;
        return false;
    }

    /** The alarm tiles with redstone that are triggered now ({@code tank} looks up a tank by id). */
    public int triggeredAlarms(java.util.function.Function<UUID, MonitorTank> tank) {
        int n = 0;
        for (DashboardTile t : tiles) if (t.redstone() && t.triggered(t.tank == null ? null : tank.apply(t.tank))) n++;
        return n;
    }

    /** The first free place for a tile of {@code w} x {@code h} cells on a grid of {@code gw} x {@code gh} (row by row), or null. */
    @javax.annotation.Nullable
    public int[] freeSpot(int w, int h, int gw, int gh, int ignoreId) {
        for (int y = 0; y + h <= gh; y++) {
            for (int x = 0; x + w <= gw; x++) {
                if (free(x, y, w, h, ignoreId)) return new int[]{x, y};
            }
        }
        return null;
    }

    /** No tile (but the one with {@code ignoreId}) overlaps this rectangle. */
    public boolean free(int x, int y, int w, int h, int ignoreId) {
        for (DashboardTile t : tiles) {
            if (t.id == ignoreId) continue;
            if (x < t.x + t.w && t.x < x + w && y < t.y + t.h && t.y < y + h) return false;
        }
        return true;
    }

    /** The tanks in the order of the automatic layout: by name (unnamed last), then by id. */
    public static List<MonitorTank> autoOrder(List<MonitorTank> tanks) {
        List<MonitorTank> sorted = new ArrayList<>(tanks);
        sorted.sort(Comparator.comparing((MonitorTank t) -> t.name.isEmpty() ? "￿" : t.name.toLowerCase(Locale.ROOT))
                .thenComparing(t -> t.id));
        return sorted;
    }

    /** Smallest tile of the automatic layout (cells): one block wide, half a block high. */
    public static final int AUTO_MIN_W = CELLS, AUTO_MIN_H = CELLS / 2;

    /**
     * The automatic layout of a screen of {@code w} x {@code h} blocks: per tank a tile with its temperature (with the graph
     * of the last day) and one with its water quality, next to each other. The tiles are as big as the screen allows (wide
     * rather than tall, at least {@link #AUTO_MIN_W} x {@link #AUTO_MIN_H} cells); tanks that do not fit are left out.
     */
    public static DashboardLayout autoLayout(List<MonitorTank> tanks, int w, int h) {
        DashboardLayout l = new DashboardLayout();
        int gw = w * CELLS, gh = h * CELLS;
        int max = (gw / AUTO_MIN_W) * (gh / AUTO_MIN_H);
        int n = Math.min(tanks.size() * 2, max - max % 2);
        if (n <= 0) return l;
        int bestCols = 1, bestW = 0, bestH = 0;
        double bestScore = -1;
        for (int cols = 1; cols <= n; cols++) {
            if (cols > 1 && cols % 2 == 1) continue; // the two tiles of a tank stay side by side
            int rows = (n + cols - 1) / cols;
            int tw = gw / cols, th = gh / rows;
            if (tw < AUTO_MIN_W || th < AUTO_MIN_H) continue;
            double score = Math.min(tw, 2.0 * th);
            if (score > bestScore) {
                bestScore = score;
                bestCols = cols;
                bestW = tw;
                bestH = th;
            }
        }
        if (bestScore < 0) return l;
        int slot = 0;
        for (MonitorTank t : autoOrder(tanks)) {
            for (MonitorValue v : new MonitorValue[]{MonitorValue.TEMPERATURE, MonitorValue.WATER_QUALITY}) {
                if (slot >= n) return l;
                l.tiles.add(DashboardTile.value(l.nextId++, slot % bestCols * bestW, slot / bestCols * bestH, bestW, bestH, t.id, v,
                        v == MonitorValue.TEMPERATURE));
                slot++;
            }
        }
        return l;
    }

    /** Same tiles (for the automatic layout: only make it again when something changed). */
    public boolean sameAs(DashboardLayout o) {
        return save().equals(o.save());
    }

    public CompoundTag save() {
        CompoundTag c = new CompoundTag();
        c.putInt("version", VERSION);
        c.putBoolean("auto", auto);
        c.putInt("next", nextId);
        ListTag list = new ListTag();
        for (DashboardTile t : tiles) list.add(t.save());
        c.put("tiles", list);
        return c;
    }

    public static DashboardLayout load(CompoundTag c) {
        DashboardLayout l = new DashboardLayout();
        if (c.isEmpty()) return l;
        l.auto = !c.contains("auto") || c.getBoolean("auto");
        l.nextId = Math.max(1, c.getInt("next"));
        ListTag list = c.getList("tiles", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && l.tiles.size() < MAX_TILES; i++) {
            DashboardTile t = DashboardTile.load(list.getCompound(i));
            l.tiles.add(t);
            l.nextId = Math.max(l.nextId, t.id + 1);
        }
        return l;
    }
}
