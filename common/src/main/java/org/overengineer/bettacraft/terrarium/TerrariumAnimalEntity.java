package org.overengineer.bettacraft.terrarium;

import org.overengineer.bettacraft.util.NbtCompat;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.Bucketable;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.food.BettaFood;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;

/**
 * M31: the base of our terrarium animals (the musk turtle; the turtles, frogs, geckos and snakes of the later parts extend
 * it). In a tank it moves with the {@link LandWalker} (no gravity, no vanilla AI: on the miniature's land, in its water,
 * to the basking spot), breathes there (the air above the water is abstracted), eats what {@link #eatsFood} says when the
 * tank is fed, and takes its {@link TerrariumNeeds} from the care step: stress from every problem, health loss from a
 * harmful one after {@code terrarium.harmAfterDays}, from hunger after {@link #starveDays}. Outside a tank it is a plain
 * animal with a few vanilla goals (walk around, look around). Subclasses give the species: needs, gait, diet, size.
 * <p>
 * M54: an animal with {@link Habits} also shows what it does while it sits still ({@link #action}, synced: a snake raises its
 * head or lies coiled, a lizard looks around ...) and, outside a tank, keeps its hours there too: it rests (under cover when it
 * hides by its habits) while it is not active, basks in the sun, walks at a calm pace with short sprints and runs from a player
 * that comes close ({@link HerpWorldGoals}).
 */
public abstract class TerrariumAnimalEntity extends Animal implements TerrariumAnimal {
    private static final EntityDataAccessor<Boolean> DATA_BASKING = SynchedEntityData.defineId(TerrariumAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IN_TANK = SynchedEntityData.defineId(TerrariumAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    /** M31 part c: the walker's {@link LandWalker.Pose} (buried, clinging to a wall). */
    private static final EntityDataAccessor<Byte> DATA_POSE = SynchedEntityData.defineId(TerrariumAnimalEntity.class, EntityDataSerializers.BYTE);
    /** M54: what it does while it sits still ({@link Habits.Action}). */
    private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(TerrariumAnimalEntity.class, EntityDataSerializers.BYTE);
    private static final double TICKS_PER_DAY = 24000.0;
    private static final long NEVER = Long.MIN_VALUE;

    protected final LandWalker walker = new LandWalker();
    @Nullable
    private TankData tank;
    private long lastFed = NEVER;
    private double harmDays;
    private float stress;
    private boolean cold;
    /** M22.3 style: spawned wild in the world (despawns far from players). */
    protected boolean wild;

    protected TerrariumAnimalEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    /** How this species moves in a tank. */
    public abstract LandWalker.Gait gait();

    /** Days without food after which it starves. */
    public double starveDays() {
        return 6.0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(DATA_BASKING, false);
        b.define(DATA_IN_TANK, false);
        b.define(DATA_POSE, (byte) 0);
        b.define(DATA_ACTION, (byte) 0);
    }

    /** M54: the habits of the animal (null: none, or the switch off). */
    @Nullable
    public Habits habits() {
        return null;
    }

    /** M54: what it does now while it sits still (synced). */
    public Habits.Action action() {
        return actionOverride != null ? actionOverride : Habits.Action.of(entityData.get(DATA_ACTION));
    }

    /** M54 (client): ticks since the action began (the models ease it in). */
    public float actionTicks(float partialTick) {
        return tickCount - actionSince + partialTick;
    }

    private int actionSince;
    @Nullable
    private Habits.Action actionOverride;
    /** M54 (client): the action before the current one (eased out while the new one eases in). */
    private Habits.Action actionBefore = Habits.Action.NONE, actionSeen = Habits.Action.NONE;

    /**
     * M54 (client): how much of an action the models show now, 0..1: the current one eases in over {@link #EASE} ticks, the one
     * before eases out.
     */
    public float actionWeight(Habits.Action a, float ageInTicks) {
        float t = Math.max(0, ageInTicks - actionSince);
        float in = Math.min(1F, t / EASE);
        in = in * in * (3 - 2 * in);
        if (action() == a) return in;
        return actionBefore == a ? 1F - in : 0F;
    }

    /** M54: ticks to ease an action in or out. */
    public static final float EASE = 12F;

    /** M54: the synced action (without the override of the smoke scenes). */
    protected Habits.Action syncedAction() {
        return Habits.Action.of(entityData.get(DATA_ACTION));
    }

    /** M54 (server): starts an action (NONE ends it). */
    protected void setAction(Habits.Action a) {
        if (entityData.get(DATA_ACTION) != (byte) a.ordinal()) entityData.set(DATA_ACTION, (byte) a.ordinal());
    }

    /** M54 smoke scenes: shows this action whatever the animal does; null = its own again (client and server). */
    public void setActionForTest(@Nullable Habits.Action a) {
        actionBefore = actionOverride != null ? actionOverride : actionSeen;
        actionOverride = a;
        actionSince = tickCount;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_ACTION.equals(key)) {
            Habits.Action now = Habits.Action.of(entityData.get(DATA_ACTION));
            if (now != actionSeen) {
                actionBefore = actionSeen;
                actionSeen = now;
                actionSince = tickCount;
            }
        }
    }

    /** M54: basking in the sun outside a tank (the world goal). */
    boolean worldBasking;
    /** M54: resting outside a tank by its hours (the world goal). */
    boolean worldResting;

    /** M54 (GameTests): resting outside a tank by its hours now. */
    public boolean restingInWorld() {
        return worldResting;
    }

    @Override
    protected void registerGoals() {
        // only outside a tank (in the tank the walker moves it); M54: the habits of the species in the world
        HerpWorldGoals.register(this, goalSelector);
        goalSelector.addGoal(5, new HerpWorldGoals.Stroll(this));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 4.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !inTank() && super.canUse();
            }
        });
    }

    /** Whether it lives in a tank now (server: its tank found; client: synced). */
    public boolean inTank() {
        return level().isClientSide() ? entityData.get(DATA_IN_TANK) : tank != null;
    }

    /** On a basking spot now (synced, for the renderer: head up, legs out). */
    public boolean isBasking() {
        return entityData.get(DATA_BASKING);
    }

    public LandWalker walker() {
        return walker;
    }

    /** M31 part c: what it shows besides walking (synced): buried in the ground, clinging to a wall of the tank. */
    public LandWalker.Pose herpPose() {
        return LandWalker.Pose.of(entityData.get(DATA_POSE));
    }

    public float stress() {
        return stress;
    }

    /** M31 part b: spawned wild by the spawn pass and still free (despawns far from players). */
    public boolean isWild() {
        return wild && !TankInhabitants.hasTankRef(this) && !hasCustomName();
    }

    /** M31 part b: the tank it lives in (server; null outside a tank). */
    @Nullable
    protected TankData tankData() {
        return tank;
    }

    /** M31 part b: whether it ate within this many days. */
    public boolean fedWithin(double days) {
        return lastFed != NEVER && daysSinceFed() <= days;
    }

    /** M31 part b: the hunger stands still for this many days (brumation). */
    protected void pauseHunger(double days) {
        if (lastFed != NEVER) lastFed += Math.round(days * TICKS_PER_DAY);
    }

    /** M31 part b: whether it rests and should not move (brumation); the walker then holds still. */
    protected boolean resting() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server && isAlive()) serverTick(server);
    }

    private void serverTick(ServerLevel server) {
        if (tickCount % 20 == 0 || tank == null && tickCount < 3) {
            TankBlockEntity c = TankInhabitants.resolveController(server, this);
            tank = c == null ? null : c.getTank();
            if (tank != null && lastFed == NEVER) lastFed = server.getGameTime();
            if (entityData.get(DATA_IN_TANK) != (tank != null)) entityData.set(DATA_IN_TANK, tank != null);
            setNoGravity(tank != null);
            if (tank != null) {
                TerrariumNeeds n = terrariumNeeds();
                cold = tank.hasClimate() && n.air().below(tank.airTemp());
            }
        }
        if (tank != null && !org.overengineer.bettacraft.config.Care.needs()) lastFed = server.getGameTime(); // M33 part c, DECOR: always fed
        if (tank != null && !resting() && daysSinceFed() > starveDays() && tickCount % 600 == 0) hurt(damageSources().starve(), 1.0F);
        boolean bask = baskOverride >= 0 ? baskOverride == 1 : tank != null ? walker.basking() : worldBasking;
        if (entityData.get(DATA_BASKING) != bask) entityData.set(DATA_BASKING, bask);
        LandWalker.Pose p = poseOverride != null ? poseOverride : tank != null && !resting() ? walker.pose() : LandWalker.Pose.NONE;
        if (entityData.get(DATA_POSE) != (byte) p.ordinal()) entityData.set(DATA_POSE, (byte) p.ordinal());
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        if (tank != null && level() instanceof ServerLevel server) {
            if (resting()) walker.hold();
            else walker.tick(this, server, tank, gait(), cold, habits());
            return;
        }
        super.customServerAiStep(serverLevel);
    }

    @Override
    public void travel(Vec3 input) {
        if (tank != null || entityData.get(DATA_IN_TANK)) {
            setDeltaMovement(Vec3.ZERO);
            // M44: the legs still move with the walker's steps (the client animates them; up a wall the climb counts too)
            if (level().isClientSide()) calculateEntityAnimation(herpPose().wall() != null);
            return;
        }
        super.travel(input);
    }

    @Override
    public boolean isPushable() {
        return !inTank() && super.isPushable();
    }

    @Override
    protected void doPush(Entity other) {
        if (!inTank()) super.doPush(other);
    }

    /** In a tank the air above the water is abstracted: it does not drown there. */
    @Override
    protected int decreaseAirSupply(int air) {
        return inTank() ? air : super.decreaseAirSupply(air);
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return !inTank() && super.causeFallDamage(distance, multiplier, source);
    }

    // ------------------------------------------------------------------ care

    @Override
    public void onTerrariumCare(ServerLevel level, TankBlockEntity c, TerrariumNeeds.Assessment a, double days) {
        stress = (float) a.stress();
        harmDays = a.harmful() && org.overengineer.bettacraft.config.Care.needs() ? harmDays + days : Math.max(0, harmDays - days); // M33 part c: DECOR
        double after = ServerConfig.getOr(ServerConfig.TERRA_HARM_DAYS, 1.0);
        if (harmDays >= after) {
            harmDays -= Math.max(0.05, after);
            hurt(damageSources().generic(), 1.0F);
        }
        // no trouble: it heals slowly
        if (a.fine() && getHealth() < getMaxHealth() && level.getGameTime() % 200 == 0) heal(1.0F);
    }

    @Override
    public double daysSinceFed() {
        if (lastFed == NEVER) return 0;
        return Math.max(0, (level().getGameTime() - lastFed) / TICKS_PER_DAY);
    }

    @Override
    public void feed() {
        lastFed = level().getGameTime();
    }

    /** Satiation 0..1 for the menu (1 = just fed). */
    public float satiation() {
        return (float) Mth.clamp(1 - daysSinceFed() / starveDays(), 0, 1);
    }

    @Override
    public boolean eatsFood(BettaFood.Kind kind) {
        return false;
    }

    // ------------------------------------------------------------------ vanilla breeding: not here (parts b-d breed in the tank)

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return wild && !hasCustomName() && !TankInhabitants.hasTankRef(this);
    }

    @Override
    protected boolean shouldDropLoot(ServerLevel serverLevel) {
        return tank == null && super.shouldDropLoot(serverLevel);
    }

    @Override
    public boolean shouldDropExperience() {
        return tank == null && super.shouldDropExperience();
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel server && !isRemoved() && !dead) {
            TankBlockEntity c = TankInhabitants.resolveController(server, this);
            if (c != null) TankCare.log(server, c, "log.bettacraft.terrarium.died", TankCare.nameArg(this));
        }
        super.die(source);
    }

    // ------------------------------------------------------------------ catching (part c: pulled up from the turtles)

    /** Whether it travels in a terrarium box (a land animal) instead of a bucket of water. */
    protected boolean usesBox() {
        return false;
    }

    /**
     * Catching: a land animal with the empty {@link org.overengineer.bettacraft.registry.ModItems#TERRARIUM_BOX}, a water
     * animal with a bucket of water (the vanilla way); either leaves its tank. Only for the animals that are {@link Bucketable}.
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!(this instanceof Bucketable b)) return super.mobInteract(player, hand);
        if (!usesBox()) return Bucketable.bucketMobPickup(player, hand, (TerrariumAnimalEntity & Bucketable) this).orElse(super.mobInteract(player, hand));
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(org.overengineer.bettacraft.registry.ModItems.TERRARIUM_BOX.get()) || !isAlive()) return super.mobInteract(player, hand);
        playSound(b.getPickupSound(), 1.0F, 1.0F);
        if (!level().isClientSide()) {
            TankInhabitants.release(this);
            ItemStack filled = b.getBucketItemStack();
            b.saveToBucketTag(filled);
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, filled, false));
            level().playSound(null, blockPosition(), b.getPickupSound(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            discard();
        }
        return InteractionResult.SUCCESS;
    }

    // ------------------------------------------------------------------ save

    protected CompoundTag saveTerrarium() {
        CompoundTag t = new CompoundTag();
        t.putDouble("fed_days", daysSinceFed());
        t.putDouble("harm", harmDays);
        return t;
    }

    protected void loadTerrarium(CompoundTag t) {
        // time in a bucket or an unloaded chunk stands still
        lastFed = t.contains("fed_days") ? level().getGameTime() - Math.round(t.getDoubleOr("fed_days", 0d) * TICKS_PER_DAY) : NEVER;
        harmDays = t.getDoubleOr("harm", 0d);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        CompoundTag tag = new CompoundTag();
        super.addAdditionalSaveData(output);
        tag.put("terrarium", saveTerrarium());
        if (wild) tag.putBoolean("wild", true);
        NbtCompat.write(output, tag);
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        CompoundTag tag = NbtCompat.read(input);

        super.readAdditionalSaveData(input);
        loadTerrarium(tag.getCompoundOrEmpty("terrarium"));
        wild = tag.getBooleanOr("wild", false);
    }

    private int baskOverride = -1;
    @Nullable
    private LandWalker.Pose poseOverride;

    /** Smoke scenes: shows this pose whatever the walker does; null = the walker again. */
    public void setPoseForTest(@Nullable LandWalker.Pose p) {
        poseOverride = p;
    }

    /** Smoke scenes: shows it basking (true / false) whatever the walker does; null = the walker again. */
    public void setBaskingForTest(@Nullable Boolean b) {
        baskOverride = b == null ? -1 : b ? 1 : 0;
    }

    /** GameTests: when it last ate. */
    public void setLastFedForTest(long time) {
        lastFed = time;
    }
}
