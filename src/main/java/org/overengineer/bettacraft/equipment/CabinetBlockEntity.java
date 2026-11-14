package org.overengineer.bettacraft.equipment;

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
 * filter item) and nine storage slots (hoppers / pipes reach the storage slots only).
 */
public class CabinetBlockEntity extends BlockEntity implements MenuProvider, FilterHost {
    public static final ResourceLocation DEFAULT_WOOD = ResourceLocation.withDefaultNamespace("oak_planks");
    public static final ModelProperty<BlockState> WOOD_STATE = new ModelProperty<>();
    public static final int FILTER_SLOT = 0, STORAGE = 9, SLOTS = 1 + STORAGE;
    /** Client-side cabinets currently loaded with a filter (bubbles and sound; never touched on the server). */
    public static final Set<CabinetBlockEntity> CLIENT_FILTERS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private ResourceLocation wood = DEFAULT_WOOD;
    /** M22.4: the side the filter hose goes up (server: as set; client: the effective side the server sent). */
    private OutletSide outlet = OutletSide.BACK;
    /** Client copy: whether the filter slot is filled (the slot itself is not synced outside the menu). */
    private boolean clientHasFilter;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot != FILTER_SLOT || stack.is(ModItems.EXTERNAL_FILTER.get());
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == FILTER_SLOT ? 1 : 64;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot == FILTER_SLOT && level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    };
    private final RangedWrapper storage = new RangedWrapper(items, 1, SLOTS);

    public CabinetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CABINET.get(), pos, state);
    }

    public ItemStackHandler items() {
        return items;
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
     * M22.4: the side the hose of the filter goes up. On the server BACK while {@code features.cabinetOutletSide} is off (the
     * setting is kept); on the client the side the server sent.
     */
    public OutletSide outletSide() {
        if (level != null && level.isClientSide) return outlet;
        return org.overengineer.bettacraft.config.CommonConfig.cabinetOutletSide() ? outlet : OutletSide.BACK;
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

    public ItemStack filterItem() {
        return items.getStackInSlot(FILTER_SLOT);
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
        tag.put("items", items.serializeNBT(registries));
        saveM23(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag raw, HolderLookup.Provider registries) {
        super.loadAdditional(raw, registries);
        CompoundTag tag = Schemas.CABINET.migrate(raw);
        ResourceLocation w = ResourceLocation.tryParse(tag.getString("wood"));
        ResourceLocation old = wood;
        wood = w == null ? DEFAULT_WOOD : w;
        outlet = OutletSide.byId(tag.getString("outlet")); // M22.4: old cabinets keep the back wall
        if (tag.contains("items")) {
            items.deserializeNBT(registries, tag.getCompound("items"));
            if (items.getSlots() != SLOTS) items.setSize(SLOTS);
        }
        if (tag.contains("has_filter")) clientHasFilter = tag.getBoolean("has_filter");
        loadM23(tag);
        if (level != null && level.isClientSide) afterClientUpdate(old);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        Schemas.CABINET.stamp(tag);
        tag.putString("wood", wood.toString());
        tag.putBoolean("has_filter", !filterItem().isEmpty());
        tag.putString("outlet", outletSide().id()); // M22.4: the effective side
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
        outlet = OutletSide.byId(tag.getString("outlet"));
        loadM23(tag);
        afterClientUpdate(old);
    }

    private void afterClientUpdate(ResourceLocation oldWood) {
        if (level == null || !level.isClientSide) return;
        if (clientHasFilter && !isRemoved()) CLIENT_FILTERS.add(this);
        else CLIENT_FILTERS.remove(this);
        if (!oldWood.equals(wood)) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide && clientHasFilter) CLIENT_FILTERS.add(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        CLIENT_FILTERS.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        CLIENT_FILTERS.remove(this);
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
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(ModDataComponents.WOOD.get(), wood);
        if (outlet != OutletSide.BACK) components.set(ModDataComponents.OUTLET_SIDE.get(), outlet.id()); // M22.4: only when set
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("wood");
        tag.remove("outlet");
    }
}
