package org.overengineer.bettacraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.entity.tankmate.TankmateExtents;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Angelfish fix (1.33.3): the server keeps a tankmate's rendered top (dorsal fin) below the water surface with the
 * height table of {@link TankmateExtents}; it must cover the real model geometry (rest pose and the dorsal fin's
 * swimming tilt) and must not be far too large (or the fish would keep needlessly away from the surface).
 */
class TankmateExtentsTest {

    /** Highest point of the baked model above the entity position in model pixels, dorsal tilted by {@code dorsalX}. */
    static double top(LayerDefinition layer, float dorsalX) {
        ModelPart root = layer.bakeRoot();
        ModelPart fish = root.getChild("fish");
        if (fish.hasChild("dorsal")) fish.getChild("dorsal").xRot = dorsalX;
        double[] max = {-1e9};
        root.visit(new PoseStack(), (pose, path, index, cube) -> {
            for (int i = 0; i < 8; i++) {
                // the part poses translate in blocks (offset / 16), the cubes are in pixels
                Vector3f v = new Vector3f((i & 1) == 0 ? cube.minX : cube.maxX, (i & 2) == 0 ? cube.minY : cube.maxY,
                        (i & 4) == 0 ? cube.minZ : cube.maxZ).div(16F);
                pose.pose().transformPosition(v);
                // the root offsets the fish by 24 px (the feet); model y grows downwards
                max[0] = Math.max(max[0], 24 - v.y * 16);
            }
        });
        return max[0];
    }

    /** The dorsal fin tilts between -0.1 (resting) and +0.05 (swimming) in {@link TankmateModel}. */
    static double animatedTop(LayerDefinition layer) {
        return Math.max(Math.max(top(layer, 0F), top(layer, -0.1F)), top(layer, 0.05F));
    }

    @Test
    void heightTableCoversEveryModel() {
        StringBuilder report = new StringBuilder(), bad = new StringBuilder();
        for (TankmateSpecies.Model m : TankmateSpecies.Model.values()) {
            double real = animatedTop(TankmateModelDefinitions.create(m.name().toLowerCase(Locale.ROOT)));
            // the guppies and endlers of the livebearer genetics use their own model (every tail shape at full size)
            if (m == TankmateSpecies.Model.LIVEBEARER) real = Math.max(real, animatedTop(GuppyModelDefinitions.createLayer()));
            double table = TankmateExtents.topPixels(m);
            report.append(String.format(Locale.ROOT, "%s model %.2f px, table %.2f px%n", m, real, table));
            // covers the model, but at most 1.5 px more
            if (table < real || table > real + 1.5) bad.append(m).append(' ');
        }
        assertTrue(bad.isEmpty(), "height table does not match the models: " + bad + "\n" + report);
    }

    @Test
    void angelfishFinReachesFarAboveItsHitbox() {
        // the cause of the bug: the adult angelfish is drawn about 1.1 blocks high above its feet, its hitbox is 0.6
        double model = TankmateExtents.topPixels(TankmateSpecies.Model.ANGEL) / 16.0 * TankmateSpecies.ANGELFISH.length() / 2.0;
        assertTrue(model > TankmateSpecies.ANGELFISH.hitboxHeight() * 1.5, "angelfish model top " + model);
    }
}
