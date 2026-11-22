package org.overengineer.bettacraft.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.plants.PlantColors;
import org.overengineer.bettacraft.plants.PlantPart;
import org.overengineer.bettacraft.plants.PlantSpecies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M62: no two items of the mod look the same in the inventory (M55 did it for the animals in their containers;
 * {@code AnimalIconsTest}). Every item model is resolved like the game does it: a flat icon ({@code item/generated},
 * {@code item/handheld}) is composed from its layers, the plants with the tint of the game ({@link PlantColors}, the leaves
 * on layer 0, the stem, rhizome or flower colour on layer 1, the light of an ordinary tank); any other model by its parent,
 * its resolved textures and its elements. Before M62 the 57 aquarium plants shared 13 pictures (all 17 epiphytes showed
 * the same Anubias), the hammer coral and the Acropora the same branch, and nine pairs of terrarium plants a look.
 * <p>
 * The plants must also be clearly apart: two plants whose pictures are almost the same (shape and colour) are hard to tell
 * apart in a chest, so every pair differs by {@link #MIN_PLANT_DISTANCE} on average over the pixels of either (the larger
 * channel difference, both drawn on the slot grey).
 */
class ItemIconsTest {
    /** Mean difference (0..255) every two plant icons keep. */
    static final double MIN_PLANT_DISTANCE = 30;
    private static final int SLOT = 0x8B8B8B;

    private static Path res(String set) {
        return Path.of(System.getProperty("bettacraft.projectDir", "."), "src", set, "resources", "assets", "bettacraft");
    }

    private static JsonObject json(Path p) throws IOException {
        return JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static JsonObject model(String path) throws IOException {
        for (String set : new String[]{"generated", "main"}) {
            Path p = res(set).resolve("models/" + path + ".json");
            if (Files.exists(p)) return json(p);
        }
        return null;
    }

    private static BufferedImage texture(String id) throws IOException {
        String ns = id.contains(":") ? id.substring(0, id.indexOf(':')) : "minecraft";
        if (!ns.equals("bettacraft")) return null;
        String path = id.substring(id.indexOf(':') + 1);
        for (String set : new String[]{"main", "generated"}) {
            Path p = res(set).resolve("textures/" + path + ".png");
            if (Files.exists(p)) return ImageIO.read(p.toFile());
        }
        return null;
    }

    /** The resolved model of an item: its own json, the textures of the whole chain and the first parent outside the mod. */
    private record Resolved(JsonObject own, Map<String, String> textures, String top, JsonElement elements) {}

    private static Resolved resolve(JsonObject item) throws IOException {
        List<JsonObject> chain = new ArrayList<>();
        chain.add(item);
        JsonObject m = item;
        while (m.has("parent") && m.get("parent").getAsString().startsWith("bettacraft:")) {
            m = model(m.get("parent").getAsString().substring("bettacraft:".length()));
            if (m == null) break;
            chain.add(m);
        }
        Map<String, String> tex = new TreeMap<>();
        JsonElement elements = null;
        for (int i = chain.size() - 1; i >= 0; i--) {
            JsonObject c = chain.get(i);
            if (c.has("textures")) for (var e : c.getAsJsonObject("textures").entrySet()) tex.put(e.getKey(), e.getValue().getAsString());
            if (c.has("elements")) elements = c.get("elements");
        }
        JsonObject last = chain.getLast();
        String top = last.has("parent") ? last.get("parent").getAsString() : last.has("loader") ? "loader:" + last.get("loader").getAsString() : "";
        return new Resolved(item, tex, top, elements);
    }

    private static boolean flat(String top) {
        return top.equals("minecraft:item/generated") || top.equals("item/generated") || top.equals("minecraft:item/handheld") || top.equals("item/handheld");
    }

    /** The layers drawn over each other, each multiplied by its tint (ARGB). */
    static int[] compose(Map<String, String> tex, int[] tints) throws IOException {
        int[] out = new int[16 * 16];
        for (int layer = 0; tex.containsKey("layer" + layer); layer++) {
            BufferedImage img = texture(tex.get("layer" + layer));
            if (img == null) break;
            int tint = layer < tints.length ? tints[layer] : 0xFFFFFF;
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                int src = img.getRGB(x, y), a = src >>> 24;
                if (a == 0) continue;
                int r = (src >> 16 & 255) * (tint >> 16 & 255) / 255, g = (src >> 8 & 255) * (tint >> 8 & 255) / 255, b = (src & 255) * (tint & 255) / 255;
                int dst = out[y * 16 + x];
                out[y * 16 + x] = 0xFF000000 | mix(dst >> 16 & 255, r, a) << 16 | mix(dst >> 8 & 255, g, a) << 8 | mix(dst & 255, b, a);
            }
        }
        return out;
    }

    private static int mix(int d, int s, int a) {
        return (d * (255 - a) + s * a) / 255;
    }

    /** The tints of a plant item: the leaves (layer 0) and the rhizome / stem (aquarium plants) or the flowers (terrarium). */
    static int[] plantTints(PlantSpecies s) {
        PlantPart detail = s.land() ? PlantPart.TOP
                : PlantColors.rootLooksLike(s.form()) == PlantPart.STEM ? PlantPart.STEM : PlantPart.RHIZOME;
        return new int[]{PlantColors.color(s, PlantPart.LEAF, 2), PlantColors.color(s, detail, 2)};
    }

    private static Map<String, PlantSpecies> plants() {
        Map<String, PlantSpecies> out = new HashMap<>();
        for (PlantSpecies s : PlantSpecies.values()) out.put(s.id(), s);
        return out;
    }

    private static List<Path> itemModels() throws IOException {
        List<Path> out = new ArrayList<>();
        for (String set : new String[]{"generated", "main"}) {
            Path dir = res(set).resolve("models/item");
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> s = Files.list(dir)) {
                s.filter(p -> p.toString().endsWith(".json")).sorted().forEach(out::add);
            }
        }
        return out;
    }

    @Test
    void noTwoItemsLookTheSame() throws IOException {
        Map<String, PlantSpecies> plants = plants();
        Map<String, String> seen = new HashMap<>();
        List<String> clashes = new ArrayList<>();
        int flatCount = 0, other = 0;
        for (Path p : itemModels()) {
            String name = p.getFileName().toString().replace(".json", "");
            Resolved r = resolve(json(p));
            // vanilla's spawn egg template, tinted with the egg colours of each animal
            if (r.top().equals("minecraft:item/template_spawn_egg")) continue;
            String key;
            if (flat(r.top()) && r.textures().containsKey("layer0")) {
                PlantSpecies s = plants.get(name);
                int[] px = compose(r.textures(), s != null ? plantTints(s) : new int[0]);
                key = "flat:" + java.util.Arrays.toString(px);
                flatCount++;
            } else {
                key = "model:" + r.top() + r.textures() + r.elements();
                other++;
            }
            String before = seen.putIfAbsent(key, name);
            if (before != null) clashes.add(before + " = " + name);
        }
        assertTrue(flatCount > 300 && other > 50, "found the item models: " + flatCount + " flat, " + other + " other");
        assertTrue(clashes.isEmpty(), "items with the same icon: " + clashes);
    }

    private static int[] onSlot(int[] px) {
        int[] out = new int[px.length];
        for (int i = 0; i < px.length; i++) {
            int a = px[i] >>> 24, c = px[i];
            out[i] = mix(SLOT >> 16 & 255, c >> 16 & 255, a) << 16 | mix(SLOT >> 8 & 255, c >> 8 & 255, a) << 8 | mix(SLOT & 255, c & 255, a);
        }
        return out;
    }

    /** Mean over the pixels either icon covers of the larger channel difference, both drawn on the slot grey. */
    static double distance(int[] a, int[] b) {
        int[] ca = onSlot(a), cb = onSlot(b);
        double sum = 0;
        int n = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] >>> 24 < 128 && b[i] >>> 24 < 128) continue;
            int d = 0;
            for (int sh = 0; sh <= 16; sh += 8) d = Math.max(d, Math.abs((ca[i] >> sh & 255) - (cb[i] >> sh & 255)));
            sum += d;
            n++;
        }
        return n == 0 ? 0 : sum / n;
    }

    @Test
    void everyPlantHasItsOwnClearlyDifferentIcon() throws IOException {
        Map<String, int[]> icons = new LinkedHashMap<>();
        for (PlantSpecies s : PlantSpecies.values()) {
            Path p = res("generated").resolve("models/item/" + s.id() + ".json");
            assertTrue(Files.exists(p), "no item model of " + s.id());
            Resolved r = resolve(json(p));
            assertTrue(flat(r.top()), s.id() + ": a flat icon");
            if (!s.land()) assertEquals("bettacraft:item/plant_" + s.id(), r.textures().get("layer0"), s.id() + ": its own icon");
            icons.put(s.id(), compose(r.textures(), plantTints(s)));
        }
        List<String> close = new ArrayList<>();
        List<String> ids = new ArrayList<>(icons.keySet());
        for (int i = 0; i < ids.size(); i++) for (int j = i + 1; j < ids.size(); j++) {
            double d = distance(icons.get(ids.get(i)), icons.get(ids.get(j)));
            if (d < MIN_PLANT_DISTANCE) close.add(ids.get(i) + " ~ " + ids.get(j) + " (" + Math.round(d * 10) / 10.0 + ")");
        }
        assertTrue(close.isEmpty(), "plant icons too much alike: " + close);
    }
}
