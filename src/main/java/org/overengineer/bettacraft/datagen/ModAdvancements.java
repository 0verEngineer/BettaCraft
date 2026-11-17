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
            "filter", "cabinet", "swamp_water", "mosquito_larvae", "pellets", "fry_food", "water_change", "cooler", "food_bred", "mirror",
            "guppy_strain", "guppy_collection", "guppy_hybrid",
            // M45
            "tankmate_inserted", "livebearer_litter", "seahorse_fry", "critters_bred", "algae_scraped", "herp_clutch", "turtle_clutch",
            "paludarium", "sea_turtle", "pet_terrarium", "foreign_animal", "mob_in_glass", "big_animal", "shared_filter",
            "auto_water_change", "varied_diet", "regulator", "light_timer", "powered", "wound_up", "create_drive", "travelling_tank",
            // M47
            "frog_tadpole", "aquarist_trade",
            // M48
            "tetra_cory_form", "mandarin_copepods", "clown_larvae", "banggai_young",
            // M49
            "sump_installed", "calcium_reactor", "archerfish_shot", "archerfish_splash", "mudskipper_paludarium", "mangrove_grown", "amano_young",
            // M50
            "powered_trip"};

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
        AdvancementHolder ideal = simple(inserted, "ideal_temperature", ModItems.HEATER_MEDIUM.get(), AdvancementType.TASK,
                "ideal", BettaEventTrigger.Instance.of(Progress.IDEAL_TEMPERATURE));
        // M9: water change and coolers
        simple(inserted, "water_change", Items.WATER_BUCKET, AdvancementType.TASK,
                "changed", BettaEventTrigger.Instance.of(Progress.WATER_CHANGE));
        simple(ideal, "cooler", ModItems.COOLER_MEDIUM.get(), AdvancementType.TASK,
                "cooler", InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(
                        ModItems.COOLER_SMALL.get(), ModItems.COOLER_MEDIUM.get(), ModItems.COOLER_LARGE.get())));
        simple(inserted, "feeder", ModItems.FEEDER.get(), AdvancementType.TASK,
                "fed", BettaEventTrigger.Instance.of(Progress.FEEDER));
        simple(inserted, "recovered", Items.GLISTERING_MELON_SLICE, AdvancementType.GOAL,
                "recovered", BettaEventTrigger.Instance.of(Progress.RECOVERED));
        // M11: the mirror stick
        simple(inserted, "mirror", ModItems.MIRROR_STICK.get(), AdvancementType.TASK,
                "mirror", BettaEventTrigger.Instance.of(Progress.MIRROR));
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

        // M21.6: guppy and endler strains: any one, all of them (the advancement shows how many), a hybrid
        var guppy = ModItems.TANKMATE_BUCKETS.get(org.overengineer.bettacraft.tankmates.TankmateSpecies.GUPPY).get();
        AdvancementHolder strain = simple(inserted, "guppy_strain", guppy, AdvancementType.GOAL,
                "strain", BettaEventTrigger.Instance.of(Progress.LB_STRAIN));
        Advancement.Builder all = Advancement.Builder.advancement().parent(strain)
                .display(new ItemStack(ModItems.TANKMATE_BUCKETS.get(org.overengineer.bettacraft.tankmates.TankmateSpecies.ENDLER_GUPPY).get()),
                        title("guppy_collection"), desc("guppy_collection"), null, AdvancementType.CHALLENGE, true, true, false);
        for (var st : org.overengineer.bettacraft.genetics.livebearer.LivebearerStrain.values()) {
            all.addCriterion(st.id(), BettaEventTrigger.Instance.of(Progress.LB_STRAIN + "_" + st.id()));
        }
        all.requirements(AdvancementRequirements.Strategy.AND).save(out, id("guppy_collection"));
        simple(strain, "guppy_hybrid", Items.SEA_PICKLE, AdvancementType.GOAL,
                "hybrid", BettaEventTrigger.Instance.of(Progress.LB_HYBRID));

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
        // M9: live food from the larva tub or the artemia hatchery
        simple(larvae, "food_bred", ModItems.LARVA_TUB.get(), AdvancementType.GOAL,
                "bred", BettaEventTrigger.Instance.of(Progress.FOOD_BRED));
        simple(swamp, "fry_food", ModItems.ARTEMIA_NAUPLII.get(), AdvancementType.TASK,
                "fry_food", InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(
                        ModItems.INFUSORIA.get(), ModItems.ARTEMIA_NAUPLII.get())));
        m45(filled, inserted, tank, interior, larvae);
    }

    /**
     * M45: the advancements of the systems since M21 (they are also the tasks of the FTB Quests chapter): each by a
     * {@code bettacraft:event} that {@link Progress} fires where it happens.
     */
    private void m45(AdvancementHolder filled, AdvancementHolder inserted, AdvancementHolder tank, AdvancementHolder interior, AdvancementHolder larvae) {
        var tm = ModItems.TANKMATE_BUCKETS;
        AdvancementHolder mates = event(filled, "tankmate_inserted", tm.get(org.overengineer.bettacraft.tankmates.TankmateSpecies.NEON_TETRA).get(),
                AdvancementType.TASK, Progress.TANKMATE);
        event(mates, "livebearer_litter", ModItems.BREEDING_BOX.get(), AdvancementType.TASK, Progress.LITTER);
        event(mates, "seahorse_fry", tm.get(org.overengineer.bettacraft.tankmates.TankmateSpecies.LINED_SEAHORSE).get(), AdvancementType.GOAL,
                Progress.SEAHORSE_FRY);
        // M48 part b: the mandarin on copepods, clownfish larvae raised on rotifers, the Banggai young
        event(mates, "mandarin_copepods", tm.get(org.overengineer.bettacraft.tankmates.TankmateSpecies.MANDARINFISH).get(), AdvancementType.GOAL,
                Progress.MANDARIN_COPEPODS);
        event(mates, "clown_larvae", ModItems.ROTIFERS.get(), AdvancementType.CHALLENGE, Progress.CLOWN_LARVAE);
        event(mates, "banggai_young", tm.get(org.overengineer.bettacraft.tankmates.TankmateSpecies.BANGGAI_CARDINAL).get(), AdvancementType.GOAL,
                Progress.BANGGAI_YOUNG);
        event(filled, "critters_bred", ModItems.CRITTER_BUCKET.get(), AdvancementType.TASK, Progress.CRITTERS_BRED);
        event(filled, "algae_scraped", ModItems.ALGAE_SCRAPER.get(), AdvancementType.TASK, Progress.ALGAE_SCRAPED);
        AdvancementHolder palu = event(tank, "paludarium", Items.MOSS_BLOCK, AdvancementType.TASK, Progress.PALUDARIUM);
        event(palu, "herp_clutch", ModItems.TERRARIUM_BOX.get(), AdvancementType.GOAL, Progress.HERP_CLUTCH);
        AdvancementHolder turtle = event(palu, "turtle_clutch", Items.TURTLE_EGG, AdvancementType.GOAL, Progress.TURTLE_CLUTCH);
        event(turtle, "sea_turtle", Items.TURTLE_SCUTE, AdvancementType.CHALLENGE, Progress.SEA_TURTLE);
        event(palu, "pet_terrarium", Items.RABBIT_FOOT, AdvancementType.TASK, Progress.PET);
        event(filled, "foreign_animal", Items.AXOLOTL_BUCKET, AdvancementType.TASK, Progress.FOREIGN);
        event(filled, "mob_in_glass", ModItems.MOB_JAR.get(), AdvancementType.TASK, Progress.MOB_IN_GLASS);
        event(tank, "big_animal", ModItems.LARGE_NET.get(), AdvancementType.CHALLENGE, Progress.BIG_ANIMAL);
        event(tank, "shared_filter", ModItems.FILTER_PUMP_HEAD.get(), AdvancementType.GOAL, Progress.SHARED_FILTER);
        event(tank, "auto_water_change", ModItems.WATER_CHANGE_CONTROLLER.get(), AdvancementType.TASK, Progress.AUTO_WATER_CHANGE);
        event(inserted, "varied_diet", ModItems.FISH_FLAKES.get(), AdvancementType.TASK, Progress.VARIED_DIET);
        AdvancementHolder reg = event(tank, "regulator", ModItems.SPONGE_FILTER.get(), AdvancementType.TASK, Progress.REGULATOR);
        event(tank, "light_timer", ModItems.LED_PANEL.get(), AdvancementType.TASK, Progress.LIGHT_TIMER);
        event(reg, "powered", Items.REDSTONE, AdvancementType.TASK, Progress.POWERED);
        event(reg, "wound_up", ModItems.CRANK.get(), AdvancementType.TASK, Progress.WOUND);
        event(tank, "create_drive", Items.PISTON, AdvancementType.TASK, Progress.CREATE_DRIVE);
        AdvancementHolder travelled = event(tank, "travelling_tank", Items.MINECART, AdvancementType.GOAL, Progress.TRAVELLED);
        event(travelled, "powered_trip", Items.REDSTONE_TORCH, AdvancementType.GOAL, Progress.POWERED_TRIP); // M50
        // M47: the frogs' first tadpoles, a trade with the aquarist villager
        event(filled, "frog_tadpole", ModItems.FROG_BUCKET.get(), AdvancementType.GOAL, Progress.FROG_TADPOLE);
        event(inserted, "aquarist_trade", ModItems.BREEDING_BASIN.get(), AdvancementType.TASK, Progress.AQUARIST_TRADE);
        // M48: a bred form of a tetra or a cory
        event(inserted, "tetra_cory_form", ModItems.FISH_BAG.get(), AdvancementType.GOAL, Progress.TETRA_CORY_FORM);
        // M49 part a: the sump under the tank, the calcium reactor; part b: the brackish water theme
        AdvancementHolder sump = event(tank, "sump_installed", ModItems.SUMP.get(), AdvancementType.TASK, Progress.SUMP);
        event(sump, "calcium_reactor", ModItems.CALCIUM_REACTOR.get(), AdvancementType.GOAL, Progress.CALCIUM_REACTOR);
        AdvancementHolder archer = event(mates, "archerfish_shot", tm.get(org.overengineer.bettacraft.tankmates.TankmateSpecies.ARCHERFISH).get(),
                AdvancementType.TASK, Progress.ARCHER_SHOT);
        event(archer, "archerfish_splash", Items.WATER_BUCKET, AdvancementType.TASK, Progress.ARCHER_SPLASH);
        event(palu, "mudskipper_paludarium", ModItems.HERP_BOXES.get(org.overengineer.bettacraft.terrarium.HerpSpecies.MUDSKIPPER).get(),
                AdvancementType.GOAL, Progress.MUDSKIPPER);
        event(interior, "mangrove_grown", Items.MANGROVE_PROPAGULE, AdvancementType.GOAL, Progress.MANGROVE);
        event(filled, "amano_young", ModItems.CRITTER_BUCKET.get(), AdvancementType.CHALLENGE, Progress.AMANO_YOUNG);
    }

    private AdvancementHolder event(AdvancementHolder parent, String name, ItemLike icon, AdvancementType type, String event) {
        return simple(parent, name, icon, type, "event", BettaEventTrigger.Instance.of(event));
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
