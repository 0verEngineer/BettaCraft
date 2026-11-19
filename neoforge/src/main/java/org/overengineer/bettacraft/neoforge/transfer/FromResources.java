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

    static Transaction open() {
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

    // ------------------------------------------------------------------ fluid container items (M52e: 26.3)

    /** The fluid of an item (a bucket, another mod's tank item), working on a copy of one item of the stack. */
    public static java.util.Optional<org.overengineer.bettacraft.platform.fluid.IFluidHandlerItem> fluidItem(ItemStack stack) {
        if (stack.isEmpty()) return java.util.Optional.empty();
        var access = net.neoforged.neoforge.transfer.access.ItemAccess.forStack(stack.copyWithCount(1));
        ResourceHandler<FluidResource> h = access.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.ITEM);
        if (h == null) return java.util.Optional.empty();
        IFluidHandler f = fluids(h);
        return java.util.Optional.of(new org.overengineer.bettacraft.platform.fluid.IFluidHandlerItem() {
            @Override
            public ItemStack getContainer() {
                return access.getResource().toStack(access.getAmount());
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
            public boolean isFluidValid(int tank, FluidStack s) {
                return f.isFluidValid(tank, s);
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
        });
    }

    /**
     * Moves up to {@code maxAmount} mB between a container item (one of the stack) and our handler: into the handler
     * ({@code fromItem}) or out of it. The result is the container after the move, or null if nothing moved.
     */
    public static @Nullable ItemStack moveWithContainer(ItemStack container, IFluidHandler other, int maxAmount, boolean execute, boolean fromItem) {
        if (container.isEmpty() || other == null) return null;
        var access = net.neoforged.neoforge.transfer.access.ItemAccess.forStack(container.copyWithCount(1));
        ResourceHandler<FluidResource> item = access.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.ITEM);
        ResourceHandler<FluidResource> tank = NeoResources.fluids(other);
        if (item == null || tank == null) return null;
        try (Transaction tx = open()) {
            int moved = fromItem ? net.neoforged.neoforge.transfer.ResourceHandlerUtil.move(item, tank, r -> true, maxAmount, tx)
                    : net.neoforged.neoforge.transfer.ResourceHandlerUtil.move(tank, item, r -> true, maxAmount, tx);
            if (moved <= 0) return null;
            ItemStack result = access.getResource().toStack(access.getAmount());
            if (execute) tx.commit();
            return result;
        }
    }
}
