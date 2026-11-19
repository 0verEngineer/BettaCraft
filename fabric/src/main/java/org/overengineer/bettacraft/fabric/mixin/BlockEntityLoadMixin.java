package org.overengineer.bettacraft.fabric.mixin;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.platform.ClientSyncedBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * M52: on the client, a block entity of ours reads its update tags itself (NeoForge calls handleUpdateTag and onDataPacket
 * where vanilla loads the tag): the chunk data and the block entity data packets both end in loadWithComponents.
 */
@Mixin(BlockEntity.class)
public abstract class BlockEntityLoadMixin {
    @Unique
    private static final ThreadLocal<Boolean> bettacraft$inside = ThreadLocal.withInitial(() -> false);

    @Shadow @Nullable protected Level level;

    @Inject(method = "loadWithComponents", at = @At("HEAD"), cancellable = true)
    private void bettacraft$clientLoad(net.minecraft.world.level.storage.ValueInput input, CallbackInfo ci) {
        if (!(this instanceof ClientSyncedBlockEntity synced) || level == null || !level.isClientSide() || bettacraft$inside.get()) return;
        bettacraft$inside.set(true);
        try {
            // M52b (1.21.6+): the tag behind the ValueInput
            synced.handleUpdateTag(org.overengineer.bettacraft.util.NbtCompat.read(input), org.overengineer.bettacraft.util.NbtCompat.registries((BlockEntity) (Object) this));
        } finally {
            bettacraft$inside.set(false);
        }
        ci.cancel();
    }
}
