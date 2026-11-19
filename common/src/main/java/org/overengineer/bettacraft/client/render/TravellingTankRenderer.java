package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.overengineer.bettacraft.client.interior.IrisCompat;
import org.overengineer.bettacraft.client.interior.MiniatureRenderer;
import org.overengineer.bettacraft.compat.create.SableCompat;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankMoves;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M32 part b (B2): a tank on a Create contraption or a Create Aeronautics airship. Create and Sable draw the tank's block
 * entity there with their own pose; from that pose this draws what the travelling tank shows: the miniature of its
 * interior ({@link MiniatureRenderer#queueTravelling}, drawn in the miniature passes like a standing one) and its packed
 * animals as a data animation: client-only copies of the entities (never added to the level) on a slow swimming loop
 * around the place they had in the tank, with the real entity renderers (bettas with their skins, tankmates, foreign fish).
 */
public final class TravellingTankRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/travelling");
    /** At most this many animals are drawn per travelling tank. */
    private static final int MAX_ANIMALS = 48;
    private static final double ANIMAL_DISTANCE = 64;

    /** Region of a travelling tank drawn lately: when (game time) and the position its interior request names. */
    public record Seen(long time, BlockPos requestPos) {}

    public static final Int2ObjectOpenHashMap<Seen> SEEN = new Int2ObjectOpenHashMap<>();
    private static final it.unimi.dsi.fastutil.ints.IntOpenHashSet LOGGED = new it.unimi.dsi.fastutil.ints.IntOpenHashSet();

    /** M50: {@code lively} 0..1 from the animal's state on the road (hungry or sick animals swim slower and lower). */
    private record Animal(Entity entity, double hx, double hy, double hz, float phase, float speed, float radius, float lively, boolean sick) {}

    private record Cache(ListTag source, List<Animal> animals) {}

    private static boolean failedOnce;

    private TravellingTankRenderer() {}

    /**
     * M50: whether this tank block (controller or member) is in a Sable sub-level (on an airship); checked once a second,
     * on the main thread (its model data leaves the water out when the travelling water is drawn sloshing).
     */
    public static boolean travellingBlock(TankBlockEntity be) {
        Level level = be.getLevel();
        if (level == null || !level.isClientSide() || !SableCompat.loaded()) return false;
        long now = level.getGameTime();
        if (be.travelCheckAt == Long.MIN_VALUE || now - be.travelCheckAt >= 20 || now < be.travelCheckAt) {
            be.travelCheckAt = now;
            boolean was = be.travelCheck;
            be.travelCheck = SableCompat.inSubLevel(level, be.getBlockPos());
            if (was != be.travelCheck) org.overengineer.bettacraft.platform.Platform.get().requestModelDataUpdate(be);
        }
        return be.travelCheck;
    }

    /** Whether this controller is drawn travelling: Create's copy on a contraption, or a tank in a Sable sub-level. */
    public static boolean travelling(TankBlockEntity be) {
        if (be.contraptionView) return true;
        Level level = be.getLevel();
        if (level == null || !SableCompat.loaded()) return false;
        long now = level.getGameTime();
        if (be.travelCheckAt == Long.MIN_VALUE || now - be.travelCheckAt >= 20 || now < be.travelCheckAt) {
            be.travelCheckAt = now;
            be.travelCheck = SableCompat.inSubLevel(level, be.getBlockPos());
        }
        return be.travelCheck;
    }

    /** From the tank's block entity renderer (pose at the controller block, on the contraption / ship). */
    public static void render(TankBlockEntity be, TankData t, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!travelling(be)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (TravelWater.drawsWater(be)) {
            // M50: the water sloshes; its blocks leave their water out once they know they travel (re-meshed once)
            if (!be.contraptionView && !be.sloshMeshed) {
                be.sloshMeshed = true;
                for (long l : t.members()) {
                    BlockPos p = BlockPos.of(l);
                    if (mc.level.getBlockEntity(p) instanceof TankBlockEntity m) {
                        travellingBlock(m);
                        org.overengineer.bettacraft.platform.Platform.get().requestModelDataUpdate(m);
                        mc.levelRenderer.setBlocksDirty(p.getX(), p.getY(), p.getZ(), p.getX(), p.getY(), p.getZ());
                    }
                }
            }
            TravelWater.render(be, t, partialTick, pose, buffers, light);
        }
        if (t.regionId() >= 0 && !IrisCompat.renderingShadowPass()) {
            // Create: at once (its contraption blocks, the water too, are drawn before the miniature passes); Sable: queued
            if (be.contraptionView && !IrisCompat.shaderPackInUse()) MiniatureRenderer.drawTravellingNow(t, be.getBlockPos(), new Matrix4f(pose.last().pose()));
            else MiniatureRenderer.queueTravelling(t, be.getBlockPos(), new Matrix4f(pose.last().pose()));
            if (SEEN.put(t.regionId(), new Seen(mc.level.getGameTime(), be.getBlockPos())) == null && LOGGED.add(t.regionId())) {
                LOGGER.info("Drawing a travelling tank (region {}, {} packed animals, {})", t.regionId(), be.stowed().size(),
                        be.contraptionView ? "Create contraption" : "Sable sub-level");
            }
        }
        if (be.stowed().isEmpty()) return;
        Vector3f eye = pose.last().pose().transformPosition(new Vector3f());
        if (eye.lengthSquared() > ANIMAL_DISTANCE * ANIMAL_DISTANCE) return;
        drawAnimals(be, t, partialTick, pose, buffers, light, mc);
    }

    private static List<Animal> animals(TankBlockEntity be, Minecraft mc) {
        ListTag src = be.stowed();
        if (be.travelCache instanceof Cache c && c.source() == src) return c.animals();
        List<Animal> out = new ArrayList<>();
        for (int i = 0; i < src.size() && out.size() < MAX_ANIMALS; i++) {
            CompoundTag tag = src.getCompoundOrEmpty(i);
            Entity e;
            try {
                e = EntityType.create(org.overengineer.bettacraft.util.NbtCompat.input(mc.level.registryAccess(), tag), mc.level,
                        net.minecraft.world.entity.EntitySpawnReason.LOAD).orElse(null);
            } catch (RuntimeException ex) {
                e = null;
            }
            if (e == null) continue;
            ListTag place = tag.getListOrEmpty(TankMoves.PLACE_KEY);
            double hx = place.size() == 3 ? place.getDoubleOr(0, 0d) : 0.5, hy = place.size() == 3 ? place.getDoubleOr(1, 0d) : 0.4,
                    hz = place.size() == 3 ? place.getDoubleOr(2, 0d) : 0.5;
            RandomSource r = RandomSource.create(e.getUUID().getLeastSignificantBits() ^ i);
            ((org.overengineer.bettacraft.mixin.EntityAccessor) e).bettacraft$setWasTouchingWater(true); // swimming, not flopping on land
            float sat = org.overengineer.bettacraft.tank.TravelCare.satiation(tag);
            boolean sick = org.overengineer.bettacraft.tank.TravelCare.sick(tag);
            float lively = sick ? 0.3F : 0.45F + 0.55F * Mth.clamp(sat * 2F, 0F, 1F);
            out.add(new Animal(e, hx, hy, hz, r.nextFloat() * Mth.TWO_PI, 0.6F + r.nextFloat() * 0.6F, 0.35F + r.nextFloat() * 0.5F, lively, sick));
        }
        be.travelCache = new Cache(src, out);
        return out;
    }

    private static void drawAnimals(TankBlockEntity be, TankData t, float partialTick, PoseStack pose, MultiBufferSource buffers, int light,
                                    Minecraft mc) {
        List<Animal> list = animals(be, mc);
        if (list.isEmpty()) return;
        BlockPos o = be.getBlockPos(), mn = t.min(), mx = t.max();
        long gt = mc.level.getGameTime();
        float time = gt + partialTick;
        double x0 = mn.getX() - o.getX() + 0.2, x1 = mx.getX() + 1 - o.getX() - 0.2;
        double z0 = mn.getZ() - o.getZ() + 0.2, z1 = mx.getZ() + 1 - o.getZ() - 0.2;
        double y0 = mn.getY() - o.getY() + 0.15;
        double y1 = Math.max(y0, Math.min(mx.getY() + 1 - o.getY(), t.waterSurfaceY() - o.getY()) - 0.2);
        for (Animal a : list) {
            Entity e = a.entity();
            double cx = mn.getX() - o.getX() + a.hx(), cy = mn.getY() - o.getY() + a.hy(), cz = mn.getZ() - o.getZ() + a.hz();
            double rx = Math.min(a.radius(), Math.max(0, (x1 - x0) / 2)), rz = Math.min(a.radius(), Math.max(0, (z1 - z0) / 2));
            cx = Mth.clamp(cx, x0 + rx, x1 - rx);
            cz = Mth.clamp(cz, z0 + rz, z1 - rz);
            cy = Mth.clamp(a.sick() ? y0 + (cy - y0) * 0.3 : cy, y0, y1); // M50: a sick animal stays near the floor
            double th = time * 0.035 * a.speed() * a.lively() + a.phase();
            double px = cx + rx * Math.cos(th), pz = cz + rz * Math.sin(th);
            double py = Mth.clamp(cy + 0.08 * Math.sin(th * 0.5 + a.phase()), y0, y1);
            double dx = -rx * Math.sin(th), dz = rz * Math.cos(th);
            if (!t.contains(BlockPos.containing(px + o.getX(), py + o.getY(), pz + o.getZ()))) {
                px = cx;
                pz = cz;
            }
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            e.tickCount = (int) gt;
            e.setYRot(yaw);
            e.yRotO = yaw;
            if (e instanceof LivingEntity le) {
                le.yBodyRot = le.yBodyRotO = yaw;
                le.yHeadRot = le.yHeadRotO = yaw;
            }
            pose.pushPose();
            try {
                // M52b: through the 1.21.9 render states, into the submit queue of the tank's block entity renderer
                var dispatcher = mc.getEntityRenderDispatcher();
                var state = dispatcher.extractEntity(e, partialTick);
                state.lightCoords = light;
                state.shadowPieces.clear();
                dispatcher.submit(state, mc.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState, px, py, pz, pose, LegacyBlockEntityRenderer.collector());
            } catch (RuntimeException ex) {
                if (!failedOnce) {
                    failedOnce = true;
                    LOGGER.warn("Could not draw a travelling {}", e.getType().getDescriptionId(), ex);
                }
            } finally {
                pose.popPose();
            }
        }
    }
}
