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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.big.BigCatching;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M30 part b client smoke ({@code -Psmoketestm30b}): a 24 x 6 x 12 freshwater tank (interior scale 8, sand) with the four
 * new giants (arapaima, redtail catfish, wels, two motoro stingrays), built in freshly loaded chunks 300 blocks away like
 * the part a scene (where the dark floor stripes of stale miniature light showed). Screenshots {@code bettacraft_m30b_*}:
 * the whole tank, the catfish closer, a stingray from above and from the side.
 */
final class DevClientSmokeM30b {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private BlockPos base;

    DevClientSmokeM30b() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(1, () -> look(base.getX() + 12, base.getY() + 3.5, base.getZ() - 14, 0F, 8F));
        // let the stingrays settle near the ground, then hold everyone still for the pictures
        then(200, () -> server(p -> place(p.server.overworld())));
        then(60, () -> {
            int n = mc.level.getEntitiesOfClass(TankmateEntity.class, new AABB(base).inflate(40), e -> e.species().isLarge()).size();
            LOGGER.info("[smoketest] M30b giants seen by the client: {}", n);
            if (n != 5) LOGGER.error("[smoketest] M30b expected 5 giants on the client, got {}", n);
            shot("giants_tank");
        });
        then(1, () -> look(base.getX() + 11, base.getY() + 2.4, base.getZ() - 5.5, 0F, 6F));
        then(40, () -> shot("catfish_near"));
        then(1, () -> look(base.getX() + 19.5, base.getY() + 7.6, base.getZ() + 6.5, 0F, 90F));
        then(40, () -> shot("stingray_top"));
        then(1, () -> look(base.getX() + 19.5, base.getY() + 1.6, base.getZ() - 2.6, 0F, 22F));
        then(40, () -> shot("stingray_side"));
    }

    /** Spreads the giants out in side view (heads to the left or right) and freezes them. */
    private void place(ServerLevel level) {
        double x = base.getX(), y = base.getY(), z = base.getZ();
        int rays = 0;
        for (TankmateEntity m : level.getEntitiesOfClass(TankmateEntity.class, new AABB(base).inflate(40), e -> e.species().isLarge())) {
            switch (m.species()) {
                case ARAPAIMA -> m.moveTo(x + 7.0, y + 3.4, z + 6.0, -90F, 0F);
                case REDTAIL_CATFISH -> m.moveTo(x + 15.5, y + 1.6, z + 4.5, 90F, 0F);
                case WELS_CATFISH -> m.moveTo(x + 8.5, y + 0.4, z + 8.0, -95F, 0F);
                case MOTORO_STINGRAY -> {
                    if (rays++ == 0) m.moveTo(x + 19.5, y + 0.05, z + 6.5, 25F, 0F);
                    else m.moveTo(x + 3.5, y + 0.05, z + 3.0, -60F, 0F);
                }
                default -> {}
            }
            SmokeFreeze.freeze(m, true);
        }
    }

    // ------------------------------------------------------------------ the scene

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlock(pos, ModBlocks.TANK.get().defaultBlockState(), 2);
        TankData t = TankMultiblock.formAll(level, blocks);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, t.capacityMb(), 0F);
        t.setFront(Direction.NORTH);
        t.setSignVisible(false);
        t.setOwner(p.getUUID());
        t.setWaterTemp(25F);
        t.setTargetTemp(25F);
        c.markTankChanged();
        return c;
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ServerLevel in = InteriorManager.interiorLevel(p.server);
        base = p.blockPosition().offset(-12, 2, 300);
        TankBlockEntity c = tank(level, p, base, 24, 6, 12);
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++)
            for (int z = 0; z < r.sizeZ(); z++) Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.SAND.defaultBlockState(), false);
        // two low stone mounds at the back
        for (int[] s : new int[][]{{30, 80, 6}, {150, 84, 8}})
            for (int x = -s[2]; x <= s[2]; x++)
                for (int z = -s[2]; z <= s[2]; z++) {
                    int h = (int) Math.round(s[2] * 0.9 * Math.max(0, 1 - (x * x + z * z) / (double) (s[2] * s[2])));
                    for (int y = 0; y < h; y++) in.setBlock(r.toWorld(s[0] + x, y, s[1] + z), Blocks.STONE.defaultBlockState(), 2);
                }
        for (TankmateSpecies s : new TankmateSpecies[]{TankmateSpecies.ARAPAIMA, TankmateSpecies.REDTAIL_CATFISH, TankmateSpecies.WELS_CATFISH,
                TankmateSpecies.MOTORO_STINGRAY, TankmateSpecies.MOTORO_STINGRAY}) {
            var tub = BigCatching.caught(level, s, NetCatching.random(level));
            var err = TankInhabitants.insert(level, c, tub, TankInhabitants.containedType(tub), null, 0);
            if (err != null) LOGGER.error("[smoketest] M30b {} refused: {}", s.id(), err.getString());
        }
        LOGGER.info("[smoketest] M30b tank {}x{}x{} scale {}, region {}x{}x{}, scene at {}", c.getTank().sizeX(), c.getTank().sizeY(), c.getTank().sizeZ(),
                c.getTank().scale(), r.sizeX(), r.sizeY(), r.sizeZ(), base);
        if (c.getTank().scale() != 8) LOGGER.error("[smoketest] M30b the 24 block tank should have interior scale 8");
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m30b_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
