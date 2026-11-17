package org.overengineer.bettacraft.platform.fluid;

import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.FlowingFluid;
import org.jetbrains.annotations.Nullable;

/**
 * M52: one of our fluids (reverse osmosis water, sea water, live food, fertilizer): water-like, never converts to a
 * source. The loader builds the fluid objects from it ({@code Platform#fluid}); on NeoForge each gets a fluid type of the
 * same id.
 *
 * @param name     registry path of the source fluid and of NeoForge's fluid type ({@code flowing_<name>} for the flowing one)
 * @param density  NeoForge's fluid type density (water: 1000)
 * @param source   the registered source fluid
 * @param flowing  the registered flowing fluid
 * @param bucket   the bucket item, or null
 */
public record FluidSpec(String name, int density, Supplier<? extends FlowingFluid> source, Supplier<? extends FlowingFluid> flowing,
        @Nullable Supplier<? extends Item> bucket) {
}
