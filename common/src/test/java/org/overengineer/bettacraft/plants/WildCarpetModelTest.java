package org.overengineer.bettacraft.plants;

import org.overengineer.bettacraft.util.NbtCompat;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M55 (M46 before): the wild carpets (fine, mat, clover, needle, each grown and trimmed) have three designs, each a template
 * of its own ({@code template_wild_carpet_<design>}: a dense lawn of leaf layers, loose cushions of leafy discs with runners,
 * fine sprigs standing apart); the shared templates only name the design in use as their parent. Every design must be a
 * valid block model (elements inside the allowed space, only the rotations Minecraft accepts, uv inside the texture), the
 * grown carpet stands higher than the trimmed one, every carpet model gives every texture variable of every design and every
 * texture exists; the sprigs stand in both bands of their texture, the lawn's ground is closed and the sprigs' ground
 * leaves the bed showing.
 */
class WildCarpetModelTest {
    private static final List<String> LOOKS = List.of("fine", "mat", "clover", "needle");
    private static final List<String> DESIGNS = List.of("lawn", "cushions", "blades");
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

    /** Checks one template; adds the texture variables it uses (without '#'); returns the highest top of its elements. */
    private static double checkTemplate(String name, Set<String> vars) throws IOException {
        JsonObject m = json(res("main").resolve("models/block/" + name + ".json"));
        assertEquals("minecraft:cutout", m.get("render_type").getAsString(), name);
        double max = 0;
        int elements = 0;
        for (JsonElement el : m.getAsJsonArray("elements")) {
            JsonObject e = el.getAsJsonObject();
            elements++;
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
            max = Math.max(max, to[1]);
        }
        assertTrue(elements >= 4, name + ": only " + elements + " elements");
        return max;
    }

    @Test
    void carpetModelsAreValid() throws IOException {
        Set<String> vars = new HashSet<>();
        for (String d : DESIGNS) {
            double grown = checkTemplate("template_wild_carpet_" + d, vars);
            double trimmed = checkTemplate("template_wild_carpet_" + d + "_short", vars);
            assertTrue(grown > trimmed && grown >= 2 && trimmed > 0.5, d + ": heights grown " + grown + ", trimmed " + trimmed);
        }
        // the shared templates name one design (both the same)
        String grown = json(res("main").resolve("models/block/template_wild_carpet.json")).get("parent").getAsString();
        String trimmed = json(res("main").resolve("models/block/template_wild_carpet_short.json")).get("parent").getAsString();
        assertTrue(DESIGNS.stream().anyMatch(d -> grown.equals("bettacraft:block/template_wild_carpet_" + d)), grown);
        assertEquals(grown + "_short", trimmed);
        for (String look : LOOKS) {
            for (String sfx : new String[]{"", "_trimmed"}) {
                Path p = res("generated").resolve("models/block/wild_carpet_" + look + sfx + ".json");
                assertTrue(Files.exists(p), "missing " + p);
                JsonObject m = json(p);
                assertEquals("bettacraft:block/template_wild_carpet" + (sfx.isEmpty() ? "" : "_short"), m.get("parent").getAsString(), p.toString());
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

    private static double cover(BufferedImage img, int y0, int y1) {
        int n = 0, open = 0;
        for (int y = y0; y < y1; y++) for (int x = 0; x < img.getWidth(); x++) {
            n++;
            if ((img.getRGB(x, y) >>> 24) != 0) open++;
        }
        return open / (double) n;
    }

    @Test
    void sprigsStandAndTheGroundShows() throws IOException {
        for (String look : LOOKS) {
            BufferedImage sprig = ImageIO.read(res("main").resolve("textures/block/wild_carpet_" + look + "_sprig.png").toFile());
            for (int col = 0; col < 4; col++) {
                for (int bottom : new int[]{7, 15}) {
                    boolean foot = false;
                    for (int x = col * 4; x < col * 4 + 4; x++) foot |= (sprig.getRGB(x, bottom) >>> 24) != 0;
                    assertTrue(foot, look + ": the sprig in column " + col + " does not stand on row " + bottom);
                }
            }
            double ground = cover(ImageIO.read(res("main").resolve("textures/block/wild_carpet_" + look + "_blade_ground.png").toFile()), 0, 16);
            assertTrue(ground > 0.2 && ground < 0.8, look + ": the ground between the sprigs covers " + ground);
            double lawn = cover(ImageIO.read(res("main").resolve("textures/block/wild_carpet_" + look + "_lawn_ground.png").toFile()), 0, 16);
            assertEquals(1.0, lawn, 1e-9, look + ": the lawn's ground is not closed");
        }
    }
}
