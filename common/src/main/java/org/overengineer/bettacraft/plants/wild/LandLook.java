package org.overengineer.bettacraft.plants.wild;

import org.overengineer.bettacraft.plants.PlantSpecies;

/**
 * M56: the look of a terrarium plant in the world ({@link WildLandPlantBlock}, block {@code wild_<species>}): a shared grey model
 * per look, tinted in the colours of the species (leaves 0, stalks 1, flowers / bracts / spines 2). Pure data (the datagen,
 * the asset generator {@code tools/assetgen/terrarium_plants.py} and the tests use it).
 */
public enum LandLook {
    BROMELIAD(Model.CROSS, 13), FERN(Model.CROSS, 14), BROADFERN(Model.CROSS, 15), BROADLEAF(Model.CROSS, 9), VINE(Model.CROSS, 12),
    ORCHID(Model.CROSS, 12), AIRPLANT(Model.CROSS, 8), GRASS(Model.CROSS, 15), RUSH(Model.CROSS, 16), PAPYRUS(Model.CROSS, 16),
    SUNDEW(Model.CROSS, 6), FLYTRAP(Model.CROSS, 6), PITCHER(Model.CROSS, 15), NEPENTHES(Model.CROSS, 13), SUCCULENT(Model.CROSS, 6),
    SPIKY(Model.CROSS, 13),
    /** The mosses: the round cushions of the wild aquarium mosses. */
    MOSS(Model.MOSS, 4),
    /** Spikemoss and stonecrop: the wild carpet of fine sprigs. */
    CARPET(Model.CARPET, 6),
    BARREL(Model.CACTUS, 10), CLUSTER(Model.CACTUS, 8), COLUMN(Model.CACTUS, 16), OPUNTIA(Model.CROSS, 15);

    /** The template a look uses. */
    public enum Model { CROSS, MOSS, CARPET, CACTUS }

    public final Model model;
    /** Height of the outline (pixels, full grown). */
    public final int height;

    LandLook(Model model, int height) {
        this.model = model;
        this.height = height;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /** The model of a terrarium plant in the world. */
    public static LandLook of(PlantSpecies s) {
        return switch (s) {
            case NEOREGELIA, GUZMANIA, VRIESEA -> BROMELIAD;
            case BOSTON_FERN -> FERN;
            case BIRDS_NEST_FERN -> BROADFERN;
            case BEGONIA, BUTTERWORT, FITTONIA, PEPEROMIA -> BROADLEAF;
            case POTHOS, MARCGRAVIA -> VINE;
            case MINI_ORCHID -> ORCHID;
            case TILLANDSIA -> AIRPLANT;
            case SWAMP_SEDGE, BLUE_GRAMA, NEEDLEGRASS -> GRASS;
            case SOFT_RUSH -> RUSH;
            case PAPYRUS, UMBRELLA_SEDGE -> PAPYRUS;
            case SUNDEW -> SUNDEW;
            case VENUS_FLYTRAP -> FLYTRAP;
            case SARRACENIA -> PITCHER;
            case NEPENTHES -> NEPENTHES;
            case ECHEVERIA, HAWORTHIA, LITHOPS -> SUCCULENT;
            case ALOE, AGAVE -> SPIKY;
            case CUSHION_MOSS, REINDEER_LICHEN -> MOSS;
            case SELAGINELLA, SEDUM -> CARPET;
            case BARREL_CACTUS -> BARREL;
            case MAMMILLARIA -> CLUSTER;
            case COLUMN_CACTUS -> COLUMN;
            case PRICKLY_PEAR -> OPUNTIA;
            default -> GRASS;
        };
    }

    /** The item icon of a terrarium plant ({@code item/terrarium_<icon>} and {@code _detail}, {@code tools/assetgen/terrarium_plants.py}). */
    public static String icon(PlantSpecies s) {
        return switch (s) {
            case NEOREGELIA -> "bromeliad";
            case GUZMANIA -> "bromeliad_spike";
            case VRIESEA -> "vriesea"; // M62: an own icon for every plant
            case TILLANDSIA -> "airplant";
            case POTHOS -> "vine";
            case MARCGRAVIA -> "shingle";
            case FITTONIA -> "bush";
            case PEPEROMIA -> "peperomia";
            case BOSTON_FERN -> "fern";
            case BIRDS_NEST_FERN -> "broadfern";
            case CUSHION_MOSS -> "moss";
            case REINDEER_LICHEN -> "lichen";
            case SELAGINELLA -> "carpet";
            case SEDUM -> "carpet_flowers";
            case MINI_ORCHID -> "orchid";
            case BEGONIA -> "begonia";
            case BUTTERWORT -> "butterwort";
            case SUNDEW -> "sundew";
            case SARRACENIA -> "pitcher";
            case NEPENTHES -> "nepenthes";
            case VENUS_FLYTRAP -> "flytrap";
            case SWAMP_SEDGE -> "grass";
            case BLUE_GRAMA -> "grama";
            case SOFT_RUSH -> "rush";
            case NEEDLEGRASS -> "needlegrass";
            case PAPYRUS -> "papyrus";
            case UMBRELLA_SEDGE -> "umbrella";
            case BARREL_CACTUS -> "barrel";
            case MAMMILLARIA -> "cluster";
            case COLUMN_CACTUS -> "column";
            case PRICKLY_PEAR -> "opuntia";
            case ECHEVERIA -> "succulent";
            case HAWORTHIA -> "zebra";
            case ALOE -> "spiky";
            case AGAVE -> "agave";
            case LITHOPS -> "lithops";
            default -> "grass";
        };
    }

    /** The carpet look (M55 wild carpets: {@code fine} sprigs or a {@code mat}) of the carpet plants. */
    public static String carpet(PlantSpecies s) {
        return s == PlantSpecies.SEDUM ? "mat" : "fine";
    }
}
