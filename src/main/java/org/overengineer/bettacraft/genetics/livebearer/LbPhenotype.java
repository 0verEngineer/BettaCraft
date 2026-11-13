package org.overengineer.bettacraft.genetics.livebearer;

/**
 * M21.6: what a guppy or endler shows ({@link LivebearerGenome#express}). {@code body} is one of
 * {@link LbLocus#BODY_COLORS}; {@code x} the expressed X pattern, {@code y} the Y pattern ("none" for a female);
 * {@code endler} the endler share of its ancestry (0 = guppy, 1 = endler, between = hybrid).
 */
public record LbPhenotype(String tail, String tailColor, String tailPattern, String body, String x, String y, boolean female, float endler) {
    public static final float HYBRID_LO = 0.25F, HYBRID_HI = 0.75F;

    public boolean hybrid() {
        return endler > HYBRID_LO && endler < HYBRID_HI;
    }

    public boolean endlerType() {
        return endler >= HYBRID_HI;
    }

    public boolean guppyType() {
        return endler <= HYBRID_LO;
    }

    /** A sex-linked pattern on either chromosome (males show both their X and their Y). */
    public boolean hasPattern(String p) {
        return x.equals(p) || y.equals(p);
    }

    /** Value of a condition key of {@link LivebearerStrain}: tail, tail_color, tail_pattern, body, x, y. */
    public String value(String key) {
        return switch (key) {
            case "tail" -> tail;
            case "tail_color" -> tailColor;
            case "tail_pattern" -> tailPattern;
            case "body" -> body;
            case "x" -> x;
            case "y" -> y;
            default -> "";
        };
    }
}
