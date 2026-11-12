package org.overengineer.bettacraft.tank;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-side multiblock logic: forming, merging and splitting tanks.
 * <ul>
 *     <li>Only tank blocks with the <b>same glass</b> connect.</li>
 *     <li>Tanks are cuboids for now. A block that would create a non-cuboid tank stays a separate tank (hint in the
 *     action bar). Breaking a block out of a cuboid splits the rest into cuboids (greedy decomposition).</li>
 *     <li>Works across chunk borders as long as the chunks are loaded (unloaded neighbours never connect).</li>
 * </ul>
 */
public final class TankMultiblock {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/tank");

    private TankMultiblock() {}

    @Nullable
    public static TankBlockEntity getBE(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TankBlockEntity be ? be : null;
    }

    /** The controller of the tank that contains {@code pos}, or null (not formed / controller not loaded). */
    @Nullable
    public static TankBlockEntity getController(Level level, BlockPos pos) {
        TankBlockEntity be = getBE(level, pos);
        if (be == null || be.getControllerPos() == null) return null;
        if (be.isController()) return be;
        BlockPos cp = be.getControllerPos();
        if (!level.isLoaded(cp)) return null;
        TankBlockEntity c = getBE(level, cp);
        return c != null && c.isController() ? c : null;
    }

    // ------------------------------------------------------------------ adding

    public static void onTankBlockAdded(ServerLevel level, BlockPos pos, @Nullable ServerPlayer player) {
        TankBlockEntity be = getBE(level, pos);
        if (be == null || be.getControllerPos() != null) return;
        ResourceLocation glass = be.getGlassId();

        Map<BlockPos, TankBlockEntity> candidates = new LinkedHashMap<>();
        boolean unloadedNeighbour = false;
        boolean otherGlass = false;
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            if (!level.isLoaded(n)) continue;
            TankBlockEntity nbe = getBE(level, n);
            if (nbe == null) continue;
            if (!glass.equals(nbe.getGlassId())) {
                otherGlass = true;
                continue;
            }
            BlockPos cp = nbe.getControllerPos();
            if (cp == null) continue;
            if (!level.isLoaded(cp)) {
                unloadedNeighbour = true;
                continue;
            }
            TankBlockEntity c = getBE(level, cp);
            if (c != null && c.isController()) candidates.putIfAbsent(cp.immutable(), c);
        }

        List<TankBlockEntity> chosen = chooseMerge(pos, new ArrayList<>(candidates.values()));
        if (chosen.isEmpty()) {
            install(level, newTank(List.of(pos), ServerConfig.defaultScale(), player));
            if (player != null) {
                if (!candidates.isEmpty()) {
                    player.displayClientMessage(Component.translatable("message.bettacraft.tank.not_cuboid"), true);
                } else if (unloadedNeighbour) {
                    player.displayClientMessage(Component.translatable("message.bettacraft.tank.unloaded"), true);
                } else if (otherGlass) {
                    player.displayClientMessage(Component.translatable("message.bettacraft.tank.other_glass"), true);
                }
            }
            return;
        }
        merge(level, chosen, pos);
        if (player != null && chosen.size() < candidates.size()) {
            player.displayClientMessage(Component.translatable("message.bettacraft.tank.not_cuboid"), true);
        }
    }

    /** Picks which neighbouring tanks the new block merges with: all if possible, else the largest that fits. */
    private static List<TankBlockEntity> chooseMerge(BlockPos pos, List<TankBlockEntity> candidates) {
        if (candidates.isEmpty()) return List.of();
        int scale = candidates.getFirst().getTank().scale();
        boolean sameScale = candidates.stream().allMatch(c -> c.getTank().scale() == scale);
        if (sameScale && fitsAsCuboid(pos, candidates)) return candidates;
        List<TankBlockEntity> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparingInt((TankBlockEntity c) -> c.getTank().memberCount()).reversed());
        for (TankBlockEntity c : sorted) {
            if (fitsAsCuboid(pos, List.of(c))) return List.of(c);
        }
        return List.of();
    }

    private static boolean fitsAsCuboid(BlockPos extra, List<TankBlockEntity> tanks) {
        long count = 1;
        int minX = extra.getX(), minY = extra.getY(), minZ = extra.getZ();
        int maxX = minX, maxY = minY, maxZ = minZ;
        for (TankBlockEntity c : tanks) {
            TankData t = c.getTank();
            if (t.contains(extra)) return false;
            count += t.memberCount();
            BlockPos mn = t.min(), mx = t.max();
            minX = Math.min(minX, mn.getX()); minY = Math.min(minY, mn.getY()); minZ = Math.min(minZ, mn.getZ());
            maxX = Math.max(maxX, mx.getX()); maxY = Math.max(maxY, mx.getY()); maxZ = Math.max(maxZ, mx.getZ());
        }
        int sx = maxX - minX + 1, sy = maxY - minY + 1, sz = maxZ - minZ + 1;
        if (sx > ServerConfig.maxSize(0) || sy > ServerConfig.maxSize(1) || sz > ServerConfig.maxSize(2)) return false;
        // Tanks are disjoint, so the union is the box exactly when the counts match.
        return (long) sx * sy * sz == count;
    }

    private static void merge(ServerLevel level, List<TankBlockEntity> controllers, BlockPos extra) {
        List<TankData> olds = new ArrayList<>();
        List<BlockPos> all = new ArrayList<>();
        long water = 0;
        TankData main = null;
        for (TankBlockEntity c : controllers) {
            TankData t = c.getTank();
            olds.add(t);
            water += t.waterMb();
            for (long l : t.members()) all.add(BlockPos.of(l));
            if (main == null || t.memberCount() > main.memberCount()) main = t;
        }
        all.add(extra.immutable());
        TankData merged = TankData.create(all, main.scale());
        merged.setId(main.id());
        merged.setOwner(main.owner());
        merged.setWaterMb(water);
        InteriorManager.onTanksMerged(level, olds, merged);
        install(level, merged);
        LOGGER.debug("Merged {} tank(s) into {}", olds.size(), merged);
    }

    private static TankData newTank(List<BlockPos> blocks, int scale, @Nullable ServerPlayer owner) {
        TankData t = TankData.create(blocks, scale);
        t.setOwner(owner == null ? null : owner.getUUID());
        return t;
    }

    // ------------------------------------------------------------------ removing

    public static void onTankBlockRemoved(ServerLevel level, BlockPos pos, TankBlockEntity removed) {
        BlockPos cp = removed.getControllerPos();
        if (cp == null) return;
        TankData t;
        if (removed.isController()) {
            t = removed.getTank();
        } else {
            TankBlockEntity c = level.isLoaded(cp) ? getBE(level, cp) : null;
            t = c != null && c.isController() ? c.getTank() : null;
        }
        if (t == null || !t.contains(pos)) {
            LOGGER.warn("Tank block at {} removed but its controller {} is not available", pos, cp);
            return;
        }

        LongSet remaining = new LongOpenHashSet(t.members());
        remaining.remove(pos.asLong());
        long newWater = t.waterMb() - t.waterMb() / Math.max(1, t.memberCount());

        // Interior part of the removed block drops as items, the rest is copied into the pieces' regions.
        InteriorManager.dropInteriorOfBlock(level, t, pos);
        if (remaining.isEmpty()) {
            InteriorManager.freeRegion(level, t);
            return;
        }

        List<List<BlockPos>> pieces = decompose(remaining);
        List<TankData> newTanks = new ArrayList<>();
        long distributed = 0;
        for (int i = 0; i < pieces.size(); i++) {
            List<BlockPos> piece = pieces.get(i);
            TankData nt = TankData.create(piece, t.scale());
            nt.setOwner(t.owner());
            nt.setId(i == 0 ? t.id() : UUID.randomUUID());
            long share = i == pieces.size() - 1 ? newWater - distributed : newWater * piece.size() / remaining.size();
            distributed += share;
            nt.setWaterMb(share);
            newTanks.add(nt);
        }
        InteriorManager.onTankSplit(level, t, newTanks);
        for (TankData nt : newTanks) install(level, nt);

        if (newTanks.size() > 1) {
            for (ServerPlayer p : level.players()) {
                if (p.blockPosition().distSqr(pos) < 32 * 32) {
                    p.displayClientMessage(Component.translatable("message.bettacraft.tank.split", newTanks.size()), true);
                }
            }
        }
    }

    /**
     * Greedy decomposition of a block set into cuboids: take the lowest block (y, z, x order), grow along x, then z,
     * then y as long as the box stays inside the set. Deterministic; a cuboid stays one piece.
     */
    public static List<List<BlockPos>> decompose(LongSet input) {
        LongSet left = new LongOpenHashSet(input);
        List<List<BlockPos>> result = new ArrayList<>();
        List<BlockPos> sorted = new ArrayList<>();
        for (long l : input) sorted.add(BlockPos.of(l));
        sorted.sort(Comparator.comparingInt((BlockPos p) -> p.getY()).thenComparingInt(p -> p.getZ()).thenComparingInt(p -> p.getX()));
        int maxX = ServerConfig.maxSize(0), maxY = ServerConfig.maxSize(1), maxZ = ServerConfig.maxSize(2);
        for (BlockPos start : sorted) {
            if (!left.contains(start.asLong())) continue;
            int x0 = start.getX(), y0 = start.getY(), z0 = start.getZ();
            int x1 = x0, y1 = y0, z1 = z0;
            while (x1 - x0 + 1 < maxX && left.contains(BlockPos.asLong(x1 + 1, y0, z0))) x1++;
            while (z1 - z0 + 1 < maxZ && rowFree(left, x0, x1, y0, z1 + 1)) z1++;
            while (y1 - y0 + 1 < maxY && layerFree(left, x0, x1, z0, z1, y1 + 1)) y1++;
            List<BlockPos> box = new ArrayList<>();
            for (int y = y0; y <= y1; y++)
                for (int z = z0; z <= z1; z++)
                    for (int x = x0; x <= x1; x++) {
                        left.remove(BlockPos.asLong(x, y, z));
                        box.add(new BlockPos(x, y, z));
                    }
            result.add(box);
        }
        return result;
    }

    private static boolean rowFree(LongSet set, int x0, int x1, int y, int z) {
        for (int x = x0; x <= x1; x++) if (!set.contains(BlockPos.asLong(x, y, z))) return false;
        return true;
    }

    private static boolean layerFree(LongSet set, int x0, int x1, int z0, int z1, int y) {
        for (int z = z0; z <= z1; z++) if (!rowFree(set, x0, x1, y, z)) return false;
        return true;
    }

    // ------------------------------------------------------------------ installing / states

    /** Writes the tank into the world: controller BE, member BEs, connection and fill block states. */
    public static void install(ServerLevel level, TankData tank) {
        BlockPos controller = pickController(tank);
        for (long l : tank.members()) {
            BlockPos p = BlockPos.of(l);
            TankBlockEntity be = getBE(level, p);
            if (be == null) continue;
            be.setController(controller, p.equals(controller) ? tank : null);
        }
        applyStates(level, tank);
        TankBlockEntity c = getBE(level, controller);
        if (c != null) c.markTankChanged();
    }

    private static BlockPos pickController(TankData tank) {
        BlockPos best = null;
        for (long l : tank.members()) {
            BlockPos p = BlockPos.of(l);
            if (best == null || p.getY() < best.getY() || (p.getY() == best.getY() && (p.getZ() < best.getZ()
                    || (p.getZ() == best.getZ() && p.getX() < best.getX())))) {
                best = p;
            }
        }
        return best;
    }

    /** Updates connection flags and water fill of all member blocks (only changed states are sent). */
    public static void applyStates(ServerLevel level, TankData tank) {
        Int2IntOpenHashMap perLayer = new Int2IntOpenHashMap();
        for (long l : tank.members()) perLayer.addTo(BlockPos.getY(l), 1);
        Int2IntOpenHashMap fill = new Int2IntOpenHashMap();
        long below = 0;
        for (int y = tank.min().getY(); y <= tank.max().getY(); y++) {
            long layerCap = perLayer.get(y) * TankData.MB_PER_BLOCK;
            fill.put(y, TankData.fillFor(tank.waterMb(), below, layerCap));
            below += layerCap;
        }
        LongSet members = tank.members();
        for (long l : tank.members()) {
            BlockPos p = BlockPos.of(l);
            BlockState state = level.getBlockState(p);
            if (!(state.getBlock() instanceof TankBlock)) continue;
            BlockState ns = state.setValue(TankBlock.FILL, fill.get(p.getY()));
            for (Direction d : Direction.values()) {
                ns = ns.setValue(TankBlock.CONNECTION.get(d), members.contains(p.relative(d).asLong()));
            }
            if (ns != state) {
                level.setBlock(p, ns, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }
}
