package org.overengineer.bettacraft.gametest.integration;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import static org.overengineer.bettacraft.gametest.integration.IntegrationRig.*;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.api.event.AnimalBornEvent;
import org.overengineer.bettacraft.api.event.AnimalDiedEvent;
import org.overengineer.bettacraft.api.event.TankAlarmEvent;
import org.overengineer.bettacraft.compat.MobCaptures;
import org.overengineer.bettacraft.gametest.PlumbingGameTestsAccess;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;

/**
 * M42 integration suite: the mob capture items of other mods filled by the mods' own code (not a written component) and
 * emptied into a terrarium, and the KubeJS example script of modpack/ handling our events in the real KubeJS.
 */
@PrefixGameTestTemplate(false)
public final class IntegrationModTests {
    private static final String EMPTY = IntegrationSuite.EMPTY;

    private IntegrationModTests() {}

    private static List<Mob> mobs(GameTestHelper h, TankBlockEntity c, EntityType<?> type) {
        return TankInhabitants.liveInhabitants(h.getLevel(), c.getTank()).stream().filter(e -> e.getType() == type).map(e -> (Mob) e).toList();
    }

    private static Spider spider(GameTestHelper h) {
        Spider s = h.spawn(EntityType.SPIDER, new BlockPos(1, 2, 8));
        s.setNoAi(true);
        return s;
    }

    /** Puts the full capture item into the terrarium like a player and checks the spider is in and the item came back empty. */
    private static ItemStack release(GameTestHelper h, TankBlockEntity c, ServerPlayer p, ItemStack full, Item emptyItem) {
        p.setItemInHand(InteractionHand.MAIN_HAND, full);
        var r = TankInhabitants.tryInsert(full, h.getLevel(), c.getBlockPos(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(r.consumesAction(), "the tank takes the full item: " + r);
        h.assertTrue(mobs(h, c, EntityType.SPIDER).size() == 1, "the spider is in the terrarium");
        ItemStack back = p.getMainHandItem();
        h.assertTrue(back.is(emptyItem) && MobCaptures.storedEntity(back) == null, "the item came back empty: " + back);
        return back;
    }

    /** EnderIO: the soul vial catches a spider by its own right-click code; the tank takes it out of the vial. */
    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_captures", timeoutTicks = 100)
    public static void enderioSoulVialCaughtByEnderIoGoesIntoATerrarium(GameTestHelper h) {
        if (IntegrationSuite.lacks(h, "enderio")) return;
        TankBlockEntity c = PlumbingGameTestsAccess.terrarium(h, 4, 2);
        ServerPlayer p = PlumbingGameTestsAccess.player(h);
        p.setGameMode(GameType.SURVIVAL);
        Item vialItem = item("enderio:soul_vial");
        ItemStack vial = new ItemStack(vialItem);
        Spider s = spider(h);
        p.setItemInHand(InteractionHand.MAIN_HAND, vial);
        var caught = vialItem.interactLivingEntity(vial, p, s, InteractionHand.MAIN_HAND);
        h.assertTrue(caught.consumesAction(), "the soul vial took the spider: " + caught);
        ItemStack full = p.getMainHandItem();
        h.assertTrue(TankInhabitants.containedType(full) == EntityType.SPIDER, "we read the spider in EnderIO's vial: " + full.getComponents());
        release(h, c, p, full, vialItem);
        h.succeed();
    }

    /** Industrial Foregoing: the mob imprisonment tool catches a spider by its own capture code; the tank takes it out. */
    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_captures", timeoutTicks = 100)
    public static void imprisonmentToolCaughtByIndustrialForegoingGoesIntoATerrarium(GameTestHelper h) {
        if (IntegrationSuite.lacks(h, "industrialforegoing")) return;
        TankBlockEntity c = PlumbingGameTestsAccess.terrarium(h, 4, 2);
        ServerPlayer p = PlumbingGameTestsAccess.player(h);
        p.setGameMode(GameType.SURVIVAL);
        Item toolItem = item("industrialforegoing:mob_imprisonment_tool");
        ItemStack tool = new ItemStack(toolItem);
        Spider s = spider(h);
        boolean ok = (boolean) call(toolItem, "capture", tool, (LivingEntity) s, p);
        h.assertTrue(ok, "Industrial Foregoing caught the spider");
        h.assertTrue(TankInhabitants.containedType(tool) == EntityType.SPIDER, "we read the spider in the tool: " + tool.getComponents());
        ItemStack back = release(h, c, p, tool, toolItem);
        h.assertTrue(!(boolean) call(toolItem, "containsEntity", back), "Industrial Foregoing sees the returned tool as empty");
        h.succeed();
    }

    /** Collects the log lines of KubeJS's server console (its logger "KubeJS Server") while attached. */
    private static final class KubeLines extends org.apache.logging.log4j.core.appender.AbstractAppender {
        final List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        KubeLines() {
            super("bettacraft-kubejs-probe", null, null, true, org.apache.logging.log4j.core.config.Property.EMPTY_ARRAY);
        }

        @Override
        public void append(org.apache.logging.log4j.core.LogEvent event) {
            if (event.getLoggerName() != null && event.getLoggerName().contains("KubeJS")) {
                lines.add(event.getLevel() + " " + event.getMessage().getFormattedMessage());
            }
        }

        String text() {
            synchronized (lines) {
                return String.join(" | ", lines);
            }
        }
    }

    /**
     * KubeJS: the example script of modpack/kubejs (copied into the run by -Pintegration) handles our three events: the born
     * and died handlers log their lines on KubeJS's server console, the alarm handler (BettacraftApi.tankValues and a broadcast
     * message) runs without a script error.
     */
    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_kubejs", timeoutTicks = 200)
    public static void kubejsExampleScriptHandlesOurEvents(GameTestHelper h) {
        if (IntegrationSuite.lacks(h, "kubejs")) return;
        TankBlockEntity t = tank(h, 2, 2, 2, 2, 2, 2, true);
        String name = "kubejs-probe-" + UUID.randomUUID().toString().substring(0, 8);
        t.getTank().setName(name);
        var level = h.getLevel();
        BlockPos pos = t.getBlockPos();
        UUID id = t.getTank().id();
        var root = (org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager.getRootLogger();
        KubeLines probe = new KubeLines();
        probe.start();
        root.addAppender(probe);
        try {
            NeoForge.EVENT_BUS.post(new AnimalBornEvent(level, pos, id, name, BettaCraft.id("betta"), 3, true, false, false));
            NeoForge.EVENT_BUS.post(new AnimalDiedEvent(level, pos, id, name, BettaCraft.id("betta"), null, 1, "hunger", null));
            NeoForge.EVENT_BUS.post(new TankAlarmEvent(level, pos, id, name, pos.above(3), "temperature", 31.5F, 30.0F, false, true));
        } finally {
            root.removeAppender(probe);
            probe.stop();
        }
        String added = probe.text();
        h.assertTrue(added.contains("3 x bettacraft:betta born in tank " + name), "the born handler logged: " + added);
        h.assertTrue(added.contains("bettacraft:betta died of hunger in tank " + name), "the died handler logged: " + added);
        h.assertTrue(!added.contains("ERROR") && !added.contains("WARN"), "no script error: " + added);
        h.succeed();
    }

    private static boolean fishEggsGoals(net.minecraft.world.entity.Mob m) {
        return java.util.stream.Stream.concat(m.goalSelector.getAvailableGoals().stream(), m.targetSelector.getAvailableGoals().stream())
                .anyMatch(w -> w.getGoal().getClass().getName().startsWith("dev.cephelo.fisheggs."));
    }

    /**
     * Fish Eggs (M42 fix): the mod gives every fish its breeding and food hunting goals (fed fish pair up and lay egg items that
     * hatch fish without our genes). A tropical fish in the world keeps them; our betta never gets them; a tropical fish put
     * into a tank loses them (suppressOwnBreeding) after a care step.
     */
    @GameTest(template = EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_fisheggs", timeoutTicks = 100)
    public static void fishEggsBreedingStaysOutOfOurFish(GameTestHelper h) {
        if (IntegrationSuite.lacks(h, "fisheggs")) return;
        var level = h.getLevel();
        var wild = h.spawn(EntityType.TROPICAL_FISH, new BlockPos(1, 2, 8));
        h.assertTrue(fishEggsGoals(wild), "Fish Eggs gives a tropical fish in the world its goals (else this test checks nothing): "
                + wild.targetSelector.getAvailableGoals().stream().map(w -> w.getGoal().getClass().getName()).toList());
        var betta = h.spawn(org.overengineer.bettacraft.registry.ModEntities.BETTA.get(), new BlockPos(3, 2, 8));
        h.assertTrue(!fishEggsGoals(betta), "our betta has none of Fish Eggs' goals");
        TankBlockEntity t = tank(h, 2, 2, 2, 2, 2, 2, true);
        var err = TankInhabitants.insert(level, t, new ItemStack(net.minecraft.world.item.Items.TROPICAL_FISH_BUCKET), EntityType.TROPICAL_FISH, null, false);
        h.assertTrue(err == null, "the tropical fish goes in: " + (err == null ? "" : err.getString()));
        org.overengineer.bettacraft.foreign.ForeignBreeding.step(level, t, 0.01);
        var inTank = TankInhabitants.liveInhabitants(level, t.getTank()).stream().filter(e -> e.getType() == EntityType.TROPICAL_FISH).findFirst()
                .orElseThrow(() -> new GameTestFailure("no tropical fish in the tank"));
        h.assertTrue(!fishEggsGoals((net.minecraft.world.entity.Mob) inTank), "the tropical fish in the tank lost Fish Eggs' goals");
        h.succeed();
    }
}
