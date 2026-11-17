package org.overengineer.bettacraft.entity.betta;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.tank.TankWaterVolume;

/**
 * How far the rendered fish (fins included) reaches from the entity position, per tail shape, for an adult (blocks;
 * from the rest pose of tools/assetgen/model_defs.py at ADULT_RENDER_SCALE, plus a margin for the swimming and flaring
 * animation). The hitbox is much smaller than the fish: keeping only the hitbox inside the tank let tails and fins
 * stick out through the glass at the walls.
 */
public final class BettaExtents {
    private static final double MARGIN = 0.03;
    private static final double NOSE = 0.25, SIDE = 0.136;

    private BettaExtents() {}

    /** Behind the position (caudal fin). */
    static double tail(String shape) {
        return switch (shape) {
            case "veil" -> 0.5;
            case "halfmoon", "doubletail" -> 0.438;
            default -> 0.469;
        };
    }

    /** Above the position (dorsal fin). */
    static double top(String shape) {
        return switch (shape) {
            case "round", "spade", "veil" -> 0.313;
            case "rosetail" -> 0.375;
            default -> 0.344;
        };
    }

    /** Below the position (anal and ventral fins). */
    static double bottom(String shape) {
        return switch (shape) {
            case "rosetail" -> 0.109;
            case "halfmoon", "crowntail" -> 0.093;
            case "doubletail" -> 0.078;
            default -> 0.062;
        };
    }

    /**
     * The shift that brings the outermost points of the fish (nose, tail and the sides of both, the tips of the dorsal
     * and the lower fins, spread wider while flaring) inside {@code water} (glass margin {@code glass}); zero if the whole fish is inside.
     */
    public static Vec3 push(TankWaterVolume water, Vec3 pos, float yawDeg, String shape, float scale, boolean flaring, double glass) {
        double yaw = yawDeg * Mth.DEG_TO_RAD;
        double fx = -Math.sin(yaw), fz = Math.cos(yaw); // facing (Entity#getLookAngle at pitch 0)
        double nose = (NOSE + MARGIN) * scale, tail = (tail(shape) + MARGIN) * scale, side = (SIDE + MARGIN) * scale;
        double top = (top(shape) + MARGIN) * scale, bottom = (bottom(shape) + MARGIN) * scale;
        if (flaring) {
            // flaring spreads the fins (BettaModel: caudal 1.15 longer, dorsal and anal 1.5 higher)
            tail *= 1.15;
            top *= 1.35;
            bottom *= 1.5;
        }
        // per axis the largest push in each direction; if the fish is too big for the water both add up (centred)
        double[] up = new double[3], down = new double[3];
        double[][] pts = {
                {nose, side, 0}, {nose, -side, 0}, {-tail, side, 0}, {-tail, -side, 0},
                {0, 0, top}, {-tail * 0.5, 0, top}, {0, 0, -bottom}, {-tail * 0.5, 0, -bottom},
        };
        for (double[] q : pts) {
            // along the fish, across it (to the left), up
            Vec3 v = new Vec3(pos.x + fx * q[0] + fz * q[1], pos.y + q[2], pos.z + fz * q[0] - fx * q[1]);
            Vec3 c = water.clamp(v, glass, glass, 0.02);
            double[] d = {c.x - v.x, c.y - v.y, c.z - v.z};
            for (int i = 0; i < 3; i++) {
                up[i] = Math.max(up[i], d[i]);
                down[i] = Math.min(down[i], d[i]);
            }
        }
        return new Vec3(up[0] + down[0], up[1] + down[1], up[2] + down[2]);
    }
}
