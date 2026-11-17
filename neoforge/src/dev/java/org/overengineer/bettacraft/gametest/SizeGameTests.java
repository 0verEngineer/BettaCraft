package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.AnimalSize;
import org.overengineer.bettacraft.config.AnimalSizes;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.BettaExtents;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M36 GameTests: the size setting of the bettas ({@code sizes.betta}, Normal / Large) scales their hitbox (every life stage
 * in proportion) and nothing else: the stocking, the data and the tank stay as they are, switching back and forth loses
 * no fish, the fins stay inside the glass at both sizes, the tankmates keep their size. Each test in a batch of its own (they
 * change the server config, the tests of one batch run at the same time); the setting is restored also when a test fails.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SizeGameTests {
    private static final String EMPTY = "empty_32x10x32";
    private static final double EPS = 1e-4;

    private SizeGameTests() {}

    private static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz) {
        for (int a = 0; a < sx; a++)
            for (int b = 0; b < sy; b++)
                for (int c = 0; c < sz; c++) {
                    BlockPos p = new BlockPos(x + a, y + b, z + c);
                    h.setBlock(p, ModBlocks.TANK.get().defaultBlockState());
                    TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(p), null);
                }
        TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(new BlockPos(x, y, z)));
        if (c == null) throw new GameTestFailure("tank did not form");
        TankInteractions.changeWater(h.getLevel(), c, c.getTank().capacityMb(), 0F);
        c.getTank().setWaterTemp(26F);
        c.getTank().setTargetTemp(26F);
        return c;
    }

    private static List<BettaEntity> bettas(GameTestHelper h, TankBlockEntity c, int n, long seed) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        for (int i = 0; i < n; i++) {
            BettaGenome g = BettaGenome.randomDomestic(RandomSource.create(seed + i));
            ItemStack s = BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), BettaData.create(g, i % 2 == 1, 0));
            EntityType<?> type = TankInhabitants.containedType(s);
            var err = TankInhabitants.insert(h.getLevel(), c, s, type, p, false);
            if (err != null) throw new GameTestFailure("insert failed: " + err.getString());
        }
        List<BettaEntity> out = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(h.getLevel(), c.getTank())) if (e instanceof BettaEntity b) out.add(b);
        return out;
    }

    /** Sets the size setting; the fish refresh their hitbox on their next tick, here at once. */
    private static void size(AnimalSize s, List<? extends Entity> fish) {
        ServerConfig.SIZE_BETTA.set(s);
        for (Entity e : fish) e.refreshDimensions();
    }

    /** The given steps with the setting restored afterwards, also when one fails. */
    private static void guarded(GameTestHelper h, Runnable body) {
        AnimalSize saved = ServerConfig.SIZE_BETTA.get();
        try {
            body.run();
        } finally {
            ServerConfig.SIZE_BETTA.set(saved);
        }
    }

    /** The default is Normal (0.77); Large is the size before M36: hitbox 0.5 x 0.35 for an adult, the young in proportion. */
    @GameTest(template = EMPTY, batch = "bettacraft_m36_size_hitbox", timeoutTicks = 100)
    public static void bettaHitboxFollowsTheSizeSetting(GameTestHelper h) {
        h.assertTrue(ServerConfig.SIZE_BETTA.getDefault() == AnimalSize.NORMAL, "default Normal: " + ServerConfig.SIZE_BETTA.getDefault());
        AnimalSize saved = ServerConfig.SIZE_BETTA.get();
        BettaEntity adult, young;
        try {
            adult = h.spawn(org.overengineer.bettacraft.registry.ModEntities.BETTA.get(), new Vec3(4.5, 2, 4.5));
            young = h.spawn(org.overengineer.bettacraft.registry.ModEntities.BETTA.get(), new Vec3(8.5, 2, 4.5));
            adult.setNoAi(true);
            young.setNoAi(true);
            young.setBettaData(young.getBettaData().withStage(LifeStage.JUVENILE));
            List<BettaEntity> both = List.of(adult, young);
            size(AnimalSize.LARGE, both);
            h.assertTrue(Math.abs(adult.getBbWidth() - 0.5) < EPS && Math.abs(adult.getBbHeight() - 0.35) < EPS,
                    "Large adult 0.5 x 0.35: " + adult.getBbWidth() + " x " + adult.getBbHeight());
            h.assertTrue(Math.abs(young.getBbWidth() - 0.5 * LifeStage.JUVENILE.scale()) < EPS, "Large juvenile: " + young.getBbWidth());
            h.assertTrue(Math.abs(adult.sizeScale() - 1F) < EPS, "Large draws at 1: " + adult.sizeScale());
            size(AnimalSize.NORMAL, both);
            float f = AnimalSizes.BETTA_NORMAL;
            h.assertTrue(Math.abs(adult.getBbWidth() - 0.5 * f) < EPS && Math.abs(adult.getBbHeight() - 0.35 * f) < EPS,
                    "Normal adult " + f + " of it: " + adult.getBbWidth() + " x " + adult.getBbHeight());
            h.assertTrue(Math.abs(young.getBbWidth() - 0.5 * f * LifeStage.JUVENILE.scale()) < EPS, "Normal juvenile in proportion: " + young.getBbWidth());
            h.assertTrue(Math.abs(adult.sizeScale() - f) < EPS && Math.abs(young.sizeScale() - f * LifeStage.JUVENILE.scale()) < EPS,
                    "the model scale follows: " + adult.sizeScale() + " / " + young.sizeScale());
            h.assertTrue(Math.abs(adult.getBoundingBox().getXsize() - 0.5 * f) < EPS, "the bounding box itself: " + adult.getBoundingBox());
            // the fish see a change by themselves on their next tick (config reload, the server's setting on a client)
            ServerConfig.SIZE_BETTA.set(AnimalSize.LARGE);
        } catch (RuntimeException e) {
            ServerConfig.SIZE_BETTA.set(saved);
            throw e;
        }
        h.runAfterDelay(2, () -> {
            try {
                h.assertTrue(Math.abs(adult.getBbWidth() - 0.5) < EPS && Math.abs(young.getBbWidth() - 0.5 * LifeStage.JUVENILE.scale()) < EPS,
                        "the hitbox followed on its own: " + adult.getBbWidth() + " / " + young.getBbWidth());
            } finally {
                ServerConfig.SIZE_BETTA.set(saved);
            }
            h.succeed();
        });
    }

    /** The stocking, the breeding rules (prey size) and the data of the fish do not depend on the size setting. */
    @GameTest(template = EMPTY, batch = "bettacraft_m36_size_stocking", timeoutTicks = 100)
    public static void stockingDoesNotDependOnTheSize(GameTestHelper h) {
        guarded(h, () -> {
            TankBlockEntity c = tank(h, 2, 1, 2, 4, 2, 2);
            List<BettaEntity> fish = bettas(h, c, 3, 360);
            TankmateEntity neon = TankmateBreeding.spawn(h.getLevel(), c, TankmateSpecies.NEON_TETRA, TankmateGenes.pure(TankmateSpecies.NEON_TETRA, "wild"),
                    false, LifeStage.ADULT, 0, Vec3.atCenterOf(c.getTank().min()).add(1, 0, 0.5));
            h.assertTrue(fish.size() == 3 && neon != null, "three bettas and a neon");
            List<Entity> all = new ArrayList<>(fish);
            all.add(neon);
            size(AnimalSize.LARGE, all);
            double large = TankInhabitants.stocking(h.getLevel(), c.getTank());
            var largeParts = TankInhabitants.breakdown(TankInhabitants.liveInhabitants(h.getLevel(), c.getTank()), c.getTank());
            float neonLarge = neon.getBbWidth();
            CompoundTag dataLarge = data(fish.getFirst());
            size(AnimalSize.NORMAL, all);
            double normal = TankInhabitants.stocking(h.getLevel(), c.getTank());
            var normalParts = TankInhabitants.breakdown(TankInhabitants.liveInhabitants(h.getLevel(), c.getTank()), c.getTank());
            h.assertTrue(Math.abs(large - normal) < 1e-9 && largeParts.equals(normalParts), "stocking " + large + " / " + normal);
            h.assertTrue(large > 0, "the fish count: " + large);
            h.assertTrue(neon.getBbWidth() == neonLarge, "the tankmates keep their size: " + neonLarge + " / " + neon.getBbWidth());
            h.assertTrue(data(fish.getFirst()).equals(dataLarge), "the data stays the same");
            h.assertTrue(TankInhabitants.accepts(fish.getFirst().getType(), fish.getFirst()) == TankInhabitants.Acceptance.OK, "still accepted");
            h.succeed();
        });
    }

    /**
     * Switching Large - Normal - Large - Normal with the fish swimming: none is lost, every one stays in its tank (in the
     * water, its fins inside the glass at either size), the tank keeps its inhabitants and every fish its data.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m36_size_switching", timeoutTicks = 400)
    public static void switchingLosesNoFish(GameTestHelper h) {
        AnimalSize saved = ServerConfig.SIZE_BETTA.get();
        TankBlockEntity c;
        List<BettaEntity> fish;
        try {
            ServerConfig.SIZE_BETTA.set(AnimalSize.LARGE);
            c = tank(h, 2, 1, 2, 3, 2, 2);
            fish = bettas(h, c, 4, 3600);
        } catch (RuntimeException e) {
            ServerConfig.SIZE_BETTA.set(saved);
            throw e;
        }
        h.assertTrue(fish.size() == 4, "four bettas");
        Map<UUID, CompoundTag> before = new java.util.HashMap<>();
        for (BettaEntity b : fish) before.put(b.getUUID(), data(b));
        int members = c.getTank().inhabitants().size();
        AnimalSize[] order = {AnimalSize.NORMAL, AnimalSize.LARGE, AnimalSize.NORMAL};
        int[] step = {0};
        h.onEachTick(() -> {
            long t = h.getTick();
            try {
                if (t % 60 == 59) {
                    check(h, c, fish, before, members);
                    if (step[0] < order.length) ServerConfig.SIZE_BETTA.set(order[step[0]++]);
                    else {
                        ServerConfig.SIZE_BETTA.set(saved);
                        h.succeed();
                    }
                }
            } catch (RuntimeException e) {
                ServerConfig.SIZE_BETTA.set(saved);
                throw e;
            }
        });
    }

    private static void check(GameTestHelper h, TankBlockEntity c, List<BettaEntity> fish, Map<UUID, CompoundTag> before, int members) {
        float f = AnimalSizes.bettaFactor();
        h.assertTrue(c.getTank().inhabitants().size() == members, "the tank keeps its inhabitants: " + c.getTank().inhabitants().size());
        for (BettaEntity b : fish) {
            h.assertTrue(b.isAlive() && !b.isRemoved(), "fish lost at size " + AnimalSizes.betta());
            h.assertTrue(c.getTank().contains(b.blockPosition()) && b.isInWater(), "fish left its tank: " + b.position());
            h.assertTrue(Math.abs(b.getBbWidth() - 0.5 * f) < EPS, "hitbox at " + AnimalSizes.betta() + ": " + b.getBbWidth());
            h.assertTrue(data(b).equals(before.get(b.getUUID())), "data of a fish changed");
            var w = b.tankWater();
            if (w != null) {
                Vec3 push = BettaExtents.push(w, b.position(), b.yBodyRot, b.genome().shape(), b.sizeScale(), false, 1.0 / 16 - 0.03);
                h.assertTrue(push.lengthSqr() < 0.01, "fins far outside the glass at " + AnimalSizes.betta() + ": " + push);
            }
        }
    }

    /** The saved data of a fish without the live counters that tick on their own (health timers, mood, growth). */
    private static CompoundTag data(BettaEntity b) {
        BettaData d = b.getBettaData();
        CompoundTag t = new CompoundTag();
        BettaGenome.CODEC.encodeStart(NbtOps.INSTANCE, d.genome()).result().ifPresent(g -> t.put("genome", g));
        t.putBoolean("female", d.female());
        t.putString("stage", d.lifeStage().id());
        t.putString("id", d.fishId().toString());
        return t;
    }
}
