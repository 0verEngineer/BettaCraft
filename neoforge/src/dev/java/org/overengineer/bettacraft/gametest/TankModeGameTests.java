package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import org.overengineer.bettacraft.util.Perms;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.network.TankModePayload;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankFluid;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tank.menu.TankMenu;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.overengineer.bettacraft.terrarium.TankModeSetting;
import org.overengineer.bettacraft.terrarium.TerrariumMode;
import org.overengineer.bettacraft.terrarium.WaterLimit;

/**
 * M33.1: the tank type switch. A fixed terrarium refuses the water of a bucket and of a pipe (fluid capability); a fixed
 * paludarium takes a pipe's water only up to the height where its land part stays (the part that fits, not a refusal);
 * an automatic tank takes everything and follows its water as before; setting a type that does not fit the water removes
 * nothing (draining still works); the packet of the menu is checked (menu, reach, lock, type, rate limit, switch).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TankModeGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private TankModeGameTests() {}

    // ------------------------------------------------------------------ helpers

    private static ServerLevel interiorLevel(GameTestHelper h) {
        ServerLevel in = InteriorManager.interiorLevel(h.getLevel().getServer());
        if (in == null) throw new GameTestFailure("no interior dimension");
        return in;
    }

    /** A dry 3 x 3 x 3 tank whose interior has a sand floor and dirt land over the half x >= 24 up to 30 of 48 interior blocks (or none). */
    private static TankBlockEntity tank(GameTestHelper h, boolean land) {
        TankBlockEntity c = CritterGameTests.tank(h, 2, 2, 2, 3, 3, 3, 0);
        InteriorRegion r = InteriorManager.ensureRegion(h.getLevel(), c);
        ServerLevel in = interiorLevel(h);
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.SAND.defaultBlockState(), false);
        }
        if (land) {
            for (int x = 24; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) for (int y = 0; y < 30; y++) {
                in.setBlock(r.toWorld(x, y, z), Blocks.DIRT.defaultBlockState(), 3);
            }
        }
        TankClimate.refreshMode(h.getLevel(), c);
        return c;
    }

    private static void set(GameTestHelper h, TankBlockEntity c, TankModeSetting s) {
        c.getTank().setTankMode(s);
        c.limitMb = -1;
        TankClimate.refreshMode(h.getLevel(), c);
    }

    /** A water bucket on the tank; true when it was poured in (the hand holds an empty bucket). */
    private static boolean bucket(GameTestHelper h, TankBlockEntity c) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        BlockPos pos = c.getBlockPos();
        TankInteractions.useItemOn(p.getMainHandItem(), h.getLevel(), pos, p, InteractionHand.MAIN_HAND,
                new BlockHitResult(pos.getCenter(), Direction.UP, pos, false));
        return p.getMainHandItem().is(Items.BUCKET);
    }

    private static int pipe(GameTestHelper h, TankBlockEntity c, int mb, IFluidHandler.FluidAction action) {
        return new TankFluid(h.getLevel(), c.getBlockPos(), Direction.NORTH).fill(new FluidStack(Fluids.WATER, mb), action);
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new GameTestFailure(what);
    }

    // ------------------------------------------------------------------ the water limit

    /** A fixed terrarium: no water from a bucket or a pipe (simulated or not), it stays a terrarium. */
    @GameTest(template = EMPTY)
    public static void fixedTerrariumRefusesBucketAndPipe(GameTestHelper h) {
        TankBlockEntity c = tank(h, false);
        TankData t = c.getTank();
        set(h, c, TankModeSetting.TERRARIUM);
        check(!bucket(h, c), "the bucket stays full");
        check(t.waterMb() == 0, "no water from the bucket: " + t.waterMb());
        check(pipe(h, c, 4000, IFluidHandler.FluidAction.SIMULATE) == 0, "the pipe may not even simulate water in");
        check(pipe(h, c, 4000, IFluidHandler.FluidAction.EXECUTE) == 0 && t.waterMb() == 0, "no water from the pipe: " + t.waterMb());
        check(WaterLimit.room(h.getLevel(), c) == 0, "no room");
        TankClimate.step(h.getLevel(), c, 0.01);
        check(t.terrariumMode() == TerrariumMode.TERRARIUM, "a terrarium: " + t.terrariumMode());
        check(t.hasClimate(), "with its climate");
        check(WaterLimit.message(h.getLevel(), c).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc
                && tc.getKey().equals("message.bettacraft.tank_mode.terrarium_no_water"), "the terrarium message");
        h.succeed();
    }

    /** A fixed paludarium: a pipe fills only up to the height where the land part stays; then neither pipe nor bucket. */
    @GameTest(template = EMPTY)
    public static void fixedPaludariumLimitsTheFillHeight(GameTestHelper h) {
        TankBlockEntity c = tank(h, true);
        TankData t = c.getTank();
        set(h, c, TankModeSetting.PALUDARIUM);
        long max = WaterLimit.maxWaterMb(h.getLevel(), c);
        check(max > 9000 && max < 18000, "the limit lies below the top of the land (30 of 48 interior blocks): " + max);
        int in = pipe(h, c, 30000, IFluidHandler.FluidAction.EXECUTE);
        check(in == max && t.waterMb() == max, "the pipe filled up to the limit: " + in + " of " + max + ", water " + t.waterMb());
        double height = TankCare.waterLocalHeight(t, InteriorManager.ensureRegion(h.getLevel(), c).scale());
        check(height < 30 - 0.05, "the land stays above the water: " + height);
        check(pipe(h, c, 1000, IFluidHandler.FluidAction.EXECUTE) == 0, "nothing more from the pipe");
        check(!bucket(h, c) && t.waterMb() == max, "nor from the bucket");
        Component msg = WaterLimit.message(h.getLevel(), c);
        check(msg.getString().contains(String.valueOf(max)) || msg.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents,
                "the paludarium message: " + msg.getString());
        // the land is still a paludarium's: detected by the water and the land, not only by the type
        TankClimate.step(h.getLevel(), c, 0.01);
        check(t.landShare() >= TankClimate.thresholds().paludarium() - 1e-6, "land share kept: " + t.landShare());
        check(TerrariumMode.detect(t.waterMb(), t.landShare(), TankClimate.thresholds()) == TerrariumMode.PALUDARIUM, "detected as a paludarium too");
        check(t.terrariumMode() == TerrariumMode.PALUDARIUM, "mode " + t.terrariumMode());
        // draining works
        var out = new TankFluid(h.getLevel(), c.getBlockPos(), Direction.NORTH).drain(2000, IFluidHandler.FluidAction.EXECUTE);
        check(out.getAmount() == 2000 && t.waterMb() == max - 2000, "drained " + out.getAmount());
        check(bucket(h, c) && t.waterMb() == max - 1000, "with room again a bucket goes in: " + t.waterMb());
        h.succeed();
    }

    /** Automatic as before: the same tank takes every drop and becomes an aquarium; dry again it is a terrarium. */
    @GameTest(template = EMPTY)
    public static void automaticTankAsBefore(GameTestHelper h) {
        TankBlockEntity c = tank(h, true);
        TankData t = c.getTank();
        check(t.tankMode() == TankModeSetting.AUTO, "a new tank is automatic: " + t.tankMode());
        check(WaterLimit.maxWaterMb(h.getLevel(), c) == Long.MAX_VALUE, "no limit");
        check(bucket(h, c) && t.waterMb() == 1000, "the bucket goes in");
        int in = pipe(h, c, 40000, IFluidHandler.FluidAction.EXECUTE);
        check(in == t.capacityMb() - 1000 && t.waterMb() == t.capacityMb(), "the pipe fills it up: " + in);
        TankClimate.step(h.getLevel(), c, 0.01);
        check(t.terrariumMode() == TerrariumMode.AQUARIUM, "full of water: aquarium, got " + t.terrariumMode());
        TankInteractions.changeWater(h.getLevel(), c, -t.waterMb());
        TankClimate.step(h.getLevel(), c, 0.01);
        check(t.terrariumMode() == TerrariumMode.TERRARIUM, "dry: terrarium, got " + t.terrariumMode());
        // the type is saved only when set (old worlds and automatic tanks save nothing new)
        check(!t.save().getCompoundOrEmpty("terrarium").contains("type"), "nothing new saved for an automatic tank");
        h.succeed();
    }

    /** A full aquarium set to terrarium or paludarium: nothing is removed, no more water comes in, draining works, the type survives saving. */
    @GameTest(template = EMPTY)
    public static void switchingTheTypeDeletesNothing(GameTestHelper h) {
        TankBlockEntity c = tank(h, true);
        TankData t = c.getTank();
        TankInteractions.changeWater(h.getLevel(), c, t.capacityMb());
        long full = t.waterMb();
        TankClimate.step(h.getLevel(), c, 0.01);
        check(t.terrariumMode() == TerrariumMode.AQUARIUM, "an aquarium first");
        for (TankModeSetting s : new TankModeSetting[]{TankModeSetting.TERRARIUM, TankModeSetting.PALUDARIUM, TankModeSetting.AQUARIUM, TankModeSetting.AUTO}) {
            set(h, c, s);
            for (int i = 0; i < 5; i++) TankClimate.step(h.getLevel(), c, 0.05);
            check(t.waterMb() == full, s + ": the water stays: " + t.waterMb() + " of " + full);
            if (s.fixed() != null) check(t.terrariumMode() == s.fixed(), s + ": mode " + t.terrariumMode());
        }
        set(h, c, TankModeSetting.TERRARIUM);
        check(pipe(h, c, 1000, IFluidHandler.FluidAction.EXECUTE) == 0, "no more water");
        var out = new TankFluid(h.getLevel(), c.getBlockPos(), Direction.NORTH).drain(3000, IFluidHandler.FluidAction.EXECUTE);
        check(out.getAmount() == 3000 && t.waterMb() == full - 3000, "draining works: " + out.getAmount());
        // saved and loaded: the type stays
        TankData copy = TankData.load(t.save());
        check(copy != null && copy.tankMode() == TankModeSetting.TERRARIUM, "saved type: " + (copy == null ? null : copy.tankMode()));
        h.succeed();
    }

    // ------------------------------------------------------------------ the packet

    /** The menu packet: wrong menu, bad type, rate limit, lock, reach and the switch are refused; a good request sets the type. */
    @GameTest(template = EMPTY, batch = "bettacraft_m331_packet", timeoutTicks = 200)
    public static void tankModePacketIsValidated(GameTestHelper h) {
        TankBlockEntity c = tank(h, false);
        TankData t = c.getTank();
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        boolean before = Features.own(Feature.TANK_MODE);
        BlockPos near = h.absolutePos(new BlockPos(1, 2, 1));
        try {
            sp.teleportTo(h.getLevel(), near.getX() + 0.5, near.getY(), near.getZ() + 0.5, java.util.Set.of(), 0F, 0F, false);
            sp.setGameMode(GameType.SURVIVAL);
            check(TankModePayload.apply(sp, new TankModePayload(0, 1)) == TankModePayload.Result.NO_MENU, "no menu open");
            org.overengineer.bettacraft.platform.Platform.get().openMenu(sp, new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new TankMenu(id, inv, c), Component.literal("tank")),
                    buf -> buf.writeBlockPos(c.getBlockPos()));
            check(sp.containerMenu instanceof TankMenu, "menu open");
            int id = sp.containerMenu.containerId;
            check(TankModePayload.apply(sp, new TankModePayload(id + 1, 1)) == TankModePayload.Result.NO_MENU, "another container id");
            check(TankModePayload.apply(sp, new TankModePayload(id, 9)) == TankModePayload.Result.INVALID, "an unknown type");
            check(TankModePayload.apply(sp, new TankModePayload(id, -1)) == TankModePayload.Result.INVALID, "a negative type");
            check(t.tankMode() == TankModeSetting.AUTO, "still automatic");
            var r = TankModePayload.apply(sp, new TankModePayload(id, TankModeSetting.TERRARIUM.ordinal()));
            check(r == TankModePayload.Result.SET && t.tankMode() == TankModeSetting.TERRARIUM, "a good request: " + r);
            check(TankModePayload.apply(sp, new TankModePayload(id, TankModeSetting.PALUDARIUM.ordinal())) == TankModePayload.Result.THROTTLED
                    && t.tankMode() == TankModeSetting.TERRARIUM, "a second one in the same tick is throttled");
            boolean seen = false;
            for (var l : t.log()) seen |= l.key().equals("log.bettacraft.tank_mode.set");
            check(seen, "the change is in the tank log");
            // switched off: refused (the switch is restored in finally)
            Features.set(Feature.TANK_MODE, false);
            check(WaterLimit.setting(t) == TankModeSetting.AUTO, "off: every tank is automatic");
            check(WaterLimit.room(h.getLevel(), c) == t.capacityMb(), "off: no limit");
            Features.set(Feature.TANK_MODE, before);
            check(t.tankMode() == TankModeSetting.TERRARIUM, "the chosen type is kept while the switch is off");
        } catch (RuntimeException e) {
            Features.set(Feature.TANK_MODE, before);
            sp.level().getServer().getPlayerList().remove(sp);
            throw e;
        }
        h.runAfterDelay(WaterLimit.SET_TICKS + 1, () -> {
            try {
                int id = sp.containerMenu.containerId;
                Features.set(Feature.TANK_MODE, false);
                check(TankModePayload.apply(sp, new TankModePayload(id, TankModeSetting.AUTO.ordinal())) == TankModePayload.Result.OFF, "switch off: refused");
                Features.set(Feature.TANK_MODE, before);
                var r = TankModePayload.apply(sp, new TankModePayload(id, TankModeSetting.PALUDARIUM.ordinal()));
                check(r == TankModePayload.Result.SET && t.tankMode() == TankModeSetting.PALUDARIUM, "after the rate limit: " + r);
                // a locked tank of someone else
                if (!Perms.has(sp, 2)) {
                    UUID other = UUID.randomUUID();
                    t.setOwner(other);
                    t.setLocked(true);
                    c.modeSetTime = Long.MIN_VALUE;
                    check(TankModePayload.apply(sp, new TankModePayload(id, TankModeSetting.AUTO.ordinal())) == TankModePayload.Result.LOCKED
                            && t.tankMode() == TankModeSetting.PALUDARIUM, "locked: refused");
                    t.setLocked(false);
                }
                // too far away: the menu is no longer valid
                c.modeSetTime = Long.MIN_VALUE;
                sp.teleportTo(h.getLevel(), near.getX() + 40.5, near.getY(), near.getZ() + 0.5, java.util.Set.of(), 0F, 0F, false);
                check(TankModePayload.apply(sp, new TankModePayload(id, TankModeSetting.AUTO.ordinal())) == TankModePayload.Result.NO_MENU
                        && t.tankMode() == TankModeSetting.PALUDARIUM, "out of reach: refused");
            } finally {
                Features.set(Feature.TANK_MODE, before);
                sp.level().getServer().getPlayerList().remove(sp);
            }
            h.succeed();
        });
    }
}
