package org.overengineer.bettacraft.tank.interior;

import org.overengineer.bettacraft.util.Perms;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.event.SubscribeEvent;
import org.overengineer.bettacraft.platform.event.TriState;
import org.overengineer.bettacraft.platform.event.PlayerEvent;
import org.overengineer.bettacraft.platform.event.PlayerInteractEvent;
import org.overengineer.bettacraft.platform.event.BlockEvent;
import org.overengineer.bettacraft.platform.event.ServerStoppedEvent;
import org.overengineer.bettacraft.platform.event.PlayerTickEvent;
import org.overengineer.bettacraft.platform.event.ServerTickEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.decor.DecorItem;
import static org.overengineer.bettacraft.tank.interior.InteriorEvents.*;

/** M52: the game event handlers of {@link InteriorEvents} (M52: on the common game events, fired by each loader). */
public final class InteriorEventsHandlers {
    private InteriorEventsHandlers() {}

    /** M17: a player who logs out while holding the leave keys must not resume the timer later; request times go too. */
    @SubscribeEvent
    public static void onLoggedOut(org.overengineer.bettacraft.platform.event.PlayerEvent.PlayerLoggedOutEvent event) {
        SNEAK_TICKS.remove(event.getEntity().getUUID());
        if (!event.getEntity().level().isClientSide()) InteriorSync.forgetPlayer(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !InteriorManager.isInterior(player.level())) {
            if (!event.getEntity().level().isClientSide()) SNEAK_TICKS.remove(event.getEntity().getUUID());
            return;
        }
        // Players outside every valid region (e.g. teleported in by command, region deleted) are sent back.
        if (player.tickCount % 40 == 0 && !InteriorManager.inOwnRegion(player) && !Perms.has(player, 2)) {
            InteriorManager.leave(player);
            player.displayClientMessage(Component.translatable("message.bettacraft.interior.invalid"), true);
            return;
        }
        leaveTick(player);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level)) return;
        // M17: machines that break blocks through a fake player and then remove them themselves (they fire this event,
        // but never call onDestroyedByPlayer) must not break a tank while a player is in its interior either
        if (!InteriorManager.isInterior(level) && org.overengineer.bettacraft.platform.Platform.get().isFakePlayer(event.getPlayer())
                && event.getState().getBlock() instanceof org.overengineer.bettacraft.tank.TankBlock
                && level instanceof net.minecraft.server.level.ServerLevel sl) {
            org.overengineer.bettacraft.tank.TankBlockEntity c = org.overengineer.bettacraft.tank.TankMultiblock.getController(sl, event.getPos());
            if (c != null && !InteriorManager.playersInside(sl.getServer(), c.getTank()).isEmpty()) event.setCanceled(true);
            return;
        }
        if (!InteriorManager.isInterior(level)) return;
        if (InteriorManager.isShellBlock(event.getState()) || !InteriorManager.canBuild(event.getPlayer(), event.getPos())) {
            event.setCanceled(true);
            return;
        }
        resetLeave(event.getPlayer());
    }

    /** M15: starting to mine a block in the interior resets the leave timer too. */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getLevel().isClientSide() && InteriorManager.isInterior(event.getLevel())) resetLeave(event.getEntity());
    }

    /** M15: explosions leave the blocks of a tank alone while a player is in its interior. */
    @SubscribeEvent
    public static void onDetonate(org.overengineer.bettacraft.platform.event.ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) return;
        java.util.Map<org.overengineer.bettacraft.tank.TankBlockEntity, Boolean> occupied = new java.util.HashMap<>();
        event.getAffectedBlocks().removeIf(pos -> {
            if (!(level.getBlockState(pos).getBlock() instanceof org.overengineer.bettacraft.tank.TankBlock)) return false;
            org.overengineer.bettacraft.tank.TankBlockEntity c = org.overengineer.bettacraft.tank.TankMultiblock.getController(level, pos);
            return c != null && occupied.computeIfAbsent(c, k -> !InteriorManager.playersInside(level.getServer(), k.getTank()).isEmpty());
        });
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        // M16: no tank inside a tank, whoever places it (players, fake players of machines, other entities)
        if (event.getLevel() instanceof Level level && InteriorManager.isInterior(level)
                && event.getPlacedBlock().is(org.overengineer.bettacraft.registry.ModTags.NOT_IN_TANK_INTERIOR)) {
            event.setCanceled(true);
            if (event.getEntity() instanceof Player p && !level.isClientSide()) {
                p.displayClientMessage(Component.translatable("message.bettacraft.interior.no_tank").withStyle(ChatFormatting.GOLD), true);
            }
            return;
        }
        if (event.getLevel() instanceof Level level && InteriorManager.isInterior(level) && event.getEntity() instanceof Player p) {
            if (!InteriorManager.canBuild(p, event.getPos())) event.setCanceled(true);
            else resetLeave(p);
        }
    }

    /**
     * Interior interactions: substrate (shift + right-click on the floor), lily pads float wherever they are placed,
     * beds and respawn anchors would explode in this dimension and are blocked instead.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (!InteriorManager.isInterior(level)) return;
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Player player = event.getEntity();
        if (!level.isClientSide()) resetLeave(player); // M15: building starts the leave timer over
        if (state.getBlock() instanceof BedBlock || state.getBlock() instanceof RespawnAnchorBlock) {
            event.setUseBlock(TriState.FALSE);
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.bettacraft.interior.no_sleep").withStyle(ChatFormatting.GOLD), true);
            }
            return;
        }
        if (player.isShiftKeyDown() && Substrate.isFloor(state) && !(event.getItemStack().getItem() instanceof DecorItem)) {
            if (level.isClientSide()) {
                // the server decides; keep the client from placing the block on top
                if (event.getItemStack().isEmpty() || event.getItemStack().getItem() instanceof net.minecraft.world.item.BlockItem) {
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                }
                return;
            }
            if (player instanceof ServerPlayer sp && Substrate.use(sp, event.getHand(), pos)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.is(Items.LILY_PAD)) {
            BlockPos target = state.canBeReplaced() ? pos : pos.relative(event.getFace() == null ? net.minecraft.core.Direction.UP : event.getFace());
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (level.isClientSide()) return;
            InteriorRegion r = InteriorManager.regionAt(level.getServer(), target);
            if (r != null && r.containsInterior(target) && level.getBlockState(target).canBeReplaced() && InteriorManager.canBuild(player, target)) {
                level.setBlockAndUpdate(target, Blocks.LILY_PAD.defaultBlockState());
                level.playSound(null, target, SoundEvents.LILY_PAD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        InteriorPreparation.tick(event.getServer());
        InteriorBench.tick(event.getServer());
        if (event.getServer().getTickCount() % 5 == 0) {
            InteriorSync.flush(event.getServer());
        }
        if (event.getServer().getTickCount() % 10 == 0) InteriorManager.updateOccupants(event.getServer()); // M15
        InteriorCleanup.tick(event.getServer()); // M53c
    }

    /** M53c: before the dimensions are loaded: the region files of removed interiors go. */
    @SubscribeEvent
    public static void onServerStarting(org.overengineer.bettacraft.platform.event.ServerAboutToStartEvent event) {
        InteriorCleanup.clearAll();
        InteriorCleanup.onServerStarting(event.getServer());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && InteriorManager.isInterior(player.level())
                && !InteriorManager.inOwnRegion(player)) {
            InteriorManager.leave(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        InteriorSync.clearAll();
        InteriorCleanup.clearAll();
        InteriorPreparation.clearAll();
        InteriorObstacles.clearAll();
        InteriorGround.clearAll();
        DecorItem.clearCache();
        SNEAK_TICKS.clear();
        InteriorManager.clearOccupants();
        // M33 part d (hardening): caches by region id or position must not reach the next world of the same game
        org.overengineer.bettacraft.plumbing.interior.InteriorPlumbing.forget();
        org.overengineer.bettacraft.tankmates.TankmateSpots.forgetAll();
        org.overengineer.bettacraft.tankmates.Seahorses.forget();
    }
}
