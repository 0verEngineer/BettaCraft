package org.overengineer.bettacraft.foreign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.tank.BigTankRules;
import org.overengineer.bettacraft.terrarium.LandWalker;

/**
 * M39 part b: the terrarium keys of the insects, mammals and birds of other mods ({@code lid}, {@code cleanup}, {@code glows}) and
 * the decisions of the shipped profiles: every flier needs a lid, the insects do not breed (question G1), the clean-up crew are
 * the woodlice and snails, the big land animals go by the size rule (largest tank: about 2.6 m) and do not breed, the giants are
 * refused.
 */
class ForeignLandAnimalsM39bTest {
    private static ForeignProfile parse(String json, List<String> warnings) {
        return ForeignProfile.parse(JsonParser.parseString(json).getAsJsonObject(), warnings::add);
    }

    private static Path dir() {
        return Path.of(System.getProperty("bettacraft.projectDir", "."), "src/main/resources/data/bettacraft/bettacraft/foreign_animals");
    }

    private static ForeignProfile shipped(String file) throws IOException {
        List<String> w = new ArrayList<>();
        try (Reader r = Files.newBufferedReader(dir().resolve(file + ".json"))) {
            ForeignProfile p = ForeignProfile.parse(JsonParser.parseReader(r).getAsJsonObject(), w::add);
            assertTrue(w.isEmpty(), file + ": " + w);
            return p;
        }
    }

    @Test
    void lidCleanupAndGlow() {
        List<String> w = new ArrayList<>();
        ForeignProfile fly = parse("""
                {"entity": "naturalist:butterfly", "terrarium": {"habitat": "dry", "air": [20, 30], "movement": "fly"}}""", w);
        assertTrue(fly.land().lid(), "a flier needs a lid by default");
        assertFalse(fly.land().cleanup());
        ForeignProfile open = parse("""
                {"entity": "naturalist:dragonfly", "terrarium": {"habitat": "paludarium", "air": [20, 30], "movement": "fly", "lid": false}}""", w);
        assertFalse(open.land().lid(), "unless the profile says so");
        ForeignProfile peacock = parse("""
                {"entity": "livingthings:peacock", "terrarium": {"habitat": "dry", "air": [20, 30], "movement": "walk", "lid": true, "length": 1.0}}""", w);
        assertTrue(peacock.land().lid(), "a walking bird that flies up now and then");
        assertTrue(peacock.land().big());
        ForeignProfile crew = parse("""
                {"entity": "crittersandcompanions:roly_poly", "terrarium": {"habitat": "dry", "air": [15, 27], "cleanup": true}}""", w);
        assertTrue(crew.land().cleanup());
        assertFalse(crew.land().lid(), "a walker");
        ForeignProfile firefly = parse("""
                {"entity": "naturalist:firefly", "terrarium": {"habitat": "any", "air": [15, 30], "movement": "fly", "glows": true}}""", w);
        assertTrue(firefly.land().glows() && firefly.land().lid());
        assertTrue(w.isEmpty(), w.toString());
        // the M35 constructor: a flier needs a lid there too
        var land = new ForeignProfile.Land(Double.NaN, BigTankRules.Volume.ALL, ForeignProfile.gait(LandWalker.Style.FLY), 0, true);
        assertTrue(land.lid());
    }

    @Test
    void shippedInsectsMammalsAndBirds() throws IOException {
        // insects: no breeding of ours (G1), fliers with a lid
        for (String f : List.of("naturalist_caterpillar", "naturalist_butterfly", "naturalist_firefly", "naturalist_ant", "crittersandcompanions_stag_beetle",
                "crittersandcompanions_stick_bug", "crittersandcompanions_jumping_spider", "crittersandcompanions_ladybug", "crittersandcompanions_weevil")) {
            ForeignProfile p = shipped(f);
            assertFalse(p.breeding().breeds(), f + " does not breed");
            assertNotNull(p.land(), f);
            boolean flies = p.land().gait() != null && p.land().gait().style() == LandWalker.Style.FLY;
            assertEquals(flies, p.land().lid(), f + ": a lid exactly for the fliers");
        }
        assertTrue(shipped("naturalist_firefly").land().glows());
        assertTrue(shipped("naturalist_dragonfly").land().lid(), "the dragonflies of M35 need one now too");
        assertTrue(shipped("crittersandcompanions_dragonfly").land().lid());
        // the clean-up crew
        for (String f : List.of("crittersandcompanions_roly_poly", "crittersandcompanions_snail", "livingthings_snail")) {
            ForeignProfile p = shipped(f);
            assertTrue(p.land().cleanup(), f);
            assertTrue(p.breeding().breeds(), f + " breeds (no insect)");
        }
        // birds: a lid
        for (String f : List.of("naturalist_bird", "crittersandcompanions_shima_enaga", "livingthings_owl", "naturalist_vulture", "livingthings_peacock",
                "naturalist_turkey")) {
            assertTrue(shipped(f).land().lid(), f + " needs a lid");
        }
        // big ones: by the size rule (at most 2.6 m fit the largest tank), never into the box, no breeding from 1.5 m on
        try (Stream<Path> s = Files.list(dir())) {
            for (Path file : s.toList()) {
                String name = file.getFileName().toString().replace(".json", "");
                ForeignProfile p = shipped(name);
                if (p.land() == null || !p.land().big()) continue;
                assertTrue(p.land().lengthM() <= 2.6, name + " fits the largest tank");
                if (p.land().lengthM() >= 1.2) assertFalse(p.land().box(), name + " goes by the large net and the tub");
                // M39 part b: from 1.5 m on no breeding (the alligator of M35 lays its eggs all the same)
                if (p.land().lengthM() >= 1.5 && !name.equals("naturalist_alligator")) assertFalse(p.breeding().breeds(), name + " does not breed in a tank");
            }
        }
        assertEquals(2.5, shipped("naturalist_komodo_dragon").land().lengthM(), 1e-9);
        assertEquals(BigTankRules.Volume.LAND, shipped("naturalist_komodo_dragon").land().volume(), "over the land of a dry terrarium");
        Set<String> giants = new TreeSet<>();
        for (String f : List.of("naturalist_refused_too_big_land", "livingthings_refused_too_big")) {
            ForeignProfile p = shipped(f);
            assertEquals("too_big", p.refuse());
            p.entities().forEach(id -> giants.add(id.toString()));
        }
        assertTrue(giants.containsAll(List.of("naturalist:elephant", "naturalist:giraffe", "naturalist:hippo", "naturalist:rhino", "naturalist:mammoth",
                "livingthings:elephant", "livingthings:giraffe")), giants.toString());
    }
}
