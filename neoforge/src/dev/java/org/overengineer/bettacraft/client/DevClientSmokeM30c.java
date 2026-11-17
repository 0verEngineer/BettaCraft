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
import org.overengineer.bettacraft.critters.TankTerrain;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tankmates.Clutch;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M30 part c client smoke ({@code -Psmoketestm30c}, with {@code -Pshaders} also the shader picture): a 28 x 6 x 14 sea water
 * tank (interior scale 8, sand, two stone mounds and a rock cave) with the five sea giants (bamboo shark, blacktip reef
 * shark, spotted eagle ray, two cownose rays, honeycomb moray) and two bamboo shark egg cases on the sand, built in freshly
 * loaded chunks 300 blocks away. Screenshots {@code bettacraft_m30c_*}: the whole tank, the sharks from the side, the egg
 * case, the rays close up and the moray in its cave.
 */
final class DevClientSmokeM30c {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private BlockPos base;
    private TankTerrain terrain;

    DevClientSmokeM30c() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(1, () -> look(base.getX() + 14, base.getY() + 3.6, base.getZ() - 17, 0F, 8F));
        then(200, () -> server(p -> place(p.server.overworld())));
        then(80, () -> {
            int n = mc.level.getEntitiesOfClass(TankmateEntity.class, new AABB(base).inflate(40), e -> e.species().isLarge()).size();
            LOGGER.info("[smoketest] M30c sea giants seen by the client: {}", n);
            if (n != 6) LOGGER.error("[smoketest] M30c expected 6 sea giants on the client, got {}", n);
            shot("sea_tank");
        });
        then(1, () -> look(base.getX() + 10.5, base.getY() + 2.3, base.getZ() - 5.0, 0F, 6F));
        then(40, () -> shot("sharks_side"));
        then(1, () -> look(base.getX() + 7.0, base.getY() + 1.3, base.getZ() - 0.5, 0F, 42F));
        then(40, () -> shot("egg_case"));
        then(1, () -> look(base.getX() + 8.0, base.getY() + 5.0, base.getZ() - 1.6, 0F, 32F));
        then(40, () -> shot("rays_side"));
        // half a wing beat later from higher up (the far wing shows in every phase)
        then(28, () -> look(base.getX() + 8.0, base.getY() + 6.4, base.getZ() - 0.6, 0F, 55F));
        then(12, () -> shot("rays_above"));
        then(1, () -> look(base.getX() + 17.3, base.getY() + 0.95, base.getZ() - 0.4, 0F, 10F));
        then(40, () -> shot("moray_cave"));
    }

    private double ground(double x, double z) {
        double g = terrain == null ? Double.NaN : terrain.ground(x, z);
        return Double.isFinite(g) ? g : base.getY();
    }

    /** Places the giants in side view (heads to the left or right) and freezes them. */
    private void place(ServerLevel level) {
        double x = base.getX(), y = base.getY(), z = base.getZ();
        int rays = 0;
        for (TankmateEntity m : level.getEntitiesOfClass(TankmateEntity.class, new AABB(base).inflate(40), e -> e.species().isLarge())) {
            switch (m.species()) {
                case BAMBOO_SHARK -> m.snapTo(x + 8.6, ground(x + 8.6, z + 1.9) + 0.02, z + 1.9, -90F, 0F);
                case BLACKTIP_REEF_SHARK -> m.snapTo(x + 11.5, y + 3.2, z + 4.5, 90F, 0F);
                case SPOTTED_EAGLE_RAY -> m.snapTo(x + 5.6, y + 3.4, z + 3.0, -65F, 0F);
                case COWNOSE_RAY -> {
                    if (rays++ == 0) m.snapTo(x + 8.8, y + 2.5, z + 2.6, -75F, 0F);
                    else m.snapTo(x + 10.8, y + 3.0, z + 3.8, -75F, 0F);
                }
                // the head out of the west mouth of the cave, the body inside
                case HONEYCOMB_MORAY -> m.snapTo(x + 18.9, ground(x + 15.0, z + 3.4) + 0.03, z + 3.4, 105F, 0F);
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
        TankInteractions.changeWater(level, c, t.capacityMb(), Salinity.SEA);
        t.setFront(Direction.NORTH);
        t.setSignVisible(false);
        t.setOwner(p.getUUID());
        t.setWaterTemp(26F);
        t.setTargetTemp(26F);
        c.markTankChanged();
        return c;
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        ServerLevel in = InteriorManager.interiorLevel(p.server);
        base = p.blockPosition().offset(-14, 2, 300);
        TankBlockEntity c = tank(level, p, base, 28, 6, 14);
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++)
            for (int z = 0; z < r.sizeZ(); z++) Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.SAND.defaultBlockState(), false);
        // two low stone mounds at the back
        for (int[] s : new int[][]{{40, 90, 7}, {120, 96, 6}})
            for (int x = -s[2]; x <= s[2]; x++)
                for (int z = -s[2]; z <= s[2]; z++) {
                    int h = (int) Math.round(s[2] * 0.9 * Math.max(0, 1 - (x * x + z * z) / (double) (s[2] * s[2])));
                    for (int y = 0; y < h; y++) in.setBlock(r.toWorld(s[0] + x, y, s[1] + z), Blocks.STONE.defaultBlockState(), 2);
                }
        // the moray's cave: a hollow, lumpy rock (world x +18.75..+23.75, z +1.5..+5.25) open to the west
        for (int x = 150; x < 190; x++)
            for (int z = 12; z < 42; z++)
                for (int y = 0; y < 16 - (int) Math.round(6 * (Math.pow((x - 170) / 21.0, 2) + Math.pow((z - 27) / 16.0, 2))) + Math.floorMod(x * 7 + z * 13, 2); y++) {
                    boolean inside = x >= 150 && x < 186 && z >= 16 && z < 38 && y < 11;
                    if (!inside) in.setBlock(r.toWorld(x, y, z), (x + y + z) % 5 == 0 ? Blocks.ANDESITE.defaultBlockState() : Blocks.STONE.defaultBlockState(), 2);
                }
        TankData t = c.getTank();
        terrain = new TankTerrain(t, p.server, r, true);
        for (TankmateSpecies s : new TankmateSpecies[]{TankmateSpecies.BAMBOO_SHARK, TankmateSpecies.BLACKTIP_REEF_SHARK, TankmateSpecies.SPOTTED_EAGLE_RAY,
                TankmateSpecies.COWNOSE_RAY, TankmateSpecies.COWNOSE_RAY, TankmateSpecies.HONEYCOMB_MORAY}) {
            var tub = BigCatching.caught(level, s, NetCatching.random(level));
            var err = TankInhabitants.insert(level, c, tub, TankInhabitants.containedType(tub), null, 0);
            if (err != null) LOGGER.error("[smoketest] M30c {} refused: {}", s.id(), err.getString());
        }
        // two egg cases on the sand in front of the bamboo shark
        TankmateGenes g = TankmateGenes.wild(TankmateSpecies.BAMBOO_SHARK);
        for (double[] e : new double[][]{{6.1, 1.5}, {6.6, 1.25}}) {
            double ex = base.getX() + e[0], ez = base.getZ() + e[1];
            Clutch k = new Clutch(t.nextClutchId(), TankmateSpecies.BAMBOO_SHARK, Clutch.Kind.EGG_CASE, ex, ground(ex, ez), ez, 1, g, g);
            t.clutches().add(k);
        }
        c.markTankChanged();
        LOGGER.info("[smoketest] M30c tank {}x{}x{} scale {}, region {}x{}x{}, scene at {}", t.sizeX(), t.sizeY(), t.sizeZ(), t.scale(), r.sizeX(), r.sizeY(),
                r.sizeZ(), base);
        if (t.scale() != 8) LOGGER.error("[smoketest] M30c the 28 block tank should have interior scale 8");
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m30c_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
