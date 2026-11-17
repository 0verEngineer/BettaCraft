package org.overengineer.bettacraft.reef;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.plumbing.PipeBlock;
import org.overengineer.bettacraft.plumbing.PipeNetwork;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M25 part c: the dosing pump ({@link DosingPumpBlock}). Four channels, each a canister slot and its mL per day (0 =
 * off). Every second it doses its share of the day into the salt water tanks it reaches (next to it, or through a pipe
 * network joined to it; several tanks share the dose) and takes it from the canisters; a used-up canister becomes an
 * empty one. Hoppers and other mods insert full canisters and take out empty ones (item capability).
 */
public class DosingPumpBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CHANNELS = 4, STEP = 20, DEFAULT_ML = 10;
    /** What the pump does now (for the window). */
    public static final int IDLE = 0, RUNNING = 1, NO_TANK = 2, FRESH = 3, OFF = 4, EMPTY = 5;

    private final ItemStackHandler items = new ItemStackHandler(CHANNELS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof DosingCanisterItem c && c.additive() != null;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** Canisters for hoppers and other mods: full ones go in, only empty ones come out. */
    private final net.neoforged.neoforge.items.IItemHandler automation = new net.neoforged.neoforge.items.wrapper.RangedWrapper(items, 0, CHANNELS) {
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack s = getStackInSlot(slot);
            if (!(s.getItem() instanceof DosingCanisterItem c) || c.additive() != null) return ItemStack.EMPTY;
            return super.extractItem(slot, amount, simulate);
        }
    };
    private final int[] perDay = {DEFAULT_ML, DEFAULT_ML, DEFAULT_ML, DEFAULT_ML};
    private final double[] carry = new double[CHANNELS];
    private int status = IDLE, tanks;
    /** Values of the (first) tank it doses into, for the window: calcium, KH x 10, magnesium, trace x 100. */
    private int shownCa, shownKh, shownMg, shownTrace;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            if (i < CHANNELS) return perDay[i];
            return switch (i) {
                case 4 -> status;
                case 5 -> tanks;
                case 6 -> shownCa;
                case 7 -> shownKh;
                case 8 -> shownMg;
                case 9 -> shownTrace;
                default -> 0;
            };
        }

        @Override
        public void set(int i, int v) {
            if (i < CHANNELS) perDay[i] = v;
        }

        @Override
        public int getCount() {
            return DATA;
        }
    };
    public static final int DATA = 10;

    public DosingPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DOSING_PUMP.get(), pos, state);
    }

    public ItemStackHandler items() {
        return items;
    }

    public net.neoforged.neoforge.items.IItemHandler automation() {
        return automation;
    }

    public int perDay(int ch) {
        return perDay[Math.max(0, Math.min(CHANNELS - 1, ch))];
    }

    public void setPerDay(int ch, int ml) {
        if (ch < 0 || ch >= CHANNELS) return;
        perDay[ch] = Math.max(0, Math.min(maxMl(), ml));
        setChanged();
    }

    public static int maxMl() {
        return ServerConfig.getOr(ServerConfig.DOSING_MAX_ML, 100);
    }

    public int status() {
        return status;
    }

    /** The salt water (and fresh water) tanks this pump reaches: next to it and at the ends of joined pipe networks. */
    public List<TankBlockEntity> targets() {
        List<TankBlockEntity> out = new ArrayList<>();
        if (level == null) return out;
        for (Direction d : Direction.values()) {
            BlockPos n = worldPosition.relative(d);
            if (!level.isLoaded(n)) continue;
            TankBlockEntity c = TankMultiblock.getController(level, n);
            if (c != null) add(out, c);
            // M49: the tank above a cabinet with a sump next to the pump
            if (level.getBlockEntity(n) instanceof org.overengineer.bettacraft.equipment.CabinetBlockEntity cab && cab.hasSump()
                    && org.overengineer.bettacraft.equipment.Sump.enabled() && cab.tankBlockPos() != null) {
                TankBlockEntity t = TankMultiblock.getController(level, cab.tankBlockPos());
                if (t != null) add(out, t);
            }
            if (Features.enabled(Feature.PIPES) && d != getBlockState().getValue(DosingPumpBlock.FACING) && level.getBlockState(n).getBlock() instanceof PipeBlock) {
                PipeNetwork net = PipeNetwork.of(level, n);
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, DosingPumpBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % STEP != 0) return;
        be.step(STEP / 24000.0);
    }

    /** Doses {@code days} worth of every channel (also for the GameTests). */
    public void step(double days) {
        List<TankBlockEntity> all = targets();
        List<TankBlockEntity> salt = new ArrayList<>(), wet = new ArrayList<>();
        for (TankBlockEntity c : all) if (c.getTank() != null && ReefDosing.dosable(c.getTank())) salt.add(c);
        // M37: fertilizer goes into every tank with water (fresh and salt)
        boolean fert = org.overengineer.bettacraft.fertilizer.Fertilizer.dosingEnabled() && fertilizerChannel();
        if (fert) for (TankBlockEntity c : all) if (c.getTank() != null && c.getTank().waterMb() > 0) wet.add(c);
        tanks = salt.isEmpty() ? wet.size() : salt.size();
        TankData shown = !salt.isEmpty() ? salt.getFirst().getTank() : !all.isEmpty() ? all.getFirst().getTank() : null;
        if (shown != null) {
            shownCa = Math.round(shown.calcium());
            shownKh = Math.round(shown.kh() * 10);
            shownMg = Math.round(shown.magnesium());
            shownTrace = Math.round(shown.trace() * 100);
        } else {
            shownCa = shownKh = shownMg = shownTrace = 0;
        }
        if (!ReefDosing.enabled()) {
            status = OFF;
            return;
        }
        if (salt.isEmpty() && wet.isEmpty()) {
            status = all.isEmpty() ? NO_TANK : FRESH;
            return;
        }
        boolean any = false, dry = false;
        for (int ch = 0; ch < CHANNELS; ch++) {
            if (perDay[ch] <= 0) continue;
            ItemStack s = items.getStackInSlot(ch);
            if (!(s.getItem() instanceof DosingCanisterItem can) || can.additive() == null) {
                if (s.getItem() instanceof DosingCanisterItem) dry = true;
                continue;
            }
            double ml = perDay[ch] * days;
            List<TankBlockEntity> to = can.additive() == ReefDosing.Additive.FERTILIZER ? wet : salt; // M37
            if (to.isEmpty()) continue;
            for (TankBlockEntity c : to) {
                ReefDosing.dose(c.getTank(), can.additive(), ml / to.size());
                c.setChanged();
            }
            any = true;
            // use it up: wear points of the canister
            carry[ch] += ml * s.getMaxDamage() / Math.max(1, DosingCanisterItem.capacityMl());
            int whole = (int) Math.floor(carry[ch]);
            if (whole > 0) {
                carry[ch] -= whole;
                int dmg = s.getDamageValue() + whole;
                if (dmg >= s.getMaxDamage()) {
                    items.setStackInSlot(ch, new ItemStack(ModItems.DOSING_CANISTER.get()));
                    carry[ch] = 0;
                } else {
                    ItemStack c = s.copy();
                    c.setDamageValue(dmg);
                    items.setStackInSlot(ch, c);
                }
            }
        }
        status = any ? RUNNING : dry ? EMPTY : IDLE;
        setChanged();
    }

    /** M37: a running channel holds a fertilizer canister. */
    private boolean fertilizerChannel() {
        for (int ch = 0; ch < CHANNELS; ch++) {
            if (perDay[ch] > 0 && items.getStackInSlot(ch).getItem() instanceof DosingCanisterItem c && c.additive() == ReefDosing.Additive.FERTILIZER) return true;
        }
        return false;
    }

    /** Comparator: 15 = all running canisters full, 0 = one of them is empty. */
    public int comparator() {
        double least = 1;
        boolean any = false;
        for (int ch = 0; ch < CHANNELS; ch++) {
            if (perDay[ch] <= 0) continue;
            ItemStack s = items.getStackInSlot(ch);
            if (!(s.getItem() instanceof DosingCanisterItem c)) continue;
            any = true;
            least = Math.min(least, c.additive() == null ? 0 : DosingCanisterItem.mlLeft(s) / Math.max(1, DosingCanisterItem.capacityMl()));
        }
        return any ? (int) Math.round(least * 15) : 0;
    }

    /** Status line of the window (also the client from the synced status). */
    public static Component statusText(int status, int tanks) {
        return switch (status) {
            case RUNNING -> Component.translatable("gui.bettacraft.dosing.running", tanks);
            case NO_TANK -> Component.translatable("gui.bettacraft.dosing.no_tank");
            case FRESH -> Component.translatable("gui.bettacraft.dosing.fresh");
            case OFF -> Component.translatable("gui.bettacraft.dosing.off");
            case EMPTY -> Component.translatable("gui.bettacraft.dosing.empty");
            default -> Component.translatable("gui.bettacraft.dosing.idle");
        };
    }

    // ------------------------------------------------------------------ menu

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.bettacraft.dosing_pump");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        step(0); // fresh values for the window
        return new DosingPumpMenu(id, inv, this, data);
    }

    // ------------------------------------------------------------------ save

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putIntArray("per_day", perDay);
        for (int i = 0; i < CHANNELS; i++) tag.putDouble("carry" + i, carry[i]);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("items")) items.deserializeNBT(registries, tag.getCompound("items"));
        int[] p = tag.getIntArray("per_day");
        for (int i = 0; i < CHANNELS; i++) {
            perDay[i] = i < p.length ? Math.max(0, p[i]) : DEFAULT_ML;
            double c = tag.getDouble("carry" + i);
            carry[i] = Double.isFinite(c) ? Math.max(0, c) : 0;
        }
    }
}
