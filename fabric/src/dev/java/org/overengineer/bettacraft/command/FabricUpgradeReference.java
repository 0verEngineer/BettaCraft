package org.overengineer.bettacraft.command;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.FeederBlockEntity;
import org.overengineer.bettacraft.equipment.FilterHeaters;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M21.5 upgrade test, OLD side. This file is NOT part of the mod: {@code tools/upgrade/build_reference.sh} copies it into
 * a git worktree of the reference version (1.13.1-beta.1, commit 909d12a) and hooks it into that version's
 * {@code DevShowcase}: after the showcase rows it builds an extra row with the things the showcase lacks (every guppy,
 * endler, platy, gourami, bristlenose and clownfish variety as pure lines, every small-animal colour, random domestic
 * bettas, cabinets of several woods with filter and heater, feeder with food, lid and light, a locked tank with an
 * owner, chests with every item of the mod incl. a bucket with a betta and a bag with shrimp). After the world is saved
 * it writes {@code bettacraft_reference.json} into the world folder: what every tank holds as the OLD version sees it
 * (water, salinity, blocks, name, lock, animals with variety and genes, small animals, floating plants, the block counts
 * of its interior, the equipment around it) and the chest contents. The new version's {@code DevSmokeUpgrade} compares
 * against that file.
 */
public final class FabricUpgradeReference {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/upgrade-reference");
    public static final UUID OWNER = UUID.nameUUIDFromBytes("bettacraft-upgrade-owner".getBytes(StandardCharsets.UTF_8));
    public static final String FILE = "bettacraft_reference.json";

    private FabricUpgradeReference() {}

    // ------------------------------------------------------------------ extra row

    public static void extras(ServerLevel level, BlockPos start) {
        RandomSource r = RandomSource.create(1301);
        BlockPos at = start;
        for (int x = -7; x < 130; x++) for (int z = -3; z <= -2; z++) level.setBlockAndUpdate(start.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
        // every tankmate variety (pure lines, a male and a female each)
        for (TankmateSpecies s : TankmateSpecies.values()) {
            if (s.varieties().size() <= 1) continue;
            TankBlockEntity c = tank(level, at, 3, 2, 2, s.marine() ? 35F : s == TankmateSpecies.GUPPY || s == TankmateSpecies.ENDLER_GUPPY ? 5F : 0F);
            for (String v : s.varieties()) {
                fish(level, c, s, TankmateGenes.pure(s, v), false, r);
                fish(level, c, s, TankmateGenes.pure(s, v), true, r);
            }
            c.getTank().setName("Ref " + s.id());
            c.markTankChanged();
            at = at.east(3 + 3);
        }
        // every small-animal colour (pure lines) plus wild ones that carry colours
        for (CritterSpecies s : CritterSpecies.values()) {
            if (s.loci().length == 0) continue;
            TankBlockEntity c = tank(level, at, 2, 1, 2, 0F);
            for (int l = 0; l < s.loci().length; l++) {
                TankCritters.add(level, c, s, CritterGenes.pure(l, 0.3F + 0.1F * l), true, s.juvenileDays() + 1);
                TankCritters.add(level, c, s, CritterGenes.pure(l, 0.6F), false, s.juvenileDays() + 1);
            }
            TankCritters.add(level, c, s, new CritterGenes(1, 0, 0.12F), true, s.juvenileDays() + 1);
            c.getTank().setName("Ref " + s.id());
            c.markTankChanged();
            at = at.east(2 + 3);
        }
        // nerites (one colour each in the old version) and random domestic bettas
        TankBlockEntity n = tank(level, at, 2, 2, 2, 0F);
        for (CritterSpecies s : CritterSpecies.values()) if (s.nerite()) for (int i = 0; i < 3; i++) TankCritters.add(level, n, s, CritterGenes.WILD, i % 2 == 0, s.juvenileDays() + 1);
        for (int i = 0; i < 4; i++) {
            BettaData d = BettaData.create(BettaGenome.randomDomestic(r), i % 2 == 0, level.getGameTime());
            TankInhabitants.insert(level, n, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d), ModEntities.BETTA.get(), null, false);
        }
        n.getTank().setName("Ref Rennschnecken + Bettas");
        n.markTankChanged();
        at = at.east(2 + 3);
        // cabinets of several woods, each with a filter and a heater, under a tank with lid, feeder (with food) and light; one locked
        String[] woods = {"spruce_planks", "mangrove_planks", "bamboo_planks", "cherry_planks", "dark_oak_planks"};
        for (int i = 0; i < woods.length; i++) {
            for (int x = 0; x < 2; x++) {
                BlockPos cp = at.offset(x, 0, 0);
                level.setBlockAndUpdate(cp, ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
                if (level.getBlockEntity(cp) instanceof CabinetBlockEntity cab) {
                    cab.setWood(Identifier.withDefaultNamespace(woods[i]));
                    if (x == 0) {
                        ItemStack f = new ItemStack(ModItems.EXTERNAL_FILTER.get());
                        FilterHeaters.write(f, List.of(new ItemStack(i % 2 == 0 ? ModItems.HEATER_MEDIUM.get() : ModItems.HEATER_SMALL.get()), ItemStack.EMPTY));
                        cab.items().setStackInSlot(CabinetBlockEntity.FILTER_SLOT, f);
                        cab.storage().insertItem(0, new ItemStack(ModItems.BETTA_PELLETS.get(), 10 + i), false);
                    }
                    cab.setChanged();
                }
            }
            for (int x = 0; x < 2; x++) {
                BlockPos cp = at.offset(x, 0, 0);
                level.setBlockAndUpdate(cp, Block.updateFromNeighbourShapes(level.getBlockState(cp), level, cp));
            }
            TankBlockEntity c = tank(level, at.above(), 2, 1, 2, 0F);
            level.setBlockAndUpdate(at.offset(0, 2, 0), ModBlocks.TANK_LIGHT.get().defaultBlockState());
            level.setBlockAndUpdate(at.offset(1, 2, 0), ModBlocks.FEEDER.get().defaultBlockState());
            if (level.getBlockEntity(at.offset(1, 2, 0)) instanceof FeederBlockEntity f) {
                f.items().insertItem(0, new ItemStack(ModItems.BETTA_PELLETS.get(), 5 + i), false);
                f.setChanged();
            }
            for (int x = 0; x < 2; x++) level.setBlockAndUpdate(at.offset(x, 2, 1), ModBlocks.TANK_LID.get().defaultBlockState());
            TankEquipment.invalidate(c);
            fish(level, c, TankmateSpecies.EMBER_TETRA, 3, r);
            c.getTank().setName("Ref Schrank " + woods[i].replace("_planks", ""));
            if (i == 0) {
                c.getTank().setOwner(OWNER);
                c.getTank().setLocked(true);
                c.getTank().setName("Ref Schloss");
            }
            c.markTankChanged();
            at = at.east(2 + 3);
        }
        // chests with every item of the mod
        List<ItemStack> all = new ArrayList<>();
        for (var e : BuiltInRegistries.ITEM.entrySet()) {
            if (!e.getKey().identifier().getNamespace().equals("bettacraft")) continue;
            all.add(new ItemStack(e.getValue()));
        }
        all.add(BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), BettaData.create(BettaGenome.randomDomestic(r), true, level.getGameTime())));
        BlockPos chest = at;
        for (int i = 0; i < all.size(); i++) {
            if (i % 27 == 0) {
                if (i > 0) chest = chest.east(2);
                level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
            }
            if (level.getBlockEntity(chest) instanceof ChestBlockEntity cb) cb.setItem(i % 27, all.get(i));
        }
        LOGGER.info("[upgrade-reference] extra row built, {} items in chests", all.size());
    }

    private static TankBlockEntity tank(ServerLevel level, BlockPos min, int sx, int sy, int sz, float salt) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos p : blocks) level.setBlockAndUpdate(p, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos p : blocks) TankMultiblock.onTankBlockAdded(level, p, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        if (c == null) throw new IllegalStateException("no tank at " + min);
        TankInteractions.changeWater(level, c, (long) blocks.size() * 1000, salt);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setWaterTemp(salt > 20 ? 26F : 25F);
        c.getTank().setTargetTemp(salt > 20 ? 26F : 25F);
        c.getTank().setWaterQuality(1F);
        c.markTankChanged();
        return c;
    }

    private static void fish(ServerLevel level, TankBlockEntity c, TankmateSpecies s, int n, RandomSource r) {
        for (int i = 0; i < n; i++) fish(level, c, s, TankmateGenes.wild(s), i % 2 == 0, r);
    }

    private static void fish(ServerLevel level, TankBlockEntity c, TankmateSpecies s, TankmateGenes g, boolean female, RandomSource r) {
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        Vec3 p = w.clamp(new Vec3(w.box().minX + r.nextDouble() * (w.box().maxX - w.box().minX), w.box().minY + 0.3,
                w.box().minZ + r.nextDouble() * (w.box().maxZ - w.box().minZ)), 0.35, 0.1, 0.2);
        var e = TankmateBreeding.spawn(level, c, s, g, female, LifeStage.ADULT, 0, p);
        if (e != null) e.health().feed();
    }

    // ------------------------------------------------------------------ manifest

    public static void manifest(MinecraftServer server, ServerLevel level) {
        JsonObject root = new JsonObject();
        root.addProperty("version", org.overengineer.bettacraft.platform.Platform.get().modVersion("bettacraft"));
        root.addProperty("gameTime", level.getGameTime());
        JsonArray tanks = new JsonArray(), chests = new JsonArray();
        BlockPos spawn = level.getRespawnData().pos();
        int cx0 = (spawn.getX() >> 4) - 2, cz0 = (spawn.getZ() >> 4) - 2;
        for (int cx = cx0; cx < cx0 + 14; cx++)
            for (int cz = cz0; cz < cz0 + 10; cz++) {
                var chunk = level.getChunk(cx, cz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof TankBlockEntity t && t.isController() && t.getTank() != null) tanks.add(tank(server, level, t));
                    if (be instanceof ChestBlockEntity cb) chests.add(chest(level, cb));
                }
            }
        root.add("tanks", tanks);
        root.add("chests", chests);
        try {
            Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve(FILE), new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        LOGGER.info("[upgrade-reference] MANIFEST WRITTEN: {} tanks, {} chests", tanks.size(), chests.size());
    }

    private static JsonObject tank(MinecraftServer server, ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        JsonObject o = new JsonObject();
        o.addProperty("pos", c.getBlockPos().asLong());
        o.addProperty("name", t.name());
        o.addProperty("water", t.waterMb());
        o.addProperty("salinity", t.salinity());
        o.addProperty("members", t.memberCount());
        o.addProperty("front", t.front().getName());
        o.addProperty("locked", t.locked());
        o.addProperty("owner", t.owner() == null ? "" : t.owner().toString());
        o.addProperty("signVisible", t.signVisible());
        o.addProperty("floating", t.plants().size());
        o.addProperty("regionId", t.regionId());
        JsonArray inh = new JsonArray();
        for (UUID u : t.inhabitants()) {
            JsonObject a = new JsonObject();
            a.addProperty("uuid", u.toString());
            Entity e = level.getEntity(u);
            if (e != null) {
                a.addProperty("type", BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString());
                if (e instanceof TankmateEntity m) {
                    a.addProperty("variety", m.variety());
                    a.addProperty("female", m.isFemale());
                    a.addProperty("stage", m.lifeStage().name());
                } else if (e instanceof BettaEntity b) {
                    a.addProperty("genome", b.genome().toString());
                    a.addProperty("female", b.isFemale());
                    a.addProperty("stage", b.lifeStage().name());
                }
            }
            inh.add(a);
        }
        o.add("inhabitants", inh);
        JsonArray crit = new JsonArray();
        List<String> cs = new ArrayList<>();
        for (Critter k : t.critters()) cs.add(k.species.id() + "|" + k.genes.variety(k.species) + "|" + k.female + "|" + k.genes.a() + "/" + k.genes.b());
        java.util.Collections.sort(cs);
        cs.forEach(crit::add);
        o.add("critters", crit);
        if (t.regionId() >= 0) {
            InteriorRegion r = InteriorRegions.get(server).get(t.regionId());
            ServerLevel in = InteriorManager.interiorLevel(server);
            if (r != null && in != null) {
                Map<String, Integer> counts = new TreeMap<>();
                for (int y = 0; y < r.sizeY(); y++)
                    for (int z = 0; z < r.sizeZ(); z++)
                        for (int x = 0; x < r.sizeX(); x++) {
                            BlockState s = in.getBlockState(r.toWorld(x, y, z));
                            if (s.isAir()) continue;
                            counts.merge(BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString(), 1, Integer::sum);
                        }
                JsonObject co = new JsonObject();
                counts.forEach(co::addProperty);
                o.add("interior", co);
            }
        }
        JsonArray eq = new JsonArray();
        BlockPos lo = t.min().offset(-1, -1, -1), hi = t.max().offset(1, 1, 1);
        for (BlockPos p : BlockPos.betweenClosed(lo, hi)) {
            BlockState s = level.getBlockState(p);
            Identifier id = BuiltInRegistries.BLOCK.getKey(s.getBlock());
            if (!id.getNamespace().equals("bettacraft") || s.is(ModBlocks.TANK.get())) continue;
            JsonObject e = new JsonObject();
            e.addProperty("pos", p.asLong());
            e.addProperty("block", id.toString());
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof CabinetBlockEntity cab) {
                e.addProperty("wood", cab.woodId().toString());
                e.addProperty("filter", BuiltInRegistries.ITEM.getKey(cab.filterItem().getItem()).toString());
                e.addProperty("heater0", BuiltInRegistries.ITEM.getKey(cab.heater(0).getItem()).toString());
                e.addProperty("items", items(cab.items()));
            } else if (be instanceof FeederBlockEntity f) {
                e.addProperty("items", items(f.items()));
                e.addProperty("feedings", f.feedingsPerDay());
            }
            eq.add(e);
        }
        o.add("equipment", eq);
        return o;
    }

    private static String items(org.overengineer.bettacraft.platform.item.IItemHandler h) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (s.isEmpty()) continue;
            sb.append(i).append(':').append(BuiltInRegistries.ITEM.getKey(s.getItem())).append('x').append(s.getCount()).append(' ');
        }
        return sb.toString().trim();
    }

    private static JsonObject chest(ServerLevel level, ChestBlockEntity cb) {
        JsonObject o = new JsonObject();
        o.addProperty("pos", cb.getBlockPos().asLong());
        JsonArray a = new JsonArray();
        for (int i = 0; i < cb.getContainerSize(); i++) {
            ItemStack s = cb.getItem(i);
            if (s.isEmpty()) continue;
            JsonObject it = new JsonObject();
            it.addProperty("slot", i);
            it.addProperty("id", BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
            it.addProperty("count", s.getCount());
            List<String> comps = new ArrayList<>();
            // M52e (26.3): no entry view of a patch any more; its added components and removed types, sorted
            var split = s.getComponentsPatch().split();
            for (var c : split.added()) comps.add(String.valueOf(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(c.type())));
            for (var t : split.removed()) comps.add(String.valueOf(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(t)));
            java.util.Collections.sort(comps);
            java.util.Collections.sort(comps);
            it.addProperty("components", String.join(",", comps));
            a.add(it);
        }
        o.add("items", a);
        return o;
    }
}
