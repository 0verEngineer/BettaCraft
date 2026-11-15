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
        // M21.5: the guide is registered from code (GuideMEClientCompat), a data-driven guide.json would clash with it
        assertTrue(!Files.exists(Path.of(System.getProperty("bettacraft.projectDir", "."))
                .resolve("src/main/resources/assets/bettacraft/guideme_guides/guide.json")), "guide.json must not exist any more");
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
                // M22.2: pages without a parent are the chapters: top-level pages only
                if (!pm.reset().find() && page.contains("/")) problems.add(where + ": no parent (only top-level chapter pages may be roots)");
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
                    // M21.7: <Recipe id="..."> names a recipe (a generated recipe file), not an item
                    if (text.lastIndexOf("<Recipe ", m.start()) >= 0 && text.lastIndexOf("<Recipe ", m.start()) > text.lastIndexOf('>', m.start())) {
                        Path recipe = Path.of(System.getProperty("bettacraft.projectDir", ".")).resolve("src/generated/resources/data/bettacraft/recipe/"
                                + id.substring(id.indexOf(':') + 1) + ".json");
                        if (!Files.exists(recipe)) problems.add(where + ": unknown recipe " + id);
                        continue;
                    }
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

    private static String body(String text) {
        if (!text.startsWith("---")) return text;
        int end = text.indexOf("\n---", 3);
        return end < 0 ? text : text.substring(end + 4);
    }

    /** M33 part b: the top-level {@code feature:} of a frontmatter. */
    private static String featureOf(String fm) {
        Matcher m = Pattern.compile("(?m)^feature:\\s*(.+?)\\s*$").matcher(fm);
        return m.find() ? m.group(1) : null;
    }

    private static String fmValue(String fm, String key) {
        Matcher m = Pattern.compile("(?m)^\\s+" + key + ":\\s*(.+?)\\s*$").matcher(fm);
        return m.find() ? m.group(1) : null;
    }

    private static final Pattern H2 = Pattern.compile("(?m)^## ");
    private static final Pattern TAG_ID = Pattern.compile("<(ItemLink|ItemImage|RecipesFor|RecipeFor|Recipe)\\s+id=\"([^\"]+)\"");

    /** Sorted multiset of a pattern's group in the body (outside the frontmatter). */
    private static List<String> all(Pattern p, String body, int group) {
        List<String> out = new ArrayList<>();
        for (Matcher m = p.matcher(body); m.find(); ) out.add(group == 0 ? m.group() : m.group(group));
        out.sort(null);
        return out;
    }

    /**
     * M22.2: the navigation is one tree (chapters, sub-chapters, pages) without cycles; every page that has children links
     * every child (a markdown link or {@code <SubPages />}); the German page matches the English one (parent, position,
     * icon, item ids, images, links, item tags, number of sections).
     */
    @Test
    void navigationTreeAndLanguagesMatch() throws IOException {
        Path root = root();
        TreeSet<String> en = new TreeSet<>(pages(root, false));
        List<String> problems = new ArrayList<>();
        Map<String, String> parents = new HashMap<>();
        Map<String, List<String>> children = new HashMap<>();
        for (String page : en) {
            String text = Files.readString(root.resolve(page), StandardCharsets.UTF_8).replace("\n\n", "\n");
            String parent = fmValue(frontmatter(text), "parent");
            if (parent != null) {
                parents.put(page, parent);
                children.computeIfAbsent(parent, k -> new ArrayList<>()).add(page);
            }
        }
        for (String page : en) {
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (String p = page; p != null; p = parents.get(p)) {
                if (!seen.add(p)) {
                    problems.add(page + ": navigation cycle");
                    break;
                }
            }
        }
        for (String lang : new String[]{"", "_de_de/"}) {
            for (var e : children.entrySet()) {
                String text = Files.readString(root.resolve(lang + e.getKey()), StandardCharsets.UTF_8).replace("\n\n", "\n");
                if (text.contains("<SubPages")) continue;
                java.util.Set<String> linked = new java.util.HashSet<>();
                for (Matcher m = LINK.matcher(text); m.find(); ) if (m.group(1).isEmpty()) linked.add(resolve(e.getKey(), m.group(2)));
                for (String c : e.getValue()) if (!linked.contains(c)) problems.add(lang + e.getKey() + ": does not link its sub page " + c + " (link it or use <SubPages />)");
            }
        }
        for (String page : en) {
            String a = Files.readString(root.resolve(page), StandardCharsets.UTF_8).replace("\n\n", "\n");
            Path dePath = root.resolve("_de_de/" + page);
            if (!Files.exists(dePath)) continue;
            String b = Files.readString(dePath, StandardCharsets.UTF_8).replace("\n\n", "\n");
            String fa = frontmatter(a), fb = frontmatter(b);
            for (String key : new String[]{"parent", "position", "icon"}) {
                if (!java.util.Objects.equals(fmValue(fa, key), fmValue(fb, key))) problems.add(page + ": en/de differ in navigation " + key);
            }
            if (!all(ITEM_ID, fa, 1).equals(all(ITEM_ID, fb, 1))) problems.add(page + ": en/de differ in item_ids");
            // M33 part b: the page's feature (the handbook hides it while that feature is off)
            if (!java.util.Objects.equals(featureOf(fa), featureOf(fb))) problems.add(page + ": en/de differ in feature");
            String feature = featureOf(fa);
            if (feature != null && org.overengineer.bettacraft.config.Feature.byPath(feature) == null) problems.add(page + ": unknown feature " + feature);
            String ba = body(a), bb = body(b);
            List<String> la = new ArrayList<>(), lb = new ArrayList<>();
            for (Matcher m = LINK.matcher(ba); m.find(); ) la.add(m.group(1) + resolve(page, m.group(2)));
            for (Matcher m = LINK.matcher(bb); m.find(); ) lb.add(m.group(1) + resolve(page, m.group(2)));
            la.sort(null);
            lb.sort(null);
            if (!new TreeSet<>(la).equals(new TreeSet<>(lb))) {
                TreeSet<String> onlyEn = new TreeSet<>(la), onlyDe = new TreeSet<>(lb);
                onlyEn.removeAll(lb);
                onlyDe.removeAll(la);
                problems.add(page + ": en/de link different pages or images: only en " + onlyEn + ", only de " + onlyDe);
            }
            if (!new TreeSet<>(all(TAG_ID, ba, 2)).equals(new TreeSet<>(all(TAG_ID, bb, 2)))) problems.add(page + ": en/de differ in item tags (ItemLink, ItemImage, Recipe)");
            int ha = all(H2, ba, 0).size(), hb = all(H2, bb, 0).size();
            if (ha != hb) problems.add(page + ": en has " + ha + " sections, de " + hb);
            // a recipe shown twice on one page (hand-written and in the generated Obtaining section)
            for (String body : new String[]{ba, bb}) {
                List<String> rec = new ArrayList<>();
                for (Matcher m = TAG_ID.matcher(body); m.find(); ) if (m.group(1).startsWith("Recipe")) rec.add(m.group(2));
                if (new TreeSet<>(rec).size() != rec.size()) problems.add(page + ": a recipe is shown twice " + rec);
            }
        }
        assertTrue(problems.isEmpty(), "guide navigation / language problems:\n" + String.join("\n", problems));
    }

    /** M22.2: every animal and plant of the mod has its own page (the catalogue of the handbook generator). */
    @Test
    void everySpeciesHasItsPage() throws IOException {
        Path root = root();
        List<String> problems = new ArrayList<>();
        for (boolean german : new boolean[]{false, true}) {
            var gen = new org.overengineer.bettacraft.guide.GuideGen(german);
            for (String target : org.overengineer.bettacraft.guide.GuideGen.speciesTargets().keySet()) {
                String page = gen.page(target);
                if (page == null || !Files.exists(root.resolve((german ? "_de_de/" : "") + page))) {
                    problems.add((german ? "de" : "en") + ": " + target + " has no page " + page);
                }
            }
            // every source of the game data leads to a page
            for (var s : org.overengineer.bettacraft.sources.Sources.all()) {
                if (gen.page(s.target()) == null) problems.add((german ? "de" : "en") + ": source of " + s.target() + " has no page");
            }
        }
        assertTrue(problems.isEmpty(), "missing pages:\n" + String.join("\n", problems));
    }

    /**
     * M26.1: GuideME reads the pages as MDX, where a "<" that does not open a tag (e.g. a button label "**<**") makes the whole
     * page fail to load. Outside code spans it must be written as "\<".
     */
    @Test
    void noBareAngleBrackets() throws IOException {
        Pattern bare = Pattern.compile("(?<!\\\\)<(?![A-Za-z/!])");
        List<String> problems = new ArrayList<>();
        Path root = root();
        for (boolean german : new boolean[]{false, true}) {
            Path dir = german ? root.resolve("_de_de") : root;
            for (String page : pages(dir, german)) {
                List<String> lines = Files.readAllLines(dir.resolve(page), StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    if (bare.matcher(lines.get(i).replaceAll("`[^`]*`", "")).find()) {
                        problems.add((german ? "_de_de/" : "") + page + ":" + (i + 1) + ": " + lines.get(i).strip());
                    }
                }
            }
        }
        assertTrue(problems.isEmpty(), "bare \"<\" in handbook pages (write \\<):\n" + String.join("\n", problems));
    }

    /** Items a player can hold (not technical blocks, not the guide itself). */
    private static boolean isPlayerItem(String id) {
        String p = id.substring(id.indexOf(':') + 1);
        // M19: the tankmate entities (their buckets and spawn eggs are items with pages)
        if (org.overengineer.bettacraft.tankmates.TankmateSpecies.byId(p) != null) return false;
        // M28.1: the wild plant blocks of the world have no item (shears give the plant, silk touch the plant too)
        if (p.equals("wild_floating_plant") || java.util.Arrays.stream(org.overengineer.bettacraft.plants.wild.WildPlantKind.values()).anyMatch(k -> k.id().equals(p))) {
            return false;
        }
        // M31: the terrarium animals are entities (their buckets and eggs are the items with pages)
        if (org.overengineer.bettacraft.terrarium.HerpSpecies.byId(p) != null) return false;
        if (p.equals("turtle_eggs")) return false; // M31 part b: eggs only in the interior, no item
        return !(p.equals("betta") || p.equals("dwarf_clawed_frog") || p.equals("wild_critter") || p.equals("guide") || p.startsWith("interior_") || p.endsWith("_segment"));
    }
}
