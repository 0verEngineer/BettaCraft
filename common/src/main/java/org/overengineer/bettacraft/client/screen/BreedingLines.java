package org.overengineer.bettacraft.client.screen;

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
            // M62: which shoals are too small and how many fish they need
            if (w.equals("tm_shoal")) {
                for (TankView.Shoal s : v.shoals) {
                    out.add(Component.literal("    ").append(Component.translatable("gui.bettacraft.shoal.line", s.name(), s.count(), s.need()))
                            .withStyle(ChatFormatting.GOLD));
                }
            }
        }
        // M19: a tank without bettas but with tankmates shows their breeding only
        int[] bs = b.getIntArray("stages").orElse(new int[0]);
        int bettas = 0;
        for (int x : bs) bettas += x;
        if (bettas == 0 && !NbtCompat.has(b, "nest", Tag.TAG_COMPOUND) && !b.getListOrEmpty("tankmates").isEmpty()) {
            tankmates(b, out);
            return out;
        }
        // nest
        if (NbtCompat.has(b, "nest", Tag.TAG_COMPOUND)) {
            CompoundTag n = b.getCompoundOrEmpty("nest");
            int pct = Math.round(n.getFloatOr("progress", 0f) * 100);
            switch (n.getStringOr("phase", "")) {
                case "building" -> out.add(header("gui.bettacraft.breeding.nest.building", pct));
                case "built" -> out.add(header("gui.bettacraft.breeding.nest.built"));
                default -> {
                    LifeStage stage = LifeStage.byId(n.getStringOr("stage", ""));
                    out.add(header(stage == LifeStage.LARVA ? "gui.bettacraft.breeding.nest.larva" : "gui.bettacraft.breeding.nest.egg",
                            n.getIntOr("eggs", 0)));
                    out.add(Component.translatable("gui.bettacraft.breeding.nest.free_in", f1(n.getFloatOr("days_left", 0f)),
                            Math.round(n.getFloatOr("care", 0f) * 100)).withStyle(ChatFormatting.GRAY));
                }
            }
        } else {
            out.add(header("gui.bettacraft.breeding.nest.none"));
            CompoundTag c = b.getCompoundOrEmpty("conditions");
            out.add(check(c.getBooleanOr("male", false) && c.getBooleanOr("female", false), Component.translatable("gui.bettacraft.breeding.cond.pair")));
            out.add(check(c.getBooleanOr("temp", false), Component.translatable("gui.bettacraft.breeding.cond.temp", f1(b.getFloatOr("min_temp", 0f)),
                    f1(b.getFloatOr("max_temp", 0f)), f1(b.getFloatOr("temp", 0f)))));
            out.add(check(c.getBooleanOr("male_fit", false), Component.translatable("gui.bettacraft.breeding.cond.male_fit")));
            out.add(check(c.getBooleanOr("female_fit", false) && c.getFloatOr("cooldown", 0f) <= 0,
                    c.getFloatOr("cooldown", 0f) > 0 ? Component.translatable("gui.bettacraft.breeding.cond.cooldown", f1(c.getFloatOr("cooldown", 0f)))
                            : Component.translatable("gui.bettacraft.breeding.cond.female_fit")));
            out.add(Component.translatable(c.getBooleanOr("live_food", false) ? "gui.bettacraft.breeding.cond.live_yes" : "gui.bettacraft.breeding.cond.live_no")
                    .withStyle(ChatFormatting.GRAY));
            if (c.getBooleanOr("mirror", false)) {
                out.add(Component.translatable("gui.bettacraft.breeding.cond.mirror").withStyle(ChatFormatting.GRAY));
            }
            if (c.getBooleanOr("aquatic", false)) {
                out.add(Component.translatable("gui.bettacraft.breeding.cond.aquatic").withStyle(ChatFormatting.GRAY));
            }
            if (c.getBooleanOr("plants", false)) {
                out.add(Component.translatable("gui.bettacraft.breeding.cond.plants").withStyle(ChatFormatting.GRAY));
            }
            if (c.getFloatOr("ready", 0f) > 0) {
                out.add(Component.translatable("gui.bettacraft.breeding.cond.ready", Math.round(c.getFloatOr("ready", 0f) * 100))
                        .withStyle(ChatFormatting.AQUA));
            }
        }
        // young fish
        int[] st = b.getIntArray("stages").orElse(new int[0]);
        if (st.length == LifeStage.values().length) {
            int fry = st[LifeStage.FRY.ordinal()], juv = st[LifeStage.JUVENILE.ordinal()], sub = st[LifeStage.SUBADULT.ordinal()];
            out.add(Component.translatable("gui.bettacraft.breeding.young", fry, juv, sub)
                    .withStyle(fry + juv + sub > 0 ? ChatFormatting.WHITE : ChatFormatting.GRAY));
            if (fry > 0) out.add(Component.translatable("gui.bettacraft.breeding.fry_food").withStyle(ChatFormatting.GRAY));
        }
        // parents + prediction
        if (b.getBooleanOr("prediction_hidden", false)) {
            // M22.3: the parents' genes are not analysed yet
            out.add(header("gui.bettacraft.breeding.parents", TankCare.argComponent(b.getStringOr("father", "")),
                    TankCare.argComponent(b.getStringOr("mother", ""))));
            out.add(Component.translatable("gui.bettacraft.breeding.prediction_hidden").withStyle(ChatFormatting.GRAY));
        } else if (NbtCompat.has(b, "prediction", Tag.TAG_LIST)) {
            out.add(header("gui.bettacraft.breeding.parents", TankCare.argComponent(b.getStringOr("father", "")),
                    TankCare.argComponent(b.getStringOr("mother", ""))));
            BettaGenotype father = BettaGenotype.load(b.getCompoundOrEmpty("father_genome"));
            BettaGenotype mother = BettaGenotype.load(b.getCompoundOrEmpty("mother_genome"));
            out.add(header("gui.bettacraft.breeding.prediction"));
            ListTag pred = b.getListOrEmpty("prediction");
            for (int i = 0; i < pred.size(); i++) {
                CompoundTag l = pred.getCompoundOrEmpty(i);
                Locus locus = BettaLoci.byId(l.getStringOr("locus", ""));
                if (locus == null) continue;
                MutableComponent line = Component.translatable(BettaLoci.translationKey(locus)).append(": ");
                ListTag outs = l.getListOrEmpty("o");
                for (int k = 0; k < Math.min(4, outs.size()); k++) {
                    CompoundTag o = outs.getCompoundOrEmpty(k);
                    if (k > 0) line.append(", ");
                    line.append(GenotypeText.allele(locus, o.getStringOr("a", ""))).append(" " + Math.round(o.getFloatOr("p", 0f) * 100) + "%");
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
        ListTag list = b.getListOrEmpty("tankmates");
        if (b.getBooleanOr("box", false)) {
            out.add(header("gui.bettacraft.breeding.tm.box", b.getIntOr("box_fry", 0), b.getIntOr("box_cap", 0)));
        }
        for (int i = 0; i < list.size(); i++) {
            CompoundTag d = list.getCompoundOrEmpty(i);
            var s = org.overengineer.bettacraft.tankmates.TankmateSpecies.byId(d.getStringOr("species", ""));
            if (s == null) continue;
            out.add(header("gui.bettacraft.breeding.tm.species", Component.translatable(s.translationKey()), Component.translatable(s.breedingKey()),
                    d.getIntOr("males", 0), d.getIntOr("females", 0), d.getIntOr("young", 0)));
            if (s.breeding() == org.overengineer.bettacraft.tankmates.TankmateSpecies.Breeding.NONE) {
                out.add(Component.translatable("gui.bettacraft.breeding.tm.never").withStyle(ChatFormatting.GRAY));
                continue;
            }
            out.add(check(d.getBooleanOr("fit", false), Component.translatable("gui.bettacraft.breeding.tm.pair")));
            boolean cory = s.group() == org.overengineer.bettacraft.tankmates.TankmateSpecies.Group.CORY;
            if (!cory) {
                out.add(check(d.getBooleanOr("temp", false), Component.translatable("gui.bettacraft.breeding.tm.temp", f1(s.breedMin()), f1(s.breedMax()))));
            }
            switch (s.breeding()) {
                case GLASS_EGGS -> {
                    if (cory) out.add(check(d.getBooleanOr("cool", false), Component.translatable("gui.bettacraft.breeding.tm.cool")));
                }
                case CAVE, CICHLID_CAVE -> out.add(check(d.getIntOr("caves", 0) > 0, Component.translatable("gui.bettacraft.breeding.tm.caves", d.getIntOr("caves", 0))));
                case ANEMONE -> out.add(check(d.getIntOr("anemones", 0) > 0, Component.translatable("gui.bettacraft.breeding.tm.anemones", d.getIntOr("anemones", 0))));
                case SCATTER -> out.add(check(d.getIntOr("mops", 0) > 0 || d.getFloatOr("plants", 0f) >= 0.005F, Component.translatable(
                        s.broadLeafSpawner() ? "gui.bettacraft.breeding.tm.broad" : "gui.bettacraft.breeding.tm.medium",
                        d.getIntOr("mops", 0), Math.round(d.getFloatOr("plants", 0f) * 1000) / 10.0F)));
                case POUCH -> {
                    // M21.8 seahorses: hitching posts, full brood pouches
                    out.add(check(d.getIntOr("posts", 0) > 0, Component.translatable("gui.bettacraft.breeding.tm.posts", d.getIntOr("posts", 0))));
                    if (d.getIntOr("gravid", 0) > 0) out.add(Component.translatable("gui.bettacraft.breeding.tm.pouches", d.getIntOr("gravid", 0))
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                }
                case LIVEBEARER, MOUTH -> {
                    if (d.getIntOr("gravid", 0) > 0) out.add(Component.translatable(s.mouthbrooder() ? "gui.bettacraft.breeding.tm.mouth" : "gui.bettacraft.breeding.tm.gravid", d.getIntOr("gravid", 0))
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                    if (s.breeding() == org.overengineer.bettacraft.tankmates.TankmateSpecies.Breeding.LIVEBEARER && !d.getBooleanOr("box", false)) out.add(Component.translatable("gui.bettacraft.breeding.tm.no_box").withStyle(ChatFormatting.GRAY));
                }
                default -> {}
            }
            // M30 part c: a big species breeds only while the size rule leaves room for its young
            if (s.isLarge() && d.contains("room")) out.add(check(d.getIntOr("room", 0) > 0, Component.translatable("gui.bettacraft.breeding.tm.room", d.getIntOr("room", 0))));
            ListTag cl = d.getListOrEmpty("clutches");
            for (int k = 0; k < cl.size(); k++) {
                CompoundTag c = cl.getCompoundOrEmpty(k);
                if (c.getBooleanOr("building", false)) {
                    out.add(Component.literal("  ").append(Component.translatable("gui.bettacraft.breeding.tm.nest_building")).withStyle(ChatFormatting.AQUA));
                    continue;
                }
                if ("box".equals(c.getStringOr("kind", ""))) continue;
                String stage = c.getIntOr("stage", 0) == 0 ? "eggs" : "larvae";
                out.add(Component.literal("  ").append(Component.translatable("gui.bettacraft.breeding.tm.clutch." + stage,
                        Component.translatable("gui.bettacraft.breeding.tm.kind." + (s.breeding() == org.overengineer.bettacraft.tankmates.TankmateSpecies.Breeding.ANEMONE
                                ? "anemone" : c.getStringOr("kind", ""))), c.getIntOr("count", 0), f1(c.getFloatOr("days", 0f))))
                        .withStyle(ChatFormatting.AQUA));
            }
        }
    }
}
