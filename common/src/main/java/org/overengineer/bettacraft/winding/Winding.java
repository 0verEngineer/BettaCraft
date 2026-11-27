package org.overengineer.bettacraft.winding;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.plumbing.DeviceEnergy;
import org.overengineer.bettacraft.registry.ModSounds;
import org.overengineer.bettacraft.tank.TankPower;

/**
 * M37: winding devices with the crank instead of FE.
 * <p>
 * <b>{@code power.costs} OFF</b> (or {@code features.power.enabled} off): only the food maker and the fertilizer maker
 * have a spring (they need it); every other device simply works, without power and without winding.
 * <p>
 * <b>{@code power.costs} EASY or HARD</b>: the food maker and the fertilizer maker run on FE or their spring. With
 * {@code features.power.winding} also the multiblock filter (its pump head), the reverse osmosis unit (its pressure pump),
 * the water change controller and the water pump can be wound. M67: the cabinet under a tank and an external filter
 * attached to one have no spring of their own any more: the crank at them charges the tank's one energy store
 * ({@link org.overengineer.bettacraft.tank.TankStore}, {@code features.power.tankCrank}), which runs every device of the
 * tank. FE always goes first; a spring only runs while FE is short.
 * <p>
 * Capacity: one charge ({@code winding.ticksPerCharge}, one in-game day) of one device's work; the multiblock filter holds
 * six charges ({@code winding.*Charges}, per device). One charge takes {@code winding.clicksPerCharge} clicks (about five
 * seconds of holding the crank), six charges six times as long. Unloaded chunks stand still.
 */
public final class Winding {
    private Winding() {}

    /** The devices with a spring. */
    public enum Device {
        FOOD_MAKER("food_maker"), FERTILIZER_MAKER("fertilizer_maker"), EXTERNAL_FILTER("external_filter"),
        MULTIBLOCK_FILTER("multiblock_filter"), CABINET("cabinet"), OSMOSIS("osmosis"), WATER_CHANGER("water_changer"),
        WATER_PUMP("water_pump");

        private final String id;

        Device(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        /** Charges of the spring ({@code winding.<device>Charges}; the food maker: {@code foodMaking.springTicks}). */
        public double charges() {
            return switch (this) {
                case FOOD_MAKER -> ServerConfig.getOr(ServerConfig.FOOD_MAKER_SPRING_TICKS, 24000) / (double) ticksPerCharge();
                case FERTILIZER_MAKER -> ServerConfig.getOr(ServerConfig.WIND_FERTILIZER_MAKER, 1.0);
                case EXTERNAL_FILTER, CABINET -> 1.0; // M67: they charge the tank's store (only an old world's spring is left)
                case MULTIBLOCK_FILTER -> ServerConfig.getOr(ServerConfig.WIND_MULTIBLOCK_FILTER, 6.0);
                case OSMOSIS -> ServerConfig.getOr(ServerConfig.WIND_OSMOSIS, 1.0);
                case WATER_CHANGER -> ServerConfig.getOr(ServerConfig.WIND_WATER_CHANGER, 1.0);
                case WATER_PUMP -> ServerConfig.getOr(ServerConfig.WIND_WATER_PUMP, 1.0);
            };
        }

        /** Device-ticks a full spring holds. */
        public long capacity() {
            if (this == FOOD_MAKER) return ServerConfig.getOr(ServerConfig.FOOD_MAKER_SPRING_TICKS, 24000);
            return Math.max(1, Math.round(charges() * ticksPerCharge()));
        }

        /** Clicks of the crank from empty to full (the food maker: {@code foodMaking.windsToFull}). */
        public int clicksToFull() {
            if (this == FOOD_MAKER) return ServerConfig.getOr(ServerConfig.FOOD_MAKER_WINDS, 25);
            return Math.max(1, (int) Math.round(charges() * clicksPerCharge()));
        }

        /** Whether the spring may be wound and used now (see the class comment). */
        public boolean windable() {
            return switch (this) {
                case FOOD_MAKER -> Features.enabled(Feature.FOOD_MACHINE) && (!machinePowered() || enabled());
                case FERTILIZER_MAKER -> Features.enabled(Feature.FERTILIZER) && (!machinePowered() || enabled());
                case EXTERNAL_FILTER, CABINET -> org.overengineer.bettacraft.tank.TankStore.crankEnabled() && TankPower.anyRequired(); // M67
                case MULTIBLOCK_FILTER -> enabled() && DeviceEnergy.required() && Features.enabled(Feature.MULTIBLOCK_FILTER);
                case OSMOSIS -> enabled() && machinePowered() && Features.enabled(Feature.OSMOSIS);
                case WATER_CHANGER -> enabled() && DeviceEnergy.required() && Features.enabled(Feature.AUTO_WATER_CHANGE);
                case WATER_PUMP -> enabled() && DeviceEnergy.required() && Features.enabled(Feature.PIPES);
            };
        }

        /** Why it does not take the crank now (a translation key with the device name as argument). */
        public String refusal() {
            boolean off = switch (this) {
                case FOOD_MAKER -> !Features.enabled(Feature.FOOD_MACHINE);
                case FERTILIZER_MAKER -> !Features.enabled(Feature.FERTILIZER);
                case MULTIBLOCK_FILTER -> !Features.enabled(Feature.MULTIBLOCK_FILTER);
                case OSMOSIS -> !Features.enabled(Feature.OSMOSIS);
                case WATER_CHANGER -> !Features.enabled(Feature.AUTO_WATER_CHANGE);
                case WATER_PUMP -> !Features.enabled(Feature.PIPES);
                default -> false;
            };
            if (off) return "message.bettacraft.crank.off";
            if (this == FOOD_MAKER || this == FERTILIZER_MAKER) return "message.bettacraft.crank.fe_only";
            return needsPower() ? "message.bettacraft.crank.fe_only" : "message.bettacraft.crank.no_need";
        }

        /** Whether the device needs FE now (its power option is on). */
        public boolean needsPower() {
            return switch (this) {
                case FOOD_MAKER, FERTILIZER_MAKER, OSMOSIS -> machinePowered();
                case EXTERNAL_FILTER, CABINET -> TankPower.anyRequired();
                case MULTIBLOCK_FILTER, WATER_CHANGER, WATER_PUMP -> DeviceEnergy.required();
            };
        }
    }

    /** {@code features.power.winding}: devices with a power option on may be wound instead. */
    public static boolean enabled() {
        return Features.enabled(Feature.WINDING);
    }

    /** The food maker, fertilizer maker and the osmosis pump take FE while any power option is on (like the crucible). */
    public static boolean machinePowered() {
        return org.overengineer.bettacraft.crucible.CrucibleBlockEntity.energyOffered();
    }

    public static int ticksPerCharge() {
        return ServerConfig.getOr(ServerConfig.WIND_TICKS_PER_CHARGE, 24000);
    }

    public static int clicksPerCharge() {
        return ServerConfig.getOr(ServerConfig.WIND_CLICKS_PER_CHARGE, 25);
    }

    /** Most ticks one care step may run a spring down (time in unloaded chunks does not count). */
    public static final long MAX_STEP = 100;

    /** Device-ticks of the work of {@code fe} FE of a device that takes {@code fePerTick} at full work. */
    public static long ticksFor(double fe, double fePerTick) {
        if (fe <= 0) return 0;
        return Math.max(1, (long) Math.ceil(fe / Math.max(1e-6, fePerTick) - 1e-9));
    }

    /** FE first, then the spring: whether the work of {@code fe} FE can be paid now (takes nothing). */
    public static boolean canPay(DeviceEnergy e, Windable w, int fe, double fePerTick) {
        if (fe <= 0 || e.getEnergyStored() >= fe) return true;
        return w.windDevice().windable() && w.windSpring().ticks() >= ticksFor(fe, fePerTick);
    }

    /** Pays the work of {@code fe} FE: with FE if there is enough, else with the spring. False if neither has it. */
    public static boolean pay(DeviceEnergy e, Windable w, int fe, double fePerTick) {
        if (e.use(fe)) return true;
        return w.windDevice().windable() && w.windSpring().use(ticksFor(fe, fePerTick));
    }

    /** The windable device at {@code pos}: its block entity, or the pump head of a multiblock filter part. */
    @Nullable
    public static Windable find(Level level, BlockPos pos) {
        if (level == null || !level.isLoaded(pos)) return null;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof Windable w) return w;
        BlockState s = level.getBlockState(pos);
        if (s.getBlock() instanceof org.overengineer.bettacraft.filter.FilterPartBlock) {
            BlockPos head = org.overengineer.bettacraft.filter.MultiblockFilter.headNear(level, pos);
            if (head != null && level.getBlockEntity(head) instanceof Windable w) return w;
        }
        return null;
    }

    /** What a click of the crank did. */
    public enum Result { WOUND, FULL, REFUSED, NOTHING }

    /**
     * One click of the crank at {@code pos} (server): winds the spring a step, with the ratchet sound and the fill in the
     * action bar. A device that does not take the crank now says why; other blocks are left alone.
     */
    public static Result wind(Level level, BlockPos pos, @Nullable Player player) {
        Windable w = find(level, pos);
        if (w == null) {
            if (player != null) player.displayClientMessage(Component.translatable("message.bettacraft.crank.nothing"), true);
            return Result.NOTHING;
        }
        BlockPos at = ((BlockEntity) w).getBlockPos();
        Component name = level.getBlockState(at).getBlock().getName();
        Device d = w.windDevice();
        if (!d.windable()) {
            if (player != null) player.displayClientMessage(Component.translatable(d.refusal(), name), true);
            return Result.REFUSED;
        }
        if (org.overengineer.bettacraft.tank.TankStore.feedsTank(w)) return windTank(level, at, w, name, player); // M67
        WindSpring s = w.windStore(); // M62: the cabinets share one store
        if (!s.windStep()) {
            if (player != null) player.displayClientMessage(Component.translatable("message.bettacraft.crank.full", name), true);
            return Result.FULL;
        }
        w.onWound();
        org.overengineer.bettacraft.progression.Progress.award(player, org.overengineer.bettacraft.progression.Progress.WOUND); // M45
        float pitch = 0.7F + 0.5F * (float) s.fill(); // the spring gets tighter
        if (ModSounds.devices()) level.playSound(null, at, ModSounds.pick(ModSounds.FOOD_MAKER_WIND, ModSounds.FOOD_MAKER_WIND_VANILLA), SoundSource.BLOCKS, 0.7F, pitch);
        if (player != null) player.displayClientMessage(Component.translatable("message.bettacraft.crank.wound", name, s.percent(), hours(s)), true);
        return Result.WOUND;
    }

    /**
     * M67: a click at a cabinet or an attached external filter: charges the tank's one store ({@code power.crankFePerClick}).
     * Without a tank it says so; a full store says it is full.
     */
    private static Result windTank(Level level, BlockPos at, Windable w, Component name, @Nullable Player player) {
        var c = org.overengineer.bettacraft.tank.TankStore.tankOf(level, w);
        if (c == null) {
            if (player != null) player.displayClientMessage(Component.translatable("message.bettacraft.crank.no_tank", name), true);
            return Result.REFUSED;
        }
        long added = org.overengineer.bettacraft.tank.TankStore.crank(c);
        if (added <= 0) {
            if (player != null) player.displayClientMessage(Component.translatable("message.bettacraft.crank.store_full", name), true);
            return Result.FULL;
        }
        w.onWound();
        org.overengineer.bettacraft.progression.Progress.award(player, org.overengineer.bettacraft.progression.Progress.WOUND); // M45
        org.overengineer.bettacraft.progression.Progress.award(player, org.overengineer.bettacraft.progression.Progress.TANK_CRANKED); // M67
        int pct = org.overengineer.bettacraft.tank.TankStore.percent(c);
        float pitch = 0.7F + 0.5F * pct / 100F;
        if (ModSounds.devices()) level.playSound(null, at, ModSounds.pick(ModSounds.FOOD_MAKER_WIND, ModSounds.FOOD_MAKER_WIND_VANILLA), SoundSource.BLOCKS, 0.7F, pitch);
        if (player != null) player.displayClientMessage(Component.translatable("message.bettacraft.crank.store", name, pct,
                String.format(java.util.Locale.ROOT, "%,d", c.getTank().powerBuffer()), org.overengineer.bettacraft.tank.TankStore.hours(c)), true);
        return Result.WOUND;
    }

    /**
     * What a window or overlay shows of a spring: its fill in percent, -1 while it may not be wound (and is empty). M67: for a
     * cabinet or an attached external filter the fill of the tank's store (-1 without a tank).
     */
    public static int shown(@Nullable Windable w) {
        if (w == null) return -1;
        if (org.overengineer.bettacraft.tank.TankStore.feedsTank(w)) {
            var c = org.overengineer.bettacraft.tank.TankStore.tankOf(((BlockEntity) w).getLevel(), w);
            return c == null || !w.windDevice().windable() ? -1 : org.overengineer.bettacraft.tank.TankStore.percent(c);
        }
        WindSpring s = w.windStore(); // M62
        return w.windDevice().windable() || !s.empty() ? s.percent() : -1;
    }

    /** A menu slot with {@link #shown} of a device (the client copy when {@code w} is null). */
    public static net.minecraft.world.inventory.DataSlot slot(@Nullable Windable w) {
        return new net.minecraft.world.inventory.DataSlot() {
            private int client = -1;

            @Override
            public int get() {
                return w == null ? client : shown(w);
            }

            @Override
            public void set(int v) {
                client = v;
            }
        };
    }

    /** In-game hours one device runs on what the spring holds ("12.5"). */
    public static String hours(WindSpring s) {
        return String.format(java.util.Locale.ROOT, "%.1f", s.ticks() / 1000.0);
    }
}
