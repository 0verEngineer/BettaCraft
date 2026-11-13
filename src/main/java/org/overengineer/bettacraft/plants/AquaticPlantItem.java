package org.overengineer.bettacraft.plants;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import org.overengineer.bettacraft.registry.ModDataComponents;

/**
 * An aquarium plant (M13): a cutting, or a whole plant taken out of a tank interior (then it keeps its growth stage in
 * {@code bettacraft:plant_stage}). Used inside a tank interior it is planted like a block: epiphytes on stone and wood
 * (decoration too), Java moss on any solid block, the others on substrate.
 */
public class AquaticPlantItem extends Item {
    private final PlantSpecies species;

    public AquaticPlantItem(PlantSpecies species, Properties props) {
        super(props);
        this.species = species;
    }

    public PlantSpecies species() {
        return species;
    }

    /** Growth stage of a plant taken out (0 = cutting). */
    public static int stage(ItemStack stack) {
        Integer s = stack.get(ModDataComponents.PLANT_STAGE.get());
        return s == null ? 0 : Math.max(0, s);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        return InteriorPlants.plant(server, ctx, species, stage(ctx.getItemInHand())) ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        int st = stage(stack);
        if (st > 0) {
            lines.add(Component.translatable("item.bettacraft.aquatic_plant.grown", Math.round(100.0 * st / species.stages()))
                    .withStyle(ChatFormatting.GREEN));
        } else {
            lines.add(Component.translatable("item.bettacraft.aquatic_plant.cutting").withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("item.bettacraft.aquatic_plant.form." + species.form().name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(ChatFormatting.DARK_AQUA));
    }
}
