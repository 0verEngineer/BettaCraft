package org.overengineer.bettacraft.compat.info;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.BettaTooltip;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.genetics.GenotypeText;
import org.overengineer.bettacraft.tank.TankCare;

/**
 * Text lines of the info overlays (PLAN 10, OFFEN 33), built from the tags of {@link InfoData}. Only vanilla
 * components, so the same lines work on the client (Jade, WTHIT) and on the server (The One Probe sends them).
 * {@code details} = sneaking (Jade/WTHIT) or the extended probe mode (The One Probe, sneaking by default).
 */
public final class InfoLines {
    private InfoLines() {}

    private static String f1(float f) {
        return String.format(Locale.ROOT, "%.1f", f);
    }

    private static Component pct(float f) {
        int p = Math.round(f * 100);
        return Component.literal(p + "%").withStyle(p > 50 ? ChatFormatting.GREEN : p > 25 ? ChatFormatting.GOLD : ChatFormatting.RED);
    }

    /**
     * M11: "Glass: Light Blue Stained Glass (textured)" - the localized name of the glass block (the id for glass of a
     * removed mod) and the style. Used by the overlays and the water tab of the tank menu.
     */
    public static Component glassLine(String glassId, String style) {
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(glassId);
        Component name;
        if (id != null && net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(id)) {
            name = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id).getName();
        } else {
            name = Component.literal(glassId);
        }
        Component st = Component.translatable(org.overengineer.bettacraft.tank.TankStyle.byId(style).key());
        return Component.translatable("jade.bettacraft.tank.glass", name.copy().withStyle(ChatFormatting.AQUA), st);
    }

    /**
     * M64: the first line of the tank's overlay (instead of the block name "Tank"): the tank's name, or for a tank without one
     * its kind (aquarium, paludarium, terrarium).
     */
    public static Component tankTitle(CompoundTag d) {
        if (d == null || d.isEmpty()) return null;
        String name = d.getString("name");
        Component base;
        if (!name.isEmpty()) base = Component.literal(name);
        else {
            String kind = d.getString("kind");
            base = Component.translatable("gui.bettacraft.terrarium.mode." + (kind.isEmpty() ? "aquarium" : kind));
        }
        // M66: the water quality in the same line: "Black Orchid - 97 % (clean)"
        if (!hasQuality(d)) return base;
        String st = d.getString("quality_state");
        ChatFormatting col = "bad".equals(st) ? ChatFormatting.RED : "poor".equals(st) ? ChatFormatting.GOLD : ChatFormatting.GREEN;
        return Component.empty().append(base).append(Component.literal(" – ").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable("jade.bettacraft.tank.title_quality", Math.round(d.getFloat("quality") * 100),
                        Component.translatable("gui.bettacraft.water.state." + st).withStyle(col)));
    }

    /** M66: whether the title carries the water quality (a tank with water that is no terrarium). */
    private static boolean hasQuality(CompoundTag d) {
        return d.contains("quality") && !"terrarium".equals(d.getString("kind")) && d.getLong("water") > 0;
    }

    /**
     * Tank (the title comes from {@link #tankTitle}). M64: without {@code details} only what a keeper looks at all the time:
     * the air of a terrarium or paludarium and the water quality of a tank with water (not of a terrarium: its water is a
     * dish), and a hint that sneaking shows the rest (with the number of warnings). With {@code details} everything: glass,
     * water (not for a terrarium), climate details, animals, nest, filters, tankmates, small animals, salt, algae, floating
     * plants, evaporation, power, warnings and the list of animals with their health.
     */
    public static List<Component> tank(CompoundTag d, boolean details) {
        List<Component> out = new ArrayList<>();
        if (d == null || d.isEmpty()) return out;
        boolean terrarium = "terrarium".equals(d.getString("kind"));
        // M31: the air of a terrarium / paludarium (always; the kind is the title of an unnamed tank, else in the details)
        if (d.contains("air")) {
            out.add(Component.translatable("jade.bettacraft.tank.air", f1(d.getFloat("air")), Math.round(d.getFloat("rh")), f1(d.getFloat("uv")))
                    .withStyle(ChatFormatting.GREEN));
        }
        // M9: the water quality moved into the title (M66, see tankTitle)
        ListTag warn = d.getList("warnings", Tag.TAG_STRING);
        ListTag fw = d.getList("filter_warn", Tag.TAG_STRING);
        if (!details) {
            int n = warn.size() + fw.size();
            out.add(n > 0 ? Component.translatable("jade.bettacraft.sneak_warnings", n).withStyle(ChatFormatting.GOLD)
                    : Component.translatable("jade.bettacraft.sneak").withStyle(ChatFormatting.DARK_GRAY));
            return out;
        }
        if (d.contains("glass")) out.add(glassLine(d.getString("glass"), d.getString("style")));
        if (!terrarium) out.add(Component.translatable("jade.bettacraft.tank.water", f1(d.getFloat("temp"))));
        if (d.contains("terra_mode")) {
            Component mode = Component.translatable(d.getString("terra_mode"));
            if (d.getBoolean("terra_fixed")) mode = Component.translatable("jade.bettacraft.tank.mode_fixed", mode); // M33.1
            out.add(mode.copy().withStyle(ChatFormatting.GREEN));
        }
        if (d.contains("air")) out.add(Component.translatable("jade.bettacraft.tank.climate_detail", f1(d.getFloat("bask")), Math.round(d.getFloat("land") * 100))
                .withStyle(ChatFormatting.GRAY));
        MutableComponent animals = Component.translatable("jade.bettacraft.tank.animals", d.getInt("count"), d.getInt("adults"));
        if (d.getBoolean("crowded")) animals.append(" ").append(Component.translatable("gui.bettacraft.tank.crowded").withStyle(ChatFormatting.RED));
        out.add(animals);
        String nest = d.getString("nest");
        if (!"none".equals(nest) && !nest.isEmpty()) {
            out.add(Component.translatable("jade.bettacraft.tank.nest", Component.translatable("jade.bettacraft.nest." + nest, d.getInt("eggs"))));
        }
        if (d.getInt("filters_out") + d.getInt("filters_in") > 0) {
            out.add(Component.translatable("jade.bettacraft.tank.filters", d.getInt("filters_out"), d.getInt("filters_in")));
        }
        // M19: tankmates by species, their clutches and the breeding box
        ListTag tm = d.getList("tankmates", Tag.TAG_COMPOUND);
        if (!tm.isEmpty()) {
            MutableComponent list = Component.empty();
            for (int i = 0; i < Math.min(3, tm.size()); i++) {
                if (i > 0) list.append(", ");
                list.append(tm.getCompound(i).getInt("n") + " ").append(Component.translatable(tm.getCompound(i).getString("k")));
            }
            if (tm.size() > 3) list.append(", …");
            out.add(Component.translatable("jade.bettacraft.tank.tankmates", list).withStyle(ChatFormatting.AQUA));
        }
        if (d.getInt("tm_eggs") > 0) out.add(Component.translatable("jade.bettacraft.tank.tm_eggs", d.getInt("tm_eggs")).withStyle(ChatFormatting.YELLOW));
        if (d.getInt("tm_box") >= 0 && d.contains("tm_box")) {
            out.add(Component.translatable("jade.bettacraft.tank.tm_box", d.getInt("tm_box")).withStyle(ChatFormatting.GRAY));
        }
        // M14: shrimp, crayfish, snails and the algae
        int[] cr = d.getIntArray("critters");
        if (cr.length == 4 && cr[0] + cr[1] + cr[2] > 0) {
            MutableComponent l = Component.translatable("jade.bettacraft.tank.critters", cr[0], cr[1], cr[2]);
            if (cr[3] > 0) l.append(" ").append(Component.translatable("jade.bettacraft.tank.critters_young", cr[3]));
            out.add(l.withStyle(ChatFormatting.GOLD));
            out.add(Component.translatable("jade.bettacraft.tank.stocking", f1(d.getFloat("stock"))).withStyle(ChatFormatting.GRAY));
        }
        // M20: salt water
        if (d.getFloat("salinity") >= org.overengineer.bettacraft.tank.Salinity.BRACKISH_FROM) {
            var kind = org.overengineer.bettacraft.tank.Salinity.kind(d.getFloat("salinity"));
            out.add(Component.translatable("jade.bettacraft.tank.salinity", String.format(java.util.Locale.ROOT, "%.1f", d.getFloat("salinity")),
                    Component.translatable("gui.bettacraft.reef.kind." + kind.name().toLowerCase(java.util.Locale.ROOT)), Math.round(d.getFloat("calcium")))
                    .withStyle(ChatFormatting.AQUA));
        }
        if (d.getFloat("algae") >= 0.1F) {
            out.add(Component.translatable("jade.bettacraft.tank.algae", Math.round(d.getFloat("algae") * 100)).withStyle(ChatFormatting.DARK_GREEN));
        }
        // M12: floating plants
        if (d.getInt("plants") > 0) {
            out.add(Component.translatable("jade.bettacraft.tank.plants", d.getInt("plants"), Math.min(100, Math.round(d.getFloat("plant_cover") * 100)))
                    .withStyle(ChatFormatting.DARK_GREEN));
        }
        // M9: evaporation and power
        if (d.getDouble("evaporation") > 0) {
            out.add(Component.translatable("jade.bettacraft.tank.evaporation", f1((float) d.getDouble("evaporation"))).withStyle(ChatFormatting.GRAY));
        }
        if (d.contains("power")) {
            MutableComponent p = Component.translatable("jade.bettacraft.tank.power", d.getLong("power"), d.getLong("power_cap"), d.getInt("demand"));
            if (d.getBoolean("unpowered")) p.append(" ").append(Component.translatable("jade.bettacraft.no_power").withStyle(ChatFormatting.RED));
            out.add(p);
        }
        if (d.contains("drive_inputs")) { // M41
            out.add(d.getBoolean("drive_overstressed") && d.getInt("drive_fe") <= 0
                    ? Component.translatable("jade.bettacraft.tank.drive_overstressed", d.getInt("drive_inputs")).withStyle(ChatFormatting.RED)
                    : Component.translatable("jade.bettacraft.tank.drive", d.getInt("drive_fe"), d.getInt("drive_inputs")).withStyle(ChatFormatting.GRAY));
        }
        for (int i = 0; i < warn.size(); i++) {
            out.add(Component.translatable("gui.bettacraft.breeding.warn." + warn.getString(i)).withStyle(ChatFormatting.RED));
            if (warn.getString(i).equals("tm_shoal")) { // M62: which fish, how many of how many
                ListTag sh = d.getList("shoals", Tag.TAG_STRING);
                for (int k = 0; k < sh.size(); k++) {
                    var s = org.overengineer.bettacraft.tank.menu.TankView.Shoal.parse(sh.getString(k));
                    if (s != null) out.add(Component.literal("  ").append(Component.translatable("gui.bettacraft.shoal.line", s.name(), s.count(), s.need()))
                            .withStyle(ChatFormatting.GOLD));
                }
            }
        }
        for (int i = 0; i < fw.size(); i++) {
            String[] kp = fw.getString(i).split("[|]", 2);
            if (kp.length == 2) out.add(Component.translatable("gui.bettacraft.water.filter_warning." + kp[1], kp[0]).withStyle(ChatFormatting.RED));
        }
        {
            ListTag list = d.getList("list", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag e = list.getCompound(i);
                MutableComponent line = Component.literal("  ");
                if (e.contains("sex")) line.append("female".equals(e.getString("sex")) ? "♀ " : "♂ ");
                line.append(TankCare.argComponent(e.getString("name")));
                if (e.contains("lb", Tag.TAG_COMPOUND)) {
                    var lb = org.overengineer.bettacraft.genetics.livebearer.LivebearerGenome.load(e.getCompound("lb"), e.getBoolean("endler"));
                    line.append(" ").append(org.overengineer.bettacraft.genetics.livebearer.LivebearerText.name(lb.express("female".equals(e.getString("sex"))))
                            .withStyle(ChatFormatting.GOLD));
                }
                if (e.contains("stage") && !LifeStage.ADULT.id().equals(e.getString("stage"))) {
                    line.append(" (").append(Component.translatable(LifeStage.byId(e.getString("stage")).translationKey())).append(")");
                }
                line.append(" ").append(pct(e.getFloat("health")));
                if (e.getBoolean("sick")) line.append(" ").append(Component.translatable("gui.bettacraft.fish.sick").withStyle(ChatFormatting.RED));
                out.add(line);
            }
        }
        return out;
    }

    /** External filter block and cabinet (M4). */
    public static List<Component> filter(CompoundTag d) {
        List<Component> out = new ArrayList<>();
        if (d == null || d.isEmpty()) return out;
        if (d.contains("storage")) {
            out.add(d.getBoolean("has_filter") ? Component.translatable("jade.bettacraft.cabinet.filter", d.getInt("heaters"))
                    : Component.translatable("jade.bettacraft.cabinet.no_filter").withStyle(ChatFormatting.GRAY));
            out.add(Component.translatable("jade.bettacraft.cabinet.storage", d.getInt("storage")));
            int[] s = d.getIntArray("sump"); // M49
            if (s.length == 5) {
                out.add(Component.translatable("jade.bettacraft.cabinet.sump", s[0], s[1], s[2]).withStyle(ChatFormatting.AQUA));
                if (s[3] > 0) out.add(Component.translatable(s[4] > 0 ? "jade.bettacraft.cabinet.refugium" : "jade.bettacraft.cabinet.refugium_dark", s[3])
                        .withStyle(s[4] > 0 ? ChatFormatting.GREEN : ChatFormatting.GOLD));
            }
        } else {
            out.add(Component.translatable("jade.bettacraft.filter.heaters", d.getInt("heaters"), d.getInt("heat")));
        }
        if (d.getInt("coolers") > 0) out.add(Component.translatable("jade.bettacraft.filter.coolers", d.getInt("coolers"), d.getInt("cool")));
        out.addAll(spring(d)); // M37
        if (d.contains("drive")) { // M41: the Create shaft of a cabinet
            Component side = Component.translatable("gui.bettacraft.cabinet.drive." + d.getString("drive"));
            int fe = d.getInt("drive_fe");
            out.add(d.getBoolean("drive_overstressed") ? Component.translatable("jade.bettacraft.cabinet.drive_overstressed", side).withStyle(ChatFormatting.RED)
                    : fe < 0 ? Component.translatable("jade.bettacraft.cabinet.drive_none", side).withStyle(ChatFormatting.GRAY)
                    : Component.translatable("jade.bettacraft.cabinet.drive", side, fe).withStyle(ChatFormatting.AQUA));
        }
        if (!d.getBoolean("has_filter")) return out;
        if (d.getBoolean("attached")) {
            String name = d.getString("tank");
            out.add(name.isEmpty() ? Component.translatable("jade.bettacraft.filter.tank_unnamed").withStyle(ChatFormatting.AQUA)
                    : Component.translatable("jade.bettacraft.filter.tank", name).withStyle(ChatFormatting.AQUA));
            if (d.getInt("below") > 0) out.add(Component.translatable("jade.bettacraft.filter.below", d.getInt("below")).withStyle(ChatFormatting.GRAY)); // M62
            if (d.getBoolean("unpowered")) out.add(Component.translatable("jade.bettacraft.no_power").withStyle(ChatFormatting.RED));
        } else {
            out.add(Component.translatable("jade.bettacraft.filter.no_tank").withStyle(ChatFormatting.GOLD));
            if (d.getBoolean("filter_block")) out.add(Component.translatable("jade.bettacraft.filter.link_hint").withStyle(ChatFormatting.GRAY)); // M62
        }
        return out;
    }

    /** M37: "Spring: 40 % (9.6 h)" of a device with a spring (nothing while it may not be wound). */
    public static List<Component> spring(CompoundTag d) {
        List<Component> out = new ArrayList<>();
        if (d == null) return out;
        waterDevice(out, d); // M68 part B
        if (!d.contains("spring")) return out;
        int pct = d.getInt("spring");
        if (d.getBoolean("spring_store")) { // M67: the tank's one store (cabinet, external filter)
            out.add(Component.translatable("jade.bettacraft.store", pct, String.format(java.util.Locale.ROOT, "%,d", d.getLong("store_fe")),
                    String.format(java.util.Locale.ROOT, "%,d", d.getLong("store_cap"))).withStyle(ChatFormatting.GOLD));
            return out;
        }
        out.add(Component.translatable("jade.bettacraft.spring", pct, d.getString("spring_hours"))
                .withStyle(pct <= 0 ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
        return out;
    }

    /**
     * M66, M68: what a pipe is and what it carries: its role (from its ring: a plain water line, an intake or return line of a filter,
     * fresh or waste water, a short circuit), its throughput a second (glass or steel) and, with the server's data ({@code d},
     * {@link InfoData#pipe}), what it moves now. A bundle: one line per pipe. Empty for any other block.
     */
    public static List<Component> pipe(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state,
                                       @javax.annotation.Nullable CompoundTag d) {
        List<Component> out = new ArrayList<>();
        int[] flows = d == null ? new int[0] : d.getIntArray("pipe_flow");
        if (state.getBlock() instanceof org.overengineer.bettacraft.plumbing.PipeBlock p) {
            pipeLines(out, null, state.getValue(org.overengineer.bettacraft.plumbing.PipeBlock.RING), p.steel(), flows.length > 0 ? flows[0] : -1, role(d, 0));
        } else if (state.getBlock() instanceof org.overengineer.bettacraft.plumbing.PipeBundleBlock) {
            int[] slots = org.overengineer.bettacraft.plumbing.Pipes.slotsAt(level, pos);
            int n = 0;
            for (int i = 0; i < slots.length; i++) {
                if (!org.overengineer.bettacraft.plumbing.PipeSlots.present(slots[i])) continue;
                pipeLines(out, ++n, org.overengineer.bettacraft.plumbing.PipeSlots.ring(slots[i]), org.overengineer.bettacraft.plumbing.PipeSlots.steel(slots[i]),
                        i < flows.length ? flows[i] : -1, role(d, i));
            }
        }
        return out;
    }

    /** M68 part B: the role key of pipe {@code i} from the server's data ("" without one). */
    private static String role(@javax.annotation.Nullable CompoundTag d, int i) {
        if (d == null) return "";
        var l = d.getList("pipe_role", net.minecraft.nbt.Tag.TAG_STRING);
        return i < l.size() ? l.getString(i) : "";
    }

    private static void pipeLines(List<Component> out, @javax.annotation.Nullable Integer slot, org.overengineer.bettacraft.plumbing.PipeRing ring, boolean steel, int flow, String roleKey) {
        // M68 part B: the four lines of the water change controller say which they are (two rings for four roles)
        boolean own = ring != org.overengineer.bettacraft.plumbing.PipeRing.SHORT && (roleKey.startsWith("fresh_") || roleKey.startsWith("waste_"));
        Component role = Component.translatable("jade.bettacraft.pipe.role." + (own ? roleKey : ring.getSerializedName()));
        Component line = slot == null ? role : Component.translatable("jade.bettacraft.pipe.slot", slot, role);
        ChatFormatting colour = switch (ring) {
            case NONE -> ChatFormatting.GRAY;
            case INTAKE -> ChatFormatting.BLUE;
            case RETURN -> ChatFormatting.GOLD;
            case FRESH -> ChatFormatting.GREEN;
            case WASTE -> ChatFormatting.DARK_RED;
            case SHORT -> ChatFormatting.RED;
        };
        out.add(line.copy().withStyle(colour));
        String rate = String.format(java.util.Locale.ROOT, "%,d", org.overengineer.bettacraft.plumbing.PipeBlock.rate(steel) * 20);
        if (flow >= 0) out.add(Component.translatable("jade.bettacraft.pipe.flow", String.format(java.util.Locale.ROOT, "%,d", flow), rate).withStyle(ChatFormatting.AQUA));
        else out.add(Component.translatable("jade.bettacraft.pipe.rate", rate).withStyle(ChatFormatting.AQUA));
    }

    /** M68 part B: the lines of the water change controller and the level sensor ({@link InfoData}'s spring data). */
    private static void waterDevice(List<Component> out, CompoundTag d) {
        if (d.contains("wc_status")) {
            org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity.Status st;
            try {
                st = org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity.Status.valueOf(d.getString("wc_status"));
            } catch (IllegalArgumentException e) {
                st = org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity.Status.WAITING;
            }
            out.add(org.overengineer.bettacraft.plumbing.ChangerTexts.status(st, d.getLong("wc_pending"), d.getInt("wc_last"), d.getInt("wc_top")));
            out.add(org.overengineer.bettacraft.plumbing.ChangerTexts.tanks(d.getInt("wc_tanks"), d.getInt("wc_filter"), d.getInt("wc_sensors")));
            out.add(org.overengineer.bettacraft.plumbing.ChangerTexts.lines(d.getInt("wc_flags")));
            out.addAll(org.overengineer.bettacraft.plumbing.ChangerTexts.warnings(d.getInt("wc_flags"), d.getInt("wc_tanks")));
        }
        if (d.contains("ls_status")) {
            String st = d.getString("ls_status").toLowerCase(java.util.Locale.ROOT);
            boolean low = st.equals("low");
            out.add(Component.translatable("jade.bettacraft.sensor.target", d.getInt("ls_target"),
                    Component.translatable("gui.bettacraft.level_sensor.status." + st)).withStyle(low ? ChatFormatting.GOLD : ChatFormatting.AQUA));
            if (d.getLong("ls_missing") > 0) out.add(Component.translatable("jade.bettacraft.sensor.missing", String.format(java.util.Locale.ROOT, "%,d", d.getLong("ls_missing"))).withStyle(ChatFormatting.GRAY));
            out.add(Component.translatable(low ? "jade.bettacraft.sensor.signal_on" : "jade.bettacraft.sensor.signal_off").withStyle(low ? ChatFormatting.RED : ChatFormatting.GRAY));
            out.add(Component.translatable(d.getBoolean("ls_served") ? "jade.bettacraft.sensor.served" : "jade.bettacraft.sensor.unserved")
                    .withStyle(d.getBoolean("ls_served") ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        }
    }

    /** M68 part B: the drain (client side only: it has no block entity). */
    public static List<Component> drain(net.minecraft.world.level.block.state.BlockState state) {
        List<Component> out = new ArrayList<>();
        if (!(state.getBlock() instanceof org.overengineer.bettacraft.plumbing.DrainBlock)) return out;
        out.add(Component.translatable(org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.DRAIN)
                ? "jade.bettacraft.drain" : "jade.bettacraft.drain.off").withStyle(ChatFormatting.GRAY));
        return out;
    }

    /** Larva tub and artemia hatchery (M9). */
    public static List<Component> culture(CompoundTag d) {
        List<Component> out = new ArrayList<>();
        if (d == null || d.isEmpty()) return out;
        boolean artemia = d.getBoolean("artemia");
        out.add(Component.translatable("jade.bettacraft.culture.water", d.getInt("water"),
                org.overengineer.bettacraft.food.CultureTubBlockEntity.WATER_CAPACITY));
        out.add(Component.translatable(artemia ? "jade.bettacraft.culture.cysts" : "jade.bettacraft.culture.organic", d.getInt("uses")));
        if (artemia) out.add(Component.translatable("jade.bettacraft.culture.bottles", d.getInt("bottles")));
        out.add(Component.translatable("jade.bettacraft.culture.progress", Math.round(d.getFloat("progress") * 100)));
        if (d.getInt("ready") > 0) {
            out.add(Component.translatable(artemia ? "jade.bettacraft.culture.ready_nauplii" : "jade.bettacraft.culture.ready_larvae", d.getInt("ready"))
                    .withStyle(ChatFormatting.GREEN));
        }
        if (d.contains("blocked")) {
            out.add(Component.translatable("message.bettacraft.culture.blocked." + d.getString("blocked")).withStyle(ChatFormatting.GOLD));
        }
        return out;
    }

    /**
     * Betta: type, sex and stage always (from the entity, also without server data); health and the first genotype
     * line in a tank; all genotype lines and the generation with {@code details}.
     */
    /** M51: the hired aquarist (from his synced data): whom he works for and what he does. */
    public static List<Component> aquarist(org.overengineer.bettacraft.aquarist.AquaristEntity a) {
        List<Component> out = new ArrayList<>();
        String status = a.status();
        boolean bad = status.equals("no_workplace") || status.equals("no_bed") || status.equals("unpaid") || status.equals("off");
        if (!a.employerName().isEmpty()) {
            out.add(Component.translatable(a.phase() == org.overengineer.bettacraft.aquarist.AquaristEntity.Phase.OFFERING ? "message.bettacraft.aquarist.offers_to"
                    : "message.bettacraft.aquarist.works_for", a.getName(), a.employerName()).withStyle(ChatFormatting.GRAY));
        }
        out.add(Component.translatable("gui.bettacraft.aquarist.status." + status).withStyle(bad ? ChatFormatting.RED : ChatFormatting.GREEN));
        return out;
    }

    public static List<Component> betta(BettaEntity b, CompoundTag d, boolean details) {
        List<Component> out = new ArrayList<>();
        if (d == null) d = new CompoundTag();
        MutableComponent stage = Component.translatable(b.lifeStage().translationKey());
        if (!b.isAdult() && d.contains("progress")) {
            stage = Component.translatable("gui.bettacraft.fish.stage_progress", stage, Math.round(d.getFloat("progress") * 100));
        }
        out.add(Component.translatable("jade.bettacraft.betta.type", BettaTooltip.type(b.genome())).withStyle(ChatFormatting.GOLD));
        out.add(Component.translatable("gui.bettacraft.fish.sex_stage", BettaTooltip.sex(b.isFemale()), stage));
        if (d.isEmpty()) return out;
        if (d.getBoolean("in_tank")) {
            MutableComponent h = Component.translatable("jade.bettacraft.betta.health", pct(d.getFloat("health")), pct(d.getFloat("satiation")));
            if (d.getBoolean("sick")) h.append(" ").append(Component.translatable("gui.bettacraft.fish.sick").withStyle(ChatFormatting.RED));
            else if (d.getBoolean("hungry")) h.append(" ").append(Component.translatable("jade.bettacraft.betta.hungry").withStyle(ChatFormatting.GOLD));
            out.add(h);
        }
        // M11: mirror training (mirror stick)
        if (d.getBoolean("trained")) out.add(Component.translatable("jade.bettacraft.betta.trained").withStyle(ChatFormatting.AQUA));
        if (d.getBoolean("genes_unknown") && details) out.add(Component.translatable("tooltip.bettacraft.genes.unknown").withStyle(ChatFormatting.GRAY)); // M22.3
        if (d.contains("genotype", Tag.TAG_COMPOUND)) {
            List<Component> g = GenotypeText.summary(BettaGenotype.load(d.getCompound("genotype")));
            if (details) {
                out.addAll(g);
                out.add(Component.translatable("gui.bettacraft.fish.generation", d.getInt("generation")).withStyle(ChatFormatting.GRAY));
            } else if (!g.isEmpty()) {
                out.add(g.getFirst());
            }
        }
        return out;
    }
}
