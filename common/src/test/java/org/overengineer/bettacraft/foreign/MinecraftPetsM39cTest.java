package org.overengineer.bettacraft.foreign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.terrarium.LandWalker;

/**
 * M39 part c (optional feature #25, question H3): the peaceful animals of Minecraft as terrarium pets. The new terrarium keys
 * {@code needs} and {@code grazes}, the needs of a scanned land, and the decision for every peaceful or passive animal of
 * Minecraft 1.21.1: a profile (real size; the big ones by the size rule, the large net and the tub from 1.2 m, no breeding from
 * 1.5 m), a refusal with its reason, or Bettacraft's own care of an earlier milestone.
 */
class MinecraftPetsM39cTest {
    /**
     * Every peaceful or passive animal of Minecraft 1.21.1 (spawn categories creature, ambient, water creature, water ambient,
     * axolotls; the neutral animals wolf, bee, llama, panda, polar bear, goat, dolphin) with how Bettacraft takes it.
     */
    static final Map<String, String> VANILLA = new TreeMap<>(Map.ofEntries(
            Map.entry("allay", "none"), // a spirit, no animal: the box does not take it, the old rules (no water animal) stay
            Map.entry("armadillo", "pet"), Map.entry("axolotl", "own"), Map.entry("bat", "pet"), Map.entry("bee", "pet"),
            Map.entry("camel", "refused"), Map.entry("cat", "pet"), Map.entry("chicken", "pet"), Map.entry("cod", "own"),
            Map.entry("cow", "pet"), Map.entry("dolphin", "own"), Map.entry("donkey", "pet"), Map.entry("fox", "pet"),
            Map.entry("frog", "pet"), Map.entry("glow_squid", "own"), Map.entry("goat", "pet"), Map.entry("horse", "pet"),
            Map.entry("llama", "pet"), Map.entry("mooshroom", "pet"), Map.entry("mule", "pet"), Map.entry("ocelot", "pet"),
            Map.entry("panda", "pet"), Map.entry("parrot", "pet"), Map.entry("pig", "pet"), Map.entry("polar_bear", "pet"),
            Map.entry("pufferfish", "own"), Map.entry("rabbit", "pet"), Map.entry("salmon", "own"), Map.entry("sheep", "pet"),
            Map.entry("skeleton_horse", "refused"), Map.entry("sniffer", "refused"), Map.entry("squid", "own"), Map.entry("strider", "refused"),
            Map.entry("tadpole", "own"), Map.entry("trader_llama", "pet"), Map.entry("tropical_fish", "own"), Map.entry("turtle", "own"),
            Map.entry("wolf", "pet"), Map.entry("zombie_horse", "refused")));

    private static ForeignProfile parse(String json, List<String> warnings) {
        return ForeignProfile.parse(JsonParser.parseString(json).getAsJsonObject(), warnings::add);
    }

    private static Path dir() {
        return Path.of(System.getProperty("bettacraft.projectDir", "."), "src/main/resources/data/bettacraft/bettacraft/foreign_animals");
    }

    private static Map<String, ForeignProfile> shippedMinecraft() throws IOException {
        Map<String, ForeignProfile> out = new TreeMap<>();
        try (Stream<Path> s = Files.list(dir())) {
            for (Path f : s.filter(p -> p.getFileName().toString().startsWith("minecraft_")).toList()) {
                List<String> w = new ArrayList<>();
                try (Reader r = Files.newBufferedReader(f)) {
                    ForeignProfile p = ForeignProfile.parse(JsonParser.parseReader(r).getAsJsonObject(), w::add);
                    assertTrue(w.isEmpty(), f.getFileName() + ": " + w);
                    for (var id : p.entities()) {
                        assertEquals("minecraft", id.getNamespace(), f.getFileName() + " only maps Minecraft animals");
                        assertNull(out.put(id.getPath(), p), id + " in two files");
                    }
                }
            }
        }
        return out;
    }

    @Test
    void needsAndGrazing() {
        List<String> w = new ArrayList<>();
        ForeignProfile bee = parse("""
                {"entity": "minecraft:bee", "terrarium": {"habitat": "dry", "air": [12, 32], "movement": "fly", "needs": ["flowers", "hive", "flowers"], "grazes": true}}""", w);
        assertEquals(List.of(ForeignProfile.Need.FLOWERS, ForeignProfile.Need.HIVE), bee.land().needs(), "each need once");
        assertTrue(bee.land().grazes() && bee.land().lid());
        assertTrue(w.isEmpty(), w.toString());
        ForeignProfile odd = parse("""
                {"entity": "minecraft:pig", "terrarium": {"habitat": "dry", "air": [0, 30], "needs": ["mud"], "grazes": true}}""", w);
        assertTrue(odd.land().needs().isEmpty(), "an unknown need is left out");
        assertFalse(odd.land().grazes(), "grazing needs grass or flowers");
        assertEquals(2, w.size(), w.toString());
        assertEquals("gui.bettacraft.terrarium.problem.no_hive", ForeignProfile.Need.HIVE.problemKey());
        ForeignProfile none = parse("""
                {"entity": "minecraft:chicken", "terrarium": {"habitat": "dry", "air": [5, 30]}}""", new ArrayList<>());
        assertTrue(none.land().needs().isEmpty() && !none.land().grazes(), "no needs by default");
    }

    @Test
    void scannedLandMeetsTheNeeds() {
        var s = new MinecraftPets.Scan(100, 3, 10, 1, 0, true);
        for (ForeignProfile.Need n : ForeignProfile.Need.values()) assertTrue(s.has(n), n.key());
        var bare = new MinecraftPets.Scan(100, 2, 9, 0, 0, true);
        for (ForeignProfile.Need n : ForeignProfile.Need.values()) assertFalse(bare.has(n), n.key());
        assertTrue(new MinecraftPets.Scan(0, 0, 0, 0, 0, false).has(ForeignProfile.Need.HIVE), "not scanned completely: nothing is missing");
        var bee = new ForeignProfile.Land(Double.NaN, org.overengineer.bettacraft.tank.BigTankRules.Volume.LAND, ForeignProfile.gait(LandWalker.Style.FLY), 0, true,
                true, false, false, List.of(ForeignProfile.Need.FLOWERS, ForeignProfile.Need.HIVE), true);
        assertTrue(MinecraftPets.grazed(bee, List.of(ForeignProfile.Need.HIVE)), "flowers without a hive: it finds its nectar");
        assertFalse(MinecraftPets.grazed(bee, List.of(ForeignProfile.Need.FLOWERS)), "no flowers: nothing to graze");
    }

    @Test
    void everyPeacefulVanillaAnimalHasItsDecision() throws IOException {
        Map<String, ForeignProfile> shipped = shippedMinecraft();
        Set<String> notListed = new TreeSet<>(shipped.keySet());
        notListed.removeAll(VANILLA.keySet());
        assertTrue(notListed.isEmpty(), "profiles of animals not in the list: " + notListed);
        for (var e : VANILLA.entrySet()) {
            ForeignProfile p = shipped.get(e.getKey());
            switch (e.getValue()) {
                case "pet" -> {
                    assertNotNull(p, e.getKey() + " has a profile");
                    assertNull(p.refuse(), e.getKey());
                    assertNotNull(p.terrarium(), e.getKey() + " lives in terraria");
                    assertNotNull(p.land().gait(), e.getKey() + " goes by our walker");
                    assertFalse(p.food().isEmpty(), e.getKey() + " has its food");
                }
                case "refused" -> {
                    assertNotNull(p, e.getKey() + " has a refusal");
                    assertTrue(ForeignProfile.REFUSE_REASONS.contains(p.refuse()), e.getKey() + ": " + p.refuse());
                }
                default -> assertNull(p, e.getKey() + " has no profile (own care or none)");
            }
        }
    }

    @Test
    void realSizesAndTheSizeRule() throws IOException {
        Map<String, ForeignProfile> shipped = shippedMinecraft();
        double largest = 2.6; // the largest tank, 32 x 16 x 32: about 2.6 m by the size rule
        for (var e : shipped.entrySet()) {
            ForeignProfile p = e.getValue();
            if (p.refuse() != null) continue;
            ForeignProfile.Land l = p.land();
            if (l.big()) {
                assertTrue(l.lengthM() <= largest, e.getKey() + " fits the largest tank: " + l.lengthM());
                assertEquals(l.lengthM() < 1.2, l.box(), e.getKey() + ": the box up to 1.2 m, the large net and the tub from there");
                if (l.lengthM() >= 1.5) assertFalse(p.breeding().breeds(), e.getKey() + " is too big to breed in a tank");
            } else {
                assertTrue(l.box(), e.getKey() + " goes into the box");
            }
            if (l.gait().style() == LandWalker.Style.FLY) assertTrue(l.lid(), e.getKey() + " flies: a lid");
        }
        assertTrue(shipped.get("chicken").land().lid(), "chickens flutter over an open top");
        assertTrue(shipped.get("chicken").breeding().nests(), "chicks from eggs in the sand");
        assertEquals(ForeignProfile.Type.LIVEBEARER, shipped.get("rabbit").breeding().type());
        assertTrue(shipped.get("rabbit").land().needs().contains(ForeignProfile.Need.GRASS), "rabbits need grass");
        assertEquals(List.of(ForeignProfile.Need.FLOWERS, ForeignProfile.Need.HIVE), shipped.get("bee").land().needs(), "bees: flowers and a hive");
        assertEquals(org.overengineer.bettacraft.terrarium.TerrariumNeeds.Habitat.PALUDARIUM, shipped.get("frog").terrarium().habitat(), "frogs need water");
        assertEquals("too_big", shipped.get("camel").refuse());
        assertEquals("unsuitable", shipped.get("strider").refuse());
    }
}
