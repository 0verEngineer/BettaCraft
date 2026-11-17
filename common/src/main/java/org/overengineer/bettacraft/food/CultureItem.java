package org.overengineer.bettacraft.food;

import org.overengineer.bettacraft.util.SinkList;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModDataComponents;

/**
 * A food culture that becomes ready after {@code food.cultureDays} (PLAN 6.3): infusoria (swamp water + plant matter) or
 * artemia nauplii (salt water + cysts, needs light). The start time is stamped on the first tick in an inventory or as a
 * dropped item; the culture turns into the food item once enough game time has passed and it is ticked again (also
 * after being stored in a chest). Light (level 9+) is checked when it finishes.
 */
public class CultureItem extends Item {
    public static final int MIN_LIGHT = 9;
    private final Supplier<? extends Item> result;
    private final boolean needsLight;

    public CultureItem(Supplier<? extends Item> result, boolean needsLight, Properties properties) {
        super(properties);
        this.result = result;
        this.needsLight = needsLight;
    }

    public boolean needsLight() {
        return needsLight;
    }

    public Item result() {
        return result.get();
    }

    /** Game ticks the culture needs. */
    public static long durationTicks() {
        return Math.round(ServerConfig.getOr(ServerConfig.FOOD_CULTURE_DAYS, 1.0) * 24000.0);
    }

    /** Returns the ripe item stack, or null if the culture is not ready yet (stamps the start time if missing). */
    public ItemStack ripen(ItemStack stack, Level level, Entity holder) {
        Long start = stack.get(ModDataComponents.CULTURE_START.get());
        long now = level.getGameTime();
        // M33 part b: its feature off (live food): the culture rests; its time goes on from where it stood when it is on again
        if (!org.overengineer.bettacraft.config.FeatureGate.item(this)) {
            if (start != null) stack.set(ModDataComponents.CULTURE_START.get(), start + 20);
            return null;
        }
        if (start == null) {
            stack.set(ModDataComponents.CULTURE_START.get(), now);
            return null;
        }
        if (now - start < durationTicks()) return null;
        if (needsLight && level.getMaxLocalRawBrightness(holder.blockPosition()) < MIN_LIGHT) return null;
        return new ItemStack(result.get(), stack.getCount());
    }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level, Entity entity, @javax.annotation.Nullable net.minecraft.world.entity.EquipmentSlot slot) {
        // M52b: server only, no slot number any more (1.21.5): spread over the ticks by the stack instead
        if ((level.getGameTime() + System.identityHashCode(stack)) % 20 != 0) return;
        ItemStack ripe = ripen(stack, level, entity);
        if (ripe == null || !(entity instanceof Player p)) return;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i) == stack) {
                inv.setItem(i, ripe);
                return;
            }
        }
    }

    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide() && entity.tickCount % 20 == 0) {
            ItemStack ripe = ripen(stack, entity.level(), entity);
            if (ripe != null) entity.setItem(ripe);
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        List<Component> tooltip = new SinkList<>(tooltipAdder);
        double days = ServerConfig.getOr(ServerConfig.FOOD_CULTURE_DAYS, 1.0);
        tooltip.add(Component.translatable(needsLight ? "tooltip.bettacraft.culture.light" : "tooltip.bettacraft.culture",
                String.format(java.util.Locale.ROOT, "%.1f", days)).withStyle(ChatFormatting.GRAY));
    }
}
