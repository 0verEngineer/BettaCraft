package org.overengineer.bettacraft.client;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.SignPosition;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorSync;
import org.overengineer.bettacraft.tank.interior.InteriorWorldOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M10 part of the dev client smoke test: free tank shapes. An L-shaped tank (two layers) with water, bettas and a name
 * sign, a T-shaped tank with raised ends (a step) in light blue glass, and a smaller L whose interior is decorated (the
 * miniature fills only the L). Screenshots {@code bettacraft_m10_*}: the shapes, the concave corner with its edge lines,
 * the miniature and the interior of the L seen from above (no floor in the notch).
 */
final class DevClientSmokeM10 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private BlockPos base;
    private volatile InteriorRegion miniRegion;

    DevClientSmokeM10() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(40, () -> look(base.getX() + 6.5, base.getY() + 5.2, base.getZ() - 6.5, 0F, 32F));
        then(80, () -> shot("shapes"));
        // the inner corner of the big L (notch towards the camera)
        then(1, () -> look(base.getX() + 4.4, base.getY() + 2.6, base.getZ() - 0.6, 30F, 28F));
        then(50, () -> shot("concave"));
        // the T with its raised ends
        then(1, () -> look(base.getX() + 10.0, base.getY() + 2.9, base.getZ() - 2.6, 0F, 22F));
        then(50, () -> shot("t_step"));
        // the miniature of the small L
        then(1, () -> look(base.getX() + 16.6, base.getY() + 3.3, base.getZ() - 1.9, 20F, 38F));
        then(140, () -> shot("miniature"));
        // inside the small L: floor only in the L, the notch is closed off
        then(1, () -> server(p -> {
            InteriorRegion r = miniRegion;
            if (r == null) return;
            ServerLevel in = InteriorManager.interiorLevel(p.level().getServer());
            BlockPos at = r.toWorld(4, r.sizeY() - 3, 4);
            p.teleportTo(in, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, java.util.Set.of(), -45F, 42F, true);
        }));
        then(100, () -> shot("interior"));
        then(1, () -> server(p -> {
            if (InteriorManager.isInterior(p.level())) {
                ServerLevel ow = p.level().getServer().overworld();
                p.teleportTo(ow, base.getX() + 6.5, base.getY() + 5.2, base.getZ() - 6.5, java.util.Set.of(), 0F, 32F, true);
            }
        }));
        then(40, () -> mc.options.hideGui = false);
    }

    private static void place(ServerLevel level, List<BlockPos> blocks, Identifier glass) {
        for (BlockPos pos : blocks) {
            level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
            if (glass != null && level.getBlockEntity(pos) instanceof TankBlockEntity be) be.setGlassId(glass);
        }
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        base = p.blockPosition().offset(0, 2, 130);
        // A: a big L, 4x2x4 without the 2x2 corner in the north-east (the notch faces the camera)
        List<BlockPos> a = new ArrayList<>();
        for (int y = 0; y < 2; y++) for (int z = 0; z < 4; z++) for (int x = 0; x < 4; x++) if (!(x >= 2 && z <= 1)) a.add(base.offset(x, y, z));
        place(level, a, null);
        TankBlockEntity ca = TankMultiblock.getController(level, a.getFirst());
        TankInteractions.changeWater(level, ca, 17000);
        ca.getTank().setName("L tank");
        ca.getTank().setFront(Direction.NORTH);
        ca.getTank().setSignPos(SignPosition.TOP_CENTER);
        ca.getTank().setOwner(p.getUUID());
        fish(level, p, ca, new double[][]{{0.9, 0.6, 0.8}, {1.3, 1.2, 3.0}, {3.0, 0.7, 3.2}, {0.6, 1.4, 2.2}});

        // B: a T in light blue glass, the ends of the bar raised by one block (a step)
        BlockPos tb = base.offset(8, 0, 0);
        List<BlockPos> t = new ArrayList<>();
        for (int x = 0; x < 5; x++) t.add(tb.offset(x, 0, 2));
        t.add(tb.offset(2, 0, 0));
        t.add(tb.offset(2, 0, 1));
        t.add(tb.offset(0, 1, 2));
        t.add(tb.offset(4, 1, 2));
        place(level, t, Identifier.withDefaultNamespace("light_blue_stained_glass"));
        TankBlockEntity cb = TankMultiblock.getController(level, tb.offset(2, 0, 0));
        TankInteractions.changeWater(level, cb, 5000);
        cb.getTank().setName("T");
        cb.getTank().setFront(Direction.NORTH);
        cb.getTank().setOwner(p.getUUID());
        Component err = TankInhabitants.insert(level, cb, new ItemStack(Items.TROPICAL_FISH_BUCKET), EntityType.TROPICAL_FISH, p, false);
        if (err != null) LOGGER.warn("[smoketest] M10 tropical fish: {}", err.getString());
        fish(level, p, cb, new double[][]{{2.5, 0.6, 0.6}, {0.6, 1.4, 2.5}});

        // C: a small L (3x2x3 without the 2x2 corner) with a decorated interior
        BlockPos mb = base.offset(15, 0, 0);
        List<BlockPos> m = new ArrayList<>();
        for (int y = 0; y < 2; y++) for (int z = 0; z < 3; z++) for (int x = 0; x < 3; x++) if (x == 0 || z == 2) m.add(mb.offset(x, y, z));
        place(level, m, null);
        TankBlockEntity cm = TankMultiblock.getController(level, m.getFirst());
        TankInteractions.changeWater(level, cm, 9000);
        cm.getTank().setSignVisible(false);
        cm.getTank().setFront(Direction.NORTH);
        cm.getTank().setOwner(p.getUUID());
        InteriorRegion r = InteriorManager.ensureRegion(level, cm);
        decorate(InteriorManager.interiorLevel(level.getServer()), r, cm.getTank());
        miniRegion = r;
        cm.markTankChanged();
        LOGGER.info("[smoketest] M10 scene: L {} ({} blocks, cuboid {}), T {} ({} blocks), mini L {} region {} {}x{}x{} full {}", ca.getTank().min(),
                ca.getTank().memberCount(), ca.getTank().isCuboid(), cb.getTank().min(), cb.getTank().memberCount(), cm.getTank().min(), r.id(),
                r.sizeX(), r.sizeY(), r.sizeZ(), r.fullBox());
    }

    /** Bettas at fixed spots (relative to the tank's box min) for the pictures. */
    private static void fish(ServerLevel level, ServerPlayer p, TankBlockEntity c, double[][] spots) {
        List<Map<String, String>> genes = List.of(
                Map.of("finform", "halfmoon", "fin_length", "long", "body_color", "royal_blue", "fin_color", "red", "pattern", "butterfly",
                        "pattern_color", "white", "iridescence", "high"),
                Map.of("finform", "round", "fin_length", "short", "body_color", "yellow", "fin_color", "yellow", "pattern", "koi",
                        "pattern_color", "orange"),
                Map.of("finform", "crowntail", "fin_length", "long", "body_color", "red", "fin_color", "red", "pattern", "none"),
                Map.of("finform", "doubletail", "fin_length", "long", "body_color", "white", "fin_color", "turquoise", "pattern", "marble",
                        "pattern_color", "black"));
        for (int i = 0; i < spots.length; i++) {
            BettaData d = BettaData.create(BettaGenome.of(genes.get(i % genes.size()), BettaGenome.DEFAULT), i % 2 == 1, level.getGameTime());
            Component err = TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d),
                    ModEntities.BETTA.get(), p, false);
            if (err != null) LOGGER.warn("[smoketest] M10 insert failed: {}", err.getString());
        }
        TankData t = c.getTank();
        int i = 0;
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) {
            if (!(e instanceof BettaEntity b) || i >= spots.length) continue;
            SmokeFreeze.freeze(b, true);
            b.snapTo(t.min().getX() + spots[i][0], t.min().getY() + spots[i][1], t.min().getZ() + spots[i][2], 60F + i * 70F, 0F);
            b.setYBodyRot(b.getYRot());
            b.setYHeadRot(b.getYRot());
            i++;
        }
    }

    /** Sand, rocks and plants in every cell of the shape (written directly into the sections). */
    private static void decorate(ServerLevel in, InteriorRegion r, TankData tank) {
        LongOpenHashSet touched = new LongOpenHashSet();
        int s = r.scale();
        java.util.Random rnd = new java.util.Random(10);
        BlockState[] rocks = {Blocks.STONE.defaultBlockState(), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Blocks.TUFF.defaultBlockState()};
        BlockState[] plants = {Blocks.SEAGRASS.defaultBlockState(), Blocks.KELP_PLANT.defaultBlockState(), Blocks.TUBE_CORAL_BLOCK.defaultBlockState(),
                Blocks.BRAIN_CORAL_BLOCK.defaultBlockState()};
        for (int cz = 0; cz < r.cellsZ(); cz++)
            for (int cx = 0; cx < r.cellsX(); cx++) {
                if (!r.hasCell(cx, 0, cz)) continue;
                BlockPos o = r.toWorld(cx * s, 0, cz * s);
                InteriorWorldOps.fill(in, o.getX(), o.getY(), o.getZ(), o.getX() + s - 1, o.getY() + 1, o.getZ() + s - 1, Blocks.SAND.defaultBlockState(), touched);
                int w = 3 + rnd.nextInt(5), h = 3 + rnd.nextInt(6);
                int x = rnd.nextInt(s - w), z = rnd.nextInt(s - w);
                InteriorWorldOps.fill(in, o.getX() + x, o.getY() + 2, o.getZ() + z, o.getX() + x + w - 1, o.getY() + 2 + h - 1, o.getZ() + z + w - 1,
                        rocks[(cx + cz) % rocks.length], touched);
                for (int k = 0; k < 6; k++) {
                    int px = rnd.nextInt(s), pz = rnd.nextInt(s), ph = 4 + rnd.nextInt(r.sizeY() / 2);
                    InteriorWorldOps.fill(in, o.getX() + px, o.getY() + 2, o.getZ() + pz, o.getX() + px, o.getY() + 2 + ph, o.getZ() + pz,
                            plants[k % plants.length], touched);
                }
            }
        InteriorWorldOps.resendChunks(in, touched);
        InteriorSync.markDirty(r.id());
        LOGGER.info("[smoketest] M10 decorated the L interior ({} chunks)", touched.size());
    }

    /** Puts the eye of the player at (x, eyeY, z). */
    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.level(), x, eyeY - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, false));
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    /** Returns true when all steps are done. */
    boolean tick() {
        if (mc.player != null && !steps.isEmpty()) {
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
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

    private int wait;

    private void server(java.util.function.Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m10_" + name + ".png", mc.getMainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
