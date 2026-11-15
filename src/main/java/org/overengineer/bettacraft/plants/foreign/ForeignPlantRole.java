package org.overengineer.bettacraft.plants.foreign;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.overengineer.bettacraft.plants.PlantShapes;
import org.overengineer.bettacraft.plants.PlantSpecies;

/**
 * M38: what a plant (or rock) of Minecraft or another mod is in our tanks. The plant roles stand for a growth form of our
 * own plants: one such plant (a connected group of its blocks) works like a full-grown plant of ours of that form, times
 * {@code interiorPlants.foreignPlantFactor}. Floating leaves also shade the surface, live rock is a small biological filter
 * in salt water, and {@link #NONE} takes a block out again (a data pack can remove a default this way).
 */
public enum ForeignPlantRole {
    STEM(PlantSpecies.Form.STEM),
    ROSETTE(PlantSpecies.Form.ROSETTE),
    EPIPHYTE(PlantSpecies.Form.EPIPHYTE),
    MOSS(PlantSpecies.Form.MOSS),
    CARPET(PlantSpecies.Form.CARPET),
    RIBBON(PlantSpecies.Form.RIBBON),
    BULB(PlantSpecies.Form.BULB),
    /** Floating leaves (lily pads, duckweed): like one of our carpets in mass, and they shade the surface block by block. */
    FLOATING(PlantSpecies.Form.CARPET),
    /** Corals, anemones, sponges: like the median of our corals. */
    CORAL(null),
    /** Live rock: a small biological filter in salt water (like ours times the factor), no plant. */
    LIVE_ROCK(null),
    /** No effect (decoration). */
    NONE(null);

    @Nullable
    private final PlantSpecies.Form form;
    private static final Map<Integer, Double> MASS = new ConcurrentHashMap<>();

    ForeignPlantRole(@Nullable PlantSpecies.Form form) {
        this.form = form;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Nullable
    public static ForeignPlantRole byId(String id) {
        for (ForeignPlantRole r : values()) if (r.id().equals(id)) return r;
        return null;
    }

    /** Counts as a plant (plant mass), unlike live rock and decoration. */
    public boolean plant() {
        return this != LIVE_ROCK && this != NONE;
    }

    /**
     * Pure: the blocks (plant mass at interior scale {@code scale}) of a full-grown plant of ours of this growth form: the
     * median over our species of that form (corals: of every coral), four plants each with a headroom of two tank blocks.
     * 0 for live rock and decoration.
     */
    public double adultMass(int scale) {
        if (!plant()) return 0;
        int s = Math.max(1, scale);
        return MASS.computeIfAbsent(s * 16 + ordinal(), k -> median(s));
    }

    private double median(int scale) {
        List<Double> masses = new ArrayList<>();
        for (PlantSpecies sp : PlantSpecies.values()) {
            boolean match = this == CORAL ? sp.form().coral() : sp.form() == form;
            if (!match) continue;
            double n = 0;
            for (long seed = 1; seed <= 4; seed++) {
                PlantShapes.Shape sh = PlantShapes.of(sp, seed * 7919L, scale, 2 * scale);
                n += sh.count(sh.stages());
            }
            masses.add(n / 4);
        }
        if (masses.isEmpty()) return 0;
        masses.sort(Double::compare);
        int m = masses.size();
        return m % 2 == 1 ? masses.get(m / 2) : (masses.get(m / 2 - 1) + masses.get(m / 2)) / 2;
    }
}
