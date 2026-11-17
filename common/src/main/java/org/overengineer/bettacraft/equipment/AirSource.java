package org.overengineer.bettacraft.equipment;

/**
 * M29.1: something that drives the air stones and sponge filters of a tank: a free-standing air pump
 * ({@link AirPumpBlockEntity}) or an air pump in the aquarium cabinet under the tank ({@link CabinetBlockEntity}).
 */
public interface AirSource {
    /** Share 0..1 of its power (the regulator of a free-standing pump; a pump in the cabinet runs at full power). */
    double airShare();

    /** Whether it drives anything right now. */
    boolean airRunning();

    /** Server: how many outlets it drives (0 = standing still), set by the tank's care step. */
    void setAirDriven(int n);
}
