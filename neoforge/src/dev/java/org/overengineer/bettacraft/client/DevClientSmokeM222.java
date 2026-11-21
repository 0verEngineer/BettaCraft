package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.progression.GuideBook;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tank.plants.TankPlants;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M22.2 dev client smoke scene ({@code -Psmoketestm22_2}), screenshots {@code bettacraft_m22_2_*}:
 * <ul>
 *     <li>a gallery for the handbook: one small tank per species in front of a white wall, the same camera for all
 *     (reef fish and seahorses posed side on, snails and the reef invertebrates, every aquarium plant and coral at full
 *     size, the floating plants from above): {@code sp_<id>};</li>
 *     <li>the handbook in English and German: the navigation with every chapter and sub-chapter folded open, the start
 *     page, a species page with its generated profile and sources, "Where to find what". The language switch reloads
 *     the resources (GuideME reads the pages of the current language then; M21.5 to M21.8 showed English pages).</li>
 * </ul>
 * Checks (errors in the log): every subject was built, the guide opens with the deep navigation, the German page is German.
 */
final class DevClientSmokeM222 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static final int SPACING = 5, COLUMNS = 8, ROW = 7;

    private record Step(int delay, Runnable action) {}

    /** One picture: what, which tank (min corner), how big the tank is, how to look at it. */
    private record Subject(String id, Kind kind, Object what) {}

    private enum Kind { FISH, CRITTER, PLANT, CORAL, FLOATING }

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final List<Subject> subjects = new ArrayList<>();
    private final List<BlockPos> tanks = new ArrayList<>();
    /** Every subject of the full gallery (the seed of a plant is its place in it). */
    private final List<String> allIds = new ArrayList<>();
    private static final java.util.Set<String> ONLY = java.util.Set.of(System.getProperty("bettacraft.smoketest.gallery", "").split(","))
            .stream().filter(x -> !x.isBlank()).collect(java.util.stream.Collectors.toSet());
    private final String lang = mc.options.languageCode;
    private BlockPos base;
    private int wait;
    private CompletableFuture<Void> reload;
    private int fov, gui;
    private volatile boolean built;

    DevClientSmokeM222() {
        for (TankmateSpecies s : TankmateSpecies.values()) if (s.marine()) subjects.add(new Subject(s.id(), Kind.FISH, s));
        for (CritterSpecies s : CritterSpecies.values()) {
            if (s.isSnail() || s.marine()) subjects.add(new Subject(s.id(), Kind.CRITTER, s));
        }
        for (PlantSpecies p : PlantSpecies.values()) subjects.add(new Subject(p.id(), p.marine() ? Kind.CORAL : Kind.PLANT, p));
        for (FloatingPlantType t : FloatingPlantType.values()) if (t.grows() && !t.plantItem()) subjects.add(new Subject(t.id(), Kind.FLOATING, t));
        for (Subject s : subjects) allIds.add(s.id());
        // M28: -Pgallery=id,id,...: only these subjects (the seeds stay those of the full gallery), no handbook pictures
        if (!ONLY.isEmpty()) subjects.removeIf(s -> !ONLY.contains(s.id()));

        then(1, () -> server(p -> p.teleportTo(p.serverLevel(), p.getX(), p.getY() + 2, p.getZ() + 820, 0F, 10F)));
        then(30, () -> server(this::build));
        // building 49 tanks with their interiors stalls the server for a while: wait until it is done
        then(1, this::awaitBuilt);
        then(1, () -> {
            mc.options.hideGui = true;
            fov = mc.options.fov().get();
            mc.options.fov().set(40);
        });
        // spectator: the camera may stand anywhere (no push out of the floor, no hand, no block overlay)
        then(1, () -> server(p -> p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR)));
        then(200, () -> server(this::pose));
        for (int i = 0; i < subjects.size(); i++) {
            int k = i;
            then(1, () -> view(k));
            then(20, () -> view(k));
            then(k == 0 ? 100 : 30, () -> {
                if (subjects.get(k).kind() == Kind.FISH) server(p -> pose(p, k));
            });
            then(5, () -> shot("sp_" + subjects.get(k).id()));
        }
        then(1, () -> server(p -> p.setGameMode(net.minecraft.world.level.GameType.CREATIVE)));
        then(1, () -> mc.options.fov().set(fov));
        if (!ONLY.isEmpty()) return;
        // the handbook, English then German
        guide("en");
        then(1, () -> language("de_de"));
        then(1, this::awaitReload);
        guide("de");
        then(1, () -> language(lang));
        then(1, this::awaitReload);
        then(1, () -> mc.setScreen(null));
    }

    // ------------------------------------------------------------------ scene

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        base = p.blockPosition().offset(-COLUMNS * SPACING / 2, 0, 6);
        ServerLevel in = InteriorManager.interiorLevel(p.server);
        RandomSource r = RandomSource.create(2222);
        for (int i = 0; i < subjects.size(); i++) {
            Subject s = subjects.get(i);
            BlockPos min = base.offset((i % COLUMNS) * SPACING, 0, (i / COLUMNS) * ROW);
            tanks.add(min);
            // floor and a white wall behind the tank (the same background for every picture)
            for (int x = -2; x < 4; x++) {
                for (int z = -4; z < 3; z++) level.setBlockAndUpdate(min.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
                for (int y = 0; y < 5; y++) level.setBlockAndUpdate(min.offset(x, y, 3), Blocks.WHITE_CONCRETE.defaultBlockState());
            }
            boolean salt = s.kind() == Kind.CORAL || (s.kind() == Kind.FISH) || (s.kind() == Kind.CRITTER && ((CritterSpecies) s.what()).marine());
            int size = s.kind() == Kind.CRITTER ? 1 : 2;
            // M56: a terrarium plant stands in a dry terrarium on soil (the dry ones on sand)
            boolean land = s.kind() == Kind.PLANT && ((PlantSpecies) s.what()).land();
            TankBlockEntity c = tank(level, p, min, size, salt ? Salinity.SEA : 0F, s.kind() != Kind.FLOATING || true, !land);
            InteriorRegion reg = InteriorManager.ensureRegion(level, c);
            BlockState floor = salt ? Blocks.SAND.defaultBlockState() : land ? (((PlantSpecies) s.what()).habitat() == PlantSpecies.Habitat.DRY
                    ? Blocks.SAND : Blocks.DIRT).defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
            for (int x = 0; x < reg.sizeX(); x++)
                for (int z = 0; z < reg.sizeZ(); z++) Substrate.set(in, new BlockPos(reg.originX() + x, InteriorRegion.FLOOR_Y, reg.originZ() + z), floor, false);
            int cx = reg.sizeX() / 2, cz = reg.sizeZ() / 2;
            switch (s.kind()) {
                case FISH -> {
                    TankmateSpecies sp = (TankmateSpecies) s.what();
                    if (sp.seahorse()) {
                        for (int y = 0; y < 22; y++) in.setBlock(reg.toWorld(cx + 6, y, cz + 6), (y == 21 ? Blocks.KELP : Blocks.KELP_PLANT).defaultBlockState(), 2);
                    }
                    for (int k = 0; k < 2; k++) fish(level, c, sp, k == 0);
                }
                case CRITTER -> {
                    CritterSpecies sp = (CritterSpecies) s.what();
                    for (int k = 0; k < 5; k++) TankCritters.add(level, c, sp, CritterGenes.WILD, k % 2 == 0, 10);
                }
                case PLANT, CORAL -> {
                    PlantSpecies sp = (PlantSpecies) s.what();
                    int z = cz + 3;
                    if (sp.marine()) {
                        mound(in, reg, cx, z, 5, 5, ModBlocks.REEF_ROCK.get().defaultBlockState(), ModBlocks.LIVE_ROCK.get().defaultBlockState());
                    } else if (sp.land() && sp.form() == PlantSpecies.Form.EPIPHYTE) {
                        mound(in, reg, cx, z, 3, 3, Blocks.OAK_LOG.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState()); // M56: a piece of wood
                    } else if (sp.form() == PlantSpecies.Form.EPIPHYTE || sp.form() == PlantSpecies.Form.MOSS && !sp.land()) {
                        mound(in, reg, cx, z, 4, 4, Blocks.STONE.defaultBlockState(), Blocks.ANDESITE.defaultBlockState());
                    }
                    int y = 0;
                    while (y < reg.sizeY() && !in.getBlockState(reg.toWorld(cx, y, z)).isAir()) y++;
                    InteriorPlant pl = InteriorPlants.plantAt(in, reg, sp, cx, y, z, sp.stages(), 22200L + allIds.indexOf(s.id()));
                    if (pl == null) {
                        LOGGER.error("[smoketest] M22.2 {} not planted", sp.id());
                    } else if (sp.marine()) {
                        pl.tint = 2;
                        pl.redness = 0.67;
                        InteriorPlants.apply(in, reg, pl);
                    }
                    if (!sp.marine()) InteriorPlants.settleTint(level, c);
                }
                case FLOATING -> {
                    FloatingPlantType t = (FloatingPlantType) s.what();
                    TankWaterVolume w = new TankWaterVolume(c.getTank());
                    for (int k = 0; k < 14; k++) {
                        double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * 0.75;
                        TankPlants.add(c, t, (w.box().minX + w.box().maxX) / 2 + Math.cos(a) * d, (w.box().minZ + w.box().maxZ) / 2 + Math.sin(a) * d,
                                0.7F + r.nextFloat() * 0.3F, r.nextFloat() * 6.28F);
                    }
                    c.markTankChanged();
                }
            }
        }
        LOGGER.info("[smoketest] M22.2 gallery at {}: {} subjects", base, subjects.size());
        built = true;
    }

    /** Fish frozen side on in the middle of their tank. */
    private void pose(ServerPlayer p) {
        for (int k = 0; k < subjects.size(); k++) if (subjects.get(k).kind() == Kind.FISH) pose(p, k);
    }

    private void pose(ServerPlayer p, int k) {
        ServerLevel level = p.serverLevel();
        TankBlockEntity c = TankMultiblock.getController(level, tanks.get(k));
        if (c == null) return;
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        List<TankmateEntity> fish = TankmateBreeding.tankmates(level, c.getTank());
        if (fish.isEmpty()) LOGGER.error("[smoketest] M22.2 no fish in the tank of {}", subjects.get(k).id());
        double cx = (w.box().minX + w.box().maxX) / 2, cy = w.box().minY + (w.box().maxY - w.box().minY) * 0.45;
        for (int i = 0; i < fish.size(); i++) {
            TankmateEntity m = fish.get(i);
            boolean sea = m.species().seahorse();
            Vec3 at = new Vec3(cx + (i == 0 ? -0.35 : 0.4), cy + (i == 0 ? 0.1 : -0.2), w.box().minZ + 0.7 + i * 0.25);
            float yaw = i == 0 ? -90F : 90F;
            m.moveTo(at.x, at.y, at.z, yaw, 0F);
            SmokeFreeze.freeze(m, true);
            m.setDeltaMovement(Vec3.ZERO);
            m.setYRot(yaw);
            m.yBodyRot = yaw;
            m.yHeadRot = yaw;
            m.setPoseForSmoke(sea ? TankmateEntity.FLAG_REST : 0);
        }
    }

    private void view(int k) {
        BlockPos t = tanks.get(k);
        Subject s = subjects.get(k);
        switch (s.kind()) {
            case CRITTER -> look(t.getX() + 0.5, t.getY() + 0.5, t.getZ() - 1.05, 0F, 8F);
            case FLOATING -> look(t.getX() + 1.0, t.getY() + 4.4, t.getZ() - 0.8, 0F, 62F);
            case FISH -> look(t.getX() + 1.0, t.getY() + 0.95, t.getZ() - 1.45, 0F, 3F);
            default -> look(t.getX() + 1.0, t.getY() + 1.0, t.getZ() - 2.7, 0F, 2F);
        }
    }

    private TankmateEntity fish(ServerLevel level, TankBlockEntity c, TankmateSpecies s, boolean female) {
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        Vec3 p = new Vec3((w.box().minX + w.box().maxX) / 2, w.box().minY + 0.6, (w.box().minZ + w.box().maxZ) / 2);
        TankmateEntity e = TankmateBreeding.spawn(level, c, s, TankmateGenes.pure(s, s.variety(0)), female, LifeStage.ADULT, 0, p);
        if (e == null) LOGGER.error("[smoketest] M22.2 {} not spawned", s.id());
        else e.health().feed();
        return e;
    }

    private static TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int size, float salt, boolean light, boolean water) {
        for (int y = 0; y < size; y++) for (int z = 0; z < size; z++) for (int x = 0; x < size; x++) level.setBlockAndUpdate(min.offset(x, y, z), ModBlocks.TANK.get().defaultBlockState());
        for (int y = 0; y < size; y++) for (int z = 0; z < size; z++) for (int x = 0; x < size; x++) TankMultiblock.onTankBlockAdded(level, min.offset(x, y, z), null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        if (water) TankInteractions.changeWater(level, c, (long) size * size * size * 1000, salt);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(p.getUUID());
        c.getTank().setWaterTemp(25F);
        c.getTank().setTargetTemp(25F);
        c.markTankChanged();
        if (light) {
            for (int x = 0; x < size; x++) {
                level.setBlockAndUpdate(min.offset(x, size, size - 1), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                        .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < size - 1).setValue(TankLightBlock.LIT, true)
                        .setValue(TankLightBlock.COLOR, salt > 0 && x % 2 == 0 ? DyeColor.BLUE : DyeColor.WHITE));
            }
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

    // ------------------------------------------------------------------ handbook

    private void guide(String l) {
        if (!GuideBook.available()) {
            LOGGER.warn("[smoketest] M22.2 GuideME not loaded, no handbook pictures");
            return;
        }
        then(1, () -> {
            gui = mc.options.guiScale().get();
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
        });
        // the start page with the whole tree folded open
        open("index.md");
        then(30, () -> expand(id -> true));
        then(10, () -> shot("guide_nav_open_" + l));
        then(1, () -> scrollNav(12));
        then(10, () -> shot("guide_nav_open_" + l + "_2"));
        then(1, () -> scrollNav(12));
        then(10, () -> shot("guide_nav_open_" + l + "_3"));
        // chapters and one branch of sub-chapters (as a player opens them)
        open("tankmates/tetras.md");
        then(30, () -> expand(id -> id.getPath().equals("tankmates.md") || id.getPath().equals("tankmates/tetras.md")));
        then(10, () -> shot("guide_tetras_" + l));
        open("plants/java_fern.md");
        then(40, () -> {
            check(l);
            shot("guide_java_fern_" + l);
        });
        then(1, this::scrollPage);
        then(10, () -> shot("guide_java_fern_" + l + "_2"));
        open("saltwater/ocellaris_clownfish.md");
        then(40, () -> shot("guide_clownfish_" + l));
        then(1, this::scrollPage);
        then(10, () -> shot("guide_clownfish_" + l + "_2"));
        open("getting_started/sources.md");
        then(40, () -> shot("guide_sources_" + l));
        then(1, this::scrollPage);
        then(10, () -> shot("guide_sources_" + l + "_2"));
        open("invertebrates/snails.md");
        then(40, () -> shot("guide_snails_" + l));
        then(1, () -> {
            mc.setScreen(null);
            mc.options.guiScale().set(gui);
            mc.resizeDisplay();
        });
    }

    private void open(String page) {
        then(5, () -> org.overengineer.bettacraft.compat.guideme.GuideMECompat.open(mc.player, GuideBook.GUIDE_ID, BettaCraft.id(page)));
    }

    private void expand(java.util.function.Predicate<ResourceLocation> which) {
        if (mc.screen == null) {
            LOGGER.error("[smoketest] M22.2 the handbook did not open");
            return;
        }
        if (!org.overengineer.bettacraft.compat.guideme.DeepNavigation.expandAll(mc.screen, true, which)) {
            LOGGER.error("[smoketest] M22.2 the deep navigation could not be folded open");
        }
    }

    private void scrollNav(int notches) {
        if (mc.screen == null) return;
        for (int i = 0; i < notches; i++) mc.screen.mouseScrolled(40, mc.getWindow().getGuiScaledHeight() / 2.0, 0, -1);
    }

    private void scrollPage() {
        if (mc.screen == null) return;
        double x = mc.getWindow().getGuiScaledWidth() * 0.65, y = mc.getWindow().getGuiScaledHeight() / 2.0;
        for (int i = 0; i < 8; i++) mc.screen.mouseScrolled(x, y, 0, -3);
    }

    /** The German handbook shows German text: the Java fern page is titled "Javafarn" there. */
    private void check(String l) {
        if (mc.screen == null) return;
        String title = mc.screen.getTitle().getString();
        String narration = "";
        try {
            var m = mc.screen.getClass().getMethod("getCurrentPageId");
            LOGGER.info("[smoketest] M22.2 {} handbook page {} ({})", l, m.invoke(mc.screen), title);
        } catch (ReflectiveOperationException e) {
            LOGGER.info("[smoketest] M22.2 {} handbook: {}", l, title);
        }
        String lc = net.minecraft.client.resources.language.I18n.get("item.bettacraft.java_fern");
        if (l.equals("de") && !lc.equals("Javafarn")) LOGGER.error("[smoketest] M22.2 German language not active: {}", lc);
        LOGGER.info("[smoketest] M22.2 language {} {}", mc.options.languageCode, narration);
    }

    private void language(String code) {
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        // M22.2: like the language screen: reload the resources, so GuideME loads the pages of the new language
        reload = mc.reloadResourcePacks();
    }

    private void awaitBuilt() {
        if (!built) steps.addFirst(new Step(10, this::awaitBuilt));
    }

    private void awaitReload() {
        // inserted at the front: wait until the resource reload is done
        if (reload != null && !reload.isDone()) {
            steps.addFirst(new Step(10, this::awaitReload));
        } else {
            LOGGER.info("[smoketest] M22.2 resources reloaded for {}", mc.options.languageCode);
        }
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m22_2_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
