package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M40 part a English texts (the multiblock filter for several tanks: filter pipes, T-pieces, docks at any side, guards).
 * The German ones are in de_de.json; edit both.
 */
final class LangM40a {
    private LangM40a() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("block.bettacraft.filter_inlet_pipe", "Filter Inlet Pipe");
        a.accept("block.bettacraft.filter_outlet_pipe", "Filter Outlet Pipe");
        a.accept("block.bettacraft.pipe_anchor", "Pipe Dock");
        a.accept("block.bettacraft.intake_guard", "Intake Guard");
        a.accept("block.bettacraft.outlet_guard", "Outlet Guard");
        a.accept("item.bettacraft.pipe_tee", "Pipe T-Piece");
        a.accept("tooltip.bettacraft.filter_pipe.usage", "Joins at most two neighbours (a T-piece: all); docks on any side, the top or the bottom of a tank");
        a.accept("tooltip.bettacraft.pipe_tee.usage", "Use on a filter pipe: it branches to every side");
        a.accept("tooltip.bettacraft.guard.usage", "Use on the glass where a filter pipe docks, or place it in the interior at the pipe dock");
        a.accept("message.bettacraft.pipe_tee.already", "This pipe is a T-piece already");
        a.accept("message.bettacraft.pipe_tee.where", "Use the T-piece on a filter inlet or outlet pipe");
        a.accept("message.bettacraft.guard.no_dock", "No filter pipe docks here: use the guard on the glass where the pipe docks");
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
        a.accept("info.bettacraft.filter_inlet_pipe", "The intake line of the multiblock filter: from the tanks to the filter inlet. It joins at most two neighbours (a straight run or a bend); a pipe T-piece branches it, so one filter serves several tanks. It docks on any side, the top or the bottom of a tank: the pipe dock then appears inside at that spot. Its connection needs an intake guard (or the intake strainer) behind it, otherwise the filter does not filter that tank.");
        a.accept("info.bettacraft.filter_outlet_pipe", "The return line of the multiblock filter: from the filter outlet back into the tanks. It joins at most two neighbours; a pipe T-piece branches it. It docks on any side, the top or the bottom of a tank, and needs an outlet guard (or a lily pipe, nozzle or spray bar) behind the dock inside. A filter shares its cleaning and its flow between its tanks by their water volume.");
        a.accept("info.bettacraft.pipe_tee", "Use it on a filter inlet or outlet pipe: that pipe becomes a branch and joins every neighbour of its line, up to all six sides. Breaking the branch gives the pipe and the T-piece back.");
        a.accept("info.bettacraft.intake_guard", "A fine mesh over the opening of a filter intake in the tank: it keeps the animals out. Every dock of a filter inlet line needs one (or an intake strainer) behind it. Use it on the glass where the pipe docks, or place it in the interior next to the pipe dock or at the end of interior pipes from it.");
        a.accept("info.bettacraft.outlet_guard", "A mesh cap on a filter outlet in the tank: the animals cannot swim in. Every dock of a filter outlet line needs one (or a lily pipe, nozzle or spray bar) behind it. Use it on the glass where the pipe docks, or place it in the interior.");
    }
}
