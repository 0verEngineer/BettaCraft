package org.overengineer.bettacraft.plants;

import org.overengineer.bettacraft.util.SinkList;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
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
        if (species.land() && !org.overengineer.bettacraft.tank.interior.InteriorManager.isInterior(server)) {
            // M56: a terrarium plant outside a tank: planted in the world as decoration (it grows only in a terrarium)
            if (!LandPlants.enabled()) return InteractionResult.FAIL;
            if (!org.overengineer.bettacraft.plants.wild.WildLandPlantBlock.plantInWorld(server, ctx.getClickedPos(), ctx.getClickedFace(), species, ctx.getPlayer())) {
                return InteractionResult.FAIL;
            }
            if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) ctx.getItemInHand().shrink(1);
            return InteractionResult.CONSUME;
        }
        return InteriorPlants.plant(server, ctx, species, stage(ctx.getItemInHand())) ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        List<Component> lines = new SinkList<>(tooltipAdder);
        int st = stage(stack);
        if (st > 0) {
            lines.add(Component.translatable("item.bettacraft.aquatic_plant.grown", Math.round(100.0 * st / species.stages()))
                    .withStyle(ChatFormatting.GREEN));
        } else {
            lines.add(Component.translatable("item.bettacraft.aquatic_plant.cutting").withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("item.bettacraft.aquatic_plant.form." + species.form().name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(ChatFormatting.DARK_AQUA));
        if (species.land()) {
            // M56: where it lives and what it wants
            lines.add(Component.translatable("item.bettacraft.terrarium_plant.habitat." + species.habitat().id()).withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable("item.bettacraft.terrarium_plant.needs", Math.round(species.humidityMin()), Math.round(species.humidityMax()),
                    Math.round(species.tempMin()), Math.round(species.tempMax()), Component.translatable("item.bettacraft.terrarium_plant.light."
                            + species.light().name().toLowerCase(java.util.Locale.ROOT))).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("item.bettacraft.terrarium_plant.propagation." + species.propagation().name().toLowerCase(java.util.Locale.ROOT))
                    .withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
