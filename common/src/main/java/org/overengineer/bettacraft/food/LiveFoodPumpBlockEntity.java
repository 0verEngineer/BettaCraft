package org.overengineer.bettacraft.food;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.platform.item.IItemHandler;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.plumbing.DeviceEnergy;
import org.overengineer.bettacraft.plumbing.PipeBlock;
import org.overengineer.bettacraft.plumbing.PipeNetwork;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.tank.TankBlock;

/**
 * M26: the live food pump ({@link LiveFoodPumpBlock}). {@link #doses} times a day (evenly from 7:00 on; 0 = only by
 * the button) it takes {@link #portions} portions of live or fry food out of the breeder next to it (any block with an
 * item capability that gives such food: daphnia barrel, artemia hatchery, larva tub, tubifex dish) and pumps each as
 * {@value LiveFood#PORTION_MB} mB of suspension ({@code bettacraft:live_food}) into the pipes next to it or a tank right
 * next to it. A tank takes it only if somebody there eats it; the animals eat it as live food. A bottle a food leaves
 * (artemia nauplii) goes back into the breeder (the hatchery needs it) or stays in the pump (hoppers take it out).
 * A redstone signal pauses it; with {@code pipes.pumpsRequireEnergy} each portion costs FE. A dose that cannot go out
 * waits until the next one replaces it.
 */
public class LiveFoodPumpBlockEntity extends BlockEntity implements MenuProvider {
    public static final int MAX_DOSES = 8, MAX_PORTIONS = 8, MAX_BOTTLES = 16, STEP = 10;
    public static final int IDLE = 0, PUMPED = 1, NO_SOURCE = 2, NO_FOOD = 3, NO_TARGET = 4, NOBODY_EATS = 5, NO_POWER = 6, BOTTLES_FULL = 7,
            OFF = 8, PAUSED = 9, BUSY = 10;
    /** First dose of the day (7:00). */
    public static final long FIRST_DOSE = 1000;
    public static final int DATA = 7;

    private int doses = 2, portions = 1, pending, status = IDLE, lastPumped, bottles;
    private long lastEvent = Long.MIN_VALUE;
    private int shownFood, shownTanks;
    private final DeviceEnergy energy = new DeviceEnergy(4000, this::setChanged);

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> doses;
                case 1 -> portions;
                case 2 -> status;
                case 3 -> lastPumped;
                case 4 -> shownFood;
                case 5 -> shownTanks;
                case 6 -> pending;
                default -> 0;
            };
        }

        @Override
        public void set(int i, int v) {
            if (i == 0) doses = v;
            else if (i == 1) portions = v;
        }

        @Override
        public int getCount() {
            return DATA;
        }
    };

    /** The empty bottles for hoppers (extract only). */
    private final IItemHandler bottleHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return bottles <= 0 ? ItemStack.EMPTY : new ItemStack(Items.GLASS_BOTTLE, bottles);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            int n = Math.min(amount, bottles);
            if (n <= 0) return ItemStack.EMPTY;
            if (!simulate) {
                bottles -= n;
                setChanged();
            }
            return new ItemStack(Items.GLASS_BOTTLE, n);
        }

        @Override
        public int getSlotLimit(int slot) {
            return MAX_BOTTLES;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };

    public LiveFoodPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIVE_FOOD_PUMP.get(), pos, state);
    }

    public int doses() { return doses; }
    public int portions() { return portions; }
    public int status() { return status; }
    public int lastPumped() { return lastPumped; }
    public int bottles() { return bottles; }
    public int pending() { return pending; }
    public DeviceEnergy energy() { return energy; }
    public IItemHandler bottleHandler() { return bottleHandler; }

    public void setDoses(int n) {
        doses = Math.max(0, Math.min(MAX_DOSES, n));
        lastEvent = Long.MIN_VALUE; // the new schedule starts with its next dose
        setChanged();
    }

    public void setPortions(int n) {
        portions = Math.max(1, Math.min(MAX_PORTIONS, n));
        setChanged();
    }

    /** The button: one dose now. */
    public void doseNow() {
        pending = portions;
        setChanged();
    }

    /** FE one portion costs (with {@code power.costs} EASY or HARD, M67: EASY a third). */
    public static int fePerPortion() {
        return org.overengineer.bettacraft.tank.TankPower.feInt(ServerConfig.getOr(ServerConfig.LIVE_FOOD_PUMP_FE, 200));
    }

    /** Key of the latest dose at or before {@code dayTime} ({@code doses} evenly spread from {@link #FIRST_DOSE}). */
    public static long eventKey(long dayTime, int doses) {
        if (doses <= 0) return 0;
        long t = dayTime - FIRST_DOSE;
        long day = Math.floorDiv(t, 24000L);
        long tod = Math.floorMod(t, 24000L);
        return day * doses + tod * doses / 24000L;
    }

    // ------------------------------------------------------------------ ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, LiveFoodPumpBlockEntity be) {
        if (!(level instanceof ServerLevel server) || (level.getGameTime() + pos.asLong()) % STEP != 0) return;
        be.step(server);
    }

    public void step(ServerLevel level) {
        if (doses > 0) {
            long key = eventKey(level.getDayTime(), doses);
            if (lastEvent == Long.MIN_VALUE || key < lastEvent) {
                lastEvent = key; // fresh pump or time set back: wait for the next dose
                setChanged();
            } else if (key != lastEvent) {
                lastEvent = key;
                pending = portions; // a dose that could not go out is replaced, never piled up
                setChanged();
            }
        }
        if (!Features.enabled(Feature.LIVE_FOOD_PUMP)) {
            status = OFF;
            return;
        }
        if (level.hasNeighborSignal(worldPosition)) {
            status = PAUSED;
            return;
        }
        refresh(level);
        if (pending <= 0) {
            if (status != PUMPED) status = IDLE;
            return;
        }
        int done = 0;
        int r = IDLE;
        while (pending > 0) {
            r = pumpOne(level);
            if (r != PUMPED) break;
            pending--;
            done++;
        }
        if (done > 0) {
            lastPumped = r == BUSY && status == BUSY ? lastPumped + done : done;
            status = r == BUSY ? BUSY : PUMPED;
            level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY_FISH, SoundSource.BLOCKS, 0.5F, 1.4F);
        } else {
            status = r;
        }
        setChanged();
    }

    /** Pumps one portion (the result code: {@link #PUMPED} or why not). */
    public int pumpOne(ServerLevel level) {
        if (DeviceEnergy.required() && energy.getEnergyStored() < fePerPortion()) return NO_POWER;
        boolean anySource = false;
        for (Direction d : Direction.values()) {
            IItemHandler h = source(level, d);
            if (h == null) continue;
            anySource = true;
            for (int slot = 0; slot < h.getSlots(); slot++) {
                ItemStack sim = h.extractItem(slot, 1, true);
                if (sim.isEmpty() || !LiveFood.pumpable(sim)) continue;
                ItemStack rest = org.overengineer.bettacraft.platform.Platform.get().craftingRemainder(sim);
                boolean back = !rest.isEmpty() && insertSomewhere(h, rest, true);
                if (!rest.isEmpty() && !back && bottles >= MAX_BOTTLES) return BOTTLES_FULL;
                FluidStack f = LiveFood.suspension(sim, 1);
                int took = push(level, f, IFluidHandler.FluidAction.SIMULATE);
                if (took < LiveFood.PORTION_MB) return pipesFull(level) ? BUSY : shownTanks == 0 ? NO_TARGET : NOBODY_EATS;
                ItemStack got = h.extractItem(slot, 1, false);
                if (got.isEmpty()) continue;
                push(level, LiveFood.suspension(got, 1), IFluidHandler.FluidAction.EXECUTE);
                if (!rest.isEmpty() && !(back && insertSomewhere(h, rest, false))) bottles++;
                if (DeviceEnergy.required()) energy.use(fePerPortion());
                return PUMPED;
            }
        }
        return anySource ? NO_FOOD : NO_SOURCE;
    }

    /** The pipes next to the pump carried all they can in this window (the rest of the dose follows). */
    private boolean pipesFull(ServerLevel level) {
        for (Direction d : Direction.values()) {
            BlockPos n = worldPosition.relative(d);
            if (!level.isLoaded(n) || !(level.getBlockState(n).getBlock() instanceof PipeBlock)) continue;
            PipeNetwork net = PipeNetwork.of(level, n);
            if (net != null && !net.tankEnds(level).isEmpty() && net.room(level) < LiveFood.PORTION_MB) return true;
        }
        return false;
    }

    private static boolean insertSomewhere(IItemHandler h, ItemStack s, boolean simulate) {
        for (int i = 0; i < h.getSlots(); i++) if (h.insertItem(i, s.copy(), simulate).isEmpty()) return true;
        return false;
    }

    /** The breeder on this side (any item capability but pipes and tanks), or null. */
    @Nullable
    private IItemHandler source(Level level, Direction d) {
        BlockPos n = worldPosition.relative(d);
        if (!level.isLoaded(n)) return null;
        BlockState s = level.getBlockState(n);
        if (s.getBlock() instanceof PipeBlock || s.getBlock() instanceof TankBlock || s.getBlock() instanceof LiveFoodPumpBlock) return null;
        return org.overengineer.bettacraft.platform.Platform.get().itemHandler(level, n, d.getOpposite());
    }

    /** Pushes the suspension into the pipes next to the pump, else into a tank right next to it. Returns the mB taken. */
    private int push(ServerLevel level, FluidStack f, IFluidHandler.FluidAction action) {
        for (Direction d : Direction.values()) {
            BlockPos n = worldPosition.relative(d);
            if (!level.isLoaded(n)) continue;
            BlockState s = level.getBlockState(n);
            int took = 0;
            if (s.getBlock() instanceof PipeBlock) {
                PipeNetwork net = PipeNetwork.of(level, n);
                if (net != null) took = net.fill(level, f.copy(), action, worldPosition);
            } else if (s.getBlock() instanceof TankBlock) {
                IFluidHandler h = org.overengineer.bettacraft.platform.Platform.get().fluidHandler(level, n, d.getOpposite());
                if (h != null) took = h.fill(f.copy(), action);
            }
            if (took >= LiveFood.PORTION_MB) return took;
        }
        return 0;
    }

    /** What the window shows: food portions in the breeders and the tanks the pump reaches. */
    private void refresh(ServerLevel level) {
        int food = 0;
        java.util.Set<BlockPos> tanks = new java.util.HashSet<>();
        for (Direction d : Direction.values()) {
            IItemHandler h = source(level, d);
            if (h != null) {
                for (int slot = 0; slot < h.getSlots(); slot++) {
                    ItemStack s = h.getStackInSlot(slot);
                    if (!s.isEmpty() && LiveFood.pumpable(s) && !h.extractItem(slot, 1, true).isEmpty()) food += s.getCount();
                }
            }
            BlockPos n = worldPosition.relative(d);
            if (!level.isLoaded(n)) continue;
            BlockState s = level.getBlockState(n);
            if (s.getBlock() instanceof PipeBlock) {
                PipeNetwork net = PipeNetwork.of(level, n);
                if (net != null) for (BlockPos p : net.tankEnds(level)) tanks.add(controllerPos(level, p));
            } else if (s.getBlock() instanceof TankBlock) {
                tanks.add(controllerPos(level, n));
            }
        }
        shownFood = food;
        shownTanks = tanks.size();
    }

    private static BlockPos controllerPos(Level level, BlockPos p) {
        var c = org.overengineer.bettacraft.tank.TankMultiblock.getController(level, p);
        return c == null ? p : c.getBlockPos();
    }

    /** Status line of the window. */
    public static Component statusText(int status, int pumped) {
        return switch (status) {
            case PUMPED -> Component.translatable(pumped == 1 ? "gui.bettacraft.live_food_pump.pumped_one" : "gui.bettacraft.live_food_pump.pumped", pumped);
            case NO_SOURCE -> Component.translatable("gui.bettacraft.live_food_pump.no_source");
            case NO_FOOD -> Component.translatable("gui.bettacraft.live_food_pump.no_food");
            case NO_TARGET -> Component.translatable("gui.bettacraft.live_food_pump.no_target");
            case NOBODY_EATS -> Component.translatable("gui.bettacraft.live_food_pump.nobody_eats");
            case NO_POWER -> Component.translatable("gui.bettacraft.live_food_pump.no_power");
            case BOTTLES_FULL -> Component.translatable("gui.bettacraft.live_food_pump.bottles_full");
            case OFF -> Component.translatable("gui.bettacraft.live_food_pump.off");
            case PAUSED -> Component.translatable("gui.bettacraft.live_food_pump.paused");
            case BUSY -> Component.translatable(pumped == 1 ? "gui.bettacraft.live_food_pump.busy_one" : "gui.bettacraft.live_food_pump.busy", pumped);
            default -> Component.translatable("gui.bettacraft.live_food_pump.idle");
        };
    }

    // ------------------------------------------------------------------ menu

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.bettacraft.live_food_pump");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        if (level instanceof ServerLevel s) refresh(s);
        return new LiveFoodPumpMenu(id, this, data);
    }

    // ------------------------------------------------------------------ save

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("doses", doses);
        tag.putInt("portions", portions);
        tag.putInt("pending", pending);
        tag.putInt("bottles", bottles);
        tag.putLong("last_event", lastEvent);
        energy.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        doses = tag.contains("doses") ? Math.max(0, Math.min(MAX_DOSES, tag.getInt("doses"))) : 2;
        portions = tag.contains("portions") ? Math.max(1, Math.min(MAX_PORTIONS, tag.getInt("portions"))) : 1;
        pending = Math.max(0, Math.min(MAX_PORTIONS, tag.getInt("pending")));
        bottles = Math.max(0, Math.min(MAX_BOTTLES, tag.getInt("bottles")));
        lastEvent = tag.contains("last_event") ? tag.getLong("last_event") : Long.MIN_VALUE;
        energy.load(tag);
    }
}
