package org.overengineer.bettacraft.config;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.TranslatableEnum;

/**
 * M36: how big an animal is drawn, with its hitbox (one factor for both): {@link #NORMAL} (the default since M36) or
 * {@link #LARGE} (the size before M36). Only the look and the hitbox change, never the animal's data or the stocking.
 */
public enum AnimalSize implements TranslatableEnum {
    NORMAL,
    LARGE;

    @Override
    public Component getTranslatedName() {
        return Component.translatable("bettacraft.configuration.size." + name().toLowerCase(Locale.ROOT));
    }

    public static AnimalSize byName(String name) {
        for (AnimalSize s : values()) if (s.name().equals(name)) return s;
        return NORMAL;
    }
}
