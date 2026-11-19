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
        // M35: a terrarium animal of another mod by the length of its profile, the vanilla sea turtle (a hatchling by its adult
        // size too: it grows up in the tank); without regard to the switches, so one in a tank is never put out as too big
        double profiled = org.overengineer.bettacraft.foreign.ForeignTerrarium.length(type);
        if (profiled > 0) return profiled;
        // M39: a big water animal of another mod by the length of its aquarium profile (Aquaculture's tuna, Fintastic's arapaima ...)
        var p = org.overengineer.bettacraft.foreign.ForeignProfiles.of(type);
        if (p != null && p.bigLength() > 0) return p.bigLength();
        if (type == EntityType.TURTLE) return org.overengineer.bettacraft.terrarium.SeaTurtles.LENGTH_M;
        if (!type.builtInRegistryHolder().is(ModTags.BIG_TANK_ANIMALS)) return 0;
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

    /**
     * M35: the part of the tank the size rule measures for this type: a terrarium animal of another mod by its profile, the
     * sea turtle the water, every other big animal the water (of an aquarium: all of it).
     */
    public static BigTankRules.Volume volume(EntityType<?> type) {
        BigTankRules.Volume v = org.overengineer.bettacraft.foreign.ForeignTerrarium.volume(type);
        return v != null ? v : BigTankRules.Volume.WATER;
    }

    /** M35: the blocks of this volume in the tank (full): its land, its water part or all of it. */
    public static double blocks(TankData t, BigTankRules.Volume v) {
        return BigTankRules.blocks(v, capacityBlocks(t), org.overengineer.bettacraft.terrarium.TerrariumAnimals.landBlocks(t));
    }

    /** M35: the blocks the big animals of {@code live} that share this volume need together (without {@code except}). */
    public static double roomOfOthers(List<Entity> live, @Nullable Entity except, BigTankRules.Volume v) {
        double sum = 0;
        for (Entity e : live) {
            if (e == except) continue;
            double l = length(e.getType(), e);
            if (l > 0 && volume(e.getType()).shares(v)) sum += BigTankRules.water(l, factors());
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
        BigTankRules.Volume vol = volume(type); // M35: the land, the water or the whole tank
        double others = roomOfOthers(live, e, vol), have = blocks(t, vol);
        BigTankRules.Verdict v = BigTankRules.check(t.sizeX(), t.sizeY(), t.sizeZ(), have, others, len, f);
        if (v == BigTankRules.Verdict.TOO_SMALL) {
            return Component.translatable("message.bettacraft.big.too_small", type.getDescription(), n.longSide(), n.shortSide(), n.height(),
                    Math.max(t.sizeX(), t.sizeZ()), Math.min(t.sizeX(), t.sizeZ()), t.sizeY());
        }
        if (v == BigTankRules.Verdict.TOO_LITTLE_WATER) {
            String key = switch (vol) {
                case WATER -> "message.bettacraft.big.too_little_water";
                case LAND -> "message.bettacraft.big.too_little_land";
                case ALL -> "message.bettacraft.big.too_little_room";
            };
            return Component.translatable(key, type.getDescription(), (int) Math.ceil(others + BigTankRules.water(len, f) - 1e-6), (int) Math.floor(have + 1e-6));
        }
        if (type == EntityType.DOLPHIN && t.salinity() < DOLPHIN_MIN_SALT) {
            return Component.translatable("message.bettacraft.big.dolphin_salt", type.getDescription(), Math.round(DOLPHIN_MIN_SALT));
        }
        if (type == EntityType.TURTLE && t.salinity() < org.overengineer.bettacraft.terrarium.SeaTurtles.MIN_SALT) { // M35
            return Component.translatable("message.bettacraft.big.dolphin_salt", type.getDescription(), Math.round(org.overengineer.bettacraft.terrarium.SeaTurtles.MIN_SALT));
        }
        return null;
    }

    /** The salt range of the dolphin (sea water). */
    public static Salinity.Range dolphinSalt() {
        return Salinity.Range.MARINE;
    }
}
