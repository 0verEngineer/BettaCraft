package org.overengineer.bettacraft.client;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.client.interior.IrisCompat;
import org.overengineer.bettacraft.client.interior.MiniaturePlants;
import org.overengineer.bettacraft.client.interior.PlantMeshRenderer;
import org.overengineer.bettacraft.client.render.FloatingPlantRenderer;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tank.plants.TankPlants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M21.1 dev client smoke scene ({@code -Psmoketestm21_1}): the plant meshes outside in the miniature, the Rotala stems,
 * the Vallisneria ribbons along the surface and the floating plants without z-fighting. Screenshots
 * {@code bettacraft_m21_1_*}:
 * <ul>
 *     <li>outside, near and far, with the meshes ({@code _new}) and as voxels ({@code _old},
 *     {@code render.miniaturePlantMeshDistance = 0}); a view beyond the LOD distance must show voxels;</li>
 *     <li>inside: Rotala and Vallisneria from below (the views of the bug pictures);</li>
 *     <li>a shear cut must show in the miniature's mesh; with a shader pack the meshes outside cast shadows (off/on);</li>
 *     <li>a tank of overlapping lily pads and other floating plants from above, with and without the height layers.</li>
 * </ul>
 */
final class DevClientSmokeM211 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private BlockPos base, pond;
    private int wait;
    private volatile InteriorRegion region;
    private final boolean savedMeshes = Features.enabled(Feature.PLANT_MESHES);
    private final int savedLod = ClientConfig.MINIATURE_PLANT_MESH_DISTANCE.get();
    private int cutBefore;

    DevClientSmokeM211() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(1, () -> setMeshes(true));
        // outside: near and far, new (meshes) and old (voxels)
        for (int lod : new int[] {24, 0}) {
            String tag = lod > 0 ? "new" : "old";
            then(1, () -> ClientConfig.MINIATURE_PLANT_MESH_DISTANCE.set(lod));
            then(1, () -> look(base.getX() + 1.5, base.getY() + 1.3, base.getZ() - 2.2, 0F, 8F));
            then(lod > 0 ? 160 : 60, () -> {
                if (lod > 0) {
                    LOGGER.info("[smoketest] M21.1 outside near: {} tank(s) with meshes, {} plants drawn, {} meshes built, {} ms render thread",
                            MiniaturePlants.lastTanks, MiniaturePlants.lastDrawn, MiniaturePlants.builds, MiniaturePlants.lastNanos / 1e6);
                    if (MiniaturePlants.lastTanks < 1 || MiniaturePlants.lastDrawn < 5) LOGGER.error("[smoketest] M21.1 plant meshes not shown outside");
                } else if (MiniaturePlants.lastTanks != 0) {
                    LOGGER.error("[smoketest] M21.1 plant meshes shown outside with the distance 0");
                }
                shot("outside_near_" + tag);
            });
            then(1, () -> look(base.getX() + 0.8, base.getY() + 0.9, base.getZ() - 0.9, 0F, 10F));
            then(50, () -> shot("outside_rotala_" + tag));
            then(1, () -> look(base.getX() + 2.4, base.getY() + 1.0, base.getZ() - 0.9, 0F, 10F));
            then(50, () -> shot("outside_vallisneria_" + tag));
            then(1, () -> look(base.getX() + 1.5, base.getY() + 3.0, base.getZ() - 12.0, 0F, 9F));
            then(60, () -> shot("outside_far_" + tag));
        }
        then(1, () -> ClientConfig.MINIATURE_PLANT_MESH_DISTANCE.set(24));
        // beyond the LOD distance: voxels again
        then(1, () -> look(base.getX() + 1.5, base.getY() + 4.0, base.getZ() - 32.0, 0F, 6F));
        then(60, () -> {
            LOGGER.info("[smoketest] M21.1 beyond the LOD distance: {} tank(s) with meshes", MiniaturePlants.lastTanks);
            if (MiniaturePlants.lastTanks != 0) LOGGER.error("[smoketest] M21.1 plant meshes shown beyond the LOD distance");
            shot("outside_lod");
        });
        // with a shader pack: the meshes outside cast shadows (off/on)
        then(1, () -> look(base.getX() + 1.5, base.getY() + 1.3, base.getZ() - 2.2, 0F, 8F));
        then(60, () -> PlantMeshRenderer.shadows = false);
        then(30, () -> {
            if (IrisCompat.shaderPackInUse()) shot("outside_shadow_off");
            PlantMeshRenderer.shadows = true;
        });
        then(30, () -> {
            if (IrisCompat.shaderPackInUse()) {
                LOGGER.info("[smoketest] M21.1 shadow pass outside: {} plants", PlantMeshRenderer.lastShadowDrawn);
                if (PlantMeshRenderer.lastShadowDrawn == 0) LOGGER.error("[smoketest] M21.1 plant meshes outside not in the shadow pass");
                shot("outside_shadow_on");
            }
        });
        // a shear cut shows in the miniature's mesh
        then(1, () -> {
            cutBefore = MiniaturePlants.meshBlocks(region.id(), 7, 0, 24);
            server(p -> {
                InteriorRegion r = region;
                ServerLevel in = InteriorManager.interiorLevel(p.server);
                for (InteriorPlant pl : r.plants().all()) {
                    if (pl.species == PlantSpecies.ROTALA && pl.x == 7 && pl.y == 0 && pl.z == 24) InteriorPlants.trim(in, r, pl, null, null, null);
                }
            });
        });
        then(60, () -> {
            int after = MiniaturePlants.meshBlocks(region.id(), 7, 0, 24);
            LOGGER.info("[smoketest] M21.1 shear cut outside: mesh blocks {} -> {}", cutBefore, after);
            if (cutBefore <= 0 || after <= 0 || after >= cutBefore) LOGGER.error("[smoketest] M21.1 shear cut not in the miniature's mesh");
            shot("outside_cut");
        });
        // inside: the Rotala and the Vallisneria from below (like the bug pictures)
        inside("inside_rotala", 9, 3, 14, 0F, -55F);
        inside("inside_rotala_side", 3, 9, 20, -90F, -8F);
        inside("inside_vallisneria", 38, 4, 16, 0F, -60F);
        inside("inside_vallisneria_side", 30, 22, 22, -90F, -28F);
        // floating plants from above: height layers on and off (the lily pads close up, then the whole tank)
        then(1, () -> server(p -> p.teleportTo(p.server.overworld(), pond.getX() + 0.85, pond.getY() + 2.0 - p.getEyeHeight(), pond.getZ() + 0.9, 0F, 90F)));
        then(100, () -> shot("lily_top_new"));
        then(1, () -> FloatingPlantRenderer.layered = false);
        then(20, () -> shot("lily_top_old"));
        then(1, () -> FloatingPlantRenderer.layered = true);
        then(1, () -> server(p -> p.teleportTo(p.server.overworld(), pond.getX() + 1.5, pond.getY() + 3.1 - p.getEyeHeight(), pond.getZ() + 1.0, 0F, 90F)));
        then(40, () -> shot("pond_top_new"));
        then(1, () -> FloatingPlantRenderer.layered = false);
        then(20, () -> shot("pond_top_old"));
        then(1, () -> FloatingPlantRenderer.layered = true);
        then(1, () -> look(pond.getX() + 1.5, pond.getY() + 2.6, pond.getZ() - 0.6, 0F, 50F));
        then(30, () -> shot("lily_angle_new"));
        then(1, () -> FloatingPlantRenderer.layered = false);
        then(20, () -> shot("lily_angle_old"));
        then(1, () -> FloatingPlantRenderer.layered = true);
        then(1, () -> {
            Features.set(Feature.PLANT_MESHES, savedMeshes);
            ClientConfig.MINIATURE_PLANT_MESH_DISTANCE.set(savedLod);
        });
    }

    private void inside(String name, int x, int y, int z, float yaw, float pitch) {
        then(1, () -> server(p -> {
            InteriorRegion r = region;
            BlockPos at = r.toWorld(x, y, z);
            p.teleportTo(InteriorManager.interiorLevel(p.server), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, pitch);
        }));
        then(100, () -> shot(name));
    }

    private void setMeshes(boolean on) {
        Features.set(Feature.PLANT_MESHES, on);
        if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
    }

    private static void place(ServerLevel level, List<BlockPos> blocks) {
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ServerLevel in = InteriorManager.interiorLevel(p.server);
        base = p.blockPosition().offset(-20, 2, 300);
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) for (int x = 0; x < 3; x++) blocks.add(base.offset(x, y, z));
        place(level, blocks);
        TankBlockEntity c = TankMultiblock.getController(level, base);
        TankInteractions.changeWater(level, c, 11000);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(p.getUUID());
        c.getTank().setWaterTemp(26F);
        for (int x = 0; x < 3; x++) {
            level.setBlockAndUpdate(base.offset(x, 2, 1), ModBlocks.TANK_LIGHT.get().defaultBlockState()
                    .setValue(TankLightBlock.AXIS, Direction.Axis.X).setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 2));
        }
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        region = r;
        for (int x = 0; x < r.sizeX(); x++)
            for (int z = 0; z < r.sizeZ(); z++) {
                BlockState s = z < 10 ? Blocks.SAND.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
                Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), s, false);
            }
        for (int x = -4; x <= 4; x++)
            for (int z = -3; z <= 3; z++) {
                double d = Math.sqrt(x * x / 16.0 + z * z / 9.0);
                if (d > 1) continue;
                int h = (int) Math.round(4 * (1 - d * d * 0.6));
                for (int y = 0; y < h; y++) in.setBlock(r.toWorld(24 + x, y, 18 + z), (x + y) % 3 == 0 ? Blocks.ANDESITE.defaultBlockState()
                        : Blocks.STONE.defaultBlockState(), 2);
            }
        long seed = 2100;
        InteriorPlants.plantAt(in, r, PlantSpecies.ROTALA, 7, 0, 24, PlantSpecies.ROTALA.stages(), seed++);
        InteriorPlants.plantAt(in, r, PlantSpecies.ROTALA, 13, 0, 20, PlantSpecies.ROTALA.stages(), seed++);
        InteriorPlants.plantAt(in, r, PlantSpecies.VALLISNERIA, 36, 0, 25, PlantSpecies.VALLISNERIA.stages(), seed++);
        InteriorPlants.plantAt(in, r, PlantSpecies.VALLISNERIA, 41, 0, 20, PlantSpecies.VALLISNERIA.stages(), seed++);
        int top = 0;
        while (top < r.sizeY() && !in.getBlockState(r.toWorld(24, top, 18)).isAir()) top++;
        InteriorPlants.plantAt(in, r, PlantSpecies.ANUBIAS_NANA, 24, top, 18, PlantSpecies.ANUBIAS_NANA.stages(), seed++);
        InteriorPlants.settleTint(level, c);

        // a shallow tank with overlapping lily pads and other floating plants
        pond = base.offset(6, 0, 0);
        List<BlockPos> pb = new ArrayList<>();
        for (int z = 0; z < 2; z++) for (int x = 0; x < 3; x++) pb.add(pond.offset(x, 0, z));
        place(level, pb);
        TankBlockEntity pc = TankMultiblock.getController(level, pond);
        TankInteractions.changeWater(level, pc, 5500);
        pc.getTank().setFront(Direction.NORTH);
        pc.getTank().setSignVisible(false);
        pc.getTank().plants().clear();
        double px = pond.getX(), pz = pond.getZ();
        double[][] lily = {{0.55, 0.6}, {0.8, 0.75}, {0.62, 0.95}, {0.95, 0.5}, {1.05, 0.9}, {0.75, 1.2}, {1.2, 1.25}};
        for (int i = 0; i < lily.length; i++) TankPlants.add(pc, FloatingPlantType.LILY_PAD, px + lily[i][0], pz + lily[i][1], 1F, i * 1.3F);
        for (int i = 0; i < 4; i++) TankPlants.add(pc, FloatingPlantType.FROGBIT, px + 1.7 + i * 0.12, pz + 0.7 + (i % 2) * 0.15, 1F, i * 0.9F);
        for (int i = 0; i < 4; i++) TankPlants.add(pc, FloatingPlantType.SALVINIA, px + 2.2 + (i % 2) * 0.15, pz + 1.1 + i * 0.1, 1F, i * 1.1F);
        for (int i = 0; i < 3; i++) TankPlants.add(pc, FloatingPlantType.RED_ROOT_FLOATER, px + 1.6 + i * 0.15, pz + 1.4, 1F, i * 0.7F);
        for (int i = 0; i < 4; i++) TankPlants.add(pc, FloatingPlantType.DUCKWEED, px + 2.4 + i * 0.08, pz + 0.5 + i * 0.06, 1F, i * 0.5F);
        TankPlants.sync(level, pc);
        LOGGER.info("[smoketest] M21.1 scene at {}: {} plants, {} plant blocks; pond at {} with {} floating plants", base, r.plants().size(),
                r.plants().blocks(), pond, pc.getTank().plants().size());
    }

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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m21_1_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
