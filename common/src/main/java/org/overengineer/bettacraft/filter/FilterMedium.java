package org.overengineer.bettacraft.filter;

import net.minecraft.util.StringRepresentable;
import org.overengineer.bettacraft.config.ServerConfig;

/**
 * M24: what a filter chamber holds. Sponge (mechanical) and ceramic rings (bacteria) clog and are cleaned in a water
 * cauldron; activated carbon and peat are used up. Peat softens the water later (M25: pH).
 */
public enum FilterMedium implements StringRepresentable {
    NONE(0, false), SPONGE(1.0, true), CERAMIC(0.5, true), CARBON(1.0, false), PEAT(0.5, false);

    /** How fast it clogs / is used up (times filters.dirtPerDay). */
    private final double dirtRate;
    private final boolean cleanable;

    FilterMedium(double dirtRate, boolean cleanable) {
        this.dirtRate = dirtRate;
        this.cleanable = cleanable;
    }

    public double dirtRate() {
        return dirtRate;
    }

    /** Cleaned in a water cauldron (else used up and replaced). */
    public boolean cleanable() {
        return cleanable;
    }

    /** Water blocks a chamber with this medium keeps clean when it is clean. */
    public int blocks() {
        return switch (this) {
            case NONE -> 0;
            case SPONGE -> ServerConfig.getOr(ServerConfig.MEDIA_SPONGE_BLOCKS, 12);
            case CERAMIC -> ServerConfig.getOr(ServerConfig.MEDIA_CERAMIC_BLOCKS, 24);
            case CARBON -> ServerConfig.getOr(ServerConfig.MEDIA_CARBON_BLOCKS, 16);
            case PEAT -> ServerConfig.getOr(ServerConfig.MEDIA_PEAT_BLOCKS, 6);
        };
    }

    /** Pure: what a medium still does at this dirt (0..1): clogged sponges and ceramic a fifth, used-up carbon and peat nothing. */
    public double efficiency(double dirt) {
        if (this == NONE) return 0;
        double d = Math.max(0, Math.min(1, dirt));
        return cleanable ? 1 - 0.8 * d : (d >= 1 ? 0 : 1 - 0.3 * d);
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
