package org.overengineer.bettacraft.client.screen;

import net.minecraft.client.Minecraft;
import org.overengineer.bettacraft.network.TankViewPayload;
import org.overengineer.bettacraft.tank.menu.TankMenu;
import org.overengineer.bettacraft.tank.menu.TankView;

/** Client handler of {@link TankViewPayload}. */
public final class ClientTankView {
    private ClientTankView() {}

    public static void accept(TankViewPayload payload) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof TankMenu menu && menu.containerId == payload.containerId()) {
            menu.acceptView(TankView.read(payload.view()));
        }
    }
}
