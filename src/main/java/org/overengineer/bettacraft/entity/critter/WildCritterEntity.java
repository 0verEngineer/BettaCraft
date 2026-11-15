package org.overengineer.bettacraft.entity.critter;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.containers.AnimalContainers;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterLooks;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.world.Wildlife;

/**
 * M22.3: a shrimp, crayfish, snail or hermit crab living in the world: one entity per animal, crawling slowly over the
 * bottom of rivers, swamps, mangroves, beaches, jungle water and warm oceans. It carries the complete data of a tank animal
 * ({@link Critter#saveAnimal}: species, sex, genes, colour density, age, look, the magnifier's analysis), so catching it
 * (aquarium net, fish bag, a bucket of water that suits it) and letting it go again loses nothing. Spawned wild animals
 * despawn far from players like vanilla fish; ones a player let go stay.
 */
public class WildCritterEntity extends WaterAnimal {
    private static final EntityDataAccessor<Integer> DATA_SPECIES = SynchedEntityData.defineId(WildCritterEntity.class, EntityDataSerializers.INT);
    /** Colour index ({@link CritterGenes#color}) in the low byte, grade * 255 in the next, flags above. */
    private static final EntityDataAccessor<Integer> DATA_LOOK = SynchedEntityData.defineId(WildCritterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SHELL = SynchedEntityData.defineId(WildCritterEntity.class, EntityDataSerializers.INT);

    private Critter animal;
    private boolean wild;
    @Nullable
    private Vec3 target;
    private int idle;
    /** Client: the animal as drawn (colour, size, look). */
    @Nullable
    private Critter drawn;

    public WildCritterEntity(EntityType<? extends WildCritterEntity> type, Level level) {
        super(type, level);
        this.animal = new Critter(0, CritterSpecies.NEOCARIDINA, false, CritterGenes.WILD, 0, 0, 0);
        this.animal.age = CritterSpecies.NEOCARIDINA.juvenileDays() + 1;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 2.0).add(Attributes.MOVEMENT_SPEED, 0.1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(DATA_SPECIES, 0);
        b.define(DATA_LOOK, 0xFF);
        b.define(DATA_SHELL, 0);
    }

    @Override
    protected void registerGoals() {
        // no goals: it crawls by itself (customServerAiStep)
    }

    // ------------------------------------------------------------------ data

    public Critter animal() {
        return animal;
    }

    public CritterSpecies species() {
        if (level().isClientSide) {
            int i = entityData.get(DATA_SPECIES);
            CritterSpecies[] all = CritterSpecies.values();
            return all[Math.floorMod(i, all.length)];
        }
        return animal.species;
    }

    public boolean isWild() {
        return wild;
    }

    /** Sets the animal (a copy) and the look the clients see. */
    public void setAnimal(Critter c) {
        this.animal = c.copy(0);
        int color = c.genes.color(c.species);
        int col = color == CritterGenes.CHOCOLATE ? 120 : color < 0 ? 0xFF : color;
        int flags = (c.female ? 1 : 0) | (c.berried() ? 2 : 0) | (c.adult() ? 4 : 0);
        entityData.set(DATA_SPECIES, c.species.ordinal());
        entityData.set(DATA_LOOK, (col & 0xFF) | (Math.round(c.genes.grade() * 255) << 8) | (flags << 16) | (Math.round((float) c.size() * 255) << 20 & 0xFF00000));
        entityData.set(DATA_SHELL, c.look);
        refreshDimensions();
    }

    /** The data of the animal (as in a bag or a tank). */
    public CompoundTag saveAnimal() {
        return animal.saveAnimal();
    }

    /** Client: the animal as the renderer draws it. */
    public Critter drawn() {
        CritterSpecies s = species();
        if (drawn == null || drawn.species != s) drawn = new Critter(getId(), s, false, CritterGenes.WILD, 0, 0, 0);
        int l = entityData.get(DATA_LOOK);
        int col = l & 0xFF;
        drawn.clientColor = col == 120 ? CritterGenes.CHOCOLATE : col == 0xFF ? CritterGenes.WILD_COLOR : col;
        drawn.clientGrade = ((l >> 8) & 0xFF) / 255F;
        int flags = (l >> 16) & 0xF;
        drawn.clientFlags = (byte) ((flags & 2) != 0 ? org.overengineer.bettacraft.critters.CritterSync.F_BERRIED : 0);
        int size = (l >> 20) & 0xFF;
        drawn.clientSize = size == 0 ? 1F : size / 255F;
        drawn.look = entityData.get(DATA_SHELL);
        return drawn;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_SPECIES.equals(key) || DATA_LOOK.equals(key)) refreshDimensions();
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        CritterSpecies s = species();
        float size = level() != null && level().isClientSide ? drawnSize() : (float) animal.size();
        float l = (float) (s.length() * Math.max(0.3F, size));
        return EntityDimensions.scalable(Math.max(0.12F, Math.min(0.5F, l * 0.9F)), Math.max(0.08F, Math.min(0.3F, l * 0.5F)));
    }

    private float drawnSize() {
        int size = (entityData.get(DATA_LOOK) >> 20) & 0xFF;
        return size == 0 ? 1F : size / 255F;
    }

    @Override
    public Component getName() {
        if (hasCustomName()) return super.getName();
        if (level().isClientSide) {
            Critter d = drawn();
            String v = CritterGenes.varietyOf(d.species, d.clientColor, d.clientGrade); // M25 part c: also Wine Red
            return TankCritters.name(d.species, v);
        }
        return TankCritters.name(animal);
    }

    @Override
    public float getPickRadius() {
        return 0.08F;
    }

    // ------------------------------------------------------------------ spawning

    /** The species of a group that spawns together. */
    public record Group(CritterSpecies species) implements SpawnGroupData {}

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData groupData) {
        wild = spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION;
        if (spawnType != MobSpawnType.BUCKET) {
            CritterSpecies s = groupData instanceof Group g ? g.species() : null;
            var rnd = NetCatching.random(level.getLevel());
            if (s == null) {
                NetCatching.Water w = Wildlife.water(level.getBiome(blockPosition()));
                s = Wildlife.pickCritter(w, NetCatching.plantsNear(level.getLevel(), blockPosition()), NetCatching.hardBottom(level.getLevel(), blockPosition()), rnd);
                if (s == null) s = CritterSpecies.NEOCARIDINA;
            }
            Critter k = new Critter(0, s, s.hasSexes() && rnd.nextBoolean(), Wildlife.critter(s, rnd), getX(), getY(), getZ());
            k.age = s.juvenileDays() + rnd.nextDouble() * 4;
            k.look = CritterLooks.random(s, rnd);
            setAnimal(k);
            setYRot(random.nextFloat() * 360F);
            groupData = new Group(s);
        }
        return super.finalizeSpawn(level, difficulty, spawnType, groupData);
    }

    /** Creates one animal in the world from its data (not added yet). */
    @Nullable
    public static WildCritterEntity of(ServerLevel level, CompoundTag animal) {
        Critter k = Critter.loadAnimal(animal, 0, 0, 0, 0);
        if (k == null) return null;
        WildCritterEntity e = ModEntities.WILD_CRITTER.get().create(level);
        if (e == null) return null;
        e.setAnimal(k);
        return e;
    }

    /**
     * Lets animals of a bag or a bucket go at {@code pos} (they sink to the bottom and crawl off); with
     * {@code features.invertebrates.wildSpawns} off they are simply gone like before. Returns how many live in the world now.
     */
    public static int release(ServerLevel level, List<CompoundTag> animals, BlockPos pos, @Nullable Player player) {
        if (!Features.enabled(Feature.INVERTEBRATES_WILD)) {
            if (player != null) player.displayClientMessage(Component.translatable("message.bettacraft.critters.released"), true);
            return 0;
        }
        int n = 0;
        for (CompoundTag a : animals) {
            Critter k = Critter.loadAnimal(a, 0, 0, 0, 0);
            if (k == null || !k.alive()) continue;
            WildCritterEntity e = of(level, a);
            if (e == null) continue;
            e.moveTo(pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.6, pos.getY() + 0.3, pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.6,
                    level.random.nextFloat() * 360F, 0);
            e.setPersistenceRequired();
            if (level.addFreshEntity(e)) n++;
        }
        if (player != null) player.displayClientMessage(Component.translatable("message.bettacraft.critters.released_world", n), true);
        return n;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return wild && !hasCustomName() && !isPersistenceRequired();
    }

    @Override
    public boolean requiresCustomPersistence() {
        return !wild || super.requiresCustomPersistence();
    }

    // ------------------------------------------------------------------ crawling

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!isInWater()) return;
        if (target == null || --idle <= 0) {
            target = null;
            idle = 40 + random.nextInt(160);
            if (random.nextInt(3) > 0) {
                double r = 0.6 + random.nextDouble() * 2.5;
                double a = random.nextDouble() * Math.PI * 2;
                BlockPos.MutableBlockPos p = BlockPos.containing(getX() + Math.cos(a) * r, getY() + 0.5, getZ() + Math.sin(a) * r).mutable();
                for (int i = 0; i < 4 && level().getFluidState(p.below()).is(FluidTags.WATER); i++) p.move(Direction.DOWN);
                if (level().getFluidState(p).is(FluidTags.WATER) && level().getBlockState(p.below()).isFaceSturdy(level(), p.below(), Direction.UP)) {
                    target = new Vec3(p.getX() + 0.2 + random.nextDouble() * 0.6, p.getY(), p.getZ() + 0.2 + random.nextDouble() * 0.6);
                }
            }
        }
        if (target != null) {
            double dx = target.x - getX(), dz = target.z - getZ();
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < 0.05) {
                target = null;
                return;
            }
            double v = species().speed() / 20.0 * 1.5;
            Vec3 m = getDeltaMovement();
            setDeltaMovement(dx / d * v, m.y, dz / d * v);
            float yaw = (float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90F;
            setYRot(net.minecraft.util.Mth.approachDegrees(getYRot(), yaw, 8F));
            yBodyRot = getYRot();
            if (horizontalCollision && onGround()) setDeltaMovement(getDeltaMovement().add(0, 0.12, 0));
        }
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected void handleAirSupply(int air) {
        // snails and crabs keep a while on land, slowly drying out like fish
        super.handleAirSupply(air);
    }

    // ------------------------------------------------------------------ catching

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean net = held.getItem() instanceof org.overengineer.bettacraft.tank.plants.AquariumNetItem;
        boolean bag = held.getItem() instanceof org.overengineer.bettacraft.entity.betta.FishBagItem;
        boolean bucket = AnimalContainers.waterBucket(held) || held.getItem() instanceof org.overengineer.bettacraft.containers.CritterBucketItem;
        if (!net && !bag && !bucket) return super.mobInteract(player, hand);
        if (level().isClientSide) return InteractionResult.SUCCESS;
        Component err = catchBy(player, hand, held, bucket);
        if (err != null) player.displayClientMessage(err, true);
        return InteractionResult.CONSUME;
    }

    /** Puts this animal into a container of the player (the one in the hand first); null on success, else the reason. */
    @Nullable
    private Component catchBy(Player player, InteractionHand hand, ItemStack held, boolean bucket) {
        if (!isAlive()) return null;
        CritterSpecies s = animal.species;
        var inv = player.getInventory();
        int handSlot = hand == InteractionHand.MAIN_HAND ? inv.selected : -1;
        AnimalContainers.Found found = null;
        if (handSlot >= 0) {
            // the container in the hand when it fits
            AnimalContainers.Found any = AnimalContainers.findForCritter(player, s, bucket);
            if (any != null && any.slot() == handSlot) found = any;
            else if (bucket && AnimalContainers.waterBucket(held)) {
                if (!AnimalContainers.suits(org.overengineer.bettacraft.reef.SaltTolerance.critter(s), AnimalContainers.seaWater(held))) {
                    return Component.translatable(org.overengineer.bettacraft.reef.SaltTolerance.critter(s).marine()
                            ? "message.bettacraft.container.needs_sea_water" : "message.bettacraft.container.needs_fresh_water").withStyle(ChatFormatting.GOLD);
                }
                found = new AnimalContainers.Found(handSlot, AnimalContainers.Mode.WATER_BUCKET);
            }
        }
        if (found == null) found = AnimalContainers.findForCritter(player, s, bucket);
        if (found == null || found.mode() == AnimalContainers.Mode.EMPTY_BUCKET) {
            return Component.translatable(bucket ? "message.bettacraft.container.no_bucket" : "message.bettacraft.net.no_bag",
                    TankCritters.name(animal)).withStyle(ChatFormatting.GOLD);
        }
        if (held.getItem() instanceof org.overengineer.bettacraft.tank.plants.AquariumNetItem && !player.getAbilities().instabuild) {
            held.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
        }
        AnimalContainers.putCritter(player, found, saveAnimal(), bucket);
        level().playSound(null, blockPosition(), SoundEvents.BUCKET_FILL_FISH, SoundSource.NEUTRAL, 0.7F, 1.4F);
        player.displayClientMessage(Component.translatable("message.bettacraft.critters.caught", TankCritters.name(animal)).withStyle(ChatFormatting.GREEN), true);
        discard();
        return null;
    }

    // ------------------------------------------------------------------ save

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("critter", animal.saveAnimal());
        if (wild) tag.putBoolean("wild", true);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("critter", Tag.TAG_COMPOUND)) {
            Critter k = Critter.loadAnimal(tag.getCompound("critter"), 0, 0, 0, 0);
            if (k != null) setAnimal(k);
        }
        wild = tag.getBoolean("wild");
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.SLIME_SQUISH_SMALL;
    }

    @Override
    protected float getSoundVolume() {
        return 0.3F;
    }
}
