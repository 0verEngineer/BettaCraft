package org.overengineer.bettacraft.plumbing.interior;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;

/**
 * M24: the plumbing pieces of the tank interior (interior pipe, intake strainer, lily pipe, nozzle, spray bar, bulkhead):
 * only placed inside a tank interior. The bulkhead also goes onto a floor block of a tank from outside (right-click the
 * glass): its connection then appears on the interior floor in the middle of that block.
 */
public class PlumbingItem extends BlockItem {
    private final Plumb kind;

    public PlumbingItem(Block block, Plumb kind, Properties properties) {
        super(block, properties);
        this.kind = kind;
    }

    private static void say(Player p, Level level, String key, Object... args) {
        if (!level.isClientSide && p != null) p.displayClientMessage(Component.translatable(key, args).withStyle(ChatFormatting.GOLD), true);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (kind == Plumb.PORT && level.getBlockState(ctx.getClickedPos()).getBlock() instanceof TankBlock) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            return installFromOutside((ServerLevel) level, ctx.getClickedPos(), ctx.getPlayer(), ctx.getItemInHand());
        }
        return super.useOn(ctx);
    }

    /** Puts a bulkhead into the floor block {@code pos} of a tank (from outside). Public for the GameTests. */
    public static InteractionResult installFromOutside(ServerLevel level, BlockPos pos, Player player, ItemStack stack) {
        if (!CommonConfig.interiorPipes()) {
            say(player, level, "message.bettacraft.feature_off");
            return InteractionResult.FAIL;
        }
        TankBlockEntity c = TankMultiblock.getController(level, pos);
        if (c == null) return InteractionResult.FAIL;
        TankData t = c.getTank();
        if (player != null && !TankInhabitants.canModify(player, t)) return InteractionResult.FAIL;
        if (t.contains(pos.below())) {
            say(player, level, "message.bettacraft.bulkhead.floor_only");
            return InteractionResult.FAIL;
        }
        if (InteriorManager.interiorLevel(level.getServer()) == null) return InteractionResult.FAIL;
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        int s = r.scale();
        BlockPos cell = pos.subtract(t.min());
        if (InteriorPlumbing.portInCell(InteriorManager.equipment(level.getServer(), r).plumbing(), cell.getX(), cell.getZ(), s) != Long.MIN_VALUE) {
            say(player, level, "message.bettacraft.bulkhead.already");
            return InteractionResult.FAIL;
        }
        // the middle of the block first, then the free spot nearest to it on the floor of that block
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (int dx = 0; dx < s; dx++)
            for (int dz = 0; dz < s; dz++) {
                BlockPos w = r.toWorld(cell.getX() * s + dx, 0, cell.getZ() * s + dz);
                if (!interior.getBlockState(w).canBeReplaced()) continue;
                double d = (dx - s / 2) * (dx - s / 2) + (dz - s / 2) * (dz - s / 2);
                if (d < bestD) {
                    bestD = d;
                    best = w;
                }
            }
        if (best == null) {
            say(player, level, "message.bettacraft.bulkhead.no_room");
            return InteractionResult.FAIL;
        }
        interior.setBlock(best, org.overengineer.bettacraft.registry.ModBlocks.BULKHEAD.get().defaultBlockState(), Block.UPDATE_ALL);
        if (player != null) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            player.displayClientMessage(Component.translatable("message.bettacraft.bulkhead.installed").withStyle(ChatFormatting.AQUA), true);
        }
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.COPPER_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.2F);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        if (!InteriorManager.isInterior(level)) {
            say(ctx.getPlayer(), level, kind == Plumb.PORT ? "message.bettacraft.bulkhead.where" : "message.bettacraft.equipment.interior_only");
            return InteractionResult.FAIL;
        }
        if (!CommonConfig.interiorPipes()) {
            say(ctx.getPlayer(), level, "message.bettacraft.feature_off");
            return InteractionResult.FAIL;
        }
        if (kind == Plumb.PORT && ctx.getClickedPos().getY() != InteriorRegion.BASE_Y) {
            say(ctx.getPlayer(), level, "message.bettacraft.bulkhead.floor_only");
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide && ctx.getPlayer() != null && !InteriorManager.canBuild(ctx.getPlayer(), ctx.getClickedPos())) return InteractionResult.FAIL;
        return super.place(ctx);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.bettacraft." + (kind == Plumb.PORT ? "bulkhead" : "interior_plumbing") + ".usage").withStyle(ChatFormatting.DARK_GRAY));
    }
}
