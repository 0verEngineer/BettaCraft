package org.overengineer.bettacraft.client.screen;

import org.overengineer.bettacraft.util.NbtCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.overengineer.bettacraft.aquarist.AquaristMenu;

/**
 * M66: the species one of his rearing tanks takes: a list to tick (two columns, scrolls with the wheel). Nothing ticked means
 * every species. Click a row to take the species in or leave it out; "All" clears the list.
 */
public class AquaristSpeciesScreen extends Screen {
    private static final int W = 344, H = 236, ROWS = 12, COLS = 2, COL_W = 164, ROW_H = 12;
    private final AquaristScreen parent;
    private final UUID tank;
    private int left, top, scroll;
    private final List<String[]> options = new ArrayList<>();

    public AquaristSpeciesScreen(AquaristScreen parent, UUID tank) {
        super(Component.translatable("gui.bettacraft.aquarist.species.title"));
        this.parent = parent;
        this.tank = tank;
    }

    AquaristScreen parent() {
        return parent;
    }

    /** The species keys that are ticked for this tank now (from the latest view). */
    private Set<String> ticked() {
        Set<String> out = new HashSet<>();
        for (Tag t : parent.view().getListOrEmpty("rearing_tanks")) {
            CompoundTag c = (CompoundTag) t;
            if (!NbtCompat.getUUID(c, "id").equals(tank)) continue;
            for (Tag k : c.getListOrEmpty("species")) out.add(NbtCompat.asString(k));
        }
        return out;
    }

    private String tankName() {
        for (Tag t : parent.view().getListOrEmpty("rearing_tanks")) {
            CompoundTag c = (CompoundTag) t;
            if (NbtCompat.getUUID(c, "id").equals(tank)) return c.getStringOr("name", "").isEmpty() ? net.minecraft.core.BlockPos.of(c.getLongOr("pos", 0L)).toShortString() : c.getStringOr("name", "");
        }
        return "";
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        options.clear();
        for (Tag t : parent.view().getListOrEmpty("species_options")) {
            CompoundTag c = (CompoundTag) t;
            options.add(new String[]{c.getStringOr("k", ""), Component.translatable(c.getStringOr("t", "")).getString()});
        }
        options.sort((a, b) -> a[1].compareToIgnoreCase(b[1]));
        addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.aquarist.species.all"), b -> parent.send(AquaristMenu.CLEAR_SPECIES, 0, tank.toString()))
                .bounds(left + 8, top + H - 22, 80, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.aquarist.species.done"), b -> onClose())
                .bounds(left + W - 88, top + H - 22, 80, 14).build());
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.gui.setScreen(parent);
    }

    private int rowsTotal() {
        return (options.size() + COLS - 1) / COLS;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scroll = Math.max(0, Math.min(Math.max(0, rowsTotal() - ROWS), scroll - (int) Math.signum(sy)));
        return true;
    }

    private int index(double mx, double my) {
        if (my < top + 50 || my >= top + 50 + ROWS * ROW_H) return -1;
        int row = (int) ((my - (top + 50)) / ROW_H) + scroll;
        int col = (int) ((mx - (left + 8)) / COL_W);
        if (mx < left + 8 || col < 0 || col >= COLS) return -1;
        int i = row * COLS + col;
        return i < options.size() ? i : -1;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        if (super.mouseClicked(event, doubleClick)) return true;
        int i = index(mx, my);
        if (i < 0 || button != 0) return false;
        parent.send(AquaristMenu.TOGGLE_SPECIES, 0, tank + "|" + options.get(i)[0]);
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.centeredText(font, Component.translatable("gui.bettacraft.aquarist.species.title", tankName()).withStyle(ChatFormatting.AQUA), left + W / 2, top + 6, org.overengineer.bettacraft.client.ClientCompat.textColor(0xFFFFFF));
        int y = top + 20;
        for (var line : font.split(Component.translatable("gui.bettacraft.aquarist.species.hint").withStyle(ChatFormatting.GRAY), W - 16)) {
            g.text(font, line, left + 8, y, org.overengineer.bettacraft.client.ClientCompat.textColor(0xFFFFFF));
            y += 10;
        }
        Set<String> on = ticked();
        int hover = index(mouseX, mouseY);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int i = (scroll + r) * COLS + c;
                if (i >= options.size()) break;
                int x = left + 8 + c * COL_W, yy = top + 50 + r * ROW_H;
                if (i == hover) g.fill(x - 1, yy - 1, x + COL_W - 4, yy + ROW_H - 1, 0x40FFFFFF);
                boolean ticked = on.contains(options.get(i)[0]);
                String name = font.plainSubstrByWidth(options.get(i)[1], COL_W - 20);
                g.text(font, (ticked ? "☑ " : "☐ ") + name, x, yy + 1, org.overengineer.bettacraft.client.ClientCompat.textColor(ticked ? 0xFFFFFF : 0xA0A0A0));
            }
        }
        if (rowsTotal() > ROWS) {
            int h = ROWS * ROW_H, bh = Math.max(8, h * ROWS / rowsTotal());
            int by = top + 50 + Math.round((h - bh) * (scroll / (float) Math.max(1, rowsTotal() - ROWS)));
            g.fill(left + W - 8, top + 50, left + W - 6, top + 50 + h, 0x40000000);
            g.fill(left + W - 8, by, left + W - 6, by + bh, 0xFFA0A0A0);
        }
        Component state = on.isEmpty() ? Component.translatable("gui.bettacraft.aquarist.rearing.species_all") : Component.translatable("gui.bettacraft.aquarist.rearing.species_n", on.size());
        g.centeredText(font, state, left + W / 2, top + H - 19, org.overengineer.bettacraft.client.ClientCompat.textColor(0xA0FFA0));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + W, top + H, 0xE0101418);
        g.outline(left, top, W, H, 0xFF4FA3A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
