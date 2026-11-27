package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M40 part a English texts (the multiblock filter for several tanks: filter pipes, T-pieces, docks at any side, guards).
 * The German ones are in de_de.json; edit both.
 */
final class LangM40a {
    private LangM40a() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("block.bettacraft.pipe_anchor", "Pipe Dock");
        a.accept("block.bettacraft.intake_guard", "Intake Guard");
        a.accept("block.bettacraft.outlet_guard", "Outlet Guard");
        a.accept("tooltip.bettacraft.guard.usage", "Use on the glass where a pipe docks, or place it in the interior at the pipe dock");
        a.accept("message.bettacraft.guard.no_dock", "No pipe docks here: use the guard on the glass where the pipe docks");
        a.accept("message.bettacraft.guard.already", "This connection is guarded already");
        a.accept("message.bettacraft.guard.installed", "Guard fitted: it sits behind the pipe dock in the interior");
        a.accept("message.bettacraft.plumbing.pass_through", "No: this pipe would lead into the tank and out again");
        a.accept("message.bettacraft.mb_filter.retrofit", "Bettacraft update: the multiblock filter at %s %s %s stopped filtering. Every pipe that docks on a tank now needs a guard inside (intake guard or intake strainer on the inlet line, outlet guard or an outlet on the return line). Use the guard on the glass where the pipe docks.");
        a.accept("gui.bettacraft.water.filter_warning.no_intake_guard", "Filter at %s: no intake guard in the tank, not filtering!");
        a.accept("gui.bettacraft.water.filter_warning.no_outlet_guard", "Filter at %s: no outlet guard in the tank, not filtering!");
        a.accept("gui.bettacraft.water.filter_warning.pass_through", "Filter at %s: a pipe leads through the tank, not filtering!");
        a.accept("gui.bettacraft.mb_filter.retrofit", "Update: this filter now needs guards in its tanks!");
        a.accept("gui.bettacraft.mb_filter.link", "Tank %s: %s %% · %s mB/t · %s blocks");
        a.accept("gui.bettacraft.mb_filter.link_off", "Tank %s: %s");
        a.accept("gui.bettacraft.mb_filter.link.ok", "working");
        a.accept("gui.bettacraft.mb_filter.link.no_intake_guard", "no intake guard, not filtering");
        a.accept("gui.bettacraft.mb_filter.link.no_outlet_guard", "no outlet guard, not filtering");
        a.accept("gui.bettacraft.mb_filter.link.pass_through", "a pipe leads through the tank, not filtering");
        a.accept("gui.bettacraft.mb_filter.link.no_water", "no water");
        a.accept("gui.bettacraft.mb_filter.more_tanks", "and %s more tanks");
        a.accept("info.bettacraft.intake_guard", "A fine mesh over the opening of a filter intake in the tank: it keeps the animals out. Every dock of a filter inlet line needs one (or an intake strainer) behind it. Use it on the glass where the pipe docks, or place it in the interior next to the pipe dock or at the end of interior pipes from it.");
        a.accept("info.bettacraft.outlet_guard", "A mesh cap on a filter outlet in the tank: the animals cannot swim in. Every dock of a filter outlet line needs one (or a lily pipe, nozzle or spray bar) behind it. Use it on the glass where the pipe docks, or place it in the interior.");
    }
}
