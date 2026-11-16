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
import net.minecraft.nbt.CompoundTag;
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
 * M39 part a client smoke ({@code -Psmoketestm39a}, background window): a 16 x 4 x 8 freshwater big tank with the big fish of other
 * mods by the size rule (Aquaculture muskellunge and catfish, Fintastic featherback, a Fish of Thieves pondie); a 10 x 3 x 6 sea
 * water tank with Naturalist's ray, starfish and jellyfish, Aquaculture's jellyfish and Fish of Thieves sea fish (with
 * {@code -Paquatic} also the dumbo octopus and a Living Things seahorse); a 12 x 4 x 6 paludarium with the Aquaculture arrau and
 * starshell turtles on a sandy bank (with {@code -Paquatic} a Living Things crab). Built 200 blocks away, the animals go in from
 * their tubs, boxes and bags like a player's, then frozen. Screenshots {@code bettacraft_m39a_*}: big, sea, turtles.
 */
final class DevClientSmokeM39a {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private BlockPos big, sea, palu;

    DevClientSmokeM39a() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(200, () -> server(pl -> place(pl.serverLevel())));
        then(1, () -> look(big.getX() + 8, big.getY() + 4.2, big.getZ() - 9.0, 0F, 20F));
        then(140, () -> shot("big"));
        then(1, () -> look(sea.getX() + 5, sea.getY() + 3.2, sea.getZ() - 6.0, 0F, 20F));
        then(100, () -> shot("sea"));
        then(1, () -> look(palu.getX() + 6, palu.getY() + 4.2, palu.getZ() - 6.5, 0F, 24F));
        then(100, () -> shot("turtles"));
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
        // --- the big freshwater tank: 16 x 4 x 8, gravel, a few stones and plants
        big = base;
        TankBlockEntity bc = tank(level, p, big, 16, 4, 8);
        InteriorRegion br = floor(level, bc, Blocks.GRAVEL.defaultBlockState());
        for (int[] rock : new int[][]{{30, 100}, {34, 104}, {200, 96}, {210, 30}}) column(in, br, rock[0], rock[1], 10, Blocks.STONE.defaultBlockState());
        for (int x = 8; x < 250; x += 19) for (int z = 100; z < 124; z += 7) in.setBlock(br.toWorld(x, 1, z), Blocks.TALL_SEAGRASS.defaultBlockState(), 2);
        TankInteractions.changeWater(level, bc, bc.getTank().capacityMb());
        // --- the sea water tank: 10 x 3 x 6, sand, live rock-ish stones
        sea = base.offset(20, 0, 0);
        TankBlockEntity sc = tank(level, p, sea, 10, 3, 6);
        InteriorRegion sr = floor(level, sc, Blocks.SAND.defaultBlockState());
        for (int[] rock : new int[][]{{20, 80}, {24, 84}, {130, 76}}) column(in, sr, rock[0], rock[1], 8, Blocks.TUFF.defaultBlockState());
        TankInteractions.changeWater(level, sc, sc.getTank().capacityMb(), 35F);
        sc.getTank().setWaterTemp(24F);
        sc.getTank().setTargetTemp(24F);
        // --- the turtle paludarium: 12 x 4 x 6, fresh water a third, a sandy bank on the right half
        palu = base.offset(34, 0, 0);
        TankBlockEntity pc = tank(level, p, palu, 12, 4, 6);
        InteriorRegion pr = floor(level, pc, Blocks.SAND.defaultBlockState());
        bank(in, pr, 96, 124, 26, Blocks.SAND.defaultBlockState());
        TankInteractions.changeWater(level, pc, pc.getTank().capacityMb() / 3);
        level.setBlock(palu.offset(9, 4, 3), ModBlocks.BASKING_LAMP.get().defaultBlockState(), 3);
        LOGGER.info("[smoketest] M39a big tank at {}, sea water tank at {}, paludarium at {}", big, sea, palu);
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
            LOGGER.error("[smoketest] M39a {} refused: {}", BuiltInRegistries.ENTITY_TYPE.getKey(type), err.getString());
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
        TankBlockEntity bc = TankMultiblock.getController(level, big), sc = TankMultiblock.getController(level, sea), pc = TankMultiblock.getController(level, palu);
        if (bc == null || sc == null || pc == null) {
            LOGGER.error("[smoketest] M39a no tanks");
            return;
        }
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        for (TankBlockEntity c : List.of(bc, sc, pc)) {
            InteriorRegion r = InteriorRegions.get(level.getServer()).get(c.getTank().regionId());
            for (int cx = r.originX() >> 4; cx <= (r.originX() + r.sizeX()) >> 4; cx++)
                for (int cz = r.originZ() >> 4; cz <= (r.originZ() + r.sizeZ()) >> 4; cz++) in.getChunk(cx, cz);
            org.overengineer.bettacraft.terrarium.TankClimate.step(level, c, 0.01);
        }
        LOGGER.info("[smoketest] M39a paludarium mode {} land {}", pc.getTank().terrariumMode(), pc.getTank().landShare());
        // the big freshwater fish
        double top = bc.getTank().waterSurfaceY();
        pose(level, bc, insertIf(level, bc, "aquaculture:muskellunge", 0), 4.5, 3.0, top - 2.2, 80F);
        pose(level, bc, insertIf(level, bc, "aquaculture:catfish", 0), 11.0, 4.6, bc.getTank().min().getY() + 0.25, 250F);
        pose(level, bc, insertIf(level, bc, "fintastic:featherback", 0), 8.5, 2.2, top - 1.4, 100F);
        pose(level, bc, insertIf(level, bc, "fishofthieves:pondie", 2), 6.0, 5.4, top - 0.9, 60F);
        pose(level, bc, insertIf(level, bc, "fishofthieves:pondie", 2), 13.0, 2.6, top - 2.8, 300F);
        // the sea water tank
        double st = sc.getTank().waterSurfaceY(), sb = sc.getTank().min().getY();
        pose(level, sc, insertIf(level, sc, "naturalist:ray", 0), 5.0, 3.4, sb + 0.12, 90F);
        pose(level, sc, insertIf(level, sc, "naturalist:starfish", 2), 1.6, 1.8, sb + 0.05, 0F);
        pose(level, sc, insertIf(level, sc, "naturalist:jellyfish", 0), 7.8, 2.8, st - 1.0, 0F);
        pose(level, sc, insertIf(level, sc, "aquaculture:jellyfish", 2), 2.6, 3.0, st - 0.8, 30F);
        LivingEntity sp = insertIf(level, sc, "fishofthieves:splashtail", 2);
        pose(level, sc, sp, 4.0, 1.6, st - 1.6, 120F);
        pose(level, sc, insertIf(level, sc, "fishofthieves:stormfish", 2), 6.5, 1.6, st - 0.6, 70F);
        pose(level, sc, insertIf(level, sc, "fishofthieves:wrecker", 2), 8.6, 4.4, st - 1.8, 250F);
        pose(level, sc, insertIf(level, sc, "crittersandcompanions:dumbo_octopus", 2), 3.0, 4.6, st - 1.3, 160F);
        pose(level, sc, insertIf(level, sc, "livingthings:seahorse", 2), 1.4, 4.2, st - 1.4, 90F);
        // the turtles
        pose(level, pc, insertIf(level, pc, "aquaculture:arrau_turtle", 0), 3.5, 3.0, Double.NaN, 120F);
        pose(level, pc, insertIf(level, pc, "aquaculture:starshell_turtle", 1), 9.0, 2.0, Double.NaN, 220F);
        pose(level, pc, insertIf(level, pc, "aquaculture:starshell_turtle", 1), 10.4, 4.0, Double.NaN, 200F);
        pose(level, pc, insertIf(level, pc, "livingthings:crab", 1), 8.2, 4.3, Double.NaN, 180F);
        for (TankBlockEntity c : List.of(bc, sc, pc)) c.markTankChanged();
    }

    /** The animal of this id into the tank by tub (0), box (1) or bag (2), or null if its mod is missing (logged) or it was refused. */
    private static LivingEntity insertIf(ServerLevel level, TankBlockEntity c, String id, int kind) {
        EntityType<?> t = type(id);
        if (t == null) {
            LOGGER.info("[smoketest] M39a {} not installed", id);
            return null;
        }
        LivingEntity e = insert(level, c, t, kind, false);
        if (e != null) LOGGER.info("[smoketest] M39a {} in the tank", id);
        return e;
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m39a_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
