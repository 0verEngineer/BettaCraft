package org.overengineer.bettacraft.entity.betta;

import java.util.Locale;
import org.overengineer.bettacraft.config.ServerConfig;

/**
 * The six life stages (PLAN 5.3). Eggs and larvae are data in the bubble nest; from fry on the fish is a real entity,
 * rendered and sized with {@link #scale()}. Only adults count for the stocking limits.
 */
public enum LifeStage {
    EGG(0.06F, 0.5),
    LARVA(0.12F, 0.5),
    FRY(0.25F, 2.0),
    JUVENILE(0.5F, 2.5),
    SUBADULT(0.75F, 3.5),
    ADULT(1.0F, 0);

    private final float scale;
    private final double defaultDays;

    LifeStage(float scale, double defaultDays) {
        this.scale = scale;
        this.defaultDays = defaultDays;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public float scale() {
        return scale;
    }

    public String translationKey() {
        return "stage.bettacraft." + id();
    }

    /** Duration in in-game days (config {@code stages.*Days}); adults: infinite. */
    public double days() {
        return switch (this) {
            case EGG -> ServerConfig.getOr(ServerConfig.STAGE_EGG_DAYS, defaultDays);
            case LARVA -> ServerConfig.getOr(ServerConfig.STAGE_LARVA_DAYS, defaultDays);
            case FRY -> ServerConfig.getOr(ServerConfig.STAGE_FRY_DAYS, defaultDays);
            case JUVENILE -> ServerConfig.getOr(ServerConfig.STAGE_JUVENILE_DAYS, defaultDays);
            case SUBADULT -> ServerConfig.getOr(ServerConfig.STAGE_SUBADULT_DAYS, defaultDays);
            case ADULT -> Double.POSITIVE_INFINITY;
        };
    }

    public double defaultDays() {
        return defaultDays;
    }

    public LifeStage next() {
        return this == ADULT ? ADULT : values()[ordinal() + 1];
    }

    public boolean isEntity() {
        return ordinal() >= FRY.ordinal();
    }

    /** Unknown ids count as adult (data of a newer version stays usable). */
    public static LifeStage byId(String id) {
        for (LifeStage s : values()) if (s.id().equals(id)) return s;
        return ADULT;
    }
}
