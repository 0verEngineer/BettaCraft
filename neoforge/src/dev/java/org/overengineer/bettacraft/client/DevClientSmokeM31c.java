package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.critters.TankTerrain;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.terrarium.HerpEntity;
import org.overengineer.bettacraft.terrarium.HerpSpecies;
import org.overengineer.bettacraft.terrarium.LandWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M31 part c client smoke ({@code -Psmoketestm31c}): four 4 x 3 x 3 tanks 200 blocks away, each with its own camera:
 * <ul>
 *     <li>a paludarium for the dart frogs: water on the left, mossy land rising to the right with ferns and grass, one frog of
 *     each species on the moss, a frog clutch and a tadpole in the water;</li>
 *     <li>a gecko terrarium: sand, a rock and a log; a crested gecko clinging to the back glass, one on the log, two leopard
 *     geckos (normal and tangerine) on the sand;</li>
 *     <li>a corn snake terrarium: sand and soil, a stone hide; a corn snake and an amelanistic one;</li>
 *     <li>a moist terrarium of soil and moss: a green horned frog sitting, a brown one buried up to the eyes.</li>
 * </ul>
 * Screenshots {@code bettacraft_m31c_*}: dart_frogs, geckos, corn_snake, horned_frog.
 */
final class DevClientSmokeM31c {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private BlockPos frogs, geckos, snakes, horned;

    DevClientSmokeM31c() {
        then(1, () -> server(this::build));
        then(1, () -> org.overengineer.bettacraft.client.DevHud.setHidden(mc, true));
        then(160, () -> server(pl -> place(pl.level())));
        then(1, () -> look(frogs.getX() + 2.4, frogs.getY() + 2.2, frogs.getZ() - 0.35, 0F, 42F));
        then(120, () -> {
            int n = mc.level.getEntitiesOfClass(HerpEntity.class, new AABB(frogs).inflate(40)).size();
            LOGGER.info("[smoketest] M31c animals seen by the client: {}", n);
            if (n < 11) LOGGER.error("[smoketest] M31c expected 11 animals on the client, got {}", n);
            for (HerpEntity e : mc.level.getEntitiesOfClass(HerpEntity.class, new AABB(frogs).inflate(40))) {
                LOGGER.info("[smoketest] M31c client {} variety {} pose {} tadpole {}", e.species().id(), e.variety(), e.herpPose(), e.isTadpole());
            }
            shot("dart_frogs");
        });
        then(1, () -> look(geckos.getX() + 2.0, geckos.getY() + 2.2, geckos.getZ() - 2.6, 0F, 18F));
        then(80, () -> shot("geckos"));
        then(1, () -> look(snakes.getX() + 2.1, snakes.getY() + 2.0, snakes.getZ() - 0.7, 0F, 36F));
        then(80, () -> shot("corn_snake"));
        then(1, () -> look(horned.getX() + 2.1, horned.getY() + 1.4, horned.getZ() - 0.5, 0F, 30F));
        then(80, () -> shot("horned_frog"));
        then(1, () -> org.overengineer.bettacraft.client.DevHud.setHidden(mc, false));
    }

    // ------------------------------------------------------------------ the scene

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlock(pos, ModBlocks.TANK.get().defaultBlockState(), 2);
        TankData t = TankMultiblock.formAll(level, blocks);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        t.setFront(Direction.NORTH);
        t.setSignVisible(false);
        t.setOwner(p.getUUID());
        t.setWaterTemp(24F);
        t.setTargetTemp(25F);
        c.markTankChanged();
        return c;
    }

    private InteriorRegion floor(ServerLevel level, TankBlockEntity c, BlockState sub) {
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), sub, false);
        }
        return r;
    }

    private static void column(ServerLevel in, InteriorRegion r, int x, int z, int height, BlockState top) {
        for (int y = 0; y < height; y++) in.setBlock(r.toWorld(x, y, z), y == height - 1 ? top : Blocks.DIRT.defaultBlockState(), 2);
    }

    private static void box(ServerLevel in, InteriorRegion r, int x0, int x1, int y0, int y1, int z0, int z1, BlockState b) {
        for (int x = x0; x < x1; x++) for (int y = y0; y < y1; y++) for (int z = z0; z < z1; z++) in.setBlock(r.toWorld(x, y, z), b, 2);
    }

    private static void put(ServerLevel level, TankBlockEntity c, HerpSpecies s, boolean female, int variety, boolean young) {
        HerpEntity t = ModEntities.herp(s).create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        t.setFemale(female);
        t.setVariety(variety);
        if (young) t.makeHatchling();
        ItemStack stack = t.getBucketItemStack();
        t.saveToBucketTag(stack);
        t.discard();
        var err = TankInhabitants.insert(level, c, stack, ModEntities.herp(s), null, 0L);
        if (err != null) LOGGER.error("[smoketest] M31c {} refused: {}", s.id(), err.getString());
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        ServerLevel in = InteriorManager.interiorLevel(p.level().getServer());
        // --- dart frogs: a paludarium, water on the left, mossy land rising to the right
        frogs = p.blockPosition().offset(-12, 2, 200);
        TankBlockEntity fc = tank(level, p, frogs, 4, 3, 3);
        InteriorRegion fr = floor(level, fc, Blocks.SAND.defaultBlockState());
        for (int x = 0; x < fr.sizeX(); x++) for (int z = 0; z < fr.sizeZ(); z++) {
            int h = x < 22 ? 1 : Math.min(22, 1 + (x - 22) * 2);
            if (h > 1) column(in, fr, x, z, h, h >= 14 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState());
            int k = Math.floorMod(x * 13 + z * 7, 23);
            if (h >= 18 && k < 2) in.setBlock(fr.toWorld(x, h, z), k == 0 ? Blocks.FERN.defaultBlockState() : Blocks.SHORT_GRASS.defaultBlockState(), 2);
        }
        box(in, fr, 8, 20, 0, 4, 34, 38, Blocks.OAK_LOG.defaultBlockState());
        TankInteractions.changeWater(level, fc, 12000);
        fc.markTankChanged();
        // --- geckos: sand, a rock, a log lying from the back corner up
        geckos = frogs.offset(7, 0, 0);
        TankBlockEntity gc = tank(level, p, geckos, 4, 3, 3);
        InteriorRegion gr = floor(level, gc, Blocks.SAND.defaultBlockState());
        box(in, gr, 40, 54, 0, 10, 28, 42, Blocks.STONE.defaultBlockState());
        box(in, gr, 42, 52, 10, 12, 30, 40, Blocks.SANDSTONE.defaultBlockState());
        box(in, gr, 6, 18, 0, 14, 30, 36, Blocks.OAK_LOG.defaultBlockState());
        level.setBlock(geckos.offset(2, 3, 1), ModBlocks.BASKING_LAMP.get().defaultBlockState(), 3);
        gc.markTankChanged();
        // --- corn snakes: sand and a soil mound, a stone hide at the back
        snakes = geckos.offset(7, 0, 0);
        TankBlockEntity sc = tank(level, p, snakes, 4, 3, 3);
        InteriorRegion sr = floor(level, sc, Blocks.SAND.defaultBlockState());
        box(in, sr, 46, 60, 0, 6, 36, 46, Blocks.COBBLESTONE.defaultBlockState());
        box(in, sr, 50, 56, 0, 4, 34, 36, Blocks.AIR.defaultBlockState());
        level.setBlock(snakes.offset(1, 3, 1), ModBlocks.BASKING_LAMP.get().defaultBlockState(), 3);
        sc.markTankChanged();
        // --- horned frogs: moist soil and moss
        horned = snakes.offset(7, 0, 0);
        TankBlockEntity hc = tank(level, p, horned, 4, 3, 3);
        InteriorRegion hr = floor(level, hc, Blocks.PODZOL.defaultBlockState());
        for (int x = 0; x < hr.sizeX(); x++) for (int z = 0; z < hr.sizeZ(); z++) {
            int h = 1 + (int) Math.round(2 + 1.5 * Math.sin(x * 0.15) * Math.cos(z * 0.2));
            column(in, hr, x, z, h, (x * 5 + z * 3) % 7 == 0 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.ROOTED_DIRT.defaultBlockState());
        }
        hc.markTankChanged();
        LOGGER.info("[smoketest] M31c tanks at {}, {}, {}, {}", frogs, geckos, snakes, horned);
    }

    private TankBlockEntity controller(ServerLevel level, BlockPos at) {
        TankBlockEntity c = TankMultiblock.getController(level, at);
        if (c == null) throw new IllegalStateException("no tank at " + at);
        org.overengineer.bettacraft.terrarium.TankClimate.step(level, c, 0.01);
        LOGGER.info("[smoketest] M31c tank at {}: {} land {}", at, c.getTank().terrariumMode(), c.getTank().landShare());
        return c;
    }

    private static List<HerpEntity> animals(ServerLevel level, BlockPos at, TankData t) {
        List<HerpEntity> l = level.getEntitiesOfClass(HerpEntity.class, new AABB(at).inflate(8), e -> TankInhabitants.inside(t, e));
        l.sort(java.util.Comparator.comparingInt((HerpEntity e) -> e.species().id().hashCode() * 31 + e.variety() * 2 + (e.isBaby() ? 1 : 0)));
        return l;
    }

    /** Puts the animal at (dx, dz) of the tank on the ground (or at height {@code y} when finite), turned and frozen. */
    private static void at(HerpEntity e, BlockPos min, TankTerrain ter, double dx, double dz, double y, float yaw, LandWalker.Pose pose) {
        double x = min.getX() + dx, z = min.getZ() + dz;
        double gy = Double.isFinite(y) ? min.getY() + y : ter.ground(x, z) + 0.01;
        e.snapTo(x, gy, z, yaw, 0F);
        e.setYHeadRot(yaw);
        e.yBodyRot = yaw;
        e.setPoseForTest(pose);
        SmokeFreeze.freeze(e, true);
    }

    private TankTerrain terrain(ServerLevel level, TankData t) {
        return new TankTerrain(t, level.getServer(), InteriorRegions.get(level.getServer()).get(t.regionId()), true);
    }

    /** After the interiors are built: the animals, posed and frozen. */
    private void place(ServerLevel level) {
        // dart frogs: one of each species on the moss, a clutch, a tadpole in the water
        TankBlockEntity fc = controller(level, frogs);
        TankData ft = fc.getTank();
        for (int v = 0; v < 3; v++) put(level, fc, HerpSpecies.DART_FROG, v != 1, v, false);
        put(level, fc, HerpSpecies.DART_FROG, true, 0, true);
        TankTerrain fter = terrain(level, ft);
        List<HerpEntity> fl = animals(level, frogs, ft);
        double[][] fs = {{2.55, 1.0, 160}, {3.2, 1.55, 220}, {2.9, 0.65, 190}};
        int i = 0;
        for (HerpEntity e : fl) {
            if (e.isBaby()) {
                at(e, frogs, fter, 0.9, 1.0, ft.waterSurfaceY() - frogs.getY() - 0.25, 110F, LandWalker.Pose.NONE);
            } else if (i < fs.length) {
                at(e, frogs, fter, fs[i][0], fs[i][1], Double.NaN, (float) fs[i][2], LandWalker.Pose.NONE);
                i++;
            }
        }
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion fr = InteriorRegions.get(level.getServer()).get(ft.regionId());
        int ex = 50, ez = 20;
        int top = org.overengineer.bettacraft.tank.interior.InteriorGround.top16(level.getServer(), fr, ex, ez) / 16;
        in.setBlock(fr.toWorld(ex, top, ez), ModBlocks.FROG_EGGS.get().withEggs(4), 3);
        // geckos: a crested gecko on the back glass, one on the log, two leopard geckos on the sand
        TankBlockEntity gc = controller(level, geckos);
        TankData gt = gc.getTank();
        put(level, gc, HerpSpecies.CRESTED_GECKO, true, 0, false);
        put(level, gc, HerpSpecies.CRESTED_GECKO, false, 1, false);
        put(level, gc, HerpSpecies.LEOPARD_GECKO, true, 0, false);
        put(level, gc, HerpSpecies.LEOPARD_GECKO, false, 1, false);
        TankTerrain gter = terrain(level, gt);
        int crested = 0, leo = 0;
        for (HerpEntity e : animals(level, geckos, gt)) {
            if (e.species() == HerpSpecies.CRESTED_GECKO) {
                if (crested++ == 0) at(e, geckos, gter, 1.6, 3 - TankTerrain.MARGIN - 0.01, 1.45, Direction.SOUTH.toYRot(), LandWalker.Pose.WALL_S);
                else at(e, geckos, gter, 0.75, 2.05, Double.NaN, 250F, LandWalker.Pose.NONE);
            } else {
                if (leo++ == 0) at(e, geckos, gter, 2.4, 0.9, Double.NaN, 140F, LandWalker.Pose.NONE);
                else at(e, geckos, gter, 3.1, 1.25, Double.NaN, 200F, LandWalker.Pose.NONE);
            }
        }
        // corn snakes: one stretched in S-curves in front, an amelanistic one at the hide
        TankBlockEntity sc = controller(level, snakes);
        TankData st = sc.getTank();
        put(level, sc, HerpSpecies.CORN_SNAKE, true, 0, false);
        put(level, sc, HerpSpecies.CORN_SNAKE, false, 1, false);
        TankTerrain ster = terrain(level, st);
        int s = 0;
        for (HerpEntity e : animals(level, snakes, st)) {
            if (s++ == 0) at(e, snakes, ster, 1.9, 1.0, Double.NaN, 260F, LandWalker.Pose.NONE);
            else at(e, snakes, ster, 2.6, 2.05, Double.NaN, 120F, LandWalker.Pose.NONE);
        }
        // horned frogs: the green one sitting, the brown one buried up to the eyes
        TankBlockEntity hc = controller(level, horned);
        TankData ht = hc.getTank();
        put(level, hc, HerpSpecies.HORNED_FROG, true, 0, false);
        put(level, hc, HerpSpecies.HORNED_FROG, false, 1, false);
        TankTerrain hter = terrain(level, ht);
        int hf = 0;
        for (HerpEntity e : animals(level, horned, ht)) {
            if (hf++ == 0) at(e, horned, hter, 1.5, 1.1, Double.NaN, 200F, LandWalker.Pose.NONE);
            else at(e, horned, hter, 2.7, 1.3, Double.NaN, 160F, LandWalker.Pose.BURIED);
        }
        for (TankBlockEntity c : List.of(fc, gc, sc, hc)) c.markTankChanged();
        LOGGER.info("[smoketest] M31c placed {} frogs, {} crested, {} leopard, {} snakes, {} horned", fl.size(), crested, leo, s, hf);
    }

    // ------------------------------------------------------------------ steps

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
        if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.PauseScreen) mc.gui.setScreen(null);
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m31c_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
