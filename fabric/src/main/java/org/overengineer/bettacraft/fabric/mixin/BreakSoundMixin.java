package org.overengineer.bettacraft.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelEventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.PositionalBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** M52: NeoForge's positional sound type when a block breaks (the client's level event 2001; M52b: LevelEventHandler since 1.21.2). */
@Mixin(LevelEventHandler.class)
public abstract class BreakSoundMixin {
    @WrapOperation(method = "levelEvent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"))
    private SoundType bettacraft$sound(BlockState state, Operation<SoundType> op, @Local(argsOnly = true) BlockPos pos) {
        var level = Minecraft.getInstance().level;
        if (state.getBlock() instanceof PositionalBlock.Sound s && level != null) return s.getSoundType(state, level, pos, null);
        return op.call(state);
    }
}
