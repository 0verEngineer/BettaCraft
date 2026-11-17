package org.overengineer.bettacraft.plants.wild;

import javax.annotation.Nullable;
import org.overengineer.bettacraft.plants.PlantSpecies;

/**
 * M28.1: the growth forms of the wild plant blocks (the block id is {@code wild_<kind>}) and the shared look of every
 * species in its block. Pure data (the unit tests and the datagen use it).
 */
public enum WildPlantKind {
    /** Anubias, Bucephalandra, Java fern: on stone and wood. */
    EPIPHYTE,
    /** Mosses and the Marimo: cushions on stone and wood (the Marimo as balls on the floor). */
    MOSS,
    /** Rosettes, ribbon plants (Vallisneria) and the tubers (lotus, Aponogeton) on the bottom. */
    ROSETTE,
    /** Stem plants: a clump of upright shoots on the bottom. */
    STEM,
    /** Carpets: a low patch on the bottom. */
    CARPET;

    public String id() {
        return "wild_" + name().toLowerCase(java.util.Locale.ROOT);
    }

    /** The kind of block a species grows wild in, or null (corals: {@code reef/WildCoralBlock}). */
    @Nullable
    public static WildPlantKind of(PlantSpecies s) {
        return switch (s.form()) {
            case EPIPHYTE -> EPIPHYTE;
            case MOSS -> MOSS;
            case ROSETTE, RIBBON, BULB -> ROSETTE;
            case STEM -> STEM;
            case CARPET -> CARPET;
            default -> null;
        };
    }

    /**
     * The shared model of a species (its leaf shape) in its block: {@code wild_<kind>_<look>}. Pure (the datagen and the
     * tests use it too).
     */
    public static String look(PlantSpecies s) {
        WildPlantKind k = of(s);
        if (k == null) return "none";
        return switch (k) {
            case EPIPHYTE -> s == PlantSpecies.JAVA_FERN ? "frond" : "needle".equals(s.leafTexture()) ? "needle"
                    : "wavy".equals(s.leafTexture()) ? "wavy" : "broad";
            case MOSS -> s == PlantSpecies.MARIMO ? "marimo" : s == PlantSpecies.RICCIA ? "riccia" : "moss";
            case ROSETTE -> switch (s.form()) {
                case RIBBON -> "ribbon";
                case BULB -> "lotus".equals(s.leafTexture()) ? "lotus" : "wavy";
                default -> "ribbon".equals(s.leafTexture()) ? "grass" : "wavy".equals(s.leafTexture()) ? "wavy" : "broad";
            };
            case STEM -> s == PlantSpecies.LIMNOPHILA_SESSILIFLORA || s == PlantSpecies.MYRIOPHYLLUM_RED ? "feathery"
                    : s == PlantSpecies.HYGROPHILA_PINNATIFIDA ? "lobed" : "fine";
            case CARPET -> s == PlantSpecies.ELEOCHARIS ? "needle" : s == PlantSpecies.MARSILEA ? "clover" : s == PlantSpecies.STAUROGYNE ? "fine" : "mat";
        };
    }
}
