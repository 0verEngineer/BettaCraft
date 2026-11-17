package org.overengineer.bettacraft.config;

/**
 * M33 part c: the ready-made profiles of the common config key {@code preset} ({@link Presets} holds their values).
 * CUSTOM = no preset, every value as in the files.
 */
public enum Preset {
    CUSTOM, SIMPLE, REALISTIC, DECOR;

    /** The lower-case name of the command and the handbook. */
    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    @javax.annotation.Nullable
    public static Preset byId(String id) {
        for (Preset p : values()) if (p.id().equalsIgnoreCase(id) || p.name().equalsIgnoreCase(id)) return p;
        return null;
    }
}
