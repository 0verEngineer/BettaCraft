package org.overengineer.bettacraft.command;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.level.block.Blocks;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M13 part of the dedicated-server smoke test: a 2x1x1 tank under a lit light gets a gravel floor, a Rotala cutting and
 * a Java fern on a stone; the real care loop runs for the rest of the test, then a few accelerated days on the plant
 * index; the plants must have grown with their blocks in the interior, the region saves its plant index (schema 4),
 * items and trades are there and the menu data of the plants page builds.
 */
final class DevSmokeM13 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private DevSmokeM13() {}

    private static BlockPos tankPos(BlockPos base) {
        return base.offset(0, 0, 28);
    }

    static void setup(ServerLevel level, BlockPos base) {
        BlockPos p = tankPos(base);
        for (int x = 0; x < 2; x++) {
            BlockPos q = p.offset(x, 0, 0);
            if (!level.getBlockState(q).isAir()) level.setBlockAndUpdate(q, Blocks.AIR.defaultBlockState());
            if (!level.getBlockState(q.above()).isAir()) level.setBlockAndUpdate(q.above(), Blocks.AIR.defaultBlockState());
        }
        for (int x = 0; x < 2; x++) {
            level.setBlockAndUpdate(p.offset(x, 0, 0), ModBlocks.TANK.get().defaultBlockState());
            TankMultiblock.onTankBlockAdded(level, p.offset(x, 0, 0), null);
        }
        TankBlockEntity c = TankMultiblock.getController(level, p);
        if (c == null) throw new IllegalStateException("M13: plant tank did not form");
        TankInteractions.changeWater(level, c, 2000);
        c.getTank().setWaterTemp(26F);
        level.setBlockAndUpdate(p.above(), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.LIT, true));
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.GRAVEL.defaultBlockState(), false);
        }
        in.setBlock(r.toWorld(6, 0, 8), Blocks.STONE.defaultBlockState(), 3);
        if (InteriorPlants.plantAt(in, r, PlantSpecies.ROTALA, 20, 0, 8, 0, 13L) == null
                || InteriorPlants.plantAt(in, r, PlantSpecies.JAVA_FERN, 6, 1, 8, 2, 14L) == null) {
            throw new IllegalStateException("M13: could not plant");
        }
    }

    static void check(ServerLevel level, BlockPos base) {
        TankBlockEntity c = TankMultiblock.getController(level, tankPos(base));
        if (c == null) throw new IllegalStateException("M13: no plant tank");
        InteriorRegion r = InteriorPlants.region(level.getServer(), c.getTank());
        if (r == null || r.plants().size() != 2) throw new IllegalStateException("M13: plants lost: " + (r == null ? "no region" : r.plants().all()));
        for (int i = 0; i < 12; i++) InteriorPlants.grow(level, c, r, 0.25);
        InteriorPlant rotala = r.plants().all().stream().filter(p -> p.species == PlantSpecies.ROTALA).findFirst().orElseThrow();
        if (rotala.stage < 3) throw new IllegalStateException("M13: Rotala did not grow: " + rotala);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        int blocks = 0;
        for (var v : InteriorPlants.shape(r, rotala).at(rotala.stage)) {
            if (InteriorPlants.speciesOf(in.getBlockState(r.toWorld(rotala.x + v.x(), rotala.y + v.y(), rotala.z + v.z()))) == PlantSpecies.ROTALA) blocks++;
        }
        if (blocks != rotala.blocks) throw new IllegalStateException("M13: blocks " + blocks + " vs index " + rotala.blocks);
        CompoundTag saved = InteriorRegions.get(level.getServer()).save(new CompoundTag(), level.registryAccess());
        if (saved.getInt("schema") != 4) throw new IllegalStateException("M13: regions saved as schema " + saved.getInt("schema"));
        for (var e : ModItems.AQUATIC_PLANTS.entrySet()) if (e.getValue().get() == null) throw new IllegalStateException("M13: item " + e.getKey());
        boolean sword = false, lotus = false;
        for (int lv = 1; lv <= 2; lv++) {
            for (VillagerTrades.ItemListing l : VillagerTrades.WANDERING_TRADER_TRADES.get(lv)) {
                {
                    // M33: the listings are wrapped by their feature switches; the plant ones need no trader
                    net.minecraft.world.item.trading.MerchantOffer offer;
                    try {
                        offer = l.getOffer(null, level.getRandom());
                    } catch (RuntimeException ex) {
                        continue;
                    }
                    if (offer == null) continue;
                    sword |= offer.getResult().is(ModItems.AQUATIC_PLANTS.get(PlantSpecies.AMAZON_SWORD).get());
                    lotus |= offer.getResult().is(ModItems.AQUATIC_PLANTS.get(PlantSpecies.TIGER_LOTUS).get());
                }
            }
        }
        if (!sword || !lotus) throw new IllegalStateException("M13: trades sword " + sword + ", lotus " + lotus);
        if (InteriorPlants.uptakePerDay(level, c, r) <= 0) throw new IllegalStateException("M13: no waste uptake under the light");
        LOGGER.info("[smoketest] M13: Rotala grew to stage {} ({} blocks), {} plants in the index, regions saved as schema 4, items, trades ok",
                rotala.stage, blocks, r.plants().size());
    }
}
