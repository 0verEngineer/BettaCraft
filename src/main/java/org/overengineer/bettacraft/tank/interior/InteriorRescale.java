package org.overengineer.bettacraft.tank.interior;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * M30: the interior of a tank that grew past {@code bigTankRules.interiorScale8Above} (or {@code interiorScale4Above}) goes
 * into the new, coarser interior of the big tank. Nothing is lost:
 * <ul>
 *     <li>The decoration is <b>shrunk</b> by the factor of the scales (2 or 4): every cube of f x f x f old blocks becomes one
 *     block, the most common plain block in it (a block without a block entity) if at least a quarter of the cube was filled.
 *     So the layout stays, at half or a quarter of the size, the miniature looks the same.</li>
 *     <li>Every block that does not stay (the rest of each cube, blocks with a block entity such as heaters, filters and
 *     chests with their contents, the aquarium plants with their stage) comes out as items in front of the tank, packed
 *     into chests when there are many (like a broken tank block).</li>
 *     <li>The floor substrate is shrunk the same way (the most common one of each f x f patch); paid substrate beyond the
 *     new floor comes back as items.</li>
 * </ul>
 * Players inside the old interior are sent out first (it is rebuilt).
 */
public final class InteriorRescale {
    private InteriorRescale() {}

    /**
     * Shrinks {@code src} (finer scale) into {@code dst} at the local offset {@code dx, dy, dz} (in {@code dst} blocks),
     * adding what does not stay to {@code drops}. {@code floor}: whether the old floor is the new floor (the tank did not
     * grow below it); otherwise the paid substrate comes back as items.
     */
    public static void shrinkInto(ServerLevel level, InteriorRegion src, InteriorRegion dst, int dx, int dy, int dz, boolean floor,
                                  List<ItemStack> drops, LongSet touched) {
        int f = Math.max(1, src.scale() / Math.max(1, dst.scale()));
        int nx = src.sizeX() / f, ny = src.sizeY() / f, nz = src.sizeZ() / f;
        int x0 = src.originX(), z0 = src.originZ(), y0 = InteriorRegion.BASE_Y;
        // the aquarium plants come out whole (their blocks are skipped by InteriorDrops)
        for (var pl : new ArrayList<>(src.plants().all())) {
            src.plants().remove(pl);
            drops.add(org.overengineer.bettacraft.plants.InteriorPlants.item(pl.species, pl.stage));
        }
        // every non-air block of the old interior, by the cell of the new one it falls into
        List<BlockPos> all = new ArrayList<>();
        InteriorWorldOps.forEachBlock(level, x0, y0, z0, x0 + src.sizeX() - 1, y0 + src.sizeY() - 1, z0 + src.sizeZ() - 1, (x, y, z, s) -> {
            if (InteriorManager.isShellBlock(s)) return;
            all.add(new BlockPos(x, y, z));
        });
        List<InteriorWorldOps.Captured> place = new ArrayList<>();
        Map<Long, List<BlockPos>> byCell = new HashMap<>();
        for (BlockPos p : all) {
            int cx = (p.getX() - x0) / f, cy = (p.getY() - y0) / f, cz = (p.getZ() - z0) / f;
            byCell.computeIfAbsent(BlockPos.asLong(cx, cy, cz), k -> new ArrayList<>()).add(p);
        }
        int quarter = Math.max(1, f * f * f / 4);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (var e : byCell.entrySet()) {
            BlockPos c = BlockPos.of(e.getKey());
            if (c.getX() >= nx || c.getY() >= ny || c.getZ() >= nz) continue;
            List<ItemStack> items = new ArrayList<>();
            Map<BlockState, Integer> plain = new HashMap<>();
            for (BlockPos p : e.getValue()) {
                BlockState s = level.getBlockState(p);
                InteriorDrops.collect(level, p, s, items);
                if (!s.hasBlockEntity() && !(s.getBlock() instanceof org.overengineer.bettacraft.plants.AquaticPlantBlock)
                        && !(s.getBlock() instanceof org.overengineer.bettacraft.equipment.SegmentBlock)) {
                    plain.merge(s, 1, Integer::sum);
                }
            }
            BlockState best = null;
            int bestN = 0;
            for (var pe : plain.entrySet()) {
                if (pe.getValue() > bestN) {
                    best = pe.getKey();
                    bestN = pe.getValue();
                }
            }
            if (best != null && bestN >= quarter) {
                place.add(new InteriorWorldOps.Captured(c.getX(), c.getY(), c.getZ(), best, null));
                take(items, best.getBlock().asItem());
            }
            drops.addAll(items);
        }
        // the old blocks go (the region is destroyed after this anyway), the new ones are written
        InteriorWorldOps.paste(level, dst.toWorld(dx, dy, dz), place, touched);
        // the floor: the most common substrate of each patch, paid ones beyond the new floor as items
        for (int x = 0; x < nx; x++)
            for (int z = 0; z < nz; z++) {
                Map<BlockState, Integer> kinds = new HashMap<>();
                boolean paid = false;
                List<ItemStack> back = new ArrayList<>();
                for (int ax = 0; ax < f; ax++)
                    for (int az = 0; az < f; az++) {
                        m.set(x0 + x * f + ax, InteriorRegion.FLOOR_Y, z0 + z * f + az);
                        InteriorFloorBlockEntity be = Substrate.active(level, m);
                        if (be == null) continue;
                        kinds.merge(be.substrate(), 1, Integer::sum);
                        paid |= be.paid();
                        ItemStack b = Substrate.clear(level, m.immutable());
                        if (!b.isEmpty()) back.add(b);
                    }
                if (floor && !kinds.isEmpty()) {
                    BlockState sub = kinds.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
                    BlockPos to = new BlockPos(dst.originX() + dx + x, InteriorRegion.FLOOR_Y, dst.originZ() + dz + z);
                    if (dst.containsShell(to)) {
                        Substrate.set(level, to, sub, paid);
                        if (paid) take(back, sub.getBlock().asItem());
                    }
                }
                drops.addAll(back);
            }
    }

    /** Removes one item of this kind from the list (the block that stays in the new interior). */
    private static void take(List<ItemStack> items, Item item) {
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = items.get(i);
            if (s.is(item)) {
                s.shrink(1);
                if (s.isEmpty()) items.remove(i);
                return;
            }
        }
    }
}
