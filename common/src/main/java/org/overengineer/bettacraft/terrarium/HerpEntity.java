package org.overengineer.bettacraft.terrarium;

import org.overengineer.bettacraft.util.NbtCompat;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.food.BettaFood;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;

/**
 * M31: an egg-laying terrarium animal of the mod ({@link HerpSpecies}: part b the turtles, part c the dart frogs, the horned
 * frog, the geckos and the corn snake). On top of the {@link TerrariumAnimalEntity}:
 * <ul>
 *     <li>a sex (decided by the nest temperature when it hatched, {@link EggSex}, or genetic; random for a wild one) and a
 *     variety (the slider: red-eared or yellow-bellied; part c: the three dart frogs, the gecko and snake morphs);</li>
 *     <li>part c: the young of the dart frogs hatch as <b>tadpoles</b> ({@link #isTadpole}): they swim only, eat aquarium
 *     food and need water until they turn into froglets at {@link #METAMORPHOSIS} of their growth;</li>
 *     <li>growth from a hatchling over {@link HerpSpecies#growDays} (size, stocking weight, the tank size it needs);</li>
 *     <li>brumation of the tortoise in a cold tank ({@code terrarium.brumation}): it rests with its head pulled in, does
 *     not get hungry and takes no harm from the cold (only from frost);</li>
 *     <li>the nesting walk of a female ({@link HerpBreeding}) and a bucket of water or a terrarium box.</li>
 * </ul>
 */
public class HerpEntity extends TerrariumAnimalEntity implements Bucketable {
    /** Part c: a tadpole becomes a froglet at this growth (0..1). */
    public static final float METAMORPHOSIS = 0.4F;
    private static final EntityDataAccessor<Integer> DATA_VARIETY = SynchedEntityData.defineId(HerpEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BRUMATING = SynchedEntityData.defineId(HerpEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_GROWTH = SynchedEntityData.defineId(HerpEntity.class, EntityDataSerializers.FLOAT);
    /** Air (°C) below which even a brumating tortoise takes harm (frost). */
    public static final double FROST = 2.0;
    private static final double TICKS_PER_DAY = 24000.0;

    private final HerpSpecies species;
    private boolean female;
    private boolean fromBucket;
    private int generation;
    /** Game time from which a female may lay again. */
    private long nextLay;
    @Nullable
    private Vec3 nestTarget;
    /** The air of its tank was below {@link #FROST} at the last care step. */
    private boolean frost;
    /** M49: a brackish water animal sits in fresh water (set by the care step). */
    private boolean fresh;
    private int nestTicks;

    public HerpEntity(EntityType<? extends Animal> type, Level level, HerpSpecies species) {
        super(type, level);
        this.species = species;
        this.female = level.getRandom().nextBoolean();
    }

    public static AttributeSupplier.Builder createAttributes(HerpSpecies s) {
        double health = s.stock() >= 6 ? 16.0 : s.stock() >= 4 ? 12.0 : 8.0;
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, health).add(Attributes.MOVEMENT_SPEED, s.box() ? 0.08 : 0.12);
    }

    public HerpSpecies species() {
        return species;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(DATA_VARIETY, 0);
        b.define(DATA_BRUMATING, false);
        b.define(DATA_GROWTH, 1F);
    }

    // ------------------------------------------------------------------ sex, variety, growth

    public boolean isFemale() {
        return female;
    }

    public void setFemale(boolean f) {
        female = f;
    }

    public int variety() {
        return Mth.clamp(entityData.get(DATA_VARIETY), 0, species.varieties().size() - 1);
    }

    public void setVariety(int v) {
        entityData.set(DATA_VARIETY, Mth.clamp(v, 0, species.varieties().size() - 1));
    }

    public int generation() {
        return generation;
    }

    public void setGeneration(int g) {
        generation = Math.max(0, g);
    }

    /** Ticks from hatchling to grown. */
    public int growTicks() {
        return (int) Math.max(1, Math.round(species.growDays() * TICKS_PER_DAY));
    }

    /** 0 = just hatched .. 1 = grown (synced). */
    public float growth() {
        if (level().isClientSide()) return entityData.get(DATA_GROWTH);
        return isBaby() ? (float) Mth.clamp(1.0 + getAge() / (double) growTicks(), 0, 1) : 1F;
    }

    /** Makes it a hatchling (it grows up over {@link #growTicks}). */
    public void makeHatchling() {
        setAge(-growTicks());
        entityData.set(DATA_GROWTH, 0F);
        refreshDimensions();
    }

    @Override
    public float getAgeScale() {
        if (!isBaby()) return 1F;
        return species.hatchScale() + (1F - species.hatchScale()) * growth();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && isBaby() && tickCount % 100 == 0) {
            float g = growth();
            if (Math.abs(g - entityData.get(DATA_GROWTH)) > 0.005F) {
                entityData.set(DATA_GROWTH, g);
                refreshDimensions();
            }
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_GROWTH.equals(key)) refreshDimensions();
    }

    @Override
    protected void ageBoundaryReached() {
        super.ageBoundaryReached();
        entityData.set(DATA_GROWTH, 1F);
    }

    @Override
    protected Component getTypeName() {
        if (species.varieties().size() > 1) return Component.translatable(species.translationKey() + "." + species.varieties().get(variety()));
        return super.getTypeName();
    }

    // ------------------------------------------------------------------ needs, food

    /** Part c: a dart frog tadpole (in the water until {@link #METAMORPHOSIS}). */
    public boolean isTadpole() {
        return species.tadpoles() && isBaby() && growth() < METAMORPHOSIS;
    }

    /** Part c: what a tadpole needs: the water of a paludarium at the species' water temperature, nothing of the air. */
    public static TerrariumNeeds tadpoleNeeds(HerpSpecies s) {
        TerrariumNeeds n = s.needs();
        return new TerrariumNeeds(TerrariumNeeds.Habitat.PALUDARIUM, TerrariumNeeds.Range.ANY, TerrariumNeeds.Range.ANY, 0, TerrariumNeeds.Range.ANY, false,
                n.water(), 0);
    }

    @Override
    public TerrariumNeeds terrariumNeeds() {
        return isTadpole() ? tadpoleNeeds(species) : species.needs(!isBaby());
    }

    @Override
    public LandWalker.Gait gait() {
        return isTadpole() ? species.gait().with(LandWalker.Style.SWIM) : species.gait();
    }

    @Override
    protected boolean usesBox() {
        return species.box();
    }

    /** Hungry enough to eat (the snake waits about a week after a meal). */
    private boolean hungry() {
        return !isBrumating() && daysSinceFed() >= species.mealDays() - 1e-9;
    }

    @Override
    public boolean eatsFood(BettaFood.Kind kind) {
        if (isTadpole()) return kind == BettaFood.Kind.NORMAL || kind == BettaFood.Kind.FRY || kind == BettaFood.Kind.FRY_START; // algae, fine food
        return species.eats(TerrariumFood.Diet.ANIMAL) && (kind == BettaFood.Kind.NORMAL || kind == BettaFood.Kind.LIVE) && hungry();
    }

    @Override
    public boolean eats(TerrariumFood.Diet d) {
        return !isTadpole() && species.eats(d) && hungry();
    }

    @Override
    public double stockWeight() {
        return species.stock() * (isBaby() ? 0.1 + 0.9 * growth() : 1.0);
    }

    @Override
    public double starveDays() {
        return species.starveDays();
    }

    // ------------------------------------------------------------------ brumation

    public boolean isBrumating() {
        return entityData.get(DATA_BRUMATING);
    }

    public void setBrumating(boolean b) {
        if (b != isBrumating()) entityData.set(DATA_BRUMATING, b);
    }

    public static boolean brumationOn() {
        return ServerConfig.getOr(ServerConfig.TERRA_BRUMATION, true);
    }

    public static double brumationBelow() {
        return ServerConfig.getOr(ServerConfig.TERRA_BRUMATION_BELOW, 12.0);
    }

    @Override
    protected boolean resting() {
        return isBrumating();
    }

    @Override
    public void onTerrariumCare(ServerLevel level, TankBlockEntity c, TerrariumNeeds.Assessment a, double days) {
        TankData t = c.getTank();
        if (species.brumates() && brumationOn() && t.hasClimate()) {
            double air = t.airTemp(), below = brumationBelow();
            if (!isBrumating() && air < below) {
                setBrumating(true);
                org.overengineer.bettacraft.tank.TankCare.log(level, c, "log.bettacraft.turtle.brumation_start", org.overengineer.bettacraft.tank.TankCare.nameArg(this));
            } else if (isBrumating() && air > below + 2) {
                setBrumating(false);
                org.overengineer.bettacraft.tank.TankCare.log(level, c, "log.bettacraft.turtle.brumation_end", org.overengineer.bettacraft.tank.TankCare.nameArg(this));
            }
        } else if (isBrumating()) {
            setBrumating(false);
        }
        frost = t.hasClimate() && t.airTemp() < FROST;
        fresh = species.brackish() && org.overengineer.bettacraft.terrarium.Mudskippers.fresh(t); // M49
        if (isBrumating()) pauseHunger(days);
        super.onTerrariumCare(level, c, adjust(a), days);
    }

    /** A brumating tortoise does not mind the cold, the missing basking spot or UV; frost and the wrong tank still count. */
    @Override
    public TerrariumNeeds.Assessment adjust(TerrariumNeeds.Assessment a) {
        if (fresh) { // M49: the mudskipper in fresh water
            EnumSet<TerrariumNeeds.Problem> p = EnumSet.noneOf(TerrariumNeeds.Problem.class);
            p.addAll(a.problems());
            p.add(TerrariumNeeds.Problem.FRESH_WATER);
            a = new TerrariumNeeds.Assessment(p, a.severity() + 0.1);
        }
        if (!isBrumating() || a.fine()) return a;
        EnumSet<TerrariumNeeds.Problem> left = EnumSet.noneOf(TerrariumNeeds.Problem.class);
        for (var p : a.problems()) {
            if (p == TerrariumNeeds.Problem.TOO_COLD && !frost) continue;
            if (p == TerrariumNeeds.Problem.NO_BASKING || p == TerrariumNeeds.Problem.NO_UV || p == TerrariumNeeds.Problem.TOO_DRY) continue;
            left.add(p);
        }
        return left.isEmpty() ? TerrariumNeeds.Assessment.FINE : new TerrariumNeeds.Assessment(left, frost ? a.severity() : 0.1);
    }

    // ------------------------------------------------------------------ breeding (HerpBreeding)

    public boolean layReady(long now) {
        return female && !isBaby() && !isBrumating() && now >= nextLay;
    }

    public void laid(long now, double intervalDays) {
        nextLay = now + Math.round(intervalDays * TICKS_PER_DAY);
        nestTarget = null;
        nestTicks = 0;
    }

    /** GameTests: she may lay now. */
    public void setNextLayForTest(long time) {
        nextLay = time;
    }

    @Nullable
    public Vec3 nestTarget() {
        return nestTarget;
    }

    /** Walk to the nest (up to 30 s). */
    public void goNest(ServerLevel level, TankData t, Vec3 nest) {
        nestTarget = nest;
        nestTicks = 0;
        walker.goTo(this, level, t, nest, 600);
    }

    /** One more second on the way to the nest; true when she gave up (the walker lost it). */
    public boolean nestTimeout() {
        nestTicks += 20;
        if (nestTicks > 900) {
            nestTarget = null;
            nestTicks = 0;
            return true;
        }
        return false;
    }

    public void forgetNest() {
        nestTarget = null;
        nestTicks = 0;
    }

    // ------------------------------------------------------------------ spawning

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnType, @Nullable SpawnGroupData data) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, spawnType, data);
        female = random.nextBoolean();
        if (species.varieties().size() > 1) setVariety(HerpSpawns.variety(species, level.getBiome(blockPosition()), random, spawnType));
        wild = spawnType == EntitySpawnReason.NATURAL;
        if (isBaby()) makeHatchling();
        return out;
    }

    // ------------------------------------------------------------------ bucket / box

    @Override
    public boolean fromBucket() {
        return fromBucket;
    }

    @Override
    public void setFromBucket(boolean b) {
        fromBucket = b;
    }

    /** What the bucket or box keeps besides the vanilla data. */
    public CompoundTag saveTurtle() {
        CompoundTag t = saveTerrarium();
        t.putBoolean("female", female);
        t.putInt("variety", variety());
        t.putInt("age", getAge());
        t.putInt("gen", generation);
        if (isBrumating()) t.putBoolean("brumating", true);
        return t;
    }

    public void loadTurtle(CompoundTag t) {
        loadTerrarium(t);
        if (t.contains("female")) female = t.getBooleanOr("female", false);
        setVariety(t.getIntOr("variety", 0));
        if (t.contains("age")) setAge(t.getIntOr("age", 0));
        generation = t.getIntOr("gen", 0);
        setBrumating(t.getBooleanOr("brumating", false));
        if (isBaby()) entityData.set(DATA_GROWTH, growth());
        refreshDimensions();
    }

    @Override
    public void saveToBucketTag(ItemStack stack) {
        Bucketable.saveDefaultDataToBucketTag(this, stack);
        CompoundTag t = saveTurtle();
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, stack, c -> c.put("terrarium", t));
    }

    @Override
    public void loadFromBucketTag(CompoundTag tag) {
        Bucketable.loadDefaultDataFromBucketTag(this, tag);
        wild = false;
        if (tag.contains("terrarium")) loadTurtle(tag.getCompoundOrEmpty("terrarium"));
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(ModItems.herpContainer(species));
    }

    @Override
    public SoundEvent getPickupSound() {
        return species.box() ? SoundEvents.ARMOR_EQUIP_LEATHER.value() : SoundEvents.BUCKET_FILL_FISH;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || fromBucket;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        CompoundTag tag = new CompoundTag();
        super.addAdditionalSaveData(output);
        tag.putBoolean("FromBucket", fromBucket);
        tag.putBoolean("Female", female);
        tag.putInt("Variety", variety());
        tag.putInt("Generation", generation);
        tag.putLong("NextLay", nextLay);
        tag.putBoolean("Brumating", isBrumating());
        NbtCompat.write(output, tag);
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        CompoundTag tag = NbtCompat.read(input);

        super.readAdditionalSaveData(input);
        fromBucket = tag.getBooleanOr("FromBucket", false);
        if (tag.contains("Female")) female = tag.getBooleanOr("Female", false);
        setVariety(tag.getIntOr("Variety", 0));
        generation = tag.getIntOr("Generation", 0);
        nextLay = tag.getLongOr("NextLay", 0L);
        setBrumating(tag.getBooleanOr("Brumating", false));
        if (isBaby()) entityData.set(DATA_GROWTH, growth());
    }

    // ------------------------------------------------------------------ sounds by body plan (vanilla sounds)

    /** M52c (26.1): the hiss of the classic cat (the cat sounds are sound variants since 26.1). */
    private static final SoundEvent CAT_HISS = SoundEvents.CAT_SOUNDS.get(net.minecraft.world.entity.animal.feline.CatSoundVariants.SoundSet.CLASSIC)
            .adultSounds().hissSound().value();

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return switch (species.body()) {
            case TURTLE -> isBaby() ? SoundEvents.TURTLE_HURT_BABY : SoundEvents.TURTLE_HURT;
            case FROG -> SoundEvents.FROG_HURT;
            case GECKO -> SoundEvents.ARMADILLO_HURT;
            case SNAKE -> CAT_HISS; // M52c (26.1): the cat sounds are variants
            case MUDSKIPPER -> SoundEvents.TROPICAL_FISH_HURT;
        };
    }

    @Override
    protected SoundEvent getDeathSound() {
        return switch (species.body()) {
            case TURTLE -> isBaby() ? SoundEvents.TURTLE_DEATH_BABY : SoundEvents.TURTLE_DEATH;
            case FROG -> SoundEvents.FROG_DEATH;
            case GECKO -> SoundEvents.ARMADILLO_DEATH;
            case SNAKE -> CAT_HISS; // M52c (26.1): the cat sounds are variants
            case MUDSKIPPER -> SoundEvents.TROPICAL_FISH_DEATH;
        };
    }

    @Override
    protected SoundEvent getAmbientSound() {
        // part c: a frog calls now and then (a dart frog high and soft), the others are silent
        return species.body() == HerpSpecies.Body.FROG && !isTadpole() && random.nextInt(4) == 0 ? SoundEvents.FROG_AMBIENT : null;
    }

    @Override
    public float getVoicePitch() {
        return species == HerpSpecies.DART_FROG ? 1.8F : super.getVoicePitch();
    }

    @Override
    protected float getSoundVolume() {
        return species.body() == HerpSpecies.Body.TURTLE ? super.getSoundVolume() : 0.35F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 600;
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        switch (species.body()) {
            case TURTLE -> playSound(isBaby() ? SoundEvents.TURTLE_SHAMBLE_BABY : SoundEvents.TURTLE_SHAMBLE, 0.15F, 1.0F);
            case FROG -> playSound(SoundEvents.FROG_STEP, 0.1F, 1.4F);
            case MUDSKIPPER -> playSound(SoundEvents.TROPICAL_FISH_FLOP, 0.06F, 1.6F);
            default -> playSound(SoundEvents.ARMADILLO_STEP, 0.08F, 1.5F);
        }
    }
}
