package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.plants.wild.WildFloatingPlantBlock;
import org.overengineer.bettacraft.plants.wild.WildPlantBlock;
import org.overengineer.bettacraft.plants.wild.WildPlantFeature;
import org.overengineer.bettacraft.sources.WildTables.PlantWater;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M28.1 part b dev client smoke scene ({@code -Psmoketestm281}): three ponds with the biome of their water (river, swamp,
 * jungle; set with {@code /fillbiome}), planted by the wild plant feature with the species of that water, floating
 * patches on the surface. From above the water and under water; a cold pond with Marimo balls. Screenshots
 * {@code bettacraft_m281_*}; with {@code -Pshaders} one more of the jungle.
 */
final class DevClientSmokeM281 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static final int SIZE = 11, DEPTH = 3;

    private record Step(int delay, Runnable action) {}

    private record Group(PlantSpecies species, int x, int z) {}

    private static final String[] NAMES = {"river", "swamp", "jungle", "cold"};
    private static final String[] BIOMES = {"minecraft:river", "minecraft:swamp", "minecraft:jungle", "minecraft:snowy_taiga"};
    private static final PlantWater[] WATERS = {PlantWater.RIVER, PlantWater.SWAMP, PlantWater.JUNGLE, PlantWater.COLD};

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final BlockPos[] ponds = new BlockPos[4];
    private int wait;

    DevClientSmokeM281() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        for (int i = 0; i < 4; i++) {
            int pi = i;
            // above: from the south bank, looking down onto the pond
            then(1, () -> look(ponds[pi].getX() + SIZE / 2.0, ponds[pi].getY() + DEPTH + 3.2, ponds[pi].getZ() + SIZE + 1.5, 180F, 38F));
            then(pi == 0 ? 200 : 80, () -> shot(NAMES[pi] + "_above"));
            // under water: from the middle of the south side, looking north over the bottom
            then(1, () -> look(ponds[pi].getX() + SIZE / 2.0, ponds[pi].getY() + 1.3, ponds[pi].getZ() + SIZE - 0.6, 180F, 12F));
            then(60, () -> shot(NAMES[pi] + "_under"));
        }
        then(1, () -> look(ponds[2].getX() + SIZE / 2.0, ponds[2].getY() + DEPTH + 3.2, ponds[2].getZ() + SIZE + 1.5, 180F, 38F));
        then(80, () -> {
            if (org.overengineer.bettacraft.client.interior.IrisCompat.shaderPackInUse()) shot("jungle_above_shaders");
        });
        then(1, () -> look(ponds[2].getX() + SIZE / 2.0, ponds[2].getY() + 1.3, ponds[2].getZ() + SIZE - 0.6, 180F, 12F));
        then(60, () -> {
            if (org.overengineer.bettacraft.client.interior.IrisCompat.shaderPackInUse()) shot("jungle_under_shaders");
        });
    }

    private static Group[] groups(int pond) {
        return switch (pond) {
            case 0 -> new Group[]{new Group(PlantSpecies.VALLISNERIA, 2, 2), new Group(PlantSpecies.SAGITTARIA, 7, 3), new Group(PlantSpecies.ELEOCHARIS, 3, 7),
                    new Group(PlantSpecies.JAVA_MOSS, 8, 8), new Group(PlantSpecies.AMAZON_SWORD, 5, 5), new Group(PlantSpecies.FLAME_MOSS, 1, 9)};
            case 1 -> new Group[]{new Group(PlantSpecies.LUDWIGIA, 2, 2), new Group(PlantSpecies.ROTALA, 5, 2), new Group(PlantSpecies.LIMNOPHILA_SESSILIFLORA, 8, 3),
                    new Group(PlantSpecies.MARSILEA, 3, 7), new Group(PlantSpecies.NYMPHAEA_GREEN, 8, 8), new Group(PlantSpecies.BACOPA_CAROLINIANA, 5, 6),
                    new Group(PlantSpecies.MONTE_CARLO, 1, 9)};
            case 2 -> new Group[]{new Group(PlantSpecies.ANUBIAS_NANA, 2, 2), new Group(PlantSpecies.BUCE_BROWNIE_RED, 5, 2), new Group(PlantSpecies.JAVA_FERN, 8, 2),
                    new Group(PlantSpecies.CRYPT_BROWN, 2, 6), new Group(PlantSpecies.AMAZON_SWORD, 8, 6), new Group(PlantSpecies.TIGER_LOTUS, 5, 5),
                    new Group(PlantSpecies.HYGROPHILA_PINNATIFIDA, 3, 9), new Group(PlantSpecies.BUCE_BLUE, 7, 9)};
            default -> new Group[]{new Group(PlantSpecies.MARIMO, 4, 4), new Group(PlantSpecies.MARIMO, 7, 7), new Group(PlantSpecies.ELEOCHARIS, 2, 8)};
        };
    }

    private static FloatingPlantType[] floating(int pond) {
        return switch (pond) {
            case 0 -> new FloatingPlantType[]{FloatingPlantType.HORNWORT};
            case 1 -> new FloatingPlantType[]{FloatingPlantType.DUCKWEED, FloatingPlantType.SALVINIA, FloatingPlantType.FROGBIT};
            case 2 -> new FloatingPlantType[]{FloatingPlantType.WATER_LETTUCE, FloatingPlantType.RED_ROOT_FLOATER};
            default -> new FloatingPlantType[]{FloatingPlantType.HORNWORT};
        };
    }

    private static final Block[] FLOORS = {Blocks.GRAVEL, Blocks.MUD, Blocks.COARSE_DIRT, Blocks.GRAVEL};
    private static final Block[] BANKS = {Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK, Blocks.SNOW_BLOCK};

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        level.setDayTime(6000);
        BlockPos base = p.blockPosition().offset(-30, 4, 380); // above the superflat ground (its floor lies at the bottom of the world)
        int total = 0, floatingTotal = 0;
        for (int i = 0; i < 4; i++) {
            BlockPos min = base.offset(i * (SIZE + 6), 0, 0);
            ponds[i] = min;
            for (int x = -4; x < SIZE + 4; x++)
                for (int z = -4; z < SIZE + 4; z++) {
                    boolean water = x >= 0 && z >= 0 && x < SIZE && z < SIZE;
                    level.setBlock(min.offset(x, -2, z), Blocks.STONE.defaultBlockState(), 2);
                    level.setBlock(min.offset(x, -1, z), water ? FLOORS[i].defaultBlockState() : Blocks.DIRT.defaultBlockState(), 2);
                    for (int y = 0; y < DEPTH; y++) level.setBlock(min.offset(x, y, z), water ? Blocks.WATER.defaultBlockState()
                            : y == DEPTH - 1 ? BANKS[i].defaultBlockState() : Blocks.DIRT.defaultBlockState(), 2);
                    for (int y = DEPTH; y < DEPTH + 12; y++) level.setBlock(min.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            BlockPos a = min.offset(-4, -3, -4), b = min.offset(SIZE + 3, DEPTH + 12, SIZE + 3);
            command(p, "fillbiome " + a.getX() + " " + a.getY() + " " + a.getZ() + " " + b.getX() + " " + b.getY() + " " + b.getZ() + " " + BIOMES[i]);
            RandomSource r = RandomSource.create(28110 + i);
            for (Group g : groups(i)) {
                if (WildPlantFeature.placeGroup(level, min.offset(g.x(), 0, g.z()), r, g.species(), WATERS[i], false)) total++;
                else LOGGER.error("[smoketest] M28.1 {} did not grow in the {} pond", g.species().id(), NAMES[i]);
            }
            int k = 0;
            for (FloatingPlantType t : floating(i)) {
                BlockPos at = min.offset(k == 0 ? 7 : k == 1 ? 2 : 6, 0, k == 0 ? 2 : k == 1 ? 5 : 9);
                if (WildPlantFeature.placeFloating(level, at, r, t, false)) floatingTotal++;
                else LOGGER.error("[smoketest] M28.1 no {} on the {} pond", t.id(), NAMES[i]);
                k++;
            }
        }
        // a sunken root with Anubias in the jungle pond, as the feature does on soft ground
        BlockState anubias = org.overengineer.bettacraft.registry.ModBlocks.WILD_PLANTS.get(org.overengineer.bettacraft.plants.wild.WildPlantKind.EPIPHYTE).get()
                .withSpecies(PlantSpecies.ANUBIAS_PINTO);
        level.setBlock(ponds[2].offset(10, -1, 5), Blocks.JUNGLE_LOG.defaultBlockState()
                .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.Z), 2);
        level.setBlock(ponds[2].offset(10, 0, 5), anubias, 2);
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 600, 0, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 20 * 600, 0, false, false));
        int blocks = 0;
        for (BlockPos min : ponds)
            for (int x = 0; x < SIZE; x++)
                for (int z = 0; z < SIZE; z++)
                    for (int y = 0; y <= DEPTH; y++) {
                        Block bl = level.getBlockState(min.offset(x, y, z)).getBlock();
                        if (bl instanceof WildPlantBlock || bl instanceof WildFloatingPlantBlock) blocks++;
                    }
        LOGGER.info("[smoketest] M28.1 scene at {}: {} groups, {} floating patches, {} wild plant blocks", base, total, floatingTotal, blocks);
    }

    private static void command(ServerPlayer p, String cmd) {
        p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(), cmd);
    }

    // ------------------------------------------------------------------ steps

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.level(), x, eyeY - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, false));
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m281_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
