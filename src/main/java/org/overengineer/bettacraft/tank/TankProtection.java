package org.overengineer.bettacraft.tank;

import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModBlocks;

/**
 * M21.5: break protection ("Bruchschutz", switch in the tank menu, owner or OP only, off for new tanks). While it is on
 * nobody can break the tank - not the owner, not in creative - nor the lid, light and feeders on it, the clip-on feeders
 * on its glass and the cabinet under it; explosions and pistons leave it alone too
 * ({@code tank.protectFromExplosionsAndPistons}, default on). {@code features.breakProtection} off: no effect.
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class TankProtection {
    private TankProtection() {}

    /** The protected tank this block belongs to (glass, lid / light / feeder on it, clip-on feeder, cabinet under it), or null. */
    @Nullable
    public static TankBlockEntity protectedBy(Level level, BlockPos pos) {
        if (!CommonConfig.breakProtection()) return null;
        BlockState s = level.getBlockState(pos);
        if (s.is(ModBlocks.TANK.get())) return check(level, pos);
        if (s.is(ModBlocks.TANK_LID.get()) || s.is(ModBlocks.TANK_LIGHT.get()) || s.is(ModBlocks.FEEDER.get())) return check(level, pos.below());
        if (s.is(ModBlocks.CABINET.get())) return check(level, pos.above());
        if (s.is(ModBlocks.CLIP_FEEDER.get())) {
            Direction d = s.getValue(org.overengineer.bettacraft.equipment.ClipFeederBlock.FACING);
            return check(level, pos.relative(d.getOpposite()));
        }
        return null;
    }

    @Nullable
    private static TankBlockEntity check(Level level, BlockPos tankPos) {
        if (!level.isLoaded(tankPos) || !level.getBlockState(tankPos).is(ModBlocks.TANK.get())) return null;
        TankBlockEntity c = TankMultiblock.getController(level, tankPos);
        return c != null && c.getTank() != null && c.getTank().breakProtected() ? c : null;
    }

    public static Component message() {
        return Component.translatable("message.bettacraft.tank.break_protected").withStyle(ChatFormatting.RED);
    }

    /** Owner or OP; null = done, else the reason it was refused. */
    @Nullable
    public static Component toggle(Player player, TankBlockEntity c) {
        if (!CommonConfig.breakProtection()) return Component.translatable("message.bettacraft.tank.break_protection_off");
        TankData t = c.getTank();
        boolean owner = player.getUUID().equals(t.owner()) || player.hasPermissions(2) || t.owner() == null;
        if (!owner) return Component.translatable("message.bettacraft.tank.only_owner");
        if (t.owner() == null) t.setOwner(player.getUUID());
        t.setBreakProtected(!t.breakProtected());
        TankCare.log((net.minecraft.server.level.ServerLevel) c.getLevel(), c, t.breakProtected() ? "log.bettacraft.protection_on" : "log.bettacraft.protection_off",
                TankCare.nameArg(player));
        c.markTankChanged();
        return null;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getLevel() instanceof Level level) || level.isClientSide) return;
        if (protectedBy(level, e.getPos()) == null) return;
        e.setCanceled(true);
        if (e.getPlayer() instanceof ServerPlayer sp) sp.displayClientMessage(message(), true);
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate e) {
        if (!ServerConfig.getOr(ServerConfig.PROTECT_FROM_EXPLOSIONS_AND_PISTONS, true)) return;
        e.getAffectedBlocks().removeIf(p -> protectedBy(e.getLevel(), p) != null);
    }

    @SubscribeEvent
    public static void onPiston(PistonEvent.Pre e) {
        if (!(e.getLevel() instanceof Level level) || level.isClientSide) return;
        if (!ServerConfig.getOr(ServerConfig.PROTECT_FROM_EXPLOSIONS_AND_PISTONS, true)) return;
        var helper = e.getStructureHelper();
        if (helper == null || !helper.resolve()) return;
        for (BlockPos p : helper.getToPush()) if (protectedBy(level, p) != null) { e.setCanceled(true); return; }
        for (BlockPos p : helper.getToDestroy()) if (protectedBy(level, p) != null) { e.setCanceled(true); return; }
    }
}
