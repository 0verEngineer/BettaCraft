package org.overengineer.bettacraft.co2;

import org.overengineer.bettacraft.util.NbtCompat;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.platform.item.IItemHandler;
import org.overengineer.bettacraft.platform.item.ItemStackHandler;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.plumbing.DeviceEnergy;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.registry.ModTags;

/** M25: the CO2 filling station ({@link Co2StationBlock}): one cylinder slot, the FE of the compressor, CO2 gas of other mods. */
public class Co2StationBlockEntity extends BlockEntity {
    public static final int STEP = 20;
    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof Co2CylinderItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            updateState(false);
        }
    };
    /** Hoppers: empty cylinders in, only full ones out. */
    private final IItemHandler automation = new IItemHandler() {
        public int getSlots() {
            return 1;
        }

        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(0);
        }

        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return items.insertItem(0, stack, simulate);
        }

        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack s = items.getStackInSlot(0);
            if (s.isEmpty() || !Co2CylinderItem.full(s)) return ItemStack.EMPTY;
            return items.extractItem(0, amount, simulate);
        }

        public int getSlotLimit(int slot) {
            return 1;
        }

        public boolean isItemValid(int slot, ItemStack stack) {
            return items.isItemValid(0, stack);
        }
    };
    private final DeviceEnergy energy = new DeviceEnergy(20000, this::setChanged);
    /** Fires that burned in the last step (for the status). */
    private int fires;
    private boolean noPower;

    public Co2StationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CO2_STATION.get(), pos, state);
    }

    public ItemStackHandler items() {
        return items;
    }

    public IItemHandler automation() {
        return automation;
    }

    public DeviceEnergy energy() {
        return energy;
    }

    public boolean hasCylinder() {
        return !items.getStackInSlot(0).isEmpty();
    }

    /** Burning fires next to the station (any side). */
    public static int fires(Level level, BlockPos pos) {
        int n = 0;
        for (Direction d : Direction.values()) {
            BlockPos p = pos.relative(d);
            if (!level.isLoaded(p)) continue;
            BlockState s = level.getBlockState(p);
            if (s.is(ModTags.FLUE_GAS_SOURCES) && s.hasProperty(BlockStateProperties.LIT) && s.getValue(BlockStateProperties.LIT)) n++;
        }
        return n;
    }

    /** Pure: see {@link Co2Model#fill}. */
    public static double fill(int fires, long ticks, double capacity, int ticksPerCylinder) {
        return Co2Model.fill(fires, ticks, capacity, ticksPerCylinder);
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, Co2StationBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % STEP != 0) return;
        be.step(level, STEP);
    }

    /** One step of {@code ticks} (public for the GameTests). Returns the bubble-days added. */
    public double step(Level level, int ticks) {
        fires = fires(level, worldPosition);
        noPower = false;
        double added = 0;
        ItemStack s = items.getStackInSlot(0);
        if (TankCo2.enabled() && !s.isEmpty() && !Co2CylinderItem.full(s) && fires > 0) {
            int fe = ServerConfig.getOr(ServerConfig.CO2_STATION_FE, 20) * ticks;
            if (org.overengineer.bettacraft.crucible.CrucibleBlockEntity.energyOffered() && !energy.use(fe)) {
                noPower = true;
            } else {
                added = fill(fires, ticks, Co2CylinderBlockEntity.capacity(), ServerConfig.getOr(ServerConfig.CO2_STATION_TICKS, 24000));
                Co2CylinderItem.setGas(s, Co2CylinderItem.gas(s) + (float) added);
                setChanged();
            }
        }
        updateState(added > 0);
        return added;
    }

    private void updateState(boolean active) {
        if (level == null || level.isClientSide()) return;
        BlockState st = getBlockState();
        BlockState next = st.setValue(Co2StationBlock.ACTIVE, active).setValue(Co2StationBlock.LOADED, hasCylinder());
        if (next != st) level.setBlock(worldPosition, next, Block.UPDATE_ALL);
    }

    // ------------------------------------------------------------------ CO2 of other mods

    private final IFluidHandler gasIn = new IFluidHandler() {
        public int getTanks() {
            return 1;
        }

        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        public int getTankCapacity(int tank) {
            return ServerConfig.getOr(ServerConfig.CO2_FOREIGN_MB, 1000);
        }

        public boolean isFluidValid(int tank, FluidStack stack) {
            return stack.getFluid().is(ModTags.CO2_GAS);
        }

        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !isFluidValid(0, resource) || !TankCo2.enabled()) return 0;
            ItemStack s = items.getStackInSlot(0);
            if (s.isEmpty() || Co2CylinderItem.full(s)) return 0;
            double perMb = Co2CylinderBlockEntity.capacity() / (double) ServerConfig.getOr(ServerConfig.CO2_FOREIGN_MB, 1000);
            int room = (int) Math.ceil((Co2CylinderBlockEntity.capacity() - Co2CylinderItem.gas(s)) / perMb);
            int n = Math.min(resource.getAmount(), Math.max(0, room));
            if (n > 0 && action.execute()) {
                Co2CylinderItem.setGas(s, (float) (Co2CylinderItem.gas(s) + n * perMb));
                setChanged();
            }
            return n;
        }

        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };

    public IFluidHandler gasIn() {
        return gasIn;
    }

    // ------------------------------------------------------------------ interaction

    public void insert(Player player, InteractionHand hand, ItemStack stack) {
        if (hasCylinder()) {
            player.sendOverlayMessage(Component.translatable("message.bettacraft.co2_station.occupied").withStyle(ChatFormatting.GOLD));
            return;
        }
        items.setStackInSlot(0, stack.copyWithCount(1));
        if (!player.getAbilities().instabuild) stack.shrink(1);
        if (level != null) level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON.value(), net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.2F);
        tell(player);
    }

    public void takeOut(Player player) {
        ItemStack s = items.getStackInSlot(0);
        if (s.isEmpty()) return;
        items.setStackInSlot(0, ItemStack.EMPTY);
        if (!player.getInventory().add(s)) player.drop(s, false, net.minecraft.util.Prediction.SERVER_ONLY);
    }

    public void tell(Player player) {
        if (level == null) return;
        Component c;
        int f = fires(level, worldPosition);
        if (!TankCo2.enabled()) c = Component.translatable("message.bettacraft.co2.off").withStyle(ChatFormatting.GOLD);
        else if (!hasCylinder()) c = Component.translatable("message.bettacraft.co2_station.empty", f).withStyle(ChatFormatting.GRAY);
        else {
            ItemStack s = items.getStackInSlot(0);
            int pct = Math.round(100 * Co2CylinderItem.share(s));
            if (Co2CylinderItem.full(s)) c = Component.translatable("message.bettacraft.co2_station.full").withStyle(ChatFormatting.GREEN);
            else if (f <= 0) c = Component.translatable("message.bettacraft.co2_station.no_fire", pct).withStyle(ChatFormatting.GOLD);
            else if (noPower) c = Component.translatable("message.bettacraft.co2_station.no_power", pct).withStyle(ChatFormatting.GOLD);
            else c = Component.translatable("message.bettacraft.co2_station.filling", pct, f).withStyle(ChatFormatting.AQUA);
        }
        player.sendOverlayMessage(c);
    }

    public int lastFires() {
        return fires;
    }

    public boolean lastNoPower() {
        return noPower;
    }

    // ------------------------------------------------------------------ save

    @Override
    protected void saveAdditional(ValueOutput output) {
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = NbtCompat.registries(this);
        super.saveAdditional(output);
        tag.put("items", items.serializeNBT(registries));
        energy.save(tag);
        NbtCompat.write(output, tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        CompoundTag tag = NbtCompat.read(input);
        HolderLookup.Provider registries = input.lookup();
        super.loadAdditional(input);
        if (tag.contains("items")) items.deserializeNBT(registries, tag.getCompoundOrEmpty("items"));
        energy.load(tag);
    }
}
