package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.overengineer.bettacraft.client.interior.PlantMeshRenderer;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantPart;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M61 client smoke ({@code -Psmoketestm61}, NeoForge and Fabric, background window): block changes reported by other mods
 * without their states must not break the client (the NPE of 1.43 in {@code PlantMeshRenderer.onBlockChanged}: Modern
 * Dynamics calls {@code sendBlockUpdated(pos, null, null, 0)} while the client reads a pipe's block entity data, the packet
 * failed and players could not join).
 * <ul>
 *     <li>calls: {@code ClientLevel.sendBlockUpdated} with null for both states, for the old one and for the new one, next to
 *     stone and to a real (meshed) plant block;</li>
 *     <li>pipes (with Modern Dynamics, {@code -Ppipes}): fluid pipes between a reservoir and a tank of ours; their block
 *     entity data arrives on the client (the remesh with null states, counted by
 *     {@link PlantMeshRenderer#statelessChanges}), then the player leaves the world and joins it again (the chunk data of the
 *     pipes once more). Without Modern Dynamics the rejoin still runs.</li>
 * </ul>
 * Every "Failed to handle packet" of the client log is counted (a log4j appender); the scene fails on any. Log lines
 * "[smoketest] M61 ...", screenshots {@code bettacraft_m61_*.png}.
 */
final class DevClientSmokeM61 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static final Identifier FLUID_PIPE = Identifier.fromNamespaceAndPath("moderndynamics", "fluid_pipe");
    /** Set by {@code -Ppipes}: Modern Dynamics must be loaded. */
    private static final boolean PIPES_REQUIRED = Boolean.getBoolean("bettacraft.smoketest.m61pipes");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final AtomicInteger failedPackets = new AtomicInteger();
    /** The first failed packet (logged from the tick: an appender must not log itself). */
    private volatile String firstFailure;
    private boolean started, finished;
    private int wait;
    private BooleanSupplier until;
    private int problems;
    private volatile BlockPos home;
    private volatile BlockPos stone, plant;
    private volatile BlockPos[] pipes;
    private volatile BlockState plantState;
    private int statelessBefore;
    private String worldId;
    /** Between leaving and joining again (the scene does not tick then; a tick hook of its own answers the backup screen). */
    private volatile boolean rejoining;
    /** The backup screen already answered (the world loads on in the next ticks while it still shows: press it once). */
    private Object answered;

    DevClientSmokeM61() {
        watchPacketErrors();
        org.overengineer.bettacraft.platform.client.ClientEvents.TICK_POST.add(this::watch);
        then(1, () -> {
            started = true;
            server(this::setup);
        });
        then(40, this::calls);
        boolean md = BuiltInRegistries.BLOCK.containsKey(FLUID_PIPE);
        LOGGER.info("[smoketest] M61 Modern Dynamics {}", md ? "loaded" : "not loaded");
        if (!md && PIPES_REQUIRED) problem("Modern Dynamics is not loaded although -Ppipes asked for it");
        if (md) {
            then(1, () -> statelessBefore = PlantMeshRenderer.statelessChanges);
            then(1, () -> server(this::buildPipes));
            then(60, () -> look(home.offset(0, 0, 5), 0, 4, -6));
            then(40, this::checkPipes);
            then(5, () -> shot("pipes"));
        }
        then(20, () -> statelessBefore = PlantMeshRenderer.statelessChanges);
        then(1, this::rejoin);
        // the world opens again: wait for the player, then for the chunks with the pipes
        then(1, () -> until = () -> mc.player != null && mc.level != null && mc.getSingleplayerServer() != null);
        then(1, () -> {
            rejoining = false;
            LOGGER.info("[smoketest] M61 joined the world {} again", worldId);
        });
        then(100, () -> {
            if (md) {
                look(home.offset(0, 0, 5), 0, 4, -6);
                checkPipes();
            }
        });
        then(20, () -> shot("rejoined"));
        then(20, this::finish);
    }

    private void finish() {
        if (finished) return;
        finished = true;
        int failed = failedPackets.get();
        if (failed > 0) problem(failed + " x \"Failed to handle packet\" in the client log, the first: " + firstFailure);
        else LOGGER.info("[smoketest] M61 no failed packets PASS");
        LOGGER.info("[smoketest] M61 done: {} problem(s) ({} block changes without states seen)", problems, PlantMeshRenderer.statelessChanges);
    }

    // ------------------------------------------------------------------ the scene

    private void setup(ServerPlayer p) {
        ServerLevel level = p.level();
        org.overengineer.bettacraft.gametest.framework.DevTime.set(level, 6000);
        home = p.blockPosition();
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        stone = home.offset(-3, 0, 4);
        plant = home.offset(-5, 0, 4);
        level.setBlock(stone, Blocks.STONE.defaultBlockState(), 3);
        // a meshed plant block in the world (no neighbour updates: it would not survive on grass)
        plantState = InteriorPlants.state(PlantSpecies.ROTALA, PlantPart.ROOT, 0);
        level.setBlock(plant, plantState, 2 | 16);
    }

    /** The calls of foreign mods on the client: both states null, the old one, the new one; next to stone and to a plant. */
    private void calls() {
        var level = mc.level;
        if (level == null) {
            problem("calls: no client level");
            return;
        }
        BlockState s = level.getBlockState(stone), pl = level.getBlockState(plant);
        if (!s.is(Blocks.STONE)) problem("calls: the stone did not arrive on the client (" + s + ")");
        if (pl.getBlock() != plantState.getBlock()) problem("calls: the plant did not arrive on the client (" + pl + ")");
        int before = PlantMeshRenderer.statelessChanges;
        call("both null (Modern Dynamics)", () -> level.sendBlockUpdated(stone, null, null, 0));
        call("old null, stone", () -> level.sendBlockUpdated(stone, null, s, 3));
        call("stone, new null", () -> level.sendBlockUpdated(stone, s, null, 3));
        call("both null at the plant", () -> level.sendBlockUpdated(plant, null, null, 0));
        call("old null, plant", () -> level.sendBlockUpdated(plant, null, pl, 3));
        call("plant, new null", () -> level.sendBlockUpdated(plant, pl, null, 3));
        call("old null, plant again", () -> level.sendBlockUpdated(plant, null, pl, 3)); // the root is known again
        int seen = PlantMeshRenderer.statelessChanges - before;
        if (seen != 7) problem("calls: " + seen + " of 7 block changes without states reached the plant meshes (the mixin?)");
        else LOGGER.info("[smoketest] M61 calls: 7 block changes without states reached the plant meshes PASS");
    }

    private void call(String name, Runnable r) {
        try {
            r.run();
            LOGGER.info("[smoketest] M61 sendBlockUpdated {} PASS", name);
        } catch (RuntimeException e) {
            problem("sendBlockUpdated " + name + ": " + e);
            LOGGER.error("[smoketest] M61 stack trace", e);
        }
    }

    /** Modern Dynamics fluid pipes from a reservoir of ours to a tank block of ours (both have a fluid handler). */
    private void buildPipes(ServerPlayer p) {
        ServerLevel level = p.level();
        Optional<Block> pipe = BuiltInRegistries.BLOCK.getOptional(FLUID_PIPE);
        if (pipe.isEmpty()) return;
        BlockPos start = home.offset(-2, 0, 6);
        for (int x = -1; x <= 6; x++) for (int z = -1; z <= 1; z++) level.setBlock(start.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
        level.setBlock(start, ModBlocks.RESERVOIR.get().defaultBlockState(), 3);
        BlockPos[] ps = new BlockPos[4];
        for (int i = 0; i < ps.length; i++) {
            ps[i] = start.offset(1 + i, 0, 0);
            level.setBlock(ps[i], pipe.get().defaultBlockState(), 3);
        }
        level.setBlock(start.offset(5, 0, 0), ModBlocks.TANK.get().defaultBlockState(), 3);
        pipes = ps;
        LOGGER.info("[smoketest] M61 pipes: {} fluid pipes between a reservoir and a tank at {}", ps.length, start);
    }

    private void checkPipes() {
        var level = mc.level;
        BlockPos[] ps = pipes;
        if (level == null || ps == null) {
            problem("pipes: nothing to check (" + (level == null ? "no client level" : "no pipes") + ")");
            return;
        }
        int found = 0;
        for (BlockPos pos : ps) {
            if (level.getBlockEntity(pos) != null && FLUID_PIPE.equals(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()))) found++;
        }
        if (found != ps.length) problem("pipes: " + found + " of " + ps.length + " pipe block entities on the client");
        int seen = PlantMeshRenderer.statelessChanges - statelessBefore;
        // Modern Dynamics re-meshes a pipe on the client with null states whenever its data says so (placed, loaded, changed)
        if (seen <= 0) problem("pipes: Modern Dynamics sent no block change without states (the scene did not reach the case)");
        else LOGGER.info("[smoketest] M61 pipes: {} pipe block entities on the client, {} block changes without states PASS", found, seen);
        statelessBefore = PlantMeshRenderer.statelessChanges;
    }

    /** Leave the world and open it again (as a player who joins again: every chunk and block entity arrives anew). */
    private void rejoin() {
        var server = mc.getSingleplayerServer();
        if (server == null || mc.level == null) {
            problem("rejoin: not in a singleplayer world");
            return;
        }
        worldId = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString();
        LOGGER.info("[smoketest] M61 leaving the world {} to join it again", worldId);
        rejoining = true;
        mc.disconnectFromWorld(net.minecraft.client.multiplayer.ClientLevel.DEFAULT_QUIT_MESSAGE);
        mc.setScreen(new TitleScreen());
        mc.createWorldOpenFlows().openWorld(worldId, () -> {
            problem("rejoin: the world " + worldId + " did not open again");
            mc.setScreen(new TitleScreen());
        });
    }

    /**
     * A world with a data pack dimension (our tank interior) counts as experimental: the client asks for a backup before it
     * opens it again. The scene answers "skip the backup" (as the showcase tour does).
     */
    private void watch() {
        if (started && !rejoining && !finished && mc.level == null) {
            // thrown out of the world (a failed packet disconnects the client: "Network Protocol Error")
            problem("the client left the world" + (mc.screen == null ? "" : " (screen " + mc.screen.getClass().getSimpleName() + ": "
                    + mc.screen.getTitle().getString() + ")"));
            finish();
            LOGGER.info("[smoketest] client world test finished (M61 only)");
            mc.stop();
            return;
        }
        if (!rejoining || !(mc.screen instanceof net.minecraft.client.gui.screens.BackupConfirmScreen
                || mc.screen instanceof net.minecraft.client.gui.screens.ConfirmScreen)) return;
        if (mc.screen == answered) return;
        String skip = Component.translatable("selectWorld.backupJoinSkipButton").getString();
        net.minecraft.client.gui.components.Button pick = null;
        for (var w : mc.screen.children()) {
            if (!(w instanceof net.minecraft.client.gui.components.Button b) || !b.isActive()) continue;
            if (pick == null || b.getMessage().getString().equals(skip)) pick = b;
        }
        if (pick != null) {
            LOGGER.info("[smoketest] M61 {}: pressing {}", mc.screen.getClass().getSimpleName(), pick.getMessage().getString());
            answered = mc.screen;
            pick.onPress(new net.minecraft.client.input.MouseButtonInfo(0, 0));
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Counts the client's "Failed to handle packet" errors (ClientCommonPacketListenerImpl.onPacketError). */
    private void watchPacketErrors() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        AbstractAppender appender = new AbstractAppender("bettacraft-m61-packets", null, null, true, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                String m = event.getMessage() == null ? "" : event.getMessage().getFormattedMessage();
                if (m.startsWith("Failed to handle packet") && failedPackets.getAndIncrement() == 0) {
                    firstFailure = m + (event.getThrown() == null ? "" : " - " + event.getThrown());
                }
            }
        };
        appender.start();
        ctx.getConfiguration().getRootLogger().addAppender(appender, org.apache.logging.log4j.Level.ERROR, null);
        ctx.updateLoggers();
    }

    int problems() {
        return problems;
    }

    private void problem(String what) {
        problems++;
        LOGGER.error("[smoketest] M61 FAIL: {}", what);
    }

    private void look(BlockPos at, double dx, double dy, double dz) {
        if (mc.player == null) return;
        Vec3 eye = Vec3.atCenterOf(at).add(dx, dy, dz);
        mc.player.setPos(eye.x, eye.y - mc.player.getEyeHeight(), eye.z);
        Vec3 d = Vec3.atCenterOf(at).subtract(eye);
        mc.player.setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
        mc.player.setXRot((float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
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
        if (until != null) {
            if (!until.getAsBoolean()) return false;
            until = null;
        }
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m61_" + name + ".png", mc.getMainRenderTarget(), 1, c -> {});
    }
}
