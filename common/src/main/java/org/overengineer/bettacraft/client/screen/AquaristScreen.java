package org.overengineer.bettacraft.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.overengineer.bettacraft.platform.network.PacketDistributor;
import org.overengineer.bettacraft.aquarist.AquaristMenu;
import org.overengineer.bettacraft.aquarist.AquaristTask;
import org.overengineer.bettacraft.network.AquaristActionPayload;

/**
 * M51: the menu of a hired aquarist. Offering: what he offers and his wage, with the buttons to hire or refuse him.
 * Hired: status, home (breeding basin and bed), wage, the six tasks (name = on/off, the number = level 1 to 3), the
 * rearing tank (arrows go through the tanks he may work at), the tanks around his workplace and what he may do there,
 * and dismissing him (with Shift). Everything comes from the server ({@code AquaristMenu#view}).
 */
public class AquaristScreen extends Screen {
    private static final int W = 300, H = 222;
    private static final int ROWS = 4;
    private CompoundTag view;
    private int left, top;
    private int scroll;
    private final List<Button> hiredButtons = new ArrayList<>();
    private final List<Button> offerButtons = new ArrayList<>();
    private final Button[] taskButtons = new Button[AquaristTask.ALL.length];
    private final Button[] levelButtons = new Button[AquaristTask.ALL.length];

    public AquaristScreen(CompoundTag view) {
        super(Component.literal(view.getString("name")));
        this.view = view;
    }

    /** A view from the server: opens the menu, or refreshes the open one of the same aquarist. */
    public static void accept(CompoundTag view) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof AquaristScreen s && s.view.getInt("id") == view.getInt("id")) {
            s.view = view;
            s.refresh();
            return;
        }
        mc.setScreen(new AquaristScreen(view));
    }

    private boolean offering() {
        return "offering".equals(view.getString("phase"));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        hiredButtons.clear();
        offerButtons.clear();
        offerButtons.add(addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.aquarist.hire").withStyle(ChatFormatting.GREEN),
                b -> send(AquaristMenu.HIRE, 0, "")).bounds(left + 40, top + H - 34, 100, 20).build()));
        offerButtons.add(addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.aquarist.decline"), b -> {
            send(AquaristMenu.DECLINE, 0, "");
            onClose();
        }).bounds(left + W - 140, top + H - 34, 100, 20).build()));
        // tasks: two columns of three
        for (AquaristTask t : AquaristTask.ALL) {
            int i = t.ordinal(), col = i / 3, row = i % 3;
            int x = left + 8 + col * 146, y = top + 76 + row * 17;
            taskButtons[i] = addRenderableWidget(new TankScreen.FitButton(x, y, 112, 15, Component.empty(), b -> {
                int lv = levels()[i];
                send(AquaristMenu.SET_LEVEL, i * 10 + (lv > 0 ? 0 : t.defaultLevel()), "");
            }));
            levelButtons[i] = addRenderableWidget(Button.builder(Component.empty(), b -> {
                int lv = levels()[i];
                send(AquaristMenu.SET_LEVEL, i * 10 + (lv % AquaristTask.MAX_LEVEL) + 1, "");
            }).bounds(x + 114, y, 22, 15).build());
            hiredButtons.add(taskButtons[i]);
            hiredButtons.add(levelButtons[i]);
        }
        hiredButtons.add(addRenderableWidget(Button.builder(Component.literal("<"), b -> cycleRearing(-1)).bounds(left + 8, top + 131, 16, 14).build()));
        hiredButtons.add(addRenderableWidget(Button.builder(Component.literal(">"), b -> cycleRearing(1)).bounds(left + 26, top + 131, 16, 14).build()));
        Button home = addRenderableWidget(new TankScreen.FitButton(left + W - 88, top + 34, 80, 14, Component.translatable("gui.bettacraft.aquarist.find_home"),
                b -> send(AquaristMenu.FIND_HOME, 0, "")));
        home.setTooltip(Tooltip.create(Component.translatable("gui.bettacraft.aquarist.find_home.tooltip")));
        hiredButtons.add(home);
        Button dismiss = addRenderableWidget(new TankScreen.FitButton(left + W - 88, top + H - 20, 80, 14,
                Component.translatable("gui.bettacraft.aquarist.dismiss").withStyle(ChatFormatting.RED), b -> {
            if (hasShiftDown()) {
                send(AquaristMenu.DISMISS, 0, "");
                onClose();
            }
        }));
        dismiss.setTooltip(Tooltip.create(Component.translatable("gui.bettacraft.aquarist.dismiss.tooltip")));
        hiredButtons.add(dismiss);
        refresh();
    }

    private int[] levels() {
        int[] lv = view.getIntArray("tasks");
        return lv.length == AquaristTask.ALL.length ? lv : new int[AquaristTask.ALL.length];
    }

    private void refresh() {
        boolean offer = offering(), boss = view.getBoolean("boss");
        for (Button b : offerButtons) {
            b.visible = offer;
            b.active = boss;
        }
        for (Button b : hiredButtons) {
            b.visible = !offer;
            b.active = boss;
        }
        int[] lv = levels();
        for (AquaristTask t : AquaristTask.ALL) {
            int i = t.ordinal();
            boolean on = lv[i] > 0;
            taskButtons[i].setMessage(Component.literal(on ? "☑ " : "☐ ").append(Component.translatable(t.translationKey()))
                    .withStyle(on ? ChatFormatting.WHITE : ChatFormatting.GRAY));
            taskButtons[i].setTooltip(Tooltip.create(Component.translatable(t.translationKey() + ".tooltip")));
            levelButtons[i].setMessage(Component.literal(on ? String.valueOf(lv[i]) : "-"));
            levelButtons[i].active = boss && on;
            levelButtons[i].setTooltip(Tooltip.create(on ? Component.translatable(t.translationKey() + ".level" + lv[i]) : Component.translatable("gui.bettacraft.aquarist.task_off")));
        }
    }

    /** The tanks of the list he may work at (the choice for the rearing tank). */
    private List<CompoundTag> workable() {
        List<CompoundTag> out = new ArrayList<>();
        for (Tag t : view.getList("tanks", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            String v = c.getString("verdict");
            if (v.equals("own") || v.equals("team") || v.equals("released")) out.add(c);
        }
        return out;
    }

    private void cycleRearing(int dir) {
        List<CompoundTag> w = workable();
        UUID cur = view.hasUUID("rearing") ? view.getUUID("rearing") : null;
        int idx = -1; // -1 = none
        for (int i = 0; i < w.size(); i++) if (w.get(i).getUUID("id").equals(cur)) idx = i;
        int n = w.size() + 1;
        int next = Math.floorMod(idx + 1 + dir, n) - 1;
        send(AquaristMenu.SET_REARING, 0, next < 0 ? "" : w.get(next).getUUID("id").toString());
    }

    private void send(int action, int value, String text) {
        PacketDistributor.sendToServer(new AquaristActionPayload(view.getInt("id"), action, value, text));
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            onClose();
            return;
        }
        var e = mc.level.getEntity(view.getInt("id"));
        if (e == null || !e.isAlive() || mc.player.distanceToSqr(e) > 8 * 8) onClose();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int n = view.getList("tanks", Tag.TAG_COMPOUND).size();
        scroll = Math.max(0, Math.min(Math.max(0, n - ROWS), scroll - (int) Math.signum(sy)));
        return true;
    }

    private static String pos(long l) {
        BlockPos p = BlockPos.of(l);
        return p.getX() + " " + p.getY() + " " + p.getZ();
    }

    private static ChatFormatting verdictColor(String v) {
        return switch (v) {
            case "own", "team", "released" -> ChatFormatting.GREEN;
            case "unloaded" -> ChatFormatting.DARK_GRAY;
            case "locked", "excluded" -> ChatFormatting.GOLD;
            default -> ChatFormatting.GRAY;
        };
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        MutableComponent head = Component.literal(view.getString("name")).withStyle(ChatFormatting.AQUA)
                .append(Component.literal(" – ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable("entity.bettacraft.hired_aquarist").withStyle(ChatFormatting.GRAY));
        g.drawCenteredString(font, head, left + W / 2, top + 7, 0xFFFFFF);
        if (offering()) renderOffer(g);
        else renderHired(g);
    }

    private void renderOffer(GuiGraphics g) {
        int y = top + 26;
        Component text = Component.translatable("gui.bettacraft.aquarist.offer_text", view.getString("employer"));
        for (FormattedCharSequence line : font.split(text, W - 20)) {
            g.drawString(font, line, left + 10, y, 0xFFE0E0E0);
            y += 10;
        }
        y += 6;
        Component pay = !view.getBoolean("wage") ? Component.translatable("message.bettacraft.aquarist.offer.free")
                : Component.translatable("message.bettacraft.aquarist.offer.wage", view.getInt("wage_emeralds"), view.getInt("wage_food"));
        for (FormattedCharSequence line : font.split(pay.copy().withStyle(ChatFormatting.GOLD), W - 20)) {
            g.drawString(font, line, left + 10, y, 0xFFFFFF);
            y += 10;
        }
        y += 6;
        for (FormattedCharSequence line : font.split(Component.translatable("gui.bettacraft.aquarist.offer_home").withStyle(ChatFormatting.GRAY), W - 20)) {
            g.drawString(font, line, left + 10, y, 0xFFFFFF);
            y += 10;
        }
        if (view.contains("offer_left")) {
            double days = view.getLong("offer_left") / 24000.0;
            g.drawString(font, Component.translatable("gui.bettacraft.aquarist.offer_left", String.format(java.util.Locale.ROOT, "%.1f", days))
                    .withStyle(ChatFormatting.DARK_GRAY), left + 10, top + H - 48, 0xFFFFFF);
        }
        if (!view.getBoolean("boss")) {
            g.drawCenteredString(font, Component.translatable("message.bettacraft.aquarist.not_yours").withStyle(ChatFormatting.RED), left + W / 2, top + H - 10, 0xFFFFFF);
        }
    }

    private void renderHired(GuiGraphics g) {
        String status = view.getString("status");
        boolean bad = status.equals("no_workplace") || status.equals("no_bed") || status.equals("unpaid") || status.equals("off");
        Component st = Component.translatable("gui.bettacraft.aquarist.status", Component.translatable("gui.bettacraft.aquarist.status." + status)
                .withStyle(bad ? ChatFormatting.RED : ChatFormatting.GREEN), view.getString("employer"));
        g.drawString(font, st, left + 8, top + 20, 0xFFE0E0E0);
        Component wp = view.contains("workplace") ? Component.translatable("gui.bettacraft.aquarist.workplace", pos(view.getLong("workplace")))
                : Component.translatable("gui.bettacraft.aquarist.no_workplace").withStyle(ChatFormatting.RED);
        Component bed = view.contains("bed") ? Component.translatable("gui.bettacraft.aquarist.bed", pos(view.getLong("bed")))
                : Component.translatable("gui.bettacraft.aquarist.no_bed").withStyle(ChatFormatting.RED);
        fit(g, wp, left + 8, top + 32, W - 100);
        fit(g, bed, left + 8, top + 42, W - 100);
        Component wage;
        if (!view.getBoolean("wage")) wage = Component.translatable("gui.bettacraft.aquarist.wage_off").withStyle(ChatFormatting.GRAY);
        else if (view.getInt("unpaid") > 0) wage = Component.translatable("gui.bettacraft.aquarist.unpaid", view.getInt("unpaid"), view.getInt("leave_days")).withStyle(ChatFormatting.RED);
        else wage = Component.translatable("gui.bettacraft.aquarist.wage", view.getInt("wage_emeralds"), view.getInt("wage_food")).withStyle(ChatFormatting.GOLD);
        fit(g, wage, left + 8, top + 53, W - 16);
        Component chest = view.getInt("chests") == 0 ? Component.translatable("gui.bettacraft.aquarist.no_chest").withStyle(ChatFormatting.GOLD)
                : view.getBoolean("food") ? Component.translatable("gui.bettacraft.aquarist.chest_food").withStyle(ChatFormatting.GRAY)
                : Component.translatable("gui.bettacraft.aquarist.chest_no_food").withStyle(ChatFormatting.GOLD);
        fit(g, chest, left + 8, top + 64, W - 16);
        // rearing tank
        String rearing = "-";
        if (view.hasUUID("rearing")) {
            UUID id = view.getUUID("rearing");
            for (Tag t : view.getList("tanks", Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) t;
                if (c.getUUID("id").equals(id)) rearing = name(c);
            }
        }
        fit(g, Component.translatable("gui.bettacraft.aquarist.rearing", rearing), left + 46, top + 134, W - 54);
        // the tanks around
        fit(g, Component.translatable("gui.bettacraft.aquarist.tanks", view.getInt("cared"), view.getInt("max_tanks"), view.getInt("radius"))
                .withStyle(ChatFormatting.AQUA), left + 8, top + 150, W - 16);
        var list = view.getList("tanks", Tag.TAG_COMPOUND);
        int y = top + 161;
        for (int i = scroll; i < Math.min(list.size(), scroll + ROWS); i++) {
            CompoundTag c = list.getCompound(i);
            String v = c.getString("verdict");
            fit(g, Component.literal(name(c)), left + 12, y, 150);
            fit(g, Component.translatable("gui.bettacraft.aquarist.tank." + v).withStyle(verdictColor(v)), left + 166, y, 120);
            y += 9;
        }
        if (list.isEmpty()) g.drawString(font, Component.translatable("gui.bettacraft.aquarist.no_tanks").withStyle(ChatFormatting.GRAY), left + 12, y, 0xFFFFFF);
    }

    private static String name(CompoundTag c) {
        String n = c.getString("name");
        return n.isEmpty() ? pos(c.getLong("pos")) : n + " (" + pos(c.getLong("pos")) + ")";
    }

    /** A line that is drawn smaller when it does not fit. */
    private void fit(GuiGraphics g, Component c, int x, int y, int room) {
        int w = font.width(c);
        if (w <= room) {
            g.drawString(font, c, x, y, 0xFFE0E0E0);
            return;
        }
        float s = Math.max(0.6F, room / (float) w);
        g.pose().pushPose();
        g.pose().translate(x, y + (1 - s) * 4, 0);
        g.pose().scale(s, s, 1);
        g.drawString(font, c, 0, 0, 0xFFE0E0E0);
        g.pose().popPose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + W, top + H, 0xE0101418);
        g.renderOutline(left, top, W, H, 0xFF4FA3A0);
        if (!offering()) {
            g.fill(left + 4, top + 72, left + W - 4, top + 73, 0x804FA3A0);
            g.fill(left + 4, top + 128, left + W - 4, top + 129, 0x804FA3A0);
            g.fill(left + 4, top + 147, left + W - 4, top + 148, 0x804FA3A0);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
