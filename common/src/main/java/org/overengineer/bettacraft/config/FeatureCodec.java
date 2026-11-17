package org.overengineer.bettacraft.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/** A feature by its path below {@code features}; an unknown path is an error (M33; M52: shared by the recipe and loot conditions). */
public final class FeatureCodec {
    public static final Codec<Feature> FEATURE = Codec.STRING.comapFlatMap(s -> {
        Feature f = Feature.byPath(s);
        return f == null ? DataResult.error(() -> "unknown Bettacraft feature: " + s) : DataResult.success(f);
    }, Feature::path);

    private FeatureCodec() {}
}
