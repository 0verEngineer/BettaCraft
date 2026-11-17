package org.overengineer.bettacraft.foreign;

import org.overengineer.bettacraft.util.NbtCompat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.tank.InhabitantRules;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.terrarium.EggSex;

/**
 * M39 part a: the profile keys of the water animals of other mods (aquarium length, refusal, sexes, sterile keys) and the ids
 * of every shipped profile and food tag entry checked against the jars of the mods (as far as they are on this machine: the
 * dev runtime's Gradle cache and the {@code run/*-mods} folders; a mod without a jar here is skipped).
 */
class ForeignAnimalsM39Test {
    private static ForeignProfile parse(String json, List<String> warnings) {
        return ForeignProfile.parse(JsonParser.parseString(json).getAsJsonObject(), warnings::add);
    }

    private static Path project() {
        return Path.of(System.getProperty("bettacraft.projectDir", "."));
    }

    @Test
    void aquariumLengthRefusalSexAndSterile() {
        List<String> w = new ArrayList<>();
        ForeignProfile tuna = parse("""
                {"entity": "aquaculture:tuna", "length": 2.0, "care": {"temperature": [14, 28], "salinity": "marine"}, "breeding": {"type": "none"}}""", w);
        assertTrue(w.isEmpty(), w.toString());
        assertEquals(2.0, tuna.bigLength(), 1e-9, "the aquarium length is the size rule's");
        assertNull(tuna.refuse());
        assertNull(tuna.land(), "an aquarium animal");

        ForeignProfile refused = parse("""
                {"entity": ["naturalist:anglerfish", "naturalist:blobfish"], "refuse": "deep_sea"}""", w);
        assertTrue(w.isEmpty(), w.toString());
        assertEquals("deep_sea", refused.refuse());
        assertFalse(refused.breeding().breeds());
        assertEquals(2, refused.entities().size());

        ForeignProfile fot = parse("""
                {"entity": "fishofthieves:splashtail", "breeding": {"type": "eggs", "inherit": ["variant"], "sterile": ["Trophy"]}}""", w);
        assertTrue(w.isEmpty(), w.toString());
        assertEquals(List.of("Trophy"), fot.breeding().sterile());
        assertFalse(fot.breeding().sexed());

        ForeignProfile turtle = parse("""
                {"entity": "aquaculture:box_turtle", "breeding": {"type": "eggs", "nest": "#bettacraft:turtle_nest_ground", "nest_temperature": 28.5,
                 "sex": "temperature"}, "terrarium": {"habitat": "dry", "air": [22, 30]}}""", w);
        assertTrue(w.isEmpty(), w.toString());
        assertEquals(ForeignProfile.Sex.TEMPERATURE, turtle.breeding().sex());
        assertTrue(turtle.breeding().sexed());
        assertEquals(0, turtle.bigLength(), 1e-9, "a small turtle: no size rule");

        // mistakes
        parse("""
                {"entity": "a:b", "length": -1}""", w);
        parse("""
                {"entity": "a:b", "length": 1, "terrarium": {"habitat": "dry", "air": [20, 30]}}""", w);
        parse("""
                {"entity": "a:b", "breeding": {"type": "eggs", "sex": "both"}}""", w);
        parse("""
                {"entity": "a:b", "breeding": {"type": "eggs", "sex": "temperature"}}""", w);
        parse("""
                {"entity": "a:b", "breeding": {"type": "eggs", "inherit": ["Trophy"], "sterile": ["Trophy"]}}""", w);
        parse("""
                {"entity": "a:b", "refuse": "Too Big!"}""", w);
        assertEquals(6, w.size(), w.toString());
    }

    /** A refusing profile wins over the tags, but not over the server's allow list. */
    @Test
    void refusalInTheRules() {
        var refused = new InhabitantRules.Facts("naturalist:whale", 3F, 2.5F, false, false, false, false, true, 0, "too_big");
        assertEquals(TankInhabitants.Acceptance.REFUSED, InhabitantRules.decide(refused, InhabitantRules.Settings.DEFAULT));
        var allowed = new InhabitantRules.Settings(List.of("naturalist:whale"), List.of(), 0.8, true);
        assertEquals(TankInhabitants.Acceptance.OK, InhabitantRules.decide(refused, allowed));
        var denied = new InhabitantRules.Settings(List.of(), List.of("naturalist:*"), 0.8, true);
        assertEquals(TankInhabitants.Acceptance.BLACKLISTED, InhabitantRules.decide(refused, denied));
        var big = new InhabitantRules.Facts("aquaculture:tuna", 0.6F, 0.4F, false, false, false, false, true, 2.0, null);
        assertEquals(TankInhabitants.Acceptance.BIG, InhabitantRules.decide(big, InhabitantRules.Settings.DEFAULT), "a profile length makes it a big animal");
    }

    /** The turtles of other mods: cool nests males, warm ones females, half at the pivot (question I1). */
    @Test
    void temperatureDecidesTheSex() {
        assertEquals(0.5, EggSex.femaleChance(33.5, 33.5), 1e-9);
        assertTrue(EggSex.femaleChance(33.5, 30.5) < 0.01, "3 K cooler: males");
        assertTrue(EggSex.femaleChance(33.5, 36.0) > 0.98, "warm: females");
    }

    // ------------------------------------------------------------------ ids in the jars

    /** Every jar of the dev runtime that might hold a mod (the Gradle cache of maven.modrinth, the run/*-mods folders). */
    private static List<Path> jars() throws IOException {
        List<Path> roots = new ArrayList<>();
        String gh = System.getenv("GRADLE_USER_HOME");
        roots.add((gh != null ? Path.of(gh) : Path.of(System.getProperty("user.home"), ".gradle")).resolve("caches/modules-2/files-2.1/maven.modrinth"));
        Path run = project().resolve("run");
        if (Files.isDirectory(run)) {
            try (Stream<Path> s = Files.list(run)) {
                s.filter(p -> p.getFileName().toString().endsWith("-mods") && Files.isDirectory(p)).forEach(roots::add);
            }
        }
        List<Path> out = new ArrayList<>();
        for (Path r : roots) {
            if (!Files.isDirectory(r)) continue;
            try (Stream<Path> s = Files.walk(r)) {
                s.filter(p -> p.toString().endsWith(".jar") && !p.toString().endsWith("-sources.jar")).forEach(out::add);
            }
        }
        return out;
    }

    private static final Pattern MOD_ID = Pattern.compile("modId\\s*=\\s*\"([a-z0-9_]+)\"");

    /** The translation keys of the en_us.json of each mod found (by mod id). */
    private static Map<String, Set<String>> langKeys() throws IOException {
        Map<String, Set<String>> out = new HashMap<>();
        for (Path j : jars()) {
            try (ZipFile z = new ZipFile(j.toFile())) {
                ZipEntry toml = z.getEntry("META-INF/neoforge.mods.toml");
                if (toml == null) continue;
                String t;
                try (InputStream in = z.getInputStream(toml)) {
                    t = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
                Matcher m = MOD_ID.matcher(t);
                while (m.find()) {
                    String ns = m.group(1);
                    ZipEntry lang = z.getEntry("assets/" + ns + "/lang/en_us.json");
                    if (lang == null) continue;
                    try (InputStream in = z.getInputStream(lang)) {
                        JsonObject o = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                        out.computeIfAbsent(ns, k -> new TreeSet<>()).addAll(o.keySet());
                    }
                }
            } catch (IOException | RuntimeException ex) {
                // not a mod jar we can read
            }
        }
        return out;
    }

    /** The entity ids of every shipped profile and the item ids of other mods in our food tags exist in their mods' jars. */
    @Test
    void idsExistInTheJars() throws IOException {
        Map<String, Set<String>> lang = langKeys();
        Map<String, String> want = new LinkedHashMap<>(); // translation key -> where it comes from
        Path dir = project().resolve("src/main/resources/data/bettacraft/bettacraft/foreign_animals");
        try (Stream<Path> s = Files.list(dir)) {
            for (Path f : s.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                JsonObject o = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
                JsonElement e = o.has("entity") ? o.get("entity") : o.get("entities");
                List<String> ids = new ArrayList<>();
                if (e.isJsonArray()) for (JsonElement x : e.getAsJsonArray()) ids.add(x.getAsString());
                else ids.add(e.getAsString());
                for (String id : ids) want.put("entity." + id.replace(':', '.'), f.getFileName().toString());
            }
        }
        Path tags = project().resolve("src/generated/resources/data/bettacraft/tags/item");
        for (String t : List.of("live_food.json", "betta_food.json", "frozen_food.json", "conditioning_food.json", "daphnia_starters.json", "food_groups/flakes.json")) {
            Path f = tags.resolve(t);
            if (!Files.exists(f)) continue;
            for (JsonElement v : JsonParser.parseString(Files.readString(f)).getAsJsonObject().getAsJsonArray("values")) {
                if (!v.isJsonObject()) continue;
                String id = v.getAsJsonObject().get("id").getAsString();
                if (id.startsWith("#")) continue;
                want.put("item." + id.replace(':', '.'), "tag " + t);
            }
        }
        int checked = 0;
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, String> en : want.entrySet()) {
            String ns = en.getKey().split("\\.")[1];
            Set<String> keys = lang.get(ns);
            if (keys == null) continue; // the mod's jar is not on this machine
            checked++;
            if (!keys.contains(en.getKey())) missing.add(en.getKey() + " (" + en.getValue() + ")");
        }
        assertTrue(missing.isEmpty(), "not in the jars: " + missing);
        System.out.println("ForeignAnimalsM39Test: " + checked + " of " + want.size() + " ids checked against the jars of " + lang.keySet());
    }
}
