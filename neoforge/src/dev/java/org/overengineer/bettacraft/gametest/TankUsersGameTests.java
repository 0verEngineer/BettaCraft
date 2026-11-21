package org.overengineer.bettacraft.gametest;

import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.monitor.DashboardLayout;
import org.overengineer.bettacraft.monitor.DashboardTile;
import org.overengineer.bettacraft.monitor.MonitorAlarms;
import org.overengineer.bettacraft.monitor.MonitorBlock;
import org.overengineer.bettacraft.monitor.MonitorBlockEntity;
import org.overengineer.bettacraft.monitor.MonitorSync;
import org.overengineer.bettacraft.monitor.MonitorValue;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankAccess;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankUsers;
import org.overengineer.bettacraft.tank.TankUsers.Role;

/**
 * M53: the users of a tank (owner, caretakers, viewers, everyone else by the lock), saved with the tank, and the alarm
 * waypoints only for the owner and the listed users with their switches on (none for a player who is merely near).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TankUsersGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private TankUsersGameTests() {}

    /** Owner, caretaker, viewer and a stranger: who may change, go in and manage, with the lock open and closed. */
    @GameTest(template = EMPTY, batch = "bettacraft_m53_users", timeoutTicks = 60)
    public static void rolesDecideWhoMayChange(GameTestHelper h) {
        TankBlockEntity c = AerationGameTests.tank(h, 2, 1, 2, 2, 2, 2);
        TankData t = c.getTank();
        ServerPlayer owner = MockPlayers.serverPlayer(h), care = MockPlayers.serverPlayer(h), view = MockPlayers.serverPlayer(h),
                other = MockPlayers.serverPlayer(h);
        try {
            t.setOwner(owner.getUUID());
            t.users().put(care.getUUID(), "care", Role.CARETAKER);
            t.users().put(view.getUUID(), "view", Role.VIEWER);
            h.assertTrue(TankAccess.role(owner, t) == Role.OWNER && TankAccess.role(care, t) == Role.CARETAKER
                    && TankAccess.role(view, t) == Role.VIEWER && TankAccess.role(other, t) == Role.OTHER, "the roles");
            for (boolean locked : new boolean[]{false, true}) {
                t.setLocked(locked);
                h.assertTrue(TankInhabitants.canModify(owner, t) && TankInhabitants.canModify(care, t), "owner and caretaker change (locked " + locked + ")");
                h.assertTrue(!TankInhabitants.canModify(view, t) && !TankAccess.canEnter(view, t), "a viewer changes nothing and stays out (locked " + locked + ")");
                h.assertTrue(TankInhabitants.canModify(other, t) == !locked && TankAccess.canEnter(other, t) == !locked, "a stranger: the lock decides (locked " + locked + ")");
                h.assertTrue(TankAccess.canManage(owner, t) && TankAccess.canManage(care, t) && !TankAccess.canManage(view, t)
                        && !TankAccess.canManage(other, t), "only owner and caretakers manage");
            }
            // a viewer becomes a caretaker, a caretaker is removed
            t.users().put(view.getUUID(), "view", Role.CARETAKER);
            t.users().remove(care.getUUID());
            h.assertTrue(TankInhabitants.canModify(view, t) && !TankInhabitants.canModify(care, t), "roles changed (the tank is locked)");
            // the owner cannot be listed, a stranger's role is not listable
            h.assertTrue(!t.users().put(other.getUUID(), "x", Role.OWNER) && !t.users().put(other.getUUID(), "x", Role.OTHER), "only caretaker or viewer");
        } finally {
            for (ServerPlayer p : new ServerPlayer[]{owner, care, view, other}) p.level().getServer().getPlayerList().remove(p);
        }
        h.succeed();
    }

    /** The users and the waypoint switches are saved with the tank; a new user's own waypoint is off, the owner's on. */
    @GameTest(template = EMPTY, batch = "bettacraft_m53_users", timeoutTicks = 60)
    public static void usersAreSavedWithTheTank(GameTestHelper h) {
        TankBlockEntity c = AerationGameTests.tank(h, 2, 1, 2, 2, 2, 2);
        TankData t = c.getTank();
        UUID owner = UUID.randomUUID(), a = UUID.randomUUID(), b = UUID.randomUUID();
        t.setOwner(owner);
        h.assertTrue(t.users().save() == null, "nothing to save for a tank without users");
        t.users().put(a, "Alice", Role.CARETAKER);
        t.users().put(b, "Bob", Role.VIEWER);
        h.assertTrue(TankAccess.ownWaypoint(owner, t) && !TankAccess.ownWaypoint(a, t) && !TankAccess.ownWaypoint(b, t), "defaults: only the owner's is on");
        t.users().setWaypoint(b, owner, true);
        t.users().setOwnerWaypoint(false);
        CompoundTag tag = t.save();
        TankData back = TankData.load(tag);
        h.assertTrue(back.users().size() == 2 && back.users().get(a).role == Role.CARETAKER && "Alice".equals(back.users().get(a).name)
                && back.users().get(b).role == Role.VIEWER && back.users().get(b).waypoint && !back.users().ownerWaypoint()
                && back.users().waypointsAll(), "loaded back: " + back.users().entries().size());
        // the list is limited
        for (int i = 0; i < TankUsers.MAX + 5; i++) t.users().put(UUID.randomUUID(), "u" + i, Role.VIEWER);
        h.assertTrue(t.users().size() == TankUsers.MAX, "at most " + TankUsers.MAX + ": " + t.users().size());
        h.succeed();
    }

    /**
     * An alarm: the waypoint goes to the owner only (his switch is on by default), not to a viewer (his own is off) and not to
     * a player near the tank; the viewer switches his on and gets it, the switch for all takes it from both.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m53_users", timeoutTicks = 100)
    public static void waypointsOnlyForUsersWithTheirSwitchOn(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity t = AerationGameTests.tank(h, 2, 1, 2, 2, 2, 2);
        TankData d = t.getTank();
        d.setWaterTemp(30F);
        ServerPlayer owner = MockPlayers.serverPlayer(h), viewer = MockPlayers.serverPlayer(h), near = MockPlayers.serverPlayer(h);
        try {
            BlockPos at = h.absolutePos(new BlockPos(4, 2, 4));
            near.teleportTo(level, at.getX(), at.getY(), at.getZ(), java.util.Set.of(), 0, 0, false);
            d.setOwner(owner.getUUID());
            d.users().put(viewer.getUUID(), "viewer", Role.VIEWER);
            BlockPos tl = new BlockPos(9, 2, 8);
            BlockState s = ModBlocks.MONITOR.get().defaultBlockState().setValue(MonitorBlock.FACING, Direction.NORTH);
            h.setBlock(tl, s);
            MonitorBlockEntity m = (MonitorBlockEntity) level.getBlockEntity(h.absolutePos(tl));
            DashboardLayout l = new DashboardLayout();
            l.auto = false;
            l.tiles.add(DashboardTile.alarm(l.nextId++, 0, 0, 4, 2, d.id(), MonitorValue.TEMPERATURE, false, 28F, false));
            m.setLayout(l);
            MonitorSync.cycle(level, m);
            h.assertTrue(MonitorAlarms.waypointOpen(d.id()), "a waypoint at the tank");
            h.assertTrue(MonitorAlarms.waypointHolders(d.id()).equals(Set.of(owner.getUUID())), "only the owner: " + MonitorAlarms.waypointHolders(d.id()));
            d.users().setWaypoint(viewer.getUUID(), d.owner(), true);
            MonitorAlarms.refreshWaypoints(t);
            h.assertTrue(MonitorAlarms.waypointHolders(d.id()).equals(Set.of(owner.getUUID(), viewer.getUUID())), "the viewer switched his on: "
                    + MonitorAlarms.waypointHolders(d.id()));
            d.users().setOwnerWaypoint(false);
            MonitorAlarms.refreshWaypoints(t);
            h.assertTrue(MonitorAlarms.waypointHolders(d.id()).equals(Set.of(viewer.getUUID())), "the owner switched his off");
            d.users().setWaypointsAll(false);
            MonitorAlarms.refreshWaypoints(t);
            h.assertTrue(MonitorAlarms.waypointHolders(d.id()).isEmpty(), "off for all: " + MonitorAlarms.waypointHolders(d.id()));
            d.users().setWaypointsAll(true);
            d.users().setOwnerWaypoint(true);
            MonitorAlarms.refreshWaypoints(t);
            h.assertTrue(MonitorAlarms.waypointHolders(d.id()).equals(Set.of(owner.getUUID(), viewer.getUUID())), "on again");
            d.setWaterTemp(25F);
            MonitorSync.cycle(level, m);
            h.assertTrue(!MonitorAlarms.waypointOpen(d.id()), "cleared");
        } finally {
            for (ServerPlayer p : new ServerPlayer[]{owner, viewer, near}) p.level().getServer().getPlayerList().remove(p);
        }
        h.succeed();
    }

    /** features.equipment.tankUsers off: the list is ignored (a listed viewer may change an open tank, a caretaker not a locked one). */
    @GameTest(template = EMPTY, batch = "bettacraft_m53_users_off", timeoutTicks = 60)
    public static void switchOffIgnoresTheList(GameTestHelper h) {
        boolean was = Features.own(Feature.TANK_USERS);
        TankBlockEntity c = AerationGameTests.tank(h, 2, 1, 2, 2, 2, 2);
        TankData t = c.getTank();
        ServerPlayer owner = MockPlayers.serverPlayer(h), care = MockPlayers.serverPlayer(h), view = MockPlayers.serverPlayer(h);
        try {
            t.setOwner(owner.getUUID());
            t.users().put(care.getUUID(), "care", Role.CARETAKER);
            t.users().put(view.getUUID(), "view", Role.VIEWER);
            Features.set(Feature.TANK_USERS, false);
            h.assertTrue(TankAccess.role(view, t) == Role.OTHER && TankAccess.role(care, t) == Role.OTHER && TankAccess.role(owner, t) == Role.OWNER, "only the owner counts");
            t.setLocked(false);
            h.assertTrue(TankInhabitants.canModify(view, t), "the viewer may change an open tank");
            t.setLocked(true);
            h.assertTrue(!TankInhabitants.canModify(care, t) && !TankAccess.canManage(care, t) && TankInhabitants.canModify(owner, t), "locked: only the owner");
            Features.set(Feature.TANK_USERS, true);
            h.assertTrue(TankInhabitants.canModify(care, t) && !TankInhabitants.canModify(view, t), "on again: the list counts");
        } finally {
            Features.set(Feature.TANK_USERS, was);
            for (ServerPlayer p : new ServerPlayer[]{owner, care, view}) p.level().getServer().getPlayerList().remove(p);
        }
        h.succeed();
    }
}
