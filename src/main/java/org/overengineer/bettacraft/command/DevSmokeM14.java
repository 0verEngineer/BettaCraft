package org.overengineer.bettacraft.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.CritterSync;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.entity.frog.DwarfFrogEntity;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankFrogs;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.algae.AlgaeSim;
import org.overengineer.bettacraft.tank.algae.TankAlgae;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M14 part of the dedicated-server smoke test: a 2x1x2 tank under a lit light with Neocaridina, nerites, an apple snail
 * pair, a ramshorn, algae on the glass and a dwarf clawed frog. The real care loop runs for the rest of the test (the
 * animals walk, the frog breathes); then accelerated days: the nerites clean the glass, the shrimp breed. The tank saves
 * as schema 8 and loads its animals and algae again, the client view round-trips, the overlay data counts them, items
 * and the trader's offers are there.
 */
final class DevSmokeM14 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static final Map<Integer, double[]> START = new HashMap<>();

    private DevSmokeM14() {}

    private static BlockPos tankPos(BlockPos base) {
        return base.offset(0, 0, 32);
    }

    static void setup(ServerLevel level, BlockPos base) {
        BlockPos p = tankPos(base);
        // frogs of an earlier (failed) run
        for (var e : level.getEntitiesOfClass(DwarfFrogEntity.class, new net.minecraft.world.phys.AABB(p).inflate(4))) e.discard();
        for (int x = 0; x < 2; x++)
            for (int z = 0; z < 2; z++) {
                for (int y = 0; y < 2; y++) {
                    BlockPos q = p.offset(x, y, z);
                    if (!level.getBlockState(q).isAir()) level.setBlockAndUpdate(q, Blocks.AIR.defaultBlockState());
                }
            }
        for (int x = 0; x < 2; x++)
            for (int z = 0; z < 2; z++) {
                level.setBlockAndUpdate(p.offset(x, 0, z), ModBlocks.TANK.get().defaultBlockState());
                TankMultiblock.onTankBlockAdded(level, p.offset(x, 0, z), null);
            }
        TankBlockEntity c = TankMultiblock.getController(level, p);
        if (c == null) throw new IllegalStateException("M14: tank did not form");
        TankInteractions.changeWater(level, c, 4000);
        c.getTank().setWaterTemp(25F);
        c.getTank().setTargetTemp(25F);
        for (int x = 0; x < 2; x++) {
            level.setBlockAndUpdate(p.offset(x, 1, 0), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.LIT, true));
        }
        TankCritters.add(level, c, CritterSpecies.NEOCARIDINA, CritterGenes.pure(0, 0.5F), true, 5);
        TankCritters.add(level, c, CritterSpecies.NEOCARIDINA, CritterGenes.pure(0, 0.5F), false, 5);
        TankCritters.add(level, c, CritterSpecies.ZEBRA_NERITE, CritterGenes.WILD, true, 5);
        TankCritters.add(level, c, CritterSpecies.OLIVE_NERITE, CritterGenes.WILD, true, 5);
        TankCritters.add(level, c, CritterSpecies.APPLE_SNAIL, CritterGenes.pure(0, 0.4F), true, 6);
        TankCritters.add(level, c, CritterSpecies.APPLE_SNAIL, CritterGenes.pure(1, 0.4F), false, 6);
        TankCritters.add(level, c, CritterSpecies.RAMSHORN, CritterGenes.pure(0, 0.4F), true, 5);
        for (var f : TankAlgae.wetFaces(c.getTank())) AlgaeSim.set(c.getTank().algae(), f.pos(), f.dir(), 0.8F);
        DwarfFrogEntity frog = ModEntities.DWARF_FROG.get().create(level);
        ItemStack bag = FishBagItem.fillWith(frog);
        frog.discard();
        var err = TankInhabitants.insert(level, c, bag, ModEntities.DWARF_FROG.get(), null, 0L);
        if (err != null) throw new IllegalStateException("M14: frog: " + err.getString());
        START.clear();
        for (Critter k : c.getTank().critters()) START.put(k.id, new double[]{k.x, k.y, k.z});
    }

    static void check(ServerLevel level, BlockPos base) {
        TankBlockEntity c = TankMultiblock.getController(level, tankPos(base));
        if (c == null) throw new IllegalStateException("M14: no tank");
        TankData t = c.getTank();
        if (TankCritters.alive(t) != 7) throw new IllegalStateException("M14: animals lost: " + t.critters().size());
        int moved = 0;
        for (Critter k : t.critters()) {
            if (!t.contains(BlockPos.containing(k.x, k.y + 0.01, k.z))) throw new IllegalStateException("M14: animal outside the tank: " + k.debug());
            double[] s = START.get(k.id);
            if (s != null && Math.abs(s[0] - k.x) + Math.abs(s[2] - k.z) + Math.abs(s[1] - k.y) > 1e-3) moved++;
        }
        if (moved == 0) throw new IllegalStateException("M14: nobody moved in the real care loop");
        List<DwarfFrogEntity> frogs = TankFrogs.frogs(level, t);
        if (frogs.size() != 1 || frogs.getFirst().breath() <= 0) throw new IllegalStateException("M14: frog " + frogs);
        if (!TankFrogs.hasAir(t)) throw new IllegalStateException("M14: no air gap");
        // a few accelerated days: the nerites eat the algae, the shrimp breed
        double algae0 = TankAlgae.mean(t);
        for (int i = 0; i < 6 * 24; i++) {
            t.setCritterFood(20);
            t.setWaterTemp(25F);
            TankAlgae.step(level, c, 1.0 / 24);
            TankCritters.step(level, c, 1.0 / 24);
        }
        double algae1 = TankAlgae.mean(t);
        if (algae1 >= algae0 * 0.5) throw new IllegalStateException("M14: the nerites did not clean the glass: " + algae0 + " -> " + algae1);
        long shrimp = t.critters().stream().filter(k -> k.alive() && k.species == CritterSpecies.NEOCARIDINA).count();
        // save / load
        CompoundTag saved = c.saveWithFullMetadata(level.registryAccess());
        // M19: the current schema (8 when M14 was written, 9 since the tankmate clutches)
        if (saved.getInt("schema") != org.overengineer.bettacraft.data.schema.Schemas.TANK_BLOCK_ENTITY.currentVersion()) throw new IllegalStateException("M14: tank saved as schema " + saved.getInt("schema"));
        TankData back = TankData.load(saved.getCompound("tank"));
        if (back.critters().size() != t.critters().size() || back.critterObjects().size() != t.critterObjects().size()) {
            throw new IllegalStateException("M14: save/load lost animals");
        }
        var dec = CritterSync.decode(CritterSync.encode(t.critters(), t.critterObjects(), t.min().getX(), t.min().getY(), t.min().getZ(), 0),
                t.min().getX(), t.min().getY(), t.min().getZ());
        if (dec.critters().size() != t.critters().size()) throw new IllegalStateException("M14: client format");
        var info = org.overengineer.bettacraft.compat.info.InfoData.tank(level, tankPos(base));
        int[] counts = info.getIntArray("critters");
        if (counts.length != 4 || counts[0] + counts[2] < 7) throw new IllegalStateException("M14: overlay counts " + java.util.Arrays.toString(counts));
        if (ModItems.ALGAE_SCRAPER.get() == null || ModItems.FROG_BUCKET.get() == null) throw new IllegalStateException("M14: items");
        var trader = EntityType.WANDERING_TRADER.create(level);
        List<CritterSpecies> offered = new ArrayList<>();
        for (int lv = 1; lv <= 2; lv++) {
            for (VillagerTrades.ItemListing l : VillagerTrades.WANDERING_TRADER_TRADES.get(lv)) {
                var offer = l.getOffer(trader, level.getRandom());
                if (offer == null) continue;
                var animals = TankCritters.bagged(offer.getResult());
                if (animals != null) offered.add(Critter.loadAnimal(animals.getFirst(), 0, 0, 0, 0).species);
            }
        }
        if (trader != null) trader.discard();
        if (!offered.contains(CritterSpecies.DWARF_CRAYFISH) || !offered.contains(CritterSpecies.NEOCARIDINA)) {
            throw new IllegalStateException("M14: trades " + offered);
        }
        LOGGER.info("[smoketest] M14: {} small animals ({} shrimp after 6 days), {} moved in the real care loop, algae {} -> {}, frog breath {}, "
                        + "saved as schema 8, trades {}", TankCritters.alive(t), shrimp, moved, Math.round(algae0 * 100), Math.round(algae1 * 100),
                frogs.getFirst().breath(), offered);
        // leave the tank for the next run without living animals (the break rule would keep it otherwise)
        t.critters().clear();
        t.critterObjects().clear();
        for (var f : frogs) f.discard();
        c.markTankChanged();
    }
}
