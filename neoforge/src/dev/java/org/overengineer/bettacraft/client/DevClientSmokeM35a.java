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
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.big.TransportTubItem;
import org.overengineer.bettacraft.critters.TankTerrain;
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
 * M35 part a client smoke ({@code -Psmoketestm35a}): a 24 x 6 x 12 paludarium with two Naturalist alligators (water on the
 * left, a sandy bank under a basking lamp on the right), a 12 x 3 x 6 dry terrarium with two Naturalist tortoises (sand, a
 * grass hill with dandelions, a basking lamp) and a 12 x 3 x 8 sea water paludarium with a sea turtle and a hatchling on its
 * beach. Built 200 blocks away; the animals go in from a transport tub / terrarium box / fish bag like a player's, then
 * frozen. Screenshots {@code bettacraft_m35a_*}: alligators, tortoises, sea_turtles.
 */
final class DevClientSmokeM35a {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private BlockPos gator, tort, sea;

    DevClientSmokeM35a() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(200, () -> server(pl -> place(pl.level())));
        then(1, () -> look(gator.getX() + 12, gator.getY() + 7.5, gator.getZ() - 13.5, 0F, 22F));
        then(140, () -> {
            LOGGER.info("[smoketest] M35a client sees {} animals at the alligator tank", mc.level.getEntitiesOfClass(LivingEntity.class, new AABB(gator).inflate(30),
                    e -> e != mc.player).size());
            shot("alligators");
        });
        then(1, () -> look(tort.getX() + 6, tort.getY() + 3.6, tort.getZ() - 6.5, 0F, 22F));
        then(100, () -> shot("tortoises"));
        then(1, () -> look(sea.getX() + 6, sea.getY() + 3.8, sea.getZ() - 7.5, 0F, 22F));
        then(100, () -> shot("sea_turtles"));
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
        t.setWaterTemp(27F);
        t.setTargetTemp(28F);
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

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        ServerLevel in = InteriorManager.interiorLevel(p.level().getServer());
        BlockPos base = p.blockPosition().offset(-12, 2, 200);
        // --- the alligators: 24 x 6 x 12 (interior scale 8: 192 x 48 x 96), water a third, a sandy bank on the right
        gator = base;
        TankBlockEntity gc = tank(level, p, gator, 24, 6, 12);
        InteriorRegion gr = floor(level, gc, Blocks.SAND.defaultBlockState());
        bank(in, gr, 90, 112, 20, Blocks.SAND.defaultBlockState());
        for (int x = 150; x < 190; x += 9) for (int z = 6; z < 90; z += 11) in.setBlock(gr.toWorld(x, 20, z), Blocks.SHORT_GRASS.defaultBlockState(), 2);
        TankInteractions.changeWater(level, gc, gc.getTank().capacityMb() / 3);
        level.setBlock(gator.offset(17, 6, 5), ModBlocks.BASKING_LAMP.get().defaultBlockState(), 3);
        // --- the tortoises: 12 x 3 x 6 dry, sand, a grass hill with dandelions
        tort = base.offset(30, 0, 0);
        TankBlockEntity tc = tank(level, p, tort, 12, 3, 6);
        InteriorRegion tr = floor(level, tc, Blocks.SAND.defaultBlockState());
        for (int x = 0; x < tr.sizeX(); x++) for (int z = 0; z < tr.sizeZ(); z++) {
            int h = (int) Math.round(8 - 0.004 * ((x - 130) * (x - 130) + (z - 50) * (z - 50)));
            if (h >= 2) {
                column(in, tr, x, z, h, Blocks.GRASS_BLOCK.defaultBlockState());
                int k = Math.floorMod(x * 31 + z * 17, 23);
                BlockState plant = k == 0 ? Blocks.DANDELION.defaultBlockState() : k == 1 ? Blocks.SHORT_GRASS.defaultBlockState() : null;
                if (plant != null) in.setBlock(tr.toWorld(x, h, z), plant, 2);
            }
        }
        level.setBlock(tort.offset(4, 3, 2), ModBlocks.BASKING_LAMP.get().defaultBlockState(), 3);
        // --- the sea turtles: 12 x 3 x 8 sea water, a sand beach on the right third
        sea = base.offset(48, 0, 0);
        TankBlockEntity sc = tank(level, p, sea, 12, 3, 8);
        InteriorRegion sr = floor(level, sc, Blocks.SAND.defaultBlockState());
        bank(in, sr, 120, 150, 20, Blocks.SAND.defaultBlockState());
        TankInteractions.changeWater(level, sc, sc.getTank().capacityMb() / 3, 35F);
        level.setBlock(sea.offset(10, 3, 3), ModBlocks.BASKING_LAMP.get().defaultBlockState(), 3);
        LOGGER.info("[smoketest] M35a alligators at {}, tortoises at {}, sea turtles at {}", gator, tort, sea);
    }

    private static EntityType<?> type(String id) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id)).orElse(null);
    }

    /** Puts a new animal of the type in by its container (as a player would); the animal, or null with an error logged. */
    private static LivingEntity insert(ServerLevel level, TankBlockEntity c, EntityType<?> type, int kind, boolean baby) {
        Entity e = type.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (e == null) return null;
        if (baby && e instanceof Mob m) m.setBaby(true);
        e.snapTo(c.getBlockPos().getX(), c.getBlockPos().getY(), c.getBlockPos().getZ());
        ItemStack stack = kind == 0 ? TransportTubItem.fillWith(e) : kind == 1 ? TerrariumBoxItem.fillWith(e)
                : org.overengineer.bettacraft.entity.betta.FishBagItem.fillWith(e);
        e.discard();
        List<Entity> before = TankInhabitants.liveInhabitants(level, c.getTank());
        var err = TankInhabitants.insert(level, c, stack, type, null, TankInhabitants.carriedWater(stack));
        if (err != null) {
            LOGGER.error("[smoketest] M35a {} refused: {}", BuiltInRegistries.ENTITY_TYPE.getKey(type), err.getString());
            return null;
        }
        for (Entity x : TankInhabitants.liveInhabitants(level, c.getTank())) if (!before.contains(x) && x instanceof LivingEntity le) return le;
        return null;
    }

    /** Puts the animal at (x, z) of the tank on the ground (or at {@code y} if given), facing {@code yaw}, frozen. */
    private static void pose(ServerLevel level, TankBlockEntity c, LivingEntity e, double dx, double dz, double y, float yaw) {
        if (e == null) return;
        TankData t = c.getTank();
        TankTerrain ter = new TankTerrain(t, level.getServer(), InteriorRegions.get(level.getServer()).get(t.regionId()), true);
        double x = t.min().getX() + dx, z = t.min().getZ() + dz;
        double g = Double.isNaN(y) ? ter.ground(x, z) + 0.01 : y;
        e.snapTo(x, g, z, yaw, 0F);
        e.setYHeadRot(yaw);
        e.yBodyRot = yaw;
        if (e instanceof Mob m) SmokeFreeze.freeze(m, true);
    }

    private void place(ServerLevel level) {
        TankBlockEntity gc = TankMultiblock.getController(level, gator), tc = TankMultiblock.getController(level, tort), sc = TankMultiblock.getController(level, sea);
        if (gc == null || tc == null || sc == null) {
            LOGGER.error("[smoketest] M35a no tanks");
            return;
        }
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        for (TankBlockEntity c : List.of(gc, tc, sc)) {
            InteriorRegion r = InteriorRegions.get(level.getServer()).get(c.getTank().regionId());
            for (int cx = r.originX() >> 4; cx <= (r.originX() + r.sizeX()) >> 4; cx++)
                for (int cz = r.originZ() >> 4; cz <= (r.originZ() + r.sizeZ()) >> 4; cz++) in.getChunk(cx, cz); // the land is there for the ground
            org.overengineer.bettacraft.terrarium.TankClimate.step(level, c, 0.01);
        }
        LOGGER.info("[smoketest] M35a modes: alligators {} land {}, tortoises {}, sea turtles {} land {} salt {}", gc.getTank().terrariumMode(),
                gc.getTank().landShare(), tc.getTank().terrariumMode(), sc.getTank().terrariumMode(), sc.getTank().landShare(), sc.getTank().salinity());
        EntityType<?> alligator = type("naturalist:alligator"), tortoise = type("naturalist:tortoise");
        if (alligator == null || tortoise == null) {
            LOGGER.error("[smoketest] M35a Naturalist is missing (run without -PnoCompat)");
        } else {
            LivingEntity a1 = insert(level, gc, alligator, 0, false), a2 = insert(level, gc, alligator, 0, false);
            pose(level, gc, a1, 17.5, 6.5, Double.NaN, 210F);
            pose(level, gc, a2, 7.0, 4.5, gc.getTank().waterSurfaceY() - 0.45, 120F);
            LivingEntity t1 = insert(level, tc, tortoise, 1, false), t2 = insert(level, tc, tortoise, 1, false);
            pose(level, tc, t1, 4.3, 2.6, Double.NaN, 200F);
            pose(level, tc, t2, 8.2, 3.4, Double.NaN, 150F);
        }
        LivingEntity s1 = insert(level, sc, EntityType.TURTLE, 0, false), s2 = insert(level, sc, EntityType.TURTLE, 2, true);
        pose(level, sc, s1, 5.0, 3.5, sc.getTank().waterSurfaceY() - 0.5, 140F);
        pose(level, sc, s2, 10.4, 3.6, Double.NaN, 200F);
        for (TankBlockEntity c : List.of(gc, tc, sc)) c.markTankChanged();
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m35a_" + name + ".png", mc.getMainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
