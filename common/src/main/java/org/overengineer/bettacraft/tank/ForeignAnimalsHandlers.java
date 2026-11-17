package org.overengineer.bettacraft.tank;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.platform.event.SubscribeEvent;
import org.overengineer.bettacraft.platform.event.EntityJoinLevelEvent;
import org.overengineer.bettacraft.platform.event.LivingConversionEvent;
import org.overengineer.bettacraft.platform.event.ServerTickEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import static org.overengineer.bettacraft.tank.ForeignAnimals.*;

/** M52: the game event handlers of {@link ForeignAnimals} (M52: on the common game events, fired by each loader). */
public final class ForeignAnimalsHandlers {
    private ForeignAnimalsHandlers() {}

    @SubscribeEvent
    public static void onConvertPre(LivingConversionEvent.Pre event) {
        if (!(event.getEntity() instanceof Tadpole tadpole) || !(tadpole.level() instanceof ServerLevel)) return;
        if (!TankInhabitants.hasTankRef(tadpole)) return;
        if (ServerConfig.getOr(ServerConfig.INHABITANTS_TADPOLES, ServerConfig.Tadpoles.LEAVE_AS_FROG) != ServerConfig.Tadpoles.STAY_TADPOLE) return;
        event.setCanceled(true);
        // back to a young tadpole, so this is not asked again every tick (Tadpole#setAge is private; the bucket data sets it)
        CompoundTag age = new CompoundTag();
        age.putInt("Age", 0);
        tadpole.loadFromBucketTag(age);
    }

    @SubscribeEvent
    public static void onConvertPost(LivingConversionEvent.Post event) {
        if (!(event.getEntity() instanceof Tadpole tadpole) || !(tadpole.level() instanceof ServerLevel level)) return;
        TankBlockEntity c = TankInhabitants.resolveController(level, tadpole);
        if (c == null) return;
        Entity frog = event.getOutcome();
        TankInhabitants.release(frog);
        FROGS_OUT.put(frog.getUUID(), inFront(level, c.getTank()));
        c.getTank().inhabitants().remove(tadpole.getUUID());
        TankCare.log(level, c, "log.bettacraft.tadpole_left", TankCare.nameArg(tadpole));
        c.markTankChanged();
    }

    /** The frog is added after the conversion event and placed where the tadpole was: move it out of the tank. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (capture != null) capture.add(event.getEntity());
        if (FROGS_OUT.isEmpty()) return;
        Vec3 to = FROGS_OUT.remove(event.getEntity().getUUID());
        if (to != null) event.getEntity().snapTo(to.x, to.y, to.z, event.getEntity().getYRot(), 0);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!FROGS_OUT.isEmpty()) FROGS_OUT.clear();
    }
}
