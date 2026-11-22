package org.overengineer.bettacraft.api;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;

/**
 * M32 part d (internal, not API): posts the public events of {@code api.event} from the places where animals are born and
 * die and alarms change. Deaths by damage of any tank inhabitant (ours, vanilla, other mods) come from one
 * death listener; the deaths of the care (the animal simply vanishes), of prey and of the small
 * animals are posted where they happen. {@code features.compat.modpackEvents} off: nothing is posted.
 */
public final class ApiEvents {
    /** Events posted (GameTests). */
    public static int posted;

    private ApiEvents() {}

    static boolean on() {
        return Features.enabled(Feature.MODPACK_EVENTS);
    }

    public static Identifier id(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type);
    }

    /**
     * M52: where the events go: the loader builds and posts the public event classes of {@code api.event} (NeoForge: on
     * its game event bus, for KubeJS and other mods). Without a sink nothing is posted.
     */
    public interface Sink {
        Sink NONE = new Sink() {};

        default void born(ServerLevel level, BlockPos tankPos, java.util.UUID tankId, String tankName, Identifier species, int count,
                boolean hatched, boolean foreign, boolean critter) {}

        default void died(ServerLevel level, BlockPos tankPos, java.util.UUID tankId, String tankName, Identifier species, @Nullable Entity entity,
                int count, String cause, @Nullable Entity killer) {}

        default void alarm(ServerLevel level, BlockPos tankPos, java.util.UUID tankId, String tankName, BlockPos monitor, String value,
                float current, float limit, boolean below, boolean active) {}

        default void featuresChanged(net.minecraft.server.MinecraftServer server) {}
    }

    public static Sink sink = Sink.NONE;

    private static void post(String name, Runnable post) {
        posted++;
        try {
            post.run();
        } catch (RuntimeException ex) {
            // a listener's error (a modpack script) must never break the tank's care loop
            org.overengineer.bettacraft.debug.LogArea.COMPAT.errorThrottled("listener:" + name, "A listener of {} failed", name, ex); // M57: a broken script fails every care step
        }
    }

    /** M45: the feature switches or the server config were reloaded (the quest script shows and hides its quests again). */
    public static void featuresChanged(net.minecraft.server.MinecraftServer server) {
        if (!on() || server == null) return;
        post("FeaturesChangedEvent", () -> sink.featuresChanged(server));
    }

    /** Young of one brood ({@code count} 0: nothing). */
    public static void born(ServerLevel level, TankBlockEntity c, Identifier species, int count, boolean hatched, boolean foreign, boolean critter) {
        if (org.overengineer.bettacraft.debug.LogArea.BREEDING.debug && count > 0 && c != null) org.overengineer.bettacraft.debug.LogArea.BREEDING.debug("tank {} at {}: {} young {} born ({}{}{})", c.getTank().id(), c.getBlockPos(),
                count, species, hatched ? "hatched" : "live birth", foreign ? ", other mod" : "", critter ? ", small animal" : ""); // M57
        if (count <= 0 || !on() || c == null) return;
        post("AnimalBornEvent", () -> sink.born(level, c.getBlockPos(), c.getTank().id(), c.getTank().name(), species, count, hatched, foreign, critter));
    }

    public static void born(ServerLevel level, TankBlockEntity c, EntityType<?> type, int count, boolean hatched) {
        born(level, c, id(type), count, hatched, false, false);
    }

    /** An animal entity of the tank dies (call it before it is removed). */
    public static void died(ServerLevel level, TankBlockEntity c, Entity e, String cause, @Nullable Entity killer) {
        if (org.overengineer.bettacraft.debug.LogArea.CARE.debug && c != null) org.overengineer.bettacraft.debug.LogArea.CARE.debug("tank {} at {}: {} {} died: {}{}", c.getTank().id(), c.getBlockPos(), id(e.getType()), e.getUUID(),
                cause, killer == null ? "" : " (by " + id(killer.getType()) + " " + killer.getUUID() + ")"); // M57
        if (!on() || c == null) return;
        post("AnimalDiedEvent", () -> sink.died(level, c.getBlockPos(), c.getTank().id(), c.getTank().name(), id(e.getType()), e, 1, cause, killer));
    }

    /** Small animals kept as data died. */
    public static void diedData(ServerLevel level, TankBlockEntity c, Identifier species, int count, String cause) {
        if (org.overengineer.bettacraft.debug.LogArea.CARE.debug && count > 0 && c != null) org.overengineer.bettacraft.debug.LogArea.CARE.debug("tank {} at {}: {} {} died: {}", c.getTank().id(), c.getBlockPos(), count, species, cause); // M57
        if (count <= 0 || !on() || c == null) return;
        post("AnimalDiedEvent", () -> sink.died(level, c.getBlockPos(), c.getTank().id(), c.getTank().name(), species, null, count, cause, null));
    }

    public static void alarm(ServerLevel level, BlockPos tankPos, java.util.UUID tankId, String tankName, BlockPos monitor, String value,
                             float current, float limit, boolean below, boolean active) {
        if (!on()) return;
        post("TankAlarmEvent", () -> sink.alarm(level, tankPos, tankId, tankName, monitor, value, current, limit, below, active));
    }

    /** The cause id of a death by damage. */
    public static String cause(Entity victim, DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof BettaEntity b && victim instanceof BettaEntity f && f.isFemale() && !b.isFemale()) return "chased";
        if (attacker instanceof BettaEntity) return "fight";
        if (attacker instanceof Player) return "player";
        if (attacker != null && attacker.getType().is(ModTags.PREDATORS)) return "predator";
        if (attacker != null) return "killed";
        if (source.is(DamageTypes.DROWN)) return "drowned";
        if (source.is(DamageTypes.STARVE)) return "starved";
        return "injuries";
    }

}
