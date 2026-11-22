package org.overengineer.bettacraft.equipment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.network.FilterLinkPayload;
import org.overengineer.bettacraft.platform.network.PacketDistributor;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M62: which tank an external filter feeds. A filter right next to a tank connects by itself (as before). With {@code
 * features.equipment.filterBelowTank} it may also stand one or two blocks lower (beside the cabinet or without one): there it
 * finds nothing by itself (several tanks side by side would make a guess wrong); a sneak-right-click with an empty hand lists
 * the tanks in reach ({@link #options}: a tank block beside the filter or one or two blocks above that, the column above the
 * filter outside the tank) and the player chooses one ({@link FilterLinkPayload}).
 */
public final class FilterLinks {
    /** How far the filter may stand below the tank block it feeds. */
    public static final int MAX_BELOW = 2;

    private FilterLinks() {}

    public static boolean belowAllowed() {
        return Features.enabled(Feature.FILTER_BELOW_TANK);
    }

    /** The tanks the filter at {@code pos} can feed, nearest height first, one per tank and side. */
    public static List<FilterLinkPayload.Option> options(Level level, BlockPos pos) {
        Map<String, FilterLinkPayload.Option> out = new LinkedHashMap<>();
        int max = belowAllowed() ? MAX_BELOW : 0;
        for (int dy = 0; dy <= max; dy++) {
            BlockPos column = pos.above(dy);
            if (dy > 0 && level.getBlockState(column).getBlock() instanceof TankBlock) break; // a tank right above: not beside it
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos wall = column.relative(d);
                if (!level.isLoaded(wall) || !(level.getBlockState(wall).getBlock() instanceof TankBlock)) continue;
                TankBlockEntity c = TankMultiblock.getController(level, wall);
                if (c == null || c.getTank() == null) continue;
                TankData t = c.getTank();
                String key = c.getBlockPos().asLong() + "/" + d;
                if (out.containsKey(key)) continue;
                out.put(key, new FilterLinkPayload.Option(wall.immutable(), t.min(), t.name(), dy, t.sizeX(), t.sizeY(), t.sizeZ()));
            }
        }
        return new ArrayList<>(out.values());
    }

    /** Whether the filter at {@code pos} feeds no tank now (no chosen one that still stands, none in front). */
    public static boolean unlinked(Level level, ExternalFilterBlockEntity f) {
        return FilterHost.tankOf(level, f.tankBlockPos()) == null;
    }

    /** The sneak-click: sends the choice (or says why there is none). Server. */
    public static void open(ServerPlayer player, BlockPos pos) {
        if (!(player.level().getBlockEntity(pos) instanceof ExternalFilterBlockEntity f)) return;
        List<FilterLinkPayload.Option> options = options(player.level(), pos);
        if (options.isEmpty()) {
            player.sendOverlayMessage(Component.translatable(belowAllowed() ? "message.bettacraft.filter_link.none" : "message.bettacraft.filter_link.none_beside")
                    .withStyle(ChatFormatting.GOLD));
            return;
        }
        int chosen = -1;
        BlockPos now = f.tankBlockPos();
        TankBlockEntity cur = FilterHost.tankOf(player.level(), now);
        for (int i = 0; i < options.size() && cur != null; i++) {
            if (options.get(i).wall().equals(now)) chosen = i;
        }
        PacketDistributor.sendToPlayer(player, new FilterLinkPayload(pos.immutable(), options, chosen));
    }

    /** The player chose a tank in the list (checked again on the server). */
    public static void choose(ServerPlayer player, FilterLinkPayload p) {
        Level level = player.level();
        BlockPos pos = p.filter();
        if (p.chosen() < 0 || p.chosen() >= p.options().size()) return;
        if (!level.isLoaded(pos) || player.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) > 64 || !level.mayInteract(player, pos)) return;
        if (!(level.getBlockEntity(pos) instanceof ExternalFilterBlockEntity f)) return;
        BlockPos wall = p.options().get(p.chosen()).wall();
        FilterLinkPayload.Option match = null;
        for (FilterLinkPayload.Option o : options(level, pos)) if (o.wall().equals(wall)) match = o;
        if (match == null) return;
        if (link(level, f, wall) == null) return;
        String name = match.name().isEmpty() ? Component.translatable("gui.bettacraft.filter_link.unnamed").getString() : match.name();
        player.sendOverlayMessage(Component.translatable("message.bettacraft.filter_link.linked", name).withStyle(ChatFormatting.AQUA));
    }

    /**
     * Connects the filter to the tank of {@code wall} (a block of {@link #options}): turns it towards the tank and lets the old
     * and the new tank find their filters again. Returns the new tank's controller, null if {@code wall} is no tank.
     */
    @Nullable
    public static TankBlockEntity link(Level level, ExternalFilterBlockEntity f, BlockPos wall) {
        TankBlockEntity c = TankMultiblock.getController(level, wall);
        if (c == null) return null;
        TankBlockEntity old = FilterHost.tankOf(level, f.tankBlockPos());
        BlockPos pos = f.getBlockPos();
        Direction d = null;
        for (Direction h : Direction.Plane.HORIZONTAL) {
            if (pos.relative(h).getX() == wall.getX() && pos.relative(h).getZ() == wall.getZ()) d = h;
        }
        if (d == null) return null;
        f.setLink(wall);
        BlockState s = level.getBlockState(pos);
        if (s.getBlock() instanceof ExternalFilterBlock && s.getValue(ExternalFilterBlock.FACING) != d) {
            level.setBlock(pos, s.setValue(ExternalFilterBlock.FACING, d), Block.UPDATE_ALL);
        }
        if (old != null) org.overengineer.bettacraft.tank.TankEquipment.invalidate(old);
        org.overengineer.bettacraft.tank.TankEquipment.invalidate(c);
        org.overengineer.bettacraft.tank.TankCurrents.invalidate(c);
        if (old != null && old != c) org.overengineer.bettacraft.tank.TankCurrents.invalidate(old);
        return c;
    }
}
