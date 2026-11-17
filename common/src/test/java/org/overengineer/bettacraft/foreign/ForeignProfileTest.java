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
        assertTrue(p.breeding().offspringNbt().getBooleanOr("CanGrowUp", false));
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

    /** M35: the land part of a terrarium profile: length, volume, movement, prey, box, and eggs into a nest. */
    @Test
    void landPartAndNest() {
        List<String> w = new ArrayList<>();
        ForeignProfile p = parse("""
                {"entity": "naturalist:alligator", "food": ["#c:foods/raw_meat"],
                 "breeding": {"type": "eggs", "nest": "#bettacraft:turtle_nest_ground", "nest_temperature": 32, "days": 5},
                 "terrarium": {"habitat": "paludarium", "length": 2.0, "volume": "all", "prey": 0.8, "box": false,
                               "movement": {"style": "walk", "walk": 0.02, "swim": 0.035}}}""", w);
        assertTrue(w.isEmpty(), w.toString());
        ForeignProfile.Land l = p.land();
        assertNotNull(l);
        assertTrue(l.big());
        assertEquals(2.0, l.lengthM(), 1e-9);
        assertEquals(org.overengineer.bettacraft.tank.BigTankRules.Volume.ALL, l.volume());
        assertEquals(0.8, l.preyLength(), 1e-9);
        assertFalse(l.box());
        assertNotNull(l.gait());
        assertEquals(0.035, l.gait().swim(), 1e-9);
        assertTrue(p.breeding().nests());
        assertEquals("bettacraft:turtle_nest_ground", p.breeding().nest().toString());
        assertEquals(32, p.breeding().nestPivot(), 1e-9);
        // the defaults: the volume by the habitat, the walker, the box, no size rule
        ForeignProfile dry = parse("{\"entity\": \"a:b\", \"terrarium\": {\"habitat\": \"dry\"}}", w);
        assertEquals(org.overengineer.bettacraft.tank.BigTankRules.Volume.LAND, dry.land().volume());
        assertFalse(dry.land().big());
        assertTrue(dry.land().box() && dry.land().walks());
        ForeignProfile own = parse("{\"entity\": \"a:b\", \"terrarium\": {\"habitat\": \"wet\", \"movement\": \"own\"}}", w);
        assertEquals(org.overengineer.bettacraft.tank.BigTankRules.Volume.WATER, own.land().volume());
        assertFalse(own.land().walks());
        assertNull(parse("{\"entity\": \"a:b\"}", w).land(), "an aquarium animal has no land part");
        assertTrue(w.isEmpty(), w.toString());
        // mistakes are warned about
        parse("{\"entity\": \"a:b\", \"terrarium\": {\"length\": -1, \"volume\": \"air\", \"movement\": \"swing\"}}", w);
        assertEquals(3, w.size(), w.toString());
    }

    /** M35 part b: the flying style, the smallest tank of a small animal, and the shipped land animals of part b. */
    @Test
    void flyingAndPartBProfiles() throws IOException {
        List<String> w = new ArrayList<>();
        ForeignProfile fly = parse("{\"entity\": \"a:b\", \"terrarium\": {\"habitat\": \"paludarium\", \"min_blocks\": 6, \"movement\": \"fly\"}}", w);
        assertTrue(w.isEmpty(), w.toString());
        assertEquals(org.overengineer.bettacraft.terrarium.LandWalker.Style.FLY, fly.land().gait().style());
        assertTrue(fly.land().gait().walk() > 0 && fly.land().gait().swim() == 0, "a flier flies, it does not swim");
        assertEquals(6, fly.terrarium().minBlocks(), 1e-9);
        ForeignProfile lizard = shipped("naturalist_lizard");
        assertEquals(org.overengineer.bettacraft.terrarium.TerrariumNeeds.Habitat.DRY, lizard.terrarium().habitat());
        assertTrue(lizard.breeding().nests() && !lizard.land().big(), "eggs into a nest; small: its hitbox decides");
        ForeignProfile snake = shipped("naturalist_snake");
        assertEquals(org.overengineer.bettacraft.terrarium.LandWalker.Style.SLITHER, snake.land().gait().style());
        assertEquals(List.of("#bettacraft:snake_prey"), snake.food());
        assertEquals(ForeignProfile.Type.LIVEBEARER, shipped("naturalist_desert_scorpion").breeding().type(), "scorpions bear live young");
        assertTrue(shipped("naturalist_desert_scorpion").land().gait().burrows(), "a scorpion hides in the ground");
        assertTrue(shipped("naturalist_jungle_scorpion").land().big(), "the jungle scorpion (hitbox 1.2) goes by its length");
        assertEquals(org.overengineer.bettacraft.terrarium.TerrariumNeeds.Habitat.PALUDARIUM, shipped("naturalist_crab").terrarium().habitat());
        assertFalse(shipped("naturalist_crab").breeding().breeds(), "crab larvae need the sea: no breeding");
        for (String big : List.of("naturalist_duck", "naturalist_capybara", "crittersandcompanions_otter")) {
            ForeignProfile b = shipped(big);
            assertTrue(b.land().big() && b.land().volume() == org.overengineer.bettacraft.tank.BigTankRules.Volume.ALL, big + ": a big paludarium animal");
        }
        assertTrue(shipped("crittersandcompanions_otter").land().preyLength() > 0, "the otter hunts small fish");
        for (String d : List.of("naturalist_dragonfly", "crittersandcompanions_dragonfly")) {
            assertEquals(org.overengineer.bettacraft.terrarium.LandWalker.Style.FLY, shipped(d).land().gait().style(), d + " flies");
        }
        assertEquals(org.overengineer.bettacraft.terrarium.LandWalker.Style.CLIMB, shipped("crittersandcompanions_leaf_insect").land().gait().style());
        ForeignProfile bunny = shipped("crittersandcompanions_sea_bunny");
        assertNull(bunny.land(), "the sea bunny is an aquarium animal");
        assertTrue(bunny.salinity().min() >= 20, "sea water");
        assertEquals(0.5, org.overengineer.bettacraft.terrarium.PaludariumWater.depthFor(0.2), 1e-9, "at least half a block of water");
        assertEquals(0.7, org.overengineer.bettacraft.terrarium.PaludariumWater.depthFor(0.6), 1e-9, "a tall fish its height and a little");
    }

    private static ForeignProfile shipped(String name) throws IOException {
        Path f = Path.of(System.getProperty("bettacraft.projectDir", "."), "src/main/resources/data/bettacraft/bettacraft/foreign_animals", name + ".json");
        List<String> w = new ArrayList<>();
        ForeignProfile p = ForeignProfile.parse(JsonParser.parseString(Files.readString(f)).getAsJsonObject(), w::add);
        assertTrue(w.isEmpty(), name + ": " + w);
        return p;
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

    /** M33 part d: the hint of the log warning for a renamed entity id: the similar ids of the mod, the most similar first. */
    @Test
    void similarIdsForARenamedEntity() {
        java.util.List<String> mod = java.util.List.of("guppy", "fancy_guppy", "neon_tetra", "betta_splendens", "piranha", "zebrafish");
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("guppy"), ForeignProfiles.similar("guppie", mod, 3));
        org.junit.jupiter.api.Assertions.assertEquals("neon_tetra", ForeignProfiles.similar("tetra", mod, 3).getFirst());
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("piranha"), ForeignProfiles.similar("red_piranha", mod, 3), "contained is close");
        org.junit.jupiter.api.Assertions.assertTrue(ForeignProfiles.similar("axolotl", mod, 3).isEmpty(), "nothing alike: no hint");
        org.junit.jupiter.api.Assertions.assertEquals(2, ForeignProfiles.similar("guppy", mod, 2).size(), "at most max");
        org.junit.jupiter.api.Assertions.assertEquals(0, ForeignProfiles.distance("koi", "koi"));
        org.junit.jupiter.api.Assertions.assertEquals(2, ForeignProfiles.distance("koi", "kio"));
    }
}
