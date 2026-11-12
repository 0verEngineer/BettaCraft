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
        sneakTick(player, player.isShiftKeyDown());
    }

    /** One tick of the "hold sneak to leave" timer (PLAN 7.1). Returns true if the player left. */
    public static boolean sneakTick(ServerPlayer player, boolean sneaking) {
        if (!sneaking) {
            SNEAK_TICKS.remove(player.getUUID());
            return false;
        }
        int needed = ServerConfig.getOr(ServerConfig.INTERIOR_LEAVE_SNEAK_TICKS, 60);
        int t = SNEAK_TICKS.merge(player.getUUID(), 1, Integer::sum);
        if (t >= needed) {
            SNEAK_TICKS.remove(player.getUUID());
            InteriorManager.leave(player);
            return true;
        }
        if (t % 10 == 0) {
            int secondsLeft = (int) Math.ceil((needed - t) / 20.0);
            player.displayClientMessage(Component.translatable("message.bettacraft.interior.leaving", secondsLeft), true);
        }
        return false;
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || !InteriorManager.isInterior(level)) return;
        if (InteriorManager.isShellBlock(event.getState()) || !InteriorManager.canBuild(event.getPlayer(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof Level level && InteriorManager.isInterior(level) && event.getEntity() instanceof Player p
                && !InteriorManager.canBuild(p, event.getPos())) {
            event.setCanceled(true);
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
        if (event.getServer().getTickCount() % 5 == 0) {
            InteriorSync.flush(event.getServer());
        }
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
        DecorItem.clearCache();
        SNEAK_TICKS.clear();
    }
}
