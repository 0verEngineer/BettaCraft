package org.overengineer.bettacraft.tank;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
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
import net.minecraft.resources.Identifier;
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
 *     <li>Only tank blocks with the <b>same glass</b> and the same {@link TankStyle} connect (M11).</li>
 *     <li>M10: any face-connected shape (L, T, steps ...) whose bounding box fits the size limit. A block that would
 *     exceed it stays a separate tank (hint in the action bar). Breaking a block splits the rest into its connected
 *     parts. With {@code tank.freeShapes = false} tanks stay cuboids (M1-M9 behaviour, greedy decomposition).</li>
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

        Map<BlockPos, TankBlockEntity> candidates = new LinkedHashMap<>();
        boolean unloadedNeighbour = false;
        boolean otherGlass = false;
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            if (!level.isLoaded(n)) continue;
            TankBlockEntity nbe = getBE(level, n);
            if (nbe == null) continue;
            if (!be.sameMaterial(nbe)) {
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
                    player.sendOverlayMessage(notJoinedMessage());
                } else if (unloadedNeighbour) {
                    player.sendOverlayMessage(Component.translatable("message.bettacraft.tank.unloaded"));
                } else if (otherGlass) {
                    player.sendOverlayMessage(Component.translatable("message.bettacraft.tank.other_glass"));
                }
            }
            return;
        }
        merge(level, chosen, pos);
        if (player != null && chosen.size() < candidates.size()) {
            player.sendOverlayMessage(notJoinedMessage());
        }
    }

    /**
     * M17: a tank block whose controller does not know it any more (the controller's data could not be loaded, or a block
     * was removed while the controller's chunk was unloaded) used to stay "not formed" forever. Once after loading, each
     * block checks that and forms a tank again with its neighbours (like a newly placed block). True if it repaired.
     */
    public static boolean repairOrphan(ServerLevel level, BlockPos pos, TankBlockEntity be) {
        if (org.overengineer.bettacraft.tank.interior.InteriorManager.isInterior(level) || be.isRemoved()) return false;
        BlockPos cp = be.getControllerPos();
        if (cp != null) {
            if (be.isController()) return false;
            if (!cp.equals(pos)) {
                if (!level.isLoaded(cp)) return false; // checked again on the next load
                TankBlockEntity c = getBE(level, cp);
                if (c != null && c.isController() && c.getTank().contains(pos)) return false;
            }
            LOGGER.warn("Tank block at {} pointed to controller {} that does not know it: forming it again", pos, cp);
            be.setController(null, null);
            be.setChanged();
        }
        onTankBlockAdded(level, pos, null);
        return be.getControllerPos() != null;
    }

    public static boolean freeShapes() {
        return ServerConfig.getOr(ServerConfig.TANK_FREE_SHAPES, true);
    }

    private static Component notJoinedMessage() {
        return freeShapes()
                ? Component.translatable("message.bettacraft.tank.too_large", ServerConfig.maxSize(0), ServerConfig.maxSize(1), ServerConfig.maxSize(2))
                : Component.translatable("message.bettacraft.tank.not_cuboid");
    }

    /**
     * Picks which neighbouring tanks the new block merges with: the largest subset (by block count) of the at most six
     * neighbouring tanks with one scale whose union with the block fits the size limit (and, with free shapes off, is a
     * cuboid). Every neighbour touches the new block, so the union is always connected.
     */
    private static List<TankBlockEntity> chooseMerge(BlockPos pos, List<TankBlockEntity> candidates) {
        int n = candidates.size();
        List<TankBlockEntity> best = List.of();
        long bestCount = -1;
        for (int mask = 1; mask < (1 << n); mask++) {
            List<TankBlockEntity> subset = new ArrayList<>();
            long count = 0;
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    subset.add(candidates.get(i));
                    count += candidates.get(i).getTank().memberCount();
                }
            }
            if (count <= bestCount) continue;
            int scale = subset.getFirst().getTank().scale();
            // M30: with big tanks, tanks of different scales join (the finer interiors are shrunk, InteriorRescale)
            if (!Features.enabled(Feature.BIG_TANKS) && subset.stream().anyMatch(c -> c.getTank().scale() != scale)) continue;
            if (freeShapes() ? fitsAsShape(pos, subset) : fitsAsCuboid(pos, subset)) {
                best = subset;
                bestCount = count;
            }
        }
        return best;
    }

    private static boolean fitsAsShape(BlockPos extra, List<TankBlockEntity> tanks) {
        int minX = extra.getX(), minY = extra.getY(), minZ = extra.getZ();
        int maxX = minX, maxY = minY, maxZ = minZ;
        for (TankBlockEntity c : tanks) {
            TankData t = c.getTank();
            if (t.contains(extra)) return false;
            BlockPos mn = t.min(), mx = t.max();
            minX = Math.min(minX, mn.getX()); minY = Math.min(minY, mn.getY()); minZ = Math.min(minZ, mn.getZ());
            maxX = Math.max(maxX, mx.getX()); maxY = Math.max(maxY, mx.getY()); maxZ = Math.max(maxZ, mx.getZ());
        }
        return TankShape.fits(new int[]{minX, minY, minZ, maxX, maxY, maxZ}, ServerConfig.maxSize(0), ServerConfig.maxSize(1), ServerConfig.maxSize(2));
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
        long water = 0, power = 0;
        double waste = 0, salt = 0, calcium = 0, tds = 0, hplus = 0, tannin = 0, kh = 0, mg = 0, trace = 0;
        TankData main = null;
        for (TankBlockEntity c : controllers) {
            TankData t = c.getTank();
            olds.add(t);
            water += t.waterMb();
            waste += (1 - t.waterQuality()) * t.waterMb();
            salt += t.salinity() * (double) t.waterMb();
            calcium += t.calcium() * (double) t.waterMb();
            tds += t.tds() * (double) t.waterMb(); // M25
            hplus += Math.pow(10, -t.ph()) * (double) t.waterMb();
            tannin += t.tannin() * (double) t.waterMb();
            kh += t.kh() * (double) t.waterMb(); // M25 part c
            mg += t.magnesium() * (double) t.waterMb();
            trace += t.trace() * (double) t.waterMb();
            power += t.powerBuffer();
            for (long l : t.members()) all.add(BlockPos.of(l));
            if (main == null || t.memberCount() > main.memberCount()) main = t;
        }
        all.add(extra.immutable());
        TankData merged = TankData.create(all, mergedScale(olds, all, main.scale()));
        merged.setId(main.id());
        merged.copySettingsFrom(main);
        // M17: the nest of a smaller tank (eggs, larvae) used to vanish when it was joined to a bigger one. Keep the
        // main tank's nest, otherwise the other tanks' nest with eggs first (only one nest fits in a tank).
        if (merged.nest() == null || !merged.nest().hasEggs()) {
            for (TankData t : olds) {
                if (t == main || t.nest() == null) continue;
                if (merged.nest() == null || (t.nest().hasEggs() && !merged.nest().hasEggs())) merged.setNest(t.nest().copy());
            }
        }
        merged.setWaterMb(water);
        // M9: the water mixes, the stored FE adds up
        merged.setWaterQuality(water <= 0 ? main.waterQuality() : (float) (1 - waste / water));
        // M20: salt and calcium mix by volume too
        merged.setSalinity(water <= 0 ? main.salinity() : (float) (salt / water));
        merged.setCalcium(water <= 0 ? main.calcium() : (float) (calcium / water));
        if (water > 0) { // M25: TDS and tannins by volume, the pH as the acid (logarithmic)
            merged.setTds((float) (tds / water));
            merged.setPh((float) -Math.log10(hplus / water));
            merged.setTannin((float) (tannin / water));
            merged.setKh((float) (kh / water)); // M25 part c
            merged.setMagnesium((float) (mg / water));
            merged.setTrace((float) (trace / water));
        } else {
            merged.setKh(main.kh());
            merged.setMagnesium(main.magnesium());
            merged.setTrace(main.trace());
        }
        merged.setPowerBuffer(Math.min(power, TankPower.capacity()));
        // equipment of all merged tanks moves into the new one; what does not fit drops
        for (TankData t : olds) {
            merged.inhabitants().addAll(t.inhabitants());
            moveHeaters(level, t, merged, extra);
            // M12: the floating plants of the other tanks join (new ids: the ids of two tanks can clash)
            if (t != main) for (var p : t.plants()) {
                var q = new org.overengineer.bettacraft.tank.plants.FloatingPlant(merged.nextPlantId(), p.type, p.x, p.z, p.size, p.rot);
                q.red = p.red;
                merged.plants().add(q);
            }
            // M14: the small animals, clutches, moults, food and algae of the other tanks join too
            if (t != main) {
                for (var k : t.critters()) merged.critters().add(k.copy(merged.nextCritterId()));
                for (var o : t.critterObjects()) merged.critterObjects().add(o.copy(merged.nextCritterId()));
                merged.setCritterFood(merged.critterFood() + t.critterFood());
                for (var e : t.algae().long2ObjectEntrySet()) merged.algae().put(e.getLongKey(), e.getValue().clone());
                // M47 part c: the frog spawn and the tadpoles too
                for (var s : t.frogSpawn()) {
                    var n = new org.overengineer.bettacraft.entity.frog.FrogSpawn(merged.nextFrogBroodId(), s.x, s.y, s.z, s.count);
                    n.age = s.age;
                    merged.frogSpawn().add(n);
                }
                for (var k : t.tadpoles()) {
                    var n = new org.overengineer.bettacraft.entity.frog.Tadpole(merged.nextFrogBroodId(), k.x, k.y, k.z);
                    n.age = k.age;
                    n.hunger = k.hunger;
                    merged.tadpoles().add(n);
                }
                merged.setTadpoleFood(merged.tadpoleFood() + t.tadpoleFood());
                // M48 part b: the plankton of the other tanks
                merged.setCopepods(merged.copepods() + t.copepods());
                merged.setRotifers(merged.rotifers() + t.rotifers());
                merged.setPhytoplankton(merged.phytoplankton() + t.phytoplankton());
                merged.setNauplii(merged.nauplii() + t.nauplii());
            }
        }
        InteriorManager.onTanksMerged(level, olds, merged);
        install(level, merged);
        LOGGER.debug("Merged {} tank(s) into {}", olds.size(), merged);
    }

    /**
     * M30: the interior scale of a joined tank: the finest scale of the parts, smaller (8, 4) once the tank is longer than
     * {@code bigTankRules.interiorScale8Above} / {@code interiorScale4Above} blocks (features.bigTanks.enabled; off: the main tank's).
     */
    static int mergedScale(List<TankData> olds, List<BlockPos> blocks, int mainScale) {
        if (!Features.enabled(Feature.BIG_TANKS)) return mainScale;
        int scale = mainScale;
        for (TankData t : olds) scale = Math.min(scale, t.scale());
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : blocks) {
            minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX()); maxY = Math.max(maxY, p.getY()); maxZ = Math.max(maxZ, p.getZ());
        }
        return BigTankRules.scaleFor(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1, scale,
                ServerConfig.getOr(ServerConfig.INTERIOR_SCALE8_ABOVE, 16), ServerConfig.getOr(ServerConfig.INTERIOR_SCALE4_ABOVE, 32));
    }

    private static void moveHeaters(ServerLevel level, TankData from, TankData to, BlockPos dropAt) {
        var src = from.legacyHeaters();
        var dst = to.legacyHeaters();
        for (int i = 0; i < src.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack s = src.removeItemNoUpdate(i);
            if (s.isEmpty()) continue;
            boolean placed = false;
            for (int k = 0; k < dst.getContainerSize() && !placed; k++) {
                if (dst.getItem(k).isEmpty()) {
                    dst.setItem(k, s);
                    placed = true;
                }
            }
            if (!placed) net.minecraft.world.Containers.dropItemStack(level, dropAt.getX() + 0.5, dropAt.getY() + 0.5, dropAt.getZ() + 0.5, s);
        }
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
        removeBlocks(level, t, List.of(pos));
    }

    /**
     * Takes blocks out of a tank (one broken block; M32 part b: the blocks of a tank that did not come along on a move):
     * their interior drops as items, the rest of the tank is installed again as its connected pieces (first = largest
     * part keeps id, equipment, power and the inhabitant list). Drops and messages at the first removed block.
     */
    public static void removeBlocks(ServerLevel level, TankData t, List<BlockPos> removedBlocks) {
        BlockPos pos = removedBlocks.getFirst();
        LongSet remaining = new LongOpenHashSet(t.members());
        for (BlockPos q : removedBlocks) remaining.remove(q.asLong());

        // Interior part of the removed block drops as items, the rest is copied into the pieces' regions.
        for (BlockPos q : removedBlocks) InteriorManager.dropInteriorOfBlock(level, t, q);
        if (remaining.isEmpty()) {
            InteriorManager.freeRegion(level, t);
            net.minecraft.world.Containers.dropContents(level, pos, t.legacyHeaters());
            org.overengineer.bettacraft.tank.plants.TankPlants.dropAll(level, t.plants(), pos); // M12
            return;
        }

        List<List<BlockPos>> pieces = pieces(remaining);
        // M10: every piece keeps the water of its own blocks (per layer); the removed block's water is gone
        List<List<Long>> asLongs = new ArrayList<>();
        for (List<BlockPos> piece : pieces) asLongs.add(piece.stream().map(BlockPos::asLong).toList());
        long[] shares = TankShape.splitWater(t.members(), t.waterMb(), asLongs);
        List<TankData> newTanks = new ArrayList<>();
        for (int i = 0; i < pieces.size(); i++) {
            List<BlockPos> piece = pieces.get(i);
            TankData nt = TankData.create(piece, t.scale());
            nt.copySettingsFrom(t);
            nt.setId(i == 0 ? t.id() : UUID.randomUUID());
            if (i == 0) {
                nt.setPowerBuffer(t.powerBuffer()); // M9: the stored FE stays with the first piece
                moveHeaters(level, t, nt, pos); // the first piece keeps the equipment
                nt.inhabitants().addAll(t.inhabitants());
            }
            nt.setWaterMb(shares[i]);
            newTanks.add(nt);
        }
        // M12: a plant never ends up in two pieces (pieces can share a column at different heights)
        java.util.Set<Integer> placed = new java.util.HashSet<>();
        for (TankData nt : newTanks) nt.plants().removeIf(p -> !placed.add(p.id));
        InteriorManager.onTankSplit(level, t, newTanks);
        for (TankData nt : newTanks) install(level, nt);
        // M12: plants above the removed block (no piece has that column any more) drop
        java.util.List<org.overengineer.bettacraft.tank.plants.FloatingPlant> lost = new ArrayList<>();
        for (var p : t.plants()) {
            boolean kept = false;
            for (TankData nt : newTanks) kept |= nt.containsColumn(p.x, p.z);
            if (!kept) lost.add(p);
        }
        org.overengineer.bettacraft.tank.plants.TankPlants.dropAll(level, lost, pos);
        // M14: small animals in the removed block (a forced break) move into the largest piece
        java.util.Set<Integer> kept = new java.util.HashSet<>();
        for (TankData nt : newTanks) for (var k : nt.critters()) kept.add(k.id);
        TankData first = newTanks.getFirst();
        for (TankData nt : newTanks) if (nt.memberCount() > first.memberCount()) first = nt; // M17: really the largest piece
        for (var k : t.critters()) {
            if (kept.contains(k.id)) continue;
            var moved = k.copy(first.nextCritterId());
            BlockPos a = first.anchor();
            moved.x = a.getX() + 0.5;
            moved.y = a.getY() + 0.05;
            moved.z = a.getZ() + 0.5;
            moved.tx = moved.x;
            moved.ty = moved.y;
            moved.tz = moved.z;
            moved.face = org.overengineer.bettacraft.critters.Critter.FLOOR;
            first.critters().add(moved);
        }

        if (newTanks.size() > 1) {
            for (ServerPlayer p : level.players()) {
                if (p.blockPosition().distSqr(pos) < 32 * 32) {
                    p.sendOverlayMessage(Component.translatable("message.bettacraft.tank.split", newTanks.size()));
                }
            }
        }
    }

    /**
     * The tanks the remaining blocks form: their connected parts (M10), each split into cuboids when free shapes are off.
     * The largest part comes first (it keeps id, equipment, power and the inhabitant list).
     */
    public static List<List<BlockPos>> pieces(LongSet remaining) {
        List<List<BlockPos>> out = new ArrayList<>();
        for (LongSet comp : TankShape.components(remaining)) {
            if (freeShapes()) {
                List<BlockPos> l = new ArrayList<>();
                for (long p : comp) l.add(BlockPos.of(p));
                l.sort(Comparator.comparingInt((BlockPos p) -> p.getY()).thenComparingInt(p -> p.getZ()).thenComparingInt(p -> p.getX()));
                out.add(l);
            } else {
                out.addAll(decompose(comp));
            }
        }
        // stable: equal sizes keep the deterministic component order
        List<List<BlockPos>> sorted = new ArrayList<>(out);
        sorted.sort(Comparator.comparingInt((List<BlockPos> l) -> -l.size()));
        return sorted;
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

    /**
     * M30: forms one tank of these placed tank blocks at once (GameTests, smoke scenes, the showcase: a 32 x 8 x 16 tank
     * block by block merges four thousand times), with the interior scale of its size like a grown tank.
     */
    public static TankData formAll(ServerLevel level, List<BlockPos> blocks) {
        TankData t = TankData.create(blocks, mergedScale(List.of(), blocks, ServerConfig.defaultScale()));
        install(level, t);
        return t;
    }

    /** Writes the tank into the world: controller BE, member BEs, connection and fill block states. */
    public static void install(ServerLevel level, TankData tank) {
        install(level, tank, it.unimi.dsi.fastutil.longs.LongSets.EMPTY_SET);
    }

    /** As above, leaving the block entities at {@code skip} alone (M32 part c: blocks of the tank that did not arrive). */
    public static void install(ServerLevel level, TankData tank, it.unimi.dsi.fastutil.longs.LongSet skip) {
        BlockPos controller = pickController(tank);
        for (long l : tank.members()) {
            if (skip.contains(l)) continue;
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
        int topFill = 16 - TankData.headroom();
        for (int y = tank.min().getY(); y <= tank.max().getY(); y++) {
            long layerCap = perLayer.get(y) * TankData.MB_PER_BLOCK;
            // top layer: air gap below the top glass (looks less full, same water)
            fill.put(y, TankData.fillFor(tank.waterMb(), below, layerCap, y == tank.max().getY() ? topFill : 16));
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
