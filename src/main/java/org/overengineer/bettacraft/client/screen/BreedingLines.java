package org.overengineer.bettacraft.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.genetics.BettaLoci;
import org.overengineer.bettacraft.genetics.GenotypeText;
import org.overengineer.bettacraft.genetics.Locus;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.menu.TankView;

/** Text lines of the breeding tab (PLAN 8): warnings, nest, young fish, parents and the Punnett prediction. */
final class BreedingLines {
    private BreedingLines() {}

    private static String f1(double d) {
        return String.format(Locale.ROOT, "%.1f", d);
    }

    private static MutableComponent check(boolean ok, Component text) {
        return Component.literal(ok ? "✔ " : "✘ ").withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED).append(text.copy()
                .withStyle(ok ? ChatFormatting.GRAY : ChatFormatting.WHITE));
    }

    private static MutableComponent header(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.YELLOW);
    }

    static List<Component> build(TankView v) {
        CompoundTag b = v.breeding;
        List<Component> out = new ArrayList<>();
        for (String w : v.warnings) {
            out.add(Component.literal("⚠ ").append(Component.translatable("gui.bettacraft.breeding.warn." + w))
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }
        // M19: a tank without bettas but with tankmates shows their breeding only
        int[] bs = b.getIntArray("stages");
        int bettas = 0;
        for (int x : bs) bettas += x;
        if (bettas == 0 && !b.contains("nest", Tag.TAG_COMPOUND) && !b.getList("tankmates", Tag.TAG_COMPOUND).isEmpty()) {
            tankmates(b, out);
            return out;
        }
        // nest
        if (b.contains("nest", Tag.TAG_COMPOUND)) {
            CompoundTag n = b.getCompound("nest");
            int pct = Math.round(n.getFloat("progress") * 100);
            switch (n.getString("phase")) {
                case "building" -> out.add(header("gui.bettacraft.breeding.nest.building", pct));
                case "built" -> out.add(header("gui.bettacraft.breeding.nest.built"));
                default -> {
                    LifeStage stage = LifeStage.byId(n.getString("stage"));
                    out.add(header(stage == LifeStage.LARVA ? "gui.bettacraft.breeding.nest.larva" : "gui.bettacraft.breeding.nest.egg",
                            n.getInt("eggs")));
                    out.add(Component.translatable("gui.bettacraft.breeding.nest.free_in", f1(n.getFloat("days_left")),
                            Math.round(n.getFloat("care") * 100)).withStyle(ChatFormatting.GRAY));
                }
            }
        } else {
            out.add(header("gui.bettacraft.breeding.nest.none"));
            CompoundTag c = b.getCompound("conditions");
            out.add(check(c.getBoolean("male") && c.getBoolean("female"), Component.translatable("gui.bettacraft.breeding.cond.pair")));
            out.add(check(c.getBoolean("temp"), Component.translatable("gui.bettacraft.breeding.cond.temp", f1(b.getFloat("min_temp")),
                    f1(b.getFloat("max_temp")), f1(b.getFloat("temp")))));
            out.add(check(c.getBoolean("male_fit"), Component.translatable("gui.bettacraft.breeding.cond.male_fit")));
            out.add(check(c.getBoolean("female_fit") && c.getFloat("cooldown") <= 0,
                    c.getFloat("cooldown") > 0 ? Component.translatable("gui.bettacraft.breeding.cond.cooldown", f1(c.getFloat("cooldown")))
                            : Component.translatable("gui.bettacraft.breeding.cond.female_fit")));
            out.add(Component.translatable(c.getBoolean("live_food") ? "gui.bettacraft.breeding.cond.live_yes" : "gui.bettacraft.breeding.cond.live_no")
                    .withStyle(ChatFormatting.GRAY));
            if (c.getBoolean("mirror")) {
                out.add(Component.translatable("gui.bettacraft.breeding.cond.mirror").withStyle(ChatFormatting.GRAY));
            }
            if (c.getBoolean("aquatic")) {
                out.add(Component.translatable("gui.bettacraft.breeding.cond.aquatic").withStyle(ChatFormatting.GRAY));
            }
            if (c.getBoolean("plants")) {
                out.add(Component.translatable("gui.bettacraft.breeding.cond.plants").withStyle(ChatFormatting.GRAY));
            }
            if (c.getFloat("ready") > 0) {
                out.add(Component.translatable("gui.bettacraft.breeding.cond.ready", Math.round(c.getFloat("ready") * 100))
                        .withStyle(ChatFormatting.AQUA));
            }
        }
        // young fish
        int[] st = b.getIntArray("stages");
        if (st.length == LifeStage.values().length) {
            int fry = st[LifeStage.FRY.ordinal()], juv = st[LifeStage.JUVENILE.ordinal()], sub = st[LifeStage.SUBADULT.ordinal()];
            out.add(Component.translatable("gui.bettacraft.breeding.young", fry, juv, sub)
                    .withStyle(fry + juv + sub > 0 ? ChatFormatting.WHITE : ChatFormatting.GRAY));
            if (fry > 0) out.add(Component.translatable("gui.bettacraft.breeding.fry_food").withStyle(ChatFormatting.GRAY));
        }
        // parents + prediction
        if (b.contains("prediction", Tag.TAG_LIST)) {
            out.add(header("gui.bettacraft.breeding.parents", TankCare.argComponent(b.getString("father")),
                    TankCare.argComponent(b.getString("mother"))));
            BettaGenotype father = BettaGenotype.load(b.getCompound("father_genome"));
            BettaGenotype mother = BettaGenotype.load(b.getCompound("mother_genome"));
            out.add(header("gui.bettacraft.breeding.prediction"));
            ListTag pred = b.getList("prediction", Tag.TAG_COMPOUND);
            for (int i = 0; i < pred.size(); i++) {
                CompoundTag l = pred.getCompound(i);
                Locus locus = BettaLoci.byId(l.getString("locus"));
                if (locus == null) continue;
                MutableComponent line = Component.translatable(BettaLoci.translationKey(locus)).append(": ");
                ListTag outs = l.getList("o", Tag.TAG_COMPOUND);
                for (int k = 0; k < Math.min(4, outs.size()); k++) {
                    CompoundTag o = outs.getCompound(k);
                    if (k > 0) line.append(", ");
                    line.append(GenotypeText.allele(locus, o.getString("a"))).append(" " + Math.round(o.getFloat("p") * 100) + "%");
                }
                if (outs.size() > 4) line.append(", ...");
                out.add(line);
                out.add(Component.literal("   ").append(Component.translatable("gui.bettacraft.breeding.cross",
                        GenotypeText.pair(father, locus), GenotypeText.pair(mother, locus))).withStyle(ChatFormatting.GRAY));
            }
            out.add(Component.translatable("gui.bettacraft.breeding.sex_ratio").withStyle(ChatFormatting.GRAY));
        } else {
            out.add(Component.translatable("gui.bettacraft.breeding.no_pair").withStyle(ChatFormatting.GRAY));
        }
        tankmates(b, out);
        return out;
    }

    /** M19: the tankmates' breeding, species by species, and the breeding box. */
    static void tankmates(CompoundTag b, List<Component> out) {
        ListTag list = b.getList("tankmates", Tag.TAG_COMPOUND);
        if (b.getBoolean("box")) {
            out.add(header("gui.bettacraft.breeding.tm.box", b.getInt("box_fry"), b.getInt("box_cap")));
        }
        for (int i = 0; i < list.size(); i++) {
            CompoundTag d = list.getCompound(i);
            var s = org.overengineer.bettacraft.tankmates.TankmateSpecies.byId(d.getString("species"));
            if (s == null) continue;
            out.add(header("gui.bettacraft.breeding.tm.species", Component.translatable(s.translationKey()), Component.translatable(s.breedingKey()),
                    d.getInt("males"), d.getInt("females"), d.getInt("young")));
            if (s.breeding() == org.overengineer.bettacraft.tankmates.TankmateSpecies.Breeding.NONE) {
                out.add(Component.translatable("gui.bettacraft.breeding.tm.never").withStyle(ChatFormatting.GRAY));
                continue;
            }
            out.add(check(d.getBoolean("fit"), Component.translatable("gui.bettacraft.breeding.tm.pair")));
            boolean cory = s.group() == org.overengineer.bettacraft.tankmates.TankmateSpecies.Group.CORY;
            if (!cory) {
                out.add(check(d.getBoolean("temp"), Component.translatable("gui.bettacraft.breeding.tm.temp", f1(s.breedMin()), f1(s.breedMax()))));
            }
            switch (s.breeding()) {
                case GLASS_EGGS -> {
                    if (cory) out.add(check(d.getBoolean("cool"), Component.translatable("gui.bettacraft.breeding.tm.cool")));
                }
                case CAVE -> out.add(check(d.getInt("caves") > 0, Component.translatable("gui.bettacraft.breeding.tm.caves", d.getInt("caves"))));
                case ANEMONE -> out.add(check(d.getInt("anemones") > 0, Component.translatable("gui.bettacraft.breeding.tm.anemones", d.getInt("anemones"))));
                case SCATTER -> out.add(check(d.getInt("mops") > 0 || d.getFloat("plants") >= 0.005F, Component.translatable(
                        s.broadLeafSpawner() ? "gui.bettacraft.breeding.tm.broad" : "gui.bettacraft.breeding.tm.medium",
                        d.getInt("mops"), Math.round(d.getFloat("plants") * 1000) / 10.0F)));
                case LIVEBEARER -> {
                    if (d.getInt("gravid") > 0) out.add(Component.translatable("gui.bettacraft.breeding.tm.gravid", d.getInt("gravid"))
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                    if (!d.getBoolean("box")) out.add(Component.translatable("gui.bettacraft.breeding.tm.no_box").withStyle(ChatFormatting.GRAY));
                }
                default -> {}
            }
            ListTag cl = d.getList("clutches", Tag.TAG_COMPOUND);
            for (int k = 0; k < cl.size(); k++) {
                CompoundTag c = cl.getCompound(k);
                if (c.getBoolean("building")) {
                    out.add(Component.literal("  ").append(Component.translatable("gui.bettacraft.breeding.tm.nest_building")).withStyle(ChatFormatting.AQUA));
                    continue;
                }
                if ("box".equals(c.getString("kind"))) continue;
                String stage = c.getInt("stage") == 0 ? "eggs" : "larvae";
                out.add(Component.literal("  ").append(Component.translatable("gui.bettacraft.breeding.tm.clutch." + stage,
                        Component.translatable("gui.bettacraft.breeding.tm.kind." + c.getString("kind")), c.getInt("count"), f1(c.getFloat("days"))))
                        .withStyle(ChatFormatting.AQUA));
            }
        }
    }
}
