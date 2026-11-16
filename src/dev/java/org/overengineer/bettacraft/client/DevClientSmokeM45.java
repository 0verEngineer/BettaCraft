package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.overengineer.bettacraft.BettaCraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M45 client smoke ({@code -Pquests -Pkubejs -Psmoketestm45}): the FTB Quests chapter "Bettacraft" in the quest book, the
 * whole chapter zoomed out and one quest with its rewards, in English and in German (screenshots
 * {@code bettacraft_m45_chapter_en}, {@code _chapter_de}, {@code _quest_en}, {@code _quest_de}). On the server: the KubeJS
 * script hid the power and Create quests (no power option, no Create), and advancements the player already has complete the
 * new quests (granted before; FTB Quests checks them on the player tick). By reflection (no compile dependency).
 */
final class DevClientSmokeM45 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private String lang;
    private Object screen;

    DevClientSmokeM45() {
        LOGGER.info("[smoketest] M45 mods: ftbquests {}, kubejs {}", ModList.get().isLoaded("ftbquests"), ModList.get().isLoaded("kubejs"));
        lang = mc.options.languageCode;
        then(1, () -> mc.options.hideGui = false);
        // the player already has some advancements (as in an old world): the new quests must complete by themselves
        then(1, () -> server(p -> {
            for (String a : List.of("build_tank", "fill_tank", "swamp_water", "mosquito_larvae", "paludarium", "tankmate_inserted")) {
                var holder = p.server.getAdvancements().get(BettaCraft.id(a));
                if (holder != null) for (String c : holder.value().criteria().keySet()) p.getAdvancements().award(holder, c);
            }
        }));
        then(60, this::checkServer);
        then(5, () -> language("en_us"));
        then(10, this::openChapter);
        then(40, () -> shot("chapter_en"));
        then(5, () -> openQuest("tankmates"));
        then(30, () -> shot("quest_en"));
        then(5, () -> language("de_de"));
        then(10, this::openChapter);
        then(40, () -> shot("chapter_de"));
        then(5, () -> openQuest("tankmates"));
        then(30, () -> shot("quest_de"));
        then(5, () -> {
            mc.setScreen(null);
            language(lang);
        });
    }

    private static Object call(Object o, String name, Object... args) throws ReflectiveOperationException {
        for (var m : o.getClass().getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == args.length) return m.invoke(o, args);
        }
        throw new NoSuchMethodException(name);
    }

    private static Object chapter(Object file) throws ReflectiveOperationException {
        for (Object c : (List<?>) call(file, "getAllChapters")) if ("bettacraft".equals(call(c, "getFilename"))) return c;
        return null;
    }

    /** Server side: the chapter, the hidden quests and the progress of the player. */
    private void checkServer() {
        server(p -> {
            try {
                Object file = Class.forName("dev.ftb.mods.ftbquests.quest.ServerQuestFile").getField("INSTANCE").get(null);
                Object chapter = chapter(file);
                if (chapter == null) {
                    LOGGER.error("[smoketest] M45 no chapter bettacraft (FAIL)");
                    return;
                }
                Object team = file.getClass().getMethod("getOrCreateTeamData", net.minecraft.world.entity.Entity.class).invoke(file, p);
                int done = 0, hidden = 0;
                StringBuilder hiddenNames = new StringBuilder();
                for (Object q : (List<?>) call(chapter, "getQuests")) {
                    String t = (String) call(q, "getRawTitle");
                    String n = t.replaceAll(".*quest\\.bettacraft\\.([a-z0-9_]+)\\.title.*", "$1");
                    net.minecraft.nbt.CompoundTag d = new net.minecraft.nbt.CompoundTag();
                    call(q, "writeData", d, p.server.registryAccess());
                    if (d.getBoolean("invisible")) {
                        hidden++;
                        hiddenNames.append(n).append(' ');
                    }
                    if ((boolean) call(team, "isCompleted", q)) {
                        done++;
                        LOGGER.info("[smoketest] M45 completed: {}", n);
                    }
                }
                LOGGER.info("[smoketest] M45 hidden quests ({}): {} {}", hidden, hiddenNames, hidden >= 2 || !ModList.get().isLoaded("kubejs") ? "PASS" : "FAIL");
                LOGGER.info("[smoketest] M45 quests completed from earned advancements: {} {}", done, done >= 5 ? "PASS" : "FAIL");
            } catch (ReflectiveOperationException e) {
                LOGGER.error("[smoketest] M45 server check (FAIL)", e);
            }
        });
    }

    private void openChapter() {
        try {
            Class<?> cqf = Class.forName("dev.ftb.mods.ftbquests.client.ClientQuestFile");
            Object file = cqf.getField("INSTANCE").get(null);
            call(file, "clearCachedData");
            screen = cqf.getMethod("openGui").invoke(null);
            Object chapter = chapter(file);
            if (chapter == null || screen == null) {
                LOGGER.error("[smoketest] M45 quest book or chapter missing (FAIL)");
                return;
            }
            call(screen, "selectChapter", chapter);
            for (int i = 0; i < 6; i++) call(screen, "addZoom", -1.0);
            LOGGER.info("[smoketest] M45 quest book open: {} zoom {}", mc.screen == null ? "NONE (FAIL)" : mc.screen.getClass().getSimpleName(), call(screen, "getZoom"));
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.error("[smoketest] M45 quest book (FAIL)", e);
        }
    }

    private void openQuest(String name) {
        try {
            Object file = Class.forName("dev.ftb.mods.ftbquests.client.ClientQuestFile").getField("INSTANCE").get(null);
            for (Object q : (List<?>) call(chapter(file), "getQuests")) {
                if (((String) call(q, "getRawTitle")).contains("quest.bettacraft." + name + ".title")) {
                    call(screen, "open", q, false);
                    return;
                }
            }
            LOGGER.error("[smoketest] M45 quest {} not found (FAIL)", name);
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.error("[smoketest] M45 quest (FAIL)", e);
        }
    }

    /** Switches the client language without a full resource reload (only the translations). */
    private void language(String code) {
        mc.setScreen(null);
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    boolean tick() {
        if (mc.player != null && !steps.isEmpty()) {
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (steps.isEmpty()) return true;
        Step st = steps.peek();
        if (wait < st.delay()) {
            wait++;
            return false;
        }
        steps.poll();
        wait = 0;
        st.action().run();
        return steps.isEmpty();
    }

    private void server(Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToasts().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m45_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
