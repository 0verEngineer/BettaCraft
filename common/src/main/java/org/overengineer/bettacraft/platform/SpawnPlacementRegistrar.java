package org.overengineer.bettacraft.platform;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

/** M52: registers the spawn rule of an entity type, replacing an existing one (NeoForge: its event, REPLACE). */
@FunctionalInterface
public interface SpawnPlacementRegistrar {
    <T extends Mob> void register(EntityType<T> type, SpawnPlacementType placement, Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate);
}
