package org.overengineer.bettacraft.datagen;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.tags.TagKey;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.tank.TankMoves;

/**
 * M50 data: Sable (Create Aeronautics) keeps the entity types of {@code sable:retain_in_sub_level} in its sub-levels instead of
 * pushing them out into the world. All our animals are in it: in a tank on an airship they swim on with the ship
 * ({@code TankMoves#swimsOn}); the animals of Minecraft and other mods are not ours to change, they travel packed.
 */
final class DataGenM50 {
    private DataGenM50() {}

    static void entityTags(java.util.function.Function<TagKey<EntityType<?>>, IntrinsicHolderTagsProvider.IntrinsicTagAppender<EntityType<?>>> tag) {
        var retain = tag.apply(TankMoves.RETAIN);
        BuiltInRegistries.ENTITY_TYPE.stream().filter(t -> BettaCraft.MOD_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(t).getNamespace()))
                .filter(t -> t != org.overengineer.bettacraft.registry.ModEntities.HIRED_AQUARIST.get()) // M51: a person, not an animal of a tank
                .sorted(java.util.Comparator.comparing(t -> BuiltInRegistries.ENTITY_TYPE.getKey(t).toString()))
                .forEach(retain::add);
    }
}
