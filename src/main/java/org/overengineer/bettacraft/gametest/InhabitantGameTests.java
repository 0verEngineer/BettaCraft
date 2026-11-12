package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankTemperature;

/** Inhabitants, caps, lid, break rule and temperature (run with {@code gradlew runGameTestServer}). */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InhabitantGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private InhabitantGameTests() {}

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
        TankInteractions.changeWater(h.getLevel(), c, water);
        return c;
    }

    private static ItemStack bettaBucket(long seed) {
        BettaGenome g = BettaGenome.randomDomestic(RandomSource.create(seed));
        return BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), BettaData.create(g, seed % 2 == 0, 0));
    }

    private static Component insert(GameTestHelper h, TankBlockEntity c, ItemStack stack, Player p) {
        EntityType<?> type = TankInhabitants.containedType(stack);
        if (type == null) throw new GameTestAssertException("no entity type in " + stack);
        return TankInhabitants.insert(h.getLevel(), c, stack, type, p, stack.getItem() instanceof FishBagItem);
    }

    private static void ok(Component error) {
        if (error != null) throw new GameTestAssertException("insert failed: " + error.getString());
    }

    @GameTest(template = EMPTY)
    public static void insertBettaFromBucketKeepsData(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 2, 2, 5000);
        ItemStack bucket = bettaBucket(7);
        BettaData expected = bucket.get(ModDataComponents.FISH_DATA.get());
        ok(insert(h, c, bucket, h.makeMockPlayer(GameType.SURVIVAL)));
        h.assertTrue(c.getTank().waterMb() == 6000, "bucket adds 1000 mB, got " + c.getTank().waterMb());
        List<Entity> live = TankInhabitants.liveInhabitants(h.getLevel(), c.getTank());
        h.assertTrue(live.size() == 1 && live.getFirst() instanceof BettaEntity, "one betta inside");
        BettaEntity b = (BettaEntity) live.getFirst();
        h.assertTrue(b.getBettaData().genome().equals(expected.genome()), "genome survives the bucket");
        h.assertTrue(b.getBettaData().fishId().equals(expected.fishId()), "fish id survives the bucket");
        h.assertTrue(c.getTank().contains(b.blockPosition()), "spawned inside the tank");
        h.assertTrue(TankInhabitants.hasTankRef(b) && b.isPersistenceRequired(), "tank_ref + persistent");
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void vanillaFishBucketAndBagAreAccepted(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 3, 2, 3, 12000);
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        ok(insert(h, c, new ItemStack(Items.TROPICAL_FISH_BUCKET), p));
        ok(insert(h, c, new ItemStack(Items.AXOLOTL_BUCKET), p));
        h.assertTrue(c.getTank().waterMb() == 14000, "two buckets add 2000 mB");
        // a fish bag: catch the tropical fish into a bag and insert it again (+100 mB)
        Entity fish = TankInhabitants.liveInhabitants(h.getLevel(), c.getTank()).stream()
                .filter(e -> e.getType() == EntityType.TROPICAL_FISH).findFirst().orElseThrow();
        ItemStack bag = FishBagItem.fillWith(fish);
        fish.discard();
        h.assertTrue(FishBagItem.isFilled(bag) && bag.getMaxStackSize() == 1, "filled bag does not stack");
        ok(insert(h, c, bag, p));
        h.assertTrue(c.getTank().waterMb() == 14100, "bag adds 100 mB, got " + c.getTank().waterMb());
        h.assertTrue(TankInhabitants.liveInhabitants(h.getLevel(), c.getTank()).size() == 2, "two inhabitants");
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void tooBigAndTooDryAreRejected(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 2, 2, 2000);
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        h.assertTrue(insert(h, c, bettaBucket(1), p) != null, "25% fill is below the 50% minimum");
        TankInteractions.changeWater(h.getLevel(), c, 3000);
        ok(insert(h, c, bettaBucket(1), p));
        h.assertTrue(TankInhabitants.accepts(EntityType.DOLPHIN, null) == TankInhabitants.Acceptance.TOO_BIG, "dolphins are too big");
        h.assertTrue(TankInhabitants.accepts(EntityType.AXOLOTL, null) == TankInhabitants.Acceptance.OK, "axolotl ok");
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void hardCapSixAdultsPerWaterBlock(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 1, 1, 1, 1000);
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        for (int i = 0; i < 6; i++) ok(insert(h, c, bettaBucket(i), p));
        h.assertTrue(insert(h, c, bettaBucket(99), p) != null, "7th adult in one water block is rejected");
        h.assertTrue(TankInhabitants.liveInhabitants(h.getLevel(), c.getTank()).size() == 6, "six inside");
        TankInhabitants.reconcile(h.getLevel(), c);
        h.assertTrue(c.isCrowded(), "6 per block is overcrowded");
        h.assertTrue(!TankInhabitants.isOvercrowded(1, 1.0) && TankInhabitants.isOvercrowded(2, 1.0)
                && !TankInhabitants.isOvercrowded(2, 1.5) && TankInhabitants.isOvercrowded(4, 2.0), "overcrowded from 2 per block");
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void takeOutWithBucketAndBag(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 2, 2, 6000);
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        try {
            ok(insert(h, c, bettaBucket(3), sp));
            ok(insert(h, c, bettaBucket(4), sp));
            List<Entity> live = new ArrayList<>(TankInhabitants.liveInhabitants(h.getLevel(), c.getTank()));
            long w0 = c.getTank().waterMb();
            h.assertTrue(TankInhabitants.takeOut(sp, c, live.get(0).getUUID(), false) != null, "no bucket in inventory -> refused");
            sp.getInventory().add(new ItemStack(Items.BUCKET));
            sp.getInventory().add(new ItemStack(ModItems.FISH_BAG.get()));
            h.assertTrue(TankInhabitants.takeOut(sp, c, live.get(0).getUUID(), false) == null, "bucket take-out");
            h.assertTrue(c.getTank().waterMb() == w0 - 1000, "bucket removes 1000 mB");
            h.assertTrue(TankInhabitants.takeOut(sp, c, live.get(1).getUUID(), true) == null, "bag take-out");
            h.assertTrue(c.getTank().waterMb() == w0 - 1100, "bag removes 100 mB");
            h.assertTrue(TankInhabitants.liveInhabitants(h.getLevel(), c.getTank()).isEmpty(), "tank empty");
            boolean bucket = false, bag = false;
            for (ItemStack s : sp.getInventory().items) {
                if (s.is(ModItems.BETTA_BUCKET.get()) && s.has(ModDataComponents.FISH_DATA.get())) bucket = true;
                if (s.getItem() instanceof FishBagItem && FishBagItem.isFilled(s) && s.has(ModDataComponents.FISH_DATA.get())) bag = true;
            }
            h.assertTrue(bucket && bag, "player got a betta bucket and a filled bag");
            h.succeed();
        } finally {
            sp.getServer().getPlayerList().remove(sp);
        }
    }

    @GameTest(template = EMPTY)
    public static void lidOnlyForInhabitants(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 1, 2, 1, 1500);
        ok(insert(h, c, bettaBucket(5), h.makeMockPlayer(GameType.SURVIVAL)));
        BettaEntity fish = (BettaEntity) TankInhabitants.liveInhabitants(h.getLevel(), c.getTank()).getFirst();
        BlockPos top = h.absolutePos(new BlockPos(2, 2, 2));
        BlockState s = h.getLevel().getBlockState(top);
        VoxelShape probe = Shapes.box(0.4, 0.95, 0.4, 0.6, 0.99, 0.6);
        VoxelShape forFish = s.getCollisionShape(h.getLevel(), top, CollisionContext.of(fish));
        VoxelShape forPlayer = s.getCollisionShape(h.getLevel(), top, CollisionContext.of(h.makeMockPlayer(GameType.SURVIVAL)));
        VoxelShape forWild = s.getCollisionShape(h.getLevel(), top, CollisionContext.of(h.spawn(EntityType.COD, new BlockPos(10, 2, 10))));
        h.assertTrue(Shapes.joinIsNotEmpty(forFish, probe, BooleanOp.AND), "closed top for inhabitants");
        h.assertTrue(!Shapes.joinIsNotEmpty(forPlayer, probe, BooleanOp.AND), "open top for players");
        h.assertTrue(!Shapes.joinIsNotEmpty(forWild, probe, BooleanOp.AND), "open top for other entities");
        // launch the fish upwards: it must stay inside the tank
        fish.moveTo(fish.getX(), h.absolutePos(new BlockPos(2, 2, 2)).getY() + 0.5, fish.getZ());
        fish.setDeltaMovement(0, 1.5, 0);
        h.runAfterDelay(15, () -> {
            h.assertTrue(fish.getY() < top.getY() + 1.0, "fish stayed below the lid, y=" + fish.getY());
            h.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void breakingBlockedWhileInhabited(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 1, 1, 2000);
        BlockPos rel = new BlockPos(3, 1, 2);
        BlockPos abs = h.absolutePos(rel);
        ServerLevel level = h.getLevel();
        h.assertTrue(TankInhabitants.breakBlockedReason(level, abs).isEmpty(), "empty tank may be broken");
        ok(insert(h, c, bettaBucket(6), h.makeMockPlayer(GameType.SURVIVAL)));
        h.assertTrue(TankInhabitants.breakBlockedReason(level, abs).isPresent(), "blocked with a fish inside");
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        BlockState s = level.getBlockState(abs);
        boolean removed = s.onDestroyedByPlayer(level, abs, p, true, level.getFluidState(abs));
        h.assertTrue(!removed && level.getBlockState(abs).is(ModBlocks.TANK.get()), "block stays");
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void temperatureFollowsAmbientAndHeaters(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 2, 2, 8000);
        ServerLevel level = h.getLevel();
        TankData t = c.getTank();
        float ambient = TankCare.ambient(level, t);
        t.setWaterTemp(5.0F);
        t.setTargetTemp(30.0F);
        TankCare.updateTemperature(level, c, 10.0);
        h.assertTrue(Math.abs(t.waterTemp() - ambient) < 0.05, "without heater the water takes the ambient temperature " + ambient + ", got " + t.waterTemp());
        TankCare.updateTemperature(level, c, 0.0);
        t.legacyHeaters().setItem(0, new ItemStack(ModItems.HEATER_SMALL.get())); // 16 blocks >= 8 water blocks
        TankCare.updateTemperature(level, c, 10.0);
        float expected = Math.max(ambient, 30.0F);
        h.assertTrue(Math.abs(t.waterTemp() - expected) < 0.05, "heater reaches the target, got " + t.waterTemp());
        float before = 5.0F;
        t.setWaterTemp(before);
        TankCare.updateTemperature(level, c, 0.05);
        h.assertTrue(t.waterTemp() > before && t.waterTemp() < expected, "gradual approach");
        h.assertTrue(TankTemperature.isTolerable(30.0F), "30 is tolerable");
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void feedingResetsHungerAndBucketMirrorsData(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 2, 2, 6000);
        ok(insert(h, c, bettaBucket(8), h.makeMockPlayer(GameType.SURVIVAL)));
        BettaEntity b = (BettaEntity) TankInhabitants.liveInhabitants(h.getLevel(), c.getTank()).getFirst();
        b.health().step(3.0, new org.overengineer.bettacraft.entity.betta.BettaHealth.Conditions(false, false),
                org.overengineer.bettacraft.entity.betta.BettaHealth.Params.DEFAULT);
        h.assertTrue(b.health().daysSinceFed() > 2.9, "hungry");
        int used = TankCare.feed(h.getLevel(), c, new ItemStack(ModItems.BETTA_PELLETS.get(), 5), 5);
        h.assertTrue(used == 1 && b.health().daysSinceFed() == 0, "one pellet feeds the fish");
        // entity NBT and bucket data use the identical key and content
        CompoundTag nbt = new CompoundTag();
        b.addAdditionalSaveData(nbt);
        ItemStack bucket = new ItemStack(ModItems.BETTA_BUCKET.get());
        b.saveToBucketTag(bucket);
        CompoundTag bucketTag = bucket.get(DataComponents.BUCKET_ENTITY_DATA).copyTag();
        h.assertTrue(nbt.getCompound(BettaData.KEY).equals(bucketTag.getCompound(BettaData.KEY)), "same data under key 'betta'");
        h.assertTrue(BettaData.load(nbt.getCompound(BettaData.KEY)).equals(bucket.get(ModDataComponents.FISH_DATA.get())), "component equals NBT");
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void wildBettaSpawnsWithoutNeeds(GameTestHelper h) {
        BettaEntity b = h.spawn(ModEntities.BETTA.get(), new BlockPos(5, 2, 5));
        h.runAfterDelay(30, () -> {
            h.assertTrue(!b.isInTank() && b.health().daysSinceFed() == 0, "no timers outside a tank");
            h.succeed();
        });
    }

    /** M8: Shift + right-click with a filled bucket inserts the fish instead of releasing it in front of the tank. */
    @GameTest(template = EMPTY)
    public static void sneakClickWithBucketInserts(GameTestHelper h) {
        TankBlockEntity c = tank(h, 4, 1, 4, 2, 2, 2, 6000);
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        try {
            net.minecraft.world.phys.Vec3 eye = net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(4, 1, 2)));
            sp.teleportTo(h.getLevel(), eye.x, eye.y - 1.0, eye.z, 0F, 0F);
            sp.setShiftKeyDown(true);
            h.assertTrue(sp.isSecondaryUseActive(), "player sneaks");
            for (ItemStack stack : List.of(new ItemStack(Items.TROPICAL_FISH_BUCKET), bettaBucket(11))) {
                sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
                BlockPos face = h.absolutePos(new BlockPos(4, 1, 4));
                var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(face).add(0, 0, -0.5),
                        net.minecraft.core.Direction.NORTH, face, false);
                sp.gameMode.useItemOn(sp, h.getLevel(), stack, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            }
            List<Entity> live = TankInhabitants.liveInhabitants(h.getLevel(), c.getTank());
            h.assertTrue(live.size() == 2, "both animals in the tank, got " + live.size());
            var area = new net.minecraft.world.phys.AABB(h.absolutePos(BlockPos.ZERO)).inflate(16);
            int outside = h.getLevel().getEntitiesOfClass(Entity.class, area, e -> (e instanceof net.minecraft.world.entity.animal.WaterAnimal || e instanceof BettaEntity)
                    && !c.getTank().contains(e.blockPosition())).size();
            h.assertTrue(outside == 0, "no animal released outside the tank, got " + outside);
            h.assertTrue(!h.getLevel().getFluidState(h.absolutePos(new BlockPos(4, 1, 3))).isSource(), "no water placed in front of the tank");
        } finally {
            sp.getServer().getPlayerList().remove(sp);
        }
        h.succeed();
    }

    /** M8: a fish caught in the world (here a water bucket) is logged and leaves the inhabitant list at once. */
    @GameTest(template = EMPTY)
    public static void catchInTheWorldIsLoggedAtOnce(GameTestHelper h) {
        TankBlockEntity c = tank(h, 4, 1, 4, 2, 2, 2, 6000);
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        try {
            ok(insert(h, c, new ItemStack(Items.TROPICAL_FISH_BUCKET), sp));
            Entity fish = TankInhabitants.liveInhabitants(h.getLevel(), c.getTank()).getFirst();
            int logs = c.getTank().log().size();
            sp.teleportTo(h.getLevel(), fish.getX(), fish.getY() - 1.0, fish.getZ() - 1.5, 0F, 0F);
            sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            var r = sp.interactOn(fish, net.minecraft.world.InteractionHand.MAIN_HAND);
            h.assertTrue(r.consumesAction() && fish.isRemoved(), "the bucket caught the fish: " + r);
            h.assertTrue(!c.getTank().inhabitants().contains(fish.getUUID()), "no longer an inhabitant");
            h.assertTrue(c.getTank().log().size() == logs + 1
                    && "log.bettacraft.taken_out".equals(c.getTank().log().getLast().key()), "logged as taken out");
        } finally {
            sp.getServer().getPlayerList().remove(sp);
        }
        h.succeed();
    }
}
