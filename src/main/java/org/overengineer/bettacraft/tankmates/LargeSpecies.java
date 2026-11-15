package org.overengineer.bettacraft.tankmates;

import org.overengineer.bettacraft.food.FoodDiet;

/**
 * M30: the extra data of a big tankmate species (giant catfish, sharks, rays). A big species is an ordinary
 * {@link TankmateSpecies} entry (one entity type, the tankmate renderer, care, feeding, handbook and sources) with
 * {@code .large(LargeSpecies.of(...))} in its definition; that one call makes it big everywhere:
 * <ul>
 *     <li><b>Size rule</b> ({@link org.overengineer.bettacraft.tank.BigTankRules}): it goes into a tank by its real
 *     {@link #lengthM} (12 x 6 x 3 blocks per metre, 100 water blocks per cubic metre by default).</li>
 *     <li><b>Body</b>: drawn {@link #BLOCKS_PER_METRE} blocks per metre long (a 1 m pangasius is two blocks long), a hitbox
 *     from that length, a stocking weight from its water need.</li>
 *     <li><b>Movement</b>: wide curves and slow turns ({@link #turnRate}, {@code LargeMoveControl}), long cruises ahead along
 *     the M21 paths around the decoration ({@code TankmateBehaviour#large}).</li>
 *     <li><b>Food</b>: its {@link #diet}; with {@link #eatsSmallFish} it eats tankmates up to {@link #preyLength} blocks,
 *     young bettas and shrimp (the M29 predation, also bettas).</li>
 *     <li><b>Catching</b>: no bucket and no fish bag (they refuse it); the large net, the transport tub and the steel leader.
 *     It is never in the small net's table; its {@code native_} waters give its wild spawns ({@code spawn.bigAnimalWeight},
 *     {@code bigAnimalRules.*}) and the catches of the large net and the steel leader.</li>
 * </ul>
 */
public final class LargeSpecies {
    /** Big animals are drawn at half a metre per block (the small fish at about 12 cm per block). */
    public static final double BLOCKS_PER_METRE = 2.0;

    private final double lengthM;
    private double turnRate = 3.0;
    private FoodDiet.Diet diet = FoodDiet.Diet.OMNIVORE;
    private boolean eatsSmallFish;
    private double preyLength = 0.6;
    private double speed = 0.55;

    private LargeSpecies(double lengthM) {
        this.lengthM = lengthM;
    }

    /** A big species of this adult length in metres (the length the size rule uses). */
    public static LargeSpecies of(double lengthM) {
        return new LargeSpecies(lengthM);
    }

    /** Most degrees per tick it turns while cruising (default 3; a ray glides with 2, a reef shark turns with 4). */
    public LargeSpecies turn(double degreesPerTick) {
        this.turnRate = degreesPerTick;
        return this;
    }

    public LargeSpecies diet(FoodDiet.Diet d) {
        this.diet = d;
        return this;
    }

    /** It eats small fish (tankmates up to {@code maxLength} blocks, bettas, shrimp). */
    public LargeSpecies eatsSmallFish(double maxLength) {
        this.eatsSmallFish = true;
        this.preyLength = maxLength;
        return this;
    }

    /** Movement speed attribute (default 0.55). */
    public LargeSpecies speed(double s) {
        this.speed = s;
        return this;
    }

    public double lengthM() { return lengthM; }
    public double turnRate() { return turnRate; }
    public FoodDiet.Diet diet() { return diet; }
    public boolean eatsSmallFish() { return eatsSmallFish; }
    public double preyLength() { return eatsSmallFish ? preyLength : 0; }
    public double speed() { return speed; }

    /** Rendered length of an adult in blocks. */
    public double lengthBlocks() {
        return lengthM * BLOCKS_PER_METRE;
    }

    /** Stocking weight of an adult, in fish: a quarter of the water blocks it needs by default (a 1 m pangasius: 25). */
    public double stock() {
        return 25.0 * lengthM * lengthM * lengthM;
    }
}
