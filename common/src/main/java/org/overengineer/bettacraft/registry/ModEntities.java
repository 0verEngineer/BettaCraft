package org.overengineer.bettacraft.registry;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.level.levelgen.Heightmap;
import org.overengineer.bettacraft.platform.registry.DeferredRegister;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.entity.betta.BettaEntity;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, BettaCraft.MOD_ID);

    public static final Supplier<EntityType<BettaEntity>> BETTA = ENTITIES.register("betta",
            () -> EntityType.Builder.of(BettaEntity::new, MobCategory.WATER_AMBIENT)
                    .sized(0.5F, 0.35F)
                    .eyeHeight(0.2F)
                    .clientTrackingRange(4)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, BettaCraft.id("betta"))));

    /** M51: the hired aquarist (a person, not a villager). */
    public static final Supplier<EntityType<org.overengineer.bettacraft.aquarist.AquaristEntity>> HIRED_AQUARIST = ENTITIES.register("hired_aquarist",
            () -> EntityType.Builder.of(org.overengineer.bettacraft.aquarist.AquaristEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.62F)
                    .clientTrackingRange(10)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, BettaCraft.id("hired_aquarist"))));

    /** M14: African dwarf clawed frog (Hymenochirus), about 3.5 cm. */
    public static final Supplier<EntityType<org.overengineer.bettacraft.entity.frog.DwarfFrogEntity>> DWARF_FROG = ENTITIES.register("dwarf_clawed_frog",
            () -> EntityType.Builder.of(org.overengineer.bettacraft.entity.frog.DwarfFrogEntity::new, MobCategory.WATER_AMBIENT)
                    .sized(0.3F, 0.15F)
                    .eyeHeight(0.1F)
                    .clientTrackingRange(4)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, BettaCraft.id("dwarf_clawed_frog"))));

    /** M31: common musk turtle (Sternotherus odoratus), about 10 cm: the first terrarium animal. */
    public static final Supplier<EntityType<org.overengineer.bettacraft.terrarium.MuskTurtleEntity>> MUSK_TURTLE = ENTITIES.register("musk_turtle",
            () -> EntityType.Builder.of(org.overengineer.bettacraft.terrarium.MuskTurtleEntity::new, MobCategory.CREATURE)
                    .sized(0.5F, 0.26F)
                    .eyeHeight(0.16F)
                    .clientTrackingRange(6)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, BettaCraft.id("musk_turtle"))));

    /** M31 part b: pond slider (Trachemys scripta, red-eared or yellow-bellied), grows to about 25 cm. */
    public static final Supplier<EntityType<org.overengineer.bettacraft.terrarium.HerpEntity>> SLIDER_TURTLE = ENTITIES.register("slider_turtle",
            () -> EntityType.Builder.<org.overengineer.bettacraft.terrarium.HerpEntity>of((t, l) -> new org.overengineer.bettacraft.terrarium.HerpEntity(t, l, org.overengineer.bettacraft.terrarium.HerpSpecies.SLIDER), MobCategory.CREATURE)
                    .sized(0.78F, 0.32F).eyeHeight(0.2F).clientTrackingRange(6)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, BettaCraft.id("slider_turtle"))));
    /** M31 part b: Hermann's tortoise (Testudo hermanni), about 18 cm. */
    public static final Supplier<EntityType<org.overengineer.bettacraft.terrarium.HerpEntity>> HERMANN_TORTOISE = ENTITIES.register("hermann_tortoise",
            () -> EntityType.Builder.<org.overengineer.bettacraft.terrarium.HerpEntity>of((t, l) -> new org.overengineer.bettacraft.terrarium.HerpEntity(t, l, org.overengineer.bettacraft.terrarium.HerpSpecies.HERMANN), MobCategory.CREATURE)
                    .sized(0.62F, 0.4F).eyeHeight(0.24F).clientTrackingRange(6)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, BettaCraft.id("hermann_tortoise"))));
    /** M31 part b: Chinese three-keeled pond turtle (Mauremys reevesii), about 15 cm. */
    public static final Supplier<EntityType<org.overengineer.bettacraft.terrarium.HerpEntity>> REEVES_TURTLE = ENTITIES.register("reeves_turtle",
            () -> EntityType.Builder.<org.overengineer.bettacraft.terrarium.HerpEntity>of((t, l) -> new org.overengineer.bettacraft.terrarium.HerpEntity(t, l, org.overengineer.bettacraft.terrarium.HerpSpecies.REEVES), MobCategory.CREATURE)
                    .sized(0.58F, 0.28F).eyeHeight(0.18F).clientTrackingRange(6)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, BettaCraft.id("reeves_turtle"))));

    /**
     * M31 part c: the amphibians and reptiles (dart frog 4 cm, horned frog 12 cm, leopard gecko 22 cm, crested gecko 20 cm,
     * corn snake 1.2 m: the hitbox is the body without the tail). Their position goes out every second tick (the hops).
     */
    public static final java.util.Map<org.overengineer.bettacraft.terrarium.HerpSpecies, Supplier<EntityType<org.overengineer.bettacraft.terrarium.HerpEntity>>> HERPS = herps();

    private static java.util.Map<org.overengineer.bettacraft.terrarium.HerpSpecies, Supplier<EntityType<org.overengineer.bettacraft.terrarium.HerpEntity>>> herps() {
        var m = new java.util.LinkedHashMap<org.overengineer.bettacraft.terrarium.HerpSpecies, Supplier<EntityType<org.overengineer.bettacraft.terrarium.HerpEntity>>>();
        herp(m, org.overengineer.bettacraft.terrarium.HerpSpecies.DART_FROG, 0.2F, 0.12F);
        herp(m, org.overengineer.bettacraft.terrarium.HerpSpecies.HORNED_FROG, 0.45F, 0.28F);
        herp(m, org.overengineer.bettacraft.terrarium.HerpSpecies.LEOPARD_GECKO, 0.36F, 0.16F);
        herp(m, org.overengineer.bettacraft.terrarium.HerpSpecies.CRESTED_GECKO, 0.32F, 0.14F);
        herp(m, org.overengineer.bettacraft.terrarium.HerpSpecies.CORN_SNAKE, 0.5F, 0.12F);
        herp(m, org.overengineer.bettacraft.terrarium.HerpSpecies.MUDSKIPPER, 0.28F, 0.16F); // M49 part b
        return java.util.Collections.unmodifiableMap(m);
    }

    private static void herp(java.util.Map<org.overengineer.bettacraft.terrarium.HerpSpecies, Supplier<EntityType<org.overengineer.bettacraft.terrarium.HerpEntity>>> m,
                             org.overengineer.bettacraft.terrarium.HerpSpecies s, float width, float height) {
        m.put(s, ENTITIES.register(s.id(), () -> EntityType.Builder.<org.overengineer.bettacraft.terrarium.HerpEntity>of(
                        (t, l) -> new org.overengineer.bettacraft.terrarium.HerpEntity(t, l, s), MobCategory.CREATURE)
                .sized(width, height).eyeHeight(height * 0.7F).clientTrackingRange(6).updateInterval(2)
                .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, BettaCraft.id(s.id())))));
    }

    /** M31 part b: the entity type of a species (part c: the amphibians and reptiles too). */
    public static EntityType<? extends org.overengineer.bettacraft.terrarium.HerpEntity> herp(org.overengineer.bettacraft.terrarium.HerpSpecies s) {
        var c = HERPS.get(s);
        if (c != null) return c.get();
        if (s == org.overengineer.bettacraft.terrarium.HerpSpecies.SLIDER) return SLIDER_TURTLE.get();
        if (s == org.overengineer.bettacraft.terrarium.HerpSpecies.HERMANN) return HERMANN_TORTOISE.get();
        if (s == org.overengineer.bettacraft.terrarium.HerpSpecies.REEVES) return REEVES_TURTLE.get();
        return MUSK_TURTLE.get();
    }

    /** M22.3: a shrimp, crayfish, snail or hermit crab living in the world (one type, the species is data). */
    public static final Supplier<EntityType<org.overengineer.bettacraft.entity.critter.WildCritterEntity>> WILD_CRITTER = ENTITIES.register("wild_critter",
            () -> EntityType.Builder.of(org.overengineer.bettacraft.entity.critter.WildCritterEntity::new, MobCategory.WATER_AMBIENT)
                    .sized(0.2F, 0.1F)
                    .eyeHeight(0.05F)
                    .clientTrackingRange(4)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, BettaCraft.id("wild_critter"))));

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
                        .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, BettaCraft.id(sp.id())));
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

    public static void register() {
        ENTITIES.register();
    }

    /** M52: the attributes of our entities (the loader hands them to its attribute registration). */
    public static void attributes(java.util.function.BiConsumer<EntityType<? extends net.minecraft.world.entity.LivingEntity>,
            net.minecraft.world.entity.ai.attributes.AttributeSupplier> event) {
        event.accept(BETTA.get(), BettaEntity.createAttributes().build());
        event.accept(HIRED_AQUARIST.get(), org.overengineer.bettacraft.aquarist.AquaristEntity.createAttributes().build()); // M51
        event.accept(DWARF_FROG.get(), org.overengineer.bettacraft.entity.frog.DwarfFrogEntity.createAttributes().build());
        event.accept(MUSK_TURTLE.get(), org.overengineer.bettacraft.terrarium.MuskTurtleEntity.createAttributes().build()); // M31
        event.accept(SLIDER_TURTLE.get(), org.overengineer.bettacraft.terrarium.HerpEntity.createAttributes(org.overengineer.bettacraft.terrarium.HerpSpecies.SLIDER).build()); // M31 part b
        event.accept(HERMANN_TORTOISE.get(), org.overengineer.bettacraft.terrarium.HerpEntity.createAttributes(org.overengineer.bettacraft.terrarium.HerpSpecies.HERMANN).build());
        event.accept(REEVES_TURTLE.get(), org.overengineer.bettacraft.terrarium.HerpEntity.createAttributes(org.overengineer.bettacraft.terrarium.HerpSpecies.REEVES).build());
        for (var e : HERPS.entrySet()) event.accept(e.getValue().get(), org.overengineer.bettacraft.terrarium.HerpEntity.createAttributes(e.getKey()).build()); // M31 part c
        event.accept(WILD_CRITTER.get(), org.overengineer.bettacraft.entity.critter.WildCritterEntity.createAttributes().build()); // M22.3
        for (var e : TANKMATES.entrySet()) event.accept(e.getValue().get(), org.overengineer.bettacraft.entity.tankmate.TankmateEntity.createAttributes(e.getKey()).build());
    }

    /** M52: the spawn rules of our entities, replacing the default ones (the loader registers them). */
    public static void spawnPlacements(org.overengineer.bettacraft.platform.SpawnPlacementRegistrar event) {
        // M33: natural spawns of wild bettas only with features.bettas.wildSpawns (the biome lists are fixed at world load)
        event.register(BETTA.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) -> (spawnType != net.minecraft.world.entity.EntitySpawnReason.NATURAL
                        && spawnType != net.minecraft.world.entity.EntitySpawnReason.CHUNK_GENERATION
                        || org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.BETTAS_WILD))
                        && WaterAnimal.checkSurfaceWaterAnimalSpawnRules(type, level, spawnType, pos, random));
        // M22.3: the wild tankmates, frogs and small animals (rare, never in a tank)
        for (var e : TANKMATES.entrySet()) {
            // M30: the big animals with their own, rarer rule
            event.register(e.getValue().get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    e.getKey().isLarge() ? org.overengineer.bettacraft.world.Wildlife::checkBig : org.overengineer.bettacraft.world.Wildlife::checkFish);
        }
        event.register(DWARF_FROG.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                org.overengineer.bettacraft.world.Wildlife::checkFish);
        event.register(WILD_CRITTER.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR,
                org.overengineer.bettacraft.world.Wildlife::checkCritter);
        // M31 part b: the wild turtles (on the shore of their water; the tortoise on dry grassland); part c the land animals
        for (var s : org.overengineer.bettacraft.terrarium.HerpSpecies.ALL) {
            event.register(herp(s), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    org.overengineer.bettacraft.terrarium.HerpSpawns::check);
        }
    }

    private ModEntities() {}
}
