package org.overengineer.bettacraft.monitor;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import org.overengineer.bettacraft.equipment.FeederBlockEntity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;

/**
 * M27: the values a monitor shows of a tank (value tiles, graphs). Each is a float sampled on the server from the tank's
 * controller; {@link Float#NaN} means "not there" (no feeder, oxygen not simulated yet). The ids are stable: layouts and
 * the saved history ({@link TankHistory}) refer to them by id, so new values only go at the end and old ones never change
 * their id.
 */
public enum MonitorValue {
    TEMPERATURE("temperature", "°C", 1),
    WATER_QUALITY("water_quality", "%", 0),
    PH("ph", "", 1),
    TDS("tds", "ppm", 0),
    SALINITY("salinity", "ppt", 1),
    CALCIUM("calcium", "mg/l", 0),
    OXYGEN("oxygen", "mg/l", 1),
    CO2("co2", "mg/l", 0),
    NITRATE("nitrate", "mg/l", 0),
    FILL("fill", "%", 0),
    STOCKING("stocking", "", 0),
    ALGAE("algae", "%", 0),
    FOOD("food", "", 0);

    public static final MonitorValue[] ALL = values();

    public final String id;
    public final String unit;
    /** Decimals the value is shown with. */
    public final int decimals;

    MonitorValue(String id, String unit, int decimals) {
        this.id = id;
        this.unit = unit;
        this.decimals = decimals;
    }

    public String translationKey() {
        return "monitor.bettacraft.value." + id;
    }

    @Nullable
    public static MonitorValue byId(String id) {
        for (MonitorValue v : ALL) if (v.id.equals(id)) return v;
        return null;
    }

    /** The value as text with its unit ("24.5 °C", "--" when it is not there). */
    public String format(float v) {
        if (!Float.isFinite(v)) return "--";
        String n = decimals == 0 ? Integer.toString(Math.round(v)) : String.format(Locale.ROOT, "%." + decimals + "f", v);
        return unit.isEmpty() ? n : n + (unit.equals("%") ? "" : " ") + unit;
    }

    /** The value of the tank now (server). */
    public float sample(ServerLevel level, TankBlockEntity controller) {
        TankData t = controller.getTank();
        if (t == null) return Float.NaN;
        return switch (this) {
            case TEMPERATURE -> t.waterTemp();
            case WATER_QUALITY -> t.waterQuality() * 100F;
            case PH -> t.ph();
            case TDS -> t.tds();
            case SALINITY -> t.salinity();
            case CALCIUM -> t.calcium();
            case OXYGEN -> t.oxygen();
            case CO2 -> t.co2();
            case NITRATE -> t.nitrate();
            case FILL -> t.capacityMb() <= 0 ? 0F : t.waterMb() * 100F / t.capacityMb();
            case STOCKING -> t.inhabitants().size() + t.critters().size();
            case ALGAE -> (float) (org.overengineer.bettacraft.tank.algae.TankAlgae.mean(t) * 100.0);
            case FOOD -> {
                FeederBlockEntity f = TankCare.findFeeder(level, t);
                yield f == null ? Float.NaN : f.foodCount();
            }
        };
    }

    /** Every value of the tank, by ordinal. */
    public static float[] sampleAll(ServerLevel level, TankBlockEntity controller) {
        float[] out = new float[ALL.length];
        for (MonitorValue v : ALL) out[v.ordinal()] = v.sample(level, controller);
        return out;
    }
}
