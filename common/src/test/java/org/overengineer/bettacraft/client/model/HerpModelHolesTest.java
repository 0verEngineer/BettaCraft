package org.overengineer.bettacraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M46: no hole in the models of the terrarium animals (frogs, geckos, the snake, the turtles) and of the small tank animals.
 * The leopard gecko looked grey from the side and one saw into it: the texture painter laid the box-UV faces out with the
 * cube sizes rounded up, while Minecraft maps the fractional sizes, so the faces of a cube like 4.2 x 2.4 x 8.6 sampled
 * transparent texels at their edges (cutout: holes, the grey insides showing). Every face of every cube of the baked models
 * (rest pose) must only sample texels with alpha > 0 in every texture of the model.
 */
class HerpModelHolesTest {

    private static Path textures(String folder) {
        return Path.of(System.getProperty("bettacraft.projectDir", "."), "src/main/resources/assets/bettacraft/textures/entity", folder);
    }

    /** The quads of a baked layer as {u0, v0, u1, v1} in texture units (0..1). */
    static List<float[]> quads(LayerDefinition layer) {
        ModelPart root = layer.bakeRoot();
        List<float[]> out = new ArrayList<>();
        float[][] uv = new float[4][];
        int[] n = {0};
        VertexConsumer capture = new VertexConsumer() {
            @Override
            public VertexConsumer addVertex(float x, float y, float z) {
                return this;
            }

            @Override
            public VertexConsumer setColor(int r, int g, int b, int a) {
                return this;
            }

            @Override
            public VertexConsumer setUv(float u, float v) {
                uv[n[0]++] = new float[]{u, v};
                if (n[0] == 4) {
                    float u0 = Float.MAX_VALUE, v0 = Float.MAX_VALUE, u1 = -Float.MAX_VALUE, v1 = -Float.MAX_VALUE;
                    for (float[] p : uv) {
                        u0 = Math.min(u0, p[0]);
                        v0 = Math.min(v0, p[1]);
                        u1 = Math.max(u1, p[0]);
                        v1 = Math.max(v1, p[1]);
                    }
                    out.add(new float[]{u0, v0, u1, v1});
                    n[0] = 0;
                }
                return this;
            }

            @Override
            public VertexConsumer setUv1(int u, int v) {
                return this;
            }

            @Override
            public VertexConsumer setUv2(int u, int v) {
                return this;
            }

            @Override
            public VertexConsumer setNormal(float x, float y, float z) {
                return this;
            }

            @Override
            public VertexConsumer setColor(int argb) {
                return this;
            }

            @Override
            public VertexConsumer setLineWidth(float width) {
                return this;
            }
        };
        root.render(new PoseStack(), capture, 0xF000F0, 0);
        return out;
    }

    /** The transparent texels the quads sample in {@code tex}, as a short report (empty: none). */
    static String holes(List<float[]> quads, BufferedImage tex) {
        int w = tex.getWidth(), h = tex.getHeight(), bad = 0, faces = 0;
        for (float[] q : quads) {
            int x0 = (int) Math.floor(q[0] * w + 1e-3), x1 = (int) Math.ceil(q[2] * w - 1e-3);
            int y0 = (int) Math.floor(q[1] * h + 1e-3), y1 = (int) Math.ceil(q[3] * h - 1e-3);
            int here = 0;
            for (int y = y0; y < Math.max(y1, y0 + 1); y++)
                for (int x = x0; x < Math.max(x1, x0 + 1); x++) {
                    if (x < 0 || y < 0 || x >= w || y >= h || (tex.getRGB(x, y) >>> 24) == 0) here++;
                }
            if (here > 0) {
                faces++;
                bad += here;
            }
        }
        return bad == 0 ? "" : String.format(Locale.ROOT, "%d transparent texels on %d of %d faces", bad, faces, quads.size());
    }

    /** The model a texture belongs to: the longest model name it starts with ({@code dart_frog_tadpole} before {@code dart_frog}). */
    static String modelOf(String texture, String[] models) {
        String best = null;
        for (String m : models) {
            if ((texture.equals(m) || texture.startsWith(m + "_")) && (best == null || m.length() > best.length())) best = m;
        }
        return best;
    }

    @Test
    void terrariumAnimalsHaveNoHoles() throws IOException {
        StringBuilder bad = new StringBuilder();
        int checked = 0;
        try (Stream<Path> files = Files.list(textures("terrarium"))) {
            for (Path f : files.filter(p -> p.toString().endsWith(".png")).sorted().toList()) {
                String name = f.getFileName().toString().replace(".png", "");
                String model = modelOf(name, TerrariumModelDefinitions.MODELS);
                if (model == null) continue;
                String r = holes(quads(TerrariumModelDefinitions.create(model)), ImageIO.read(f.toFile()));
                checked++;
                if (!r.isEmpty()) bad.append("\n  ").append(name).append(" (").append(model).append("): ").append(r);
            }
        }
        assertTrue(checked >= 20, "textures checked: " + checked);
        assertTrue(bad.isEmpty(), "holes in the terrarium animals:" + bad);
    }

    @Test
    void leopardGeckoMorphsAreClosed() throws IOException {
        // the case of the report: all three morphs (the young use the same model and texture, smaller)
        List<float[]> q = quads(TerrariumModelDefinitions.create("leopard_gecko"));
        assertFalse(q.isEmpty());
        for (String morph : new String[]{"normal", "tangerine", "albino"}) {
            String r = holes(q, ImageIO.read(textures("terrarium").resolve("leopard_gecko_" + morph + ".png").toFile()));
            assertTrue(r.isEmpty(), "leopard_gecko_" + morph + ": " + r);
        }
    }

    @Test
    void smallTankAnimalsHaveNoHoles() throws IOException {
        // the base textures (the overlays of lines, bands and dots are transparent where they draw nothing, by design)
        StringBuilder bad = new StringBuilder();
        for (String m : CritterModelDefinitions.MODELS) {
            Path f = textures("critters").resolve(m + ".png");
            if (!Files.exists(f)) continue;
            String r = holes(quads(CritterModelDefinitions.create(m)), ImageIO.read(f.toFile()));
            if (!r.isEmpty()) bad.append("\n  ").append(m).append(": ").append(r);
        }
        assertTrue(bad.isEmpty(), "holes in the small animals:" + bad);
    }
}
