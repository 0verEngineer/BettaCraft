package org.overengineer.bettacraft.big;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.BigTankRules;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M30: the big animals and their size rule ({@link BigTankRules}) in the game: which animals are big and how long they are,
 * and whether a tank takes one more. Big are our big species ({@link org.overengineer.bettacraft.tankmates.LargeSpecies}, by
 * their real length) and the animals of {@code #bettacraft:big_tank_animals} (the vanilla dolphin; other mods can add theirs),
 * whose length is estimated from their hitbox ({@link #METRES_PER_HITBOX_BLOCK}: a dolphin, 0.9 blocks wide, is 2.6 m).
 * Everything else is sorted by the hitbox limit of M16 as before (small animals into any tank, whales and bosses never).
 */
public final class BigAnimals {
    /** Metres of animal per block of its larger hitbox side (animals of the tag). */
    public static final double METRES_PER_HITBOX_BLOCK = 2.9;
    /** A dolphin only goes into sea water from this salinity on (ppt). */
    public static final float DOLPHIN_MIN_SALT = 25;

    private BigAnimals() {}

    /** The length in metres of a big animal of this type (the instance's size if there is one), 0 if it is no big animal. */
    public static double length(EntityType<?> type, @Nullable Entity e) {
        TankmateSpecies sp = TankmateSpecies.of(type);
        if (sp != null) return sp.isLarge() ? sp.large().lengthM() : 0;
        if (!type.is(ModTags.BIG_TANK_ANIMALS)) return 0;
        float w = e != null ? e.getBbWidth() : type.getDimensions().width(), h = e != null ? e.getBbHeight() : type.getDimensions().height();
        // a young one is judged by its adult size (it grows in the tank)
        if (e instanceof net.minecraft.world.entity.LivingEntity le && le.isBaby()) {
            w = type.getDimensions().width();
            h = type.getDimensions().height();
        }
        return Math.max(0.5, Math.max(w, h) * METRES_PER_HITBOX_BLOCK);
    }

    public static boolean isBig(Entity e) {
        return length(e.getType(), e) > 0;
    }

    /** The factors of the config (defaults before it is loaded). */
    public static BigTankRules.Factors factors() {
        return new BigTankRules.Factors(ServerConfig.getOr(ServerConfig.BIG_LONG_SIDE, 12.0), ServerConfig.getOr(ServerConfig.BIG_SHORT_SIDE, 6.0),
                ServerConfig.getOr(ServerConfig.BIG_HEIGHT, 3.0), ServerConfig.getOr(ServerConfig.BIG_WATER, 100.0));
    }

    public static BigTankRules.Need need(double lengthM) {
        return BigTankRules.need(lengthM, factors());
    }

    /** Water blocks the big animals of {@code live} need together (without {@code except}). */
    public static double waterOfOthers(List<Entity> live, @Nullable Entity except) {
        double sum = 0;
        for (Entity e : live) {
            if (e == except) continue;
            double l = length(e.getType(), e);
            if (l > 0) sum += BigTankRules.water(l, factors());
        }
        return sum;
    }

    /** Water blocks of the full tank. */
    public static double capacityBlocks(TankData t) {
        return t.capacityMb() / (double) TankData.MB_PER_BLOCK;
    }

    /**
     * Whether the tank takes this big animal (feature switch, size, water for it and the big ones already in it, a dolphin
     * only in sea water): null if it does, else the message that names what is missing.
     */
    @Nullable
    public static Component refusal(TankData t, List<Entity> live, EntityType<?> type, @Nullable Entity e) {
        if (!Features.enabled(Feature.BIG_ANIMALS)) return Component.translatable("message.bettacraft.tank.too_big", type.getDescription());
        double len = length(type, e);
        if (len <= 0) return null;
        BigTankRules.Factors f = factors();
        BigTankRules.Need n = BigTankRules.need(len, f);
        double others = waterOfOthers(live, e);
        BigTankRules.Verdict v = BigTankRules.check(t.sizeX(), t.sizeY(), t.sizeZ(), capacityBlocks(t), others, len, f);
        if (v == BigTankRules.Verdict.TOO_SMALL) {
            return Component.translatable("message.bettacraft.big.too_small", type.getDescription(), n.longSide(), n.shortSide(), n.height(),
                    Math.max(t.sizeX(), t.sizeZ()), Math.min(t.sizeX(), t.sizeZ()), t.sizeY());
        }
        if (v == BigTankRules.Verdict.TOO_LITTLE_WATER) {
            return Component.translatable("message.bettacraft.big.too_little_water", type.getDescription(),
                    (int) Math.ceil(others + BigTankRules.water(len, f) - 1e-6), (int) Math.floor(capacityBlocks(t) + 1e-6));
        }
        if (type == EntityType.DOLPHIN && t.salinity() < DOLPHIN_MIN_SALT) {
            return Component.translatable("message.bettacraft.big.dolphin_salt", type.getDescription(), Math.round(DOLPHIN_MIN_SALT));
        }
        return null;
    }

    /** The salt range of the dolphin (sea water). */
    public static Salinity.Range dolphinSalt() {
        return Salinity.Range.MARINE;
    }
}
