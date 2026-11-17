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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

/**
 * M52: converts the GuideME handbook (the one source: {@code guides/bettacraft/guide/*.md}, en and {@code _de_de}) into a
 * Patchouli book for the loaders without GuideME (Fabric). Pages with sub-pages become categories (and an entry of their
 * own), the others entries; text pages are split to fit a Patchouli page; images become 256x256 textures; GameME scenes
 * become Patchouli multiblock pages (blocks) or entity pages (animals), recipes crafting / smelting pages.
 *
 * <p>Usage: {@code PatchouliConverter <guide dir> <en lang json> <de lang json> <output resources dir> <recipe dir>...}
 */
public final class PatchouliConverter {
    public static final String NS = "bettacraft";
    public static final String BOOK = "handbook";
    /** Visible characters per text page (Patchouli's page is small); the first page of an entry has the title too. */
    static final int PAGE = 400;
    static final int FIRST_PAGE = 320;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** One GuideME page. */
    public record Page(String path, String title, String icon, String parent, int position, List<String> itemIds, String body) {}

    private final Path guide;
    private final Map<String, String> en;
    private final Map<String, String> de;
    private final List<Path> recipeDirs;
    /** recipe id -> json, for the result lookups of RecipesFor */
    private final Map<String, JsonObject> recipes = new TreeMap<>();
    /** item id -> entry id (the page that lists the item in item_ids) */
    private final Map<String, String> itemEntry = new HashMap<>();
    private Map<String, Page> pages;
    private final Map<String, List<String>> children = new HashMap<>();
    public final List<String> warnings = new ArrayList<>();
    public int images;

    /** The items of the mod: their generated item models (assets/bettacraft/models/item next to lang/). */
    private final java.util.Set<String> itemModels = new java.util.HashSet<>();

    public PatchouliConverter(Path guide, Path enLang, Path deLang, List<Path> recipeDirs) throws IOException {
        Path models = enLang.getParent().getParent().resolve("models/item");
        if (Files.isDirectory(models)) {
            try (var st = Files.list(models)) {
                st.forEach(f -> itemModels.add(f.getFileName().toString().replace(".json", "")));
            }
        }
        this.guide = guide;
        this.en = lang(enLang);
        this.de = lang(deLang);
        this.recipeDirs = recipeDirs;
    }

    public static void main(String[] args) throws IOException {
        List<Path> rec = new ArrayList<>();
        for (int i = 4; i < args.length; i++) rec.add(Path.of(args[i]));
        PatchouliConverter c = new PatchouliConverter(Path.of(args[0]), Path.of(args[1]), Path.of(args[2]), rec);
        int n = c.convert(Path.of(args[3]));
        System.out.println("PatchouliConverter: " + n + " entries per language, " + c.images + " images, " + c.warnings.size() + " warning(s)");
        for (String w : c.warnings) System.out.println("  " + w);
        // coverage: every GuideME page (English and German) is an entry, nothing is lost on the way (build fails otherwise)
        long pages;
        try (var s = Files.walk(Path.of(args[0]))) {
            pages = s.filter(p -> p.toString().endsWith(".md") && !Path.of(args[0]).relativize(p).toString().startsWith("_")).count();
        }
        if (n != pages || !c.warnings.isEmpty()) {
            System.err.println("PatchouliConverter: coverage failed: " + n + " entries for " + pages + " GuideME pages, "
                    + c.warnings.size() + " warning(s)");
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ reading

    private static Map<String, String> lang(Path p) throws IOException {
        Map<String, String> m = new HashMap<>();
        if (!Files.exists(p)) return m;
        JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
        for (var e : o.entrySet()) if (e.getValue().isJsonPrimitive()) m.put(e.getKey(), e.getValue().getAsString());
        return m;
    }

    /** The pages of one language (relative paths with '/', without the {@code _de_de} prefix). */
    public static Map<String, Page> readPages(Path root) throws IOException {
        Map<String, Page> out = new TreeMap<>();
        try (Stream<Path> s = Files.walk(root)) {
            for (Path p : (Iterable<Path>) s.filter(f -> f.toString().endsWith(".md"))::iterator) {
                String rel = root.relativize(p).toString().replace('\\', '/');
                if (rel.startsWith("_")) continue; // another language
                out.put(rel, parse(rel, Files.readString(p, StandardCharsets.UTF_8)));
            }
        }
        return out;
    }

    static Page parse(String rel, String text) {
        text = text.replace("\r\n", "\n");
        String title = rel, icon = "minecraft:book", parent = null, body = text;
        int position = 0;
        List<String> items = new ArrayList<>();
        if (text.startsWith("---\n")) {
            int end = text.indexOf("\n---", 4);
            String fm = text.substring(4, end);
            body = text.substring(text.indexOf('\n', end + 1) + 1);
            String section = "";
            for (String line : fm.split("\n")) {
                if (!line.startsWith(" ") && line.endsWith(":")) section = line.substring(0, line.length() - 1).trim();
                String t = line.trim();
                if (section.equals("navigation")) {
                    if (t.startsWith("title:")) title = unquote(t.substring(6));
                    else if (t.startsWith("icon:")) icon = unquote(t.substring(5));
                    else if (t.startsWith("parent:")) parent = unquote(t.substring(7));
                    else if (t.startsWith("position:")) position = Integer.parseInt(t.substring(9).trim());
                } else if (section.equals("item_ids") && t.startsWith("- ")) {
                    items.add(unquote(t.substring(2)));
                }
            }
        }
        return new Page(rel, title, icon, parent, position, items, body);
    }

    private static String unquote(String s) {
        s = s.trim();
        if (s.length() >= 2 && (s.startsWith("\"") && s.endsWith("\"") || s.startsWith("'") && s.endsWith("'"))) s = s.substring(1, s.length() - 1);
        return s;
    }

    /** The Patchouli id of a page: its path without .md ("aquarium/shapes"). */
    public static String entryId(String path) {
        return path.endsWith(".md") ? path.substring(0, path.length() - 3) : path;
    }

    // ------------------------------------------------------------------ writing

    /** Writes the book into {@code out} (a resources root); returns the number of entries per language. */
    public int convert(Path out) throws IOException {
        pages = readPages(guide);
        Map<String, Page> dePages = Files.isDirectory(guide.resolve("_de_de")) ? readPages(guide.resolve("_de_de")) : Map.of();
        for (Path d : recipeDirs) readRecipes(d);
        for (Page p : pages.values()) {
            for (String id : p.itemIds()) itemEntry.putIfAbsent(id, entryId(p.path()));
            if (p.parent() != null) children.computeIfAbsent(p.parent(), k -> new ArrayList<>()).add(p.path());
        }
        children.values().forEach(l -> l.sort(Comparator.comparingInt((String c) -> pages.get(c).position()).thenComparing(c -> c)));
        Path assets = out.resolve("assets/" + NS + "/patchouli_books/" + BOOK);
        writeBook(out);
        writeLanguage(assets.resolve("en_us"), pages, en, out);
        writeLanguage(assets.resolve("de_de"), dePages.isEmpty() ? pages : merged(dePages), de.isEmpty() ? en : fallback(de, en), out);
        return pages.size();
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
                // structure (parent, position, icon, items) from the English page, text and title from the German one
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

    private void writeBook(Path out) throws IOException {
        JsonObject book = new JsonObject();
        book.addProperty("name", "book.bettacraft.handbook.name");
        book.addProperty("landing_text", "book.bettacraft.handbook.landing");
        book.addProperty("subtitle", "book.bettacraft.handbook.subtitle");
        book.addProperty("i18n", true);
        book.addProperty("use_resource_pack", true);
        book.addProperty("show_progress", false);
        book.addProperty("version", "1");
        book.addProperty("book_texture", "patchouli:textures/gui/book_cyan.png");
        book.addProperty("model", "patchouli:book_cyan");
        book.addProperty("creative_tab", "bettacraft:main");
        write(out.resolve("data/" + NS + "/patchouli_books/" + BOOK + "/book.json"), book);
    }

    /** A page is a category when it has sub-pages (or is a top-level page). */
    private boolean isCategory(String path) {
        return children.containsKey(path) || pages.get(path).parent() == null;
    }

    private String categoryOf(String path) {
        return isCategory(path) ? entryId(path) : entryId(pages.get(path).parent());
    }

    private void writeLanguage(Path dir, Map<String, Page> ps, Map<String, String> lang, Path out) throws IOException {
        for (Page p : ps.values()) {
            if (isCategory(p.path())) {
                JsonObject c = new JsonObject();
                c.addProperty("name", p.title());
                c.addProperty("description", firstSentence(p.body(), lang));
                c.addProperty("icon", icon(p.icon()));
                c.addProperty("sortnum", p.position());
                if (p.parent() != null && pages.containsKey(p.parent())) c.addProperty("parent", NS + ":" + entryId(p.parent()));
                write(dir.resolve("categories/" + entryId(p.path()) + ".json"), c);
            }
            JsonObject e = new JsonObject();
            e.addProperty("name", p.title());
            e.addProperty("icon", icon(p.icon()));
            e.addProperty("category", NS + ":" + categoryOf(p.path()));
            // the category's own entry first
            e.addProperty("sortnum", isCategory(p.path()) ? -1 : p.position());
            if (isCategory(p.path())) e.addProperty("priority", true);
            JsonArray pg = new PageBuilder(p, lang, out).build();
            e.add("pages", pg);
            if (!p.itemIds().isEmpty()) {
                JsonObject map = new JsonObject();
                // only real items (GuideME also lists ids of non-items, e.g. the frog spawn of a tank)
                for (String id : p.itemIds()) {
                    String path = id.substring(id.indexOf(':') + 1);
                    if (!id.startsWith(NS + ":") || itemModels.contains(path)) {
                        map.addProperty(id, 0);
                    }
                }
                if (map.size() > 0) e.add("extra_recipe_mappings", map);
            }
            write(dir.resolve("entries/" + entryId(p.path()) + ".json"), e);
        }
    }

    private static String icon(String id) {
        return id == null || id.isBlank() ? "minecraft:book" : id;
    }

    private String firstSentence(String body, Map<String, String> lang) {
        for (String line : body.split("\n")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#") || t.startsWith("<") || t.startsWith("!") || t.startsWith("|")) continue;
            String plain = stripCodes(inline(t, lang));
            int dot = plain.indexOf(". ");
            return dot > 0 && dot < 200 ? plain.substring(0, dot + 1) : plain.length() > 200 ? plain.substring(0, 200) + "…" : plain;
        }
        return "";
    }

    static String stripCodes(String s) {
        return s.replaceAll("\\$\\([^)]*\\)", "");
    }

    /** Every string value with % escaped as %% (an i18n book formats its texts). */
    private static JsonElement escapePercent(JsonElement e) {
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) return new com.google.gson.JsonPrimitive(e.getAsString().replace("%", "%%"));
        if (e.isJsonArray()) {
            JsonArray a = new JsonArray();
            for (JsonElement x : e.getAsJsonArray()) a.add(escapePercent(x));
            return a;
        }
        if (e.isJsonObject()) {
            JsonObject o = new JsonObject();
            for (var en : e.getAsJsonObject().entrySet()) o.add(en.getKey(), escapePercent(en.getValue()));
            return o;
        }
        return e;
    }

    private static void write(Path p, JsonElement json) throws IOException {
        json = escapePercent(json);
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

    /** The recipes (ids) that make the item, crafting first. */
    private List<String> recipesFor(String item) {
        List<String> out = new ArrayList<>();
        for (var e : recipes.entrySet()) if (item.equals(resultOf(e.getValue()))) out.add(e.getKey());
        return out;
    }

    private static String patchouliType(JsonObject r) {
        String t = r.has("type") ? r.get("type").getAsString() : "";
        return switch (t) {
            case "minecraft:crafting_shaped", "minecraft:crafting_shapeless" -> "patchouli:crafting";
            case "minecraft:smelting" -> "patchouli:smelting";
            case "minecraft:blasting" -> "patchouli:blasting";
            case "minecraft:smoking" -> "patchouli:smoking";
            case "minecraft:campfire_cooking" -> "patchouli:campfire";
            case "minecraft:stonecutting" -> "patchouli:stonecutting";
            case "minecraft:smithing_transform" -> "patchouli:smithing";
            default -> null;
        };
    }

    // ------------------------------------------------------------------ names and inline markup

    String itemName(String id, Map<String, String> lang) {
        String ns = id.contains(":") ? id.substring(0, id.indexOf(':')) : "minecraft";
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        for (String k : new String[]{"item." + ns + "." + path, "block." + ns + "." + path, "entity." + ns + "." + path}) {
            String v = lang.get(k);
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

    /** Converts one line of GuideME markdown to Patchouli text (no block elements). */
    String inline(String s, Map<String, String> lang) {
        return inline(s, lang, null);
    }

    String inline(String s, Map<String, String> lang, String from) {
        s = s.replace("$", "＄");
        Matcher m = ITEM_LINK.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String id = m.group(1);
            String name = itemName(id, lang);
            String entry = itemEntry.get(id);
            String rep = entry != null && (from == null || !entry.equals(entryId(from))) ? "$(l:" + entry + ")" + name + "$(/l)" : "$(item)" + name + "$()";
            m.appendReplacement(sb, Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        s = sb.toString();
        m = ITEM_IMAGE.matcher(s);
        sb = new StringBuilder();
        while (m.find()) m.appendReplacement(sb, Matcher.quoteReplacement("$(item)" + itemName(m.group(1), lang) + "$()"));
        m.appendTail(sb);
        s = sb.toString();
        m = LINK.matcher(s);
        sb = new StringBuilder();
        while (m.find()) {
            String text = m.group(1), url = m.group(2);
            String rep;
            if (url.startsWith("http")) rep = text;
            else {
                String target = resolve(from, url.replaceAll("#.*$", ""));
                rep = target != null && pages.containsKey(target) ? "$(l:" + entryId(target) + ")" + text + "$(/l)" : text;
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        s = sb.toString();
        s = s.replaceAll("\\*\\*([^*]+)\\*\\*", "\\$(l)$1\\$()");
        s = s.replaceAll("(?<![\\w*])\\*([^*\\s][^*]*)\\*(?![\\w*])", "\\$(o)$1\\$()");
        s = s.replaceAll("(?<![\\w_])_([^_\\s][^_]*)_(?![\\w_])", "\\$(o)$1\\$()");
        s = s.replaceAll("`([^`]+)`", "\\$(8)$1\\$()");
        s = s.replaceAll("</?(Row|Small|Version|Spieler|Pfad|Name|br)\\b[^>]*>", "");
        s = s.replaceAll("<[A-Z][A-Za-z]*\\b[^>]*/>", "");
        return s.replace("＄", "$");
    }

    /** A link target relative to the page {@code from}. */
    String resolve(String from, String url) {
        if (url.isEmpty()) return null;
        Path base = from == null || !from.contains("/") ? Path.of("") : Path.of(from).getParent();
        String r = base.resolve(url).normalize().toString().replace('\\', '/');
        if (pages.containsKey(r)) return r;
        return pages.containsKey(url) ? url : null;
    }

    // ------------------------------------------------------------------ pages of one entry

    private final class PageBuilder {
        final Page page;
        final Map<String, String> lang;
        final Path out;
        final JsonArray pages = new JsonArray();
        final StringBuilder text = new StringBuilder();
        int visible;

        PageBuilder(Page page, Map<String, String> lang, Path out) {
            this.page = page;
            this.lang = lang;
            this.out = out;
        }

        int budget() {
            return pages.isEmpty() ? FIRST_PAGE : PAGE;
        }

        void add(String patchouli, boolean newParagraph) {
            int n = stripCodes(patchouli).length();
            if (visible > 0 && visible + n > budget()) flush();
            if (visible > 0) text.append(newParagraph ? "$(br2)" : "$(br)");
            text.append(patchouli);
            visible += n + (newParagraph ? 45 : 25);
            // a very long paragraph: split at sentences
            while (visible > budget() && text.length() > 0) {
                String all = text.toString();
                int cut = cutAt(all, budget());
                if (cut <= 0 || cut >= all.length()) break;
                text.setLength(0);
                text.append(all, 0, cut);
                flushRaw();
                text.append(all.substring(cut).trim());
                visible = stripCodes(text.toString()).length();
            }
        }

        /** A sentence end near the budget (visible characters), outside formatting codes. */
        int cutAt(String s, int budget) {
            int vis = 0, best = -1;
            for (int i = 0; i < s.length(); i++) {
                if (s.startsWith("$(", i)) {
                    int e = s.indexOf(')', i);
                    if (e < 0) break;
                    i = e;
                    continue;
                }
                vis++;
                if ((s.charAt(i) == '.' || s.charAt(i) == ';' || s.charAt(i) == ':') && i + 1 < s.length() && s.charAt(i + 1) == ' ') {
                    if (vis <= budget) best = i + 1;
                    else break;
                }
            }
            return best;
        }

        void flushRaw() {
            JsonObject t = new JsonObject();
            t.addProperty("type", "patchouli:text");
            t.addProperty("text", closeLinks(text.toString()));
            pages.add(t);
            text.setLength(0);
            visible = 0;
        }

        void flush() {
            if (text.length() > 0) flushRaw();
        }

        /** A link or format left open at a page cut is closed (Patchouli would carry it over). */
        String closeLinks(String s) {
            int open = count(s, "$(l:"), close = count(s, "$(/l)");
            StringBuilder b = new StringBuilder(s);
            for (int i = close; i < open; i++) b.append("$(/l)");
            return b.toString();
        }

        int count(String s, String what) {
            int n = 0;
            for (int i = s.indexOf(what); i >= 0; i = s.indexOf(what, i + 1)) n++;
            return n;
        }

        void special(JsonObject p) {
            flush();
            pages.add(p);
        }

        JsonArray build() throws IOException {
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
                    String h = t.replaceFirst("^#+\\s*", "");
                    flush();
                    add("$(l)" + inline(h, lang, page.path()) + "$()", true);
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
                    recipe(one.group(1));
                    continue;
                }
                if (t.startsWith("<SubPages")) {
                    paragraph(para);
                    for (String c : children.getOrDefault(page.path(), List.of())) {
                        String title = lang == de ? titleDe(c) : PatchouliConverter.this.pages.get(c).title();
                        add("$(li)$(l:" + entryId(c) + ")" + title + "$(/l)", false);
                    }
                    continue;
                }
                if (t.matches("^([-*]|\\d+\\.)\\s+.*")) {
                    paragraph(para);
                    String item = t.replaceFirst("^([-*]|\\d+\\.)\\s+", "");
                    int depth = (l.length() - l.stripLeading().length()) / 2;
                    while (i + 1 < lines.length && lines[i + 1].matches("^\\s{2,}\\S.*") && !lines[i + 1].trim().matches("^([-*]|\\d+\\.)\\s+.*")) {
                        item += " " + lines[++i].trim();
                    }
                    add((depth > 0 ? "$(li" + (depth + 1) + ")" : "$(li)") + inline(item, lang, page.path()), false);
                    continue;
                }
                para.add(t);
            }
            paragraph(para);
            flush();
            if (pages.isEmpty()) {
                JsonObject t = new JsonObject();
                t.addProperty("type", "patchouli:text");
                t.addProperty("text", page.title());
                pages.add(t);
            }
            return pages;
        }

        String titleDe(String path) {
            return PatchouliConverter.this.pages.get(path).title();
        }

        void paragraph(List<String> para) {
            if (para.isEmpty()) return;
            add(inline(String.join(" ", para), lang, page.path()), true);
            para.clear();
        }

        void table(List<String> rows) {
            boolean header = true;
            for (String r : rows) {
                if (r.matches("^[|\\s:-]+$")) continue;
                List<String> cells = new ArrayList<>();
                for (String c : r.replaceAll("^\\|", "").replaceAll("\\|\\s*$", "").split("\\|")) cells.add(inline(c.trim(), lang, page.path()));
                String line = String.join(" · ", cells);
                add(header ? "$(l)" + line + "$()" : "$(li)" + line, header);
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
            if (!Files.exists(tex)) {
                writeTexture(file, tex);
                images++;
            }
            JsonObject p = new JsonObject();
            p.addProperty("type", "patchouli:image");
            JsonArray imgs = new JsonArray();
            imgs.add(NS + ":textures/gui/handbook/" + name + ".png");
            p.add("images", imgs);
            p.addProperty("border", true);
            if (!alt.isBlank()) p.addProperty("text", inline(alt, lang, page.path()));
            special(p);
        }

        void recipes(String item) {
            List<String> ids = recipesFor(item);
            boolean any = false;
            for (String id : ids) any |= recipe(id);
            if (!any) {
                // a recipe Patchouli cannot draw (food maker, crucible, stone cabinet ...): the item with its name
                JsonObject p = new JsonObject();
                p.addProperty("type", "patchouli:spotlight");
                p.addProperty("item", item); // Patchouli reads an item stack string
                p.addProperty("link_recipe", true);
                special(p);
            }
        }

        boolean recipe(String id) {
            JsonObject r = recipes.get(id);
            String type = r == null ? null : patchouliType(r);
            if (type == null) return false;
            JsonObject p = new JsonObject();
            p.addProperty("type", type);
            p.addProperty("recipe", id);
            special(p);
            return true;
        }

        void scene(String scene) {
            Matcher ent = Pattern.compile("<Entity\\s+id=\"([^\"]+)\"([^>]*)/>").matcher(scene);
            if (ent.find()) {
                JsonObject p = new JsonObject();
                p.addProperty("type", "patchouli:entity");
                Matcher data = Pattern.compile("data=\"(\\{.*\\})\"").matcher(ent.group(2));
                p.addProperty("entity", ent.group(1) + (data.find() ? data.group(1) : ""));
                p.addProperty("scale", 1.6F);
                p.addProperty("offset", 0.2F);
                p.addProperty("name", itemName(ent.group(1), lang));
                special(p);
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
            // Patchouli: pattern[layer from top][z][x]; exactly one '0' marks the centre (here: the middle of the bottom
            // layer; it keeps its block through the mapping "0")
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
            keys.forEach((state, k) -> mapping.addProperty(String.valueOf(k), state));
            if (centreKey != null) keys.forEach((state, k) -> {
                if (k.equals(centreKey)) mapping.addProperty("0", state);
            });
            JsonObject mb = new JsonObject();
            mb.add("pattern", pattern);
            mb.add("mapping", mapping);
            JsonObject p = new JsonObject();
            p.addProperty("type", "patchouli:multiblock");
            p.addProperty("name", page.title());
            p.add("multiblock", mb);
            p.addProperty("enable_visualize", false);
            special(p);
        }

        int intAttr(String attrs, String name) {
            Matcher m = Pattern.compile("(?<![\\w:])" + name + "=\"(-?[0-9.]+)\"").matcher(attrs);
            return m.find() ? (int) Math.floor(Double.parseDouble(m.group(1))) : 0;
        }
    }

    /** Fits the picture into the 200x200 area Patchouli shows of a 256x256 texture (aspect kept, centred). */
    static void writeTexture(Path src, Path dst) throws IOException {
        BufferedImage in = ImageIO.read(src.toFile());
        BufferedImage out = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        double f = Math.min(200.0 / in.getWidth(), 200.0 / in.getHeight());
        int w = Math.max(1, (int) Math.round(in.getWidth() * f)), h = Math.max(1, (int) Math.round(in.getHeight() * f));
        var g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(in, (200 - w) / 2, (200 - h) / 2, w, h, null);
        g.dispose();
        Files.createDirectories(dst.getParent());
        ImageIO.write(out, "png", dst.toFile());
    }
}
