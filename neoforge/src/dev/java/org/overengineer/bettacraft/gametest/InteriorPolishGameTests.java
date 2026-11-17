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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorEvents;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;

/**
 * M15 GameTests: a tank cannot be broken (by a player, any block of a free shape, or an explosion) while a player is in
 * its interior; leaving the interior needs sneaking AND looking up, and placing or breaking starts the timer over.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InteriorPolishGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private InteriorPolishGameTests() {}

    private static TankBlockEntity tank(GameTestHelper h, List<BlockPos> rel) {
        for (BlockPos p : rel) h.setBlock(p, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos p : rel) TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(p), null);
        TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(rel.getFirst()));
        if (c == null || c.getTank().memberCount() != rel.size()) throw new GameTestAssertException("tank did not form");
        return c;
    }

    private static InteriorRegion region(GameTestHelper h, TankBlockEntity c) {
        if (InteriorManager.interiorLevel(h.getLevel().getServer()) == null) throw new GameTestAssertException("no interior dimension");
        InteriorRegion r = InteriorManager.ensureRegion(h.getLevel(), c);
        h.assertTrue(r.built(), "region built");
        return r;
    }

    private static boolean allTank(GameTestHelper h, List<BlockPos> rel) {
        for (BlockPos p : rel) if (!h.getBlockState(p).is(ModBlocks.TANK.get())) return false;
        return true;
    }

    /** 2.1: a player inside keeps every block of an L-shaped tank from being broken, by hand and by an explosion. */
    @GameTest(template = EMPTY)
    public static void tankWithPlayerInsideCannotBeBroken(GameTestHelper h) {
        List<BlockPos> l = List.of(new BlockPos(10, 1, 10), new BlockPos(11, 1, 10), new BlockPos(10, 1, 11));
        TankBlockEntity c = tank(h, l);
        region(h, c);
        ServerPlayer inside = MockPlayers.serverPlayer(h), outside = MockPlayers.serverPlayer(h);
        try {
            Vec3 start = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(10, 1, 7)));
            inside.teleportTo(h.getLevel(), start.x, start.y, start.z, java.util.Set.of(), 0, 0, false);
            InteriorManager.enter(inside, c.getBlockPos());
            h.assertTrue(InteriorManager.isInterior(inside.level()), "the first player is in the interior");
            TankData t = c.getTank();
            h.assertTrue(InteriorManager.playersInside(h.getLevel().getServer(), t).size() == 1, "one player counted inside");
            h.assertTrue(TankInhabitants.breakBlockedReason(h.getLevel(), c.getBlockPos()).map(m -> m.getString().contains(inside.getGameProfile().name()))
                    .orElse(false), "the reason names the player inside");
            // the clients learn about it too (keeps them from predicting the break)
            InteriorManager.updateOccupants(h.getLevel().getServer());
            h.assertTrue(t.occupants() == 1, "occupants counted: " + t.occupants());
            TankData view = TankData.loadClientView(t.saveClientView());
            h.assertTrue(view.occupants() == 1, "occupants in the client view");
            // break every block of the free shape like a player
            outside.teleportTo(h.getLevel(), start.x, start.y, start.z, java.util.Set.of(), 0, 0, false);
            for (BlockPos p : l) {
                outside.gameMode.destroyBlock(h.absolutePos(p)); // (true in creative even when nothing happened)
                h.assertTrue(h.getBlockState(p).is(ModBlocks.TANK.get()), "block " + p + " refused");
            }
            h.assertTrue(allTank(h, l) && c.getTank().memberCount() == 3, "the tank is whole");
            // an explosion next to it: the glass block beside it breaks, the tank stays
            h.setBlock(new BlockPos(12, 1, 11), Blocks.GLASS.defaultBlockState());
            Vec3 boom = Vec3.atCenterOf(h.absolutePos(new BlockPos(12, 1, 12)));
            h.getLevel().explode(null, boom.x, boom.y, boom.z, 3.0F, Level.ExplosionInteraction.TNT);
            h.assertTrue(h.getBlockState(new BlockPos(12, 1, 11)).isAir(), "the explosion broke the glass next to the tank");
            h.assertTrue(allTank(h, l), "the explosion left the tank alone");
            // the player leaves: now it can be broken
            InteriorManager.leave(inside);
            h.assertTrue(InteriorManager.playersInside(h.getLevel().getServer(), t).isEmpty(), "nobody inside");
            InteriorManager.updateOccupants(h.getLevel().getServer());
            h.assertTrue(t.occupants() == 0, "occupants back to 0");
            h.assertTrue(TankInhabitants.breakBlockedReason(h.getLevel(), c.getBlockPos()).isEmpty(), "no reason any more");
            outside.gameMode.destroyBlock(h.absolutePos(l.get(2)));
            h.assertTrue(h.getBlockState(l.get(2)).isAir(), "broken once the player is out");
            h.succeed();
        } finally {
            inside.getServer().getPlayerList().remove(inside);
            outside.getServer().getPlayerList().remove(outside);
        }
    }

    /** 4: only sneaking while looking up counts; placing and breaking start over; then the player is back in front. */
    @GameTest(template = EMPTY)
    public static void leaveNeedsSneakAndLookingUp(GameTestHelper h) {
        TankBlockEntity c = tank(h, List.of(new BlockPos(4, 1, 4)));
        InteriorRegion r = region(h, c);
        ServerPlayer p = MockPlayers.serverPlayer(h);
        try {
            Vec3 start = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(4, 1, 2)));
            p.teleportTo(h.getLevel(), start.x, start.y, start.z, java.util.Set.of(), 0, 0, false);
            InteriorManager.enter(p, c.getBlockPos());
            h.assertTrue(InteriorManager.isInterior(p.level()), "inside");
            ServerLevel in = (ServerLevel) p.level();
            int needed = ServerConfig.getOr(ServerConfig.INTERIOR_LEAVE_SNEAK_TICKS, 60);
            p.setShiftKeyDown(true);
            // sneaking and looking at the floor (to set substrate): never leaves
            p.setXRot(45F);
            for (int i = 0; i < needed + 20; i++) h.assertFalse(InteriorEvents.leaveTick(p), "sneaking while looking down does not leave");
            p.setXRot(-30F);
            for (int i = 0; i < needed + 20; i++) h.assertFalse(InteriorEvents.leaveTick(p), "looking up a little is not enough");
            h.assertTrue(InteriorEvents.leaveTicks(p) == 0, "the timer did not run");
            // looking steeply up counts
            p.setXRot(-75F);
            for (int i = 0; i < needed - 5; i++) h.assertFalse(InteriorEvents.leaveTick(p), "not yet");
            h.assertTrue(InteriorEvents.leaveTicks(p) == needed - 5, "the timer runs: " + InteriorEvents.leaveTicks(p));
            // placing a block like a player starts over
            BlockPos floor = r.toWorld(5, 0, 5).below();
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 4));
            BlockPos stoneAt = floor.above();
            in.setBlockAndUpdate(stoneAt, Blocks.STONE.defaultBlockState());
            p.gameMode.useItemOn(p, in, p.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(stoneAt).add(0, 0.5, 0), Direction.UP, stoneAt, false));
            h.assertTrue(in.getBlockState(stoneAt.above()).is(Blocks.STONE), "the block was placed");
            h.assertTrue(InteriorEvents.leaveTicks(p) == 0, "placing reset the timer");
            for (int i = 0; i < needed - 5; i++) InteriorEvents.leaveTick(p);
            h.assertTrue(InteriorEvents.leaveTicks(p) == needed - 5, "counting again");
            // breaking a block starts over too
            p.gameMode.destroyBlock(stoneAt.above());
            h.assertTrue(in.getBlockState(stoneAt.above()).isAir(), "the block was broken");
            h.assertTrue(InteriorEvents.leaveTicks(p) == 0, "breaking reset the timer");
            // releasing sneak starts over
            for (int i = 0; i < 10; i++) InteriorEvents.leaveTick(p);
            p.setShiftKeyDown(false);
            InteriorEvents.leaveTick(p);
            h.assertTrue(InteriorEvents.leaveTicks(p) == 0, "releasing sneak reset the timer");
            p.setShiftKeyDown(true);
            boolean left = false;
            for (int i = 0; i < needed && !left; i++) left = InteriorEvents.leaveTick(p);
            h.assertTrue(left && p.level() == h.getLevel(), "sneaking and looking up for " + needed + " ticks brings the player out");
            h.assertTrue(p.position().distanceTo(start) < 0.5, "back in front of the tank");
            h.succeed();
        } finally {
            p.getServer().getPlayerList().remove(p);
        }
    }

    /** 4: with {@code interior.leaveLookUpDegrees = 0} sneaking alone is enough again. */
    @GameTest(template = EMPTY, batch = "bettacraft_leave_look_config")
    public static void leaveLookUpCanBeTurnedOff(GameTestHelper h) {
        TankBlockEntity c = tank(h, List.of(new BlockPos(4, 1, 4)));
        region(h, c);
        ServerPlayer p = MockPlayers.serverPlayer(h);
        int saved = ServerConfig.INTERIOR_LEAVE_LOOK_UP.get();
        try {
            ServerConfig.INTERIOR_LEAVE_LOOK_UP.set(0);
            InteriorManager.enter(p, c.getBlockPos());
            h.assertTrue(InteriorManager.isInterior(p.level()), "inside");
            p.setShiftKeyDown(true);
            p.setXRot(40F);
            boolean left = false;
            int needed = ServerConfig.getOr(ServerConfig.INTERIOR_LEAVE_SNEAK_TICKS, 60);
            for (int i = 0; i < needed && !left; i++) left = InteriorEvents.leaveTick(p);
            h.assertTrue(left && !InteriorManager.isInterior(p.level()), "sneaking alone leaves with the look-up rule off");
            h.succeed();
        } finally {
            ServerConfig.INTERIOR_LEAVE_LOOK_UP.set(saved);
            p.getServer().getPlayerList().remove(p);
        }
    }
}
