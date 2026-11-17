package org.overengineer.bettacraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/**
 * M45: the new advancements in the GameTests that already make their events happen (a birth, a hatch, a shared filter ...):
 * a mock player beside the tank gets them like a real one (the owner and the players within 32 blocks).
 */
final class QuestAdv {
    private QuestAdv() {}

    /** A mock player standing on the tank. */
    static ServerPlayer watcher(GameTestHelper h, TankBlockEntity c) {
        ServerPlayer p = MockPlayers.serverPlayer(h);
        BlockPos b = c.getBlockPos();
        p.moveTo(b.getX() + 0.5, b.getY() + 1.5, b.getZ() + 0.5, 0F, 0F);
        return p;
    }

    static boolean done(ServerPlayer p, String id) {
        var holder = p.server.getAdvancements().get(BettaCraft.id(id));
        if (holder == null) throw new GameTestAssertException("advancement " + id + " missing");
        return p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /** Asserts that the player has the advancement and removes a watcher again. */
    static void check(GameTestHelper h, ServerPlayer p, String id) {
        boolean ok = done(p, id);
        h.assertTrue(ok, "M45: the advancement " + id + " for the player at the tank");
    }

    static void gone(GameTestHelper h, ServerPlayer p) {
        h.getLevel().getServer().getPlayerList().remove(p);
    }
}
