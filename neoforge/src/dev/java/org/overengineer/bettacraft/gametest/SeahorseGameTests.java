package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateCare;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.food.BettaFood;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tankmates.Seahorses;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateFeeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M21.8 GameTests: seahorses. They find hitching posts (decoration sticking up in the interior) and curl their tail round
 * one, are stressed without a post and in strong flow (a flow pump), eat only live food and artemia (the fry from birth)
 * and need it more often, are harassed by tangs but not by gobies, and the male carries the eggs in his brood pouch and
 * gives birth to fry.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SeahorseGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private SeahorseGameTests() {}

    // ------------------------------------------------------------------ helpers

    static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz, long water, float salt, float temp) {
        for (int a = 0; a < sx; a++)
            for (int b = 0; b < sy; b++)
                for (int c = 0; c < sz; c++) {
                    BlockPos p = new BlockPos(x + a, y + b, z + c);
                    h.setBlock(p, ModBlocks.TANK.get().defaultBlockState());
                    TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(p), null);
                }
        TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(new BlockPos(x, y, z)));
        if (c == null) throw new GameTestAssertException("tank did not form");
        if (water > 0) TankInteractions.changeWater(h.getLevel(), c, water, salt);
        c.getTank().setWaterTemp(temp);
        c.getTank().setTargetTemp(temp);
        return c;
    }

    static TankmateEntity add(GameTestHelper h, TankBlockEntity c, TankmateSpecies s, boolean female, LifeStage stage) {
        var w = new TankWaterVolume(c.getTank());
        Vec3 p = w.clamp(new Vec3((w.box().minX + w.box().maxX) / 2, w.box().minY + 0.3, (w.box().minZ + w.box().maxZ) / 2), 0.35, 0.1, 0.7);
        TankmateEntity e = TankmateBreeding.spawn(h.getLevel(), c, s, TankmateGenes.wild(s), female, stage, 0, p);
        if (e == null) throw new GameTestAssertException("could not spawn " + s.id());
        e.health().feed();
        return e;
    }

    private static List<TankmateEntity> mates(GameTestHelper h, TankBlockEntity c) {
        List<TankmateEntity> out = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(h.getLevel(), c.getTank())) if (e instanceof TankmateEntity m && m.isAlive()) out.add(m);
        return out;
    }

    private static boolean logged(TankData t, String key) {
        return t.log().stream().anyMatch(e -> e.key().equals(key));
    }

    private static InteriorRegion region(GameTestHelper h, TankBlockEntity c) {
        if (InteriorManager.interiorLevel(h.getLevel().getServer()) == null) throw new GameTestAssertException("no interior dimension");
        return InteriorManager.ensureRegion(h.getLevel(), c);
    }

    /** A stone pillar in the interior (a hitching post), {@code w} x {@code w} interior blocks, {@code height} tall. */
    static void pillar(GameTestHelper h, InteriorRegion r, int lx, int lz, int w, int height) {
        ServerLevel in = InteriorManager.interiorLevel(h.getLevel().getServer());
        for (int x = 0; x < w; x++) for (int z = 0; z < w; z++) for (int y = 0; y < height; y++) in.setBlock(r.toWorld(lx + x, y, lz + z), Blocks.STONE.defaultBlockState(), 3);
    }

    /**
     * M22.5: loads the interior chunks of the region and keeps them loaded (the interior ticket, renewed on every call).
     * Without a player in the interior nothing holds them: placed blocks loaded them for a moment, and when they had
     * unloaded again before the posts were looked up, the height map saw an empty floor (no posts; failed 2 of 30 runs).
     */
    static void keepLoaded(GameTestHelper h, InteriorRegion r) {
        ServerLevel in = InteriorManager.interiorLevel(h.getLevel().getServer());
        for (int cx = (r.originX() - 1) >> 4; cx <= (r.originX() + r.sizeX()) >> 4; cx++) {
            for (int cz = (r.originZ() - 1) >> 4; cz <= (r.originZ() + r.sizeZ()) >> 4; cz++) {
                net.minecraft.world.level.ChunkPos cp = new net.minecraft.world.level.ChunkPos(cx, cz);
                in.getChunkSource().addRegionTicket(org.overengineer.bettacraft.tank.interior.InteriorPreparation.TICKET, cp, 0, cp);
                in.getChunk(cx, cz);
            }
        }
    }

    // ------------------------------------------------------------------ hitching posts

    /**
     * A seahorse finds the pillars of the interior as hitching posts, swims to one and holds on to it (curled tail); in a
     * tank without decoration it is stressed (nothing to hold on to).
     */
    @GameTest(template = EMPTY, timeoutTicks = 900)
    public static void seahorsesHitchToPosts(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 2, 2, 3, 2, 3, 18000, 33F, 23F);
        InteriorRegion r = region(h, c);
        pillar(h, r, 8, 8, 3, 12);
        pillar(h, r, 34, 30, 3, 10);
        keepLoaded(h, r);
        TankBlockEntity bare = tank(h, 10, 2, 2, 2, 2, 2, 8000, 33F, 23F);
        TankmateEntity sh = add(h, c, TankmateSpecies.LINED_SEAHORSE, false, LifeStage.ADULT);
        TankmateEntity lonely = add(h, bare, TankmateSpecies.LINED_SEAHORSE, true, LifeStage.ADULT);
        h.runAfterDelay(30, () -> {
            keepLoaded(h, r);
            Seahorses.forget();
            var spots = Seahorses.spots(level, c.getTank());
            h.assertTrue(!spots.isEmpty(), "the pillars are hitching posts");
            h.assertTrue(Seahorses.spots(level, bare.getTank()).isEmpty(), "no posts in the bare tank");
            TankmateCare.advance(level, c, sh, 0.05);
            TankmateCare.advance(level, bare, lonely, 0.05);
            h.assertTrue((sh.stressBits() & TankmateCare.STRESS_NO_POSTS) == 0, "posts: no stress " + sh.stressBits());
            h.assertTrue((lonely.stressBits() & TankmateCare.STRESS_NO_POSTS) != 0, "nothing to hold on to: stressed " + lonely.stressBits());
            h.assertTrue(sh.hitchNowForTest(), "swims to a post");
            h.assertTrue(sh.hitchPost() != null, "has a post");
            h.succeedWhen(() -> {
                keepLoaded(h, r);
                h.assertTrue(sh.isHitched(), "holds on to the post at " + sh.position() + " (target " + sh.behaviourTarget() + ")");
                h.assertTrue(sh.flag(TankmateEntity.FLAG_REST), "tail curled (synced flag)");
                Vec3 post = sh.hitchPost();
                h.assertTrue(post != null && Math.hypot(post.x - sh.getX(), post.z - sh.getZ()) < 0.4, "right beside the post");
            });
        });
    }

    /** Strong flow (a flow pump in a small tank) stresses seahorses; the same tank without the pump does not. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void strongFlowStressesSeahorses(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 1, 1, 2000, 33F, 23F);
        TankBlockEntity calm = tank(h, 2, 1, 8, 2, 1, 1, 2000, 33F, 23F);
        InteriorRegion r = region(h, c);
        region(h, calm);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        sp.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        try {
            sp.teleportTo(in, r.toWorld(2, 0, 2).getX() + 0.5, r.toWorld(2, 0, 2).getY(), r.toWorld(2, 0, 2).getZ() + 0.5, 0F, 0F);
            BlockPos at = r.toWorld(20, 0, 8).below();
            ItemStack pump = new ItemStack(ModItems.FLOW_PUMP.get());
            sp.setItemInHand(InteractionHand.MAIN_HAND, pump);
            sp.gameMode.useItemOn(sp, in, pump, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(at).add(0, 0.5, 0), Direction.UP, at, false));
        } finally {
            sp.getServer().getPlayerList().remove(sp);
        }
        TankmateEntity a = add(h, c, TankmateSpecies.DWARF_SEAHORSE, false, LifeStage.ADULT);
        TankmateEntity b = add(h, calm, TankmateSpecies.DWARF_SEAHORSE, false, LifeStage.ADULT);
        h.runAfterDelay(20, () -> {
            Seahorses.forget();
            double flow = Seahorses.flow(level, c);
            h.assertTrue(Seahorses.flowTooStrong(flow), "a pump in 2 blocks of water is too strong: " + flow);
            h.assertTrue(!Seahorses.flowTooStrong(Seahorses.flow(level, calm)), "no pump: calm water " + Seahorses.flow(level, calm));
            double before = a.health().crowdStress();
            TankmateCare.advance(level, c, a, 0.2);
            TankmateCare.advance(level, calm, b, 0.2);
            h.assertTrue((a.stressBits() & TankmateCare.STRESS_FLOW) != 0, "stressed by the current: " + a.stressBits());
            h.assertTrue(a.health().crowdStress() > before, "the stress counts");
            h.assertTrue((b.stressBits() & TankmateCare.STRESS_FLOW) == 0, "calm tank: no flow stress " + b.stressBits());
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ food

    /** Seahorses eat live food and artemia (the fry from birth), no pellets, wafers or infusoria; they get hungry sooner. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void seahorsesEatLiveFoodOnly(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 1, 2, 3, 2, 3, 18000, 33F, 23F);
        TankmateEntity adult = add(h, c, TankmateSpecies.LINED_SEAHORSE, true, LifeStage.ADULT);
        TankmateEntity fry = add(h, c, TankmateSpecies.LINED_SEAHORSE, false, LifeStage.FRY);
        TankData t = c.getTank();
        h.assertTrue(TankmateFeeding.eaters(level, t, BettaFood.Kind.NORMAL, false).isEmpty(), "no pellets");
        h.assertTrue(TankmateFeeding.eaters(level, t, BettaFood.Kind.NORMAL, true).isEmpty(), "no algae wafers");
        h.assertTrue(TankmateFeeding.eaters(level, t, BettaFood.Kind.FRY_START, false).isEmpty(), "no infusoria");
        var live = TankmateFeeding.eaters(level, t, BettaFood.Kind.LIVE, false);
        h.assertTrue(live.contains(adult) && !live.contains(fry), "live food for the adult");
        var artemia = TankmateFeeding.eaters(level, t, BettaFood.Kind.FRY, false);
        h.assertTrue(artemia.contains(adult) && artemia.contains(fry), "artemia nauplii for both, the fry from birth");
        // pellets by hand: nobody eats them; nauplii: eaten
        h.assertTrue(TankCare.feed(level, c, new ItemStack(ModItems.BETTA_PELLETS.get()), 1) == 0, "pellets are left");
        adult.health().feed();
        TankmateCare.advance(level, c, adult, 0.6);
        h.assertTrue(adult.health().daysSinceFed() >= 0.59, "time passed: " + adult.health().daysSinceFed());
        h.assertTrue(!adult.health().isHungry(org.overengineer.bettacraft.entity.betta.BettaCare.params()), "a betta would not be hungry yet");
        h.assertTrue(adult.health().isHungry(Seahorses.params(org.overengineer.bettacraft.entity.betta.BettaCare.params())), "a seahorse is (twice a day)");
        h.assertTrue(TankCare.feed(level, c, new ItemStack(ModItems.ARTEMIA_NAUPLII.get()), 1) == 1, "artemia eaten");
        h.assertTrue(adult.health().daysSinceFed() < 1e-6 && fry.health().daysSinceFed() < 1e-6, "both fed");
        h.succeed();
    }

    // ------------------------------------------------------------------ tankmates

    /** A yellow tang harasses the seahorses (chased, logged); a watchman goby does not. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void fastFishHarassSeahorses(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 1, 2, 3, 2, 3, 18000, 35F, 25F);
        TankmateEntity sh = add(h, c, TankmateSpecies.LINED_SEAHORSE, false, LifeStage.ADULT);
        add(h, c, TankmateSpecies.YELLOW_TANG, false, LifeStage.ADULT);
        TankBlockEntity calm = tank(h, 12, 1, 2, 3, 2, 3, 18000, 35F, 25F);
        TankmateEntity sh2 = add(h, calm, TankmateSpecies.LINED_SEAHORSE, false, LifeStage.ADULT);
        add(h, calm, TankmateSpecies.YELLOW_WATCHMAN_GOBY, false, LifeStage.ADULT);
        TankmateBreeding.step(level, c, 0.05);
        TankmateBreeding.step(level, calm, 0.05);
        h.assertTrue((sh.stressBits() & TankmateCare.STRESS_CHASED) != 0, "the tang harasses the seahorse");
        h.assertTrue(logged(c.getTank(), "log.bettacraft.tm.seahorse_harassed"), "logged");
        h.assertTrue((sh2.stressBits() & TankmateCare.STRESS_CHASED) == 0, "the goby leaves it alone");
        h.succeed();
    }

    // ------------------------------------------------------------------ breeding

    /**
     * A fit pair: the female lays her eggs into the male's brood pouch (he is the one who is 'pregnant', with her genes),
     * after the pouch days he gives birth to seahorse fry; the female never carries.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void maleCarriesTheEggsInHisBroodPouch(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 1, 2, 3, 2, 3, 18000, 33F, 23F);
        TankmateEntity f = add(h, c, TankmateSpecies.LINED_SEAHORSE, true, LifeStage.ADULT);
        TankmateEntity m = add(h, c, TankmateSpecies.LINED_SEAHORSE, false, LifeStage.ADULT);
        f.genes();
        for (int i = 0; i < 120 && !m.isGravid(); i++) {
            for (TankmateEntity x : List.of(f, m)) {
                x.health().feed();
                x.breeding().condition();
                TankmateCare.advance(level, c, x, 0.1);
            }
            c.getTank().setWaterQuality(1F);
            TankmateBreeding.step(level, c, 0.1);
        }
        h.assertTrue(m.isGravid(), "the male carries the eggs");
        h.assertTrue(!f.isGravid(), "not the female");
        h.assertTrue(m.gravidFather() != null && m.gravidFather().text().equals(f.genes().text()), "with the female's genes");
        h.assertTrue(logged(c.getTank(), "log.bettacraft.tm.pouch"), "logged");
        int before = mates(h, c).size();
        var watcher = QuestAdv.watcher(h, c); // M45
        m.setGravid(Seahorses.pouchDays() + 0.01, m.gravidFather());
        TankmateBreeding.step(level, c, 0.01);
        QuestAdv.check(h, watcher, "seahorse_fry");
        QuestAdv.gone(h, watcher);
        List<TankmateEntity> all = mates(h, c);
        long fry = all.stream().filter(x -> x.species() == TankmateSpecies.LINED_SEAHORSE && x.lifeStage() == LifeStage.FRY).count();
        h.assertTrue(!m.isGravid(), "the pouch is empty again");
        h.assertTrue(fry >= TankmateSpecies.LINED_SEAHORSE.broodMin() && all.size() > before, "fry born: " + fry);
        h.assertTrue(logged(c.getTank(), "log.bettacraft.tm.pouch_born"), "birth logged");
        h.assertTrue(c.getTank().clutches().isEmpty(), "live fry, no clutch or breeding box");
        h.succeed();
    }

    /** Seahorses tolerate strong brackish water but no fresh water; they are refused in a freshwater tank. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void seahorsesLiveInSeaAndBrackishWater(GameTestHelper h) {
        TankBlockEntity fresh = tank(h, 2, 1, 2, 2, 1, 2, 4000, 0F, 23F);
        TankBlockEntity brackish = tank(h, 8, 1, 2, 2, 1, 2, 4000, 22F, 23F);
        ItemStack bucket = new ItemStack(ModItems.TANKMATE_BUCKETS.get(TankmateSpecies.DWARF_SEAHORSE).get());
        var type = TankInhabitants.containedType(bucket);
        h.assertTrue(type != null, "a seahorse bucket");
        var msg = TankInhabitants.insert(h.getLevel(), fresh, bucket.copy(), type, null, 0L);
        h.assertTrue(msg != null && mates(h, fresh).isEmpty(), "refused in fresh water: " + msg);
        TankInhabitants.insert(h.getLevel(), brackish, bucket.copy(), type, null, 0L);
        h.assertTrue(mates(h, brackish).size() == 1, "fine at 22 permille");
        h.assertTrue(TankmateSpecies.DWARF_SEAHORSE.salinity().contains(22) && !TankmateSpecies.DWARF_SEAHORSE.salinity().contains(10), "range");
        h.succeed();
    }
}
