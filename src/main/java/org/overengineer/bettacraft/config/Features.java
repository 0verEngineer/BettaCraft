package org.overengineer.bettacraft.config;

import java.util.function.Predicate;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * M33: the one place that answers whether a feature is on. {@link #enabled} is the switch of the feature itself, of its
 * parent (up to the group) and of every feature it requires; code, recipe and loot conditions, the trader, the net, the
 * spawns and (part b) the creative tab, JEI and the handbook all ask here. Before the common config is loaded (datagen,
 * unit tests) every switch counts as its default (on).
 */
public final class Features {
    private Features() {}

    /** Whether the feature is on: its own switch, its parents and the features it requires. */
    public static boolean enabled(Feature f) {
        return resolve(f, Features::own);
    }

    /** M33 part b: why a feature is off: the outermost parent or requirement that is off, else the feature itself. */
    public static Feature cause(Feature f) {
        if (f.parent() != null && !enabled(f.parent())) return cause(f.parent());
        for (Feature r : f.requires()) if (!enabled(r)) return cause(r);
        return f;
    }

    /** All of these features are on (none given = true). */
    public static boolean all(Iterable<Feature> features) {
        for (Feature f : features) if (!enabled(f)) return false;
        return true;
    }

    /**
     * Part b: on a client connected to another (dedicated or LAN) server, the server's own values ({@link #setRemote}):
     * the common config is not synced by NeoForge, but the creative tab, JEI, the handbook and the client-side logic
     * (sounds, overlays) must follow the server. Null in single player and on every server.
     */
    @javax.annotation.Nullable
    private static volatile java.util.Set<Feature> remoteOff;

    /** Only the feature's own switch (not its parents); for the config screen and tests. */
    public static boolean own(Feature f) {
        java.util.Set<Feature> remote = remoteOff;
        if (remote != null) return !remote.contains(f);
        ModConfigSpec.BooleanValue v = CommonConfig.FEATURES.get(f);
        return v == null || CommonConfig.getOr(v, true);
    }

    /** The features whose own switch is off here (what a server sends to its clients). */
    public static java.util.List<String> ownOff() {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (Feature f : Feature.values()) if (!own(f)) out.add(f.path());
        return out;
    }

    /**
     * Client of a remote server: its own values from now on (paths this client does not know are ignored); null goes back
     * to the local config (logout). Returns whether anything changed.
     */
    public static boolean setRemote(@javax.annotation.Nullable java.util.Collection<String> offPaths) {
        java.util.Set<Feature> before = remoteOff;
        java.util.Set<Feature> next = null;
        if (offPaths != null) {
            next = java.util.EnumSet.noneOf(Feature.class);
            for (String p : offPaths) {
                Feature f = Feature.byPath(p);
                if (f != null) next.add(f);
            }
            next = java.util.Collections.unmodifiableSet(next);
        }
        remoteOff = next;
        return !java.util.Objects.equals(before, next);
    }

    /** Whether the values come from a remote server. */
    public static boolean remote() {
        return remoteOff != null;
    }

    /** Sets the feature's own switch (GameTests, presets); it is not saved to the file by itself. */
    public static void set(Feature f, boolean on) {
        CommonConfig.FEATURES.get(f).set(on);
    }

    /** The parent/child rule on any source of own values (pure; unit tests, presets). */
    public static boolean resolve(Feature f, Predicate<Feature> own) {
        if (!own.test(f)) return false;
        if (f.parent() != null && !resolve(f.parent(), own)) return false;
        for (Feature r : f.requires()) if (!resolve(r, own)) return false;
        return true;
    }
}
