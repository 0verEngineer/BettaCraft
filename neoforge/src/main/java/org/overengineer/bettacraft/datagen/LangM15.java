package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/** M15 English texts: sign size, leaving the interior, breaking a tank with players inside (de_de.json by hand). */
final class LangM15 {
    private LangM15() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("hud.bettacraft.interior.leave_hint", "Sneak + look up: leave");
        a.accept("hud.bettacraft.interior.leave_hint_sneak", "Hold sneak: leave");
        a.accept("hud.bettacraft.interior.leaving", "Leaving the tank...");
        a.accept("message.bettacraft.tank.break_blocked_interior", "%s is in the interior of this tank. It can only be broken when nobody is inside.");
        a.accept("bettacraft.configuration.signScale", "Name sign size");
        a.accept("bettacraft.configuration.signScale.tooltip", "Size of the tank name sign: factor on the default text height of 0.12 blocks (0.5 to 2)");
        a.accept("bettacraft.configuration.hud", "On-screen hints");
        a.accept("bettacraft.configuration.interiorLeaveHint", "Leave hint in the interior");
        a.accept("bettacraft.configuration.interiorLeaveHint.tooltip", "Show the small hint how to leave (sneak + look up) at the bottom left while you are in a tank interior; the progress bar while you hold it is always shown");
        a.accept("bettacraft.configuration.leaveLookUpDegrees", "Look up to leave (degrees)");
        a.accept("bettacraft.configuration.leaveLookUpDegrees.tooltip", "While sneaking you also have to look up at least this many degrees above the horizon to leave the interior (0 = sneaking alone is enough)");
    }
}
