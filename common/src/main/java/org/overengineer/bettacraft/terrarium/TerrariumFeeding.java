package org.overengineer.bettacraft.terrarium;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.overengineer.bettacraft.food.BettaFood;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;

/**
 * M31: the terrarium animals at the tank's food ({@code TankCare#feed}: by hand, from the feeder): every animal that
 * eats the kind of food gets a portion, the hungriest first, after the fish and frogs.
 */
public final class TerrariumFeeding {
    private TerrariumFeeding() {}

    /** The terrarium animals of the tank that eat this food, hungriest first. */
    public static List<Entity> eaters(ServerLevel level, TankData t, BettaFood.Kind kind) {
        List<Entity> out = new ArrayList<>();
        if (kind == null) return out;
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) {
            if (e instanceof TerrariumAnimal a && a.eatsFood(kind)) out.add(e);
        }
        out.sort(Comparator.comparingDouble((Entity e) -> ((TerrariumAnimal) e).daysSinceFed()).reversed());
        return out;
    }

    /** Feeds up to {@code portions} of them (one portion each); returns how many ate. */
    public static int feed(List<Entity> eaters, int portions, @Nullable Set<UUID> round) {
        int n = 0;
        for (Entity e : eaters) {
            if (n >= portions) break;
            ((TerrariumAnimal) e).feed();
            if (round != null) round.add(e.getUUID());
            n++;
        }
        return n;
    }
}
