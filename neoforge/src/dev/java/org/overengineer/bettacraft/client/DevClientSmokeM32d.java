package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.client.map.AlarmWaypoints;
import org.overengineer.bettacraft.client.map.JourneyMapWaypoints;
import org.overengineer.bettacraft.monitor.DashboardLayout;
import org.overengineer.bettacraft.monitor.DashboardTile;
import org.overengineer.bettacraft.monitor.MonitorBlock;
import org.overengineer.bettacraft.monitor.MonitorBlockEntity;
import org.overengineer.bettacraft.monitor.MonitorSync;
import org.overengineer.bettacraft.monitor.MonitorValue;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M32 part d client smoke ({@code -Pmaps -Psmoketestm32d}): a tank at 30 °C with a monitor whose alarm tile watches its
 * temperature; the alarm puts a JourneyMap waypoint at the tank (screenshot {@code bettacraft_m32d_waypoint} from 30
 * blocks away: the beacon and the minimap), then the water cools and the waypoint goes away (log lines
 * "[smoketest] M32d waypoint ..." with PASS / FAIL).
 */
final class DevClientSmokeM32d {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private BlockPos base;
    private TankBlockEntity tank;
    private MonitorBlockEntity monitor;

    DevClientSmokeM32d() {
        then(1, () -> server(this::build));
        then(10, () -> server(p -> MonitorSync.cycle(p.level(), monitor)));
        then(40, () -> look(base.getX() + 0.5, base.getY() + 4, base.getZ() - 30, 0F, 6F));
        then(10, this::checkShown);
        then(140, () -> shot("waypoint"));
        then(1, () -> server(p -> {
            tank.getTank().setWaterTemp(24F);
            MonitorSync.cycle(p.level(), monitor);
        }));
        then(20, this::checkGone);
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        org.overengineer.bettacraft.gametest.framework.DevTime.set(level, 6000);
        base = p.blockPosition().offset(0, 0, 40);
        for (int x = -3; x <= 4; x++) for (int z = -2; z <= 3; z++) level.setBlock(base.offset(x, -1, z), Blocks.STONE_BRICKS.defaultBlockState(), 2);
        List<BlockPos> blocks = List.of(base, base.east(), base.above(), base.above().east());
        for (BlockPos q : blocks) level.setBlock(q, ModBlocks.TANK.get().defaultBlockState(), 2);
        TankData t = TankMultiblock.formAll(level, blocks);
        tank = TankMultiblock.getController(level, base);
        t.setFront(Direction.NORTH);
        t.setOwner(p.getUUID());
        t.setName("Show tank");
        t.setWaterTemp(30F);
        t.setTargetTemp(30F);
        TankInteractions.changeWater(level, tank, t.capacityMb());
        tank.markTankChanged();
        BlockPos m = base.offset(-2, 0, 0);
        level.setBlock(m, ModBlocks.MONITOR.get().defaultBlockState().setValue(MonitorBlock.FACING, Direction.NORTH), 3);
        monitor = (MonitorBlockEntity) level.getBlockEntity(m);
        DashboardLayout l = new DashboardLayout();
        l.auto = false;
        l.tiles.add(DashboardTile.alarm(l.nextId++, 0, 0, 8, 5, t.id(), MonitorValue.TEMPERATURE, false, 28F, false));
        monitor.setLayout(l);
        LOGGER.info("[smoketest] M32d tank at {} with an alarm monitor; JourneyMap {}", tank.getBlockPos(),
                AlarmWaypoints.available("journeymap") ? "found" : "MISSING (-Pmaps)");
    }

    private void checkShown() {
        boolean ok = AlarmWaypoints.shown().size() == 1;
        LOGGER.info("[smoketest] M32d waypoint shown {}: {}", ok ? "PASS" : "FAIL", AlarmWaypoints.shown());
    }

    private void checkGone() {
        boolean ok = AlarmWaypoints.shown().isEmpty();
        LOGGER.info("[smoketest] M32d waypoint removed {}: {}", ok ? "PASS" : "FAIL", AlarmWaypoints.shown());
    }

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.level().getServer().overworld(), x, eyeY - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, true));
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    boolean tick() {
        if (mc.player != null && !steps.isEmpty()) {
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        // JourneyMap may open a screen of its own on the first start
        if (mc.screen != null) mc.setScreen(null);
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
        mc.getToastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m32d_" + name + ".png", mc.getMainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
