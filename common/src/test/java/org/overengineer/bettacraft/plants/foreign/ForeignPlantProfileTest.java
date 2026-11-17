package org.overengineer.bettacraft.plants.foreign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** M38: the profiles of the plants of Minecraft and other mods (pure parsing, the shipped files, the reference masses). */
class ForeignPlantProfileTest {
    private static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    @Test
    void parsesBlocksTagsRoleAndFactor() {
        List<String> warn = new ArrayList<>();
        ForeignPlantProfile p = ForeignPlantProfile.parse(json("{\"blocks\":[\"fintastic:hornwort\",\"#minecraft:corals\"],\"role\":\"stem\",\"factor\":0.5}"), warn::add);
        assertEquals(1, p.blocks().size());
        assertEquals("fintastic:hornwort", p.blocks().get(0).toString());
        assertEquals("minecraft:corals", p.tags().get(0).toString());
        assertEquals(ForeignPlantRole.STEM, p.role());
        assertEquals(0.5, p.factor(), 1e-9);
        assertFalse(p.required(), "not required by default");
        assertTrue(warn.isEmpty());
        ForeignPlantProfile one = ForeignPlantProfile.parse(json("{\"block\":\"minecraft:kelp\",\"role\":\"none\",\"required\":true}"), warn::add);
        assertEquals(ForeignPlantRole.NONE, one.role());
        assertTrue(one.required());
        assertEquals(1.0, one.factor(), 1e-9);
    }

    @Test
    void refusesBrokenProfilesAndClampsTheFactor() {
        assertThrows(ForeignPlantProfile.Invalid.class, () -> ForeignPlantProfile.parse(json("{\"role\":\"stem\"}"), w -> {}));
        assertThrows(ForeignPlantProfile.Invalid.class, () -> ForeignPlantProfile.parse(json("{\"blocks\":[],\"role\":\"stem\"}"), w -> {}));
        assertThrows(ForeignPlantProfile.Invalid.class, () -> ForeignPlantProfile.parse(json("{\"blocks\":[\"a:b\"]}"), w -> {}));
        assertThrows(ForeignPlantProfile.Invalid.class, () -> ForeignPlantProfile.parse(json("{\"blocks\":[\"a:b\"],\"role\":\"tree\"}"), w -> {}));
        List<String> warn = new ArrayList<>();
        ForeignPlantProfile p = ForeignPlantProfile.parse(json("{\"blocks\":[\"a:b\",\"Not Valid\"],\"role\":\"moss\",\"factor\":9}"), warn::add);
        assertEquals(4.0, p.factor(), 1e-9);
        assertEquals(2, warn.size(), "the bad id and the factor are warned about");
    }

    @Test
    void everyShippedProfileParsesAndNoBlockHasTwo() throws IOException {
        Path dir = Path.of(System.getProperty("bettacraft.projectDir", ".")).resolve("src/main/resources/data/bettacraft/bettacraft/foreign_plants");
        Set<String> seen = new HashSet<>();
        int n = 0;
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.sorted().toList()) {
                List<String> warn = new ArrayList<>();
                ForeignPlantProfile p = ForeignPlantProfile.parse(json(Files.readString(f)), warn::add);
                assertTrue(warn.isEmpty(), f + ": " + warn);
                assertFalse(p.required(), f + ": the defaults are not required (the mods are optional)");
                assertTrue(p.role() != ForeignPlantRole.NONE, f + ": a default with an effect");
                for (var b : p.blocks()) assertTrue(seen.add(b.toString()), b + " is in two profiles");
                for (var t : p.tags()) assertTrue(seen.add("#" + t), t + " is in two profiles");
                String ns = f.getFileName().toString().split("_")[0];
                for (var b : p.blocks()) assertTrue(f.getFileName().toString().startsWith(b.getNamespace()) || ns.equals("minecraft"), f + ": named after its mod");
                n++;
            }
        }
        assertTrue(n >= 25, "the defaults of vanilla, Fintastic, Regions Unexplored, Alex's Caves and Twilight Forest");
        // no id contains the words Better Fish Tanks removes recipes for
        for (String id : seen) for (String bad : List.of("aquarium", "fishtank", "fish_tank", "fishbowl", "fish_bowl")) assertFalse(id.contains(bad), id);
    }

    @Test
    void referenceMassesFollowOurPlants() {
        for (ForeignPlantRole r : ForeignPlantRole.values()) {
            double m = r.adultMass(16);
            if (!r.plant()) {
                assertEquals(0, m, 1e-9, r + " is no plant");
                continue;
            }
            assertTrue(m >= 40 && m <= 600, r + ": a full-grown plant of ours has some dozens to hundreds of blocks: " + m);
            assertEquals(m, r.adultMass(16), 1e-9, "cached");
        }
        assertTrue(ForeignPlantRole.CARPET.adultMass(16) < ForeignPlantRole.STEM.adultMass(16), "a carpet is smaller than a stem plant");
        assertEquals(ForeignPlantRole.CARPET.adultMass(16), ForeignPlantRole.FLOATING.adultMass(16), 1e-9, "floating leaves weigh like a carpet");
        assertTrue(ForeignPlantRole.STEM.adultMass(8) < ForeignPlantRole.STEM.adultMass(16), "a smaller scale has fewer blocks");
    }
}
