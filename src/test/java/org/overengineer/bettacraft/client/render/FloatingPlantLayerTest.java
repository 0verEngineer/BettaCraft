package org.overengineer.bettacraft.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.tank.plants.FloatingPlant;
import org.overengineer.bettacraft.tank.plants.FloatingPlantSim;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;

/** M21.1: overlapping floating plants (lily pads) never lie at the same height, and new pads are put beside the others. */
class FloatingPlantLayerTest {
    @Test
    void overlappingPlantsGetHeightsOfTheirOwn() {
        List<FloatingPlant> pads = new ArrayList<>();
        for (int i = 0; i < FloatingPlantRenderer.LAYERS; i++) pads.add(new FloatingPlant(i, FloatingPlantType.LILY_PAD, 3 + i * 0.1, 4 + (i % 3) * 0.07, 1F, 0F));
        pads.sort(Comparator.comparingLong(FloatingPlantRenderer::orderKey));
        Set<Float> heights = new HashSet<>();
        for (int i = 0; i < pads.size(); i++) heights.add(FloatingPlantRenderer.layerOffset(i));
        assertEquals(pads.size(), heights.size(), "every pad of the tank at a height of its own");
        // the leaves of one plant stay within its step (they never reach the next plant's height)
        assertTrue(7 * FloatingPlantRenderer.LEAF_STEP < FloatingPlantRenderer.LAYER);
        // fixed pads: the order comes from the position and does not depend on the list order or the ids
        FloatingPlant a = new FloatingPlant(1, FloatingPlantType.LILY_PAD, 2.5, 2.5, 1F, 0F);
        FloatingPlant b = new FloatingPlant(99, FloatingPlantType.LILY_PAD, 2.5, 2.5, 1F, 0F);
        assertEquals(FloatingPlantRenderer.orderKey(a), FloatingPlantRenderer.orderKey(b));
        // at most about 3.6 hundredths of a block above the water: less than a pixel
        assertTrue(FloatingPlantRenderer.layerOffset(FloatingPlantRenderer.LAYERS - 1) + 7 * FloatingPlantRenderer.LEAF_STEP < 1 / 16F);
    }

    @Test
    void newLilyPadsArePutBesideTheOthers() {
        List<FloatingPlant> pads = new ArrayList<>();
        pads.add(new FloatingPlant(1, FloatingPlantType.LILY_PAD, 5, 5, 1F, 0F));
        double r = FloatingPlantType.LILY_PAD.radius(1.0);
        double[] at = FloatingPlantSim.awayFromPads(pads, 5.05, 5, r);
        double d = Math.hypot(at[0] - 5, at[1] - 5);
        assertTrue(d > 0.3 && d <= 0.05 + r + 1e-9, "pushed aside, but not far from where it was aimed: " + d);
        // right on top of another one: still moved
        double[] same = FloatingPlantSim.awayFromPads(pads, 5, 5, r);
        assertTrue(Math.hypot(same[0] - 5, same[1] - 5) > 0.2);
        // far enough away: unchanged
        double[] free = FloatingPlantSim.awayFromPads(pads, 7, 7, r);
        assertEquals(7, free[0], 1e-9);
        assertEquals(7, free[1], 1e-9);
        // other plants do not push it
        List<FloatingPlant> weed = List.of(new FloatingPlant(2, FloatingPlantType.DUCKWEED, 5, 5, 1F, 0F));
        assertEquals(5, FloatingPlantSim.awayFromPads(weed, 5, 5, r)[0], 1e-9);
    }
}
