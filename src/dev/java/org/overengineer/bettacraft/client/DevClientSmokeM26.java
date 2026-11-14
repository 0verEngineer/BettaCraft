package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.food.LiveCultureBlockEntity;
import org.overengineer.bettacraft.food.LiveFoodPumpBlockEntity;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M26 part a client smoke ({@code -Psmoketestm26}): a tubifex dish, a daphnia barrel with a live food pump that feeds a
 * tank through glass pipes, an artemia hatchery and a larva tub for comparison. Screenshots {@code bettacraft_m26_cultures},
 * {@code _pump} (its window) and {@code _pump_de}. Checks (errors in the log): the pump reaches the tank and pumps.
 * Part b: the food maker with a hopper on top ({@code _maker}) and its window with flakes in the making ({@code _maker_menu},
 * {@code _maker_menu_de}).
 */
final class DevClientSmokeM26 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final BlockPos base;
    private final String lang = Minecraft.getInstance().options.languageCode;
    private int wait;

    DevClientSmokeM26() {
        base = mc.player.blockPosition().offset(-10, 0, 760);
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(1, () -> look(base.getX() + 3.0, base.getY() + 2.4, base.getZ() - 2.6, 0F, 38F));
        then(100, () -> shot("cultures"));
        then(1, () -> mc.options.hideGui = false);
        then(1, () -> server(p -> {
            if (p.serverLevel().getBlockEntity(base.offset(4, 0, 0)) instanceof LiveFoodPumpBlockEntity pump) {
                pump.doseNow();
                pump.step(p.serverLevel());
                if (pump.status() != LiveFoodPumpBlockEntity.PUMPED) LOGGER.error("[smoketest] M26 the live food pump does not pump: {}", pump.status());
                p.openMenu(pump, base.offset(4, 0, 0));
            } else LOGGER.error("[smoketest] M26 no live food pump");
        }));
        then(30, () -> {
            if (!(mc.screen instanceof org.overengineer.bettacraft.client.screen.LiveFoodPumpScreen)) LOGGER.error("[smoketest] M26 no pump screen");
            shot("pump");
        });
        then(1, () -> quickLanguage("de_de"));
        then(20, () -> shot("pump_de"));
        then(1, () -> quickLanguage(lang));
        then(1, () -> mc.setScreen(null));
        // part b: the food maker
        then(1, () -> mc.options.hideGui = true);
        then(1, () -> look(base.getX() + 12.3, base.getY() + 1.5, base.getZ() - 1.3, 45F, 28F));
        then(60, () -> shot("maker"));
        then(1, () -> mc.options.hideGui = false);
        then(1, () -> server(p -> {
            BlockPos at = base.offset(10, 0, 0);
            if (p.serverLevel().getBlockEntity(at) instanceof org.overengineer.bettacraft.food.FoodMakerBlockEntity m) {
                for (int i = 0; i < 120; i++) m.tick(p.serverLevel(), at, p.serverLevel().getBlockState(at));
                if (m.status() != org.overengineer.bettacraft.food.FoodMakerBlockEntity.WORKING) LOGGER.error("[smoketest] M26 the food maker does not work: {}", m.status());
                LOGGER.info("[smoketest] M26 food maker progress {} of {}", m.progress(), org.overengineer.bettacraft.food.FoodMakerBlockEntity.batchTicks());
                p.openMenu(m, at);
            } else LOGGER.error("[smoketest] M26 no food maker");
        }));
        then(30, () -> {
            if (!(mc.screen instanceof org.overengineer.bettacraft.client.screen.FoodMakerScreen s)) LOGGER.error("[smoketest] M26 no food maker screen");
            else LOGGER.info("[smoketest] M26 food maker window: progress {} of {}, drive {} of {}, status {}", s.getMenu().progress(), s.getMenu().batchTicks(),
                    s.getMenu().drive(), s.getMenu().driveMax(), s.getMenu().status());
            shot("maker_menu");
        });
        then(1, () -> quickLanguage("de_de"));
        then(20, () -> shot("maker_menu_de"));
        then(1, () -> quickLanguage(lang));
        then(1, () -> mc.setScreen(null));
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        for (int x = -4; x <= 12; x++)
            for (int z = -6; z <= 6; z++) {
                level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
                for (int y = 0; y < 5; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
        // a tank at the back with a betta, glass pipes to the pump, the daphnia barrel next to the pump
        TankBlockEntity c = tank(level, p, base.offset(4, 0, 2), 3, 2, 2);
        level.setBlockAndUpdate(base.offset(5, 0, 1), ModBlocks.GLASS_PIPE.get().defaultBlockState());
        level.setBlockAndUpdate(base.offset(5, 0, 0), ModBlocks.GLASS_PIPE.get().defaultBlockState());
        level.setBlockAndUpdate(base.offset(4, 0, 0), ModBlocks.LIVE_FOOD_PUMP.get().defaultBlockState());
        level.setBlockAndUpdate(base.offset(3, 0, 0), ModBlocks.DAPHNIA_BARREL.get().defaultBlockState());
        level.setBlockAndUpdate(base.offset(1, 0, 0), ModBlocks.TUBIFEX_DISH.get().defaultBlockState());
        level.setBlockAndUpdate(base.offset(0, 0, 1), ModBlocks.ARTEMIA_HATCHERY.get().defaultBlockState());
        level.setBlockAndUpdate(base.offset(7, 0, 0), ModBlocks.LARVA_TUB.get().defaultBlockState());
        if (level.getBlockEntity(base.offset(3, 0, 0)) instanceof LiveCultureBlockEntity b) {
            b.setWater(4000);
            b.model().population = 0.8;
            b.model().food = 0.5;
            b.advance(level, 0.001);
        }
        if (level.getBlockEntity(base.offset(1, 0, 0)) instanceof LiveCultureBlockEntity d) {
            d.setMud(true);
            d.setWater(800);
            d.model().population = 0.7;
            d.model().food = 0.4;
            d.advance(level, 0.001);
        }
        if (level.getBlockEntity(base.offset(0, 0, 1)) instanceof org.overengineer.bettacraft.food.CultureTubBlockEntity h) h.setWater(750);
        if (level.getBlockEntity(base.offset(7, 0, 0)) instanceof org.overengineer.bettacraft.food.CultureTubBlockEntity t) t.setWater(750);
        level.setBlockAndUpdate(base.offset(2, 2, 0), Blocks.LIGHT.defaultBlockState());
        // part b: the food maker (front to the camera, the crank on the right), flakes in the making
        level.setBlockAndUpdate(base.offset(10, 0, 0), ModBlocks.FOOD_MAKER.get().defaultBlockState());
        if (level.getBlockEntity(base.offset(10, 0, 0)) instanceof org.overengineer.bettacraft.food.FoodMakerBlockEntity m) {
            m.items().setStackInSlot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SALMON, 6));
            m.items().setStackInSlot(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WHEAT, 12));
            m.items().setStackInSlot(2, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.KELP, 8));
            m.items().setStackInSlot(3, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SUGAR, 8));
            m.items().setStackInSlot(org.overengineer.bettacraft.food.FoodMakerBlockEntity.OUTPUT,
                    new net.minecraft.world.item.ItemStack(org.overengineer.bettacraft.registry.ModItems.FISH_FLAKES.get(), 24));
            m.setSpring(org.overengineer.bettacraft.food.FoodMakerBlockEntity.springMax() * 5 / 8);
            for (int i = 0; i < 2; i++) m.wind(null); // the crank turns
        }
        var d = org.overengineer.bettacraft.entity.betta.BettaData.create(org.overengineer.bettacraft.genetics.BettaGenotype.randomDomestic(
                net.minecraft.util.RandomSource.create(26)), false, 0);
        var bucket = org.overengineer.bettacraft.entity.betta.BettaBucketItem.withFish(new net.minecraft.world.item.ItemStack(
                org.overengineer.bettacraft.registry.ModItems.BETTA_BUCKET.get()), d);
        var err = org.overengineer.bettacraft.tank.TankInhabitants.insert(level, c, bucket, org.overengineer.bettacraft.registry.ModEntities.BETTA.get(), null, false);
        if (err != null) LOGGER.error("[smoketest] M26 betta not inserted: {}", err.getString());
        c.markTankChanged();
    }

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, (long) sx * sy * sz * 900, 0F);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(p.getUUID());
        c.getTank().setWaterTemp(26F);
        c.getTank().setTargetTemp(26F);
        c.markTankChanged();
        return c;
    }

    // ------------------------------------------------------------------ helpers

    private void quickLanguage(String code) {
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m26_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
