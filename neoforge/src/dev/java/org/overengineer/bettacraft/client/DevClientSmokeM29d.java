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
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterLife;
import org.overengineer.bettacraft.critters.CritterMovement;
import org.overengineer.bettacraft.critters.CritterObject;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.AxolotlCare;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M29 part d client smoke ({@code -Psmoketestm29d}): a tank with the big crayfish (Florida crayfish blue, orange and wild,
 * two marbled crayfish, a young one and a shed shell) on gravel with plants, and a cool axolotl tank with the five vanilla
 * colours, a larva and a clutch of eggs on a Rotala. Screenshots {@code bettacraft_m29d_crayfish}, {@code _crayfish_close},
 * {@code _axolotl}, {@code _axolotl_close}. Checks (errors in the log): the critters and the eggs are there.
 */
final class DevClientSmokeM29d {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final BlockPos base;
    private int wait;
    /** Where the axolotl eggs are (set on the server thread, read for the close-up). */
    private volatile Vec3 eggAt;

    DevClientSmokeM29d() {
        base = mc.player.blockPosition().offset(-10, 0, 760);
        then(1, () -> server(this::build));
        then(1, () -> org.overengineer.bettacraft.client.DevHud.setHidden(mc, true));
        then(20, () -> look(base.getX() + 2.0, base.getY() + 2.2, base.getZ() - 3.0, 0F, 18F));
        then(100, () -> server(this::arrange));
        // the crayfish tank (camera looks south: +x is on the left of the picture)
        then(1, () -> look(base.getX() + 2.0, base.getY() + 1.5, base.getZ() - 1.9, 0F, 24F));
        then(80, () -> shot("crayfish"));
        then(1, () -> look(base.getX() + 1.7, base.getY() + 0.6, base.getZ() - 0.5, 0F, 26F));
        then(60, () -> shot("crayfish_close"));
        // the axolotl tank
        then(1, () -> look(base.getX() + 8.5, base.getY() + 1.6, base.getZ() - 2.6, 0F, 22F));
        then(60, () -> shot("axolotl"));
        // the close-up of the eggs: the axolotls near them swim off to the other end
        then(1, () -> server(p -> {
            Vec3 e = eggAt;
            if (e == null) return;
            for (var a : p.level().getEntitiesOfClass(Axolotl.class, new net.minecraft.world.phys.AABB(e, e).inflate(1.6, 2, 2))) {
                a.setPos(a.getX() + 2.6, a.getY(), a.getZ() + 0.2);
            }
        }));
        then(5, () -> {
            Vec3 e = eggAt != null ? eggAt : new Vec3(base.getX() + 8, base.getY() + 0.6, base.getZ() + 1.5);
            double dz = e.z - (base.getZ() - 0.3), dy = 0.3;
            look(e.x + 0.15, e.y + dy, base.getZ() - 0.3, 8F, (float) Math.toDegrees(Math.atan2(dy, dz)));
        });
        then(60, () -> shot("axolotl_close"));
        then(1, () -> org.overengineer.bettacraft.client.DevHud.setHidden(mc, false));
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        for (int x = -4; x <= 14; x++)
            for (int z = -8; z <= 8; z++) {
                level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
                for (int y = 0; y < 5; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
        TankBlockEntity cray = tank(level, p, base, 4, 2, 2, 22F);
        TankBlockEntity axo = tank(level, p, base.offset(6, 0, 0), 5, 2, 2, 18F);
        for (TankBlockEntity c : List.of(cray, axo)) {
            ServerLevel in = InteriorManager.interiorLevel(level.getServer());
            InteriorRegion r = InteriorManager.ensureRegion(level, c);
            for (int x = 0; x < r.sizeX(); x++)
                for (int z = 0; z < r.sizeZ(); z++)
                    Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.GRAVEL.defaultBlockState(), false);
            boolean crayfish = c == cray;
            // plants at the back (south, +z) of the tank
            int[][] spots = crayfish ? new int[][]{{8, 24}, {24, 26}, {40, 25}, {56, 24}} : new int[][]{{10, 26}, {30, 27}, {50, 26}, {70, 27}};
            PlantSpecies[] kinds = crayfish ? new PlantSpecies[]{PlantSpecies.ROTALA, PlantSpecies.LUDWIGIA, PlantSpecies.ROTALA, PlantSpecies.LUDWIGIA}
                    : new PlantSpecies[]{PlantSpecies.ROTALA, PlantSpecies.LUDWIGIA, PlantSpecies.ROTALA, PlantSpecies.LUDWIGIA};
            for (int i = 0; i < spots.length; i++) {
                if (InteriorPlants.plantAt(in, r, kinds[i], spots[i][0], 0, spots[i][1], 4, 11L + i) == null) LOGGER.error("[smoketest] M29d plant {} not planted", i);
            }
            c.markTankChanged();
        }
    }

    private void arrange(ServerPlayer p) {
        ServerLevel level = p.level();
        TankBlockEntity cray = TankMultiblock.getController(level, base), axo = TankMultiblock.getController(level, base.offset(6, 0, 0));
        if (cray == null || axo == null) {
            LOGGER.error("[smoketest] M29d tanks missing");
            return;
        }
        double bx = base.getX(), bz = base.getZ();
        TankData t = cray.getTank();
        t.critters().clear();
        t.critterObjects().clear();
        var terrain = TankCritters.terrain(level, t);
        put(t, terrain, CritterSpecies.FLORIDA_CRAYFISH, CritterGenes.pure(0, 0.6F), true, bx + 0.8, bz + 0.6, 30F, true);
        put(t, terrain, CritterSpecies.FLORIDA_CRAYFISH, CritterGenes.pure(1, 0.6F), false, bx + 1.9, bz + 0.5, -40F, true);
        put(t, terrain, CritterSpecies.FLORIDA_CRAYFISH, CritterGenes.WILD, false, bx + 3.0, bz + 0.6, 160F, true);
        put(t, terrain, CritterSpecies.MARBLED_CRAYFISH, CritterGenes.WILD, true, bx + 1.3, bz + 1.15, 80F, true);
        put(t, terrain, CritterSpecies.MARBLED_CRAYFISH, CritterGenes.WILD, true, bx + 2.6, bz + 1.2, -110F, false);
        Critter shed = put(t, terrain, CritterSpecies.FLORIDA_CRAYFISH, CritterGenes.pure(0, 0.6F), true, bx + 3.4, bz + 1.3, 60F, true);
        t.critterObjects().add(CritterLife.molt(shed, t.nextCritterId()));
        t.critters().remove(shed);
        if (t.critters().size() != 5) LOGGER.error("[smoketest] M29d crayfish: {}", t.critters().size());
        cray.markTankChanged();
        TankCritters.sync(level, cray);
        // the axolotls: the five colours (adults) and a larva, eggs on the middle Rotala
        Axolotl.Variant[] vs = {Axolotl.Variant.LUCY, Axolotl.Variant.WILD, Axolotl.Variant.GOLD, Axolotl.Variant.CYAN, Axolotl.Variant.BLUE};
        double ax = bx + 6;
        for (int i = 0; i < vs.length; i++) {
            int l = AxolotlCare.locus(vs[i]);
            Vec3 at = new Vec3(ax + 0.7 + i * 0.9, base.getY() + 0.2 + (i % 2) * 0.55, bz + 0.75 + (i % 2) * 0.25);
            Axolotl a = AxolotlCare.spawn(level, axo, l < 0 ? CritterGenes.WILD : CritterGenes.pure(l, 0.1F), 0, at);
            if (a == null) {
                LOGGER.error("[smoketest] M29d axolotl {} missing", vs[i]);
                continue;
            }
            a.setAge(0);
            a.setYRot(i % 2 == 0 ? 35F : -145F);
            a.setYBodyRot(a.getYRot());
            a.setYHeadRot(a.getYRot());
            SmokeFreeze.freeze(a, true);
        }
        Axolotl larva = AxolotlCare.spawn(level, axo, CritterGenes.pure(AxolotlCare.GOLD, 0.1F), 1, new Vec3(ax + 2.0, base.getY() + 0.12, bz + 0.45));
        if (larva != null) {
            larva.setYRot(20F);
            SmokeFreeze.freeze(larva, true);
        }
        TankData at = axo.getTank();
        at.critterObjects().removeIf(o -> o.kind == CritterObject.Kind.AXOLOTL_EGGS);
        var spots = org.overengineer.bettacraft.tankmates.TankmateSpots.plants(level.getServer(), at, false);
        if (spots.isEmpty()) LOGGER.error("[smoketest] M29d no plant spot for the eggs");
        else {
            Vec3 s = spots.getFirst().pos().add(0, 0.05, -0.12); // on the front leaves of the right-most Rotala
            eggAt = s;
            CritterObject o = new CritterObject(at.nextCritterId(), CritterObject.Kind.AXOLOTL_EGGS, s.x, s.y, s.z);
            o.count = 5;
            o.mother = CritterGenes.WILD;
            o.father = CritterGenes.WILD;
            at.critterObjects().add(o);
        }
        axo.markTankChanged();
        TankCritters.sync(level, axo);
        LOGGER.info("[smoketest] M29d arranged {} crayfish, axolotl objects {}", t.critters().size(), at.critterObjects().size());
    }

    private static Critter put(TankData t, CritterMovement.Terrain terrain, CritterSpecies s, CritterGenes g, boolean female, double x, double z, float yaw,
                               boolean adult) {
        double y = terrain.ground(x, z);
        Critter k = new Critter(t.nextCritterId(), s, s.hasSexes() && female, g, x, Double.isFinite(y) ? y : t.min().getY() + 0.05, z);
        k.age = adult ? s.juvenileDays() + 2 : s.juvenileDays() * 0.3;
        k.yaw = yaw;
        k.idle = 1_000_000;
        t.critters().add(k);
        return k;
    }

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz, float temp) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, (long) sx * sy * sz * 900, 0F);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(p.getUUID());
        c.getTank().setWaterTemp(temp);
        c.getTank().setTargetTemp(temp);
        c.markTankChanged();
        return c;
    }

    // ------------------------------------------------------------------ helpers

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.level(), x, eyeY - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, false));
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

    private void server(java.util.function.Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.gui.toastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m29d_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
