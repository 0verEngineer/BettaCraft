package org.overengineer.bettacraft.platform.event;

/** M52: allow, default, deny (NeoForge's TriState of the click events). */
public enum TriState {
    TRUE, DEFAULT, FALSE;

    public boolean isTrue() {
        return this == TRUE;
    }

    public boolean isFalse() {
        return this == FALSE;
    }

    public boolean isDefault() {
        return this == DEFAULT;
    }
}
