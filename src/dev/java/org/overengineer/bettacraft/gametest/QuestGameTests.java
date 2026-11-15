package org.overengineer.bettacraft.gametest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;

/**
 * M32 part d (with {@code -Pquests}: FTB Quests in the run, the chapter of {@code modpack/} copied into the run's quest
 * folder by build.gradle): FTB Quests loaded the chapter "Bettacraft" with all of its quests; every item task has its
 * item, every advancement task its advancement, and the titles are our translation keys. By reflection (no compile
 * dependency on FTB Quests). Without FTB Quests the test passes with a log line.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class QuestGameTests {
    private static final String EMPTY = "empty_32x10x32";
    private static final int QUESTS = 33;

    private QuestGameTests() {}

    private static Object call(Object o, String name) throws ReflectiveOperationException {
        Method m = o.getClass().getMethod(name);
        return m.invoke(o);
    }

    /**
     * The title as a client shows it: on the server FTB Quests keeps it literal, the client parses it with
     * {@code TextUtils.parseRawText} (our JSON text component with the translation key).
     */
    private static Component title(GameTestHelper h, Object questObject) throws ReflectiveOperationException {
        String raw = (String) call(questObject, "getRawTitle");
        Method parse = Class.forName("dev.ftb.mods.ftbquests.util.TextUtils").getMethod("parseRawText", String.class, net.minecraft.core.HolderLookup.Provider.class);
        return (Component) parse.invoke(null, raw, h.getLevel().registryAccess());
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void ftbQuestsLoadsTheChapter(GameTestHelper h) {
        if (!ModList.get().isLoaded("ftbquests")) {
            org.slf4j.LoggerFactory.getLogger("bettacraft/gametest").info("FTB Quests not installed: quest chapter test skipped");
            h.succeed();
            return;
        }
        try {
            Object file = Class.forName("dev.ftb.mods.ftbquests.quest.ServerQuestFile").getField("INSTANCE").get(null);
            if (file == null) throw new GameTestAssertException("FTB Quests has no quest file");
            Object chapter = null;
            for (Object c : (List<?>) call(file, "getAllChapters")) if ("bettacraft".equals(call(c, "getFilename"))) chapter = c;
            if (chapter == null) throw new GameTestAssertException("no chapter bettacraft (copied into run/gametest/config/ftbquests?)");
            Component title = title(h, chapter);
            h.assertTrue(title.getContents() instanceof TranslatableContents t && t.getKey().equals("quest.bettacraft.chapter.title"),
                    "chapter title is our key: " + title);
            List<?> quests = (List<?>) call(chapter, "getQuests");
            h.assertTrue(quests.size() == QUESTS, "all quests: " + quests.size());
            List<String> problems = new ArrayList<>();
            int deps = 0, items = 0, advancements = 0;
            for (Object q : quests) {
                Component qt = title(h, q);
                if (!(qt.getContents() instanceof TranslatableContents t) || !t.getKey().startsWith("quest.bettacraft.")) problems.add("title " + qt);
                deps += (int) ((java.util.stream.Stream<?>) call(q, "streamDependencies")).count();
                for (Object task : (Collection<?>) call(q, "getTasks")) {
                    String type = task.getClass().getSimpleName();
                    if (type.equals("ItemTask")) {
                        items++;
                        ItemStack s = (ItemStack) call(task, "getItemStack");
                        if (s.isEmpty()) problems.add("item task without item in " + qt.getString());
                    } else if (type.equals("AdvancementTask")) {
                        advancements++;
                        var f = task.getClass().getDeclaredField("advancement");
                        f.setAccessible(true);
                        Object id = f.get(task);
                        var adv = id == null ? null : h.getLevel().getServer().getAdvancements().get(net.minecraft.resources.ResourceLocation.parse(id.toString()));
                        if (adv == null) problems.add("unknown advancement " + id + " in " + qt.getString());
                    } else {
                        problems.add("task " + type);
                    }
                }
            }
            h.assertTrue(problems.isEmpty(), String.join("; ", problems));
            h.assertTrue(deps >= QUESTS - 1 && items >= 15 && advancements >= 15, "dependencies " + deps + ", items " + items + ", advancements " + advancements);
        } catch (ReflectiveOperationException e) {
            throw new GameTestAssertException("FTB Quests API changed: " + e);
        }
        h.succeed();
    }
}
