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
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.big.TransportTubItem;
import org.overengineer.bettacraft.critters.TankTerrain;
import org.overengineer.bettacraft.foreign.ForeignHabits;
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
 * M39 part b client smoke ({@code -Psmoketestm39b}, best with {@code -Paquatic}; background window): a closed 10 x 3 x 6 terrarium
 * with the insects and crawlers of Naturalist (caterpillar, butterfly, firefly, ant) and Critters and Companions (roly-poly, snail,
 * stag beetle, stick bug, ladybug, jumping spider, weevil) and a Living Things snail; a closed 12 x 3 x 6 terrarium with the small
 * mammals and birds (hedgehog, mole, rat, songbird; ferret, red panda, shima enaga; koala, monkey, raccoon, owl); a 30 x 8 x 15 big
 * terrarium with Naturalist's komodo dragon; a 16 x 4 x 8 paludarium with a Living Things penguin and flamingo; at night the
 * insect terrarium again with the fireflies glowing. Built 200 blocks away, the animals go in from their boxes and tubs like a
 * player's, then frozen. Screenshots {@code bettacraft_m39b_*}: insects, mammals, komodo, birds_water, fireflies.
 */
final class DevClientSmokeM39b {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private BlockPos insects, mammals, komodo, palu;
    private final List<LivingEntity> fireflies = new ArrayList<>();
    private long dayTime;

    DevClientSmokeM39b() {
        then(1, () -> server(this::build));
        then(1, () -> org.overengineer.bettacraft.client.DevHud.setHidden(mc, true));
        then(200, () -> server(pl -> place(pl.level())));
        then(1, () -> look(insects.getX() + 5, insects.getY() + 3.0, insects.getZ() - 5.2, 0F, 22F));
        then(140, () -> shot("insects"));
        then(1, () -> look(mammals.getX() + 6, mammals.getY() + 3.2, mammals.getZ() - 6.0, 0F, 22F));
        then(100, () -> shot("mammals"));
        then(1, () -> look(komodo.getX() + 15, komodo.getY() + 8.5, komodo.getZ() - 14.0, 0F, 24F));
        then(120, () -> shot("komodo"));
        then(1, () -> look(palu.getX() + 8, palu.getY() + 4.4, palu.getZ() - 8.5, 0F, 22F));
        then(100, () -> shot("birds_water"));
        // night: the fireflies glow
        then(1, () -> server(p -> {
            dayTime = p.level().getOverworldClockTime();
            org.overengineer.bettacraft.gametest.framework.DevTime.set(p.level(), 18000);
        }));
        then(1, () -> look(insects.getX() + 5, insects.getY() + 3.0, insects.getZ() - 5.2, 0F, 22F));
        then(80, () -> server(p -> fireflies.forEach(f -> {
            if (f instanceof Mob m) ForeignHabits.flashNow(m);
        })));
        then(3, () -> shot("fireflies"));
        then(1, () -> server(p -> org.overengineer.bettacraft.gametest.framework.DevTime.set(p.level(), dayTime)));
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
        t.setTargetTemp(24F);
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

    /** A lid on every open top block of the tank (the fliers need one). */
    private static void lid(ServerLevel level, TankBlockEntity c) {
        for (BlockPos top : c.getTank().openTops()) level.setBlock(top, ModBlocks.TANK_LID.get().defaultBlockState(), 3);
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        ServerLevel in = InteriorManager.interiorLevel(p.level().getServer());
        BlockPos base = p.blockPosition().offset(-12, 2, 200);
        // --- the insect terrarium: 10 x 3 x 6, soil with moss, a few logs and stones
        insects = base;
        TankBlockEntity ic = tank(level, p, insects, 10, 3, 6);
        InteriorRegion ir = floor(level, ic, Blocks.DIRT.defaultBlockState());
        for (int x = 0; x < ir.sizeX(); x += 2) for (int z = 0; z < ir.sizeZ(); z += 3) {
            if ((x * 7 + z * 13) % 5 < 2) in.setBlock(ir.toWorld(x, 1, z), Blocks.MOSS_CARPET.defaultBlockState(), 2);
        }
        for (int[] rock : new int[][]{{20, 70}, {24, 74}, {130, 30}}) column(in, ir, rock[0], rock[1], 8, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
        for (int x = 60; x < 90; x++) for (int z = 60; z < 66; z++) in.setBlock(ir.toWorld(x, 1, z), Blocks.OAK_LOG.defaultBlockState(), 2);
        lid(level, ic);
        // --- the mammal and bird terrarium: 12 x 3 x 6, grass and a hill
        mammals = base.offset(14, 0, 0);
        TankBlockEntity mc0 = tank(level, p, mammals, 12, 3, 6);
        InteriorRegion mr = floor(level, mc0, Blocks.GRASS_BLOCK.defaultBlockState());
        bank(in, mr, 150, 190, 10, Blocks.GRASS_BLOCK.defaultBlockState());
        lid(level, mc0);
        // --- the big terrarium of the komodo dragon: 30 x 8 x 15, sand
        komodo = base.offset(30, 0, 0);
        TankBlockEntity kc = tank(level, p, komodo, 30, 8, 15);
        InteriorRegion kr = floor(level, kc, Blocks.SAND.defaultBlockState());
        for (int[] rock : new int[][]{{60, 120}, {66, 126}, {400, 60}}) column(in, kr, rock[0], rock[1], 20, Blocks.SANDSTONE.defaultBlockState());
        level.setBlock(komodo.offset(20, 8, 7), ModBlocks.BASKING_LAMP.get().defaultBlockState(), 3);
        // --- the paludarium of the penguin and the flamingo: 16 x 4 x 8, water a third, a sandy bank on the right half
        palu = base.offset(64, 0, 0);
        TankBlockEntity pc = tank(level, p, palu, 16, 4, 8);
        InteriorRegion pr = floor(level, pc, Blocks.SAND.defaultBlockState());
        bank(in, pr, 128, 160, 26, Blocks.SAND.defaultBlockState());
        TankInteractions.changeWater(level, pc, pc.getTank().capacityMb() / 3);
        LOGGER.info("[smoketest] M39b insects at {}, mammals at {}, komodo at {}, paludarium at {}", insects, mammals, komodo, palu);
    }

    private static EntityType<?> type(String id) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id)).orElse(null);
    }

    /** Puts a new animal of the type in by its container (as a player would); the animal, or null with an error logged. */
    private static LivingEntity insert(ServerLevel level, TankBlockEntity c, EntityType<?> type, boolean tub) {
        Entity e = type.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (e == null) return null;
        e.snapTo(c.getBlockPos().getX(), c.getBlockPos().getY(), c.getBlockPos().getZ());
        ItemStack stack = tub ? TransportTubItem.fillWith(e) : TerrariumBoxItem.fillWith(e);
        e.discard();
        List<Entity> before = TankInhabitants.liveInhabitants(level, c.getTank());
        var err = TankInhabitants.insert(level, c, stack, type, null, TankInhabitants.carriedWater(stack));
        if (err != null) {
            LOGGER.error("[smoketest] M39b {} refused: {}", BuiltInRegistries.ENTITY_TYPE.getKey(type), err.getString());
            return null;
        }
        for (Entity x : TankInhabitants.liveInhabitants(level, c.getTank())) if (!before.contains(x) && x instanceof LivingEntity le) return le;
        return null;
    }

    /** Puts the animal at (x, z) of the tank on the ground (or {@code up} blocks above it), facing {@code yaw}, frozen. */
    private static void pose(ServerLevel level, TankBlockEntity c, LivingEntity e, double dx, double dz, double up, float yaw) {
        if (e == null) return;
        TankData t = c.getTank();
        TankTerrain ter = new TankTerrain(t, level.getServer(), InteriorRegions.get(level.getServer()).get(t.regionId()), true);
        double x = t.min().getX() + dx, z = t.min().getZ() + dz;
        double g = ter.ground(x, z) + 0.01 + up;
        e.snapTo(x, g, z, yaw, 0F);
        e.setYHeadRot(yaw);
        e.yBodyRot = yaw;
        if (e instanceof Mob m) SmokeFreeze.freeze(m, true);
    }

    private void place(ServerLevel level) {
        TankBlockEntity ic = TankMultiblock.getController(level, insects), mc0 = TankMultiblock.getController(level, mammals),
                kc = TankMultiblock.getController(level, komodo), pc = TankMultiblock.getController(level, palu);
        if (ic == null || mc0 == null || kc == null || pc == null) {
            LOGGER.error("[smoketest] M39b no tanks");
            return;
        }
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        for (TankBlockEntity c : List.of(ic, mc0, kc, pc)) {
            InteriorRegion r = InteriorRegions.get(level.getServer()).get(c.getTank().regionId());
            for (int cx = r.originX() >> 4; cx <= (r.originX() + r.sizeX()) >> 4; cx++)
                for (int cz = r.originZ() >> 4; cz <= (r.originZ() + r.sizeZ()) >> 4; cz++) in.getChunk(cx, cz);
            org.overengineer.bettacraft.terrarium.TankClimate.step(level, c, 0.01);
        }
        LOGGER.info("[smoketest] M39b modes {} {} {} {}", ic.getTank().terrariumMode(), mc0.getTank().terrariumMode(), kc.getTank().terrariumMode(),
                pc.getTank().terrariumMode());
        // the insects and crawlers
        pose(level, ic, insertIf(level, ic, "naturalist:caterpillar", false), 4.6, 4.0, 0.12, 80F);
        pose(level, ic, insertIf(level, ic, "naturalist:butterfly", false), 3.0, 3.0, 1.2, 60F);
        pose(level, ic, insertIf(level, ic, "naturalist:butterfly", false), 7.6, 2.4, 1.6, 230F);
        LivingEntity f1 = insertIf(level, ic, "naturalist:firefly", false), f2 = insertIf(level, ic, "naturalist:firefly", false),
                f3 = insertIf(level, ic, "naturalist:firefly", false);
        pose(level, ic, f1, 5.4, 2.8, 1.8, 100F);
        pose(level, ic, f2, 8.6, 4.4, 1.3, 200F);
        pose(level, ic, f3, 1.8, 4.8, 2.0, 30F);
        for (LivingEntity f : List.of(f1, f2, f3)) if (f != null) fireflies.add(f);
        pose(level, ic, insertIf(level, ic, "naturalist:ant", false), 2.2, 1.6, 0, 120F);
        pose(level, ic, insertIf(level, ic, "naturalist:ant", false), 2.8, 1.4, 0, 160F);
        pose(level, ic, insertIf(level, ic, "crittersandcompanions:roly_poly", false), 6.2, 1.5, 0, 200F);
        pose(level, ic, insertIf(level, ic, "crittersandcompanions:snail", false), 1.2, 3.0, 0, 90F);
        pose(level, ic, insertIf(level, ic, "crittersandcompanions:stag_beetle", false), 4.2, 1.8, 0, 160F);
        pose(level, ic, insertIf(level, ic, "crittersandcompanions:stick_bug", false), 9.0, 2.0, 0, 250F);
        pose(level, ic, insertIf(level, ic, "crittersandcompanions:ladybug", false), 6.8, 3.4, 0.9, 140F);
        pose(level, ic, insertIf(level, ic, "crittersandcompanions:jumping_spider", false), 8.0, 1.2, 0, 190F);
        pose(level, ic, insertIf(level, ic, "crittersandcompanions:weevil", false), 3.4, 4.6, 0, 210F);
        pose(level, ic, insertIf(level, ic, "livingthings:snail", false), 5.6, 4.9, 0, 300F);
        // the mammals and birds
        pose(level, mc0, insertIf(level, mc0, "naturalist:hedgehog", false), 2.0, 1.6, 0, 150F);
        pose(level, mc0, insertIf(level, mc0, "naturalist:mole", false), 3.4, 3.6, 0, 200F);
        pose(level, mc0, insertIf(level, mc0, "naturalist:rat", false), 4.8, 1.4, 0, 120F);
        pose(level, mc0, insertIf(level, mc0, "naturalist:bird", false), 6.0, 2.6, 1.4, 90F);
        pose(level, mc0, insertIf(level, mc0, "crittersandcompanions:ferret", false), 6.6, 1.5, 0, 220F);
        pose(level, mc0, insertIf(level, mc0, "crittersandcompanions:red_panda", false), 8.4, 3.2, 0, 230F);
        pose(level, mc0, insertIf(level, mc0, "crittersandcompanions:shima_enaga", false), 3.0, 2.8, 1.7, 60F);
        pose(level, mc0, insertIf(level, mc0, "livingthings:koala", false), 10.6, 3.8, 0, 200F);
        pose(level, mc0, insertIf(level, mc0, "livingthings:monkey", false), 9.6, 1.6, 0, 170F);
        pose(level, mc0, insertIf(level, mc0, "livingthings:raccoon", false), 1.6, 4.4, 0, 140F);
        pose(level, mc0, insertIf(level, mc0, "livingthings:owl", false), 8.2, 4.8, 1.2, 180F);
        // the komodo dragon
        pose(level, kc, insertIf(level, kc, "naturalist:komodo_dragon", true), 14.0, 6.0, 0, 120F);
        // the penguin in the water, the flamingo wading at the bank
        double surface = pc.getTank().waterSurfaceY();
        LivingEntity penguin = insertIf(level, pc, "livingthings:penguin", true);
        if (penguin != null) {
            pose(level, pc, penguin, 4.5, 3.2, 0, 90F);
            penguin.snapTo(penguin.getX(), surface - 0.5, penguin.getZ(), 90F, 0F);
        }
        pose(level, pc, insertIf(level, pc, "livingthings:penguin", true), 11.4, 5.0, 0, 200F);
        pose(level, pc, insertIf(level, pc, "livingthings:flamingo", true), 9.0, 2.6, 0, 240F);
        for (TankBlockEntity c : List.of(ic, mc0, kc, pc)) c.markTankChanged();
    }

    /** The animal of this id into the tank by tub or box, or null if its mod is missing (logged) or it was refused. */
    private static LivingEntity insertIf(ServerLevel level, TankBlockEntity c, String id, boolean tub) {
        EntityType<?> t = type(id);
        if (t == null) {
            LOGGER.info("[smoketest] M39b {} not installed", id);
            return null;
        }
        LivingEntity e = insert(level, c, t, tub);
        if (e != null) LOGGER.info("[smoketest] M39b {} in the tank", id);
        return e;
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m39b_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
