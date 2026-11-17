package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M25 part b English texts (CO2 dosing, reverse osmosis, the water fluids). Generated together with the German ones in
 * de_de.json; edit both.
 */
final class LangM25b {
    private LangM25b() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("item.bettacraft.yeast", "Yeast");
        a.accept("item.bettacraft.co2_solenoid", "CO2 Solenoid Valve");
        a.accept("item.bettacraft.ro_membrane", "RO Membrane");
        a.accept("item.bettacraft.ro_prefilter", "RO Prefilter Cartridge");
        a.accept("item.bettacraft.ro_water_bucket", "Bucket of RO Water");
        a.accept("block.bettacraft.bio_co2", "Bio CO2 Bottle");
        a.accept("block.bettacraft.co2_cylinder", "CO2 Cylinder");
        a.accept("block.bettacraft.co2_station", "CO2 Filling Station");
        a.accept("block.bettacraft.ro_unit", "Reverse Osmosis Unit");
        a.accept("fluid_type.bettacraft.ro_water", "Reverse Osmosis Water");
        a.accept("fluid_type.bettacraft.sea_water", "Sea Water");
        a.accept("tooltip.bettacraft.ro_water_bucket", "Pours plain water; into a tank a water change with nothing dissolved");
        a.accept("tooltip.bettacraft.co2_cylinder.gas", "CO2: %s %% (%s bubble-days)");
        a.accept("tooltip.bettacraft.co2_cylinder.solenoid", "With solenoid valve: closes while the lights are off");
        a.accept("info.bettacraft.yeast", "Baker's yeast for the bio CO2 bottle: brown mushrooms and sugar set up in a bowl.\nOne yeast starts one brew.");
        a.accept("info.bettacraft.bio_co2", "Place it next to a tank or on it. Fill it: a water bucket, up to four sugar, then yeast.\nIt ferments for about four days (less sugar: shorter) and gives about one bubble per second - weak and uneven, day and night.\nWhen it is spent, set it up again. Needs no power. Sneak + empty hand: pour it out.");
        a.accept("info.bettacraft.co2_cylinder", "CO2 gas cylinder with pressure regulator and bubble counter. Place it next to a tank or on it.\nRight-click: 0 to 10 bubbles per second (sneak + mouse wheel too). A bigger tank needs more bubbles.\nPlants grow much faster with CO2 (strong-light plants most) and the pH drops. Too much CO2 stresses and kills the animals, at night too when the plants take none up - use a solenoid valve.\nRefill empty cylinders at a CO2 filling station. Broken, it keeps its gas.");
        a.accept("info.bettacraft.co2_solenoid", "Right-click a CO2 cylinder to put it on: it closes the cylinder while the lights of the tank are off (its daylight timer, or the night).\nWithout it the CO2 keeps flowing at night, when the plants take none up.");
        a.accept("info.bettacraft.co2_station", "Fills CO2 cylinders with the flue gas of a burning furnace, smoker, blast furnace or campfire next to it (on it, beside it or above it). More fires fill faster: about a day of burning for a full cylinder.\nWith a power option on its compressor also needs FE. CO2 gas of other mods (fluid tag #bettacraft:co2_gas) goes in through pipes.\nRight-click with a cylinder to put it in, empty hand to take it out, sneak to see what it does. Hoppers take out full cylinders.");
        a.accept("info.bettacraft.ro_unit", "Makes reverse osmosis water: nothing dissolved (TDS 0, pH 6.8), for soft-water fish and shrimp.\nWater comes in through pipes, buckets or a water source block next to it (sea water too). For each part of RO water three parts of waste water leave - into a pipe or tank below, or down the drain.\nIt simply runs without power, slowly; with a power option on and FE its pressure pump makes it four times faster.\nNeeds a membrane and a prefilter; both wear out. Empty bucket or pipes take the RO water out (fluid bettacraft:ro_water).");
        a.accept("info.bettacraft.ro_membrane", "The membrane of the reverse osmosis unit: about 128 buckets of RO water, then it is worn out.\nRight-click the unit with it to put it in (the old one comes out).");
        a.accept("info.bettacraft.ro_prefilter", "Sediment and carbon prefilter of the reverse osmosis unit: protects the membrane, about 32 buckets.\nRight-click the unit with it to put it in (the old one comes out).");
        a.accept("info.bettacraft.ro_water_bucket", "Reverse osmosis water: nothing dissolved (TDS 0, pH 6.8). Into a tank it is a water change that softens the water; mix it with normal water or remineralising salt for the right hardness.\nPoured into the world it is plain water. Other mods store it as the fluid bettacraft:ro_water.");
        // messages
        a.accept("message.bettacraft.bio_co2.has_water", "There is a brew in it already (sneak + empty hand: pour it out)");
        a.accept("message.bettacraft.bio_co2.water", "Water in: now up to four sugar, then yeast");
        a.accept("message.bettacraft.bio_co2.fermenting_already", "It is fermenting already");
        a.accept("message.bettacraft.bio_co2.needs_water", "Fill it with a water bucket first");
        a.accept("message.bettacraft.bio_co2.needs_sugar", "Add sugar first");
        a.accept("message.bettacraft.bio_co2.sugar_full", "Four sugar is enough: now the yeast");
        a.accept("message.bettacraft.bio_co2.sugar", "Sugar %s/%s");
        a.accept("message.bettacraft.bio_co2.started", "The yeast ferments: about %s days of CO2");
        a.accept("message.bettacraft.bio_co2.poured", "Poured out");
        a.accept("message.bettacraft.bio_co2.status", "Fermenting: %s bubbles per second, %s days left");
        a.accept("message.bettacraft.bio_co2.mixed", "Water and %s/%s sugar: add yeast");
        a.accept("message.bettacraft.bio_co2.spent", "Spent: set it up again (water bucket, sugar, yeast)");
        a.accept("message.bettacraft.bio_co2.empty", "Empty: a water bucket, up to four sugar, then yeast");
        a.accept("message.bettacraft.co2.off", "CO2 dosing is switched off (features.co2.enabled)");
        a.accept("message.bettacraft.co2.no_tank", "Not next to a tank: place it beside a tank or on it");
        a.accept("message.bettacraft.co2.has_solenoid", "It has a solenoid valve already");
        a.accept("message.bettacraft.co2.solenoid_on", "Solenoid valve on: it closes while the lights are off");
        a.accept("message.bettacraft.co2_station.occupied", "There is a cylinder in it already");
        a.accept("message.bettacraft.co2_station.empty", "No cylinder in it (%s fires burning next to it)");
        a.accept("message.bettacraft.co2_station.full", "The cylinder is full");
        a.accept("message.bettacraft.co2_station.no_fire", "Cylinder %s %%: no fire burning next to it (furnace, smoker, blast furnace, campfire)");
        a.accept("message.bettacraft.co2_station.no_power", "Cylinder %s %%: the compressor has no FE");
        a.accept("message.bettacraft.co2_station.filling", "Filling: %s %% (%s fires)");
        a.accept("message.bettacraft.ro.membrane_in", "Membrane in");
        a.accept("message.bettacraft.ro.prefilter_in", "Prefilter in");
        a.accept("message.bettacraft.ro.not_enough", "Only %s mB of RO water: a bucket needs 1000");
        a.accept("message.bettacraft.ro.feed_full", "The water inlet is full");
        a.accept("message.bettacraft.ro.stop.off", "switched off (features.osmosis.enabled)");
        a.accept("message.bettacraft.ro.stop.no_membrane", "no membrane");
        a.accept("message.bettacraft.ro.stop.no_prefilter", "no prefilter");
        a.accept("message.bettacraft.ro.stop.full", "full");
        a.accept("message.bettacraft.ro.stop.no_water", "no water");
        a.accept("message.bettacraft.ro.running", "%s mB/t");
        a.accept("message.bettacraft.ro.running_pump", "%s mB/t (pressure pump)");
        a.accept("message.bettacraft.ro.status", "RO: %s · RO water %s mB · inlet %s mB · membrane %s %% · prefilter %s %%");
        a.accept("message.bettacraft.regulator.set_value", "%s: %s");
        a.accept("gui.bettacraft.co2.bubbles", "%s bubbles per second");
        a.accept("gui.bettacraft.co2.empty", "Empty: refill it at a CO2 filling station");
        a.accept("gui.bettacraft.co2.closed", "Solenoid valve closed (lights off) · gas %s %%");
        a.accept("gui.bettacraft.co2.running", "Bubbling into the tank · gas %s %%");
        a.accept("gui.bettacraft.co2.idle", "Not bubbling (no tank or 0) · gas %s %%");
        a.accept("gui.bettacraft.co2.state.low", "low");
        a.accept("gui.bettacraft.co2.state.good", "good for the plants");
        a.accept("gui.bettacraft.co2.state.high", "too high");
        a.accept("gui.bettacraft.co2.state.deadly", "deadly");
        a.accept("gui.bettacraft.water.co2_tip", "CO2: %s sources, %s bubbles/s, heading for %s mg/L (%s)");
        a.accept("log.bettacraft.co2_high", "CO2 too high: %s mg/L, the animals are stressed");
        a.accept("log.bettacraft.co2_deadly", "CO2 deadly: %s mg/L");
        a.accept("cause.bettacraft.co2", "too much CO2");
        a.accept("cause.bettacraft.critter.co2", "too much CO2");
        // config
        a.accept("bettacraft.configuration.co2Dosing", "CO2 dosing");
        a.accept("bettacraft.configuration.osmosis", "Reverse osmosis unit");
        a.accept("bettacraft.configuration.co2PerBubble", "CO2 per bubble");
        a.accept("bettacraft.configuration.co2PerBubble.tooltip", "mg/L per day one bubble per second gives one block of water (a bigger tank needs more bubbles)");
        a.accept("bettacraft.configuration.co2StressFrom", "CO2 stress from");
        a.accept("bettacraft.configuration.co2StressFrom.tooltip", "CO2 (mg/L) from which the animals are stressed");
        a.accept("bettacraft.configuration.co2LethalFrom", "CO2 lethal from");
        a.accept("bettacraft.configuration.co2LethalFrom.tooltip", "CO2 (mg/L) from which the water is bad for the animals (sick, in the end dead; bettas and gouramis breathe air and bear a little more, shrimp less)");
        a.accept("bettacraft.configuration.co2Breath", "CO2 breath");
        a.accept("bettacraft.configuration.co2Breath.tooltip", "mg/L per day one adult animal per water block breathes out");
        a.accept("bettacraft.configuration.co2PlantUptake", "CO2 plant uptake");
        a.accept("bettacraft.configuration.co2PlantUptake.tooltip", "mg/L per day a densely planted tank in strong light takes up");
        a.accept("bettacraft.configuration.bioCo2Bubbles", "Bio CO2 bubbles");
        a.accept("bettacraft.configuration.bioCo2Bubbles.tooltip", "Bubbles per second of a bio CO2 bottle at its best (it starts slowly, varies and fades at the end)");
        a.accept("bettacraft.configuration.bioCo2Days", "Bio CO2 days");
        a.accept("bettacraft.configuration.bioCo2Days.tooltip", "Days a bio CO2 bottle with four sugar ferments (less sugar: shorter)");
        a.accept("bettacraft.configuration.cylinderBubbleDays", "Cylinder size");
        a.accept("bettacraft.configuration.cylinderBubbleDays.tooltip", "Days a full CO2 cylinder lasts at one bubble per second");
        a.accept("bettacraft.configuration.stationTicksPerCylinder", "Filling time");
        a.accept("bettacraft.configuration.stationTicksPerCylinder.tooltip", "Ticks a CO2 filling station needs to fill a cylinder from the flue gas of one burning furnace, smoker, blast furnace or campfire (more burning fires fill faster)");
        a.accept("bettacraft.configuration.stationFe", "Compressor FE");
        a.accept("bettacraft.configuration.stationFe.tooltip", "FE per tick of the compressor of the CO2 filling station while it fills (only while a power option is on)");
        a.accept("bettacraft.configuration.foreignCo2Mb", "Foreign CO2 per cylinder");
        a.accept("bettacraft.configuration.foreignCo2Mb.tooltip", "mB of another mod's CO2 gas (fluid tag #bettacraft:co2_gas) for a full cylinder");
        a.accept("bettacraft.configuration.roRate", "RO water per tick");
        a.accept("bettacraft.configuration.roRate.tooltip", "mB of reverse osmosis water per tick without a pressure pump (it simply runs on the water it gets)");
        a.accept("bettacraft.configuration.roPumpBoost", "Pressure pump boost");
        a.accept("bettacraft.configuration.roPumpBoost.tooltip", "How much faster the FE pressure pump makes it (only while a power option is on and it has FE)");
        a.accept("bettacraft.configuration.roPumpFe", "Pressure pump FE");
        a.accept("bettacraft.configuration.roPumpFe.tooltip", "FE per tick of the pressure pump");
        a.accept("bettacraft.configuration.roWastePerPart", "Waste water");
        a.accept("bettacraft.configuration.roWastePerPart.tooltip", "Parts of waste water for each part of reverse osmosis water (it leaves through a pipe or the drain)");
        a.accept("bettacraft.configuration.roMembraneBuckets", "Membrane life");
        a.accept("bettacraft.configuration.roMembraneBuckets.tooltip", "Buckets of reverse osmosis water a membrane makes before it is worn out");
        a.accept("bettacraft.configuration.roPrefilterBuckets", "Prefilter life");
        a.accept("bettacraft.configuration.roPrefilterBuckets.tooltip", "Buckets of reverse osmosis water a prefilter cartridge lasts");
    }
}
