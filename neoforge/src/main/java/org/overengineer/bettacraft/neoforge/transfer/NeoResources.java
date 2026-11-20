package org.overengineer.bettacraft.neoforge.transfer;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.platform.energy.IEnergyStorage;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.platform.item.IItemHandler;

/**
 * M52b: our item, fluid and energy handlers as NeoForge 21.9+ transfer handlers (the capabilities of 1.21.11 are
 * transactional). What moves inside a transaction is only simulated on our handler and noted in a journal; when the
 * root transaction commits it is done for real, an aborted transaction forgets it (the same design as the Fabric
 * module's storages). Looking up other mods' handlers goes the other way through NeoForge's own adapters
 * ({@code IItemHandler.of}), see {@link NeoTransfer}.
 */
public final class NeoResources {
    private NeoResources() {}

    /** What moved per slot or tank inside the open transactions; executed once at the root commit. */
    static final class Moved {
        int in, out;
        Object resource;

        Moved copy() {
            Moved m = new Moved();
            m.in = in;
            m.out = out;
            m.resource = resource;
            return m;
        }
    }

    abstract static class Ledger<K> extends SnapshotJournal<Map<K, Moved>> {
        private Map<K, Moved> moved = new HashMap<>();

        Moved entry(TransactionContext tx, K key) {
            updateSnapshots(tx);
            return moved.computeIfAbsent(key, k -> new Moved());
        }

        @Nullable Moved peek(K key) {
            return moved.get(key);
        }

        @Override
        protected Map<K, Moved> createSnapshot() {
            Map<K, Moved> copy = new HashMap<>();
            moved.forEach((k, v) -> copy.put(k, v.copy()));
            return copy;
        }

        @Override
        protected void revertToSnapshot(Map<K, Moved> snapshot) {
            moved = snapshot;
        }

        @Override
        protected void onRootCommit(Map<K, Moved> original) {
            Map<K, Moved> done = moved;
            moved = new HashMap<>();
            done.forEach(this::apply);
        }

        abstract void apply(K key, Moved moved);
    }

    // ------------------------------------------------------------------ items

    public static @Nullable ResourceHandler<ItemResource> items(@Nullable IItemHandler h) {
        if (h instanceof FromResources.Items theirs) return theirs.h(); // M52e: another mod's handler stays itself
        return h == null ? null : new Items(h);
    }

    static final class Items implements ResourceHandler<ItemResource> {
        final IItemHandler handler;
        final Ledger<Integer> ledger = new Ledger<>() {
            @Override
            void apply(Integer slot, Moved m) {
                if (m.out > 0) handler.extractItem(slot, m.out, false);
                if (m.in > 0 && m.resource instanceof ItemResource r) handler.insertItem(slot, r.toStack(m.in), false);
            }
        };

        Items(IItemHandler handler) {
            this.handler = handler;
        }

        @Override
        public int size() {
            return handler.getSlots();
        }

        @Override
        public ItemResource getResource(int index) {
            return ItemResource.of(handler.getStackInSlot(index));
        }

        @Override
        public long getAmountAsLong(int index) {
            return handler.getStackInSlot(index).getCount();
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            ItemStack in = handler.getStackInSlot(index);
            int max = resource.isEmpty() ? (in.isEmpty() ? 64 : in.getMaxStackSize()) : resource.getMaxStackSize();
            return Math.min(handler.getSlotLimit(index), max);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return handler.isItemValid(index, resource.toStack());
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext tx) {
            if (resource.isEmpty() || amount <= 0) return 0;
            Moved e = ledger.entry(tx, index);
            if (e.resource != null && !e.resource.equals(resource)) return 0;
            int want = (int) Math.min(Integer.MAX_VALUE, (long) e.in + amount);
            ItemStack rest = handler.insertItem(index, resource.toStack(want), true);
            int accepted = Math.max(0, want - rest.getCount() - e.in);
            if (accepted > 0) {
                e.in += accepted;
                e.resource = resource;
            }
            return accepted;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext tx) {
            ItemStack in = handler.getStackInSlot(index);
            if (in.isEmpty() || resource.isEmpty() || !resource.matches(in) || amount <= 0) return 0;
            Moved e = ledger.entry(tx, index);
            int want = (int) Math.min(Integer.MAX_VALUE, (long) e.out + amount);
            ItemStack out = handler.extractItem(index, want, true);
            int got = Math.max(0, out.getCount() - e.out);
            e.out += got;
            return got;
        }
    }

    // ------------------------------------------------------------------ fluids (mB both)

    public static @Nullable ResourceHandler<FluidResource> fluids(@Nullable IFluidHandler h) {
        if (h instanceof FromResources.Fluids theirs) return theirs.h(); // M52e: another mod's handler stays itself
        return h == null ? null : new Fluids(h);
    }

    static FluidStack ours(FluidResource r, int amount) {
        return NeoTransfer.fromNeo(r.toStack(amount));
    }

    static final class Fluids implements ResourceHandler<FluidResource> {
        final IFluidHandler handler;
        final Ledger<FluidResource> ledger = new Ledger<>() {
            @Override
            void apply(FluidResource r, Moved m) {
                if (m.out > 0) handler.drain(ours(r, m.out), IFluidHandler.FluidAction.EXECUTE);
                if (m.in > 0) handler.fill(ours(r, m.in), IFluidHandler.FluidAction.EXECUTE);
            }
        };

        Fluids(IFluidHandler handler) {
            this.handler = handler;
        }

        @Override
        public int size() {
            return handler.getTanks();
        }

        @Override
        public FluidResource getResource(int index) {
            return FluidResource.of(NeoTransfer.toNeo(handler.getFluidInTank(index)));
        }

        @Override
        public long getAmountAsLong(int index) {
            return handler.getFluidInTank(index).getAmount();
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            return handler.getTankCapacity(index);
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return resource.isEmpty() || handler.isFluidValid(index, ours(resource, 1000));
        }

        /** Our fluid handlers fill and drain the handler as a whole: the index only says which tank the caller looked at. */
        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext tx) {
            return insert(resource, amount, tx);
        }

        @Override
        public int insert(FluidResource resource, int amount, TransactionContext tx) {
            if (resource.isEmpty() || amount <= 0) return 0;
            Moved e = ledger.entry(tx, resource);
            int want = (int) Math.min(Integer.MAX_VALUE, (long) e.in + amount);
            int accepted = Math.max(0, handler.fill(ours(resource, want), IFluidHandler.FluidAction.SIMULATE) - e.in);
            e.in += accepted;
            return accepted;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext tx) {
            return extract(resource, amount, tx);
        }

        @Override
        public int extract(FluidResource resource, int amount, TransactionContext tx) {
            if (resource.isEmpty() || amount <= 0) return 0;
            Moved e = ledger.entry(tx, resource);
            int want = (int) Math.min(Integer.MAX_VALUE, (long) e.out + amount);
            int got = Math.max(0, handler.drain(ours(resource, want), IFluidHandler.FluidAction.SIMULATE).getAmount() - e.out);
            e.out += got;
            return got;
        }
    }

    // ------------------------------------------------------------------ energy

    public static @Nullable EnergyHandler energy(@Nullable IEnergyStorage e) {
        if (e instanceof FromResources.Energy theirs) return theirs.h(); // M52e: another mod's handler stays itself
        return e == null ? null : new Energy(e);
    }

    static final class Energy implements EnergyHandler {
        final IEnergyStorage storage;
        final Ledger<Boolean> ledger = new Ledger<>() {
            @Override
            void apply(Boolean key, Moved m) {
                if (m.out > 0) storage.extractEnergy(m.out, false);
                if (m.in > 0) storage.receiveEnergy(m.in, false);
            }
        };

        Energy(IEnergyStorage storage) {
            this.storage = storage;
        }

        @Override
        public long getAmountAsLong() {
            Moved m = ledger.peek(Boolean.TRUE);
            return storage.getEnergyStored() + (m == null ? 0 : m.in - m.out);
        }

        @Override
        public long getCapacityAsLong() {
            return storage.getMaxEnergyStored();
        }

        @Override
        public int insert(int amount, TransactionContext tx) {
            if (amount <= 0 || !storage.canReceive()) return 0;
            Moved e = ledger.entry(tx, Boolean.TRUE);
            int want = (int) Math.min(Integer.MAX_VALUE, (long) e.in + amount);
            int accepted = Math.max(0, storage.receiveEnergy(want, true) - e.in);
            e.in += accepted;
            return accepted;
        }

        @Override
        public int extract(int amount, TransactionContext tx) {
            if (amount <= 0 || !storage.canExtract()) return 0;
            Moved e = ledger.entry(tx, Boolean.TRUE);
            int want = (int) Math.min(Integer.MAX_VALUE, (long) e.out + amount);
            int got = Math.max(0, storage.extractEnergy(want, true) - e.out);
            e.out += got;
            return got;
        }
    }
}
