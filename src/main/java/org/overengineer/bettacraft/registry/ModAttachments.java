package org.overengineer.bettacraft.registry;

import com.mojang.serialization.Dynamic;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.data.schema.Schemas;
import org.overengineer.bettacraft.tank.TankRef;
import org.overengineer.bettacraft.tank.interior.InteriorReturn;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, BettaCraft.MOD_ID);

    /** Return position for players inside a tank interior; kept on logout (PLAN 7.1). */
    public static final Supplier<AttachmentType<InteriorReturn>> INTERIOR_RETURN = ATTACHMENTS.register("interior_return",
            () -> AttachmentType.builder(() -> InteriorReturn.NONE)
                    .serialize(new IAttachmentSerializer<CompoundTag, InteriorReturn>() {
                        @Override
                        public InteriorReturn read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                            CompoundTag migrated = Schemas.INTERIOR_RETURN.migrate(tag);
                            return InteriorReturn.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, migrated))
                                    .result().orElse(InteriorReturn.NONE);
                        }

                        @Override
                        public CompoundTag write(InteriorReturn value, HolderLookup.Provider provider) {
                            if (!value.isSet()) return null;
                            Tag t = InteriorReturn.CODEC.encodeStart(NbtOps.INSTANCE, value).result().orElseGet(CompoundTag::new);
                            CompoundTag tag = t instanceof CompoundTag c ? c : new CompoundTag();
                            return Schemas.INTERIOR_RETURN.stamp(tag);
                        }
                    })
                    .build());

    /** Tank an inhabitant entity belongs to (PLAN 4.2). Persistent; absent on entities outside tanks. */
    public static final Supplier<AttachmentType<TankRef>> TANK_REF = ATTACHMENTS.register("tank_ref",
            () -> AttachmentType.builder(() -> TankRef.NONE)
                    .serialize(new IAttachmentSerializer<CompoundTag, TankRef>() {
                        @Override
                        public TankRef read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                            return TankRef.load(Schemas.TANK_REF.migrate(tag));
                        }

                        @Override
                        public CompoundTag write(TankRef value, HolderLookup.Provider provider) {
                            return value.isSet() ? Schemas.TANK_REF.stamp(value.save()) : null;
                        }
                    })
                    .build());

    /** Whether the player already got the GuideME handbook (M6). Survives death and logout. */
    public static final Supplier<AttachmentType<org.overengineer.bettacraft.progression.GuideBookState>> GUIDE_BOOK = ATTACHMENTS.register("guide_book",
            () -> AttachmentType.builder(() -> org.overengineer.bettacraft.progression.GuideBookState.NONE)
                    .serialize(new IAttachmentSerializer<CompoundTag, org.overengineer.bettacraft.progression.GuideBookState>() {
                        @Override
                        public org.overengineer.bettacraft.progression.GuideBookState read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                            CompoundTag migrated = Schemas.GUIDE_BOOK.migrate(tag);
                            return org.overengineer.bettacraft.progression.GuideBookState.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, migrated))
                                    .result().orElse(org.overengineer.bettacraft.progression.GuideBookState.NONE);
                        }

                        @Override
                        public CompoundTag write(org.overengineer.bettacraft.progression.GuideBookState value, HolderLookup.Provider provider) {
                            if (!value.given()) return null;
                            Tag t = org.overengineer.bettacraft.progression.GuideBookState.CODEC.encodeStart(NbtOps.INSTANCE, value).result().orElseGet(CompoundTag::new);
                            CompoundTag tag = t instanceof CompoundTag c ? c : new CompoundTag();
                            return Schemas.GUIDE_BOOK.stamp(tag);
                        }
                    })
                    .copyOnDeath()
                    .build());

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
    }

    private ModAttachments() {}
}
