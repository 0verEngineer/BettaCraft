package org.overengineer.bettacraft.fabric;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.overengineer.bettacraft.platform.Attachment;
import org.overengineer.bettacraft.platform.registry.DeferredRegister;

/**
 * M52: registers the common registers on Fabric, registry by registry in the order of the vanilla registries (like
 * NeoForge's register events), so that an object's factory can use the objects of earlier registries (spawn eggs their
 * entity types, block items their blocks). And the attachments through Fabric's attachment API.
 */
final class FabricRegistration {
    private FabricRegistration() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void registerAll() {
        List<DeferredRegister<?>> regs = new ArrayList<>(DeferredRegister.registered());
        for (Registry<?> registry : BuiltInRegistries.REGISTRY) {
            for (DeferredRegister<?> reg : regs) {
                if (!reg.getRegistryKey().location().equals(registry.key().location())) continue;
                for (Map.Entry<ResourceLocation, ? extends Supplier<?>> e : reg.takeEntries().entrySet()) {
                    Object value = e.getValue().get();
                    var holder = Registry.registerForHolder((Registry) registry, e.getKey(), value);
                    // NeoForge maps the block states of a point of interest type when it is registered; vanilla only
                    // for its own types
                    if (value instanceof net.minecraft.world.entity.ai.village.poi.PoiType poi) {
                        net.minecraft.world.entity.ai.village.poi.PoiTypes.registerBlockStates(
                                (net.minecraft.core.Holder<net.minecraft.world.entity.ai.village.poi.PoiType>) holder, poi.matchingStates());
                    }
                }
            }
        }
        for (DeferredRegister<?> reg : regs) {
            if (BuiltInRegistries.REGISTRY.get(reg.getRegistryKey().location()) == null) {
                throw new IllegalStateException("Bettacraft: no registry " + reg.getRegistryKey().location() + " on Fabric");
            }
        }
        for (Attachment<?> a : Attachment.all()) attach(a);
    }

    private static <T> void attach(Attachment<T> a) {
        Attachment.Serializer<T> s = a.serializer();
        AttachmentType<T> type;
        if (s == null) {
            type = AttachmentRegistry.<T>builder().initializer(a.defaultValue()).buildAndRegister(a.id());
        } else {
            // saved as the same compound NeoForge saves (the serializer's tag); a value that writes no tag is removed
            Codec<T> codec = CompoundTag.CODEC.xmap(tag -> s.read(tag, RegistryAccess.EMPTY), v -> {
                CompoundTag t = s.write(v, RegistryAccess.EMPTY);
                return t == null ? new CompoundTag() : t;
            });
            AttachmentRegistry.Builder<T> b = AttachmentRegistry.<T>builder().initializer(a.defaultValue()).persistent(codec);
            if (a.copyOnDeath()) b.copyOnDeath();
            type = b.buildAndRegister(a.id());
        }
        a.bind(new Attachment.Backend<>() {
            @Override
            public T get(Entity holder) {
                return holder.getAttachedOrCreate(type);
            }

            @Override
            public Optional<T> existing(Entity holder) {
                return Optional.ofNullable(holder.getAttached(type));
            }

            @Override
            public boolean has(Entity holder) {
                return holder.hasAttached(type);
            }

            @Override
            public void set(Entity holder, T value) {
                if (s != null && s.write(value, RegistryAccess.EMPTY) == null) holder.removeAttached(type);
                else holder.setAttached(type, value);
            }

            @Override
            public void remove(Entity holder) {
                holder.removeAttached(type);
            }
        });
    }
}
