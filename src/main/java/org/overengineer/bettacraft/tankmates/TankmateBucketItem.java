package org.overengineer.bettacraft.tankmates;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluids;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.registry.ModEntities;

/** M19: a tankmate in a bucket of water (one item per species), with a tooltip of the fish. */
public class TankmateBucketItem extends org.overengineer.bettacraft.containers.AnimalBucketItem {
    private final TankmateSpecies species;

    public TankmateBucketItem(TankmateSpecies species, Properties properties) {
        super(ModEntities.TANKMATES.get(species).get(), Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, properties);
        this.species = species;
    }

    public TankmateSpecies species() {
        return species;
    }

    /** The fish data in a bucket or bag ({@code bucket_entity_data} key "tankmate"), or null. */
    @Nullable
    public static CompoundTag data(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.BUCKET_ENTITY_DATA);
        if (cd == null || !cd.contains(TankmateEntity.KEY)) return null;
        return cd.copyTag().getCompound(TankmateEntity.KEY);
    }

    /** A bucket (or bag) holding a fish of this species with the given genes, sex and stage (creative tab, tests, trades). */
    public static ItemStack withFish(ItemStack stack, TankmateSpecies s, TankmateGenes genes, boolean female,
                                     org.overengineer.bettacraft.entity.betta.LifeStage stage) {
        CompoundTag fish = new CompoundTag();
        fish.putString("species", s.id());
        fish.put("genes", genes.save());
        fish.putBoolean("female", female);
        fish.putString("stage", stage.id());
        org.overengineer.bettacraft.genetics.GeneVisibility.write(fish, false); // M22.3: a new animal: genes unknown
        CompoundTag t = new CompoundTag();
        t.put(TankmateEntity.KEY, fish);
        t.putFloat("Health", 4.0F);
        stack.set(DataComponents.BUCKET_ENTITY_DATA, CustomData.of(t));
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (stack.has(org.overengineer.bettacraft.registry.ModDataComponents.TANKMATE_GROUP.get())) {
            TankmateItems.appendBag(stack, tooltip); // M22.3: several fish
            return;
        }
        append(species, data(stack), tooltip);
        CompoundTag d = data(stack);
        if (d != null) genesLine(species, d, tooltip);
    }

    @Override
    public Component getName(ItemStack stack) {
        var g = stack.get(org.overengineer.bettacraft.registry.ModDataComponents.TANKMATE_GROUP.get());
        if (g != null && g.size() > 1) return Component.translatable("item.bettacraft.bucket.group", g.size(), Component.translatable(species.translationKey()));
        return super.getName(stack);
    }

    /** Lines shown per container before "... and N more". */
    public static final int LINES = 8;

    /** M22.3: one line per fish: sex, stage, colour, the genes when analysed (else a note), gravid. */
    public static void appendEach(TankmateSpecies s, List<CompoundTag> fish, List<Component> tooltip) {
        int i = 0;
        for (CompoundTag f : fish) {
            if (i++ >= LINES) {
                tooltip.add(Component.translatable("tooltip.bettacraft.critters.more", fish.size() - LINES).withStyle(ChatFormatting.DARK_GRAY));
                break;
            }
            tooltip.add(line(s, f.getCompound(TankmateEntity.KEY)).withStyle(ChatFormatting.GRAY));
        }
    }

    /** One fish of a container (its "tankmate" data). */
    public static net.minecraft.network.chat.MutableComponent line(TankmateSpecies s, CompoundTag fish) {
        var stage = org.overengineer.bettacraft.entity.betta.LifeStage.byId(fish.getString("stage"));
        boolean female = fish.getBoolean("female");
        net.minecraft.network.chat.MutableComponent c = Component.literal(female ? "♀ " : "♂ ").append(Component.translatable(stage.translationKey()));
        TankmateGenes g = TankmateGenes.load(fish.getCompound("genes"), s);
        Component lb = livebearerName(s, fish);
        if (lb != null) c.append(", ").append(lb.copy().withStyle(ChatFormatting.GRAY));
        else if (s.varieties().size() > 1) c.append(", ").append(Component.translatable(s.varietyKey(g.phenotype(s))));
        if (org.overengineer.bettacraft.genetics.GeneVisibility.visible(org.overengineer.bettacraft.genetics.GeneVisibility.analysed(fish))) {
            if (s.varieties().size() > 1 && lb == null) c.append(Component.translatable("tooltip.bettacraft.genes.short", g.text()));
        } else {
            c.append(Component.translatable("tooltip.bettacraft.genes.unknown_short"));
        }
        if (fish.getDouble("gravid") > 0) c.append(Component.translatable("tooltip.bettacraft.critters.eggs"));
        return c;
    }

    /** M22.3: the genes of one fish in a bucket (or a note that they are not analysed). */
    public static void genesLine(TankmateSpecies s, CompoundTag bucketData, List<Component> tooltip) {
        CompoundTag fish = bucketData.getCompound(TankmateEntity.KEY);
        if (!org.overengineer.bettacraft.genetics.GeneVisibility.visible(org.overengineer.bettacraft.genetics.GeneVisibility.analysed(fish))) {
            tooltip.add(Component.translatable("tooltip.bettacraft.genes.unknown").withStyle(ChatFormatting.DARK_GRAY));
        } else if (s.varieties().size() > 1 && livebearerName(s, fish) == null) {
            tooltip.add(Component.translatable("tooltip.bettacraft.genes", TankmateGenes.load(fish.getCompound("genes"), s).text()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** M21.6: the strain (or hybrid, or description) of a guppy or endler in a bucket or bag, null without the genetics. */
    @Nullable
    public static Component livebearerName(TankmateSpecies s, CompoundTag fish) {
        if (!TankmateGenes.livebearer(s) || !org.overengineer.bettacraft.config.CommonConfig.livebearerGenetics()) return null;
        TankmateGenes g = TankmateGenes.load(fish.getCompound("genes"), s);
        if (g.lb() == null) return null;
        return org.overengineer.bettacraft.genetics.livebearer.LivebearerText.name(g.lb().express(fish.getBoolean("female"))).withStyle(ChatFormatting.GOLD);
    }

    /** Tooltip lines of one tankmate: latin name, sex and stage, variety (shared with the fish bag). */
    public static void append(TankmateSpecies s, @Nullable CompoundTag fish, List<Component> tooltip) {
        tooltip.add(Component.literal(s.latin()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (fish == null) return;
        var stage = org.overengineer.bettacraft.entity.betta.LifeStage.byId(fish.getString("stage"));
        Component sex = org.overengineer.bettacraft.entity.betta.BettaTooltip.sex(fish.getBoolean("female"));
        tooltip.add(Component.translatable("tooltip.bettacraft.tankmate.sex_stage", sex, Component.translatable(stage.translationKey()))
                .withStyle(ChatFormatting.GRAY));
        Component lb = livebearerName(s, fish);
        if (lb != null) {
            tooltip.add(lb);
        } else if (s.varieties().size() > 1) {
            TankmateGenes g = TankmateGenes.load(fish.getCompound("genes"), s);
            tooltip.add(Component.translatable(s.varietyKey(g.phenotype(s))).withStyle(ChatFormatting.GOLD));
        }
        if (fish.getDouble("gravid") > 0 || fish.contains("gravid_father") && fish.getDouble("gravid") >= 0) {
            tooltip.add(Component.translatable("tooltip.bettacraft.tankmate.gravid").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
