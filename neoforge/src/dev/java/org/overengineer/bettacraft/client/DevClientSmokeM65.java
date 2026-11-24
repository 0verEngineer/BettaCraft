package org.overengineer.bettacraft.client;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.client.tank.TankWaterGeometry;
import org.overengineer.bettacraft.client.tank.TankWaterRenderer;
import org.overengineer.bettacraft.plumbing.FilterPipeBlock;
import org.overengineer.bettacraft.plumbing.FilterPipeBundleBlock;
import org.overengineer.bettacraft.plumbing.PipeLine;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankStyle;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M65 client smoke ({@code -Psmoketestm65}, NeoForge and Fabric, background window).
 * <ul>
 *     <li>{@code pipes}: a pipe yard: stainless steel and glass pipes (straight, bends, T, cross, up), the four filter lines
 *     with T-pieces, and bundles where the lines come in and leave as single pipes (the step from a bundle's arm to a single
 *     pipe).</li>
 *     <li>{@code wall}: tanks with blocks standing inside against the front glass (a wall in an aquarium with clear glass,
 *     a paludarium's land behind vanilla glass, a wall behind stained glass). Guard: every view three times, without any
 *     water ({@code _dry}), the old way ({@code _old}: the water's side faces where the miniature's blocks are) and M65; on the
 *     blocks at the glass the M65 picture must equal the dry one (no water drawn over them), next to them it must not (the
 *     water is still there). The old way shows the water over the blocks (the red half of the guard).</li>
 * </ul>
 * Screenshots {@code bettacraft_m65_<tag>_<pack>_<view>.png} ({@code -Pm65tag}, default {@code run}), packs {@code -Pm65packs}
 * (default none and Complementary Reimagined when Iris is there), summary {@code bettacraft_m65_<tag>_summary.txt}.
 */
final class DevClientSmokeM65 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static final String TAG = System.getProperty("bettacraft.smoketest.m65tag", "run");
    private static final String PARTS = System.getProperty("bettacraft.smoketest.m65parts", "pipes,wall");
    private static final String DEFAULT_PACKS = "none,ComplementaryReimagined_r5.10.zip";
    /** The wall tanks: [glass block, style, fill share of the height, what stands at the glass]. */
    private static final Object[][] WALL_TANKS = {
            {"glass", TankStyle.CLEAN, 1.0F, "wall"},
            {"glass", TankStyle.TEXTURED, 0.62F, "land"},
            {"light_blue_stained_glass", TankStyle.TEXTURED, 1.0F, "wall"},
    };
    private static final int WALL_SX = 4, WALL_SY = 3, WALL_SZ = 3;
    /** The patch of blocks against the front glass of a "wall" tank (interior blocks: x0, x1 exclusive, y1 exclusive). */
    private static final int WALL_X0 = 8, WALL_X1 = 36, WALL_Y1 = 30;
    /** The land of the paludarium against the front glass: x from 0 to LAND_X1, up to LAND_Y1 (above the water). */
    private static final int LAND_X1 = 30, LAND_Y1 = 40;

    private record Step(int delay, Runnable action) {}

    private record Cam(double x, double y, double z, float yaw, float pitch, double fov) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private volatile BlockPos origin, yard;
    private final BlockPos[] wall = new BlockPos[WALL_TANKS.length];
    private final List<String> summary = new ArrayList<>();
    private int wait;
    private int problems;
    private volatile boolean built;
    private boolean started;
    private String oldPack;
    private boolean oldEnabled;

    DevClientSmokeM65() {
        then(1, () -> server(this::build));
        then(1, () -> {
            mc.options.hideGui = true;
            mc.options.fovEffectScale().set(0.0);
        });
        then(1, this::rememberIris);
        for (String pack : packs()) {
            String name = shortName(pack);
            then(1, () -> switchPack(pack));
            then(1, () -> time(6000));
            then(200, () -> LOGGER.info("[smoketest] M65 pack {}: shader pack in use {}", pack,
                    org.overengineer.bettacraft.client.interior.IrisCompat.shaderPackInUse()));
            if (PARTS.contains("pipes")) pipeViews(name);
            if (PARTS.contains("wall")) wallViews(name);
        }
        then(1, this::restoreIris);
        then(1, () -> time(6000));
        then(40, () -> {
            mc.options.hideGui = false;
            writeSummary();
        });
    }

    int problems() {
        return problems;
    }

    // ------------------------------------------------------------------ pipes

    private void pipeViews(String pack) {
        // overview from the front and above, then close-ups
        then(1, () -> lookAt(yard.getX() + 6, yard.getY() + 9.0, yard.getZ() - 6.0, new Vec3(yard.getX() + 6, yard.getY(), yard.getZ() + 12)));
        then(80, () -> shot(pack, "pipes_overview"));
        // the steel row (z 0) and the glass row (z 4)
        then(1, () -> lookAt(yard.getX() + 3.5, yard.getY() + 2.2, yard.getZ() - 2.8, new Vec3(yard.getX() + 3.5, yard.getY() + 0.5, yard.getZ() + 0.5)));
        then(40, () -> shot(pack, "pipes_steel"));
        then(1, () -> lookAt(yard.getX() + 3.5, yard.getY() + 2.2, yard.getZ() + 1.2, new Vec3(yard.getX() + 3.5, yard.getY() + 0.5, yard.getZ() + 4.5)));
        then(40, () -> shot(pack, "pipes_glass"));
        // the filter lines with T-pieces (z 8..17)
        then(1, () -> lookAt(yard.getX() + 4.5, yard.getY() + 3.2, yard.getZ() + 4.5, new Vec3(yard.getX() + 4.5, yard.getY() + 0.5, yard.getZ() + 12.5)));
        then(40, () -> shot(pack, "pipes_tee"));
        then(1, () -> lookAt(yard.getX() + 4.5, yard.getY() + 1.0, yard.getZ() + 6.6, new Vec3(yard.getX() + 4.5, yard.getY() + 0.5, yard.getZ() + 8.5)));
        then(40, () -> shot(pack, "pipes_tee_close"));
        // the bundles (z 23): single pipes going into a bundle and leaving it
        then(1, () -> lookAt(yard.getX() + 5.5, yard.getY() + 3.2, yard.getZ() + 19.0, new Vec3(yard.getX() + 5.5, yard.getY() + 0.5, yard.getZ() + 23.5)));
        then(40, () -> shot(pack, "pipes_bundle"));
        then(1, () -> lookAt(yard.getX() + 2.6, yard.getY() + 1.1, yard.getZ() + 21.6, new Vec3(yard.getX() + 3.0, yard.getY() + 0.5, yard.getZ() + 23.5)));
        then(40, () -> shot(pack, "pipes_bundle_step"));
        then(1, () -> lookAt(yard.getX() + 1.0, yard.getY() + 1.4, yard.getZ() + 22.0, new Vec3(yard.getX() + 3.0, yard.getY() + 0.5, yard.getZ() + 23.5)));
        then(40, () -> shot(pack, "pipes_bundle_step_side"));
        then(1, () -> lookAt(yard.getX() + 8.5, yard.getY() + 2.4, yard.getZ() + 26.4, new Vec3(yard.getX() + 7.0, yard.getY() + 0.5, yard.getZ() + 23.5)));
        then(40, () -> shot(pack, "pipes_bundle_back"));
    }

    private void buildYard(ServerLevel level) {
        BlockPos y = yard;
        for (BlockPos q : BlockPos.betweenClosed(y.offset(-3, -1, -3), y.offset(20, 6, 29)))
            level.setBlock(q, q.getY() == y.getY() - 1 ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        Block steel = ModBlocks.STAINLESS_STEEL_PIPE.get(), glass = ModBlocks.GLASS_PIPE.get();
        for (int r = 0; r < 2; r++) {
            Block b = r == 0 ? steel : glass;
            BlockPos o = y.offset(0, 0, r * 4);
            // a straight run, a bend, a T and a cross, a pipe going up
            for (int x = 0; x <= 7; x++) set(level, o.offset(x, 0, 0), b);
            set(level, o.offset(0, 0, 1), b);
            set(level, o.offset(0, 0, 2), b);
            set(level, o.offset(3, 0, 1), b);
            set(level, o.offset(5, 0, 1), b);
            set(level, o.offset(5, 0, -1), b);
            for (int h = 1; h <= 2; h++) set(level, o.offset(7, h, 0), b);
            set(level, o.offset(8, 2, 0), b);
            set(level, o.offset(9, 2, 0), b);
        }
        // the filter lines with T-pieces: four parallel runs (z 8, 11, 14, 17), each with a branch
        Block[] lines = {ModBlocks.FILTER_INLET_PIPE.get(), ModBlocks.FILTER_OUTLET_PIPE.get(), ModBlocks.FILTER_FRESH_PIPE.get(), ModBlocks.FILTER_WASTE_PIPE.get()};
        for (int i = 0; i < lines.length; i++) {
            BlockPos o = y.offset(0, 0, 8 + 3 * i);
            for (int x = 0; x <= 8; x++) set(level, o.offset(x, 0, 0), lines[i]);
            set(level, o.offset(4, 0, 1), lines[i]);
            FilterPipeBlock.makeJunction(level, o.offset(4, 0, 0));
        }
        // the bundles: the intake runs along x (z 23); the return comes in from the north at x 3, the waste water from the south
        // at x 4; the return leaves to the south at x 7, the waste water at x 9 to the south, the intake goes on east
        BlockPos o = y.offset(0, 0, 23);
        for (int x = 0; x <= 11; x++) set(level, o.offset(x, 0, 0), ModBlocks.FILTER_INLET_PIPE.get());
        for (int z = -3; z <= -1; z++) set(level, o.offset(3, 0, z), ModBlocks.FILTER_OUTLET_PIPE.get());
        for (int z = 1; z <= 3; z++) set(level, o.offset(7, 0, z), ModBlocks.FILTER_OUTLET_PIPE.get());
        for (int x = 3; x <= 7; x++) FilterPipeBundleBlock.addLine(level, o.offset(x, 0, 0), PipeLine.RETURN, null, null);
        for (int z = 1; z <= 3; z++) set(level, o.offset(4, 0, z), ModBlocks.FILTER_WASTE_PIPE.get());
        for (int x = 4; x <= 9; x++) FilterPipeBundleBlock.addLine(level, o.offset(x, 0, 0), PipeLine.WASTE, null, null);
        for (int z = 1; z <= 3; z++) set(level, o.offset(9, 0, z), ModBlocks.FILTER_WASTE_PIPE.get());
        // a T-piece on the intake line inside a bundle and on a single pipe
        FilterPipeBlock.makeJunction(level, o.offset(5, 0, 0));
        FilterPipeBlock.makeJunction(level, o.offset(1, 0, 0));
        set(level, o.offset(1, 0, -1), ModBlocks.FILTER_INLET_PIPE.get());
        LOGGER.info("[smoketest] M65 pipe yard at {}", y);
    }

    private static void set(ServerLevel level, BlockPos pos, Block b) {
        level.setBlock(pos, b.defaultBlockState(), Block.UPDATE_ALL);
    }

    // ------------------------------------------------------------------ water at blocks against the glass

    private void wallViews(String pack) {
        for (int t = 0; t < WALL_TANKS.length; t++) {
            final int ti = t;
            String kind = (String) WALL_TANKS[t][3] + t;
            for (double d : new double[] {3.2, 8, 20}) {
                String view = "wall_" + kind + "_front_" + (int) d;
                wallShot(pack, view, ti, () -> {
                    BlockPos m = wall[ti];
                    lookAt(m.getX() + WALL_SX / 2.0, m.getY() + 1.2, m.getZ() - d, new Vec3(m.getX() + WALL_SX / 2.0, m.getY() + 1.2, m.getZ()));
                });
            }
            String view = "wall_" + kind + "_oblique";
            wallShot(pack, view, ti, () -> {
                BlockPos m = wall[ti];
                lookAt(m.getX() - 1.6, m.getY() + 2.0, m.getZ() - 3.6, new Vec3(m.getX() + 1.5, m.getY() + 1.0, m.getZ()));
            });
        }
    }

    /**
     * Every view three times: no water ({@code _dry}), the old way ({@code _old}) and M65. On the blocks at the glass (inside a
     * margin) M65 must equal the dry picture; beside them, where the water is, M65 must differ from it.
     */
    private void wallShot(String pack, String view, int tank, Runnable aim) {
        Cam[] cam = new Cam[1];
        String[] modes = {"dry", "old", "new", "dry2"};
        for (String mode : modes) {
            String v = mode.equals("new") ? view : view + "_" + mode;
            // the camera set again for every picture (a client that drifted between two pictures compared different views)
            then(1, aim);
            then(1, () -> {
                TankWaterRenderer.hiddenForTest = mode.startsWith("dry");
                TankWaterGeometry.oldSideInsetForTest = mode.equals("old");
                mc.levelRenderer.allChanged();
            });
            then(60, () -> {
                cam[0] = new Cam(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), mc.options.fov().get());
                shot(pack, v);
            });
        }
        then(1, () -> {
            TankWaterRenderer.hiddenForTest = false;
            TankWaterGeometry.oldSideInsetForTest = false;
            mc.levelRenderer.allChanged();
        });
        then(20, () -> {
            double[] old = compare(pack, view, view + "_old", tank, cam[0]);
            double[] now = compare(pack, view, view, tank, cam[0]);
            double[] noise = compare(pack, view, view + "_dry2", tank, cam[0]);
            if (old == null || now == null || noise == null) {
                problems++;
                guard(false, "wall " + pack + " " + view + ": pictures missing");
                return;
            }
            if (now[0] < 0) {
                guard(true, "wall " + pack + " " + view + ": not checked (the blocks are too small on screen)");
                return;
            }
            if (old[0] >= 0 && old[0] < 5) {
                // the water does not show on the blocks even the old way (a shader pack that draws the water almost clear there)
                guard(true, String.format(Locale.ROOT, "wall %s %s: not checked (the water shows on the blocks neither way: old way %.2f %%, now %.2f %%)",
                        pack, view, old[0], now[0]));
                return;
            }
            // on the blocks: like the dry picture (at most the noise of two dry pictures plus a little), or, with shader packs whose
            // light and shadows change with the water around (BSL), at least ten times cleaner than the old way; beside them: water
            boolean blocks = now[0] <= 1.0 + 2 * noise[0] || now[0] <= 6.0 && now[0] <= old[0] * 0.1;
            boolean water = now[1] < 0 || now[1] >= 20;
            boolean ok = blocks && water;
            if (!ok) problems++;
            String line = String.format(Locale.ROOT, "%s wall %s %s: on the blocks at the glass %.2f %% differ from no water (old way %.2f %%, noise %.2f %%), "
                    + "the water beside them %.2f %% (old way %.2f %%)", ok ? "PASS" : "FAIL", pack, view, now[0], old[0], noise[0], now[1], old[1]);
            summary.add(line);
            LOGGER.info("[smoketest] M65 guard {}", line);
        });
    }

    /**
     * {share of the pixels on the blocks at the glass that differ from the dry picture, the same beside them in the water}
     * (percent; -1 when the area is too small on screen), null when a picture is missing.
     */
    private double[] compare(String pack, String view, String test, int tank, Cam cam) {
        File dir = new File(mc.gameDirectory, "screenshots");
        BufferedImage ref, img;
        try {
            ref = javax.imageio.ImageIO.read(new File(dir, "bettacraft_m65_" + TAG + "_" + pack + "_" + view + "_dry.png"));
            img = javax.imageio.ImageIO.read(new File(dir, "bettacraft_m65_" + TAG + "_" + pack + "_" + test + ".png"));
        } catch (java.io.IOException e) {
            return null;
        }
        if (ref == null || img == null || ref.getWidth() != img.getWidth() || ref.getHeight() != img.getHeight()) return null;
        BlockPos m = wall[tank];
        boolean land = WALL_TANKS[tank][3].equals("land");
        double z = m.getZ() + 0.02, u = 1 / 16.0, in = 2.5 * u;
        double fillTop = m.getY() + WALL_SY * (float) WALL_TANKS[tank][2] - 3 * u;
        // the blocks: a margin inside the patch, below the water surface
        double bx0 = m.getX() + (land ? 0 : WALL_X0) * u + in, bx1 = m.getX() + (land ? LAND_X1 : WALL_X1) * u - in;
        double by0 = m.getY() + in, by1 = Math.min(m.getY() + (land ? LAND_Y1 : WALL_Y1) * u - in, fillTop);
        // the water: right of the patch, the same height
        double wx0 = m.getX() + (land ? LAND_X1 : WALL_X1) * u + in, wx1 = Math.min(wx0 + 1.5, m.getX() + WALL_SX - in);
        return new double[] {share(img, ref, cam, bx0, by0, bx1, by1, z), share(img, ref, cam, wx0, by0, wx1, by1, z)};
    }

    private static double share(BufferedImage img, BufferedImage ref, Cam cam, double x0, double y0, double x1, double y1, double z) {
        int[][] c = {project(img, cam, x0, y0, z), project(img, cam, x1, y0, z), project(img, cam, x1, y1, z), project(img, cam, x0, y1, z)};
        for (int[] p : c) if (p == null) return -1;
        int px0 = Integer.MAX_VALUE, px1 = Integer.MIN_VALUE, py0 = Integer.MAX_VALUE, py1 = Integer.MIN_VALUE;
        for (int[] p : c) {
            px0 = Math.min(px0, p[0]);
            px1 = Math.max(px1, p[0]);
            py0 = Math.min(py0, p[1]);
            py1 = Math.max(py1, p[1]);
        }
        long n = 0, differ = 0;
        for (int y = py0; y <= py1; y++) for (int x = px0; x <= px1; x++) {
            if (!inside(c, x, y)) continue;
            n++;
            if (Math.abs(luma(img, x, y) - luma(ref, x, y)) > 6 || colourDiff(img, ref, x, y) > 14) differ++;
        }
        if (n < 150) return -1;
        return differ * 100.0 / n;
    }

    private static int colourDiff(BufferedImage a, BufferedImage b, int x, int y) {
        int p = a.getRGB(x, y), q = b.getRGB(x, y);
        return Math.max(Math.abs((p >> 16 & 255) - (q >> 16 & 255)), Math.max(Math.abs((p >> 8 & 255) - (q >> 8 & 255)), Math.abs((p & 255) - (q & 255))));
    }

    private static boolean inside(int[][] c, int x, int y) {
        int sign = 0;
        for (int i = 0; i < 4; i++) {
            int[] a = c[i], b = c[(i + 1) % 4];
            long cross = (long) (b[0] - a[0]) * (y - a[1]) - (long) (b[1] - a[1]) * (x - a[0]);
            int sg = cross > 0 ? 1 : cross < 0 ? -1 : 0;
            if (sg == 0) continue;
            if (sign == 0) sign = sg;
            else if (sg != sign) return false;
        }
        return true;
    }

    private static double luma(BufferedImage img, int x, int y) {
        int p = img.getRGB(x, y);
        return 0.299 * (p >> 16 & 255) + 0.587 * (p >> 8 & 255) + 0.114 * (p & 255);
    }

    private static int[] project(BufferedImage img, Cam cam, double px, double py, double pz) {
        int w = img.getWidth(), h = img.getHeight();
        double yr = Math.toRadians(cam.yaw()), pr = Math.toRadians(cam.pitch());
        double fX = -Math.sin(yr) * Math.cos(pr), fY = -Math.sin(pr), fZ = Math.cos(yr) * Math.cos(pr);
        double rX = -Math.cos(yr), rY = 0, rZ = -Math.sin(yr);
        double uX = rY * fZ - rZ * fY, uY = rZ * fX - rX * fZ, uZ = rX * fY - rY * fX;
        double k = (h / 2.0) / Math.tan(Math.toRadians(cam.fov()) / 2);
        double dx = px - cam.x(), dy = py - cam.y(), dz = pz - cam.z();
        double zc = dx * fX + dy * fY + dz * fZ;
        if (zc < 0.05) return null;
        double sx = w / 2.0 + (dx * rX + dy * rY + dz * rZ) / zc * k, sy = h / 2.0 - (dx * uX + dy * uY + dz * uZ) / zc * k;
        if (sx < 1 || sy < 1 || sx >= w - 1 || sy >= h - 1) return null;
        return new int[] {(int) Math.round(sx), (int) Math.round(sy)};
    }

    // ------------------------------------------------------------------ scene

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ServerLevel in = InteriorManager.interiorLevel(p.server);
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        DevClientWorldTour.clearWeather(level);
        origin = p.blockPosition().offset(-20, 0, 180);
        yard = origin.offset(0, 1, 0);
        buildYard(level);
        BlockPos row = origin.offset(30, 0, 0);
        clear(level, row.offset(-4, 0, -24), 30, 10, 30);
        for (int t = 0; t < WALL_TANKS.length; t++) {
            BlockPos base = row.offset(t * 7, 0, 0);
            for (int x = 0; x < WALL_SX; x++) for (int z = 0; z < WALL_SZ; z++) level.setBlockAndUpdate(base.offset(x, 0, z), Blocks.OAK_PLANKS.defaultBlockState());
            wall[t] = base.offset(0, 1, 0);
            TankBlockEntity c = tank(level, p, wall[t], ResourceLocation.withDefaultNamespace((String) WALL_TANKS[t][0]), (TankStyle) WALL_TANKS[t][1],
                    (float) WALL_TANKS[t][2], "M65 " + WALL_TANKS[t][3] + " " + t);
            InteriorRegion r = InteriorManager.ensureRegion(level, c);
            for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++)
                Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.SAND.defaultBlockState(), false);
            boolean land = WALL_TANKS[t][3].equals("land");
            BlockState a = land ? Blocks.DIRT.defaultBlockState() : Blocks.GRANITE.defaultBlockState();
            BlockState b = land ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.POLISHED_GRANITE.defaultBlockState();
            int x0 = land ? 0 : WALL_X0, x1 = land ? LAND_X1 : WALL_X1, y1 = land ? LAND_Y1 : WALL_Y1;
            // one block thick at the front glass, a slope behind it for the land
            for (int x = x0; x < x1; x++) for (int y = 0; y < y1; y++) {
                int depth = land ? Math.max(1, (y1 - y) / 2) : 2;
                for (int z = 0; z < depth && z < r.sizeZ(); z++) in.setBlock(r.toWorld(x, y, z), (x / 4 + y / 4) % 2 == 0 ? a : b, 2);
            }
            // a back wall in the aquarium (the user's case of a wall built into a tank)
            if (!land) for (int x = 0; x < r.sizeX(); x++) for (int y = 0; y < r.sizeY() - 4; y++) in.setBlock(r.toWorld(x, y, r.sizeZ() - 1), Blocks.STONE_BRICKS.defaultBlockState(), 2);
        }
        built = true;
        LOGGER.info("[smoketest] M65 scene at {}: wall tanks {} {} {}", origin, wall[0], wall[1], wall[2]);
    }

    private static void clear(ServerLevel level, BlockPos min, int sx, int sy, int sz) {
        for (int x = 0; x < sx; x++) for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) {
            BlockPos pos = min.offset(x, y, z);
            if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (int x = 0; x < sx; x++) for (int z = 0; z < sz; z++) level.setBlock(min.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    private static TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, ResourceLocation glassId, TankStyle style, float fill, String name) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < WALL_SY; y++) for (int z = 0; z < WALL_SZ; z++) for (int x = 0; x < WALL_SX; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) {
            level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
            if (level.getBlockEntity(pos) instanceof TankBlockEntity be) {
                be.setGlassId(glassId);
                be.setStyle(style);
            }
        }
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, (long) (WALL_SX * WALL_SY * WALL_SZ * 1000 * fill), 0F);
        c.getTank().setName(name);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(p.getUUID());
        c.getTank().setWaterTemp(25F);
        c.getTank().setTargetTemp(25F);
        c.markTankChanged();
        return c;
    }

    private void time(long t) {
        server(p -> DevClientWorldTour.clearWeather(p.serverLevel()));
        server(p -> p.serverLevel().setDayTime(t));
    }

    // ------------------------------------------------------------------ shader packs (Iris, by reflection; dev only)

    private static List<String> packs() {
        String wanted = System.getProperty("bettacraft.smoketest.m65packs", DEFAULT_PACKS);
        List<String> out = new ArrayList<>();
        boolean iris = irisClass() != null;
        File dir = new File(Minecraft.getInstance().gameDirectory, "shaderpacks");
        for (String s : wanted.split(",")) {
            String pack = s.trim();
            if (pack.isEmpty()) continue;
            if (pack.equals("none")) out.add(pack);
            else if (!iris) LOGGER.info("[smoketest] M65 pack {} skipped: no Iris", pack);
            else if (!new File(dir, pack).exists()) LOGGER.info("[smoketest] M65 pack {} skipped: not in {}", pack, dir);
            else out.add(pack);
        }
        return out;
    }

    private static Class<?> irisClass() {
        try {
            return Class.forName("net.irisshaders.iris.Iris");
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object irisConfig() throws ReflectiveOperationException {
        return irisClass().getMethod("getIrisConfig").invoke(null);
    }

    private void rememberIris() {
        if (irisClass() == null) return;
        try {
            Object cfg = irisConfig();
            Object name = cfg.getClass().getMethod("getShaderPackName").invoke(cfg);
            oldPack = name instanceof java.util.Optional<?> o ? o.map(String::valueOf).orElse("") : String.valueOf(name);
            oldEnabled = (boolean) cfg.getClass().getMethod("areShadersEnabled").invoke(cfg);
        } catch (Throwable t) {
            LOGGER.warn("[smoketest] M65 could not read the Iris config", t);
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
            Object cfg = irisConfig();
            if (pack != null) cfg.getClass().getMethod("setShaderPackName", String.class).invoke(cfg, pack);
            cfg.getClass().getMethod("setShadersEnabled", boolean.class).invoke(cfg, enabled);
            cfg.getClass().getMethod("save").invoke(cfg);
            irisClass().getMethod("reload").invoke(null);
        } catch (Throwable t) {
            problems++;
            LOGGER.error("[smoketest] M65 FAIL: could not switch Iris to {} ({})", pack, enabled, t);
        }
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

    private void guard(boolean ok, String line) {
        String l = (ok ? "PASS " : "FAIL ") + line;
        summary.add(l);
        LOGGER.info("[smoketest] M65 guard {}", l);
    }

    private void writeSummary() {
        try {
            File dir = new File(mc.gameDirectory, "screenshots");
            java.nio.file.Files.write(new File(dir, "bettacraft_m65_" + TAG + "_summary.txt").toPath(), summary, java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            LOGGER.warn("[smoketest] M65 summary not written", e);
        }
    }

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> {
            ServerLevel to = InteriorManager.isInterior(p.level()) ? p.server.overworld() : p.serverLevel();
            p.teleportTo(to, x, eyeY - p.getEyeHeight(), z, yaw, pitch);
        });
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

    /** Returns true when all steps are done. */
    boolean tick() {
        if (mc.player != null && !steps.isEmpty()) {
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (steps.isEmpty()) return true;
        if (started && !built) return false;
        Step s = steps.peek();
        if (wait < s.delay()) {
            wait++;
            return false;
        }
        steps.poll();
        wait = 0;
        started = true;
        s.action().run();
        return steps.isEmpty();
    }

    private void server(java.util.function.Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String pack, String view) {
        mc.getToasts().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m65_" + TAG + "_" + pack + "_" + view + ".png", mc.getMainRenderTarget(), c -> {});
    }
}
