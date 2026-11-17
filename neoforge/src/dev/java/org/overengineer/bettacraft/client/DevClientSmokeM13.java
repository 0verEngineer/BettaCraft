package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.client.interior.ClientInteriorCache;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorWorldOps;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tank.plants.TankPlants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M13 part of the dev client smoke test: aquarium plants in the interior. A 4x2x2 tank under a lit tank light with a
 * gravel and sand floor, two rock piles and a log, planted with all 17 species at full size (red ones red), and a 3x1x2
 * tank under a stone roof (weak light) with the red species, which stay green. A third 2x1x1 tank with a Rotala cutting
 * grows stage by stage while the camera looks at it (the miniature follows). Screenshots {@code bettacraft_m13_*}:
 * overview, close-up, red species in strong and weak light, growth stages, inside the interior and the aquarium plants
 * page of the menu in English and German.
 */
final class DevClientSmokeM13 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private BlockPos base;
    private int wait;
    private volatile InteriorRegion mainRegion;

    DevClientSmokeM13() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(40, () -> server(p -> {
            for (BlockPos at : List.of(base, base.offset(0, 0, 8), base.offset(8, 0, 8))) {
                TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), at);
                if (c != null) InteriorPlants.settleTint(p.serverLevel(), c);
            }
        }));
        // the planted tank from the front, slightly from above
        then(120, () -> look(base.getX() + 2.0, base.getY() + 2.3, base.getZ() - 3.1, 0F, 18F));
        then(120, () -> {
            logMiniature("main");
            shot("overview");
        });
        // close-up of the left rock with the epiphytes and the moss on the log, at eye level
        then(1, () -> look(base.getX() + 0.9, base.getY() + 0.75, base.getZ() - 1.0, 18F, 12F));
        then(60, () -> shot("closeup"));
        // the right half: stem plants up to the surface and the Bucephalandras
        then(1, () -> look(base.getX() + 3.0, base.getY() + 1.4, base.getZ() - 1.3, -12F, 10F));
        then(60, () -> shot("stems"));
        // red species: strong light (main tank) and weak light (under the roof)
        then(1, () -> look(base.getX() + 1.5, base.getY() + 1.0, base.getZ() + 8 - 2.2, 0F, 10F));
        then(80, () -> shot("weak_light"));
        // growth: a Rotala cutting in the small tank grows stage by stage
        then(1, () -> look(base.getX() + 9.0, base.getY() + 0.7, base.getZ() + 8 - 1.3, 0F, 10F));
        then(60, () -> shot("growth_0"));
        for (int i = 1; i <= 4; i++) {
            int n = i;
            then(1, () -> server(p -> growSmall(p.serverLevel(), n * 2)));
            then(60, () -> shot("growth_" + n));
        }
        // inside the main tank's interior
        then(1, () -> server(p -> {
            InteriorRegion r = mainRegion;
            if (r == null) return;
            ServerLevel in = InteriorManager.interiorLevel(p.server);
            BlockPos at = r.toWorld(32, 9, 1);
            p.teleportTo(in, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F, 12F);
        }));
        then(120, () -> shot("interior"));
        then(1, () -> server(p -> {
            InteriorRegion r = mainRegion;
            if (r == null) return;
            BlockPos at = r.toWorld(4, 4, 6);
            p.teleportTo(p.serverLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, -30F, 8F);
        }));
        then(80, () -> shot("interior_rock"));
        then(1, () -> server(p -> {
            InteriorRegion r = mainRegion;
            if (r == null) return;
            BlockPos at = r.toWorld(46, 22, 4);
            p.teleportTo(p.serverLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 20F, 35F);
        }));
        then(80, () -> shot("interior_top"));
        then(1, () -> server(p -> {
            if (InteriorManager.isInterior(p.level())) {
                ServerLevel ow = p.server.overworld();
                p.teleportTo(ow, base.getX() + 2.0, base.getY() + 1.5, base.getZ() - 2.5, 0F, 0F);
            }
        }));
        // the aquarium plants page of the menu, English and German
        then(60, () -> mc.options.hideGui = false);
        then(1, () -> server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), base);
            org.overengineer.bettacraft.platform.Platform.get().openMenu(p, new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        }));
        then(30, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.showAquaticPageForTest();
        });
        then(30, () -> shot("menu_aquatic"));
        String lang = mc.options.languageCode;
        then(1, () -> language("de_de"));
        then(20, () -> shot("menu_aquatic_de"));
        then(1, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.showPlantsPageForTest();
        });
        then(20, () -> shot("menu_plants_de"));
        then(1, () -> {
            language(lang);
            mc.options.guiScale().set(0);
            mc.resizeDisplay();
        });
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.setScreen(null));
        then(1, () -> server(p -> {
            for (BlockPos at : List.of(base, base.offset(0, 0, 8), base.offset(8, 0, 8))) {
                TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), at);
                if (c == null) continue;
                InteriorRegion r = InteriorPlants.region(p.server, c.getTank());
                LOGGER.info("[smoketest] M13 tank {}: {} plants, {} plant blocks, light {}", at, r == null ? 0 : r.plants().size(),
                        r == null ? 0 : r.plants().blocks(), TankPlants.light(p.serverLevel(), c));
            }
        }));
    }

    private void logMiniature(String what) {
        InteriorRegion r = mainRegion;
        if (r == null) return;
        var cr = ClientInteriorCache.get(r.id());
        int plants = 0;
        if (cr != null) {
            for (int x = 0; x < r.sizeX(); x++)
                for (int y = 0; y < r.sizeY(); y++)
                    for (int z = 0; z < r.sizeZ(); z++) if (cr.get(x, y, z).getBlock() instanceof org.overengineer.bettacraft.plants.AquaticPlantBlock) plants++;
        }
        LOGGER.info("[smoketest] M13 miniature {}: {} plant blocks on the client (server {})", what, plants, r.plants().blocks());
        if (plants == 0) LOGGER.error("[smoketest] M13 the miniature shows no plants");
    }

    private static void place(ServerLevel level, List<BlockPos> blocks) {
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
    }

    private static List<BlockPos> cuboid(BlockPos min, int sx, int sy, int sz) {
        List<BlockPos> l = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) l.add(min.offset(x, y, z));
        return l;
    }

    private static TankBlockEntity setup(ServerLevel level, ServerPlayer p, BlockPos min, long water) {
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, water);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(p.getUUID());
        c.getTank().setWaterTemp(26F);
        return c;
    }

    private static void lights(ServerLevel level, BlockPos min, int sx, int y, int z) {
        for (int x = 0; x < sx; x++) {
            level.setBlockAndUpdate(min.offset(x, y, z), ModBlocks.TANK_LIGHT.get().defaultBlockState()
                    .setValue(TankLightBlock.AXIS, Direction.Axis.X).setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < sx - 1));
        }
    }

    /** Floor substrate: gravel at the back, sand in front. */
    private static void floor(ServerLevel in, InteriorRegion r, int sandFrontRows) {
        for (int x = 0; x < r.sizeX(); x++)
            for (int z = 0; z < r.sizeZ(); z++) {
                BlockState s = z < sandFrontRows ? Blocks.SAND.defaultBlockState() : (x * 7 + z * 3) % 11 == 0 ? Blocks.ANDESITE.defaultBlockState()
                        : Blocks.GRAVEL.defaultBlockState();
                Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), s, false);
            }
    }

    /** A rounded pile of stone (local x/z centre, radius, height). */
    private static void rock(ServerLevel in, InteriorRegion r, int cx, int cz, int rad, int h) {
        it.unimi.dsi.fastutil.longs.LongOpenHashSet touched = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        for (int x = -rad; x <= rad; x++)
            for (int z = -rad; z <= rad; z++) {
                double d = Math.sqrt(x * x + z * z) / rad;
                if (d > 1.0) continue;
                int top = (int) Math.round(h * (1 - d * d * 0.7));
                for (int y = 0; y < top; y++) {
                    BlockState s = (x + y + z) % 3 == 0 ? Blocks.ANDESITE.defaultBlockState() : Blocks.STONE.defaultBlockState();
                    in.setBlock(r.toWorld(cx + x, y, cz + z), s, 2);
                }
            }
        InteriorWorldOps.resendChunks(in, touched);
    }

    private static int topAt(ServerLevel in, InteriorRegion r, int x, int z) {
        int y = 0;
        while (y < r.sizeY() && !in.getBlockState(r.toWorld(x, y, z)).isAir()) y++;
        return y;
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ServerLevel in = InteriorManager.interiorLevel(p.server);
        base = p.blockPosition().offset(0, 2, 230);
        // A: the planted tank 4x2x2 under a lit light bar
        place(level, cuboid(base, 4, 2, 2));
        TankBlockEntity main = setup(level, p, base, 14000);
        lights(level, base, 4, 2, 1);
        InteriorRegion r = InteriorManager.ensureRegion(level, main);
        mainRegion = r;
        floor(in, r, 9);
        rock(in, r, 9, 14, 5, 6);
        rock(in, r, 55, 10, 5, 5);
        for (int x = 14; x <= 22; x++) {
            in.setBlock(r.toWorld(x, 0, 6), Blocks.OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X), 2);
            in.setBlock(r.toWorld(x, 1, 6), Blocks.OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X), 2);
        }
        long seed = 1300;
        // back row
        for (var e : List.of(Map.entry(PlantSpecies.VALLISNERIA, 3), Map.entry(PlantSpecies.VALLISNERIA, 10), Map.entry(PlantSpecies.AMAZON_SWORD, 20),
                Map.entry(PlantSpecies.ROTALA, 33), Map.entry(PlantSpecies.LUDWIGIA, 40), Map.entry(PlantSpecies.ALTERNANTHERA, 47),
                Map.entry(PlantSpecies.ROTALA, 61))) {
            InteriorPlants.plantAt(in, r, e.getKey(), e.getValue(), 0, 26, e.getKey().stages(), seed++);
        }
        // middle
        InteriorPlants.plantAt(in, r, PlantSpecies.CRYPT_GREEN, 25, 0, 16, 6, seed++);
        InteriorPlants.plantAt(in, r, PlantSpecies.CRYPT_BROWN, 31, 0, 13, 6, seed++);
        InteriorPlants.plantAt(in, r, PlantSpecies.CRYPT_FLAMINGO, 37, 0, 16, 6, seed++);
        InteriorPlants.plantAt(in, r, PlantSpecies.TIGER_LOTUS, 44, 0, 18, 8, seed++);
        // epiphytes on the rocks, moss on the log
        InteriorPlants.plantAt(in, r, PlantSpecies.JAVA_FERN, 9, topAt(in, r, 9, 14), 14, 8, seed++);
        InteriorPlants.plantAt(in, r, PlantSpecies.ANUBIAS_NANA, 12, topAt(in, r, 12, 11), 11, 6, seed++);
        InteriorPlants.plantAt(in, r, PlantSpecies.BUCE_GREEN_WAVY, 53, topAt(in, r, 53, 9), 9, 6, seed++);
        InteriorPlants.plantAt(in, r, PlantSpecies.BUCE_BROWNIE_RED, 57, topAt(in, r, 57, 11), 11, 6, seed++);
        InteriorPlants.plantAt(in, r, PlantSpecies.BUCE_BLUE, 55, topAt(in, r, 55, 13), 13, 6, seed++);
        InteriorPlants.plantAt(in, r, PlantSpecies.BUCE_MINI_NEEDLE, 58, topAt(in, r, 58, 7), 7, 6, seed++);
        InteriorPlants.plantAt(in, r, PlantSpecies.JAVA_MOSS, 18, 2, 6, 6, seed++);
        // a carpet in front
        for (int x : new int[]{27, 33, 39}) InteriorPlants.plantAt(in, r, PlantSpecies.MONTE_CARLO, x, 0, 4, 6, seed++);
        fish(level, p, main, new double[][]{{1.4, 0.9, 0.5}, {2.7, 1.3, 0.6}});

        // B: red species under a stone roof (weak light)
        BlockPos dark = base.offset(0, 0, 8);
        place(level, cuboid(dark, 3, 1, 2));
        TankBlockEntity weak = setup(level, p, dark, 6000);
        for (int x = -5; x <= 7; x++) for (int z = -5; z <= 6; z++) level.setBlockAndUpdate(dark.offset(x, 3, z), Blocks.SMOOTH_STONE.defaultBlockState());
        InteriorRegion rb = InteriorManager.ensureRegion(level, weak);
        floor(in, rb, 0);
        int x = 4;
        for (PlantSpecies s : List.of(PlantSpecies.ROTALA, PlantSpecies.LUDWIGIA, PlantSpecies.ALTERNANTHERA, PlantSpecies.CRYPT_BROWN,
                PlantSpecies.TIGER_LOTUS, PlantSpecies.CRYPT_FLAMINGO)) {
            InteriorPlants.plantAt(in, rb, s, x, 0, 12, s.stages(), seed++);
            x += 8;
        }

        // C: a small tank with a Rotala cutting that grows in steps during the scene
        BlockPos small = base.offset(8, 0, 8);
        place(level, cuboid(small, 2, 1, 1));
        TankBlockEntity sc = setup(level, p, small, 2000);
        lights(level, small, 2, 1, 0);
        InteriorRegion rc = InteriorManager.ensureRegion(level, sc);
        floor(in, rc, 0);
        InteriorPlants.plantAt(in, rc, PlantSpecies.ROTALA, 16, 0, 8, 0, seed++);
        InteriorPlants.plantAt(in, rc, PlantSpecies.AMAZON_SWORD, 7, 0, 8, 0, seed++);
        LOGGER.info("[smoketest] M13 scene at {}: main {} plants / {} blocks, weak light {} plants, small {} plants", base, r.plants().size(),
                r.plants().blocks(), rb.plants().size(), rc.plants().size());
    }

    /** Grows the plants of the small tank to {@code stage} (as the care loop would over the days). */
    private void growSmall(ServerLevel level, int stage) {
        TankBlockEntity c = TankMultiblock.getController(level, base.offset(8, 0, 8));
        if (c == null) return;
        InteriorRegion r = InteriorPlants.region(level.getServer(), c.getTank());
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        if (r == null || in == null) return;
        for (InteriorPlant p : r.plants().all()) {
            p.target = Math.min(p.species.stages(), stage);
            InteriorPlants.apply(in, r, p);
        }
        LOGGER.info("[smoketest] M13 small tank at stage {}: {} plant blocks", stage, r.plants().blocks());
    }

    private static void fish(ServerLevel level, ServerPlayer p, TankBlockEntity c, double[][] spots) {
        List<Map<String, String>> genes = List.of(
                Map.of("finform", "halfmoon", "fin_length", "long", "body_color", "royal_blue", "fin_color", "red", "pattern", "butterfly",
                        "pattern_color", "white", "iridescence", "high"),
                Map.of("finform", "veil", "fin_length", "long", "body_color", "red", "fin_color", "red"));
        for (int i = 0; i < spots.length; i++) {
            BettaData d = BettaData.create(BettaGenome.of(genes.get(i % genes.size()), BettaGenome.DEFAULT), i > 0, level.getGameTime());
            Component err = TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d),
                    ModEntities.BETTA.get(), p, false);
            if (err != null) LOGGER.warn("[smoketest] M13 insert failed: {}", err.getString());
        }
        TankData t = c.getTank();
        int i = 0;
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) {
            if (!(e instanceof BettaEntity b) || i >= spots.length) continue;
            SmokeFreeze.freeze(b, true);
            b.moveTo(t.min().getX() + spots[i][0], t.min().getY() + spots[i][1], t.min().getZ() + spots[i][2], 70F + i * 140F, 0F);
            b.setYBodyRot(b.getYRot());
            b.setYHeadRot(b.getYRot());
            i++;
        }
    }

    /** Puts the eye of the player at (x, eyeY, z). */
    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.serverLevel(), x, eyeY - p.getEyeHeight(), z, yaw, pitch));
    }

    private void language(String code) {
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
        if (mc.screen != null) mc.screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    /** Returns true when all steps are done. */
    boolean tick() {
        if (mc.player != null && !steps.isEmpty()) {
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (steps.isEmpty()) return true;
        Step s = steps.peek();
        if (wait < s.delay()) {
            wait++;
            return false;
        }
        steps.poll();
        wait = 0;
        s.action().run();
        return steps.isEmpty();
    }

    private void server(java.util.function.Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToasts().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m13_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
