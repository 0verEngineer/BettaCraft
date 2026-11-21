package org.overengineer.bettacraft.sources;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.plants.wild.WildPlantBlock;
import org.overengineer.bettacraft.plants.wild.WildPlantKind;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;

/**
 * M28.1: every plant of the mod, every aquarium plant, coral and floating plant (also the ones later milestones add),
 * grows wild in the world: a wild block the shears cut (the wild plants of {@code plants/wild}, the wild corals of the
 * reefs) or a wild floating patch on the water. Net, fishing, bottles or the trader alone are not enough. Deliberate
 * exceptions stand in {@link #EXCEPTIONS} with their reason.
 */
class WildPlantSourcesTest {
    /** Plants without a wild block on purpose (target key, reason). Keep it short. */
    static final Map<String, String> EXCEPTIONS = Map.of(
            Target.floating(FloatingPlantType.LILY_PAD), "the vanilla lily pad: grows in every swamp already");

    /** Every plant target of the mod. */
    static Map<String, String> plants() {
        Map<String, String> out = new LinkedHashMap<>();
        for (PlantSpecies s : PlantSpecies.values()) out.put(Target.plant(s), s.id());
        for (FloatingPlantType t : FloatingPlantType.values()) {
            out.put(t.plantItem() ? Target.plant(PlantSpecies.byId(t.id())) + "#floating" : Target.floating(t), t.id() + " (floating)");
        }
        return out;
    }

    /** A wild source of the target: shears on a wild block, or breaking a wild floating patch. */
    private static boolean wild(String key) {
        boolean riccia = key.endsWith("#floating"); // floating Riccia: the aquarium plant item, from a floating patch
        boolean floating = riccia || key.startsWith("floating:");
        String target = riccia ? key.substring(0, key.length() - "#floating".length()) : key;
        for (Source s : Sources.of(target)) {
            if (s.block() == null || !s.block().startsWith("bettacraft:wild_")) continue;
            boolean patch = s.block().equals(Sources.WILD_FLOATING_BLOCK);
            if (floating == patch && (s.kind() == Source.Kind.SHEARS || s.kind() == Source.Kind.BLOCK_DROP)) return true;
        }
        return false;
    }

    @Test
    void everyPlantGrowsWildInTheWorld() {
        List<String> problems = new ArrayList<>();
        for (var e : plants().entrySet()) {
            if (EXCEPTIONS.containsKey(e.getKey())) continue;
            if (!wild(e.getKey())) problems.add(e.getValue() + ": no wild block in the world");
        }
        assertTrue(problems.isEmpty(), "plants that do not grow wild (add them to WildTables.wildPlants / wildFloating or the wild corals, "
                + "or put them on the exception list with a reason):\n" + String.join("\n", problems));
    }

    @Test
    void theExceptionsAreStillNeeded() {
        for (String key : EXCEPTIONS.keySet()) {
            assertTrue(plants().containsKey(key), "exception for an unknown plant: " + key);
            assertTrue(!wild(key), key + " grows wild now: take it off the exception list");
        }
    }

    /** Strong-light and soft-water plants are rare in every water (weight 3 at most), and every table is freshwater only. */
    @Test
    void strongLightAndSoftWaterPlantsAreRare() {
        for (WildTables.PlantWater w : WildTables.PlantWater.values()) {
            for (var e : WildTables.wildPlants(w).entries()) {
                PlantSpecies s = e.value();
                assertTrue(!s.marine(), s + " is a coral in " + w);
                boolean demanding = s.light() == PlantSpecies.Light.HIGH || s.light() == PlantSpecies.Light.VERY_HIGH || s.softWater();
                if (demanding) assertTrue(e.weight() <= 3, s + " is too common in " + w + " (" + e.weight() + ")");
            }
            assertTrue(!WildTables.wildFloating(w).contains(FloatingPlantType.LILY_PAD), "the lily pad in " + w);
        }
    }

    /** Every wild model has its textures (the shared looks of the assetgen script {@code wild_plants.py}). */
    @Test
    void everyLookHasItsTextures() {
        Path tex = Path.of("src/main/resources/assets/bettacraft/textures/block");
        for (PlantSpecies s : PlantSpecies.values()) {
            WildPlantKind k = WildPlantKind.of(s);
            if (k == null || k == WildPlantKind.MOSS) continue;
            String name = k.id() + "_" + WildPlantKind.look(s);
            // M55: the carpets have their own textures per design (WildCarpetModelTest checks them)
            String[] suffixes = k == WildPlantKind.CARPET ? new String[]{"_sprig", "_blade_ground", "_lawn_ground", "_dome"} : new String[]{"", "_accent", "_top"};
            for (String suffix : suffixes) {
                assertTrue(Files.exists(tex.resolve(name + suffix + ".png")), "missing texture " + name + suffix + " for " + s);
            }
        }
        for (FloatingPlantType t : FloatingPlantType.values()) {
            if (t == FloatingPlantType.LILY_PAD) continue;
            assertTrue(Files.exists(tex.resolve("wild_floating_" + t.id() + ".png")), "missing texture of the wild " + t.id());
        }
    }
}
