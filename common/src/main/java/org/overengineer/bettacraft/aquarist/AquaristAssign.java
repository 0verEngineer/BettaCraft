package org.overengineer.bettacraft.aquarist;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.BedBlock;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.platform.event.PlayerInteractEvent;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/**
 * M66: assigning the chest of his wage or his bed. The menu's button starts it ({@link #start}); the next right-click of that
 * player on a chest (or any container block) or a bed assigns it, within {@code hiredAquarist.homeRadius} blocks of his
 * workplace. A click on anything else says why it does not fit and keeps waiting; it times out after a minute.
 */
public final class AquaristAssign {
    /** What a player is about to assign. */
    public enum Kind { CHEST, BED, BASIN }

    public record Pending(int entity, Kind kind, long until) {}

    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    /** One minute of game time. */
    private static final long WAIT = 1200L;

    private AquaristAssign() {}

    public static void start(ServerPlayer p, AquaristEntity npc, Kind kind) {
        PENDING.put(p.getUUID(), new Pending(npc.getId(), kind, p.level().getGameTime() + WAIT));
        p.sendOverlayMessage(Component.translatable("message.bettacraft.aquarist.assign_" + kind.name().toLowerCase(java.util.Locale.ROOT),
                ServerConfig.getOr(ServerConfig.AQUARIST_HOME_RADIUS, 32)).withStyle(ChatFormatting.YELLOW));
    }

    /** GameTests: whether the player waits to assign something. */
    public static Pending pending(UUID player) {
        return PENDING.get(player);
    }

    public static void forget() {
        PENDING.clear();
    }

    /** The right-click handler ({@link AquaristArrivalHandlers}). */
    static void onRightClick(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || PENDING.isEmpty() || e.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        Pending w = PENDING.get(p.getUUID());
        if (w == null) return;
        ServerLevel level = (ServerLevel) p.level();
        if (level.getGameTime() > w.until() || !(level.getEntity(w.entity()) instanceof AquaristEntity npc) || !npc.isAlive()) {
            PENDING.remove(p.getUUID());
            return;
        }
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        Component err = assign(level, p, npc, e.getPos(), w.kind());
        if (err == null) PENDING.remove(p.getUUID());
        else p.sendOverlayMessage(err.copy().withStyle(ChatFormatting.RED));
    }

    /** Assigns the block at {@code pos}; returns the reason when it does not fit, null when done (the menu opens again). */
    public static Component assign(ServerLevel level, ServerPlayer p, AquaristEntity npc, BlockPos pos, Kind kind) {
        if (!p.getUUID().equals(npc.employer()) && !p.hasPermissions(2)) return Component.translatable("message.bettacraft.aquarist.not_yours");
        BlockPos from = npc.workplace() != null ? npc.workplace() : npc.blockPosition();
        int r = ServerConfig.getOr(ServerConfig.AQUARIST_HOME_RADIUS, 32);
        if (pos.distSqr(from) > (double) r * r) return Component.translatable("message.bettacraft.aquarist.assign_far", r);
        if (kind == Kind.BASIN) {
            if (!level.getBlockState(pos).is(org.overengineer.bettacraft.registry.ModBlocks.BREEDING_BASIN.get())) return Component.translatable("message.bettacraft.aquarist.assign_no_basin");
            if (!npc.assignBasin(level, pos)) return Component.translatable("message.bettacraft.aquarist.assign_basin_taken");
            p.sendOverlayMessage(Component.translatable("message.bettacraft.aquarist.assigned_basin").withStyle(ChatFormatting.GREEN));
        } else if (kind == Kind.BED) {
            if (!(level.getBlockState(pos).getBlock() instanceof BedBlock)) return Component.translatable("message.bettacraft.aquarist.assign_no_bed");
            if (!npc.assignBed(level, pos)) return Component.translatable("message.bettacraft.aquarist.assign_bed_taken");
            p.sendOverlayMessage(Component.translatable("message.bettacraft.aquarist.assigned_bed").withStyle(ChatFormatting.GREEN));
        } else {
            if (level.getBlockEntity(pos) instanceof TankBlockEntity
                    || org.overengineer.bettacraft.platform.Platform.get().itemHandler(level, pos, null) == null) {
                return Component.translatable("message.bettacraft.aquarist.assign_no_chest");
            }
            npc.setChest(pos);
            p.sendOverlayMessage(Component.translatable("message.bettacraft.aquarist.assigned_chest").withStyle(ChatFormatting.GREEN));
        }
        AquaristMenu.open(p, npc);
        return null;
    }
}
