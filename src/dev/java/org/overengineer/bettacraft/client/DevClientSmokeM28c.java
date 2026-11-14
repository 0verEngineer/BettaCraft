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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.decor.DecorItem;
import org.overengineer.bettacraft.decor.DecorType;
import org.overengineer.bettacraft.equipment.AirPumpBlock;
import org.overengineer.bettacraft.equipment.TankLightBlock;
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
 * M28 part c dev client smoke scene ({@code -Psmoketestm28c}): the new decorations in five tanks (wrecks, stones,
 * roots, caves and tubes, ornaments with the treasure chest bubbling from an air pump) and a small tank with leaf
 * litter (almond leaves and alder cones). Outside near and far, inside wide and close up. Screenshots
 * {@code bettacraft_m28c_*}; with {@code -Pshaders} one more of the wrecks.
 */
final class DevClientSmokeM28c {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private record Spot(DecorType type, int tank, int x, int z) {}

    private static final String[] NAMES = {"wrecks", "stones", "roots", "caves", "kitsch", "litter"};
    private final int[] widths = {4, 5, 4, 4, 4, 2};

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final BlockPos[] tanks = new BlockPos[6];
    private final InteriorRegion[] regions = new InteriorRegion[6];
    private final List<Spot> spots = new ArrayList<>();
    private int wait;

    DevClientSmokeM28c() {
        spots.add(new Spot(DecorType.ROWING_BOAT, 0, 14, 14));
        spots.add(new Spot(DecorType.SHIP_BOW, 0, 44, 16));
        spots.add(new Spot(DecorType.HOLEY_ROCK, 1, 8, 14));
        spots.add(new Spot(DecorType.SLATE_CAVE, 1, 23, 14));
        spots.add(new Spot(DecorType.SEIRYU_STONE, 1, 39, 16));
        spots.add(new Spot(DecorType.DRAGON_STONE, 1, 55, 14));
        spots.add(new Spot(DecorType.LAVA_ROCK, 1, 70, 14));
        spots.add(new Spot(DecorType.MANGROVE_ROOT, 2, 11, 15));
        spots.add(new Spot(DecorType.SPIDERWOOD, 2, 31, 16));
        spots.add(new Spot(DecorType.BOGWOOD_BRIDGE, 2, 51, 14));
        spots.add(new Spot(DecorType.COCONUT_CAVE, 3, 9, 14));
        spots.add(new Spot(DecorType.CLAY_TUBES, 3, 29, 16));
        spots.add(new Spot(DecorType.SHRIMP_TUBES, 3, 52, 14));
        spots.add(new Spot(DecorType.RUIN_COLUMN, 4, 11, 15));
        spots.add(new Spot(DecorType.DIVER_HELMET, 4, 31, 15));
        spots.add(new Spot(DecorType.TREASURE_CHEST, 4, 51, 15));

        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        for (int t = 0; t < 6; t++) {
            int ti = t;
            then(1, () -> look(tanks[ti].getX() + widths[ti] / 2.0, tanks[ti].getY() + 1.2, tanks[ti].getZ() - 2.6, 0F, 6F));
            then(ti == 0 ? 200 : 60, () -> shot("outside_" + NAMES[ti]));
        }
        then(1, () -> look(tanks[2].getX() + 2.0, tanks[2].getY() + 5.0, tanks[2].getZ() - 16.0, 0F, 12F));
        then(60, () -> shot("outside_far"));
        // shadows with a shader pack
        then(1, () -> look(tanks[0].getX() + 2.0, tanks[0].getY() + 1.2, tanks[0].getZ() - 2.6, 0F, 6F));
        then(60, () -> {
            if (org.overengineer.bettacraft.client.interior.IrisCompat.shaderPackInUse()) shot("outside_wrecks_shaders");
        });
        // inside: every tank wide, then every decoration close up
        for (int t = 0; t < 6; t++) {
            int ti = t;
            then(1, () -> insideWide(ti));
            then(ti == 0 ? 100 : 50, () -> shot("inside_" + NAMES[ti] + "_wide"));
        }
        for (Spot s : spots) {
            then(1, () -> insideAt(s));
            then(40, () -> shot("inside_" + s.type().id()));
        }
    }

    private void insideWide(int t) {
        server(p -> {
            InteriorRegion r = regions[t];
            BlockPos at = r.toWorld(r.sizeX() / 2, 13, 0);
            p.teleportTo(InteriorManager.interiorLevel(p.server), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F, 24F);
        });
    }

    private void insideAt(Spot s) {
        server(p -> {
            InteriorRegion r = regions[s.tank()];
            // a little from the side (30 degrees), so the build shows its depth
            int size = Math.max(s.type().sizeX, s.type().sizeY);
            int back = s.type().sizeZ / 2 + size;
            BlockPos at = r.toWorld(Math.max(0, s.x() - back / 2), Math.max(2, s.type().sizeY / 2 + 2), Math.max(0, s.z() - back));
            p.teleportTo(InteriorManager.interiorLevel(p.server), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, -28F, 18F);
        });
    }

    // ------------------------------------------------------------------ the scene

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx) {
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
            level.setBlockAndUpdate(min.offset(x, 2, 1), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < sx - 1).setValue(TankLightBlock.LIT, true)
                    .setValue(TankLightBlock.COLOR, DyeColor.WHITE));
        }
        return c;
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ServerLevel in = InteriorManager.interiorLevel(p.server);
        level.setDayTime(6000);
        BlockPos base = p.blockPosition().offset(-14, 2, 460);
        int x0 = 0;
        for (int t = 0; t < 6; t++) {
            tanks[t] = base.offset(x0, 0, 0);
            x0 += widths[t] + 2;
            TankBlockEntity c = tank(level, p, tanks[t], widths[t]);
            InteriorRegion r = InteriorManager.ensureRegion(level, c);
            regions[t] = r;
            BlockState floor = t == 1 || t == 3 ? Blocks.SAND.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
            for (int x = 0; x < r.sizeX(); x++)
                for (int z = 0; z < r.sizeZ(); z++) Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), floor, false);
        }
        int placed = 0;
        for (Spot s : spots) {
            InteriorRegion r = regions[s.tank()];
            var err = DecorItem.place(in, s.type(), r.toWorld(s.x(), 0, s.z()), Rotation.NONE, null);
            if (err != null) LOGGER.error("[smoketest] M28c {} not placed: {}", s.type().id(), err.getString());
            else placed++;
        }
        // the air pump for the treasure chest, on the north side of the ornaments tank
        level.setBlockAndUpdate(tanks[4].offset(widths[4] - 1, 0, -1), ModBlocks.AIR_PUMP.get().defaultBlockState().setValue(AirPumpBlock.FACING, Direction.NORTH));
        // leaf litter: almond leaves and alder cones scattered on the gravel
        java.util.Random rnd = new java.util.Random(2813);
        InteriorRegion lr = regions[5];
        int litter = 0;
        for (int i = 0; i < 40; i++) {
            int x = 2 + rnd.nextInt(lr.sizeX() - 4), z = 2 + rnd.nextInt(lr.sizeZ() - 4);
            BlockPos pos = lr.toWorld(x, 0, z);
            if (!in.getBlockState(pos).isAir()) continue;
            Block b = i % 3 == 2 ? ModBlocks.ALDER_CONES.get() : ModBlocks.ALMOND_LEAF_LITTER.get();
            in.setBlock(pos, b.defaultBlockState(), Block.UPDATE_ALL);
            litter++;
        }
        var eq = InteriorManager.equipment(p.server, lr);
        LOGGER.info("[smoketest] M28c scene at {}: {} of {} decorations placed, {} litter (counted {} leaves, {} cones)", base, placed, spots.size(), litter,
                eq.leafLitter(), eq.alderCones());
        if (placed < spots.size()) LOGGER.error("[smoketest] M28c not every decoration placed");
        if (eq.leafLitter() + eq.alderCones() != litter) LOGGER.error("[smoketest] M28c litter not counted");
    }

    // ------------------------------------------------------------------ steps

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> {
            if (InteriorManager.isInterior(p.level())) p.teleportTo(p.server.overworld(), x, eyeY - p.getEyeHeight(), z, yaw, pitch);
            else p.teleportTo(p.serverLevel(), x, eyeY - p.getEyeHeight(), z, yaw, pitch);
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
        mc.getToasts().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m28c_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
