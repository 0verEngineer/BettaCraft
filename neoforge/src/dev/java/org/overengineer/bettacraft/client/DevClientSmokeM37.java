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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.fertilizer.FertilizerMakerBlock;
import org.overengineer.bettacraft.fertilizer.FertilizerMakerBlockEntity;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.winding.Winding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M37 client smoke ({@code -Psmoketestm37}): a tank on cabinets with an external filter, a light and a fertilizer maker
 * that pipes its fertilizer into the tank; the power options for filters, heaters and lights are on and there is no FE, the
 * cabinet is wound. Screenshots {@code bettacraft_m37_machine} (world, no HUD), {@code _crank} (the crank in the hand at the
 * cabinet, the action bar), {@code _maker_menu}, {@code _cabinet_menu} and {@code _water} (the water tab with the fertilizer
 * level and the spring), the menus also in German ({@code _de}). Checks (errors in the log): the maker works on its spring,
 * the crank winds the cabinet, the cabinet powers the tank. The power config is restored at the end.
 */
final class DevClientSmokeM37 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final BlockPos base;
    private final String lang = Minecraft.getInstance().options.languageCode;
    private final boolean heater = ServerConfig.POWER_HEATER.get(), filter = ServerConfig.POWER_FILTER.get(), light = ServerConfig.POWER_LIGHT.get();
    private int wait;

    DevClientSmokeM37() {
        base = mc.player.blockPosition().offset(30, 0, 900);
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(1, () -> look(base.getX() + 6.2, base.getY() + 2.4, base.getZ() - 0.8, 40F, 28F));
        then(100, () -> shot("machine"));
        then(1, () -> mc.options.hideGui = false);
        // the crank in the hand, winding the cabinet (the action bar shows the spring)
        then(1, () -> look(base.getX() + 1.5, base.getY() + 1.1, base.getZ() - 0.4, 0F, 25F));
        then(20, () -> server(p -> {
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CRANK.get()));
            for (int i = 0; i < 3; i++) {
                var r = Winding.wind(p.serverLevel(), base.offset(1, 0, 2), p);
                if (r != Winding.Result.WOUND) LOGGER.error("[smoketest] M37 the crank does not wind the cabinet: {}", r);
            }
        }));
        then(10, () -> shot("crank"));
        // the fertilizer maker works on its spring (the power options are on, no FE)
        then(1, () -> server(p -> {
            BlockPos at = base.offset(5, 1, 3);
            if (p.serverLevel().getBlockEntity(at) instanceof FertilizerMakerBlockEntity m) {
                for (int i = 0; i < 120; i++) m.tick(p.serverLevel(), at, p.serverLevel().getBlockState(at));
                if (m.status() != FertilizerMakerBlockEntity.WORKING) LOGGER.error("[smoketest] M37 the fertilizer maker does not work: {}", m.status());
                org.overengineer.bettacraft.platform.Platform.get().openMenu(p, m, at);
            } else LOGGER.error("[smoketest] M37 no fertilizer maker");
        }));
        then(30, () -> {
            if (!(mc.screen instanceof org.overengineer.bettacraft.client.screen.FertilizerMakerScreen s)) LOGGER.error("[smoketest] M37 no fertilizer maker screen");
            else LOGGER.info("[smoketest] M37 fertilizer maker window: progress {} of {}, water {}, fertilizer {}, spring {} %, status {}", s.getMenu().progress(),
                    s.getMenu().batchTicks(), s.getMenu().water(), s.getMenu().fertilizer(), s.getMenu().springPercent(), s.getMenu().status());
            shot("maker_menu");
        });
        then(1, () -> quickLanguage("de_de"));
        then(20, () -> shot("maker_menu_de"));
        then(1, () -> quickLanguage(lang));
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.setScreen(null));
        // the cabinet's window with its spring
        then(1, () -> server(p -> {
            BlockPos at = base.offset(1, 0, 2);
            if (p.serverLevel().getBlockEntity(at) instanceof CabinetBlockEntity cab) org.overengineer.bettacraft.platform.Platform.get().openMenu(p, cab, at);
            else LOGGER.error("[smoketest] M37 no cabinet");
        }));
        then(30, () -> {
            if (!(mc.screen instanceof org.overengineer.bettacraft.client.screen.CabinetScreen s)) LOGGER.error("[smoketest] M37 no cabinet screen");
            else if (s.getMenu().springPercent() <= 0) LOGGER.error("[smoketest] M37 the cabinet shows no spring: {}", s.getMenu().springPercent());
            shot("cabinet_menu");
        });
        then(1, () -> quickLanguage("de_de"));
        then(20, () -> shot("cabinet_menu_de"));
        then(1, () -> quickLanguage(lang));
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.setScreen(null));
        // the water tab: the fertilizer level and the devices on the spring
        then(1, () -> server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), base.offset(0, 1, 2));
            org.overengineer.bettacraft.tank.TankCare.care(p.serverLevel(), c, 20);
            if (!c.filterPowered() || c.springUnits() <= 0) LOGGER.error("[smoketest] M37 the cabinet does not power the tank: filter {}, devices {}",
                    c.filterPowered(), c.springUnits());
            org.overengineer.bettacraft.platform.Platform.get().openMenu(p, new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        }));
        then(30, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.selectTabForTest(5);
            else LOGGER.error("[smoketest] M37 no tank screen");
        });
        then(30, () -> shot("water"));
        then(1, () -> quickLanguage("de_de"));
        then(20, () -> shot("water_de"));
        then(1, () -> quickLanguage(lang));
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.setScreen(null));
        then(1, () -> {
            ServerConfig.POWER_HEATER.set(heater);
            ServerConfig.POWER_FILTER.set(filter);
            ServerConfig.POWER_LIGHT.set(light);
        });
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ServerConfig.POWER_HEATER.set(true);
        ServerConfig.POWER_FILTER.set(true);
        ServerConfig.POWER_LIGHT.set(true);
        if (!Features.enabled(Feature.WINDING) || !Features.enabled(Feature.FERTILIZER)) LOGGER.error("[smoketest] M37 winding or fertilizer is off");
        for (int x = -4; x <= 10; x++)
            for (int z = -6; z <= 8; z++) {
                level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
                for (int y = 0; y < 6; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
        // three cabinets under the front row of the tank (the middle one with a filter), stone under the back row
        for (int x = 0; x < 3; x++) {
            level.setBlockAndUpdate(base.offset(x, 0, 2), ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
            level.setBlockAndUpdate(base.offset(x, 0, 3), Blocks.SMOOTH_STONE.defaultBlockState());
        }
        TankBlockEntity c = tank(level, p, base.offset(0, 1, 2), 3, 2, 2);
        if (level.getBlockEntity(base.offset(1, 0, 2)) instanceof CabinetBlockEntity cab) {
            cab.items().setStackInSlot(CabinetBlockEntity.FILTER_SLOT, new ItemStack(ModItems.EXTERNAL_FILTER.get()));
            cab.windSpring().set(cab.windSpring().capacity() * 3 / 5);
        }
        // an external filter with a heater beside the tank, lights on top
        level.setBlockAndUpdate(base.offset(3, 1, 2), ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.WEST));
        if (level.getBlockEntity(base.offset(3, 1, 2)) instanceof ExternalFilterBlockEntity f) f.insertHeater(new ItemStack(ModItems.HEATER_SMALL.get()));
        for (int x = 0; x < 3; x++) level.setBlockAndUpdate(base.offset(x, 3, 2), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.LIT, true));
        // the fertilizer maker on a stone, glass pipes into the tank
        level.setBlockAndUpdate(base.offset(5, 0, 3), Blocks.SMOOTH_STONE.defaultBlockState());
        level.setBlockAndUpdate(base.offset(5, 1, 3), ModBlocks.FERTILIZER_MAKER.get().defaultBlockState().setValue(FertilizerMakerBlock.FACING, Direction.NORTH));
        level.setBlockAndUpdate(base.offset(4, 1, 3), ModBlocks.GLASS_PIPE.get().defaultBlockState());
        level.setBlockAndUpdate(base.offset(3, 1, 3), ModBlocks.GLASS_PIPE.get().defaultBlockState());
        if (level.getBlockEntity(base.offset(5, 1, 3)) instanceof FertilizerMakerBlockEntity m) {
            m.items().setStackInSlot(FertilizerMakerBlockEntity.MEAL, new ItemStack(ModItems.KELP_MEAL.get(), 6));
            m.items().setStackInSlot(FertilizerMakerBlockEntity.SALT, new ItemStack(ModItems.NUTRIENT_SALT.get(), 5));
            m.items().setStackInSlot(FertilizerMakerBlockEntity.BOTTLES, new ItemStack(Items.GLASS_BOTTLE, 3));
            m.setWater(2750);
            m.setFertilizer(1250);
            m.windSpring().set(m.windSpring().capacity() * 7 / 10);
        }
        // a few plants floating, some fertilizer in the water
        c.getTank().setFertilizer(0.8F);
        level.setBlockAndUpdate(base.offset(1, 4, 1), Blocks.LIGHT.defaultBlockState());
        var d = org.overengineer.bettacraft.entity.betta.BettaData.create(org.overengineer.bettacraft.genetics.BettaGenotype.randomDomestic(
                net.minecraft.util.RandomSource.create(37)), false, 0);
        var bucket = org.overengineer.bettacraft.entity.betta.BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d);
        var err = org.overengineer.bettacraft.tank.TankInhabitants.insert(level, c, bucket, org.overengineer.bettacraft.registry.ModEntities.BETTA.get(), null, false);
        if (err != null) LOGGER.error("[smoketest] M37 betta not inserted: {}", err.getString());
        org.overengineer.bettacraft.tank.TankEquipment.invalidate(c);
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
        c.getTank().setPowerBuffer(0);
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m37_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
