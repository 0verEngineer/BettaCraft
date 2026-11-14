package org.overengineer.bettacraft.registry;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.entity.betta.BettaEntity;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, BettaCraft.MOD_ID);

    public static final Supplier<EntityType<BettaEntity>> BETTA = ENTITIES.register("betta",
            () -> EntityType.Builder.of(BettaEntity::new, MobCategory.WATER_AMBIENT)
                    .sized(0.5F, 0.35F)
                    .eyeHeight(0.2F)
                    .clientTrackingRange(4)
                    .build(BettaCraft.id("betta").toString()));

    /** M14: African dwarf clawed frog (Hymenochirus), about 3.5 cm. */
    public static final Supplier<EntityType<org.overengineer.bettacraft.entity.frog.DwarfFrogEntity>> DWARF_FROG = ENTITIES.register("dwarf_clawed_frog",
            () -> EntityType.Builder.of(org.overengineer.bettacraft.entity.frog.DwarfFrogEntity::new, MobCategory.WATER_AMBIENT)
                    .sized(0.3F, 0.15F)
                    .eyeHeight(0.1F)
                    .clientTrackingRange(4)
                    .build(BettaCraft.id("dwarf_clawed_frog").toString()));

    /** M22.3: a shrimp, crayfish, snail or hermit crab living in the world (one type, the species is data). */
    public static final Supplier<EntityType<org.overengineer.bettacraft.entity.critter.WildCritterEntity>> WILD_CRITTER = ENTITIES.register("wild_critter",
            () -> EntityType.Builder.of(org.overengineer.bettacraft.entity.critter.WildCritterEntity::new, MobCategory.WATER_AMBIENT)
                    .sized(0.2F, 0.1F)
                    .eyeHeight(0.05F)
                    .clientTrackingRange(4)
                    .build(BettaCraft.id("wild_critter").toString()));

    /** M19: one entity type per tankmate species (same class, size and attributes per species). */
    public static final java.util.Map<org.overengineer.bettacraft.tankmates.TankmateSpecies,
            Supplier<EntityType<org.overengineer.bettacraft.entity.tankmate.TankmateEntity>>> TANKMATES = tankmates();
    private static final java.util.Map<EntityType<?>, org.overengineer.bettacraft.tankmates.TankmateSpecies> SPECIES_BY_TYPE =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static java.util.Map<org.overengineer.bettacraft.tankmates.TankmateSpecies,
            Supplier<EntityType<org.overengineer.bettacraft.entity.tankmate.TankmateEntity>>> tankmates() {
        var m = new java.util.EnumMap<org.overengineer.bettacraft.tankmates.TankmateSpecies,
                Supplier<EntityType<org.overengineer.bettacraft.entity.tankmate.TankmateEntity>>>(org.overengineer.bettacraft.tankmates.TankmateSpecies.class);
        for (var sp : org.overengineer.bettacraft.tankmates.TankmateSpecies.values()) {
            m.put(sp, ENTITIES.register(sp.id(), () -> {
                EntityType<org.overengineer.bettacraft.entity.tankmate.TankmateEntity> type = EntityType.Builder
                        .of(org.overengineer.bettacraft.entity.tankmate.TankmateEntity::new, MobCategory.WATER_AMBIENT)
                        .sized(sp.hitboxWidth(), sp.hitboxHeight())
                        .eyeHeight(sp.hitboxHeight() * 0.6F)
                        .clientTrackingRange(4)
                        .build(BettaCraft.id(sp.id()).toString());
                SPECIES_BY_TYPE.put(type, sp);
                return type;
            }));
        }
        return java.util.Collections.unmodifiableMap(m);
    }

    /** The tankmate species of an entity type of ours, or null. */
    @javax.annotation.Nullable
    public static org.overengineer.bettacraft.tankmates.TankmateSpecies tankmateSpecies(EntityType<?> type) {
        return SPECIES_BY_TYPE.get(type);
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(ModEntities::onAttributes);
        bus.addListener(ModEntities::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(BETTA.get(), BettaEntity.createAttributes().build());
        event.put(DWARF_FROG.get(), org.overengineer.bettacraft.entity.frog.DwarfFrogEntity.createAttributes().build());
        event.put(WILD_CRITTER.get(), org.overengineer.bettacraft.entity.critter.WildCritterEntity.createAttributes().build()); // M22.3
        for (var e : TANKMATES.entrySet()) event.put(e.getValue().get(), org.overengineer.bettacraft.entity.tankmate.TankmateEntity.createAttributes(e.getKey()).build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(BETTA.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                WaterAnimal::checkSurfaceWaterAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // M22.3: the wild tankmates, frogs and small animals (rare, never in a tank)
        for (var t : TANKMATES.values()) {
            event.register(t.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    org.overengineer.bettacraft.world.Wildlife::checkFish, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
        event.register(DWARF_FROG.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                org.overengineer.bettacraft.world.Wildlife::checkFish, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(WILD_CRITTER.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR,
                org.overengineer.bettacraft.world.Wildlife::checkCritter, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private ModEntities() {}
}
