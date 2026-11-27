package org.overengineer.bettacraft.plumbing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * M68 part B: the texts of the water change controller, shared by its screen, the tank menu and the overlays (Jade, WTHIT, The One
 * Probe): its status, the four ports, what it serves, its stores and its warnings. Only components (no client class).
 */
public final class ChangerTexts {
    private ChangerTexts() {}

    /** The status line ({@code pendingMb}: what the running change still has to do). */
    public static Component status(WaterChangerBlockEntity.Status st, long pendingMb, int lastMb, int lastTopOff) {
        String key = "gui.bettacraft.water_changer.status." + st.name().toLowerCase(java.util.Locale.ROOT);
        Object arg = switch (st) {
            case CHANGING -> pendingMb;
            case TOPPED -> lastTopOff;
            default -> lastMb;
        };
        boolean ok = switch (st) {
            case CHANGED, WAITING, CHANGING, TOPPED -> true;
            default -> false;
        };
        return Component.translatable(key, arg).withStyle(ok ? ChatFormatting.AQUA : ChatFormatting.GOLD);
    }

    /** "Next change in 3.5 days". */
    public static Component next(long last, int intervalDays, long now) {
        double days = Math.max(0, (last + intervalDays * 24000L - now) / 24000.0);
        return Component.translatable("gui.bettacraft.water_changer.next", String.format(java.util.Locale.ROOT, "%.1f", days)).withStyle(ChatFormatting.GRAY);
    }

    /** "Serves 2 tanks (2 through a multiblock filter) · 1 level sensor". */
    public static Component tanks(int tanks, int filterTanks, int sensors) {
        return Component.translatable("gui.bettacraft.water_changer.tanks", tanks, filterTanks, sensors).withStyle(ChatFormatting.DARK_AQUA);
    }

    /** "Fresh in ✔ · fresh out touch · waste in touch · waste out ✘" from the synced flags. */
    public static Component lines(int flags) {
        boolean touch = (flags & WaterChangerBlockEntity.CONTACT) != 0;
        MutableComponent c = Component.empty();
        port(c, "fresh_in", (flags & WaterChangerBlockEntity.FRESH_LINE) != 0, false);
        c.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
        port(c, "fresh_out", (flags & WaterChangerBlockEntity.FRESH_OUT_LINE) != 0, touch);
        c.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
        port(c, "waste_in", (flags & WaterChangerBlockEntity.WASTE_IN_LINE) != 0, touch);
        c.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
        port(c, "waste_out", (flags & WaterChangerBlockEntity.WASTE_LINE) != 0, false);
        return c;
    }

    private static void port(MutableComponent c, String key, boolean line, boolean touch) {
        c.append(Component.translatable("gui.bettacraft.water_changer.port." + key).withStyle(ChatFormatting.GRAY));
        c.append(Component.literal(" "));
        if (line) c.append(Component.literal("✔").withStyle(ChatFormatting.GREEN));
        else if (touch) c.append(Component.translatable("gui.bettacraft.water_changer.port.touch").withStyle(ChatFormatting.GREEN));
        else c.append(Component.literal("✘").withStyle(ChatFormatting.RED));
    }

    /** "Its stores: fresh 0 mB, waste 250 mB · topped off 1,200 mB in all". */
    public static Component stores(int fresh, int waste, long topped) {
        return Component.translatable("gui.bettacraft.water_changer.stores", fresh, waste, topped).withStyle(ChatFormatting.GRAY);
    }

    /** The warnings: a short circuit, no fresh water line, no drain, it sucks in young animals. */
    public static List<Component> warnings(int flags, int tanks) {
        List<Component> out = new ArrayList<>();
        if ((flags & WaterChangerBlockEntity.SHORT) != 0) out.add(warn("gui.bettacraft.water_changer.warn.short"));
        if (tanks > 0 && (flags & WaterChangerBlockEntity.FRESH_LINE) == 0) out.add(warn("gui.bettacraft.water_changer.warn.no_fresh"));
        if (tanks > 0 && (flags & WaterChangerBlockEntity.WASTE_LINE) == 0) out.add(warn("gui.bettacraft.water_changer.warn.no_drain"));
        if ((flags & WaterChangerBlockEntity.YOUNG) != 0) out.add(warn("gui.bettacraft.water_changer.warn.young"));
        return out;
    }

    private static Component warn(String key) {
        return Component.literal("⚠ ").append(Component.translatable(key)).withStyle(ChatFormatting.GOLD);
    }
}
