package org.overengineer.bettacraft.winding;

/**
 * M37: a block entity the crank winds ({@link CrankItem}, {@link Winding#wind}). Its spring stores the work of the
 * winding; the device uses it while it works without FE.
 */
public interface Windable {
    WindSpring windSpring();

    Winding.Device windDevice();

    /**
     * M62: what the crank winds and the windows show: the own spring, or the store it shares with others (the cabinets
     * under a tank, {@code features.power.cabinetSharedSpring}).
     */
    default WindSpring windStore() {
        return windSpring();
    }

    /** After a click of the crank (the food maker turns its crank handle). */
    default void onWound() {}
}
