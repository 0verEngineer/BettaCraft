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
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.client.interior.IrisCompat;
import org.overengineer.bettacraft.co2.Co2CylinderBlock;
import org.overengineer.bettacraft.co2.Co2CylinderBlockEntity;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.equipment.AirPumpBlock;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.LedPanelBlock;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.filter.FilterPartBlock;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.genetics.BettaVarietyTable;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.progression.GuideBook;
import org.overengineer.bettacraft.reef.DosingPumpBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankStyle;
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
 * README media ({@code gradlew runClient -Psmoketestreadme}, background window, 1600x900): builds a small aquarium room on
 * a spruce cabinet counter (a planted show tank with LED panels, CO2 cylinder, multiblock filter and air pump, a reef tank
 * with corals, clownfish and seahorses and a dosing pump, a betta nano tank) and a long tank with a line of betta
 * varieties, then takes stills {@code run/screenshots/readme_*.png} and frame sequences
 * {@code readme_anim_<name>_<nnn>.png} (one frame per game tick, taken right after the frame is drawn).
 * {@code tools/assetgen/readme_media.py} turns them into {@code docs/images}. With a shader pack (add {@code -Pshaders})
 * only the stills of the room are taken, with the suffix {@code _shaders}.
 */
final class DevClientSmokeReadme {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static final Identifier WOOD = Identifier.withDefaultNamespace("spruce_planks");

    record Step(int delay, Runnable action) {}

    final Minecraft mc = Minecraft.getInstance();
    final Deque<Step> steps = new ArrayDeque<>();
    private final String lang = mc.options.languageCode;
    BlockPos base, gallery;
    private int wait;
    boolean shaders;

    // frame capture: one picture per game tick of the client level, taken after the frame is drawn
    private static DevClientSmokeReadme active;
    private static boolean listening;
    private String seqName;
    private int seqLeft, seqIndex;
    private double seqNext;

    DevClientSmokeReadme() {
        this(false);
    }

    /**
     * @param media M43 ({@code -Psmoketestm43}, {@link DevClientMedia}): in a copy of the showcase world: the time and the
     *              weather fixed, the name plates of the showcase tanks hidden, the room built beside the showcase, and after
     *              the README pictures the extra tanks (flaring, a growth time lapse, an Amazon tank) and fixed camera scenes
     *              of the showcase rows (terraria, critter colours, guppies, big tanks, the filter lines, the overview)
     */
    DevClientSmokeReadme(boolean media) {
        this.media = media;
        parts = java.util.Set.of(System.getProperty("bettacraft.media.parts", media ? "room,extras,showcase,m58" : "room")
                .toLowerCase(java.util.Locale.ROOT).split(","));
        LOGGER.info("[smoketest] README parts {}", parts);
        active = this;
        if (!listening) {
            listening = true;
            NeoForge.EVENT_BUS.addListener((RenderFrameEvent.Post e) -> {
                if (active != null) active.frame(e.getPartialTick().getGameTimeDeltaPartialTick(false));
            });
        }
        then(1, () -> {
            shaders = IrisCompat.shaderPackInUse();
            mc.getWindow().setWindowed(1600, 900);
            mc.options.hideGui = true;
            mc.options.enableVsync().set(false);
            mc.getWindow().updateVsync(false);
            mc.options.framerateLimit().set(120);
            mc.options.fov().set(70);
        });
        // M58: the pictures are for English pages (the menus, the chest titles): English while recording
        if (media) then(1, () -> language("en_us"));
        if (media) then(1, () -> server(this::prepareShowcase));
        then(1, () -> {
            building = true;
            server(this::build);
        });
        then(1, () -> server(p -> {
            p.level().setDayTime(media ? 6000 : 5000);
            reach(p, false);
        }));
        // the room: let the chunks, miniatures and plant meshes build
        if (parts.contains("room")) room();
        if (media) mediaScenes();
        // with the M58 scenes their steps come after this one: they switch the language back at their end
        if (media) then(1, () -> {
            if (m58 == null) languageBack();
        });
        else then(1, () -> mc.options.languageCode = lang);
    }

    /** M58: back to the language the client had. */
    void languageBack() {
        language(lang);
    }

    /** M58: switches the game language (the resources reload; the steps wait for it). */
    private void language(String code) {
        if (code.equals(mc.options.languageCode)) return;
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.reloadResourcePacks();
    }

    /** The README room pictures, the betta varieties, the interior and the menus. */
    private void room() {
        then(1, () -> look(base.getX() + 4.0, base.getY() + 2.5, base.getZ() - 7.6, 0F, 2F));
        then(240, () -> shot("room"));
        then(1, () -> look(base.getX() + 2.0, base.getY() + 3.0, base.getZ() - 6.0, -10F, 10F));
        then(60, () -> shot("room_left"));
        if (!Boolean.getBoolean("bettacraft.readme.roomonly")) {
            // the planted tank close up and as a frame sequence
            then(1, () -> look(base.getX() + 3.0, base.getY() + 2.45, base.getZ() - 3.3, 0F, 4F));
            then(80, () -> shot("planted"));
            then(1, () -> look(base.getX() + 3.0, base.getY() + 2.5, base.getZ() - 2.4, 0F, 3F));
            then(40, () -> { if (!shaders) sequence("planted", 110); });
            then(20, () -> {});
            // the reef tank close up and as a frame sequence
            then(1, () -> look(base.getX() + 11.0, base.getY() + 2.0, base.getZ() - 2.5, 0F, 6F));
            then(80, () -> shot("reef"));
            then(1, () -> look(base.getX() + 11.0, base.getY() + 1.95, base.getZ() - 1.7, 0F, 4F));
            then(40, () -> { if (!shaders) sequence("reef", 110); });
            then(20, () -> {});
            // the nano tank
            then(1, () -> look(base.getX() - 3.0, base.getY() + 2.1, base.getZ() - 2.0, 0F, 10F));
            then(60, () -> shot("nano"));
            // the betta varieties
            then(1, () -> look(gallery.getX() + 2.5, gallery.getY() + 0.55, gallery.getZ() - 1.5, 0F, 0F));
            then(80, () -> shot("varieties"));
            // inside the planted tank (scale 16)
            then(1, () -> server(p -> {
                TankBlockEntity c = TankMultiblock.getController(p.level(), base.offset(0, 1, 0));
                InteriorRegion r = InteriorManager.ensureRegion(p.level(), c);
                BlockPos at = r.toWorld(48, 16, 1);
                p.teleportTo(InteriorManager.interiorLevel(p.level().getServer()), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, java.util.Set.of(), 0F, 18F, true);
            }));
            then(160, () -> shot("inside"));
            then(1, () -> { if (!shaders) sequence("inside", 70); });
            then(20, () -> {});
            // the tank menu: the betta list with the genes of the first, the water tab; the handbook
            then(1, () -> mc.options.hideGui = false);
            then(1, () -> look(gallery.getX() + 2.5, gallery.getY() + 0.55, gallery.getZ() - 2.0, 0F, 0F));
            then(20, () -> openMenu(gallery));
            then(20, () -> {
                if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.selectTabForTest(1);
                else LOGGER.error("[smoketest] README no tank screen");
            });
            then(20, () -> shot("menu_bettas"));
            then(1, () -> server(ServerPlayer::closeContainer));
            then(5, () -> mc.setScreen(null));
            then(1, () -> look(base.getX() + 3.0, base.getY() + 2.0, base.getZ() - 3.0, 0F, 0F));
            then(20, () -> openMenu(base.offset(0, 1, 0)));
            then(20, () -> {
                if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.selectTabForTest(5);
            });
            then(20, () -> shot("menu_water"));
            then(1, () -> server(ServerPlayer::closeContainer));
            then(5, () -> mc.setScreen(null));
            then(1, () -> {
                if (GuideBook.available()) org.overengineer.bettacraft.compat.guideme.GuideMECompat.open(mc.player, GuideBook.GUIDE_ID,
                        BettaCraft.id("bettas/genetics.md"));
            });
            then(60, () -> shot("guide"));
            then(5, () -> mc.setScreen(null));
            then(1, () -> mc.options.hideGui = true);
        }
    }

    // ------------------------------------------------------------------ building

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        ServerLevel in = InteriorManager.interiorLevel(p.level().getServer());
        RandomSource r = RandomSource.create(1800);
        base = p.blockPosition().offset(-4, 0, 24);
        // the room: plank floor, a back wall with a dark skirting, a long spruce cabinet counter
        for (int x = -10; x <= 18; x++)
            for (int z = -14; z <= 5; z++) {
                level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.DARK_OAK_PLANKS.defaultBlockState());
                for (int y = 0; y < 8; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
        for (int x = -10; x <= 18; x++)
            for (int y = 0; y < 14; y++) level.setBlockAndUpdate(base.offset(x, y, 4), (y == 0 ? Blocks.STRIPPED_DARK_OAK_WOOD : Blocks.SMOOTH_QUARTZ).defaultBlockState());
        // black background foil behind the planted and the reef tank
        for (int x = 0; x < 6; x++) for (int y = 1; y < 4; y++) level.setBlockAndUpdate(base.offset(x, y, 3), Blocks.BLACK_CONCRETE.defaultBlockState());
        for (int x = 9; x < 15; x++) for (int y = 1; y < 3; y++) level.setBlockAndUpdate(base.offset(x, y, 3), Blocks.BLACK_CONCRETE.defaultBlockState());
        for (int x = -5; x <= 13; x++)
            for (int z = 0; z <= 2; z++) cabinet(level, base.offset(x, 0, z));
        // the planted show tank, 6 x 3 x 3
        BlockPos h = base.offset(0, 1, 0);
        TankBlockEntity hero = tank(level, h, 6, 3, 3, 0F);
        hero.getTank().setWaterTemp(26F);
        hero.getTank().setTargetTemp(26F);
        for (int x = 0; x < 6; x++)
            for (int z = 0; z < 2; z++)
                level.setBlockAndUpdate(h.offset(x, 3, z), ModBlocks.LED_PANEL.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                        .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 5).setValue(LedPanelBlock.PARTNER_POS, z == 0)
                        .setValue(TankLightBlock.LIT, true));
        scape(level, in, hero);
        // CO2 cylinder with a solenoid on the left, multiblock filter and air pump on the right
        level.setBlockAndUpdate(h.offset(-1, 0, 1), ModBlocks.CO2_CYLINDER.get().defaultBlockState()
                .setValue(Co2CylinderBlock.FACING, Direction.NORTH).setValue(Co2CylinderBlock.SOLENOID, true));
        if (level.getBlockEntity(h.offset(-1, 0, 1)) instanceof Co2CylinderBlockEntity cyl) cyl.setGas(Co2CylinderBlockEntity.capacity() * 0.8F);
        net.minecraft.world.item.Item[] media = {ModItems.FILTER_FOAM.get(), ModItems.CERAMIC_RINGS.get()};
        for (int y = 0; y < 2; y++) {
            level.setBlockAndUpdate(h.offset(7, y, 1), ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
            if (level.getBlockEntity(h.offset(7, y, 1)) instanceof org.overengineer.bettacraft.filter.FilterChamberBlockEntity ch) ch.setMedium(new ItemStack(media[y]));
        }
        level.setBlockAndUpdate(h.offset(7, 2, 1), ModBlocks.FILTER_PUMP_HEAD.get().defaultBlockState());
        level.setBlockAndUpdate(h.offset(6, 0, 1), ModBlocks.FILTER_INLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.WEST));
        level.setBlockAndUpdate(h.offset(6, 1, 1), ModBlocks.FILTER_OUTLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.WEST));
        level.setBlockAndUpdate(h.offset(6, 0, 2), ModBlocks.AIR_PUMP.get().defaultBlockState().setValue(AirPumpBlock.FACING, Direction.WEST));
        org.overengineer.bettacraft.gametest.FilterLinesGameTests.guardAll(level, h.offset(7, 2, 1)); // M40: the intake guards in the tank
        TankEquipment.invalidate(hero);
        // the reef tank, 4 x 2 x 3, blue and white lights, a dosing pump
        BlockPos rf = base.offset(9, 1, 0);
        TankBlockEntity reef = tank(level, rf, 4, 2, 3, 35F);
        for (int x = 0; x < 4; x++)
            level.setBlockAndUpdate(rf.offset(x, 2, 1), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 3).setValue(TankLightBlock.LIT, true)
                    .setValue(TankLightBlock.COLOR, x % 2 == 0 ? DyeColor.BLUE : DyeColor.WHITE));
        level.setBlockAndUpdate(rf.offset(4, 0, 1), ModBlocks.DOSING_PUMP.get().defaultBlockState().setValue(DosingPumpBlock.FACING, Direction.NORTH));
        reefScape(level, in, reef, r);
        // the betta nano tank, 2 x 2 x 2: a Nemo male, floating plants
        BlockPos nn = base.offset(-4, 1, 0);
        TankBlockEntity nano = tank(level, nn, 2, 2, 2, 0F);
        for (int x = 0; x < 2; x++)
            level.setBlockAndUpdate(nn.offset(x, 2, 1), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 1).setValue(TankLightBlock.LIT, true));
        InteriorRegion nr = region(level, nano, Blocks.SAND.defaultBlockState());
        rock(in, nr, 8, 22, 5, 5, Blocks.OAK_LOG.defaultBlockState());
        plant(in, nr, PlantSpecies.JAVA_FERN, 8, -1, 22, 31);
        plant(in, nr, PlantSpecies.CRYPT_GREEN, 22, 0, 10, 32);
        plant(in, nr, PlantSpecies.VALLISNERIA, 24, 0, 26, 33);
        plant(in, nr, PlantSpecies.ANUBIAS_NANA, 14, 0, 14, 34);
        InteriorPlants.settleTint(level, nano);
        for (int i = 0; i < 6; i++) TankPlants.add(nano, i % 2 == 0 ? FloatingPlantType.FROGBIT : FloatingPlantType.RED_ROOT_FLOATER,
                nn.getX() + 0.3 + r.nextDouble() * 1.4, nn.getZ() + 0.6 + r.nextDouble() * 1.2, 0.7F + r.nextFloat() * 0.3F, r.nextFloat() * 6.28F);
        nano.markTankChanged();
        betta(level, nano, "nemo", false);
        // the varieties: a long low tank further away (not in the room pictures), bettas in a row, still
        gallery = base.offset(0, 1, -40);
        for (int x = -4; x <= 9; x++)
            for (int y = -1; y < 5; y++) level.setBlockAndUpdate(gallery.offset(x, y, 1), Blocks.SMOOTH_QUARTZ.defaultBlockState());
        for (int x = 0; x < 5; x++) cabinet(level, gallery.offset(x, -1, 0));
        TankBlockEntity gal = tank(level, gallery, 5, 1, 1, 0F);
        InteriorRegion gr = region(level, gal, Blocks.SAND.defaultBlockState());
        for (int x = 3; x < 80; x += 5) plant(in, gr, x % 2 == 0 ? PlantSpecies.VALLISNERIA : PlantSpecies.ROTALA, x, 0, 13, 40 + x);
        InteriorPlants.settleTint(level, gal);
        for (int x = 0; x < 5; x++)
            level.setBlockAndUpdate(gallery.offset(x, 1, 0), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 4).setValue(TankLightBlock.LIT, true));
        String[] varieties = {"koi_galaxy", "black_orchid", "mustard_gas", "samurai", "avatar", "hellboy"};
        for (String v : varieties) betta(level, gal, v, false);
        server(pl -> arrangeGallery(pl.level(), varieties.length));
        if (this.media) buildExtras(level, in);
        if (this.media && (parts.contains("m58") || parts.contains("create"))) {
            m58 = new DevClientMediaM58(this);
            m58.build(p);
        }
        built = true;
        LOGGER.info("[smoketest] README scene at {}, varieties at {}", base, gallery);
    }

    // ------------------------------------------------------------------ M43: media in the showcase world

    private final boolean media;
    /** M58: which parts of the media run ({@code -Pmediaparts}): room, extras, showcase, m58, create. */
    final java.util.Set<String> parts;
    private DevClientMediaM58 m58;
    /** The build runs on the server thread (after the showcase scan in the media run): the steps wait for it. */
    private volatile boolean building, built;
    /** The extra tanks of the media run: an Amazon tank, a flaring male, a growth time lapse (min corners). */
    private BlockPos amazonAt, flareAt, growAt, shrimpAt;
    private TankBlockEntity growTank;
    private final List<InteriorPlant> growing = new ArrayList<>();
    /** Per frame of a sequence, on the server: what changes in the scene (flaring on and off, the plants growing). */
    java.util.function.BiConsumer<ServerPlayer, Integer> seqHook;
    record Box(String name, BlockPos min, BlockPos max) {
        Box union(Box o) {
            return new Box(name + "+" + o.name, new BlockPos(Math.min(min.getX(), o.min.getX()), Math.min(min.getY(), o.min.getY()), Math.min(min.getZ(), o.min.getZ())),
                    new BlockPos(Math.max(max.getX(), o.max.getX()), Math.max(max.getY(), o.max.getY()), Math.max(max.getZ(), o.max.getZ())));
        }
    }
    /** The named tanks of the showcase (their German name plates, read before they are hidden), filled on the server. */
    final Map<String, Box> named = new java.util.concurrent.ConcurrentHashMap<>();
    private final List<Box> allTanks = java.util.Collections.synchronizedList(new ArrayList<>());
    private volatile BlockPos showcaseOrigin;
    private volatile int showcaseGround;

    /**
     * The showcase world: day and clear weather that stay, no mobs; every tank of the showcase found (the chunks around the
     * spawn loaded), their names noted and the name plates hidden (the pictures are for an English page), and the player
     * moved to the free ground west of the showcase where the room is built.
     */
    private void prepareShowcase(ServerPlayer p) {
        ServerLevel level = p.level();
        var rules = level.getGameRules();
        rules.set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME, false, p.level().getServer());
        rules.set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_WEATHER, false, p.level().getServer());
        rules.set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS, false, p.level().getServer());
        level.setWeatherParameters(1_000_000, 0, false, false);
        level.setDayTime(6000);
        BlockPos spawn = level.getRespawnData().pos();
        showcaseGround = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, spawn.getX() - 40, spawn.getZ() - 40);
        showcaseOrigin = new BlockPos(spawn.getX() + 3, showcaseGround, spawn.getZ() + 6);
        int hidden = 0;
        for (int cx = (spawn.getX() - 24) >> 4; cx <= (spawn.getX() + 230) >> 4; cx++)
            for (int cz = (spawn.getZ() - 8) >> 4; cz <= (spawn.getZ() + 260) >> 4; cz++) {
                var chunk = level.getChunk(cx, cz);
                for (var be : List.copyOf(chunk.getBlockEntities().values())) {
                    if (!(be instanceof TankBlockEntity t) || !t.isController()) continue;
                    var d = t.getTank();
                    Box b = new Box(d.name() == null ? "" : d.name(), d.min(), d.max());
                    allTanks.add(b);
                    if (!b.name().isEmpty()) named.put(b.name(), b);
                    d.setSignVisible(false);
                    t.markTankChanged();
                    hidden++;
                }
            }
        LOGGER.info("[smoketest] media: {} showcase tanks, {} named, origin {}", hidden, named.size(), showcaseOrigin);
        int x = spawn.getX() - 70, z = spawn.getZ() - 40;
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);
        p.teleportTo(level, x + 0.5, y, z + 0.5, java.util.Set.of(), 0F, 0F, false);
    }

    /** The extra tanks of the media run, in a line behind the varieties tank (each against its own back wall). */
    private void buildExtras(ServerLevel level, ServerLevel in) {
        BlockPos line = base.offset(0, 1, -62);
        for (int x = -8; x <= 34; x++)
            for (int y = -1; y < 6; y++) level.setBlockAndUpdate(line.offset(x, y, 3), Blocks.SMOOTH_QUARTZ.defaultBlockState());
        // an Amazon tank, 5 x 3 x 2: discus, angelfish and cardinal tetras over sand and roots, sword plants
        amazonAt = line;
        for (int x = 0; x < 5; x++) for (int z = 0; z < 2; z++) cabinet(level, amazonAt.offset(x, -1, z));
        TankBlockEntity am = tank(level, amazonAt, 5, 3, 2, 0F);
        am.getTank().setWaterTemp(28F);
        am.getTank().setTargetTemp(28F);
        for (int x = 0; x < 5; x++)
            for (int z = 0; z < 2; z++)
                level.setBlockAndUpdate(amazonAt.offset(x, 3, z), ModBlocks.LED_PANEL.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                        .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 4).setValue(LedPanelBlock.PARTNER_POS, z == 0)
                        .setValue(TankLightBlock.LIT, true));
        for (int x = 0; x < 5; x++) for (int y = 0; y < 3; y++) level.setBlockAndUpdate(amazonAt.offset(x, y, 2), Blocks.BLACK_CONCRETE.defaultBlockState());
        InteriorRegion ar = region(level, am, Blocks.SAND.defaultBlockState());
        rock(in, ar, 22, 22, 6, 9, Blocks.STRIPPED_OAK_LOG.defaultBlockState());
        rock(in, ar, 58, 24, 5, 12, Blocks.STRIPPED_OAK_LOG.defaultBlockState());
        long seed = 4300;
        for (int x = 4; x < 78; x += 6) plant(in, ar, PlantSpecies.VALLISNERIA, x, 0, 28 - (x / 6) % 2 * 2, seed++);
        plant(in, ar, PlantSpecies.AMAZON_SWORD, 10, 0, 18, seed++);
        plant(in, ar, PlantSpecies.AMAZON_SWORD, 40, 0, 20, seed++);
        plant(in, ar, PlantSpecies.AMAZON_SWORD, 70, 0, 16, seed++);
        plant(in, ar, PlantSpecies.CRYPT_BROWN, 30, 0, 8, seed++);
        plant(in, ar, PlantSpecies.CRYPT_GREEN, 50, 0, 6, seed++);
        plant(in, ar, PlantSpecies.JAVA_FERN, 22, -1, 22, seed++);
        plant(in, ar, PlantSpecies.ANUBIAS_NANA, 58, -1, 24, seed);
        InteriorPlants.settleTint(level, am);
        RandomSource fr = RandomSource.create(43);
        for (int i = 0; i < 3; i++) fish(level, am, TankmateSpecies.DISCUS, TankmateGenes.wild(TankmateSpecies.DISCUS), i % 2 == 0, fr);
        // 1.33.3: the angelfish pair is back (their dorsal fins no longer stick out of the water at the surface)
        for (int i = 0; i < 2; i++) fish(level, am, TankmateSpecies.ANGELFISH, TankmateGenes.wild(TankmateSpecies.ANGELFISH), i % 2 == 0, fr);
        for (int i = 0; i < 8; i++) fish(level, am, TankmateSpecies.RUMMY_NOSE_TETRA, TankmateGenes.wild(TankmateSpecies.RUMMY_NOSE_TETRA), i % 2 == 0, fr);
        for (int i = 0; i < 12; i++) fish(level, am, TankmateSpecies.CARDINAL_TETRA, TankmateGenes.wild(TankmateSpecies.CARDINAL_TETRA), i % 2 == 0, fr);
        for (int i = 0; i < 3; i++) fish(level, am, TankmateSpecies.PANDA_CORY, TankmateGenes.wild(TankmateSpecies.PANDA_CORY), i % 2 == 0, fr);
        // a flaring male, 2 x 1 x 1, side on in the middle (frozen; the flaring is switched on and off while recording)
        flareAt = line.offset(12, 0, 0);
        for (int x = 0; x < 2; x++) cabinet(level, flareAt.offset(x, -1, 0));
        TankBlockEntity fl = tank(level, flareAt, 2, 1, 1, 0F);
        for (int x = 0; x < 2; x++) level.setBlockAndUpdate(flareAt.offset(x, 0, 1), Blocks.BLACK_CONCRETE.defaultBlockState());
        InteriorRegion flr = region(level, fl, Blocks.SAND.defaultBlockState());
        plant(in, flr, PlantSpecies.JAVA_FERN, 4, 0, 12, 4310);
        plant(in, flr, PlantSpecies.CRYPT_GREEN, 27, 0, 12, 4311);
        InteriorPlants.settleTint(level, fl);
        betta(level, fl, "mustard_gas", false);
        // a growth time lapse, 3 x 2 x 2: cuttings that grow while recording
        growAt = line.offset(20, 0, 0);
        for (int x = 0; x < 3; x++) for (int z = 0; z < 2; z++) cabinet(level, growAt.offset(x, -1, z));
        growTank = tank(level, growAt, 3, 2, 2, 0F);
        for (int x = 0; x < 3; x++) for (int y = 0; y < 2; y++) level.setBlockAndUpdate(growAt.offset(x, y, 2), Blocks.BLACK_CONCRETE.defaultBlockState());
        for (int x = 0; x < 3; x++)
            level.setBlockAndUpdate(growAt.offset(x, 2, 1), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 2).setValue(TankLightBlock.LIT, true));
        InteriorRegion gr = InteriorManager.ensureRegion(level, growTank);
        for (int x = 0; x < gr.sizeX(); x++)
            for (int z = 0; z < gr.sizeZ(); z++)
                Substrate.set(in, new BlockPos(gr.originX() + x, InteriorRegion.FLOOR_Y, gr.originZ() + z),
                        (z < 10 ? Blocks.SAND : ModBlocks.ACTIVE_SOIL.get()).defaultBlockState(), false);
        rock(in, gr, 14, 18, 6, 7, Blocks.STONE.defaultBlockState());
        long gs = 4400;
        int[][] spots = {{4, 26}, {12, 28}, {36, 27}, {44, 25}, {24, 24}, {30, 20}, {40, 16}, {6, 14}, {20, 6}, {28, 5}, {36, 7}, {14, 18}};
        PlantSpecies[] kinds = {PlantSpecies.VALLISNERIA, PlantSpecies.VALLISNERIA, PlantSpecies.VALLISNERIA, PlantSpecies.LUDWIGIA, PlantSpecies.ROTALA,
                PlantSpecies.ALTERNANTHERA, PlantSpecies.AMAZON_SWORD, PlantSpecies.CRYPT_FLAMINGO, PlantSpecies.MONTE_CARLO, PlantSpecies.MONTE_CARLO,
                PlantSpecies.MONTE_CARLO, PlantSpecies.JAVA_FERN};
        for (int i = 0; i < spots.length; i++) {
            int y = kinds[i] == PlantSpecies.JAVA_FERN ? topAt(in, gr, spots[i][0], spots[i][1]) : 0;
            InteriorPlant gp = InteriorPlants.plantAt(in, gr, kinds[i], spots[i][0], y, spots[i][1], 0, gs++);
            if (gp != null) growing.add(gp);
            else LOGGER.warn("[smoketest] media growth {} not planted", kinds[i].id());
        }
        for (int i = 0; i < 8; i++) fish(level, growTank, TankmateSpecies.EMBER_TETRA, TankmateGenes.wild(TankmateSpecies.EMBER_TETRA), i % 2 == 0, fr);
        for (int i = 0; i < 5; i++) TankCritters.add(level, growTank, CritterSpecies.NEOCARIDINA, CritterGenes.pure(0, 0.7F), i % 2 == 0, 10);
        // a shrimp tank, 3 x 1 x 1: every Neocaridina colour, bee shrimp, nerites and a dwarf crayfish on moss and wood
        shrimpAt = line.offset(28, 0, 0);
        for (int x = 0; x < 3; x++) cabinet(level, shrimpAt.offset(x, -1, 0));
        TankBlockEntity sh = tank(level, shrimpAt, 3, 1, 1, 0F);
        for (int x = 0; x < 3; x++) level.setBlockAndUpdate(shrimpAt.offset(x, 0, 1), Blocks.BLACK_CONCRETE.defaultBlockState());
        for (int x = 0; x < 3; x++)
            level.setBlockAndUpdate(shrimpAt.offset(x, 1, 0), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 2).setValue(TankLightBlock.LIT, true));
        InteriorRegion sr = region(level, sh, ModBlocks.ACTIVE_SOIL.get().defaultBlockState());
        rock(in, sr, 14, 10, 5, 4, Blocks.OAK_LOG.defaultBlockState());
        rock(in, sr, 36, 11, 4, 3, Blocks.STONE.defaultBlockState());
        plant(in, sr, PlantSpecies.JAVA_MOSS, 14, -1, 10, 4500);
        plant(in, sr, PlantSpecies.JAVA_MOSS, 12, -1, 8, 4501);
        plant(in, sr, PlantSpecies.ANUBIAS_NANA, 36, -1, 11, 4502);
        plant(in, sr, PlantSpecies.CRYPT_GREEN, 26, 0, 13, 4503);
        plant(in, sr, PlantSpecies.CRYPT_BROWN, 44, 0, 12, 4504);
        InteriorPlants.settleTint(level, sh);
        int colours = CritterSpecies.NEOCARIDINA.loci().length;
        for (int l = 0; l < colours; l++)
            for (int i = 0; i < 2; i++) TankCritters.add(level, sh, CritterSpecies.NEOCARIDINA, CritterGenes.pure(l, 0.8F), i == 0, 10);
        for (int i = 0; i < 3; i++) TankCritters.add(level, sh, CritterSpecies.CARIDINA, CritterGenes.pure(Math.min(i, CritterSpecies.CARIDINA.loci().length - 1), 0.8F), i % 2 == 0, 10);
        for (int i = 0; i < 2; i++) TankCritters.add(level, sh, CritterSpecies.ZEBRA_NERITE, CritterGenes.WILD, i == 0, 10);
        TankCritters.add(level, sh, CritterSpecies.DWARF_CRAYFISH, CritterGenes.WILD, true, 10);
    }

    /** The flaring male side on in the middle of its tank, frozen. */
    private void arrangeFlare(ServerLevel level) {
        for (BettaEntity b : level.getEntitiesOfClass(BettaEntity.class, new AABB(flareAt).inflate(2, 1, 1))) {
            b.snapTo(flareAt.getX() + 1.0, flareAt.getY() + 0.48, flareAt.getZ() + 0.5, 90F, 0F);
            b.setYBodyRot(90F);
            b.setYHeadRot(90F);
            SmokeFreeze.freeze(b, true);
        }
    }

    /** The media scenes after the README pictures: the extra tanks, then the showcase rows. */
    private void mediaScenes() {
        if (parts.contains("extras")) extraScenes();
        if (parts.contains("showcase")) showcaseScenes();
        // M58: the new scenes (built with the room, so their steps are added once the build is done)
        if (parts.contains("m58") || parts.contains("create")) then(1, () -> {
            if (m58 != null) m58.scenes();
        });
    }

    private void extraScenes() {
        // the Amazon tank
        then(1, () -> lookAt(new Box("amazon", amazonAt.below(), amazonAt.offset(4, 2, 1)), 0F, 6F, 0.85));
        then(120, () -> shot("amazon"));
        then(1, () -> { if (!shaders) sequence("amazon", 100); });
        then(20, () -> {});
        // the flaring male: zoomed in, flaring on and off
        then(1, () -> server(p -> arrangeFlare(p.level())));
        then(1, () -> {
            mc.options.fov().set(30);
            look(flareAt.getX() + 1.0, flareAt.getY() + 0.52, flareAt.getZ() - 1.25, 0F, 1F);
        });
        then(60, () -> server(p -> flareAll(p.level(), 40)));
        then(20, () -> shot("flare"));
        then(60, () -> {
            if (shaders) return;
            seqHook = (p, i) -> { if (i >= 12 && i < 52 || i >= 76) flareAll(p.level(), 2); };
            sequence("flare", 100);
        });
        then(20, () -> {
            seqHook = null;
            mc.options.fov().set(70);
        });
        // the growth time lapse: every plant from its cutting to full size, one stage after the other
        then(1, () -> lookAt(new Box("grow", growAt.below(), growAt.offset(2, 1, 1)), 0F, 6F, 0.72));
        then(120, () -> {
            if (shaders) return;
            seqHook = (p, i) -> grow(p.level(), i, 70);
            sequence("grow", 96);
        });
        then(20, () -> {
            seqHook = null;
            shot("grow_done");
        });
        // the shrimp tank: zoomed in a little
        then(1, () -> {
            mc.options.fov().set(40);
            look(shrimpAt.getX() + 1.5, shrimpAt.getY() + 0.62, shrimpAt.getZ() - 1.75, 0F, 7F);
        });
        then(120, () -> shot("shrimp"));
        then(1, () -> { if (!shaders) sequence("shrimp", 80); });
        then(20, () -> mc.options.fov().set(70));
    }

    private void showcaseScenes() {
        then(1, () -> showcaseShot("terraria", 0F, 12F, 0.85, "Paludarium: Schmuckschildkröten", "Terrarium: Leopardgeckos"));
        then(1, () -> showcaseShot("paludarium", 0F, 10F, 0.8, "Paludarium: Schmuckschildkröten"));
        then(1, () -> showcaseShot("pets", 0F, 12F, 0.85, "Minecraft-Haustiere"));
        then(1, () -> showcaseShot("mobs_in_glass", 0F, 10F, 0.8, "Mobs im Glas: Spinne, Skelett"));
        then(1, () -> showcaseShot("sea_giants", 0F, 14F, 0.8, "Riesen des Meeres"));
        then(1, () -> showcaseShot("fresh_giants", 0F, 14F, 0.8, "Süßwasser-Riesen"));
        then(1, () -> showcaseShot("filter_lines", -25F, 38F, 0.85, "Ein Filter, zwei Becken", "Rohrbündel mit vier Leitungen"));
        then(1, () -> {
            if (showcaseOrigin == null) return;
            look(showcaseOrigin.getX() + 70, showcaseGround + 44, showcaseOrigin.getZ() - 20, 0F, 50F);
        });
        then(200, () -> shot("showcase_overview"));
    }

    /** The male of the flare tank flares for {@code ticks} more ticks. */
    private void flareAll(ServerLevel level, int ticks) {
        for (BettaEntity b : level.getEntitiesOfClass(BettaEntity.class, new AABB(flareAt).inflate(2, 1, 1)))
            b.flare(level.getGameTime() + ticks);
    }

    /** Frame {@code i} of the time lapse: every plant at the stage of its share of {@code frames} (staggered a little). */
    private void grow(ServerLevel level, int i, int frames) {
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion r = InteriorManager.ensureRegion(level, growTank);
        for (int k = 0; k < growing.size(); k++) {
            InteriorPlant gp = growing.get(k);
            double t = Math.max(0, Math.min(1, (i - k * 1.5) / (frames - growing.size() * 1.5)));
            int stage = (int) Math.round(t * gp.species.stages());
            if (stage != gp.target) {
                gp.target = stage;
                InteriorPlants.apply(in, r, gp);
            }
        }
        if (i % 10 == 0) InteriorPlants.settleTint(level, growTank);
    }

    /** A still of named showcase tanks (union of their boxes), after the chunks and miniatures have loaded. */
    private void showcaseShot(String name, float yaw, float pitch, double dist, String... tanks) {
        Box box = null;
        for (String t : tanks) {
            Box b = named.get(t);
            if (b == null) {
                LOGGER.error("[smoketest] media: no showcase tank named {}", t);
                continue;
            }
            box = box == null ? b : box.union(b);
        }
        if (box == null) return;
        Box fb = box;
        steps.addFirst(new Step(140, () -> shot(name)));
        lookAt(new Box(fb.name(), fb.min().below(), fb.max()), yaw, pitch, dist);
    }

    /** A still of {@code count} tanks of a showcase row from the {@code from}th on (the row found by its header plate). */
    private void showcaseRowShot(String name, String header, int from, int count) {
        Box head = null;
        for (var e : named.entrySet()) if (e.getKey().startsWith(header)) head = e.getValue();
        if (head == null) {
            LOGGER.error("[smoketest] media: no showcase row {}", header);
            return;
        }
        Box h = head;
        List<Box> row;
        synchronized (allTanks) {
            row = new ArrayList<>(allTanks.stream().filter(b -> b.min().getZ() == h.min().getZ() && b.min().getX() > h.min().getX())
                    .sorted(java.util.Comparator.comparingInt(b -> b.min().getX())).toList());
        }
        if (row.size() < from + count) {
            LOGGER.error("[smoketest] media: row {} has {} tanks", header, row.size());
            return;
        }
        Box box = row.get(from);
        for (int i = from + 1; i < from + count; i++) box = box.union(row.get(i));
        Box fb = box;
        steps.addFirst(new Step(140, () -> shot(name)));
        lookAt(new Box(fb.name(), fb.min().below(), fb.max()), 0F, 10F, 1.0);
    }

    /**
     * The camera on a box of blocks (min and max inclusive): looking along {@code yaw} / {@code pitch} at its centre, far
     * enough that the box fills about four fifths of the 16:9 picture at the field of view of 70 ({@code dist} scales it).
     */
    void lookAt(Box b, float yaw, float pitch, double dist) {
        double w = b.max().getX() - b.min().getX() + 1, h = b.max().getY() - b.min().getY() + 1, d = b.max().getZ() - b.min().getZ() + 1;
        double cx = b.min().getX() + w / 2, cy = b.min().getY() + h / 2, cz = b.min().getZ() + d / 2;
        double tanV = Math.tan(Math.toRadians(35)), tanH = tanV * 16 / 9;
        double r = Math.max(w / 2 / (tanH * 0.84), h / 2 / (tanV * 0.84)) + d / 2;
        r *= dist;
        double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
        double dx = -Math.sin(yr) * Math.cos(pr), dy = -Math.sin(pr), dz = Math.cos(yr) * Math.cos(pr);
        look(cx - dx * r, cy - dy * r, cz - dz * r, yaw, pitch);
    }

    /** The bettas of the varieties tank in a row, side on, alternating directions, frozen. */
    private void arrangeGallery(ServerLevel level, int n) {
        List<BettaEntity> fish = level.getEntitiesOfClass(BettaEntity.class, new AABB(gallery).inflate(6, 1, 1));
        fish.sort(java.util.Comparator.comparingInt(BettaEntity::getId));
        for (int i = 0; i < fish.size(); i++) {
            BettaEntity b = fish.get(i);
            float yaw = i % 2 == 0 ? 90F : -90F;
            b.snapTo(gallery.getX() + 0.45 + i * 0.82, gallery.getY() + 0.42 + (i % 3) * 0.08, gallery.getZ() + 0.5, yaw, 0F);
            b.setYBodyRot(yaw);
            b.setYHeadRot(yaw);
            SmokeFreeze.freeze(b, true);
            org.overengineer.bettacraft.genetics.MagnifierItem.analyse(b);
        }
        if (fish.size() != n) LOGGER.error("[smoketest] README {} bettas in the varieties tank, expected {}", fish.size(), n);
    }

    /** The planted tank: a stone hill on the left, a root on the right, a carpet in front, stem plants and Vallisneria behind. */
    private void scape(ServerLevel level, ServerLevel in, TankBlockEntity c) {
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++)
            for (int z = 0; z < r.sizeZ(); z++)
                Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z),
                        (z < 14 ? Blocks.SAND : ModBlocks.ACTIVE_SOIL.get()).defaultBlockState(), false);
        rock(in, r, 20, 26, 9, 11, Blocks.STONE.defaultBlockState());
        rock(in, r, 28, 20, 5, 6, Blocks.ANDESITE.defaultBlockState());
        rock(in, r, 72, 28, 6, 6, Blocks.OAK_LOG.defaultBlockState());
        long seed = 1800;
        for (int x = 4; x < 94; x += 7) plant(in, r, PlantSpecies.VALLISNERIA, x, 0, 43 - (x / 7) % 2 * 2, seed++);
        plant(in, r, PlantSpecies.ROTALA, 38, 0, 38, seed++);
        plant(in, r, PlantSpecies.ROTALA, 44, 0, 40, seed++);
        plant(in, r, PlantSpecies.ROTALA, 41, 0, 34, seed++);
        plant(in, r, PlantSpecies.LUDWIGIA, 84, 0, 37, seed++);
        plant(in, r, PlantSpecies.LUDWIGIA, 89, 0, 32, seed++);
        plant(in, r, PlantSpecies.ALTERNANTHERA, 54, 0, 32, seed++);
        plant(in, r, PlantSpecies.ALTERNANTHERA, 60, 0, 36, seed++);
        plant(in, r, PlantSpecies.AMAZON_SWORD, 8, 0, 34, seed++);
        plant(in, r, PlantSpecies.TIGER_LOTUS, 50, 0, 22, seed++);
        plant(in, r, PlantSpecies.CRYPT_GREEN, 10, 0, 14, seed++);
        plant(in, r, PlantSpecies.CRYPT_BROWN, 86, 0, 18, seed++);
        plant(in, r, PlantSpecies.CRYPT_FLAMINGO, 62, 0, 18, seed++);
        for (int x = 22; x < 80; x += 6) plant(in, r, PlantSpecies.MONTE_CARLO, x, 0, 6 + (x % 4), seed++);
        plant(in, r, PlantSpecies.JAVA_FERN, 20, -1, 26, seed++);
        plant(in, r, PlantSpecies.BUCE_BROWNIE_RED, 28, -1, 20, seed++);
        plant(in, r, PlantSpecies.ANUBIAS_NANA, 72, -1, 28, seed++);
        plant(in, r, PlantSpecies.JAVA_MOSS, 67, -1, 25, seed++);
        plant(in, r, PlantSpecies.BUCE_GREEN_WAVY, 76, -1, 24, seed);
        InteriorPlants.settleTint(level, c);
        for (int k = 0; k < 3; k++) in.setBlock(r.toWorld(90 - k * 3, 0, 44), ModBlocks.AIR_STONE.get().defaultBlockState(), 3);
        BlockPos min = c.getTank().min();
        RandomSource fr = RandomSource.create(7);
        for (int i = 0; i < 9; i++) {
            TankPlants.add(c, i % 3 == 0 ? FloatingPlantType.SALVINIA : FloatingPlantType.FROGBIT, min.getX() + 4.0 + fr.nextDouble() * 1.8,
                    min.getZ() + 0.6 + fr.nextDouble() * 2.0, 0.7F + fr.nextFloat() * 0.3F, fr.nextFloat() * 6.28F);
        }
        c.markTankChanged();
        betta(level, c, "koi_galaxy", false);
        for (int i = 0; i < 10; i++) fish(level, c, TankmateSpecies.NEON_TETRA, TankmateGenes.wild(TankmateSpecies.NEON_TETRA), i % 2 == 0, fr);
        for (int i = 0; i < 6; i++) fish(level, c, TankmateSpecies.HARLEQUIN_RASBORA, TankmateGenes.wild(TankmateSpecies.HARLEQUIN_RASBORA), i % 2 == 0, fr);
        for (int i = 0; i < 4; i++) fish(level, c, TankmateSpecies.PANDA_CORY, TankmateGenes.wild(TankmateSpecies.PANDA_CORY), i % 2 == 0, fr);
        for (int i = 0; i < 2; i++) fish(level, c, TankmateSpecies.OTOCINCLUS, TankmateGenes.wild(TankmateSpecies.OTOCINCLUS), i % 2 == 0, fr);
        for (int i = 0; i < 8; i++) TankCritters.add(level, c, CritterSpecies.NEOCARIDINA, CritterGenes.pure(0, 0.7F), i % 2 == 0, 10);
        for (int i = 0; i < 3; i++) TankCritters.add(level, c, CritterSpecies.ZEBRA_NERITE, CritterGenes.WILD, i % 2 == 0, 10);
    }

    private void reefScape(ServerLevel level, ServerLevel in, TankBlockEntity c, RandomSource r) {
        InteriorRegion ir = region(level, c, Blocks.SAND.defaultBlockState());
        rock(in, ir, 36, 34, 10, 10, ModBlocks.REEF_ROCK.get().defaultBlockState());
        rock(in, ir, 12, 32, 8, 6, ModBlocks.LIVE_ROCK.get().defaultBlockState());
        rock(in, ir, 54, 30, 7, 7, ModBlocks.LIVE_ROCK.get().defaultBlockState());
        rock(in, ir, 20, 14, 5, 4, ModBlocks.LIVE_ROCK.get().defaultBlockState());
        long seed = 5000;
        coral(in, ir, PlantSpecies.ACROPORA, 36, 34, seed++);
        coral(in, ir, PlantSpecies.HAMMER_CORAL, 28, 30, seed++);
        coral(in, ir, PlantSpecies.LOBOPHYLLIA, 44, 30, seed++);
        coral(in, ir, PlantSpecies.LEATHER_CORAL, 12, 32, seed++);
        coral(in, ir, PlantSpecies.GREEN_STAR_POLYPS, 54, 30, seed++);
        coral(in, ir, PlantSpecies.MUSHROOM_CORAL, 50, 16, seed++);
        coral(in, ir, PlantSpecies.ZOANTHUS, 8, 26, seed++);
        coral(in, ir, PlantSpecies.BUBBLE_TIP_ANEMONE, 20, 14, seed);
        for (int x = 30; x < 62; x++)
            for (int z = 4; z < 14; z++) {
                int k = (x * 7 + z * 13) % 9;
                if (k == 0) in.setBlock(ir.toWorld(x, 0, z), Blocks.TALL_SEAGRASS.defaultBlockState(), 2);
                else if (k == 4) in.setBlock(ir.toWorld(x, 0, z), Blocks.SEAGRASS.defaultBlockState(), 2);
            }
        for (int y = 0; y < 24; y++) in.setBlock(ir.toWorld(60, y, 10), (y == 23 ? Blocks.KELP : Blocks.KELP_PLANT).defaultBlockState(), 2);
        TankEquipment.invalidate(c);
        for (int i = 0; i < 2; i++) fish(level, c, TankmateSpecies.OCELLARIS_CLOWNFISH, TankmateGenes.wild(TankmateSpecies.OCELLARIS_CLOWNFISH), i == 0, r);
        for (int i = 0; i < 4; i++) fish(level, c, TankmateSpecies.GREEN_CHROMIS, TankmateGenes.wild(TankmateSpecies.GREEN_CHROMIS), i % 2 == 0, r);
        fish(level, c, TankmateSpecies.YELLOW_TANG, TankmateGenes.wild(TankmateSpecies.YELLOW_TANG), false, r);
        fish(level, c, TankmateSpecies.LINED_SEAHORSE, TankmateGenes.pure(TankmateSpecies.LINED_SEAHORSE, "yellow"), true, r);
        fish(level, c, TankmateSpecies.LINED_SEAHORSE, TankmateGenes.wild(TankmateSpecies.LINED_SEAHORSE), false, r);
        TankCritters.add(level, c, CritterSpecies.CLEANER_SHRIMP, CritterGenes.WILD, true, 10);
        for (int i = 0; i < 2; i++) TankCritters.add(level, c, CritterSpecies.HERMIT_CRAB, CritterGenes.WILD, i == 0, 10);
    }

    // ------------------------------------------------------------------ helpers

    static void cabinet(ServerLevel level, BlockPos at) {
        level.setBlockAndUpdate(at, ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
        if (level.getBlockEntity(at) instanceof CabinetBlockEntity cab) cab.setWood(WOOD);
        level.setBlockAndUpdate(at, Block.updateFromNeighbourShapes(level.getBlockState(at), level, at));
    }

    static TankBlockEntity tank(ServerLevel level, BlockPos min, int sx, int sy, int sz, float salt) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) {
            level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
            if (level.getBlockEntity(pos) instanceof TankBlockEntity be) be.setStyle(TankStyle.CLEAN);
        }
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        // the cabinets below join into one counter with the tank on top
        for (int x = -1; x <= sx; x++)
            for (int z = -1; z <= sz; z++) {
                BlockPos cp = min.offset(x, -1, z);
                if (level.getBlockState(cp).is(ModBlocks.CABINET.get())) level.setBlockAndUpdate(cp, Block.updateFromNeighbourShapes(level.getBlockState(cp), level, cp));
            }
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, (long) blocks.size() * 1000, salt);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setWaterTemp(salt > 20 ? 26F : 25F);
        c.getTank().setTargetTemp(salt > 20 ? 26F : 25F);
        c.getTank().setWaterQuality(1F);
        c.markTankChanged();
        return c;
    }

    static InteriorRegion region(ServerLevel level, TankBlockEntity c, BlockState floor) {
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++)
            for (int z = 0; z < r.sizeZ(); z++) Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), floor, false);
        return r;
    }

    static void rock(ServerLevel in, InteriorRegion r, int cx, int cz, int rad, int h, BlockState s) {
        for (int x = -rad; x <= rad; x++)
            for (int z = -rad; z <= rad; z++) {
                double d = Math.sqrt(x * x + z * z) / rad;
                if (d > 1.0 || cx + x < 0 || cz + z < 0 || cx + x >= r.sizeX() || cz + z >= r.sizeZ()) continue;
                int top = (int) Math.round(h * (1 - d * d * 0.7));
                for (int y = 0; y < top; y++) in.setBlock(r.toWorld(cx + x, y, cz + z), s, 2);
            }
    }

    static int topAt(ServerLevel in, InteriorRegion r, int x, int z) {
        int y = 0;
        while (y < r.sizeY() && !in.getBlockState(r.toWorld(x, y, z)).isAir()) y++;
        return y;
    }

    /** Plants {@code s} fully grown at (x, z); y < 0: on top of whatever stands there. */
    static void plant(ServerLevel in, InteriorRegion r, PlantSpecies s, int x, int y, int z, long seed) {
        int py = y < 0 ? topAt(in, r, x, z) : y;
        if (InteriorPlants.plantAt(in, r, s, x, py, z, s.stages(), seed) == null) LOGGER.warn("[smoketest] README {} not planted at {} {} {}", s.id(), x, py, z);
    }

    static void coral(ServerLevel in, InteriorRegion r, PlantSpecies s, int x, int z, long seed) {
        InteriorPlant p = InteriorPlants.plantAt(in, r, s, x, topAt(in, r, x, z), z, s.stages(), seed);
        if (p == null) {
            LOGGER.warn("[smoketest] README {} not placed", s.id());
            return;
        }
        p.tint = 3;
        p.redness = 1;
        InteriorPlants.apply(in, r, p);
    }

    private static Map<String, String> example(String variety) {
        for (var v : BettaVarietyTable.VARIETIES) if (v.id().equals(variety)) return v.example();
        throw new IllegalArgumentException(variety);
    }

    static void betta(ServerLevel level, TankBlockEntity c, String variety, boolean female) {
        BettaData d = BettaData.create(BettaGenome.of(example(variety), BettaGenome.DEFAULT), female, level.getGameTime());
        Component err = TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d),
                ModEntities.BETTA.get(), null, 0L);
        if (err != null) LOGGER.error("[smoketest] README betta {}: {}", variety, err.getString());
    }

    static void fish(ServerLevel level, TankBlockEntity c, TankmateSpecies s, TankmateGenes genes, boolean female, RandomSource r) {
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        Vec3 p = w.clamp(new Vec3(w.box().minX + r.nextDouble() * (w.box().maxX - w.box().minX), w.box().minY + 0.3 + r.nextDouble() * 0.6,
                w.box().minZ + r.nextDouble() * (w.box().maxZ - w.box().minZ)), 0.35, 0.1, 0.2);
        var e = TankmateBreeding.spawn(level, c, s, genes, female, LifeStage.ADULT, 0, p);
        if (e == null) LOGGER.error("[smoketest] README {} not spawned", s.id());
        else e.health().feed();
    }

    /** No info overlay (Jade) in the pictures. */
    static void reach(ServerPlayer p, boolean normal) {
        var b = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE);
        var e = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE);
        if (b != null) b.setBaseValue(normal ? 4.5 : 0.0);
        if (e != null) e.setBaseValue(normal ? 3.0 : 0.0);
    }

    void openMenu(BlockPos at) {
        server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.level(), at);
            if (c == null) return;
            org.overengineer.bettacraft.platform.Platform.get().openMenu(p, new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        });
    }

    void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> {
            if (InteriorManager.isInterior(p.level())) p.teleportTo(p.level().getServer().overworld(), x, eyeY - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, true);
            else p.teleportTo(p.level(), x, eyeY - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, false);
        });
    }

    void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    boolean tick() {
        if (mc.player != null && !steps.isEmpty()) {
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (m58 != null) m58.tickHolds();
        if (mc.getOverlay() != null) return false; // a resource reload (the language)
        if (seqLeft > 0) return false;
        if (building && !built) return false;
        if (steps.isEmpty()) {
            active = null;
            return true;
        }
        Step s = steps.peek();
        if (wait < s.delay()) {
            wait++;
            return false;
        }
        steps.poll();
        wait = 0;
        s.action().run();
        return false;
    }

    void server(java.util.function.Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    void shot(String name) {
        mc.getToastManager().clear();
        String file = "readme_" + name + (shaders ? "_shaders" : "") + ".png";
        Screenshot.grab(mc.gameDirectory, file, mc.getMainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }

    /** Starts a frame sequence of {@code frames} pictures, one per game tick. */
    void sequence(String name, int frames) {
        // quarter speed while recording: four times as many drawn frames per game tick, so every picture is close to its
        // tick (the animations of the mod run on game time); the steps wait until the sequence is complete
        server(p -> p.level().getServer().tickRateManager().setTickRate(5F));
        seqName = name;
        seqLeft = frames;
        seqIndex = 0;
        seqNext = -1;
    }

    private void frame(float partial) {
        if (seqLeft <= 0 || mc.level == null) return;
        double t = mc.level.getGameTime() + partial;
        if (seqNext < 0) seqNext = Math.ceil(t) + 1;
        if (t < seqNext) return;
        seqNext += 1;
        if (t > seqNext) seqNext = Math.floor(t) + 1; // dropped behind: keep the pace of the game
        mc.getToastManager().clear();
        if (seqHook != null) {
            var hook = seqHook;
            int i = seqIndex;
            server(p -> hook.accept(p, i));
        }
        String file = String.format(java.util.Locale.ROOT, "readme_anim_%s_%03d.png", seqName, seqIndex++);
        Screenshot.grab(mc.gameDirectory, file, mc.getMainRenderTarget(), 1, c -> {});
        if (--seqLeft == 0) server(p -> p.level().getServer().tickRateManager().setTickRate(20F));
        if (seqLeft == 0) LOGGER.info("[smoketest] README sequence {} done ({} frames)", seqName, seqIndex);
    }
}
