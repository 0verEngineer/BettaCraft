package org.overengineer.bettacraft.entity.betta;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.client.model.BettaModelDefinitions;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Angelfish fix (1.33.3): the dorsal fin height of {@link BettaExtents#top} (blocks for an adult, times the size setting
 * at run time) covers the real betta models at {@link BettaModelDefinitions#ADULT_RENDER_SCALE}.
 */
class BettaExtentsTopTest {

    @Test
    void dorsalHeightCoversEveryTailShape() {
        StringBuilder report = new StringBuilder(), bad = new StringBuilder();
        for (BettaModelDefinitions.Shape s : BettaModelDefinitions.Shape.values()) {
            ModelPart root = BettaModelDefinitions.createBodyLayer(s).bakeRoot();
            double[] max = {-1e9};
            root.visit(new PoseStack(), (pose, path, index, cube) -> {
                for (int i = 0; i < 8; i++) {
                    Vector3f v = new Vector3f((i & 1) == 0 ? cube.minX : cube.maxX, (i & 2) == 0 ? cube.minY : cube.maxY,
                            (i & 4) == 0 ? cube.minZ : cube.maxZ).div(16F);
                    pose.pose().transformPosition(v);
                    max[0] = Math.max(max[0], 24 - v.y * 16);
                }
            });
            double real = max[0] / 16 * BettaModelDefinitions.ADULT_RENDER_SCALE;
            double table = BettaExtents.top(s.name().toLowerCase(Locale.ROOT));
            report.append(String.format(Locale.ROOT, "%s real %.3f table %.3f%n", s, real, table));
            if (table + 0.005 < real) bad.append(s).append(' ');
        }
        System.out.println(report);
        assertTrue(bad.isEmpty(), "betta dorsal heights below the models: " + bad + "\n" + report);
    }
}
