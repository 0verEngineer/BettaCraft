package org.overengineer.bettacraft.mixin.client;

import java.util.List;
import org.overengineer.bettacraft.compat.guideme.GuidePageFeatures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * M33 part b: the handbook search leaves out the pages of features that are off ({@link GuidePageFeatures}). Skipped
 * without GuideME ({@link Pseudo}, {@code require = 0}).
 */
@Pseudo
@Mixin(targets = "guideme.internal.search.GuideSearch", remap = false)
public abstract class GuideSearchMixin {
    @Inject(method = "searchGuide(Ljava/lang/String;Lguideme/Guide;)Ljava/util/List;", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private void bettacraft$hideFeatures(String text, guideme.Guide guide, CallbackInfoReturnable<List<guideme.internal.search.GuideSearch.SearchResult>> cir) {
        cir.setReturnValue(GuidePageFeatures.filterSearch(cir.getReturnValue(), guide, guideme.internal.search.GuideSearch.SearchResult::pageId));
    }
}
