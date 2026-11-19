package org.overengineer.bettacraft.guidetool;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.overengineer.bettacraft.guidetool.PatchouliConverter.Page;

/**
 * M52b: converts the GuideME handbook (the one source: {@code guides/bettacraft/guide/*.md}, en and {@code _de_de}) into a
 * Modonomicon book for Fabric 1.21.11 (no GuideME and no Patchouli there). The book is in index mode: the start page shows
 * the welcome text and the chapters; every top-level page is a category (an index of its pages, the chapter's own page
 * first, the sub-pages in navigation order); every page is an entry. Texts are language keys (merged into the mod's
 * {@code en_us.json} / {@code de_de.json}), written in Modonomicon's Markdown and split to fit a page; images become square
 * textures; GuideME scenes become multiblock pages (blocks) or entity pages (animals); recipes crafting / smelting ...
 * pages, recipes Modonomicon cannot draw a spotlight of the item.
 *
 * <p>Usage: {@code ModonomiconConverter <guide dir> <en lang json> <de lang json> <output resources dir> <recipe dir>...};
 * the build fails when a GuideME page has no entry, a link or image cannot be resolved, or a text key is missing in a language.
 */
public final class ModonomiconConverter {
    public static final String NS = "bettacraft";
    public static final String BOOK = "handbook";
    static final String KEY = "book." + NS + "." + BOOK + ".";
    /** Visible characters per text page (124 x 155 px); the first page of an entry has the title too. */
    static final int PAGE = 320;
    static final int FIRST_PAGE = 260;
    /**
     * GuideME scenes as Modonomicon multiblock / entity pages. Off: Modonomicon 1.21.11-1.121.0 draws them away from their
     * frame (its picture-in-picture bounds are offset at the usual GUI scales), so a scene becomes a spotlight of its block
     * (or of the animal's spawn egg) instead. The multiblock data is still written when this is on.
     */
    static final boolean SCENE_PAGES = false;
    /** Items that exist only with another mod (an item link to them would not resolve): their name in bold. */
    static final Set<String> OPTIONAL_ITEMS = Set.of("bettacraft:drive_adapter");
    /** Vanilla blocks of the scenes that have no item (a spotlight cannot show them). */
    static final Set<String> NO_ITEM = Set.of("minecraft:water", "minecraft:lava", "minecraft:air", "minecraft:bubble_column", "minecraft:fire");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path guide;
    private final Path enLang;
    private final Path deLang;
    private final List<Path> recipeDirs;
    private final Map<String, JsonObject> recipes = new TreeMap<>();
    /** item id -> entry id (the page that lists the item in item_ids) */
    private final Map<String, String> itemEntry = new HashMap<>();
    private Map<String, Page> pages;
    private final Map<String, List<String>> children = new HashMap<>();
    /** entry id -> its sort number in its category (depth-first navigation order) */
    private final Map<String, Integer> order = new HashMap<>();
    public final List<String> warnings = new ArrayList<>();
    public int images;
    public int multiblocks;
    private final Set<String> itemModels = new HashSet<>();

    public ModonomiconConverter(Path guide, Path enLang, Path deLang, List<Path> recipeDirs) throws IOException {
        Path models = enLang.getParent().getParent().resolve("models/item");
        if (Files.isDirectory(models)) {
            try (var st = Files.list(models)) {
                st.forEach(f -> itemModels.add(f.getFileName().toString().replace(".json", "")));
            }
        }
        this.guide = guide;
        this.enLang = enLang;
        this.deLang = deLang;
        this.recipeDirs = recipeDirs;
    }

    public static void main(String[] args) throws IOException {
        List<Path> rec = new ArrayList<>();
        for (int i = 4; i < args.length; i++) rec.add(Path.of(args[i]));
        ModonomiconConverter c = new ModonomiconConverter(Path.of(args[0]), Path.of(args[1]), Path.of(args[2]), rec);
        int n = c.convert(Path.of(args[3]));
        System.out.println("ModonomiconConverter: " + n + " entries, " + c.images + " images, " + c.multiblocks + " multiblocks, "
                + c.warnings.size() + " warning(s)");
        for (String w : c.warnings) System.out.println("  " + w);
        long count;
        try (var s = Files.walk(Path.of(args[0]))) {
            count = s.filter(p -> p.toString().endsWith(".md") && !Path.of(args[0]).relativize(p).toString().startsWith("_")).count();
        }
        if (n != count || !c.warnings.isEmpty()) {
            System.err.println("ModonomiconConverter: coverage failed: " + n + " entries for " + count + " GuideME pages, "
                    + c.warnings.size() + " warning(s)");
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ book

    /** Writes the book into {@code out} (a resources root); returns the number of entries. */
    public int convert(Path out) throws IOException {
        pages = PatchouliConverter.readPages(guide);
        Map<String, Page> dePages = Files.isDirectory(guide.resolve("_de_de")) ? PatchouliConverter.readPages(guide.resolve("_de_de")) : Map.of();
        for (Path d : recipeDirs) readRecipes(d);
        for (Page p : pages.values()) {
            for (String id : p.itemIds()) itemEntry.putIfAbsent(id, PatchouliConverter.entryId(p.path()));
            if (p.parent() != null) children.computeIfAbsent(p.parent(), k -> new ArrayList<>()).add(p.path());
        }
        children.values().forEach(l -> l.sort(Comparator.comparingInt((String c) -> pages.get(c).position()).thenComparing(c -> c)));
        for (Page p : pages.values()) if (p.parent() == null) number(p.path(), new int[]{0});
        // M52c: Modonomicon 2.x (26.1.2) links are <category>/<entry>; the entry ids keep the rest of the path in one segment
        for (Page p : pages.values()) {
            String eid = PatchouliConverter.entryId(p.path()), top = topOf(p.path());
            String rest = eid.equals(top) ? top : eid.startsWith(top + "/") ? eid.substring(top.length() + 1) : eid;
            links.put(eid, top + "/" + rest.replace('/', '.'));
        }

        Map<String, String> en = lang(enLang), de = lang(deLang);
        Map<String, String> enKeys = new TreeMap<>(), deKeys = new TreeMap<>();
        Map<String, Page> dePageMap = merged(dePages);
        dePageMap.forEach((k, v) -> deTitles.put(k, v.title()));
        Path data = out.resolve("data/" + NS + "/modonomicon/books/" + BOOK);
        // one structure (from the English pages), the texts per language
        Map<String, JsonObject> entryJson = new TreeMap<>();
        for (Page p : pages.values()) {
            Lang lEn = new Lang(en, enKeys), lDe = new Lang(fallback(de, en), deKeys);
            String id = PatchouliConverter.entryId(p.path());
            List<Item> itEn = new EntryBuilder(p, lEn, out, true).build();
            List<Item> itDe = new EntryBuilder(dePageMap.get(p.path()), lDe, out, false).build();
            JsonArray pg = pages(p.path(), itEn, itDe, enKeys, deKeys);
            // M52c: Modonomicon 2.x (26.1.2): every page has an id (its anchor) and a title (empty: none)
            for (int n = 0; n < pg.size(); n++) {
                JsonObject po = pg.get(n).getAsJsonObject();
                if (!po.has("id")) po.addProperty("id", "p" + n);
                if (!po.has("title")) po.addProperty("title", "");
                if (!po.has("text") && !po.get("type").getAsString().endsWith("_recipe")) po.addProperty("text", "");
            }
            JsonObject e = new JsonObject();
            // M52c: Modonomicon 2.x (26.1.2) dispatches entries by their type
            e.addProperty("type", "modonomicon:content");
            e.addProperty("id", NS + ":" + link(id));
            e.addProperty("category", NS + ":" + topOf(p.path()));
            e.addProperty("name", key(id, "name"));
            enKeys.put(key(id, "name"), p.title());
            deKeys.put(key(id, "name"), dePageMap.get(p.path()).title());
            e.addProperty("description", key(id, "description"));
            enKeys.put(key(id, "description"), firstSentence(p.body(), lEn, p.path()));
            deKeys.put(key(id, "description"), firstSentence(dePageMap.get(p.path()).body(), lDe, p.path()));
            e.addProperty("icon", icon(p.icon()));
            e.addProperty("x", 0);
            e.addProperty("y", 0);
            e.addProperty("sort_number", order.getOrDefault(p.path(), 0));
            e.add("pages", pg);
            entryJson.put(id, e);
        }
        for (var e : entryJson.entrySet()) write(data.resolve("entries/" + e.getKey() + ".json"), e.getValue());
        // categories: the top-level pages
        for (Page p : pages.values()) {
            if (p.parent() != null) continue;
            String id = PatchouliConverter.entryId(p.path());
            JsonObject c = new JsonObject();
            c.addProperty("name", key(id, "category"));
            enKeys.put(key(id, "category"), p.title());
            deKeys.put(key(id, "category"), dePageMap.get(p.path()).title());
            c.addProperty("description", key(id, "description"));
            c.addProperty("icon", icon(p.icon()));
            c.addProperty("sort_number", p.position());
            c.addProperty("display_mode", "index");
            c.addProperty("show_category_button", true);
            write(data.resolve("categories/" + id + ".json"), c);
        }
        writeBook(data, enKeys, deKeys, dePageMap);
        // the texts: the mod's language files with the book's keys added (the build puts these first)
        writeLang(out.resolve("assets/" + NS + "/lang/en_us.json"), enLang, enKeys);
        writeLang(out.resolve("assets/" + NS + "/lang/de_de.json"), deLang, deKeys);
        for (String k : enKeys.keySet()) if (!deKeys.containsKey(k)) warnings.add("no German text for " + k);
        for (String k : deKeys.keySet()) if (!enKeys.containsKey(k)) warnings.add("no English text for " + k);
        return entryJson.size();
    }

    private static Map<String, String> lang(Path p) throws IOException {
        Map<String, String> m = new HashMap<>();
        if (!Files.exists(p)) return m;
        JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
        for (var e : o.entrySet()) if (e.getValue().isJsonPrimitive()) m.put(e.getKey(), e.getValue().getAsString());
        return m;
    }

    /** Depth-first numbers: the chapter's page first, then each sub-page followed by its own sub-pages. */
    private void number(String path, int[] next) {
        order.put(path, next[0]++);
        for (String c : children.getOrDefault(path, List.of())) number(c, next);
    }

    /** M52c: the entry ids in the form of Modonomicon 2.x (category, then the entry), by the GuideME page id. */
    private final Map<String, String> links = new java.util.HashMap<>();

    private String link(String entryId) {
        return links.getOrDefault(entryId, entryId);
    }

    private String topOf(String path) {
        String p = path;
        while (pages.get(p).parent() != null && pages.containsKey(pages.get(p).parent())) p = pages.get(p).parent();
        return PatchouliConverter.entryId(p);
    }

    private Map<String, Page> merged(Map<String, Page> dePages) {
        Map<String, Page> m = new TreeMap<>();
        for (var e : pages.entrySet()) {
            Page d = dePages.get(e.getKey());
            if (d == null) {
                warnings.add("no German page for " + e.getKey());
                m.put(e.getKey(), e.getValue());
            } else {
                Page en = e.getValue();
                m.put(e.getKey(), new Page(en.path(), d.title(), en.icon(), en.parent(), en.position(), en.itemIds(), d.body()));
            }
        }
        return m;
    }

    private static Map<String, String> fallback(Map<String, String> a, Map<String, String> b) {
        Map<String, String> m = new HashMap<>(b);
        m.putAll(a);
        return m;
    }

    private void writeBook(Path data, Map<String, String> enKeys, Map<String, String> deKeys, Map<String, Page> dePages) throws IOException {
        JsonObject book = new JsonObject();
        book.addProperty("name", KEY + "name");
        book.addProperty("tooltip", KEY + "subtitle");
        // the start page: the first sentence of the GuideME start page (index.md) in each language
        book.addProperty("description", KEY + "landing_modonomicon");
        Page index = pages.get("index.md");
        enKeys.put(KEY + "landing_modonomicon", index == null ? "" : firstSentence(index.body(), new Lang(Map.of(), new HashMap<>()), "index.md"));
        deKeys.put(KEY + "landing_modonomicon", index == null ? "" : firstSentence(dePages.get("index.md").body(), new Lang(Map.of(), new HashMap<>()), "index.md"));
        book.addProperty("display_mode", "index");
        book.addProperty("model", "modonomicon:modonomicon_blue");
        book.addProperty("creative_tab", NS + ":main");
        book.addProperty("generate_book_item", true);
        book.addProperty("auto_add_read_conditions", false);
        book.addProperty("allow_open_book_with_invalid_links", false);
        write(data.resolve("book.json"), book);
    }

    private void writeLang(Path dst, Path base, Map<String, String> keys) throws IOException {
        JsonObject o = Files.exists(base) ? JsonParser.parseString(Files.readString(base, StandardCharsets.UTF_8)).getAsJsonObject() : new JsonObject();
        for (var e : keys.entrySet()) {
            if (o.has(e.getKey()) && !e.getKey().startsWith(KEY)) warnings.add("language key " + e.getKey() + " exists already");
            // Modonomicon formats its texts (I18n.get): a literal % is %%
            o.addProperty(e.getKey(), e.getValue().replace("%", "%%"));
        }
        Files.createDirectories(dst.getParent());
        Files.writeString(dst, GSON.toJson(o) + "\n", StandardCharsets.UTF_8);
    }

    static String key(String entryId, String what) {
        return KEY + entryId.replace('/', '.') + "." + what;
    }

    private static String icon(String id) {
        return id == null || id.isBlank() ? "minecraft:book" : id;
    }

    private String firstSentence(String body, Lang lang, String from) {
        for (String line : body.split("\n")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#") || t.startsWith("<") || t.startsWith("!") || t.startsWith("|") || t.startsWith("*") || t.startsWith("-")) continue;
            String plain = plain(inline(t, lang, from));
            int dot = plain.indexOf(". ");
            return dot > 0 && dot < 220 ? plain.substring(0, dot + 1) : plain.length() > 220 ? plain.substring(0, 220) + "…" : plain;
        }
        return "";
    }

    /** The visible text of Modonomicon Markdown (links reduced to their text, emphasis marks dropped). */
    static String plain(String md) {
        return md.replaceAll("\\[([^\\]]*)\\]\\([^)]*\\)", "$1").replace("**", "").replaceAll("(?<!\\w)[*_](\\S)", "$1").replaceAll("(\\S)[*_](?!\\w)", "$1");
    }

    private static void write(Path p, JsonElement json) throws IOException {
        Files.createDirectories(p.getParent());
        Files.writeString(p, GSON.toJson(json) + "\n", StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------ recipes

    private void readRecipes(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return;
        try (Stream<Path> s = Files.walk(dir)) {
            for (Path p : (Iterable<Path>) s.filter(f -> f.toString().endsWith(".json"))::iterator) {
                String id = NS + ":" + dir.relativize(p).toString().replace('\\', '/').replaceAll("\\.json$", "");
                try {
                    recipes.put(id, JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject());
                } catch (RuntimeException ex) {
                    warnings.add("unreadable recipe " + p);
                }
            }
        }
    }

    private static String resultOf(JsonObject r) {
        JsonElement res = r.get("result");
        if (res == null) return null;
        if (res.isJsonPrimitive()) return res.getAsString();
        if (res.isJsonObject() && res.getAsJsonObject().has("id")) return res.getAsJsonObject().get("id").getAsString();
        if (res.isJsonObject() && res.getAsJsonObject().has("item")) return res.getAsJsonObject().get("item").getAsString();
        return null;
    }

    private List<String> recipesFor(String item) {
        List<String> out = new ArrayList<>();
        for (var e : recipes.entrySet()) if (item.equals(resultOf(e.getValue()))) out.add(e.getKey());
        return out;
    }

    private static String pageType(JsonObject r) {
        String t = r.has("type") ? r.get("type").getAsString() : "";
        return switch (t) {
            case "minecraft:crafting_shaped", "minecraft:crafting_shapeless" -> "modonomicon:crafting_recipe";
            case "minecraft:smelting" -> "modonomicon:smelting_recipe";
            case "minecraft:blasting" -> "modonomicon:blasting_recipe";
            case "minecraft:smoking" -> "modonomicon:smoking_recipe";
            case "minecraft:campfire_cooking" -> "modonomicon:campfire_cooking_recipe";
            case "minecraft:stonecutting" -> "modonomicon:stonecutting_recipe";
            case "minecraft:smithing_transform" -> "modonomicon:smithing_recipe";
            default -> null;
        };
    }

    // ------------------------------------------------------------------ names and inline markup

    /** One language: its texts (item names) and the keys written for it. */
    private record Lang(Map<String, String> texts, Map<String, String> keys) {}

    String itemName(String id, Lang lang) {
        String ns = id.contains(":") ? id.substring(0, id.indexOf(':')) : "minecraft";
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        for (String k : new String[]{"item." + ns + "." + path, "block." + ns + "." + path, "entity." + ns + "." + path}) {
            String v = lang.texts().get(k);
            if (v != null) return v;
        }
        StringBuilder b = new StringBuilder();
        for (String w : path.split("_")) {
            if (w.isEmpty()) continue;
            if (b.length() > 0) b.append(' ');
            b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return b.toString();
    }

    private static final Pattern ITEM_LINK = Pattern.compile("<ItemLink\\s+id=\"([^\"]+)\"[^>]*/>");
    private static final Pattern ITEM_IMAGE = Pattern.compile("<ItemImage\\s+id=\"([^\"]+)\"[^>]*/>");
    private static final Pattern LINK = Pattern.compile("(?<!!)\\[([^\\]]+)\\]\\(([^)\\s]+)\\)");

    /** An item: a link to its page when it has one (and it is not this page), else an item link (tooltip; JEI on click). */
    private String item(String id, Lang lang, String from) {
        String name = itemName(id, lang).replace("[", "(").replace("]", ")");
        if (OPTIONAL_ITEMS.contains(id)) return "**" + name + "**";
        String entry = itemEntry.get(id);
        if (entry != null && (from == null || !entry.equals(PatchouliConverter.entryId(from)))) return "[" + name + "](entry://" + link(entry) + ")";
        return "[" + name + "](item://" + id + ")";
    }

    /** Converts one line of GuideME markdown to Modonomicon Markdown (no block elements). */
    String inline(String s, Lang lang, String from) {
        Matcher m = ITEM_LINK.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) m.appendReplacement(sb, Matcher.quoteReplacement(item(m.group(1), lang, from)));
        m.appendTail(sb);
        s = sb.toString();
        m = ITEM_IMAGE.matcher(s);
        sb = new StringBuilder();
        while (m.find()) m.appendReplacement(sb, Matcher.quoteReplacement(item(m.group(1), lang, from)));
        m.appendTail(sb);
        s = sb.toString();
        m = LINK.matcher(s);
        sb = new StringBuilder();
        while (m.find()) {
            String text = m.group(1), url = m.group(2);
            String rep;
            if (url.startsWith("entry://") || url.startsWith("item://")) rep = m.group(0);
            else if (url.startsWith("http")) rep = text;
            else {
                String target = resolve(from, url.replaceAll("#.*$", ""));
                if (target == null && !url.startsWith("#")) warnings.add(from + ": link to a missing page " + url);
                rep = target != null ? "[" + text + "](entry://" + link(PatchouliConverter.entryId(target)) + ")" : text;
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        s = sb.toString();
        s = s.replaceAll("`([^`]+)`", "$1");
        s = s.replaceAll("</?(Row|Small|Version|Spieler|Pfad|Name|br)\\b[^>]*>", "");
        s = s.replaceAll("<[A-Z][A-Za-z]*\\b[^>]*/>", "");
        return s;
    }

    String resolve(String from, String url) {
        if (url.isEmpty()) return null;
        Path base = from == null || !from.contains("/") ? Path.of("") : Path.of(from).getParent();
        String r = base.resolve(url).normalize().toString().replace('\\', '/');
        if (pages.containsKey(r)) return r;
        return pages.containsKey(url) ? url : null;
    }

    // ------------------------------------------------------------------ pages of one entry

    /** One piece of an entry: a text unit (paragraph, heading, list item, table row) or a page of its own. */
    private sealed interface Item permits Unit, Special {}

    private record Unit(String md, boolean newParagraph, boolean listItem) implements Item {}

    /** A page of its own; {@code texts}: what its text keys say ("text", "entity"), filled in per language. */
    private record Special(JsonObject page, Map<String, String> texts) implements Item {}

    /** Reads one language's page into items (phase 1; the paging is done for both languages together). */
    private final class EntryBuilder {
        final Page page;
        final Lang lang;
        final Path out;
        /** Whether the textures / multiblocks are written (once, from the English pages). */
        final boolean writeAssets;
        final String id;
        final List<Item> items = new ArrayList<>();
        int scenes;

        EntryBuilder(Page page, Lang lang, Path out, boolean writeAssets) {
            this.page = page;
            this.lang = lang;
            this.out = out;
            this.writeAssets = writeAssets;
            this.id = PatchouliConverter.entryId(page.path());
        }

        void add(String md, boolean newParagraph, boolean listItem) {
            items.add(new Unit(md, newParagraph, listItem));
        }

        void special(JsonObject p, Map<String, String> texts) {
            items.add(new Special(p, texts));
        }

        List<Item> build() throws IOException {
            String[] lines = page.body().split("\n", -1);
            List<String> para = new ArrayList<>();
            for (int i = 0; i < lines.length; i++) {
                String l = lines[i];
                String t = l.trim();
                if (t.startsWith("# ") && i < 3) continue; // the title is the entry's name
                if (t.isEmpty()) {
                    paragraph(para);
                    continue;
                }
                if (t.startsWith("<GameScene")) {
                    paragraph(para);
                    StringBuilder scene = new StringBuilder();
                    while (i < lines.length && !lines[i].contains("</GameScene>")) scene.append(lines[i++]).append('\n');
                    scene(scene.toString());
                    continue;
                }
                if (t.startsWith("|")) {
                    paragraph(para);
                    List<String> rows = new ArrayList<>();
                    while (i < lines.length && lines[i].trim().startsWith("|")) rows.add(lines[i++].trim());
                    i--;
                    table(rows);
                    continue;
                }
                if (t.startsWith("#")) {
                    paragraph(para);
                    add("**" + inline(t.replaceFirst("^#+\\s*", ""), lang, page.path()) + "**", true, false);
                    continue;
                }
                Matcher img = Pattern.compile("^!\\[([^\\]]*)\\]\\(([^)\\s]+)\\)").matcher(t);
                if (img.find()) {
                    paragraph(para);
                    image(img.group(2), img.group(1));
                    continue;
                }
                Matcher rec = Pattern.compile("<Recipes?For\\s+id=\"([^\"]+)\"").matcher(t);
                if (rec.find()) {
                    paragraph(para);
                    recipes(rec.group(1));
                    continue;
                }
                Matcher one = Pattern.compile("<Recipe\\s+id=\"([^\"]+)\"").matcher(t);
                if (one.find()) {
                    paragraph(para);
                    // a recipe Modonomicon cannot draw (the stone cabinet's): the item it makes
                    if (!recipe(one.group(1))) {
                        JsonObject r = recipes.get(one.group(1));
                        String result = r == null ? null : resultOf(r);
                        if (result == null) warnings.add(page.path() + ": unknown recipe " + one.group(1));
                        else spotlight(result);
                    }
                    continue;
                }
                if (t.startsWith("<SubPages")) {
                    paragraph(para);
                    for (String c : children.getOrDefault(page.path(), List.of())) {
                        String title = writeAssets ? ModonomiconConverter.this.pages.get(c).title() : germanTitle(c);
                        add("- [" + title + "](entry://" + link(PatchouliConverter.entryId(c)) + ")", false, true);
                    }
                    continue;
                }
                if (t.matches("^([-*]|\\d+\\.)\\s+.*")) {
                    paragraph(para);
                    boolean numbered = t.matches("^\\d+\\..*");
                    String item = t.replaceFirst("^([-*]|\\d+\\.)\\s+", "");
                    int depth = (l.length() - l.stripLeading().length()) / 2;
                    while (i + 1 < lines.length && lines[i + 1].matches("^\\s{2,}\\S.*") && !lines[i + 1].trim().matches("^([-*]|\\d+\\.)\\s+.*")) {
                        item += " " + lines[++i].trim();
                    }
                    String bullet = numbered ? t.substring(0, t.indexOf('.') + 1) + " " : "- ";
                    add("  ".repeat(Math.min(depth, 2)) + bullet + inline(item, lang, page.path()), false, true);
                    continue;
                }
                para.add(t);
            }
            paragraph(para);
            return items;
        }

        String germanTitle(String path) {
            return deTitles.getOrDefault(path, ModonomiconConverter.this.pages.get(path).title());
        }

        void paragraph(List<String> para) {
            if (para.isEmpty()) return;
            add(inline(String.join(" ", para), lang, page.path()), true, false);
            para.clear();
        }

        void table(List<String> rows) {
            boolean header = true;
            for (String r : rows) {
                if (r.matches("^[|\\s:-]+$")) continue;
                List<String> cells = new ArrayList<>();
                for (String c : r.replaceAll("^\\|", "").replaceAll("\\|\\s*$", "").split("\\|")) cells.add(inline(c.trim(), lang, page.path()));
                String line = String.join(" · ", cells);
                add(header ? "**" + line + "**" : "- " + line, header, !header);
                header = false;
            }
        }

        void image(String src, String alt) throws IOException {
            Path file = guide.resolve(src).normalize();
            if (!Files.exists(file)) file = guide.resolve(Path.of(page.path()).getParent() == null ? src : Path.of(page.path()).getParent().resolve(src).toString()).normalize();
            if (!Files.exists(file)) {
                warnings.add(page.path() + ": missing image " + src);
                return;
            }
            String name = file.getFileName().toString().replaceAll("\\.png$", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
            Path tex = out.resolve("assets/" + NS + "/textures/gui/handbook/" + name + ".png");
            if (writeAssets && !Files.exists(tex)) {
                writeTexture(file, tex);
                images++;
            }
            JsonObject p = new JsonObject();
            p.addProperty("type", "modonomicon:image");
            JsonArray imgs = new JsonArray();
            imgs.add(NS + ":textures/gui/handbook/" + name + ".png");
            p.add("images", imgs);
            p.addProperty("border", true);
            special(p, alt.isBlank() ? Map.of() : Map.of("text", inline(alt, lang, page.path())));
        }

        void recipes(String item) {
            List<String> ids = recipesFor(item);
            boolean any = false;
            for (String id : ids) any |= recipe(id);
            // a recipe Modonomicon cannot draw (food maker, crucible, stone cabinet ...): the item, its recipes by JEI / REI
            if (!any) spotlight(item);
        }

        void spotlight(String item) {
            JsonObject p = new JsonObject();
            p.addProperty("type", "modonomicon:spotlight");
            JsonObject it = new JsonObject();
            it.addProperty("id", item);
            p.add("item", it);
            special(p, Map.of());
        }

        boolean recipe(String rid) {
            JsonObject r = recipes.get(rid);
            // a recipe of an integration (it needs another mod, e.g. Create): not in the book
            if (r != null && r.has("neoforge:conditions") && r.get("neoforge:conditions").toString().contains("neoforge:mod_loaded")) return true;
            String type = r == null ? null : pageType(r);
            if (type == null) return false;
            JsonObject p = new JsonObject();
            p.addProperty("type", type);
            p.addProperty("recipe_id_1", rid);
            special(p, Map.of());
            return true;
        }

        void scene(String scene) throws IOException {
            int n = scenes++;
            Matcher ent = Pattern.compile("<Entity\\s+id=\"([^\"]+)\"([^>]*)/>").matcher(scene);
            if (!SCENE_PAGES) {
                // the scene's animal (its spawn egg) or its first block that has an item
                String item = null;
                if (ent.find()) {
                    String egg = ent.group(1) + "_spawn_egg";
                    if (itemModels.contains(egg.substring(egg.indexOf(':') + 1))) item = egg;
                } else {
                    Matcher blocks = Pattern.compile("<Block\\s+id=\"([^\"]+)\"").matcher(scene);
                    while (item == null && blocks.find()) {
                        String bl = blocks.group(1);
                        // our blocks with an item model; vanilla blocks except those without an item (water)
                        if (bl.startsWith(NS + ":") ? itemModels.contains(bl.substring(bl.indexOf(':') + 1)) : !NO_ITEM.contains(bl)) item = bl;
                    }
                }
                if (item == null) warnings.add(page.path() + ": a scene without an item to show");
                else spotlight(item);
                return;
            }
            if (ent.find()) {
                JsonObject p = new JsonObject();
                p.addProperty("type", "modonomicon:entity");
                Matcher data = Pattern.compile("data=\"(\\{.*\\})\"").matcher(ent.group(2));
                p.addProperty("entity_id", ent.group(1) + (data.find() ? data.group(1) : ""));
                p.addProperty("scale", 1.6F);
                p.addProperty("offset", 0.2F);
                p.addProperty("rotate", true);
                special(p, Map.of("entity", itemName(ent.group(1), lang)));
                return;
            }
            Matcher b = Pattern.compile("<Block\\s+id=\"([^\"]+)\"([^>]*)/>").matcher(scene);
            Map<String, Character> keys = new LinkedHashMap<>();
            Map<String, Character> grid = new HashMap<>();
            int minX = 99, minY = 99, minZ = 99, maxX = -99, maxY = -99, maxZ = -99;
            while (b.find()) {
                String attrs = b.group(2);
                int x = intAttr(attrs, "x"), y = intAttr(attrs, "y"), z = intAttr(attrs, "z");
                StringBuilder props = new StringBuilder();
                Matcher pm = Pattern.compile("p:(\\w+)=\"([^\"]+)\"").matcher(attrs);
                while (pm.find()) props.append(props.length() == 0 ? "[" : ",").append(pm.group(1)).append('=').append(pm.group(2));
                if (props.length() > 0) props.append(']');
                String state = b.group(1) + props;
                char key = keys.computeIfAbsent(state, k -> (char) ('A' + keys.size()));
                grid.put(x + "," + y + "," + z, key);
                minX = Math.min(minX, x); minY = Math.min(minY, y); minZ = Math.min(minZ, z);
                maxX = Math.max(maxX, x); maxY = Math.max(maxY, y); maxZ = Math.max(maxZ, z);
            }
            if (keys.isEmpty()) {
                warnings.add(page.path() + ": empty scene");
                return;
            }
            // Modonomicon's dense multiblock: pattern[layer from top][z][x]; exactly one '0' marks the centre (the middle of
            // the bottom layer; it keeps its block through the mapping "0"); ' ' is air
            int cx = (minX + maxX) / 2, cz = (minZ + maxZ) / 2;
            String centre = cx + "," + minY + "," + cz;
            Character centreKey = grid.get(centre);
            JsonArray pattern = new JsonArray();
            for (int y = maxY; y >= minY; y--) {
                JsonArray layer = new JsonArray();
                for (int z = minZ; z <= maxZ; z++) {
                    StringBuilder row = new StringBuilder();
                    for (int x = minX; x <= maxX; x++) {
                        String at = x + "," + y + "," + z;
                        row.append(at.equals(centre) ? '0' : grid.getOrDefault(at, ' '));
                    }
                    layer.add(row.toString());
                }
                pattern.add(layer);
            }
            JsonObject mapping = new JsonObject();
            keys.forEach((state, k) -> mapping.add(String.valueOf(k), matcher(state)));
            if (centreKey != null) keys.forEach((state, k) -> {
                if (k.equals(centreKey)) mapping.add("0", matcher(state));
            });
            String mbId = id.replace('/', '_') + (n == 0 ? "" : "_" + n);
            if (writeAssets) {
                JsonObject mb = new JsonObject();
                mb.addProperty("type", "modonomicon:dense");
                mb.add("pattern", pattern);
                mb.add("mapping", mapping);
                write(out.resolve("data/" + NS + "/modonomicon/multiblocks/" + BOOK + "/" + mbId + ".json"), mb);
                multiblocks++;
            }
            JsonObject p = new JsonObject();
            p.addProperty("type", "modonomicon:multiblock");
            p.addProperty("multiblock_id", NS + ":" + BOOK + "/" + mbId);
            p.addProperty("multiblock_name", key(id, "name"));
            p.addProperty("show_visualize_button", false);
            special(p, Map.of());
        }

        JsonObject matcher(String state) {
            JsonObject m = new JsonObject();
            m.addProperty("type", "modonomicon:blockstate");
            m.addProperty("block", state);
            return m;
        }

        int intAttr(String attrs, String name) {
            Matcher m = Pattern.compile("(?<![\\w:])" + name + "=\"(-?[0-9.]+)\"").matcher(attrs);
            return m.find() ? (int) Math.floor(Double.parseDouble(m.group(1))) : 0;
        }
    }

    // ------------------------------------------------------------------ paging (both languages together)

    /**
     * The pages of an entry from its items in English and German: Modonomicon has one page list for all languages, so each
     * run of text between two pages of their own gets as many text pages as the longer language needs, and the other
     * language is spread over the same number of pages. Writes the texts of both languages into their key maps.
     */
    JsonArray pages(String path, List<Item> en, List<Item> de, Map<String, String> enKeys, Map<String, String> deKeys) {
        String id = PatchouliConverter.entryId(path);
        List<List<Unit>> enRuns = new ArrayList<>(), deRuns = new ArrayList<>();
        List<Special> enSp = new ArrayList<>(), deSp = new ArrayList<>();
        split(en, enRuns, enSp);
        split(de, deRuns, deSp);
        if (enSp.size() != deSp.size()) {
            warnings.add(path + ": the German page has " + deSp.size() + " pictures / recipes / scenes, the English one " + enSp.size());
            return new JsonArray();
        }
        JsonArray out = new JsonArray();
        boolean titled = false;
        for (int r = 0; r < enRuns.size(); r++) {
            List<String> a = pack(enRuns.get(r), !titled), b = pack(deRuns.get(r), !titled);
            int n = Math.max(a.size(), b.size());
            a = stretch(a, n);
            b = stretch(b, n);
            for (int i = 0; i < n; i++) {
                JsonObject t = new JsonObject();
                t.addProperty("type", "modonomicon:text");
                // the entry's first text page carries its title (a page may start with a picture or a scene)
                if (!titled) {
                    t.addProperty("title", key(id, "name"));
                    t.addProperty("show_title_separator", true);
                    titled = true;
                }
                String k = key(id, out.size() + ".text");
                enKeys.put(k, tidy(a.get(i)));
                deKeys.put(k, tidy(b.get(i)));
                t.addProperty("text", k);
                out.add(t);
            }
            if (r < enSp.size()) {
                Special se = enSp.get(r), sd = deSp.get(r);
                if (!se.page().get("type").equals(sd.page().get("type"))) warnings.add(path + ": page " + out.size() + " is " + se.page().get("type")
                        + " in English but " + sd.page().get("type") + " in German");
                JsonObject p = se.page().deepCopy();
                for (var e : se.texts().entrySet()) {
                    String k = key(id, out.size() + "." + e.getKey());
                    enKeys.put(k, e.getValue());
                    deKeys.put(k, sd.texts().getOrDefault(e.getKey(), e.getValue()));
                    p.addProperty(e.getKey().equals("entity") ? "name" : e.getKey(), k);
                }
                out.add(p);
            }
        }
        if (out.isEmpty()) {
            JsonObject t = new JsonObject();
            t.addProperty("type", "modonomicon:text");
            t.addProperty("title", key(id, "name"));
            out.add(t);
        }
        return out;
    }

    /** The runs of text units and the pages of their own between them (there is one run more than pages of their own). */
    private static void split(List<Item> items, List<List<Unit>> runs, List<Special> specials) {
        List<Unit> run = new ArrayList<>();
        for (Item i : items) {
            if (i instanceof Unit u) run.add(u);
            else {
                runs.add(run);
                run = new ArrayList<>();
                specials.add((Special) i);
            }
        }
        runs.add(run);
    }

    /** Fills text pages greedily; a paragraph longer than a page is cut at sentence ends. */
    static List<String> pack(List<Unit> units, boolean first) {
        List<String> pages = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        int visible = 0;
        Unit last = null;
        for (Unit u : units) {
            int budget = pages.isEmpty() && first ? FIRST_PAGE : PAGE;
            int n = plain(u.md()).length();
            if (visible > 0 && visible + n > budget) {
                pages.add(text.toString().trim());
                text.setLength(0);
                visible = 0;
                budget = PAGE;
            }
            if (visible > 0) text.append(separator(last, u));
            text.append(u.md());
            last = u;
            // a line costs its characters plus the rest of its last line (about 20 characters a line)
            visible += n + (u.newParagraph() ? 30 : u.listItem() ? 12 : 20);
            while (visible > budget) {
                String all = text.toString();
                int cut = cutAt(all, budget);
                if (cut <= 0 || cut >= all.length()) break;
                pages.add(all.substring(0, cut).trim());
                text.setLength(0);
                text.append(all.substring(cut).trim());
                visible = plain(text.toString()).length();
                budget = PAGE;
            }
        }
        if (text.length() > 0) pages.add(text.toString().trim());
        return pages;
    }

    /**
     * What goes between two units on a page. Modonomicon's Markdown joins paragraphs without a break, so lines are separated
     * by hard line breaks (a backslash at the line end), paragraphs by two; a list starts after a blank line (Markdown) and
     * ends with one, followed by a hard break for the empty line.
     */
    static String separator(Unit prev, Unit next) {
        String hard = "\\\n";
        if (next.listItem()) return prev.listItem() ? "\n" : "\n\n";
        if (prev.listItem()) return next.newParagraph() ? "\n\n" + hard : "\n\n";
        return next.newParagraph() ? hard + hard : hard;
    }

    /** Spreads text pages over {@code n} pages: the longest page is split (at a line, else a sentence end) until there are n. */
    static List<String> stretch(List<String> pages, int n) {
        List<String> out = new ArrayList<>(pages);
        while (out.size() < n) {
            int longest = -1;
            for (int i = 0; i < out.size(); i++) if (longest < 0 || plain(out.get(i)).length() > plain(out.get(longest)).length()) longest = i;
            if (longest < 0) {
                out.add("");
                continue;
            }
            String s = out.get(longest);
            int mid = s.length() / 2, cut = -1;
            for (int d = 0; d < s.length() && cut < 0; d++) {
                for (int at : new int[]{mid - d, mid + d}) {
                    if (at > 0 && at < s.length() && s.charAt(at) == '\n' && outsideLink(s, at)) {
                        cut = at;
                        break;
                    }
                }
            }
            if (cut < 0) {
                int c = cutAt(s, plain(s).length() / 2 + 20);
                cut = c > 0 && c < s.length() ? c : -1;
            }
            if (cut < 0) {
                out.add(longest + 1, "");
                continue;
            }
            out.set(longest, s.substring(0, cut).trim());
            out.add(longest + 1, s.substring(cut).trim());
        }
        return out;
    }

    /** A page's text without hard line breaks at its start or end (a backslash there would be shown). */
    static String tidy(String s) {
        s = s.strip();
        boolean changed = true;
        while (changed) {
            changed = false;
            if (s.endsWith("\\")) {
                s = s.substring(0, s.length() - 1).strip();
                changed = true;
            }
            if (s.startsWith("\\\n")) {
                s = s.substring(2).strip();
                changed = true;
            }
        }
        return s;
    }

    private static boolean outsideLink(String s, int at) {
        int open = 0;
        for (int i = 0; i < at; i++) {
            if (s.charAt(i) == '[') open++;
            if (s.charAt(i) == ']') open--;
        }
        return open == 0;
    }

    /** A sentence end near the budget (visible characters), outside links. */
    static int cutAt(String s, int budget) {
        int vis = 0, best = -1, depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(' && i > 0 && s.charAt(i - 1) == ']') {
                int e = s.indexOf(')', i);
                if (e < 0) break;
                i = e;
                continue;
            }
            if (c == '[') depth++;
            if (c == ']') depth--;
            if (c == '*') continue;
            vis++;
            if (depth == 0 && (c == '.' || c == ';' || c == ':') && i + 1 < s.length() && s.charAt(i + 1) == ' ') {
                if (vis <= budget) best = i + 1;
                else break;
            }
        }
        return best;
    }

    /** The German titles of the pages (for the sub-page lists of the German entries). */
    private final Map<String, String> deTitles = new HashMap<>();

    /** The picture in a square texture (Modonomicon draws the whole texture into a 100 x 100 square), aspect kept, centred. */
    static void writeTexture(Path src, Path dst) throws IOException {
        BufferedImage in = ImageIO.read(src.toFile());
        BufferedImage out = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        double f = Math.min(256.0 / in.getWidth(), 256.0 / in.getHeight());
        int w = Math.max(1, (int) Math.round(in.getWidth() * f)), h = Math.max(1, (int) Math.round(in.getHeight() * f));
        var g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(in, (256 - w) / 2, (256 - h) / 2, w, h, null);
        g.dispose();
        Files.createDirectories(dst.getParent());
        ImageIO.write(out, "png", dst.toFile());
    }
}
