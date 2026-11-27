package org.overengineer.bettacraft.equipment;

import net.minecraft.core.component.DataComponentGetter;
import org.overengineer.bettacraft.util.NbtCompat;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.item.ItemStackHandler;
import org.overengineer.bettacraft.data.schema.Schemas;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;

/** External filter block next to a tank (PLAN 6.2): two heater slots; the heaters stay in the item when it is broken. */
public class ExternalFilterBlockEntity extends BlockEntity implements MenuProvider, FilterHost, org.overengineer.bettacraft.winding.Windable, org.overengineer.bettacraft.platform.BlockEntityLifecycle {
    /** Client-side filters currently loaded (bubbles and sound; never touched on the server). */
    public static final java.util.Set<ExternalFilterBlockEntity> CLIENT_LOADED =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    private final ItemStackHandler heaters = new ItemStackHandler(FilterHeaters.SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return FilterHeaters.isHeater(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    /** M66.5: the render box of the hose (client), see {@link FilterHost#renderBoxCache}. */
    private final org.overengineer.bettacraft.util.RenderBoxCache renderBox = new org.overengineer.bettacraft.util.RenderBoxCache();

    @Override
    public org.overengineer.bettacraft.util.RenderBoxCache renderBoxCache() {
        return renderBox;
    }

    public ExternalFilterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXTERNAL_FILTER.get(), pos, state);
    }

    public ItemStackHandler heaters() {
        return heaters;
    }

    /** M37: the spring of the filter. M67: only an older world's winding is left in it (the tank's store takes it); the crank charges the tank's store. */
    private final org.overengineer.bettacraft.winding.WindSpring spring =
            org.overengineer.bettacraft.winding.WindSpring.of(org.overengineer.bettacraft.winding.Winding.Device.EXTERNAL_FILTER, this::setChanged);

    @Override
    public org.overengineer.bettacraft.winding.WindSpring windSpring() {
        return spring;
    }

    @Override
    public org.overengineer.bettacraft.winding.Winding.Device windDevice() {
        return org.overengineer.bettacraft.winding.Winding.Device.EXTERNAL_FILTER;
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
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
    }

    private void saveM23(CompoundTag tag) {
        if (regulator != 10) tag.putInt("regulator", regulator);
        if (intakeSponge) tag.putBoolean("intake_sponge", true);
    }

    private void loadM23(CompoundTag tag) {
        regulator = tag.contains("regulator") ? Math.max(0, Math.min(10, tag.getIntOr("regulator", 0))) : 10;
        intakeSponge = tag.getBooleanOr("intake_sponge", false);
    }

    // ------------------------------------------------------------------ FilterHost

    @Override
    public boolean hasFilter() {
        return true;
    }

    @Override
    public ItemStack heater(int slot) {
        return heaters.getStackInSlot(slot);
    }

    @Override
    public boolean insertHeater(ItemStack heater) {
        for (int i = 0; i < FilterHeaters.SLOTS; i++) {
            if (heaters.getStackInSlot(i).isEmpty()) {
                heaters.setStackInSlot(i, heater.copyWithCount(1));
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public BlockPos tankBlockPos() {
        BlockState s = getBlockState();
        if (!(s.getBlock() instanceof ExternalFilterBlock)) return null;
        if (link != null) {
            // M62: the tank chosen with a sneak-click; one below the tank only while features.equipment.filterBelowTank is on
            if (link.getY() != worldPosition.getY() && !Features.enabled(Feature.FILTER_BELOW_TANK)) return null;
            return link;
        }
        return worldPosition.relative(s.getValue(ExternalFilterBlock.FACING));
    }

    // ------------------------------------------------------------------ M62 the chosen tank

    /**
     * M62: the tank block (a wall block beside the filter or one or two blocks above that) whose tank the player chose with a
     * sneak-click; null: the filter works with the tank right in front of it, as before M62 (also every filter of an older
     * world: nothing to migrate).
     */
    @Nullable
    private BlockPos link;

    @Nullable
    public BlockPos link() {
        return link;
    }

    /** Sets the chosen tank block (null: back to the tank in front); the client draws the hose there. */
    public void setLink(@Nullable BlockPos wall) {
        BlockPos w = wall == null ? null : wall.immutable();
        if (java.util.Objects.equals(w, link)) return;
        link = w;
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
    }

    /** M62: how many blocks the filter stands below the tank block it feeds (0: beside it). */
    public int below() {
        BlockPos t = tankBlockPos();
        return t == null ? 0 : t.getY() - worldPosition.getY();
    }

    public void onLoad() {
        org.overengineer.bettacraft.platform.Platform.get().requestModelDataUpdate(this); // NeoForge's default onLoad
        if (level != null && level.isClientSide()) CLIENT_LOADED.add(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        CLIENT_LOADED.remove(this);
    }

    public void onChunkUnloaded() {
        CLIENT_LOADED.remove(this);
    }

    // ------------------------------------------------------------------ menu

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.bettacraft.external_filter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ExternalFilterMenu(id, inv, this);
    }

    // ------------------------------------------------------------------ NBT / components

    @Override
    protected void saveAdditional(ValueOutput output) {
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = NbtCompat.registries(this);
        super.saveAdditional(output);
        Schemas.EXTERNAL_FILTER.stamp(tag);
        tag.put("heaters", heaters.serializeNBT(registries));
        saveM23(tag);
        spring.save(tag, "spring");
        if (link != null) tag.putLong("link", link.asLong()); // M62
        NbtCompat.write(output, tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        CompoundTag raw = NbtCompat.read(input);
        HolderLookup.Provider registries = input.lookup();
        super.loadAdditional(input);
        CompoundTag tag = Schemas.EXTERNAL_FILTER.migrate(raw);
        if (tag.contains("heaters")) {
            heaters.deserializeNBT(registries, tag.getCompoundOrEmpty("heaters"));
            if (heaters.getSlots() != FilterHeaters.SLOTS) heaters.setSize(FilterHeaters.SLOTS);
        }
        loadM23(tag);
        spring.load(tag, "spring");
        link = tag.contains("link") ? BlockPos.of(tag.getLongOr("link", 0L)) : null; // M62
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveM23(tag);
        tag.putInt("regulator", regulator());
        if (link != null) tag.putLong("link", link.asLong()); // M62: the client draws the hose to the chosen tank
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadM23(tag);
        link = tag.contains("link") ? BlockPos.of(tag.getLongOr("link", 0L)) : null; // M62
    }

    /** M52b: NeoForge 21.11 hands the client update over as a value input (the same handling as the compound). */
    public void handleUpdateTag(net.minecraft.world.level.storage.ValueInput input) {
        handleUpdateTag(org.overengineer.bettacraft.util.NbtCompat.read(input), org.overengineer.bettacraft.util.NbtCompat.registries(this));
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        ItemContainerContents c = input.get(DataComponents.CONTAINER);
        if (c != null) {
            NonNullList<ItemStack> list = NonNullList.withSize(FilterHeaters.SLOTS, ItemStack.EMPTY);
            c.copyInto(list);
            for (int i = 0; i < FilterHeaters.SLOTS; i++) heaters.setStackInSlot(i, FilterHeaters.isHeater(list.get(i)) ? list.get(i).copyWithCount(1) : ItemStack.EMPTY);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        List<ItemStack> list = new ArrayList<>();
        boolean any = false;
        for (int i = 0; i < FilterHeaters.SLOTS; i++) {
            list.add(heaters.getStackInSlot(i).copy());
            any |= !heaters.getStackInSlot(i).isEmpty();
        }
        if (any) components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        output.discard("heaters");
    }
}
