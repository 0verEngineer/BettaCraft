package org.overengineer.bettacraft.platform.event;

/** M52: a game event of the common code (see {@link GameEvents}); some can be cancelled, like NeoForge's. */
public abstract class GameEvent {
    private boolean canceled;

    public boolean isCancelable() {
        return false;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        if (!isCancelable()) throw new UnsupportedOperationException(getClass().getSimpleName() + " cannot be cancelled");
        this.canceled = canceled;
    }
}
