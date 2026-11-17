package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/** M40 part c English texts (the pipe cover). The German ones are in de_de.json; edit both. */
final class LangM40c {
    private LangM40c() {}

    static void addAll(BiConsumer<String, String> a) {
        a.accept("item.bettacraft.pipe_cover", "Pipe Cover");
        a.accept("tooltip.bettacraft.pipe_cover.usage", "Main hand, a full block in the off hand: right-click a filter pipe or bundle and it looks like that block; sneak-right-click takes the cover off");
        a.accept("message.bettacraft.pipe_cover.where", "Use the pipe cover on a filter pipe or a pipe bundle, with a full block in the off hand");
        a.accept("message.bettacraft.pipe_cover.need_block", "Hold a full block in the off hand: the pipe is drawn as that block");
        a.accept("message.bettacraft.pipe_cover.not_full", "%s cannot cover a pipe: only full blocks without a block entity");
        a.accept("message.bettacraft.pipe_cover.none", "This pipe has no cover");
        a.accept("info.bettacraft.pipe_cover", "Hides filter pipes and pipe bundles. Hold it in the main hand and a full block (stone, planks, bricks, glass ...) in the off hand, then right-click a pipe: it is drawn as that block and is a full block to walk on, while its lines, T-pieces and connections stay as they were. One block is used and comes back when the cover comes off (sneak-right-click with the pipe cover) or the pipe is broken. The pipe cover itself is not used up. The facades of Applied Energistics 2, EnderIO conduit facades and Mekanism Covers can be used on the pipes directly, and the facades of the Cable Facades mod go on them as on any cable.");
    }
}
