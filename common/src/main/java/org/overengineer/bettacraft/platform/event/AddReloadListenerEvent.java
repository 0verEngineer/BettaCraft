package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public class AddReloadListenerEvent extends GameEvent {
    private final java.util.List<net.minecraft.server.packs.resources.PreparableReloadListener> listeners = new java.util.ArrayList<>();

    public AddReloadListenerEvent() {
    }

    public void addListener(net.minecraft.server.packs.resources.PreparableReloadListener listener) {
        listeners.add(listener);
    }

    public java.util.List<net.minecraft.server.packs.resources.PreparableReloadListener> getListeners() {
        return listeners;
    }
}
