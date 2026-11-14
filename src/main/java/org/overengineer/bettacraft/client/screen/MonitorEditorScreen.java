package org.overengineer.bettacraft.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.overengineer.bettacraft.monitor.DashboardLayout;
import org.overengineer.bettacraft.monitor.DashboardTile;
import org.overengineer.bettacraft.monitor.MonitorBlockEntity;
import org.overengineer.bettacraft.monitor.MonitorTank;
import org.overengineer.bettacraft.monitor.MonitorValue;
import org.overengineer.bettacraft.network.MonitorLayoutPayload;

/**
 * M27 part b: the dashboard editor of a monitor screen (right-click a monitor). On the left the grid of the screen
 * ({@link DashboardLayout#CELLS} cells per block): click a tile to pick it, drag it to move it, drag its bottom right corner
 * to resize it (tiles never overlap). On the right the settings of the picked tile: widget type, tank (among the tanks the
 * screen found), value(s), graph days, alarm limit and redstone, size, colours. "Save" sends the layout to the server
 * ({@link MonitorLayoutPayload}, which checks the build rights), "Automatic" gives the screen back its automatic layout.
 */
public class MonitorEditorScreen extends Screen {
    private static final int PANEL_W = 150, ROW = 18, BH = 16;
    private static final int TILE = 0xFF12303A;
    /** Background colours (0 = the widget's default). */
    public static final int[] BACKGROUNDS = {0, 0xFF1F3A12, 0xFF3A2A12, 0xFF3A1230, 0xFF1A1A1A, 0xFF0E2A5A, 0xFF5A0E0E, 0xFF2E3A44};
    /** Text colours (0 = the widget's default). */
    public static final int[] FOREGROUNDS = {0, 0xFFFFFFFF, 0xFFFFE070, 0xFF70E0FF, 0xFF90F090, 0xFFFFA040, 0xFFFF90C0};

    private final BlockPos pos;
    @Nullable
    private DashboardLayout layout;
    private int gw = 4, gh = 4, bw = 1, bh = 1;
    private int selected = -1;
    // grid geometry (screen pixels)
    private int gx, gy, cell = 8;
    // dragging: 1 = move, 2 = resize
    private int drag, grabX, grabY;
    private Component note = Component.empty();
    @Nullable
    private EditBox limitBox;

    public MonitorEditorScreen(BlockPos pos) {
        super(Component.translatable("gui.bettacraft.monitor.title"));
        this.pos = pos.immutable();
    }

    @Nullable
    private MonitorBlockEntity controller() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getBlockEntity(pos) instanceof MonitorBlockEntity be)) return null;
        return be.controller();
    }

    private List<MonitorTank> tanks() {
        MonitorBlockEntity c = controller();
        return c == null ? List.of() : DashboardLayout.autoOrder(c.clientTanks);
    }

    @Nullable
    private DashboardTile tile() {
        if (layout == null) return null;
        for (DashboardTile t : layout.tiles) if (t.id == selected) return t;
        return null;
    }

    @Override
    protected void init() {
        MonitorBlockEntity c = controller();
        if (c == null) return;
        if (layout == null) {
            layout = c.layout().copy();
            bw = c.width();
            bh = c.height();
            gw = bw * DashboardLayout.CELLS;
            gh = bh * DashboardLayout.CELLS;
        }
        int availW = width - PANEL_W - 30, availH = height - 64;
        cell = Math.max(3, Math.min(availW / gw, availH / gh));
        gx = 12 + Math.max(0, (availW - cell * gw) / 2);
        gy = 30;
        int by = height - 24, bwid = Math.min(90, (width - 24) / 4 - 4);
        addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.monitor.add"), b -> addTile()).bounds(12, by, bwid, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.monitor.auto"), b -> sendAuto()).bounds(12 + (bwid + 4), by, bwid, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.monitor.save"), b -> save()).bounds(width - 12 - 2 * bwid - 4, by, bwid, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose()).bounds(width - 12 - bwid, by, bwid, 20).build());
        limitBox = null;
        DashboardTile t = tile();
        if (t == null) return;
        int px = width - PANEL_W - 8, y = 30, half = (PANEL_W - 4) / 2;
        String kind = t.kind();
        addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.monitor.type", typeName(kind)), b -> {
            int i = DashboardTile.TYPES.indexOf(kind);
            setType(t, DashboardTile.TYPES.get((i + (hasShiftDown() ? DashboardTile.TYPES.size() - 1 : 1)) % DashboardTile.TYPES.size()));
            changed();
        }).bounds(px, y, PANEL_W, BH).build());
        y += ROW;
        addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.monitor.tank", tankLabel(t)), b -> {
            cycleTank(t);
            changed();
        }).bounds(px, y, PANEL_W, BH).build());
        y += ROW;
        if (!DashboardTile.STATUS.equals(kind) && !DashboardTile.ANIMALS.equals(kind) && !DashboardTile.DEVICES.equals(kind) && !DashboardTile.LOG.equals(kind)) {
            if (DashboardTile.GRAPH.equals(kind)) {
                List<MonitorValue> vs = t.graphValues();
                for (int i = 0; i < DashboardTile.MAX_GRAPH_VALUES; i++) {
                    int slot = i;
                    MonitorValue v = slot < vs.size() ? vs.get(slot) : null;
                    int w = i == 0 ? PANEL_W : half;
                    int x = i == 0 ? px : px + (i - 1) * (half + 4);
                    Component label = v == null ? Component.translatable("gui.bettacraft.monitor.line_none", i + 1)
                            : i == 0 ? Component.translatable("gui.bettacraft.monitor.line", i + 1, Component.translatable(v.translationKey()))
                            : Component.translatable(v.translationKey()); // half buttons: only the name
                    addRenderableWidget(Button.builder(label, b -> {
                        cycleGraphValue(t, slot);
                        changed();
                    }).bounds(x, y, w, BH).build());
                    if (i == 0 || i == 2) y += ROW;
                }
                addRenderableWidget(Button.builder(Component.literal("-"), b -> {
                    t.settings.putInt("days", Math.max(1, t.days() - 1));
                    changed();
                }).bounds(px, y, 20, BH).build());
                addRenderableWidget(Button.builder(Component.literal("+"), b -> {
                    t.settings.putInt("days", Math.min(7, t.days() + 1));
                    changed();
                }).bounds(px + PANEL_W - 20, y, 20, BH).build());
                y += ROW;
            } else {
                addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.monitor.value", Component.translatable(t.value().translationKey())), b -> {
                    t.settings.putString("value", next(MonitorValue.ALL, t.value(), hasShiftDown()).id);
                    changed();
                }).bounds(px, y, PANEL_W, BH).build());
                y += ROW;
                if (DashboardTile.VALUE.equals(kind)) {
                    addRenderableWidget(Button.builder(Component.translatable(t.spark() ? "gui.bettacraft.monitor.spark_on" : "gui.bettacraft.monitor.spark_off"), b -> {
                        t.settings.putBoolean("spark", !t.spark());
                        changed();
                    }).bounds(px, y, PANEL_W, BH).build());
                    y += ROW;
                } else {
                    addRenderableWidget(Button.builder(Component.translatable(t.below() ? "gui.bettacraft.monitor.below" : "gui.bettacraft.monitor.above"), b -> {
                        t.settings.putBoolean("below", !t.below());
                        changed();
                    }).bounds(px, y, half, BH).build());
                    limitBox = new EditBox(font, px + half + 4, y, half, BH, Component.translatable("gui.bettacraft.monitor.limit"));
                    limitBox.setMaxLength(10);
                    limitBox.setValue(fmt(t.limit()));
                    limitBox.setResponder(s -> {
                        try {
                            float f = Float.parseFloat(s.trim().replace(',', '.'));
                            if (Float.isFinite(f)) t.settings.putFloat("limit", f);
                        } catch (NumberFormatException ignored) {
                            // keep the old limit until the text is a number
                        }
                    });
                    addRenderableWidget(limitBox);
                    y += ROW;
                    addRenderableWidget(Button.builder(Component.translatable(t.redstone() ? "gui.bettacraft.monitor.redstone_on" : "gui.bettacraft.monitor.redstone_off"), b -> {
                        t.settings.putBoolean("redstone", !t.redstone());
                        changed();
                    }).bounds(px, y, PANEL_W, BH).build());
                    y += ROW;
                }
            }
        }
        // size
        int sw = (PANEL_W - 12) / 4;
        String[] labels = {"W-", "W+", "H-", "H+"};
        for (int i = 0; i < 4; i++) {
            int k = i;
            addRenderableWidget(Button.builder(Component.literal(labels[i]), b -> resize(t, k < 2 ? (k == 0 ? -1 : 1) : 0, k >= 2 ? (k == 2 ? -1 : 1) : 0))
                    .bounds(px + i * (sw + 4), y, sw, BH).build());
        }
        y += ROW;
        addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.monitor.background"), b -> {
            t.background = nextColour(BACKGROUNDS, t.background);
            changed();
        }).bounds(px, y, half, BH).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.monitor.foreground"), b -> {
            t.foreground = nextColour(FOREGROUNDS, t.foreground);
            changed();
        }).bounds(px + half + 4, y, half, BH).build());
        y += ROW;
        addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.monitor.delete").withStyle(ChatFormatting.RED), b -> {
            layout.tiles.remove(t);
            selected = -1;
            changed();
        }).bounds(px, y, PANEL_W, BH).build());
    }

    private void changed() {
        rebuildWidgets();
    }

    // ------------------------------------------------------------------ editing

    private static String typeName(String kind) {
        return Component.translatable("monitor.bettacraft.tile." + kind).getString();
    }

    private Component tankLabel(DashboardTile t) {
        if (t.tank == null) return Component.translatable(DashboardTile.LOG.equals(t.kind()) ? "monitor.bettacraft.all_tanks" : "gui.bettacraft.monitor.no_tank");
        for (MonitorTank k : tanks()) if (k.id.equals(t.tank)) return Component.literal(k.name.isEmpty() ? Component.translatable("monitor.bettacraft.unnamed").getString() : k.name);
        return Component.literal("?");
    }

    /** The tanks in the order of the automatic layout; the log can also show all tanks (null). */
    private void cycleTank(DashboardTile t) {
        List<UUID> ids = new ArrayList<>();
        if (DashboardTile.LOG.equals(t.kind())) ids.add(null);
        for (MonitorTank k : tanks()) ids.add(k.id);
        if (ids.isEmpty()) return;
        int i = ids.indexOf(t.tank);
        int step = hasShiftDown() ? ids.size() - 1 : 1;
        t.tank = ids.get(i < 0 ? 0 : (i + step) % ids.size());
    }

    private void setType(DashboardTile t, String type) {
        t.type = type;
        if (DashboardTile.GRAPH.equals(type) && !t.settings.contains("days")) {
            t.setHistory(List.of(t.value()));
            t.settings.putInt("days", 1);
        }
        if (DashboardTile.ALARM.equals(type) && !t.settings.contains("limit")) t.settings.putFloat("limit", defaultLimit(t.value()));
        if (!DashboardTile.LOG.equals(type) && t.tank == null && !tanks().isEmpty()) t.tank = tanks().get(0).id;
    }

    /** A sensible first limit for an alarm (about where the menu warns). */
    public static float defaultLimit(MonitorValue v) {
        return switch (v) {
            case TEMPERATURE -> 29F;
            case WATER_QUALITY -> 60F;
            case PH -> 8F;
            case NITRATE -> 50F;
            case FILL -> 90F;
            case FOOD -> 4F;
            case OXYGEN -> 4F;
            default -> 0F;
        };
    }

    private void cycleGraphValue(DashboardTile t, int slot) {
        List<MonitorValue> vs = new ArrayList<>(t.graphValues());
        List<MonitorValue> options = new ArrayList<>();
        if (slot > 0) options.add(null);
        for (MonitorValue v : MonitorValue.ALL) if (!vs.contains(v) || slot < vs.size() && vs.get(slot) == v) options.add(v);
        MonitorValue cur = slot < vs.size() ? vs.get(slot) : null;
        int i = options.indexOf(cur);
        MonitorValue nv = options.get((i + (hasShiftDown() ? options.size() - 1 : 1)) % options.size());
        if (slot < vs.size()) {
            if (nv == null) vs.remove(slot);
            else vs.set(slot, nv);
        } else if (nv != null) vs.add(nv);
        t.setHistory(vs);
    }

    private static <T> T next(T[] all, T cur, boolean back) {
        int i = 0;
        for (int k = 0; k < all.length; k++) if (all[k] == cur) i = k;
        return all[(i + (back ? all.length - 1 : 1)) % all.length];
    }

    private static int nextColour(int[] palette, int cur) {
        int i = 0;
        for (int k = 0; k < palette.length; k++) if (palette[k] == cur) i = k;
        return palette[(i + (hasShiftDown() ? palette.length - 1 : 1)) % palette.length];
    }

    private void resize(DashboardTile t, int dw, int dh) {
        int nw = Math.max(1, Math.min(gw - t.x, t.w + dw)), nh = Math.max(1, Math.min(gh - t.y, t.h + dh));
        if (layout.free(t.x, t.y, nw, nh, t.id)) {
            t.w = nw;
            t.h = nh;
        }
    }

    private void addTile() {
        if (layout == null) return;
        if (layout.tiles.size() >= DashboardLayout.MAX_TILES) {
            note = Component.translatable("gui.bettacraft.monitor.full").withStyle(ChatFormatting.GOLD);
            return;
        }
        int[][] sizes = {{4, 2}, {2, 2}, {2, 1}, {1, 1}};
        for (int[] sz : sizes) {
            int[] at = layout.freeSpot(Math.min(sz[0], gw), Math.min(sz[1], gh), gw, gh, -1);
            if (at == null) continue;
            DashboardTile cur = tile();
            UUID tank = cur != null && cur.tank != null ? cur.tank : tanks().isEmpty() ? null : tanks().get(0).id;
            DashboardTile t = DashboardTile.value(layout.nextId++, at[0], at[1], Math.min(sz[0], gw), Math.min(sz[1], gh), tank, MonitorValue.TEMPERATURE, false);
            layout.tiles.add(t);
            selected = t.id;
            note = Component.empty();
            changed();
            return;
        }
        note = Component.translatable("gui.bettacraft.monitor.full").withStyle(ChatFormatting.GOLD);
    }

    private void save() {
        if (layout == null) return;
        layout.auto = false;
        PacketDistributor.sendToServer(new MonitorLayoutPayload(pos, layout.save()));
        onClose();
    }

    private void sendAuto() {
        DashboardLayout l = new DashboardLayout();
        l.auto = true;
        PacketDistributor.sendToServer(new MonitorLayoutPayload(pos, l.save()));
        onClose();
    }

    private static String fmt(float f) {
        return f == Math.rint(f) ? Integer.toString((int) f) : String.format(Locale.ROOT, "%.1f", f);
    }

    // ------------------------------------------------------------------ mouse

    private int cellX(double mx) {
        return (int) Math.floor((mx - gx) / cell);
    }

    private int cellY(double my) {
        return (int) Math.floor((my - gy) / cell);
    }

    @Nullable
    private DashboardTile tileAt(int cx, int cy) {
        if (layout == null) return null;
        for (int i = layout.tiles.size() - 1; i >= 0; i--) {
            DashboardTile t = layout.tiles.get(i);
            if (cx >= t.x && cx < t.x + t.w && cy >= t.y && cy < t.y + t.h) return t;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0 || layout == null) return false;
        int cx = cellX(mx), cy = cellY(my);
        if (cx < 0 || cy < 0 || cx >= gw || cy >= gh) return false;
        DashboardTile t = tileAt(cx, cy);
        int before = selected;
        if (t == null) {
            selected = -1;
            drag = 0;
        } else {
            selected = t.id;
            double ex = gx + (t.x + t.w) * cell, ey = gy + (t.y + t.h) * cell;
            boolean corner = mx >= ex - Math.max(3, cell / 2) && my >= ey - Math.max(3, cell / 2);
            drag = corner ? 2 : 1;
            grabX = cx - t.x;
            grabY = cy - t.y;
        }
        if (before != selected) changed();
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        DashboardTile t = tile();
        if (drag == 0 || t == null || button != 0) return super.mouseDragged(mx, my, button, dx, dy);
        int cx = cellX(mx), cy = cellY(my);
        if (drag == 1) {
            int nx = Math.max(0, Math.min(gw - t.w, cx - grabX)), ny = Math.max(0, Math.min(gh - t.h, cy - grabY));
            if ((nx != t.x || ny != t.y) && layout.free(nx, ny, t.w, t.h, t.id)) {
                t.x = nx;
                t.y = ny;
            }
        } else {
            int nw = Math.max(1, Math.min(gw - t.x, cx - t.x + 1)), nh = Math.max(1, Math.min(gh - t.y, cy - t.y + 1));
            if ((nw != t.w || nh != t.h) && layout.free(t.x, t.y, nw, nh, t.id)) {
                t.w = nw;
                t.h = nh;
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        drag = 0;
        return super.mouseReleased(mx, my, button);
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (controller() == null || mc.player == null || mc.player.distanceToSqr(pos.getCenter()) > 64) onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        MonitorBlockEntity c = controller();
        if (c == null || layout == null) return;
        g.drawString(font, title, 12, 8, 0xFFFFFF, false);
        g.drawString(font, Component.translatable("gui.bettacraft.monitor.info", bw, bh, c.clientTanks.size()).withStyle(ChatFormatting.GRAY), 12, 18, 0xA0A0A0, false);
        // the grid
        g.fill(gx - 2, gy - 2, gx + gw * cell + 2, gy + gh * cell + 2, 0xFF05090B);
        for (int x = 0; x <= gw; x++) g.fill(gx + x * cell, gy, gx + x * cell + 1, gy + gh * cell, x % DashboardLayout.CELLS == 0 ? 0x60FFFFFF : 0x20FFFFFF);
        for (int y = 0; y <= gh; y++) g.fill(gx, gy + y * cell, gx + gw * cell, gy + y * cell + 1, y % DashboardLayout.CELLS == 0 ? 0x60FFFFFF : 0x20FFFFFF);
        for (DashboardTile t : layout.tiles) {
            int x0 = gx + t.x * cell + 1, y0 = gy + t.y * cell + 1, x1 = gx + (t.x + t.w) * cell, y1 = gy + (t.y + t.h) * cell;
            g.fill(x0, y0, x1, y1, t.background != 0 ? t.background : TILE);
            boolean sel = t.id == selected;
            g.renderOutline(x0, y0, x1 - x0, y1 - y0, sel ? 0xFFFFD040 : 0xFF6080A0);
            g.fill(x1 - 3, y1 - 3, x1, y1, sel ? 0xFFFFD040 : 0xFF6080A0); // resize handle
            label(g, t, x0 + 2, y0 + 2, x1 - x0 - 4, y1 - y0 - 4);
        }
        // the panel
        int px = width - PANEL_W - 8;
        if (tile() == null) {
            int y = 30;
            for (var line : font.split(Component.translatable("gui.bettacraft.monitor.help").withStyle(ChatFormatting.GRAY), PANEL_W)) {
                g.drawString(font, line, px, y, 0xA0A0A0, false);
                y += 10;
            }
        } else if (DashboardTile.GRAPH.equals(tile().kind())) {
            // the days between the -/+ buttons
            int y = 30 + ROW * 4;
            g.drawCenteredString(font, Component.translatable("monitor.bettacraft.days", tile().days()), px + PANEL_W / 2, y + 4, 0xFFFFFF);
        }
        g.drawString(font, note, 12 + 2 * (Math.min(90, (width - 24) / 4 - 4) + 4), height - 18, 0xFFFFFF, false);
    }

    /** The type, tank and value of a tile in the grid, small. */
    private void label(GuiGraphics g, DashboardTile t, int x, int y, int w, int h) {
        if (w < 4 || h < 3) return;
        List<String> lines = new ArrayList<>();
        lines.add(typeName(t.kind()));
        lines.add(tankLabel(t).getString());
        String k = t.kind();
        if (DashboardTile.VALUE.equals(k) || DashboardTile.ALARM.equals(k)) lines.add(Component.translatable(t.value().translationKey()).getString());
        if (DashboardTile.GRAPH.equals(k)) {
            StringBuilder b = new StringBuilder();
            for (MonitorValue v : t.graphValues()) b.append(b.length() == 0 ? "" : ", ").append(Component.translatable(v.translationKey()).getString());
            lines.add(b.toString());
        }
        float scale = Math.max(0.5F, Math.min(1F, h / (lines.size() * 9F)));
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1F);
        int maxW = (int) (w / scale);
        int ly = 0;
        for (String s : lines) {
            if ((ly + 8) * scale > h) break;
            String cut = font.width(s) > maxW ? font.plainSubstrByWidth(s, Math.max(0, maxW - font.width("..."))) + "..." : s;
            g.drawString(font, cut, 0, ly, ly == 0 ? 0xFFFFFF : 0xB0C8D0, false);
            ly += 9;
        }
        g.pose().popPose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(4, 4, width - 4, height - 4, 0xE0101418);
        g.renderOutline(4, 4, width - 8, height - 8, 0xFF6080A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** For the dev smoke test: the layout being edited. */
    @Nullable
    public DashboardLayout editing() {
        return layout;
    }

    /** For the dev smoke test: pick a tile. */
    public void select(int id) {
        selected = id;
        changed();
    }
}
