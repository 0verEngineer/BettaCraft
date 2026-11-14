package org.overengineer.bettacraft.sources;

import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M22.2: keys for the things a {@link Source} gives (the handbook finds a page per key): {@code betta}, {@code frog},
 * {@code tankmate:<id>}, {@code critter:<id>}, {@code plant:<id>} (aquarium plants and corals), {@code floating:<id>}
 * and {@code item:<id>} for other Bettacraft items.
 */
public final class Target {
    private Target() {}

    public static final String BETTA = "betta";
    public static final String FROG = "frog";

    public static String tankmate(TankmateSpecies s) {
        return "tankmate:" + s.id();
    }

    public static String critter(CritterSpecies s) {
        return "critter:" + s.id();
    }

    public static String plant(PlantSpecies s) {
        return "plant:" + s.id();
    }

    public static String floating(FloatingPlantType t) {
        return "floating:" + t.id();
    }

    public static String item(String path) {
        return "item:" + path;
    }

    /** The Bettacraft item id path a target stands for (plants, floating plants, items), or null for animals. */
    public static String itemPath(String target) {
        int c = target.indexOf(':');
        if (c < 0) return null;
        String kind = target.substring(0, c);
        return kind.equals("plant") || kind.equals("floating") || kind.equals("item") ? target.substring(c + 1) : null;
    }
}
