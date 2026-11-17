package org.overengineer.bettacraft.genetics;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;

/**
 * M22.3: whether the hidden genes of an animal can be seen. Every animal (betta, tankmate, shrimp, crayfish, snail)
 * carries an {@value #KEY} flag: a new animal (spawned, caught, bought, born) does not know its genes until it is analysed
 * with the magnifier in a tank; the result stays with the animal (in tanks, buckets and bags). Animals of older worlds have
 * no flag and count as analysed, so nobody loses what they could see before. With {@code features.breeding.magnifier} off or
 * {@code wildlife.genesAlwaysVisible} on every animal shows its genes.
 * <p>
 * The visible look (variety, colour, fins) is always shown; only the alleles behind it (carriers, the genotype, the
 * breeding prediction) need the analysis.
 */
public final class GeneVisibility {
    /** Key of the flag in the animal's data (entity NBT, bucket data, critter data). */
    public static final String KEY = "analysed";

    /** Client: genes are free for everyone on the server (synced with the server settings). */
    private static volatile boolean clientFree;

    private GeneVisibility() {}

    /** True if no animal needs the magnifier (feature off or genes always visible). */
    public static boolean free() {
        if (org.overengineer.bettacraft.platform.Platform.get().currentServer() == null) return clientFree;
        return serverFree();
    }

    /** The server's (or this instance's) settings. */
    public static boolean serverFree() {
        return !Features.enabled(Feature.MAGNIFIER) || ServerConfig.getOr(ServerConfig.GENES_ALWAYS_VISIBLE, false);
    }

    public static void setClientFree(boolean free) {
        clientFree = free;
    }

    /** Whether the genes of an animal with this analysis state are shown. */
    public static boolean visible(boolean analysed) {
        return analysed || free();
    }

    /** The flag of an animal's data: a missing flag (older worlds) counts as analysed. */
    public static boolean analysed(CompoundTag animal) {
        return !animal.contains(KEY, Tag.TAG_ANY_NUMERIC) || animal.getBoolean(KEY);
    }

    public static void write(CompoundTag animal, boolean analysed) {
        animal.putBoolean(KEY, analysed);
    }
}
