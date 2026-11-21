package org.overengineer.bettacraft.tank.interior;

import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M53c: interior chunks with nothing in them are not stored. The interior is a void ({@code minecraft:flat} without layers,
 * features or structures): a chunk without a block, block entity, scheduled tick, structure, foreign biome or mod data comes
 * out exactly the same when it is generated again. Before, every tank stored the chunks of its shell, the 3 rings of
 * generation around it and every chunk a player inside had in view (all of them 1040 blocks high), ~0.5 MB per tank and
 * more after every visit; now only the chunks of the shell and what was built there. Part of the switch
 * {@code features.interior.cleanup}; off alone with {@code -Dbettacraft.keepEmptyInteriorChunks=true}.
 */
public final class InteriorChunks {
    public static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/interior");
    public static final boolean KEEP_EMPTY = Boolean.getBoolean("bettacraft.keepEmptyInteriorChunks");
    /** Empty chunks not written so far (tests, log). */
    public static volatile long skipped;

    private InteriorChunks() {}

    /** Whether the chunk of the interior dimension holds nothing that would not come back by generating it again. */
    public static boolean empty(ServerLevel level, ChunkAccess chunk) {
        String why = full(level, chunk);
        if (why != null && DEBUG) LOGGER.info("interior chunk {} ({}) is stored: {}", chunk.getPos(), chunk.getPersistedStatus(), why);
        return why == null;
    }

    static final boolean DEBUG = Boolean.getBoolean("bettacraft.debugInteriorChunks");

    private static Object key(Holder<Biome> b) {
        return b.unwrapKey().<Object>map(k -> k).orElse(b);
    }

    /** Why the chunk is not empty, or null. */
    @javax.annotation.Nullable
    static String full(ServerLevel level, ChunkAccess chunk) {
        if (KEEP_EMPTY || !(chunk instanceof LevelChunk || chunk instanceof ProtoChunk) || !InteriorCleanup.enabled()) return "off";
        try {
            // by key (the generator's holders need not be the chunk's instances)
            Set<Object> biomes = new java.util.HashSet<>();
            for (Holder<Biome> b : level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes()) biomes.add(key(b));
            // before the biome step a proto chunk holds the default biome, not its own (it gets them when it goes on)
            boolean ownBiomes = chunk.getPersistedStatus().isOrAfter(net.minecraft.world.level.chunk.status.ChunkStatus.BIOMES);
            for (LevelChunkSection s : chunk.getSections()) {
                if (!s.hasOnlyAir()) return "blocks";
                if (!ownBiomes) continue;
                // a foreign biome (e.g. /fillbiome); the palette may hold entries no longer used (the default before generation)
                if (s.getBiomes().maybeHas(b -> !biomes.contains(key(b)))) {
                    boolean[] foreign = {false};
                    s.getBiomes().getAll(b -> foreign[0] |= !biomes.contains(key(b)));
                    if (foreign[0]) return "biomes";
                }
            }
            if (!chunk.getBlockEntitiesPos().isEmpty()) return "block entities";
            if (chunk.getBlockTicks().count() > 0 || chunk.getFluidTicks().count() > 0) return "ticks";
            if (!chunk.getAllStarts().isEmpty() || !chunk.getAllReferences().isEmpty() || chunk.getBlendingData() != null) return "structures";
            for (var list : chunk.getPostProcessing()) if (list != null && !list.isEmpty()) return "post processing";
            if (chunk instanceof ProtoChunk p && !(chunk instanceof net.minecraft.world.level.chunk.ImposterProtoChunk)
                    && (!p.getBlockEntityNbts().isEmpty() || !p.getEntities().isEmpty())) return "proto block entities / entities";
            return org.overengineer.bettacraft.platform.Platform.get().chunkHasAttachments(chunk) ? "attachments" : null;
        } catch (RuntimeException e) { // in doubt it is written as before
            LOGGER.debug("Could not check the interior chunk {}", chunk.getPos(), e);
            return "error " + e;
        }
    }
}
