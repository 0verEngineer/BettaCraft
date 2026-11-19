package org.overengineer.bettacraft.terrarium;

import com.google.gson.JsonParser;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.foreign.ForeignProfile;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/**
 * M35: the vanilla sea turtle is a big animal ({@code features.turtles.seaTurtles}). A grown one and a hatchling alike go
 * only into a big sea water paludarium: the size rule of the big animals ({@code BigAnimals}, M30) with {@link #LENGTH_M}
 * over the water part of the tank, from {@link #MIN_SALT} ppt salt, and with a sand (or soil) beach on the land
 * ({@code #bettacraft:turtle_nest_ground}) for the eggs. A hatchling therefore never outgrows its tank (the M17 bag in front
 * of the tank is gone for it). Its own AI stays (it swims and walks by vanilla, its own breeding suppressed in the tank,
 * M32); a pair lays its eggs into the sand of the beach by our breeding of the foreign egg layers ({@link #BREEDING}, the
 * nest of {@code ForeignBreeding}, with {@code features.turtles.breeding}): vanilla has no sexes, so the nest temperature
 * only sets how fast the eggs develop (no TSD).
 */
public final class SeaTurtles {
    /** Its length for the size rule: a 12 x 6 x 3 tank and 100 water blocks each. */
    public static final double LENGTH_M = 1.0;
    /** Sea water from this salinity on (ppt). */
    public static final float MIN_SALT = 25;

    /** The breeding of the sea turtle in a tank (a profile of our foreign breeding, not loaded from data: the turtle has its own care). */
    public static final ForeignProfile BREEDING = ForeignProfile.parse(JsonParser.parseString("""
            {"entity": "minecraft:turtle", "group": "sea_turtle", "food": [],
             "breeding": {"type": "eggs", "nest": "#bettacraft:turtle_nest_ground", "nest_temperature": 29, "young": [2, 4], "days": 3,
                          "pause_days": 6, "rate": 0.4},
             "terrarium": {"habitat": "paludarium", "length": 1.0, "volume": "water", "movement": "own", "box": false},
             "suppress_own_breeding": true}""").getAsJsonObject(), w -> {});

    private SeaTurtles() {}

    public static boolean enabled() {
        return Features.enabled(Feature.SEA_TURTLES) && TankClimate.enabled();
    }

    /** Whether a pair of sea turtles lays eggs in tanks now. */
    public static boolean breedingOn() {
        return enabled() && Features.enabled(Feature.TURTLE_BREEDING);
    }

    /** The breeding profile of this type (the sea turtle), or null. */
    @Nullable
    public static ForeignProfile breeding(EntityType<?> type) {
        return type == EntityTypes.TURTLE && breedingOn() ? BREEDING : null;
    }

    /** Why a sea turtle cannot go into this tank (no sand on its land), or null. The size and the salt: {@code BigAnimals#refusal}. */
    @Nullable
    public static Component refusal(ServerLevel level, TankBlockEntity c) {
        if (!HerpBreeding.anyNest(level, c.getTank(), ModTags.TURTLE_NEST_GROUND)) {
            return Component.translatable("message.bettacraft.sea_turtle.no_beach", EntityTypes.TURTLE.getDescription());
        }
        return null;
    }
}
