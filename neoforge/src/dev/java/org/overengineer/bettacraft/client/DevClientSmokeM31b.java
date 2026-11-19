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
import net.minecraft.network.chat.Component;
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
import org.overengineer.bettacraft.terrarium.HeatMatBlock;
import org.overengineer.bettacraft.terrarium.HerpBreeding;
import org.overengineer.bettacraft.terrarium.HerpClutch;
import org.overengineer.bettacraft.terrarium.HerpEntity;
import org.overengineer.bettacraft.terrarium.HerpSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M31 part b client smoke ({@code -Psmoketestm31b}): a 5 x 3 x 3 paludarium (water on the left, a sandy beach rising to the
 * right under a basking lamp) with a red-eared and a yellow-bellied slider, a Reeves' turtle, a musk turtle, a clutch of
 * eggs in the sand and two slider hatchlings next to it; a 4 x 3 x 3 dry terrarium with two Hermann's tortoises (one under
 * the lamp, one among the greens). Built 200 blocks away. Screenshots {@code bettacraft_m31b_*}: the paludarium, the eggs
 * and hatchlings up close, the tortoises, the climate tab with the clutch.
 */
final class DevClientSmokeM31b {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private BlockPos palu, terra;
    private Vec3 nest;

    DevClientSmokeM31b() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(160, () -> server(pl -> place(pl.level())));
        then(1, () -> look(palu.getX() + 2.5, palu.getY() + 2.1, palu.getZ() - 3.0, 0F, 18F));
        then(120, () -> {
            int n = mc.level.getEntitiesOfClass(HerpEntity.class, new AABB(palu).inflate(12)).size();
            LOGGER.info("[smoketest] M31b turtles seen by the client: {}", n);
            if (n < 6) LOGGER.error("[smoketest] M31b expected 6 turtles in the paludarium on the client, got {}", n);
            for (HerpEntity t : mc.level.getEntitiesOfClass(HerpEntity.class, new AABB(palu).inflate(12))) {
                LOGGER.info("[smoketest] M31b client {} variety {} baby {} growth {}", t.species().id(), t.variety(), t.isBaby(), t.growth());
            }
            shot("paludarium");
        });
        then(1, () -> look(nest.x - 0.2, nest.y + 0.75, nest.z - 1.0, 0F, 34F));
        then(50, () -> shot("eggs"));
        then(1, () -> look(terra.getX() + 2.0, terra.getY() + 2.2, terra.getZ() - 3.2, 0F, 20F));
        then(80, () -> shot("tortoises"));
        then(1, () -> mc.options.hideGui = false);
        then(1, () -> server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.level(), palu);
            if (c == null) return;
            org.overengineer.bettacraft.platform.Platform.get().openMenu(p, new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        }));
        then(40, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.selectTabForTest(8);
            else LOGGER.error("[smoketest] M31b no tank menu");
        });
        then(30, () -> shot("climate_tab"));
        then(1, () -> mc.setScreen(null));
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
        t.setWaterTemp(26F);
        t.setTargetTemp(28F);
        c.markTankChanged();
        return c;
    }

    private InteriorRegion floor(ServerLevel level, TankBlockEntity c) {
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.SAND.defaultBlockState(), false);
        }
        return r;
    }

    private static void column(ServerLevel in, InteriorRegion r, int x, int z, int height, BlockState top) {
        for (int y = 0; y < height; y++) in.setBlock(r.toWorld(x, y, z), y == height - 1 ? top : y < height - 4 ? Blocks.STONE.defaultBlockState()
                : Blocks.DIRT.defaultBlockState(), 2);
    }

    private static void put(ServerLevel level, TankBlockEntity c, HerpSpecies s, boolean female, int variety, boolean hatchling) {
        HerpEntity t = ModEntities.herp(s).create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        t.setFemale(female);
        t.setVariety(variety);
        if (hatchling) t.makeHatchling();
        ItemStack stack = t.getBucketItemStack();
        t.saveToBucketTag(stack);
        t.discard();
        var err = TankInhabitants.insert(level, c, stack, ModEntities.herp(s), null, 0L);
        if (err != null) LOGGER.error("[smoketest] M31b {} refused: {}", s.id(), err.getString());
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        ServerLevel in = InteriorManager.interiorLevel(p.level().getServer());
        // --- the paludarium: water on the left, a sandy beach rising to the right, a log in the water
        palu = p.blockPosition().offset(-10, 2, 200);
        TankBlockEntity pc = tank(level, p, palu, 5, 3, 3);
        InteriorRegion pr = floor(level, pc);
        for (int x = 0; x < pr.sizeX(); x++) for (int z = 0; z < pr.sizeZ(); z++) {
            int h = x < 40 ? 1 : Math.min(24, 1 + (x - 40));
            if (h > 1) column(in, pr, x, z, h, h >= 16 ? Blocks.SAND.defaultBlockState() : Blocks.DIRT.defaultBlockState());
        }
        for (int x = 70; x < 80; x++) for (int z = 34; z < 48; z++) {
            if ((x * 7 + z * 3) % 9 == 0) in.setBlock(pr.toWorld(x, 24, z), Blocks.SHORT_GRASS.defaultBlockState(), 2);
        }
        for (int x = 20; x < 32; x++) for (int z = 28; z < 32; z++) for (int y = 0; y < 5; y++) in.setBlock(pr.toWorld(x, y, z), Blocks.OAK_LOG.defaultBlockState(), 2);
        TankInteractions.changeWater(level, pc, 15000); // one tank block of water (16 of 48 interior blocks)
        pc.getTank().setWaterTemp(26F);
        level.setBlock(palu.offset(3, 3, 1), ModBlocks.BASKING_LAMP.get().defaultBlockState(), 3);
        for (int x = 2; x < 5; x++) for (int z = 0; z < 3; z++)
            level.setBlock(palu.offset(x, -1, z), ModBlocks.HEAT_MAT.get().defaultBlockState().setValue(HeatMatBlock.FACING, Direction.UP), 3);
        pc.markTankChanged();

        // --- the dry terrarium: sand, a soil hill with grass, dandelions and pink petals; a lamp, heat mats
        terra = palu.offset(8, 0, 0);
        TankBlockEntity tc = tank(level, p, terra, 4, 3, 3);
        InteriorRegion tr = floor(level, tc);
        for (int x = 0; x < tr.sizeX(); x++) for (int z = 0; z < tr.sizeZ(); z++) {
            double hh = 9 - 0.01 * ((x - 46) * (x - 46) + (z - 32) * (z - 32));
            int h = (int) Math.round(hh);
            if (h >= 2) {
                column(in, tr, x, z, h, Blocks.GRASS_BLOCK.defaultBlockState());
                int k = Math.floorMod(x * 31 + z * 17, 19);
                BlockState plant = k == 0 ? Blocks.DANDELION.defaultBlockState() : k == 1 ? Blocks.SHORT_GRASS.defaultBlockState() : null;
                if (plant != null) in.setBlock(tr.toWorld(x, h, z), plant, 2);
            }
        }
        for (int x = 10; x < 18; x++) for (int z = 8; z < 14; z++) for (int y = 0; y < 5; y++) in.setBlock(tr.toWorld(x, y, z), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 2);
        level.setBlock(terra.offset(1, 3, 1), ModBlocks.BASKING_LAMP.get().defaultBlockState(), 3);
        for (int x = 0; x < 4; x++) for (int z = 0; z < 3; z++)
            level.setBlock(terra.offset(x, -1, z), ModBlocks.HEAT_MAT.get().defaultBlockState().setValue(HeatMatBlock.FACING, Direction.UP), 3);
        tc.markTankChanged();
        LOGGER.info("[smoketest] M31b paludarium at {}, terrarium at {}", palu, terra);
    }

    /** After the interiors are built: the animals, the clutch, everything in place and frozen. */
    private void place(ServerLevel level) {
        TankBlockEntity pc = TankMultiblock.getController(level, palu), tc = TankMultiblock.getController(level, terra);
        if (pc == null || tc == null) {
            LOGGER.error("[smoketest] M31b no tanks");
            return;
        }
        org.overengineer.bettacraft.terrarium.TankClimate.step(level, pc, 0.01);
        org.overengineer.bettacraft.terrarium.TankClimate.step(level, tc, 0.01);
        TankData t = pc.getTank();
        LOGGER.info("[smoketest] M31b paludarium: {} land {}, terrarium: {}", t.terrariumMode(), t.landShare(), tc.getTank().terrariumMode());
        put(level, pc, HerpSpecies.SLIDER, true, 0, false);
        put(level, pc, HerpSpecies.SLIDER, false, 1, false);
        put(level, pc, HerpSpecies.REEVES, true, 0, false);
        put(level, pc, HerpSpecies.MUSK, false, 0, false);
        put(level, pc, HerpSpecies.SLIDER, true, 0, true);
        put(level, pc, HerpSpecies.SLIDER, false, 1, true);
        put(level, tc, HerpSpecies.HERMANN, true, 0, false);
        put(level, tc, HerpSpecies.HERMANN, false, 0, false);
        // the clutch: a nest in the sand of the beach
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion pr = InteriorRegions.get(level.getServer()).get(t.regionId());
        for (int cx = pr.originX() >> 4; cx <= (pr.originX() + pr.sizeX()) >> 4; cx++)
            for (int cz = pr.originZ() >> 4; cz <= (pr.originZ() + pr.sizeZ()) >> 4; cz++) in.getChunk(cx, cz); // the interior is there
        HerpBreeding.Nest n = HerpBreeding.nestAt(level, t, new Vec3(palu.getX() + 3.3, palu.getY() + 1.6, palu.getZ() + 0.9), HerpSpecies.SLIDER);
        if (n == null) n = HerpBreeding.findNest(level, t, level.getRandom(), HerpSpecies.SLIDER);
        if (n == null) {
            LOGGER.error("[smoketest] M31b no nest in the sand");
        } else {
            in.setBlock(n.interior(), ModBlocks.TURTLE_EGGS.get().withEggs(4), 3);
            HerpClutch k = new HerpClutch(t.nextHerpClutchId(), HerpSpecies.SLIDER, 5, n.world(), n.interior(), 31L);
            k.incubate(30.5, 1.5, 1);
            t.herpClutches().add(k);
            nest = n.world();
            LOGGER.info("[smoketest] M31b clutch at {} (interior {})", nest, n.interior());
        }
        if (nest == null) nest = new Vec3(palu.getX() + 3.3, palu.getY() + 1.6, palu.getZ() + 0.9);
        TankTerrain ter = new TankTerrain(t, level.getServer(), InteriorRegions.get(level.getServer()).get(t.regionId()), true);
        double[][] spots = {{4.4, 2.2, 200, 1}, {1.4, 1.6, 120, 0}, {2.6, 0.7, 250, 0}, {0.8, 1.2, 60, 0}, {nest.x - palu.getX() + 0.25, nest.z - palu.getZ() - 0.15, 210, 0},
                {nest.x - palu.getX() - 0.3, nest.z - palu.getZ() + 0.1, 150, 0}};
        int i = 0;
        List<HerpEntity> list = level.getEntitiesOfClass(HerpEntity.class, new AABB(palu).inflate(8), e -> TankInhabitants.inside(t, e));
        list.sort(java.util.Comparator.comparingInt((HerpEntity e) -> (e.isBaby() ? 10 : 0) + (e.species() == HerpSpecies.SLIDER ? e.variety() : e.species() == HerpSpecies.REEVES ? 2 : 3)));
        for (HerpEntity e : list) {
            if (i >= spots.length) break;
            double x = palu.getX() + spots[i][0], z = palu.getZ() + spots[i][1];
            double y = ter.ground(x, z);
            // the swimmer in the open water (the yellow-bellied slider), the others on the ground
            if (i == 1) y = Math.max(y, t.waterSurfaceY() - 0.3);
            e.snapTo(x, y + 0.01, z, (float) spots[i][2], 0F);
            e.setBaskingForTest(spots[i][3] > 0);
            SmokeFreeze.freeze(e, true);
            i++;
        }
        TankData tt = tc.getTank();
        TankTerrain tter = new TankTerrain(tt, level.getServer(), InteriorRegions.get(level.getServer()).get(tt.regionId()), true);
        double[][] tspots = {{1.5, 1.4, 190, 1}, {2.9, 1.7, 230, 0}};
        int j = 0;
        for (HerpEntity e : level.getEntitiesOfClass(HerpEntity.class, new AABB(terra).inflate(8), e -> TankInhabitants.inside(tt, e))) {
            if (j >= tspots.length) break;
            double x = terra.getX() + tspots[j][0], z = terra.getZ() + tspots[j][1];
            e.snapTo(x, tter.ground(x, z) + 0.01, z, (float) tspots[j][2], 0F);
            e.setBaskingForTest(tspots[j][3] > 0);
            SmokeFreeze.freeze(e, true);
            j++;
        }
        pc.markTankChanged();
        LOGGER.info("[smoketest] M31b placed {} paludarium turtles, {} tortoises", i, j);
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

    private void server(Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m31b_" + name + ".png", mc.getMainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
