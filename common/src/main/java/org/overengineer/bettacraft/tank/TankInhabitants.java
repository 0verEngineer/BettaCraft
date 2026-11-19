package org.overengineer.bettacraft.tank;

import org.overengineer.bettacraft.util.Perms;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.compat.FishContainers;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.registry.ModAttachments;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inhabitants of a tank are real entities (PLAN 4.2/4.3). This class handles acceptance, inserting from buckets and
 * fish bags, taking out via the menu, the periodic reconciliation with the entities in the tank, the stocking
 * limits and the break-block rule.
 */
public final class TankInhabitants {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/inhabitants");
    public static final long BUCKET_MB = TankData.MB_PER_BLOCK;
    public static final long BAG_MB = 100L;
    /** Default of {@code inhabitants.maxHitbox}. */
    public static final float MAX_HITBOX = 0.8F;
    /** Reconciliations an inhabitant may be missing (e.g. entity section not loaded yet) before it is forgotten. */
    private static final int MISSING_LIMIT = 6;

    /** M16: BOSS, HOSTILE and NOT_AQUATIC added; NOT_BUCKETABLE is no longer used (the fish bag holds any water animal). */
    public enum Acceptance { OK, BLACKLISTED, TOO_BIG, NOT_BUCKETABLE, BOSS, HOSTILE, NOT_AQUATIC,
        /** M30: a big animal of known length: the tank decides ({@link org.overengineer.bettacraft.big.BigAnimals#refusal}); never a bucket or bag. */
        BIG,
        /** M39 (question E1): refused by its profile, with a reason ({@link org.overengineer.bettacraft.foreign.ForeignProfiles#refusal}). */
        REFUSED }

    @Nullable
    private static final Field MOB_BUCKET_TYPE = findBucketTypeField();

    private TankInhabitants() {}

    // ------------------------------------------------------------------ acceptance

    @Nullable
    private static Field findBucketTypeField() {
        for (Field f : MobBucketItem.class.getDeclaredFields()) {
            if (f.getType() == EntityType.class) {
                f.setAccessible(true);
                return f;
            }
        }
        LOGGER.error("MobBucketItem has no EntityType field; inserting fish from buckets will not work");
        return null;
    }

    /**
     * Entity type held by a mob bucket, a filled fish bag or a filled container of another mod (Better Fish Tanks fish
     * bag, Fintastic fishnet), or null if the stack is none of these.
     */
    @Nullable
    public static EntityType<?> containedType(ItemStack stack) {
        if (stack.getItem() instanceof FishBagItem) return FishBagItem.containedType(stack);
        if (stack.getItem() instanceof org.overengineer.bettacraft.big.TransportTubItem) return org.overengineer.bettacraft.big.TransportTubItem.containedType(stack); // M30
        if (stack.getItem() instanceof org.overengineer.bettacraft.terrarium.TerrariumBoxItem) return org.overengineer.bettacraft.terrarium.TerrariumBoxItem.containedType(stack); // M35
        if (FishContainers.kind(stack) != null) return FishContainers.containedType(stack);
        if (stack.getItem() instanceof org.overengineer.bettacraft.mobs.MobJarItem) return org.overengineer.bettacraft.mobs.MobJarItem.containedType(stack); // M31 part d
        if (org.overengineer.bettacraft.compat.MobCaptures.format(stack) != null) return org.overengineer.bettacraft.compat.MobCaptures.containedType(stack);
        if (stack.getItem() instanceof MobBucketItem && MOB_BUCKET_TYPE != null) {
            try {
                return (EntityType<?>) MOB_BUCKET_TYPE.get(stack.getItem());
            } catch (IllegalAccessException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Acceptance rules (M16, {@link InhabitantRules}): config deny and allow lists, blacklist tag, whitelist tag, bosses,
     * hitbox at most {@code inhabitants.maxHitbox} (0.8), no hostile mobs, and (with an instance) a water animal.
     */
    /**
     * M35: whether a fish bag takes this animal: one the tank rules accept, or a young sea turtle (a big animal judged by its
     * adult size; the tank decides by the size rule when it goes in).
     */
    public static boolean bagTakes(Entity e) {
        Acceptance a = accepts(e.getType(), e);
        if (a == Acceptance.OK) return true;
        return a == Acceptance.BIG && e.getType() == EntityType.TURTLE && e instanceof LivingEntity le && le.isBaby();
    }

    public static Acceptance accepts(EntityType<?> type, @Nullable Entity instance) {
        // M31 part d: a mob in glass (drowned, zombie, spider ...) is shrunk in the tank: its size and hostility do not matter
        if (org.overengineer.bettacraft.mobs.GlassMobs.isAllowed(type)) {
            return instance == null || org.overengineer.bettacraft.mobs.GlassMobs.fits(instance) ? Acceptance.OK : Acceptance.TOO_BIG;
        }
        // M16: the size of the animal itself when there is one (variants of other mods differ in size), else of the type
        EntityDimensions d = type.getDimensions();
        float w = instance != null ? instance.getBbWidth() : d.width(), h = instance != null ? instance.getBbHeight() : d.height();
        String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        // M39 part c (#25): the vanilla frog (on the blacklist since M16) is a paludarium pet while the Minecraft pets are on
        boolean black = type.builtInRegistryHolder().is(ModTags.TANK_BLACKLIST) && !org.overengineer.bettacraft.foreign.ForeignProfiles.pet(type);
        var facts = new InhabitantRules.Facts(id, w, h, black, type.builtInRegistryHolder().is(ModTags.TANK_INHABITANTS),
                type.builtInRegistryHolder().is(org.overengineer.bettacraft.platform.Tags.EntityTypes.BOSSES), type.getCategory() == net.minecraft.world.entity.MobCategory.MONSTER,
                // M31: a terrarium animal (turtle, frog, gecko ...) needs no water to live in
                instance == null || org.overengineer.bettacraft.terrarium.TerrariumAnimals.isTerrariumAnimal(type) ? null : isWaterAnimal(instance),
                org.overengineer.bettacraft.big.BigAnimals.length(type, instance), org.overengineer.bettacraft.foreign.ForeignProfiles.refusal(type));
        return InhabitantRules.decide(facts, rules());
    }

    /** The config side of the acceptance rules (defaults before the server config is loaded). */
    public static InhabitantRules.Settings rules() {
        return new InhabitantRules.Settings(ServerConfig.getOr(ServerConfig.INHABITANTS_ALLOW, List.of()),
                ServerConfig.getOr(ServerConfig.INHABITANTS_DENY, List.of()),
                ServerConfig.getOr(ServerConfig.INHABITANTS_MAX_HITBOX, (double) MAX_HITBOX),
                ServerConfig.getOr(ServerConfig.INHABITANTS_REFUSE_HOSTILE, true), Features.enabled(Feature.BIG_ANIMALS));
    }

    /**
     * M16: whether an animal lives in water: fish and other water animals, axolotls, tadpoles, the water spawn categories,
     * and everything that breathes under water (bettas, frogs of other mods). A butterfly from a bug net is none.
     */
    public static boolean isWaterAnimal(Entity e) {
        if (e instanceof net.minecraft.world.entity.animal.fish.WaterAnimal || e instanceof net.minecraft.world.entity.animal.fish.AbstractFish
                || e instanceof net.minecraft.world.entity.animal.axolotl.Axolotl || e instanceof net.minecraft.world.entity.animal.frog.Tadpole) {
            return true;
        }
        if (org.overengineer.bettacraft.compat.AquaticSpecies.waterCategory(e.getType().getCategory())) return true;
        return e instanceof LivingEntity le && le.canBreatheUnderwater();
    }

    public static boolean canModify(Player player, TankData tank) {
        return !tank.locked() || player.getUUID().equals(tank.owner()) || Perms.has(player, 2);
    }

    // ------------------------------------------------------------------ inserting

    /** Water that comes along with the animal of this (filled) container: bucket 1000 mB, bags 100 mB, nets 0. */
    public static long carriedWater(ItemStack stack) {
        if (stack.getItem() instanceof FishBagItem) return BAG_MB;
        if (stack.getItem() instanceof org.overengineer.bettacraft.big.TransportTubItem) return 0; // M30: the tub brings no water
        if (stack.getItem() instanceof org.overengineer.bettacraft.terrarium.AnimalBoxItem || stack.getItem() instanceof org.overengineer.bettacraft.terrarium.TerrariumBoxItem) return 0; // M31 part b: a terrarium box neither (M35: nor one of a foreign animal)
        if (stack.getItem() instanceof org.overengineer.bettacraft.mobs.MobJarItem || org.overengineer.bettacraft.compat.MobCaptures.format(stack) != null) return 0; // M31 part d
        FishContainers.Foreign f = FishContainers.kind(stack);
        return f != null ? f.waterMb : BUCKET_MB;
    }

    /** Right-click on a tank with a mob bucket or a filled fish bag. PASS if the stack is neither. */
    public static InteractionResult tryInsert(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        EntityType<?> type = containedType(stack);
        if (type == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ServerLevel server = (ServerLevel) level;
        TankBlockEntity controller = TankMultiblock.getController(level, pos);
        if (controller == null) {
            player.sendOverlayMessage(Component.translatable("message.bettacraft.tank.not_formed"));
            return InteractionResult.CONSUME;
        }
        if (TankMoves.travelling(level, pos) && !TankMoves.swimsOn(type)) { // M32 part b: on an airship the animals travel packed (M50: ours swim on)
            player.sendOverlayMessage(Component.translatable("message.bettacraft.travel.travelling"));
            return InteractionResult.CONSUME;
        }
        Component error = insert(server, controller, stack, type, player, carriedWater(stack));
        if (error != null) {
            player.sendOverlayMessage(error);
            return InteractionResult.CONSUME;
        }
        ItemStack empty = stack.getItem() instanceof org.overengineer.bettacraft.big.TransportTubItem ? new ItemStack(ModItems.TRANSPORT_TUB.get()) // M30
                : stack.getItem() instanceof org.overengineer.bettacraft.terrarium.AnimalBoxItem || stack.getItem() instanceof org.overengineer.bettacraft.terrarium.TerrariumBoxItem
                        ? new ItemStack(ModItems.TERRARIUM_BOX.get()) // M31 part b (M35: a box of a foreign animal)
                : stack.getItem() instanceof org.overengineer.bettacraft.mobs.MobJarItem ? new ItemStack(ModItems.MOB_JAR.get()) // M31 part d
                : org.overengineer.bettacraft.compat.MobCaptures.format(stack) != null ? org.overengineer.bettacraft.compat.MobCaptures.emptied(stack)
                : stack.getItem() instanceof FishBagItem ? new ItemStack(ModItems.FISH_BAG.get())
                : FishContainers.kind(stack) != null ? FishContainers.emptied(stack) : new ItemStack(Items.BUCKET);
        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, empty));
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_FISH, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
        return InteractionResult.CONSUME;
    }

    /** As {@link #insert(ServerLevel, TankBlockEntity, ItemStack, EntityType, Player, long)} with bucket or bag water. */
    @Nullable
    public static Component insert(ServerLevel level, TankBlockEntity controller, ItemStack stack, EntityType<?> type,
                                   @Nullable Player player, boolean bag) {
        return insert(level, controller, stack, type, player, bag ? BAG_MB : BUCKET_MB);
    }

    /** Spawns the contained animal in the tank (adding {@code addMb} water); returns an error message or null on success. */
    @Nullable
    public static Component insert(ServerLevel level, TankBlockEntity controller, ItemStack stack, EntityType<?> type,
                                   @Nullable Player player, long addMb) {
        TankData tank = controller.getTank();
        if (player != null && !canModify(player, tank)) return Component.translatable("message.bettacraft.tank.locked");
        // M39 (question E1): an unsuitable animal (deep sea, too big, a fungus ...) is refused with its reason before anything else
        if (org.overengineer.bettacraft.foreign.ForeignProfiles.refusal(type) != null && accepts(type, null) == Acceptance.REFUSED) return refusedMessage(type);
        double minFill = ServerConfig.getOr(ServerConfig.TANK_MIN_FILL_TO_INSERT, 0.5);
        // M31: a terrarium animal goes into the tank mode it lives in (a terrarium needs no water), not by the fill
        boolean terra = org.overengineer.bettacraft.terrarium.TerrariumAnimals.isTerrariumAnimal(type);
        if (terra) {
            Component no = org.overengineer.bettacraft.terrarium.TerrariumAnimals.refusal(level, controller, type);
            if (no != null) return no;
            Component lid = org.overengineer.bettacraft.foreign.ForeignHabits.lidRefusal(level, controller, type); // M39 part b: fliers need a lid
            if (lid != null) return lid;
        } else if (org.overengineer.bettacraft.terrarium.PaludariumWater.applies(level, controller)) {
            // M35 part b: a water animal in a paludarium goes by its water part (deep enough for it; the stocking and the size
            // rule of the big animals below measure the water), not by the fill of the whole tank (a third of water is normal there)
            Component no = org.overengineer.bettacraft.terrarium.PaludariumWater.refusal(tank, type);
            if (no != null) return no;
        } else if (tank.capacityMb() <= 0 || tank.waterMb() < minFill * tank.capacityMb()) {
            return Component.translatable("message.bettacraft.tank.too_little_water", Math.round(minFill * 100));
        }
        Acceptance pre = accepts(type, null);
        // M31 part b: a young animal of a big species in a bag (a sea turtle hatchling) is judged by its own size below
        boolean ownSize = pre == Acceptance.TOO_BIG && (FishContainers.storedEntity(stack) != null || FishBagItem.storedEntity(stack) != null);
        if (pre != Acceptance.OK && pre != Acceptance.BIG && !ownSize) return rejection(pre, type);
        // M30: a big animal needs a tank big enough for it (and the other big ones in it); said before the water
        if (pre == Acceptance.BIG) {
            Component big = org.overengineer.bettacraft.big.BigAnimals.refusal(tank, liveInhabitants(level, tank), type, null);
            if (big != null) return big;
        }
        if (type == EntityType.TURTLE) { // M35: a sea turtle needs a sand beach for its eggs
            Component beach = org.overengineer.bettacraft.terrarium.SeaTurtles.refusal(level, controller);
            if (beach != null) return beach;
        }
        // M20: an animal that would die quickly in this water (freshwater fish into salt water and the other way round)
        var salt = org.overengineer.bettacraft.reef.SaltTolerance.of(type);
        if (org.overengineer.bettacraft.reef.SaltTolerance.refuse(salt, tank)) {
            return Component.translatable(org.overengineer.bettacraft.reef.SaltTolerance.refuseKey(salt), type.getDescription(),
                    org.overengineer.bettacraft.reef.ReefItems.fmt(tank.salinity()));
        }
        List<Entity> live = liveInhabitants(level, tank);
        int maxEntities = ServerConfig.getOr(ServerConfig.TANK_MAX_ENTITIES, 64);
        if (live.size() >= maxEntities) return Component.translatable("message.bettacraft.tank.max_entities", maxEntities);

        Vec3 spawn = terra ? org.overengineer.bettacraft.terrarium.TerrariumAnimals.spawnPos(level, tank, type, level.getRandom()) : null;
        if (spawn == null && tank.terrariumMode().hasLand() && org.overengineer.bettacraft.terrarium.TankClimate.enabled()) { // M35 part b: in the water part, not in the land
            spawn = org.overengineer.bettacraft.terrarium.PaludariumWater.waterSpot(level, tank, level.getRandom(), type.getDimensions().height());
        }
        if (spawn == null) spawn = findSpawnPos(level, tank, level.getRandom());
        CompoundTag stored = FishContainers.storedEntity(stack);
        if (stored == null) stored = FishBagItem.storedEntity(stack); // M16: a water animal without a bucket in our fish bag
        if (stored == null) stored = org.overengineer.bettacraft.big.TransportTubItem.storedEntity(stack); // M30: a big animal in its tub
        if (stored == null) stored = org.overengineer.bettacraft.terrarium.TerrariumBoxItem.storedEntity(stack); // M35: a land animal of another mod in its box
        if (stored == null) stored = org.overengineer.bettacraft.mobs.MobJarItem.storedEntity(stack); // M31 part d: a mob in a jar
        if (stored == null) stored = org.overengineer.bettacraft.compat.MobCaptures.storedEntity(stack); // M31 part d: soul vial, imprisonment tool
        Entity e;
        if (stored != null) {
            e = FishContainers.create(level, stored); // Better Fish Tanks bag / Fintastic net: the complete entity
        } else {
            @SuppressWarnings("unchecked")
            EntityType<Entity> anyType = (EntityType<Entity>) type;
            Consumer<Entity> config = EntityType.createDefaultStackConfig(level, stack, player);
            e = createFromBucket(level, anyType, config, spawn);
        }
        if (e == null || e.getType() != type) {
            if (e != null) e.discard();
            return Component.translatable("message.bettacraft.tank.rejected", type.getDescription());
        }
        if (e instanceof net.minecraft.world.entity.Mob glass && org.overengineer.bettacraft.mobs.GlassMobs.isAllowed(type)) {
            org.overengineer.bettacraft.mobs.GlassMobs.prepare(glass); // M31 part d: shrunk, harmless, without drops
        }
        if (e instanceof Bucketable b) {
            if (stored == null) {
                CompoundTag data = stack.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).copyTag();
                b.loadFromBucketTag(data);
                AxolotlCare.fromBucket(e, data); // M29 part d: the colour genes of an axolotl
            }
            b.setFromBucket(true);
        }
        // M16: after the bucket data, which can change the size (Fintastic gouramis: giant, three-spot or dwarf variant)
        Acceptance acc = accepts(type, e);
        if (acc != Acceptance.OK && acc != Acceptance.BIG) {
            e.discard();
            return rejection(acc, type);
        }
        if (acc == Acceptance.BIG) { // M30: with its own size (a young one of another mod is judged by the adult size)
            Component big = org.overengineer.bettacraft.big.BigAnimals.refusal(tank, live, type, e);
            if (big != null) {
                e.discard();
                return big;
            }
        }
        if (weight(e) > 0) { // M18: the young count in fractions too
            long waterAfter = tank.waterMb() + Math.max(0, org.overengineer.bettacraft.terrarium.WaterLimit.clamp(level, controller, addMb)); // M33.1: the limit of the type
            int cap = hardCap(waterAfter / (double) TankData.MB_PER_BLOCK);
            if (terra) cap = hardCap(waterAfter / (double) TankData.MB_PER_BLOCK + org.overengineer.bettacraft.terrarium.TerrariumAnimals.landBlocks(tank)); // M31
            // M14: the small animals and the frogs count by config (stocking.*PerFish)
            if (stocking(live, tank) + weight(e) > cap + 1e-6) {
                e.discard();
                return Component.translatable("message.bettacraft.tank.crowded_full", cap);
            }
        }
        // keep the hitbox clear of the glass walls and the lid: an entity that starts inside a wall's collision box is not
        // held by it and could slip through (out of the tank, or into the notch of a free shape)
        double half = e.getBbWidth() / 2 + 1.0 / 16 + 0.01;
        if (!terra) spawn = new TankWaterVolume(tank.members(), Math.max(tank.waterSurfaceY(), tank.min().getY() + 0.9), TankCare.waterBox(tank))
                .clamp(spawn, half, 1.0 / 16 + 0.01, Math.min(0.3, e.getBbHeight()), e.getBbHeight());
        e.snapTo(spawn.x, spawn.y, spawn.z, level.getRandom().nextFloat() * 360F, 0F);
        adopt(e, controller);
        // M17: the animal must really join the level (a UUID clash or another mod cancelling the join): otherwise the
        // container stays full instead of losing the animal
        if (!level.tryAddFreshEntityWithPassengers(e) || !org.overengineer.bettacraft.platform.Platform.get().isAddedToLevel(e) || e.isRemoved()) {
            if (!e.isRemoved()) e.discard();
            return Component.translatable("message.bettacraft.tank.rejected", type.getDescription());
        }
        addMb = org.overengineer.bettacraft.terrarium.WaterLimit.clamp(level, controller, addMb); // M33.1: the water of the container spills beside a fixed terrarium / paludarium
        if (addMb > 0) TankInteractions.changeWater(level, controller, addMb, tank.salinity()); // M20: the animal comes in water of its tank
        tank.inhabitants().add(e.getUUID());
        TankCare.log(level, controller, "log.bettacraft.inserted", TankCare.nameArg(e));
        if (e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity) org.overengineer.bettacraft.progression.Progress.award(player, org.overengineer.bettacraft.progression.Progress.INSERTED);
        org.overengineer.bettacraft.progression.Progress.onInserted(player, level, controller, e, acc == Acceptance.BIG); // M45
        reconcile(level, controller);
        controller.markTankChanged(); // clients learn the inhabitant count (break rule without flicker)
        return null;
    }

    /**
     * Creates the animal of a bucket like the bucket would (with {@code finalizeSpawn}). M16: some mods swap an animal for
     * one of their own while it spawns (Fintastic replaces vanilla cod and salmon, at a random spot nearby, or not at all);
     * then the original is created again without that step and whatever joined the level meanwhile is removed, so the
     * animal from the bucket is the one that lives in the tank.
     */
    @Nullable
    static Entity createFromBucket(ServerLevel level, EntityType<Entity> type, Consumer<Entity> config, Vec3 spawn) {
        List<Entity> joined = new ArrayList<>();
        Entity e;
        ForeignAnimals.capture(joined);
        try {
            e = type.create(level, config, BlockPos.containing(spawn), EntitySpawnReason.BUCKET, false, false);
        } finally {
            ForeignAnimals.capture(null);
        }
        if (e != null && e.isRemoved()) {
            LOGGER.info("{} was replaced while spawning ({}); putting the original into the tank",
                    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type), joined.stream().map(j -> j.getType().toShortString()).toList());
            joined.forEach(Entity::discard);
            e = type.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
            if (e != null) {
                config.accept(e);
                e.snapTo(spawn.x, spawn.y, spawn.z, 0, 0);
            }
        }
        return e;
    }

    /** The message for a refused animal (M16: one per reason). */
    public static Component rejection(Acceptance a, EntityType<?> type) {
        return switch (a) {
            case TOO_BIG -> Component.translatable("message.bettacraft.tank.too_big", type.getDescription());
            case BIG -> Component.translatable("message.bettacraft.big.no_bucket", type.getDescription()); // M30: no bucket, no bag
            case BOSS -> Component.translatable("message.bettacraft.tank.boss", type.getDescription());
            case HOSTILE -> Component.translatable("message.bettacraft.tank.hostile", type.getDescription());
            case NOT_AQUATIC -> Component.translatable("message.bettacraft.tank.not_aquatic", type.getDescription());
            case BLACKLISTED -> Component.translatable("message.bettacraft.tank.blacklisted", type.getDescription());
            case REFUSED -> refusedMessage(type); // M39
            default -> Component.translatable("message.bettacraft.tank.rejected", type.getDescription());
        };
    }

    /**
     * M39 (question E1): why our tanks refuse this animal of another mod: {@code message.bettacraft.tank.refused.<reason>} of its
     * profile, the general one for a reason without a text of its own.
     */
    public static Component refusedMessage(EntityType<?> type) {
        String why = org.overengineer.bettacraft.foreign.ForeignProfiles.refusal(type);
        if (why == null || !org.overengineer.bettacraft.foreign.ForeignProfile.REFUSE_REASONS.contains(why)) why = "unsuitable";
        return Component.translatable("message.bettacraft.tank.refused." + why, type.getDescription());
    }

    /** Random position inside the water of the tank (in a block that has water). */
    public static Vec3 findSpawnPos(ServerLevel level, TankData tank, RandomSource r) {
        List<BlockPos> wet = new ArrayList<>();
        for (long l : tank.members()) {
            BlockPos p = BlockPos.of(l);
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof TankBlock && s.getValue(TankBlock.FILL) > 0) wet.add(p);
        }
        if (wet.isEmpty()) {
            BlockPos mn = tank.min();
            return new Vec3(mn.getX() + 0.5, mn.getY() + 0.1, mn.getZ() + 0.5);
        }
        // prefer the lowest layer, which is always completely filled
        wet.sort(Comparator.comparingInt(BlockPos::getY));
        int lowest = wet.getFirst().getY();
        List<BlockPos> bottom = wet.stream().filter(p -> p.getY() == lowest).toList();
        BlockPos p = r.nextInt(3) == 0 ? wet.get(r.nextInt(wet.size())) : bottom.get(r.nextInt(bottom.size()));
        float fill = level.getBlockState(p).getValue(TankBlock.FILL) / 16F;
        double y = p.getY() + 0.1 + r.nextDouble() * Math.max(0, fill - 0.5);
        return new Vec3(p.getX() + 0.3 + r.nextDouble() * 0.4, y, p.getZ() + 0.3 + r.nextDouble() * 0.4);
    }

    /** Marks an entity as inhabitant of this tank: tank_ref, persistence, no despawning. */
    public static void adopt(Entity e, TankBlockEntity controller) {
        ModAttachments.TANK_REF.set(e, new TankRef(controller.getBlockPos(), controller.getTank().id()));
        if (e instanceof Mob m) m.setPersistenceRequired();
        if (e instanceof Bucketable b) b.setFromBucket(true);
    }

    public static void release(Entity e) {
        // M35 part b: a mother of another mod with laid eggs takes her clutch along (the egg block goes, she lays them again)
        if (e instanceof net.minecraft.world.entity.LivingEntity le && e.level() instanceof ServerLevel && hasTankRef(e)) {
            org.overengineer.bettacraft.foreign.ForeignNests.leave(le);
        }
        ModAttachments.TANK_REF.remove(e);
    }

    // ------------------------------------------------------------------ queries

    public static boolean hasTankRef(Entity e) {
        return ModAttachments.TANK_REF.has(e) && ModAttachments.TANK_REF.get(e).isSet();
    }

    /**
     * Whether an entity is inside the tank's blocks (M10: the actual shape, not the bounding box; a frog sitting on the
     * water surface right under the open top still counts).
     */
    public static boolean inside(TankData tank, Entity e) {
        BlockPos p = e.blockPosition();
        return tank.contains(p) || (tank.contains(p.below()) && e.getY() - p.getY() < 0.5);
    }

    public static AABB tankBox(TankData tank) {
        BlockPos mn = tank.min(), mx = tank.max();
        return new AABB(mn.getX(), mn.getY(), mn.getZ(), mx.getX() + 1, mx.getY() + 1, mx.getZ() + 1);
    }

    /** Living entities that belong to this tank and are inside it. */
    public static List<Entity> liveInhabitants(ServerLevel level, TankData tank) {
        UUID id = tank.id();
        return level.getEntities((Entity) null, tankBox(tank), e -> e.isAlive() && hasTankRef(e)
                && id.equals(ModAttachments.TANK_REF.get(e).tankId()));
    }

    public static boolean isAdult(Entity e) {
        if (e instanceof BettaEntity b) return b.isAdult();
        if (e instanceof org.overengineer.bettacraft.entity.tankmate.TankmateEntity m) return m.isAdult();
        if (e instanceof org.overengineer.bettacraft.entity.frog.DwarfFrogEntity f) return f.isAdult(); // M47: froglets
        return !(e instanceof LivingEntity le) || !le.isBaby();
    }

    /**
     * How much one inhabitant counts for the stocking: adults 1, frogs 1 / stocking.frogsPerFish. M18: young bettas by
     * their life stage ({@link org.overengineer.bettacraft.entity.betta.LifeStage#stocking}: fry 0.02, juvenile 0.1,
     * subadult 0.3), young animals of other mods (and vanilla) like a juvenile betta.
     */
    public static double weight(Entity e) {
        if (e instanceof BettaEntity b) return b.lifeStage().stocking();
        // M19: tankmates by their species (a neon 0.35 ... a clown loach 2.5) and their stage like the bettas
        if (e instanceof org.overengineer.bettacraft.entity.tankmate.TankmateEntity m) {
            return m.species().stock() * ServerConfig.getOr(ServerConfig.TANKMATES_STOCKING, 1.0) * m.lifeStage().stocking();
        }
        // M30: a big animal of another kind (the dolphin) like a big species: a quarter of the water blocks it needs
        if (org.overengineer.bettacraft.mobs.GlassMobs.isGlassMob(e)) return org.overengineer.bettacraft.mobs.GlassMobs.weight(e); // M31 part d
        if (e instanceof org.overengineer.bettacraft.terrarium.TerrariumAnimal ta) return ta instanceof org.overengineer.bettacraft.terrarium.HerpEntity ? ta.stockWeight() : ta.stockWeight() * (isAdult(e) ? 1.0 : 0.25); // M31 (part b: a turtle by its growth)
        double big = org.overengineer.bettacraft.big.BigAnimals.length(e.getType(), e);
        if (big > 0) return 25.0 * big * big * big;
        var fp = org.overengineer.bettacraft.foreign.ForeignProfiles.cared(e); // M32 part a: the stocking of its profile
        if (fp != null && !Double.isNaN(fp.stock())) return isAdult(e) ? fp.stock() : fp.stock() * org.overengineer.bettacraft.entity.betta.LifeStage.JUVENILE.stocking();
        double w = e instanceof org.overengineer.bettacraft.entity.frog.DwarfFrogEntity
                ? 1.0 / Math.max(0.1, ServerConfig.getOr(ServerConfig.STOCKING_FROGS_PER_FISH, 1.0)) : 1.0;
        return isAdult(e) ? w : w * org.overengineer.bettacraft.entity.betta.LifeStage.JUVENILE.stocking();
    }

    /**
     * M14: stocking of a tank in fish: inhabitants (frogs by config, M18: the young in fractions), the small animals
     * (stocking.*PerFish) and (M18) the eggs and larvae in the bubble nest (0 by default).
     */
    public static double stocking(List<Entity> live, TankData tank) {
        return breakdown(live, tank).total();
    }

    /** M18: the stocking in its parts (shown in the menu): adult and young inhabitants, small animals, the nest. */
    public record Stocking(double adults, double young, double critters, double nest) {
        public double total() {
            return adults + young + critters + nest;
        }
    }

    public static Stocking breakdown(List<Entity> live, TankData tank) {
        double adults = 0, young = 0;
        for (Entity e : live) {
            if (isAdult(e)) adults += weight(e);
            else young += weight(e);
        }
        return new Stocking(adults, young, org.overengineer.bettacraft.critters.TankCritters.stock(tank), nestStocking(tank));
    }

    /** M18: eggs and larvae in the bubble nest (stocking.eggStocking / larvaStocking, 0 by default). */
    public static double nestStocking(TankData tank) {
        BubbleNest n = tank.nest();
        if (n == null || !n.hasEggs()) return 0;
        return n.eggCount() * n.stage().stocking();
    }

    public static double stocking(ServerLevel level, TankData tank) {
        return stocking(liveInhabitants(level, tank), tank);
    }

    public static int countAdults(List<Entity> list) {
        int n = 0;
        for (Entity e : list) if (isAdult(e)) n++;
        return n;
    }

    /** Hard limit of adults for the given number of water blocks (6 per block by default). */
    public static int hardCap(double waterBlocks) {
        return (int) Math.floor(ServerConfig.getOr(ServerConfig.TANK_HARD_CAP_PER_WATER_BLOCK, 6) * waterBlocks + 1e-6);
    }

    /** Overcrowded from 2 adults per water block (default); one betta per block is fine. */
    public static boolean isOvercrowded(int adults, double waterBlocks) {
        return isOvercrowded((double) adults, waterBlocks);
    }

    /** M14: with the stocking in fish (small animals count in fractions). */
    public static boolean isOvercrowded(double stock, double waterBlocks) {
        int per = ServerConfig.getOr(ServerConfig.TANK_OVERCROWDED_PER_WATER_BLOCK, 2);
        return stock >= 2 - 1e-6 && stock >= per * waterBlocks - 1e-6;
    }

    // ------------------------------------------------------------------ reconciliation

    /**
     * Matches the tank's inhabitant list with the entities in the tank. Entities with a tank_ref inside the tank are
     * (re)assigned to it (merges/splits change ids), entities of this tank found outside are released, and ids that
     * stay missing are forgotten. Updates the overcrowding flag.
     */
    public static void reconcile(ServerLevel level, TankBlockEntity controller) {
        TankData tank = controller.getTank();
        AABB box = tankBox(tank);
        TankRef ours = new TankRef(controller.getBlockPos(), tank.id());
        Set<UUID> found = new HashSet<>();
        double adults = org.overengineer.bettacraft.critters.TankCritters.stock(tank) + nestStocking(tank);
        double land = 0; // M31: the terrarium animals count against the land (only they)
        boolean evict = ServerConfig.getOr(ServerConfig.INHABITANTS_OUTGROWN, ServerConfig.Outgrown.BAG_IN_FRONT) == ServerConfig.Outgrown.BAG_IN_FRONT;
        for (Entity e : level.getEntities((Entity) null, box, e -> e.isAlive() && (hasTankRef(e) || adoptable(e)))) {
            if (!tank.contains(e.blockPosition())) continue;
            // M17: a young animal of a big species that grew past inhabitants.maxHitbox in the tank goes out in a bag
            // M30: a big animal stays (the size rule only decides when it goes in; with features.bigTanks.bigAnimals off it stays too)
            if (evict && !org.overengineer.bettacraft.big.BigAnimals.isBig(e) && accepts(e.getType(), e) == Acceptance.TOO_BIG) {
                evictOutgrown(level, controller, e);
                continue;
            }
            if (!ours.equals(ModAttachments.TANK_REF.get(e))) adopt(e, controller);
            found.add(e.getUUID());
            if (org.overengineer.bettacraft.terrarium.TerrariumAnimals.isTerrariumAnimal(e.getType())) land += weight(e);
            else adults += weight(e);
        }
        for (Entity e : level.getEntities((Entity) null, box.inflate(6), e -> hasTankRef(e) && !inside(tank, e))) {
            if (ours.equals(ModAttachments.TANK_REF.get(e))) release(e); // left the tank (e.g. teleported)
        }
        boolean changed = false;
        Set<UUID> keep = new HashSet<>(found);
        for (UUID u : tank.inhabitants()) {
            if (found.contains(u)) {
                controller.missing.removeInt(u);
                continue;
            }
            Entity e = level.getEntity(u);
            if (e != null && (!e.isAlive() || !inside(tank, e))) {
                controller.missing.removeInt(u);
                changed = true;
                continue;
            }
            int n = controller.missing.getInt(u) + 1;
            if (n >= MISSING_LIMIT) {
                controller.missing.removeInt(u);
                changed = true;
            } else {
                controller.missing.put(u, n);
                keep.add(u);
            }
        }
        if (!keep.equals(tank.inhabitants())) {
            changed = true;
            tank.inhabitants().clear();
            tank.inhabitants().addAll(keep);
        }
        controller.setCrowded(isOvercrowded(adults, tank.systemBlocks())
                || land > 0 && isOvercrowded(land, tank.systemBlocks() + org.overengineer.bettacraft.terrarium.TerrariumAnimals.landBlocks(tank)));
        if (changed) controller.markTankChanged();
    }

    /**
     * M17: puts an inhabitant that grew too big into a filled fish bag (bucket data or its full data) that drops in front
     * of the tank, like a frog that grew from a tadpole. The bag comes from nothing: a kelp bag, the animal is kept.
     */
    static void evictOutgrown(ServerLevel level, TankBlockEntity controller, Entity e) {
        TankData tank = controller.getTank();
        release(e);
        ItemStack bag = org.overengineer.bettacraft.entity.betta.FishBagItem.fillWith(e);
        Vec3 at = ForeignAnimals.inFront(level, tank);
        net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(level, at.x, at.y, at.z, bag);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
        TankCare.log(level, controller, "log.bettacraft.outgrown", TankCare.nameArg(e));
        LOGGER.info("{} outgrew tank {} and was bagged in front of it", e.getType().getDescriptionId(), tank.id());
        tank.inhabitants().remove(e.getUUID());
        e.discard();
        controller.markTankChanged();
    }

    /** Small water animals that got into the tank without a bucket (spawn egg, /summon) become inhabitants. */
    private static boolean adoptable(Entity e) {
        // M16: also water animals without a bucket (a squid from a spawn egg)
        if (org.overengineer.bettacraft.mobs.GlassMobs.isAllowed(e.getType())) return false; // M31 part d: a mob comes in by a jar or a bait, shrunk
        return e instanceof Mob && (e instanceof Bucketable || isWaterAnimal(e)) && !(e instanceof net.minecraft.world.entity.player.Player)
                && e.isInWater() && accepts(e.getType(), e) == Acceptance.OK;
    }

    /**
     * Controller of the tank an entity lives in. Repairs the reference after merges/splits and releases the entity if
     * its tank no longer exists. Null if the entity is not (or no longer) in a tank.
     */
    @Nullable
    public static TankBlockEntity resolveController(ServerLevel level, Entity e) {
        if (!hasTankRef(e)) return null;
        TankRef ref = ModAttachments.TANK_REF.get(e);
        if (level.isLoaded(ref.controller())) {
            TankBlockEntity be = TankMultiblock.getBE(level, ref.controller());
            if (be != null && be.isController() && be.getTank().id().equals(ref.tankId()) && be.getTank().contains(e.blockPosition())) {
                return be;
            }
        }
        TankBlockEntity here = TankMultiblock.getController(level, e.blockPosition());
        if (here != null) {
            adopt(e, here);
            return here;
        }
        if (level.isLoaded(ref.controller())) release(e); // tank is gone (e.g. exploded)
        return null;
    }

    // ------------------------------------------------------------------ taking out

    /** Takes an inhabitant out with an empty bucket (-1000 mB) or fish bag (-100 mB) from the player's inventory. */
    @Nullable
    public static Component takeOut(ServerPlayer player, TankBlockEntity controller, UUID entityId, boolean bag) {
        ServerLevel level = player.level();
        TankData tank = controller.getTank();
        if (!canModify(player, tank)) return Component.translatable("message.bettacraft.tank.locked");
        Entity e = level.getEntity(entityId);
        if (e == null || !e.isAlive() || !hasTankRef(e) || !tank.id().equals(ModAttachments.TANK_REF.get(e).tankId())) {
            return Component.translatable("message.bettacraft.tank.not_inside");
        }
        // M31 part d: a mob in glass only goes into an empty mob jar (whichever button)
        if (org.overengineer.bettacraft.mobs.GlassMobs.isGlassMob(e)) {
            int jar = -1;
            for (int i = 0; i < player.getInventory().getContainerSize() && jar < 0; i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (s.getItem() instanceof org.overengineer.bettacraft.mobs.MobJarItem && !org.overengineer.bettacraft.mobs.MobJarItem.isFilled(s)) jar = i;
            }
            if (jar < 0) return Component.translatable("message.bettacraft.glass.need_jar", e.getName());
            release(e);
            org.overengineer.bettacraft.containers.AnimalContainers.give(player, jar, org.overengineer.bettacraft.mobs.MobJarItem.fillWith(e));
            TankCare.log(level, controller, "log.bettacraft.taken_out", TankCare.nameArg(e));
            tank.inhabitants().remove(e.getUUID());
            e.discard();
            level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.2F);
            reconcile(level, controller);
            return null;
        }
        // M35: a land animal of another mod with a box profile goes into an empty terrarium box first (whichever button)
        if (org.overengineer.bettacraft.foreign.ForeignTerrarium.boxTakes(e)) {
            int box = -1;
            for (int i = 0; i < player.getInventory().getContainerSize() && box < 0; i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (s.is(ModItems.TERRARIUM_BOX.get()) && !org.overengineer.bettacraft.terrarium.TerrariumBoxItem.isFilled(s)) box = i;
            }
            if (box >= 0) {
                release(e);
                org.overengineer.bettacraft.containers.AnimalContainers.give(player, box, org.overengineer.bettacraft.terrarium.TerrariumBoxItem.fillWith(e));
                TankCare.log(level, controller, "log.bettacraft.taken_out", TankCare.nameArg(e));
                tank.inhabitants().remove(e.getUUID());
                e.discard();
                level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                reconcile(level, controller);
                return null;
            }
            if (!org.overengineer.bettacraft.big.BigAnimals.isBig(e)) return Component.translatable("message.bettacraft.box.need_box", e.getName());
        }
        // M30: a big animal only goes into an empty transport tub (whichever button), with all its data
        if (org.overengineer.bettacraft.big.BigAnimals.isBig(e)) {
            int tub = -1;
            for (int i = 0; i < player.getInventory().getContainerSize() && tub < 0; i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (s.getItem() instanceof org.overengineer.bettacraft.big.TransportTubItem && !org.overengineer.bettacraft.big.TransportTubItem.isFilled(s)) tub = i;
            }
            if (tub < 0) return Component.translatable("message.bettacraft.big.need_tub", e.getName());
            release(e);
            org.overengineer.bettacraft.containers.AnimalContainers.give(player, tub, org.overengineer.bettacraft.big.TransportTubItem.fillWith(e));
            TankCare.log(level, controller, "log.bettacraft.taken_out", TankCare.nameArg(e));
            tank.inhabitants().remove(e.getUUID());
            e.discard();
            level.playSound(null, player.blockPosition(), SoundEvents.BUCKET_FILL_FISH, SoundSource.NEUTRAL, 1.0F, 0.7F);
            reconcile(level, controller);
            return null;
        }
        // M16: water animals without a bucket (squids ...) go into bags, nets and Better Fish Tanks bags with their full data
        Bucketable bucketable = e instanceof Bucketable b ? b : null;
        if (bucketable == null && !bag) return Component.translatable("message.bettacraft.tank.cannot_take", e.getName());
        // M22.3: a bucket or bag of the same species with room, a bucket of water that suits the animal, then an empty one
        var tm = e instanceof org.overengineer.bettacraft.entity.tankmate.TankmateEntity m ? m.species() : null;
        var range = org.overengineer.bettacraft.reef.SaltTolerance.of(e);
        var ours = org.overengineer.bettacraft.containers.AnimalContainers.findForFish(player, tm, range, !bag);
        int slot;
        long cost;
        if (ours != null) {
            slot = ours.slot();
            cost = ours.waterMb();
        } else {
            slot = findContainer(player, bag);
            if (slot < 0) {
                boolean wrongWater = !bag && hasWaterBucket(player);
                return Component.translatable(wrongWater ? (range != null && range.marine() ? "message.bettacraft.container.needs_sea_water"
                        : "message.bettacraft.container.needs_fresh_water") : bag ? "message.bettacraft.tank.need_bag" : "message.bettacraft.tank.need_bucket");
            }
            cost = containerWater(player.getInventory().getItem(slot));
        }
        ItemStack container = player.getInventory().getItem(slot);
        if (tank.waterMb() < cost) return Component.translatable("message.bettacraft.tank.not_enough_water");
        release(e);
        net.minecraft.sounds.SoundEvent pickup = bucketable != null ? bucketable.getPickupSound() : SoundEvents.BUCKET_FILL_FISH;
        if (ours != null && ours.mode() == org.overengineer.bettacraft.containers.AnimalContainers.Mode.ADD && tm != null) {
            ItemStack tmp = bucketable.getBucketItemStack();
            bucketable.saveToBucketTag(tmp);
            org.overengineer.bettacraft.containers.AnimalContainers.addFish(player, slot, tm,
                    tmp.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).copyTag());
        } else {
            ItemStack filled;
            if (FishContainers.kind(container) != null) {
                filled = FishContainers.fill(container, e, player); // Better Fish Tanks bag / Fintastic net
            } else if (bag) {
                filled = FishBagItem.fillWith(e);
            } else {
                filled = bucketable.getBucketItemStack();
                bucketable.saveToBucketTag(filled);
                if (e instanceof net.minecraft.world.entity.animal.axolotl.Axolotl) { // M29 part d: keep the colour genes
                    CompoundTag data = filled.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).copyTag();
                    AxolotlCare.toBucket(e, data);
                    filled.set(DataComponents.BUCKET_ENTITY_DATA, CustomData.of(data));
                }
            }
            org.overengineer.bettacraft.containers.AnimalContainers.give(player, slot, filled);
        }
        TankCare.log(level, controller, "log.bettacraft.taken_out", TankCare.nameArg(e));
        tank.inhabitants().remove(e.getUUID());
        e.discard();
        if (cost > 0) TankInteractions.changeWater(level, controller, -cost);
        level.playSound(null, player.blockPosition(), pickup, SoundSource.NEUTRAL, 1.0F, 1.0F);
        reconcile(level, controller);
        return null;
    }

    /**
     * Slot of an empty bucket, or for {@code bag} of an empty fish bag in the player's inventory, or -1. Our fish bag
     * comes first, then the Better Fish Tanks fish bag, then the Fintastic fishnet (M7).
     */
    public static int findContainer(Player player, boolean bag) {
        var inv = player.getInventory();
        if (!bag) {
            for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(Items.BUCKET)) return i;
            return -1;
        }
        int best = -1, bestRank = Integer.MAX_VALUE;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            int rank = s.getItem() instanceof FishBagItem && !FishBagItem.isFilled(s) ? 0
                    : FishContainers.isEmptyContainer(s) ? 1 + FishContainers.kind(s).ordinal() : Integer.MAX_VALUE;
            if (rank < bestRank) {
                bestRank = rank;
                best = i;
            }
        }
        return best;
    }

    /** M22.3: a bucket of water (plain or sea water) in the inventory. */
    static boolean hasWaterBucket(Player player) {
        for (ItemStack s : player.getInventory().getNonEquipmentItems()) if (org.overengineer.bettacraft.containers.AnimalContainers.waterBucket(s)) return true;
        return false;
    }

    /** Water an (empty) container takes out of the tank together with the animal. */
    public static long containerWater(ItemStack container) {
        if (container.is(Items.BUCKET)) return BUCKET_MB;
        FishContainers.Foreign f = FishContainers.kind(container);
        return f != null ? f.waterMb : BAG_MB;
    }

    /** Water the bag button would take (with the container {@link #findContainer} picks), or -1 without one. */
    public static long bagWater(Player player) {
        int slot = findContainer(player, true);
        return slot < 0 ? -1 : containerWater(player.getInventory().getItem(slot));
    }

    // ------------------------------------------------------------------ break rule

    /**
     * Reason why the tank block at {@code pos} must not be broken: a player in its interior (M15), living inhabitants or
     * eggs inside (PLAN 4.1).
     */
    public static Optional<Component> breakBlockedReason(Level level, BlockPos pos) {
        TankBlockEntity controller = TankMultiblock.getController(level, pos);
        if (controller == null) return Optional.empty();
        TankData tank = controller.getTank();
        // M15: nobody may break a tank while a player is in its interior (every block of it, also of a free shape)
        if (level instanceof ServerLevel sl) {
            List<ServerPlayer> inside = org.overengineer.bettacraft.tank.interior.InteriorManager.playersInside(sl.getServer(), tank);
            if (!inside.isEmpty()) {
                Component names = inside.getFirst().getDisplayName();
                for (int i = 1; i < Math.min(3, inside.size()); i++) names = Component.translatable("cause.bettacraft.and", names, inside.get(i).getDisplayName());
                return Optional.of(Component.translatable("message.bettacraft.tank.break_blocked_interior", names));
            }
        } else if (tank.occupants() > 0) {
            return Optional.of(Component.empty());
        }
        BubbleNest nest = tank.nest();
        if (nest != null && nest.hasEggs()) {
            return Optional.of(Component.translatable("message.bettacraft.tank.break_blocked_nest", nest.visibleEggCount()));
        }
        // M19: eggs and young of the tankmates (clutches, the breeding box) keep the tank too
        int young = 0;
        for (var k : tank.clutches()) if (k.hasYoung()) young += k.count;
        if (young > 0) return Optional.of(Component.translatable("message.bettacraft.tank.break_blocked_clutch", young));
        if (tank.breedingBox()) {
            return Optional.of(Component.translatable("message.bettacraft.tank.break_blocked_box"));
        }
        if (level instanceof ServerLevel server) {
            List<Entity> live = liveInhabitants(server, tank);
            if (live.isEmpty()) {
                // M14: shrimp, crayfish and snails keep the tank too
                Component c = org.overengineer.bettacraft.critters.TankCritters.breakBlocked(tank);
                return c == null ? Optional.empty() : Optional.of(c);
            }
            Component names = live.getFirst().getName();
            for (int i = 1; i < Math.min(3, live.size()); i++) names = Component.translatable("cause.bettacraft.and", names, live.get(i).getName());
            return Optional.of(Component.translatable("message.bettacraft.tank.break_blocked", live.size(), names));
        }
        // client: use the synced count so the block does not flicker away
        return tank.inhabitantCount() > 0 || org.overengineer.bettacraft.critters.TankCritters.alive(tank) > 0 ? Optional.of(Component.empty()) : Optional.empty();
    }
}
