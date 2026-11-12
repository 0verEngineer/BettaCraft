package org.overengineer.bettacraft.tank;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/** Item interactions with a tank: water bucket fills, empty bucket drains (PLAN 3.2). */
public final class TankInteractions {
    private TankInteractions() {}

    public static ItemInteractionResult useItemOn(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand,
                                                  net.minecraft.world.phys.BlockHitResult hit) {
        ItemInteractionResult r = TankInhabitants.tryInsert(stack, level, pos, player, hand);
        if (r != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return r;
        r = TankCare.handFeed(stack, level, pos, player, hand, hit.getLocation());
        if (r != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return r;
        boolean fill = stack.is(Items.WATER_BUCKET);
        boolean drain = stack.is(Items.BUCKET);
        if (!fill && !drain) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        ServerLevel server = (ServerLevel) level;
        TankBlockEntity controller = TankMultiblock.getController(level, pos);
        if (controller == null) {
            return ItemInteractionResult.CONSUME;
        }
        TankData tank = controller.getTank();
        if (!TankInhabitants.canModify(player, tank)) {
            player.displayClientMessage(Component.translatable("message.bettacraft.tank.locked"), true);
            return ItemInteractionResult.CONSUME;
        }
        if (fill) {
            if (tank.waterMb() + TankData.MB_PER_BLOCK > tank.capacityMb()) {
                player.displayClientMessage(Component.translatable("message.bettacraft.tank.full"), true);
                return ItemInteractionResult.CONSUME;
            }
            changeWater(server, controller, TankData.MB_PER_BLOCK);
            if (!player.getAbilities().instabuild) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
            }
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
        } else {
            if (tank.waterMb() < TankData.MB_PER_BLOCK) {
                player.displayClientMessage(Component.translatable("message.bettacraft.tank.not_enough_water"), true);
                return ItemInteractionResult.CONSUME;
            }
            double minFill = org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.TANK_MIN_FILL_TO_INSERT, 0.5);
            if (tank.waterMb() - TankData.MB_PER_BLOCK < minFill * tank.capacityMb() && !TankInhabitants.liveInhabitants(server, tank).isEmpty()) {
                // keep the inhabitants in water
                player.displayClientMessage(Component.translatable("message.bettacraft.tank.keep_water", Math.round(minFill * 100)), true);
                return ItemInteractionResult.CONSUME;
            }
            changeWater(server, controller, -TankData.MB_PER_BLOCK);
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.WATER_BUCKET)));
            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        }
        return ItemInteractionResult.CONSUME;
    }

    /** Adds (or removes, if negative) water and updates the block states bottom-up. */
    public static void changeWater(ServerLevel level, TankBlockEntity controller, long deltaMb) {
        TankData tank = controller.getTank();
        tank.setWaterMb(tank.waterMb() + deltaMb);
        TankMultiblock.applyStates(level, tank);
        controller.markTankChanged();
    }

    public static void showInfo(ServerPlayer player, BlockPos pos) {
        TankBlockEntity controller = TankMultiblock.getController(player.level(), pos);
        TankBlockEntity be = TankMultiblock.getBE(player.level(), pos);
        if (controller == null || be == null) {
            player.displayClientMessage(Component.translatable("message.bettacraft.tank.not_formed"), true);
            return;
        }
        TankData t = controller.getTank();
        player.displayClientMessage(Component.translatable("message.bettacraft.tank.info",
                t.sizeX(), t.sizeY(), t.sizeZ(), t.waterMb(), t.capacityMb(), t.scale(), be.getGlassId().toString()), true);
    }
}
