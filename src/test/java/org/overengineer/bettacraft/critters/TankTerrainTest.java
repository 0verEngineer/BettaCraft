package org.overengineer.bettacraft.critters;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.tank.TankData;

/** M14: the movement on a real tank's terrain (far from the origin, negative coordinates), glass and clutches. */
class TankTerrainTest {
    private static TankData tank(int x0, int y0, int z0, int sx, int sy, int sz, long water) {
        List<BlockPos> b = new ArrayList<>();
        for (int x = 0; x < sx; x++) for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) b.add(new BlockPos(x0 + x, y0 + y, z0 + z));
        TankData t = TankData.create(b, 16);
        t.setWaterMb(water);
        return t;
    }

    @Test
    void snailsReachTheGlassAndAppleSnailsLayAboveTheWater() {
        TankData t = tank(12830312, -58, -7902484, 3, 2, 2, 12000);
        TankTerrain terrain = new TankTerrain(t, null, null, true);
        assertTrue(Double.isFinite(terrain.airTop()), "air above the water");
        Random rnd = new Random(1);
        Critter a = new Critter(1, CritterSpecies.APPLE_SNAIL, true, CritterGenes.WILD, 12830313.5, -58 + TankTerrain.MARGIN, -7902483.0);
        a.age = 10;
        a.eggs = 0;
        Critter n = new Critter(2, CritterSpecies.ZEBRA_NERITE, true, CritterGenes.WILD, 12830313.5, -58 + TankTerrain.MARGIN, -7902483.0);
        n.age = 10;
        boolean above = false, nGlass = false;
        for (int s = 0; s < 4000 && !(above && nGlass); s++) {
            CritterMovement.step(a, terrain, rnd);
            CritterMovement.step(n, terrain, rnd);
            above |= a.onGlass() && a.y > terrain.surface() + 0.005;
            nGlass |= n.onGlass();
        }
        assertTrue(nGlass, "nerite on the glass");
        assertTrue(above, "apple snail above the water: y " + a.y + " surface " + terrain.surface() + " face " + a.face);
    }
}
