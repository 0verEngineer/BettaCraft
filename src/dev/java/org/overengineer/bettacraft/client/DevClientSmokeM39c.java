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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.big.TransportTubItem;
import org.overengineer.bettacraft.critters.TankTerrain;
import org.overengineer.bettacraft.foreign.MinecraftPets;
import org.overengineer.bettacraft.foreign.ForeignProfiles;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.terrarium.TerrariumBoxItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M39 part c client smoke ({@code -Psmoketestm39c}; background window): the peaceful animals of Minecraft as terrarium pets at
 * their real size. A closed 12 x 3 x 6 terrarium on grass with flowers and a beehive (a mini hive in the interior): chickens and a
 * chick, rabbits, bees, a cat, a fox, a parrot, an armadillo and a bat; a 8 x 3 x 6 paludarium with frogs; a 30 x 8 x 16 big
 * terrarium on grass with a cow, a sheep, a horse, a pig and a panda (from their tubs). Built 200 blocks away, the animals go in
 * from their boxes and tubs like a player's, then frozen. Screenshots {@code bettacraft_m39c_*}: pets, frogs, big.
 */
final class DevClientSmokeM39c {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private BlockPos pets, frogs, big;

    DevClientSmokeM39c() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(200, () -> server(pl -> place(pl.serverLevel())));
        // long waits: the miniatures of the interiors come to the client chunk by chunk
        then(1, () -> look(pets.getX() + 6, pets.getY() + 3.0, pets.getZ() - 4.6, 0F, 26F));
        then(300, () -> shot("pets"));
        then(1, () -> look(frogs.getX() + 4, frogs.getY() + 3.0, frogs.getZ() - 4.8, 0F, 24F));
        then(200, () -> shot("frogs"));
        then(1, () -> look(big.getX() + 15, big.getY() + 9.5, big.getZ() - 14.0, 0F, 24F));
        then(500, () -> shot("big"));
        then(1, () -> mc.options.hideGui = false);
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
        t.setWaterTemp(22F);
        t.setTargetTemp(22F);
        c.markTankChanged();
        return c;
    }

    private static InteriorRegion floor(ServerLevel level, TankBlockEntity c, BlockState ground) {
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), ground, false);
        }
        return r;
    }

    private static void column(ServerLevel in, InteriorRegion r, int x, int z, int height, BlockState top) {
        for (int y = 0; y < height; y++) in.setBlock(r.toWorld(x, y, z), y == height - 1 ? top : Blocks.DIRT.defaultBlockState(), 2);
    }

    /** Land rising from {@code from} to {@code to} (interior x) up to {@code top} blocks, flat beyond. */
    private static void bank(ServerLevel in, InteriorRegion r, int from, int to, int top, BlockState surface) {
        for (int x = from; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            int h = x >= to ? top : 1 + (int) Math.round((top - 1) * (x - from) / (double) (to - from));
            if (h > 1) column(in, r, x, z, h, surface);
        }
    }

    /** Flowers and tufts of grass scattered over the interior columns x in [from, to). */
    private static void meadow(ServerLevel in, InteriorRegion r, int from, int to, int y) {
        BlockState[] flowers = {Blocks.DANDELION.defaultBlockState(), Blocks.POPPY.defaultBlockState(), Blocks.CORNFLOWER.defaultBlockState(),
                Blocks.OXEYE_DAISY.defaultBlockState(), Blocks.ALLIUM.defaultBlockState()};
        for (int x = from; x < to; x++) for (int z = 0; z < r.sizeZ(); z++) {
            int k = (x * 7 + z * 13) % 23;
            if (k == 0) in.setBlock(r.toWorld(x, y, z), flowers[(x + z) % flowers.length], 2);
            else if (k < 4) in.setBlock(r.toWorld(x, y, z), Blocks.SHORT_GRASS.defaultBlockState(), 2);
        }
    }

    private static void lid(ServerLevel level, TankBlockEntity c) {
        for (BlockPos top : c.getTank().openTops()) level.setBlock(top, ModBlocks.TANK_LID.get().defaultBlockState(), 3);
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ServerLevel in = InteriorManager.interiorLevel(p.server);
        BlockPos base = p.blockPosition().offset(-12, 2, 200);
        // --- the pet terrarium: 12 x 3 x 6, grass with flowers, a beehive on a stump, a closed top
        pets = base;
        TankBlockEntity pc = tank(level, p, pets, 12, 3, 6);
        InteriorRegion pr = floor(level, pc, Blocks.GRASS_BLOCK.defaultBlockState());
        meadow(in, pr, 0, pr.sizeX(), 0);
        for (int y = 0; y < 3; y++) for (int dx = 0; dx < 2; dx++) for (int dz = 0; dz < 2; dz++) {
            in.setBlock(pr.toWorld(160 + dx, y, 20 + dz), y < 2 ? Blocks.OAK_LOG.defaultBlockState() : Blocks.BEEHIVE.defaultBlockState(), 2);
        }
        lid(level, pc);
        // --- the frog paludarium: 8 x 3 x 6, water a third, a grassy bank on the right half
        frogs = base.offset(16, 0, 0);
        TankBlockEntity fc = tank(level, p, frogs, 8, 3, 6);
        InteriorRegion fr = floor(level, fc, Blocks.SAND.defaultBlockState());
        bank(in, fr, 56, 80, 20, Blocks.GRASS_BLOCK.defaultBlockState());
        TankInteractions.changeWater(level, fc, fc.getTank().capacityMb() / 3);
        // --- the big terrarium: 24 x 6 x 12, grass, a few bushes
        big = base.offset(28, 0, 0);
        TankBlockEntity bc = tank(level, p, big, 30, 8, 16);
        InteriorRegion br = floor(level, bc, Blocks.GRASS_BLOCK.defaultBlockState());
        meadow(in, br, 0, br.sizeX(), 0);
        for (int[] b : new int[][]{{40, 200}, {380, 40}, {420, 220}}) for (int dx = 0; dx < 6; dx++) for (int dz = 0; dz < 6; dz++) {
            column(in, br, b[0] + dx, b[1] + dz, 4 + (dx + dz) % 3, Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
        }
        LOGGER.info("[smoketest] M39c pets at {}, frogs at {}, big at {}", pets, frogs, big);
    }

    /** Puts a new animal of the type in by its container (as a player would); the animal, or null with an error logged. */
    private static LivingEntity insert(ServerLevel level, TankBlockEntity c, EntityType<?> type, boolean baby) {
        Entity e = type.create(level);
        if (e == null) return null;
        if (baby && e instanceof Mob m) m.setBaby(true);
        var p = ForeignProfiles.of(type);
        boolean tub = p != null && p.land() != null && !p.land().box();
        ItemStack stack = tub ? TransportTubItem.fillWith(e) : TerrariumBoxItem.fillWith(e);
        e.discard();
        List<Entity> before = TankInhabitants.liveInhabitants(level, c.getTank());
        var err = TankInhabitants.insert(level, c, stack, type, null, TankInhabitants.carriedWater(stack));
        if (err != null) {
            LOGGER.error("[smoketest] M39c {} refused: {}", BuiltInRegistries.ENTITY_TYPE.getKey(type), err.getString());
            return null;
        }
        for (Entity x : TankInhabitants.liveInhabitants(level, c.getTank())) {
            if (!before.contains(x) && x instanceof LivingEntity le) {
                LOGGER.info("[smoketest] M39c {} in the tank", BuiltInRegistries.ENTITY_TYPE.getKey(type));
                return le;
            }
        }
        return null;
    }

    /** Puts the animal at (x, z) of the tank on the ground (or {@code up} blocks above it), facing {@code yaw}, frozen. */
    private static void pose(ServerLevel level, TankBlockEntity c, LivingEntity e, double dx, double dz, double up, float yaw) {
        if (e == null) return;
        TankData t = c.getTank();
        TankTerrain ter = new TankTerrain(t, level.getServer(), InteriorRegions.get(level.getServer()).get(t.regionId()), true);
        double x = t.min().getX() + dx, z = t.min().getZ() + dz;
        double g = ter.ground(x, z) + 0.01 + up;
        e.moveTo(x, g, z, yaw, 0F);
        e.setYHeadRot(yaw);
        e.yBodyRot = yaw;
        if (e instanceof Mob m) SmokeFreeze.freeze(m, true);
    }

    private void place(ServerLevel level) {
        TankBlockEntity pc = TankMultiblock.getController(level, pets), fc = TankMultiblock.getController(level, frogs),
                bc = TankMultiblock.getController(level, big);
        if (pc == null || fc == null || bc == null) {
            LOGGER.error("[smoketest] M39c no tanks");
            return;
        }
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        for (TankBlockEntity c : List.of(pc, fc, bc)) {
            InteriorRegion r = InteriorRegions.get(level.getServer()).get(c.getTank().regionId());
            for (int cx = r.originX() >> 4; cx <= (r.originX() + r.sizeX()) >> 4; cx++)
                for (int cz = r.originZ() >> 4; cz <= (r.originZ() + r.sizeZ()) >> 4; cz++) in.getChunk(cx, cz);
            org.overengineer.bettacraft.terrarium.TankClimate.step(level, c, 0.01);
            c.getTank().setClimate(new org.overengineer.bettacraft.terrarium.Climate.State(22, 22, 28, 60, 2, 0));
        }
        LOGGER.info("[smoketest] M39c modes {} {} {}", pc.getTank().terrariumMode(), fc.getTank().terrariumMode(), bc.getTank().terrariumMode());
        pose(level, pc, insert(level, pc, EntityType.CHICKEN, false), 1.6, 2.0, 0, 150F);
        pose(level, pc, insert(level, pc, EntityType.CHICKEN, false), 2.6, 3.8, 0, 210F);
        pose(level, pc, insert(level, pc, EntityType.CHICKEN, true), 2.2, 1.4, 0, 170F);
        pose(level, pc, insert(level, pc, EntityType.RABBIT, false), 4.2, 1.6, 0, 120F);
        pose(level, pc, insert(level, pc, EntityType.RABBIT, false), 4.8, 3.0, 0, 200F);
        pose(level, pc, insert(level, pc, EntityType.BEE, false), 9.4, 1.6, 0.9, 200F);
        pose(level, pc, insert(level, pc, EntityType.BEE, false), 10.6, 2.6, 1.3, 150F);
        pose(level, pc, insert(level, pc, EntityType.CAT, false), 6.2, 1.5, 0, 160F);
        pose(level, pc, insert(level, pc, EntityType.FOX, false), 7.6, 3.6, 0, 220F);
        pose(level, pc, insert(level, pc, EntityType.PARROT, false), 3.4, 4.6, 1.4, 140F);
        pose(level, pc, insert(level, pc, EntityType.ARMADILLO, false), 8.6, 1.4, 0, 190F);
        pose(level, pc, insert(level, pc, EntityType.BAT, false), 11.0, 4.6, 1.6, 180F);
        pose(level, fc, insert(level, fc, EntityType.FROG, false), 5.6, 1.6, 0, 200F);
        pose(level, fc, insert(level, fc, EntityType.FROG, false), 6.6, 3.4, 0, 160F);
        LivingEntity swim = insert(level, fc, EntityType.FROG, false);
        if (swim != null) {
            pose(level, fc, swim, 1.8, 2.4, 0, 120F);
            swim.moveTo(swim.getX(), fc.getTank().waterSurfaceY() - 0.25, swim.getZ(), 120F, 0F);
        }
        pose(level, bc, insert(level, bc, EntityType.COW, false), 7.0, 5.0, 0, 150F);
        pose(level, bc, insert(level, bc, EntityType.SHEEP, false), 11.5, 3.0, 0, 200F);
        pose(level, bc, insert(level, bc, EntityType.HORSE, false), 17.0, 8.0, 0, 120F);
        pose(level, bc, insert(level, bc, EntityType.PIG, false), 21.5, 3.0, 0, 230F);
        pose(level, bc, insert(level, bc, EntityType.PANDA, false), 24.5, 10.0, 0, 200F);
        for (TankBlockEntity c : List.of(pc, fc, bc)) {
            MinecraftPets.step(level, c, 0.01);
            c.markTankChanged();
        }
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m39c_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
