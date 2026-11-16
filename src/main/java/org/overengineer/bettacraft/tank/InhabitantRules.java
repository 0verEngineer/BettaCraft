package org.overengineer.bettacraft.tank;

import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;

/**
 * M16: which animals may live in a tank, as plain logic without registries (unit tested). {@link TankInhabitants#accepts}
 * collects the facts of an entity type (tags, size, spawn category, instance) and asks {@link #decide}.
 * <p>
 * Order: config deny list, config allow list, M39: a refusing profile (deep-sea fish, whales, fungus creatures ...), tag {@code #bettacraft:tank_blacklist}, tag
 * {@code #bettacraft:tank_inhabitants}, bosses ({@code #c:bosses}), M30: big animals of known length ({@link TankInhabitants.Acceptance#BIG}:
 * the tank decides, {@link BigTankRules}), size (hitbox wider or taller than
 * {@code inhabitants.maxHitbox}), hostile mobs (spawn category monster, {@code inhabitants.refuseHostile}), and animals
 * that are no water animals at all (a land animal in a bucket of another mod).
 */
public final class InhabitantRules {
    /** Everything known about a type. {@code aquatic} is null while no instance was looked at. */
    public record Facts(String id, float width, float height, boolean blacklistTag, boolean whitelistTag, boolean boss,
                        boolean hostile, @Nullable Boolean aquatic, double bigLength, @Nullable String refuse) {
        /** M30: without a refusal of a profile. */
        public Facts(String id, float width, float height, boolean blacklistTag, boolean whitelistTag, boolean boss, boolean hostile,
                     @Nullable Boolean aquatic, double bigLength) {
            this(id, width, height, blacklistTag, whitelistTag, boss, hostile, aquatic, bigLength, null);
        }

        /** A type without a known big length (M16). */
        public Facts(String id, float width, float height, boolean blacklistTag, boolean whitelistTag, boolean boss, boolean hostile,
                     @Nullable Boolean aquatic) {
            this(id, width, height, blacklistTag, whitelistTag, boss, hostile, aquatic, 0);
        }
    }

    /** The config side of the rules. */
    public record Settings(List<? extends String> allow, List<? extends String> deny, double maxHitbox, boolean refuseHostile, boolean bigAnimals) {
        public static final Settings DEFAULT = new Settings(List.of(), List.of(), 0.8, true, true);

        public Settings(List<? extends String> allow, List<? extends String> deny, double maxHitbox, boolean refuseHostile) {
            this(allow, deny, maxHitbox, refuseHostile, true);
        }
    }

    private InhabitantRules() {}

    public static TankInhabitants.Acceptance decide(Facts f, Settings s) {
        if (matches(s.deny(), f.id())) return TankInhabitants.Acceptance.BLACKLISTED;
        if (matches(s.allow(), f.id())) return TankInhabitants.Acceptance.OK;
        if (f.refuse() != null) return TankInhabitants.Acceptance.REFUSED; // M39 (E1): a refusing profile, with its reason
        if (f.blacklistTag()) return TankInhabitants.Acceptance.BLACKLISTED;
        if (f.whitelistTag()) return TankInhabitants.Acceptance.OK;
        if (f.boss()) return TankInhabitants.Acceptance.BOSS;
        // M30: an animal of known length (the big species, the dolphin, #bettacraft:big_tank_animals) depends on the tank
        if (f.bigLength() > 0) {
            if (f.hostile() && s.refuseHostile()) return TankInhabitants.Acceptance.HOSTILE;
            return s.bigAnimals() ? TankInhabitants.Acceptance.BIG : TankInhabitants.Acceptance.TOO_BIG;
        }
        if (f.width() > s.maxHitbox() + 1e-6 || f.height() > s.maxHitbox() + 1e-6) return TankInhabitants.Acceptance.TOO_BIG;
        if (f.hostile() && s.refuseHostile()) return TankInhabitants.Acceptance.HOSTILE;
        if (Boolean.FALSE.equals(f.aquatic())) return TankInhabitants.Acceptance.NOT_AQUATIC;
        return TankInhabitants.Acceptance.OK;
    }

    /**
     * Whether a config list names this id: {@code modid:name} exactly, {@code modid:*} for every animal of a mod, or
     * {@code *} for all. Case and surrounding spaces do not matter.
     */
    public static boolean matches(List<? extends String> patterns, String id) {
        String low = id.toLowerCase(Locale.ROOT);
        int colon = low.indexOf(':');
        String ns = colon < 0 ? "minecraft" : low.substring(0, colon);
        String full = colon < 0 ? "minecraft:" + low : low;
        for (String raw : patterns) {
            if (raw == null) continue;
            String p = raw.trim().toLowerCase(Locale.ROOT);
            if (p.isEmpty()) continue;
            if (p.equals("*")) return true;
            if (p.endsWith(":*") && p.substring(0, p.length() - 2).equals(ns)) return true;
            if (p.indexOf(':') < 0) p = "minecraft:" + p;
            if (p.equals(full)) return true;
        }
        return false;
    }

    /** Whether a config list entry is well formed (for the config validator). */
    public static boolean validPattern(Object o) {
        if (!(o instanceof String s)) return false;
        String p = s.trim().toLowerCase(Locale.ROOT);
        return p.equals("*") || p.matches("([a-z0-9_.-]+:)?([a-z0-9_./-]+|\\*)");
    }
}
