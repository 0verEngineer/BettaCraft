package org.overengineer.bettacraft.client.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M62: the shells of the snails stay on their bodies. A snail stands on y = 0 (its sole; the model's y points down) and lies
 * with the sole against the glass when it clings to it, so no corner of a shell box may reach below -0.5 (the shells used to
 * poke through the sole and the glass: the lowest whorl of the Tylomelania hung 0.3 px below it, the trumpet snail's 0.03 px).
 * The Tylomelania's shell is also no wider than its foot (it was 5.4 px against a 3.8 px foot) and rests on top of it. The
 * corners are computed from the generated layer definitions (box, grow, offset, x rotation like {@code ModelPart}).
 */
class SnailShellExtentsTest {
    private static final Pattern LAYER = Pattern.compile("public static LayerDefinition create(\\w+)Layer\\(\\) \\{(.*?)\\n    }", Pattern.DOTALL);
    private static final Pattern CUBE = Pattern.compile("g_(\\w+)\\.addOrReplaceChild\\(\"(\\w+)\", CubeListBuilder\\.create\\(\\)\\.texOffs\\(\\d+, \\d+\\)"
            + "\\.addBox\\((-?[\\d.]+)F, (-?[\\d.]+)F, (-?[\\d.]+)F, ([\\d.]+)F, ([\\d.]+)F, ([\\d.]+)F"
            + "(?:, new CubeDeformation\\((-?[\\d.]+)F, (-?[\\d.]+)F, (-?[\\d.]+)F\\))?\\), "
            + "PartPose\\.offsetAndRotation\\((-?[\\d.]+)F, (-?[\\d.]+)F, (-?[\\d.]+)F, (-?[\\d.]+)F, (-?[\\d.]+)F, (-?[\\d.]+)F\\)\\);");
    private static final List<String> SNAILS = List.of("Nerite", "Apple", "Ramshorn", "Bladder", "Trumpet", "Assassin", "Turbo", "Tylomelania");

    /** A box's corners in the model: {x, y, z} each. */
    private record Box(String group, String name, List<double[]> corners) {
        double max(int axis) {
            return corners.stream().mapToDouble(c -> c[axis]).max().orElseThrow();
        }

        double min(int axis) {
            return corners.stream().mapToDouble(c -> c[axis]).min().orElseThrow();
        }
    }

    private static Map<String, List<Box>> layers() throws IOException {
        Path p = Path.of(System.getProperty("bettacraft.projectDir", "."), "src", "main", "java", "org", "overengineer", "bettacraft", "client", "model",
                "CritterModelDefinitions.java");
        String src = Files.readString(p, StandardCharsets.UTF_8);
        Map<String, List<Box>> out = new LinkedHashMap<>();
        Matcher l = LAYER.matcher(src);
        while (l.find()) {
            List<Box> boxes = new ArrayList<>();
            Matcher m = CUBE.matcher(l.group(2));
            while (m.find()) {
                double[] v = new double[15];
                for (int i = 0; i < 15; i++) {
                    String g = m.group(i + 3);
                    v[i] = g == null ? 0 : Double.parseDouble(g);
                }
                // box from (v0..v2) with size (v3..v5), grown by (v6..v8), offset (v9..v11), rotation x (v12; y, z unused by the snails)
                double x0 = v[0] - v[6], y0 = v[1] - v[7], z0 = v[2] - v[8];
                double x1 = v[0] + v[3] + v[6], y1 = v[1] + v[4] + v[7], z1 = v[2] + v[5] + v[8];
                assertTrue(v[13] == 0 && v[14] == 0 || !m.group(1).equals("shell"), "only x rotations on the shells");
                double c = Math.cos(v[12]), s = Math.sin(v[12]);
                List<double[]> corners = new ArrayList<>();
                for (double x : new double[]{x0, x1}) for (double y : new double[]{y0, y1}) for (double z : new double[]{z0, z1}) {
                    corners.add(new double[]{v[9] + x, v[10] + y * c - z * s, v[11] + y * s + z * c});
                }
                boxes.add(new Box(m.group(1), m.group(2), corners));
            }
            out.put(l.group(1), boxes);
        }
        return out;
    }

    @Test
    void shellsStayAboveTheSole() throws IOException {
        Map<String, List<Box>> layers = layers();
        List<String> bad = new ArrayList<>();
        for (String snail : SNAILS) {
            List<Box> boxes = layers.get(snail);
            assertTrue(boxes != null && !boxes.isEmpty(), "no layer of " + snail);
            boolean shell = false;
            for (Box b : boxes) {
                if (!b.group().equals("shell")) continue;
                shell = true;
                if (b.max(1) > -0.5 + 1e-4) bad.add(snail + "." + b.name() + " reaches y " + b.max(1));
            }
            assertTrue(shell, snail + " has no shell boxes");
        }
        assertTrue(bad.isEmpty(), "shell boxes through the sole (y > -0.5): " + bad);
    }

    @Test
    void tylomelaniaShellRestsOnItsFoot() throws IOException {
        List<Box> boxes = layers().get("Tylomelania");
        Box foot = boxes.stream().filter(b -> b.name().equals("foot")).findFirst().orElseThrow();
        double footTop = foot.min(1), footHalf = foot.max(0);
        List<Box> shell = boxes.stream().filter(b -> b.group().equals("shell")).toList();
        assertFalse(shell.isEmpty());
        for (Box b : shell) {
            assertTrue(b.max(1) <= footTop + 0.1, b.name() + " reaches y " + b.max(1) + " below the top of the foot " + footTop);
            assertTrue(Math.max(b.max(0), -b.min(0)) <= footHalf + 1e-4, b.name() + " is wider than the foot: " + b.max(0) + " > " + footHalf);
        }
        // the tall spire of a rabbit snail (it lies back at a low angle, but the apex still stands high)
        double highest = shell.stream().mapToDouble(b -> b.min(1)).min().orElseThrow();
        assertTrue(highest < -7, "a tall spire (top at y " + highest + ")");
    }
}
