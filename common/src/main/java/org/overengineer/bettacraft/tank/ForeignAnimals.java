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
public final class ForeignAnimals {
    /** Frogs that just grew from a tadpole in a tank and where they land. */
    static final Map<UUID, Vec3> FROGS_OUT = new HashMap<>();

    static void clear() {
        FROGS_OUT.clear();
    }

    private ForeignAnimals() {}



    /** Entities that join the level while an animal for a tank is created (see TankInhabitants#createFromBucket). */
    @Nullable
    static java.util.List<Entity> capture;

    static void capture(@Nullable java.util.List<Entity> into) {
        capture = into;
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
