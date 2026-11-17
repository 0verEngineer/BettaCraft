package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.containers.AnimalContainers;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterLooks;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.critter.WildCritterEntity;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.genetics.MagnifierItem;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateBucketItem;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateItems;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M22.3 dev client smoke scene ({@code -Psmoketestm22_3}): wild fish shoals and small animals crawling on the bottom of a
 * glass pool, the wild corals on a reef, the magnifier and the single small animals in the tank menu (en + de), the
 * tooltips of buckets and bags with several animals. Screenshots {@code bettacraft_m22_3_*}.
 */
final class DevClientSmokeM223 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private BlockPos pool, reef, tank;
    private final String lang = Minecraft.getInstance().options.languageCode;

    DevClientSmokeM223() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        // 1. wild fish and small animals in a pool, seen through the glass and from above
        // a long lens (field of view 30) for the small animals
        then(1, () -> mc.options.fov().set(30));
        then(1, () -> look(pool.getX() + 6.5, pool.getY() + 2.6, pool.getZ() - 2.5, 0F, 8F));
        then(120, () -> shot("pool_fish"));
        then(1, () -> look(pool.getX() + 3.0, pool.getY() + 2.0, pool.getZ() - 1.5, 0F, 26F));
        then(60, () -> {
            countCritters();
            shot("pool_critters");
        });
        then(1, () -> look(pool.getX() + 9.0, pool.getY() + 2.0, pool.getZ() - 1.5, 0F, 26F));
        then(40, () -> shot("pool_snails"));
        then(1, () -> mc.options.fov().set(70));
        then(1, () -> look(pool.getX() + 6.5, pool.getY() + 7.0, pool.getZ() + 4.0, 0F, 89F));
        then(40, () -> shot("pool_top"));
        // 2. the wild corals on a reef
        then(1, () -> look(reef.getX() + 6.5, reef.getY() + 6.5, reef.getZ() - 3.5, 0F, 38F));
        then(80, () -> shot("reef"));
        then(1, () -> look(reef.getX() + 3.5, reef.getY() + 4.0, reef.getZ() - 1.6, 0F, 35F));
        then(40, () -> shot("reef_near"));
        // 3. the tank menu: single small animals, a fish with unknown genes, the magnifier
        then(1, () -> mc.options.hideGui = false);
        then(1, () -> server(this::fillInventory));
        then(1, () -> server(p -> p.teleportTo(p.serverLevel(), tank.getX() + 1.5, tank.getY() + 0.2, tank.getZ() - 2.5, 0F, 0F)));
        menu("menu_critter_list", ts -> {
            ts.showCrittersPageForTest();
            ts.selectFirstCritterForTest();
        });
        menu("menu_fish_unknown", ts -> ts.showBettaForTest());
        then(1, () -> server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), tank);
            int n = MagnifierItem.analyseAll(p.serverLevel(), c, p, MagnifierItem.find(p), null);
            LOGGER.info("[smoketest] M22.3 magnifier analysed {} animals", n);
            if (n < 3) LOGGER.error("[smoketest] M22.3 the magnifier analysed too few animals: {}", n);
        }));
        menu("menu_fish_analysed", ts -> ts.showBettaForTest());
        menu("menu_critter_analysed", ts -> {
            ts.showCrittersPageForTest();
            ts.selectFirstCritterForTest();
        });
        // 4. tooltips of buckets and bags with several animals
        then(5, () -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
        then(20, () -> shot("inventory"));
        tooltip("tooltip_critter_bucket", s -> s.is(ModItems.CRITTER_BUCKET.get()));
        tooltip("tooltip_neon_bucket", s -> s.getItem() instanceof TankmateBucketItem);
        tooltip("tooltip_betta_bucket", s -> s.is(ModItems.BETTA_BUCKET.get()));
        tooltip("tooltip_bag", s -> s.is(ModItems.FISH_BAG.get()) && s.has(org.overengineer.bettacraft.registry.ModDataComponents.TANKMATE_GROUP.get()));
        tooltip("tooltip_magnifier", s -> s.is(ModItems.MAGNIFIER.get()));
        then(5, () -> mc.setScreen(null));
    }

    // ------------------------------------------------------------------ the scene

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        level.setDayTime(6000);
        level.setWeatherParameters(24000, 0, false, false);
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, p.server);
        BlockPos base = p.blockPosition().offset(-30, 2, 420);
        pool = base;
        reef = base.offset(20, 0, 0);
        tank = base.offset(40, 0, 0);
        glassPool(level, pool, 13, 8, 4, Blocks.SAND.defaultBlockState());
        glassPool(level, reef, 13, 8, 5, Blocks.SAND.defaultBlockState());
        // fish shoals in the pool (wild-looking: spawned like in the world)
        java.util.Random r = new java.util.Random(223);
        spawnShoal(level, TankmateSpecies.NEON_TETRA, pool.offset(4, 2, 3), 6);
        spawnShoal(level, TankmateSpecies.HARLEQUIN_RASBORA, pool.offset(9, 2, 4), 5);
        spawnShoal(level, TankmateSpecies.PANDA_CORY, pool.offset(6, 1, 2), 4);
        // small animals on the bottom: a swarm of shrimp, snails, a crayfish, a hermit crab
        for (int i = 0; i < 8; i++) critter(level, CritterSpecies.NEOCARIDINA, pool.offset(2, 1, 1), i == 0 ? CritterGenes.pure(0, 0.6F) : CritterGenes.WILD, r);
        for (int i = 0; i < 3; i++) critter(level, CritterSpecies.RAMSHORN, pool.offset(9, 1, 1), CritterGenes.WILD, r);
        critter(level, CritterSpecies.APPLE_SNAIL, pool.offset(10, 1, 1), CritterGenes.pure(0, 0.4F), r);
        critter(level, CritterSpecies.ZEBRA_NERITE, pool.offset(8, 1, 1), CritterGenes.WILD, r);
        critter(level, CritterSpecies.DWARF_CRAYFISH, pool.offset(4, 1, 1), CritterGenes.pure(0, 0.5F), r);
        critter(level, CritterSpecies.HERMIT_CRAB, pool.offset(5, 1, 1), CritterGenes.WILD, r);
        // a reef: coral blocks, vanilla corals and our wild colonies
        BlockState[] blocks = {Blocks.BRAIN_CORAL_BLOCK.defaultBlockState(), Blocks.TUBE_CORAL_BLOCK.defaultBlockState(), Blocks.FIRE_CORAL_BLOCK.defaultBlockState(),
                Blocks.HORN_CORAL_BLOCK.defaultBlockState(), Blocks.BUBBLE_CORAL_BLOCK.defaultBlockState()};
        for (int x = 0; x < 13; x++)
            for (int z = 0; z < 8; z++) {
                int h = 1 + ((x * 7 + z * 3) % 3 == 0 ? 1 : 0);
                for (int y = 1; y <= h; y++) level.setBlock(reef.offset(x, y, z), blocks[(x + z + y) % blocks.length], 2);
            }
        PlantSpecies[] corals = {PlantSpecies.ZOANTHUS, PlantSpecies.GREEN_STAR_POLYPS, PlantSpecies.MUSHROOM_CORAL, PlantSpecies.LEATHER_CORAL,
                PlantSpecies.HAMMER_CORAL, PlantSpecies.LOBOPHYLLIA, PlantSpecies.ACROPORA, PlantSpecies.BUBBLE_TIP_ANEMONE};
        int i = 0;
        for (int x = 0; x < 13; x++)
            for (int z = 0; z < 4; z++) {
                BlockPos top = topOfReef(level, reef.offset(x, 1, z));
                if (top == null) continue;
                if ((x + z) % 3 == 0) {
                    level.setBlock(top, ModBlocks.WILD_CORALS.get(corals[i++ % corals.length]).get().defaultBlockState(), 2);
                } else if ((x + z) % 3 == 1) {
                    level.setBlock(top, (z % 2 == 0 ? Blocks.TUBE_CORAL : Blocks.FIRE_CORAL_FAN).defaultBlockState(), 2);
                }
            }
        for (int k = 0; k < 6; k++) org.overengineer.bettacraft.reef.WildCoralFeature.place(level, reef.offset(6, 2, 6), level.random);
        // a tank: shrimp of two colours and both sexes, a betta and a neon shoal, nothing analysed yet
        TankBlockEntity c = tank(level, p, tank, 3, 2, 2);
        for (int k = 0; k < 6; k++) TankCritters.add(level, c, CritterSpecies.NEOCARIDINA, CritterGenes.cross(CritterGenes.pure(0, 0.45F),
                new CritterGenes(1, 0b10, 0.4F), 6, r), k % 2 == 0, 12);
        for (int k = 0; k < 4; k++) TankCritters.add(level, c, CritterSpecies.RAMSHORN, CritterGenes.WILD, false, 12);
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        BettaEntity b = ModEntities.BETTA.get().create(level);
        b.setBettaData(BettaData.create(BettaGenotype.randomWild(level.random), false, 0));
        ItemStack bb = b.getBucketItemStack();
        b.saveToBucketTag(bb);
        b.discard();
        TankInhabitants.insert(level, c, bb, ModEntities.BETTA.get(), p, 0L);
        for (int k = 0; k < 4; k++) {
            TankmateBreeding.spawn(level, c, TankmateSpecies.NEON_TETRA, TankmateGenes.wild(TankmateSpecies.NEON_TETRA), k % 2 == 0, LifeStage.ADULT, 0,
                    new Vec3(w.box().minX + 0.5 + k * 0.4, w.box().minY + 0.6, w.box().minZ + 0.6));
        }
        for (var e : TankInhabitants.liveInhabitants(level, c.getTank())) if (e instanceof net.minecraft.world.entity.Mob m) SmokeFreeze.freeze(m, true);
        LOGGER.info("[smoketest] M22.3 scene at {}: pool {}, reef {}, tank {}", base, pool, reef, tank);
    }

    private static BlockPos topOfReef(ServerLevel level, BlockPos from) {
        BlockPos.MutableBlockPos m = from.mutable();
        for (int i = 0; i < 4; i++) {
            if (level.getBlockState(m).is(Blocks.WATER)) return m.immutable();
            m.move(Direction.UP);
        }
        return null;
    }

    private void spawnShoal(ServerLevel level, TankmateSpecies s, BlockPos at, int n) {
        for (int i = 0; i < n; i++) {
            var e = ModEntities.TANKMATES.get(s).get().spawn(level, at.offset(i % 3 - 1, 0, i / 3), MobSpawnType.SPAWN_EGG);
            if (e == null) continue;
            e.moveTo(at.getX() + 0.5 + (i % 3) * 0.45, at.getY() + 0.3 + (i % 2) * 0.25, at.getZ() + 0.5 + (i / 3) * 0.4, 90F, 0F);
            e.setup(org.overengineer.bettacraft.world.Wildlife.tankmate(s, new java.util.Random(i)::nextLong), i % 2 == 0, LifeStage.ADULT, 0);
            SmokeFreeze.freeze(e, true);
        }
    }

    private void critter(ServerLevel level, CritterSpecies s, BlockPos at, CritterGenes g, java.util.Random r) {
        Critter k = new Critter(0, s, s.hasSexes() && r.nextBoolean(), g, 0, 0, 0);
        k.age = s.juvenileDays() + 4;
        k.look = CritterLooks.random(s, r);
        WildCritterEntity e = WildCritterEntity.of(level, k.saveAnimal());
        if (e == null) return;
        e.moveTo(at.getX() + r.nextDouble(), at.getY() + 0.05, at.getZ() + r.nextDouble() * 1.5, r.nextFloat() * 360F, 0F);
        e.setPersistenceRequired();
        level.addFreshEntity(e);
    }

    private void countCritters() {
        server(p -> {
            var list = p.serverLevel().getEntitiesOfClass(WildCritterEntity.class, new net.minecraft.world.phys.AABB(pool).inflate(16));
            LOGGER.info("[smoketest] M22.3 {} wild small animals in the pool", list.size());
            if (list.size() < 10) LOGGER.error("[smoketest] M22.3 wild small animals missing: {}", list.size());
            int inWater = 0;
            for (var e : list) if (e.isInWater()) inWater++;
            if (inWater < list.size()) LOGGER.error("[smoketest] M22.3 small animals out of the water: {} of {}", list.size() - inWater, list.size());
        });
    }

    /** A pool with glass walls (the front one low enough to look over), water {@code depth} deep on {@code floor}. */
    private static void glassPool(ServerLevel level, BlockPos min, int sx, int sz, int depth, BlockState floor) {
        for (int x = -1; x <= sx; x++)
            for (int z = -1; z <= sz; z++) {
                level.setBlock(min.offset(x, 0, z), floor, 2);
                for (int y = 1; y <= depth; y++) {
                    boolean wall = x == -1 || x == sx || z == -1 || z == sz;
                    // the front wall: invisible barriers (a clear view through the side of the pool)
                    BlockState w = z == -1 ? Blocks.BARRIER.defaultBlockState() : Blocks.GLASS.defaultBlockState();
                    level.setBlock(min.offset(x, y, z), wall ? w : Blocks.WATER.defaultBlockState(), 2);
                }
                for (int y = depth + 1; y <= depth + 3; y++) level.setBlock(min.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
    }

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, (long) sx * sy * sz * 1000, 0F);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(p.getUUID());
        c.getTank().setWaterTemp(25F);
        c.getTank().setTargetTemp(25F);
        c.markTankChanged();
        return c;
    }

    private void fillInventory(ServerPlayer p) {
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.getAbilities().mayfly = true;
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.getInventory().clearContent();
        java.util.Random r = new java.util.Random(7);
        List<CompoundTag> shrimp = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Critter k = new Critter(0, CritterSpecies.NEOCARIDINA, i % 2 == 0, CritterGenes.cross(CritterGenes.pure(0, 0.5F), new CritterGenes(1, 0b100, 0.5F), 6, r), 0, 0, 0);
            k.age = 6 + i;
            k.analysed = i < 4;
            shrimp.add(k.saveAnimal());
        }
        List<CompoundTag> neons = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack tmp = TankmateBucketItem.withFish(new ItemStack(ModItems.FISH_BAG.get()), TankmateSpecies.NEON_TETRA,
                    TankmateGenes.wild(TankmateSpecies.NEON_TETRA), i % 2 == 0, LifeStage.ADULT);
            neons.add(tmp.get(net.minecraft.core.component.DataComponents.BUCKET_ENTITY_DATA).copyTag());
        }
        ItemStack betta = BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()),
                BettaData.create(BettaGenotype.randomDomestic(net.minecraft.util.RandomSource.create(5)), false, 0));
        ItemStack plecos = TankmateItems.groupBag(TankmateSpecies.BRISTLENOSE_PLECO, 3, () -> TankmateGenes.wild(TankmateSpecies.BRISTLENOSE_PLECO, 0.5, r), r, true);
        ItemStack[] hotbar = {AnimalContainers.critterBucket(shrimp), AnimalContainers.tankmateBucket(TankmateSpecies.NEON_TETRA, neons), betta, plecos,
                new ItemStack(ModItems.MAGNIFIER.get()), new ItemStack(Items.BUCKET, 4), new ItemStack(ModItems.FISH_BAG.get(), 8),
                new ItemStack(ModItems.WILD_CORALS.get(PlantSpecies.ACROPORA).get())};
        for (int i = 0; i < hotbar.length; i++) p.getInventory().setItem(i, hotbar[i]);
        p.inventoryMenu.broadcastChanges();
    }

    // ------------------------------------------------------------------ steps

    private void menu(String name, java.util.function.Consumer<org.overengineer.bettacraft.client.screen.TankScreen> page) {
        then(10, () -> server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), tank);
            if (c == null) return;
            p.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        }));
        then(30, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) page.accept(ts);
            else LOGGER.error("[smoketest] M22.3 no tank menu for {}", name);
        });
        then(20, () -> shot(name));
        then(1, () -> language("de_de"));
        then(20, () -> shot(name + "_de"));
        then(1, () -> language(lang));
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.setScreen(null));
    }

    private void tooltip(String name, java.util.function.Predicate<ItemStack> which) {
        then(1, () -> hover(which));
        then(10, () -> shot(name));
        then(1, () -> language("de_de"));
        then(10, () -> hover(which));
        then(10, () -> shot(name + "_de"));
        then(1, () -> language(lang));
    }

    private void hover(java.util.function.Predicate<ItemStack> which) {
        if (!(mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> scr)) return;
        for (var s : scr.getMenu().slots) {
            if (which.test(s.getItem())) {
                mouseTo(scr.getGuiLeft() + s.x + 8, scr.getGuiTop() + s.y + 8);
                return;
            }
        }
        LOGGER.warn("[smoketest] M22.3 item to hover not found");
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

    private int wait;

    private void server(java.util.function.Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToasts().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m22_3_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
