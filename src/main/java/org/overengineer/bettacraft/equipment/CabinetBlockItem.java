package org.overengineer.bettacraft.equipment;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.overengineer.bettacraft.registry.ModDataComponents;

/** Cabinet item: remembers its planks ({@code bettacraft:wood}) and is named after them ("Oak Cabinet"). */
public class CabinetBlockItem extends BlockItem {
    public CabinetBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static ResourceLocation getWood(ItemStack stack) {
        ResourceLocation w = stack.get(ModDataComponents.WOOD.get());
        return w == null ? CabinetBlockEntity.DEFAULT_WOOD : w;
    }

    public static ItemStack setWood(ItemStack stack, ResourceLocation wood) {
        stack.set(ModDataComponents.WOOD.get(), wood);
        return stack;
    }

    public static Block woodBlock(ItemStack stack) {
        Block b = BuiltInRegistries.BLOCK.get(getWood(stack));
        return b == Blocks.AIR ? Blocks.OAK_PLANKS : b;
    }

    static Component nameFor(ResourceLocation wood) {
        return Component.translatable("block.bettacraft.cabinet.with_wood", woodName(wood));
    }

    private static final java.util.regex.Pattern PLANKS_SUFFIX = java.util.regex.Pattern.compile("(?i)\s*(holz)?(planks|bretter|planken)$");

    /**
     * Name of the wood, not of the planks: "Oak" / "Eichenholz". Vanilla woods have translations ({@code wood.minecraft.oak});
     * other mods fall back to their planks name without "Planks" / "bretter" in the current language.
     */
    public static Component woodName(ResourceLocation planks) {
        Block b = BuiltInRegistries.BLOCK.get(planks);
        if (b == Blocks.AIR) b = Blocks.OAK_PLANKS;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
        String base = id.getPath().endsWith("_planks") ? id.getPath().substring(0, id.getPath().length() - "_planks".length()) : id.getPath();
        String full = net.minecraft.locale.Language.getInstance().getOrDefault(b.getDescriptionId());
        String stripped = PLANKS_SUFFIX.matcher(full).replaceAll("").trim();
        return Component.translatableWithFallback("wood." + id.getNamespace() + "." + base, stripped.isEmpty() ? full : stripped);
    }

    @Override
    public Component getName(ItemStack stack) {
        return nameFor(getWood(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.bettacraft.cabinet").withStyle(ChatFormatting.GRAY));
    }
}
