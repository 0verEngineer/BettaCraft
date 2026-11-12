package org.overengineer.bettacraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorSync;
import org.overengineer.bettacraft.tank.interior.InteriorWorldOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development-only visual smoke test ({@code -Dbettacraft.smoketest.world=true}, {@code gradlew runClient -Psmoketestworld}):
 * creates a flat creative world, builds a small decorated tank and a 16x16x16 tank (scale 16), lets the client mesh
 * the miniatures, saves screenshots to run/screenshots and quits. Never active in normal play.
 */
final class DevClientSmokeTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static int ticks;
    private static boolean created;

    private DevClientSmokeTest() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Opening e) -> {
            if (!created && e.getNewScreen() instanceof TitleScreen) {
                created = true;
                Minecraft mc = Minecraft.getInstance();
                LevelSettings settings = new LevelSettings("bettacraft_smoketest_" + System.currentTimeMillis(), GameType.CREATIVE, false,
                        Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, new WorldOptions(42L, false, false),
                        ra -> ra.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                        e.getNewScreen());
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getSingleplayerServer() == null) return;
        ticks++;
        if (ticks == 60) {
            mc.getSingleplayerServer().execute(DevClientSmokeTest::buildScene);
        } else if (ticks == 400) {
            shot(mc, "bettacraft_small_tank");
        } else if (ticks == 420) {
            mc.getSingleplayerServer().execute(() -> {
                ServerPlayer p = mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                p.teleportTo(p.serverLevel(), 34.5, p.getY() + 14, -14, 40F, 25F);
            });
        } else if (ticks == 560) {
            shot(mc, "bettacraft_big_tank");
        } else if (ticks == 600) {
            LOGGER.info("[smoketest] client world test finished");
            mc.stop();
        }
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }

    private static void buildScene() {
        Minecraft mc = Minecraft.getInstance();
        ServerPlayer p = mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
        ServerLevel level = p.serverLevel();
        BlockPos feet = p.blockPosition();
        p.teleportTo(level, feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0F, 15F);
        // small tank 4x3x3 in front (south) of the player, ~60% water, decorated interior
        BlockPos a = feet.offset(-2, 0, 4);
        place(level, a, 4, 3, 3);
        TankBlockEntity c = TankMultiblock.getController(level, a);
        TankInteractions.changeWater(level, c, 22000);
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        decorate(in, r, 3);
        InteriorSync.markDirty(r.id());
        // ordinary water next to it for comparison (must stay vanilla)
        level.setBlockAndUpdate(feet.offset(3, -1, 4), Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(feet.offset(3, -1, 5), Blocks.WATER.defaultBlockState());

        // big tank 16x16x16 (scale 16) to the east
        long t0 = System.nanoTime();
        BlockPos b = feet.offset(8, 0, 4);
        place(level, b, 16, 16, 16);
        TankBlockEntity big = TankMultiblock.getController(level, b);
        LOGGER.info("[smoketest] formed 16x16x16 tank block by block in {} ms: {}", (System.nanoTime() - t0) / 1_000_000, big == null ? null : big.getTank());
        TankInteractions.changeWater(level, big, 3000000);
        InteriorRegion rb = InteriorManager.ensureRegion(level, big);
        decorate(in, rb, 16);
        InteriorSync.markDirty(rb.id());
        LOGGER.info("[smoketest] scene built");
    }

    private static void place(ServerLevel level, BlockPos min, int sx, int sy, int sz) {
        for (int y = 0; y < sy; y++)
            for (int z = 0; z < sz; z++)
                for (int x = 0; x < sx; x++) {
                    BlockPos pos = min.offset(x, y, z);
                    level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
                    TankMultiblock.onTankBlockAdded(level, pos, null);
                }
    }

    /** Substrate (1/16 of the height), rock boxes and plant columns, written directly into the sections. */
    private static void decorate(ServerLevel in, InteriorRegion r, int tankHeight) {
        LongOpenHashSet touched = new LongOpenHashSet();
        int s = r.scale();
        int sub = Math.max(2, r.sizeY() / 16);
        BlockPos o = r.toWorld(0, 0, 0);
        InteriorWorldOps.fill(in, o.getX(), o.getY(), o.getZ(), o.getX() + r.sizeX() - 1, o.getY() + sub - 1, o.getZ() + r.sizeZ() - 1,
                Blocks.SAND.defaultBlockState(), touched);
        java.util.Random rnd = new java.util.Random(7);
        BlockState[] rocks = {Blocks.STONE.defaultBlockState(), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Blocks.TUFF.defaultBlockState()};
        int rockCount = Math.max(3, r.sizeX() * r.sizeZ() / 1600);
        for (int i = 0; i < rockCount; i++) {
            int w = s / 2 + rnd.nextInt(s), h = s / 2 + rnd.nextInt(s);
            int x = rnd.nextInt(r.sizeX() - w), z = rnd.nextInt(r.sizeZ() - w);
            InteriorWorldOps.fill(in, o.getX() + x, o.getY() + sub, o.getZ() + z, o.getX() + x + w - 1, o.getY() + sub + h - 1,
                    o.getZ() + z + w - 1, rocks[i % rocks.length], touched);
        }
        BlockState[] plants = {Blocks.SEAGRASS.defaultBlockState(), Blocks.KELP_PLANT.defaultBlockState(), Blocks.TUBE_CORAL_BLOCK.defaultBlockState(),
                Blocks.FIRE_CORAL_BLOCK.defaultBlockState(), Blocks.BRAIN_CORAL_BLOCK.defaultBlockState()};
        int plantCount = Math.max(20, r.sizeX() * r.sizeZ() / 220);
        for (int i = 0; i < plantCount; i++) {
            int x = rnd.nextInt(r.sizeX()), z = rnd.nextInt(r.sizeZ());
            int h = s * (1 + rnd.nextInt(Math.max(1, tankHeight / 2)));
            InteriorWorldOps.fill(in, o.getX() + x, o.getY() + sub, o.getZ() + z, o.getX() + x, Math.min(o.getY() + sub + h, o.getY() + r.sizeY() - 1),
                    o.getZ() + z, plants[i % plants.length], touched);
        }
        InteriorWorldOps.resendChunks(in, touched);
    }
}
