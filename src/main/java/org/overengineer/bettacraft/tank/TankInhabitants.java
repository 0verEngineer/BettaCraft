package org.overengineer.bettacraft.tank;

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
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
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
    public static final float MAX_HITBOX = 0.8F;
    /** Reconciliations an inhabitant may be missing (e.g. entity section not loaded yet) before it is forgotten. */
    private static final int MISSING_LIMIT = 6;

    public enum Acceptance { OK, BLACKLISTED, TOO_BIG, NOT_BUCKETABLE }

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
        if (FishContainers.kind(stack) != null) return FishContainers.containedType(stack);
        if (stack.getItem() instanceof MobBucketItem && MOB_BUCKET_TYPE != null) {
            try {
                return (EntityType<?>) MOB_BUCKET_TYPE.get(stack.getItem());
            } catch (IllegalAccessException e) {
                return null;
            }
        }
        return null;
    }

    /** Acceptance rules: blacklist tag, then whitelist tag, then "bucketable and hitbox at most 0.8 x 0.8". */
    public static Acceptance accepts(EntityType<?> type, @Nullable Entity instance) {
        if (type.is(ModTags.TANK_BLACKLIST)) return Acceptance.BLACKLISTED;
        if (type.is(ModTags.TANK_INHABITANTS)) return Acceptance.OK;
        EntityDimensions d = type.getDimensions();
        if (d.width() > MAX_HITBOX || d.height() > MAX_HITBOX) return Acceptance.TOO_BIG;
        if (instance != null && !(instance instanceof Bucketable)) return Acceptance.NOT_BUCKETABLE;
        return Acceptance.OK;
    }

    public static boolean canModify(Player player, TankData tank) {
        return !tank.locked() || player.getUUID().equals(tank.owner()) || player.hasPermissions(2);
    }

    // ------------------------------------------------------------------ inserting

    /** Water that comes along with the animal of this (filled) container: bucket 1000 mB, bags 100 mB, nets 0. */
    public static long carriedWater(ItemStack stack) {
        if (stack.getItem() instanceof FishBagItem) return BAG_MB;
        FishContainers.Foreign f = FishContainers.kind(stack);
        return f != null ? f.waterMb : BUCKET_MB;
    }

    /** Right-click on a tank with a mob bucket or a filled fish bag. PASS if the stack is neither. */
    public static ItemInteractionResult tryInsert(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        EntityType<?> type = containedType(stack);
        if (type == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        ServerLevel server = (ServerLevel) level;
        TankBlockEntity controller = TankMultiblock.getController(level, pos);
        if (controller == null) {
            player.displayClientMessage(Component.translatable("message.bettacraft.tank.not_formed"), true);
            return ItemInteractionResult.CONSUME;
        }
        Component error = insert(server, controller, stack, type, player, carriedWater(stack));
        if (error != null) {
            player.displayClientMessage(error, true);
            return ItemInteractionResult.CONSUME;
        }
        ItemStack empty = stack.getItem() instanceof FishBagItem ? new ItemStack(ModItems.FISH_BAG.get())
                : FishContainers.kind(stack) != null ? FishContainers.emptied(stack) : new ItemStack(Items.BUCKET);
        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, empty));
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_FISH, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
        return ItemInteractionResult.CONSUME;
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
        double minFill = ServerConfig.getOr(ServerConfig.TANK_MIN_FILL_TO_INSERT, 0.5);
        if (tank.capacityMb() <= 0 || tank.waterMb() < minFill * tank.capacityMb()) {
            return Component.translatable("message.bettacraft.tank.too_little_water", Math.round(minFill * 100));
        }
        Acceptance pre = accepts(type, null);
        if (pre != Acceptance.OK) return rejection(pre, type);
        List<Entity> live = liveInhabitants(level, tank);
        int maxEntities = ServerConfig.getOr(ServerConfig.TANK_MAX_ENTITIES, 64);
        if (live.size() >= maxEntities) return Component.translatable("message.bettacraft.tank.max_entities", maxEntities);

        Vec3 spawn = findSpawnPos(level, tank, level.getRandom());
        CompoundTag stored = FishContainers.storedEntity(stack);
        Entity e;
        if (stored != null) {
            e = FishContainers.create(level, stored); // Better Fish Tanks bag / Fintastic net: the complete entity
        } else {
            @SuppressWarnings("unchecked")
            EntityType<Entity> anyType = (EntityType<Entity>) type;
            Consumer<Entity> config = EntityType.createDefaultStackConfig(level, stack, player);
            e = anyType.create(level, config, BlockPos.containing(spawn), MobSpawnType.BUCKET, false, false);
        }
        if (e == null || e.getType() != type) {
            if (e != null) e.discard();
            return Component.translatable("message.bettacraft.tank.rejected", type.getDescription());
        }
        Acceptance acc = accepts(type, e);
        if (acc != Acceptance.OK) {
            e.discard();
            return rejection(acc, type);
        }
        if (e instanceof Bucketable b) {
            if (stored == null) b.loadFromBucketTag(stack.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).copyTag());
            b.setFromBucket(true);
        }
        if (isAdult(e)) {
            long waterAfter = Math.min(tank.capacityMb(), tank.waterMb() + addMb);
            int cap = hardCap(waterAfter / (double) TankData.MB_PER_BLOCK);
            if (countAdults(live) + 1 > cap) {
                e.discard();
                return Component.translatable("message.bettacraft.tank.crowded_full", cap);
            }
        }
        e.moveTo(spawn.x, spawn.y, spawn.z, level.getRandom().nextFloat() * 360F, 0F);
        adopt(e, controller);
        level.addFreshEntityWithPassengers(e);
        if (addMb > 0) TankInteractions.changeWater(level, controller, addMb);
        tank.inhabitants().add(e.getUUID());
        TankCare.log(level, controller, "log.bettacraft.inserted", TankCare.nameArg(e));
        if (e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity) org.overengineer.bettacraft.progression.Progress.award(player, org.overengineer.bettacraft.progression.Progress.INSERTED);
        reconcile(level, controller);
        controller.markTankChanged(); // clients learn the inhabitant count (break rule without flicker)
        return null;
    }

    private static Component rejection(Acceptance a, EntityType<?> type) {
        return switch (a) {
            case TOO_BIG -> Component.translatable("message.bettacraft.tank.too_big", type.getDescription());
            default -> Component.translatable("message.bettacraft.tank.rejected", type.getDescription());
        };
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
        e.setData(ModAttachments.TANK_REF.get(), new TankRef(controller.getBlockPos(), controller.getTank().id()));
        if (e instanceof Mob m) m.setPersistenceRequired();
        if (e instanceof Bucketable b) b.setFromBucket(true);
    }

    public static void release(Entity e) {
        e.removeData(ModAttachments.TANK_REF.get());
    }

    // ------------------------------------------------------------------ queries

    public static boolean hasTankRef(Entity e) {
        return e.hasData(ModAttachments.TANK_REF.get()) && e.getData(ModAttachments.TANK_REF.get()).isSet();
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
                && id.equals(e.getData(ModAttachments.TANK_REF.get()).tankId()));
    }

    public static boolean isAdult(Entity e) {
        if (e instanceof BettaEntity b) return b.isAdult();
        return !(e instanceof LivingEntity le) || !le.isBaby();
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
        int per = ServerConfig.getOr(ServerConfig.TANK_OVERCROWDED_PER_WATER_BLOCK, 2);
        return adults >= 2 && adults >= per * waterBlocks - 1e-6;
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
        int adults = 0;
        for (Entity e : level.getEntities((Entity) null, box, e -> e.isAlive() && (hasTankRef(e) || adoptable(e)))) {
            if (!tank.contains(e.blockPosition())) continue;
            if (!ours.equals(e.getData(ModAttachments.TANK_REF.get()))) adopt(e, controller);
            found.add(e.getUUID());
            if (isAdult(e)) adults++;
        }
        for (Entity e : level.getEntities((Entity) null, box.inflate(6), e -> hasTankRef(e) && !inside(tank, e))) {
            if (ours.equals(e.getData(ModAttachments.TANK_REF.get()))) release(e); // left the tank (e.g. teleported)
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
        controller.setCrowded(isOvercrowded(adults, tank.waterBlocks()));
        if (changed) controller.markTankChanged();
    }

    /** Small water animals that got into the tank without a bucket (spawn egg, /summon) become inhabitants. */
    private static boolean adoptable(Entity e) {
        return e instanceof Mob && e instanceof Bucketable && e.isInWater() && accepts(e.getType(), e) == Acceptance.OK;
    }

    /**
     * Controller of the tank an entity lives in. Repairs the reference after merges/splits and releases the entity if
     * its tank no longer exists. Null if the entity is not (or no longer) in a tank.
     */
    @Nullable
    public static TankBlockEntity resolveController(ServerLevel level, Entity e) {
        if (!hasTankRef(e)) return null;
        TankRef ref = e.getData(ModAttachments.TANK_REF.get());
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
        ServerLevel level = player.serverLevel();
        TankData tank = controller.getTank();
        if (!canModify(player, tank)) return Component.translatable("message.bettacraft.tank.locked");
        Entity e = level.getEntity(entityId);
        if (e == null || !e.isAlive() || !hasTankRef(e) || !tank.id().equals(e.getData(ModAttachments.TANK_REF.get()).tankId())) {
            return Component.translatable("message.bettacraft.tank.not_inside");
        }
        if (!(e instanceof Bucketable bucketable)) return Component.translatable("message.bettacraft.tank.cannot_take", e.getName());
        int slot = findContainer(player, bag);
        if (slot < 0) {
            return Component.translatable(bag ? "message.bettacraft.tank.need_bag" : "message.bettacraft.tank.need_bucket");
        }
        ItemStack container = player.getInventory().getItem(slot);
        long cost = containerWater(container);
        if (tank.waterMb() < cost) return Component.translatable("message.bettacraft.tank.not_enough_water");
        release(e);
        ItemStack filled;
        if (FishContainers.kind(container) != null) {
            filled = FishContainers.fill(container, e, player); // Better Fish Tanks bag / Fintastic net
        } else if (bag) {
            filled = FishBagItem.fillWith(e);
        } else {
            filled = bucketable.getBucketItemStack();
            bucketable.saveToBucketTag(filled);
        }
        if (!player.getAbilities().instabuild) player.getInventory().getItem(slot).shrink(1);
        if (!player.getInventory().add(filled)) player.drop(filled, false);
        TankCare.log(level, controller, "log.bettacraft.taken_out", TankCare.nameArg(e));
        tank.inhabitants().remove(e.getUUID());
        e.discard();
        if (cost > 0) TankInteractions.changeWater(level, controller, -cost);
        level.playSound(null, player.blockPosition(), bucketable.getPickupSound(), SoundSource.NEUTRAL, 1.0F, 1.0F);
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

    /** Reason why the tank block at {@code pos} must not be broken: living inhabitants or eggs inside (PLAN 4.1). */
    public static Optional<Component> breakBlockedReason(Level level, BlockPos pos) {
        TankBlockEntity controller = TankMultiblock.getController(level, pos);
        if (controller == null) return Optional.empty();
        TankData tank = controller.getTank();
        BubbleNest nest = tank.nest();
        if (nest != null && nest.hasEggs()) {
            return Optional.of(Component.translatable("message.bettacraft.tank.break_blocked_nest", nest.visibleEggCount()));
        }
        if (level instanceof ServerLevel server) {
            List<Entity> live = liveInhabitants(server, tank);
            if (live.isEmpty()) return Optional.empty();
            Component names = live.getFirst().getName();
            for (int i = 1; i < Math.min(3, live.size()); i++) names = Component.translatable("cause.bettacraft.and", names, live.get(i).getName());
            return Optional.of(Component.translatable("message.bettacraft.tank.break_blocked", live.size(), names));
        }
        // client: use the synced count so the block does not flicker away
        return tank.inhabitantCount() > 0 ? Optional.of(Component.empty()) : Optional.empty();
    }
}
