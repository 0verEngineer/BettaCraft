package org.overengineer.bettacraft.client.interior;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorSnapshot;

/** Client copy of an interior region's blocks (as received), plus its cached miniature mesh. */
public final class ClientRegion {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    final int id;
    final int version;
    final int sizeX, sizeY, sizeZ, scale;
    final boolean truncated;
    private final Long2ObjectOpenHashMap<InteriorSnapshot.Section> sections = new Long2ObjectOpenHashMap<>();
    int nonAir;

    @Nullable
    MiniatureMesh mesh;
    boolean meshDirty = true;
    int lightKey = Integer.MIN_VALUE;
    int idleTicks;

    ClientRegion(int id, int version, int sizeX, int sizeY, int sizeZ, int scale, boolean truncated, List<InteriorSnapshot.Section> list) {
        this.id = id;
        this.version = version;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.scale = scale;
        this.truncated = truncated;
        for (InteriorSnapshot.Section s : list) {
            sections.put(SectionPos.asLong(s.sx(), s.sy(), s.sz()), s);
            nonAir += s.nonAir();
        }
    }

    /** Block at interior-local coordinates (0,0,0 = first block above the floor). */
    public BlockState get(int lx, int ly, int lz) {
        if (lx < 0 || ly < 0 || lz < 0 || lx >= sizeX || ly >= sizeY || lz >= sizeZ) return AIR;
        int wy = ly + InteriorRegion.BASE_Y; // world y in the interior dimension; x/z origins are chunk aligned
        InteriorSnapshot.Section s = sections.get(SectionPos.asLong(lx >> 4, wy >> 4, lz >> 4));
        if (s == null) return AIR;
        return s.palette()[s.indices()[InteriorSnapshot.index(lx & 15, wy & 15, lz & 15)]];
    }

    Iterable<InteriorSnapshot.Section> sections() {
        return sections.values();
    }

    void close() {
        if (mesh != null) {
            mesh.close();
            mesh = null;
        }
    }
}
