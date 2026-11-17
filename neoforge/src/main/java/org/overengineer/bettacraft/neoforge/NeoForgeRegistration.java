package org.overengineer.bettacraft.neoforge;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.platform.Attachment;

/**
 * M52: hands the common registers ({@link org.overengineer.bettacraft.platform.registry.DeferredRegister}) and the
 * attachments ({@link Attachment}) to NeoForge: one NeoForge DeferredRegister each, in the order the common code handed
 * them over, registered on the mod bus.
 */
final class NeoForgeRegistration {
    private NeoForgeRegistration() {}

    static void register(IEventBus modBus) {
        for (var reg : org.overengineer.bettacraft.platform.registry.DeferredRegister.registered()) register(reg, modBus);
        DeferredRegister<AttachmentType<?>> attachments = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, BettaCraft.MOD_ID);
        for (Attachment<?> a : Attachment.all()) attach(attachments, a);
        attachments.register(modBus);
    }

    private static <T> void register(org.overengineer.bettacraft.platform.registry.DeferredRegister<T> reg, IEventBus modBus) {
        DeferredRegister<T> neo = DeferredRegister.create(reg.getRegistryKey(), reg.getNamespace());
        for (Map.Entry<ResourceLocation, Supplier<? extends T>> e : reg.takeEntries().entrySet()) neo.register(e.getKey().getPath(), e.getValue());
        neo.register(modBus);
    }

    private static <T> void attach(DeferredRegister<AttachmentType<?>> reg, Attachment<T> a) {
        Supplier<AttachmentType<T>> type = reg.register(a.id().getPath(), () -> {
            AttachmentType.Builder<T> b = AttachmentType.builder(a.defaultValue());
            Attachment.Serializer<T> s = a.serializer();
            if (s != null) {
                b.serialize(new IAttachmentSerializer<CompoundTag, T>() {
                    @Override
                    public T read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                        return s.read(tag, provider);
                    }

                    @Override
                    public CompoundTag write(T value, HolderLookup.Provider provider) {
                        return s.write(value, provider);
                    }
                });
                if (a.copyOnDeath()) b.copyOnDeath();
            }
            return b.build();
        });
        a.bind(new Attachment.Backend<>() {
            @Override
            public T get(Entity holder) {
                return holder.getData(type);
            }

            @Override
            public Optional<T> existing(Entity holder) {
                return holder.getExistingData(type);
            }

            @Override
            public boolean has(Entity holder) {
                return holder.hasData(type);
            }

            @Override
            public void set(Entity holder, T value) {
                holder.setData(type, value);
            }

            @Override
            public void remove(Entity holder) {
                holder.removeData(type);
            }
        });
    }
}
