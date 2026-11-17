package org.overengineer.bettacraft.registry;

import com.mojang.serialization.Dynamic;
import java.util.function.Supplier;
import org.overengineer.bettacraft.platform.Attachment;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.overengineer.bettacraft.platform.registry.DeferredRegister;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.data.schema.Schemas;
import org.overengineer.bettacraft.tank.TankRef;
import org.overengineer.bettacraft.tank.interior.InteriorReturn;

public final class ModAttachments {

    /** Return position for players inside a tank interior; kept on logout (PLAN 7.1). */
    public static final Attachment<InteriorReturn> INTERIOR_RETURN = Attachment.saved(BettaCraft.id("interior_return"), () -> InteriorReturn.NONE,
            new Attachment.Serializer<InteriorReturn>() {
                        @Override
                        public InteriorReturn read(CompoundTag tag, HolderLookup.Provider provider) {
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
                    }, false);

    /** Tank an inhabitant entity belongs to (PLAN 4.2). Persistent; absent on entities outside tanks. */
    public static final Attachment<TankRef> TANK_REF = Attachment.saved(BettaCraft.id("tank_ref"), () -> TankRef.NONE,
            new Attachment.Serializer<TankRef>() {
                        @Override
                        public TankRef read(CompoundTag tag, HolderLookup.Provider provider) {
                            return TankRef.load(Schemas.TANK_REF.migrate(tag));
                        }

                        @Override
                        public CompoundTag write(TankRef value, HolderLookup.Provider provider) {
                            return value.isSet() ? Schemas.TANK_REF.stamp(value.save()) : null;
                        }
                    }, false);

    /** Whether the player already got the GuideME handbook (M6). Survives death and logout. */
    public static final Attachment<org.overengineer.bettacraft.progression.GuideBookState> GUIDE_BOOK = Attachment.saved(BettaCraft.id("guide_book"), () -> org.overengineer.bettacraft.progression.GuideBookState.NONE,
            new Attachment.Serializer<org.overengineer.bettacraft.progression.GuideBookState>() {
                        @Override
                        public org.overengineer.bettacraft.progression.GuideBookState read(CompoundTag tag, HolderLookup.Provider provider) {
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
                    }, true);

    /**
     * M31 part d: a mob that lives (or lived) shrunk in a tank ({@link org.overengineer.bettacraft.mobs.GlassMobs}): its
     * growth and breeding times. Persistent; its presence means it drops nothing and gives no experience.
     */
    public static final Attachment<org.overengineer.bettacraft.mobs.GlassMobData> GLASS_MOB = Attachment.saved(BettaCraft.id("glass_mob"), org.overengineer.bettacraft.mobs.GlassMobData::new,
            new Attachment.Serializer<org.overengineer.bettacraft.mobs.GlassMobData>() {
                        @Override
                        public org.overengineer.bettacraft.mobs.GlassMobData read(CompoundTag tag, HolderLookup.Provider provider) {
                            return org.overengineer.bettacraft.mobs.GlassMobData.load(Schemas.GLASS_MOB.migrate(tag));
                        }

                        @Override
                        public CompoundTag write(org.overengineer.bettacraft.mobs.GlassMobData value, HolderLookup.Provider provider) {
                            return Schemas.GLASS_MOB.stamp(value.save());
                        }
                    }, false);

    /** M31 part d: the running state of a mob in glass (walker, tank); not saved. */
    public static final Attachment<org.overengineer.bettacraft.mobs.GlassMobState> GLASS_STATE = Attachment.transientData(BettaCraft.id("glass_state"), org.overengineer.bettacraft.mobs.GlassMobState::new);

    /** Creates the attachments (the loader registers {@link Attachment#all()} afterwards). */
    public static void register() {}

    private ModAttachments() {}
}
