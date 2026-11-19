package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.PackedTank;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMoves;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;

/**
 * M42 part b: dupes and abuse. Two players take the same animal out in the same tick (one bag fills, not two); a packed tank
 * item copied (a creative copy) and placed twice (the animals and small animals come back once, not twice, and the second
 * copy does not share the interior of the first); a block of a tank broken, put back and broken again at once (its part of
 * the interior drops once).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DupeGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private DupeGameTests() {}

    private static int filledBags(ServerPlayer p) {
        int n = 0;
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) if (s.is(ModItems.FISH_BAG.get()) && TankInhabitants.containedType(s) != null) n += s.getCount();
        return n;
    }

    /** Two players with an empty fish bag each take the same betta out in the same tick: one bag holds it, the other gets a message. */
    @GameTest(template = EMPTY, batch = "bettacraft_m42_dupes", timeoutTicks = 100)
    public static void twoPlayersTakingTheSameAnimalOutGetOneBag(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = ForeignGameTests.tank(h, 2, 2, 3, 2, 3, 26F);
        UUID betta = LiveFoodGameTests.betta(h, c, false, 42_21L).getUUID();
        ServerPlayer a = MockPlayers.serverPlayer(h), b = MockPlayers.serverPlayer(h);
        try {
            a.getInventory().setItem(0, new ItemStack(ModItems.FISH_BAG.get()));
            b.getInventory().setItem(0, new ItemStack(ModItems.FISH_BAG.get()));
            Component first = TankInhabitants.takeOut(a, c, betta, true), second = TankInhabitants.takeOut(b, c, betta, true);
            h.assertTrue(first == null, "the first takes it: " + (first == null ? "" : first.getString()));
            h.assertTrue(second != null, "the second gets a message, not a second betta");
            h.assertTrue(filledBags(a) + filledBags(b) == 1, "one filled bag: " + filledBags(a) + " + " + filledBags(b));
            Entity e = level.getEntity(betta);
            h.assertTrue(e == null || !e.isAlive(), "the betta left the tank");
            h.assertTrue(!c.getTank().inhabitants().contains(betta), "and its list");
        } finally {
            a.discard();
            b.discard();
        }
        h.succeed();
    }

    /** A packed tank item and a copy of it placed: the bettas and snails come back once, the copy gets no share of the interior. */
    @GameTest(template = EMPTY, batch = "bettacraft_m42_dupes", timeoutTicks = 200)
    public static void aCopiedPackedTankDoesNotDoubleItsAnimals(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = ForeignGameTests.tank(h, 2, 2, 1, 1, 1, 26F);
        List<UUID> bettas = List.of(LiveFoodGameTests.betta(h, c, false, 42_22L).getUUID());
        TankCritters.add(level, c, CritterSpecies.ZEBRA_NERITE, CritterGenes.WILD, true, 10);
        TankCritters.add(level, c, CritterSpecies.ZEBRA_NERITE, CritterGenes.WILD, false, 10);
        InteriorRegion region = InteriorManager.ensureRegion(level, c);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        BlockPos chestAt = region.toWorld(4, 0, 4);
        in.setBlock(chestAt, Blocks.CHEST.defaultBlockState(), 3);
        if (in.getBlockEntity(chestAt) instanceof ChestBlockEntity chest) chest.setItem(0, new ItemStack(Items.DIAMOND, 7));
        UUID tankId = c.getTank().id();
        // what a mover holds when it cannot put the tank down (CreateLandings): the controller's data with the packed animals
        List<Entity> out = new ArrayList<>();
        ListTag stowed = TankMoves.pack(level, c.getTank(), out);
        for (Entity e : out) e.discard();
        CompoundTag data = c.saveWithoutMetadata(level.registryAccess());
        data.put("stowed", stowed);
        BlockPos old = c.getBlockPos();
        level.removeBlockEntity(old); // taken by the mover: no removal of the tank
        level.setBlock(old, Blocks.AIR.defaultBlockState(), 3);
        ItemStack packed = PackedTank.pack(new ItemStack(ModBlocks.TANK.get()), data);
        ItemStack copy = packed.copy();
        BlockPos at1 = h.absolutePos(new BlockPos(10, 1, 4)), at2 = h.absolutePos(new BlockPos(20, 1, 4));
        ServerPlayer p = MockPlayers.serverPlayer(h);
        try {
            for (var pair : List.of(java.util.Map.entry(at1, packed), java.util.Map.entry(at2, copy))) {
                level.setBlockAndUpdate(pair.getKey(), Blocks.STONE.defaultBlockState());
                p.setItemInHand(InteractionHand.MAIN_HAND, pair.getValue());
                pair.getValue().useOn(new UseOnContext(level, p, InteractionHand.MAIN_HAND, pair.getValue(),
                        new BlockHitResult(Vec3.atCenterOf(pair.getKey()).add(0, 0.5, 0), Direction.UP, pair.getKey(), false)));
            }
        } finally {
            p.discard();
        }
        h.runAfterDelay(60, () -> {
            TankBlockEntity a = TankMultiblock.getController(level, at1.above()), b = TankMultiblock.getController(level, at2.above());
            h.assertTrue(a != null && a.getTank().id().equals(tankId), "the first packed tank stands with its id");
            for (UUID u : bettas) {
                Entity e = level.getEntity(u);
                h.assertTrue(e != null && e.isAlive(), "the betta is back");
                long n = level.getEntities((Entity) null, new AABB(at1).inflate(40), x -> x.getUUID().equals(u)).size();
                h.assertTrue(n == 1, "exactly once");
            }
            int snails = a.getTank().critters().size() + (b == null ? 0 : b.getTank().critters().size());
            h.assertTrue(snails == 2, "the two snails exist once: " + snails);
            if (b != null) {
                h.assertTrue(!b.getTank().id().equals(tankId), "the copy is not a second tank with the same id");
                h.assertTrue(b.getTank().regionId() != a.getTank().regionId(), "the copy does not share the interior");
            }
            h.succeed();
        });
    }

    /**
     * A creative copy of a standing tank block (pick block with its data: the controller's tank data in the item) placed beside it:
     * a new plain tank, not a second one with the same id, interior, small animals or bettas.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m42_dupes", timeoutTicks = 100)
    public static void aCreativeCopyOfAStandingTankIsAPlainTank(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = ForeignGameTests.tank(h, 2, 2, 1, 1, 1, 26F);
        UUID betta = LiveFoodGameTests.betta(h, c, false, 42_23L).getUUID();
        TankCritters.add(level, c, CritterSpecies.ZEBRA_NERITE, CritterGenes.WILD, true, 10);
        int region = InteriorManager.ensureRegion(level, c).id();
        ItemStack copy = new ItemStack(ModBlocks.TANK.get());
        copy.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.TypedEntityData.of(
                org.overengineer.bettacraft.registry.ModBlockEntities.TANK.get(), c.saveWithoutMetadata(level.registryAccess())));
        BlockPos floor = h.absolutePos(new BlockPos(10, 1, 4));
        level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        ServerPlayer p = MockPlayers.serverPlayer(h);
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND, copy);
            copy.useOn(new UseOnContext(level, p, InteractionHand.MAIN_HAND, copy, new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false)));
        } finally {
            p.discard();
        }
        h.runAfterDelay(20, () -> {
            TankBlockEntity n = TankMultiblock.getController(level, floor.above());
            h.assertTrue(n != null, "the copy forms a tank");
            h.assertTrue(!n.getTank().id().equals(c.getTank().id()), "a new tank, not a second one with the same id");
            h.assertTrue(n.getTank().regionId() != region, "not the same interior");
            h.assertTrue(n.getTank().critters().isEmpty() && n.getTank().inhabitants().isEmpty(), "no small animals or animals of the original");
            h.assertTrue(level.getEntities((Entity) null, new AABB(floor).inflate(30), e -> e.getUUID().equals(betta)).size() == 1, "the betta exists once");
            h.succeed();
        });
    }

    /** One block of a tank broken, put back and broken again in the same tick: its part of the interior (a chest of diamonds) drops once. */
    @GameTest(template = EMPTY, batch = "bettacraft_m42_dupes", timeoutTicks = 100)
    public static void breakingAndPuttingBackABlockDropsItsInteriorOnce(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = CritterGameTests.tank(h, 2, 2, 2, 2, 1, 1, 0);
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        BlockPos chestAt = r.toWorld(16 + 4, 0, 4); // in the part of the second block (x 16..31)
        in.setBlock(chestAt, Blocks.CHEST.defaultBlockState(), 3);
        if (in.getBlockEntity(chestAt) instanceof ChestBlockEntity chest) chest.setItem(0, new ItemStack(Items.DIAMOND, 5));
        BlockPos second = h.absolutePos(new BlockPos(3, 2, 2));
        for (int i = 0; i < 2; i++) {
            level.destroyBlock(second, false);
            level.setBlock(second, ModBlocks.TANK.get().defaultBlockState(), 3);
            TankMultiblock.onTankBlockAdded(level, second, null);
        }
        level.destroyBlock(second, false);
        int diamonds = 0;
        for (ItemEntity ie : level.getEntitiesOfClass(ItemEntity.class, new AABB(second).inflate(4))) {
            ItemStack s = ie.getItem();
            if (s.is(Items.DIAMOND)) diamonds += s.getCount();
            var cont = s.get(net.minecraft.core.component.DataComponents.CONTAINER);
            if (cont != null) for (var x : cont.nonEmptyItems()) if (x.is(Items.DIAMOND)) diamonds += x.count();
        }
        if (diamonds != 5) throw new GameTestFailure("the chest's 5 diamonds came out as " + diamonds);
        h.succeed();
    }
}
