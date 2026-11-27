package org.overengineer.bettacraft.monitor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.FeederBlockEntity;
import org.overengineer.bettacraft.tank.BubbleNest;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankBreeding;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankNitrate;
import org.overengineer.bettacraft.tank.TankPower;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.tank.WaterQuality;

/**
 * M27 part b: what the status, animal, device and log tiles show of a tank, beyond the plain values. Built on the server
 * ({@link #build}, only while a tile of the screen needs it and at most every {@link MonitorSync#DISCOVER_EVERY} cycles),
 * sent with the tank ({@link MonitorTank#detail}) as a small NBT tag:
 * <ul>
 * <li>{@code warn}: the warnings of the tank's menu ({@code k} translation key, {@code a} arguments, {@code s} 1 = warning,
 * 2 = alarm), the worst first.</li>
 * <li>{@code animals}: per species {@code k} (translation key), {@code n} count, {@code y} young; {@code nest} (phase or
 * empty), {@code eggs}, {@code young} (young fish and small animals, fry in the breeding box), {@code berried}.</li>
 * <li>{@code dev}: per kind of device {@code k}, {@code n} count, {@code on} running, {@code p} power level in % (-1 none),
 * {@code pw} false = lacks power; {@code e} / {@code ec} / {@code ed}: the tank's energy buffer, its capacity and the demand
 * per tick ({@code ec} 0 = no power option on).</li>
 * <li>{@code log}: the latest {@link #LOG_LINES} logbook entries ({@code t}, {@code k}, {@code a}), newest last.</li>
 * </ul>
 */
public final class MonitorDetail {
    public static final int LOG_LINES = 8;
    public static final int WARNING = 1, ALARM = 2;

    private MonitorDetail() {}

    public static CompoundTag build(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        CompoundTag out = new CompoundTag();
        List<Entity> live = TankInhabitants.liveInhabitants(level, t);
        // ---- warnings (like the menu)
        List<CompoundTag> warn = new ArrayList<>();
        long minInsert = Math.round(org.overengineer.bettacraft.config.ServerConfig.getOr(
                org.overengineer.bettacraft.config.ServerConfig.TANK_MIN_FILL_TO_INSERT, 0.5) * t.capacityMb());
        if (t.waterMb() < minInsert) warn.add(w("monitor.bettacraft.warn.low_water", WARNING));
        if (TankWater.qualityEnabled()) {
            WaterQuality.State st = WaterQuality.state(t.waterQuality(), TankWater.params());
            if (st == WaterQuality.State.BAD) warn.add(w("monitor.bettacraft.warn.water_bad", ALARM, Math.round(t.waterQuality() * 100)));
            else if (st == WaterQuality.State.POOR) warn.add(w("monitor.bettacraft.warn.water_poor", WARNING, Math.round(t.waterQuality() * 100)));
            if (TankWater.filterBlocks(level, c) <= 0 && TankWater.internalFilters(level, t) == 0) warn.add(w("monitor.bettacraft.warn.no_filter", WARNING));
        }
        if (TankNitrate.intervalDays(TankNitrate.difficulty()) > 0) {
            TankNitrate.State ns = TankNitrate.state(t.nitrate(), TankNitrate.stressFrom(), TankNitrate.lethalFrom());
            if (ns == TankNitrate.State.LETHAL) warn.add(w("monitor.bettacraft.warn.nitrate_lethal", ALARM, Math.round(t.nitrate())));
            else if (ns == TankNitrate.State.HIGH) warn.add(w("monitor.bettacraft.warn.nitrate_high", WARNING, Math.round(t.nitrate())));
        }
        boolean heatOff = TankPower.heaterRequired() && !c.heatPowered(), filterOff = TankPower.filterRequired() && !c.filterPowered(),
                lightOff = TankPower.lightRequired() && !c.lightPowered(), airOff = TankPower.airRequired() && !c.airPowered();
        if (heatOff || filterOff || lightOff || airOff) warn.add(w("monitor.bettacraft.warn.no_power", ALARM));
        if (c.isCrowded()) warn.add(w("monitor.bettacraft.warn.crowded", ALARM));
        int sick = 0;
        for (Entity e : live) {
            if (e instanceof BettaEntity b && b.health().isSick()) sick++;
            else if (e instanceof TankmateEntity m && m.health().isSick()) sick++;
        }
        if (sick > 0) warn.add(w("monitor.bettacraft.warn.sick", WARNING, sick));
        FeederBlockEntity feeder = TankCare.findFeeder(level, t);
        if (feeder != null && feeder.foodCount() <= 0 && !live.isEmpty()) warn.add(w("monitor.bettacraft.warn.feeder_empty", WARNING));
        for (String k : TankBreeding.warnings(level, c)) warn.add(w("gui.bettacraft.breeding.warn." + k, WARNING));
        warn.sort((a, b) -> Integer.compare(b.getIntOr("s", 0), a.getIntOr("s", 0)));
        ListTag wl = new ListTag();
        wl.addAll(warn);
        out.put("warn", wl);
        // ---- animals
        Map<String, int[]> species = new LinkedHashMap<>();
        int young = 0;
        for (Entity e : live) {
            int[] n = species.computeIfAbsent(e.getType().getDescriptionId(), k -> new int[2]);
            n[0]++;
            if (!TankInhabitants.isAdult(e)) {
                n[1]++;
                young++;
            }
        }
        int berried = 0;
        for (Critter k : t.critters()) {
            if (!k.alive()) continue;
            int[] n = species.computeIfAbsent(k.species.translationKey(), x -> new int[2]);
            n[0]++;
            if (!k.adult()) {
                n[1]++;
                young++;
            }
            if (k.berried()) berried++;
        }
        ListTag al = new ListTag();
        species.entrySet().stream().sorted((a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0])).forEach(e -> {
            CompoundTag s = new CompoundTag();
            s.putString("k", e.getKey());
            s.putInt("n", e.getValue()[0]);
            s.putInt("y", e.getValue()[1]);
            al.add(s);
        });
        out.put("animals", al);
        BubbleNest nest = t.nest();
        out.putString("nest", nest == null ? "" : nest.phase().name().toLowerCase(java.util.Locale.ROOT));
        out.putInt("eggs", nest == null ? 0 : nest.eggCount());
        out.putInt("young", young + org.overengineer.bettacraft.tankmates.TankmateBreeding.boxFry(t));
        out.putInt("berried", berried);
        // ---- devices
        ListTag dl = new ListTag();
        int filters = TankEquipment.filters(level, c).size() + (int) org.overengineer.bettacraft.filter.MultiblockFilter.serving(level, c)[0]
                + TankWater.internalFilters(level, t);
        dl.add(dev("filter", filters, filterOff ? 0 : filters, -1, !filterOff));
        int heaters = (int) Math.ceil(TankCare.heaterBlocks(level, t) - 1e-6), coolers = (int) Math.ceil(TankCare.coolerBlocks(level, t) - 1e-6);
        dl.add(dev("heater", heaters, heatOff ? 0 : heaters, -1, !heatOff));
        if (coolers > 0) dl.add(dev("cooler", coolers, heatOff ? 0 : coolers, -1, !heatOff));
        int[] lights = TankCare.lights(level, t);
        dl.add(dev("light", lights[0], lightOff ? 0 : lights[1], t.lightDimmer(), !lightOff));
        if (org.overengineer.bettacraft.co2.TankCo2.enabled()) {
            int co2 = TankEquipment.co2Sources(level, c).size();
            dl.add(dev("co2", co2, c.co2Bubbles > 1e-6 ? co2 : 0, -1, true));
        }
        var pumps = TankEquipment.airPumps(level, c);
        int running = 0;
        for (var p : pumps) if (p.airRunning()) running++;
        dl.add(dev("air_pump", pumps.size(), running, -1, !airOff));
        out.put("dev", dl);
        out.putLong("e", t.powerBuffer());
        out.putLong("ec", TankPower.anyRequired() ? TankPower.capacity(c) : 0);
        out.putInt("ed", c.lastDemand());
        // ---- log
        ListTag lg = new ListTag();
        List<TankData.LogEntry> entries = t.log();
        for (int k = Math.max(0, entries.size() - LOG_LINES); k < entries.size(); k++) {
            TankData.LogEntry e = entries.get(k);
            CompoundTag l = new CompoundTag();
            l.putLong("t", e.time());
            l.putString("k", e.key());
            ListTag a = new ListTag();
            for (String s : e.args()) a.add(StringTag.valueOf(s.length() > 64 ? s.substring(0, 64) : s));
            l.put("a", a);
            lg.add(l);
        }
        out.put("log", lg);
        return out;
    }

    private static CompoundTag w(String key, int severity, Object... args) {
        CompoundTag c = new CompoundTag();
        c.putString("k", key);
        c.putInt("s", severity);
        ListTag a = new ListTag();
        for (Object o : args) a.add(StringTag.valueOf(String.valueOf(o)));
        c.put("a", a);
        return c;
    }

    private static CompoundTag dev(String kind, int n, int on, int power, boolean powered) {
        CompoundTag c = new CompoundTag();
        c.putString("k", kind);
        c.putInt("n", n);
        c.putInt("on", on);
        c.putInt("p", power);
        c.putBoolean("pw", powered);
        return c;
    }

    // ------------------------------------------------------------------ reading (client and tests)

    /** The worst warning of a detail tag: 0 = all fine, {@link #WARNING}, {@link #ALARM}. */
    public static int state(CompoundTag detail) {
        int s = 0;
        ListTag l = detail.getListOrEmpty("warn");
        for (int i = 0; i < l.size(); i++) s = Math.max(s, l.getCompoundOrEmpty(i).getIntOr("s", 0));
        return s;
    }

    /** The arguments of a warning or log entry. */
    public static List<String> args(CompoundTag entry) {
        List<String> out = new ArrayList<>();
        ListTag a = entry.getListOrEmpty("a");
        for (int i = 0; i < a.size(); i++) out.add(a.getStringOr(i, ""));
        return out;
    }
}
