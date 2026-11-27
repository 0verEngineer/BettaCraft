package org.overengineer.bettacraft.reef;

import org.overengineer.bettacraft.util.NbtCompat;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.item.ItemStackHandler;
import org.overengineer.bettacraft.chem.WaterChemistry;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.plumbing.PipeBlock;
import org.overengineer.bettacraft.plumbing.PipeNetwork;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M49 part a: the calcium reactor, a closed column of coral rubble that the tank water runs through. CO2 from a gas cylinder
 * next to it makes the water acidic, the rubble (calcium carbonate) dissolves and the effluent brings calcium and the
 * matching alkalinity back into the salt water tanks it reaches (next to it, next to the cabinet of their sump, or at the
 * ends of a pipe network joined to it), the other way besides the dosing pump. It holds the calcium at about the level of
 * fresh sea water ({@link #TARGET_CALCIUM}) and uses {@code sump.calciumReactorRubblePerDay} rubble while it runs.
 * Switch: {@code features.sump.calciumReactor}.
 */
public class CalciumReactorBlockEntity extends BlockEntity implements MenuProvider {
    public static final int STEP = 20;
    public static final int IDLE = 0, RUNNING = 1, NO_TANK = 2, FRESH = 3, OFF = 4, NO_RUBBLE = 5, NO_CO2 = 6, FULL = 7;
    /** It stops dissolving while the calcium is this high (its effluent valve closes). */
    public static final float TARGET_CALCIUM = 440.0F;
    /** Gas it takes from the cylinder per day of running (bubbles x days, like a tank's CO2). */
    public static final double GAS_PER_DAY = 0.5;
    public static final int DATA = 5;

    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ModItems.CORAL_RUBBLE.get());
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private int status = IDLE, tanks;
    private double carry;
    private int shownCa, shownKh;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> status;
                case 1 -> tanks;
                case 2 -> shownCa;
                case 3 -> shownKh;
                case 4 -> (int) Math.round(carry * 100);
                default -> 0;
            };
        }

        @Override
        public void set(int i, int v) {}

        @Override
        public int getCount() {
            return DATA;
        }
    };

    public CalciumReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CALCIUM_REACTOR.get(), pos, state);
    }

    public ItemStackHandler items() {
        return items;
    }

    public int status() {
        return status;
    }

    public static boolean enabled() {
        return Features.enabled(Feature.CALCIUM_REACTOR);
    }

    /** The tanks it reaches: next to it, above a cabinet with a sump next to it, at the ends of a joined pipe network. */
    public List<TankBlockEntity> targets() {
        List<TankBlockEntity> out = new ArrayList<>();
        if (level == null) return out;
        for (Direction d : Direction.values()) {
            BlockPos n = worldPosition.relative(d);
            if (!level.isLoaded(n)) continue;
            TankBlockEntity c = TankMultiblock.getController(level, n);
            if (c != null) add(out, c);
            if (level.getBlockEntity(n) instanceof org.overengineer.bettacraft.equipment.CabinetBlockEntity cab && cab.hasSump()
                    && org.overengineer.bettacraft.equipment.Sump.enabled() && cab.tankBlockPos() != null) {
                TankBlockEntity t = TankMultiblock.getController(level, cab.tankBlockPos());
                if (t != null) add(out, t);
            }
            if (Features.enabled(Feature.PIPES) && org.overengineer.bettacraft.plumbing.Pipes.isPipe(level.getBlockState(n))) { // M68 part B: bundles too
                PipeNetwork net = org.overengineer.bettacraft.plumbing.Pipes.networkNextTo(level, worldPosition, d);
                if (net == null) continue;
                for (BlockPos e : net.tankEnds(level)) {
                    TankBlockEntity t = TankMultiblock.getController(level, e);
                    if (t != null) add(out, t);
                }
            }
        }
        return out;
    }

    private static void add(List<TankBlockEntity> out, TankBlockEntity c) {
        for (TankBlockEntity o : out) if (o.getBlockPos().equals(c.getBlockPos())) return;
        out.add(c);
    }

    /** A gas cylinder with gas next to it, or null. */
    @Nullable
    private org.overengineer.bettacraft.co2.Co2CylinderBlockEntity cylinder() {
        if (level == null) return null;
        for (Direction d : Direction.values()) {
            BlockPos n = worldPosition.relative(d);
            if (level.isLoaded(n) && level.getBlockEntity(n) instanceof org.overengineer.bettacraft.co2.Co2CylinderBlockEntity cyl && cyl.gas() > 0) return cyl;
        }
        return null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CalciumReactorBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % STEP != 0) return;
        be.step(STEP / 24000.0);
    }

    /** Pure: the calcium (mg/L in one block of water) and the alkalinity (dKH) a day of running brings into {@code blocks} of water. */
    public static double[] dose(double caPerDay, double days, double blocks) {
        double ca = caPerDay * days / Math.max(1.0, blocks);
        return new double[]{ca, WaterChemistry.khForCalcium(ca) * ReefDosing.KH_SHARE};
    }

    /** {@code days} of running (also for the GameTests). */
    public void step(double days) {
        List<TankBlockEntity> all = targets();
        List<TankBlockEntity> salt = new ArrayList<>();
        for (TankBlockEntity c : all) if (c.getTank() != null && ReefDosing.dosable(c.getTank())) salt.add(c);
        tanks = salt.size();
        TankData shown = !salt.isEmpty() ? salt.getFirst().getTank() : !all.isEmpty() ? all.getFirst().getTank() : null;
        shownCa = shown == null ? 0 : Math.round(shown.calcium());
        shownKh = shown == null ? 0 : Math.round(shown.kh() * 10);
        int st;
        if (!enabled()) st = OFF;
        else if (salt.isEmpty()) st = all.isEmpty() ? NO_TANK : FRESH;
        else if (items.getStackInSlot(0).isEmpty()) st = NO_RUBBLE;
        else {
            var cyl = cylinder();
            List<TankBlockEntity> low = new ArrayList<>();
            for (TankBlockEntity c : salt) if (c.getTank().calcium() < TARGET_CALCIUM) low.add(c);
            if (cyl == null) st = NO_CO2;
            else if (low.isEmpty()) st = FULL;
            else {
                st = RUNNING;
                if (days > 0) {
                    double caDay = ServerConfig.getOr(ServerConfig.CALCIUM_REACTOR_CA, 40.0) / low.size();
                    for (TankBlockEntity c : low) {
                        TankData t = c.getTank();
                        double[] d = dose(caDay, days, t.systemBlocks());
                        t.setCalcium((float) Math.min(TARGET_CALCIUM + 20, t.calcium() + d[0]));
                        if (ReefDosing.enabled()) t.setKh((float) (t.kh() + d[1]));
                        c.setChanged();
                    }
                    cyl.setGas((float) Math.max(0, cyl.gas() - GAS_PER_DAY * days));
                    carry += ServerConfig.getOr(ServerConfig.CALCIUM_REACTOR_RUBBLE, 2.0) * days;
                    int whole = (int) Math.floor(carry);
                    if (whole > 0) {
                        carry -= whole;
                        ItemStack s = items.getStackInSlot(0).copy();
                        s.shrink(whole);
                        items.setStackInSlot(0, s);
                    }
                }
            }
        }
        boolean lit = st == RUNNING;
        BlockState s = getBlockState();
        if (level != null && s.hasProperty(CalciumReactorBlock.ACTIVE) && s.getValue(CalciumReactorBlock.ACTIVE) != lit) {
            level.setBlock(worldPosition, s.setValue(CalciumReactorBlock.ACTIVE, lit), Block.UPDATE_CLIENTS);
        }
        if (st != status) {
            if (st == RUNNING && level instanceof net.minecraft.server.level.ServerLevel sl && !salt.isEmpty()) {
                org.overengineer.bettacraft.progression.Progress.awardNear(sl, salt.getFirst(), org.overengineer.bettacraft.progression.Progress.CALCIUM_REACTOR, 0);
            }
            status = st;
            setChanged();
        }
        if (days > 0) setChanged();
    }

    /** Status line of the window and the overlays. */
    public static Component statusText(int status, int tanks) {
        return switch (status) {
            case RUNNING -> Component.translatable("gui.bettacraft.calcium_reactor.running", tanks);
            case NO_TANK -> Component.translatable("gui.bettacraft.dosing.no_tank");
            case FRESH -> Component.translatable("gui.bettacraft.calcium_reactor.fresh");
            case OFF -> Component.translatable("gui.bettacraft.calcium_reactor.off");
            case NO_RUBBLE -> Component.translatable("gui.bettacraft.calcium_reactor.no_rubble");
            case NO_CO2 -> Component.translatable("gui.bettacraft.calcium_reactor.no_co2");
            case FULL -> Component.translatable("gui.bettacraft.calcium_reactor.full", Math.round(TARGET_CALCIUM));
            default -> Component.translatable("gui.bettacraft.dosing.idle");
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.bettacraft.calcium_reactor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        step(0);
        return new CalciumReactorMenu(id, inv, this, data);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = NbtCompat.registries(this);
        super.saveAdditional(output);
        tag.put("items", items.serializeNBT(registries));
        tag.putDouble("carry", carry);
        tag.putInt("status", status);
        NbtCompat.write(output, tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        CompoundTag tag = NbtCompat.read(input);
        HolderLookup.Provider registries = input.lookup();
        super.loadAdditional(input);
        if (tag.contains("items")) items.deserializeNBT(registries, tag.getCompoundOrEmpty("items"));
        double c = tag.getDoubleOr("carry", 0d);
        carry = Double.isFinite(c) ? Math.max(0, c) : 0;
        status = tag.getIntOr("status", 0);
    }
}
