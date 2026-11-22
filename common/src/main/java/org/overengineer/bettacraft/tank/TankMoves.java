package org.overengineer.bettacraft.tank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M32 part b: tanks that travel as block data (Create contraptions: pistons, bearings, carts, trains; Sable sub-levels:
 * Create Aeronautics airships; any mod that moves block entities by their data).
 * <ul>
 *     <li><b>Packing:</b> the animals of a tank go into its data before the trip ({@link #pack}, Create's start of a move;
 *     for Sable when the tank arrives in the sub-level), like into a fish bag: entity data with the id. The live animals
 *     are only removed at the end of the tick, after the tank really left (an assembly that failed keeps them).</li>
 *     <li><b>Arrival:</b> every tank block entity remembers where it was saved ({@code at}); one loaded at another
 *     position was moved. Its controller finds the rigid move (the rotation the mover reported, else the one of the 24
 *     that matches most blocks), moves the tank data along ({@link TankData#relocate}), turns the interior if the tank
 *     was turned, installs the tank again, releases the packed animals at their places and brings along the animals a
 *     mover left at the old place. Blocks that did not come along count as broken ({@link TankMultiblock#removeBlocks}).</li>
 *     <li>A tank on a moving structure ({@link #TRAVELLING}, Sable sub-levels) keeps its animals packed and its care
 *     paused until it is put down (B2 in M32: the trip is drawn, not simulated) - unless {@code features.compat.create.travelCare}
 *     is on (M50): then the animals whose type Sable keeps in its sub-levels ({@link #RETAIN}, ours by our data) swim on in the
 *     tank on the airship and its care runs as on the ground; only the others travel packed.</li>
 * </ul>
 */
public final class TankMoves {
    static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/tank-moves");
    /**
     * Ticks a moved member waits for its controller before it forms a tank of its own (the controller did not come along);
     * longer than the controller waits for blocks in chunks that are not loaded yet.
     */
    static final int MEMBER_WAIT = 300;
    /** Ticks a moved controller waits for blocks of its tank that are in chunks not loaded yet. */
    static final int CONTROLLER_WAIT = 200;

    /** Whether a position lies on a moving structure (Sable sub-level); set by the Sable compat. */
    public static volatile java.util.function.BiPredicate<Level, BlockPos> TRAVELLING = (l, p) -> false;

    /** Where a point of a moving structure is in the world now (Sable: the ship's pose); set by the Sable compat. */
    public static volatile java.util.function.BiFunction<Level, Vec3, Vec3> PROJECT = (l, p) -> p;

    /**
     * M50: entity types Sable keeps in its sub-levels ({@code sable:retain_in_sub_level}; all ours are in it by our data). Other
     * types it would push out of the airship into the world, so they travel packed.
     */
    public static final net.minecraft.tags.TagKey<EntityType<?>> RETAIN = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("sable", "retain_in_sub_level"));

    /** M50: whether an animal of this type swims on in a tank on a moving structure (Sable) instead of travelling packed. */
    public static boolean swimsOn(EntityType<?> type) {
        return TravelCare.enabled() && type.is(RETAIN);
    }

    private static boolean swimsOn(CompoundTag tag) {
        return EntityType.by(tag).map(TankMoves::swimsOn).orElse(false);
    }

    /** M50: whether the care of a tank on a moving structure runs (features.compat.create.travelCare). */
    public static boolean livesOn(Level level, BlockPos pos) {
        return TravelCare.enabled() && travelling(level, pos);
    }

    /** Interior regions of tanks on Create contraptions (no block entity in the world): where they are now (null = gone). */
    private record TravelSpot(ResourceKey<Level> dim, java.util.function.Supplier<Vec3> where) {}

    static final Map<Integer, TravelSpot> TRAVELLING_REGIONS = new HashMap<>();

    record Lift(ResourceKey<Level> dim, BlockPos pos, @Nullable BlockPos controller, boolean wasController, UUID tank,
                        List<BlockPos> members, List<Entity> packed) {}

    static final List<Lift> LIFTS = new ArrayList<>();

    private TankMoves() {}

    public static boolean travelling(Level level, BlockPos pos) {
        try {
            return TRAVELLING.test(level, pos);
        } catch (RuntimeException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------ packing

    /** The live animals of the tank as entity data (with id); the entities themselves are left alone. */
    public static ListTag pack(ServerLevel level, TankData tank, List<Entity> packedOut) {
        ListTag list = new ListTag();
        for (Entity e : inhabitantsNear(level, tank)) {
            if (e instanceof net.minecraft.world.entity.player.Player || e.isPassenger() || e.isVehicle()) continue;
            CompoundTag t = new CompoundTag();
            if (!e.save(t)) continue;
            markPlace(t, e, tank);
            TravelCare.annotate(t, e); // M50
            list.add(t);
            packedOut.add(e);
        }
        return list;
    }

    /**
     * The tank's animals at its place: those with its reference in its box, and those of its inhabitant list right there
     * that already lost the reference (a mover took the blocks first and the animal noticed before the tank came back).
     */
    static List<Entity> inhabitantsNear(ServerLevel level, TankData tank) {
        List<Entity> out = new ArrayList<>(TankInhabitants.liveInhabitants(level, tank));
        AABB box = TankInhabitants.tankBox(tank).inflate(2);
        for (UUID u : tank.inhabitants()) {
            Entity e = level.getEntity(u);
            if (e != null && e.isAlive() && !out.contains(e) && box.contains(e.position())) out.add(e);
        }
        return out;
    }

    /**
     * A mover took the tank block at {@code pos} into its data this tick (Create's start of a move). At the end of the tick
     * the packed animals are removed if the tank really left, and the rest of a tank that only partly left is tidied up.
     */
    public static void lifted(ServerLevel level, BlockPos pos, @Nullable BlockPos controller, boolean wasController, UUID tank,
                              List<BlockPos> members, List<Entity> packed) {
        LIFTS.add(new Lift(level.dimension(), pos.immutable(), controller == null ? null : controller.immutable(), wasController, tank,
                List.copyOf(members), List.copyOf(packed)));
    }


    static void settle(ServerLevel level, List<Lift> group) {
        Lift head = group.stream().filter(Lift::wasController).findFirst().orElse(null);
        if (head != null) {
            if (stillHere(level, head.pos(), head.tank())) return; // the assembly failed: the tank and its animals stay
            for (Entity e : head.packed()) if (e.isAlive()) e.discard();
            LOGGER.info("Tank {} left {} as block data with {} packed animal(s)", head.tank(), head.pos(), head.packed().size());
            // blocks of the tank that stayed form tanks of their own (the moving data has them as missing)
            for (BlockPos p : head.members()) {
                if (p.equals(head.pos()) || !level.isLoaded(p)) continue;
                TankBlockEntity be = TankMultiblock.getBE(level, p);
                if (be != null && head.pos().equals(be.getControllerPos())) TankMultiblock.repairOrphan(level, p, be);
            }
            return;
        }
        // only some blocks left, the controller stayed: they are taken out of the tank like broken blocks
        BlockPos cp = group.getFirst().controller();
        if (cp == null || !level.isLoaded(cp)) return;
        TankBlockEntity c = TankMultiblock.getBE(level, cp);
        if (c == null || !c.isController() || !c.getTank().id().equals(group.getFirst().tank())) return;
        List<BlockPos> gone = new ArrayList<>();
        for (Lift l : group) if (c.getTank().contains(l.pos()) && !(level.getBlockState(l.pos()).getBlock() instanceof TankBlock)) gone.add(l.pos());
        if (!gone.isEmpty()) TankMultiblock.removeBlocks(level, c.getTank(), gone);
    }

    private static boolean stillHere(ServerLevel level, BlockPos pos, UUID tank) {
        TankBlockEntity be = TankMultiblock.getBE(level, pos);
        return be != null && be.getTank() != null && be.getTank().id().equals(tank);
    }


    /** A tank with this interior region travels on a contraption that is at {@code where} (Create, every tick of the trip). */
    public static void travellingRegion(int regionId, Level level, java.util.function.Supplier<Vec3> where) {
        if (regionId >= 0) TRAVELLING_REGIONS.put(regionId, new TravelSpot(level.dimension(), where));
    }

    /** M53c: whether a tank with this interior region is travelling on a contraption (its region is never cleaned up). */
    public static boolean isTravellingRegion(int regionId) {
        return TRAVELLING_REGIONS.containsKey(regionId);
    }

    public static void stoppedTravelling(int regionId) {
        TRAVELLING_REGIONS.remove(regionId);
    }

    /**
     * Whether a player may get the interior of a region whose tank travels (the miniature on the contraption or airship):
     * the contraption is near, or the claimed controller is a travelling tank of that region whose ship is near.
     */
    public static boolean travellingRegionNear(net.minecraft.server.level.ServerPlayer player, int regionId, BlockPos claimed, double range) {
        TravelSpot spot = TRAVELLING_REGIONS.get(regionId);
        if (spot != null) {
            Vec3 w = spot.where().get();
            if (w == null) {
                TRAVELLING_REGIONS.remove(regionId);
            } else if (spot.dim() == player.level().dimension() && w.distanceToSqr(player.position()) <= range * range) {
                return true;
            }
        }
        Level level = player.level();
        if (!level.isLoaded(claimed) || !travelling(level, claimed)) return false;
        TankBlockEntity c = TankMultiblock.getController(level, claimed);
        if (c == null || c.getTank().regionId() != regionId) return false;
        return PROJECT.apply(level, Vec3.atCenterOf(claimed)).distanceToSqr(player.position()) <= range * range;
    }

    // ------------------------------------------------------------------ arrival

    /** Server tick of a tank block entity loaded at another position than it was saved at. */
    static void tick(ServerLevel level, BlockPos pos, TankBlockEntity be) {
        BlockPos from = be.movedFrom;
        TankData t = be.getTank();
        boolean wasController = t != null && from.equals(be.getControllerPos());
        if (!wasController) {
            // a member waits for its controller; alone after a while (its controller stayed or was lost) it forms a tank again
            if (++be.moveWait > MEMBER_WAIT) {
                be.movedFrom = null;
                be.moveWait = 0;
                be.setController(null, null);
                TankMultiblock.onTankBlockAdded(level, pos, null);
            }
            return;
        }
        List<int[]> candidates = new ArrayList<>();
        if (be.moveRotation != null) candidates.add(be.moveRotation);
        candidates.addAll(TankMoveMap.rotations());
        TankMoveMap best = null;
        int bestFound = -1;
        boolean bestUnloaded = false;
        for (int[] r : candidates) {
            TankMoveMap m = new TankMoveMap(from, pos, r);
            int found = 0;
            boolean unloaded = false;
            for (long l : t.members()) {
                BlockPos q = m.block(BlockPos.of(l));
                if (!level.isLoaded(q)) {
                    unloaded = true;
                    continue;
                }
                if (arrived(level, q, l, from)) found++;
            }
            if (found > bestFound) {
                best = m;
                bestFound = found;
                bestUnloaded = unloaded;
            }
            if (found == t.memberCount()) break;
        }
        if (bestFound < t.memberCount() && (bestUnloaded ? be.moveWait < CONTROLLER_WAIT : be.moveWait < 2)) {
            be.moveWait++;
            return;
        }
        arrive(level, be, t, best);
    }

    private static boolean arrived(ServerLevel level, BlockPos at, long oldPos, BlockPos oldController) {
        TankBlockEntity o = TankMultiblock.getBE(level, at);
        return o != null && o.movedFrom != null && o.movedFrom.asLong() == oldPos && oldController.equals(o.getControllerPos());
    }

    private static void arrive(ServerLevel level, TankBlockEntity be, TankData t, TankMoveMap map) {
        ListTag stowed = be.stowed;
        be.stowed = new ListTag();
        arrive(level, be, t, be.movedFrom, stowed, map);
    }

    /**
     * M32 part c: a tank whose controller did not come down (Create could not place that block, it dropped it as an
     * item) arrives from the data the mover had: the blocks that did come down form the tank (the controller's place
     * counts as broken), its interior region, water and packed animals with it. {@code map} takes the old controller
     * position to where the controller would have been.
     */
    public static void arriveWithoutController(ServerLevel level, TankData t, BlockPos oldController, ListTag stowed, TankMoveMap map) {
        arrive(level, null, t, oldController, stowed, map);
    }

    private static void arrive(ServerLevel level, @Nullable TankBlockEntity be, TankData t, BlockPos from, ListTag stowed, TankMoveMap map) {
        TankData before = TankData.load(t.save());
        List<BlockPos> missing = new ArrayList<>();
        List<BlockPos> landed = new ArrayList<>();
        it.unimi.dsi.fastutil.longs.LongSet skip = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        for (long l : before.members()) {
            BlockPos q = map.block(BlockPos.of(l));
            TankBlockEntity o = level.isLoaded(q) ? TankMultiblock.getBE(level, q) : null;
            if (o != null && arrived(level, q, l, from)) {
                o.movedFrom = null;
                o.moveWait = 0;
                o.moveRotation = null;
                landed.add(q);
            } else {
                missing.add(q);
                skip.add(q.asLong()); // M32 part c: a block of another tank there is never taken over
            }
        }
        // animals a mover left at the old place (it moved only the blocks), found before anything is released or moved
        List<Entity> left = new ArrayList<>();
        if (level.isLoaded(before.min()) && !(map.identityRotation() && map.from().equals(map.to()))) {
            for (Entity e : inhabitantsNear(level, before)) {
                if (!e.isPassenger() && !e.isVehicle() && !(e instanceof net.minecraft.world.entity.player.Player)) left.add(e);
            }
        }
        t.relocate(map, null);
        if (!map.identityRotation() && t.regionId() >= 0) InteriorManager.onTankTurned(level, before, t, map);
        if (be != null) be.setController(null, null);
        TankMultiblock.install(level, t, skip);
        TankBlockEntity controller = TankMultiblock.getController(level, t.anchor());
        if (controller != null && t.regionId() >= 0) InteriorManager.regionFor(level, controller);
        if (controller != null && !(map.identityRotation() && map.from().equals(map.to()))) {
            org.overengineer.bettacraft.progression.Progress.awardNear(level, controller, org.overengineer.bettacraft.progression.Progress.TRAVELLED, 0); // M45
        }
        if (!missing.isEmpty()) {
            LOGGER.warn("Tank {} arrived at {} without {} of its blocks: they count as broken", t.id(), t.anchor(), missing.size());
            TankMultiblock.removeBlocks(level, t, missing);
        }
        BlockPos home = null; // a block of the arrived tank (its controller's place may be the one that is missing)
        for (BlockPos q : landed) if (TankMultiblock.getController(level, q) != null) {
            home = q;
            break;
        }
        if (home == null) home = t.anchor();
        boolean travelling = travelling(level, home);
        int released = 0, brought = 0;
        if (travelling) {
            // on a moving structure the animals travel packed (also the ones a mover left at the old place); M50: with the
            // care on the road the ones Sable keeps in its sub-levels swim on in the tank on the airship
            List<Entity> packNow = new ArrayList<>();
            for (Entity e : left) {
                if (e.isAlive() && swimsOn(e.getType()) && place(level, e, map, true, home)) brought++;
                else packNow.add(e);
            }
            stowed.addAll(packAndRemove(level, packNow, before));
            ListTag keep = new ListTag();
            ListTag free = new ListTag();
            for (int i = 0; i < stowed.size(); i++) (swimsOn(stowed.getCompound(i)) ? free : keep).add(stowed.getCompound(i));
            released = release(level, free, map, home);
            TankBlockEntity c = TankMultiblock.getController(level, home);
            if (c != null) {
                c.stowed.addAll(keep);
                c.markTankChanged();
            }
        } else {
            released = release(level, stowed, map, home);
            for (Entity e : left) if (e.isAlive() && place(level, e, map, true, home)) brought++;
        }
        TankBlockEntity c = TankMultiblock.getController(level, home);
        if (c != null) {
            brought += adoptMoved(level, c); // M50: animals the mover took along itself (Sable puts down what swam on its airship)
            if (be != null && !travelling) {
                TravelCare.arrived(level, c, be.tripLog); // M50: the trip on a Create contraption into the log
                be.tripLog = null;
            }
            c.markTankChanged();
        }
        LOGGER.info("Tank {} moved from {} to {} (rotation {}): {} animal(s) released, {} brought along{}", t.id(), from, t.anchor(),
                java.util.Arrays.toString(map.r()), released, brought, travelling ? (TravelCare.enabled() ? ", on a moving structure" : ", travelling packed") : "");
    }

    /**
     * A controller standing still with packed animals (a trip that ended without a move, an older save): they come out,
     * unless the tank is on a moving structure.
     */
    static void unpackLeftovers(ServerLevel level, BlockPos pos, TankBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
        ListTag stowed = be.stowed;
        if (travelling(level, pos)) {
            // M50: on a moving structure the ones that may swim on come out when the care runs on the road
            ListTag keep = new ListTag(), free = new ListTag();
            for (int i = 0; i < stowed.size(); i++) (swimsOn(stowed.getCompound(i)) ? free : keep).add(stowed.getCompound(i));
            if (free.isEmpty()) return;
            be.stowed = keep;
            int n = release(level, free, null);
            be.markTankChanged();
            LOGGER.info("Tank {} on a moving structure at {} let {} packed animal(s) swim", be.getTank().id(), pos, n);
            return;
        }
        be.stowed = new ListTag();
        int n = release(level, stowed, null);
        be.markTankChanged();
        LOGGER.info("Tank {} at {} unpacked {} animal(s)", be.getTank().id(), pos, n);
    }

    /**
     * M50: animals of the tank (by its id) the mover took along itself, inside the arrived tank but still adopted by its old
     * controller position (Sable moves what is in its plot when it puts an airship down): adopted by the new controller.
     */
    private static int adoptMoved(ServerLevel level, TankBlockEntity c) {
        int n = 0;
        for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) {
            TankRef ref = org.overengineer.bettacraft.registry.ModAttachments.TANK_REF.get(e);
            if (c.getBlockPos().equals(ref.controller())) continue;
            TankInhabitants.adopt(e, c);
            c.getTank().inhabitants().add(e.getUUID());
            n++;
        }
        return n;
    }

    /** Key of a packed animal's place in its tank (from the tank's lowest corner), for drawing it while the tank travels. */
    public static final String PLACE_KEY = "bettacraft:tank_place";

    private static void markPlace(CompoundTag t, Entity e, TankData tank) {
        BlockPos mn = tank.min();
        t.put(PLACE_KEY, newDoubleList(e.getX() - mn.getX(), e.getY() - mn.getY(), e.getZ() - mn.getZ()));
    }

    private static ListTag newDoubleList(double... v) {
        ListTag l = new ListTag();
        for (double d : v) l.add(net.minecraft.nbt.DoubleTag.valueOf(d));
        return l;
    }

    /** Animals as data (they travel packed); the entities are removed. */
    private static ListTag packAndRemove(ServerLevel level, List<Entity> animals, TankData tank) {
        ListTag out = new ListTag();
        for (Entity e : animals) {
            CompoundTag t = new CompoundTag();
            if (!e.isAlive() || !e.save(t)) continue;
            markPlace(t, e, tank);
            TravelCare.annotate(t, e); // M50
            out.add(t);
            e.discard();
        }
        return out;
    }

    /**
     * Puts packed animals back into the tank at the place they had in it (turned with it). An animal that is still in the
     * world (the same entity) is never doubled. Returns how many came back.
     */
    public static int release(ServerLevel level, ListTag stowed, @Nullable TankMoveMap map) {
        return release(level, stowed, map, null);
    }

    private static int release(ServerLevel level, ListTag stowed, @Nullable TankMoveMap map, @Nullable BlockPos home) {
        int n = 0;
        for (int i = 0; i < stowed.size(); i++) {
            CompoundTag tag = stowed.getCompound(i);
            if (tag.hasUUID("UUID") && level.getEntity(tag.getUUID("UUID")) != null) continue;
            Entity e = EntityType.loadEntityRecursive(tag, level, en -> en);
            if (e == null) {
                org.overengineer.bettacraft.debug.LogArea.TANK.warnThrottled("packed_load", "A packed animal ({}) could not be loaded again", tag.getString("id"));
                continue;
            }
            if (place(level, e, map, false, home) && level.addFreshEntity(e)) n++;
        }
        return n;
    }

    /** Moves an entity to its place in the moved tank and adopts it there (the nearest tank of the move if that place is gone). */
    private static boolean place(ServerLevel level, Entity e, @Nullable TankMoveMap map, boolean live, @Nullable BlockPos home) {
        Vec3 p = map == null ? e.position() : map.point(e.position());
        float yaw = map == null ? e.getYRot() : map.yaw(e.getYRot());
        TankBlockEntity c = TankMultiblock.getController(level, BlockPos.containing(p));
        if (c == null && map != null) c = TankMultiblock.getController(level, map.to());
        if (c == null && home != null) c = TankMultiblock.getController(level, home);
        if (c == null) {
            org.overengineer.bettacraft.debug.LogArea.TANK.warnThrottled("released_no_tank", "No tank for a released {} at {}", e.getType().getDescriptionId(), p);
            return false;
        }
        if (!c.getTank().contains(BlockPos.containing(p))) p = TankInhabitants.findSpawnPos(level, c.getTank(), level.getRandom());
        if (live) e.teleportTo(level, p.x, p.y, p.z, java.util.Set.of(), yaw, e.getXRot());
        else e.moveTo(p.x, p.y, p.z, yaw, e.getXRot());
        e.setYHeadRot(yaw);
        if (e instanceof LivingEntity le) le.yBodyRot = yaw;
        e.setDeltaMovement(Vec3.ZERO);
        TankInhabitants.adopt(e, c);
        c.getTank().inhabitants().add(e.getUUID());
        return true;
    }
}
