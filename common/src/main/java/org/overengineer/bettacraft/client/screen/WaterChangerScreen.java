package org.overengineer.bettacraft.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.overengineer.bettacraft.network.WaterChangerPayload;
import org.overengineer.bettacraft.platform.network.PacketDistributor;
import org.overengineer.bettacraft.plumbing.ChangerTexts;
import org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity;

/**
 * M24: the water change controller: interval (days, sneak: 5), share (%, sneak: 1), on/off, change now, and what it did last.
 * Values come live from the synced block entity; changes go to the server.
 * <p>
 * M68 part B: what it serves (tanks, through a filter, level sensors), its four ports (a line, the touch, or missing), the running
 * change, its stores and the warnings (no fresh water, no drain, a short circuit, it sucks in young animals); the hint names the
 * ports as seen from the front.
 */
public class WaterChangerScreen extends Screen {
    private static final int W = 300, H = 214;
    private final BlockPos pos;
    private int left, top;
    private Button toggle;

    public WaterChangerScreen(BlockPos pos) {
        super(Component.translatable("block.bettacraft.water_change_controller"));
        this.pos = pos;
    }

    private WaterChangerBlockEntity be() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getBlockEntity(pos) instanceof WaterChangerBlockEntity w ? w : null;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.literal("-"), b -> send(Minecraft.getInstance().hasShiftDown() ? -5 : -1, 0, null, false)).bounds(left + 180, top + 30, 20, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> send(Minecraft.getInstance().hasShiftDown() ? 5 : 1, 0, null, false)).bounds(left + W - 30, top + 30, 20, 16).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> send(0, Minecraft.getInstance().hasShiftDown() ? -1 : -5, null, false)).bounds(left + 180, top + 50, 20, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> send(0, Minecraft.getInstance().hasShiftDown() ? 1 : 5, null, false)).bounds(left + W - 30, top + 50, 20, 16).build());
        toggle = addRenderableWidget(Button.builder(Component.empty(), b -> {
            WaterChangerBlockEntity w = be();
            if (w != null) send(0, 0, !w.enabled(), false);
        }).bounds(left + 10, top + 72, 135, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.water_changer.now"), b -> send(0, 0, null, true))
                .bounds(left + 155, top + 72, 135, 18).build());
    }

    private void send(int dInterval, int dShare, Boolean enabled, boolean now) {
        WaterChangerBlockEntity w = be();
        if (w == null) return;
        PacketDistributor.sendToServer(new WaterChangerPayload(pos, w.intervalDays() + dInterval, w.share() + dShare,
                enabled != null ? enabled : w.enabled(), now));
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (be() == null || mc.player == null || mc.player.distanceToSqr(pos.getCenter()) > 64) onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        WaterChangerBlockEntity w = be();
        if (w != null && toggle != null) toggle.setMessage(Component.translatable(w.enabled() ? "gui.bettacraft.water_changer.on" : "gui.bettacraft.water_changer.off"));
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (w == null) return;
        g.centeredText(font, title, left + W / 2, top + 8, org.overengineer.bettacraft.client.ClientCompat.textColor(0xFFFFFF));
        g.text(font, Component.translatable("gui.bettacraft.water_changer.interval", w.intervalDays()), left + 10, top + 34, 0xFFE0E0E0);
        g.text(font, Component.translatable("gui.bettacraft.water_changer.share", w.share()), left + 10, top + 54, 0xFFE0E0E0);
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        int x = left + 10, y = top + 98, tw = W - 20;
        y = text(g, ChangerTexts.status(w.status(), w.shownPending(), w.lastMb(), w.lastTopOff()), x, y, tw);
        if (w.last() >= 0 && w.enabled() && w.shownPending() <= 0) y = text(g, ChangerTexts.next(w.last(), w.intervalDays(), now), x, y, tw);
        y = text(g, ChangerTexts.tanks(w.tanks(), w.filterTanks(), w.sensors()), x, y, tw);
        y = text(g, ChangerTexts.lines(w.flags()), x, y, tw);
        y = text(g, ChangerTexts.stores(w.shownFresh(), w.shownWaste(), w.toppedTotal()), x, y, tw);
        for (Component c : ChangerTexts.warnings(w.flags(), w.tanks())) y = text(g, c, x, y, tw);
        y = Math.max(y + 2, top + H - 24);
        text(g, Component.translatable("gui.bettacraft.water_changer.hint").withStyle(net.minecraft.ChatFormatting.GRAY), x, y, tw);
    }

    /** Draws {@code c} wrapped to {@code width}; returns the y below it. */
    private int text(GuiGraphics g, Component c, int x, int y, int width) {
        for (FormattedCharSequence l : font.split(c, width)) {
            g.text(font, l, x, y, org.overengineer.bettacraft.client.ClientCompat.textColor(0xFFFFFF));
            y += 10;
        }
        return y + 1;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + W, top + H, 0xE0101418);
        g.outline(left, top, W, H, 0xFF6080A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
