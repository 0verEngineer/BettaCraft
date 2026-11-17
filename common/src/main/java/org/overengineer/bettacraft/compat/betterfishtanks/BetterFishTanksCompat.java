package org.overengineer.bettacraft.compat.betterfishtanks;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.registry.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Better Fish Tanks (M7): our filled fish bag and the betta bucket release their animal into a Better Fish Tanks tank.
 * BFT itself only takes fish from its own bag; this uses its public {@code FishTankBlockEntity#releaseEntityInside}
 * and {@code FishClassifier#isCatchable} through reflection, so no BFT class is linked. Registered only if the mod is
 * loaded ({@link #register()}).
 */
public final class BetterFishTanksCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/compat");
    public static final String MOD_ID = "betterfishtanks";
    public static final Identifier TANK_BLOCK = Identifier.fromNamespaceAndPath(MOD_ID, "fish_tank_block");
    private static final String CLASSIFIER = "net.mint_gaming_studios.betterfishtanksmod.datagen.custom.FishClassifier";

    @Nullable
    private static Method isCatchable;
    private static boolean looked;

    private BetterFishTanksCompat() {}

    /** M52: the loader calls {@link #onRightClickBlock} from its block click event (NeoForge: in BettaCraftNeoForge). */
    public static void register() {}

    public static boolean isTankBlock(Level level, BlockPos pos) {
        return TANK_BLOCK.equals(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()));
    }

    /** Our containers that hold an animal: a filled fish bag or a betta bucket. */
    private static boolean isOurFilledContainer(ItemStack stack) {
        // M17: a bag of shrimp or snails (critters) is not an animal BFT can take; the click goes on as usual
        if (stack.has(org.overengineer.bettacraft.registry.ModDataComponents.CRITTERS.get())) return false;
        return (stack.getItem() instanceof FishBagItem && FishBagItem.isFilled(stack)) || stack.getItem() instanceof BettaBucketItem;
    }

    /** A right click on a block: our filled bag or bucket on a BFT tank releases the animal there (null: not ours, go on). */
    @Nullable
    public static InteractionResult onRightClickBlock(Player player, Level level, InteractionHand hand, BlockPos pos, ItemStack stack) {
        if (!org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.COMPAT_BETTER_FISH_TANKS)) return null; // M33 part b
        if (!isOurFilledContainer(stack) || !isTankBlock(level, pos)) return null;
        if (level instanceof ServerLevel server) {
            Component error = release(server, pos, player, hand, stack);
            if (error != null) player.displayClientMessage(error, true);
        }
        return InteractionResult.SUCCESS;
    }

    /** Releases the animal of {@code stack} into the BFT tank at {@code pos}; returns an error or null. */
    @Nullable
    public static Component release(ServerLevel level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
        EntityType<?> type = stack.getItem() instanceof FishBagItem ? FishBagItem.containedType(stack)
                : org.overengineer.bettacraft.tank.TankInhabitants.containedType(stack);
        BlockEntity be = level.getBlockEntity(pos);
        if (type == null || be == null) return Component.translatable("message.bettacraft.compat.bft_failed");
        @SuppressWarnings("unchecked")
        EntityType<Entity> anyType = (EntityType<Entity>) type;
        Consumer<Entity> config = EntityType.createDefaultStackConfig(level, stack, player);
        // M17: an animal without a bucket (squid) is stored with its full data in our bag: keep name, health, age ...
        net.minecraft.nbt.CompoundTag stored = FishBagItem.storedEntity(stack);
        Entity e;
        if (stored != null) {
            e = org.overengineer.bettacraft.compat.FishContainers.create(level, stored);
            if (e != null) e.snapTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0);
        } else {
            e = anyType.create(level, config, pos, EntitySpawnReason.BUCKET, false, false);
        }
        if (e == null) return Component.translatable("message.bettacraft.compat.bft_failed");
        if (e instanceof Bucketable b) {
            b.loadFromBucketTag(stack.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).copyTag());
            b.setFromBucket(true);
        }
        if (!catchable(e)) {
            e.discard();
            return Component.translatable("message.bettacraft.compat.bft_not_catchable", type.getDescription());
        }
        boolean released;
        try {
            released = (boolean) be.getClass().getMethod("releaseEntityInside", Entity.class).invoke(be, e);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            LOGGER.warn("Better Fish Tanks: releaseEntityInside failed", ex);
            released = false;
        }
        if (!released) {
            if (!org.overengineer.bettacraft.platform.Platform.get().isAddedToLevel(e)) e.discard();
            return Component.translatable("message.bettacraft.compat.bft_failed");
        }
        boolean bucket = !(stack.getItem() instanceof FishBagItem);
        if (bucket) fillWater(be, 1000); // the bucket brings its water along
        ItemStack empty = bucket ? new ItemStack(Items.BUCKET) : new ItemStack(ModItems.FISH_BAG.get());
        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, empty));
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_FISH, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
        return null;
    }

    /** BFT's own rule for its bag and tanks; true if the classifier cannot be found. */
    public static boolean catchable(Entity e) {
        if (!looked) {
            looked = true;
            try {
                isCatchable = Class.forName(CLASSIFIER).getMethod("isCatchable", Entity.class);
            } catch (ReflectiveOperationException ex) {
                LOGGER.warn("Better Fish Tanks: FishClassifier.isCatchable not found, every animal is released");
            }
        }
        if (isCatchable == null) return true;
        try {
            return (boolean) isCatchable.invoke(null, e);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return true;
        }
    }

    /**
     * Fills water into a BFT tank like their water bucket does: through the fluid tank of the block entity (this also
     * sets the fluid type; {@code addWater} alone only changes the amount). Returns the amount filled.
     */
    public static int fillWater(BlockEntity be, int mb) {
        try {
            Object tank = be.getClass().getMethod("getFluidTank").invoke(be);
            // the tank may be a private subclass: call the method through the first public class that has it
            Class<?> cls = tank.getClass();
            while (cls != null && !java.lang.reflect.Modifier.isPublic(cls.getModifiers())) cls = cls.getSuperclass();
            if (cls == null) return 0;
            Method fill = cls.getMethod("fill", net.minecraft.world.level.material.Fluid.class, int.class, boolean.class);
            return (int) fill.invoke(tank, net.minecraft.world.level.material.Fluids.WATER, mb, false);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            LOGGER.warn("Better Fish Tanks: filling water failed: {}", ex.toString());
            return 0;
        }
    }

    /** Water in the BFT tank (mB), or -1 if unknown. */
    public static int waterAmount(BlockEntity be) {
        try {
            return (int) be.getClass().getMethod("getFluidAmount").invoke(be);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return -1;
        }
    }
}
