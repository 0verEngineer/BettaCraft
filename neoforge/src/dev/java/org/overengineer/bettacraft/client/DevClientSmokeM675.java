package org.overengineer.bettacraft.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.client.screen.ConfirmEmptyScreen;
import org.overengineer.bettacraft.client.tank.AlgaeFilm;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.FilterHeaters;
import org.overengineer.bettacraft.registry.ModBlocks;
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
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateItems;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M67.5 client smoke ({@code -Psmoketestm675}, NeoForge and Fabric, background window; {@code -Pm675parts=animals,algae,screens,
 * cabinet,bigtank,create}). The points of the user test of M66 to M67:
 * <ul>
 *     <li>{@code animals}: shrimp, a CPO, a fire shrimp, snails walking (the legs on the body), an angelfish side-on (its sails),
 *     a pleco on the front glass (its mouth), a female guppy nose to the front glass (nothing through the glass);</li>
 *     <li>{@code algae}: a long tank with algae at three levels (no repeating pattern, the blots stay where they are);</li>
 *     <li>{@code screens}: the release confirmation with a long bucket name, en and de (the text above the buttons);</li>
 *     <li>{@code cabinet}: a cabinet with an air pump and none with a filter, one with a filter;</li>
 *     <li>{@code bigtank}: a 24 x 6 x 12 tank at interior scale 8 from the front and from above (no white-blue haze over its
 *     far half) and the player swimming in it (the view under water);</li>
 *     <li>{@code create} (with {@code -Pcreate}): a tank with algae on a bearing contraption; guard: with and without the film,
 *     the pixels of the tank differ only as much as a translucent film can (no holes in the glass and water behind it).</li>
 * </ul>
 * Screenshots {@code bettacraft_m675_<shot>.png}; problems are logged as FAIL and counted.
 */
final class DevClientSmokeM675 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final Set<String> parts;
    private int wait;
    private int problems;
    private volatile BlockPos base;
    private volatile BlockPos animals, algaeTank, cabAir, cabFilter, big, bearingPos;
    private BlockEntity bearing;
    private float angle = Float.NaN, speed;
    private String lang;
    private int renderDistance = 12;

    DevClientSmokeM675() {
        String p = System.getProperty("bettacraft.smoketest.m675parts", "animals,algae,screens,cabinet,bigtank,create");
        parts = Set.of(p.split(","));
        LOGGER.info("[smoketest] M67.5 parts {}", parts);
        then(1, () -> {
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            lang = mc.options.languageCode;
            server(this::ground);
        });
        then(20, () -> {});
        if (parts.contains("animals")) animals();
        if (parts.contains("algae")) algae();
        if (parts.contains("screens")) screens();
        if (parts.contains("cabinet")) cabinet();
        if (parts.contains("bigtank")) bigTank();
        if (parts.contains("create")) create();
        then(1, () -> {
            mc.options.hideGui = false;
            language(lang);
        });
        then(5, () -> LOGGER.info("[smoketest] M67.5 done: {} problem(s)", problems));
    }

    int problems() {
        return problems;
    }

    private void ground(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        level.setDayTime(6000);
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        level.setWeatherParameters(24000, 0, false, false);
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        base = p.blockPosition().offset(4, 0, 12);
    }

    // ------------------------------------------------------------------ animals (P1-P4)

    private void animals() {
        then(1, () -> server(p -> {
            ServerLevel level = p.serverLevel();
            animals = base;
            clear(level, animals.offset(-2, -1, -4), animals.offset(7, 4, 5));
            TankBlockEntity c = tank(level, animals, 5, 2, 3, Blocks.SAND.defaultBlockState());
            TankData t = c.getTank();
            t.critters().clear();
            CritterSpecies[] sp = {CritterSpecies.NEOCARIDINA, CritterSpecies.FIRE_SHRIMP, CritterSpecies.DWARF_CRAYFISH, CritterSpecies.ZEBRA_NERITE,
                    CritterSpecies.NEOCARIDINA, CritterSpecies.FIRE_SHRIMP};
            for (int i = 0; i < sp.length; i++) {
                Critter k = new Critter(t.nextCritterId(), sp[i], sp[i].hasSexes(), CritterGenes.WILD, animals.getX() + 0.6 + i * 0.7, t.min().getY() + 0.05,
                        animals.getZ() + 0.6 + (i % 2) * 0.5);
                k.age = sp[i].juvenileDays() + 2;
                t.critters().add(k);
            }
            RandomSource r = RandomSource.create(675);
            insert(level, c, TankmateSpecies.ANGELFISH, 1, r);
            insert(level, c, TankmateSpecies.BRISTLENOSE_PLECO, 1, r);
            insert(level, c, TankmateSpecies.GUPPY, 2, r);
            c.markTankChanged();
            TankCritters.sync(level, c);
        }));
        then(40, () -> server(p -> {
            ServerLevel level = p.serverLevel();
            TankBlockEntity c = TankMultiblock.getController(level, animals);
            TankCritters.sync(level, c);
            var w = new org.overengineer.bettacraft.tank.TankWaterVolume(c.getTank());
            for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) {
                if (!(e instanceof TankmateEntity m)) continue;
                double front = w.box().minZ + m.sideMargin();
                switch (m.species().model()) {
                    case ANGEL -> pose(m, animals.getX() + 1.5, w.box().minY + 0.9, front + 0.35, -90F, 0); // side-on
                    case PLECO -> pose(m, animals.getX() + 3.2, w.box().minY + 0.6, w.box().minZ + m.sideMargin() - 0.005, 180F, TankmateEntity.FLAG_GLASS);
                    default -> {
                        // nose to the front glass at its margin (the female is the longer one)
                        double x = animals.getX() + (m.isFemale() ? 4.2 : 3.8);
                        pose(m, x, w.box().minY + 1.2, front, 180F, 0);
                    }
                }
            }
        }));
        then(1, () -> mc.options.hideGui = true);
        // the whole tank, then close-ups through the front glass
        then(1, () -> look(animals.getX() + 2.5, animals.getY() + 1.4, animals.getZ() - 3.2, 0F, 12F));
        then(40, () -> shot("animals_front"));
        then(1, () -> look(animals.getX() + 1.5, animals.getY() + 1.0, animals.getZ() - 0.9, 0F, 8F));
        then(25, () -> shot("animals_angelfish"));
        then(1, () -> look(animals.getX() + 3.2, animals.getY() + 0.65, animals.getZ() - 0.55, 0F, 0F));
        then(25, () -> shot("animals_pleco_mouth"));
        then(3, () -> shot("animals_pleco_mouth2"));
        // the guppies' snouts at the front glass, seen along the glass from above
        then(1, () -> look(animals.getX() + 4.0, animals.getY() + 2.3, animals.getZ() - 0.6, 0F, 62F));
        then(25, () -> shot("animals_glass_top"));
        // the small animals walking on the floor, low through the front glass, two frames apart (the legs move)
        then(1, () -> look(animals.getX() + 2.0, animals.getY() + 0.25, animals.getZ() - 0.7, 0F, 12F));
        then(30, () -> shot("animals_critters_a"));
        then(4, () -> shot("animals_critters_b"));
        then(1, () -> look(animals.getX() + 2.5, animals.getY() + 2.4, animals.getZ() + 1.5, 0F, 89F));
        then(20, () -> shot("animals_critters_top"));
        then(1, () -> mc.options.hideGui = false);
    }

    private static void insert(ServerLevel level, TankBlockEntity c, TankmateSpecies s, int n, RandomSource r) {
        ItemStack bag = TankmateItems.groupBag(s, n, () -> TankmateGenes.wild(s), new java.util.Random(675), true);
        Component err = TankInhabitants.insert(level, c, bag, TankInhabitants.containedType(bag), null, true);
        if (err != null) LOGGER.warn("[smoketest] M67.5 {} not inserted: {}", s.id(), err.getString());
    }

    private static void pose(TankmateEntity m, double x, double y, double z, float yaw, int flags) {
        SmokeFreeze.freeze(m, true);
        m.moveTo(x, y, z, yaw, 0F);
        m.setDeltaMovement(Vec3.ZERO);
        m.setYRot(yaw);
        m.yBodyRot = yaw;
        m.yHeadRot = yaw;
        m.setPoseForSmoke(flags);
    }

    // ------------------------------------------------------------------ algae (P5)

    private void algae() {
        then(1, () -> server(p -> {
            ServerLevel level = p.serverLevel();
            algaeTank = base.offset(14, 0, 0);
            clear(level, algaeTank.offset(-2, -1, -6), algaeTank.offset(17, 5, 5));
            tank(level, algaeTank, 14, 3, 2, Blocks.SAND.defaultBlockState());
            BlockPos second = algaeTank.offset(0, 0, 3);
            tank(level, second, 14, 3, 1, Blocks.GRAVEL.defaultBlockState());
        }));
        then(1, () -> mc.options.hideGui = true);
        then(1, () -> look(algaeTank.getX() + 7.0, algaeTank.getY() + 1.6, algaeTank.getZ() - 7.5, 0F, 4F));
        for (float lvl : new float[]{0.35F, 0.65F, 1F}) {
            then(1, () -> server(p -> setAlgae(p.serverLevel(), lvl, algaeTank, algaeTank.offset(0, 0, 3))));
            then(60, () -> shot("algae_" + Math.round(lvl * 100)));
        }
        // close: the film continues over the block borders
        then(1, () -> look(algaeTank.getX() + 3.0, algaeTank.getY() + 1.5, algaeTank.getZ() - 2.0, 0F, 0F));
        then(20, () -> shot("algae_close"));
        then(1, () -> mc.options.hideGui = false);
    }

    private static void setAlgae(ServerLevel level, float lvl, BlockPos... tanks) {
        for (BlockPos at : tanks) {
            TankBlockEntity c = TankMultiblock.getController(level, at);
            if (c == null) continue;
            c.getTank().algae().clear();
            for (var f : TankAlgae.wetFaces(c.getTank())) AlgaeSim.set(c.getTank().algae(), f.pos(), f.dir(), lvl);
            c.markTankChanged();
        }
    }

    // ------------------------------------------------------------------ screens (P6)

    private void screens() {
        for (String code : List.of("en_us", "de_de")) {
            then(1, () -> {
                language(code);
                ItemStack bag = new ItemStack(ModItems.FISH_BAG.get());
                bag.set(DataComponents.CUSTOM_NAME, Component.literal(code.equals("de_de") ? "Eimer mit 5 Sakura (Neocaridina-Garnele)"
                        : "Bucket of 5 Sakura (Neocaridina Shrimp)"));
                mc.setScreen(new ConfirmEmptyScreen(InteractionHand.MAIN_HAND, bag));
            });
            then(10, () -> {
                if (mc.screen instanceof ConfirmEmptyScreen s && !s.textAboveButtons()) problem("the confirmation text runs under the buttons (" + code + ")");
                shot("confirm_" + code);
            });
            then(1, () -> mc.setScreen(null));
        }
    }

    // ------------------------------------------------------------------ cabinet (P7)

    private void cabinet() {
        then(1, () -> server(p -> {
            ServerLevel level = p.serverLevel();
            cabAir = base.offset(0, 0, 16);
            cabFilter = cabAir.offset(5, 0, 0);
            clear(level, cabAir.offset(-2, -1, -5), cabAir.offset(9, 5, 4));
            for (BlockPos at : List.of(cabAir, cabFilter)) {
                for (int x = 0; x < 2; x++) {
                    level.setBlockAndUpdate(at.offset(x, 0, 0), ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
                }
                tank(level, at.above(), 2, 2, 1, Blocks.SAND.defaultBlockState());
                for (int x = 0; x < 2; x++) level.setBlockAndUpdate(at.offset(x, 0, 0), Block.updateFromNeighbourShapes(level.getBlockState(at.offset(x, 0, 0)), level, at.offset(x, 0, 0)));
            }
            if (level.getBlockEntity(cabAir) instanceof CabinetBlockEntity cb) {
                cb.items().setStackInSlot(CabinetBlockEntity.AIR_PUMP_SLOT, new ItemStack(ModItems.AIR_PUMP.get()));
                cb.setChanged();
            }
            if (level.getBlockEntity(cabFilter) instanceof CabinetBlockEntity cb) {
                ItemStack f = new ItemStack(ModItems.EXTERNAL_FILTER.get());
                FilterHeaters.write(f, List.of(new ItemStack(ModItems.HEATER_MEDIUM.get()), ItemStack.EMPTY));
                cb.items().setStackInSlot(CabinetBlockEntity.FILTER_SLOT, f);
                cb.setChanged();
            }
        }));
        then(1, () -> mc.options.hideGui = true);
        then(1, () -> look(cabAir.getX() + 3.5, cabAir.getY() + 2.2, cabAir.getZ() - 4.5, -20F, 18F));
        then(40, () -> shot("cabinet_front"));
        then(1, () -> look(cabAir.getX() + 3.5, cabAir.getY() + 2.2, cabAir.getZ() + 4.5, 160F, 18F));
        then(20, () -> shot("cabinet_back"));
        then(1, () -> mc.options.hideGui = false);
    }

    // ------------------------------------------------------------------ big tank (P8, P15)

    private void bigTank() {
        then(1, () -> server(p -> {
            ServerLevel level = p.serverLevel();
            big = base.offset(0, 1, 30);
            clear(level, big.offset(-4, -2, -14), big.offset(28, 8, 16));
            TankBlockEntity c = tank(level, big, 24, 6, 12, Blocks.SAND.defaultBlockState());
            InteriorRegion ir = InteriorManager.ensureRegion(level, c);
            ServerLevel in = InteriorManager.interiorLevel(level.getServer());
            rock(in, ir, ir.sizeX() / 5, ir.sizeZ() * 2 / 3, 12, 10);
            rock(in, ir, ir.sizeX() * 4 / 5, ir.sizeZ() * 3 / 5, 14, 12);
            c.markTankChanged();
            LOGGER.info("[smoketest] M67.5 big tank {} region {} x {} x {}", big, ir.sizeX(), ir.sizeY(), ir.sizeZ());
        }));
        then(1, () -> {
            mc.options.hideGui = true;
            renderDistance = mc.options.renderDistance().get();
            mc.options.renderDistance().set(8); // fog from about 115 blocks: the old bug fogged the far half of the 192 region blocks
        });
        // the user's view: from the path in front, the eye at the top of the tank (south of it, looking north)
        then(1, () -> look(big.getX() + 12, big.getY() + 5.5, big.getZ() + 18, 180F, 14F));
        then(80, () -> shot("bigtank_front"));
        then(1, () -> look(big.getX() + 12, big.getY() + 20, big.getZ() + 22, 180F, 45F));
        then(40, () -> shot("bigtank_above"));
        // in the water: at the middle, looking along the tank and down to the floor
        then(1, () -> look(big.getX() + 4, big.getY() + 3.0, big.getZ() + 6, -90F, 8F));
        then(40, () -> shot("bigtank_inside_along"));
        then(1, () -> look(big.getX() + 12, big.getY() + 3.0, big.getZ() + 9, 180F, 25F));
        then(20, () -> shot("bigtank_inside_back"));
        then(1, () -> look(big.getX() + 12, big.getY() + 4.8, big.getZ() + 6, 0F, 50F));
        then(20, () -> shot("bigtank_inside_down"));
        then(1, () -> look(big.getX() + 12, big.getY() + 4.8, big.getZ() + 6, 0F, -40F));
        then(20, () -> shot("bigtank_inside_up"));
        then(1, () -> {
            mc.options.hideGui = false;
            mc.options.renderDistance().set(renderDistance);
        });
    }

    private static void rock(ServerLevel in, InteriorRegion r, int cx, int cz, int rad, int h) {
        for (int x = -rad; x <= rad; x++)
            for (int z = -rad; z <= rad; z++) {
                double d = Math.sqrt(x * x + z * z) / rad;
                if (d > 1.0) continue;
                int top = (int) Math.round(h * (1 - d * d * 0.7));
                for (int y = 0; y < top; y++) in.setBlock(r.toWorld(cx + x, y, cz + z), Blocks.STONE.defaultBlockState(), 2);
            }
    }

    // ------------------------------------------------------------------ algae on a contraption (P12)

    private final NativeImage[] createShots = new NativeImage[3];

    private void create() {
        if (!org.overengineer.bettacraft.platform.Platform.get().isModLoaded("create")) {
            LOGGER.info("[smoketest] M67.5 create part skipped (no Create, -Pcreate)");
            return;
        }
        then(1, () -> server(p -> {
            ServerLevel level = p.serverLevel();
            bearingPos = base.offset(40, 6, 0);
            clear(level, bearingPos.offset(-3, -7, -8), bearingPos.offset(6, 5, 6));
            var bearingBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:mechanical_bearing"));
            for (int y = 1; y < 7; y++) level.setBlock(bearingPos.below(y), Blocks.STONE_BRICKS.defaultBlockState(), 2);
            level.setBlock(bearingPos, with(bearingBlock.defaultBlockState(), "facing", Direction.UP), 3);
            bearing = level.getBlockEntity(bearingPos);
            TankBlockEntity c = tank(level, bearingPos.above(), 3, 2, 2, Blocks.SAND.defaultBlockState());
            setAlgae(level, 0.85F, c.getBlockPos());
        }));
        then(1, () -> mc.options.hideGui = true);
        then(1, () -> look(bearingPos.getX() + 1.5, bearingPos.getY() + 2.6, bearingPos.getZ() - 3.6, 0F, 14F));
        then(200, () -> server(p -> assemble()));
        // the server may be behind (the interiors of the scene are being built): wait until the client has the contraption
        waitFor(() -> clientContraptions() > 0, 600, "the contraption on the client");
        then(10, () -> speed = 1.5F);
        then(20, () -> {
            int contraptions = 0;
            for (Entity e : mc.level.entitiesForRendering()) if (BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath().contains("contraption")) contraptions++;
            LOGGER.info("[smoketest] M67.5 create (client): {} contraption entities, block above the bearing {}, film quads {}", contraptions,
                    mc.level.getBlockState(bearingPos.above()), org.overengineer.bettacraft.client.render.AlgaeRenderer.quadsForTest);
            shot("create_turning");
        });
        then(20, () -> speed = 0F);
        then(30, () -> {
            int quads = org.overengineer.bettacraft.client.render.AlgaeRenderer.quadsForTest;
            LOGGER.info("[smoketest] M67.5 create: {} film quads drawn on the contraption so far, bearing at {} degrees", quads, angle);
            if (quads == 0) problem("no algae film drawn on the contraption");
        });
        // without the film, with it, without again: the pixels that change between the two without (the water's animation, the
        // contraption settling) are left out of the guard
        then(1, () -> AlgaeFilm.hiddenForTest = true);
        then(3, () -> {
            createShots[1] = Screenshot.takeScreenshot(mc.getMainRenderTarget());
            shot("create_nofilm");
            AlgaeFilm.hiddenForTest = false;
        });
        then(3, () -> {
            createShots[0] = Screenshot.takeScreenshot(mc.getMainRenderTarget());
            shot("create_film");
            AlgaeFilm.hiddenForTest = true;
        });
        then(3, () -> {
            createShots[2] = Screenshot.takeScreenshot(mc.getMainRenderTarget());
            AlgaeFilm.hiddenForTest = false;
            guardCreate();
        });
        then(1, () -> mc.options.hideGui = false);
    }

    private int clientContraptions() {
        int n = 0;
        if (mc.level != null) for (Entity e : mc.level.entitiesForRendering()) if (BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath().contains("contraption")) n++;
        return n;
    }

    /** Waits (at most {@code maxTicks}) until {@code done} holds; a problem when it never does. */
    private void waitFor(java.util.function.BooleanSupplier done, int maxTicks, String what) {
        int[] n = {0};
        then(1, new Runnable() {
            @Override
            public void run() {
                if (done.getAsBoolean()) {
                    LOGGER.info("[smoketest] M67.5 {} after {} ticks", what, n[0]);
                    return;
                }
                if (++n[0] > maxTicks) {
                    problem("no " + what + " after " + maxTicks + " ticks");
                    return;
                }
                steps.addFirst(new Step(1, this));
            }
        });
    }

    /**
     * The film is translucent (alpha at most 0.38 per film, two films on a line of sight): a pixel with film differs from the
     * same pixel without by at most about 0.62 of the way to the film's colour. A hole (glass, water or film behind it not
     * drawn) shows the world behind the tank instead, far more than that. Pixels that change between two pictures without the
     * film (the water's and the sand's animation) are left out. Fails when more than 0.3 % of the picture differs too much.
     */
    private void guardCreate() {
        NativeImage a = createShots[0], b = createShots[1], c = createShots[2];
        if (a == null || b == null || c == null) {
            problem("create guard: no pictures");
            return;
        }
        int w = Math.min(a.getWidth(), b.getWidth()), h = Math.min(a.getHeight(), b.getHeight());
        int bad = 0, changed = 0;
        float[] green = {0.36F, 0.58F, 0.20F};
        for (int y = 0; y < h; y += 2) {
            for (int x = 0; x < w; x += 2) {
                int pa = a.getPixelRGBA(x, y), pb = b.getPixelRGBA(x, y), pc = c.getPixelRGBA(x, y); // ABGR
                int noise = 0;
                for (int k = 0; k < 24; k += 8) noise = Math.max(noise, Math.abs((pb >> k & 255) - (pc >> k & 255)));
                if (noise > 12) continue;
                float[] ca = {(pa & 255) / 255F, (pa >> 8 & 255) / 255F, (pa >> 16 & 255) / 255F};
                float[] cb = {(pb & 255) / 255F, (pb >> 8 & 255) / 255F, (pb >> 16 & 255) / 255F};
                float diff = 0, span = 0;
                for (int k = 0; k < 3; k++) {
                    diff = Math.max(diff, Math.abs(ca[k] - cb[k]));
                    span = Math.max(span, Math.abs(green[k] * 0.9F - cb[k]));
                }
                if (diff > 0.04F) changed++;
                if (diff > 0.12F && diff > 0.75F * Math.max(span, 0.2F)) bad++;
            }
        }
        int total = (w / 2) * (h / 2);
        double share = bad / (double) total;
        LOGGER.info("[smoketest] M67.5 create guard: {} of {} pixels changed by the film, {} beyond a translucent film ({}) {}", changed, total, bad,
                String.format(java.util.Locale.ROOT, "%.3f%%", share * 100), share <= 0.003 && changed >= total * 0.04 ? "PASS" : "FAIL");
        // the film of the chunk mesh at level 0.85 covers the walls visibly (the old film of travelling tanks, a faint entity texture
        // of its own, changed 1.4 % of this picture, the new one about 12 %)
        if (changed < total * 0.04) problem("create guard: the film is not drawn on the contraption (or too faint): " + changed + " pixels");
        if (share > 0.003) problem("create guard: holes behind the film");
        a.close();
        b.close();
        c.close();
    }

    private void assemble() {
        try {
            bearing.getClass().getMethod("assemble").invoke(bearing);
            Object moved = field(bearing.getClass(), "movedContraption").get(bearing);
            if (moved == null) problem("the bearing did not take the tank: " + field(bearing.getClass(), "lastException").get(bearing));
            LOGGER.info("[smoketest] M67.5 create: bearing assembled {}, running {}, block above now {}", moved, field(bearing.getClass(), "running").get(bearing),
                    bearing.getLevel().getBlockState(bearingPos.above()));
            angle = 0F;
            hold(0F); // at once: the bearing has no speed and would take the contraption apart next tick
        } catch (ReflectiveOperationException e) {
            problem("bearing: " + e);
        }
    }

    private void hold(float degrees) {
        try {
            field(bearing.getClass(), "assembleNextTick").setBoolean(bearing, false);
            field(bearing.getClass(), "angle").setFloat(bearing, degrees);
        } catch (ReflectiveOperationException e) {
            problem("bearing angle: " + e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState with(BlockState s, String property, Comparable value) {
        for (Property<?> p : s.getProperties()) if (p.getName().equals(property)) return s.setValue((Property) p, value);
        return s;
    }

    private static Field field(Class<?> c, String name) throws NoSuchFieldException {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try {
                Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    // ------------------------------------------------------------------ helpers

    private static void clear(ServerLevel level, BlockPos a, BlockPos b) {
        for (BlockPos q : BlockPos.betweenClosed(a, b)) {
            level.setBlock(q, q.getY() < a.getY() + 1 ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static TankBlockEntity tank(ServerLevel level, BlockPos min, int sx, int sy, int sz, BlockState floor) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlock(pos, ModBlocks.TANK.get().defaultBlockState(), 2);
        TankData t = TankMultiblock.formAll(level, blocks);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        t.setFront(Direction.NORTH);
        t.setSignVisible(false);
        t.setWaterTemp(26F);
        t.setTargetTemp(26F);
        TankInteractions.changeWater(level, c, Math.round(t.capacityMb() * 0.92));
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), floor, false);
        }
        c.markTankChanged();
        return c;
    }

    private void language(String code) {
        if (code == null || code.equals(mc.options.languageCode)) return;
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
        if (mc.screen != null) mc.screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private void problem(String what) {
        problems++;
        LOGGER.error("[smoketest] M67.5 FAIL: {}", what);
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
        if (bearing != null && !Float.isNaN(angle)) { // like the M50 scene: the server's bearing angle every tick
            angle += speed;
            float a = angle;
            server(p -> hold(a));
        }
        if (steps.isEmpty()) return true;
        Step st = steps.peek();
        if (wait < st.delay()) {
            wait++;
            return false;
        }
        steps.poll();
        wait = 0;
        st.action().run();
        return steps.isEmpty();
    }

    private void server(Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToasts().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m675_" + name + ".png", mc.getMainRenderTarget(), c -> {});
    }
}
