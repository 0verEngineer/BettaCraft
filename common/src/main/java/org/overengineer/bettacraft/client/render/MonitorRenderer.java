package org.overengineer.bettacraft.client.render;

import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.overengineer.bettacraft.monitor.MonitorDetail;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.monitor.DashboardLayout;
import org.overengineer.bettacraft.monitor.DashboardTile;
import org.overengineer.bettacraft.monitor.MonitorBlockEntity;
import org.overengineer.bettacraft.monitor.MonitorSync;
import org.overengineer.bettacraft.monitor.MonitorTank;
import org.overengineer.bettacraft.monitor.MonitorValue;
import org.overengineer.bettacraft.network.MonitorDataPayload;

/**
 * M27: draws the screen of a monitor (in its controller, the top left block) over the front faces of all its blocks:
 * the dark background, the tiles of the layout on its grid ({@link DashboardLayout#CELLS} cells per block) and their
 * widgets ({@link #tile}: value, traffic light, graph, animals, devices, alarm, log). The screen glows (full light) while
 * it is on.
 * <p>
 * Coordinates: "screen pixels" of {@link #PX} per block, x to the right and y down from the top left corner inside the
 * frame, drawn {@link #LIFT} in front of the glass.
 */
public class MonitorRenderer extends org.overengineer.bettacraft.client.render.LegacyBlockEntityRenderer<MonitorBlockEntity> {
    public static final float PX = 64F;
    /** The frame around a screen (blocks). */
    public static final float BEZEL = 1F / 16F;
    /** The glass lies this far behind the front of the block (the frame sticks out). */
    public static final float RECESS = 1F / 16F;
    private static final float LIFT = 0.002F;
    private static final int SCREEN = 0xFF071014, TILE = 0xFF12303A, TEXT = 0xFFE6F2F5, DIM = 0xFF7F9AA2, WARN = 0xFFE8B04A,
            BAD = 0xFFE05A4A, SPARK = 0x9948C8E0, GOOD = 0xFF5CCB6A, ALARM_BG = 0xFF8A1E1E, GRAPH_BG = 0xFF0B1F26, GRID = 0x40FFFFFF;
    /** The colours of the lines of a graph. */
    private static final int[] LINES = {0xFF48C8E0, 0xFFE8B04A, 0xFF9BE06A};

    private final Font font;

    public MonitorRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.font();
    }

    /** The bundled data from the server (one entry per screen). */
    public static void accept(MonitorDataPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        for (MonitorDataPayload.Entry e : payload.entries()) {
            if (level.isLoaded(e.pos()) && level.getBlockEntity(e.pos()) instanceof MonitorBlockEntity be) be.acceptClient(e, level.getGameTime());
        }
    }

    @Override
    public void render(MonitorBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!be.isController() || be.getLevel() == null) return;
        Direction facing = be.facing();
        int w = be.width(), h = be.height();
        boolean on = be.status() == MonitorBlockEntity.ON;
        int packed = on ? net.minecraft.util.LightCoordsUtil.FULL_BRIGHT : LevelRenderer.getLightCoords(be.getLevel(), be.getBlockPos().relative(facing));
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        // local +Z = out of the screen, +X = right as the viewer sees it, +Y = up; this block is the top left one
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        pose.translate(-0.5F + BEZEL, 0.5F - BEZEL, 0.5F - RECESS + LIFT);
        pose.scale(1F / PX, -1F / PX, 1F / PX);
        float sw = w * PX - 2 * BEZEL * PX, sh = h * PX - 2 * BEZEL * PX;
        quad(buffers, pose.last().pose(), 0, 0, sw, sh, 0F, SCREEN, packed);
        switch (be.status()) {
            case MonitorBlockEntity.NO_POWER -> centered(pose, buffers, I18n.get("monitor.bettacraft.status.no_power"), sw, sh, BAD, packed);
            case MonitorBlockEntity.OFF -> centered(pose, buffers, I18n.get("monitor.bettacraft.status.off"), sw, sh, DIM, packed);
            default -> {
                DashboardLayout layout = be.layout();
                if (layout.tiles.isEmpty()) {
                    centered(pose, buffers, I18n.get(be.clientDataTime == Long.MIN_VALUE ? "monitor.bettacraft.waiting" : "monitor.bettacraft.no_tanks"),
                            sw, sh, DIM, packed);
                } else {
                    float cw = sw / (w * DashboardLayout.CELLS), ch = sh / (h * DashboardLayout.CELLS);
                    for (DashboardTile t : layout.tiles) {
                        float x0 = t.x * cw + 1, y0 = t.y * ch + 1, x1 = (t.x + t.w) * cw - 1, y1 = (t.y + t.h) * ch - 1;
                        if (x1 - x0 < 2 || y1 - y0 < 2) continue;
                        tile(be, t, pose, buffers, x0, y0, x1, y1, packed);
                    }
                }
            }
        }
        pose.popPose();
    }

    /** One tile: its background, then the widget of its type. */
    private void tile(MonitorBlockEntity be, DashboardTile t, PoseStack pose, MultiBufferSource buffers,
                      float x0, float y0, float x1, float y1, int light) {
        int bg = t.background != 0 ? t.background : TILE;
        // an alarm blinks while it is past its limit
        if (DashboardTile.ALARM.equals(t.kind()) && t.triggered(be.clientTank(t.tank)) && (be.getLevel().getGameTime() / 10) % 2 == 0) bg = ALARM_BG;
        quad(buffers, pose.last().pose(), x0, y0, x1, y1, 0.25F, bg, light);
        switch (t.kind()) {
            case DashboardTile.STATUS -> statusTile(be, t, pose, buffers, x0, y0, x1, y1, light);
            case DashboardTile.GRAPH -> graphTile(be, t, pose, buffers, x0, y0, x1, y1, light);
            case DashboardTile.ANIMALS -> animalsTile(be, t, pose, buffers, x0, y0, x1, y1, light);
            case DashboardTile.DEVICES -> devicesTile(be, t, pose, buffers, x0, y0, x1, y1, light);
            case DashboardTile.ALARM -> alarmTile(be, t, pose, buffers, x0, y0, x1, y1, light);
            case DashboardTile.LOG -> logTile(be, t, pose, buffers, x0, y0, x1, y1, light);
            default -> valueTile(be, t, pose, buffers, x0, y0, x1, y1, light);
        }
    }

    private static float pad(float tw, float th) {
        return Math.max(1F, Math.min(tw, th) * 0.06F);
    }

    private static float headH(float th) {
        return Math.max(3F, Math.min(th * 0.13F, 7F));
    }

    private static String tankName(@Nullable MonitorTank tank) {
        return tank == null ? "?" : tank.name.isEmpty() ? I18n.get("monitor.bettacraft.unnamed") : tank.name;
    }

    /** The head line of a tile ("Tank · what"); smaller when it is too long, then cut. Returns the y below it. */
    private float head(String s, PoseStack pose, MultiBufferSource buffers, float x0, float y0, float x1, float th, int light) {
        float tw = x1 - x0, pad = pad(tw, th), h = headH(th);
        float need = font.width(s) * h / 9F;
        if (need > tw - 2 * pad) h = Math.max(h * 0.55F, h * (tw - 2 * pad) / need);
        text(pose, buffers, s, x0 + pad, y0 + pad, tw - 2 * pad, h, DIM, light, false);
        return y0 + pad + headH(th) + pad * 0.5F;
    }

    /** Text rows below the head: the height of one row for a tile of height {@code th}. */
    private static float rowH(float th) {
        return Math.max(3.5F, Math.min(th / 10F, 6F));
    }

    /** "not loaded" / "no data" / "waiting" for a tile without data; true if nothing more can be drawn. */
    private boolean noData(@Nullable MonitorTank tank, boolean needDetail, PoseStack pose, MultiBufferSource buffers, float x, float y, float w, float h, int light) {
        if (tank != null && tank.live && !(needDetail && tank.detail.isEmpty())) return false;
        if (tank != null && !tank.live && !tank.detail.isEmpty()) return false; // the last data, the head says nothing more
        String key = tank == null ? "monitor.bettacraft.no_data" : !tank.live ? "monitor.bettacraft.not_loaded" : "monitor.bettacraft.waiting";
        text(pose, buffers, I18n.get(key), x, y, w, h, WARN, light, false);
        return true;
    }

    /** A value tile: the tank's name and the value's name on top, the value big, "not loaded" or the graph of the last day below. */
    private void valueTile(MonitorBlockEntity be, DashboardTile t, PoseStack pose, MultiBufferSource buffers,
                           float x0, float y0, float x1, float y1, int light) {
        MonitorValue v = t.value();
        MonitorTank tank = be.clientTank(t.tank);
        float tw = x1 - x0, th = y1 - y0, pad = pad(tw, th);
        int fg = t.foreground != 0 ? t.foreground : TEXT;
        float headH = headH(th);
        float valueTop = head(tankName(tank) + " · " + I18n.get(v.translationKey()), pose, buffers, x0, y0, x1, th, light);
        float value = tank == null ? Float.NaN : tank.value(v);
        boolean live = tank != null && tank.live;
        float valueH = th * 0.40F;
        text(pose, buffers, v.format(value), x0 + pad, valueTop, tw - 2 * pad, valueH, live ? fg : DIM, light, true);
        float bottom = valueTop + valueH + pad * 0.5F, bottomH = y1 - pad - bottom;
        if (bottomH < 2) return;
        if (tank == null || !tank.live) {
            text(pose, buffers, I18n.get(tank == null ? "monitor.bettacraft.no_data" : "monitor.bettacraft.not_loaded"), x0 + pad, bottom,
                    tw - 2 * pad, Math.min(bottomH, headH), WARN, light, false);
            return;
        }
        if (t.spark()) {
            MonitorDataPayload.HistoryLine line = be.clientHistory(t.tank, v);
            if (line != null) spark(buffers, pose.last().pose(), line.points(), 24, x0 + pad, bottom, x1 - pad, y1 - pad, light);
        }
    }

    /** Traffic light: a lamp in the colour of the worst warning, the state and the warnings of the tank's menu. */
    private void statusTile(MonitorBlockEntity be, DashboardTile t, PoseStack pose, MultiBufferSource buffers,
                            float x0, float y0, float x1, float y1, int light) {
        MonitorTank tank = be.clientTank(t.tank);
        float tw = x1 - x0, th = y1 - y0, pad = pad(tw, th), rh = rowH(th);
        float top = head(tankName(tank) + " · " + I18n.get("monitor.bettacraft.tile.status"), pose, buffers, x0, y0, x1, th, light);
        if (noData(tank, true, pose, buffers, x0 + pad, top, tw - 2 * pad, headH(th), light)) return;
        int state = MonitorDetail.state(tank.detail);
        int col = !tank.live ? DIM : state == MonitorDetail.ALARM ? BAD : state == MonitorDetail.WARNING ? WARN : GOOD;
        float lamp = Math.min(Math.min(y1 - pad - top, tw * 0.3F), 3 * rh);
        lamp(buffers, pose.last().pose(), x0 + pad, top, lamp, col, light);
        float tx = x0 + pad * 2 + lamp, txw = x1 - pad - tx;
        String stateKey = !tank.live ? "not_loaded" : state == MonitorDetail.ALARM ? "alarm" : state == MonitorDetail.WARNING ? "warning" : "ok";
        float sh = Math.min(lamp * 0.6F, rh * 1.6F);
        text(pose, buffers, I18n.get("monitor.bettacraft.state." + stateKey), tx, top + (lamp - sh) / 2, txw, sh, col, light, true);
        float y = top + lamp + pad * 0.5F;
        ListTag warn = tank.detail.getListOrEmpty("warn");
        for (int i = 0; i < warn.size(); i++) {
            if (y + rh > y1 - pad + 0.01F) break;
            if (y + 2 * rh > y1 - pad + 0.01F && warn.size() - i > 1) {
                text(pose, buffers, I18n.get("monitor.bettacraft.more", warn.size() - i), x0 + pad, y, tw - 2 * pad, rh, DIM, light, false);
                break;
            }
            CompoundTag w = warn.getCompoundOrEmpty(i);
            text(pose, buffers, "! " + translate(w), x0 + pad, y, tw - 2 * pad, rh, w.getIntOr("s", 0) == MonitorDetail.ALARM ? BAD : WARN, light, false);
            y += rh;
        }
    }

    /** History graph: up to three values over 1-7 days, each scaled to its own range, the latest value in the legend. */
    private void graphTile(MonitorBlockEntity be, DashboardTile t, PoseStack pose, MultiBufferSource buffers,
                           float x0, float y0, float x1, float y1, int light) {
        MonitorTank tank = be.clientTank(t.tank);
        List<MonitorValue> values = t.graphValues();
        float tw = x1 - x0, th = y1 - y0, pad = pad(tw, th);
        StringBuilder names = new StringBuilder();
        for (MonitorValue v : values) names.append(names.length() == 0 ? "" : " / ").append(I18n.get(v.translationKey()));
        String what = names.toString();
        float top = head(tankName(tank) + " · " + what + " · " + I18n.get("monitor.bettacraft.days", t.days()), pose, buffers, x0, y0, x1, th, light);
        if (tank == null) {
            noData(null, false, pose, buffers, x0 + pad, top, tw - 2 * pad, headH(th), light);
            return;
        }
        float rh = rowH(th) * 0.9F;
        // legend: one row with the latest value of each line in its colour
        float lx = x0 + pad, legendW = (tw - 2 * pad) / values.size();
        for (int i = 0; i < values.size(); i++) {
            MonitorValue v = values.get(i);
            String s = v.format(tank.value(v)); // the names are in the head, the colour tells the line
            text(pose, buffers, s, lx + i * legendW, top, legendW - pad, rh, tank.live ? LINES[i] : DIM, light, false);
        }
        float gy0 = top + rh + pad * 0.5F, gy1 = y1 - pad, gx0 = x0 + pad, gx1 = x1 - pad;
        if (gy1 - gy0 < 3) return;
        Matrix4f m = pose.last().pose();
        quad(buffers, m, gx0, gy0, gx1, gy1, 0.4F, GRAPH_BG, light);
        int hours = t.days() * 24;
        VertexConsumer vc = buffers.getBuffer(RenderTypes.textBackground());
        // a line at every day
        for (int d = 1; d < t.days(); d++) {
            float x = gx1 - (gx1 - gx0) * d * 24F / (hours - 1);
            quad(vc, m, x, gy0, x + 0.4F, gy1, 0.45F, GRID, light);
        }
        float thick = Math.max(0.5F, Math.min(1.2F, (gy1 - gy0) / 30F));
        boolean any = false;
        for (int i = 0; i < values.size(); i++) {
            MonitorDataPayload.HistoryLine line = be.clientHistory(t.tank, values.get(i));
            if (line == null) continue;
            any |= polyline(vc, m, line.points(), hours, gx0, gy0 + thick, gx1, gy1 - thick, thick, 0.5F + i * 0.02F, LINES[i], light);
        }
        if (!any) text(pose, buffers, I18n.get("monitor.bettacraft.no_history"), gx0 + pad, gy0 + pad, gx1 - gx0 - 2 * pad, rh, DIM, light, false);
    }

    /** Animal list: count per species (young in brackets), then nest, eggs and young. */
    private void animalsTile(MonitorBlockEntity be, DashboardTile t, PoseStack pose, MultiBufferSource buffers,
                             float x0, float y0, float x1, float y1, int light) {
        MonitorTank tank = be.clientTank(t.tank);
        float tw = x1 - x0, th = y1 - y0, pad = pad(tw, th), rh = rowH(th);
        float y = head(tankName(tank) + " · " + I18n.get("monitor.bettacraft.tile.animals"), pose, buffers, x0, y0, x1, th, light);
        if (noData(tank, true, pose, buffers, x0 + pad, y, tw - 2 * pad, headH(th), light)) return;
        int fg = t.foreground != 0 ? t.foreground : TEXT;
        CompoundTag d = tank.detail;
        List<String> breed = new ArrayList<>();
        String nest = d.getStringOr("nest", "");
        if (!nest.isEmpty()) breed.add(I18n.get("monitor.bettacraft.nest." + nest));
        if (d.getIntOr("eggs", 0) > 0) breed.add(I18n.get("monitor.bettacraft.eggs", d.getIntOr("eggs", 0)));
        if (d.getIntOr("young", 0) > 0) breed.add(I18n.get("monitor.bettacraft.young", d.getIntOr("young", 0)));
        if (d.getIntOr("berried", 0) > 0) breed.add(I18n.get("monitor.bettacraft.berried", d.getIntOr("berried", 0)));
        int rows = (int) ((y1 - pad - y) / rh + 0.01F);
        ListTag al = d.getListOrEmpty("animals");
        int room = rows - (breed.isEmpty() ? 0 : 1);
        if (al.isEmpty() && room > 0) {
            text(pose, buffers, I18n.get("monitor.bettacraft.no_animals"), x0 + pad, y, tw - 2 * pad, rh, DIM, light, false);
            y += rh;
        }
        for (int i = 0; i < al.size() && room > 0; i++) {
            if (room == 1 && al.size() - i > 1) {
                text(pose, buffers, I18n.get("monitor.bettacraft.more", al.size() - i), x0 + pad, y, tw - 2 * pad, rh, DIM, light, false);
                y += rh;
                break;
            }
            CompoundTag a = al.getCompoundOrEmpty(i);
            String n = a.getIntOr("n", 0) + "x " + I18n.get(a.getStringOr("k", "")) + (a.getIntOr("y", 0) > 0 ? " " + I18n.get("monitor.bettacraft.young_short", a.getIntOr("y", 0)) : "");
            text(pose, buffers, n, x0 + pad, y, tw - 2 * pad, rh, tank.live ? fg : DIM, light, false);
            y += rh;
            room--;
        }
        if (!breed.isEmpty() && y + rh <= y1 - pad + 0.01F) text(pose, buffers, String.join(" · ", breed), x0 + pad, y, tw - 2 * pad, rh, WARN, light, false);
    }

    /** Device status: a dot (green on, grey off, red without power), the kind, running / count and power level; the energy. */
    private void devicesTile(MonitorBlockEntity be, DashboardTile t, PoseStack pose, MultiBufferSource buffers,
                             float x0, float y0, float x1, float y1, int light) {
        MonitorTank tank = be.clientTank(t.tank);
        float tw = x1 - x0, th = y1 - y0, pad = pad(tw, th), rh = rowH(th);
        float y = head(tankName(tank) + " · " + I18n.get("monitor.bettacraft.tile.devices"), pose, buffers, x0, y0, x1, th, light);
        if (noData(tank, true, pose, buffers, x0 + pad, y, tw - 2 * pad, headH(th), light)) return;
        int fg = t.foreground != 0 ? t.foreground : TEXT;
        CompoundTag d = tank.detail;
        ListTag dl = d.getListOrEmpty("dev");
        boolean energy = d.getLongOr("ec", 0L) > 0;
        int rows = (int) ((y1 - pad - y) / rh + 0.01F) - (energy ? 1 : 0);
        for (int i = 0; i < dl.size() && i < rows; i++) {
            CompoundTag k = dl.getCompoundOrEmpty(i);
            int n = k.getIntOr("n", 0), on = k.getIntOr("on", 0), p = k.getIntOr("p", 0);
            boolean powered = k.getBooleanOr("pw", false);
            int dot = n == 0 ? DIM : !powered ? BAD : on > 0 ? GOOD : DIM;
            quad(buffers, pose.last().pose(), x0 + pad, y + rh * 0.2F, x0 + pad + rh * 0.6F, y + rh * 0.8F, 0.5F, dot, light);
            String s = I18n.get("monitor.bettacraft.device." + k.getStringOr("k", "")) + ": "
                    + (n == 0 ? I18n.get("monitor.bettacraft.none") : !powered ? I18n.get("monitor.bettacraft.status.no_power")
                    : on + "/" + n + (p >= 0 && on > 0 ? " · " + p + " %" : ""));
            text(pose, buffers, s, x0 + pad + rh, y, tw - 2 * pad - rh, rh, n == 0 || !tank.live ? DIM : fg, light, false);
            y += rh;
        }
        if (energy && y + rh <= y1 - pad + 0.01F) {
            text(pose, buffers, I18n.get("monitor.bettacraft.energy", d.getLongOr("e", 0L), d.getLongOr("ec", 0L), d.getIntOr("ed", 0)), x0 + pad, y, tw - 2 * pad, rh,
                    DIM, light, false);
        }
    }

    /** Alarm: the value big, the limit below; the tile blinks red while the value is past it ({@link #tile}). */
    private void alarmTile(MonitorBlockEntity be, DashboardTile t, PoseStack pose, MultiBufferSource buffers,
                           float x0, float y0, float x1, float y1, int light) {
        MonitorValue v = t.value();
        MonitorTank tank = be.clientTank(t.tank);
        float tw = x1 - x0, th = y1 - y0, pad = pad(tw, th), rh = rowH(th);
        boolean on = t.triggered(tank);
        float top = head((on ? I18n.get("monitor.bettacraft.alarm") + " · " : "") + tankName(tank) + " · " + I18n.get(v.translationKey()), pose, buffers,
                x0, y0, x1, th, light);
        int fg = t.foreground != 0 ? t.foreground : TEXT;
        float value = tank == null ? Float.NaN : tank.value(v);
        float valueH = Math.max(3F, Math.min(th * 0.40F, y1 - pad - top - rh));
        text(pose, buffers, v.format(value), x0 + pad, top, tw - 2 * pad, valueH, on ? 0xFFFFFFFF : tank != null && tank.live ? fg : DIM, light, true);
        float y = top + valueH + pad * 0.5F;
        if (y + rh > y1 - pad + 0.01F) return;
        String limit = I18n.get(t.below() ? "monitor.bettacraft.below" : "monitor.bettacraft.above", v.format(t.limit()));
        if (t.redstone()) limit += " · " + I18n.get("monitor.bettacraft.redstone");
        if (tank == null || !tank.live) limit = I18n.get(tank == null ? "monitor.bettacraft.no_data" : "monitor.bettacraft.not_loaded");
        text(pose, buffers, limit, x0 + pad, y, tw - 2 * pad, rh, on ? 0xFFFFFFFF : WARN, light, false);
    }

    private record LogRow(long time, String tank, CompoundTag entry) {}

    /** Event log: the latest logbook entries of the tank, or of every tank of the screen (tank null), newest on top. */
    private void logTile(MonitorBlockEntity be, DashboardTile t, PoseStack pose, MultiBufferSource buffers,
                         float x0, float y0, float x1, float y1, int light) {
        MonitorTank only = be.clientTank(t.tank);
        float tw = x1 - x0, th = y1 - y0, pad = pad(tw, th), rh = rowH(th);
        String who = t.tank == null ? I18n.get("monitor.bettacraft.all_tanks") : tankName(only);
        float y = head(who + " · " + I18n.get("monitor.bettacraft.tile.log"), pose, buffers, x0, y0, x1, th, light);
        if (t.tank != null && noData(only, true, pose, buffers, x0 + pad, y, tw - 2 * pad, headH(th), light)) return;
        List<LogRow> lines = new ArrayList<>();
        boolean anyDetail = false;
        for (MonitorTank k : be.clientTanks) {
            if (t.tank != null && !k.id.equals(t.tank)) continue;
            anyDetail |= !k.detail.isEmpty();
            ListTag lg = k.detail.getListOrEmpty("log");
            for (int i = 0; i < lg.size(); i++) lines.add(new LogRow(lg.getCompoundOrEmpty(i).getLongOr("t", 0L), tankName(k), lg.getCompoundOrEmpty(i)));
        }
        lines.sort((a, b) -> Long.compare(b.time(), a.time()));
        int fg = t.foreground != 0 ? t.foreground : TEXT;
        if (lines.isEmpty()) {
            text(pose, buffers, I18n.get(anyDetail ? "monitor.bettacraft.log_empty" : "monitor.bettacraft.waiting"), x0 + pad, y, tw - 2 * pad, rh,
                    DIM, light, false);
            return;
        }
        for (LogRow l : lines) {
            if (y + rh > y1 - pad + 0.01F) break;
            Object[] args = MonitorDetail.args(l.entry()).stream().map(TankCare::argComponent).toArray();
            String day = I18n.get("monitor.bettacraft.day", l.time() / 24000L + 1);
            String msg = Component.translatable(l.entry().getStringOr("k", ""), args).getString();
            if (t.tank == null) {
                // several tanks: day and tank in a dim row, the entry below it
                if (y + 2 * rh > y1 - pad + 0.01F) break;
                text(pose, buffers, day + " " + l.tank(), x0 + pad, y, tw - 2 * pad, rh, DIM, light, false);
                y += rh;
                text(pose, buffers, msg, x0 + pad + rh * 0.5F, y, tw - 2 * pad - rh * 0.5F, rh, fg, light, false);
            } else {
                text(pose, buffers, day + " " + msg, x0 + pad, y, tw - 2 * pad, rh, fg, light, false);
            }
            y += rh;
        }
    }

    private static String translate(CompoundTag w) {
        return Component.translatable(w.getStringOr("k", ""), MonitorDetail.args(w).toArray()).getString();
    }

    /** A round lamp of {@code size} (a cross of three rectangles), top left at x, y. */
    private static void lamp(MultiBufferSource buffers, Matrix4f m, float x, float y, float size, int argb, int light) {
        VertexConsumer vc = buffers.getBuffer(RenderTypes.textBackground());
        float a = size * 0.3F, b = size * 0.12F;
        quad(vc, m, x + a, y, x + size - a, y + size, 0.5F, argb, light);
        quad(vc, m, x, y + a, x + size, y + size - a, 0.5F, argb, light);
        quad(vc, m, x + b, y + b, x + size - b, y + size - b, 0.5F, argb, light);
    }

    /** The last {@code hours} points as a line, scaled between their lowest and highest value; false if there were none. */
    private static boolean polyline(VertexConsumer vc, Matrix4f m, float[] points, int hours, float x0, float y0, float x1, float y1,
                                    float thick, float z, int argb, int light) {
        int n = Math.min(hours, points.length);
        if (n < 1) return false;
        float lo = Float.POSITIVE_INFINITY, hi = Float.NEGATIVE_INFINITY;
        for (int i = points.length - n; i < points.length; i++) {
            if (!Float.isFinite(points[i])) continue;
            lo = Math.min(lo, points[i]);
            hi = Math.max(hi, points[i]);
        }
        if (lo > hi) return false;
        if (hi - lo < 1e-3F) {
            lo -= 0.5F;
            hi += 0.5F;
        }
        float step = (x1 - x0) / Math.max(1, hours - 1);
        float px = Float.NaN, py = Float.NaN;
        for (int i = 0; i < n; i++) {
            float p = points[points.length - n + i];
            float x = x1 - (n - 1 - i) * step;
            if (!Float.isFinite(p)) {
                px = Float.NaN;
                continue;
            }
            float y = y1 - (p - lo) / (hi - lo) * (y1 - y0);
            if (Float.isFinite(px)) segment(vc, m, px, py, x, y, thick, z, argb, light);
            else quad(vc, m, x - thick / 2, y - thick / 2, x + thick / 2, y + thick / 2, z, argb, light);
            px = x;
            py = y;
        }
        return true;
    }

    /** A line of {@code thick} pixels from (ax, ay) to (bx, by), both windings. */
    private static void segment(VertexConsumer vc, Matrix4f m, float ax, float ay, float bx, float by, float thick, float z, int argb, int light) {
        float dx = bx - ax, dy = by - ay, len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-4F) return;
        float nx = -dy / len * thick / 2, ny = dx / len * thick / 2;
        vc.addVertex(m, ax + nx, ay + ny, z).setColor(argb).setLight(light);
        vc.addVertex(m, ax - nx, ay - ny, z).setColor(argb).setLight(light);
        vc.addVertex(m, bx - nx, by - ny, z).setColor(argb).setLight(light);
        vc.addVertex(m, bx + nx, by + ny, z).setColor(argb).setLight(light);
        vc.addVertex(m, bx + nx, by + ny, z).setColor(argb).setLight(light);
        vc.addVertex(m, bx - nx, by - ny, z).setColor(argb).setLight(light);
        vc.addVertex(m, ax - nx, ay - ny, z).setColor(argb).setLight(light);
        vc.addVertex(m, ax + nx, ay + ny, z).setColor(argb).setLight(light);
    }

    /** Bars of the last {@code hours} points, scaled between their lowest and highest value. */
    private static void spark(MultiBufferSource buffers, Matrix4f m, float[] points, int hours, float x0, float y0, float x1, float y1, int light) {
        int n = Math.min(hours, points.length);
        if (n < 2) return;
        float lo = Float.POSITIVE_INFINITY, hi = Float.NEGATIVE_INFINITY;
        for (int i = points.length - n; i < points.length; i++) {
            if (!Float.isFinite(points[i])) continue;
            lo = Math.min(lo, points[i]);
            hi = Math.max(hi, points[i]);
        }
        if (lo > hi) return;
        if (hi - lo < 1e-3F) {
            lo -= 0.5F;
            hi += 0.5F;
        }
        float bw = (x1 - x0) / n;
        VertexConsumer vc = buffers.getBuffer(RenderTypes.textBackground());
        for (int i = 0; i < n; i++) {
            float p = points[points.length - n + i];
            if (!Float.isFinite(p)) continue;
            float f = 0.15F + 0.85F * (p - lo) / (hi - lo);
            float top = y1 - f * (y1 - y0);
            quad(vc, m, x0 + i * bw, top, x0 + (i + 1) * bw - Math.min(0.5F, bw * 0.2F), y1, 0.5F, SPARK, light);
        }
    }

    private void centered(PoseStack pose, MultiBufferSource buffers, String s, float sw, float sh, int color, int light) {
        float hgt = Math.min(sh * 0.3F, 12F);
        float scale = Math.min(hgt / 8F, (sw * 0.9F) / Math.max(1, font.width(s)));
        pose.pushPose();
        pose.translate(sw / 2 - font.width(s) * scale / 2, sh / 2 - 4 * scale, 0.75F);
        pose.scale(scale, scale, 1F);
        font.drawInBatch(s, 0, 0, color, false, pose.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, 0, light);
        pose.popPose();
    }

    /** Text in a box: as big as fits ({@code fill}) or at most the box height, cut with "..." when too long. */
    private void text(PoseStack pose, MultiBufferSource buffers, String s, float x, float y, float maxW, float maxH, int color, int light, boolean fill) {
        if (maxW <= 1 || maxH <= 1) return;
        float scale = maxH / 9F;
        int width = font.width(s);
        if (fill) scale = Math.min(scale, maxW / Math.max(1, width));
        else if (width * scale > maxW) {
            String cut = font.plainSubstrByWidth(s, (int) (maxW / scale) - font.width("..."));
            s = cut + "...";
        }
        pose.pushPose();
        pose.translate(x, y, 0.75F);
        pose.scale(scale, scale, 1F);
        font.drawInBatch(s, 0, 0, color, false, pose.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, 0, light);
        pose.popPose();
    }

    /** A rectangle in screen pixels (takes the buffer itself: text in between ends the batch of an older one). */
    static void quad(MultiBufferSource buffers, Matrix4f m, float x0, float y0, float x1, float y1, float z, int argb, int light) {
        quad(buffers.getBuffer(RenderTypes.textBackground()), m, x0, y0, x1, y1, z, argb, light);
    }

    /** A rectangle in screen pixels, both windings (seen from the front whatever the transform does). */
    static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float z, int argb, int light) {
        vc.addVertex(m, x0, y0, z).setColor(argb).setLight(light);
        vc.addVertex(m, x0, y1, z).setColor(argb).setLight(light);
        vc.addVertex(m, x1, y1, z).setColor(argb).setLight(light);
        vc.addVertex(m, x1, y0, z).setColor(argb).setLight(light);
        vc.addVertex(m, x1, y0, z).setColor(argb).setLight(light);
        vc.addVertex(m, x1, y1, z).setColor(argb).setLight(light);
        vc.addVertex(m, x0, y1, z).setColor(argb).setLight(light);
        vc.addVertex(m, x0, y0, z).setColor(argb).setLight(light);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    public AABB getRenderBoundingBox(MonitorBlockEntity be) {
        return be.isController() ? be.screenBox().inflate(0.1) : new net.minecraft.world.phys.AABB(be.getBlockPos());
    }

    @Override
    public int getViewDistance() {
        return MonitorSync.VIEW_DISTANCE;
    }
}
