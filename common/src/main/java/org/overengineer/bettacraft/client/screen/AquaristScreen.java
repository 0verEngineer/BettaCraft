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
 * M51 / M66: the menu of a hired aquarist. Offering: what he offers and his wage, with the buttons to hire or refuse him.
 * Hired, in three tabs: <b>Work</b> (his basin, bed and chest with the buttons to assign them, the wage, the six tasks: the
 * name = on/off, the number = level 1 to 3), <b>Rearing tanks</b> (several, in order; the species each takes; the next tank is
 * chosen with the arrows and put in with the check mark at the right) and <b>Tanks</b> (the tanks around his workplace; click
 * one to choose what he does there). Dismissing him needs Shift. Everything comes from the server ({@code AquaristMenu#view}).
 * M69: <b>Team</b> (every aquarist of the employer: name, status, tanks, workplace) in every aquarist's menu, <b>Friend</b>
 * (only the one who recommends friends: the conditions of the next one with their progress, the offer with hire / no
 * thanks, "team full"), and on the tanks tab the aquarist of the team who looks after the selected tank (click: the next).
 */
public class AquaristScreen extends Screen {
    private static final int W = 344, H = 236;
    private static final int TEXT = 0xFFE0E0E0;
    private static final int REARING_ROWS = AquaristMenu.MAX_REARING, TANK_ROWS = 6;

    enum Tab { WORK, REARING, TANKS, TEAM, FRIEND }

    private CompoundTag view;
    private int left, top;
    private Tab tab = Tab.WORK;
    private int scroll;
    private int candidate; // the index in the list of tanks that can be added
    @Nullable
    private UUID selectedTank;
    private final List<Button> offerButtons = new ArrayList<>();
    private final List<Button> hiredButtons = new ArrayList<>();
    private final List<Button> workButtons = new ArrayList<>();
    private final List<Button> rearingButtons = new ArrayList<>();
    private final List<Button> tankButtons = new ArrayList<>();
    private final Button[] tabButtons = new Button[Tab.values().length];
    private final Button[] taskButtons = new Button[AquaristTask.ALL.length];
    private final Button[] levelButtons = new Button[AquaristTask.ALL.length];
    private final Button[] tankTaskButtons = new Button[AquaristTask.ALL.length];
    private Button addButton, prevButton, nextButton, autoChest;
    /** M69: the friend's offer, and who looks after the selected tank. */
    private final List<Button> friendButtons = new ArrayList<>();
    private Button whoButton;
    private final Button[] speciesButtons = new Button[REARING_ROWS];
    private final Button[] removeButtons = new Button[REARING_ROWS];

    public AquaristScreen(CompoundTag view) {
        super(Component.literal(view.getString("name")));
        this.view = view;
        // M69: the mediator with a friend on offer opens at the tab Friend (the chat button leads here)
        if (view.getBoolean("mediator") && view.getCompound("friend").getBoolean("offered")) tab = Tab.FRIEND;
    }

    /** M69: whether the tab Friend is there (only the aquarist who recommends friends, with the switch on). */
    private boolean friendTab() {
        return view.getBoolean("mediator") && view.getBoolean("team_feature");
    }

    /** A view from the server: opens the menu, or refreshes the open one (or its species list) of the same aquarist. */
    public static void accept(CompoundTag view) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof AquaristScreen s && s.view.getInt("id") == view.getInt("id")) {
            s.view = view;
            s.refresh();
            return;
        }
        if (mc.screen instanceof AquaristSpeciesScreen sp && sp.parent().view.getInt("id") == view.getInt("id")) {
            sp.parent().view = view;
            sp.parent().refresh();
            return;
        }
        mc.setScreen(new AquaristScreen(view));
    }

    CompoundTag view() {
        return view;
    }

    private boolean offering() {
        return "offering".equals(view.getString("phase"));
    }

    private boolean boss() {
        return view.getBoolean("boss");
    }

    /** Dev smoke test only: the tab (0 work, 1 rearing tanks, 2 tanks, 3 team, 4 friend). */
    public void showTabForTest(int index) {
        tab = Tab.values()[index];
        scroll = 0;
        refresh();
    }

    /** Dev smoke test only: selects the first tank of the list on the tanks tab. */
    public void selectFirstTankForTest() {
        var list = view.getList("tanks", Tag.TAG_COMPOUND);
        if (!list.isEmpty()) selectedTank = list.getCompound(0).getUUID("id");
        refresh();
    }

    /** Dev smoke test only (M69): selects this tank on the tanks tab. */
    public void selectTankForTest(UUID id) {
        selectedTank = id;
        refresh();
    }

    /** Dev smoke test only (M69): a button's action without a click (e.g. {@link AquaristMenu#FRIEND_HIRE}). */
    public void sendForTest(int action) {
        send(action, 0, "");
    }

    /** Dev smoke test only: opens the species list of the first rearing tank. */
    public void openSpeciesForTest() {
        UUID id = rearingId(0);
        if (id != null && minecraft != null) minecraft.setScreen(new AquaristSpeciesScreen(this, id));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        offerButtons.clear();
        hiredButtons.clear();
        workButtons.clear();
        rearingButtons.clear();
        tankButtons.clear();
        friendButtons.clear();
        offerButtons.add(addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.aquarist.hire").withStyle(ChatFormatting.GREEN),
                b -> send(AquaristMenu.HIRE, 0, "")).bounds(left + 40, top + H - 34, 100, 20).build()));
        offerButtons.add(addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.aquarist.decline"), b -> {
            send(AquaristMenu.DECLINE, 0, "");
            onClose();
        }).bounds(left + W - 140, top + H - 34, 100, 20).build()));

        // the tabs (placed in refresh: the tab Friend only for the one who recommends friends)
        for (Tab t : Tab.values()) {
            tabButtons[t.ordinal()] = addRenderableWidget(new TankScreen.FitButton(left + 8, top + 18, 60, 14,
                    Component.translatable("gui.bettacraft.aquarist.tab." + t.name().toLowerCase(java.util.Locale.ROOT)), b -> {
                tab = t;
                scroll = 0;
                refresh();
            }));
        }

        // ---- work tab: the home rows (basin, bed, chest) and the six tasks
        int bx = left + W - 126;
        homeButton(bx, top + 38, "basin", AquaristMenu.ASSIGN_BASIN, AquaristMenu.AUTO_BASIN);
        homeButton(bx, top + 54, "bed", AquaristMenu.ASSIGN_BED, AquaristMenu.AUTO_BED);
        homeButton(bx, top + 70, "chest", AquaristMenu.ASSIGN_CHEST, AquaristMenu.AUTO_CHEST);
        for (AquaristTask t : AquaristTask.ALL) {
            int i = t.ordinal(), col = i / 3, row = i % 3;
            int x = left + 8 + col * 168, y = top + 128 + row * 17;
            taskButtons[i] = addRenderableWidget(new TankScreen.FitButton(x, y, 130, 15, Component.empty(), b -> {
                int lv = levels()[i];
                send(AquaristMenu.SET_LEVEL, i * 10 + (lv > 0 ? 0 : t.defaultLevel()), "");
            }));
            levelButtons[i] = addRenderableWidget(Button.builder(Component.empty(), b -> {
                int lv = levels()[i];
                send(AquaristMenu.SET_LEVEL, i * 10 + (lv % AquaristTask.MAX_LEVEL) + 1, "");
            }).bounds(x + 132, y, 24, 15).build());
            workButtons.add(taskButtons[i]);
            workButtons.add(levelButtons[i]);
        }

        // ---- rearing tab: the rows (species, remove) and the choice of the next tank
        for (int i = 0; i < REARING_ROWS; i++) {
            int row = i;
            int y = top + 62 + i * 16;
            speciesButtons[i] = addRenderableWidget(new TankScreen.FitButton(left + W - 8 - 16 - 2 - 110, y - 1, 110, 14, Component.empty(), b -> {
                UUID id = rearingId(row);
                if (id != null && minecraft != null) minecraft.setScreen(new AquaristSpeciesScreen(this, id));
            }));
            removeButtons[i] = addRenderableWidget(Button.builder(Component.literal("✕").withStyle(ChatFormatting.RED), b -> {
                UUID id = rearingId(row);
                if (id != null) send(AquaristMenu.REMOVE_REARING, 0, id.toString());
            }).bounds(left + W - 8 - 16, y - 1, 16, 14).build());
            removeButtons[i].setTooltip(Tooltip.create(Component.translatable("gui.bettacraft.aquarist.rearing.remove")));
            rearingButtons.add(speciesButtons[i]);
            rearingButtons.add(removeButtons[i]);
        }
        prevButton = addRenderableWidget(Button.builder(Component.literal("<"), b -> moveCandidate(-1)).bounds(left + 8, top + H - 56, 16, 14).build());
        nextButton = addRenderableWidget(Button.builder(Component.literal(">"), b -> moveCandidate(1)).bounds(left + 26, top + H - 56, 16, 14).build());
        addButton = addRenderableWidget(Button.builder(Component.literal("✔").withStyle(ChatFormatting.GREEN), b -> {
            CompoundTag c = candidateTank();
            if (c != null) send(AquaristMenu.ADD_REARING, 0, c.getUUID("id").toString());
        }).bounds(left + W - 8 - 20, top + H - 56, 20, 14).build());
        addButton.setTooltip(Tooltip.create(Component.translatable("gui.bettacraft.aquarist.rearing.add_tooltip")));
        rearingButtons.add(prevButton);
        rearingButtons.add(nextButton);
        rearingButtons.add(addButton);

        // ---- tanks tab: what he does at the tank that is selected
        for (AquaristTask t : AquaristTask.ALL) {
            int i = t.ordinal(), col = i / 3, row = i % 3;
            tankTaskButtons[i] = addRenderableWidget(new TankScreen.FitButton(left + 8 + col * 168, top + H - 72 + row * 15, 160, 14, Component.empty(), b -> {
                CompoundTag c = selectedTankTag();
                if (c == null) return;
                boolean off = (c.getInt("off") & (1 << i)) != 0; // switched off now: switch on
                send(AquaristMenu.SET_TANK_TASK, i * 2 + (off ? 1 : 0), c.getUUID("id").toString());
            }));
            tankButtons.add(tankTaskButtons[i]);
        }
        // M69: who of the team looks after the selected tank (click: the next one, then back to the nearest workplace)
        whoButton = addRenderableWidget(new TankScreen.FitButton(left + W - 8 - 130, top + H - 88, 130, 13, Component.empty(), b -> cycleWho()));
        whoButton.setTooltip(Tooltip.create(Component.translatable("gui.bettacraft.aquarist.team.who.tooltip")));

        // ---- M69 friend tab: hire the friend or not
        friendButtons.add(addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.aquarist.friend.hire").withStyle(ChatFormatting.GREEN),
                b -> send(AquaristMenu.FRIEND_HIRE, 0, "")).bounds(left + 40, top + H - 46, 110, 18).build()));
        friendButtons.add(addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.aquarist.friend.decline"),
                b -> send(AquaristMenu.FRIEND_DECLINE, 0, "")).bounds(left + W - 150, top + H - 46, 110, 18).build()));

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

    /** "Assign" (waits for the next click on a block) and "Auto" for the bed or the chest. */
    private void homeButton(int x, int y, String what, int assign, int auto) {
        Button a = addRenderableWidget(new TankScreen.FitButton(x, y, 80, 14, Component.translatable("gui.bettacraft.aquarist.assign"), b -> {
            send(assign, 0, "");
            onClose();
        }));
        a.setTooltip(Tooltip.create(Component.translatable("gui.bettacraft.aquarist.assign." + what + ".tooltip", view.getInt("home_radius"))));
        Button u = addRenderableWidget(new TankScreen.FitButton(x + 82, y, 36, 14, Component.translatable("gui.bettacraft.aquarist.auto"), b -> send(auto, 0, "")));
        u.setTooltip(Tooltip.create(Component.translatable("gui.bettacraft.aquarist.auto." + what + ".tooltip")));
        workButtons.add(a);
        workButtons.add(u);
        if (what.equals("chest")) autoChest = u;
    }

    private int[] levels() {
        int[] lv = view.getIntArray("tasks");
        return lv.length == AquaristTask.ALL.length ? lv : new int[AquaristTask.ALL.length];
    }

    private void refresh() {
        boolean offer = offering(), boss = boss();
        for (Button b : offerButtons) {
            b.visible = offer;
            b.active = boss;
        }
        for (Button b : hiredButtons) {
            b.visible = !offer;
            b.active = boss;
        }
        if (tab == Tab.FRIEND && !friendTab()) tab = Tab.TEAM;
        int count = friendTab() ? Tab.values().length : Tab.values().length - 1;
        int tw = (W - 16 - 4 * (count - 1)) / count;
        for (Tab t : Tab.values()) {
            Button b = tabButtons[t.ordinal()];
            b.setX(left + 8 + t.ordinal() * (tw + 4));
            b.setWidth(tw);
            b.visible = !offer && t.ordinal() < count;
            b.active = t != tab;
        }
        // the tab Friend shines while a friend is on offer
        CompoundTag friend = view.getCompound("friend");
        tabButtons[Tab.FRIEND.ordinal()].setMessage(friend.getBoolean("offered")
                ? Component.literal("★ ").append(Component.translatable("gui.bettacraft.aquarist.tab.friend")).withStyle(ChatFormatting.GOLD)
                : Component.translatable("gui.bettacraft.aquarist.tab.friend"));
        boolean full = view.getInt("team_hired") >= view.getInt("team_max");
        for (Button b : friendButtons) {
            b.visible = !offer && tab == Tab.FRIEND && friend.getBoolean("offered") && !full;
            b.active = boss;
        }
        for (Button b : workButtons) {
            b.visible = !offer && tab == Tab.WORK;
            b.active = boss;
        }
        for (Button b : rearingButtons) {
            b.visible = !offer && tab == Tab.REARING;
            b.active = boss;
        }
        CompoundTag sel = selectedTankTag();
        for (Button b : tankButtons) b.visible = !offer && tab == Tab.TANKS && sel != null;
        int teamSize = view.getList("team", Tag.TAG_COMPOUND).size();
        whoButton.visible = !offer && tab == Tab.TANKS && sel != null && teamSize > 1;
        whoButton.active = boss;
        if (sel != null) {
            String who = sel.hasUUID("who") ? memberName(sel.getUUID("who")) : "";
            Component w = who.isEmpty() ? Component.translatable("gui.bettacraft.aquarist.team.who_nobody")
                    : Component.translatable(sel.getBoolean("given") ? "gui.bettacraft.aquarist.team.who_given" : "gui.bettacraft.aquarist.team.who_auto", who);
            whoButton.setMessage(w);
        }
        autoChest.active = boss && view.contains("chest");
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
        // the rearing rows
        var rear = view.getList("rearing_tanks", Tag.TAG_COMPOUND);
        for (int i = 0; i < REARING_ROWS; i++) {
            boolean row = tab == Tab.REARING && !offer && i < rear.size();
            speciesButtons[i].visible = row;
            removeButtons[i].visible = row;
            if (!row) continue;
            int n = rear.getCompound(i).getList("species", Tag.TAG_STRING).size();
            speciesButtons[i].setMessage(n == 0 ? Component.translatable("gui.bettacraft.aquarist.rearing.species_all")
                    : Component.translatable("gui.bettacraft.aquarist.rearing.species_n", n));
            speciesButtons[i].setTooltip(Tooltip.create(Component.translatable("gui.bettacraft.aquarist.rearing.species_tooltip")));
        }
        List<CompoundTag> cands = candidates();
        candidate = cands.isEmpty() ? 0 : Math.floorMod(candidate, cands.size());
        prevButton.active = boss && cands.size() > 1;
        nextButton.active = boss && cands.size() > 1;
        addButton.active = boss && !cands.isEmpty() && rear.size() < REARING_ROWS;
        // the six switches of the tanks tab
        if (sel != null) {
            for (AquaristTask t : AquaristTask.ALL) {
                Button b = tankTaskButtons[t.ordinal()];
                boolean on = (sel.getInt("off") & (1 << t.ordinal())) == 0;
                b.setMessage(Component.literal(on ? "☑ " : "☐ ").append(Component.translatable(t.translationKey())).withStyle(on ? ChatFormatting.WHITE : ChatFormatting.GRAY));
                b.setTooltip(Tooltip.create(Component.translatable(t.translationKey() + ".tooltip")));
                b.active = boss && works(sel.getString("verdict"));
            }
        }
    }

    /** M69: the name of a member of the team ("" when he is not in it). */
    private String memberName(UUID id) {
        for (Tag t : view.getList("team", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            if (c.getUUID("id").equals(id)) return c.getString("name");
        }
        return "";
    }

    /** M69: the selected tank goes to the next aquarist of the team (after the last: back to the nearest workplace). */
    private void cycleWho() {
        CompoundTag sel = selectedTankTag();
        if (sel == null) return;
        var team = view.getList("team", Tag.TAG_COMPOUND);
        if (team.isEmpty()) return;
        String tank = sel.getUUID("id").toString();
        if (!sel.getBoolean("given")) { // automatic now: the first one that is not the automatic one
            UUID auto = sel.hasUUID("who") ? sel.getUUID("who") : null;
            for (Tag t : team) {
                UUID id = ((CompoundTag) t).getUUID("id");
                if (!id.equals(auto)) {
                    send(AquaristMenu.SET_RESPONSIBLE, 0, tank + "|" + id);
                    return;
                }
            }
            return;
        }
        UUID cur = sel.getUUID("who");
        for (int i = 0; i < team.size(); i++) {
            if (!team.getCompound(i).getUUID("id").equals(cur)) continue;
            if (i + 1 < team.size()) send(AquaristMenu.SET_RESPONSIBLE, 0, tank + "|" + team.getCompound(i + 1).getUUID("id"));
            else send(AquaristMenu.SET_RESPONSIBLE, 0, tank + "|");
            return;
        }
        send(AquaristMenu.SET_RESPONSIBLE, 0, tank + "|");
    }

    private static boolean works(String verdict) {
        return verdict.equals("own") || verdict.equals("team") || verdict.equals("released");
    }

    /** The tanks of the list he may work at. */
    private List<CompoundTag> workable() {
        List<CompoundTag> out = new ArrayList<>();
        for (Tag t : view.getList("tanks", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            if (works(c.getString("verdict"))) out.add(c);
        }
        return out;
    }

    /** The workable tanks that are no rearing tank yet (the choice for the next one). */
    private List<CompoundTag> candidates() {
        List<UUID> have = new ArrayList<>();
        for (Tag t : view.getList("rearing_tanks", Tag.TAG_COMPOUND)) have.add(((CompoundTag) t).getUUID("id"));
        List<CompoundTag> out = new ArrayList<>();
        for (CompoundTag c : workable()) if (!have.contains(c.getUUID("id"))) out.add(c);
        return out;
    }

    @Nullable
    private CompoundTag candidateTank() {
        List<CompoundTag> c = candidates();
        return c.isEmpty() ? null : c.get(Math.floorMod(candidate, c.size()));
    }

    private void moveCandidate(int dir) {
        candidate += dir;
        refresh();
    }

    @Nullable
    private UUID rearingId(int row) {
        var rear = view.getList("rearing_tanks", Tag.TAG_COMPOUND);
        return row >= 0 && row < rear.size() ? rear.getCompound(row).getUUID("id") : null;
    }

    @Nullable
    private CompoundTag selectedTankTag() {
        if (selectedTank == null) return null;
        for (Tag t : view.getList("tanks", Tag.TAG_COMPOUND)) if (((CompoundTag) t).getUUID("id").equals(selectedTank)) return (CompoundTag) t;
        return null;
    }

    void send(int action, int value, String text) {
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
        int n = tab == Tab.TANKS ? view.getList("tanks", Tag.TAG_COMPOUND).size() : 0;
        scroll = Math.max(0, Math.min(Math.max(0, n - TANK_ROWS), scroll - (int) Math.signum(sy)));
        return true;
    }

    /** The top of the row {@code i} of the tank list. */
    private int rowTop(int i) {
        return top + 57 + i * 12;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (!offering() && tab == Tab.TANKS && button == 0) {
            var list = view.getList("tanks", Tag.TAG_COMPOUND);
            for (int i = 0; i < TANK_ROWS && scroll + i < list.size(); i++) {
                if (mx >= left + 8 && mx < left + W - 12 && my >= rowTop(i) && my < rowTop(i) + 12) {
                    selectedTank = list.getCompound(scroll + i).getUUID("id");
                    refresh();
                    return true;
                }
            }
        }
        return false;
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
        g.drawCenteredString(font, head, left + W / 2, top + 6, 0xFFFFFF);
        if (offering()) {
            renderOffer(g);
            return;
        }
        switch (tab) {
            case WORK -> renderWork(g);
            case REARING -> renderRearing(g);
            case TANKS -> renderTanks(g);
            case TEAM -> renderTeam(g);
            case FRIEND -> renderFriend(g);
        }
        renderStatusLine(g);
    }

    private void renderOffer(GuiGraphics g) {
        int y = top + 26;
        Component text = Component.translatable("gui.bettacraft.aquarist.offer_text", view.getString("employer"));
        for (FormattedCharSequence line : font.split(text, W - 20)) {
            g.drawString(font, line, left + 10, y, TEXT);
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
        if (!boss()) {
            g.drawCenteredString(font, Component.translatable("message.bettacraft.aquarist.not_yours").withStyle(ChatFormatting.RED), left + W / 2, top + H - 10, 0xFFFFFF);
        }
    }

    /** The line at the bottom, left of the dismiss button: the status. */
    private void renderStatusLine(GuiGraphics g) {
        String status = view.getString("status");
        boolean bad = status.equals("no_workplace") || status.equals("no_bed") || status.equals("unpaid") || status.equals("off") || status.equals("waiting_home");
        Component st = Component.translatable("gui.bettacraft.aquarist.status", Component.translatable("gui.bettacraft.aquarist.status." + status)
                .withStyle(bad ? ChatFormatting.RED : ChatFormatting.GREEN), view.getString("employer"));
        fit(g, st, left + 8, top + H - 17, W - 108);
    }

    private void renderWork(GuiGraphics g) {
        int room = W - 132 - 8;
        Component wp = view.contains("workplace") ? Component.translatable("gui.bettacraft.aquarist.workplace", pos(view.getLong("workplace")))
                : Component.translatable("gui.bettacraft.aquarist.no_workplace").withStyle(ChatFormatting.RED);
        Component bed = view.contains("bed") ? Component.translatable("gui.bettacraft.aquarist.bed", pos(view.getLong("bed")))
                : Component.translatable("gui.bettacraft.aquarist.no_bed").withStyle(ChatFormatting.RED);
        Component chest = view.contains("chest") ? Component.translatable("gui.bettacraft.aquarist.chest_assigned", pos(view.getLong("chest")))
                : Component.translatable("gui.bettacraft.aquarist.chest_auto");
        fit(g, wp, left + 8, top + 41, room);
        fit(g, bed, left + 8, top + 57, room);
        fit(g, chest, left + 8, top + 73, room);
        Component stock = view.getInt("chests") == 0 ? Component.translatable("gui.bettacraft.aquarist.no_chest").withStyle(ChatFormatting.GOLD)
                : view.getBoolean("food") ? Component.translatable("gui.bettacraft.aquarist.chest_food").withStyle(ChatFormatting.GRAY)
                : Component.translatable("gui.bettacraft.aquarist.chest_no_food").withStyle(ChatFormatting.GOLD);
        fit(g, stock, left + 8, top + 89, W - 16);
        Component wage;
        if (!view.getBoolean("wage")) wage = Component.translatable("gui.bettacraft.aquarist.wage_off").withStyle(ChatFormatting.GRAY);
        else if (view.getInt("unpaid") > 0) wage = Component.translatable("gui.bettacraft.aquarist.unpaid", view.getInt("unpaid"), view.getInt("leave_days")).withStyle(ChatFormatting.RED);
        else wage = Component.translatable("gui.bettacraft.aquarist.wage", view.getInt("wage_emeralds"), view.getInt("wage_food")).withStyle(ChatFormatting.GOLD);
        fit(g, wage, left + 8, top + 101, W - 16);
        g.drawString(font, Component.translatable("gui.bettacraft.aquarist.tasks").withStyle(ChatFormatting.YELLOW), left + 8, top + 116, 0xFFFFFF);
    }

    private void renderRearing(GuiGraphics g) {
        int y = top + 38;
        for (FormattedCharSequence line : font.split(Component.translatable("gui.bettacraft.aquarist.rearing.title").withStyle(ChatFormatting.GRAY), W - 16)) {
            g.drawString(font, line, left + 8, y, 0xFFFFFF);
            y += 10;
        }
        var rear = view.getList("rearing_tanks", Tag.TAG_COMPOUND);
        if (rear.isEmpty()) {
            g.drawString(font, Component.translatable("gui.bettacraft.aquarist.rearing.none").withStyle(ChatFormatting.GRAY), left + 12, top + 64, 0xFFFFFF);
        }
        for (int i = 0; i < Math.min(REARING_ROWS, rear.size()); i++) {
            CompoundTag c = rear.getCompound(i);
            MutableComponent name = Component.literal((i + 1) + ". " + name(c));
            if (!c.getBoolean("ok")) name.withStyle(ChatFormatting.RED).append(Component.literal(" ")).append(Component.translatable("gui.bettacraft.aquarist.rearing.not_his"));
            fit(g, name, left + 8, top + 62 + i * 16 + 2, W - 8 - 16 - 2 - 110 - 8 - 6);
        }
        // the choice of the next one
        g.drawString(font, Component.translatable("gui.bettacraft.aquarist.rearing.add").withStyle(ChatFormatting.YELLOW), left + 8, top + H - 72, 0xFFFFFF);
        CompoundTag c = candidateTank();
        Component cand = c == null ? Component.translatable("gui.bettacraft.aquarist.rearing.no_candidate").withStyle(ChatFormatting.GRAY)
                : Component.literal(name(c));
        fit(g, cand, left + 46, top + H - 53, W - 46 - 8 - 20 - 6);
    }

    private void renderTanks(GuiGraphics g) {
        fit(g, Component.translatable("gui.bettacraft.aquarist.tanks", view.getInt("cared"), view.getInt("max_tanks"), view.getInt("radius"))
                .withStyle(ChatFormatting.AQUA), left + 8, top + 40, W - 16);
        var list = view.getList("tanks", Tag.TAG_COMPOUND);
        g.fill(left + 6, rowTop(0) - 1, left + W - 6, rowTop(TANK_ROWS), 0x30000000);
        for (int i = scroll; i < Math.min(list.size(), scroll + TANK_ROWS); i++) {
            CompoundTag c = list.getCompound(i);
            String v = c.getString("verdict");
            int y = rowTop(i - scroll);
            if (c.getUUID("id").equals(selectedTank)) g.fill(left + 7, y, left + W - 12, y + 12, 0x60FFFFFF);
            fit(g, Component.literal(name(c)), left + 10, y + 2, 170);
            int off = Integer.bitCount(c.getInt("off"));
            Component verdict = Component.translatable("gui.bettacraft.aquarist.tank." + v).withStyle(verdictColor(v));
            // M69: a tank another aquarist of the team looks after
            if (works(v) && c.hasUUID("who") && view.hasUUID("uuid") && !c.getUUID("who").equals(view.getUUID("uuid"))) {
                verdict = Component.translatable("gui.bettacraft.aquarist.tank.other", memberName(c.getUUID("who"))).withStyle(ChatFormatting.DARK_AQUA);
            } else if (works(v) && off > 0) verdict = Component.translatable("gui.bettacraft.aquarist.tanks.some_off", AquaristTask.ALL.length - off, AquaristTask.ALL.length).withStyle(ChatFormatting.YELLOW);
            fit(g, verdict, left + 186, y + 2, W - 186 - 16);
        }
        if (list.isEmpty()) {
            g.drawString(font, Component.translatable(view.contains("workplace") ? "gui.bettacraft.aquarist.no_tanks" : "gui.bettacraft.aquarist.no_tanks_no_workplace")
                    .withStyle(ChatFormatting.GRAY), left + 12, rowTop(0) + 2, 0xFFFFFF);
        }
        if (list.size() > TANK_ROWS) {
            int h = TANK_ROWS * 12, bh = Math.max(8, h * TANK_ROWS / list.size());
            int by = rowTop(0) + Math.round((h - bh) * (scroll / (float) Math.max(1, list.size() - TANK_ROWS)));
            g.fill(left + W - 10, rowTop(0), left + W - 8, rowTop(0) + h, 0x40000000);
            g.fill(left + W - 10, by, left + W - 8, by + bh, 0xFFA0A0A0);
        }
        CompoundTag sel = selectedTankTag();
        if (sel == null) {
            g.drawString(font, Component.translatable("gui.bettacraft.aquarist.tanks.select").withStyle(ChatFormatting.GRAY), left + 8, top + H - 76, 0xFFFFFF);
        } else {
            int room = view.getList("team", Tag.TAG_COMPOUND).size() > 1 ? W - 16 - 134 : W - 16;
            fit(g, Component.translatable("gui.bettacraft.aquarist.tanks.at", name(sel)).withStyle(ChatFormatting.YELLOW), left + 8, top + H - 84, room);
        }
    }

    /** M69: every aquarist of the employer: name, status, tanks, workplace, days with him; ★ = the one who recommends friends. */
    private void renderTeam(GuiGraphics g) {
        var team = view.getList("team", Tag.TAG_COMPOUND);
        fit(g, Component.translatable("gui.bettacraft.aquarist.team.title", view.getInt("team_hired"), view.getInt("team_max")).withStyle(ChatFormatting.AQUA),
                left + 8, top + 40, W - 16);
        int y = top + 56;
        g.fill(left + 6, y - 2, left + W - 6, y + 12 * Math.max(1, Math.min(team.size(), 10)) + 1, 0x30000000);
        for (int i = 0; i < Math.min(team.size(), 10); i++) {
            CompoundTag c = team.getCompound(i);
            int ry = y + i * 12;
            if (c.getBoolean("me")) g.fill(left + 7, ry - 1, left + W - 7, ry + 11, 0x40FFFFFF);
            MutableComponent nm = Component.literal((c.getBoolean("mediator") ? "★ " : "   ") + c.getString("name")).withStyle(c.getBoolean("mediator") ? ChatFormatting.GOLD : ChatFormatting.WHITE);
            fit(g, nm, left + 10, ry + 1, 78);
            String st = c.getString("status");
            boolean bad = st.equals("no_workplace") || st.equals("no_bed") || st.equals("unpaid") || st.equals("off") || st.equals("waiting_home");
            fit(g, Component.translatable("gui.bettacraft.aquarist.status." + st).withStyle(bad ? ChatFormatting.RED : st.equals("unloaded") ? ChatFormatting.DARK_GRAY : ChatFormatting.GREEN),
                    left + 92, ry + 1, 94);
            fit(g, Component.translatable("gui.bettacraft.aquarist.team.cared", c.getInt("cared")).withStyle(ChatFormatting.GRAY), left + 190, ry + 1, 56);
            Component wp = c.contains("workplace") ? Component.literal(pos(c.getLong("workplace"))).withStyle(ChatFormatting.GRAY)
                    : Component.translatable("gui.bettacraft.aquarist.team.no_workplace").withStyle(ChatFormatting.RED);
            fit(g, wp, left + 250, ry + 1, W - 250 - 10);
        }
        int ty = y + 12 * Math.max(1, Math.min(team.size(), 10)) + 8;
        Component note = view.getBoolean("mediator") ? Component.translatable("gui.bettacraft.aquarist.team.you_recommend")
                : view.contains("mediator_name") ? Component.translatable("gui.bettacraft.aquarist.team.recommends", view.getString("mediator_name"))
                : Component.empty();
        if (!view.getBoolean("team_feature")) note = Component.translatable("gui.bettacraft.aquarist.team.feature_off");
        for (FormattedCharSequence line : font.split(note.copy().withStyle(ChatFormatting.GRAY), W - 16)) {
            g.drawString(font, line, left + 8, ty, 0xFFFFFF);
            ty += 10;
        }
        for (FormattedCharSequence line : font.split(Component.translatable("gui.bettacraft.aquarist.team.shared").withStyle(ChatFormatting.DARK_GRAY), W - 16)) {
            g.drawString(font, line, left + 8, ty, 0xFFFFFF);
            ty += 10;
        }
    }

    /** M69: the friend: "team full", the offer, or the conditions of the next one with their progress (like the breeding tab). */
    private void renderFriend(GuiGraphics g) {
        CompoundTag f = view.getCompound("friend");
        int hired = view.getInt("team_hired"), max = view.getInt("team_max");
        int y = top + 40;
        if (hired >= max) {
            g.drawCenteredString(font, Component.translatable("gui.bettacraft.aquarist.friend.full", hired, max).withStyle(ChatFormatting.GOLD), left + W / 2, top + 80, 0xFFFFFF);
            int fy = top + 100;
            for (FormattedCharSequence line : font.split(Component.translatable("gui.bettacraft.aquarist.friend.full_text").withStyle(ChatFormatting.GRAY), W - 40)) {
                g.drawCenteredString(font, line, left + W / 2, fy, 0xFFFFFF);
                fy += 10;
            }
            return;
        }
        if (f.getBoolean("offered")) {
            Component text = Component.translatable("gui.bettacraft.aquarist.friend.offer_text", f.getString("name"));
            for (FormattedCharSequence line : font.split(text, W - 20)) {
                g.drawString(font, line, left + 10, y, TEXT);
                y += 10;
            }
            y += 6;
            Component pay = !view.getBoolean("wage") ? Component.translatable("gui.bettacraft.aquarist.friend.free")
                    : Component.translatable("gui.bettacraft.aquarist.friend.wage", view.getInt("wage_emeralds"), view.getInt("wage_food"));
            for (FormattedCharSequence line : font.split(pay.copy().withStyle(ChatFormatting.GOLD), W - 20)) {
                g.drawString(font, line, left + 10, y, 0xFFFFFF);
                y += 10;
            }
            y += 6;
            for (FormattedCharSequence line : font.split(Component.translatable("gui.bettacraft.aquarist.friend.home_text").withStyle(ChatFormatting.GRAY), W - 20)) {
                g.drawString(font, line, left + 10, y, 0xFFFFFF);
                y += 10;
            }
            g.drawString(font, Component.translatable("gui.bettacraft.aquarist.friend.team_after", hired + 1, max).withStyle(ChatFormatting.DARK_GRAY), left + 10, top + H - 62, 0xFFFFFF);
            return;
        }
        for (FormattedCharSequence line : font.split(Component.translatable("gui.bettacraft.aquarist.friend.intro").withStyle(ChatFormatting.GRAY), W - 16)) {
            g.drawString(font, line, left + 8, y, 0xFFFFFF);
            y += 10;
        }
        y += 4;
        int daysLeft = -1;
        for (Tag t : f.getList("conds", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            boolean ok = c.getBoolean("ok");
            String k = c.getString("k");
            MutableComponent line = Component.literal(ok ? "✔ " : "✘ ").withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED)
                    .append(Component.translatable("gui.bettacraft.aquarist.friend.cond." + k, c.getInt("have"), c.getInt("need")).withStyle(ok ? ChatFormatting.WHITE : ChatFormatting.GRAY));
            fit(g, line, left + 12, y, W - 24);
            y += 13;
            if (k.equals("days") && !ok) daysLeft = Math.max(daysLeft, c.getInt("need") - c.getInt("have"));
            if (k.equals("wait") && !ok) daysLeft = Math.max(daysLeft, c.getInt("have"));
        }
        y += 4;
        if (daysLeft > 0) {
            g.drawString(font, Component.translatable("gui.bettacraft.aquarist.friend.in_days", daysLeft).withStyle(ChatFormatting.YELLOW), left + 8, y, 0xFFFFFF);
        }
    }

    private static String name(CompoundTag c) {
        String n = c.getString("name");
        return n.isEmpty() ? pos(c.getLong("pos")) : n + " (" + pos(c.getLong("pos")) + ")";
    }

    /** A line that is drawn smaller when it does not fit. */
    private void fit(GuiGraphics g, Component c, int x, int y, int room) {
        int w = font.width(c);
        if (w <= room) {
            g.drawString(font, c, x, y, TEXT);
            return;
        }
        float s = Math.max(0.6F, room / (float) w);
        g.pose().pushPose();
        g.pose().translate(x, y + (1 - s) * 4, 0);
        g.pose().scale(s, s, 1);
        g.drawString(font, c, 0, 0, TEXT);
        g.pose().popPose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + W, top + H, 0xE0101418);
        g.renderOutline(left, top, W, H, 0xFF4FA3A0);
        if (!offering()) {
            g.fill(left + 4, top + 35, left + W - 4, top + 36, 0x804FA3A0);
            if (tab == Tab.WORK) g.fill(left + 4, top + 113, left + W - 4, top + 114, 0x804FA3A0);
            if (tab == Tab.REARING) g.fill(left + 4, top + H - 78, left + W - 4, top + H - 77, 0x804FA3A0);
            if (tab == Tab.TANKS) g.fill(left + 4, top + H - 90, left + W - 4, top + H - 89, 0x804FA3A0);
            if (tab == Tab.FRIEND && view.getCompound("friend").getBoolean("offered")) g.fill(left + 4, top + H - 52, left + W - 4, top + H - 51, 0x804FA3A0);
            g.fill(left + 4, top + H - 24, left + W - 4, top + H - 23, 0x804FA3A0);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
