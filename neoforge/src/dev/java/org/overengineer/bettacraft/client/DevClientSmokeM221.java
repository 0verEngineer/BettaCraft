package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.client.interior.IrisCompat;
import org.overengineer.bettacraft.client.interior.MiniaturePlants;
import org.overengineer.bettacraft.client.interior.MiniatureSway;
import org.overengineer.bettacraft.client.interior.PlantMeshRenderer;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M22.1 dev client smoke scene ({@code -Psmoketestm22_1}): every plant and coral with its new shape, inside and outside,
 * near and far, old and new; kelp, seagrass and corals swaying in the miniature; polyps closing near a fish and near the
 * player. Screenshots {@code bettacraft_m22_1_*} ({@code _old}: client setting {@code render.plantMeshes} off).
 */
final class DevClientSmokeM221 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    /** A plant of the scene: where it stands (interior-local) and in which tank. */
    private record Spot(PlantSpecies species, int tank, int x, int z) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final BlockPos[] tanks = new BlockPos[4];
    private final int[] widths = {3, 4, 4, 2};
    private final InteriorRegion[] regions = new InteriorRegion[4];
    private final List<Spot> spots = new ArrayList<>();
    private final List<int[]> tops = new ArrayList<>();
    private int wait;
    private final boolean savedMeshes = ClientConfig.plantMeshes();
    private int maxClosedOutside;
    private BlockPos base;

    DevClientSmokeM221() {
        // epiphytes on stone and wood, rooted plants, corals on reef rock, Minecraft's water plants
        int[][] epi = {{8, 14}, {18, 12}, {28, 14}, {38, 12}, {13, 22}, {33, 22}};
        PlantSpecies[] epis = {PlantSpecies.JAVA_FERN, PlantSpecies.BUCE_GREEN_WAVY, PlantSpecies.BUCE_BROWNIE_RED, PlantSpecies.BUCE_BLUE,
                PlantSpecies.BUCE_MINI_NEEDLE, PlantSpecies.JAVA_MOSS};
        for (int i = 0; i < epis.length; i++) spots.add(new Spot(epis[i], 0, epi[i][0], epi[i][1]));
        PlantSpecies[] rooted = {PlantSpecies.CRYPT_GREEN, PlantSpecies.CRYPT_BROWN, PlantSpecies.CRYPT_FLAMINGO, PlantSpecies.AMAZON_SWORD,
                PlantSpecies.LUDWIGIA, PlantSpecies.ALTERNANTHERA, PlantSpecies.MONTE_CARLO, PlantSpecies.TIGER_LOTUS};
        int[][] ro = {{7, 10}, {19, 10}, {31, 10}, {49, 20}, {10, 24}, {24, 24}, {40, 8}, {36, 23}};
        for (int i = 0; i < rooted.length; i++) spots.add(new Spot(rooted[i], 1, ro[i][0], ro[i][1]));
        PlantSpecies[] corals = {PlantSpecies.ZOANTHUS, PlantSpecies.GREEN_STAR_POLYPS, PlantSpecies.MUSHROOM_CORAL, PlantSpecies.LEATHER_CORAL,
                PlantSpecies.HAMMER_CORAL, PlantSpecies.LOBOPHYLLIA, PlantSpecies.ACROPORA, PlantSpecies.BUBBLE_TIP_ANEMONE};
        int[][] co = {{8, 10}, {22, 9}, {38, 10}, {53, 11}, {9, 23}, {24, 23}, {40, 23}, {54, 24}};
        for (int i = 0; i < corals.length; i++) spots.add(new Spot(corals[i], 2, co[i][0], co[i][1]));

        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(1, () -> meshes(true));
        // outside: every tank near and far, new and old
        for (boolean on : new boolean[] {true, false}) {
            String tag = on ? "new" : "old";
            then(1, () -> meshes(on));
            for (int t = 0; t < 4; t++) {
                int ti = t;
                then(1, () -> look(tanks[ti].getX() + widths[ti] / 2.0, tanks[ti].getY() + 1.2, tanks[ti].getZ() - 2.6, 0F, 6F));
                then(ti == 0 && on ? 200 : 70, () -> {
                    if (on && ti < 3) {
                        LOGGER.info("[smoketest] M22.1 outside tank {}: {} tank(s) with meshes, {} plants drawn, {} ms render thread", ti,
                                MiniaturePlants.lastTanks, MiniaturePlants.lastDrawn, MiniaturePlants.lastNanos / 1e6);
                        if (MiniaturePlants.lastTanks < 1 || MiniaturePlants.lastDrawn < 4) LOGGER.error("[smoketest] M22.1 plant meshes not shown outside (tank {})", ti);
                    }
                    shot("outside_" + name(ti) + "_" + tag);
                });
                then(1, () -> look(tanks[ti].getX() + widths[ti] / 2.0, tanks[ti].getY() + 3.0, tanks[ti].getZ() - 11.0, 0F, 9F));
                then(50, () -> shot("outside_" + name(ti) + "_far_" + tag));
            }
        }
        then(1, () -> meshes(true));
        // Minecraft's water plants sway outside: two pictures a moment apart, the quads drawn swaying
        then(1, () -> look(tanks[3].getX() + 1.0, tanks[3].getY() + 1.1, tanks[3].getZ() - 1.5, 0F, 4F));
        then(80, () -> {
            int q = MiniatureSway.quads(regions[3].id());
            LOGGER.info("[smoketest] M22.1 swaying outside: {} quads captured, {} drawn in {} tank(s)", q, MiniatureSway.lastQuads, MiniatureSway.lastTanks);
            if (q <= 0 || MiniatureSway.lastQuads <= 0) LOGGER.error("[smoketest] M22.1 kelp and seagrass do not sway outside");
            shot("outside_kelp_a");
        });
        then(11, () -> shot("outside_kelp_b"));
        then(1, () -> ClientConfig.SWAYING_PLANTS.set(false));
        then(10, () -> {
            if (MiniatureSway.lastQuads != 0) LOGGER.error("[smoketest] M22.1 swaying with render.swayingPlants off");
            shot("outside_kelp_still");
            ClientConfig.SWAYING_PLANTS.set(true);
        });
        // shadows with a shader pack (the meshes of the miniature in the shadow pass)
        then(1, () -> look(tanks[1].getX() + 2.0, tanks[1].getY() + 1.2, tanks[1].getZ() - 2.6, 0F, 6F));
        then(60, () -> {
            if (IrisCompat.shaderPackInUse()) {
                LOGGER.info("[smoketest] M22.1 shadow pass: {} meshes", PlantMeshRenderer.lastShadowDrawn);
                if (PlantMeshRenderer.lastShadowDrawn == 0) LOGGER.error("[smoketest] M22.1 plant meshes not in the shadow pass");
            }
        });
        // polyps outside close when a fish swims by (the reef tank has fish): watch a while
        then(1, () -> look(tanks[2].getX() + 2.0, tanks[2].getY() + 1.2, tanks[2].getZ() - 2.6, 0F, 6F));
        // one clownfish swims right over the zoanthids (in the tank: the miniature's coordinates)
        then(1, () -> server(p -> {
            Spot z = spots.stream().filter(sp -> sp.species() == PlantSpecies.ZOANTHUS).findFirst().orElseThrow();
            InteriorRegion r = regions[2];
            var c = TankMultiblock.getController(p.level(), tanks[2]);
            float m = 0.5F / 16F;
            double f = (widths[2] - 2 * m) / (double) r.sizeX();
            Vec3 at = new Vec3(tanks[2].getX() + m + (z.x() + 0.5) * f, tanks[2].getY() + m + (top(z) + 3) * f, tanks[2].getZ() + m + (z.z() + 0.5) * f);
            var fish = org.overengineer.bettacraft.tankmates.TankmateBreeding.tankmates(p.level(), c.getTank());
            if (!fish.isEmpty()) fish.getFirst().snapTo(at.x, at.y, at.z);
        }));
        for (int i = 0; i < 20; i++) {
            then(10, () -> maxClosedOutside = Math.max(maxClosedOutside, MiniaturePlants.closedMeshes(regions[2].id(), time())));
        }
        then(1, () -> {
            LOGGER.info("[smoketest] M22.1 polyps closed by the fish outside: {} plants", maxClosedOutside);
            if (maxClosedOutside == 0) LOGGER.error("[smoketest] M22.1 the polyps outside never closed for a fish");
        });
        // inside: every species close up, new and old
        for (boolean on : new boolean[] {true, false}) {
            String tag = on ? "new" : "old";
            then(1, () -> meshes(on));
            for (int i = 0; i < spots.size(); i++) {
                int si = i;
                then(1, () -> insideAt(spots.get(si)));
                then(si == 0 ? 100 : 40, () -> shot("inside_" + spots.get(si).species().id() + "_" + tag));
            }
            for (int t = 0; t < 3; t++) {
                int ti = t;
                then(1, () -> insideWide(ti));
                then(60, () -> shot("inside_" + name(ti) + "_wide_" + tag));
            }
        }
        then(1, () -> meshes(true));
        // the player swims up to the zoanthids: they close
        then(1, () -> {
            Spot z = spots.stream().filter(s -> s.species() == PlantSpecies.ZOANTHUS).findFirst().orElseThrow();
            server(p -> {
                InteriorRegion r = regions[z.tank()];
                BlockPos at = r.toWorld(z.x(), top(z) + 2, z.z() - 2);
                p.teleportTo(InteriorManager.interiorLevel(p.server), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F, 30F);
            });
        });
        then(30, () -> {
            int closed = PlantMeshRenderer.closedMeshes(time());
            LOGGER.info("[smoketest] M22.1 polyps closed next to the player inside: {} plants", closed);
            if (closed == 0) LOGGER.error("[smoketest] M22.1 the zoanthids did not close next to the player");
            shot("inside_zoanthus_closed");
        });
        // the question before animals leave a bucket: the screen, "Cancel" keeps the betta, "Empty" lets it go
        then(1, () -> {
            look(base.getX() - 3.5, base.getY() + 1.62, base.getZ() - 4.5, 0F, 70F);
            server(p -> {
                var g = org.overengineer.bettacraft.genetics.BettaGenome.randomDomestic(net.minecraft.util.RandomSource.create(2211));
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, org.overengineer.bettacraft.entity.betta.BettaBucketItem.withFish(
                        new net.minecraft.world.item.ItemStack(org.overengineer.bettacraft.registry.ModItems.BETTA_BUCKET.get()),
                        org.overengineer.bettacraft.entity.betta.BettaData.create(g, false, 0)));
                p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            });
        });
        then(30, () -> {
            mc.options.hideGui = false;
            mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        });
        then(20, () -> {
            if (!(mc.screen instanceof org.overengineer.bettacraft.client.screen.ConfirmEmptyScreen)) LOGGER.error("[smoketest] M22.1 no confirm screen for the betta bucket");
            shot("confirm_screen");
        });
        then(5, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.ConfirmEmptyScreen c) c.cancel();
        });
        then(20, () -> {
            boolean kept = mc.player.getMainHandItem().is(org.overengineer.bettacraft.registry.ModItems.BETTA_BUCKET.get());
            LOGGER.info("[smoketest] M22.1 confirm screen: cancel keeps the betta: {}", kept);
            if (!kept) LOGGER.error("[smoketest] M22.1 the betta bucket emptied although cancelled");
            mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        });
        then(20, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.ConfirmEmptyScreen c) c.confirm();
            else LOGGER.error("[smoketest] M22.1 no confirm screen the second time");
        });
        then(30, () -> {
            boolean empty = mc.player.getMainHandItem().is(net.minecraft.world.item.Items.BUCKET);
            int bettas = mc.level.getEntitiesOfClass(org.overengineer.bettacraft.entity.betta.BettaEntity.class, mc.player.getBoundingBox().inflate(6)).size();
            LOGGER.info("[smoketest] M22.1 confirm screen: empty lets the betta go: bucket empty {}, {} betta(s) near", empty, bettas);
            if (!empty || bettas < 1) LOGGER.error("[smoketest] M22.1 the confirmed betta bucket did not empty");
            mc.options.hideGui = true;
            shot("confirm_released");
            server(p -> p.setGameMode(net.minecraft.world.level.GameType.CREATIVE));
        });
        then(1, () -> ClientConfig.PLANT_MESHES.set(savedMeshes));
    }

    private static String name(int t) {
        return switch (t) {
            case 0 -> "epiphytes";
            case 1 -> "rooted";
            case 2 -> "reef";
            default -> "kelp";
        };
    }

    private float time() {
        return mc.level == null ? 0 : mc.level.getGameTime();
    }

    private int top(Spot s) {
        for (int[] t : tops) if (t[0] == s.tank() && t[1] == s.x() && t[2] == s.z()) return t[3];
        return 0;
    }

    private void insideAt(Spot s) {
        server(p -> {
            InteriorRegion r = regions[s.tank()];
            int size = switch (s.species()) {
                case JAVA_FERN, AMAZON_SWORD, LUDWIGIA, ALTERNANTHERA, TIGER_LOTUS, LEATHER_CORAL, ACROPORA -> 20;
                case MONTE_CARLO, JAVA_MOSS, ZOANTHUS, GREEN_STAR_POLYPS, MUSHROOM_CORAL -> 10;
                default -> 13;
            };
            int y = top(s);
            BlockPos at = r.toWorld(s.x(), y + size / 3, s.z() - size);
            p.teleportTo(InteriorManager.interiorLevel(p.server), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F, 12F);
        });
    }

    private void insideWide(int t) {
        server(p -> {
            InteriorRegion r = regions[t];
            BlockPos at = r.toWorld(r.sizeX() / 2, 14, 1);
            p.teleportTo(InteriorManager.interiorLevel(p.server), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F, 22F);
        });
    }

    private void meshes(boolean on) {
        ClientConfig.PLANT_MESHES.set(on);
        if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
    }

    // ------------------------------------------------------------------ the scene

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, float salt) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, (long) sx * 4 * 1000, salt);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(p.getUUID());
        c.getTank().setWaterTemp(25F);
        c.getTank().setTargetTemp(25F);
        c.markTankChanged();
        for (int x = 0; x < sx; x++) {
            level.setBlockAndUpdate(min.offset(x, 2, 1), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < sx - 1).setValue(TankLightBlock.LIT, true)
                    .setValue(TankLightBlock.COLOR, salt > 0 && x % 2 == 0 ? DyeColor.BLUE : DyeColor.WHITE));
        }
        return c;
    }

    private static void mound(ServerLevel in, InteriorRegion r, int cx, int cz, int rad, int h, BlockState a, BlockState b) {
        for (int x = -rad; x <= rad; x++)
            for (int z = -rad; z <= rad; z++) {
                double d = Math.sqrt(x * x + z * z) / rad;
                if (d > 1) continue;
                int top = (int) Math.round(h * Math.max(0.2, 1 - d * d * 0.8));
                for (int y = 0; y < top; y++) in.setBlock(r.toWorld(cx + x, y, cz + z), (x + y + z) % 3 == 0 ? b : a, 2);
            }
    }

    private static int topAt(ServerLevel in, InteriorRegion r, int x, int z) {
        int y = 0;
        while (y < r.sizeY() && !in.getBlockState(r.toWorld(x, y, z)).isAir()) y++;
        return y;
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        ServerLevel in = InteriorManager.interiorLevel(p.server);
        base = p.blockPosition().offset(-30, 2, 360);
        int x0 = 0;
        for (int t = 0; t < 4; t++) {
            tanks[t] = base.offset(x0, 0, 0);
            x0 += widths[t] + 2;
            TankBlockEntity c = tank(level, p, tanks[t], widths[t], t == 2 ? Salinity.SEA : 0F);
            InteriorRegion r = InteriorManager.ensureRegion(level, c);
            regions[t] = r;
            BlockState floor = t == 2 || t == 3 ? Blocks.SAND.defaultBlockState() : t == 0 ? Blocks.GRAVEL.defaultBlockState() : Blocks.DIRT.defaultBlockState();
            for (int x = 0; x < r.sizeX(); x++)
                for (int z = 0; z < r.sizeZ(); z++) Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), floor, false);
        }
        long seed = 22100;
        for (Spot s : spots) {
            InteriorRegion r = regions[s.tank()];
            if (s.tank() == 0) {
                // a stone or a piece of wood under every epiphyte (moss on a stone too)
                boolean wood = s.species() == PlantSpecies.JAVA_FERN || s.species() == PlantSpecies.BUCE_BLUE;
                mound(in, r, s.x(), s.z(), 3, 4, wood ? Blocks.OAK_LOG.defaultBlockState() : Blocks.STONE.defaultBlockState(),
                        wood ? Blocks.OAK_LOG.defaultBlockState() : Blocks.ANDESITE.defaultBlockState());
            } else if (s.tank() == 2) {
                mound(in, r, s.x(), s.z(), 4, 4, ModBlocks.REEF_ROCK.get().defaultBlockState(), ModBlocks.LIVE_ROCK.get().defaultBlockState());
            }
            int y = topAt(in, r, s.x(), s.z());
            tops.add(new int[] {s.tank(), s.x(), s.z(), y});
            InteriorPlant pl = InteriorPlants.plantAt(in, r, s.species(), s.x(), y, s.z(), s.species().stages(), seed++);
            if (pl == null) {
                LOGGER.error("[smoketest] M22.1 {} not planted", s.species().id());
                continue;
            }
            if (s.species().marine()) {
                pl.tint = 2;
                pl.redness = 0.67;
                InteriorPlants.apply(in, r, pl);
            }
        }
        for (int t = 0; t < 2; t++) InteriorPlants.settleTint(level, TankMultiblock.getController(level, tanks[t]));
        // Minecraft's water plants: kelp of several heights, seagrass, corals and coral fans
        InteriorRegion k = regions[3];
        for (int i = 0; i < 7; i++) {
            int kx = 4 + i * 4, kz = 12 + (i % 2) * 6, h = 10 + (i * 7) % 18;
            for (int y = 0; y < h; y++) in.setBlock(k.toWorld(kx, y, kz), (y == h - 1 ? Blocks.KELP : Blocks.KELP_PLANT).defaultBlockState(), 2);
        }
        for (int x = 2; x < 30; x += 3) {
            in.setBlock(k.toWorld(x, 0, 6), Blocks.SEAGRASS.defaultBlockState(), 2);
            if (x % 2 == 0) {
                in.setBlock(k.toWorld(x + 1, 0, 26), Blocks.TALL_SEAGRASS.defaultBlockState(), 2);
                in.setBlock(k.toWorld(x + 1, 1, 26), Blocks.TALL_SEAGRASS.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.TallSeagrassBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), 2);
            }
        }
        BlockState[] corals = {Blocks.TUBE_CORAL.defaultBlockState(), Blocks.BRAIN_CORAL_FAN.defaultBlockState(), Blocks.FIRE_CORAL.defaultBlockState(),
                Blocks.HORN_CORAL_FAN.defaultBlockState(), Blocks.BUBBLE_CORAL.defaultBlockState()};
        for (int i = 0; i < 10; i++) in.setBlock(k.toWorld(3 + i * 3, 0, 20), corals[i % corals.length], 2);
        // fish in the reef tank (they close the polyps they swim by)
        TankBlockEntity reef = TankMultiblock.getController(level, tanks[2]);
        for (int i = 0; i < 4; i++) {
            TankWaterVolume w = new TankWaterVolume(reef.getTank());
            Vec3 pos = new Vec3(w.box().minX + 0.4 + i * 0.9, w.box().minY + 0.25, w.box().minZ + 0.4 + (i % 2) * 0.7);
            var e = TankmateBreeding.spawn(level, reef, TankmateSpecies.OCELLARIS_CLOWNFISH, TankmateGenes.wild(TankmateSpecies.OCELLARIS_CLOWNFISH), i % 2 == 0,
                    LifeStage.ADULT, 0, pos);
            if (e != null) e.health().feed();
        }
        int blocks = 0;
        for (InteriorRegion r : regions) blocks += r.plants().blocks();
        LOGGER.info("[smoketest] M22.1 scene at {}: {} plants, {} plant blocks", base, spots.size(), blocks);
    }

    // ------------------------------------------------------------------ steps

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> {
            if (InteriorManager.isInterior(p.level())) p.teleportTo(p.server.overworld(), x, eyeY - p.getEyeHeight(), z, yaw, pitch);
            else p.teleportTo(p.level(), x, eyeY - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, false);
        });
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m22_1_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
