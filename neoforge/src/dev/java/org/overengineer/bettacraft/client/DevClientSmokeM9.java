package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
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
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.overengineer.bettacraft.client.screen.TankScreen;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity;
import org.overengineer.bettacraft.food.CultureTubBlockEntity;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.menu.TankMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M9 part of the dev client smoke test: a tank with lids, a light and a feeder on top, an external filter with a cooler
 * and a heater, used water; next to it a larva tub and an artemia hatchery with food ready. Screenshots
 * {@code bettacraft_m9_*}: the scene, the breeders, the tank menu (water tab and overview with the warning) and, with
 * Jade, the overlays of the tank and the larva tub.
 */
final class DevClientSmokeM9 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private BlockPos tankMin, hatchPos;

    DevClientSmokeM9() {
        then(1, () -> server(this::build));
        // the glowstone light has spread now: hatch one batch of nauplii
        then(20, () -> server(p -> {
            if (p.level().getBlockEntity(hatchPos) instanceof CultureTubBlockEntity t) {
                int n = t.advance(p.level(), 1.05);
                LOGGER.info("[smoketest] M9 hatchery: {} batch(es), {} (blocked {})", n, t.items().getStackInSlot(CultureTubBlockEntity.OUTPUT), t.blocker(p.level()));
            }
        }));
        then(1, () -> org.overengineer.bettacraft.client.DevHud.setHidden(mc, true));
        then(30, () -> look(tankMin.getX() + 3.2, tankMin.getY() + 3.4, tankMin.getZ() - 4.2, 0F, 30F));
        then(60, () -> shot("scene"));
        then(1, () -> look(tankMin.getX() + 6.0, tankMin.getY() + 1.4, tankMin.getZ() - 1.3, 0F, 45F));
        then(50, () -> shot("breeders"));
        then(1, () -> org.overengineer.bettacraft.client.DevHud.setHidden(mc, false));
        // Jade (if installed): the tank and the larva tub
        then(1, () -> look(tankMin.getX() + 0.5, tankMin.getY() + 1.5, tankMin.getZ() - 2.2, 0F, 0F));
        then(50, () -> shot("overlay_tank"));
        then(1, () -> look(tankMin.getX() + 5.5, tankMin.getY() + 1.1, tankMin.getZ() - 0.9, 0F, 40F));
        then(50, () -> shot("overlay_larva_tub"));
        // the tank menu: water tab, then the overview with the warning line
        then(1, () -> server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.level(), tankMin);
            org.overengineer.bettacraft.platform.Platform.get().openMenu(p, new SimpleMenuProvider((id, inv, pl) -> new TankMenu(id, inv, c), Component.translatable("gui.bettacraft.tank.title")),
                    buf -> buf.writeBlockPos(c.getBlockPos()));
        }));
        then(30, () -> {
            if (mc.gui.screen() instanceof TankScreen ts) ts.selectTabForTest(5);
        });
        then(30, () -> shot("menu_water"));
        then(1, () -> {
            if (mc.gui.screen() instanceof TankScreen ts) ts.selectTabForTest(0);
        });
        then(20, () -> shot("menu_overview"));
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.gui.setScreen(null));
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        BlockPos base = p.blockPosition().offset(0, 2, 100);
        tankMin = base;
        for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) for (int x = 0; x < 3; x++) {
            BlockPos pos = tankMin.offset(x, y, z);
            level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
            TankMultiblock.onTankBlockAdded(level, pos, null);
        }
        TankBlockEntity c = TankMultiblock.getController(level, tankMin);
        TankInteractions.changeWater(level, c, 10000);
        c.getTank().setName("Desert tank");
        c.getTank().setSignVisible(true);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setOwner(p.getUUID());
        c.getTank().setTargetTemp(24.0F);
        BlockPos efp = tankMin.offset(-1, 0, 0);
        level.setBlockAndUpdate(efp, ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.EAST));
        if (level.getBlockEntity(efp) instanceof ExternalFilterBlockEntity ef) {
            ef.insertHeater(new ItemStack(ModItems.COOLER_MEDIUM.get()));
            ef.insertHeater(new ItemStack(ModItems.HEATER_SMALL.get()));
        }
        TankEquipment.invalidate(c);
        // top: lids on the back row, a light and the feeder in front, one block open
        int top = tankMin.getY() + 2;
        for (int x = 0; x < 3; x++) level.setBlockAndUpdate(new BlockPos(tankMin.getX() + x, top, tankMin.getZ() + 1), ModBlocks.TANK_LID.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(tankMin.getX(), top, tankMin.getZ()), ModBlocks.TANK_LIGHT.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(tankMin.getX() + 1, top, tankMin.getZ()), ModBlocks.FEEDER.get().defaultBlockState());
        List<Map<String, String>> genes = List.of(
                Map.of("finform", "halfmoon", "fin_length", "long", "body_color", "royal_blue", "fin_color", "red", "pattern", "butterfly",
                        "pattern_color", "white", "iridescence", "high"),
                Map.of("finform", "round", "fin_length", "short", "body_color", "yellow", "fin_color", "yellow", "pattern", "koi",
                        "pattern_color", "orange"));
        for (int i = 0; i < genes.size(); i++) {
            BettaData d = BettaData.create(BettaGenome.of(genes.get(i), BettaGenome.DEFAULT), i == 1, level.getGameTime());
            ItemStack bucket = BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d);
            Component err = TankInhabitants.insert(level, c, bucket, ModEntities.BETTA.get(), p, false);
            if (err != null) LOGGER.warn("[smoketest] M9 insert failed: {}", err.getString());
        }
        int i = 0;
        for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) {
            if (!(e instanceof BettaEntity b)) continue;
            SmokeFreeze.freeze(b, true);
            b.snapTo(tankMin.getX() + 1.8 + i * 0.6, tankMin.getY() + 0.7 + i * 0.4, tankMin.getZ() + 0.8, 90F - i * 180F, 0F);
            b.setYBodyRot(b.getYRot());
            b.setYHeadRot(b.getYRot());
            i++;
        }
        // used water: the menu warns
        c.getTank().setWaterQuality(0.48F);
        c.markTankChanged();
        // live food breeders next to the tank
        BlockPos tub = tankMin.offset(5, 0, 0), hatch = tankMin.offset(7, 0, 0);
        hatchPos = hatch;
        level.setBlockAndUpdate(tub, ModBlocks.LARVA_TUB.get().defaultBlockState());
        level.setBlockAndUpdate(hatch, ModBlocks.ARTEMIA_HATCHERY.get().defaultBlockState());
        level.setBlockAndUpdate(hatch.above().east(), Blocks.GLOWSTONE.defaultBlockState());
        if (level.getBlockEntity(tub) instanceof CultureTubBlockEntity t) {
            t.setWater(1000);
            t.automation().insertItem(CultureTubBlockEntity.INPUT, new ItemStack(Items.OAK_LEAVES, 8), false);
            t.advance(level, 2.5);
        }
        if (level.getBlockEntity(hatch) instanceof CultureTubBlockEntity t) {
            for (int k = 0; k < 4; k++) t.automation().insertItem(CultureTubBlockEntity.INPUT, new ItemStack(ModItems.SALT_WATER_BOTTLE.get()), false);
            t.automation().insertItem(CultureTubBlockEntity.INPUT, new ItemStack(ModItems.ARTEMIA_CYSTS.get()), false);
            t.automation().insertItem(CultureTubBlockEntity.BOTTLES, new ItemStack(Items.GLASS_BOTTLE, 4), false);
            t.setWater(1000);
        }
        LOGGER.info("[smoketest] M9 scene: tank {} with {} bettas, quality {}, breeders at {} / {}", tankMin, i, c.getTank().waterQuality(), tub, hatch);
    }

    /** Puts the eye of the player at (x, eyeY, z). */
    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.level(), x, eyeY - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, false));
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    /** Returns true when all steps are done. */
    boolean tick() {
        if (mc.player != null && !steps.isEmpty()) {
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        }
        if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.PauseScreen) mc.gui.setScreen(null);
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
        mc.gui.toastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m9_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
