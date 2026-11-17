package org.overengineer.bettacraft.foreign;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaHealth;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tankmates.TankmateSpots;
import org.overengineer.bettacraft.terrarium.TerrariumNeeds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.overengineer.bettacraft.foreign.ForeignBreeding.*;

/** M52: the NeoForge event handlers of {@link ForeignBreeding} (moved here from the common class). */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
final class ForeignBreedingNeoForgeEvents {
    private ForeignBreedingNeoForgeEvents() {}

    /** A foreign inhabitant cannot be fed its breeding food by hand (that would make it fall in love). */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide || !Features.enabled(Feature.SUPPRESS_FOREIGN_BREEDING)) return;
        Entity e = event.getTarget();
        if (!TankInhabitants.hasTankRef(e) || !foreign(e) || !isBreedingFood(e, event.getItemStack())) return;
        Player p = event.getEntity();
        p.displayClientMessage(Component.translatable("message.bettacraft.foreign.no_love", e.getDisplayName()), true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onServerAboutToStart(net.neoforged.neoforge.event.server.ServerAboutToStartEvent event) {
        hookModEvents();
    }

    /** A vanilla baby of two parents in a tank. */
    @SubscribeEvent
    public static void onBaby(BabyEntitySpawnEvent event) {
        if (!Features.enabled(Feature.SUPPRESS_FOREIGN_BREEDING) || event.getParentA().level().isClientSide) return;
        Mob a = event.getParentA(), b = event.getParentB();
        if (foreign(a) && TankInhabitants.hasTankRef(a) || b != null && foreign(b) && TankInhabitants.hasTankRef(b)) {
            event.setCanceled(true);
            if (a instanceof net.minecraft.world.entity.animal.Animal x) x.resetLove();
            if (b instanceof net.minecraft.world.entity.animal.Animal x) x.resetLove();
        }
    }

    /**
     * A baby of a mod's own breeding (no event) that appears right where an inhabitant of the same type lives in a tank:
     * not loaded from disk, no tank reference, not added by us.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (ours > 0 || event.loadedFromDisk() || event.getLevel().isClientSide || !Features.enabled(Feature.SUPPRESS_FOREIGN_BREEDING)) return;
        Entity e = event.getEntity();
        if (!(e instanceof LivingEntity le) || !le.isBaby() || TankInhabitants.hasTankRef(e) || !foreign(e) || ForeignProfiles.ownCare(e.getType())) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        List<Entity> parents = level.getEntities((Entity) null, e.getBoundingBox().inflate(1.5),
                o -> o != e && o.getType() == e.getType() && TankInhabitants.hasTankRef(o));
        for (Entity parent : parents) {
            TankBlockEntity c = TankInhabitants.resolveController(level, parent);
            if (c == null || !TankInhabitants.inside(c.getTank(), e)) continue;
            event.setCanceled(true);
            if (c.logDue("foreign_suppressed:" + e.getType().getDescriptionId(), level.getGameTime(), LOG_TICKS)) {
                LOGGER.debug("suppressed a young {} of its mod's own breeding in tank {}", BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()), c.getTank().id());
            }
            return;
        }
    }

    /** Breeding events of other mods with {@code getParentA()} / {@code getParentB()}, cancellable (by class name, no compile dependency). */
    static final List<String> MOD_BABY_EVENTS = List.of("net.voidarkana.fintastic.common.entity.custom.base.BreedableWaterAnimal$BabyFishSpawnEvent");

    static boolean hooked;

    /** Listens to the breeding events of the mods in {@link #MOD_BABY_EVENTS} that are loaded (once). */
    static synchronized void hookModEvents() {
        if (hooked) return;
        hooked = true;
        for (String name : MOD_BABY_EVENTS) hook(name);
    }

    @SuppressWarnings("unchecked")
    static void hook(String name) {
        Class<?> c;
        Method pa, pb;
        try {
            c = Class.forName(name, false, ForeignBreeding.class.getClassLoader());
            if (!net.neoforged.bus.api.Event.class.isAssignableFrom(c) || !net.neoforged.bus.api.ICancellableEvent.class.isAssignableFrom(c)) return;
            pa = c.getMethod("getParentA");
            pb = c.getMethod("getParentB");
        } catch (ClassNotFoundException | NoSuchMethodException | LinkageError ex) {
            return; // the mod is not installed (or changed)
        }
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, (Class<net.neoforged.bus.api.Event>) c, ev -> {
            if (!Features.enabled(Feature.SUPPRESS_FOREIGN_BREEDING)) return;
            try {
                Object a = pa.invoke(ev), b = pb.invoke(ev);
                if (inTank(a) || inTank(b)) ((net.neoforged.bus.api.ICancellableEvent) ev).setCanceled(true);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        });
        LOGGER.info("suppressing {} in tanks", name.substring(name.lastIndexOf('.') + 1));
    }
}
