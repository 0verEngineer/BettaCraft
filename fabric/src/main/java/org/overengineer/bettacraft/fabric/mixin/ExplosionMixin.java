package org.overengineer.bettacraft.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import org.overengineer.bettacraft.fabric.FabricGameEvents;
import org.overengineer.bettacraft.platform.event.ExplosionEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * M52: fires the common game event NeoForge fires here (Fabric API has no callback for it). M52b (1.21.2+): the server's
 * explosion; the listeners get the blocks it is about to destroy (a list they may shorten), before entities and blocks
 * are hit.
 */
@Mixin(ServerExplosion.class)
public abstract class ExplosionMixin {
    @Shadow @Final private ServerLevel level;

    @WrapOperation(method = "explode", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/ServerExplosion;calculateExplodedPositions()Ljava/util/List;"))
    private List<BlockPos> bettacraft$detonate(ServerExplosion self, Operation<List<BlockPos>> op) {
        List<BlockPos> blocks = new ArrayList<>(op.call(self));
        FabricGameEvents.post(new ExplosionEvent.Detonate(level, blocks));
        return blocks;
    }
}
