package org.overengineer.bettacraft.tank.interior;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.tank.TankData;

/**
 * M21: an entity's way to a goal in a tank along the waypoints of {@link InteriorPaths#swim}. The path is searched again
 * when the goal moves or after a while (the decoration may have changed); straight lines without decoration in the way
 * need no search. Without an interior or with the feature off it steers straight to the goal.
 */
public final class PathFollower {
    private static final int REFRESH = 100;
    @Nullable
    private List<Vec3> path;
    private int index;
    @Nullable
    private Vec3 goal;
    private long at = Long.MIN_VALUE;
    private boolean failed;
    /** Closest the entity came to the current waypoint, and since when it does not get closer (stuck at the glass). */
    private double best = Double.MAX_VALUE;
    private long bestAt;

    public void clear() {
        path = null;
        goal = null;
        failed = false;
    }

    /** Whether the last search found no way to the goal (the caller should pick another one). */
    public boolean failed() {
        return failed;
    }

    /**
     * The point to steer to now on the way from {@code pos} to {@code goal}; {@code reach} is how close a waypoint counts
     * as passed. Returns the goal itself when there is nothing in the way or no interior.
     */
    public Vec3 next(MinecraftServer server, TankData tank, Vec3 pos, Vec3 goal, double topY, double reach, long now) {
        return next(server, tank, pos, goal, topY, reach, now, 0, false);
    }

    /** With the clearance of {@link InteriorPaths#swim(MinecraftServer, TankData, Vec3, Vec3, double, double, boolean)}. */
    public Vec3 next(MinecraftServer server, TankData tank, Vec3 pos, Vec3 goal, double topY, double reach, long now, double margin, boolean inflate) {
        if (this.goal == null || this.goal.distanceToSqr(goal) > 0.01 || now - at > REFRESH) {
            this.goal = goal;
            at = now;
            index = 0;
            failed = false;
            best = Double.MAX_VALUE;
            bestAt = now;
            if (InteriorPaths.straight(server, tank, pos, goal, margin, inflate)) {
                path = null;
            } else {
                path = InteriorPaths.swim(server, tank, pos, goal, topY, margin, inflate);
                failed = path == null;
            }
        }
        if (path == null || path.isEmpty()) return goal;
        int before = index;
        while (index < path.size() - 1 && pos.distanceToSqr(path.get(index)) < reach * reach) index++;
        // a waypoint it cannot reach (the body keeps it from the glass): on to the next one after a while
        double d = pos.distanceTo(path.get(index));
        if (index != before || d < best - 0.01) {
            best = d;
            bestAt = now;
        } else if (now - bestAt > 30 && index < path.size() - 1) {
            // a waypoint it cannot reach (the body keeps it from the glass): on to the next one if that is in sight,
            // otherwise a new search from here
            if (InteriorPaths.straight(server, tank, pos, path.get(index + 1))) index++;
            else this.goal = null;
            best = Double.MAX_VALUE;
            bestAt = now;
        }
        // skip ahead when the next but one waypoint is already in sight
        if (index < path.size() - 1 && now % 10 == 0 && InteriorPaths.straight(server, tank, pos, path.get(index + 1), margin, inflate)) index++;
        return path.get(index);
    }

    /** Waypoints left (tests, debugging). */
    public int remaining() {
        return path == null ? 0 : path.size() - index;
    }
}
