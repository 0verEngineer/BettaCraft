package org.overengineer.bettacraft.genetics;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M22.3: the magnifier analyses the hidden genes of the animals in a tank: right-click on the tank analyses every animal
 * in it, the "Analyse" button of the tank menu one selected animal. Each analysed animal wears it by one (64 uses,
 * repaired at the anvil with glass panes). The result stays with the animal ({@link GeneVisibility}).
 */
public class MagnifierItem extends Item {
    public MagnifierItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(Items.GLASS_PANE) || repair.is(net.neoforged.neoforge.common.Tags.Items.GLASS_PANES) || super.isValidRepairItem(stack, repair);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.bettacraft.magnifier").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.bettacraft.magnifier.menu").withStyle(ChatFormatting.DARK_GRAY));
        if (GeneVisibility.free()) tooltip.add(Component.translatable("tooltip.bettacraft.magnifier.free").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** Right-click on a tank: every animal in it that is not analysed yet. */
    public static ItemInteractionResult useOnTank(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!(stack.getItem() instanceof MagnifierItem)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        TankBlockEntity c = TankMultiblock.getController(level, pos);
        if (c == null) return ItemInteractionResult.CONSUME;
        int n = analyseAll((ServerLevel) level, c, player, stack, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        player.displayClientMessage(n > 0 ? Component.translatable("message.bettacraft.magnifier.analysed", n).withStyle(ChatFormatting.GREEN)
                : Component.translatable(GeneVisibility.free() ? "message.bettacraft.magnifier.free" : "message.bettacraft.magnifier.none"), true);
        return ItemInteractionResult.CONSUME;
    }

    /** Analyses every animal of the tank not analysed yet (one use each, as long as the magnifier lasts); returns how many. */
    public static int analyseAll(ServerLevel level, TankBlockEntity c, Player player, ItemStack magnifier, @Nullable EquipmentSlot slot) {
        TankData t = c.getTank();
        if (GeneVisibility.free()) return 0;
        int n = 0;
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) {
            if (magnifier.isEmpty()) break;
            if (analyse(e)) {
                n++;
                wear(player, magnifier, slot);
            }
        }
        for (Critter k : t.critters()) {
            if (magnifier.isEmpty()) break;
            if (!k.alive() || k.analysed) continue;
            k.analysed = true;
            n++;
            wear(player, magnifier, slot);
        }
        if (n > 0) {
            level.playSound(null, c.getBlockPos(), SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
            TankCare.log(level, c, "log.bettacraft.analysed", TankCare.nameArg(player), String.valueOf(n));
            c.markTankChanged();
        }
        return n;
    }

    /** Marks one entity analysed; false if it has no hidden genes or was analysed already. */
    public static boolean analyse(Entity e) {
        if (e instanceof BettaEntity b) {
            if (b.getBettaData().analysed()) return false;
            b.setBettaData(b.getBettaData().withAnalysed(true));
            return true;
        }
        if (e instanceof TankmateEntity m) {
            if (m.analysed()) return false;
            m.setAnalysed(true);
            return true;
        }
        return false;
    }

    /** The magnifier of the player (main hand, off hand, then the inventory), or empty. */
    public static ItemStack find(Player player) {
        if (player.getMainHandItem().getItem() instanceof MagnifierItem) return player.getMainHandItem();
        if (player.getOffhandItem().getItem() instanceof MagnifierItem) return player.getOffhandItem();
        for (ItemStack s : player.getInventory().items) if (s.getItem() instanceof MagnifierItem) return s;
        return ItemStack.EMPTY;
    }

    public static void wear(Player player, ItemStack magnifier, @Nullable EquipmentSlot slot) {
        if (player.getAbilities().instabuild) return;
        if (slot != null) {
            magnifier.hurtAndBreak(1, player, slot);
        } else if (player.level() instanceof ServerLevel sl) {
            magnifier.hurtAndBreak(1, sl, player instanceof net.minecraft.server.level.ServerPlayer sp ? sp : null, item -> { });
        }
    }
}
