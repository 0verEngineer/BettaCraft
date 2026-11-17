package org.overengineer.bettacraft.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.overengineer.bettacraft.platform.network.PacketDistributor;
import org.overengineer.bettacraft.equipment.AirPumpBlockEntity;
import org.overengineer.bettacraft.equipment.Regulator;
import org.overengineer.bettacraft.network.RegulatorPayload;

/**
 * M23: the power regulator of a device (right-click with an empty hand): 0..100 % in steps of 10 with -/+ and three
 * quick buttons, a bar, and what the device does right now (air pump: air stones driven; motor filters: intake
 * sponge). The value comes live from the block (state or synced block entity); changes go to the server.
 */
public class RegulatorScreen extends Screen {
    private static final int W = 260, H = 110;
    private final BlockPos pos;
    private int left, top;

    public RegulatorScreen(BlockPos pos) {
        super(Component.translatable("gui.bettacraft.regulator.title"));
        this.pos = pos;
    }

    private Regulator.Device device() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? null : Regulator.find(mc.level, pos);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.literal("-"), b -> send(-1, true)).bounds(left + 10, top + 46, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> send(1, true)).bounds(left + W - 30, top + 46, 20, 20).build());
        int[] quick = {0, 5, 10};
        Regulator.Device dev = device();
        for (int i = 0; i < quick.length; i++) {
            int v = quick[i];
            Component q = dev != null ? dev.quickLabel(v) : null; // M25c: the CO2 cylinder in bubbles per second
            addRenderableWidget(Button.builder(q != null ? q : Component.literal(v * 10 + " %"), b -> send(v, false))
                    .bounds(left + 10 + i * ((W - 20) / 3), top + 74, (W - 20) / 3 - 6, 18).build());
        }
    }

    private void send(int value, boolean relative) {
        Regulator.Device d = device();
        if (d == null) return;
        PacketDistributor.sendToServer(new RegulatorPayload(d.pos(), value, relative));
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (device() == null || mc.player == null || mc.player.distanceToSqr(pos.getCenter()) > 64) onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        Regulator.Device d = device();
        if (d == null) return;
        g.drawCenteredString(font, d.name(), left + W / 2, top + 8, org.overengineer.bettacraft.client.ClientCompat.textColor(0xFFFFFF));
        int p = d.power();
        Component label = d.valueLabel(); // M25: the CO2 cylinder shows bubbles per second
        g.drawCenteredString(font, label != null ? label : Component.translatable("gui.bettacraft.regulator.power", p * 10), left + W / 2, top + 22, 0xFFE0E0E0);
        int bx = left + 36, bw = W - 72, by = top + 52;
        g.fill(bx, by, bx + bw, by + 8, 0xFF202020);
        g.fill(bx, by, bx + Math.round(bw * p / 10F), by + 8, 0xFF50A0E0);
        Component note = null;
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getBlockEntity(d.pos()) instanceof AirPumpBlockEntity pump) {
            note = pump.running() ? Component.translatable("gui.bettacraft.regulator.pump_running", pump.driven()).withStyle(ChatFormatting.GREEN)
                    : Component.translatable("gui.bettacraft.regulator.pump_idle").withStyle(ChatFormatting.GOLD);
        } else if (d.note() != null) {
            note = d.note().copy().withStyle(ChatFormatting.AQUA); // M24
        } else if (d.takesSponge()) {
            note = d.hasSponge() ? Component.translatable("gui.bettacraft.regulator.sponge_on").withStyle(ChatFormatting.GREEN)
                    : Component.translatable("gui.bettacraft.regulator.sponge_off").withStyle(ChatFormatting.GOLD);
        }
        if (note != null) g.drawCenteredString(font, note, left + W / 2, top + 34, org.overengineer.bettacraft.client.ClientCompat.textColor(0xFFFFFF));
        Component hint = d.hint() != null ? d.hint() : Component.translatable("gui.bettacraft.regulator.hint");
        g.drawCenteredString(font, hint.copy().withStyle(ChatFormatting.GRAY), left + W / 2, top + H + 4, org.overengineer.bettacraft.client.ClientCompat.textColor(0xA0A0A0));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + W, top + H, 0xE0101418);
        g.renderOutline(left, top, W, H, 0xFF6080A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
