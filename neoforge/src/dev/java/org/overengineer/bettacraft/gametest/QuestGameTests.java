package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.api.ApiEvents;
import org.overengineer.bettacraft.api.BettacraftApi;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;

/**
 * M32 part d, M45 (with {@code -Pquests}: FTB Quests in the run, the chapter of {@code modpack/} copied into the run's quest
 * folder by build.gradle): FTB Quests loaded the chapter "Bettacraft" with all of its quests; every item task has its
 * item, every advancement task its advancement, every item reward its item, and the titles are our translation keys. M45:
 * advancements a player already has complete the advancement tasks of new quests (FTB Quests checks them every 5 ticks);
 * with {@code -Pkubejs} the example script {@code bettacraft_quests.js} hides the quests of switched-off features and shows
 * them again. By reflection (no compile dependency on FTB Quests). Without FTB Quests (or KubeJS) the tests pass with a log line.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class QuestGameTests {
    private static final String EMPTY = "empty_32x10x32";
    private static final int QUESTS = 117; // M47: aquarist, frog_tadpole; M48: bottom_fish, tetra_cory_form, mandarin, banggai, clown_larvae; M49: 7; M50: powered_trip; M51: aquarist_hired; M53: shared_tank; M56: 2 (terrarium plants); M67: tank_cranked; M68: drain

    private QuestGameTests() {}

    private static Object call(Object o, String name) throws ReflectiveOperationException {
        // M52: newer FTB Quests (the Fabric build) made some getters private (getFilename): from the class hierarchy too
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(name);
                m.setAccessible(true);
                return m.invoke(o);
            } catch (NoSuchMethodException ignored) {
                // up
            }
        }
        return o.getClass().getMethod(name).invoke(o);
    }

    private static boolean skip(String mod, String what) {
        if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded(mod)) return false;
        org.slf4j.LoggerFactory.getLogger("bettacraft/gametest").info("{} not installed: {} skipped", mod, what);
        return true;
    }

    /**
     * The title as a client shows it: on the server FTB Quests keeps it literal, the client parses it with
     * {@code TextUtils.parseRawText} (our JSON text component with the translation key).
     */
    private static Component title(GameTestHelper h, Object questObject) throws ReflectiveOperationException {
        String raw = (String) call(questObject, "getRawTitle");
        if (raw == null || raw.isEmpty()) raw = translatedTitle(questObject);
        Method parse = Class.forName("dev.ftb.mods.ftbquests.util.TextUtils").getMethod("parseRawText", String.class, HolderLookup.Provider.class);
        return (Component) parse.invoke(null, raw, h.getLevel().registryAccess());
    }

    /**
     * M52c: FTB Quests 26.1 moves the titles of a chapter into its translation tables on load ({@code lang/<locale>.json5}); the
     * raw title is empty then. The title of the fallback locale (en_us): our JSON text component with the translation key.
     */
    private static String translatedTitle(Object questObject) throws ReflectiveOperationException {
        Object file = questFile();
        Object manager = file.getClass().getMethod("getTranslationManager").invoke(file);
        Class<?> keyType = Class.forName("dev.ftb.mods.ftbquests.quest.translation.TranslationKey");
        Object titleKey = keyType.getField("TITLE").get(null);
        String locale = (String) file.getClass().getMethod("getFallbackLocale").invoke(file);
        Class<?> base = Class.forName("dev.ftb.mods.ftbquests.quest.QuestObjectBase");
        Object t = manager.getClass().getMethod("getStringTranslation", base, String.class, keyType).invoke(manager, questObject, locale, titleKey);
        return ((java.util.Optional<?>) t).map(Object::toString).orElse("");
    }

    /** The quest progress of the player's team: getOrCreateTeamData(Entity) before FTB Quests 26.1, getTeamData(Player) since. */
    private static Object teamData(Object file, ServerPlayer sp) throws ReflectiveOperationException {
        try {
            return file.getClass().getMethod("getOrCreateTeamData", net.minecraft.world.entity.Entity.class).invoke(file, sp);
        } catch (NoSuchMethodException e) {
            Object known = file.getClass().getMethod("getTeamData", net.minecraft.world.entity.player.Player.class).invoke(file, sp);
            if (known instanceof java.util.Optional<?> o && o.isPresent()) return o.get();
            // the personal team of a player has the player's UUID
            return file.getClass().getMethod("getOrCreateTeamData", java.util.UUID.class).invoke(file, sp.getUUID());
        }
    }

    private static Object questFile() throws ReflectiveOperationException {
        Object file = serverQuestFile(); // M52b: FTB Quests 2111 has getInstance() instead of the field INSTANCE
        if (file == null) throw new GameTestFailure("FTB Quests has no quest file");
        return file;
    }

    private static Object chapter() throws ReflectiveOperationException {
        for (Object c : (List<?>) call(questFile(), "getAllChapters")) if ("bettacraft".equals(call(c, "getFilename"))) return c;
        throw new GameTestFailure("no chapter bettacraft (copied into run/gametest/config/ftbquests?)");
    }

    /** Quest name (from the title key) -> quest. */
    private static Map<String, Object> quests(GameTestHelper h) throws ReflectiveOperationException {
        Map<String, Object> out = new HashMap<>();
        for (Object q : (List<?>) call(chapter(), "getQuests")) {
            if (title(h, q).getContents() instanceof TranslatableContents t && t.getKey().startsWith("quest.bettacraft.")) {
                out.put(t.getKey().substring("quest.bettacraft.".length(), t.getKey().length() - ".title".length()), q);
            }
        }
        return out;
    }

    /** The data of a quest as FTB Quests writes it (invisible, optional ...). */
    private static CompoundTag data(GameTestHelper h, Object quest) throws ReflectiveOperationException {
        CompoundTag nbt = new CompoundTag();
        for (Method m : quest.getClass().getMethods()) {
            if (!m.getName().equals("writeData") || m.getParameterCount() != 2 || m.getParameterTypes()[1] != HolderLookup.Provider.class) continue;
            Class<?> type = m.getParameterTypes()[0];
            if (type == CompoundTag.class) {
                m.invoke(quest, nbt, h.getLevel().registryAccess());
                return nbt;
            }
            if (type.getName().equals("de.marhali.json5.Json5Object")) {
                // M52c: FTB Quests 26.1 keeps its data as JSON5; the two flags the tests read
                Object json = type.getConstructor().newInstance();
                m.invoke(quest, json, h.getLevel().registryAccess());
                for (String key : new String[]{"invisible", "optional"}) {
                    if (!(boolean) type.getMethod("has", String.class).invoke(json, key)) continue;
                    Object element = type.getMethod("get", String.class).invoke(json, key);
                    nbt.putBoolean(key, (boolean) element.getClass().getMethod("getAsBoolean").invoke(element));
                }
                return nbt;
            }
        }
        throw new NoSuchMethodException("writeData of " + quest.getClass().getName());
    }

    @SuppressWarnings("unchecked")
    private static Set<String> tags(Object quest) throws ReflectiveOperationException {
        return (Set<String>) call(quest, "getTags");
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void ftbQuestsLoadsTheChapter(GameTestHelper h) {
        if (skip("ftbquests", "quest chapter test")) {
            h.succeed();
            return;
        }
        try {
            Object chapter = chapter();
            Component title = title(h, chapter);
            h.assertTrue(title.getContents() instanceof TranslatableContents t && t.getKey().equals("quest.bettacraft.chapter.title"),
                    "chapter title is our key: " + title);
            List<?> quests = (List<?>) call(chapter, "getQuests");
            h.assertTrue(quests.size() == QUESTS, "all quests: " + quests.size());
            List<String> problems = new ArrayList<>();
            int deps = 0, items = 0, advancements = 0, itemRewards = 0;
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
                        var adv = id == null ? null : h.getLevel().getServer().getAdvancements().get(net.minecraft.resources.Identifier.parse(id.toString()));
                        if (adv == null) problems.add("unknown advancement " + id + " in " + qt.getString());
                    } else {
                        problems.add("task " + type);
                    }
                }
                for (Object reward : (Collection<?>) call(q, "getRewards")) {
                    String type = reward.getClass().getSimpleName();
                    if (type.equals("ItemReward")) {
                        itemRewards++;
                        ItemStack s = (ItemStack) call(reward, "getItem");
                        if (s.isEmpty()) problems.add("item reward without item in " + qt.getString());
                    } else if (!type.equals("XPReward")) {
                        problems.add("reward " + type);
                    }
                }
                if (tags(q).isEmpty() && !List.of("first_tank", "fill_tank", "equipment", "water_change", "external_filter", "fish_bag", "cabinet",
                        "cooler", "swamp_water").contains(qt.getContents() instanceof TranslatableContents t ? t.getKey().split("\\.")[2] : "")) {
                    problems.add("no tags (feature switches) on " + qt.getString());
                }
            }
            h.assertTrue(problems.isEmpty(), String.join("; ", problems));
            h.assertTrue(deps >= QUESTS - 1 && items >= 40 && advancements >= 45 && itemRewards >= 30,
                    "dependencies " + deps + ", items " + items + ", advancements " + advancements + ", item rewards " + itemRewards);
        } catch (ReflectiveOperationException e) {
            throw new GameTestFailure("FTB Quests API changed: " + e);
        }
        h.succeed();
    }

    private static void grant(ServerPlayer p, String id) {
        var holder = p.level().getServer().getAdvancements().get(BettaCraft.id(id));
        if (holder == null) throw new GameTestFailure("no advancement " + id);
        for (String c : holder.value().criteria().keySet()) p.getAdvancements().award(holder, c);
    }

    /**
     * M45: a player who already has advancements (an old world: they were earned before the new quests came) gets the new
     * quests of them without doing anything again: FTB Quests checks advancement tasks every 5 ticks, once the quest may start.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void earnedAdvancementsCompleteNewQuests(GameTestHelper h) {
        if (skip("ftbquests", "quest progress test")) {
            h.succeed();
            return;
        }
        ServerPlayer sp = MockPlayers.serverPlayer(h, true);
        try {
            Map<String, Object> q = quests(h);
            for (String a : List.of("build_tank", "swamp_water", "mosquito_larvae", "paludarium")) grant(sp, a);
            Object file = questFile();
            Object team = teamData(file, sp);
            Method completed = team.getClass().getMethod("isCompleted", Class.forName("dev.ftb.mods.ftbquests.quest.QuestObject"));
            // the player tick of FTB Quests (a mock player is not ticked by a connection; the client smoke has a real one)
            h.onEachTick(() -> LoaderHooks.get().playerTickPost(sp));
            h.succeedWhen(() -> {
                try {
                    for (String n : List.of("first_tank", "swamp_water", "mosquito_larvae", "paludarium")) {
                        if ((boolean) completed.invoke(team, q.get(n))) continue;
                        Object task = ((Collection<?>) call(q.get(n), "getTasks")).iterator().next();
                        Object known = file.getClass().getMethod("getTeamData", net.minecraft.world.entity.player.Player.class).invoke(file, sp);
                        boolean start = (boolean) team.getClass().getMethod("canStartTasks", q.get(n).getClass()).invoke(team, q.get(n));
                        boolean submit = (boolean) task.getClass().getMethod("canSubmit", team.getClass(), ServerPlayer.class).invoke(task, team, sp);
                        throw new GameTestFailure(n + " not completed from the advancement the player had (team of the player " + known
                                + ", may start " + start + ", task can submit " + submit + ")");
                    }
                    h.assertTrue(!(boolean) completed.invoke(team, q.get("food_bred")), "food_bred not (no advancement)");
                    QuestAdv.gone(h, sp);
                } catch (ReflectiveOperationException e) {
                    throw new GameTestFailure("FTB Quests API changed: " + e);
                }
            });
        } catch (ReflectiveOperationException e) {
            QuestAdv.gone(h, sp);
            throw new GameTestFailure("FTB Quests API changed: " + e);
        }
    }

    /** Every Bettacraft quest is hidden (and optional) exactly while its tags are off. */
    private static void assertShownByTags(GameTestHelper h, Map<String, Object> quests, String when) throws ReflectiveOperationException {
        List<String> wrong = new ArrayList<>();
        for (Map.Entry<String, Object> e : quests.entrySet()) {
            Set<String> tags = tags(e.getValue());
            boolean show = BettacraftApi.questTagsOn(tags);
            CompoundTag d = data(h, e.getValue());
            boolean optional = !show || tags.contains(BettacraftApi.TAG_OPTIONAL);
            if (d.getBooleanOr("invisible", false) == show || d.getBooleanOr("optional", false) != optional) {
                wrong.add(e.getKey() + " (invisible " + d.getBooleanOr("invisible", false) + ", optional " + d.getBooleanOr("optional", false) + ", tags on " + show + ")");
            }
        }
        h.assertTrue(wrong.isEmpty(), when + ": " + String.join(", ", wrong));
    }

    /**
     * M45 (with {@code -Pquests -Pkubejs}): the example script has hidden the quests whose features are off when the server
     * started (by default the power quests without a power option and the Create quests without Create); switching the
     * tankmates off hides their quests and the quests that need them, switching them on again shows them (the script listens to
     * {@code FeaturesChangedEvent}, posted when the configs are reloaded). Config changed: own batch, restored.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m45_quest_hiding", timeoutTicks = 60)
    public static void scriptHidesQuestsOfSwitchedOffFeatures(GameTestHelper h) {
        if (skip("ftbquests", "quest hiding test") || skip("kubejs", "quest hiding test")) {
            h.succeed();
            return;
        }
        boolean saved = Features.own(Feature.TANKMATES);
        try {
            Map<String, Object> q = quests(h);
            h.assertTrue(q.size() == QUESTS, "all quests by name: " + q.size());
            ApiEvents.featuresChanged(h.getLevel().getServer()); // also a plain reload: nothing changes
            assertShownByTags(h, q, "after the start");
            h.assertTrue(data(h, q.get("travelling_tank")).getBooleanOr("invisible", false) == !org.overengineer.bettacraft.platform.Platform.get().isModLoaded("create"), "the Create quests by Create");
            h.assertTrue(data(h, q.get("fight")).getBooleanOr("optional", false), "the fight is optional by design");
            Features.set(Feature.TANKMATES, false);
            ApiEvents.featuresChanged(h.getLevel().getServer());
            for (String n : List.of("tankmates", "livebearer_litter", "guppy_strain", "guppy_collection", "cichlids", "reef_fish", "seahorse_fry")) {
                CompoundTag d = data(h, q.get(n));
                h.assertTrue(d.getBooleanOr("invisible", false) && d.getBooleanOr("optional", false), n + " hidden and optional with the tankmates off");
            }
            for (String n : List.of("first_tank", "fish_bag", "critters", "algae", "corals", "dynasty")) {
                h.assertTrue(!data(h, q.get(n)).getBooleanOr("invisible", false), n + " stays");
            }
            assertShownByTags(h, q, "tankmates off");
            Features.set(Feature.TANKMATES, true);
            ApiEvents.featuresChanged(h.getLevel().getServer());
            CompoundTag back = data(h, q.get("tankmates"));
            h.assertTrue(!back.getBooleanOr("invisible", false) && !back.getBooleanOr("optional", false), "shown and required again");
            assertShownByTags(h, q, "tankmates on again");
        } catch (ReflectiveOperationException e) {
            throw new GameTestFailure("FTB Quests API changed: " + e);
        } finally {
            Features.set(Feature.TANKMATES, saved);
            ApiEvents.featuresChanged(h.getLevel().getServer());
        }
        h.succeed();
    }

    /** The server's quest file: the field INSTANCE (FTB Quests 2101, 1.21.1) or getInstance() (2111, 1.21.11). */
    private static Object serverQuestFile() throws ReflectiveOperationException {
        Class<?> c = Class.forName("dev.ftb.mods.ftbquests.quest.ServerQuestFile");
        try {
            return c.getField("INSTANCE").get(null);
        } catch (NoSuchFieldException e) {
            return c.getMethod("getInstance").invoke(null);
        }
    }
}
