package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.client.map.AlarmWaypoints;
import org.overengineer.bettacraft.client.screen.TankScreen;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
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
import org.overengineer.bettacraft.tank.TankUsers;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.menu.TankMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M53 client smoke ({@code -Psmoketestm53}, NeoForge and Fabric, background window; parts by
 * {@code -Pm53parts=users,maps,interior,animals,critters}, default all):
 * <ul>
 *     <li>users: the users tab en/de, the overview with the gear, the client config the gear opens;</li>
 *     <li>maps: a monitor alarm puts the waypoint into every installed map mod ({@code -Pmaps}: JourneyMap, Xaero's,
 *     FTB Chunks), PASS per mod, then it goes away;</li>
 *     <li>interior: goes into a tank's interior and looks to the four sides, up and down; the edges of the picture must
 *     not be nearly black (the 26.x "black interior"), PASS / FAIL per picture;</li>
 *     <li>animals: every mob of Bettacraft in the sky, in groups, from the front, the back, both sides, above and below;</li>
 *     <li>critters: every small animal species on the floor of a tank, from four sides and above.</li>
 * </ul>
 * Screenshots {@code bettacraft_m53_*.png}; log lines "[smoketest] M53 ...".
 */
final class DevClientSmokeM53 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final Set<String> parts;
    private int wait;
    private BlockPos base;
    private TankBlockEntity tank;
    private MonitorBlockEntity monitor;
    private final UUID alice = UUID.nameUUIDFromBytes("m53-alice".getBytes()), bob = UUID.nameUUIDFromBytes("m53-bob".getBytes());
    private final List<Entity> shown = new ArrayList<>();
    private int interiorFails, interiorShots;
    /** A scene of an earlier milestone running inside this one (render part: M11, M13). */
    private java.util.function.BooleanSupplier sub;

    DevClientSmokeM53() {
        String p = System.getProperty("bettacraft.smoketest.m53parts", "users,maps,interior,animals,critters");
        parts = Set.of(p.toLowerCase(Locale.ROOT).split(","));
        LOGGER.info("[smoketest] M53 parts {}", parts);
        logRenderSetup();
        then(1, () -> server(this::build));
        then(20, () -> {});
        if (parts.contains("render")) render();
        if (parts.contains("users")) users();
        if (parts.contains("maps")) maps();
        if (parts.contains("interior")) interior();
        if (parts.contains("animals")) animals();
        if (parts.contains("critters")) critters();
        then(5, () -> LOGGER.info("[smoketest] M53 done: interior {} of {} pictures too dark", interiorFails, interiorShots));
    }

    // ------------------------------------------------------------------ set-up

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        org.overengineer.bettacraft.gametest.framework.DevTime.set(level, 6000);
        base = p.blockPosition().offset(0, 0, 12);
        for (int x = -4; x <= 6; x++) for (int z = -3; z <= 4; z++) level.setBlock(base.offset(x, -1, z), Blocks.STONE_BRICKS.defaultBlockState(), 2);
        List<BlockPos> blocks = new ArrayList<>();
        for (int x = 0; x < 3; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) blocks.add(base.offset(x, y, z));
        for (BlockPos q : blocks) level.setBlock(q, ModBlocks.TANK.get().defaultBlockState(), 2);
        TankData t = TankMultiblock.formAll(level, blocks);
        tank = TankMultiblock.getController(level, base);
        t.setFront(Direction.NORTH);
        t.setOwner(p.getUUID());
        t.setName("M53 tank");
        t.setWaterTemp(30F);
        t.setTargetTemp(30F);
        t.users().put(alice, "Alice", TankUsers.Role.CARETAKER);
        t.users().put(bob, "Bob", TankUsers.Role.VIEWER);
        t.users().setWaypoint(alice, t.owner(), true);
        TankInteractions.changeWater(level, tank, t.capacityMb());
        tank.markTankChanged();
        BlockPos m = base.offset(-2, 0, 0);
        level.setBlock(m, ModBlocks.MONITOR.get().defaultBlockState().setValue(MonitorBlock.FACING, Direction.NORTH), 3);
        monitor = (MonitorBlockEntity) level.getBlockEntity(m);
        LOGGER.info("[smoketest] M53 tank at {}", tank.getBlockPos());
    }

    // ------------------------------------------------------------------ render matrix: M11 (styles, glass, water, menus), M13 (plants, interior, miniature), frame rate

    private void render() {
        then(1, () -> {
            LOGGER.info("[smoketest] M53 render: M11 scene");
            DevClientSmokeM11 s11 = new DevClientSmokeM11();
            sub = s11::tick;
        });
        then(1, () -> {
            LOGGER.info("[smoketest] M53 render: M13 scene");
            DevClientSmokeM13 s13 = new DevClientSmokeM13();
            sub = s13::tick;
        });
        then(1, () -> {
            if (mc.gui.screen() != null) mc.gui.setScreen(null);
            if (mc.player != null && mc.level != null && !mc.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) server(InteriorManager::leave);
        });
        then(40, () -> look(base.getX() + 1.5, base.getY() + 1.4, base.getZ() - 3.0, 0F, 10F));
        int[] samples = new int[5];
        for (int i = 0; i < samples.length; i++) {
            int k = i;
            then(i == 0 ? 60 : 20, () -> samples[k] = mc.getFps());
        }
        then(1, () -> {
            double sum = 0;
            for (int f : samples) sum += f;
            LOGGER.info("[smoketest] M53 fps at the tank: {} (samples {})", Math.round(sum / samples.length), java.util.Arrays.toString(samples));
            shot("render_fps_view");
        });
    }

    // ------------------------------------------------------------------ users tab, gear, client config

    private void users() {
        for (String lang : new String[]{"en_us", "de_de"}) {
            then(1, () -> language(lang));
            // the menu stays open only near the tank (stillValid)
            then(40, () -> look(base.getX() + 1.5, base.getY() + 1.2, base.getZ() - 2.5, 0F, 10F));
            then(5, () -> openMenu());
            then(30, () -> {
                if (mc.gui.screen() instanceof TankScreen ts) ts.selectTabForTest(0);
            });
            then(20, () -> shot("overview_" + lang));
            then(1, () -> {
                if (mc.gui.screen() instanceof TankScreen ts) {
                    ts.selectTabForTest(4);
                    ts.selectUserForTest(bob);
                }
            });
            then(20, () -> {
                boolean ok = mc.gui.screen() instanceof TankScreen ts && ts.tabButtonsForTest().stream().allMatch(b -> b.getWidth() > 0);
                LOGGER.info("[smoketest] M53 users tab {} {}", lang, ok ? "PASS" : "FAIL");
                shot("users_" + lang);
            });
            then(1, () -> {
                boolean ok = mc.gui.screen() instanceof TankScreen ts && reopenOverview(ts) && ts.openClientConfigForTest();
                LOGGER.info("[smoketest] M53 gear opens the client config {}: {}", ok ? "PASS" : "FAIL", mc.gui.screen() == null ? "-" : mc.gui.screen().getClass().getSimpleName());
            });
            then(30, () -> shot("client_config_" + lang));
            then(1, () -> {
                if (mc.gui.screen() != null) mc.gui.screen().onClose();
            });
            then(20, () -> LOGGER.info("[smoketest] M53 back from the config: {}", mc.gui.screen() == null ? "-" : mc.gui.screen().getClass().getSimpleName()));
            then(1, () -> server(ServerPlayer::closeContainer));
            then(5, () -> mc.gui.setScreen(null));
        }
        then(1, () -> language("en_us"));
        then(40, () -> {});
    }

    private static boolean reopenOverview(TankScreen ts) {
        ts.selectTabForTest(0);
        return true;
    }

    private void openMenu() {
        server(p -> org.overengineer.bettacraft.platform.Platform.get().openMenu(p,
                new SimpleMenuProvider((id, inv, pl) -> new TankMenu(id, inv, tank), Component.translatable("gui.bettacraft.tank.title")),
                buf -> buf.writeBlockPos(tank.getBlockPos())));
    }

    // ------------------------------------------------------------------ map mods

    private void maps() {
        then(1, () -> server(p -> {
            DashboardLayout l = new DashboardLayout();
            l.auto = false;
            l.tiles.add(DashboardTile.alarm(l.nextId++, 0, 0, 8, 5, tank.getTank().id(), MonitorValue.TEMPERATURE, false, 28F, false));
            monitor.setLayout(l);
            MonitorSync.cycle(p.level(), monitor);
        }));
        then(40, () -> look(base.getX() + 0.5, base.getY() + 4, base.getZ() - 25, 0F, 6F));
        then(60, () -> {
            UUID id = tank.getTank().id();
            for (String mod : new String[]{"journeymap", "xaero", "ftbchunks"}) {
                boolean there = AlarmWaypoints.available(mod);
                boolean ok = !there || AlarmWaypoints.shownIn(id).contains(mod);
                LOGGER.info("[smoketest] M53 waypoint in {} {} (installed {})", mod, ok ? "PASS" : "FAIL", there);
            }
            shot("waypoint");
        });
        then(1, () -> server(p -> {
            tank.getTank().setWaterTemp(24F);
            MonitorSync.cycle(p.level(), monitor);
        }));
        then(20, () -> LOGGER.info("[smoketest] M53 waypoint removed {}", AlarmWaypoints.shown().isEmpty() ? "PASS" : "FAIL " + AlarmWaypoints.shown()));
    }

    // ------------------------------------------------------------------ interior (26.x: black walls and fog)

    private void interior() {
        then(1, () -> server(p -> InteriorManager.enter(p, tank.getBlockPos())));
        then(200, () -> LOGGER.info("[smoketest] M53 interior: in {}", mc.level == null ? "-" : mc.level.dimension().identifier()));
        float[][] views = {{0F, 5F}, {90F, 5F}, {180F, 5F}, {270F, 5F}, {0F, -60F}, {0F, 60F}};
        String[] names = {"south", "west", "north", "east", "up", "down"};
        for (int i = 0; i < views.length; i++) {
            float[] v = views[i];
            String n = names[i];
            then(1, () -> {
                if (mc.player == null) return;
                mc.player.setYRot(v[0]);
                mc.player.setXRot(v[1]);
                mc.player.yRotO = v[0];
                mc.player.xRotO = v[1];
            });
            then(20, () -> checkInterior(n));
        }
        then(1, () -> server(p -> InteriorManager.leave(p)));
        then(60, () -> {});
    }

    /** Mean brightness of the left, right and top edges of the picture (not the hotbar); nearly black = FAIL. */
    private void checkInterior(String name) {
        shot("interior_" + name);
        try {
            Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), 1, img -> {
                try (img) {
                    analyse(name, img);
                }
            });
        } catch (Throwable t) {
            LOGGER.warn("[smoketest] M53 interior {}: no pixel check ({})", name, t.toString());
        }
    }

    private void analyse(String name, com.mojang.blaze3d.platform.NativeImage img) {
        int w = img.getWidth(), h = img.getHeight();
        double[] sums = new double[3];
        int[] counts = new int[3];
        for (int y = h / 10; y < h * 7 / 10; y += 2) {
            for (int x = 0; x < w; x += 2) {
                int band = x < w / 8 ? 0 : x >= w * 7 / 8 ? 1 : y < h / 5 ? 2 : -1;
                if (band < 0) continue;
                int c = img.getPixel(x, y); // ARGB
                double lum = 0.299 * ((c >> 16) & 0xFF) + 0.587 * ((c >> 8) & 0xFF) + 0.114 * (c & 0xFF);
                sums[band] += lum;
                counts[band]++;
            }
        }
        double left = sums[0] / Math.max(1, counts[0]), right = sums[1] / Math.max(1, counts[1]), top = sums[2] / Math.max(1, counts[2]);
        boolean ok = Math.min(left, Math.min(right, top)) > 40;
        interiorShots++;
        if (!ok) interiorFails++;
        LOGGER.info("[smoketest] M53 interior {} {}: edges left {} right {} top {}", name, ok ? "PASS" : "FAIL",
                Math.round(left), Math.round(right), Math.round(top));
    }

    // ------------------------------------------------------------------ every mob from six sides

    private void animals() {
        List<EntityType<?>> types = new ArrayList<>();
        for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
            if (BuiltInRegistries.ENTITY_TYPE.getKey(t).getNamespace().equals(BettaCraft.MOD_ID)) types.add(t);
        }
        types.sort(Comparator.comparingDouble((EntityType<?> t) -> t.getWidth()).thenComparing(t -> BuiltInRegistries.ENTITY_TYPE.getKey(t).getPath()));
        List<List<EntityType<?>>> groups = new ArrayList<>();
        List<EntityType<?>> cur = new ArrayList<>();
        double width = 0;
        for (EntityType<?> t : types) {
            double w = Math.max(0.45, t.getWidth() * 1.3);
            if (!cur.isEmpty()) { // one animal per picture: the small ones fill it
                groups.add(cur);
                cur = new ArrayList<>();
                width = 0;
            }
            cur.add(t);
            width += w;
        }
        if (!cur.isEmpty()) groups.add(cur);
        LOGGER.info("[smoketest] M53 animals: {} types in {} groups", types.size(), groups.size());
        then(1, () -> {
            hideHud(true);
            mc.options.fov().set(30);
        });
        for (int g = 0; g < groups.size(); g++) {
            List<EntityType<?>> group = groups.get(g);
            int gi = g;
            double[] span = new double[1];
            then(1, () -> server(p -> span[0] = spawnGroup(p, group, gi)));
            then(30, () -> {});
            String[] sides = {"yaw0", "yaw90", "yaw180", "yaw270"};
            float[] yaws = {0F, 90F, 180F, 270F};
            for (int s = 0; s < 4; s++) {
                float yaw = yaws[s];
                String side = sides[s];
                then(1, () -> server(p -> turn(yaw)));
                // the camera south of the row, looking north; the animals face it at yaw 0 (south)
                then(1, () -> look(centre().x, centre().y + 0.3, centre().z + dist(span[0]), 180F, 8F));
                then(25, () -> shot("animals_" + gi + "_" + side));
            }
            then(1, () -> server(p -> turn(0F)));
            then(1, () -> look(centre().x, centre().y + dist(span[0]) + 0.5, centre().z + 0.01, 180F, 89.9F));
            then(25, () -> shot("animals_" + gi + "_top"));
            then(1, () -> look(centre().x, centre().y - dist(span[0]) - 2.0, centre().z + 0.01, 180F, -89.9F));
            then(25, () -> shot("animals_" + gi + "_bottom"));
            then(1, () -> {
                StringBuilder b = new StringBuilder();
                for (Entity e : shown) b.append(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath()).append(e.isAlive() ? "" : "(dead)").append(' ');
                LOGGER.info("[smoketest] M53 animals group {}: {}", gi, b.toString().trim());
            });
            then(1, () -> server(p -> clearShown()));
        }
        then(1, () -> {
            hideHud(false);
            mc.options.fov().set(70);
        });
    }

    private Vec3 groupCentre = Vec3.ZERO;

    private Vec3 centre() {
        return groupCentre;
    }

    private static double dist(double span) {
        // field of view 30 while the animals are shown: the picture is 0.71 times the distance wide
        return Math.max(0.55, span * 1.1 / 0.71 + 0.2);
    }

    private double spawnGroup(ServerPlayer p, List<EntityType<?>> group, int index) {
        ServerLevel level = p.level();
        clearShown();
        double y = base.getY() + 40;
        double x = base.getX() + 0.5, z = base.getZ() + 0.5 + 40;
        double total = 0;
        for (EntityType<?> t : group) total += Math.max(t.getHeight(), t.getWidth()) * 2.0;
        double at = x - total / 2;
        for (EntityType<?> t : group) {
            double w = Math.max(0.45, t.getWidth() * 1.3);
            Entity e;
            try {
                e = t.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            } catch (Throwable ex) {
                LOGGER.info("[smoketest] M53 animals: {} cannot be made ({})", BuiltInRegistries.ENTITY_TYPE.getKey(t), ex.toString());
                at += w;
                continue;
            }
            if (!(e instanceof Mob m)) {
                if (e != null) e.discard();
                at += w;
                continue;
            }
            m.snapTo(at + w / 2, y - t.getHeight() / 2, z, 0F, 0F);
            m.setAirSupply(m.getMaxAirSupply()); // 26.3: no setInvulnerable; the air is refreshed at every turn
            m.setPersistenceRequired();
            SmokeFreeze.freeze(m, true);
            level.addFreshEntity(m);
            shown.add(m);
            at += w;
        }
        groupCentre = new Vec3(x, y, z);
        return total;
    }

    private void turn(float yaw) {
        for (Entity e : shown) {
            e.setYRot(yaw);
            e.setYHeadRot(yaw);
            if (e instanceof Mob m) {
                m.setYBodyRot(yaw);
                m.setAirSupply(m.getMaxAirSupply());
            }
        }
    }

    private void clearShown() {
        for (Entity e : shown) e.discard();
        shown.clear();
    }

    // ------------------------------------------------------------------ the small animals in a tank

    private void critters() {
        then(1, () -> server(p -> {
            ServerLevel level = p.level();
            TankData t = tank.getTank();
            t.critters().clear();
            double bx = base.getX(), bz = base.getZ();
            int i = 0;
            for (CritterSpecies s : CritterSpecies.values()) {
                double x = bx + 0.3 + (i % 7) * 0.4, zz = bz + 0.3 + (i / 7) * 0.45;
                Critter k = new Critter(t.nextCritterId(), s, s.hasSexes(), CritterGenes.WILD, x, t.min().getY() + 0.05, zz);
                k.age = s.juvenileDays() + 2;
                k.yaw = 0F;
                k.idle = 1_000_000;
                t.critters().add(k);
                i++;
            }
            tank.markTankChanged();
            TankCritters.sync(level, tank);
            LOGGER.info("[smoketest] M53 critters: {} species", i);
        }));
        then(1, () -> hideHud(true));
        String[] names = {"front", "back", "left", "right", "top"};
        double[][] at = {{0, 0, -2.3, 0, 15}, {0, 0, 2.3, 180, 15}, {-2.8, 0, 0, 270, 15}, {2.8, 0, 0, 90, 15}, {0, 2.0, 0.01, 180, 89.9}};
        for (int i = 0; i < names.length; i++) {
            double[] a = at[i];
            String n = names[i];
            then(i == 0 ? 30 : 1, () -> look(base.getX() + 1.5 + a[0], base.getY() + 0.6 + a[1], base.getZ() + 1.0 + a[2], (float) a[3], (float) a[4]));
            then(30, () -> shot("critters_" + n));
        }
        then(1, () -> hideHud(false));
    }

    // ------------------------------------------------------------------ helpers

    /** 26.2: the HUD is hidden by its own toggle (F1). */
    private void hideHud(boolean hide) {
        if (mc.gui.hud.isHidden() != hide) mc.gui.hud.toggle();
    }

    /** The render matrix: which render and performance mods run, and whether a shader pack is really in use (Iris API). */
    private void logRenderSetup() {
        StringBuilder mods = new StringBuilder();
        for (String id : new String[]{"sodium", "embeddium", "iris", "oculus", "sodium_extra", "immediatelyfast", "entityculling", "modernfix", "ferritecore",
                "distanthorizons", "moreculling", "lambdynamiclights", "notenoughanimations", "entity_model_features", "entity_texture_features", "polytone",
                "fusion", "continuity", "ctm", "vulkanmod", "journeymap", "xaerominimap", "xaeroworldmap", "ftbchunks"}) {
            if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded(id)) mods.append(id).append(' ');
        }
        LOGGER.info("[smoketest] M53 render mods: {}", mods.toString().trim());
        try {
            Object api = Class.forName("net.irisshaders.iris.api.v0.IrisApi").getMethod("getInstance").invoke(null);
            Object inUse = api.getClass().getMethod("isShaderPackInUse").invoke(api);
            LOGGER.info("[smoketest] M53 shader pack in use: {}", inUse);
        } catch (Throwable t) {
            LOGGER.info("[smoketest] M53 shader pack in use: no Iris ({})", t.getClass().getSimpleName());
        }
    }

    private void language(String code) {
        if (code.equals(mc.options.languageCode)) return;
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.reloadResourcePacks();
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
        // a map mod may open a screen of its own on the first start
        if (mc.gui.screen() != null && mc.gui.screen().getClass().getName().toLowerCase(Locale.ROOT).matches(".*(journeymap|xaero|ftb).*")) mc.gui.setScreen(null);
        if (sub != null) {
            if (sub.getAsBoolean()) sub = null;
            return false;
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
        mc.gui.toastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m53_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
