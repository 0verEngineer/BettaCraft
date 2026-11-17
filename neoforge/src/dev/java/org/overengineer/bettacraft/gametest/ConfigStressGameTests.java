package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import org.overengineer.bettacraft.util.NbtCompat;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.Preset;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M42 part b: the server config at its limits and the presets and care modes switched while the care runs. A stocked tank (a
 * betta pair, tetras and corys, shrimp and snails) lives through passes in which every number of the server config is at the
 * lowest, then the highest value its range allows, every switch the other way round, every choice at each of its values, and
 * the presets and care modes one after the other: each pass a day in care steps of different lengths and two seconds of real
 * ticks. Nothing may throw, no number of the tank or its animals may become NaN or infinite, the water stays within the tank,
 * and back on the old values the config is what it was. Own batches (they change the config; restored even on failure).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ConfigStressGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private ConfigStressGameTests() {}

    /** Every config value (static fields) of a config class. */
    static List<ModConfigSpec.ConfigValue<?>> values(Class<?> cls) {
        List<ModConfigSpec.ConfigValue<?>> out = new ArrayList<>();
        for (Field f : cls.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) || !ModConfigSpec.ConfigValue.class.isAssignableFrom(f.getType())) continue;
            try {
                f.setAccessible(true);
                Object v = f.get(null);
                if (v instanceof ModConfigSpec.ConfigValue<?> cv) out.add(cv);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        return out;
    }

    /** Sets every value {@code pick} gives (null: unchanged); returns the old values to restore. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static Map<ModConfigSpec.ConfigValue<?>, Object> set(List<ModConfigSpec.ConfigValue<?>> all, Function<ModConfigSpec.ConfigValue<?>, Object> pick) {
        Map<ModConfigSpec.ConfigValue<?>, Object> old = new LinkedHashMap<>();
        for (ModConfigSpec.ConfigValue<?> cv : all) {
            Object v = pick.apply(cv);
            if (v == null) continue;
            old.put(cv, cv.get());
            ((ModConfigSpec.ConfigValue) cv).set(v);
        }
        return old;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void restore(Map<ModConfigSpec.ConfigValue<?>, Object> old) {
        for (var e : old.entrySet()) ((ModConfigSpec.ConfigValue) e.getKey()).set(e.getValue());
    }

    /** The lowest ({@code max} false) or highest value of a number's range, null for values without a range. */
    static Object limit(ModConfigSpec.ConfigValue<?> cv, boolean max) {
        if (!(cv instanceof ModConfigSpec.IntValue || cv instanceof ModConfigSpec.LongValue || cv instanceof ModConfigSpec.DoubleValue)) return null;
        ModConfigSpec.Range<?> r = cv.getSpec().getRange();
        if (r == null) return null;
        return max ? r.getMax() : r.getMin();
    }

    // ------------------------------------------------------------------ the stocked tank

    static TankBlockEntity stocked(GameTestHelper h, int x, int z) {
        TankBlockEntity c = ForeignGameTests.tank(h, x, z, 5, 3, 4, 26F);
        LiveFoodGameTests.betta(h, c, false, 42_1L);
        LiveFoodGameTests.betta(h, c, true, 42_2L);
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        var b = w.box();
        for (int i = 0; i < 6; i++) {
            TankmateSpecies s = i < 4 ? TankmateSpecies.NEON_TETRA : TankmateSpecies.PANDA_CORY;
            Vec3 p = w.clamp(new Vec3(b.minX + 0.6 + i * 0.6, b.minY + 0.5, (b.minZ + b.maxZ) / 2), 0.35, 0.1, 0.2);
            var m = TankmateBreeding.spawn(h.getLevel(), c, s, TankmateGenes.wild(s), i % 2 == 0, LifeStage.ADULT, 0, p);
            if (m == null) throw new GameTestFailure("could not spawn " + s);
        }
        for (int i = 0; i < 6; i++) TankCritters.add(h.getLevel(), c, i < 4 ? CritterSpecies.NEOCARIDINA : CritterSpecies.ZEBRA_NERITE, CritterGenes.WILD, i % 2 == 0, 10);
        return c;
    }

    /** The paths of every NaN or infinite number in the tag. */
    static void nonFinite(Tag t, String path, List<String> out) {
        if (t instanceof FloatTag f && !Float.isFinite(f.floatValue())) out.add(path + "=" + f.floatValue());
        else if (t instanceof DoubleTag d && !Double.isFinite(d.doubleValue())) out.add(path + "=" + d.doubleValue());
        else if (t instanceof CompoundTag c) for (String k : c.keySet()) nonFinite(c.get(k), path + "." + k, out);
        else if (t instanceof CollectionTag l) for (int i = 0; i < l.size(); i++) nonFinite(l.get(i), path + "[" + i + "]", out);
    }

    /** A day of care in steps of different lengths; then the tank and its animals checked. Problems go to {@code out}. */
    static void careAndCheck(GameTestHelper h, List<TankBlockEntity> tanks, String pass, List<String> out) {
        ServerLevel level = h.getLevel();
        for (TankBlockEntity c : tanks) {
            try {
                for (long dt : new long[]{20, 20, 100, 1200, 6000, 16660}) TankCare.care(level, c, dt);
                TankInhabitants.reconcile(level, c);
            } catch (Throwable t) {
                out.add(pass + ": care threw " + t);
                org.slf4j.LoggerFactory.getLogger("bettacraft/gametest").error("[m42] {} care", pass, t);
            }
            check(h, c, pass, out);
        }
    }

    static void check(GameTestHelper h, TankBlockEntity c, String pass, List<String> out) {
        TankData t = c.getTank();
        List<String> bad = new ArrayList<>();
        nonFinite(t.save(), "tank", bad);
        for (Entity e : TankInhabitants.liveInhabitants(h.getLevel(), t)) {
            CompoundTag tag = new CompoundTag();
            tag.merge(NbtCompat.saveEntityWithoutId(e));
            nonFinite(tag, e.getType().toShortString(), bad);
            if (e instanceof LivingEntity le && !Float.isFinite(le.getHealth())) bad.add(e.getType().toShortString() + " health " + le.getHealth());
        }
        if (t.waterMb() < 0 || t.waterMb() > t.capacityMb()) bad.add("water " + t.waterMb() + " of " + t.capacityMb());
        if (!bad.isEmpty()) out.add(pass + ": " + bad.subList(0, Math.min(5, bad.size())));
    }

    // ------------------------------------------------------------------ the tests

    /** Lowest numbers, highest numbers, every switch flipped, every choice at each of its values. */
    @GameTest(template = EMPTY, batch = "bettacraft_m42_config_extremes", timeoutTicks = 2400)
    public static void serverConfigAtItsLimits(GameTestHelper h) {
        List<ModConfigSpec.ConfigValue<?>> all = values(ServerConfig.class);
        List<TankBlockEntity> tanks = List.of(stocked(h, 2, 2), stocked(h, 10, 2));
        List<String> problems = new ArrayList<>();
        List<Function<ModConfigSpec.ConfigValue<?>, Object>> passes = new ArrayList<>();
        List<String> names = new ArrayList<>();
        passes.add(cv -> limit(cv, false));
        names.add("lowest numbers");
        passes.add(cv -> limit(cv, true));
        names.add("highest numbers");
        passes.add(cv -> cv instanceof ModConfigSpec.BooleanValue b ? !b.getDefault() : null);
        names.add("switches flipped");
        int choices = 0;
        for (ModConfigSpec.ConfigValue<?> cv : all) if (cv instanceof ModConfigSpec.EnumValue<?> ev) choices = Math.max(choices, ev.getDefault().getDeclaringClass().getEnumConstants().length);
        for (int i = 0; i < choices; i++) {
            int k = i;
            passes.add(cv -> {
                if (!(cv instanceof ModConfigSpec.EnumValue<?> ev)) return null;
                Object[] c = ev.getDefault().getDeclaringClass().getEnumConstants();
                return c[k % c.length];
            });
            names.add("choices #" + i);
        }
        org.slf4j.LoggerFactory.getLogger("bettacraft/gametest").info("[m42] config extremes: {} server values, {} passes", all.size(), passes.size());
        runPasses(h, all, tanks, passes, names, 0, problems);
    }

    private static void runPasses(GameTestHelper h, List<ModConfigSpec.ConfigValue<?>> all, List<TankBlockEntity> tanks,
                                  List<Function<ModConfigSpec.ConfigValue<?>, Object>> passes, List<String> names, int i, List<String> problems) {
        if (i >= passes.size()) {
            if (!problems.isEmpty()) throw new GameTestFailure(problems.size() + " problem(s): " + String.join("; ", problems.subList(0, Math.min(6, problems.size()))));
            h.succeed();
            return;
        }
        Map<ModConfigSpec.ConfigValue<?>, Object> old;
        long t0 = System.nanoTime();
        Map<ModConfigSpec.ConfigValue<?>, Object> before = snapshot(all);
        old = set(all, passes.get(i));
        try {
            careAndCheck(h, tanks, names.get(i), problems);
        } catch (RuntimeException ex) {
            restore(old);
            throw ex;
        }
        long ms = (System.nanoTime() - t0) / 1_000_000;
        org.slf4j.LoggerFactory.getLogger("bettacraft/gametest").info("[m42] config pass '{}': {} values set, care {} ms", names.get(i), old.size(), ms);
        if (ms > 10_000) problems.add(names.get(i) + ": a day of care took " + ms + " ms");
        // two seconds of real ticks (the animals' own ticks, the care loop) with these values, then back
        h.runAfterDelay(40, () -> {
            try {
                for (TankBlockEntity c : tanks) check(h, c, names.get(i) + " (ticks)", problems);
            } finally {
                restore(old);
            }
            Map<ModConfigSpec.ConfigValue<?>, Object> after = snapshot(all);
            if (!after.equals(before)) problems.add(names.get(i) + ": the config is not back to its old values");
            runPasses(h, all, tanks, passes, names, i + 1, problems);
        });
    }

    static Map<ModConfigSpec.ConfigValue<?>, Object> snapshot(List<ModConfigSpec.ConfigValue<?>> all) {
        Map<ModConfigSpec.ConfigValue<?>, Object> m = new LinkedHashMap<>();
        for (var cv : all) m.put(cv, cv.get());
        return m;
    }

    /** The presets and care modes switched one after the other while the care runs; CUSTOM gives the file's values back. */
    @GameTest(template = EMPTY, batch = "bettacraft_m42_config_presets", timeoutTicks = 2400)
    @SuppressWarnings("unchecked")
    public static void presetsAndCareModesSwitchedWhileTheCareRuns(GameTestHelper h) {
        List<TankBlockEntity> tanks = List.of(stocked(h, 2, 2));
        List<ModConfigSpec.ConfigValue<?>> server = values(ServerConfig.class);
        Map<ModConfigSpec.ConfigValue<?>, Object> fileValues = new LinkedHashMap<>();
        for (var cv : server) fileValues.put(cv, cv.get());
        Preset presetBefore = CommonConfig.PRESET.get();
        ServerConfig.CareMode modeBefore = ServerConfig.CARE_MODE.get();
        List<String> problems = new ArrayList<>();
        List<Runnable> steps = new ArrayList<>();
        for (Preset p : Preset.values()) {
            for (ServerConfig.CareMode m : ServerConfig.CareMode.values()) {
                steps.add(() -> {
                    CommonConfig.PRESET.set(p);
                    ServerConfig.CARE_MODE.set(m);
                    careAndCheck(h, tanks, "preset " + p + ", care " + m, problems);
                });
            }
        }
        Runnable restore = () -> {
            CommonConfig.PRESET.set(presetBefore);
            ServerConfig.CARE_MODE.set(modeBefore);
        };
        chain(h, steps, 0, restore, () -> {
            CommonConfig.PRESET.set(Preset.CUSTOM);
            for (var e : fileValues.entrySet()) {
                if (e.getKey() == ServerConfig.CARE_MODE) continue;
                Object eff = ServerConfig.getOr((ModConfigSpec.ConfigValue<Object>) e.getKey(), null);
                if (!java.util.Objects.equals(eff, e.getValue())) problems.add("CUSTOM: " + String.join(".", e.getKey().getPath()) + " is " + eff + ", the file says " + e.getValue());
            }
            restore.run();
            if (!problems.isEmpty()) throw new GameTestFailure(problems.size() + " problem(s): " + String.join("; ", problems.subList(0, Math.min(6, problems.size()))));
            h.succeed();
        });
    }

    private static void chain(GameTestHelper h, List<Runnable> steps, int i, Runnable restore, Runnable last) {
        if (i >= steps.size()) {
            last.run();
            return;
        }
        try {
            steps.get(i).run();
        } catch (RuntimeException ex) {
            restore.run();
            throw ex;
        }
        h.runAfterDelay(20, () -> chain(h, steps, i + 1, restore, last));
    }
}
