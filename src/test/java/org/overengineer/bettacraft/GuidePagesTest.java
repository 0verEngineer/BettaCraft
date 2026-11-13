package org.overengineer.bettacraft;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The GuideME handbook (M6): every English page has a German counterpart (and vice versa), links and images resolve,
 * navigation parents exist, every bettacraft id names a real item/block/entity, every Bettacraft item belongs to exactly
 * one page (item_ids) per language.
 */
class GuidePagesTest {
    private static final Pattern LINK = Pattern.compile("(!?)\\[[^\\]]*\\]\\(([^)\\s]+)\\)");
    private static final Pattern ID_ATTR = Pattern.compile("\\b(?:id|icon)\\s*[=:]\\s*\"?([a-z0-9_.-]+:[a-z0-9_/.-]+)\"?");
    private static final Pattern PARENT = Pattern.compile("(?m)^\\s+parent:\\s*(\\S+)\\s*$");
    private static final Pattern TITLE = Pattern.compile("(?m)^\\s+title:\\s*\\S");
    private static final Pattern ITEM_ID = Pattern.compile("(?m)^\\s+-\\s*([a-z0-9_]+:[a-z0-9_/]+)\\s*$");

    private static Path root() {
        return Path.of(System.getProperty("bettacraft.projectDir", ".")).resolve("src/main/resources/assets/bettacraft/guides/bettacraft/guide");
    }

    private static List<String> pages(Path dir, boolean german) throws IOException {
        List<String> out = new ArrayList<>();
        try (Stream<Path> s = Files.walk(dir)) {
            s.filter(p -> p.toString().endsWith(".md")).forEach(p -> {
                String rel = dir.relativize(p).toString().replace('\\', '/');
                if (german || !rel.startsWith("_")) out.add(rel);
            });
        }
        return out;
    }

    private static String frontmatter(String text) {
        if (!text.startsWith("---")) return "";
        int end = text.indexOf("\n---", 3);
        return end < 0 ? "" : text.substring(3, end);
    }

    /** Normalised path of {@code link} relative to the page {@code page} (both guide-root relative, without language). */
    private static String resolve(String page, String link) {
        int hash = link.indexOf('#');
        if (hash >= 0) link = link.substring(0, hash);
        List<String> parts = new ArrayList<>(List.of(page.split("/")));
        parts.removeLast();
        for (String s : link.split("/")) {
            if (s.equals("..")) {
                if (parts.isEmpty()) return null;
                parts.removeLast();
            } else if (!s.equals(".") && !s.isEmpty()) {
                parts.add(s);
            }
        }
        return String.join("/", parts);
    }

    private static TreeSet<String> knownIds() throws IOException {
        Path en = Path.of(System.getProperty("bettacraft.projectDir", ".")).resolve("src/generated/resources/assets/bettacraft/lang/en_us.json");
        JsonObject o = JsonParser.parseString(Files.readString(en, StandardCharsets.UTF_8)).getAsJsonObject();
        TreeSet<String> ids = new TreeSet<>();
        for (String k : o.keySet()) {
            for (String prefix : new String[]{"item.bettacraft.", "block.bettacraft.", "entity.bettacraft."}) {
                if (k.startsWith(prefix) && k.indexOf('.', prefix.length()) < 0) ids.add("bettacraft:" + k.substring(prefix.length()));
            }
        }
        return ids;
    }

    @Test
    void everyPageExistsInBothLanguages() throws IOException {
        Path root = root();
        assertTrue(Files.isDirectory(root), "missing guide folder " + root);
        TreeSet<String> en = new TreeSet<>(pages(root, false));
        TreeSet<String> de = new TreeSet<>(pages(root.resolve("_de_de"), true));
        assertTrue(en.contains("index.md"), "no index.md");
        TreeSet<String> missing = new TreeSet<>(en);
        missing.removeAll(de);
        TreeSet<String> extra = new TreeSet<>(de);
        extra.removeAll(en);
        assertTrue(missing.isEmpty(), "German pages missing: " + missing);
        assertTrue(extra.isEmpty(), "German pages without English page: " + extra);
        assertTrue(Files.exists(Path.of(System.getProperty("bettacraft.projectDir", "."))
                .resolve("src/main/resources/assets/bettacraft/guideme_guides/guide.json")), "guide.json missing");
    }

    @Test
    void linksImagesParentsAndIdsResolve() throws IOException {
        Path root = root();
        TreeSet<String> en = new TreeSet<>(pages(root, false));
        TreeSet<String> ids = knownIds();
        List<String> problems = new ArrayList<>();
        for (String lang : new String[]{"", "_de_de/"}) {
            Map<String, String> itemPage = new HashMap<>();
            for (String page : en) {
                Path file = root.resolve(lang + page);
                String text = Files.readString(file, StandardCharsets.UTF_8).replace("\r\n", "\n");
                String where = lang + page;
                String fm = frontmatter(text);
                if (!TITLE.matcher(fm).find()) problems.add(where + ": navigation title missing");
                Matcher pm = PARENT.matcher(fm);
                if (pm.find() && !en.contains(pm.group(1))) problems.add(where + ": unknown parent " + pm.group(1));
                if (!page.equals("index.md") && !pm.reset().find()) problems.add(where + ": no parent");
                for (Matcher m = ITEM_ID.matcher(fm); m.find(); ) {
                    String id = m.group(1);
                    String prev = itemPage.put(id, page);
                    if (prev != null) problems.add(where + ": item " + id + " also on " + prev);
                    if (id.startsWith("bettacraft:") && !ids.contains(id)) problems.add(where + ": unknown item_id " + id);
                }
                for (Matcher m = LINK.matcher(text); m.find(); ) {
                    String target = m.group(2);
                    if (target.startsWith("http://") || target.startsWith("https://")) continue;
                    String r = resolve(page, target);
                    if (r == null) {
                        problems.add(where + ": link leaves the guide: " + target);
                    } else if (m.group(1).equals("!")) {
                        if (!Files.exists(root.resolve(r)) && !Files.exists(root.resolve("_de_de/" + r))) {
                            problems.add(where + ": missing image " + target);
                        }
                    } else if (!en.contains(r)) {
                        problems.add(where + ": broken link " + target);
                    }
                }
                for (Matcher m = ID_ATTR.matcher(text); m.find(); ) {
                    String id = m.group(1);
                    if (id.startsWith("bettacraft:") && !ids.contains(id)) problems.add(where + ": unknown id " + id);
                }
            }
            // every Bettacraft item has a page (hold the guide key over it)
            for (String id : ids) {
                if (id.startsWith("bettacraft:") && isPlayerItem(id) && !itemPage.containsKey(id)) {
                    problems.add((lang.isEmpty() ? "en" : "de") + ": no page lists " + id + " in item_ids");
                }
            }
        }
        assertTrue(problems.isEmpty(), "guide problems:\n" + String.join("\n", problems));
    }

    /** Items a player can hold (not technical blocks, not the guide itself). */
    private static boolean isPlayerItem(String id) {
        String p = id.substring(id.indexOf(':') + 1);
        // M19: the tankmate entities (their buckets and spawn eggs are items with pages)
        if (org.overengineer.bettacraft.tankmates.TankmateSpecies.byId(p) != null) return false;
        return !(p.equals("betta") || p.equals("dwarf_clawed_frog") || p.equals("guide") || p.startsWith("interior_") || p.endsWith("_segment"));
    }
}
