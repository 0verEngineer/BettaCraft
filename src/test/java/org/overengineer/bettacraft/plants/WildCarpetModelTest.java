package org.overengineer.bettacraft.plants;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M46: the wild carpets (fine, mat, clover, needle, each grown and trimmed) are cushions of several heights on a ground layer
 * with a ragged rim instead of one flat plate. Their models must be valid block models: elements inside the allowed space,
 * only the rotations Minecraft accepts, uv inside the texture, every texture variable the template uses given by each carpet
 * model and every texture there; the cushions have several heights and the grown carpet stands higher than the trimmed one;
 * the ground texture has a soft rim (open texels at the block edge) and the cushion tops are closed.
 */
class WildCarpetModelTest {
    private static final List<String> LOOKS = List.of("fine", "mat", "clover", "needle");
    private static final Set<Double> ANGLES = Set.of(-45.0, -22.5, 0.0, 22.5, 45.0);

    private static Path res(String set) {
        return Path.of(System.getProperty("bettacraft.projectDir", "."), "src", set, "resources", "assets", "bettacraft");
    }

    private static JsonObject json(Path p) throws IOException {
        return JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static double[] vec(JsonElement e) {
        JsonArray a = e.getAsJsonArray();
        return new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()};
    }

    /** Checks one template; returns the texture variables it uses (without '#') and the top heights of its cushions. */
    private static double[] checkTemplate(String name, Set<String> vars) throws IOException {
        JsonObject m = json(res("main").resolve("models/block/" + name + ".json"));
        assertEquals("minecraft:cutout", m.get("render_type").getAsString(), name);
        Set<Double> tops = new HashSet<>();
        double max = 0;
        int cushions = 0;
        for (JsonElement el : m.getAsJsonArray("elements")) {
            JsonObject e = el.getAsJsonObject();
            double[] fr = vec(e.get("from")), to = vec(e.get("to"));
            for (int i = 0; i < 3; i++) {
                assertTrue(fr[i] >= -16 && to[i] <= 32 && fr[i] <= to[i], name + ": element outside the model space " + e);
            }
            if (e.has("rotation")) {
                double a = e.getAsJsonObject("rotation").get("angle").getAsDouble();
                assertTrue(ANGLES.contains(a), name + ": rotation " + a + " is not allowed");
            }
            for (Map.Entry<String, JsonElement> f : e.getAsJsonObject("faces").entrySet()) {
                JsonObject face = f.getValue().getAsJsonObject();
                String tex = face.get("texture").getAsString();
                assertTrue(tex.startsWith("#"), name + ": " + tex);
                vars.add(tex.substring(1));
                if (face.has("uv")) for (JsonElement u : face.getAsJsonArray("uv")) {
                    assertTrue(u.getAsDouble() >= 0 && u.getAsDouble() <= 16, name + ": uv outside the texture " + face);
                }
            }
            if (e.getAsJsonObject("faces").has("up") && "#cushion".equals(e.getAsJsonObject("faces").getAsJsonObject("up").get("texture").getAsString())) {
                cushions++;
                tops.add(to[1]);
                max = Math.max(max, to[1]);
            }
        }
        assertTrue(cushions >= 4, name + ": cushions " + cushions);
        assertTrue(tops.size() >= 4, name + ": the cushions have only " + tops.size() + " heights");
        return new double[]{max, cushions};
    }

    @Test
    void carpetModelsAreValid() throws IOException {
        Set<String> vars = new HashSet<>();
        double grown = checkTemplate("template_wild_carpet", vars)[0];
        double trimmed = checkTemplate("template_wild_carpet_short", vars)[0];
        assertTrue(grown > trimmed && grown >= 2.5 && trimmed > 0.5, "heights grown " + grown + ", trimmed " + trimmed);
        assertTrue(vars.containsAll(Set.of("cushion", "side", "ground")), "variables " + vars);
        for (String look : LOOKS) {
            for (String sfx : new String[]{"", "_trimmed"}) {
                Path p = res("generated").resolve("models/block/wild_carpet_" + look + sfx + ".json");
                assertTrue(Files.exists(p), "missing " + p);
                JsonObject m = json(p);
                String parent = m.get("parent").getAsString();
                assertEquals("bettacraft:block/template_wild_carpet" + (sfx.isEmpty() ? "" : "_short"), parent, p.toString());
                JsonObject t = m.getAsJsonObject("textures");
                for (String v : vars) {
                    assertTrue(t.has(v), p.getFileName() + " does not give #" + v);
                    String id = t.get(v).getAsString();
                    Path png = res("main").resolve("textures/" + id.substring(id.indexOf(':') + 1) + ".png");
                    assertTrue(Files.exists(png), p.getFileName() + ": missing texture " + png);
                }
            }
        }
    }

    @Test
    void groundHasASoftRimAndCushionsAreClosed() throws IOException {
        for (String look : LOOKS) {
            BufferedImage ground = ImageIO.read(res("main").resolve("textures/block/wild_carpet_" + look + "_ground.png").toFile());
            BufferedImage cushion = ImageIO.read(res("main").resolve("textures/block/wild_carpet_" + look + "_cushion.png").toFile());
            assertNotNull(ground);
            int open = 0, edge = 0;
            for (int i = 0; i < 16; i++) {
                for (int[] p : new int[][]{{i, 0}, {i, 15}, {0, i}, {15, i}}) {
                    edge++;
                    if ((ground.getRGB(p[0], p[1]) >>> 24) == 0) open++;
                }
            }
            assertTrue(open > edge / 5 && open < edge * 4 / 5, look + ": ragged rim " + open + " of " + edge);
            int min = 255, max = 0;
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                int argb = cushion.getRGB(x, y);
                assertTrue((argb >>> 24) == 255, look + ": cushion top not closed at " + x + "," + y);
                min = Math.min(min, argb & 0xFF);
                max = Math.max(max, argb & 0xFF);
            }
            assertTrue(max - min > 60, look + ": the cushion top has no leaf pattern " + min + ".." + max);
        }
    }
}
