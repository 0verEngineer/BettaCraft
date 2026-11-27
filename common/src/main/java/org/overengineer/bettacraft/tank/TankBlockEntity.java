package org.overengineer.bettacraft.tank;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.overengineer.bettacraft.util.NbtCompat;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.data.schema.Schemas;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.registry.ModDataComponents;

/**
 * Block entity of every tank block: knows its glass and its controller. The controller instance additionally owns
 * the {@link TankData}. (PLAN calls the controller "TankControllerBE"; we use one BE type for all tank blocks and the
 * controller role is the non-null {@link #tank} field, which avoids swapping block entities on merge/split.)
 */
public class TankBlockEntity extends BlockEntity implements org.overengineer.bettacraft.platform.model.ModelDataSource, org.overengineer.bettacraft.platform.BlockEntityLifecycle, org.overengineer.bettacraft.platform.ClientSyncedBlockEntity {
    public static final Identifier DEFAULT_GLASS = Identifier.withDefaultNamespace("glass");
    public static final org.overengineer.bettacraft.platform.model.ModelKey<BlockState> GLASS_STATE = new org.overengineer.bettacraft.platform.model.ModelKey<>("glass_state");
    /** M11: textured (connected glass texture) or clean (clear glass, edge lines only). */
    public static final org.overengineer.bettacraft.platform.model.ModelKey<TankStyle> STYLE = new org.overengineer.bettacraft.platform.model.ModelKey<>("style");
    /**
     * M32 part b: the water is part of the block model (Create draws a contraption's blocks by their models only, without
     * the fluid pass that draws the tank water in the world).
     */
    public static final org.overengineer.bettacraft.platform.model.ModelKey<Boolean> WATER_IN_MODEL = new org.overengineer.bettacraft.platform.model.ModelKey<>("water_in_model");
    /** M50: the water of this travelling tank block is drawn sloshing by its controller's renderer, not by the model or fluid. */
    public static final org.overengineer.bettacraft.platform.model.ModelKey<Boolean> WATER_DYNAMIC = new org.overengineer.bettacraft.platform.model.ModelKey<>("water_dynamic");

    /** M53c: whether the first server tick checked that the interior region still belongs to this tank (not saved). */
    public boolean regionChecked;
    /** Client-side controllers currently loaded (used by the miniature renderer; never touched on the server). */
    public static final Set<TankBlockEntity> CLIENT_CONTROLLERS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Transient server state of a controller (not saved).
    /** Reconciliations an inhabitant id has been missing (see TankInhabitants#reconcile). */
    final it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap<java.util.UUID> missing = new it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap<>();
    private boolean crowded;
    long lastCareTick = -1;
    /** Headroom the fill states were last computed with (transient): recomputed after loading or a config change. */
    int appliedHeadroom = -1;
    /** M17: ticks after loading until this block checks once that its controller still knows it (orphan repair). */
    int orphanCheck = 40;
    /** M17: last critter / floating plant payloads (sent only when they change). */
    public final SyncGate critterSync = new SyncGate(), plantSync = new SyncGate();
    /** M17: outer faces of the tank for the equipment scan (outside position and direction), for {@link #equipmentFacesOf}. */
    long[] equipmentFaces = new long[0];
    byte[] equipmentFaceDirs = new byte[0];
    TankData equipmentFacesOf;
    /** M17: what the clients last got of the nest (see TankBreeding#syncNest). */
    long nestSignature = Long.MIN_VALUE;
    /** Whether power was needed when the energy capabilities were last announced (transient, null = not yet). */
    Boolean appliedPower;

    /** Recompute the fill states of the tank on its next tick (as after loading). */
    public void markFillStatesStale() {
        appliedHeadroom = -1;
    }
    /** Log only once per chase / fight (transient). */
    boolean chaseLogged, fightLogged;
    /** M13: care steps in which a female hid in the aquarium plants from the male (GameTests, menu). */
    public int femaleHidSteps;
    /** M14: the "frogs have no air" log line was written (transient). */
    public boolean frogAirLogged;
    /** M19: log once per episode (male bettas chasing tankmates, a clown loach eating snails, a shoal that is too small). */
    public boolean tankmateChaseLogged, tankmateSnailLogged;
    public final java.util.Set<String> tankmateLogged = new java.util.HashSet<>();
    /** M29 part d: game time of the last log line per kind (big crayfish, axolotls), not saved. */
    public final java.util.Map<String, Long> logTimes = new java.util.HashMap<>();

    /** M29 part d: whether a log line of this kind may be written now (at most one per {@code ticks}); remembers the time. */
    public boolean logDue(String kind, long now, long ticks) {
        Long last = logTimes.get(kind);
        if (last != null && now - last < ticks && now >= last) return false;
        logTimes.put(kind, now);
        return true;
    }
    /** M14: signature of the algae stages the clients have (transient; a change sends the tank view). */
    public long algaeSignature = Long.MIN_VALUE;
    /** M14: game time of the last death log line per species (transient: several deaths in a row are one line). */
    public final java.util.Map<org.overengineer.bettacraft.critters.CritterSpecies, Long> critterDeathLog = new java.util.EnumMap<>(
            org.overengineer.bettacraft.critters.CritterSpecies.class);
    /** Attached external filters and cabinets under the tank (transient cache, see TankEquipment). */
    java.util.List<BlockPos> filterCache = java.util.List.of(), cabinetCache = java.util.List.of(), skimmerCache = java.util.List.of();
    long equipmentScanTime = Long.MIN_VALUE;
    /**
     * M9 power state of the equipment (transient, recomputed every care step by {@link TankPower}): whether heaters /
     * coolers, filters and lights have the FE they need (always true while the config does not require power).
     */
    boolean heatPowered = true, filterPowered = true, lightPowered = true;
    /**
     * M37 (transient, set by {@link TankPower#step}): the external filters that clean on their own spring while the tank's
     * FE is short, those whose heaters / coolers run on it, and the devices the springs drove in the last care step.
     */
    final java.util.Set<org.overengineer.bettacraft.equipment.FilterHost> springFilters = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()),
            springHeat = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    double springUnits;

    /** M37: devices the wound springs drove in the last care step (0: none or all on FE). */
    public double springUnits() { return springUnits; }

    public java.util.Set<org.overengineer.bettacraft.equipment.FilterHost> springHeat() { return springHeat; }
    /** M23: air pumps next to the tank (positions), whether they have FE, and the light factor (dimmer x daylight curve) last applied to the lights. */
    java.util.List<BlockPos> airPumpCache = java.util.List.of();
    /** M25: CO2 sources (bio CO2 bottles, gas cylinders) that serve this tank. */
    java.util.List<BlockPos> co2Cache = java.util.List.of();
    /** M25: bubbles per second the CO2 sources give now and the CO2 the water moves to (set by the care step, for the menu). */
    public double co2Bubbles, co2Target = org.overengineer.bettacraft.chem.WaterChemistry.CO2_AIR;
    boolean airPowered = true;
    public double appliedLightFactor = -1;
    /** M23: what the last care step found (menu, overlays): oxygen exchange per day, consumption, equilibrium, air outlets driven. */
    public double o2Exchange, o2Use, o2Target;
    public int airOutlets, airDriven;
    /** FE per tick the equipment needed in the last care step (for the menu and the info overlays). */
    int lastDemand;
    /** M41 (transient, set by {@link TankDrive#step}): FE per tick Create's rotation gave, its inputs, whether one is overstressed. */
    int driveFe, driveInputs;
    boolean driveOverstressed;
    /** M41: positions of the drive adapters on this tank ({@link TankEquipment}). */
    java.util.List<BlockPos> driveCache = java.util.List.of();
    /** M31: heat mats and misters at the tank (transient cache, see TankEquipment), whether the misters have FE. */
    java.util.List<BlockPos> heatMatCache = java.util.List.of(), misterCache = java.util.List.of();
    boolean misterPowered = true;
    /** M31: the land scan of the climate step (transient): when and at which water amount, land and plant share. */
    public long climateScanTime = Long.MIN_VALUE, climateScanWater = Long.MIN_VALUE;
    public double climateLand = -1, climatePlants;
    /** M33.1: the walkable tops of the sampled interior columns (last land scan, not saved) and the water limit worked out from them. */
    @javax.annotation.Nullable public int[] climateTops;
    public int[] limitTops;
    public double limitShare = Double.NaN;
    public long limitMb = -1;
    /** M33.1: game time of the last tank type change from the menu (at most one every {@code WaterLimit.SET_TICKS}). */
    public long modeSetTime = Long.MIN_VALUE;
    /** M31: what the last climate step found (FE demand, menu): heat mats working, their duty, misters spraying, lamps lit. */
    public double climateMats;
    public int climateMisting, climateMistWant, climateLamps;
    /** M31: the climate the clients last got, and when. */
    public float syncedAir = Float.NaN, syncedRh = Float.NaN, syncedUv = Float.NaN;
    public long climateSyncTime = Long.MIN_VALUE;
    /** M31: days each registered foreign terrarium animal has borne a harmful problem (transient). */
    public final java.util.Map<java.util.UUID, Double> terrariumHarm = new java.util.HashMap<>();

    /**
     * M32 part b: where this block entity was last saved ({@code at} in its data). A block entity loaded from data saved at
     * another position was moved as data (a Create contraption put it down, Sable took it into a sub-level, another mod
     * moved it): {@link TankMoves} then moves the whole tank along. Server only; transient.
     */
    @Nullable
    BlockPos movedFrom;
    /** M32 part b: the rotation the mover reported (Create's StructureTransform), or null (it is worked out from the blocks). */
    @Nullable
    int[] moveRotation;
    int moveWait;
    /** Where the data this block entity was loaded from had been saved (null: never loaded from data). */
    @Nullable
    private BlockPos savedAt;
    /**
     * M32 part b: the animals packed into the tank's data for a trip (entity data with id, saved like the fish bag does),
     * released again where the tank is put down; on the client the same list draws them while the tank moves.
     */
    net.minecraft.nbt.ListTag stowed = new net.minecraft.nbt.ListTag();
    /** M32 part b (client): a copy of the controller drawn on a Create contraption (its own local coordinates). */
    public boolean contraptionView;
    /** M32 part b (client): the swimming animals drawn while travelling, and when the client last checked for a sub-level. */
    public Object travelCache;
    /** M50 (client): the water of this travelling tank is drawn sloshing, its blocks were told to leave theirs out. */
    public boolean sloshMeshed;
    /** M50 (client): decides whether a tank block's water is drawn sloshing by the travelling renderer (set by the client). */
    public static volatile java.util.function.Predicate<TankBlockEntity> DYNAMIC_WATER = be -> false;
    public long travelCheckAt = Long.MIN_VALUE;
    public boolean travelCheck;

    public net.minecraft.nbt.ListTag stowed() { return stowed; }

    /** M50: the summary of a trip on a Create contraption ({@link TravelCare#TRIP_KEY}), written into the log on arrival. Transient. */
    @Nullable
    CompoundTag tripLog;

    /** M32 part b (client): Create's copy of a tank block on a contraption (its level is the contraption's, not the world). */
    public boolean onContraption() {
        return contraptionView || level != null && level.isClientSide() && org.overengineer.bettacraft.client.tank.TankClientLevels.notTheWorld(level);
    }

    @Nullable
    public BlockPos movedFrom() { return movedFrom; }

    @Nullable
    public int[] moveRotation() { return moveRotation == null ? null : moveRotation.clone(); }

    /**
     * A mover put this block down from data (Create's block entity transformer): it is handled as moved even when it came
     * back to the very place it left (a bearing turned once round), with the rotation the mover reported (null = find out).
     */
    /** M42 part b: drops the tank data a placed item brought (a copy of a packed tank): the block forms a tank like a new one. */
    void forgetPackedData() {
        movedFrom = null;
        savedAt = null;
        moveRotation = null;
        stowed = new net.minecraft.nbt.ListTag();
        setController(null, null);
    }

    public void markMoved(@Nullable int[] rotation) {
        this.moveRotation = TankMoveMap.valid(rotation) ? rotation.clone() : null;
        if (savedAt != null) this.movedFrom = savedAt;
    }

    private Identifier glass = DEFAULT_GLASS;
    private TankStyle style = TankStyle.TEXTURED;
    @Nullable
    private BlockPos controllerPos;
    @Nullable
    private TankData tank;

    public TankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TANK.get(), pos, state);
    }

    // ------------------------------------------------------------------ accessors

    public Identifier getGlassId() {
        return glass;
    }

    public void setGlassId(Identifier glass) {
        this.glass = glass;
        setChanged();
    }

    public TankStyle getStyle() {
        return style;
    }

    public void setStyle(TankStyle style) {
        this.style = style;
        setChanged();
    }

    /** Same glass and same style: only such tank blocks connect (M11: the variants never mix). */
    public boolean sameMaterial(TankBlockEntity other) {
        return glass.equals(other.glass) && style == other.style;
    }

    /** The glass block (falls back to vanilla glass for unknown ids; the id itself is preserved). */
    public BlockState getGlassState() {
        Block b = BuiltInRegistries.BLOCK.getValue(glass);
        return b == Blocks.AIR ? Blocks.GLASS.defaultBlockState() : b.defaultBlockState();
    }

    @Nullable
    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public boolean isController() {
        return tank != null && worldPosition.equals(controllerPos);
    }

    @Nullable
    public TankData getTank() {
        return tank;
    }

    void setController(@Nullable BlockPos controllerPos, @Nullable TankData tank) {
        this.controllerPos = controllerPos == null ? null : controllerPos.immutable();
        this.tank = tank;
        setChanged();
    }

    /** Call after mutating {@link #getTank()} on the server: saves and syncs the summary to clients. */
    public void markTankChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public boolean heatPowered() { return heatPowered; }
    public boolean filterPowered() { return filterPowered; }
    public boolean lightPowered() { return lightPowered; }
    public boolean airPowered() { return airPowered; }
    public boolean misterPowered() { return misterPowered; } // M31
    public int lastDemand() { return lastDemand; }
    public int driveFe() { return driveFe; }
    public int driveInputs() { return driveInputs; }
    public boolean driveOverstressed() { return driveOverstressed; }

    public boolean isCrowded() {
        return crowded;
    }

    void setCrowded(boolean crowded) {
        this.crowded = crowded;
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void loadAdditional(ValueInput input) {
        CompoundTag rawTag = NbtCompat.read(input);
        HolderLookup.Provider registries = input.lookup();
        super.loadAdditional(input);
        CompoundTag tag = Schemas.TANK_BLOCK_ENTITY.migrate(rawTag);
        Identifier g = Identifier.tryParse(tag.getStringOr("glass", ""));
        glass = g == null ? DEFAULT_GLASS : g;
        style = TankStyle.byId(tag.getStringOr("style", ""));
        int[] c = tag.getIntArray("controller").orElse(new int[0]);
        controllerPos = c.length == 3 ? new BlockPos(c[0], c[1], c[2]) : null;
        // M32 part b: saved somewhere else = moved as data (the controller position is the old one until TankMoves ran)
        movedFrom = null;
        savedAt = null;
        if (NbtCompat.has(tag, "at", Tag.TAG_LONG)) {
            BlockPos at = BlockPos.of(tag.getLongOr("at", 0L));
            savedAt = at;
            if (!at.equals(worldPosition)) movedFrom = at;
        }
        stowed = tag.getListOrEmpty("stowed").copy();
        tripLog = NbtCompat.has(tag, TravelCare.TRIP_KEY, Tag.TAG_COMPOUND) ? tag.getCompoundOrEmpty(TravelCare.TRIP_KEY).copy() : null; // M50
        if (NbtCompat.has(tag, "tank", Tag.TAG_COMPOUND)) {
            tank = TankData.load(tag.getCompoundOrEmpty("tank"));
        } else if (NbtCompat.has(tag, "tank_view", Tag.TAG_COMPOUND)) {
            TankData old = tank;
            tank = TankData.loadClientView(tag.getCompoundOrEmpty("tank_view"));
            // M12: floating plants glide to their new place instead of jumping
            if (old != null && level != null) org.overengineer.bettacraft.tank.plants.FloatingPlantSim.adopt(old.plants(), tank.plants(),
                    level.getGameTime(), org.overengineer.bettacraft.tank.plants.TankPlants.SYNC_TICKS);
            // M14: the small animals too
            if (old != null && level != null) org.overengineer.bettacraft.critters.CritterSync.adopt(old.critters(), tank.critters(),
                    level.getGameTime(), org.overengineer.bettacraft.tank.plants.TankPlants.SYNC_TICKS);
            // M32 part b: a controller's view at another position than its controller = Create's copy on a contraption
            // (block entities there sit at contraption-local positions): its data moves to the local position, so the
            // sign, nest, plants and small animals are drawn on the contraption
            if (controllerPos != null && !controllerPos.equals(worldPosition)) {
                tank.relocate(TankMoveMap.translation(controllerPos, worldPosition), null);
                controllerPos = worldPosition.immutable();
                contraptionView = true;
            }
        } else {
            tank = null;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = NbtCompat.registries(this);
        super.saveAdditional(output);
        writeCommon(tag);
        tag.putLong("at", worldPosition.asLong()); // M32 part b
        if (!stowed.isEmpty()) tag.put("stowed", stowed.copy());
        // a moved controller keeps its data until TankMoves ran (its controller position is still the old one)
        if (tank != null && (isController() || movedFrom != null && movedFrom.equals(controllerPos))) {
            tag.put("tank", tank.save());
        }
        NbtCompat.write(output, tag);
    }

    private void writeCommon(CompoundTag tag) {
        Schemas.TANK_BLOCK_ENTITY.stamp(tag);
        tag.putString("glass", glass.toString());
        tag.putString("style", style.getSerializedName());
        if (controllerPos != null) {
            tag.putIntArray("controller", new int[]{controllerPos.getX(), controllerPos.getY(), controllerPos.getZ()});
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeCommon(tag);
        if (tank != null && isController()) {
            tag.put("tank_view", tank.saveClientView());
            if (!stowed.isEmpty()) tag.put("stowed", stowed.copy()); // M32 part b: drawn while the tank travels
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        Identifier oldGlass = glass;
        TankStyle oldStyle = style;
        boolean wasController = isController();
        if (!pkt.getTag().isEmpty()) loadWithComponents(NbtCompat.input(registries, pkt.getTag())); // NeoForge's default
        afterClientUpdate(oldGlass, oldStyle, wasController);
    }

    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        Identifier oldGlass = glass;
        TankStyle oldStyle = style;
        boolean wasController = isController();
        loadWithComponents(NbtCompat.input(registries, tag)); // NeoForge's default
        afterClientUpdate(oldGlass, oldStyle, wasController);
    }

    /** M52b: NeoForge 21.11 hands the client update over as a value input (the same handling as the compound). */
    public void handleUpdateTag(net.minecraft.world.level.storage.ValueInput input) {
        handleUpdateTag(org.overengineer.bettacraft.util.NbtCompat.read(input), org.overengineer.bettacraft.util.NbtCompat.registries(this));
    }

    /** M52b: NeoForge 21.11 hands the update packet's data over as a value input. */
    public void onDataPacket(Connection net, net.minecraft.world.level.storage.ValueInput input) {
        net.minecraft.nbt.CompoundTag tag = org.overengineer.bettacraft.util.NbtCompat.read(input);
        if (!tag.isEmpty()) handleUpdateTag(tag, org.overengineer.bettacraft.util.NbtCompat.registries(this));
    }

    private void afterClientUpdate(Identifier oldGlass, TankStyle oldStyle, boolean wasController) {
        if (level != null && level.isClientSide()) {
            updateClientControllerSet();
            // M66: only the controller's renderer draws (TankSignRenderer.shouldRenderOffScreen); the chunk mesher sorts the block
            // entities into "always" and "when the section is visible" when it builds the section, so a new or former controller
            // gets its section built again
            boolean looks = !oldGlass.equals(glass) || oldStyle != style;
            if (looks) org.overengineer.bettacraft.platform.Platform.get().requestModelDataUpdate(this);
            if (looks || wasController != isController()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
            // M66 v6: the algae film is part of the chunk mesh; the sections whose film changed are meshed again
            org.overengineer.bettacraft.client.tank.AlgaeFilm.update(this);
        }
    }

    // ------------------------------------------------------------------ components (item <-> block)

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        Identifier g = input.get(ModDataComponents.GLASS.get());
        if (g != null) {
            glass = g;
        }
        TankStyle st = input.get(ModDataComponents.TANK_STYLE.get());
        if (st != null) {
            style = st;
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(ModDataComponents.GLASS.get(), glass);
        components.set(ModDataComponents.TANK_STYLE.get(), style);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        output.discard("glass");
        output.discard("style");
    }

    // ------------------------------------------------------------------ client

    @Override
    public org.overengineer.bettacraft.platform.model.ModelValues modelValues() {
        var b = org.overengineer.bettacraft.platform.model.ModelValues.builder().with(GLASS_STATE, getGlassState()).with(STYLE, style);
        if (DYNAMIC_WATER.test(this)) b.with(WATER_DYNAMIC, true); // M50
        else if (onContraption()) b.with(WATER_IN_MODEL, true);
        return b.build();
    }

    public void onLoad() {
        org.overengineer.bettacraft.platform.Platform.get().requestModelDataUpdate(this); // NeoForge's default onLoad
        if (level != null && level.isClientSide()) {
            updateClientControllerSet();
            org.overengineer.bettacraft.client.tank.AlgaeFilm.update(this); // M66 v6
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        CLIENT_CONTROLLERS.remove(this);
        if (level != null && level.isClientSide()) org.overengineer.bettacraft.client.tank.AlgaeFilm.remove(this); // M66 v6
    }

    public void onChunkUnloaded() {
        CLIENT_CONTROLLERS.remove(this);
        if (level != null && level.isClientSide()) org.overengineer.bettacraft.client.tank.AlgaeFilm.remove(this); // M66 v6
    }

    private void updateClientControllerSet() {
        if (isController() && !isRemoved() && !contraptionView) { // M32 part b: Create's copies are drawn by their renderer
            CLIENT_CONTROLLERS.add(this);
        } else {
            CLIENT_CONTROLLERS.remove(this);
        }
    }
}
