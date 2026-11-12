package org.overengineer.bettacraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.overengineer.bettacraft.BettaCraft;

/** The tank interior dimension is defined by datapack JSON (data/bettacraft/dimension[_type]/tank_interior.json). */
public final class ModDimensions {
    public static final ResourceKey<Level> TANK_INTERIOR = ResourceKey.create(Registries.DIMENSION, BettaCraft.id("tank_interior"));
    public static final ResourceKey<DimensionType> TANK_INTERIOR_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, BettaCraft.id("tank_interior"));

    private ModDimensions() {}
}
