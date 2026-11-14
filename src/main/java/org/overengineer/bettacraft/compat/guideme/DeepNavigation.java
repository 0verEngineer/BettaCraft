package org.overengineer.bettacraft.compat.guideme;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import guideme.document.LytRect;
import guideme.document.block.LytParagraph;
import guideme.layout.LayoutContext;
import guideme.layout.MinecraftFontMetrics;
import guideme.navigation.NavigationNode;
import guideme.navigation.NavigationTree;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M22.2: GuideME's navigation bar (21.1.x) only shows the root pages and their direct children, so pages one level deeper
 * (the species of a tankmate group, the plants of a growth form) were not reachable from the navigation. With the
 * mixins {@code GuideNavBarMixin} / {@code GuideNavBarRowMixin} the bar builds a row for every page of the tree, at any
 * depth: a row with children folds open and closed (the triangle moves in with its depth), a row is visible when all its
 * ancestors are open, and when the guide opens the path to the current page is open already.
 * <p>
 * GuideME is optional and its navigation bar is internal: everything here goes through reflection, and if anything does
 * not match (another GuideME version) the bar keeps its own two-level behaviour ({@link #recreate} returns false).
 */
public final class DeepNavigation {
    private static final Logger LOG = LoggerFactory.getLogger("bettacraft/guide");
    /** Indentation per level (px). */
    public static final int STEP = 9;
    private static final int NAVBAR_WIDTH = 150;

    private static boolean failed;
    private static Field rowsF, navTreeF, screenF, nodeF, paragraphF, expandedF, parentF, hasChildrenF, topF, bottomF;
    private static Constructor<?> rowCtor;
    private static Method updateLayoutM, setScrollM;
    private static Field scrollF;

    private DeepNavigation() {}

    private static synchronized boolean init(Object navbar) {
        if (failed) return false;
        if (rowCtor != null) return true;
        try {
            Class<?> bar = navbar.getClass();
            Class<?> row = Class.forName(bar.getName() + "$Row", false, bar.getClassLoader());
            rowsF = field(bar, "rows");
            navTreeF = field(bar, "navTree");
            screenF = field(bar, "screen");
            updateLayoutM = bar.getDeclaredMethod("updateLayout");
            updateLayoutM.setAccessible(true);
            setScrollM = bar.getDeclaredMethod("setScrollOffset", int.class);
            setScrollM.setAccessible(true);
            scrollF = field(bar, "scrollOffset");
            nodeF = field(row, "node");
            paragraphF = field(row, "paragraph");
            expandedF = field(row, "expanded");
            parentF = field(row, "parent");
            hasChildrenF = field(row, "hasChildren");
            topF = field(row, "top");
            bottomF = field(row, "bottom");
            Constructor<?> c = row.getDeclaredConstructor(bar, NavigationNode.class, row);
            c.setAccessible(true);
            rowCtor = c;
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            failed = true;
            LOG.warn("GuideME navigation bar not as expected, keeping its two levels: {}", e.toString());
            return false;
        }
    }

    private static Field field(Class<?> c, String name) throws NoSuchFieldException {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    /** Replaces {@code GuideNavBar.recreateRows()}: a row for every node of the tree. False = let GuideME do it. */
    @SuppressWarnings("unchecked")
    public static boolean recreate(Object navbar) {
        if (!init(navbar)) return false;
        try {
            Object screen = screenF.get(navbar);
            Object guide = screen.getClass().getMethod("getGuide").invoke(screen);
            NavigationTree tree = (NavigationTree) guide.getClass().getMethod("getNavigationTree").invoke(guide);
            navTreeF.set(navbar, tree);
            List<Object> rows = (List<Object>) rowsF.get(navbar);
            rows.clear();
            for (NavigationNode root : tree.getRootNodes()) add(navbar, rows, root, null);
            ResourceLocation current = currentPage(screen);
            if (current != null) {
                for (Object r : rows) {
                    if (contains((NavigationNode) nodeF.get(r), current) && !((NavigationNode) nodeF.get(r)).children().isEmpty()) {
                        expandedF.setBoolean(r, true);
                    }
                }
            }
            updateLayoutM.invoke(navbar);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true;
            LOG.warn("GuideME navigation bar: deep rows failed, keeping its two levels: {}", e.toString());
            return false;
        }
    }

    private static void add(Object navbar, List<Object> rows, NavigationNode node, Object parent) throws ReflectiveOperationException {
        Object row = rowCtor.newInstance(navbar, node, parent);
        rows.add(row);
        for (NavigationNode child : node.children()) {
            hasChildrenF.setBoolean(row, true);
            add(navbar, rows, child, row);
        }
    }

    private static boolean contains(NavigationNode node, ResourceLocation page) {
        if (page.equals(node.pageId())) return true;
        for (NavigationNode c : node.children()) if (contains(c, page)) return true;
        return false;
    }

    private static ResourceLocation currentPage(Object screen) {
        try {
            return (ResourceLocation) screen.getClass().getMethod("getCurrentPageId").invoke(screen);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** Replaces {@code GuideNavBar.updateLayout()}: indentation by depth. False = let GuideME do it. */
    @SuppressWarnings("unchecked")
    public static boolean layout(Object navbar) {
        if (failed || rowCtor == null) return false;
        try {
            LayoutContext context = new LayoutContext(new MinecraftFontMetrics());
            int y = 0;
            for (Object row : (List<Object>) rowsF.get(navbar)) {
                if (!visible(row)) continue;
                NavigationNode node = (NavigationNode) nodeF.get(row);
                int indent = depth(row) * STEP + 7;
                if (!node.iconFactory().get().isEmpty()) indent += 8;
                LytRect bounds = ((LytParagraph) paragraphF.get(row)).layout(context, indent, y, NAVBAR_WIDTH - indent);
                topF.setInt(row, bounds.y());
                bottomF.setInt(row, bounds.bottom());
                y = bounds.bottom();
            }
            // rows folded away: keep the scroll position inside the shorter list
            setScrollM.invoke(navbar, scrollF.getInt(navbar));
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true;
            LOG.warn("GuideME navigation bar: layout failed: {}", e.toString());
            return false;
        }
    }

    /** A row is visible when every ancestor is open. Null = unknown (let GuideME decide). */
    public static Boolean visible(Object row) {
        if (failed || rowCtor == null) return null;
        try {
            for (Object p = parentF.get(row); p != null; p = parentF.get(p)) if (!expandedF.getBoolean(p)) return false;
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** Levels above the row (0 = root). */
    public static int depth(Object row) {
        if (failed || rowCtor == null) return 0;
        try {
            int d = 0;
            for (Object p = parentF.get(row); p != null; p = parentF.get(p)) d++;
            return d;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return 0;
        }
    }

    private static Object navbar(Object guideScreen) throws ReflectiveOperationException {
        for (Field f : guideScreen.getClass().getDeclaredFields()) {
            if (f.getType().getSimpleName().equals("GuideNavBar")) {
                f.setAccessible(true);
                return f.get(guideScreen);
            }
        }
        return null;
    }

    /**
     * After a page was loaded (a link, the search, the item key): the chapters and sub-chapters above it fold open and the
     * navigation scrolls to its row, so the bar always shows where the reader is. Nothing else is folded.
     */
    @SuppressWarnings("unchecked")
    public static void reveal(Object guideScreen) {
        if (failed) return;
        try {
            Object navbar = navbar(guideScreen);
            if (navbar == null || !init(navbar)) return;
            List<Object> rows = (List<Object>) rowsF.get(navbar);
            ResourceLocation current = currentPage(guideScreen);
            if (rows.isEmpty() || current == null) return;
            Object row = null;
            for (Object r : rows) {
                NavigationNode node = (NavigationNode) nodeF.get(r);
                if (!node.children().isEmpty() && contains(node, current)) expandedF.setBoolean(r, true);
                if (current.equals(node.pageId())) row = r;
            }
            updateLayoutM.invoke(navbar);
            if (row != null) {
                int top = topF.getInt(row), bottom = bottomF.getInt(row), scroll = scrollF.getInt(navbar);
                int height = ((net.minecraft.client.gui.components.AbstractWidget) navbar).getHeight();
                if (top < scroll || bottom > scroll + height) setScrollM.invoke(navbar, Math.max(0, top - height / 3));
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.debug("reveal failed: {}", e.toString());
        }
    }

    /** Dev smoke: opens (or closes) every row of the navigation bar of the open guide screen. */
    @SuppressWarnings("unchecked")
    public static boolean expandAll(Object guideScreen, boolean open, java.util.function.Predicate<ResourceLocation> which) {
        try {
            Object navbar = navbar(guideScreen);
            if (navbar == null || !init(navbar)) return false;
            for (Object row : (List<Object>) rowsF.get(navbar)) {
                NavigationNode node = (NavigationNode) nodeF.get(row);
                if (!node.children().isEmpty()) expandedF.setBoolean(row, open && (node.pageId() == null || which.test(node.pageId())));
            }
            updateLayoutM.invoke(navbar);
            setScrollM.invoke(navbar, 0);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warn("expandAll failed: {}", e.toString());
            return false;
        }
    }
}
