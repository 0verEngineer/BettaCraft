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
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.client.render.TravelWater;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
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
 * M50 client smoke ({@code -Pcreate -Psmoketestm50}): the care on the road and the sloshing water.
 * <ul>
 *     <li>a 3 x 2 x 2 tank on a mechanical bearing that turns fast (the water pushed outwards in the turn) and stops at
 *     once (it swings back);</li>
 *     <li>a 2 x 1 x 2 tank on a minecart contraption (a cart assembler) that runs along a rail line into a curve;</li>
 *     <li>a 2 x 2 x 2 tank on an iron plate assembled into a Sable sub-level (an airship): the bettas swim in it live.</li>
 * </ul>
 * Screenshots {@code bettacraft_m50_*}: bearing_turn, bearing_stop, cart, cart_curve, airship.
 */
final class DevClientSmokeM50 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private BlockPos bearingPos, railStart, curve, shipPos;
    private BlockEntity bearing;
    private float angle = Float.NaN, speed;
    private Minecart cart;
    private boolean pushing;

    DevClientSmokeM50() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(1, () -> look(bearingPos.getX() + 1.5, bearingPos.getY() + 3.6, bearingPos.getZ() - 4.6, 0F, 24F));
        then(200, () -> server(p -> assemble()));
        then(10, () -> speed = 4.5F);
        then(70, () -> {
            logTilt("bearing turning");
            shot("bearing_turn");
        });
        then(1, () -> speed = 0F);
        then(9, () -> {
            logTilt("bearing stopped");
            shot("bearing_stop");
        });
        then(1, () -> look(curve.getX() - 3.0, curve.getY() + 4.2, curve.getZ() - 4.2, -30F, 42F));
        then(60, () -> server(this::startCart));
        then(30, () -> {
            logTilt("cart running");
            shot("cart");
        });
        then(1, this::waitForCurve);
        then(1, () -> server(p -> airship(p.level())));
        then(1, () -> look(shipPos.getX() + 1.0, shipPos.getY() + 2.6, shipPos.getZ() - 3.4, 0F, 24F));
        then(200, () -> {
            int live = 0, packed = 0;
            for (TankBlockEntity be : TankBlockEntity.CLIENT_CONTROLLERS) {
                if (be.getLevel() != mc.level || !org.overengineer.bettacraft.compat.create.SableCompat.inSubLevel(mc.level, be.getBlockPos())) continue;
                packed += be.stowed().size();
                AABB box = new AABB(be.getTank().min().getCenter(), be.getTank().max().getCenter()).inflate(1.5);
                live += mc.level.getEntitiesOfClass(BettaEntity.class, box).size();
            }
            LOGGER.info("[smoketest] M50 airship: {} betta(s) live on the ship for the client, {} packed", live, packed);
            if (live < 2) LOGGER.error("[smoketest] M50 the bettas do not swim on the airship");
            shot("airship");
        });
        then(1, () -> {
            int received;
            try {
                received = org.overengineer.bettacraft.compat.create.CreateTravelClient.received;
            } catch (LinkageError e) {
                received = -1;
            }
            LOGGER.info("[smoketest] M50 travelling tank states received by the client: {}", received);
            if (received <= 0) LOGGER.error("[smoketest] M50 the client got no state of a travelling tank");
            mc.options.hideGui = false;
        });
    }

    // ------------------------------------------------------------------ the scene

    private static TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlock(pos, ModBlocks.TANK.get().defaultBlockState(), 2);
        TankData t = TankMultiblock.formAll(level, blocks);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        t.setFront(Direction.NORTH);
        t.setSignVisible(true);
        t.setName("Travel " + sx + "x" + sy + "x" + sz);
        t.setOwner(p.getUUID());
        t.setWaterTemp(26F);
        t.setTargetTemp(26F);
        TankInteractions.changeWater(level, c, Math.round(t.capacityMb() * 0.8));
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.SAND.defaultBlockState(), false);
        }
        for (int x = 4; x < r.sizeX() - 4; x += 7) {
            int h = 5 + x % 7;
            for (int y = 0; y < h; y++) in.setBlock(r.toWorld(x, y, r.sizeZ() - 6), Blocks.KELP_PLANT.defaultBlockState(), 2);
        }
        c.markTankChanged();
        return c;
    }

    private static void fish(ServerLevel level, TankBlockEntity c, int bettas) {
        RandomSource r = RandomSource.create(50);
        for (int i = 0; i < bettas; i++) {
            BettaData d = BettaData.create(BettaGenotype.randomDomestic(r), i % 2 == 1, 0);
            TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d), ModEntities.BETTA.get(), null, false);
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
        level.getGameRules().getRule(net.minecraft.world.level.gamerules.GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        bearingPos = p.blockPosition().offset(0, 6, 160);
        var bearingBlock = BuiltInRegistries.BLOCK.getValue(Identifier.parse("create:mechanical_bearing"));
        if (bearingBlock == Blocks.AIR) {
            LOGGER.error("[smoketest] M50 needs Create (-Pcreate)");
            return;
        }
        for (int y = 1; y < 8; y++) level.setBlock(bearingPos.below(y), Blocks.STONE_BRICKS.defaultBlockState(), 2);
        level.setBlock(bearingPos, with(bearingBlock.defaultBlockState(), "facing", Direction.UP), 3);
        bearing = level.getBlockEntity(bearingPos);
        TankBlockEntity c = tank(level, p, bearingPos.above(), 3, 2, 2);
        fish(level, c, 3);
        // the rail line: 12 blocks east, a curve to the south, 8 blocks south; a cart assembler with a tank above it
        railStart = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, bearingPos.offset(-30, 0, 20));
        curve = railStart.offset(14, 0, 0);
        for (int x = -1; x <= 16; x++) for (int z = -2; z <= 11; z++) {
            level.setBlock(railStart.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            for (int y = 0; y < 4; y++) level.setBlock(railStart.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        BlockState ew = Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST);
        BlockState ns = Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH);
        for (int x = 0; x < 14; x++) level.setBlock(railStart.offset(x, 0, 0), ew, 2);
        level.setBlock(curve, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.SOUTH_WEST), 2);
        for (int z = 1; z <= 9; z++) level.setBlock(curve.offset(0, 0, z), ns, 2);
        var assembler = BuiltInRegistries.BLOCK.getValue(Identifier.parse("create:cart_assembler"));
        BlockPos at = railStart.offset(3, 0, 0);
        level.setBlock(at, with(with(assembler.defaultBlockState(), "shape", RailShape.EAST_WEST), "powered", true), 3);
        level.setBlock(at.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        TankBlockEntity cartTank = tank(level, p, at.above(), 2, 1, 2);
        fish(level, cartTank, 2);
        LOGGER.info("[smoketest] M50 bearing tank at {}, cart tank at {}", c.getBlockPos(), cartTank.getBlockPos());
        shipPos = bearingPos.offset(10, 0, 0);
    }

    private void assemble() {
        try {
            bearing.getClass().getMethod("assemble").invoke(bearing);
            Object moved = field(bearing.getClass(), "movedContraption").get(bearing);
            LOGGER.info("[smoketest] M50 bearing assembled: {}", moved != null);
            if (moved == null) LOGGER.error("[smoketest] M50 the bearing did not take the tank: {}", field(bearing.getClass(), "lastException").get(bearing));
            angle = 0F;
            hold(0F);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("[smoketest] M50 bearing", e);
        }
    }

    private void hold(float degrees) {
        try {
            field(bearing.getClass(), "assembleNextTick").setBoolean(bearing, false);
            field(bearing.getClass(), "angle").setFloat(bearing, degrees);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("[smoketest] M50 angle", e);
        }
    }

    private void startCart(ServerPlayer p) {
        ServerLevel level = p.level();
        cart = new Minecart(level, railStart.getX() + 0.5, railStart.getY() + 0.1, railStart.getZ() + 0.5);
        level.addFreshEntity(cart);
        pushing = true;
    }

    /** Waits until the cart is in the curve (at most 6 s), then takes the picture. */
    private void waitForCurve() {
        int[] n = {0};
        steps.addFirst(new Step(1, new Runnable() {
            @Override
            public void run() {
                Vec3 c = cart == null ? null : cart.position();
                boolean in = c != null && c.x > curve.getX() - 0.3 && c.z > curve.getZ() + 0.4;
                if (in || ++n[0] > 120) {
                    logTilt("cart in the curve at " + c);
                    shot("cart_curve");
                    steps.addFirst(new Step(8, () -> {
                        logTilt("cart after the curve");
                        shot("cart_curve2");
                        server(p -> pushing = false);
                    }));
                    return;
                }
                steps.addFirst(new Step(1, this));
            }
        }));
    }

    private void logTilt(String what) {
        LOGGER.info("[smoketest] M50 {}: largest water tilt {} {}", what, String.format(java.util.Locale.ROOT, "%.3f", TravelWater.maxTilt()), TravelWater.debug());
    }

    /** A tank on an iron plate on the ground, assembled into a Sable sub-level like the physics assembler does. */
    private void airship(ServerLevel level) {
        BlockPos ground = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, shipPos);
        shipPos = ground.above();
        ServerPlayer p = level.getServer().getPlayerList().getPlayers().getFirst();
        for (int x = -1; x <= 2; x++) for (int z = -1; z <= 2; z++) level.setBlock(ground.offset(x, 0, z), Blocks.IRON_BLOCK.defaultBlockState(), 3);
        TankBlockEntity c = tank(level, p, shipPos, 2, 2, 2);
        fish(level, c, 2);
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
            LOGGER.info("[smoketest] M50 airship assembled from {} blocks", ((java.util.Set<?>) blocks).size());
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.error("[smoketest] M50 airship (Sable)", e);
        }
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
        if (bearing != null && !Float.isNaN(angle)) {
            angle += speed;
            float a = angle;
            server(p -> hold(a));
        }
        if (pushing) {
            server(p -> {
                if (cart == null || !cart.isAlive()) return;
                Vec3 v = cart.getDeltaMovement();
                double len = v.horizontalDistance();
                Vec3 dir = len < 1e-3 ? new Vec3(1, 0, 0) : new Vec3(v.x / len, 0, v.z / len);
                cart.setDeltaMovement(dir.x * 0.4, v.y, dir.z * 0.4);
            });
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
        mc.getToasts().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m50_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
