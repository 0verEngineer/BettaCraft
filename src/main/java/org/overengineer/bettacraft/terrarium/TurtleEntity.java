package org.overengineer.bettacraft.terrarium;

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
import net.minecraft.world.entity.MobSpawnType;
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
 * M31 part b: a turtle of the mod ({@link TurtleSpecies}: musk turtle, slider, Hermann's tortoise, Reeves' turtle). On top
 * of the {@link TerrariumAnimalEntity}:
 * <ul>
 *     <li>a sex (decided by the nest temperature when it hatched, {@link TurtleSex}; random for a wild one) and a variety
 *     (the slider: red-eared or yellow-bellied);</li>
 *     <li>growth from a hatchling over {@link TurtleSpecies#growDays} (size, stocking weight, the tank size it needs);</li>
 *     <li>brumation of the tortoise in a cold tank ({@code terrarium.brumation}): it rests with its head pulled in, does
 *     not get hungry and takes no harm from the cold (only from frost);</li>
 *     <li>the nesting walk of a female ({@link TurtleBreeding}) and a bucket of water or a terrarium box.</li>
 * </ul>
 */
public class TurtleEntity extends TerrariumAnimalEntity implements Bucketable {
    private static final EntityDataAccessor<Integer> DATA_VARIETY = SynchedEntityData.defineId(TurtleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BRUMATING = SynchedEntityData.defineId(TurtleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_GROWTH = SynchedEntityData.defineId(TurtleEntity.class, EntityDataSerializers.FLOAT);
    /** Air (°C) below which even a brumating tortoise takes harm (frost). */
    public static final double FROST = 2.0;
    private static final double TICKS_PER_DAY = 24000.0;

    private final TurtleSpecies species;
    private boolean female;
    private boolean fromBucket;
    private int generation;
    /** Game time from which a female may lay again. */
    private long nextLay;
    @Nullable
    private Vec3 nestTarget;
    /** The air of its tank was below {@link #FROST} at the last care step. */
    private boolean frost;
    private int nestTicks;

    public TurtleEntity(EntityType<? extends Animal> type, Level level, TurtleSpecies species) {
        super(type, level);
        this.species = species;
        this.female = level.random.nextBoolean();
    }

    public static AttributeSupplier.Builder createAttributes(TurtleSpecies s) {
        double health = s.stock() >= 6 ? 16.0 : s.stock() >= 4 ? 12.0 : 8.0;
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, health).add(Attributes.MOVEMENT_SPEED, s.box() ? 0.08 : 0.12);
    }

    public TurtleSpecies species() {
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
        if (level().isClientSide) return entityData.get(DATA_GROWTH);
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
        if (!level().isClientSide && isBaby() && tickCount % 100 == 0) {
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

    @Override
    public TerrariumNeeds terrariumNeeds() {
        return species.needs(!isBaby());
    }

    @Override
    public LandWalker.Gait gait() {
        return species.gait();
    }

    @Override
    public boolean eatsFood(BettaFood.Kind kind) {
        return species.animalFood() && (kind == BettaFood.Kind.NORMAL || kind == BettaFood.Kind.LIVE) && !isBrumating();
    }

    @Override
    public boolean eatsGreens() {
        return species.greens() && !isBrumating();
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
        if (isBrumating()) pauseHunger(days);
        super.onTerrariumCare(level, c, adjust(a), days);
    }

    /** A brumating tortoise does not mind the cold, the missing basking spot or UV; frost and the wrong tank still count. */
    @Override
    public TerrariumNeeds.Assessment adjust(TerrariumNeeds.Assessment a) {
        if (!isBrumating() || a.fine()) return a;
        EnumSet<TerrariumNeeds.Problem> left = EnumSet.noneOf(TerrariumNeeds.Problem.class);
        for (var p : a.problems()) {
            if (p == TerrariumNeeds.Problem.TOO_COLD && !frost) continue;
            if (p == TerrariumNeeds.Problem.NO_BASKING || p == TerrariumNeeds.Problem.NO_UV || p == TerrariumNeeds.Problem.TOO_DRY) continue;
            left.add(p);
        }
        return left.isEmpty() ? TerrariumNeeds.Assessment.FINE : new TerrariumNeeds.Assessment(left, frost ? a.severity() : 0.1);
    }

    // ------------------------------------------------------------------ breeding (TurtleBreeding)

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
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData data) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, spawnType, data);
        female = random.nextBoolean();
        if (species.varieties().size() > 1) setVariety(TurtleSpawns.variety(species, level.getBiome(blockPosition()), random));
        wild = spawnType == MobSpawnType.NATURAL;
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
        if (t.contains("female")) female = t.getBoolean("female");
        setVariety(t.getInt("variety"));
        if (t.contains("age")) setAge(t.getInt("age"));
        generation = t.getInt("gen");
        setBrumating(t.getBoolean("brumating"));
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
        if (tag.contains("terrarium")) loadTurtle(tag.getCompound("terrarium"));
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(ModItems.turtleContainer(species));
    }

    @Override
    public SoundEvent getPickupSound() {
        return species.box() ? SoundEvents.ARMOR_EQUIP_LEATHER.value() : SoundEvents.BUCKET_FILL_FISH;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (species.box()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(ModItems.TERRARIUM_BOX.get()) && isAlive()) {
                playSound(getPickupSound(), 1.0F, 1.0F);
                if (!level().isClientSide) {
                    org.overengineer.bettacraft.tank.TankInhabitants.release(this);
                    ItemStack filled = getBucketItemStack();
                    saveToBucketTag(filled);
                    player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, filled, false));
                    level().playSound(null, blockPosition(), getPickupSound(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                    discard();
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            return super.mobInteract(player, hand);
        }
        return Bucketable.bucketMobPickup(player, hand, this).orElse(super.mobInteract(player, hand));
    }

    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || fromBucket;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("FromBucket", fromBucket);
        tag.putBoolean("Female", female);
        tag.putInt("Variety", variety());
        tag.putInt("Generation", generation);
        tag.putLong("NextLay", nextLay);
        tag.putBoolean("Brumating", isBrumating());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        fromBucket = tag.getBoolean("FromBucket");
        if (tag.contains("Female")) female = tag.getBoolean("Female");
        setVariety(tag.getInt("Variety"));
        generation = tag.getInt("Generation");
        nextLay = tag.getLong("NextLay");
        setBrumating(tag.getBoolean("Brumating"));
        if (isBaby()) entityData.set(DATA_GROWTH, growth());
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return isBaby() ? SoundEvents.TURTLE_HURT_BABY : SoundEvents.TURTLE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return isBaby() ? SoundEvents.TURTLE_DEATH_BABY : SoundEvents.TURTLE_DEATH;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(isBaby() ? SoundEvents.TURTLE_SHAMBLE_BABY : SoundEvents.TURTLE_SHAMBLE, 0.15F, 1.0F);
    }
}
