package org.overengineer.bettacraft.entity.betta;

import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModItems;

/**
 * The betta (PLAN 5.1). Wild bettas have no needs. Inside a Bettacraft tank (attachment {@code bettacraft:tank_ref})
 * the fish is confined to the tank's water and its health timers run ({@link BettaCare}).
 */
public class BettaEntity extends AbstractFish {
    /** Genome, sex and stage for the renderer (server -> client). */
    private static final EntityDataAccessor<CompoundTag> DATA_LOOK = SynchedEntityData.defineId(BettaEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Boolean> DATA_SICK = SynchedEntityData.defineId(BettaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final ResourceLocation SICK_SLOW = BettaCraft.id("sick_slowness");

    private BettaData data;
    BettaHealth health = new BettaHealth();
    /** Game time up to which the health timers have been simulated (-1 = never). */
    long lastSimTime = -1;
    /** False until the first server tick after loading (catch-up or "time stood still"). */
    boolean simStarted;
    /** Water volume of the tank this fish lives in (server, refreshed regularly); null outside tanks. */
    @Nullable
    AABB tankWater;
    /** Hand-fed food sinking here (server); bettas swim towards it until {@link #foodUntil}. */
    @Nullable
    Vec3 foodTarget;
    long foodUntil;

    // client cache of DATA_LOOK
    private BettaGenome lookGenome = BettaGenome.DEFAULT;
    private boolean lookFemale;
    private String lookStage = BettaData.STAGE_ADULT;

    public BettaEntity(EntityType<? extends BettaEntity> type, Level level) {
        super(type, level);
        this.data = BettaData.create(BettaGenome.DEFAULT, false, 0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.7);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LOOK, new CompoundTag());
        builder.define(DATA_SICK, false);
    }

    @Override
    protected void registerGoals() {
        // Not calling super: AbstractFish's random swim goal is replaced by one that knows the tank.
        this.goalSelector.addGoal(0, new PanicGoal(this, 1.25));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 8.0F, 1.6, 1.4,
                e -> EntitySelector.NO_SPECTATORS.test(e) && tankWater == null));
        this.goalSelector.addGoal(4, new BettaSwimGoal(this));
    }

    // ------------------------------------------------------------------ data

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData groupData) {
        boolean wild = spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION;
        BettaGenome genome = wild ? BettaGenome.randomWild(random) : BettaGenome.randomDomestic(random);
        setBettaData(BettaData.create(genome, random.nextBoolean(), level.getLevel().getGameTime()));
        return super.finalizeSpawn(level, difficulty, spawnType, groupData);
    }

    /** Current data including the live health timers (server). */
    public BettaData getBettaData() {
        return data.withHealth(health);
    }

    public void setBettaData(BettaData d) {
        this.data = d;
        this.health = d.healthState();
        syncLook();
    }

    void syncLook() {
        CompoundTag look = new CompoundTag();
        BettaGenome.CODEC.encodeStart(NbtOps.INSTANCE, data.genome()).result().ifPresent(t -> look.put("genome", t));
        look.putBoolean("female", data.female());
        look.putString("stage", data.stage());
        entityData.set(DATA_LOOK, look);
        entityData.set(DATA_SICK, health.isSick());
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            if (health.isSick()) {
                speed.addOrUpdateTransientModifier(new AttributeModifier(SICK_SLOW, -0.4, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else {
                speed.removeModifier(SICK_SLOW);
            }
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_LOOK.equals(key)) {
            CompoundTag look = entityData.get(DATA_LOOK);
            lookGenome = BettaGenome.CODEC.parse(NbtOps.INSTANCE, look.getCompound("genome")).result().orElse(BettaGenome.DEFAULT);
            lookFemale = look.getBoolean("female");
            lookStage = look.contains("stage", Tag.TAG_STRING) ? look.getString("stage") : BettaData.STAGE_ADULT;
        }
    }

    /** Genome as known on this side (server: authoritative, client: synced). */
    public BettaGenome genome() {
        return level().isClientSide ? lookGenome : data.genome();
    }

    public boolean isFemale() {
        return level().isClientSide ? lookFemale : data.female();
    }

    public String stage() {
        return level().isClientSide ? lookStage : data.stage();
    }

    public boolean isAdult() {
        return BettaData.STAGE_ADULT.equals(stage());
    }

    public boolean isSick() {
        return entityData.get(DATA_SICK);
    }

    public BettaHealth health() {
        return health;
    }

    /** Hand-fed food: swim towards {@code spot} until game time {@code until}. */
    public void setFoodTarget(Vec3 spot, long until) {
        this.foodTarget = spot;
        this.foodUntil = until;
    }

    public boolean isInTank() {
        return tankWater != null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put(BettaData.KEY, getBettaData().save());
        tag.putLong("last_sim", lastSimTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(BettaData.KEY, Tag.TAG_COMPOUND)) {
            setBettaData(BettaData.load(tag.getCompound(BettaData.KEY)));
        }
        lastSimTime = tag.contains("last_sim") ? tag.getLong("last_sim") : -1;
        simStarted = false;
    }

    // ------------------------------------------------------------------ bucket

    @Override
    public void saveToBucketTag(ItemStack stack) {
        Bucketable.saveDefaultDataToBucketTag(this, stack);
        BettaData d = getBettaData();
        // identical keys in entity NBT and bucket data (key "betta")
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, stack, t -> t.put(BettaData.KEY, d.save()));
        stack.set(ModDataComponents.FISH_DATA.get(), d);
    }

    @Override
    public void loadFromBucketTag(CompoundTag tag) {
        Bucketable.loadDefaultDataFromBucketTag(this, tag);
        BettaData d = BettaData.fromBucketTag(tag);
        if (d != null) setBettaData(d);
        // time in a bucket stands still
        lastSimTime = level().getGameTime();
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(ModItems.BETTA_BUCKET.get());
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server && isAlive()) {
            BettaCare.serverTick(server, this);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        AABB box = tankWater;
        if (box != null && !level().isClientSide && isInWater()) {
            // Keep our bettas inside the tank water (walls stop them anyway; this keeps them off the surface).
            Vec3 p = position();
            double cx = Mth.clamp(p.x, box.minX + 0.2, box.maxX - 0.2);
            double cy = Mth.clamp(p.y, box.minY + 0.05, Math.max(box.minY + 0.05, box.maxY - 0.3));
            double cz = Mth.clamp(p.z, box.minZ + 0.2, box.maxZ - 0.2);
            if (cx != p.x || cy != p.y || cz != p.z) {
                setDeltaMovement(getDeltaMovement().scale(0.5).add((cx - p.x) * 0.05, (cy - p.y) * 0.05, (cz - p.z) * 0.05));
            }
        }
    }

    @Override
    protected boolean canRandomSwim() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return tankWater == null && super.removeWhenFarAway(distance);
    }

    // ------------------------------------------------------------------ sounds

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.TROPICAL_FISH_FLOP;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.TROPICAL_FISH_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.TROPICAL_FISH_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.TROPICAL_FISH_HURT;
    }
}
