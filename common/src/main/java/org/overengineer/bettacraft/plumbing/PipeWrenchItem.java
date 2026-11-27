package org.overengineer.bettacraft.plumbing;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;

/**
 * M68: the pipe wrench ({@code features.pipes.pipeWrench}). Right-click a side of a pipe: that side is closed (the pipe no longer
 * joins what is there; a cap shows it) or opened again. The side is the one the click points at from the middle of the block
 * (an arm, or the face of the core where a closed side is). So two lines run side by side without joining, and joints are moved.
 * On a bundle it works on the pipe clicked; sneaking with the wrench on a bundle moves that pipe to the next spot of the
 * bundle ({@link PipeBundleBlock#reslot}), to line it up with the next bundle. Not used up.
 */
public class PipeWrenchItem extends Item {
    public PipeWrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState s = level.getBlockState(pos);
        if (!Pipes.isPipe(s)) return InteractionResult.PASS;
        Player player = ctx.getPlayer();
        if (!Features.enabled(Feature.PIPE_WRENCH)) {
            if (!level.isClientSide && player != null) player.displayClientMessage(Component.translatable("message.bettacraft.feature_off").withStyle(ChatFormatting.GOLD), true);
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Vec3 hit = ctx.getClickLocation();
        if (s.getBlock() instanceof PipeBundleBlock) {
            int slot = PipeBundleBlock.slotHit(level, pos, hit);
            if (slot < 0) return InteractionResult.FAIL;
            if (ctx.isSecondaryUseActive()) {
                int to = PipeBundleBlock.reslot(level, pos, slot);
                if (to >= 0 && player != null) player.displayClientMessage(Component.translatable("message.bettacraft.pipe_wrench.moved", to + 1), true);
                click(level, pos, 1.3F);
                return InteractionResult.CONSUME;
            }
            Direction d = side(pos, hit);
            boolean closed = toggle(level, pos, slot, d);
            tell(player, closed, d);
            return InteractionResult.CONSUME;
        }
        Direction d = side(pos, hit);
        boolean closed = toggle(level, pos, -1, d);
        tell(player, closed, d);
        return InteractionResult.CONSUME;
    }

    private static void tell(Player player, boolean closed, Direction d) {
        if (player == null) return;
        player.displayClientMessage(Component.translatable(closed ? "message.bettacraft.pipe_wrench.closed" : "message.bettacraft.pipe_wrench.opened",
                Component.translatable("direction.bettacraft." + d.getSerializedName())).withStyle(closed ? ChatFormatting.GOLD : ChatFormatting.AQUA), true);
    }

    private static void click(Level level, BlockPos pos, float pitch) {
        level.playSound(null, pos, SoundEvents.COPPER_HIT, SoundSource.BLOCKS, 0.9F, pitch);
    }

    /** The side a click at {@code hit} (world coordinates) on the pipe at {@code pos} points at: from the middle of the block. */
    public static Direction side(BlockPos pos, Vec3 hit) {
        Vec3 v = hit.subtract(Vec3.atCenterOf(pos));
        return Direction.getNearest(v.x, v.y, v.z);
    }

    /**
     * Closes side {@code d} of the pipe at {@code pos} (slot {@code slot} of a bundle, -1 = a single pipe) or opens it again; the
     * pipe and its neighbour look at each other again. Returns whether the side is closed now. Public for the GameTests and the
     * showcase (two lines side by side).
     */
    public static boolean toggle(Level level, BlockPos pos, int slot, Direction d) {
        boolean closed = !Pipes.closed(level, pos, slot, d);
        setClosed(level, pos, slot, d, closed);
        click(level, pos, closed ? 0.8F : 1.1F);
        return closed;
    }

    /** Closes or opens side {@code d} of the pipe at {@code pos} (slot of a bundle, -1 = single pipe). */
    public static void setClosed(Level level, BlockPos pos, int slot, Direction d, boolean closed) {
        if (!(level.getBlockEntity(pos) instanceof PipeBlockEntity be)) return;
        BlockState s = level.getBlockState(pos);
        int bit = PipeSlots.bit(d);
        if (s.getBlock() instanceof PipeBlock) {
            be.setClosed(closed ? be.closed() | bit : be.closed() & ~bit);
            BlockState now = s.setValue(PipeBlock.SIDES.get(d), Pipes.canJoin(level, pos, -1, d, s.getValue(PipeBlock.SIDES.get(d))));
            if (now != s) level.setBlock(pos, now, Block.UPDATE_ALL);
            else level.sendBlockUpdated(pos, s, s, Block.UPDATE_CLIENTS);
            // the neighbour looks at us again (it may be joined to us or want to be)
            BlockPos n = pos.relative(d);
            BlockState ns = level.getBlockState(n);
            BlockState nn = ns.updateShape(d.getOpposite(), level.getBlockState(pos), level, n, pos);
            if (nn != ns) level.setBlock(n, nn, Block.UPDATE_ALL);
            PipeNetwork.invalidate(level, pos);
            PipeNetwork.invalidate(level, n);
            PipeUpdates.schedule(level, pos, false);
        } else if (s.getBlock() instanceof PipeBundleBlock) {
            int v = be.slot(slot);
            if (!PipeSlots.present(v)) return;
            be.setSlot(slot, PipeSlots.withClosed(v, closed ? PipeSlots.closed(v) | bit : PipeSlots.closed(v) & ~bit));
            be.setSlots(PipeBundleBlock.rejoin(level, pos, be.slots(), Direction.values()));
            PipeBundleBlock.after(level, pos);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.bettacraft.pipe_wrench.usage").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.bettacraft.pipe_wrench.bundle").withStyle(ChatFormatting.DARK_GRAY));
    }
}
