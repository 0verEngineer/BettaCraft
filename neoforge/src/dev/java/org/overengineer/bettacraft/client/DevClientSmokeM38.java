package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.client.interior.MiniatureSway;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.plants.foreign.ForeignPlants;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M38 client smoke ({@code -Psmoketestm38}): a freshwater tank planted only with plants of Minecraft and other mods (kelp,
 * seagrass, Fintastic's hornwort, Amazon sword, anubias, algae and moss, sea grapes, lily pads and duckweed; Regions
 * Unexplored's cattail and lily pads with {@code -Pbiomes}) and a salt water tank with Minecraft's corals, sea pickles and
 * Fintastic's live rock (Alex's Caves' ping-pong sponge with {@code -Paquatic}). Screenshots {@code bettacraft_m38_fresh_a}
 * and {@code _b} (a moment apart: they sway), {@code _reef}, {@code _plants_tab} (and {@code _de}). Checks (errors in the
 * log): the swaying quads of both tanks, every block of the default profiles that is registered sways in the miniature
 * (the flat floating leaves and the rock excepted), they count in the tank.
 */
final class DevClientSmokeM38 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final BlockPos base;
    private final String lang = Minecraft.getInstance().options.languageCode;
    private int wait;
    private int freshRegion = -1, reefRegion = -1;

    DevClientSmokeM38() {
        base = mc.player.blockPosition().offset(-40, 0, 940);
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(1, () -> look(base.getX() + 1.5, base.getY() + 2.0, base.getZ() - 1.9, 0F, 10F));
        then(160, () -> {
            int q = MiniatureSway.quads(freshRegion);
            LOGGER.info("[smoketest] M38 fresh tank: {} swaying quads captured, {} drawn in {} tank(s)", q, MiniatureSway.lastQuads, MiniatureSway.lastTanks);
            if (q <= 0 || MiniatureSway.lastQuads <= 0) LOGGER.error("[smoketest] M38 the plants of other mods do not sway in the fresh tank");
            shot("fresh_a");
        });
        then(11, () -> shot("fresh_b"));
        then(1, () -> look(base.getX() + 6.0, base.getY() + 2.0, base.getZ() - 1.7, 0F, 12F));
        then(80, () -> {
            int q = MiniatureSway.quads(reefRegion);
            LOGGER.info("[smoketest] M38 reef tank: {} swaying quads", q);
            if (q <= 0) LOGGER.error("[smoketest] M38 the corals do not sway in the reef tank");
            shot("reef");
        });
        then(1, this::swayTable);
        then(1, () -> mc.options.hideGui = false);
        then(1, () -> server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.level(), base.offset(0, 1, 0));
            var e = ForeignPlants.effect(p.level().getServer(), org.overengineer.bettacraft.plants.InteriorPlants.region(p.level().getServer(), c.getTank()));
            LOGGER.info("[smoketest] M38 fresh tank: {} plants of other mods, mass {}, floating {}, over the limit {}", e.plants(), Math.round(e.mass()),
                    e.floating(), e.overLimit());
            if (e.plants() < 10) LOGGER.error("[smoketest] M38 too few plants of other mods count: {}", e.plants());
            org.overengineer.bettacraft.platform.Platform.get().openMenu(p, new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        }));
        then(30, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.showAquaticPageForTest();
            else LOGGER.error("[smoketest] M38 no tank screen");
        });
        then(30, () -> shot("plants_tab"));
        then(1, () -> quickLanguage("de_de"));
        then(20, () -> shot("plants_tab_de"));
        then(1, () -> quickLanguage(lang));
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.setScreen(null));
    }

    /** Whether every block of the default profiles sways in the miniature (logged as a table for the report). */
    private void swayTable() {
        for (var f : ForeignPlants.files().entrySet()) {
            for (Identifier id : f.getValue().blocks()) {
                var b = BuiltInRegistries.BLOCK.getOptional(id);
                if (b.isEmpty()) {
                    LOGGER.info("[smoketest] M38 sway {}: not loaded", id);
                    continue;
                }
                boolean sways = MiniatureSway.candidate(b.get().defaultBlockState());
                LOGGER.info("[smoketest] M38 sway {}: {} ({})", id, sways ? "yes" : "no", f.getValue().role().id());
                boolean flat = f.getValue().role() == org.overengineer.bettacraft.plants.foreign.ForeignPlantRole.FLOATING
                        || f.getValue().role() == org.overengineer.bettacraft.plants.foreign.ForeignPlantRole.LIVE_ROCK
                        || id.getPath().endsWith("_block") || id.getPath().equals("sea_pickle");
                if (!sways && !flat) LOGGER.error("[smoketest] M38 {} does not sway in the miniature", id);
            }
            for (Identifier tag : f.getValue().tags()) {
                var key = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, tag);
                for (var h : BuiltInRegistries.BLOCK.getTagOrEmpty(key)) {
                    boolean sways = MiniatureSway.candidate(h.value().defaultBlockState());
                    LOGGER.info("[smoketest] M38 sway {} (#{}): {}", BuiltInRegistries.BLOCK.getKey(h.value()), tag, sways ? "yes" : "no");
                    if (!sways && !tag.getPath().equals("coral_blocks")) LOGGER.error("[smoketest] M38 {} does not sway", BuiltInRegistries.BLOCK.getKey(h.value()));
                }
            }
        }
    }

    // ------------------------------------------------------------------ the scene

    @javax.annotation.Nullable
    private static BlockState state(String id) {
        return BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id)).map(Block::defaultBlockState).orElse(null);
    }

    private static void put(ServerLevel in, InteriorRegion r, int x, int y, int z, @javax.annotation.Nullable BlockState s) {
        if (s == null || x < 0 || z < 0 || x >= r.sizeX() || z >= r.sizeZ() || y < 0 || y >= r.sizeY()) return;
        in.setBlock(r.toWorld(x, y, z), s, 2);
    }

    /** A column of {@code body} topped with {@code top}. */
    private static void column(ServerLevel in, InteriorRegion r, int x, int z, int h, @javax.annotation.Nullable BlockState body, @javax.annotation.Nullable BlockState top) {
        for (int y = 0; y < h; y++) put(in, r, x, y, z, y == h - 1 ? top : body);
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        ServerLevel in = InteriorManager.interiorLevel(p.level().getServer());
        for (int x = -4; x <= 14; x++)
            for (int z = -6; z <= 6; z++) {
                level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
                for (int y = 0; y < 6; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
        for (int x = -1; x <= 10; x++) for (int z = 0; z < 2; z++) level.setBlockAndUpdate(base.offset(x, 0, z), Blocks.SPRUCE_PLANKS.defaultBlockState());
        TankBlockEntity fresh = tank(level, p, base.offset(0, 1, 0), 3, 2, 2, 0F);
        TankBlockEntity reef = tank(level, p, base.offset(5, 1, 0), 3, 2, 2, Salinity.SEA);
        for (int x = 0; x < 3; x++) {
            level.setBlockAndUpdate(base.offset(x, 3, 0), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.LIT, true));
            level.setBlockAndUpdate(base.offset(5 + x, 3, 0), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.LIT, true));
        }
        InteriorRegion rf = InteriorManager.ensureRegion(level, fresh), rr = InteriorManager.ensureRegion(level, reef);
        freshRegion = rf.id();
        reefRegion = rr.id();
        floor(in, rf, Blocks.GRAVEL.defaultBlockState());
        floor(in, rr, Blocks.SAND.defaultBlockState());
        freshScape(in, rf);
        reefScape(in, rr);
        fresh.markTankChanged();
        reef.markTankChanged();
    }

    private static void floor(ServerLevel in, InteriorRegion r, BlockState s) {
        for (int x = 0; x < r.sizeX(); x++)
            for (int z = 0; z < r.sizeZ(); z++) Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), s, false);
    }

    /** 48 x 32 x 32: a kelp forest at the back, hornwort and sea grapes, seagrass meadows, Fintastic's plants in front, leaves on top. */
    private static void freshScape(ServerLevel in, InteriorRegion r) {
        java.util.Random rnd = new java.util.Random(38);
        BlockState kelp = Blocks.KELP_PLANT.defaultBlockState(), tip = Blocks.KELP.defaultBlockState();
        for (int x = 1; x < 47; x += 2) column(in, r, x, 26 + rnd.nextInt(4), 12 + rnd.nextInt(14), kelp, tip);
        BlockState hornwort = state("fintastic:hornwort"), grapes = state("fintastic:sea_grapes_plant"), grapesTip = state("fintastic:sea_grapes");
        for (int i = 0; i < 10; i++) column(in, r, 2 + i * 5, 20 + rnd.nextInt(3), 6 + rnd.nextInt(10), hornwort, hornwort);
        for (int i = 0; i < 8; i++) column(in, r, 4 + i * 6, 16 + rnd.nextInt(3), 4 + rnd.nextInt(6), grapes, grapesTip);
        // seagrass meadows: tall and short
        BlockState tallLow = Blocks.TALL_SEAGRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState tallUp = Blocks.TALL_SEAGRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER);
        for (int x = 0; x < 48; x++)
            for (int z = 8; z < 16; z++) {
                int n = rnd.nextInt(6);
                if (n == 0) {
                    put(in, r, x, 0, z, tallLow);
                    put(in, r, x, 1, z, tallUp);
                } else if (n == 1) put(in, r, x, 0, z, Blocks.SEAGRASS.defaultBlockState());
            }
        // Fintastic's plants in front, one kind per patch
        String[] front = {"fintastic:amazon_sword", "fintastic:anubias", "fintastic:red_algae", "fintastic:caulerpa", "fintastic:dragons_breath_algae",
                "fintastic:mermaid_fan", "fintastic:blue_hypnea", "fintastic:aquatic_moss_phyllid", "fintastic:red_algae_fan", "fintastic:stromatolite_growths",
                "regions_unexplored:cattail", "fintastic:lotus"};
        for (int i = 0; i < front.length; i++) {
            BlockState s = state(front[i]);
            if (s == null) continue;
            int x0 = 1 + i * 4;
            for (int dx = 0; dx < 3; dx++)
                for (int dz = 2; dz < 6; dz++) {
                    if (rnd.nextInt(3) == 0) continue;
                    if (s.hasProperty(DoublePlantBlock.HALF)) {
                        put(in, r, x0 + dx, 0, dz, s.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
                        put(in, r, x0 + dx, 1, dz, s.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
                    } else put(in, r, x0 + dx, 0, dz, s);
                }
        }
        BlockState tallSword = state("fintastic:tall_amazon_sword");
        if (tallSword != null) for (int i = 0; i < 4; i++) {
            put(in, r, 6 + i * 11, 0, 18, tallSword.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
            put(in, r, 6 + i * 11, 1, 18, tallSword.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
        }
        // algae carpet and moss
        BlockState carpet = state("fintastic:green_algae_carpet"), moss = state("fintastic:aquatic_moss_carpet");
        for (int x = 30; x < 40; x++) for (int z = 1; z < 3; z++) put(in, r, x, 0, z, (x + z) % 3 == 0 ? moss : carpet);
        // floating leaves at the surface
        int top = r.sizeY() - 3;
        String[] floating = {"minecraft:lily_pad", "fintastic:duckweed", "fintastic:lotus_pad", "regions_unexplored:duckweed", "regions_unexplored:flowering_lily_pad"};
        for (int i = 0; i < 14; i++) put(in, r, 2 + rnd.nextInt(44), top, 4 + rnd.nextInt(26), state(floating[i % floating.length]));
    }

    /** 48 x 32 x 32 in salt water: coral blocks with fans, coral plants, sea pickles, Fintastic's live rock. */
    private static void reefScape(ServerLevel in, InteriorRegion r) {
        java.util.Random rnd = new java.util.Random(380);
        BlockState live = state("fintastic:live_rock"), porous = state("fintastic:porous_live_rock");
        for (int x = 4; x < 44; x++)
            for (int z = 18; z < 26; z++) {
                int h = 3 + (int) (4 * Math.sin(x / 5.0) * Math.cos(z / 4.0)) + rnd.nextInt(2);
                for (int y = 0; y < Math.max(1, h); y++) put(in, r, x, y, z, live != null && rnd.nextBoolean() ? (rnd.nextBoolean() ? live : porous)
                        : Blocks.HORN_CORAL_BLOCK.defaultBlockState());
            }
        Block[] fans = {Blocks.BRAIN_CORAL_FAN, Blocks.TUBE_CORAL_FAN, Blocks.FIRE_CORAL_FAN, Blocks.BUBBLE_CORAL_FAN, Blocks.HORN_CORAL_FAN};
        Block[] plants = {Blocks.BRAIN_CORAL, Blocks.TUBE_CORAL, Blocks.FIRE_CORAL, Blocks.BUBBLE_CORAL, Blocks.HORN_CORAL};
        for (int x = 2; x < 46; x++)
            for (int z = 2; z < 16; z++) {
                int n = rnd.nextInt(7);
                if (n == 0) put(in, r, x, 0, z, fans[rnd.nextInt(fans.length)].defaultBlockState());
                else if (n == 1) put(in, r, x, 0, z, plants[rnd.nextInt(plants.length)].defaultBlockState());
                else if (n == 2 && rnd.nextInt(3) == 0) put(in, r, x, 0, z, Blocks.SEA_PICKLE.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.SeaPickleBlock.PICKLES, 1 + rnd.nextInt(4)));
            }
        // coral plants on top of the rock
        for (int x = 4; x < 44; x += 2) {
            int y = 0;
            while (y < r.sizeY() && !in.getBlockState(r.toWorld(x, y, 18)).isAir()) y++;
            put(in, r, x, y, 18, fans[x % fans.length].defaultBlockState());
            put(in, r, x, 1, 17, Blocks.FIRE_CORAL_WALL_FAN.defaultBlockState().setValue(net.minecraft.world.level.block.BaseCoralWallFanBlock.FACING, Direction.NORTH));
        }
        BlockState sponge = state("alexscaves:ping_pong_sponge");
        if (sponge != null) for (int i = 0; i < 6; i++) put(in, r, 6 + i * 7, 0, 8, sponge);
    }

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz, float salt) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, (long) sx * sy * sz * 900, salt);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(p.getUUID());
        c.getTank().setWaterTemp(25F);
        c.getTank().setTargetTemp(25F);
        c.markTankChanged();
        return c;
    }

    // ------------------------------------------------------------------ helpers

    private void quickLanguage(String code) {
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
        if (mc.screen != null) mc.screen.resize(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

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
        mc.getToastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m38_" + name + ".png", mc.getMainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
