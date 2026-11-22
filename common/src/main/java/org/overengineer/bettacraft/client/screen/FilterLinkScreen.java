package org.overengineer.bettacraft.client.screen;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.network.FilterLinkPayload;
import org.overengineer.bettacraft.platform.client.RenderLevelStageEvent;
import org.overengineer.bettacraft.platform.network.PacketDistributor;

/**
 * M62: the choice of the tank an external filter feeds (sneak-right-click on the filter). One button per tank in reach: its
 * name, position, side and how far below it the filter stands; the current one is marked. The tank under the mouse (else the
 * current one) is outlined in the world behind the screen; after a choice the outline stays a few seconds.
 */
public final class FilterLinkScreen extends Screen {
    private static final int W = 260, ROW = 22;
    private final FilterLinkPayload data;
    private final List<Button> rows = new ArrayList<>();
    /** The last screen opened (smoke scenes press its buttons). */
    @Nullable
    public static FilterLinkScreen last;
    private int hovered = -1;

    /** The outline drawn in the world (client thread). */
    @Nullable
    private static AABB outline, wallBox;
    private static long outlineUntil;

    public FilterLinkScreen(FilterLinkPayload data) {
        super(Component.translatable("gui.bettacraft.filter_link.title"));
        this.data = data;
        last = this;
    }

    /** The server sent the tanks in reach. */
    public static void accept(FilterLinkPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) mc.setScreen(new FilterLinkScreen(p));
    }

    private int height() {
        return 44 + data.options().size() * ROW + 28;
    }

    @Override
    protected void init() {
        rows.clear();
        int left = (width - W) / 2, top = (height - height()) / 2;
        for (int i = 0; i < data.options().size(); i++) {
            int idx = i;
            Button b = Button.builder(label(i), x -> choose(idx)).bounds(left + 10, top + 36 + i * ROW, W - 20, 20).build();
            rows.add(addRenderableWidget(b));
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), x -> onClose())
                .bounds(left + W / 2 - 50, top + height() - 26, 100, 20).build());
    }

    private Component label(int i) {
        FilterLinkPayload.Option o = data.options().get(i);
        Component name = o.name().isEmpty() ? Component.translatable("gui.bettacraft.filter_link.unnamed") : Component.literal(o.name());
        Direction side = side(o);
        Component where = Component.translatable(o.below() == 0 ? "gui.bettacraft.filter_link.beside" : "gui.bettacraft.filter_link.below",
                Component.translatable("gui.bettacraft.filter_link.dir." + side.getSerializedName()), o.below());
        Component line = Component.translatable("gui.bettacraft.filter_link.option", name, o.tank().getX(), o.tank().getY(), o.tank().getZ(), where);
        return i == data.chosen() ? Component.literal("> ").append(line) : line;
    }

    /** The side of the filter the tank is on. */
    private Direction side(FilterLinkPayload.Option o) {
        BlockPos f = data.filter();
        int dx = o.wall().getX() - f.getX(), dz = o.wall().getZ() - f.getZ();
        return Direction.getApproximateNearest(dx, 0, dz);
    }

    /** Chooses option {@code i} (smoke scenes call it too). */
    public void choose(int i) {
        if (i < 0 || i >= data.options().size()) return;
        PacketDistributor.sendToServer(new FilterLinkPayload(data.filter(), data.options(), i));
        show(data.options().get(i), 60);
        onClose();
    }

    private static void show(FilterLinkPayload.Option o, int ticks) {
        outline = new AABB(o.tank().getX(), o.tank().getY(), o.tank().getZ(), o.tank().getX() + o.sizeX(), o.tank().getY() + o.sizeY(), o.tank().getZ() + o.sizeZ()).inflate(0.02);
        wallBox = new AABB(o.wall()).inflate(0.03);
        Minecraft mc = Minecraft.getInstance();
        outlineUntil = mc.level == null ? 0 : mc.level.getGameTime() + ticks;
    }

    /** Smoke scenes: shows the outline of option {@code i} as if the mouse were over it. */
    public void hover(int i) {
        hovered = i;
    }

    @Override
    public void tick() {
        int over = hovered;
        if (over < 0) {
            for (int i = 0; i < rows.size(); i++) if (rows.get(i).isHovered()) over = i;
        }
        if (over < 0) over = data.chosen();
        if (over >= 0 && over < data.options().size()) show(data.options().get(over), 2);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // no blur: the outlined tank in the world stays visible
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int left = (width - W) / 2, top = (height - height()) / 2;
        g.fill(left, top, left + W, top + height(), 0xD0101820);
        g.renderOutline(left, top, W, height(), 0xFF6A8CA8);
        g.drawCenteredString(font, title, width / 2, top + 8, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable("gui.bettacraft.filter_link.help"), width / 2, top + 21, 0xA0B0C0);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Draws the outline of the tank under the mouse (or just chosen) in the world. */
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || outline == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.getGameTime() > outlineUntil) {
            outline = null;
            return;
        }
        Vec3 cam = event.getCamera().position();
        var ps = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        float lw = mc.getWindow().getAppropriateLineWidth();
        net.minecraft.client.renderer.ShapeRenderer.renderShape(ps, buffers.getBuffer(RenderTypes.lines()), net.minecraft.world.phys.shapes.Shapes.create(outline),
                0, 0, 0, net.minecraft.util.ARGB.colorFromFloat(1F, 0.3F, 0.95F, 1F), lw);
        if (wallBox != null) net.minecraft.client.renderer.ShapeRenderer.renderShape(ps, buffers.getBuffer(RenderTypes.lines()),
                net.minecraft.world.phys.shapes.Shapes.create(wallBox), 0, 0, 0, net.minecraft.util.ARGB.colorFromFloat(1F, 1F, 0.85F, 0.2F), lw);
        ps.popPose();
        buffers.endBatch(RenderTypes.lines());
    }

    /** GameTests / smoke: the options shown. */
    public FilterLinkPayload data() {
        return data;
    }
}
