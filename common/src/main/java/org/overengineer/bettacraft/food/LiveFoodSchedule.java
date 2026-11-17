package org.overengineer.bettacraft.food;

/** When the live food pump doses, apart from its block entity (JUnit tests without Minecraft's block entity classes). */
public final class LiveFoodSchedule {
    private LiveFoodSchedule() {}

    /** Key of the latest dose at or before {@code dayTime} ({@code doses} evenly spread from {@link LiveFoodPumpBlockEntity#FIRST_DOSE}). */
    public static long eventKey(long dayTime, int doses) {
        if (doses <= 0) return 0;
        long t = dayTime - LiveFoodPumpBlockEntity.FIRST_DOSE;
        long day = Math.floorDiv(t, 24000L);
        long tod = Math.floorMod(t, 24000L);
        return day * doses + tod * doses / 24000L;
    }
}
