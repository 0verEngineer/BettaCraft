package org.overengineer.bettacraft.plumbing;

import org.overengineer.bettacraft.util.NbtCompat;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.tank.WaterValues;

/** M24: the reservoir: 16 buckets of water (with values) for the top-off and the water change controller. */
public class ReservoirBlockEntity extends BlockEntity {
    public static final int CAPACITY = 16000;
    private final WaterStore water = new WaterStore(CAPACITY, this::changed);

    public ReservoirBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESERVOIR.get(), pos, state);
    }

    public WaterStore water() {
        return water;
    }

    private void changed() {
        setChanged();
        if (level == null || level.isClientSide()) return;
        BlockState s = getBlockState();
        int fill = (int) Math.ceil(8.0 * water.amount() / CAPACITY);
        if (s.hasProperty(WaterDeviceBlock.FILL) && s.getValue(WaterDeviceBlock.FILL) != fill) {
            level.setBlock(worldPosition, s.setValue(WaterDeviceBlock.FILL, fill), Block.UPDATE_ALL);
        }
    }

    public void tellFill(Player player) {
        WaterValues v = water.values();
        Component kind = Component.translatable(v == null ? "message.bettacraft.reservoir.tap" : v.fresh() ? "message.bettacraft.reservoir.fresh"
                : "message.bettacraft.reservoir.salty", v == null ? "" : String.format(java.util.Locale.ROOT, "%.1f", v.salinity()));
        player.displayClientMessage(Component.translatable("message.bettacraft.reservoir.fill", water.amount(), CAPACITY, kind).withStyle(ChatFormatting.AQUA), true);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = NbtCompat.registries(this);
        super.saveAdditional(output);
        water.save(tag, registries);
        NbtCompat.write(output, tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        CompoundTag tag = NbtCompat.read(input);
        HolderLookup.Provider registries = input.lookup();
        super.loadAdditional(input);
        water.load(tag, registries);
    }
}
