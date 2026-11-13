package org.overengineer.bettacraft.tank;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;

/**
 * M16: vanilla special cases of animals of others in our tanks. Their hunger, health and breeding stay their own business;
 * our care loop only counts them for the stocking.
 * <ul>
 *     <li>A tadpole that grows up does not become a frog in the glass (a frog cannot live under water for good): with
 *     {@code inhabitants.tadpoles = LEAVE_AS_FROG} the new frog hops out in front of the tank (log entry), with
 *     {@code STAY_TADPOLE} the tadpole stays a tadpole while it lives in a tank.</li>
 *     <li>Axolotls are predators ({@code #bettacraft:predators}): they hunt small fish and fry, like their vanilla AI.</li>
 * </ul>
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class ForeignAnimals {
    /** Frogs that just grew from a tadpole in a tank and where they land. */
    private static final Map<UUID, Vec3> FROGS_OUT = new HashMap<>();

    private ForeignAnimals() {}

    @SubscribeEvent
    public static void onConvertPre(LivingConversionEvent.Pre event) {
        if (!(event.getEntity() instanceof Tadpole tadpole) || !(tadpole.level() instanceof ServerLevel)) return;
        if (!TankInhabitants.hasTankRef(tadpole)) return;
        if (ServerConfig.getOr(ServerConfig.INHABITANTS_TADPOLES, ServerConfig.Tadpoles.LEAVE_AS_FROG) != ServerConfig.Tadpoles.STAY_TADPOLE) return;
        event.setCanceled(true);
        // back to a young tadpole, so this is not asked again every tick (Tadpole#setAge is private; the bucket data sets it)
        CompoundTag age = new CompoundTag();
        age.putInt("Age", 0);
        tadpole.loadFromBucketTag(age);
    }

    @SubscribeEvent
    public static void onConvertPost(LivingConversionEvent.Post event) {
        if (!(event.getEntity() instanceof Tadpole tadpole) || !(tadpole.level() instanceof ServerLevel level)) return;
        TankBlockEntity c = TankInhabitants.resolveController(level, tadpole);
        if (c == null) return;
        Entity frog = event.getOutcome();
        TankInhabitants.release(frog);
        FROGS_OUT.put(frog.getUUID(), inFront(level, c.getTank()));
        c.getTank().inhabitants().remove(tadpole.getUUID());
        TankCare.log(level, c, "log.bettacraft.tadpole_left", TankCare.nameArg(tadpole));
        c.markTankChanged();
    }

    /** Entities that join the level while an animal for a tank is created (see TankInhabitants#createFromBucket). */
    @Nullable
    private static java.util.List<Entity> capture;

    static void capture(@Nullable java.util.List<Entity> into) {
        capture = into;
    }

    /** The frog is added after the conversion event and placed where the tadpole was: move it out of the tank. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (capture != null) capture.add(event.getEntity());
        if (FROGS_OUT.isEmpty()) return;
        Vec3 to = FROGS_OUT.remove(event.getEntity().getUUID());
        if (to != null) event.getEntity().moveTo(to.x, to.y, to.z, event.getEntity().getYRot(), 0);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!FROGS_OUT.isEmpty()) FROGS_OUT.clear();
    }

    /**
     * A free spot right in front of the tank (its front side, middle, lowest free height from the bottom of the tank), or on
     * top of it if the front is walled in.
     */
    public static Vec3 inFront(ServerLevel level, TankData tank) {
        Direction f = tank.front();
        BlockPos mn = tank.min(), mx = tank.max();
        int x = (mn.getX() + mx.getX()) / 2, z = (mn.getZ() + mx.getZ()) / 2;
        switch (f) {
            case NORTH -> z = mn.getZ() - 1;
            case SOUTH -> z = mx.getZ() + 1;
            case WEST -> x = mn.getX() - 1;
            default -> x = mx.getX() + 1;
        }
        Vec3 spot = free(level, x, z, mn.getY() - 1, mx.getY() + 2);
        return spot != null ? spot : new Vec3((mn.getX() + mx.getX() + 1) / 2.0, mx.getY() + 1, (mn.getZ() + mx.getZ() + 1) / 2.0);
    }

    @Nullable
    private static Vec3 free(ServerLevel level, int x, int z, int fromY, int toY) {
        for (int y = fromY; y <= toY; y++) {
            BlockPos p = new BlockPos(x, y, z);
            if (level.getBlockState(p).getCollisionShape(level, p).isEmpty() && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
                    && !level.getBlockState(p.below()).getCollisionShape(level, p.below()).isEmpty()) {
                return new Vec3(x + 0.5, y, z + 0.5);
            }
        }
        return null;
    }
}
