package org.overengineer.bettacraft.client;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.platform.client.ClientEvents;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M63b: the real-world tour ({@code gradlew runClient -Psmoketestworldtour}, NeoForge and Fabric, background window; the rule
 * "Rendering-Änderungen: Abnahme in einer echten Welt"). Gradle copies a world (default: the showcase, else this branch's upgrade
 * reference world) to {@code saves/Bettacraft Rundgang} and deletes the copy afterwards; the tour opens it, finds every tank
 * within {@code bettacraft.tour.radius} chunks of the spawn and, for every shader pack of {@code bettacraft.tour.packs} (Iris;
 * "none" = without shaders), takes each tank from the front, the side, above and from just below its water surface, then
 * 360 degree sweeps (with sky) at the corners of the tank area and above its middle. Pictures:
 * {@code tour/<tag>/screenshots/<pack>/<tank or sweep>_<view>.png} (same names in every run, so two runs, e.g. this version
 * and the last release, compare picture by picture: {@code py tools/tour/compare.py}).
 * <p>
 * Guards (fail the run): every sweep view is taken again without the miniatures; outside the screen boxes of the tanks both
 * must be the same (no miniature geometry outside its tank, nothing of it in the sky); the top faces of the tanks carry no
 * glass (an open tank must not look closed). With {@code bettacraft.tour.baseline} (a run of an old release) the guards are
 * only logged.
 */
final class DevClientWorldTour {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    static final String WORLD = System.getProperty("bettacraft.tour.world", "Bettacraft Rundgang");
    private static final String TAG = System.getProperty("bettacraft.tour.tag", "run");
    private static final String PACKS = System.getProperty("bettacraft.tour.packs", "none,ComplementaryReimagined_r5.10.zip,BSL_v10.1.8.zip");
    private static final String VIEWS = System.getProperty("bettacraft.tour.views", "front,side,top,below");
    private static final int RADIUS = Integer.getInteger("bettacraft.tour.radius", 16);
    private static final int LIMIT = Integer.getInteger("bettacraft.tour.limit", Integer.MAX_VALUE);
    private static final boolean BASELINE = Boolean.getBoolean("bettacraft.tour.baseline");
    /** Ticks after a teleport before the picture (the client follows the server position, the frame is drawn). */
    private static final int SETTLE = Integer.getInteger("bettacraft.tour.settle", 4);
    private static final int WIDTH = 960, HEIGHT = 540;
    /** The world's ticks stand still during the tour ({@code bettacraft.tour.freeze}, default on): the animals do not move. */
    private static final boolean FREEZE = !"false".equals(System.getProperty("bettacraft.tour.freeze"));

    /** {@code atTarget}: the step waits until the client's camera stands where the last {@link #look} put it (at most 60 ticks). */
    private record Step(int delay, Runnable action, boolean atTarget) {
        Step(int delay, Runnable action) {
            this(delay, action, false);
        }
    }

    /** Where the last {@link #look} puts the camera: x, eye y, z, yaw, pitch. */
    private volatile double[] target;
    private int arrivedFor;

    /** A tank: its box (min inclusive, size) and the side its front faces. */
    record Tank(BlockPos min, int sx, int sy, int sz, Direction front, String name) {
        String id() {
            return "t" + min.getX() + "_" + min.getY() + "_" + min.getZ();
        }

        double cx() {
            return min.getX() + sx / 2.0;
        }

        double cz() {
            return min.getZ() + sz / 2.0;
        }

        double top() {
            return min.getY() + sy;
        }
    }

    private static boolean opened;
    private static int waitTicks;
    private static String lastScreen = "";
    private static DevClientWorldTour tour;
    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private int problems;
    private final List<String> summary = new ArrayList<>();
    private volatile List<Tank> tanks = List.of();
    private String oldPack;
    private boolean oldEnabled;
    private File out;

    static void register() {
        ClientEvents.SCREEN_OPENING.add(screen -> {
            if (opened && tour == null) LOGGER.info("[smoketest] world tour: screen {}", screen == null ? "none" : screen.getClass().getName());
            if (opened || !(screen instanceof TitleScreen)) return;
            opened = true;
            Minecraft mc = Minecraft.getInstance();
            if (!mc.getLevelSource().levelExists(WORLD)) {
                LOGGER.error("[smoketest] FAILED: world tour: the copy {} is missing (made by Gradle from the source world)", WORLD);
                mc.stop();
                return;
            }
            LOGGER.info("[smoketest] world tour: opening {}", WORLD);
            mc.createWorldOpenFlows().openWorld(WORLD, () -> {
                LOGGER.error("[smoketest] FAILED: world tour: {} could not be opened", WORLD);
                mc.stop();
            });
        });
        ClientEvents.TICK_POST.add(DevClientWorldTour::clientTick);
    }

    private static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getSingleplayerServer() == null) {
            String cls = mc.screen == null ? "none" : mc.screen.getClass().getSimpleName();
            if (opened && !cls.equals(lastScreen)) {
                lastScreen = cls;
                LOGGER.info("[smoketest] world tour: tick sees screen {}", cls);
            }
            if (opened && ++waitTicks % 100 == 0) {
                StringBuilder b = new StringBuilder();
                if (mc.screen != null) for (var w : mc.screen.children()) if (w instanceof net.minecraft.client.gui.components.AbstractWidget aw) b.append('[').append(aw.getMessage().getString()).append(']');
                LOGGER.info("[smoketest] world tour: waiting for the world, screen {} {}", mc.screen == null ? "none" : mc.screen.getClass().getName(), b);
            }
            confirmScreens(mc);
            return;
        }
        mc.options.pauseOnLostFocus = false;
        // no screen during the tour: the level loading screen of a copied world can stay open, and an open screen pauses the
        // integrated server (the teleports wait)
        if (mc.screen != null) mc.setScreen(null);
        if (tour == null) {
            mc.getWindow().setWindowed(WIDTH, HEIGHT);
            tour = new DevClientWorldTour();
        }
        try {
            if (tour.tick()) {
                tour.finish();
                mc.stop();
            }
        } catch (RuntimeException e) {
            LOGGER.error("[smoketest] FAILED: world tour", e);
            mc.stop();
        }
    }

    /**
     * After a screen got its widgets (NeoForge: ScreenEvent.Init.Post in DevClientHooks; Fabric: ScreenEvents.AFTER_INIT in
     * FabricClientSmoke): the questions before a world opens are answered at once.
     */
    static void afterInit(net.minecraft.client.gui.screens.Screen screen) {
        if (opened && tour == null && (screen instanceof net.minecraft.client.gui.screens.BackupConfirmScreen
                || screen instanceof net.minecraft.client.gui.screens.ConfirmScreen)) confirmScreens(Minecraft.getInstance(), screen);
    }

    /** A world saved by another version or loader: the client asks first (backup, experimental settings): go on without. */
    private static void confirmScreens(Minecraft mc) {
        if (mc.screen != null) confirmScreens(mc, mc.screen);
    }

    /** The last screen answered: Fabric initialises a screen several times (resizes); its answer must run once. */
    private static net.minecraft.client.gui.screens.Screen answered;

    private static void confirmScreens(Minecraft mc, net.minecraft.client.gui.screens.Screen screen) {
        if (!(screen instanceof net.minecraft.client.gui.screens.BackupConfirmScreen || screen instanceof net.minecraft.client.gui.screens.ConfirmScreen)) return;
        if (screen == answered) return;
        String skip = net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinSkipButton").getString();
        String proceed = net.minecraft.network.chat.Component.translatable("gui.proceed").getString();
        String yes = net.minecraft.network.chat.Component.translatable("gui.yes").getString();
        net.minecraft.client.gui.components.Button pick = null;
        StringBuilder all = new StringBuilder();
        for (var w : screen.children()) if (w instanceof net.minecraft.client.gui.components.AbstractWidget aw) all.append('[').append(aw.getMessage().getString()).append(']');
        LOGGER.info("[smoketest] world tour: {} with {}", screen.getClass().getSimpleName(), all);
        for (var w : screen.children()) {
            if (!(w instanceof net.minecraft.client.gui.components.Button b) || !b.isActive()) continue;
            String m = b.getMessage().getString();
            if (m.equals(skip) || m.equals(proceed) || m.equals(yes)) pick = b;
        }
        if (pick != null) {
            LOGGER.info("[smoketest] world tour: pressing {} on {}", pick.getMessage().getString(), screen.getClass().getSimpleName());
            answered = screen;
            press(pick);
        }
    }

    private DevClientWorldTour() {
        out = new File(mc.gameDirectory, "tour/" + TAG);
        then(40, () -> {
            mc.options.hideGui = true;
            mc.options.fovEffectScale().set(0.0);
            mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
        });
        then(1, () -> server(p -> {
            ServerLevel level = p.level();
            level.setWeatherParameters(6000, 0, false, false);
            level.setDayTime(6000);
            gameRules(level);
            clearWeather(level);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            // the animals stand still (the same picture in every run; and the server has less to do)
            if (FREEZE) p.level().getServer().tickRateManager().setFrozen(true);
        }));
        then(1, () -> tanks = findTanks());
        then(1, this::rememberIris);
        then(1, this::plan);
    }

    // ------------------------------------------------------------------ the tour

    private void plan() {
        List<Tank> list = tanks;
        LOGGER.info("[smoketest] world tour: {} tanks within {} chunks of the spawn, packs {}, views {}", list.size(), RADIUS, packs(), VIEWS);
        if (list.isEmpty()) {
            problems++;
            summary.add("FAIL no tanks found");
            return;
        }
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE, ground = Integer.MAX_VALUE;
        for (Tank t : list) {
            minX = Math.min(minX, t.min().getX());
            minZ = Math.min(minZ, t.min().getZ());
            maxX = Math.max(maxX, t.min().getX() + t.sx());
            maxZ = Math.max(maxZ, t.min().getZ() + t.sz());
            ground = Math.min(ground, t.min().getY());
        }
        int x0 = minX, z0 = minZ, x1 = maxX, z1 = maxZ, gy = ground;
        boolean firstPack = true;
        for (String pack : packs()) {
            String pn = shortName(pack);
            boolean first = firstPack;
            firstPack = false;
            then(1, () -> switchPack(pack));
            then(1, () -> look(x0 - 6, gy + 4, z0 - 6, -45F, 10F));
            // shader compile and the rebuild of the chunks and miniatures
            then(240, () -> logPack(pack));
            if (first) then(0, () -> {});
            for (Tank t : list) {
                for (String v : VIEWS.split(",")) {
                    String view = v.trim();
                    double[] cam = clear(camera(t, view), list, t);
                    if (cam == null) continue;
                    then(1, () -> lookAt(cam[0], cam[1], cam[2], new Vec3(cam[3], cam[4], cam[5])));
                    thenAt(SETTLE, () -> shot(pn, t.id() + "_" + view));
                    if (first && view.equals(VIEWS.split(",")[0].trim())) then(0, () -> checkTop(t));
                }
            }
            // 360 degree sweeps (pitch -20: sky; 25: ground) at the four corners outside the tank area and high above its middle
            double[][] spots = {{x0 - 10, gy + 4, z0 - 10}, {x1 + 10, gy + 4, z0 - 10}, {x0 - 10, gy + 4, z1 + 10}, {x1 + 10, gy + 4, z1 + 10},
                    {(x0 + x1) / 2.0, gy + 40, (z0 + z1) / 2.0}};
            for (int s = 0; s < spots.length; s++) {
                double[] sp = spots[s];
                for (int yaw = 0; yaw < 360; yaw += 45) {
                    for (float pitch : new float[]{-20F, 25F}) {
                        String view = "sweep" + s + "_y" + yaw + "_p" + (int) pitch;
                        float fy = yaw;
                        boolean firstAtSpot = yaw == 0 && pitch < 0;
                        then(1, () -> look(sp[0], sp[1], sp[2], fy, pitch));
                        // a new spot: the chunks around it load and fade in first
                        thenAt(firstAtSpot ? 100 : SETTLE + 6, () -> shot(pn, view));
                        if (hideMiniatures(true, true)) {
                            then(1, () -> hideMiniatures(true, false));
                            then(SETTLE, () -> shot(pn, view + "_nomini"));
                            then(1, () -> hideMiniatures(false, false));
                            then(SETTLE, () -> shot(pn, view + "_again"));
                            then(4, () -> miniatureGuard(pn, view, sp[0], sp[1], sp[2], fy, pitch));
                        }
                    }
                }
            }
        }
        then(1, this::openTopGuard);
        then(1, this::restoreIris);
    }

    /** {x, eyeY, z, targetX, targetY, targetZ} of one view of a tank, null when it has no such view. */
    static double[] camera(Tank t, String view) {
        Direction f = t.front() == null || t.front().getAxis() == Direction.Axis.Y ? Direction.NORTH : t.front();
        // width along the front, depth away from it
        int width = f.getAxis() == Direction.Axis.Z ? t.sx() : t.sz(), depth = f.getAxis() == Direction.Axis.Z ? t.sz() : t.sx();
        double cx = t.cx(), cz = t.cz(), cy = t.min().getY() + t.sy() / 2.0;
        // the outer face of the front and the centre of the box
        double fx = cx + f.getStepX() * (depth / 2.0), fz = cz + f.getStepZ() * (depth / 2.0);
        Direction side = f.getClockWise();
        double sideHalf = width / 2.0;
        switch (view) {
            case "front" -> {
                double d = Math.max(width * 0.95, t.sy() * 1.6) + 1.5;
                return new double[]{fx + f.getStepX() * d, cy + 0.4, fz + f.getStepZ() * d, cx, cy, cz};
            }
            case "side" -> {
                // close to the side face (the showcase tanks stand a few blocks apart: farther away the camera is in the next one)
                double d = Math.min(Math.max(depth * 0.9, t.sy() * 1.2) + 1.0, 2.6);
                return new double[]{cx + side.getStepX() * (sideHalf + d), cy + 0.4, cz + side.getStepZ() * (sideHalf + d), cx, cy, cz};
            }
            case "top" -> {
                double d = Math.max(width, depth) * 0.55 + 1.5;
                return new double[]{fx + f.getStepX() * d * 0.6, t.top() + d, fz + f.getStepZ() * d * 0.6, cx, t.top() - 0.2, cz};
            }
            case "below" -> {
                // just below the water surface, outside the front glass, looking slightly up across the surface
                double d = Math.max(1.4, width * 0.25);
                double far = depth / 2.0;
                return new double[]{fx + f.getStepX() * d, t.top() - 0.35, fz + f.getStepZ() * d, cx - f.getStepX() * far, t.top() + 0.15, cz - f.getStepZ() * far};
            }
            default -> {
                return null;
            }
        }
    }

    /** A camera inside another tank (its water would fill the picture) goes up to just above that tank's top. */
    static double[] clear(double[] cam, List<Tank> all, Tank own) {
        if (cam == null) return null;
        for (int pass = 0; pass < 3; pass++) {
            boolean moved = false;
            for (Tank o : all) {
                if (o == own) continue;
                if (cam[0] > o.min().getX() - 0.3 && cam[0] < o.min().getX() + o.sx() + 0.3 && cam[2] > o.min().getZ() - 0.3
                        && cam[2] < o.min().getZ() + o.sz() + 0.3 && cam[1] > o.min().getY() - 0.3 && cam[1] < o.top() + 0.3) {
                    cam[1] = o.top() + 0.6;
                    moved = true;
                }
            }
            if (!moved) break;
        }
        return cam;
    }

    /** The tank controllers in the chunks within {@link #RADIUS} of the spawn (loaded on the server), sorted by position. */
    private List<Tank> findTanks() {
        var server = mc.getSingleplayerServer();
        return server.submit(() -> {
            ServerLevel level = server.overworld();
            BlockPos spawn = spawn(level);
            List<Tank> found = new ArrayList<>();
            int cx = spawn.getX() >> 4, cz = spawn.getZ() >> 4;
            for (int x = cx - RADIUS; x <= cx + RADIUS; x++)
                for (int z = cz - RADIUS; z <= cz + RADIUS; z++) {
                    LevelChunk chunk = level.getChunk(x, z);
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (!(be instanceof TankBlockEntity tb) || !tb.isController() || tb.getTank() == null) continue;
                        TankData t = tb.getTank();
                        found.add(new Tank(t.min(), t.sizeX(), t.sizeY(), t.sizeZ(), t.front(), t.name()));
                    }
                }
            found.sort(Comparator.comparingInt((Tank t) -> t.min().getZ()).thenComparingInt(t -> t.min().getX()).thenComparingInt(t -> t.min().getY()));
            return found.size() > LIMIT ? new ArrayList<>(found.subList(0, LIMIT)) : found;
        }).join();
    }

    // ------------------------------------------------------------------ guards

    private int topBlocks, topsWithGlass, frontsChecked, frontsWithGlass, tanksTopChecked;
    private final List<String> topsBad = new ArrayList<>(), topsMissing = new ArrayList<>();

    /**
     * Guard, per tank while the camera is at it (its chunks are loaded on the client): the top faces carry no glass. The model's
     * quads of the up side of every top block (no tank block above; without block entity data the model draws the textured
     * style with vanilla glass) may only be the edge lines; the front faces must have glass (the check sees glass at all).
     */
    private void checkTop(Tank t) {
        RandomSource rand = RandomSource.create(42);
        Direction f = t.front() == null || t.front().getAxis() == Direction.Axis.Y ? Direction.NORTH : t.front();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int found = 0;
        for (int x = 0; x < t.sx(); x++)
            for (int z = 0; z < t.sz(); z++)
                for (int y = t.sy() - 1; y >= 0; y--) {
                    p.set(t.min().getX() + x, t.min().getY() + y, t.min().getZ() + z);
                    BlockState s = mc.level.getBlockState(p);
                    if (!(s.getBlock() instanceof TankBlock)) continue;
                    found++;
                    if (mc.level.getBlockState(p.above()).getBlock() instanceof TankBlock) break;
                    topBlocks++;
                    if (glassQuads(s, Direction.UP, rand) > 0) {
                        topsWithGlass++;
                        if (topsBad.size() < 5) topsBad.add(t.name() + " " + p.toShortString());
                    }
                    if (!(mc.level.getBlockState(p.relative(f)).getBlock() instanceof TankBlock)) {
                        frontsChecked++;
                        if (glassQuads(s, f, rand) > 0) frontsWithGlass++;
                    }
                    break;
                }
        if (found == 0) {
            if (topsMissing.size() < 5) topsMissing.add(t.id());
        } else {
            tanksTopChecked++;
        }
    }

    private void openTopGuard() {
        boolean ok = topsWithGlass == 0 && topsMissing.isEmpty() && (frontsChecked == 0 || frontsWithGlass > 0);
        guard(ok, String.format(Locale.ROOT, "open tops: %d top blocks of %d tanks (%d not loaded %s), %d with glass on top %s; front faces with glass %d of %d",
                topBlocks, tanksTopChecked, tanks.size() - tanksTopChecked, topsMissing, topsWithGlass, topsBad, frontsWithGlass, frontsChecked));
    }

    /** The quads of one side of a tank block state that are not the edge lines (the glass). */
    private int glassQuads(BlockState s, Direction side, RandomSource rand) {
        int n = 0;
        for (var q : modelQuads(s, side, rand)) {
            if (!spriteName(q).contains("tank_edge")) n++;
        }
        return n;
    }

    /** Our model's quads of one side of a tank block state, all layers, without block entity data. */
    private List<net.minecraft.client.renderer.block.model.BakedQuad> modelQuads(BlockState s, Direction side, RandomSource rand) {
        // the loader's adapter (NeoDynamicModel, FabricDynamicModel) holds our model: dynamic() on every version and loader
        Object model = mc.getBlockRenderer().getBlockModel(s);
        org.overengineer.bettacraft.platform.client.DynamicModel dm;
        try {
            dm = (org.overengineer.bettacraft.platform.client.DynamicModel) model.getClass().getMethod("dynamic").invoke(model);
        } catch (NoSuchMethodException e) {
            // 1.21.1: the block state's model wraps ours (a multipart model of the tank's states): its quads without render data
            return org.overengineer.bettacraft.platform.client.ClientPlatform.get().quads(mc.getBlockRenderer().getBlockModel(s), s, side, rand, null);
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new IllegalStateException("the tank block has no Bettacraft model: " + model.getClass().getName(), e);
        }
        return dm.quads(s, side, rand, org.overengineer.bettacraft.platform.model.ModelValues.EMPTY, null);
    }

    private static String spriteName(net.minecraft.client.renderer.block.model.BakedQuad q) {
        return q.sprite().contents().name().toString();
    }

    /** Shows or hides the miniatures (by reflection: an old release has no switch); {@code probe}: only whether it can. */
    private static boolean hideMiniatures(boolean hide, boolean probe) {
        if (BASELINE && probe) return false;
        try {
            var f = org.overengineer.bettacraft.client.interior.MiniatureRenderer.class.getField("hiddenForTest");
            if (!probe) f.setBoolean(null, hide);
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    /**
     * Guard: the view with the miniatures, without them and with them again; outside the screen boxes of the tanks (with a margin),
     * where the first and the third picture agree (no chunk loading in between), at most 0.3 % of the pixels may differ clearly
     * without the miniatures.
     */
    private void miniatureGuard(String pack, String view, double x, double eyeY, double z, float yaw, float pitch) {
        File dir = new File(out, "screenshots/" + pack);
        try {
            java.awt.image.BufferedImage a = javax.imageio.ImageIO.read(new File(dir, view + ".png")), b = javax.imageio.ImageIO.read(new File(dir, view + "_nomini.png")),
                    c = javax.imageio.ImageIO.read(new File(dir, view + "_again.png"));
            if (a == null || b == null || c == null || a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight() || a.getWidth() != c.getWidth()
                    || a.getHeight() != c.getHeight()) {
                guard(false, "miniature " + pack + " " + view + ": pictures missing");
                return;
            }
            int w = a.getWidth(), h = a.getHeight();
            boolean[] mask = new boolean[w * h];
            double fov = mc.options.fov().get();
            int masked = 0;
            // every tank the client knows (also those outside the tour's radius or limit)
            for (TankBlockEntity be : TankBlockEntity.CLIENT_CONTROLLERS) {
                TankData td = be.getTank();
                if (td == null || be.getLevel() != mc.level) continue;
                masked += maskBox(mask, w, h, new Tank(td.min(), td.sizeX(), td.sizeY(), td.sizeZ(), td.front(), td.name()), x, eyeY, z, yaw, pitch, fov);
            }
            long differ = 0, total = 0;
            for (int py = 0; py < h; py++)
                for (int px = 0; px < w; px++) {
                    if (mask[py * w + px]) continue;
                    total++;
                    int pa = a.getRGB(px, py), pb = b.getRGB(px, py), pc = c.getRGB(px, py);
                    // only where the picture is steady (the same before and after: no chunk loading or fading in between)
                    if (delta(pa, pc) > 24) continue;
                    if (delta(pa, pb) > 48) differ++;
                }
            double share = total == 0 ? 0 : differ * 100.0 / total;
            guard(share <= 0.3, String.format(Locale.ROOT, "miniature %s %s: %d of %d pixels outside the %d tank boxes differ (%.2f %%)", pack, view, differ,
                    total, masked, share));
        } catch (IOException e) {
            guard(false, "miniature " + pack + " " + view + ": " + e);
        }
    }

    private static int delta(int pa, int pb) {
        return Math.max(Math.abs((pa >> 16 & 255) - (pb >> 16 & 255)), Math.max(Math.abs((pa >> 8 & 255) - (pb >> 8 & 255)), Math.abs((pa & 255) - (pb & 255))));
    }

    /** Marks the screen box of a tank (inflated by half a block, 12 px margin); a tank partly behind the camera masks all. */
    private static int maskBox(boolean[] mask, int w, int h, Tank t, double x, double eyeY, double z, float yaw, float pitch, double fovDeg) {
        double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
        double fX = -Math.sin(yr) * Math.cos(pr), fY = -Math.sin(pr), fZ = Math.cos(yr) * Math.cos(pr);
        double rX = -Math.cos(yr), rY = 0, rZ = -Math.sin(yr);
        // up = right x forward
        double uX = rY * fZ - rZ * fY, uY = rZ * fX - rX * fZ, uZ = rX * fY - rY * fX;
        double k = (h / 2.0) / Math.tan(Math.toRadians(fovDeg) / 2);
        double minSx = Double.MAX_VALUE, minSy = Double.MAX_VALUE, maxSx = -Double.MAX_VALUE, maxSy = -Double.MAX_VALUE;
        int behind = 0;
        for (int i = 0; i < 8; i++) {
            double px = t.min().getX() - 0.5 + ((i & 1) != 0 ? t.sx() + 1 : 0), py = t.min().getY() - 0.5 + ((i & 2) != 0 ? t.sy() + 1 : 0),
                    pz = t.min().getZ() - 0.5 + ((i & 4) != 0 ? t.sz() + 1 : 0);
            double dx = px - x, dy = py - eyeY, dz = pz - z;
            double zc = dx * fX + dy * fY + dz * fZ;
            if (zc < 0.05) {
                behind++;
                continue;
            }
            double sx = w / 2.0 + (dx * rX + dy * rY + dz * rZ) / zc * k, sy = h / 2.0 - (dx * uX + dy * uY + dz * uZ) / zc * k;
            minSx = Math.min(minSx, sx);
            maxSx = Math.max(maxSx, sx);
            minSy = Math.min(minSy, sy);
            maxSy = Math.max(maxSy, sy);
        }
        if (behind == 8) return 0;
        if (behind > 0) {
            java.util.Arrays.fill(mask, true);
            return 1;
        }
        int x0 = (int) Math.max(0, Math.floor(minSx - 12)), x1 = (int) Math.min(w - 1, Math.ceil(maxSx + 12));
        int y0 = (int) Math.max(0, Math.floor(minSy - 12)), y1 = (int) Math.min(h - 1, Math.ceil(maxSy + 12));
        if (x0 > x1 || y0 > y1) return 0;
        for (int py = y0; py <= y1; py++) for (int px = x0; px <= x1; px++) mask[py * w + px] = true;
        return 1;
    }

    private void guard(boolean ok, String line) {
        String l = (ok ? "PASS " : BASELINE ? "DIFF " : "FAIL ") + line;
        summary.add(l);
        if (!ok && !BASELINE) problems++;
        LOGGER.info("[smoketest] world tour guard {}", l);
    }

    private void finish() {
        try {
            Files.createDirectories(out.toPath());
            Files.write(new File(out, "summary.txt").toPath(), summary, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("[smoketest] world tour: summary not written", e);
        }
        mc.options.hideGui = false;
        LOGGER.info(problems == 0 ? "[smoketest] PASSED (world tour, {} guard lines)" : "[smoketest] FAILED: world tour, {} guard lines, see summary.txt", summary.size());
    }

    // ------------------------------------------------------------------ shader packs (Iris, by reflection; dev only)

    private static List<String> packs() {
        List<String> list = new ArrayList<>();
        boolean iris = irisClass() != null;
        File dir = new File(Minecraft.getInstance().gameDirectory, "shaderpacks");
        for (String s : PACKS.split(",")) {
            String pack = s.trim();
            if (pack.isEmpty()) continue;
            if (pack.equals("none")) list.add(pack);
            else if (!iris) LOGGER.info("[smoketest] world tour: pack {} skipped: no Iris", pack);
            else if (!new File(dir, pack).exists()) LOGGER.info("[smoketest] world tour: pack {} skipped: not in {}", pack, dir);
            else list.add(pack);
        }
        return list;
    }

    private static Class<?> irisClass() {
        try {
            return Class.forName("net.irisshaders.iris.Iris");
        } catch (Throwable t) {
            return null;
        }
    }

    private void rememberIris() {
        if (irisClass() == null) return;
        try {
            Object cfg = irisClass().getMethod("getIrisConfig").invoke(null);
            Object name = cfg.getClass().getMethod("getShaderPackName").invoke(cfg);
            oldPack = name instanceof java.util.Optional<?> o ? o.map(String::valueOf).orElse("") : String.valueOf(name);
            oldEnabled = (boolean) cfg.getClass().getMethod("areShadersEnabled").invoke(cfg);
        } catch (Throwable t) {
            LOGGER.warn("[smoketest] world tour: could not read the Iris config", t);
        }
    }

    private void restoreIris() {
        if (irisClass() == null || oldPack == null) return;
        apply(oldPack.isEmpty() ? null : oldPack, oldEnabled);
    }

    private void switchPack(String pack) {
        if (irisClass() == null) return;
        apply(pack.equals("none") ? null : pack, !pack.equals("none"));
    }

    private void apply(String pack, boolean enabled) {
        try {
            Object cfg = irisClass().getMethod("getIrisConfig").invoke(null);
            if (pack != null) cfg.getClass().getMethod("setShaderPackName", String.class).invoke(cfg, pack);
            cfg.getClass().getMethod("setShadersEnabled", boolean.class).invoke(cfg, enabled);
            cfg.getClass().getMethod("save").invoke(cfg);
            irisClass().getMethod("reload").invoke(null);
        } catch (Throwable t) {
            problems++;
            LOGGER.error("[smoketest] world tour FAIL: could not switch Iris to {} ({})", pack, enabled, t);
        }
    }

    private void logPack(String pack) {
        boolean inUse = org.overengineer.bettacraft.client.interior.IrisCompat.shaderPackInUse();
        boolean ok = pack.equals("none") ? !inUse : inUse;
        guard(ok, "pack " + pack + ": shader pack in use " + inUse);
    }

    private static String shortName(String pack) {
        if (pack.equals("none")) return "noshader";
        String s = pack.replace(".zip", "").toLowerCase(Locale.ROOT);
        if (s.startsWith("complementaryreimagined")) return "reimagined";
        if (s.startsWith("complementaryunbound")) return "unbound";
        if (s.startsWith("bsl")) return "bsl";
        if (s.startsWith("makeup")) return "makeup";
        return s.replaceAll("[^a-z0-9]+", "_");
    }

    // ------------------------------------------------------------------ helpers

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        target = new double[]{x, eyeY, z, yaw, pitch};
        arrivedFor = 0;
        server(p -> teleport(p, x, eyeY - p.getEyeHeight(), z, yaw, pitch));
    }

    private void lookAt(double x, double eyeY, double z, Vec3 target) {
        double dx = target.x - x, dy = target.y - eyeY, dz = target.z - z;
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        look(x, eyeY, z, yaw, pitch);
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    /** After the camera arrived where the last look put it and stood there {@code ticks} ticks. */
    private void thenAt(int ticks, Runnable action) {
        steps.add(new Step(ticks, action, true));
    }

    private boolean arrived() {
        double[] t = target;
        if (t == null || mc.player == null) return true;
        double dx = mc.player.getX() - t[0], dy = mc.player.getEyeY() - t[1], dz = mc.player.getZ() - t[2];
        float dyaw = net.minecraft.util.Mth.wrapDegrees(mc.player.getYRot() - (float) t[3]);
        return dx * dx + dy * dy + dz * dz < 0.01 && Math.abs(dyaw) < 0.5F && Math.abs(mc.player.getXRot() - (float) t[4]) < 0.5F;
    }

    /** Returns true when all steps are done. */
    private boolean tick() {
        if (mc.player != null) {
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        if (steps.isEmpty()) return true;
        Step s = steps.peek();
        if (s.atTarget()) {
            // the picture only once the camera is there (a busy server answers a teleport late: the picture showed the last place)
            if (arrived()) arrivedFor++;
            else arrivedFor = 0;
            if (arrivedFor < s.delay() && wait < 60) {
                wait++;
                return false;
            }
            if (arrivedFor < s.delay()) LOGGER.warn("[smoketest] world tour: the camera did not reach {} in 60 ticks (pushed out of a block?)", java.util.Arrays.toString(target));
        } else if (wait < s.delay()) {
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

    private void shot(String pack, String view) {
        File dir = new File(out, "screenshots/" + pack);
        dir.mkdirs();
        mc.getToastManager().clear();
        Screenshot.grab(out, pack + "/" + view + ".png", mc.getMainRenderTarget(), 1, c -> {});
    }

    private static void press(net.minecraft.client.gui.components.Button b) {
        Minecraft.getInstance().execute(() -> b.onPress(new net.minecraft.client.input.MouseButtonInfo(0, 0)));
    }

    // ------------------------------------------------------------------ version specific

    private static BlockPos spawn(ServerLevel level) {
        return level.getRespawnData().pos();
    }

    private static void teleport(ServerPlayer p, double x, double y, double z, float yaw, float pitch) {
        p.teleportTo(p.level(), x, y, z, java.util.Set.of(), yaw, pitch, false);
    }

    /** Clear weather for long (a copied world may have rain; the frozen ticks keep it so). */
    static void clearWeather(ServerLevel level) {
        var server = level.getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "weather clear 1000000");
    }

    private static void gameRules(ServerLevel level) {
        level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME, false, level.getServer());
        level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_WEATHER, false, level.getServer());
    }
}
