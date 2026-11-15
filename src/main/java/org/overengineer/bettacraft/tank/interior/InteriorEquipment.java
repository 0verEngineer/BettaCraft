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
    /** M29: blocks of the small caves for the cave brooding cichlids ({@code #bettacraft:small_caves}). */
    final LongOpenHashSet smallCaveBlocks = new LongOpenHashSet();
    /** Bumped whenever a cave or mop block changes (cached cave/mop clusters). */
    int spawningVersion;
    /** M20: anchors of the flow pumps and the number of live rock blocks (biological filter in salt water). */
    final LongOpenHashSet pumps = new LongOpenHashSet();
    int liveRock;
    /** M23: corner filters, sponge filters and air stones (anchors / positions), the regulator of every device (absent = 100 %) and the motor filters with an intake sponge. */
    final LongOpenHashSet corners = new LongOpenHashSet();
    final LongOpenHashSet spongeFilters = new LongOpenHashSet();
    final LongOpenHashSet airStones = new LongOpenHashSet();
    final Long2IntOpenHashMap power = new Long2IntOpenHashMap();
    final LongOpenHashSet intakeSponges = new LongOpenHashSet();
    /** M24: plumbing pieces (bulkhead connection, pipes, outlets) -> {@code Plumb} code; bumped version on every change but the outlets' activity. */
    final Long2IntOpenHashMap plumbing = new Long2IntOpenHashMap();
    int plumbingVersion;
    /**
     * M25 water chemistry: floor blocks (all, with soil, with limestone), limestone rocks and driftwood blocks in the
     * volume. Absent in regions of earlier versions: they are counted by a rescan. Part d: floor blocks with active soil.
     */
    int floorBlocks, acidFloor, limeFloor, limeRocks, driftwood, activeSoil;
    /** M28: almond leaves and alder cones on the floor, shelter blocks (clay and terracotta) for the young shrimp. */
    int leafLitter, alderCones, shelter;

    public Long2IntOpenHashMap heaters() { return heaters; }
    public Long2IntOpenHashMap coolers() { return coolers; }
    public LongOpenHashSet filters() { return filters; }
    public LongOpenHashSet floatingPlants() { return floating; }
    public int substrateCount() { return substrate; }
    public LongOpenHashSet caveBlocks() { return caveBlocks; }
    public LongOpenHashSet mopBlocks() { return mopBlocks; }
    public LongOpenHashSet smallCaveBlocks() { return smallCaveBlocks; }
    public int spawningVersion() { return spawningVersion; }
    public LongOpenHashSet pumps() { return pumps; }
    public int liveRock() { return liveRock; }
    public LongOpenHashSet corners() { return corners; }
    public LongOpenHashSet spongeFilters() { return spongeFilters; }
    public LongOpenHashSet airStones() { return airStones; }
    public Long2IntOpenHashMap plumbing() { return plumbing; }
    // M25
    public int floorBlocks() { return floorBlocks; }
    public int acidFloor() { return acidFloor; }
    public int limeFloor() { return limeFloor; }
    public int limeRocks() { return limeRocks; }
    public int driftwood() { return driftwood; }
    public int activeSoil() { return activeSoil; }
    public int leafLitter() { return leafLitter; }
    public int alderCones() { return alderCones; }
    public int shelter() { return shelter; }

    /** M25: a floor block's substrate changed ({@code null} = none): the soil / limestone counts follow. */
    public void onSubstrate(@javax.annotation.Nullable BlockState old, @javax.annotation.Nullable BlockState neu) {
        if (old != null) {
            if (old.is(ModTags.ACTIVE_SOIL)) activeSoil = Math.max(0, activeSoil - 1); // M25 part d
            else if (old.is(ModTags.ACID_SUBSTRATE)) acidFloor = Math.max(0, acidFloor - 1);
            else if (old.is(ModTags.LIMESTONE)) limeFloor = Math.max(0, limeFloor - 1);
        }
        if (neu != null) {
            if (neu.is(ModTags.ACTIVE_SOIL)) activeSoil++;
            else if (neu.is(ModTags.ACID_SUBSTRATE)) acidFloor++;
            else if (neu.is(ModTags.LIMESTONE)) limeFloor++;
        }
    }
    public int plumbingVersion() { return plumbingVersion; }

    /** M23: regulator 0..10 of the device at this local position. */
    public int powerOf(long key) {
        return power.getOrDefault(key, 10);
    }

    /** M23: regulator share 0..1 (1 with features.powerRegulator off). */
    public double shareOf(long key) {
        return org.overengineer.bettacraft.equipment.Regulator.share(powerOf(key));
    }

    public boolean hasIntakeSponge(long key) {
        return intakeSponges.contains(key);
    }

    /** M23: sum of the regulator shares of the devices in {@code set} below the water surface. */
    public double shareBelow(LongOpenHashSet set, double localWaterY) {
        double n = 0;
        for (long l : set) if (BlockPos.getY(l) < localWaterY) n += shareOf(l);
        return n;
    }

    /** M23: running motor filters (internal and corner) below the water without an intake sponge: sum of their shares. */
    public double unprotectedMotorShare(double localWaterY) {
        double n = 0;
        for (LongOpenHashSet set : java.util.List.of(filters, corners)) {
            for (long l : set) if (BlockPos.getY(l) < localWaterY && !intakeSponges.contains(l)) n += shareOf(l);
        }
        return n;
    }

    /** M20: flow pumps whose anchor is below the water surface. */
    public int pumpsBelow(double localWaterY) {
        int n = 0;
        for (long l : pumps) if (BlockPos.getY(l) < localWaterY) n++;
        return n;
    }
    public boolean needsScan() { return needsScan; }
    public void markNeedsScan() { needsScan = true; }

    private void clearM23() {
        corners.clear();
        spongeFilters.clear();
        airStones.clear();
        power.clear();
        intakeSponges.clear();
    }

    /** Applies one block change at interior-local coordinates. */
    void onChange(int lx, int ly, int lz, BlockState oldState, BlockState newState) {
        long key = BlockPos.asLong(lx, ly, lz);
        // M24: plumbing (an outlet that only switches its activity changes nothing here)
        int oc = org.overengineer.bettacraft.plumbing.interior.PlumbingBlock.code(oldState), nc = org.overengineer.bettacraft.plumbing.interior.PlumbingBlock.code(newState);
        if (oc != nc) {
            if (nc != 0) plumbing.put(key, nc);
            else plumbing.remove(key);
            plumbingVersion++;
        }
        remove(key, oldState);
        add(key, newState);
    }

    private void remove(long key, BlockState s) {
        if (s.is(ModTags.LIMESTONE)) limeRocks = Math.max(0, limeRocks - 1); // M25
        else if (s.is(ModTags.DRIFTWOOD)) driftwood = Math.max(0, driftwood - 1);
        if (s.is(ModBlocks.ALMOND_LEAF_LITTER.get())) leafLitter = Math.max(0, leafLitter - 1); // M28
        else if (s.is(ModBlocks.ALDER_CONES.get())) alderCones = Math.max(0, alderCones - 1);
        if (s.is(ModTags.SHRIMP_SHELTER)) shelter = Math.max(0, shelter - 1);
        if (s.hasProperty(org.overengineer.bettacraft.equipment.DeviceSegmentBlock.POWER)) {
            power.remove(key);
            intakeSponges.remove(key);
        }
        if (s.is(ModBlocks.AIR_STONE.get())) airStones.remove(key);
        else if (isAnchor(s, ModBlocks.CORNER_FILTER_SEGMENT.get())) corners.remove(key);
        else if (isAnchor(s, ModBlocks.SPONGE_FILTER_SEGMENT.get())) spongeFilters.remove(key);
        else if (isHeaterAnchor(s)) heaters.remove(key);
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
        if (s.is(ModTags.SMALL_CAVES) && smallCaveBlocks.remove(key)) spawningVersion++; // M29
    }

    private void add(long key, BlockState s) {
        if (s.is(ModTags.LIMESTONE)) limeRocks++; // M25
        else if (s.is(ModTags.DRIFTWOOD)) driftwood++;
        if (s.is(ModBlocks.ALMOND_LEAF_LITTER.get())) leafLitter++; // M28
        else if (s.is(ModBlocks.ALDER_CONES.get())) alderCones++;
        if (s.is(ModTags.SHRIMP_SHELTER)) shelter++;
        boolean device = s.is(ModBlocks.AIR_STONE.get()) || s.hasProperty(org.overengineer.bettacraft.equipment.DeviceSegmentBlock.POWER)
                && s.getValue(SegmentBlock.PART) == SegmentBlock.Part.ANCHOR;
        if (device) {
            int pw = s.getValue(org.overengineer.bettacraft.equipment.DeviceSegmentBlock.POWER);
            if (pw != 10) power.put(key, pw);
            if (s.hasProperty(org.overengineer.bettacraft.equipment.DeviceSegmentBlock.SPONGE) && s.getValue(org.overengineer.bettacraft.equipment.DeviceSegmentBlock.SPONGE)) intakeSponges.add(key);
        }
        if (s.is(ModBlocks.AIR_STONE.get())) airStones.add(key);
        else if (isAnchor(s, ModBlocks.CORNER_FILTER_SEGMENT.get())) corners.add(key);
        else if (isAnchor(s, ModBlocks.SPONGE_FILTER_SEGMENT.get())) spongeFilters.add(key);
        else if (isHeaterAnchor(s)) heaters.put(key, s.getValue(SegmentBlock.SIZE).capacity);
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
        if (s.is(ModTags.SMALL_CAVES) && smallCaveBlocks.add(key)) spawningVersion++; // M29
    }

    static boolean isAnchor(BlockState s, net.minecraft.world.level.block.Block b) {
        return s.is(b) && s.getValue(SegmentBlock.PART) == SegmentBlock.Part.ANCHOR;
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
        smallCaveBlocks.clear();
        pumps.clear();
        clearM23();
        plumbing.clear();
        plumbingVersion++;
        liveRock = 0;
        spawningVersion++;
        substrate = 0;
        floorBlocks = acidFloor = limeFloor = limeRocks = driftwood = activeSoil = 0;
        leafLitter = alderCones = shelter = 0;
        needsScan = false;
        if (!r.built()) return;
        int x0 = r.originX(), z0 = r.originZ();
        it.unimi.dsi.fastutil.longs.LongArrayList strayWater = new it.unimi.dsi.fastutil.longs.LongArrayList();
        InteriorWorldOps.forEachBlock(interior, x0, InteriorRegion.BASE_Y, z0, x0 + r.sizeX() - 1, InteriorRegion.BASE_Y + r.sizeY() - 1,
                z0 + r.sizeZ() - 1, (x, y, z, s) -> {
                    if (s.is(net.minecraft.world.level.block.Blocks.WATER)) {
                        strayWater.add(BlockPos.asLong(x, y, z)); // M29.1: left behind by broken water plants before the fix
                        return;
                    }
                    long k = BlockPos.asLong(x - x0, y - InteriorRegion.BASE_Y, z - z0);
                    add(k, s);
                    int pc = org.overengineer.bettacraft.plumbing.interior.PlumbingBlock.code(s);
                    if (pc != 0) plumbing.put(k, pc);
                });
        InteriorWorldOps.forEachBlock(interior, x0, InteriorRegion.FLOOR_Y, z0, x0 + r.sizeX() - 1, InteriorRegion.FLOOR_Y, z0 + r.sizeZ() - 1,
                (x, y, z, s) -> {
                    if (!s.is(ModBlocks.INTERIOR_FLOOR.get())) return;
                    floorBlocks++;
                    if (!s.getValue(InteriorFloorBlock.SUBSTRATE)) return;
                    substrate++;
                    if (interior.getBlockEntity(new BlockPos(x, y, z)) instanceof InteriorFloorBlockEntity be) onSubstrate(null, be.substrate());
                });
        // M29.1: stray vanilla water blocks go (the interior is dry); plants, corals and decor stay as they are
        for (long l : strayWater) interior.setBlock(BlockPos.of(l), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
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
        t.putIntArray("chem", new int[]{floorBlocks, acidFloor, limeFloor, limeRocks, driftwood, activeSoil}); // M25 (part d: active soil)
        t.putIntArray("litter", new int[]{leafLitter, alderCones, shelter}); // M28
        t.putLongArray("caves", caveBlocks.toLongArray()); // M19
        t.putLongArray("mops", mopBlocks.toLongArray());
        t.putLongArray("small_caves", smallCaveBlocks.toLongArray()); // M29
        t.putBoolean("dry", true); // M29.1: scanned for stray water blocks
        t.putLongArray("pumps", pumps.toLongArray()); // M20
        t.putInt("live_rock", liveRock);
        // M23
        t.putLongArray("corners", corners.toLongArray());
        t.putLongArray("sponge_filters", spongeFilters.toLongArray());
        t.putLongArray("air_stones", airStones.toLongArray());
        t.putLongArray("intake_sponges", intakeSponges.toLongArray());
        long[] pk = new long[power.size()];
        int[] pv = new int[power.size()];
        int j = 0;
        for (var e : power.long2IntEntrySet()) {
            pk[j] = e.getLongKey();
            pv[j++] = e.getIntValue();
        }
        t.putLongArray("power_keys", pk);
        t.putIntArray("power_values", pv);
        // M24
        long[] qk = new long[plumbing.size()];
        int[] qv = new int[plumbing.size()];
        int q = 0;
        for (var e : plumbing.long2IntEntrySet()) {
            qk[q] = e.getLongKey();
            qv[q++] = e.getIntValue();
        }
        t.putLongArray("plumbing_keys", qk);
        t.putIntArray("plumbing_codes", qv);
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
        smallCaveBlocks.clear();
        for (long l : t.getLongArray("small_caves")) smallCaveBlocks.add(l);
        pumps.clear();
        for (long l : t.getLongArray("pumps")) pumps.add(l);
        liveRock = Math.max(0, t.getInt("live_rock"));
        clearM23();
        for (long l : t.getLongArray("corners")) corners.add(l);
        for (long l : t.getLongArray("sponge_filters")) spongeFilters.add(l);
        for (long l : t.getLongArray("air_stones")) airStones.add(l);
        for (long l : t.getLongArray("intake_sponges")) intakeSponges.add(l);
        long[] pk = t.getLongArray("power_keys");
        int[] pv = t.getIntArray("power_values");
        for (int i = 0; i < pk.length && i < pv.length; i++) power.put(pk[i], pv[i]);
        plumbing.clear();
        long[] qk = t.getLongArray("plumbing_keys");
        int[] qv = t.getIntArray("plumbing_codes");
        for (int i = 0; i < qk.length && i < qv.length; i++) plumbing.put(qk[i], qv[i]);
        plumbingVersion++;
        spawningVersion++;
        substrate = t.getInt("substrate");
        needsScan = t.getBoolean("rescan");
        if (!t.contains("small_caves")) needsScan = true; // M29: regions of earlier versions find their small caves once
        if (!t.getBoolean("dry")) needsScan = true; // M29.1: regions of earlier versions lose their stray water blocks once
        int[] chem = t.getIntArray("chem"); // M25: regions of earlier versions count the soil, limestone and wood once
        if (chem.length >= 5) {
            floorBlocks = chem[0];
            acidFloor = chem[1];
            limeFloor = chem[2];
            limeRocks = chem[3];
            driftwood = chem[4];
            activeSoil = chem.length >= 6 ? chem[5] : 0; // part d: there was no active soil before
        } else {
            floorBlocks = acidFloor = limeFloor = limeRocks = driftwood = activeSoil = 0;
            needsScan = true;
        }
        int[] litter = t.getIntArray("litter"); // M28: regions of earlier versions count the litter and shelter once
        if (litter.length >= 3) {
            leafLitter = litter[0];
            alderCones = litter[1];
            shelter = litter[2];
        } else {
            leafLitter = alderCones = shelter = 0;
            needsScan = true;
        }
    }
}
