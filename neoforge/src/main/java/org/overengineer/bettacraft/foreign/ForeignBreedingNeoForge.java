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
import org.overengineer.bettacraft.platform.event.EventPriority;
import org.overengineer.bettacraft.platform.event.SubscribeEvent;
import org.overengineer.bettacraft.platform.event.EntityJoinLevelEvent;
import org.overengineer.bettacraft.platform.event.BabyEntitySpawnEvent;
import org.overengineer.bettacraft.platform.event.PlayerInteractEvent;
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
import static org.overengineer.bettacraft.foreign.ForeignBreeding.*;

/** M52: other mods' own breeding events on NeoForge's bus (Fintastic), cancelled in our tanks (see ForeignBreeding). */
public final class ForeignBreedingNeoForge {
    private ForeignBreedingNeoForge() {}


    /** Breeding events of other mods with {@code getParentA()} / {@code getParentB()}, cancellable (by class name, no compile dependency). */
    static final List<String> MOD_BABY_EVENTS = List.of("net.voidarkana.fintastic.common.entity.custom.base.BreedableWaterAnimal$BabyFishSpawnEvent");

    static boolean hooked;

    /** Listens to the breeding events of the mods in {@link #MOD_BABY_EVENTS} that are loaded (once). */
    public static synchronized void hookModEvents() {
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
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.NORMAL, false, (Class<net.neoforged.bus.api.Event>) c, ev -> {
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
