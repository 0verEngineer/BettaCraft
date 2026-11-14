package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M25 English texts (water chemistry: TDS, pH, hardness, measuring tools, remineralising salt, almond leaves). Generated
 * together with the German ones in de_de.json; edit both.
 */
final class LangM25 {
    private LangM25() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("item.bettacraft.tds_meter", "TDS Meter");
        a.accept("item.bettacraft.ph_test_kit", "pH Drop Test");
        a.accept("item.bettacraft.remineralising_salt", "Remineralising Salt");
        a.accept("item.bettacraft.almond_leaf", "Indian Almond Leaf");
        a.accept("tooltip.bettacraft.tds_meter", "Right-click a tank: TDS (dissolved minerals) and hardness");
        a.accept("tooltip.bettacraft.ph_test", "Right-click a tank: pH and where it is drifting");
        a.accept("tooltip.bettacraft.remineraliser", "Right-click a tank: raises the TDS (sneak: up to 150 ppm)");
        a.accept("tooltip.bettacraft.almond_leaf", "Right-click a tank: tannins lower the pH a little and fade; or lay it on the floor");
        a.accept("info.bettacraft.tds_meter", "Measures the TDS (total dissolved solids, ppm) of a tank and shows the hardness: soft, medium hard or hard.\nNormal water has about 250 ppm, reverse osmosis water 0. Food and waste raise it, water changes bring it back.\nNever wears out.");
        a.accept("info.bettacraft.ph_test_kit", "Measures the pH of a tank (5 to 9) and shows where it is drifting.\nSoil lowers the pH, limestone raises it; peat, almond leaves, driftwood and CO2 lower it.\nEnough for 50 tests.");
        a.accept("info.bettacraft.remineralising_salt", "Raises the TDS of a tank: one portion adds 400 ppm to a bucket of water (config).\nSneaking adds as much as brings the tank to 150 ppm, the middle of the range of soft-water shrimp.\nNot for salt water.");
        a.accept("info.bettacraft.almond_leaf", "Dried catappa leaf: its tannins lower the pH a little (blackwater) and fade within about a week.\nOne leaf per water block colours the water fully.\nLaid on the floor of a tank interior it keeps giving off tannins (about half as many) and the shrimp graze on it.\nDry jungle leaves in a smoker, furnace or on a campfire.");
        a.accept("message.bettacraft.chem.off", "Water chemistry is switched off (features.waterChemistry)");
        a.accept("message.bettacraft.chem.salty", "Salt water does not need remineralising salt");
        a.accept("message.bettacraft.chem.hard_enough", "Already %s ppm: hard enough");
        a.accept("message.bettacraft.chem.remineralised", "%s remineralising salt dissolved: TDS %s ppm");
        a.accept("message.bettacraft.chem.leaf", "Almond leaf added: tannins %s %%");
        a.accept("message.bettacraft.chem.tds", "TDS %s ppm · %s");
        a.accept("message.bettacraft.chem.ph", "pH %s (%s, %s)");
        a.accept("gui.bettacraft.chem.hardness.soft", "soft");
        a.accept("gui.bettacraft.chem.hardness.medium", "medium hard");
        a.accept("gui.bettacraft.chem.hardness.hard", "hard");
        a.accept("gui.bettacraft.chem.ph.acidic", "acidic");
        a.accept("gui.bettacraft.chem.ph.neutral", "neutral");
        a.accept("gui.bettacraft.chem.ph.alkaline", "alkaline");
        a.accept("gui.bettacraft.chem.trend.falling", "falling to %s");
        a.accept("gui.bettacraft.chem.trend.rising", "rising to %s");
        a.accept("gui.bettacraft.chem.trend.steady", "steady");
        a.accept("gui.bettacraft.water.chem", "TDS %s ppm (%s) · pH %s, %s");
        a.accept("gui.bettacraft.water.chem_tip.values", "TDS %s ppm, pH %s (drifting to %s), CO2 %s mg/L");
        a.accept("gui.bettacraft.water.chem_tip.drivers", "pH:");
        a.accept("gui.bettacraft.water.chem_tip.nothing", "Nothing in the tank changes the pH (sand, no peat, leaves or wood)");
        a.accept("gui.bettacraft.water.chem_tip.salt", "Salt water: the alkalinity holds the pH");
        a.accept("gui.bettacraft.water.chem_tip.soil", "soil %s %%");
        a.accept("gui.bettacraft.water.chem_tip.lime", "limestone %s %%");
        a.accept("gui.bettacraft.water.chem_tip.peat", "peat %s %%");
        a.accept("gui.bettacraft.water.chem_tip.leaves", "almond leaves %s %%");
        a.accept("gui.bettacraft.water.chem_tip.wood", "driftwood %s %%");
        a.accept("gui.bettacraft.water.chem_tip.stress", "Not quite right for: %s");
        a.accept("gui.bettacraft.water.chem_tip.bad", "Wrong water for: %s");
        a.accept("gui.bettacraft.water.chem_tip.measure", "Measure at the tank: TDS meter, pH drop test");
        a.accept("gui.bettacraft.tm.chemistry", "pH or TDS out of range");
        a.accept("cause.bettacraft.water_chemistry", "the wrong pH or hardness");
        a.accept("cause.bettacraft.critter.chemistry", "wrong pH or hardness");
        a.accept("bettacraft.configuration.waterChemistry", "Water chemistry");
        a.accept("bettacraft.configuration.waterChemistry.tooltip", "Water chemistry: TDS, pH and hardness from the substrate, peat, almond leaves, wood, food and water changes; every animal has its range (off: the values stand still and no animal minds them)");
        a.accept("bettacraft.configuration.chemistry", "Water chemistry");
        a.accept("bettacraft.configuration.tdsPerDay", "TDS per day");
        a.accept("bettacraft.configuration.tdsPerDay.tooltip", "TDS (ppm) a normally stocked tank gains per day from food and waste (water changes bring it back near the 250 of normal water)");
        a.accept("bettacraft.configuration.remineraliserPpm", "Remineralising salt");
        a.accept("bettacraft.configuration.remineraliserPpm.tooltip", "TDS (ppm) one remineralising salt adds to 1000 mB of water");
        a.accept("bettacraft.configuration.leafBlocks", "Almond leaf strength");
        a.accept("bettacraft.configuration.leafBlocks.tooltip", "Water blocks one almond leaf colours fully with its tannins (they lower the pH)");
        a.accept("bettacraft.configuration.leafHalfLifeDays", "Almond leaf half-life");
        a.accept("bettacraft.configuration.leafHalfLifeDays.tooltip", "Days until half of the tannins of the almond leaves have faded");
    }
}
