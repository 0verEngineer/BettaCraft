package org.overengineer.bettacraft.equipment;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M21.5: the clip-on feeder: hangs on the outside of the glass at the top edge (for tanks whose top is taken by the
 * light), one stack of food, 1-3 feedings a day like the big feeder. No hopper, no menu (right-click: put food in or
 * change the feedings, sneak: take the food out), does not cover the water against evaporation.
 */
public class ClipFeederBlockEntity extends FeederBlockEntity {
    /** M66: two slots (before one), with a menu like the feeder's. */
    public static final int SLOTS = 2;

    public ClipFeederBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CLIP_FEEDER.get(), pos, state, SLOTS);
    }

    @Override
    public net.minecraft.world.inventory.MenuType<FeederMenu> menuType() {
        return org.overengineer.bettacraft.registry.ModMenus.CLIP_FEEDER.get();
    }

    @Override
    protected boolean active() {
        return Features.enabled(Feature.CLIP_FEEDER);
    }

    /** The tank block it hangs on (behind it). */
    public BlockPos tankPos() {
        Direction d = getBlockState().hasProperty(ClipFeederBlock.FACING) ? getBlockState().getValue(ClipFeederBlock.FACING) : Direction.NORTH;
        return worldPosition.relative(d.getOpposite());
    }

    @Nullable
    @Override
    public TankBlockEntity tankBelow() {
        return level == null ? null : TankMultiblock.getController(level, tankPos());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.bettacraft.clip_feeder");
    }
}
