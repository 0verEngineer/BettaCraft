package org.overengineer.bettacraft.decor;

import org.overengineer.bettacraft.util.NbtCompat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Blocks of a decoration structure (relative positions, air left out) and the shared placement layout used by the
 * server (placing) and the client (preview hologram): the footprint is centred on the target block, the structure's
 * north side (its front) faces the player.
 */
public record DecorTemplate(int sizeX, int sizeY, int sizeZ, List<Entry> blocks) {
    public record Entry(BlockPos pos, BlockState state) {}

    /** A placed block. */
    public record Placed(BlockPos pos, BlockState state) {}

    /** Reads the template from the server's structure manager (datapacks may replace it). */
    public static Optional<DecorTemplate> load(ServerLevel level, Identifier id) {
        Optional<StructureTemplate> t = level.getStructureManager().get(id);
        if (t.isEmpty()) return Optional.empty();
        CompoundTag nbt = t.get().save(new CompoundTag());
        HolderGetter<Block> blocks = level.holderLookup(net.minecraft.core.registries.Registries.BLOCK);
        ListTag palette = nbt.getListOrEmpty("palette");
        if (palette.isEmpty() && NbtCompat.has(nbt, "palettes", Tag.TAG_LIST)) palette = nbt.getListOrEmpty("palettes").getListOrEmpty(0);
        List<BlockState> states = new ArrayList<>();
        for (int i = 0; i < palette.size(); i++) states.add(NbtUtils.readBlockState(blocks, palette.getCompoundOrEmpty(i)));
        List<Entry> entries = new ArrayList<>();
        ListTag list = nbt.getListOrEmpty("blocks");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag b = list.getCompoundOrEmpty(i);
            ListTag pos = b.getListOrEmpty("pos");
            int s = b.getIntOr("state", 0);
            if (s < 0 || s >= states.size() || states.get(s).isAir()) continue;
            entries.add(new Entry(new BlockPos(pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0)), states.get(s)));
        }
        entries.sort(Comparator.comparingInt((Entry e) -> e.pos().getY()));
        ListTag size = nbt.getListOrEmpty("size");
        return Optional.of(new DecorTemplate(size.getIntOr(0, 0), size.getIntOr(1, 0), size.getIntOr(2, 0), List.copyOf(entries)));
    }

    /** Rotation that turns the structure's north side towards the player. */
    public static Rotation rotationFor(Direction playerFacing) {
        return switch (playerFacing.getOpposite()) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /** World positions and rotated states when placed with the footprint centred on {@code base}. */
    public List<Placed> layout(BlockPos base, Rotation rot) {
        int cx = (sizeX - 1) / 2, cz = (sizeZ - 1) / 2;
        List<Placed> out = new ArrayList<>(blocks.size());
        for (Entry e : blocks) {
            int dx = e.pos().getX() - cx, dz = e.pos().getZ() - cz;
            int rx, rz;
            switch (rot) {
                case CLOCKWISE_90 -> { rx = -dz; rz = dx; }
                case CLOCKWISE_180 -> { rx = -dx; rz = -dz; }
                case COUNTERCLOCKWISE_90 -> { rx = dz; rz = -dx; }
                default -> { rx = dx; rz = dz; }
            }
            @SuppressWarnings("deprecation")
            BlockState s = e.state().rotate(rot);
            out.add(new Placed(base.offset(rx, e.pos().getY(), rz), s));
        }
        return out;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(sizeX);
        buf.writeVarInt(sizeY);
        buf.writeVarInt(sizeZ);
        buf.writeVarInt(blocks.size());
        for (Entry e : blocks) {
            buf.writeBlockPos(e.pos());
            buf.writeVarInt(Block.BLOCK_STATE_REGISTRY.getId(e.state()));
        }
    }

    public static DecorTemplate read(FriendlyByteBuf buf) {
        int sx = buf.readVarInt(), sy = buf.readVarInt(), sz = buf.readVarInt();
        int n = Math.min(buf.readVarInt(), 65536);
        List<Entry> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            BlockPos p = buf.readBlockPos();
            BlockState s = Block.BLOCK_STATE_REGISTRY.byId(buf.readVarInt());
            if (s != null && !s.isAir()) list.add(new Entry(p, s));
        }
        return new DecorTemplate(sx, sy, sz, List.copyOf(list));
    }
}
