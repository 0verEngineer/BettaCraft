package org.overengineer.bettacraft.mixin.client;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** M52: instead of reflection by name (names differ in a Fabric production run): the cached tab parameters. */
@Mixin(CreativeModeTabs.class)
public interface CreativeModeTabsAccessor {
    @Accessor("CACHED_PARAMETERS")
    static void bettacraft$setCachedParameters(CreativeModeTab.ItemDisplayParameters value) {
        throw new AssertionError();
    }
}
