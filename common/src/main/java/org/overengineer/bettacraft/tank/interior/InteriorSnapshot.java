package org.overengineer.bettacraft.tank.interior;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Compact, palette-based encoding of the non-air blocks of an interior region (PLAN 7.4 "palettierte Sections").
 * <p>
 * Only non-empty 16x16x16 sections are written (delta updates also write emptied sections, palette size 0). Per
 * section: palette of block state ids, then a sparse list (index, palette entry), a dense bit-packed array or a run
 * length encoding (M3), whichever is smallest. Air is never stored. Section coordinates are relative to the region's
 * origin chunk; y is the world section index, {@link #FLOOR_SY} marks the floor layer (substrate, y index 0 only).
 */
public final class InteriorSnapshot {
    public static final int SECTION_VOLUME = 4096;
    /** Section y of the pseudo section holding the floor substrate of one chunk column (cells at y index 0). */
    public static final int FLOOR_SY = -1;

    /** Decoded section: palette (index 0 = air) and 4096 palette indices (x + z*16 + y*256). */
    public record Section(int sx, int sy, int sz, BlockState[] palette, short[] indices, int nonAir) {}

    public record Stats(int sections, int nonAir, int bytes, long encodeNanos) {}

    private final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(1024));
    private final IntArrayList sectionStarts = new IntArrayList();
    private int nonAir;
    private int sections;
    private long nanos;

    /** Encodes one section. {@code states[i]} may be null or air for empty cells. */
    public void addSection(int sx, int sy, int sz, BlockState[] states) {
        addSection(sx, sy, sz, states, false);
    }

    /** As {@link #addSection(int, int, int, BlockState[])}; {@code keepEmpty} writes empty sections too (delta: "cleared"). */
    public void addSection(int sx, int sy, int sz, BlockState[] states, boolean keepEmpty) {
        long t0 = System.nanoTime();
        Int2IntOpenHashMap paletteIndex = new Int2IntOpenHashMap();
        IntArrayList palette = new IntArrayList();
        short[] idx = new short[SECTION_VOLUME];
        int count = 0;
        for (int i = 0; i < SECTION_VOLUME; i++) {
            BlockState s = states[i];
            if (s == null || s.isAir()) continue;
            int id = Block.BLOCK_STATE_REGISTRY.getId(s);
            int p = paletteIndex.getOrDefault(id, -1);
            if (p < 0) {
                p = palette.size();
                palette.add(id);
                paletteIndex.put(id, p);
            }
            idx[i] = (short) (p + 1);
            count++;
        }
        if (count == 0 && !keepEmpty) return;
        sectionStarts.add(buf.writerIndex());
        buf.writeVarInt(sx);
        buf.writeVarInt(sy);
        buf.writeVarInt(sz);
        buf.writeVarInt(palette.size());
        for (int i = 0; i < palette.size(); i++) buf.writeVarInt(palette.getInt(i));
        int bits = Math.max(1, Mth.ceillog2(palette.size() + 1));
        int perLong = 64 / bits;
        int denseBytes = ((SECTION_VOLUME + perLong - 1) / perLong) * 8;
        int sparseBytes = count * (2 + (palette.size() > 127 ? 2 : 1));
        int runs = 0;
        int rleBytes = 0;
        for (int i = 0; i < SECTION_VOLUME; ) {
            int j = i;
            while (j < SECTION_VOLUME && idx[j] == idx[i]) j++;
            runs++;
            rleBytes += varIntSize(j - i) + varIntSize(idx[i]);
            i = j;
        }
        if (rleBytes < sparseBytes && rleBytes < denseBytes) {
            buf.writeByte(2);
            buf.writeVarInt(runs);
            for (int i = 0; i < SECTION_VOLUME; ) {
                int j = i;
                while (j < SECTION_VOLUME && idx[j] == idx[i]) j++;
                buf.writeVarInt(j - i);
                buf.writeVarInt(idx[i]);
                i = j;
            }
        } else if (sparseBytes <= denseBytes) {
            buf.writeByte(0);
            buf.writeVarInt(count);
            for (int i = 0; i < SECTION_VOLUME; i++) {
                if (idx[i] != 0) {
                    buf.writeShort(i);
                    buf.writeVarInt(idx[i] - 1);
                }
            }
        } else {
            buf.writeByte(1);
            buf.writeByte(bits);
            long[] packed = new long[(SECTION_VOLUME + perLong - 1) / perLong];
            for (int i = 0; i < SECTION_VOLUME; i++) {
                packed[i / perLong] |= ((long) idx[i]) << ((i % perLong) * bits);
            }
            for (long l : packed) buf.writeLong(l);
        }
        nonAir += count;
        sections++;
        nanos += System.nanoTime() - t0;
    }

    public Stats stats() {
        return new Stats(sections, nonAir, buf.writerIndex(), nanos);
    }

    public int nonAir() {
        return nonAir;
    }

    /** Splits the encoded data into parts of at most {@code maxPartBytes}, cutting only at section boundaries. */
    public List<byte[]> toParts(int maxPartBytes) {
        List<byte[]> parts = new ArrayList<>();
        int end = buf.writerIndex();
        IntArrayList bounds = new IntArrayList(sectionStarts);
        bounds.add(end);
        int partStart = 0;
        for (int k = 1; k < bounds.size(); k++) {
            int prev = bounds.getInt(k - 1);
            if (bounds.getInt(k) - partStart > maxPartBytes && prev > partStart) {
                parts.add(slice(partStart, prev));
                partStart = prev;
            }
        }
        if (end > partStart || parts.isEmpty()) parts.add(slice(partStart, end));
        return parts;
    }

    private byte[] slice(int from, int to) {
        byte[] b = new byte[to - from];
        buf.getBytes(from, b);
        return b;
    }

    /** Decodes one part (a sequence of sections). Unknown state ids decode as air. */
    public static List<Section> decode(byte[] data) {
        List<Section> out = new ArrayList<>();
        ByteBuf raw = Unpooled.wrappedBuffer(data);
        FriendlyByteBuf in = new FriendlyByteBuf(raw);
        while (in.isReadable()) {
            int sx = in.readVarInt(), sy = in.readVarInt(), sz = in.readVarInt();
            int pn = in.readVarInt();
            if (pn < 0 || pn > SECTION_VOLUME) break; // M17: malformed data (a section has at most 4096 states)
            BlockState[] palette = new BlockState[pn + 1];
            palette[0] = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            for (int i = 0; i < pn; i++) {
                BlockState s = Block.BLOCK_STATE_REGISTRY.byId(in.readVarInt());
                palette[i + 1] = s == null ? palette[0] : s;
            }
            short[] idx = new short[SECTION_VOLUME];
            int mode = in.readByte();
            int count = 0;
            if (mode == 2) {
                int runs = in.readVarInt();
                int i = 0;
                for (int k = 0; k < runs && i < SECTION_VOLUME; k++) {
                    int len = in.readVarInt();
                    short v = (short) in.readVarInt();
                    if (v > pn || v < 0) v = 0; // M17: negative indices broke the mesher later
                    if (len < 0) break;
                    for (int e = Math.min(SECTION_VOLUME, i + len); i < e; i++) idx[i] = v;
                    if (v != 0) count += len;
                }
            } else if (mode == 0) {
                count = in.readVarInt();
                for (int k = 0; k < count; k++) {
                    int i = in.readShort() & 0xFFFF;
                    int v = in.readVarInt() + 1;
                    if (i < SECTION_VOLUME && v > 0 && v <= pn) idx[i] = (short) v; // M17: bounds checked
                }
            } else {
                int bits = in.readByte();
                if (bits < 1 || bits > 16) break; // M17: malformed data
                int perLong = 64 / bits;
                long mask = (1L << bits) - 1;
                long[] packed = new long[(SECTION_VOLUME + perLong - 1) / perLong];
                for (int k = 0; k < packed.length; k++) packed[k] = in.readLong();
                for (int i = 0; i < SECTION_VOLUME; i++) {
                    idx[i] = (short) ((packed[i / perLong] >>> ((i % perLong) * bits)) & mask);
                    if (idx[i] > pn) idx[i] = 0;
                    if (idx[i] != 0) count++;
                }
            }
            out.add(new Section(sx, sy, sz, palette, idx, count));
        }
        return out;
    }

    private static int varIntSize(int v) {
        int n = 1;
        while ((v & ~0x7F) != 0) {
            v >>>= 7;
            n++;
        }
        return n;
    }

    /** Raw encoded bytes (bench). */
    public byte[] bytes() {
        return slice(0, buf.writerIndex());
    }

    /** Size after zlib compression, as the network compression would roughly see it (bench). */
    public static int deflatedSize(byte[] data) {
        java.util.zip.Deflater d = new java.util.zip.Deflater();
        d.setInput(data);
        d.finish();
        byte[] out = new byte[64 * 1024];
        int total = 0;
        while (!d.finished()) total += d.deflate(out);
        d.end();
        return total;
    }

    public static int index(int x, int y, int z) {
        return x | (z << 4) | (y << 8);
    }
}
