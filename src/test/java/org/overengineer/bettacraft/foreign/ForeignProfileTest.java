package org.overengineer.bettacraft.foreign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.terrarium.TerrariumNeeds;

/** M32 part a: parsing of the foreign animal profiles and the shipped defaults. */
class ForeignProfileTest {
    private static ForeignProfile parse(String json, List<String> warnings) {
        return ForeignProfile.parse(JsonParser.parseString(json).getAsJsonObject(), warnings::add);
    }

    @Test
    void speciesGivesItsValuesAndTheFileWins() {
        List<String> w = new ArrayList<>();
        ForeignProfile p = parse("""
                {"entity": "fintastic:guppy", "species": "guppy",
                 "care": {"temperature": [20, 29]},
                 "breeding": {"inherit": ["FinColor", "TailColor"], "offspring_nbt": "{CanGrowUp:1b}"}}""", w);
        assertTrue(w.isEmpty(), w.toString());
        TankmateSpecies g = TankmateSpecies.GUPPY;
        assertEquals("fintastic:guppy", p.entities().get(0).toString());
        assertEquals("guppy", p.species());
        assertEquals("livebearer", p.group());
        assertEquals(20, p.tempMin());
        assertEquals(29, p.tempMax());
        assertEquals(g.minQuality(), p.minQuality());
        assertEquals(g.salinity(), p.salinity());
        assertNotNull(p.chemistry());
        assertEquals(ForeignProfile.Type.LIVEBEARER, p.breeding().type());
        assertEquals(g.breedMin(), p.breeding().tempMin());
        assertTrue(p.breeding().breeds());
        assertEquals(List.of("FinColor", "TailColor"), p.breeding().inherit());
        assertTrue(p.breeding().offspringNbt().getBoolean("CanGrowUp"));
        assertTrue(p.suppressOwn());
        assertTrue(p.eatsKind("normal") && p.eatsKind("live") && !p.eatsKind("fry"));
        assertTrue(p.tolerates(25) && !p.tolerates(30));
    }

    @Test
    void ourBreedingTypesBecomeTheThreeForeignOnes() {
        assertEquals(ForeignProfile.Type.LIVEBEARER, ForeignProfile.typeOf(TankmateSpecies.Breeding.MOUTH));
        assertEquals(ForeignProfile.Type.LIVEBEARER, ForeignProfile.typeOf(TankmateSpecies.Breeding.POUCH));
        assertEquals(ForeignProfile.Type.EGGS, ForeignProfile.typeOf(TankmateSpecies.Breeding.FOAM_NEST));
        assertEquals(ForeignProfile.Type.NONE, ForeignProfile.typeOf(TankmateSpecies.Breeding.NONE));
        // a scattering species needs plants for its eggs
        ForeignProfile tetra = parse("{\"entity\": \"x:neon\", \"species\": \"bettacraft:neon_tetra\"}", new ArrayList<>());
        assertEquals(ForeignProfile.Type.EGGS, tetra.breeding().type());
        assertTrue(tetra.breeding().needsPlants());
    }

    @Test
    void ownValuesWithoutSpecies() {
        List<String> w = new ArrayList<>();
        ForeignProfile p = parse("""
                {"entity": ["a:koi", "b:koi"], "group": "pond_fish", "required": true,
                 "care": {"temperature": [28, 4], "salinity": [0, 5], "ph": [6.5, 8.5], "tds": [50, 600], "stock": 1.5, "hunger_days": 2},
                 "food": ["normal", "#c:foods/fish", "minecraft:wheat", "pizza"],
                 "breeding": {"type": "eggs", "min_water_blocks": 16, "needs_plants": true, "young": [5, 2], "baby": false},
                 "suppress_own_breeding": false}""", w);
        assertEquals(2, p.entities().size());
        assertTrue(p.required());
        assertEquals(4, p.tempMin());
        assertEquals(28, p.tempMax());
        assertEquals(new Salinity.Range(0, 5), p.salinity());
        assertEquals(6.5F, p.chemistry().phMin());
        assertEquals(600F, p.chemistry().tdsMax());
        assertEquals(1.5, p.stock());
        assertEquals(2, p.hungerDays());
        assertEquals(List.of("normal", "#c:foods/fish", "minecraft:wheat"), p.food());
        assertEquals(1, w.size(), "the unknown food is reported");
        assertEquals(2, p.breeding().youngMin());
        assertEquals(5, p.breeding().youngMax());
        assertEquals(16, p.breeding().minWaterBlocks());
        assertFalse(p.breeding().baby());
        assertFalse(p.suppressOwn());
        assertNull(p.terrarium());
    }

    @Test
    void defaultsAndMistakes() {
        List<String> w = new ArrayList<>();
        ForeignProfile p = parse("{\"entity\": \"a:fish\"}", w);
        assertEquals(ForeignProfile.Type.NONE, p.breeding().type());
        assertFalse(p.breeding().breeds());
        assertEquals(Salinity.Range.FRESH, p.salinity());
        assertNull(p.chemistry());
        assertTrue(Double.isNaN(p.stock()));
        assertTrue(w.isEmpty());
        parse("{\"entity\": \"a:fish\", \"species\": \"nothing\", \"care\": {\"salinity\": \"lava\"}, \"breeding\": {\"type\": \"cloning\"}}", w);
        assertEquals(3, w.size(), w.toString());
        assertThrows(ForeignProfile.Invalid.class, () -> parse("{\"species\": \"guppy\"}", new ArrayList<>()));
        assertThrows(ForeignProfile.Invalid.class, () -> parse("{\"entity\": \"a:fish\", \"care\": {\"temperature\": \"warm\"}}", new ArrayList<>()));
        assertThrows(ForeignProfile.Invalid.class, () -> parse("{\"entity\": \"Not An Id\"}", new ArrayList<>()));
    }

    @Test
    void terrariumNeeds() {
        ForeignProfile p = parse("""
                {"entity": "naturalist:tortoise", "terrarium": {"habitat": "dry", "air": [20, 32], "humidity": [30, 60], "uv": 3, "basking": true}}""",
                new ArrayList<>());
        assertNotNull(p.terrarium());
        assertEquals(TerrariumNeeds.Habitat.DRY, p.terrarium().habitat());
    }

    /** Every shipped default parses without a warning. */
    @Test
    void shippedDefaultsParse() throws IOException {
        Path dir = Path.of(System.getProperty("bettacraft.projectDir", "."), "src/main/resources/data/bettacraft/bettacraft/foreign_animals");
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(f -> f.toString().endsWith(".json")).toList();
        }
        assertTrue(files.size() >= 10, "defaults for Fintastic, Aquaculture, Critters and Companions, Naturalist");
        for (Path f : files) {
            List<String> w = new ArrayList<>();
            JsonObject o = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
            ForeignProfile p = ForeignProfile.parse(o, w::add);
            assertTrue(w.isEmpty(), f.getFileName() + ": " + w);
            assertFalse(p.required(), f.getFileName() + " must not require its mod");
        }
    }
}
