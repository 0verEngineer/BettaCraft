package org.overengineer.bettacraft.platform.network;

import net.minecraft.world.entity.player.Player;

/** M52: what a payload handler gets: the player (server: the sender; client: the local player) and the main thread. */
public interface IPayloadContext {
    Player player();

    /** Runs the work on the game thread of this side. */
    void enqueueWork(Runnable work);
}
