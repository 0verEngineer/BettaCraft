package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.api.BettacraftApi;
import org.overengineer.bettacraft.api.event.AnimalBornEvent;
import org.overengineer.bettacraft.api.event.AnimalDiedEvent;
import org.overengineer.bettacraft.api.event.TankAlarmEvent;
import org.overengineer.bettacraft.api.event.TankEvent;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.foreign.ForeignBreeding;
import org.overengineer.bettacraft.foreign.ForeignCare;
import org.overengineer.bettacraft.foreign.ForeignProfiles;
import org.overengineer.bettacraft.monitor.DashboardLayout;
import org.overengineer.bettacraft.monitor.DashboardTile;
import org.overengineer.bettacraft.monitor.MonitorAlarms;
import org.overengineer.bettacraft.monitor.MonitorBlock;
import org.overengineer.bettacraft.monitor.MonitorBlockEntity;
import org.overengineer.bettacraft.monitor.MonitorSync;
import org.overengineer.bettacraft.monitor.MonitorValue;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/**
 * M32 part d: the public NeoForge events for modpack scripts (KubeJS) and other mods, caught by a listener on
 * {@code NeoForge.EVENT_BUS} like a script would: young born (an animal of another mod by its profile), an animal dying of
 * our care and by damage (with the cause and the entity), a monitor alarm going off and clearing (and its map waypoint
 * opening for a player nearby and closing), the script helpers of {@link BettacraftApi}, and the switch
 * {@code features.compat.modpackEvents} off. Own batches; the config is restored in {@code finally}.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ModpackEventGameTests {
    private static final String EMPTY = "empty_32x10x32";
    /** What the listener caught (the GameTests of one batch run one after another on the server thread). */
    private static final List<TankEvent> CAUGHT = new ArrayList<>();
    private static boolean registered;

    private ModpackEventGameTests() {}

    private static void listen() {
        if (!registered) {
            registered = true;
            NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, AnimalBornEvent.class, CAUGHT::add);
            NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, AnimalDiedEvent.class, CAUGHT::add);
            NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TankAlarmEvent.class, CAUGHT::add);
        }
        CAUGHT.clear();
    }

    private static <T extends TankEvent> List<T> caught(Class<T> type) {
        List<T> out = new ArrayList<>();
        for (TankEvent e : CAUGHT) if (type.isInstance(e)) out.add(type.cast(e));
        return out;
    }

    /** A brood of a foreign animal with a profile, its death of hunger and a death by damage. */
    @GameTest(template = EMPTY, batch = "bettacraft_m32_events", timeoutTicks = 200)
    public static void bornAndDiedEventsReachTheListener(GameTestHelper h) {
        listen();
        ForeignGameTests.withProfile(ForeignGameTests.TROPICAL, () -> {
            ServerLevel level = h.getLevel();
            TankBlockEntity c = ForeignGameTests.tank(h, 2, 2, 2, 2, 2, 25F);
            c.getTank().setName("Gametest tank");
            LivingEntity a = ForeignGameTests.add(h, c, EntityType.TROPICAL_FISH, t -> {});
            ForeignGameTests.add(h, c, EntityType.TROPICAL_FISH, t -> {});
            ForeignBreeding.step(level, c, 0.01);
            ForeignBreeding.step(level, c, 1.1);
            List<AnimalBornEvent> born = caught(AnimalBornEvent.class);
            h.assertTrue(born.size() == 1, "one brood: " + born.size());
            AnimalBornEvent b = born.getFirst();
            h.assertTrue(b.getSpecies().equals(ResourceLocation.parse("minecraft:tropical_fish")) && b.getCount() == 3, "3 tropical fish: " + b.getSpecies() + " " + b.getCount());
            h.assertTrue(b.isForeign() && !b.isHatched() && !b.isCritter(), "a live birth of a foreign animal");
            h.assertTrue(b.getTankId().equals(c.getTank().id()) && b.getTankPos().equals(c.getBlockPos()) && "Gametest tank".equals(b.getTankName()),
                    "names the tank");
            // dies of our care: hunger
            CAUGHT.clear();
            var p = ForeignProfiles.cared(a);
            for (int i = 0; i < 60 && a.isAlive(); i++) ForeignCare.advance(level, c, a, p, 1.0);
            List<AnimalDiedEvent> died = caught(AnimalDiedEvent.class);
            h.assertTrue(died.size() == 1 && "hunger".equals(died.getFirst().getCause()), "died of hunger: " + died.size()
                    + (died.isEmpty() ? "" : " " + died.getFirst().getCause()));
            h.assertTrue(died.getFirst().getEntity() == a && died.getFirst().getKiller() == null && died.getFirst().getCount() == 1, "the animal itself");
            // dies by damage
            CAUGHT.clear();
            LivingEntity other = (LivingEntity) org.overengineer.bettacraft.tank.TankInhabitants.liveInhabitants(level, c.getTank()).stream()
                    .filter(e -> e != a && e.isAlive()).findFirst().orElseThrow(() -> new GameTestAssertException("no other fish"));
            other.hurt(level.damageSources().generic(), 1000F);
            died = caught(AnimalDiedEvent.class);
            h.assertTrue(died.size() == 1 && "injuries".equals(died.getFirst().getCause()) && died.getFirst().getEntity() == other,
                    "killed: " + died.size() + (died.isEmpty() ? "" : " " + died.getFirst().getCause()));
            // the script helpers
            BlockPos any = c.getTank().max();
            h.assertTrue(BettacraftApi.isTank(level, any) && c.getTank().id().equals(BettacraftApi.tankId(level, any)), "a tank from any of its blocks");
            h.assertTrue(Math.abs(BettacraftApi.tankValues(level, any).get("temperature") - 25F) < 0.5F, "values: " + BettacraftApi.tankValues(level, any));
            h.assertTrue(!BettacraftApi.isTank(level, h.absolutePos(new BlockPos(20, 2, 20))) && BettacraftApi.tankValues(level, h.absolutePos(new BlockPos(20, 2, 20))).isEmpty(),
                    "no tank");
        });
        h.succeed();
    }

    /** A monitor alarm: the event when it goes off and when it clears; the waypoint for the tank's owner. */
    @GameTest(template = EMPTY, batch = "bettacraft_m32_events", timeoutTicks = 100)
    public static void alarmEventsAndWaypoints(GameTestHelper h) {
        listen();
        ServerLevel level = h.getLevel();
        TankBlockEntity t = AerationGameTests.tank(h, 2, 1, 2, 2, 2, 2);
        t.getTank().setWaterTemp(30F);
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        t.getTank().setOwner(sp.getUUID()); // M53: the waypoint goes to the owner and the users only
        try {
            Vec3 near = h.absoluteVec(new Vec3(4, 3, 8));
            sp.teleportTo(level, near.x, near.y, near.z, 0, 0);
            BlockPos tl = new BlockPos(9, 2, 8);
            BlockState s = ModBlocks.MONITOR.get().defaultBlockState().setValue(MonitorBlock.FACING, Direction.NORTH);
            h.setBlock(tl, s);
            h.setBlock(tl.west(), s);
            MonitorBlockEntity c = (MonitorBlockEntity) level.getBlockEntity(h.absolutePos(tl));
            DashboardLayout l = new DashboardLayout();
            l.auto = false;
            l.tiles.add(DashboardTile.alarm(l.nextId++, 0, 0, 4, 2, t.getTank().id(), MonitorValue.TEMPERATURE, false, 28F, false));
            l.tiles.add(DashboardTile.alarm(l.nextId++, 4, 0, 4, 2, t.getTank().id(), MonitorValue.TEMPERATURE, false, 29F, false));
            c.setLayout(l);
            int sent = MonitorAlarms.sentWaypoints;
            MonitorSync.cycle(level, c); // nobody looks: alarm tiles watch their tanks anyway
            List<TankAlarmEvent> alarms = caught(TankAlarmEvent.class);
            h.assertTrue(alarms.size() == 2 && alarms.stream().allMatch(TankAlarmEvent::isActive), "two alarms went off: " + alarms.size());
            TankAlarmEvent a = alarms.getFirst();
            h.assertTrue("temperature".equals(a.getValue()) && Math.abs(a.getCurrent() - 30F) < 0.5F && !a.isBelow()
                    && a.getTankId().equals(t.getTank().id()) && a.getMonitorPos().equals(c.getBlockPos()), "the value, tank and monitor");
            h.assertTrue(MonitorAlarms.waypointOpen(t.getTank().id()), "a waypoint at the tank");
            // M53: only the owner (and listed users with their switch on) get it, never a player who is merely near
            long inRange = 1;
            h.assertTrue(MonitorAlarms.sentWaypoints == sent + inRange, "one waypoint for the owner: " + (MonitorAlarms.sentWaypoints - sent));
            CAUGHT.clear();
            MonitorSync.cycle(level, c);
            h.assertTrue(CAUGHT.isEmpty(), "no change, no event");
            t.getTank().setWaterTemp(28.5F); // the 29 alarm clears, the 28 one stays
            MonitorSync.cycle(level, c);
            alarms = caught(TankAlarmEvent.class);
            h.assertTrue(alarms.size() == 1 && !alarms.getFirst().isActive() && alarms.getFirst().getLimit() == 29F, "the 29 alarm cleared");
            h.assertTrue(MonitorAlarms.waypointOpen(t.getTank().id()), "the other alarm keeps the waypoint");
            CAUGHT.clear();
            t.getTank().setWaterTemp(25F);
            MonitorSync.cycle(level, c);
            h.assertTrue(caught(TankAlarmEvent.class).size() == 1, "the 28 alarm cleared");
            h.assertTrue(!MonitorAlarms.waypointOpen(t.getTank().id()) && MonitorAlarms.sentWaypoints == sent + 2 * inRange, "the waypoint is removed");
            // the monitor broken while an alarm is on: cleared
            t.getTank().setWaterTemp(30F);
            MonitorSync.cycle(level, c);
            CAUGHT.clear();
            h.setBlock(tl, net.minecraft.world.level.block.Blocks.AIR);
            h.assertTrue(caught(TankAlarmEvent.class).size() == 2 && caught(TankAlarmEvent.class).stream().noneMatch(TankAlarmEvent::isActive),
                    "broken: both cleared (" + caught(TankAlarmEvent.class).size() + ")");
            h.assertTrue(!MonitorAlarms.waypointOpen(t.getTank().id()), "no waypoint left");
        } finally {
            sp.getServer().getPlayerList().remove(sp);
        }
        h.succeed();
    }

    /** features.compat.modpackEvents off: nothing is posted; features.compat.journeymap off: the alarm event, but no waypoint. */
    @GameTest(template = EMPTY, batch = "bettacraft_m32_events_off", timeoutTicks = 100)
    public static void switchesTurnEventsAndWaypointsOff(GameTestHelper h) {
        listen();
        boolean events = Features.own(Feature.MODPACK_EVENTS), waypoints = Features.own(Feature.ALARM_WAYPOINTS);
        try {
            ServerLevel level = h.getLevel();
            TankBlockEntity t = AerationGameTests.tank(h, 2, 1, 2, 2, 2, 2);
            t.getTank().setWaterTemp(30F);
            BlockPos tl = new BlockPos(9, 2, 8);
            h.setBlock(tl, ModBlocks.MONITOR.get().defaultBlockState().setValue(MonitorBlock.FACING, Direction.NORTH));
            MonitorBlockEntity c = (MonitorBlockEntity) level.getBlockEntity(h.absolutePos(tl));
            DashboardLayout l = new DashboardLayout();
            l.auto = false;
            l.tiles.add(DashboardTile.alarm(l.nextId++, 0, 0, 4, 2, t.getTank().id(), MonitorValue.TEMPERATURE, false, 28F, false));
            c.setLayout(l);
            Features.set(Feature.ALARM_WAYPOINTS, false);
            MonitorSync.cycle(level, c);
            h.assertTrue(caught(TankAlarmEvent.class).size() == 1, "the event still comes");
            h.assertTrue(!MonitorAlarms.waypointOpen(t.getTank().id()), "but no waypoint");
            t.getTank().setWaterTemp(25F);
            MonitorSync.cycle(level, c);
            Features.set(Feature.MODPACK_EVENTS, false);
            CAUGHT.clear();
            t.getTank().setWaterTemp(30F);
            MonitorSync.cycle(level, c);
            h.assertTrue(CAUGHT.isEmpty(), "events off: nothing posted (" + CAUGHT.size() + ")");
        } finally {
            Features.set(Feature.MODPACK_EVENTS, events);
            Features.set(Feature.ALARM_WAYPOINTS, waypoints);
        }
        h.succeed();
    }
}
