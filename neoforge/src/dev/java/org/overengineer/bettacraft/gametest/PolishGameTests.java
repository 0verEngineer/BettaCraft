package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.ClipFeederBlock;
import org.overengineer.bettacraft.equipment.ClipFeederBlockEntity;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModMenus;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankProtection;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M21.5 (polish): plecos and otocinclus stand upright only once they reach the glass; the break protection (nobody
 * breaks a protected tank or its equipment, only the owner switches it, explosions and pistons per server config); the
 * clip-on feeder (placed only on the side of a top tank block, feeds a one-block tank with a light on it).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PolishGameTests {
    static final String EMPTY = "empty_32x10x32";

    private PolishGameTests() {}

    static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz) {
        TankBlockEntity c = CritterGameTests.tank(h, x, y, z, sx, sy, sz, (long) sx * sy * sz * 1000);
        c.getTank().setWaterQuality(1F);
        return c;
    }

    static TankmateEntity spawn(GameTestHelper h, TankBlockEntity c, TankmateSpecies s, Vec3 p) {
        TankmateEntity e = TankmateBreeding.spawn(h.getLevel(), c, s, TankmateGenes.wild(s), false, LifeStage.ADULT, 0, p);
        if (e == null) throw new GameTestAssertException("could not spawn " + s.id());
        e.health().feed();
        return e;
    }

    private static ServerPlayer player(GameTestHelper h, GameType mode) {
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        sp.gameMode.changeGameModeForPlayer(mode);
        sp.getInventory().clearContent();
        sp.getInventory().selected = 8;
        BlockPos at = h.absolutePos(new BlockPos(1, 2, 1));
        sp.teleportTo(h.getLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F, 0F);
        return sp;
    }

    // ------------------------------------------------------------------ glass cleaners

    /**
     * An otocinclus and an L134 in the middle of a big tank swim to the glass lying flat and stand up only there. (M52: an
     * otocinclus crosses at about 0.003 blocks a tick and slows down on its last 0.4 blocks: a spot up to 1.9 blocks away takes
     * up to about 1000 ticks; 900 was too short for the far spots.)
     */
    @GameTest(template = EMPTY, timeoutTicks = 1600)
    public static void glassCleanersStandUpOnlyAtTheGlass(GameTestHelper h) {
        // each in its own tank (the territorial L134 chases other fish away)
        TankmateEntity[] made = new TankmateEntity[2];
        TankmateSpecies[] kinds = {TankmateSpecies.OTOCINCLUS, TankmateSpecies.LEOPARD_FROG_PLECO};
        for (int i = 0; i < 2; i++) {
            TankBlockEntity c = tank(h, 2 + i * 8, 1, 2, 3, 2, 3);
            TankWaterVolume w = new TankWaterVolume(c.getTank());
            Vec3 mid = new Vec3((w.box().minX + w.box().maxX) / 2, w.box().minY + 0.8, (w.box().minZ + w.box().maxZ) / 2);
            made[i] = spawn(h, c, kinds[i], mid);
        }
        TankmateEntity oto = made[0], pleco = made[1];
        boolean[] started = new boolean[2], arrived = new boolean[2];
        TankmateEntity[] fish = {oto, pleco};
        h.onEachTick(() -> {
            for (int i = 0; i < 2; i++) {
                TankmateEntity f = fish[i];
                if (!started[i]) {
                    started[i] = f.swimToGlassForTest();
                    continue;
                }
                Vec3 t = f.behaviourTarget();
                if (t == null) continue;
                double d = f.position().distanceTo(t);
                if (f.tickCount % 40 == 0) org.slf4j.LoggerFactory.getLogger("bettacraft/gametest").info("[glass] {} at {} target {} d {} v {} speed {}",
                        f.species().id(), f.position(), t, String.format("%.3f", d), f.getDeltaMovement(), f.getSpeed());
                boolean upright = f.flag(TankmateEntity.FLAG_GLASS);
                if (upright && d > 0.2) throw new GameTestAssertException(f.species().id() + " stands upright " + String.format("%.2f", d) + " blocks before the glass");
                if (upright) arrived[i] = true;
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(started[0] && started[1], "both found a spot on the glass");
            for (int i = 0; i < 2; i++) {
                Vec3 t = fish[i].behaviourTarget();
                h.assertTrue(arrived[i], fish[i].species().id() + " reached the glass and stands upright (now "
                        + (t == null ? "no target" : String.format("%.2f", fish[i].position().distanceTo(t))) + " blocks away)");
            }
        });
    }

    /**
     * M52: an otocinclus pressed to the surface with its spot on the glass 0.2 blocks below it goes down and stands up there. The
     * lift that undoes the vanilla sinking was also added when a slow fish had no sinking left (vanilla zeroes a vertical motion
     * below 0.003): it rose by up to 0.005 a tick, stayed at the surface 0.19 blocks above its spot and never stood up.
     */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void glassCleanerReachesASpotJustBelowIt(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 3, 2, 3);
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        double mz = (w.box().minZ + w.box().maxZ) / 2;
        TankmateEntity oto = spawn(h, c, TankmateSpecies.OTOCINCLUS, new Vec3(w.box().minX + 0.5, w.box().minY + 0.8, mz));
        double top = w.surface() - org.overengineer.bettacraft.entity.tankmate.TankmateExtents.surfaceMargin(oto);
        Vec3 spot = new Vec3(w.box().minX + oto.sideMargin() + 0.005, top - 0.2, mz);
        boolean[] started = {false};
        h.onEachTick(() -> {
            if (started[0]) return;
            oto.setPos(spot.x + 0.35, top, mz);
            started[0] = oto.swimToGlassForTest(spot, 2);
        });
        h.succeedWhen(() -> {
            h.assertTrue(started[0], "it knows its tank");
            String at = String.format("%.3f from its spot, %.3f above it", oto.position().distanceTo(spot), oto.getY() - spot.y);
            h.assertTrue(oto.isAlive() && oto.behaviourTarget() != null && oto.behaviourTarget().distanceToSqr(spot) < 1e-9, "still on the way to its spot");
            h.assertTrue(oto.flag(TankmateEntity.FLAG_GLASS), "the otocinclus stands on the glass (" + at + ")");
            h.assertTrue(oto.position().distanceTo(spot) < 0.2, "upright at its spot (" + at + ")");
        });
    }

    // ------------------------------------------------------------------ break protection

    /** A tank on a cabinet with lid, light, feeder and a clip-on feeder: tank at (3..4, 2, 3..4), cabinet below (3, 1, 3). */
    private static TankBlockEntity protectedSetup(GameTestHelper h) {
        h.setBlock(new BlockPos(3, 1, 3), ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
        TankBlockEntity c = tank(h, 3, 2, 3, 2, 1, 2);
        h.setBlock(new BlockPos(3, 3, 3), ModBlocks.TANK_LIGHT.get().defaultBlockState());
        h.setBlock(new BlockPos(4, 3, 3), ModBlocks.FEEDER.get().defaultBlockState());
        h.setBlock(new BlockPos(3, 3, 4), ModBlocks.TANK_LID.get().defaultBlockState());
        h.setBlock(new BlockPos(3, 2, 2), ModBlocks.CLIP_FEEDER.get().defaultBlockState().setValue(ClipFeederBlock.FACING, Direction.NORTH));
        return c;
    }

    private static final BlockPos[] PARTS = {new BlockPos(3, 2, 3), new BlockPos(4, 2, 4), new BlockPos(3, 1, 3), new BlockPos(3, 3, 3),
            new BlockPos(4, 3, 3), new BlockPos(3, 3, 4), new BlockPos(3, 2, 2)};

    /** Owner in survival and in creative: nothing of a protected tank can be broken; after switching it off it can. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void breakProtectionStopsEvenTheOwner(GameTestHelper h) {
        TankBlockEntity c = protectedSetup(h);
        ServerPlayer owner = player(h, GameType.SURVIVAL);
        c.getTank().setOwner(owner.getUUID());
        h.assertTrue(!c.getTank().breakProtected(), "off for a new tank");
        h.assertTrue(TankProtection.toggle(owner, c) == null && c.getTank().breakProtected(), "the owner switches it on");
        for (GameType mode : new GameType[]{GameType.SURVIVAL, GameType.CREATIVE}) {
            owner.gameMode.changeGameModeForPlayer(mode);
            for (BlockPos p : PARTS) {
                var before = h.getBlockState(p).getBlock();
                boolean broke = owner.gameMode.destroyBlock(h.absolutePos(p));
                h.assertTrue(!broke && h.getBlockState(p).is(before), mode + ": " + before + " at " + p.toShortString() + " was broken");
            }
        }
        // saved and loaded
        var tag = c.getTank().save();
        h.assertTrue(tag.getBoolean("break_protected"), "saved");
        h.assertTrue(TankProtection.toggle(owner, c) == null && !c.getTank().breakProtected(), "switched off");
        h.assertTrue(owner.gameMode.destroyBlock(h.absolutePos(new BlockPos(3, 3, 4))), "the lid can be broken again");
        h.succeed();
    }

    /** Only the owner or an OP switches it. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void onlyTheOwnerSwitchesBreakProtection(GameTestHelper h) {
        TankBlockEntity c = tank(h, 3, 1, 3, 1, 1, 1);
        c.getTank().setOwner(UUID.randomUUID());
        ServerPlayer other = player(h, GameType.CREATIVE);
        h.assertTrue(!other.hasPermissions(2), "the mock player is no OP");
        h.assertTrue(TankProtection.toggle(other, c) != null && !c.getTank().breakProtected(), "another player cannot switch it on");
        c.getTank().setOwner(other.getUUID());
        h.assertTrue(TankProtection.toggle(other, c) == null && c.getTank().breakProtected(), "the owner can");
        h.succeed();
    }

    /** Explosions and pistons leave a protected tank alone; with the server config off they do not. */
    @GameTest(template = EMPTY, batch = "bettacraft_protection_config", timeoutTicks = 80)
    public static void breakProtectionStopsExplosionsAndPistons(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = protectedSetup(h);
        c.getTank().setBreakProtected(true);
        Vec3 boom = Vec3.atCenterOf(h.absolutePos(new BlockPos(3, 2, 1)));
        level.explode(null, boom.x, boom.y, boom.z, 3F, Level.ExplosionInteraction.TNT);
        for (BlockPos p : PARTS) h.assertTrue(!h.getBlockState(p).isAir(), "the explosion destroyed " + p.toShortString());
        // a piston next to the lid pushes towards it (the lid has room behind it)
        BlockPos piston = new BlockPos(2, 3, 4), lid = new BlockPos(3, 3, 4);
        h.setBlock(piston, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
        h.setBlock(piston.west(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(6, () -> {
            h.assertTrue(h.getBlockState(lid).is(ModBlocks.TANK_LID.get()), "the piston moved the lid of a protected tank");
            boolean was = ServerConfig.PROTECT_FROM_EXPLOSIONS_AND_PISTONS.get();
            ServerConfig.PROTECT_FROM_EXPLOSIONS_AND_PISTONS.set(false);
            try {
                h.setBlock(piston.west(), Blocks.AIR.defaultBlockState());
                h.setBlock(piston, Blocks.AIR.defaultBlockState());
            } catch (RuntimeException e) {
                ServerConfig.PROTECT_FROM_EXPLOSIONS_AND_PISTONS.set(was);
                throw e;
            }
            h.runAfterDelay(2, () -> {
                try {
                    h.setBlock(piston, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
                    h.setBlock(piston.west(), Blocks.REDSTONE_BLOCK.defaultBlockState());
                } catch (RuntimeException e) {
                    ServerConfig.PROTECT_FROM_EXPLOSIONS_AND_PISTONS.set(was);
                    throw e;
                }
                h.runAfterDelay(6, () -> {
                    try {
                        h.assertTrue(!h.getBlockState(lid).is(ModBlocks.TANK_LID.get()), "config off: the piston moves the lid");
                    } finally {
                        ServerConfig.PROTECT_FROM_EXPLOSIONS_AND_PISTONS.set(was);
                    }
                    h.succeed();
                });
            });
        });
    }

    // ------------------------------------------------------------------ clip-on feeder

    /** Placed like a player on a side face of a top tank block (not on a lower one), feeds a one-block tank with a light on top. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void clipFeederFeedsAOneBlockTankWithALight(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity tall = tank(h, 8, 1, 3, 1, 2, 1);
        TankBlockEntity small = tank(h, 3, 1, 3, 1, 1, 1);
        h.setBlock(new BlockPos(3, 2, 3), ModBlocks.TANK_LIGHT.get().defaultBlockState());
        ServerPlayer sp = player(h, GameType.SURVIVAL);
        // lower block of the tall tank: refused
        use(sp, h.absolutePos(new BlockPos(8, 1, 3)), Direction.NORTH, new ItemStack(ModItems.CLIP_FEEDER.get()));
        h.assertTrue(h.getBlockState(new BlockPos(8, 1, 2)).isAir(), "not on a lower tank block");
        use(sp, h.absolutePos(new BlockPos(8, 2, 3)), Direction.NORTH, new ItemStack(ModItems.CLIP_FEEDER.get()));
        h.assertTrue(h.getBlockState(new BlockPos(8, 2, 2)).is(ModBlocks.CLIP_FEEDER.get()), "on the top block of the tall tank");
        // the one-block tank with a light on top
        use(sp, h.absolutePos(new BlockPos(3, 1, 3)), Direction.WEST, new ItemStack(ModItems.CLIP_FEEDER.get()));
        BlockPos fp = new BlockPos(2, 1, 3);
        h.assertTrue(h.getBlockState(fp).is(ModBlocks.CLIP_FEEDER.get()) && h.getBlockState(fp).getValue(ClipFeederBlock.FACING) == Direction.WEST,
                "clip-on feeder on the west side");
        ClipFeederBlockEntity f = (ClipFeederBlockEntity) level.getBlockEntity(h.absolutePos(fp));
        h.assertTrue(TankCare.findFeeder(level, small.getTank()) == f, "the tank finds its clip-on feeder");
        // food in by hand: two stacks at most (M66: two slots, a menu like the feeder's)
        use(sp, h.absolutePos(fp), Direction.WEST, new ItemStack(ModItems.BETTA_PELLETS.get(), 64));
        use(sp, h.absolutePos(fp), Direction.WEST, new ItemStack(ModItems.BETTA_PELLETS.get(), 10));
        h.assertTrue(f.foodCount() == 74, "two slots: 64 and 10: " + f.foodCount());
        use(sp, h.absolutePos(fp), Direction.WEST, new ItemStack(ModItems.BETTA_PELLETS.get(), 64));
        h.assertTrue(f.foodCount() == 128, "two stacks at most: " + f.foodCount());
        var menu = f.createMenu(7, sp.getInventory(), sp);
        h.assertTrue(menu instanceof org.overengineer.bettacraft.equipment.FeederMenu fm && fm.slotCount() == 2 && fm.getType() == ModMenus.CLIP_FEEDER.get(),
                "its menu has two slots");
        var data = org.overengineer.bettacraft.entity.betta.BettaData.create(org.overengineer.bettacraft.genetics.BettaGenotype.randomDomestic(
                net.minecraft.util.RandomSource.create(5)), false, level.getGameTime());
        var err = TankInhabitants.insert(level, small, org.overengineer.bettacraft.entity.betta.BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), data),
                ModEntities.BETTA.get(), null, false);
        h.assertTrue(err == null, "betta inserted: " + (err == null ? "" : err.getString()));
        int used = f.feedNow(level, small);
        h.assertTrue(used == 1 && f.foodCount() == 127, "fed one pellet, used " + used);
        // the menu's button: 1 -> 2 feedings a day
        h.assertTrue(menu.clickMenuButton(sp, org.overengineer.bettacraft.equipment.FeederMenu.BUTTON_CYCLE) && f.feedingsPerDay() == 2, "two feedings a day");
        // breaking the tank block drops the feeder
        level.destroyBlock(h.absolutePos(new BlockPos(8, 2, 3)), false);
        h.runAfterDelay(2, () -> {
            h.assertTrue(h.getBlockState(new BlockPos(8, 2, 2)).isAir(), "without its glass the clip-on feeder falls off");
            for (var e : TankInhabitants.liveInhabitants(level, small.getTank())) e.discard();
            small.getTank().inhabitants().clear();
            h.succeed();
        });
        h.assertTrue(tall != null, "tall tank");
    }

    // ------------------------------------------------------------------ detailed decoration

    /** The new skull and wreck are about twice as big (more blocks); with features.decor.detailed off the old small ones. */
    @GameTest(template = EMPTY, batch = "bettacraft_decor_config", timeoutTicks = 40)
    public static void detailedSkullAndWreckAndTheSmallOnes(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        boolean was = Features.own(Feature.DETAILED_DECOR);
        try {
            int[] big = new int[2], small = new int[2];
            var types = new org.overengineer.bettacraft.decor.DecorType[]{org.overengineer.bettacraft.decor.DecorType.DINO_SKULL,
                    org.overengineer.bettacraft.decor.DecorType.SHIPWRECK};
            for (int pass = 0; pass < 2; pass++) {
                Features.set(Feature.DETAILED_DECOR, pass == 0);
                for (int i = 0; i < 2; i++) {
                    var t = org.overengineer.bettacraft.decor.DecorItem.template(level, types[i].structure());
                    h.assertTrue(t.isPresent(), types[i] + " template " + types[i].structure());
                    (pass == 0 ? big : small)[i] = t.get().blocks().size();
                    if (pass == 0) {
                        h.assertTrue(t.get().sizeX() == types[i].sizeX && t.get().sizeY() == types[i].sizeY && t.get().sizeZ() == types[i].sizeZ,
                                types[i] + " size " + t.get().sizeX() + "x" + t.get().sizeY() + "x" + t.get().sizeZ());
                        // placed in the world (an interior works the same way), at least 1.5x the blocks of the old one
                        BlockPos base = h.absolutePos(new BlockPos(8 + i * 14, 1, 8));
                        h.assertTrue(org.overengineer.bettacraft.decor.DecorItem.place(level, types[i], base, net.minecraft.world.level.block.Rotation.NONE, null) == null,
                                types[i] + " placed");
                    }
                }
            }
            for (int i = 0; i < 2; i++) h.assertTrue(big[i] >= small[i] * 2, types[i] + ": " + big[i] + " blocks vs. " + small[i] + " before");
        } finally {
            Features.set(Feature.DETAILED_DECOR, was);
        }
        h.succeed();
    }

    private static void use(ServerPlayer sp, BlockPos pos, Direction face, ItemStack stack) {
        sp.setItemInHand(InteractionHand.MAIN_HAND, stack);
        Vec3 hit = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        sp.gameMode.useItemOn(sp, sp.serverLevel(), stack, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, pos, false));
    }
}
