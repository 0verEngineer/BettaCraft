package org.overengineer.bettacraft.food;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A jar of dried artemia cysts (brine shrimp eggs, PLAN 6.3): rare drop when mining sand. One jar lasts {@link #USES}
 * cultures; crafting a culture uses one (the used jar comes back from the crafting grid).
 * <p>
 * M21.8: jars stack ({@code food.artemiaCystStackSize}: 16, 32 or 64, server config, synced to the clients). The uses
 * spent on a jar are kept in the vanilla {@code minecraft:damage} component as before, but without {@code max_damage}
 * (vanilla forbids stacking damageable items), so old jars keep their uses and only jars with the same uses left stack
 * (identical components). Taking one use off a stack splits one jar off it ({@link #useOne}).
 */
public class ArtemiaCystsItem extends Item {
    public static final int USES = 16;
    public static final int DEFAULT_STACK = 16;
    /** M21.8: the stack size the server sent (live config reloads); 0 = the synced server config decides. */
    private static volatile int clientOverride;

    public ArtemiaCystsItem(Properties properties) {
        // the component default is the largest allowed size; getMaxStackSize(stack) applies the configured one
        super(properties.stacksTo(64));
    }

    /** The configured stack size: 16, 32 or 64 (16 while no world is loaded). */
    public static int stackSize() {
        int o = clientOverride;
        int v = o > 0 ? o : org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.FOOD_CYST_STACK, DEFAULT_STACK);
        return v >= 64 ? 64 : v >= 32 ? 32 : 16;
    }

    /** Client: the value from the server's settings payload (0 = forget it, e.g. on logout). */
    public static void setClientOverride(int size) {
        clientOverride = size;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        // an explicit max_stack_size on the stack (commands, other mods) wins
        var own = stack.getComponentsPatch().get(DataComponents.MAX_STACK_SIZE);
        if (own != null && own.isPresent()) return own.get();
        return stackSize();
    }

    /** Uses already spent on one jar of this stack (0..USES-1). */
    public static int used(ItemStack stack) {
        return Mth.clamp(stack.getOrDefault(DataComponents.DAMAGE, 0), 0, USES - 1);
    }

    /** Uses left in one jar of this stack. */
    public static int usesLeft(ItemStack stack) {
        return USES - used(stack);
    }

    /** All uses in the stack. */
    public static int totalUses(ItemStack stack) {
        return stack.isEmpty() ? 0 : stack.getCount() * usesLeft(stack);
    }

    /** One jar of {@code stack} with one more use spent, or EMPTY when that was its last use. */
    public static ItemStack usedJar(ItemStack stack) {
        int u = used(stack) + 1;
        if (u >= USES) return ItemStack.EMPTY;
        ItemStack copy = stack.copyWithCount(1);
        copy.set(DataComponents.DAMAGE, u);
        return copy;
    }

    /**
     * Takes one use off {@code stack} (shrinks it by one jar) and returns the opened jar with the rest of its uses
     * (EMPTY if the jar is used up); the caller keeps or returns that jar.
     */
    public static ItemStack useOne(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack jar = usedJar(stack);
        stack.shrink(1);
        return jar;
    }

    /** A jar with {@code left} uses left (1..USES). */
    public static ItemStack withUsesLeft(ItemStack template, int left) {
        ItemStack s = template.copyWithCount(1);
        int u = USES - Mth.clamp(left, 1, USES);
        if (u == 0) s.remove(DataComponents.DAMAGE);
        else s.set(DataComponents.DAMAGE, u);
        return s;
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return usedJar(stack);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return used(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * usesLeft(stack) / USES);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float f = Math.max(0.0F, (float) usesLeft(stack) / USES);
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.bettacraft.artemia_cysts", usesLeft(stack)).withStyle(ChatFormatting.GRAY));
        if (stack.getCount() > 1) {
            tooltip.add(Component.translatable("tooltip.bettacraft.artemia_cysts.total", stack.getCount(), totalUses(stack)).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
