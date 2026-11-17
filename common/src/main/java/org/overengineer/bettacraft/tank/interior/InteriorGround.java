package org.overengineer.bettacraft.tank.interior;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.plants.AquaticPlantBlock;
import org.overengineer.bettacraft.plants.PlantPart;

/**
 * Height of the walkable top of every interior column (M14): where shrimp, crayfish and snails walk on substrate,
 * decoration and plants. One value per interior block column, in 1/16 of an interior block above the interior floor (0 =
 * the floor). Solid blocks count with the top of their collision shape, aquarium plants (no collision) with their leaves
 * (the flat carpet low), the invisible walls of a free shape like solid blocks. Computed lazily, cached, invalidated by
 * block changes like {@link InteriorObstacles}; columns in interior chunks that are not loaded get a short ticket and count
 * as floor until they are there.
 */
public final class InteriorGround {
    private static final short UNKNOWN = Short.MIN_VALUE;

    private static final class Map {
        final int sx, sy, sz;
        final short[] top;
        final Long2LongOpenHashMap ticketTime = new Long2LongOpenHashMap();

        Map(InteriorRegion r) {
            sx = r.sizeX();
            sy = r.sizeY();
            sz = r.sizeZ();
            top = new short[sx * sz];
            java.util.Arrays.fill(top, UNKNOWN);
        }
    }

    private static final Int2ObjectOpenHashMap<Map> MAPS = new Int2ObjectOpenHashMap<>();

    private InteriorGround() {}

    static void invalidate(int regionId, int lx, int lz) {
        Map m = MAPS.get(regionId);
        if (m == null || lx < 0 || lz < 0 || lx >= m.sx || lz >= m.sz) return;
        m.top[lz * m.sx + lx] = UNKNOWN;
    }

    public static void forget(int regionId) {
        MAPS.remove(regionId);
    }

    public static void clearAll() {
        MAPS.clear();
    }

    /**
     * Top of the walkable surface of the interior column (lx, lz) in 1/16 interior block above the floor, 0 when the
     * column is empty, the interior is not there or its chunk is not loaded yet.
     */
    public static int top16(MinecraftServer server, InteriorRegion r, int lx, int lz) {
        if (lx < 0 || lz < 0 || lx >= r.sizeX() || lz >= r.sizeZ()) return 0;
        ServerLevel interior = InteriorManager.interiorLevel(server);
        if (interior == null || !r.built()) return 0;
        Map m = MAPS.get(r.id());
        if (m == null || m.sx != r.sizeX() || m.sz != r.sizeZ() || m.sy != r.sizeY()) {
            m = new Map(r);
            MAPS.put(r.id(), m);
        }
        int i = lz * m.sx + lx;
        if (m.top[i] == UNKNOWN) {
            int v = compute(interior, r, m, lx, lz);
            if (v == UNKNOWN) return 0;
            m.top[i] = (short) v;
        }
        return m.top[i];
    }

    /**
     * M21: the walkable surface of the column (lx, lz) at or below the interior height {@code from16} (1/16 interior
     * block above the floor): under an overhang or in a cave the floor there, not the top of the rock above. Not cached
     * (a short scan of one column); -1 when the chunk is not loaded.
     */
    public static int floor16(MinecraftServer server, InteriorRegion r, int lx, int lz, int from16) {
        if (lx < 0 || lz < 0 || lx >= r.sizeX() || lz >= r.sizeZ()) return 0;
        ServerLevel interior = InteriorManager.interiorLevel(server);
        if (interior == null || !r.built()) return 0;
        int x = r.originX() + lx, z = r.originZ() + lz;
        LevelChunk chunk = interior.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (chunk == null) return -1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int start = Math.min(r.sizeY() - 1, Math.max(0, Math.floorDiv(from16, 16)));
        for (int ly = start; ly >= 0; ly--) {
            p.set(x, InteriorRegion.BASE_Y + ly, z);
            int top = surface(interior, chunk, p, ly);
            if (top < 0) continue;
            // M22.5: the height lies inside a solid block: the ground is the top of the whole solid stack, not of the
            // block it was found in (a crayfish beside a pillar under a table stood inside the pillar's next block)
            if (ly == start && top > from16) {
                for (int up = ly + 1; up < r.sizeY(); up++) {
                    p.set(x, InteriorRegion.BASE_Y + up, z);
                    int above = surface(interior, chunk, p, up);
                    if (above < 0 || above <= top) break;
                    top = above;
                }
            }
            return top;
        }
        return 0;
    }

    /** Top of the walkable surface of one block in 1/16 above the interior floor, -1 when it is passable. */
    private static int surface(ServerLevel interior, LevelChunk chunk, BlockPos.MutableBlockPos p, int ly) {
        BlockState s = chunk.getBlockState(p);
        if (s.isAir()) return -1;
        if (InteriorManager.isShellBlock(s)) return (ly + 1) * 16;
        if (s.getBlock() instanceof AquaticPlantBlock) {
            PlantPart part = s.getValue(AquaticPlantBlock.PART);
            if (part == PlantPart.CUTTING) return -1;
            return ly * 16 + (part == PlantPart.MAT ? 6 : 16);
        }
        VoxelShape shape = s.getCollisionShape(interior, p);
        if (shape.isEmpty()) return -1;
        return ly * 16 + (int) Math.round(Math.max(1, Math.min(16, shape.max(Direction.Axis.Y) * 16)));
    }

    private static int compute(ServerLevel interior, InteriorRegion r, Map m, int lx, int lz) {
        int x = r.originX() + lx, z = r.originZ() + lz;
        LevelChunk chunk = interior.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (chunk == null) {
            long key = ChunkPos.asLong(x >> 4, z >> 4);
            long now = interior.getGameTime();
            if (now - m.ticketTime.getOrDefault(key, -1000L) > 200) {
                m.ticketTime.put(key, now);
                interior.getChunkSource().addRegionTicket(InteriorPreparation.TICKET, new ChunkPos(key), 0, new ChunkPos(key));
            }
            return UNKNOWN;
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int ly = r.sizeY() - 1; ly >= 0; ly--) {
            p.set(x, InteriorRegion.BASE_Y + ly, z);
            BlockState s = chunk.getBlockState(p);
            if (s.isAir()) continue;
            if (InteriorManager.isShellBlock(s)) return (ly + 1) * 16;
            if (s.getBlock() instanceof AquaticPlantBlock) {
                PlantPart part = s.getValue(AquaticPlantBlock.PART);
                if (part == PlantPart.CUTTING) continue;
                return ly * 16 + (part == PlantPart.MAT ? 6 : 16);
            }
            VoxelShape shape = s.getCollisionShape(interior, p);
            if (shape.isEmpty()) continue;
            return ly * 16 + (int) Math.round(Math.max(1, Math.min(16, shape.max(Direction.Axis.Y) * 16)));
        }
        return 0;
    }
}
