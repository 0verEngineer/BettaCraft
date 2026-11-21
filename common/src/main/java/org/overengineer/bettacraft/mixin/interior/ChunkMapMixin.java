package org.overengineer.bettacraft.mixin.interior;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import org.overengineer.bettacraft.registry.ModDimensions;
import org.overengineer.bettacraft.tank.interior.InteriorChunkMap;
import org.overengineer.bettacraft.tank.interior.InteriorChunks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * M31.1: lets the chunk generation tell the interior dimension apart ({@link ChunkGenerationTaskMixin}).
 * M53c: an interior chunk without anything in it is not written (and an older copy of it is deleted): it comes out the
 * same when it is generated again ({@link InteriorChunks#empty}); and the clean-up asks which chunks are held.
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin implements InteriorChunkMap {
    @Shadow
    @Final
    ServerLevel level;

    @Shadow
    @Final
    private Long2ObjectLinkedOpenHashMap<ChunkHolder> updatingChunkMap;

    @Shadow
    @Final
    private Long2ObjectLinkedOpenHashMap<ChunkHolder> pendingUnloads;

    @Shadow
    @Final
    private Long2ByteMap chunkTypeCache;

    @Override
    public boolean bettacraft$isInterior() {
        return level.dimension() == ModDimensions.TANK_INTERIOR;
    }

    @Override
    public LongSet bettacraft$heldChunks() {
        LongSet out = new LongOpenHashSet(updatingChunkMap.keySet());
        out.addAll(pendingUnloads.keySet());
        return out;
    }

    @Override
    public void bettacraft$forgetStored(long chunk) {
        chunkTypeCache.remove(chunk);
    }

    /** Right before vanilla serializes the chunk (after its own checks, the chunk is marked saved already). */
    @Inject(method = "save(Lnet/minecraft/world/level/chunk/ChunkAccess;)Z", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/storage/ChunkSerializer;write(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;)Lnet/minecraft/nbt/CompoundTag;"),
            cancellable = true)
    private void bettacraft$skipEmptyInteriorChunk(ChunkAccess chunk, CallbackInfoReturnable<Boolean> cir) {
        if (level.dimension() != ModDimensions.TANK_INTERIOR || !InteriorChunks.empty(level, chunk)) return;
        ChunkPos pos = chunk.getPos();
        // -1: not on the disk (generated here, deleted before) or an unfinished chunk there (void as well): nothing to write;
        // else (a full chunk read from or written to the disk) its stored copy is deleted
        if (chunkTypeCache.get(pos.toLong()) != (byte) -1) {
            ((ChunkStorage) (Object) this).write(pos, null).exceptionally(e -> {
                InteriorChunks.LOGGER.warn("Could not delete the empty interior chunk {}", pos, e);
                return null;
            });
        }
        chunkTypeCache.put(pos.toLong(), (byte) -1);
        InteriorChunks.skipped++;
        cir.setReturnValue(true);
    }
}
