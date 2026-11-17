package org.overengineer.bettacraft.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M32 part b client smoke ({@code -Pcreate -Psmoketestm32b}): a tank travelling on Create and Create Aeronautics.
 * <ul>
 *     <li>a 3 x 2 x 2 tank on a mechanical bearing (sand, stones, kelp in the interior; bettas and neon tetras): standing,
 *     then lifted by the bearing and held at 30 and at 70 degrees (the miniature and the packed animals drawn at the
 *     contraption's pose);</li>
 *     <li>a 2 x 2 x 2 tank on an iron plate assembled into a Sable sub-level (an airship) that falls onto the ground.</li>
 * </ul>
 * Screenshots {@code bettacraft_m32b_*}: standing, bearing, bearing_turned, airship.
 */
final class DevClientSmokeM32b {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private BlockPos bearingPos, shipPos;
    private BlockEntity bearing;
    /** The angle the bearing is held at (it has no rotation source). */
    private float hold = Float.NaN;

    DevClientSmokeM32b() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(1, () -> look(bearingPos.getX() + 1.5, bearingPos.getY() + 2.6, bearingPos.getZ() - 3.6, 0F, 18F));
        then(220, () -> shot("standing"));
        then(1, () -> server(p -> assemble(30F)));
        then(80, () -> {
            long n = mc.level.getEntitiesOfClass(Entity.class, new net.minecraft.world.phys.AABB(bearingPos).inflate(8),
                    e -> e.getType().toShortString().contains("contraption")).size();
            LOGGER.info("[smoketest] M32b contraption entities seen by the client: {}", n);
            if (n == 0) LOGGER.error("[smoketest] M32b the client sees no contraption");
            shot("bearing");
        });
        then(1, () -> hold = 70F);
        then(60, () -> shot("bearing_turned"));
        then(1, () -> server(p -> airship(p.level())));
        then(1, () -> look(shipPos.getX() + 1.0, shipPos.getY() + 2.4, shipPos.getZ() - 3.4, 0F, 22F));
        then(200, () -> {
            for (TankBlockEntity be : TankBlockEntity.CLIENT_CONTROLLERS) {
                if (be.getLevel() != mc.level) continue;
                LOGGER.info("[smoketest] M32b client tank at {}: sub-level {}, travelling {}, packed {}", be.getBlockPos(),
                        org.overengineer.bettacraft.compat.create.SableCompat.inSubLevel(mc.level, be.getBlockPos()),
                        org.overengineer.bettacraft.client.render.TravellingTankRenderer.travelling(be), be.stowed().size());
            }
            shot("airship");
        });
        then(1, () -> mc.options.hideGui = false);
    }

    // ------------------------------------------------------------------ the scene

    private static TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlock(pos, ModBlocks.TANK.get().defaultBlockState(), 2);
        TankData t = TankMultiblock.formAll(level, blocks);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        t.setFront(Direction.NORTH);
        t.setSignVisible(false);
        t.setOwner(p.getUUID());
        t.setWaterTemp(26F);
        t.setTargetTemp(26F);
        TankInteractions.changeWater(level, c, t.capacityMb());
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.SAND.defaultBlockState(), false);
        }
        for (int x = 4; x < r.sizeX() - 4; x += 7) {
            int h = 6 + x % 9;
            for (int y = 0; y < h; y++) in.setBlock(r.toWorld(x, y, r.sizeZ() - 6), Blocks.KELP_PLANT.defaultBlockState(), 2);
        }
        for (int x = 8; x < 16; x++) for (int y = 0; y < 5; y++) for (int z = 18; z < 26; z++) in.setBlock(r.toWorld(x, y, z), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 2);
        c.markTankChanged();
        return c;
    }

    private static void fish(ServerLevel level, TankBlockEntity c, int bettas, int tetras) {
        RandomSource r = RandomSource.create(32);
        for (int i = 0; i < bettas; i++) {
            BettaData d = BettaData.create(BettaGenotype.randomDomestic(r), i % 2 == 1, 0);
            TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d), ModEntities.BETTA.get(), null, false);
        }
        var w = new org.overengineer.bettacraft.tank.TankWaterVolume(c.getTank());
        var b = w.box();
        for (int i = 0; i < tetras; i++) {
            Vec3 p = w.clamp(new Vec3(b.minX + (b.maxX - b.minX) * (0.2 + 0.15 * i), b.minY + 0.6, (b.minZ + b.maxZ) / 2), 0.3, 0.1, 0.2);
            org.overengineer.bettacraft.tankmates.TankmateBreeding.spawn(level, c, org.overengineer.bettacraft.tankmates.TankmateSpecies.NEON_TETRA,
                    org.overengineer.bettacraft.tankmates.TankmateGenes.wild(org.overengineer.bettacraft.tankmates.TankmateSpecies.NEON_TETRA), i % 2 == 0,
                    org.overengineer.bettacraft.entity.betta.LifeStage.ADULT, 0, p);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState with(BlockState s, String property, Comparable value) {
        for (Property<?> p : s.getProperties()) if (p.getName().equals(property)) return s.setValue((Property) p, value);
        return s;
    }

    private static Field field(Class<?> c, String name) throws NoSuchFieldException {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try {
                Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        level.setDayTime(6000);
        bearingPos = p.blockPosition().offset(0, 6, 160);
        var bearingBlock = BuiltInRegistries.BLOCK.getValue(Identifier.parse("create:mechanical_bearing"));
        if (bearingBlock == Blocks.AIR) {
            LOGGER.error("[smoketest] M32b needs Create (-Pcreate)");
            return;
        }
        for (int y = 1; y < 8; y++) level.setBlock(bearingPos.below(y), Blocks.STONE_BRICKS.defaultBlockState(), 2);
        level.setBlock(bearingPos, with(bearingBlock.defaultBlockState(), "facing", Direction.UP), 3);
        bearing = level.getBlockEntity(bearingPos);
        TankBlockEntity c = tank(level, p, bearingPos.above(), 3, 2, 2);
        fish(level, c, 3, 3);
        LOGGER.info("[smoketest] M32b bearing tank at {}: {} animals", c.getBlockPos(), TankInhabitants.liveInhabitants(level, c.getTank()).size());
        shipPos = bearingPos.offset(10, 0, 0);
    }

    private void assemble(float degrees) {
        try {
            bearing.getClass().getMethod("assemble").invoke(bearing);
            Object moved = field(bearing.getClass(), "movedContraption").get(bearing);
            LOGGER.info("[smoketest] M32b bearing assembled: {}", moved != null);
            if (moved == null) LOGGER.error("[smoketest] M32b the bearing did not take the tank: {}", field(bearing.getClass(), "lastException").get(bearing));
            hold = degrees;
            angle(degrees);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("[smoketest] M32b bearing", e);
        }
    }

    /** Holds the bearing (no rotation source) at this angle. */
    private void angle(float degrees) {
        try {
            field(bearing.getClass(), "assembleNextTick").setBoolean(bearing, false);
            field(bearing.getClass(), "angle").setFloat(bearing, degrees);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("[smoketest] M32b angle", e);
        }
    }

    /** A tank on an iron plate on the ground, assembled into a Sable sub-level like the physics assembler does. */
    private void airship(ServerLevel level) {
        BlockPos ground = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, shipPos);
        shipPos = ground.above();
        ServerPlayer p = level.getServer().getPlayerList().getPlayers().getFirst();
        for (int x = -1; x <= 2; x++) for (int z = -1; z <= 2; z++) level.setBlock(ground.offset(x, 0, z), Blocks.IRON_BLOCK.defaultBlockState(), 3);
        TankBlockEntity c = tank(level, p, shipPos, 2, 2, 2);
        fish(level, c, 2, 2);
        try {
            Class<?> helper = Class.forName("dev.ryanhcode.sable.api.SubLevelAssemblyHelper");
            Class<?> predicate = Class.forName("dev.ryanhcode.sable.api.SubLevelAssemblyHelper$FrontierPredicate");
            Object ours = Proxy.newProxyInstance(predicate.getClassLoader(), new Class<?>[]{predicate}, (pr, m, a) -> {
                if (m.getName().equals("isValidConnection")) {
                    BlockState s = (BlockState) a[3];
                    return s.getBlock() instanceof TankBlock || s.is(Blocks.IRON_BLOCK);
                }
                return m.getName().equals("hashCode") ? 0 : m.getName().equals("equals") ? pr == a[0] : null;
            });
            Method gather = null, assemble = null;
            for (Method m : helper.getMethods()) {
                if (m.getName().equals("gatherConnectedBlocks")) gather = m;
                if (m.getName().equals("assembleBlocks")) assemble = m;
            }
            Object g = gather.invoke(null, shipPos, level, 256, ours);
            Object blocks = g.getClass().getMethod("blocks").invoke(g);
            Object bounds = g.getClass().getMethod("boundingBox").invoke(g);
            assemble.invoke(null, level, shipPos, blocks, bounds);
            LOGGER.info("[smoketest] M32b airship assembled from {} blocks", ((java.util.Set<?>) blocks).size());
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.error("[smoketest] M32b airship (Sable)", e);
        }
    }

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
        if (bearing != null && !Float.isNaN(hold)) {
            float a = hold;
            server(p -> angle(a));
        }
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m32b_" + name + ".png", mc.getMainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
