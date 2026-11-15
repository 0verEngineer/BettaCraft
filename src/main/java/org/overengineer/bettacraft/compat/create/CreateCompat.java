package org.overengineer.bettacraft.compat.create;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankMoveMap;
import org.overengineer.bettacraft.tank.TankMoves;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M32 part b: our tanks on Create contraptions (pistons, bearings, pulleys, gantries, carts, trains), through Create's
 * public registries only, by reflection (no compile dependency):
 * <ul>
 *     <li>{@code BlockMovementChecks}: a tank may move ({@code features.createContraptions}, nobody in its interior, no
 *     eggs or young that cannot travel), and all blocks of a tank are attached to each other, so a tank always moves as a
 *     whole.</li>
 *     <li>{@code MovementBehaviour} of the tank block: at the start of a move the controller packs the animals into the
 *     captured block data ({@link TankMoves#pack}) and into the update tag the clients draw from; the live animals go at
 *     the end of the tick ({@link TankMoves#lifted}). Create draws the tank's block entity on the contraption; our renderer
 *     adds the miniature and the packed animals there ({@code client/render/TravellingTankRenderer}).</li>
 *     <li>{@code MovedBlockTransformerRegistries}: when a contraption puts the tank down, its rotation is handed to
 *     {@link TankMoves} (which then moves the tank data, turns the interior and releases the animals).</li>
 * </ul>
 */
public final class CreateCompat {
    public static final String MOD_ID = "create";
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/create");
    private static boolean registered;

    private static Field ctxWorld, ctxLocalPos, ctxBlockEntityData, ctxContraption;
    private static Field contraptionAnchor, contraptionUpdateTags;
    private static Method transformApplyWithoutOffset;
    private static Object resultSuccess, resultFail, resultPass;
    private static final java.util.Map<UUID, Long> MESSAGED = new java.util.HashMap<>();

    private CreateCompat() {}

    public static boolean loaded() {
        return ModList.get() != null && ModList.get().isLoaded(MOD_ID);
    }

    /** Common setup (main thread). Logs and gives up quietly if Create's API changed. */
    public static void register() {
        if (registered || !loaded()) return;
        try {
            ClassLoader cl = CreateCompat.class.getClassLoader();
            Class<?> checks = Class.forName("com.simibubi.create.api.contraption.BlockMovementChecks", true, cl);
            Class<?> allowed = Class.forName("com.simibubi.create.api.contraption.BlockMovementChecks$MovementAllowedCheck", true, cl);
            Class<?> attached = Class.forName("com.simibubi.create.api.contraption.BlockMovementChecks$AttachedCheck", true, cl);
            Class<?> result = Class.forName("com.simibubi.create.api.contraption.BlockMovementChecks$CheckResult", true, cl);
            Class<?> behaviour = Class.forName("com.simibubi.create.api.behaviour.movement.MovementBehaviour", true, cl);
            Class<?> registries = Class.forName("com.simibubi.create.api.contraption.transformable.MovedBlockTransformerRegistries", true, cl);
            Class<?> beTransformer = Class.forName("com.simibubi.create.api.contraption.transformable.MovedBlockTransformerRegistries$BlockEntityTransformer", true, cl);
            Class<?> context = Class.forName("com.simibubi.create.content.contraptions.behaviour.MovementContext", true, cl);
            Class<?> contraption = Class.forName("com.simibubi.create.content.contraptions.Contraption", true, cl);
            Class<?> transform = Class.forName("com.simibubi.create.content.contraptions.StructureTransform", true, cl);
            resultSuccess = enumConstant(result, "SUCCESS");
            resultFail = enumConstant(result, "FAIL");
            resultPass = enumConstant(result, "PASS");
            ctxWorld = context.getField("world");
            ctxLocalPos = context.getField("localPos");
            ctxBlockEntityData = context.getField("blockEntityData");
            ctxContraption = context.getField("contraption");
            contraptionAnchor = contraption.getField("anchor");
            contraptionUpdateTags = contraption.getDeclaredField("updateTags");
            contraptionUpdateTags.setAccessible(true);
            transformApplyWithoutOffset = transform.getMethod("applyWithoutOffset", BlockPos.class);

            Object allowedCheck = proxy(allowed, (p, m, a) -> m.getName().equals("isMovementAllowed")
                    ? movementAllowed((BlockState) a[0], (Level) a[1], (BlockPos) a[2]) : objectMethod(p, m, a));
            Object attachedCheck = proxy(attached, (p, m, a) -> m.getName().equals("isBlockAttachedTowards")
                    ? attachedTowards((BlockState) a[0], (Level) a[1], (BlockPos) a[2], (Direction) a[3]) : objectMethod(p, m, a));
            checks.getMethod("registerMovementAllowedCheck", allowed).invoke(null, allowedCheck);
            checks.getMethod("registerAttachedCheck", attached).invoke(null, attachedCheck);

            Object movement = proxy(behaviour, CreateCompat::movementBehaviour);
            Object registry = behaviour.getField("REGISTRY").get(null);
            registry.getClass().getMethod("register", Object.class, Object.class).invoke(registry, ModBlocks.TANK.get(), movement);

            Object transformer = proxy(beTransformer, (p, m, a) -> {
                if (!m.getName().equals("transform")) return objectMethod(p, m, a);
                transformed((BlockEntity) a[0], a[1]);
                return null;
            });
            Object beRegistry = registries.getField("BLOCK_ENTITY_TRANSFORMERS").get(null);
            beRegistry.getClass().getMethod("register", Object.class, Object.class).invoke(beRegistry, ModBlockEntities.TANK.get(), transformer);
            registered = true;
            LOGGER.info("Create found: tanks travel on contraptions (features.createContraptions)");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.error("Create found, but its contraption API could not be reached: tanks stay unmovable", e);
        }
    }

    public static boolean registered() {
        return registered;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumConstant(Class<?> c, String name) {
        return Enum.valueOf((Class) c, name);
    }

    private static Object proxy(Class<?> iface, InvocationHandler h) {
        return Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, h);
    }

    @Nullable
    private static Object objectMethod(Object proxy, Method m, Object[] a) throws Throwable {
        return switch (m.getName()) {
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == a[0];
            case "toString" -> "Bettacraft tank (" + m.getDeclaringClass().getSimpleName() + ")";
            default -> m.isDefault() ? InvocationHandler.invokeDefault(proxy, m, a) : null;
        };
    }

    // ------------------------------------------------------------------ checks

    private static Object movementAllowed(BlockState state, Level level, BlockPos pos) {
        if (!(state.getBlock() instanceof TankBlock)) return resultPass;
        if (!CommonConfig.createContraptions()) return resultFail;
        Optional<Component> why = cannotTravel(level, pos);
        if (why.isEmpty()) return resultSuccess;
        tell(level, pos, why.get());
        return resultFail;
    }

    /** Why the tank at {@code pos} cannot travel now (players inside, eggs or young that would not survive it), if so. */
    public static Optional<Component> cannotTravel(Level level, BlockPos pos) {
        TankBlockEntity c = TankMultiblock.getController(level, pos);
        if (c == null) return Optional.empty();
        TankData t = c.getTank();
        if (level instanceof ServerLevel sl && !org.overengineer.bettacraft.tank.interior.InteriorManager.playersInside(sl.getServer(), t).isEmpty()) {
            return Optional.of(Component.translatable("message.bettacraft.travel.blocked.interior"));
        }
        if (t.nest() != null && t.nest().hasEggs()) return Optional.of(Component.translatable("message.bettacraft.travel.blocked.nest"));
        boolean young = t.clutches().stream().anyMatch(k -> k.hasYoung());
        if (young || t.breedingBox() || !t.herpClutches().isEmpty()) {
            return Optional.of(Component.translatable("message.bettacraft.travel.blocked.eggs"));
        }
        return Optional.empty();
    }

    private static void tell(Level level, BlockPos pos, Component why) {
        if (!(level instanceof ServerLevel sl)) return;
        TankBlockEntity c = TankMultiblock.getController(level, pos);
        if (c == null) return;
        long now = sl.getGameTime();
        Long last = MESSAGED.get(c.getTank().id());
        if (last != null && now - last < 60 && now >= last) return;
        MESSAGED.put(c.getTank().id(), now);
        Component msg = Component.translatable("message.bettacraft.travel.blocked", why).withStyle(net.minecraft.ChatFormatting.GOLD);
        for (ServerPlayer p : sl.players()) if (p.blockPosition().distSqr(pos) < 24 * 24) p.displayClientMessage(msg, true);
    }

    private static Object attachedTowards(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (!(state.getBlock() instanceof TankBlock) || !CommonConfig.createContraptions()) return resultPass;
        TankBlockEntity a = TankMultiblock.getBE(level, pos), b = TankMultiblock.getBE(level, pos.relative(direction));
        if (a == null || b == null || a.getControllerPos() == null) return resultPass;
        return a.getControllerPos().equals(b.getControllerPos()) ? resultSuccess : resultPass;
    }

    // ------------------------------------------------------------------ movement behaviour

    @Nullable
    private static Object movementBehaviour(Object proxy, Method m, Object[] a) throws Throwable {
        switch (m.getName()) {
            case "startMoving" -> {
                startMoving(a[0]);
                return null;
            }
            case "canBeDisabledVia" -> {
                return null; // contraption controls do not switch a tank off
            }
            case "renderInContraption" -> {
                return null; // drawn by the tank's own block entity renderer, which Create renders on the contraption
            }
            default -> {
                return objectMethod(proxy, m, a);
            }
        }
    }

    /** Server, the start of a move (the blocks are captured, still in the world): pack the animals of a controller. */
    private static void startMoving(Object context) {
        try {
            if (!(ctxWorld.get(context) instanceof ServerLevel level)) return;
            CompoundTag nbt = (CompoundTag) ctxBlockEntityData.get(context);
            if (nbt == null) return;
            Object contraption = ctxContraption.get(context);
            BlockPos local = (BlockPos) ctxLocalPos.get(context);
            BlockPos abs = ((BlockPos) contraptionAnchor.get(contraption)).offset(local);
            int[] c = nbt.getIntArray("controller");
            BlockPos cp = c.length == 3 ? new BlockPos(c[0], c[1], c[2]) : null;
            TankBlockEntity here = TankMultiblock.getController(level, abs);
            if (nbt.contains("tank", Tag.TAG_COMPOUND) && abs.equals(cp)) {
                TankData t = TankData.load(nbt.getCompound("tank"));
                List<Entity> packed = new ArrayList<>();
                ListTag list = TankMoves.pack(level, t, packed);
                ListTag stowed = nbt.getList("stowed", Tag.TAG_COMPOUND);
                stowed.addAll(list);
                nbt.put("stowed", stowed);
                @SuppressWarnings("unchecked")
                Map<BlockPos, CompoundTag> updateTags = (Map<BlockPos, CompoundTag>) contraptionUpdateTags.get(contraption);
                CompoundTag update = updateTags.get(local);
                if (update != null) update.put("stowed", stowed.copy());
                List<BlockPos> members = new ArrayList<>();
                for (long l : t.members()) members.add(BlockPos.of(l));
                TankMoves.lifted(level, abs, cp, true, t.id(), members, packed);
            } else if (cp != null && here != null) {
                TankMoves.lifted(level, abs, cp, false, here.getTank().id(), List.of(), List.of());
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.error("Could not pack a tank for a contraption", e);
        }
    }

    /** Create put a tank block down: hand the contraption's rotation to {@link TankMoves}. */
    private static void transformed(BlockEntity be, Object transform) {
        if (!(be instanceof TankBlockEntity t) || be.getLevel() == null || be.getLevel().isClientSide) return;
        try {
            BlockPos o = (BlockPos) transformApplyWithoutOffset.invoke(transform, BlockPos.ZERO);
            BlockPos x = ((BlockPos) transformApplyWithoutOffset.invoke(transform, new BlockPos(1, 0, 0))).subtract(o);
            BlockPos y = ((BlockPos) transformApplyWithoutOffset.invoke(transform, new BlockPos(0, 1, 0))).subtract(o);
            BlockPos z = ((BlockPos) transformApplyWithoutOffset.invoke(transform, new BlockPos(0, 0, 1))).subtract(o);
            t.markMoved(TankMoveMap.fromColumns(x, y, z));
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.error("Could not read the rotation of a contraption", e);
            t.markMoved(null);
        }
    }
}
