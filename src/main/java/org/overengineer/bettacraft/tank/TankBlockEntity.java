package org.overengineer.bettacraft.tank;

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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.overengineer.bettacraft.data.schema.Schemas;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.registry.ModDataComponents;

/**
 * Block entity of every tank block: knows its glass and its controller. The controller instance additionally owns
 * the {@link TankData}. (PLAN calls the controller "TankControllerBE"; we use one BE type for all tank blocks and the
 * controller role is the non-null {@link #tank} field, which avoids swapping block entities on merge/split.)
 */
public class TankBlockEntity extends BlockEntity {
    public static final ResourceLocation DEFAULT_GLASS = ResourceLocation.withDefaultNamespace("glass");
    public static final ModelProperty<BlockState> GLASS_STATE = new ModelProperty<>();
    /** M11: textured (connected glass texture) or clean (clear glass, edge lines only). */
    public static final ModelProperty<TankStyle> STYLE = new ModelProperty<>();

    /** Client-side controllers currently loaded (used by the miniature renderer; never touched on the server). */
    public static final Set<TankBlockEntity> CLIENT_CONTROLLERS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Transient server state of a controller (not saved).
    /** Reconciliations an inhabitant id has been missing (see TankInhabitants#reconcile). */
    final it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap<java.util.UUID> missing = new it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap<>();
    private boolean crowded;
    long lastCareTick = -1;
    /** Headroom the fill states were last computed with (transient): recomputed after loading or a config change. */
    int appliedHeadroom = -1;
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
    /** Attached external filters and cabinets under the tank (transient cache, see TankEquipment). */
    java.util.List<BlockPos> filterCache = java.util.List.of(), cabinetCache = java.util.List.of();
    long equipmentScanTime = Long.MIN_VALUE;
    /**
     * M9 power state of the equipment (transient, recomputed every care step by {@link TankPower}): whether heaters /
     * coolers, filters and lights have the FE they need (always true while the config does not require power).
     */
    boolean heatPowered = true, filterPowered = true, lightPowered = true;
    /** FE per tick the equipment needed in the last care step (for the menu and the info overlays). */
    int lastDemand;

    private ResourceLocation glass = DEFAULT_GLASS;
    private TankStyle style = TankStyle.TEXTURED;
    @Nullable
    private BlockPos controllerPos;
    @Nullable
    private TankData tank;

    public TankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TANK.get(), pos, state);
    }

    // ------------------------------------------------------------------ accessors

    public ResourceLocation getGlassId() {
        return glass;
    }

    public void setGlassId(ResourceLocation glass) {
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
        Block b = BuiltInRegistries.BLOCK.get(glass);
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
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public boolean heatPowered() { return heatPowered; }
    public boolean filterPowered() { return filterPowered; }
    public boolean lightPowered() { return lightPowered; }
    public int lastDemand() { return lastDemand; }

    public boolean isCrowded() {
        return crowded;
    }

    void setCrowded(boolean crowded) {
        this.crowded = crowded;
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void loadAdditional(CompoundTag rawTag, HolderLookup.Provider registries) {
        super.loadAdditional(rawTag, registries);
        CompoundTag tag = Schemas.TANK_BLOCK_ENTITY.migrate(rawTag);
        ResourceLocation g = ResourceLocation.tryParse(tag.getString("glass"));
        glass = g == null ? DEFAULT_GLASS : g;
        style = TankStyle.byId(tag.getString("style"));
        int[] c = tag.getIntArray("controller");
        controllerPos = c.length == 3 ? new BlockPos(c[0], c[1], c[2]) : null;
        if (tag.contains("tank", Tag.TAG_COMPOUND)) {
            tank = TankData.load(tag.getCompound("tank"));
        } else if (tag.contains("tank_view", Tag.TAG_COMPOUND)) {
            TankData old = tank;
            tank = TankData.loadClientView(tag.getCompound("tank_view"));
            // M12: floating plants glide to their new place instead of jumping
            if (old != null && level != null) org.overengineer.bettacraft.tank.plants.FloatingPlantSim.adopt(old.plants(), tank.plants(),
                    level.getGameTime(), org.overengineer.bettacraft.tank.plants.TankPlants.SYNC_TICKS);
        } else {
            tank = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeCommon(tag);
        if (tank != null && isController()) {
            tag.put("tank", tank.save());
        }
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
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        ResourceLocation oldGlass = glass;
        TankStyle oldStyle = style;
        super.onDataPacket(net, pkt, registries);
        afterClientUpdate(oldGlass, oldStyle);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        ResourceLocation oldGlass = glass;
        TankStyle oldStyle = style;
        super.handleUpdateTag(tag, registries);
        afterClientUpdate(oldGlass, oldStyle);
    }

    private void afterClientUpdate(ResourceLocation oldGlass, TankStyle oldStyle) {
        if (level != null && level.isClientSide) {
            updateClientControllerSet();
            if (!oldGlass.equals(glass) || oldStyle != style) {
                requestModelDataUpdate();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
            }
        }
    }

    // ------------------------------------------------------------------ components (item <-> block)

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        ResourceLocation g = input.get(ModDataComponents.GLASS.get());
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
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("glass");
        tag.remove("style");
    }

    // ------------------------------------------------------------------ client

    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(GLASS_STATE, getGlassState()).with(STYLE, style).build();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            updateClientControllerSet();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        CLIENT_CONTROLLERS.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        CLIENT_CONTROLLERS.remove(this);
    }

    private void updateClientControllerSet() {
        if (isController() && !isRemoved()) {
            CLIENT_CONTROLLERS.add(this);
        } else {
            CLIENT_CONTROLLERS.remove(this);
        }
    }
}
