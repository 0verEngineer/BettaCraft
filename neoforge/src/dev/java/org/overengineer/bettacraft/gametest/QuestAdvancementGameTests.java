package org.overengineer.bettacraft.gametest;

import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.food.FoodDiet;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.network.TankActionPayload;
import org.overengineer.bettacraft.progression.Progress;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.menu.TankActions;
import org.overengineer.bettacraft.tank.menu.TankMenu;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.overengineer.bettacraft.terrarium.TankModeSetting;
import org.overengineer.bettacraft.terrarium.TerrariumMode;

/**
 * M45: the new advancements whose events are a player's action or the state of a tank when its menu opens (the others are
 * checked in the GameTests that make their events happen: births and hatches, the shared filter, the water change
 * controller, the regulator, the crank, the scraper, the big animals, the mobs, the pets, the sea turtle, Create).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class QuestAdvancementGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private QuestAdvancementGameTests() {}

    private static ServerPlayer player(GameTestHelper h, TankBlockEntity c) {
        return QuestAdv.watcher(h, c);
    }

    /** A tankmate and an axolotl put in by a player: "Community Tank" and "Guest from Afar"; a betta gives neither. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void insertingTankmatesAndGuests(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = AerationGameTests.tank(h, 2, 2, 2, 4, 2, 3);
        ServerPlayer sp = player(h, c);
        try {
            BettaEntity b = CommunityFishGameTests.betta(h, c, BettaGenotype.randomWild(net.minecraft.util.RandomSource.create(45)), false);
            h.assertTrue(b != null, "a betta");
            h.assertTrue(!QuestAdv.done(sp, "tankmate_inserted") && !QuestAdv.done(sp, "foreign_animal"), "a betta is no tankmate and no guest");
            var tetra = ModEntities.TANKMATES.get(TankmateSpecies.NEON_TETRA).get();
            Component err = TankInhabitants.insert(level, c, new ItemStack(ModItems.TANKMATE_BUCKETS.get(TankmateSpecies.NEON_TETRA).get()), tetra, sp, false);
            h.assertTrue(err == null, "the tetra goes in: " + (err == null ? "" : err.getString()));
            QuestAdv.check(h, sp, "tankmate_inserted");
            h.assertTrue(!QuestAdv.done(sp, "foreign_animal"), "no guest yet");
            err = TankInhabitants.insert(level, c, new ItemStack(Items.AXOLOTL_BUCKET), EntityType.AXOLOTL, sp, false);
            h.assertTrue(err == null, "the axolotl goes in: " + (err == null ? "" : err.getString()));
            QuestAdv.check(h, sp, "foreign_animal");
            // the classification of the other kinds (their own GameTests put them in)
            Entity cow = EntityType.COW.create(level);
            h.assertTrue(cow != null && Progress.insertEvent(cow, true) != null, "a cow by the size rule counts as a pet or a big animal: " + Progress.insertEvent(cow, true));
            Entity zombie = EntityType.ZOMBIE.create(level);
            h.assertTrue(Progress.MOB_IN_GLASS.equals(Progress.insertEvent(zombie, false)), "a zombie is a mob in glass");
            Entity turtle = EntityType.TURTLE.create(level);
            h.assertTrue(Progress.SEA_TURTLE.equals(Progress.insertEvent(turtle, true)), "a sea turtle has its own");
            h.succeed();
        } finally {
            QuestAdv.gone(h, sp);
        }
    }

    /** The menu of a paludarium, of a tank with a fish on a varied diet: their advancements; an aquarium of plain pellets: none. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void menuOfAPaludariumAndOfAVariedDiet(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = AerationGameTests.tank(h, 2, 2, 2, 3, 2, 3);
        ServerPlayer sp = player(h, c);
        try {
            BettaEntity b = CommunityFishGameTests.betta(h, c, BettaGenotype.randomWild(net.minecraft.util.RandomSource.create(45)), false);
            b.health().ate(0);
            Progress.onMenuOpened(sp, c);
            h.assertTrue(!QuestAdv.done(sp, "paludarium") && !QuestAdv.done(sp, "varied_diet"), "an aquarium of one food: nothing");
            int need = FoodDiet.params().groups();
            for (int g = 0; g < need; g++) b.health().ate(g);
            Progress.onMenuOpened(sp, c);
            QuestAdv.check(h, sp, "varied_diet");
            c.getTank().setTankMode(TankModeSetting.PALUDARIUM);
            h.assertTrue(TankClimate.refreshMode(level, c) == TerrariumMode.PALUDARIUM, "a paludarium now");
            Progress.onMenuOpened(sp, c);
            QuestAdv.check(h, sp, "paludarium");
            h.succeed();
        } finally {
            QuestAdv.gone(h, sp);
        }
    }

    /** The daylight timer switched on in the menu: "Sunrise, Sunset" (switching it off again gives nothing new). */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void lightTimerFromTheMenu(GameTestHelper h) {
        TankBlockEntity c = AerationGameTests.tank(h, 2, 2, 2, 2, 2, 2);
        ServerPlayer sp = player(h, c);
        try {
            TankMenu menu = new TankMenu(4545, sp.getInventory(), c);
            sp.containerMenu = menu;
            boolean before = c.getTank().lightTimer();
            if (before) TankActions.handle(sp, new TankActionPayload(menu.containerId, TankActions.TOGGLE_TIMER, 0, "", Optional.empty()));
            h.assertTrue(!QuestAdv.done(sp, "light_timer"), "switching it off gives nothing");
            TankActions.handle(sp, new TankActionPayload(menu.containerId, TankActions.TOGGLE_TIMER, 0, "", Optional.empty()));
            h.assertTrue(c.getTank().lightTimer(), "the timer is on");
            QuestAdv.check(h, sp, "light_timer");
            h.succeed();
        } finally {
            QuestAdv.gone(h, sp);
        }
    }

    /** "Plugged In": only while a device needs power and the tank has FE (config changed: own batch, restored). */
    @GameTest(template = EMPTY, batch = "bettacraft_m45_power", timeoutTicks = 100)
    public static void pluggedInNeedsAPowerOption(GameTestHelper h) {
        boolean heater = ServerConfig.POWER_HEATER.get();
        TankBlockEntity c = AerationGameTests.tank(h, 2, 2, 2, 2, 2, 2);
        ServerPlayer sp = player(h, c);
        try {
            ServerConfig.POWER_HEATER.set(false);
            c.getTank().setPowerBuffer(1000);
            Progress.onMenuOpened(sp, c);
            h.assertTrue(!QuestAdv.done(sp, "powered"), "no power option on: nothing");
            ServerConfig.POWER_HEATER.set(true);
            c.getTank().setPowerBuffer(0);
            Progress.onMenuOpened(sp, c);
            h.assertTrue(!QuestAdv.done(sp, "powered"), "an empty buffer: nothing");
            c.getTank().setPowerBuffer(1000);
            Progress.onMenuOpened(sp, c);
            QuestAdv.check(h, sp, "powered");
            h.succeed();
        } finally {
            ServerConfig.POWER_HEATER.set(heater);
            QuestAdv.gone(h, sp);
        }
    }
}
