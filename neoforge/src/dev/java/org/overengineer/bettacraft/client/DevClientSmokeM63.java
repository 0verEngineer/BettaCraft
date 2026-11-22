package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M63 client smoke ({@code -Psmoketestm63}, NeoForge and Fabric, background window): the water of the tanks with and without
 * shader packs. Two tanks: a 5 x 3 x 3 tank in a closed room lit unevenly by a torch, a lantern and a glowstone (the
 * block light differs from block to block, the situation of the report: block-sized rectangles on the surface with
 * Complementary), and a 6 x 2 x 3 tank outside, half under a roof (sky light differs). For every pack of
 * {@code bettacraft.smoketest.m63packs} (default: none and every pack of the manifest found in {@code shaderpacks/}) the
 * scene switches Iris to it and takes the same views: from the front just below the surface looking up, from the front,
 * from the side, from above, from high above, and the outside tank at night. Screenshots
 * {@code bettacraft_m63_<tag>_<pack>_<view>.png} ({@code -Pm63tag}, default {@code run}). The pack Iris had before is
 * restored at the end.
 */
final class DevClientSmokeM63 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static final Identifier GLASS = Identifier.withDefaultNamespace("glass");
    private static final String TAG = System.getProperty("bettacraft.smoketest.m63tag", "run");
    private static final String DEFAULT_PACKS = "none,ComplementaryReimagined_r5.10.zip,ComplementaryUnbound_r5.10.zip,BSL_v10.1.8.zip,MakeUp-UltraFast-9.6.zip";

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private volatile BlockPos room, outside;
    private int wait;
    private int problems;
    private String oldPack;
    private boolean oldEnabled;

    DevClientSmokeM63() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(1, this::rememberIris);
        List<String> packs = packs();
        LOGGER.info("[smoketest] M63 packs: {}", packs);
        for (String pack : packs) {
            String name = shortName(pack);
            then(1, () -> switchPack(pack));
            then(1, () -> time(6000));
            // shader compile and the rebuild of every chunk
            then(200, () -> logPack(pack));
            // the room: just below the surface looking up (the view of the report), front, side, above, high above
            then(1, () -> lookAt(room.getX() + 2.5, room.getY() + 2.0, room.getZ() - 1.0, new Vec3(room.getX() + 2.5, room.getY() + 3.3, room.getZ() + 2.0)));
            then(60, () -> shot(name, "room_below_surface"));
            then(1, () -> lookAt(room.getX() + 2.5, room.getY() + 2.4, room.getZ() - 3.4, new Vec3(room.getX() + 2.5, room.getY() + 2.2, room.getZ() + 1.5)));
            then(40, () -> shot(name, "room_front"));
            then(1, () -> lookAt(room.getX() + 7.2, room.getY() + 2.6, room.getZ() + 1.0, new Vec3(room.getX() + 2.5, room.getY() + 2.2, room.getZ() + 1.5)));
            then(40, () -> shot(name, "room_side"));
            then(1, () -> lookAt(room.getX() + 2.5, room.getY() + 5.3, room.getZ() - 2.0, new Vec3(room.getX() + 2.5, room.getY() + 3.5, room.getZ() + 1.5)));
            then(40, () -> shot(name, "room_above"));
            // outside: the same views, half under the roof
            then(1, () -> lookAt(outside.getX() + 3.0, outside.getY() + 1.0, outside.getZ() - 1.0, new Vec3(outside.getX() + 3.0, outside.getY() + 2.3, outside.getZ() + 2.0)));
            then(60, () -> shot(name, "outside_below_surface"));
            then(1, () -> lookAt(outside.getX() + 3.0, outside.getY() + 1.8, outside.getZ() - 4.0, new Vec3(outside.getX() + 3.0, outside.getY() + 1.2, outside.getZ() + 1.5)));
            then(40, () -> shot(name, "outside_front"));
            then(1, () -> lookAt(outside.getX() + 9.0, outside.getY() + 1.8, outside.getZ() + 1.5, new Vec3(outside.getX() + 3.0, outside.getY() + 1.2, outside.getZ() + 1.5)));
            then(40, () -> shot(name, "outside_side"));
            then(1, () -> lookAt(outside.getX() + 3.0, outside.getY() + 4.6, outside.getZ() - 2.6, new Vec3(outside.getX() + 3.0, outside.getY() + 2.0, outside.getZ() + 1.5)));
            then(40, () -> shot(name, "outside_above"));
            then(1, () -> lookAt(outside.getX() + 3.0, outside.getY() + 8.0, outside.getZ() - 0.5, new Vec3(outside.getX() + 3.0, outside.getY() + 2.0, outside.getZ() + 1.5)));
            then(40, () -> shot(name, "outside_top"));
            then(1, () -> time(18000));
            then(80, () -> lookAt(outside.getX() + 3.0, outside.getY() + 1.0, outside.getZ() - 1.0, new Vec3(outside.getX() + 3.0, outside.getY() + 2.3, outside.getZ() + 2.0)));
            then(40, () -> shot(name, "outside_night_below_surface"));
            then(1, () -> lookAt(outside.getX() + 3.0, outside.getY() + 1.8, outside.getZ() - 4.0, new Vec3(outside.getX() + 3.0, outside.getY() + 1.2, outside.getZ() + 1.5)));
            then(40, () -> shot(name, "outside_night_front"));
        }
        then(1, this::restoreIris);
        then(1, () -> time(6000));
        then(40, () -> mc.options.hideGui = false);
    }

    int problems() {
        return problems;
    }

    // ------------------------------------------------------------------ scene

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        org.overengineer.bettacraft.gametest.framework.DevTime.set(level, 6000);
        BlockPos origin = p.blockPosition().offset(-20, 0, 120);
        // the room: inside x 0..10, y 0..5, z 0..8 (relative to origin); walls, floor and ceiling of stone bricks
        for (int x = -1; x <= 11; x++) for (int y = -1; y <= 6; y++) for (int z = -1; z <= 9; z++) {
            boolean shell = x == -1 || x == 11 || y == -1 || y == 6 || z == -1 || z == 9;
            level.setBlockAndUpdate(origin.offset(x, y, z), shell ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        // the tank on a row of planks at the back wall, lit unevenly: a torch left, a lantern right, glowstone in the ceiling
        for (int x = 3; x <= 7; x++) for (int z = 5; z <= 7; z++) level.setBlockAndUpdate(origin.offset(x, 0, z), Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(1, 0, 6), Blocks.TORCH.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(9, 0, 4), Blocks.LANTERN.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(4, 6, 2), Blocks.GLOWSTONE.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(8, 6, 7), Blocks.GLOWSTONE.defaultBlockState());
        room = origin.offset(3, 1, 5);
        tank(level, p, room, 5, 3, 3, "Room tank");
        // outside: a 6 x 2 x 3 tank on planks, a stone roof over its left half (two blocks above), torches for the night
        BlockPos o2 = origin.offset(20, 0, 0);
        for (int x = -2; x <= 8; x++) for (int y = 0; y <= 6; y++) for (int z = -6; z <= 6; z++) level.setBlockAndUpdate(o2.offset(x, y, z), Blocks.AIR.defaultBlockState());
        for (int x = 0; x < 6; x++) for (int z = 0; z < 3; z++) level.setBlockAndUpdate(o2.offset(x, 0, z), Blocks.OAK_PLANKS.defaultBlockState());
        for (int x = -1; x <= 2; x++) for (int z = -1; z <= 3; z++) level.setBlockAndUpdate(o2.offset(x, 5, z), Blocks.SMOOTH_STONE.defaultBlockState());
        level.setBlockAndUpdate(o2.offset(-2, 0, 1), Blocks.TORCH.defaultBlockState());
        level.setBlockAndUpdate(o2.offset(7, 0, -1), Blocks.TORCH.defaultBlockState());
        outside = o2.offset(0, 1, 0);
        tank(level, p, outside, 6, 2, 3, "Outside tank");
        LOGGER.info("[smoketest] M63 scene: room tank at {}, outside tank at {}", room, outside);
    }

    private static void tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz, String name) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) {
            level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
            if (level.getBlockEntity(pos) instanceof TankBlockEntity be) {
                be.setGlassId(GLASS);
                be.setStyle(TankStyle.CLEAN);
            }
        }
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, sx * sy * sz * 1000);
        c.getTank().setName(name);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(p.getUUID());
        fish(level, p, c, new double[][]{{sx * 0.3, sy * 0.6, 1.2}, {sx * 0.7, sy * 0.35, 1.6}});
        c.markTankChanged();
    }

    /** Two adult bettas at fixed spots (relative to the tank's box min), frozen. */
    private static void fish(ServerLevel level, ServerPlayer p, TankBlockEntity c, double[][] spots) {
        List<Map<String, String>> genes = List.of(
                Map.of("finform", "halfmoon", "fin_length", "long", "body_color", "red", "fin_color", "red", "iridescence", "low"),
                Map.of("finform", "round", "fin_length", "short", "body_color", "yellow", "fin_color", "yellow", "pattern", "koi",
                        "pattern_color", "orange"));
        for (int i = 0; i < spots.length; i++) {
            BettaData d = BettaData.create(BettaGenome.of(genes.get(i % genes.size()), BettaGenome.DEFAULT), i > 0, level.getGameTime());
            Component err = TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d),
                    ModEntities.BETTA.get(), p, false);
            if (err != null) LOGGER.warn("[smoketest] M63 insert failed: {}", err.getString());
        }
        TankData t = c.getTank();
        int i = 0;
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) {
            if (!(e instanceof BettaEntity b) || i >= spots.length) continue;
            SmokeFreeze.freeze(b, true);
            b.snapTo(t.min().getX() + spots[i][0], t.min().getY() + spots[i][1], t.min().getZ() + spots[i][2], 90F + i * 60F, 0F);
            b.setYBodyRot(b.getYRot());
            b.setYHeadRot(b.getYRot());
            i++;
        }
    }

    private void time(long t) {
        server(p -> org.overengineer.bettacraft.gametest.framework.DevTime.set(p.level(), t));
    }

    // ------------------------------------------------------------------ shader packs (Iris, by reflection; dev only)

    private static List<String> packs() {
        String wanted = System.getProperty("bettacraft.smoketest.m63packs", DEFAULT_PACKS);
        List<String> out = new ArrayList<>();
        boolean iris = irisClass() != null;
        java.io.File dir = new java.io.File(Minecraft.getInstance().gameDirectory, "shaderpacks");
        for (String s : wanted.split(",")) {
            String pack = s.trim();
            if (pack.isEmpty()) continue;
            if (pack.equals("none")) out.add(pack);
            else if (!iris) LOGGER.info("[smoketest] M63 pack {} skipped: no Iris", pack);
            else if (!new java.io.File(dir, pack).exists()) LOGGER.info("[smoketest] M63 pack {} skipped: not in {}", pack, dir);
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
            LOGGER.warn("[smoketest] M63 could not read the Iris config", t);
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
            LOGGER.error("[smoketest] M63 FAIL: could not switch Iris to {} ({})", pack, enabled, t);
        }
    }

    private void logPack(String pack) {
        boolean inUse = org.overengineer.bettacraft.client.interior.IrisCompat.shaderPackInUse();
        String current = "-";
        try {
            if (irisClass() != null) current = String.valueOf(irisClass().getMethod("getCurrentPackName").invoke(null));
        } catch (Throwable ignored) {
        }
        boolean ok = pack.equals("none") ? !inUse : inUse;
        if (!ok) problems++;
        LOGGER.info("[smoketest] M63 pack {}: shader pack in use {}, Iris pack {} {}", pack, inUse, current, ok ? "PASS" : "FAIL");
    }

    private static String shortName(String pack) {
        if (pack.equals("none")) return "noshader";
        String s = pack.replace(".zip", "").toLowerCase(java.util.Locale.ROOT);
        if (s.startsWith("complementaryreimagined")) return "reimagined";
        if (s.startsWith("complementaryunbound")) return "unbound";
        if (s.startsWith("bsl")) return "bsl";
        if (s.startsWith("makeup")) return "makeup";
        return s.replaceAll("[^a-z0-9]+", "_");
    }

    // ------------------------------------------------------------------ helpers

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.level(), x, eyeY - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, false));
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

    private void shot(String pack, String view) {
        mc.getToastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m63_" + TAG + "_" + pack + "_" + view + ".png", mc.getMainRenderTarget(), 1, c -> {});
    }
}
