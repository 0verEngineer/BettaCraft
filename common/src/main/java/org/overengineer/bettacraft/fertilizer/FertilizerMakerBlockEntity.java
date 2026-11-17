package org.overengineer.bettacraft.fertilizer;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.platform.item.IItemHandler;
import org.overengineer.bettacraft.platform.item.ItemStackHandler;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.plumbing.DeviceEnergy;
import org.overengineer.bettacraft.plumbing.PipeBlock;
import org.overengineer.bettacraft.plumbing.PipeNetwork;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.registry.ModFluids;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.winding.WindSpring;
import org.overengineer.bettacraft.winding.Windable;
import org.overengineer.bettacraft.winding.Winding;

/**
 * M37: the fertilizer maker. One batch ({@code fertilizer.fertilizerTicksPerBatch} ticks) mixes a kelp meal and a nutrient
 * salt into {@value Fertilizer#BATCH_MB} mB of water: {@value Fertilizer#BATCH_MB} mB of liquid fertilizer (a bottle) in
 * its output tank. Empty glass bottles in the bottle slot are filled from it; the rest goes by itself into the pipes or a
 * tank next to it (a tank takes only up to 100 %, {@link Fertilizer#fillTank}); pipes and tanks of other mods may also
 * drain it, and fill the water tank.
 * <p>
 * Drive like the food maker: its spring (wound with the crank, {@link Winding}) while no power option is on; with a power
 * option on {@code fertilizer.fertilizerFePerTick} FE while it works, and its spring when the FE is short
 * ({@code features.power.winding}). Create's rotation gives the FE (or winds the spring). Redstone pauses it.
 * <p>
 * Hoppers, AE2 / RS: kelp meal, nutrient salt and glass bottles in, the filled bottles out ({@link #automation()}).
 */
public class FertilizerMakerBlockEntity extends BlockEntity implements MenuProvider, Windable {
    public static final int MEAL = 0, SALT = 1, BOTTLES = 2, OUTPUT = 3, SLOTS = 4, DATA = 10;
    public static final int TANK_MB = 4000;
    public static final int IDLE = 0, WORKING = 1, NO_INGREDIENTS = 2, NO_WATER = 3, FULL = 4, WIND_IT = 5, NO_POWER = 6, PAUSED = 7, OFF = 8, FE_OR_WIND = 9;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case MEAL -> stack.is(ModItems.KELP_MEAL.get());
                case SALT -> stack.is(ModItems.NUTRIENT_SALT.get());
                case BOTTLES -> stack.is(Items.GLASS_BOTTLE);
                default -> false;
            };
        }
    };
    private final DeviceEnergy energy = new DeviceEnergy(20000, this::setChanged);
    private final WindSpring spring = WindSpring.of(Winding.Device.FERTILIZER_MAKER, this::setChanged);
    private int water, fertilizer, progress, status = IDLE;
    private float turned;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> batchTicks();
                case 2 -> powered() ? energy.getEnergyStored() : (int) Math.round(spring.fill() * 1000);
                case 3 -> powered() ? energy.getMaxEnergyStored() : 1000;
                case 4 -> status;
                case 5 -> powered() ? 1 : 0;
                case 6 -> Winding.Device.FERTILIZER_MAKER.windable() ? spring.percent() : -1;
                case 7 -> water;
                case 8 -> fertilizer;
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

    public FertilizerMakerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FERTILIZER_MAKER.get(), pos, state);
    }

    public ItemStackHandler items() { return items; }
    public DeviceEnergy energy() { return energy; }
    public int water() { return water; }
    public int fertilizer() { return fertilizer; }
    public int progress() { return progress; }
    public int status() { return status; }

    public void setWater(int mb) {
        water = Math.max(0, Math.min(TANK_MB, mb));
        setChanged();
    }

    public void setFertilizer(int mb) {
        fertilizer = Math.max(0, Math.min(TANK_MB, mb));
        setChanged();
    }

    @Override
    public WindSpring windSpring() {
        return spring;
    }

    @Override
    public Winding.Device windDevice() {
        return Winding.Device.FERTILIZER_MAKER;
    }

    @Override
    public void onWound() {
        BlockState st = getBlockState();
        if (level != null && st.hasProperty(FertilizerMakerBlock.CRANK)) {
            level.setBlock(worldPosition, st.setValue(FertilizerMakerBlock.CRANK, (st.getValue(FertilizerMakerBlock.CRANK) + 1) % 4), Block.UPDATE_CLIENTS);
        }
    }

    public static int batchTicks() {
        return ServerConfig.getOr(ServerConfig.FERT_MAKER_TICKS, 200);
    }

    /** Runs on FE (and its spring when the FE is short) while any power option is on. */
    public static boolean powered() {
        return Winding.machinePowered();
    }

    // ------------------------------------------------------------------ work

    static void serverTick(Level level, BlockPos pos, BlockState state, FertilizerMakerBlockEntity be) {
        be.tick(level, pos, state);
    }

    /** One tick (public for the GameTests). */
    public void tick(Level level, BlockPos pos, BlockState state) {
        int now = step(level, pos);
        if (now != status) {
            status = now;
            setChanged();
        }
        if ((level.getGameTime() + pos.asLong()) % 20 == 0) {
            bottle();
            if (level instanceof ServerLevel s) push(s);
        }
        boolean working = now == WORKING;
        if (state.hasProperty(FertilizerMakerBlock.WORKING) && state.getValue(FertilizerMakerBlock.WORKING) != working) {
            level.setBlock(pos, state.setValue(FertilizerMakerBlock.WORKING, working), Block.UPDATE_CLIENTS);
        }
    }

    private int step(Level level, BlockPos pos) {
        if (!Fertilizer.enabled()) return OFF;
        if (level.hasNeighborSignal(pos)) return PAUSED;
        if (items.getStackInSlot(MEAL).isEmpty() || items.getStackInSlot(SALT).isEmpty()) {
            if (progress > 0) progress = 0;
            return NO_INGREDIENTS;
        }
        if (water < Fertilizer.BATCH_MB) return NO_WATER;
        if (fertilizer + Fertilizer.BATCH_MB > TANK_MB) return FULL;
        if ((level.getGameTime() + pos.asLong()) % 20 == 0) turned = org.overengineer.bettacraft.compat.create.CreateKinetics.drive(level, pos);
        if (powered()) {
            int fe = ServerConfig.getOr(ServerConfig.FERT_MAKER_FE, 10);
            if (turned > 0F) energy.drive(level, pos, fe); // Create's rotation instead of FE
            if (!energy.use(fe)) {
                if (!Winding.enabled()) return NO_POWER;
                if (!spring.use(1)) return FE_OR_WIND;
            }
        } else {
            // Create's rotation winds the spring (at full speed twice as fast as the machine uses it)
            if (turned > 0F) spring.set(spring.ticks() + (int) Math.ceil(2F * turned));
            if (!spring.use(1)) return WIND_IT;
        }
        progress++;
        if (progress >= batchTicks()) {
            items.extractItem(MEAL, 1, false);
            items.extractItem(SALT, 1, false);
            water -= Fertilizer.BATCH_MB;
            fertilizer += Fertilizer.BATCH_MB;
            progress = 0;
        }
        setChanged();
        return WORKING;
    }

    /** Fills a glass bottle from the output tank into the output slot. */
    public boolean bottle() {
        if (fertilizer < Fertilizer.BATCH_MB || items.getStackInSlot(BOTTLES).isEmpty() || !items.getStackInSlot(OUTPUT).isEmpty()) return false;
        items.extractItem(BOTTLES, 1, false);
        items.setStackInSlot(OUTPUT, new ItemStack(ModItems.LIQUID_FERTILIZER.get()));
        fertilizer -= Fertilizer.BATCH_MB;
        setChanged();
        return true;
    }

    /** Pushes fertilizer into the pipes or a tank next to it (only while no bottle waits to be filled). Returns the mB given. */
    public int push(ServerLevel level) {
        if (fertilizer <= 0 || !Fertilizer.enabled() || !items.getStackInSlot(BOTTLES).isEmpty()) return 0;
        int given = 0;
        for (Direction d : Direction.values()) {
            if (fertilizer <= 0) break;
            BlockPos n = worldPosition.relative(d);
            if (!level.isLoaded(n)) continue;
            BlockState s = level.getBlockState(n);
            FluidStack f = Fertilizer.fluid(Math.min(fertilizer, Fertilizer.BATCH_MB));
            int took = 0;
            if (s.getBlock() instanceof PipeBlock) {
                PipeNetwork net = PipeNetwork.of(level, n);
                if (net != null) took = net.fill(level, f, IFluidHandler.FluidAction.EXECUTE, worldPosition);
            } else if (s.getBlock() instanceof TankBlock) {
                IFluidHandler h = org.overengineer.bettacraft.platform.Platform.get().fluidHandler(level, n, d.getOpposite());
                if (h != null) took = h.fill(f, IFluidHandler.FluidAction.EXECUTE);
            }
            if (took > 0) {
                fertilizer -= Math.min(fertilizer, took);
                given += took;
                setChanged();
            }
        }
        return given;
    }

    /** Comparator: how full the output tank is. */
    public int comparator() {
        return fertilizer <= 0 ? 0 : 1 + Math.round(14F * fertilizer / TANK_MB);
    }

    // ------------------------------------------------------------------ capabilities

    /** Hoppers and storage mods: kelp meal, nutrient salt and glass bottles in, the filled bottles out. */
    public IItemHandler automation() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return SLOTS;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return items.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return slot == OUTPUT ? stack : items.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return slot == OUTPUT ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return items.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot != OUTPUT && items.isItemValid(slot, stack);
            }
        };
    }

    /** Pipes and tanks: water in (tank 0), liquid fertilizer out (tank 1). */
    public IFluidHandler fluids() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 2;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                if (tank == 0) return water <= 0 ? FluidStack.EMPTY : new FluidStack(net.minecraft.world.level.material.Fluids.WATER, water);
                return fertilizer <= 0 ? FluidStack.EMPTY : Fertilizer.fluid(fertilizer);
            }

            @Override
            public int getTankCapacity(int tank) {
                return TANK_MB;
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return tank == 0 ? ModFluids.isWater(stack.getFluid()) : Fertilizer.isFluid(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (resource.isEmpty() || !ModFluids.isWater(resource.getFluid())) return 0;
                int n = Math.min(resource.getAmount(), TANK_MB - water);
                if (n > 0 && action.execute()) setWater(water + n);
                return Math.max(0, n);
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                if (!Fertilizer.isFluid(resource)) return FluidStack.EMPTY;
                return drain(resource.getAmount(), action);
            }

            @Override
            public FluidStack drain(int max, FluidAction action) {
                int n = Math.min(max, fertilizer);
                if (n <= 0) return FluidStack.EMPTY;
                if (action.execute()) setFertilizer(fertilizer - n);
                return Fertilizer.fluid(n);
            }
        };
    }

    public static Component statusText(int status) {
        return Component.translatable("gui.bettacraft.fertilizer_maker.status." + switch (status) {
            case WORKING -> "working";
            case NO_INGREDIENTS -> "no_ingredients";
            case NO_WATER -> "no_water";
            case FULL -> "full";
            case WIND_IT -> "wind";
            case NO_POWER -> "no_power";
            case FE_OR_WIND -> "fe_or_wind";
            case PAUSED -> "paused";
            case OFF -> "off";
            default -> "idle";
        });
    }

    // ------------------------------------------------------------------ menu

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.bettacraft.fertilizer_maker");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FertilizerMakerMenu(id, inv, this, data);
    }

    // ------------------------------------------------------------------ save

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("water", water);
        tag.putInt("fertilizer", fertilizer);
        tag.putInt("progress", progress);
        spring.save(tag, "spring");
        energy.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("items")) items.deserializeNBT(registries, tag.getCompound("items"));
        if (items.getSlots() != SLOTS) items.setSize(SLOTS);
        water = Math.max(0, Math.min(TANK_MB, tag.getInt("water")));
        fertilizer = Math.max(0, Math.min(TANK_MB, tag.getInt("fertilizer")));
        progress = Math.max(0, tag.getInt("progress"));
        spring.load(tag, "spring");
        energy.load(tag);
    }
}
