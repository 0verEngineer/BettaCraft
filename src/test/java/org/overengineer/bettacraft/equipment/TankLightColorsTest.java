package org.overengineer.bettacraft.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;

/** M18: colours of the aquarium light, their blend and the tint of the water. */
class TankLightColorsTest {
    private static final int WATER = 0xFF4A90E2;

    @Test
    void sixteenDistinctColoursWhiteIsNoTint() {
        Set<Integer> seen = new HashSet<>();
        for (DyeColor c : DyeColor.values()) seen.add(TankLightColors.rgb(c));
        assertEquals(16, seen.size(), "16 different light colours");
        assertEquals(0xFFFFFF, TankLightColors.rgb(DyeColor.WHITE));
        assertEquals(0xFFFFFF, TankLightColors.panel(DyeColor.WHITE, true), "white panel: look of earlier versions");
        assertEquals(0xFFFFFF, TankLightColors.panel(DyeColor.WHITE, false));
    }

    @Test
    void blendOfLitLights() {
        assertEquals(0, TankLightColors.blend(List.of()), "no light: no tint");
        assertEquals(0, TankLightColors.blend(List.of(DyeColor.WHITE, DyeColor.WHITE)), "only white: no tint");
        assertEquals(TankLightColors.rgb(DyeColor.RED), TankLightColors.blend(List.of(DyeColor.RED, DyeColor.RED)));
        int half = TankLightColors.blend(List.of(DyeColor.RED, DyeColor.WHITE));
        assertEquals(0xFF, (half >> 16) & 0xFF, "red channel stays");
        assertTrue(((half >> 8) & 0xFF) > 0x46, "white dilutes the red");
    }

    @Test
    void waterTint() {
        assertEquals(WATER, TankLightColors.tintWater(WATER, 0, 0.5F), "no light colour");
        assertEquals(WATER, TankLightColors.tintWater(WATER, 0xFF4646, 0F), "strength 0");
        int red = TankLightColors.tintWater(WATER, TankLightColors.rgb(DyeColor.RED), 0.5F);
        assertNotEquals(WATER, red);
        assertEquals(0xFF000000, red & 0xFF000000, "alpha kept");
        assertTrue(((red >> 16) & 0xFF) > ((WATER >> 16) & 0xFF), "redder");
        // the brightness of the water stays about the same for every colour
        for (DyeColor c : DyeColor.values()) {
            int t = TankLightColors.tintWater(WATER, TankLightColors.rgb(c), 1F);
            assertTrue(Math.abs(TankLightColors.luma(t) - TankLightColors.luma(WATER)) < 30, c + ": " + Integer.toHexString(t));
        }
    }
}
