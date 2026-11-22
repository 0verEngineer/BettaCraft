package org.overengineer.bettacraft.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.api.BettacraftApi;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.FeatureContent;

/**
 * M32 part d, M45: the FTB Quests chapter for modpacks ({@code modpack/config/ftbquests/quests/chapters/bettacraft.snbt}, made
 * by {@code tools/modpack/ftbquests_chapter.py}) is valid SNBT, its ids are unique 16-digit hex numbers, every dependency is
 * a quest of the chapter (no cycles), every advancement task names one of our advancements, every item task and item reward
 * one of our items, and every text is a translation key that en_us and de_de have. M45: the quests of the first chapter
 * (1.25.0) keep their ids, tasks, dependencies and xp rewards (players keep their progress); the feature tags of every quest
 * cover its items, its rewards and its dependencies (a quest of a switch that is on never waits for a hidden one); every
 * feature group of the feature tree has a quest or is listed below with its reason. (The dev run with {@code -Pquests} also
 * loads it in FTB Quests: {@code QuestGameTests}.)
 */
class QuestChapterTest {
    private static Path root() {
        return Path.of(System.getProperty("bettacraft.projectDir", "."));
    }

    private static final Path CHAPTER = Path.of(System.getProperty("bettacraft.rootDir", ".."), "modpack", "config", "ftbquests", "quests", "chapters", "bettacraft.snbt").toAbsolutePath();

    /** M45: the feature groups without a quest of their own, and why. */
    static final Map<Feature, String> NO_QUEST = new EnumMap<>(Feature.class);

    static {
        NO_QUEST.put(Feature.BREAK_PROTECTION, "a safety rule (tanks with animals resist breaking), nothing to do");
        NO_QUEST.put(Feature.CLIP_FEEDER, "a smaller variant of the feeder, which has its quest");
        NO_QUEST.put(Feature.STONE_CABINETS, "a look of the cabinet (its quest names them); the item is the cabinet itself");
        NO_QUEST.put(Feature.CABINET_OUTLET_SIDE, "a setting in the cabinet menu, no task to check");
        NO_QUEST.put(Feature.CABINET_FRONTS, "an automatic look of cabinet rows (the cabinet quest names it), no task to check");
        NO_QUEST.put(Feature.CABINET_AREA, "M62: cabinets joining to the back, an automatic look and placement (the cabinet quest names it), no task to check");
        NO_QUEST.put(Feature.FILTER_BELOW_TANK, "M62: where the external filter may stand, no new item or advancement (the external filter quest covers the filter)");
        NO_QUEST.put(Feature.BIG_CONTAINERS, "a rule for buckets and bags, nothing to do");
        NO_QUEST.put(Feature.FILTER_MEDIA_AUTOMATION, "hoppers and buses feeding the multiblock filter, no own item or event");
        NO_QUEST.put(Feature.PLANT_MESHES, "a visual option");
        NO_QUEST.put(Feature.SWAYING_PLANTS, "a visual option");
        NO_QUEST.put(Feature.LIVELY_MOVEMENT, "M54: how the animals move (hours, pace, climbing, hiding, actions): nothing to do, no item or event");
        NO_QUEST.put(Feature.WORLD_CLIMBING, "M62: animals climb steps and walls in the world: nothing to do, no item or event");
        NO_QUEST.put(Feature.SMOOTH_SWIMMING, "a visual option");
        NO_QUEST.put(Feature.SOUNDS, "an audio option");
        NO_QUEST.put(Feature.WILD_SPAWN_PASS, "world generation, nothing to do");
        NO_QUEST.put(Feature.COMPAT_MEKANISM, "a recipe in a machine of another mod");
        NO_QUEST.put(Feature.COMPAT_ENDERIO, "a recipe in a machine of another mod");
        NO_QUEST.put(Feature.COMPAT_THERMAL, "a recipe in a machine of another mod");
        NO_QUEST.put(Feature.COMPAT_JEI, "recipe viewer integration");
        NO_QUEST.put(Feature.COMPAT_OVERLAYS, "info overlay integration (Jade, TOP, WTHIT)");
        NO_QUEST.put(Feature.COMPAT_GUIDEME, "the handbook itself");
        NO_QUEST.put(Feature.COMPAT_BETTER_FISH_TANKS, "integration with another mod's tanks");
        NO_QUEST.put(Feature.ALARM_WAYPOINTS, "map waypoints of the monitor alarms (the monitor quest names them)");
        NO_QUEST.put(Feature.MODPACK_EVENTS, "events for scripts, nothing for players");
    }

    private static JsonObject en, de;
    private static CompoundTag chapter;
    private static ListTag quests;
    /** Quest name (from its title key) -> quest. */
    private static final Map<String, CompoundTag> BY_NAME = new LinkedHashMap<>();
    private static final Map<String, CompoundTag> BY_ID = new HashMap<>();

    private static JsonObject lang(String rel) throws Exception {
        return JsonParser.parseString(Files.readString(root().resolve(rel), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @BeforeAll
    static void load() throws Exception {
        chapter = TagParser.parseTag(Files.readString(root().resolve(CHAPTER), StandardCharsets.UTF_8));
        en = lang("src/generated/resources/assets/bettacraft/lang/en_us.json");
        de = lang("src/main/resources/assets/bettacraft/lang/de_de.json");
        quests = chapter.getList("quests", Tag.TAG_COMPOUND);
        for (int i = 0; i < quests.size(); i++) {
            CompoundTag q = quests.getCompound(i);
            String k = JsonParser.parseString(q.getString("title")).getAsJsonObject().get("translate").getAsString();
            BY_NAME.put(k.substring("quest.bettacraft.".length(), k.length() - ".title".length()), q);
            BY_ID.put(q.getString("id"), q);
        }
    }

    private static void key(String raw, List<String> problems, String where) {
        JsonObject c = JsonParser.parseString(raw).getAsJsonObject();
        String k = c.get("translate").getAsString();
        if (!k.startsWith("quest.bettacraft.")) problems.add(where + ": key " + k + " is not quest.bettacraft.*");
        if (!en.has(k)) problems.add(where + ": en_us has no " + k);
        if (!de.has(k)) problems.add(where + ": de_de has no " + k);
    }

    private static boolean isItem(String id) {
        String n = id.substring(id.indexOf(':') + 1);
        return id.startsWith("bettacraft:") && (en.has("item.bettacraft." + n) || en.has("block.bettacraft." + n));
    }

    @Test
    void chapterIsValid() {
        List<String> problems = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        assertEquals("bettacraft", chapter.getString("filename"));
        ids.add(chapter.getString("id"));
        key(chapter.getString("title"), problems, "chapter title");
        key(chapter.getList("subtitle", Tag.TAG_STRING).getString(0), problems, "chapter subtitle");
        assertTrue(quests.size() >= 90, "the chapter of M45 has about 95 quests: " + quests.size());
        int advancements = 0, items = 0, itemRewards = 0;
        for (int i = 0; i < quests.size(); i++) {
            CompoundTag q = quests.getCompound(i);
            String qid = q.getString("id");
            if (!ids.add(qid)) problems.add("id twice: " + qid);
            key(q.getString("title"), problems, qid + " title");
            key(q.getList("description", Tag.TAG_STRING).getString(0), problems, qid + " description");
            for (Tag d : q.getList("dependencies", Tag.TAG_STRING)) {
                if (!BY_ID.containsKey(d.getAsString())) problems.add(qid + ": unknown dependency " + d.getAsString());
            }
            ListTag tasks = q.getList("tasks", Tag.TAG_COMPOUND);
            if (tasks.isEmpty()) problems.add(qid + ": no task");
            for (int j = 0; j < tasks.size(); j++) {
                CompoundTag t = tasks.getCompound(j);
                if (!ids.add(t.getString("id"))) problems.add("id twice: " + t.getString("id"));
                switch (t.getString("type")) {
                    case "advancement" -> {
                        advancements++;
                        String a = t.getString("advancement");
                        Path f = root().resolve("src/generated/resources/data/bettacraft/advancement/" + a.substring("bettacraft:".length()) + ".json");
                        if (!a.startsWith("bettacraft:") || !Files.exists(f)) problems.add(qid + ": no advancement " + a);
                    }
                    case "item" -> {
                        items++;
                        String item = t.getCompound("item").getString("id");
                        if (!isItem(item)) problems.add(qid + ": no item " + item);
                    }
                    default -> problems.add(qid + ": unexpected task type " + t.getString("type"));
                }
            }
            ListTag rewards = q.getList("rewards", Tag.TAG_COMPOUND);
            boolean xp = false;
            for (int j = 0; j < rewards.size(); j++) {
                CompoundTag r = rewards.getCompound(j);
                if (!ids.add(r.getString("id"))) problems.add("reward id twice in " + qid);
                switch (r.getString("type")) {
                    case "xp" -> xp = true;
                    case "item" -> {
                        itemRewards++;
                        String item = r.getCompound("item").getString("id");
                        if (!isItem(item)) problems.add(qid + ": reward of no item " + item);
                        int n = r.getInt("count");
                        if (n < 1 || n > 16) problems.add(qid + ": reward of " + n + " " + item + " (small rewards: 1 to 16)");
                    }
                    default -> problems.add(qid + ": unexpected reward type " + r.getString("type"));
                }
            }
            if (!xp) problems.add(qid + ": no xp reward");
            boolean optionalTag = q.getList("tags", Tag.TAG_STRING).stream().anyMatch(t -> t.getAsString().equals(BettacraftApi.TAG_OPTIONAL));
            if (optionalTag != q.getBoolean("optional")) problems.add(qid + ": optional and the tag " + BettacraftApi.TAG_OPTIONAL + " differ");
        }
        for (String id : ids) {
            if (!id.matches("[0-7][0-9A-F]{15}")) problems.add("not a 16-digit positive hex id: " + id);
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
        assertTrue(advancements >= 45 && items >= 40 && itemRewards >= 30, "advancement and item tasks, item rewards: " + advancements + ", " + items + ", " + itemRewards);
    }

    /** M45: the quests of the chapter of 1.25.0 keep their ids, tasks, dependencies and xp rewards (players keep their progress). */
    @Test
    void firstChapterQuestsAreUnchanged() throws Exception {
        List<String> problems = new ArrayList<>();
        int n = 0;
        for (String line : Files.readAllLines(root().resolve("src/test/resources/quests/chapter_1.25.0.txt"), StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) continue;
            n++;
            String[] p = line.split("\\|", -1);
            String name = p[0].trim(), id = p[1].trim();
            CompoundTag q = BY_ID.get(id);
            if (q == null) {
                problems.add(name + ": quest " + id + " is gone");
                continue;
            }
            if (BY_NAME.get(name) != q) problems.add(name + ": " + id + " has another title now");
            Set<String> tasks = new TreeSet<>();
            ListTag tl = q.getList("tasks", Tag.TAG_COMPOUND);
            for (int j = 0; j < tl.size(); j++) {
                CompoundTag t = tl.getCompound(j);
                tasks.add(t.getString("type").equals("advancement") ? t.getString("id") + "=advancement:" + t.getString("advancement")
                        : t.getString("id") + "=item:" + t.getCompound("item").getString("id") + "*" + t.getCompound("item").getInt("count"));
            }
            Set<String> wanted = new TreeSet<>(List.of(p[2].trim().split(" ")));
            if (!tasks.equals(wanted)) problems.add(name + ": tasks " + tasks + " instead of " + wanted);
            Set<String> deps = new TreeSet<>();
            for (Tag d : q.getList("dependencies", Tag.TAG_STRING)) deps.add(d.getAsString());
            Set<String> wantedDeps = new TreeSet<>();
            for (String d : p[3].trim().split(" ")) if (!d.isBlank()) wantedDeps.add(d);
            if (!deps.equals(wantedDeps)) problems.add(name + ": dependencies " + deps + " instead of " + wantedDeps);
            String[] xp = p[4].trim().split("=");
            boolean found = false;
            ListTag rl = q.getList("rewards", Tag.TAG_COMPOUND);
            for (int j = 0; j < rl.size(); j++) {
                CompoundTag r = rl.getCompound(j);
                if (r.getString("id").equals(xp[0]) && r.getString("type").equals("xp") && r.getInt("xp") == Integer.parseInt(xp[1])) found = true;
            }
            if (!found) problems.add(name + ": xp reward " + p[4].trim() + " changed");
        }
        assertEquals(33, n, "quests of the chapter of 1.25.0");
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void noDependencyCycles() {
        Map<String, Integer> state = new HashMap<>();
        for (String id : BY_ID.keySet()) assertTrue(acyclic(id, state), "dependency cycle through " + id);
    }

    private static boolean acyclic(String id, Map<String, Integer> state) {
        Integer s = state.get(id);
        if (s != null) return s == 2;
        state.put(id, 1);
        for (Tag d : BY_ID.get(id).getList("dependencies", Tag.TAG_STRING)) if (!acyclic(d.getAsString(), state)) return false;
        state.put(id, 2);
        return true;
    }

    private static List<Feature> features(CompoundTag q, List<String> problems) {
        List<Feature> out = new ArrayList<>();
        for (Tag t : q.getList("tags", Tag.TAG_STRING)) {
            String s = t.getAsString();
            if (s.startsWith(BettacraftApi.TAG_FEATURE)) {
                Feature f = Feature.byPath(s.substring(BettacraftApi.TAG_FEATURE.length()));
                if (f == null) problems.add(q.getString("id") + ": unknown feature tag " + s);
                else out.add(f);
            } else if (!s.equals(BettacraftApi.TAG_OPTIONAL) && !s.equals(BettacraftApi.TAG_NEEDS_POWER) && !s.equals(BettacraftApi.TAG_MOD + "create")) {
                problems.add(q.getString("id") + ": unknown tag " + s);
            }
        }
        return out;
    }

    /** Whether the quest's features switch {@code f} on with them (one of them is f or needs f). */
    private static boolean covered(List<Feature> tags, Feature f) {
        for (Feature g : tags) if (g == f || FeatureContent.implies(g, f)) return true;
        return false;
    }

    /**
     * M45: a quest is shown only while all of its feature tags are on (the KubeJS script). So its tags must cover the features
     * of its item tasks and its item rewards, and those of every quest it depends on: a shown quest never waits for a hidden one.
     */
    @Test
    void featureTagsCoverItemsRewardsAndDependencies() {
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, CompoundTag> e : BY_NAME.entrySet()) {
            CompoundTag q = e.getValue();
            List<Feature> tags = features(q, problems);
            for (Tag d : q.getList("dependencies", Tag.TAG_STRING)) {
                CompoundTag dq = BY_ID.get(d.getAsString());
                for (Feature f : features(dq, problems)) if (!covered(tags, f)) problems.add(e.getKey() + ": depends on a quest of " + f.path());
                for (Tag t : dq.getList("tags", Tag.TAG_STRING)) {
                    String s = t.getAsString();
                    if ((s.startsWith(BettacraftApi.TAG_MOD) || s.equals(BettacraftApi.TAG_NEEDS_POWER))
                            && q.getList("tags", Tag.TAG_STRING).stream().noneMatch(x -> x.getAsString().equals(s))) {
                        problems.add(e.getKey() + ": depends on a quest with " + s);
                    }
                }
            }
            ListTag tasks = q.getList("tasks", Tag.TAG_COMPOUND);
            for (int j = 0; j < tasks.size(); j++) {
                CompoundTag t = tasks.getCompound(j);
                if (!t.getString("type").equals("item")) continue;
                String item = t.getCompound("item").getString("id");
                for (Feature f : FeatureContent.ofItem(item.substring(item.indexOf(':') + 1))) {
                    if (!covered(tags, f)) problems.add(e.getKey() + ": item task " + item + " of " + f.path() + " without the tag");
                }
            }
            ListTag rewards = q.getList("rewards", Tag.TAG_COMPOUND);
            for (int j = 0; j < rewards.size(); j++) {
                CompoundTag r = rewards.getCompound(j);
                if (!r.getString("type").equals("item")) continue;
                String item = r.getCompound("item").getString("id");
                for (Feature f : FeatureContent.ofItem(item.substring(item.indexOf(':') + 1))) {
                    if (!covered(tags, f)) problems.add(e.getKey() + ": reward " + item + " of " + f.path() + " without the tag");
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    /** M45: every feature group (a switch without a parent) has a quest, or is listed in {@link #NO_QUEST} with its reason. */
    @Test
    void everyFeatureGroupHasAQuest() {
        List<String> problems = new ArrayList<>();
        Set<Feature> coveredGroups = new HashSet<>();
        for (CompoundTag q : BY_NAME.values()) {
            List<Feature> tags = features(q, problems);
            for (Feature f : Feature.values()) if (f.parent() == null && covered(tags, f)) coveredGroups.add(f);
        }
        for (Feature f : Feature.values()) {
            if (f.parent() != null) continue;
            boolean listed = NO_QUEST.containsKey(f);
            if (!coveredGroups.contains(f) && !listed) problems.add(f.path() + ": no quest and not in NO_QUEST");
            if (coveredGroups.contains(f) && listed) problems.add(f.path() + ": has a quest, remove it from NO_QUEST");
        }
        for (Feature f : NO_QUEST.keySet()) if (f.parent() != null) problems.add(f.path() + " in NO_QUEST is not a group");
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }
}
