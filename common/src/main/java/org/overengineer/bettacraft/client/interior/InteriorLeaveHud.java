package org.overengineer.bettacraft.client.interior;

import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.network.InteriorLeavePayload;
import org.overengineer.bettacraft.registry.ModDimensions;

/**
 * M15: small hint at the bottom left while the player is in a tank interior ("Sneak + look up: leave", client config
 * {@code hud.interiorLeaveHint}); while the player holds it, a progress bar fills (the server sends the timer).
 */
public final class InteriorLeaveHud {
    private static final float SCALE = 0.75F;
    private static int ticks, needed = 60;
    private static long receivedAt = Long.MIN_VALUE;

    private InteriorLeaveHud() {}

    public static void accept(InteriorLeavePayload p) {
        Minecraft mc = Minecraft.getInstance();
        ticks = p.ticks();
        needed = Math.max(1, p.needed());
        receivedAt = mc.level == null ? 0 : mc.level.getGameTime();
    }

    /** Timer progress 0..1 as the client last heard it (0 when nothing is held or the news is old). */
    public static float progress() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ticks <= 0 || mc.level.getGameTime() - receivedAt > 10 || mc.level.getGameTime() < receivedAt) return 0F;
        return Math.min(1F, ticks / (float) needed);
    }

    /** Whether the hint is drawn now (in an interior, GUI shown, hint on or progress running). */
    public static boolean visible() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.level.dimension() != ModDimensions.TANK_INTERIOR || mc.gui.hud.isHidden()) return false;
        return hintEnabled() || progress() > 0;
    }

    private static boolean hintEnabled() {
        try {
            return ClientConfig.INTERIOR_LEAVE_HINT.get();
        } catch (IllegalStateException e) {
            return true;
        }
    }

    /** The hint text: with the look-up rule (server config) or just sneaking. */
    public static Component text() {
        int deg = ServerConfig.getOr(ServerConfig.INTERIOR_LEAVE_LOOK_UP, 60);
        return Component.translatable(deg > 0 ? "hud.bettacraft.interior.leave_hint" : "hud.bettacraft.interior.leave_hint_sneak");
    }

    public static void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (!visible()) return;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int w = g.guiWidth(), h = g.guiHeight();
        float p = progress();
        // in the free strip left of the hotbar (182 px wide, centred); on a very narrow screen not at all
        int avail = w / 2 - 91 - 9;
        if (avail < 36) return;
        Component text = p > 0 ? Component.translatable("hud.bettacraft.interior.leaving") : text();
        List<FormattedCharSequence> lines = font.split(text, (int) ((avail - 4) / SCALE));
        int lineH = (int) Math.ceil(font.lineHeight * SCALE);
        int textW = 0;
        for (FormattedCharSequence l : lines) textW = Math.max(textW, (int) Math.ceil(font.width(l) * SCALE));
        int barH = p > 0 ? 4 : 0;
        int boxW = Math.max(textW, p > 0 ? Math.min(avail - 4, 60) : 0) + 4;
        int boxH = lines.size() * lineH + barH + 3;
        int x = 3, y = h - 3 - boxH;
        g.fill(x, y, x + boxW, y + boxH, 0x70000000);
        g.pose().pushMatrix();
        g.pose().translate((float) (x + 2), (float) (y + 2));
        g.pose().scale(SCALE, SCALE);
        int ty = 0;
        for (FormattedCharSequence l : lines) {
            g.text(font, l, 0, ty, org.overengineer.bettacraft.client.ClientCompat.textColor(p > 0 ? 0xFFFFFFFF : 0xFFD8D8D8), true);
            ty += font.lineHeight;
        }
        g.pose().popMatrix();
        if (p > 0) {
            int bx = x + 2, by = y + 2 + lines.size() * lineH + 1, bw = boxW - 4;
            g.fill(bx, by, bx + bw, by + 2, 0xA0303030);
            g.fill(bx, by, bx + Math.round(bw * p), by + 2, 0xFF5FD8E8);
        }
    }
}
