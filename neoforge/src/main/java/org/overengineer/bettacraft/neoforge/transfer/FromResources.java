package org.overengineer.bettacraft.neoforge.transfer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.platform.energy.IEnergyStorage;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.platform.item.IItemHandler;

/**
 * M52b: the block capabilities of NeoForge 21.9+ (transactional {@link ResourceHandler}s) as our handlers. Our own handlers
 * ({@link NeoResources}) are unwrapped, so moves between our blocks need no transaction at all. A foreign handler is used in
 * a transaction of its own: nested into the one already open on this thread (our handlers simulate inside the transactions
 * of other mods' pipes, and our pipe network then asks its neighbours), otherwise a root one. NeoForge's legacy adapters
 * ({@code IFluidHandler.of}) always open a root transaction and fail inside another one.
 */
@SuppressWarnings("deprecation") // Transaction.getCurrentOpenedTransaction: our handler methods carry no transaction context
public final class FromResources {
    private FromResources() {}

    private static Transaction open() {
        return Transaction.open(Transaction.getCurrentOpenedTransaction());
    }

    // ------------------------------------------------------------------ items

    public static @Nullable IItemHandler items(@Nullable ResourceHandler<ItemResource> h) {
        if (h == null) return null;
        if (h instanceof NeoResources.Items ours) return ours.handler;
        return new Items(h);
    }

    record Items(ResourceHandler<ItemResource> h) implements IItemHandler {
        @Override
        public int getSlots() {
            return h.size();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return h.getResource(slot).toStack(h.getAmountAsInt(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            int done;
            try (Transaction tx = open()) {
                done = h.insert(slot, ItemResource.of(stack), stack.getCount(), tx);
                if (!simulate) tx.commit();
            }
            return done >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - done);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemResource r = h.getResource(slot);
            if (r.isEmpty() || amount <= 0) return ItemStack.EMPTY;
            int done;
            try (Transaction tx = open()) {
                done = h.extract(slot, r, amount, tx);
                if (!simulate) tx.commit();
            }
            return done <= 0 ? ItemStack.EMPTY : r.toStack(done);
        }

        @Override
        public int getSlotLimit(int slot) {
            return h.getCapacityAsInt(slot, ItemResource.EMPTY);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return h.isValid(slot, ItemResource.of(stack));
        }
    }

    // ------------------------------------------------------------------ fluids

    public static @Nullable IFluidHandler fluids(@Nullable ResourceHandler<FluidResource> h) {
        if (h == null) return null;
        if (h instanceof NeoResources.Fluids ours) return ours.handler;
        return new Fluids(h);
    }

    static FluidResource resource(FluidStack s) {
        return FluidResource.of(NeoTransfer.toNeo(s));
    }

    record Fluids(ResourceHandler<FluidResource> h) implements IFluidHandler {
        @Override
        public int getTanks() {
            return h.size();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return NeoResources.ours(h.getResource(tank), h.getAmountAsInt(tank));
        }

        @Override
        public int getTankCapacity(int tank) {
            return h.getCapacityAsInt(tank, FluidResource.EMPTY);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return h.isValid(tank, resource(stack));
        }

        @Override
        public int fill(FluidStack stack, FluidAction action) {
            if (stack.isEmpty()) return 0;
            try (Transaction tx = open()) {
                int done = h.insert(resource(stack), stack.getAmount(), tx);
                if (action.execute()) tx.commit();
                return done;
            }
        }

        @Override
        public FluidStack drain(FluidStack stack, FluidAction action) {
            if (stack.isEmpty()) return FluidStack.EMPTY;
            FluidResource r = resource(stack);
            try (Transaction tx = open()) {
                int done = h.extract(r, stack.getAmount(), tx);
                if (action.execute()) tx.commit();
                return done <= 0 ? FluidStack.EMPTY : NeoResources.ours(r, done);
            }
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) return FluidStack.EMPTY;
            for (int i = 0; i < h.size(); i++) {
                FluidResource r = h.getResource(i);
                if (r.isEmpty() || h.getAmountAsLong(i) <= 0) continue;
                try (Transaction tx = open()) {
                    int done = h.extract(r, maxDrain, tx);
                    if (done <= 0) continue;
                    if (action.execute()) tx.commit();
                    return NeoResources.ours(r, done);
                }
            }
            return FluidStack.EMPTY;
        }
    }

    // ------------------------------------------------------------------ energy

    public static @Nullable IEnergyStorage energy(@Nullable EnergyHandler h) {
        if (h == null) return null;
        if (h instanceof NeoResources.Energy ours) return ours.storage;
        return new Energy(h);
    }

    record Energy(EnergyHandler h) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            if (toReceive <= 0) return 0;
            try (Transaction tx = open()) {
                int done = h.insert(toReceive, tx);
                if (!simulate) tx.commit();
                return done;
            }
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            if (toExtract <= 0) return 0;
            try (Transaction tx = open()) {
                int done = h.extract(toExtract, tx);
                if (!simulate) tx.commit();
                return done;
            }
        }

        @Override
        public int getEnergyStored() {
            return h.getAmountAsInt();
        }

        @Override
        public int getMaxEnergyStored() {
            return h.getCapacityAsInt();
        }

        @Override
        public boolean canExtract() {
            try (Transaction tx = open()) {
                return h.extract(1, tx) > 0; // aborted: only a test
            }
        }

        @Override
        public boolean canReceive() {
            try (Transaction tx = open()) {
                return h.insert(1, tx) > 0; // aborted: only a test
            }
        }
    }
}
