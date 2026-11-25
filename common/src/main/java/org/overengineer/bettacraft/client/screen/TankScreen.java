package org.overengineer.bettacraft.client.screen;

import org.overengineer.bettacraft.util.NbtCompat;
import org.overengineer.bettacraft.util.Perms;
import net.minecraft.client.Minecraft;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.overengineer.bettacraft.platform.network.PacketDistributor;
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

/** Tank menu screen with the tabs Overview, Inhabitants, Breeding, Water (M9), Plants (M12), Users (M53, was Interior) and Log (PLAN 8). */
public class TankScreen extends AbstractContainerScreen<TankMenu> {
    private static final Identifier TEXTURE = BettaCraft.id("textures/gui/tank.png");
    private static final int TEXT = 0x404040;
    private static final int TEXT_LIGHT = 0xE0E0E0;
    private static final int CONTENT_X = 7, CONTENT_Y = 17, CONTENT_W = 234, CONTENT_H = 123;
    private static final int LIST_ROWS = 9, ROW_H = 11, LOG_ROWS = 10, BREED_ROWS = 12;
    /** Width of the bars of the water quality, the plant cover, oxygen and climate (right of their line). */
    private static final int BAR_W = 66;
    /**
     * M62: the content area grows with the screen (when the GUI scale leaves room), from the old 234 x 123 up to these; the panel
     * around it is drawn in code, the player inventory stays at the bottom, centred.
     */
    public static final int MAX_CONTENT_W = 380, MAX_CONTENT_H = 214;
    /** M62: the content size now ({@link #layout}) and the offset of the panel from {@code leftPos/topPos} (the slot frame). */
    private int cw = CONTENT_W, ch = CONTENT_H, ox, oy;
    /** M62: width of the list on the left of the animals tab. */
    private int listW = 100;

    /** Order of the constants = index of {@link #selectTabForTest}; the buttons are drawn in a different order. */
    /** M53: USERS took the place (and the index) of the old interior tab. */
    private enum Tab { OVERVIEW, INHABITANTS, BREEDING, LOG, USERS, WATER, PLANTS, DEVICES, CLIMATE, SUMP }

    /**
     * Scale of the tab labels (set in init): full size when all of them fit across the menu (M62: a wider menu), else smaller,
     * down to 0.55 (ten tabs in the 236 px of the smallest menu, also the longer German labels).
     */
    public static float TAB_LABEL_SCALE = 0.75F;

    /** Log filter: all, breeding (nest, eggs, fry, growth), health (sick, deaths, fights), feeding. */
    private enum LogFilter { ALL, BREEDING, HEALTH, FEEDING }

    private Tab tab = Tab.OVERVIEW;
    private final List<AbstractWidget> overview = new ArrayList<>();
    private final List<AbstractWidget> inhabitants = new ArrayList<>();
    private Button tabOverview, tabInhabitants, tabBreeding, tabLog, tabUsers, tabWater, tabPlants, logFilterButton;
    /** M53: the gear right of the interior button: the client config of Bettacraft (alarm waypoints of all tanks, map mods ...). */
    private Button configButton;
    /** M53: the users tab. */
    private final List<AbstractWidget> usersWidgets = new ArrayList<>();
    private Button userRoleButton, userRemoveButton, myWaypointButton, newRoleButton, addUserButton, allWaypointsButton, othersButton;
    private EditBox addUserBox;
    @Nullable
    private UUID selectedUser;
    private org.overengineer.bettacraft.tank.TankUsers.Role newRole = org.overengineer.bettacraft.tank.TankUsers.Role.CARETAKER;
    private int userScroll;
    private static final int USER_ROWS = 5;
    /** M23: tab Devices (oxygen, aeration, lights with dimmer and timer). */
    private Button tabDevices;
    /** M31: the climate tab, only for a terrarium or paludarium (the tabs are laid out again when that changes). */
    @Nullable
    private Button tabClimate;
    private boolean climateTabShown;
    /** M49: the sump tab, only while a sump sits in a cabinet under the tank. */
    @Nullable
    private Button tabSump;
    private boolean sumpTabShown;
    private final List<AbstractWidget> devices = new ArrayList<>();
    private Button timerButton;
    private Button thinButton;
    /** M13: the plants tab shows the aquarium plants of the interior instead of the floating plants. */
    private boolean aquaticPage;
    private Button plantPageButton;
    /** M14: the animals tab shows the shrimp, crayfish and snails instead of the fish and frogs. */
    private boolean critterPage;
    private Button critterPageButton, critterTakeButton, clutchButton;
    /** M22.3: sex and container of the quick take-out, the magnifier button, the single animals of a kind. */
    private Button sexButton, containerButton, analyseButton;
    private int critterSex;
    private boolean critterBucket;
    @Nullable
    private Integer selectedCritter;
    private boolean critterExpanded = true;
    private static final int CRITTER_ROWS = 8;
    /** M19: takes the breeding box out (breeding tab). */
    private Button boxButton;
    @Nullable
    private String selectedGroup;
    private int groupScroll;
    private LogFilter logFilter = LogFilter.ALL;
    private int breedScroll;
    private EditBox nameBox, fishNameBox;
    private Button protectButton;
    /** M51: who may let his hired aquarist work here (right of the lock). */
    private Button aquaristButton;
    /** M33.1: the tank type, in the overview (every tank) and in the climate tab. */
    private Button modeButton, climateModeButton;
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
    /** M24: tooltip of the nitrate line in the water tab and its y. */
    private Component nitrateTip;
    private int nitrateTipY;
    /** M25: tooltip of the water chemistry line in the water tab and its y. */
    private List<Component> chemTip;
    private int chemTipY;
    /** M66: tooltip of the oxygen line in the water tab (target, saturation, exchange, use) and its y. */
    private Component oxygenTip;
    private int oxygenTipY;
    /** M66: tooltip of the "improves the water" line in the plants tab (floating and aquarium plants) and its y. */
    private Component uptakeTip;
    private int uptakeTipY;
    /** M66: tab completion of the name in the users tab: the text typed first, the matches and the one shown. */
    private String completionBase, completionShown;
    private List<String> completions = List.of();
    private int completionIndex = -1;

    private static String toText(net.minecraft.util.FormattedCharSequence s) {
        StringBuilder b = new StringBuilder();
        s.accept((i, style, cp) -> {
            b.appendCodePoint(cp);
            return true;
        });
        return b.toString();
    }

    public TankScreen(TankMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 248, 230);
        inventoryLabelX = TankMenu.INV_X;
        inventoryLabelY = 10000; // no inventory label (no room)
    }

    @Nullable
    private TankView view() {
        return menu.view();
    }

    /** M33.1: the next tank type (shift: the one before). */
    private void cycleMode() {
        TankView v = view();
        if (v == null) return;
        int n = org.overengineer.bettacraft.terrarium.TankModeSetting.values().length;
        int next = Math.floorMod(v.climate.setting + (Minecraft.getInstance().hasShiftDown() ? -1 : 1), n);
        PacketDistributor.sendToServer(new org.overengineer.bettacraft.network.TankModePayload(menu.containerId, next));
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
        layout();
        overview.clear();
        inhabitants.clear();
        // M62: x, y = the corner of the panel; dw, dh = how much wider and taller the content is than in the smallest menu
        int x = leftPos + ox, y = topPos + oy;
        int dw = cw - CONTENT_W, dh = ch - CONTENT_H;
        // the tabs across the top (the title moved into the overview: no room for both) with labels at full size when they fit,
        // else in a smaller font; widths follow the labels, the rest of the width is shared out
        TankView cv = view();
        climateTabShown = cv != null && cv.climate.hasLand();
        sumpTabShown = cv != null && cv.sump.contains("count");
        java.util.List<Tab> tabList = new java.util.ArrayList<>(java.util.List.of(Tab.OVERVIEW, Tab.INHABITANTS, Tab.BREEDING, Tab.WATER, Tab.DEVICES));
        if (sumpTabShown) tabList.add(Tab.SUMP); // M49
        if (climateTabShown) tabList.add(Tab.CLIMATE);
        tabList.addAll(java.util.List.of(Tab.PLANTS, Tab.USERS, Tab.LOG));
        Tab[] order = tabList.toArray(new Tab[0]);
        String[] keys = new String[order.length];
        for (int i = 0; i < order.length; i++) keys[i] = order[i].name().toLowerCase(java.util.Locale.ROOT);
        int[] labels = new int[order.length];
        for (int i = 0; i < order.length; i++) labels[i] = font.width(Component.translatable("gui.bettacraft.tab." + keys[i]));
        int labelSum = 0;
        for (int l : labels) labelSum += l;
        int tabsW = cw + 2;
        TAB_LABEL_SCALE = tabScale(labelSum, order.length, tabsW);
        int[] widths = tabWidths(labels, TAB_LABEL_SCALE, tabsW);
        int tx = x + 5;
        Button[] tabs = new Button[order.length];
        for (int i = 0; i < order.length; i++) {
            Tab t = order[i];
            tabs[i] = addRenderableWidget(new TabButton(tx, y + 3, widths[i], 13, Component.translatable("gui.bettacraft.tab." + keys[i]), b -> setTab(t)));
            tx += widths[i];
        }
        tabClimate = null;
        tabSump = null;
        for (int i = 0; i < order.length; i++) {
            switch (order[i]) {
                case OVERVIEW -> tabOverview = tabs[i];
                case INHABITANTS -> tabInhabitants = tabs[i];
                case BREEDING -> tabBreeding = tabs[i];
                case WATER -> tabWater = tabs[i];
                case DEVICES -> tabDevices = tabs[i];
                case CLIMATE -> tabClimate = tabs[i];
                case SUMP -> tabSump = tabs[i];
                case PLANTS -> tabPlants = tabs[i];
                case USERS -> tabUsers = tabs[i];
                case LOG -> tabLog = tabs[i];
            }
        }
        if (!climateTabShown && tab == Tab.CLIMATE) tab = Tab.OVERVIEW;
        if (!sumpTabShown && tab == Tab.SUMP) tab = Tab.OVERVIEW;
        // M23: dimmer, daylight timer with on and off time (M62: right under the lights line; the timer button on the right)
        devices.clear();
        devices.add(addRenderableWidget(Button.builder(Component.literal("-"), b -> lightSetting(TankActions.SET_DIMMER, -10)).bounds(x + 62, y + LIGHT_Y + 22, 14, 13).build()));
        devices.add(addRenderableWidget(Button.builder(Component.literal("+"), b -> lightSetting(TankActions.SET_DIMMER, 10)).bounds(x + 112, y + LIGHT_Y + 22, 14, 13).build()));
        timerButton = addRenderableWidget(new FitButton(x + 150 + dw, y + LIGHT_Y + 22, 88, 13, Component.empty(), b -> send(TankActions.TOGGLE_TIMER, 0, "", null)));
        timerButton.setTooltip(Tooltip.create(Component.translatable("gui.bettacraft.devices.timer_tooltip")));
        devices.add(timerButton);
        devices.add(addRenderableWidget(Button.builder(Component.literal("-"), b -> lightSetting(TankActions.SET_LIGHT_ON, -30)).bounds(x + 46, y + LIGHT_Y + 38, 14, 13).build()));
        devices.add(addRenderableWidget(Button.builder(Component.literal("+"), b -> lightSetting(TankActions.SET_LIGHT_ON, 30)).bounds(x + 96, y + LIGHT_Y + 38, 14, 13).build()));
        devices.add(addRenderableWidget(Button.builder(Component.literal("-"), b -> lightSetting(TankActions.SET_LIGHT_OFF, -30)).bounds(x + 168, y + LIGHT_Y + 38, 14, 13).build()));
        devices.add(addRenderableWidget(Button.builder(Component.literal("+"), b -> lightSetting(TankActions.SET_LIGHT_OFF, 30)).bounds(x + 218, y + LIGHT_Y + 38, 14, 13).build()));
        thinButton = addRenderableWidget(new FitButton(x + 166 + dw, y + 124 + dh, 72, 14, Component.translatable("gui.bettacraft.plants.thin"),
                b -> send(TankActions.THIN_PLANTS, 0, "", null)));
        plantPageButton = addRenderableWidget(new FitButton(x + 92 + dw, y + 124 + dh, 72, 14, Component.empty(), b -> {
            aquaticPage = !aquaticPage;
            updateWidgets();
        }));
        logFilterButton = addRenderableWidget(new FitButton(x + 176 + dw, y + 125 + dh, 62, 13, Component.empty(), b -> {
            logFilter = LogFilter.values()[(logFilter.ordinal() + 1) % LogFilter.values().length];
            logScroll = 0;
            updateWidgets();
        }));

        // ---- overview: the name and the three switches, then the sign row; the extra width goes half to the name
        int nameW = 80 + dw / 2, protW = 52 + dw / 6, lockW = 62 + dw / 6, aqW = 26 + dw - dw / 2 - 2 * (dw / 6);
        nameBox = new EditBox(font, x + 11, y + 21, nameW, 13, Component.translatable("gui.bettacraft.tank.name"));
        nameBox.setMaxLength(TankData.MAX_NAME_LENGTH);
        nameBox.setHint(Component.translatable("gui.bettacraft.tank.name_hint").withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setTooltip(Tooltip.create(Component.translatable("gui.bettacraft.tank.name_tooltip"))); // M56: where the name shows
        nameBox.setResponder(s -> nameDirtyTicks = 15);
        overview.add(addRenderableWidget(nameBox));
        int px = x + 14 + nameW;
        // M21.5: break protection, between the name and the lock
        protectButton = addRenderableWidget(new FitButton(px, y + 20, protW, 15, Component.empty(), b -> send(TankActions.TOGGLE_PROTECT, 0, "", null)));
        overview.add(protectButton);
        lockButton = addRenderableWidget(new FitButton(px + protW + 2, y + 20, lockW, 15, Component.empty(), b -> send(TankActions.TOGGLE_LOCK, 0, "", null)));
        overview.add(lockButton);
        // M51: the hired aquarist (mine / everyone's / none), right of the lock
        aquaristButton = addRenderableWidget(new FitButton(px + protW + lockW + 4, y + 20, aqW, 15, Component.empty(), b -> {
            TankView v = view();
            if (v != null) send(TankActions.SET_AQUARIST, v.aquaristAccess.next().ordinal(), "", null);
        }));
        overview.add(aquaristButton);
        // sign, sign position and front side share the row; labels that are still too long are drawn smaller, not scrolled
        int signW = 62 + dw / 3, posW = 82 + dw / 3, frontW = 80 + dw - 2 * (dw / 3);
        signButton = addRenderableWidget(new FitButton(x + 10, y + 38, signW, 14, Component.empty(), b -> send(TankActions.TOGGLE_SIGN, 0, "", null)));
        signPosButton = addRenderableWidget(new FitButton(x + 12 + signW, y + 38, posW, 14, Component.empty(), b -> {
            TankView v = view();
            if (v != null) send(TankActions.SET_SIGN_POS, v.signPos.next().ordinal(), "", null);
        }));
        frontButton = addRenderableWidget(new FitButton(x + 14 + signW + posW, y + 38, frontW, 14, Component.empty(), b -> {
            TankView v = view();
            if (v != null) send(TankActions.SET_FRONT, v.front.getClockWise().get2DDataValue(), "", null);
        }));
        int ty = y + overviewY(2) - 2;
        minusButton = addRenderableWidget(Button.builder(Component.literal("-"), b -> changeTarget(-1))
                .bounds(x + 168 + dw, ty, 14, 12).build());
        plusButton = addRenderableWidget(Button.builder(Component.literal("+"), b -> changeTarget(1))
                .bounds(x + 224 + dw, ty, 14, 12).build());
        // M53: the interior button; right of it the gear opens the client config (M62: a little wider when there is room)
        int enterW = 46 + Math.min(30, dw / 4);
        enterButton = addRenderableWidget(new FitButton(x + 222 + dw - enterW, y + 123 + dh, enterW, 14, Component.translatable("gui.bettacraft.tank.enter"),
                b -> send(TankActions.ENTER_INTERIOR, 0, "", null)));
        configButton = addRenderableWidget(Button.builder(Component.literal("⚙"), b -> openClientConfig()).bounds(x + 224 + dw, y + 123 + dh, 14, 14).build());
        configButton.setTooltip(Tooltip.create(Component.translatable("gui.bettacraft.tank.client_config")));
        overview.addAll(List.of(signButton, signPosButton, frontButton, minusButton, plusButton, enterButton, configButton));
        // M33.1: the tank type (automatic, aquarium, paludarium, terrarium) left of the target temperature, again in the climate tab
        int modeW = 49 + Math.min(30, dw / 3);
        modeButton = addRenderableWidget(new FitButton(x + 165 + dw - modeW, ty, modeW, 12, Component.empty(), b -> cycleMode()));
        overview.add(modeButton);
        climateModeButton = addRenderableWidget(new FitButton(x + 166 + dw, y + 18, 72, 12, Component.empty(), b -> cycleMode()));

        // ---- inhabitants: the list on the left (wider with the menu), the details on the right; dl / dr = their extra width
        int dl = listW - 100, dr = dw - dl;
        fishNameBox = new EditBox(font, x + 115 + dl, y + 109 + dh, 84 + dr, 12, Component.translatable("gui.bettacraft.fish.name"));
        fishNameBox.setMaxLength(TankActions.MAX_FISH_NAME);
        inhabitants.add(addRenderableWidget(fishNameBox));
        renameButton = addRenderableWidget(new FitButton(x + 201 + dw, y + 108 + dh, 37, 14, Component.translatable("gui.bettacraft.fish.rename"), b -> {
            if (selected != null) send(TankActions.RENAME_FISH, 0, fishNameBox.getValue(), selected);
        }));
        // the two take-out buttons share the width under the details (62 + 61 in the smallest menu)
        int takeW = 62 + dr / 2, takeX = x + 114 + dl, take2X = takeX + takeW + 1, take2W = x + 238 + dw - take2X;
        bucketButton = addRenderableWidget(new FitButton(takeX, y + 124 + dh, takeW, 14, Component.translatable("gui.bettacraft.fish.take_bucket"),
                b -> takeOut(false)));
        bagButton = addRenderableWidget(new FitButton(take2X, y + 124 + dh, take2W, 14, Component.translatable("gui.bettacraft.fish.take_bag"), b -> takeOut(true)));
        inhabitants.addAll(List.of(renameButton, bucketButton, bagButton));
        // M14: second page of the animals tab (shrimp, crayfish, snails)
        critterPageButton = addRenderableWidget(new FitButton(x + 10, y + 124 + dh, 64 + dl, 14, Component.empty(), b -> {
            critterPage = !critterPage;
            updateWidgets();
        }));
        critterTakeButton = addRenderableWidget(new FitButton(takeX, y + 124 + dh, takeW, 14, Component.translatable("gui.bettacraft.critters.take"), b -> {
            if (selectedCritter != null) {
                send(TankActions.TAKE_CRITTER_ONE, selectedCritter, critterBucket ? "bucket" : "bag", null); // M22.3: exactly this one
            } else if (selectedGroup != null) {
                send(TankActions.TAKE_CRITTER, TankActions.critterValue(Minecraft.getInstance().hasControlDown() ? 2 : Minecraft.getInstance().hasShiftDown() ? 1 : 0, critterSex, critterBucket), selectedGroup, null);
            }
        }));
        // M22.3: sex and container of the take-out, under the list of the small animals
        sexButton = addRenderableWidget(new FitButton(x + 10, y + 108 + dh, 49 + dl / 2, 13, Component.empty(), b -> {
            critterSex = (critterSex + 1) % 3;
            updateWidgets();
        }));
        containerButton = addRenderableWidget(new FitButton(x + 60 + dl / 2, y + 108 + dh, 50 + dl - dl / 2, 13, Component.empty(), b -> {
            critterBucket = !critterBucket;
            updateWidgets();
        }));
        // M22.3: the magnifier on the selected fish or small animal
        analyseButton = addRenderableWidget(new FitButton(x + 75 + dl, y + 124 + dh, 35, 14, Component.translatable("gui.bettacraft.magnifier.analyse"), b -> {
            if (critterPage) {
                if (selectedCritter != null) send(TankActions.ANALYSE, selectedCritter, "critter", null);
            } else if (selected != null) {
                send(TankActions.ANALYSE, 0, "", selected);
            }
        }));
        clutchButton = addRenderableWidget(new FitButton(take2X, y + 124 + dh, take2W, 14, Component.translatable("gui.bettacraft.critters.clutches"),
                b -> send(TankActions.REMOVE_CLUTCHES, 0, "", null)));
        boxButton = addRenderableWidget(new FitButton(x + 150 + dw, y + 124 + dh, 88, 14, Component.translatable("gui.bettacraft.breeding.tm.box_out"),
                b -> send(TankActions.REMOVE_BREEDING_BOX, 0, "", null)));
        initUsers(x, y);
        sentName = null;
        updateWidgets();
        if (pendingName != null && nameBox != null) {
            nameBox.setValue(pendingName);
            sentName = pendingName;
        }
    }

    /**
     * M62: the size of the menu for this screen: the content area grows (up to {@link #MAX_CONTENT_W} x {@link #MAX_CONTENT_H})
     * while the screen has room around it, the panel is centred, and {@code leftPos/topPos} (where the slots of the player
     * inventory hang, fixed in the menu) move so that the inventory sits centred at the bottom of the panel.
     */
    private void layout() {
        cw = contentWidth(width);
        ch = contentHeight(height);
        int pw = cw + 14, ph = ch + 107;
        int panelX = (width - pw) / 2, panelY = (height - ph) / 2;
        // the inventory grid (162 px at x 44 of the slot frame) stays centred as in the smallest menu
        leftPos = panelX + (pw - 248) / 2;
        topPos = panelY + ph - 230;
        ox = panelX - leftPos;
        oy = panelY - topPos;
        listW = 100 + Math.min(60, (cw - CONTENT_W) / 3);
    }

    /** M62: the content width for a screen this wide (GUI pixels): 234 in the smallest menu, wider while 24 px and a sixth of the screen (room for a recipe
     * viewer) stay free on each side. */
    public static int contentWidth(int screenW) {
        return Mth.clamp(Math.min(screenW - 48, Math.round(screenW * 0.68F)) - 14, CONTENT_W, MAX_CONTENT_W);
    }

    /** M62: the content height for a screen this high: 123 in the smallest menu, taller while 10 px stay free above and below. */
    public static int contentHeight(int screenH) {
        return Mth.clamp(screenH - 20 - 107, CONTENT_H, MAX_CONTENT_H);
    }

    /** M62: the scale of the tab labels: full size when they fit with a margin each, else smaller (at least 0.55). */
    public static float tabScale(int labelSum, int tabs, int total) {
        if (labelSum <= 0) return 1F;
        return Math.max(0.55F, Math.min(1F, (total - 6F * tabs) / labelSum));
    }

    /** M62: the panel's bounds on the screen (x, y, w, h), e.g. for recipe viewers that must keep clear of it. */
    public int[] panelBounds() {
        return new int[]{leftPos + ox, topPos + oy, cw + 14, ch + 107};
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int left, int top) {
        int[] p = panelBounds();
        return mx < p[0] || my < p[1] || mx >= p[0] + p[2] || my >= p[1] + p[3];
    }

    /**
     * M62: the y of the overview's status lines (size, temperature, target, heaters, feeder, filters, animals, then the warnings):
     * the old spacing in the smallest menu, up to 3 px more between them when the menu is taller.
     */
    private int overviewY(int line) {
        int[] base = {56, 67, 79, 92, 102, 112, 122, 132};
        int extra = Mth.clamp((ch - CONTENT_H) / 12, 0, 3);
        int i = Math.min(line, base.length - 1);
        return base[i] + extra * i + (line - i) * (10 + extra);
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
        thinButton.onPress(new net.minecraft.client.input.KeyEvent(0, 0, 0));
        return true;
    }

    /** Dev smoke test only: the tab buttons from left to right (to check that they fit). */
    public List<Button> tabButtonsForTest() {
        if (tabClimate != null) return List.of(tabOverview, tabInhabitants, tabBreeding, tabWater, tabDevices, tabClimate, tabPlants, tabUsers, tabLog);
        return List.of(tabOverview, tabInhabitants, tabBreeding, tabWater, tabDevices, tabPlants, tabUsers, tabLog);
    }

    /** M23: dimmer (+-10 %, shift +-1 %) and the timer times (+-30 min, shift +-60 min). */
    private void lightSetting(int action, int delta) {
        TankView v = view();
        if (v == null) return;
        TankView.Tech t = v.tech;
        switch (action) {
            case TankActions.SET_DIMMER -> send(action, Math.max(0, Math.min(100, t.dimmer + (Minecraft.getInstance().hasShiftDown() ? Integer.signum(delta) : delta))), "", null);
            case TankActions.SET_LIGHT_ON -> send(action, Math.floorMod(t.lightOn + (Minecraft.getInstance().hasShiftDown() ? 2 * delta : delta), 1440), "", null);
            default -> send(action, Math.floorMod(t.lightOff + (Minecraft.getInstance().hasShiftDown() ? 2 * delta : delta), 1440), "", null);
        }
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

        // M52b: buttons draw their contents themselves since 1.21.11 (sprite + label)
        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            extractDefaultSprite(g);
            net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
            int color = net.minecraft.util.ARGB.color(net.minecraft.util.Mth.ceil(alpha * 255F), active ? 0xFFFFFF : 0xA0A0A0);
            float s = TAB_LABEL_SCALE;
            int room = (int) ((getWidth() - 3) / s);
            net.minecraft.network.chat.FormattedText text = getMessage();
            if (font.width(text) > room) {
                text = net.minecraft.network.chat.FormattedText.composite(font.substrByWidth(text, room - font.width("...")),
                        net.minecraft.network.chat.FormattedText.of("..."));
            }
            net.minecraft.util.FormattedCharSequence seq = net.minecraft.locale.Language.getInstance().getVisualOrder(text);
            g.pose().pushMatrix();
            g.pose().translate((float) (getX() + getWidth() / 2.0F), (float) (getY() + (getHeight() - 8 * s) / 2.0F + 0.5F));
            g.pose().scale(s, s);
            g.text(font, seq, -font.width(seq) / 2, 0, org.overengineer.bettacraft.client.ClientCompat.textColor(color), true);
            g.pose().popMatrix();
        }
    }

    /** A button whose label is drawn smaller when it does not fit (instead of vanilla's scrolling text), centred. */
    public static final class FitButton extends Button {
        public FitButton(int x, int y, int w, int h, Component label, OnPress onPress) {
            super(x, y, w, h, label, onPress, DEFAULT_NARRATION);
        }

        // M52b: buttons draw their contents themselves since 1.21.11 (sprite + label)
        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            extractDefaultSprite(g);
            net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
            int color = net.minecraft.util.ARGB.color(net.minecraft.util.Mth.ceil(alpha * 255F), active ? 0xFFFFFF : 0xA0A0A0);
            int room = getWidth() - 4, w = font.width(getMessage());
            if (w <= room) {
                extractDefaultLabel(g.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
                return;
            }
            float s = Math.max(0.5F, room / (float) w);
            g.pose().pushMatrix();
            g.pose().translate((float) (getX() + getWidth() / 2.0F), (float) (getY() + (getHeight() - 8 * s) / 2.0F + 0.5F));
            g.pose().scale(s, s);
            g.text(font, getMessage(), -w / 2, 0, org.overengineer.bettacraft.client.ClientCompat.textColor(color), true);
            g.pose().popMatrix();
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
        float step = Minecraft.getInstance().hasShiftDown() ? 1.0F : 0.5F;
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
        if (climateModeButton != null) climateModeButton.visible = false; // M33.1: shown below once the view is there
        if (modeButton != null && (v == null || !v.climate.settingOn)) modeButton.visible = false;
        for (AbstractWidget w : inhabitants) w.visible = tab == Tab.INHABITANTS && !critterPage;
        critterPageButton.visible = tab == Tab.INHABITANTS;
        critterTakeButton.visible = tab == Tab.INHABITANTS && critterPage;
        clutchButton.visible = tab == Tab.INHABITANTS && critterPage && v != null && v.critters.clutches > 0;
        // M62: without the clutch button the take-out button takes its room too
        critterTakeButton.setWidth(clutchButton.visible ? bucketButton.getWidth() : bagButton.getX() + bagButton.getWidth() - critterTakeButton.getX());
        // M22.3
        boolean list = Features.enabled(Feature.ANIMAL_LIST);
        sexButton.visible = tab == Tab.INHABITANTS && critterPage && list && selectedCritter == null;
        containerButton.visible = tab == Tab.INHABITANTS && critterPage && Features.enabled(Feature.BIG_CONTAINERS);
        if (!containerButton.visible) critterBucket = false;
        analyseButton.visible = tab == Tab.INHABITANTS && v != null && !v.genesFree;
        sexButton.setMessage(Component.translatable("gui.bettacraft.critters.sex." + (critterSex == 1 ? "male" : critterSex == 2 ? "female" : "any")));
        containerButton.setMessage(Component.translatable(critterBucket ? "gui.bettacraft.critters.into_bucket" : "gui.bettacraft.critters.into_bag"));
        critterTakeButton.setMessage(Component.translatable(critterBucket ? "gui.bettacraft.critters.take_bucket" : "gui.bettacraft.critters.take"));
        sexButton.setTooltip(Tooltip.create(Component.translatable("gui.bettacraft.critters.sex_tooltip")));
        containerButton.setTooltip(Tooltip.create(Component.translatable("gui.bettacraft.critters.container_tooltip")));
        boxButton.visible = tab == Tab.BREEDING && v != null && v.breeding.getBooleanOr("box", false);
        if (v != null) boxButton.active = v.canModify;
        if (v != null) {
            critterPageButton.setMessage(critterPage ? Component.translatable("gui.bettacraft.critters.page_fish", v.inhabitants.size())
                    : Component.translatable("gui.bettacraft.critters.page", v.critters.total));
            if (selectedGroup == null || v.critters.groups.stream().noneMatch(gr -> gr.key().equals(selectedGroup))) {
                selectedGroup = v.critters.groups.isEmpty() ? null : v.critters.groups.getFirst().key();
            }
            if (selectedCritter != null && v.critters.animals.stream().noneMatch(k -> k.id() == selectedCritter)) selectedCritter = null;
            critterTakeButton.active = v.canModify && selectedGroup != null && (v.hasBag || v.critters.total > 0);
            critterTakeButton.setTooltip(Tooltip.create(Component.translatable(!v.canModify ? "message.bettacraft.tank.locked"
                    : selectedCritter != null ? "gui.bettacraft.critters.take_one_tooltip" : "gui.bettacraft.critters.take_tooltip")));
            // M22.3: the magnifier on the selected animal
            boolean target;
            if (critterPage) {
                TankView.CritterInfo ci = selectedCritterInfo(v);
                target = ci != null && !ci.analysed();
            } else {
                TankView.Inhabitant si = selectedInhabitant(v);
                target = si != null && (si.betta() || si.isTankmate()) && !si.analysed();
            }
            analyseButton.active = target && v.hasMagnifier;
            analyseButton.setTooltip(Tooltip.create(Component.translatable(!target ? "gui.bettacraft.magnifier.nothing"
                    : v.hasMagnifier ? "gui.bettacraft.magnifier.tooltip" : "message.bettacraft.magnifier.need")));
            clutchButton.active = v.canModify;
        }
        tabOverview.active = tab != Tab.OVERVIEW;
        tabInhabitants.active = tab != Tab.INHABITANTS;
        tabBreeding.active = tab != Tab.BREEDING;
        tabLog.active = tab != Tab.LOG;
        tabUsers.active = tab != Tab.USERS;
        tabWater.active = tab != Tab.WATER;
        tabPlants.active = tab != Tab.PLANTS;
        tabDevices.active = tab != Tab.DEVICES;
        if (tabClimate != null) tabClimate.active = tab != Tab.CLIMATE;
        if (tabSump != null) tabSump.active = tab != Tab.SUMP;
        boolean lightControls = tab == Tab.DEVICES && v != null && v.tech.timerFeature;
        for (AbstractWidget w : devices) {
            w.visible = lightControls;
            w.active = v != null && v.canModify;
        }
        if (v != null) timerButton.setMessage(Component.translatable(v.tech.timer ? "gui.bettacraft.devices.timer_on" : "gui.bettacraft.devices.timer_off"));
        thinButton.visible = tab == Tab.PLANTS && !aquaticPage;
        plantPageButton.visible = tab == Tab.PLANTS;
        plantPageButton.setX(leftPos + ox + (aquaticPage ? 166 : 92) + cw - CONTENT_W);
        plantPageButton.setMessage(Component.translatable(aquaticPage ? "gui.bettacraft.plants.page_floating" : "gui.bettacraft.plants.page_aquatic"));
        updateUsers(v);
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
        lockButton.active = v.users.canManage; // M53: owner, caretakers, OP (or nobody owns it)
        lockButton.setMessage(Component.translatable(v.locked ? "gui.bettacraft.tank.locked" : "gui.bettacraft.tank.unlocked"));
        protectButton.visible = v.protectionFeature && tab == Tab.OVERVIEW;
        protectButton.active = lockButton.active;
        protectButton.setMessage(Component.translatable(v.breakProtected ? "gui.bettacraft.tank.protect_on" : "gui.bettacraft.tank.protect_off")
                .withStyle(v.breakProtected ? ChatFormatting.GOLD : ChatFormatting.WHITE));
        protectButton.setTooltip(Tooltip.create(Component.translatable("gui.bettacraft.tank.protect_tooltip")));
        aquaristButton.visible = v.aquaristFeature && tab == Tab.OVERVIEW;
        aquaristButton.active = lockButton.active;
        aquaristButton.setMessage(Component.translatable(v.aquaristAccess.translationKey() + ".short").withStyle(switch (v.aquaristAccess) {
            case OWNER -> ChatFormatting.WHITE;
            case ALL -> ChatFormatting.GREEN;
            case NONE -> ChatFormatting.GRAY;
        }));
        aquaristButton.setTooltip(Tooltip.create(Component.translatable("gui.bettacraft.tank.aquarist.tooltip",
                Component.translatable(v.aquaristAccess.translationKey()).withStyle(ChatFormatting.AQUA))));
        lockButton.setTooltip(Tooltip.create(Component.translatable("gui.bettacraft.tank.lock_tooltip",
                v.ownerName.isEmpty() ? Component.translatable("gui.bettacraft.tank.no_owner") : Component.literal(v.ownerName))));
        signButton.setMessage(Component.translatable(v.signVisible ? "gui.bettacraft.tank.sign_on" : "gui.bettacraft.tank.sign_off"));
        signPosButton.setMessage(Component.translatable(v.signPos.translationKey()));
        frontButton.setMessage(Component.translatable("gui.bettacraft.tank.front", Component.translatable("gui.bettacraft.direction." + v.front.getSerializedName())));
        for (Button b : List.of(signButton, signPosButton, frontButton, minusButton, plusButton)) b.active = mod;
        // M33.1: the tank type
        var setting = v.climate.setting();
        Component modeName = Component.translatable(setting.translationKey());
        modeButton.visible = tab == Tab.OVERVIEW && v.climate.settingOn;
        climateModeButton.visible = tab == Tab.CLIMATE && v.climate.settingOn;
        for (Button b : List.of(modeButton, climateModeButton)) {
            b.active = mod;
            // the overview has only room for the name (the tooltip says what it is)
            b.setMessage((b == modeButton ? modeName.copy() : Component.translatable("gui.bettacraft.tank_mode.button", modeName))
                    .withStyle(setting == org.overengineer.bettacraft.terrarium.TankModeSetting.AUTO ? ChatFormatting.WHITE : ChatFormatting.AQUA));
            b.setTooltip(Tooltip.create(mod ? modeTooltip(v) : Component.translatable("message.bettacraft.tank.locked")));
        }
        enterButton.active = mod; // M53: a viewer stays out
        configButton.active = true; // M53: everyone's own client settings
        enterButton.setTooltip(Tooltip.create(interiorTooltip(v))); // M53: what the old interior tab showed
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
        bucketButton.active = has && mod && sel.bucketable() && v.hasBucket; // M22.3: water buckets and buckets of the same species too
        bagButton.active = has && mod && v.hasBag && v.waterMb >= v.bagMb;
        bucketButton.setTooltip(Tooltip.create(takeTooltip(v, sel, false)));
        bagButton.setTooltip(Tooltip.create(takeTooltip(v, sel, true)));
    }

    /** M33.1: what the tank type does, its water limit and how to change it. */
    private static Component modeTooltip(TankView v) {
        var s = v.climate.setting();
        MutableComponent t = Component.translatable("gui.bettacraft.tank_mode.tooltip." + s.id(), Component.translatable(v.climate.mode().translationKey()));
        if (s == org.overengineer.bettacraft.terrarium.TankModeSetting.PALUDARIUM && v.climate.maxWater >= 0) {
            t.append("\n").append(Component.translatable("gui.bettacraft.tank_mode.limit", v.climate.maxWater));
        }
        if (mismatch(v)) t.append("\n").append(Component.translatable("gui.bettacraft.tank_mode.too_much_water").withStyle(ChatFormatting.GOLD));
        return t.append("\n").append(Component.translatable("gui.bettacraft.tank_mode.tooltip_cycle").withStyle(ChatFormatting.GRAY));
    }

    /** M33.1: a fixed type that holds less water than the tank has (nothing is removed, no more comes in). */
    private static boolean mismatch(TankView v) {
        return v.climate.settingOn && v.climate.maxWater >= 0 && v.waterMb > v.climate.maxWater;
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
        // M31: the tank became a terrarium / paludarium or an aquarium again: the tabs are laid out anew
        TankView cv = view();
        if (cv != null && (cv.climate.hasLand() != climateTabShown || cv.sump.contains("count") != sumpTabShown)) {
            flushName();
            rebuildWidgets();
            return;
        }
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
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int key = event.key();
        if (key == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) return super.keyPressed(event);
        for (EditBox box : List.of(nameBox, fishNameBox, addUserBox)) {
            if (box.isVisible() && box.isFocused()) {
                if (box == fishNameBox && (key == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN || key == com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER) && renameButton.active) {
                    renameButton.onPress(new net.minecraft.client.input.KeyEvent(0, 0, 0));
                    return true;
                }
                if (box == addUserBox && (key == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN || key == com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER) && addUserButton.active) {
                    addUserButton.onPress(event);
                    return true;
                }
                if (box == addUserBox && key == com.mojang.blaze3d.platform.InputConstants.KEY_TAB) { // M66: complete the name with the players that are online
                    completeUserName(event.hasShiftDown());
                    return true;
                }
                box.keyPressed(event);
                return true; // never let typing trigger the inventory key
            }
        }
        return super.keyPressed(event);
    }

    /**
     * M66: Tab in the name field of the users tab: completes the name with an online player whose name starts with what was typed
     * (the next one at each Tab, Shift + Tab the one before), like the chat does.
     */
    private void completeUserName(boolean back) {
        if (minecraft == null || minecraft.getConnection() == null) return;
        String cur = addUserBox.getValue();
        if (completionBase == null || !cur.equals(completionShown)) {
            completionBase = cur;
            List<String> names = new ArrayList<>();
            for (var info : minecraft.getConnection().getOnlinePlayers()) {
                String n = info.getProfile().name();
                if (n.regionMatches(true, 0, completionBase, 0, completionBase.length())) names.add(n);
            }
            names.sort(String.CASE_INSENSITIVE_ORDER);
            completions = names;
            completionIndex = -1;
        }
        if (completions.isEmpty()) return;
        completionIndex = Math.floorMod(completionIndex + (back ? -1 : 1), completions.size());
        completionShown = completions.get(completionIndex);
        addUserBox.setValue(completionShown);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        TankView v = view();
        if (tab == Tab.INHABITANTS && v != null && critterPage) {
            int lx = leftPos + ox + 10, ly = topPos + oy + 20;
            if (mx >= lx && mx < lx + listW && my >= ly && my < ly + critterRowCount() * ROW_H) {
                int idx = groupScroll + (int) ((my - ly) / ROW_H);
                List<CritterRow> rows = critterRows(v);
                if (idx >= 0 && idx < rows.size()) {
                    CritterRow r = rows.get(idx);
                    if (r.animal() != null) {
                        selectedCritter = r.animal().id(); // M22.3: exactly this animal
                    } else {
                        if (r.group().key().equals(selectedGroup) && selectedCritter == null) critterExpanded = !critterExpanded;
                        else critterExpanded = true;
                        selectedGroup = r.group().key();
                        selectedCritter = null;
                    }
                    updateWidgets();
                    return true;
                }
            }
        } else if (tab == Tab.USERS && v != null) {
            int lx = leftPos + ox + 10, ly = topPos + oy + USERS_LIST_Y;
            if (mx >= lx && mx < lx + cw - 6 && my >= ly && my < ly + userRows() * ROW_H) {
                int idx = userScroll + (int) ((my - ly) / ROW_H) - 1; // row 0 is the owner
                if (idx >= 0 && idx < v.users.list.size()) {
                    selectedUser = v.users.list.get(idx).id();
                    updateWidgets();
                    return true;
                }
            }
        } else if (tab == Tab.INHABITANTS && v != null) {
            int lx = leftPos + ox + 10, ly = topPos + oy + 20;
            if (mx >= lx && mx < lx + listW && my >= ly && my < ly + listRows() * ROW_H) {
                int idx = listScroll + (int) ((my - ly) / ROW_H);
                if (idx >= 0 && idx < v.inhabitants.size()) {
                    selected = v.inhabitants.get(idx).uuid();
                    updateWidgets();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        TankView v = view();
        if (v != null && my < topPos + oy + CONTENT_Y + ch) {
            if (tab == Tab.INHABITANTS && critterPage) {
                groupScroll = Mth.clamp(groupScroll - (int) Math.signum(sy), 0, Math.max(0, critterRows(v).size() - critterRowCount()));
                return true;
            }
            if (tab == Tab.INHABITANTS) {
                listScroll = Mth.clamp(listScroll - (int) Math.signum(sy), 0, Math.max(0, v.inhabitants.size() - listRows()));
                return true;
            }
            if (tab == Tab.USERS) {
                userScroll = Mth.clamp(userScroll - (int) Math.signum(sy), 0, Math.max(0, v.users.list.size() + 1 - userRows()));
                return true;
            }
            if (tab == Tab.LOG) {
                logScroll = Mth.clamp(logScroll - (int) Math.signum(sy), 0, Math.max(0, filteredLog(v).size() - logRows()));
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
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        // M62: the panel is drawn in code (it grows with the screen): a vanilla frame, the dark content area, the inventory slots
        int[] p = panelBounds();
        panel(g, p[0], p[1], p[2], p[3]);
        int cx = p[0] + CONTENT_X, cy = p[1] + CONTENT_Y;
        g.fill(cx, cy, cx + cw, cy + ch, 0xFF373737);
        g.fill(cx + 1, cy + 1, cx + cw, cy + ch, 0xFFFFFFFF);
        g.fill(cx + 1, cy + 1, cx + cw - 1, cy + ch - 1, 0xFF2E3339);
        for (int i = TankData.LEGACY_HEATER_SLOTS; i < menu.slots.size(); i++) slotFrame(g, leftPos + menu.getSlot(i).x - 1, topPos + menu.getSlot(i).y - 1);
        // old heater slots (M1-M3): a slot frame only while a heater is left in it (take-out only since M4)
        for (int i = 0; i < TankData.LEGACY_HEATER_SLOTS; i++) {
            if (menu.getSlot(i).hasItem()) slotFrame(g, leftPos + TankMenu.HEATER_X - 1, topPos + TankMenu.SLOT_Y + i * 18 - 1);
        }
    }

    /** M62: a vanilla-style panel (black outline, white light edge, grey face, dark shadow edge). */
    static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 2, y, x + w - 3, y + 1, 0xFF000000);
        g.fill(x, y + 2, x + 1, y + h - 3, 0xFF000000);
        g.fill(x + w - 1, y + 3, x + w, y + h - 2, 0xFF000000);
        g.fill(x + 3, y + h - 1, x + w - 2, y + h, 0xFF000000);
        g.fill(x + 1, y + 1, x + 2, y + 2, 0xFF000000);
        g.fill(x + w - 3, y + 1, x + w - 2, y + 2, 0xFF000000);
        g.fill(x + w - 2, y + 2, x + w - 1, y + 3, 0xFF000000);
        g.fill(x + 1, y + h - 3, x + 2, y + h - 2, 0xFF000000);
        g.fill(x + 2, y + h - 2, x + 3, y + h - 1, 0xFF000000);
        g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, 0xFF000000);
        g.fill(x + 2, y + 1, x + w - 3, y + 3, 0xFFFFFFFF);
        g.fill(x + 1, y + 2, x + 3, y + h - 3, 0xFFFFFFFF);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xFFC6C6C6);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 2, 0xFF555555);
        g.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, 0xFF555555);
        g.fill(x + w - 3, y + 2, x + w - 2, y + 3, 0xFFC6C6C6);
        g.fill(x + 2, y + h - 3, x + 3, y + h - 2, 0xFFC6C6C6);
    }

    /** M62: the frame of a slot (18 x 18 at x, y). */
    private static void slotFrame(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF373737);
        g.fill(x + 1, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        pendingTooltip = null;
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (pendingTooltip != null) {
            if (geneTooltipForTest) g.setComponentTooltipForNextFrame(font, pendingTooltip, leftPos + ox + 96, topPos + oy + 22);
            else g.setComponentTooltipForNextFrame(font, pendingTooltip, mouseX, mouseY);
        } else {
        }
        if (nameTooltipForTest && nameBox.visible) {
            g.setTooltipForNextFrame(font, font.split(Component.translatable("gui.bettacraft.tank.name_tooltip"), 170), nameBox.getX() + 4, nameBox.getY() + 26);
        }
    }

    /** M56 dev smoke test only: shows the tooltip of the name field as if the mouse were on it. */
    private boolean nameTooltipForTest;

    public void hoverNameForTest() {
        nameTooltipForTest = true;
    }

    /**
     * M56 dev smoke test only: the visible buttons and text fields of the current tab whose label (or empty field's hint) is wider
     * than their room; a {@link FitButton} shrinks its label to fit, so it only counts below 75 % of its size.
     */
    public List<String> overflowForTest() {
        List<String> out = new java.util.ArrayList<>();
        for (var r : children()) {
            if (!(r instanceof net.minecraft.client.gui.components.AbstractWidget w) || !w.visible) continue;
            if (w instanceof EditBox e) {
                Component hint = e == nameBox ? Component.translatable("gui.bettacraft.tank.name_hint")
                        : e == addUserBox ? Component.translatable("gui.bettacraft.users.name_hint") : null;
                if (hint != null && e.getValue().isEmpty() && font.width(hint) > e.getInnerWidth()) {
                    out.add("hint '" + hint.getString() + "' " + font.width(hint) + " > " + e.getInnerWidth());
                }
                continue;
            }
            int room = w.getWidth() - 4, tw = font.width(w.getMessage());
            if (w instanceof TabButton) { // drawn smaller, cut off with "..." when too long
                int tabRoom = (int) ((w.getWidth() - 3) / TAB_LABEL_SCALE);
                if (tw > tabRoom) out.add("tab '" + w.getMessage().getString() + "' " + tw + " > " + tabRoom + " (cut off)");
                continue;
            }
            if (tw <= room) continue;
            if (w instanceof FitButton && room / (float) tw >= 0.75F) continue; // a little smaller stays readable
            out.add("'" + w.getMessage().getString() + "' " + tw + " > " + room + (w instanceof FitButton ? " (shrunk to " + Math.round(100F * room / tw) + " %)" : ""));
        }
        return out;
    }

    /** M62 dev smoke test only: the lines of text of the last frame that were drawn at less than 75 % of their size. */
    public List<String> textFitForTest() {
        return List.copyOf(fitProblems);
    }

    /** M62 dev smoke test only: the size of the content area now (width, height). */
    public int[] contentSizeForTest() {
        return new int[]{cw, ch};
    }

    /** M21.6 dev smoke test only: shows the gene tooltip of the selected tankmate as if the mouse were on its name. */
    private boolean geneTooltipForTest;

    public void showGeneTooltipForTest(boolean on) {
        geneTooltipForTest = on;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // M62: everything below is placed from the corner of the panel (the slots hang at leftPos / topPos)
        g.pose().pushMatrix();
        g.pose().translate(ox, oy);
        fitProblems.clear();
        drawTab(g, mouseX - ox, mouseY - oy);
        g.pose().popMatrix();
    }

    private void drawTab(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        TankView v = view();
        if (v == null) {
            g.text(font, Component.translatable("gui.bettacraft.loading"), 12, 24, org.overengineer.bettacraft.client.ClientCompat.textColor(TEXT_LIGHT), false);
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
            case USERS -> drawUsers(g, v, mouseX - leftPos, mouseY - topPos);
            case WATER -> {
                drawWater(g, v);
                // M24: what the nitrate means (hover over its line)
                int mx = mouseX - leftPos, my = mouseY - topPos;
                if (nitrateTip != null && mx >= 11 && mx <= cw + 3 && my >= nitrateTipY - 1 && my < nitrateTipY + 10) {
                    pendingTooltip = new java.util.ArrayList<>();
                    for (var l : font.split(nitrateTip, 220)) pendingTooltip.add(Component.literal(toText(l)));
                }
                // M25: what the water chemistry is made of and whom it does not suit
                if (chemTip != null && mx >= 11 && mx <= cw + 3 && my >= chemTipY - 1 && my < chemTipY + 10) pendingTooltip = new java.util.ArrayList<>(chemTip);
                // M66: the oxygen: the target, the saturation, what the air and the plants add, what the animals use
                if (oxygenTip != null && mx >= 11 && mx <= cw + 3 && my >= oxygenTipY - 1 && my < oxygenTipY + 10) {
                    pendingTooltip = new java.util.ArrayList<>();
                    for (var l : font.split(oxygenTip, 220)) pendingTooltip.add(Component.literal(toText(l)));
                }
            }
            case DEVICES -> drawDevices(g, v);
            case CLIMATE -> drawClimate(g, v);
            case SUMP -> drawSump(g, v);
            case PLANTS -> {
                uptakeTip = null;
                if (aquaticPage) drawAquatic(g, v);
                else drawPlants(g, v);
                int mx = mouseX - leftPos, my = mouseY - topPos;
                if (uptakeTip != null && mx >= 11 && mx <= cw + 3 && my >= uptakeTipY - 1 && my < uptakeTipY + 10) {
                    pendingTooltip = new java.util.ArrayList<>();
                    for (var l : font.split(uptakeTip, 220)) pendingTooltip.add(Component.literal(toText(l)));
                }
            }
        }
        // lock symbol left of the interior button (not on its label)
        if (v.locked && tab == Tab.OVERVIEW) g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXTURE, enterButton.getX() - leftPos - ox - 12, 125 + ch - CONTENT_H, 16, 232, 10, 10, 256, 256);
    }

    /** A line of text that ends inside the content area (a longer translation is drawn a little smaller). */
    private void line(GuiGraphicsExtractor g, Component c, int x, int y, int color) {
        fitLine(g, c, x, y, color, CONTENT_X + cw - 3 - x);
    }

    /** M62: the width a line of text has from the left edge of the content (226 in the smallest menu). */
    private int tw() {
        return cw - 8;
    }

    /** M62: the x of the bars at the right end of a line (168 in the smallest menu). */
    private int barX() {
        return cw - 66;
    }

    private int listRows() {
        return LIST_ROWS + (ch - CONTENT_H) / ROW_H;
    }

    private int critterRowCount() {
        return CRITTER_ROWS + (ch - CONTENT_H) / ROW_H;
    }

    private int userRows() {
        return USER_ROWS + (ch - CONTENT_H) / ROW_H;
    }

    private int logRows() {
        return LOG_ROWS + (ch - CONTENT_H) / 10;
    }

    /** M62 dev smoke test only: lines of text drawn this frame at less than 75 % of their size (they did not fit). */
    private final List<String> fitProblems = new ArrayList<>();

    /** Text at (x, y), scaled down (never up) to at most {@code maxWidth} pixels. */
    /**
     * M62: text at (x, y) cut to {@code maxWidth} with "..." (instead of a tiny font); the whole text is the tooltip while the
     * mouse is on it.
     */
    private void clipLine(GuiGraphicsExtractor g, Component c, int x, int y, int color, int maxWidth, int mx, int my) {
        if (font.width(c) <= maxWidth) {
            g.text(font, c, x, y, org.overengineer.bettacraft.client.ClientCompat.textColor(color), false);
            return;
        }
        String cut = font.plainSubstrByWidth(c.getString(), maxWidth - font.width("...")) + "...";
        g.text(font, Component.literal(cut).withStyle(c.getStyle()), x, y, org.overengineer.bettacraft.client.ClientCompat.textColor(color), false);
        if (mx >= x && mx < x + maxWidth && my >= y - 1 && my < y + 9) pendingTooltip = List.of(c);
    }

    private void fitLine(GuiGraphicsExtractor g, Component c, int x, int y, int color, int maxWidth) {
        int w = font.width(c);
        if (w <= maxWidth || w <= 0) {
            g.text(font, c, x, y, org.overengineer.bettacraft.client.ClientCompat.textColor(color), false);
            return;
        }
        float s = Math.max(0.5F, maxWidth / (float) w);
        if (s < 0.75F) fitProblems.add("'" + c.getString() + "' " + w + " > " + maxWidth + " (" + Math.round(s * 100) + " %)");
        g.pose().pushMatrix();
        g.pose().translate(x, y + (1 - s) * 4);
        g.pose().scale(s, s);
        g.text(font, c, 0, 0, org.overengineer.bettacraft.client.ClientCompat.textColor(color), false);
        g.pose().popMatrix();
    }

    private void drawOverview(GuiGraphicsExtractor g, TankView v, int mx, int my) {
        int x = 11, dw = cw - CONTENT_W, dh = ch - CONTENT_H;
        int pct = v.capacityMb <= 0 ? 0 : (int) (v.waterMb * 100 / v.capacityMb);
        line(g, Component.translatable("gui.bettacraft.tank.size_water", v.sizeX, v.sizeY, v.sizeZ, v.waterMb, v.capacityMb, pct), x, overviewY(0), TEXT_LIGHT);
        int tempColor = TankTemperature.isTolerable(v.waterTemp) ? (TankTemperature.isOptimal(v.waterTemp) ? 0x80FF80 : 0xFFE080) : 0xFF7070;
        TankView.Climate cl = v.climate;
        int y = overviewY(1);
        if (cl.hasLand() && Float.isFinite(cl.air)) {
            // M31: a terrarium shows its air, a paludarium its water and its air
            Component air = Component.translatable("gui.bettacraft.terrarium.overview_air", fmt(cl.air), Math.round(cl.humidity), fmt(cl.uv));
            line(g, v.waterMb > 0 ? Component.translatable("gui.bettacraft.terrarium.overview_both", fmt(v.waterTemp), air, fmt(v.ambient))
                    : Component.translatable("gui.bettacraft.terrarium.overview_dry", air, fmt(v.ambient)), x, y, 0xC8E8A0);
        } else {
            line(g, Component.translatable("gui.bettacraft.tank.temperature", fmt(v.waterTemp), fmt(v.ambient)), x, y, tempColor);
        }
        y = overviewY(2);
        int labelRoom = (modeButton.visible ? modeButton.getX() : minusButton.getX()) - leftPos - ox - 4 - x;
        fitLine(g, Component.translatable("gui.bettacraft.tank.target"), x, y + 2, TEXT_LIGHT, labelRoom);
        g.centeredText(font, fmt(v.targetTemp) + " °C", 203 + dw, y + 2, org.overengineer.bettacraft.client.ClientCompat.textColor(0xFFFFFF));
        y = overviewY(3);
        double water = Math.max(0.001, v.waterMb / 1000.0);
        // M9: below the ambient temperature the coolers matter, above it the heaters
        boolean cooling = v.targetTemp < v.ambient;
        double cap = cooling ? v.coolerBlocks : v.heaterBlocks;
        Component heat;
        if (cap <= 0) heat = Component.translatable("gui.bettacraft.tank.heaters_none").withStyle(ChatFormatting.GRAY);
        else if (cap + 1e-6 >= water) heat = Component.translatable("gui.bettacraft.tank.heaters_ok", (int) cap, fmt((float) water)).withStyle(ChatFormatting.GREEN);
        else heat = Component.translatable("gui.bettacraft.tank.heaters_weak", (int) cap, fmt((float) water)).withStyle(ChatFormatting.GOLD);
        line(g, Component.translatable(cooling ? "gui.bettacraft.tank.coolers" : "gui.bettacraft.tank.heaters", heat), x, y, TEXT_LIGHT);
        // M66: "-" where a device is not installed (no word that needs a gender in every language)
        Component dash = Component.literal("-").withStyle(ChatFormatting.GRAY);
        Component feeder = v.feederFood < 0 ? dash
                : Component.translatable("gui.bettacraft.tank.feeder_state", v.feederFood, v.feederPerDay)
                .withStyle(v.feederFood == 0 ? ChatFormatting.GOLD : ChatFormatting.GREEN);
        Component light = v.lights == 0 ? dash
                : Component.translatable("gui.bettacraft.tank.light_state", v.lightsLit, v.lights);
        line(g, Component.translatable("gui.bettacraft.tank.feeder", feeder).append("   ")
                .append(Component.translatable("gui.bettacraft.tank.light", light)), x, overviewY(4), TEXT_LIGHT);
        // M4: filters outside (blocks next to the tank, filters in cabinets below) and inside (interior)
        int outside = v.filterBlocks + v.cabinetFilters;
        Component filters = outside + v.filters == 0 ? dash
                : Component.translatable("gui.bettacraft.tank.filter_state", outside, v.filterHeaters, v.filters).withStyle(ChatFormatting.AQUA);
        line(g, Component.translatable("gui.bettacraft.tank.filters", filters), x, overviewY(5), TEXT_LIGHT);
        // M66: the stored energy and the demand (before in the water tab), only while a device needs power
        int next = 6;
        if (v.water.anyPower()) {
            TankView.Water pw = v.water;
            fitLine(g, Component.translatable("gui.bettacraft.water.power", String.format(java.util.Locale.ROOT, "%,d", pw.buffer),
                    String.format(java.util.Locale.ROOT, "%,d", pw.bufferCap), pw.demand), x, overviewY(next), TEXT_LIGHT, tw());
            if (mx >= x && mx < x + tw() && my >= overviewY(next) && my < overviewY(next) + 9) pendingTooltip = powerTooltip(pw);
            next++;
        }
        // M18: as soon as anything counts in fractions (small animals, young, nest), the line shows the stocking
        boolean fractions = v.critters.total > 0 || v.stockYoung > 1e-6 || v.stockNest > 1e-6;
        MutableComponent inh = v.critters.total > 0
                ? Component.translatable("gui.bettacraft.tank.stocking", v.inhabitants.size(), v.critters.total, fmt2(v.stock), v.hardCap)
                : fractions ? Component.translatable("gui.bettacraft.tank.stocking_fish", v.inhabitants.size(), fmt2(v.stock), v.hardCap)
                : Component.translatable("gui.bettacraft.tank.inhabitants", v.inhabitants.size(), v.adults, v.hardCap);
        if (v.crowded) inh.append(" ").append(Component.translatable("gui.bettacraft.tank.crowded").withStyle(ChatFormatting.RED));
        // the lines beside the interior button end left of it (and of the lock symbol)
        int enterX = enterButton.getX() - leftPos - ox, enterY = 123 + dh;
        int narrow = enterX - (v.locked ? 15 : 3) - x;
        y = overviewY(next);
        int room = y + 9 > enterY ? narrow : tw();
        fitLine(g, inh, x, y, TEXT_LIGHT, room);
        if (mx >= x && mx < x + room && my >= y && my < y + 9) pendingTooltip = stockingTooltip(v);
        stockLineX = leftPos + ox + x + 20;
        stockLineY = topPos + oy + y + 4;
        // the warnings, as many as there is room for (M62: all of them in a taller menu; before only the first one)
        List<Component> warn = new ArrayList<>();
        List<List<Component>> tips = new ArrayList<>();
        if (mismatch(v)) warn.add(Component.translatable("gui.bettacraft.tank_mode.too_much_water").withStyle(ChatFormatting.GOLD)); // M33.1
        else if (TankView.showFillHint(v)) {
            warn.add(Component.translatable("gui.bettacraft.tank.fill_hint", v.minInsertMb).withStyle(ChatFormatting.GOLD));
        }
        if (v.water.qualityEnabled && v.water.quality < org.overengineer.bettacraft.tank.WaterQuality.POOR_BELOW) {
            warn.add(Component.translatable(v.water.quality < v.water.badBelow ? "gui.bettacraft.water.warn_bad" : "gui.bettacraft.water.warn_poor",
                    Math.round(v.water.quality * 100)).withStyle(v.water.quality < v.water.badBelow ? ChatFormatting.RED : ChatFormatting.GOLD));
        }
        Component unpowered = unpowered(v);
        if (unpowered != null) warn.add(unpowered);
        while (tips.size() < warn.size()) tips.add(null);
        // M62: a shoal that is too small: which fish and how many it should be (all of them in the tooltip)
        if (!v.shoals.isEmpty()) {
            warn.add(shoalWarning(v));
            tips.add(shoalTooltip(v));
        }
        if (v.legacyHeaters > 0) warn.add(Component.translatable("gui.bettacraft.tank.legacy_heaters", v.legacyHeaters).withStyle(ChatFormatting.GOLD));
        while (tips.size() < warn.size()) tips.add(null);
        for (int i = 0; i < warn.size(); i++) {
            y = overviewY(next + 1 + i);
            if (y + 8 > CONTENT_Y + ch) break;
            room = y + 9 > enterY ? narrow : tw();
            fitLine(g, warn.get(i), x, y, TEXT_LIGHT, room);
            if (tips.get(i) != null && mx >= x && mx < x + room && my >= y && my < y + 9) pendingTooltip = tips.get(i);
        }
    }

    /** M66: the groups of the equipment that need power and whether they have it, and the springs that stand in. */
    private static List<Component> powerTooltip(TankView.Water w) {
        List<Component> out = new ArrayList<>();
        out.add(Component.translatable("gui.bettacraft.water.power_tip").withStyle(ChatFormatting.WHITE));
        MutableComponent groups = Component.empty();
        groups.append(powerGroup("power_heat", w.heaterPower, w.heatPowered)).append("  ")
                .append(powerGroup("power_filter", w.filterPower, w.filterPowered)).append("  ")
                .append(powerGroup("power_light", w.lightPower, w.lightPowered));
        // M37: the wound springs (cabinet, external filters) drive devices while the FE is short
        if (w.springUnits > 0) groups.append("  ").append(Component.translatable("gui.bettacraft.water.power_spring",
                String.format(java.util.Locale.ROOT, "%.1f", w.springUnits)).withStyle(ChatFormatting.GOLD));
        out.add(groups);
        return out;
    }

    /** M62: "Shoal too small: Neon Tetra 3 of 6" (and how many more kinds). */
    private static Component shoalWarning(TankView v) {
        TankView.Shoal s = v.shoals.getFirst();
        MutableComponent c = Component.translatable("gui.bettacraft.tank.shoal_warning", Component.translatable("gui.bettacraft.shoal.line",
                s.name(), s.count(), s.need()));
        if (v.shoals.size() > 1) c.append(Component.translatable("gui.bettacraft.shoal.more", v.shoals.size() - 1));
        return c.withStyle(ChatFormatting.GOLD);
    }

    /** M62: every shoal that is too small, with its fish and the minimum, and what to do. */
    static List<Component> shoalTooltip(TankView v) {
        List<Component> tip = new ArrayList<>();
        tip.add(Component.translatable("gui.bettacraft.shoal.title").withStyle(ChatFormatting.YELLOW));
        for (TankView.Shoal s : v.shoals) {
            tip.add(Component.translatable("gui.bettacraft.shoal.line", s.name(), s.count(), s.need()).withStyle(ChatFormatting.GOLD));
        }
        tip.add(Component.translatable("gui.bettacraft.shoal.hint").withStyle(ChatFormatting.GRAY));
        return tip;
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

    /** M66: a thin line across the content, between two groups of lines. */
    private void separator(GuiGraphicsExtractor g, int y) {
        g.fill(9, y, cw + 5, y + 1, 0x55FFFFFF);
    }

    /** M66: a small caption with a line after it, over a group of lines (9 px high). */
    private void section(GuiGraphicsExtractor g, String key, int y) {
        Component label = Component.translatable(key);
        float s = 0.75F;
        int w = Math.round(font.width(label) * s);
        g.pose().pushMatrix();
        g.pose().translate(11, y + 1);
        g.pose().scale(s, s);
        g.text(font, label, 0, 0, org.overengineer.bettacraft.client.ClientCompat.textColor(0x8FB8C8), false);
        g.pose().popMatrix();
        g.fill(11 + w + 4, y + 4, cw + 5, y + 5, 0x55FFFFFF);
    }

    /**
     * M9: the tab Water. M66: first what the water is (the amount, TDS and pH, evaporation, oxygen), then a line, then how good it is and
     * what keeps it good (quality, nitrate, filters, heaters and coolers); the power moved to the overview, the oxygen came
     * from the tab Devices, the lines about the glass and "power not needed" are gone.
     */
    private void drawWater(GuiGraphicsExtractor g, TankView v) {
        TankView.Water w = v.water;
        int x = 11, y = 20;
        oxygenTip = null;
        int pct = v.capacityMb <= 0 ? 0 : (int) (v.waterMb * 100 / v.capacityMb);
        line(g, Component.translatable("gui.bettacraft.water.volume", v.waterMb, v.capacityMb, pct), x, y, TEXT_LIGHT);
        y += 11;
        // M31: a dry terrarium has no water values
        if (v.waterMb <= 0 && v.climate.hasLand()) {
            fitLine(g, Component.translatable("gui.bettacraft.terrarium.no_water").withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT, tw());
            return;
        }
        // M20: in salt or brackish water the salt values instead; M25: with water chemistry its values (salt water: the pH after the salt)
        boolean salty = w.salinity >= org.overengineer.bettacraft.tank.Salinity.BRACKISH_FROM;
        chemTip = null;
        if (w.chemOn) {
            fitLine(g, salty ? salinityLine(w).copy().append(Component.literal(" · pH " + org.overengineer.bettacraft.chem.ChemItem.ph(w.ph)).withStyle(phColor(w)))
                    : chemLine(w), x, y, TEXT_LIGHT, tw());
            chemTip = chemTooltip(w, salty);
            chemTipY = y;
            y += 11;
        } else if (salty) {
            fitLine(g, salinityLine(w), x, y, TEXT_LIGHT, tw());
            y += 11;
        }
        // M66: the evaporation belongs to the amount of water
        Component evap;
        if (!w.evaporationEnabled) evap = Component.translatable("gui.bettacraft.water.evaporation_off").withStyle(ChatFormatting.GRAY);
        else if (w.openTop == 0) evap = Component.translatable("gui.bettacraft.water.covered").withStyle(ChatFormatting.GREEN);
        else evap = Component.translatable("gui.bettacraft.water.evaporation", fmt((float) w.evaporationPerDay), w.openTop, w.totalTop)
                    .withStyle(ChatFormatting.GOLD);
        line(g, evap, x, y, TEXT_LIGHT);
        y += 11;
        // M66: the oxygen is a water value (before in the tab Devices); the details are in the tooltip
        TankView.Tech t = v.tech;
        if (t.oxygenOn && !Float.isNaN(t.oxygen) && v.waterMb > 0) {
            String st = org.overengineer.bettacraft.tank.TankAeration.state(t.oxygen);
            ChatFormatting col = st.equals("good") ? ChatFormatting.GREEN : st.equals("low") ? ChatFormatting.GOLD : ChatFormatting.RED;
            fitLine(g, Component.translatable("gui.bettacraft.devices.oxygen", fmt(t.oxygen), Component.translatable("gui.bettacraft.devices.oxygen." + st).withStyle(col)),
                    x, y, TEXT_LIGHT, barX() - 4 - x);
            int bx = barX(), bw = BAR_W;
            double max = Math.max(1, t.saturation * 1.3);
            g.fill(bx, y + 1, bx + bw, y + 7, 0xFF202020);
            int c = st.equals("good") ? 0xFF50A0E0 : st.equals("low") ? 0xFFD0A040 : 0xFFD04040;
            g.fill(bx, y + 1, bx + (int) Math.round(bw * Mth.clamp(t.oxygen / max, 0, 1)), y + 7, c);
            int sat = bx + (int) Math.round(bw * t.saturation / max);
            g.fill(sat, y, sat + 1, y + 8, 0xFFFFFFFF);
            oxygenTip = Component.translatable("gui.bettacraft.devices.oxygen_detail", fmt((float) t.target), fmt((float) t.saturation), fmt((float) t.exchange),
                    fmt((float) t.use));
            oxygenTipY = y;
            y += 11;
        }
        separator(g, y + 1);
        y += 4;
        // ---- how good the water is and what keeps it good
        if (w.qualityEnabled) {
            ChatFormatting col = switch (w.state) {
                case "clean" -> ChatFormatting.GREEN;
                case "good" -> ChatFormatting.DARK_GREEN;
                case "poor" -> ChatFormatting.GOLD;
                default -> ChatFormatting.RED;
            };
            Component label = Component.translatable("gui.bettacraft.water.quality", Math.round(w.quality * 100),
                    Component.translatable("gui.bettacraft.water.state." + w.state).withStyle(col));
            int bx = barX(), bw = BAR_W;
            fitLine(g, label, x, y, TEXT_LIGHT, bx - 4 - x);
            g.fill(bx, y + 1, bx + bw, y + 7, 0xFF202020);
            int color = w.quality < w.badBelow ? 0xFFD04040 : w.quality < org.overengineer.bettacraft.tank.WaterQuality.POOR_BELOW ? 0xFFD0A040 : 0xFF50A0E0;
            g.fill(bx, y + 1, bx + Math.round(bw * Mth.clamp(w.quality, 0, 1)), y + 7, color);
            g.fill(bx + Math.round(bw * w.badBelow), y, bx + Math.round(bw * w.badBelow) + 1, y + 8, 0xFFFF6060);
            y += 11;
            line(g, Component.translatable("gui.bettacraft.water.decline", fmt((float) (w.declinePerDay * 100)), fmt((float) w.animalsPerBlock)), x, y, TEXT_LIGHT);
            y += 11;
            int outside = v.filterBlocks + v.cabinetFilters;
            // M20: a reef tank cleaned by its skimmer and live rock alone has no "no filter" warning
            Component filters = outside + w.internalFilters == 0 && w.filterCapacity <= 0
                    ? Component.translatable("gui.bettacraft.water.no_filter").withStyle(ChatFormatting.GOLD)
                    : Component.translatable("gui.bettacraft.water.filters", outside, w.internalFilters, (int) w.filterCapacity,
                    Math.round(w.filterShare * 100)).withStyle(ChatFormatting.AQUA);
            // M40: a multiblock filter that reaches the tank without its guards (or with a pipe through it) does not filter it
            if (!w.filterWarnings.isEmpty()) {
                String[] kp = w.filterWarnings.getFirst().split("[|]", 2);
                filters = Component.translatable("gui.bettacraft.water.filter_warning." + (kp.length > 1 ? kp[1] : "no_intake_guard"), kp[0])
                        .withStyle(ChatFormatting.RED);
                if (w.filterWarnings.size() > 1) filters = filters.copy().append(Component.literal(" (+" + (w.filterWarnings.size() - 1) + ")"));
            }
            fitLine(g, filters, x, y, TEXT_LIGHT, tw());
            y += 11;
        } else {
            line(g, Component.translatable("gui.bettacraft.water.quality_off").withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT);
            y += 11;
        }
        Component fresh = w.lastFreshWater < 0 ? Component.translatable("gui.bettacraft.water.fresh_never")
                : Component.translatable("gui.bettacraft.water.fresh", w.lastFreshWater / 24000L + 1);
        // M24: nitrate (dissolved waste) and when the water was last changed, in one line
        if (w.nitrateInterval > 0) {
            var st = org.overengineer.bettacraft.tank.TankNitrate.state(w.nitrate, w.nitrateStress, w.nitrateLethal);
            ChatFormatting col = switch (st) {
                case LOW -> ChatFormatting.GREEN;
                case RISING -> ChatFormatting.YELLOW;
                case HIGH -> ChatFormatting.GOLD;
                case LETHAL -> ChatFormatting.RED;
            };
            Component trend = w.nitrateRate < -1e-6 ? Component.translatable("gui.bettacraft.water.nitrate_falling")
                    : Component.translatable("gui.bettacraft.water.nitrate_rate", fmt((float) w.nitrateRate));
            Component no3 = Component.translatable("gui.bettacraft.water.nitrate", Math.round(w.nitrate),
                    Component.translatable("gui.bettacraft.water.nitrate." + st.name().toLowerCase(java.util.Locale.ROOT)).withStyle(col), trend);
            fitLine(g, no3.copy().append(Component.literal(" · ").append(fresh).withStyle(ChatFormatting.GRAY)), x, y, TEXT_LIGHT, tw());
            nitrateTip = Component.translatable("gui.bettacraft.water.nitrate_tooltip", w.nitrateInterval, Math.round(w.nitrateStress), Math.round(w.nitrateLethal));
            nitrateTipY = y;
        } else {
            line(g, fresh.copy().withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT);
            nitrateTip = null;
        }
        y += 11;
        Component equipment = Component.translatable("gui.bettacraft.water.temp_equipment", (int) v.heaterBlocks, (int) v.coolerBlocks);
        if (w.fertilizerOn) equipment = equipment.copy().append(Component.literal(" · ").withStyle(ChatFormatting.GRAY)).append(fertilizerLine(w)); // M37
        fitLine(g, equipment, x, y, TEXT_LIGHT, tw());
        y += 11;
        if (w.reefLine()) fitLine(g, reefLine(w), x, y, TEXT_LIGHT, tw()); // M20: skimmers, pumps, live rock
        else if (!w.chemOn) line(g, Component.translatable("gui.bettacraft.water.hint").withStyle(ChatFormatting.GRAY), x, y, 0xA0A0A0);
    }

    /** M37: "Fertilizer 80 % (good)": none / low (grey, yellow), good (green), overdosed (gold). */
    static Component fertilizerLine(TankView.Water w) {
        var st = org.overengineer.bettacraft.fertilizer.Fertilizer.state(w.fertilizer, w.fertilizerOver);
        ChatFormatting col = switch (st) {
            case NONE -> ChatFormatting.GRAY;
            case LOW -> ChatFormatting.YELLOW;
            case GOOD -> ChatFormatting.GREEN;
            case OVERDOSED -> ChatFormatting.GOLD;
        };
        return Component.translatable("gui.bettacraft.water.fertilizer", Math.round(w.fertilizer * 100),
                Component.translatable("gui.bettacraft.water.fertilizer." + st.name().toLowerCase(java.util.Locale.ROOT))).withStyle(col);
    }

    /** M25: "TDS 250 ppm (medium hard) · pH 7.5 (neutral, steady)", red when the water does not suit an animal. */
    private static Component chemLine(TankView.Water w) {
        MutableComponent hard = Component.translatable("gui.bettacraft.chem.hardness." + w.hardness);
        MutableComponent c = Component.translatable("gui.bettacraft.water.chem", Math.round(w.tds), hard,
                Component.literal(org.overengineer.bettacraft.chem.ChemItem.ph(w.ph)).withStyle(phColor(w)),
                org.overengineer.bettacraft.chem.ChemItem.trend(w.ph, w.phTarget));
        if (!w.chemOff.isEmpty()) c.append(Component.literal(" (!)").withStyle(anyBad(w) ? ChatFormatting.RED : ChatFormatting.GOLD));
        return c;
    }

    private static boolean anyBad(TankView.Water w) {
        for (String s : w.chemOff) if (s.endsWith("|bad")) return true;
        return false;
    }

    private static ChatFormatting phColor(TankView.Water w) {
        return w.ph < 6.5 ? ChatFormatting.GOLD : w.ph <= 7.5 ? ChatFormatting.GREEN : ChatFormatting.AQUA;
    }

    /** M25: the drivers of the pH, the CO2 and the animals the water does not suit. */
    private static List<Component> chemTooltip(TankView.Water w, boolean salty) {
        List<Component> out = new ArrayList<>();
        out.add(Component.translatable("gui.bettacraft.water.chem_tip.values", Math.round(w.tds), org.overengineer.bettacraft.chem.ChemItem.ph(w.ph),
                org.overengineer.bettacraft.chem.ChemItem.ph(w.phTarget), fmt(w.co2)).withStyle(ChatFormatting.WHITE));
        List<Component> drivers = new ArrayList<>();
        if (w.acid > 0.01) drivers.add(Component.translatable("gui.bettacraft.water.chem_tip.soil", Math.round(w.acid * 100)));
        if (w.alkaline > 0.01) drivers.add(Component.translatable("gui.bettacraft.water.chem_tip.lime", Math.round(w.alkaline * 100)));
        if (w.peat > 0.01) drivers.add(Component.translatable("gui.bettacraft.water.chem_tip.peat", Math.round(w.peat * 100)));
        if (w.tannin > 0.01) drivers.add(Component.translatable("gui.bettacraft.water.chem_tip.leaves", Math.round(w.tannin * 100)));
        if (w.wood > 0.01) drivers.add(Component.translatable("gui.bettacraft.water.chem_tip.wood", Math.round(w.wood * 100)));
        // M25 part b: what doses CO2 and how much there is
        if (w.co2On && (w.co2Sources > 0 || w.co2 > 5)) {
            ChatFormatting f = switch (w.co2State) {
                case "deadly" -> ChatFormatting.RED;
                case "high" -> ChatFormatting.GOLD;
                case "good" -> ChatFormatting.GREEN;
                default -> ChatFormatting.GRAY;
            };
            out.add(Component.translatable("gui.bettacraft.water.co2_tip", w.co2Sources, fmt((float) w.co2Bubbles), fmt((float) w.co2Target),
                    Component.translatable("gui.bettacraft.co2.state." + w.co2State)).withStyle(f));
        }
        if (salty) out.add(Component.translatable("gui.bettacraft.water.chem_tip.salt").withStyle(ChatFormatting.GRAY));
        if (salty && w.dosingOn && org.overengineer.bettacraft.tank.Salinity.marine(w.salinity)) { // M25 part c: Balling light
            boolean low = w.kh < 6 || w.magnesium < 1150;
            out.add(Component.translatable("gui.bettacraft.water.reef_tip", String.format(java.util.Locale.ROOT, "%.1f", w.kh), Math.round(w.magnesium),
                    Math.round(w.trace * 100)).withStyle(low ? ChatFormatting.GOLD : ChatFormatting.AQUA));
            if (low) out.add(Component.translatable("gui.bettacraft.water.reef_tip.low").withStyle(ChatFormatting.GOLD));
            if (org.overengineer.bettacraft.chem.WaterChemistry.traceBonus(w.trace))
                out.add(Component.translatable("gui.bettacraft.water.reef_tip.trace").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (drivers.isEmpty()) out.add(Component.translatable("gui.bettacraft.water.chem_tip.nothing").withStyle(ChatFormatting.GRAY));
        else {
            MutableComponent l = Component.translatable("gui.bettacraft.water.chem_tip.drivers").withStyle(ChatFormatting.GRAY);
            for (int i = 0; i < drivers.size(); i++) l.append(i == 0 ? " " : ", ").append(drivers.get(i));
            out.add(l);
        }
        for (String s : w.chemOff) {
            int bar = s.indexOf('|');
            boolean bad = s.endsWith("|bad");
            out.add(Component.translatable(bad ? "gui.bettacraft.water.chem_tip.bad" : "gui.bettacraft.water.chem_tip.stress",
                    Component.translatable(bar < 0 ? s : s.substring(0, bar))).withStyle(bad ? ChatFormatting.RED : ChatFormatting.GOLD));
        }
        out.add(Component.translatable("gui.bettacraft.water.chem_tip.measure").withStyle(ChatFormatting.DARK_GRAY));
        return out;
    }

    /**
     * M23: tab Devices: aeration, fry and the intake sponges, the lights with dimmer and daylight timer. M66: in two groups with a
     * caption each (air, light); the oxygen is a water value now and shows in the tab Water.
     */
    private void drawDevices(GuiGraphicsExtractor g, TankView v) {
        TankView.Tech t = v.tech;
        int x = 11, y = 20;
        section(g, "gui.bettacraft.section.air", y);
        y += 10;
        Component air;
        if (!t.aerationOn) air = Component.translatable("gui.bettacraft.devices.aeration_off").withStyle(ChatFormatting.GRAY);
        else if (t.pumps == 0) air = Component.translatable("gui.bettacraft.devices.no_pump", t.stones, t.spongeFilters)
                .withStyle(t.stones + t.spongeFilters > 0 ? ChatFormatting.GOLD : ChatFormatting.GRAY);
        else air = Component.translatable("gui.bettacraft.devices.air", t.pumps, t.pumpsRunning, t.stones, t.spongeFilters)
                .withStyle(t.airPower && !t.airPowered ? ChatFormatting.RED : ChatFormatting.AQUA);
        fitLine(g, air, x, y, TEXT_LIGHT, tw());
        y += 11;
        if (t.oxygenOn && t.exchange > 0) {
            fitLine(g, Component.translatable("gui.bettacraft.devices.co2", Math.round(t.co2Strip * 100)).withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT, tw());
            y += 11;
        }
        if (t.fry) {
            fitLine(g, t.unprotected > 0 ? Component.translatable("gui.bettacraft.devices.fry_risk", t.unprotected).withStyle(ChatFormatting.GOLD)
                    : Component.translatable("gui.bettacraft.devices.fry_safe").withStyle(ChatFormatting.GREEN), x, y, TEXT_LIGHT, tw());
        }
        y = LIGHT_Y;
        section(g, "gui.bettacraft.section.light", y);
        y += 10;
        Component kind = Component.translatable("gui.bettacraft.devices.lights", t.bars, t.panels, t.highPower, fmt((float) t.light),
                Component.translatable("gui.bettacraft.devices.light_class." + lightClass(t.light)));
        fitLine(g, kind, x, y, TEXT_LIGHT, tw());
        y += 12;
        if (!t.timerFeature) {
            line(g, Component.translatable("gui.bettacraft.devices.timer_feature_off").withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT);
            return;
        }
        line(g, Component.translatable("gui.bettacraft.devices.dimmer"), x, LIGHT_Y + 25, TEXT_LIGHT);
        g.centeredText(font, t.dimmer + " %", 94, LIGHT_Y + 25, org.overengineer.bettacraft.client.ClientCompat.textColor(0xFFFFFF));
        line(g, Component.translatable("gui.bettacraft.devices.on"), x, LIGHT_Y + 41, TEXT_LIGHT);
        g.centeredText(font, clock(t.lightOn), 78, LIGHT_Y + 41, org.overengineer.bettacraft.client.ClientCompat.textColor(t.timer ? 0xFFFFFF : 0x808080));
        line(g, Component.translatable("gui.bettacraft.devices.off"), 128, LIGHT_Y + 41, TEXT_LIGHT);
        g.centeredText(font, clock(t.lightOff), 200, LIGHT_Y + 41, org.overengineer.bettacraft.client.ClientCompat.textColor(t.timer ? 0xFFFFFF : 0x808080));
        fitLine(g, Component.translatable("gui.bettacraft.devices.now", Math.round(t.factor * 100)).withStyle(ChatFormatting.GRAY), x, LIGHT_Y + 55, TEXT_LIGHT, tw());
    }

    /** M66: the y of the caption of the light group in the tab Devices (the buttons of the dimmer and the timer hang under it). */
    private static final int LIGHT_Y = 66;

    /** M31: the climate tab of a terrarium or paludarium. */
    /** M49: the sump tab: the water it adds, its chambers, the refugium and the devices beside its cabinet. */
    private void drawSump(GuiGraphicsExtractor g, TankView v) {
        net.minecraft.nbt.CompoundTag s = v.sump;
        int x = 11, y = 20, w = tw();
        int n = s.getIntOr("count", 0);
        double blocks = s.getDoubleOr("blocks", 0d), tank = s.getDoubleOr("tank_blocks", 0d);
        fitLine(g, Component.translatable("gui.bettacraft.sump.line", n, fmt2(blocks), fmt2(tank + blocks)), x, y, TEXT_LIGHT, w);
        y += 11;
        fitLine(g, Component.translatable(s.getBooleanOr("running", false) ? "gui.bettacraft.sump.pump_on" : "gui.bettacraft.sump.pump_off")
                .withStyle(s.getBooleanOr("running", false) ? ChatFormatting.GREEN : ChatFormatting.RED), x, y, TEXT_LIGHT, w);
        y += 13;
        MutableComponent gear = Component.translatable("gui.bettacraft.sump.heaters_line", s.getIntOr("heaters", 0)).withStyle(s.getIntOr("heaters", 0) > 0 ? ChatFormatting.GOLD : ChatFormatting.GRAY);
        gear.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
        gear.append(Component.translatable("gui.bettacraft.sump.skimmer_line", s.getIntOr("skimmers", 0)).withStyle(s.getIntOr("skimmers", 0) > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        fitLine(g, gear, x, y, TEXT_LIGHT, w);
        y += 11;
        int media = s.getIntOr("media", 0);
        fitLine(g, (media == 0 ? Component.translatable("gui.bettacraft.sump.media_none")
                : Component.translatable("gui.bettacraft.sump.media_line", media, Math.round(s.getDoubleOr("media_blocks", 0d)), Math.round(s.getDoubleOr("dirt", 0d) * 100)))
                .withStyle(media == 0 ? ChatFormatting.GRAY : s.getDoubleOr("dirt", 0d) >= 0.75 ? ChatFormatting.GOLD : ChatFormatting.WHITE), x, y, TEXT_LIGHT, w);
        y += 13;
        int algae = s.getIntOr("algae", 0);
        MutableComponent ref;
        if (!s.getBooleanOr("refugium_on", false)) ref = Component.translatable("gui.bettacraft.sump.refugium_off").withStyle(ChatFormatting.GRAY);
        else if (algae == 0) ref = Component.translatable("gui.bettacraft.sump.refugium_empty").withStyle(ChatFormatting.GRAY);
        else if (s.getIntOr("lit", 0) == 0) ref = Component.translatable("gui.bettacraft.sump.refugium_dark", algae).withStyle(ChatFormatting.GOLD);
        else {
            fitLine(g, Component.translatable("gui.bettacraft.sump.refugium_line", algae, org.overengineer.bettacraft.equipment.Sump.MAX_ALGAE)
                    .withStyle(ChatFormatting.GREEN), x, y, TEXT_LIGHT, w);
            y += 11;
            ref = Component.translatable("gui.bettacraft.sump.refugium_effect", fmt2(s.getDoubleOr("uptake", 0d)), Math.round((1 - s.getDoubleOr("algae_factor", 0d)) * 100))
                    .withStyle(ChatFormatting.GREEN);
        }
        fitLine(g, ref, x, y, TEXT_LIGHT, w);
        y += 13;
        MutableComponent dev = Component.translatable("gui.bettacraft.sump.devices", s.getIntOr("sensors", 0), s.getIntOr("dosing", 0));
        if (s.getBooleanOr("reactor", false)) dev.append("  ").append(Component.translatable("gui.bettacraft.sump.reactor"));
        fitLine(g, dev.withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT, w);
        y += 11;
        fitLine(g, Component.translatable("gui.bettacraft.sump.hint").withStyle(ChatFormatting.DARK_GRAY), x, y, TEXT_LIGHT, w);
    }

    private void drawClimate(GuiGraphicsExtractor g, TankView v) {
        TankView.Climate c = v.climate;
        int x = 11, y = 20, w = tw();
        Component mode = Component.translatable(c.mode().translationKey()).withStyle(ChatFormatting.GREEN);
        int landPct = Math.round(Math.max(0, c.land) * 100);
        fitLine(g, Component.translatable(v.waterMb > 0 ? "gui.bettacraft.terrarium.mode_line" : "gui.bettacraft.terrarium.mode_line_dry", mode, landPct, 100 - landPct),
                x, y, TEXT_LIGHT, c.settingOn ? 150 : w);
        y += 11;
        // M33.1: a fixed paludarium shows how high the water may go; a fixed type with too much water says that none comes in
        if (mismatch(v)) {
            fitLine(g, Component.translatable("gui.bettacraft.tank_mode.too_much_water").withStyle(ChatFormatting.GOLD), x, y, TEXT_LIGHT, w);
            y += 11;
        } else if (c.settingOn && c.setting() == org.overengineer.bettacraft.terrarium.TankModeSetting.PALUDARIUM && c.maxWater >= 0) {
            fitLine(g, Component.translatable("gui.bettacraft.tank_mode.limit", c.maxWater).withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT, w);
            y += 11;
        }
        if (!Float.isFinite(c.air)) {
            fitLine(g, Component.translatable("gui.bettacraft.terrarium.no_values").withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT, w);
            return;
        }
        fitLine(g, Component.translatable("gui.bettacraft.terrarium.air", fmt(c.air), fmt(c.ambient), fmt(c.target)), x, y, 0xFFD8A0, barX() - 4 - x);
        bar(g, y, (c.air - 10) / 30F, 0xFFE08040, (c.target - 10) / 30F);
        y += 11;
        fitLine(g, (c.matsUnder + c.matsWall > 0 ? Component.translatable("gui.bettacraft.terrarium.ground_bask", fmt(c.ground), fmt(c.bask))
                : Component.translatable("gui.bettacraft.terrarium.bask_only", fmt(c.bask))).withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT, w);
        y += 11;
        fitLine(g, Component.translatable("gui.bettacraft.terrarium.humidity", Math.round(c.humidity), Math.round(c.roomHumidity)), x, y, 0xA8D8FF, barX() - 4 - x);
        bar(g, y, c.humidity / 100F, 0xFF50A0E0, c.mistSetting >= 0 ? c.mistSetting / 100F : -1);
        y += 11;
        String uvClass = c.uv < 0.5 ? "none" : c.uv < 2 ? "low" : c.uv < 4 ? "medium" : "high";
        fitLine(g, Component.translatable("gui.bettacraft.terrarium.uv", fmt(c.uv), Component.translatable("gui.bettacraft.terrarium.uv." + uvClass)),
                x, y, 0xE0B0FF, barX() - 4 - x);
        bar(g, y, c.uv / 8F, 0xFFB070E0, -1);
        y += 11;
        fitLine(g, Component.translatable("gui.bettacraft.terrarium.lid", c.tops - c.openTops, c.tops).withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT, w);
        y += 12;
        separator(g, y - 2); // M66: the values | the devices
        if (!c.devicesOn) {
            fitLine(g, Component.translatable("gui.bettacraft.terrarium.devices_off").withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT, w);
            y += 11;
        } else {
            MutableComponent mats = c.matsUnder + c.matsWall == 0 ? Component.translatable("gui.bettacraft.terrarium.mats_none").withStyle(ChatFormatting.GRAY)
                    : Component.translatable("gui.bettacraft.terrarium.mats", c.matsUnder, c.matsWall, Math.round(c.duty * 100))
                    .withStyle(c.heatPowered ? ChatFormatting.GOLD : ChatFormatting.RED);
            fitLine(g, mats, x, y, TEXT_LIGHT, w);
            y += 11;
            MutableComponent lamps = Component.translatable("gui.bettacraft.terrarium.lamps", c.lampsLit, c.lamps).withStyle(c.lamps == 0 ? ChatFormatting.GRAY : ChatFormatting.YELLOW);
            lamps.append("   ");
            lamps.append(c.misters == 0 ? Component.translatable("gui.bettacraft.terrarium.misters_none").withStyle(ChatFormatting.GRAY)
                    : Component.translatable(c.misting > 0 ? "gui.bettacraft.terrarium.misters_on" : "gui.bettacraft.terrarium.misters", c.misters, c.mistWater, c.mistSetting)
                    .withStyle(!c.misterPowered ? ChatFormatting.RED : c.mistWater <= 0 ? ChatFormatting.GOLD : ChatFormatting.AQUA));
            fitLine(g, lamps, x, y, TEXT_LIGHT, w);
            y += 12;
        }
        separator(g, y - 2); // M66: the devices | the animals
        if (c.animals == 0) {
            fitLine(g, Component.translatable("gui.bettacraft.terrarium.no_animals").withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT, w);
        } else if (c.problems.isEmpty()) {
            fitLine(g, Component.translatable("gui.bettacraft.terrarium.animals_fine", c.animals).withStyle(ChatFormatting.GREEN), x, y, TEXT_LIGHT, w);
        } else {
            for (int i = 0; i < Math.min(3 + (ch - CONTENT_H) / 10, c.problems.size()) && y < 135 + ch - CONTENT_H; i++) {
                String[] kp = c.problems.get(i).split("[|]", 2);
                fitLine(g, Component.translatable("gui.bettacraft.terrarium.animal_problem", Component.translatable(kp[0]),
                        Component.translatable(kp.length > 1 ? kp[1] : "")).withStyle(ChatFormatting.GOLD), x, y, TEXT_LIGHT, w);
                y += 10;
            }
        }
        // M31 part b: the turtle eggs in the land (progress, nest temperature, what the temperature makes of them)
        y += c.problems.isEmpty() ? 11 : 1;
        for (int i = 0; i < Math.min(2 + (ch - CONTENT_H) / 10, c.eggs.size()) && y < 140 + ch - CONTENT_H; i++) {
            String[] e = c.eggs.get(i).split("[|]");
            if (e.length < 5) continue;
            fitLine(g, Component.translatable("gui.bettacraft.terrarium.eggs", e[1], Component.translatable(e[0]), e[2], e[3],
                    Component.translatable("gui.bettacraft.terrarium.eggs." + e[4])).withStyle(ChatFormatting.AQUA), x, y, TEXT_LIGHT, w);
            y += 10;
        }
    }

    /** A value bar on the right of a climate line (0..1), with an optional white mark (the target). */
    private void bar(GuiGraphicsExtractor g, int y, float f, int color, float mark) {
        int bx = barX();
        g.fill(bx, y + 1, bx + BAR_W, y + 7, 0xFF202020);
        g.fill(bx, y + 1, bx + Math.round(BAR_W * Mth.clamp(f, 0, 1)), y + 7, color);
        if (mark >= 0) {
            int m = bx + Math.round(BAR_W * Mth.clamp(mark, 0, 1));
            g.fill(m, y, m + 1, y + 8, 0xFFFFFFFF);
        }
    }

    /** "08:30" for minutes of the day. */
    static String clock(int minutes) {
        int m = Math.floorMod(minutes, 1440);
        return String.format(java.util.Locale.ROOT, "%02d:%02d", m / 60, m % 60);
    }

    /** dark / weak / good / strong / very_strong by the light at the surface (up to 20). */
    static String lightClass(double light) {
        return light < 9 ? "dark" : light < 12 ? "weak" : light < 14.5 ? "good" : light < 16.5 ? "strong" : "very_strong";
    }

    /** M20: "Salinity 35.0 ppt (sea water) · calcium 420 mg/L · nitrate 5 mg/L". */
    static Component salinityLine(TankView.Water w) {
        var kind = org.overengineer.bettacraft.tank.Salinity.kind(w.salinity);
        ChatFormatting col = kind == org.overengineer.bettacraft.tank.Salinity.Kind.MARINE ? ChatFormatting.AQUA : ChatFormatting.YELLOW;
        MutableComponent c = Component.translatable("gui.bettacraft.reef.salinity", String.format(java.util.Locale.ROOT, "%.1f", w.salinity),
                Component.translatable("gui.bettacraft.reef.kind." + kind.name().toLowerCase(java.util.Locale.ROOT)).withStyle(col));
        if (kind == org.overengineer.bettacraft.tank.Salinity.Kind.MARINE) {
            ChatFormatting ca = w.calcium < 300 ? ChatFormatting.RED : w.calcium < 380 ? ChatFormatting.GOLD : ChatFormatting.GREEN;
            int no3 = w.nitrateInterval > 0 ? Math.round(w.nitrate) : org.overengineer.bettacraft.tank.Salinity.nitrate(w.quality); // M24: its own value
            c.append(Component.translatable("gui.bettacraft.reef.calcium", Math.round(w.calcium)).withStyle(ca));
            if (w.dosingOn) { // M25 part c: the alkalinity next to the calcium
                c.append(Component.translatable("gui.bettacraft.reef.kh", String.format(java.util.Locale.ROOT, "%.1f", w.kh))
                        .withStyle(w.kh < 5 ? ChatFormatting.RED : w.kh < 6 ? ChatFormatting.GOLD : ChatFormatting.GREEN));
            }
            c
                    .append(Component.translatable("gui.bettacraft.reef.nitrate", no3).withStyle(no3 > 40 ? ChatFormatting.RED : no3 > 20 ? ChatFormatting.GOLD : ChatFormatting.GREEN));
        }
        return c;
    }

    /** M20: "Skimmers 1 · flow pumps 2 (flow 85 %) · live rock 1,024 · light for corals 100 %". */
    static Component reefLine(TankView.Water w) {
        boolean salt = org.overengineer.bettacraft.tank.Salinity.marine(w.salinity);
        MutableComponent c = Component.translatable("gui.bettacraft.reef.skimmers", w.skimmers)
                .withStyle(w.skimmers > 0 && !salt ? ChatFormatting.GRAY : ChatFormatting.WHITE);
        c.append(Component.translatable("gui.bettacraft.reef.pumps", w.pumps, Math.round(Math.min(3, w.flow) * 100)));
        if (w.liveRock > 0) c.append(Component.translatable("gui.bettacraft.reef.live_rock", String.format(java.util.Locale.ROOT, "%,d", w.liveRock)));
        // M48 part b: the copepods of the live rock (in % of what the rock holds), the rotifers and the phytoplankton
        if (w.copepodCap > 0) c.append(Component.translatable("gui.bettacraft.reef.copepods", Math.round(100 * Math.min(1, w.copepods / w.copepodCap))));
        if (w.rotifers > 0.05 || w.phytoplankton > 0.05) {
            c.append(Component.translatable("gui.bettacraft.reef.plankton", String.format(java.util.Locale.ROOT, "%.1f", w.rotifers),
                    String.format(java.util.Locale.ROOT, "%.1f", w.phytoplankton)));
        }
        c.append(Component.translatable("gui.bettacraft.reef.spectrum", Math.round(w.spectrum * 100)));
        return c;
    }

    /** M12: the floating plants tab. */
    private void drawPlants(GuiGraphicsExtractor g, TankView v) {
        TankView.Plants p = v.plants;
        int x = 11, y = 20;
        line(g, Component.translatable("gui.bettacraft.plants.count", p.total, p.maxPlants), x, y, TEXT_LIGHT);
        y += 11;
        int cov = Math.round(p.coverage * 100);
        int bx = barX(), bw = BAR_W;
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
                Component name = Component.translatable(org.overengineer.bettacraft.tank.plants.TankPlants.item(t).getItem().getDescriptionId());
                MutableComponent sp = Component.translatable("gui.bettacraft.plants.species", name, p.counts[t.ordinal()]).withStyle(ChatFormatting.GREEN);
                if (t == org.overengineer.bettacraft.tank.plants.FloatingPlantType.RED_ROOT_FLOATER) {
                    // red under strong light, green under weak light
                    double red = org.overengineer.bettacraft.tank.plants.FloatingPlantSim.redTarget(p.light);
                    // drawn in the colour the leaves have in this light (no room for more text)
                    sp = Component.translatable("gui.bettacraft.plants.species", name, p.counts[t.ordinal()])
                            .withStyle(red >= 0.75 ? ChatFormatting.RED : red > 0 ? ChatFormatting.GOLD : ChatFormatting.GREEN);
                }
                fitLine(g, sp, x + col * (tw() / 2), y, TEXT_LIGHT, tw() / 2 - 4);
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
        y = uptakeLine(g, v, x, y);
        if (p.dense) {
            line(g, Component.literal("⚠ ").append(Component.translatable("gui.bettacraft.plants.dense", Math.round(p.denseFrom * 100)))
                    .withStyle(ChatFormatting.GOLD), x, y, TEXT_LIGHT);
        }
        fitLine(g, Component.translatable("gui.bettacraft.plants.hint").withStyle(ChatFormatting.GRAY), x, 128 + ch - CONTENT_H, 0xA0A0A0,
                plantPageButton.getX() - leftPos - ox - 4 - x);
    }

    /**
     * M66: "Improves the water quality: +x.x % a day": what all the plants of the tank do together, the floating plants and the
     * plants of the interior (ours and those of other mods); the tooltip tells the two shares. Not drawn when the water quality is off.
     */
    private int uptakeLine(GuiGraphicsExtractor g, TankView v, int x, int y) {
        if (!v.water.qualityEnabled) return y;
        double floating = v.plants.uptakePerDay, interior = v.aquatic.uptakePerDay, sum = floating + interior;
        Component c = Component.translatable("gui.bettacraft.plants.uptake", fmt((float) (sum * 100))).withStyle(sum > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY);
        fitLine(g, c, x, y, TEXT_LIGHT, tw());
        uptakeTip = Component.translatable("gui.bettacraft.plants.uptake_tooltip", fmt((float) (floating * 100)), fmt((float) (interior * 100)));
        uptakeTipY = y;
        return y + 11;
    }

    /** M13: the aquarium plants of the interior (second page of the plants tab). */
    private void drawAquatic(GuiGraphicsExtractor g, TankView v) {
        TankView.Aquatic a = v.aquatic;
        int x = 11, y = 20;
        int right = cw + 4 - x;
        fitLine(g, Component.translatable("gui.bettacraft.aquatic.count", a.total, a.maxPlants), x, y, TEXT_LIGHT, right);
        y += 11;
        if (a.foreign > 0) {
            // M38: plants of Minecraft and other mods (they count, but do not grow)
            fitLine(g, Component.translatable("gui.bettacraft.aquatic.foreign", a.foreign, String.format(java.util.Locale.ROOT, "%,d", a.foreignMass))
                    .withStyle(ChatFormatting.DARK_AQUA), x, y, TEXT_LIGHT, right);
            y += 11;
            if (a.total == 0) {
                fitLine(g, Component.translatable("gui.bettacraft.aquatic.foreign_share", fmt(a.share * 100)), x, y, TEXT_LIGHT, right);
                y += 11;
            }
        }
        if (a.total == 0) {
            if (a.foreign == 0) {
                fitLine(g, Component.translatable(a.built ? "gui.bettacraft.aquatic.none" : "gui.bettacraft.aquatic.none_built").withStyle(ChatFormatting.GRAY),
                        x, y, TEXT_LIGHT, right);
                y += 11;
            }
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
                if (rows >= 3 + (ch - CONTENT_H) / 11 && col == 0 && kinds - shown > 2) {
                    fitLine(g, Component.translatable("gui.bettacraft.aquatic.more", kinds - shown).withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT, right);
                    y += 11;
                    break;
                }
                Component name = Component.translatable(org.overengineer.bettacraft.plants.InteriorPlants.item(s, 0).getItem().getDescriptionId());
                MutableComponent sp = Component.translatable("gui.bettacraft.plants.species", name, n);
                int color = org.overengineer.bettacraft.plants.PlantColors.color(s,
                        s.red() == org.overengineer.bettacraft.plants.PlantSpecies.Red.TIPS ? org.overengineer.bettacraft.plants.PlantPart.TOP
                                : org.overengineer.bettacraft.plants.PlantPart.LEAF, a.tints[s.ordinal()]);
                // drawn in the colour the leaves have now, lightened for the dark background
                fitLine(g, sp, x + col * (tw() / 2 + 2), y, lighten(color), tw() / 2 - 2);
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
            else if (a.land < a.total && (a.temp < 18 || a.temp > 32)) growth = Component.translatable("gui.bettacraft.aquatic.temp", fmt(a.temp)).withStyle(ChatFormatting.GOLD);
            else {
                MutableComponent m = Component.translatable("gui.bettacraft.aquatic.growing", a.growing, a.fullGrown).withStyle(ChatFormatting.GREEN);
                if (a.dark > 0) m.append(" ").append(Component.translatable("gui.bettacraft.aquatic.dark", a.dark).withStyle(ChatFormatting.GOLD));
                if (a.dry > 0) m.append(" ").append(Component.translatable("gui.bettacraft.aquatic.dry", a.dry).withStyle(ChatFormatting.GOLD));
                if (a.wilting > 0) m.append(" ").append(Component.translatable("gui.bettacraft.aquatic.wilting", a.wilting).withStyle(ChatFormatting.GOLD)); // M56
                growth = m;
            }
            fitLine(g, growth, x, y, TEXT_LIGHT, right);
            y += 11;
            y = uptakeLine(g, v, x, y);
            MutableComponent hide = Component.translatable("gui.bettacraft.aquatic.hiding", Math.round(a.hiding * 100));
            if (a.bonus) hide.append("  ").append(Component.translatable("gui.bettacraft.aquatic.bonus").withStyle(ChatFormatting.GREEN));
            fitLine(g, hide, x, y, TEXT_LIGHT, right);
            y += 11;
            if (a.overgrown && y < 118 + ch - CONTENT_H) {
                fitLine(g, Component.literal("⚠ ").append(Component.translatable("gui.bettacraft.aquatic.overgrown", Math.round(a.surfaceCover * 100)))
                        .withStyle(ChatFormatting.GOLD), x, y, TEXT_LIGHT, right);
            }
        }
        fitLine(g, Component.translatable("gui.bettacraft.aquatic.hint").withStyle(ChatFormatting.GRAY), x, 128 + ch - CONTENT_H, 0xA0A0A0,
                plantPageButton.getX() - leftPos - ox - 4 - x);
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

    /** Dev smoke test only (M22.3): the fish page of the animals tab with the first betta selected; false without one. */
    public boolean showBettaForTest() {
        TankView v = view();
        if (v == null) return false;
        for (TankView.Inhabitant i : v.inhabitants) {
            if (!i.betta()) continue;
            critterPage = false;
            selected = i.uuid();
            setTab(Tab.INHABITANTS);
            return true;
        }
        return false;
    }

    /** Dev smoke test only: the fish page of the animals tab with the first tankmate of this species selected (M19). */
    public boolean showTankmateForTest(String species) {
        TankView v = view();
        if (v == null) return false;
        for (TankView.Inhabitant i : v.inhabitants) {
            if (!i.isTankmate() || !species.equals(i.tankmate().getStringOr("species", ""))) continue;
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
        critterTakeButton.onPress(new net.minecraft.client.input.KeyEvent(0, 0, 0));
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

    // ------------------------------------------------------------------ M53: users tab

    /** y of the first row of the users list (row 0 is the owner). */
    private static final int USERS_LIST_Y = 31;

    private void initUsers(int x, int y) {
        usersWidgets.clear();
        // M62: the buttons sit under the list (at the bottom of the tab), wider with the menu
        int dw = cw - CONTENT_W, dh = ch - CONTENT_H;
        int roleW = 74 + dw / 3, removeW = 50 + dw / 3, wayW = 100 + dw - 2 * (dw / 3);
        userRoleButton = addRenderableWidget(new FitButton(x + 10, y + 88 + dh, roleW, 14, Component.empty(), b -> {
            TankView v = view();
            TankView.User u = v == null ? null : selectedUser(v);
            if (u != null) send(TankActions.USERS_ROLE, (u.role() == org.overengineer.bettacraft.tank.TankUsers.Role.CARETAKER
                    ? org.overengineer.bettacraft.tank.TankUsers.Role.VIEWER : org.overengineer.bettacraft.tank.TankUsers.Role.CARETAKER).ordinal(), "", u.id());
        }));
        userRemoveButton = addRenderableWidget(new FitButton(x + 12 + roleW, y + 88 + dh, removeW, 14, Component.translatable("gui.bettacraft.users.remove"), b -> {
            TankView v = view();
            TankView.User u = v == null ? null : selectedUser(v);
            if (u != null) send(TankActions.USERS_REMOVE, 0, "", u.id());
        }));
        myWaypointButton = addRenderableWidget(new FitButton(x + 14 + roleW + removeW, y + 88 + dh, wayW, 14, Component.empty(), b -> {
            TankView v = view();
            if (v != null) send(TankActions.WAYPOINT_OWN, v.users.myWaypoint ? 0 : 1, "", null);
        }));
        int boxW = 95 + dw / 2, newW = 64 + dw / 4;
        addUserBox = new EditBox(font, x + 11, y + 106 + dh, boxW, 12, Component.translatable("gui.bettacraft.users.name"));
        addUserBox.setMaxLength(16);
        addUserBox.setHint(Component.translatable("gui.bettacraft.users.name_hint").withStyle(ChatFormatting.DARK_GRAY));
        addRenderableWidget(addUserBox);
        newRoleButton = addRenderableWidget(new FitButton(x + 15 + boxW, y + 105 + dh, newW, 14, Component.empty(), b -> {
            newRole = newRole == org.overengineer.bettacraft.tank.TankUsers.Role.CARETAKER ? org.overengineer.bettacraft.tank.TankUsers.Role.VIEWER
                    : org.overengineer.bettacraft.tank.TankUsers.Role.CARETAKER;
            updateWidgets();
        }));
        addUserButton = addRenderableWidget(new FitButton(x + 17 + boxW + newW, y + 105 + dh, 238 + dw - 17 - boxW - newW, 14, Component.translatable("gui.bettacraft.users.add"), b -> {
            String n = addUserBox.getValue().strip();
            if (n.isEmpty()) return;
            send(TankActions.USERS_ADD, newRole.ordinal(), n, null);
            addUserBox.setValue("");
        }));
        allWaypointsButton = addRenderableWidget(new FitButton(x + 10, y + 123 + dh, 112 + dw / 2, 14, Component.empty(), b -> {
            TankView v = view();
            if (v != null) send(TankActions.WAYPOINT_ALL, v.users.waypointsAll ? 0 : 1, "", null);
        }));
        othersButton = addRenderableWidget(new FitButton(x + 124 + dw / 2, y + 123 + dh, 114 + dw - dw / 2, 14, Component.empty(), b -> send(TankActions.TOGGLE_LOCK, 0, "", null)));
        usersWidgets.addAll(List.of(userRoleButton, userRemoveButton, myWaypointButton, addUserBox, newRoleButton, addUserButton, allWaypointsButton, othersButton));
    }

    @Nullable
    private TankView.User selectedUser(TankView v) {
        for (TankView.User u : v.users.list) if (u.id().equals(selectedUser)) return u;
        return null;
    }

    private void updateUsers(@Nullable TankView v) {
        boolean shown = tab == Tab.USERS;
        for (AbstractWidget w : usersWidgets) w.visible = shown;
        if (v == null) {
            for (AbstractWidget w : usersWidgets) w.active = false;
            return;
        }
        TankView.Users u = v.users;
        boolean manage = u.canManage, list = u.feature;
        if (selectedUser != null && selectedUser(v) == null) selectedUser = null;
        TankView.User sel = selectedUser(v);
        boolean self = sel != null && minecraft != null && minecraft.player != null && sel.id().equals(minecraft.player.getUUID());
        userRoleButton.visible = shown && list;
        userRemoveButton.visible = shown && list;
        addUserBox.visible = shown && list;
        newRoleButton.visible = shown && list;
        addUserButton.visible = shown && list;
        userRoleButton.active = manage && sel != null;
        userRoleButton.setMessage(sel == null ? Component.translatable("gui.bettacraft.users.role_button_none")
                : Component.translatable("gui.bettacraft.users.role_button", Component.translatable(sel.role().translationKey())));
        userRoleButton.setTooltip(Tooltip.create(Component.translatable(manage ? "gui.bettacraft.users.role_tooltip" : "message.bettacraft.tank.only_owner")));
        userRemoveButton.active = sel != null && (manage || self);
        userRemoveButton.setTooltip(Tooltip.create(Component.translatable(self && !manage ? "gui.bettacraft.users.leave_tooltip" : "gui.bettacraft.users.remove_tooltip")));
        boolean isUser = u.myRole != org.overengineer.bettacraft.tank.TankUsers.Role.OTHER;
        myWaypointButton.active = u.waypointFeature && isUser;
        myWaypointButton.setMessage(Component.translatable(u.myWaypoint ? "gui.bettacraft.users.my_waypoint_on" : "gui.bettacraft.users.my_waypoint_off")
                .withStyle(u.myWaypoint && u.waypointsAll ? ChatFormatting.GREEN : ChatFormatting.WHITE));
        myWaypointButton.setTooltip(Tooltip.create(Component.translatable(!u.waypointFeature ? "message.bettacraft.users.waypoints_off"
                : isUser ? "gui.bettacraft.users.my_waypoint_tooltip" : "message.bettacraft.users.not_user")));
        addUserBox.active = manage;
        addUserBox.setEditable(manage);
        newRoleButton.active = manage;
        newRoleButton.setMessage(Component.translatable("gui.bettacraft.users.new_role", Component.translatable(newRole.translationKey())));
        newRoleButton.setTooltip(Tooltip.create(Component.translatable("gui.bettacraft.users.new_role_tooltip")));
        addUserButton.active = manage && !addUserBox.getValue().isBlank();
        addUserButton.setTooltip(Tooltip.create(Component.translatable(manage ? "gui.bettacraft.users.add_tooltip" : "message.bettacraft.tank.only_owner")));
        allWaypointsButton.active = manage && u.waypointFeature;
        allWaypointsButton.setMessage(Component.translatable(u.waypointsAll ? "gui.bettacraft.users.all_waypoints_on" : "gui.bettacraft.users.all_waypoints_off")
                .withStyle(u.waypointsAll ? ChatFormatting.WHITE : ChatFormatting.GRAY));
        allWaypointsButton.setTooltip(Tooltip.create(Component.translatable(!u.waypointFeature ? "message.bettacraft.users.waypoints_off"
                : manage ? "gui.bettacraft.users.all_waypoints_tooltip" : "message.bettacraft.tank.only_owner")));
        othersButton.active = manage;
        othersButton.setMessage(Component.translatable(v.locked ? "gui.bettacraft.users.others_view" : "gui.bettacraft.users.others_change")
                .withStyle(v.locked ? ChatFormatting.GOLD : ChatFormatting.WHITE));
        othersButton.setTooltip(Tooltip.create(Component.translatable("gui.bettacraft.users.others_tooltip")));
    }

    private void drawUsers(GuiGraphicsExtractor g, TankView v, int mx, int my) {
        TankView.Users u = v.users;
        int x = 11, y = 20;
        Component me = Component.translatable(u.myRole.translationKey()).withStyle(ChatFormatting.AQUA);
        line(g, Component.translatable("gui.bettacraft.users.header", me), x, y, TEXT_LIGHT);
        // the rows: the owner first, then the listed users
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{v.ownerName.isEmpty() ? Component.translatable("gui.bettacraft.tank.no_owner").getString() : v.ownerName,
                org.overengineer.bettacraft.tank.TankUsers.Role.OWNER, u.ownerWaypoint, u.ownerOnline, null});
        if (u.feature) {
            for (TankView.User e : u.list) {
                rows.add(new Object[]{e.name().isEmpty() ? e.id().toString().substring(0, 8) : e.name(), e.role(), e.waypoint(), e.online(), e.id()});
            }
        }
        int ly = USERS_LIST_Y, dw = cw - CONTENT_W;
        // M62: the columns share the extra width: the name half of it, the role and the waypoint a quarter each
        int nameW = 98 + dw / 2, roleX = 120 + dw / 2, roleW = 62 + dw / 4, wayX = 186 + dw / 2 + dw / 4, wayW = 50 + dw - dw / 2 - dw / 4;
        g.fill(9, ly - 1, cw + 5, ly + userRows() * ROW_H, 0x30000000);
        for (int i = 0; i < userRows() && userScroll + i < rows.size(); i++) {
            Object[] r = rows.get(userScroll + i);
            int ry = ly + i * ROW_H;
            boolean sel = r[4] != null && r[4].equals(selectedUser);
            if (sel) g.fill(9, ry - 1, cw + 5, ry + ROW_H - 1, 0x60FFFFFF);
            var role = (org.overengineer.bettacraft.tank.TankUsers.Role) r[1];
            int color = role == org.overengineer.bettacraft.tank.TankUsers.Role.OWNER ? 0xFFD060
                    : role == org.overengineer.bettacraft.tank.TankUsers.Role.CARETAKER ? 0x80E080 : 0xA0C8FF;
            g.fill(12, ry + 3, 15, ry + 6, (Boolean) r[3] ? 0xFF40D040 : 0xFF707070); // online dot
            fitLine(g, Component.literal((String) r[0]), 18, ry, color, nameW);
            fitLine(g, Component.translatable(role.translationKey()), roleX, ry, TEXT_LIGHT, roleW);
            if (u.waypointFeature) {
                boolean on = (Boolean) r[2] && u.waypointsAll;
                fitLine(g, Component.translatable(on ? "gui.bettacraft.users.waypoint_on" : "gui.bettacraft.users.waypoint_off"), wayX, ry, on ? 0x80E080 : 0x909090, wayW);
            }
        }
        if (!u.feature) {
            line(g, Component.translatable("gui.bettacraft.users.feature_off").withStyle(ChatFormatting.GRAY), x, ly + 2 * ROW_H, 0xA0A0A0);
        } else if (u.list.isEmpty()) {
            line(g, Component.translatable("gui.bettacraft.users.empty").withStyle(ChatFormatting.GRAY), x, ly + ROW_H, 0xA0A0A0);
        }
        // hover over the role column: what the roles may do
        if (mx >= roleX - 2 && mx < roleX + roleW + 2 && my >= ly && my < ly + userRows() * ROW_H) {
            pendingTooltip = List.of(Component.translatable("gui.bettacraft.users.roles_title").withStyle(ChatFormatting.YELLOW),
                    Component.translatable("gui.bettacraft.users.role.owner.desc"), Component.translatable("gui.bettacraft.users.role.caretaker.desc"),
                    Component.translatable("gui.bettacraft.users.role.viewer.desc"), Component.translatable("gui.bettacraft.users.role.other.desc"));
        }
    }

    /** M53: what the old interior tab showed, as the tooltip of the interior button. */
    private static Component interiorTooltip(TankView v) {
        MutableComponent t = Component.translatable("gui.bettacraft.interior.size", v.interiorX, v.interiorY, v.interiorZ, v.scale, 16 / Math.max(1, v.scale));
        if (!v.interiorBuilt) {
            t.append("\n").append(Component.translatable("gui.bettacraft.interior.not_built").withStyle(ChatFormatting.GRAY));
        } else {
            Component blocks = v.interiorBlocks < 0 ? Component.translatable("gui.bettacraft.interior.blocks_unknown")
                    : Component.translatable("gui.bettacraft.interior.blocks", String.format(java.util.Locale.ROOT, "%,d", v.interiorBlocks),
                    String.format(java.util.Locale.ROOT, "%,d", v.renderLimit));
            t.append("\n").append(blocks.copy().withStyle(v.interiorTruncated ? ChatFormatting.RED : ChatFormatting.RESET));
            if (v.interiorTruncated) t.append("\n").append(Component.translatable("gui.bettacraft.interior.limit").withStyle(ChatFormatting.RED));
            t.append("\n").append(Component.translatable("gui.bettacraft.interior.substrate", v.substrate));
            MutableComponent heat = Component.translatable("gui.bettacraft.interior.heaters", v.heatersInside, v.heaterCapWet);
            if (v.heaterCapDry > 0) heat.append(" ").append(Component.translatable("gui.bettacraft.interior.heaters_dry", v.heaterCapDry).withStyle(ChatFormatting.GOLD));
            if (v.coolersInside > 0) heat.append("  ").append(Component.translatable("gui.bettacraft.interior.coolers", v.coolersInside, v.coolerCapWet));
            t.append("\n").append(heat);
            t.append("\n").append(Component.translatable("gui.bettacraft.interior.filters", v.filters, v.floating));
        }
        if (!v.canModify) {
            t.append("\n").append(Component.translatable(v.users.myRole == org.overengineer.bettacraft.tank.TankUsers.Role.VIEWER
                    ? "message.bettacraft.users.viewer" : "message.bettacraft.tank.locked").withStyle(ChatFormatting.RED));
        }
        t.append("\n").append(Component.translatable("gui.bettacraft.interior.hint_substrate").withStyle(ChatFormatting.GRAY));
        return t;
    }

    /** M53: the gear of the overview: Bettacraft's client config (back to this menu when it closes). */
    private void openClientConfig() {
        net.minecraft.client.gui.screens.Screen s = org.overengineer.bettacraft.platform.client.ClientPlatform.get().clientConfigScreen(this);
        if (s == null) org.slf4j.LoggerFactory.getLogger(TankScreen.class).warn("No client config screen of Bettacraft found");
        if (s != null && minecraft != null) minecraft.gui.setScreen(s);
    }

    /** Dev smoke test only (M53): opens the client config like the gear button (false when there is none). */
    public boolean openClientConfigForTest() {
        if (!configButton.visible || !configButton.active) {
            org.slf4j.LoggerFactory.getLogger(TankScreen.class).warn("gear: visible {} active {}", configButton.visible, configButton.active);
            return false;
        }
        configButton.onPress(new net.minecraft.client.input.KeyEvent(0, 0, 0));
        return true;
    }

    /** Dev smoke test only (M53): selects a listed user of the users tab. */
    public void selectUserForTest(UUID id) {
        selectedUser = id;
        updateWidgets();
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

    private void drawInhabitants(GuiGraphicsExtractor g, TankView v, int mx, int my) {
        int lx = 10, ly = 20;
        int rows = listRows();
        g.fill(lx - 1, ly - 1, lx + listW, ly + rows * ROW_H, 0x60000000);
        if (v.inhabitants.isEmpty()) {
            fitLine(g, Component.translatable("gui.bettacraft.fish.empty"), lx + 2, ly + 2, 0xA0A0A0, listW - 4);
        }
        listScroll = Mth.clamp(listScroll, 0, Math.max(0, v.inhabitants.size() - rows));
        for (int r = 0; r < rows; r++) {
            int idx = listScroll + r;
            if (idx >= v.inhabitants.size()) break;
            TankView.Inhabitant i = v.inhabitants.get(idx);
            int y = ly + r * ROW_H;
            boolean hover = mx >= lx && mx < lx + listW && my >= y && my < y + ROW_H;
            if (i.uuid().equals(selected)) g.fill(lx, y, lx + listW - 1, y + ROW_H, 0x80FFFFFF);
            else if (hover) g.fill(lx, y, lx + listW - 1, y + ROW_H, 0x30FFFFFF);
            String sym = i.betta() || i.isTankmate() || i.sexed() ? (i.female() ? "♀ " : "♂ ") : ""; // M39: foreign animals with a sex of ours
            Component name = Component.literal(sym).append(inhabitantName(i));
            // M62: a name that does not fit ends in "...", the whole name shows when the mouse is on it
            int room = listW - 8;
            String full = name.getString(), s = full;
            if (font.width(full) > room) s = font.plainSubstrByWidth(full, room - font.width("...")) + "...";
            // M62: red: sick; orange: a shoaling fish whose shoal is too small
            boolean smallShoal = i.isTankmate() && (i.tankmate().getIntOr("stress", 0) & org.overengineer.bettacraft.entity.tankmate.TankmateCare.STRESS_SHOAL) != 0;
            int color = i.sick() ? 0xFF8080 : smallShoal ? 0xFFC060 : 0xFFFFFF;
            g.text(font, s, lx + 2, y + 2, org.overengineer.bettacraft.client.ClientCompat.textColor(color), false);
            if (hover && !s.equals(full)) pendingTooltip = List.of(Component.literal(full));
        }
        listBar(g, lx + listW - 3, ly, rows * ROW_H, v.inhabitants.size(), rows, listScroll);
        TankView.Inhabitant sel = selectedInhabitant(v);
        if (sel == null) return;
        int x = detailX(), y = 19;
        Component header = sel.customName().isEmpty() ? Component.translatable(sel.typeKey()) : Component.literal(sel.customName());
        g.text(font, font.plainSubstrByWidth(header.getString(), detailW()), x, y, org.overengineer.bettacraft.client.ClientCompat.textColor(0xFFFFFF), false);
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
                fitLine(g, Component.translatable("gui.bettacraft.frog.no_air").withStyle(ChatFormatting.RED), x, y, TEXT_LIGHT, detailW());
                y += 10;
            }
            fitLine(g, Component.translatable("gui.bettacraft.frog.hint").withStyle(ChatFormatting.GRAY), x, y, 0xA0A0A0, detailW());
            y += 9;
            fitLine(g, Component.translatable("gui.bettacraft.frog.hint2").withStyle(ChatFormatting.GRAY), x, y, 0xA0A0A0, detailW());
            return;
        }
        if (sel.isTankmate()) {
            drawTankmate(g, sel, x, y, mx, my);
            return;
        }
        if (!sel.betta()) {
            line(g, Component.translatable(sel.typeKey()).withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT);
            y += 10;
            if (sel.sexed()) { // M39
                line(g, BettaTooltip.sex(sel.female()), x, y, TEXT_LIGHT);
                y += 10;
            }
            line(g, Component.translatable("gui.bettacraft.fish.foreign"), x, y, 0xA0A0A0);
            return;
        }
        MutableComponent stage = Component.translatable("stage.bettacraft." + sel.stage());
        if (!sel.adult()) stage = Component.translatable("gui.bettacraft.fish.stage_progress", stage, Math.round(sel.progress() * 100));
        Component type = BettaTooltip.type(sel.genome());
        String ts = font.plainSubstrByWidth(type.getString(), detailW());
        g.text(font, ts, x, y, org.overengineer.bettacraft.client.ClientCompat.textColor(0xFFD27A), false);
        if (mx >= x && mx < x + detailW() && my >= y && my < y + 9 && !ts.equals(type.getString())) pendingTooltip = List.of(type);
        y += 9;
        line(g, Component.translatable("gui.bettacraft.fish.sex_stage", BettaTooltip.sex(sel.female()), stage), x, y, TEXT_LIGHT);
        if (mx >= x && mx < x + detailW() && my >= y && my < y + 9) {
            // M18: how much this betta counts for the stocking (fry 0.02, juvenile 0.1, subadult 0.3 by default)
            pendingTooltip = List.of(Component.translatable("gui.bettacraft.fish.counts", fmt2(org.overengineer.bettacraft.entity.betta.LifeStage.byId(sel.stage()).stocking())));
        }
        y += 9;
        float hp = Math.min(sel.health(), sel.hp());
        bar(g, x, y, Component.translatable(sel.sick() ? "gui.bettacraft.fish.sick" : "gui.bettacraft.fish.health"), hp,
                hp > 0.5F ? 0xFF50C050 : 0xFFD04040);
        y += 9;
        bar(g, x, y, Component.translatable("gui.bettacraft.fish.satiation"), sel.satiation(), 0xFFD0A040);
        dietTooltip(sel.tankmate().getCompoundOrEmpty("diet"), x, y, mx, my); // M26 part b
        y += 10;
        var gt = sel.genotype();
        int top = y;
        if (!sel.genesVisible()) {
            // M22.3: the hidden genes need the magnifier
            fitLine(g, Component.translatable("gui.bettacraft.fish.genes_unknown").withStyle(ChatFormatting.GRAY), x, y, 0xC8C8C8, detailW());
            y += 9;
            fitLine(g, Component.translatable("gui.bettacraft.fish.genes_unknown_hint").withStyle(ChatFormatting.DARK_GRAY), x, y, 0xA0A0A0, detailW());
            return;
        }
        for (Locus l : List.of(BettaLoci.FINFORM, BettaLoci.BODY_COLOR, BettaLoci.FIN_COLOR, BettaLoci.PATTERN)) {
            Component c = GenotypeText.line(gt, l);
            String s = font.plainSubstrByWidth(c.getString(), detailW());
            g.text(font, s, x, y, org.overengineer.bettacraft.client.ClientCompat.textColor(0xC8C8C8), false);
            y += 9;
        }
        if (mx >= x && mx < x + detailW() && my >= top && my < y) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("gui.bettacraft.fish.genotype").withStyle(ChatFormatting.YELLOW));
            for (Locus l : BettaLoci.ALL) tip.add(GenotypeText.line(gt, l));
            tip.add(Component.translatable("gui.bettacraft.fish.generation", sel.generation()).withStyle(ChatFormatting.GRAY));
            tip.add(Component.translatable("gui.bettacraft.fish.genotype_hint").withStyle(ChatFormatting.DARK_GRAY));
            pendingTooltip = tip;
        }
    }

    /** M19: a tankmate: species or variety, sex and stage, health, food, its shoal, temperature range, stress and breeding state. */
    /** M26 part b: hovering the food bar shows how varied the diet was lately. */
    private void dietTooltip(net.minecraft.nbt.CompoundTag diet, int x, int y, int mx, int my) {
        if (diet.isEmpty() || mx < x || mx >= x + detailW() || my < y || my >= y + 9) return;
        List<Component> l = new java.util.ArrayList<>();
        l.add(Component.translatable("gui.bettacraft.fish.diet", diet.getIntOr("n", 0)));
        if (diet.getBooleanOr("varied", false)) l.add(Component.translatable("gui.bettacraft.fish.diet_varied").withStyle(ChatFormatting.GREEN));
        else if (diet.getIntOr("need", 0) > 0) l.add(Component.translatable("gui.bettacraft.fish.diet_need", diet.getIntOr("need", 0)).withStyle(ChatFormatting.GRAY));
        pendingTooltip = l;
    }

    private void drawTankmate(GuiGraphicsExtractor g, TankView.Inhabitant sel, int x, int y, int mx, int my) {
        CompoundTag d = sel.tankmate();
        var s = org.overengineer.bettacraft.tankmates.TankmateSpecies.byId(d.getStringOr("species", ""));
        if (s == null) return;
        Component sub = s.varieties().size() > 1 ? Component.translatable(s.varietyKey(d.getStringOr("variety", ""))).withStyle(ChatFormatting.GOLD)
                : Component.literal(s.latin()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        // M21.6: guppies and endlers with their genes: the strain, a tooltip with every locus
        org.overengineer.bettacraft.genetics.livebearer.LivebearerGenome lb = NbtCompat.has(d, "lb", net.minecraft.nbt.Tag.TAG_COMPOUND)
                ? org.overengineer.bettacraft.genetics.livebearer.LivebearerGenome.load(d.getCompoundOrEmpty("lb"), s == org.overengineer.bettacraft.tankmates.TankmateSpecies.ENDLER_GUPPY)
                : null;
        if (lb != null) sub = org.overengineer.bettacraft.genetics.livebearer.LivebearerText.name(lb.express(sel.female())).withStyle(ChatFormatting.GOLD);
        fitLine(g, sub, x, y, 0xFFD27A, detailW());
        if (geneTooltipForTest || mx >= x && mx < x + detailW() && my >= y && my < y + 9) {
            if (lb != null) {
                var ph = lb.express(sel.female());
                List<Component> tip = new ArrayList<>();
                tip.add(Component.literal(s.latin()).withStyle(ChatFormatting.ITALIC));
                tip.add(org.overengineer.bettacraft.genetics.livebearer.LivebearerText.name(ph).withStyle(ChatFormatting.GOLD));
                tip.add(Component.empty().append(org.overengineer.bettacraft.genetics.livebearer.LivebearerText.summary(ph)).withStyle(ChatFormatting.YELLOW));
                if (d.getBooleanOr("lb_hidden", false)) tip.add(Component.translatable("gui.bettacraft.fish.genes_unknown").withStyle(ChatFormatting.GRAY)); // M22.3
                else tip.addAll(org.overengineer.bettacraft.genetics.livebearer.LivebearerText.lines(lb, sel.female()));
                tip.add(Component.translatable("gui.bettacraft.fish.generation", sel.generation()).withStyle(ChatFormatting.GRAY));
                tip.add(Component.translatable(sel.female() ? "gui.bettacraft.lb.female_hint" : "gui.bettacraft.lb.male_hint").withStyle(ChatFormatting.DARK_GRAY));
                pendingTooltip = tip;
            } else {
                pendingTooltip = List.of(Component.literal(s.latin()).withStyle(ChatFormatting.ITALIC),
                        d.getStringOr("genes", "").isEmpty() ? Component.translatable("gui.bettacraft.fish.genes_unknown").withStyle(ChatFormatting.GRAY) // M22.3
                                : Component.translatable("gui.bettacraft.tm.genes", d.getStringOr("genes", "")).withStyle(ChatFormatting.GRAY),
                        Component.translatable("gui.bettacraft.fish.generation", sel.generation()).withStyle(ChatFormatting.GRAY));
            }
        }
        y += 9;
        MutableComponent stage = Component.translatable("stage.bettacraft." + sel.stage());
        if (!sel.adult()) stage = Component.translatable("gui.bettacraft.fish.stage_progress", stage, Math.round(sel.progress() * 100));
        line(g, Component.translatable("gui.bettacraft.fish.sex_stage", BettaTooltip.sex(sel.female()), stage), x, y, TEXT_LIGHT);
        if (mx >= x && mx < x + detailW() && my >= y && my < y + 9) {
            pendingTooltip = List.of(Component.translatable("gui.bettacraft.fish.counts", fmt2(d.getFloatOr("stock", 0f))));
        }
        y += 9;
        float hp = Math.min(sel.health(), sel.hp());
        bar(g, x, y, Component.translatable(sel.sick() ? "gui.bettacraft.fish.sick" : "gui.bettacraft.fish.health"), hp, hp > 0.5F ? 0xFF50C050 : 0xFFD04040);
        y += 9;
        bar(g, x, y, Component.translatable("gui.bettacraft.fish.satiation"), sel.satiation(), 0xFFD0A040);
        dietTooltip(d.getCompoundOrEmpty("diet"), x, y, mx, my); // M26 part b
        y += 11;
        int stress = d.getIntOr("stress", 0);
        if (s.shoaling()) {
            boolean small = (stress & org.overengineer.bettacraft.entity.tankmate.TankmateCare.STRESS_SHOAL) != 0;
            fitLine(g, Component.translatable(small ? "gui.bettacraft.tm.shoal_small" : "gui.bettacraft.tm.shoal", d.getIntOr("shoal", 0), s.shoal())
                    .withStyle(small ? ChatFormatting.RED : ChatFormatting.GREEN), x, y, TEXT_LIGHT, detailW());
        } else {
            fitLine(g, Component.translatable("gui.bettacraft.tm.solitary").withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT, detailW());
        }
        y += 9;
        boolean temp = (stress & org.overengineer.bettacraft.entity.tankmate.TankmateCare.STRESS_TEMP) == 0;
        // M20: the reef fish with their salinity
        Component tl = s.marine() || !s.salinity().contains(0) ? Component.translatable("gui.bettacraft.tm.temp_salt", f1(s.tempMin()), f1(s.tempMax()),
                Math.round(s.salinity().min()), Math.round(s.salinity().max())) : Component.translatable("gui.bettacraft.tm.temp", f1(s.tempMin()), f1(s.tempMax()));
        fitLine(g, tl.copy().withStyle(temp ? ChatFormatting.GREEN : ChatFormatting.RED), x, y, TEXT_LIGHT, detailW());
        y += 9;
        List<Component> notes = new ArrayList<>();
        if ((stress & org.overengineer.bettacraft.entity.tankmate.TankmateCare.STRESS_WATER) != 0)
            notes.add(Component.translatable("gui.bettacraft.tm.water_bad").withStyle(ChatFormatting.RED));
        if ((stress & org.overengineer.bettacraft.entity.tankmate.TankmateCare.STRESS_CHASED) != 0)
            notes.add(Component.translatable("gui.bettacraft.tm.chased").withStyle(ChatFormatting.RED));
        if ((stress & org.overengineer.bettacraft.entity.tankmate.TankmateCare.STRESS_CROWDED) != 0)
            notes.add(Component.translatable("gui.bettacraft.tm.crowded").withStyle(ChatFormatting.RED));
        // M21.8 seahorses
        if ((stress & org.overengineer.bettacraft.entity.tankmate.TankmateCare.STRESS_FLOW) != 0)
            notes.add(Component.translatable("gui.bettacraft.tm.flow").withStyle(ChatFormatting.RED));
        if ((stress & org.overengineer.bettacraft.entity.tankmate.TankmateCare.STRESS_NO_POSTS) != 0)
            notes.add(Component.translatable("gui.bettacraft.tm.no_posts").withStyle(ChatFormatting.RED));
        if ((stress & org.overengineer.bettacraft.entity.tankmate.TankmateCare.STRESS_OXYGEN) != 0)
            notes.add(Component.translatable("gui.bettacraft.tm.oxygen").withStyle(ChatFormatting.RED)); // M23
        if ((stress & org.overengineer.bettacraft.entity.tankmate.TankmateCare.STRESS_NITRATE) != 0)
            notes.add(Component.translatable("gui.bettacraft.tm.nitrate").withStyle(ChatFormatting.GOLD)); // M24
        if ((stress & org.overengineer.bettacraft.entity.tankmate.TankmateCare.STRESS_CHEMISTRY) != 0)
            notes.add(Component.translatable("gui.bettacraft.tm.chemistry").withStyle(ChatFormatting.GOLD)); // M25
        if ((stress & org.overengineer.bettacraft.entity.tankmate.TankmateCare.STRESS_TEETH) != 0)
            notes.add(Component.translatable("gui.bettacraft.tm.teeth").withStyle(ChatFormatting.RED)); // M29 part b
        if ((stress & org.overengineer.bettacraft.entity.tankmate.TankmateCare.STRESS_NIPPED) != 0)
            notes.add(Component.translatable("gui.bettacraft.tm.nipped").withStyle(ChatFormatting.RED));
        if (d.getBooleanOr("hitched", false)) notes.add(Component.translatable("gui.bettacraft.tm.hitched").withStyle(ChatFormatting.AQUA));
        if (d.getFloatOr("gravid", 0f) >= 0) notes.add(Component.translatable(s.seahorse() ? "gui.bettacraft.tm.pouch" : "gui.bettacraft.tm.gravid",
                Math.round(d.getFloatOr("gravid", 0f) * 100)).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (s.seahorse()) notes.add(Component.translatable("gui.bettacraft.tm.live_food").withStyle(ChatFormatting.GRAY));

        if (d.getBooleanOr("guard", false)) notes.add(Component.translatable("gui.bettacraft.tm.guarding").withStyle(ChatFormatting.AQUA));
        else if (d.getBooleanOr("cave", false)) notes.add(Component.translatable(s.breeding() == org.overengineer.bettacraft.tankmates.TankmateSpecies.Breeding.ANEMONE
                ? "gui.bettacraft.tm.in_anemone" : "gui.bettacraft.tm.has_cave").withStyle(ChatFormatting.AQUA));
        notes.add(Component.translatable(s.breedingKey()).withStyle(ChatFormatting.GRAY));
        for (Component c : notes) {
            if (y > 128 + ch - CONTENT_H) break;
            fitLine(g, c, x, y, TEXT_LIGHT, detailW());
            y += 9;
        }
    }

    private static String f1(double d) {
        return String.format(java.util.Locale.ROOT, "%.1f", d);
    }

    /** M22.3: a row of the list of small animals: a kind, or (under the selected kind) one animal of it. */
    private record CritterRow(TankView.CritterGroup group, @Nullable TankView.CritterInfo animal) {}

    private List<CritterRow> critterRows(TankView v) {
        List<CritterRow> rows = new ArrayList<>();
        boolean list = Features.enabled(Feature.ANIMAL_LIST);
        for (TankView.CritterGroup gr : v.critters.groups) {
            rows.add(new CritterRow(gr, null));
            if (list && critterExpanded && gr.key().equals(selectedGroup)) {
                for (TankView.CritterInfo k : v.critters.animals) if (k.key().equals(gr.key())) rows.add(new CritterRow(gr, k));
            }
        }
        return rows;
    }

    @Nullable
    private TankView.CritterInfo selectedCritterInfo(TankView v) {
        if (selectedCritter == null) return null;
        for (TankView.CritterInfo k : v.critters.animals) if (k.id() == selectedCritter) return k;
        return null;
    }

    /** Dev smoke test only (M22.3): selects the kind and its first animal in the list of small animals. */
    public void selectFirstCritterForTest() {
        TankView v = view();
        if (v == null || v.critters.groups.isEmpty()) return;
        selectedGroup = v.critters.groups.getFirst().key();
        critterExpanded = true;
        for (TankView.CritterInfo k : v.critters.animals) {
            if (k.key().equals(selectedGroup)) {
                selectedCritter = k.id();
                break;
            }
        }
        updateWidgets();
    }

    /** M14: shrimp, crayfish and snails (second page of the animals tab): the kinds on the left, details and the tank on the right. */
    private void drawCritters(GuiGraphicsExtractor g, TankView v, int mx, int my) {
        TankView.Critters c = v.critters;
        int lx = 10, ly = 20;
        g.fill(lx - 1, ly - 1, lx + listW, ly + critterRowCount() * ROW_H, 0x60000000);
        if (c.groups.isEmpty()) {
            fitLine(g, Component.translatable("gui.bettacraft.critters.empty"), lx + 2, ly + 2, 0xA0A0A0, listW - 4);
            fitLine(g, Component.translatable("gui.bettacraft.critters.empty_hint").withStyle(ChatFormatting.GRAY), lx + 2, ly + 13, 0xA0A0A0, listW - 4);
        }
        List<CritterRow> rows = critterRows(v);
        boolean list = Features.enabled(Feature.ANIMAL_LIST);
        for (int r = 0; r < critterRowCount(); r++) {
            int idx = groupScroll + r;
            if (idx >= rows.size()) break;
            CritterRow row = rows.get(idx);
            TankView.CritterGroup gr = row.group();
            int y = ly + r * ROW_H;
            if (row.animal() != null) {
                // M22.3: one animal: sex, young, grade, genes (or "?")
                TankView.CritterInfo k = row.animal();
                if (selectedCritter != null && selectedCritter == k.id()) g.fill(lx, y, lx + listW - 1, y + ROW_H, 0x80FFFFFF);
                else if (mx >= lx && mx < lx + listW && my >= y && my < y + ROW_H) g.fill(lx, y, lx + listW - 1, y + ROW_H, 0x30FFFFFF);
                g.fill(lx + 6, y + 3, lx + 10, y + 7, 0xFF000000 | lighten(k.rgb()));
                var sp = org.overengineer.bettacraft.critters.CritterSpecies.values()[k.species()];
                MutableComponent t = Component.literal(!sp.hasSexes() ? "⚥" : k.female() ? "♀" : "♂");
                if (!k.adult()) t.append(" ").append(Component.translatable("gui.bettacraft.critters.young_short"));
                if (sp.hasGrade()) t.append(" " + Math.round(k.grade() * 100) + "%");
                if (sp.loci().length > 0) t.append(" ").append(k.genes().isEmpty() ? Component.literal("?").withStyle(ChatFormatting.GRAY)
                        : Component.literal(k.genes()).withStyle(ChatFormatting.AQUA));
                if (k.berried()) t.append(" ").append(Component.literal("•").withStyle(ChatFormatting.LIGHT_PURPLE));
                fitLine(g, t, lx + 12, y + 2, 0xE0E0E0, listW - 14);
                continue;
            }
            if (gr.key().equals(selectedGroup) && selectedCritter == null) g.fill(lx, y, lx + listW - 1, y + ROW_H, 0x80FFFFFF);
            else if (gr.key().equals(selectedGroup)) g.fill(lx, y, lx + listW - 1, y + ROW_H, 0x40FFFFFF);
            else if (mx >= lx && mx < lx + listW && my >= y && my < y + ROW_H) g.fill(lx, y, lx + listW - 1, y + ROW_H, 0x30FFFFFF);
            g.fill(lx + 2, y + 2, lx + 8, y + 8, 0xFF000000 | lighten(gr.rgb()));
            // in the list the colour species show only their variety (the header names the species)
            var sp = org.overengineer.bettacraft.critters.CritterSpecies.values()[gr.species()];
            MutableComponent name = sp.loci().length == 0 ? Component.translatable(sp.translationKey())
                    : "wild".equals(gr.variety()) ? org.overengineer.bettacraft.critters.TankCritters.name(sp, gr.variety())
                    : Component.translatable("variety.bettacraft." + sp.id() + "." + gr.variety());
            String count = " " + (gr.adults() + gr.young());
            int room = listW - 15 - font.width(count);
            clipLine(g, name, lx + 10, y + 2, 0xFFFFFF, room, mx, my);
            g.text(font, count, lx + listW - 5 - font.width(count), y + 2, org.overengineer.bettacraft.client.ClientCompat.textColor(0xC8C8C8), false);
        }
        groupScroll = Mth.clamp(groupScroll, 0, Math.max(0, rows.size() - critterRowCount()));
        listBar(g, lx + listW - 3, ly, critterRowCount() * ROW_H, rows.size(), critterRowCount(), groupScroll);
        int x = detailX(), y = 19, w = detailW();
        TankView.CritterGroup sel = null;
        for (TankView.CritterGroup gr : c.groups) if (gr.key().equals(selectedGroup)) sel = gr;
        TankView.CritterInfo one = selectedCritterInfo(v);
        if (one != null) {
            // M22.3: one animal: sex and age, colour density, look, genes, eggs
            var sp = org.overengineer.bettacraft.critters.CritterSpecies.values()[one.species()];
            fitLine(g, org.overengineer.bettacraft.critters.TankCritters.name(sp, one.variety()).withStyle(ChatFormatting.YELLOW), x, y, 0xFFFFFF, w);
            y += 10;
            Component sex = !sp.hasSexes() ? Component.translatable("gui.bettacraft.critters.hermaphrodite_one") : BettaTooltip.sex(one.female());
            fitLine(g, Component.translatable("gui.bettacraft.critters.one_age", sex, Component.translatable(one.adult() ? "gui.bettacraft.critters.adult"
                    : "gui.bettacraft.critters.young_one"), f1(one.ageDays())), x, y, TEXT_LIGHT, w);
            y += 10;
            if (sp.hasGrade()) {
                fitLine(g, Component.translatable("gui.bettacraft.critters.one_grade", Math.round(one.grade() * 100)), x, y, TEXT_LIGHT, w);
                y += 10;
            }
            if (!one.lookKey().isEmpty()) {
                fitLine(g, Component.translatable(one.lookKey()).withStyle(ChatFormatting.GOLD), x, y, TEXT_LIGHT, w);
                y += 10;
            }
            if (sp.loci().length > 0) {
                Component genes = one.genes().isEmpty() ? Component.translatable("gui.bettacraft.critters.genes_unknown").withStyle(ChatFormatting.GRAY)
                        : Component.translatable("gui.bettacraft.critters.genes", one.genes()).withStyle(ChatFormatting.AQUA);
                fitLine(g, genes, x, y, TEXT_LIGHT, w);
                y += 10;
            }
            if (one.berried()) {
                fitLine(g, Component.translatable("gui.bettacraft.critters.one_eggs").withStyle(ChatFormatting.LIGHT_PURPLE), x, y, TEXT_LIGHT, w);
                y += 10;
            }
            y += 2;
            sel = null;
        }
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
            Component breed = !sp.breeds() ? Component.translatable(sp.marine() ? "gui.bettacraft.critters.no_breeding_reef" : "gui.bettacraft.critters.no_breeding").withStyle(ChatFormatting.GRAY)
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
        if (c.clutches > 0 && y < 118 + ch - CONTENT_H) {
            fitLine(g, Component.translatable("gui.bettacraft.critters.clutch_count", c.clutches).withStyle(ChatFormatting.LIGHT_PURPLE), x, y, TEXT_LIGHT, w);
            y += 10;
        }
        if (c.dead > 0 && y < 118 + ch - CONTENT_H) {
            fitLine(g, Component.translatable("gui.bettacraft.critters.dead", c.dead).withStyle(ChatFormatting.GRAY), x, y, TEXT_LIGHT, w);
        }
    }

    /** M62: the x of the details right of the animal list (114 in the smallest menu) and their width (124). */
    private int detailX() {
        return 14 + listW;
    }

    private int detailW() {
        return CONTENT_X + cw - 3 - detailX();
    }

    private void bar(GuiGraphicsExtractor g, int x, int y, Component label, float value, int color) {
        fitLine(g, label, x, y, TEXT_LIGHT, 60);
        int bx = x + 62, bw = detailW() - 64;
        g.fill(bx, y + 1, bx + bw, y + 7, 0xFF202020);
        g.fill(bx, y + 1, bx + Math.round(bw * Mth.clamp(value, 0, 1)), y + 7, color);
    }

    private List<TankView.LogLine> filteredLog(TankView v) {
        if (logFilter == LogFilter.ALL) return v.log;
        List<TankView.LogLine> out = new ArrayList<>();
        for (TankView.LogLine l : v.log) if (LogStyle.category(l.key()) == logFilter.ordinal()) out.add(l);
        return out;
    }

    private void drawLog(GuiGraphicsExtractor g, TankView v, int mx, int my) {
        int x = 11, y = 21;
        List<TankView.LogLine> lines = filteredLog(v);
        if (lines.isEmpty()) {
            line(g, Component.translatable("gui.bettacraft.log.empty"), x, y, 0xA0A0A0);
        }
        int lw = cw - 18;
        for (int r = 0; r < logRows(); r++) {
            int idx = lines.size() - 1 - logScroll - r;
            if (idx < 0) break;
            TankView.LogLine l = lines.get(idx);
            int ly = y + r * 10;
            int color = LogStyle.color(l.key());
            g.fill(x, ly + 2, x + 3, ly + 5, 0xFF000000 | color);
            Component c = Component.translatable("gui.bettacraft.log.line", l.time() / 24000L + 1, TankActions.logLine(l));
            g.text(font, font.plainSubstrByWidth(c.getString(), lw), x + 6, ly, org.overengineer.bettacraft.client.ClientCompat.textColor(color), false);
            if (my >= ly && my < ly + 10 && mx >= x && mx < x + tw() && font.width(c) > lw) pendingTooltip = List.of(c);
        }
        scrollbar(g, lines.size(), logRows(), logScroll, true);
        g.text(font, Component.translatable("gui.bettacraft.log.count", lines.size()), x, 128 + ch - CONTENT_H, org.overengineer.bettacraft.client.ClientCompat.textColor(0x909090), false);
    }

    /** A row of the breeding tab: a piece of a wrapped line and its indent. */
    private record BreedRow(@Nullable net.minecraft.util.FormattedCharSequence text, int indent) {
        /** M66: a thin line between two groups (the text is null). */
        static BreedRow line() {
            return new BreedRow(null, 0);
        }
    }

    /** Text width of the breeding tab (left of the scroll bar; 222 in the smallest menu). */
    private int breedW() {
        return cw - 12;
    }

    /**
     * The lines of the breeding tab wrapped to the content width (long genotype lines, mostly in German): the
     * continuation rows are indented a little further than the line they belong to.
     */
    private List<BreedRow> breedRows(TankView v) {
        List<BreedRow> rows = new ArrayList<>();
        for (Component c : BreedingLines.build(v)) {
            int bw = breedW();
            if (c == BreedingLines.SEPARATOR) {
                rows.add(BreedRow.line());
                continue;
            }
            if (font.width(c) <= bw) {
                rows.add(new BreedRow(c.getVisualOrderText(), 0));
                continue;
            }
            String plain = c.getString();
            int lead = 0;
            while (lead < plain.length() && plain.charAt(lead) == ' ') lead++;
            int indent = font.width(plain.substring(0, lead)) + 8;
            List<net.minecraft.util.FormattedCharSequence> parts = font.split(c, bw - indent);
            for (int i = 0; i < parts.size(); i++) rows.add(new BreedRow(parts.get(i), i == 0 ? 0 : indent));
        }
        return rows;
    }

    private void drawBreeding(GuiGraphicsExtractor g, TankView v) {
        int x = 11, y = 20;
        List<BreedRow> rows = breedRows(v);
        int shown = breedRowCount();
        breedScroll = Mth.clamp(breedScroll, 0, Math.max(0, rows.size() - shown));
        for (int r = 0; r < shown; r++) {
            int idx = breedScroll + r;
            if (idx >= rows.size()) break;
            BreedRow row = rows.get(idx);
            if (row.text() == null) g.fill(9, y + r * 10 + 4, cw + 5, y + r * 10 + 5, 0x55FFFFFF);
            else g.text(font, row.text(), x + row.indent(), y + r * 10, org.overengineer.bettacraft.client.ClientCompat.textColor(TEXT_LIGHT), false);
        }
        scrollbar(g, rows.size(), shown, breedScroll, false);
    }

    /** M19: the breeding tab leaves room for the breeding box button. */
    private int breedRowCount() {
        int rows = BREED_ROWS + (ch - CONTENT_H) / 10;
        return boxButton != null && boxButton.visible ? rows - 2 : rows;
    }

    /** Thin scroll indicator at the right edge of the content area. */
    private void scrollbar(GuiGraphicsExtractor g, int total, int rows, int scroll, boolean fromBottom) {
        if (total <= rows) return;
        int top = CONTENT_Y + 2, h = ch - 4, x = CONTENT_X + cw - 4;
        int bh = Math.max(8, h * rows / total);
        int maxScroll = total - rows;
        float f = maxScroll <= 0 ? 0 : scroll / (float) maxScroll;
        if (fromBottom) f = 1 - f;
        int by = top + Math.round((h - bh) * f);
        g.fill(x, top, x + 2, top + h, 0x40000000);
        g.fill(x, by, x + 2, by + bh, 0xFFA0A0A0);
    }

    /** M62: a thin scroll indicator inside a list (at x, from y, h high) when it has more rows than it shows. */
    private static void listBar(GuiGraphicsExtractor g, int x, int y, int h, int total, int rows, int scroll) {
        if (total <= rows) return;
        int bh = Math.max(6, h * rows / total);
        int by = y + Math.round((h - bh) * (scroll / (float) Math.max(1, total - rows)));
        g.fill(x, y, x + 2, y + h, 0x40000000);
        g.fill(x, by, x + 2, by + bh, 0xFFA0A0A0);
    }

    static String dirKey(Direction d) {
        return "gui.bettacraft.direction." + d.getSerializedName();
    }

    static Component signPos(SignPosition p) {
        return Component.translatable(p.translationKey());
    }
}
