package org.overengineer.bettacraft.fabric.dev;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M52: the server half of the Fabric multiplayer test ({@code gradlew fabricMultiplayerTest}): when Dev1 and Dev2 are
 * online it builds a tank with water and two bettas next to them, opens the tank menu for Dev2 and sends Dev1 into the
 * interior; the clients check what they got (FabricMpClient) and report it in the chat. Passes when both reported ok.
 */
public final class FabricMpSmoke implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    static final Set<String> NAMES = Set.of("Dev1", "Dev2");
    private static final Map<String, String> REPORTS = new ConcurrentHashMap<>();
    private static int stage, wait;
    private static BlockPos base;
    private static int checks;

    @Override
    public void onInitialize() {
        if (!Boolean.getBoolean("bettacraft.smoketest.mp")) return;
        ServerMessageEvents.CHAT_MESSAGE.register((msg, sender, params) -> {
            String t = msg.signedContent();
            if (t.startsWith("mp-report ")) REPORTS.put(sender.getGameProfile().getName(), t.substring("mp-report ".length()));
        });
        ServerTickEvents.END_SERVER_TICK.register(FabricMpSmoke::tick);
    }

    private static void tick(MinecraftServer server) {
        if (wait > 0) {
            wait--;
            return;
        }
        ServerPlayer d1 = server.getPlayerList().getPlayerByName("Dev1"), d2 = server.getPlayerList().getPlayerByName("Dev2");
        try {
            switch (stage) {
                case 0 -> {
                    if (d1 == null || d2 == null) return;
                    LOGGER.info("[smoketest] MP: both clients joined");
                    checks++;
                    wait = 60;
                    stage = 1;
                }
                case 1 -> {
                    ServerLevel level = server.overworld();
                    base = level.getSharedSpawnPos().above(2).offset(3, 0, 3);
                    for (int x = 0; x < 3; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
                        level.setBlockAndUpdate(base.offset(x, y, z), ModBlocks.TANK.get().defaultBlockState());
                    }
                    for (ServerPlayer p : new ServerPlayer[]{d1, d2}) {
                        p.teleportTo(level, base.getX() + 1.5, base.getY(), base.getZ() - 3.0, 0F, 20F);
                        p.getAbilities().flying = true;
                        p.onUpdateAbilities();
                    }
                    wait = 40;
                    stage = 2;
                }
                case 2 -> {
                    ServerLevel level = server.overworld();
                    TankBlockEntity c = TankMultiblock.getController(level, base);
                    check(c != null, "the tank formed");
                    TankInteractions.changeWater(level, c, c.getTank().capacityMb());
                    for (int i = 0; i < 2; i++) {
                        BettaData d = BettaData.create(BettaGenome.of(Map.of("body_color", "royal_blue"), BettaGenome.DEFAULT), i > 0, level.getGameTime());
                        Component err = TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d),
                                ModEntities.BETTA.get(), d1, false);
                        check(err == null, "betta " + i + " inserted" + (err == null ? "" : ": " + err.getString()));
                    }
                    c.markTankChanged();
                    LOGGER.info("[smoketest] MP: tank at {} with water and two bettas", c.getBlockPos());
                    wait = 60;
                    stage = 3;
                }
                case 3 -> {
                    TankBlockEntity c = TankMultiblock.getController(server.overworld(), base);
                    // the menu for Dev2, the interior for Dev1
                    org.overengineer.bettacraft.platform.Platform.get().openMenu(d2, new net.minecraft.world.SimpleMenuProvider(
                            (id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c), Component.translatable("gui.bettacraft.tank.title")),
                            buf -> buf.writeBlockPos(c.getBlockPos()));
                    InteriorManager.enter(d1, c.getBlockPos()); // the first entry builds the region first (some ticks)
                    stage = 30;
                }
                case 30 -> {
                    if (!InteriorManager.isInterior(d1.level())) {
                        if (++checks > 600) throw new IllegalStateException("Dev1 did not get into the interior");
                        return;
                    }
                    check(true, "Dev1 is in the interior");
                    TankBlockEntity c = TankMultiblock.getController(server.overworld(), base);
                    for (ServerPlayer p : new ServerPlayer[]{d1, d2}) {
                        p.sendSystemMessage(Component.literal("mp-check " + c.getBlockPos().getX() + " " + c.getBlockPos().getY() + " " + c.getBlockPos().getZ()));
                    }
                    checks = 0;
                    wait = 20;
                    stage = 4;
                }
                case 4 -> {
                    if (REPORTS.size() < 2) {
                        if (++checks > 2000) throw new IllegalStateException("no reports of the clients: " + REPORTS);
                        return;
                    }
                    REPORTS.forEach((who, r) -> {
                        LOGGER.info("[smoketest] MP report of {}: {}", who, r);
                        if (!r.startsWith("ok")) throw new IllegalStateException(who + ": " + r);
                    });
                    InteriorManager.leave(d1);
                    LOGGER.info("[smoketest] MP PASSED (both clients ok)");
                    stage = 5;
                    wait = 40;
                }
                case 5 -> {
                    server.getPlayerList().getPlayers().forEach(p -> p.connection.disconnect(Component.literal("MP test finished")));
                    stage = 6;
                    wait = 20;
                }
                case 6 -> {
                    stage = 7;
                    server.halt(false);
                }
                default -> {
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("[smoketest] MP FAILED", e);
            stage = 5;
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new IllegalStateException(what);
        LOGGER.info("[smoketest] MP: {}", what);
    }
}
