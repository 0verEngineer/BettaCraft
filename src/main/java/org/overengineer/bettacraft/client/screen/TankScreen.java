package org.overengineer.bettacraft.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.entity.betta.BettaTooltip;
import org.overengineer.bettacraft.genetics.BettaLoci;
import org.overengineer.bettacraft.genetics.Locus;
import org.overengineer.bettacraft.genetics.GenotypeText;
import org.overengineer.bettacraft.network.TankActionPayload;
import org.overengineer.bettacraft.tank.SignPosition;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankTemperature;
import org.overengineer.bettacraft.tank.menu.TankActions;
import org.overengineer.bettacraft.tank.menu.TankMenu;
import org.overengineer.bettacraft.tank.menu.TankView;

/** Tank menu screen with the tabs Overview, Inhabitants, Breeding, Interior (M3) and Log (PLAN 8). */
public class TankScreen extends AbstractContainerScreen<TankMenu> {
    private static final ResourceLocation TEXTURE = BettaCraft.id("textures/gui/tank.png");
    private static final int TEXT = 0x404040;
    private static final int TEXT_LIGHT = 0xE0E0E0;
    private static final int CONTENT_X = 7, CONTENT_Y = 17, CONTENT_W = 234, CONTENT_H = 123;
    private static final int LIST_ROWS = 10, ROW_H = 11, LOG_ROWS = 10, BREED_ROWS = 12;

    private enum Tab { OVERVIEW, INHABITANTS, BREEDING, LOG, INTERIOR }

    /** Log filter: all, breeding (nest, eggs, fry, growth), health (sick, deaths, fights), feeding. */
    private enum LogFilter { ALL, BREEDING, HEALTH, FEEDING }

    private Tab tab = Tab.OVERVIEW;
    private final List<AbstractWidget> overview = new ArrayList<>();
    private final List<AbstractWidget> inhabitants = new ArrayList<>();
    private Button tabOverview, tabInhabitants, tabBreeding, tabLog, tabInterior, logFilterButton, interiorEnterButton;
    private LogFilter logFilter = LogFilter.ALL;
    private int breedScroll;
    private EditBox nameBox, fishNameBox;
    private Button lockButton, signButton, signPosButton, frontButton, minusButton, plusButton, enterButton;
    private Button renameButton, bucketButton, bagButton;
    private String sentName;
    private int nameDirtyTicks = -1;
    @Nullable
    private UUID selected;
    private UUID nameBoxFish;
    private int listScroll, logScroll;
    @Nullable
    private List<Component> pendingTooltip;

    public TankScreen(TankMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 248;
        imageHeight = 230;
        inventoryLabelX = TankMenu.INV_X;
        inventoryLabelY = 10000; // no inventory label (no room)
    }

    @Nullable
    private TankView view() {
        return menu.view();
    }

    private void send(int action, int value, String text, @Nullable UUID target) {
        PacketDistributor.sendToServer(new TankActionPayload(menu.containerId, action, value, text, Optional.ofNullable(target)));
    }

    @Override
    protected void init() {
        super.init();
        overview.clear();
        inhabitants.clear();
        int x = leftPos, y = topPos;
        // five tabs across the top (the title moved into the overview: no room for both)
        tabOverview = addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.tab.overview"), b -> setTab(Tab.OVERVIEW))
                .bounds(x + 5, y + 3, 53, 12).build());
        tabInhabitants = addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.tab.inhabitants"), b -> setTab(Tab.INHABITANTS))
                .bounds(x + 58, y + 3, 50, 12).build());
        tabBreeding = addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.tab.breeding"), b -> setTab(Tab.BREEDING))
                .bounds(x + 108, y + 3, 50, 12).build());
        tabInterior = addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.tab.interior"), b -> setTab(Tab.INTERIOR))
                .bounds(x + 158, y + 3, 56, 12).build());
        tabLog = addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.tab.log"), b -> setTab(Tab.LOG))
                .bounds(x + 214, y + 3, 27, 12).build());
        interiorEnterButton = addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.tank.enter"), b -> send(TankActions.ENTER_INTERIOR, 0, "", null))
                .bounds(x + 176, y + 124, 62, 14).build());
        logFilterButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            logFilter = LogFilter.values()[(logFilter.ordinal() + 1) % LogFilter.values().length];
            logScroll = 0;
            updateWidgets();
        }).bounds(x + 176, y + 125, 62, 13).build());

        // ---- overview
        nameBox = new EditBox(font, x + 11, y + 21, 150, 13, Component.translatable("gui.bettacraft.tank.name"));
        nameBox.setMaxLength(TankData.MAX_NAME_LENGTH);
        nameBox.setHint(Component.translatable("gui.bettacraft.tank.name_hint").withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setResponder(s -> nameDirtyTicks = 15);
        overview.add(addRenderableWidget(nameBox));
        lockButton = addRenderableWidget(Button.builder(Component.empty(), b -> send(TankActions.TOGGLE_LOCK, 0, "", null))
                .bounds(x + 164, y + 20, 74, 15).build());
        overview.add(lockButton);
        signButton = addRenderableWidget(Button.builder(Component.empty(), b -> send(TankActions.TOGGLE_SIGN, 0, "", null))
                .bounds(x + 10, y + 38, 62, 14).build());
        signPosButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            TankView v = view();
            if (v != null) send(TankActions.SET_SIGN_POS, v.signPos.next().ordinal(), "", null);
        }).bounds(x + 74, y + 38, 94, 14).build());
        frontButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            TankView v = view();
            if (v != null) send(TankActions.SET_FRONT, v.front.getClockWise().get2DDataValue(), "", null);
        }).bounds(x + 170, y + 38, 68, 14).build());
        minusButton = addRenderableWidget(Button.builder(Component.literal("-"), b -> changeTarget(-1))
                .bounds(x + 168, y + 77, 14, 12).build());
        plusButton = addRenderableWidget(Button.builder(Component.literal("+"), b -> changeTarget(1))
                .bounds(x + 224, y + 77, 14, 12).build());
        enterButton = addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.tank.enter"), b -> send(TankActions.ENTER_INTERIOR, 0, "", null))
                .bounds(x + 186, y + 123, 52, 14).build());
        overview.addAll(List.of(signButton, signPosButton, frontButton, minusButton, plusButton, enterButton));

        // ---- inhabitants
        fishNameBox = new EditBox(font, x + 115, y + 109, 84, 12, Component.translatable("gui.bettacraft.fish.name"));
        fishNameBox.setMaxLength(TankActions.MAX_FISH_NAME);
        inhabitants.add(addRenderableWidget(fishNameBox));
        renameButton = addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.fish.rename"), b -> {
            if (selected != null) send(TankActions.RENAME_FISH, 0, fishNameBox.getValue(), selected);
        }).bounds(x + 201, y + 108, 37, 14).build());
        bucketButton = addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.fish.take_bucket"),
                b -> takeOut(false)).bounds(x + 114, y + 124, 62, 14).build());
        bagButton = addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.fish.take_bag"),
                b -> takeOut(true)).bounds(x + 177, y + 124, 61, 14).build());
        inhabitants.addAll(List.of(renameButton, bucketButton, bagButton));
        sentName = null;
        updateWidgets();
    }

    /** Dev smoke test only. */
    public void selectTabForTest(int index) {
        setTab(Tab.values()[index]);
    }

    private void setTab(Tab t) {
        tab = t;
        setFocused(null);
        updateWidgets();
    }

    private void changeTarget(int dir) {
        TankView v = view();
        if (v == null) return;
        float step = hasShiftDown() ? 1.0F : 0.5F;
        float t = TankTemperature.clampTarget(v.targetTemp + dir * step);
        send(TankActions.SET_TARGET, Math.round(t * 2), "", null);
    }

    private void takeOut(boolean bag) {
        if (selected != null) send(TankActions.TAKE_OUT, bag ? 1 : 0, "", selected);
    }

    @Nullable
    private TankView.Inhabitant selectedInhabitant(TankView v) {
        for (TankView.Inhabitant i : v.inhabitants) if (i.uuid().equals(selected)) return i;
        return null;
    }

    private void updateWidgets() {
        TankView v = view();
        for (AbstractWidget w : overview) w.visible = tab == Tab.OVERVIEW;
        for (AbstractWidget w : inhabitants) w.visible = tab == Tab.INHABITANTS;
        tabOverview.active = tab != Tab.OVERVIEW;
        tabInhabitants.active = tab != Tab.INHABITANTS;
        tabBreeding.active = tab != Tab.BREEDING;
        tabLog.active = tab != Tab.LOG;
        tabInterior.active = tab != Tab.INTERIOR;
        interiorEnterButton.visible = tab == Tab.INTERIOR;
        logFilterButton.visible = tab == Tab.LOG;
        logFilterButton.setMessage(Component.translatable("gui.bettacraft.log.filter." + logFilter.name().toLowerCase(java.util.Locale.ROOT)));
        if (v == null) {
            for (AbstractWidget w : overview) w.active = false;
            for (AbstractWidget w : inhabitants) w.active = false;
            return;
        }
        if (sentName == null) {
            sentName = v.name;
            nameBox.setValue(v.name);
            nameDirtyTicks = -1;
        }
        boolean mod = v.canModify;
        nameBox.setEditable(mod);
        nameBox.active = mod;
        lockButton.active = v.isOwner || v.ownerName.isEmpty() || minecraft.player.hasPermissions(2);
        lockButton.setMessage(Component.translatable(v.locked ? "gui.bettacraft.tank.locked" : "gui.bettacraft.tank.unlocked"));
        lockButton.setTooltip(Tooltip.create(Component.translatable("gui.bettacraft.tank.lock_tooltip",
                v.ownerName.isEmpty() ? Component.translatable("gui.bettacraft.tank.no_owner") : Component.literal(v.ownerName))));
        signButton.setMessage(Component.translatable(v.signVisible ? "gui.bettacraft.tank.sign_on" : "gui.bettacraft.tank.sign_off"));
        signPosButton.setMessage(Component.translatable(v.signPos.translationKey()));
        frontButton.setMessage(Component.translatable("gui.bettacraft.tank.front", Component.translatable("gui.bettacraft.direction." + v.front.getSerializedName())));
        for (Button b : List.of(signButton, signPosButton, frontButton, minusButton, plusButton)) b.active = mod;
        enterButton.active = mod || !v.locked;
        interiorEnterButton.active = mod;

        TankView.Inhabitant sel = selectedInhabitant(v);
        if (sel == null && !v.inhabitants.isEmpty()) {
            selected = v.inhabitants.getFirst().uuid();
            sel = v.inhabitants.getFirst();
        }
        boolean has = sel != null;
        if (has && !sel.uuid().equals(nameBoxFish)) {
            nameBoxFish = sel.uuid();
            fishNameBox.setValue(sel.customName());
        }
        fishNameBox.active = has && mod;
        fishNameBox.setEditable(has && mod);
        renameButton.active = has && mod;
        bucketButton.active = has && mod && sel.bucketable() && v.hasBucket && v.waterMb >= TankInhabitants.BUCKET_MB;
        bagButton.active = has && mod && sel.bucketable() && v.hasBag && v.waterMb >= TankInhabitants.BAG_MB;
        bucketButton.setTooltip(Tooltip.create(takeTooltip(v, sel, false)));
        bagButton.setTooltip(Tooltip.create(takeTooltip(v, sel, true)));
    }

    private Component takeTooltip(TankView v, @Nullable TankView.Inhabitant sel, boolean bag) {
        long cost = bag ? TankInhabitants.BAG_MB : TankInhabitants.BUCKET_MB;
        if (sel == null) return Component.translatable("gui.bettacraft.fish.none_selected");
        if (!v.canModify) return Component.translatable("message.bettacraft.tank.locked");
        if (!sel.bucketable()) return Component.translatable("gui.bettacraft.fish.not_bucketable");
        if (!(bag ? v.hasBag : v.hasBucket)) return Component.translatable(bag ? "message.bettacraft.tank.need_bag" : "message.bettacraft.tank.need_bucket");
        if (v.waterMb < cost) return Component.translatable("message.bettacraft.tank.not_enough_water");
        return Component.translatable(bag ? "gui.bettacraft.fish.take_bag_tooltip" : "gui.bettacraft.fish.take_bucket_tooltip", cost);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (nameDirtyTicks > 0 && --nameDirtyTicks == 0) {
            String v = nameBox.getValue();
            if (!v.equals(sentName)) {
                sentName = v;
                send(TankActions.SET_NAME, 0, v, null);
            }
        }
        updateWidgets();
    }

    @Override
    public void removed() {
        // flush a pending name edit
        if (nameDirtyTicks > 0 && sentName != null && !nameBox.getValue().equals(sentName)) {
            send(TankActions.SET_NAME, 0, nameBox.getValue(), null);
        }
        super.removed();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) return super.keyPressed(key, scan, mods);
        for (EditBox box : List.of(nameBox, fishNameBox)) {
            if (box.isVisible() && box.isFocused()) {
                if (box == fishNameBox && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && renameButton.active) {
                    renameButton.onPress();
                    return true;
                }
                box.keyPressed(key, scan, mods);
                return true; // never let typing trigger the inventory key
            }
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        TankView v = view();
        if (tab == Tab.INHABITANTS && v != null) {
            int lx = leftPos + 10, ly = topPos + 20;
            if (mx >= lx && mx < lx + 100 && my >= ly && my < ly + LIST_ROWS * ROW_H) {
                int idx = listScroll + (int) ((my - ly) / ROW_H);
                if (idx >= 0 && idx < v.inhabitants.size()) {
                    selected = v.inhabitants.get(idx).uuid();
                    updateWidgets();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        TankView v = view();
        if (v != null && my < topPos + CONTENT_Y + CONTENT_H) {
            if (tab == Tab.INHABITANTS) {
                listScroll = Mth.clamp(listScroll - (int) Math.signum(sy), 0, Math.max(0, v.inhabitants.size() - LIST_ROWS));
                return true;
            }
            if (tab == Tab.LOG) {
                logScroll = Mth.clamp(logScroll - (int) Math.signum(sy), 0, Math.max(0, filteredLog(v).size() - LOG_ROWS));
                return true;
            }
            if (tab == Tab.BREEDING) {
                breedScroll = Mth.clamp(breedScroll - (int) Math.signum(sy), 0, Math.max(0, BreedingLines.build(v).size() - BREED_ROWS));
                return true;
            }
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    // ------------------------------------------------------------------ drawing

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        // ghost icons in empty heater slots
        for (int i = 0; i < TankData.HEATER_SLOTS; i++) {
            if (!menu.getSlot(i).hasItem()) g.blit(TEXTURE, leftPos + TankMenu.HEATER_X, topPos + TankMenu.SLOT_Y + i * 18, 0, 232, 16, 16);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        pendingTooltip = null;
        super.render(g, mouseX, mouseY, partialTick);
        if (pendingTooltip != null) g.renderComponentTooltip(font, pendingTooltip, mouseX, mouseY);
        else renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        TankView v = view();
        if (v == null) {
            g.drawString(font, Component.translatable("gui.bettacraft.loading"), 12, 24, TEXT_LIGHT, false);
            return;
        }
        switch (tab) {
            case OVERVIEW -> drawOverview(g, v);
            case INHABITANTS -> drawInhabitants(g, v, mouseX - leftPos, mouseY - topPos);
            case BREEDING -> drawBreeding(g, v, mouseX - leftPos, mouseY - topPos);
            case LOG -> drawLog(g, v, mouseX - leftPos, mouseY - topPos);
            case INTERIOR -> drawInterior(g, v, mouseX - leftPos, mouseY - topPos);
        }
        if (v.locked && tab == Tab.OVERVIEW) g.blit(TEXTURE, 228, 125, 16, 232, 10, 10);
    }

    private void line(GuiGraphics g, Component c, int x, int y, int color) {
        g.drawString(font, c, x, y, color, false);
    }

    private void drawOverview(GuiGraphics g, TankView v) {
        int x = 11, y = 56;
        int pct = v.capacityMb <= 0 ? 0 : (int) (v.waterMb * 100 / v.capacityMb);
        line(g, Component.translatable("gui.bettacraft.tank.size_water", v.sizeX, v.sizeY, v.sizeZ, v.waterMb, v.capacityMb, pct), x, y, TEXT_LIGHT);
        y += 11;
        int tempColor = TankTemperature.isTolerable(v.waterTemp) ? (v.waterTemp >= TankTemperature.OPTIMAL_MIN && v.waterTemp <= TankTemperature.OPTIMAL_MAX ? 0x80FF80 : 0xFFE080) : 0xFF7070;
        line(g, Component.translatable("gui.bettacraft.tank.temperature", fmt(v.waterTemp), fmt(v.ambient)), x, y, tempColor);
        y += 11;
        y += 1;
        line(g, Component.translatable("gui.bettacraft.tank.target"), x, y + 2, TEXT_LIGHT);
        String t = fmt(v.targetTemp) + " °C";
        g.drawCenteredString(font, t, 203, y + 2, 0xFFFFFF);
        y += 14;
        double water = Math.max(0.001, v.waterMb / 1000.0);
        Component heat;
        if (v.heaterBlocks <= 0) heat = Component.translatable("gui.bettacraft.tank.heaters_none").withStyle(ChatFormatting.GRAY);
        else if (v.heaterBlocks + 1e-6 >= water) heat = Component.translatable("gui.bettacraft.tank.heaters_ok", (int) v.heaterBlocks, fmt((float) water)).withStyle(ChatFormatting.GREEN);
        else heat = Component.translatable("gui.bettacraft.tank.heaters_weak", (int) v.heaterBlocks, fmt((float) water)).withStyle(ChatFormatting.GOLD);
        line(g, Component.translatable("gui.bettacraft.tank.heaters", heat), x, y, TEXT_LIGHT);
        y += 11;
        Component feeder = v.feederFood < 0 ? Component.translatable("gui.bettacraft.tank.none").withStyle(ChatFormatting.GRAY)
                : Component.translatable("gui.bettacraft.tank.feeder_state", v.feederFood, v.feederPerDay)
                .withStyle(v.feederFood == 0 ? ChatFormatting.GOLD : ChatFormatting.GREEN);
        line(g, Component.translatable("gui.bettacraft.tank.feeder", feeder), x, y, TEXT_LIGHT);
        y += 11;
        Component light = v.lights == 0 ? Component.translatable("gui.bettacraft.tank.none").withStyle(ChatFormatting.GRAY)
                : Component.translatable("gui.bettacraft.tank.light_state", v.lightsLit, v.lights);
        line(g, Component.translatable("gui.bettacraft.tank.light", light), x, y, TEXT_LIGHT);
        y += 11;
        MutableComponent inh = Component.translatable("gui.bettacraft.tank.inhabitants", v.inhabitants.size(), v.adults, v.hardCap);
        if (v.crowded) inh.append(" ").append(Component.translatable("gui.bettacraft.tank.crowded").withStyle(ChatFormatting.RED));
        line(g, inh, x, y, TEXT_LIGHT);
        y += 11;
        if (v.waterMb < v.minInsertMb) {
            line(g, Component.translatable("gui.bettacraft.tank.fill_hint", v.minInsertMb).withStyle(ChatFormatting.GOLD), x, y, TEXT_LIGHT);
        }
    }

    private void drawInterior(GuiGraphics g, TankView v, int mx, int my) {
        int x = 11, y = 20;
        line(g, Component.translatable("gui.bettacraft.interior.size", v.interiorX, v.interiorY, v.interiorZ, v.scale, 16 / v.scale), x, y, TEXT_LIGHT);
        y += 11;
        if (!v.interiorBuilt) {
            line(g, Component.translatable("gui.bettacraft.interior.not_built").withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT);
        } else {
            Component blocks = v.interiorBlocks < 0 ? Component.translatable("gui.bettacraft.interior.blocks_unknown")
                    : Component.translatable("gui.bettacraft.interior.blocks", String.format(java.util.Locale.ROOT, "%,d", v.interiorBlocks),
                    String.format(java.util.Locale.ROOT, "%,d", v.renderLimit));
            line(g, blocks.copy().withStyle(v.interiorTruncated ? ChatFormatting.RED : ChatFormatting.RESET), x, y, v.interiorTruncated ? 0xFF7070 : TEXT_LIGHT);
            if (v.interiorTruncated) {
                y += 10;
                line(g, Component.translatable("gui.bettacraft.interior.limit").withStyle(ChatFormatting.RED), x, y, 0xFF7070);
            }
            y += 11;
            line(g, Component.translatable("gui.bettacraft.interior.substrate", v.substrate), x, y, TEXT_LIGHT);
            y += 11;
            MutableComponent heat = Component.translatable("gui.bettacraft.interior.heaters", v.heatersInside, v.heaterCapWet);
            if (v.heaterCapDry > 0) heat.append(" ").append(Component.translatable("gui.bettacraft.interior.heaters_dry", v.heaterCapDry).withStyle(ChatFormatting.GOLD));
            line(g, heat, x, y, TEXT_LIGHT);
            y += 11;
            line(g, Component.translatable("gui.bettacraft.interior.filters", v.filters, v.floating), x, y, TEXT_LIGHT);
        }
        y += 13;
        line(g, Component.translatable("gui.bettacraft.interior.decor").withStyle(ChatFormatting.YELLOW), x, y, TEXT_LIGHT);
        y += 10;
        int i = 0;
        for (var d : org.overengineer.bettacraft.registry.ModItems.DECOR.values()) {
            int ix = x + (i % 11) * 20, iy = y + (i / 11) * 18;
            net.minecraft.world.item.ItemStack st = new net.minecraft.world.item.ItemStack(d.get());
            g.fill(ix - 1, iy - 1, ix + 17, iy + 17, 0x40FFFFFF);
            g.renderItem(st, ix, iy);
            if (mx >= ix && mx < ix + 16 && my >= iy && my < iy + 16) {
                List<Component> tip = new ArrayList<>(getTooltipFromContainerItem(st));
                pendingTooltip = tip;
            }
            i++;
        }
        y += 20;
        line(g, Component.translatable("gui.bettacraft.interior.hint_enter").withStyle(ChatFormatting.GRAY), x, y, 0xA0A0A0);
        y += 10;
        line(g, Component.translatable("gui.bettacraft.interior.hint_substrate").withStyle(ChatFormatting.GRAY), x, y, 0xA0A0A0);
    }

    private static String fmt(float f) {
        return String.format(java.util.Locale.ROOT, "%.1f", f);
    }

    private Component inhabitantName(TankView.Inhabitant i) {
        return i.customName().isEmpty() ? Component.translatable(i.typeKey()) : Component.literal(i.customName());
    }

    private void drawInhabitants(GuiGraphics g, TankView v, int mx, int my) {
        int lx = 10, ly = 20;
        g.fill(lx - 1, ly - 1, lx + 100, ly + LIST_ROWS * ROW_H, 0x60000000);
        if (v.inhabitants.isEmpty()) {
            line(g, Component.translatable("gui.bettacraft.fish.empty"), lx + 2, ly + 2, 0xA0A0A0);
        }
        for (int r = 0; r < LIST_ROWS; r++) {
            int idx = listScroll + r;
            if (idx >= v.inhabitants.size()) break;
            TankView.Inhabitant i = v.inhabitants.get(idx);
            int y = ly + r * ROW_H;
            if (i.uuid().equals(selected)) g.fill(lx, y, lx + 99, y + ROW_H, 0x80FFFFFF);
            else if (mx >= lx && mx < lx + 100 && my >= y && my < y + ROW_H) g.fill(lx, y, lx + 99, y + ROW_H, 0x30FFFFFF);
            String sym = i.betta() ? (i.female() ? "♀ " : "♂ ") : "";
            Component name = Component.literal(sym).append(inhabitantName(i));
            String s = font.plainSubstrByWidth(name.getString(), 90);
            int color = i.sick() ? 0xFF8080 : 0xFFFFFF;
            g.drawString(font, s, lx + 2, y + 2, color, false);
        }
        TankView.Inhabitant sel = selectedInhabitant(v);
        if (sel == null) return;
        int x = 114, y = 19;
        g.drawString(font, font.plainSubstrByWidth(inhabitantName(sel).getString(), 124), x, y, 0xFFFFFF, false);
        y += 9;
        if (!sel.betta()) {
            line(g, Component.translatable(sel.typeKey()).withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT);
            y += 10;
            line(g, Component.translatable("gui.bettacraft.fish.foreign"), x, y, 0xA0A0A0);
            return;
        }
        MutableComponent stage = Component.translatable("stage.bettacraft." + sel.stage());
        if (!sel.adult()) stage = Component.translatable("gui.bettacraft.fish.stage_progress", stage, Math.round(sel.progress() * 100));
        line(g, Component.translatable("gui.bettacraft.fish.sex_stage", BettaTooltip.sex(sel.female()), stage), x, y, TEXT_LIGHT);
        y += 9;
        float hp = Math.min(sel.health(), sel.hp());
        bar(g, x, y, Component.translatable(sel.sick() ? "gui.bettacraft.fish.sick" : "gui.bettacraft.fish.health"), hp,
                hp > 0.5F ? 0xFF50C050 : 0xFFD04040);
        y += 9;
        bar(g, x, y, Component.translatable("gui.bettacraft.fish.satiation"), sel.satiation(), 0xFFD0A040);
        y += 10;
        var gt = sel.genotype();
        int top = y;
        for (Locus l : List.of(BettaLoci.FINFORM, BettaLoci.BODY_COLOR, BettaLoci.FIN_COLOR, BettaLoci.PATTERN, BettaLoci.IRIDESCENCE)) {
            Component c = GenotypeText.line(gt, l);
            String s = font.plainSubstrByWidth(c.getString(), 124);
            g.drawString(font, s, x, y, 0xC8C8C8, false);
            y += 9;
        }
        if (mx >= x && mx < x + 124 && my >= top && my < y) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("gui.bettacraft.fish.genotype").withStyle(ChatFormatting.YELLOW));
            for (Locus l : BettaLoci.ALL) tip.add(GenotypeText.line(gt, l));
            tip.add(Component.translatable("gui.bettacraft.fish.generation", sel.generation()).withStyle(ChatFormatting.GRAY));
            tip.add(Component.translatable("gui.bettacraft.fish.genotype_hint").withStyle(ChatFormatting.DARK_GRAY));
            pendingTooltip = tip;
        }
    }

    private void bar(GuiGraphics g, int x, int y, Component label, float value, int color) {
        g.drawString(font, label, x, y, TEXT_LIGHT, false);
        int bx = x + 62, bw = 60;
        g.fill(bx, y + 1, bx + bw, y + 7, 0xFF202020);
        g.fill(bx, y + 1, bx + Math.round(bw * Mth.clamp(value, 0, 1)), y + 7, color);
    }

    private List<TankView.LogLine> filteredLog(TankView v) {
        if (logFilter == LogFilter.ALL) return v.log;
        List<TankView.LogLine> out = new ArrayList<>();
        for (TankView.LogLine l : v.log) if (LogStyle.category(l.key()) == logFilter.ordinal()) out.add(l);
        return out;
    }

    private void drawLog(GuiGraphics g, TankView v, int mx, int my) {
        int x = 11, y = 21;
        List<TankView.LogLine> lines = filteredLog(v);
        if (lines.isEmpty()) {
            line(g, Component.translatable("gui.bettacraft.log.empty"), x, y, 0xA0A0A0);
        }
        for (int r = 0; r < LOG_ROWS; r++) {
            int idx = lines.size() - 1 - logScroll - r;
            if (idx < 0) break;
            TankView.LogLine l = lines.get(idx);
            int ly = y + r * 10;
            int color = LogStyle.color(l.key());
            g.fill(x, ly + 2, x + 3, ly + 5, 0xFF000000 | color);
            Component c = Component.translatable("gui.bettacraft.log.line", l.time() / 24000L + 1, TankActions.logLine(l));
            g.drawString(font, font.plainSubstrByWidth(c.getString(), 216), x + 6, ly, color, false);
            if (my >= ly && my < ly + 10 && mx >= x && mx < x + 226 && font.width(c) > 216) pendingTooltip = List.of(c);
        }
        scrollbar(g, lines.size(), LOG_ROWS, logScroll, true);
        g.drawString(font, Component.translatable("gui.bettacraft.log.count", lines.size()), x, 128, 0x909090, false);
    }

    private void drawBreeding(GuiGraphics g, TankView v, int mx, int my) {
        int x = 11, y = 20;
        List<Component> lines = BreedingLines.build(v);
        breedScroll = Mth.clamp(breedScroll, 0, Math.max(0, lines.size() - BREED_ROWS));
        for (int r = 0; r < BREED_ROWS; r++) {
            int idx = breedScroll + r;
            if (idx >= lines.size()) break;
            Component c = lines.get(idx);
            int ly = y + r * 10;
            g.drawString(font, net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(c, 220)), x, ly, TEXT_LIGHT, false);
            if (my >= ly && my < ly + 10 && mx >= x && mx < x + 226 && font.width(c) > 220) pendingTooltip = List.of(c);
        }
        scrollbar(g, lines.size(), BREED_ROWS, breedScroll, false);
    }

    /** Thin scroll indicator at the right edge of the content area. */
    private void scrollbar(GuiGraphics g, int total, int rows, int scroll, boolean fromBottom) {
        if (total <= rows) return;
        int top = CONTENT_Y + 2, h = CONTENT_H - 4, x = CONTENT_X + CONTENT_W - 4;
        int bh = Math.max(8, h * rows / total);
        int maxScroll = total - rows;
        float f = maxScroll <= 0 ? 0 : scroll / (float) maxScroll;
        if (fromBottom) f = 1 - f;
        int by = top + Math.round((h - bh) * f);
        g.fill(x, top, x + 2, top + h, 0x40000000);
        g.fill(x, by, x + 2, by + bh, 0xFFA0A0A0);
    }

    static String dirKey(Direction d) {
        return "gui.bettacraft.direction." + d.getSerializedName();
    }

    static Component signPos(SignPosition p) {
        return Component.translatable(p.translationKey());
    }
}
