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

    @Inject(method = "saveWithoutId", at = @At("HEAD"))
    private void bettacraft$save(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (bettacraft$data != null && !bettacraft$data.isEmpty()) tag.put("NeoForgeData", bettacraft$data.copy());
    }

    @Inject(method = "load", at = @At("HEAD"))
    private void bettacraft$load(CompoundTag tag, CallbackInfo ci) {
        if (NbtCompat.has(tag, "NeoForgeData", Tag.TAG_COMPOUND)) bettacraft$data = tag.getCompoundOrEmpty("NeoForgeData").copy();
    }
}
