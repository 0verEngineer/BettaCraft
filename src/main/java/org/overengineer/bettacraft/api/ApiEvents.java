package org.overengineer.bettacraft.api;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.api.event.AnimalBornEvent;
import org.overengineer.bettacraft.api.event.AnimalDiedEvent;
import org.overengineer.bettacraft.api.event.TankAlarmEvent;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;

/**
 * M32 part d (internal, not API): posts the public events of {@code api.event} from the places where animals are born and
 * die and alarms change. Deaths by damage of any tank inhabitant (ours, vanilla, other mods) come from one
 * {@link LivingDeathEvent} listener; the deaths of the care (the animal simply vanishes), of prey and of the small
 * animals are posted where they happen. {@code features.modpackEvents} off: nothing is posted.
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class ApiEvents {
    /** Events posted (GameTests). */
    public static int posted;

    private ApiEvents() {}

    private static boolean on() {
        return CommonConfig.modpackEvents();
    }

    public static ResourceLocation id(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type);
    }

    private static void post(net.neoforged.bus.api.Event e) {
        posted++;
        try {
            NeoForge.EVENT_BUS.post(e);
        } catch (RuntimeException ex) {
            // a listener's error (a modpack script) must never break the tank's care loop
            com.mojang.logging.LogUtils.getLogger().error("A listener of {} failed", e.getClass().getSimpleName(), ex);
        }
    }

    /** Young of one brood ({@code count} 0: nothing). */
    public static void born(ServerLevel level, TankBlockEntity c, ResourceLocation species, int count, boolean hatched, boolean foreign, boolean critter) {
        if (count <= 0 || !on() || c == null) return;
        post(new AnimalBornEvent(level, c.getBlockPos(), c.getTank().id(), c.getTank().name(), species, count, hatched, foreign, critter));
    }

    public static void born(ServerLevel level, TankBlockEntity c, EntityType<?> type, int count, boolean hatched) {
        born(level, c, id(type), count, hatched, false, false);
    }

    /** An animal entity of the tank dies (call it before it is removed). */
    public static void died(ServerLevel level, TankBlockEntity c, Entity e, String cause, @Nullable Entity killer) {
        if (!on() || c == null) return;
        post(new AnimalDiedEvent(level, c.getBlockPos(), c.getTank().id(), c.getTank().name(), id(e.getType()), e, 1, cause, killer));
    }

    /** Small animals kept as data died. */
    public static void diedData(ServerLevel level, TankBlockEntity c, ResourceLocation species, int count, String cause) {
        if (count <= 0 || !on() || c == null) return;
        post(new AnimalDiedEvent(level, c.getBlockPos(), c.getTank().id(), c.getTank().name(), species, null, count, cause, null));
    }

    public static void alarm(ServerLevel level, BlockPos tankPos, java.util.UUID tankId, String tankName, BlockPos monitor, String value,
                             float current, float limit, boolean below, boolean active) {
        if (!on()) return;
        post(new TankAlarmEvent(level, tankPos, tankId, tankName, monitor, value, current, limit, below, active));
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

    /** Every tank inhabitant that dies by damage (fights, predators, players, drowning, ...). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !on()) return;
        Entity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel level) || !TankInhabitants.hasTankRef(e)) return;
        TankBlockEntity c = TankInhabitants.resolveController(level, e);
        if (c == null) return;
        died(level, c, e, cause(e, event.getSource()), event.getSource().getEntity());
    }
}
