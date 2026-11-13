package org.overengineer.bettacraft.compat.info;

import java.util.function.Predicate;

/**
 * M21: which block/entity overlay mod shows our lines (config {@code compat.overlayProvider}). With two overlay mods in
 * one pack (e.g. Jade and The One Probe) both used to show the tank, one box next to the other. {@link Choice#AUTO}
 * hands our lines only to the first installed overlay in the order Jade, WTHIT, The One Probe; {@link Choice#ALL}
 * keeps the old behaviour.
 */
public final class OverlayProvider {
    public enum Choice { AUTO, JADE, TOP, WTHIT, ALL }

    public enum Overlay {
        JADE("jade"), WTHIT("wthit"), TOP("theoneprobe");

        public final String modId;

        Overlay(String modId) {
            this.modId = modId;
        }
    }

    private OverlayProvider() {}

    /** Pure selection (unit tested): whether {@code overlay} shows our lines for the given choice and installed mods. */
    public static boolean shows(Choice choice, Overlay overlay, Predicate<String> loaded) {
        if (!loaded.test(overlay.modId)) return false;
        return switch (choice) {
            case ALL -> true;
            case JADE -> overlay == Overlay.JADE;
            case TOP -> overlay == Overlay.TOP;
            case WTHIT -> overlay == Overlay.WTHIT;
            case AUTO -> {
                for (Overlay o : Overlay.values()) {
                    if (loaded.test(o.modId)) yield o == overlay;
                }
                yield false;
            }
        };
    }

    /** Whether {@code overlay} shows our lines in this game (config + installed mods). */
    public static boolean shows(Overlay overlay) {
        Choice c = org.overengineer.bettacraft.config.CommonConfig.getOr(org.overengineer.bettacraft.config.CommonConfig.OVERLAY_PROVIDER, Choice.AUTO);
        return shows(c, overlay, id -> net.neoforged.fml.ModList.get() != null && net.neoforged.fml.ModList.get().isLoaded(id));
    }
}
