package org.overengineer.bettacraft.entity.tankmate;

import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * How high the rendered tankmate (dorsal fin included) reaches above its position, like {@code BettaExtents} for the
 * bettas. Up to 1.33.2 the server kept only 0.8 of the hitbox height below the surface; the tall angelfish (model 2.5
 * times as high as its hitbox) and the discus then swam with their dorsal fins out of the water and through the lid.
 * The table is the highest point of each model in model pixels above the feet (rest pose and the dorsal fin's swimming
 * tilt; {@code TankmateExtentsTest} checks it against the baked models of {@code TankmateModelDefinitions}).
 */
public final class TankmateExtents {
    /** Model pixels of the nominal 32 px model length per block of species length (32 px = 2 blocks at scale 1). */
    private static final double MODEL_BLOCKS = 2.0;
    /** Gap between the fin tip and the surface (blocks). */
    public static final double MARGIN = 0.02;

    private TankmateExtents() {}

    /** Highest point of the model above the feet in model pixels (adult, scale 1). */
    public static double topPixels(TankmateSpecies.Model m) {
        return switch (m) {
            case TETRA -> 14.5;
            case RASBORA, LOACH, LIVEBEARER, SWORDTAIL, PANGASIUS, CLOWN -> 15.5;
            case CORY -> 14.75;
            case PLECO, GARRA -> 11.5;
            case OTO -> 11;
            case KUHLI -> 5;
            case PLATY, DAMSEL -> 17;
            case GOURAMI -> 19;
            case TANG -> 23;
            case GOBY, PUFFER -> 13;
            case SEAHORSE -> 32.25;
            case CICHLID -> 20;
            case APISTO -> 18.5;
            case ANGEL -> 40.5;
            case DISCUS -> 26.5;
            case BARB -> 19.25;
            case RAINBOW -> 16.5;
            case REDTAIL -> 15.75;
            case WELS -> 10.5;
            case ARAPAIMA -> 10;
            case STINGRAY -> 4.25;
            case BAMBOO_SHARK -> 7.75;
            case REEF_SHARK -> 14;
            case EAGLE_RAY, COWNOSE_RAY -> 3.5;
            case MORAY -> 6.25;
        };
    }

    /** Length factor of the sexes: livebearer females are bigger, gourami females a little smaller. */
    public static float sexScale(TankmateSpecies s, boolean female) {
        return switch (s.model()) {
            case LIVEBEARER -> female ? 1.2F : 0.9F;
            case PLATY -> female ? 1.08F : 0.95F;
            case SWORDTAIL -> female ? 1.05F : 0.95F; // M29 part c
            case GOURAMI -> female ? 0.92F : 1.0F;
            case CICHLID -> female ? 0.9F : 1.0F; // M29: dwarf cichlid females are smaller
            case APISTO -> female ? 0.72F : 1.0F;
            default -> female ? 1.04F : 1.0F;
        };
    }

    /** M21.6: hybrids are between the guppy and the endler in size (by their endler share). */
    public static float hybridScale(TankmateEntity fish) {
        var p = fish.livebearerPhenotype();
        if (p == null) return 1F;
        double len = TankmateSpecies.GUPPY.length() + (TankmateSpecies.ENDLER_GUPPY.length() - TankmateSpecies.GUPPY.length()) * p.endler();
        return (float) (len / fish.species().length());
    }

    /** The scale the renderer draws the model at: the species' length, the stage, the sex and the hybrid share. */
    public static float renderScale(TankmateEntity fish) {
        TankmateSpecies s = fish.species();
        return (float) (s.length() / MODEL_BLOCKS) * fish.lifeStage().scale() * sexScale(s, fish.isFemale()) * hybridScale(fish);
    }

    /** Extra height of a variety drawn taller than its model (the round balloon ram). */
    public static float heightScale(TankmateEntity fish) {
        return fish.species() == TankmateSpecies.RAM_CICHLID && "balloon".equals(fish.variety()) ? 1.15F : 1F;
    }

    /** M48 part a: the length factor of the tail and dorsal fin (the longfin panda cory; 1 = the model). */
    public static float finScale(TankmateEntity fish) {
        return fish.species() == TankmateSpecies.PANDA_CORY && "longfin".equals(fish.variety()) ? 1.7F : 1F;
    }

    /** How high the rendered fish reaches above its position (blocks; never below the hitbox). */
    public static double top(TankmateEntity fish) {
        double model = topPixels(fish.species().model()) / 16.0 * renderScale(fish) * heightScale(fish) * (finScale(fish) > 1F ? 1.25 : 1.0);
        return Math.max(model, fish.getBbHeight());
    }

    /** The distance the position keeps below the surface so that the whole fish stays in the water. */
    public static double surfaceMargin(TankmateEntity fish) {
        return top(fish) + MARGIN;
    }
}
