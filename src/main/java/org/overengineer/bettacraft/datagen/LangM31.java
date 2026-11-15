package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M31 part a English texts (terrarium and paludarium: modes, climate, devices, the musk turtle). The German ones are in
 * de_de.json; edit both.
 */
final class LangM31 {
    private LangM31() {}

    static void addAll(BiConsumer<String, String> a) {
        // config
        a.accept("bettacraft.configuration.terrarium", "Terrarium and paludarium");
        a.accept("bettacraft.configuration.terrarium.tooltip", "Terrarium and paludarium: a tank without water is a terrarium, one with a land part built in the interior above the water a paludarium (terrarium.*); they get air temperature, humidity and UV, terrarium animals live there (off: every tank is an aquarium as before, no climate values)");
        a.accept("bettacraft.configuration.terrariumDevices", "Terrarium devices");
        a.accept("bettacraft.configuration.terrariumDevices.tooltip", "Terrarium devices: heat mat, basking lamp, mister and thermo-hygrometer (off: they do nothing; a basking lamp still shines like a light)");
        a.accept("bettacraft.configuration.turtles", "Turtles");
        a.accept("bettacraft.configuration.turtles.tooltip", "Turtles in paludaria and terraria: the musk turtle (off: none go into tanks, none are sold; those in tanks stay)");
        a.accept("bettacraft.configuration.paludariumLandShare", "Paludarium from land share");
        a.accept("bettacraft.configuration.paludariumLandShare.tooltip", "A tank with water is a paludarium from this share of land on (interior columns whose top is above the water)");
        a.accept("bettacraft.configuration.terrariumLandShare", "Terrarium from land share");
        a.accept("bettacraft.configuration.terrariumLandShare.tooltip", "A tank with water is a terrarium (the water is only a dish) from this share of land on; without water it is always a terrarium");
        a.accept("bettacraft.configuration.heatMatLift", "Heat mat lift");
        a.accept("bettacraft.configuration.heatMatLift.tooltip", "Kelvin the ground rises over heat mats covering half the floor or more (the air gets 40 % of it); they run while the air is below the tank's target temperature");
        a.accept("bettacraft.configuration.baskingLampAir", "Basking lamp: air warming");
        a.accept("bettacraft.configuration.baskingLampAir.tooltip", "Kelvin the air rises under basking lamps over a quarter of the top or more");
        a.accept("bettacraft.configuration.baskingSpotLift", "Basking spot warmer by");
        a.accept("bettacraft.configuration.baskingSpotLift.tooltip", "Kelvin the basking spot under a lit basking lamp is warmer than the air");
        a.accept("bettacraft.configuration.baskingLampUv", "Basking lamp UV");
        a.accept("bettacraft.configuration.baskingLampUv.tooltip", "UV index at the basking spot of a lit basking lamp");
        a.accept("bettacraft.configuration.sunUv", "Sunlight UV");
        a.accept("bettacraft.configuration.sunUv.tooltip", "UV index of sunlight through an open top (glass and lids block it)");
        a.accept("bettacraft.configuration.misterHumidity", "Mister humidity");
        a.accept("bettacraft.configuration.misterHumidity.tooltip", "Percent humidity a spraying mister adds in a closed tank (an open top lets most of it out)");
        a.accept("bettacraft.configuration.waterHumidity", "Humidity from water");
        a.accept("bettacraft.configuration.waterHumidity.tooltip", "Percent humidity a closed tank full of water surface adds (more when the water is warm)");
        a.accept("bettacraft.configuration.plantHumidity", "Humidity from plants");
        a.accept("bettacraft.configuration.plantHumidity.tooltip", "Percent humidity land fully covered with plants adds in a closed tank");
        a.accept("bettacraft.configuration.airTauDays", "Air temperature time constant");
        a.accept("bettacraft.configuration.airTauDays.tooltip", "In-game days in which the air temperature gets 63 % of the way to its equilibrium");
        a.accept("bettacraft.configuration.humidityTauDays", "Humidity time constant");
        a.accept("bettacraft.configuration.humidityTauDays.tooltip", "In-game days in which the humidity gets 63 % of the way to its equilibrium");
        a.accept("bettacraft.configuration.misterMbPerSecond", "Mister water use");
        a.accept("bettacraft.configuration.misterMbPerSecond.tooltip", "mB of water a mister uses per second while it sprays (its tank holds 4000 mB)");
        a.accept("bettacraft.configuration.harmAfterDays", "Harm after");
        a.accept("bettacraft.configuration.harmAfterDays.tooltip", "In-game days a terrarium animal bears a harmful problem (wrong tank, too cold or hot) before it loses health");
        a.accept("bettacraft.configuration.heatMatFePerTick", "Heat mat FE per tick");
        a.accept("bettacraft.configuration.heatMatFePerTick.tooltip", "FE per tick a heat mat needs while it heats (with power.heaterRequiresEnergy)");
        a.accept("bettacraft.configuration.misterRequiresEnergy", "Misters need FE");
        a.accept("bettacraft.configuration.misterRequiresEnergy.tooltip", "Misters only spray while the tank has enough FE");
        a.accept("bettacraft.configuration.misterFePerTick", "Mister FE per tick");
        a.accept("bettacraft.configuration.misterFePerTick.tooltip", "FE per tick a mister needs while it sprays (with misterRequiresEnergy)");
        // blocks, items, the turtle
        a.accept("block.bettacraft.heat_mat", "Heat Mat");
        a.accept("block.bettacraft.basking_lamp", "Basking Lamp");
        a.accept("block.bettacraft.mister", "Mister");
        a.accept("block.bettacraft.thermo_hygrometer", "Thermo-Hygrometer");
        a.accept("entity.bettacraft.musk_turtle", "Musk Turtle");
        a.accept("item.bettacraft.musk_turtle_bucket", "Bucket of Musk Turtle");
        a.accept("item.bettacraft.musk_turtle_spawn_egg", "Musk Turtle Spawn Egg");
        // modes, habitats, problems
        a.accept("gui.bettacraft.terrarium.mode.aquarium", "Aquarium");
        a.accept("gui.bettacraft.terrarium.mode.paludarium", "Paludarium");
        a.accept("gui.bettacraft.terrarium.mode.terrarium", "Terrarium");
        a.accept("gui.bettacraft.terrarium.habitat.dry", "a terrarium without water");
        a.accept("gui.bettacraft.terrarium.habitat.paludarium", "a paludarium (water and land)");
        a.accept("gui.bettacraft.terrarium.habitat.any", "a terrarium or paludarium");
        a.accept("gui.bettacraft.terrarium.problem.wrong_habitat", "wrong kind of tank");
        a.accept("gui.bettacraft.terrarium.problem.too_cold", "too cold");
        a.accept("gui.bettacraft.terrarium.problem.too_hot", "too hot");
        a.accept("gui.bettacraft.terrarium.problem.too_dry", "air too dry");
        a.accept("gui.bettacraft.terrarium.problem.too_humid", "air too humid");
        a.accept("gui.bettacraft.terrarium.problem.no_uv", "not enough UV");
        a.accept("gui.bettacraft.terrarium.problem.no_basking", "no basking spot");
        a.accept("gui.bettacraft.terrarium.problem.too_little_land", "too little land");
        a.accept("gui.bettacraft.terrarium.problem.too_little_water", "too little water");
        a.accept("gui.bettacraft.terrarium.problem.water_cold", "water too cold");
        a.accept("gui.bettacraft.terrarium.problem.water_hot", "water too warm");
        // tank menu
        a.accept("gui.bettacraft.tab.climate", "Climate");
        a.accept("gui.bettacraft.terrarium.overview_air", "air %1$s °C, %2$s %% RH, UV %3$s");
        a.accept("gui.bettacraft.terrarium.overview_both", "Water %1$s °C · %2$s (room %3$s °C)");
        a.accept("gui.bettacraft.terrarium.overview_dry", "Terrarium: %1$s (room %2$s °C)");
        a.accept("gui.bettacraft.terrarium.mode_line", "%1$s · land %2$s %% · water %3$s %%");
        a.accept("gui.bettacraft.terrarium.mode_line_dry", "%1$s · land %2$s %% · no water");
        a.accept("gui.bettacraft.terrarium.no_values", "The climate is worked out in a moment");
        a.accept("gui.bettacraft.terrarium.air", "Air %1$s °C (room %2$s, target %3$s)");
        a.accept("gui.bettacraft.terrarium.ground_bask", "Ground over the heat mats %1$s °C · basking spot %2$s °C");
        a.accept("gui.bettacraft.terrarium.bask_only", "Basking spot %s °C");
        a.accept("gui.bettacraft.terrarium.humidity", "Humidity %1$s %% (room %2$s %%)");
        a.accept("gui.bettacraft.terrarium.uv", "UV at the basking spot %1$s (%2$s)");
        a.accept("gui.bettacraft.terrarium.uv.none", "none");
        a.accept("gui.bettacraft.terrarium.uv.low", "low");
        a.accept("gui.bettacraft.terrarium.uv.medium", "medium");
        a.accept("gui.bettacraft.terrarium.uv.high", "high");
        a.accept("gui.bettacraft.terrarium.lid", "Lid: %1$s of %2$s top blocks covered");
        a.accept("gui.bettacraft.terrarium.devices_off", "Terrarium devices are switched off (features.terrariumDevices)");
        a.accept("gui.bettacraft.terrarium.mats_none", "Heat mats: none");
        a.accept("gui.bettacraft.terrarium.mats", "Heat mats: %1$s under the floor, %2$s on the walls, heating %3$s %%");
        a.accept("gui.bettacraft.terrarium.lamps", "Basking lamps: %1$s of %2$s lit");
        a.accept("gui.bettacraft.terrarium.misters_none", "Misters: none");
        a.accept("gui.bettacraft.terrarium.misters", "Misters: %1$s (%2$s mB, set to %3$s %%)");
        a.accept("gui.bettacraft.terrarium.misters_on", "Misters: %1$s spraying (%2$s mB, set to %3$s %%)");
        a.accept("gui.bettacraft.terrarium.no_animals", "No terrarium animals in this tank");
        a.accept("gui.bettacraft.terrarium.animals_fine", "All %s terrarium animals are fine");
        a.accept("gui.bettacraft.terrarium.animal_problem", "%1$s: %2$s");
        a.accept("gui.bettacraft.terrarium.no_water", "A terrarium without water: water quality, filters, oxygen and the water chemistry stand still");
        // overlays, monitor
        a.accept("jade.bettacraft.tank.climate", "%1$s: air %2$s °C, %3$s %% RH, UV %4$s");
        a.accept("jade.bettacraft.tank.climate_detail", "Basking spot %1$s °C, land %2$s %%");
        a.accept("monitor.bettacraft.value.air_temperature", "Air temperature");
        a.accept("monitor.bettacraft.value.humidity", "Humidity");
        a.accept("monitor.bettacraft.value.uv", "UV");
        // messages, log
        a.accept("message.bettacraft.mister.no_tank", "no terrarium");
        a.accept("message.bettacraft.mister.now", "now %s %%");
        a.accept("message.bettacraft.mister.state", "Mister: sprays below %1$s %% humidity · water %2$s / %3$s mB · %4$s");
        a.accept("message.bettacraft.thermometer.no_tank", "Not on a tank");
        a.accept("message.bettacraft.thermometer.water", "Water %s °C (an aquarium: no air values)");
        a.accept("message.bettacraft.thermometer.climate", "%1$s: air %2$s °C · humidity %3$s %% · basking spot %4$s °C · UV %5$s");
        a.accept("message.bettacraft.terrarium.wrong_mode", "%1$s lives in %2$s (this tank: %3$s)");
        a.accept("log.bettacraft.terrarium.mode", "The tank is now: %s");
        a.accept("log.bettacraft.terrarium.problem", "%1$s: %2$s");
        a.accept("log.bettacraft.terrarium.died", "%s died");
    }
}
