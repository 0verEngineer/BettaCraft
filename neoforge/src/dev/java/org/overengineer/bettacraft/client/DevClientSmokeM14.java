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
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterMovement;
import org.overengineer.bettacraft.critters.CritterObject;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.entity.frog.DwarfFrogEntity;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.genetics.BettaGenome;
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
import org.overengineer.bettacraft.tank.algae.AlgaeSim;
import org.overengineer.bettacraft.tank.algae.TankAlgae;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M14 part of the dev client smoke test: the small animals. A planted 4x2x2 tank under a lit light with a colony of
 * Neocaridina in all colour lines (a female with eggs), orange and wild dwarf crayfish (a shed shell), nerites, apple
 * snails, ramshorns, bladder, trumpet (one dug in) and assassin snails on the floor and the front glass, a pink apple
 * snail clutch above the water line, a betta and a dwarf clawed frog breathing at the surface. A 3x1x2 tank shows algae
 * on the glass before and after the scraper. Screenshots {@code bettacraft_m14_*}: overview, close-ups (shrimp, snails,
 * frog), algae before / after, the small animals page and a frog in the animals tab of the menu (English, German).
 */
final class DevClientSmokeM14 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private BlockPos base;
    private int wait;

    DevClientSmokeM14() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        // the colony from the front, slightly from above (the chunks load first, then the animals are put in place)
        then(20, () -> look(base.getX() + 2.0, base.getY() + 2.2, base.getZ() - 3.0, 0F, 18F));
        then(80, () -> server(this::arrange));
        then(100, () -> {
            logClient();
            shot("overview");
        });
        // close-up of the shrimp on the floor, eye at the glass
        then(1, () -> look(base.getX() + 1.3, base.getY() + 0.55, base.getZ() - 0.55, 0F, 22F));
        then(60, () -> shot("closeup"));
        // the snails on the front glass and the clutch above the water line
        then(1, () -> look(base.getX() + 2.9, base.getY() + 1.3, base.getZ() - 0.9, 0F, 12F));
        then(60, () -> shot("snails"));
        then(1, () -> look(base.getX() + 2.6, base.getY() + 2.2, base.getZ() - 0.7, 8F, 35F));
        then(60, () -> shot("clutch"));
        // the frog breathing at the surface, from the side
        then(1, () -> server(this::placeFrog));
        then(1, () -> look(base.getX() + 0.9, base.getY() + 1.85, base.getZ() - 0.8, 10F, 10F));
        then(20, () -> server(this::placeFrog));
        then(30, () -> shot("frog"));
        // algae: green glass, then the north side scraped clean
        then(1, () -> look(base.getX() + 9.5, base.getY() + 0.9, base.getZ() - 2.2, 0F, 12F));
        then(80, () -> shot("algae_before"));
        then(1, () -> server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), base.offset(8, 0, 0));
            int n = c == null ? 0 : TankAlgae.scrape(p.serverLevel(), c, base.offset(8, 0, 0), Direction.NORTH, true);
            LOGGER.info("[smoketest] M14 scraped {} faces", n);
        }));
        then(60, () -> shot("algae_after"));
        // the menu: small animals page (English, German), a frog in the animals tab
        then(1, () -> mc.options.hideGui = false);
        then(1, () -> server(p -> {
            p.teleportTo(p.serverLevel(), base.getX() + 2.0, base.getY() + 1.0, base.getZ() - 2.5, 0F, 0F);
            TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), base);
            org.overengineer.bettacraft.platform.Platform.get().openMenu(p, new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        }));
        then(30, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.showCrittersPageForTest();
        });
        then(30, () -> shot("menu_critters"));
        then(1, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts && !ts.showFrogForTest()) {
                LOGGER.error("[smoketest] M14 no frog in the animals tab");
            }
        });
        then(20, () -> shot("menu_frog"));
        String lang = mc.options.languageCode;
        then(1, () -> language("de_de"));
        then(1, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.showCrittersPageForTest();
        });
        then(20, () -> shot("menu_critters_de"));
        then(1, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts && !ts.showFrogForTest()) {
                LOGGER.error("[smoketest] M14 no frog in the animals tab");
            }
        });
        then(20, () -> shot("menu_frog_de"));
        then(1, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.selectTabForTest(0);
        });
        then(20, () -> shot("menu_overview_de"));
        then(1, () -> language(lang));
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.setScreen(null));
        then(1, () -> server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), base);
            if (c == null) return;
            LOGGER.info("[smoketest] M14 colony: {} animals, {} objects, algae {}", c.getTank().critters().size(), c.getTank().critterObjects().size(),
                    TankAlgae.mean(c.getTank()));
        }));
    }

    private void logClient() {
        if (mc.level == null) return;
        var be = mc.level.getBlockEntity(base);
        int n = be instanceof TankBlockEntity t && t.getTank() != null ? t.getTank().critters().size() : -1;
        var algaeBe = mc.level.getBlockEntity(base.offset(8, 0, 0));
        int faces = algaeBe instanceof TankBlockEntity t && t.getTank() != null ? t.getTank().algae().size() : -1;
        LOGGER.info("[smoketest] M14 client: {} small animals in the colony tank, algae on {} blocks", n, faces);
        if (be instanceof TankBlockEntity t && t.getTank() != null) {
            for (Critter k : t.getTank().critters()) if (k.onGlass()) LOGGER.info("[smoketest] M14 client glass: {}", k.debug());
        }
        for (Entity e : mc.level.entitiesForRendering()) if (e instanceof DwarfFrogEntity f) LOGGER.info("[smoketest] M14 client frog at {} breathing {}", f.position(), f.isBreathing());
        if (n <= 0) LOGGER.error("[smoketest] M14 the client shows no small animals");
        if (faces <= 0) LOGGER.error("[smoketest] M14 the client shows no algae");
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
        c.getTank().setWaterTemp(25F);
        c.getTank().setTargetTemp(25F);
        return c;
    }

    private static void lights(ServerLevel level, BlockPos min, int sx, int y, int z) {
        for (int x = 0; x < sx; x++) {
            level.setBlockAndUpdate(min.offset(x, y, z), ModBlocks.TANK_LIGHT.get().defaultBlockState()
                    .setValue(TankLightBlock.AXIS, Direction.Axis.X).setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < sx - 1));
        }
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ServerLevel in = InteriorManager.interiorLevel(p.server);
        base = p.blockPosition().offset(0, 2, 260);
        // A: the colony tank
        place(level, cuboid(base, 4, 2, 2));
        TankBlockEntity c = setup(level, p, base, 15000);
        lights(level, base, 4, 2, 1);
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++)
            for (int z = 0; z < r.sizeZ(); z++) {
                BlockState s = z < 12 ? Blocks.SAND.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
                Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), s, false);
            }
        // a flat stone in the middle (shrimp and snails on top), plants at the back
        for (int x = 26; x <= 36; x++) for (int z = 10; z <= 18; z++) for (int y = 0; y < 3; y++) in.setBlock(r.toWorld(x, y, z), Blocks.STONE.defaultBlockState(), 2);
        long seed = 1400;
        InteriorPlants.plantAt(in, r, PlantSpecies.JAVA_MOSS, 30, 3, 14, PlantSpecies.JAVA_MOSS.stages(), seed++);
        for (var e : List.of(Map.entry(PlantSpecies.VALLISNERIA, 4), Map.entry(PlantSpecies.ROTALA, 14), Map.entry(PlantSpecies.AMAZON_SWORD, 46),
                Map.entry(PlantSpecies.VALLISNERIA, 58))) {
            InteriorPlants.plantAt(in, r, e.getKey(), e.getValue(), 0, 26, e.getKey().stages(), seed++);
        }
        InteriorPlants.plantAt(in, r, PlantSpecies.CRYPT_GREEN, 20, 0, 22, 6, seed++);
        InteriorPlants.plantAt(in, r, PlantSpecies.CRYPT_BROWN, 42, 0, 20, 6, seed);
        // a betta and a frog
        BettaData d = BettaData.create(BettaGenome.of(Map.of("finform", "halfmoon", "fin_length", "long", "body_color", "royal_blue", "fin_color", "red"),
                BettaGenome.DEFAULT), false, level.getGameTime());
        TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d), ModEntities.BETTA.get(), p, 0L);
        DwarfFrogEntity f = ModEntities.DWARF_FROG.get().create(level);
        if (f != null) {
            ItemStack bag = FishBagItem.fillWith(f);
            f.discard();
            TankInhabitants.insert(level, c, bag, ModEntities.DWARF_FROG.get(), p, 0L);
        }
        // B: algae tank
        BlockPos ab = base.offset(8, 0, 0);
        place(level, cuboid(ab, 3, 1, 2));
        TankBlockEntity a = setup(level, p, ab, 6000);
        lights(level, ab, 3, 1, 1);
        InteriorRegion ra = InteriorManager.ensureRegion(level, a);
        for (int x = 0; x < ra.sizeX(); x++)
            for (int z = 0; z < ra.sizeZ(); z++) Substrate.set(in, new BlockPos(ra.originX() + x, InteriorRegion.FLOOR_Y, ra.originZ() + z), Blocks.GRAVEL.defaultBlockState(), false);
        for (var face : TankAlgae.wetFaces(a.getTank())) AlgaeSim.set(a.getTank().algae(), face.pos(), face.dir(), 0.45F + 0.55F * ((face.pos() + face.dir()) % 3 == 0 ? 0.5F : 1F));
        TankAlgae.syncIfChanged(a);
        a.markTankChanged();
        TankCritters.add(level, a, CritterSpecies.ZEBRA_NERITE, CritterGenes.WILD, true, 10);
        LOGGER.info("[smoketest] M14 scene at {}", base);
    }

    /** Places the animals for the pictures and holds them still. */
    private void arrange(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        TankBlockEntity c = TankMultiblock.getController(level, base);
        if (c == null) return;
        TankData t = c.getTank();
        t.critters().clear();
        t.critterObjects().clear();
        var terrain = TankCritters.terrain(level, t);
        double bx = base.getX(), by = base.getY(), bz = base.getZ();
        // shrimp on the floor in front: every colour line
        Object[][] shrimp = {
                {0, 0.45F, 0.55, 0.45, 30F}, {0, 0.45F, 0.85, 0.65, -40F}, {0, 0.2F, 1.15, 0.4, 80F}, {0, 0.9F, 1.45, 0.55, 10F},
                {0, 0.9F, 1.75, 0.38, -100F}, {1, 0.6F, 0.65, 0.95, 60F}, {1, 0.6F, 1.05, 0.85, -20F}, {2, 0.6F, 2.25, 0.45, 150F},
                {3, 0.6F, 2.55, 0.62, -60F}, {4, 0.6F, 2.85, 0.42, 20F}, {5, 0.6F, 3.15, 0.6, -150F}, {100, 0.6F, 3.45, 0.45, 70F},
                {-1, 0.1F, 1.95, 0.75, 100F}, {0, 0.7F, 2.0, 1.3, 0F}};
        int i = 0;
        for (Object[] s : shrimp) {
            int color = (Integer) s[0];
            CritterGenes g = color == 100 ? new CritterGenes((1 << 2) | (1 << 5), (1 << 2) | (1 << 5), 0.6F) : CritterGenes.pure(color, (Float) s[1]);
            Critter k = put(t, terrain, CritterSpecies.NEOCARIDINA, g, i % 2 == 0, bx + (Double) s[2], bz + (Double) s[3], (Float) s[4]);
            if (i == 3) {
                k.female = true;
                k.eggs = 0.5;
                k.father = g;
            }
            i++;
        }
        // on the stone in the middle
        put(t, terrain, CritterSpecies.NEOCARIDINA, CritterGenes.pure(0, 0.85F), true, bx + 1.85, bz + 0.95, 40F);
        put(t, terrain, CritterSpecies.NEOCARIDINA, CritterGenes.pure(1, 0.7F), false, bx + 2.15, bz + 1.05, -120F);
        // dwarf crayfish and a shed shell
        put(t, terrain, CritterSpecies.DWARF_CRAYFISH, CritterGenes.pure(0, 0.6F), true, bx + 0.6, bz + 1.35, 20F);
        put(t, terrain, CritterSpecies.DWARF_CRAYFISH, CritterGenes.pure(0, 0.6F), false, bx + 3.3, bz + 1.2, -30F);
        Critter wild = put(t, terrain, CritterSpecies.DWARF_CRAYFISH, CritterGenes.WILD, false, bx + 2.7, bz + 1.55, 160F);
        CritterObject molt = new CritterObject(t.nextCritterId(), CritterObject.Kind.MOLT, bx + 2.95, terrain.ground(bx + 2.95, bz + 0.95), bz + 0.95);
        molt.species = CritterSpecies.DWARF_CRAYFISH;
        molt.yaw = 70F;
        t.critterObjects().add(molt);
        wild.sinceMolt = 0.2;
        // snails on the floor
        put(t, terrain, CritterSpecies.APPLE_SNAIL, CritterGenes.pure(0, 0.5F), true, bx + 3.4, bz + 0.6, -20F);
        put(t, terrain, CritterSpecies.APPLE_SNAIL, CritterGenes.pure(1, 0.5F), false, bx + 0.35, bz + 0.9, 60F);
        put(t, terrain, CritterSpecies.APPLE_SNAIL, CritterGenes.pure(2, 0.5F), true, bx + 1.2, bz + 1.6, 90F);
        put(t, terrain, CritterSpecies.TRUMPET_SNAIL, CritterGenes.WILD, false, bx + 2.45, bz + 0.3, 40F);
        put(t, terrain, CritterSpecies.TRUMPET_SNAIL, CritterGenes.WILD, false, bx + 2.6, bz + 0.85, -40F).buried = true;
        put(t, terrain, CritterSpecies.ASSASSIN_SNAIL, CritterGenes.WILD, true, bx + 1.6, bz + 0.3, 110F);
        // snails on the front glass (north)
        glass(t, CritterSpecies.ZEBRA_NERITE, CritterGenes.WILD, bx + 2.6, by + 0.55, bz, 20F);
        glass(t, CritterSpecies.HORNED_NERITE, CritterGenes.WILD, bx + 3.05, by + 0.95, bz, -30F);
        glass(t, CritterSpecies.OLIVE_NERITE, CritterGenes.WILD, bx + 3.5, by + 0.45, bz, 170F);
        glass(t, CritterSpecies.RED_NERITE, CritterGenes.WILD, bx + 2.3, by + 1.25, bz, 60F);
        glass(t, CritterSpecies.RAMSHORN, CritterGenes.pure(0, 0.7F), bx + 2.75, by + 1.45, bz, 0F);
        glass(t, CritterSpecies.RAMSHORN, CritterGenes.pure(1, 0.7F), bx + 3.3, by + 1.6, bz, 90F);
        glass(t, CritterSpecies.RAMSHORN, CritterGenes.WILD, bx + 3.65, by + 1.15, bz, -60F);
        glass(t, CritterSpecies.BLADDER_SNAIL, CritterGenes.WILD, bx + 2.95, by + 1.8, bz, 30F);
        glass(t, CritterSpecies.NEOCARIDINA, CritterGenes.pure(2, 0.7F), bx + 1.6, by + 0.8, bz, -10F);
        // the apple snail clutch above the water line (front glass)
        CritterObject clutch = new CritterObject(t.nextCritterId(), CritterObject.Kind.CLUTCH, bx + 2.75, t.waterSurfaceY() + 0.02,
                bz + CritterMovement.GLASS_INSET);
        clutch.face = Critter.GLASS_NORTH;
        clutch.count = 7;
        t.critterObjects().add(clutch);
        // the betta in the middle, the frog at the surface
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) {
            if (e instanceof BettaEntity b) {
                SmokeFreeze.freeze(b, true);
                b.moveTo(bx + 1.2, by + 1.0, bz + 1.1, 120F, 0F);
                b.setYBodyRot(120F);
                b.setYHeadRot(120F);
            }
        }
        placeFrog(p);
        c.markTankChanged();
        TankCritters.sync(level, c);
        LOGGER.info("[smoketest] M14 arranged {} animals, {} objects", t.critters().size(), t.critterObjects().size());
    }

    /** The frog at the surface, breathing, held still. */
    private void placeFrog(ServerPlayer p) {
        TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), base);
        if (c == null) return;
        TankData t = c.getTank();
        for (Entity e : TankInhabitants.liveInhabitants(p.serverLevel(), t)) {
            if (e instanceof BettaEntity b) {
                SmokeFreeze.freeze(b, true);
                b.setDeltaMovement(Vec3.ZERO);
                b.moveTo(base.getX() + 2.3, base.getY() + 1.05, base.getZ() + 1.2, 120F, 0F);
                b.setYBodyRot(120F);
                b.setYHeadRot(120F);
            }
            if (!(e instanceof DwarfFrogEntity f)) continue;
            SmokeFreeze.freeze(f, true);
            f.setBreathForTest(10);
            f.setDeltaMovement(Vec3.ZERO);
            f.moveTo(base.getX() + 1.2, t.waterSurfaceY() - f.getBbHeight() + 0.03, base.getZ() + 0.45, 200F, 0F);
            f.setYBodyRot(200F);
            f.setYHeadRot(200F);
            LOGGER.info("[smoketest] M14 frog placed at {}", f.position());
        }
    }

    private static Critter put(TankData t, CritterMovement.Terrain terrain, CritterSpecies s, CritterGenes g, boolean female, double x, double z, float yaw) {
        double y = terrain.ground(x, z);
        Critter k = new Critter(t.nextCritterId(), s, s.hasSexes() && female, g, x, Double.isFinite(y) ? y : t.min().getY() + 0.05, z);
        k.age = s.juvenileDays() + 2;
        k.yaw = yaw;
        k.idle = 1_000_000;
        t.critters().add(k);
        return k;
    }

    private static void glass(TankData t, CritterSpecies s, CritterGenes g, double x, double y, double bz, float yaw) {
        Critter k = new Critter(t.nextCritterId(), s, true, g, x, y, bz + CritterMovement.GLASS_INSET);
        k.age = s.juvenileDays() + 2;
        k.face = Critter.GLASS_NORTH;
        k.yaw = yaw;
        k.idle = 1_000_000;
        t.critters().add(k);
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m14_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
