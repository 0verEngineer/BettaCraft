package org.overengineer.bettacraft.terrarium;

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
 */
public abstract class TerrariumAnimalEntity extends Animal implements TerrariumAnimal {
    private static final EntityDataAccessor<Boolean> DATA_BASKING = SynchedEntityData.defineId(TerrariumAnimalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IN_TANK = SynchedEntityData.defineId(TerrariumAnimalEntity.class, EntityDataSerializers.BOOLEAN);
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
    }

    @Override
    protected void registerGoals() {
        // only outside a tank (in the tank the walker moves it)
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return !inTank() && super.canUse();
            }
        });
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
        return level().isClientSide ? entityData.get(DATA_IN_TANK) : tank != null;
    }

    /** On a basking spot now (synced, for the renderer: head up, legs out). */
    public boolean isBasking() {
        return entityData.get(DATA_BASKING);
    }

    public LandWalker walker() {
        return walker;
    }

    public float stress() {
        return stress;
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
        if (tank != null && daysSinceFed() > starveDays() && tickCount % 600 == 0) hurt(damageSources().starve(), 1.0F);
        boolean bask = tank != null && walker.basking();
        if (entityData.get(DATA_BASKING) != bask) entityData.set(DATA_BASKING, bask);
    }

    @Override
    protected void customServerAiStep() {
        if (tank != null && level() instanceof ServerLevel server) {
            walker.tick(this, server, tank, gait(), cold);
            return;
        }
        super.customServerAiStep();
    }

    @Override
    public void travel(Vec3 input) {
        if (tank != null || entityData.get(DATA_IN_TANK)) {
            setDeltaMovement(Vec3.ZERO);
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
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return !inTank() && super.causeFallDamage(distance, multiplier, source);
    }

    // ------------------------------------------------------------------ care

    @Override
    public void onTerrariumCare(ServerLevel level, TankBlockEntity c, TerrariumNeeds.Assessment a, double days) {
        stress = (float) a.stress();
        harmDays = a.harmful() ? harmDays + days : Math.max(0, harmDays - days);
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
    protected boolean shouldDropLoot() {
        return tank == null && super.shouldDropLoot();
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

    // ------------------------------------------------------------------ save

    protected CompoundTag saveTerrarium() {
        CompoundTag t = new CompoundTag();
        t.putDouble("fed_days", daysSinceFed());
        t.putDouble("harm", harmDays);
        return t;
    }

    protected void loadTerrarium(CompoundTag t) {
        // time in a bucket or an unloaded chunk stands still
        lastFed = t.contains("fed_days") ? level().getGameTime() - Math.round(t.getDouble("fed_days") * TICKS_PER_DAY) : NEVER;
        harmDays = t.getDouble("harm");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("terrarium", saveTerrarium());
        if (wild) tag.putBoolean("wild", true);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        loadTerrarium(tag.getCompound("terrarium"));
        wild = tag.getBoolean("wild");
    }

    /** GameTests: when it last ate. */
    public void setLastFedForTest(long time) {
        lastFed = time;
    }
}
