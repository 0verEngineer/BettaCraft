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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.client.interior.PlantMeshRenderer;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M21 part of the dev client smoke test: the plant prototype. A 3x2x2 tank under a light with Rotala, Vallisneria and
 * Anubias (on a rock), grown. Screenshots {@code bettacraft_m21_*}: the tank from outside (the miniature keeps the
 * blocks), inside the interior with the new look ({@code plants_new_*}) and the same views with
 * {@code features.plantMeshes} off ({@code plants_old_*}); the new fish buckets in the hotbar.
 */
final class DevClientSmokeM21 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private record View(String name, int x, int y, int z, float yaw, float pitch) {}

    private static final List<View> VIEWS = List.of(
            new View("wide", 24, 10, 2, 0F, 8F),
            new View("rotala", 10, 8, 10, 0F, 0F),
            new View("vallisneria", 38, 9, 10, 0F, 2F),
            new View("anubias", 24, 6, 9, 0F, 28F));

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private BlockPos base;
    private int wait;
    private volatile InteriorRegion region;
    private final boolean saved = CommonConfig.plantMeshes();

    DevClientSmokeM21() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(1, () -> setMeshes(true));
        then(120, () -> look(base.getX() + 1.5, base.getY() + 1.6, base.getZ() - 2.6, 0F, 12F));
        then(100, () -> shot("outside_miniature"));
        for (boolean meshes : new boolean[] {true, false}) {
            then(1, () -> setMeshes(meshes));
            for (View v : VIEWS) {
                then(1, () -> server(p -> {
                    InteriorRegion r = region;
                    if (r == null) return;
                    ServerLevel in = InteriorManager.interiorLevel(p.server);
                    BlockPos at = r.toWorld(v.x(), v.y(), v.z());
                    p.teleportTo(in, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, v.yaw(), v.pitch());
                }));
                then(v == VIEWS.getFirst() ? 140 : 70, () -> {
                    if (meshes) {
                        LOGGER.info("[smoketest] M21 plant meshes: {} roots known, {} plants drawn", PlantMeshRenderer.knownRoots(), PlantMeshRenderer.lastDrawn);
                        if (PlantMeshRenderer.lastDrawn == 0 && v == VIEWS.getFirst()) LOGGER.error("[smoketest] M21 no plant mesh drawn in the interior");
                    }
                    shot("plants_" + (meshes ? "new" : "old") + "_" + v.name());
                });
            }
        }
        then(1, () -> setMeshes(saved));
        then(1, () -> server(p -> {
            if (InteriorManager.isInterior(p.level())) p.teleportTo(p.server.overworld(), base.getX() + 1.5, base.getY() + 1.0, base.getZ() - 3.0, 0F, 10F);
        }));
        // the new fish buckets in the hotbar
        then(60, () -> server(p -> {
            p.getInventory().clearContent();
            List<ItemStack> items = new ArrayList<>();
            items.add(new ItemStack(ModItems.BETTA_BUCKET.get()));
            for (var e : ModItems.TANKMATE_BUCKETS.values()) if (items.size() < 8) items.add(new ItemStack(e.get()));
            items.add(new ItemStack(ModItems.FROG_BUCKET.get()));
            for (int i = 0; i < items.size() && i < 9; i++) p.getInventory().setItem(i, items.get(i));
        }));
        then(1, () -> mc.options.hideGui = false);
        then(30, () -> shot("buckets_hotbar"));
        then(1, () -> mc.options.hideGui = true);
    }

    private void setMeshes(boolean on) {
        CommonConfig.FEATURE_PLANT_MESHES.set(on);
        if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
    }

    private static void place(ServerLevel level, List<BlockPos> blocks) {
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ServerLevel in = InteriorManager.interiorLevel(p.server);
        base = p.blockPosition().offset(-20, 2, 260);
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
        // a rock for the anubias in the middle
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
        LOGGER.info("[smoketest] M21 plant scene at {}: {} plants, {} plant blocks", base, r.plants().size(), r.plants().blocks());
    }

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.serverLevel(), x, eyeY - p.getEyeHeight(), z, yaw, pitch));
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m21_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
