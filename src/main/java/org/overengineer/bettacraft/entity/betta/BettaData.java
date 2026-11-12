package org.overengineer.bettacraft.entity.betta;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.overengineer.bettacraft.data.schema.Schemas;
import org.overengineer.bettacraft.genetics.BettaGenome;

/**
 * Everything that makes one betta individual. One format for entity NBT (key {@link #KEY}), the
 * {@code bettacraft:fish_data} item component and the mirror in {@code minecraft:bucket_entity_data} (same key
 * {@link #KEY}, identical content). Schema-versioned ({@link Schemas#BETTA}); unknown keys are kept in {@code raw}.
 *
 * @param genome     genes (string ids)
 * @param female     sex
 * @param stage      life stage id (egg, larva, fry, juvenile, subadult, adult); M1 only uses adult
 * @param fishId     stable id of this individual (entity UUIDs change when a fish is bucketed)
 * @param mother     pedigree
 * @param father     pedigree
 * @param generation 0 = wild / bought
 * @param bornTime   game time of birth (or of first spawn)
 * @param health     {@link BettaHealth} NBT
 * @param raw        the full tag as read (unknown keys survive a save)
 */
public record BettaData(BettaGenome genome, boolean female, String stage, UUID fishId, Optional<UUID> mother,
                        Optional<UUID> father, int generation, long bornTime, CompoundTag health, CompoundTag raw) {
    public static final String KEY = "betta";
    public static final String STAGE_ADULT = "adult";

    public static final Codec<BettaData> CODEC = CompoundTag.CODEC.xmap(BettaData::load, BettaData::save);
    public static final StreamCodec<ByteBuf, BettaData> STREAM_CODEC = ByteBufCodecs.COMPOUND_TAG.map(BettaData::load, BettaData::save);

    public static BettaData create(BettaGenome genome, boolean female, long bornTime) {
        return new BettaData(genome, female, STAGE_ADULT, UUID.randomUUID(), Optional.empty(), Optional.empty(), 0, bornTime,
                new BettaHealth().save(), new CompoundTag());
    }

    public boolean isAdult() {
        return STAGE_ADULT.equals(stage);
    }

    public BettaHealth healthState() {
        return BettaHealth.load(health);
    }

    public BettaData withHealth(BettaHealth h) {
        return new BettaData(genome, female, stage, fishId, mother, father, generation, bornTime, h.save(), raw);
    }

    public CompoundTag save() {
        CompoundTag t = raw.copy();
        Schemas.BETTA.stamp(t);
        Tag g = BettaGenome.CODEC.encodeStart(NbtOps.INSTANCE, genome).result().orElseGet(CompoundTag::new);
        t.put("genome", g);
        t.putString("sex", female ? "female" : "male");
        t.putString("stage", stage);
        t.putUUID("fish_id", fishId);
        mother.ifPresentOrElse(u -> t.putUUID("mother", u), () -> t.remove("mother"));
        father.ifPresentOrElse(u -> t.putUUID("father", u), () -> t.remove("father"));
        t.putInt("generation", generation);
        t.putLong("born", bornTime);
        t.put("health", health.copy());
        return t;
    }

    public static BettaData load(CompoundTag in) {
        CompoundTag t = Schemas.BETTA.migrate(in);
        BettaGenome genome = BettaGenome.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, t.getCompound("genome")))
                .result().orElse(BettaGenome.DEFAULT);
        boolean female = "female".equals(t.getString("sex"));
        String stage = t.contains("stage", Tag.TAG_STRING) ? t.getString("stage") : STAGE_ADULT;
        UUID id = t.hasUUID("fish_id") ? t.getUUID("fish_id") : UUID.randomUUID();
        Optional<UUID> mother = t.hasUUID("mother") ? Optional.of(t.getUUID("mother")) : Optional.empty();
        Optional<UUID> father = t.hasUUID("father") ? Optional.of(t.getUUID("father")) : Optional.empty();
        return new BettaData(genome, female, stage, id, mother, father, t.getInt("generation"), t.getLong("born"),
                t.getCompound("health").copy(), t);
    }

    /** Reads the betta data from a bucket's {@code bucket_entity_data} tag (null if there is none). */
    @Nullable
    public static BettaData fromBucketTag(CompoundTag bucketTag) {
        return bucketTag.contains(KEY, Tag.TAG_COMPOUND) ? load(bucketTag.getCompound(KEY)) : null;
    }
}
