package org.overengineer.bettacraft.tank;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.overengineer.bettacraft.critters.AmanoLarvae;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterObject;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.frog.DwarfFrogEntity;
import org.overengineer.bettacraft.entity.frog.FrogBreeding;
import org.overengineer.bettacraft.terrarium.HerpBreeding;
import org.overengineer.bettacraft.terrarium.HerpEntity;
import org.overengineer.bettacraft.terrarium.HerpSpecies;
import org.overengineer.bettacraft.terrarium.TankClimate;

/**
 * M66: what the breeding tab shows for the animals besides bettas and tankmates: the small animals (shrimp, snails, crayfish, the
 * Amano larvae), the dwarf clawed frogs, the terrarium animals and the animals of other mods. Per species an entry with the counts,
 * the conditions the breeding logic ({@code CritterLife}, {@link FrogBreeding}, {@link HerpBreeding}, {@code ForeignBreeding})
 * asks for (each ok or missing) and what is going on now (berried females, clutches, larvae, spawn, tadpoles). The tag
 * {@code {n: name key, c: [males, females, young], cond: [{k, ok, a}], st: [{k, a}]}}; the client writes the texts
 * {@code gui.bettacraft.breeding.c.<k>} and {@code gui.bettacraft.breeding.s.<k>}.
 */
public final class BreedingOverview {
    private BreedingOverview() {}

    /** Adds the lists {@code g_small}, {@code g_frogs}, {@code g_herps}, {@code g_foreign} (only the non-empty ones). */
    public static void describe(ServerLevel level, TankBlockEntity c, List<Entity> live, CompoundTag out) {
        TankData t = c.getTank();
        ListTag small = small(t);
        if (!small.isEmpty()) out.put("g_small", small);
        ListTag frogs = frogs(level, t, live);
        if (!frogs.isEmpty()) out.put("g_frogs", frogs);
        ListTag herps = herps(level, c, live);
        if (!herps.isEmpty()) out.put("g_herps", herps);
        ListTag foreign = org.overengineer.bettacraft.foreign.ForeignBreeding.overview(level, c, live);
        if (!foreign.isEmpty()) out.put("g_foreign", foreign);
    }

    // ------------------------------------------------------------------ entry helpers

    public static CompoundTag entry(String name, int males, int females, int young) {
        CompoundTag e = new CompoundTag();
        e.putString("n", name);
        e.putIntArray("c", new int[]{males, females, young});
        e.put("cond", new ListTag());
        e.put("st", new ListTag());
        return e;
    }

    public static void cond(CompoundTag e, String k, boolean ok, Object... args) {
        e.getList("cond", 10).add(line(k, ok, args));
    }

    public static void state(CompoundTag e, String k, Object... args) {
        e.getList("st", 10).add(line(k, true, args));
    }

    private static CompoundTag line(String k, boolean ok, Object[] args) {
        CompoundTag l = new CompoundTag();
        l.putString("k", k);
        l.putBoolean("ok", ok);
        ListTag a = new ListTag();
        for (Object o : args) a.add(StringTag.valueOf(String.valueOf(o)));
        l.put("a", a);
        return l;
    }

    private static String f1(double d) {
        return String.format(java.util.Locale.ROOT, "%.1f", d);
    }

    // ------------------------------------------------------------------ small animals

    private static ListTag small(TankData t) {
        ListTag out = new ListTag();
        int cap = TankCritters.maxPerTank(), alive = TankCritters.alive(t);
        boolean air = t.waterMb() < t.capacityMb();
        for (CritterSpecies s : CritterSpecies.values()) {
            int males = 0, females = 0, young = 0, berried = 0;
            for (Critter k : t.critters()) {
                if (k.species != s || !k.alive()) continue;
                if (!k.adult()) young++;
                else if (k.female) females++;
                else males++;
                if (k.berried()) berried++;
            }
            int clutches = 0, larvae = 0, larvaeCount = 0;
            for (CritterObject o : t.critterObjects()) {
                if (o.species != s) continue;
                if (o.kind == CritterObject.Kind.CLUTCH) clutches++;
                else if (o.kind == CritterObject.Kind.LARVAE) {
                    larvae++;
                    larvaeCount += o.count;
                }
            }
            if (males + females + young + clutches + larvae == 0 || !s.breeds()) continue;
            CompoundTag e = entry(s.translationKey(), males, females, young);
            boolean one = s.breeding() == CritterSpecies.Breeding.SELF || s.breeding() == CritterSpecies.Breeding.PARTHENO;
            cond(e, one ? "pair_one" : "pair", one ? males + females > 0 : males > 0 && females > 0);
            cond(e, "temp", t.waterTemp() >= s.minTemp() + 1 && t.waterTemp() <= s.maxTemp() - 1, f1(s.minTemp() + 1), f1(s.maxTemp() - 1), f1(t.waterTemp()));
            if (TankWater.qualityEnabled()) {
                cond(e, "water", t.waterQuality() >= s.badWater() + 0.1F, Math.round((s.badWater() + 0.1F) * 100));
            }
            cond(e, "room", alive < cap, alive, cap);
            if (s.breeding() == CritterSpecies.Breeding.PAIR_CLUTCH) cond(e, "air", air);
            if (AmanoLarvae.hasLarvae(s)) cond(e, "fresh", AmanoLarvae.matesIn(t.salinity()));
            if (berried > 0) state(e, "berried", berried);
            if (clutches > 0) state(e, "clutches", clutches);
            if (larvae > 0) {
                state(e, "larvae", larvaeCount, larvae);
                cond(e, "brackish", AmanoLarvae.grows(t.salinity()));
            }
            out.add(e);
        }
        return out;
    }

    // ------------------------------------------------------------------ dwarf clawed frogs

    private static ListTag frogs(ServerLevel level, TankData t, List<Entity> live) {
        ListTag out = new ListTag();
        int males = 0, females = 0, young = 0, readyM = 0, readyF = 0, clasped = 0;
        String name = null;
        for (Entity en : live) {
            if (!(en instanceof DwarfFrogEntity f)) continue;
            name = f.getType().getDescriptionId();
            if (!f.isAdult()) {
                young++;
                continue;
            }
            if (f.isFemale()) females++;
            else males++;
            if (FrogBreeding.ready(f)) {
                if (f.isFemale()) readyF++;
                else readyM++;
            }
            if (f.inAmplexus()) clasped++;
        }
        int eggs = FrogBreeding.eggs(t), tadpoles = t.tadpoles().size();
        if (name == null && eggs + tadpoles == 0) return out;
        CompoundTag e = entry(name == null ? "entity.bettacraft.dwarf_frog" : name, males, females, young);
        cond(e, "pair", males > 0 && females > 0);
        cond(e, "ready", readyM > 0 && readyF > 0);
        cond(e, "temp", FrogBreeding.breedingTemp(t), f1(FrogBreeding.MIN_TEMP), f1(FrogBreeding.MAX_TEMP), f1(t.waterTemp()));
        cond(e, "frog_air", TankFrogs.hasAir(t));
        cond(e, "cool", org.overengineer.bettacraft.tankmates.TankmateBreeding.coolChangeRecent(level, t));
        cond(e, "tadpole_room", tadpoles + eggs < FrogBreeding.maxTadpoles(), tadpoles + eggs, FrogBreeding.maxTadpoles());
        if (!FrogBreeding.enabled()) cond(e, "off", false);
        if (clasped > 0) state(e, "amplexus", clasped);
        if (eggs > 0) state(e, "spawn", eggs, t.frogSpawn().size());
        if (tadpoles > 0) state(e, "tadpoles", tadpoles);
        out.add(e);
        return out;
    }

    // ------------------------------------------------------------------ terrarium animals

    private static ListTag herps(ServerLevel level, TankBlockEntity c, List<Entity> live) {
        TankData t = c.getTank();
        ListTag out = new ListTag();
        long now = level.getGameTime();
        var cond = TankClimate.conditions(t);
        for (HerpSpecies s : HerpSpecies.ALL) {
            int males = 0, females = 0, young = 0;
            HerpEntity mother = null;
            boolean male = false;
            for (Entity en : live) {
                if (!(en instanceof HerpEntity h) || h.species() != s) continue;
                if (h.isBaby()) young++;
                else if (h.isFemale()) {
                    females++;
                    if (mother == null || h.layReady(now)) mother = h;
                } else {
                    males++;
                    if (!h.isBrumating()) male = true;
                }
            }
            int clutches = 0, eggs = 0;
            double progress = 0;
            for (var k : t.herpClutches()) {
                if (k.species != s) continue;
                clutches++;
                eggs += k.eggs;
                progress = Math.max(progress, k.progress);
            }
            if (males + females + young + clutches == 0) continue;
            CompoundTag e = entry(s.translationKey(), males, females, young);
            if (!s.breeds()) {
                cond(e, "never", false);
            } else if (!HerpBreeding.breedingOn(s)) {
                cond(e, "off", false);
            } else {
                cond(e, "pair", males > 0 && females > 0 && male);
                if (mother != null) {
                    var a = mother.adjust(mother.terrariumNeeds().assess(cond));
                    cond(e, "climate", !a.harmful() && a.stress() <= HerpBreeding.MAX_STRESS);
                    cond(e, "fed", mother.fedWithin(2.0));
                    cond(e, "rest", mother.layReady(now) || clutches > 0);
                }
                cond(e, "nest", HerpBreeding.anyNest(level, t, HerpBreeding.nestGround(s)));
                cond(e, "herp_room", HerpBreeding.room(live, t, s, s.eggsMax()));
            }
            if (clutches > 0) state(e, "clutch", eggs, clutches, Math.round(progress * 100), f1(HerpBreeding.nestTemp(t)));
            out.add(e);
        }
        return out;
    }
}
