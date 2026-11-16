package org.overengineer.bettacraft.equipment;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import org.overengineer.bettacraft.data.schema.Schemas;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModItems;

/**
 * Cabinet block entity: the planks it is made of, slot 0 for an external filter (the filter's heaters live in the
 * filter item), nine storage slots (hoppers / pipes reach the storage slots only) and (M29.1) slot 10 for an air pump
 * that drives the air stones and sponge filters of the tank above (its hose goes up with the filter hose).
 */
public class CabinetBlockEntity extends BlockEntity implements MenuProvider, FilterHost, AirSource, org.overengineer.bettacraft.winding.Windable {
    public static final ResourceLocation DEFAULT_WOOD = ResourceLocation.withDefaultNamespace("oak_planks");
    public static final ModelProperty<BlockState> WOOD_STATE = new ModelProperty<>();
    public static final int FILTER_SLOT = 0, STORAGE = 9, AIR_PUMP_SLOT = 1 + STORAGE, SLOTS = 2 + STORAGE;
    /** Client-side cabinets currently loaded with a filter (bubbles and sound; never touched on the server). */
    public static final Set<CabinetBlockEntity> CLIENT_FILTERS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    /** M29.1: client-side cabinets currently loaded with an air pump (hum; never touched on the server). */
    public static final Set<CabinetBlockEntity> CLIENT_AIR_PUMPS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private ResourceLocation wood = DEFAULT_WOOD;
    /** M22.4: the side the filter hose goes up (server: as set; client: the effective side the server sent). */
    private OutletSide outlet = OutletSide.BACK;
    /** M41: the side that takes a Create shaft (server: as set; client: the effective side the server sent). */
    private DriveSide drive = DriveSide.NONE;
    /** Client copy: whether the filter slot is filled (the slot itself is not synced outside the menu). */
    private boolean clientHasFilter;
    /** M29.1: client copy of whether the air pump slot is filled. */
    private boolean clientHasAirPump;
    /** M29.1: air stones and sponge filters the air pump in the cabinet drives right now (server: set by the care step; synced). */
    private int airDriven;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == AIR_PUMP_SLOT) return stack.is(ModItems.AIR_PUMP.get()); // M29.1
            return slot != FILTER_SLOT || stack.is(ModItems.EXTERNAL_FILTER.get());
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == FILTER_SLOT || slot == AIR_PUMP_SLOT ? 1 : 64;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot == AIR_PUMP_SLOT && getStackInSlot(AIR_PUMP_SLOT).isEmpty()) airDriven = 0;
            if ((slot == FILTER_SLOT || slot == AIR_PUMP_SLOT) && level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
                if (slot == AIR_PUMP_SLOT) {
                    var tank = FilterHost.tankOf(level, tankBlockPos());
                    if (tank != null) org.overengineer.bettacraft.tank.TankEquipment.invalidate(tank);
                }
            }
        }
    };
    private final RangedWrapper storage = new RangedWrapper(items, 1, 1 + STORAGE);

    public CabinetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CABINET.get(), pos, state);
    }

    public ItemStackHandler items() {
        return items;
    }

    /**
     * M37: the spring the crank winds: it stores the winding and gives it to every device of the tank above that needs FE
     * while the tank's FE is short ({@link org.overengineer.bettacraft.tank.TankPower}), the more devices the faster.
     */
    private final org.overengineer.bettacraft.winding.WindSpring spring =
            org.overengineer.bettacraft.winding.WindSpring.of(org.overengineer.bettacraft.winding.Winding.Device.CABINET, this::setChanged);

    @Override
    public org.overengineer.bettacraft.winding.WindSpring windSpring() {
        return spring;
    }

    @Override
    public org.overengineer.bettacraft.winding.Winding.Device windDevice() {
        return org.overengineer.bettacraft.winding.Winding.Device.CABINET;
    }

    /** Storage slots only (capability for hoppers and pipes). */
    public RangedWrapper storage() {
        return storage;
    }

    public ResourceLocation woodId() {
        return wood;
    }

    public void setWood(ResourceLocation wood) {
        this.wood = wood;
        setChanged();
    }

    /** The planks block (oak for unknown ids; the id itself is kept). */
    public BlockState woodState() {
        Block b = BuiltInRegistries.BLOCK.get(wood);
        return b == Blocks.AIR ? Blocks.OAK_PLANKS.defaultBlockState() : b.defaultBlockState();
    }

    /**
     * M22.4: the side the hose of the filter goes up. On the server BACK while {@code features.equipment.cabinetOutletSide} is off (the
     * setting is kept); on the client the side the server sent.
     */
    public OutletSide outletSide() {
        if (level != null && level.isClientSide) return outlet;
        return Features.enabled(Feature.CABINET_OUTLET_SIDE) ? outlet : OutletSide.BACK;
    }

    /** M22.4: the stored setting (also while the feature is off). */
    public OutletSide outletSetting() {
        return outlet;
    }

    public void setOutletSide(OutletSide side) {
        if (side == null || side == outlet) return;
        outlet = side;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            var tank = FilterHost.tankOf(level, tankBlockPos());
            if (tank != null) org.overengineer.bettacraft.tank.TankCurrents.invalidate(tank);
        }
    }

    // ------------------------------------------------------------------ M41 Create shaft

    /**
     * M41: the side that takes a Create shaft. On the server NONE while Create is missing, {@code
     * features.compat.create.cabinetDrive} is off or {@code createDrive.cabinetSides} does not list the side (the setting is
     * kept); on the client the side the server sent.
     */
    public DriveSide driveSide() {
        if (level != null && level.isClientSide) return drive;
        if (drive == DriveSide.NONE || !org.overengineer.bettacraft.tank.TankDrive.cabinetsEnabled()
                || !org.overengineer.bettacraft.tank.TankDrive.sideAllowed(drive)) return DriveSide.NONE;
        return drive;
    }

    /** M41: the stored setting (also while the feature is off). */
    public DriveSide driveSetting() {
        return drive;
    }

    /** M41: the face of the cabinet the shaft goes into (pointing away from it), or null. */
    @Nullable
    public net.minecraft.core.Direction driveFace() {
        BlockState s = getBlockState();
        if (!s.hasProperty(CabinetBlock.FACING)) return null;
        return driveSide().face(s.getValue(CabinetBlock.FACING));
    }

    public void setDriveSide(DriveSide side) {
        if (side == null || side == drive) return;
        drive = side;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            org.overengineer.bettacraft.tank.TankDrive.Bridge b = org.overengineer.bettacraft.tank.TankDrive.BRIDGE;
            if (b != null) b.release(level, this); // the next care step charges the new side
        }
    }

    public ItemStack filterItem() {
        return items.getStackInSlot(FILTER_SLOT);
    }

    // ------------------------------------------------------------------ M29.1 air pump

    /** Whether an air pump sits in the cabinet (client: as the server sent it). */
    public boolean hasAirPump() {
        return level != null && level.isClientSide ? clientHasAirPump : !items.getStackInSlot(AIR_PUMP_SLOT).isEmpty();
    }

    @Override
    public double airShare() {
        return hasAirPump() ? 1.0 : 0.0;
    }

    @Override
    public boolean airRunning() {
        return hasAirPump() && airDriven > 0;
    }

    /** Outlets the air pump in the cabinet drives right now. */
    public int airDriven() {
        return airDriven;
    }

    @Override
    public void setAirDriven(int n) {
        if (n == airDriven) return;
        airDriven = n;
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    // ------------------------------------------------------------------ M23 regulator and intake sponge

    private int regulator = 10;
    private boolean intakeSponge;

    @Override
    public int regulator() {
        return regulator;
    }

    @Override
    public void setRegulator(int v) {
        v = Math.max(0, Math.min(10, v));
        if (v == regulator) return;
        regulator = v;
        m23Changed();
    }

    @Override
    public boolean intakeSponge() {
        return intakeSponge;
    }

    @Override
    public void setIntakeSponge(boolean on) {
        if (on == intakeSponge) return;
        intakeSponge = on;
        m23Changed();
    }

    private void m23Changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
    }

    private void saveM23(CompoundTag tag) {
        if (regulator != 10) tag.putInt("regulator", regulator);
        if (intakeSponge) tag.putBoolean("intake_sponge", true);
    }

    private void loadM23(CompoundTag tag) {
        regulator = tag.contains("regulator") ? Math.max(0, Math.min(10, tag.getInt("regulator"))) : 10;
        intakeSponge = tag.getBoolean("intake_sponge");
    }

    // ------------------------------------------------------------------ FilterHost

    @Override
    public boolean hasFilter() {
        return level != null && level.isClientSide ? clientHasFilter : !filterItem().isEmpty();
    }

    @Override
    public ItemStack heater(int slot) {
        ItemStack f = filterItem();
        return f.isEmpty() ? ItemStack.EMPTY : FilterHeaters.read(f).get(slot);
    }

    @Override
    public boolean insertHeater(ItemStack heater) {
        ItemStack f = filterItem();
        if (f.isEmpty()) return false;
        NonNullList<ItemStack> list = FilterHeaters.read(f);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).isEmpty()) {
                list.set(i, heater.copyWithCount(1));
                ItemStack nf = f.copy();
                FilterHeaters.write(nf, list);
                items.setStackInSlot(FILTER_SLOT, nf);
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public BlockPos tankBlockPos() {
        return worldPosition.above();
    }

    // ------------------------------------------------------------------ menu

    @Override
    public Component getDisplayName() {
        return CabinetBlockItem.nameFor(wood);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new CabinetMenu(id, inv, this);
    }

    // ------------------------------------------------------------------ NBT / sync / components

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        Schemas.CABINET.stamp(tag);
        tag.putString("wood", wood.toString());
        if (outlet != OutletSide.BACK) tag.putString("outlet", outlet.id()); // M22.4
        tag.putString("drive", drive.id()); // M41
        tag.put("items", items.serializeNBT(registries));
        if (airDriven > 0) tag.putInt("air_driven", airDriven); // M29.1
        saveM23(tag);
        spring.save(tag, "spring"); // M37
    }

    @Override
    protected void loadAdditional(CompoundTag raw, HolderLookup.Provider registries) {
        super.loadAdditional(raw, registries);
        CompoundTag tag = Schemas.CABINET.migrate(raw);
        ResourceLocation w = ResourceLocation.tryParse(tag.getString("wood"));
        ResourceLocation old = wood;
        wood = w == null ? DEFAULT_WOOD : w;
        outlet = OutletSide.byId(tag.getString("outlet")); // M22.4: old cabinets keep the back wall
        drive = DriveSide.byId(tag.getString("drive")); // M41: old cabinets take no shaft
        if (tag.contains("items")) {
            items.deserializeNBT(registries, tag.getCompound("items"));
            if (items.getSlots() != SLOTS) {
                // M29.1: cabinets of older versions have no air pump slot: keep their items, add the slot
                java.util.List<ItemStack> kept = new java.util.ArrayList<>();
                for (int i = 0; i < items.getSlots(); i++) kept.add(items.getStackInSlot(i));
                items.setSize(SLOTS);
                for (int i = 0; i < Math.min(kept.size(), SLOTS); i++) items.setStackInSlot(i, kept.get(i));
            }
        }
        airDriven = Math.max(0, tag.getInt("air_driven"));
        if (tag.contains("has_filter")) clientHasFilter = tag.getBoolean("has_filter");
        if (tag.contains("has_air_pump")) clientHasAirPump = tag.getBoolean("has_air_pump");
        loadM23(tag);
        if (level == null || !level.isClientSide) spring.load(tag, "spring"); // M37 (not in the client copy)
        if (level != null && level.isClientSide) afterClientUpdate(old);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        Schemas.CABINET.stamp(tag);
        tag.putString("wood", wood.toString());
        tag.putBoolean("has_filter", !filterItem().isEmpty());
        tag.putBoolean("has_air_pump", hasAirPump()); // M29.1
        tag.putInt("air_driven", airDriven);
        tag.putString("outlet", outletSide().id()); // M22.4: the effective side
        tag.putString("drive", driveSide().id()); // M41: the effective side
        saveM23(tag);
        tag.putInt("regulator", regulator());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        handleUpdateTag(pkt.getTag(), registries);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        ResourceLocation old = wood;
        ResourceLocation w = ResourceLocation.tryParse(tag.getString("wood"));
        wood = w == null ? DEFAULT_WOOD : w;
        clientHasFilter = tag.getBoolean("has_filter");
        clientHasAirPump = tag.getBoolean("has_air_pump"); // M29.1
        airDriven = Math.max(0, tag.getInt("air_driven"));
        outlet = OutletSide.byId(tag.getString("outlet"));
        drive = DriveSide.byId(tag.getString("drive")); // M41
        loadM23(tag);
        afterClientUpdate(old);
    }

    private void afterClientUpdate(ResourceLocation oldWood) {
        if (level == null || !level.isClientSide) return;
        if (clientHasFilter && !isRemoved()) CLIENT_FILTERS.add(this);
        else CLIENT_FILTERS.remove(this);
        if (clientHasAirPump && !isRemoved()) CLIENT_AIR_PUMPS.add(this);
        else CLIENT_AIR_PUMPS.remove(this);
        if (!oldWood.equals(wood)) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide && clientHasFilter) CLIENT_FILTERS.add(this);
        if (level != null && level.isClientSide && clientHasAirPump) CLIENT_AIR_PUMPS.add(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // M41: a shaft at this cabinet stops costing stress
        org.overengineer.bettacraft.tank.TankDrive.Bridge b = org.overengineer.bettacraft.tank.TankDrive.BRIDGE;
        if (b != null && level != null && !level.isClientSide) b.release(level, this);
        CLIENT_FILTERS.remove(this);
        CLIENT_AIR_PUMPS.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        CLIENT_FILTERS.remove(this);
        CLIENT_AIR_PUMPS.remove(this);
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(WOOD_STATE, woodState()).build();
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        ResourceLocation w = input.get(ModDataComponents.WOOD.get());
        if (w != null) wood = w;
        outlet = OutletSide.byId(input.get(ModDataComponents.OUTLET_SIDE.get())); // M22.4
        drive = DriveSide.byId(input.get(ModDataComponents.DRIVE_SIDE.get())); // M41
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(ModDataComponents.WOOD.get(), wood);
        if (outlet != OutletSide.BACK) components.set(ModDataComponents.OUTLET_SIDE.get(), outlet.id()); // M22.4: only when set
        if (drive != DriveSide.NONE) components.set(ModDataComponents.DRIVE_SIDE.get(), drive.id()); // M41: only when set
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("wood");
        tag.remove("outlet");
        tag.remove("drive");
    }
}
