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
import net.minecraft.nbt.CompoundTag;
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

/** Tank menu screen with the tabs Overview, Inhabitants, Breeding, Water (M9), Plants (M12), Interior (M3) and Log (PLAN 8). */
public class TankScreen extends AbstractContainerScreen<TankMenu> {
    private static final ResourceLocation TEXTURE = BettaCraft.id("textures/gui/tank.png");
    private static final int TEXT = 0x404040;
    private static final int TEXT_LIGHT = 0xE0E0E0;
    private static final int CONTENT_X = 7, CONTENT_Y = 17, CONTENT_W = 234, CONTENT_H = 123;
    private static final int LIST_ROWS = 9, ROW_H = 11, LOG_ROWS = 10, BREED_ROWS = 12;
    /** Bars of the water quality and the plant cover (right of their line). */
    private static final int BAR_X = 168, BAR_W = 66;
    /** Left edge of the overview's interior button: the last two overview lines end before it. */
    private static final int ENTER_X = 176;

    /** Order of the constants = index of {@link #selectTabForTest}; the buttons are drawn in a different order. */
    private enum Tab { OVERVIEW, INHABITANTS, BREEDING, LOG, INTERIOR, WATER, PLANTS }

    /** Scale of the tab labels: seven tabs fit into the 236 px of the menu (also the longer German labels). */
    public static final float TAB_LABEL_SCALE = 0.75F;

    /** Log filter: all, breeding (nest, eggs, fry, growth), health (sick, deaths, fights), feeding. */
    private enum LogFilter { ALL, BREEDING, HEALTH, FEEDING }

    private Tab tab = Tab.OVERVIEW;
    private final List<AbstractWidget> overview = new ArrayList<>();
    private final List<AbstractWidget> inhabitants = new ArrayList<>();
    private Button tabOverview, tabInhabitants, tabBreeding, tabLog, tabInterior, tabWater, tabPlants, logFilterButton, interiorEnterButton;
    private Button thinButton;
    /** M13: the plants tab shows the aquarium plants of the interior instead of the floating plants. */
    private boolean aquaticPage;
    private Button plantPageButton;
    /** M14: the animals tab shows the shrimp, crayfish and snails instead of the fish and frogs. */
    private boolean critterPage;
    private Button critterPageButton, critterTakeButton, clutchButton;
    /** M19: takes the breeding box out (breeding tab). */
    private Button boxButton;
    @Nullable
    private String selectedGroup;
    private int groupScroll;
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

    /** Sends a name edit that is still waiting for the debounce; returns the sent name or null. */
    private String flushName() {
        if (nameBox == null || nameDirtyTicks <= 0 || sentName == null) return null;
        String v = nameBox.getValue();
        nameDirtyTicks = -1;
        if (v.equals(sentName)) return null;
        sentName = v;
        send(TankActions.SET_NAME, 0, v, null);
        return v;
    }

    @Override
    protected void init() {
        // M17: a resize calls init again; a name typed just before must neither be lost nor overwritten by the old one
        String pendingName = flushName();
        super.init();
        overview.clear();
        inhabitants.clear();
        int x = leftPos, y = topPos;
        // seven tabs across the top (the title moved into the overview: no room for both) with labels in a smaller font;
        // widths follow the scaled labels, the rest of the 236 px is shared out
        Tab[] order = {Tab.OVERVIEW, Tab.INHABITANTS, Tab.BREEDING, Tab.WATER, Tab.PLANTS, Tab.INTERIOR, Tab.LOG};
        String[] keys = {"overview", "inhabitants", "breeding", "water", "plants", "interior", "log"};
        int[] labels = new int[order.length];
        for (int i = 0; i < order.length; i++) labels[i] = font.width(Component.translatable("gui.bettacraft.tab." + keys[i]));
        int[] widths = tabWidths(labels, TAB_LABEL_SCALE, 236);
        int tx = x + 5;
        Button[] tabs = new Button[order.length];
        for (int i = 0; i < order.length; i++) {
            Tab t = order[i];
            tabs[i] = addRenderableWidget(new TabButton(tx, y + 3, widths[i], 12, Component.translatable("gui.bettacraft.tab." + keys[i]), b -> setTab(t)));
            tx += widths[i];
        }
        tabOverview = tabs[0];
        tabInhabitants = tabs[1];
        tabBreeding = tabs[2];
        tabWater = tabs[3];
        tabPlants = tabs[4];
        tabInterior = tabs[5];
        tabLog = tabs[6];
        interiorEnterButton = addRenderableWidget(new FitButton(x + 176, y + 124, 62, 14, Component.translatable("gui.bettacraft.tank.enter"),
                b -> send(TankActions.ENTER_INTERIOR, 0, "", null)));
        thinButton = addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.plants.thin"), b -> send(TankActions.THIN_PLANTS, 0, "", null))
                .bounds(x + 166, y + 124, 72, 14).build());
        plantPageButton = addRenderableWidget(new FitButton(x + 92, y + 124, 72, 14, Component.empty(), b -> {
            aquaticPage = !aquaticPage;
            updateWidgets();
        }));
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
        // sign, sign position and front side share the row; labels that are still too long are drawn smaller, not scrolled
        signButton = addRenderableWidget(new FitButton(x + 10, y + 38, 62, 14, Component.empty(), b -> send(TankActions.TOGGLE_SIGN, 0, "", null)));
        signPosButton = addRenderableWidget(new FitButton(x + 74, y + 38, 82, 14, Component.empty(), b -> {
            TankView v = view();
            if (v != null) send(TankActions.SET_SIGN_POS, v.signPos.next().ordinal(), "", null);
        }));
        frontButton = addRenderableWidget(new FitButton(x + 158, y + 38, 80, 14, Component.empty(), b -> {
            TankView v = view();
            if (v != null) send(TankActions.SET_FRONT, v.front.getClockWise().get2DDataValue(), "", null);
        }));
        minusButton = addRenderableWidget(Button.builder(Component.literal("-"), b -> changeTarget(-1))
                .bounds(x + 168, y + 77, 14, 12).build());
        plusButton = addRenderableWidget(Button.builder(Component.literal("+"), b -> changeTarget(1))
                .bounds(x + 224, y + 77, 14, 12).build());
        enterButton = addRenderableWidget(new FitButton(x + ENTER_X, y + 123, 238 - ENTER_X, 14, Component.translatable("gui.bettacraft.tank.enter"),
                b -> send(TankActions.ENTER_INTERIOR, 0, "", null)));
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
        // M14: second page of the animals tab (shrimp, crayfish, snails)
        critterPageButton = addRenderableWidget(new FitButton(x + 10, y + 124, 100, 14, Component.empty(), b -> {
            critterPage = !critterPage;
            updateWidgets();
        }));
        critterTakeButton = addRenderableWidget(new FitButton(x + 114, y + 124, 62, 14, Component.translatable("gui.bettacraft.critters.take"), b -> {
            if (selectedGroup != null) send(TankActions.TAKE_CRITTER, hasControlDown() ? 2 : hasShiftDown() ? 1 : 0, selectedGroup, null);
        }));
        clutchButton = addRenderableWidget(new FitButton(x + 177, y + 124, 61, 14, Component.translatable("gui.bettacraft.critters.clutches"),
                b -> send(TankActions.REMOVE_CLUTCHES, 0, "", null)));
        boxButton = addRenderableWidget(new FitButton(x + 150, y + 124, 88, 14, Component.translatable("gui.bettacraft.breeding.tm.box_out"),
                b -> send(TankActions.REMOVE_BREEDING_BOX, 0, "", null)));
        sentName = null;
        updateWidgets();
        if (pendingName != null && nameBox != null) {
            nameBox.setValue(pendingName);
            sentName = pendingName;
        }
    }

    @Override
    public void onClose() {
        // M17: closing sends the close packet before removed() runs, so an edit flushed there arrived too late
        flushName();
        super.onClose();
    }

    /** Dev smoke test only: the floating plants tab (M12). */
    public void showPlantsPageForTest() {
        aquaticPage = false;
        setTab(Tab.PLANTS);
    }

    /** Dev smoke test only (M18): gui position of the animals line of the overview, where the stocking tooltip shows. */
    public int[] stockingLineForTest() {
        return new int[]{stockLineX, stockLineY};
    }

    private int stockLineX, stockLineY;

    /** Dev smoke test only. */
    public void selectTabForTest(int index) {
        setTab(Tab.values()[index]);
    }

    /** Dev smoke test only: scrolls the breeding tab (clamped to its rows when drawn). */
    public void scrollBreedingForTest(int rows) {
        breedScroll = rows;
    }

    /** Dev smoke test only: presses the "Thin out" button of the plants tab like a click (false when it is not active). */
    public boolean pressThinForTest() {
        if (tab != Tab.PLANTS || !thinButton.visible || !thinButton.active) return false;
        thinButton.onPress();
        return true;
    }

    /** Dev smoke test only: the tab buttons from left to right (to check that they fit). */
    public List<Button> tabButtonsForTest() {
        return List.of(tabOverview, tabInhabitants, tabBreeding, tabWater, tabPlants, tabInterior, tabLog);
    }

    /**
     * Widths of the tab buttons for the label widths {@code labels} (unscaled font pixels) drawn at {@code scale}: each
     * label plus a margin, the rest of {@code total} shared out evenly; when they do not fit they shrink in proportion.
     * The widths always add up to {@code total}.
     */
    public static int[] tabWidths(int[] labels, float scale, int total) {
        int n = labels.length;
        int[] w = new int[n];
        int sum = 0;
        for (int i = 0; i < n; i++) sum += w[i] = (int) Math.ceil(labels[i] * scale) + 5;
        int left = total - sum;
        for (int i = 0; i < n; i++) w[i] = left >= 0 ? w[i] + left / n + (i < left % n ? 1 : 0) : Math.max(12, w[i] * total / sum);
        int used = 0;
        for (int i = 0; i < n - 1; i++) used += w[i];
        w[n - 1] = total - used;
        return w;
    }

    /** A tab button whose label is drawn in the smaller tab font, centred (cut with "..." only if it still does not fit). */
    public static final class TabButton extends Button {
        TabButton(int x, int y, int w, int h, Component label, OnPress onPress) {
            super(x, y, w, h, label, onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderString(GuiGraphics g, net.minecraft.client.gui.Font font, int color) {
            float s = TAB_LABEL_SCALE;
            int room = (int) ((getWidth() - 3) / s);
            net.minecraft.network.chat.FormattedText text = getMessage();
            if (font.width(text) > room) {
                text = net.minecraft.network.chat.FormattedText.composite(font.substrByWidth(text, room - font.width("...")),
                        net.minecraft.network.chat.FormattedText.of("..."));
            }
            net.minecraft.util.FormattedCharSequence seq = net.minecraft.locale.Language.getInstance().getVisualOrder(text);
            g.pose().pushPose();
            g.pose().translate(getX() + getWidth() / 2.0F, getY() + (getHeight() - 8 * s) / 2.0F + 0.5F, 0);
            g.pose().scale(s, s, 1);
            g.drawString(font, seq, -font.width(seq) / 2, 0, color, true);
            g.pose().popPose();
        }
    }

    /** A button whose label is drawn smaller when it does not fit (instead of vanilla's scrolling text), centred. */
    public static final class FitButton extends Button {
        FitButton(int x, int y, int w, int h, Component label, OnPress onPress) {
            super(x, y, w, h, label, onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderString(GuiGraphics g, net.minecraft.client.gui.Font font, int color) {
            int room = getWidth() - 4, w = font.width(getMessage());
            if (w <= room) {
                super.renderString(g, font, color);
                return;
            }
            float s = Math.max(0.5F, room / (float) w);
            g.pose().pushPose();
            g.pose().translate(getX() + getWidth() / 2.0F, getY() + (getHeight() - 8 * s) / 2.0F + 0.5F, 0);
            g.pose().scale(s, s, 1);
            g.drawString(font, getMessage(), -w / 2, 0, color, true);
            g.pose().popPose();
        }
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
        for (AbstractWidget w : inhabitants) w.visible = tab == Tab.INHABITANTS && !critterPage;
        critterPageButton.visible = tab == Tab.INHABITANTS;
        critterTakeButton.visible = tab == Tab.INHABITANTS && critterPage;
        clutchButton.visible = tab == Tab.INHABITANTS && critterPage && v != null && v.critters.clutches > 0;
        boxButton.visible = tab == Tab.BREEDING && v != null && v.breeding.getBoolean("box");
        if (v != null) boxButton.active = v.canModify;
        if (v != null) {
            critterPageButton.setMessage(critterPage ? Component.translatable("gui.bettacraft.critters.page_fish", v.inhabitants.size())
                    : Component.translatable("gui.bettacraft.critters.page", v.critters.total));
            if (selectedGroup == null || v.critters.groups.stream().noneMatch(gr -> gr.key().equals(selectedGroup))) {
                selectedGroup = v.critters.groups.isEmpty() ? null : v.critters.groups.getFirst().key();
            }
            critterTakeButton.active = v.canModify && selectedGroup != null && (v.hasBag || v.critters.total > 0);
            critterTakeButton.setTooltip(Tooltip.create(Component.translatable(!v.canModify ? "message.bettacraft.tank.locked"
                    : "gui.bettacraft.critters.take_tooltip")));
            clutchButton.active = v.canModify;
        }
        tabOverview.active = tab != Tab.OVERVIEW;
        tabInhabitants.active = tab != Tab.INHABITANTS;
        tabBreeding.active = tab != Tab.BREEDING;
        tabLog.active = tab != Tab.LOG;
        tabInterior.active = tab != Tab.INTERIOR;
        tabWater.active = tab != Tab.WATER;
        tabPlants.active = tab != Tab.PLANTS;
        thinButton.visible = tab == Tab.PLANTS && !aquaticPage;
        plantPageButton.visible = tab == Tab.PLANTS;
        plantPageButton.setX(leftPos + (aquaticPage ? 166 : 92));
        plantPageButton.setMessage(Component.translatable(aquaticPage ? "gui.bettacraft.plants.page_floating" : "gui.bettacraft.plants.page_aquatic"));
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
        thinButton.active = mod && v.plants.total - v.plants.counts[org.overengineer.bettacraft.tank.plants.FloatingPlantType.LILY_PAD.ordinal()] > 0;
        thinButton.setTooltip(Tooltip.create(Component.translatable(mod ? "gui.bettacraft.plants.thin_tooltip" : "message.bettacraft.tank.locked")));

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
        bagButton.active = has && mod && v.hasBag && v.waterMb >= v.bagMb;
        bucketButton.setTooltip(Tooltip.create(takeTooltip(v, sel, false)));
        bagButton.setTooltip(Tooltip.create(takeTooltip(v, sel, true)));
    }

    private Component takeTooltip(TankView v, @Nullable TankView.Inhabitant sel, boolean bag) {
        long cost = bag ? v.bagMb : TankInhabitants.BUCKET_MB;
        if (sel == null) return Component.translatable("gui.bettacraft.fish.none_selected");
        if (!v.canModify) return Component.translatable("message.bettacraft.tank.locked");
        if (!bag && !sel.bucketable()) return Component.translatable("gui.bettacraft.fish.not_bucketable"); // M16: bags take every animal
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
        flushName(); // fallback (e.g. another screen replaced this one)
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
        if (tab == Tab.INHABITANTS && v != null && critterPage) {
            int lx = leftPos + 10, ly = topPos + 20;
            if (mx >= lx && mx < lx + 100 && my >= ly && my < ly + LIST_ROWS * ROW_H) {
                int idx = groupScroll + (int) ((my - ly) / ROW_H);
                if (idx >= 0 && idx < v.critters.groups.size()) {
                    selectedGroup = v.critters.groups.get(idx).key();
                    updateWidgets();
                    return true;
                }
            }
        } else if (tab == Tab.INHABITANTS && v != null) {
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
            if (tab == Tab.INHABITANTS && critterPage) {
                groupScroll = Mth.clamp(groupScroll - (int) Math.signum(sy), 0, Math.max(0, v.critters.groups.size() - LIST_ROWS));
                return true;
            }
            if (tab == Tab.INHABITANTS) {
                listScroll = Mth.clamp(listScroll - (int) Math.signum(sy), 0, Math.max(0, v.inhabitants.size() - LIST_ROWS));
                return true;
            }
            if (tab == Tab.LOG) {
                logScroll = Mth.clamp(logScroll - (int) Math.signum(sy), 0, Math.max(0, filteredLog(v).size() - LOG_ROWS));
                return true;
            }
            if (tab == Tab.BREEDING) {
                breedScroll = Mth.clamp(breedScroll - (int) Math.signum(sy), 0, Math.max(0, breedRows(v).size() - breedRowCount()));
                return true;
            }
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    // ------------------------------------------------------------------ drawing

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        // old heater slots (M1-M3): a slot frame only while a heater is left in it (take-out only since M4)
        for (int i = 0; i < TankData.LEGACY_HEATER_SLOTS; i++) {
            if (menu.getSlot(i).hasItem()) g.blit(TEXTURE, leftPos + TankMenu.HEATER_X - 1, topPos + TankMenu.SLOT_Y + i * 18 - 1, 36, 232, 18, 18);
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
            case OVERVIEW -> drawOverview(g, v, mouseX - leftPos, mouseY - topPos);
            case INHABITANTS -> {
                if (critterPage) drawCritters(g, v, mouseX - leftPos, mouseY - topPos);
                else drawInhabitants(g, v, mouseX - leftPos, mouseY - topPos);
            }
            case BREEDING -> drawBreeding(g, v);
            case LOG -> drawLog(g, v, mouseX - leftPos, mouseY - topPos);
            case INTERIOR -> drawInterior(g, v, mouseX - leftPos, mouseY - topPos);
            case WATER -> drawWater(g, v);
            case PLANTS -> {
                if (aquaticPage) drawAquatic(g, v);
                else drawPlants(g, v);
            }
        }
        // lock symbol left of the interior button (not on its label)
        if (v.locked && tab == Tab.OVERVIEW) g.blit(TEXTURE, ENTER_X - 12, 125, 16, 232, 10, 10);
    }

    /** A line of text that ends inside the content area (a longer translation is drawn a little smaller). */
    private void line(GuiGraphics g, Component c, int x, int y, int color) {
        fitLine(g, c, x, y, color, CONTENT_X + CONTENT_W - 3 - x);
    }

    /** Text at (x, y), scaled down (never up) to at most {@code maxWidth} pixels. */
    private void fitLine(GuiGraphics g, Component c, int x, int y, int color, int maxWidth) {
        int w = font.width(c);
        if (w <= maxWidth || w <= 0) {
            g.drawString(font, c, x, y, color, false);
            return;
        }
        float s = Math.max(0.5F, maxWidth / (float) w);
        g.pose().pushPose();
        g.pose().translate(x, y + (1 - s) * 4, 0);
        g.pose().scale(s, s, 1);
        g.drawString(font, c, 0, 0, color, false);
        g.pose().popPose();
    }

    private void drawOverview(GuiGraphics g, TankView v, int mx, int my) {
        int x = 11, y = 56;
        int pct = v.capacityMb <= 0 ? 0 : (int) (v.waterMb * 100 / v.capacityMb);
        line(g, Component.translatable("gui.bettacraft.tank.size_water", v.sizeX, v.sizeY, v.sizeZ, v.waterMb, v.capacityMb, pct), x, y, TEXT_LIGHT);
        y += 11;
        int tempColor = TankTemperature.isTolerable(v.waterTemp) ? (TankTemperature.isOptimal(v.waterTemp) ? 0x80FF80 : 0xFFE080) : 0xFF7070;
        line(g, Component.translatable("gui.bettacraft.tank.temperature", fmt(v.waterTemp), fmt(v.ambient)), x, y, tempColor);
        y += 11;
        y += 1;
        line(g, Component.translatable("gui.bettacraft.tank.target"), x, y + 2, TEXT_LIGHT);
        String t = fmt(v.targetTemp) + " °C";
        g.drawCenteredString(font, t, 203, y + 2, 0xFFFFFF);
        y += 13;
        double water = Math.max(0.001, v.waterMb / 1000.0);
        // M9: below the ambient temperature the coolers matter, above it the heaters
        boolean cooling = v.targetTemp < v.ambient;
        double cap = cooling ? v.coolerBlocks : v.heaterBlocks;
        Component heat;
        if (cap <= 0) heat = Component.translatable("gui.bettacraft.tank.heaters_none").withStyle(ChatFormatting.GRAY);
        else if (cap + 1e-6 >= water) heat = Component.translatable("gui.bettacraft.tank.heaters_ok", (int) cap, fmt((float) water)).withStyle(ChatFormatting.GREEN);
        else heat = Component.translatable("gui.bettacraft.tank.heaters_weak", (int) cap, fmt((float) water)).withStyle(ChatFormatting.GOLD);
        line(g, Component.translatable(cooling ? "gui.bettacraft.tank.coolers" : "gui.bettacraft.tank.heaters", heat), x, y, TEXT_LIGHT);
        y += 10;
        Component feeder = v.feederFood < 0 ? Component.translatable("gui.bettacraft.tank.none").withStyle(ChatFormatting.GRAY)
                : Component.translatable("gui.bettacraft.tank.feeder_state", v.feederFood, v.feederPerDay)
                .withStyle(v.feederFood == 0 ? ChatFormatting.GOLD : ChatFormatting.GREEN);
        Component light = v.lights == 0 ? Component.translatable("gui.bettacraft.tank.none").withStyle(ChatFormatting.GRAY)
                : Component.translatable("gui.bettacraft.tank.light_state", v.lightsLit, v.lights);
        line(g, Component.translatable("gui.bettacraft.tank.feeder", feeder).append("   ")
                .append(Component.translatable("gui.bettacraft.tank.light", light)), x, y, TEXT_LIGHT);
        y += 10;
        // M4: filters outside (blocks next to the tank, filters in cabinets below) and inside (interior)
        int outside = v.filterBlocks + v.cabinetFilters;
        Component filters = outside + v.filters == 0 ? Component.translatable("gui.bettacraft.tank.none").withStyle(ChatFormatting.GRAY)
                : Component.translatable("gui.bettacraft.tank.filter_state", outside, v.filterHeaters, v.filters).withStyle(ChatFormatting.AQUA);
        line(g, Component.translatable("gui.bettacraft.tank.filters", filters), x, y, TEXT_LIGHT);
        y += 10;
        // M18: as soon as anything counts in fractions (small animals, young, nest), the line shows the stocking
        boolean fractions = v.critters.total > 0 || v.stockYoung > 1e-6 || v.stockNest > 1e-6;
        MutableComponent inh = v.critters.total > 0
                ? Component.translatable("gui.bettacraft.tank.stocking", v.inhabitants.size(), v.critters.total, fmt2(v.stock), v.hardCap)
                : fractions ? Component.translatable("gui.bettacraft.tank.stocking_fish", v.inhabitants.size(), fmt2(v.stock), v.hardCap)
                : Component.translatable("gui.bettacraft.tank.inhabitants", v.inhabitants.size(), v.adults, v.hardCap);
        if (v.crowded) inh.append(" ").append(Component.translatable("gui.bettacraft.tank.crowded").withStyle(ChatFormatting.RED));
        // the last two lines end left of the interior button (and the lock symbol)
        int room = ENTER_X - (v.locked ? 15 : 3) - x;
        fitLine(g, inh, x, y, TEXT_LIGHT, room);
        if (mx >= x && mx < x + room && my >= y && my < y + 9) pendingTooltip = stockingTooltip(v);
        stockLineX = leftPos + x + 20;
        stockLineY = topPos + y + 4;
        y += 10;
        Component unpowered = unpowered(v);
        if (v.waterMb < v.minInsertMb) {
            fitLine(g, Component.translatable("gui.bettacraft.tank.fill_hint", v.minInsertMb).withStyle(ChatFormatting.GOLD), x, y, TEXT_LIGHT, room);
        } else if (v.water.qualityEnabled && v.water.quality < org.overengineer.bettacraft.tank.WaterQuality.POOR_BELOW) {
            fitLine(g, Component.translatable(v.water.quality < v.water.badBelow ? "gui.bettacraft.water.warn_bad" : "gui.bettacraft.water.warn_poor",
                    Math.round(v.water.quality * 100)).withStyle(v.water.quality < v.water.badBelow ? ChatFormatting.RED : ChatFormatting.GOLD), x, y, TEXT_LIGHT, room);
        } else if (unpowered != null) {
            fitLine(g, unpowered, x, y, TEXT_LIGHT, room);
        } else if (v.legacyHeaters > 0) {
            fitLine(g, Component.translatable("gui.bettacraft.tank.legacy_heaters", v.legacyHeaters).withStyle(ChatFormatting.GOLD), x, y, TEXT_LIGHT, room);
        }
    }

    /** Warning line if some equipment lacks power (M9), else null. */
    @Nullable
    private static Component unpowered(TankView v) {
        TankView.Water w = v.water;
        List<Component> off = new ArrayList<>();
        if (w.heaterPower && !w.heatPowered) off.add(Component.translatable("gui.bettacraft.water.power_heat"));
        if (w.filterPower && !w.filterPowered) off.add(Component.translatable("gui.bettacraft.water.power_filter"));
        if (w.lightPower && !w.lightPowered) off.add(Component.translatable("gui.bettacraft.water.power_light"));
        if (off.isEmpty()) return null;
        MutableComponent list = off.getFirst().copy();
        for (int i = 1; i < off.size(); i++) list.append(", ").append(off.get(i));
        return Component.translatable("gui.bettacraft.water.no_power", list).withStyle(ChatFormatting.RED);
    }

    /** M9: water quality, water changes, evaporation, temperature equipment and power. */
    private void drawWater(GuiGraphics g, TankView v) {
        TankView.Water w = v.water;
        int x = 11, y = 20;
        int pct = v.capacityMb <= 0 ? 0 : (int) (v.waterMb * 100 / v.capacityMb);
        line(g, Component.translatable("gui.bettacraft.water.volume", v.waterMb, v.capacityMb, pct), x, y, TEXT_LIGHT);
        y += 11;
        // M11: what the tank is made of (glass and style)
        line(g, org.overengineer.bettacraft.compat.info.InfoLines.glassLine(v.glass, v.style.getSerializedName()), x, y, TEXT_LIGHT);
        y += 11;
        if (w.qualityEnabled) {
            ChatFormatting col = switch (w.state) {
                case "clean" -> ChatFormatting.GREEN;
                case "good" -> ChatFormatting.DARK_GREEN;
                case "poor" -> ChatFormatting.GOLD;
                default -> ChatFormatting.RED;
            };
            Component label = Component.translatable("gui.bettacraft.water.quality", Math.round(w.quality * 100),
                    Component.translatable("gui.bettacraft.water.state." + w.state).withStyle(col));
            int bx = BAR_X, bw = BAR_W;
            fitLine(g, label, x, y, TEXT_LIGHT, bx - 4 - x);
            g.fill(bx, y + 1, bx + bw, y + 7, 0xFF202020);
            int color = w.quality < w.badBelow ? 0xFFD04040 : w.quality < org.overengineer.bettacraft.tank.WaterQuality.POOR_BELOW ? 0xFFD0A040 : 0xFF50A0E0;
            g.fill(bx, y + 1, bx + Math.round(bw * Mth.clamp(w.quality, 0, 1)), y + 7, color);
            g.fill(bx + Math.round(bw * w.badBelow), y, bx + Math.round(bw * w.badBelow) + 1, y + 8, 0xFFFF6060);
            y += 11;
            line(g, Component.translatable("gui.bettacraft.water.decline", fmt((float) (w.declinePerDay * 100)), fmt((float) w.animalsPerBlock)), x, y, TEXT_LIGHT);
            y += 11;
            int outside = v.filterBlocks + v.cabinetFilters;
            Component filters = outside + w.internalFilters == 0 ? Component.translatable("gui.bettacraft.water.no_filter").withStyle(ChatFormatting.GOLD)
                    : Component.translatable("gui.bettacraft.water.filters", outside, w.internalFilters, (int) w.filterCapacity,
                    Math.round(w.filterShare * 100)).withStyle(ChatFormatting.AQUA);
            line(g, filters, x, y, TEXT_LIGHT);
            y += 11;
        } else {
            line(g, Component.translatable("gui.bettacraft.water.quality_off").withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT);
            y += 11;
        }
        Component fresh = w.lastFreshWater < 0 ? Component.translatable("gui.bettacraft.water.fresh_never")
                : Component.translatable("gui.bettacraft.water.fresh", w.lastFreshWater / 24000L + 1);
        line(g, fresh.copy().withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT);
        y += 11;
        Component evap;
        if (!w.evaporationEnabled) evap = Component.translatable("gui.bettacraft.water.evaporation_off").withStyle(ChatFormatting.GRAY);
        else if (w.openTop == 0) evap = Component.translatable("gui.bettacraft.water.covered").withStyle(ChatFormatting.GREEN);
        else evap = Component.translatable("gui.bettacraft.water.evaporation", fmt((float) w.evaporationPerDay), w.openTop, w.totalTop)
                    .withStyle(ChatFormatting.GOLD);
        line(g, evap, x, y, TEXT_LIGHT);
        y += 11;
        line(g, Component.translatable("gui.bettacraft.water.temp_equipment", (int) v.heaterBlocks, (int) v.coolerBlocks), x, y, TEXT_LIGHT);
        y += 11;
        if (!w.anyPower()) {
            line(g, Component.translatable("gui.bettacraft.water.power_off").withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT);
        } else {
            line(g, Component.translatable("gui.bettacraft.water.power", String.format(java.util.Locale.ROOT, "%,d", w.buffer),
                    String.format(java.util.Locale.ROOT, "%,d", w.bufferCap), w.demand), x, y, TEXT_LIGHT);
            y += 11;
            MutableComponent groups = Component.empty();
            groups.append(powerGroup("power_heat", w.heaterPower, w.heatPowered)).append("  ")
                    .append(powerGroup("power_filter", w.filterPower, w.filterPowered)).append("  ")
                    .append(powerGroup("power_light", w.lightPower, w.lightPowered));
            line(g, groups, x, y, TEXT_LIGHT);
        }
        y += 13;
        line(g, Component.translatable("gui.bettacraft.water.hint").withStyle(ChatFormatting.GRAY), x, y, 0xA0A0A0);
    }

    /** M12: the floating plants tab. */
    private void drawPlants(GuiGraphics g, TankView v) {
        TankView.Plants p = v.plants;
        int x = 11, y = 20;
        line(g, Component.translatable("gui.bettacraft.plants.count", p.total, p.maxPlants), x, y, TEXT_LIGHT);
        y += 11;
        int cov = Math.round(p.coverage * 100);
        int bx = BAR_X, bw = BAR_W;
        fitLine(g, Component.translatable("gui.bettacraft.plants.coverage", Math.min(100, cov), Math.round(p.maxCoverage * 100)), x, y, TEXT_LIGHT, bx - 4 - x);
        g.fill(bx, y + 1, bx + bw, y + 7, 0xFF202020);
        g.fill(bx, y + 1, bx + Math.round(bw * Mth.clamp(p.coverage, 0, 1)), y + 7, p.dense ? 0xFFD0A040 : 0xFF5FB040);
        g.fill(bx + Math.round(bw * p.maxCoverage), y, bx + Math.round(bw * p.maxCoverage) + 1, y + 8, 0xFFE0E0E0);
        y += 11;
        var types = org.overengineer.bettacraft.tank.plants.FloatingPlantType.values();
        int col = 0;
        if (p.total == 0) {
            line(g, Component.translatable("gui.bettacraft.plants.none").withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT);
            y += 11;
        } else {
            for (var t : types) {
                if (p.counts[t.ordinal()] <= 0) continue;
                Component name = Component.translatable(org.overengineer.bettacraft.tank.plants.TankPlants.item(t).getDescriptionId());
                MutableComponent sp = Component.translatable("gui.bettacraft.plants.species", name, p.counts[t.ordinal()]).withStyle(ChatFormatting.GREEN);
                if (t == org.overengineer.bettacraft.tank.plants.FloatingPlantType.RED_ROOT_FLOATER) {
                    // red under strong light, green under weak light
                    double red = org.overengineer.bettacraft.tank.plants.FloatingPlantSim.redTarget(p.light);
                    // drawn in the colour the leaves have in this light (no room for more text)
                    sp = Component.translatable("gui.bettacraft.plants.species", name, p.counts[t.ordinal()])
                            .withStyle(red >= 0.75 ? ChatFormatting.RED : red > 0 ? ChatFormatting.GOLD : ChatFormatting.GREEN);
                }
                line(g, sp, x + col * 100, y, TEXT_LIGHT);
                if (++col == 2) {
                    col = 0;
                    y += 11;
                }
            }
            if (col != 0) y += 11;
        }
        int lc = org.overengineer.bettacraft.tank.plants.TankPlants.lightClass(p.light);
        String[] lk = {"dark", "weak", "good", "strong"};
        ChatFormatting[] lcol = {ChatFormatting.RED, ChatFormatting.GOLD, ChatFormatting.GREEN, ChatFormatting.AQUA};
        line(g, Component.translatable("gui.bettacraft.plants.light", p.light,
                Component.translatable("gui.bettacraft.plants.light." + lk[lc]).withStyle(lcol[lc])), x, y, TEXT_LIGHT);
        y += 11;
        Component growth;
        if (p.growthSpeed <= 0) growth = Component.translatable("gui.bettacraft.plants.growth_off").withStyle(ChatFormatting.GRAY);
        else if (lc == 0) growth = Component.translatable("gui.bettacraft.plants.too_dark").withStyle(ChatFormatting.GOLD);
        else if (p.coverage >= p.maxCoverage || p.total >= p.maxPlants) growth = Component.translatable("gui.bettacraft.plants.full").withStyle(ChatFormatting.GRAY);
        else growth = Component.translatable("gui.bettacraft.plants.growing").withStyle(ChatFormatting.GREEN);
        line(g, growth, x, y, TEXT_LIGHT);
        y += 11;
        if (p.uptakePerDay > 0) {
            line(g, Component.translatable("gui.bettacraft.plants.uptake", fmt((float) (p.uptakePerDay * 100))).withStyle(ChatFormatting.AQUA), x, y, TEXT_LIGHT);
            y += 11;
        }
        if (p.dense) {
            line(g, Component.literal("⚠ ").append(Component.translatable("gui.bettacraft.plants.dense", Math.round(p.denseFrom * 100)))
                    .withStyle(ChatFormatting.GOLD), x, y, TEXT_LIGHT);
        }
        fitLine(g, Component.translatable("gui.bettacraft.plants.hint").withStyle(ChatFormatting.GRAY), x, 128, 0xA0A0A0,
                plantPageButton.getX() - leftPos - 4 - x);
    }

    /** M13: the aquarium plants of the interior (second page of the plants tab). */
    private void drawAquatic(GuiGraphics g, TankView v) {
        TankView.Aquatic a = v.aquatic;
        int x = 11, y = 20;
        int right = 238 - x;
        fitLine(g, Component.translatable("gui.bettacraft.aquatic.count", a.total, a.maxPlants), x, y, TEXT_LIGHT, right);
        y += 11;
        if (a.total == 0) {
            fitLine(g, Component.translatable(a.built ? "gui.bettacraft.aquatic.none" : "gui.bettacraft.aquatic.none_built").withStyle(ChatFormatting.GRAY),
                    x, y, TEXT_LIGHT, right);
            y += 11;
        } else {
            fitLine(g, Component.translatable("gui.bettacraft.aquatic.mass", String.format(java.util.Locale.ROOT, "%,d", a.blocks),
                    fmt(a.share * 100)), x, y, TEXT_LIGHT, right);
            y += 11;
            var species = org.overengineer.bettacraft.plants.PlantSpecies.values();
            int col = 0, rows = 0, shown = 0, kinds = 0;
            for (var s : species) if (a.counts[s.ordinal()] > 0) kinds++;
            for (var s : species) {
                int n = a.counts[s.ordinal()];
                if (n <= 0) continue;
                if (rows >= 3 && col == 0 && kinds - shown > 2) {
                    fitLine(g, Component.translatable("gui.bettacraft.aquatic.more", kinds - shown).withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT, right);
                    y += 11;
                    break;
                }
                Component name = Component.translatable(org.overengineer.bettacraft.plants.InteriorPlants.item(s, 0).getDescriptionId());
                MutableComponent sp = Component.translatable("gui.bettacraft.plants.species", name, n);
                int color = org.overengineer.bettacraft.plants.PlantColors.color(s,
                        s.red() == org.overengineer.bettacraft.plants.PlantSpecies.Red.TIPS ? org.overengineer.bettacraft.plants.PlantPart.TOP
                                : org.overengineer.bettacraft.plants.PlantPart.LEAF, a.tints[s.ordinal()]);
                // drawn in the colour the leaves have now, lightened for the dark background
                fitLine(g, sp, x + col * 112, y, lighten(color), 108);
                shown++;
                if (++col == 2) {
                    col = 0;
                    rows++;
                    y += 11;
                }
            }
            if (col != 0) y += 11;
        }
        int lc = org.overengineer.bettacraft.tank.plants.TankPlants.lightClass(a.light);
        String[] lk = {"dark", "weak", "good", "strong"};
        ChatFormatting[] lcol = {ChatFormatting.RED, ChatFormatting.GOLD, ChatFormatting.GREEN, ChatFormatting.AQUA};
        fitLine(g, Component.translatable("gui.bettacraft.plants.light", a.light,
                Component.translatable("gui.bettacraft.plants.light." + lk[lc]).withStyle(lcol[lc])), x, y, TEXT_LIGHT, right);
        y += 11;
        if (a.total > 0) {
            Component growth;
            if (a.growthSpeed <= 0) growth = Component.translatable("gui.bettacraft.plants.growth_off").withStyle(ChatFormatting.GRAY);
            else if (a.temp < 18 || a.temp > 32) growth = Component.translatable("gui.bettacraft.aquatic.temp", fmt(a.temp)).withStyle(ChatFormatting.GOLD);
            else {
                MutableComponent m = Component.translatable("gui.bettacraft.aquatic.growing", a.growing, a.fullGrown).withStyle(ChatFormatting.GREEN);
                if (a.dark > 0) m.append(" ").append(Component.translatable("gui.bettacraft.aquatic.dark", a.dark).withStyle(ChatFormatting.GOLD));
                if (a.dry > 0) m.append(" ").append(Component.translatable("gui.bettacraft.aquatic.dry", a.dry).withStyle(ChatFormatting.GOLD));
                growth = m;
            }
            fitLine(g, growth, x, y, TEXT_LIGHT, right);
            y += 11;
            if (a.uptakePerDay > 0) {
                fitLine(g, Component.translatable("gui.bettacraft.plants.uptake", fmt((float) (a.uptakePerDay * 100))).withStyle(ChatFormatting.AQUA),
                        x, y, TEXT_LIGHT, right);
                y += 11;
            }
            MutableComponent hide = Component.translatable("gui.bettacraft.aquatic.hiding", Math.round(a.hiding * 100));
            if (a.bonus) hide.append("  ").append(Component.translatable("gui.bettacraft.aquatic.bonus").withStyle(ChatFormatting.GREEN));
            fitLine(g, hide, x, y, TEXT_LIGHT, right);
            y += 11;
            if (a.overgrown && y < 118) {
                fitLine(g, Component.literal("⚠ ").append(Component.translatable("gui.bettacraft.aquatic.overgrown", Math.round(a.surfaceCover * 100)))
                        .withStyle(ChatFormatting.GOLD), x, y, TEXT_LIGHT, right);
            }
        }
        fitLine(g, Component.translatable("gui.bettacraft.aquatic.hint").withStyle(ChatFormatting.GRAY), x, 128, 0xA0A0A0,
                plantPageButton.getX() - leftPos - 4 - x);
    }

    private static int lighten(int rgb) {
        int r = (rgb >> 16) & 0xFF, gg = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        r = r + (255 - r) * 2 / 5;
        gg = gg + (255 - gg) * 2 / 5;
        b = b + (255 - b) * 2 / 5;
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }

    /** Dev smoke test only: the page of the small animals in the animals tab (M14). */
    public void showCrittersPageForTest() {
        critterPage = true;
        setTab(Tab.INHABITANTS);
    }

    /** Dev smoke test only: the fish page of the animals tab with the first frog selected (M14); false without a frog. */
    public boolean showFrogForTest() {
        TankView v = view();
        if (v == null) return false;
        for (TankView.Inhabitant i : v.inhabitants) {
            if (!i.frog()) continue;
            critterPage = false;
            selected = i.uuid();
            setTab(Tab.INHABITANTS);
            return true;
        }
        return false;
    }

    /** Dev smoke test only: presses "take one" of the small animals page (false when it is not there). */
    public boolean pressTakeCritterForTest() {
        if (tab != Tab.INHABITANTS || !critterPage || !critterTakeButton.visible || !critterTakeButton.active) return false;
        critterTakeButton.onPress();
        return true;
    }

    /** Dev smoke test only: the aquarium plants page of the plants tab. */
    public void showAquaticPageForTest() {
        aquaticPage = true;
        setTab(Tab.PLANTS);
    }

    private static Component powerGroup(String key, boolean required, boolean ok) {
        MutableComponent c = Component.translatable("gui.bettacraft.water." + key);
        if (!required) return c.append(": ").append(Component.translatable("gui.bettacraft.water.power_free")).withStyle(ChatFormatting.GRAY);
        return c.append(ok ? " ✔" : " ✘").withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED);
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
            if (v.coolersInside > 0) heat.append("  ").append(Component.translatable("gui.bettacraft.interior.coolers", v.coolersInside, v.coolerCapWet));
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

    /** M18: two decimals below 10 (a few fry count 0.02 each), one above. */
    static String fmt2(double d) {
        return String.format(java.util.Locale.ROOT, d < 10 - 0.005 ? "%.2f" : "%.1f", d);
    }

    /** M18: the parts of the stocking (hover over the animals line of the overview). */
    private static List<Component> stockingTooltip(TankView v) {
        List<Component> tip = new ArrayList<>();
        tip.add(Component.translatable("gui.bettacraft.stock.title", fmt2(v.stock), v.hardCap));
        tip.add(Component.translatable("gui.bettacraft.stock.adults", v.adults, fmt2(v.stockAdults)).withStyle(ChatFormatting.GRAY));
        if (v.young > 0) tip.add(Component.translatable("gui.bettacraft.stock.young", v.young, fmt2(v.stockYoung)).withStyle(ChatFormatting.GRAY));
        if (v.critters.total > 0) tip.add(Component.translatable("gui.bettacraft.stock.critters", v.critters.total, fmt2(v.stockCritters)).withStyle(ChatFormatting.GRAY));
        if (v.stockNest > 1e-6) tip.add(Component.translatable("gui.bettacraft.stock.nest", fmt2(v.stockNest)).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("gui.bettacraft.stock.hint", fmt2(org.overengineer.bettacraft.entity.betta.LifeStage.FRY.stocking()),
                fmt2(org.overengineer.bettacraft.entity.betta.LifeStage.JUVENILE.stocking()), fmt2(org.overengineer.bettacraft.entity.betta.LifeStage.SUBADULT.stocking()))
                .withStyle(ChatFormatting.DARK_GRAY));
        return tip;
    }

    private Component inhabitantName(TankView.Inhabitant i) {
        if (!i.customName().isEmpty()) return Component.literal(i.customName());
        // M5: unnamed bettas are listed by their type
        return i.betta() ? BettaTooltip.type(i.genome()) : Component.translatable(i.typeKey());
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
            String sym = i.betta() || i.isTankmate() ? (i.female() ? "♀ " : "♂ ") : "";
            Component name = Component.literal(sym).append(inhabitantName(i));
            String s = font.plainSubstrByWidth(name.getString(), 90);
            int color = i.sick() ? 0xFF8080 : 0xFFFFFF;
            g.drawString(font, s, lx + 2, y + 2, color, false);
        }
        TankView.Inhabitant sel = selectedInhabitant(v);
        if (sel == null) return;
        int x = 114, y = 19;
        Component header = sel.customName().isEmpty() ? Component.translatable(sel.typeKey()) : Component.literal(sel.customName());
        g.drawString(font, font.plainSubstrByWidth(header.getString(), 124), x, y, 0xFFFFFF, false);
        y += 9;
        if (sel.frog()) {
            // M14: dwarf clawed frog: health, food, air
            line(g, Component.translatable(sel.typeKey()).withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT);
            y += 10;
            bar(g, x, y, Component.translatable("gui.bettacraft.fish.health"), sel.hp(), sel.hp() > 0.5F ? 0xFF50C050 : 0xFFD04040);
            y += 9;
            bar(g, x, y, Component.translatable("gui.bettacraft.fish.satiation"), sel.satiation(), 0xFFD0A040);
            y += 9;
            bar(g, x, y, Component.translatable("gui.bettacraft.frog.air"), sel.air(), 0xFF60A0E0);
            y += 11;
            if (sel.noAir()) {
                fitLine(g, Component.translatable("gui.bettacraft.frog.no_air").withStyle(ChatFormatting.RED), x, y, TEXT_LIGHT, 124);
                y += 10;
            }
            fitLine(g, Component.translatable("gui.bettacraft.frog.hint").withStyle(ChatFormatting.GRAY), x, y, 0xA0A0A0, 124);
            y += 9;
            fitLine(g, Component.translatable("gui.bettacraft.frog.hint2").withStyle(ChatFormatting.GRAY), x, y, 0xA0A0A0, 124);
            return;
        }
        if (sel.isTankmate()) {
            drawTankmate(g, sel, x, y, mx, my);
            return;
        }
        if (!sel.betta()) {
            line(g, Component.translatable(sel.typeKey()).withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT);
            y += 10;
            line(g, Component.translatable("gui.bettacraft.fish.foreign"), x, y, 0xA0A0A0);
            return;
        }
        MutableComponent stage = Component.translatable("stage.bettacraft." + sel.stage());
        if (!sel.adult()) stage = Component.translatable("gui.bettacraft.fish.stage_progress", stage, Math.round(sel.progress() * 100));
        Component type = BettaTooltip.type(sel.genome());
        String ts = font.plainSubstrByWidth(type.getString(), 124);
        g.drawString(font, ts, x, y, 0xFFD27A, false);
        if (mx >= x && mx < x + 124 && my >= y && my < y + 9 && !ts.equals(type.getString())) pendingTooltip = List.of(type);
        y += 9;
        line(g, Component.translatable("gui.bettacraft.fish.sex_stage", BettaTooltip.sex(sel.female()), stage), x, y, TEXT_LIGHT);
        if (mx >= x && mx < x + 124 && my >= y && my < y + 9) {
            // M18: how much this betta counts for the stocking (fry 0.02, juvenile 0.1, subadult 0.3 by default)
            pendingTooltip = List.of(Component.translatable("gui.bettacraft.fish.counts", fmt2(org.overengineer.bettacraft.entity.betta.LifeStage.byId(sel.stage()).stocking())));
        }
        y += 9;
        float hp = Math.min(sel.health(), sel.hp());
        bar(g, x, y, Component.translatable(sel.sick() ? "gui.bettacraft.fish.sick" : "gui.bettacraft.fish.health"), hp,
                hp > 0.5F ? 0xFF50C050 : 0xFFD04040);
        y += 9;
        bar(g, x, y, Component.translatable("gui.bettacraft.fish.satiation"), sel.satiation(), 0xFFD0A040);
        y += 10;
        var gt = sel.genotype();
        int top = y;
        for (Locus l : List.of(BettaLoci.FINFORM, BettaLoci.BODY_COLOR, BettaLoci.FIN_COLOR, BettaLoci.PATTERN)) {
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

    /** M19: a tankmate: species or variety, sex and stage, health, food, its shoal, temperature range, stress and breeding state. */
    private void drawTankmate(GuiGraphics g, TankView.Inhabitant sel, int x, int y, int mx, int my) {
        CompoundTag d = sel.tankmate();
        var s = org.overengineer.bettacraft.tankmates.TankmateSpecies.byId(d.getString("species"));
        if (s == null) return;
        Component sub = s.varieties().size() > 1 ? Component.translatable(s.varietyKey(d.getString("variety"))).withStyle(ChatFormatting.GOLD)
                : Component.literal(s.latin()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        fitLine(g, sub, x, y, 0xFFD27A, 124);
        if (mx >= x && mx < x + 124 && my >= y && my < y + 9) {
            pendingTooltip = List.of(Component.literal(s.latin()).withStyle(ChatFormatting.ITALIC),
                    Component.translatable("gui.bettacraft.tm.genes", d.getString("genes")).withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.bettacraft.fish.generation", sel.generation()).withStyle(ChatFormatting.GRAY));
        }
        y += 9;
        MutableComponent stage = Component.translatable("stage.bettacraft." + sel.stage());
        if (!sel.adult()) stage = Component.translatable("gui.bettacraft.fish.stage_progress", stage, Math.round(sel.progress() * 100));
        line(g, Component.translatable("gui.bettacraft.fish.sex_stage", BettaTooltip.sex(sel.female()), stage), x, y, TEXT_LIGHT);
        if (mx >= x && mx < x + 124 && my >= y && my < y + 9) {
            pendingTooltip = List.of(Component.translatable("gui.bettacraft.fish.counts", fmt2(d.getFloat("stock"))));
        }
        y += 9;
        float hp = Math.min(sel.health(), sel.hp());
        bar(g, x, y, Component.translatable(sel.sick() ? "gui.bettacraft.fish.sick" : "gui.bettacraft.fish.health"), hp, hp > 0.5F ? 0xFF50C050 : 0xFFD04040);
        y += 9;
        bar(g, x, y, Component.translatable("gui.bettacraft.fish.satiation"), sel.satiation(), 0xFFD0A040);
        y += 11;
        int stress = d.getInt("stress");
        if (s.shoaling()) {
            boolean small = (stress & org.overengineer.bettacraft.entity.tankmate.TankmateCare.STRESS_SHOAL) != 0;
            fitLine(g, Component.translatable(small ? "gui.bettacraft.tm.shoal_small" : "gui.bettacraft.tm.shoal", d.getInt("shoal"), s.shoal())
                    .withStyle(small ? ChatFormatting.RED : ChatFormatting.GREEN), x, y, TEXT_LIGHT, 124);
        } else {
            fitLine(g, Component.translatable("gui.bettacraft.tm.solitary").withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT, 124);
        }
        y += 9;
        boolean temp = (stress & org.overengineer.bettacraft.entity.tankmate.TankmateCare.STRESS_TEMP) == 0;
        fitLine(g, Component.translatable("gui.bettacraft.tm.temp", f1(s.tempMin()), f1(s.tempMax()))
                .withStyle(temp ? ChatFormatting.GREEN : ChatFormatting.RED), x, y, TEXT_LIGHT, 124);
        y += 9;
        List<Component> notes = new ArrayList<>();
        if ((stress & org.overengineer.bettacraft.entity.tankmate.TankmateCare.STRESS_WATER) != 0)
            notes.add(Component.translatable("gui.bettacraft.tm.water_bad").withStyle(ChatFormatting.RED));
        if ((stress & org.overengineer.bettacraft.entity.tankmate.TankmateCare.STRESS_CHASED) != 0)
            notes.add(Component.translatable("gui.bettacraft.tm.chased").withStyle(ChatFormatting.RED));
        if ((stress & org.overengineer.bettacraft.entity.tankmate.TankmateCare.STRESS_CROWDED) != 0)
            notes.add(Component.translatable("gui.bettacraft.tm.crowded").withStyle(ChatFormatting.RED));
        if (d.getFloat("gravid") >= 0) notes.add(Component.translatable("gui.bettacraft.tm.gravid", Math.round(d.getFloat("gravid") * 100))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        if (d.getBoolean("guard")) notes.add(Component.translatable("gui.bettacraft.tm.guarding").withStyle(ChatFormatting.AQUA));
        else if (d.getBoolean("cave")) notes.add(Component.translatable("gui.bettacraft.tm.has_cave").withStyle(ChatFormatting.AQUA));
        notes.add(Component.translatable(s.breedingKey()).withStyle(ChatFormatting.GRAY));
        for (Component c : notes) {
            if (y > 128) break;
            fitLine(g, c, x, y, TEXT_LIGHT, 124);
            y += 9;
        }
    }

    private static String f1(double d) {
        return String.format(java.util.Locale.ROOT, "%.1f", d);
    }

    /** M14: shrimp, crayfish and snails (second page of the animals tab): the kinds on the left, details and the tank on the right. */
    private void drawCritters(GuiGraphics g, TankView v, int mx, int my) {
        TankView.Critters c = v.critters;
        int lx = 10, ly = 20;
        g.fill(lx - 1, ly - 1, lx + 100, ly + LIST_ROWS * ROW_H, 0x60000000);
        if (c.groups.isEmpty()) {
            fitLine(g, Component.translatable("gui.bettacraft.critters.empty"), lx + 2, ly + 2, 0xA0A0A0, 96);
            fitLine(g, Component.translatable("gui.bettacraft.critters.empty_hint").withStyle(ChatFormatting.GRAY), lx + 2, ly + 13, 0xA0A0A0, 96);
        }
        for (int r = 0; r < LIST_ROWS; r++) {
            int idx = groupScroll + r;
            if (idx >= c.groups.size()) break;
            TankView.CritterGroup gr = c.groups.get(idx);
            int y = ly + r * ROW_H;
            if (gr.key().equals(selectedGroup)) g.fill(lx, y, lx + 99, y + ROW_H, 0x80FFFFFF);
            else if (mx >= lx && mx < lx + 100 && my >= y && my < y + ROW_H) g.fill(lx, y, lx + 99, y + ROW_H, 0x30FFFFFF);
            g.fill(lx + 2, y + 2, lx + 8, y + 8, 0xFF000000 | lighten(gr.rgb()));
            // in the list the colour species show only their variety (the header names the species)
            var sp = org.overengineer.bettacraft.critters.CritterSpecies.values()[gr.species()];
            MutableComponent name = sp.loci().length == 0 ? Component.translatable(sp.translationKey())
                    : "wild".equals(gr.variety()) ? org.overengineer.bettacraft.critters.TankCritters.name(sp, gr.variety())
                    : Component.translatable("variety.bettacraft." + sp.id() + "." + gr.variety());
            String count = " " + (gr.adults() + gr.young());
            int room = 88 - font.width(count);
            fitLine(g, name, lx + 10, y + 2, 0xFFFFFF, room);
            g.drawString(font, count, lx + 98 - font.width(count), y + 2, 0xC8C8C8, false);
        }
        int x = 114, y = 19, w = 124;
        TankView.CritterGroup sel = null;
        for (TankView.CritterGroup gr : c.groups) if (gr.key().equals(selectedGroup)) sel = gr;
        if (sel != null) {
            var sp = org.overengineer.bettacraft.critters.CritterSpecies.values()[sel.species()];
            fitLine(g, org.overengineer.bettacraft.critters.TankCritters.name(sp, sel.variety()).withStyle(ChatFormatting.YELLOW), x, y, 0xFFFFFF, w);
            y += 10;
            Component sexes = sp.hasSexes() ? Component.translatable("gui.bettacraft.critters.sexes", sel.females(), sel.males())
                    : Component.translatable("gui.bettacraft.critters.hermaphrodite", sel.adults());
            MutableComponent l1 = sexes.copy();
            if (sel.young() > 0) l1.append(", ").append(Component.translatable("gui.bettacraft.critters.young", sel.young()));
            fitLine(g, l1, x, y, TEXT_LIGHT, w);
            y += 10;
            if (sp.hasGrade()) {
                fitLine(g, Component.translatable("gui.bettacraft.critters.grade", Math.round(sel.gradeAvg() * 100), Math.round(sel.gradeMin() * 100),
                        Math.round(sel.gradeMax() * 100)), x, y, TEXT_LIGHT, w);
                y += 10;
            }
            Component breed = !sp.breeds() ? Component.translatable("gui.bettacraft.critters.no_breeding").withStyle(ChatFormatting.GRAY)
                    : sel.berried() > 0 ? Component.translatable(sp.breeding() == org.overengineer.bettacraft.critters.CritterSpecies.Breeding.PAIR_CLUTCH
                    ? "gui.bettacraft.critters.gravid" : "gui.bettacraft.critters.berried", sel.berried()).withStyle(ChatFormatting.GREEN)
                    : Component.translatable("gui.bettacraft.critters.breeds." + sp.breeding().name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GRAY);
            fitLine(g, breed, x, y, TEXT_LIGHT, w);
            y += 12;
        }
        // the whole tank
        fitLine(g, Component.translatable("gui.bettacraft.critters.total", c.total, c.max, fmt((float) c.stock)), x, y, TEXT_LIGHT, w);
        y += 10;
        ChatFormatting ac = c.algae < 0.15 ? ChatFormatting.GREEN : c.algae < 0.5 ? ChatFormatting.GOLD : ChatFormatting.RED;
        fitLine(g, Component.translatable("gui.bettacraft.critters.algae", Math.round(c.algae * 100)).withStyle(ac), x, y, TEXT_LIGHT, w);
        y += 10;
        fitLine(g, Component.translatable("gui.bettacraft.critters.cleaners", fmt((float) c.grazing), fmt((float) c.algaeGrowth)),
                x, y, TEXT_LIGHT, w);
        y += 10;
        if (c.total > 0) {
            ChatFormatting fc = c.foodDays >= 1 ? ChatFormatting.GREEN : c.foodDays >= 0.6 ? ChatFormatting.GOLD : ChatFormatting.RED;
            String fk = c.foodDays >= 1 ? "plenty" : c.foodDays >= 0.6 ? "short" : "hungry";
            fitLine(g, Component.translatable("gui.bettacraft.critters.food", Component.translatable("gui.bettacraft.critters.food." + fk).withStyle(fc)),
                    x, y, TEXT_LIGHT, w);
            y += 10;
            MutableComponent hide = Component.translatable("gui.bettacraft.critters.hiding", Math.round(c.hiding * 100));
            if (c.hunting) hide.append(" ").append(Component.translatable("gui.bettacraft.critters.hunting").withStyle(ChatFormatting.GOLD));
            fitLine(g, hide, x, y, TEXT_LIGHT, w);
            y += 10;
        }
        if (c.clutches > 0 && y < 118) {
            fitLine(g, Component.translatable("gui.bettacraft.critters.clutch_count", c.clutches).withStyle(ChatFormatting.LIGHT_PURPLE), x, y, TEXT_LIGHT, w);
            y += 10;
        }
        if (c.dead > 0 && y < 118) {
            fitLine(g, Component.translatable("gui.bettacraft.critters.dead", c.dead).withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT, w);
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

    /** A row of the breeding tab: a piece of a wrapped line and its indent. */
    private record BreedRow(net.minecraft.util.FormattedCharSequence text, int indent) {}

    /** Text width of the breeding tab (left of the scroll bar). */
    private static final int BREED_W = 222;

    /**
     * The lines of the breeding tab wrapped to the content width (long genotype lines, mostly in German): the
     * continuation rows are indented a little further than the line they belong to.
     */
    private List<BreedRow> breedRows(TankView v) {
        List<BreedRow> rows = new ArrayList<>();
        for (Component c : BreedingLines.build(v)) {
            if (font.width(c) <= BREED_W) {
                rows.add(new BreedRow(c.getVisualOrderText(), 0));
                continue;
            }
            String plain = c.getString();
            int lead = 0;
            while (lead < plain.length() && plain.charAt(lead) == ' ') lead++;
            int indent = font.width(plain.substring(0, lead)) + 8;
            List<net.minecraft.util.FormattedCharSequence> parts = font.split(c, BREED_W - indent);
            for (int i = 0; i < parts.size(); i++) rows.add(new BreedRow(parts.get(i), i == 0 ? 0 : indent));
        }
        return rows;
    }

    private void drawBreeding(GuiGraphics g, TankView v) {
        int x = 11, y = 20;
        List<BreedRow> rows = breedRows(v);
        int shown = breedRowCount();
        breedScroll = Mth.clamp(breedScroll, 0, Math.max(0, rows.size() - shown));
        for (int r = 0; r < shown; r++) {
            int idx = breedScroll + r;
            if (idx >= rows.size()) break;
            BreedRow row = rows.get(idx);
            g.drawString(font, row.text(), x + row.indent(), y + r * 10, TEXT_LIGHT, false);
        }
        scrollbar(g, rows.size(), shown, breedScroll, false);
    }

    /** M19: the breeding tab leaves room for the breeding box button. */
    private int breedRowCount() {
        return boxButton != null && boxButton.visible ? BREED_ROWS - 2 : BREED_ROWS;
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
