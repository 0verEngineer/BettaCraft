package org.overengineer.bettacraft.entity.frog;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankWaterVolume;

/**
 * African dwarf clawed frog (Hymenochirus, M14): a real entity like the bettas, a few per tank. It walks and paddles
 * along the bottom and regularly swims up to breathe at the surface; that needs air above the water (the air gap below
 * the glass, {@code tank.waterHeadroom} > 0, or a tank that is not filled to the top). Without air it slowly drowns. It
 * eats live and frozen food (mosquito larvae, artemia) and sinking pellets, but slowly: bettas snatch its food away
 * (config {@code frogs.frogFoodShareWithBettas}); sneak + right-click with food feeds the frogs directly.
 * <p>
 * M47 part c: frogs have a sex (frogs of older worlds get one from their UUID, then it is saved) and breed
 * ({@link FrogBreeding}): in amplexus the male holds on to the back of the female ({@link #clasp}) while she swims up to the
 * surface, where she releases her spawn. Tadpoles become froglets ({@link #makeFroglet}) that are drawn smaller and grow up
 * in {@code frogs.frogletDays}.
 */
public class DwarfFrogEntity extends AbstractFish {
    private static final EntityDataAccessor<Boolean> DATA_BREATHING = SynchedEntityData.defineId(DwarfFrogEntity.class, EntityDataSerializers.BOOLEAN);
    /** M47: the entity id of the frog this one clasps in amplexus (-1: none) and the drawn size (froglets are smaller). */
    private static final EntityDataAccessor<Integer> DATA_CLASPS = SynchedEntityData.defineId(DwarfFrogEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(DwarfFrogEntity.class, EntityDataSerializers.FLOAT);
    /** M47: how long the amplexus lasts until the female releases her eggs at the surface (ticks). */
    public static final int AMPLEXUS_TICKS = 400;
    /** M47: a froglet is drawn at this share of the adult size and grows to 1. */
    public static final float FROGLET_SIZE = 0.5F;
    private static final double TICKS_PER_DAY = 24000.0;
    /** Days without food after which a frog starts to starve. */
    public static final double STARVE_DAYS = 4.0;

    /** M17: frogs.frogStarveDays (default {@link #STARVE_DAYS}). */
    public static double starveDays() {
        return org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.FROGS_STARVE_DAYS, STARVE_DAYS);
    }

    /** Ticks of air left (in a tank; a wild frog never runs out). */
    int breath = -1;
    /** Game time of the last meal ({@link #NEVER}: counts as just fed when it comes into a tank). */
    long lastFed = NEVER;
    private static final long NEVER = Long.MIN_VALUE;
    /** Swimming up to breathe / holding at the surface. */
    boolean surfacing;
    int atSurface;
    /** Wandering target on the bottom. */
    @Nullable
    Vec3 target;
    int targetTicks;
    @Nullable
    TankWaterVolume tankWater;
    /** Whether the tank has air above the water (server, refreshed every second). */
    boolean airAvailable = true;
    long noAirSince = -1;
    /** Ticks until the next drowning damage. */
    int drown;
    /** M21: the tank (server, refreshed with {@link #tankWater}) and the way around the decoration. */
    @Nullable
    TankData tank;
    final org.overengineer.bettacraft.tank.interior.PathFollower path = new org.overengineer.bettacraft.tank.interior.PathFollower();
    /** M47: the sex (null: from the UUID, frogs of older worlds), days since the metamorphosis (-1: grown), days of rest after spawning. */
    @Nullable
    private Boolean female;
    private double youngDays = -1;
    private double rest;
    /** M47: amplexus: the female the male clasps (server), ticks since it began; the female knows her male. */
    @Nullable
    private DwarfFrogEntity clasped, claspedBy;
    private int amplexus;

    public DwarfFrogEntity(EntityType<? extends DwarfFrogEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FrogMoveControl(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.55);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BREATHING, false);
        builder.define(DATA_CLASPS, -1);
        builder.define(DATA_SIZE, 1F);
    }

    @Override
    protected void registerGoals() {
        // no vanilla fish goals: the frog's behaviour is in customServerAiStep
    }

    public boolean isBreathing() {
        return entityData.get(DATA_BREATHING);
    }

    // ------------------------------------------------------------------ M47: sex, age, amplexus

    public boolean isFemale() {
        return female != null ? female : (getUUID().getLeastSignificantBits() & 1L) == 1L;
    }

    public void setFemale(boolean f) {
        female = f;
    }

    /** Grown up (froglets are not). */
    public boolean isAdult() {
        return youngDays < 0;
    }

    /** A froglet that just left its tadpole life. */
    public void makeFroglet() {
        youngDays = 0;
        entityData.set(DATA_SIZE, FROGLET_SIZE);
    }

    /** Days since the metamorphosis (-1: grown). */
    public double youngDays() {
        return youngDays;
    }

    /** Client and server: the drawn size (1 = grown). */
    public float sizeFactor() {
        return entityData.get(DATA_SIZE);
    }

    public double restDays() {
        return rest;
    }

    public void rest(double days) {
        rest = Math.max(rest, days);
    }

    /** GameTests: grown up, rested. */
    public void makeReadyForTest() {
        youngDays = -1;
        rest = 0;
        entityData.set(DATA_SIZE, 1F);
    }

    /** In amplexus (the clasping male or the clasped female). */
    public boolean inAmplexus() {
        return clasped != null || claspedBy != null || entityData.get(DATA_CLASPS) >= 0;
    }

    /** Client: the frog this male clasps (null: none). */
    @Nullable
    public DwarfFrogEntity claspedForRender() {
        int id = entityData.get(DATA_CLASPS);
        return id >= 0 && level().getEntity(id) instanceof DwarfFrogEntity f ? f : null;
    }

    /** The male clasps the female from behind (amplexus, {@link FrogBreeding}). */
    public void clasp(DwarfFrogEntity f) {
        clasped = f;
        f.claspedBy = this;
        amplexus = 0;
        entityData.set(DATA_CLASPS, f.getId());
        f.surfacing = false;
        f.target = null;
    }

    private void release() {
        if (clasped != null) clasped.claspedBy = null;
        clasped = null;
        amplexus = 0;
        entityData.set(DATA_CLASPS, -1);
    }

    /** Ticks since the amplexus began (the male). */
    public int amplexusTicks() {
        return amplexus;
    }

    /** Maximum air in ticks (config frogs.frogAirMinutes). */
    public static int maxBreath() {
        return (int) Math.round(ServerConfig.getOr(ServerConfig.FROGS_AIR_MINUTES, 2.0) * 60 * 20);
    }

    public int breath() {
        return breath < 0 ? maxBreath() : breath;
    }

    public boolean inTank() {
        return tankWater != null;
    }

    public boolean airAvailable() {
        return airAvailable;
    }

    /** Days since the last meal (server). */
    public double daysSinceFed() {
        if (lastFed == NEVER) return 0;
        return Math.max(0, (level().getGameTime() - lastFed) / TICKS_PER_DAY);
    }

    public void feed() {
        lastFed = level().getGameTime();
    }

    /** Smoke tests: air left (a low value sends it up to breathe). */
    public void setBreathForTest(int ticks) {
        breath = ticks;
        surfacing = ticks < maxBreath() / 4;
    }

    /** GameTests: when the frog last ate. */
    public void setLastFedForTest(long time) {
        lastFed = time;
    }

    /** Satiation 0..1 for the menu (1 = just fed, 0 = starving). */
    public float satiation() {
        return (float) Mth.clamp(1 - daysSinceFed() / starveDays(), 0, 1);
    }

    /** M21.5: the smoothed body yaw on the client (no 1.4° steps of the synced yaw). */
    public final org.overengineer.bettacraft.entity.SwimPose swimPose = new org.overengineer.bettacraft.entity.SwimPose();

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server && isAlive()) serverTick(server);
        if (level().isClientSide) {
            swimPose.tick(this, 0.5F, 0F, 0F, false);
            if (Features.enabled(Feature.SMOOTH_SWIMMING)) {
                yBodyRot = swimPose.yaw();
                yHeadRot = yBodyRot;
            }
        }
    }

    private void serverTick(ServerLevel server) {
        // M47: the froglet grows, the rest after spawning passes (in a tank or not)
        if (tickCount % 20 == 0) {
            double d = 20 / TICKS_PER_DAY;
            if (rest > 0) rest = Math.max(0, rest - d);
            if (youngDays >= 0) {
                youngDays += d;
                if (youngDays >= FrogBreeding.frogletDays()) youngDays = -1;
                float size = youngDays < 0 ? 1F : (float) (FROGLET_SIZE + (1 - FROGLET_SIZE) * youngDays / FrogBreeding.frogletDays());
                if (Math.abs(size - entityData.get(DATA_SIZE)) > 0.01F) entityData.set(DATA_SIZE, size);
            }
        }
        if (claspedBy != null && (!claspedBy.isAlive() || claspedBy.clasped != this)) claspedBy = null;
        if (tickCount % 20 == 0 || tankWater == null && tickCount < 3) {
            TankBlockEntity c = TankInhabitants.resolveController(server, this);
            if (c == null) {
                tankWater = null;
                tank = null;
            } else {
                TankData t = c.getTank();
                tank = t;
                tankWater = new TankWaterVolume(t);
                airAvailable = org.overengineer.bettacraft.tank.TankFrogs.hasAir(t);
                if (lastFed == NEVER) lastFed = server.getGameTime();
            }
        }
        if (tankWater == null) {
            breath = -1;
            surfacing = false;
            setBreathing(false);
            if (clasped != null) release();
            return;
        }
        if (clasped != null) amplexusStep(server);
        // M33 part b: the frogs off: they rest (time stands still for their hunger, they hold their breath without drowning)
        if (!Features.enabled(Feature.FROGS)) {
            if (lastFed != NEVER) lastFed++;
            setBreathing(false);
            return;
        }
        // M33 part c, care.mode DECOR: always fed, never out of breath
        if (!org.overengineer.bettacraft.config.Care.needs()) {
            lastFed = server.getGameTime();
            breath = maxBreath();
            drown = 0;
            setBreathing(false);
            return;
        }
        if (breath < 0) breath = maxBreath();
        boolean up = surfacing && Math.abs(getY() + getBbHeight() - tankWater.surface()) < 0.12;
        if (up && airAvailable) {
            breath = Math.min(maxBreath(), breath + 60);
            atSurface++;
            noAirSince = -1;
            if (breath >= maxBreath() && atSurface > 30) {
                surfacing = false;
                atSurface = 0;
                target = null;
            }
        } else {
            breath = Math.max(0, breath - 1);
        }
        setBreathing(up && airAvailable);
        if (breath < maxBreath() / 4) surfacing = true;
        if (breath <= 0) {
            if (noAirSince < 0) noAirSince = server.getGameTime();
            if (--drown <= 0) {
                drown = 400;
                hurt(damageSources().drown(), 1.0F);
            }
        } else {
            drown = 0;
        }
        // starving
        if (daysSinceFed() > starveDays() && tickCount % 600 == 0) hurt(damageSources().starve(), 1.0F);
    }

    /** M47: the male rides on the female; at the end of the amplexus at the surface she lays her spawn there. */
    private void amplexusStep(ServerLevel server) {
        DwarfFrogEntity f = clasped;
        if (f == null) return;
        if (!f.isAlive() || f.tankWater == null || !isAlive() || !Features.enabled(Feature.FROGS)) {
            release();
            return;
        }
        amplexus++;
        // on her back, a little behind her head, looking the same way
        float yaw = f.getYRot();
        double back = 0.05 * f.sizeFactor();
        double x = f.getX() + Mth.sin(yaw * Mth.DEG_TO_RAD) * back, z = f.getZ() - Mth.cos(yaw * Mth.DEG_TO_RAD) * back;
        setPos(x, f.getY() + 0.06 * f.sizeFactor(), z);
        setDeltaMovement(f.getDeltaMovement());
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
        breath = Math.max(breath, f.breath);
        boolean atSurface = Math.abs(f.getY() + f.getBbHeight() - f.tankWater.surface()) < 0.2;
        if (amplexus >= AMPLEXUS_TICKS && (atSurface || amplexus >= AMPLEXUS_TICKS * 2)) {
            org.overengineer.bettacraft.tank.TankBlockEntity c = TankInhabitants.resolveController(server, this);
            if (c != null) FrogBreeding.lay(server, c, f, this);
            release();
        }
    }

    private void setBreathing(boolean b) {
        if (entityData.get(DATA_BREATHING) != b) entityData.set(DATA_BREATHING, b);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        TankWaterVolume w = tankWater;
        if (w == null || !isInWater()) {
            if (isInWater() && (target == null || --targetTicks <= 0)) {
                target = position().add(random.nextGaussian() * 2, -0.5, random.nextGaussian() * 2);
                targetTicks = 100;
                moveControl.setWantedPosition(target.x, target.y, target.z, 0.6);
            }
            return;
        }
        double floor = w.box().minY + 0.02;
        if (clasped != null) {
            moveControl.setWantedPosition(getX(), getY(), getZ(), 0); // M47: he holds on, she swims
            return;
        }
        if (claspedBy != null) {
            // M47: the female carries the male up to the surface and paddles in small circles there
            Vec3 p = position();
            double a = tickCount * 0.04;
            Vec3 up = w.clamp(new Vec3(p.x + Math.cos(a) * 0.15, w.surface() - getBbHeight() - 0.02, p.z + Math.sin(a) * 0.15), 0.2, 0.05, 0.0);
            moveControl.setWantedPosition(up.x, w.surface() - getBbHeight() - 0.03, up.z, 0.8);
            if (getY() + getBbHeight() < w.surface() - 0.15) setDeltaMovement(getDeltaMovement().add(0, 0.008, 0));
            return;
        }
        if (surfacing) {
            Vec3 p = position();
            Vec3 up = w.clamp(new Vec3(p.x, w.surface() - getBbHeight() + 0.02, p.z), 0.2, 0.05, 0.0);
            up = new Vec3(up.x, w.surface() - getBbHeight() + 0.03, up.z);
            // M21: from under an overhang or out of a cave around the decoration
            Vec3 n = steer(up, w);
            moveControl.setWantedPosition(n.x, n.y, n.z, 1.0);
            if (n == up) setDeltaMovement(getDeltaMovement().add(0, 0.012, 0));
            return;
        }
        if (target == null || --targetTicks <= 0 || position().distanceToSqr(target) < 0.02) {
            if (random.nextInt(4) == 0) {
                target = null; // rests on the bottom
                targetTicks = 40 + random.nextInt(80);
                moveControl.setWantedPosition(getX(), getY(), getZ(), 0);
                return;
            }
            target = null;
            for (int i = 0; i < 6 && target == null; i++) {
                Vec3 p = position().add(random.nextGaussian() * 0.8, 0, random.nextGaussian() * 0.8);
                Vec3 t = w.clamp(new Vec3(p.x, floor + 0.02, p.z), 0.2, 0.05, 0.2);
                // M21: never a spot inside the decoration
                if (i < 5 && tank != null && level() instanceof ServerLevel server
                        && org.overengineer.bettacraft.tank.interior.InteriorObstacles.blocked(server.getServer(), tank, t.add(0, 0.05, 0))) continue;
                target = t;
            }
            targetTicks = 60 + random.nextInt(80);
            path.clear();
        }
        if (target != null) {
            Vec3 n = steer(target, w);
            if (path.failed()) {
                target = null; // no way there: rest and pick another spot
                targetTicks = 10;
                return;
            }
            moveControl.setWantedPosition(n.x, n.y, n.z, 0.5);
        }
    }

    /** M21: the next point on the way to {@code goal} around the interior decoration (the goal itself without one). */
    private Vec3 steer(Vec3 goal, TankWaterVolume w) {
        if (tank == null || !(level() instanceof ServerLevel server)) return goal;
        return path.next(server.getServer(), tank, position(), goal, w.surface() + 0.1, 0.06, server.getGameTime(), 0.2, true);
    }

    /** GameTests: walk to this spot (for a long time). */
    public void goToForTest(Vec3 spot) {
        target = spot;
        targetTicks = 100000;
        path.clear();
    }

    /** GameTests: the waypoints left on the frog's way. */
    public int pathLeftForTest() {
        return path.remaining();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        TankWaterVolume w = tankWater;
        if (w != null && !level().isClientSide && isInWater() && clasped == null) {
            Vec3 p = position();
            Vec3 c = w.clamp(p, 0.16 + 1.0 / 16, 1.0 / 16 + 0.01, getBbHeight() - 0.04);
            if (c.x != p.x || c.y != p.y || c.z != p.z) {
                setDeltaMovement(getDeltaMovement().scale(0.5).add((c.x - p.x) * 0.05, (c.y - p.y) * 0.05, (c.z - p.z) * 0.05));
            }
        }
    }

    /** M22.3: spawned wild in the world (despawns far from players like vanilla fish). */
    private boolean wild;

    public boolean isWild() {
        return wild;
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
                                                                   net.minecraft.world.entity.MobSpawnType spawnType, @javax.annotation.Nullable net.minecraft.world.entity.SpawnGroupData groupData) {
        wild = spawnType == net.minecraft.world.entity.MobSpawnType.NATURAL || spawnType == net.minecraft.world.entity.MobSpawnType.CHUNK_GENERATION;
        return super.finalizeSpawn(level, difficulty, spawnType, groupData);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return wild && !fromBucket() && !hasCustomName() && !org.overengineer.bettacraft.tank.TankInhabitants.hasTankRef(this);
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
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel server && !isRemoved() && !dead) {
            org.overengineer.bettacraft.tank.TankFrogs.onDeath(server, this, source);
        }
        super.die(source);
    }

    // ------------------------------------------------------------------ save / bucket

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("frog", saveFrog());
        if (wild) tag.putBoolean("wild", true); // M22.3
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        loadFrog(tag.getCompound("frog"));
        wild = tag.getBoolean("wild");
    }

    private CompoundTag saveFrog() {
        CompoundTag t = new CompoundTag();
        t.putInt("breath", breath);
        t.putDouble("fed_days", daysSinceFed());
        t.putBoolean("female", isFemale()); // M47 (older frogs: from the UUID)
        if (youngDays >= 0) t.putDouble("young_days", youngDays);
        if (rest > 0) t.putDouble("rest", rest);
        return t;
    }

    private void loadFrog(CompoundTag t) {
        breath = t.contains("breath") ? t.getInt("breath") : -1;
        // time in a bag or an unloaded chunk stands still: the frog is as hungry as it was
        lastFed = t.contains("fed_days") ? level().getGameTime() - Math.round(t.getDouble("fed_days") * TICKS_PER_DAY) : NEVER;
        if (t.contains("female")) female = t.getBoolean("female"); // M47: older frogs keep the sex of their UUID
        youngDays = t.contains("young_days") ? Math.max(0, t.getDouble("young_days")) : -1;
        rest = Math.max(0, t.getDouble("rest"));
        entityData.set(DATA_SIZE, youngDays < 0 ? 1F : (float) (FROGLET_SIZE + (1 - FROGLET_SIZE) * Math.min(1, youngDays / FrogBreeding.frogletDays())));
    }

    @Override
    public void saveToBucketTag(ItemStack stack) {
        Bucketable.saveDefaultDataToBucketTag(this, stack);
        CompoundTag f = saveFrog();
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, stack, t -> t.put("frog", f));
    }

    @Override
    public void loadFromBucketTag(CompoundTag tag) {
        Bucketable.loadDefaultDataFromBucketTag(this, tag);
        wild = false;
        if (tag.contains("frog")) loadFrog(tag.getCompound("frog"));
    }

    @Override
    public void remove(RemovalReason reason) {
        if (clasped != null) release(); // M47
        if (claspedBy != null) claspedBy.release();
        super.remove(reason);
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(ModItems.FROG_BUCKET.get());
    }

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.TADPOLE_FLOP;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FROG_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.TADPOLE_HURT;
    }

    @Override
    protected boolean canRandomSwim() {
        return false;
    }

    /** Paddles straight towards the wanted position (no path finding: the tank is small and open). */
    static final class FrogMoveControl extends MoveControl {
        private final DwarfFrogEntity frog;

        FrogMoveControl(DwarfFrogEntity frog) {
            super(frog);
            this.frog = frog;
        }

        @Override
        public void tick() {
            if (operation != Operation.MOVE_TO || !frog.isInWater()) {
                frog.setSpeed(0);
                return;
            }
            double dx = wantedX - frog.getX(), dy = wantedY - frog.getY(), dz = wantedZ - frog.getZ();
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d < 0.04 || speedModifier <= 0) {
                frog.setSpeed(0);
                operation = Operation.WAIT;
                return;
            }
            float speed = (float) (speedModifier * frog.getAttributeValue(Attributes.MOVEMENT_SPEED));
            frog.setSpeed(Mth.lerp(0.125F, frog.getSpeed(), speed));
            frog.setDeltaMovement(frog.getDeltaMovement().add(0, frog.getSpeed() * (dy / d) * 0.08, 0));
            if (dx * dx + dz * dz > 1e-4) {
                float yaw = (float) (Mth.atan2(dz, dx) * 180.0F / Math.PI) - 90.0F;
                frog.setYRot(rotlerp(frog.getYRot(), yaw, 20.0F));
                frog.yBodyRot = frog.getYRot();
                frog.yHeadRot = frog.getYRot();
            }
        }
    }
}
