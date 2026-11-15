package org.overengineer.bettacraft.terrarium;

import net.minecraft.server.level.ServerLevel;
import org.overengineer.bettacraft.food.BettaFood;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/**
 * M31: an entity of ours that lives in a terrarium or paludarium (the musk turtle; the turtles, frogs, geckos and snakes of
 * the later parts). {@link TerrariumCare} hands it the assessment of its needs every care step; {@link TerrariumAnimals}
 * knows its type (register it there too, so the tank can judge it before the animal exists). The base entity
 * {@link TerrariumAnimalEntity} implements all of this; another entity can implement it on its own.
 */
public interface TerrariumAnimal {
    /** What it needs (by its species; may depend on its age). */
    TerrariumNeeds terrariumNeeds();

    /** One care step in its tank with the assessment of its needs ({@code days} since the last step). */
    void onTerrariumCare(ServerLevel level, TankBlockEntity tank, TerrariumNeeds.Assessment assessment, double days);

    /** Whether it eats this food (fed by hand or by the feeder like the fish). */
    default boolean eatsFood(BettaFood.Kind kind) {
        return false;
    }

    /** Days since its last meal (the hungriest is fed first). */
    default double daysSinceFed() {
        return 0;
    }

    /** It ate. */
    default void feed() {}

    /** How much it counts for the stocking of the tank (an adult betta = 1). */
    default double stockWeight() {
        return 1.0;
    }
}
