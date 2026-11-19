package org.overengineer.bettacraft.fabric.mixin;

import org.overengineer.bettacraft.util.NbtCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import org.overengineer.bettacraft.fabric.PersistentDataHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** M52: the entity data we keep across saves (NeoForge's persistent data, under the same key). */
@Mixin(Entity.class)
public abstract class EntityPersistentDataMixin implements PersistentDataHolder {
    @Unique
    private CompoundTag bettacraft$data;

    @Override
    public CompoundTag bettacraft$persistentData() {
        if (bettacraft$data == null) bettacraft$data = new CompoundTag();
        return bettacraft$data;
    }

    // M52b (1.21.6+): entities save through ValueOutput / ValueInput; same key and codec as NeoForge 21.11
    @Inject(method = "saveWithoutId", at = @At("HEAD"))
    private void bettacraft$save(net.minecraft.world.level.storage.ValueOutput out, CallbackInfo ci) {
        if (bettacraft$data != null && !bettacraft$data.isEmpty()) out.store("NeoForgeData", CompoundTag.CODEC, bettacraft$data.copy());
    }

    @Inject(method = "load", at = @At("HEAD"))
    private void bettacraft$load(net.minecraft.world.level.storage.ValueInput in, CallbackInfo ci) {
        in.read("NeoForgeData", CompoundTag.CODEC).ifPresent(t -> bettacraft$data = t.copy());
    }
}
