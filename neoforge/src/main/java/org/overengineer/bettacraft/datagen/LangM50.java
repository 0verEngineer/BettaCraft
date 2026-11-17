package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M50 English texts: the care of tanks on the road (Create contraptions, Create Aeronautics airships), the power on the
 * road, sloshing water. The German ones are in de_de.json; edit both.
 */
final class LangM50 {
    private LangM50() {}

    static void addAll(BiConsumer<String, String> a) {
        // config
        a.accept("bettacraft.configuration.createTravel", "Care on Create contraptions");
        a.accept("bettacraft.configuration.dynamoFullSpeed", "Drive adapter on the road: full speed (blocks/s)");
        a.accept("bettacraft.configuration.dynamoFullSpeed.tooltip", "Speed of the contraption (blocks per second) at which a drive adapter travelling at the tank gives the full power of an input (createDrive.fePerTickPerInput; slower proportionally)");
        a.accept("bettacraft.configuration.batteryFePerTick", "FE per battery on the road");
        a.accept("bettacraft.configuration.batteryFePerTick.tooltip", "FE per tick the tank takes at most from each FE battery of the contraption next to it (0: none)");
        a.accept("bettacraft.configuration.travelSloshing", "Sloshing water on the road");
        a.accept("bettacraft.configuration.travelSloshing.tooltip", "How strongly the water of a tank on a Create contraption or an airship sloshes when it speeds up, brakes or takes a curve: the surface tilts and waves run over it (0 = off: flat water as in a standing tank, 1 = natural, 2 = twice as much)");
        // log, messages
        a.accept("log.bettacraft.travel.arrived", "Arrived after %s min on the road (devices powered %s of the time, %s FE from the contraption)");
        // advancement and quest
        a.accept("advancements.bettacraft.powered_trip.title", "Fish Transport with Power");
        a.accept("advancements.bettacraft.powered_trip.description", "The filter and heater of a travelling tank run on the power of its contraption or airship");
        a.accept("quest.bettacraft.powered_trip.title", "Fish Transport with Power");
        a.accept("quest.bettacraft.powered_trip.desc", "Optional, with Create: on the road the animals get hungry and the water changes. Put a drive adapter or an FE battery next to the tank on the contraption (or a generator on the airship), so filter and heater keep running during the trip.");
    }
}
