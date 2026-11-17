package org.overengineer.bettacraft.foreign;

import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.critters.TankTerrain;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.overengineer.bettacraft.terrarium.PaludariumWater;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.overengineer.bettacraft.terrarium.TerrariumAnimals;
import static org.overengineer.bettacraft.foreign.ForeignWaterPart.*;

/** M52: the NeoForge event handlers of {@link ForeignWaterPart} (moved here from the common class). */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
final class ForeignWaterPartNeoForgeEvents {
    private ForeignWaterPartNeoForgeEvents() {}

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        Entity e = event.getEntity();
        if (!(e instanceof Mob m) || !(e.level() instanceof ServerLevel level) || !TankInhabitants.hasTankRef(e)) return;
        if (!applies(m)) return;
        State s = STATES.computeIfAbsent(m, k -> new State());
        if (m.tickCount >= s.nextResolve || m.tickCount < s.nextResolve - 40 || s.controller == null) {
            s.nextResolve = m.tickCount + 20;
            s.controller = TankInhabitants.resolveController(level, m);
            if (s.controller == null) return;
        }
        if (s.controller.isRemoved()) {
            s.controller = null;
            return;
        }
        TankData t = s.controller.getTank();
        if (!t.terrariumMode().hasLand() || t.waterMb() <= 0 || !TankClimate.enabled()) return;
        TankTerrain ter = terrain(level, s.controller);
        Vec3 pos = m.position();
        if (inWaterPart(ter, pos)) {
            s.good = pos;
            return;
        }
        Vec3 back = s.good;
        if (back == null || !inWaterPart(ter, back)) back = PaludariumWater.waterSpot(level, t, m.getRandom(), m.getBbHeight());
        if (back == null) return; // no open water at all
        Vec3 v = m.getDeltaMovement();
        m.setPos(back.x, back.y, back.z);
        m.setDeltaMovement(-v.x * 0.5, v.y * 0.5, -v.z * 0.5);
        m.getNavigation().stop();
        s.putBack++;
    }
}
