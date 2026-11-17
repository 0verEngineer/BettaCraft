package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.monitor.DashboardLayout;
import org.overengineer.bettacraft.monitor.DashboardTile;
import org.overengineer.bettacraft.monitor.MonitorBlock;
import org.overengineer.bettacraft.monitor.MonitorBlockEntity;
import org.overengineer.bettacraft.monitor.MonitorValue;
import org.overengineer.bettacraft.monitor.TankHistory;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M27 part a client smoke ({@code -Psmoketestm27}): a 3 x 2 monitor wall and a single monitor between two named tanks,
 * with a day of history recorded beforehand. Screenshots {@code bettacraft_m27_wall} (the screens with the automatic
 * layout: temperature with its graph and water quality per tank), {@code _wall_de} and {@code _single} (close up).
 * Part b: {@code _dashboard} / {@code _dashboard_de} (a hand-made dashboard with every widget type, the alarm triggered) and
 * {@code _editor} (the editor with the graph picked).
 * Checks (errors in the log): the screen formed, the client got the data of both tanks, the dashboard and the detail.
 */
final class DevClientSmokeM27 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final BlockPos base;
    private final String lang = Minecraft.getInstance().options.languageCode;
    private int wait;

    DevClientSmokeM27() {
        base = mc.player.blockPosition().offset(-10, 0, 800);
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(1, () -> look(base.getX() + 1.5, base.getY() + 2.2, base.getZ() - 5.5, 0F, 8F));
        then(120, this::check);
        then(1, () -> shot("wall"));
        then(1, () -> quickLanguage("de_de"));
        then(20, () -> shot("wall_de"));
        then(1, () -> quickLanguage(lang));
        then(1, () -> look(base.getX() - 3.5, base.getY() + 1.6, base.getZ() - 1.3, 0F, 10F));
        then(40, () -> shot("single"));
        // part b: a hand-made dashboard with every widget on the wall, then the editor
        then(1, () -> server(this::dashboard));
        then(1, () -> look(base.getX() + 1.5, base.getY() + 2.0, base.getZ() - 3.2, 0F, 6F));
        then(140, this::checkDashboard);
        then(1, () -> shot("dashboard"));
        then(1, () -> quickLanguage("de_de"));
        then(20, () -> shot("dashboard_de"));
        then(1, () -> quickLanguage(lang));
        then(1, () -> mc.options.hideGui = false);
        then(1, this::openEditor);
        then(20, () -> shot("editor"));
        then(1, () -> mc.setScreen(null));
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        for (int x = -10; x <= 10; x++)
            for (int z = -8; z <= 8; z++) {
                level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
                for (int y = 0; y < 6; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
        TankBlockEntity a = tank(level, p, base.offset(4, 0, 1), 2, 2, 2, "Betta Nano", 26.5F);
        TankBlockEntity b = tank(level, p, base.offset(-7, 0, 1), 3, 2, 2, "Reef", 25.0F);
        b.getTank().setSalinity(35F);
        b.markTankChanged();
        // a day of history: the temperature swings with the day
        long hour = TankHistory.hour(level.getGameTime());
        TankHistory h = TankHistory.get(level);
        for (TankBlockEntity t : new TankBlockEntity[]{a, b}) {
            float[] v = MonitorValue.sampleAll(level, t);
            for (int i = 0; i < 24; i++) {
                float[] pt = v.clone();
                pt[MonitorValue.TEMPERATURE.ordinal()] = v[MonitorValue.TEMPERATURE.ordinal()] + (float) Math.sin(i / 24.0 * Math.PI * 2) * 1.2F;
                h.record(t.getTank().id(), hour - 24 + i, pt);
            }
        }
        // the wall: 3 x 2, facing north (to the camera), top left (as seen) at x + 2
        var s = ModBlocks.MONITOR.get().defaultBlockState().setValue(MonitorBlock.FACING, Direction.NORTH);
        for (int y = 2; y >= 1; y--) for (int x = 2; x >= 0; x--) level.setBlockAndUpdate(base.offset(x, y, 1), s);
        level.setBlockAndUpdate(base.offset(-4, 1, 1), s);
        for (int x = 0; x <= 2; x++) level.setBlockAndUpdate(base.offset(x, 0, 1), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        level.setBlockAndUpdate(base.offset(-4, 0, 1), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        level.setBlockAndUpdate(base.offset(0, 4, -2), Blocks.LIGHT.defaultBlockState());
    }

    /** Every widget type on the 3 x 2 wall (12 x 8 cells); the alarm (Reef above 24 °C) is triggered and blinks. */
    private void dashboard(ServerPlayer p) {
        ServerLevel level = p.level();
        BlockPos tl = base.offset(2, 2, 1);
        if (!(level.getBlockEntity(tl) instanceof MonitorBlockEntity c)) return;
        java.util.UUID nano = null, reef = null;
        for (var k : c.tanks()) {
            if ("Betta Nano".equals(k.name)) nano = k.id;
            if ("Reef".equals(k.name)) reef = k.id;
        }
        if (level.getBlockEntity(base.offset(4, 0, 1)) instanceof TankBlockEntity a && a.isController()) {
            var err = org.overengineer.bettacraft.tank.TankInhabitants.insert(level, a, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TROPICAL_FISH_BUCKET),
                    net.minecraft.world.entity.EntityType.TROPICAL_FISH, p, false);
            if (err != null) LOGGER.warn("[smoketest] M27 tropical fish: {}", err.getString());
            org.overengineer.bettacraft.tank.TankCare.log(level, a, "log.bettacraft.critters_born", "6", "@" + org.overengineer.bettacraft.critters.CritterSpecies.values()[0].translationKey());
        }
        DashboardLayout l = new DashboardLayout();
        l.auto = false;
        l.tiles.add(new DashboardTile(l.nextId++, 0, 0, 4, 4, DashboardTile.STATUS, nano));
        l.tiles.add(DashboardTile.graph(l.nextId++, 4, 0, 4, 4, nano, 1, MonitorValue.TEMPERATURE, MonitorValue.WATER_QUALITY));
        l.tiles.add(DashboardTile.alarm(l.nextId++, 8, 0, 4, 2, reef, MonitorValue.TEMPERATURE, false, 24F, true));
        DashboardTile v = DashboardTile.value(l.nextId++, 8, 2, 4, 2, reef, MonitorValue.SALINITY, false);
        v.background = 0xFF0E2A5A;
        l.tiles.add(v);
        l.tiles.add(new DashboardTile(l.nextId++, 0, 4, 4, 4, DashboardTile.ANIMALS, nano));
        l.tiles.add(new DashboardTile(l.nextId++, 4, 4, 4, 4, DashboardTile.DEVICES, reef));
        l.tiles.add(new DashboardTile(l.nextId++, 8, 4, 4, 4, DashboardTile.LOG, null));
        c.setLayout(l);
    }

    private void checkDashboard() {
        if (!(mc.level.getBlockEntity(base.offset(2, 2, 1)) instanceof MonitorBlockEntity c)) return;
        if (c.layout().tiles.size() != 7 || c.layout().auto) LOGGER.error("[smoketest] M27 the dashboard did not reach the client: {} tiles", c.layout().tiles.size());
        int detail = 0;
        for (var k : c.clientTanks) if (!k.detail.isEmpty()) detail++;
        if (detail != 2) LOGGER.error("[smoketest] M27 {} tanks with detail instead of 2", detail);
        LOGGER.info("[smoketest] M27 dashboard: {} tiles, {} tanks with detail, {} history lines", c.layout().tiles.size(), detail, c.clientHistory.size());
    }

    private void openEditor() {
        var screen = new org.overengineer.bettacraft.client.screen.MonitorEditorScreen(base.offset(2, 2, 1));
        mc.setScreen(screen);
        if (screen.editing() == null) {
            LOGGER.error("[smoketest] M27 the editor has no layout");
            return;
        }
        screen.select(screen.editing().tiles.get(1).id); // the graph
    }

    private void check() {
        BlockPos tl = base.offset(2, 2, 1);
        if (!(mc.level.getBlockEntity(tl) instanceof MonitorBlockEntity c)) {
            LOGGER.error("[smoketest] M27 no monitor at the top left");
            return;
        }
        if (!c.isController() || c.width() != 3 || c.height() != 2) LOGGER.error("[smoketest] M27 the wall is not one 3x2 screen: {}x{}", c.width(), c.height());
        if (c.clientTanks.size() != 2) LOGGER.error("[smoketest] M27 the client has {} tanks instead of 2", c.clientTanks.size());
        LOGGER.info("[smoketest] M27 screen {}x{}, {} tiles, {} tanks, {} history lines, status {}", c.width(), c.height(), c.layout().tiles.size(),
                c.clientTanks.size(), c.clientHistory.size(), c.status());
    }

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz, String name, float temp) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, (long) sx * sy * sz * 900, 0F);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setName(name);
        c.getTank().setSignVisible(true);
        c.getTank().setOwner(p.getUUID());
        c.getTank().setWaterTemp(temp);
        c.getTank().setTargetTemp(temp);
        c.getTank().setWaterQuality(0.86F);
        c.markTankChanged();
        return c;
    }

    // ------------------------------------------------------------------ helpers

    private void quickLanguage(String code) {
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
        if (mc.screen != null) mc.screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.level(), x, eyeY - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, false));
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    boolean tick() {
        if (mc.player != null && !steps.isEmpty()) {
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (steps.isEmpty()) return true;
        Step s = steps.peek();
        if (wait < s.delay()) {
            wait++;
            return false;
        }
        steps.poll();
        wait = 0;
        s.action().run();
        return steps.isEmpty();
    }

    private void server(java.util.function.Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToasts().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m27_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
