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
import net.minecraft.world.entity.Entity;
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
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModItems;

/**
 * The betta (PLAN 5.1). Wild bettas have no needs. Inside a Bettacraft tank (attachment {@code bettacraft:tank_ref})
 * the fish is confined to the tank's water, its health timers run and it grows ({@link BettaCare}); breeding, fights
 * and predators are handled per tank ({@link org.overengineer.bettacraft.tank.TankBreeding}).
 */
public class BettaEntity extends AbstractFish {
    /** Genome, sex and stage for the renderer (server -> client). */
    private static final EntityDataAccessor<CompoundTag> DATA_LOOK = SynchedEntityData.defineId(BettaEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Boolean> DATA_SICK = SynchedEntityData.defineId(BettaEntity.class, EntityDataSerializers.BOOLEAN);
    /** Bit 0: flaring, bit 1: breeding colours (female bars). */
    private static final EntityDataAccessor<Byte> DATA_FLAGS = SynchedEntityData.defineId(BettaEntity.class, EntityDataSerializers.BYTE);
    private static final ResourceLocation SICK_SLOW = BettaCraft.id("sick_slowness");
    public static final int FLAG_FLARE = 1, FLAG_MOOD = 2;

    private BettaData data;
    BettaHealth health = new BettaHealth();
    BettaBreeding breeding = new BettaBreeding();
    LifeStage liveStage = LifeStage.ADULT;
    double stageDays;
    /** Game time up to which the health timers have been simulated (-1 = never). */
    long lastSimTime = -1;
    /** False until the first server tick after loading (catch-up or "time stood still"). */
    boolean simStarted;
    /** Water volume of the tank this fish lives in (server, refreshed regularly; M10: the actual shape); null outside tanks. */
    @Nullable
    org.overengineer.bettacraft.tank.TankWaterVolume tankWater;
    /** Hand-fed food sinking here (server); bettas swim towards it until {@link #foodUntil}. */
    @Nullable
    Vec3 foodTarget;
    long foodUntil;

    // behaviour set by the tank simulation (server, transient)
    @Nullable
    Entity pursueTarget;
    long pursueUntil;
    @Nullable
    Entity fleeFrom;
    long fleeUntil;
    @Nullable
    Vec3 nestSpot;
    long nestUntil;
    long flareUntil, moodUntil;
    /** Fractional damage (fights, chasing) not applied yet; applied in whole points. */
    double pendingDamage;
    @Nullable
    Entity pendingAttacker;

    // client cache of DATA_LOOK
    private BettaGenome lookGenome = BettaGenome.DEFAULT;
    private boolean lookFemale;
    private LifeStage lookStage = LifeStage.ADULT;

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
        builder.define(DATA_FLAGS, (byte) 0);
    }

    @Override
    protected void registerGoals() {
        // Not calling super: AbstractFish's random swim goal is replaced by one that knows the tank.
        this.goalSelector.addGoal(0, new PanicGoal(this, 1.25) {
            @Override
            public boolean canUse() {
                return pursueTarget == null && super.canUse();
            }
        });
        this.goalSelector.addGoal(1, new BettaBehaviourGoals.Flee(this));
        this.goalSelector.addGoal(1, new BettaBehaviourGoals.Pursue(this));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 8.0F, 1.6, 1.4,
                e -> EntitySelector.NO_SPECTATORS.test(e) && tankWater == null));
        this.goalSelector.addGoal(3, new BettaBehaviourGoals.Nest(this));
        this.goalSelector.addGoal(4, new BettaSwimGoal(this));
    }

    // ------------------------------------------------------------------ data

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData groupData) {
        boolean wild = spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION;
        BettaGenotype genotype = wild ? BettaGenotype.randomWild(random) : BettaGenotype.randomDomestic(random);
        setBettaData(BettaData.create(genotype, random.nextBoolean(), level.getLevel().getGameTime()));
        return super.finalizeSpawn(level, difficulty, spawnType, groupData);
    }

    /** Current data including the live health, breeding and growth state (server). */
    public BettaData getBettaData() {
        return data.withLive(health, breeding, liveStage, stageDays);
    }

    public void setBettaData(BettaData d) {
        this.data = d;
        this.health = d.healthState();
        this.breeding = d.breedingState();
        this.liveStage = d.lifeStage().isEntity() ? d.lifeStage() : LifeStage.FRY;
        this.stageDays = d.stageDays();
        syncLook();
        refreshDimensions();
    }

    /** Moves to the next life stage (server). */
    void setStage(LifeStage s) {
        this.liveStage = s;
        this.stageDays = 0;
        this.data = data.withStage(s);
        syncLook();
        refreshDimensions();
    }

    void syncLook() {
        CompoundTag look = new CompoundTag();
        BettaGenome.CODEC.encodeStart(NbtOps.INSTANCE, data.genome()).result().ifPresent(t -> look.put("genome", t));
        look.putBoolean("female", data.female());
        look.putString("stage", liveStage.id());
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
            lookStage = look.contains("stage", Tag.TAG_STRING) ? LifeStage.byId(look.getString("stage")) : LifeStage.ADULT;
            refreshDimensions();
        }
    }

    /** Genome as known on this side (server: authoritative, client: synced). */
    public BettaGenome genome() {
        return level().isClientSide ? lookGenome : data.genome();
    }

    /** Genotype (server only; the client sees it through the tank menu). */
    public BettaGenotype genotype() {
        return data.genotype();
    }

    public boolean isFemale() {
        return level().isClientSide ? lookFemale : data.female();
    }

    public LifeStage lifeStage() {
        LifeStage s = level() != null && level().isClientSide ? lookStage : liveStage;
        return s == null ? LifeStage.ADULT : s;
    }

    public String stage() {
        return lifeStage().id();
    }

    public boolean isAdult() {
        return lifeStage() == LifeStage.ADULT;
    }

    /** Progress 0..1 within the current stage (server). */
    public double stageProgress() {
        double d = liveStage.days();
        return Double.isInfinite(d) || d <= 0 ? 1.0 : Mth.clamp(stageDays / d, 0, 1);
    }

    /**
     * Young fish are tiny (fry 1/4 size): a slightly larger target for the crosshair (bucket, fish bag, Jade) without
     * changing the collision box.
     */
    @Override
    public float getPickRadius() {
        return switch (lifeStage()) {
            case FRY, EGG, LARVA -> 0.18F;
            case JUVENILE -> 0.1F;
            default -> 0.0F;
        };
    }

    @Override
    public boolean isBaby() {
        return !isAdult();
    }

    @Override
    public float getAgeScale() {
        return lifeStage().scale();
    }

    public boolean isSick() {
        return entityData.get(DATA_SICK);
    }

    public boolean isFlaring() {
        return (entityData.get(DATA_FLAGS) & FLAG_FLARE) != 0;
    }

    public boolean isInBreedingMood() {
        return (entityData.get(DATA_FLAGS) & FLAG_MOOD) != 0;
    }

    public BettaHealth health() {
        return health;
    }

    public BettaBreeding breeding() {
        return breeding;
    }

    /** Hand-fed food: swim towards {@code spot} until game time {@code until}. */
    public void setFoodTarget(Vec3 spot, long until) {
        this.foodTarget = spot;
        this.foodUntil = until;
    }

    /** Chase {@code target} (fights, nest defence) for a while. */
    public void pursue(Entity target, long until) {
        this.pursueTarget = target;
        this.pursueUntil = until;
    }

    public void flee(Entity from, long until) {
        this.fleeFrom = from;
        this.fleeUntil = until;
    }

    /** Stay below the nest (building, guarding, spawning). */
    public void keepAtNest(Vec3 spot, long until) {
        this.nestSpot = spot;
        this.nestUntil = until;
    }

    public void flare(long until) {
        this.flareUntil = Math.max(flareUntil, until);
    }

    public void showBreedingMood(long until) {
        this.moodUntil = Math.max(moodUntil, until);
    }

    /**
     * Adds damage from a fight or chase; whole points are dealt as a hit by {@code attacker} (fractions accumulate so
     * slow damage over a day stays exact without a red flash every second).
     */
    public void addDamage(double amount, @Nullable Entity attacker) {
        pendingDamage += amount;
        if (attacker != null) pendingAttacker = attacker;
        if (pendingDamage >= 1.0) {
            float dmg = (float) Math.floor(pendingDamage);
            pendingDamage -= dmg;
            DamageSource src = pendingAttacker instanceof net.minecraft.world.entity.LivingEntity le
                    ? damageSources().mobAttack(le) : damageSources().generic();
            invulnerableTime = 0;
            hurt(src, dmg);
        }
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

    /** Bucket pickup (vanilla, via AbstractFish): counts as catching a betta (advancement, handbook). */
    @Override
    protected net.minecraft.world.InteractionResult mobInteract(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        boolean wasPresent = !isRemoved();
        net.minecraft.world.InteractionResult r = super.mobInteract(player, hand);
        if (!level().isClientSide && wasPresent && isRemoved() && r.consumesAction()) {
            org.overengineer.bettacraft.progression.Progress.onBettaCaught(player);
        }
        return r;
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
            long now = server.getGameTime();
            byte flags = (byte) ((now < flareUntil ? FLAG_FLARE : 0) | (now < moodUntil ? FLAG_MOOD : 0));
            if (flags != entityData.get(DATA_FLAGS)) entityData.set(DATA_FLAGS, flags);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        var water = tankWater;
        if (water != null && !level().isClientSide && isInWater()) {
            // Keep our bettas inside the tank water (walls stop them anyway; this keeps them off the surface and, in a
            // free-shape tank, out of the notches).
            Vec3 p = position();
            double m = Math.max(0.05, 0.2 * lifeStage().scale());
            Vec3 c = water.clamp(p, m + 1.0 / 16, 1.0 / 16 + 0.05, 0.3 * lifeStage().scale());
            double cx = c.x, cy = c.y, cz = c.z;
            if (cx != p.x || cy != p.y || cz != p.z) {
                setDeltaMovement(getDeltaMovement().scale(0.5).add((cx - p.x) * 0.05, (cy - p.y) * 0.05, (cz - p.z) * 0.05));
            }
            // inside interior decoration (rock, castle ...): swim up and out, and drop a path that leads through it
            if (tickCount % 5 == 0 && level() instanceof ServerLevel server && insideDecor(server, p)) {
                setDeltaMovement(getDeltaMovement().scale(0.3).add(0, 0.04, 0));
                getNavigation().stop();
            }
        }
    }

    private boolean insideDecor(ServerLevel server, Vec3 p) {
        var c = org.overengineer.bettacraft.tank.TankInhabitants.resolveController(server, this);
        return c != null && org.overengineer.bettacraft.tank.interior.InteriorObstacles.blocked(server.getServer(), c.getTank(), p);
    }

    /** True if the straight path from {@code a} to {@code b} does not cross solid interior decoration (samples every 1/8 block). */
    boolean pathFreeOfDecor(Vec3 a, Vec3 b) {
        if (!(level() instanceof ServerLevel server)) return true;
        var c = org.overengineer.bettacraft.tank.TankInhabitants.resolveController(server, this);
        if (c == null || c.getTank().regionId() < 0) return true;
        double len = a.distanceTo(b);
        int steps = Math.max(1, (int) Math.ceil(len * 8));
        for (int i = 1; i <= steps; i++) {
            if (org.overengineer.bettacraft.tank.interior.InteriorObstacles.blocked(server.getServer(), c.getTank(), a.lerp(b, i / (double) steps))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel server && !isRemoved() && !dead) {
            BettaCare.onDeath(server, this, source);
        }
        super.die(source);
    }

    @Override
    protected boolean shouldDropLoot() {
        return tankWater == null && super.shouldDropLoot(); // tank deaths: no drops (PLAN 5.5)
    }

    @Override
    public boolean shouldDropExperience() {
        return tankWater == null && super.shouldDropExperience();
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
