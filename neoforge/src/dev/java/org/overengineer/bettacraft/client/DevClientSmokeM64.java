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
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.equipment.AirPumpBlock;
import org.overengineer.bettacraft.equipment.LedPanelBlock;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankStyle;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tank.plants.TankPlants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M64 client smoke ({@code -Psmoketestm64}, NeoForge and Fabric, background window): the tank optics of M64.
 * <ul>
 *     <li>{@code glass}: two big textured tanks (vanilla glass, light blue stained glass) from the front at 8 to 64 blocks and
 *     at an oblique angle. Guard: along every block border inside a face the picture must not differ from the middle of the
 *     blocks more than the middle of the blocks differs from itself (the frame of every single pane showing again at a
 *     distance: mipmapping of the glass frame).</li>
 *     <li>{@code hose}: an air pump beside a tank with air stones and a sponge filter (the hose must not lie on the floor).</li>
 *     <li>{@code coral}: a reef tank with mushroom corals (stalks on their base) and the other corals, inside and outside.</li>
 *     <li>{@code float}: floating plants with roots (hornwort), two pictures a moment apart (the roots sway).</li>
 *     <li>{@code lid}: lids with a light bar on them, an LED panel directly on a tank (counts as a cover).</li>
 * </ul>
 * Screenshots {@code bettacraft_m64_<tag>_<pack>_<view>.png} ({@code -Pm64tag}, default {@code run}), packs
 * {@code -Pm64packs} (default none and Complementary Reimagined when Iris is there).
 */
final class DevClientSmokeM64 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static final String TAG = System.getProperty("bettacraft.smoketest.m64tag", "run");
    private static final String PARTS = System.getProperty("bettacraft.smoketest.m64parts", "glass,hose,coral,float,lid");
    private static final String DEFAULT_PACKS = "none,ComplementaryReimagined_r5.10.zip";
    /** The glass tanks: [glass block, x size, y size, z size, offset x, offset z]. */
    private static final Object[][] GLASS_TANKS = {
            {"glass", 16, 5, 3, 0, 0},
            {"light_blue_stained_glass", 10, 4, 3, -60, 40},
    };

    private record Step(int delay, Runnable action) {}

    /** Where the camera was when a picture was taken (for the glass guard). */
    private record Cam(double x, double y, double z, float yaw, float pitch, double fov) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private volatile BlockPos origin;
    private final BlockPos[] glass = new BlockPos[GLASS_TANKS.length];
    private volatile BlockPos hoseTank, cabTank, coralTank, floatTank, lidTank, panelTank;
    private volatile InteriorRegion coralRegion;
    private final List<int[]> mushrooms = java.util.Collections.synchronizedList(new ArrayList<>());
    private final List<String> summary = new ArrayList<>();
    private int wait;
    private int problems;
    /** The server built the scene (it builds on its own thread; the steps wait for it). */
    private volatile boolean built;
    private boolean started;
    private String oldPack;
    private boolean oldEnabled;

    DevClientSmokeM64() {
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
            then(200, () -> LOGGER.info("[smoketest] M64 pack {}: shader pack in use {}", pack,
                    org.overengineer.bettacraft.client.interior.IrisCompat.shaderPackInUse()));
            if (PARTS.contains("glass")) glassViews(name);
            if (PARTS.contains("hose")) hoseViews(name);
            if (PARTS.contains("coral")) coralViews(name);
            if (PARTS.contains("float")) floatViews(name);
            if (PARTS.contains("lid")) lidViews(name);
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

    // ------------------------------------------------------------------ glass

    private void glassViews(String pack) {
        for (int t = 0; t < GLASS_TANKS.length; t++) {
            final int ti = t;
            String g = (String) GLASS_TANKS[t][0];
            int sx = (int) GLASS_TANKS[t][1], sy = (int) GLASS_TANKS[t][2];
            // straight from the front at growing distances, then at 60 degrees from the side
            for (int d : new int[] {16, 44, 100, 150}) {
                String view = "glass_" + g + "_front_" + d;
                then(1, () -> {
                    BlockPos m = glass[ti];
                    lookAt(m.getX() + sx / 2.0, m.getY() + sy / 2.0 + 0.4, m.getZ() - d, new Vec3(m.getX() + sx / 2.0, m.getY() + sy / 2.0, m.getZ()));
                });
                glassShot(pack, view, ti);
            }
            for (int d : new int[] {12, 30, 60}) {
                String view = "glass_" + g + "_oblique_" + d;
                then(1, () -> {
                    BlockPos m = glass[ti];
                    double a = Math.toRadians(76);
                    double cx = m.getX() + sx / 2.0, cz = m.getZ();
                    lookAt(cx - Math.sin(a) * d, m.getY() + sy / 2.0 + 0.6, cz - Math.cos(a) * d, new Vec3(cx, m.getY() + sy / 2.0, cz));
                });
                glassShot(pack, view, ti);
            }
        }
    }

    /**
     * Every view three times: a reference with every face of the tank frameless ({@code _ref}, no frame anywhere), the old way
     * ({@code _stretched}: the frame hidden by stretching the UVs, at full size only) and the frameless copies of M64. Inside
     * the front face (a third of a block away from its outer edge, where the tank's own frame stays) the M64 picture must equal
     * the reference; the old way may differ (the frame of every pane in the smaller mipmaps).
     */
    private void glassShot(String pack, String view, int tank) {
        Cam[] cam = new Cam[1];
        // ref2: the reference once more (the noise of a shader pack's animation between two pictures)
        String[] modes = {"ref", "stretched", "new", "ref2"};
        for (String mode : modes) {
            String v = mode.equals("new") ? view : view + "_" + mode;
            then(1, () -> {
                org.overengineer.bettacraft.client.tank.GlassVariants.allFramelessForTest = mode.startsWith("ref");
                org.overengineer.bettacraft.client.tank.GlassVariants.offForTest = mode.equals("stretched");
                mc.levelRenderer.allChanged();
            });
            then(60, () -> {
                cam[0] = new Cam(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), mc.options.fov().get());
                shot(pack, v);
            });
        }
        then(1, () -> {
            org.overengineer.bettacraft.client.tank.GlassVariants.allFramelessForTest = false;
            org.overengineer.bettacraft.client.tank.GlassVariants.offForTest = false;
        });
        then(15, () -> {
            double old = framesInside(pack, view, view + "_stretched", tank, cam[0]);
            double now = framesInside(pack, view, view, tank, cam[0]);
            double noise = Math.max(0, framesInside(pack, view, view + "_ref2", tank, cam[0]));
            if (old < -1.5 || now < -1.5) {
                problems++;
                guard(false, "glass " + pack + " " + view + ": pictures missing");
                return;
            }
            if (now < 0) {
                guard(true, "glass " + pack + " " + view + ": not checked (the face is too small on screen)");
                return;
            }
            // the grid back would make M64 as bad as the old way; at flat angles some packs (BSL) sample a footprint wider than the one
            // block margin, so the tank's own corner frame reaches into the compared area: at least ten times cleaner than the old way
            boolean ok = now <= 0.5 + 1.5 * noise || now <= 2.0 && now <= old * 0.1;
            if (!ok) problems++;
            summary.add(String.format(Locale.ROOT, "%s glass %s %s: inner frames old way %.2f %%, M64 %.2f %% (noise %.2f %%)", ok ? "PASS" : "FAIL", pack, view, old, now, noise));
            LOGGER.info("[smoketest] M64 guard {} glass {} {}: pixels inside the face that differ from the frameless reference: old way {} %, M64 {} % (noise {} %)",
                    ok ? "PASS" : "FAIL", pack, view, String.format(Locale.ROOT, "%.2f", old), String.format(Locale.ROOT, "%.2f", now),
                    String.format(Locale.ROOT, "%.2f", noise));
        });
    }

    /**
     * Share (percent) of the pixels of the inner panes of the tank's front face (one block in from its outer edge) that differ clearly
     * from the frameless reference; -1 when the face covers too few pixels, -2 when a picture is missing.
     */
    private double framesInside(String pack, String view, String test, int tank, Cam cam) {
        File dir = new File(mc.gameDirectory, "screenshots");
        BufferedImage ref, img;
        try {
            ref = javax.imageio.ImageIO.read(new File(dir, "bettacraft_m64_" + TAG + "_" + pack + "_" + view + "_ref.png"));
            img = javax.imageio.ImageIO.read(new File(dir, "bettacraft_m64_" + TAG + "_" + pack + "_" + test + ".png"));
        } catch (java.io.IOException e) {
            return -2;
        }
        if (ref == null || img == null || ref.getWidth() != img.getWidth() || ref.getHeight() != img.getHeight()) return -2;
        BlockPos m = glass[tank];
        int sx = (int) GLASS_TANKS[tank][1], sy = (int) GLASS_TANKS[tank][2];
        // the inner panes only: the outer row keeps the tank's own frame on its outer edge, and at the smallest mip levels (far away,
        // flat angles, shader packs that sample lower levels) that frame tints the whole outer pane - no grid, the tank's edge
        double in = 1.02, z = m.getZ() - 0.002;
        int[][] c = {project(img, cam, m.getX() + in, m.getY() + in, z), project(img, cam, m.getX() + sx - in, m.getY() + in, z),
                project(img, cam, m.getX() + sx - in, m.getY() + sy - in, z), project(img, cam, m.getX() + in, m.getY() + sy - in, z)};
        for (int[] p : c) if (p == null) return -1;
        int x0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y0 = Integer.MAX_VALUE, y1 = Integer.MIN_VALUE;
        for (int[] p : c) {
            x0 = Math.min(x0, p[0]);
            x1 = Math.max(x1, p[0]);
            y0 = Math.min(y0, p[1]);
            y1 = Math.max(y1, p[1]);
        }
        long n = 0, differ = 0;
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
            if (!inside(c, x, y)) continue;
            n++;
            if (Math.abs(luma(img, x, y) - luma(ref, x, y)) > 6) differ++;
        }
        if (n < 200) return -1;
        return differ * 100.0 / n;
    }

    /** Point in the convex quad (screen corners in order). */
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

    /** Screen pixel of a world point (the camera of the picture), or null when behind the camera or off screen. */
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

    // ------------------------------------------------------------------ hoses, corals, floating plants, lids

    private void hoseViews(String pack) {
        then(1, () -> lookAt(hoseTank.getX() + 2.0, hoseTank.getY() + 1.4, hoseTank.getZ() - 2.6, new Vec3(hoseTank.getX() + 2.0, hoseTank.getY() + 0.5, hoseTank.getZ() + 1.5)));
        then(80, () -> shot(pack, "hose_front"));
        then(1, () -> lookAt(hoseTank.getX() + 2.0, hoseTank.getY() + 4.2, hoseTank.getZ() - 1.0, new Vec3(hoseTank.getX() + 2.0, hoseTank.getY(), hoseTank.getZ() + 1.5)));
        then(40, () -> shot(pack, "hose_above"));
        then(1, () -> lookAt(cabTank.getX() + 1.5, cabTank.getY() + 1.4, cabTank.getZ() - 2.4, new Vec3(cabTank.getX() + 1.5, cabTank.getY() + 0.5, cabTank.getZ() + 1.5)));
        then(60, () -> shot(pack, "hose_cabinet_front"));
        then(1, () -> lookAt(cabTank.getX() + 1.5, cabTank.getY() + 4.0, cabTank.getZ() - 0.8, new Vec3(cabTank.getX() + 1.5, cabTank.getY(), cabTank.getZ() + 1.5)));
        then(40, () -> shot(pack, "hose_cabinet_above"));
    }

    private void coralViews(String pack) {
        // outside: the miniature close up
        then(1, () -> lookAt(coralTank.getX() + 2.0, coralTank.getY() + 1.0, coralTank.getZ() - 1.6, new Vec3(coralTank.getX() + 2.0, coralTank.getY() + 0.4, coralTank.getZ() + 1.5)));
        then(100, () -> shot(pack, "coral_outside_front"));
        then(1, () -> lookAt(coralTank.getX() + 1.0, coralTank.getY() + 0.5, coralTank.getZ() - 0.9, new Vec3(coralTank.getX() + 1.2, coralTank.getY() + 0.25, coralTank.getZ() + 1.0)));
        then(40, () -> shot(pack, "coral_outside_low"));
        // inside: every mushroom coral from four sides and from below the discs
        for (int i = 0; i < 3; i++) {
            final int mi = i;
            float[] yaws = {0F, 90F, 180F, 270F};
            for (float yaw : yaws) {
                String view = "coral_mushroom" + i + "_" + (int) yaw;
                then(1, () -> inside(mi, yaw, 7.0, 3.5, 12F));
                then(mi == 0 && yaw == 0F ? 100 : 40, () -> shot(pack, view));
            }
            then(1, () -> inside(mi, 0F, 5.0, 0.8, -8F));
            then(40, () -> shot(pack, "coral_mushroom" + mi + "_low"));
        }
    }

    /** The camera in the interior, {@code dist} blocks from mushroom coral {@code i} on the side {@code yaw} looks from. */
    private void inside(int i, float yaw, double dist, double up, float pitch) {
        server(p -> {
            int[] mu = mushrooms.get(i);
            BlockPos root = coralRegion.toWorld(mu[0], mu[1], mu[2]);
            double yr = Math.toRadians(yaw);
            double x = root.getX() + 0.5 + Math.sin(yr) * dist, z = root.getZ() + 0.5 - Math.cos(yr) * dist;
            p.teleportTo(InteriorManager.interiorLevel(p.level().getServer()), x, root.getY() + up - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, false);
        });
    }

    private void floatViews(String pack) {
        then(1, () -> lookAt(floatTank.getX() + 2.0, floatTank.getY() + 1.2, floatTank.getZ() - 2.4, new Vec3(floatTank.getX() + 2.0, floatTank.getY() + 1.2, floatTank.getZ() + 1.5)));
        then(80, () -> shot(pack, "float_front_a"));
        then(9, () -> shot(pack, "float_front_b"));
        then(1, () -> lookAt(floatTank.getX() + 2.0, floatTank.getY() + 0.6, floatTank.getZ() - 1.6, new Vec3(floatTank.getX() + 2.0, floatTank.getY() + 1.5, floatTank.getZ() + 1.5)));
        then(40, () -> shot(pack, "float_below"));
    }

    private void lidViews(String pack) {
        then(1, () -> lookAt(lidTank.getX() + 2.0, lidTank.getY() + 3.0, lidTank.getZ() - 3.0, new Vec3(lidTank.getX() + 2.0, lidTank.getY() + 2.0, lidTank.getZ() + 1.5)));
        then(60, () -> shot(pack, "lid_front"));
        then(1, () -> lookAt(lidTank.getX() + 5.6, lidTank.getY() + 2.4, lidTank.getZ() + 1.5, new Vec3(lidTank.getX() + 2.0, lidTank.getY() + 2.0, lidTank.getZ() + 1.5)));
        then(40, () -> shot(pack, "lid_side"));
        then(1, () -> lookAt(panelTank.getX() + 2.0, panelTank.getY() + 3.0, panelTank.getZ() - 3.0, new Vec3(panelTank.getX() + 2.0, panelTank.getY() + 2.0, panelTank.getZ() + 1.5)));
        then(40, () -> shot(pack, "lid_panel_front"));
        then(1, () -> server(p -> {
            ServerLevel level = p.level();
            for (BlockPos t : new BlockPos[] {lidTank, panelTank}) {
                TankBlockEntity c = TankMultiblock.getController(level, t);
                int[] top = TankWater.openTop(level, c.getTank());
                boolean mobs = org.overengineer.bettacraft.mobs.GlassMobs.openTop(level, c.getTank());
                LOGGER.info("[smoketest] M64 lid {}: {} of {} tops open, open for mobs and fliers {}", t.equals(lidTank) ? "lid tank" : "panel tank",
                        top[0], top[1], mobs);
                summary.add("lid " + (t.equals(lidTank) ? "lid tank" : "panel tank") + ": " + top[0] + " of " + top[1] + " tops open, open for mobs " + mobs);
            }
        }));
    }

    // ------------------------------------------------------------------ scene

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        ServerLevel in = InteriorManager.interiorLevel(p.level().getServer());
        DevClientWorldTour.clearWeather(level);
        origin = p.blockPosition().offset(-30, 0, 260);
        // the glass tanks on planks, the field in front of them empty (the far views look over it)
        // the field first (the second tank's field must not clear the first tank)
        for (Object[] gt : GLASS_TANKS) {
            BlockPos base = origin.offset((int) gt[4], 0, (int) gt[5]);
            clear(level, base.offset(-8, 0, -155), (int) gt[1] + 16, 12, 155 + (int) gt[3] + 6);
        }
        for (int t = 0; t < GLASS_TANKS.length; t++) {
            int sx = (int) GLASS_TANKS[t][1], sy = (int) GLASS_TANKS[t][2], sz = (int) GLASS_TANKS[t][3];
            BlockPos base = origin.offset((int) GLASS_TANKS[t][4], 0, (int) GLASS_TANKS[t][5]);
            for (int x = 0; x < sx; x++) for (int z = 0; z < sz; z++) level.setBlockAndUpdate(base.offset(x, 0, z), Blocks.OAK_PLANKS.defaultBlockState());
            glass[t] = base.offset(0, 1, 0);
            Identifier g = Identifier.withDefaultNamespace((String) GLASS_TANKS[t][0]);
            TankBlockEntity c = tank(level, p, glass[t], sx, sy, sz, "M64 " + g.getPath(), g, TankStyle.TEXTURED, 0F);
            // empty: the water's shimmer would differ between the pictures of the guard
            TankInteractions.changeWater(level, c, -c.getTank().waterMb(), 0F);
            sand(level, in, c);
        }
        // the hose tank: an air pump beside it, two air stones and a sponge filter inside; a tank on cabinets with a pump in one
        BlockPos row = origin.offset(40, 0, 0);
        clear(level, row.offset(-4, 0, -8), 80, 10, 16);
        hoseTank = row.offset(0, 1, 0);
        planks(level, hoseTank.below(), 4, 3);
        TankBlockEntity h = tank(level, p, hoseTank, 4, 2, 3, "M64 hoses", Identifier.withDefaultNamespace("glass"), TankStyle.CLEAN, 0F);
        level.setBlockAndUpdate(hoseTank.offset(-1, 0, 1), ModBlocks.AIR_PUMP.get().defaultBlockState().setValue(AirPumpBlock.FACING, Direction.EAST));
        InteriorRegion rh = sand(level, in, h);
        for (int[] s : new int[][] {{14, 10}, {40, 34}}) in.setBlock(rh.toWorld(s[0], 0, s[1]), ModBlocks.AIR_STONE.get().defaultBlockState(), 3);
        in.setBlock(rh.toWorld(52, 0, 12), ModBlocks.SPONGE_FILTER_SEGMENT.get().defaultBlockState()
                .setValue(org.overengineer.bettacraft.equipment.SegmentBlock.PART, org.overengineer.bettacraft.equipment.SegmentBlock.Part.ANCHOR), 3);
        org.overengineer.bettacraft.tank.TankEquipment.invalidate(h);
        org.overengineer.bettacraft.tank.TankAeration.step(level, h, 0.01);
        cabTank = row.offset(8, 1, 0);
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++)
            level.setBlockAndUpdate(cabTank.offset(x, -1, z), ModBlocks.CABINET.get().defaultBlockState().setValue(org.overengineer.bettacraft.equipment.CabinetBlock.FACING, Direction.NORTH));
        TankBlockEntity cb = tank(level, p, cabTank, 3, 2, 3, "M64 cabinet hoses", Identifier.withDefaultNamespace("glass"), TankStyle.CLEAN, 0F);
        if (level.getBlockEntity(cabTank.below()) instanceof org.overengineer.bettacraft.equipment.CabinetBlockEntity cab) {
            cab.items().setStackInSlot(org.overengineer.bettacraft.equipment.CabinetBlockEntity.FILTER_SLOT, new net.minecraft.world.item.ItemStack(org.overengineer.bettacraft.registry.ModItems.EXTERNAL_FILTER.get()));
            cab.items().setStackInSlot(org.overengineer.bettacraft.equipment.CabinetBlockEntity.AIR_PUMP_SLOT, new net.minecraft.world.item.ItemStack(org.overengineer.bettacraft.registry.ModItems.AIR_PUMP.get()));
            cab.setOutletSide(org.overengineer.bettacraft.equipment.OutletSide.LEFT);
        }
        InteriorRegion rc = sand(level, in, cb);
        for (int[] s : new int[][] {{10, 30}, {34, 14}}) in.setBlock(rc.toWorld(s[0], 0, s[1]), ModBlocks.AIR_STONE.get().defaultBlockState(), 3);
        org.overengineer.bettacraft.tank.TankEquipment.invalidate(cb);
        org.overengineer.bettacraft.tank.TankAeration.step(level, cb, 0.01);
        // the reef tank: reef rock mounds, three mushroom corals and the other corals
        coralTank = row.offset(15, 1, 0);
        planks(level, coralTank.below(), 4, 3);
        TankBlockEntity rt = tank(level, p, coralTank, 4, 3, 3, "M64 reef", Identifier.withDefaultNamespace("glass"), TankStyle.CLEAN, Salinity.SEA);
        InteriorRegion rr = sand(level, in, rt);
        coralRegion = rr;
        mushrooms.clear();
        Object[][] corals = {{PlantSpecies.MUSHROOM_CORAL, 12, 14}, {PlantSpecies.MUSHROOM_CORAL, 30, 12}, {PlantSpecies.MUSHROOM_CORAL, 50, 16},
                {PlantSpecies.LEATHER_CORAL, 10, 34}, {PlantSpecies.HAMMER_CORAL, 24, 34}, {PlantSpecies.BUBBLE_TIP_ANEMONE, 38, 34},
                {PlantSpecies.ZOANTHUS, 54, 34}, {PlantSpecies.ACROPORA, 22, 24}, {PlantSpecies.LOBOPHYLLIA, 44, 26}};
        long seed = 64100;
        for (Object[] co : corals) {
            PlantSpecies sp = (PlantSpecies) co[0];
            int x = (int) co[1], z = (int) co[2];
            mound(in, rr, x, z, 3, sp == PlantSpecies.MUSHROOM_CORAL ? 3 : 4);
            int y = 0;
            while (y < rr.sizeY() && !in.getBlockState(rr.toWorld(x, y, z)).isAir()) y++;
            InteriorPlant pl = InteriorPlants.plantAt(in, rr, sp, x, y, z, sp.stages(), seed++);
            if (pl == null) {
                LOGGER.error("[smoketest] M64 {} not planted", sp.id());
                continue;
            }
            pl.tint = 2;
            pl.redness = 0.67;
            InteriorPlants.apply(in, rr, pl);
            if (sp == PlantSpecies.MUSHROOM_CORAL) mushrooms.add(new int[] {x, y, z});
        }
        // the floating plants: hornwort, frogbit, water lettuce, salvinia, red root floater
        floatTank = row.offset(22, 1, 0);
        planks(level, floatTank.below(), 4, 3);
        TankBlockEntity ft = tank(level, p, floatTank, 4, 3, 3, "M64 floating plants", Identifier.withDefaultNamespace("glass"), TankStyle.CLEAN, 0F);
        sand(level, in, ft);
        FloatingPlantType[] types = {FloatingPlantType.HORNWORT, FloatingPlantType.FROGBIT, FloatingPlantType.HORNWORT, FloatingPlantType.WATER_LETTUCE,
                FloatingPlantType.SALVINIA, FloatingPlantType.HORNWORT, FloatingPlantType.RED_ROOT_FLOATER, FloatingPlantType.FROGBIT};
        for (int i = 0; i < types.length; i++) {
            TankPlants.add(ft, types[i], floatTank.getX() + 0.5 + (i % 4) * 0.95, floatTank.getZ() + 0.7 + (i / 4) * 1.2, 1F, i * 1.1F);
        }
        ft.markTankChanged();
        // lids with a light bar on them (on the lid itself where the light knows it, else one block above as before) and an
        // LED panel directly on a tank
        lidTank = row.offset(29, 1, 0);
        planks(level, lidTank.below(), 4, 3);
        tank(level, p, lidTank, 4, 2, 3, "M64 lids", Identifier.withDefaultNamespace("glass"), TankStyle.CLEAN, 0F);
        BlockState bar = ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X).setValue(TankLightBlock.LIT, true);
        @SuppressWarnings("unchecked")
        Property<Boolean> lid = (Property<Boolean>) bar.getBlock().getStateDefinition().getProperty("lid");
        for (int x = 0; x < 4; x++) for (int z = 0; z < 3; z++) {
            BlockPos top = lidTank.offset(x, 2, z);
            if (z == 1 && lid instanceof BooleanProperty lp) {
                level.setBlockAndUpdate(top, bar.setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 3).setValue(lp, true));
            } else {
                level.setBlockAndUpdate(top, ModBlocks.TANK_LID.get().defaultBlockState());
                if (z == 1) level.setBlockAndUpdate(top.above(), bar.setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 3));
            }
        }
        LOGGER.info("[smoketest] M64 the light knows lids: {}", lid != null);
        panelTank = row.offset(36, 1, 0);
        planks(level, panelTank.below(), 4, 2);
        tank(level, p, panelTank, 4, 2, 2, "M64 panel", Identifier.withDefaultNamespace("glass"), TankStyle.CLEAN, 0F);
        BlockState panel = ModBlocks.LED_PANEL.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X).setValue(TankLightBlock.LIT, true);
        for (int x = 0; x < 4; x++) {
            level.setBlockAndUpdate(panelTank.offset(x, 2, 0), panel.setValue(LedPanelBlock.PARTNER_POS, true).setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 3));
            level.setBlockAndUpdate(panelTank.offset(x, 2, 1), panel.setValue(LedPanelBlock.PARTNER_POS, false).setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 3));
        }
        built = true;
        LOGGER.info("[smoketest] M64 scene at {}: glass tanks {} / {}, hoses {}, cabinet {}, reef {}, floating {}, lids {}, panel {}", origin, glass[0], glass[1],
                hoseTank, cabTank, coralTank, floatTank, lidTank, panelTank);
    }

    private static void clear(ServerLevel level, BlockPos min, int sx, int sy, int sz) {
        for (int x = 0; x < sx; x++) for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) {
            BlockPos pos = min.offset(x, y, z);
            if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (int x = 0; x < sx; x++) for (int z = 0; z < sz; z++) level.setBlock(min.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    private static void planks(ServerLevel level, BlockPos min, int sx, int sz) {
        for (int x = 0; x < sx; x++) for (int z = 0; z < sz; z++) level.setBlockAndUpdate(min.offset(x, 0, z), Blocks.OAK_PLANKS.defaultBlockState());
    }

    private static InteriorRegion sand(ServerLevel level, ServerLevel in, TankBlockEntity c) {
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++)
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.SAND.defaultBlockState(), false);
        return r;
    }

    private static void mound(ServerLevel in, InteriorRegion r, int cx, int cz, int rad, int h) {
        BlockState a = ModBlocks.REEF_ROCK.get().defaultBlockState(), b = ModBlocks.LIVE_ROCK.get().defaultBlockState();
        for (int x = -rad; x <= rad; x++)
            for (int z = -rad; z <= rad; z++) {
                double d = Math.sqrt(x * x + z * z) / rad;
                if (d > 1) continue;
                int top = (int) Math.round(h * Math.max(0.2, 1 - d * d * 0.8));
                for (int y = 0; y < top; y++) in.setBlock(r.toWorld(cx + x, y, cz + z), (x + y + z) % 3 == 0 ? b : a, 2);
            }
    }

    private static TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz, String name, Identifier glassId,
                                        TankStyle style, float salt) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) {
            level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
            if (level.getBlockEntity(pos) instanceof TankBlockEntity be) {
                be.setGlassId(glassId);
                be.setStyle(style);
            }
        }
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, (long) sx * sy * sz * 1000, salt);
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
        server(p -> DevClientWorldTour.clearWeather(p.level()));
        server(p -> p.level().setDayTime(t));
    }

    // ------------------------------------------------------------------ shader packs (Iris, by reflection; dev only)

    private static List<String> packs() {
        String wanted = System.getProperty("bettacraft.smoketest.m64packs", DEFAULT_PACKS);
        List<String> out = new ArrayList<>();
        boolean iris = irisClass() != null;
        File dir = new File(Minecraft.getInstance().gameDirectory, "shaderpacks");
        for (String s : wanted.split(",")) {
            String pack = s.trim();
            if (pack.isEmpty()) continue;
            if (pack.equals("none")) out.add(pack);
            else if (!iris) LOGGER.info("[smoketest] M64 pack {} skipped: no Iris", pack);
            else if (!new File(dir, pack).exists()) LOGGER.info("[smoketest] M64 pack {} skipped: not in {}", pack, dir);
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
            LOGGER.warn("[smoketest] M64 could not read the Iris config", t);
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
            LOGGER.error("[smoketest] M64 FAIL: could not switch Iris to {} ({})", pack, enabled, t);
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
        LOGGER.info("[smoketest] M64 guard {}", l);
    }

    private void writeSummary() {
        try {
            File dir = new File(mc.gameDirectory, "screenshots");
            java.nio.file.Files.write(new File(dir, "bettacraft_m64_" + TAG + "_summary.txt").toPath(), summary, java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            LOGGER.warn("[smoketest] M64 summary not written", e);
        }
    }

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> {
            ServerLevel to = InteriorManager.isInterior(p.level()) ? p.level().getServer().overworld() : p.level();
            p.teleportTo(to, x, eyeY - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, false);
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
        // the first step starts the build on the server; the others wait for it (and for the teleports it queues behind)
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
        mc.getToastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m64_" + TAG + "_" + pack + "_" + view + ".png", mc.getMainRenderTarget(), 1, c -> {});
    }
}
