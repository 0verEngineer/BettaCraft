package org.overengineer.bettacraft.tank.interior;

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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.decor.DecorItem;

/** Game-bus events of the interior dimension (server side; also harmless on the client's integrated server). */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class InteriorEvents {
    private static final Map<UUID, Integer> SNEAK_TICKS = new ConcurrentHashMap<>();

    private InteriorEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !InteriorManager.isInterior(player.level())) {
            if (!event.getEntity().level().isClientSide) SNEAK_TICKS.remove(event.getEntity().getUUID());
            return;
        }
        // Players outside every valid region (e.g. teleported in by command, region deleted) are sent back.
        if (player.tickCount % 40 == 0 && InteriorManager.regionAt(player.server, player.blockPosition()) == null
                && !player.hasPermissions(2)) {
            InteriorManager.leave(player);
            player.displayClientMessage(Component.translatable("message.bettacraft.interior.invalid"), true);
            return;
        }
        leaveTick(player);
    }

    /**
     * M15: one tick of the leave timer with the player's own input: it counts while the player sneaks AND looks steeply
     * up ({@code interior.leaveLookUpDegrees}), so sneaking to set substrate does not take you out. True if the player left.
     */
    public static boolean leaveTick(ServerPlayer player) {
        return sneakTick(player, player.isShiftKeyDown() && looksUp(player));
    }

    /** Whether the player looks up steeply enough to leave (pitch at most minus {@code interior.leaveLookUpDegrees}). */
    public static boolean looksUp(Player player) {
        int deg = ServerConfig.getOr(ServerConfig.INTERIOR_LEAVE_LOOK_UP, 60);
        return deg <= 0 || player.getXRot() <= -deg;
    }

    /** One tick of the "hold to leave" timer (PLAN 7.1); the client shows the progress (M15). Returns true if the player left. */
    public static boolean sneakTick(ServerPlayer player, boolean sneaking) {
        int needed = ServerConfig.getOr(ServerConfig.INTERIOR_LEAVE_SNEAK_TICKS, 60);
        if (!sneaking) {
            resetLeave(player);
            return false;
        }
        int t = SNEAK_TICKS.merge(player.getUUID(), 1, Integer::sum);
        if (t >= needed) {
            SNEAK_TICKS.remove(player.getUUID());
            progress(player, 0, needed);
            InteriorManager.leave(player);
            return true;
        }
        progress(player, t, needed);
        return false;
    }

    /** M15: placing or breaking something (any use of a block) starts the leave timer over. */
    public static void resetLeave(Player player) {
        if (player instanceof ServerPlayer sp && SNEAK_TICKS.remove(sp.getUUID()) != null) {
            progress(sp, 0, ServerConfig.getOr(ServerConfig.INTERIOR_LEAVE_SNEAK_TICKS, 60));
        }
    }

    /** Ticks the leave timer has counted for this player (0 = not holding). */
    public static int leaveTicks(Player player) {
        return SNEAK_TICKS.getOrDefault(player.getUUID(), 0);
    }

    private static void progress(ServerPlayer player, int ticks, int needed) {
        if (player.connection != null) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                    new org.overengineer.bettacraft.network.InteriorLeavePayload(ticks, needed));
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || !InteriorManager.isInterior(level)) return;
        if (InteriorManager.isShellBlock(event.getState()) || !InteriorManager.canBuild(event.getPlayer(), event.getPos())) {
            event.setCanceled(true);
            return;
        }
        resetLeave(event.getPlayer());
    }

    /** M15: starting to mine a block in the interior resets the leave timer too. */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getLevel().isClientSide && InteriorManager.isInterior(event.getLevel())) resetLeave(event.getEntity());
    }

    /** M15: explosions leave the blocks of a tank alone while a player is in its interior. */
    @SubscribeEvent
    public static void onDetonate(net.neoforged.neoforge.event.level.ExplosionEvent.Detonate event) {
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
            if (event.getEntity() instanceof Player p && !level.isClientSide) {
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
        if (!level.isClientSide) resetLeave(player); // M15: building starts the leave timer over
        if (state.getBlock() instanceof BedBlock || state.getBlock() instanceof RespawnAnchorBlock) {
            event.setUseBlock(TriState.FALSE);
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.bettacraft.interior.no_sleep").withStyle(ChatFormatting.GOLD), true);
            }
            return;
        }
        if (player.isShiftKeyDown() && Substrate.isFloor(state) && !(event.getItemStack().getItem() instanceof DecorItem)) {
            if (level.isClientSide) {
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
            if (level.isClientSide) return;
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
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && InteriorManager.isInterior(player.level())
                && InteriorManager.regionAt(player.server, player.blockPosition()) == null) {
            InteriorManager.leave(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        InteriorSync.clearAll();
        InteriorPreparation.clearAll();
        InteriorObstacles.clearAll();
        InteriorGround.clearAll();
        DecorItem.clearCache();
        SNEAK_TICKS.clear();
        InteriorManager.clearOccupants();
    }
}
