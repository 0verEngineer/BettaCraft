package org.overengineer.bettacraft.terrarium;

import net.minecraft.server.level.ServerLevel;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;

/**
 * M49 part b: the mudskipper ({@link HerpSpecies#MUDSKIPPER}, switch {@code features.brackish.mudskipper}) lives in a brackish
 * paludarium: land to skip over and roots to climb (it climbs like the crested gecko, also the stilt roots of a mangrove or the
 * mangrove root decoration), water to wet its skin. In fresh water it is stressed ({@link TerrariumNeeds.Problem#FRESH_WATER}).
 */
public final class Mudskippers {
    private Mudskippers() {}

    /** The water of the tank is fresh (the mudskipper needs brackish water). */
    public static boolean fresh(TankData t) {
        return t.waterMb() > 0 && Salinity.kind(t.salinity()) == Salinity.Kind.FRESH;
    }

    /** A mudskipper lives in this tank as it should: a paludarium with brackish water. */
    public static boolean happy(ServerLevel level, TankData t) {
        if (t.terrariumMode() != TerrariumMode.PALUDARIUM || t.waterMb() <= 0 || fresh(t)) return false;
        for (var e : TankInhabitants.liveInhabitants(level, t)) if (e instanceof HerpEntity h && h.species() == HerpSpecies.MUDSKIPPER) return true;
        return false;
    }
}
