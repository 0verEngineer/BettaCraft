package org.overengineer.bettacraft.reef;

import org.overengineer.bettacraft.util.SinkList;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.overengineer.bettacraft.config.ServerConfig;

/**
 * M25 part c: a canister of the dosing pump: empty, or full of calcium, alkalinity, magnesium or trace element solution.
 * A full one holds {@code reefDosing.canisterMl} mL; what is used shows as wear (each of the {@link #POINTS} points is a
 * share of it). Used up it becomes an empty canister again, which is refilled in the crafting grid.
 */
public class DosingCanisterItem extends Item {
    public static final int POINTS = 500;
    @Nullable
    private final ReefDosing.Additive additive;

    public DosingCanisterItem(@Nullable ReefDosing.Additive additive, Properties props) {
        super(additive == null ? props : props.durability(POINTS));
        this.additive = additive;
    }

    /** The solution in it (null: an empty canister). */
    @Nullable
    public ReefDosing.Additive additive() {
        return additive;
    }

    /** mL a full canister holds. */
    public static int capacityMl() {
        return ServerConfig.getOr(ServerConfig.DOSING_CANISTER_ML, 500);
    }

    /** mL left in a canister. */
    public static double mlLeft(ItemStack s) {
        if (!(s.getItem() instanceof DosingCanisterItem c) || c.additive == null) return 0;
        return (s.getMaxDamage() - s.getDamageValue()) * (double) capacityMl() / Math.max(1, s.getMaxDamage());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        List<Component> tooltip = new SinkList<>(tooltipAdder);
        if (additive == null) {
            tooltip.add(Component.translatable("tooltip.bettacraft.dosing_canister.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("tooltip.bettacraft.dosing_canister.left", Math.round(mlLeft(stack)), capacityMl()).withStyle(ChatFormatting.GRAY));
    }
}
