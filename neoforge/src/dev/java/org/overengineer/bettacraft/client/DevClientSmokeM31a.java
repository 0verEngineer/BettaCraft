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
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
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
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.terrarium.HeatMatBlock;
import org.overengineer.bettacraft.terrarium.MisterBlock;
import org.overengineer.bettacraft.terrarium.MisterBlockEntity;
import org.overengineer.bettacraft.terrarium.MuskTurtleEntity;
import org.overengineer.bettacraft.terrarium.ThermoHygrometerBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M31 part a client smoke ({@code -Psmoketestm31a}): a dry 4 x 3 x 3 terrarium (sand, grassy hills with ferns and flowers,
 * a basking lamp, a mister, heat mats under it, a thermo-hygrometer on the front) and a 4 x 3 x 3 paludarium (water a
 * third high, a ramp up to a planted land part on the right, a basking lamp over it, two musk turtles: one basking under
 * the lamp, one on the bottom), built 200 blocks away. Screenshots {@code bettacraft_m31a_*}: the terrarium, the paludarium,
 * the turtle up close and the climate tab of the paludarium.
 */
final class DevClientSmokeM31a {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private BlockPos terra, palu;

    DevClientSmokeM31a() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        // the turtles: one on the basking spot, one on the bottom of the water (once the interior is there)
        then(150, () -> server(pl -> place(pl.serverLevel())));
        then(1, () -> look(terra.getX() + 2.0, terra.getY() + 2.2, terra.getZ() - 3.4, 0F, 18F));
        then(120, () -> {
            int n = mc.level.getEntitiesOfClass(MuskTurtleEntity.class, new AABB(palu).inflate(20)).size();
            LOGGER.info("[smoketest] M31a musk turtles seen by the client: {}", n);
            if (n != 2) LOGGER.error("[smoketest] M31a expected 2 musk turtles on the client, got {}", n);
            var c = TankMultiblock.getController(mc.level, terra);
            if (c == null || !c.getTank().hasClimate()) LOGGER.error("[smoketest] M31a the client has no climate of the terrarium");
            else LOGGER.info("[smoketest] M31a client terrarium: {} air {} rh {} uv {}", c.getTank().terrariumMode(), c.getTank().airTemp(),
                    c.getTank().humidity(), c.getTank().uv());
            shot("terrarium");
        });
        then(1, () -> look(palu.getX() + 2.0, palu.getY() + 2.0, palu.getZ() - 3.4, 0F, 14F));
        then(60, () -> shot("paludarium"));
        then(1, () -> look(palu.getX() + 2.9, palu.getY() + 2.0, palu.getZ() - 0.9, 0F, 38F));
        then(40, () -> shot("turtle"));
        then(1, () -> mc.options.hideGui = false);
        then(1, () -> server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), palu);
            if (c == null) return;
            p.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        }));
        then(40, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.selectTabForTest(8);
            else LOGGER.error("[smoketest] M31a no tank menu");
        });
        then(30, () -> shot("climate_tab"));
        then(1, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.selectTabForTest(0);
        });
        then(20, () -> shot("overview_tab"));
        then(1, () -> mc.setScreen(null));
    }

    // ------------------------------------------------------------------ the scene

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz, long water) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlock(pos, ModBlocks.TANK.get().defaultBlockState(), 2);
        TankData t = TankMultiblock.formAll(level, blocks);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        if (water > 0) TankInteractions.changeWater(level, c, water);
        t.setFront(Direction.NORTH);
        t.setSignVisible(false);
        t.setOwner(p.getUUID());
        t.setWaterTemp(25F);
        t.setTargetTemp(28F);
        c.markTankChanged();
        return c;
    }

    private static void column(ServerLevel in, InteriorRegion r, int x, int z, int height, boolean grass) {
        for (int y = 0; y < height; y++) {
            BlockState s = grass && y == height - 1 ? Blocks.GRASS_BLOCK.defaultBlockState() : y < height - 3 ? Blocks.STONE.defaultBlockState() : Blocks.DIRT.defaultBlockState();
            in.setBlock(r.toWorld(x, y, z), s, 2);
        }
    }

    private static void plants(ServerLevel in, InteriorRegion r, int x0, int x1, int seed) {
        for (int x = x0; x <= x1; x++) for (int z = 0; z < r.sizeZ(); z++) {
            int top = -1;
            for (int y = r.sizeY() - 1; y >= 0; y--) if (in.getBlockState(r.toWorld(x, y, z)).is(Blocks.GRASS_BLOCK)) { top = y; break; }
            if (top < 0 || top + 1 >= r.sizeY()) continue;
            int k = Math.floorMod(x * 31 + z * 17 + seed, 23);
            BlockState plant = k == 0 ? Blocks.FERN.defaultBlockState() : k == 1 ? Blocks.SHORT_GRASS.defaultBlockState() : k == 2 ? Blocks.DANDELION.defaultBlockState()
                    : k == 3 ? Blocks.BLUE_ORCHID.defaultBlockState() : null;
            if (plant != null) in.setBlock(r.toWorld(x, top + 1, z), plant, 2);
        }
    }

    private InteriorRegion floor(ServerLevel level, TankBlockEntity c) {
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.SAND.defaultBlockState(), false);
        }
        return r;
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ServerLevel in = InteriorManager.interiorLevel(p.server);
        // --- the dry terrarium: two grassy hills, ferns and flowers, a stone in the middle
        terra = p.blockPosition().offset(-8, 2, 200);
        TankBlockEntity tc = tank(level, p, terra, 4, 3, 3, 0);
        InteriorRegion tr = floor(level, tc);
        for (int x = 0; x < tr.sizeX(); x++) for (int z = 0; z < tr.sizeZ(); z++) {
            double h1 = 14 - 0.012 * ((x - 14) * (x - 14) + (z - 30) * (z - 30));
            double h2 = 10 - 0.010 * ((x - 48) * (x - 48) + (z - 34) * (z - 34));
            int h = (int) Math.round(Math.max(2, Math.max(h1, h2)));
            column(in, tr, x, z, h, true);
        }
        for (int x = 34; x < 42; x++) for (int z = 10; z < 18; z++) for (int y = 0; y < 8; y++) in.setBlock(tr.toWorld(x, y, z), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 2);
        plants(in, tr, 0, tr.sizeX() - 1, 1);
        level.setBlock(terra.offset(1, 3, 1), ModBlocks.BASKING_LAMP.get().defaultBlockState(), 3);
        level.setBlock(terra.offset(3, 3, 2), ModBlocks.MISTER.get().defaultBlockState().setValue(MisterBlock.FACING, Direction.NORTH), 3);
        for (int x = 0; x < 3; x++) level.setBlock(terra.offset(x, 3, 2), ModBlocks.TANK_LID.get().defaultBlockState(), 3); // a lid on the back row
        for (int x = 0; x < 2; x++) for (int z = 0; z < 3; z++)
            level.setBlock(terra.offset(x, -1, z), ModBlocks.HEAT_MAT.get().defaultBlockState().setValue(HeatMatBlock.FACING, Direction.UP), 3);
        level.setBlock(terra.offset(1, 1, -1), ModBlocks.THERMO_HYGROMETER.get().defaultBlockState().setValue(ThermoHygrometerBlock.FACING, Direction.NORTH), 3);
        if (level.getBlockEntity(terra.offset(3, 3, 2)) instanceof MisterBlockEntity m) {
            m.water().fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
            m.setSetting(70);
        }

        // --- the paludarium: water a third high on the left, a ramp up to planted land on the right
        palu = terra.offset(8, 0, 0);
        TankBlockEntity pc = tank(level, p, palu, 4, 3, 3, 0);
        InteriorRegion pr = floor(level, pc);
        for (int x = 0; x < pr.sizeX(); x++) for (int z = 0; z < pr.sizeZ(); z++) {
            int h = x < 26 ? 1 : Math.min(24, 1 + (x - 26));
            if (h > 1) column(in, pr, x, z, h, h >= 20);
        }
        for (int x = 18; x < 22; x++) for (int z = 20; z < 26; z++) for (int y = 0; y < 6; y++) in.setBlock(pr.toWorld(x, y, z), Blocks.OAK_LOG.defaultBlockState(), 2);
        plants(in, pr, 40, pr.sizeX() - 1, 7);
        TankData pt = pc.getTank();
        TankInteractions.changeWater(level, pc, 9000); // three quarters of the lowest layer (12 of 48 interior blocks)
        pt.setWaterTemp(25F);
        level.setBlock(palu.offset(3, 3, 1), ModBlocks.BASKING_LAMP.get().defaultBlockState(), 3);
        level.setBlock(palu.offset(0, 1, -1), ModBlocks.THERMO_HYGROMETER.get().defaultBlockState().setValue(ThermoHygrometerBlock.FACING, Direction.NORTH), 3);
        pc.markTankChanged();
        for (int i = 0; i < 2; i++) {
            MuskTurtleEntity t = ModEntities.MUSK_TURTLE.get().create(level);
            ItemStack bucket = t.getBucketItemStack();
            t.saveToBucketTag(bucket);
            t.discard();
            var err = TankInhabitants.insert(level, pc, bucket, ModEntities.MUSK_TURTLE.get(), null, 0L);
            if (err != null) LOGGER.error("[smoketest] M31a turtle refused: {}", err.getString());
        }
        LOGGER.info("[smoketest] M31a terrarium at {}, paludarium at {}", terra, palu);
    }

    private void place(ServerLevel level) {
        TankBlockEntity pc = TankMultiblock.getController(level, palu);
        if (pc == null) {
            LOGGER.error("[smoketest] M31a no paludarium");
            return;
        }
        TankData t = pc.getTank();
        TankTerrain ter = new TankTerrain(t, level.getServer(), org.overengineer.bettacraft.tank.interior.InteriorRegions.get(level.getServer()).get(t.regionId()), true);
        int i = 0;
        for (MuskTurtleEntity m : level.getEntitiesOfClass(MuskTurtleEntity.class, new AABB(palu).inflate(8))) {
            double x = i == 0 ? palu.getX() + 3.5 : palu.getX() + 1.0, z = i == 0 ? palu.getZ() + 1.5 : palu.getZ() + 1.2;
            m.moveTo(x, ter.ground(x, z) + 0.01, z, i == 0 ? 160F : 250F, 0F);
            m.setBaskingForTest(i == 0);
            SmokeFreeze.freeze(m, true);
            i++;
        }
        LOGGER.info("[smoketest] M31a paludarium: {} land {} air {} rh {} uv {}", t.terrariumMode(), t.landShare(), t.airTemp(), t.humidity(), t.uv());
        if (t.terrariumMode() != org.overengineer.bettacraft.terrarium.TerrariumMode.PALUDARIUM) LOGGER.error("[smoketest] M31a the paludarium is a {}", t.terrariumMode());
    }

    // ------------------------------------------------------------------ steps

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.server.overworld(), x, eyeY - p.getEyeHeight(), z, yaw, pitch));
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
        mc.getToasts().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m31a_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
