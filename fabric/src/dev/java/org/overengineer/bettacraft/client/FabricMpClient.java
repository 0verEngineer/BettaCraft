package org.overengineer.bettacraft.client;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.platform.client.ClientEvents;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M52: a client of the Fabric multiplayer test: joins localhost from the title screen, waits for "mp-check x y z" of the
 * server and checks what reached it (the tank block entity with its synced data and water, the two bettas, the tank menu
 * for Dev2, the interior dimension for Dev1), reports in the chat and quits when the server ends the test.
 */
public final class FabricMpClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static boolean joining;
    private static BlockPos tank;
    private static int delay = -1;

    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("bettacraft.smoketest.mpclient")) return;
        ClientEvents.SCREEN_OPENING.add(screen -> {
            if (screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> {
                    mc.options.onboardAccessibility = false;
                    mc.setScreen(new TitleScreen());
                });
            } else if (!joining && screen instanceof TitleScreen) {
                joining = true;
                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> net.minecraft.client.gui.screens.ConnectScreen.startConnecting(screen, mc, ServerAddress.parseString("localhost:25595"),
                        new ServerData("mptest", "localhost:25595", ServerData.Type.OTHER), false, null));
            }
        });
        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> {
            String t = msg.getString();
            if (t.startsWith("mp-check ")) {
                String[] p = t.split(" ");
                tank = new BlockPos(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
                delay = 60;
            }
        });
        ClientEvents.TICK_POST.add(FabricMpClient::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((h, mc) -> {
            LOGGER.info("[smoketest] MP client {} disconnected: finished", mc.getUser().getName());
            mc.execute(mc::stop);
        });
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.options.pauseOnLostFocus = false;
        if (delay < 0 || --delay > 0) return;
        delay = -1;
        String me = mc.getUser().getName();
        List<String> bad = new ArrayList<>();
        int ok = 0;
        if (me.equals("Dev1")) {
            // in the interior: the dimension of the tanks
            if (mc.level != null && mc.level.dimension() == org.overengineer.bettacraft.registry.ModDimensions.TANK_INTERIOR) ok++;
            else bad.add("not in the interior: " + (mc.level == null ? null : mc.level.dimension().location()));
        } else {
            if (mc.level.getBlockEntity(tank) instanceof TankBlockEntity be && be.getTank() != null) {
                ok++;
                if (be.getTank().waterMb() > 0) ok++;
                else bad.add("no water in the client's tank data");
            } else bad.add("no synced tank at " + tank);
            long bettas = mc.level.getEntitiesOfClass(BettaEntity.class, new net.minecraft.world.phys.AABB(tank).inflate(6)).size();
            if (bettas == 2) ok++;
            else bad.add(bettas + " bettas seen");
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen) ok++;
            else bad.add("menu: " + (mc.screen == null ? null : mc.screen.getClass().getSimpleName()));
        }
        String report = bad.isEmpty() ? "ok " + ok + " checks" : "bad " + bad;
        LOGGER.info("[smoketest] MP client {}: {}", me, report);
        if (mc.screen != null) mc.setScreen(null);
        mc.player.connection.sendChat("mp-report " + report);
    }
}
