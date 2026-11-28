package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.aquarist.AquaristArrival;
import org.overengineer.bettacraft.aquarist.AquaristEntity;
import org.overengineer.bettacraft.aquarist.AquaristMenu;
import org.overengineer.bettacraft.aquarist.AquaristRecords;
import org.overengineer.bettacraft.aquarist.AquaristTeam;
import org.overengineer.bettacraft.aquarist.AquaristWork;
import org.overengineer.bettacraft.platform.item.IItemHandler;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M69 client smoke ({@code -Psmoketestm69}, NeoForge and Fabric, background window): the menus of a team of aquarists in English
 * and German. A station with two hired aquarists (each with his basin, chest and bed) and seven tanks; the tab Friend of the first
 * with the conditions (the tanks still short), then with the friend on offer (two more tanks) and the recommendation in the chat;
 * the tab Team of the second; the tab Tanks with who looks after the selected tank; the friend hired without a home of his own
 * (he waits: "waits for a bed and a basin"); the tab Friend at "Team full (3/3)"; the station from outside. Checks: the view of
 * each step (offer, team size, status) and that no tank is looked after by two. Screenshots
 * {@code run/screenshots/bettacraft_m69_<lang>_<shot>.png}.
 */
final class DevClientSmokeM69 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private int problems;
    private volatile BlockPos base, basinA, basinB;
    private volatile AquaristEntity first, second;
    private final List<BlockPos> tanks = new ArrayList<>();
    private final List<UUID> tankIds = new java.util.concurrent.CopyOnWriteArrayList<>();
    private String lang;
    private int guiScale;
    private boolean once;

    DevClientSmokeM69() {
        then(1, () -> {
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            lang = mc.options.languageCode;
            guiScale = mc.options.guiScale().get();
            server(this::setup);
        });
        then(100, () -> server(this::setupTeam)); // later: the point of interest tickets of the basins and beds exist after a few ticks
        for (String code : List.of("en_us", "de_de")) {
            then(1, () -> {
                language(code);
                scale(2);
            });
            then(10, () -> server(p -> reset(p, code.equals("en_us"))));
            then(5, () -> {});
            shots(code);
        }
        then(1, () -> {
            language(lang);
            scale(guiScale);
        });
        then(5, () -> LOGGER.info("[smoketest] M69 done: {} problem(s)", problems));
    }

    int problems() {
        return problems;
    }

    // ------------------------------------------------------------------ the shots of one language

    private void shots(String code) {
        String pre = code + "_";
        // the tab Friend of the first: the conditions (7 of 9 tanks: not yet)
        menu(() -> first);
        then(1, () -> tab(4));
        then(8, () -> shot(pre + "friend_conditions"));
        then(1, () -> server(p -> {
            CompoundTag v = AquaristMenu.view(p, first);
            if (!v.getBooleanOr("mediator", false) || v.getCompoundOrEmpty("friend").getBooleanOr("offered", false)) problem("the first recommends, nothing offered yet: " + v.getCompoundOrEmpty("friend"));
        }));
        // the tab Team of the second, the tab Tanks of the first with the tank of the second selected
        closeAll();
        menu(() -> second);
        then(1, () -> tab(3));
        then(8, () -> shot(pre + "team"));
        closeAll();
        menu(() -> first);
        then(1, () -> {
            tab(2);
            selectTank(tankIds.get(5));
        });
        then(8, () -> shot(pre + "tanks_who"));
        closeAll();
        // two more tanks: the friend is recommended (chat), the tab Friend shines
        then(1, () -> server(p -> {
            for (int i = 7; i < 9; i++) tanks.add(tank(p.level(), base.offset(2 + (i - 7) * 4, 0, 20), p.getUUID()));
            AquaristRecords.PlayerRecord r = AquaristRecords.get(p.level().getServer()).player(p.getUUID());
            if (!AquaristTeam.ready(p.level().getServer(), p.getUUID(), r)) problem("ready with 9 tanks: " + AquaristTeam.conditions(p.level().getServer(), p.getUUID(), r));
            AquaristTeam.offer(p.level().getServer(), p, r);
        }));
        then(1, () -> look(basinA, -4.5, 2.5, -5.5));
        then(20, () -> shot(pre + "chat"));
        menu(() -> first);
        then(8, () -> shot(pre + "friend_offer"));
        then(1, () -> server(p -> {
            if (!AquaristMenu.view(p, first).getCompoundOrEmpty("friend").getBooleanOr("offered", false)) problem("the friend is on offer");
        }));
        // hired: no free home, he waits next to the first; the team is full
        then(1, () -> send(AquaristMenu.FRIEND_HIRE));
        then(20, () -> {});
        then(1, () -> server(p -> {
            AquaristEntity f = AquaristTeam.lastHired;
            if (f == null || !f.isAlive() || !"waiting_home".equals(AquaristMenu.view(p, f).getStringOr("status", ""))) problem("the friend waits for a home: " + f);
            CompoundTag v = AquaristMenu.view(p, first);
            if (v.getIntOr("team_hired", 0) != 3 || v.getListOrEmpty("team").size() != 3) problem("a team of three: " + v.getIntOr("team_hired", 0));
            checkShares(p);
        }));
        then(1, () -> tab(4));
        then(8, () -> shot(pre + "friend_full"));
        then(1, () -> tab(3));
        then(8, () -> shot(pre + "team_full"));
        closeAll();
        if (!once) {
            once = true;
            then(1, () -> look(basinA.offset(10, 0, 6), -4.0, 9.0, -14.0));
            then(30, () -> shot("station"));
        }
    }

    /** No tank of the station is looked after by two aquarists of the team. */
    private void checkShares(ServerPlayer p) {
        ServerLevel level = p.level();
        Set<UUID> seen = new HashSet<>();
        int n = 0;
        for (AquaristEntity a : List.of(first, second)) {
            for (TankBlockEntity c : AquaristWork.tanks(level, a)) {
                n++;
                if (!seen.add(c.getTank().id())) problem("a tank with two aquarists: " + c.getBlockPos());
            }
        }
        if (n != tanks.size()) problem("every tank has one aquarist: " + n + " of " + tanks.size());
    }

    // ------------------------------------------------------------------ the scene

    private void setup(ServerPlayer p) {
        ServerLevel level = p.level();
        org.overengineer.bettacraft.gametest.framework.DevTime.set(level, 6000);
        level.getServer().setWeatherParameters(24000, 0, false, false);
        level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME, false, level.getServer());
        level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS, false, level.getServer());
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        base = p.blockPosition().offset(-8, -1, 6);
        for (int x = -4; x <= 30; x++) for (int z = -6; z <= 28; z++) {
            level.setBlock(base.offset(x, -1, z), Blocks.SMOOTH_QUARTZ.defaultBlockState(), 2);
            for (int y = 0; y < 8; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        basinA = base.offset(2, 0, 0);
        basinB = base.offset(22, 0, 0);
        home(level, basinA, Blocks.BED.pick(net.minecraft.world.item.DyeColor.RED));
        home(level, basinB, Blocks.BED.pick(net.minecraft.world.item.DyeColor.BLUE));
        UUID me = p.getUUID();
        // seven tanks: four near A, three near B
        int[][] at = {{0, 8}, {4, 8}, {8, 8}, {0, 14}, {16, 8}, {20, 8}, {24, 8}};
        for (int[] a : at) {
            tanks.add(tank(level, base.offset(a[0], 0, a[1]), me));
            tankIds.add(TankMultiblock.getController(level, tanks.get(tanks.size() - 1)).getTank().id());
        }
    }

    /** A breeding basin with a chest (an emerald a day for a while) and a bed three blocks to the south. */
    private static void home(ServerLevel level, BlockPos basin, net.minecraft.world.level.block.Block bed) {
        level.setBlock(basin, ModBlocks.BREEDING_BASIN.get().defaultBlockState(), 3);
        level.setBlock(basin.east(), Blocks.CHEST.defaultBlockState(), 3);
        IItemHandler chest = org.overengineer.bettacraft.platform.Platform.get().itemHandler(level, basin.east(), null);
        if (chest != null) chest.insertItem(0, new ItemStack(Items.EMERALD, 30), false);
        BlockState foot = bed.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.FOOT);
        level.setBlock(basin.offset(0, 0, 3), foot, 3);
        level.setBlock(basin.offset(0, 0, 4), foot.setValue(BedBlock.PART, BedPart.HEAD), 3);
    }

    private static BlockPos tank(ServerLevel level, BlockPos min, UUID owner) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) for (int x = 0; x < 3; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlock(pos, ModBlocks.TANK.get().defaultBlockState(), 3);
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, 12_000, 0F);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setOwner(owner);
        c.getTank().setWaterTemp(26F);
        c.getTank().setTargetTemp(26F);
        c.getTank().setWaterQuality(0.92F);
        AquaristRecords.get(level.getServer()).noteTank(level, c);
        return min;
    }

    private void setupTeam(ServerPlayer p) {
        ServerLevel level = p.level();
        first = AquaristArrival.spawnHiredHomeless(level, p.getUUID(), p.getGameProfile().name(), Vec3.atCenterOf(basinA.offset(1, 0, 1)));
        first.setCustomName(net.minecraft.network.chat.Component.literal("Lena"));
        if (!first.setHome(level, basinA, basinA.offset(0, 0, 4))) problem("the first found no home: " + first.workplace() + " " + first.bed());
        second = AquaristTeam.spawnMember(level, p.getUUID(), p.getGameProfile().name(), Vec3.atCenterOf(basinB.offset(-1, 0, 1)));
        second.setCustomName(net.minecraft.network.chat.Component.literal("Finn"));
        if (!second.setHome(level, basinB, basinB.offset(0, 0, 4))) problem("the second found no home: " + second.workplace() + " " + second.bed());
        for (AquaristEntity a : List.of(first, second)) {
            a.alwaysDay = true;
            a.setNoAi(true);
        }
    }

    /**
     * Each language starts from the same state: two hired (Lena with the player for 12 days, Finn for 9), seven tanks, the
     * conditions of the third all met but the tanks; the friend of the first language gone again.
     */
    private void reset(ServerPlayer p, boolean firstLanguage) {
        ServerLevel level = p.level();
        AquaristRecords.PlayerRecord r = AquaristRecords.get(p.level().getServer()).player(p.getUUID());
        if (!firstLanguage) {
            AquaristEntity f = AquaristTeam.lastHired;
            if (f != null && f.isAlive() && f != first && f != second) {
                AquaristArrival.leave(level, f, "dismissed");
                f.discard();
            }
            while (tanks.size() > 7) {
                BlockPos t = tanks.remove(tanks.size() - 1);
                for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) for (int x = 0; x < 3; x++) level.setBlock(t.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        long now = AquaristTeam.now(p.level().getServer());
        for (AquaristRecords.Member m : r.team) {
            boolean lena = first != null && m.id.equals(first.getUUID());
            m.since = now - (lena ? 12 : 9) * 24000L;
            m.paidSince = m.since;
            m.trouble = Long.MIN_VALUE;
        }
        r.lastHire = now - 9 * 24000L;
        r.breedings = r.breedingsAtHire + 2;
        r.friendOffered = false;
        r.friendNotBefore = Long.MIN_VALUE;
        AquaristRecords.get(p.level().getServer()).setDirty();
        p.teleportTo(level, basinA.getX() + 4.5, basinA.getY() + 0.2, basinA.getZ() - 1.5, java.util.Set.of(), 140F, 10F, false);
    }

    // ------------------------------------------------------------------ helpers

    private void menu(java.util.function.Supplier<AquaristEntity> who) {
        then(1, () -> server(p -> {
            AquaristEntity a = who.get();
            p.teleportTo(p.level(), a.getX() - 1.5, a.getY() + 0.3, a.getZ() - 2.5, java.util.Set.of(), -30F, 15F, false);
        }));
        then(8, () -> {});
        then(1, () -> server(p -> AquaristMenu.open(p, who.get())));
        then(20, () -> {
            if (!(mc.gui.screen() instanceof org.overengineer.bettacraft.client.screen.AquaristScreen)) problem("no aquarist menu: " + mc.gui.screen());
        });
    }

    private void tab(int index) {
        call(mc.gui.screen(), "showTabForTest", new Class<?>[]{int.class}, index);
    }

    /** Selects the tank on the tab Tanks. */
    private void selectTank(UUID id) {
        if (mc.gui.screen() instanceof org.overengineer.bettacraft.client.screen.AquaristScreen s) s.selectTankForTest(id);
        else problem("no aquarist menu to select a tank");
    }

    private void send(int action) {
        if (mc.gui.screen() instanceof org.overengineer.bettacraft.client.screen.AquaristScreen s) s.sendForTest(action);
        else problem("no aquarist menu for action " + action);
    }

    private void closeAll() {
        then(1, () -> server(ServerPlayer::closeContainer));
        then(3, () -> mc.gui.setScreen(null));
    }

    private void language(String code) {
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
        if (mc.gui.screen() != null) mc.gui.screen().resize(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private void scale(int s) {
        mc.options.guiScale().set(s);
        mc.resizeGui();
    }

    private static void call(Object target, String name, Class<?>[] types, Object... args) {
        if (target == null) return;
        try {
            target.getClass().getMethod(name, types).invoke(target, args);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("[smoketest] M69 {} failed: {}", name, e.toString());
        }
    }

    private void problem(String what) {
        problems++;
        LOGGER.error("[smoketest] M69 FAIL: {}", what);
    }

    private void look(BlockPos at, double dx, double dy, double dz) {
        Vec3 c = Vec3.atCenterOf(at);
        Vec3 eye = c.add(dx, dy, dz);
        Vec3 d = c.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
        server(p -> p.teleportTo(p.level(), eye.x, eye.y - p.getEyeHeight(), eye.z, java.util.Set.of(), yaw, pitch, false));
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    boolean tick() {
        if (mc.player != null && !steps.isEmpty()) {
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.PauseScreen) mc.gui.setScreen(null);
        if (steps.isEmpty()) return true;
        Step st = steps.peek();
        if (wait < st.delay()) {
            wait++;
            return false;
        }
        steps.poll();
        wait = 0;
        st.action().run();
        return steps.isEmpty();
    }

    private void server(Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        Screenshot.grab(mc.gameDirectory, "bettacraft_m69_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, c -> {});
    }
}
