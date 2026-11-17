package org.overengineer.bettacraft.client;

import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTabs;
import org.overengineer.bettacraft.config.Features;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M33 part b: the client side of the feature switches. On a remote server its switches (sent at login and after a reload
 * of its common config) replace the local ones ({@link Features#setRemote}); in single player and on a LAN host both sides
 * read the same config. After a change the creative tab is built again on its next opening, JEI hides or shows the items
 * and recipe categories again, and the handbook's navigation asks {@link Features} whenever it is built.
 */
public final class ClientFeatures {
    private static final Logger LOG = LoggerFactory.getLogger("bettacraft/features");
    private static Field cachedParameters;
    private static boolean failed;

    private ClientFeatures() {}

    public static void accept(List<String> off, String preset, List<String> keep) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.hasSingleplayerServer()) {
            if (Features.setRemote(off)) LOG.info("Feature switches of the server: {} off", off.isEmpty() ? "none" : String.join(", ", off));
            // M33 part c: the server's preset counts for its server config values here too
            if (org.overengineer.bettacraft.config.Presets.setRemote(preset, keep)) LOG.info("Preset of the server: {}", preset);
        }
        refresh();
    }

    /** Back to the local config (leaving a server). */
    public static void logout() {
        org.overengineer.bettacraft.config.AnimalSizes.setClient(null); // M36: the local config again
        boolean presetChanged = org.overengineer.bettacraft.config.Presets.setRemote(null, null);
        if (Features.setRemote(null) || presetChanged) refresh();
    }

    /** M52: hides and shows our items in JEI after a switch (set by the loader's client glue where the JEI plugin is). */
    public static Runnable jeiRefresh;

    /** The creative tab and JEI follow the current switches. */
    public static void refresh() {
        invalidateCreativeTabs();
        if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded("jei") && jeiRefresh != null) jeiRefresh.run();
    }

    /** The tab contents are cached per (features, op items, registries): forget them so the next opening builds them anew. */
    public static void invalidateCreativeTabs() {
        if (failed) return;
        try {
            org.overengineer.bettacraft.mixin.client.CreativeModeTabsAccessor.bettacraft$setCachedParameters(null);
        } catch (RuntimeException e) {
            failed = true;
            LOG.warn("Cannot rebuild the creative tabs after a change of the feature switches (they follow on the next login): {}", e.toString());
        }
    }
}
