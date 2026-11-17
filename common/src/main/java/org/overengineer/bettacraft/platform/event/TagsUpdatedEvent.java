package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public class TagsUpdatedEvent extends GameEvent {
    private final TagsUpdatedEvent.UpdateCause updateCause;

    public TagsUpdatedEvent(TagsUpdatedEvent.UpdateCause updateCause) {
        this.updateCause = updateCause;
    }

    public TagsUpdatedEvent.UpdateCause getUpdateCause() {
        return updateCause;
    }

    public enum UpdateCause { SERVER_DATA_LOAD, CLIENT_PACKET_RECEIVED }
}
