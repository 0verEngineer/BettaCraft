package org.overengineer.bettacraft.tank;

/**
 * M30: the size rule of the big animals and the interior scale of big tanks, as plain logic (unit tested).
 * <p>
 * <b>Size rule.</b> An animal of known length {@code L} (metres) goes into a tank whose longer horizontal side is at least
 * {@code longSide * L} blocks, the shorter one {@code shortSide * L} and the height {@code height * L} (rounded up), and
 * whose water (full tank) holds {@code water * L³} blocks for it and for every other big animal already in it. With the
 * defaults 12 / 6 / 3 / 100: a 1 m bamboo shark from 12 x 6 x 3 blocks (100 water blocks each), a 2.6 m dolphin from
 * 32 x 16 x 8 (1758 water blocks each, two in 32 x 16 x 8). Small animals (hitbox up to {@code inhabitants.maxHitbox})
 * are not affected and go into any tank as before.
 * <p>
 * <b>Interior scale.</b> A tank whose longest side is longer than {@code scale8Above} blocks gets interior scale 8, longer
 * than {@code scale4Above} scale 4, so the interior stays about 256 blocks long. The scale never grows back: a big tank
 * that is made small again keeps its scale (nothing to lose, the interior only gets finer when it is built new).
 */
public final class BigTankRules {
    /** Blocks per metre of the animal for the three sides, water blocks per cubic metre. */
    public record Factors(double longSide, double shortSide, double height, double water) {
        public static final Factors DEFAULT = new Factors(12, 6, 3, 100);
    }

    /** The smallest tank for one animal: sides in blocks, water blocks for it alone. */
    public record Need(int longSide, int shortSide, int height, int water) {}

    public enum Verdict { OK, TOO_SMALL, TOO_LITTLE_WATER }

    private BigTankRules() {}

    private static int up(double v) {
        return (int) Math.ceil(v - 1e-6);
    }

    /** The water blocks one animal of this length needs. */
    public static double water(double lengthM, Factors f) {
        return f.water() * lengthM * lengthM * lengthM;
    }

    public static Need need(double lengthM, Factors f) {
        return new Need(Math.max(1, up(f.longSide() * lengthM)), Math.max(1, up(f.shortSide() * lengthM)), Math.max(1, up(f.height() * lengthM)),
                up(water(lengthM, f)));
    }

    /**
     * Whether an animal of {@code lengthM} fits a tank with the bounding box {@code sx * sy * sz}, {@code waterBlocks} of
     * water when full, while big animals that need {@code othersWater} water blocks already live in it.
     */
    public static Verdict check(int sx, int sy, int sz, double waterBlocks, double othersWater, double lengthM, Factors f) {
        Need n = need(lengthM, f);
        int lng = Math.max(sx, sz), shrt = Math.min(sx, sz);
        if (lng < n.longSide() || shrt < n.shortSide() || sy < n.height()) return Verdict.TOO_SMALL;
        if (waterBlocks + 1e-6 < othersWater + water(lengthM, f)) return Verdict.TOO_LITTLE_WATER;
        return Verdict.OK;
    }

    /**
     * M35: the part of a tank the size rule measures for an animal. An aquarium animal the water, a land animal of a
     * terrarium the land with the air above it, an animal of both (an alligator) the whole tank.
     */
    public enum Volume { LAND, WATER, ALL;

        /** Whether the room of two animals of these volumes is the same room (they count against each other). */
        public boolean shares(Volume o) {
            return this == ALL || o == ALL || this == o;
        }
    }

    /**
     * M35: the blocks of this volume in a tank of {@code capacityBlocks} (full) with {@code landBlocks} of them above its
     * land: the land, the rest (the water), or all of it. A tank without land: the water is all of it, the land nothing.
     */
    public static double blocks(Volume v, double capacityBlocks, double landBlocks) {
        double land = Math.max(0, Math.min(capacityBlocks, landBlocks));
        return switch (v) {
            case LAND -> land;
            case WATER -> capacityBlocks - land;
            case ALL -> capacityBlocks;
        };
    }

    /**
     * The interior scale of a tank of this bounding box: the current one, or smaller (8, 4) once the longest side is longer
     * than {@code scale8Above} / {@code scale4Above}. Never larger than {@code current}.
     */
    public static int scaleFor(int sx, int sy, int sz, int current, int scale8Above, int scale4Above) {
        int longest = Math.max(sx, Math.max(sy, sz));
        int s = longest > scale4Above ? 4 : longest > scale8Above ? 8 : 16;
        return Math.min(current, s);
    }
}
