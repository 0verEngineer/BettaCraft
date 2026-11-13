package org.overengineer.bettacraft.tank.interior;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.equipment.SegmentBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * What stands in an interior region apart from decoration (PLAN 7.3): heaters and coolers (M9) (anchor position +
 * capacity), internal filters, floating plants and the number of substrate floor blocks. Positions are interior-local
 * ({@link BlockPos#asLong} of local x/y/z). Kept up to date from block updates; after bulk copies (merge/split) the
 * region is rescanned lazily.
 */
public final class InteriorEquipment {
    /** Anchor of each heater -> capacity in water blocks. */
    final Long2IntOpenHashMap heaters = new Long2IntOpenHashMap();
    /** Anchor of each cooler (M9) -> capacity in water blocks. */
    final Long2IntOpenHashMap coolers = new Long2IntOpenHashMap();
    final LongOpenHashSet filters = new LongOpenHashSet();
    final LongOpenHashSet floating = new LongOpenHashSet();
    int substrate;
    boolean needsScan;
    /** M19: blocks of the spawning caves and of the spawning mops (the tankmates find caves and mops from them). */
    final LongOpenHashSet caveBlocks = new LongOpenHashSet();
    final LongOpenHashSet mopBlocks = new LongOpenHashSet();
    /** Bumped whenever a cave or mop block changes (cached cave/mop clusters). */
    int spawningVersion;
    /** M20: anchors of the flow pumps and the number of live rock blocks (biological filter in salt water). */
    final LongOpenHashSet pumps = new LongOpenHashSet();
    int liveRock;

    public Long2IntOpenHashMap heaters() { return heaters; }
    public Long2IntOpenHashMap coolers() { return coolers; }
    public LongOpenHashSet filters() { return filters; }
    public LongOpenHashSet floatingPlants() { return floating; }
    public int substrateCount() { return substrate; }
    public LongOpenHashSet caveBlocks() { return caveBlocks; }
    public LongOpenHashSet mopBlocks() { return mopBlocks; }
    public int spawningVersion() { return spawningVersion; }
    public LongOpenHashSet pumps() { return pumps; }
    public int liveRock() { return liveRock; }

    /** M20: flow pumps whose anchor is below the water surface. */
    public int pumpsBelow(double localWaterY) {
        int n = 0;
        for (long l : pumps) if (BlockPos.getY(l) < localWaterY) n++;
        return n;
    }
    public boolean needsScan() { return needsScan; }
    public void markNeedsScan() { needsScan = true; }

    /** Applies one block change at interior-local coordinates. */
    void onChange(int lx, int ly, int lz, BlockState oldState, BlockState newState) {
        long key = BlockPos.asLong(lx, ly, lz);
        remove(key, oldState);
        add(key, newState);
    }

    private void remove(long key, BlockState s) {
        if (isHeaterAnchor(s)) heaters.remove(key);
        else if (isCoolerAnchor(s)) coolers.remove(key);
        else if (isFilterAnchor(s)) filters.remove(key);
        else if (isPumpAnchor(s)) pumps.remove(key);
        else if (s.is(ModBlocks.LIVE_ROCK.get())) liveRock = Math.max(0, liveRock - 1);
        else if (s.is(ModTags.FLOATING_PLANTS)) floating.remove(key);
        else if (s.is(ModBlocks.SPAWNING_CAVE.get())) {
            caveBlocks.remove(key);
            spawningVersion++;
        } else if (s.is(ModBlocks.SPAWNING_MOP.get())) {
            mopBlocks.remove(key);
            spawningVersion++;
        }
    }

    private void add(long key, BlockState s) {
        if (isHeaterAnchor(s)) heaters.put(key, s.getValue(SegmentBlock.SIZE).capacity);
        else if (isCoolerAnchor(s)) coolers.put(key, s.getValue(SegmentBlock.SIZE).capacity);
        else if (isFilterAnchor(s)) filters.add(key);
        else if (isPumpAnchor(s)) pumps.add(key);
        else if (s.is(ModBlocks.LIVE_ROCK.get())) liveRock++;
        else if (s.is(ModTags.FLOATING_PLANTS)) floating.add(key);
        else if (s.is(ModBlocks.SPAWNING_CAVE.get())) {
            caveBlocks.add(key);
            spawningVersion++;
        } else if (s.is(ModBlocks.SPAWNING_MOP.get())) {
            mopBlocks.add(key);
            spawningVersion++;
        }
    }

    static boolean isHeaterAnchor(BlockState s) {
        return s.is(ModBlocks.HEATER_SEGMENT.get()) && s.getValue(SegmentBlock.PART) == SegmentBlock.Part.ANCHOR;
    }

    static boolean isCoolerAnchor(BlockState s) {
        return s.is(ModBlocks.COOLER_SEGMENT.get()) && s.getValue(SegmentBlock.PART) == SegmentBlock.Part.ANCHOR;
    }

    static boolean isFilterAnchor(BlockState s) {
        return s.is(ModBlocks.FILTER_SEGMENT.get()) && s.getValue(SegmentBlock.PART) == SegmentBlock.Part.ANCHOR;
    }

    static boolean isPumpAnchor(BlockState s) {
        return s.is(ModBlocks.PUMP_SEGMENT.get()) && s.getValue(SegmentBlock.PART) == SegmentBlock.Part.ANCHOR;
    }

    /** Full rescan of the region (interior volume and floor). */
    public void rescan(ServerLevel interior, InteriorRegion r) {
        heaters.clear();
        coolers.clear();
        filters.clear();
        floating.clear();
        caveBlocks.clear();
        mopBlocks.clear();
        pumps.clear();
        liveRock = 0;
        spawningVersion++;
        substrate = 0;
        needsScan = false;
        if (!r.built()) return;
        int x0 = r.originX(), z0 = r.originZ();
        InteriorWorldOps.forEachBlock(interior, x0, InteriorRegion.BASE_Y, z0, x0 + r.sizeX() - 1, InteriorRegion.BASE_Y + r.sizeY() - 1,
                z0 + r.sizeZ() - 1, (x, y, z, s) -> add(BlockPos.asLong(x - x0, y - InteriorRegion.BASE_Y, z - z0), s));
        InteriorWorldOps.forEachBlock(interior, x0, InteriorRegion.FLOOR_Y, z0, x0 + r.sizeX() - 1, InteriorRegion.FLOOR_Y, z0 + r.sizeZ() - 1,
                (x, y, z, s) -> {
                    if (s.is(ModBlocks.INTERIOR_FLOOR.get()) && s.getValue(InteriorFloorBlock.SUBSTRATE)) substrate++;
                });
    }

    /** Sum of heater capacities whose anchor is below the given interior-local water height. */
    public int heaterCapacityBelow(double localWaterY) {
        return capacityBelow(heaters, localWaterY);
    }

    /** Sum of cooler capacities whose anchor is below the given interior-local water height. */
    public int coolerCapacityBelow(double localWaterY) {
        return capacityBelow(coolers, localWaterY);
    }

    private static int capacityBelow(Long2IntOpenHashMap map, double localWaterY) {
        int sum = 0;
        for (var e : map.long2IntEntrySet()) {
            if (BlockPos.getY(e.getLongKey()) < localWaterY) sum += e.getIntValue();
        }
        return sum;
    }

    /** Internal filters whose anchor is below the water surface (they clean the water, M9). */
    public int filtersBelow(double localWaterY) {
        int n = 0;
        for (long l : filters) if (BlockPos.getY(l) < localWaterY) n++;
        return n;
    }

    CompoundTag save() {
        CompoundTag t = new CompoundTag();
        long[] hk = new long[heaters.size()];
        int[] hv = new int[heaters.size()];
        int i = 0;
        for (var e : heaters.long2IntEntrySet()) {
            hk[i] = e.getLongKey();
            hv[i++] = e.getIntValue();
        }
        t.putLongArray("heaters", hk);
        t.putIntArray("heater_capacity", hv);
        long[] ck = new long[coolers.size()];
        int[] cv = new int[coolers.size()];
        i = 0;
        for (var e : coolers.long2IntEntrySet()) {
            ck[i] = e.getLongKey();
            cv[i++] = e.getIntValue();
        }
        t.putLongArray("coolers", ck);
        t.putIntArray("cooler_capacity", cv);
        t.putLongArray("filters", filters.toLongArray());
        t.putLongArray("floating", floating.toLongArray());
        t.putInt("substrate", substrate);
        t.putLongArray("caves", caveBlocks.toLongArray()); // M19
        t.putLongArray("mops", mopBlocks.toLongArray());
        t.putLongArray("pumps", pumps.toLongArray()); // M20
        t.putInt("live_rock", liveRock);
        if (needsScan) t.putBoolean("rescan", true);
        return t;
    }

    void load(CompoundTag t) {
        heaters.clear();
        coolers.clear();
        filters.clear();
        floating.clear();
        long[] hk = t.getLongArray("heaters");
        int[] hv = t.getIntArray("heater_capacity");
        for (int i = 0; i < hk.length; i++) heaters.put(hk[i], i < hv.length ? hv[i] : 16);
        long[] ck = t.getLongArray("coolers"); // M9; absent in older regions (there were no coolers)
        int[] cv = t.getIntArray("cooler_capacity");
        for (int i = 0; i < ck.length; i++) coolers.put(ck[i], i < cv.length ? cv[i] : 16);
        for (long l : t.getLongArray("filters")) filters.add(l);
        for (long l : t.getLongArray("floating")) floating.add(l);
        caveBlocks.clear();
        mopBlocks.clear();
        for (long l : t.getLongArray("caves")) caveBlocks.add(l);
        for (long l : t.getLongArray("mops")) mopBlocks.add(l);
        pumps.clear();
        for (long l : t.getLongArray("pumps")) pumps.add(l);
        liveRock = Math.max(0, t.getInt("live_rock"));
        spawningVersion++;
        substrate = t.getInt("substrate");
        needsScan = t.getBoolean("rescan");
    }
}
