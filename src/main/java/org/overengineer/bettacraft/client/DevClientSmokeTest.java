package org.overengineer.bettacraft.client;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.overengineer.bettacraft.client.screen.TankScreen;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.equipment.FeederBlockEntity;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.SignPosition;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorSync;
import org.overengineer.bettacraft.tank.interior.InteriorWorldOps;
import org.overengineer.bettacraft.tank.menu.TankMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development-only visual smoke test ({@code -Dbettacraft.smoketest.world=true}, {@code gradlew runClient -Psmoketestworld}):
 * creates a flat creative world, builds a decorated tank with bettas, light bar, feeder and name sign, takes
 * screenshots of the tank and of the tank menu (run/screenshots) and quits. Never active in normal play.
 */
final class DevClientSmokeTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static int ticks;
    private static boolean created;
    private static BlockPos tankMin;
    private static BlockPos showMin;
    private static BlockPos breedMin;
    private static BlockPos bigMin;
    private static volatile InteriorRegion bigRegion;
    private static boolean bigDecorated;

    private DevClientSmokeTest() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Opening e) -> {
            if (!created && e.getNewScreen() instanceof TitleScreen) {
                created = true;
                Minecraft mc = Minecraft.getInstance();
                GameRules rules = new GameRules();
                rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
                rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
                LevelSettings settings = new LevelSettings("bettacraft_smoketest_" + System.currentTimeMillis(), GameType.CREATIVE, false,
                        Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, new WorldOptions(42L, false, false),
                        ra -> ra.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                        e.getNewScreen());
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getSingleplayerServer() == null) return;
        ticks++;
        mc.options.pauseOnLostFocus = false;
        if (ticks > 60) mc.player.getAbilities().flying = true; // hover at the camera positions
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        try {
            if (bigRegion != null && !bigDecorated && bigRegion.built()) {
                bigDecorated = true;
                mc.getSingleplayerServer().execute(() -> {
                    ServerLevel in = InteriorManager.interiorLevel(mc.getSingleplayerServer());
                    long t0 = System.nanoTime();
                    decorate(in, bigRegion, 16);
                    InteriorSync.markAllDirty(bigRegion.id());
                    LOGGER.info("[smoketest] big tank 16x16x16 decorated in {} ms", (System.nanoTime() - t0) / 1_000_000);
                });
            }
            switch (ticks) {
                case 60 -> mc.getSingleplayerServer().execute(DevClientSmokeTest::buildScene);
                case 290 -> mc.getSingleplayerServer().execute(() -> {
                    // close to the front glass: the miniature decoration
                    ServerPlayer p = player(mc);
                    p.teleportTo(p.serverLevel(), tankMin.getX() + 2.0, tankMin.getY() + 2.3, tankMin.getZ() - 2.0, 0F, 32F);
                });
                case 330 -> shot(mc, "bettacraft_m5_tank");
                case 340 -> mc.getSingleplayerServer().execute(() -> {
                    // M5: all named varieties side by side
                    ServerPlayer p = player(mc);
                    p.teleportTo(p.serverLevel(), typesMin.getX() + 4.0, typesMin.getY() + 1.0 - 1.62, typesMin.getZ() - 3.4, 0F, 0F);
                });
                case 400 -> shot(mc, "bettacraft_m5_types");
                case 410 -> mc.getSingleplayerServer().execute(() -> {
                    ServerPlayer p = player(mc);
                    p.teleportTo(p.serverLevel(), typesMin.getX() + 1.6, typesMin.getY() + 0.95 - 1.62, typesMin.getZ() - 1.25, 0F, 0F);
                });
                case 450 -> shot(mc, "bettacraft_m5_types_closeup");
                case 455 -> mc.getSingleplayerServer().execute(() -> {
                    ServerPlayer p = player(mc);
                    p.teleportTo(p.serverLevel(), typesMin.getX() + 6.4, typesMin.getY() + 0.95 - 1.62, typesMin.getZ() - 1.25, 0F, 0F);
                });
                case 495 -> shot(mc, "bettacraft_m5_types_closeup2");
                case 500 -> mc.getSingleplayerServer().execute(() -> {
                    // fin forms: 8 long-finned tails and 4 plakat forms in the same colours
                    ServerPlayer p = player(mc);
                    p.teleportTo(p.serverLevel(), formsMin.getX() + 3.0, formsMin.getY() + 1.0 - 1.62, formsMin.getZ() - 2.9, 0F, 0F);
                });
                case 550 -> shot(mc, "bettacraft_m5_forms");
                case 555 -> mc.getSingleplayerServer().execute(() -> {
                    // stages, flaring male and the female in breeding colours (bars)
                    ServerPlayer p = player(mc);
                    p.teleportTo(p.serverLevel(), showMin.getX() + 2.5, showMin.getY() - 1.15, showMin.getZ() - 2.1, 0F, 2F);
                });
                case 600 -> shot(mc, "bettacraft_m5_stages_bars");
                case 601 -> mc.getSingleplayerServer().execute(() -> {
                    // close-up of the female in breeding colours
                    ServerPlayer p = player(mc);
                    p.teleportTo(p.serverLevel(), showMin.getX() + 4.4, showMin.getY() + 0.42 - 1.62, showMin.getZ() - 0.55, 0F, 0F);
                });
                case 630 -> shot(mc, "bettacraft_m5_female_bars");
                case 635 -> mc.getSingleplayerServer().execute(() -> {
                    // straight at one variety (Jade shows the type)
                    ServerPlayer p = player(mc);
                    p.teleportTo(p.serverLevel(), typesMin.getX() + 1.5, typesMin.getY() + 0.42 - 1.62, typesMin.getZ() - 0.7, 0F, 0F);
                });
                case 660 -> shot(mc, "bettacraft_m5_jade_betta");
                case 665 -> mc.getSingleplayerServer().execute(() -> {
                    ServerPlayer p = player(mc);
                    p.teleportTo(p.serverLevel(), typesMin.getX() + 4.0, typesMin.getY() + 1.2, typesMin.getZ() - 2.5, 0F, 20F);
                    TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), typesMin);
                    p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new TankMenu(id, inv, c), Component.translatable("gui.bettacraft.tank.title")),
                            buf -> buf.writeBlockPos(c.getBlockPos()));
                });
                case 700 -> {
                    if (mc.screen instanceof TankScreen ts) ts.selectTabForTest(1);
                }
                case 730 -> shot(mc, "bettacraft_m5_menu_inhabitants");
                case 735 -> mc.getSingleplayerServer().execute(() -> {
                    // inventory with the new icons: decor, internal filter, buckets of every variety, cabinets
                    ServerPlayer p = player(mc);
                    p.closeContainer();
                    p.setGameMode(GameType.SURVIVAL);
                    p.getAbilities().flying = true;
                    p.getAbilities().mayfly = true;
                    p.onUpdateAbilities();
                    fillInventory(p);
                });
                case 760 -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
                case 770 -> hoverSlot(mc, 0, 1);
                case 800 -> shot(mc, "bettacraft_m5_inventory");
                case 805 -> hoverSlot(mc, 3, 3);
                case 830 -> shot(mc, "bettacraft_m5_inventory_decor");
                case 835 -> hoverSlot(mc, 7, 2);
                case 860 -> shot(mc, "bettacraft_m5_inventory_cabinet");
                case 865 -> {
                    mc.setScreen(null);
                    mc.getSingleplayerServer().execute(() -> {
                        ServerPlayer p = player(mc);
                        p.getInventory().clearContent();
                        p.setGameMode(GameType.CREATIVE);
                        p.teleportTo(p.serverLevel(), tankMin.getX() + 2.0, tankMin.getY() + 3.5, tankMin.getZ() - 7.0, 0F, 20F);
                        InteriorManager.enter(p, tankMin);
                    });
                }
                case 940 -> mc.getSingleplayerServer().execute(() -> {
                    // holding a dino skull: placement hologram on the floor in front
                    ServerPlayer p = player(mc);
                    InteriorRegion r = InteriorManager.regionAt(p.server, p.blockPosition());
                    if (r != null) {
                        BlockPos eye = r.toWorld(59, 1, 6);
                        p.teleportTo(p.serverLevel(), eye.getX() + 0.5, eye.getY(), eye.getZ() + 0.5, 0F, 45F);
                    }
                    p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                            new ItemStack(ModItems.DECOR.get(org.overengineer.bettacraft.decor.DecorType.DINO_SKULL).get()));
                });
                case 970 -> shot(mc, "bettacraft_m5_interior_hologram");
                case 975 -> mc.getSingleplayerServer().execute(() -> {
                    ServerPlayer p = player(mc);
                    p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    InteriorManager.leave(p);
                });
                case 1040 -> mc.getSingleplayerServer().execute(() -> {
                    ServerPlayer p = player(mc);
                    p.teleportTo(p.serverLevel(), cabMin.getX() + 5.5, cabMin.getY() + 2.4, cabMin.getZ() - 4.5, 0F, 16F);
                });
                case 1090 -> shot(mc, "bettacraft_m5_cabinets");
                case 1100 -> {
                    LOGGER.info("[smoketest] miniature render-thread time last frame: {} ms",
                            org.overengineer.bettacraft.client.interior.MiniatureRenderer.lastFrameNanos / 1e6);
                    LOGGER.info("[smoketest] client world test finished");
                    mc.stop();
                }
                default -> {}
            }
        } catch (RuntimeException ex) {
            LOGGER.error("[smoketest] FAILED", ex);
            mc.stop();
        }
    }

    /** Moves the mouse over a slot of the open inventory screen (col 0-8, row 0-2 main inventory, row 3 hotbar). */
    private static void hoverSlot(Minecraft mc, int col, int row) {
        if (!(mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> scr)) return;
        double scale = mc.getWindow().getGuiScale();
        int left = (scr.width - 176) / 2, top = (scr.height - 166) / 2;
        int x = left + 8 + col * 18 + 8, y = top + (row < 3 ? 84 + row * 18 : 142) + 8;
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

    private static void fillInventory(ServerPlayer p) {
        var inv = p.getInventory();
        inv.clearContent();
        int slot = 0;
        for (var d : org.overengineer.bettacraft.decor.DecorType.values()) inv.setItem(slot++, new ItemStack(ModItems.DECOR.get(d).get()));
        inv.setItem(slot++, new ItemStack(ModItems.INTERNAL_FILTER.get()));
        inv.setItem(slot++, new ItemStack(ModItems.EXTERNAL_FILTER.get()));
        inv.setItem(slot++, new ItemStack(ModItems.HEATER_SMALL.get()));
        inv.setItem(slot++, new ItemStack(ModItems.BETTA_PELLETS.get(), 16));
        inv.setItem(slot++, new ItemStack(ModItems.MOSQUITO_LARVAE.get(), 8));
        inv.setItem(slot++, new ItemStack(ModItems.INFUSORIA.get()));
        inv.setItem(slot++, new ItemStack(ModItems.ARTEMIA_NAUPLII.get()));
        // main inventory: one bucket per variety, then cabinets
        slot = 17;
        for (var v : org.overengineer.bettacraft.genetics.GeneticsData.get().varieties()) {
            if (slot >= 33) break;
            BettaGenome g = BettaGenome.of(v.example(), BettaGenome.DEFAULT);
            inv.setItem(slot++, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), BettaData.create(g, false, 0)));
        }
        String[] woods = {"cherry_planks", "dark_oak_planks", "warped_planks"};
        for (String w : woods) {
            if (slot >= 36) break;
            inv.setItem(slot++, org.overengineer.bettacraft.equipment.CabinetBlockItem.setWood(new ItemStack(ModItems.CABINET.get()),
                    net.minecraft.resources.ResourceLocation.withDefaultNamespace(w)));
        }
    }

    private static BlockPos typesMin, formsMin;

    /** M5: a showcase tank with every named variety and one with the fin forms (motionless fish, side view). */
    private static void m5Scene(ServerLevel level, BlockPos feet, ServerPlayer p) {
        typesMin = feet.offset(14, 0, 4);
        place(level, typesMin, 8, 2, 1);
        TankBlockEntity types = TankMultiblock.getController(level, typesMin);
        TankInteractions.changeWater(level, types, 16000);
        types.getTank().setName("Varieties");
        types.getTank().setSignVisible(false);
        types.getTank().setFront(Direction.NORTH);
        java.util.List<BettaData> list = new java.util.ArrayList<>();
        for (var v : org.overengineer.bettacraft.genetics.GeneticsData.get().varieties()) {
            list.add(BettaData.create(BettaGenome.of(v.example(), BettaGenome.DEFAULT), false, level.getGameTime()));
        }
        showcase(level, types, typesMin, list, 8, p);
        formsMin = feet.offset(14, 0, 10);
        place(level, formsMin, 6, 2, 1);
        TankBlockEntity forms = TankMultiblock.getController(level, formsMin);
        TankInteractions.changeWater(level, forms, 12000);
        forms.getTank().setSignVisible(false);
        java.util.List<BettaData> fl = new java.util.ArrayList<>();
        java.util.Map<String, String> colours = java.util.Map.of("body_color", "royal_blue", "fin_color", "red", "pattern", "butterfly",
                "pattern_color", "white", "iridescence", "low", "iridescence_color", "steel");
        for (String len : new String[]{"long", "short"}) {
            for (String shape : BettaGenome.SHAPES) {
                if (len.equals("short") && !java.util.List.of("round", "halfmoon", "crowntail", "doubletail").contains(shape)) continue;
                java.util.Map<String, String> m = new java.util.HashMap<>(colours);
                m.put("finform", shape);
                m.put("fin_length", len);
                fl.add(BettaData.create(BettaGenome.of(m, BettaGenome.DEFAULT), false, level.getGameTime()));
            }
        }
        showcase(level, forms, formsMin, fl, 6, p);
    }

    private static void showcase(ServerLevel level, TankBlockEntity c, BlockPos min, java.util.List<BettaData> fish, int perRow, ServerPlayer p) {
        java.util.List<java.util.UUID> order = new java.util.ArrayList<>();
        for (BettaData d : fish) {
            ItemStack bucket = BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d);
            Component err = TankInhabitants.insert(level, c, bucket, ModEntities.BETTA.get(), p, false);
            if (err != null) LOGGER.warn("[smoketest] showcase insert failed: {}", err.getString());
            order.add(d.fishId());
        }
        for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) {
            if (e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity m) {
                int k = order.indexOf(m.getBettaData().fishId());
                m.setNoAi(true);
                m.setNoGravity(true);
                int col = k % perRow, row = k / perRow;
                m.moveTo(min.getX() + col + 0.5, min.getY() + (1 - row) + 0.38, min.getZ() + 0.5, 90F, 0F);
                m.setYBodyRot(90F);
                m.setYHeadRot(90F);
            }
        }
        c.markTankChanged();
    }

    private static ServerPlayer player(Minecraft mc) {
        return mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }

    private static void buildScene() {
        Minecraft mc = Minecraft.getInstance();
        ServerPlayer p = player(mc);
        ServerLevel level = p.serverLevel();
        level.setDayTime(6000);
        p.getAbilities().flying = true; // keep camera positions (creative, no falling)
        p.onUpdateAbilities();
        BlockPos feet = p.blockPosition();
        // tank 4x3x3 south of the player, front facing the player
        tankMin = feet.offset(-2, 0, 4);
        place(level, tankMin, 4, 3, 3);
        TankBlockEntity c = TankMultiblock.getController(level, tankMin);
        TankInteractions.changeWater(level, c, 30000);
        c.getTank().setName("Betta Paradise");
        c.getTank().setSignPos(SignPosition.TOP_CENTER);
        c.getTank().setFront(Direction.NORTH);
        // M4: external filter on the west side with a large heater
        BlockPos efp = tankMin.offset(-1, 0, 1);
        level.setBlockAndUpdate(efp, ModBlocks.EXTERNAL_FILTER.get().defaultBlockState()
                .setValue(org.overengineer.bettacraft.equipment.ExternalFilterBlock.FACING, Direction.EAST));
        if (level.getBlockEntity(efp) instanceof org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity ef) {
            ef.insertHeater(new ItemStack(ModItems.HEATER_LARGE.get()));
        }
        c.getTank().setOwner(p.getUUID());
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        decorate(in, r, 3);
        decorItems(in, r);
        InteriorSync.markDirty(r.id());

        RandomSource rnd = RandomSource.create(1234);
        for (int i = 0; i < 6; i++) {
            BettaGenome g = BettaGenome.randomDomestic(rnd).withShape(BettaGenome.SHAPES.get(i % BettaGenome.SHAPES.size()));
            ItemStack bucket = BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), BettaData.create(g, i >= 4, level.getGameTime()));
            Component err = TankInhabitants.insert(level, c, bucket, ModEntities.BETTA.get(), p, false);
            if (err != null) LOGGER.warn("[smoketest] insert failed: {}", err.getString());
        }
        int n = 0;
        for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) {
            if (n++ == 0) e.setCustomName(Component.literal("Bubbles"));
        }
        // light bar along the front and a feeder on top
        for (int x = 0; x < 4; x++) {
            BlockPos lp = tankMin.offset(x, 3, 1);
            level.setBlockAndUpdate(lp, ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X));
        }
        for (int x = 0; x < 4; x++) {
            BlockPos lp = tankMin.offset(x, 3, 1);
            BlockState s = level.getBlockState(lp);
            level.setBlockAndUpdate(lp, s.updateShape(Direction.EAST, level.getBlockState(lp.east()), level, lp, lp.east())
                    .updateShape(Direction.WEST, level.getBlockState(lp.west()), level, lp, lp.west()));
        }
        BlockPos fp = tankMin.offset(3, 3, 2);
        level.setBlockAndUpdate(fp, ModBlocks.FEEDER.get().defaultBlockState());
        if (level.getBlockEntity(fp) instanceof FeederBlockEntity f) {
            f.items().insertItem(0, new ItemStack(ModItems.BETTA_PELLETS.get(), 16), false);
            f.items().insertItem(1, new ItemStack(ModItems.MOSQUITO_LARVAE.get(), 8), false);
            f.feedNow(level, c);
        }
        c.markTankChanged();
        // a pool with a wild betta next to it (ordinary water stays vanilla)
        for (int x = 0; x < 2; x++)
            for (int z = 0; z < 2; z++) level.setBlockAndUpdate(feet.offset(4 + x, -1, 4 + z), Blocks.WATER.defaultBlockState());
        var wild = ModEntities.BETTA.get().create(level);
        if (wild != null) {
            wild.moveTo(feet.getX() + 5, feet.getY() - 0.8, feet.getZ() + 5, 0, 0);
            wild.finalizeSpawn(level, level.getCurrentDifficultyAt(wild.blockPosition()), net.minecraft.world.entity.MobSpawnType.NATURAL, null);
            level.addFreshEntity(wild);
        }
        // showcase: one motionless betta per body shape in a 5x1x1 tank (for a close-up screenshot)
        showMin = feet.offset(-12, 0, 4);
        place(level, showMin, 5, 1, 1);
        TankBlockEntity show = TankMultiblock.getController(level, showMin);
        TankInteractions.changeWater(level, show, 5000);
        show.getTank().setSignVisible(false);
        // showcase: fry, juvenile, subadult, flaring adult male, female in breeding colours (motionless, for a close-up)
        RandomSource sr = RandomSource.create(99);
        org.overengineer.bettacraft.entity.betta.LifeStage[] stages = {org.overengineer.bettacraft.entity.betta.LifeStage.FRY,
                org.overengineer.bettacraft.entity.betta.LifeStage.JUVENILE, org.overengineer.bettacraft.entity.betta.LifeStage.SUBADULT,
                org.overengineer.bettacraft.entity.betta.LifeStage.ADULT, org.overengineer.bettacraft.entity.betta.LifeStage.ADULT};
        java.util.List<java.util.UUID> order = new java.util.ArrayList<>();
        BettaGenotype showGenes = BettaGenotype.randomDomestic(sr).with(org.overengineer.bettacraft.genetics.BettaLoci.FINFORM,
                new BettaGenotype.AllelePair("halfmoon", "round"));
        for (int i = 0; i < 5; i++) {
            BettaData d = BettaData.create(showGenes, i == 4, level.getGameTime()).withStage(stages[i]);
            ItemStack bucket = BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d);
            TankInhabitants.insert(level, show, bucket, ModEntities.BETTA.get(), p, false);
            order.add(d.fishId());
        }
        for (Entity e : TankInhabitants.liveInhabitants(level, show.getTank())) {
            if (e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity m) {
                int k = order.indexOf(m.getBettaData().fishId());
                m.setNoAi(true);
                m.setNoGravity(true);
                m.moveTo(showMin.getX() + k + 0.5, showMin.getY() + 0.4, showMin.getZ() + 0.5, 90F, 0F);
                m.setYBodyRot(90F);
                m.setYHeadRot(90F);
                if (k == 3) m.flare(Long.MAX_VALUE / 2);
                if (k == 4) m.showBreedingMood(Long.MAX_VALUE / 2);
                if (k == 3) m.setCustomName(Component.literal("Sultan"));
            }
        }
        // breeding tank: a pair in warm water, run until the female has spawned into the bubble nest
        breedMin = feet.offset(5, 0, 9);
        place(level, breedMin, 3, 2, 2);
        TankBlockEntity breed = TankMultiblock.getController(level, breedMin);
        TankInteractions.changeWater(level, breed, 12000);
        breed.getTank().setName("Breeding");
        breed.getTank().setFront(Direction.NORTH);
        breed.getTank().setOwner(p.getUUID());
        BlockPos bfp = breedMin.offset(3, 0, 0);
        level.setBlockAndUpdate(bfp, ModBlocks.EXTERNAL_FILTER.get().defaultBlockState()
                .setValue(org.overengineer.bettacraft.equipment.ExternalFilterBlock.FACING, Direction.WEST));
        if (level.getBlockEntity(bfp) instanceof org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity bf) {
            bf.insertHeater(new ItemStack(ModItems.HEATER_LARGE.get()));
        }
        org.overengineer.bettacraft.tank.TankEquipment.invalidate(breed);
        breed.getTank().setTargetTemp(27.0F);
        breed.getTank().setWaterTemp(27.0F);
        RandomSource br = RandomSource.create(4321);
        for (int i = 0; i < 2; i++) {
            BettaGenotype g = BettaGenotype.randomDomestic(br).with(org.overengineer.bettacraft.genetics.BettaLoci.FINFORM,
                    new BettaGenotype.AllelePair(i == 0 ? "round" : "veil", "halfmoon"));
            ItemStack bucket = BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), BettaData.create(g, i == 1, level.getGameTime()));
            TankInhabitants.insert(level, breed, bucket, ModEntities.BETTA.get(), p, false);
        }
        for (Entity e : TankInhabitants.liveInhabitants(level, breed.getTank())) {
            if (e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity b) e.setCustomName(Component.literal(b.isFemale() ? "Luna" : "Rex"));
        }
        TankCare.feed(level, breed, new ItemStack(ModItems.MOSQUITO_LARVAE.get()), 1);
        for (int i = 0; i < 60 && (breed.getTank().nest() == null || !breed.getTank().nest().hasEggs()); i++) {
            org.overengineer.bettacraft.tank.TankBreeding.step(level, breed, 0.05);
        }
        org.overengineer.bettacraft.tank.TankBreeding.step(level, breed, 0.3); // larvae
        breed.markTankChanged();
        // 16x16x16 tank at scale 16, prepared asynchronously like for a player, decorated when ready (see tick)
        bigMin = feet.offset(-40, 0, 30);
        place(level, bigMin, 16, 16, 16);
        TankBlockEntity big = TankMultiblock.getController(level, bigMin);
        big.getTank().setSignVisible(false);
        TankInteractions.changeWater(level, big, 16L * 16 * 16 * 1000);
        InteriorRegion bigR = InteriorManager.regionFor(level, big);
        org.overengineer.bettacraft.tank.interior.InteriorPreparation.prepare(level.getServer(), bigR, null);
        bigRegion = bigR;
        m4Scene(level, feet, p);
        m5Scene(level, feet, p);
        p.teleportTo(level, tankMin.getX() + 2.0, tankMin.getY() + 3.5, tankMin.getZ() - 7.0, 0F, 20F);
        LOGGER.info("[smoketest] scene built: {} inhabitants, {}", TankInhabitants.liveInhabitants(level, c.getTank()).size(), c.getTank());
    }

    private static BlockPos cabMin, filterMin;

    /**
     * M4: four pairs of cabinets (oak, cherry, crimson, bamboo) each carrying a 2x2x1 tank, a filter with heater in
     * one cabinet per pair; and a small 2x1x1 tank with low water, an external filter (east) and an internal filter.
     */
    private static void m4Scene(ServerLevel level, BlockPos feet, ServerPlayer p) {
        cabMin = feet.offset(-16, 0, 14);
        String[] woods = {"oak_planks", "cherry_planks", "crimson_planks", "bamboo_planks"};
        for (int g = 0; g < woods.length; g++) {
            BlockPos gx = cabMin.offset(g * 3, 0, 0);
            for (int i = 0; i < 2; i++) {
                BlockPos cp = gx.offset(i, 0, 0);
                level.setBlockAndUpdate(cp, ModBlocks.CABINET.get().defaultBlockState()
                        .setValue(org.overengineer.bettacraft.equipment.CabinetBlock.FACING, Direction.NORTH));
                if (level.getBlockEntity(cp) instanceof org.overengineer.bettacraft.equipment.CabinetBlockEntity cab) {
                    cab.setWood(net.minecraft.resources.ResourceLocation.withDefaultNamespace(woods[g]));
                    if (i == 1) {
                        ItemStack f = new ItemStack(ModItems.EXTERNAL_FILTER.get());
                        org.overengineer.bettacraft.equipment.FilterHeaters.write(f, java.util.List.of(new ItemStack(ModItems.HEATER_MEDIUM.get()), ItemStack.EMPTY));
                        cab.items().setStackInSlot(org.overengineer.bettacraft.equipment.CabinetBlockEntity.FILTER_SLOT, f);
                        cab.storage().insertItem(0, new ItemStack(ModItems.BETTA_PELLETS.get(), 32), false);
                    }
                    cab.setChanged();
                    level.sendBlockUpdated(cp, level.getBlockState(cp), level.getBlockState(cp), 3);
                }
            }
            for (int i = 0; i < 2; i++) {
                BlockPos cp = gx.offset(i, 0, 0);
                level.setBlockAndUpdate(cp, net.minecraft.world.level.block.Block.updateFromNeighbourShapes(level.getBlockState(cp), level, cp));
            }
            place(level, gx.above(), 2, 2, 1);
            TankBlockEntity t = TankMultiblock.getController(level, gx.above());
            TankInteractions.changeWater(level, t, 3400);
            t.getTank().setSignVisible(false);
            t.markTankChanged();
        }
        // filter demo tank
        filterMin = feet.offset(8, 0, -6);
        place(level, filterMin, 2, 1, 1);
        TankBlockEntity ft = TankMultiblock.getController(level, filterMin);
        TankInteractions.changeWater(level, ft, 900);
        ft.getTank().setSignVisible(false);
        ft.getTank().setFront(Direction.NORTH);
        BlockPos fp = filterMin.offset(2, 0, 0);
        level.setBlockAndUpdate(fp, ModBlocks.EXTERNAL_FILTER.get().defaultBlockState()
                .setValue(org.overengineer.bettacraft.equipment.ExternalFilterBlock.FACING, Direction.WEST));
        if (level.getBlockEntity(fp) instanceof org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity f) {
            f.insertHeater(new ItemStack(ModItems.HEATER_SMALL.get()));
            f.insertHeater(new ItemStack(ModItems.HEATER_SMALL.get()));
        }
        InteriorRegion r = InteriorManager.ensureRegion(level, ft);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        LongOpenHashSet touched = new LongOpenHashSet();
        BlockPos o = r.toWorld(0, 0, 0);
        InteriorWorldOps.fill(in, o.getX(), o.getY(), o.getZ(), o.getX() + r.sizeX() - 1, o.getY(), o.getZ() + r.sizeZ() - 1,
                Blocks.SAND.defaultBlockState(), touched);
        InteriorWorldOps.resendChunks(in, touched);
        int[] shape = org.overengineer.bettacraft.equipment.SegmentBlock.filterShape(r.scale());
        for (int y = 0; y < shape[1]; y++)
            for (int z = 0; z < shape[0]; z++)
                for (int x = 0; x < shape[0]; x++) {
                    var part = y == 0 ? (x == 0 && z == 0 ? org.overengineer.bettacraft.equipment.SegmentBlock.Part.ANCHOR
                            : org.overengineer.bettacraft.equipment.SegmentBlock.Part.BASE)
                            : y == shape[1] - 1 ? org.overengineer.bettacraft.equipment.SegmentBlock.Part.TOP
                            : org.overengineer.bettacraft.equipment.SegmentBlock.Part.MIDDLE;
                    in.setBlockAndUpdate(r.toWorld(8 + x, 1 + y, 8 + z), ModBlocks.FILTER_SEGMENT.get().defaultBlockState()
                            .setValue(org.overengineer.bettacraft.equipment.SegmentBlock.PART, part)
                            .setValue(org.overengineer.bettacraft.equipment.SegmentBlock.FACING, Direction.NORTH));
                }
        for (int i = 0; i < 6; i++) in.setBlockAndUpdate(r.toWorld(20 + (i % 3) * 3, 1, 6 + (i / 3) * 4), Blocks.SEAGRASS.defaultBlockState());
        InteriorSync.markDirty(r.id());
        ft.markTankChanged();
    }

    private static void place(ServerLevel level, BlockPos min, int sx, int sy, int sz) {
        for (int y = 0; y < sy; y++)
            for (int z = 0; z < sz; z++)
                for (int x = 0; x < sx; x++) {
                    BlockPos pos = min.offset(x, y, z);
                    level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
                    TankMultiblock.onTankBlockAdded(level, pos, null);
                }
    }

    /** M3: decoration structures, a substrate patch, a heater rod and lily pads in the main tank. */
    private static void decorItems(ServerLevel in, InteriorRegion r) {
        // clear a stage in the front half and put the decoration structures on it
        LongOpenHashSet touched = new LongOpenHashSet();
        InteriorWorldOps.clear(in, r.originX() + 1, InteriorRegion.BASE_Y, r.originZ() + 1, r.originX() + r.sizeX() - 2,
                InteriorRegion.BASE_Y + r.sizeY() - 1, r.originZ() + 30, touched);
        InteriorWorldOps.resendChunks(in, touched);
        org.overengineer.bettacraft.decor.DecorType[] types = {org.overengineer.bettacraft.decor.DecorType.CASTLE,
                org.overengineer.bettacraft.decor.DecorType.SHIPWRECK, org.overengineer.bettacraft.decor.DecorType.VOLCANO,
                org.overengineer.bettacraft.decor.DecorType.DINO_SKULL, org.overengineer.bettacraft.decor.DecorType.CAVE,
                org.overengineer.bettacraft.decor.DecorType.DRIFTWOOD, org.overengineer.bettacraft.decor.DecorType.STONES,
                org.overengineer.bettacraft.decor.DecorType.HAMMOCK, org.overengineer.bettacraft.decor.DecorType.ALMOND_LEAF,
                org.overengineer.bettacraft.decor.DecorType.MOSS_BALL};
        int[][] spots = {{32, 14}, {12, 10}, {52, 12}, {20, 22}, {44, 24}, {8, 24}, {26, 6}, {38, 6}, {16, 4}, {28, 26}};
        for (int i = 0; i < types.length; i++) {
            var err = org.overengineer.bettacraft.decor.DecorItem.place(in, types[i], r.toWorld(spots[i][0], 0, spots[i][1]),
                    net.minecraft.world.level.block.Rotation.NONE, null);
            if (err != null) LOGGER.warn("[smoketest] decor {}: {}", types[i], err.getString());
        }
        for (int y = 0; y < 6; y++) {
            in.setBlockAndUpdate(r.toWorld(58, y, 4), ModBlocks.HEATER_SEGMENT.get().defaultBlockState()
                    .setValue(org.overengineer.bettacraft.equipment.SegmentBlock.PART, y == 0 ? org.overengineer.bettacraft.equipment.SegmentBlock.Part.ANCHOR
                            : y == 5 ? org.overengineer.bettacraft.equipment.SegmentBlock.Part.TOP : org.overengineer.bettacraft.equipment.SegmentBlock.Part.MIDDLE)
                    .setValue(org.overengineer.bettacraft.equipment.SegmentBlock.SIZE, org.overengineer.bettacraft.equipment.SegmentBlock.Size.LARGE));
        }
        for (int[] lp : new int[][]{{20, 14}, {21, 15}, {44, 8}}) {
            in.setBlockAndUpdate(r.toWorld(lp[0], r.sizeY() - 6, lp[1]), Blocks.LILY_PAD.defaultBlockState());
        }
        // substrate pattern on the floor of the front stage (its sand layer was cleared above the floor only)
        for (int x = 0; x < r.sizeX(); x++)
            for (int z = 0; z < r.sizeZ(); z++)
                org.overengineer.bettacraft.tank.interior.Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z),
                        ((x / 4 + z / 4) % 2 == 0 ? Blocks.GRAVEL : Blocks.RED_SAND).defaultBlockState(), false);
    }

    /** Substrate (1/16 of the height), rock boxes and plant columns, written directly into the sections. */
    private static void decorate(ServerLevel in, InteriorRegion r, int tankHeight) {
        LongOpenHashSet touched = new LongOpenHashSet();
        int s = r.scale();
        int sub = Math.max(2, r.sizeY() / 16);
        BlockPos o = r.toWorld(0, 0, 0);
        InteriorWorldOps.fill(in, o.getX(), o.getY(), o.getZ(), o.getX() + r.sizeX() - 1, o.getY() + sub - 1, o.getZ() + r.sizeZ() - 1,
                Blocks.SAND.defaultBlockState(), touched);
        java.util.Random rnd = new java.util.Random(7);
        BlockState[] rocks = {Blocks.STONE.defaultBlockState(), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Blocks.TUFF.defaultBlockState()};
        int rockCount = Math.max(3, r.sizeX() * r.sizeZ() / 1600);
        for (int i = 0; i < rockCount; i++) {
            int w = s / 2 + rnd.nextInt(s), h = s / 2 + rnd.nextInt(s);
            int x = rnd.nextInt(r.sizeX() - w), z = rnd.nextInt(r.sizeZ() - w);
            InteriorWorldOps.fill(in, o.getX() + x, o.getY() + sub, o.getZ() + z, o.getX() + x + w - 1, o.getY() + sub + h - 1,
                    o.getZ() + z + w - 1, rocks[i % rocks.length], touched);
        }
        BlockState[] plants = {Blocks.SEAGRASS.defaultBlockState(), Blocks.KELP_PLANT.defaultBlockState(), Blocks.TUBE_CORAL_BLOCK.defaultBlockState()};
        int plantCount = Math.max(20, r.sizeX() * r.sizeZ() / 220);
        for (int i = 0; i < plantCount; i++) {
            int x = rnd.nextInt(r.sizeX()), z = rnd.nextInt(r.sizeZ());
            int h = s * (1 + rnd.nextInt(Math.max(1, tankHeight / 2)));
            InteriorWorldOps.fill(in, o.getX() + x, o.getY() + sub, o.getZ() + z, o.getX() + x, Math.min(o.getY() + sub + h, o.getY() + r.sizeY() - 1),
                    o.getZ() + z, plants[i % plants.length], touched);
        }
        InteriorWorldOps.resendChunks(in, touched);
    }
}
