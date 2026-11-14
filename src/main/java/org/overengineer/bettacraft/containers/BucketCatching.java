package org.overengineer.bettacraft.containers;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.reef.SaltTolerance;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tankmates.TankmateBucketItem;

/**
 * M22.3: catching fish in the world with buckets and bags.
 * <ul>
 *     <li>A bucket or a fish bag that already holds fish of the same species takes one more while it has room
 *     ({@link AnimalContainers#bucket}, {@link AnimalContainers#bag}).</li>
 *     <li>A bucket of sea water works like a bucket of water for every animal that goes into a bucket (vanilla and other
 *     mods' fish too): the item is handed to the animal as a water bucket and comes back unchanged if nothing happened.</li>
 *     <li>The water must suit our animals: a plain water bucket takes no reef animal, a bucket of sea water no freshwater
 *     animal.</li>
 * </ul>
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class BucketCatching {
    private BucketCatching() {}

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        if (!(event.getTarget() instanceof Mob mob) || !(mob instanceof Bucketable) || !mob.isAlive()) return;
        if (TankInhabitants.hasTankRef(mob)) return; // animals of a tank: the tank menu
        // a bucket or bag of the same species with room
        if (mob instanceof TankmateEntity m && CommonConfigs.big() && (held.getItem() instanceof TankmateBucketItem || held.getItem() instanceof FishBagItem && FishBagItem.isFilled(held))) {
            var s = AnimalContainers.tankmateSpecies(held);
            if (s != m.species() || held.has(org.overengineer.bettacraft.registry.ModDataComponents.TANKMATE_CLUTCH.get())) return;
            int cap = held.getItem() instanceof TankmateBucketItem ? AnimalContainers.bucket(s) : AnimalContainers.bag(s);
            List<CompoundTag> fish = AnimalContainers.fish(held);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
            if (player.level().isClientSide) return;
            if (fish.size() >= cap) {
                player.displayClientMessage(Component.translatable("message.bettacraft.container.full", cap).withStyle(ChatFormatting.GOLD), true);
                return;
            }
            ItemStack tmp = m.getBucketItemStack();
            m.saveToBucketTag(tmp);
            List<CompoundTag> more = new java.util.ArrayList<>(fish);
            more.add(tmp.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).copyTag());
            player.setItemInHand(event.getHand(), AnimalContainers.withFish(held, s, more));
            mob.playSound(m.getPickupSound(), 1.0F, 1.0F);
            mob.discard();
            return;
        }
        boolean water = held.is(Items.WATER_BUCKET), sea = held.is(ModItems.SEA_WATER_BUCKET.get());
        if (!water && !sea) return;
        // our animals: the water must suit them
        var range = SaltTolerance.of(mob);
        boolean ours = mob.getType().builtInRegistryHolder().key().location().getNamespace().equals(BettaCraft.MOD_ID);
        if (ours && range != null && !AnimalContainers.suits(range, sea)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.translatable(sea ? "message.bettacraft.container.needs_fresh_water"
                        : "message.bettacraft.container.needs_sea_water").withStyle(ChatFormatting.GOLD), true);
            }
            return;
        }
        if (!sea) return;
        // a bucket of sea water is a bucket of water for the animal (vanilla checks for the water bucket item)
        ItemStack before = held;
        player.setItemInHand(event.getHand(), new ItemStack(Items.WATER_BUCKET));
        InteractionResult r = mob.interact(player, event.getHand());
        if (player.getItemInHand(event.getHand()).is(Items.WATER_BUCKET)) player.setItemInHand(event.getHand(), before);
        if (r.consumesAction()) {
            event.setCanceled(true);
            event.setCancellationResult(r);
        }
    }

    /** The config switch (common). */
    private static final class CommonConfigs {
        static boolean big() {
            return org.overengineer.bettacraft.config.CommonConfig.bigContainers();
        }
    }
}
