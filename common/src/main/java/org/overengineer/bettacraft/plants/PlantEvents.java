package org.overengineer.bettacraft.plants;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.Tags;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;

/**
 * Player actions around the aquarium plants (M13): breaking plant blocks in the interior (root = take out, rhizome =
 * divide), shears on the tank from outside (trim the plant looked at in the miniature) and shears on a moss block
 * next to water (Java moss).
 */
public final class PlantEvents {
    private PlantEvents() {}





    /** Whether water touches the block on any side. */
    public static boolean nextToWater(Level level, BlockPos pos) {
        for (Direction d : Direction.values()) if (level.getFluidState(pos.relative(d)).is(FluidTags.WATER)) return true;
        return false;
    }
}
