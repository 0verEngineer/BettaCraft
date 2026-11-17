package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.genetics.BettaSpecies;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankMoves;
import org.overengineer.bettacraft.tank.TravelCare;

/**
 * M50 (A1, C1), the data simulation of a tank on a Create contraption without Create: a stocked tank is packed like Create
 * does at the start of a trip and its block entity data advanced by {@link TravelCare#step} for game days, once with power
 * (the batteries give what is asked), once without (an empty buffer, no batteries). With power the heater holds the target
 * and the filter the water; without, the water goes to the cold air and gets dirty. The bettas get hungry either way; on
 * the road nobody dies, also not after weeks.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TravelCareGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private TravelCareGameTests() {}

    /** The block entity data of a stocked 2x2x2 tank with its two bettas packed (as at Create's start of a move). */
    private static CompoundTag packed(GameTestHelper h, int x, int z) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = CritterGameTests.tank(h, x, 1, z, 2, 2, 2, 8000L);
        TankData t = c.getTank();
        t.setWaterQuality(1F);
        t.setTargetTemp(27F);
        t.setWaterTemp(27F);
        t.setPowerBuffer(0);
        RandomSource r = RandomSource.create(50);
        CommunityFishGameTests.betta(h, c, BettaGenotype.randomWild(BettaSpecies.SPLENDENS, r), false);
        CommunityFishGameTests.betta(h, c, BettaGenotype.randomWild(BettaSpecies.SPLENDENS, r), true);
        List<Entity> out = new ArrayList<>();
        ListTag stowed = TankMoves.pack(level, t, out);
        if (stowed.size() != 2) throw new GameTestAssertException("two bettas packed: " + stowed.size());
        for (Entity e : out) e.discard();
        CompoundTag nbt = c.saveWithFullMetadata(level.registryAccess());
        nbt.put("stowed", stowed);
        return nbt;
    }

    private static TravelCare.Env env(boolean battery) {
        // 15 °C outside, a heater for 8 water blocks and an external filter travel along
        return new TravelCare.Env(15F, 8, 0, 32, 1, 0, 0, fe -> battery ? fe : 0);
    }

    private static double sinceFed(CompoundTag nbt, int i) {
        return nbt.getList("stowed", Tag.TAG_COMPOUND).getCompound(i).getCompound(BettaData.KEY).getCompound("health").getDouble("days_since_fed");
    }

    @GameTest(template = EMPTY, batch = "bettacraft_m50_travel", timeoutTicks = 100)
    public static void travelCareWithAndWithoutPower(GameTestHelper h) {
        boolean heater = ServerConfig.POWER_HEATER.get(), filter = ServerConfig.POWER_FILTER.get(), power = Features.own(Feature.POWER);
        try {
            ServerConfig.POWER_HEATER.set(true);
            ServerConfig.POWER_FILTER.set(true);
            Features.set(Feature.POWER, true);
            ServerLevel level = h.getLevel();
            CompoundTag on = packed(h, 2, 2), off = packed(h, 10, 2);
            TravelCare.Result ron = null, roff = null;
            for (int i = 0; i < 24; i++) { // one day in hours
                ron = TravelCare.step(level, on, env(true), 1000);
                roff = TravelCare.step(level, off, env(false), 1000);
            }
            TankData ton = TankData.load(on.getCompound("tank")), toff = TankData.load(off.getCompound("tank"));
            h.assertTrue(ron != null && ron.filterPowered() && ron.heatPowered() && ron.fromBattery() > 0, "with power the devices run: " + ron);
            h.assertTrue(roff != null && !roff.filterPowered() && !roff.heatPowered(), "without power they stand: " + roff);
            h.assertTrue(ton.waterTemp() > 25F, "the heater holds the water warm: " + ton.waterTemp());
            h.assertTrue(toff.waterTemp() < 17F, "without power the water goes to the cold air: " + toff.waterTemp());
            h.assertTrue(toff.waterQuality() < ton.waterQuality(), "without the filter the water gets dirtier: " + toff.waterQuality() + " / " + ton.waterQuality());
            for (int i = 0; i < 2; i++) {
                h.assertTrue(Math.abs(sinceFed(on, i) - 1.0) < 0.05 && Math.abs(sinceFed(off, i) - 1.0) < 0.05, "a day without food on the road: " + sinceFed(on, i));
                h.assertTrue(on.getList("stowed", Tag.TAG_COMPOUND).getCompound(i).getLong("last_sim") == level.getGameTime(), "the trip is not simulated twice");
            }
            CompoundTag trip = off.getCompound(TravelCare.TRIP_KEY);
            h.assertTrue(trip.getLong("ticks") == 24000 && trip.getLong("unpowered") == 24000, "the trip summary: " + trip);
            h.assertTrue(on.getCompound(TravelCare.TRIP_KEY).getLong("powered") == 24000, "the powered trip: " + on.getCompound(TravelCare.TRIP_KEY));
            // weeks on the road without power or food: sick, but nobody dies before the arrival
            for (int i = 0; i < 40; i++) TravelCare.step(level, off, env(false), 24000);
            ListTag st = off.getList("stowed", Tag.TAG_COMPOUND);
            h.assertTrue(st.size() == 2, "both bettas still packed");
            for (int i = 0; i < 2; i++) {
                CompoundTag care = st.getCompound(i).getCompound(TravelCare.CARE_KEY);
                h.assertTrue(care.getBoolean("sick") && care.getFloat("hp") > 0F, "sick but alive: " + care);
                h.assertTrue(TravelCare.satiation(st.getCompound(i)) < 0.01F, "starving: " + care);
            }
        } finally {
            ServerConfig.POWER_HEATER.set(heater);
            ServerConfig.POWER_FILTER.set(filter);
            Features.set(Feature.POWER, power);
        }
        h.succeed();
    }

    /** Without the power options the devices that travel along simply run (as on the ground), nothing is taken from batteries. */
    @GameTest(template = EMPTY, batch = "bettacraft_m50_travel", timeoutTicks = 100)
    public static void travelCareWithoutPowerOptions(GameTestHelper h) {
        boolean heater = ServerConfig.POWER_HEATER.get(), filter = ServerConfig.POWER_FILTER.get();
        try {
            ServerConfig.POWER_HEATER.set(false);
            ServerConfig.POWER_FILTER.set(false);
            CompoundTag nbt = packed(h, 2, 12);
            TravelCare.Result r = null;
            for (int i = 0; i < 24; i++) r = TravelCare.step(h.getLevel(), nbt, env(false), 1000);
            h.assertTrue(r != null && r.filterPowered() && r.heatPowered() && r.demand() == 0 && r.fromBattery() == 0, "no FE needed: " + r);
            h.assertTrue(TankData.load(nbt.getCompound("tank")).waterTemp() > 25F, "the heater runs");
        } finally {
            ServerConfig.POWER_HEATER.set(heater);
            ServerConfig.POWER_FILTER.set(filter);
        }
        h.succeed();
    }
}
