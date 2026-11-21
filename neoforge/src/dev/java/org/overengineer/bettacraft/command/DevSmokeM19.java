package org.overengineer.bettacraft.command;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateCare;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tankmates.Clutch;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateItems;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M19 part of the dedicated-server smoke test: a 3x2x2 tank with a shoal of six cardinals (from one fish bag), six
 * sterbai corys and a bristlenose, a clutch of cory eggs and a breeding box; a second tank with a guppy pair whose
 * breeding runs ten accelerated days in the check (the level's random seeded). The real care loop runs for the rest of the test; at the end everybody is alive
 * in the tank without shoal stress, the stocking adds up, the overlay lists them, the clutch and the box survive saving
 * (schema 9) and reach the client view, and the guppies have young.
 */
final class DevSmokeM19 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private DevSmokeM19() {}

    private static BlockPos tankPos(BlockPos base) {
        return base.offset(0, 0, 58);
    }

    private static TankBlockEntity tank(ServerLevel level, BlockPos p, int sx, int sy, int sz, float temp) {
        AABB box = new AABB(p).expandTowards(sx, sy + 1, sz).inflate(2);
        for (Entity e : level.getEntities((Entity) null, box, e -> !(e instanceof net.minecraft.world.entity.player.Player))) e.discard();
        for (int x = 0; x < sx; x++) for (int y = 0; y < sy + 1; y++) for (int z = 0; z < sz; z++) {
            BlockPos q = p.offset(x, y, z);
            if (!level.getBlockState(q).isAir()) level.setBlockAndUpdate(q, Blocks.AIR.defaultBlockState());
        }
        for (int x = 0; x < sx; x++) for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) {
            level.setBlockAndUpdate(p.offset(x, y, z), ModBlocks.TANK.get().defaultBlockState());
            TankMultiblock.onTankBlockAdded(level, p.offset(x, y, z), null);
        }
        TankBlockEntity c = TankMultiblock.getController(level, p);
        if (c == null) throw new IllegalStateException("M19: tank did not form");
        TankInteractions.changeWater(level, c, c.getTank().capacityMb() - c.getTank().waterMb());
        c.getTank().setWaterTemp(temp);
        c.getTank().setTargetTemp(temp);
        return c;
    }

    static void setup(ServerLevel level, BlockPos base) {
        BlockPos p = tankPos(base);
        idsFrom = net.minecraft.world.entity.EntityType.MARKER.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND).getId();
        // beyond the spawn chunks: keep the chunks ticking (the animals live only in ticking chunks)
        for (int cx = (p.getX() - 2) >> 4; cx <= (p.getX() + 9) >> 4; cx++) for (int cz = (p.getZ() - 2) >> 4; cz <= (p.getZ() + 4) >> 4; cz++) level.setChunkForced(cx, cz, true);
        TankBlockEntity c = tank(level, p, 3, 2, 2, 27F);
        TankData t = c.getTank();
        // six cardinals from one bag (the way a player puts them in), four corys, a bristlenose
        var bag = TankmateItems.groupBag(TankmateSpecies.CARDINAL_TETRA, 6, () -> TankmateGenes.wild(TankmateSpecies.CARDINAL_TETRA),
                new java.util.Random(19)::nextLong, true);
        for (CompoundTag f : TankmateItems.bagFish(bag)) {
            var single = TankmateItems.groupBag(TankmateSpecies.CARDINAL_TETRA, List.of(f));
            Component err = TankInhabitants.insert(level, c, single, ModEntities.TANKMATES.get(TankmateSpecies.CARDINAL_TETRA).get(), null, 0L);
            if (err != null) throw new IllegalStateException("M19: cardinal not inserted: " + err.getString());
        }
        var w = new org.overengineer.bettacraft.tank.TankWaterVolume(t);
        for (int i = 0; i < 6; i++) spawn(level, c, TankmateSpecies.STERBAI_CORY, TankmateGenes.wild(TankmateSpecies.STERBAI_CORY), i % 2 == 0, w);
        spawn(level, c, TankmateSpecies.BRISTLENOSE_PLECO, TankmateGenes.pure(TankmateSpecies.BRISTLENOSE_PLECO, "albino"), false, w);
        Clutch k = new Clutch(t.nextClutchId(), TankmateSpecies.STERBAI_CORY, Clutch.Kind.GLASS, p.getX() + 1.5, p.getY() + 0.5, p.getZ() + 1.0 / 16 + 0.004,
                12, TankmateGenes.wild(TankmateSpecies.STERBAI_CORY), TankmateGenes.wild(TankmateSpecies.STERBAI_CORY));
        k.face = 0;
        t.clutches().add(k);
        t.setBreedingBox(true);
        c.markTankChanged();
        // guppies: a few accelerated days of breeding (in the check)
        TankBlockEntity g = tank(level, p.offset(5, 0, 0), 2, 2, 2, 26F);
        var wg = new org.overengineer.bettacraft.tank.TankWaterVolume(g.getTank());
        spawn(level, g, TankmateSpecies.GUPPY, TankmateGenes.pure(TankmateSpecies.GUPPY, "blue_grass"), true, wg);
        spawn(level, g, TankmateSpecies.GUPPY, TankmateGenes.pure(TankmateSpecies.GUPPY, "blue_grass"), false, wg);
        LOGGER.info("[smoketest] M19 tanks at {}: 6 cardinals, 6 sterbai corys, an albino bristlenose, a clutch, a breeding box; guppies {}", p,
                TankmateBreeding.tankmates(level, g.getTank()).size());
        idsTo = net.minecraft.world.entity.EntityType.MARKER.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND).getId();
    }

    /**
     * M55: the fish of this run have the entity ids handed out during the set-up. Those of an earlier run in the saved dev
     * world load with the forced chunks after the set-up (it could not send them away; they added up run by run): the check
     * sends them away first.
     */
    private static int idsFrom, idsTo;

    private static void spawn(ServerLevel level, TankBlockEntity c, TankmateSpecies s, TankmateGenes genes, boolean female,
                              org.overengineer.bettacraft.tank.TankWaterVolume w) {
        var r = level.getRandom();
        Vec3 pos = w.clamp(new Vec3(w.box().minX + r.nextDouble() * (w.box().maxX - w.box().minX), w.box().minY + 0.3,
                w.box().minZ + r.nextDouble() * (w.box().maxZ - w.box().minZ)), 0.4, 0.1, 0.2);
        TankmateEntity e = TankmateBreeding.spawn(level, c, s, genes, female, LifeStage.ADULT, 0, pos);
        if (e == null) throw new IllegalStateException("M19: " + s.id() + " not spawned");
        e.health().feed();
    }

    static void check(ServerLevel level, BlockPos base) {
        BlockPos p = tankPos(base);
        TankBlockEntity c = TankMultiblock.getController(level, p);
        if (c == null) throw new IllegalStateException("M19: tank gone");
        TankData t = c.getTank();
        for (TankmateEntity m : TankmateBreeding.tankmates(level, t)) {
            if (m.getId() < idsFrom || m.getId() > idsTo) {
                LOGGER.info("[smoketest] M19 a fish of an earlier run loaded late: {}", m);
                m.discard();
            }
        }
        List<TankmateEntity> mates = TankmateBreeding.tankmates(level, t);
        long cardinals = mates.stream().filter(m -> m.species() == TankmateSpecies.CARDINAL_TETRA).count();
        long corys = mates.stream().filter(m -> m.species() == TankmateSpecies.STERBAI_CORY && m.isAdult()).count();
        long plecos = mates.stream().filter(m -> m.species() == TankmateSpecies.BRISTLENOSE_PLECO).count();
        LOGGER.info("[smoketest] M19 {} cardinals, {} corys, {} plecos alive; clutches {}, box {}", cardinals, corys, plecos, t.clutches().size(), t.breedingBox());
        if (cardinals != 6 || corys != 6 || plecos != 1) throw new IllegalStateException("M19: tankmates missing");
        for (TankmateEntity m : mates) {
            if (!t.contains(m.blockPosition()) && !t.contains(m.blockPosition().below())) throw new IllegalStateException("M19: " + m.species().id() + " left the tank at " + m.position());
            if (!m.isInTank()) throw new IllegalStateException("M19: " + m.species().id() + " does not know its tank");
            if ((m.stressBits() & TankmateCare.STRESS_SHOAL) != 0) throw new IllegalStateException("M19: a full shoal is stressed: " + m.species().id());
        }
        if (!"albino".equals(mates.stream().filter(m -> m.species() == TankmateSpecies.BRISTLENOSE_PLECO).findFirst().orElseThrow().variety())) {
            throw new IllegalStateException("M19: the albino lost its colour");
        }
        double stock = TankInhabitants.stocking(level, t);
        double want = 6 * TankmateSpecies.CARDINAL_TETRA.stock() + 6 * TankmateSpecies.STERBAI_CORY.stock() + TankmateSpecies.BRISTLENOSE_PLECO.stock();
        LOGGER.info("[smoketest] M19 stocking {} (adults alone {})", stock, want);
        if (stock < want - 1e-6) throw new IllegalStateException("M19: stocking " + stock + " below " + want);
        // the clutch and the box: saved (schema 9), loaded, in the client view
        if (t.clutches().isEmpty() && !TankmateBreeding.tankmates(level, t).stream().anyMatch(m -> m.lifeStage() == LifeStage.FRY)) {
            throw new IllegalStateException("M19: the clutch is gone without young");
        }
        TankData back = TankData.load(t.save());
        if (back.clutches().size() != t.clutches().size() || !back.breedingBox()) throw new IllegalStateException("M19: clutches or box not saved");
        TankData view = TankData.loadClientView(t.saveClientView());
        if (view.clutches().size() != t.clutches().size() || !view.breedingBox()) throw new IllegalStateException("M19: clutches or box not in the client view");
        var info = org.overengineer.bettacraft.compat.info.InfoData.tank(level, p);
        if (info == null || info.getListOrEmpty("tankmates").size() != 3) throw new IllegalStateException("M19: the overlay does not list the three species");
        // the guppies bred
        TankBlockEntity g = TankMultiblock.getController(level, p.offset(5, 0, 0));
        // M52.5: the ten accelerated days run here, not in the setup: right after spawning, the guppies' chunk did not list its
        // entities yet now and then (CI: "guppies 0" after the setup, so nobody bred); the level's random seeded
        if (g != null) {
            level.getRandom().setSeed(1919L);
            for (int i = 0; i < 40; i++) {
                for (TankmateEntity m : TankmateBreeding.tankmates(level, g.getTank())) {
                    m.health().feed();
                    TankmateCare.advance(level, g, m, 0.25);
                }
                g.getTank().setWaterTemp(26F);
                TankmateBreeding.step(level, g, 0.25);
            }
        }
        // (the first young are grown up after ten days)
        long all = g == null ? 0 : TankmateBreeding.tankmates(level, g.getTank()).size();
        long young = g == null ? 0 : TankmateBreeding.tankmates(level, g.getTank()).stream().filter(m -> m.generation() > 0).count();
        LOGGER.info("[smoketest] M19 guppies: {} ({} born in the tank)", all, young);
        if (young == 0) throw new IllegalStateException("M19: the guppies did not breed in 10 accelerated days");
        for (TankmateEntity m : TankmateBreeding.tankmates(level, g.getTank())) {
            if (!"blue_grass".equals(m.variety())) throw new IllegalStateException("M19: blue grass x blue grass gave " + m.variety());
        }
        // trader offers
        var offer = org.overengineer.bettacraft.world.TankmateTrades.offer(TankmateSpecies.PANDA_CORY, new java.util.Random(5)::nextLong);
        if (TankmateItems.bagFish(offer.getResult()).size() != 6) throw new IllegalStateException("M19: trader shoal");
        LOGGER.info("[smoketest] M19 checks passed");
    }
}
