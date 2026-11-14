package org.overengineer.bettacraft.sources;

import javax.annotation.Nullable;

/**
 * M22.2: one way to get a thing, for the handbook: what kind of source, the {@link Target} key, where (null = not tied
 * to a place), the chance (per try: a swing of the net, a catch with the rod, a bottle, a block; for the trader the
 * share among the products of the offer; for spawns the spawn weight), how many (min..max), the price in emeralds
 * (trader), a range up to {@code chanceMax} when conditions raise it (plants nearby, hard bottom), and the config switch
 * it needs, and the block it comes from (shears, shovel, block drops).
 */
public record Source(Kind kind, String target, @Nullable Where where, double chance, double chanceMax, int min, int max, int emeralds,
                     boolean moreSometimes, @Nullable String condition, @Nullable String block) {
    public enum Kind {
        /** The aquarium net swung in open water. */
        NET,
        /** Fishing with a rod (instead of the catch). */
        FISHING,
        /** Filling a glass bottle. */
        BOTTLE,
        /** Shears on a block. */
        SHEARS,
        /** A shovel on a block. */
        SHOVEL,
        /** Breaking a block. */
        BLOCK_DROP,
        /** Spawns in the world ({@code chance} = spawn weight). */
        SPAWN,
        /** Wandering trader, among his usual offers. */
        TRADER_USUAL,
        /** Wandering trader, among his rare offers. */
        TRADER_RARE
    }
}
