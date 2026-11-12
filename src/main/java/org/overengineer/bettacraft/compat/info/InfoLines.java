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
     * Tank: name, temperature and water, animals, nest, filters and warnings always; the list of animals with health
     * only with {@code details}.
     */
    public static List<Component> tank(CompoundTag d, boolean details) {
        List<Component> out = new ArrayList<>();
        if (d == null || d.isEmpty()) return out;
        if (!d.getString("name").isEmpty()) out.add(Component.literal(d.getString("name")).withStyle(ChatFormatting.WHITE));
        out.add(Component.translatable("jade.bettacraft.tank.water", f1(d.getFloat("temp")), d.getLong("water"), d.getLong("cap")));
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
        ListTag warn = d.getList("warnings", Tag.TAG_STRING);
        for (int i = 0; i < warn.size(); i++) {
            out.add(Component.translatable("gui.bettacraft.breeding.warn." + warn.getString(i)).withStyle(ChatFormatting.RED));
        }
        if (details) {
            ListTag list = d.getList("list", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag e = list.getCompound(i);
                MutableComponent line = Component.literal("  ");
                if (e.contains("sex")) line.append("female".equals(e.getString("sex")) ? "♀ " : "♂ ");
                line.append(TankCare.argComponent(e.getString("name")));
                if (e.contains("stage") && !LifeStage.ADULT.id().equals(e.getString("stage"))) {
                    line.append(" (").append(Component.translatable(LifeStage.byId(e.getString("stage")).translationKey())).append(")");
                }
                line.append(" ").append(pct(e.getFloat("health")));
                if (e.getBoolean("sick")) line.append(" ").append(Component.translatable("gui.bettacraft.fish.sick").withStyle(ChatFormatting.RED));
                out.add(line);
            }
        } else if (d.getInt("count") > 0) {
            out.add(Component.translatable("jade.bettacraft.sneak").withStyle(ChatFormatting.DARK_GRAY));
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
        } else {
            out.add(Component.translatable("jade.bettacraft.filter.heaters", d.getInt("heaters"), d.getInt("heat")));
        }
        if (!d.getBoolean("has_filter")) return out;
        if (d.getBoolean("attached")) {
            String name = d.getString("tank");
            out.add(name.isEmpty() ? Component.translatable("jade.bettacraft.filter.tank_unnamed").withStyle(ChatFormatting.AQUA)
                    : Component.translatable("jade.bettacraft.filter.tank", name).withStyle(ChatFormatting.AQUA));
        } else {
            out.add(Component.translatable("jade.bettacraft.filter.no_tank").withStyle(ChatFormatting.GOLD));
        }
        return out;
    }

    /**
     * Betta: type, sex and stage always (from the entity, also without server data); health and the first genotype
     * line in a tank; all genotype lines and the generation with {@code details}.
     */
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
