package org.overengineer.bettacraft.fabric.transfer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.platform.Platform;
import org.overengineer.bettacraft.platform.energy.IEnergyStorage;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.platform.fluid.IFluidHandlerItem;
import org.overengineer.bettacraft.platform.item.IItemHandler;

/**
 * M52: the common item / fluid / energy handlers (simulate or execute, millibuckets, FE) and Fabric's transfer API
 * (transactions, droplets: 81 per millibucket) and Team Reborn Energy (1 E = 1 FE), both ways.
 *
 * <p>Ours offered to Fabric: inside a transaction every insert / extract is simulated against the handler with what the
 * transaction already moved (a ledger), and the total is executed once when the outermost transaction commits; an aborted
 * transaction leaves the handler untouched. Theirs used by us: one transaction per call, committed unless simulating.
 */
public final class FabricTransfer {
    public static final long DROPLETS_PER_MB = 81;

    private FabricTransfer() {}

    // ------------------------------------------------------------------ lookups of other mods' blocks

    public static @Nullable IItemHandler items(Level level, BlockPos pos, @Nullable Direction side) {
        Storage<ItemVariant> s = ItemStorage.SIDED.find(level, pos, side);
        if (s == null) return null;
        if (s instanceof OurItems o) return o.handler;
        return new TheirItems(s);
    }

    /** Another mod's fluid storage as our fluid handler. */
    public static IFluidHandler fromFabric(Storage<FluidVariant> s) {
        return s instanceof OurFluids o ? o.handler : new TheirFluids(s);
    }

    public static @Nullable IFluidHandler fluids(Level level, BlockPos pos, @Nullable Direction side) {
        Storage<FluidVariant> s = FluidStorage.SIDED.find(level, pos, side);
        if (s == null) return null;
        if (s instanceof OurFluids o) return o.handler;
        return new TheirFluids(s);
    }

    public static @Nullable IEnergyStorage energy(Level level, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity be, @Nullable Direction side) {
        team.reborn.energy.api.EnergyStorage s = state == null ? team.reborn.energy.api.EnergyStorage.SIDED.find(level, pos, side)
                : team.reborn.energy.api.EnergyStorage.SIDED.find(level, pos, state, be, side);
        if (s == null) return null;
        if (s instanceof OurEnergy o) return o.energy;
        return new TheirEnergy(s);
    }

    // ------------------------------------------------------------------ conversions

    public static FluidVariant variant(FluidStack s) {
        return s.isEmpty() ? FluidVariant.blank() : FluidVariant.of(s.getFluid(), s.getComponentsPatch());
    }

    public static FluidStack stack(FluidVariant v, long droplets) {
        int mb = (int) Math.min(Integer.MAX_VALUE, droplets / DROPLETS_PER_MB);
        return v.isBlank() || mb <= 0 ? FluidStack.EMPTY : new FluidStack(v.getFluid().builtInRegistryHolder(), mb, v.getComponentsPatch());
    }

    static TransactionContext.Result commit(Transaction t, boolean simulate) {
        if (simulate) t.abort();
        else t.commit();
        return null;
    }

    static Transaction open() {
        TransactionContext current = Transaction.getCurrentUnsafe();
        return current == null ? Transaction.openOuter() : current.openNested();
    }

    // ------------------------------------------------------------------ ours as Fabric storages

    public static @Nullable Storage<ItemVariant> toFabric(@Nullable IItemHandler h) {
        if (h == null) return null;
        if (h instanceof TheirItems t) return t.storage;
        return CACHE_ITEMS.computeIfAbsent(h, OurItems::new);
    }

    public static @Nullable Storage<FluidVariant> toFabric(@Nullable IFluidHandler h) {
        if (h == null) return null;
        if (h instanceof TheirFluids t) return t.storage;
        return new OurFluids(h);
    }

    public static @Nullable team.reborn.energy.api.EnergyStorage toFabric(@Nullable IEnergyStorage e) {
        if (e == null) return null;
        if (e instanceof TheirEnergy t) return t.storage;
        return new OurEnergy(e);
    }

    private static final Map<IItemHandler, OurItems> CACHE_ITEMS = java.util.Collections.synchronizedMap(new WeakHashMap<>());

    /** What moved inside a transaction (in, out, the resource); executed once when the outermost transaction commits. */
    static final class Moved {
        long in, out;
        Object resource;

        Moved copy() {
            Moved m = new Moved();
            m.in = in;
            m.out = out;
            m.resource = resource;
            return m;
        }
    }

    /** The ledger of one handler: a snapshot participant, so nested and aborted transactions are handled by Fabric. */
    abstract static class Ledger<K> extends net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant<Map<K, Moved>> {
        private Map<K, Moved> moved = new HashMap<>();

        Moved entry(TransactionContext tx, K key) {
            updateSnapshots(tx);
            return moved.computeIfAbsent(key, k -> new Moved());
        }

        @Override
        protected Map<K, Moved> createSnapshot() {
            Map<K, Moved> copy = new HashMap<>();
            moved.forEach((k, v) -> copy.put(k, v.copy()));
            return copy;
        }

        @Override
        protected void readSnapshot(Map<K, Moved> snapshot) {
            moved = snapshot;
        }

        @Override
        protected void onFinalCommit() {
            Map<K, Moved> done = moved;
            moved = new HashMap<>();
            done.forEach(this::apply);
        }

        abstract void apply(K key, Moved moved);
    }

    /** Our item handler as a slotted Fabric storage. */
    static final class OurItems implements SlottedStorage<ItemVariant> {
        final IItemHandler handler;
        final List<Slot> slots = new ArrayList<>();
        final Ledger<Integer> ledger = new Ledger<>() {
            @Override
            void apply(Integer slot, Moved m) {
                if (m.out > 0) handler.extractItem(slot, (int) m.out, false);
                if (m.in > 0 && m.resource instanceof ItemVariant v) handler.insertItem(slot, v.toStack((int) m.in), false);
            }
        };

        OurItems(IItemHandler handler) {
            this.handler = handler;
        }

        void ensure() {
            int n = handler.getSlots();
            while (slots.size() < n) slots.add(new Slot(slots.size()));
            while (slots.size() > n) slots.remove(slots.size() - 1);
        }

        @Override
        public int getSlotCount() {
            ensure();
            return slots.size();
        }

        @Override
        public SingleSlotStorage<ItemVariant> getSlot(int slot) {
            ensure();
            return slots.get(slot);
        }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext tx) {
            ensure();
            long done = 0;
            // stacks of the same item first, then empty slots
            for (int pass = 0; pass < 2 && done < maxAmount; pass++) {
                for (Slot s : slots) {
                    ItemStack in = handler.getStackInSlot(s.index);
                    if (pass == 0 ? in.isEmpty() || !resource.matches(in) : !in.isEmpty()) continue;
                    done += s.insert(resource, maxAmount - done, tx);
                    if (done >= maxAmount) break;
                }
            }
            return done;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext tx) {
            ensure();
            long done = 0;
            for (Slot s : slots) {
                done += s.extract(resource, maxAmount - done, tx);
                if (done >= maxAmount) break;
            }
            return done;
        }

        @Override
        public Iterator<StorageView<ItemVariant>> iterator() {
            ensure();
            return new ArrayList<StorageView<ItemVariant>>(slots).iterator();
        }

        final class Slot implements SingleSlotStorage<ItemVariant> {
            final int index;

            Slot(int index) {
                this.index = index;
            }

            @Override
            public long insert(ItemVariant resource, long maxAmount, TransactionContext tx) {
                if (resource.isBlank() || maxAmount <= 0) return 0;
                Moved e = ledger.entry(tx, index);
                if (e.resource != null && !e.resource.equals(resource)) return 0;
                int want = (int) Math.min(Integer.MAX_VALUE - e.in, e.in + maxAmount);
                ItemStack rest = handler.insertItem(index, resource.toStack(want), true);
                long accepted = Math.max(0, want - rest.getCount() - e.in);
                if (accepted > 0) {
                    e.in += accepted;
                    e.resource = resource;
                }
                return accepted;
            }

            @Override
            public long extract(ItemVariant resource, long maxAmount, TransactionContext tx) {
                ItemStack in = handler.getStackInSlot(index);
                if (in.isEmpty() || !resource.matches(in) || maxAmount <= 0) return 0;
                Moved e = ledger.entry(tx, index);
                int want = (int) Math.min(Integer.MAX_VALUE - e.out, e.out + maxAmount);
                ItemStack out = handler.extractItem(index, want, true);
                long got = Math.max(0, out.getCount() - e.out);
                e.out += got;
                return got;
            }

            @Override
            public boolean isResourceBlank() {
                return handler.getStackInSlot(index).isEmpty();
            }

            @Override
            public ItemVariant getResource() {
                return ItemVariant.of(handler.getStackInSlot(index));
            }

            @Override
            public long getAmount() {
                return handler.getStackInSlot(index).getCount();
            }

            @Override
            public long getCapacity() {
                ItemStack in = handler.getStackInSlot(index);
                return Math.min(handler.getSlotLimit(index), in.isEmpty() ? 64 : in.getMaxStackSize());
            }
        }
    }

    /** Our fluid handler as a Fabric storage (droplets). */
    static final class OurFluids implements Storage<FluidVariant> {
        final IFluidHandler handler;
        final Ledger<FluidVariant> fills = new Ledger<>() {
            @Override
            void apply(FluidVariant v, Moved m) {
                if (m.out > 0) handler.drain(stack(v, m.out * DROPLETS_PER_MB), IFluidHandler.FluidAction.EXECUTE);
                if (m.in > 0) handler.fill(stack(v, m.in * DROPLETS_PER_MB), IFluidHandler.FluidAction.EXECUTE);
            }
        };

        OurFluids(IFluidHandler handler) {
            this.handler = handler;
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext tx) {
            long mb = maxAmount / DROPLETS_PER_MB;
            if (resource.isBlank() || mb <= 0) return 0;
            Moved e = fills.entry(tx, resource);
            int want = (int) Math.min(Integer.MAX_VALUE, e.in + mb);
            int fits = handler.fill(stack(resource, want * DROPLETS_PER_MB), IFluidHandler.FluidAction.SIMULATE);
            long accepted = Math.max(0, fits - e.in);
            e.in += accepted;
            return accepted * DROPLETS_PER_MB;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext tx) {
            long mb = maxAmount / DROPLETS_PER_MB;
            if (resource.isBlank() || mb <= 0) return 0;
            Moved e = fills.entry(tx, resource);
            int want = (int) Math.min(Integer.MAX_VALUE, e.out + mb);
            FluidStack out = handler.drain(stack(resource, want * DROPLETS_PER_MB), IFluidHandler.FluidAction.SIMULATE);
            long got = Math.max(0, out.getAmount() - e.out);
            e.out += got;
            return got * DROPLETS_PER_MB;
        }

        @Override
        public Iterator<StorageView<FluidVariant>> iterator() {
            List<StorageView<FluidVariant>> views = new ArrayList<>();
            for (int i = 0; i < handler.getTanks(); i++) {
                final int tank = i;
                views.add(new StorageView<>() {
                    @Override
                    public long extract(FluidVariant resource, long maxAmount, TransactionContext tx) {
                        return OurFluids.this.extract(resource, maxAmount, tx);
                    }

                    @Override
                    public boolean isResourceBlank() {
                        return handler.getFluidInTank(tank).isEmpty();
                    }

                    @Override
                    public FluidVariant getResource() {
                        return variant(handler.getFluidInTank(tank));
                    }

                    @Override
                    public long getAmount() {
                        return handler.getFluidInTank(tank).getAmount() * DROPLETS_PER_MB;
                    }

                    @Override
                    public long getCapacity() {
                        return handler.getTankCapacity(tank) * DROPLETS_PER_MB;
                    }
                });
            }
            return views.iterator();
        }
    }

    /** Our energy buffer as Team Reborn Energy. */
    static final class OurEnergy implements team.reborn.energy.api.EnergyStorage {
        final IEnergyStorage energy;
        final Ledger<Boolean> ledger = new Ledger<>() {
            @Override
            void apply(Boolean key, Moved m) {
                if (m.out > 0) energy.extractEnergy((int) m.out, false);
                if (m.in > 0) energy.receiveEnergy((int) m.in, false);
            }
        };

        OurEnergy(IEnergyStorage energy) {
            this.energy = energy;
        }

        @Override
        public boolean supportsInsertion() {
            return energy.canReceive();
        }

        @Override
        public boolean supportsExtraction() {
            return energy.canExtract();
        }

        @Override
        public long insert(long maxAmount, TransactionContext tx) {
            Moved e = ledger.entry(tx, Boolean.TRUE);
            int want = (int) Math.min(Integer.MAX_VALUE, e.in + maxAmount);
            long got = Math.max(0, energy.receiveEnergy(want, true) - e.in);
            e.in += got;
            return got;
        }

        @Override
        public long extract(long maxAmount, TransactionContext tx) {
            Moved e = ledger.entry(tx, Boolean.TRUE);
            int want = (int) Math.min(Integer.MAX_VALUE, e.out + maxAmount);
            long got = Math.max(0, energy.extractEnergy(want, true) - e.out);
            e.out += got;
            return got;
        }

        @Override
        public long getAmount() {
            return energy.getEnergyStored();
        }

        @Override
        public long getCapacity() {
            return energy.getMaxEnergyStored();
        }
    }

    // ------------------------------------------------------------------ theirs as our handlers

    static final class TheirItems implements IItemHandler {
        final Storage<ItemVariant> storage;

        TheirItems(Storage<ItemVariant> storage) {
            this.storage = storage;
        }

        List<StorageView<ItemVariant>> views() {
            List<StorageView<ItemVariant>> out = new ArrayList<>();
            for (StorageView<ItemVariant> v : storage) out.add(v);
            return out;
        }

        @Override
        public int getSlots() {
            return storage instanceof SlottedStorage<ItemVariant> s ? s.getSlotCount() : Math.max(1, views().size());
        }

        StorageView<ItemVariant> view(int slot) {
            if (storage instanceof SlottedStorage<ItemVariant> s) return slot < s.getSlotCount() ? s.getSlot(slot) : null;
            List<StorageView<ItemVariant>> v = views();
            return slot < v.size() ? v.get(slot) : null;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            StorageView<ItemVariant> v = view(slot);
            return v == null || v.isResourceBlank() ? ItemStack.EMPTY : v.getResource().toStack((int) Math.min(Integer.MAX_VALUE, v.getAmount()));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            Storage<ItemVariant> target = storage instanceof SlottedStorage<ItemVariant> s && slot < s.getSlotCount() ? s.getSlot(slot) : storage;
            try (Transaction t = open()) {
                long n = target.insert(ItemVariant.of(stack), stack.getCount(), t);
                commit(t, simulate);
                return n >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - (int) n);
            }
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            StorageView<ItemVariant> v = view(slot);
            if (v == null || v.isResourceBlank() || amount <= 0) return ItemStack.EMPTY;
            ItemVariant res = v.getResource();
            try (Transaction t = open()) {
                long n = v.extract(res, amount, t);
                commit(t, simulate);
                return n <= 0 ? ItemStack.EMPTY : res.toStack((int) n);
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            StorageView<ItemVariant> v = view(slot);
            return v == null ? 64 : (int) Math.min(Integer.MAX_VALUE, v.getCapacity());
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return storage.supportsInsertion();
        }
    }

    static final class TheirFluids implements IFluidHandler {
        final Storage<FluidVariant> storage;

        TheirFluids(Storage<FluidVariant> storage) {
            this.storage = storage;
        }

        List<StorageView<FluidVariant>> views() {
            List<StorageView<FluidVariant>> out = new ArrayList<>();
            for (StorageView<FluidVariant> v : storage) out.add(v);
            return out;
        }

        @Override
        public int getTanks() {
            return Math.max(1, views().size());
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            List<StorageView<FluidVariant>> v = views();
            return tank < v.size() && !v.get(tank).isResourceBlank() ? stack(v.get(tank).getResource(), v.get(tank).getAmount()) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            List<StorageView<FluidVariant>> v = views();
            return tank < v.size() ? (int) Math.min(Integer.MAX_VALUE, v.get(tank).getCapacity() / DROPLETS_PER_MB) : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return storage.supportsInsertion();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return 0;
            try (Transaction t = open()) {
                long n = storage.insert(variant(resource), resource.getAmount() * DROPLETS_PER_MB, t);
                commit(t, action.simulate());
                return (int) (n / DROPLETS_PER_MB);
            }
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return FluidStack.EMPTY;
            FluidVariant v = variant(resource);
            try (Transaction t = open()) {
                long n = storage.extract(v, resource.getAmount() * DROPLETS_PER_MB, t);
                commit(t, action.simulate());
                return stack(v, n);
            }
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidVariant v = StorageUtil.findStoredResource(storage);
            return v == null ? FluidStack.EMPTY : drain(stack(v, (long) maxDrain * DROPLETS_PER_MB), action);
        }
    }

    static final class TheirEnergy implements IEnergyStorage {
        final team.reborn.energy.api.EnergyStorage storage;

        TheirEnergy(team.reborn.energy.api.EnergyStorage storage) {
            this.storage = storage;
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            try (Transaction t = open()) {
                long n = storage.insert(toReceive, t);
                commit(t, simulate);
                return (int) n;
            }
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            try (Transaction t = open()) {
                long n = storage.extract(toExtract, t);
                commit(t, simulate);
                return (int) n;
            }
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, storage.getAmount());
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, storage.getCapacity());
        }

        @Override
        public boolean canExtract() {
            return storage.supportsExtraction();
        }

        @Override
        public boolean canReceive() {
            return storage.supportsInsertion();
        }
    }

    // ------------------------------------------------------------------ fluid container items

    /** The fluid of an item (a bucket, another mod's tank item), working on a copy of the stack. */
    public static @Nullable IFluidHandlerItem fluidItem(ItemStack stack) {
        if (stack.isEmpty()) return null;
        StackSlot slot = new StackSlot(stack.copyWithCount(1));
        ContainerItemContext ctx = ContainerItemContext.ofSingleSlot(slot);
        Storage<FluidVariant> s = ctx.find(FluidStorage.ITEM);
        if (s == null) return null;
        TheirFluids f = new TheirFluids(s);
        return new IFluidHandlerItem() {
            @Override
            public ItemStack getContainer() {
                return slot.current().copy();
            }

            @Override
            public int getTanks() {
                return f.getTanks();
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return f.getFluidInTank(tank);
            }

            @Override
            public int getTankCapacity(int tank) {
                return f.getTankCapacity(tank);
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack s2) {
                return f.isFluidValid(tank, s2);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return f.fill(resource, action);
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return f.drain(resource, action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return f.drain(maxDrain, action);
            }
        };
    }

    public static boolean interact(Player player, InteractionHand hand, Level level, BlockPos pos, @Nullable Direction side) {
        Storage<FluidVariant> s = FluidStorage.SIDED.find(level, pos, side);
        return s != null && FluidStorageUtil.interactWithFluidStorage(s, player, hand);
    }

    public static boolean interact(Player player, InteractionHand hand, IFluidHandler handler) {
        Storage<FluidVariant> s = toFabric(handler);
        return s != null && FluidStorageUtil.interactWithFluidStorage(s, player, hand);
    }

    public static Platform.FluidActionResult emptyContainer(ItemStack container, IFluidHandler dest, int maxAmount, boolean execute) {
        return move(container, dest, maxAmount, execute, true);
    }

    public static Platform.FluidActionResult fillContainer(ItemStack container, IFluidHandler source, int maxAmount, boolean execute) {
        return move(container, source, maxAmount, execute, false);
    }

    private static Platform.FluidActionResult move(ItemStack container, IFluidHandler other, int maxAmount, boolean execute, boolean fromItem) {
        if (container.isEmpty()) return Platform.FluidActionResult.FAILURE;
        StackSlot slot = new StackSlot(container.copyWithCount(1));
        ContainerItemContext ctx = ContainerItemContext.ofSingleSlot(slot);
        Storage<FluidVariant> item = ctx.find(FluidStorage.ITEM);
        Storage<FluidVariant> tank = toFabric(other);
        if (item == null || tank == null) return Platform.FluidActionResult.FAILURE;
        try (Transaction t = open()) {
            long moved = fromItem ? StorageUtil.move(item, tank, v -> true, maxAmount * DROPLETS_PER_MB, t)
                    : StorageUtil.move(tank, item, v -> true, maxAmount * DROPLETS_PER_MB, t);
            if (moved <= 0) return Platform.FluidActionResult.FAILURE;
            ItemStack result = slot.current().copy();
            commit(t, !execute);
            return new Platform.FluidActionResult(true, result);
        }
    }

    /** A single item slot for the fluid item context (the container changes in it when it is filled or emptied). */
    static final class StackSlot extends net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage {
        private ItemStack stack;

        StackSlot(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        protected ItemStack getStack() {
            return stack;
        }

        @Override
        protected void setStack(ItemStack stack) {
            this.stack = stack;
        }

        ItemStack current() {
            return stack;
        }
    }
}
