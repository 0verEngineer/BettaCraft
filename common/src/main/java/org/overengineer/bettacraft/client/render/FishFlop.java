package org.overengineer.bettacraft.client.render;

import net.minecraft.world.entity.Entity;

/**
 * M67.5: whether a fish is drawn lying on its side out of water (like vanilla's fish). Only a fish that lives in the level:
 * an info overlay (The One Probe) draws a copy it made from the fish's data, which is in no level and so never in water;
 * that copy lay on its side in the probe's box. It is drawn swimming upright now.
 */
public final class FishFlop {
    private FishFlop() {}

    public static boolean outOfWater(Entity fish) {
        return !fish.isInWater() && fish.level() != null && fish.level().getEntity(fish.getId()) == fish;
    }
}
