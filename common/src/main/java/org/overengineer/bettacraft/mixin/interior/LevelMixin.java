package org.overengineer.bettacraft.mixin.interior;

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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every successful {@code Level#setBlock} ends in {@code markAndNotifyBlock} (NeoForge; also for captured player
 * placements, once they are confirmed). In the interior dimension this marks the 16^3 section dirty for the miniature
 * delta sync and keeps the equipment list current, regardless of chunk ticking status and update flags.
 */
@Mixin(Level.class)
public abstract class LevelMixin {
    @Inject(method = "markAndNotifyBlock", at = @At("HEAD"), remap = false)
    private void bettacraft$interiorChanged(BlockPos pos, @Nullable LevelChunk chunk, BlockState oldState, BlockState newState, int flags,
                                            int recursionLeft, CallbackInfo ci) {
        if ((Object) this instanceof ServerLevel server) InteriorSync.onBlockUpdated(server, pos, oldState, newState);
    }

    /**
     * M29.1: the interior is dry, its water is the tank's own model. Kelp, seagrass, corals, sea pickles and every
     * waterlogged block (also of other mods) leave their water state behind when they are broken, pushed away by a
     * piston or washed out (vanilla {@code FluidState#createLegacyBlock}); a water bucket would pour a water block.
     * In the interior dimension such a vanilla water block becomes air (on both sides, so the client does not flicker).
     * Waterlogging a block keeps working, and outside the interior nothing changes.
     */
    @ModifyVariable(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"),
            argsOnly = true, ordinal = 0)
    private BlockState bettacraft$noWaterInInterior(BlockState state) {
        return org.overengineer.bettacraft.tank.interior.InteriorManager.dryInterior((Level) (Object) this, state);
    }
}
