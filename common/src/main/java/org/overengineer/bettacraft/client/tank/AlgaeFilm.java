package org.overengineer.bettacraft.client.tank;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.algae.AlgaeSim;

/**
 * M66 v6: the algae on the inside of the glass are part of the tank's chunk mesh: a film quad on every water side face with
 * algae, at exactly the corners of that face (so it sorts with the glass and the water like the water itself, see
 * {@link TankWaterGeometry}), drawn right after the water face by the same mesher (the fluid path of {@link TankWaterRenderer}
 * without a shader pack, the model's water quads with one).
 * <p>
 * Why (the causes the earlier block entity film had, proven by the M66 v6 tour pictures): it was drawn by the controller's
 * block entity renderer with a translucent entity render type, which writes depth and is drawn before the translucent chunk
 * layer. Every film quad therefore hid all glass and water behind it (of its own tank and of any tank behind), and which
 * pieces were hidden depended on the draw order, so the patches jumped with the camera. In the chunk mesh the film is
 * sorted back to front together with the glass and the water: nothing behind it goes missing, and it costs nothing per frame
 * (the section is meshed again only when a film level of one of its blocks changes).
 * <p>
 * Two quads per face: the front one (normal out of the tank) is seen through the glass from outside, the back one (normal
 * into the tank) is the film of the far wall seen through the water, faded by the depth of water in front of it ({@link #back}).
 * Back face culling shows only one of them at a time. The texture is in world space: {@link #SPRITES} tile over {@link #TILE} x {@link #TILE} blocks
 * and every level covers the pixels of the lower ones plus more ({@code tools/assetgen/critters.py}), so neighbouring blocks
 * at different levels read as one film that is denser on one side, not as squares.
 * <p>
 * The levels per block live in {@link #FILM} (written on the main thread from the controller's synced tank view, read by
 * the chunk meshing threads).
 */
public final class AlgaeFilm {
    /** Number of film textures (density levels). The front film of stage s uses level 2s. */
    public static final int LEVELS = 8;
    public static final Identifier[] SPRITES = new Identifier[LEVELS + 1];

    static {
        for (int i = 1; i <= LEVELS; i++) SPRITES[i] = BettaCraft.id("block/algae_film_" + i);
    }

    /** The film's texture spans this many blocks each way (64 px, 16 per block). */
    public static final int TILE = 4;
    /** Water depth (blocks) at which the far film keeps 1/e of its density. */
    static final float WATER_CLARITY = 8F;
    /** Longest water path counted (blocks). */
    private static final int MAX_PATH = 48;

    /** Packed levels per block position (see {@link #pack}); absent = no film. */
    private static final Map<Long, Integer> FILM = new ConcurrentHashMap<>();
    /** The positions each controller put into {@link #FILM} (main thread only). */
    private static final Map<BlockPos, LongOpenHashSet> BY_CONTROLLER = new HashMap<>();
    /** The client level {@link #FILM} belongs to. */
    private static volatile Object owner;

    /** Dev tour only: no film at all (the "algae off" frame time samples); the chunks have to be meshed again after a change. */
    public static volatile boolean hiddenForTest;

    private AlgaeFilm() {}

    /** The packed film levels of the tank block at {@code pos} (0 = none), any thread. */
    public static int at(BlockPos pos) {
        if (hiddenForTest || FILM.isEmpty() || owner != Minecraft.getInstance().level) return 0;
        Integer v = FILM.get(pos.asLong());
        return v == null ? 0 : v;
    }

    /** Level (0..{@link #LEVELS}) of the front film of the glass face towards {@code d}. */
    public static int front(int packed, Direction d) {
        return d.getAxis() == Direction.Axis.Y ? 0 : (packed >>> (d.get2DDataValue() * 8)) & 15;
    }

    /** Level (0..{@link #LEVELS}) of the back film (seen from inside the tank) of the glass face towards {@code d}. */
    public static int back(int packed, Direction d) {
        return d.getAxis() == Direction.Axis.Y ? 0 : (packed >>> (d.get2DDataValue() * 8 + 4)) & 15;
    }

    /**
     * The packed levels of one tank block: per glass face (2D data value d) the front level in bits 8d..8d+3 and the back level
     * in bits 8d+4..8d+7. {@code stages[d]} is the synced algae level (stage / 4); faces with a tank block beyond them are no glass.
     */
    static int pack(TankData t, long pos, float[] stages) {
        int x = BlockPos.getX(pos), y = BlockPos.getY(pos), z = BlockPos.getZ(pos);
        int packed = 0;
        for (int d = 0; d < 4; d++) {
            int stage = Math.min(AlgaeSim.STAGES, Math.round(stages[d] * AlgaeSim.STAGES));
            if (stage <= 0) continue;
            int nx = AlgaeSim.stepX(d), nz = AlgaeSim.stepZ(d);
            if (t.contains(BlockPos.asLong(x + nx, y, z + nz))) continue;
            int front = stage * LEVELS / AlgaeSim.STAGES;
            // the water between this film and the opposite glass, through which it is seen from outside
            int run = 1;
            while (run < MAX_PATH && t.contains(BlockPos.asLong(x - nx * run, y, z - nz * run))) run++;
            int back = Math.round(front * (float) Math.exp(-run / WATER_CLARITY));
            packed |= (front | back << 4) << (d * 8);
        }
        return packed;
    }

    /** The controller's synced tank view changed (or it loaded): its blocks get their new film, changed sections are meshed again. */
    public static void update(TankBlockEntity be) {
        var mc = Minecraft.getInstance();
        if (be.getLevel() != mc.level || mc.level == null) return;
        if (owner != mc.level) {
            FILM.clear();
            BY_CONTROLLER.clear();
            owner = mc.level;
        }
        TankData t = be.isController() && !be.isRemoved() ? be.getTank() : null;
        LongOpenHashSet old = BY_CONTROLLER.remove(be.getBlockPos());
        LongOpenHashSet now = new LongOpenHashSet();
        LongOpenHashSet dirty = new LongOpenHashSet();
        if (t != null) {
            for (var e : t.algae().long2ObjectEntrySet()) {
                long pos = e.getLongKey();
                if (!t.contains(pos)) continue;
                int packed = pack(t, pos, e.getValue());
                if (packed == 0) continue;
                now.add(pos);
                Integer prev = FILM.put(pos, packed);
                if (prev == null || prev != packed) dirty.add(SectionPos.asLong(BlockPos.getX(pos) >> 4, BlockPos.getY(pos) >> 4, BlockPos.getZ(pos) >> 4));
            }
        }
        if (old != null) {
            for (long pos : old) {
                if (now.contains(pos)) continue;
                if (FILM.remove(pos) != null) dirty.add(SectionPos.asLong(BlockPos.getX(pos) >> 4, BlockPos.getY(pos) >> 4, BlockPos.getZ(pos) >> 4));
            }
        }
        if (!now.isEmpty()) BY_CONTROLLER.put(be.getBlockPos().immutable(), now);
        for (long s : dirty) mc.levelRenderer.setSectionDirty(SectionPos.x(s), SectionPos.y(s), SectionPos.z(s));
    }

    /** The controller left the client (removed, chunk unloaded, no controller any more): its film goes. */
    public static void remove(TankBlockEntity be) {
        var mc = Minecraft.getInstance();
        if (be.getLevel() != mc.level || owner != mc.level) return;
        LongOpenHashSet old = BY_CONTROLLER.remove(be.getBlockPos());
        if (old == null) return;
        LongOpenHashSet dirty = new LongOpenHashSet();
        for (long pos : old) if (FILM.remove(pos) != null) dirty.add(SectionPos.asLong(BlockPos.getX(pos) >> 4, BlockPos.getY(pos) >> 4, BlockPos.getZ(pos) >> 4));
        for (long s : dirty) mc.levelRenderer.setSectionDirty(SectionPos.x(s), SectionPos.y(s), SectionPos.z(s));
    }

    /** Dev tour: every film again (after {@link #hiddenForTest} changed): all chunks are meshed again. */
    public static void refreshAllForTest() {
        Minecraft.getInstance().levelRenderer.allChanged();
    }

    /**
     * Film texture coordinates of a corner of a side face of the block at {@code pos} (in 0..1 of the sprite): in world space,
     * one sprite over {@link #TILE} blocks each way, so the film runs on across the block borders of a wall without a seam (only
     * the position modulo {@link #TILE} matters).
     */
    public static float u(Direction d, BlockPos pos, float x, float y, float z) {
        // along the wall, in the direction the face's own u runs (BoxFaces.u): +x south, -x north, +z west, -z east
        float local = BoxFaces.u(d, x, y, z);
        int block = switch (d) {
            case SOUTH -> pos.getX();
            case NORTH -> -pos.getX() - 1;
            case WEST -> pos.getZ();
            default -> -pos.getZ() - 1;
        };
        return (Math.floorMod(block, TILE) + local) / TILE;
    }

    public static float v(BlockPos pos, float y) {
        return (Math.floorMod(-pos.getY() - 1, TILE) + 1 - y) / TILE;
    }
}
