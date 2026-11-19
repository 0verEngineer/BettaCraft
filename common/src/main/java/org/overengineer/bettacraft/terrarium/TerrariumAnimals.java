package org.overengineer.bettacraft.terrarium;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.critters.TankTerrain;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;

/**
 * M31: the registry of the terrarium animals (the care hook of parts b-d): every entity type that lives in terraria or
 * paludaria registers its {@link TerrariumNeeds} here at common setup ({@link #register}); an entity of ours also
 * implements {@link TerrariumAnimal} (its needs may then depend on the animal). With that the tank:
 * <ul>
 *     <li>lets it in by the mode instead of the water fill ({@link #refusal}), at a spot on the land ({@link #spawnPos}),
 *     counted against water and land ({@link #landBlocks});</li>
 *     <li>assesses its needs every care step ({@link TerrariumCare}) and hands the result to it;</li>
 *     <li>does not refuse it for not being a water animal.</li>
 * </ul>
 * Animals of other mods (or vanilla ones) can be registered the same way; they then get the generic care of
 * {@link TerrariumCare} (health loss from harmful problems).
 */
public final class TerrariumAnimals {
    private static final Map<EntityType<?>, TerrariumNeeds> TYPES = new ConcurrentHashMap<>();
    private static final Map<EntityType<?>, java.util.function.BooleanSupplier> ENABLED = new ConcurrentHashMap<>();

    private TerrariumAnimals() {}

    /**
     * The terrarium animals of the mod (common setup): our turtles, frogs, geckos and the snake by their grown needs, and the vanilla
     * sea turtle (M31 part b: the hatchlings; M35: a big animal, grown ones and hatchlings only in big sea water paludaria,
     * {@link SeaTurtles}).
     */
    public static void registerOurs() {
        for (HerpSpecies s : HerpSpecies.ALL) register(org.overengineer.bettacraft.registry.ModEntities.herp(s), s.needs(),
                () -> org.overengineer.bettacraft.config.Features.enabled(s.feature())); // part c: the amphibians and reptiles
        register(EntityTypes.TURTLE, SEA_TURTLE, SeaTurtles::enabled);
    }

    /** M31 part b: a sea turtle: a paludarium with a sandy beach and warm water (M35: sea water, a big tank, {@link SeaTurtles}). */
    public static final TerrariumNeeds SEA_TURTLE = TerrariumNeeds.builder(TerrariumNeeds.Habitat.PALUDARIUM).air(18, 32).water(20, 30).land(0.1, 0.7).build();

    /** Registers (or replaces) the needs of a type. */
    public static void register(EntityType<?> type, TerrariumNeeds needs) {
        TYPES.put(type, needs);
    }

    /** Registers the needs of a type whose feature switch is {@code enabled} (off: it does not go into tanks). */
    public static void register(EntityType<?> type, TerrariumNeeds needs, java.util.function.BooleanSupplier enabled) {
        TYPES.put(type, needs);
        ENABLED.put(type, enabled);
    }

    /** Whether the feature switch of the type is on (true without one). */
    public static boolean enabled(EntityType<?> type) {
        var e = ENABLED.get(type);
        if (e == null && !TYPES.containsKey(type) && org.overengineer.bettacraft.foreign.ForeignTerrarium.listed(type)) return org.overengineer.bettacraft.foreign.ForeignTerrarium.enabled(); // M35
        if (e == null && !TYPES.containsKey(type) && org.overengineer.bettacraft.mobs.GlassMobs.isAllowed(type)) return org.overengineer.bettacraft.mobs.GlassMobs.enabled(); // M31 part d
        return e == null || e.getAsBoolean();
    }

    /** GameTests: removes a registration again. */
    public static void unregister(EntityType<?> type) {
        TYPES.remove(type);
        ENABLED.remove(type);
    }

    public static boolean isTerrariumAnimal(EntityType<?> type) {
        return isRegistered(type) || org.overengineer.bettacraft.foreign.ForeignTerrarium.listed(type); // M35: and the animals of terrarium.boxAllow
    }

    /** Registered here (our own, the profiles) or a mob in glass; not the animals of {@code terrarium.boxAllow}. */
    public static boolean isRegistered(EntityType<?> type) {
        return TYPES.containsKey(type) || org.overengineer.bettacraft.mobs.GlassMobs.isAllowed(type); // M31 part d: the mobs in glass too
    }

    @Nullable
    public static TerrariumNeeds needs(EntityType<?> type) {
        TerrariumNeeds n = TYPES.get(type);
        if (n != null) return n;
        n = org.overengineer.bettacraft.mobs.GlassMobs.needs(type);
        if (n != null) return n;
        return org.overengineer.bettacraft.foreign.ForeignTerrarium.listed(type) ? org.overengineer.bettacraft.foreign.ForeignTerrarium.LISTED_NEEDS : null; // M35
    }

    /** The needs of this animal (its own when it is a {@link TerrariumAnimal}), or null for every other animal. */
    @Nullable
    public static TerrariumNeeds needs(Entity e) {
        if (e instanceof TerrariumAnimal a) return a.terrariumNeeds();
        return needs(e.getType());
    }

    /** Why the animal cannot go into this tank now (feature off, wrong mode), or null. */
    @Nullable
    public static Component refusal(ServerLevel level, TankBlockEntity c, EntityType<?> type) {
        TerrariumNeeds n = needs(type);
        if (n == null) return null;
        if (!TankClimate.enabled() || !enabled(type)) return Component.translatable("message.bettacraft.feature_off");
        TerrariumMode mode = TankClimate.refreshMode(level, c);
        if (!n.habitat().allows(mode)) {
            return Component.translatable("message.bettacraft.terrarium.wrong_mode", type.getDescription(),
                    Component.translatable("gui.bettacraft.terrarium.habitat." + n.habitat().name().toLowerCase(java.util.Locale.ROOT)),
                    Component.translatable(mode.translationKey()));
        }
        return null;
    }

    /** Blocks of the tank above its land (they count for the stocking of the terrarium animals besides the water). */
    public static double landBlocks(TankData t) {
        if (!t.terrariumMode().hasLand() || t.landShare() <= 0) return 0;
        return t.landShare() * t.capacityMb() / (double) TankData.MB_PER_BLOCK;
    }

    /** A spot on the land of the tank (the ground of a random land column; the middle of the floor without land). */
    public static Vec3 spawnPos(ServerLevel level, TankData t, EntityType<?> type, RandomSource r) {
        TankTerrain terrain = new TankTerrain(t, level.getServer(), InteriorRegions.get(level.getServer()).get(t.regionId()), level.isBrightOutside());
        double surface = t.waterMb() <= 0 ? Double.NEGATIVE_INFINITY : t.waterSurfaceY();
        double half = Math.max(0.1, type.getDimensions().width() / 2 + 1.0 / 16);
        Vec3 water = null;
        for (int i = 0; i < 40; i++) {
            double x = t.min().getX() + half + r.nextDouble() * Math.max(0.01, t.sizeX() - 2 * half);
            double z = t.min().getZ() + half + r.nextDouble() * Math.max(0.01, t.sizeZ() - 2 * half);
            double g = terrain.ground(x, z);
            if (!Double.isFinite(g)) continue;
            if (g > surface + 0.01) return new Vec3(x, g + 0.01, z);
            if (water == null) water = new Vec3(x, g + 0.01, z);
        }
        if (water != null) return water;
        return new Vec3((t.min().getX() + t.max().getX() + 1) / 2.0, t.min().getY() + 0.1, (t.min().getZ() + t.max().getZ() + 1) / 2.0);
    }
}
