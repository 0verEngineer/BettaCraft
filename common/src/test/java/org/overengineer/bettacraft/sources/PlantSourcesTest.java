package org.overengineer.bettacraft.sources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;

/**
 * M22.4: the wandering trader (or another hard way) is never the only source of a plant. Every aquarium plant, floating
 * plant and coral, also the ones later milestones add, must come from the net or from fishing somewhere, and with
 * {@code features.aquaticPlants.netCatches} off still from the wild (not only the trader). The info texts (JEI, EMI, the shift
 * tooltip) name the same ways as the data: the net, fishing and the trader exactly when the game has them.
 */
class PlantSourcesTest {
    private static final Set<Source.Kind> TRADER = EnumSet.of(Source.Kind.TRADER_USUAL, Source.Kind.TRADER_RARE);

    /** Every plant target: aquarium plants and corals, the growing floating plants (not the vanilla lily pad). */
    private static Map<String, String> plants() {
        Map<String, String> out = new LinkedHashMap<>();
        for (PlantSpecies p : PlantSpecies.values()) out.put(Target.plant(p), p.id());
        for (FloatingPlantType f : FloatingPlantType.values()) if (f.grows() && !f.plantItem()) out.put(Target.floating(f), f.id());
        return out;
    }

    @Test
    void noPlantOnlyAtTheTrader() {
        List<String> problems = new ArrayList<>();
        for (var e : plants().entrySet()) {
            List<Source> all = Sources.of(e.getKey());
            boolean netOrFishing = all.stream().anyMatch(s -> s.kind() == Source.Kind.NET || s.kind() == Source.Kind.FISHING);
            boolean wildWithoutNetSwitch = all.stream().anyMatch(s -> !TRADER.contains(s.kind()) && !"features.aquaticPlants.netCatches".equals(s.condition()));
            if (!netOrFishing) problems.add(e.getValue() + ": neither the net nor fishing brings it");
            if (!wildWithoutNetSwitch) problems.add(e.getValue() + ": only the trader with features.aquaticPlants.netCatches off");
        }
        assertTrue(problems.isEmpty(), "plants only at the trader (add a net or fishing source to sources/WildTables):\n" + String.join("\n", problems));
    }

    @Test
    void theSixTraderPlantsHaveWildSources() {
        // the plants that were trader-only before M22.4, with the waters the net and the rod find them in
        Map<PlantSpecies, Set<Where>> expected = new LinkedHashMap<>();
        expected.put(PlantSpecies.AMAZON_SWORD, EnumSet.of(Where.JUNGLE, Where.RIVER));
        expected.put(PlantSpecies.ALTERNANTHERA, EnumSet.of(Where.SWAMP, Where.JUNGLE, Where.RIVER));
        expected.put(PlantSpecies.VALLISNERIA, EnumSet.of(Where.RIVER));
        expected.put(PlantSpecies.MONTE_CARLO, EnumSet.of(Where.SWAMP, Where.RIVER));
        expected.put(PlantSpecies.CRYPT_FLAMINGO, EnumSet.of(Where.SWAMP));
        expected.put(PlantSpecies.TIGER_LOTUS, EnumSet.of(Where.JUNGLE, Where.SWAMP));
        for (var e : expected.entrySet()) {
            Set<Where> net = EnumSet.noneOf(Where.class);
            for (Source s : Sources.of(Target.plant(e.getKey()))) if (s.kind() == Source.Kind.NET) net.add(s.where());
            assertEquals(e.getValue(), net, e.getKey() + ": net waters");
            assertTrue(Sources.of(Target.plant(e.getKey())).stream().anyMatch(s -> s.kind() == Source.Kind.FISHING), e.getKey() + ": fishing too");
        }
        // the Flamingo stays rare: below 1 % per swing and per catch
        for (Source s : Sources.of(Target.plant(PlantSpecies.CRYPT_FLAMINGO))) {
            if (s.kind() == Source.Kind.NET || s.kind() == Source.Kind.FISHING) assertTrue(s.chanceMax() < 0.01, "Flamingo rare: " + s);
        }
    }

    @Test
    void netOddsAddUpWithPlants() {
        for (NetCatching.Water w : NetCatching.Water.values()) {
            var t = WildTables.netPlants(w);
            Map<Object, Double> odds = NetCatching.odds(w, false, false, false, 0.3, 0.4, 0.4, 0.2, t);
            double sum = odds.values().stream().mapToDouble(Double::doubleValue).sum();
            assertTrue(sum <= 1 + 1e-9, w + ": chances above 1: " + sum);
            double plants = 0;
            for (var e : odds.entrySet()) if (e.getKey() instanceof PlantSpecies p && !p.marine()) plants += e.getValue();
            assertEquals(t.isEmpty() ? 0 : 0.3 * 0.2, plants, 1e-9, w + ": a plant in 6 % of the swings");
        }
    }

    @Test
    void configListsRoundTrip() {
        for (var t : List.of(WildTables.NET_PLANTS_RIVER, WildTables.NET_PLANTS_JUNGLE, WildTables.NET_PLANTS_SWAMP, WildTables.FISHING_PLANTS_JUNGLE,
                WildTables.FISHING_PLANTS_RIVER, WildTables.FISHING_PLANTS_SWAMP, WildTables.FISHING_PLANTS_MANGROVE)) {
            List<String> list = PlantTables.toList(t);
            for (String s : list) assertTrue(PlantTables.validEntry(s), s);
            var back = PlantTables.fromList(list);
            assertEquals(t.total(), back.total());
            for (var e : t.entries()) assertEquals(t.share(e.value()), back.share(e.value()), 1e-12);
        }
        assertTrue(!PlantTables.validEntry("no_such_plant=3") && !PlantTables.validEntry("amazon_sword") && !PlantTables.validEntry("amazon_sword=-1"));
        assertTrue(PlantTables.validEntry("bettacraft:amazon_sword=7"));
        assertTrue(PlantTables.fromList(List.of()).isEmpty(), "an empty list brings up nothing");
    }

    /**
     * M28: a config list an older version wrote (the default without the new species) counts as the default, so the new
     * plants come up after an update; a changed list does not.
     */
    @Test
    void oldDefaultListsGetTheNewPlants() {
        for (var t : List.of(WildTables.NET_PLANTS_RIVER, WildTables.NET_PLANTS_JUNGLE, WildTables.NET_PLANTS_SWAMP, WildTables.FISHING_PLANTS_JUNGLE,
                WildTables.FISHING_PLANTS_RIVER, WildTables.FISHING_PLANTS_SWAMP)) {
            var old = PlantTables.older(t);
            assertTrue(old.entries().size() < t.entries().size(), "the table has new plants");
            assertTrue(old.entries().stream().noneMatch(e -> e.value().newerThanConfigLists()), "older: without them");
            // written by the old version and read back, in any order
            List<String> list = new ArrayList<>(PlantTables.toList(old));
            java.util.Collections.reverse(list);
            assertTrue(PlantTables.sameEntries(PlantTables.fromList(list), old), "recognised");
            // one weight changed: the pack's own list
            list.set(0, list.get(0).replaceAll("=\\d+$", "=99"));
            assertTrue(!PlantTables.sameEntries(PlantTables.fromList(list), old), "a changed list stays");
        }
        assertTrue(PlantTables.sameEntries(PlantTables.older(WildTables.FISHING_PLANTS_MANGROVE), WildTables.FISHING_PLANTS_MANGROVE), "unchanged table");
    }

    private static JsonObject lang(String rel) throws IOException {
        Path p = Path.of(System.getProperty("bettacraft.projectDir", "."), rel);
        assertTrue(Files.exists(p), "missing " + p + " (run gradlew runData)");
        return JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    void infoTextsNameTheSameWays() throws IOException {
        JsonObject en = lang("src/generated/resources/assets/bettacraft/lang/en_us.json");
        JsonObject de = lang("src/main/resources/assets/bettacraft/lang/de_de.json");
        List<String> problems = new ArrayList<>();
        for (var e : plants().entrySet()) {
            List<Source> all = Sources.of(e.getKey());
            boolean net = all.stream().anyMatch(s -> s.kind() == Source.Kind.NET);
            boolean fishing = all.stream().anyMatch(s -> s.kind() == Source.Kind.FISHING);
            boolean trader = all.stream().anyMatch(s -> TRADER.contains(s.kind()));
            String key = "info.bettacraft." + e.getValue();
            String tEn = findLine(en, key), tDe = findLine(de, key);
            if (tEn.isEmpty() || tDe.isEmpty()) problems.add(key + ": no line on where to find it");
            check(problems, key + " (en)", tEn, "aquarium net", net);
            check(problems, key + " (en)", tEn, "fishing", fishing);
            check(problems, key + " (en)", tEn, "wandering trader", trader);
            check(problems, key + " (de)", tDe, "Kescher", net);
            check(problems, key + " (de)", tDe, "Angeln", fishing);
            check(problems, key + " (de)", tDe, "Händler", trader);
        }
        assertTrue(problems.isEmpty(), "info texts and sources differ:\n" + String.join("\n", problems));
    }

    /** The line of an info text that says where to find the plant ("Find it:", "Frag it ...", "Bezug:", "Finden:", "Mit der Schere Ableger ..."). */
    private static String findLine(JsonObject lang, String key) {
        if (!lang.has(key)) return "";
        for (String line : lang.get(key).getAsString().split("\n")) {
            for (String start : List.of("Find it", "Frag it", "Bezug", "Finden", "Mit der Schere Ableger")) if (line.startsWith(start)) return line;
        }
        return "";
    }

    private static void check(List<String> problems, String what, String text, String word, boolean has) {
        boolean says = text.toLowerCase(java.util.Locale.ROOT).contains(word.toLowerCase(java.util.Locale.ROOT));
        if (says != has) problems.add(what + (has ? " does not mention " : " mentions ") + "\"" + word + "\"");
    }
}
