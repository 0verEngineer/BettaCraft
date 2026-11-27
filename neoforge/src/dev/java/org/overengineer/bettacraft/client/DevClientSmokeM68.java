package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.command.DevShowcaseM68;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.plumbing.PipeBlock;
import org.overengineer.bettacraft.plumbing.PipeRing;
import org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M68 client smoke ({@code -Psmoketestm68}, NeoForge and Fabric, background window): the two showcase rows of the one pipe system
 * ({@link DevShowcaseM68}: the same stations as in the showcase world) built in the smoke world, plus a copy of the controller at a
 * single tank without its intake guard and with young shrimp (the warnings).
 * <p>
 * World pictures (the GUI hidden): pipes joining in every direction, the wrench caps between two lines, the rings of the roles,
 * the short circuit, glass lines, a bundle with two lines of one role, a bundle at the glass with a dock per line inside and an
 * interior bundle (full size inside and the miniature), the controller against a tank (its ports, the drain, the sensor with its
 * lamp), the sensor alone (the lamp lit), the controller at a multiblock filter with three tanks, the technical room. Menus, en and de:
 * the filter of the short circuit (its warning) and of the glass lines (what they carry), the controller of the technical room and
 * the one that sucks in young, the tank menu of that tank (water, devices and breeding tab); an overlay picture on a glass pipe.
 * Checks (fail the smoke): every station works ({@link DevShowcaseM68#PROBLEMS}), the rings on the client (blue, red, green), the
 * lamp of the sensor alone lit, the menus open, the controller without a guard warns.
 * Screenshots {@code run/screenshots/bettacraft_m68_<shot>.png}, menus {@code bettacraft_m68_<lang>_<shot>.png}.
 */
final class DevClientSmokeM68 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final Map<String, BlockPos> at = new HashMap<>();
    private volatile boolean built;
    private boolean started;
    private int wait;
    private int problems;
    private String lang;
    private int guiScale;
    private ServerConfig.PowerCosts costs;

    DevClientSmokeM68() {
        then(1, () -> {
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            lang = mc.options.languageCode;
            guiScale = mc.options.guiScale().get();
            server(this::build);
        });
        then(100, () -> {
            mc.options.hideGui = true;
            mc.options.fovEffectScale().set(0.0);
            checkClient();
        });
        // ---- the pipe row
        world("pipes_every_direction", () -> c("junction").add(2, -0.2, 0), new Vec3(0.2, 0.9, -3.4));
        world("pipe_wrench_caps", () -> c("wrench_pipe").add(0.5, 0.5, 0), new Vec3(0.9, 0.5, -1.7));
        world("rings_intake_return", () -> c("wrench_tank").add(2.5, 0.6, 0.5), new Vec3(1.8, 2.0, -4.6));
        world("short_circuit", () -> c("short_tank").add(2.5, 0.6, 0.5), new Vec3(1.8, 2.0, -4.6));
        world("short_circuit_close", () -> c("short_pipe").add(0.5, 0.5, 0), new Vec3(0.9, 0.5, -1.7));
        world("glass_lines", () -> c("glass_tank").add(2.5, 0.6, 0.5), new Vec3(1.8, 2.0, -4.6));
        world("glass_lines_close", () -> c("glass_pipe").add(0.5, 0.5, 0), new Vec3(0.9, 0.5, -1.7));
        world("bundle_same_role", () -> c("same_bundle"), new Vec3(2.0, 2.0, -2.6));
        world("bundle_same_role_wide", () -> c("same_bundle").add(1.0, 0.3, -0.5), new Vec3(1.5, 3.5, -5.5));
        world("bundle_at_glass", () -> c("lines_bundle"), new Vec3(1.7, 1.0, -2.0));
        world("bundle_at_glass_wide", () -> c("lines_bundle").add(1.5, 0.5, 0), new Vec3(1.0, 2.6, -5.0));
        inside("lines_inside_docks", "lines_dock", new Vec3(-3.5, 1.5, -4.0), new Vec3(-0.5, -0.5, 1.5));
        inside("lines_inside_bundle", "lines_run", new Vec3(-1.0, 3.5, 6.5), new Vec3(0.5, 0, 0));
        // the same from above, the substrate behind it (the handbook picture of the interior bundle)
        inside("lines_inside_above", "lines_run", new Vec3(-2.0, 9.0, 5.0), new Vec3(1.0, -1.0, 0));
        inside("lines_inside_above_steep", "lines_run", new Vec3(0.5, 12.0, 3.0), new Vec3(0.5, 0, 0));
        world("lines_miniature", () -> c("lines_tank").add(2.2, 1.05, -0.05), new Vec3(-0.2, 0.7, -0.15));
        // ---- the water row
        world("changer_at_tank", () -> c("contact_changer").add(-0.5, 0, 0), new Vec3(1.6, 2.6, -4.8));
        world("changer_ports", () -> c("contact_changer"), new Vec3(0.9, 1.5, -2.1));
        world("changer_drain", () -> c("contact_drain"), new Vec3(0.9, 1.6, -1.7));
        world("sensor_lamp", () -> c("contact_sensor").add(0, 0.5, 0), new Vec3(-0.6, 0.6, -2.3));
        world("sensor_alone", () -> c("alone_sensor").add(0.7, 0.4, 0.5), new Vec3(0.2, 1.4, -3.6));
        world("changer_at_filter", () -> c("filter_tank1").add(4.5, 0.6, 1.5), new Vec3(1.0, 6.5, -10.5));
        world("changer_at_filter_back", () -> c("filter_changer").add(-5.0, 0.5, 0.5), new Vec3(3.0, 5.0, 6.5));
        world("changer_at_filter_close", () -> c("filter_changer"), new Vec3(1.2, 1.4, -2.2));
        world("technical_room", () -> c("room_changer").add(-4.0, 0.5, 0.5), new Vec3(3.5, 5.5, -9.0));
        world("technical_room_back", () -> c("room_changer").add(-4.0, 0.5, 0.5), new Vec3(4.0, 5.0, 7.5));
        world("technical_room_close", () -> c("room_changer").add(0.8, 0.2, -0.2), new Vec3(0.6, 1.4, -2.6));
        // ---- an overlay (Jade/WTHIT/TOP, when one is there) on a glass pipe
        then(1, () -> {
            mc.options.hideGui = false;
            scale(2);
        });
        world("overlay_glass_pipe", () -> c("glass_pipe").add(0, 1, 0), new Vec3(0.0, 0.25, -1.6));
        world("overlay_short_pipe", () -> c("short_pipe").add(0, 1, 0), new Vec3(0.0, 0.25, -1.6));
        // ---- menus, en and de
        for (String code : java.util.List.of("en_us", "de_de")) {
            then(1, () -> {
                language(code);
                scale(2);
                mc.setScreen(null);
            });
            menu(code, "filter_short_circuit", "short_head", true);
            menu(code, "filter_glass", "glass_head", true);
            menu(code, "changer_room", "room_changer", false);
            menu(code, "changer_at_filter", "filter_changer", false);
            menu(code, "changer_young", "young_changer", false);
            near(() -> at.get("young_tank"));
            then(1, () -> openTank(at.get("young_tank")));
            then(25, () -> {
                if (!(mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen)) problem("no tank screen");
            });
            for (int[] tab : new int[][] {{5, 0}, {7, 1}, {2, 2}}) {
                String[] names = {"tank_water", "tank_devices", "tank_breeding"};
                then(1, () -> {
                    if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.selectTabForTest(tab[0]);
                });
                then(15, () -> shot(code + "_" + names[tab[1]]));
            }
            closeAll();
        }
        then(1, () -> {
            language(lang);
            scale(guiScale);
            server(p -> ServerConfig.POWER_COSTS.set(costs));
        });
        then(5, () -> LOGGER.info("[smoketest] M68 done: {} problem(s)", problems));
    }

    int problems() {
        return problems;
    }

    // ------------------------------------------------------------------ the scene

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        costs = ServerConfig.POWER_COSTS.get();
        ServerConfig.POWER_COSTS.set(ServerConfig.PowerCosts.OFF);
        level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME, false, level.getServer());
        DevClientWorldTour.clearWeather(level);
        org.overengineer.bettacraft.gametest.framework.DevTime.set(level, 6000);
        p.getAbilities().flying = true;
        BlockPos base = p.blockPosition().offset(12, 1, 24);
        clear(level, base.offset(-8, 0, -5), 100, 12);
        clear(level, base.offset(-8, 0, 13), 110, 12);
        DevShowcaseM68.PROBLEMS.clear();
        at.putAll(DevShowcaseM68.pipeRow(level, base));
        Map<String, BlockPos> water = DevShowcaseM68.waterRow(level, base.offset(0, 0, 18));
        at.putAll(water);
        DevShowcaseM68.contact(level, water.get("end").offset(2, 0, 0), false, true, at, "young");
        // the warning pictures without the cabinet's motor filter (it warns about the young as well)
        if (level.getBlockEntity(at.get("young_tank").below()) instanceof org.overengineer.bettacraft.equipment.CabinetBlockEntity cab) {
            cab.items().setStackInSlot(org.overengineer.bettacraft.equipment.CabinetBlockEntity.FILTER_SLOT, net.minecraft.world.item.ItemStack.EMPTY);
            cab.setChanged();
        }
        TankBlockEntity yt = TankMultiblock.getController(level, at.get("young_tank"));
        if (yt != null) org.overengineer.bettacraft.tank.TankEquipment.invalidate(yt);
        for (String problem : DevShowcaseM68.PROBLEMS) problem("station: " + problem);
        if (level.getBlockEntity(at.get("young_changer")) instanceof WaterChangerBlockEntity w && !w.flag(WaterChangerBlockEntity.YOUNG))
            problem("the controller without a guard does not warn");
        LOGGER.info("[smoketest] M68 scene at {}: {}", base, at);
        built = true;
    }

    /** Air from y-1 up, smooth stone at y-2 (the showcase's ground), over x 0..sx, z 0..sz from {@code min}. */
    private static void clear(ServerLevel level, BlockPos min, int sx, int sz) {
        for (int x = 0; x < sx; x++)
            for (int z = 0; z < sz; z++) {
                level.setBlock(min.offset(x, -2, z), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                for (int y = -1; y < 12; y++) {
                    BlockPos q = min.offset(x, y, z);
                    if (!level.getBlockState(q).isAir()) level.setBlock(q, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
    }

    private void checkClient() {
        var l = mc.level;
        if (l == null) return;
        ring("wrench_pipe", PipeRing.INTAKE);
        ring("short_pipe", PipeRing.SHORT);
        BlockPos fresh = at.get("contact_changer") == null ? null : at.get("contact_changer").south();
        if (fresh != null && l.getBlockState(fresh).hasProperty(PipeBlock.RING) && l.getBlockState(fresh).getValue(PipeBlock.RING) != PipeRing.FRESH)
            problem("the fresh water pipe shows " + l.getBlockState(fresh).getValue(PipeBlock.RING));
        BlockPos lamp = at.get("alone_sensor") == null ? null : at.get("alone_sensor").above();
        if (lamp == null || !l.getBlockState(lamp).hasProperty(RedstoneLampBlock.LIT) || !l.getBlockState(lamp).getValue(RedstoneLampBlock.LIT))
            problem("the lamp of the sensor alone is not lit: " + (lamp == null ? null : l.getBlockState(lamp)));
    }

    private void ring(String key, PipeRing want) {
        BlockPos q = at.get(key);
        var s = q == null ? null : mc.level.getBlockState(q);
        if (s == null || !s.hasProperty(PipeBlock.RING) || s.getValue(PipeBlock.RING) != want) problem(key + " shows " + s + ", " + want + " wanted");
    }

    // ------------------------------------------------------------------ pictures

    private Vec3 c(String key) {
        BlockPos q = at.get(key);
        if (q == null) {
            problem("no position " + key);
            return Vec3.ZERO;
        }
        return Vec3.atCenterOf(q);
    }

    /** A picture of the overworld: the camera at {@code target + offset}, looking at the target. */
    private void world(String name, Supplier<Vec3> target, Vec3 offset) {
        then(1, () -> {
            Vec3 t = target.get();
            lookAt(t.add(offset), t, false);
        });
        then(50, () -> shot(name));
    }

    /** A picture inside the interior: the camera at {@code pos + eye}, looking at {@code pos + to} ({@code pos} an interior position). */
    private void inside(String name, String key, Vec3 eye, Vec3 to) {
        then(1, () -> {
            Vec3 t = c(key);
            lookAt(t.add(eye), t.add(to), true);
        });
        then(80, () -> shot(name));
    }

    private void menu(String code, String name, String key, boolean filter) {
        near(() -> at.get(key));
        then(1, () -> {
            if (filter) org.overengineer.bettacraft.util.ClientHooks.openFilter(at.get(key));
            else org.overengineer.bettacraft.util.ClientHooks.openWaterChanger(at.get(key));
        });
        then(20, () -> {
            boolean ok = filter ? mc.screen instanceof org.overengineer.bettacraft.client.screen.MultiblockFilterScreen
                    : mc.screen instanceof org.overengineer.bettacraft.client.screen.WaterChangerScreen;
            if (!ok) problem("no " + (filter ? "filter" : "controller") + " screen at " + key + ": " + mc.screen);
        });
        then(5, () -> shot(code + "_" + name));
        then(1, () -> mc.setScreen(null));
    }

    private void near(Supplier<BlockPos> target) {
        then(1, () -> {
            Vec3 t = Vec3.atCenterOf(target.get());
            lookAt(t.add(0.5, 2.5, -3.5), t, false);
        });
        then(10, () -> {});
    }

    private void closeAll() {
        then(1, () -> server(ServerPlayer::closeContainer));
        then(3, () -> mc.setScreen(null));
    }

    private void openTank(BlockPos tankPos) {
        server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.level(), tankPos);
            if (c == null) {
                LOGGER.error("[smoketest] M68 FAIL: no tank at {}", tankPos);
                return;
            }
            org.overengineer.bettacraft.platform.Platform.get().openMenu(p, new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        });
    }

    // ------------------------------------------------------------------ helpers

    private void language(String code) {
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
        if (mc.screen != null) mc.screen.resize(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private void scale(int s) {
        mc.options.guiScale().set(s);
        mc.resizeGui();
    }

    private void problem(String what) {
        problems++;
        LOGGER.error("[smoketest] M68 FAIL: {}", what);
    }

    private void lookAt(Vec3 eye, Vec3 target, boolean interior) {
        Vec3 d = target.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
        server(p -> {
            ServerLevel to = interior ? InteriorManager.interiorLevel(p.level().getServer()) : p.level().getServer().overworld();
            p.teleportTo(to, eye.x, eye.y - p.getEyeHeight(), eye.z, java.util.Set.of(), yaw, pitch, false);
        });
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    /** Returns true when all steps are done. */
    boolean tick() {
        if (mc.player != null && !steps.isEmpty()) {
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (steps.isEmpty()) return true;
        if (started && !built) return false;
        Step st = steps.peek();
        if (wait < st.delay()) {
            wait++;
            return false;
        }
        steps.poll();
        wait = 0;
        started = true;
        st.action().run();
        return steps.isEmpty();
    }

    private void server(Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m68_" + name + ".png", mc.getMainRenderTarget(), 1, c -> {});
    }
}
