package org.overengineer.bettacraft.equipment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.registry.ModBlockEntities;

/**
 * M23: the air stone in a tank interior. It only remembers where its bubbles reach the water surface (interior y, set
 * by the server with the care step) so the client can draw the bubble column inside the tank; the client tick spawns
 * the bubbles and plays the quiet bubbling.
 */
public class AirStoneBlockEntity extends BlockEntity {
    /** Interior y of the water surface above the stone (NaN = unknown: the bubbles rise 6 blocks). */
    private float surfaceY = Float.NaN;

    public AirStoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AIR_STONE.get(), pos, state);
    }

    public float surfaceY() {
        return surfaceY;
    }

    /** Server: sets the surface height; sends it to the clients only when it changed. */
    public void setSurfaceY(float y) {
        if (!Float.isNaN(surfaceY) && Math.abs(surfaceY - y) < 0.01F) return;
        surfaceY = y;
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    static void clientTick(Level level, BlockPos pos, BlockState state, AirStoneBlockEntity be) {
        org.overengineer.bettacraft.util.ClientHooks.bubbles.tick(level, pos, state, be.surfaceY);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!Float.isNaN(surfaceY)) tag.putFloat("surface", surfaceY);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        surfaceY = tag.contains("surface") ? tag.getFloat("surface") : Float.NaN;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag t = new CompoundTag();
        if (!Float.isNaN(surfaceY)) t.putFloat("surface", surfaceY);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
