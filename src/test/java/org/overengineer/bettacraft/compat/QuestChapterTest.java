package org.overengineer.bettacraft.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import org.junit.jupiter.api.Test;

/**
 * M32 part d: the FTB Quests chapter for modpacks ({@code modpack/config/ftbquests/quests/chapters/bettacraft.snbt}, made by
 * {@code tools/modpack/ftbquests_chapter.py}) is valid SNBT, its ids are unique 16-digit hex numbers, every dependency is
 * a quest of the chapter, every advancement task names one of our advancements, every item task one of our items, and
 * every text is a translation key that en_us and de_de have. (The dev run with {@code -Pquests} also loads it in FTB
 * Quests: {@code QuestGameTests}.)
 */
class QuestChapterTest {
    private static Path root() {
        return Path.of(System.getProperty("bettacraft.projectDir", "."));
    }

    private static final Path CHAPTER = Path.of("modpack", "config", "ftbquests", "quests", "chapters", "bettacraft.snbt");

    private static JsonObject lang(String rel) throws Exception {
        return JsonParser.parseString(Files.readString(root().resolve(rel), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static void key(String raw, JsonObject en, JsonObject de, List<String> problems, String where) {
        JsonObject c = JsonParser.parseString(raw).getAsJsonObject();
        String k = c.get("translate").getAsString();
        if (!k.startsWith("quest.bettacraft.")) problems.add(where + ": key " + k + " is not quest.bettacraft.*");
        if (!en.has(k)) problems.add(where + ": en_us has no " + k);
        if (!de.has(k)) problems.add(where + ": de_de has no " + k);
    }

    @Test
    void chapterIsValid() throws Exception {
        String text = Files.readString(root().resolve(CHAPTER), StandardCharsets.UTF_8);
        CompoundTag ch = TagParser.parseTag(text);
        JsonObject en = lang("src/generated/resources/assets/bettacraft/lang/en_us.json");
        JsonObject de = lang("src/main/resources/assets/bettacraft/lang/de_de.json");
        List<String> problems = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        assertEquals("bettacraft", ch.getString("filename"));
        ids.add(ch.getString("id"));
        key(ch.getString("title"), en, de, problems, "chapter title");
        key(ch.getList("subtitle", Tag.TAG_STRING).getString(0), en, de, problems, "chapter subtitle");
        ListTag quests = ch.getList("quests", Tag.TAG_COMPOUND);
        assertTrue(quests.size() >= 30, "a chapter of about 30 quests: " + quests.size());
        Set<String> questIds = new HashSet<>();
        for (int i = 0; i < quests.size(); i++) questIds.add(quests.getCompound(i).getString("id"));
        int advancements = 0, items = 0;
        for (int i = 0; i < quests.size(); i++) {
            CompoundTag q = quests.getCompound(i);
            String qid = q.getString("id");
            if (!ids.add(qid)) problems.add("id twice: " + qid);
            key(q.getString("title"), en, de, problems, qid + " title");
            key(q.getList("description", Tag.TAG_STRING).getString(0), en, de, problems, qid + " description");
            for (Tag d : q.getList("dependencies", Tag.TAG_STRING)) {
                if (!questIds.contains(d.getAsString())) problems.add(qid + ": unknown dependency " + d.getAsString());
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
                        String n = item.substring(item.indexOf(':') + 1);
                        if (!item.startsWith("bettacraft:") || !(en.has("item.bettacraft." + n) || en.has("block.bettacraft." + n))) {
                            problems.add(qid + ": no item " + item);
                        }
                    }
                    default -> problems.add(qid + ": unexpected task type " + t.getString("type"));
                }
            }
            ListTag rewards = q.getList("rewards", Tag.TAG_COMPOUND);
            for (int j = 0; j < rewards.size(); j++) if (!ids.add(rewards.getCompound(j).getString("id"))) problems.add("reward id twice in " + qid);
        }
        for (String id : ids) {
            if (!id.matches("[0-7][0-9A-F]{15}")) problems.add("not a 16-digit positive hex id: " + id);
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
        assertTrue(advancements >= 15 && items >= 15, "advancement and item tasks: " + advancements + ", " + items);
    }
}
