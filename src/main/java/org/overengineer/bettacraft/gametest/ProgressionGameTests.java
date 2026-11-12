package org.overengineer.bettacraft.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig.GiveBook;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.progression.GuideBook;
import org.overengineer.bettacraft.progression.Progress;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;

/** M6: the handbook per config mode (once, drop when the inventory is full, owner-only pickup) and advancements. */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ProgressionGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private ProgressionGameTests() {}

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper h, double x, double z) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        Vec3 at = h.absoluteVec(new Vec3(x, 1, z));
        p.moveTo(at.x, at.y, at.z, 0, 0);
        return p;
    }

    private static void remove(GameTestHelper h, ServerPlayer... players) {
        for (ServerPlayer p : players) h.getLevel().getServer().getPlayerList().remove(p);
    }

    private static int books(ServerPlayer p) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (GuideBook.isBook(s)) n += s.getCount();
        return n;
    }

    private static boolean done(ServerPlayer p, String id) {
        var holder = p.server.getAdvancements().get(BettaCraft.id(id));
        if (holder == null) throw new GameTestAssertException("advancement " + id + " missing");
        return p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static List<ItemEntity> droppedBooks(GameTestHelper h, ServerPlayer p) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(p.blockPosition()).inflate(3),
                e -> e.isAlive() && GuideBook.isBook(e.getItem()));
    }

    @GameTest(template = EMPTY)
    public static void bookOncePerConfigMode(GameTestHelper h) {
        if (!GuideBook.available()) {
            h.assertTrue(GuideBook.createBook().isEmpty(), "no book without GuideME");
            h.succeed();
            return;
        }
        ServerPlayer join = player(h, 2, 2), catcher = player(h, 4, 2), crafter = player(h, 6, 2), never = player(h, 8, 2);
        try {
            // ON_JOIN: only the join trigger, only once
            h.assertTrue(!GuideBook.handle(join, GuideBook.Trigger.CATCH, GiveBook.ON_JOIN), "ON_JOIN ignores catches");
            h.assertTrue(GuideBook.handle(join, GuideBook.Trigger.JOIN, GiveBook.ON_JOIN), "ON_JOIN gives the book on join");
            h.assertTrue(!GuideBook.handle(join, GuideBook.Trigger.JOIN, GiveBook.ON_JOIN), "second join: no second book");
            h.assertTrue(books(join) == 1 && GuideBook.wasGiven(join), "exactly one book, flag set");
            // ON_FIRST_CATCH_OR_CRAFT: catch or craft, not join, once
            h.assertTrue(!GuideBook.handle(catcher, GuideBook.Trigger.JOIN, GiveBook.ON_FIRST_CATCH_OR_CRAFT), "no book on join");
            h.assertTrue(GuideBook.handle(catcher, GuideBook.Trigger.CATCH, GiveBook.ON_FIRST_CATCH_OR_CRAFT), "book with the first catch");
            h.assertTrue(!GuideBook.handle(catcher, GuideBook.Trigger.CRAFT, GiveBook.ON_FIRST_CATCH_OR_CRAFT), "not again when crafting");
            h.assertTrue(books(catcher) == 1, "catcher has one book");
            h.assertTrue(GuideBook.handle(crafter, GuideBook.Trigger.CRAFT, GiveBook.ON_FIRST_CATCH_OR_CRAFT), "book with the first craft");
            h.assertTrue(!GuideBook.handle(crafter, GuideBook.Trigger.CATCH, GiveBook.ON_FIRST_CATCH_OR_CRAFT), "not again when catching");
            h.assertTrue(books(crafter) == 1, "crafter has one book");
            // NEVER
            for (GuideBook.Trigger t : GuideBook.Trigger.values()) {
                h.assertTrue(!GuideBook.handle(never, t, GiveBook.NEVER), "NEVER gives nothing (" + t + ")");
            }
            h.assertTrue(books(never) == 0 && !GuideBook.wasGiven(never), "NEVER: no book, no flag");
            // the flag survives death (attachment copied on death)
            h.assertTrue(join.getData(org.overengineer.bettacraft.registry.ModAttachments.GUIDE_BOOK).given(), "flag stored");
            h.succeed();
        } finally {
            remove(h, join, catcher, crafter, never);
        }
    }

    @GameTest(template = EMPTY)
    public static void bookDroppedWhenInventoryFullOwnerOnly(GameTestHelper h) {
        if (!GuideBook.available()) {
            h.succeed();
            return;
        }
        ServerPlayer owner = player(h, 10, 10), other = player(h, 10, 10);
        try {
            for (int i = 0; i < owner.getInventory().items.size(); i++) owner.getInventory().items.set(i, new ItemStack(Items.STONE, 64));
            h.assertTrue(GuideBook.handle(owner, GuideBook.Trigger.CRAFT, GiveBook.ON_FIRST_CATCH_OR_CRAFT), "delivered by dropping");
            h.assertTrue(GuideBook.wasGiven(owner), "flag set after the drop");
            List<ItemEntity> drops = droppedBooks(h, owner);
            h.assertTrue(drops.size() == 1, "one book at the feet, found " + drops.size());
            ItemEntity book = drops.getFirst();
            h.assertTrue(owner.getUUID().equals(book.getTarget()), "only the owner may pick it up");
            h.assertTrue(book.getPersistentData().getLong(GuideBook.OWNER_UNTIL) > h.getLevel().getGameTime(), "owner-only time stored");
            // another player cannot take it
            book.playerTouch(other);
            h.assertTrue(book.isAlive() && books(other) == 0, "other player cannot pick the book up within 60 s");
            // the owner can, once there is room
            owner.getInventory().items.set(0, ItemStack.EMPTY);
            book.playerTouch(owner);
            h.assertTrue(!book.isAlive() && books(owner) == 1, "owner picks the book up");

            // after 60 s anyone may take it
            GuideBook.deliver(owner, GuideBook.createBook()); // inventory is full again
            ItemEntity second = droppedBooks(h, owner).getFirst();
            second.playerTouch(other);
            h.assertTrue(second.isAlive(), "still owner-only");
            second.getPersistentData().putLong(GuideBook.OWNER_UNTIL, h.getLevel().getGameTime() - 1);
            second.playerTouch(other);
            h.assertTrue(!second.isAlive() && books(other) == 1, "after the owner-only time the other player picks it up");
            h.succeed();
        } finally {
            remove(h, owner, other);
        }
    }

    @GameTest(template = EMPTY)
    public static void advancementsTrigger(GameTestHelper h) {
        ServerPlayer p = player(h, 12, 12);
        try {
            // catching a betta with a water bucket (vanilla bucket pickup through BettaEntity.mobInteract)
            BettaEntity betta = ModEntities.BETTA.get().create(h.getLevel());
            if (betta == null) throw new GameTestAssertException("no betta");
            BlockPos water = h.absolutePos(new BlockPos(12, 1, 14));
            h.getLevel().setBlockAndUpdate(water, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
            betta.moveTo(water.getX() + 0.5, water.getY() + 0.3, water.getZ() + 0.5, 0, 0);
            betta.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(water), MobSpawnType.BUCKET, null);
            h.getLevel().addFreshEntity(betta);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            h.assertTrue(!done(p, "catch_betta"), "not caught yet");
            p.interactOn(betta, InteractionHand.MAIN_HAND);
            h.assertTrue(betta.isRemoved(), "betta in the bucket");
            h.assertTrue(done(p, "catch_betta"), "advancement catch_betta");
            h.assertTrue(done(p, "root"), "root advancement (caught)");
            if (GuideBook.available() && GuideBook.mode() == GiveBook.ON_FIRST_CATCH_OR_CRAFT) {
                h.assertTrue(books(p) == 1, "default mode: the first catch brings the handbook");
            }

            // tank events go to the owner: build, fill, nest
            for (int x = 0; x < 2; x++) {
                BlockPos bp = new BlockPos(2 + x, 1, 2);
                h.setBlock(bp, ModBlocks.TANK.get().defaultBlockState());
                TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(bp), p);
            }
            TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 2)));
            if (c == null) throw new GameTestAssertException("tank did not form");
            c.getTank().setOwner(p.getUUID());
            Progress.award(p, Progress.TANK_BUILT, c.getTank().memberCount());
            h.assertTrue(done(p, "build_tank") && !done(p, "big_tank"), "small tank: build_tank only");
            Progress.award(p, Progress.TANK_BUILT, 64);
            h.assertTrue(done(p, "big_tank"), "64 blocks: big_tank");
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            TankInteractions.useItemOn(p.getMainHandItem(), h.getLevel(), h.absolutePos(new BlockPos(2, 1, 2)), p, InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 1, 2))), net.minecraft.core.Direction.UP,
                            h.absolutePos(new BlockPos(2, 1, 2)), false));
            h.assertTrue(c.getTank().waterMb() == 1000 && done(p, "fill_tank"), "half full: fill_tank");
            p.moveTo(h.absoluteVec(new Vec3(30, 1, 30)).add(500, 0, 500)); // far away: tank events still reach the owner
            TankCare.log(h.getLevel(), c, "log.bettacraft.nest_built", "Rex");
            h.assertTrue(done(p, "nest_built"), "nest_built via the tank log (owner)");
            h.assertTrue(!done(p, "spawned"), "spawned not yet");
            h.succeed();
        } finally {
            remove(h, p);
        }
    }
}
