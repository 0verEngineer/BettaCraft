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

    /** M31: common musk turtle (Sternotherus odoratus), about 10 cm: the first terrarium animal. */
    public static final Supplier<EntityType<org.overengineer.bettacraft.terrarium.MuskTurtleEntity>> MUSK_TURTLE = ENTITIES.register("musk_turtle",
            () -> EntityType.Builder.of(org.overengineer.bettacraft.terrarium.MuskTurtleEntity::new, MobCategory.CREATURE)
                    .sized(0.5F, 0.26F)
                    .eyeHeight(0.16F)
                    .clientTrackingRange(6)
                    .build(BettaCraft.id("musk_turtle").toString()));

    /** M31 part b: pond slider (Trachemys scripta, red-eared or yellow-bellied), grows to about 25 cm. */
    public static final Supplier<EntityType<org.overengineer.bettacraft.terrarium.TurtleEntity>> SLIDER_TURTLE = ENTITIES.register("slider_turtle",
            () -> EntityType.Builder.<org.overengineer.bettacraft.terrarium.TurtleEntity>of((t, l) -> new org.overengineer.bettacraft.terrarium.TurtleEntity(t, l, org.overengineer.bettacraft.terrarium.TurtleSpecies.SLIDER), MobCategory.CREATURE)
                    .sized(0.78F, 0.32F).eyeHeight(0.2F).clientTrackingRange(6)
                    .build(BettaCraft.id("slider_turtle").toString()));
    /** M31 part b: Hermann's tortoise (Testudo hermanni), about 18 cm. */
    public static final Supplier<EntityType<org.overengineer.bettacraft.terrarium.TurtleEntity>> HERMANN_TORTOISE = ENTITIES.register("hermann_tortoise",
            () -> EntityType.Builder.<org.overengineer.bettacraft.terrarium.TurtleEntity>of((t, l) -> new org.overengineer.bettacraft.terrarium.TurtleEntity(t, l, org.overengineer.bettacraft.terrarium.TurtleSpecies.HERMANN), MobCategory.CREATURE)
                    .sized(0.62F, 0.4F).eyeHeight(0.24F).clientTrackingRange(6)
                    .build(BettaCraft.id("hermann_tortoise").toString()));
    /** M31 part b: Chinese three-keeled pond turtle (Mauremys reevesii), about 15 cm. */
    public static final Supplier<EntityType<org.overengineer.bettacraft.terrarium.TurtleEntity>> REEVES_TURTLE = ENTITIES.register("reeves_turtle",
            () -> EntityType.Builder.<org.overengineer.bettacraft.terrarium.TurtleEntity>of((t, l) -> new org.overengineer.bettacraft.terrarium.TurtleEntity(t, l, org.overengineer.bettacraft.terrarium.TurtleSpecies.REEVES), MobCategory.CREATURE)
                    .sized(0.58F, 0.28F).eyeHeight(0.18F).clientTrackingRange(6)
                    .build(BettaCraft.id("reeves_turtle").toString()));

    /** M31 part b: the entity type of a turtle species. */
    public static EntityType<? extends org.overengineer.bettacraft.terrarium.TurtleEntity> turtle(org.overengineer.bettacraft.terrarium.TurtleSpecies s) {
        if (s == org.overengineer.bettacraft.terrarium.TurtleSpecies.SLIDER) return SLIDER_TURTLE.get();
        if (s == org.overengineer.bettacraft.terrarium.TurtleSpecies.HERMANN) return HERMANN_TORTOISE.get();
        if (s == org.overengineer.bettacraft.terrarium.TurtleSpecies.REEVES) return REEVES_TURTLE.get();
        return MUSK_TURTLE.get();
    }

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
        event.put(MUSK_TURTLE.get(), org.overengineer.bettacraft.terrarium.MuskTurtleEntity.createAttributes().build()); // M31
        event.put(SLIDER_TURTLE.get(), org.overengineer.bettacraft.terrarium.TurtleEntity.createAttributes(org.overengineer.bettacraft.terrarium.TurtleSpecies.SLIDER).build()); // M31 part b
        event.put(HERMANN_TORTOISE.get(), org.overengineer.bettacraft.terrarium.TurtleEntity.createAttributes(org.overengineer.bettacraft.terrarium.TurtleSpecies.HERMANN).build());
        event.put(REEVES_TURTLE.get(), org.overengineer.bettacraft.terrarium.TurtleEntity.createAttributes(org.overengineer.bettacraft.terrarium.TurtleSpecies.REEVES).build());
        event.put(WILD_CRITTER.get(), org.overengineer.bettacraft.entity.critter.WildCritterEntity.createAttributes().build()); // M22.3
        for (var e : TANKMATES.entrySet()) event.put(e.getValue().get(), org.overengineer.bettacraft.entity.tankmate.TankmateEntity.createAttributes(e.getKey()).build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(BETTA.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                WaterAnimal::checkSurfaceWaterAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // M22.3: the wild tankmates, frogs and small animals (rare, never in a tank)
        for (var e : TANKMATES.entrySet()) {
            // M30: the big animals with their own, rarer rule
            event.register(e.getValue().get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    e.getKey().isLarge() ? org.overengineer.bettacraft.world.Wildlife::checkBig : org.overengineer.bettacraft.world.Wildlife::checkFish,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
        event.register(DWARF_FROG.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                org.overengineer.bettacraft.world.Wildlife::checkFish, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(WILD_CRITTER.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR,
                org.overengineer.bettacraft.world.Wildlife::checkCritter, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // M31 part b: the wild turtles (on the shore of their water; the tortoise on dry grassland)
        for (var s : org.overengineer.bettacraft.terrarium.TurtleSpecies.ALL) {
            event.register(turtle(s), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    org.overengineer.bettacraft.terrarium.TurtleSpawns::check, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }

    private ModEntities() {}
}
