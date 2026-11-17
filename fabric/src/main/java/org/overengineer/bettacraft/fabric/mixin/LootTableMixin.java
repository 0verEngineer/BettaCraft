package org.overengineer.bettacraft.fabric.mixin;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.overengineer.bettacraft.fabric.FabricWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** M52: our global loot modifiers on Fabric, where NeoForge applies its own (the rolled list of a table). */
@Mixin(LootTable.class)
public abstract class LootTableMixin {
    @Shadow @Final private Optional<Identifier> randomSequence;

    @Inject(method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;", at = @At("RETURN"))
    private void bettacraft$modify(LootContext context, CallbackInfoReturnable<ObjectArrayList<ItemStack>> cir) {
        FabricWorld.modify(randomSequence.orElse(null), context, cir.getReturnValue());
    }
}
