package org.overengineer.bettacraft.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.plants.AquaticPlantBlock;
import org.overengineer.bettacraft.plants.AquaticPlantItem;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantPart;
import org.overengineer.bettacraft.plants.PlantShapes;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorObstacles;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.overengineer.bettacraft.tank.interior.InteriorSync;
import org.overengineer.bettacraft.tank.interior.InteriorWorldOps;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tank.menu.TankMenu;

/**
 * M13 GameTests: aquarium plants in the tank interior. Planting only on the right ground (like a player), growth over
 * accelerated days on the plant index with the blocks and the miniature following (also in the dark: none), trimming
 * inside and from outside through the glass, dividing and taking out, drops when a tank block breaks, merging tanks,
 * waste uptake, bettas swimming around plants, and the sources (fishing loot with fillbiome, trader, moss, swamp water).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AquaticPlantGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private AquaticPlantGameTests() {}

    // ------------------------------------------------------------------ helpers

    private static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz, long water) {
        for (int a = 0; a < sx; a++)
            for (int b = 0; b < sy; b++)
                for (int c = 0; c < sz; c++) {
                    BlockPos p = new BlockPos(x + a, y + b, z + c);
                    h.setBlock(p, ModBlocks.TANK.get().defaultBlockState());
                    TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(p), null);
                }
        TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(new BlockPos(x, y, z)));
        if (c == null) throw new GameTestAssertException("tank did not form");
        if (water > 0) TankInteractions.changeWater(h.getLevel(), c, water);
        c.getTank().setWaterTemp(26F);
        c.getTank().setTargetTemp(26F);
        return c;
    }

    private static ServerLevel interior(GameTestHelper h) {
        ServerLevel l = InteriorManager.interiorLevel(h.getLevel().getServer());
        if (l == null) throw new GameTestAssertException("interior dimension not loaded");
        return l;
    }

    private static InteriorRegion region(GameTestHelper h, TankBlockEntity c) {
        interior(h);
        return InteriorManager.ensureRegion(h.getLevel(), c);
    }

    private static void light(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel, ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.LIT, true));
    }

    private static BlockPos floor(InteriorRegion r, int lx, int lz) {
        return new BlockPos(r.originX() + lx, InteriorRegion.FLOOR_Y, r.originZ() + lz);
    }

    private static void substrate(ServerLevel in, InteriorRegion r) {
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) Substrate.set(in, floor(r, x, z), Blocks.GRAVEL.defaultBlockState(), false);
    }

    private static ItemStack item(PlantSpecies s) {
        return InteriorPlants.item(s, 0);
    }

    private static ServerPlayer playerIn(GameTestHelper h, ServerLevel level, BlockPos at) {
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        sp.teleportTo(level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F, 0F);
        sp.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        sp.getInventory().selected = 8;
        return sp;
    }

    /** Uses the item in the hand on a face of a block, like a right-click. */
    private static void use(ServerPlayer sp, ServerLevel level, BlockPos pos, Direction face, ItemStack stack) {
        sp.setItemInHand(InteractionHand.MAIN_HAND, stack);
        Vec3 hit = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        sp.gameMode.useItemOn(sp, level, stack, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, pos, false));
    }

    private static int count(ServerPlayer sp, Item item) {
        int n = 0;
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            ItemStack s = sp.getInventory().getItem(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }

    private static int plantBlocks(ServerLevel in, InteriorRegion r, PlantSpecies s) {
        int[] n = new int[1];
        InteriorWorldOps.forEachBlock(in, r.originX(), InteriorRegion.BASE_Y, r.originZ(), r.originX() + r.sizeX() - 1,
                InteriorRegion.BASE_Y + r.sizeY() - 1, r.originZ() + r.sizeZ() - 1, (x, y, z, st) -> {
                    if (InteriorPlants.speciesOf(st) == s) n[0]++;
                });
        return n[0];
    }

    /** World position of a block of a plant's shape (first one of the part, at least {@code minY} above the root). */
    private static BlockPos voxel(InteriorRegion r, InteriorPlant p, PlantPart part, int minY) {
        for (PlantShapes.Voxel v : InteriorPlants.shape(r, p).at(p.stage)) {
            if (v.part() == part && v.y() >= minY) return r.toWorld(p.x + v.x(), p.y + v.y(), p.z + v.z());
        }
        throw new GameTestAssertException("no " + part + " block in " + p);
    }

    // ------------------------------------------------------------------ planting

    /** Rooted plants only on substrate, epiphytes only on stone or wood, moss on any solid block, nothing outside. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void plantingNeedsTheRightGround(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 1, 1, 2000);
        InteriorRegion r = region(h, c);
        ServerLevel in = interior(h);
        ServerPlayer sp = playerIn(h, in, r.toWorld(2, 0, 2));
        try {
            // a crypt on the bare floor: refused, the cutting stays in the hand
            BlockPos bare = floor(r, 5, 5);
            use(sp, in, bare, Direction.UP, item(PlantSpecies.CRYPT_GREEN).copyWithCount(3));
            h.assertTrue(r.plants().isEmpty() && in.getBlockState(bare.above()).isAir(), "no crypt without substrate");
            h.assertTrue(sp.getMainHandItem().getCount() == 3, "item kept");
            // with gravel as substrate it grows
            Substrate.set(in, bare, Blocks.GRAVEL.defaultBlockState(), false);
            use(sp, in, bare, Direction.UP, sp.getMainHandItem());
            InteriorPlant crypt = r.plants().at(5, 0, 5);
            h.assertTrue(crypt != null && crypt.species == PlantSpecies.CRYPT_GREEN && crypt.stage == 0, "crypt planted: " + r.plants().all());
            BlockState cutting = in.getBlockState(bare.above());
            h.assertTrue(cutting.getBlock() instanceof AquaticPlantBlock && cutting.getValue(AquaticPlantBlock.PART) == PlantPart.CUTTING, "a cutting block");
            h.assertTrue(sp.getMainHandItem().getCount() == 2, "one item used");
            // right next to it: too close
            Substrate.set(in, floor(r, 6, 5), Blocks.GRAVEL.defaultBlockState(), false);
            use(sp, in, floor(r, 6, 5), Direction.UP, sp.getMainHandItem());
            h.assertTrue(r.plants().size() == 1, "too close to the first plant");
            // a Java fern on gravel: refused; on a stone: planted on top
            Substrate.set(in, floor(r, 12, 5), Blocks.GRAVEL.defaultBlockState(), false);
            use(sp, in, floor(r, 12, 5), Direction.UP, item(PlantSpecies.JAVA_FERN));
            h.assertTrue(r.plants().size() == 1, "no fern on gravel");
            BlockPos stone = r.toWorld(12, 0, 10);
            in.setBlock(stone, Blocks.STONE.defaultBlockState(), 3);
            use(sp, in, stone, Direction.UP, item(PlantSpecies.JAVA_FERN));
            h.assertTrue(r.plants().at(12, 1, 10) != null, "fern on the stone");
            // Java moss on the side of another stone
            BlockPos stone2 = r.toWorld(20, 0, 10);
            in.setBlock(stone2, Blocks.COBBLESTONE.defaultBlockState(), 3);
            use(sp, in, stone2, Direction.EAST, item(PlantSpecies.JAVA_MOSS));
            h.assertTrue(r.plants().at(21, 0, 10) != null, "moss on the side of the stone");
            // outside of a tank interior: nothing
            sp.teleportTo(h.getLevel(), h.absolutePos(new BlockPos(20, 2, 20)).getX(), h.absolutePos(new BlockPos(20, 2, 20)).getY(),
                    h.absolutePos(new BlockPos(20, 2, 20)).getZ(), 0F, 0F);
            h.setBlock(new BlockPos(20, 1, 20), Blocks.SAND);
            use(sp, h.getLevel(), h.absolutePos(new BlockPos(20, 1, 20)), Direction.UP, item(PlantSpecies.VALLISNERIA));
            h.assertTrue(h.getLevel().getBlockState(h.absolutePos(new BlockPos(20, 2, 20))).isAir(), "nothing planted outside");
            h.assertTrue(sp.getMainHandItem().getCount() == 1, "item kept outside");
        } finally {
            sp.getServer().getPlayerList().remove(sp);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ growth

    /**
     * A Rotala cutting under a lit tank light grows stage by stage over accelerated days on the plant index; its blocks
     * appear in the interior and the miniature gets a new version. Under a stone roof (dark) nothing grows.
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void growsOverTheDaysWithLightOnly(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 1, 1, 2000);
        light(h, new BlockPos(2, 2, 2));
        InteriorRegion r = region(h, c);
        ServerLevel in = interior(h);
        substrate(in, r);
        InteriorPlant rotala = InteriorPlants.plantAt(in, r, PlantSpecies.ROTALA, 8, 0, 8, 0, 7L);
        h.assertTrue(rotala != null && rotala.stage == 0, "cutting");
        // dark twin: a stone roof right above it
        TankBlockEntity dark = tank(h, 2, 1, 10, 2, 1, 1, 2000);
        // closed in by stone on every side: no light reaches the surface
        for (BlockPos b : List.of(new BlockPos(2, 2, 10), new BlockPos(3, 2, 10), new BlockPos(1, 1, 10), new BlockPos(4, 1, 10),
                new BlockPos(2, 1, 9), new BlockPos(3, 1, 9), new BlockPos(2, 1, 11), new BlockPos(3, 1, 11), new BlockPos(2, 0, 10),
                new BlockPos(3, 0, 10))) h.setBlock(b, Blocks.STONE);
        InteriorRegion rd = region(h, dark);
        substrate(in, rd);
        InteriorPlant shaded = InteriorPlants.plantAt(in, rd, PlantSpecies.ROTALA, 8, 0, 8, 0, 7L);
        // the light engine needs a few ticks for the stone around the dark tank
        h.runAfterDelay(20, () -> growPart(h, level, c, r, in, rotala, dark, rd, shaded));
    }

    private static void growPart(GameTestHelper h, ServerLevel level, TankBlockEntity c, InteriorRegion r, ServerLevel in, InteriorPlant rotala,
                                 TankBlockEntity dark, InteriorRegion rd, InteriorPlant shaded) {
        InteriorSync.flush(level.getServer());
        int v0 = InteriorSync.version(r.id());
        int before = plantBlocks(in, r, PlantSpecies.ROTALA);
        for (int i = 0; i < 12; i++) {
            InteriorPlants.grow(level, c, r, 0.25);
            InteriorPlants.grow(level, dark, rd, 0.25);
        }
        InteriorSync.flush(level.getServer());
        h.assertTrue(rotala.stage >= 4, "three days with light: stage " + rotala.stage);
        h.assertTrue(rotala.stage == rotala.target, "blocks written for the stage");
        int after = plantBlocks(in, r, PlantSpecies.ROTALA);
        h.assertTrue(after > before + 20 && after == rotala.blocks, "blocks in the interior: " + before + " -> " + after + " (index " + rotala.blocks + ")");
        h.assertTrue(InteriorSync.version(r.id()) > v0, "the miniature got the change");
        h.assertTrue(shaded.stage == 0 && plantBlocks(in, rd, PlantSpecies.ROTALA) == 1, "no growth in the dark: stage " + shaded.stage + ", "
                + plantBlocks(in, rd, PlantSpecies.ROTALA) + " blocks, light " + org.overengineer.bettacraft.tank.plants.TankPlants.light(level, dark));
        // the index survives saving
        var again = InteriorRegion.load(r.save());
        h.assertTrue(again.plants().size() == 1 && again.plants().all().getFirst().stage == rotala.stage, "saved with the region");
        // to full size, then side shoots
        for (int i = 0; i < 40 && r.plants().size() == 1; i++) InteriorPlants.grow(level, c, r, 0.5);
        h.assertTrue(rotala.fullGrown(), "full-grown");
        h.assertTrue(r.plants().size() > 1, "side shoot formed: " + r.plants().all());
        h.assertTrue(c.getTank().log().stream().anyMatch(e -> e.key().equals("log.bettacraft.aquatic_side_shoot")), "side shoot logged");
        h.succeed();
    }

    /** Red species follow the light: red under the lit tank light. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void redPlantsTurnRedInStrongLight(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 1, 1, 2000);
        light(h, new BlockPos(2, 2, 2));
        InteriorRegion r = region(h, c);
        ServerLevel in = interior(h);
        InteriorPlant p = InteriorPlants.plantAt(in, r, PlantSpecies.LUDWIGIA, 8, 0, 8, 4, 3L);
        h.assertTrue(p.tint == 0, "starts green");
        for (int i = 0; i < 12; i++) InteriorPlants.grow(level, c, r, 0.5);
        h.assertTrue(p.tint == 3, "red under strong light, tint " + p.tint);
        BlockState leaf = in.getBlockState(voxel(r, p, PlantPart.LEAF, 1));
        h.assertTrue(leaf.getValue(AquaticPlantBlock.TINT) == 3, "the blocks show it");
        h.succeed();
    }

    // ------------------------------------------------------------------ trimming, dividing, taking out

    /** Shears on a plant block inside trim it and give cuttings; shears on the tank from outside trim the plant looked at. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void trimInsideAndFromOutside(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 1, 1, 2000);
        InteriorRegion r = region(h, c);
        ServerLevel in = interior(h);
        InteriorPlant p = InteriorPlants.plantAt(in, r, PlantSpecies.ROTALA, 16, 0, 8, 8, 11L);
        ServerPlayer sp = playerIn(h, in, r.toWorld(16, 0, 2));
        try {
            BlockPos stem = voxel(r, p, PlantPart.STEM, 3);
            use(sp, in, stem, Direction.NORTH, new ItemStack(Items.SHEARS));
            h.assertTrue(p.stage == 6, "cut back two stages: " + p.stage);
            int cuttings = count(sp, item(PlantSpecies.ROTALA).getItem());
            h.assertTrue(cuttings >= 2, "cuttings: " + cuttings);
            h.assertTrue(sp.getMainHandItem().getDamageValue() == 1, "shears used");
            // from outside: in front of the tank (north), looking at a block of the plant in the miniature
            TankData t = c.getTank();
            BlockPos target = voxel(r, p, PlantPart.STEM, 4);
            double fx = (t.sizeX() - 2 * InteriorPlants.MARGIN) / r.sizeX(), fy = (t.sizeY() - 2 * InteriorPlants.MARGIN) / r.sizeY(),
                    fz = (t.sizeZ() - 2 * InteriorPlants.MARGIN) / r.sizeZ();
            Vec3 point = new Vec3(t.min().getX() + InteriorPlants.MARGIN + (r.localX(target.getX()) + 0.5) * fx,
                    t.min().getY() + InteriorPlants.MARGIN + (r.localY(target.getY()) + 0.5) * fy,
                    t.min().getZ() + InteriorPlants.MARGIN + (r.localZ(target.getZ()) + 0.5) * fz);
            Vec3 eye = new Vec3(point.x, point.y, t.min().getZ() - 1.2);
            sp.teleportTo(level, eye.x, eye.y - sp.getEyeHeight(), eye.z, 0F, 0F);
            sp.setYRot(0F);
            sp.setXRot(0F);
            sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
            BlockPos glass = BlockPos.containing(point.x, point.y, t.min().getZ());
            sp.gameMode.useItemOn(sp, level, sp.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(new Vec3(point.x, point.y, t.min().getZ()), Direction.NORTH, glass, false));
            InteriorPlant aimed = InteriorPlants.aimedAt(in, r, t, sp.getEyePosition(), sp.getViewVector(1.0F));
            h.assertTrue(p.stage == 4, "trimmed from outside: " + p.stage + " (aimed at " + aimed + " from " + sp.getEyePosition() + " looking "
                    + sp.getViewVector(1.0F) + " at " + point + ", in " + sp.level().dimension().location() + ")");
            h.assertTrue(!(sp.containerMenu instanceof TankMenu), "no menu");
            // looking at an empty part of the tank: nothing happens
            sp.teleportTo(level, t.min().getX() + 0.2, eye.y - sp.getEyeHeight(), eye.z, 0F, 0F);
            sp.gameMode.useItemOn(sp, level, sp.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(new Vec3(t.min().getX() + 0.2, eye.y, t.min().getZ()), Direction.NORTH, t.min(), false));
            h.assertTrue(p.stage == 4 && !(sp.containerMenu instanceof TankMenu), "empty spot: no trim, no menu");
        } finally {
            sp.getServer().getPlayerList().remove(sp);
        }
        h.succeed();
    }

    /** Breaking a rhizome piece divides a Java fern; breaking the root takes the whole plant out (item keeps its stage). */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void divideAndTakeOut(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 1, 1, 2000);
        InteriorRegion r = region(h, c);
        ServerLevel in = interior(h);
        in.setBlock(r.toWorld(16, 0, 8), Blocks.STONE.defaultBlockState(), 3);
        InteriorPlant fern = InteriorPlants.plantAt(in, r, PlantSpecies.JAVA_FERN, 16, 1, 8, 8, 5L);
        ServerPlayer sp = playerIn(h, in, r.toWorld(16, 2, 3));
        try {
            BlockPos rhizome = voxel(r, fern, PlantPart.RHIZOME, 0);
            sp.gameMode.destroyBlock(rhizome);
            h.assertTrue(fern.stage == 4, "divided: half left, stage " + fern.stage);
            ItemStack half = dropped(in, rhizome, PlantSpecies.JAVA_FERN);
            h.assertTrue(AquaticPlantItem.stage(half) == 4, "the other half as an item, stage " + AquaticPlantItem.stage(half));
            BlockPos root = r.toWorld(16, 1, 8);
            sp.gameMode.destroyBlock(root);
            h.assertTrue(r.plants().isEmpty(), "plant taken out of the index");
            h.assertTrue(plantBlocks(in, r, PlantSpecies.JAVA_FERN) == 0, "no fern blocks left");
            h.assertTrue(AquaticPlantItem.stage(dropped(in, root, PlantSpecies.JAVA_FERN)) == 4, "whole plant as an item");
            // the item plants a grown fern again
            in.setBlock(r.toWorld(8, 0, 8), Blocks.OAK_LOG.defaultBlockState(), 3);
            use(sp, in, r.toWorld(8, 0, 8), Direction.UP, InteriorPlants.item(PlantSpecies.JAVA_FERN, 4));
            InteriorPlant again = r.plants().at(8, 1, 8);
            h.assertTrue(again != null && again.stage == 4 && again.blocks > 10, "replanted at its size: " + again);
        } finally {
            sp.getServer().getPlayerList().remove(sp);
        }
        h.succeed();
    }

    private static ItemStack dropped(ServerLevel level, BlockPos at, PlantSpecies s) {
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(2))) {
            if (e.getItem().getItem() instanceof AquaticPlantItem p && p.species() == s) {
                ItemStack st = e.getItem().copy();
                e.discard();
                return st;
            }
        }
        throw new GameTestAssertException("no " + s + " item dropped at " + at);
    }

    /** Breaking the tank block a plant is rooted in drops the plant (with its stage); merging tanks keeps the plants. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void plantsDropWithTheirTankBlockAndMergeAlong(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 1, 2, 1, 1, 1, 1000);
        InteriorRegion r = region(h, c);
        ServerLevel in = interior(h);
        InteriorPlants.plantAt(in, r, PlantSpecies.CRYPT_BROWN, 8, 0, 8, 6, 9L);
        // merge: a second block on the east side
        h.setBlock(new BlockPos(3, 1, 2), ModBlocks.TANK.get().defaultBlockState());
        TankMultiblock.onTankBlockAdded(level, h.absolutePos(new BlockPos(3, 1, 2)), null);
        TankBlockEntity merged = TankMultiblock.getController(level, h.absolutePos(new BlockPos(2, 1, 2)));
        InteriorRegion mr = InteriorRegions.get(level.getServer()).get(merged.getTank().regionId());
        h.assertTrue(mr != null && mr.plants().size() == 1, "plant moved into the merged region");
        InteriorPlant p = mr.plants().all().getFirst();
        h.assertTrue(InteriorPlants.speciesOf(in.getBlockState(mr.toWorld(p.x, p.y, p.z))) == PlantSpecies.CRYPT_BROWN, "its root block came along");
        // the block with the root drops it
        InteriorManager.dropInteriorOfBlock(level, merged.getTank(), merged.getTank().min());
        h.assertTrue(mr.plants().isEmpty(), "gone from the index");
        boolean found = false;
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(merged.getTank().min()).inflate(3))) {
            if (e.getItem().getItem() instanceof AquaticPlantItem ai && ai.species() == PlantSpecies.CRYPT_BROWN && AquaticPlantItem.stage(e.getItem()) == 6) found = true;
            if (e.getItem().getItem() instanceof AquaticPlantItem && e.getItem().getCount() > 1) throw new GameTestAssertException("one item per plant, not per block");
        }
        h.assertTrue(found, "plant dropped with its stage");
        h.succeed();
    }

    // ------------------------------------------------------------------ effects

    /** Plants take up waste with light; bettas swim around them (obstacles), not around a cutting. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void plantsCleanTheWaterAndAreObstacles(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 1, 1, 2000);
        light(h, new BlockPos(2, 2, 2));
        InteriorRegion r = region(h, c);
        ServerLevel in = interior(h);
        InteriorPlant sword = InteriorPlants.plantAt(in, r, PlantSpecies.AMAZON_SWORD, 10, 0, 8, 8, 2L);
        InteriorPlants.plantAt(in, r, PlantSpecies.VALLISNERIA, 22, 0, 8, 8, 3L);
        InteriorPlant cutting = InteriorPlants.plantAt(in, r, PlantSpecies.CRYPT_GREEN, 28, 0, 3, 0, 4L);
        TankData t = c.getTank();
        t.setWaterQuality(0.6F);
        InteriorPlants.grow(level, c, r, 1.0);
        h.assertTrue(t.waterQuality() > 0.6F, "waste taken up: " + t.waterQuality());
        BlockPos leaf = voxel(r, sword, PlantPart.LEAF, 4);
        double fx = 1.0 / r.scale();
        Vec3 world = new Vec3(t.min().getX() + (r.localX(leaf.getX()) + 0.5) * fx, t.min().getY() + (r.localY(leaf.getY()) + 0.5) * fx,
                t.min().getZ() + (r.localZ(leaf.getZ()) + 0.5) * fx);
        boolean blocked = false;
        for (int i = 0; i < 4 && !blocked; i++) blocked = InteriorObstacles.blocked(level.getServer(), t, world); // first calls may load chunks
        h.assertTrue(blocked, "bettas swim around the sword's leaves");
        Vec3 cut = new Vec3(t.min().getX() + (cutting.x + 0.5) * fx, t.min().getY() + 0.5 * fx, t.min().getZ() + (cutting.z + 0.5) * fx);
        h.assertFalse(InteriorObstacles.blocked(level.getServer(), t, cut), "a cutting is no obstacle");
        h.succeed();
    }

    // ------------------------------------------------------------------ sources

    /** Fishing in jungles and rivers brings up epiphytes, in swamps crypts and stem plants, in plains nothing. */
    @GameTest(template = EMPTY, batch = "bettacraft_aquatic_fishing")
    public static void fishingBringsUpAquariumPlants(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        double saved = ServerConfig.AQUATIC_FISHING_CHANCE.get(), savedFloating = ServerConfig.PLANTS_FISHING_CHANCE.get();
        try {
            ServerConfig.AQUATIC_FISHING_CHANCE.set(1.0);
            ServerConfig.PLANTS_FISHING_CHANCE.set(0.0);
            var table = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
            for (String biome : new String[]{"minecraft:jungle", "minecraft:river", "minecraft:swamp", "minecraft:plains"}) {
                BlockPos at = h.absolutePos(new BlockPos(16, 2, 16));
                var src = level.getServer().createCommandSourceStack().withSuppressedOutput().withLevel(level);
                level.getServer().getCommands().performPrefixedCommand(src, "fillbiome " + (at.getX() - 4) + " " + (at.getY() - 1) + " " + (at.getZ() - 4) + " "
                        + (at.getX() + 4) + " " + (at.getY() + 4) + " " + (at.getZ() + 4) + " " + biome);
                h.assertTrue(level.getBiome(at).is(net.minecraft.resources.ResourceLocation.parse(biome)), "biome set: " + level.getBiome(at));
                java.util.Set<PlantSpecies> seen = java.util.EnumSet.noneOf(PlantSpecies.class);
                int plants = 0, rolls = 300;
                for (int i = 0; i < rolls; i++) {
                    LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(at))
                            .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD)).create(LootContextParamSets.FISHING);
                    for (ItemStack s : table.getRandomItems(params)) {
                        if (s.getItem() instanceof AquaticPlantItem p) {
                            plants++;
                            seen.add(p.species());
                        }
                    }
                }
                switch (biome) {
                    case "minecraft:plains" -> h.assertTrue(plants == 0, "nothing in plains: " + plants);
                    case "minecraft:swamp" -> {
                        h.assertTrue(plants == rolls, "swamp: every catch a plant");
                        h.assertTrue(seen.equals(java.util.EnumSet.of(PlantSpecies.CRYPT_GREEN, PlantSpecies.CRYPT_BROWN, PlantSpecies.LUDWIGIA,
                                PlantSpecies.ROTALA)), "swamp plants: " + seen);
                    }
                    default -> {
                        h.assertTrue(plants == rolls, biome + ": every catch a plant");
                        h.assertTrue(seen.contains(PlantSpecies.JAVA_FERN) && seen.contains(PlantSpecies.ANUBIAS_NANA) && seen.contains(PlantSpecies.BUCE_BLUE)
                                && seen.stream().allMatch(s -> s.form() == PlantSpecies.Form.EPIPHYTE), biome + " epiphytes: " + seen);
                    }
                }
            }
        } finally {
            ServerConfig.AQUATIC_FISHING_CHANCE.set(saved);
            ServerConfig.PLANTS_FISHING_CHANCE.set(savedFloating);
        }
        h.succeed();
    }

    /** The wandering trader sells sword, Vallisneria, Monte Carlo and Alternanthera; the tiger lotus and Flamingo rarely. */
    @GameTest(template = EMPTY)
    public static void wanderingTraderSellsAquariumPlants(GameTestHelper h) {
        WanderingTrader trader = h.spawn(EntityType.WANDERING_TRADER, new BlockPos(2, 1, 2));
        var rnd = h.getLevel().getRandom();
        java.util.Set<Item> generic = new java.util.HashSet<>(), rare = new java.util.HashSet<>();
        for (int level = 1; level <= 2; level++) {
            for (VillagerTrades.ItemListing l : VillagerTrades.WANDERING_TRADER_TRADES.get(level)) {
                var offer = l.getOffer(trader, rnd);
                if (offer != null) (level == 1 ? generic : rare).add(offer.getResult().getItem());
            }
        }
        for (PlantSpecies s : List.of(PlantSpecies.AMAZON_SWORD, PlantSpecies.VALLISNERIA, PlantSpecies.MONTE_CARLO, PlantSpecies.ALTERNANTHERA)) {
            h.assertTrue(generic.contains(item(s).getItem()), s + " for sale");
        }
        h.assertTrue(rare.contains(item(PlantSpecies.TIGER_LOTUS).getItem()) && rare.contains(item(PlantSpecies.CRYPT_FLAMINGO).getItem()), "rare offers");
        trader.discard();
        h.succeed();
    }

    /** Shears on a moss block next to water give Java moss (chance 1); without water they do nothing special. */
    @GameTest(template = EMPTY, batch = "bettacraft_aquatic_moss")
    public static void shearsOnMossNextToWaterGiveJavaMoss(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        double saved = ServerConfig.AQUATIC_MOSS_CHANCE.get();
        h.setBlock(new BlockPos(5, 1, 5), Blocks.MOSS_BLOCK);
        h.setBlock(new BlockPos(6, 1, 5), Blocks.WATER);
        h.setBlock(new BlockPos(12, 1, 5), Blocks.MOSS_BLOCK);
        ServerPlayer sp = playerIn(h, level, h.absolutePos(new BlockPos(5, 2, 3)));
        try {
            ServerConfig.AQUATIC_MOSS_CHANCE.set(1.0);
            use(sp, level, h.absolutePos(new BlockPos(5, 1, 5)), Direction.UP, new ItemStack(Items.SHEARS));
            h.assertTrue(count(sp, item(PlantSpecies.JAVA_MOSS).getItem()) == 1, "Java moss");
            h.assertTrue(level.getBlockState(h.absolutePos(new BlockPos(5, 1, 5))).is(Blocks.MOSS_BLOCK), "the moss block stays");
            use(sp, level, h.absolutePos(new BlockPos(12, 1, 5)), Direction.UP, new ItemStack(Items.SHEARS));
            h.assertTrue(count(sp, item(PlantSpecies.JAVA_MOSS).getItem()) == 1, "no Java moss away from water");
        } finally {
            ServerConfig.AQUATIC_MOSS_CHANCE.set(saved);
            sp.getServer().getPlayerList().remove(sp);
        }
        h.succeed();
    }

    /** A bottle of swamp water brings up a swamp cutting with chance 1 (the swamp water stays). */
    @GameTest(template = EMPTY, batch = "bettacraft_aquatic_bottle")
    public static void swampBottleSometimesBringsACutting(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos water = h.absolutePos(new BlockPos(20, 1, 20));
        h.setBlock(new BlockPos(20, 1, 20), Blocks.WATER);
        fillSwamp(level, water);
        // the biome change can need a moment (and a second try) right after the template was placed
        h.runAfterDelay(10, () -> {
            if (!level.getBiome(water).is(net.minecraft.world.level.biome.Biomes.SWAMP)) fillSwamp(level, water);
            bottleCheck(h, level, water);
        });
    }

    private static void fillSwamp(ServerLevel level, BlockPos water) {
        var src = level.getServer().createCommandSourceStack().withSuppressedOutput().withLevel(level);
        // a few blocks around the water (the biome lookup is fuzzy across neighbouring 4x4x4 cells), inside the loaded template
        level.getServer().getCommands().performPrefixedCommand(src, "fillbiome " + (water.getX() - 4) + " " + (water.getY() - 1) + " " + (water.getZ() - 4) + " "
                + (water.getX() + 4) + " " + (water.getY() + 4) + " " + (water.getZ() + 4) + " minecraft:swamp");
    }

    private static void bottleCheck(GameTestHelper h, ServerLevel level, BlockPos water) {
        double saved = ServerConfig.AQUATIC_BOTTLE_CHANCE.get(), savedDuck = ServerConfig.PLANTS_BOTTLE_CHANCE.get();
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        sp.teleportTo(level, water.getX() + 0.5, water.getY() + 2.6 - sp.getEyeHeight(), water.getZ() + 0.5, 0F, 90F);
        sp.setXRot(90F);
        sp.getInventory().selected = 8;
        try {
            ServerConfig.AQUATIC_BOTTLE_CHANCE.set(1.0);
            ServerConfig.PLANTS_BOTTLE_CHANCE.set(0.0);
            sp.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
            h.assertTrue(level.getBiome(water).is(net.minecraft.world.level.biome.Biomes.SWAMP), "swamp: " + level.getBiome(water));
            sp.gameMode.useItem(sp, level, sp.getMainHandItem(), InteractionHand.MAIN_HAND);
            h.assertTrue(count(sp, org.overengineer.bettacraft.registry.ModItems.SWAMP_WATER_BOTTLE.get()) == 1, "swamp water as before");
            int cuttings = 0;
            for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
                ItemStack s = sp.getInventory().getItem(i);
                if (s.getItem() instanceof AquaticPlantItem p) {
                    cuttings += s.getCount();
                    h.assertTrue(List.of(PlantSpecies.CRYPT_GREEN, PlantSpecies.CRYPT_BROWN, PlantSpecies.LUDWIGIA, PlantSpecies.ROTALA).contains(p.species()),
                            "a swamp plant: " + p.species());
                }
            }
            h.assertTrue(cuttings == 1, "one cutting: " + cuttings);
        } finally {
            ServerConfig.AQUATIC_BOTTLE_CHANCE.set(saved);
            ServerConfig.PLANTS_BOTTLE_CHANCE.set(savedDuck);
            sp.getServer().getPlayerList().remove(sp);
        }
        h.succeed();
    }

    /** Shears on a tank without plants change nothing and open no menu. */
    @GameTest(template = EMPTY)
    public static void shearsOnATankWithoutPlants(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 1, 1, 2000);
        region(h, c);
        ServerPlayer sp = playerIn(h, h.getLevel(), h.absolutePos(new BlockPos(2, 1, 0)));
        try {
            use(sp, h.getLevel(), c.getBlockPos(), Direction.NORTH, new ItemStack(Items.SHEARS));
            h.assertTrue(!(sp.containerMenu instanceof TankMenu), "no menu");
            h.assertTrue(c.getTank().waterMb() == 2000, "tank unchanged");
        } finally {
            sp.getServer().getPlayerList().remove(sp);
        }
        h.succeed();
    }
}
