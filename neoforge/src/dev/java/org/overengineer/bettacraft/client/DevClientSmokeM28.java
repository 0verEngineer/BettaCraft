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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.client.interior.IrisCompat;
import org.overengineer.bettacraft.client.interior.MiniaturePlants;
import org.overengineer.bettacraft.client.interior.PlantMeshRenderer;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.equipment.HighPowerLedBlock;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M28 part a dev client smoke scene ({@code -Psmoketestm28}): the new Anubias and Bucephalandras on stone and wood, the
 * new stem plants (Hygrophila pinnatifida on a log) under high-power LEDs, the white and pink Bucephalandras under the
 * light bar and under a high-power LED; outside near and far (with the meshes and, for comparison, without), inside close
 * up and wide. Screenshots {@code bettacraft_m28_*}; with {@code -Pshaders} it checks the shadow pass.
 */
final class DevClientSmokeM28 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    /** A plant of the scene: where it stands (interior-local), in which tank, on wood or stone (epiphytes). */
    private record Spot(PlantSpecies species, int tank, int x, int z, boolean wood) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final BlockPos[] tanks = new BlockPos[4];
    private final int[] widths = {4, 4, 3, 3};
    /** High-power LEDs over the stem plants and the second white tank, the light bar over the others. */
    private final boolean[] leds = {false, true, false, true};
    private final InteriorRegion[] regions = new InteriorRegion[4];
    private final List<Spot> spots = new ArrayList<>();
    private final List<int[]> tops = new ArrayList<>();
    private int wait;
    private final boolean savedMeshes = ClientConfig.plantMeshes();

    DevClientSmokeM28() {
        PlantSpecies[] epis = {PlantSpecies.ANUBIAS_PETITE, PlantSpecies.ANUBIAS_PINTO, PlantSpecies.ANUBIAS_WHITE, PlantSpecies.ANUBIAS_COFFEEFOLIA,
                PlantSpecies.ANUBIAS_ROUND_LEAF, PlantSpecies.BUCE_KEDAGANG, PlantSpecies.BUCE_THEIA, PlantSpecies.BUCE_GODZILLA};
        int[][] ep = {{8, 11}, {22, 11}, {36, 11}, {51, 11}, {10, 23}, {24, 23}, {38, 23}, {52, 23}};
        for (int i = 0; i < epis.length; i++) spots.add(new Spot(epis[i], 0, ep[i][0], ep[i][1], i == 3 || i == 4 || i == 7));
        PlantSpecies[] stems = {PlantSpecies.LUDWIGIA_SUPER_RED, PlantSpecies.ROTALA_HRA, PlantSpecies.ROTALA_WALLICHII, PlantSpecies.HYGROPHILA_PINNATIFIDA,
                PlantSpecies.POGOSTEMON_DOWNOI, PlantSpecies.LIMNOPHILA_SESSILIFLORA, PlantSpecies.MYRIOPHYLLUM_RED, PlantSpecies.BACOPA_CAROLINIANA};
        int[][] st = {{8, 22}, {22, 22}, {36, 22}, {52, 20}, {10, 9}, {26, 12}, {42, 12}, {56, 9}};
        for (int i = 0; i < stems.length; i++) spots.add(new Spot(stems[i], 1, st[i][0], st[i][1], stems[i] == PlantSpecies.HYGROPHILA_PINNATIFIDA));
        PlantSpecies[] white = {PlantSpecies.BUCE_PINK_LADY, PlantSpecies.BUCE_MINI_PINK, PlantSpecies.BUCE_SNOW_WHITE};
        for (int t = 2; t <= 3; t++) for (int i = 0; i < 3; i++) spots.add(new Spot(white[i], t, 9 + i * 15, 15, false));

        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(1, () -> meshes(true));
        // outside: every tank near and far; the first two also without the meshes (block models) for comparison
        for (boolean on : new boolean[] {true, false}) {
            String tag = on ? "" : "_old";
            then(1, () -> meshes(on));
            for (int t = 0; t < (on ? 4 : 2); t++) {
                int ti = t;
                then(1, () -> look(tanks[ti].getX() + widths[ti] / 2.0, tanks[ti].getY() + 1.2, tanks[ti].getZ() - 2.6, 0F, 6F));
                then(ti == 0 && on ? 200 : 70, () -> {
                    if (on) {
                        LOGGER.info("[smoketest] M28 outside tank {}: {} tank(s) with meshes, {} plants drawn, {} ms render thread", ti,
                                MiniaturePlants.lastTanks, MiniaturePlants.lastDrawn, MiniaturePlants.lastNanos / 1e6);
                        if (MiniaturePlants.lastTanks < 1 || MiniaturePlants.lastDrawn < 3) LOGGER.error("[smoketest] M28 plant meshes not shown outside (tank {})", ti);
                    }
                    shot("outside_" + name(ti) + tag);
                });
                if (on) {
                    then(1, () -> look(tanks[ti].getX() + widths[ti] / 2.0, tanks[ti].getY() + 3.0, tanks[ti].getZ() - 11.0, 0F, 9F));
                    then(50, () -> shot("outside_" + name(ti) + "_far"));
                }
            }
        }
        then(1, () -> meshes(true));
        // both white tanks side by side: greenish under the bar, white under the LED
        then(1, () -> look(tanks[2].getX() + 4.0, tanks[2].getY() + 1.4, tanks[2].getZ() - 4.6, 0F, 8F));
        then(60, () -> shot("outside_white_compare"));
        // shadows with a shader pack (the meshes of the miniature in the shadow pass)
        then(1, () -> look(tanks[1].getX() + 2.0, tanks[1].getY() + 1.2, tanks[1].getZ() - 2.6, 0F, 6F));
        then(60, () -> {
            if (IrisCompat.shaderPackInUse()) {
                LOGGER.info("[smoketest] M28 shadow pass: {} meshes", PlantMeshRenderer.lastShadowDrawn);
                if (PlantMeshRenderer.lastShadowDrawn == 0) LOGGER.error("[smoketest] M28 plant meshes not in the shadow pass");
                shot("outside_stems_shaders");
            }
        });
        // inside: every species close up (the white ones under the LED), then every tank wide
        for (int i = 0; i < spots.size(); i++) {
            int si = i;
            if (spots.get(si).tank() == 2) continue;
            then(1, () -> insideAt(spots.get(si)));
            then(si == 0 ? 100 : 40, () -> shot("inside_" + spots.get(si).species().id()));
        }
        for (int t = 0; t < 4; t++) {
            int ti = t;
            then(1, () -> insideWide(ti));
            then(60, () -> shot("inside_" + name(ti) + "_wide"));
        }
        then(1, () -> ClientConfig.PLANT_MESHES.set(savedMeshes));
    }

    private static String name(int t) {
        return switch (t) {
            case 0 -> "epiphytes";
            case 1 -> "stems";
            case 2 -> "white_bar";
            default -> "white_led";
        };
    }

    private int top(Spot s) {
        for (int[] t : tops) if (t[0] == s.tank() && t[1] == s.x() && t[2] == s.z()) return t[3];
        return 0;
    }

    private void insideAt(Spot s) {
        server(p -> {
            InteriorRegion r = regions[s.tank()];
            int size = switch (s.species().form()) {
                case STEM -> s.species() == PlantSpecies.POGOSTEMON_DOWNOI ? 12 : 20;
                default -> s.species() == PlantSpecies.ANUBIAS_ROUND_LEAF || s.species() == PlantSpecies.ANUBIAS_COFFEEFOLIA ? 15 : 11;
            };
            int y = top(s);
            BlockPos at = r.toWorld(s.x(), y + size / 3, Math.max(1, s.z() - size));
            p.teleportTo(InteriorManager.interiorLevel(p.level().getServer()), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, java.util.Set.of(), 0F, 12F, true);
        });
    }

    private void insideWide(int t) {
        server(p -> {
            InteriorRegion r = regions[t];
            BlockPos at = r.toWorld(r.sizeX() / 2, 14, 1);
            p.teleportTo(InteriorManager.interiorLevel(p.level().getServer()), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, java.util.Set.of(), 0F, 22F, true);
        });
    }

    private void meshes(boolean on) {
        ClientConfig.PLANT_MESHES.set(on);
        if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
    }

    // ------------------------------------------------------------------ the scene

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, boolean led) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, (long) sx * 4 * 1000, 0F);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(p.getUUID());
        c.getTank().setWaterTemp(25F);
        c.getTank().setTargetTemp(25F);
        c.markTankChanged();
        for (int x = 0; x < sx; x++) {
            if (led) {
                for (int z = 0; z < 2; z++) {
                    level.setBlockAndUpdate(min.offset(x, 2, z), ModBlocks.HIGH_POWER_LED.get().defaultBlockState().setValue(TankLightBlock.NEG, x > 0)
                            .setValue(TankLightBlock.POS, x < sx - 1).setValue(HighPowerLedBlock.NORTH, z > 0).setValue(HighPowerLedBlock.SOUTH, z < 1)
                            .setValue(TankLightBlock.LIT, true).setValue(TankLightBlock.COLOR, DyeColor.WHITE));
                }
            } else {
                level.setBlockAndUpdate(min.offset(x, 2, 1), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                        .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < sx - 1).setValue(TankLightBlock.LIT, true)
                        .setValue(TankLightBlock.COLOR, DyeColor.WHITE));
            }
        }
        return c;
    }

    private static void mound(ServerLevel in, InteriorRegion r, int cx, int cz, int rad, int h, BlockState a, BlockState b) {
        for (int x = -rad; x <= rad; x++)
            for (int z = -rad; z <= rad; z++) {
                double d = Math.sqrt(x * x + z * z) / rad;
                if (d > 1) continue;
                int top = (int) Math.round(h * Math.max(0.2, 1 - d * d * 0.8));
                for (int y = 0; y < top; y++) in.setBlock(r.toWorld(cx + x, y, cz + z), (x + y + z) % 3 == 0 ? b : a, 2);
            }
    }

    private static int topAt(ServerLevel in, InteriorRegion r, int x, int z) {
        int y = 0;
        while (y < r.sizeY() && !in.getBlockState(r.toWorld(x, y, z)).isAir()) y++;
        return y;
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        ServerLevel in = InteriorManager.interiorLevel(p.level().getServer());
        org.overengineer.bettacraft.gametest.framework.DevTime.set(level, 6000);
        BlockPos base = p.blockPosition().offset(-12, 2, 380);
        int x0 = 0;
        for (int t = 0; t < 4; t++) {
            tanks[t] = base.offset(x0, 0, 0);
            x0 += widths[t] + 2;
            TankBlockEntity c = tank(level, p, tanks[t], widths[t], leds[t]);
            InteriorRegion r = InteriorManager.ensureRegion(level, c);
            regions[t] = r;
            BlockState floor = t == 1 ? Blocks.DIRT.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
            for (int x = 0; x < r.sizeX(); x++)
                for (int z = 0; z < r.sizeZ(); z++) Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), floor, false);
        }
        long seed = 28100;
        for (Spot s : spots) {
            InteriorRegion r = regions[s.tank()];
            if (s.species().form() == PlantSpecies.Form.EPIPHYTE || s.wood()) {
                BlockState a = s.wood() ? Blocks.OAK_LOG.defaultBlockState() : Blocks.STONE.defaultBlockState();
                mound(in, r, s.x(), s.z(), 3, s.wood() && s.tank() == 1 ? 3 : 4, a, s.wood() ? a : Blocks.ANDESITE.defaultBlockState());
            }
            int y = topAt(in, r, s.x(), s.z());
            tops.add(new int[] {s.tank(), s.x(), s.z(), y});
            InteriorPlant pl = InteriorPlants.plantAt(in, r, s.species(), s.x(), y, s.z(), s.species().stages(), seed++);
            if (pl == null) LOGGER.error("[smoketest] M28 {} not planted", s.species().id());
        }
        for (int t = 0; t < 4; t++) InteriorPlants.settleTint(level, TankMultiblock.getController(level, tanks[t]));
        int bar = -1, led = -1;
        for (InteriorPlant pl : regions[2].plants().all()) bar = Math.max(bar, pl.tint);
        for (InteriorPlant pl : regions[3].plants().all()) led = Math.max(led, pl.tint);
        LOGGER.info("[smoketest] M28 white Bucephalandras: tint {} under the light bar, {} under the high-power LED", bar, led);
        if (!(led > bar)) LOGGER.error("[smoketest] M28 the white Bucephalandras are not whiter under the LED");
        int blocks = 0;
        for (InteriorRegion r : regions) blocks += r.plants().blocks();
        LOGGER.info("[smoketest] M28 scene at {}: {} plants, {} plant blocks", base, spots.size(), blocks);
    }

    // ------------------------------------------------------------------ steps

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> {
            if (InteriorManager.isInterior(p.level())) p.teleportTo(p.level().getServer().overworld(), x, eyeY - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, true);
            else p.teleportTo(p.level(), x, eyeY - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, false);
        });
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
        mc.getToastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m28_" + name + ".png", mc.getMainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
