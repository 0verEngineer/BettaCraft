package org.overengineer.bettacraft.containers;

import net.minecraft.world.InteractionResult;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.entity.betta.FishBagItem;

/**
 * M22.1: the rules every container of the mod follows (buckets, bags, bottles, jars - also every container added later).
 * <ol>
 *     <li><b>Everything can be emptied everywhere.</b> Buckets pour into the world (also into the sea, rivers and other
 *     water surfaces, from a boat or the shore), into a cauldron, into every tank (fresh, brackish or salt water) and
 *     into other mods' tanks and pipes (fluid capability). Bottles of water or live food are emptied anywhere with sneak +
 *     right-click (or right-click into the air): only splashes and a splash sound, no water block, an empty glass bottle
 *     remains; in tanks they work as before. An empty fish bag can be shaken out anywhere (sneak + right-click), no water
 *     block. Dry goods (artemia cyst jars) are no liquid: one jar is one culture.</li>
 *     <li><b>Animals need a confirmation.</b> A container holding animals (an animal bucket, a filled fish or critter
 *     bag) never lets them go into the world without asking: the player confirms in a small screen with "Cancel" and
 *     "Empty" ({@code ConfirmEmptyScreen}); only then does the server release them ({@link #confirmed}). Putting them
 *     into a tank (as inhabitants, where the net brings them back) needs no confirmation.</li>
 *     <li>The server decides; the client never predicts a release (no ghost blocks or animals in multiplayer).</li>
 * </ol>
 * New containers: implement the release through {@link #release} and the pouring through {@link #pour}, and add a
 * GameTest per container and target (see {@code ContainerGameTests}).
 */
public final class ContainerRules {
    /** Players whose confirmed release runs right now (server thread). */
    private static final Set<UUID> CONFIRMED = ConcurrentHashMap.newKeySet();
    /** Client: opens the confirmation screen for the item in this hand (set by the client entry point). */
    public static volatile BiConsumer<InteractionHand, ItemStack> confirmScreen = (h, s) -> { };

    private ContainerRules() {}

    /** True if the stack holds animals (an animal bucket, a filled bag of fish, critters or a clutch). */
    public static boolean holdsAnimals(ItemStack stack) {
        return stack.getItem() instanceof MobBucketItem || stack.getItem() instanceof FishBagItem && FishBagItem.isFilled(stack)
                || stack.getItem() instanceof CritterBucketItem && CritterBucketItem.animals(stack) != null; // M22.3
    }

    /** The player confirmed emptying the container in their hand; the release runs now on the server. */
    public static boolean confirmed(Player player) {
        return CONFIRMED.contains(player.getUUID());
    }

    /**
     * Before animals leave a container into the world: on the client the confirmation screen opens (and nothing
     * happens), on the server only a confirmed use goes on. Returns true if the release may happen now.
     */
    public static boolean release(Level level, Player player, InteractionHand hand, ItemStack stack) {
        if (!holdsAnimals(stack)) return true;
        if (level.isClientSide()) {
            confirmScreen.accept(hand, stack.copy());
            return false;
        }
        return confirmed(player);
    }

    /**
     * The player pressed "Empty" (packet from the client): uses the item in that hand again on the server with the
     * confirmation (the same ray as a right-click: where the player looks now). Nothing happens if the hand holds
     * something else by now.
     */
    public static void confirm(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!holdsAnimals(stack) || player.isSpectator()) return;
        CONFIRMED.add(player.getUUID());
        try {
            player.gameMode.useItem(player, player.level(), stack, hand);
            player.swing(hand, true);
        } finally {
            CONFIRMED.remove(player.getUUID());
        }
        player.inventoryMenu.sendAllDataToRemote();
    }

    /**
     * Pours a little water (or live food) out at {@code at}: splashes and a splash sound, nothing else (no block).
     * Server side; the client sees the particles and hears the sound from the server.
     */
    public static void pour(Level level, Player player, Vec3 at) {
        if (!(level instanceof ServerLevel server)) return;
        server.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 24, 0.25, 0.1, 0.25, 0.4);
        server.sendParticles(ParticleTypes.FALLING_WATER, at.x, at.y + 0.3, at.z, 8, 0.15, 0.1, 0.15, 0);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.5F, 1.2F + level.getRandom().nextFloat() * 0.2F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.FLUID_PLACE, at);
    }

    /** Where poured liquid lands: the clicked spot, else a little in front of the player at their feet. */
    public static Vec3 pourSpot(Player player, net.minecraft.world.phys.HitResult hit) {
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) return hit.getLocation();
        Vec3 look = player.getViewVector(1.0F);
        return player.position().add(look.x * 1.2, 0.4, look.z * 1.2);
    }

    /** The result of a use that waits for the confirmation: nothing happens, no swing. */
    public static InteractionResult waiting(ItemStack stack) {
        return InteractionResult.FAIL;
    }
}
