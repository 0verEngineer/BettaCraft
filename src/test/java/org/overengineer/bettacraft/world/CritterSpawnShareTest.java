package org.overengineer.bettacraft.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.NetCatching;

/**
 * M46: the species of the small animals that spawn in a water. The apple snail is damped
 * ({@link Wildlife#APPLE_SNAIL_SPAWN_FACTOR}) so the other animals of the jungle show up too; the net keeps its weights. A
 * distribution over many picks shows several species per water (the table goes into the M46 report).
 */
class CritterSpawnShareTest {
    private static final NetCatching.Water[] WATERS = {NetCatching.Water.RIVER, NetCatching.Water.SWAMP, NetCatching.Water.JUNGLE,
            NetCatching.Water.MANGROVE, NetCatching.Water.BEACH, NetCatching.Water.WARM_OCEAN, NetCatching.Water.LUKEWARM_OCEAN};

    /** Shares of {@code n} picks with a fixed seed. */
    static Map<CritterSpecies, Integer> picks(NetCatching.Water w, boolean plants, boolean hard, int n, long seed) {
        Random r = new Random(seed);
        Map<CritterSpecies, Integer> out = new EnumMap<>(CritterSpecies.class);
        for (int i = 0; i < n; i++) {
            CritterSpecies s = Wildlife.pickCritter(w, plants, hard, r);
            if (s != null) out.merge(s, 1, Integer::sum);
        }
        return out;
    }

    @Test
    void severalSpeciesSpawnInEveryWater() {
        int n = 20000;
        StringBuilder table = new StringBuilder("| Water | Ground | Species (share of the spawns) |\n|---|---|---|\n");
        for (NetCatching.Water w : WATERS) {
            for (boolean hard : new boolean[]{false, true}) {
                Map<CritterSpecies, Integer> p = picks(w, false, hard, n, 4600 + w.ordinal());
                StringBuilder row = new StringBuilder();
                p.entrySet().stream().sorted(Map.Entry.<CritterSpecies, Integer>comparingByValue().reversed())
                        .forEach(e -> row.append(String.format(Locale.ROOT, "%s %.0f %%, ", e.getKey().id(), 100.0 * e.getValue() / n)));
                table.append("| ").append(w.name().toLowerCase(Locale.ROOT)).append(" | ").append(hard ? "sand/gravel/stone" : "soft")
                        .append(" | ").append(row.length() > 2 ? row.substring(0, row.length() - 2) : "-").append(" |\n");
                long common = p.values().stream().filter(v -> v >= n * 0.04).count();
                assertTrue(common >= 3, w + (hard ? " hard" : "") + ": only " + common + " species with 4 % or more: " + p);
                int top = p.values().stream().mapToInt(Integer::intValue).max().orElse(0);
                assertTrue(top < n * 0.55, w + ": one species takes " + 100.0 * top / n + " %");
            }
        }
        System.out.println("M46 small animal spawns per water:\n" + table);
    }

    @Test
    void theAppleSnailIsDampedOnlyWhenSpawning() {
        var net = Wildlife.critterTable(NetCatching.Water.JUNGLE, false, false);
        var spawn = Wildlife.critterSpawnTable(NetCatching.Water.JUNGLE, false, false);
        double netApple = net.stream().filter(c -> c.species() == CritterSpecies.APPLE_SNAIL).mapToDouble(NetCatching.Catch::weight).sum();
        double spawnApple = spawn.stream().filter(c -> c.species() == CritterSpecies.APPLE_SNAIL).mapToDouble(NetCatching.Catch::weight).sum();
        assertEquals(netApple * Wildlife.APPLE_SNAIL_SPAWN_FACTOR, spawnApple, 1e-9);
        assertEquals(net.size(), spawn.size(), "the same species");
        // before M46 three of four jungle spawns were apple snails; now well under half
        Map<CritterSpecies, Integer> p = picks(NetCatching.Water.JUNGLE, false, false, 20000, 46);
        double apple = p.getOrDefault(CritterSpecies.APPLE_SNAIL, 0) / 20000.0;
        assertTrue(apple > 0.2 && apple < 0.5, "apple snail share " + apple);
    }
}
