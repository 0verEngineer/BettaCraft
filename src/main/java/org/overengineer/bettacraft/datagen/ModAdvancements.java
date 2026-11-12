package org.overengineer.bettacraft.datagen;

import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.FilledBucketTrigger;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.decor.DecorType;
import org.overengineer.bettacraft.progression.BettaEventTrigger;
import org.overengineer.bettacraft.progression.Progress;
import org.overengineer.bettacraft.registry.ModItems;

/**
 * Advancement tree "Bettacraft" (M6). Titles/descriptions: {@code advancements.bettacraft.<id>.title/.description}
 * (en in {@link LangEn}, de by hand). Most criteria are {@code bettacraft:event} fired by {@link Progress}.
 */
final class ModAdvancements implements AdvancementProvider.AdvancementGenerator {
    /** All ids in tree order (also used by the language files and tests). */
    static final String[] IDS = {"root", "catch_betta", "build_tank", "fill_tank", "insert_betta", "ideal_temperature", "feeder", "recovered",
            "fight", "nest_built", "spawned", "fry_free", "grown_up", "variety_bred", "dynasty", "big_tank", "enter_interior", "decor", "substrate",
            "filter", "cabinet", "swamp_water", "mosquito_larvae", "pellets", "fry_food"};

    /** Generation a betta must reach for "Dynasty". */
    static final int DYNASTY_GENERATION = 5;
    /** Tank blocks for "Public Aquarium" (a 4x4x4 cube). */
    static final int BIG_TANK_BLOCKS = 64;

    private Consumer<AdvancementHolder> out;

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper efh) {
        out = saver;
        AdvancementHolder root = Advancement.Builder.advancement()
                .display(ModItems.BETTA_BUCKET.get(), title("root"), desc("root"),
                        ResourceLocation.withDefaultNamespace("textures/block/mud.png"), AdvancementType.TASK, false, false, false)
                .addCriterion("has_item", InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(
                        ModItems.BETTA_BUCKET.get(), ModItems.FISH_BAG.get(), ModItems.TANK.get(), ModItems.SWAMP_WATER_BOTTLE.get(),
                        ModItems.BETTA_PELLETS.get(), ModItems.MOSQUITO_LARVAE.get(), ModItems.HEATER_SMALL.get(), ModItems.SALT_WATER_BOTTLE.get())))
                .addCriterion("caught", BettaEventTrigger.Instance.of(Progress.CATCH_BETTA))
                .requirements(AdvancementRequirements.Strategy.OR)
                .save(saver, id("root"));

        // catching and keeping
        simple(root, "catch_betta", ModItems.BETTA_BUCKET.get(), AdvancementType.TASK,
                "bucket", FilledBucketTrigger.TriggerInstance.filledBucket(ItemPredicate.Builder.item().of(ModItems.BETTA_BUCKET.get())),
                "event", BettaEventTrigger.Instance.of(Progress.CATCH_BETTA));
        AdvancementHolder tank = simple(root, "build_tank", ModItems.TANK.get(), AdvancementType.TASK,
                "built", BettaEventTrigger.Instance.of(Progress.TANK_BUILT));
        AdvancementHolder filled = simple(tank, "fill_tank", Items.WATER_BUCKET, AdvancementType.TASK,
                "filled", BettaEventTrigger.Instance.atLeast(Progress.TANK_FILLED, 50));
        AdvancementHolder inserted = simple(filled, "insert_betta", ModItems.FISH_BAG.get(), AdvancementType.TASK,
                "inserted", BettaEventTrigger.Instance.of(Progress.INSERTED));
        simple(inserted, "ideal_temperature", ModItems.HEATER_MEDIUM.get(), AdvancementType.TASK,
                "ideal", BettaEventTrigger.Instance.of(Progress.IDEAL_TEMPERATURE));
        simple(inserted, "feeder", ModItems.FEEDER.get(), AdvancementType.TASK,
                "fed", BettaEventTrigger.Instance.of(Progress.FEEDER));
        simple(inserted, "recovered", Items.GLISTERING_MELON_SLICE, AdvancementType.GOAL,
                "recovered", BettaEventTrigger.Instance.of(Progress.RECOVERED));
        simpleHidden(inserted, "fight", Items.IRON_SWORD, "fight", BettaEventTrigger.Instance.of(Progress.FIGHT));

        // breeding
        AdvancementHolder nest = simple(inserted, "nest_built", Items.LILY_PAD, AdvancementType.TASK,
                "nest", BettaEventTrigger.Instance.of(Progress.NEST_BUILT));
        AdvancementHolder spawned = simple(nest, "spawned", Items.FROGSPAWN, AdvancementType.TASK,
                "spawned", BettaEventTrigger.Instance.of(Progress.SPAWNED));
        AdvancementHolder fry = simple(spawned, "fry_free", ModItems.INFUSORIA.get(), AdvancementType.TASK,
                "fry", BettaEventTrigger.Instance.of(Progress.FRY_FREE));
        AdvancementHolder grown = simple(fry, "grown_up", ModItems.BETTA_BUCKET.get(), AdvancementType.GOAL,
                "grown", BettaEventTrigger.Instance.atLeast(Progress.GROWN_UP, 1));
        simple(grown, "variety_bred", ModItems.BETTA_PELLETS.get(), AdvancementType.CHALLENGE,
                "variety", BettaEventTrigger.Instance.of(Progress.VARIETY));
        simple(grown, "dynasty", Items.GOLDEN_APPLE, AdvancementType.CHALLENGE,
                "dynasty", BettaEventTrigger.Instance.atLeast(Progress.GROWN_UP, DYNASTY_GENERATION));

        // tank building and the interior
        simple(tank, "big_tank", Items.GLASS, AdvancementType.GOAL,
                "big", BettaEventTrigger.Instance.atLeast(Progress.TANK_BUILT, BIG_TANK_BLOCKS));
        AdvancementHolder interior = simple(tank, "enter_interior", ModItems.DECOR.get(DecorType.CASTLE).get(), AdvancementType.TASK,
                "entered", BettaEventTrigger.Instance.of(Progress.ENTER_INTERIOR));
        simple(interior, "decor", ModItems.DECOR.get(DecorType.SHIPWRECK).get(), AdvancementType.TASK,
                "decor", BettaEventTrigger.Instance.of(Progress.DECOR));
        simple(interior, "substrate", Items.GRAVEL, AdvancementType.TASK,
                "substrate", BettaEventTrigger.Instance.of(Progress.SUBSTRATE));
        simple(tank, "filter", ModItems.EXTERNAL_FILTER.get(), AdvancementType.TASK,
                "filter", BettaEventTrigger.Instance.of(Progress.FILTER));
        simple(tank, "cabinet", ModItems.CABINET.get(), AdvancementType.TASK,
                "cabinet", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.CABINET.get()));

        // food chain
        AdvancementHolder swamp = simple(root, "swamp_water", ModItems.SWAMP_WATER_BOTTLE.get(), AdvancementType.TASK,
                "swamp_water", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.SWAMP_WATER_BOTTLE.get()));
        AdvancementHolder larvae = simple(swamp, "mosquito_larvae", ModItems.MOSQUITO_LARVAE.get(), AdvancementType.TASK,
                "larvae", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.MOSQUITO_LARVAE.get()));
        simple(larvae, "pellets", ModItems.BETTA_PELLETS.get(), AdvancementType.TASK,
                "pellets", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.BETTA_PELLETS.get()));
        simple(swamp, "fry_food", ModItems.ARTEMIA_NAUPLII.get(), AdvancementType.TASK,
                "fry_food", InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(
                        ModItems.INFUSORIA.get(), ModItems.ARTEMIA_NAUPLII.get())));
    }

    /** An advancement with one or more criteria (pairs of name, criterion), any of which completes it. */
    private AdvancementHolder simple(AdvancementHolder parent, String name, ItemLike icon, AdvancementType type, Object... criteria) {
        return build(parent, name, icon, type, false, criteria);
    }

    private AdvancementHolder simpleHidden(AdvancementHolder parent, String name, ItemLike icon, Object... criteria) {
        return build(parent, name, icon, AdvancementType.TASK, true, criteria);
    }

    private AdvancementHolder build(AdvancementHolder parent, String name, ItemLike icon, AdvancementType type, boolean hidden, Object... criteria) {
        Advancement.Builder b = Advancement.Builder.advancement().parent(parent)
                .display(new ItemStack(icon), title(name), desc(name), null, type, true, type != AdvancementType.TASK, hidden);
        for (int i = 0; i < criteria.length; i += 2) b.addCriterion((String) criteria[i], (Criterion<?>) criteria[i + 1]);
        return b.requirements(AdvancementRequirements.Strategy.OR).save(out, id(name));
    }

    static String id(String name) {
        return BettaCraft.MOD_ID + ":" + name;
    }

    private static Component title(String name) {
        return Component.translatable("advancements.bettacraft." + name + ".title");
    }

    private static Component desc(String name) {
        return Component.translatable("advancements.bettacraft." + name + ".description");
    }
}
