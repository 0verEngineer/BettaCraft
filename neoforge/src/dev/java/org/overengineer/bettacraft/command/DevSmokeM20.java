package org.overengineer.bettacraft.command;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.reef.ProteinSkimmerBlock;
import org.overengineer.bettacraft.reef.TankReef;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M20 part of the dedicated-server smoke test: a 3x2x2 reef tank filled with sea water, a protein skimmer on its side,
 * actinic + white lights, reef rock with an Acropora and an anemone in the interior, a clownfish pair, a chromis shoal
 * and reef critters. The real care loop runs for the rest of the test; at the end every reef animal is alive, the water
 * is still sea water (saved, loaded, client view), the skimmer counts as a filter, the coral grew and a betta is refused.
 */
final class DevSmokeM20 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static InteriorPlant acropora;

    private DevSmokeM20() {}

    private static BlockPos tankPos(BlockPos base) {
        return base.offset(12, 0, 58);
    }

    static void setup(ServerLevel level, BlockPos base) {
        BlockPos p = tankPos(base);
        for (int cx = (p.getX() - 2) >> 4; cx <= (p.getX() + 5) >> 4; cx++) for (int cz = (p.getZ() - 2) >> 4; cz <= (p.getZ() + 4) >> 4; cz++) level.setChunkForced(cx, cz, true);
        AABB box = new AABB(p).expandTowards(3, 3, 2).inflate(2);
        for (Entity e : level.getEntities((Entity) null, box, e -> !(e instanceof net.minecraft.world.entity.player.Player))) e.discard();
        for (int x = -1; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = -1; z < 3; z++) {
            BlockPos q = p.offset(x, y, z);
            if (!level.getBlockState(q).isAir()) level.setBlockAndUpdate(q, Blocks.AIR.defaultBlockState());
        }
        for (int x = 0; x < 3; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
            level.setBlockAndUpdate(p.offset(x, y, z), ModBlocks.TANK.get().defaultBlockState());
            TankMultiblock.onTankBlockAdded(level, p.offset(x, y, z), null);
        }
        TankBlockEntity c = TankMultiblock.getController(level, p);
        if (c == null) throw new IllegalStateException("M20: tank did not form");
        TankData t = c.getTank();
        // half with sea water buckets' worth, half with fresh water and sea salt: 35 ppt either way
        TankInteractions.changeWater(level, c, 6000, Salinity.SEA);
        TankInteractions.changeWater(level, c, 6000);
        float before = t.salinity();
        t.setSalinity(Salinity.addSalt(before, t.waterMb(), 6));
        t.setCalcium(t.calcium() + Salinity.calciumOf(t.salinity() - before)); // sea salt brings its calcium (as ReefItems does)
        t.setWaterTemp(26F);
        t.setTargetTemp(26F);
        if (Math.abs(t.salinity() - 35F) > 0.1F) throw new IllegalStateException("M20: salinity " + t.salinity());
        for (int x = 0; x < 3; x++) {
            level.setBlockAndUpdate(p.offset(x, 2, 0), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 2).setValue(TankLightBlock.LIT, true)
                    .setValue(TankLightBlock.COLOR, x == 1 ? DyeColor.WHITE : DyeColor.BLUE));
        }
        level.setBlockAndUpdate(p.offset(0, 0, -1), ModBlocks.PROTEIN_SKIMMER.get().defaultBlockState().setValue(ProteinSkimmerBlock.FACING, Direction.SOUTH));
        TankEquipment.invalidate(c);
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        for (int x = 4; x < 12; x++) for (int z = 4; z < 12; z++) in.setBlock(r.toWorld(x, 0, z), ModBlocks.LIVE_ROCK.get().defaultBlockState(), 3);
        for (int x = 24; x < 30; x++) for (int z = 4; z < 10; z++) in.setBlock(r.toWorld(x, 0, z), ModBlocks.REEF_ROCK.get().defaultBlockState(), 3);
        // a flow pump (2x2x2) on the floor at the back
        BlockPos pump = r.toWorld(40, 0, 26);
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
            in.setBlock(pump.offset(x, y, z), ModBlocks.PUMP_SEGMENT.get().defaultBlockState()
                    .setValue(org.overengineer.bettacraft.equipment.SegmentBlock.FACING, Direction.NORTH)
                    .setValue(org.overengineer.bettacraft.equipment.SegmentBlock.PART, y == 1 ? org.overengineer.bettacraft.equipment.SegmentBlock.Part.TOP
                            : x == 0 && z == 0 ? org.overengineer.bettacraft.equipment.SegmentBlock.Part.ANCHOR : org.overengineer.bettacraft.equipment.SegmentBlock.Part.BASE), 3);
        }
        acropora = InteriorPlants.plantAt(in, r, PlantSpecies.ACROPORA, 8, 1, 8, 1, 20L);
        InteriorPlants.plantAt(in, r, PlantSpecies.BUBBLE_TIP_ANEMONE, 26, 1, 6, 3, 21L);
        var w = new org.overengineer.bettacraft.tank.TankWaterVolume(t);
        spawn(level, c, TankmateSpecies.OCELLARIS_CLOWNFISH, true, w);
        spawn(level, c, TankmateSpecies.OCELLARIS_CLOWNFISH, false, w);
        for (int i = 0; i < 5; i++) spawn(level, c, TankmateSpecies.GREEN_CHROMIS, i % 2 == 0, w);
        TankCritters.add(level, c, CritterSpecies.CLEANER_SHRIMP, CritterGenes.WILD, true, 10);
        for (int i = 0; i < 3; i++) TankCritters.add(level, c, CritterSpecies.HERMIT_CRAB, CritterGenes.WILD, i % 2 == 0, 10);
        for (int i = 0; i < 2; i++) TankCritters.add(level, c, CritterSpecies.TURBO_SNAIL, CritterGenes.WILD, i % 2 == 0, 10);
        c.markTankChanged();
        LOGGER.info("[smoketest] M20 reef tank at {}: {} ppt, calcium {}, clownfish pair, 5 chromis, 6 reef critters, Acropora and anemone", p,
                t.salinity(), t.calcium());
    }

    private static void spawn(ServerLevel level, TankBlockEntity c, TankmateSpecies s, boolean female, org.overengineer.bettacraft.tank.TankWaterVolume w) {
        var r = level.getRandom();
        Vec3 pos = w.clamp(new Vec3(w.box().minX + r.nextDouble() * (w.box().maxX - w.box().minX), w.box().minY + 0.4,
                w.box().minZ + r.nextDouble() * (w.box().maxZ - w.box().minZ)), 0.4, 0.1, 0.2);
        TankmateEntity e = TankmateBreeding.spawn(level, c, s, TankmateGenes.wild(s), female, LifeStage.ADULT, 0, pos);
        if (e == null) throw new IllegalStateException("M20: " + s.id() + " not spawned");
        e.health().feed();
    }

    static void check(ServerLevel level, BlockPos base) {
        BlockPos p = tankPos(base);
        TankBlockEntity c = TankMultiblock.getController(level, p);
        if (c == null) throw new IllegalStateException("M20: tank gone");
        TankData t = c.getTank();
        List<TankmateEntity> mates = TankmateBreeding.tankmates(level, t);
        long clowns = mates.stream().filter(m -> m.species() == TankmateSpecies.OCELLARIS_CLOWNFISH).count();
        long chromis = mates.stream().filter(m -> m.species() == TankmateSpecies.GREEN_CHROMIS).count();
        long critters = t.critters().stream().filter(k -> k.alive() && k.species.marine()).count();
        LOGGER.info("[smoketest] M20 {} clownfish, {} chromis, {} reef critters alive; salinity {}, calcium {}, quality {}", clowns, chromis, critters,
                t.salinity(), t.calcium(), t.waterQuality());
        if (clowns != 2 || chromis != 5) throw new IllegalStateException("M20: reef fish missing");
        if (critters != 6) throw new IllegalStateException("M20: reef critters missing: " + critters);
        for (TankmateEntity m : mates) if (m.health().saltShock() > 0) throw new IllegalStateException("M20: salt shock in sea water: " + m.species().id());
        if (!t.marine()) throw new IllegalStateException("M20: not sea water any more: " + t.salinity());
        TankData back = TankData.load(t.save());
        if (Math.abs(back.salinity() - t.salinity()) > 1e-4 || Math.abs(back.calcium() - t.calcium()) > 1e-3) throw new IllegalStateException("M20: salt not saved");
        if (Math.abs(TankData.loadClientView(t.saveClientView()).salinity() - t.salinity()) > 1e-4) throw new IllegalStateException("M20: salt not in the client view");
        if (TankReef.skimmerCount(level, c) != 1 || TankReef.skimmerBlocks(level, c) <= 0) throw new IllegalStateException("M20: skimmer not working");
        if (TankReef.liveRock(level, t) != 64) throw new IllegalStateException("M20: live rock " + TankReef.liveRock(level, t));
        if (TankWater.filterBlocks(level, c) <= 0) throw new IllegalStateException("M20: no filtering in the reef tank");
        if (TankReef.pumps(level, t) != 1 || TankReef.flow(level, c) < 1.0) throw new IllegalStateException("M20: flow pump " + TankReef.pumps(level, t) + ", flow " + TankReef.flow(level, c));
        if (TankReef.spectrum(level, t) < 0.99) throw new IllegalStateException("M20: actinic + white light not seen: " + TankReef.spectrum(level, t));
        // the coral: grow it a few accelerated days (the real loop ran too short for a stage), the anemone is a host
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int i = 0; i < 20; i++) InteriorPlants.grow(level, c, r, 0.5);
        if (acropora == null || acropora.stage < 3) throw new IllegalStateException("M20: Acropora did not grow: " + (acropora == null ? "none" : acropora.stage));
        if (org.overengineer.bettacraft.tankmates.TankmateSpots.anemones(level.getServer(), t).size() != 1) throw new IllegalStateException("M20: no host anemone");
        // a betta is refused, the overlay shows the salt
        Component err = TankInhabitants.insert(level, c, new ItemStack(ModItems.BETTA_BUCKET.get()), ModEntities.BETTA.get(), null, 0L);
        if (err == null) throw new IllegalStateException("M20: a betta went into sea water");
        var info = org.overengineer.bettacraft.compat.info.InfoData.tank(level, p);
        if (info == null || info.getFloatOr("salinity", 0F) < 30) throw new IllegalStateException("M20: the overlay does not know the salt");
        // trader offers
        var bag = TankCritters.bagOf(CritterSpecies.HERMIT_CRAB, 3, () -> CritterGenes.WILD, new java.util.Random(3)::nextLong, false);
        if (TankCritters.bagged(bag) == null || TankCritters.bagged(bag).size() != 3) throw new IllegalStateException("M20: hermit crab bag");
        LOGGER.info("[smoketest] M20 checks passed");
    }
}
