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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.critters.TankTerrain;
import org.overengineer.bettacraft.mobs.GlassMobs;
import org.overengineer.bettacraft.mobs.MobJarItem;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M31 part d client smoke ({@code -Psmoketestm31d}): mobs in glass, 200 blocks away:
 * <ul>
 *     <li>an aquarium (4 x 3 x 3, sand, a stone ruin) with two drowned and a baby drowned on the bottom;</li>
 *     <li>a terrarium with a lid (sand, stone, a cobweb): a spider on the back glass, a cave spider, a skeleton and a zombie;</li>
 *     <li>a dry terrarium: a small magma cube, a small slime, a silverfish and an endermite;</li>
 *     <li>two item frames with the empty and the full mob jar.</li>
 * </ul>
 * Screenshots {@code bettacraft_m31d_*}: drowned, spiders, slimes, mob_jar. The client must see the mobs shrunk (scale synced).
 */
final class DevClientSmokeM31d {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private BlockPos drowned, spiders, slimes, frames;
    /** The spider on the glass: frozen, so the walker does not set its climbing flag; the scene does, every tick. */
    private Mob climber;

    DevClientSmokeM31d() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(160, () -> server(pl -> place(pl.level())));
        then(1, () -> look(drowned.getX() + 2.0, drowned.getY() + 1.9, drowned.getZ() - 1.6, 0F, 22F));
        then(120, () -> {
            List<Mob> seen = mc.level.getEntitiesOfClass(Mob.class, new AABB(drowned).inflate(40), m -> m.getScale() < 0.5F);
            LOGGER.info("[smoketest] M31d shrunk mobs seen by the client: {}", seen.size());
            if (seen.size() < 11) LOGGER.error("[smoketest] M31d expected 11 shrunk mobs on the client, got {}", seen.size());
            for (Mob m : seen) LOGGER.info("[smoketest] M31d client {} scale {} baby {}", m.getType().toShortString(), m.getScale(), m.isBaby());
            shot("drowned");
        });
        then(1, () -> look(spiders.getX() + 2.0, spiders.getY() + 1.9, spiders.getZ() - 1.7, 0F, 20F));
        then(80, () -> shot("spiders"));
        then(1, () -> look(slimes.getX() + 1.5, slimes.getY() + 1.5, slimes.getZ() - 1.2, 0F, 28F));
        then(80, () -> shot("slimes"));
        then(1, () -> look(frames.getX() + 1.0, frames.getY() + 1.5, frames.getZ() - 1.1, 0F, 0F));
        then(60, () -> shot("mob_jar"));
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
        t.setWaterTemp(22F);
        t.setTargetTemp(22F);
        c.markTankChanged();
        return c;
    }

    private InteriorRegion floor(ServerLevel level, TankBlockEntity c, BlockState sub) {
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), sub, false);
        }
        return r;
    }

    private static void box(ServerLevel in, InteriorRegion r, int x0, int x1, int y0, int y1, int z0, int z1, BlockState b) {
        for (int x = x0; x < x1; x++) for (int y = y0; y < y1; y++) for (int z = z0; z < z1; z++) in.setBlock(r.toWorld(x, y, z), b, 2);
    }

    private static void lid(ServerLevel level, BlockPos min, int sx, int sy, int sz) {
        for (int x = 0; x < sx; x++) for (int z = 0; z < sz; z++) level.setBlock(min.offset(x, sy, z), ModBlocks.TANK_LID.get().defaultBlockState(), 3);
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        ServerLevel in = InteriorManager.interiorLevel(p.level().getServer());
        org.overengineer.bettacraft.gametest.framework.DevTime.set(level, 6000);
        // --- drowned: an aquarium with a sand bottom and a ruined stone wall at the back
        drowned = p.blockPosition().offset(-12, 2, 200);
        TankBlockEntity dc = tank(level, p, drowned, 4, 3, 3);
        InteriorRegion dr = floor(level, dc, Blocks.SAND.defaultBlockState());
        box(in, dr, 6, 30, 0, 14, 40, 46, Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
        box(in, dr, 14, 20, 14, 22, 40, 46, Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
        for (int x = 36; x < 60; x += 5) box(in, dr, x, x + 1, 0, 10 + x % 7, 30 + x % 9, 31 + x % 9, Blocks.KELP_PLANT.defaultBlockState());
        TankInteractions.changeWater(level, dc, dc.getTank().capacityMb());
        dc.markTankChanged();
        // --- spiders: sand, a stone outcrop, cobwebs; a lid on top
        spiders = drowned.offset(7, 0, 0);
        TankBlockEntity sc = tank(level, p, spiders, 4, 3, 3);
        InteriorRegion sr = floor(level, sc, Blocks.COARSE_DIRT.defaultBlockState());
        box(in, sr, 42, 62, 0, 18, 30, 46, Blocks.STONE.defaultBlockState());
        box(in, sr, 46, 58, 18, 24, 34, 46, Blocks.COBBLESTONE.defaultBlockState());
        box(in, sr, 4, 12, 0, 6, 34, 44, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
        box(in, sr, 8, 9, 6, 7, 38, 39, Blocks.COBWEB.defaultBlockState());
        lid(level, spiders, 4, 3, 3);
        sc.markTankChanged();
        // --- slimes: a dry terrarium, 3 x 2 x 3, netherrack and basalt
        slimes = spiders.offset(7, 0, 0);
        TankBlockEntity lc = tank(level, p, slimes, 3, 2, 3);
        InteriorRegion lr = floor(level, lc, Blocks.SAND.defaultBlockState());
        box(in, lr, 26, 48, 0, 6, 26, 48, Blocks.NETHERRACK.defaultBlockState());
        box(in, lr, 34, 42, 6, 14, 34, 44, Blocks.BASALT.defaultBlockState());
        box(in, lr, 2, 12, 0, 4, 30, 40, Blocks.MOSS_BLOCK.defaultBlockState());
        lid(level, slimes, 3, 2, 3);
        lc.markTankChanged();
        // --- the mob jars in two item frames on a stone wall
        frames = slimes.offset(6, 0, 0);
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) level.setBlock(frames.offset(x, y, 1), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 3);
        LOGGER.info("[smoketest] M31d tanks at {}, {}, {}", drowned, spiders, slimes);
    }

    private TankBlockEntity controller(ServerLevel level, BlockPos at) {
        TankBlockEntity c = TankMultiblock.getController(level, at);
        if (c == null) throw new IllegalStateException("no tank at " + at);
        org.overengineer.bettacraft.terrarium.TankClimate.step(level, c, 0.01);
        LOGGER.info("[smoketest] M31d tank at {}: {}", at, org.overengineer.bettacraft.terrarium.TankClimate.refreshMode(level, c));
        return c;
    }

    private static Mob put(ServerLevel level, TankBlockEntity c, EntityType<? extends Mob> type, boolean baby) {
        Mob m = type.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (m == null) return null;
        if (baby) m.setBaby(true);
        if (m instanceof Slime s) s.setSize(1, true);
        m.setHealth(1F);
        ItemStack jar = MobJarItem.fillWith(m);
        m.discard();
        List<net.minecraft.world.entity.Entity> before = TankInhabitants.liveInhabitants(level, c.getTank());
        var err = TankInhabitants.insert(level, c, jar, type, null, 0L);
        if (err != null) {
            LOGGER.error("[smoketest] M31d {} refused: {}", type.toShortString(), err.getString());
            return null;
        }
        for (var e : TankInhabitants.liveInhabitants(level, c.getTank())) if (e instanceof Mob n && e.getType() == type && !before.contains(e)) {
            n.setHealth(n.getMaxHealth());
            return n;
        }
        return null;
    }

    private TankTerrain terrain(ServerLevel level, TankData t) {
        return new TankTerrain(t, level.getServer(), InteriorRegions.get(level.getServer()).get(t.regionId()), true);
    }

    /** Puts the mob at (dx, dz) of the tank on the ground (or at height {@code y} when finite), turned and frozen. */
    private static void at(Mob e, BlockPos min, TankTerrain ter, double dx, double dz, double y, float yaw) {
        if (e == null) return;
        double x = min.getX() + dx, z = min.getZ() + dz;
        double gy = Double.isFinite(y) ? min.getY() + y : ter.ground(x, z) + 0.01;
        e.snapTo(x, gy, z, yaw, 0F);
        e.setYHeadRot(yaw);
        e.yBodyRot = yaw;
        SmokeFreeze.freeze(e, true);
    }

    /** After the interiors are built: the mobs, posed and frozen. */
    private void place(ServerLevel level) {
        TankBlockEntity dc = controller(level, drowned);
        TankTerrain dter = terrain(level, dc.getTank());
        at(put(level, dc, EntityType.DROWNED, false), drowned, dter, 1.4, 1.2, Double.NaN, 200F);
        at(put(level, dc, EntityType.DROWNED, false), drowned, dter, 2.7, 1.6, Double.NaN, 150F);
        at(put(level, dc, EntityType.DROWNED, true), drowned, dter, 2.05, 0.9, Double.NaN, 180F);
        TankBlockEntity sc = controller(level, spiders);
        TankTerrain ster = terrain(level, sc.getTank());
        Mob spider = put(level, sc, EntityType.SPIDER, false);
        at(spider, spiders, ster, 1.5, 3 - TankTerrain.MARGIN - 0.01, 1.2, 0F); // on the back glass, facing it
        climber = spider;
        at(put(level, sc, EntityType.CAVE_SPIDER, false), spiders, ster, 0.8, 1.0, Double.NaN, 150F);
        at(put(level, sc, EntityType.SKELETON, false), spiders, ster, 2.4, 1.0, Double.NaN, 190F);
        at(put(level, sc, EntityType.ZOMBIE, false), spiders, ster, 3.3, 1.3, Double.NaN, 160F);
        TankBlockEntity lc = controller(level, slimes);
        TankTerrain lter = terrain(level, lc.getTank());
        at(put(level, lc, EntityType.MAGMA_CUBE, false), slimes, lter, 1.0, 0.9, Double.NaN, 170F);
        at(put(level, lc, EntityType.SLIME, false), slimes, lter, 2.1, 1.0, Double.NaN, 200F);
        at(put(level, lc, EntityType.SILVERFISH, false), slimes, lter, 1.6, 0.7, Double.NaN, 120F);
        at(put(level, lc, EntityType.ENDERMITE, false), slimes, lter, 2.5, 1.8, Double.NaN, 220F);
        // the jars
        ItemStack full = MobJarItem.fillWith(EntityType.ZOMBIE.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED));
        for (int i = 0; i < 2; i++) {
            ItemFrame f = new ItemFrame(level, frames.offset(i, 1, 0), Direction.NORTH);
            f.setItem(i == 0 ? new ItemStack(ModItems.MOB_JAR.get()) : full, false);
            f.setInvisible(false);
            level.addFreshEntity(f);
        }
        for (TankBlockEntity c : List.of(dc, sc, lc)) c.markTankChanged();
        LOGGER.info("[smoketest] M31d placed; spider glass mob {}", spider != null && GlassMobs.isGlassMob(spider));
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
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (climber instanceof net.minecraft.world.entity.monster.spider.Spider sp) server(p -> sp.setClimbing(true));
        if (steps.isEmpty()) return true;
        Step st = steps.peek();
        if (wait < st.delay()) {
            wait++;
            return false;
        }
        steps.poll();
        wait = 0;
        st.action().run();
        return steps.isEmpty();
    }

    private void server(Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m31d_" + name + ".png", mc.getMainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
