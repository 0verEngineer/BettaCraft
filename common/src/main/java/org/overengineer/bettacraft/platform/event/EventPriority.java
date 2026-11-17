package org.overengineer.bettacraft.platform.event;

/** M52: the order of handlers (NeoForge's priorities; the loader glue keeps it relative to other mods' listeners). */
public enum EventPriority {
    HIGHEST, HIGH, NORMAL, LOW, LOWEST
}
