package org.overengineer.bettacraft.entity.betta;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.overengineer.bettacraft.data.schema.Schemas;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.genetics.BettaGenotype;

/**
 * Everything that makes one betta individual. One format for entity NBT (key {@link #KEY}), the
 * {@code bettacraft:fish_data} item component and the mirror in {@code minecraft:bucket_entity_data} (same key
 * {@link #KEY}, identical content). Schema-versioned ({@link Schemas#BETTA}); unknown keys are kept in {@code raw}.
 *
 * @param genotype   allele pairs (string ids); the visible fish is {@link #genome()}
 * @param female     sex
 * @param stage      life stage id ({@link LifeStage}); bettas as entities are fry or older
 * @param stageDays  days of growth spent in the current stage
 * @param fishId     stable id of this individual (entity UUIDs change when a fish is bucketed)
 * @param mother     pedigree (fish id)
 * @param father     pedigree (fish id)
 * @param generation 0 = wild / bought
 * @param bornTime   game time of birth (or of first spawn)
 * @param health     {@link BettaHealth} NBT
 * @param breeding   {@link BettaBreeding} NBT
 * @param raw        the full tag as read (unknown keys survive a save)
 */
public record BettaData(BettaGenotype genotype, boolean female, String stage, double stageDays, UUID fishId, Optional<UUID> mother,
                        Optional<UUID> father, int generation, long bornTime, CompoundTag health, CompoundTag breeding, CompoundTag raw) {
    public static final String KEY = "betta";
    public static final String STAGE_ADULT = "adult";

    public static final Codec<BettaData> CODEC = CompoundTag.CODEC.xmap(BettaData::load, BettaData::save);
    public static final StreamCodec<ByteBuf, BettaData> STREAM_CODEC = ByteBufCodecs.COMPOUND_TAG.map(BettaData::load, BettaData::save);

    /** Adult with a homozygous genotype showing {@code genome}. */
    public static BettaData create(BettaGenome genome, boolean female, long bornTime) {
        return create(BettaGenotype.homozygous(genome), female, bornTime);
    }

    public static BettaData create(BettaGenotype genotype, boolean female, long bornTime) {
        return new BettaData(genotype, female, STAGE_ADULT, 0, UUID.randomUUID(), Optional.empty(), Optional.empty(), 0, bornTime,
                new BettaHealth().save(), new CompoundTag(), new CompoundTag());
    }

    /** Offspring from the nest: fry stage, pedigree set. */
    public static BettaData offspring(BettaGenotype genotype, boolean female, @Nullable UUID mother, @Nullable UUID father, int generation,
                                      long bornTime) {
        return new BettaData(genotype, female, LifeStage.FRY.id(), 0, UUID.randomUUID(), Optional.ofNullable(mother),
                Optional.ofNullable(father), generation, bornTime, new BettaHealth().save(), new CompoundTag(), new CompoundTag());
    }

    /** The visible fish (phenotype). */
    public BettaGenome genome() {
        return genotype.express();
    }

    public LifeStage lifeStage() {
        return LifeStage.byId(stage);
    }

    public boolean isAdult() {
        return lifeStage() == LifeStage.ADULT;
    }

    public BettaHealth healthState() {
        return BettaHealth.load(health);
    }

    public BettaBreeding breedingState() {
        return BettaBreeding.load(breeding);
    }

    public BettaData withHealth(BettaHealth h) {
        return new BettaData(genotype, female, stage, stageDays, fishId, mother, father, generation, bornTime, h.save(), breeding, raw);
    }

    public BettaData withLive(BettaHealth h, BettaBreeding b, LifeStage s, double days) {
        return new BettaData(genotype, female, s.id(), days, fishId, mother, father, generation, bornTime, h.save(), b.save(), raw);
    }

    /** M22.3: the hidden genes were analysed with the magnifier (kept in {@code raw}; older bettas: migrated to true). */
    public boolean analysed() {
        return raw.getBoolean("analysed");
    }

    public BettaData withAnalysed(boolean a) {
        CompoundTag r = raw.copy();
        r.putBoolean("analysed", a);
        return new BettaData(genotype, female, stage, stageDays, fishId, mother, father, generation, bornTime, health, breeding, r);
    }

    public BettaData withStage(LifeStage s) {
        return new BettaData(genotype, female, s.id(), 0, fishId, mother, father, generation, bornTime, health, breeding, raw);
    }

    public CompoundTag save() {
        CompoundTag t = raw.copy();
        Schemas.BETTA.stamp(t);
        t.put("genome", genotype.save());
        t.putString("sex", female ? "female" : "male");
        t.putString("stage", stage);
        if (stageDays > 0) t.putDouble("stage_days", stageDays);
        else t.remove("stage_days");
        t.putUUID("fish_id", fishId);
        mother.ifPresentOrElse(u -> t.putUUID("mother", u), () -> t.remove("mother"));
        father.ifPresentOrElse(u -> t.putUUID("father", u), () -> t.remove("father"));
        t.putInt("generation", generation);
        t.putLong("born", bornTime);
        t.put("health", health.copy());
        if (breeding.isEmpty()) t.remove("breeding");
        else t.put("breeding", breeding.copy());
        return t;
    }

    public static BettaData load(CompoundTag in) {
        CompoundTag t = Schemas.BETTA.migrate(in);
        BettaGenotype genotype = BettaGenotype.load(t.getCompound("genome"));
        boolean female = "female".equals(t.getString("sex"));
        String stage = t.contains("stage", Tag.TAG_STRING) ? t.getString("stage") : STAGE_ADULT;
        UUID id = t.hasUUID("fish_id") ? t.getUUID("fish_id") : UUID.randomUUID();
        Optional<UUID> mother = t.hasUUID("mother") ? Optional.of(t.getUUID("mother")) : Optional.empty();
        Optional<UUID> father = t.hasUUID("father") ? Optional.of(t.getUUID("father")) : Optional.empty();
        return new BettaData(genotype, female, stage, BettaHealth.days(t, "stage_days"), id, mother, father, Math.max(0, t.getInt("generation")),
                t.getLong("born"), t.getCompound("health").copy(), t.getCompound("breeding").copy(), t);
    }

    /** Reads the betta data from a bucket's {@code bucket_entity_data} tag (null if there is none). */
    @Nullable
    public static BettaData fromBucketTag(CompoundTag bucketTag) {
        return bucketTag.contains(KEY, Tag.TAG_COMPOUND) ? load(bucketTag.getCompound(KEY)) : null;
    }
}
