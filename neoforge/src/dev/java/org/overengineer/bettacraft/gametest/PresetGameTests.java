package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.chem.ChemTolerance;
import org.overengineer.bettacraft.chem.TankChemistry;
import org.overengineer.bettacraft.command.PresetCommand;
import org.overengineer.bettacraft.config.Care;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Preset;
import org.overengineer.bettacraft.config.Presets;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaCare;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.frog.DwarfFrogEntity;
import org.overengineer.bettacraft.entity.tankmate.TankmateCare;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.network.FeatureSyncPayload;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankNitrate;
import org.overengineer.bettacraft.tank.TankPower;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M33 part c: the presets applied in a running game (only in memory, each test in its own batch, every value restored in
 * {@code finally}): simple (no chemistry stress, no FE although a device option asks for it, a changed value and a kept path
 * win), realistic (strict: sick after five hungry days, FE required, foreign animals mind the salt) and decor (a stocked
 * tank of bettas, neons, shrimp and snails, a dwarf frog and an axolotl in hopeless water and never fed: nobody dies, nobody
 * gets sick or hungry, damage leaves half a heart). And {@code care.mode = NO_DEATH} alone: sick and starving at the brink,
 * but alive; /kill still kills.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PresetGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private PresetGameTests() {}

    // ------------------------------------------------------------------ config save / restore

    /** The values these tests change, read raw (past the preset) and set back afterwards. */
    private record Saved(Preset preset, List<? extends String> keep, ServerConfig.CareMode care, ServerConfig.PowerCosts heater, double sick, boolean hunt) {
        static Saved take() {
            return new Saved(CommonConfig.PRESET.get(), CommonConfig.PRESET_KEEP.get(), ServerConfig.CARE_MODE.get(),
                    ServerConfig.POWER_COSTS.get(), ServerConfig.HEALTH_DAYS_UNTIL_SICK.get(), ServerConfig.CRITTERS_BETTAS_HUNT.get());
        }

        void restore() {
            CommonConfig.PRESET.set(preset);
            CommonConfig.PRESET_KEEP.set(keep);
            ServerConfig.CARE_MODE.set(care);
            ServerConfig.POWER_COSTS.set(heater);
            ServerConfig.HEALTH_DAYS_UNTIL_SICK.set(sick);
            ServerConfig.CRITTERS_BETTAS_HUNT.set(hunt);
        }
    }

    private static void check(GameTestHelper h, boolean ok, String what) {
        h.assertTrue(ok, what);
    }

    // ------------------------------------------------------------------ the tank

    private record Stock(TankBlockEntity tank, List<BettaEntity> bettas, List<TankmateEntity> mates, List<Critter> critters,
                         DwarfFrogEntity frog, Axolotl axolotl) {
        List<LivingEntity> animals() {
            List<LivingEntity> out = new ArrayList<>(bettas);
            out.addAll(mates);
            if (frog != null) out.add(frog);
            if (axolotl != null) out.add(axolotl);
            return out;
        }
    }

    private static TankBlockEntity tank(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int a = 0; a < 4; a++)
            for (int b = 0; b < 3; b++)
                for (int c = 0; c < 4; c++) {
                    BlockPos p = new BlockPos(4 + a, 1 + b, 4 + c);
                    h.setBlock(p, ModBlocks.TANK.get().defaultBlockState());
                    TankMultiblock.onTankBlockAdded(level, h.absolutePos(p), null);
                }
        TankBlockEntity c = TankMultiblock.getController(level, h.absolutePos(new BlockPos(4, 1, 4)));
        if (c == null) throw new GameTestAssertException("tank did not form");
        TankInteractions.changeWater(level, c, 44_000); // an air gap for the frog
        c.getTank().setWaterTemp(26F);
        c.getTank().setTargetTemp(26F);
        return c;
    }

    private static Stock stock(GameTestHelper h, boolean frogAndAxolotl) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h);
        List<BettaEntity> bettas = new ArrayList<>();
        for (boolean female : new boolean[]{false, true}) {
            BettaData d = BettaData.create(BettaGenotype.randomDomestic(RandomSource.create(female ? 2 : 1)), female, 0);
            var err = TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d), ModEntities.BETTA.get(), null, false);
            if (err != null) throw new GameTestAssertException("betta: " + err.getString());
        }
        for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) if (e instanceof BettaEntity b) bettas.add(b);
        List<TankmateEntity> mates = new ArrayList<>();
        var w = new org.overengineer.bettacraft.tank.TankWaterVolume(c.getTank());
        for (int i = 0; i < 3; i++) {
            Vec3 p = w.clamp(new Vec3(w.box().getCenter().x + i * 0.3, w.box().minY + 0.4, w.box().getCenter().z), 0.35, 0.1, 0.2);
            TankmateEntity e = TankmateBreeding.spawn(level, c, TankmateSpecies.NEON_TETRA, TankmateGenes.wild(TankmateSpecies.NEON_TETRA), i % 2 == 0,
                    LifeStage.ADULT, 0, p);
            if (e == null) throw new GameTestAssertException("could not spawn a neon");
            mates.add(e);
        }
        List<Critter> critters = new ArrayList<>();
        for (CritterSpecies s : List.of(CritterSpecies.NEOCARIDINA, CritterSpecies.NEOCARIDINA, CritterSpecies.RAMSHORN, CritterSpecies.ZEBRA_NERITE)) {
            critters.add(TankCritters.add(level, c, s, CritterGenes.WILD, critters.size() % 2 == 0, s.juvenileDays() + 1));
        }
        if (critters.contains(null)) throw new GameTestAssertException("could not add the small animals");
        DwarfFrogEntity frog = null;
        Axolotl axolotl = null;
        if (frogAndAxolotl) {
            DwarfFrogEntity f = ModEntities.DWARF_FROG.get().create(level);
            ItemStack bag = FishBagItem.fillWith(f);
            f.discard();
            var err = TankInhabitants.insert(level, c, bag, ModEntities.DWARF_FROG.get(), null, 0L);
            if (err != null) throw new GameTestAssertException("frog: " + err.getString());
            for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) if (e instanceof DwarfFrogEntity d) frog = d;
            axolotl = (Axolotl) ForeignGameTests.add(h, c, EntityType.AXOLOTL, t -> {});
        }
        return new Stock(c, bettas, mates, critters, frog, axolotl);
    }

    /** {@code days} of whole care steps in hopeless water (hot, dirty, acid, never fed); the entity side too. */
    private static void neglect(GameTestHelper h, Stock s, int days) {
        ServerLevel level = h.getLevel();
        var t = s.tank().getTank();
        for (int i = 0; i < days * 2; i++) {
            t.setWaterTemp(35F);
            t.setTargetTemp(35F);
            t.setWaterQuality(0F);
            t.setPh(4.0F);
            t.setNitrate(300F);
            t.setCritterFood(0);
            TankCare.care(level, s.tank(), 12_000);
            for (BettaEntity b : s.bettas()) if (b.isAlive()) BettaCare.advance(level, s.tank(), b, 0.5);
            for (TankmateEntity m : s.mates()) if (m.isAlive()) TankmateCare.advance(level, s.tank(), m, 0.5);
        }
        if (s.frog() != null && s.frog().isAlive()) s.frog().setLastFedForTest(level.getGameTime() - 24_000L * days);
    }

    private static int aliveCritters(Stock s) {
        int n = 0;
        for (Critter k : s.tank().getTank().critters()) if (k.alive()) n++;
        return n;
    }

    // ------------------------------------------------------------------ simple

    @GameTest(template = EMPTY, batch = "bettacraft_m33c_preset_simple", timeoutTicks = 200)
    public static void presetSimple(GameTestHelper h) {
        Saved saved = Saved.take();
        try {
            PresetCommand.apply(h.getLevel().getServer(), Preset.SIMPLE, false);
            check(h, Presets.active() == Preset.SIMPLE, "simple is active");
            check(h, !Features.enabled(Feature.WATER_CHEMISTRY) && !TankChemistry.enabled() && !Features.enabled(Feature.CO2), "no water chemistry, no CO2");
            check(h, TankNitrate.difficulty() == ServerConfig.WaterChangeDifficulty.OFF, "no forced water changes");
            // a device option asking for FE has no effect: power is off
            ServerConfig.POWER_COSTS.set(ServerConfig.PowerCosts.HARD);
            check(h, !TankPower.anyRequired(), "no FE needed with power off");
            check(h, BettaCare.params().daysUntilSick() == 14.0, "gentler: sick after 14 days");
            // the tank: acid, hard water and much nitrate stress nobody (the bettas do not hunt the shrimp here)
            ServerConfig.CRITTERS_BETTAS_HUNT.set(false);
            Stock s = stock(h, false);
            var t = s.tank().getTank();
            ServerLevel level = h.getLevel();
            for (int i = 0; i < 6; i++) {
                t.setPh(4.0F);
                t.setTds(2000F);
                t.setNitrate(300F);
                t.setWaterQuality(1F); // no filter in this tank: the water quality is not what is tested (it stays in simple)
                for (BettaEntity b : s.bettas()) b.health().feed();
                for (TankmateEntity m : s.mates()) m.health().feed();
                t.setCritterFood(50);
                TankCare.care(level, s.tank(), 12_000);
                for (BettaEntity b : s.bettas()) BettaCare.advance(level, s.tank(), b, 0.5);
                for (TankmateEntity m : s.mates()) TankmateCare.advance(level, s.tank(), m, 0.5);
            }
            check(h, !TankChemistry.bad(t, ChemTolerance.BETTA), "pH 4 is no problem without chemistry");
            for (BettaEntity b : s.bettas()) check(h, b.isAlive() && b.health().waterStress() == 0, "no water stress for " + b.getName().getString());
            check(h, aliveCritters(s) >= s.critters().size(), "no small animal died, young may come (" + aliveCritters(s) + "/" + s.critters().size() + ")");
            // precedence: a changed value wins, a kept path keeps its default
            ServerConfig.HEALTH_DAYS_UNTIL_SICK.set(10.0);
            check(h, BettaCare.params().daysUntilSick() == 10.0, "a value changed in the file wins over the preset");
            CommonConfig.PRESET_KEEP.set(List.of("power.enabled"));
            check(h, Features.enabled(Feature.POWER) && TankPower.anyRequired(), "power kept on: the heater needs FE again");
            CommonConfig.PRESET_KEEP.set(List.of());
            // back to custom: every value the file's
            PresetCommand.apply(h.getLevel().getServer(), Preset.CUSTOM, false);
            check(h, Features.enabled(Feature.WATER_CHEMISTRY) && TankNitrate.difficulty() == ServerConfig.WaterChangeDifficulty.NORMAL, "custom again");
            h.succeed();
        } finally {
            saved.restore();
        }
    }

    // ------------------------------------------------------------------ realistic

    @GameTest(template = EMPTY, batch = "bettacraft_m33c_preset_realistic", timeoutTicks = 200)
    public static void presetRealistic(GameTestHelper h) {
        Saved saved = Saved.take();
        try {
            PresetCommand.apply(h.getLevel().getServer(), Preset.REALISTIC, false);
            check(h, Features.enabled(Feature.WATER_CHEMISTRY) && Features.enabled(Feature.POWER) && Features.enabled(Feature.BREEDING), "everything on");
            check(h, ServerConfig.getOr(ServerConfig.SALT_FOREIGN, null) == ServerConfig.ForeignSalinity.REALISTIC, "foreign animals mind the salt");
            check(h, TankNitrate.difficulty() == ServerConfig.WaterChangeDifficulty.REALISTIC, "water changes every week");
            check(h, TankPower.heaterRequired() && TankPower.filterRequired() && TankPower.lightRequired() && TankPower.airRequired()
                    && TankPower.misterRequired(), "the devices need FE");
            // strict: a hungry betta is sick after five days (the default: seven)
            Stock s = stock(h, false);
            ServerLevel level = h.getLevel();
            BettaEntity b = s.bettas().getFirst();
            b.health().feed();
            BettaCare.advance(level, s.tank(), b, 5.5);
            check(h, b.isAlive() && b.health().isSick(), "sick after 5.5 hungry days");
            // kept: power.costs keeps its file value (M67: OFF, the devices run without FE again)
            ServerConfig.POWER_COSTS.set(ServerConfig.PowerCosts.OFF);
            CommonConfig.PRESET_KEEP.set(List.of("power.costs"));
            check(h, !TankPower.heaterRequired() && !TankPower.filterRequired(), "the kept power.costs");
            h.succeed();
        } finally {
            saved.restore();
        }
    }

    // ------------------------------------------------------------------ decor

    @GameTest(template = EMPTY, batch = "bettacraft_m33c_preset_decor", timeoutTicks = 400)
    public static void presetDecor(GameTestHelper h) {
        Saved saved = Saved.take();
        try {
            PresetCommand.apply(h.getLevel().getServer(), Preset.DECOR, false);
            check(h, Care.mode() == ServerConfig.CareMode.DECOR && !Care.canDie() && !Care.needs(), "care.mode DECOR");
            check(h, !Features.enabled(Feature.BREEDING) && !Features.enabled(Feature.ALGAE) && !Features.enabled(Feature.POWER), "no breeding, algae, power");
            FeatureSyncPayload sync = FeatureSyncPayload.current();
            check(h, "decor".equals(sync.preset()) && sync.off().contains("breeding.enabled"), "the clients get the preset and its switches: " + sync);
            Stock s = stock(h, true);
            int animals = s.animals().size(), critters = s.critters().size();
            neglect(h, s, 30);
            for (LivingEntity e : s.animals()) check(h, e.isAlive(), e.getName().getString() + " died");
            for (BettaEntity b : s.bettas()) check(h, !b.health().isSick() && !b.health().isHungry(BettaCare.params()), "a betta is sick or hungry");
            for (TankmateEntity m : s.mates()) check(h, !m.health().isSick(), "a neon is sick");
            check(h, aliveCritters(s) == critters, "the small animals live (" + aliveCritters(s) + "/" + critters + ")");
            for (Critter k : s.tank().getTank().critters()) check(h, k.hunger == 0, "a small animal is hungry");
            check(h, s.axolotl().getHealth() == s.axolotl().getMaxHealth(), "the axolotl takes no heat damage");
            // damage that is not a player's leaves half a heart
            s.frog().hurt(h.getLevel().damageSources().starve(), 100F);
            s.axolotl().hurt(h.getLevel().damageSources().generic(), 100F);
            check(h, s.frog().isAlive() && s.axolotl().isAlive() && s.frog().getHealth() >= 1F, "damage killed");
            check(h, s.animals().size() == animals, "nobody left");
            // the frog's hunger: fed again by the next tick (decor), it neither starves nor drowns
            h.runAfterDelay(30, () -> {
                try {
                    check(h, s.frog().isAlive() && s.frog().daysSinceFed() < 0.01, "the frog is not hungry (" + s.frog().daysSinceFed() + ")");
                    h.succeed();
                } finally {
                    saved.restore();
                }
            });
        } catch (RuntimeException e) {
            saved.restore();
            throw e;
        }
    }

    // ------------------------------------------------------------------ care.mode NO_DEATH alone

    @GameTest(template = EMPTY, batch = "bettacraft_m33c_care_no_death", timeoutTicks = 200)
    public static void careModeNoDeath(GameTestHelper h) {
        Saved saved = Saved.take();
        try {
            ServerConfig.CARE_MODE.set(ServerConfig.CareMode.NO_DEATH);
            check(h, Presets.active() == Preset.CUSTOM, "no preset");
            Stock s = stock(h, true);
            int critters = s.critters().size();
            neglect(h, s, 40);
            for (LivingEntity e : s.animals()) check(h, e.isAlive(), e.getName().getString() + " died");
            for (BettaEntity b : s.bettas()) check(h, b.health().isSick() && b.health().isHungry(BettaCare.params()), "the needs stay: sick and hungry");
            check(h, aliveCritters(s) >= critters, "the small animals live (" + aliveCritters(s) + "/" + critters + ")");
            check(h, s.axolotl().getHealth() < s.axolotl().getMaxHealth(), "the hot water hurts the axolotl (" + s.axolotl().getHealth() + ")");
            s.frog().hurt(h.getLevel().damageSources().drown(), 100F);
            check(h, s.frog().isAlive(), "drowning killed the frog");
            // /kill still kills
            s.frog().kill();
            check(h, !s.frog().isAlive(), "/kill does not kill");
            // NORMAL again: the hopeless water kills
            ServerConfig.CARE_MODE.set(ServerConfig.CareMode.NORMAL);
            s.axolotl().hurt(h.getLevel().damageSources().generic(), 100F);
            check(h, !s.axolotl().isAlive(), "normal: damage kills");
            h.succeed();
        } finally {
            saved.restore();
        }
    }
}
