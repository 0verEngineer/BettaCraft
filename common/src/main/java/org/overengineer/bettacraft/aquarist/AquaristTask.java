package org.overengineer.bettacraft.aquarist;

import javax.annotation.Nullable;

/**
 * M51 (question C1): the tasks of a hired aquarist, each ticked on or off in his menu with its own level 1 to 3 (0 = off).
 * What a level means is in {@link AquaristWork} and on the handbook page.
 */
public enum AquaristTask {
    /** Feeds from the chest at his workplace: level = feedings per day (1, 2, 3). */
    FEED("feed", 2),
    /** Changes water from the reservoir, pipe or water source at his workplace: level = share 15, 25, 35 % when it is due. */
    WATER("water", 2),
    /** Scrapes the algae off the glass: level = from 50, 30, 15 % algae. */
    ALGAE("algae", 2),
    /** Thins out floating plants and trims grown aquarium plants; the cuttings go into the chest: level = how early. */
    PLANTS("plants", 2),
    /** Moves young animals into the rearing tank shown to him: level = 2, 4, 8 per trip. */
    YOUNG("young", 2),
    /** Reports warnings to the employer (chat, JourneyMap waypoint): level 1 serious ones, 2 also care, 3 also breeding. */
    WARN("warn", 2);

    public static final AquaristTask[] ALL = values();
    public static final int MAX_LEVEL = 3;

    private final String id;
    private final int defaultLevel;

    AquaristTask(String id, int defaultLevel) {
        this.id = id;
        this.defaultLevel = defaultLevel;
    }

    public String id() {
        return id;
    }

    public int defaultLevel() {
        return defaultLevel;
    }

    public String translationKey() {
        return "gui.bettacraft.aquarist.task." + id;
    }

    @Nullable
    public static AquaristTask byId(String id) {
        for (AquaristTask t : ALL) if (t.id.equals(id)) return t;
        return null;
    }
}
