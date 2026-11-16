package org.overengineer.bettacraft.plumbing.interior;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
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
        // M40: a guard used on the glass where a filter pipe docks goes behind that connection in the interior
        if ((kind == Plumb.INTAKE_GUARD || kind == Plumb.OUTLET_GUARD) && level.getBlockState(ctx.getClickedPos()).getBlock() instanceof TankBlock) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            return TankAnchors.installGuard((ServerLevel) level, ctx.getClickedPos(), ctx.getClickedFace(), ctx.getPlayer(), ctx.getItemInHand(), kind);
        }
        return super.useOn(ctx);
    }

    /** Puts a bulkhead into the floor block {@code pos} of a tank (from outside). Public for the GameTests. */
    public static InteractionResult installFromOutside(ServerLevel level, BlockPos pos, Player player, ItemStack stack) {
        if (!Features.enabled(Feature.INTERIOR_PIPES)) {
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
        if (!Features.enabled(Feature.INTERIOR_PIPES)) {
            say(ctx.getPlayer(), level, "message.bettacraft.feature_off");
            return InteractionResult.FAIL;
        }
        if (kind == Plumb.PORT && ctx.getClickedPos().getY() != InteriorRegion.BASE_Y) {
            say(ctx.getPlayer(), level, "message.bettacraft.bulkhead.floor_only");
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide && ctx.getPlayer() != null && !InteriorManager.canBuild(ctx.getPlayer(), ctx.getClickedPos())) return InteractionResult.FAIL;
        // M40: no pipe that leads into the tank and out again
        if (!level.isClientSide && kind == Plumb.PIPE && joinsTwoPorts((ServerLevel) level, ctx.getClickedPos())) {
            say(ctx.getPlayer(), level, "message.bettacraft.plumbing.pass_through");
            return InteractionResult.FAIL;
        }
        return super.place(ctx);
    }

    /** M40: whether an interior pipe at {@code pos} would join two connections to the outside (bulkheads, pipe docks). */
    public static boolean joinsTwoPorts(ServerLevel interior, BlockPos pos) {
        InteriorRegion r = InteriorManager.regionAt(interior.getServer(), pos);
        if (r == null || !r.built()) return false;
        it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap m = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap(r.equipment().plumbing());
        long k = BlockPos.asLong(r.localX(pos.getX()), r.localY(pos.getY()), r.localZ(pos.getZ()));
        m.put(k, Plumb.code(Plumb.PIPE, 0));
        return passThrough(m, k);
    }

    /** Pure: the pipe at {@code key} reaches two or more connections to the outside. */
    public static boolean passThrough(it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap plumbing, long key) {
        return InteriorPlumbing.search(plumbing, key).ports().size() >= 2;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String key = switch (kind) {
            case PORT -> "bulkhead";
            case INTAKE_GUARD, OUTLET_GUARD -> "guard"; // M40
            default -> "interior_plumbing";
        };
        tooltip.add(Component.translatable("tooltip.bettacraft." + key + ".usage").withStyle(ChatFormatting.DARK_GRAY));
    }
}
