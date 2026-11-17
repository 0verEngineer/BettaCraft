package org.overengineer.bettacraft.reef;

import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tank.Salinity.Range;
import org.overengineer.bettacraft.tank.TankData;

/**
 * M20: which salinity each animal lives in. Bettas, the freshwater tankmates and most small animals need fresh water;
 * livebearers (guppies, endlers, platys) and nerite snails tolerate brackish water; the reef animals need sea water.
 * Animals of vanilla and other mods: see {@link ServerConfig.ForeignSalinity} (default lenient: salinity does not
 * matter to them).
 */
public final class SaltTolerance {
    /** Bettas live in fresh water (a pinch of salt is fine). */
    public static final Range BETTA = new Range(0, 4);
    /** The dwarf clawed frog. */
    public static final Range FROG = Range.FRESH;

    private SaltTolerance() {}

    public static double speed() {
        if (!org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.SALTWATER)) return 0; // M33 part b: salt water off: salinity harms nobody
        return ServerConfig.getOr(ServerConfig.SALT_SHOCK_SPEED, 1.0);
    }

    /** Salt shock per day for an animal with {@code range} in this tank (0 = fine). */
    public static double shock(Range range, TankData t) {
        if (range == null) return 0;
        return Salinity.shockPerDay(range, t.salinity(), speed());
    }

    public static Range critter(CritterSpecies s) {
        return switch (s) {
            case ZEBRA_NERITE, HORNED_NERITE, OLIVE_NERITE, RED_NERITE, YELLOW_NERITE -> Range.NERITE;
            case CLEANER_SHRIMP, TURBO_SNAIL, FIRE_SHRIMP -> Range.MARINE; // M48 part b: the fire shrimp
            case HERMIT_CRAB -> Range.HARDY_MARINE;
            default -> Range.FRESH;
        };
    }

    /**
     * Range of an animal of vanilla or another mod, or null when the salinity does not matter to it (lenient config,
     * unknown animals).
     */
    @Nullable
    public static Range foreign(EntityType<?> type) {
        if (ServerConfig.getOr(ServerConfig.SALT_FOREIGN, ServerConfig.ForeignSalinity.LENIENT) != ServerConfig.ForeignSalinity.REALISTIC) return null;
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (!"minecraft".equals(id.getNamespace())) return null;
        return switch (id.getPath()) {
            case "cod", "tropical_fish", "pufferfish", "squid", "glow_squid" -> Range.HARDY_MARINE;
            case "salmon" -> Range.ANY;
            case "axolotl", "tadpole" -> Range.FRESH;
            default -> null;
        };
    }

    @Nullable
    public static Range of(Entity e) {
        if (e.getType() == net.minecraft.world.entity.EntityType.DOLPHIN) return Range.MARINE; // M30: sea water only
        if (e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity) return BETTA;
        if (e instanceof org.overengineer.bettacraft.entity.tankmate.TankmateEntity m) return m.species().salinity();
        if (e instanceof org.overengineer.bettacraft.entity.frog.DwarfFrogEntity) return FROG;
        var fp = org.overengineer.bettacraft.foreign.ForeignProfiles.cared(e); // M32 part a: by its profile (REALISTIC)
        if (fp != null) return org.overengineer.bettacraft.foreign.ForeignProfiles.salinity(fp);
        return foreign(e.getType());
    }

    /** Range of the animal an entity type stands for (our own types and foreign ones), or null. */
    @Nullable
    public static Range of(EntityType<?> type) {
        if (type == EntityType.DOLPHIN) return Range.MARINE; // M30
        if (type == org.overengineer.bettacraft.registry.ModEntities.BETTA.get()) return BETTA;
        var tm = org.overengineer.bettacraft.tankmates.TankmateSpecies.of(type);
        if (tm != null) return tm.salinity();
        if (type == org.overengineer.bettacraft.registry.ModEntities.DWARF_FROG.get()) return FROG;
        var fp = org.overengineer.bettacraft.foreign.ForeignProfiles.cared(type); // M32 part a (REALISTIC)
        if (fp != null) return org.overengineer.bettacraft.foreign.ForeignProfiles.salinity(fp);
        return foreign(type);
    }

    /** Whether putting an animal with {@code range} into this tank is refused (the water would kill it fast). */
    public static boolean refuse(@Nullable Range range, TankData t) {
        if (range == null || speed() <= 0 || !ServerConfig.getOr(ServerConfig.SALT_REFUSE_DEADLY, true)) return false;
        return t.waterMb() > 0 && Salinity.deadly(range, t.salinity());
    }

    /** Message key for a refused animal: "freshwater animal into salt water" or the other way round. */
    public static String refuseKey(Range range) {
        return range.marine() ? "message.bettacraft.reef.needs_salt" : "message.bettacraft.reef.needs_fresh";
    }
}
