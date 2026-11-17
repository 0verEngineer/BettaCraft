package net.neoforged.neoforge.common;

/**
 * M52: compile-time stand-in for the common module only (never in a jar). Forge Config API Port's API jar has this interface
 * with intermediary names (on NeoForge it is NeoForge's own, on Fabric the port's, both with this signature).
 */
public interface TranslatableEnum {
    net.minecraft.network.chat.Component getTranslatedName();
}
