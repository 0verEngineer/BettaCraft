package org.overengineer.bettacraft.gametest.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.foreign.ForeignBreeding;
import org.overengineer.bettacraft.foreign.ForeignCare;
import org.overengineer.bettacraft.gametest.GenerationsGameTests;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;

/**
 * M42 part c: breeding runs over generations with animals of other mods by their shipped profiles (the normal suite has the
 * vanilla stand-ins and Fintastic's guppies, {@code GenerationsGameTests}).
 */
@PrefixGameTestTemplate(false)
public final class IntegrationBreedingTests {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("bettacraft/integration");

    private IntegrationBreedingTests() {}

    /**
     * Critters and Companions koi (eggs on the plants, 18-24 °C, the Variant inherited) over three generations: two colours
     * breed young of a parent's colour; two young of one colour by fish bag into a new planted pond tank breed only that colour.
     */
    @GameTest(template = IntegrationSuite.EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_breeding", timeoutTicks = 300)
    public static void koiBreedTheirColoursOverGenerations(GameTestHelper h) {
        if (IntegrationSuite.lacks(h, "crittersandcompanions")) return;
        ServerLevel level = h.getLevel();
        ServerPlayer sp = GenerationsGameTests.breeder(h);
        EntityType<?> koi = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("crittersandcompanions:koi_fish"));
        TankBlockEntity c = pond(h, 1);
        GenerationsGameTests.addAnimal(h, c, koi, t -> t.putInt("Variant", 0));
        GenerationsGameTests.addAnimal(h, c, koi, t -> t.putInt("Variant", 3));
        Set<Integer> parents = Set.of(0, 3);
        for (int gen = 1; gen <= 3; gen++) {
            List<Entity> before = new ArrayList<>(TankInhabitants.liveInhabitants(level, c.getTank()));
            level.getRandom().setSeed(4340L + gen);
            for (int i = 0; i < 3000 && TankInhabitants.liveInhabitants(level, c.getTank()).size() == before.size(); i++) {
                ForeignCare.feed(GenerationsGameTests.livings(before), 2, null);
                c.getTank().setWaterTemp(21F);
                c.getTank().setWaterQuality(1F);
                ForeignBreeding.step(level, c, 0.05);
            }
            List<Entity> young = TankInhabitants.liveInhabitants(level, c.getTank()).stream().filter(e -> !before.contains(e)).toList();
            h.assertTrue(!young.isEmpty(), "young koi in generation " + gen + ": " + c.getTank().log().stream().map(l -> l.key()).distinct().toList());
            for (Entity e : young) {
                h.assertTrue(parents.contains(GenerationsGameTests.variant(e)), "the colour of a parent: " + GenerationsGameTests.variant(e));
                h.assertTrue(TankInhabitants.hasTankRef(e), "the young live in the tank");
                if (e instanceof Mob m) m.setBaby(false); // grown up (sped up)
            }
            LOG.info("[integration] koi F{}: {} young, colours {}", gen, young.size(), young.stream().map(GenerationsGameTests::variant).toList());
            if (gen == 3) break;
            int keep = GenerationsGameTests.variant(young.getFirst());
            List<Entity> pair = young.stream().filter(e -> GenerationsGameTests.variant(e) == keep).limit(2).toList();
            if (pair.size() < 2) pair = List.of(young.getFirst(), before.stream().filter(e -> GenerationsGameTests.variant(e) == keep).findFirst().orElseThrow());
            TankBlockEntity n = pond(h, 1 + gen * 10);
            for (Entity e : pair) {
                Entity m = GenerationsGameTests.move(h, sp, c, e, n, new ItemStack(ModItems.FISH_BAG.get()), true);
                h.assertTrue(GenerationsGameTests.variant(m) == keep, "the bag keeps the colour");
            }
            parents = Set.of(keep);
            c = n;
        }
        h.succeed();
    }

    /** A planted 4x2x4 tank of 21 °C (the koi need plants for their eggs and 16 water blocks). */
    private static TankBlockEntity pond(GameTestHelper h, int x) {
        TankBlockEntity c = GenerationsGameTests.waterTank(h, x, 2, 4, 2, 4, 21F);
        for (int px = 8; px < 64; px += 16) for (int pz = 8; pz < 64; pz += 16) GenerationsGameTests.plant(h, c, px, pz);
        return c;
    }
}
