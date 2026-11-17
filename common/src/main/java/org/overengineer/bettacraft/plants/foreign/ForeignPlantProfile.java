package org.overengineer.bettacraft.plants.foreign;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;

/**
 * M38: one profile file {@code data/<ns>/bettacraft/foreign_plants/<name>.json}: the blocks (ids, or {@code #tags}) of a
 * plant of Minecraft or another mod, its {@link ForeignPlantRole} and a factor on top of the config factor.
 * <pre>
 * { "blocks": ["fintastic:hornwort"], "role": "stem", "factor": 1.0, "required": false }
 * </pre>
 * The blocks of one profile that touch each other form one plant (a kelp column, a carpet), so a profile should hold the
 * parts of one plant (kelp and kelp_plant). {@code required} warns about block ids that are not registered (default
 * false: a profile for a mod that is not installed is quietly skipped; ids missing from a loaded mod are always warned
 * about, they were probably renamed).
 *
 * @param blocks the block ids
 * @param tags   the block tags
 * @param role   what the plant is in our tanks
 * @param factor on top of {@code interiorPlants.foreignPlantFactor} (default 1)
 */
public record ForeignPlantProfile(List<ResourceLocation> blocks, List<ResourceLocation> tags, ForeignPlantRole role, double factor, boolean required) {
    /** A profile that cannot be read. */
    public static final class Invalid extends RuntimeException {
        public Invalid(String m) {
            super(m);
        }
    }

    /** Pure: reads a profile; problems that do not stop it go to {@code warn}. */
    public static ForeignPlantProfile parse(JsonObject o, Consumer<String> warn) {
        List<ResourceLocation> blocks = new ArrayList<>(), tags = new ArrayList<>();
        JsonElement b = o.get("blocks");
        if (b == null) b = o.get("block");
        if (b == null) throw new Invalid("no \"blocks\"");
        List<String> raw = new ArrayList<>();
        if (b.isJsonArray()) for (JsonElement e : b.getAsJsonArray()) raw.add(e.getAsString());
        else raw.add(b.getAsString());
        for (String s : raw) {
            boolean tag = s.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(tag ? s.substring(1) : s);
            if (id == null) {
                warn.accept("not a block id: " + s);
                continue;
            }
            (tag ? tags : blocks).add(id);
        }
        if (blocks.isEmpty() && tags.isEmpty()) throw new Invalid("empty \"blocks\" list");
        if (!o.has("role")) throw new Invalid("no \"role\"");
        ForeignPlantRole role = ForeignPlantRole.byId(o.get("role").getAsString());
        if (role == null) throw new Invalid("unknown role \"" + o.get("role").getAsString() + "\"");
        double factor = o.has("factor") ? o.get("factor").getAsDouble() : 1.0;
        if (factor < 0 || factor > 4) {
            warn.accept("factor " + factor + " outside 0..4, clamped");
            factor = Math.max(0, Math.min(4, factor));
        }
        boolean required = o.has("required") && o.get("required").getAsBoolean();
        return new ForeignPlantProfile(List.copyOf(blocks), List.copyOf(tags), role, factor, required);
    }
}
