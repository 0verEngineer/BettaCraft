package org.overengineer.bettacraft.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.overengineer.bettacraft.platform.event.SubscribeEvent;
import org.overengineer.bettacraft.platform.event.RegisterCommandsEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.tank.interior.InteriorBench;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.overengineer.bettacraft.tank.interior.InteriorSync;
import static org.overengineer.bettacraft.command.BettaCraftCommands.*;

/** M52: the game event handlers of {@link BettaCraftCommands} (M52: on the common game events, fired by each loader). */
public final class BettaCraftCommandsHandlers {
    private BettaCraftCommandsHandlers() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }
}
