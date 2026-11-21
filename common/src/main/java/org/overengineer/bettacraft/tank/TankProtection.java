package org.overengineer.bettacraft.tank;

import org.overengineer.bettacraft.util.Perms;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModBlocks;

/**
 * M21.5: break protection ("Bruchschutz", switch in the tank menu, owner or OP only, off for new tanks). While it is on
 * nobody can break the tank - not the owner, not in creative - nor the lid, light and feeders on it, the clip-on feeders
 * on its glass and the cabinet under it; explosions and pistons leave it alone too
 * ({@code tank.protectFromExplosionsAndPistons}, default on). {@code features.equipment.breakProtection} off: no effect.
 */
public final class TankProtection {
    private TankProtection() {}

    /** The protected tank this block belongs to (glass, lid / light / feeder on it, clip-on feeder, cabinet under it), or null. */
    @Nullable
    public static TankBlockEntity protectedBy(Level level, BlockPos pos) {
        if (!Features.enabled(Feature.BREAK_PROTECTION)) return null;
        BlockState s = level.getBlockState(pos);
        if (s.is(ModBlocks.TANK.get())) return check(level, pos);
        if (s.is(ModBlocks.TANK_LID.get()) || s.getBlock() instanceof org.overengineer.bettacraft.equipment.TankLightBlock || s.is(ModBlocks.FEEDER.get())) return check(level, pos.below());
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
        if (!Features.enabled(Feature.BREAK_PROTECTION)) return Component.translatable("message.bettacraft.tank.break_protection_off");
        TankData t = c.getTank();
        boolean owner = TankAccess.canManage(player, t); // M53: also the caretakers
        if (!owner) return Component.translatable("message.bettacraft.tank.only_owner");
        if (t.owner() == null) t.setOwner(player.getUUID());
        t.setBreakProtected(!t.breakProtected());
        TankCare.log((net.minecraft.server.level.ServerLevel) c.getLevel(), c, t.breakProtected() ? "log.bettacraft.protection_on" : "log.bettacraft.protection_off",
                TankCare.nameArg(player));
        c.markTankChanged();
        return null;
    }



}
