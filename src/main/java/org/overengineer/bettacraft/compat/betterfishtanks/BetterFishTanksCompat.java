package org.overengineer.bettacraft.compat.betterfishtanks;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
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
    public static final ResourceLocation TANK_BLOCK = ResourceLocation.fromNamespaceAndPath(MOD_ID, "fish_tank_block");
    private static final String CLASSIFIER = "net.mint_gaming_studios.betterfishtanksmod.datagen.custom.FishClassifier";

    @Nullable
    private static Method isCatchable;
    private static boolean looked;

    private BetterFishTanksCompat() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(BetterFishTanksCompat::onRightClickBlock);
    }

    public static boolean isTankBlock(Level level, BlockPos pos) {
        return TANK_BLOCK.equals(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()));
    }

    /** Our containers that hold an animal: a filled fish bag or a betta bucket. */
    private static boolean isOurFilledContainer(ItemStack stack) {
        return (stack.getItem() instanceof FishBagItem && FishBagItem.isFilled(stack)) || stack.getItem() instanceof BettaBucketItem;
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!isOurFilledContainer(stack) || !isTankBlock(event.getLevel(), event.getPos())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (event.getLevel() instanceof ServerLevel server) {
            Component error = release(server, event.getPos(), event.getEntity(), event.getHand(), stack);
            if (error != null) event.getEntity().displayClientMessage(error, true);
        }
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
        Entity e = anyType.create(level, config, pos, MobSpawnType.BUCKET, false, false);
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
        } catch (ReflectiveOperationException | ClassCastException ex) {
            LOGGER.warn("Better Fish Tanks: releaseEntityInside failed", ex);
            released = false;
        }
        if (!released) {
            if (!e.isAddedToLevel()) e.discard();
            return Component.translatable("message.bettacraft.compat.bft_failed");
        }
        boolean bucket = !(stack.getItem() instanceof FishBagItem);
        if (bucket) addWater(be, 1000); // the bucket brings its water along
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
        } catch (ReflectiveOperationException | ClassCastException ex) {
            return true;
        }
    }

    private static void addWater(BlockEntity be, int mb) {
        try {
            be.getClass().getMethod("addWater", int.class).invoke(be, mb);
        } catch (ReflectiveOperationException ex) {
            LOGGER.debug("Better Fish Tanks: addWater not available", ex);
        }
    }
}
