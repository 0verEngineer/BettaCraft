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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.foreign.ForeignTerrarium;
import org.overengineer.bettacraft.gametest.zoo.ZooAnimalTests;
import org.overengineer.bettacraft.gametest.zoo.ZooPlantTests;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.terrarium.TerrariumBoxItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M42 part b client smoke ({@code -Pzoo -Psmoketestm42b}, background window): the miniature with animals and plants of mods we do
 * not integrate (the zoo): a 10 x 3 x 6 tank with every water animal without a profile that goes in (our bag), a 10 x 3 x 6
 * terrarium with the small land animals without a profile (allowed for the box by config, as a modpack would), and a 10 x 2 x 6
 * tank whose interior has every water plant without a plant profile that can be planted there. Built 200 blocks away, the
 * animals frozen in rows. Screenshots {@code bettacraft_m42b_water}, {@code _land}, {@code _plants} and close-ups.
 */
final class DevClientSmokeM42b {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private BlockPos water, land, plants;

    DevClientSmokeM42b() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(200, () -> server(pl -> place(pl.level())));
        then(1, () -> look(water.getX() + 5, water.getY() + 3.0, water.getZ() - 6.5, 0F, 18F));
        then(140, () -> shot("water"));
        then(1, () -> look(water.getX() + 2.5, water.getY() + 2.0, water.getZ() - 2.5, 0F, 18F));
        then(80, () -> shot("water_close"));
        then(1, () -> look(land.getX() + 5, land.getY() + 3.0, land.getZ() - 6.5, 0F, 18F));
        then(100, () -> shot("land"));
        then(1, () -> look(land.getX() + 2.5, land.getY() + 2.0, land.getZ() - 2.5, 0F, 22F));
        then(80, () -> shot("land_close"));
        then(1, () -> look(plants.getX() + 5, plants.getY() + 2.6, plants.getZ() - 5.5, 0F, 22F));
        then(100, () -> shot("plants"));
        then(1, () -> look(plants.getX() + 1.6, plants.getY() + 0.9, plants.getZ() - 0.6, 0F, 35F));
        then(80, () -> shot("plants_close"));
        then(1, () -> mc.options.hideGui = false);
    }

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlock(pos, ModBlocks.TANK.get().defaultBlockState(), 2);
        TankMultiblock.formAll(level, blocks);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankData t = c.getTank();
        t.setFront(Direction.NORTH);
        t.setSignVisible(false);
        t.setOwner(p.getUUID());
        t.setWaterTemp(25F);
        t.setTargetTemp(25F);
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

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        BlockPos base = p.blockPosition().offset(-12, 2, 200);
        water = base;
        TankBlockEntity wc = tank(level, p, water, 10, 3, 6);
        floor(level, wc, Blocks.SAND.defaultBlockState());
        TankInteractions.changeWater(level, wc, wc.getTank().capacityMb());
        land = base.offset(14, 0, 0);
        TankBlockEntity lc = tank(level, p, land, 10, 3, 6);
        floor(level, lc, Blocks.SAND.defaultBlockState());
        plants = base.offset(28, 0, 0);
        TankBlockEntity pc = tank(level, p, plants, 10, 2, 6);
        InteriorRegion pr = floor(level, pc, Blocks.SAND.defaultBlockState());
        TankInteractions.changeWater(level, pc, pc.getTank().capacityMb());
        // every plantable water plant without a profile, in rows of 8 over the interior (160 x 96)
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        int k = 0;
        for (Block b : ZooPlantTests.candidates()) {
            int lx = 10 + 18 * (k % 8), lz = 8 + 16 * (k / 8);
            if (lz >= pr.sizeZ() - 4) break;
            BlockPos at = pr.toWorld(lx, 0, lz);
            BlockState s = b.defaultBlockState();
            if (s.hasProperty(BlockStateProperties.WATERLOGGED)) s = s.setValue(BlockStateProperties.WATERLOGGED, true);
            BlockPos put = null;
            for (BlockState f : ZooPlantTests.FLOORS) {
                if (f.is(Blocks.SAND)) {
                    if (s.canSurvive(in, at)) {
                        put = at;
                        break;
                    }
                    continue;
                }
                in.setBlock(at, f, 3);
                if (s.canSurvive(in, at.above())) {
                    put = at.above();
                    break;
                }
                in.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
            }
            if (put == null) continue;
            if (b instanceof DoublePlantBlock) DoublePlantBlock.placeAt(in, s, put, 3);
            else in.setBlock(put, s, 3);
            LOGGER.info("[smoketest] M42b plant {} at {},{}", BuiltInRegistries.BLOCK.getKey(b), lx, lz);
            k++;
        }
        LOGGER.info("[smoketest] M42b zoo tanks at {}, {}, {}: {} plants", water, land, plants, k);
    }

    private void place(ServerLevel level) {
        TankBlockEntity wc = TankMultiblock.getController(level, water), lc = TankMultiblock.getController(level, land);
        if (wc == null || lc == null) {
            LOGGER.error("[smoketest] M42b no tanks");
            return;
        }
        org.overengineer.bettacraft.terrarium.TankClimate.step(level, lc, 0.01);
        // the water animals: in rows along the front glass
        TankData wt = wc.getTank();
        int i = 0;
        for (ZooAnimalTests.Cand c : ZooAnimalTests.waterAnimals(level)) {
            Entity e = c.type().create(level);
            if (e == null) continue;
            LivingEntity in = insert(level, wc, c.type(), FishBagItem.fillWith(e));
            e.discard();
            double x = wt.min().getX() + 0.8 + (i % 9) * 1.05, y = wt.min().getY() + 0.6 + (i / 9) * 1.3, z = wt.min().getZ() + 1.2;
            pose(in, x, y, z);
            if (in != null) LOGGER.info("[smoketest] M42b water {} in", c.id());
            i++;
        }
        // the land animals: allowed for the box as a modpack would; animals only (summons and familiars vanish on their own)
        // one of each mod first (the familiars and summons vanish on their own), then the rest
        List<ZooAnimalTests.Cand> all = ZooAnimalTests.landAnimals(level);
        List<ZooAnimalTests.Cand> walkers = new ArrayList<>();
        java.util.Set<String> mods = new java.util.HashSet<>();
        for (ZooAnimalTests.Cand c : all) if (!c.id().contains("familiar") && !c.id().contains("summon") && mods.add(c.mod())) walkers.add(c);
        for (ZooAnimalTests.Cand c : all) if (!walkers.contains(c) && !c.id().contains("familiar") && !c.id().contains("summon")) walkers.add(c);
        List<? extends String> before = ServerConfig.TERRA_BOX_ALLOW.get();
        ServerConfig.TERRA_BOX_ALLOW.set(all.stream().map(ZooAnimalTests.Cand::id).toList());
        TankData lt = lc.getTank();
        int j = 0;
        try {
            for (ZooAnimalTests.Cand c : walkers) {
                if (j >= 18) break;
                Entity e = c.type().create(level);
                if (e == null) continue;
                Vec3 at = Vec3.atCenterOf(land).add(0, 6, 0);
                e.snapTo(at.x, at.y, at.z, 0, 0);
                if (!ForeignTerrarium.boxTakes(e)) {
                    e.discard();
                    continue;
                }
                LivingEntity in = insert(level, lc, c.type(), TerrariumBoxItem.fillWith(e));
                e.discard();
                double x = lt.min().getX() + 0.8 + (j % 9) * 1.05, z = lt.min().getZ() + 1.0 + (j / 9) * 1.6;
                pose(in, x, lt.min().getY() + 0.05, z);
                if (in != null) LOGGER.info("[smoketest] M42b land {} in", c.id());
                j++;
            }
        } finally {
            ServerConfig.TERRA_BOX_ALLOW.set(before);
        }
        LOGGER.info("[smoketest] M42b {} water animals, {} land animals", i, j);
        wc.markTankChanged();
        lc.markTankChanged();
    }

    private static LivingEntity insert(ServerLevel level, TankBlockEntity c, net.minecraft.world.entity.EntityType<?> type, ItemStack stack) {
        List<Entity> before = TankInhabitants.liveInhabitants(level, c.getTank());
        var err = TankInhabitants.insert(level, c, stack, type, null, TankInhabitants.carriedWater(stack));
        if (err != null) {
            LOGGER.info("[smoketest] M42b {} refused: {}", BuiltInRegistries.ENTITY_TYPE.getKey(type), err.getString());
            return null;
        }
        for (Entity x : TankInhabitants.liveInhabitants(level, c.getTank())) if (!before.contains(x) && x instanceof LivingEntity le) return le;
        return null;
    }

    private static void pose(LivingEntity e, double x, double y, double z) {
        if (e == null) return;
        e.snapTo(x, y, z, 180F, 0F);
        e.setYHeadRot(180F);
        e.yBodyRot = 180F;
        if (e instanceof Mob m) SmokeFreeze.freeze(m, true);
    }

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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m42b_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
