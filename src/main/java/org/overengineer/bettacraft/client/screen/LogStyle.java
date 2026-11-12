package org.overengineer.bettacraft.client.screen;

import java.util.Set;

/** Colour and filter category of tank log entries (by translation key). */
final class LogStyle {
    static final int OTHER = 0, BREEDING = 1, HEALTH = 2, FEEDING = 3;

    private static final Set<String> BREEDING_KEYS = Set.of("nest_started", "nest_built", "nest_abandoned", "nest_collapsed", "spawned",
            "larvae", "fry_free", "fry_eaten", "female_chased", "grown_up");
    private static final Set<String> HEALTH_KEYS = Set.of("sick", "recovered", "died", "killed", "eaten", "fight");
    private static final Set<String> FEEDING_KEYS = Set.of("fed", "feeder_empty");
    private static final Set<String> BAD = Set.of("died", "killed", "eaten", "fry_eaten", "fight");
    private static final Set<String> WARN = Set.of("female_chased", "sick", "feeder_empty", "nest_abandoned", "nest_collapsed");

    private LogStyle() {}

    private static String shortKey(String key) {
        int i = key.lastIndexOf('.');
        return i < 0 ? key : key.substring(i + 1);
    }

    static int category(String key) {
        String k = shortKey(key);
        if (k.startsWith("killed")) return HEALTH;
        if (BREEDING_KEYS.contains(k)) return BREEDING;
        if (HEALTH_KEYS.contains(k)) return HEALTH;
        if (FEEDING_KEYS.contains(k)) return FEEDING;
        return OTHER;
    }

    static int color(String key) {
        String k = shortKey(key);
        if (BAD.contains(k) || k.startsWith("killed")) return 0xFF7A7A;
        if (WARN.contains(k)) return 0xFFC860;
        if ("recovered".equals(k)) return 0x8EE6E6;
        if (BREEDING_KEYS.contains(k)) return 0x96E696;
        if (FEEDING_KEYS.contains(k)) return 0xB8B8B8;
        return 0xE0E0E0;
    }
}
