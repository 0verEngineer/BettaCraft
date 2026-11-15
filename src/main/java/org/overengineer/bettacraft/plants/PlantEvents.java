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
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
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
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class PlantEvents {
    private PlantEvents() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (e.isCanceled() || !(e.getLevel() instanceof ServerLevel level) || !InteriorManager.isInterior(level)) return;
        BlockState s = e.getState();
        if (!(s.getBlock() instanceof AquaticPlantBlock)) return;
        InteriorPlants.onBreak(level, e.getPos(), s, e.getPlayer());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        ItemStack stack = e.getItemStack();
        if (!stack.is(Tags.Items.TOOLS_SHEAR)) return;
        Level level = e.getLevel();
        BlockPos pos = e.getPos();
        BlockState state = level.getBlockState(pos);
        Player player = e.getEntity();
        if (state.getBlock() instanceof TankBlock) {
            TankBlockEntity c = TankMultiblock.getController(level, pos);
            if (c == null || c.getTank() == null) return;
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            if (!(level instanceof ServerLevel server)) return;
            if (!TankInhabitants.canModify(player, c.getTank())) {
                player.displayClientMessage(Component.translatable("message.bettacraft.tank.locked").withStyle(ChatFormatting.RED), true);
                return;
            }
            InteriorPlants.trimFromOutside(server, c, player, stack, e.getHand());
            return;
        }
        if (state.is(Blocks.MOSS_BLOCK) && nextToWater(level, pos) && org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.AQUATIC_PLANTS)) { // M33
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            if (level.isClientSide) return;
            level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.8F, 1.2F);
            if (!player.getAbilities().instabuild) {
                stack.hurtAndBreak(1, player, e.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
            if (level.getRandom().nextDouble() < ServerConfig.getOr(ServerConfig.AQUATIC_MOSS_CHANCE, 0.5)) {
                ItemStack moss = InteriorPlants.item(PlantSpecies.JAVA_MOSS, 0);
                if (!player.getInventory().add(moss)) player.drop(moss, false);
                player.displayClientMessage(Component.translatable("message.bettacraft.aquatic.moss_found").withStyle(ChatFormatting.GREEN), true);
            }
        }
    }

    /** Whether water touches the block on any side. */
    public static boolean nextToWater(Level level, BlockPos pos) {
        for (Direction d : Direction.values()) if (level.getFluidState(pos.relative(d)).is(FluidTags.WATER)) return true;
        return false;
    }
}
