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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
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
 *     paused until it is put down (B2 in the milestone: the trip is drawn, not simulated).</li>
 * </ul>
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class TankMoves {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/tank-moves");
    /** Ticks a moved member waits for its controller before it forms a tank of its own (the controller did not come along). */
    static final int MEMBER_WAIT = 100;
    /** Ticks a moved controller waits for blocks of its tank that are in chunks not loaded yet. */
    static final int CONTROLLER_WAIT = 200;

    /** Whether a position lies on a moving structure (Sable sub-level); set by the Sable compat. */
    public static volatile java.util.function.BiPredicate<Level, BlockPos> TRAVELLING = (l, p) -> false;

    private record Lift(ResourceKey<Level> dim, BlockPos pos, @Nullable BlockPos controller, boolean wasController, UUID tank,
                        List<BlockPos> members, List<Entity> packed) {}

    private static final List<Lift> LIFTS = new ArrayList<>();

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

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (LIFTS.isEmpty()) return;
        List<Lift> todo = new ArrayList<>(LIFTS);
        LIFTS.clear();
        Map<String, List<Lift>> byTank = new HashMap<>();
        for (Lift l : todo) byTank.computeIfAbsent(l.dim().location() + "|" + l.tank(), k -> new ArrayList<>()).add(l);
        for (List<Lift> group : byTank.values()) {
            ServerLevel level = event.getServer().getLevel(group.getFirst().dim());
            if (level == null) continue;
            try {
                settle(level, group);
            } catch (RuntimeException ex) {
                LOGGER.error("Could not tidy up after a tank left on a contraption", ex);
            }
        }
    }

    private static void settle(ServerLevel level, List<Lift> group) {
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

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        LIFTS.clear();
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
        BlockPos from = be.movedFrom;
        TankData before = TankData.load(t.save());
        List<BlockPos> missing = new ArrayList<>();
        for (long l : before.members()) {
            BlockPos q = map.block(BlockPos.of(l));
            TankBlockEntity o = level.isLoaded(q) ? TankMultiblock.getBE(level, q) : null;
            if (o != null && arrived(level, q, l, from)) {
                o.movedFrom = null;
                o.moveWait = 0;
                o.moveRotation = null;
            } else {
                missing.add(q);
            }
        }
        ListTag stowed = be.stowed;
        be.stowed = new ListTag();
        // animals a mover left at the old place (it moved only the blocks), found before anything is released or moved
        List<Entity> left = new ArrayList<>();
        if (level.isLoaded(before.min()) && !(map.identityRotation() && map.from().equals(map.to()))) {
            for (Entity e : inhabitantsNear(level, before)) {
                if (!e.isPassenger() && !e.isVehicle() && !(e instanceof net.minecraft.world.entity.player.Player)) left.add(e);
            }
        }
        t.relocate(map, null);
        if (!map.identityRotation() && t.regionId() >= 0) InteriorManager.onTankTurned(level, before, t, map);
        be.setController(null, null);
        TankMultiblock.install(level, t);
        TankBlockEntity controller = TankMultiblock.getController(level, t.anchor());
        if (controller != null && t.regionId() >= 0) InteriorManager.regionFor(level, controller);
        if (!missing.isEmpty()) {
            LOGGER.warn("Tank {} arrived at {} without {} of its blocks: they count as broken", t.id(), t.anchor(), missing.size());
            TankMultiblock.removeBlocks(level, t, missing);
        }
        boolean travelling = travelling(level, t.anchor());
        int released = 0, brought = 0;
        if (travelling) {
            // on a moving structure the animals travel packed (also the ones a mover left at the old place)
            stowed.addAll(packAndRemove(level, left));
            TankBlockEntity c = TankMultiblock.getController(level, t.anchor());
            if (c != null) {
                c.stowed.addAll(stowed);
                c.markTankChanged();
            }
        } else {
            released = release(level, stowed, map);
            for (Entity e : left) if (e.isAlive() && place(level, e, map, true)) brought++;
        }
        TankBlockEntity c = TankMultiblock.getController(level, t.anchor());
        if (c != null) c.markTankChanged();
        LOGGER.info("Tank {} moved from {} to {} (rotation {}): {} animal(s) released, {} brought along{}", t.id(), from, t.anchor(),
                java.util.Arrays.toString(map.r()), released, brought, travelling ? ", travelling packed" : "");
    }

    /**
     * A controller standing still with packed animals (a trip that ended without a move, an older save): they come out,
     * unless the tank is on a moving structure.
     */
    static void unpackLeftovers(ServerLevel level, BlockPos pos, TankBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % 20 != 0 || travelling(level, pos)) return;
        ListTag stowed = be.stowed;
        be.stowed = new ListTag();
        int n = release(level, stowed, null);
        be.markTankChanged();
        LOGGER.info("Tank {} at {} unpacked {} animal(s)", be.getTank().id(), pos, n);
    }

    /** Animals as data (they travel packed); the entities are removed. */
    private static ListTag packAndRemove(ServerLevel level, List<Entity> animals) {
        ListTag out = new ListTag();
        for (Entity e : animals) {
            CompoundTag t = new CompoundTag();
            if (!e.isAlive() || !e.save(t)) continue;
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
        int n = 0;
        for (int i = 0; i < stowed.size(); i++) {
            CompoundTag tag = stowed.getCompound(i);
            if (tag.hasUUID("UUID") && level.getEntity(tag.getUUID("UUID")) != null) continue;
            Entity e = EntityType.loadEntityRecursive(tag, level, en -> en);
            if (e == null) {
                LOGGER.warn("A packed animal ({}) could not be loaded again", tag.getString("id"));
                continue;
            }
            if (place(level, e, map, false) && level.addFreshEntity(e)) n++;
        }
        return n;
    }

    /** Moves an entity to its place in the moved tank and adopts it there (the nearest tank of the move if that place is gone). */
    private static boolean place(ServerLevel level, Entity e, @Nullable TankMoveMap map, boolean live) {
        Vec3 p = map == null ? e.position() : map.point(e.position());
        float yaw = map == null ? e.getYRot() : map.yaw(e.getYRot());
        TankBlockEntity c = TankMultiblock.getController(level, BlockPos.containing(p));
        if (c == null && map != null) c = TankMultiblock.getController(level, map.to());
        if (c == null) {
            LOGGER.warn("No tank for a released {} at {}", e.getType().getDescriptionId(), p);
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
