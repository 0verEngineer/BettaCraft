package org.overengineer.bettacraft.compat.info;

import org.overengineer.bettacraft.util.NbtCompat;
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
        net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.tryParse(glassId);
        Component name;
        if (id != null && net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(id)) {
            name = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(id).getName();
        } else {
            name = Component.literal(glassId);
        }
        Component st = Component.translatable(org.overengineer.bettacraft.tank.TankStyle.byId(style).key());
        return Component.translatable("jade.bettacraft.tank.glass", name.copy().withStyle(ChatFormatting.AQUA), st);
    }

    /**
     * Tank: name, temperature and water, animals, nest, filters and warnings always; the list of animals with health
     * only with {@code details}.
     */
    public static List<Component> tank(CompoundTag d, boolean details) {
        List<Component> out = new ArrayList<>();
        if (d == null || d.isEmpty()) return out;
        if (!d.getStringOr("name", "").isEmpty()) out.add(Component.literal(d.getStringOr("name", "")).withStyle(ChatFormatting.WHITE));
        if (d.contains("glass")) out.add(glassLine(d.getStringOr("glass", ""), d.getStringOr("style", "")));
        out.add(Component.translatable("jade.bettacraft.tank.water", f1(d.getFloatOr("temp", 0f)), d.getLongOr("water", 0L), d.getLongOr("cap", 0L)));
        // M31: terrarium / paludarium climate
        if (d.contains("terra_mode")) {
            Component mode = Component.translatable(d.getStringOr("terra_mode", ""));
            if (d.getBooleanOr("terra_fixed", false)) mode = Component.translatable("jade.bettacraft.tank.mode_fixed", mode); // M33.1
            if (d.contains("air")) {
                out.add(Component.translatable("jade.bettacraft.tank.climate", mode, f1(d.getFloatOr("air", 0f)), Math.round(d.getFloatOr("rh", 0f)), f1(d.getFloatOr("uv", 0f)))
                        .withStyle(ChatFormatting.GREEN));
                if (details) out.add(Component.translatable("jade.bettacraft.tank.climate_detail", f1(d.getFloatOr("bask", 0f)), Math.round(d.getFloatOr("land", 0f) * 100))
                        .withStyle(ChatFormatting.GRAY));
            } else {
                out.add(mode.copy().withStyle(ChatFormatting.GREEN));
            }
        }
        MutableComponent animals = Component.translatable("jade.bettacraft.tank.animals", d.getIntOr("count", 0), d.getIntOr("adults", 0));
        if (d.getBooleanOr("crowded", false)) animals.append(" ").append(Component.translatable("gui.bettacraft.tank.crowded").withStyle(ChatFormatting.RED));
        out.add(animals);
        String nest = d.getStringOr("nest", "");
        if (!"none".equals(nest) && !nest.isEmpty()) {
            out.add(Component.translatable("jade.bettacraft.tank.nest", Component.translatable("jade.bettacraft.nest." + nest, d.getIntOr("eggs", 0))));
        }
        if (d.getIntOr("filters_out", 0) + d.getIntOr("filters_in", 0) > 0) {
            out.add(Component.translatable("jade.bettacraft.tank.filters", d.getIntOr("filters_out", 0), d.getIntOr("filters_in", 0)));
        }
        // M19: tankmates by species, their clutches and the breeding box
        ListTag tm = d.getListOrEmpty("tankmates");
        if (!tm.isEmpty()) {
            MutableComponent list = Component.empty();
            for (int i = 0; i < Math.min(3, tm.size()); i++) {
                if (i > 0) list.append(", ");
                list.append(tm.getCompoundOrEmpty(i).getIntOr("n", 0) + " ").append(Component.translatable(tm.getCompoundOrEmpty(i).getStringOr("k", "")));
            }
            if (tm.size() > 3) list.append(", …");
            out.add(Component.translatable("jade.bettacraft.tank.tankmates", list).withStyle(ChatFormatting.AQUA));
        }
        if (d.getIntOr("tm_eggs", 0) > 0) out.add(Component.translatable("jade.bettacraft.tank.tm_eggs", d.getIntOr("tm_eggs", 0)).withStyle(ChatFormatting.YELLOW));
        if (d.getIntOr("tm_box", 0) >= 0 && d.contains("tm_box")) {
            out.add(Component.translatable("jade.bettacraft.tank.tm_box", d.getIntOr("tm_box", 0)).withStyle(ChatFormatting.GRAY));
        }
        // M14: shrimp, crayfish, snails and the algae
        int[] cr = d.getIntArray("critters").orElse(new int[0]);
        if (cr.length == 4 && cr[0] + cr[1] + cr[2] > 0) {
            MutableComponent l = Component.translatable("jade.bettacraft.tank.critters", cr[0], cr[1], cr[2]);
            if (cr[3] > 0) l.append(" ").append(Component.translatable("jade.bettacraft.tank.critters_young", cr[3]));
            out.add(l.withStyle(ChatFormatting.GOLD));
            if (details) out.add(Component.translatable("jade.bettacraft.tank.stocking", f1(d.getFloatOr("stock", 0f))).withStyle(ChatFormatting.GRAY));
        }
        // M20: salt water
        if (d.getFloatOr("salinity", 0f) >= org.overengineer.bettacraft.tank.Salinity.BRACKISH_FROM) {
            var kind = org.overengineer.bettacraft.tank.Salinity.kind(d.getFloatOr("salinity", 0f));
            out.add(Component.translatable("jade.bettacraft.tank.salinity", String.format(java.util.Locale.ROOT, "%.1f", d.getFloatOr("salinity", 0f)),
                    Component.translatable("gui.bettacraft.reef.kind." + kind.name().toLowerCase(java.util.Locale.ROOT)), Math.round(d.getFloatOr("calcium", 0f)))
                    .withStyle(ChatFormatting.AQUA));
        }
        if (d.getFloatOr("algae", 0f) >= 0.1F) {
            out.add(Component.translatable("jade.bettacraft.tank.algae", Math.round(d.getFloatOr("algae", 0f) * 100)).withStyle(ChatFormatting.DARK_GREEN));
        }
        // M12: floating plants
        if (d.getIntOr("plants", 0) > 0) {
            out.add(Component.translatable("jade.bettacraft.tank.plants", d.getIntOr("plants", 0), Math.min(100, Math.round(d.getFloatOr("plant_cover", 0f) * 100)))
                    .withStyle(ChatFormatting.DARK_GREEN));
        }
        // M9: water quality, evaporation and power
        if (d.contains("quality")) {
            String st = d.getStringOr("quality_state", "");
            ChatFormatting col = "bad".equals(st) ? ChatFormatting.RED : "poor".equals(st) ? ChatFormatting.GOLD : ChatFormatting.GREEN;
            out.add(Component.translatable("jade.bettacraft.tank.quality", Math.round(d.getFloatOr("quality", 0f) * 100),
                    Component.translatable("gui.bettacraft.water.state." + st).withStyle(col)));
        }
        if (details && d.getDoubleOr("evaporation", 0d) > 0) {
            out.add(Component.translatable("jade.bettacraft.tank.evaporation", f1((float) d.getDoubleOr("evaporation", 0d))).withStyle(ChatFormatting.GRAY));
        }
        if (d.contains("power")) {
            MutableComponent p = Component.translatable("jade.bettacraft.tank.power", d.getLongOr("power", 0L), d.getLongOr("power_cap", 0L), d.getIntOr("demand", 0));
            if (d.getBooleanOr("unpowered", false)) p.append(" ").append(Component.translatable("jade.bettacraft.no_power").withStyle(ChatFormatting.RED));
            out.add(p);
        }
        if (d.contains("drive_inputs")) { // M41
            out.add(d.getBooleanOr("drive_overstressed", false) && d.getIntOr("drive_fe", 0) <= 0
                    ? Component.translatable("jade.bettacraft.tank.drive_overstressed", d.getIntOr("drive_inputs", 0)).withStyle(ChatFormatting.RED)
                    : Component.translatable("jade.bettacraft.tank.drive", d.getIntOr("drive_fe", 0), d.getIntOr("drive_inputs", 0)).withStyle(ChatFormatting.GRAY));
        }
        ListTag warn = d.getListOrEmpty("warnings");
        for (int i = 0; i < warn.size(); i++) {
            out.add(Component.translatable("gui.bettacraft.breeding.warn." + warn.getStringOr(i, "")).withStyle(ChatFormatting.RED));
        }
        ListTag fw = d.getListOrEmpty("filter_warn");
        for (int i = 0; i < fw.size(); i++) {
            String[] kp = fw.getStringOr(i, "").split("[|]", 2);
            if (kp.length == 2) out.add(Component.translatable("gui.bettacraft.water.filter_warning." + kp[1], kp[0]).withStyle(ChatFormatting.RED));
        }
        if (details) {
            ListTag list = d.getListOrEmpty("list");
            for (int i = 0; i < list.size(); i++) {
                CompoundTag e = list.getCompoundOrEmpty(i);
                MutableComponent line = Component.literal("  ");
                if (e.contains("sex")) line.append("female".equals(e.getStringOr("sex", "")) ? "♀ " : "♂ ");
                line.append(TankCare.argComponent(e.getStringOr("name", "")));
                if (NbtCompat.has(e, "lb", Tag.TAG_COMPOUND)) {
                    var lb = org.overengineer.bettacraft.genetics.livebearer.LivebearerGenome.load(e.getCompoundOrEmpty("lb"), e.getBooleanOr("endler", false));
                    line.append(" ").append(org.overengineer.bettacraft.genetics.livebearer.LivebearerText.name(lb.express("female".equals(e.getStringOr("sex", ""))))
                            .withStyle(ChatFormatting.GOLD));
                }
                if (e.contains("stage") && !LifeStage.ADULT.id().equals(e.getStringOr("stage", ""))) {
                    line.append(" (").append(Component.translatable(LifeStage.byId(e.getStringOr("stage", "")).translationKey())).append(")");
                }
                line.append(" ").append(pct(e.getFloatOr("health", 0f)));
                if (e.getBooleanOr("sick", false)) line.append(" ").append(Component.translatable("gui.bettacraft.fish.sick").withStyle(ChatFormatting.RED));
                out.add(line);
            }
        } else if (d.getIntOr("count", 0) > 0) {
            out.add(Component.translatable("jade.bettacraft.sneak").withStyle(ChatFormatting.DARK_GRAY));
        }
        return out;
    }

    /** External filter block and cabinet (M4). */
    public static List<Component> filter(CompoundTag d) {
        List<Component> out = new ArrayList<>();
        if (d == null || d.isEmpty()) return out;
        if (d.contains("storage")) {
            out.add(d.getBooleanOr("has_filter", false) ? Component.translatable("jade.bettacraft.cabinet.filter", d.getIntOr("heaters", 0))
                    : Component.translatable("jade.bettacraft.cabinet.no_filter").withStyle(ChatFormatting.GRAY));
            out.add(Component.translatable("jade.bettacraft.cabinet.storage", d.getIntOr("storage", 0)));
            int[] s = d.getIntArray("sump").orElse(new int[0]); // M49
            if (s.length == 5) {
                out.add(Component.translatable("jade.bettacraft.cabinet.sump", s[0], s[1], s[2]).withStyle(ChatFormatting.AQUA));
                if (s[3] > 0) out.add(Component.translatable(s[4] > 0 ? "jade.bettacraft.cabinet.refugium" : "jade.bettacraft.cabinet.refugium_dark", s[3])
                        .withStyle(s[4] > 0 ? ChatFormatting.GREEN : ChatFormatting.GOLD));
            }
        } else {
            out.add(Component.translatable("jade.bettacraft.filter.heaters", d.getIntOr("heaters", 0), d.getIntOr("heat", 0)));
        }
        if (d.getIntOr("coolers", 0) > 0) out.add(Component.translatable("jade.bettacraft.filter.coolers", d.getIntOr("coolers", 0), d.getIntOr("cool", 0)));
        out.addAll(spring(d)); // M37
        if (d.contains("drive")) { // M41: the Create shaft of a cabinet
            Component side = Component.translatable("gui.bettacraft.cabinet.drive." + d.getStringOr("drive", ""));
            int fe = d.getIntOr("drive_fe", 0);
            out.add(d.getBooleanOr("drive_overstressed", false) ? Component.translatable("jade.bettacraft.cabinet.drive_overstressed", side).withStyle(ChatFormatting.RED)
                    : fe < 0 ? Component.translatable("jade.bettacraft.cabinet.drive_none", side).withStyle(ChatFormatting.GRAY)
                    : Component.translatable("jade.bettacraft.cabinet.drive", side, fe).withStyle(ChatFormatting.AQUA));
        }
        if (!d.getBooleanOr("has_filter", false)) return out;
        if (d.getBooleanOr("attached", false)) {
            String name = d.getStringOr("tank", "");
            out.add(name.isEmpty() ? Component.translatable("jade.bettacraft.filter.tank_unnamed").withStyle(ChatFormatting.AQUA)
                    : Component.translatable("jade.bettacraft.filter.tank", name).withStyle(ChatFormatting.AQUA));
            if (d.getIntOr("below", 0) > 0) out.add(Component.translatable("jade.bettacraft.filter.below", d.getIntOr("below", 0)).withStyle(ChatFormatting.GRAY)); // M62
            if (d.getBooleanOr("unpowered", false)) out.add(Component.translatable("jade.bettacraft.no_power").withStyle(ChatFormatting.RED));
        } else {
            out.add(Component.translatable("jade.bettacraft.filter.no_tank").withStyle(ChatFormatting.GOLD));
            if (d.getBooleanOr("filter_block", false)) out.add(Component.translatable("jade.bettacraft.filter.link_hint").withStyle(ChatFormatting.GRAY)); // M62
        }
        return out;
    }

    /** M37: "Spring: 40 % (9.6 h)" of a device with a spring (nothing while it may not be wound). */
    public static List<Component> spring(CompoundTag d) {
        List<Component> out = new ArrayList<>();
        if (d == null || !d.contains("spring")) return out;
        int pct = d.getIntOr("spring", 0);
        out.add(Component.translatable("jade.bettacraft.spring", pct, d.getStringOr("spring_hours", ""))
                .withStyle(pct <= 0 ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
        if (d.getIntOr("spring_shared", 0) > 1) out.add(Component.translatable("jade.bettacraft.spring.shared", d.getIntOr("spring_shared", 0)).withStyle(ChatFormatting.GRAY)); // M62
        return out;
    }

    /** Larva tub and artemia hatchery (M9). */
    public static List<Component> culture(CompoundTag d) {
        List<Component> out = new ArrayList<>();
        if (d == null || d.isEmpty()) return out;
        boolean artemia = d.getBooleanOr("artemia", false);
        out.add(Component.translatable("jade.bettacraft.culture.water", d.getIntOr("water", 0),
                org.overengineer.bettacraft.food.CultureTubBlockEntity.WATER_CAPACITY));
        out.add(Component.translatable(artemia ? "jade.bettacraft.culture.cysts" : "jade.bettacraft.culture.organic", d.getIntOr("uses", 0)));
        if (artemia) out.add(Component.translatable("jade.bettacraft.culture.bottles", d.getIntOr("bottles", 0)));
        out.add(Component.translatable("jade.bettacraft.culture.progress", Math.round(d.getFloatOr("progress", 0f) * 100)));
        if (d.getIntOr("ready", 0) > 0) {
            out.add(Component.translatable(artemia ? "jade.bettacraft.culture.ready_nauplii" : "jade.bettacraft.culture.ready_larvae", d.getIntOr("ready", 0))
                    .withStyle(ChatFormatting.GREEN));
        }
        if (d.contains("blocked")) {
            out.add(Component.translatable("message.bettacraft.culture.blocked." + d.getStringOr("blocked", "")).withStyle(ChatFormatting.GOLD));
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
            stage = Component.translatable("gui.bettacraft.fish.stage_progress", stage, Math.round(d.getFloatOr("progress", 0f) * 100));
        }
        out.add(Component.translatable("jade.bettacraft.betta.type", BettaTooltip.type(b.genome())).withStyle(ChatFormatting.GOLD));
        out.add(Component.translatable("gui.bettacraft.fish.sex_stage", BettaTooltip.sex(b.isFemale()), stage));
        if (d.isEmpty()) return out;
        if (d.getBooleanOr("in_tank", false)) {
            MutableComponent h = Component.translatable("jade.bettacraft.betta.health", pct(d.getFloatOr("health", 0f)), pct(d.getFloatOr("satiation", 0f)));
            if (d.getBooleanOr("sick", false)) h.append(" ").append(Component.translatable("gui.bettacraft.fish.sick").withStyle(ChatFormatting.RED));
            else if (d.getBooleanOr("hungry", false)) h.append(" ").append(Component.translatable("jade.bettacraft.betta.hungry").withStyle(ChatFormatting.GOLD));
            out.add(h);
        }
        // M11: mirror training (mirror stick)
        if (d.getBooleanOr("trained", false)) out.add(Component.translatable("jade.bettacraft.betta.trained").withStyle(ChatFormatting.AQUA));
        if (d.getBooleanOr("genes_unknown", false) && details) out.add(Component.translatable("tooltip.bettacraft.genes.unknown").withStyle(ChatFormatting.GRAY)); // M22.3
        if (NbtCompat.has(d, "genotype", Tag.TAG_COMPOUND)) {
            List<Component> g = GenotypeText.summary(BettaGenotype.load(d.getCompoundOrEmpty("genotype")));
            if (details) {
                out.addAll(g);
                out.add(Component.translatable("gui.bettacraft.fish.generation", d.getIntOr("generation", 0)).withStyle(ChatFormatting.GRAY));
            } else if (!g.isEmpty()) {
                out.add(g.getFirst());
            }
        }
        return out;
    }
}
