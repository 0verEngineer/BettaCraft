package org.overengineer.bettacraft.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.PickableBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** M52: NeoForge's getCloneItemStack with the hit and the player for the client's pick block. */
@Mixin(Minecraft.class)
public abstract class PickBlockMixin {
    @WrapOperation(method = "pickBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;getCloneItemStack(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack bettacraft$pick(Block block, LevelReader level, BlockPos pos, BlockState state, Operation<ItemStack> op) {
        Minecraft mc = (Minecraft) (Object) this;
        if (block instanceof PickableBlock p && mc.hitResult != null && mc.player != null) {
            return p.getCloneItemStack(state, mc.hitResult, level, pos, mc.player);
        }
        return op.call(block, level, pos, state);
    }
}
