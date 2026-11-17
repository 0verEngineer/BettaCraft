package org.overengineer.bettacraft.platform.fluid;

/**
 * M52: tanks other blocks and mods can fill and drain (millibuckets). The loader-independent counterpart of NeoForge's
 * interface of the same name; the platform layer offers it to other mods (NeoForge: the fluid handler capability;
 * Fabric: the transfer API, droplets converted) and wraps theirs into it.
 */
public interface IFluidHandler {
    enum FluidAction {
        EXECUTE, SIMULATE;

        public boolean execute() {
            return this == EXECUTE;
        }

        public boolean simulate() {
            return this == SIMULATE;
        }
    }

    int getTanks();

    FluidStack getFluidInTank(int tank);

    int getTankCapacity(int tank);

    boolean isFluidValid(int tank, FluidStack stack);

    /** Fills; returns the amount that was (or would be) filled. */
    int fill(FluidStack resource, FluidAction action);

    /** Drains that fluid (up to its amount); returns what was (or would be) drained. */
    FluidStack drain(FluidStack resource, FluidAction action);

    /** Drains any fluid up to {@code maxDrain}; returns what was (or would be) drained. */
    FluidStack drain(int maxDrain, FluidAction action);
}
