package org.overengineer.bettacraft.client;

/** M52b: client helpers for the APIs that changed between Minecraft 1.21.1 and 1.21.11. */
public final class ClientCompat {
    private ClientCompat() {}

    /**
     * A text colour as 1.21.1 drew it: an alpha of 0 (an RGB value like {@code 0xFFFFFF}) meant opaque. Since 1.21.6 text
     * colours are ARGB and such a colour would be invisible.
     */
    public static int textColor(int color) {
        return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
    }
}
