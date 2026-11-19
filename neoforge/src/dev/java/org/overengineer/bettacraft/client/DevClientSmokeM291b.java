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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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
 * M29.1 part b client smoke ({@code -Psmoketestm291b}): the Bucephalandras, Cryptocorynes and the Vallisneria with their
 * new leaves, inside and outside; the sound test command. Screenshots {@code bettacraft_m291b_*}.
 */
final class DevClientSmokeM291b {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private record Spot(PlantSpecies species, int tank, int x, int z) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final BlockPos[] tanks = new BlockPos[2];
    private final InteriorRegion[] regions = new InteriorRegion[2];
    private final List<Spot> spots = new ArrayList<>();
    private final List<int[]> tops = new ArrayList<>();
    private static final int WIDTH = 4;
    private final String tag = System.getProperty("bettacraft.smoketest.m291b.tag", "");
    private int wait;
    private BlockPos base;

    DevClientSmokeM291b() {
        PlantSpecies[] buces = {PlantSpecies.BUCE_GREEN_WAVY, PlantSpecies.BUCE_BROWNIE_RED, PlantSpecies.BUCE_MINI_NEEDLE, PlantSpecies.BUCE_KEDAGANG,
                PlantSpecies.BUCE_GODZILLA, PlantSpecies.BUCE_MINI_PINK};
        int[][] bp = {{8, 12}, {22, 12}, {36, 12}, {50, 12}, {15, 23}, {43, 23}};
        for (int i = 0; i < buces.length; i++) spots.add(new Spot(buces[i], 0, bp[i][0], bp[i][1]));
        PlantSpecies[] rooted = {PlantSpecies.CRYPT_GREEN, PlantSpecies.CRYPT_BROWN, PlantSpecies.CRYPT_FLAMINGO, PlantSpecies.CRYPT_PARVA,
                PlantSpecies.VALLISNERIA};
        int[][] rp = {{8, 12}, {22, 12}, {36, 12}, {50, 12}, {30, 24}};
        for (int i = 0; i < rooted.length; i++) spots.add(new Spot(rooted[i], 1, rp[i][0], rp[i][1]));

        then(1, () -> server(this::build));
        then(1, () -> org.overengineer.bettacraft.client.DevHud.setHidden(mc, true));
        plants();
        if (!Boolean.getBoolean("bettacraft.smoketest.m291b.plantsonly")) {
            devices();
            soundTest();
        }
    }

    // ------------------------------------------------------------------ air pump in the cabinet, multiblock filter under a tank

    private BlockPos cabTank, freeTank, filterTank;

    private void devices() {
        then(1, () -> server(this::buildDevices));
        // the hoses: the cabinet's (left outlet side, beside the filter hose) and a free-standing pump's, down to the stones
        then(1, () -> look(cabTank.getX() + 1.5, cabTank.getY() + 1.2, cabTank.getZ() - 2.3, 0F, 10F));
        then(120, () -> {
            var be = mc.level.getBlockEntity(cabTank.below());
            boolean pump = be instanceof org.overengineer.bettacraft.equipment.CabinetBlockEntity cab && cab.hasAirPump();
            boolean runs = be instanceof org.overengineer.bettacraft.equipment.CabinetBlockEntity cab2 && cab2.airRunning();
            LOGGER.info("[smoketest] M29.1 cabinet air pump on the client: present {}, running {}", pump, runs);
            if (!pump || !runs) LOGGER.error("[smoketest] M29.1 the cabinet air pump is not synced to the client");
            shot("cabinet_air_hose");
        });
        then(1, () -> look(cabTank.getX() - 0.9, cabTank.getY() + 1.4, cabTank.getZ() + 1.0, -90F, 22F));
        then(40, () -> shot("cabinet_air_hose_side"));
        then(1, () -> look(freeTank.getX() + 1.5, freeTank.getY() + 1.2, freeTank.getZ() - 2.3, 0F, 10F));
        then(60, () -> shot("free_pump_air_hose"));
        // the cabinet menu with the air pump slot
        then(1, () -> server(p -> {
            if (p.level().getBlockEntity(cabTank.below()) instanceof org.overengineer.bettacraft.equipment.CabinetBlockEntity cab) p.openMenu(cab);
        }));
        then(20, () -> {
            org.overengineer.bettacraft.client.DevHud.setHidden(mc, false);
            shot("cabinet_menu");
        });
        then(1, () -> {
            if (mc.player != null) mc.player.closeContainer();
            org.overengineer.bettacraft.client.DevHud.setHidden(mc, true);
        });
        // the multiblock filter under its tank
        then(1, () -> look(filterTank.getX() + 1.0, filterTank.getY() + 0.6, filterTank.getZ() - 3.2, 0F, 12F));
        then(60, () -> {
            var be = mc.level.getBlockEntity(filterTank.below(2).east(2));
            LOGGER.info("[smoketest] M29.1 multiblock filter under the tank: {}", be == null ? "missing" : be.getBlockState());
            shot("multiblock_filter_under_tank");
        });
        then(1, () -> look(filterTank.getX() + 3.4, filterTank.getY() + 0.9, filterTank.getZ() - 1.8, 45F, 18F));
        then(40, () -> shot("multiblock_filter_under_tank_side"));
    }

    private void buildDevices(ServerPlayer p) {
        ServerLevel level = p.level().getServer().overworld(); // the player may be in a tank interior right now
        ServerLevel in = InteriorManager.interiorLevel(p.level().getServer());
        // a tank on cabinets: the front left one has a filter (outlet side left) and an air pump; three air stones
        cabTank = base.offset(0, 0, 8);
        for (int x = 0; x < 3; x++) for (int z = 0; z < 2; z++) {
            BlockPos cp = cabTank.offset(x, -1, z);
            level.setBlockAndUpdate(cp, ModBlocks.CABINET.get().defaultBlockState().setValue(org.overengineer.bettacraft.equipment.CabinetBlock.FACING, Direction.NORTH));
        }
        TankBlockEntity a = tank(level, p, cabTank, 3);
        if (level.getBlockEntity(cabTank.below()) instanceof org.overengineer.bettacraft.equipment.CabinetBlockEntity cab) {
            cab.items().setStackInSlot(org.overengineer.bettacraft.equipment.CabinetBlockEntity.FILTER_SLOT, new net.minecraft.world.item.ItemStack(org.overengineer.bettacraft.registry.ModItems.EXTERNAL_FILTER.get()));
            cab.items().setStackInSlot(org.overengineer.bettacraft.equipment.CabinetBlockEntity.AIR_PUMP_SLOT, new net.minecraft.world.item.ItemStack(org.overengineer.bettacraft.registry.ModItems.AIR_PUMP.get()));
            cab.setOutletSide(org.overengineer.bettacraft.equipment.OutletSide.LEFT);
        }
        InteriorRegion ra = InteriorManager.ensureRegion(level, a);
        for (int x = 0; x < ra.sizeX(); x++)
            for (int z = 0; z < ra.sizeZ(); z++) Substrate.set(in, new BlockPos(ra.originX() + x, InteriorRegion.FLOOR_Y, ra.originZ() + z), Blocks.SAND.defaultBlockState(), false);
        for (int[] s : new int[][]{{10, 8}, {24, 20}, {38, 10}}) in.setBlock(ra.toWorld(s[0], 0, s[1]), ModBlocks.AIR_STONE.get().defaultBlockState(), 3);
        org.overengineer.bettacraft.tank.TankEquipment.invalidate(a);
        org.overengineer.bettacraft.tank.TankAeration.step(level, a, 0.01);
        // a free-standing air pump beside a tank with two stones
        freeTank = base.offset(6, 0, 8);
        TankBlockEntity b = tank(level, p, freeTank, 3);
        level.setBlockAndUpdate(freeTank.offset(-1, 0, 0), ModBlocks.AIR_PUMP.get().defaultBlockState()
                .setValue(org.overengineer.bettacraft.equipment.AirPumpBlock.FACING, Direction.EAST));
        InteriorRegion rb = InteriorManager.ensureRegion(level, b);
        for (int x = 0; x < rb.sizeX(); x++)
            for (int z = 0; z < rb.sizeZ(); z++) Substrate.set(in, new BlockPos(rb.originX() + x, InteriorRegion.FLOOR_Y, rb.originZ() + z), Blocks.SAND.defaultBlockState(), false);
        for (int[] s : new int[][]{{12, 12}, {30, 18}}) in.setBlock(rb.toWorld(s[0], 0, s[1]), ModBlocks.AIR_STONE.get().defaultBlockState(), 3);
        in.setBlock(rb.toWorld(40, 0, 6), ModBlocks.SPONGE_FILTER_SEGMENT.get().defaultBlockState()
                .setValue(org.overengineer.bettacraft.equipment.SegmentBlock.PART, org.overengineer.bettacraft.equipment.SegmentBlock.Part.ANCHOR), 3);
        org.overengineer.bettacraft.tank.TankEquipment.invalidate(b);
        org.overengineer.bettacraft.tank.TankAeration.step(level, b, 0.01);
        // a multiblock filter directly under a tank: chambers, connectors facing up, the pump head beside
        filterTank = base.offset(12, 1, 8);
        BlockPos fl = filterTank.below(2);
        for (int z = 0; z < 2; z++) for (int x = 0; x < 2; x++) level.setBlockAndUpdate(fl.offset(x, 0, z), ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
        for (int x = 0; x < 2; x++) level.setBlockAndUpdate(fl.offset(x, 1, 1), ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
        level.setBlockAndUpdate(fl.offset(0, 1, 0), ModBlocks.FILTER_INLET.get().defaultBlockState().setValue(org.overengineer.bettacraft.filter.FilterPartBlock.FACING, Direction.UP));
        level.setBlockAndUpdate(fl.offset(1, 1, 0), ModBlocks.FILTER_OUTLET.get().defaultBlockState().setValue(org.overengineer.bettacraft.filter.FilterPartBlock.FACING, Direction.UP));
        level.setBlockAndUpdate(fl.offset(2, 0, 0), ModBlocks.FILTER_PUMP_HEAD.get().defaultBlockState());
        if (level.getBlockEntity(fl) instanceof org.overengineer.bettacraft.filter.FilterChamberBlockEntity ch)
            ch.setMedium(new net.minecraft.world.item.ItemStack(org.overengineer.bettacraft.registry.ModItems.FILTER_FOAM.get()));
        TankBlockEntity f = tank(level, p, filterTank, 2);
        if (level.getBlockEntity(fl.offset(2, 0, 0)) instanceof org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity head) {
            org.overengineer.bettacraft.gametest.FilterLinesGameTests.guardAll(level, head.getBlockPos()); // M40: its guards
            int moved = head.run(level, true);
            LOGGER.info("[smoketest] M29.1 filter under the tank: {} moved {} mB, serves {}", head.problem(), moved, head.servedTank());
            if (moved <= 0 || !f.getBlockPos().equals(head.servedTank())) LOGGER.error("[smoketest] M29.1 the multiblock filter under the tank does not run");
        }
    }

    // ------------------------------------------------------------------ plants

    private void plants() {
        for (int t = 0; t < 2; t++) {
            int ti = t;
            then(1, () -> look(tanks[ti].getX() + WIDTH / 2.0, tanks[ti].getY() + 1.1, tanks[ti].getZ() - 2.4, 0F, 8F));
            then(ti == 0 ? 220 : 80, () -> shot("outside_" + (ti == 0 ? "buce" : "crypt") + tag));
            then(1, () -> look(tanks[ti].getX() + WIDTH / 2.0, tanks[ti].getY() + 0.75, tanks[ti].getZ() - 1.1, 0F, 12F));
            then(40, () -> shot("outside_" + (ti == 0 ? "buce" : "crypt") + "_near" + tag));
        }
        for (int i = 0; i < spots.size(); i++) {
            int si = i;
            then(1, () -> insideAt(spots.get(si)));
            then(si == 0 ? 100 : 40, () -> shot("inside_" + spots.get(si).species().id() + tag));
        }
        // the Vallisneria swaying: four pictures 1 s apart, side by side they show how far it swings
        Spot val = spots.getLast();
        then(1, () -> insideAt(val));
        for (int i = 0; i < 4; i++) {
            int n = i;
            then(i == 0 ? 40 : 20, () -> shot("vallisneria_sway_" + n + tag));
        }
    }

    // ------------------------------------------------------------------ sound test

    private void soundTest() {
        then(1, () -> {
            org.overengineer.bettacraft.client.DevHud.setHidden(mc, false);
            mc.player.connection.sendCommand("bettacraft sounds");
        });
        then(30, () -> {
            boolean running = org.overengineer.bettacraft.client.SoundTest.runningForTest();
            LOGGER.info("[smoketest] M29.1 sound test running: {}, played {}", running, org.overengineer.bettacraft.client.SoundTest.playedForTest());
            if (!running) LOGGER.error("[smoketest] M29.1 /bettacraft sounds did not start");
            shot("sound_test_chat");
        });
        then(SoundTestSteps.TOTAL, () -> {
            List<String> played = org.overengineer.bettacraft.client.SoundTest.playedForTest();
            LOGGER.info("[smoketest] M29.1 sound test played {} sounds: {}", played.size(), played);
            if (played.size() != SoundTestSteps.COUNT) LOGGER.error("[smoketest] M29.1 sound test played {} of {} sounds", played.size(), SoundTestSteps.COUNT);
            org.overengineer.bettacraft.client.DevHud.setHidden(mc, true);
        });
    }

    /** How long the sound test takes. */
    private static final class SoundTestSteps {
        static final int COUNT = 16;
        static final int TOTAL = COUNT * 70 + 20;
    }

    // ------------------------------------------------------------------ the scene

    private int top(Spot s) {
        for (int[] t : tops) if (t[0] == s.tank() && t[1] == s.x() && t[2] == s.z()) return t[3];
        return 0;
    }

    private void insideAt(Spot s) {
        server(p -> {
            InteriorRegion r = regions[s.tank()];
            int size = s.species() == PlantSpecies.VALLISNERIA ? 24 : s.species() == PlantSpecies.CRYPT_PARVA || s.species() == PlantSpecies.BUCE_MINI_NEEDLE ? 8 : 11;
            int y = top(s);
            BlockPos at = r.toWorld(s.x(), y + size / 3, s.z() - size);
            p.teleportTo(InteriorManager.interiorLevel(p.level().getServer()), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, java.util.Set.of(), 0F, s.species() == PlantSpecies.VALLISNERIA ? -5F : 14F, true);
        });
    }

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min) {
        return tank(level, p, min, WIDTH);
    }

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int width) {
        final int WIDTH = width;
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) for (int x = 0; x < WIDTH; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, (long) WIDTH * 4 * 1000, 0F);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(p.getUUID());
        c.getTank().setWaterTemp(25F);
        c.getTank().setTargetTemp(25F);
        c.markTankChanged();
        for (int x = 0; x < WIDTH; x++) {
            level.setBlockAndUpdate(min.offset(x, 2, 1), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < WIDTH - 1).setValue(TankLightBlock.LIT, true)
                    .setValue(TankLightBlock.COLOR, DyeColor.WHITE));
        }
        return c;
    }

    private static void mound(ServerLevel in, InteriorRegion r, int cx, int cz, int rad, int h, BlockState a) {
        for (int x = -rad; x <= rad; x++)
            for (int z = -rad; z <= rad; z++) {
                double d = Math.sqrt(x * x + z * z) / rad;
                if (d > 1) continue;
                int top = (int) Math.round(h * Math.max(0.2, 1 - d * d * 0.8));
                for (int y = 0; y < top; y++) in.setBlock(r.toWorld(cx + x, y, cz + z), a, 2);
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
        base = p.blockPosition().offset(-30, 2, 300);
        for (int t = 0; t < 2; t++) {
            tanks[t] = base.offset(t * (WIDTH + 2), 0, 0);
            TankBlockEntity c = tank(level, p, tanks[t]);
            InteriorRegion r = InteriorManager.ensureRegion(level, c);
            regions[t] = r;
            BlockState floor = t == 0 ? Blocks.GRAVEL.defaultBlockState() : Blocks.DIRT.defaultBlockState();
            for (int x = 0; x < r.sizeX(); x++)
                for (int z = 0; z < r.sizeZ(); z++) Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), floor, false);
        }
        long seed = 29100;
        for (Spot s : spots) {
            InteriorRegion r = regions[s.tank()];
            if (s.tank() == 0) mound(in, r, s.x(), s.z(), 3, 3, Blocks.STONE.defaultBlockState());
            int y = topAt(in, r, s.x(), s.z());
            tops.add(new int[] {s.tank(), s.x(), s.z(), y});
            InteriorPlant pl = InteriorPlants.plantAt(in, r, s.species(), s.x(), y, s.z(), s.species().stages(), seed++);
            if (pl == null) LOGGER.error("[smoketest] M29.1 {} not planted", s.species().id());
        }
        for (int t = 0; t < 2; t++) InteriorPlants.settleTint(level, TankMultiblock.getController(level, tanks[t]));
        LOGGER.info("[smoketest] M29.1 part b scene at {}: {} plants", base, spots.size());
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
        if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.PauseScreen) mc.gui.setScreen(null);
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
        mc.gui.toastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m291b_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
