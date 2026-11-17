package org.overengineer.bettacraft.neoforge.mixin;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.overengineer.bettacraft.tank.interior.InteriorSync;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every successful {@code Level#setBlock} ends in {@code markAndNotifyBlock} (NeoForge; also for captured player
 * placements): the interior sync learns of every block change of a tank interior there (M52: NeoForge's half of the
 * former LevelMixin; Fabric: fabric/mixin/LevelSetBlockMixin).
 */
@Mixin(Level.class)
public abstract class LevelNotifyMixin {
    @Inject(method = "markAndNotifyBlock", at = @At("HEAD"), remap = false)
    private void bettacraft$interiorChanged(BlockPos pos, @Nullable LevelChunk chunk, BlockState oldState, BlockState newState, int flags,
                                            int recursionLeft, CallbackInfo ci) {
        if ((Object) this instanceof ServerLevel server) InteriorSync.onBlockUpdated(server, pos, oldState, newState);
    }
}
