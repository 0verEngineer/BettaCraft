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

    /** All of these features are on (none given = true). */
    public static boolean all(Iterable<Feature> features) {
        for (Feature f : features) if (!enabled(f)) return false;
        return true;
    }

    /** Only the feature's own switch (not its parents); for the config screen and tests. */
    public static boolean own(Feature f) {
        ModConfigSpec.BooleanValue v = CommonConfig.FEATURES.get(f);
        return v == null || CommonConfig.getOr(v, true);
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
