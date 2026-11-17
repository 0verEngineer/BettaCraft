package org.overengineer.bettacraft.compat.create;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.PackedTank;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankMoveMap;
import org.overengineer.bettacraft.tank.TankMoves;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M32 part c: what happens to a travelling tank when its Create contraption ends without putting the controller down.
 * Create calls our movement behaviour's {@code stopMoving} when a contraption is put down (before its blocks are placed)
 * and when the contraption entity is removed ({@code remove}: killed, discarded, a train carriage that goes into unloaded
 * chunks). At the end of that tick:
 * <ul>
 *     <li><b>Put down, the controller not placed</b> (Create could not place it: an unbreakable block at its target; Create
 *     drops the block as a plain item without data): if other blocks of the tank came down, they form the tank with all its
 *     data, interior and packed animals ({@link TankMoves#arriveWithoutController}; the controller's place counts as broken,
 *     the dropped item gets its glass back). If none came down, the dropped tank item gets the whole tank as data
 *     ({@link PackedTank}): placed again, the tank comes back with everything.</li>
 *     <li><b>Destroyed</b> (the contraption entity was killed, or discarded by Create for good: not a train carriage, which
 *     Create discards when it goes into unloaded chunks and makes again later): the tank data is gone with the entity, so its
 *     interior region is freed (no orphan region in the interior level).</li>
 *     <li>Anything else (a train carriage unloading, an entity still alive) is left alone: the data still exists.</li>
 * </ul>
 * A contraption that was put down is told apart from a removed one by Create's own flag {@code skipActorStop}, which
 * {@code disassemble} sets just before it discards the entity. Without that flag (Create changed) nothing is recovered or
 * freed (safe side: no doubles, a possible orphan region).
 */
public final class CreateLandings {
    static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/create");

    /** A tank controller's trip that ended this tick (Create's stopMoving). */
    record Stop(ResourceKey<Level> dim, @Nullable Entity contraption, CompoundTag data, BlockPos oldController,
                        @Nullable Vec3 position, @Nullable BlockPos local, UUID tank) {}

    static final List<Stop> STOPS = new ArrayList<>();
    /** Tanks whose controller Create placed this tick (its block entity transformer). */
    static final Set<UUID> LANDED = new HashSet<>();
    /** Member blocks Create placed this tick, by dimension and old controller position. */
    static final Map<String, List<TankBlockEntity>> MEMBERS = new HashMap<>();

    private static Field skipActorStop;
    private static boolean skipLookedUp;
    static final Set<String> CREATE_OWN = Set.of(
            "com.simibubi.create.content.contraptions.ControlledContraptionEntity",
            "com.simibubi.create.content.contraptions.OrientedContraptionEntity",
            "com.simibubi.create.content.contraptions.gantry.GantryContraptionEntity");

    private CreateLandings() {}

    private static String key(ResourceKey<Level> dim, BlockPos oldController) {
        return dim.location() + "|" + oldController.asLong();
    }

    /** The trip of a controller ends (put down or removed). */
    static void stopped(ServerLevel level, @Nullable Entity contraption, CompoundTag data, @Nullable Vec3 position, @Nullable BlockPos local) {
        if (!data.contains("tank", Tag.TAG_COMPOUND)) return;
        TankData t = TankData.load(data.getCompound("tank"));
        int[] c = data.getIntArray("controller");
        if (c.length != 3) return;
        UUID id = t.id();
        for (Stop s : STOPS) if (s.tank().equals(id)) return; // Create can stop the actors twice in one tick
        STOPS.add(new Stop(level.dimension(), contraption, data.copy(), new BlockPos(c[0], c[1], c[2]), position,
                local == null ? null : local.immutable(), id));
    }

    /** Create put a tank block down (its block entity transformer). */
    static void placed(TankBlockEntity be) {
        if (be.getLevel() == null) return;
        if (be.getTank() != null) {
            LANDED.add(be.getTank().id());
        } else if (be.getControllerPos() != null && be.movedFrom() != null) {
            MEMBERS.computeIfAbsent(key(be.getLevel().dimension(), be.getControllerPos()), k -> new ArrayList<>()).add(be);
        }
    }



    /** Whether Create put the contraption down (its {@code skipActorStop} flag), or null if that cannot be read. */
    @Nullable
    static Boolean putDown(Entity e) {
        if (!skipLookedUp) {
            skipLookedUp = true;
            try {
                Class<?> c = Class.forName("com.simibubi.create.content.contraptions.AbstractContraptionEntity", false, e.getClass().getClassLoader());
                skipActorStop = c.getDeclaredField("skipActorStop");
                skipActorStop.setAccessible(true);
            } catch (ReflectiveOperationException | RuntimeException ex) {
                LOGGER.warn("Create's contraption entity has no skipActorStop flag: tanks that cannot be put down are not recovered", ex);
            }
        }
        if (skipActorStop == null || !skipActorStop.getDeclaringClass().isInstance(e)) return null;
        try {
            return skipActorStop.getBoolean(e);
        } catch (IllegalAccessException ex) {
            return null;
        }
    }

    /** Create put the contraption down but could not place the controller (it dropped it as a plain item). */
    static void notPlaced(ServerLevel level, Stop s) {
        TankData t = TankData.load(s.data().getCompound("tank"));
        ListTag stowed = s.data().getList("stowed", Tag.TAG_COMPOUND);
        List<TankBlockEntity> members = new ArrayList<>();
        for (TankBlockEntity be : MEMBERS.getOrDefault(key(s.dim(), s.oldController()), List.of())) {
            if (!be.isRemoved() && be.movedFrom() != null && s.oldController().equals(be.getControllerPos())) members.add(be);
        }
        if (!members.isEmpty()) {
            TankBlockEntity m = members.getFirst();
            int[] r = m.moveRotation() != null ? m.moveRotation() : TankMoveMap.IDENTITY;
            BlockPos target = new TankMoveMap(m.movedFrom(), m.getBlockPos(), r).block(s.oldController());
            TankMoves.arriveWithoutController(level, t, s.oldController(), stowed, new TankMoveMap(s.oldController(), target, r));
            ItemEntity drop = droppedItem(level, Vec3.atCenterOf(target));
            if (drop != null) {
                ItemStack st = drop.getItem().copy();
                PackedTank.restyle(st, s.data());
                drop.setItem(st);
            }
            LOGGER.warn("Create could not put down the controller of tank {} at {}: the tank formed from its other {} block(s) with all its data",
                    t.id(), target, members.size());
            return;
        }
        // where Create dropped it: the contraption's own transform of the controller's place (else its last position)
        BlockPos target = s.contraption() != null && s.local() != null ? CreateCompat.target(s.contraption(), s.local()) : null;
        Vec3 at = target != null ? Vec3.atCenterOf(target) : s.position() != null ? s.position()
                : s.contraption() != null ? s.contraption().position() : Vec3.atCenterOf(s.oldController());
        ItemEntity drop = droppedItem(level, at);
        if (drop != null && drop.getItem().getCount() == 1) {
            drop.setItem(PackedTank.pack(drop.getItem().copy(), s.data()));
        } else {
            if (drop != null) {
                ItemStack rest = drop.getItem().copy();
                rest.shrink(1);
                drop.setItem(rest);
            }
            ItemEntity e = new ItemEntity(level, at.x, at.y, at.z, PackedTank.pack(new ItemStack(ModBlocks.TANK.get()), s.data()));
            e.setDefaultPickUpDelay();
            level.addFreshEntity(e);
        }
        LOGGER.warn("Create could not put down tank {} near {}: it is a packed tank item now ({} animal(s), interior region {})",
                t.id(), BlockPos.containing(at), stowed.size(), t.regionId());
    }

    /** The plain tank item Create dropped this tick near {@code at}, if any. */
    @Nullable
    private static ItemEntity droppedItem(ServerLevel level, Vec3 at) {
        ItemEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(2.5))) {
            ItemStack st = e.getItem();
            if (!st.is(ModBlocks.TANK.get().asItem()) || PackedTank.isPacked(st) || e.tickCount > 2) continue;
            double d = e.position().distanceToSqr(at);
            if (d < bestD) {
                best = e;
                bestD = d;
            }
        }
        return best;
    }

    /** The contraption was destroyed with the tank on it: its interior region is freed (nothing could claim it again). */
    static void destroyed(ServerLevel level, Stop s) {
        TankData t = TankData.load(s.data().getCompound("tank"));
        if (t.regionId() < 0 || InteriorRegions.get(level.getServer()).get(t.regionId()) == null) return;
        int region = t.regionId();
        InteriorManager.freeRegion(level, t);
        TankMoves.stoppedTravelling(region);
        LOGGER.warn("The contraption with tank {} was destroyed: its interior region {} was freed ({} packed animal(s) lost with it)",
                t.id(), region, s.data().getList("stowed", Tag.TAG_COMPOUND).size());
    }
}
