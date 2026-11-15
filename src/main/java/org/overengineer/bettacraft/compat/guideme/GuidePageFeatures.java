package org.overengineer.bettacraft.compat.guideme;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import guideme.PageCollection;
import guideme.color.ConstantColor;
import guideme.compiler.ParsedGuidePage;
import guideme.document.block.LytBlock;
import guideme.document.block.LytDocument;
import guideme.document.block.LytParagraph;
import guideme.document.flow.LytFlowSpan;
import guideme.document.flow.LytFlowText;
import guideme.navigation.NavigationNode;
import guideme.style.TextStyle;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.FeatureContent;
import org.overengineer.bettacraft.config.FeatureGate;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.progression.GuideBook;

/**
 * M33 part b: the handbook hides the pages of features that are off. GuideME has no conditional pages, so the navigation
 * ({@link DeepNavigation}) leaves them out with everything below them, the search drops them, and a page reached anyway (a
 * link, the item key) starts with a notice. A page belongs to:
 * <ol>
 * <li>its frontmatter {@code feature: <path>} (pages without own items: chapters, care pages), else</li>
 * <li>its {@code item_ids}: hidden when every Bettacraft item of it is off (one core or foreign item keeps it), else</li>
 * <li>its name when that is a small animal ({@code invertebrates/neocaridina.md}).</li>
 * </ol>
 * Only pages of our guide; client only, callers have checked that GuideME is loaded.
 */
public final class GuidePageFeatures {
    private GuidePageFeatures() {}

    /** The first feature that is off and hides the page itself (not its parents), or null. */
    @Nullable
    public static Feature offFeature(@Nullable ParsedGuidePage page) {
        if (page == null || !BettaCraft.MOD_ID.equals(page.getId().getNamespace())) return null;
        Map<String, Object> fm = page.getFrontmatter().additionalProperties();
        Object own = fm.get("feature");
        if (own instanceof String path) {
            Feature f = Feature.byPath(path);
            return f != null && !Features.enabled(f) ? f : null;
        }
        if (fm.get("item_ids") instanceof List<?> ids && !ids.isEmpty()) {
            Feature off = null;
            for (Object o : ids) {
                ResourceLocation id = ResourceLocation.tryParse(String.valueOf(o));
                if (id == null || !BettaCraft.MOD_ID.equals(id.getNamespace()) || !BuiltInRegistries.ITEM.containsKey(id)) return null;
                var item = BuiltInRegistries.ITEM.get(id);
                if (FeatureGate.item(item)) return null;
                if (off == null) for (Feature f : FeatureGate.of(item)) if (!Features.enabled(f)) off = f;
            }
            return off;
        }
        String path = page.getId().getPath();
        String name = path.substring(path.lastIndexOf('/') + 1).replace(".md", "");
        CritterSpecies s = CritterSpecies.byIdOrNull(name);
        if (s != null) for (Feature f : FeatureContent.ofCritter(s)) if (!Features.enabled(f)) return f;
        return null;
    }

    /** The pages hidden now: off themselves or below such a page in the navigation. */
    public static Set<ResourceLocation> hidden(PageCollection guide) {
        Set<ResourceLocation> out = new HashSet<>();
        for (NavigationNode root : guide.getNavigationTree().getRootNodes()) collect(guide, root, false, out);
        // pages outside the navigation
        for (ParsedGuidePage p : guide.getPages()) if (!out.contains(p.getId()) && offFeature(p) != null) out.add(p.getId());
        return out;
    }

    private static void collect(PageCollection guide, NavigationNode node, boolean parentHidden, Set<ResourceLocation> out) {
        boolean hidden = parentHidden || node.pageId() != null && offFeature(guide.getParsedPage(node.pageId())) != null;
        if (hidden && node.pageId() != null) out.add(node.pageId());
        for (NavigationNode c : node.children()) collect(guide, c, hidden, out);
    }

    /** Whether a navigation node is hidden (itself; its children go with it). */
    public static boolean hides(PageCollection guide, NavigationNode node) {
        return node.pageId() != null && offFeature(guide.getParsedPage(node.pageId())) != null;
    }

    /** A page of a feature that is off (itself or a page above it) starts with a notice in the feature's colour. */
    public static void addNotice(PageCollection guide, @Nullable ResourceLocation pageId, @Nullable LytDocument document) {
        if (pageId == null || document == null || !GuideBook.GUIDE_ID.getNamespace().equals(pageId.getNamespace())) return;
        Feature off = offFeature(guide.getParsedPage(pageId));
        if (off == null && hidden(guide).contains(pageId)) {
            // a page below a hidden chapter: name the chapter's feature
            for (ResourceLocation p = parent(guide, pageId); p != null && off == null; p = parent(guide, p)) off = offFeature(guide.getParsedPage(p));
        }
        if (off == null) return;
        List<LytBlock> blocks = new ArrayList<>(document.getBlocks());
        document.clearContent();
        LytParagraph notice = new LytParagraph();
        LytFlowSpan span = new LytFlowSpan();
        span.setStyle(TextStyle.builder().color(new ConstantColor(0xFFE0A030)).bold(true).build());
        span.append(LytFlowText.of(Component.translatable("guide.bettacraft.feature_off", Component.translatable(off.translationKey()), off.key()).getString()));
        notice.append(span);
        document.append(notice);
        for (LytBlock b : blocks) document.append(b);
    }

    private static java.lang.reflect.Field currentPageF;
    private static boolean noticeFailed;

    /** From the mixin on {@code GuideScreen.loadPage}, before its layout: the notice on the page just compiled. */
    public static void noticeOnScreen(Object guideScreen) {
        if (noticeFailed) return;
        try {
            if (currentPageF == null) {
                currentPageF = guideScreen.getClass().getDeclaredField("currentPage");
                currentPageF.setAccessible(true);
            }
            Object page = currentPageF.get(guideScreen);
            Object guide = guideScreen.getClass().getMethod("getGuide").invoke(guideScreen);
            if (page instanceof guideme.GuidePage gp && guide instanceof PageCollection pc) addNotice(pc, gp.id(), gp.document());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            noticeFailed = true;
            org.slf4j.LoggerFactory.getLogger("bettacraft/guide").warn("GuideME screen not as expected, no notice on pages of features that are off: {}", e.toString());
        }
    }

    /** From the mixin on the search: the results without hidden pages. */
    public static <T> List<T> filterSearch(List<T> results, Object guide, java.util.function.Function<T, ResourceLocation> page) {
        if (!(guide instanceof PageCollection pc) || results.isEmpty()) return results;
        Set<ResourceLocation> hidden;
        try {
            hidden = hidden(pc);
        } catch (RuntimeException e) {
            return results;
        }
        if (hidden.isEmpty()) return results;
        List<T> out = new ArrayList<>();
        for (T r : results) if (!hidden.contains(page.apply(r))) out.add(r);
        return out;
    }

    @Nullable
    private static ResourceLocation parent(PageCollection guide, ResourceLocation page) {
        ParsedGuidePage p = guide.getParsedPage(page);
        if (p == null || p.getFrontmatter().navigationEntry() == null) return null;
        return p.getFrontmatter().navigationEntry().parent();
    }
}
