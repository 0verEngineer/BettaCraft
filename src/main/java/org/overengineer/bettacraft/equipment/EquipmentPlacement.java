package org.overengineer.bettacraft.equipment;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;

/**
 * Places heaters and internal filters as segment columns into a tank interior (PLAN 7.3). The column size follows
 * the interior scale of the tank; outside an interior the items cannot be placed.
 */
public final class EquipmentPlacement {
    private EquipmentPlacement() {}

    /** Internal filter ({@code size == null}) or heater of this size. */
    public static InteractionResult place(UseOnContext ctx, SegmentBlock.Size size) {
        return place(ctx, size == null ? SegmentBlock.Kind.FILTER : SegmentBlock.Kind.HEATER, size);
    }

    /** @param size rod size of a heater or cooler (ignored for the internal filter). */
    public static InteractionResult place(UseOnContext ctx, SegmentBlock.Kind kind, SegmentBlock.Size size) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (!InteriorManager.isInterior(level)) {
            if (!level.isClientSide && player != null) {
                player.displayClientMessage(Component.translatable("message.bettacraft.equipment.interior_only").withStyle(ChatFormatting.GOLD), true);
            }
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ServerLevel server = (ServerLevel) level;
        BlockPlaceContext place = new BlockPlaceContext(ctx);
        BlockPos base = place.getClickedPos();
        InteriorRegion r = InteriorManager.regionAt(server.getServer(), base);
        if (r == null || (player != null && !InteriorManager.canBuild(player, base))) return InteractionResult.FAIL;
        boolean heater = kind == SegmentBlock.Kind.HEATER || kind == SegmentBlock.Kind.COOLER;
        int foot, height;
        if (heater) {
            foot = 1;
            height = SegmentBlock.heaterLength(size, r.scale());
        } else {
            int[] f = SegmentBlock.shape(kind, r.scale());
            foot = f[0];
            height = f[1];
        }
        Block block = switch (kind) {
            case HEATER -> ModBlocks.HEATER_SEGMENT.get();
            case COOLER -> ModBlocks.COOLER_SEGMENT.get();
            case FILTER -> ModBlocks.FILTER_SEGMENT.get();
            case PUMP -> ModBlocks.PUMP_SEGMENT.get();
            case SPONGE_FILTER -> ModBlocks.SPONGE_FILTER_SEGMENT.get();
            case CORNER_FILTER -> ModBlocks.CORNER_FILTER_SEGMENT.get();
        };
        // M23: the sponge filter and the corner filter are switched off with features.equipment.moreInternalFilters
        if ((kind == SegmentBlock.Kind.SPONGE_FILTER || kind == SegmentBlock.Kind.CORNER_FILTER) && !Features.enabled(Feature.MORE_INTERNAL_FILTERS)) {
            if (player != null) player.displayClientMessage(Component.translatable("message.bettacraft.feature_off").withStyle(ChatFormatting.GOLD), true);
            return InteractionResult.FAIL;
        }
        // the filter outlet points at the player (into the room of the tank)
        net.minecraft.core.Direction facing = player != null ? player.getDirection().getOpposite() : net.minecraft.core.Direction.NORTH;
        List<BlockPos> cells = new ArrayList<>();
        for (int y = 0; y < height; y++)
            for (int z = 0; z < foot; z++)
                for (int x = 0; x < foot; x++) {
                    BlockPos p = base.offset(x, y, z);
                    if (!r.containsInterior(p) || !(server.getBlockState(p).canBeReplaced(place) || server.getBlockState(p).isAir())) {
                        if (player != null) player.displayClientMessage(Component.translatable("message.bettacraft.equipment.no_room", foot, foot, height)
                                .withStyle(ChatFormatting.RED), true);
                        return InteractionResult.FAIL;
                    }
                    cells.add(p);
                }
        for (BlockPos p : cells) {
            int y = p.getY() - base.getY();
            SegmentBlock.Part part = y == 0 ? (p.getX() == base.getX() && p.getZ() == base.getZ() ? SegmentBlock.Part.ANCHOR : SegmentBlock.Part.BASE)
                    : y == height - 1 ? SegmentBlock.Part.TOP : SegmentBlock.Part.MIDDLE;
            BlockState s = block.defaultBlockState().setValue(SegmentBlock.PART, part).setValue(SegmentBlock.FACING, facing);
            if (heater) s = s.setValue(SegmentBlock.SIZE, size);
            server.setBlock(p, s, Block.UPDATE_ALL);
        }
        if (player == null || !player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        server.playSound(null, base, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
        return InteractionResult.CONSUME;
    }
}
