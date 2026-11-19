package org.overengineer.bettacraft.neoforge.transfer;

import org.overengineer.bettacraft.platform.fluid.FluidStack;

/**
 * M52: converts between the common fluid stack ({@code platform.fluid}) and NeoForge's. M52e: NeoForge 26.3 removed the
 * legacy item / fluid / energy handler capabilities; ours are offered and theirs used through the transfer API
 * ({@link NeoResources}, {@link FromResources}), only the stack conversion is left here.
 */
public final class NeoTransfer {
    private NeoTransfer() {}

    public static net.neoforged.neoforge.fluids.FluidStack toNeo(FluidStack s) {
        if (s.isEmpty()) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
        return new net.neoforged.neoforge.fluids.FluidStack(s.getFluidHolder(), s.getAmount(), s.getComponentsPatch());
    }

    public static FluidStack fromNeo(net.neoforged.neoforge.fluids.FluidStack s) {
        if (s.isEmpty()) return FluidStack.EMPTY;
        return new FluidStack(s.typeHolder(), s.getAmount(), s.getComponentsPatch());
    }
}
