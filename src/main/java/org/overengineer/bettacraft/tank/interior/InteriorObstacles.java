package org.overengineer.bettacraft.tank.interior;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.tank.TankData;

/**
 * Coarse occupancy of a tank interior for our bettas (PLAN 4.2: "Bettas weichen der Innenraum-Deko aus"). One cell is
 * a quarter of a tank block ({@code scale / 4} interior blocks, at least one); a cell is solid if any interior block in
 * it has a collision shape (plants without collision stay passable). Cells are computed lazily from the interior
 * dimension and cached; block changes invalidate their cell. Interior chunks that are not loaded are requested with a
 * short ticket and count as free until they are there.
 */
public final class InteriorObstacles {
    private static final byte UNKNOWN = 0, FREE = 1, SOLID = 2;

    private static final class Grid {
        final int cell, nx, ny, nz;
        final byte[] cells;
        final Long2LongOpenHashMap ticketTime = new Long2LongOpenHashMap();

        Grid(InteriorRegion r) {
            cell = Math.max(1, r.scale() / 4);
            nx = (r.sizeX() + cell - 1) / cell;
            ny = (r.sizeY() + cell - 1) / cell;
            nz = (r.sizeZ() + cell - 1) / cell;
            cells = new byte[nx * ny * nz];
        }
    }

    private static final Int2ObjectOpenHashMap<Grid> GRIDS = new Int2ObjectOpenHashMap<>();

    private InteriorObstacles() {}

    static void invalidate(int regionId, int lx, int ly, int lz) {
        Grid g = GRIDS.get(regionId);
        if (g == null) return;
        int cx = lx / g.cell, cy = ly / g.cell, cz = lz / g.cell;
        if (cx >= 0 && cy >= 0 && cz >= 0 && cx < g.nx && cy < g.ny && cz < g.nz) g.cells[(cy * g.nz + cz) * g.nx + cx] = UNKNOWN;
    }

    public static void forget(int regionId) {
        GRIDS.remove(regionId);
    }

    public static void clearAll() {
        GRIDS.clear();
    }

    /** Whether a world position inside the tank lies in solid interior decoration. */
    public static boolean blocked(MinecraftServer server, TankData tank, Vec3 world) {
        InteriorRegion r = InteriorRegions.get(server).get(tank.regionId());
        if (r == null || !r.built()) return false;
        ServerLevel interior = InteriorManager.interiorLevel(server);
        if (interior == null) return false;
        Grid g = GRIDS.get(r.id());
        if (g == null || g.cells.length != ((r.sizeY() + g.cell - 1) / g.cell) * ((r.sizeZ() + g.cell - 1) / g.cell) * ((r.sizeX() + g.cell - 1) / g.cell)) {
            g = new Grid(r);
            GRIDS.put(r.id(), g);
        }
        BlockPos min = tank.min();
        int s = r.scale();
        int lx = (int) Math.floor((world.x - min.getX()) * s), ly = (int) Math.floor((world.y - min.getY()) * s),
                lz = (int) Math.floor((world.z - min.getZ()) * s);
        if (lx < 0 || ly < 0 || lz < 0 || lx >= r.sizeX() || ly >= r.sizeY() || lz >= r.sizeZ()) return false;
        int cx = lx / g.cell, cy = ly / g.cell, cz = lz / g.cell;
        int i = (cy * g.nz + cz) * g.nx + cx;
        if (g.cells[i] == UNKNOWN) g.cells[i] = compute(interior, r, g, cx, cy, cz);
        return g.cells[i] == SOLID;
    }

    private static byte compute(ServerLevel interior, InteriorRegion r, Grid g, int cx, int cy, int cz) {
        int x0 = r.originX() + cx * g.cell, y0 = InteriorRegion.BASE_Y + cy * g.cell, z0 = r.originZ() + cz * g.cell;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = x0; x < x0 + g.cell; x++)
            for (int z = z0; z < z0 + g.cell; z++) {
                LevelChunk chunk = interior.getChunkSource().getChunkNow(x >> 4, z >> 4);
                if (chunk == null) {
                    long key = ChunkPos.asLong(x >> 4, z >> 4);
                    long now = interior.getGameTime();
                    if (now - g.ticketTime.getOrDefault(key, -1000L) > 200) {
                        g.ticketTime.put(key, now);
                        interior.getChunkSource().addRegionTicket(InteriorPreparation.TICKET, new ChunkPos(key), 0, new ChunkPos(key));
                    }
                    return UNKNOWN;
                }
                for (int y = y0; y < y0 + g.cell; y++) {
                    BlockState s = chunk.getBlockState(p.set(x, y, z));
                    if (s.isAir() || InteriorManager.isShellBlock(s)) continue;
                    // M13: aquarium plants have no collision (players walk through) but bettas swim around them
                    if (s.getBlock() instanceof org.overengineer.bettacraft.plants.AquaticPlantBlock
                            && s.getValue(org.overengineer.bettacraft.plants.AquaticPlantBlock.PART).obstacle()) return SOLID;
                    if (!s.getCollisionShape(interior, p).isEmpty()) return SOLID;
                }
            }
        return FREE;
    }
}
