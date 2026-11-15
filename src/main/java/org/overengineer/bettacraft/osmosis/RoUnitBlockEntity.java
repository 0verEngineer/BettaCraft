package org.overengineer.bettacraft.osmosis;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.plumbing.DeviceEnergy;
import org.overengineer.bettacraft.plumbing.FluidMoves;
import org.overengineer.bettacraft.plumbing.WaterSources;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.registry.ModFluids;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.WaterValues;

/**
 * M25: the reverse osmosis unit ({@link RoUnitBlock}): the feed water (any water, also sea water; a water source block
 * next to it is endless), the reverse osmosis water it made, membrane and prefilter (slots 0 and 1) and the FE of the
 * pressure pump.
 */
public class RoUnitBlockEntity extends BlockEntity implements org.overengineer.bettacraft.winding.Windable {
    public static final int STEP = 20, FEED_CAPACITY = 4000, PURE_CAPACITY = 8000;
    public static final int MEMBRANE = 0, PREFILTER = 1;
    private final ItemStackHandler parts = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == MEMBRANE ? stack.is(ModItems.RO_MEMBRANE.get()) : stack.is(ModItems.RO_PREFILTER.get());
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
    private final DeviceEnergy energy = new DeviceEnergy(20000, this::setChanged);
    private int feed, pure;
    /** Made but not yet whole mB, and the wear not yet a whole point of the parts. */
    private double carry, wearMembrane, wearPrefilter;
    /** mB per tick in the last step (0 = standing still), and what stopped it. */
    private double lastRate;
    private String stop = "";
    private boolean boosted;

    public RoUnitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RO_UNIT.get(), pos, state);
    }

    public ItemStackHandler parts() {
        return parts;
    }

    public DeviceEnergy energy() {
        return energy;
    }

    /** M37: the spring the crank winds ({@code features.power.winding}): the pressure pump boosts on it while its FE is short. */
    private final org.overengineer.bettacraft.winding.WindSpring spring =
            org.overengineer.bettacraft.winding.WindSpring.of(org.overengineer.bettacraft.winding.Winding.Device.OSMOSIS, this::setChanged);

    @Override
    public org.overengineer.bettacraft.winding.WindSpring windSpring() {
        return spring;
    }

    @Override
    public org.overengineer.bettacraft.winding.Winding.Device windDevice() {
        return org.overengineer.bettacraft.winding.Winding.Device.OSMOSIS;
    }

    public int feed() {
        return feed;
    }

    public int pure() {
        return pure;
    }

    public void setFeed(int mb) {
        feed = Math.max(0, Math.min(FEED_CAPACITY, mb));
        setChanged();
    }

    public void setPure(int mb) {
        pure = Math.max(0, Math.min(PURE_CAPACITY, mb));
        setChanged();
    }

    public double lastRate() {
        return lastRate;
    }

    public boolean boosted() {
        return boosted;
    }

    public String stopReason() {
        return stop;
    }

    // ------------------------------------------------------------------ pure model

    /** Pure: mB of feed water for {@code ro} mB of reverse osmosis water with {@code wastePerPart} parts of waste. */
    public static double feedFor(double ro, double wastePerPart) {
        return ro * (1 + Math.max(0, wastePerPart));
    }

    /** Pure: mB of reverse osmosis water from this much feed. */
    public static double roFrom(double feed, double wastePerPart) {
        return feed / (1 + Math.max(0, wastePerPart));
    }

    /** Pure: wear points (of 100) of a part that lasts {@code buckets} buckets after {@code mb} mB. */
    public static double wear(double mb, double buckets) {
        return mb * 100.0 / (Math.max(0.001, buckets) * 1000.0);
    }

    // ------------------------------------------------------------------ work

    static void serverTick(Level level, BlockPos pos, BlockState state, RoUnitBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % STEP != 0) return;
        be.step(level, STEP);
    }

    /** One step of {@code ticks} (public for the GameTests). Returns the mB of reverse osmosis water made. */
    public int step(Level level, int ticks) {
        int made = 0;
        boosted = false;
        stop = "";
        double waste = ServerConfig.getOr(ServerConfig.RO_WASTE, 3.0);
        boolean endless = WaterSources.endless(level, worldPosition);
        if (!Features.enabled(Feature.OSMOSIS)) stop = "off";
        else if (parts.getStackInSlot(MEMBRANE).isEmpty()) stop = "no_membrane";
        else if (parts.getStackInSlot(PREFILTER).isEmpty()) stop = "no_prefilter";
        else if (pure >= PURE_CAPACITY) stop = "full";
        else if (!endless && feed < 1 + waste) stop = "no_water";
        else {
            double rate = ServerConfig.getOr(ServerConfig.RO_RATE, 1.0);
            int fe = ServerConfig.getOr(ServerConfig.RO_FE, 20) * ticks;
            // M32 part c: Create's rotation drives the pressure pump (the boost) like FE, also without the power options
            boolean turned = org.overengineer.bettacraft.compat.create.CreateKinetics.drive(level, worldPosition) > 0F;
            if (turned) energy.drive(level, worldPosition, fe);
            int fePerTick = ServerConfig.getOr(ServerConfig.RO_FE, 20);
            if ((org.overengineer.bettacraft.crucible.CrucibleBlockEntity.energyOffered() || turned) && org.overengineer.bettacraft.winding.Winding.canPay(energy, this, fe, fePerTick)) {
                org.overengineer.bettacraft.winding.Winding.pay(energy, this, fe, fePerTick); // M37: FE, else the spring
                rate *= ServerConfig.getOr(ServerConfig.RO_BOOST, 4.0);
                boosted = true;
            }
            double want = rate * ticks + carry;
            double can = Math.min(want, PURE_CAPACITY - pure);
            if (!endless) can = Math.min(can, roFrom(feed, waste));
            made = (int) Math.floor(can);
            carry = can - made;
            if (carry >= 1) carry = 0;
            if (made > 0) {
                pure += made;
                double used = feedFor(made, waste);
                if (!endless) feed = Math.max(0, feed - (int) Math.ceil(used - 1e-9));
                dumpWaste(level, (int) Math.round(used - made));
                wearMembrane += wear(made, ServerConfig.getOr(ServerConfig.RO_MEMBRANE, 128.0));
                wearPrefilter += wear(made, ServerConfig.getOr(ServerConfig.RO_PREFILTER, 32.0));
                wearMembrane = damage(MEMBRANE, wearMembrane, level);
                wearPrefilter = damage(PREFILTER, wearPrefilter, level);
                setChanged();
            }
        }
        lastRate = made / (double) ticks;
        BlockState s = getBlockState();
        if (s.getValue(RoUnitBlock.ACTIVE) != made > 0) level.setBlock(worldPosition, s.setValue(RoUnitBlock.ACTIVE, made > 0), Block.UPDATE_ALL);
        return made;
    }

    /** Whole wear points of a part: it takes damage and breaks at the end. Returns the remaining fraction. */
    private double damage(int slot, double wear, Level level) {
        int points = (int) Math.floor(wear);
        if (points <= 0) return wear;
        ItemStack s = parts.getStackInSlot(slot);
        if (s.isEmpty()) return 0;
        int d = s.getDamageValue() + points;
        if (d >= s.getMaxDamage()) {
            parts.setStackInSlot(slot, ItemStack.EMPTY);
            level.playSound(null, worldPosition, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 0.6F, 1.0F);
        } else {
            s.setDamageValue(d);
            parts.setStackInSlot(slot, s);
        }
        return wear - points;
    }

    /** The waste water (with the minerals the membrane kept) goes into a pipe or a tank below, otherwise down the drain. */
    private void dumpWaste(Level level, int mb) {
        if (mb <= 0) return;
        IFluidHandler below = FluidMoves.neighbour(level, worldPosition, Direction.DOWN);
        if (below == null) return;
        WaterValues v = new WaterValues(WaterValues.FLUID_TEMP, 0F, 0F, 0.9F, 0F,
                org.overengineer.bettacraft.chem.WaterChemistry.clampTds(org.overengineer.bettacraft.chem.WaterChemistry.TAP_TDS * 1.33F), org.overengineer.bettacraft.chem.WaterChemistry.TAP_PH);
        below.fill(WaterValues.stack(mb, v), IFluidHandler.FluidAction.EXECUTE);
    }

    // ------------------------------------------------------------------ fluids

    private final IFluidHandler fluid = new IFluidHandler() {
        public int getTanks() {
            return 2;
        }

        public FluidStack getFluidInTank(int tank) {
            if (tank == 0) return feed <= 0 ? FluidStack.EMPTY : new FluidStack(net.minecraft.world.level.material.Fluids.WATER, feed);
            return pure <= 0 ? FluidStack.EMPTY : new FluidStack(ModFluids.RO_WATER.get(), pure);
        }

        public int getTankCapacity(int tank) {
            return tank == 0 ? FEED_CAPACITY : PURE_CAPACITY;
        }

        public boolean isFluidValid(int tank, FluidStack stack) {
            // any water but its own (sea water too: the membrane holds the salt back)
            return tank == 0 && ModFluids.isWater(stack.getFluid()) && !stack.getFluid().isSame(ModFluids.RO_WATER.get());
        }

        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;
            int n = Math.min(resource.getAmount(), FEED_CAPACITY - feed);
            if (n > 0 && action.execute()) {
                feed += n;
                setChanged();
            }
            return Math.max(0, n);
        }

        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !resource.getFluid().isSame(ModFluids.RO_WATER.get())) return FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }

        public FluidStack drain(int maxDrain, FluidAction action) {
            int n = Math.min(maxDrain, pure);
            if (n <= 0) return FluidStack.EMPTY;
            if (action.execute()) {
                pure -= n;
                setChanged();
            }
            return new FluidStack(ModFluids.RO_WATER.get(), n);
        }
    };

    public IFluidHandler fluid() {
        return fluid;
    }

    // ------------------------------------------------------------------ interaction

    public boolean accepts(ItemStack s) {
        return s.is(ModItems.RO_MEMBRANE.get()) || s.is(ModItems.RO_PREFILTER.get()) || s.is(Items.WATER_BUCKET) || s.is(ModItems.SEA_WATER_BUCKET.get())
                || s.is(Items.BUCKET);
    }

    public void use(Player player, InteractionHand hand, ItemStack stack) {
        if (level == null) return;
        if (stack.is(ModItems.RO_MEMBRANE.get()) || stack.is(ModItems.RO_PREFILTER.get())) {
            int slot = stack.is(ModItems.RO_MEMBRANE.get()) ? MEMBRANE : PREFILTER;
            ItemStack old = parts.getStackInSlot(slot);
            parts.setStackInSlot(slot, stack.copyWithCount(1));
            if (!player.getAbilities().instabuild) stack.shrink(1);
            if (!old.isEmpty() && !player.getInventory().add(old)) player.drop(old, false);
            if (slot == MEMBRANE) wearMembrane = 0;
            else wearPrefilter = 0;
            level.playSound(null, worldPosition, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.BLOCKS, 0.6F, 1.3F);
            player.displayClientMessage(Component.translatable(slot == MEMBRANE ? "message.bettacraft.ro.membrane_in" : "message.bettacraft.ro.prefilter_in")
                    .withStyle(ChatFormatting.GREEN), true);
        } else if (stack.is(Items.BUCKET)) {
            if (pure < 1000) {
                player.displayClientMessage(Component.translatable("message.bettacraft.ro.not_enough", pure).withStyle(ChatFormatting.GOLD), true);
                return;
            }
            pure -= 1000;
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(ModItems.RO_WATER_BUCKET.get())));
            level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else {
            if (feed + 1000 > FEED_CAPACITY) {
                player.displayClientMessage(Component.translatable("message.bettacraft.ro.feed_full").withStyle(ChatFormatting.GOLD), true);
                return;
            }
            feed += 1000;
            if (!player.getAbilities().instabuild) player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
            level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        setChanged();
    }

    /** Sneaking with an empty hand: the prefilter, then the membrane come out. */
    public void takeParts(Player player) {
        for (int slot : new int[]{PREFILTER, MEMBRANE}) {
            ItemStack s = parts.getStackInSlot(slot);
            if (s.isEmpty()) continue;
            parts.setStackInSlot(slot, ItemStack.EMPTY);
            if (!player.getInventory().add(s)) player.drop(s, false);
            return;
        }
        tell(player);
    }

    private static int left(ItemStack s) {
        return s.isEmpty() ? 0 : Math.round(100F * (s.getMaxDamage() - s.getDamageValue()) / Math.max(1, s.getMaxDamage()));
    }

    public void tell(Player player) {
        Component state;
        if (!stop.isEmpty()) state = Component.translatable("message.bettacraft.ro.stop." + stop).withStyle(ChatFormatting.GOLD);
        else state = Component.translatable(boosted ? "message.bettacraft.ro.running_pump" : "message.bettacraft.ro.running",
                String.format(java.util.Locale.ROOT, "%.1f", lastRate)).withStyle(ChatFormatting.GREEN);
        player.displayClientMessage(Component.translatable("message.bettacraft.ro.status", state, pure, feed,
                left(parts.getStackInSlot(MEMBRANE)), left(parts.getStackInSlot(PREFILTER))), true);
    }

    // ------------------------------------------------------------------ save

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("parts", parts.serializeNBT(registries));
        tag.putInt("feed", feed);
        tag.putInt("pure", pure);
        tag.putDouble("carry", carry);
        tag.putDouble("wear_membrane", wearMembrane);
        tag.putDouble("wear_prefilter", wearPrefilter);
        energy.save(tag);
        spring.save(tag, "spring");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("parts")) parts.deserializeNBT(registries, tag.getCompound("parts"));
        feed = Math.max(0, Math.min(FEED_CAPACITY, tag.getInt("feed")));
        pure = Math.max(0, Math.min(PURE_CAPACITY, tag.getInt("pure")));
        carry = tag.getDouble("carry");
        wearMembrane = tag.getDouble("wear_membrane");
        wearPrefilter = tag.getDouble("wear_prefilter");
        energy.load(tag);
        spring.load(tag, "spring");
    }
}
