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
import net.minecraft.resources.ResourceLocation;
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
 * M35 part b client smoke ({@code -Psmoketestm35b}): a 12 x 3 x 6 dry terrarium with the Naturalist lizard, snake and the two
 * scorpions (sand, a grass hill, a basking lamp); a 12 x 3 x 8 paludarium with fish in its water part (a betta, a tropical fish
 * and, if installed, a Fintastic guppy) and a Naturalist crab (and a Critters and Companions otter with {@code -Paquatic}) on the
 * sandy bank; a 10 x 3 x 6 paludarium with dragonflies in the air over the water. Built 200 blocks away; the animals go in from
 * their boxes, buckets and bags like a player's, then frozen. Screenshots {@code bettacraft_m35b_*}: terrarium, paludarium,
 * dragonfly.
 */
final class DevClientSmokeM35b {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private BlockPos terra, palu, fly;

    DevClientSmokeM35b() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(200, () -> server(pl -> place(pl.serverLevel())));
        then(1, () -> look(terra.getX() + 6, terra.getY() + 3.4, terra.getZ() - 5.5, 0F, 24F));
        then(140, () -> shot("terrarium"));
        then(1, () -> look(palu.getX() + 6, palu.getY() + 3.6, palu.getZ() - 7.0, 0F, 22F));
        then(100, () -> shot("paludarium"));
        then(1, () -> look(fly.getX() + 5, fly.getY() + 3.3, fly.getZ() - 6.0, 0F, 20F));
        then(100, () -> shot("dragonfly"));
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
        ServerLevel level = p.serverLevel();
        ServerLevel in = InteriorManager.interiorLevel(p.server);
        BlockPos base = p.blockPosition().offset(-12, 2, 200);
        // --- the dry terrarium: 12 x 3 x 6, sand, a grass hill on the right, a basking lamp
        terra = base;
        TankBlockEntity tc = tank(level, p, terra, 12, 3, 6);
        InteriorRegion tr = floor(level, tc, Blocks.SAND.defaultBlockState());
        for (int x = 0; x < tr.sizeX(); x++) for (int z = 0; z < tr.sizeZ(); z++) {
            int h = (int) Math.round(7 - 0.004 * ((x - 150) * (x - 150) + (z - 60) * (z - 60)));
            if (h >= 2) {
                column(in, tr, x, z, h, Blocks.GRASS_BLOCK.defaultBlockState());
                if (Math.floorMod(x * 31 + z * 17, 29) == 0) in.setBlock(tr.toWorld(x, h, z), Blocks.SHORT_GRASS.defaultBlockState(), 2);
            }
        }
        for (int[] rock : new int[][]{{40, 30}, {44, 33}, {90, 70}}) column(in, tr, rock[0], rock[1], 4, Blocks.SANDSTONE.defaultBlockState());
        level.setBlock(terra.offset(3, 3, 2), ModBlocks.BASKING_LAMP.get().defaultBlockState(), 3);
        // --- the paludarium: 12 x 3 x 8, fresh water a third, a sandy bank on the right half
        palu = base.offset(18, 0, 0);
        TankBlockEntity pc = tank(level, p, palu, 12, 3, 8);
        InteriorRegion pr = floor(level, pc, Blocks.SAND.defaultBlockState());
        bank(in, pr, 96, 124, 20, Blocks.SAND.defaultBlockState());
        for (int x = 20; x < 90; x += 13) for (int z = 10; z < 120; z += 17) in.setBlock(pr.toWorld(x, 1, z), Blocks.SEAGRASS.defaultBlockState(), 2);
        TankInteractions.changeWater(level, pc, pc.getTank().capacityMb() / 3);
        level.setBlock(palu.offset(9, 3, 4), ModBlocks.BASKING_LAMP.get().defaultBlockState(), 3);
        // --- the dragonflies: 10 x 3 x 6, water on the left, a grassy bank on the right
        fly = base.offset(34, 0, 0);
        TankBlockEntity fc = tank(level, p, fly, 10, 3, 6);
        InteriorRegion fr = floor(level, fc, Blocks.SAND.defaultBlockState());
        bank(in, fr, 90, 110, 20, Blocks.GRASS_BLOCK.defaultBlockState());
        for (int x = 115; x < 158; x += 7) for (int z = 5; z < 92; z += 9) in.setBlock(fr.toWorld(x, 20, z), Blocks.SHORT_GRASS.defaultBlockState(), 2);
        TankInteractions.changeWater(level, fc, fc.getTank().capacityMb() / 3);
        LOGGER.info("[smoketest] M35b terrarium at {}, paludarium at {}, dragonflies at {}", terra, palu, fly);
    }

    private static EntityType<?> type(String id) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(id)).orElse(null);
    }

    /** Puts a new animal of the type in by its container (as a player would); the animal, or null with an error logged. */
    private static LivingEntity insert(ServerLevel level, TankBlockEntity c, EntityType<?> type, int kind, boolean baby) {
        Entity e = type.create(level);
        if (e == null) return null;
        if (baby && e instanceof Mob m) m.setBaby(true);
        e.moveTo(c.getBlockPos().getX(), c.getBlockPos().getY(), c.getBlockPos().getZ());
        ItemStack stack = kind == 0 ? TransportTubItem.fillWith(e) : kind == 1 ? TerrariumBoxItem.fillWith(e)
                : org.overengineer.bettacraft.entity.betta.FishBagItem.fillWith(e);
        e.discard();
        List<Entity> before = TankInhabitants.liveInhabitants(level, c.getTank());
        var err = TankInhabitants.insert(level, c, stack, type, null, TankInhabitants.carriedWater(stack));
        if (err != null) {
            LOGGER.error("[smoketest] M35b {} refused: {}", BuiltInRegistries.ENTITY_TYPE.getKey(type), err.getString());
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
        e.moveTo(x, g, z, yaw, 0F);
        e.setYHeadRot(yaw);
        e.yBodyRot = yaw;
        if (e instanceof Mob m) SmokeFreeze.freeze(m, true);
    }

    private void place(ServerLevel level) {
        TankBlockEntity tc = TankMultiblock.getController(level, terra), pc = TankMultiblock.getController(level, palu), fc = TankMultiblock.getController(level, fly);
        if (tc == null || pc == null || fc == null) {
            LOGGER.error("[smoketest] M35b no tanks");
            return;
        }
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        for (TankBlockEntity c : List.of(tc, pc, fc)) {
            InteriorRegion r = InteriorRegions.get(level.getServer()).get(c.getTank().regionId());
            for (int cx = r.originX() >> 4; cx <= (r.originX() + r.sizeX()) >> 4; cx++)
                for (int cz = r.originZ() >> 4; cz <= (r.originZ() + r.sizeZ()) >> 4; cz++) in.getChunk(cx, cz); // the land is there for the ground
            org.overengineer.bettacraft.terrarium.TankClimate.step(level, c, 0.01);
        }
        LOGGER.info("[smoketest] M35b modes: terrarium {}, paludarium {} land {}, dragonflies {} land {}", tc.getTank().terrariumMode(),
                pc.getTank().terrariumMode(), pc.getTank().landShare(), fc.getTank().terrariumMode(), fc.getTank().landShare());
        EntityType<?> lizard = type("naturalist:lizard"), snake = type("naturalist:snake"), desert = type("naturalist:desert_scorpion"),
                jungle = type("naturalist:jungle_scorpion"), crab = type("naturalist:crab"), dragonfly = type("naturalist:dragonfly");
        if (lizard == null) {
            LOGGER.error("[smoketest] M35b Naturalist is missing (run without -PnoCompat)");
            return;
        }
        // the terrarium
        pose(level, tc, insert(level, tc, lizard, 1, false), 3.2, 2.4, Double.NaN, 200F);
        pose(level, tc, insert(level, tc, snake, 1, false), 6.3, 1.8, Double.NaN, 140F);
        pose(level, tc, insert(level, tc, desert, 1, false), 2.0, 4.3, Double.NaN, 170F);
        pose(level, tc, insert(level, tc, jungle, 1, false), 9.3, 3.6, Double.NaN, 220F);
        // the paludarium: fish in the water part, the crab (and the otter) on the bank
        double surface = pc.getTank().waterSurfaceY();
        LivingEntity betta = fish(level, pc, org.overengineer.bettacraft.entity.betta.BettaBucketItem.withFish(
                new ItemStack(org.overengineer.bettacraft.registry.ModItems.BETTA_BUCKET.get()),
                org.overengineer.bettacraft.entity.betta.BettaData.create(org.overengineer.bettacraft.genetics.BettaGenome.randomDomestic(
                        net.minecraft.util.RandomSource.create(35)), true, 0)), org.overengineer.bettacraft.registry.ModEntities.BETTA.get());
        pose(level, pc, betta, 2.5, 3.0, surface - 0.55, 100F);
        pose(level, pc, fish(level, pc, new ItemStack(net.minecraft.world.item.Items.TROPICAL_FISH_BUCKET), EntityType.TROPICAL_FISH), 4.6, 4.6, surface - 0.7, 60F);
        EntityType<?> guppy = type("fintastic:guppy");
        if (guppy != null) pose(level, pc, insert(level, pc, guppy, 2, false), 3.4, 5.8, surface - 0.4, 120F);
        pose(level, pc, insert(level, pc, crab, 1, false), 8.0, 2.8, Double.NaN, 200F);
        EntityType<?> otter = type("crittersandcompanions:otter");
        if (otter != null) pose(level, pc, insert(level, pc, otter, 1, false), 10.0, 5.2, Double.NaN, 230F);
        // the dragonflies in the air over the water (M39 part b: fliers go into a closed tank only; frozen, they stay without one)
        for (BlockPos top : fc.getTank().openTops()) level.setBlock(top, org.overengineer.bettacraft.registry.ModBlocks.TANK_LID.get().defaultBlockState(), 3);
        double fs = fc.getTank().waterSurfaceY();
        pose(level, fc, insert(level, fc, dragonfly, 1, false), 3.2, 2.8, fs + 0.6, 80F);
        EntityType<?> cnc = type("crittersandcompanions:dragonfly");
        if (cnc != null) pose(level, fc, insert(level, fc, cnc, 1, false), 6.4, 3.6, fs + 0.95, 250F);
        else pose(level, fc, insert(level, fc, dragonfly, 1, false), 6.6, 3.4, fs + 0.95, 250F);
        for (BlockPos top : fc.getTank().openTops()) level.setBlock(top, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        for (TankBlockEntity c : List.of(tc, pc, fc)) c.markTankChanged();
    }

    /** A fish from its bucket into the water part. */
    private static LivingEntity fish(ServerLevel level, TankBlockEntity c, ItemStack bucket, EntityType<?> type) {
        List<Entity> before = TankInhabitants.liveInhabitants(level, c.getTank());
        var err = TankInhabitants.insert(level, c, bucket, type, null, false);
        if (err != null) {
            LOGGER.error("[smoketest] M35b {} refused: {}", BuiltInRegistries.ENTITY_TYPE.getKey(type), err.getString());
            return null;
        }
        for (Entity x : TankInhabitants.liveInhabitants(level, c.getTank())) if (!before.contains(x) && x instanceof LivingEntity le) return le;
        return null;
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m35b_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
