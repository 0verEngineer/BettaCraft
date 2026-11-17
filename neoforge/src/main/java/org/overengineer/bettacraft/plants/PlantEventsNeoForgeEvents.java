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
import org.overengineer.bettacraft.platform.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import static org.overengineer.bettacraft.plants.PlantEvents.*;

/** M52: the NeoForge event handlers of {@link PlantEvents} (moved here from the common class). */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
final class PlantEventsNeoForgeEvents {
    private PlantEventsNeoForgeEvents() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (e.isCanceled() || !(e.getLevel() instanceof ServerLevel level) || !InteriorManager.isInterior(level)) return;
        BlockState s = e.getState();
        if (!(s.getBlock() instanceof AquaticPlantBlock)) return;
        InteriorPlants.onBreak(level, e.getPos(), s, e.getPlayer());
    }

    /** M49: a mangrove propagule placed in an interior grows into a mangrove of its tank. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (e.isCanceled() || !(e.getLevel() instanceof ServerLevel level) || !InteriorManager.isInterior(level)) return;
        if (e.getPlacedBlock().is(Blocks.MANGROVE_PROPAGULE)) Mangroves.placedInInterior(level, e.getPos());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (propagule(e)) return; // M49
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

    /** M49: a mangrove propagule used on a tank from outside is planted at the floor under the spot looked at. */
    static boolean propagule(PlayerInteractEvent.RightClickBlock e) {
        ItemStack stack = e.getItemStack();
        Level level = e.getLevel();
        if (!stack.is(net.minecraft.world.item.Items.MANGROVE_PROPAGULE) || !(level.getBlockState(e.getPos()).getBlock() instanceof TankBlock)) return false;
        if (!Mangroves.enabled() || InteriorManager.isInterior(level)) return false;
        TankBlockEntity c = TankMultiblock.getController(level, e.getPos());
        if (c == null || c.getTank() == null) return false;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (!(level instanceof ServerLevel server)) return true;
        Player player = e.getEntity();
        if (!TankInhabitants.canModify(player, c.getTank())) {
            player.displayClientMessage(Component.translatable("message.bettacraft.tank.locked").withStyle(ChatFormatting.RED), true);
            return true;
        }
        var r = org.overengineer.bettacraft.tank.interior.InteriorManager.ensureRegion(server, c);
        int scale = r == null ? 16 : r.scale();
        var hit = e.getHitVec().getLocation();
        int lx = (int) Math.floor((hit.x - c.getTank().min().getX()) * scale), lz = (int) Math.floor((hit.z - c.getTank().min().getZ()) * scale);
        String err = Mangroves.plant(server, c, lx, lz);
        if (err != null) {
            player.displayClientMessage(Component.translatable(err).withStyle(ChatFormatting.GOLD), true);
            return true;
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(null, e.getPos(), SoundEvents.MANGROVE_ROOTS_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
        org.overengineer.bettacraft.tank.TankCare.log(server, c, "log.bettacraft.mangrove.planted", org.overengineer.bettacraft.tank.TankCare.nameArg(player));
        player.displayClientMessage(Component.translatable("message.bettacraft.mangrove.planted").withStyle(ChatFormatting.GREEN), true);
        return true;
    }
}
