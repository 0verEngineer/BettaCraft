package org.overengineer.bettacraft.neoforge.transfer;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.platform.energy.IEnergyStorage;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.platform.fluid.IFluidHandlerItem;
import org.overengineer.bettacraft.platform.item.IItemHandler;
import org.overengineer.bettacraft.platform.item.IItemHandlerModifiable;

/**
 * M52: converts between the common item / fluid / energy handlers ({@code platform.*}) and NeoForge's capabilities.
 * Ours are offered to NeoForge wrapped ({@code toNeo}); NeoForge's (other mods') are used wrapped ({@code fromNeo}). A
 * wrapper of a wrapper is unwrapped, so our own handlers come back as themselves.
 */
public final class NeoTransfer {
    private NeoTransfer() {}

    // ---- fluids

    public static net.neoforged.neoforge.fluids.FluidStack toNeo(FluidStack s) {
        if (s.isEmpty()) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
        return new net.neoforged.neoforge.fluids.FluidStack(s.getFluidHolder(), s.getAmount(), s.getComponentsPatch());
    }

    public static FluidStack fromNeo(net.neoforged.neoforge.fluids.FluidStack s) {
        if (s.isEmpty()) return FluidStack.EMPTY;
        return new FluidStack(s.getFluidHolder(), s.getAmount(), s.getComponentsPatch());
    }

    static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction toNeo(IFluidHandler.FluidAction a) {
        return a.execute() ? net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE
                : net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE;
    }

    static IFluidHandler.FluidAction fromNeo(net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction a) {
        return a.execute() ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE;
    }

    public static @Nullable net.neoforged.neoforge.fluids.capability.IFluidHandler toNeo(@Nullable IFluidHandler h) {
        if (h == null) return null;
        if (h instanceof FromNeoFluid f) return f.neo;
        return new ToNeoFluid(h);
    }

    public static @Nullable IFluidHandler fromNeo(@Nullable net.neoforged.neoforge.fluids.capability.IFluidHandler h) {
        if (h == null) return null;
        if (h instanceof ToNeoFluid t) return t.ours;
        if (h instanceof net.neoforged.neoforge.fluids.capability.IFluidHandlerItem item) return new FromNeoFluidItem(item);
        return new FromNeoFluid(h);
    }

    public static IFluidHandlerItem fromNeoItem(net.neoforged.neoforge.fluids.capability.IFluidHandlerItem h) {
        return new FromNeoFluidItem(h);
    }

    static class ToNeoFluid implements net.neoforged.neoforge.fluids.capability.IFluidHandler {
        final IFluidHandler ours;

        ToNeoFluid(IFluidHandler ours) {
            this.ours = ours;
        }

        @Override
        public int getTanks() {
            return ours.getTanks();
        }

        @Override
        public net.neoforged.neoforge.fluids.FluidStack getFluidInTank(int tank) {
            return toNeo(ours.getFluidInTank(tank));
        }

        @Override
        public int getTankCapacity(int tank) {
            return ours.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, net.neoforged.neoforge.fluids.FluidStack stack) {
            return ours.isFluidValid(tank, fromNeo(stack));
        }

        @Override
        public int fill(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
            return ours.fill(fromNeo(resource), fromNeo(action));
        }

        @Override
        public net.neoforged.neoforge.fluids.FluidStack drain(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
            return toNeo(ours.drain(fromNeo(resource), fromNeo(action)));
        }

        @Override
        public net.neoforged.neoforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            return toNeo(ours.drain(maxDrain, fromNeo(action)));
        }
    }

    static class FromNeoFluid implements IFluidHandler {
        final net.neoforged.neoforge.fluids.capability.IFluidHandler neo;

        FromNeoFluid(net.neoforged.neoforge.fluids.capability.IFluidHandler neo) {
            this.neo = neo;
        }

        @Override
        public int getTanks() {
            return neo.getTanks();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return fromNeo(neo.getFluidInTank(tank));
        }

        @Override
        public int getTankCapacity(int tank) {
            return neo.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return neo.isFluidValid(tank, toNeo(stack));
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return neo.fill(toNeo(resource), toNeo(action));
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return fromNeo(neo.drain(toNeo(resource), toNeo(action)));
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return fromNeo(neo.drain(maxDrain, toNeo(action)));
        }
    }

    static final class FromNeoFluidItem extends FromNeoFluid implements IFluidHandlerItem {
        FromNeoFluidItem(net.neoforged.neoforge.fluids.capability.IFluidHandlerItem neo) {
            super(neo);
        }

        @Override
        public ItemStack getContainer() {
            return ((net.neoforged.neoforge.fluids.capability.IFluidHandlerItem) neo).getContainer();
        }
    }

    // ---- items

    public static @Nullable net.neoforged.neoforge.items.IItemHandler toNeo(@Nullable IItemHandler h) {
        if (h == null) return null;
        if (h instanceof FromNeoItem f) return f.neo;
        return h instanceof IItemHandlerModifiable m ? new ToNeoItemModifiable(m) : new ToNeoItem(h);
    }

    public static @Nullable IItemHandler fromNeo(@Nullable net.neoforged.neoforge.items.IItemHandler h) {
        if (h == null) return null;
        if (h instanceof ToNeoItem t) return t.ours;
        return h instanceof net.neoforged.neoforge.items.IItemHandlerModifiable m ? new FromNeoItemModifiable(m) : new FromNeoItem(h);
    }

    static class ToNeoItem implements net.neoforged.neoforge.items.IItemHandler {
        final IItemHandler ours;

        ToNeoItem(IItemHandler ours) {
            this.ours = ours;
        }

        @Override
        public int getSlots() {
            return ours.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ours.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return ours.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ours.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return ours.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return ours.isItemValid(slot, stack);
        }
    }

    static final class ToNeoItemModifiable extends ToNeoItem implements net.neoforged.neoforge.items.IItemHandlerModifiable {
        ToNeoItemModifiable(IItemHandlerModifiable ours) {
            super(ours);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            ((IItemHandlerModifiable) ours).setStackInSlot(slot, stack);
        }
    }

    static class FromNeoItem implements IItemHandler {
        final net.neoforged.neoforge.items.IItemHandler neo;

        FromNeoItem(net.neoforged.neoforge.items.IItemHandler neo) {
            this.neo = neo;
        }

        @Override
        public int getSlots() {
            return neo.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return neo.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return neo.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return neo.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return neo.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return neo.isItemValid(slot, stack);
        }
    }

    static final class FromNeoItemModifiable extends FromNeoItem implements IItemHandlerModifiable {
        FromNeoItemModifiable(net.neoforged.neoforge.items.IItemHandlerModifiable neo) {
            super(neo);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            ((net.neoforged.neoforge.items.IItemHandlerModifiable) neo).setStackInSlot(slot, stack);
        }
    }

    // ---- energy

    public static @Nullable net.neoforged.neoforge.energy.IEnergyStorage toNeo(@Nullable IEnergyStorage e) {
        if (e == null) return null;
        if (e instanceof FromNeoEnergy f) return f.neo;
        return new ToNeoEnergy(e);
    }

    public static @Nullable IEnergyStorage fromNeo(@Nullable net.neoforged.neoforge.energy.IEnergyStorage e) {
        if (e == null) return null;
        if (e instanceof ToNeoEnergy t) return t.ours;
        return new FromNeoEnergy(e);
    }

    record ToNeoEnergy(IEnergyStorage ours) implements net.neoforged.neoforge.energy.IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return ours.receiveEnergy(toReceive, simulate);
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return ours.extractEnergy(toExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return ours.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return ours.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return ours.canExtract();
        }

        @Override
        public boolean canReceive() {
            return ours.canReceive();
        }
    }

    record FromNeoEnergy(net.neoforged.neoforge.energy.IEnergyStorage neo) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return neo.receiveEnergy(toReceive, simulate);
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return neo.extractEnergy(toExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return neo.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return neo.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return neo.canExtract();
        }

        @Override
        public boolean canReceive() {
            return neo.canReceive();
        }
    }
}
