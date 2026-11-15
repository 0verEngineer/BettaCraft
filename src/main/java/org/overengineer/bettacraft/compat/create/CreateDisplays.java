package org.overengineer.bettacraft.compat.create;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.monitor.MonitorBlockEntity;
import org.overengineer.bettacraft.monitor.MonitorTank;
import org.overengineer.bettacraft.monitor.MonitorValue;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M32 part c: our blocks as sources of Create's Display Link (tank values on display boards, nixie tubes, signs, lecterns).
 * <ul>
 *     <li><b>Tank values</b> (any tank block): one line per value of the tank (temperature, water quality, pH, ... as the
 *     monitor shows them), as many as the target has rows, in the order of {@link #ORDER}.</li>
 *     <li><b>Monitored tanks</b> (any monitor block): one line per tank of the monitor: its name, temperature and water
 *     quality, or "not loaded".</li>
 * </ul>
 * Create's {@code DisplaySource} is an abstract class, so this one class is compiled against Create's API (compileOnly,
 * never in our jar) and only loaded when Create is there ({@link #register} from the mod constructor behind a
 * {@code ModList} check). {@code features.createDisplayLinks} off: the sources give no lines.
 */
public final class CreateDisplays {
    private static final DeferredRegister<DisplaySource> SOURCES = DeferredRegister.create(CreateRegistries.DISPLAY_SOURCE, BettaCraft.MOD_ID);
    public static final DeferredHolder<DisplaySource, TankValues> TANK_VALUES = SOURCES.register("tank_values", TankValues::new);
    public static final DeferredHolder<DisplaySource, MonitoredTanks> MONITORED_TANKS = SOURCES.register("monitored_tanks", MonitoredTanks::new);

    /** The values on a display, most useful first (a sign shows the first four). */
    public static final MonitorValue[] ORDER = {MonitorValue.TEMPERATURE, MonitorValue.WATER_QUALITY, MonitorValue.PH, MonitorValue.TDS,
            MonitorValue.OXYGEN, MonitorValue.CO2, MonitorValue.NITRATE, MonitorValue.FILL, MonitorValue.STOCKING, MonitorValue.FOOD,
            MonitorValue.SALINITY, MonitorValue.KH, MonitorValue.CALCIUM, MonitorValue.MAGNESIUM, MonitorValue.ALGAE,
            MonitorValue.AIR_TEMPERATURE, MonitorValue.HUMIDITY, MonitorValue.UV};

    private CreateDisplays() {}

    public static void register(IEventBus modBus) {
        SOURCES.register(modBus);
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent e) -> e.enqueueWork(() -> {
            DisplaySource.BY_BLOCK_ENTITY.add(ModBlockEntities.TANK.get(), TANK_VALUES.get());
            DisplaySource.BY_BLOCK_ENTITY.add(ModBlockEntities.MONITOR.get(), MONITORED_TANKS.get());
        }));
    }

    private static MutableComponent line(MonitorValue v, float x, int columns) {
        MutableComponent l = Component.translatable(v.translationKey()).append(": " + v.format(x));
        // a narrow target (a sign) cuts the end of a line: there the value comes first
        if (columns > 0 && l.getString().length() > columns) l = Component.literal(v.format(x) + " ").append(Component.translatable(v.translationKey()));
        return l;
    }

    /** The lines of the tank the block at the source belongs to (empty without a tank or with the switch off). */
    public static List<MutableComponent> tankLines(ServerLevel level, net.minecraft.core.BlockPos pos, int max, int columns) {
        List<MutableComponent> out = new ArrayList<>();
        if (!CommonConfig.createDisplayLinks()) return out;
        TankBlockEntity c = TankMultiblock.getController(level, pos);
        if (c == null || c.getTank() == null) return out;
        for (MonitorValue v : ORDER) {
            if (out.size() >= max) break;
            float x = v.sample(level, c);
            if (Float.isFinite(x)) out.add(line(v, x, columns));
        }
        return out;
    }

    /** One line per tank of the monitor at the source. */
    public static List<MutableComponent> monitorLines(BlockEntity be, int max) {
        List<MutableComponent> out = new ArrayList<>();
        if (!CommonConfig.createDisplayLinks() || !(be instanceof MonitorBlockEntity m)) return out;
        MonitorBlockEntity screen = m.controller();
        if (screen == null) return out;
        for (MonitorTank t : screen.tanks()) {
            if (out.size() >= max) break;
            MutableComponent l = Component.literal(t.name.isEmpty() ? "?" : t.name).append(": ");
            if (!t.live) l.append(Component.translatable("display.bettacraft.not_loaded"));
            else l.append(MonitorValue.TEMPERATURE.format(t.value(MonitorValue.TEMPERATURE)) + ", ")
                    .append(Component.translatable(MonitorValue.WATER_QUALITY.translationKey()))
                    .append(" " + MonitorValue.WATER_QUALITY.format(t.value(MonitorValue.WATER_QUALITY)));
            out.add(l);
        }
        return out;
    }

    private static int rows(DisplayTargetStats stats) {
        return Math.max(1, stats.maxRows());
    }

    public static final class TankValues extends DisplaySource {
        @Override
        public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
            if (!(context.level() instanceof ServerLevel level)) return EMPTY;
            List<MutableComponent> l = tankLines(level, context.getSourcePos(), rows(stats), stats.maxColumns());
            return l.isEmpty() ? EMPTY : l;
        }
    }

    public static final class MonitoredTanks extends DisplaySource {
        @Override
        public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
            List<MutableComponent> l = monitorLines(context.getSourceBlockEntity(), rows(stats));
            return l.isEmpty() ? EMPTY : l;
        }
    }
}
