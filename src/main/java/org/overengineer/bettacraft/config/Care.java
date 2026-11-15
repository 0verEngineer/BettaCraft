package org.overengineer.bettacraft.config;

/**
 * M33 part c: the care mode ({@code care.mode}, {@link ServerConfig.CareMode}) asked by every death path of the animals in
 * tanks and terraria: bettas, tankmates (also the big ones), small animals, frogs, amphibians and reptiles, turtles,
 * dolphins, axolotls and the animals of other mods, mobs in glass. {@link #canDie} guards the deaths of the care (illness,
 * hunger, salt, filter intake) and of other animals (eating, hunting; damage is capped at one heart in
 * {@code TankDamageGuard}); {@link #needs} switches the needs themselves off (decoration tanks).
 */
public final class Care {
    private Care() {}

    public static ServerConfig.CareMode mode() {
        return ServerConfig.getOr(ServerConfig.CARE_MODE, ServerConfig.CareMode.NORMAL);
    }

    /** Whether an animal in a tank can die of its care or of another animal (only NORMAL). */
    public static boolean canDie() {
        return canDie(mode());
    }

    /** Whether the animals have needs at all: hunger, stress, sickness (not in DECOR). */
    public static boolean needs() {
        return needs(mode());
    }

    public static boolean canDie(ServerConfig.CareMode m) {
        return m == null || m == ServerConfig.CareMode.NORMAL;
    }

    public static boolean needs(ServerConfig.CareMode m) {
        return m != ServerConfig.CareMode.DECOR;
    }
}
