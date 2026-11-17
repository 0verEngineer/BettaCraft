package org.overengineer.bettacraft.mixin.interior;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Water plants of other mods ({@code #bettacraft:interior_water_plants}) refuse to be placed without water; in the dry
 * interior dimension they get their default state instead (PLAN 7.3, M7).
 */
@Mixin(BlockItem.class)
public abstract class InteriorWaterPlantPlacementMixin {
    @Inject(method = "getPlacementState", at = @At("RETURN"), cancellable = true)
    private void bettacraft$placeDry(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        if (cir.getReturnValue() != null || !InteriorManager.isInterior(context.getLevel())) return;
        BlockState s = ((BlockItem) (Object) this).getBlock().defaultBlockState();
        if (s.is(ModTags.INTERIOR_WATER_PLANTS)) cir.setReturnValue(s);
    }
}
