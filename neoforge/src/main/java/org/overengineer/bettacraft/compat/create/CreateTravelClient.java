package org.overengineer.bettacraft.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.client.Minecraft;
import org.overengineer.bettacraft.network.TravellingTankPayload;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M50 (client, only with Create): the state of a tank travelling on a contraption arrives; Create's copy of the controller on
 * that contraption loads it, so the sign, the water and the animals show the tank as it is now.
 */
public final class CreateTravelClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/create-travel");
    private static boolean loggedOnce;
    /** How many states arrived (dev smoke and multiplayer test). */
    public static int received;

    private CreateTravelClient() {}

    public static void accept(TravellingTankPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(p.entity()) instanceof AbstractContraptionEntity e)) return;
        try {
            if (!(e.getContraption().getBlockEntityClientSide(p.local()) instanceof TankBlockEntity be)) return;
            be.loadCustomOnly(p.view(), mc.level.registryAccess());
            received++;
            if (!loggedOnce) {
                loggedOnce = true;
                LOGGER.info("A travelling tank on contraption {} shows its state ({} packed animals)", p.entity(), be.stowed().size());
            }
        } catch (RuntimeException | LinkageError ex) {
            LOGGER.debug("Could not show the state of a travelling tank", ex);
        }
    }
}
