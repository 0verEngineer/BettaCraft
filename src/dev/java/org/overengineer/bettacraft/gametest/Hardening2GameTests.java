package org.overengineer.bettacraft.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.mobs.GlassMobs;
import org.overengineer.bettacraft.network.ModNetwork;
import org.overengineer.bettacraft.plumbing.PipeBlock;
import org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModFluids;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/**
 * M33 part d (Hardening 2): one GameTest per bug of the code review of the systems of M21-M32 that a game can show (see
 * meilensteine/M33.md, findings table). The pure ones are unit tests ({@code MonitorModelTest.clientLayoutIsSanitized}).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class Hardening2GameTests {
    private static final String EMPTY = "empty_32x10x32";

    private Hardening2GameTests() {}

    /** A mob in glass converted by vanilla (here: a skeleton in powder snow) keeps the mark: it still drops nothing. */
    @GameTest(template = EMPTY)
    public static void glassMobKeepsItsMarkWhenConverted(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Skeleton s = EntityType.SKELETON.create(level);
        if (s == null) throw new GameTestAssertException("no skeleton");
        Vec3 at = h.absoluteVec(new Vec3(4.5, 2, 4.5));
        s.moveTo(at.x, at.y, at.z, 0, 0);
        level.addFreshEntity(s);
        GlassMobs.prepare(s);
        h.assertTrue(GlassMobs.isGlassMob(s), "marked");
        // what Skeleton#doFreezeConversion does
        Stray n = s.convertTo(EntityType.STRAY, true);
        if (n == null) throw new GameTestAssertException("no conversion");
        net.neoforged.neoforge.event.EventHooks.onLivingConvert(s, n);
        h.assertTrue(GlassMobs.isGlassMob(n), "the stray carries the mark of the skeleton");
        n.kill();
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, n.getBoundingBox().inflate(3));
        h.assertTrue(drops.isEmpty(), "no drops: " + drops.size());
        h.succeed();
    }

    /** "Change now" of a water change controller from the menu runs at most once a second (a client can send it in a loop). */
    @GameTest(template = EMPTY)
    public static void waterChangerChangesNowOncePerSecond(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 2, 2), ModBlocks.WATER_CHANGE_CONTROLLER.get().defaultBlockState());
        if (!(h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(2, 2, 2))) instanceof WaterChangerBlockEntity be)) {
            throw new GameTestAssertException("no water changer");
        }
        long now = h.getLevel().getGameTime();
        h.assertTrue(be.manualChangeAllowed(now), "the first one runs");
        h.assertTrue(!be.manualChangeAllowed(now) && !be.manualChangeAllowed(now + 19), "again within a second: refused");
        h.assertTrue(be.manualChangeAllowed(now + 20), "a second later it runs again");
        h.succeed();
    }

    /** Decoration template requests are answered at most 32 times per player and second. */
    @GameTest(template = EMPTY)
    public static void decorTemplateRequestsAreRateLimited(GameTestHelper h) {
        ServerPlayer p = MockPlayers.serverPlayer(h);
        try {
            int ok = 0;
            for (int i = 0; i < 100; i++) if (ModNetwork.templateRequestAllowed(p)) ok++;
            h.assertTrue(ok == ModNetwork.TEMPLATE_REQUESTS_PER_SECOND, "answered " + ok + " of 100 in one second");
            h.succeed();
        } finally {
            p.getServer().getPlayerList().remove(p);
        }
    }

    /** A pipe asked for one fluid (drain with a FluidStack) gives only that fluid, not the water of the tank at its end. */
    @GameTest(template = EMPTY)
    public static void pipeDrainGivesOnlyTheAskedFluid(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity t = AerationGameTests.tank(h, 1, 2, 1, 2, 2, 2);
        h.setBlock(new BlockPos(3, 2, 1), ModBlocks.GLASS_PIPE.get().defaultBlockState());
        BlockPos pipe = h.absolutePos(new BlockPos(3, 2, 1));
        h.assertTrue(level.getBlockState(pipe).getValue(PipeBlock.SIDES.get(Direction.WEST)), "the pipe joins the tank");
        IFluidHandler f = level.getCapability(Capabilities.FluidHandler.BLOCK, pipe, Direction.EAST);
        if (f == null) throw new GameTestAssertException("no fluid handler on the pipe");
        h.assertTrue(t.getTank().waterMb() > 0, "the tank has water");
        FluidStack sea = f.drain(new FluidStack(ModFluids.SEA_WATER.get(), 100), IFluidHandler.FluidAction.SIMULATE);
        h.assertTrue(sea.isEmpty(), "asked for sea water, got " + sea.getAmount() + " mB of " + sea.getFluid());
        FluidStack water = f.drain(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.SIMULATE);
        h.assertTrue(!water.isEmpty() && water.getFluid().isSame(Fluids.WATER), "asked for water, got it: " + water.getAmount());
        h.succeed();
    }
}
