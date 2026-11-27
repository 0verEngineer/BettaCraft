package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M68 English texts (one pipe system: the pipe wrench, the roles and rings of the lines, bundles of any pipes, the interior pipes,
 * the short circuit). The German ones are in de_de.json; edit both.
 */
final class LangM68 {
    private LangM68() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("block.bettacraft.interior_steel_pipe", "Interior Pipe (Stainless Steel)");
        a.accept("item.bettacraft.pipe_wrench", "Pipe Wrench");
        a.accept("tooltip.bettacraft.pipe_wrench.usage", "Right-click a side of a pipe: closes it (the pipe no longer joins what is there) or opens it again");
        a.accept("tooltip.bettacraft.pipe_wrench.bundle", "On a bundle: the pipe you look at; sneaking moves it to the next spot");
        a.accept("tooltip.bettacraft.pipe.rate", "Carries up to %s mB/s");
        a.accept("tooltip.bettacraft.pipe.usage", "Joins every neighbour that fits; at a filter's inlet or outlet it becomes its intake or return line");
        a.accept("tooltip.bettacraft.pipe.bundle", "Sneak-right-click a pipe with it: both in one block (up to four); in a tank interior: an interior pipe");
        a.accept("message.bettacraft.pipe_wrench.closed", "Closed: %s");
        a.accept("message.bettacraft.pipe_wrench.opened", "Open: %s");
        a.accept("message.bettacraft.pipe_wrench.moved", "The pipe is in spot %s of the bundle now");
        a.accept("message.bettacraft.pipe_bundle.full", "Four pipes run here already");
        a.accept("direction.bettacraft.north", "north");
        a.accept("direction.bettacraft.south", "south");
        a.accept("direction.bettacraft.east", "east");
        a.accept("direction.bettacraft.west", "west");
        a.accept("direction.bettacraft.up", "up");
        a.accept("direction.bettacraft.down", "down");
        a.accept("jade.bettacraft.pipe.role.none", "Water line");
        a.accept("jade.bettacraft.pipe.role.intake", "Filter intake line");
        a.accept("jade.bettacraft.pipe.role.return", "Filter return line");
        a.accept("jade.bettacraft.pipe.role.fresh", "Fresh water line");
        a.accept("jade.bettacraft.pipe.role.waste", "Waste water line");
        a.accept("jade.bettacraft.pipe.role.short", "Short circuit: this line touches ports of different roles and does nothing");
        a.accept("jade.bettacraft.pipe.slot", "Pipe %s: %s");
        a.accept("jade.bettacraft.pipe.flow", "Moves %s of up to %s mB/s");
        a.accept("gui.bettacraft.mb_filter.problem.short_circuit", "Short circuit: a line of the filter also touches a port of another role (pipe wrench!)");
        a.accept("gui.bettacraft.mb_filter.line_limit", "Its lines carry at most %s mB/t");
        a.accept("info.bettacraft.pipe_wrench", "The pipe wrench closes and opens the sides of a pipe: right-click a side (an arm of the pipe, or the face of its middle where a side is closed). A closed side joins nothing and shows a cap; so two lines run side by side without joining, and joints are moved. On a bundle it works on the pipe you look at; sneak-right-click moves that pipe to the next spot of the bundle, to line it up with the next bundle. It is not used up.");
    }
}
