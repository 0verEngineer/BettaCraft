package org.overengineer.bettacraft.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.overengineer.bettacraft.registry.ModSounds;

/**
 * M29.1 sound test: the client command {@code /bettacraft sounds} plays every device sound (own and Minecraft twin) one
 * after another at the player, with a chat line that names it. Client side only, so only the player hears it; it ignores the
 * sound switches and volumes of the client config (so the pump sounds that are off by default can be judged too).
 */
public final class SoundTest {
    /** Ticks between two sounds. */
    static final int GAP = 70;

    record Entry(Supplier<SoundEvent> event, String device, boolean vanilla, boolean offByDefault, int repeats) {}

    static final List<Entry> ENTRIES = List.of(
            new Entry(ModSounds.FILTER_RUNNING, "filter", false, false, 1),
            new Entry(ModSounds.FILTER_RUNNING_VANILLA, "filter", true, false, 1),
            new Entry(ModSounds.SPONGE_FILTER_GURGLE, "sponge_filter", false, false, 1),
            new Entry(ModSounds.SPONGE_FILTER_GURGLE_VANILLA, "sponge_filter", true, false, 1),
            new Entry(ModSounds.AIR_STONE_BUBBLES, "air_stone", false, false, 1),
            new Entry(ModSounds.AIR_STONE_BUBBLES_VANILLA, "air_stone", true, false, 1),
            new Entry(ModSounds.AIR_PUMP_HUM, "air_pump", false, true, 2),
            new Entry(ModSounds.AIR_PUMP_HUM_VANILLA, "air_pump", true, true, 1),
            new Entry(ModSounds.FLOW_PUMP_WHIR, "flow_pump", false, true, 2),
            new Entry(ModSounds.FLOW_PUMP_WHIR_VANILLA, "flow_pump", true, true, 1),
            new Entry(ModSounds.FEEDER_CLICK, "feeder", false, false, 1),
            new Entry(ModSounds.FEEDER_CLICK_VANILLA, "feeder", true, false, 1),
            new Entry(ModSounds.FOOD_MAKER_WIND, "food_maker", false, false, 1),
            new Entry(ModSounds.FOOD_MAKER_WIND_VANILLA, "food_maker", true, false, 1),
            new Entry(ModSounds.TANK_FILL, "tank_fill", false, false, 1),
            new Entry(ModSounds.TANK_DRAIN, "tank_drain", false, false, 1));

    private static int index = -1;
    private static int ticks;
    /** Played so far in this run (dev smoke test). */
    private static final List<String> PLAYED = new ArrayList<>();

    private SoundTest() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent e) -> e.getDispatcher().register(
                Commands.literal("bettacraft").then(Commands.literal("sounds").executes(ctx -> start(ctx.getSource())))));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
    }

    private static int start(CommandSourceStack src) {
        index = 0;
        ticks = 0;
        PLAYED.clear();
        src.sendSystemMessage(Component.translatable("command.bettacraft.sounds.start", ENTRIES.size(), GAP / 20.0));
        return ENTRIES.size();
    }

    /** Dev smoke test: whether a run is going, and the sound events played in the current or last run. */
    public static boolean runningForTest() {
        return index >= 0;
    }

    public static List<String> playedForTest() {
        return List.copyOf(PLAYED);
    }

    private static void tick() {
        if (index < 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            index = -1;
            return;
        }
        Entry e = ENTRIES.get(index);
        int loopTicks = 40; // the loops are 2 s long: play them again for a longer impression
        if (ticks == 0) {
            Component kind = Component.translatable("sound_test.bettacraft." + (e.vanilla() ? "vanilla" : "own"));
            if (e.offByDefault()) kind = kind.copy().append(" ").append(Component.translatable("command.bettacraft.sounds.off_by_default"));
            mc.player.displayClientMessage(Component.translatable("command.bettacraft.sounds.playing", index + 1, ENTRIES.size(),
                    Component.translatable("sound_test.bettacraft." + e.device()), Component.literal(e.event().get().getLocation().getPath())
                            .append(" (").append(kind).append(")")), false);
            PLAYED.add(e.event().get().getLocation().toString());
        }
        if (ticks % loopTicks == 0 && ticks / loopTicks < e.repeats()) {
            mc.getSoundManager().play(new SimpleSoundInstance(e.event().get().getLocation(), SoundSource.BLOCKS, 1.0F, 1.0F,
                    RandomSource.create(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true));
        }
        if (++ticks >= GAP) {
            ticks = 0;
            if (++index >= ENTRIES.size()) {
                index = -1;
                mc.player.displayClientMessage(Component.translatable("command.bettacraft.sounds.done"), false);
            }
        }
    }
}
