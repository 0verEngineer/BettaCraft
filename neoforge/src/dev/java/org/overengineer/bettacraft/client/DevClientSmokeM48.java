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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.reef.ClownLarvae;
import org.overengineer.bettacraft.reef.Copepods;
import org.overengineer.bettacraft.tank.TankBlockEntity;
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
 * M48 dev client smoke scene ({@code -Psmoketestm48}), screenshots {@code bettacraft_m48_*}: part a a tank with every new tetra
 * and cory form, the neon forms, the panda cory forms (the longfin with its long fins) and the bronze cory forms side by side,
 * close-ups of the bronze cory and the panda garra; part b a reef tank with live rock, a mandarin, a Banggai pair, the anthias
 * (male and female) and fire shrimp, close-ups, and a clownfish tank with larvae, rotifers and phytoplankton and its menu (the
 * reef line with copepods, rotifers and phytoplankton, en and de). Checks (errors in the log): the client sees the fish, the
 * copepods grow on the rock.
 */
final class DevClientSmokeM48 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private BlockPos forms, neons, pandas, bronzes, garras, reef, larvae;
    private final java.util.Map<String, Vec3> at = new java.util.concurrent.ConcurrentHashMap<>();
    private int wait;

    DevClientSmokeM48() {
        then(1, () -> server(p -> p.teleportTo(p.serverLevel(), p.getX(), p.getY() + 2, p.getZ() + 900, 0F, 10F)));
        then(20, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(100, () -> server(this::pose));
        // part a
        then(1, () -> look(forms.getX() + 3.0, forms.getY() + 1.0, forms.getZ() - 4.6, 0F, 6F));
        then(60, () -> {
            shot("forms");
            int n = 0;
            for (Entity e : mc.level.entitiesForRendering()) if (e instanceof TankmateEntity m && m.species().m48Forms()) n++;
            LOGGER.info("[smoketest] M48 client sees {} tetras and corys of the new forms", n);
            if (n < 20) LOGGER.error("[smoketest] M48 the client sees too few tetras and corys: {}", n);
        });
        then(1, () -> look(neons.getX() + 1.5, neons.getY() + 0.95, neons.getZ() - 0.55, 0F, 2F));
        then(40, () -> shot("neon_forms"));
        then(1, () -> look(pandas.getX() + 1.5, pandas.getY() + 0.75, pandas.getZ() - 0.75, 0F, 3F));
        then(40, () -> shot("panda_cory_forms"));
        then(1, () -> look(bronzes.getX() + 1.5, bronzes.getY() + 0.75, bronzes.getZ() - 0.85, 0F, 3F));
        then(40, () -> shot("bronze_cory_forms"));
        then(1, () -> look(garras.getX() + 1.5, garras.getY() + 0.95, garras.getZ() - 0.55, 0F, 2F));
        then(40, () -> shot("sp_panda_garra"));
        // part b
        then(1, () -> look(reef.getX() + 2.5, reef.getY() + 1.1, reef.getZ() - 4.8, 0F, 8F));
        then(80, () -> {
            shot("reef");
            int n = 0;
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e instanceof TankmateEntity m && (m.species() == TankmateSpecies.MANDARINFISH || m.species() == TankmateSpecies.BANGGAI_CARDINAL
                        || m.species() == TankmateSpecies.LYRETAIL_ANTHIAS)) n++;
            }
            LOGGER.info("[smoketest] M48 client sees {} new reef fish", n);
            if (n < 5) LOGGER.error("[smoketest] M48 the client sees too few reef fish: {}", n);
        });
        for (String k : List.of("mandarinfish", "banggai_cardinal", "lyretail_anthias", "lyretail_anthias_female", "fire_shrimp")) {
            then(1, () -> server(p -> {
                // the animal where it is now (the bottom animals do not stay exactly where they were posed)
                Vec3 a = where(p.serverLevel(), k);
                if (a == null) {
                    LOGGER.error("[smoketest] M48 no {}", k);
                    return;
                }
                boolean low = k.equals("fire_shrimp") || k.equals("mandarinfish");
                double back = k.equals("fire_shrimp") ? 0.75 : k.equals("mandarinfish") ? 0.95 : 1.1;
                // never inside the tank: at most just outside the front glass
                double z = Math.min(a.z - back, reef.getZ() - 0.12);
                double dz = a.z - z;
                p.teleportTo(p.serverLevel(), a.x, a.y + (low ? 0.12 + 0.3 * dz : 0.05) - p.getEyeHeight(), z, 0F, low ? 18F : 2F);
            }));
            then(40, () -> shot("sp_" + k));
        }
        then(1, () -> look(larvae.getX() + 1.0, larvae.getY() + 0.75, larvae.getZ() - 0.7, 0F, 14F));
        then(60, () -> shot("larvae"));
        // the menu: the reef line with copepods, rotifers and phytoplankton
        then(10, () -> mc.options.hideGui = false);
        then(1, () -> server(p -> {
            reach(p, true);
            p.teleportTo(p.serverLevel(), reef.getX() + 2.5, reef.getY() + 0.5, reef.getZ() - 2.5, 0F, 0F);
        }));
        String lang = mc.options.languageCode;
        menu(() -> reef, 5, "menu_reef", lang);
        menu(() -> larvae, 5, "menu_larvae", lang);
        menu(() -> reef, 1, "menu_animals", lang);
        then(1, () -> mc.options.hideGui = true);
    }

    // ------------------------------------------------------------------ scene

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        reach(p, false);
        level.setDayTime(6000);
        BlockPos base = p.blockPosition().offset(0, 0, -10);
        forms = base;
        TankBlockEntity f = tank(level, p, forms, 6, 2, 2, 25F, 0F);
        region(level, f, Blocks.SAND.defaultBlockState());
        light(level, forms, 6, 2, 1, DyeColor.WHITE);
        for (TankmateSpecies s : List.of(TankmateSpecies.NEON_TETRA, TankmateSpecies.CARDINAL_TETRA, TankmateSpecies.GLOWLIGHT_TETRA,
                TankmateSpecies.STERBAI_CORY, TankmateSpecies.PANDA_CORY, TankmateSpecies.BRONZE_CORY, TankmateSpecies.PANDA_GARRA)) {
            for (String v : s.varieties()) fish(level, f, s, v, false);
        }
        neons = base.offset(8, 0, 0);
        TankBlockEntity n = tank(level, p, neons, 3, 2, 2, 25F, 0F);
        region(level, n, Blocks.SAND.defaultBlockState());
        light(level, neons, 3, 2, 1, DyeColor.WHITE);
        for (String v : TankmateSpecies.NEON_TETRA.varieties()) fish(level, n, TankmateSpecies.NEON_TETRA, v, false);
        pandas = base.offset(12, 0, 0);
        TankBlockEntity pc = tank(level, p, pandas, 3, 2, 2, 24F, 0F);
        region(level, pc, Blocks.SAND.defaultBlockState());
        light(level, pandas, 3, 2, 1, DyeColor.WHITE);
        for (String v : TankmateSpecies.PANDA_CORY.varieties()) fish(level, pc, TankmateSpecies.PANDA_CORY, v, false);
        bronzes = base.offset(16, 0, 0);
        TankBlockEntity bc = tank(level, p, bronzes, 3, 2, 2, 24F, 0F);
        region(level, bc, Blocks.SAND.defaultBlockState());
        light(level, bronzes, 3, 2, 1, DyeColor.WHITE);
        for (String v : TankmateSpecies.BRONZE_CORY.varieties()) fish(level, bc, TankmateSpecies.BRONZE_CORY, v, false);
        garras = base.offset(20, 0, 0);
        TankBlockEntity gc = tank(level, p, garras, 3, 2, 2, 23F, 0F);
        region(level, gc, Blocks.SAND.defaultBlockState());
        light(level, garras, 3, 2, 1, DyeColor.WHITE);
        for (int i = 0; i < 2; i++) fish(level, gc, TankmateSpecies.PANDA_GARRA, "wild", i == 1);
        // part b: the reef tank with live rock
        reef = base.offset(0, 2, -8);
        // raised on a stone plinth: the camera can look at the floor of the tank (the fire shrimp)
        for (int x = 0; x < 5; x++) for (int z = 0; z < 3; z++) for (int y = 1; y <= 2; y++) level.setBlockAndUpdate(reef.offset(x, -y, z), Blocks.SMOOTH_STONE.defaultBlockState());
        TankBlockEntity r = tank(level, p, reef, 5, 2, 3, 26F, 35F);
        InteriorRegion rr = region(level, r, Blocks.SAND.defaultBlockState());
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        for (int x = 0; x < rr.sizeX(); x++) for (int z = 32; z < rr.sizeZ(); z++) {
            int h = 2 + (int) Math.round(3 + 2 * Math.sin(x * 0.3) + Math.sin(z * 0.5));
            for (int y = 0; y < h; y++) in.setBlock(rr.toWorld(x, y, z), ModBlocks.LIVE_ROCK.get().defaultBlockState(), 2);
        }
        coral(in, rr, PlantSpecies.GREEN_STAR_POLYPS, 20, 36, 4801L);
        coral(in, rr, PlantSpecies.ZOANTHUS, 56, 38, 4802L);
        light(level, reef, 5, 2, 1, null);
        fish(level, r, TankmateSpecies.MANDARINFISH, "wild", false);
        fish(level, r, TankmateSpecies.BANGGAI_CARDINAL, "wild", false);
        fish(level, r, TankmateSpecies.BANGGAI_CARDINAL, "wild", true);
        fish(level, r, TankmateSpecies.LYRETAIL_ANTHIAS, "wild", false);
        for (int i = 0; i < 4; i++) fish(level, r, TankmateSpecies.LYRETAIL_ANTHIAS, "wild", true);
        for (int i = 0; i < 2; i++) TankCritters.add(level, r, CritterSpecies.FIRE_SHRIMP, CritterGenes.WILD, i == 0, 10);
        for (int i = 0; i < 40; i++) Copepods.step(level, r, 0.25);
        double cap = Copepods.capacity(level, r.getTank());
        LOGGER.info("[smoketest] M48 copepods {} of {}", r.getTank().copepods(), cap);
        if (cap <= 0 || r.getTank().copepods() < cap * 0.5) LOGGER.error("[smoketest] M48 the copepods did not grow: {} of {}", r.getTank().copepods(), cap);
        // the clownfish larvae with rotifers and phytoplankton
        larvae = base.offset(8, 0, -8);
        TankBlockEntity l = tank(level, p, larvae, 2, 2, 2, 26F, 35F);
        InteriorRegion lr = region(level, l, Blocks.SAND.defaultBlockState());
        coral(in, lr, PlantSpecies.BUBBLE_TIP_ANEMONE, 16, 18, 4803L);
        light(level, larvae, 2, 2, 0, null);
        fish(level, l, TankmateSpecies.OCELLARIS_CLOWNFISH, "wild", false);
        fish(level, l, TankmateSpecies.OCELLARIS_CLOWNFISH, "wild", true);
        var spots = TankmateSpots.anemones(level.getServer(), l.getTank());
        Vec3 foot = spots.isEmpty() ? new TankWaterVolume(l.getTank()).box().getCenter()
                : TankmateSpots.anemoneFoot(level.getServer(), l.getTank(), spots.getFirst().pos());
        if (spots.isEmpty()) LOGGER.error("[smoketest] M48 no anemone");
        Clutch k = new Clutch(l.getTank().nextClutchId(), TankmateSpecies.OCELLARIS_CLOWNFISH, Clutch.Kind.CAVE, foot.x, foot.y, foot.z, 30,
                TankmateGenes.wild(TankmateSpecies.OCELLARIS_CLOWNFISH), TankmateGenes.wild(TankmateSpecies.OCELLARIS_CLOWNFISH));
        k.age = Clutch.eggDays(TankmateSpecies.OCELLARIS_CLOWNFISH) + 0.5;
        k.rotifers = true;
        for (TankmateEntity m : TankmateBreeding.tankmates(level, l.getTank())) if (!m.isFemale()) k.guardian = m.getUUID();
        l.getTank().clutches().add(k);
        l.getTank().setRotifers(12);
        ClownLarvae.dosePhytoplankton(l.getTank());
        ClownLarvae.dosePhytoplankton(l.getTank());
        l.markTankChanged();
        if (k.stage() != 1) LOGGER.error("[smoketest] M48 the clownfish clutch is not in its larva stage: {}", k.stage());
    }

    private void pose(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        TankBlockEntity f = TankMultiblock.getController(level, forms);
        if (f != null) row(level, f, 6, new double[]{1.45, 1.05, 0.8}, 0.95);
        for (BlockPos t : List.of(neons, pandas, bronzes, garras)) {
            TankBlockEntity c = TankMultiblock.getController(level, t);
            if (c != null) front(level, c, t == neons || t == garras ? 0.95 : 0.7, t == neons ? 0.42 : t == garras ? 0.8 : 0.62);
        }
        TankBlockEntity r = TankMultiblock.getController(level, reef);
        if (r != null) {
            TankWaterVolume w = new TankWaterVolume(r.getTank());
            java.util.Map<TankmateSpecies, Integer> count = new java.util.HashMap<>();
            List<TankmateEntity> fish = new ArrayList<>(TankmateBreeding.tankmates(level, r.getTank()));
            fish.sort((x, y) -> Boolean.compare(x.isFemale(), y.isFemale())); // the anthias male first
            for (TankmateEntity m : fish) {
                int i = count.merge(m.species(), 1, Integer::sum) - 1;
                double y = m.species() == TankmateSpecies.MANDARINFISH ? 0.2 : m.species() == TankmateSpecies.BANGGAI_CARDINAL ? 0.8 : 1.35;
                // one row per species from the left (seen from the north: high x is left)
                Vec3 a = new Vec3(w.box().maxX - 0.6 - i * 0.95, w.box().minY + y, w.box().minZ + 0.6);
                freezeAt(m, a, -90F);
                String key = m.species() == TankmateSpecies.LYRETAIL_ANTHIAS && m.isFemale() ? "lyretail_anthias_female" : m.species().id();
                at.putIfAbsent(key, a);
            }
            List<Critter> shrimp = new ArrayList<>();
            for (Critter k : r.getTank().critters()) if (k.species == CritterSpecies.FIRE_SHRIMP) shrimp.add(k);
            if (!shrimp.isEmpty()) {
                Critter k = shrimp.getFirst();
                k.x = w.box().minX + 2.6;
                k.z = w.box().minZ + 0.45;
                k.y = w.box().minY + 0.02;
                r.markTankChanged();
                at.put("fire_shrimp", new Vec3(k.x, k.y, k.z));
            }
        }
        TankBlockEntity l = TankMultiblock.getController(level, larvae);
        if (l != null) {
            var b = new TankWaterVolume(l.getTank()).box();
            int i = 0;
            for (TankmateEntity m : TankmateBreeding.tankmates(level, l.getTank())) {
                freezeAt(m, new Vec3(b.minX + 0.65 + i++ * 0.65, b.minY + 0.75, b.minZ + 0.7), -90F);
            }
        }
    }

    /** The current place of a posed reef animal (server side). */
    private Vec3 where(ServerLevel level, String k) {
        TankBlockEntity r = TankMultiblock.getController(level, reef);
        if (r == null) return at.get(k);
        if (k.equals("fire_shrimp")) {
            for (Critter c : r.getTank().critters()) if (c.species == CritterSpecies.FIRE_SHRIMP) return new Vec3(c.x, c.y, c.z);
            return null;
        }
        Vec3 posed = at.get(k);
        TankmateEntity best = null;
        for (TankmateEntity m : TankmateBreeding.tankmates(level, r.getTank())) {
            String key = m.species() == TankmateSpecies.LYRETAIL_ANTHIAS && m.isFemale() ? "lyretail_anthias_female" : m.species().id();
            if (!key.equals(k)) continue;
            if (best == null || posed != null && m.position().distanceToSqr(posed) < best.position().distanceToSqr(posed)) best = m;
        }
        return best == null ? posed : best.position();
    }

    private void row(ServerLevel level, TankBlockEntity c, int perRow, double[] ys, double step) {
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        List<TankmateEntity> fish = new ArrayList<>(TankmateBreeding.tankmates(level, c.getTank()));
        fish.sort((a, b) -> a.species() != b.species() ? Integer.compare(a.species().ordinal(), b.species().ordinal())
                : Integer.compare(a.species().varietyIndex(a.variety()), b.species().varietyIndex(b.variety())));
        for (int i = 0; i < fish.size(); i++) {
            int col = i % perRow, rowN = i / perRow;
            Vec3 a = new Vec3(w.box().minX + 0.4 + col * step, w.box().minY + ys[Math.min(rowN, ys.length - 1)], w.box().minZ + 0.6);
            freezeAt(fish.get(i), a, -90F);
            at.putIfAbsent(fish.get(i).species().id(), a);
        }
    }

    /** The forms of one species in a row just behind the front glass, the wild type on the left (seen from the north). */
    private void front(ServerLevel level, TankBlockEntity c, double y, double step) {
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        List<TankmateEntity> fish = new ArrayList<>(TankmateBreeding.tankmates(level, c.getTank()));
        fish.sort((a, b) -> Integer.compare(a.species().varietyIndex(a.variety()), b.species().varietyIndex(b.variety())));
        double mid = (w.box().minX + w.box().maxX) / 2, half = (fish.size() - 1) * step / 2;
        for (int i = 0; i < fish.size(); i++) {
            Vec3 a = new Vec3(mid + half - i * step, w.box().minY + y, w.box().minZ + 0.42);
            freezeAt(fish.get(i), a, -90F);
            at.putIfAbsent(fish.get(i).species().id() + "_" + fish.get(i).variety(), a);
        }
    }

    private static void freezeAt(TankmateEntity m, Vec3 a, float yaw) {
        m.moveTo(a.x, a.y, a.z, yaw, 0F);
        SmokeFreeze.freeze(m, true);
        m.setDeltaMovement(Vec3.ZERO);
        m.setYRot(yaw);
        m.yBodyRot = yaw;
        m.yHeadRot = yaw;
    }

    // ------------------------------------------------------------------ helpers

    private void menu(java.util.function.Supplier<BlockPos> tank, int tab, String name, String lang) {
        then(10, () -> server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), tank.get());
            if (c == null) return;
            org.overengineer.bettacraft.platform.Platform.get().openMenu(p, new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        }));
        then(30, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.selectTabForTest(tab);
            else LOGGER.error("[smoketest] M48 no tank menu for {}", name);
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
            LOGGER.error("[smoketest] M48 {} not placed", s.id());
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
        if (e == null) LOGGER.error("[smoketest] M48 {} {} not spawned", s.id(), variety);
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
        if (mc.player != null) LOGGER.info("[smoketest] M48 shot {} from {}", name, mc.player.getEyePosition());
        Screenshot.grab(mc.gameDirectory, "bettacraft_m48_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
