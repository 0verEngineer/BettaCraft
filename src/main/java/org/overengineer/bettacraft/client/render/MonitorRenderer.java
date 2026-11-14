package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
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
 * widgets. The screen glows (full light) while it is on. Part a draws the value tile ({@link #valueTile}); part b adds the
 * other widgets in {@link #tile}.
 * <p>
 * Coordinates: "screen pixels" of {@link #PX} per block, x to the right and y down from the top left corner inside the
 * frame, drawn {@link #LIFT} in front of the glass.
 */
public class MonitorRenderer implements BlockEntityRenderer<MonitorBlockEntity> {
    public static final float PX = 64F;
    /** The frame around a screen (blocks). */
    public static final float BEZEL = 1F / 16F;
    /** The glass lies this far behind the front of the block (the frame sticks out). */
    public static final float RECESS = 1F / 16F;
    private static final float LIFT = 0.002F;
    private static final int SCREEN = 0xFF071014, TILE = 0xFF12303A, TEXT = 0xFFE6F2F5, DIM = 0xFF7F9AA2, WARN = 0xFFE8B04A,
            BAD = 0xFFE05A4A, SPARK = 0x9948C8E0;

    private final Font font;

    public MonitorRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.getFont();
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
        int packed = on ? LightTexture.FULL_BRIGHT : LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(facing));
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

    /** One tile (part b: a switch over {@link DashboardTile#type}). */
    private void tile(MonitorBlockEntity be, DashboardTile t, PoseStack pose, MultiBufferSource buffers,
                      float x0, float y0, float x1, float y1, int light) {
        quad(buffers, pose.last().pose(), x0, y0, x1, y1, 0.25F, t.background != 0 ? t.background : TILE, light);
        valueTile(be, t, pose, buffers, x0, y0, x1, y1, light);
    }

    /** A value tile: the tank's name and the value's name on top, the value big, "not loaded" or the graph of the last day below. */
    private void valueTile(MonitorBlockEntity be, DashboardTile t, PoseStack pose, MultiBufferSource buffers,
                           float x0, float y0, float x1, float y1, int light) {
        MonitorValue v = t.value();
        MonitorTank tank = be.clientTank(t.tank);
        float tw = x1 - x0, th = y1 - y0, pad = Math.max(1F, Math.min(tw, th) * 0.06F);
        int fg = t.foreground != 0 ? t.foreground : TEXT;
        String name = tank == null ? "?" : tank.name.isEmpty() ? I18n.get("monitor.bettacraft.unnamed") : tank.name;
        String head = name + " · " + I18n.get(v.translationKey());
        float headH = Math.max(3F, Math.min(th * 0.17F, 9F));
        text(pose, buffers, head, x0 + pad, y0 + pad, tw - 2 * pad, headH, DIM, light, false);
        float value = tank == null ? Float.NaN : tank.value(v);
        boolean live = tank != null && tank.live;
        float valueTop = y0 + pad + headH + pad * 0.5F, valueH = th * 0.40F;
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
        VertexConsumer vc = buffers.getBuffer(RenderType.textBackground());
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
        quad(buffers.getBuffer(RenderType.textBackground()), m, x0, y0, x1, y1, z, argb, light);
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
    public boolean shouldRenderOffScreen(MonitorBlockEntity be) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(MonitorBlockEntity be) {
        return be.isController() ? be.screenBox().inflate(0.1) : BlockEntityRenderer.super.getRenderBoundingBox(be);
    }

    @Override
    public int getViewDistance() {
        return MonitorSync.VIEW_DISTANCE;
    }
}
