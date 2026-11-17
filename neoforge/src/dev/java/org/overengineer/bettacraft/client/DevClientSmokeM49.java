package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.critters.AmanoLarvae;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.decor.DecorItem;
import org.overengineer.bettacraft.decor.DecorType;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.Sump;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.Mangroves;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.reef.CalciumReactorBlock;
import org.overengineer.bettacraft.reef.CalciumReactorBlockEntity;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tankmates.Archerfish;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.terrarium.HerpEntity;
import org.overengineer.bettacraft.terrarium.HerpSpecies;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M49 dev client smoke scene ({@code -Psmoketestm49}), screenshots {@code bettacraft_m49_*}: part a a reef tank on cabinets
 * with a sump (heater, skimmer, media, a lit refugium), a calcium reactor with its CO2 cylinder and a level sensor beside the
 * cabinet; the cabinet menu with the sump slot, the sump window, the reactor window and the tab "Sump" of the tank menu (en and
 * de). Part b a brackish paludarium: mud land, a grown mangrove over the water, the stilt root decoration, archerfish (one
 * shooting), mudskippers on the land, Amano shrimp and a cloud of their larvae; close-ups. Checks (errors in the log): the
 * client sees the animals, the sump counts, the mangrove has its crown.
 */
final class DevClientSmokeM49 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private BlockPos reef, pal;
    private int wait;

    DevClientSmokeM49() {
        then(1, () -> server(p -> p.teleportTo(p.serverLevel(), p.getX(), p.getY() + 2, p.getZ() + 1100, 0F, 10F)));
        then(20, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(120, () -> server(this::pose));
        // part a: the reef tank on its cabinets with the sump and the calcium reactor
        then(1, () -> look(reef.getX() + 2.4, reef.getY() + 0.7, reef.getZ() - 4.6, 0F, 12F));
        then(60, () -> shot("sump"));
        then(1, () -> look(reef.getX() + 3.4, reef.getY() - 0.1, reef.getZ() - 1.9, 0F, 18F));
        then(40, () -> shot("calcium_reactor"));
        // part b: the brackish paludarium
        then(1, () -> look(pal.getX() + 2.5, pal.getY() + 1.5, pal.getZ() - 5.0, 0F, 8F));
        then(80, () -> {
            shot("brackish");
            int archers = 0, skippers = 0;
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e instanceof TankmateEntity m && m.species() == TankmateSpecies.ARCHERFISH) archers++;
                if (e instanceof HerpEntity h && h.species() == HerpSpecies.MUDSKIPPER) skippers++;
            }
            LOGGER.info("[smoketest] M49 client sees {} archerfish, {} mudskippers", archers, skippers);
            if (archers < 3 || skippers < 2) LOGGER.error("[smoketest] M49 the client sees too few brackish animals: {} archerfish, {} mudskippers", archers, skippers);
        });
        // the archerfish shoots at the leaves of the mangrove
        then(1, () -> look(pal.getX() + 2.0, pal.getY() + 1.75, pal.getZ() - 1.6, 0F, 8F));
        for (int i = 0; i < 8; i++) then(2, () -> server(this::shoot));
        then(1, () -> shot("archer_shot"));
        then(20, () -> look(pal.getX() + 2.6, pal.getY() + 1.55, pal.getZ() - 1.0, 0F, 4F));
        then(40, () -> shot("sp_archerfish"));
        then(1, () -> server(p -> {
            // close to the first mudskipper, from the front glass, looking down at it
            TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), pal);
            if (c == null) return;
            for (Entity e : TankInhabitants.liveInhabitants(p.serverLevel(), c.getTank())) {
                if (e instanceof HerpEntity h && h.species() == HerpSpecies.MUDSKIPPER) {
                    p.teleportTo(p.serverLevel(), h.getX() + 0.05, h.getY() + 0.42 - p.getEyeHeight(), pal.getZ() - 0.25, 0F, 26F);
                    return;
                }
            }
            LOGGER.error("[smoketest] M49 no mudskipper for the close-up");
        }));
        then(40, () -> shot("sp_mudskipper"));
        then(1, () -> look(pal.getX() + 2.2, pal.getY() + 2.6, pal.getZ() - 1.8, 0F, 22F));
        then(40, () -> shot("mangrove"));
        then(1, () -> look(pal.getX() + 1.3, pal.getY() + 0.5, pal.getZ() - 0.8, 0F, 20F));
        then(40, () -> shot("sp_amano_shrimp"));
        // the menus
        then(10, () -> mc.options.hideGui = false);
        then(1, () -> server(p -> {
            reach(p, true);
            p.teleportTo(p.serverLevel(), reef.getX() + 1.5, reef.getY() + 0.5, reef.getZ() - 2.5, 0F, 0F);
        }));
        String lang = mc.options.languageCode;
        screen(p -> {
            if (p.serverLevel().getBlockEntity(reef.offset(2, -1, 0)) instanceof CabinetBlockEntity cab) org.overengineer.bettacraft.platform.Platform.get().openMenu(p, cab, cab.getBlockPos());
        }, "menu_cabinet", lang);
        screen(p -> {
            if (p.serverLevel().getBlockEntity(reef.offset(2, -1, 0)) instanceof CabinetBlockEntity cab) {
                BlockPos pos = cab.getBlockPos();
                org.overengineer.bettacraft.platform.Platform.get().openMenu(p, new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.equipment.SumpMenu(id, inv, cab),
                        Component.translatable("gui.bettacraft.sump.title")), b -> b.writeBlockPos(pos));
            }
        }, "menu_sump", lang);
        screen(p -> {
            if (p.serverLevel().getBlockEntity(reef.offset(3, -1, 0)) instanceof CalciumReactorBlockEntity r) org.overengineer.bettacraft.platform.Platform.get().openMenu(p, r, r.getBlockPos());
        }, "menu_reactor", lang);
        menu(() -> reef, 9, "menu_sump_tab", lang);
        menu(() -> pal, 0, "menu_paludarium", lang);
        then(1, () -> mc.options.hideGui = true);
    }

    // ------------------------------------------------------------------ scene

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        reach(p, false);
        level.setDayTime(6000);
        BlockPos base = p.blockPosition().offset(0, 0, -10);
        // part a: a 3x2x2 reef tank on a row of cabinets, the sump in the middle one
        reef = base.offset(0, 1, 0);
        for (int x = 0; x < 3; x++) {
            level.setBlockAndUpdate(reef.offset(x, -1, 0), ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
            level.setBlockAndUpdate(reef.offset(x, -1, 1), Blocks.SMOOTH_STONE.defaultBlockState());
        }
        TankBlockEntity r = tank(level, p, reef, 3, 2, 2, 26F, 35F);
        InteriorRegion rr = region(level, r, Blocks.SAND.defaultBlockState());
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        for (int x = 0; x < rr.sizeX(); x++) for (int z = 20; z < rr.sizeZ(); z++) {
            int h = 2 + (int) Math.round(3 + 2 * Math.sin(x * 0.3) + Math.sin(z * 0.5));
            for (int y = 0; y < h; y++) in.setBlock(rr.toWorld(x, y, z), ModBlocks.LIVE_ROCK.get().defaultBlockState(), 2);
        }
        coral(in, rr, PlantSpecies.ACROPORA, 12, 26, 4901L);
        coral(in, rr, PlantSpecies.HAMMER_CORAL, 34, 24, 4902L);
        light(level, reef, 3, 2, 1, null);
        if (level.getBlockEntity(reef.offset(2, -1, 0)) instanceof CabinetBlockEntity cab) {
            ItemStack sump = new ItemStack(ModItems.SUMP.get());
            NonNullList<ItemStack> ch = NonNullList.withSize(Sump.CHAMBERS, ItemStack.EMPTY);
            ch.set(Sump.HEATER0, new ItemStack(ModItems.HEATER_MEDIUM.get()));
            ch.set(Sump.SKIMMER, new ItemStack(ModItems.PROTEIN_SKIMMER.get()));
            ch.set(Sump.MEDIA0, new ItemStack(ModItems.FILTER_FOAM.get()));
            ch.set(Sump.MEDIA0 + 1, new ItemStack(ModItems.CERAMIC_RINGS.get()));
            ch.set(Sump.ALGAE, new ItemStack(ModItems.CHAETOMORPHA.get(), 10));
            ch.set(Sump.LIGHT, new ItemStack(ModItems.LED_PANEL.get()));
            Sump.write(sump, ch);
            cab.setSumpItem(sump);
        } else LOGGER.error("[smoketest] M49 no cabinet");
        // the calcium reactor beside the cabinet of the sump (it reaches the tank through the sump), its CO2 cylinder next to it
        level.setBlockAndUpdate(reef.offset(3, -1, 0), ModBlocks.CALCIUM_REACTOR.get().defaultBlockState().setValue(CalciumReactorBlock.FACING, Direction.NORTH));
        level.setBlockAndUpdate(reef.offset(4, -1, 0), ModBlocks.CO2_CYLINDER.get().defaultBlockState());
        if (level.getBlockEntity(reef.offset(4, -1, 0)) instanceof org.overengineer.bettacraft.co2.Co2CylinderBlockEntity cyl) cyl.setGas(500F);
        if (level.getBlockEntity(reef.offset(3, -1, 0)) instanceof CalciumReactorBlockEntity cr) {
            cr.items().setStackInSlot(0, new ItemStack(ModItems.CORAL_RUBBLE.get(), 40));
            r.getTank().setCalcium(380F);
            cr.step(0.01);
            if (cr.status() != CalciumReactorBlockEntity.RUNNING) LOGGER.error("[smoketest] M49 the calcium reactor does not run: {}", cr.status());
        }
        fish(level, r, TankmateSpecies.OCELLARIS_CLOWNFISH, "wild", false);
        fish(level, r, TankmateSpecies.YELLOW_TANG, "wild", false);
        TankEquipment.invalidate(r);
        Sump.step(level, r, 0.001);
        LOGGER.info("[smoketest] M49 sump blocks {} system {}", r.getTank().sumpBlocks(), r.getTank().systemBlocks());
        if (r.getTank().sumpBlocks() <= 0) LOGGER.error("[smoketest] M49 the sump does not count");
        // part b: a brackish paludarium 5x3x3: mud land on the left (seen from the north), water on the right half
        pal = base.offset(8, 0, 0);
        TankBlockEntity c = tank(level, p, pal, 5, 3, 3, 27F, 12F);
        c.getTank().setWaterMb(5 * 3 * 3 * 1000L * 45 / 100);
        InteriorRegion pr = region(level, c, Blocks.MUD.defaultBlockState());
        for (int x = 46; x < pr.sizeX(); x++) for (int z = 0; z < pr.sizeZ(); z++) {
            int top = 14 + (x - 46) / 2 + (int) Math.round(1.5 * Math.sin(z * 0.35)); // a mud beach rising out of the water
            for (int y = 0; y < top; y++) in.setBlock(pr.toWorld(x, y, z), y >= top - 2 ? Blocks.MUD.defaultBlockState() : Blocks.PACKED_MUD.defaultBlockState(), 2);
        }
        DecorItem.place(in, DecorType.MANGROVE_STILT_ROOTS, pr.toWorld(40, 0, 30), Rotation.NONE, null);
        light(level, pal, 5, 3, 2, DyeColor.WHITE);
        TankClimate.step(level, c, 0.01);
        c.getTank().setClimate(new org.overengineer.bettacraft.terrarium.Climate.State(28, 28, 28, 90, 0, 0));
        String err = Mangroves.plant(level, c, 24, 22);
        if (err != null) LOGGER.error("[smoketest] M49 mangrove not planted: {}", err);
        else {
            Mangroves.Tree m = c.getTank().mangroves().getFirst();
            Mangroves.grow(level, c, pr, in, m, Mangroves.STAGES);
            if (Mangroves.crowns(c.getTank()) == 0) LOGGER.error("[smoketest] M49 the mangrove has no crown");
        }
        TankClimate.step(level, c, 0.01);
        LOGGER.info("[smoketest] M49 paludarium mode {} land {}", c.getTank().terrariumMode(), c.getTank().landShare());
        for (int i = 0; i < 3; i++) fish(level, c, TankmateSpecies.ARCHERFISH, "wild", i == 1);
        for (int i = 0; i < 2; i++) herp(level, c, HerpSpecies.MUDSKIPPER);
        for (int i = 0; i < 4; i++) TankCritters.add(level, c, CritterSpecies.AMANO_SHRIMP, CritterGenes.WILD, i % 2 == 0, 20);
        Critter mother = c.getTank().critters().getLast();
        var cloud = AmanoLarvae.hatch(mother, c.getTank().nextCritterId(), org.overengineer.bettacraft.critters.NetCatching.random(level));
        cloud.x = pal.getX() + 1.6;
        cloud.y = pal.getY() + 0.45;
        cloud.z = pal.getZ() + 0.35;
        c.getTank().critterObjects().add(cloud);
        c.markTankChanged();
        TankCritters.sync(level, c);
    }

    private void pose(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        TankBlockEntity r = TankMultiblock.getController(level, reef);
        if (r != null) {
            var b = new TankWaterVolume(r.getTank()).box();
            int i = 0;
            for (TankmateEntity m : TankmateBreeding.tankmates(level, r.getTank())) freezeAt(m, new Vec3(b.maxX - 0.9 - i++ * 1.1, b.minY + 1.0, b.minZ + 0.6), -90F);
        }
        TankBlockEntity c = TankMultiblock.getController(level, pal);
        if (c == null) return;
        var b = new TankWaterVolume(c.getTank()).box();
        int i = 0;
        for (TankmateEntity m : TankmateBreeding.tankmates(level, c.getTank())) {
            if (m.species() != TankmateSpecies.ARCHERFISH) continue;
            freezeAt(m, new Vec3(pal.getX() + 3.1 - i * 0.9, c.getTank().waterSurfaceY() - 0.22, pal.getZ() + 0.55 + i * 0.35), -90F);
            i++;
        }
        int j = 0;
        for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) {
            if (!(e instanceof HerpEntity h) || h.species() != HerpSpecies.MUDSKIPPER) continue;
            double x = pal.getX() + 3.9 + j * 0.45, z = pal.getZ() + 0.45 + j * 0.45; // on the mud (seen from the north: on the left)
            double y = groundY(level, c, x, z);
            h.moveTo(x, y, z, -70F + j * 40F, 0F);
            SmokeFreeze.freeze(h, false);
            h.setYRot(-70F + j * 40F);
            h.yBodyRot = h.getYRot();
            h.yHeadRot = h.getYRot();
            j++;
        }
        int k = 0;
        for (Critter s : c.getTank().critters()) {
            if (s.species != CritterSpecies.AMANO_SHRIMP) continue;
            s.x = pal.getX() + 1.0 + k * 0.22;
            s.z = pal.getZ() + 0.35 + (k % 2) * 0.15;
            s.y = pal.getY() + 0.02;
            s.yaw = 90;
            k++;
        }
        c.markTankChanged();
        TankCritters.sync(level, c);
    }

    /** The ground of the paludarium under (x, z): the top of the interior column. */
    private static double groundY(ServerLevel level, TankBlockEntity c, double x, double z) {
        var terrain = new org.overengineer.bettacraft.critters.TankTerrain(c.getTank(), level.getServer(),
                org.overengineer.bettacraft.tank.interior.InteriorRegions.get(level.getServer()).get(c.getTank().regionId()), true);
        double g = terrain.ground(x, z);
        return Double.isFinite(g) ? g + 0.01 : pal_y(c);
    }

    private static double pal_y(TankBlockEntity c) {
        return c.getTank().min().getY() + 1.5;
    }

    private void shoot(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        TankBlockEntity c = TankMultiblock.getController(level, pal);
        if (c == null) return;
        // the archerfish next to the mangrove shoots at its crown
        Vec3 crown = null;
        if (!c.getTank().mangroves().isEmpty()) {
            var t = c.getTank().mangroves().getFirst();
            crown = new Vec3(pal.getX() + (t.x + 0.5) / 16.0, c.getTank().waterSurfaceY() + 0.3, pal.getZ() + (t.z + 0.5) / 16.0);
        }
        TankmateEntity best = null;
        for (TankmateEntity m : TankmateBreeding.tankmates(level, c.getTank())) {
            if (m.species() != TankmateSpecies.ARCHERFISH) continue;
            if (best == null || crown != null && m.position().distanceToSqr(crown) < best.position().distanceToSqr(crown)) best = m;
        }
        if (best != null) {
            c.getTank().setFlies(c.getTank().flies() + 1);
            Archerfish.shoot(level, c, best, crown != null ? crown.add(level.random.nextDouble() * 0.3 - 0.15, 0, -0.2) : best.position().add(0, 1.2, 0));
            return;
        }
        LOGGER.error("[smoketest] M49 no archerfish to shoot");
    }

    private static void freezeAt(TankmateEntity m, Vec3 a, float yaw) {
        m.moveTo(a.x, a.y, a.z, yaw, 0F);
        SmokeFreeze.freeze(m, true);
        m.setDeltaMovement(Vec3.ZERO);
        m.setYRot(yaw);
        m.yBodyRot = yaw;
        m.yHeadRot = yaw;
    }

    private static void herp(ServerLevel level, TankBlockEntity c, HerpSpecies s) {
        HerpEntity e = ModEntities.herp(s).create(level);
        if (e == null) return;
        ItemStack box = e.getBucketItemStack();
        e.saveToBucketTag(box);
        e.discard();
        var err = TankInhabitants.insert(level, c, box, ModEntities.herp(s), null, 0L);
        if (err != null) LOGGER.error("[smoketest] M49 {} not in: {}", s.id(), err.getString());
    }

    // ------------------------------------------------------------------ helpers

    private void screen(java.util.function.Consumer<ServerPlayer> open, String name, String lang) {
        if (name == null) return;
        then(10, () -> server(open));
        then(30, () -> {
            if (mc.screen == null) LOGGER.error("[smoketest] M49 no window for {}", name);
        });
        then(10, () -> shot(name));
        then(1, () -> language("de_de"));
        then(20, () -> shot(name + "_de"));
        then(1, () -> language(lang));
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.setScreen(null));
    }

    private void menu(java.util.function.Supplier<BlockPos> tank, int tab, String name, String lang) {
        then(10, () -> server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), tank.get());
            if (c == null) return;
            org.overengineer.bettacraft.platform.Platform.get().openMenu(p, new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        }));
        then(30, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.selectTabForTest(tab);
            else LOGGER.error("[smoketest] M49 no tank menu for {}", name);
        });
        then(20, () -> shot(name));
        then(1, () -> language("de_de"));
        then(20, () -> shot(name + "_de"));
        then(1, () -> language(lang));
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.setScreen(null));
    }

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
        c.getTank().setWaterQuality(1F);
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

    private static void coral(ServerLevel in, InteriorRegion r, PlantSpecies s, int x, int z, long seed) {
        int y = 0;
        while (y < r.sizeY() && !in.getBlockState(r.toWorld(x, y, z)).isAir()) y++;
        InteriorPlant p = InteriorPlants.plantAt(in, r, s, x, y, z, s.stages(), seed);
        if (p == null) {
            LOGGER.error("[smoketest] M49 {} not placed", s.id());
            return;
        }
        p.tint = 3;
        p.redness = 1;
        InteriorPlants.apply(in, r, p);
    }

    /** A row of lights over the back of the tank; {@code color} null: actinic and white alternating (reef). */
    private static void light(ServerLevel level, BlockPos min, int sx, int y, int z, DyeColor color) {
        for (int x = 0; x < sx; x++) {
            level.setBlockAndUpdate(min.offset(x, y, z), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < sx - 1).setValue(TankLightBlock.LIT, true)
                    .setValue(TankLightBlock.COLOR, color != null ? color : x % 2 == 0 ? DyeColor.BLUE : DyeColor.WHITE));
        }
    }

    private static void fish(ServerLevel level, TankBlockEntity c, TankmateSpecies s, String variety, boolean female) {
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        Vec3 p = w.clamp(new Vec3(w.box().minX + 0.5, w.box().minY + 0.5, w.box().minZ + 0.5), 0.3, 0.1, 0.3);
        TankmateEntity e = TankmateBreeding.spawn(level, c, s, TankmateGenes.pure(s, variety), female, LifeStage.ADULT, 0, p);
        if (e == null) LOGGER.error("[smoketest] M49 {} {} not spawned", s.id(), variety);
        else e.health().feed();
    }

    private static void reach(ServerPlayer p, boolean normal) {
        var b = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE);
        var e = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE);
        if (b != null) b.setBaseValue(normal ? 4.5 : 0.0);
        if (e != null) e.setBaseValue(normal ? 3.0 : 0.0);
    }

    private void language(String code) {
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
        if (mc.screen != null) mc.screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.serverLevel(), x, eyeY - p.getEyeHeight(), z, yaw, pitch));
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
        if (mc.player != null) LOGGER.info("[smoketest] M49 shot {} from {}", name, mc.player.getEyePosition());
        Screenshot.grab(mc.gameDirectory, "bettacraft_m49_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
