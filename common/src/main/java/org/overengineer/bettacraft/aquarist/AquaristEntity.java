package org.overengineer.bettacraft.aquarist;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.overengineer.bettacraft.util.Perms;
import org.overengineer.bettacraft.util.NbtCompat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.platform.item.IItemHandler;
import org.overengineer.bettacraft.platform.item.ItemHandlerHelper;
import org.overengineer.bettacraft.platform.item.ItemStackHandler;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.plumbing.WaterSources;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.WaterValues;
import org.overengineer.bettacraft.villager.ModVillagers;

/**
 * M51 (question E1): the hired aquarist, a person of his own (not a villager): a human model with procedural work clothes
 * ({@code tools/assetgen/hired_aquarist.py}), a first name (a name tag renames him, only for his employer). He cannot be
 * hurt (only {@code /kill} and the void remove him), runs away from monsters, cannot be pushed, leashed, ridden or
 * caught, and only his employer (or an operator) can use his menu.
 * <p>
 * Life: he comes to a player and offers his work ({@link Phase#OFFERING}, {@link AquaristArrival}); hired
 * ({@link Phase#HIRED}) he takes a free bed and a free breeding basin (the POI tickets, so villagers leave them alone)
 * within {@code hiredAquarist.homeRadius}, sleeps at night, takes his wage each morning from the chests at the basin and
 * works by day ({@link AquaristWork}); refused, dismissed or unpaid he leaves ({@link Phase#LEAVING}) after putting back
 * whatever he carries.
 */
public class AquaristEntity extends PathfinderMob {
    public enum Phase { OFFERING, HIRED, LEAVING }

    public static final int VARIANTS = 4;
    public static final int BAG_SLOTS = 18;
    static final String[] NAMES = {"Lena", "Finn", "Mika", "Jonas", "Aiko", "Ravi", "Sofia", "Mateo", "Noor", "Emil", "Ida", "Kai", "Yuki",
            "Tomas", "Marta", "Lars", "Ana", "Oskar", "Leni", "Paul", "Mila", "Theo", "Nora", "Jan", "Lea", "Hugo", "Ella", "Ben", "Zoe", "Anouk"};

    private static final EntityDataAccessor<Byte> DATA_PHASE = SynchedEntityData.defineId(AquaristEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(AquaristEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_EMPLOYER = SynchedEntityData.defineId(AquaristEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_STATUS = SynchedEntityData.defineId(AquaristEntity.class, EntityDataSerializers.STRING);

    @Nullable
    private UUID employer;
    private long offerUntil = Long.MAX_VALUE;
    private boolean offerSaid;
    @Nullable
    private BlockPos workplace, bed;
    private final int[] levels = new int[AquaristTask.ALL.length];
    /** M66: the rearing tanks in the order the young are offered to them, each with the species it takes (empty: all). */
    private final List<RearingTank> rearing = new ArrayList<>();
    /** M66: the tasks switched off per tank (bit = task ordinal); a tank not in the map gets everything. */
    private final Map<UUID, Integer> tankOff = new HashMap<>();
    /** M66: the chest of his wage and his food, assigned in the menu; null: he looks in the blocks around the workplace. */
    @Nullable
    private BlockPos chest;
    private final ItemStackHandler bag = new ItemStackHandler(BAG_SLOTS);
    private int waterMb;
    @Nullable
    private WaterValues waterValues;
    private boolean tapWater;
    @Nullable
    private UUID waterFor;
    private final List<ItemStack> carried = new ArrayList<>();
    @Nullable
    private UUID carriedFrom;
    /** M66: the rearing tank the carried young are on their way to (null: the first). */
    @Nullable
    private UUID carriedTo;
    private long lastWageDay = Long.MIN_VALUE;
    private int unpaidDays, homelessDays;
    private long leaveAt = -1;
    /** Warnings reported per tank (until they clear). */
    private final Map<UUID, Set<String>> reported = new HashMap<>();
    /** Complaints said (key, game time), at most one of a kind per in-game day. */
    private final Map<String, Long> complained = new HashMap<>();
    // transient
    private final ArrayDeque<AquaristWork.Job> jobs = new ArrayDeque<>();
    @Nullable
    private AquaristWork.Job current;
    private int jobTicks, travelTicks;
    private long nextPlan;
    private final Map<UUID, Long> warnLooks = new HashMap<>();
    /** GameTests and the client smoke: works around the clock (no sleep, no time windows). */
    public boolean alwaysDay;
    /** Jobs done since he was created (GameTests). */
    public int jobsDone;

    public AquaristEntity(EntityType<? extends AquaristEntity> type, Level level) {
        super(type, level);
        for (AquaristTask t : AquaristTask.ALL) levels[t.ordinal()] = t.defaultLevel();
        setPersistenceRequired();
        setCanPickUpLoot(false);
        for (EquipmentSlot s : EquipmentSlot.values()) setDropChance(s, 0F);
        getNavigation().setCanFloat(true);
        if (getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation g) g.setCanOpenDoors(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.5).add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(DATA_PHASE, (byte) Phase.OFFERING.ordinal());
        b.define(DATA_VARIANT, 0);
        b.define(DATA_EMPLOYER, "");
        b.define(DATA_STATUS, "offering");
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Monster.class, 10.0F, 0.6, 0.75));
        goalSelector.addGoal(2, new OfferGoal());
        goalSelector.addGoal(2, new LeaveGoal());
        goalSelector.addGoal(3, new SleepGoal());
        goalSelector.addGoal(4, new WorkGoal());
        goalSelector.addGoal(5, new HomeGoal());
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    // ------------------------------------------------------------------ accessors

    public Phase phase() {
        int i = entityData.get(DATA_PHASE);
        return i >= 0 && i < Phase.values().length ? Phase.values()[i] : Phase.OFFERING;
    }

    void setPhase(Phase p) {
        entityData.set(DATA_PHASE, (byte) p.ordinal());
    }

    public int variant() {
        return Math.floorMod(entityData.get(DATA_VARIANT), VARIANTS);
    }

    public void setVariant(int v) {
        entityData.set(DATA_VARIANT, Math.floorMod(v, VARIANTS));
    }

    /** The employer's name (synced; empty while he offers to nobody). */
    public String employerName() {
        return entityData.get(DATA_EMPLOYER);
    }

    /** The status key (synced): offering, working, idle, sleeping, no_workplace, no_bed, unpaid, leaving, off. */
    public String status() {
        return entityData.get(DATA_STATUS);
    }

    @Nullable
    public UUID employer() {
        return employer;
    }

    void setEmployer(@Nullable UUID id, String name) {
        employer = id;
        entityData.set(DATA_EMPLOYER, name == null ? "" : name);
    }

    @Nullable
    public BlockPos workplace() {
        return workplace;
    }

    @Nullable
    public BlockPos bed() {
        return bed;
    }

    public int taskLevel(AquaristTask t) {
        return levels[t.ordinal()];
    }

    public void setTaskLevel(AquaristTask t, int lvl) {
        levels[t.ordinal()] = Math.max(0, Math.min(AquaristTask.MAX_LEVEL, lvl));
    }

    /** M66: a rearing tank with the species it takes (their keys, {@link AquaristWork#speciesKey}); no species: every one. */
    public static final class RearingTank {
        private final UUID tank;
        private final java.util.LinkedHashSet<String> species = new java.util.LinkedHashSet<>();

        RearingTank(UUID tank) {
            this.tank = tank;
        }

        public UUID tank() {
            return tank;
        }

        public Set<String> species() {
            return java.util.Collections.unmodifiableSet(species);
        }

        public boolean takes(String key) {
            return species.isEmpty() || species.contains(key);
        }
    }

    /** M66: every rearing tank he has, in order. */
    public List<RearingTank> rearingTanks() {
        return java.util.Collections.unmodifiableList(rearing);
    }

    /** The first rearing tank (before M66 the only one), or null. */
    @Nullable
    public UUID rearingTank() {
        return rearing.isEmpty() ? null : rearing.get(0).tank;
    }

    /** Makes {@code id} his only rearing tank (null: none). */
    public void setRearingTank(@Nullable UUID id) {
        rearing.clear();
        if (id != null) rearing.add(new RearingTank(id));
    }

    /** M66: adds a rearing tank at the end of the list (false when it is one already). */
    public boolean addRearingTank(UUID id) {
        for (RearingTank r : rearing) if (r.tank.equals(id)) return false;
        rearing.add(new RearingTank(id));
        return true;
    }

    public boolean removeRearingTank(UUID id) {
        return rearing.removeIf(r -> r.tank.equals(id));
    }

    /** M66: the species a rearing tank takes: switches {@code key} on or off (none switched on: every species). */
    public void toggleRearingSpecies(UUID id, String key) {
        for (RearingTank r : rearing) {
            if (!r.tank.equals(id)) continue;
            if (!r.species.remove(key)) r.species.add(key);
        }
    }

    public void clearRearingSpecies(UUID id) {
        for (RearingTank r : rearing) if (r.tank.equals(id)) r.species.clear();
    }

    /** M66: whether he may do {@code task} at the tank {@code id} (everything unless switched off for it in the menu). */
    public boolean mayDo(@Nullable UUID id, AquaristTask task) {
        return id == null || (tankOff.getOrDefault(id, 0) & (1 << task.ordinal())) == 0;
    }

    public void setMayDo(UUID id, AquaristTask task, boolean on) {
        int m = tankOff.getOrDefault(id, 0);
        m = on ? m & ~(1 << task.ordinal()) : m | (1 << task.ordinal());
        if (m == 0) tankOff.remove(id);
        else tankOff.put(id, m);
    }

    /** The bit mask of the tasks switched off at a tank (for the menu). */
    public int tasksOff(UUID id) {
        return tankOff.getOrDefault(id, 0);
    }

    /** M66: forgets the bed without giving back its ticket (the caller did). */
    void clearBed() {
        if (isSleeping()) stopSleeping();
        bed = null;
    }

    /** M66: the assigned chest (null: automatic). */
    @Nullable
    public BlockPos chest() {
        return chest;
    }

    public void setChest(@Nullable BlockPos pos) {
        chest = pos == null ? null : pos.immutable();
    }

    /**
     * M66: makes the bed at {@code pos} (either half) his: gives back the old bed and takes the new one (its POI ticket).
     * Returns false when it is no bed or not free.
     */
    public boolean assignBed(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BedBlock)) return false;
        BlockPos head = state.getValue(BedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.HEAD ? pos
                : pos.relative(state.getValue(BedBlock.FACING));
        if (head.equals(bed)) return true;
        var poi = level.getPoiManager();
        if (!poi.existsAtPosition(PoiTypes.HOME, head)) return false;
        BlockPos old = bed;
        if (old != null && level.isLoaded(old) && poi.existsAtPosition(PoiTypes.HOME, old)) poi.release(old);
        BlockPos taken = poi.take(h -> h.is(PoiTypes.HOME), (h, p) -> p.equals(head), head, 1).map(BlockPos::immutable).orElse(null);
        if (taken == null) { // taken by someone else: take the old one back
            if (old != null) poi.take(h -> h.is(PoiTypes.HOME), (h, p) -> p.equals(old), old, 1);
            return false;
        }
        if (isSleeping()) stopSleeping();
        bed = taken;
        return true;
    }

    /**
     * M66: makes the free breeding basin at {@code pos} his workplace: gives back the old basin and takes the new one (its POI
     * ticket). Returns false when it is no basin or somebody else has it.
     */
    public boolean assignBasin(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).is(ModBlocks.BREEDING_BASIN.get())) return false;
        if (pos.equals(workplace)) return true;
        var poi = level.getPoiManager();
        if (!poi.existsAtPosition(ModVillagers.AQUARIST_POI_KEY, pos)) return false;
        BlockPos old = workplace;
        if (old != null && level.isLoaded(old) && poi.existsAtPosition(ModVillagers.AQUARIST_POI_KEY, old)) poi.release(old);
        BlockPos taken = poi.take(h -> h.is(ModVillagers.AQUARIST_POI_KEY), (h, p) -> p.equals(pos), pos, 1).map(BlockPos::immutable).orElse(null);
        if (taken == null) { // occupied: take the old one back
            if (old != null) poi.take(h -> h.is(ModVillagers.AQUARIST_POI_KEY), (h, p) -> p.equals(old), old, 1);
            return false;
        }
        workplace = taken;
        return true;
    }

    /** M66: "auto" for the basin: gives it back and looks for another free one (the old one again when there is none). */
    public void autoBasin(ServerLevel level) {
        BlockPos old = workplace;
        var poi = level.getPoiManager();
        if (old != null && level.isLoaded(old) && poi.existsAtPosition(ModVillagers.AQUARIST_POI_KEY, old)) poi.release(old);
        workplace = null;
        int r = ServerConfig.getOr(ServerConfig.AQUARIST_HOME_RADIUS, 32);
        poi.take(h -> h.is(ModVillagers.AQUARIST_POI_KEY), (h, p) -> !p.equals(old), blockPosition(), r).ifPresent(p -> workplace = p.immutable());
        if (workplace == null && old != null) poi.take(h -> h.is(ModVillagers.AQUARIST_POI_KEY), (h, p) -> p.equals(old), old, 1).ifPresent(p -> workplace = p.immutable());
    }

    public ItemStackHandler bag() {
        return bag;
    }

    public long offerUntil() {
        return offerUntil;
    }

    public void setOfferUntil(long t) {
        offerUntil = t;
    }

    public int unpaidDays() {
        return unpaidDays;
    }

    public int homelessDays() {
        return homelessDays;
    }

    @Nullable
    public AquaristWork.Job currentJob() {
        return current;
    }

    public int queuedJobs() {
        return jobs.size() + (current == null ? 0 : 1);
    }

    /** The name in the tank log ("Lena (aquarist)"). */
    public String logName() {
        return getName().getString();
    }

    public boolean hasFood() {
        for (int i = 0; i < bag.getSlots(); i++) if (AquaristWork.isTankFood(bag.getStackInSlot(i))) return true;
        return false;
    }

    public boolean hasCarriedItems() {
        for (int i = 0; i < bag.getSlots(); i++) if (!bag.getStackInSlot(i).isEmpty()) return true;
        return false;
    }

    public int bagFreeSlots() {
        int n = 0;
        for (int i = 0; i < bag.getSlots(); i++) if (bag.getStackInSlot(i).isEmpty()) n++;
        return n;
    }

    /** Into the bag; what does not fit goes into the chests at the workplace, else onto the ground at his feet (never lost). */
    public void putInBag(ServerLevel level, ItemStack s) {
        ItemStack rest = ItemHandlerHelper.insertItemStacked(bag, s.copy(), false);
        if (rest.isEmpty()) return;
        for (IItemHandler h : AquaristWork.chests(level, this)) {
            rest = ItemHandlerHelper.insertItemStacked(h, rest, false);
            if (rest.isEmpty()) return;
        }
        spawnAtLocation((net.minecraft.server.level.ServerLevel) level(), rest);
    }

    // ---- water

    public int waterMb() {
        return waterMb;
    }

    @Nullable
    public WaterValues waterValues() {
        return tapWater ? null : waterValues;
    }

    @Nullable
    public UUID waterFor() {
        return waterFor;
    }

    void setWaterFor(@Nullable UUID id) {
        waterFor = id;
    }

    /** Adds water he took (values null: tap water). */
    void addWater(int mb, @Nullable WaterValues v) {
        if (mb <= 0) return;
        if (waterMb <= 0) {
            waterValues = v;
            tapWater = v == null;
        } else if (!tapWater || v != null) {
            WaterValues a = tapWater ? WaterValues.tap(org.overengineer.bettacraft.tank.WaterValues.FLUID_TEMP) : waterValues;
            waterValues = org.overengineer.bettacraft.plumbing.WaterStore.mix(a, waterMb, v == null ? WaterValues.tap(WaterValues.FLUID_TEMP) : v, mb);
            tapWater = false;
        }
        waterMb += mb;
    }

    void useWater(int mb) {
        waterMb = Math.max(0, waterMb - mb);
        if (waterMb == 0) {
            waterValues = null;
            tapWater = false;
            waterFor = null;
        }
    }

    /** Pours what is left in his can back into the source at the workplace (tap water just goes down the drain). */
    void pourBackWater(ServerLevel level) {
        if (waterMb <= 0) {
            useWater(0);
            return;
        }
        if (!tapWater && workplace != null) {
            FluidStack s = WaterValues.stack(waterMb, waterValues);
            for (IFluidHandler h : WaterSources.handlers(level, workplace)) {
                int n = h.fill(s, IFluidHandler.FluidAction.EXECUTE);
                s.shrink(n);
                if (s.isEmpty()) break;
            }
        }
        useWater(waterMb);
    }

    // ---- young

    public List<ItemStack> carriedAnimals() {
        return carried;
    }

    @Nullable
    public UUID carriedFrom() {
        return carriedFrom;
    }

    @Nullable
    public UUID carriedTo() {
        return carriedTo;
    }

    void setCarriedTo(@Nullable UUID id) {
        carriedTo = id;
    }

    void carry(ItemStack filledBag, UUID from) {
        carried.add(filledBag);
        carriedFrom = from;
    }

    void setCarried(List<ItemStack> left) {
        carried.clear();
        carried.addAll(left);
        if (carried.isEmpty()) carriedFrom = carriedTo = null;
    }

    /** Nowhere to put the animals he carries: as filled fish bags into a chest at the workplace (else dropped there). */
    void storeCarriedAnimals(ServerLevel level) {
        for (ItemStack b : carried) {
            ItemStack rest = b.copy();
            for (IItemHandler h : AquaristWork.chests(level, this)) {
                rest = ItemHandlerHelper.insertItemStacked(h, rest, false);
                if (rest.isEmpty()) break;
            }
            if (!rest.isEmpty()) {
                BlockPos at = workplace != null ? workplace.above() : blockPosition();
                net.minecraft.world.Containers.dropItemStack(level, at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5, rest);
            }
            org.slf4j.LoggerFactory.getLogger("bettacraft/aquarist").info("{} put a fish bag he could not empty into the chest at {}", logName(), workplace);
        }
        if (!carried.isEmpty()) complain(level, "young_stored", null);
        setCarried(List.of());
    }

    // ---- warnings and messages

    long lastWarnLook(UUID tank) {
        return warnLooks.getOrDefault(tank, Long.MIN_VALUE / 2);
    }

    void markWarnLook(UUID tank, long time) {
        warnLooks.put(tank, time);
    }

    /** Tells the employer the new warnings of a tank (chat and, with JourneyMap, a waypoint) and clears the ones that went away. */
    void reportWarnings(ServerLevel level, TankBlockEntity c, Set<String> now) {
        TankData t = c.getTank();
        Set<String> had = reported.computeIfAbsent(t.id(), k -> new HashSet<>());
        String tankName = t.name().isEmpty() ? "" : t.name();
        for (String w : now) {
            if (had.contains(w)) continue;
            Component where = t.name().isEmpty() ? Component.translatable("message.bettacraft.aquarist.tank_at", c.getBlockPos().getX(), c.getBlockPos().getY(), c.getBlockPos().getZ())
                    : Component.literal("\"" + t.name() + "\"");
            say(level, Component.translatable("message.bettacraft.aquarist.warning", where, Component.translatable("aquarist.bettacraft.warning." + w)), ChatFormatting.GOLD);
            org.overengineer.bettacraft.monitor.MonitorAlarms.external(level, t.id(), "aquarist:" + getUUID() + ":" + w, c.getBlockPos(), tankName,
                    "aquarist:warning." + w, true, employer);
        }
        for (String w : new ArrayList<>(had)) {
            if (now.contains(w)) continue;
            org.overengineer.bettacraft.monitor.MonitorAlarms.external(level, t.id(), "aquarist:" + getUUID() + ":" + w, c.getBlockPos(), tankName,
                    "aquarist:warning." + w, false, employer);
        }
        had.clear();
        had.addAll(now);
        if (had.isEmpty()) reported.remove(t.id());
    }

    /** Warnings he reported and that are still open (GameTests, menu). */
    public Map<UUID, Set<String>> reported() {
        return reported;
    }

    /** Says something to his employer (chat, if online): "<Lena> ...". */
    public void say(ServerLevel level, Component what, ChatFormatting style) {
        if (employer == null) return;
        ServerPlayer p = level.getServer().getPlayerList().getPlayer(employer);
        if (p == null) return;
        p.sendSystemMessage(Component.translatable("message.bettacraft.aquarist.say", getName(), what.copy().withStyle(style)));
    }

    /** A complaint (no food, no water, chest full ...), at most once per kind and in-game day. */
    void complain(ServerLevel level, String key, @Nullable TankBlockEntity c) {
        long now = level.getGameTime();
        Long last = complained.get(key);
        if (last != null && now - last < 24000L && now >= last) return;
        complained.put(key, now);
        say(level, Component.translatable("message.bettacraft.aquarist.complaint." + key), ChatFormatting.YELLOW);
        if (c != null) org.overengineer.bettacraft.tank.TankCare.log(level, c, "log.bettacraft.aquarist.complaint." + key, logName());
    }

    /** Complaints said (GameTests). */
    public Set<String> complaints() {
        return complained.keySet();
    }

    /** Swings his arm at a place (the work shows). */
    void swingWork(ServerLevel level, @Nullable BlockPos at) {
        swing(InteractionHand.MAIN_HAND);
        if (at != null) getLookControl().setLookAt(Vec3.atCenterOf(at));
    }

    // ------------------------------------------------------------------ home

    /** Takes a free breeding basin and a free bed within {@code hiredAquarist.homeRadius} (the POI tickets). */
    public void findHome(ServerLevel level) {
        int r = ServerConfig.getOr(ServerConfig.AQUARIST_HOME_RADIUS, 32);
        var poi = level.getPoiManager();
        BlockPos center = blockPosition();
        if (workplace == null) {
            poi.take(h -> h.is(ModVillagers.AQUARIST_POI_KEY), (h, p) -> true, center, r).ifPresent(p -> workplace = p.immutable());
        }
        if (bed == null) {
            poi.take(h -> h.is(PoiTypes.HOME), (h, p) -> true, workplace != null ? workplace : center, r).ifPresent(p -> bed = p.immutable());
        }
    }

    /**
     * Takes exactly this basin and bed (their POI tickets; GameTests and the client smoke, where several homes lie close
     * together). Returns whether both were free.
     */
    public boolean setHome(ServerLevel level, BlockPos basin, BlockPos bedHead) {
        releaseHome(level);
        var poi = level.getPoiManager();
        workplace = poi.take(h -> h.is(ModVillagers.AQUARIST_POI_KEY), (h, p) -> p.equals(basin), basin, 1).map(BlockPos::immutable).orElse(null);
        bed = poi.take(h -> h.is(PoiTypes.HOME), (h, p) -> p.equals(bedHead), bedHead, 1).map(BlockPos::immutable).orElse(null);
        return housed();
    }

    /** Gives back the basin and the bed (the POI tickets). */
    public void releaseHome(ServerLevel level) {
        var poi = level.getPoiManager();
        if (workplace != null && level.isLoaded(workplace) && poi.existsAtPosition(ModVillagers.AQUARIST_POI_KEY, workplace)) poi.release(workplace);
        if (bed != null && level.isLoaded(bed) && poi.existsAtPosition(PoiTypes.HOME, bed)) poi.release(bed);
        if (isSleeping()) stopSleeping();
        workplace = null;
        bed = null;
    }

    /** Forgets a basin or bed that is gone (broken), then looks for new ones. */
    void checkHome(ServerLevel level) {
        if (workplace != null && level.isLoaded(workplace) && !level.getBlockState(workplace).is(ModBlocks.BREEDING_BASIN.get())) workplace = null;
        if (bed != null && level.isLoaded(bed) && !(level.getBlockState(bed).getBlock() instanceof BedBlock)) {
            if (isSleeping()) stopSleeping();
            bed = null;
        }
        if (chest != null && level.isLoaded(chest) && org.overengineer.bettacraft.platform.Platform.get().itemHandler(level, chest, null) == null) chest = null; // M66: broken
        if (workplace == null || bed == null) findHome(level);
    }

    public boolean housed() {
        return workplace != null && bed != null;
    }

    // ------------------------------------------------------------------ wage

    static long day(Level level) {
        return Math.floorDiv(level.getDayTime(), 24000L);
    }

    /** Once per in-game day (the first time he is loaded on a new day): the wage, or one more day without it. */
    public void payDay(ServerLevel level) {
        payDay(level, day(level));
    }

    /** As {@link #payDay(ServerLevel)} for day {@code day} (GameTests). */
    public void payDay(ServerLevel level, long day) {
        if (lastWageDay == day) return;
        boolean first = lastWageDay == Long.MIN_VALUE;
        lastWageDay = day;
        if (first) return; // the first day is free
        homelessDays = housed() ? 0 : homelessDays + 1;
        if (!housed()) complain(level, workplace == null ? "no_workplace" : "no_bed", null);
        if (!ServerConfig.getOr(ServerConfig.AQUARIST_WAGE, true)) {
            unpaidDays = 0;
        } else if (housed() && takeWage(level)) {
            if (unpaidDays > 0) say(level, Component.translatable("message.bettacraft.aquarist.paid_again"), ChatFormatting.GREEN);
            unpaidDays = 0;
        } else if (housed()) {
            unpaidDays++;
            say(level, Component.translatable("message.bettacraft.aquarist.unpaid", unpaidDays, ServerConfig.getOr(ServerConfig.AQUARIST_LEAVE_DAYS, 3)),
                    ChatFormatting.RED);
        }
        int leave = ServerConfig.getOr(ServerConfig.AQUARIST_LEAVE_DAYS, 3);
        if (unpaidDays >= leave) AquaristArrival.leave(level, this, "unpaid");
        else if (homelessDays >= leave) AquaristArrival.leave(level, this, "homeless");
    }

    /** Takes the day's wage from the chests at the workplace: emeralds, else food. */
    boolean takeWage(ServerLevel level) {
        int emeralds = ServerConfig.getOr(ServerConfig.AQUARIST_WAGE_EMERALDS, 1), food = ServerConfig.getOr(ServerConfig.AQUARIST_WAGE_FOOD, 3);
        if (emeralds <= 0 && food <= 0) return true;
        List<IItemHandler> chests = AquaristWork.chests(level, this);
        if (emeralds > 0 && count(chests, s -> s.is(Items.EMERALD)) >= emeralds) {
            take(chests, s -> s.is(Items.EMERALD), emeralds);
            return true;
        }
        java.util.function.Predicate<ItemStack> meal = s -> !s.isEmpty() && org.overengineer.bettacraft.platform.Platform.get().foodProperties(s, this) != null && !AquaristWork.isTankFood(s)
                && !s.is(ModTags.BETTA_FOOD);
        if (food > 0 && count(chests, meal) >= food) {
            take(chests, meal, food);
            return true;
        }
        return false;
    }

    private static int count(List<IItemHandler> hs, java.util.function.Predicate<ItemStack> p) {
        int n = 0;
        for (IItemHandler h : hs) for (int i = 0; i < h.getSlots(); i++) if (p.test(h.getStackInSlot(i))) n += h.getStackInSlot(i).getCount();
        return n;
    }

    private static void take(List<IItemHandler> hs, java.util.function.Predicate<ItemStack> p, int n) {
        for (IItemHandler h : hs) {
            for (int i = 0; i < h.getSlots() && n > 0; i++) {
                if (!p.test(h.getStackInSlot(i))) continue;
                n -= h.extractItem(i, n, false).getCount();
            }
        }
    }

    /** How fast he works now (slower without his wage). */
    public double workSpeed() {
        return unpaidDays > 0 ? ServerConfig.getOr(ServerConfig.AQUARIST_UNPAID_SPEED, 0.5) : 1.0;
    }

    // ------------------------------------------------------------------ leaving

    /** Puts back everything he carries (animals into their tank or a chest, food and cuttings into the chests, water back). */
    void putEverythingBack(ServerLevel level) {
        if (!carried.isEmpty()) {
            TankBlockEntity from = AquaristWork.tank(level, this, carriedFrom);
            if (from != null) AquaristWork.releaseYoung(level, this, from, true);
            if (!carried.isEmpty()) storeCarriedAnimals(level);
        }
        AquaristWork.deposit(level, this);
        for (int i = 0; i < bag.getSlots(); i++) {
            ItemStack s = bag.getStackInSlot(i);
            if (s.isEmpty()) continue;
            BlockPos at = workplace != null ? workplace.above() : blockPosition();
            net.minecraft.world.Containers.dropItemStack(level, at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5, s.copy());
            bag.setStackInSlot(i, ItemStack.EMPTY);
        }
        pourBackWater(level);
        jobs.clear();
        current = null;
    }

    /** Starts to leave (the records were updated by the caller): puts things back, gives the home back, walks off. */
    void startLeaving(ServerLevel level) {
        if (phase() == Phase.LEAVING) return;
        putEverythingBack(level);
        releaseHome(level);
        for (Map.Entry<UUID, Set<String>> e : new HashMap<>(reported).entrySet()) {
            for (String w : e.getValue()) {
                org.overengineer.bettacraft.monitor.MonitorAlarms.external(level, e.getKey(), "aquarist:" + getUUID() + ":" + w, BlockPos.ZERO, "",
                        "aquarist:warning." + w, false, employer);
            }
        }
        reported.clear();
        setPhase(Phase.LEAVING);
        leaveAt = level.getGameTime() + 200;
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    // ------------------------------------------------------------------ ticking

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);
        if (!(level() instanceof ServerLevel level) || tickCount % 20 != 0) return;
        AquaristArrival.check(level, this);
        if (isRemoved()) return;
        if (phase() == Phase.LEAVING) {
            if (leaveAt < 0) leaveAt = level.getGameTime() + 200;
            if (level.getGameTime() >= leaveAt) {
                level.sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 12, 0.3, 0.6, 0.3, 0.02);
                discard();
            }
            return;
        }
        // switched off he only waits: no wage day counts, nothing makes him leave
        if (phase() == Phase.HIRED && Features.enabled(Feature.AQUARIST_NPC)) {
            if (tickCount % 100 == 0) checkHome(level);
            payDay(level);
        }
        entityData.set(DATA_STATUS, computeStatus(level));
    }

    String computeStatus(ServerLevel level) {
        if (phase() == Phase.OFFERING) return "offering";
        if (phase() == Phase.LEAVING) return "leaving";
        if (!Features.enabled(Feature.AQUARIST_NPC)) return "off";
        if (workplace == null) return "no_workplace";
        if (bed == null) return "no_bed";
        if (isSleeping()) return "sleeping";
        if (unpaidDays > 0) return "unpaid";
        return current != null ? "working" : "idle";
    }

    /** Day for his work: 0:00 to 12:00 of the day (6 to 18 o'clock). */
    boolean workTime() {
        if (alwaysDay) return true;
        long t = Math.floorMod(level().getDayTime(), 24000L);
        return t < 12000L || t >= 23500L;
    }

    public boolean canWork() {
        return phase() == Phase.HIRED && Features.enabled(Feature.AQUARIST_NPC) && housed() && workTime() && !isSleeping();
    }

    /** The place a job takes him to (the workplace or the tank's nearest block), null when it cannot be done any more. */
    @Nullable
    BlockPos target(ServerLevel level, AquaristWork.Job job) {
        if (job.atWorkplace()) return job.kind() != AquaristWork.Kind.FETCH_WATER && chest != null ? chest : workplace; // M66: the assigned chest
        TankBlockEntity c = AquaristWork.tank(level, this, job.tank());
        if (c == null) return null;
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (long l : c.getTank().members()) {
            BlockPos p = BlockPos.of(l);
            if (c.getTank().contains(p.below())) continue; // the bottom layer: where he stands next to the tank
            double d = p.distSqr(blockPosition());
            if (d < bd) {
                bd = d;
                best = p;
            }
        }
        return best;
    }

    boolean arrived(ServerLevel level, AquaristWork.Job job, BlockPos target) {
        if (job.atWorkplace()) return position().distanceToSqr(Vec3.atCenterOf(target)) <= 2.6 * 2.6;
        TankBlockEntity c = AquaristWork.tank(level, this, job.tank());
        return c != null && AquaristWork.reach(c.getTank()).contains(position());
    }

    /** One tick of work: walk to the job's place, work there for two seconds (longer without wage), do it. */
    void workTick(ServerLevel level) {
        if (current == null) {
            current = jobs.poll();
            jobTicks = 0;
            travelTicks = 0;
            if (current == null) {
                setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                return;
            }
            setItemSlot(EquipmentSlot.MAINHAND, tool(current));
        }
        BlockPos target = target(level, current);
        if (target == null) { // the tank is gone, unloaded, locked or not his any more
            if (current.kind() == AquaristWork.Kind.RELEASE_YOUNG || current.kind() == AquaristWork.Kind.RETURN_YOUNG) {
                jobs.addAll(AquaristWork.perform(level, this, current)); // sends the young back or into a chest
            } else if (current.kind() == AquaristWork.Kind.CHANGE_WATER) {
                pourBackWater(level);
            }
            current = null;
            return;
        }
        double speed = workSpeed();
        if (!arrived(level, current, target)) {
            travelTicks++;
            if (getNavigation().isDone() || travelTicks % 40 == 1) {
                Path p = getNavigation().createPath(target, current.atWorkplace() ? 1 : 2);
                if (p != null) getNavigation().moveTo(p, 0.6 * speed);
            }
            if (travelTicks > 900 / speed) { // he does not get there
                complain(level, "unreachable", current.atWorkplace() ? null : AquaristWork.tank(level, this, current.tank()));
                if (current.kind() == AquaristWork.Kind.RELEASE_YOUNG || current.kind() == AquaristWork.Kind.RETURN_YOUNG) storeCarriedAnimals(level);
                if (current.kind() == AquaristWork.Kind.CHANGE_WATER) pourBackWater(level);
                current = null;
                getNavigation().stop();
            }
            return;
        }
        getNavigation().stop();
        getLookControl().setLookAt(Vec3.atCenterOf(target));
        jobTicks++;
        if (jobTicks % 10 == 1) swing(InteractionHand.MAIN_HAND);
        if (jobTicks < 40 / speed) return;
        List<AquaristWork.Job> follow = AquaristWork.perform(level, this, current);
        for (int i = follow.size() - 1; i >= 0; i--) jobs.addFirst(follow.get(i));
        jobsDone++;
        current = null;
    }

    /** What he holds while he does a job (looks only; never dropped). */
    static ItemStack tool(AquaristWork.Job job) {
        return switch (job.kind()) {
            case FETCH_FOOD, TEND -> new ItemStack(ModItems.BETTA_PELLETS.get());
            case FETCH_WATER, CHANGE_WATER -> new ItemStack(Items.WATER_BUCKET);
            case TAKE_YOUNG, RELEASE_YOUNG, RETURN_YOUNG -> new ItemStack(ModItems.FISH_BAG.get());
            case DEPOSIT -> ItemStack.EMPTY;
        };
    }

    /** Plans the next round when it is time and nothing is left. */
    boolean planIfIdle(ServerLevel level) {
        if (current != null || !jobs.isEmpty()) return true;
        if (level.getGameTime() < nextPlan) return false;
        nextPlan = level.getGameTime() + 100;
        jobs.addAll(AquaristWork.plan(level, this));
        return !jobs.isEmpty();
    }

    /** GameTests: plans now (ignores the pause between rounds). */
    public List<AquaristWork.Job> planNow(ServerLevel level) {
        nextPlan = 0;
        jobs.clear();
        current = null;
        planIfIdle(level);
        return new ArrayList<>(jobs);
    }

    // ------------------------------------------------------------------ goals

    /** Offering: walks to the player he offers his work to and says it once. */
    final class OfferGoal extends Goal {
        OfferGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Nullable
        private Player target() {
            if (employer == null) return null;
            Player p = level().getPlayerByUUID(employer);
            return p != null && p.isAlive() && !p.isSpectator() && p.distanceToSqr(AquaristEntity.this) < 64 * 64 ? p : null;
        }

        @Override
        public boolean canUse() {
            return phase() == Phase.OFFERING && target() != null;
        }

        @Override
        public void tick() {
            Player p = target();
            if (p == null) return;
            getLookControl().setLookAt(p, 30F, 30F);
            if (distanceToSqr(p) > 3.5 * 3.5) {
                if (getNavigation().isDone() || tickCount % 40 == 0) getNavigation().moveTo(p, 0.6);
            } else {
                getNavigation().stop();
            }
            if (!offerSaid && distanceToSqr(p) < 8 * 8 && p instanceof ServerPlayer sp) {
                offerSaid = true;
                AquaristArrival.sayOffer(sp, AquaristEntity.this);
            }
        }
    }

    /** Leaving: walks away. */
    final class LeaveGoal extends Goal {
        LeaveGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return phase() == Phase.LEAVING;
        }

        @Override
        public void tick() {
            if (!getNavigation().isDone()) return;
            Vec3 away = DefaultRandomPos.getPos(AquaristEntity.this, 16, 4);
            if (away != null) getNavigation().moveTo(away.x, away.y, away.z, 0.6);
        }
    }

    /** At night: to his bed and sleep; in the morning up again. */
    final class SleepGoal extends Goal {
        SleepGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return phase() == Phase.HIRED && bed != null && !workTime();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            if (isSleeping() || bed == null) return;
            if (position().distanceToSqr(Vec3.atBottomCenterOf(bed)) <= 1.8 * 1.8) {
                getNavigation().stop();
                startSleeping(bed);
            } else if (getNavigation().isDone() || tickCount % 40 == 0) {
                getNavigation().moveTo(bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5, 0.5);
            }
        }

        @Override
        public void stop() {
            if (isSleeping()) stopSleeping();
        }
    }

    /** By day: his jobs. */
    final class WorkGoal extends Goal {
        WorkGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return level() instanceof ServerLevel level && canWork() && planIfIdle(level);
        }

        @Override
        public boolean canContinueToUse() {
            return canWork() && (current != null || !jobs.isEmpty());
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (level() instanceof ServerLevel level) workTick(level);
        }

        @Override
        public void stop() {
            getNavigation().stop();
            if (current == null && jobs.isEmpty()) setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    /** Idle: stays near his workplace and strolls around it. */
    final class HomeGoal extends Goal {
        private int wait;

        HomeGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (phase() != Phase.HIRED || isSleeping()) return false;
            if (--wait > 0) return false;
            wait = 80 + getRandom().nextInt(120);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return !getNavigation().isDone() && phase() == Phase.HIRED;
        }

        @Override
        public void start() {
            BlockPos home = workplace != null ? workplace : bed;
            if (home == null) {
                Vec3 v = DefaultRandomPos.getPos(AquaristEntity.this, 6, 2);
                if (v != null) getNavigation().moveTo(v.x, v.y, v.z, 0.45);
                return;
            }
            if (position().distanceToSqr(Vec3.atCenterOf(home)) > 8 * 8) {
                getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.5);
                return;
            }
            BlockPos p = home.offset(getRandom().nextInt(9) - 4, 0, getRandom().nextInt(9) - 4);
            getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0.4);
        }
    }

    // ------------------------------------------------------------------ interaction and protection

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        boolean boss = player.getUUID().equals(employer) || Perms.has(player, 2);
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.NAME_TAG) && player.getUUID().equals(employer) && phase() == Phase.HIRED) return InteractionResult.PASS; // the name tag renames him
        if (!boss) {
            sp.displayClientMessage(Component.translatable(phase() == Phase.OFFERING ? "message.bettacraft.aquarist.offers_to"
                    : "message.bettacraft.aquarist.works_for", getName(), employerName()), true);
            return InteractionResult.CONSUME;
        }
        if (phase() == Phase.LEAVING) return InteractionResult.CONSUME;
        AquaristMenu.open(sp, this);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurtServer(level, source, amount);
        // he cannot be hurt; a monster that comes close sends him running (the avoid goal)
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity e) {
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean startRiding(Entity vehicle, boolean force, boolean sendEvent) {
        return false;
    }

    @Override
    public boolean canTeleport(Level from, Level to) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return false;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        // removed by /kill: nothing he carried is lost
        for (int i = 0; i < bag.getSlots(); i++) {
            ItemStack s = bag.getStackInSlot(i);
            if (!s.isEmpty()) spawnAtLocation((net.minecraft.server.level.ServerLevel) level(), s.copy());
            bag.setStackInSlot(i, ItemStack.EMPTY);
        }
        for (ItemStack b : carried) spawnAtLocation((net.minecraft.server.level.ServerLevel) level(), b.copy());
        carried.clear();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) AquaristArrival.onGone(level, this); // /kill: the records know at once
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && level() instanceof ServerLevel level) AquaristArrival.onGone(level, this);
        super.remove(reason);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name == null || name.getString().isBlank() ? Component.literal(NAMES[Math.floorMod(getUUID().hashCode(), NAMES.length)]) : name);
    }

    // ------------------------------------------------------------------ save

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        CompoundTag tag = new CompoundTag();
        super.addAdditionalSaveData(output);
        CompoundTag a = new CompoundTag();
        a.putString("phase", phase().name().toLowerCase(java.util.Locale.ROOT));
        a.putInt("variant", variant());
        if (employer != null) NbtCompat.putUUID(a, "employer", employer);
        a.putString("employer_name", employerName());
        if (offerUntil != Long.MAX_VALUE) a.putLong("offer_until", offerUntil);
        a.putBoolean("offer_said", offerSaid);
        if (workplace != null) a.put("workplace", NbtCompat.writeBlockPos(workplace));
        if (bed != null) a.put("bed", NbtCompat.writeBlockPos(bed));
        CompoundTag lv = new CompoundTag();
        for (AquaristTask t : AquaristTask.ALL) lv.putInt(t.id(), levels[t.ordinal()]);
        a.put("tasks", lv);
        ListTag rl = new ListTag();
        for (RearingTank r : rearing) {
            CompoundTag e = new CompoundTag();
            e.putUUID("id", r.tank);
            ListTag sp = new ListTag();
            for (String k : r.species) sp.add(StringTag.valueOf(k));
            e.put("species", sp);
            rl.add(e);
        }
        if (!rl.isEmpty()) a.put("rearing_tanks", rl);
        ListTag off = new ListTag();
        for (var e : tankOff.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", e.getKey());
            c.putInt("off", e.getValue());
            off.add(c);
        }
        if (!off.isEmpty()) a.put("tank_off", off);
        if (chest != null) a.put("chest", NbtUtils.writeBlockPos(chest));
        a.put("bag", bag.serializeNBT(registryAccess()));
        if (waterMb > 0) {
            a.putInt("water_mb", waterMb);
            a.putBoolean("tap_water", tapWater);
            if (waterValues != null) WaterValues.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, waterValues).result().ifPresent(v -> a.put("water_values", v));
            if (waterFor != null) NbtCompat.putUUID(a, "water_for", waterFor);
        }
        if (!carried.isEmpty()) {
            ListTag l = new ListTag();
            for (ItemStack s : carried) l.add(NbtCompat.saveStack(registryAccess(), s));
            a.put("carried", l);
            if (carriedFrom != null) a.putUUID("carried_from", carriedFrom);
            if (carriedTo != null) a.putUUID("carried_to", carriedTo);
        }
        if (lastWageDay != Long.MIN_VALUE) a.putLong("wage_day", lastWageDay);
        a.putInt("unpaid_days", unpaidDays);
        a.putInt("homeless_days", homelessDays);
        if (leaveAt >= 0) a.putLong("leave_at", leaveAt);
        CompoundTag rep = new CompoundTag();
        for (var e : reported.entrySet()) {
            ListTag l = new ListTag();
            for (String w : e.getValue()) l.add(StringTag.valueOf(w));
            rep.put(e.getKey().toString(), l);
        }
        a.put("reported", rep);
        CompoundTag com = new CompoundTag();
        for (var e : complained.entrySet()) com.put(e.getKey(), LongTag.valueOf(e.getValue()));
        a.put("complained", com);
        tag.put("bettacraft_aquarist", a);
        NbtCompat.write(output, tag);
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        CompoundTag tag = NbtCompat.read(input);

        super.readAdditionalSaveData(input);
        CompoundTag a = tag.getCompoundOrEmpty("bettacraft_aquarist");
        Phase p = Phase.OFFERING;
        for (Phase x : Phase.values()) if (x.name().toLowerCase(java.util.Locale.ROOT).equals(a.getStringOr("phase", ""))) p = x;
        setPhase(p);
        setVariant(a.getIntOr("variant", 0));
        employer = a.hasUUID("employer") ? a.getUUID("employer") : null;
        entityData.set(DATA_EMPLOYER, a.getStringOr("employer_name", ""));
        offerUntil = a.contains("offer_until") ? a.getLongOr("offer_until", 0L) : Long.MAX_VALUE;
        offerSaid = a.getBooleanOr("offer_said", false);
        workplace = NbtCompat.readBlockPos(a, "workplace").orElse(null);
        bed = NbtCompat.readBlockPos(a, "bed").orElse(null);
        CompoundTag lv = a.getCompoundOrEmpty("tasks");
        for (AquaristTask t : AquaristTask.ALL) if (lv.contains(t.id())) setTaskLevel(t, lv.getInt(t.id()));
        rearing.clear();
        if (a.contains("rearing_tanks")) { // M66
            for (Tag t : a.getListOrEmpty("rearing_tanks")) {
                CompoundTag e = (CompoundTag) t;
                if (!e.hasUUID("id")) continue;
                RearingTank r = new RearingTank(e.getUUID("id"));
                for (Tag k : e.getListOrEmpty("species")) r.species.add(k.getAsString());
                rearing.add(r);
            }
        } else if (a.hasUUID("rearing")) { // before M66: the one rearing tank stays his rearing tank
            rearing.add(new RearingTank(a.getUUID("rearing")));
        }
        tankOff.clear();
        for (Tag t : a.getListOrEmpty("tank_off")) {
            CompoundTag e = (CompoundTag) t;
            if (e.hasUUID("id") && e.getIntOr("off", 0) != 0) tankOff.put(e.getUUID("id"), e.getIntOr("off", 0));
        }
        chest = NbtCompat.readBlockPos(a, "chest").orElse(null);
        if (a.contains("bag")) bag.deserializeNBT(registryAccess(), a.getCompoundOrEmpty("bag"));
        waterMb = Math.max(0, a.getIntOr("water_mb", 0));
        tapWater = a.getBooleanOr("tap_water", false);
        waterValues = a.contains("water_values") ? WaterValues.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, a.get("water_values")).result().orElse(null) : null;
        waterFor = NbtCompat.hasUUID(a, "water_for") ? NbtCompat.getUUID(a, "water_for") : null;
        carried.clear();
        for (Tag t : a.getListOrEmpty("carried")) ItemStack.parse(registryAccess(), t).ifPresent(carried::add);
        carriedFrom = a.hasUUID("carried_from") ? a.getUUID("carried_from") : null;
        carriedTo = a.hasUUID("carried_to") ? a.getUUID("carried_to") : null;
        lastWageDay = a.contains("wage_day") ? a.getLongOr("wage_day", 0L) : Long.MIN_VALUE;
        unpaidDays = a.getIntOr("unpaid_days", 0);
        homelessDays = a.getIntOr("homeless_days", 0);
        leaveAt = a.contains("leave_at") ? a.getLongOr("leave_at", 0L) : -1;
        reported.clear();
        CompoundTag rep = a.getCompoundOrEmpty("reported");
        for (String k : rep.keySet()) {
            try {
                Set<String> s = new HashSet<>();
                for (Tag t : rep.getListOrEmpty(k)) s.add(NbtCompat.asString(t));
                reported.put(UUID.fromString(k), s);
            } catch (IllegalArgumentException ignored) {
                // a broken key: forget it
            }
        }
        complained.clear();
        CompoundTag com = a.getCompoundOrEmpty("complained");
        for (String k : com.keySet()) complained.put(k, com.getLongOr(k, 0L));
    }
}
