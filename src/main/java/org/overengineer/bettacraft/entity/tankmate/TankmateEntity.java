package org.overengineer.bettacraft.entity.tankmate;

import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.entity.betta.BettaBreeding;
import org.overengineer.bettacraft.entity.betta.BettaHealth;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M19: a tankmate fish (one entity type per {@link TankmateSpecies}). Like the betta it has health timers, grows through
 * the life stages and breeds, but only inside a Bettacraft tank; outside it just swims around. Inside a tank it keeps to
 * its zone of the water column, shoals with its kind, rests on the bottom or sticks to the glass (corydoras, plecos,
 * otocinclus), hides in caves and plants and avoids the interior decoration ({@link TankmateBehaviour}); the needs are in
 * {@link TankmateCare}, the tank-level breeding and predation in {@link org.overengineer.bettacraft.tankmates.TankmateBreeding}.
 */
public class TankmateEntity extends AbstractFish {
    /** Variety, sex and stage for the renderer (server -> client). */
    private static final EntityDataAccessor<CompoundTag> DATA_LOOK = SynchedEntityData.defineId(TankmateEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Byte> DATA_FLAGS = SynchedEntityData.defineId(TankmateEntity.class, EntityDataSerializers.BYTE);
    public static final int FLAG_SICK = 1, FLAG_GRAVID = 2, FLAG_GUARD = 4, FLAG_GLASS = 8, FLAG_REST = 16, FLAG_STRESS = 32, FLAG_HIDDEN = 64;
    public static final String KEY = "tankmate";
    static final double TICKS_PER_DAY = 24000.0;

    private final TankmateSpecies species;
    TankmateGenes genes;
    boolean female;
    LifeStage stage = LifeStage.ADULT;
    double stageDays;
    int generation;
    BettaHealth health = new BettaHealth();
    BettaBreeding breeding = new BettaBreeding();
    /** Livebearer females: days pregnant (-1 = not pregnant) and the father's genes. */
    double gravidDays = -1;
    @Nullable
    TankmateGenes gravidFather;
    long lastSimTime = -1;
    boolean simStarted;
    /** Stress reasons of the last care step (menu): bit 0 shoal too small, 1 harassed, 2 temperature, 3 water, 4 crowded. */
    int stress;
    /** Same species in the tank at the last care step (adults and subadults; menu). */
    int shoalSize;
    /** Flags from the care step (sick, gravid, guarding, stressed); the behaviour adds its own. */
    int careFlags;
    /** A male guarding a cave clutch or a foam nest (set by the tank's breeding step). */
    boolean guarding;

    // tank state (server, transient)
    @Nullable
    TankWaterVolume tankWater;
    final TankmateBehaviour behaviour = new TankmateBehaviour(this);
    @Nullable
    Vec3 foodTarget;
    long foodUntil;
    @Nullable
    Entity fleeFrom;
    long fleeUntil;
    /** Where a guarding male (cave, foam nest) or a female about to spawn stays. */
    @Nullable
    Vec3 holdAt;
    long holdUntil;
    double pendingDamage;
    @Nullable
    Entity pendingAttacker;

    // client cache
    private String lookVariety = "wild";
    private boolean lookFemale;
    private LifeStage lookStage = LifeStage.ADULT;
    /** M21.6: the guppy or endler genome the server sends while features.livebearerGenetics is on (null: the old varieties). */
    @Nullable
    private org.overengineer.bettacraft.genetics.livebearer.LivebearerGenome lookLb;

    public TankmateEntity(EntityType<? extends TankmateEntity> type, Level level) {
        super(type, level);
        TankmateSpecies s = TankmateSpecies.of(type);
        this.species = s == null ? TankmateSpecies.NEON_TETRA : s;
        this.genes = TankmateGenes.wild(species);
        this.lookVariety = species.variety(0);
        this.moveControl = new TankmateMoveControl(this);
    }

    public static AttributeSupplier.Builder createAttributes(TankmateSpecies s) {
        double speed = switch (s.model()) {
            case TETRA, RASBORA -> 0.75;
            case LIVEBEARER, PLATY -> 0.65;
            case CORY -> 0.5;
            case PLECO -> 0.35;
            case OTO -> 0.4;
            case KUHLI -> 0.45;
            case LOACH -> 0.6;
            case GOURAMI -> 0.45;
            case CLOWN -> 0.55; // M20
            case TANG -> 0.7;
            case GOBY -> 0.4;
            case DAMSEL -> 0.7;
            case SEAHORSE -> 0.22; // M21.8: slow, upright swimmers
        };
        double hp = s.length() >= 0.6 ? 6.0 : s.length() >= 0.35 ? 4.0 : 3.0;
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, hp).add(Attributes.MOVEMENT_SPEED, speed);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LOOK, new CompoundTag());
        builder.define(DATA_FLAGS, (byte) 0);
    }

    @Override
    protected void registerGoals() {
        // no vanilla goals: the behaviour knows the tank (TankmateBehaviour, run in customServerAiStep)
    }

    // ------------------------------------------------------------------ data

    public TankmateSpecies species() {
        return species;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData groupData) {
        if (spawnType != MobSpawnType.BUCKET) {
            female = random.nextBoolean();
            genes = TankmateGenes.wild(species, 0.08, random::nextLong);
            // spawn eggs and commands: a random bred variety now and then
            if (spawnType == MobSpawnType.SPAWN_EGG || spawnType == MobSpawnType.COMMAND) {
                if (TankmateGenes.livebearer(species) && org.overengineer.bettacraft.config.CommonConfig.livebearerGenetics()) {
                    // M21.6: a named strain of the species now and then
                    if (random.nextInt(3) == 0) {
                        var strains = org.overengineer.bettacraft.genetics.livebearer.LivebearerStrain.of(species == org.overengineer.bettacraft.tankmates.TankmateSpecies.ENDLER_GUPPY);
                        genes = TankmateGenes.strain(species, strains.get(random.nextInt(strains.size())));
                    }
                } else if (species.varieties().size() > 1 && random.nextInt(3) == 0) {
                    genes = TankmateGenes.pure(species, species.variety(1 + random.nextInt(species.varieties().size() - 1)));
                }
            }
            syncLook();
        }
        return super.finalizeSpawn(level, difficulty, spawnType, groupData);
    }

    /** Sets everything that makes up this fish (offspring, tests, catches). */
    public void setup(TankmateGenes genes, boolean female, LifeStage stage, int generation) {
        this.genes = genes;
        this.female = female;
        this.stage = stage.isEntity() ? stage : LifeStage.FRY;
        this.stageDays = 0;
        this.generation = generation;
        syncLook();
        refreshDimensions();
    }

    public TankmateGenes genes() {
        return genes;
    }

    public String variety() {
        return level().isClientSide ? lookVariety : genes.phenotype(species);
    }

    public boolean isFemale() {
        return level().isClientSide ? lookFemale : female;
    }

    /**
     * M21.6: the guppy or endler genome that decides the look (client: as sent by the server), or null when the species
     * has none or features.livebearerGenetics is off (then the old fixed varieties show).
     */
    @Nullable
    public org.overengineer.bettacraft.genetics.livebearer.LivebearerGenome livebearerGenome() {
        if (level().isClientSide) return lookLb;
        return org.overengineer.bettacraft.config.CommonConfig.livebearerGenetics() ? genes.lb() : null;
    }

    /** M21.6: what a guppy or endler shows (null: no genome, see {@link #livebearerGenome()}). */
    @Nullable
    public org.overengineer.bettacraft.genetics.livebearer.LbPhenotype livebearerPhenotype() {
        var g = livebearerGenome();
        return g == null ? null : g.express(isFemale());
    }

    public LifeStage lifeStage() {
        LifeStage s = level() != null && level().isClientSide ? lookStage : stage;
        return s == null ? LifeStage.ADULT : s;
    }

    public boolean isAdult() {
        return lifeStage() == LifeStage.ADULT;
    }

    @Override
    public boolean isBaby() {
        return !isAdult();
    }

    @Override
    public float getAgeScale() {
        return lifeStage().scale();
    }

    public int generation() {
        return generation;
    }

    public BettaHealth health() {
        return health;
    }

    public BettaBreeding breeding() {
        return breeding;
    }

    public double stageProgress() {
        double d = stageDuration(stage);
        return Double.isInfinite(d) || d <= 0 ? 1.0 : Mth.clamp(stageDays / d, 0, 1);
    }

    /** Duration of a life stage of this species (the bettas' stages times the species' growth). */
    public double stageDuration(LifeStage s) {
        return s.days() * species.growth();
    }

    public boolean isGravid() {
        return level().isClientSide ? (entityData.get(DATA_FLAGS) & FLAG_GRAVID) != 0 : gravidDays >= 0;
    }

    public double gravidDays() {
        return gravidDays;
    }

    @Nullable
    public TankmateGenes gravidFather() {
        return gravidFather;
    }

    public void setGravid(double days, @Nullable TankmateGenes father) {
        this.gravidDays = days;
        this.gravidFather = father;
    }

    public int stressBits() {
        return stress;
    }

    public int shoalSize() {
        return shoalSize;
    }

    public boolean flag(int f) {
        return (entityData.get(DATA_FLAGS) & f) != 0;
    }

    public boolean isSick() {
        return level().isClientSide ? flag(FLAG_SICK) : health.isSick();
    }

    /** Set by the tank step: size of the shoal (same species, juveniles and older) and whether it is too small. */
    public void setShoal(int n, boolean tooSmall) {
        this.shoalSize = n;
        stress = tooSmall ? stress | TankmateCare.STRESS_SHOAL : stress & ~TankmateCare.STRESS_SHOAL;
    }

    public void setChased(boolean chased) {
        stress = chased ? stress | TankmateCare.STRESS_CHASED : stress & ~TankmateCare.STRESS_CHASED;
    }

    /** The spawning cave of a pleco male (set by the tank step), or null. */
    @Nullable
    public Vec3 cave() {
        return cave;
    }

    public void setCave(@Nullable Vec3 cave, Vec3 dir) {
        this.cave = cave;
        this.caveDir = dir;
    }

    /** Out of the mouth of the cave (a pleco in it looks out). */
    public Vec3 caveDir() {
        return caveDir;
    }

    @Nullable
    private Vec3 cave;
    private Vec3 caveDir = Vec3.ZERO;

    public boolean isGuarding() {
        return level().isClientSide ? flag(FLAG_GUARD) : guarding;
    }

    public void setGuarding(boolean g) {
        this.guarding = g;
    }

    /** Distance the centre keeps from the glass: half the body (rendered length) or the hitbox, plus the glass. */
    public double sideMargin() {
        // M21.8: a seahorse stands upright (its length is its height; the snout and the curled tail reach a little forward)
        double half = Math.max(getBbWidth() * 0.5, Math.min(0.45, species.length() * lifeStage().scale() * (species.seahorse() ? 0.18 : 0.4)));
        return half + 1.0 / 16 + 0.005;
    }

    /** Smoke scenes: shows a pose (resting, on the glass) while the fish is frozen (its behaviour does not run then). */
    public void setPoseForSmoke(int behaviourFlags) {
        behaviour.behaviourFlags = behaviourFlags;
        setFlags(careFlags | behaviourFlags);
    }

    /** M21.5 GameTests: swim to the glass now (false if there is no free spot). */
    public boolean swimToGlassForTest() {
        return behaviour.glassNow();
    }

    /** M21.8: a seahorse that holds on to a hitching post (server). */
    public boolean isHitched() {
        return behaviour.hitched && behaviour.hitchPost != null;
    }

    /** M21.8: the hitching post a seahorse holds on to or swims to (server; null if none). */
    @Nullable
    public Vec3 hitchPost() {
        return behaviour.hitchPost;
    }

    /** M21.8 GameTests and smoke scenes: swim to the nearest hitching post now (false if the tank has none). */
    public boolean hitchNowForTest() {
        return behaviour.hitchNow();
    }

    /** M21.5 GameTests: the behaviour target (null if none). */
    @Nullable
    public Vec3 behaviourTarget() {
        return behaviour.target;
    }

    /** Behaviour tests and smoke scenes: stop and stay where it is. */
    public void freezeBehaviour() {
        behaviour.freeze();
    }

    public boolean isInTank() {
        return tankWater != null;
    }

    @Nullable
    public TankWaterVolume tankWater() {
        return tankWater;
    }

    public void setFoodTarget(Vec3 spot, long until) {
        this.foodTarget = spot;
        this.foodUntil = until;
    }

    public void flee(Entity from, long until) {
        this.fleeFrom = from;
        this.fleeUntil = until;
    }

    public void holdAt(Vec3 spot, long until) {
        this.holdAt = spot;
        this.holdUntil = until;
    }

    @Nullable
    public Vec3 holdSpot() {
        return holdAt != null && level().getGameTime() < holdUntil ? holdAt : null;
    }

    /** Damage from a chase or a fight, in whole points (fractions add up, like the bettas). */
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

    /** Moves to the next life stage (server). */
    void setStage(LifeStage s) {
        this.stage = s;
        this.stageDays = 0;
        syncLook();
        refreshDimensions();
    }

    void syncLook() {
        CompoundTag look = new CompoundTag();
        look.putString("variety", genes.phenotype(species));
        look.putBoolean("female", female);
        look.putString("stage", stage.id());
        if (genes.lb() != null && org.overengineer.bettacraft.config.CommonConfig.livebearerGenetics()) look.put("lb", genes.lb().save());
        entityData.set(DATA_LOOK, look);
    }

    void setFlags(int flags) {
        byte b = (byte) flags;
        if (entityData.get(DATA_FLAGS) != b) entityData.set(DATA_FLAGS, b);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_LOOK.equals(key)) {
            CompoundTag look = entityData.get(DATA_LOOK);
            lookVariety = look.contains("variety", Tag.TAG_STRING) ? look.getString("variety") : species.variety(0);
            lookFemale = look.getBoolean("female");
            lookStage = look.contains("stage", Tag.TAG_STRING) ? LifeStage.byId(look.getString("stage")) : LifeStage.ADULT;
            lookLb = look.contains("lb", Tag.TAG_COMPOUND)
                    ? org.overengineer.bettacraft.genetics.livebearer.LivebearerGenome.load(look.getCompound("lb"), species == org.overengineer.bettacraft.tankmates.TankmateSpecies.ENDLER_GUPPY)
                    : null;
            refreshDimensions();
        }
    }

    @Override
    public float getPickRadius() {
        return switch (lifeStage()) {
            case FRY, EGG, LARVA -> 0.15F;
            case JUVENILE -> 0.08F;
            default -> species.length() < 0.25 ? 0.06F : 0.0F;
        };
    }

    // ------------------------------------------------------------------ save / bucket

    public CompoundTag saveTankmate() {
        CompoundTag t = new CompoundTag();
        t.putString("species", species.id());
        t.put("genes", genes.save());
        t.putBoolean("female", female);
        t.putString("stage", stage.id());
        t.putDouble("stage_days", stageDays);
        t.putInt("gen", generation);
        t.put("health", health.save());
        t.put("breeding", breeding.save());
        t.putDouble("gravid", gravidDays);
        if (gravidFather != null) t.put("gravid_father", gravidFather.save());
        return t;
    }

    public void loadTankmate(CompoundTag t) {
        genes = TankmateGenes.load(t.getCompound("genes"), species);
        female = t.getBoolean("female");
        LifeStage s = t.contains("stage", Tag.TAG_STRING) ? LifeStage.byId(t.getString("stage")) : LifeStage.ADULT;
        stage = s.isEntity() ? s : LifeStage.FRY;
        double sd = t.getDouble("stage_days");
        stageDays = Double.isFinite(sd) ? Math.max(0, sd) : 0;
        generation = t.getInt("gen");
        health = BettaHealth.load(t.getCompound("health"));
        breeding = BettaBreeding.load(t.getCompound("breeding"));
        gravidDays = t.contains("gravid", Tag.TAG_ANY_NUMERIC) ? t.getDouble("gravid") : -1;
        if (!Double.isFinite(gravidDays)) gravidDays = -1;
        gravidFather = t.contains("gravid_father", Tag.TAG_COMPOUND) ? TankmateGenes.load(t.getCompound("gravid_father"), species) : null;
        if (gravidDays >= 0 && gravidFather == null) gravidFather = TankmateGenes.wild(species);
        syncLook();
        refreshDimensions();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put(KEY, saveTankmate());
        tag.putLong("last_sim", lastSimTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(KEY, Tag.TAG_COMPOUND)) loadTankmate(tag.getCompound(KEY));
        lastSimTime = tag.contains("last_sim") ? tag.getLong("last_sim") : -1;
        simStarted = false;
    }

    @Override
    public void saveToBucketTag(ItemStack stack) {
        Bucketable.saveDefaultDataToBucketTag(this, stack);
        CompoundTag d = saveTankmate();
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, stack, t -> t.put(KEY, d));
    }

    @Override
    public void loadFromBucketTag(CompoundTag tag) {
        Bucketable.loadDefaultDataFromBucketTag(this, tag);
        if (tag.contains(KEY, Tag.TAG_COMPOUND)) loadTankmate(tag.getCompound(KEY));
        lastSimTime = level().getGameTime(); // time in a bucket or bag stands still
    }

    @Override
    public ItemStack getBucketItemStack() {
        var bucket = ModItems.TANKMATE_BUCKETS.get(species);
        return bucket == null ? new ItemStack(ModItems.FISH_BAG.get()) : new ItemStack(bucket.get());
    }

    // ------------------------------------------------------------------ ticking

    /** M21.5: the smoothed swimming pose on the client (pitch, tail beat, body yaw, the turn onto the glass). */
    public final org.overengineer.bettacraft.entity.SwimPose swimPose = new org.overengineer.bettacraft.entity.SwimPose();

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server && isAlive()) TankmateCare.serverTick(server, this);
        if (level().isClientSide) {
            boolean eel = species.model() == TankmateSpecies.Model.KUHLI;
            if (species.seahorse()) {
                // M21.8: upright, never pitched; the small dorsal fin flutters fast all the time
                swimPose.tick(this, 0.9F, 0.6F, 0F, false);
            } else {
                // an eel-like kuhli loach undulates slowly; the other fish beat faster the faster they swim
                swimPose.tick(this, eel ? 0.16F : 0.45F, eel ? 0.32F : 1.1F, eel ? 12F : 35F, flag(FLAG_GLASS) && isInWater());
            }

            if (org.overengineer.bettacraft.config.CommonConfig.smoothSwimming()) {
                yBodyRot = swimPose.yaw();
                yHeadRot = yBodyRot;
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        behaviour.tick();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        TankWaterVolume w = tankWater;
        if (w != null && !level().isClientSide && isInWater()) {
            // keep the whole fish inside the water of the tank (not in the glass, not above the surface)
            Vec3 p = position();
            Vec3 c = w.clamp(p, sideMargin(), 1.0 / 16 + 0.005, Math.max(0.04, getBbHeight() * 0.8));
            if (c.x != p.x || c.y != p.y || c.z != p.z) {
                setPos(p.x + (c.x - p.x) * 0.5, p.y + (c.y - p.y) * 0.5, p.z + (c.z - p.z) * 0.5);
                Vec3 dm = getDeltaMovement();
                setDeltaMovement((c.x - p.x) * dm.x < 0 ? 0 : dm.x, (c.y - p.y) * dm.y < 0 ? 0 : dm.y, (c.z - p.z) * dm.z < 0 ? 0 : dm.z);
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel server && !isRemoved() && !dead) TankmateCare.onDeath(server, this, source);
        super.die(source);
    }

    @Override
    protected boolean shouldDropLoot() {
        return tankWater == null && super.shouldDropLoot();
    }

    @Override
    public boolean shouldDropExperience() {
        return tankWater == null && super.shouldDropExperience();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean canRandomSwim() {
        return false;
    }

    // ------------------------------------------------------------------ sounds

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.TROPICAL_FISH_FLOP;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.TROPICAL_FISH_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.TROPICAL_FISH_HURT;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.TROPICAL_FISH_AMBIENT;
    }
}
