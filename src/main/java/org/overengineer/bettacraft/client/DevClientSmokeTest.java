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
            switch (ticks) {
                case 60 -> mc.getSingleplayerServer().execute(DevClientSmokeTest::buildScene);
                case 330 -> shot(mc, "bettacraft_m2_tank");
                case 340 -> mc.getSingleplayerServer().execute(() -> {
                    // close to the breeding tank, looking at the nest (Jade shows the tank)
                    ServerPlayer p = player(mc);
                    p.teleportTo(p.serverLevel(), breedMin.getX() + 1.5, breedMin.getY() + 1.2, breedMin.getZ() - 1.6, 0F, 28F);
                });
                case 420 -> shot(mc, "bettacraft_m2_nest");
                case 430 -> mc.getSingleplayerServer().execute(() -> {
                    ServerPlayer p = player(mc);
                    p.teleportTo(p.serverLevel(), showMin.getX() + 2.5, showMin.getY() - 1.15, showMin.getZ() - 1.25, 0F, 2F);
                });
                case 520 -> shot(mc, "bettacraft_m2_stages");
                case 530 -> mc.getSingleplayerServer().execute(() -> {
                    // straight at the flaring male (Jade shows the betta)
                    ServerPlayer p = player(mc);
                    p.teleportTo(p.serverLevel(), showMin.getX() + 3.5, showMin.getY() + 0.42 - 1.62, showMin.getZ() - 0.7, 0F, 0F);
                });
                case 590 -> shot(mc, "bettacraft_m2_jade_betta");
                case 600 -> mc.getSingleplayerServer().execute(() -> {
                    ServerPlayer p = player(mc);
                    p.teleportTo(p.serverLevel(), breedMin.getX() + 1.5, breedMin.getY() + 1.2, breedMin.getZ() - 2.5, 0F, 20F);
                    TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), breedMin);
                    p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new TankMenu(id, inv, c), Component.translatable("gui.bettacraft.tank.title")),
                            buf -> buf.writeBlockPos(c.getBlockPos()));
                });
                case 640 -> {
                    if (mc.screen instanceof TankScreen ts) ts.selectTabForTest(2);
                }
                case 670 -> shot(mc, "bettacraft_m2_menu_breeding");
                case 680 -> {
                    if (mc.screen instanceof TankScreen ts) ts.selectTabForTest(1);
                }
                case 710 -> shot(mc, "bettacraft_m2_menu_inhabitants");
                case 720 -> {
                    if (mc.screen instanceof TankScreen ts) ts.selectTabForTest(3);
                }
                case 750 -> shot(mc, "bettacraft_m2_menu_log");
                case 760 -> {
                    if (mc.screen instanceof TankScreen ts) ts.selectTabForTest(0);
                }
                case 790 -> shot(mc, "bettacraft_m2_menu_overview");
                case 830 -> {
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
        c.getTank().heaters().setItem(0, new ItemStack(ModItems.HEATER_LARGE.get()));
        c.getTank().setOwner(p.getUUID());
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        decorate(in, r, 3);
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
                new BettaGenotype.AllelePair("halfmoon", "plakat"));
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
        breed.getTank().heaters().setItem(0, new ItemStack(ModItems.HEATER_LARGE.get()));
        breed.getTank().setTargetTemp(27.0F);
        breed.getTank().setWaterTemp(27.0F);
        RandomSource br = RandomSource.create(4321);
        for (int i = 0; i < 2; i++) {
            BettaGenotype g = BettaGenotype.randomDomestic(br).with(org.overengineer.bettacraft.genetics.BettaLoci.FINFORM,
                    new BettaGenotype.AllelePair(i == 0 ? "plakat" : "veil", "halfmoon"));
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
        p.teleportTo(level, tankMin.getX() + 2.0, tankMin.getY() + 3.5, tankMin.getZ() - 7.0, 0F, 20F);
        LOGGER.info("[smoketest] scene built: {} inhabitants, {}", TankInhabitants.liveInhabitants(level, c.getTank()).size(), c.getTank());
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
