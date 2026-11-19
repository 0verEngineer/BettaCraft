package org.overengineer.bettacraft.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.PickableBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * M52: NeoForge's getCloneItemStack with the hit and the player for pick block. M52b (1.21.4+): the server picks the item
 * (the client only sends the position); the hit is the player's view ray, as on NeoForge 21.11.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PickBlockMixin {
    @Shadow public ServerPlayer player;

    @WrapOperation(method = "handlePickItemFromBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getCloneItemStack(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack bettacraft$pick(BlockState state, LevelReader level, BlockPos pos, boolean includeData, Operation<ItemStack> op) {
        if (state.getBlock() instanceof PickableBlock p) {
            return p.getCloneItemStack(state, player.pick(player.blockInteractionRange(), 1.0F, false), level, pos, player);
        }
        return op.call(state, level, pos, includeData);
    }
}
