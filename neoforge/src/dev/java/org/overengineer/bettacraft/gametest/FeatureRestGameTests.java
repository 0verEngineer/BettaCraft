package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.FeatureContent;
import org.overengineer.bettacraft.config.FeatureGate;
import org.overengineer.bettacraft.config.FeatureLootCondition;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaCare;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateCare;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.sources.TraderOffers;
import org.overengineer.bettacraft.sources.WildTables;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankPower;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M33 part b: one GameTest per feature switch (generated from {@link Feature}, each in its own batch). With the switch off:
 * a stocked tank (bettas, neons, snails; no shrimp, the bettas would hunt them) goes through whole care steps without a crash and loses nobody; the
 * recipes whose result belongs to a feature that is off are gone, no offer of the trader makes anything that is off, its loot
 * condition fails, the net brings nothing that is off, the creative tab / JEI / handbook gate hides its items; a probe per
 * group checks what "rests" means for it (no hunger, no breeding, no FE, no interior, no sounds ...). Switched on again,
 * every recipe and offer is back and the probe sees the normal behaviour. The switch is restored in {@code finally}.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FeatureRestGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private FeatureRestGameTests() {}

    @GameTestGenerator
    public static Collection<TestFunction> everySwitch() {
        List<TestFunction> out = new ArrayList<>();
        for (Feature f : Feature.values()) {
            String name = f.path().replace('.', '_').toLowerCase(java.util.Locale.ROOT);
            out.add(new TestFunction("bettacraft_m33b_" + name, "featureoff_" + name, BettaCraft.MOD_ID + ":" + EMPTY, 400, 0, true, h -> run(h, f)));
        }
        return out;
    }

    // ------------------------------------------------------------------ the stocked tank

    private record Stock(TankBlockEntity tank, List<BettaEntity> bettas, List<TankmateEntity> mates, List<Critter> critters) {
        int alive(ServerLevel level) {
            int n = 0;
            for (Entity e : TankInhabitants.liveInhabitants(level, tank.getTank())) if (e.isAlive()) n++;
            for (Critter k : tank.getTank().critters()) if (k.alive()) n++;
            return n;
        }
    }

    private static Stock stock(GameTestHelper h) {
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
        TankInteractions.changeWater(level, c, 48_000);
        c.getTank().setWaterTemp(26F);
        c.getTank().setTargetTemp(26F);
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
        critters.add(TankCritters.add(level, c, CritterSpecies.ZEBRA_NERITE, CritterGenes.WILD, true, CritterSpecies.ZEBRA_NERITE.juvenileDays() + 1));
        critters.add(TankCritters.add(level, c, CritterSpecies.RAMSHORN, CritterGenes.WILD, false, CritterSpecies.RAMSHORN.juvenileDays() + 1));
        critters.add(TankCritters.add(level, c, CritterSpecies.ZEBRA_NERITE, CritterGenes.WILD, false, CritterSpecies.ZEBRA_NERITE.juvenileDays() + 1));
        if (critters.contains(null)) throw new GameTestAssertException("could not add the small animals");
        return new Stock(c, bettas, mates, critters);
    }

    /** A few hours of whole care steps, everyone fed (also the entity side: their needs). */
    private static void care(GameTestHelper h, Stock s) {
        ServerLevel level = h.getLevel();
        for (int i = 0; i < 3; i++) {
            for (BettaEntity b : s.bettas()) b.health().feed();
            for (TankmateEntity m : s.mates()) m.health().feed();
            s.tank().getTank().setCritterFood(50);
            TankCare.care(level, s.tank(), 1000);
            for (BettaEntity b : s.bettas()) if (b.isAlive()) BettaCare.advance(level, s.tank(), b, 1000 / 24000.0);
            for (TankmateEntity m : s.mates()) if (m.isAlive()) TankmateCare.advance(level, s.tank(), m, 1000 / 24000.0);
        }
    }

    // ------------------------------------------------------------------ what a feature that is off must not give

    private static void check(boolean ok, Feature f, String what) {
        if (!ok) throw new GameTestAssertException(f.key() + ": " + what);
    }

    /** Recipe id -> result item path (ours only). */
    private static Map<ResourceLocation, String> results(ServerLevel level, RecipeManager rm) {
        Map<ResourceLocation, String> out = new java.util.HashMap<>();
        for (RecipeHolder<?> r : rm.getRecipes()) {
            ItemStack res = r.value().getResultItem(level.registryAccess());
            ResourceLocation id = res.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(res.getItem());
            out.put(r.id(), id != null && id.getNamespace().equals(BettaCraft.MOD_ID) ? id.getPath() : "");
        }
        return out;
    }

    private static Set<String> allowedOffers() {
        Set<String> out = new HashSet<>();
        for (TraderOffers.Offer o : TraderOffers.all()) if (o.allowed()) out.add(o.id());
        return out;
    }

    private static boolean on(Object catchKey) {
        if (catchKey instanceof TankmateSpecies t) return Features.enabled(t.feature());
        if (catchKey instanceof CritterSpecies c) return Features.all(FeatureContent.ofCritter(c));
        if (catchKey instanceof PlantSpecies p) return Features.all(FeatureContent.ofItem(p.id()));
        if ("frog".equals(catchKey)) return Features.enabled(Feature.FROGS);
        return true;
    }

    private static void contentGone(GameTestHelper h, Feature f, Map<ResourceLocation, String> before) {
        ServerLevel level = h.getLevel();
        RecipeManager rm = FeatureSwitchGameTests.recipes(level);
        for (var e : before.entrySet()) {
            if (e.getValue().isEmpty() || Features.all(FeatureContent.ofItem(e.getValue()))) continue;
            check(rm.byKey(e.getKey()).isEmpty(), f, "recipe " + e.getKey() + " of " + e.getValue() + " is still there");
        }
        WanderingTrader trader = h.spawn(EntityType.WANDERING_TRADER, new BlockPos(1, 1, 1));
        try {
            for (TraderOffers.Offer o : TraderOffers.all()) {
                if (o.allowed()) continue;
                for (int i = 0; i < 10; i++) check(o.gatedListing().getOffer(trader, level.getRandom()) == null, f, "the trader still offers " + o.id());
            }
        } finally {
            trader.discard();
        }
        check(!new FeatureLootCondition(f).test(null), f, "its loot condition holds");
        for (NetCatching.Water w : NetCatching.Water.values()) {
            var odds = NetCatching.odds(w, true, true, true, 1, 0.5, 0.5, NetCatching.plantShare(), WildTables.netPlants(w));
            for (var k : odds.keySet()) check(on(k), f, "the net in " + w + " brings " + k);
        }
        for (var item : BuiltInRegistries.ITEM) {
            if (FeatureGate.of(item).contains(f)) check(!FeatureGate.item(item) && !FeatureGate.stack(new ItemStack(item)), f, "item " + item + " is not hidden");
        }
    }

    // ------------------------------------------------------------------ the probes: what "rests" means

    private interface Probe {
        /** Returns a description of what is wrong, or null. {@code off}: the feature is off now. */
        String test(GameTestHelper h, Stock s, boolean off);
    }

    private static final Map<Feature, Probe> PROBES = new java.util.EnumMap<>(Feature.class);

    static {
        PROBES.put(Feature.BETTAS, (h, s, off) -> {
            BettaEntity b = s.bettas().getFirst();
            b.health().feed();
            double before = b.health().daysSinceFed();
            BettaCare.advance(h.getLevel(), s.tank(), b, 0.5);
            boolean moved = b.health().daysSinceFed() > before + 0.4;
            return off == moved ? (off ? "the betta still gets hungry" : "the betta does not get hungry") : null;
        });
        PROBES.put(Feature.TANKMATES, (h, s, off) -> {
            TankmateEntity m = s.mates().getFirst();
            m.health().feed();
            double before = m.health().daysSinceFed();
            TankmateCare.advance(h.getLevel(), s.tank(), m, 0.5);
            boolean moved = m.health().daysSinceFed() > before + 0.4;
            return off == moved ? (off ? "the neon still gets hungry" : "the neon does not get hungry") : null;
        });
        PROBES.put(Feature.INVERTEBRATES, (h, s, off) -> {
            Critter k = s.critters().getFirst();
            double before = k.age;
            TankCritters.step(h.getLevel(), s.tank(), 0.5);
            boolean aged = k.age > before + 0.4;
            return off == aged ? (off ? "the snails still age" : "the snails do not age") : null;
        });
        PROBES.put(Feature.BREEDING, (h, s, off) -> {
            boolean none = TankCritters.breeding().values().stream().allMatch(v -> v == 0);
            return off != none ? "small animal breeding rates " + TankCritters.breeding() : null;
        });
        PROBES.put(Feature.POWER, (h, s, off) -> {
            boolean was = ServerConfig.POWER_HEATER.get();
            ServerConfig.POWER_HEATER.set(true);
            try {
                return off == TankPower.anyRequired() ? (off ? "devices still need FE" : "the heater option has no effect") : null;
            } finally {
                ServerConfig.POWER_HEATER.set(was);
            }
        });
        PROBES.put(Feature.SALTWATER, (h, s, off) ->
                off == (org.overengineer.bettacraft.reef.SaltTolerance.speed() > 0) ? "salt shock speed " + org.overengineer.bettacraft.reef.SaltTolerance.speed() : null);
        PROBES.put(Feature.SOUNDS, (h, s, off) -> off == org.overengineer.bettacraft.registry.ModSounds.devices() ? "device sounds" : null);
        PROBES.put(Feature.COMPAT_GUIDEME, (h, s, off) -> off && !org.overengineer.bettacraft.progression.GuideBook.createBook().isEmpty() ? "a handbook is made" : null);
        PROBES.put(Feature.COMPAT_OVERLAYS, (h, s, off) -> off && org.overengineer.bettacraft.compat.info.OverlayProvider.shows(
                org.overengineer.bettacraft.compat.info.OverlayProvider.Overlay.JADE) ? "the overlay lines show" : null);
        PROBES.put(Feature.LIVE_FOOD, (h, s, off) -> {
            ItemStack culture = new ItemStack(ModItems.INFUSORIA_CULTURE.get());
            culture.set(org.overengineer.bettacraft.registry.ModDataComponents.CULTURE_START.get(), h.getLevel().getGameTime() - 10_000_000L);
            ItemStack ripe = ((org.overengineer.bettacraft.food.CultureItem) culture.getItem()).ripen(culture, h.getLevel(), s.bettas().getFirst());
            return off == (ripe != null) ? (off ? "a culture ripens" : "an old culture does not ripen") : null;
        });
        PROBES.put(Feature.INTERIOR, (h, s, off) -> {
            if (!off) return null; // going in prepares the region over many ticks: only the refusal is checked
            ServerPlayer sp = MockPlayers.serverPlayer(h);
            var before = sp.level();
            org.overengineer.bettacraft.tank.interior.InteriorManager.enter(sp, s.tank().getBlockPos());
            return sp.level() != before ? "a player went into the interior" : null;
        });
    }

    // ------------------------------------------------------------------ the test

    private static void run(GameTestHelper h, Feature f) {
        ServerLevel level = h.getLevel();
        check(Features.enabled(f), f, "on before the test (another test left it off?)");
        Map<ResourceLocation, String> before = results(level, FeatureSwitchGameTests.recipes(level));
        Set<String> offers = allowedOffers();
        Stock s = stock(h);
        Probe probe = PROBES.get(f);
        boolean was = Features.own(f);
        Features.set(f, false);
        try {
            check(!Features.enabled(f), f, "still on");
            int alive = s.alive(level);
            care(h, s);
            check(s.alive(level) >= alive, f, "animals were lost while it rested: " + alive + " -> " + s.alive(level));
            contentGone(h, f, before);
            if (probe != null) {
                String bad = probe.test(h, s, true);
                if (bad != null) check(false, f, "off: " + bad);
            }
        } finally {
            Features.set(f, was);
        }
        // on again: everything is back
        check(Features.enabled(f), f, "not on again");
        RecipeManager rm = FeatureSwitchGameTests.recipes(level);
        for (ResourceLocation id : before.keySet()) check(rm.byKey(id).isPresent(), f, "recipe " + id + " not back");
        check(allowedOffers().containsAll(offers), f, "offers not back");
        check(new FeatureLootCondition(f).test(null), f, "loot condition not back");
        care(h, s);
        if (probe != null) {
            String bad = probe.test(h, s, false);
            if (bad != null) check(false, f, "on again: " + bad);
        }
        h.succeed();
    }
}
