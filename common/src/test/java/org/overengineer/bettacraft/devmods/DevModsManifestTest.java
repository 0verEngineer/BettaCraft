package org.overengineer.bettacraft.devmods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * M59: the optional dev mod groups come from the manifest gradle/dev-mods.json (gradle/dev-mods.gradle downloads them from their
 * official source and checks the hashes). Every group folder the loader builds read (NeoForge extraMods, the Fabric flag
 * map) must be in the manifest of this branch or listed there as a gap, every entry needs an official source and its hashes,
 * and one file name never stands for two different files of a loader. A new test mod therefore cannot be used without its manifest entry.
 */
class DevModsManifestTest {
    private static final Set<String> NOT_MANAGED = Set.of("local-mods", "nested-mods");
    private static final Pattern MAVEN = Pattern.compile(
            "maven\\.modrinth:[a-z0-9_.-]+:[A-Za-z0-9]{8}|curse\\.maven:[A-Za-z0-9_-]+-\\d+:\\d+|dev\\.ftb\\.mods:[a-z0-9-]+:[A-Za-z0-9.+_-]+");
    private static final Pattern HEX40 = Pattern.compile("[0-9a-f]{40}");
    private static final Pattern HEX64 = Pattern.compile("[0-9a-f]{64}");

    private static Path root() {
        return Path.of(System.getProperty("bettacraft.rootDir", ".."));
    }

    private static String read(String rel) throws IOException {
        return Files.readString(root().resolve(rel), StandardCharsets.UTF_8);
    }

    private static JsonObject manifest() throws IOException {
        return JsonParser.parseString(read("gradle/dev-mods.json")).getAsJsonObject();
    }

    private static Map<String, List<JsonObject>> entries(JsonObject m) {
        Map<String, List<JsonObject>> out = new HashMap<>();
        for (String loader : List.of("neoforge", "fabric")) {
            JsonObject groups = m.has(loader) ? m.getAsJsonObject(loader) : new JsonObject();
            for (Map.Entry<String, JsonElement> g : groups.entrySet()) {
                List<JsonObject> list = new ArrayList<>();
                for (JsonElement e : g.getValue().getAsJsonArray()) list.add(e.getAsJsonObject());
                out.put(loader + "/" + g.getKey(), list);
            }
        }
        List<JsonObject> packs = new ArrayList<>();
        if (m.has("shaderpacks")) for (JsonElement e : m.getAsJsonArray("shaderpacks")) packs.add(e.getAsJsonObject());
        out.put("shaderpacks", packs);
        return out;
    }

    /** The group folders a loader build reads: "<group>-mods" in quotes after a slash (NeoForge) or as a value of the Fabric flag map. */
    private static Set<String> groupsRead(String build, boolean fabric) {
        Set<String> out = new TreeSet<>();
        Matcher m = (fabric ? Pattern.compile("\\w+: '([a-z]+-mods)'") : Pattern.compile("/([a-z]+-mods)['\"]")).matcher(build);
        while (m.find()) if (!NOT_MANAGED.contains(m.group(1))) out.add(m.group(1));
        return out;
    }

    @Test
    void theManifestIsForThisMinecraftVersion() throws IOException {
        String mc = read("gradle.properties").lines().filter(l -> l.startsWith("minecraft_version=")).findFirst().orElseThrow().substring(18).trim();
        assertEquals(mc, manifest().get("minecraft").getAsString(), "gradle/dev-mods.json is the manifest of another Minecraft version");
    }

    @Test
    void everyEntryHasAnOfficialSourceAndItsHashes() throws IOException {
        List<String> problems = new ArrayList<>();
        int n = 0;
        for (Map.Entry<String, List<JsonObject>> g : entries(manifest()).entrySet()) {
            Set<String> names = new TreeSet<>();
            for (JsonObject e : g.getValue()) {
                n++;
                String file = e.has("file") ? e.get("file").getAsString() : "";
                String what = g.getKey() + "/" + file;
                if (!(g.getKey().equals("shaderpacks") ? file.endsWith(".zip") : file.endsWith(".jar"))) problems.add(what + ": file name");
                if (!names.add(file)) problems.add(what + ": twice in its group");
                if (!e.has("maven") || !MAVEN.matcher(e.get("maven").getAsString()).matches()) problems.add(what + ": no Modrinth / CurseMaven / FTB Maven coordinate");
                if (!e.has("sha1") || !HEX40.matcher(e.get("sha1").getAsString()).matches()) problems.add(what + ": SHA-1");
                if (!e.has("sha256") || !HEX64.matcher(e.get("sha256").getAsString()).matches()) problems.add(what + ": SHA-256");
                if (!e.has("size") || e.get("size").getAsLong() <= 0) problems.add(what + ": size");
            }
        }
        assertTrue(n > 0, "gradle/dev-mods.json lists no file");
        assertTrue(problems.isEmpty(), "gradle/dev-mods.json: " + problems);
    }

    @Test
    void oneFileNameIsOneFile() throws IOException {
        // per loader: the run folders of NeoForge and Fabric are apart (Cloth Config names both of its builds the same way)
        Map<String, String> hashOf = new HashMap<>();
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, List<JsonObject>> g : entries(manifest()).entrySet()) {
            String loader = g.getKey().contains("/") ? g.getKey().substring(0, g.getKey().indexOf('/')) : g.getKey();
            for (JsonObject e : g.getValue()) {
                String file = e.get("file").getAsString(), sha = e.get("sha256").getAsString();
                String before = hashOf.putIfAbsent(loader + "/" + file, sha);
                if (before != null && !before.equals(sha)) problems.add(file + " (" + g.getKey() + ")");
            }
        }
        assertTrue(problems.isEmpty(), "the same file name with different contents: " + problems);
    }

    @Test
    void everyGroupTheBuildsReadIsInTheManifest() throws IOException {
        JsonObject m = manifest();
        String script = read("gradle/dev-mods.gradle");
        List<String> problems = new ArrayList<>();
        for (String loader : List.of("neoforge", "fabric")) {
            Set<String> read = groupsRead(read(loader + "/build.gradle"), loader.equals("fabric"));
            assertTrue(!read.isEmpty(), loader + "/build.gradle reads no group folder (pattern out of date?)");
            JsonObject groups = m.has(loader) ? m.getAsJsonObject(loader) : new JsonObject();
            JsonObject gaps = m.has("gaps") && m.getAsJsonObject("gaps").has(loader) ? m.getAsJsonObject("gaps").getAsJsonObject(loader) : new JsonObject();
            for (String g : read) {
                boolean listed = groups.has(g) && !groups.getAsJsonArray(g).isEmpty();
                boolean gap = gaps.has(g) && !gaps.get(g).getAsString().isBlank();
                if (!listed && !gap) problems.add(loader + "/" + g + ": read by the build, not in gradle/dev-mods.json (nor a gap with its reason)");
                if (!script.contains("'" + g + "'")) problems.add(loader + "/" + g + ": no flag in gradle/dev-mods.gradle");
            }
            for (String g : groups.keySet()) if (!read.contains(g)) problems.add(loader + "/" + g + ": in gradle/dev-mods.json, but no build reads it");
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void theOldCiListsAreGone() {
        for (String f : List.of("tools/ci/power-mods.json", "tools/ci/integration-mods.json", "tools/ci/fetch_mods.py")) {
            assertTrue(!Files.exists(root().resolve(f)), f + ": the CI uses gradle/dev-mods.json since M59");
        }
        // M69: the manifest moved next to gradle/dev-mods.gradle (as in Fast Craft Book)
        assertTrue(!Files.exists(root().resolve("dev-mods.json")), "dev-mods.json: the manifest is gradle/dev-mods.json since M69");
    }
}
