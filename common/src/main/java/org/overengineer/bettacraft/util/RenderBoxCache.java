package org.overengineer.bettacraft.util;

import java.util.function.Supplier;
import net.minecraft.world.phys.AABB;

/**
 * M66.5: the render box of a block entity, worked out at most once a second. NeoForge asks every block entity renderer for
 * its box every frame (and again in the Iris shadow pass); for cabinets and external filters the box reaches the tank, and
 * finding the tank's controller every frame for every cabinet of the world cost up to 15 % of the render thread.
 */
public final class RenderBoxCache {
    /** How long (game ticks) a box is kept; a tank that changed its size shows its hose box at most this late. */
    static final long TTL = 20;
    private AABB box;
    private long at = Long.MIN_VALUE;

    /** The cached box, or a new one from {@code compute} when it is older than a second (or the game time went back). */
    public AABB get(long gameTime, Supplier<AABB> compute) {
        if (box == null || gameTime - at >= TTL || gameTime < at) {
            box = compute.get();
            at = gameTime;
        }
        return box;
    }

    public void invalidate() {
        box = null;
    }
}
