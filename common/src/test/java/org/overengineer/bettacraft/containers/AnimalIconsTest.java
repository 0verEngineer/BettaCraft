package org.overengineer.bettacraft.containers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.terrarium.HerpSpecies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M55: every animal has its own icon in its container. The item models (generated) of every bucket, box, critter bucket,
 * fish bag and transport tub with an animal are composed from their texture layers like the game draws them; no two
 * animals may have the same picture in a kind of container (the apple snail showed the shrimp, every tankmate the betta),
 * every small tankmate and small animal has a bag model, every big animal a tub model, chosen by the property
 * {@code bettacraft:animal} with its number, and the body layer of a small animal is grey exactly when the game tints it.
 */
class AnimalIconsTest {
    private static Path res(String set) {
        return Path.of(System.getProperty("bettacraft.projectDir", "."), "src", set, "resources", "assets", "bettacraft");
    }

    private static JsonObject itemModel(String name) throws IOException {
        Path p = res("generated").resolve("models/item/" + name + ".json");
        assertTrue(Files.exists(p), "missing item model " + p);
        return JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static BufferedImage texture(String id) throws IOException {
        String path = id.substring(id.indexOf(':') + 1);
        Path p = res("main").resolve("textures/" + path + ".png");
        assertTrue(Files.exists(p), "missing texture " + p);
        return ImageIO.read(p.toFile());
    }

    /** The model's layers drawn over each other (alpha blending), as ARGB pixels. */
    private static int[] compose(JsonObject model) throws IOException {
        JsonObject t = model.getAsJsonObject("textures");
        int[] out = new int[16 * 16];
        for (int layer = 0; t.has("layer" + layer); layer++) {
            BufferedImage img = texture(t.get("layer" + layer).getAsString());
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                int src = img.getRGB(x, y), a = src >>> 24;
                if (a == 0) continue;
                int dst = out[y * 16 + x];
                int r = mix(dst >> 16 & 255, src >> 16 & 255, a), g = mix(dst >> 8 & 255, src >> 8 & 255, a), b = mix(dst & 255, src & 255, a);
                out[y * 16 + x] = 0xFF000000 | r << 16 | g << 8 | b;
            }
        }
        return out;
    }

    private static int mix(int d, int s, int a) {
        return (d * (255 - a) + s * a) / 255;
    }

    private static String hash(int[] px) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (int v : px) md.update(new byte[]{(byte) (v >>> 24), (byte) (v >>> 16), (byte) (v >>> 8), (byte) v});
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The overrides of a model by the value of {@code bettacraft:animal}: value -> model name (without the namespace). On
     * 1.21.1 the item model's {@code overrides} (the last match wins); from 1.21.4 on the item definition
     * ({@code items/<id>.json}): nested conditions, the outermost checked first (the same order reversed).
     */
    private static Map<Integer, String> animalOverrides(String name) throws IOException {
        List<JsonObject> rising = new ArrayList<>(); // {threshold, model} in the order of 1.21.1 (rising)
        JsonObject model = itemModel(name);
        if (model.has("overrides")) {
            for (JsonElement e : model.getAsJsonArray("overrides")) {
                JsonObject o = e.getAsJsonObject();
                JsonObject pred = o.getAsJsonObject("predicate");
                if (!pred.has("bettacraft:animal")) continue;
                JsonObject r = new JsonObject();
                r.add("threshold", pred.get("bettacraft:animal"));
                r.add("model", o.get("model"));
                rising.add(r);
            }
        } else {
            Path def = res("generated").resolve("items/" + name + ".json");
            assertTrue(Files.exists(def), "neither overrides nor an item definition for " + name);
            JsonObject m = JsonParser.parseString(Files.readString(def, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("model");
            List<JsonObject> outerFirst = new ArrayList<>();
            while (m != null && "minecraft:condition".equals(m.get("type").getAsString())) {
                if ("bettacraft:animal".equals(m.get("name").getAsString())) {
                    JsonObject r = new JsonObject();
                    r.add("threshold", m.get("threshold"));
                    r.add("model", m.getAsJsonObject("on_true").get("model"));
                    outerFirst.add(r);
                }
                m = m.getAsJsonObject("on_false");
            }
            for (int i = outerFirst.size() - 1; i >= 0; i--) rising.add(outerFirst.get(i));
        }
        Map<Integer, String> out = new HashMap<>();
        double last = 0;
        for (JsonObject r : rising) {
            double v = r.get("threshold").getAsDouble();
            assertTrue(v > last, name + ": the overrides must rise (the last match wins): " + v + " after " + last);
            last = v;
            int index = Math.round((float) v * AnimalIcons.SCALE + 0.5F);
            assertEquals(AnimalIcons.threshold(index), (float) v, 1e-6, name + ": threshold of " + index);
            String model2 = r.get("model").getAsString();
            out.put(index, model2.substring(model2.indexOf(":item/") + 6));
        }
        return out;
    }

    private static void assertUnique(String kind, Map<String, String> modelOfAnimal) throws IOException {
        Map<String, String> seen = new HashMap<>();
        List<String> clashes = new ArrayList<>();
        for (var e : modelOfAnimal.entrySet()) {
            String h = hash(compose(itemModel(e.getValue())));
            String other = seen.putIfAbsent(h, e.getKey());
            if (other != null) clashes.add(other + " = " + e.getKey());
        }
        assertTrue(clashes.isEmpty(), kind + ": animals with the same icon: " + clashes);
    }

    @Test
    void everyAnimalHasItsOwnBucketOrBoxIcon() throws IOException {
        Map<String, String> buckets = new LinkedHashMap<>();
        buckets.put("betta", "betta_bucket");
        buckets.put("dwarf_frog", "dwarf_frog_bucket");
        for (TankmateSpecies s : TankmateSpecies.values()) {
            if (s.isLarge()) continue;
            JsonObject m = itemModel(s.bucketId());
            assertEquals("bettacraft:" + AnimalIcons.sprite(s), m.getAsJsonObject("textures").get("layer1").getAsString(), s.bucketId());
            buckets.put(s.id(), s.bucketId());
        }
        for (HerpSpecies s : HerpSpecies.ALL) buckets.put("herp_" + s.containerId(), s.containerId());
        Map<Integer, String> critters = animalOverrides("critter_bucket");
        for (CritterSpecies s : CritterSpecies.values()) {
            String model = critters.get(AnimalIcons.index(s));
            assertEquals("critter_bucket_" + s.id(), model, "critter bucket of " + s.id());
            buckets.put(s.id(), model);
        }
        assertUnique("buckets and boxes", buckets);
    }

    @Test
    void everyAnimalHasItsOwnBagAndTubIcon() throws IOException {
        Map<Integer, String> bag = animalOverrides("fish_bag");
        Map<Integer, String> tub = animalOverrides("transport_tub");
        Map<String, String> bags = new LinkedHashMap<>();
        Map<String, String> tubs = new LinkedHashMap<>();
        for (TankmateSpecies s : TankmateSpecies.values()) {
            if (s.isLarge()) {
                assertEquals("transport_tub_" + s.id(), tub.get(AnimalIcons.index(s)), "tub of " + s.id());
                tubs.put(s.id(), tub.get(AnimalIcons.index(s)));
            } else {
                assertEquals("fish_bag_" + s.id(), bag.get(AnimalIcons.index(s)), "bag of " + s.id());
                bags.put(s.id(), bag.get(AnimalIcons.index(s)));
            }
        }
        for (CritterSpecies s : CritterSpecies.values()) {
            assertEquals("fish_bag_" + s.id(), bag.get(AnimalIcons.index(s)), "bag of " + s.id());
            bags.put(s.id(), bag.get(AnimalIcons.index(s)));
        }
        assertEquals(bags.size(), bag.size(), "bag overrides for other animals");
        assertEquals(tubs.size(), tub.size(), "tub overrides for other animals");
        assertUnique("fish bags", bags);
        assertUnique("transport tubs", tubs);
    }

    @Test
    void geneColouredBodiesAreGrey() throws IOException {
        for (CritterSpecies s : CritterSpecies.values()) {
            BufferedImage body = texture("bettacraft:" + AnimalIcons.body(s));
            assertNotNull(body);
            int opaque = 0, coloured = 0;
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                int c = body.getRGB(x, y);
                if (c >>> 24 == 0) continue;
                opaque++;
                int r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
                if (Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) > 6) coloured++;
            }
            assertTrue(opaque > 0, s.id() + ": empty body layer");
            assertEquals(AnimalIcons.tintedBody(s), coloured == 0, s.id() + ": body grey " + (coloured == 0) + ", tinted " + AnimalIcons.tintedBody(s));
        }
    }
}
