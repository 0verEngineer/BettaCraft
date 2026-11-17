package org.overengineer.bettacraft.compat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.compat.info.OverlayProvider;
import org.overengineer.bettacraft.compat.info.OverlayProvider.Choice;
import org.overengineer.bettacraft.compat.info.OverlayProvider.Overlay;

/** M21: config compat.overlayProvider picks which overlay mod shows our lines. */
class OverlayProviderTest {
    private static boolean shows(Choice c, Overlay o, String... mods) {
        Set<String> loaded = Set.of(mods);
        return OverlayProvider.shows(c, o, loaded::contains);
    }

    @Test
    void autoPicksTheFirstInstalledOneOnly() {
        // Jade + The One Probe (the dev client before M21): only Jade
        assertTrue(shows(Choice.AUTO, Overlay.JADE, "jade", "theoneprobe"));
        assertFalse(shows(Choice.AUTO, Overlay.TOP, "jade", "theoneprobe"));
        // all three: Jade
        assertTrue(shows(Choice.AUTO, Overlay.JADE, "jade", "wthit", "theoneprobe"));
        assertFalse(shows(Choice.AUTO, Overlay.WTHIT, "jade", "wthit", "theoneprobe"));
        assertFalse(shows(Choice.AUTO, Overlay.TOP, "jade", "wthit", "theoneprobe"));
        // WTHIT before The One Probe
        assertTrue(shows(Choice.AUTO, Overlay.WTHIT, "wthit", "theoneprobe"));
        assertFalse(shows(Choice.AUTO, Overlay.TOP, "wthit", "theoneprobe"));
        // a single overlay mod always shows
        assertTrue(shows(Choice.AUTO, Overlay.TOP, "theoneprobe"));
        assertTrue(shows(Choice.AUTO, Overlay.WTHIT, "wthit"));
        assertTrue(shows(Choice.AUTO, Overlay.JADE, "jade"));
    }

    @Test
    void fixedChoicesAndAll() {
        assertTrue(shows(Choice.TOP, Overlay.TOP, "jade", "theoneprobe"));
        assertFalse(shows(Choice.TOP, Overlay.JADE, "jade", "theoneprobe"));
        assertTrue(shows(Choice.WTHIT, Overlay.WTHIT, "jade", "wthit"));
        assertFalse(shows(Choice.JADE, Overlay.WTHIT, "jade", "wthit"));
        assertTrue(shows(Choice.ALL, Overlay.JADE, "jade", "wthit", "theoneprobe"));
        assertTrue(shows(Choice.ALL, Overlay.WTHIT, "jade", "wthit", "theoneprobe"));
        assertTrue(shows(Choice.ALL, Overlay.TOP, "jade", "wthit", "theoneprobe"));
        // a missing mod never shows, whatever the choice
        assertFalse(shows(Choice.TOP, Overlay.TOP, "jade"));
        assertFalse(shows(Choice.ALL, Overlay.WTHIT, "jade"));
        // the chosen mod is missing: nobody shows (the player asked for that one)
        assertFalse(shows(Choice.TOP, Overlay.JADE, "jade"));
    }
}
