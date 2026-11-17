package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.EquipmentPlacement;
import org.overengineer.bettacraft.equipment.SegmentBlock;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.reef.ProteinSkimmerBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tankmates.Clutch;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.tankmates.TankmateSpots;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M20 part of the dev client smoke test: a reef tank (sea water, reef rock and live rock with all corals and the bubble
 * tip anemone, a clownfish pair in its anemone with eggs at its foot, a tang, chromis, a goby, a damsel, cleaner shrimp,
 * hermit crabs and turbo snails, actinic and white lights, a protein skimmer, a flow pump), the water colours of fresh,
 * brackish and sea water side by side, an Acropora bleached / pale / healthy / glowing, inside the reef interior, the
 * water, plants, animals and breeding tabs of the menu and the items (English, German). Screenshots
 * {@code bettacraft_m20_*}. Checks (errors in the log): the client knows the salinity of the tanks and draws the
 * water in its colour, sees the reef fish and the clutch.
 */
final class DevClientSmokeM20 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private BlockPos base, reef, colors, corals;
    private volatile InteriorRegion reefRegion;
    private int wait;

    DevClientSmokeM20() {
        then(1, () -> server(p -> p.teleportTo(p.level(), p.getX(), p.getY() + 2, p.getZ() + 520, java.util.Set.of(), 0F, 10F, false)));
        then(20, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(160, () -> server(this::pose));
        // 1. the reef tank
        then(40, () -> look(reef.getX() + 2.5, reef.getY() + 1.7, reef.getZ() - 4.3, 0F, 10F));
        then(60, () -> {
            shot("reef_tank");
            check();
        });
        then(1, () -> look(reef.getX() + 1.3, reef.getY() + 1.0, reef.getZ() - 1.5, 0F, 10F));
        then(40, () -> shot("reef_clownfish"));
        then(1, () -> look(reef.getX() + 3.6, reef.getY() + 1.2, reef.getZ() - 1.6, 0F, 14F));
        then(40, () -> shot("reef_corals"));
        then(1, () -> look(reef.getX() + 2.5, reef.getY() + 0.6, reef.getZ() - 1.3, 0F, 18F));
        then(40, () -> shot("reef_bottom"));
        then(1, () -> look(reef.getX() - 1.8, reef.getY() + 3.6, reef.getZ() - 2.2, -35F, 38F));
        then(40, () -> shot("reef_technique"));
        // 2. water colours and coral colours
        then(1, () -> look(colors.getX() + 5.0, colors.getY() + 3.2, colors.getZ() - 6.0, 0F, 22F));
        then(40, () -> {
            shot("water_colors");
            checkColors();
        });
        then(1, () -> look(corals.getX() + 5.5, corals.getY() + 1.4, corals.getZ() - 6.0, 0F, 8F));
        then(40, () -> shot("coral_colors"));
        // 3. inside the reef
        then(1, () -> server(p -> {
            InteriorRegion r = reefRegion;
            if (r == null) return;
            ServerLevel in = InteriorManager.interiorLevel(p.server);
            BlockPos at = r.toWorld(40, 14, 2);
            p.teleportTo(in, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F, 18F);
        }));
        then(120, () -> shot("reef_interior"));
        then(1, () -> server(p -> {
            if (InteriorManager.isInterior(p.level())) p.teleportTo(p.server.overworld(), reef.getX() + 2.5, reef.getY() + 0.5, reef.getZ() - 2.5, 0F, 0F);
        }));
        // 4. menus
        then(10, () -> mc.options.hideGui = false);
        then(1, () -> server(p -> {
            reach(p, true);
            p.teleportTo(p.level(), reef.getX() + 2.5, reef.getY() + 0.5, reef.getZ() - 2.5, java.util.Set.of(), 0F, 0F, false);
        }));
        String lang = mc.options.languageCode;
        menu(5, "menu_water", lang, null);
        menu(6, "menu_corals", lang, s -> s.showAquaticPageForTest());
        menu(1, "menu_reef_animals", lang, s -> s.showTankmateForTest("ocellaris_clownfish"));
        menu(2, "menu_breeding", lang, null);
        // 5. items
        then(10, this::openCreativeTab);
        then(10, this::hoverSalt);
        then(20, () -> shot("items"));
        then(1, () -> language("de_de"));
        then(5, this::hoverSalt);
        then(20, () -> shot("items_de"));
        then(1, () -> language(lang));
        then(1, () -> mc.setScreen(null));
        then(1, () -> mc.options.hideGui = true);
    }

    private void menu(int tab, String name, String lang, java.util.function.Consumer<org.overengineer.bettacraft.client.screen.TankScreen> page) {
        then(10, () -> openMenu(reef));
        then(30, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) {
                ts.selectTabForTest(tab);
                if (page != null) page.accept(ts);
            } else {
                LOGGER.error("[smoketest] M20 no tank menu for {}", name);
            }
        });
        then(20, () -> shot(name));
        then(1, () -> language("de_de"));
        then(20, () -> shot(name + "_de"));
        then(1, () -> language(lang));
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.setScreen(null));
    }

    // ------------------------------------------------------------------ scene

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        reach(p, false);
        level.setDayTime(6000);
        base = p.blockPosition().offset(0, 0, -10);
        reef = base;
        TankBlockEntity c = tank(level, p, reef, 5, 3, 3, 26F, 35F);
        InteriorRegion r = region(level, c, Blocks.SAND.defaultBlockState());
        reefRegion = r;
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        // the rock work: a ridge of reef rock along the back, live rock islands in front
        rock(in, r, 40, 36, 11, 10, false);
        rock(in, r, 16, 36, 9, 7, true);
        rock(in, r, 66, 34, 9, 8, true);
        rock(in, r, 22, 16, 5, 4, true);
        rock(in, r, 58, 16, 6, 5, false);
        // corals on top of the rocks (full grown), the anemone on the front island
        long seed = 2020;
        coral(in, r, PlantSpecies.ACROPORA, 40, 34, seed++);
        coral(in, r, PlantSpecies.HAMMER_CORAL, 30, 32, seed++);
        coral(in, r, PlantSpecies.LOBOPHYLLIA, 50, 33, seed++);
        coral(in, r, PlantSpecies.LEATHER_CORAL, 16, 36, seed++);
        coral(in, r, PlantSpecies.GREEN_STAR_POLYPS, 66, 34, seed++);
        coral(in, r, PlantSpecies.ZOANTHUS, 58, 14, seed++);
        coral(in, r, PlantSpecies.MUSHROOM_CORAL, 62, 40, seed++);
        coral(in, r, PlantSpecies.BUBBLE_TIP_ANEMONE, 22, 14, seed);
        // equipment: actinic + white lights, a skimmer at the west side, a flow pump in the interior
        light(level, reef, 5, 3, 1);
        level.setBlockAndUpdate(reef.offset(-1, 2, 1), ModBlocks.PROTEIN_SKIMMER.get().defaultBlockState().setValue(ProteinSkimmerBlock.FACING, Direction.EAST));
        BlockPos pumpAt = r.toWorld(76, 20, 44);
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
            BlockState s = ModBlocks.PUMP_SEGMENT.get().defaultBlockState().setValue(SegmentBlock.FACING, Direction.WEST)
                    .setValue(SegmentBlock.PART, y == 1 ? SegmentBlock.Part.TOP : x == 0 && z == 0 ? SegmentBlock.Part.ANCHOR : SegmentBlock.Part.BASE);
            in.setBlock(pumpAt.offset(x, y, z), s, 3);
        }
        // the animals
        fish(level, c, TankmateSpecies.OCELLARIS_CLOWNFISH, 2, null);
        fish(level, c, TankmateSpecies.GREEN_CHROMIS, 6, null);
        fish(level, c, TankmateSpecies.YELLOW_TANG, 1, null);
        fish(level, c, TankmateSpecies.YELLOW_WATCHMAN_GOBY, 1, null);
        fish(level, c, TankmateSpecies.YELLOWTAIL_DAMSEL, 1, null);
        for (int i = 0; i < 2; i++) TankCritters.add(level, c, CritterSpecies.CLEANER_SHRIMP, CritterGenes.WILD, i == 0, 10);
        for (int i = 0; i < 4; i++) TankCritters.add(level, c, CritterSpecies.HERMIT_CRAB, CritterGenes.WILD, i % 2 == 0, 10);
        for (int i = 0; i < 3; i++) TankCritters.add(level, c, CritterSpecies.TURBO_SNAIL, CritterGenes.WILD, i % 2 == 0, 10);
        // the clownfish eggs at the foot of the anemone
        var spots = TankmateSpots.anemones(level.getServer(), c.getTank());
        if (spots.isEmpty()) {
            LOGGER.error("[smoketest] M20 no anemone found");
        } else {
            Vec3 foot = TankmateSpots.anemoneFoot(level.getServer(), c.getTank(), spots.getFirst().pos());
            Clutch k = new Clutch(c.getTank().nextClutchId(), TankmateSpecies.OCELLARIS_CLOWNFISH, Clutch.Kind.CAVE, foot.x, foot.y, foot.z, 30,
                    TankmateGenes.wild(TankmateSpecies.OCELLARIS_CLOWNFISH), TankmateGenes.wild(TankmateSpecies.OCELLARIS_CLOWNFISH));
            c.getTank().clutches().add(k);
            c.markTankChanged();
        }
        // fresh, brackish and sea water side by side, the same light
        colors = base.offset(0, 0, -26);
        // seen from the north the first tank is on the right: fresh water right, sea water left; a white wall behind them
        float[] salts = {0F, 12F, 35F};
        for (int i = 0; i < 3; i++) {
            tank(level, p, colors.offset(i * 4, 0, 0), 2, 2, 2, 26F, salts[i]);
            light(level, colors.offset(i * 4, 0, 0), 2, 2, 0, DyeColor.WHITE);
        }
        for (int x = -2; x < 12; x++) for (int y = -1; y < 4; y++) {
            level.setBlockAndUpdate(colors.offset(x, y, 3), Blocks.WHITE_CONCRETE.defaultBlockState());
        }
        for (int x = -2; x < 12; x++) for (int z = 0; z < 3; z++) level.setBlockAndUpdate(colors.offset(x, -1, z), Blocks.SMOOTH_QUARTZ.defaultBlockState());
        // Acropora: bleached (fresh water), pale, healthy (white light), glowing (actinic)
        corals = base.offset(0, 0, -14);
        int[] tints = {0, 1, 2, 3};
        for (int i = 0; i < 4; i++) {
            // seen from the north: bleached on the left ... glowing on the right
            TankBlockEntity t = tank(level, p, corals.offset((3 - i) * 3, 0, 0), 2, 2, 2, 26F, 35F);
            InteriorRegion cr = region(level, t, Blocks.SAND.defaultBlockState());
            rock(in, cr, 16, 16, 7, 6, false);
            InteriorPlant a = coral(in, cr, PlantSpecies.ACROPORA, 16, 16, 77L + i);
            if (a != null) {
                a.tint = tints[i];
                a.redness = tints[i] / 3.0;
                InteriorPlants.apply(in, cr, a);
            }
            light(level, corals.offset((3 - i) * 3, 0, 0), 2, 2, 0, i == 3 ? DyeColor.BLUE : DyeColor.WHITE);
        }
    }

    /** Fish freeze around the scene; the clownfish in their anemone. */
    private void pose(ServerPlayer p) {
        ServerLevel level = p.level();
        TankBlockEntity c = TankMultiblock.getController(level, reef);
        if (c == null) return;
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        var spots = TankmateSpots.anemones(level.getServer(), c.getTank());
        int i = 0, chromis = 0;
        for (TankmateEntity m : TankmateBreeding.tankmates(level, c.getTank())) {
            double x, y, z;
            float yaw = i % 2 == 0 ? -90F : 90F;
            int flags = 0;
            switch (m.species()) {
                case OCELLARIS_CLOWNFISH -> {
                    Vec3 a = spots.isEmpty() ? new Vec3(w.box().minX + 1.3, w.box().minY + 0.8, w.box().minZ + 0.6) : spots.getFirst().pos();
                    x = a.x + (m.isFemale() ? -0.22 : 0.18);
                    y = a.y + (m.isFemale() ? 0.16 : 0.06);
                    z = a.z - 0.15;
                    yaw = m.isFemale() ? -70F : 110F;
                }
                case GREEN_CHROMIS -> {
                    x = w.box().minX + 1.6 + chromis * 0.32;
                    y = w.box().minY + 1.9 + (chromis % 2) * 0.18;
                    z = w.box().minZ + 0.7 + (chromis % 3) * 0.2;
                    chromis++;
                }
                case YELLOW_TANG -> {
                    x = w.box().minX + 3.3;
                    y = w.box().minY + 1.3;
                    z = w.box().minZ + 0.55;
                    yaw = 90F;
                }
                case YELLOW_WATCHMAN_GOBY -> {
                    x = w.box().minX + 2.4;
                    y = w.box().minY + 0.02;
                    z = w.box().minZ + 0.35;
                    flags = TankmateEntity.FLAG_REST;
                }
                default -> {
                    x = w.box().minX + 4.2;
                    y = w.box().minY + 0.7;
                    z = w.box().minZ + 0.5;
                }
            }
            m.snapTo(x, y, z, yaw, 0F);
            SmokeFreeze.freeze(m, true);
            m.setDeltaMovement(Vec3.ZERO);
            m.setYRot(yaw);
            m.yBodyRot = yaw;
            m.yHeadRot = yaw;
            m.setPoseForSmoke(flags);
            i++;
        }
        for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) {
            if (e instanceof Mob mob && !(e instanceof TankmateEntity)) SmokeFreeze.freeze(mob, true);
        }
    }

    private void check() {
        int reefFish = 0;
        for (Entity e : mc.level.entitiesForRendering()) if (e instanceof TankmateEntity m && m.species().marine()) reefFish++;
        LOGGER.info("[smoketest] M20 client sees {} reef fish", reefFish);
        if (reefFish < 10) LOGGER.error("[smoketest] M20 the client sees too few reef fish: {}", reefFish);
        TankBlockEntity c = TankMultiblock.getController(mc.level, reef);
        if (c == null || c.getTank() == null) {
            LOGGER.error("[smoketest] M20 the client has no reef tank");
            return;
        }
        LOGGER.info("[smoketest] M20 client: salinity {}, clutches {}, critters {}", c.getTank().salinity(), c.getTank().clutches().size(),
                c.getTank().critters().size());
        if (c.getTank().salinity() < 30) LOGGER.error("[smoketest] M20 the client does not know the salt: {}", c.getTank().salinity());
        if (c.getTank().clutches().isEmpty()) LOGGER.error("[smoketest] M20 the client sees no clownfish eggs");
        if (c.getTank().critters().size() < 9) LOGGER.error("[smoketest] M20 the client sees too few reef critters");
    }

    private void checkColors() {
        int[] salts = new int[3];
        for (int i = 0; i < 3; i++) {
            TankBlockEntity c = TankMultiblock.getController(mc.level, colors.offset(i * 4, 0, 0));
            BlockPos ctrl = c == null ? null : c.getBlockPos();
            salts[i] = ctrl == null ? -1 : org.overengineer.bettacraft.client.tank.TankLightTint.saltOf(ctrl);
        }
        LOGGER.info("[smoketest] M20 water drawn with salinity {} / {} / {}", salts[0], salts[1], salts[2]);
        if (salts[0] != 0 || salts[1] < 10 || salts[2] < 30) LOGGER.error("[smoketest] M20 wrong water colours: {} / {} / {}", salts[0], salts[1], salts[2]);
    }

    // ------------------------------------------------------------------ helpers

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz, float temp, float salt) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, (long) sx * sy * sz * 1000, salt);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(p.getUUID());
        c.getTank().setWaterTemp(temp);
        c.getTank().setTargetTemp(temp);
        c.markTankChanged();
        return c;
    }

    private static InteriorRegion region(ServerLevel level, TankBlockEntity c, BlockState floor) {
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++)
            for (int z = 0; z < r.sizeZ(); z++) Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), floor, false);
        return r;
    }

    /** A rounded pile of reef rock (or live rock) with a little live rock mixed in. */
    private static void rock(ServerLevel in, InteriorRegion r, int cx, int cz, int rad, int h, boolean live) {
        for (int x = -rad; x <= rad; x++)
            for (int z = -rad; z <= rad; z++) {
                double d = Math.sqrt(x * x + z * z) / rad;
                // a rough outline and surface (not a smooth dome)
                double noise = Math.sin(x * 1.7 + z * 0.9) * 0.18 + Math.sin(x * 0.6 - z * 1.3) * 0.15;
                if (d > 1.0 + noise * 0.5) continue;
                int top = (int) Math.round(h * Math.max(0.15, 1 - d * d * 0.8 + noise));
                for (int y = 0; y < top; y++) {
                    boolean l = live ? (x * 3 + y + z * 5) % 4 != 0 : (x + y * 2 + z) % 5 == 0;
                    in.setBlock(r.toWorld(cx + x, y, cz + z), (l ? ModBlocks.LIVE_ROCK : ModBlocks.REEF_ROCK).get().defaultBlockState(), 2);
                }
            }
    }

    private static int topAt(ServerLevel in, InteriorRegion r, int x, int z) {
        int y = 0;
        while (y < r.sizeY() && !in.getBlockState(r.toWorld(x, y, z)).isAir()) y++;
        return y;
    }

    private static InteriorPlant coral(ServerLevel in, InteriorRegion r, PlantSpecies s, int x, int z, long seed) {
        InteriorPlant p = InteriorPlants.plantAt(in, r, s, x, topAt(in, r, x, z), z, s.stages(), seed);
        if (p == null) {
            LOGGER.error("[smoketest] M20 {} not placed", s.id());
            return null;
        }
        p.tint = 3;
        p.redness = 1;
        InteriorPlants.apply(in, r, p);
        return p;
    }

    private static void light(ServerLevel level, BlockPos min, int sx, int y, int z) {
        for (int x = 0; x < sx; x++) {
            level.setBlockAndUpdate(min.offset(x, y, z), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < sx - 1).setValue(TankLightBlock.LIT, true)
                    .setValue(TankLightBlock.COLOR, x % 2 == 0 ? DyeColor.BLUE : DyeColor.WHITE));
        }
    }

    private static void light(ServerLevel level, BlockPos min, int sx, int y, int z, DyeColor color) {
        for (int x = 0; x < sx; x++) {
            level.setBlockAndUpdate(min.offset(x, y, z), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < sx - 1).setValue(TankLightBlock.LIT, true)
                    .setValue(TankLightBlock.COLOR, color));
        }
    }

    private static void fish(ServerLevel level, TankBlockEntity c, TankmateSpecies s, int n, String variety) {
        for (int i = 0; i < n; i++) {
            TankWaterVolume w = new TankWaterVolume(c.getTank());
            var r = level.getRandom();
            Vec3 p = w.clamp(new Vec3(w.box().minX + r.nextDouble() * (w.box().maxX - w.box().minX), w.box().minY + 0.3 + r.nextDouble() * 0.5,
                    w.box().minZ + r.nextDouble() * (w.box().maxZ - w.box().minZ)), 0.35, 0.1, 0.2);
            TankmateGenes g = variety == null ? TankmateGenes.wild(s) : TankmateGenes.pure(s, variety);
            TankmateEntity e = TankmateBreeding.spawn(level, c, s, g, i % 2 == 0, LifeStage.ADULT, 0, p);
            if (e == null) LOGGER.error("[smoketest] M20 {} not spawned", s.id());
            else e.health().feed();
        }
    }

    private void openMenu(BlockPos at) {
        server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.level(), at);
            if (c == null) return;
            org.overengineer.bettacraft.platform.Platform.get().openMenu(p, new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        });
    }

    private static void reach(ServerPlayer p, boolean normal) {
        var b = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE);
        var e = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE);
        if (b != null) b.setBaseValue(normal ? 4.5 : 0.0);
        if (e != null) e.setBaseValue(normal ? 3.0 : 0.0);
    }

    /** The creative inventory on the Bettacraft tab, scrolled to the reef items. */
    private void openCreativeTab() {
        var scr = new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true);
        mc.setScreen(scr);
        try {
            var m = net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", net.minecraft.world.item.CreativeModeTab.class);
            m.setAccessible(true);
            m.invoke(scr, org.overengineer.bettacraft.registry.ModCreativeTabs.MAIN.get());
            var menu = (net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.ItemPickerMenu) scr.getMenu();
            int idx = 0;
            while (idx < menu.items.size() && !menu.items.get(idx).is(ModItems.SEA_SALT.get())) idx++;
            int rows = Math.max(1, (menu.items.size() + 8) / 9 - 5);
            float pos = Math.min(1F, (idx / 9) / (float) rows);
            menu.scrollTo(pos);
            var f = net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.class.getDeclaredField("scrollOffs");
            f.setAccessible(true);
            f.setFloat(scr, pos);
        } catch (ReflectiveOperationException ex) {
            LOGGER.warn("[smoketest] M20 cannot select the creative tab: {}", ex.toString());
        }
    }

    private void hoverSalt() {
        if (!(mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> scr)) return;
        for (var s : scr.getMenu().slots) {
            ItemStack st = s.getItem();
            if (st.is(ModItems.SEA_SALT.get())) {
                mouseTo(scr.getGuiLeft() + s.x + 8, scr.getGuiTop() + s.y + 8);
                return;
            }
        }
        LOGGER.warn("[smoketest] M20 no sea salt visible in the creative tab");
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

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.level(), x, eyeY - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, false));
    }

    private void language(String code) {
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
        if (mc.screen != null) mc.screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private void mouseTo(int x, int y) {
        double scale = mc.getWindow().getGuiScale();
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), x * scale, y * scale);
        try {
            for (String f : new String[]{"xpos", "ypos"}) {
                java.lang.reflect.Field fld = net.minecraft.client.MouseHandler.class.getDeclaredField(f);
                fld.setAccessible(true);
                fld.setDouble(mc.mouseHandler, f.equals("xpos") ? x * scale : y * scale);
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("[smoketest] cannot move the mouse: {}", e.toString());
        }
    }

    private void shot(String name) {
        mc.getToasts().clear();
        if (mc.player != null) LOGGER.info("[smoketest] M20 shot {} from {}", name, mc.player.getEyePosition());
        Screenshot.grab(mc.gameDirectory, "bettacraft_m20_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] " + c.getString()));
    }
}
