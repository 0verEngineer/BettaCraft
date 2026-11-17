package org.overengineer.bettacraft.mixin.interior;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.SeagrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Seagrass and kelp can be placed without water, but only in the (dry) interior dimension (PLAN 7.3). */
@Mixin({SeagrassBlock.class, KelpBlock.class})
public abstract class WaterPlantPlacementMixin {
    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void bettacraft$placeDry(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        if (cir.getReturnValue() == null && InteriorManager.isInterior(context.getLevel())) {
            BlockState s = ((Block) (Object) this).defaultBlockState();
            if (s.canSurvive(context.getLevel(), context.getClickedPos())) cir.setReturnValue(s);
        }
    }
}
