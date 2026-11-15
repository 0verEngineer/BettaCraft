package org.overengineer.bettacraft.terrarium;

import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.overengineer.bettacraft.equipment.AirPumpBlockEntity;
import org.overengineer.bettacraft.plumbing.WaterSources;
import org.overengineer.bettacraft.plumbing.WaterStore;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/**
 * M31: the mister ({@link MisterBlock}): 4 buckets of water and a hygrostat setting. {@link TankClimate} asks it every
 * second whether it sprays ({@link #wants}) and takes the water ({@link #spray}); its own tick refills it from what is next
 * to it (a reservoir, pipes, a water source block) and turns the spray off again.
 */
public class MisterBlockEntity extends BlockEntity {
    public static final int CAPACITY = 4000, MIN_SETTING = 40, MAX_SETTING = 95, DEFAULT_SETTING = 75;
    private final WaterStore water = new WaterStore(CAPACITY, this::changed);
    private int setting = DEFAULT_SETTING;
    private long sprayUntil = Long.MIN_VALUE;
    private double carry;

    public MisterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MISTER.get(), pos, state);
    }

    public WaterStore water() {
        return water;
    }

    /** The fluid handler for pipes and other mods (water goes in, nothing comes out). */
    public IFluidHandler fluidHandler() {
        return new IFluidHandler() {
            @Override public int getTanks() { return 1; }
            @Override public FluidStack getFluidInTank(int tank) { return water.getFluidInTank(tank); }
            @Override public int getTankCapacity(int tank) { return CAPACITY; }
            @Override public boolean isFluidValid(int tank, FluidStack stack) { return water.isFluidValid(tank, stack); }
            @Override public int fill(FluidStack resource, FluidAction action) { return water.fill(resource, action); }
            @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
            @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
        };
    }

    public int setting() {
        return setting;
    }

    public void setSetting(int pct) {
        setting = Math.max(MIN_SETTING, Math.min(MAX_SETTING, pct));
        setChanged();
    }

    public int amount() {
        return water.amount();
    }

    /** The tank the mister serves: the tank block under it or beside it. */
    @Nullable
    public TankBlockEntity tank() {
        return level == null ? null : AirPumpBlockEntity.findTank(level, worldPosition);
    }

    /** Whether it wants to spray at this humidity (a hygrostat with a little hysteresis while it already sprays). */
    public boolean wants(float humidity, long now) {
        if (!org.overengineer.bettacraft.config.CommonConfig.terrariumDevices()) return false;
        float on = spraying(now) ? setting + 2 : setting;
        return !(humidity >= on);
    }

    public boolean spraying(long now) {
        return now < sprayUntil;
    }

    /** Sprays for {@code days} (one care step): takes the water; false (and no spray) when it is dry. */
    public boolean spray(long now, double seconds) {
        double need = org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.TERRA_MISTER_MB, 2.0) * seconds + carry;
        int whole = (int) Math.floor(need);
        if (whole > water.amount()) return false;
        if (whole > 0) water.drain(whole, IFluidHandler.FluidAction.EXECUTE);
        carry = need - whole;
        sprayUntil = now + 30;
        setMisting(true);
        return true;
    }

    private void setMisting(boolean on) {
        if (level == null || level.isClientSide) return;
        BlockState s = getBlockState();
        if (s.hasProperty(MisterBlock.MISTING) && s.getValue(MisterBlock.MISTING) != on) level.setBlock(worldPosition, s.setValue(MisterBlock.MISTING, on), Block.UPDATE_CLIENTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MisterBlockEntity be) {
        long now = level.getGameTime();
        if ((now + pos.asLong()) % 20 != 0) return;
        if (!be.spraying(now)) be.setMisting(false);
        // refill from what is next to it: a water source block (endless), a reservoir, pipes, other mods' tanks
        int room = CAPACITY - be.water.amount();
        if (room >= 250) {
            if (WaterSources.endless(level, pos)) {
                be.water.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, room), IFluidHandler.FluidAction.EXECUTE);
            } else {
                for (WaterSources.Portion p : WaterSources.take(level, pos, Math.min(room, 250), true)) {
                    be.water.fill(org.overengineer.bettacraft.tank.WaterValues.stack(p.mb(), p.values()), IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

    /** A bucket or another fluid container of water fills it. */
    public boolean fillFrom(ItemStack stack, Player player, InteractionHand hand) {
        if (level == null || !FluidUtil.getFluidHandler(stack).isPresent()) return false;
        return FluidUtil.interactWithFluidHandler(player, hand, fluidHandler());
    }

    /** Right-click: the setting up (or down when sneaking) and what it does now. */
    public void adjust(Player player, int delta) {
        setSetting(setting + delta);
        TankBlockEntity c = tank();
        Component rh = c == null || !c.getTank().hasClimate() ? Component.translatable("message.bettacraft.mister.no_tank")
                : Component.translatable("message.bettacraft.mister.now", Math.round(c.getTank().humidity()));
        player.displayClientMessage(Component.translatable("message.bettacraft.mister.state", setting, water.amount(), CAPACITY, rh)
                .withStyle(water.amount() <= 0 ? ChatFormatting.GOLD : ChatFormatting.AQUA), true);
    }

    public int comparator() {
        return water.amount() <= 0 ? 0 : 1 + (int) Math.floor(14.0 * water.amount() / CAPACITY);
    }

    private void changed() {
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        water.save(tag, registries);
        tag.putInt("setting", setting);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        water.load(tag, registries);
        setting = tag.contains("setting") ? Math.max(MIN_SETTING, Math.min(MAX_SETTING, tag.getInt("setting"))) : DEFAULT_SETTING;
    }
}
